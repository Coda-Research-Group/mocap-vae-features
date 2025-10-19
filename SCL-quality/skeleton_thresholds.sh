#!/bin/bash

#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=16:mem=64gb
#PBS -o /dev/null
#PBS -e /dev/null

REPO_DIR='/storage/brno12-cerit/home/drking/experiments'
ENV_NAME='cuda4'

module add conda-modules
module add mambaforge

conda activate "/storage/brno12-cerit/home/drking/.conda/envs/${ENV_NAME}" || {
    echo >&2 "Conda environment does not exist!"
    exit 2
}

# for FILE in "parts_norm/motion_hands_l_norm" "parts_norm/motion_hands_r_norm" "parts_norm/motion_legs_l_norm" "parts_norm/motion_legs_r_norm" "parts_norm/motion_torso_norm" "class130-actions-segment80_shift16-coords_normPOS-fps12"; do

#     python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.npz \
#         --subset-size 3000 --subset-repeats 3 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/hdm05/${FILE}.json
# done

# for FILE in "cv_parts/motion_hands_l_norm" "cv_parts/motion_hands_r_norm" "cv_parts/motion_legs_l_norm" "cv_parts/motion_legs_r_norm" "cv_parts/motion_torso_norm" "actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10"; do

#     python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.data-cv-train \
#         --subset-size 3000 --subset-repeats 3 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.json
# done

# for FILE in "cs_parts/motion_hands_l_norm" "cs_parts/motion_hands_r_norm" "cs_parts/motion_legs_l_norm" "cs_parts/motion_legs_r_norm" "cs_parts/motion_torso_norm" "actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10"; do

#     python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.data-cs-train \
#         --subset-size 3000 --subset-repeats 3 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/${FILE}.json
# done

for EXP in "cs" "cv"; do
    python3 /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/SCL-quality/dtw_thresholds.py --input /storage/brno12-cerit/home/drking/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-${EXP}-train \
        --subset-size 3000 --subset-repeats 3 --n-jobs 16 --output /storage/brno12-cerit/home/drking/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10-${EXP}.json

done