#!/bin/bash


EXPS=("all" "cs" "cv")
DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")

# PATHS to scripts
WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/single_job.sh"
WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote.sh"

PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting submission process..."

for EXP in "${EXPS[@]}"; do
    for DIM in "${DIMS[@]}"; do
        for BETA in "${BETAS[@]}"; do

            JOB_NAME="clustering_${EXP}_${DIM}_${BETA}"

            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"

            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH}"

        done
    done
done


for EXP in "${EXPS[@]}"; do
    for BETA in "${BETAS[@]}"; do

        JOB_NAME="vae_${EXP}_${DIM}_${BETA}"

        echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"

        qsub \
            -N "${JOB_NAME}" \
            -v "PASSED_EXP=${EXP},PASSED_BETA=${BETA}" \
            "${WHOLE_SCRIPT_PATH}"

    done
done

