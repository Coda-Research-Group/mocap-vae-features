#!/bin/bash
#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=8:mem=8gb:scratch_local=50gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

ITER=${PASSED_ITER}
BETA=${PASSED_BETA}
DIM=${PASSED_DIM}
SETUP=${SETUP}
PART=${PART}


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

    ALGORITHM='messif.pivotselection.KMeansPivotChooser'
    MEDOIDS_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/medoids_new.jar'
    EXTRACTED_MEDOIDS_FILE='medoids.txt'


	for K in "20" "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750"; do

    	for model in "pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR"; do

            for FUNC in 'Cosine'; do

                ALGORITHM_PARAMS="-kmeans.k ${K}"

                # PKU-MMD CV - no folds or splits
                DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cs/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model_norm=${model}.data-cs-train"
                ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/cs/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


                DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVector${FUNC}"

                formatResultFolderName

                mkdir -p "${RESULT_FOLDER_NAME}"

                COMMAND="\
${JDK_PATH} \
-jar ${MEDOIDS_JAR_PATH} \
1 \
-pcuseall \
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

createCompositeMWClusteringMessif
