#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=8gb:scratch_local=50gb
#PBS -o /dev/null
#PBS -e /dev/null

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

JDK_PATH=''
BODY_PART_MWS_FOLDER=''
COMBINER_JAR_PATH=''

## Defaults

JDK_PATH=${JDK_PATH:-'/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'}
COMBINER_JAR_PATH=${COMBINER_JAR_PATH:-'/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/compositeMWCombiner.jar'}

## Functions

function combineBodyPartsIntoCompositeMW() {
    echo 'combineBodyPartsIntoCompositeMW'

    FILE_NAME=$(basename "${BODY_PART_MWS_FOLDER}")

    COMMAND="\
${JDK_PATH} \
-jar ${COMBINER_JAR_PATH} \
--sequenceFolder='${BODY_PART_MWS_FOLDER}' \
"

    echo "${COMMAND}"

    # 5 BPs
    eval "${COMMAND}" >"/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/composites/Elki_${DIM}_${BETA}_${K}.composite"
    # 5 BPs + 3 relations
    # eval "${COMMAND}" >"${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite-extended"
}

# HDM05-130
for DIM in "64" "32" "16" "8" "4"; do
  	for BETA in "0.1" "1" "10"; do
 		for K in "5" "10" "20" "50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750"; do

            BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/MWs-parts/KMeansPivotChooser--kmeans.k_${K}"

            combineBodyPartsIntoCompositeMW

        done
    done
done

## HDM05-65
#for SPLIT in '0' '1' '2' '3' '4'; do
#    for FOLD in '0,1,2,3,4,5,6,7,8' '0,1,2,3,4,5,6,7,9' '0,1,2,3,4,5,6,8,9' '0,1,2,3,4,5,7,8,9' '0,1,2,3,4,6,7,8,9' '0,1,2,3,5,6,7,8,9' '0,1,2,4,5,6,7,8,9' '0,1,3,4,5,6,7,8,9' '0,2,3,4,5,6,7,8,9' '1,2,3,4,5,6,7,8,9'; do
#        for TYPE in 'test' 'train'; do
#            for K in '250' '300'; do
#
#                BODY_PART_MWS_FOLDER="/home/xprocha6/cybela1-storage/folds-mw/hdm05/65/split${SPLIT}-fold${FOLD}/KMeansPivotChooser--kmeans.k_${K}-${TYPE}"
#
#                combineBodyPartsIntoCompositeMW
#
#            done
#        done
#    done
#done

# PKU CS
# for TYPE in 'test' 'train'; do # 5 body parts
# for TYPE in 'test-extended' 'train-extended'; do # 5 body parts + 3 relations
#     for K in '300'; do

#         BODY_PART_MWS_FOLDER="/home/xprocha6/cybela1-storage/folds-mw/pku/cs/KMeansPivotChooser--kmeans.k_${K}-${TYPE}"

#         combineBodyPartsIntoCompositeMW

#     done
# done

# PKU CV
# for TYPE in 'test' 'train'; do # 5 body parts
# for TYPE in 'test-extended' 'train-extended'; do # 5 body parts + 3 relations
#     for K in '300'; do

#         BODY_PART_MWS_FOLDER="/home/xprocha6/cybela1-storage/folds-mw/pku/cv/KMeansPivotChooser--kmeans.k_${K}-${TYPE}"

#         combineBodyPartsIntoCompositeMW

#     done
# done
