#!/usr/bin/env python3
from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/experiment_results_summary.csv"

# Flexible pattern:
# - optional model prefix like "hdm05" or "pku-mmd-handR" (anything before "_lat_dim=")
# - captures: optional model, lat_dim, beta, k_dir
# - DOES NOT include iteration in the grouping key (we will aggregate across all iteration files)
# Example matched path:
#  .../hdm05_lat_dim=64_beta=0.1/5/results-1.txt
PATH_PARAM_PATTERN = re.compile(
    r"""
    /                                   # path separator before the parameters
    (?:(?P<model>[^/]+?)_)?              # optional model prefix followed by underscore (non-greedy)
    lat[_-]?dim=?(?P<lat>\d+)            # lat dimension (accept lat_dim or lat-dim or latdim)
    [_-]?beta=?(?P<beta>[\d.eE+\-]+)     # beta (floats allowed, scientific)
    .*?                                  # any chars (non-greedy) until the k dir portion
    /(?P<k>\d+)                          # k directory (a number folder)
    /results-[0-9]+\.txt$                # file name results-<iter>.txt (we don't capture iter)
    """,
    re.VERBOSE,
)

def parse_log_file(filepath):
    """Extracts kNN and classification precision from the file (if present)."""
    results = {"kNN_Precision": None, "Classification_Precision": None}
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return results

    runs = content.split("===== GLOBAL PARAMS =====")
    # If file format is different, try to still extract values by searching entire file.
    if len(runs) >= 3:
        # FIRST RUN: classification precision (runs[1])
        m = re.search(r"classification precision over objects and categories:\s*([\d.]+)", runs[1], re.IGNORECASE)
        if m:
            results["Classification_Precision"] = float(m.group(1))

        # SECOND RUN: kNN precision (runs[2])
        m = re.search(r"precision over objects and categories:\s*([\d.]+)", runs[2], re.IGNORECASE)
        if m:
            results["kNN_Precision"] = float(m.group(1))
    else:
        # fallback: search whole file for typical lines
        m = re.search(r"classification precision over objects and categories:\s*([\d.]+)", content, re.IGNORECASE)
        if m:
            results["Classification_Precision"] = float(m.group(1))
        m = re.search(r"precision over objects and categories:\s*([\d.]+)", content, re.IGNORECASE)
        if m:
            results["kNN_Precision"] = float(m.group(1))

    return results


def main():
    # recursive search for any results-*.txt under BASE_DIR
    search_pattern = os.path.join(os.path.expanduser(BASE_DIR), "**", "results-*.txt")
    log_files = glob(search_pattern, recursive=True)

    print(f"Searching: {search_pattern}")
    print(f"Found {len(log_files)} result files.\n")

    if not log_files:
        print("No results found. Check BASE_DIR or file names.")
        return

    grouped = defaultdict(list)

    for filepath in log_files:
        abs_path = os.path.abspath(filepath)
        m = PATH_PARAM_PATTERN.search(abs_path)
        if not m:
            # show some unmatched paths to help debugging (but keep output short)
            print(f"Skipping unmatched path: {filepath}")
            continue

        model = m.group("model") or "base"   # "base" for directories that start with lat_dim=...
        lat_dim = m.group("lat")
        beta = m.group("beta")
        k_dir = m.group("k")
        # intentionally DO NOT extract or use iteration here; we will aggregate across all iterations

        key = (model, lat_dim, beta, k_dir)
        grouped[key].append(filepath)

    print(f"Detected {len(grouped)} experiment groups (model,lat,beta,k).\n")

    fieldnames = [
        "MODEL", "LAT_DIM", "BETA", "K_DIR",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]
    all_data = []

    for (model, lat_dim, beta, k_dir), files in sorted(grouped.items()):
        knn_vals, class_vals = [], []
        for f in files:
            parsed = parse_log_file(f)
            if parsed["kNN_Precision"] is not None:
                knn_vals.append(parsed["kNN_Precision"])
            if parsed["Classification_Precision"] is not None:
                class_vals.append(parsed["Classification_Precision"])

        mean_or_na = lambda vals: round(statistics.mean(vals), 4) if vals else "N/A"
        std_or_na = lambda vals: round(statistics.stdev(vals), 4) if len(vals) > 1 else "N/A"

        all_data.append({
            "MODEL": model,
            "LAT_DIM": lat_dim,
            "BETA": beta,
            "K_DIR": k_dir,
            "Mean_kNN_Precision": mean_or_na(knn_vals),
            "Mean_Classification_Precision": mean_or_na(class_vals),
            "Std_kNN_Precision": std_or_na(knn_vals),
            "Std_Classification_Precision": std_or_na(class_vals),
            "Num_Files": len(files),
        })

    try:
        with open(OUTPUT_CSV, "w", newline="") as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(all_data)
        print(f"\n✅ Saved summary CSV to: {OUTPUT_CSV}")
        print(f"→ Aggregated {len(all_data)} groups (iteration files combined per group).")
    except Exception as e:
        print(f"Error writing CSV: {e}")


if __name__ == "__main__":
    main()
