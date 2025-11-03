#!/bin/bash



DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")

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

# for K in 2 3 4 5 6 7 8 9 10 20 50 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2250; do
#     for ITER in 1 2 3 4 5; do
for K in 350; do
    for ITER in 1; do

        JOB_NAME="clustering_full_hdm05_it${ITER}_${K}"

        qsub \
            -N "${JOB_NAME}" \
            -v "PASSED_K=${K},PASSED_ITER=${ITER}" \
            "${WHOLE_SCRIPT_PATH}"
    done
done

echo "Job finished."

