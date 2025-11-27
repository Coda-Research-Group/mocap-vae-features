#!/usr/bin/env bash
set -e

# Base directory
BASE_DIR="/storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all"

# Output directory for grouped results
OUTPUT_DIR="$BASE_DIR/grouped"
mkdir -p "$OUTPUT_DIR"

# Iterate over model directories (supports hdm05-* and pku-mmd-*)
for model_dir in "$BASE_DIR"/model=hdm05-* "$BASE_DIR"/model=pku-mmd-*; do
    [[ -d "$model_dir" ]] || continue

    model_name=$(basename "$model_dir")
    dim=$(echo "$model_name"  | sed -E 's/.*_lat-dim=([0-9]+).*/\1/')
    beta=$(echo "$model_name" | sed -E 's/.*_beta=([0-9.]+).*/\1/')

    # Go inside iteration folder (always named "3")
    iter_dir="$model_dir/3"
    [[ -d "$iter_dir" ]] || continue

    # Loop through any clustering subdir like *--kmeans.k_*
    for kdir in "$iter_dir"/*--kmeans.k_*; do
        [[ -d "$kdir" ]] || continue

        cluster_dir_name=$(basename "$kdir")
        K=$(echo "$cluster_dir_name" | sed -E 's/.*k_([0-9]+).*/\1/')

        # Create target folder
        target_dir="$OUTPUT_DIR/group_lat-dim=${dim}_beta=${beta}_k=${K}"
        mkdir -p "$target_dir"

        echo "Grouping model=$model_name, K=$K → $target_dir"

        for file in "$kdir"/*; do
            [[ -e "$file" ]] || continue
            ln -sf "$(realpath "$file")" "$target_dir/"
        done
    done
done

echo "✅ Grouping complete → $OUTPUT_DIR"
