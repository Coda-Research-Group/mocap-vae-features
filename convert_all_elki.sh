#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb
#PBS -o /dev/null
#PBS -e /dev/null


SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
PBS_LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/pbs'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'


cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}

# PATHS to scripts
ELKI_CONVERT_SCRIPT="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl"

EXPS=("all" "cs" "cv")
DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")
MODELS=("pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR")


#for EXP in "${EXPS[@]}"; do
#    for DIM in "${DIMS[@]}"; do
#        for BETA in "${BETAS[@]}"; do
#          	for MODEL in "${MODELS[@]}"; do
#
#            	FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/predictions_segmented_model\=${MODEL}.data"
#				ELKI_FILE_TO_CREATE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/elki-predictions_segmented_model\=${MODEL}.data"
#
#				gunzip -k "${FILE_TO_CONVERT}.gz"
#
#				perl ELKI_CONVERT_SCRIPT FILE_TO_CONVERT > ELKI_FILE_TO_CREATE
#
#			done
#        done
#    done
#done

EXPS=("all")
MODELS=("hdm05-torso" "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR")

for EXP in "${EXPS[@]}"; do
    for DIM in "${DIMS[@]}"; do
        for BETA in "${BETAS[@]}"; do
          	for MODEL in "${MODELS[@]}"; do

            	FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/hdm05/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/predictions_segmented_model_norm\=${MODEL}.data"
				ELKI_FILE_TO_CREATE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/hdm05/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/elki-predictions_segmented_model_norm\=${MODEL}.data"

				gunzip "${FILE_TO_CONVERT}.gz"

				perl ELKI_CONVERT_SCRIPT FILE_TO_CONVERT > ELKI_FILE_TO_CREATE

			done
        done
    done
done

#EXPS=("all" "cs" "cv")
#DIMS=("256" "128" "64" "32" "16" "8")
#BETAS=("0.1" "1" "10")
#MODELS=("pku-mmd")
#
#for EXP in "${EXPS[@]}"; do
#    for DIM in "${DIMS[@]}"; do
#        for BETA in "${BETAS[@]}"; do
#          	for MODEL in "${MODELS[@]}"; do
#
#            	FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/predictions_segmented_model\=${MODEL}.data"
#				ELKI_FILE_TO_CREATE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/elki-predictions_segmented_model\=${MODEL}.data"
#
#				gunzip -k "${FILE_TO_CONVERT}.gz"
#
#				perl ELKI_CONVERT_SCRIPT FILE_TO_CONVERT > ELKI_FILE_TO_CREATE
#
#			done
#        done
#    done
#done

#EXPS=("all")
#DIMS=("256" "128" "64" "32" "16" "8" "4" "2" "1")
#MODELS=("hdm05")
#
#for EXP in "${EXPS[@]}"; do
#    for DIM in "${DIMS[@]}"; do
#        for BETA in "${BETAS[@]}"; do
#          	for MODEL in "${MODELS[@]}"; do
#
#            	FILE_TO_CONVERT="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/predictions_segmented_model\=${MODEL}.data"
#				ELKI_FILE_TO_CREATE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/${EXP}/lat_dim\=${DIM}_beta\=${BETA}/elki-predictions_segmented_model\=${MODEL}.data"
#
#				gunzip -k "${FILE_TO_CONVERT}.gz"
#
#				perl ELKI_CONVERT_SCRIPT FILE_TO_CONVERT > ELKI_FILE_TO_CREATE
#
#			done
#        done
#    done
#done

wait
