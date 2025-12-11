#!/bin/bash

#PBS -q gpu@pbs-m1.metacentrum.cz
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=2:ngpus=1:mem=16gb:gpu_mem=8gb:scratch_local=50gb:cuda_version=13.0
#PBS -o /dev/null
#PBS -e /dev/null

#This script uses conda eviroment available on metacentrum.cz
#TODO: edit this path to the location of the repository
REPO_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
ENV_NAME='cuda4'


module add conda-modules
module add mambaforge

cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}



python ${REPO_DIR}/mocap-vae-features/train.py --multirun exp=hdm05/all \
    latent_dim=256 beta=0.1 iteration=1 body_model=hdm05 > /dev/null 2>&1




wait
