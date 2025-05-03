#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

DIM=${PASSED_DIM}
BETA=${PASSED_BETA}


[ -f "/storage/brno12-cerit/home/drking/experiments/SCL-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_model=hdm05.data.gz" ] && gunzip "/storage/brno12-cerit/home/drking/experiments/SCL-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_model=hdm05.data.gz"
[ -f "/storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_model=pku-mmd.data.gz" ] && gunzip "/storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_model=pku-mmd.data.gz"
[ -f "/storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_model=pku-mmd.data.gz" ] && gunzip "/storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_model=pku-mmd.data.gz"


# COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=hdm05.data \
# -k 4 \
# --scl \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
# echo "${COMMAND}"
# mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}"
# eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}/scl.txt"

# COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=hdm05.data \
# --scl \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
# echo "${COMMAND}"
# mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}"
# eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/all/lat_dim=${DIM}_beta=${BETA}/scl.txt"

# # ----------------------------------------------------------------------------------------

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=pku-mmd.data \
-cv \
-k 4 \
--scl \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
echo "${COMMAND}"
mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/scl-redo.txt"

COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/SCL-actions-norm/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_model_norm=pku-mmd.data \
-cv \
--scl \
-dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
"
echo "${COMMAND}"
mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}"
eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/scl-redo.txt"

# # ----------------------------------------------------------------------------------------

# COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_model=pku-mmd.data \
# -cs \
# -k 4 \
# --scl \
# -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
# "
# echo "${COMMAND}"
# mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}"
# eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/scl.txt"

# COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/SCL-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_model=pku-mmd.data \
# -cs \
# --scl \
# -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
# "
# echo "${COMMAND}"
# mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}"
# eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/scl.txt"