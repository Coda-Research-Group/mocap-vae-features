#!/bin/bash

#PBS -l walltime=8:0:0
#PBS -l select=1:ncpus=1:mem=4gb
#PBS -o /dev/null
#PBS -e /dev/null

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'
SINGLE_HDM='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/single_precision_hdm05.sh'
SINGLE_PKU='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/single_precision_pku-mmd.sh'


for DATAFILE in "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR" "hdm05-torso"; do
    for BETA in "0.1" "1" "10"; do 
        for DIM in "64" "32" "16" "8" "4"; do 
            for ITER in "1" "2" "3" "4" "5"; do

                JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE}" \
                    "${SINGLE_HDM}"

            done
        done
    done
done

for DATAFILE in "hdm05"; do
    for BETA in "0.1" "1" "10"; do 
        for DIM in "256" "128" "64" "32" "16" "8" "4"; do 
            for ITER in "1" "2" "3" "4" "5"; do

                JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE}" \
                    "${SINGLE_HDM}"

            done
        done
    done
done


for DATAFILE in "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR" "pku-mmd-torso"; do
    for BETA in "0.1" "1" "10"; do 
        for DIM in "64" "32" "16" "8" "4"; do 
            for ITER in "1" "2" "3" "4" "5"; do
                for SETUP in "cs" "cv"; do 

                    JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}_${SETUP}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE},SETUP=${SETUP}" \
                        "${SINGLE_PKU}"
                        
                done
            done
        done
    done
done

for DATAFILE in "pku-mmd"; do
    for BETA in "0.1" "1" "10"; do 
        for DIM in "256" "128" "64" "32" "16" "8" "4"; do 
            for ITER in "1" "2" "3" "4" "5"; do
                for SETUP in "cs" "cv"; do 

                    JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}_${SETUP}"

                    qsub \
                        -N "${JOB_NAME}" \
                        -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE},SETUP=${SETUP}" \
                        "${SINGLE_PKU}"

                done
            done
        done
    done
done


# for DATAFILE in "class130-actions-segment80_shift16-coords_normPOS-fps12"; do
#     for BETA in "0.1" "1" "10"; do 
#         for DIM in "256" "128" "64" "32" "16" "8" "4"; do 
#             for ITER in "1" "2" "3" "4" "5"; do

#                 JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE}" \
#                     "${SINGLE_HDM}"

#             done
#         done
#     done
# done

# for FILE in "cv_parts/motion_hands_l_norm" "cv_parts/motion_hands_r_norm" "cv_parts/motion_legs_l_norm" "cv_parts/motion_legs_r_norm" "cv_parts/motion_torso_norm"; do

#     python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.data-cv-train \
#         --subset-size 3000 --subset-repeats 5 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.json --plot-output /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.png
# done

# for FILE in "cs_parts/motion_hands_l_norm" "cs_parts/motion_hands_r_norm" "cs_parts/motion_legs_l_norm" "cs_parts/motion_legs_r_norm" "cs_parts/motion_torso_norm"; do

#     python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.data-cs-train \
#         --subset-size 3000 --subset-repeats 5 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.json --plot-output /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.png
# done

# for EXP in "cs" "cv"; do
#     python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-${EXP}-train \
#         --subset-size 3000 --subset-repeats 5 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10-${EXP}.json --plot-output /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.png

# done

