#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

K="350"
ITER="1"

DISTANCE_FUNCTION='de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction'
DISTANCE_FUNCTION_PARAMS=""
ALGORITHM='clustering.kmeans.KMedoidsFastPAM'
#ALGORITHM_PARAMS='-kmeans.k 3'
# JAR of the "clustering" project with "ELKIWithDistances" as the main class
ELKI_JAR_PATH='/home/drking/Documents/Bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances.jar'
# JAR of the "clustering" project with "Convertor" as the main class
#CONVERTOR_JAR_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor_old.jar'
# My edited convertor.
CONVERTOR_JAR_PATH='/home/drking/Documents/Bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor.jar'

# JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'
# Subfolder name for the result of createClusters function
CLUSTER_SUBFOLDER='cluster'
# Subfolder name for the result of convertElkiClusteringFormatToElkiFormat function
ELKI_FORMAT_CLUSTER_SUBFOLDER='clusters-elki-format'
# Subfolder name for the result of runKMedoidsClusteringOnEveryCluster function
KMEDOIDS_CLUSTER_SUBFOLDER='kmedoids-clusters'
# File name for the result of extractClusterMedoids function
EXTRACTED_MEDOIDS_FILE='medoids.txt'

JDK_PATH='/usr/bin/java'

# The core functionality of this script

function formatResultFolderName() {
    # Remove the algorithm prefix
    ALGORITHM_NAME="${ALGORITHM##*.}"
    # Replace every ' ' with '_'
    ESCAPED_ALGORITHM_PARAMS=${ALGORITHM_PARAMS//' '/'_'}
	RESULT_FOLDER_NAME="${ROOT_FOLDER_FOR_RESULTS}/${ALGORITHM_NAME}-${ESCAPED_ALGORITHM_PARAMS}"
}

## Composite MW clustering using MESSIF
function createCompositeMWClusteringMessif() {


    ALGORITHM='messif.pivotselection.KMeansPivotChooser'
    MEDOIDS_JAR_PATH='/home/drking/Documents/Bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/medoids_new.jar'
    EXTRACTED_MEDOIDS_FILE='medoids.txt'

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH='/home/drking/Documents/Bakalarka/data/SCL/1/predictions_segmented.data.gz'
    ROOT_FOLDER_FOR_RESULTS="/home/drking/Documents/Bakalarka/data/SCL/1/clusters/${K}/"

    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

    formatResultFolderName

    mkdir -p "${RESULT_FOLDER_NAME}"

    COMMAND="\
${JDK_PATH} \
-jar ${MEDOIDS_JAR_PATH} \
1 \
-pcuseall \
-kmeans-max-iters 10 \
-sf ${DATASET_PATH} \
-cls ${DISTANCE_FUNCTION} \
-pc ${ALGORITHM} \
-np ${K} \
"
    echo "${COMMAND}"

    eval "${COMMAND}" >"${RESULT_FOLDER_NAME}/${EXTRACTED_MEDOIDS_FILE}" 2>"${RESULT_FOLDER_NAME}/log.txt"

}


##########################################

# ELKI clustering:
# for K in 2 3 4 5 6 7 8 9 10 20 50 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2250; do
#     ALGORITHM_PARAMS="-kmeans.k ${K}"
#     createCompositeMWClusteringELKI
#     sleep 10
# done

#  for K in 50 100 200 350 500; do
#      ALGORITHM_PARAMS="-kmeans.k ${K}"
#      createCompositeMWClusteringELKI
#      sleep 1
#  done


# MESSIF clustering:
createCompositeMWClusteringMessif
