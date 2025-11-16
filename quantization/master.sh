#!/bin/bash

WHOLE_SCRIPT_PATH_HDM05="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/hdm05-full.sh"
WHOLE_SCRIPT_PATH_PKU_MMD="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/pku-mmd-full.sh"

PART="hdm05"

for ITER in 1 2 3 4 5; do
    for BETA in "0.1" "1"; do
        for DIM in 16 32 64 128; do

            gunzip -kf "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"

            # aby se smazal v pripade 2. pokusu ten predchozi.
            rm -f /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

            perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


            for K in 10 25 50 100 200 400 800 1600 3200;do 

                JOB_NAME="quantization_hdm05_${DIM}_${BETA}_${ITER}_${K}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K}" \
                    "${WHOLE_SCRIPT_PATH_HDM05}"

            done
        done
    done
done

PART="pku-mmd"


for SETUP in "cs" "cv"; do 
    for ITER in 1 2 3 4 5; do
        for BETA in "0.1" "1"; do
            for DIM in 16 32 64 128; do

                python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/clustering/subset-maker.py "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data-train" --output "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented_subset.data"

                # aby se smazal v pripade 2. pokusu ten predchozi.
                rm -f /storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

                perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented_subset.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


                for K in 25 50 100 200 400 800 1600 3200 6400; do 

                    JOB_NAME="quantization_pku-mmd_${SETUP}_${DIM}_${BETA}_${ITER}_${K}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP}" \
                        "${WHOLE_SCRIPT_PATH_PKU_MMD}"

                done
            done
        done
    done
done