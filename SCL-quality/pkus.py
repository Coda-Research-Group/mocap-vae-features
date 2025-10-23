#!/usr/bin/env python3
import argparse
import gzip
import json
import numpy as np
from scipy.spatial.distance import cosine
from itertools import combinations
from tqdm import tqdm
import random
from math import inf
import os

# ---------------------
# Load thresholds
# ---------------------
def load_thresholds(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return data["p0.5"], data["p40"]

# ---------------------
# Load PKU-MMD skeletons
# ---------------------
def load_pku_mmd_skeleton_objects(file_path, valid_ids=None, max_objects=None, seed=None):
    if seed is not None:
        random.seed(seed)
        np.random.seed(seed)

    objects = []
    current_id = None
    frame_lines = []

    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if line.startswith("#objectKey"):
                if current_id is not None and frame_lines:
                    obj_array = np.stack(frame_lines)
                    if valid_ids is None or current_id in valid_ids:
                        objects.append(obj_array)
                    frame_lines = []
                    if max_objects is not None and len(objects) >= max_objects:
                        break
                current_id = "_".join(line.split()[-1].split("_")[:-1])
            else:
                if ";" in line and any(c.isalpha() for c in line.split(";")[1]):
                    continue
                parts = [p.strip() for p in line.replace(";", ",").split(",") if p.strip()]
                if not parts:
                    continue
                try:
                    xyz = np.array([float(x) for x in parts], dtype=np.float32)
                    frame_lines.append(xyz)
                except ValueError:
                    continue

        if current_id is not None and frame_lines:
            obj_array = np.stack(frame_lines)
            if valid_ids is None or current_id in valid_ids:
                objects.append(obj_array)

    if not objects:
        raise RuntimeError(f"No skeleton objects loaded from {file_path}")
    return objects

# ---------------------
# Load postprocessed SCL
# ---------------------
# ---------------------
# Load HDM05 skeletons (skip sequences not exactly 8 frames)
# ---------------------
def load_hdm05_skeleton_objects(file_path, valid_ids=None, max_objects=None):
    objects = []
    current_id = None
    frame_lines = []

    def finalize_object():
        if len(frame_lines) != 8:
            return  # Skip sequences not exactly 8 frames
        obj_array = np.stack(frame_lines)
        if valid_ids is None or current_id in valid_ids:
            objects.append(obj_array)

    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if line.startswith("#objectKey"):
                if current_id is not None:
                    finalize_object()
                current_id = "_".join(line.split()[-1].split("_")[:-1])
                frame_lines = []
            else:
                if current_id is None:
                    continue
                if ";" in line and any(c.isalpha() for c in line.split(";")[1]):
                    continue
                frames = line.split(";")
                frame_arrays = []
                for fr in frames:
                    fr = fr.strip()
                    if fr:
                        xyz = np.array([float(x) for x in fr.split(",")], dtype=np.float32)
                        frame_arrays.append(xyz)
                frame_lines.append(frame_arrays)

        if current_id is not None:
            finalize_object()

    if not objects:
        raise RuntimeError(f"No skeleton objects loaded from {file_path}")
    return objects


# ---------------------
# Load postprocessed SCL from .data.gz
# ---------------------
def load_postprocessed_objects(file_path, valid_ids=None, max_objects=None):
    objects = []
    current_id = None
    opener = gzip.open if file_path.endswith(".gz") else open

    with opener(file_path, "rt", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if line.startswith("#objectKey"):
                current_id = "_".join(line.split()[-1].split("_")[:-1])
            else:
                if current_id is None:
                    continue
                if valid_ids is None or current_id in valid_ids:
                    vector = np.array([float(x) for x in line.split(",")], dtype=np.float32)
                    objects.append(vector)
                current_id = None
                if max_objects is not None and len(objects) >= max_objects:
                    break

    if not objects:
        raise RuntimeError(f"No postprocessed objects loaded from {file_path}")
    return objects


# ---------------------
# DTW distance
# ---------------------
def frame_distance(a, b):
    # Reshape flattened vectors into (num_joints, 3)
    a = a.reshape(-1, 3)
    b = b.reshape(-1, 3)
    # Compute per-joint Euclidean distances and sum them
    return float(np.sum(np.linalg.norm(a - b, axis=1)))

def dtw_distance(seqA, seqB):
    T1, T2 = seqA.shape[0], seqB.shape[0]
    dp = np.full((T1 + 1, T2 + 1), inf, dtype=np.float64)
    dp[0, 0] = 0.0
    local = np.zeros((T1, T2), dtype=np.float64)
    for i in range(T1):
        for j in range(T2):
            local[i, j] = frame_distance(seqA[i], seqB[j])
    for i in range(1, T1 + 1):
        for j in range(1, T2 + 1):
            dp[i, j] = local[i - 1, j - 1] + min(dp[i - 1, j], dp[i, j - 1], dp[i - 1, j - 1])
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
    return float(dp[T1, T2] / path_len if path_len else dp[T1, T2])

# ---------------------
# Evaluation
# ---------------------
def evaluate_pair(i, j, skeletons, scl_vectors, skel_thresh, scl_thresh):
    p0_5_skel, p40_skel = skel_thresh
    p0_5_scl, p40_scl = scl_thresh

    d_skel = dtw_distance(skeletons[i], skeletons[j])
    if d_skel < p0_5_skel:
        label_skel = True
    elif d_skel > p40_skel:
        label_skel = False
    else:
        return (0, 0, 0, 0, 0, 0)

    d_scl = cosine(scl_vectors[i], scl_vectors[j])
    if d_scl < p0_5_scl:
        label_scl = True
    elif d_scl > p40_scl:
        label_scl = False
    else:
        if label_skel:
            return (0, 0, 0, 0, 1, 0)
        else: 
            return (0, 0, 0, 0, 0, 1)
    # print(d_skel, d_scl)

    TP = FP = FN = TN = 0
    if label_skel and label_scl:
        TP = 1
    elif label_skel and not label_scl:
        FN = 1
    elif not label_skel and label_scl:
        FP = 1
    else:
        TN = 1
    return TP, FP, FN, TN, 0, 0

# ---------------------
# Compute metrics on a single subset
# ---------------------
def compute_metrics_for_subset(indices, skeletons, scl_vectors, skel_thresh, scl_thresh):
    TP = FP = FN = TN = g = o = 0
    pairs = list(combinations(indices, 2))
    
    for i, j in tqdm(pairs, desc="Processing pairs", leave=False):
        r = evaluate_pair(i, j, skeletons, scl_vectors, skel_thresh, scl_thresh)
        TP += r[0]
        FP += r[1]
        FN += r[2]
        TN += r[3]
        g += r[4]
        o += r[5]

    precision = TP / (TP + FP) if (TP + FP) > 0 else 0.0
    recall = TP / (TP + FN) if (TP + FN) > 0 else 0.0
    F1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    F025 = (1 + 0.25 ** 2) * precision * recall / ((0.25 ** 2) * precision + recall) if (precision + recall) > 0 else 0.0
    accuracy = (TP + TN) / (TP + TN + FP + FN) if (TP + TN + FP + FN) > 0 else 0.0
    
    return {
        'precision': precision,
        'recall': recall,
        'F025': F025,
        'F1': F1,
        'accuracy': accuracy,
        'TP': TP,
        'TN': TN,
        'FP': FP,
        'FN': FN,
        'gray': g,
        'o': o
    }

# ---------------------
# Compute metrics on multiple random subsets
# ---------------------
def compute_metrics_on_subsets(skeletons, scl_vectors, skel_thresh, scl_thresh, 
                                n_subsets=5, subset_size=1000, seed=42):
    random.seed(seed)
    np.random.seed(seed)
    
    n_objects = len(skeletons)
    print(f"Total objects: {n_objects}")
    print(f"Computing metrics on {n_subsets} random subsets of size {subset_size}")
    print()
    
    results = []
    
    for subset_idx in range(n_subsets):
        print(f"=== Subset {subset_idx + 1}/{n_subsets} ===")
        
        # Randomly sample indices (with replacement for potential overlap)
        indices = random.sample(range(n_objects), min(subset_size, n_objects))
        
        # Compute metrics for this subset
        metrics = compute_metrics_for_subset(indices, skeletons, scl_vectors, skel_thresh, scl_thresh)
        results.append(metrics)
        
        print(f"TP: {metrics['TP']}, TN: {metrics['TN']}, FP: {metrics['FP']}, FN: {metrics['FN']}, Gray: {metrics['gray']}, true-gray: {metrics['o']}")
        print(f"Precision: {metrics['precision']:.6f}")
        print(f"Recall:    {metrics['recall']:.6f}")
        print(f"F0.25:     {metrics['F025']:.6f}")
        print(f"F1:        {metrics['F1']:.6f}")
        print(f"Accuracy:  {metrics['accuracy']:.6f}")
        print()
    
    # Aggregate results
    print("=" * 50)
    print("AGGREGATED RESULTS (Mean ± Std)")
    print("=" * 50)
    
    metrics_names = ['precision', 'recall', 'F025', 'F1', 'accuracy']
    aggregated = {}
    
    for metric in metrics_names:
        values = [r[metric] for r in results]
        mean_val = np.mean(values)
        std_val = np.std(values)
        aggregated[metric] = (mean_val, std_val)
        print(f"{metric.capitalize():12s}: {mean_val:.6f} ± {std_val:.6f}")
    
    return results, aggregated

# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Compare skeleton vs postprocessed objects for HDM05 or PKU-MMD with subset sampling.")
    parser.add_argument("skeleton_file")
    parser.add_argument("scl_file")
    parser.add_argument("skeleton_thresh")
    parser.add_argument("scl_thresh")
    parser.add_argument("--dataset", choices=["hdm05", "pku-mmd"], required=True)
    parser.add_argument("--max-objects", type=int, default=None, help="Max objects to load from files")
    parser.add_argument("--n-subsets", type=int, default=5, help="Number of random subsets to evaluate")
    parser.add_argument("--subset-size", type=int, default=1000, help="Size of each subset")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility")
    args = parser.parse_args()

    skel_thresh = load_thresholds(args.skeleton_thresh)
    scl_thresh = load_thresholds(args.scl_thresh)

    if args.dataset == "hdm05":
        skeletons = load_hdm05_skeleton_objects(
            args.skeleton_file, max_objects=args.max_objects
        )
    else:
        skeletons = load_pku_mmd_skeleton_objects(args.skeleton_file, max_objects=args.max_objects)

    scl_vectors = load_postprocessed_objects(args.scl_file, max_objects=args.max_objects)

    # if len(skeletons) != len(scl_vectors):
    #     raise ValueError(f"Mismatch: {len(skeletons)} skeletons vs {len(scl_vectors)} SCL vectors")

    results, aggregated = compute_metrics_on_subsets(
        skeletons, scl_vectors, skel_thresh, scl_thresh,
        n_subsets=args.n_subsets, subset_size=args.subset_size, seed=args.seed
    )

if __name__ == "__main__":
    main()