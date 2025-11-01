#!/bin/bash

 
EXPS=("cs" "cv")
DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")
MODELS=("pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR")


WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/vae-scripts/single_pku_job.sh"

PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting submission process..."

for EXP in "${EXPS[@]}"; do
    for DIM in "${DIMS[@]}"; do
        for BETA in "${BETAS[@]}"; do
            for MOD in "${MODELS[@]}"; do 

                JOB_NAME="train_vae_pku-mmd_${EXP}_${DIM}_${BETA}_${MOD}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA},PASSED_MODEL=${MOD}" \
                    "${WORKER_SCRIPT_PATH}"

            done
        done
    done
done


# DIMS=("256" "128" "64" "32" "16" "8" "4")
# MODELS=("pku-mmd")

# for EXP in "${EXPS[@]}"; do
#     for DIM in "${DIMS[@]}"; do
#         for BETA in "${BETAS[@]}"; do
#             for MOD in "${MODELS[@]}"; do 

#                 JOB_NAME="vae_pku-mmd_${EXP}_${DIM}_${BETA}_${MOD}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA},PASSED_MODEL=${MOD}" \
#                     "${WORKER_SCRIPT_PATH}"

#             done
#         done
#     done
# done


