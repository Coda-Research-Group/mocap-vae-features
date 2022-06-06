import argparse
from pathlib import Path

import pandas as pd
import pytorch_lightning as pl
from pytorch_lightning import Trainer, seed_everything
from pytorch_lightning.callbacks import ModelCheckpoint, LearningRateMonitor
import seaborn as sns
import torch
import torch.nn as nn
import torch.nn.functional as F

from datamodules import MoCapDataModule


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
        input_dim=93,
        enc_out_dim=512,
        latent_dim=256,
    ):
        super().__init__()

        self.save_hyperparameters()

        # input: T x (3*J)

        # encoder, decoder
        self.encoder = nn.Sequential(  # input: T x input_dim
            nn.Conv1d(input_dim, 64, kernel_size=1, stride=1, padding=0),  # output: T x 64
            ResBlock(64, 64),  # output: T x 64
            ResBlock(64, 128, downsample=True),  # output: (T/2) x 128
            ResBlock(128, 256, downsample=True),  # output: (T/4) x 256
        )

        # distribution parameters
        self.fc_mu = nn.Linear(enc_out_dim, latent_dim)
        self.fc_var = nn.Linear(enc_out_dim, latent_dim)

        self.decoder = nn.Sequential(  # input: 1 x latent_dim
            nn.Upsample(scale_factor=2),  # output: 2 x latent_dim
            ResBlock(latent_dim, 256),  # output: 2 x 256
            nn.Upsample(scale_factor=2),  # output: 4 x 256
            ResBlock(256, 128),  # output: 4 x 128
            nn.Upsample(scale_factor=2),  # output: 8 x 128
            ResBlock(128, 64),  # output: 8 x 64
            nn.Conv1d(64, input_dim, kernel_size=1, stride=1, padding=0),  # output: T x 64
        )

        # for the gaussian likelihood
        self.log_scale = nn.Parameter(torch.Tensor([0.0]))

    def configure_optimizers(self):
        return torch.optim.AdamW(self.parameters(), lr=1e-4)

    def gaussian_likelihood(self, x_hat, logscale, x):
        scale = torch.exp(logscale)
        mean = x_hat
        dist = torch.distributions.Normal(mean, scale)

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
        x, = batch  # B x T x J x 3
        x = x.flatten(start_dim=2)  # B x T x (J*3)
        x = x.swapaxes(1, 2)  # B x (J*3) x T

        # encode x to get the mu and variance parameters
        x_encoded = self.encoder(x).flatten(start_dim=1)
        mu, log_var = self.fc_mu(x_encoded), self.fc_var(x_encoded)

        # sample z from q
        std = torch.exp(log_var / 2)
        q = torch.distributions.Normal(mu, std)
        z = q.rsample()

        # decoded
        x_hat = self.decoder(z.unsqueeze(-1))

        # reconstruction loss
        recon_loss = self.gaussian_likelihood(x_hat, self.log_scale, x)

        # kl
        kl = self.kl_divergence(z, mu, std)

        # elbo
        elbo = (kl - recon_loss)
        elbo = elbo.mean()

        self.log_dict({
            f'{stage}/elbo': elbo,
            f'{stage}/kl': kl.mean(),
            f'{stage}/recon_loss': recon_loss.mean(),
        }, prog_bar=True)

        return elbo

    def training_step(self, *args, **kwargs):
        return self._common_step('train', *args, **kwargs)
    
    def validation_step(self, *args, **kwargs):
        return self._common_step('val', *args, **kwargs)
    
    def test_step(self, *args, **kwargs):
        return self._common_step('test', *args, **kwargs)

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
        z = z.unsqueeze(-1)
        x_hat = self.decoder(z)
        x_hat = x_hat.swapaxes(1, 2)
        n_batches, n_frames, n_coords = x_hat.shape
        n_joints = n_coords // 3
        x_hat = x_hat.reshape(n_batches, n_frames, n_joints, 3)
        return x_hat


def main(args):
    seed_everything(127, workers=True)

    root_dir = Path('runs') / args.data_path.stem
    root_dir.mkdir(parents=True, exist_ok=True)

    dm = MoCapDataModule(args.data_path, batch_size=args.batch_size)
    model = LitVAE(latent_dim=args.latent_dim)

    resume = None
    if args.resume:
        ckpts = root_dir.glob('version_*/checkpoints/*.ckpt')
        ckpts = sorted(ckpts, reverse=True, key=lambda p: p.stat().st_mtime)
        resume = ckpts[0] if ckpts else None

    trainer = Trainer(
        default_root_dir=root_dir,
        resume_from_checkpoint=resume,
        max_epochs=args.epochs,
        gpus=1,
        deterministic=True,
        log_every_n_steps=5,
        terminate_on_nan=True,
        callbacks=[
            ModelCheckpoint(monitor='val/elbo', save_last=True),
            LearningRateMonitor(logging_interval='step'),
        ]
    )

    try:
        trainer.fit(model, dm)
    except ValueError as e:
        print('Something\'s wrong happened while fitting:', e)

    trainer.test(datamodule=dm)
    
    predictions = trainer.predict(datamodule=dm)
    predictions = torch.concat(predictions, 0).numpy()
    predictions = pd.DataFrame(predictions, index=dm.predict_ids)
    predictions.index.name = 'id'

    # prediction csv
    run_dir = Path(trainer.log_dir)
    predictions_csv = run_dir / 'predictions.csv'
    predictions.to_csv(predictions_csv)

    # predictions in .data format
    predictions_data_file = run_dir / 'predictions.data'
    predictions.index = predictions.index.str.rsplit('_', 1, expand=True).rename(['seq_id', 'frame'])
    
    with open(predictions_data_file, 'w') as f:
        for seq_id, group in predictions.groupby(level='seq_id'):
            print(f'#objectKey messif.objects.keys.AbstractObjectKey {seq_id}', file=f)
            print(f'{len(group)};mcdr.objects.ObjectMocapPose', file=f)
            print(group.to_csv(index=False, header=False), end='', file=f)
    
    # segments ids
    pd.DataFrame(dm.train_ids).to_csv(run_dir / 'train_ids.txt', header=False, index=False)
    pd.DataFrame(dm.valid_ids).to_csv(run_dir / 'valid_ids.txt', header=False, index=False)
    pd.DataFrame( dm.test_ids).to_csv(run_dir /  'test_ids.txt', header=False, index=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Train MoCap VAE')
    parser.add_argument('data_path', type=Path, help='data path')
    
    parser.add_argument('-d', '--latent-dim', type=int, default=32, help='VAE code size')
    parser.add_argument('-b', '--batch-size', type=int, default=512, help='number of training epochs')
    parser.add_argument('-e', '--epochs', type=int, default=100, help='number of training epochs')
    parser.add_argument('-r', '--resume', default=False, action='store_true', help='resume training')

    args = parser.parse_args()
    main(args)