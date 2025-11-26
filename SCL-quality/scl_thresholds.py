#!/usr/bin/env python3
import argparse
import gzip
import numpy as np
from scipy.spatial.distance import pdist
from tqdm import tqdm
import random
import json
import os
import matplotlib.pyplot as plt  # For visualization

# ---------------------
# File Streaming and Sampling
# ---------------------
def stream_sample_objects(file_path, valid_prefixes, subset_size, seed=None):
    """
    Stream through the data file (.data or .data.gz) and randomly sample 
    up to subset_size objects whose prefix matches one of the valid_prefixes.
    """
    if seed is not None:
        random.seed(seed)
        np.random.seed(seed)

    reservoir = []
    total_matching = 0
    current_id = None

    if file_path.endswith(".gz"):
        opener = gzip.open
        mode = "rt"
    else:
        opener = open
        mode = "r"

    try:
        with opener(file_path, mode, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue

                if line.startswith("#objectKey"):
                    parts = line.split()
                    current_id = parts[2] if len(parts) >= 3 else None
                else:
                    if current_id is None:
                        continue
                    
                    if current_id.startswith("8;mcdr.objects"):
                        continue

                    prefix_parts = current_id.rsplit('_', 1)
                    prefix = prefix_parts[0] if len(prefix_parts) > 1 else current_id
                    
                    if valid_prefixes is None or prefix in valid_prefixes:
                        total_matching += 1
                        
                        try:
                            vector = np.array([float(x) for x in line.split(",")], dtype=np.float32)
                        except ValueError:
                            current_id = None
                            continue

                        if len(reservoir) < subset_size:
                            reservoir.append(vector)
                        else:
                            j = random.randint(0, total_matching - 1)
                            if j < subset_size:
                                reservoir[j] = vector

                    current_id = None
        
    except Exception as e:
        print(f"Error processing file {file_path}: {e}")
        raise

    if not reservoir:
        raise RuntimeError("No matching objects found for the given prefixes or subset size is too large.")
    return np.stack(reservoir), total_matching

# ---------------------
# Computation and Plotting
# ---------------------
def compute_percentiles(vectors, metric="cosine", percentiles=(0.5, 40), return_dists=False):
    """Compute pairwise distance percentiles and optionally returns the distance array."""
    dists = pdist(vectors, metric=metric)
    if return_dists:
        return np.percentile(dists, percentiles), dists
    return np.percentile(dists, percentiles)

def plot_distribution(distances, metric, save_path):
    """Generates and saves a histogram of pairwise distances."""
    plt.figure(figsize=(10, 6))
    plt.hist(distances, bins=50, density=True, alpha=0.7, edgecolor='black')
    plt.title(f'Distribution of Pairwise {metric.capitalize()} Distances', fontsize=14)
    plt.xlabel(f'{metric.capitalize()} Distance', fontsize=12)
    plt.ylabel('Density', fontsize=12)
    plt.grid(axis='y', alpha=0.6, linestyle='--')
    plt.xlim(0, 2)
    plt.savefig(save_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Plot saved to {save_path}")

# ---------------------
# Main
# ---------------------
def main():
    parser = argparse.ArgumentParser(description="Compute percentile distances for objects (optionally filtered by train IDs) and optionally save a plot of the distribution.")
    parser.add_argument("objects_file", help="Path to the .data or .data.gz objects file (assumed to contain one vector per object block)")
    parser.add_argument("--train-ids-file", help="Optional path to train IDs text file (base prefixes)", default=None)
    parser.add_argument("--metric", choices=["cosine", "euclidean"], default="cosine", help="Distance metric to use")
    parser.add_argument("--subset-size", type=int, required=True, help="Number of objects to randomly sample per run")
    parser.add_argument("--runs", type=int, default=10, help="Number of random subset runs to average over (default: 10)")
    parser.add_argument("--output", type=str, default=None, help="Optional path to output simple JSON with averaged percentiles")
    parser.add_argument("--plot", type=str, default=None, help="Path to save histogram plot (only from first run).")
    parser.add_argument("--seed", type=int, default=42, help="Initial random seed for reproducibility (default: 42)")
    args = parser.parse_args()

    if args.train_ids_file:
        try:
            with open(args.train_ids_file, "r", encoding="utf-8") as f:
                train_ids = {line.strip() for line in f if line.strip()}
            print(f"Loaded {len(train_ids)} train IDs (prefixes)")
        except FileNotFoundError:
            print(f"Error: Train IDs file not found at {args.train_ids_file}")
            return
    else:
        train_ids = None
        print("No train IDs file provided — using all objects.")

    all_results = []
    first_run_distances = None 
    
    for run in range(args.runs):

        vectors, total_matching = stream_sample_objects(
            args.objects_file, train_ids, subset_size=args.subset_size, seed=args.seed + run 
        )
        
        return_dists = (args.plot is not None and run == 0)
        
        if return_dists:
            p, dists = compute_percentiles(vectors, metric=args.metric, return_dists=True)
            first_run_distances = dists
        else:
            p = compute_percentiles(vectors, metric=args.metric)
        
        all_results.append(p)

    all_results = np.array(all_results)
    mean_p = np.mean(all_results, axis=0)

    print("-" * 30)
    print(f"Processed {args.runs} runs (avg {args.subset_size} samples/run)")
    print(f"Total matching objects found in stream: ~{total_matching}")
    print(f"{args.metric.capitalize()} distance percentiles (averaged):")
    print(f"0.5th percentile: {mean_p[0]:.6f}")
    print(f"40th percentile:  {mean_p[1]:.6f}")
    print("-" * 30)

    if args.output:
        results = {
            "p0.5": float(mean_p[0]), 
            "p40": float(mean_p[1]),
        }
        with open(args.output, "w", encoding="utf-8") as jf:
            json.dump(results, jf, indent=2)
        print(f"Saved results to {args.output}")

    if args.plot and first_run_distances is not None:
        plot_distribution(first_run_distances, args.metric, args.plot)

if __name__ == "__main__":
    main()
