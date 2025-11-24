#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=20gb
#PBS -o /dev/null
#PBS -e /dev/null

#######################################
function single() {

    rm -f "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
    -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
    --nmatches 2 \
    -k 18 \
    ${SETUP} \
    "
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
    -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
    --nmatches 2 \
    -${SETUP} \
    "
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/pku-mmd/multi-overlay/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"
}

for PART in "pku-mmd"; do
    for BETA in "0.1" "1"; do
        for DIM in 256; do
            for SETUP in "cs" "cv"; do
                for K in 100 200 400 800 1600; do 
                    single
                done 
            done
        done
    done
done

