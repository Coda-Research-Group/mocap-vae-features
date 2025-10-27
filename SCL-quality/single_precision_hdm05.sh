#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=16:mem=64gb
#PBS -o /dev/null
#PBS -e /dev/null

module add conda-modules
module add mambaforge

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

ITER=${ITERATION}
DIM=${DIMENSION}
BETA=${BETA}
DATAFILE=${DATAFILE}

case "$DATAFILE" in
  "hdm05-handL") PATH_PART="parts_norm/motion_hands_l_norm" ;;
  "hdm05-handR") PATH_PART="parts_norm/motion_hands_r_norm" ;;
  "hdm05-legL")  PATH_PART="parts_norm/motion_legs_l_norm" ;;
  "hdm05-legR")  PATH_PART="parts_norm/motion_legs_r_norm" ;;
  "hdm05-torso") PATH_PART="parts_norm/motion_torso_norm" ;;
  "hdm05")       PATH_PART="class130-actions-segment80_shift16-coords_normPOS-fps12" ;;
  *)
    echo "Unknown DATAFILE: $DATAFILE"
    exit 7
    ;;
esac

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}

python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/scl_thresholds.py \
    /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz \
    --subset-size 20000 --subset-repeats 5 --n-jobs 16 \
    --output /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/scl.json

wait 

python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/precision-recall.py \
    /storage/brno12-cerit/home/drking/data/hdm05/${PATH_PART}.npz \
    /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz \
    /storage/brno12-cerit/home/drking/data/hdm05/${PATH_PART}.json \
    /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/scl.json \
    --dataset hdm05 --n-subsets 5 --subset-size 5000
