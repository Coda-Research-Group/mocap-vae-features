#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=2:mem=16gb:scratch_local=16gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

ITER=${PASSED_ITER}
BETA=${PASSED_BETA}
DIM=${PASSED_DIM}
PART=${PASSED_PART}
SETUP=${PASSED_SETUP}

# Path to a dataset in ELKI format
#DATASET_PATH='/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/elki-predictions_segmented_model=hdm05.data'
DISTANCE_FUNCTION='de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction'
DISTANCE_FUNCTION_PARAMS=""
ALGORITHM='clustering.kmeans.KMedoidsFastPAM'
#ALGORITHM_PARAMS='-kmeans.k 3'
# JAR of the "clustering" project with "ELKIWithDistances" as the main class
ELKI_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances.jar'
# JAR of the "clustering" project with "Convertor" as the main class
#CONVERTOR_JAR_PATH='/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor_old.jar'
# My edited convertor.
CONVERTOR_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor.jar'

JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'
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

function formatResultFolderName() {
    # Remove the algorithm prefix
    ALGORITHM_NAME="${ALGORITHM##*.}"
    # Replace every ' ' with '_'
    ESCAPED_ALGORITHM_PARAMS=${ALGORITHM_PARAMS//' '/'_'}
	RESULT_FOLDER_NAME="${ROOT_FOLDER_FOR_RESULTS}/${ALGORITHM_NAME}-${ESCAPED_ALGORITHM_PARAMS}"
}

## Composite MW clustering using MESSIF
function createCompositeMWClusteringMessif() {

    for K in 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500 3500 5000 7500 10000 15000; do
        ALGORITHM='messif.pivotselection.KMeansPivotChooser'
        MEDOIDS_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/medoids_new.jar'
        EXTRACTED_MEDOIDS_FILE='medoids.txt'


        ALGORITHM_PARAMS="-kmeans.k ${K}"

        DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model\=${PART}_lat-dim\=${DIM}_beta\=${BETA}/${ITER}/predictions_segmented.data-train"
        ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/clusters/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/"


        DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

}


# MESSIF clustering:
createCompositeMWClusteringMessif
