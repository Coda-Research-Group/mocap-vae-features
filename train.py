import argparse
import gzip
import math
from pathlib import Path

from joblib import delayed, Parallel
import hydra
import numpy as np
import pandas as pd
import pytorch_lightning as pl
from pytorch_lightning import Trainer, seed_everything
from pytorch_lightning.callbacks import ModelCheckpoint, LearningRateMonitor, EarlyStopping
from pytorch_lightning.loggers import TensorBoardLogger
from torch.optim.lr_scheduler import ReduceLROnPlateau
import seaborn as sns
import torch
import torch.nn as nn
import torch.nn.functional as F
from tqdm import tqdm

import body_models
from datamodules import MoCapDataModule
from reconstruct import create_tensor
import evaluate


class ResBlock(nn.Module):
    def __init__(self, in_channels, out_channels, downsample=False):
        super(ResBlock, self).__init__()

        stride = 2 if downsample else 1

        self.module = nn.Sequential(
            nn.BatchNorm1d(in_channels),
            nn.ReLU(),
            nn.Conv1d(in_channels, out_channels, kernel_size=3, stride=stride, padding=1),
            nn.BatchNorm1d(out_channels),
            nn.ReLU(),
            nn.Conv1d(out_channels, out_channels, kernel_size=3, stride=1, padding=1),
        )

        if in_channels != out_channels:
            self.shortcut = nn.Conv1d(in_channels, out_channels, kernel_size=3, stride=stride, padding=1)
        else:
            self.shortcut = nn.Identity()

    def forward(self, x):
        return self.module(x) + self.shortcut(x)


