#!/bin/bash

#PBS -q gpu@pbs-m1.metacentrum.cz
#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=2:ngpus=1:mem=16gb:gpu_mem=8gb:scratch_local=50gb:cuda_version=13.0
#PBS -o /dev/null
#PBS -e /dev/null

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

module add conda-modules
module add mambaforge


conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}


for ITER in "1" "2" "3" "4" "5"; do 
    for DIM in "256" "128"; do
        for BETA in "0.1" "1" "10"; do
            python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/train.py --multirun exp=hdm05/all \
                latent_dim=${DIM} beta=${BETA} iteration=${ITER} body_model=hdm05 > /dev/null 2>&1
        done
    done
done