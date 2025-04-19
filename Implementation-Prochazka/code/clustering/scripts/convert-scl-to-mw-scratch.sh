#!/bin/bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit


CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"
SOFTASSIGNPARAM="D0K1"   # optional (default is D0K1)
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
MEMORY="3g"
JDK_PATH="/usr/bin/java"
#CLUSTER_FILE_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_350/medoids.txt"
VOCTYPE='-v'


CLASSPATH=${CLASSPATH:-'medoids_new.jar:MESSIF.jar:MESSIF-Utility.jar:/home/drking/Documents/bakalarka/Implementation/code/motionvocabulary/MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:/home/drking/Documents/bakalarka/Implementation/code/motionvocabulary/MCDR.jar:m-index.jar:trove4j-3.0.3.jar'}

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


#for K in '350'; do
#	for PART in 'Cosine'; do
#		echo "${PART}"
#		SOFTASSIGNPARAM="D0.2K5"
#		CLS_OBJ="messif.objects.impl.ObjectFloatVector${PART}"
#		DATAFILE="/home/drking/Documents/bakalarka/data/SCL/cluster_test/predictions_segmented_model=hdm05.data"
#		OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/results/hdm05/MW/KMeansPivotChooser--kmeans.k_${K}"
#		CLUSTER_FOLDER_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/results/hdm05/KMedoidsFastPAM--kmeans.k_${K}"
#		convert
#
#	done
#done

## PKU-MMD CV - training and testing set
for K in '3000'; do

	CLUSTER_FOLDER_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/results/pku-mmd/Cosine/KMeansPivotChooser--kmeans.k_${K}"

     ## Training data:
	DATAFILE="/home/drking/Documents/bakalarka/data/SCL/cluster_test/predictions_segmented_model=pku-mmd.data-cv-train"
	OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/results/pku-mmd/Cosine/MW/KMeansPivotChooser--kmeans.k_${K}-train"
	convert

     ## Testing data:
	DATAFILE="/home/drking/Documents/bakalarka/data/SCL/cluster_test/predictions_segmented_model=pku-mmd.data-cv-test"
	OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/results/pku-mmd/Cosine/MW/KMeansPivotChooser--kmeans.k_${K}-test"
	convert

 done
