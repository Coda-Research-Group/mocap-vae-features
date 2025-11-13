#!/bin/bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

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

#ROOT_FOLDER_FOR_RESULTS='/run/media/drking/Nový\ svazek/bakalarka/data/segmented_actions/SCL-segmented-actions/hdm05/all/lat_dim=256_beta=1/clusters'
# Path to a dataset in ELKI format
#DATASET_PATH='/home/drking/Documents/bakalarka/data/SCL/elki-predictions_segmented_model=hdm05.data.gz'
DISTANCE_FUNCTION='de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction'
# OPTIONAL - Specifies which joint indices should be used for clustering.
# '' ~ use all joints
# '-clustering.distance.SequenceMocapPoseCoordsL2DTW.usedJointIds 1,2,3' ~ use only joints with ids 1, 2, and 3,
#   every id should be in range [1, 31], see clustering.Joint enum
DISTANCE_FUNCTION_PARAMS=''
ALGORITHM='clustering.kmeans.KMedoidsFastPAM'
ALGORITHM_PARAMS='-kmeans.k 3'
# JAR of the "clustering" project with "ELKIWithDistances" as the main class
ELKI_JAR_PATH='/home/drking/Documents/Bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances.jar'
# JAR of the "clustering" project with "Convertor" as the main class
# My edited convertor.
CONVERTOR_JAR_PATH='/home/drking/Documents/Bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor.jar'

JDK_PATH='/usr/bin/java'
# Subfolder name for the result of createClusters function
CLUSTER_SUBFOLDER='cluster'
# Subfolder name for the result of convertElkiClusteringFormatToElkiFormat function
ELKI_FORMAT_CLUSTER_SUBFOLDER='clusters-elki-format'
# Subfolder name for the result of runKMedoidsClusteringOnEveryCluster function
KMEDOIDS_CLUSTER_SUBFOLDER='kmedoids-clusters'
# File name for the result of extractClusterMedoids function
EXTRACTED_MEDOIDS_FILE='medoids.txt'

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
#ELKI_JAR_PATH=${ELKI_JAR_PATH:-'/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances.jar'}
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
-Xmx12G \
-jar ${ELKI_JAR_PATH} \
KDDCLIApplication \
-verbose \
-dbc.in ${DATASET_PATH} \
-time \
-algorithm ${ALGORITHM} \
-algorithm.distancefunction ${DISTANCE_FUNCTION} \
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

        echo "${RESULT_FOLDER_NAME}/${ELKI_FORMAT_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}"

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
--vector-dim=${CURRENT_DIM}
"
        echo "${COMMAND}"

        # Executes the medoid extraction. The results are appended to a single file in MESSIF format.
        eval "${COMMAND}" >>"${RESULT_FOLDER_NAME}/${EXTRACTED_MEDOIDS_FILE}"
    done
}

# The actual clustering - combines the core functionality to produce the clustering

## Composite MW clustering using ELKI
function createCompositeMWClusteringELKI() {
		CURRENT_DIM=256
		DATASET_PATH="/home/drking/Documents/Bakalarka/data/test/pku/elki-subset.txt"
		ROOT_FOLDER_FOR_RESULTS="/home/drking/Documents/Bakalarka/data/test/pku/ELKI"


      	createClusters
      	convertElkiClusteringFormatToElkiFormat
      	runKMedoidsClusteringOnEveryCluster
      	extractClusterMedoids

}

