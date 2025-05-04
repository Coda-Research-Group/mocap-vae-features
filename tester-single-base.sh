#!/bin/bash
#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'



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


# if [[ "${SET}" == "all" ]]; then
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/data/hdm05/class130-actions-coords_normPOS-fps12.data \
# --base \
# -k 4 \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/all"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/all/results-4.txt"
# fi                    
# if [[ "${SET}" == "cv" ]]; then
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/data/pku-mmd/actions-single-subject-all-POS-fps10.data \
# --base \
# -cv \
# -k 4 \
# -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cv"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cv/results-4.txt"
# fi
# if [[ "${SET}" == "cs" ]]; then
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/data/pku-mmd/actions-single-subject-all-POS-fps10.data \
# --base \
# -cs \
# -k 4 \
# -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cs"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cs/results-4.txt"
# fi
# #---------------------------------------------------------------------------------------------------------------
# if [[ "${SET}" == "alln" ]]; then
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/data/hdm05/class130-actions-coords_normPOS-fps12.data \
# --base \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/all"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/all/results-n.txt"
# fi                    
# if [[ "${SET}" == "cvn" ]]; then
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/data/pku-mmd/actions-single-subject-all-POS-fps10.data \
# --base \
# -cv \
# -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cv"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cv/results-n.txt"
# fi
# if [[ "${SET}" == "csn" ]]; then
#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/data/pku-mmd/actions-single-subject-all-POS-fps10.data \
# --base \
# -cs \
# -dd /storage/brno12-cerit/home/drking/data/pku-mmd/category_description.txt \
# "
#     echo "${COMMAND}"
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cs"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/cs/results-n.txt"
# fi

for K in "100" "200" "350" "500" "750" "1000" "1500" "3000"; do

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
-fp /storage/brno12-cerit/home/drking/experiments/baseline-skeleton/hdm05/all/composites/KMeansPivotChooser--kmeans.k_${K}/1.D0K1 \
--nmatches 2 \
-k 4 \
-dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
"
    echo "${COMMAND}"
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/hdm05/all/results"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/hdm05/all/results/results.txt"

done