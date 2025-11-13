#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /home/drking/Documents/Bakalarka/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit


CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"
SOFTASSIGNPARAM="D0K1"   # optional (default is D0K1)
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
MEMORY="8g"
JDK_PATH="/usr/bin/java"
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


# for K in "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750"; do
#     # for SOFTASSIGNPARAM in "" "" "" "" ""; do 
# 	echo "${PART}"

# 	DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${PART}.data"
# 	OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/MWs-parts/KMeansPivotChooser--kmeans.k_${K}"
# 	CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/clusters-${PART}/KMeansPivotChooser--kmeans.k_${K}"

#     [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
# 	convert

# done


#HDM
for K in "50"; do
  PART="MWs"
	echo "${PART}"

	    DATAFILE="/home/drking/Documents/Bakalarka/data/test/pku/predictions_segmented.data.gz"
	    OUTPUT_ROOT_PATH="/home/drking/Documents/Bakalarka/data/test/pku/MWs"
	    CLUSTER_FOLDER_PATH="/home/drking/Documents/Bakalarka/data/test/pku/ELKI/KMedoidsFastPAM--kmeans.k_350"

    [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
	convert

done

echo "Job finished successfully!"