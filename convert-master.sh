#!/bin/bash
#PBS -l walltime=4:0:0
#PBS -l select=1:ncpus=4:mem=4gb


PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting unzip process..."


PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-scl-to-mw-final.sh"
PARTS_SCRIPT_PATH_FULL="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-scl-to-mw-final-full.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-scl-to-mw-final-norm.sh"
# DIMS=("256" "128" "64" "32" "16" "8")
# BETAS=("0.1" "1" "10")
# MODELS=("hdm05")
# KS=("50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750")
# DATAS=("all")

DIMS=("256" "128" "64" "32" "16" "8")
BETAS=("0.1" "1" "10")
MODELS=("1" "2" "3" "4" "5")
# KS=("1000" "2000" "3000" "4000" "5000" "6000" "7000" "8000" "9000" "10000")
KS=("50" "100" "200" "500" "650")
DATAS=("k")


for DIM in "${DIMS[@]}"; do
    for BETA in "${BETAS[@]}"; do
        for MODEL in "${MODELS[@]}"; do
            for DATA in "${DATAS[@]}"; do
                JOB_NAME="converting_${DIM}_${BETA}_${MODEL}_${DATA}_full"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "PASSED_DATA=${DATA},PASSED_DIM=${DIM},PASSED_BETA=${BETA},PASSED_MODEL=${MODEL}" \
                    "${PARTS_SCRIPT_PATH_FULL}"
            done
        done
    done
done

# DIMS=("64" "32" "16" "8" "4")
# MODELS=("hdm05-legR" "hdm05-legL" "hdm05-torso" "hdm05-handL" "hdm05-handR")

# for DIM in "${DIMS[@]}"; do
#     for BETA in "${BETAS[@]}"; do
#         for MODEL in "${MODELS[@]}"; do
#             for DATA in "${DATAS[@]}"; do
#                 JOB_NAME="converting_${DIM}_${BETA}_${MODEL}_${DATA}"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "PASSED_DATA=${DATA},PASSED_DIM=${DIM},PASSED_BETA=${BETA},PASSED_MODEL=${MODEL}" \
#                     "${PARTS_SCRIPT_PATH}"
                
#                 JOB_NAME="converting_${DIM}_${BETA}_${MODEL}_${DATA}_norm"

#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "PASSED_DATA=${DATA},PASSED_DIM=${DIM},PASSED_BETA=${BETA},PASSED_MODEL=${MODEL}" \
#                     "${PARTS_SCRIPT_PATH_NORM}"
#             done
#         done
#     done
# done

