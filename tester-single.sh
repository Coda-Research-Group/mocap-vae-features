#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

DIM=${PASSED_DIM}
BETA=${PASSED_BETA}
EXP=${PASSED_EXP}



for K in "1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000"; do

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}/MWs-parts/KMeansPivotChooser--kmeans.k_${K}.composite \
-k 4 \
-cv \
--nmatches 2 \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}/results.txt"

done


for K in "1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000"; do

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}/MWs-parts/KMeansPivotChooser--kmeans.k_${K}.composite \
-cv \
--nmatches 2 \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}/results.txt"

done




