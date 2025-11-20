#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'
DIM=${DIM}
BETA=${BETA}


# for K in "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750" "1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000"; do

for K in 100; do

# # Precision

#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}_non-norm.composite \
# --nmatches 2 \
# -k 4 \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/Multi-Overlay/parts"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/Multi-Overlay/parts/dim=${DIM}_beta=${BETA}_${K}_non-norm.txt"

# # recall
# #---------------------------------------------------
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}_non-norm.composite \
# --nmatches 2 \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/Multi-Overlay/parts/dim=${DIM}_beta=${BETA}_${K}_non-norm.txt"


# # Normed
# # Precision

#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
# --nmatches 2 \
# -k 4 \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/Multi-Overlay/parts/dim=${DIM}_beta=${BETA}_${K}.txt"

# # recall
# #---------------------------------------------------
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
# --nmatches 2 \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/Multi-Overlay/parts/dim=${DIM}_beta=${BETA}_${K}.txt"


# Precision

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/model=hdm05_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
--nmatches 2 \
-k 4 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/Multi-Overlay/full"

    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/Multi-Overlay/full/dim=${DIM}_beta=${BETA}_${K}.txt"

# recall
#---------------------------------------------------
    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/model=hdm05_lat-dim=${DIM}_beta=${BETA}_k=${K}.composite \
--nmatches 1 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/Multi-Overlay/full/dim=${DIM}_beta=${BETA}_${K}.txt"
done

