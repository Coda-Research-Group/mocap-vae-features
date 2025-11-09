#!/bin/bash

# PATHS to scripts
WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-full.sh"
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-parts.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-parts-norm.sh"


echo "Starting submission process..."

# for PART in "hdm05-handR" "hdm05-handL" "hdm05-legR" "hdm05-legL" "hdm05-torso"; do
#     for BETA in "0.1" "1"; do 
#         for DIM in 8 16; do 
#             for ITER in 3; do

#                 JOB_NAME="clustering_${PART}_${ITER}_${DIM}_${BETA}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM},PASSED_PART=${PART}" \
#                     "${PARTS_SCRIPT_PATH}"

#                 JOB_NAME="clustering_${PART}_norm-${ITER}_${DIM}_${BETA}"


#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM},PASSED_PART=${PART}" \
#                     "${PARTS_SCRIPT_PATH_NORM}"


#             done
#         done
#     done
# done

echo "Halfway there"


for ITER in 3 ; do
    for BETA in "0.1" "1"; do
        for DIM in 256; do

            JOB_NAME="clustering_full_hdm05_${ITER}__${DIM}_${BETA}"

            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM}" \
                "${WHOLE_SCRIPT_PATH}"

        done
    done
done


echo "Job finished."

