#!/usr/bin/env python3
"""
dtw_matcher_lowmem.py (with visualization + percentile markers)

Low-memory DTW matcher (NPZ version):
- Computes DTW distance percentiles between motion sequences.
- Visualizes distance distribution with vertical percentile markers.
"""

import argparse
import numpy as np
import random
import json
from math import inf
from typing import List, Tuple, Dict

import matplotlib.pyplot as plt
import seaborn as sns

try:
    from joblib import Parallel, delayed
    _HAS_JOBLIB = True
except Exception:
    _HAS_JOBLIB = False


# ---------------------
# Data loading
# ---------------------
def load_data(path: str) -> Tuple[Dict[str, np.ndarray], List[str]]:
    """Load mocap sequences from .npz or .data file."""
    if path.endswith('.npz'):
        data = np.load(path, allow_pickle=True)
        ids = list(data['sample_ids'])
        seqs = data['subsequences']
        seq_dict = {}
        for i, key in enumerate(ids):
            seq = seqs[i]
            if not isinstance(seq, np.ndarray):
                seq = np.array(seq)
            seq_dict[key] = seq.astype(np.float32)
        return seq_dict, ids

    # .data text format (legacy)
    seq_dict, ids = {}, []
    with open(path, 'r', encoding='utf-8') as f:
        lines = [ln.rstrip() for ln in f]
    i = 0
    while i < len(lines):
        ln = lines[i].strip()
        if ln.startswith("#objectKey"):
            key = ln.split()[-1]
            i += 1
            while i < len(lines) and (lines[i].strip() == "" or lines[i].startswith("8;mcdr.objects")):
                i += 1
            pose_lines = []
            while i < len(lines) and not lines[i].startswith("#objectKey"):
                if lines[i].strip() and not lines[i].startswith("8;mcdr.objects"):
                    pose_lines.append(lines[i].strip())
                i += 1
            frames = []
            for pl in pose_lines:
                coords = []
                parts = [p.strip() for p in pl.split(';') if ',' in p]
                for p in parts:
                    nums = [float(x.strip()) for x in p.split(',') if x.strip()]
                    if len(nums) == 3:
                        coords.append(nums)
                if coords:
                    frames.append(np.array(coords).reshape(-1))
            if frames:
                seq_array = np.stack(frames, axis=0).astype(np.float32)
                seq_dict[key] = seq_array
                ids.append(key)
        else:
            i += 1
    return seq_dict, ids


def load_train_ids(trainset_path: str) -> set:
    """Load train IDs from text file."""
    with open(trainset_path, 'r', encoding='utf-8') as f:
        return {line.strip() for line in f if line.strip()}


# ---------------------
# DTW helpers
# ---------------------
def frame_distance(a: np.ndarray, b: np.ndarray, metric: str = 'euclidean') -> float:
    if metric == 'euclidean':
        # Reshape flattened vectors into (num_joints, 3)
        a = a.reshape(-1, 3)
        b = b.reshape(-1, 3)
        # Compute per-joint Euclidean distances and sum them
        return float(np.sum(np.linalg.norm(a - b, axis=1)))
    elif metric == 'cosine':
        na, nb = np.linalg.norm(a), np.linalg.norm(b)
        if na == 0 or nb == 0:
            return 0.0 if na == 0 and nb == 0 else 1.0
        cos_sim = np.dot(a, b) / (na * nb)
        return float(1.0 - np.clip(cos_sim, -1.0, 1.0))
    raise ValueError("Unsupported metric")


def dtw_distance(seqA: np.ndarray, seqB: np.ndarray, metric: str = 'euclidean', normalize: bool = True) -> float:
    T1, T2 = seqA.shape[0], seqB.shape[0]
    dp = np.full((T1 + 1, T2 + 1), inf, dtype=np.float64)
    dp[0, 0] = 0.0
    local = np.zeros((T1, T2), dtype=np.float64)
    for i in range(T1):
        for j in range(T2):
            local[i, j] = frame_distance(seqA[i], seqB[j], metric)
    for i in range(1, T1 + 1):
        for j in range(1, T2 + 1):
            dp[i, j] = local[i - 1, j - 1] + min(dp[i - 1, j], dp[i, j - 1], dp[i - 1, j - 1])
    total_cost = dp[T1, T2]
    if not normalize:
        return float(total_cost)
    # return total_cost

    i, j, path_len = T1, T2, 0
    while i > 0 or j > 0:
        path_len += 1
        choices = []
        if i > 0 and j > 0:
            choices.append((dp[i - 1, j - 1], i - 1, j - 1))
        if i > 0:
            choices.append((dp[i - 1, j], i - 1, j))
        if j > 0:
            choices.append((dp[i, j - 1], i, j - 1))
        i, j = min(choices, key=lambda x: x[0])[1:]
    return float(total_cost / path_len if path_len else total_cost)


