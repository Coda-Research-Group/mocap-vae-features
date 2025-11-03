#!/bin/bash

# PATHS to scripts
WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-full.sh"
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-parts.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-parts-norm.sh"


echo "Starting submission process..."

# for DIM in "${DIMS[@]}"; do
#     for BETA in "${BETAS[@]}"; do

#         JOB_NAME="clustering_${DIM}_${BETA}_part_hdm05"

#         echo "Submitting job for body part DIM=${DIM}, BETA=${BETA}"

#         qsub \
#             -N "${JOB_NAME}" \
#             -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#             "${PARTS_SCRIPT_PATH}"

#         JOB_NAME="clustering_${DIM}_${BETA}_part_hdm05_norm"

#         echo "Submitting job for body part DIM=${DIM}, BETA=${BETA}"

#         qsub \
#             -N "${JOB_NAME}" \
#             -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#             "${PARTS_SCRIPT_PATH_NORM}"

#     done
# done

echo "Halfway there"

for ITER in 1 2 3 4 5; do

    JOB_NAME="clustering_full_hdm05_iter_${ITER}"

    qsub \
        -N "${JOB_NAME}" \
        -v "PASSED_ITER=${ITER}" \
        "${WHOLE_SCRIPT_PATH}"
done

echo "Job finished."

