#!/bin/bash
#PBS -l walltime=8:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

K=${PASSED_K}

##########################################

# Script for running ELKI clustering on ELKI formatted dataset.
# The final result of the clustering is a file (${EXTRACTED_MEDOIDS_FILE})
# containing list of medoids (one for each created cluster) in MESSIF format.
#
# The resulting files are placed into a folder into ${ROOT_FOLDER_FOR_RESULTS}.
# See formatResultFolderName function for the folder name.
#
# See README.md for the project terminology and the pipline stages.

# 1. Insert your custom values (see defaults for inspiration)

# ROOT_FOLDER_FOR_RESULTS='/home/drking/Documents/bakalarka/data/clustering'
# Path to a dataset in ELKI format
# DATASET_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/elki-class130-actions-segment80_shift16-coords_normPOS-fps12.data'
DISTANCE_FUNCTION='clustering.distance.SequenceMocapPoseCoordsL2DTW'
# OPTIONAL - Specifies which joint indices should be used for clustering.
# '' ~ use all joints
# '-clustering.distance.SequenceMocapPoseCoordsL2DTW.usedJointIds 1,2,3' ~ use only joints with ids 1, 2, and 3,
#   every id should be in range [1, 31], see clustering.Joint enum
# DISTANCE_FUNCTION_PARAMS=''
# ALGORITHM='clustering.kmeans.KMedoidsFastPAM'
# ALGORITHM_PARAMS='-kmeans.k 3'
# JAR of the "clustering" project with "ELKIWithDistances" as the main class
# ELKI_JAR_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances.jar'
# JAR of the "clustering" project with "Convertor" as the main class
# CONVERTOR_JAR_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor.jar'
JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'
# Subfolder name for the result of createClusters function
CLUSTER_SUBFOLDER='cluster'
# Subfolder name for the result of convertElkiClusteringFormatToElkiFormat function
ELKI_FORMAT_CLUSTER_SUBFOLDER='clusters-elki-format'
# Subfolder name for the result of runKMedoidsClusteringOnEveryCluster function
KMEDOIDS_CLUSTER_SUBFOLDER='kmedoids-clusters'
# File name for the result of extractClusterMedoids function
EXTRACTED_MEDOIDS_FILE='medoids.txt'
# OPTIONAL - Used during the composite MW clustering.
# Specifies the name of subfolder to which the output of a single body part is placed.
# COMPOSITE_MW_BODYPART_SUBFOLDER=''

# 2. Uncomment desired functions at the bottom of the file in the Main section

# 3. Run the script as follows:
# nohup ./cluster.sh &> <output>.txt &

##########################################

# Defaults

#ROOT_FOLDER_FOR_RESULTS=${ROOT_FOLDER_FOR_RESULTS:-'/home/drking/Documents/bakalarka/mocap-vae-features/data/clustering/results'}
#DATASET_PATH=${DATASET_PATH:-'/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/elki-class130-actions-segment80_shift16-coords_normPOS-fps12.data'}
#DISTANCE_FUNCTION=${DISTANCE_FUNCTION:-'clustering.distance.SequenceMocapPoseCoordsL2DTW'}
#ALGORITHM=${ALGORITHM:-'clustering.kmeans.KMedoidsFastPAM'}
#ALGORITHM_PARAMS=${ALGORITHM_PARAMS:-'-kmeans.k 3'}
#ELKI_JAR_PATH=${ELKI_JAR_PATH:-'/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances1.jar'}
#CONVERTOR_JAR_PATH=${CONVERTOR_JAR_PATH:-'/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor.jar'}
#JDK_PATH=${JDK_PATH:-'/usr/bin/java'}
#CLUSTER_SUBFOLDER=${CLUSTER_SUBFOLDER:-'cluster'}
#ELKI_FORMAT_CLUSTER_SUBFOLDER=${ELKI_FORMAT_CLUSTER_SUBFOLDER:-'clusters-elki-format'}
#KMEDOIDS_CLUSTER_SUBFOLDER=${KMEDOIDS_CLUSTER_SUBFOLDER:-'kmedoids-clusters'}
#EXTRACTED_MEDOIDS_FILE=${EXTRACTED_MEDOIDS_FILE:-'medoids.txt'}

# The core functionality of this script

