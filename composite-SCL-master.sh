#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb




DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")
MODELS=("hdm05-torso" "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR")



PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"


for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
      	for MODEL in "${MODELS[@]}"; do

        FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_model=${MODEL}.data.gz"
		gunzip "${FILE_TO_CONVERT}"

        FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=${MODEL}.data.gz"
		gunzip "${FILE_TO_CONVERT}"

		done
    done
done


echo "Start clustering parts..."
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/composite-SCL.py"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/composite-SCL-norm.py"


for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
 
        python ${PARTS_SCRIPT_PATH} /storage/brno12-cerit/home/drking/experiments/SCL-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/ -o /storage/brno12-cerit/home/drking/experiments/SCL-composites/hdm05-all-${DIM}-${BETA}.data

        python ${PARTS_SCRIPT_PATH_NORM} /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/hdm05/all/lat_dim=${DIM}_beta=${BETA}/ -o /storage/brno12-cerit/home/drking/experiments/SCL-composites/hdm05-all-${DIM}-${BETA}-norm.data


    done
done

echo "finished hdm05"

MODELS=("pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR")

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
      	for MODEL in "${MODELS[@]}"; do

        FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_model=${MODEL}.data.gz"
		gunzip "${FILE_TO_CONVERT}"

        FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=${MODEL}.data.gz"
		gunzip "${FILE_TO_CONVERT}"

        FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_model=${MODEL}.data.gz"
		gunzip "${FILE_TO_CONVERT}"

        FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=${MODEL}.data.gz"
		gunzip "${FILE_TO_CONVERT}"

		done
    done
done

echo "unziped pku"

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
 
        python ${PARTS_SCRIPT_PATH} /storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/ -o /storage/brno12-cerit/home/drking/experiments/SCL-composites/pku-mmd-cs-${DIM}-${BETA}.data

        python ${PARTS_SCRIPT_PATH_NORM} /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/ -o /storage/brno12-cerit/home/drking/experiments/SCL-composites/pku-mmd-cs-${DIM}-${BETA}-norm.data

        python ${PARTS_SCRIPT_PATH} /storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/ -o /storage/brno12-cerit/home/drking/experiments/SCL-composites/pku-mmd-cv-${DIM}-${BETA}.data

        python ${PARTS_SCRIPT_PATH_NORM} /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/ -o /storage/brno12-cerit/home/drking/experiments/SCL-composites/pku-mmd-cv-${DIM}-${BETA}-norm.data

    done
done