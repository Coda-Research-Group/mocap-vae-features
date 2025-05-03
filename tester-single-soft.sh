#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'



for SOFTASSIGNPARAM in "D0.5K5" "D0.1K5" "D0.05K5"; do
    K="400"
    DIM="256"
    BETA="1"
    PART="hdm05"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-soft/hdm05.${SOFTASSIGNPARAM} \
-k 4 \
--soft \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/soft"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/soft/results.txt"


    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-soft/hdm05.${SOFTASSIGNPARAM} \
--soft \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/soft/results.txt"

# ----------------------------------------------------------------------------------------------------------------
    
    K="750"
    DIM="256"
    BETA="0.1"
    PART="pku-mmd"    
    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/MWs-soft/pku-mmd.${SOFTASSIGNPARAM} \
-k 4 \
--soft \
-cv \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/soft"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/soft/results.txt"


    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/MWs-soft/pku-mmd.${SOFTASSIGNPARAM} \
--soft \
-cv \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    echo "${COMMAND}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/soft/results.txt"


# --------------------------------------------------------------------------------------------------------------

    K="500"
    DIM="256"
    BETA="0.1"
    PART="pku-mmd"    
    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/MWs-soft/pku-mmd.${SOFTASSIGNPARAM} \
-k 4 \
--soft \
-cs \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/soft"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/soft/results.txt"


    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/MWs-soft/pku-mmd.${SOFTASSIGNPARAM} \
--soft \
-cs \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
    echo "${COMMAND}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/soft/results.txt"

done
