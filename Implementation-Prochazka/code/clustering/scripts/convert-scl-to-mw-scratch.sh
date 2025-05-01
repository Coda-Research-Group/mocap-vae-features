#!/bin/bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit


CLS_OBJ=""
SOFTASSIGNPARAM="D0K1"   # optional (default is D0K1)
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
MEMORY="4g"
JDK_PATH="/usr/bin/java"
#CLUSTER_FILE_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_350/medoids.txt"
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

# PKU-MMD CV - training and testing set
 for K in '6000'; do
     PART="FULL"

 	CLUSTER_FOLDER_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/rand/KMeansPivotChooser--kmeans.k_6000/"
  	CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"

      ## Training data:
 	DATAFILE="/home/drking/Documents/bakalarka/data/SCL/cluster_test/predictions_segmented_model=pku-mmd.data"
 	OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/cluster_test/rand/MWs"
 	convert

      ## Testing data:
# 	DATAFILE="/home/drking/Documents/bakalarka/data/pku-test/lat_dim=256_beta=1/predictions_segmented_model=pku-mmd.data-cv-test"
# 	OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/pku-test/lat_dim=256_beta=1/MWs/KMeansPivotChooser--kmeans.k_${K}-test"
# 	convert

  done


#for K in "5" "10" "20" "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750"; do
#for K in "3000"; do
#	for PART in "legR" "legL" "handR" "handL" "torso"; do
#
#		echo "${PART}"
#	  	DATAFILE="/home/drking/Documents/bakalarka/data/SCL/composite/predictions_segmented_model=hdm05-${PART}.data"
#		OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/composite/clusters/fold-mws/KMeansPivotChooser--kmeans.k_350"
#		CLUSTER_FOLDER_PATH="/home/drking/Documents/bakalarka/data/SCL/composite/clusters/${PART}/KMedoidsFastPAM--kmeans.k_350"
#
#		convert
#	done
#done
#
#
#for K in "5" "10" "20" "50" "100" "150" "200" "250" "300" "350" "400" "500" "600"; do
#	for PART in "pku-mmd"; do
#
#		echo "${PART}"
#	  	DATAFILE="/home/drking/Documents/bakalarka/data/pku-test/lat_dim=32_beta=1/predictions_segmented_model=${PART}.data"
#		OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/pku-test/lat_dim=32_beta=1/MWs-parts/KMeansPivotChooser--kmeans.k_${K}"
#		CLUSTER_FOLDER_PATH="/home/drking/Documents/bakalarka/data/pku-test/lat_dim=32_beta=1/clusters-${PART}/KMeansPivotChooser--kmeans.k_${K}"
#
#		convert
#	done
#done