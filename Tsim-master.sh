#!/bin/bash

#PBS -l walltime=48:0:0
#PBS -l select=1:ncpus=4:mem=64gb


# TODO: replace LOGIN with your login
# TODO: select a particular cluster: https://metavo.metacentrum.cz/pbsmon2/nodes/pbs


REPO_DIR='/storage/brno12-cerit/home/drking/experiments'


cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 1
}



for EXP in "all" ; do
    # for DIM in "256" "128" "64" "32" "16" "8" "4" "2" "1"; do
    #     for BETA in "0.1" "1" "10"; do
    for DIM in "8"; do
        for BETA in "1"; do

            python /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/Tsim.py /storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/hdm05/all/lat_dim=${DIM}_beta=${BETA}/predictions_segmented_model=hdm05.data

        done
    done
done

wait