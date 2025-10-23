#!/usr/bin/env python3
import argparse
import gzip
import json
import numpy as np
from itertools import combinations
from tqdm import tqdm
import random
from math import inf
from scipy.spatial.distance import cosine

# ---------------------
# DTW helpers
# ---------------------
def frame_distance(a: np.ndarray, b: np.ndarray) -> float:
    """Euclidean distance between frames"""
    return float(np.linalg.norm(a - b))

def dtw_distance(seqA: np.ndarray, seqB: np.ndarray, normalize: bool = True) -> float:
    """
    Compute DTW distance between two sequences of shape (T, n_joints*3)
    seqA, seqB: np.ndarray of shape (T, n_features)
    """
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
    total_cost = dp[T1, T2]
    if not normalize:
        return float(total_cost)
    # normalize by warping path length
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
# Thresholds
# ---------------------
def load_thresholds(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return data["p0.5"], data["p40"]


# ---------------------
# Load skeletons
# ---------------------
def load_skeleton_objects(file_path, valid_ids=None, max_objects=None, seed=None):
    """Load skeleton objects from multi-frame .data file (8 x n_joints x 3)"""
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
                current_id = "_".join(line.split()[-1].split("_")[:-1])
                frame_lines = []
            elif current_id is not None:
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
                if len(frame_lines) == 8:
                    obj_array = np.stack(frame_lines)  # 8 x n_joints x 3
                    if valid_ids is None or current_id in valid_ids:
                        objects.append(obj_array)
                    current_id = None
                    frame_lines = []
                    if max_objects is not None and len(objects) >= max_objects:
                        break
    if not objects:
        raise RuntimeError(f"No skeleton objects loaded from {file_path}")
    return objects


# ---------------------
# Load postprocessed SCL vectors
# ---------------------
def load_postprocessed_objects(file_path, valid_ids=None, max_objects=None, seed=None):
    if seed is not None:
        random.seed(seed)
        np.random.seed(seed)

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
                prefix = current_id
                if valid_ids is None or prefix in valid_ids:
                    vector = np.array([float(x) for x in line.split(",")], dtype=np.float32)
                    objects.append(vector)
                current_id = None
                if max_objects is not None and len(objects) >= max_objects:
                    break
    if not objects:
        raise RuntimeError(f"No postprocessed objects loaded from {file_path}")
    return objects


# ---------------------
# Metrics
# ---------------------
def compute_metrics(skeletons, scl_vectors, skeleton_thresh, scl_thresh):
    p0_5_skel, p40_skel = skeleton_thresh
    p0_5_scl, p40_scl = scl_thresh

    TP = FP = FN = TN = 0
    g = 0
    o = 0

    # Iterate over unique pairs
    for i, j in tqdm(combinations(range(len(skeletons)), 2), total=len(skeletons)*(len(skeletons)-1)//2):
        # Compute DTW+Euclidean distance
        seqA = skeletons[i].reshape(8, -1)
        seqB = skeletons[j].reshape(8, -1)
        d_skel = dtw_distance(seqA, seqB)

        if d_skel < p0_5_skel:
            label_skel = True
        elif d_skel > p40_skel:
            label_skel = False
        else:
            o += 1
            continue  # gray zone

        # Postprocessed distance
        d_scl = cosine(scl_vectors[i], scl_vectors[j])
        if d_scl < p0_5_scl:
            label_scl = True
        elif d_scl > p40_scl:
            label_scl = False
        else:
            g += 1
            continue  # gray zone

        # Confusion matrix
        if label_skel and label_scl:
            TP += 1
        elif label_skel and not label_scl:
            FN += 1
        elif not label_skel and label_scl:
            FP += 1
        else:
            TN += 1

    precision = TP / (TP + FP) if (TP + FP) > 0 else 0.0
    recall = TP / (TP + FN) if (TP + FN) > 0 else 0.0
    F1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    F025 = (1 + 0.25 ** 2) * precision * recall / ((0.25 ** 2) * precision + recall) if (precision + recall) > 0 else 0.0
    return precision, recall, F025, F1, g, o


# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Compare skeleton vs SCL objects using DTW + Euclidean")
    parser.add_argument("skeleton_file", help="Skeleton .data file")
    parser.add_argument("scl_file", help="Postprocessed SCL .data or .data.gz file")
    parser.add_argument("skeleton_thresh", help="skeleton.json")
    parser.add_argument("scl_thresh", help="SCL.json")
    parser.add_argument("--max-objects", type=int, default=None, help="Max number of objects to load")
    args = parser.parse_args()

    skel_thresh = load_thresholds(args.skeleton_thresh)
    scl_thresh = load_thresholds(args.scl_thresh)

    skeletons = load_skeleton_objects(args.skeleton_file, max_objects=args.max_objects)
    scl_vectors = load_postprocessed_objects(args.scl_file, max_objects=args.max_objects)

    assert len(skeletons) == len(scl_vectors), "Skeleton and SCL object counts must match"

    precision, recall, F025, F1, g, o = compute_metrics(skeletons, scl_vectors, skel_thresh, scl_thresh)

    print(f"Precision: {precision:.6f}")
    print(f"Recall:    {recall:.6f}")
    print(f"F0.25:     {F025:.6f}")
    print(f"F1:        {F1:.6f}")
    print(f"Gray zone skipped: {g}")
    print(o)


if __name__ == "__main__":
    main()
