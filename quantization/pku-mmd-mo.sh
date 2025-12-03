#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=20gb
#PBS -o /dev/null
#PBS -e /dev/null

DIM=${DIM}
BETA=${BETA}
K=${K}
SETUP=${SETUP}
JDK_PATH="/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java"

#######################################
function single() {

    # rm -f "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
    -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
    --nmatches 2 \
    -k 18 \
    -${SETUP} \
    "
    # mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results-classification.txt"

    # COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    # -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
    # -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
    # --nmatches 2 \
    # -${SETUP} \
    # "
    # eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results-classification.txt"
}

for PART in "pku-mmd"; do

    single

done

