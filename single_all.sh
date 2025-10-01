#!/bin/bash

export OMP_NUM_THREADS=$PBS_NUM_PPN # set it equal to PBS variable PBS_NUM_PPN (number of CPUs in a chunk)
export GPUS=$CUDA_VISIBLE_DEVICES

echo "image: ${SINGULARITY_IMG}"
echo "CPUs: ${OMP_NUM_THREADS}"
echo "GPUs: ${GPUS}"

SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
PBS_LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/pbs'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

DIMS=("256" "128" "64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")
ITERS=("1" "2" "3")

# PATHS to scripts
WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/single_config.sh"


module add conda-modules
module add mambaforge

cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
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
