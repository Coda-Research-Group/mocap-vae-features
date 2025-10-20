#!/usr/bin/env python3
import argparse
import gzip
import numpy as np
from scipy.spatial.distance import pdist
from tqdm import tqdm
import random
import json


def stream_sample_objects(file_path, valid_prefixes, subset_size, seed=None):
    """
    Stream through a .data.gz file and randomly sample up to subset_size objects
    whose prefix matches one of the valid_prefixes.
    """
    if seed is not None:
        random.seed(seed)
        np.random.seed(seed)

    reservoir = []
    total_matching = 0
    current_id = None

    with gzip.open(file_path, "rt", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue

            if line.startswith("#objectKey"):
                current_id = line.split()[-1]
            else:
                if current_id is None:
                    continue

                prefix = "_".join(current_id.split("_")[:-1])
                if prefix in valid_prefixes:
                    total_matching += 1
                    vector = np.array([float(x) for x in line.split(",")], dtype=np.float32)

                    # Reservoir sampling
                    if len(reservoir) < subset_size:
                        reservoir.append(vector)
                    else:
                        j = random.randint(0, total_matching - 1)
                        if j < subset_size:
                            reservoir[j] = vector

                current_id = None

    if not reservoir:
        raise RuntimeError("No matching objects found for the given train IDs.")
    return np.stack(reservoir), total_matching


def compute_percentiles(vectors, metric="cosine", percentiles=(0.5, 40)):
    """Compute pairwise distance percentiles."""
    dists = pdist(vectors, metric=metric)
    return np.percentile(dists, percentiles)


def main():
    parser = argparse.ArgumentParser(description="Compute percentile distances for train ID-matched objects (streaming).")
    parser.add_argument("objects_file", help="Path to the .data.gz objects file")
    parser.add_argument("train_ids_file", help="Path to the train IDs text file")
    parser.add_argument("--metric", choices=["cosine", "euclidean"], default="cosine",
                        help="Distance metric to use")
    parser.add_argument("--subset-size", type=int, required=True,
                        help="Number of objects to randomly sample per run")
    parser.add_argument("--runs", type=int, default=10,
                        help="Number of random subset runs to average over (default: 10)")
    parser.add_argument("--output", type=str, default=None,
                        help="Optional path to output simple JSON with averaged percentiles")
    args = parser.parse_args()

    # Load train IDs
    with open(args.train_ids_file, "r", encoding="utf-8") as f:
        train_ids = {line.strip() for line in f if line.strip()}

    print(f"Loaded {len(train_ids)} train IDs")

    all_results = []
    for run in tqdm(range(args.runs), desc="Runs"):
        vectors, total_matching = stream_sample_objects(
            args.objects_file, train_ids, subset_size=args.subset_size, seed=run
        )
        p = compute_percentiles(vectors, metric=args.metric)
        all_results.append(p)

    all_results = np.array(all_results)
    mean_p = np.mean(all_results, axis=0)

    print(f"Processed {args.runs} runs (avg {args.subset_size} samples/run)")
    print(f"Total matching objects found: ~{total_matching}")
    print(f"{args.metric.capitalize()} distance percentiles (averaged):")
    print(f"0.5th percentile: {mean_p[0]:.6f}")
    print(f"40th percentile:  {mean_p[1]:.6f}")

    if args.output:
        results = {
            "p0.5": float(mean_p[0]),
            "p40": float(mean_p[1])
        }
        with open(args.output, "w", encoding="utf-8") as jf:
            json.dump(results, jf, indent=2)
        print(f"Saved results to {args.output}")



if __name__ == "__main__":
    main()
