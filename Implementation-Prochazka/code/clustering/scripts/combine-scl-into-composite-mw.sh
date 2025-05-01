#!/bin/bash
#PBS -l walltime=4:0:0
#PBS -l select=1:ncpus=4:mem=16gb


# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

JDK_PATH=''
BODY_PART_MWS_FOLDER=''
COMBINER_JAR_PATH=''

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
    [ -f "${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite" ] && rm "${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite"



    echo "${COMMAND}"

    # 5 BPs
    eval "${COMMAND}" >"${BODY_PART_MWS_FOLDER}/../${FILE_NAME}.composite"

}

DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")
MODELS=("hdm05")
KS=("50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750" "1000" "1500" "2000")
DATAS=("all")

for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
        for K in "${KS[@]}"; do
            for MODEL in "${MODELS[@]}"; do
                for DATA in "${DATAS[@]}"; do

                    BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/${DATA}/lat_dim=${DIM}_beta=${BETA}/MWs-parts-M/KMeansPivotChooser--kmeans.k_${K}"

                    combineBodyPartsIntoCompositeMW

                    BODY_PART_MWS_FOLDER="/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions-norm/hdm05/${DATA}/lat_dim=${DIM}_beta=${BETA}/MWs-parts-M/KMeansPivotChooser--kmeans.k_${K}"

                    combineBodyPartsIntoCompositeMW

                done
            done
        done
    done
done
        