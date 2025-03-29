#!/bin/bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars || exit
pwd

# Which clusters to use
CLUSTERS_ROOT_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/results/KMedoidsFastPAM--kmeans.10'
# Where to store them
OUTPUT_ROOT_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/MWs'
JDK_PATH='/usr/bin/java'
MEMORY='3g'
CLASSPATH=${CLASSPATH:-'MESSIF.jar:MESSIF-Utility.jar:MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:MCDR.jar:m-index.jar:trove4j-3.0.3.jar'}
# Object class to work with in the dataset and pivots (default is messif.objects.impl.ObjectFloatVectorNeuralNetworkL2)
CLS_OBJ='mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW'
# Dataset file path - which objects to convert
DATAFILE='/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data'
# OPTIONAL - (default is D0K1)
SOFTASSIGNPARAM='D0K1'
# OPTIONAL - set TOSEQ='--tosequence' if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
TOSEQ='--tosequence'
# '-v' ~ Medoids are loaded and Voronoi diagram is created.
VOCTYPE='-v'
# Should contain the same value as ${EXTRACTED_MEDOIDS_FILE} in ./script/cluster.sh
MEDOIDS_FILENAME='medoids.txt'
# The folder name in ${CLUSTERS_ROOT_PATH} to be processed
CLUSTER_FOLDER_NAME='KMedoidsFastPAM--kmeans.k_10'
# Which joint IDs should be used, specific to each body part
FILTER_JOINT_IDS=''

## Defaults

#CLUSTERS_ROOT_PATH=${CLUSTERS_ROOT_PATH:-'/home/xprocha6/cybela1-storage/clustering-results/pku-mmd'}
#OUTPUT_ROOT_PATH=${OUTPUT_ROOT_PATH:-'/home/xprocha6/cybela1-storage/converted-mw-sequences/pku-mmd'}
#JDK_PATH=${JDK_PATH:-'/usr/bin/java'}
#MEMORY=${MEMORY:-'3g'}
#CLASSPATH=${CLASSPATH:-'MESSIF.jar:MESSIF-Utility.jar:/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/MCDR.jar:m-index.jar:trove4j-3.0.3.jar'}
#CLS_OBJ=${CLS_OBJ:-'mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTWFiltered'}
#DATAFILE=${DATAFILE:-'/home/xprocha6/cybela1-storage/datasets/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data'}
#VOCTYPE=${VOCTYPE:-'-v'}
#MEDOIDS_FILENAME=${MEDOIDS_FILENAME:-'medoids.txt'}
#CLUSTER_FOLDER_NAME=${CLUSTER_FOLDER_NAME:-'KMedoidsFastPAM--kmeans.k_3'}

## Functions

function convertBodyPart() {
    echo 'convertBodyPart'
    echo "body part IDs: ${FILTER_JOINT_IDS}"

    mkdir -p "${OUTPUT_ROOT_PATH}"

    COMMAND="\
${JDK_PATH} \
-Xmx${MEMORY} \
-cp ${CLASSPATH} \
messif.motionvocabulary.MotionVocabulary \
-d ${DATAFILE} \
-c ${CLS_OBJ} \
-filter ${FILTER_JOINT_IDS} \
--quantize ${TOSEQ} ${VOCTYPE} ${CLUSTER_FOLDER_PATH}/${MEDOIDS_FILENAME} \
--soft-assign ${SOFTASSIGNPARAM} \
--output ${OUTPUT_ROOT_PATH}/${CLUSTER_FOLDER_NAME}.${SOFTASSIGNPARAM} \
"

    echo "${COMMAND}"

    eval "${COMMAND}"
}

