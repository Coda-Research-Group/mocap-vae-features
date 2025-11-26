#!/usr/bin/env python3
from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cv/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cv/experiment_results_summary.csv"

# Pattern: captures only lat_dim and beta
PATH_PARAM_PATTERN = re.compile(
    r"""
    /lat[_-]?dim=?(?P<lat>\d+)              # lat dimension
    [_-]?beta=?(?P<beta>[\d.eE+\-]+)        # beta
    /results-(?P<iter>\d+)\.txt$            # iteration (ignored in grouping)
    """,
    re.VERBOSE
)

def parse_log_file(filepath):
    results = {"kNN_Precision": None, "Classification_Precision": None}
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return results

    runs = content.split("===== GLOBAL PARAMS =====")
    if len(runs) >= 3:
        m = re.search(r"classification precision over objects and categories:\s*([\d.]+)", runs[1], re.IGNORECASE)
        if m:
            results["Classification_Precision"] = float(m.group(1))
        m = re.search(r"precision over objects and categories:\s*([\d.]+)", runs[2], re.IGNORECASE)
        if m:
            results["kNN_Precision"] = float(m.group(1))
    else:
        m = re.search(r"classification precision over objects and categories:\s*([\d.]+)", content, re.IGNORECASE)
        if m:
            results["Classification_Precision"] = float(m.group(1))
        m = re.search(r"precision over objects and categories:\s*([\d.]+)", content, re.IGNORECASE)
        if m:
            results["kNN_Precision"] = float(m.group(1))
    return results


def main():
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
            print(f"Skipping unmatched path: {filepath}")
            continue

        lat_dim = m.group("lat")
        beta = m.group("beta")

        key = (lat_dim, beta)
        grouped[key].append(filepath)

    print(f"Detected {len(grouped)} groups (lat_dim, beta).\n")

    fieldnames = [
        "LAT_DIM", "BETA",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]

    all_data = []

    for (lat_dim, beta), files in sorted(grouped.items()):
        knn_vals, class_vals = [], []
        for f in files:
            parsed = parse_log_file(f)
            if parsed["kNN_Precision"] is not None:
                knn_vals.append(parsed["kNN_Precision"])
            if parsed["Classification_Precision"] is not None:
                class_vals.append(parsed["Classification_Precision"])

        mean = lambda vals: round(statistics.mean(vals), 4) if vals else "N/A"
        std = lambda vals: round(statistics.stdev(vals), 4) if len(vals) > 1 else "N/A"

        all_data.append({
            "LAT_DIM": lat_dim,
            "BETA": beta,
            "Mean_kNN_Precision": mean(knn_vals),
            "Mean_Classification_Precision": mean(class_vals),
            "Std_kNN_Precision": std(knn_vals),
            "Std_Classification_Precision": std(class_vals),
            "Num_Files": len(files),
        })

    try:
        with open(OUTPUT_CSV, "w", newline="") as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(all_data)
        print(f"\n✅ Saved summary CSV to: {OUTPUT_CSV}")
        print(f"→ Aggregated {len(all_data)} (lat_dim, beta) experiment groups across iterations.")
    except Exception as e:
        print(f"Error writing CSV: {e}")


if __name__ == "__main__":
    main()
