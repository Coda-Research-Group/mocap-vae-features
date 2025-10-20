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
# Load HDM05 skeletons
# ---------------------
def load_hdm05_skeleton_objects(file_path, valid_ids=None, max_objects=None, seed=None, pad_to_8=False):
    if seed is not None:
        random.seed(seed)
        np.random.seed(seed)

    objects = []
    current_id = None
    frame_lines = []

    def finalize_object():
        if not frame_lines:
            return
        obj_array = np.stack(frame_lines)
        if pad_to_8 and len(frame_lines) < 8:
            last_frame = frame_lines[-1]
            pad_count = 8 - len(frame_lines)
            pad_frames = [last_frame] * pad_count
            obj_array = np.concatenate([obj_array, np.stack(pad_frames)], axis=0)
        if valid_ids is None or current_id in valid_ids:
            objects.append(obj_array)

    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if line.startswith("#objectKey"):
                if current_id is not None and frame_lines:
                    finalize_object()
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
                    finalize_object()
                    current_id = None
                    frame_lines = []
                    if max_objects is not None and len(objects) >= max_objects:
                        break
        if current_id is not None and frame_lines:
            finalize_object()

    if not objects:
        raise RuntimeError(f"No skeleton objects loaded from {file_path}")
    return objects

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
    return float(np.linalg.norm(a - b))

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
        return (0, 0, 0, 0, 1)

    d_scl = cosine(scl_vectors[i], scl_vectors[j])
    if d_scl < p0_5_scl:
        label_scl = True
    elif d_scl > p40_scl:
        label_scl = False
    else:
        return (0, 0, 0, 0, 1)

    TP = FP = FN = TN = 0
    if label_skel and label_scl:
        TP = 1
    elif label_skel and not label_scl:
        FN = 1
    elif not label_skel and label_scl:
        FP = 1
    else:
        TN = 1
    return TP, FP, FN, TN, 0

# ---------------------
# Sequential computation (no parallelism)
# ---------------------
def compute_metrics_chunked(skeletons, scl_vectors, skel_thresh, scl_thresh, chunk_size=10000):
    n_objects = len(skeletons)
    TP = FP = FN = TN = g = 0
    indices = list(combinations(range(n_objects), 2))
    for start in tqdm(range(0, len(indices), chunk_size)):
        chunk = indices[start:start + chunk_size]
        for i, j in chunk:
            r = evaluate_pair(i, j, skeletons, scl_vectors, skel_thresh, scl_thresh)
            TP += r[0]
            FP += r[1]
            FN += r[2]
            TN += r[3]
            g += r[4]

    precision = TP / (TP + FP) if (TP + FP) > 0 else 0.0
    recall = TP / (TP + FN) if (TP + FN) > 0 else 0.0
    F1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    F025 = (1 + 0.25 ** 2) * precision * recall / ((0.25 ** 2) * precision + recall) if (precision + recall) > 0 else 0.0
    print(TP, TN, FP, FN)
    return precision, recall, F025, F1, g

# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Compare skeleton vs postprocessed objects for HDM05 or PKU-MMD.")
    parser.add_argument("skeleton_file")
    parser.add_argument("scl_file")
    parser.add_argument("skeleton_thresh")
    parser.add_argument("scl_thresh")
    parser.add_argument("--dataset", choices=["hdm05", "pku-mmd"], required=True)
    parser.add_argument("--max-objects", type=int, default=None)
    parser.add_argument("--chunk-size", type=int, default=10000)
    parser.add_argument("--pad-to-8", action="store_true", help="Pad shorter sequences to 8 frames")
    args = parser.parse_args()

    skel_thresh = load_thresholds(args.skeleton_thresh)
    scl_thresh = load_thresholds(args.scl_thresh)

    if args.dataset == "hdm05":
        skeletons = load_hdm05_skeleton_objects(
            args.skeleton_file, max_objects=args.max_objects, pad_to_8=args.pad_to_8
        )
    else:
        skeletons = load_pku_mmd_skeleton_objects(args.skeleton_file, max_objects=args.max_objects)

    scl_vectors = load_postprocessed_objects(args.scl_file, max_objects=args.max_objects)

    precision, recall, F025, F1, g = compute_metrics_chunked(
        skeletons, scl_vectors, skel_thresh, scl_thresh, chunk_size=args.chunk_size
    )

    print(f"Precision: {precision:.6f}")
    print(f"Recall:    {recall:.6f}")
    print(f"F0.25:     {F025:.6f}")
    print(f"F1:        {F1:.6f}")
    print(f"Gray:      {g}")

if __name__ == "__main__":
    main()
