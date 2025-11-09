from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

# --- Configuration ---
BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/experiment_results_summary.csv"

# --- Regex Pattern ---
# Example full path:
# /.../hdm05-handL_lat_dim=64_beta=0.1/350/results-3.txt
# or
# /.../lat_dim=64_beta=0.1/350/results-3.txt
PATH_PARAM_PATTERN = re.compile(
    r"/([^/]*?)_?lat_dim=(\d+)_beta=(\d+\.?\d*)/(\d+)/results-(\d+)\.txt$"
)

def parse_log_file(filepath):
    """Extracts precision values from results-n.txt"""
    results = {
        "kNN_Precision": None,
        "Classification_Precision": None,
    }

    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return results

    runs = content.split("===== GLOBAL PARAMS =====")
    if len(runs) < 3:
        return results

    # FIRST RUN: classification
    first_run = runs[1]
    class_prec = re.findall(r"classification precision over objects and categories:\s*([\d.]+)", first_run)
    if class_prec:
        results["Classification_Precision"] = float(class_prec[0])

    # SECOND RUN: kNN
    second_run = runs[2]
    knn_prec = re.findall(r"precision over objects and categories:\s*([\d.]+)", second_run)
    if knn_prec:
        results["kNN_Precision"] = float(knn_prec[0])

    return results


def main():
    search_pattern = os.path.join(os.path.expanduser(BASE_DIR), "*lat_dim=*_*", "*", "results-*.txt")
    log_files = glob(search_pattern)

    if not log_files:
        print(f"No log files found under {BASE_DIR}")
        return

    print(f"Found {len(log_files)} result files. Grouping...")

    grouped = defaultdict(list)

    # Group by MODEL, LAT_DIM, BETA, K_DIR
    for filepath in log_files:
        abs_path = os.path.abspath(filepath)
        m = PATH_PARAM_PATTERN.search(abs_path)
        if not m:
            print(f"⚠️ Skipping unmatched path: {filepath}")
            continue

        model_name, lat_dim, beta, k_dir, _ = m.groups()
        model_name = model_name or "global"  # if empty prefix (like lat_dim=...)
        key = (model_name, lat_dim, beta, k_dir)
        grouped[key].append(filepath)

    print(f"→ Found {len(grouped)} unique experiment groups.\n")

    all_data = []
    fieldnames = [
        "MODEL", "LAT_DIM", "BETA", "K_DIR",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]

    for (model, lat_dim, beta, k_dir), files in grouped.items():
        knn_vals, class_vals = [], []

        for f in files:
            parsed = parse_log_file(f)
            if parsed["kNN_Precision"] is not None:
                knn_vals.append(parsed["kNN_Precision"])
            if parsed["Classification_Precision"] is not None:
                class_vals.append(parsed["Classification_Precision"])

        def mean_or_na(values):
            return round(statistics.mean(values), 4) if values else "N/A"

        def std_or_na(values):
            return round(statistics.stdev(values), 4) if len(values) > 1 else "N/A"

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

    # --- Write to CSV ---
    try:
        with open(OUTPUT_CSV, "w", newline="") as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(all_data)
        print(f"✅ Saved summary CSV to: {OUTPUT_CSV}")
        print(f"→ Aggregated {len(all_data)} experiment groups.\n")
    except Exception as e:
        print(f"❌ Error writing CSV: {e}")


if __name__ == "__main__":
    main