## Composite MW clustering using MESSIF
function createCompositeMWClusteringMessif() {

    ALGORITHM='messif.pivotselection.KMeansPivotChooser'
    MEDOIDS_JAR_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/medoids-test.jar'

    for FOLD in '0'; do # HDM05-130 folds
        # for FOLD in '0,1,2,3,4,5,6,7,8' '0,1,2,3,4,5,6,7,9' '0,1,2,3,4,5,6,8,9' '0,1,2,3,4,5,7,8,9' '0,1,2,3,4,6,7,8,9' '0,1,2,3,5,6,7,8,9' '0,1,2,4,5,6,7,8,9' '0,1,3,4,5,6,7,8,9' '0,2,3,4,5,6,7,8,9' '1,2,3,4,5,6,7,8,9'; do # HDM05-65 folds

        for K in '6000'; do

            for FUNC in 'Cosine'; do

                ALGORITHM_PARAMS="-kmeans.k ${K}"


                # SELECT DATASET_PATH AND ROOT_FOLDER_FOR_RESULTS:

                # HDM05-130 - 2-fold cross validation - folds: '0' '1'
                DATASET_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/predictions_segmented_model=pku-mmd.data-cv-train"
                ROOT_FOLDER_FOR_RESULTS="/home/drking/Documents/bakalarka/data/SCL/cluster_test/rand/"

                # HDM05-65 - 10-fold cross validation - folds: '0,1,2,3,4,5,6,7,8' '0,1,2,3,4,5,6,7,9' '0,1,2,3,4,5,6,8,9' '0,1,2,3,4,5,7,8,9' '0,1,2,3,4,6,7,8,9' '0,1,2,3,5,6,7,8,9' '0,1,2,4,5,6,7,8,9' '0,1,3,4,5,6,7,8,9' '0,2,3,4,5,6,7,8,9' '1,2,3,4,5,6,7,8,9'
                # DATASET_PATH="/home/xprocha6/cybela1-storage/folds-cluster/hdm05/65/class130-actions-segment80_shift16-coords_normPOS-fps12.data-cho2014-split${SPLIT}-fold${FOLD}"
                # ROOT_FOLDER_FOR_RESULTS="/home/xprocha6/cybela1-storage/folds-cluster-results/hdm05/65/split${SPLIT}-fold${FOLD}"

                # PKU-MMD CS - no folds or splits
#                 DATASET_PATH='/home/xprocha6/cybela1-storage/folds-cluster/pku/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cs-train'
#                 ROOT_FOLDER_FOR_RESULTS='/home/xprocha6/cybela1-storage/folds-cluster-results/pku/cs'

                # PKU-MMD CV - no folds or splits
#                 DATASET_PATH="/home/drking/Documents/bakalarka/data/pku-test/lat_dim=256_beta=1/predictions_segmented_model=pku-mmd.data-cv-train"
#                 ROOT_FOLDER_FOR_RESULTS="/home/drking/Documents/bakalarka/data/pku-test/Cosine"


                # SELECT JOINTS_IDS:

                # HDM05 - body parts

                    # HDM05 - relations consisting of RH (28,29,30,31), LH (21,22,23,24), and HEAD (16,17)
                    # for JOINT_IDS in '28,29,30,31,21,22,23,24' '16,17,28,29,30,31' '16,17,21,22,23,24'; do
                    #  1. RH + LH
                    # for JOINT_IDS in '28,29,30,31,21,22,23,24'; do
                    #  2. HEAD + RH
                    # for JOINT_IDS in '16,17,28,29,30,31'; do
                    #  3. HEAD + LH
                    # for JOINT_IDS in '16,17,21,22,23,24'; do

                    # PKU-MMD - body parts
                    # for JOINT_IDS in '2,3,4,21' '5,6,7,8,22,23' '9,10,11,12,24,25' '13,14,15,16' '17,18,19,20'; do

                    # PKU-MMD - relations consisting of RH (11,12,24,25), LH (7,8,22,23), and HEAD (3,4)
                    #  1. RH + LH
                    # for JOINT_IDS in '11,12,24,25,7,8,22,23'; do
                    #  2. HEAD + RH
                    # for JOINT_IDS in '3,4,11,12,24,25'; do
                    #  3. HEAD + LH
                    # for JOINT_IDS in '3,4,7,8,22,23'; do

                DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVector${FUNC}"
#				DISTANCE_FUNCTION="mcdr.objects.impl.ObjectSegmentCodeList"
                formatResultFolderName

                mkdir -p "${RESULT_FOLDER_NAME}"

                COMMAND="\
${JDK_PATH} \
-jar ${MEDOIDS_JAR_PATH} \
0 \
-sf ${DATASET_PATH} \
-pcuseall \
-kmeans-max-iters 10 \
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

 for K in 350; do
     ALGORITHM_PARAMS="-kmeans.k ${K}"
     createCompositeMWClusteringELKI
 done


# MESSIF clustering:
#createCompositeMWClusteringMessif