class LitVAE(pl.LightningModule):
    def __init__(
        self,
        body_model='hdm05',
        input_length=8,
        input_fps=12,
        latent_dim=256,
        beta=1,
        learning_rate=1e-4,
        extra_dms=None,
    ):
        super().__init__()

        self.beta = beta
        self.input_fps = input_fps
        self.learning_rate = learning_rate
        self.save_hyperparameters()
        self.extra_dms = extra_dms

        self.body_model = body_models.get_by_name(body_model)
        input_dim = self.body_model.num_joints * self.body_model.num_dimensions

        # encoder, decoder
        self.encoder = nn.Sequential(  # input: input_dim x T
            nn.Conv1d(input_dim, 64, kernel_size=1, stride=1, padding=0),  # output: 64 x T
            ResBlock(64, 64),  # output: 64 x T
            ResBlock(64, 128, downsample=True),  # output: 128 x (T/2)
            ResBlock(128, 256, downsample=True),  # output: 256 x (T/4)
        )

        encoder_output_dim = 256 * input_length // 4
        up_factor = lambda i: 2 if 2**(i+1) <= input_length else 1
        last_factor = input_length / min(8, 2**math.floor(math.log2(input_length)))

        # distribution parameters
        self.fc_mu  = nn.Linear(encoder_output_dim, latent_dim)
        self.fc_var = nn.Linear(encoder_output_dim, latent_dim)

        self.decoder = nn.Sequential(  # input: latent_dim x 1
            nn.Upsample(scale_factor=up_factor(0)),  # output: latent_dim x 2
            ResBlock(latent_dim, 256),  # output: 256 x 2
            nn.Upsample(scale_factor=up_factor(1)),  # output: 256 x 4
            ResBlock(256, 128),  # output: 128 x 4
            nn.Upsample(scale_factor=up_factor(2)),  # output: 128 x 8
            ResBlock(128, 64),  # output: 64 x 8
            nn.Upsample(scale_factor=last_factor),  # output: 64 x T
            ResBlock(64, 64),  # output: 64 x T
            nn.Conv1d(64, 2*input_dim, kernel_size=1, stride=1, padding=0),  # output: 2*input_dim (mean and logstd) x T
        )

        self._do_videos = False
        self._do_retrieval_eval = False
        self._preview_samples = []

    def configure_optimizers(self):
        optimizer = torch.optim.AdamW(self.parameters(), lr=self.learning_rate)
        lr_scheduler = ReduceLROnPlateau(optimizer, mode='min', factor=0.1, patience=10)
        return {
            "optimizer": optimizer,
            "lr_scheduler": {
                "scheduler": lr_scheduler,
                "monitor": "val/elbo",
                "interval": "epoch",
                "frequency": 1,
            },
        }

    def gaussian_likelihood(self, x_mean, x_logstd, x):
        x_std = torch.exp(x_logstd)
        dist = torch.distributions.Normal(x_mean, x_std)

        # measure prob of seeing sample under p(x|z)
        log_pxz = dist.log_prob(x)
        return log_pxz.sum(dim=(1, 2))

    def kl_divergence(self, z, mu, std):
        # --------------------------
        # Monte carlo KL divergence
        # --------------------------
        # 1. define the first two probabilities (in this case Normal for both)
        p = torch.distributions.Normal(torch.zeros_like(mu), torch.ones_like(std))
        q = torch.distributions.Normal(mu, std)

        # 2. get the probabilities from the equation
        log_qzx = q.log_prob(z)
        log_pz = p.log_prob(z)

        # kl
        kl = (log_qzx - log_pz)
        kl = kl.sum(-1)
        return kl

    def _common_step(self, stage, batch, batch_idx):
        x, = batch  # B x T x J x D
        x = x.flatten(start_dim=2)  # B x T x (J*D)
        x = x.swapaxes(1, 2)  # B x (J*D) x T

        # encode x to get the mu and variance parameters
        x_encoded = self.encoder(x).flatten(start_dim=1)
        mu, log_var = self.fc_mu(x_encoded), self.fc_var(x_encoded)

        # sample z from q
        std = torch.exp(log_var / 2)
        q = torch.distributions.Normal(mu, std)
        z = q.rsample()

        # decoded
        x_hat = self.decoder(z.unsqueeze(-1))  # B x 2*J*D x T
        x_mean, x_logstd = torch.tensor_split(x_hat, 2, dim=1)  # B x J*D x T,  B x J*D x T

        # reconstruction loss
        recon_loss = self.gaussian_likelihood(x_mean, x_logstd, x)
        l2_loss = F.mse_loss(x_mean, x)

        # kl
        kl = self.kl_divergence(z, mu, std)

        # elbo
        elbo = (self.beta * kl - recon_loss)
        elbo = elbo.mean()

        metrics = {
            f'{stage}/elbo': elbo,
            f'{stage}/kl': kl.mean(),
            f'{stage}/recon_loss': recon_loss.mean(),
            f'{stage}/l2_loss': l2_loss.mean(),
        }

        self.log_dict(metrics, prog_bar=(stage != 'train'))

        return metrics

    def on_validation_batch_start(self, batch, batch_idx, dataloader_idx=0):
        every_n_batches = 7
        num_samples = 4

        if len(self._preview_samples) == num_samples:
            return

        if batch_idx % every_n_batches != 0:
            return

        sample = batch[0][:1]  # get first sample
        self._preview_samples.append(sample)

    def on_validation_start(self):
        every_n_epochs = 1
        self._do_videos = self.current_epoch % every_n_epochs == 0
        self._do_retrieval_eval = self.current_epoch % every_n_epochs == 0

    def _retrieval_validation(self):
        trainer = self.trainer

        def _get_info(ids):
            x_info = pd.DataFrame(ids)[0].str.split('_', expand=True)
            x_info.columns = ['parentSeqID', 'classID', 'offsetWithinParentSeq', 'actionLength', 'frameID']
            x_info = x_info.groupby(['parentSeqID', 'classID', 'offsetWithinParentSeq', 'actionLength'])
            x_info = x_info.groups
            return x_info

        def _extract(dl, info):
            # x = trainer.predict(self, dl)  # this breaks model device placement
            x = [self.encode(batch[0].cuda())[0] for batch in tqdm(dl, leave=False)]
            x = torch.vstack(x)
            x = F.normalize(x)
            x = x.cpu().numpy()

            x_actions = [x[indices] for group, indices in info.items()]
            x_labels = np.array([group[1] for group in info.keys()])

            return x_actions, x_labels

        accuracies = []

        for i, dm in enumerate(self.extra_dms):
            db_dl = dm.train_dataloader()
            q_dl = dm.val_dataloader()

            db_info = _get_info(dm.train_ids)
            q_info = _get_info(dm.valid_ids)

            db_actions, db_labels = _extract(db_dl, db_info)
            q_actions, q_labels = _extract(q_dl, q_info)

            accuracy = evaluate.one_nn_accuracy(
                q_actions, q_labels,
                db_actions, db_labels,
                approx=True,
                exclude_first_neighbor=False,
            )
            accuracies.append(accuracy)

        return accuracies

    def on_validation_epoch_end(self):
        if self._do_retrieval_eval:
            mean_1nn_accuracies = self._retrieval_validation()

            # Cannot use self.log_dict() here..
            acc_dict = {f'val/1nn_accuracy/dm{i}': v for i, v in enumerate(mean_1nn_accuracies)}
            self.log_dict(acc_dict, on_step=False, on_epoch=True)
            # for i, v in enumerate(mean_1nn_accuracies):
            #     self.logger.experiment.add_scalar(f'val/1nn_accuracy/dm{i}', v)

        # if self._do_videos:
        #     batch = torch.cat(self._preview_samples, dim=0)
        #     mu, std = self.encode(batch)
        #     recon, _ = self.decode(mu)

        #     batch = batch.cpu().numpy()
        #     recon = recon.cpu().numpy()

        #     func = delayed(create_tensor)
        #     videos = (func(x, x_hat, body_model=self.body_model) for x, x_hat in zip(batch, recon))
        #     videos = Parallel(n_jobs=-1)(videos)
        #     videos = [torch.from_numpy(v) for v in videos]
        #     videos = torch.stack(videos)  # B x T x 3 x H x W

        #     self.logger.experiment.add_video(f'val/anim', videos, self.current_epoch, self.input_fps)

    def on_train_start(self):
        self.logger.log_hyperparams(self.hparams, {"val/l2_loss": 0, "val/elbo": 0, 'val/1nn_accuracy/dm0': 0})

    def training_step(self, *args, **kwargs):
        metrics = self._common_step('train', *args, **kwargs)
        return metrics['train/elbo']

    def validation_step(self, *args, **kwargs):
        metrics = self._common_step('val', *args, **kwargs)
        return metrics['val/elbo']

    def test_step(self, *args, **kwargs):
        metrics = self._common_step('test', *args, **kwargs)
        return metrics['test/elbo']

    def predict_step(self, batch, batch_idx):
        return self.encode(batch[0])[0]

    def encode(self, x):
        # x has shape B x T x J x 3
        x = x.flatten(start_dim=2)  # B x T x (J*3)
        x = x.swapaxes(1, 2)  # B x (J*3) x T

        x_encoded = self.encoder(x).flatten(start_dim=1)
        mu, log_var = self.fc_mu(x_encoded), self.fc_var(x_encoded)
        std = torch.exp(log_var / 2)

        return mu, std

    def sample_z(self, mu, std):
        q = torch.distributions.Normal(mu, std)
        z = q.rsample()
        return z

    def decode(self, z):
        z = z.unsqueeze(-1)  # B x latent_dim x 1
        x_hat = self.decoder(z)  # B x (2*J*D) x T
        x_hat = x_hat.swapaxes(1, 2)  # B x T x (2*J*D)
        x_mean, x_logstd = torch.tensor_split(x_hat, 2, dim=2)  # B x T x J*D,  B x T x J*D

        n_batches, n_frames, n_coords = x_mean.shape
        n_joints = self.body_model.num_joints
        n_dims = self.body_model.num_dimensions

        x_mean = x_mean.reshape(n_batches, n_frames, n_joints, n_dims)
        x_logstd = x_logstd.reshape(n_batches, n_frames, n_joints, n_dims)
        return x_mean, x_logstd


