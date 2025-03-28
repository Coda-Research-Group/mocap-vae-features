#!/bin/bash

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

JDK_PATH=''
BODY_PART_MWS_FOLDER=''
COMBINER_JAR_PATH=''

## Defaults

JDK_PATH=${JDK_PATH:-'/usr/bin/java'}
COMBINER_JAR_PATH=${COMBINER_JAR_PATH:-'/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/code/clustering/jars/compositeMWCombiner.jar'}

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
    eval "${COMMAND}" >"${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite"
    # 5 BPs + 3 relations
    # eval "${COMMAND}" >"${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite-extended"
}

# HDM05-130
 for SPLIT in '0' '1' '2' '3' '4'; do
     for FOLD in '0' '1'; do
         for TYPE in 'test' 'train'; do
             for K in '250'; do

                 BODY_PART_MWS_FOLDER="/home/xprocha6/cybela1-storage/folds-mw/hdm05/130/split${SPLIT}-fold${FOLD}/KMeansPivotChooser--kmeans.k_${K}-${TYPE}"

                 combineBodyPartsIntoCompositeMW

             done
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
