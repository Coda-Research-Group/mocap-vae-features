#!/bin/bash

#PBS -l walltime=8:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null


DISTANCE_FUNCTION='de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction'
DISTANCE_FUNCTION_PARAMS=""
ALGORITHM='clustering.kmeans.KMedoidsFastPAM'
ALGORITHM_PARAMS='-kmeans.k 3'
ELKI_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/elki-with-distances.jar'
# My edited convertor.
CONVERTOR_JAR_PATH='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/convertor.jar'
JDK_PATH="/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java"
CLUSTER_SUBFOLDER='cluster'
ELKI_FORMAT_CLUSTER_SUBFOLDER='clusters-elki-format'
KMEDOIDS_CLUSTER_SUBFOLDER='kmedoids-clusters'
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

    formatResultFolderName

    COMMAND="\
${JDK_PATH} \
-Xmx16G \
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


    eval "${COMMAND}" > /dev/null 2>&1

    # Stores the run command alongside the results.
    echo "${COMMAND}" >"${RESULT_FOLDER_NAME}/${CLUSTER_SUBFOLDER}/clustering-command.txt"
}

function convertElkiClusteringFormatToElkiFormat() {

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


        echo "${RESULT_FOLDER_NAME}/${ELKI_FORMAT_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}"

        eval "${COMMAND}" >"${RESULT_FOLDER_NAME}/${ELKI_FORMAT_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}"
    done
}

function runKMedoidsClusteringOnEveryCluster() {

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
            eval "${COMMAND}" > /dev/null 2>&1

            # Stores the run command alongside the results.
            echo "${COMMAND}" >"${RESULT_FOLDER_NAME}/${KMEDOIDS_CLUSTER_SUBFOLDER}/${CLUSTER_FILENAME}/clustering-command.txt"
        fi
    done

}

function extractClusterMedoids() {

    formatResultFolderName

    for CLUSTER_FOLDER_PATH in "${RESULT_FOLDER_NAME}"/"${KMEDOIDS_CLUSTER_SUBFOLDER}"/cluster_*; do
        COMMAND="\
${JDK_PATH} \
-jar ${CONVERTOR_JAR_PATH} \
--parse-medoids-from-elki-clustering-folder \
--elki-clustering-folder=${CLUSTER_FOLDER_PATH} \
--vector-dim=${DIM}
"

        # Executes the medoid extraction. The results are appended to a single file in MESSIF format.
        eval "${COMMAND}" >>"${RESULT_FOLDER_NAME}/${EXTRACTED_MEDOIDS_FILE}"
    done
}

# The actual clustering - combines the core functionality to produce the clustering

## Composite MW clustering using ELKI
function createCompositeMWClusteringELKI() {
      	createClusters
      	convertElkiClusteringFormatToElkiFormat
      	runKMedoidsClusteringOnEveryCluster
      	extractClusterMedoids
}

###########################################

cd /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit

CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"
SOFTASSIGNPARAM="D0K1"
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
MEMORY="8g"
VOCTYPE='-v'


CLASSPATH=${CLASSPATH:-'MESSIF.jar:MESSIF-Utility.jar:MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:MCDR.jar:m-index.jar:trove4j-3.0.3.jar'}

function convert() {
	CLUSTER_FOLDER_NAME=$(basename "${CLUSTER_FOLDER_PATH}")

    mkdir -p "${OUTPUT_ROOT_PATH}"

      COMMAND="\
  ${JDK_PATH} \
  -Xmx${MEMORY} \
  -cp ${CLASSPATH} \
  messif.motionvocabulary.MotionVocabulary \
  -d ${DATAFILE} \
  -c ${CLS_OBJ} \
  --quantize ${TOSEQ} ${VOCTYPE} ${CLUSTER_FOLDER_PATH}/medoids.txt \
  --soft-assign ${SOFTASSIGNPARAM} \
  --output ${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM} \
  "

      eval "${COMMAND}" > /dev/null 2>&1
}


#######################################
function single() {

    DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
    OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMedoidsFastPAM--kmeans.k_${K}"
    CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/elki-clusters/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMedoidsFastPAM--kmeans.k_${K}"
    if [[ ! -f "$DATAFILE" ]]; then
        exit 67 
    fi
    [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
    convert

    ##########################################

    rm -f "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results-${SOFTASSIGNPARAM}.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMedoidsFastPAM--kmeans.k_${K} \
    -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
    -soft \
    -k 4 \
    "
    mkdir -p "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/"
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results-${SOFTASSIGNPARAM}.txt"

    COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
    -fp /storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMedoidsFastPAM--kmeans.k_${K} \
    -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
    -soft \

    "
    eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/elki-results/hdm05/soft/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${K}/results-${SOFTASSIGNPARAM}.txt"
}

for PART in "hdm05"; do
    for ITER in 3; do
        BETA="1"
        for DIM in 256; do
            for K in 100 200 400 800 1600; do 
                for PARAM in "D0.48K6" "D0.12K6" "D0.03K6"; do 
                    SOFTASSIGNPARAM="${PARAM}"

                    single
                done
            done
        done
        BETA="0.1"
        for DIM in 256; do
            for K in 100 200 400 800 1600; do 
                for PARAM in "D0.35K6" "D0.09K6" "D0.02K6"; do 
                    SOFTASSIGNPARAM="${PARAM}"

                    single
                done
            done
        done
    done
done

