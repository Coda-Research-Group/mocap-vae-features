#!/bin/bash

#PBS -l walltime=8:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null

JDK_PATH="/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java"


#######################################
function single() {

    rm -f "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/multi-overlay/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/model=hdm05_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
    -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
    --nmatches 2 \
    -k 4 \
    "
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/multi-overlay/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/multi-overlay/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/model=hdm05_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
    -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
    --nmatches 2 \
    "
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/multi-overlay/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results.txt"
}

for PART in "hdm05"; do
    for BETA in "0.1" "1"; do
        for DIM in 256; do
            for K in 100 200 400 800 1600; do 
                single
            done
        done
    done
done

