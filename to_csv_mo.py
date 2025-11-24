#!/usr/bin/env python3
from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/cv/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/cv/experiment_results_summary.csv"

# Regex to capture Model, Lat, Beta from path AND D/K values from filename
PATH_PARAM_PATTERN = re.compile(
    r"""
    model=(?P<model>[^/_]+)           # Capture model (e.g., pku-mmd)
    .* # loose match for separators
    lat[_-]?dim=?(?P<lat>\d+)         # Capture lat dimension
    [_-]?beta=?(?P<beta>[\d.eE+\-]+)  # Capture beta
    /                                 # Path separator
    (?P<k_dir>\d+)                    # Capture the K-directory
    /results.txt$                            # End of string
    """,
    re.VERBOSE | re.IGNORECASE
)

def parse_log_file(filepath):
    """
    Splits the file into experiment blocks.
    - Run 1: Extracts Classification Precision.
    - Run 2: Extracts kNN Precision.
    """
    results = {"kNN_Precision": None, "Classification_Precision": None}
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return results

    # Split by the "NEW EXPERIMENT" header.
    # content_blocks[0] is usually the first Global Params (ignore).
    # content_blocks[1] is the FIRST Run.
    # content_blocks[2] is the SECOND Run.
    content_blocks = content.split("===== NEW EXPERIMENT")

    # Need at least 2 blocks to have the 1st run (block 0 is pre-header)
    if len(content_blocks) > 1:
        # --- BLOCK 1 (First Run): Extract CLASSIFICATION Precision ---
        run1_txt = content_blocks[1]
        m_class = re.search(r"classification precision over objects and categories:\s*([\d.]+)", run1_txt, re.IGNORECASE)
        if m_class:
            results["Classification_Precision"] = float(m_class.group(1))

    # Need at least 3 blocks to have the 2nd run
    if len(content_blocks) > 2:
        # --- BLOCK 2 (Second Run): Extract kNN Precision ---
        run2_txt = content_blocks[2]
        # kNN precision is "precision over objects..." NOT preceded by "classification"
        m_knn = re.search(r"(?<!classification )precision over objects and categories:\s*([\d.]+)", run2_txt, re.IGNORECASE)
        if m_knn:
            results["kNN_Precision"] = float(m_knn.group(1))

    return results

def main():
    search_pattern = os.path.join(os.path.expanduser(BASE_DIR), "**", "results-*.txt")
    log_files = glob(search_pattern, recursive=True)

    print(f"Searching: {search_pattern}")
    print(f"Found {len(log_files)} result files.\n")

    if not log_files:
        print("No results found.")
        return

    # Key: (model, lat, beta, k_dir, d_val, k_val)
    grouped = defaultdict(list)

    for filepath in log_files:
        abs_path = os.path.abspath(filepath)
        m = PATH_PARAM_PATTERN.search(abs_path)
        
        if not m:
            continue
            
        model = m.group("model")
        lat_dim = m.group("lat")
        beta = m.group("beta")
        k_dir = m.group("k_dir")
  

        key = (model, lat_dim, beta, k_dir)
        grouped[key].append(filepath)

    print(f"Detected {len(grouped)} unique configurations (D/K variants).\n")

    fieldnames = [
        "MODEL", "LAT_DIM", "BETA", "K_DIR",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]
    all_data = []

    for (model, lat_dim, beta, k_dir, d_val, k_val), files in sorted(grouped.items()):
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
        print(f"✅ Saved summary CSV to: {OUTPUT_CSV}")
    except Exception as e:
        print(f"Error writing CSV: {e}")

if __name__ == "__main__":
    main()