def predict(trainer, model, ckpt_path, dm, prefix='', force=False):
    run_dir = Path(trainer.log_dir)

    predictions_csv = run_dir / f'{prefix}predictions.csv.gz'
    predictions_data_file = run_dir / f'{prefix}predictions.data.gz'

    if predictions_csv.exists() and not force:
        print('Skipping prediction. File exists:', predictions_csv.stem)
        return False

    print(f'Predicting: {prefix}')

    # prediction csv
    predictions = trainer.predict(model, ckpt_path=ckpt_path, datamodule=dm)
    predictions = torch.concat(predictions, 0).numpy()
    predictions = pd.DataFrame(predictions, index=dm.predict_ids)
    predictions.index.name = 'id'
    predictions.to_csv(predictions_csv)

    # predictions in .data format
    predictions.index = predictions.index.str.rsplit('_', n=1, expand=True).rename(['seq_id', 'frame'])
    with gzip.open(predictions_data_file, 'wt', encoding='utf8') as f:
        for seq_id, group in predictions.groupby(level='seq_id'):
            print(f'#objectKey messif.objects.keys.AbstractObjectKey {seq_id}', file=f)
            print(f'{len(group)};mcdr.objects.ObjectMocapPose', file=f)
            print(group.to_csv(index=False, header=False), end='', file=f)

    return True


