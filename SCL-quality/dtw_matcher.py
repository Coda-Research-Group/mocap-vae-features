#!/usr/bin/env python3
"""
dtw_matcher_lowmem.py

Low-memory DTW matcher:
- Loads only subsets of mocap objects at a time to compute approximate percentiles.
- Supports variable-length sequences (not just 8 poses).
- Computes 0.5th and 40th percentile thresholds by averaging over multiple random subsets.

Usage:
  python dtw_matcher_lowmem.py --input data.txt --metric cosine --subset-size 200 --out results.csv
"""

import argparse
import numpy as np
import re
import random
from math import inf
from typing import List, Tuple, Dict

try:
    from joblib import Parallel, delayed
    _HAS_JOBLIB = True
except Exception:
    _HAS_JOBLIB = False


# ---------------------
# Parsing helpers
# ---------------------
def parse_object_keys(path: str) -> List[str]:
    """Quickly scan file and return list of all object keys."""
    keys = []
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            if line.startswith("#objectKey"):
                parts = line.split()
                if parts:
                    keys.append(parts[-1])
    return keys


def load_objects_subset(path: str, subset_keys: set) -> Dict[str, List[str]]:
    """
    Load only selected objects (subset_keys) from the file.
    Allows variable-length sequences (≥2 poses).
    """
    objects = {}
    with open(path, 'r', encoding='utf-8') as f:
        lines = [ln.rstrip('\n') for ln in f]

    i = 0
    while i < len(lines):
        ln = lines[i].strip()
        if ln.startswith("#objectKey"):
            parts = ln.split()
            key = parts[-1]
            i += 1

            # Skip metadata lines or blank lines
            while i < len(lines) and (
                lines[i].strip() == "" or
                re.match(r'^\s*\d+\s*;mcdr\.objects', lines[i])
            ):
                i += 1

            pose_lines = []
            # Collect pose lines until next object or EOF
            while i < len(lines) and not lines[i].strip().startswith("#objectKey"):
                line = lines[i].strip()
                if not line or re.match(r'^\s*\d+\s*;mcdr\.objects', line):
                    i += 1
                    continue
                pose_lines.append(line)
                i += 1

            if key in subset_keys and len(pose_lines) >= 2:
                objects[key] = pose_lines
        else:
            i += 1
    return objects


def parse_pose_line_to_array(pose_line: str) -> np.ndarray:
    """
    Convert one pose line to an array of 3D joint coordinates.
    """
    parts = [p.strip() for p in pose_line.split(';') if ',' in p]
    coords = []
    for p in parts:
        nums = [float(x.strip()) for x in p.split(',') if x.strip()]
        if len(nums) != 3:
            continue  # skip malformed entries
        coords.append(nums)
    if not coords:
        raise ValueError(f"Pose line contains no valid coordinates: '{pose_line}'")
    return np.array(coords, dtype=np.float32)


def object_to_sequence(pose_lines: List[str]) -> np.ndarray:
    frames = [parse_pose_line_to_array(pl) for pl in pose_lines]
    J = frames[0].shape[0]
    for f in frames:
        if f.shape[0] != J:
            raise ValueError("Inconsistent joint counts inside object")
    return np.stack([f.reshape(-1) for f in frames], axis=0)


# ---------------------
# DTW helpers
# ---------------------
def frame_distance(a: np.ndarray, b: np.ndarray, metric: str = 'euclidean') -> float:
    if metric == 'euclidean':
        return float(np.linalg.norm(a - b))
    elif metric == 'cosine':
        na, nb = np.linalg.norm(a), np.linalg.norm(b)
        if na == 0 or nb == 0:
            return 0.0 if na == 0 and nb == 0 else 1.0
        cos_sim = np.dot(a, b) / (na * nb)
        cos_sim = np.clip(cos_sim, -1.0, 1.0)
        return float(1.0 - cos_sim)
    else:
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
def compute_subset_percentiles(path: str, subset_keys: List[str], metric: str, n_jobs: int = 1) -> Tuple[float, float]:
    """Load subset, compute pairwise DTW distances, return (p0.5, p40)."""
    objs = load_objects_subset(path, set(subset_keys))
    seqs = {}
    for k, v in objs.items():
        try:
            seqs[k] = object_to_sequence(v)
        except Exception as e:
            print(f"Skipping {k}: {e}")
    if len(seqs) < 2:
        raise RuntimeError("Too few valid sequences in subset.")

    keys = list(seqs.keys())
    pairs = [(keys[i], keys[j]) for i in range(len(keys)) for j in range(i + 1, len(keys))]

    if n_jobs != 1 and _HAS_JOBLIB:
        dists = Parallel(n_jobs=n_jobs)(
            delayed(dtw_distance)(seqs[a], seqs[b], metric=metric, normalize=True)
            for a, b in pairs
        )
    else:
        dists = [dtw_distance(seqs[a], seqs[b], metric=metric, normalize=True) for a, b in pairs]

    dvals = np.array(dists, dtype=np.float64)
    return np.percentile(dvals, 0.5), np.percentile(dvals, 40.0)


def estimate_thresholds_lowmem(path: str, all_keys: List[str], subset_size: int, repeats: int, metric: str, n_jobs: int = 1):
    """Compute percentile thresholds using multiple random subsets."""
    subset_size = min(subset_size, len(all_keys))
    p0_5_list, p40_list = [], []

    for r in range(repeats):
        subset_keys = random.sample(all_keys, subset_size)
        print(f"[Subset {r+1}/{repeats}] Computing percentiles for {len(subset_keys)} objects ...")
        p0_5, p40 = compute_subset_percentiles(path, subset_keys, metric, n_jobs)
        p0_5_list.append(p0_5)
        p40_list.append(p40)
        import gc
        gc.collect()

    return float(np.mean(p0_5_list)), float(np.mean(p40_list))


# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Low-memory DTW matcher with percentile approximation (variable-length support).")
    parser.add_argument('--input', required=True, help='Path to input text file with mocap objects.')
    parser.add_argument('--metric', choices=['euclidean', 'cosine'], default='euclidean', help='Distance metric.')
    parser.add_argument('--subset-size', type=int, default=1000, help='Subset size for each percentile estimate.')
    parser.add_argument('--subset-repeats', type=int, default=1, help='Number of random subsets.')
    parser.add_argument('--n-jobs', type=int, default=1, help='Parallel jobs (requires joblib).')
    args = parser.parse_args()

    print("Scanning file for object keys ...")
    all_keys = parse_object_keys(args.input)
    print(f"Found {len(all_keys)} objects in total.")

    print(f"Estimating percentile thresholds using {args.subset_repeats} subsets of size {args.subset_size} ...")
    p0_5, p40 = estimate_thresholds_lowmem(args.input, all_keys, args.subset_size, args.subset_repeats, args.metric, args.n_jobs)

    print(f"Final averaged thresholds: p0.5 = {p0_5:.6g}, p40 = {p40:.6g}")
    print("Done.")


if __name__ == '__main__':
    main()

