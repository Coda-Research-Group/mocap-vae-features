#!/bin/bash
#PBS -q gpu@pbs-m1.metacentrum.cz
#PBS -l walltime=48:0:0 
#PBS -l select=1:ncpus=2:ngpus=1:mem=16gb:gpu_mem=4gb:scratch_local=50gb:cuda_version=12.6
#PBS -o /dev/null
#PBS -e /dev/null

SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

if [ -z "${PASSED_EXP}" ] || [ -z "${PASSED_DIM}" ] || [ -z "${PASSED_BETA}" ]; then
    echo "Error: One or more required variables (PASSED_EXP, PASSED_DIM, PASSED_BETA) were not provided." >&2
    exit 1
fi

CURRENT_EXP=${PASSED_EXP}
CURRENT_DIM=${PASSED_DIM}
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

# MODELS=("pku-mmd-torso" "pku-mmd-handL" "pku-mmd-handR" "pku-mmd-legL" "pku-mmd-legR")

# for MODEL in "${MODELS[@]}"; do

#     python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/train.py --multirun exp=pku-mmd/${CURRENT_EXP} \
#         latent_dim=${CURRENT_DIM} beta=${CURRENT_BETA} body_model=${MODEL} > /dev/null 2>&1

# done
# for MODEL in "${MODELS[@]}"; do

    python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/train.py --multirun exp=hdm05/all \
        latent_dim=256 beta=1 body_model=hdm05 > /dev/null 2>&1

# done

conda deactivate
