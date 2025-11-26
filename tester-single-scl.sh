#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=20gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

DIM=${DIM}
BETA=${BETA}
ITER=${ITER}

[ -f "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data.gz" ] && gunzip -k "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data.gz"
[ -f "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cs/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data.gz" ] && gunzip -k "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cs/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data.gz"
[ -f "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cv/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data.gz" ] && gunzip -k "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cv/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data.gz"


COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data \
-k 4 \
--scl \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/scl/hdm05/all/lat_dim=${DIM}_beta=${BETA}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/scl/hdm05/all/lat_dim=${DIM}_beta=${BETA}/results-${ITER}.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data \
--scl \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/scl/hdm05/all/lat_dim=${DIM}_beta=${BETA}/results-${ITER}.txt"

# # ----------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cv/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data \
-cv \
-k 18 \
--scl \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
echo "${COMMAND}"
mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/results-${ITER}.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cv/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data \
-cv \
--scl \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
echo "${COMMAND}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/results-${ITER}.txt"

# # ----------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cs/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data \
-cs \
-k 18 \
--scl \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
echo "${COMMAND}"
mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/results-${ITER}.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/cs/model=pku-mmd_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_full.data \
-cs \
--scl \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
echo "${COMMAND}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/scl/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/results-${ITER}.txt"