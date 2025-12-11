#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=8gb:cl_galdor=True
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

#--------------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/data/hdm05/class130-actions-coords_normPOS-fps12.data \
--base \
-k 4 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
mkdir -p "/storage/brno12-cerit/home/drking/experiments/efficiency"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-base.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/data/hdm05/class130-actions-coords_normPOS-fps12.data \
--base \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-base.txt"

#--------------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=256_beta=0.1/3/predictions_full.data \
-k 4 \
--scl \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-scl.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=256_beta=0.1/3/predictions_full.data \
--scl \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-scl.txt"

#---------------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /experiments/elki-MWs/hdm05/all/model=hdm05_lat-dim=256_beta=0.1/3/KMedoidsFastPAM--kmeans.k_1600/hdm05.D0K1 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
-k 4 \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-hard.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /experiments/elki-MWs/hdm05/all/model=hdm05_lat-dim=256_beta=0.1/3/KMedoidsFastPAM--kmeans.k_1600/hdm05.D0K1 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-hard.txt"

#---------------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/soft/model=hdm05_lat-dim=256_beta=0.1/3/KMedoidsFastPAM--kmeans.k_1600/hdm05.D0.35K6 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
--soft \
-k 4 \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-soft.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/soft/model=hdm05_lat-dim=256_beta=0.1/3/KMedoidsFastPAM--kmeans.k_1600/hdm05.D0.35K6 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
--soft \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-soft.txt"

#---------------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/model=hdm05_lat-dim=256_beta=0.1_k=1600.composite \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
--nmatches 2 \
-k 4 \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-m-o.txt"


COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/model=hdm05_lat-dim=256_beta=0.1_k=1600.composite \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
--nmatches 2 \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-m-o.txt"

#------------------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs-non-norm/hdm05/all/grouped/group_lat-dim=256_beta=0.1_k=1600.composite \
--nmatches 2 \
-k 4 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-composites.txt"

# recall
#---------------------------------------------------
    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs-non-norm/hdm05/all/grouped/group_lat-dim=256_beta=0.1_k=1600.composite \
--nmatches 2 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/efficiency/results-composites.txt"
