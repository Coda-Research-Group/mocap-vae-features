import os
import json
import csv
from pathlib import Path
import re

# --- Configuration ---
# NOTE: Replace this with the actual base path on MetaCentrum
BASE_PATH = Path('/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/') 
OUTPUT_CSV_PATH = 'aggregated_metrics_summary.csv'

# Regex to extract key parameters from the path structure
# Captures: 1. Model, 2. Latent Dimension, 3. Beta, 4. Repetition number (which we will ignore here)
PATH_PATTERN = re.compile(r'/([^/]+)/model=([^/]+)_lat-dim=(\d+)_beta=([\d\.]+)/(\d+)/metrics\.json$')

# --- Metrics to Extract ---
TARGET_METRICS = ['precision', 'recall', 'F025', 'F1', 'accuracy']

# --- Aggregation Logic ---
def aggregate_metrics_summary(base_path: Path, output_csv_path: str, target_metrics: list):
    print(f"Starting summary aggregation from {base_path}...")
    
    all_metrics_files = list(base_path.rglob('metrics.json'))
    
    if not all_metrics_files:
        print("Error: No 'metrics.json' files found. Check BASE_PATH and file existence.")
        return

    data_rows = []
    
    # Store unique configurations processed to avoid duplicates (since we only care about the
    # aggregated result per setup, not the individual repetition files themselves)
    processed_configs = set() 
    
    for file_path in all_metrics_files:
        try:
            # 1. Extract parameters from the path
            relative_path = str(file_path).replace(str(base_path), '', 1)
            match = PATH_PATTERN.search(relative_path)
            
            if not match:
                continue

            _, model_name, lat_dim, beta_value, repetition = match.groups()
            
            # Create a unique key for the configuration (ignoring the repetition number)
            config_key = (model_name, lat_dim, beta_value)
            
            # Since the 'aggregated' section is identical across repetitions for the same setup,
            # we only process it once per configuration.
            if config_key in processed_configs:
                continue
            
            # 2. Load JSON content
            with open(file_path, 'r') as f:
                data = json.load(f)

            # 3. Extract Aggregated Mean Values
            row = {
                'model_name': model_name,
                'lat_dim': int(lat_dim),
                'beta': float(beta_value),
            }
            
            # Extract mean and std for the target metrics
            for metric in target_metrics:
                if metric in data['aggregated']:
                    # Extract mean and rename the column for clarity (e.g., 'precision_mean')
                    row[f'{metric}_mean'] = data['aggregated'][metric]['mean']
                    # Extract standard deviation
                    row[f'{metric}_std'] = data['aggregated'][metric]['std']
            
            data_rows.append(row)
            processed_configs.add(config_key) # Mark this configuration as processed

        except Exception as e:
            print(f"Could not process file {file_path}: {e}")
            continue

    if not data_rows:
        print("No valid data rows were generated.")
        return

    # 4. Write to CSV
    # Define the header based on the fixed keys and the requested metrics (mean and std)
    fieldnames = ['model_name', 'lat_dim', 'beta']
    for metric in target_metrics:
        fieldnames.append(f'{metric}_mean')
        fieldnames.append(f'{metric}_std')

    with open(output_csv_path, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        # Write header and all data rows
        writer.writeheader()
        writer.writerows(data_rows)

    print(f"\n✅ Successfully aggregated {len(data_rows)} unique configurations into {output_csv_path}")

# Run the function
aggregate_metrics_summary(BASE_PATH, OUTPUT_CSV_PATH, TARGET_METRICS)