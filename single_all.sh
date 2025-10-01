#!/bin/bash

SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
PBS_LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/pbs'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/single_config.sh"


cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}


for MOD in "hdm05" "hdm05-torso" "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR"; do
    for ITER in "1" "2" "3"; do
        JOB_NAME="training_VAE_on_${MOD}-iteration-${ITER}"
        echo "Submitting job for MODEL = ${MOD}, ITERATION = ${ITER}"

        qsub \
            -N "${JOB_NAME}" \
            -v "ITERATION=${ITER},MODEL=${MOD}" \
            "${WORKER_SCRIPT_PATH}"
    done
done

wait
