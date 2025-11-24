#!/usr/bin/env bash
set -euo pipefail

# Base directories
BASE_DIR="/storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/cs"
OUTPUT_DIR="$BASE_DIR/grouped"
mkdir -p "$OUTPUT_DIR"

# Only process global models (ignore body-part models)
for model_dir in "$BASE_DIR"/model=pku-mmd_lat-dim=*; do
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
        for k_dir in "$iter_dir"/KMedoidsFastPAM--kmeans.k_*; do
            [[ -d "$k_dir" ]] || continue

            K=$(basename "$k_dir" | sed -E 's/.*k_([0-9]+).*/\1/')

            # Target folder for grouped results
            target_dir="$OUTPUT_DIR/model=pku-mmd_lat-dim=${dim}_beta=${beta}_k=${K}"
            mkdir -p "$target_dir"

            echo "→ Grouping iteration ${iter_name} into $target_dir"

            # Symlink files (prefix with iteration number)
            for file in "$k_dir"/*; do
                [[ -e "$file" ]] || continue
                base=$(basename "$file")
                ln -sf "$(realpath "$file")" "$target_dir/${iter_name}_${base}"
            done
        done
    done
done

echo "✅ Grouping complete. Output in: $OUTPUT_DIR"
