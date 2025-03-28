#!/bin/bash
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -l walltime=24:00:00

# TODO: replace LOGIN with your login
# TODO: select a particular cluster: https://metavo.metacentrum.cz/pbsmon2/nodes/pbs

export OMP_NUM_THREADS=$PBS_NUM_PPN # set it equal to PBS variable PBS_NUM_PPN (number of CPUs in a chunk)

echo "CPUs: ${OMP_NUM_THREADS}"

SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
PBS_LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/pbs'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/logs'
ENV_NAME='experiments'

module add conda-modules

cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}

export EXPERIMENT

python /storage/brno12-cerit/home/drking/experiments/train.py --multirun exp=hdm05/all \
    latent_dim=32 beta=1 body_model=hdm05-torso &  # Run in background

wait


# LATENT_DIMS="256,128,64,32,16,8"
# BETAS="0,0.01,0.1,1,10"
# BODY_MODELS="hdm05-torso,hdm05-handL,hdm05-handR,hdm05-legL,hdm05-legR,hdm05"

# # Run experiments in parallel using background jobs
# for EXP in "fold1" "fold2" "all"; do
#     for DIM in "256" "128" "64" "32" "16" "8"; do
#         for BETA in "0" "0.01" "0.1" "1" "10"; do 
#             for MODEL in "hdm05-torso" "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR" "hdm05"; do

#     python /storage/brno12-cerit/home/drking/experiments/train.py --multirun exp=hdm05/${EXP} \
#         latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS} &  # Run in background
# done
