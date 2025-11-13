#!/bin/bash

WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/hdm05-full.sh"

PART="hdm05"

for ITER in 3 4 5; do
    for BETA in "0.1" "1" "10"; do
        for DIM in 256; do

            gunzip -kf "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"

            # aby se smazal v pripade 2. pokusu ten predchozi.
            rm -f /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

            perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


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

storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=256_beta=0.1/2
/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=hdm05_lat-dim=256_beta=0.1/2/