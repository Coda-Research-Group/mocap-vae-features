#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

DIM=${PASSED_DIM}
BETA=${PASSED_BETA}
ITER=${PASSED_ITER}


for K in 10 20 35 50 60 80 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500; do

    rm "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/${PART}_lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K} \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
-k 4 \
"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/${PART}_lat_dim=${DIM}_beta=${BETA}/${K}/"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/${PART}_lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K} \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/${PART}_lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt"

done
