#!/bin/bash
#PBS -q gpu@pbs-m1.metacentrum.cz
#PBS -l walltime=48:0:0 
#PBS -l select=1:ncpus=2:ngpus=1:mem=16gb:gpu_mem=4gb:scratch_local=50gb:cuda_version=12.6
#PBS -o /dev/null
#PBS -e /dev/null

SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'


if [ -z "${PASSED_EXP}" ] || [ -z "${PASSED_BETA}" ]; then
    echo "Error: One or more required variables (PASSED_EXP, PASSED_BETA) were not provided." >&2
    exit 1
fi

CURRENT_EXP=${PASSED_EXP}
CURRENT_BETA=${PASSED_BETA}

module add conda-modules
module add mambaforge

cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment /storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME} does not exist or couldn't be activated!"
    exit 2
}

DIMS=("256" "128" "64" "32" "16" "8")


for DIM in "${DIMS[@]}"; do

    python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/train.py --multirun exp=pku-mmd/${CURRENT_EXP} \
        latent_dim=${DIM} beta=${CURRENT_BETA} body_model=pku-mmd > /dev/null 2>&1

done

conda deactivate

