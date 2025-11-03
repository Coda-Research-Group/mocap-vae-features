#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=10:mem=16gb
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
SETUP=${SETUP}

case "$DATAFILE" in
  "pku-mmd-handL") PATH_PART="parts/${SETUP}/motion_hands_l" ;;
  "pku-mmd-handR") PATH_PART="parts/${SETUP}/motion_hands_r" ;;
  "pku-mmd-legL")  PATH_PART="parts/${SETUP}/motion_legs_l" ;;
  "pku-mmd-legR")  PATH_PART="parts/${SETUP}/motion_legs_r" ;;
  "pku-mmd-torso") PATH_PART="parts/${SETUP}/motion_torso" ;;
  "pku-mmd")       PATH_PART="actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10" ;;
  *)
    echo "Unknown DATAFILE: $DATAFILE"
    exit 7
    ;;
esac

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 8
}

python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/data-splitting-scripts/train-test-splitting.py \
    /storage/brno12-cerit/home/drking/data/pku-mmd/splits/${SETUP}_train_objects_messif-lines.txt \
    /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz \
    /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data-train


python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-non-norm-quality/scl_thresholds.py \
    /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data-train \
    --subset-size 30000 --runs 5 \
    --output /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/scl.json

python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-non-norm-quality/precision-recall.py \
    /storage/brno12-cerit/home/drking/data/pku-mmd/${PATH_PART}.data-train \
    /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data-train \
    /storage/brno12-cerit/home/drking/data/pku-mmd/${PATH_PART}.json \
    /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/scl.json \
    --dataset pku-mmd --n-subsets 5 --subset-size 5000 --n-jobs 10 \
    --output /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${DATAFILE}_lat-dim=${DIM}_beta=${BETA}/${ITER}/metrics.json
