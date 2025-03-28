from collections import Counter
import re
from itertools import groupby, chain
from pathlib import Path

import pytorch_lightning as pl
import numpy as np
import pandas as pd
import torch
from torch.utils.data import TensorDataset, DataLoader
from tqdm import tqdm


def split_subsequences_from_data_file(data_path):
    subsequence_lines = []
    with open(data_path, 'r') as lines:
        for line in lines:
            if line.lstrip().startswith("#objectKey"):
                if subsequence_lines:
                    yield subsequence_lines
                    subsequence_lines = []
            subsequence_lines.append(line.strip())

        if subsequence_lines:
            yield subsequence_lines


def parse_pose_line(line):
    values = re.split('; |, | ', line)
    values = map(float, values)
    values = np.fromiter(values, dtype=np.float32)
    values = values.reshape(-1, 3)
    return values


def convert_subsequence_to_numpy(subsequence_lines):
    header = subsequence_lines[:2]
    data = subsequence_lines[2:]

    # matches = re.match(r'.*\s(\d+_\d+_\d+_\d+)', header[0])
    # assert matches is not None, f'Error parsing data header: {header[0]}'
    # sample_id = matches.group(0)
    # seq_id, action_id, start_frame, duration = map(int, attributes[1:])
    sample_id = header[0].split(' ')[-1]

    data = map(parse_pose_line, data)
    data = np.stack(list(data))

    expected_frames = int(header[1].split(';')[0])
    assert len(data) == expected_frames, f'Error parsing data: expected {expected_frames} frames, got {len(data)}'

    return sample_id, data


def normalize_sequence_length(sequence, max_length):
    pad = max_length - len(sequence)
    if pad > 0:  # pad small sequences
        pad = ((pad // 2, -(-pad // 2)), (0, 0), (0, 0))
        sequence = np.pad(sequence, pad, mode='edge')
    
    sequence = sequence[:max_length]  # crop big sequences
    return sequence


def convert_data_file(data_path):
    subsequences = split_subsequences_from_data_file(data_path)
    subsequences = map(convert_subsequence_to_numpy, tqdm(subsequences))
    subsequences = list(subsequences)

    # pad/cut subsequences to the same length
    max_length = max(len(s) for _, s in subsequences)
    print('Fixed Subsequence Length:', max_length)
    sample_ids, subsequences = zip(*subsequences)
    subsequences = map(lambda x: normalize_sequence_length(x, max_length), subsequences)

    sample_ids = np.array(sample_ids)
    subsequences = np.stack(list(subsequences))
    return sample_ids, subsequences
    

class MoCapDataModule(pl.LightningDataModule):
    def __init__(
        self,
        data_path,
        train=None,
        valid=None,
        test=None,
        batch_size=8,
        seed=7,
        fps=12,
        shuffle_train=True,
        force=False
    ):
        super().__init__()
        self.data_path = Path(data_path)
        self.train = train
        self.valid = valid
        self.test = test
        self.cache_path = self.data_path.with_suffix('.npz')
        self.batch_size = batch_size
        self.fps = fps

        self.force = force
        self.rng = np.random.default_rng(seed)
        self.shuffle_train = shuffle_train

        self.save_hyperparameters(ignore=('force',))
    
    def prepare_data(self):
        if self.force or not self.cache_path.exists():
            # convert .data into .npy/pth
            sample_ids, subsequences = convert_data_file(self.data_path)
            np.savez_compressed(self.cache_path, sample_ids=sample_ids, subsequences=subsequences)

    def setup(self, stage=None):
        # load data
        data = np.load(self.cache_path)
        sample_ids, subsequences = data['sample_ids'], data['subsequences']

        # split train/val/test by sequence_id
        samples = pd.DataFrame({'id': sample_ids})
        samples = samples.id.str.rsplit('_', n=1, expand=True)
        samples.columns = ['sequence_id', 'subsequence_id']

        grouped = samples.groupby('sequence_id')

        if self.train is None:
            groups = np.array([x for x in grouped.groups])
            self.rng.shuffle(groups)

            n_sequences = len(groups)
            n_train = round(n_sequences * 0.70)
            n_valid = round(n_sequences * 0.15)
            n_test  = n_sequences - n_train - n_valid
            
            train_groups = groups[:n_train]
            valid_groups = groups[n_train:n_train + n_valid]
            test_groups  = groups[-n_test:]
        else:
            with open(self.train, 'r') as train_file:
                train_groups = list(map(str.rstrip, train_file))

            valid_groups = []
            if self.valid is not None:
                with open(self.valid, 'r') as valid_file:
                    valid_groups = list(map(str.rstrip, valid_file))

            test_groups = []
            if self.test is not None:
                with open(self.test , 'r') as  test_file:
                    test_groups  = list(map(str.rstrip,  test_file))


        train_idx = list(chain.from_iterable(grouped.get_group(x).index for x in train_groups))
        valid_idx = list(chain.from_iterable(grouped.get_group(x).index for x in valid_groups))
        test_idx  = list(chain.from_iterable(grouped.get_group(x).index for x in  test_groups))

        self.train_ids   = sample_ids[train_idx]
        self.valid_ids   = sample_ids[valid_idx]
        self.test_ids    = sample_ids[test_idx ]
        self.predict_ids = sample_ids

        self.train_dataset    = TensorDataset(torch.from_numpy(subsequences[train_idx]))
        self.valid_dataset    = TensorDataset(torch.from_numpy(subsequences[valid_idx]))
        self.test_dataset     = TensorDataset(torch.from_numpy(subsequences[ test_idx]))
        self.predict_dataset  = TensorDataset(torch.from_numpy(subsequences))

        print(  "train_dataset len:", len(self.train_dataset))
        print(  "valid_dataset len:", len(self.valid_dataset))
        print(   "test_dataset len:", len(self.test_dataset))
        print("predict_dataset len:", len(self.predict_dataset))

    def train_dataloader(self):
        return DataLoader(self.train_dataset, batch_size=self.batch_size, shuffle=self.shuffle_train, pin_memory=True, num_workers=4)

    def val_dataloader(self):
        return DataLoader(self.valid_dataset, batch_size=self.batch_size, pin_memory=True, num_workers=4)

    def test_dataloader(self):
        return DataLoader(self.test_dataset, batch_size=self.batch_size, pin_memory=True, num_workers=4)

    def predict_dataloader(self):
        return DataLoader(self.predict_dataset, batch_size=self.batch_size, pin_memory=True, num_workers=4)

    def teardown(self, stage=None):
        # Used to clean-up when the run is finished
        pass


if __name__ == "__main__":
    data_path = 'data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data'
    # data_path = 'data/class130-actions-segment40_shift20-coords_normPOS-fps12.data'
    train_split = 'data/hdm05/2version/splits/2folds20_80split_2-class122.txt' 
    test_split = 'data/hdm05/2version/splits/2folds20_80split_1-class122.txt'
    dm = MoCapDataModule(
        data_path,
        train=train_split,
        valid=test_split,
        test=test_split
    )
    dm.prepare_data()
    dm.setup()

    minX, minY, minZ = dm.predict_dataset.tensors[0].numpy().min(axis=(0, 1, 2))
    maxX, maxY, maxZ = dm.predict_dataset.tensors[0].numpy().max(axis=(0, 1, 2))

    print(f'xlim=({minX}, {maxX})')
    print(f'ylim=({minY}, {maxY})')
    print(f'zlim=({minZ}, {maxZ})')

    for x in dm.train_dataloader():
        breakpoint()
