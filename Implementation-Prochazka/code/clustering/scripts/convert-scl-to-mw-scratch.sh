#!/bin/bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

cd /home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/motionvocabulary/dist/lib || exit


CLS_OBJ="messif.objects.impl.ObjectFloatVectorCosine"
DATAFILE="/home/drking/Documents/bakalarka/data/quantized-vae/predictions_segmented_dim=256_beta=1_modelhdm05.data"
SOFTASSIGNPARAM="--soft-assign D0K1"   # optional (default is D0K1)
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
OUTPUT="/home/drking/Documents/bakalarka/data/SCL-converted-MWs-data" # output file with the input data quantized to motion words
MEMORY="3g"
JDK_PATH="/home/bin/java"
CLUSTER_FILE_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_350/medoids.txt"
VOCTYPE='-v'


CLASSPATH=${CLASSPATH:-'MESSIF.jar:MESSIF-Utility.jar:MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:MCDR.jar:m-index.jar:trove4j-3.0.3.jar'}

#java -Xmx${MEM:-500m} -cp $CLASSPATH messif.motionvocabulary.MotionVocabulary -d $DATAFILE -c $CLS_OBJ --quantize $TOSEQ ${VOCTYPE} $VOCABULARY $SOFTASSIGNPARAM --output $OUTPUT


function convert() {
    echo 'convertBodyPart'

    mkdir -p "${OUTPUT_ROOT_PATH}"

      COMMAND="\
  ${JDK_PATH} \
  -Xmx${MEMORY} \
  -cp ${CLASSPATH} \
  messif.motionvocabulary.MotionVocabulary \
  -d ${DATAFILE} \
  -c ${CLS_OBJ} \
  --quantize ${TOSEQ} ${VOCTYPE} ${CLUSTER_FILE_PATH} ${SOFTASSIGNPARAM} \
  --output ${OUTPUT_ROOT_PATH}\
  "

      echo "${COMMAND}"

      eval "${COMMAND}"
}

 for K in '350'; do
     for SPLIT in '0' '1' '2' '3' '4'; do
#          for SPLIT in '4'; do

         ## Training data for fold 0:
         DATAFILE="/home/drking/Documents/bakalarka/data/SCL/folds-cluster/predictions_segmented_dim=256_beta=1_modelhdm05.data-split${SPLIT}-fold0"
#         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/folds-MW/split${SPLIT}-fold0/KMeansPivotChooser--kmeans.k_${K}-train"
         convert

         ## Testing data for fold 0:
         DATAFILE="/home/drking/Documents/bakalarka/data/SCL/folds-cluster/predictions_segmented_dim=256_beta=1_modelhdm05.data-split${SPLIT}-fold1"
#         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/folds-MW/split${SPLIT}-fold0/KMeansPivotChooser--kmeans.k_${K}-test"
         convert

         ## Training data for fold 1:
         DATAFILE="/home/drking/Documents/bakalarka/data/SCL/folds-cluster/predictions_segmented_dim=256_beta=1_modelhdm05.data-split${SPLIT}-fold1"
#         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/folds-MW/split${SPLIT}-fold1/KMeansPivotChooser--kmeans.k_${K}-train"
         convert

         ## Testing data for fold 1:
         DATAFILE="/home/drking/Documents/bakalarka/data/SCL/folds-cluster/predictions_segmented_dim=256_beta=1_modelhdm05.data-split${SPLIT}-fold0"
#         CLUSTERS_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL-clustering/KMedoidsFastPAM--kmeans.k_${K}"
         OUTPUT_ROOT_PATH="/home/drking/Documents/bakalarka/data/SCL/folds-MW/split${SPLIT}-fold1/KMeansPivotChooser--kmeans.k_${K}-test"
         convert

     done
 done
