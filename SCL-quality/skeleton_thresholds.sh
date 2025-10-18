#!/bin/bash

#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=16:ngpus=1:mem=64gb
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

python3 dtw_matcher_new.py --input /storage/brno12-cerit/home/drking/data/hdm05/parts_norm/motion_hands_l_norm.npz \
    --subset-size 2000 --subset-repeats 3 --n-jobs 8 --output /storage/brno12-cerit/home/drking/data/hdm05/parts_norm/percentile_hands_l_norm.json