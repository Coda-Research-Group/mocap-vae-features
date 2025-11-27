#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'
DIM=${DIM}
BETA=${BETA}
K=${K}
SETUP=${SETUP}


# Precision
    rm -f "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite/${SETUP}/dim=${DIM}_beta=${BETA}_${K}.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
--nmatches 2 \
-k 18 \
-${SETUP} \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite/${SETUP}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite/${SETUP}/dim=${DIM}_beta=${BETA}_${K}.txt"

# recall
#---------------------------------------------------
    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
--nmatches 2 \
-${SETUP} \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite/${SETUP}/dim=${DIM}_beta=${BETA}_${K}.txt"


#=================================================================================

    rm -f "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite-non-norm/${SETUP}/dim=${DIM}_beta=${BETA}_${K}.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs-non-norm/pku-mmd/${SETUP}/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
--nmatches 2 \
-k 18 \
-${SETUP} \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite-non-norm/${SETUP}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite-non-norm/${SETUP}/dim=${DIM}_beta=${BETA}_${K}.txt"

# recall
#---------------------------------------------------
    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs-non-norm/pku-mmd/${SETUP}/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
--nmatches 2 \
-${SETUP} \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/composite-non-norm/${SETUP}/dim=${DIM}_beta=${BETA}_${K}.txt"
