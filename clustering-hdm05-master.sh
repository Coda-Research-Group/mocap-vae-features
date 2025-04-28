#!/bin/bash



DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")

# PATHS to scripts
WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-full.sh"
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-parts.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-parts-norm.sh"
PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting submission process..."

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do

        JOB_NAME="clustering_${DIM}_${BETA}_part_hdm05"

        echo "Submitting job for body part DIM=${DIM}, BETA=${BETA}"

        qsub \
            -N "${JOB_NAME}" \
            -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
            "${PARTS_SCRIPT_PATH}"

        JOB_NAME="clustering_${DIM}_${BETA}_part_hdm05_norm"

        echo "Submitting job for body part DIM=${DIM}, BETA=${BETA}"

        qsub \
            -N "${JOB_NAME}" \
            -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
            "${PARTS_SCRIPT_PATH_NORM}"

    done
done

echo "Halfway there"

DIMS=("256" "128" "64" "32" "16" "8" "4" "2" "1")

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do

        JOB_NAME="clustering_${DIM}_${BETA}_full_hdm05"

        echo "Submitting job for full body DIM=${DIM}, BETA=${BETA}"

        qsub \
            -N "${JOB_NAME}" \
            -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
            "${WHOLE_SCRIPT_PATH}"

    done
done

echo "Job finished."

