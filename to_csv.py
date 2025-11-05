import os
import re
import csv
from glob import glob

# --- Configuration ---
# Set the base directory for your experiments.
# The script will start searching from here.
BASE_DIR = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/"
OUTPUT_CSV = "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/experiment_results_summary.csv"


# --- Regex Patterns for Extraction ---
# Pattern 1: Extracts the first float after 'precision over objects and categories:' (kNN Search Precision)
KNN_PRECISION_PATTERN = re.compile(r"precision over objects and categories:\s*(\d+\.\d+)")

# Pattern 2: Extracts the first float after 'classification precision over objects and categories:' (Classification Precision)
CLASSIFICATION_PRECISION_PATTERN = re.compile(r"classification precision over objects and categories:\s*(\d+\.\d+)")

# Pattern 3: Extracts parameters from the file path structure
# Looks for: .../lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt
# Updated: BETA parameter (\d+\.?\d*) now accepts floating-point numbers like 0.1
PATH_PARAM_PATTERN = re.compile(r"/lat_dim=(\d+)_beta=(\d+\.?\d*)/(\d+)/results-(\d+)\.txt$")


def parse_log_file(filepath):
    """
    Reads a single log file and extracts:
      - kNN_Precision from the FIRST run (k=4)
      - Classification_Precision from the SECOND run (adaptive)
    """
    results = {
        "kNN_Precision": "N/A",
        "Classification_Precision": "N/A",
    }

    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading file {filepath}: {e}")
        return results

    # Split the file into two runs by '===== GLOBAL PARAMS ====='
    runs = content.split("===== GLOBAL PARAMS =====")
    if len(runs) < 3:
        # Only one or invalid run found
        return results

    # --- FIRST RUN (k=4) ---
    first_run = runs[1]
    class_prec_matches = re.findall(
        r"classification precision over objects and categories:\s*([\d.]+)", first_run)
    if class_prec_matches:
        results["Classification_Precision"] = class_prec_matches[0]

    # --- SECOND RUN (adaptive) ---
    second_run = runs[2]
    knn_prec_matches = re.findall(
        r"precision over objects and categories:\s*([\d.]+)", second_run)
    if knn_prec_matches:
        results["kNN_Precision"] = knn_prec_matches[0]

    return results



def main():
    # Expand the user directory symbol (~)
    search_dir = os.path.expanduser(BASE_DIR)
    
    # Define the full glob pattern to find all files matching the structure
    # This finds files deep within the DIM/BETA/K directories
    search_pattern = os.path.join(search_dir, "lat_dim=*_beta=*", "*", "results-*.txt")
    log_files = glob(search_pattern)

    if not log_files:
        print(f"No log files found matching the pattern in: {search_dir}")
        print("Please check the BASE_DIR and file path structure.")
        return

    print(f"Found {len(log_files)} log files. Starting extraction...")

    all_data = []
    
    # Define the final CSV headers
    fieldnames = ["LAT_DIM", "BETA", "K_DIR", "ITER", "kNN_Precision", "Classification_Precision", "Filepath"]

    for filepath in log_files:
        # --- 1. Extract path parameters ---
        # The path needs to be absolute for the regex to match the pattern at the end of the string
        abs_filepath = os.path.abspath(filepath)
        
        path_match = PATH_PARAM_PATTERN.search(abs_filepath)
        
        # Initialize row data with filepath and defaults
        row = {
            "Filepath": filepath,
            "LAT_DIM": "N/A", "BETA": "N/A", "K_DIR": "N/A", "ITER": "N/A",
        }
        
        if path_match:
            row["LAT_DIM"] = path_match.group(1)
            row["BETA"] = path_match.group(2)
            row["K_DIR"] = path_match.group(3)
            row["ITER"] = path_match.group(4)
        
        # --- 2. Extract precision values from file content ---
        precision_data = parse_log_file(filepath)
        row.update(precision_data)
        
        all_data.append(row)
        
    # --- 3. Write to CSV ---
    try:
        with open(OUTPUT_CSV, 'w', newline='') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(all_data)
        
        print(f"\nSuccessfully created CSV report: {OUTPUT_CSV}")
        print(f"Data saved for {len(all_data)} experiment runs.")
    
    except Exception as e:
        print(f"Error writing CSV file: {e}")

if __name__ == "__main__":
    main()