#!/bin/bash
#PBS -l walltime=4:0:0
#PBS -l select=1:ncpus=4:mem=4gb


PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

echo "Starting unzip process..."


PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-scl-to-mw-final.sh"
PARTS_SCRIPT_PATH_FULL="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-scl-to-mw-final-full.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/convert-scl-to-mw-final-norm.sh"
DIMS=("256" "128" "64" "32" "16" "8")
BETAS=("0.1" "1" "10")
MODELS=("pku-mmd")
KS=("50" "100" "150" "200" "250" "300" "350" "400" "500" "600" "750")
DATAS=("cs" "cv")

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

DIMS=("64" "32" "16" "8" "4")
MODELS=("pku-mmd-legR" "pku-mmd-legL" "pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR")

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