function formatResultFolderName() {
    # Remove the algorithm prefix
    ALGORITHM_NAME="${ALGORITHM##*.}"
    # Replace every ' ' with '_'
    ESCAPED_ALGORITHM_PARAMS=${ALGORITHM_PARAMS//' '/'_'}
	RESULT_FOLDER_NAME="${ROOT_FOLDER_FOR_RESULTS}/${ALGORITHM_NAME}-${ESCAPED_ALGORITHM_PARAMS}"

}

function createClusters() {
    echo 'createClusters'

    formatResultFolderName

    COMMAND="\
${JDK_PATH} \
-jar ${ELKI_JAR_PATH} \
KDDCLIApplication \
-verbose \
-dbc.in ${DATASET_PATH} \
-time \
-algorithm ${ALGORITHM} \
-algorithm.distancefunction ${DISTANCE_FUNCTION} \
${DISTANCE_FUNCTION_PARAMS} \
${ALGORITHM_PARAMS} \
-resulthandler ResultWriter \
-out ${RESULT_FOLDER_NAME}/${CLUSTER_SUBFOLDER}\
"

    echo "${COMMAND}"

    eval "${COMMAND}"

    # Stores the run command alongside the results.
    echo "${COMMAND}" >"${RESULT_FOLDER_NAME}/${CLUSTER_SUBFOLDER}/clustering-command.txt"
}

function convertElkiClusteringFormatToElkiFormat() {
    echo 'convertElkiClusteringFormatToElkiFormat'

    formatResultFolderName

    mkdir -p "${RESULT_FOLDER_NAME}"/"${ELKI_FORMAT_CLUSTER_SUBFOLDER}"

    for CLUSTER_PATH in "${RESULT_FOLDER_NAME}"/"${CLUSTER_SUBFOLDER}"/cluster_*; do
        # Parses the filename (the string after the last "/")
        CLUSTER_FILENAME=$(basename "${CLUSTER_PATH}")

        COMMAND="\
${JDK_PATH} \
-jar ${CONVERTOR_JAR_PATH} \
--convert-elki-clustering-file-to-elki-format \
--elki-clustering-file=${RESULT_FOLDER_NAME}/${CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME} \
"

        echo "${COMMAND}"

        eval "${COMMAND}" >"${RESULT_FOLDER_NAME}/${ELKI_FORMAT_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}"
    done
}

function runKMedoidsClusteringOnEveryCluster() {
    echo 'runKMedoidsClusteringOnEveryCluster'

    formatResultFolderName

    for CLUSTER_PATH in "${RESULT_FOLDER_NAME}"/"${ELKI_FORMAT_CLUSTER_SUBFOLDER}"/cluster_*; do
        # Parses the filename (the string after the last "/") and removes the file extension
        CLUSTER_FILENAME=$(basename "${CLUSTER_PATH%.*}")

        NUMBER_OF_OBJECTS_IN_CLUSTER=$(wc -l "${CLUSTER_PATH}" | awk -F " " '{print $1}')

        # if CLUSTER_PATH contains only one object use this object as the medoid/pivot
        if [[ "${NUMBER_OF_OBJECTS_IN_CLUSTER}" == '1' ]]; then
            mkdir -p "${RESULT_FOLDER_NAME}/${KMEDOIDS_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}"

            # Remove commas
            ELKI_RESULT=$(sed 's/,//g' "${CLUSTER_PATH}")

            echo -e "# Cluster: Cluster\n\
# Cluster name: Cluster\n\
# Cluster noise flag: false\n\
# Cluster size: 1\n\
# Model class: de.lmu.ifi.dbs.elki.data.model.MedoidModel\n\
# Cluster Medoid: 1\n\
ID=1 ${ELKI_RESULT}" >"${RESULT_FOLDER_NAME}/${KMEDOIDS_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}/cluster.txt"
        else
            COMMAND="\
${JDK_PATH} \
-jar ${ELKI_JAR_PATH} \
KDDCLIApplication \
-verbose \
-dbc.in ${CLUSTER_PATH} \
-time \
-algorithm clustering.kmeans.KMedoidsFastPAM \
-algorithm.distancefunction ${DISTANCE_FUNCTION} \
${DISTANCE_FUNCTION_PARAMS} \
-kmeans.k 1 \
-resulthandler ResultWriter \
-out ${RESULT_FOLDER_NAME}/${KMEDOIDS_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME} \
"
            echo "${COMMAND}"

            eval "${COMMAND}"

            # Stores the run command alongside the results.
            echo "${COMMAND}" >"${RESULT_FOLDER_NAME}/${KMEDOIDS_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}/clustering-command.txt"
        fi
    done

}

function extractClusterMedoids() {
    echo 'extractClusterMedoids'

    formatResultFolderName

    for CLUSTER_FOLDER_PATH in "${RESULT_FOLDER_NAME}"/"${KMEDOIDS_CLUSTER_SUBFOLDER}"/cluster_*; do
        COMMAND="\
${JDK_PATH} \
-jar ${CONVERTOR_JAR_PATH} \
--parse-medoids-from-elki-clustering-folder \
--elki-clustering-folder=${CLUSTER_FOLDER_PATH} \
"
        echo "${COMMAND}"

        # Executes the medoid extraction. The results are appended to a single file in MESSIF format.
        eval "${COMMAND}" >>"${RESULT_FOLDER_NAME}/${EXTRACTED_MEDOIDS_FILE}"
    done
}

# The actual clustering - combines the core functionality to produce the clustering

## Composite MW clustering using ELKI
function createCompositeMWClusteringELKI() {
    DATASET_PATH='/home/drking/Documents/bakalarka/data/hdm05/elki-class130-actions-segment80_shift16-coords_normPOS-fps12.data'
    ROOT_FOLDER_FOR_RESULTS='/home/drking/Documents/bakalarka/data/clustering-results'

    for JOINT_IDS in '2,3,4,5,6' '7,8,9,10,11' '12,13,14,15,16,17' '18,19,20,21,22,23,24' '25,26,27,28,29,30,31'; do # HDM05 - body parts
        DISTANCE_FUNCTION_PARAMS="-clustering.distance.SequenceMocapPoseCoordsL2DTW.usedJointIds ${JOINT_IDS}"
        COMPOSITE_MW_BODYPART_SUBFOLDER="${JOINT_IDS}"

        createClusters
        convertElkiClusteringFormatToElkiFormat
        runKMedoidsClusteringOnEveryCluster
        extractClusterMedoids
    done
}

## Composite MW clustering using MESSIF
function createCompositeMWClusteringMessif() {

    ALGORITHM='messif.pivotselection.KMeansPivotChooser'
    MEDOIDS_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/medoids_new.jar'

    for FOLD in '0'; do # HDM05-130 folds
        # for FOLD in '0,1,2,3,4,5,6,7,8' '0,1,2,3,4,5,6,7,9' '0,1,2,3,4,5,6,8,9' '0,1,2,3,4,5,7,8,9' '0,1,2,3,4,6,7,8,9' '0,1,2,3,5,6,7,8,9' '0,1,2,4,5,6,7,8,9' '0,1,3,4,5,6,7,8,9' '0,2,3,4,5,6,7,8,9' '1,2,3,4,5,6,7,8,9'; do # HDM05-65 folds

        for EXP in 'cs' ; do

            for FUNC in ''; do

                ALGORITHM_PARAMS="-kmeans.k ${K}"

                # PKU-MMD CV - no folds or splits
                DATASET_PATH="/storage/brno12-cerit/home/drking/hdm05/class130-actions-segment80_shift16-coords_normPOS-fps12.data"
                ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/hdm05/all/"

#                DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVector${FUNC}"
				DISTANCE_FUNCTION="mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW"
                formatResultFolderName

                mkdir -p "${RESULT_FOLDER_NAME}"

                COMMAND="\
${JDK_PATH} \
-jar ${MEDOIDS_JAR_PATH} \
1 \
-pcuseall \
-kmeans-max-iters 15 \
-sf ${DATASET_PATH} \
-cls ${DISTANCE_FUNCTION} \
-pc ${ALGORITHM} \
-np ${K} \
"
                echo "${COMMAND}"

                eval "${COMMAND}" >"${RESULT_FOLDER_NAME}/${EXTRACTED_MEDOIDS_FILE}" 2>"${RESULT_FOLDER_NAME}/log.txt"

            done
        done
    done
}

##########################################

# ELKI clustering:
# for K in 2 3 4 5 6 7 8 9 10 20 50 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2250; do
#     ALGORITHM_PARAMS="-kmeans.k ${K}"
#     createCompositeMWClusteringELKI
#     sleep 10
# done
#
# for K in 350; do
#     ALGORITHM_PARAMS="-kmeans.k ${K}"
#     createCompositeMWClusteringELKI
#     sleep 10
# done


# MESSIF clustering:
createCompositeMWClusteringMessif
