#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=16:mem=64gb
#PBS -o /dev/null
#PBS -e /dev/null

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'
SINGLE_JOB='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/single_precision.sh'

module add conda-modules
module add mambaforge

ITER=${ITERATION}
DIM=${DIMENSION}
BETA=${BETA}
FILE=${FILE}

case "$FILE" in
  "parts_norm/motion_hands_l_norm") VAR="hdm05-handL" ;;
  "parts_norm/motion_hands_r_norm") VAR="hdm05-handR" ;;
  "parts_norm/motion_legs_l_norm")  VAR="hdm05-legL" ;;
  "parts_norm/motion_legs_r_norm")  VAR="hdm05-legR" ;;
  "parts_norm/motion_torso_norm")   VAR="hdm05-torso" ;;
  "class130-actions-segment80_shift16-coords_normPOS-fps12") VAR="hdm05" ;;
  *)
    echo "Unknown FILE: $FILE"
    exit 1
    ;;
esac


conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}

python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/scl_thresholds.py \
    /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${VAR}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz \
    --subset-size 20000 --subset-repeats 5 --n-jobs 16 --output /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${VAR}_lat-dim=${DIM}_beta=${BETA}/${ITER}/scl.json


python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/precision-recall.py \
    /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.npz \
    /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${VAR}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz \
    /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.json \
    /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${VAR}_lat-dim=${DIM}_beta=${BETA}/${ITER}/scl.json \
    --dataset hdm05 --n-subsets 5 --subset-size 5000
