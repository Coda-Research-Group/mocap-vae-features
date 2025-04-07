#!/bin/bash

#PBS -q gpu@pbs-m1.metacentrum.cz
#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=8:ngpus=1:mem=16gb:gpu_mem=4gb:scratch_local=50gb:cuda_version=12.6

# TODO: replace LOGIN with your login
# TODO: select a particular cluster: https://metavo.metacentrum.cz/pbsmon2/nodes/pbs

export OMP_NUM_THREADS=$PBS_NUM_PPN # set it equal to PBS variable PBS_NUM_PPN (number of CPUs in a chunk)
export GPUS=$CUDA_VISIBLE_DEVICES

echo "image: ${SINGULARITY_IMG}"
echo "CPUs: ${OMP_NUM_THREADS}"
echo "GPUs: ${GPUS}"

SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
PBS_LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/pbs'
REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'


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


# 5x3x5 = 75
for EXP in "all"; do
    for DIM in "64" "32" "16" "8" "4"; do
        for BETA in "0.1" "1" "10"; do
            for MODEL in "hdm05-torso" "hdm05-handL" "hdm05-handR" "hdm05-legL" "hdm05-legR"; do
                python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/train.py --multirun exp=hdm05/${EXP} \
                    latent_dim=${DIM} beta=${BETA} body_model=${MODEL}
            done
        done
    done
done

wait
