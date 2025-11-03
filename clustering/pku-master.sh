#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb


DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")
MODELS=("pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR")



PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting unzip process..."

# for DIM in "${DIMS[@]}"; do
#     for BETA in "${BETAS[@]}"; do
#       	for MODEL in "${MODELS[@]}"; do

#         	FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model_norm=${MODEL}.data"
# 			gunzip -k "${FILE_TO_CONVERT}.gz"

# 		done
#     done
# done


echo "Start creating train splits..."

# python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/create-n-fold-cross-validation-data-pku-full.py
python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/create-n-fold-cross-validation-data-pku-parts.py

echo "Start clustering parts..."
# PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-pku-parts.sh"
PARTS_SCRIPT_PATH2="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-pku-full.sh"
DIMS=("64" "32" "16" "8" "4")

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do

        JOB_NAME="clustering_${DIM}_${BETA}_parts"

        echo "Submitting job for body part DIM=${DIM}, BETA=${BETA}"

        # qsub \
        #     -N "${JOB_NAME}-cs" \
        #     -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
        #     "${PARTS_SCRIPT_PATH}"

        qsub \
            -N "${JOB_NAME}-cv" \
            -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
            "${PARTS_SCRIPT_PATH2}"

    done
done


echo "Job finished."

