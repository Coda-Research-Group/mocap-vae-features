#!/usr/bin/env python3
import argparse
import gzip
import json
import numpy as np
from scipy.spatial.distance import cosine
from itertools import combinations
import random
from math import inf
import os
from joblib import Parallel, delayed

# ---------------------
# Load thresholds
# ---------------------
def load_thresholds(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return data["p0.5"], data["p40"]

# ---------------------
# Unified skeleton loader
# ---------------------
import numpy as np
import gzip
from typing import List, Tuple, Dict # Added imports for type hints

def load_skeleton_objects(file_path, valid_ids=None, max_objects=None) -> Tuple[List[np.ndarray], List[str]]:
    """
    Unified loader for HDM05, PKU-MMD, NPZ. The text-based (.data/.data.gz) 
    format is revised to load sequences of ALL lengths and use the full ID 
    (mimicking the load_data function).
    """
    # NPZ format (Retained original Code 2 NPZ logic, which filters for 8 frames)
    if file_path.endswith(".npz"):
        data = np.load(file_path, allow_pickle=True)
        ids = list(data['sample_ids'])
        seqs = data['subsequences']
        objects, loaded_ids = [], []
        for i, key in enumerate(ids):
            seq = np.array(seqs[i], dtype=np.float32)
            if seq.shape[0] != 8:
                continue
            if valid_ids is None or key in valid_ids:
                objects.append(seq)
                loaded_ids.append(key)
            if max_objects and len(objects) >= max_objects:
                break
        if not objects:
            raise RuntimeError(f"No sequences loaded from {file_path}")
        return objects, loaded_ids

    # ----------------------------------------------------------------------
    # Text-based format (.data or .data.gz) - REVISED to match load_data
    # ----------------------------------------------------------------------
    opener = gzip.open if file_path.endswith(".gz") else open
    
    # We use lists (objects, loaded_ids) to conform to the function's return type
    objects, loaded_ids = [], []
    
    # Load all lines into memory first, mimicking the load_data style for easier parsing
    try:
        with opener(file_path, "rt", encoding="utf-8") as f:
            lines = [ln.rstrip() for ln in f]
    except Exception as e:
        raise RuntimeError(f"Error reading file {file_path}: {e}")

    i = 0
    while i < len(lines):
        ln = lines[i].strip()
        
        if ln.startswith("#objectKey"):
            # FIX 1: Use the full segmented ID (last token, index -1)
            key = ln.split()[-1]
            i += 1
            
            # Skip blank lines and metadata lines (8;mcdr.objects)
            while i < len(lines) and (lines[i].strip() == "" or lines[i].startswith("8;mcdr.objects")):
                i += 1
                
            pose_lines = []
            # Collect all lines until the next objectKey
            while i < len(lines) and not lines[i].startswith("#objectKey"):
                if lines[i].strip() and not lines[i].startswith("8;mcdr.objects"):
                    pose_lines.append(lines[i].strip())
                i += 1
                
            frames = []
            # Parse the pose lines collected
            for pl in pose_lines:
                coords = []
                # Split by semicolon, only keeping parts with coordinate data (contain a comma)
                parts = [p.strip() for p in pl.split(';') if ',' in p]
                for p in parts:
                    # Split by comma and convert to float (3 numbers per joint)
                    nums = [float(x.strip()) for x in p.split(',') if x.strip()]
                    if len(nums) % 3 == 0 and len(nums) > 0:
                        coords.append(np.array(nums).reshape(-1))
                
                # If valid coordinates were found for the frame, concatenate them
                if coords:
                    frames.append(np.concatenate(coords))
            
            # FIX 2: Load the sequence if it has any frames (no 8-frame filter)
            if frames:
                seq_array = np.stack(frames, axis=0).astype(np.float32)
                
                # FIX 3: Check against valid_ids and max_objects using the full ID
                if valid_ids is None or key in valid_ids:
                    objects.append(seq_array)
                    loaded_ids.append(key)
                    
                if max_objects and len(objects) >= max_objects:
                    break
        else:
            i += 1
            
    if not objects:
        raise RuntimeError(f"No sequences loaded from {file_path}")
        
    return objects, loaded_ids
    
# ---------------------
# Postprocessed SCL loader
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
                    try:
                        vector = np.array([float(x) for x in line.split(",")], dtype=np.float32)
                        objects.append(vector)
                    except ValueError:
                        continue
                current_id = None
                if max_objects and len(objects) >= max_objects:
                    break
    if not objects:
        raise RuntimeError(f"No postprocessed objects loaded from {file_path}")
    return objects

# ---------------------
# DTW distance
# ---------------------
def frame_distance(a, b):
    a = a.reshape(-1, 3)
    b = b.reshape(-1, 3)
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
        return (0, 0, 0, 0, 1, 0)

    d_scl = cosine(scl_vectors[i], scl_vectors[j])
    if d_scl < p0_5_scl:
        label_scl = True
    elif d_scl > p40_scl:
        label_scl = False
    else:
        return (0, 0, 0, 0, 0, 1)

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
# Metrics computation
# ---------------------
def compute_metrics_for_subset(indices, skeletons, scl_vectors, skel_thresh, scl_thresh, subset_idx=None):
    TP = FP = FN = TN = g = o = 0
    pairs = list(combinations(indices, 2))
    
    for i, j in pairs:
        r = evaluate_pair(i, j, skeletons, scl_vectors, skel_thresh, scl_thresh)
        TP += r[0]; FP += r[1]; FN += r[2]; TN += r[3]; g += r[4]; o += r[5]

    precision = TP / (TP + FP) if (TP + FP) > 0 else 0.0
    recall = TP / (TP + FN) if (TP + FN) > 0 else 0.0
    F1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    F025 = (1 + 0.25 ** 2) * precision * recall / ((0.25 ** 2) * precision + recall) if (precision + recall) > 0 else 0.0
    accuracy = (TP + TN) / (TP + TN + FP + FN) if (TP + TN + FP + FN) > 0 else 0.0
    return {'precision': precision, 'recall': recall, 'F025': F025, 'F1': F1,
            'accuracy': accuracy, 'TP': TP, 'TN': TN, 'FP': FP, 'FN': FN, 'gray': g, 'o': o}

def compute_metrics_on_subsets(skeletons, scl_vectors, skel_thresh, scl_thresh,
                               n_subsets=5, subset_size=1000, seed=42, n_jobs=-1):
    random.seed(seed); np.random.seed(seed)
    n_objects = len(skeletons)
    print(f"Total objects: {n_objects}")
    print(f"Processing {n_subsets} subsets in parallel using {n_jobs} cores...")
    
    # Generate all subset indices upfront
    subset_indices = []
    for subset_idx in range(n_subsets):
        indices = random.sample(range(n_objects), min(subset_size, n_objects))
        subset_indices.append((subset_idx, indices))
    
    # Process subsets in parallel
    results = Parallel(n_jobs=n_jobs, verbose=0)(
        delayed(compute_metrics_for_subset)(
            indices, skeletons, scl_vectors, skel_thresh, scl_thresh, subset_idx
        )
        for subset_idx, indices in subset_indices
    )
    
    # Print individual results
    for subset_idx, metrics in enumerate(results):
        print(f"\n=== Subset {subset_idx + 1}/{n_subsets} ===")
        print(f"TP: {metrics['TP']}, TN: {metrics['TN']}, FP: {metrics['FP']}, FN: {metrics['FN']}, out_skel: {metrics['gray']}, out_scl: {metrics['o']}")
        print(f"Precision: {metrics['precision']:.6f}, Recall: {metrics['recall']:.6f}, F0.25: {metrics['F025']:.6f}, F1: {metrics['F1']:.6f}, Accuracy: {metrics['accuracy']:.6f}")
    
    # Aggregate
    print("\n" + "="*50)
    print("AGGREGATED RESULTS (Mean ± Std)")
    for metric in ['precision', 'recall', 'F025', 'F1', 'accuracy']:
        vals = [r[metric] for r in results]
        print(f"{metric.capitalize():10s}: {np.mean(vals):.6f} ± {np.std(vals):.6f}")
    return results

# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Compare skeleton vs postprocessed objects (HDM05/PKU-MMD) using DTW.")
    parser.add_argument("skeleton_file")
    parser.add_argument("scl_file")
    parser.add_argument("skeleton_thresh")
    parser.add_argument("scl_thresh")
    parser.add_argument("--dataset", choices=["hdm05","pku-mmd"], required=True)
    parser.add_argument("--max-objects", type=int, default=None)
    parser.add_argument("--n-subsets", type=int, default=5)
    parser.add_argument("--subset-size", type=int, default=1000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--n-jobs", type=int, default=-1, 
                       help="Number of parallel jobs (-1 uses all cores)")
    args = parser.parse_args()

    skel_thresh = load_thresholds(args.skeleton_thresh)
    scl_thresh = load_thresholds(args.scl_thresh)

    skeletons, _ = load_skeleton_objects(args.skeleton_file, max_objects=args.max_objects)
    scl_vectors = load_postprocessed_objects(args.scl_file, max_objects=args.max_objects)

    if len(skeletons) != len(scl_vectors):
        print(f"Warning: {len(skeletons)} skeletons vs {len(scl_vectors)} SCL vectors")
        raise RuntimeError(f"Lengths must be the same so the indexes in the cycle are equal.")

    compute_metrics_on_subsets(skeletons, scl_vectors, skel_thresh, scl_thresh,
                               n_subsets=args.n_subsets, subset_size=args.subset_size, 
                               seed=args.seed, n_jobs=args.n_jobs)

if __name__ == "__main__":
    main()