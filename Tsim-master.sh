#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb


# TODO: replace LOGIN with your login
# TODO: select a particular cluster: https://metavo.metacentrum.cz/pbsmon2/nodes/pbs


REPO_DIR='/storage/brno12-cerit/home/drking/experiments'

module add conda-modules
module add mambaforge

cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/cuda4" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}


for EXP in "cv" "cs" ; do
    for DIM in "256" "128" "64" "32" "16" "8"; do
        for BETA in "0.1" "1" "10"; do
            python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Tsim.py /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/${EXP}/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=pku-mmd.data
            echo "--------------------------------------------------------------------"
        done
    done
done

wait