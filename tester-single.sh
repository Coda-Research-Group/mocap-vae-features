#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

DIM=${PASSED_DIM}
BETA=${PASSED_BETA}
EXP=${PASSED_EXP}



for K in "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750" "1000" "1500" "2000"; do

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-parts-M/KMeansPivotChooser--kmeans.k_${K}.composite \
-k 4 \
--nmatches 2 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}/results.txt"

done


for K in "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750" "1000" "1500" "2000"; do

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-parts-M/KMeansPivotChooser--kmeans.k_${K}.composite \
--nmatches 2 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}/results.txt"

done
