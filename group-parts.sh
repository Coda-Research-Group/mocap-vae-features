#!/usr/bin/env bash
set -e

# Base directory (current dir by default)
BASE_DIR="/storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all"

# Output directory for grouped results
OUTPUT_DIR="$BASE_DIR/grouped"
mkdir -p "$OUTPUT_DIR"

# Iterate over all model directories
for model_dir in "$BASE_DIR"/model=hdm05-*; do
    [[ -d "$model_dir" ]] || continue

    # Extract parameters
    model_name=$(basename "$model_dir")
    dim=$(echo "$model_name" | sed -E 's/.*_lat-dim=([0-9]+).*/\1/')
    beta=$(echo "$model_name" | sed -E 's/.*_beta=([0-9.]+).*/\1/')
    non_norm=""
    [[ "$model_name" == *non-norm* ]] && non_norm="_non-norm"

    # Loop through KMeans subdirs (e.g. KMeansPivotChooser--kmeans.k_100)
    for kdir in "$model_dir"/KMeansPivotChooser--kmeans.k_*; do
        [[ -d "$kdir" ]] || continue

        # Extract K value
        K=$(basename "$kdir" | sed -E 's/.*k_([0-9]+).*/\1/')

        # Create target folder for this (dim, beta, K, norm)
        target_dir="$OUTPUT_DIR/group_lat-dim=${dim}_beta=${beta}_k=${K}${non_norm}"
        mkdir -p "$target_dir"

        # Copy or link all files from this subdir into the target
        # To save space, we’ll use symlinks (change to 'cp -r' if you prefer copies)
        echo "Grouping: $model_dir → $target_dir"
        for file in "$kdir"/*; do
            [[ -e "$file" ]] || continue
            ln -sf "$(realpath "$file")" "$target_dir/"
        done
    done
done

echo "✅ Grouping complete. Results in: $OUTPUT_DIR"