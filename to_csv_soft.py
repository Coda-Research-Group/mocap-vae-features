#!/usr/bin/env python3
from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/soft/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/soft/experiment_results_summary.csv"

# Updated Regex to handle:
# Directory: .../model=hdm05_lat-dim=256_beta=0.1/100/
# Filename:  results-D0.04K6.txt
PATH_PARAM_PATTERN = re.compile(
    r"""
    model=(?P<model>[^/_]+)           # Capture model (e.g., hdm05)
    .* # loose match for separators
    lat[_-]?dim=?(?P<lat>\d+)         # Capture lat dimension (256)
    [_-]?beta=?(?P<beta>[\d.eE+\-]+)  # Capture beta (0.1)
    /                                 # Path separator
    (?P<k_dir>\d+)                    # Capture the K-directory (e.g., 100)
    /results-                         # Filename start
    D(?P<d_val>[\d.]+)                # Capture D value (e.g., 0.04)
    K(?P<k_val>\d+)                   # Capture K value (e.g., 6)
    \.txt$                            # End of string
    """,
    re.VERBOSE | re.IGNORECASE
)

def parse_log_file(filepath):
    """
    Parses the log file. 
    Based on your cat output, the file has multiple '===== NEW EXPERIMENT' blocks.
    We usually want the last valid run (likely the adaptive k or specifically the classification run).
    """
    results = {"kNN_Precision": None, "Classification_Precision": None}
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return results

    # Split by the separator to isolate runs
    # Your file seems to restart parameters, so we split by "===== GLOBAL PARAMS ====="
    runs = content.split("===== GLOBAL PARAMS =====")
    
    # If empty or just header, fallback to whole content
    chunks_to_check = runs[1:] if len(runs) > 1 else [content]

    # We iterate through chunks and update values; the last valid value found in the file will overwrite previous ones
    # (This effectively grabs the last/best run if multiple exist in one file)
    for chunk in chunks_to_check:
        # Extract Classification Precision
        m_class = re.search(r"classification precision over objects and categories:\s*([\d.]+)", chunk, re.IGNORECASE)
        if m_class:
            results["Classification_Precision"] = float(m_class.group(1))

        # Extract kNN Precision (look for 'precision over objects' that isn't 'classification precision')
        # The text format is "precision over objects and categories: 60.86..."
        m_knn = re.search(r"(?<!classification )precision over objects and categories:\s*([\d.]+)", chunk, re.IGNORECASE)
        if m_knn:
            results["kNN_Precision"] = float(m_knn.group(1))

    return results

def main():
    # Search for results-*.txt
    search_pattern = os.path.join(os.path.expanduser(BASE_DIR), "**", "results-*.txt")
    # Use recursive glob
    log_files = glob(search_pattern, recursive=True)

    print(f"Searching: {search_pattern}")
    print(f"Found {len(log_files)} result files.\n")

    if not log_files:
        print("No results found. Check BASE_DIR.")
        return

    # Dictionary Key: (model, lat, beta, k_dir, d_val, k_val)
    grouped = defaultdict(list)

    matched_count = 0
    for filepath in log_files:
        abs_path = os.path.abspath(filepath)
        m = PATH_PARAM_PATTERN.search(abs_path)
        
        if not m:
            # Optional: print unmatched for debugging
            # print(f"Skipping unmatched: {filepath}")
            continue
            
        matched_count += 1
        model = m.group("model")
        lat_dim = m.group("lat")
        beta = m.group("beta")
        k_dir = m.group("k_dir")
        d_val = m.group("d_val")
        k_val = m.group("k_val")

        key = (model, lat_dim, beta, k_dir, d_val, k_val)
        grouped[key].append(filepath)

    print(f"Successfully matched {matched_count} files.")
    print(f"Detected {len(grouped)} unique configurations (D/K variants).\n")

    # Define CSV Columns
    fieldnames = [
        "MODEL", "LAT_DIM", "BETA", "K_DIR", "D_VAL", "K_VAL",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]
    all_data = []

    # Process groups
    for (model, lat_dim, beta, k_dir, d_val, k_val), files in sorted(grouped.items()):
        knn_vals, class_vals = [], []
        for f in files:
            parsed = parse_log_file(f)
            if parsed["kNN_Precision"] is not None:
                knn_vals.append(parsed["kNN_Precision"])
            if parsed["Classification_Precision"] is not None:
                class_vals.append(parsed["Classification_Precision"])

        # Helper for formatting
        mean_or_na = lambda vals: round(statistics.mean(vals), 4) if vals else "N/A"
        std_or_na = lambda vals: round(statistics.stdev(vals), 4) if len(vals) > 1 else "N/A"

        all_data.append({
            "MODEL": model,
            "LAT_DIM": lat_dim,
            "BETA": beta,
            "K_DIR": k_dir,
            "D_VAL": d_val,
            "K_VAL": k_val,
            "Mean_kNN_Precision": mean_or_na(knn_vals),
            "Mean_Classification_Precision": mean_or_na(class_vals),
            "Std_kNN_Precision": std_or_na(knn_vals),
            "Std_Classification_Precision": std_or_na(class_vals),
            "Num_Files": len(files),
        })

    # Write to CSV
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