@hydra.main(version_base=None, config_path='experiments', config_name='config')
def main(args):
    root_dir = Path.cwd()
    log_dir = root_dir / 'lightning_logs' / 'version_0'

    seed_everything(127, workers=True)

    dm = MoCapDataModule(
        args.data_path,
        train=args.train_split,
        valid=args.valid_split,
        test=args.test_split,
        batch_size=args.batch_size
    )

    # print("-------------------------")
    # print( args.additional_data_path,
    #         args.additional_train_split,
    #         args.additional_valid_split,
    #         args.additional_valid_split)
    # print("-------------------------")

    # extra_dms = [
    #     MoCapDataModule(
    #         path,
    #         train=train,
    #         valid=valid,
    #         test=test,
    #         batch_size=args.batch_size,
    #         shuffle_train=False,
    #     ) for path, train, valid, test in zip(
    #         args.additional_data_path,
    #         args.additional_train_split,
    #         args.additional_valid_split,
    #         args.additional_valid_split,
    #     )
    # ]

    # for edm in extra_dms:
    #     edm.prepare_data()
    #     edm.setup()

    extra_dms = []

    model = LitVAE(
        body_model=args.body_model,
        input_length=args.input_length,
        input_fps=args.input_fps,
        latent_dim=args.latent_dim,
        beta=args.beta,
        learning_rate=args.learning_rate,
        extra_dms=extra_dms,
    )

    logger = TensorBoardLogger(root_dir, version=0, default_hp_metric=False)
    trainer = Trainer(
        default_root_dir=root_dir,
        max_epochs=args.epochs,
        logger=logger,
        accelerator='gpu',
        devices=1,
        deterministic=True,
        num_sanity_val_steps=0,
        log_every_n_steps=5,
        callbacks=[
            EarlyStopping(monitor='val/l2_loss', patience=50),
            ModelCheckpoint(monitor='val/elbo', save_last=True),
            LearningRateMonitor(logging_interval='step'),
        ]
    )

    if not args.skip_train:
        last_ckpt_path = log_dir / 'checkpoints' / 'last.ckpt'
        resume_ckpt = last_ckpt_path if args.resume and last_ckpt_path.exists() else None
        trainer.fit(model, dm, ckpt_path=resume_ckpt)
        try:
            trainer.fit(model, dm, ckpt_path=resume_ckpt)
        except ValueError as e:
            print('Train terminated by error:', e)
            with open('terminated_by_error.txt', 'w') as f:
                f.write(str(e))
        ckpt_path = 'best'
    else:
        ckpts = (log_dir / 'checkpoints').glob('epoch=*.ckpt')
        ckpt_path = max(ckpts, key=lambda x: int(x.stem.split('-')[0].split('=')[1]))

    trainer.test(model, ckpt_path=ckpt_path, datamodule=dm)

    # predictions in .csv and .data format
    if predict(trainer, model, ckpt_path, dm):

        # save segments ids per split
        pd.DataFrame(dm.train_ids).to_csv(log_dir / 'train_ids.txt.gz', header=False, index=False)
        pd.DataFrame(dm.valid_ids).to_csv(log_dir / 'valid_ids.txt.gz', header=False, index=False)
        pd.DataFrame( dm.test_ids).to_csv(log_dir /  'test_ids.txt.gz', header=False, index=False)

    # predictions on additional datasets
    if hasattr(args, 'additional_data_path'):
        for additional_data_path in args.additional_data_path:
            dm = MoCapDataModule(additional_data_path, batch_size=args.batch_size)
            prefix = Path(additional_data_path).stem
            predict(trainer, model, ckpt_path, dm, prefix=prefix)


def argparse_cli():
    parser = argparse.ArgumentParser(description='Train MoCap VAE')
    parser.add_argument('data_path', type=Path, help='data path')
    parser.add_argument('--train-split', type=Path, help='train sequence ids')
    parser.add_argument('--valid-split', type=Path, help='validation sequence ids')
    parser.add_argument('--test-split', type=Path, help='test sequence ids')

    parser.add_argument('-m', '--body-model', default='hdm05', choices=('hdm05', 'pku-mmd'), help='body model')
    parser.add_argument('-i', '--input-length', type=int, default=512, help='input sequence length')
    parser.add_argument('-f', '--input-fps', type=int, default=12, help='sequence fps')
    parser.add_argument('-d', '--latent-dim', type=int, default=32, help='VAE code size')
    parser.add_argument('--beta', type=float, default=1, help='KL divergence weight')

    parser.add_argument('-b', '--batch-size', type=int, default=512, help='batch size')
    parser.add_argument('-e', '--epochs', type=int, default=250, help='number of training epochs')
    parser.add_argument('-r', '--resume', default=False, action='store_true', help='resume training')
    parser.add_argument('-s', '--skip-train', default=False, action='store_true', help='perform prediction only')

    parser.add_argument('-a', '--additional-data-path', default=None, type=Path, nargs='+', help='additional data on which prediction is run after training')
    parser.add_argument('--additional-train-split', type=Path, nargs='+', help='additional train sequence ids')
    parser.add_argument('--additional-valid-split', type=Path, nargs='+', help='additional validation sequence ids')
    parser.add_argument('--additional-test-split', type=Path, nargs='+', help='additional test sequence ids')

    args = parser.parse_args()
    main(args)


if __name__ == "__main__":
    main()
