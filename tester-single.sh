#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

ITER=${PASSED_ITER}



# for K in "350" "1000" ; do

#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-full-MO/KMeansPivotChooser--kmeans.k_${K}.composite \
# -k 4 \
# --nmatches 2 \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}/results-MO.txt"

# done


for K in 10 20 35 50 60 80 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500; do

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/full/lat_dim=32_beta=0.1/${ITER}/KMeansPivotChooser--kmeans.k_${K}/full.D0K1 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/lat_dim=32_beta=0.1/${ITER}/"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/lat_dim=32_beta=0.1/${K}/results-${ITER}.txt"

done
