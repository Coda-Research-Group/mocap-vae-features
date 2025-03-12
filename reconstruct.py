import argparse
import itertools
from pathlib import Path

import matplotlib.pyplot as plt
import matplotlib.backends.backend_agg as plt_backend_agg
import matplotlib.animation as animation
import numpy as np
import torch
from tqdm import tqdm

import body_models
from datamodules import MoCapDataModule


def visualize_pose(ax, pose, color, edges):
    artists = []
    for start, end in edges:
        x0, y0, z0 = pose[start]
        x1, y1, z1 = pose[end]
        artist = ax.plot([x0, x1], [y0, y1], [z0, z1], c=color, zdir='x')
        artists.extend(artist)
    
    return artists


def prepare_animation(ax, x, x_hat, xlim, ylim, zlim, edges):
    
    def animate(i):
        ax.clear()
        ax.set(xlim=xlim, xticks=[],
               ylim=ylim, yticks=[],
               zlim=zlim, zticks=[])
        ax.grid(False)

        artists = []
        artists.extend( visualize_pose(ax, x    [i], 'b', edges) )
        artists.extend( visualize_pose(ax, x_hat[i], 'r', edges) )
        return artists
    
    return animate


def create_gif(
    output_path,
    x,
    x_hat,
    body_model,
    xlim=None,
    ylim=None,
    zlim=None,
    fps=30,
):
    seq_len = len(x)

    fig = plt.figure()
    ax = fig.add_subplot(projection='3d')
    ani_func = prepare_animation(ax, x, x_hat, xlim, ylim, zlim, body_models.get_by_name(body_model).edges)
    ani = animation.FuncAnimation(fig, ani_func, seq_len, repeat=False, blit=True)
    ani.save(output_path, fps=fps)
    plt.close()


def _figure_to_numpy(figure):
    canvas = plt_backend_agg.FigureCanvasAgg(figure)
    canvas.draw()
    data = np.frombuffer(canvas.buffer_rgba(), dtype=np.uint8)
    w, h = figure.canvas.get_width_height()
    image_hwc = data.reshape([h, w, 4])[:, :, 0:3]
    image_chw = np.moveaxis(image_hwc, source=2, destination=0)
    return image_chw


def create_tensor(
    x,
    x_hat,
    body_model=None,
    body_edges=None,
    xlim=None,
    ylim=None,
    zlim=None,
):

    if body_model:
        body_edges = body_model.edges
        xlim = body_model.xlim
        ylim = body_model.ylim
        zlim = body_model.zlim

    fig = plt.figure()
    ax = fig.add_subplot(projection='3d')

    images = []
    for xi, xhi in zip(x, x_hat):
        ax.clear()
        ax.set(xlim=xlim, xticks=[],
               ylim=ylim, yticks=[],
               zlim=zlim, zticks=[])
        ax.grid(False)
        visualize_pose(ax, xi, 'b', body_edges)
        visualize_pose(ax, xhi, 'r', body_edges)
        image = _figure_to_numpy(fig)
        images.append(image)
    
    plt.close()
    return np.stack(images)


def main(args):
    from train import LitVAE

    ckpt = Path("runs/hdm05/all/beta=1,latent_dim=256/lightning_logs/version_0/checkpoints").glob('epoch*.ckpt')
    ckpt = next(iter(ckpt))

    model = LitVAE.load_from_checkpoint(ckpt)
    model = model.to('cpu')

    dm = MoCapDataModule(
        args.data_path,
        train=args.train_split,
        valid=args.valid_split,
        test=args.test_split,
        batch_size=1
    )

    dm.setup()
    dm.prepare_data()

    minX, minY, minZ = dm.predict_dataset.tensors[0].numpy().min(axis=(0, 1, 2))
    maxX, maxY, maxZ = dm.predict_dataset.tensors[0].numpy().max(axis=(0, 1, 2))

    gif_kws = dict(
        xlim=(minX, maxX),
        ylim=(minY, maxY),
        zlim=(minZ, maxZ),
        fps=args.fps,
        body_model=args.body_model,
    )

    gif_dir = Path("runs/hdm05/all/beta=1,latent_dim=256/lightning_logs/version_0") / 'reconstructions'
    gif_dir.mkdir(exist_ok=True)

    print(f'GIF output dir:', gif_dir)

    ids_and_samples = zip(dm.test_ids, dm.test_dataloader())
    ids_and_samples = itertools.islice(ids_and_samples, 0, None, args.every_n)
    ids_and_samples = itertools.islice(ids_and_samples, 0, args.limit)

    with torch.no_grad():
        for seq_id, (x,) in tqdm(ids_and_samples):
            mu, std = model.encode(x)
            x_mu, _ = model.decode(mu)

            gif_path = gif_dir / f'{seq_id}.gif'
            create_gif(gif_path, x[0], x_mu[0], **gif_kws)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Create reconstruction GIFs')
    parser.add_argument('run_dir', type=Path, help='Run directory')

    parser.add_argument('data_path', type=Path, help='Path to data file')
    parser.add_argument('--train-split', type=Path, help='train sequence ids')
    parser.add_argument('--valid-split', type=Path, help='validation sequence ids')
    parser.add_argument('--test-split', type=Path, help='test sequence ids')
    parser.add_argument('-b', '--body-model', choices=('hdm05', 'pku-mmd'), default=None, help='Body model')
    parser.add_argument('-r', '--fps', type=float, default=30, help='animation FPS')

    parser.add_argument('-e', '--every-n', type=int, default=100, help='how many samples to skip between reconstructed samples')
    parser.add_argument('-l', '--limit', type=int, default=5, help='how many samples to reconstruct')

    args = parser.parse_args()

    main(args)