#!/bin/bash
#PBS -l walltime=2:0:0
#PBS -l select=1:ncpus=1:mem=2gb

WHOLE_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-pku-full.sh"
PARTS_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-pku-parts.sh"
PARTS_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Implementation-Prochazka/code/clustering/scripts/cluster-scl-pku-parts-norm.sh"

for ITER in 3; do
    for BETA in "0.1" "1"; do 
        for DIM in 32 64; do 
            for SETUP in "cv" "cs"; do

                JOB_NAME="clustering_full_hdm05_${ITER}__${DIM}_${BETA}_${SETUP}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM},SETUP=${SETUP}" \
                    "${WHOLE_SCRIPT_PATH}"
            done
        done
    done
done


for ITER in 3; do
    for BETA in "0.1" "1"; do 
        for DIM in 8 16; do 
            for SETUP in "cv" "cs"; do
                for PART in "pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR"; do

                    JOB_NAME="clustering_full_hdm05_${ITER}__${DIM}_${BETA}_${SETUP}"
    
                    qsub \
                        -N "${JOB_NAME}" \
                        -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM},SETUP=${SETUP},PART=${PART}" \
                        "${PARTS_SCRIPT_PATH}"
    
    
                    qsub \
                        -N "${JOB_NAME}_norm" \
                        -v "PASSED_ITER=${ITER},PASSED_BETA=${BETA},PASSED_DIM=${DIM},SETUP=${SETUP},PART=${PART}" \
                        "${PARTS_SCRIPT_PATH_NORM}"

                done
            done
        done
    done
done


echo "Job finished."

