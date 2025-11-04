#!/bin/bash

# PATHS to scripts
WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-full.sh"
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-parts.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-hdm05-parts-norm.sh"


echo "Starting submission process..."

# for PART in "handR" "handL" "legR" "legL" "torso"; do
#     for ITER in 1 2 3 4 5; do

#         JOB_NAME="clustering_hdm05-${PART}_iter_${ITER}"

#         qsub \
#             -N "${JOB_NAME}" \
#             -v "PASSED_ITER=${ITER},PASSED_PART=${PART}" \
#             "${PARTS_SCRIPT_PATH}"

#         JOB_NAME="clustering_hdm05-${PART}_iter_${ITER}_norm"


#         qsub \
#             -N "${JOB_NAME}" \
#             -v "PASSED_ITER=${ITER},PASSED_PART=${PART}" \
#             "${PARTS_SCRIPT_PATH_NORM}"

#     done
# done

echo "Halfway there"

for ITER in 1 2 3 4 5; do
    for BETA in "0.1" "1" "10"; do 
        for DIM in 32 64; do 

            JOB_NAME="clustering_full_hdm05_${ITER}__${DIM}_${BETA}"

            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM}" \
                "${WHOLE_SCRIPT_PATH}"

        done
    done
done


echo "Job finished."

