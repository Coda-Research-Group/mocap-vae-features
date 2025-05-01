#!/bin/bash
#PBS -l walltime=4:0:0
#PBS -l select=1:ncpus=4:mem=4gb


PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting unzip process..."


PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-pku-full.sh"
DIMS=("256" "128" "64" "32" "16" "8")
BETAS=("0.1" "1" "10")
MODELS=("1" "2" "3" "4" "5")
# KS=("1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000")
KS=("350" "1000")
DATAS=("all" "cs" "cv")



for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
        for K in "${KS[@]}"; do
            for MODEL in "${MODELS[@]}"; do
                for DATA in "${DATAS[@]}"; do

                    JOB_NAME="clustering_${DIM}_${BETA}_${MODEL}_${DATA}_${K}"

                    if [[ "${DATA}" == "all" ]]; then
                        DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=hdm05.data"
                        ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
                    fi                    
                    if [[ "${DATA}" == "cv" ]]; then
                        DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=pku-mmd.data-cv-train"
                        ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
                    fi
                    if [[ "${DATA}" == "cs" ]]; then
                        DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=pku-mmd.data-cs-train"
                        ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
                    fi

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "PASSED_DATA=${DATA_DIR},PASSED_ROOT=${ROOT_DIR},PASSED_K=${K}" \
                        "${PARTS_SCRIPT_PATH}"

                done
            done
        done
    done
done

# for DIM in "${DIMS[@]}"; do
#     for BETA in "${BETAS[@]}"; do
#         for K in "${KS[@]}"; do
#             for MODEL in "${MODELS[@]}"; do
#                 for DATA in "${DATAS[@]}"; do

#                     JOB_NAME="clustering_${DIM}_${BETA}_${MODEL}_${DATA}_${K}"

#                     if [[ "${DATA}" == "all" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${MODEL}.data"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi                    
#                     if [[ "${DATA}" == "cv" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${MODEL}.data-cv-train"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi
#                     if [[ "${DATA}" == "cs" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${MODEL}.data-cs-train"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi

#                     qsub \
#                         -N "${JOB_NAME}" \
#                         -v "PASSED_DATA=${DATA_DIR},PASSED_ROOT=${ROOT_DIR},PASSED_K=${K}" \
#                         "${PARTS_SCRIPT_PATH}"

#                 done
#             done
#         done
#     done
# done

# DIMS=("64" "32" "16" "8" "4")
# MODELS=("hdm05-legR" "hdm05-legL" "hdm05-torso" "hdm05-handL" "hdm05-handR")
# # KS=("50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750")

# for DIM in "${DIMS[@]}"; do
#     for BETA in "${BETAS[@]}"; do
#         for K in "${KS[@]}"; do
#             for MODEL in "${MODELS[@]}"; do
#                 for DATA in "${DATAS[@]}"; do

#                     JOB_NAME="clustering_${DIM}_${BETA}_${MODEL}_${DATA}_${K}"

#                     if [[ "${DATA}" == "all" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${MODEL}.data"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi                    
#                     if [[ "${DATA}" == "cv" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${MODEL}.data-cv-train"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi
#                     if [[ "${DATA}" == "cs" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${MODEL}.data-cs-train"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi

#                     qsub \
#                         -N "${JOB_NAME}" \
#                         -v "PASSED_DATA=${DATA_DIR},PASSED_ROOT=${ROOT_DIR},PASSED_K=${K}" \
#                         "${PARTS_SCRIPT_PATH}"

#                 done
#             done
#         done
#     done
# done

# for DIM in "${DIMS[@]}"; do
#     for BETA in "${BETAS[@]}"; do
#         for K in "${KS[@]}"; do
#             for MODEL in "${MODELS[@]}"; do
#                 for DATA in "${DATAS[@]}"; do

#                     JOB_NAME="clustering_${DIM}_${BETA}_${MODEL}_${DATA}_${K}_norm"

#                     if [[ "${DATA}" == "all" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model_norm=${MODEL}.data"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/hdm05/all/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi                    
#                     if [[ "${DATA}" == "cv" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model_norm=${MODEL}.data-cv-train"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi
#                     if [[ "${DATA}" == "cs" ]]; then
#                         DATA_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model_norm=${MODEL}.data-cs-train"
#                         ROOT_DIR="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}"
#                     fi

#                     qsub \
#                         -N "${JOB_NAME}" \
#                         -v "PASSED_DATA=${DATA_DIR},PASSED_ROOT=${ROOT_DIR},PASSED_K=${K}" \
#                         "${PARTS_SCRIPT_PATH}"

#                 done
#             done
#         done
#     done
# done



echo "Job finished."

