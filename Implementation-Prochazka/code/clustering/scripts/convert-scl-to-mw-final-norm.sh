#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit

DIM=${PASSED_DIM}
BETA=${PASSED_BETA}
PART=${PASSED_MODEL}
DATA=${PASSED_DATA}


CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"
SOFTASSIGNPARAM="D0K1"   # optional (default is D0K1)
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
MEMORY="8g"
JDK_PATH="/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java"
VOCTYPE='-v'


CLASSPATH=${CLASSPATH:-'MESSIF.jar:MESSIF-Utility.jar:MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:MCDR.jar:m-index.jar:trove4j-3.0.3.jar'}

#java -Xmx${MEM:-500m} -cp $CLASSPATH messif.motionvocabulary.MotionVocabulary -d $DATAFILE -c $CLS_OBJ --quantize $TOSEQ ${VOCTYPE} $VOCABULARY $SOFTASSIGNPARAM --output $OUTPUT


function convert() {
    echo 'convertBodyPart'

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

      echo "${COMMAND}"

      eval "${COMMAND}"
}


for K in "1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000"; do

	DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model_norm=${MODEL}.data"
	OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/MWs-parts/KMeansPivotChooser--kmeans.k_${K}"
	CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/clusters-${MODEL}/KMedoidsFastPAM--kmeans.k_${K}"


    [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
	convert

done


echo "Job finished successfully!"