#!/usr/bin/env python3
from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite/cs/"
OUTPUT_CSV = os.path.join(BASE_DIR, "experiment_results_summary.csv")

# Capture LAT dim, BETA, K directly from filename
FILENAME_PATTERN = re.compile(
    r"dim=(?P<lat>\d+)_beta=(?P<beta>[\d.eE+\-]+)_(?P<k>\d+)\.txt$",
    re.IGNORECASE
)

def parse_log_file(filepath):
    """
    Extracts metrics from files:
    - Run 1 -> Classification Precision
    - Run 2 -> kNN Precision
    """
    results = {"kNN_Precision": None, "Classification_Precision": None}
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return results

    content_blocks = content.split("===== NEW EXPERIMENT")

    if len(content_blocks) > 1:
        run1_txt = content_blocks[1]
        m_class = re.search(r"classification precision over objects and categories:\s*([\d.]+)", run1_txt, re.IGNORECASE)
        if m_class:
            results["Classification_Precision"] = float(m_class.group(1))

    if len(content_blocks) > 2:
        run2_txt = content_blocks[2]
        m_knn = re.search(r"(?<!classification )precision over objects and categories:\s*([\d.]+)", run2_txt, re.IGNORECASE)
        if m_knn:
            results["kNN_Precision"] = float(m_knn.group(1))

    return results

def main():
    search_pattern = os.path.join(BASE_DIR, "*.txt")
    log_files = glob(search_pattern)

    print(f"Searching: {search_pattern}")
    print(f"Found {len(log_files)} result files.\n")

    if not log_files:
        print("No results found.")
        return

    grouped = defaultdict(list)

    for filepath in log_files:
        filename = os.path.basename(filepath)
        m = FILENAME_PATTERN.search(filename)
        if not m:
            print(f"⚠️ File ignored (doesn't match pattern): {filename}")
            continue

        lat_dim = m.group("lat")
        beta = m.group("beta")
        k_val = m.group("k")

        key = (lat_dim, beta, k_val)
        grouped[key].append(filepath)

    print(f"Detected {len(grouped)} unique configurations.\n")

    fieldnames = [
        "LAT_DIM", "BETA", "K",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]
    all_data = []

    for (lat_dim, beta, k_val), files in sorted(grouped.items()):
        knn_vals, class_vals = [], []
        for f in files:
            parsed = parse_log_file(f)
            if parsed["kNN_Precision"] is not None:
                knn_vals.append(parsed["kNN_Precision"])
            if parsed["Classification_Precision"] is not None:
                class_vals.append(parsed["Classification_Precision"])

        mean = lambda v: round(statistics.mean(v), 4) if v else "N/A"
        std = lambda v: round(statistics.stdev(v), 4) if len(v) > 1 else "N/A"

        all_data.append({
            "LAT_DIM": lat_dim,
            "BETA": beta,
            "K": k_val,
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
        print(f"✅ Saved summary CSV to: {OUTPUT_CSV}")
    except Exception as e:
        print(f"Error writing CSV: {e}")

if __name__ == "__main__":
    main()
