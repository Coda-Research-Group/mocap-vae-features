#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=16gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit

K=${PASSED_K}
PART=${PASSED_MODEL}
EXP=${PASSED_EXP}


CLS_OBJ="mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW"
SOFTASSIGNPARAM="D0K1"
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


# for DIM in "64" "32" "16" "8" "4"; do
#  	for BETA in "0.1" "1" "10"; do
# 		for K in "5" "10" "20" "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750" "1000"; do
# 			for PART in "hdm05-legR" "hdm05-legL" "hdm05-handR" "hdm05-handL" "hdm05-torso"; do
# 				echo "${PART}"

# 		  	    DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${PART}.data"
# 				OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-parts/KMeansPivotChooser--kmeans.k_${K}"
# 				CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/clusters-${PART}/KMedoidsFastPAM--kmeans.k_${K}"
#                rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"

# 				convert

# 			done
# 		done
# 	done
# done

# PKU
# for K in "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750"; do
# 	echo "${PART}"

# 	DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=${PART}.data"
# 	OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/MWs-full/KMeansPivotChooser--kmeans.k_${K}"
# 	CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${DATA}/lat_dim=${DIM}_beta=${BETA}/clusters-${PART}/KMeansPivotChooser--kmeans.k_${K}"

#     [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
# 	convert

# done

#HDM
# for K in "100" "200" "350" "500" "750" "1000" "1500" "3000"; do
    # for I in "1" "2" "3" "4" "5"; do
	    # echo "${PART}"

            DATAFILE="/storage/brno12-cerit/home/drking/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data"
            CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/pku-mmd/${EXP}/${PART}"
	        OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/baseline-skeleton/pku-mmd/KMeansPivotChooser--kmeans.k_${K}"

        [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
	    convert
    # done
# done

#CV
# for K in "50" "100" "200" "500" "650"; do
# 	echo "${PART}"

# 	    DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=pku-mmd.data"
# 	    OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/MWs-full-MO/KMeansPivotChooser--kmeans.k_${K}"
# 	    CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv/lat_dim=${DIM}_beta=${BETA}/clusters-${PART}/KMeansPivotChooser--kmeans.k_${K}"

#     [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
# 	convert

# done

# #CS
# for K in "50" "100" "200" "500" "650"; do
# 	echo "${PART}"

# 	    DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=pku-mmd.data"
# 	    OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/MWs-full-MO/KMeansPivotChooser--kmeans.k_${K}"
# 	    CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/clusters-${PART}/KMeansPivotChooser--kmeans.k_${K}"

#     [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
# 	convert

# done

# #=======  SOFT ASSIGNMENT  =========
# SOFTASSIGNPARAM="D0K1"
# DIM="256"
# BETA="1"
# PART="hdm05"
# DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=pku-mmd.data"
# OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/MWs-full-MO/KMeansPivotChooser--kmeans.k_${K}"
# CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cs/lat_dim=${DIM}_beta=${BETA}/clusters-${PART}/KMeansPivotChooser--kmeans.k_${K}"

# [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
# convert
# #----------------------------------------------















echo "Job finished successfully!"