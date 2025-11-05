#!/bin/bash
#PBS -l walltime=8:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'


CURRENT_K=${PASSED_K}
CURRENT_DATA=${PASSED_DATA}
CURRENT_ROOT=${PASSED_ROOT}


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'


function formatResultFolderName() {
    # Remove the algorithm prefix
    ALGORITHM_NAME="${ALGORITHM##*.}"
    # Replace every ' ' with '_'
    ESCAPED_ALGORITHM_PARAMS=${ALGORITHM_PARAMS//' '/'_'}
	RESULT_FOLDER_NAME="${ROOT_FOLDER_FOR_RESULTS}/${ALGORITHM_NAME}-${ESCAPED_ALGORITHM_PARAMS}"
}
# The actual clustering - combines the core functionality to produce the clustering

## Composite MW clustering using MESSIF
function createCompositeMWClusteringMessif() {


    for K in 10 20 35 50 60 80 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500 3500 5000 7500 10000; do

        ALGORITHM='messif.pivotselection.KMeansPivotChooser'
        MEDOIDS_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/medoids_new.jar'
        EXTRACTED_MEDOIDS_FILE='medoids.txt'


        ALGORITHM_PARAMS="-kmeans.k ${CURRENT_K}"

        DATASET_PATH=${CURRENT_DATA}
        ROOT_FOLDER_FOR_RESULTS=${CURRENT_ROOT}


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
-np ${CURRENT_K} \
"
        echo "${COMMAND}"

        eval "${COMMAND}" >"${RESULT_FOLDER_NAME}/${EXTRACTED_MEDOIDS_FILE}" 2>"${RESULT_FOLDER_NAME}/log.txt"

    done
}

##########################################

createCompositeMWClusteringMessif
