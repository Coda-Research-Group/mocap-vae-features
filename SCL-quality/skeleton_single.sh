#!/bin/bash

#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=16:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

DATAFILE=${DATAFILE}

module add conda-modules
module add mambaforge

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}

for SETUP in "cv" "cs"; do
    for NORM in "parts"; do
        python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py \
            --input /storage/brno12-cerit/home/drking/data/pku-mmd/${NORM}/${SETUP}/${DATAFILE}.data-train \
            --subset-size 5000 --subset-repeats 5 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/${NORM}/${SETUP}/${DATAFILE}.json
    done
done
