#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb


PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"



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

# --------------------------------------------------------------------------------------------------------------------------------------------------------------

    K="600"
    CURRENT_DIM="8"
    CURRENT_BETA="1"
    model="pku-mmd-handL"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="600"
    CURRENT_DIM="32"
    CURRENT_BETA="0.1"
    model="pku-mmd"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="600"
    CURRENT_DIM="8"
    CURRENT_BETA="0.1"
    model="pku-mmd-legR"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="150"
    CURRENT_DIM="32"
    CURRENT_BETA="10"
    model="pku-mmd-legR"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="250"
    CURRENT_DIM="16"
    CURRENT_BETA="10"
    model="pku-mmd-legR"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="300"
    CURRENT_DIM="4"
    CURRENT_BETA="0.1"
    model="pku-mmd-handR"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="600"
    CURRENT_DIM="16"
    CURRENT_BETA="0.1"
    model="pku-mmd-handL"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="500"
    CURRENT_DIM="8"
    CURRENT_BETA="10"
    model="pku-mmd-handL"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="500"
    CURRENT_DIM="8"
    CURRENT_BETA="10"
    model="pku-mmd-legL"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="600"
    CURRENT_DIM="64"
    CURRENT_BETA="1"
    model="pku-mmd-torso"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="300"
    CURRENT_DIM="64"
    CURRENT_BETA="1"
    model="pku-mmd-legL"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="500"
    CURRENT_DIM="16"
    CURRENT_BETA="1"
    model="pku-mmd-handR"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="750"
    CURRENT_DIM="4"
    CURRENT_BETA="10"
    model="pku-mmd-handR"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

#---------------------------------------------------------------------------------------------------------------------------------------------
    K="750"
    CURRENT_DIM="32"
    CURRENT_BETA="1"
    model="pku-mmd"

    ALGORITHM_PARAMS="-kmeans.k ${K}"

    DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
    ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


    DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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



    K="750"
    CURRENT_DIM="16"
    CURRENT_BETA="0.1"
    model="pku-mmd-legL"

    for K in "5" "10" "20" "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750"; do
        ALGORITHM_PARAMS="-kmeans.k ${K}"

        DATASET_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/predictions_segmented_model=${model}.data-cv-train"
        ROOT_FOLDER_FOR_RESULTS="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${CURRENT_DIM}_beta=${CURRENT_BETA}/clusters-${model}"


        DISTANCE_FUNCTION="messif.objects.impl.ObjectFloatVectorCosine"

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

}


##########################################

createCompositeMWClusteringMessif

