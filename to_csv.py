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
# - or no prefix at all (start with "lat_dim=")
# Captures: model (optional), lat_dim, beta, k_dir, iter
PATH_PARAM_PATTERN = re.compile(
    r"/(?:(?P<model>[^/]+?)_)?lat_dim=(?P<lat>\d+)_beta=(?P<beta>[\d.]+)/(?P<k>\d+)/results-(?P<iter>\d+)\.txt$"
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
    if len(runs) < 3:
        return results

    # FIRST RUN: classification precision
    m = re.search(r"classification precision over objects and categories:\s*([\d.]+)", runs[1])
    if m:
        results["Classification_Precision"] = float(m.group(1))

    # SECOND RUN: kNN precision
    m = re.search(r"precision over objects and categories:\s*([\d.]+)", runs[2])
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
            # optional: show a few unmatched paths to help debugging
            print(f"Skipping unmatched path: {filepath}")
            continue

        model = m.group("model") or "base"   # "base" for directories that start with lat_dim=
        lat_dim = m.group("lat")
        beta = m.group("beta")
        k_dir = m.group("k")
        # iter_ = m.group("iter")   # if you need iter later you can use it

        key = (model, lat_dim, beta, k_dir)
        grouped[key].append(filepath)

    print(f"Detected {len(grouped)} experiment groups.\n")

    fieldnames = [
        "MODEL", "LAT_DIM", "BETA", "K_DIR",
        "Mean_kNN_Precision", "Mean_Classification_Precision",
        "Std_kNN_Precision", "Std_Classification_Precision",
        "Num_Files"
    ]
    all_data = []

    for (model, lat_dim, beta, k_dir), files in grouped.items():
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
        print(f"→ Aggregated {len(all_data)} groups.")
    except Exception as e:
        print(f"Error writing CSV: {e}")


if __name__ == "__main__":
    main()