function convertBodyParts() {
    echo 'convertBodyParts'

    for CLUSTER_FOLDER_PATH in "${CLUSTERS_ROOT_PATH}"/*; do # take all body parts and relations inside the folder
        # for CLUSTER_FOLDER_PATH in "${CLUSTERS_ROOT_PATH}"/{'11,12,24,25,7,8,22,23','3,4,11,12,24,25','3,4,7,8,22,23'}; do # only PKU-MMD relations
        # for CLUSTER_FOLDER_PATH in "${CLUSTERS_ROOT_PATH}"/{'28,29,30,31,21,22,23,24','16,17,28,29,30,31','16,17,21,22,23,24'}; do # only HDM05 relations

        # Parses the folder name (the string after the last "/")
        CLUSTER_FOLDER_NAME=$(basename "${CLUSTER_FOLDER_PATH}")
        FILTER_JOINT_IDS="${CLUSTER_FOLDER_NAME}"

        convertBodyPart
    done
}

function convertBodyPartsCli() {
    echo "convertBodyPartsCli ${1}"
    
    # takes the body part or relation IDs as the function's argument
    INPUT="${CLUSTERS_ROOT_PATH}/${1}"

    for CLUSTER_FOLDER_PATH in ${INPUT}; do 
        # Parses the folder name (the string after the last "/")
        CLUSTER_FOLDER_NAME=$(basename "${CLUSTER_FOLDER_PATH}")
        FILTER_JOINT_IDS="${CLUSTER_FOLDER_NAME}"

        convertBodyPart
    done
}

# HDM05-130 - 2-fold cross validation - folds: '0' '1'
 for K in '10'; do
     for SPLIT in '0' '1' '2' '3' '4'; do
         # for SPLIT in '4'; do

         ## Training data for fold 0:
         DATAFILE="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data-split${SPLIT}-fold0"
         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/toMWs/split${SPLIT}-fold0/KMeansPivotChooser--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/results/split${SPLIT}-fold0/KMeansPivotChooser--kmeans.k_${K}-train"
         convertBodyParts

         ## Testing data for fold 0:
         DATAFILE="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data-split${SPLIT}-fold1"
         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/toMWs/split${SPLIT}-fold0/KMeansPivotChooser--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/results/split${SPLIT}-fold0/KMeansPivotChooser--kmeans.k_${K}-test"
         convertBodyParts

         ## Training data for fold 1:
         DATAFILE="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data-split${SPLIT}-fold1"
         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/toMWs/split${SPLIT}-fold1/KMeansPivotChooser--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/results/split${SPLIT}-fold1/KMeansPivotChooser--kmeans.k_${K}-train"
         convertBodyParts

         ## Testing data for fold 1:
         DATAFILE="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data-split${SPLIT}-fold0"
         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/toMWs/split${SPLIT}-fold1/KMeansPivotChooser--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/results/split${SPLIT}-fold1/KMeansPivotChooser--kmeans.k_${K}-test"
         convertBodyParts

     done
 done

# HDM05-65 - 10-fold cross validation - folds: '0,1,2,3,4,5,6,7,8' '0,1,2,3,4,5,6,7,9' '0,1,2,3,4,5,6,8,9' '0,1,2,3,4,5,7,8,9' '0,1,2,3,4,6,7,8,9' '0,1,2,3,5,6,7,8,9' '0,1,2,4,5,6,7,8,9' '0,1,3,4,5,6,7,8,9' '0,2,3,4,5,6,7,8,9' '1,2,3,4,5,6,7,8,9'
#for K in '250' '300'; do
#
#    SPLIT='4' # '0' '1' '2' '3' '4'
#
#    declare -a FOLDS
#    FOLDS=('1,2,3,4,5,6,7,8,9' '0,2,3,4,5,6,7,8,9' '0,1,3,4,5,6,7,8,9' '0,1,2,4,5,6,7,8,9' '0,1,2,3,5,6,7,8,9' '0,1,2,3,4,6,7,8,9' '0,1,2,3,4,5,7,8,9' '0,1,2,3,4,5,6,8,9' '0,1,2,3,4,5,6,7,9' '0,1,2,3,4,5,6,7,8')
#
#    for i in "${!FOLDS[@]}"; do
#
#        TRAINING_FOLD="${FOLDS[$i]}"
#        TEST_FOLD="$i"
#
#        echo "${TRAINING_FOLD}" "${TEST_FOLD}"
#
#        CLUSTERS_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-cluster-results/hdm05/65/split${SPLIT}-fold${TRAINING_FOLD}/KMeansPivotChooser--kmeans.k_${K}"
#
#        # Training data:
#        DATAFILE="/home/xprocha6/cybela1-storage/folds-cluster/hdm05/65/class130-actions-segment80_shift16-coords_normPOS-fps12.data-cho2014-split${SPLIT}-fold${TRAINING_FOLD}"
#        OUTPUT_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-mw/hdm05/65/split${SPLIT}-fold${TRAINING_FOLD}/KMeansPivotChooser--kmeans.k_${K}-train"
#        convertBodyParts
#
#        # Testing data:
#        DATAFILE="/home/xprocha6/cybela1-storage/folds-cluster/hdm05/65/class130-actions-segment80_shift16-coords_normPOS-fps12.data-cho2014-split${SPLIT}-fold${TEST_FOLD}"
#        OUTPUT_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-mw/hdm05/65/split${SPLIT}-fold${TRAINING_FOLD}/KMeansPivotChooser--kmeans.k_${K}-test"
#        convertBodyParts
#
#    done
#
#done

# PKU-MMD CS - training and testing set
# for K in '500'; do

#     CLUSTERS_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-cluster-results/pku/cs/KMeansPivotChooser--kmeans.k_${K}"

#     ## Training data:
#     DATAFILE="/home/xprocha6/cybela1-storage/folds-cluster/pku/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cs-train"
#     OUTPUT_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-mw/pku/cs/KMeansPivotChooser--kmeans.k_${K}-train"
#     convertBodyPartsCli "${1}"

#     ## Testing data:
#     DATAFILE="/home/xprocha6/cybela1-storage/folds-cluster/pku/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cs-test"
#     OUTPUT_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-mw/pku/cs/KMeansPivotChooser--kmeans.k_${K}-test"
#     convertBodyPartsCli "${1}"

# done

# PKU-MMD CV - training and testing set
# for K in '500'; do

#     CLUSTERS_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-cluster-results/pku/cv/KMeansPivotChooser--kmeans.k_${K}"

#     ## Training data:
#     DATAFILE="/home/xprocha6/cybela1-storage/folds-cluster/pku/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cv-train"
#     OUTPUT_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-mw/pku/cv/KMeansPivotChooser--kmeans.k_${K}-train"
#     convertBodyPartsCli "${1}"

#     ## Testing data:
#     DATAFILE="/home/xprocha6/cybela1-storage/folds-cluster/pku/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cv-test"
#     OUTPUT_ROOT_PATH="/home/xprocha6/cybela1-storage/folds-mw/pku/cv/KMeansPivotChooser--kmeans.k_${K}-test"
#     convertBodyPartsCli "${1}"

# done
