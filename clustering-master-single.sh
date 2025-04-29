#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb






PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting unzip process..."


PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-remote-pku-full.sh"
DIMS=("256")
BETAS=("1")
MODELS=("pku-mmd")
KS=("1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000")
DATAS=("cs" "cv")

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
        for K in "${KS[@]}"; do
            for MODEL in "${MODELS[@]}"; do
                for DATA in "${DATAS[@]}"; do

                    JOB_NAME="clustering_${DIM}_${BETA}_${MODEL}_${DATA}_${K}"

                    if [[ "${DATA}" == "all" ]]; then
                        DATA="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${MODEL}.data"
                        ROOT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${MODEL}"
                    fi                    
                    if [[ "${DATA}" == "cv" ]]; then
                        DATA="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${MODEL}.data-cv-train"
                        ROOT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${MODEL}"
                    fi
                    if [[ "${DATA}" == "cs" ]]; then
                        DATA="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${MODEL}.data-cs-train"
                        ROOT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${MODEL}"
                    fi

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "PASSED_DATA=${DATA},PASSED_ROOT=${ROOT},PASSED_K=${K}" \
                        "${PARTS_SCRIPT_PATH}"

                done
            done
        done
    done
done


echo "Job finished."

