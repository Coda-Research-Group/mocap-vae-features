#!/bin/bash

WHOLE_SCRIPT_PATH_HDM05="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/hdm05-full.sh"
WHOLE_SCRIPT_PATH_PKU_MMD="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/pku-mmd-full.sh"
WHOLE_SCRIPT_PATH_PKU_MMD_SOFT="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/pku-mmd-soft.sh"
WHOLE_SCRIPT_PATH_PKU_MMD_MO="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/quantization/pku-mmd-mo.sh"

PART="hdm05"

# for ITER in 1 2 3 4 5; do
#     for BETA in "0.1" "1"; do
#         for DIM in 16 32 64 128; do

#             gunzip -kf "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"

#             # aby se smazal v pripade 2. pokusu ten predchozi.
#             rm -f /storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

#             perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


#             for K in 10 25 50 100 200 400 800 1600 3200;do 

#                 JOB_NAME="quantization_hdm05_${DIM}_${BETA}_${ITER}_${K}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K}" \
#                     "${WHOLE_SCRIPT_PATH_HDM05}"

#             done
#         done
#     done
# done
for PART in "hdm05-handR" "hdm05-handL" "hdm05-legR" "hdm05-legL" "hdm05-torso"; do
    for ITER in 3; do
        for BETA in "0.1" "1"; do
            for DIM in 8 16 32 64; do

                gunzip -kf "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data.gz"

                # aby se smazal v pripade 2. pokusu ten predchozi.
                rm -f /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

                perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/hdm05/all/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


                for K in 10 25 50 100 200 400 800 1600 3200;do 

                    JOB_NAME="q_${PART}_${DIM}_${BETA}_${ITER}_${K}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},PART=${PART}" \
                        "${WHOLE_SCRIPT_PATH_HDM05}"

                done
            done
        done
    done
done


# PART="pku-mmd"
# for SETUP in "cs" "cv"; do 
#     for ITER in 1 2 3 4 5; do
#         for BETA in "0.1" "1"; do
#             for DIM in 32 64 128 256; do

#                 python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/clustering/subset-maker.py "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data-train" --output "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented_subset.data"

#                 # aby se smazal v pripade 2. pokusu ten predchozi.
#                 rm -f /storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

#                 perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented_subset.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


#                 for K in 25 50 100 200 400 800 1600 3200 6400 12800; do 

#                     JOB_NAME="quantization_${PART}_${SETUP}_${DIM}_${BETA}_${ITER}_${K}"

#                     qsub \
#                         -N "${JOB_NAME}" \
#                         -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP},PART=${PART}" \
#                         "${WHOLE_SCRIPT_PATH_PKU_MMD}"

#                 done
#             done
#         done
#     done
# done

for PART in "pku-mmd-handR" "pku-mmd-handL" "pku-mmd-legR" "pku-mmd-legL" "pku-mmd-torso"; do
    for SETUP in "cs" "cv"; do 
        for ITER in 3; do
            for BETA in "0.1" "1"; do
                for DIM in 8 16 32 64; do

                    python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/clustering/subset-maker.py "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented.data-train" --output "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented_subset.data"

                    # aby se smazal v pripade 2. pokusu ten predchozi.
                    rm -f /storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data

                    perl /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-from-messif.pl "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/predictions_segmented_subset.data" >> "/storage/brno12-cerit/home/drking/experiments/SCL-non-norm/pku-mmd/${SETUP}/model=${PART}_lat-dim=${DIM}_beta=${BETA}/${ITER}/elki-predictions_segmented.data"


                    for K in 25 50 100 200 400 800 1600 3200 6400; do 

                        JOB_NAME="q_${PART}_${SETUP}_${DIM}_${BETA}_${ITER}_${K}"

                        qsub \
                            -N "${JOB_NAME}" \
                            -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP},PART=${PART}" \
                            "${WHOLE_SCRIPT_PATH_PKU_MMD}"

                    done
                done
            done
        done
    done
done

PART="pku-mmd"
for SETUP in "cv"; do 
    for ITER in 3; do
        for DIM in 256; do
            for K in 100 200 400 800 1600 3200 6400 12800; do 
                BETA=1

                for ASSIGN in "D0.63K6" "D0.31K6" "D0.16K6" "D0.08K6"; do 

                    JOB_NAME="soft-test_${PART}_${SETUP}_${DIM}_${BETA}_${ASSIGN}_${K}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP},PART=${PART},ASSIGN=${ASSIGN}" \
                        "${WHOLE_SCRIPT_PATH_PKU_MMD_SOFT}"
                done 
            
                BETA="0.1"
                for ASSIGN in "D0.44K6" "D0.22K6" "D0.11K6" "D0.06K6"; do 

                    JOB_NAME="soft-test_${PART}_${SETUP}_${DIM}_${BETA}_${ASSIGN}_${K}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP},PART=${PART},ASSIGN=${ASSIGN}" \
                        "${WHOLE_SCRIPT_PATH_PKU_MMD_SOFT}"
                done 
            done
        done
    done
done

for SETUP in "cs"; do 
    for ITER in 3; do
        for DIM in 256; do
            for K in 100 200 400 800 1600 3200 6400 12800; do 
                BETA=1

                for ASSIGN in "D0.63K6" "D0.31K6" "D0.16K6" "D0.08K6"; do 

                    JOB_NAME="soft-test_${PART}_${SETUP}_${DIM}_${BETA}_${ASSIGN}_${K}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP},PART=${PART},ASSIGN=${ASSIGN}" \
                        "${WHOLE_SCRIPT_PATH_PKU_MMD_SOFT}"
                done
            
                BETA="0.1"
                for ASSIGN in "D0.44K6" "D0.22K6" "D0.11K6" "D0.06K6"; do 

                    JOB_NAME="soft-test_${PART}_${SETUP}_${DIM}_${BETA}_${ASSIGN}_${K}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITER=${ITER},BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP},PART=${PART},ASSIGN=${ASSIGN}" \
                        "${WHOLE_SCRIPT_PATH_PKU_MMD_SOFT}"
                done 
            done
        done
    done
done


# for BETA in "0.1" "1"; do
#     for DIM in 256; do
#         for SETUP in "cs" "cv"; do
#             for K in 100 200 400 800 1600; do 
#                 JOB_NAME="mo-test_${PART}_${SETUP}_${DIM}_${BETA}_${K}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "BETA=${BETA},DIM=${DIM},K=${K},SETUP=${SETUP}" \
#                     "${WHOLE_SCRIPT_PATH_PKU_MMD_MO}"

#             done 
#         done
#     done
# done
