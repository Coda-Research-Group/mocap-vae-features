#!/bin/bash

WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/hdm05-full.sh"

for ITER in 3 4 5; do
    for BETA in "0.1" "1" "10"; do
        for DIM in 256; do
            for K in 10 25 50 100 200 400 800 1600 3200;do 

                JOB_NAME="quantization_hdm05_${DIM}_${BETA}_${ITER}_${K}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K}" \
                    "${WHOLE_SCRIPT_PATH}"

            done
        done
    done
done