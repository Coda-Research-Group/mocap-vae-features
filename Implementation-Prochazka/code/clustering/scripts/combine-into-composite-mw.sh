#!/bin/bash
#PBS -l walltime=4:0:0
#PBS -l select=1:ncpus=4:mem=16gb

## Defaults

JDK_PATH="/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java"
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
    eval "${COMMAND}" >"${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite"
    # 5 BPs + 3 relations
    # eval "${COMMAND}" >"${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite-extended"
}

# HDM05-130
for BETA in "0.1" "1"; do 
    for DIM in 8 16 32 64; do 
        for K in 10 25 50 100 200 400 800 1600 3200; do
        
            # BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/MWs/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}"
            # combineBodyPartsIntoCompositeMW

            BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/elki-MWs-non-norm/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}"
            combineBodyPartsIntoCompositeMW

            BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/elki-MWs/hdm05/all/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}"
            combineBodyPartsIntoCompositeMW

        done
    done
done

for SETUP in "cv" "cs"; do 
    for BETA in "0.1" "1"; do
        for DIM in 8 16 32 64; do
            for K in 25 50 100 200 400 800 1600 3200 6400 ; do 

                    BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/elki-MWs-non-norm/pku-mmd/${SETUP}/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}"
                    combineBodyPartsIntoCompositeMW

                    BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/elki-MWs/pku-mmd/${SETUP}/grouped/group_lat-dim=${DIM}_beta=${BETA}_k=${K}"
                    combineBodyPartsIntoCompositeMW


            done
        done
    done
done