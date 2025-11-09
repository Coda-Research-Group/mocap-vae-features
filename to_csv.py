from collections import defaultdict
import statistics
import os
import re
import csv
from glob import glob

BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/experiment_results_summary.csv"

PATH_PARAM_PATTERN = re.compile(r"/lat_dim=(\d+)_beta=(\d+\.?\d*)/(\d+)/results-(\d+)\.txt$")

def parse_log_file(filepath):
    results = {
        "kNN_Precision": None,
        "Classification_Precision": None,
    }

    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading file {filepath}: {e}")
        return results

    runs = content.split("===== GLOBAL PARAMS =====")
    if len(runs) < 3:
        return results

    # FIRST RUN (classification)
    first_run = runs[1]
    class_prec_matches = re.findall(
        r"classification precision over objects and categories:\s*([\d.]+)", first_run)
    if class_prec_matches:
        results["Classification_Precision"] = float(class_prec_matches[0])

    # SECOND RUN (kNN)
    second_run = runs[2]
    knn_prec_matches = re.findall(
        r"precision over objects and categories:\s*([\d.]+)", second_run)
    if knn_prec_matches:
        results["kNN_Precision"] = float(knn_prec_matches[0])

    return results


def main():
    search_pattern = os.path.join(os.path.expanduser(BASE_DIR), "lat_dim=*_beta=*", "*", "results-*.txt")
    log_files = glob(search_pattern)

    if not log_files:
        print(f"No log files found in: {BASE_DIR}")
        return

    print(f"Found {len(log_files)} log files. Grouping by experiment...")

    grouped = defaultdict(list)

    # Group files by experiment parameters
    for filepath in log_files:
        abs_path = os.path.abspath(filepath)
        match = PATH_PARAM_PATTERN.search(abs_path)
        if not match:
            continue
        lat_dim, beta, k_dir, _ = match.groups()
        key = (lat_dim, beta, k_dir)
        grouped[key].append(filepath)

    print(f"Detected {len(grouped)} unique experiment groups.")

    all_data = []
    fieldnames = ["LAT_DIM", "BETA", "K_DIR", "Mean_kNN_Precision", "Mean_Classification_Precision", "Num_Files"]

    for (lat_dim, beta, k_dir), files in grouped.items():
        knn_values, class_values = [], []

        for fpath in files:
            data = parse_log_file(fpath)
            if data["kNN_Precision"] is not None:
                knn_values.append(data["kNN_Precision"])
            if data["Classification_Precision"] is not None:
                class_values.append(data["Classification_Precision"])

        row = {
            "LAT_DIM": lat_dim,
            "BETA": beta,
            "K_DIR": k_dir,
            "Mean_kNN_Precision": statistics.mean(knn_values) if knn_values else "N/A",
            "Mean_Classification_Precision": statistics.mean(class_values) if class_values else "N/A",
            "Num_Files": len(files),
        }
        all_data.append(row)

    try:
        with open(OUTPUT_CSV, "w", newline="") as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(all_data)
        print(f"\n✅ Averaged CSV created at: {OUTPUT_CSV}")
        print(f"Included {len(all_data)} aggregated experiment groups.")
    except Exception as e:
        print(f"Error writing CSV: {e}")


if __name__ == "__main__":
    main()
