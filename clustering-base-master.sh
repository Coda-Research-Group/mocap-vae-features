#!/bin/bash
#PBS -l walltime=2:0:0
#PBS -l select=1:ncpus=4:mem=4gb




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


# echo "Start creating train splits..."

# python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/create-n-fold-cross-validation-data-pku-full.py
# python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/create-n-fold-cross-validation-data-pku-parts.py

echo "Start clustering parts..."
# PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-pku-parts.sh"
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-origin.sh"
PARTS_SCRIPT_PATH2="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-origin-hdm.sh"

for K in "100" "200" "350" "500" "750" "1000" "1500" "3000" ; do

        JOB_NAME="clustering_${K}_base"

        echo "Submitting job for body part ${K}"

        qsub \
            -N "${JOB_NAME}-pku" \
            -v "PASSED_K=${K}" \
            "${PARTS_SCRIPT_PATH}"

        qsub \
            -N "${JOB_NAME}-hdm" \
            -v "PASSED_K=${K}" \
            "${PARTS_SCRIPT_PATH2}"

done


echo "Job finished."

