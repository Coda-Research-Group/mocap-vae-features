#!/usr/bin/env bash
set -euo pipefail

# Base directory
BASE_DIR="/storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all"
OUTPUT_CSV="$BASE_DIR/all_results.csv"

# Write CSV header
echo "dim,beta,iter,K,source_file,content" > "$OUTPUT_CSV"

# Only process global models (ignore body-part models)
for model_dir in "$BASE_DIR"/model=hdm05_lat-dim=*; do
    [[ -d "$model_dir" ]] || continue

    model_name=$(basename "$model_dir")

    # Extract hyperparameters
    dim=$(echo "$model_name" | sed -E 's/.*_lat-dim=([0-9]+).*/\1/')
    beta=$(echo "$model_name" | sed -E 's/.*_beta=([0-9.]+).*/\1/')

    # Iterate through iteration folders (1, 2, 3, 4, 5)
    for iter_dir in "$model_dir"/*; do
        [[ -d "$iter_dir" ]] || continue
        iter_name=$(basename "$iter_dir")

        # KMeans subfolders inside iteration folder
        for k_dir in "$iter_dir"/KMeansPivotChooser--kmeans.k_*; do
            [[ -d "$k_dir" ]] || continue
            K=$(basename "$k_dir" | sed -E 's/.*k_([0-9]+).*/\1/')

            for csv_file in "$k_dir"/*.csv; do
                [[ -f "$csv_file" ]] || continue

                echo "→ Adding $csv_file"

                # Add each line of CSV, prefixing with metadata
                tail -n +2 "$csv_file" | \
                    awk -v dim="$dim" -v beta="$beta" -v iter="$iter_name" -v K="$K" -v file="$(basename "$csv_file")" \
                        -F, '{print dim "," beta "," iter "," K "," file "," $0}' \
                    >> "$OUTPUT_CSV"
            done
        done
    done
done

echo "✅ Combined CSV created at: $OUTPUT_CSV"