# ---------------------
# Core percentile estimation
# ---------------------
def compute_subset_percentiles(
    seqs: Dict[str, np.ndarray],
    subset_keys: List[str],
    metric: str,
    n_jobs: int = 1,
    return_all: bool = False
) -> Tuple[float, float, np.ndarray]:
    subset = {k: seqs[k] for k in subset_keys if k in seqs}
    keys = list(subset.keys())
    pairs = [(keys[i], keys[j]) for i in range(len(keys)) for j in range(i + 1, len(keys))]

    if n_jobs != 1 and _HAS_JOBLIB:
        dists = Parallel(n_jobs=n_jobs)(
            delayed(dtw_distance)(subset[a], subset[b], metric=metric, normalize=True)
            for a, b in pairs
        )
    else:
        dists = [dtw_distance(subset[a], subset[b], metric=metric, normalize=True) for a, b in pairs]

    dvals = np.array(dists, dtype=np.float64)
    p0_5, p40 = np.percentile(dvals, [0.5, 40])
    # p0_5, p40 = np.percentile(dvals, [2, 60])
    if return_all:
        return p0_5, p40, dvals
    return p0_5, p40, None


def estimate_thresholds(seqs, all_keys, subset_size, repeats, metric, n_jobs=1):
    subset_size = min(subset_size, len(all_keys))
    p0_5_list, p40_list = [], []

    for r in range(repeats):
        subset_keys = random.sample(all_keys, subset_size)
        print(f"[Subset {r+1}/{repeats}] {len(subset_keys)} keys (sample): {subset_keys[:3]} ...")
        p0_5, p40, _ = compute_subset_percentiles(seqs, subset_keys, metric, n_jobs)
        p0_5_list.append(p0_5)
        p40_list.append(p40)
    return float(np.mean(p0_5_list)), float(np.mean(p40_list))


# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Low-memory DTW matcher (NPZ version) with visualization.")
    parser.add_argument('--input', required=True, help='Path to input .npz file.')
    parser.add_argument('--metric', choices=['euclidean', 'cosine'], default='euclidean', help='Distance metric.')
    parser.add_argument('--subset-size', type=int, default=1000, help='Subset size.')
    parser.add_argument('--subset-repeats', type=int, default=1, help='Number of subsets.')
    parser.add_argument('--n-jobs', type=int, default=1, help='Parallel jobs.')
    parser.add_argument('--seed', type=int, default=42, help='Random seed.')
    parser.add_argument('--trainset', type=str, default=None, help='Optional train ID list.')
    parser.add_argument('--output', type=str, default=None, help='Path to save percentile JSON.')
    parser.add_argument('--plot-output', type=str, help='Path to save plot.')
    parser.add_argument('--show-plot', action='store_true', help='Show plot interactively.')
    args = parser.parse_args()

    random.seed(args.seed)
    np.random.seed(args.seed)

    print(f"Loading data from {args.input} ...")
    seqs, all_keys = load_data(args.input)
    print(f"Loaded {len(all_keys)} sequences.")

    if args.trainset:
        train_ids = load_train_ids(args.trainset)
        all_keys = [k for k in all_keys if any(k.startswith(tid + "_") or k == tid for tid in train_ids)]
        print(f"Filtered: {len(all_keys)} remain after trainset filtering.")
        if not all_keys:
            print("No sequences remain. Exiting.")
            return

    print(f"Estimating percentiles with subset size {args.subset_size} × {args.subset_repeats} repeats...")
    p0_5, p40 = estimate_thresholds(seqs, all_keys, args.subset_size, args.subset_repeats, args.metric, args.n_jobs)
    print(f"Final thresholds: p0.5 = {p0_5:.6g}, p40 = {p40:.6g}")

    # Visualization
    subset_keys = random.sample(all_keys, min(args.subset_size, len(all_keys)))
    _, _, dvals = compute_subset_percentiles(seqs, subset_keys, args.metric, args.n_jobs, return_all=True)

    sns.set(style="darkgrid")
    plt.figure(figsize=(8, 5))
    sns.histplot(dvals, bins=50, kde=True, color="royalblue")
    plt.axvline(p0_5, color='red', linestyle='--', label=f'0.5th percentile ({p0_5:.3f})')
    plt.axvline(p40, color='orange', linestyle='--', label=f'40th percentile ({p40:.3f})')
    plt.xlabel(f"DTW Distance ({args.metric})")
    plt.ylabel("Frequency")
    plt.title("Distribution of Pairwise DTW Distances")
    plt.legend()
    plt.tight_layout()

    if args.plot_output:
        plt.savefig(args.plot_output, dpi=150)
        print(f"Saved plot to {args.plot_output}")
    if args.show_plot or not args.plot_output:
        plt.show()

    if args.output:
        result = {"p0.5": p0_5, "p40": p40}
        with open(args.output, "w") as f:
            json.dump(result, f, indent=2)
        print(f"Saved percentiles to {args.output}")

    print("Done.")


if __name__ == '__main__':
    main()
