#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=2:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit

DIM=32
BETA=0.1
PART="full"

CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"
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




#HDM
for K in 10 20 35 50 60 80 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500; do
    for ITER in 1 2 3 4 5; do 
        for BETA in "0.1" "1"; do 
            for DIM in 32 64; do 
                PART="hdm05"
	            echo "${PART}"

	                DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
	                OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K}"
                    CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/clusters/hdm05/all/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K}"

                [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
	            convert

            done
        done   
    done
done

for K in 10 20 35 50 60 80 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500; do
    for ITER in 3; do 
        for BETA in "0.1" "1"; do 
            for DIM in 8 16; do
                for PART in "hdm05-handR" "hdm05-handL" "hdm05-legR" "hdm05-legL" "hdm05-torso"; do

	                echo "${PART}"

	                DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
	                OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/KMeansPivotChooser--kmeans.k_${K}"
                    CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/clusters/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/KMeansPivotChooser--kmeans.k_${K}"

                    [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
	                convert



	                DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
	                OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}_non-norm/KMeansPivotChooser--kmeans.k_${K}"
                    CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/clusters/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}_non-norm/KMeansPivotChooser--kmeans.k_${K}"

                    [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
	                convert

                done
            done
        done   
    done
done

#pku
for SETUP in "cs" "cv"; do 
    for K in 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500 3500 5000 7500 10000 15000; do
        for ITER in 1 2 3 4 5; do 
            for BETA in "0.1" "1"; do 
                for DIM in 8 16 64; do 
                    PART="pku-mmd"
    	            echo "${PART}"

    	                DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
    	                OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/MWs/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K}"
                        CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/clusters/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K}"

                    [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
    	            convert

                done
            done   
        done

        for ITER in 3; do 
            for BETA in "0.1" "1"; do 
                for DIM in 8 16; do
                    for PART in "pku-mmd-handR" "pku-mmd-handL" "pku-mmd-legR" "pku-mmd-legL" "pku-mmd-torso"; do

    	                echo "${PART}"

    	                DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
    	                OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/MWs/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/KMeansPivotChooser--kmeans.k_${K}"
                        CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/clusters/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/KMeansPivotChooser--kmeans.k_${K}"

                        [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
    	                convert



    	                DATAFILE="/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"
    	                OUTPUT_ROOT_PATH="/storage/brno12-cerit/home/drking/experiments/MWs/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}_non-norm/KMeansPivotChooser--kmeans.k_${K}"
                        CLUSTER_FOLDER_PATH="/storage/brno12-cerit/home/drking/experiments/clusters/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}_non-norm/KMeansPivotChooser--kmeans.k_${K}"

                        [ -f "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}" ] && rm "${OUTPUT_ROOT_PATH}/${PART}.${SOFTASSIGNPARAM}"
    	                convert

                    done
                done
            done   
        done
    done
done



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