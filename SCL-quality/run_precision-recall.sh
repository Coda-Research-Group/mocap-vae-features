#!/bin/bash

#PBS -l walltime=2:0:0
#PBS -l select=1:ncpus=1:mem=4gb
#PBS -o /dev/null
#PBS -e /dev/null

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'
SINGLE_HDM='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/single_precision_hdm05.sh'
SINGLE_PKU='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/single_precision_pku-mmd.sh'


# for DATAFILE in "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR" "hdm05-torso"; do
#     for BETA in "0.1" "1" "10"; do 
#         for DIM in "64" "32" "16" "8" "4"; do 
#             for ITER in "1" "2" "3" "4" "5"; do

#                 JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE}" \
#                     "${SINGLE_HDM}"
#                 sleep 0.2
#             done
#         done
#     done
# done

# for DATAFILE in "hdm05"; do
#     for BETA in "0.1" "1" "10"; do 
#         for DIM in "256" "128" "64" "32" "16" "8" "4"; do 
#             for ITER in "1" "2" "3" "4" "5"; do

#                 JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE}" \
#                     "${SINGLE_HDM}"
#                 sleep 0.2

#             done
#         done
#     done
# done

# for DATAFILE in "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR" "pku-mmd-torso"; do
#     for BETA in "0.1" "1" "10"; do 
#         for DIM in "64" "32" "16" "8" "4"; do 
#             for ITER in "1" "2" "3" "4" "5"; do
#                 for SETUP in "cs" "cv"; do 

#                     JOB_NAME="vae_${DATAFILE}_${DIM}_${BETA}_${ITER}_${SETUP}"

#                     qsub \
#                         -N "${JOB_NAME}" \
#                         -v "ITERATION=${ITER},DIMENSION=${DIM},BETA=${BETA},DATAFILE=${DATAFILE},SETUP=${SETUP}" \
#                         "${SINGLE_PKU}"

#                     sleep 0.2

#                 done
#             done
#         done
#     done
# done

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
                    sleep 0.2

                done
            done
        done
    done
done



