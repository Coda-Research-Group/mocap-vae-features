#!/bin/bash

set -e

HDM05_RUNS=(    
    runs/class130*/lightning_logs/version_*
)
PKUMMD_RUNS=(
    runs/actions*/lightning_logs/version_*
)

for RUN in ${HDM05_RUNS[@]}; do
    # DATA=$(basename $(dirname $(dirname "${RUN}")))
    DATA="/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/parts/motion_torso.npz"

    python reconstruct.py \
        $RUN \
        $DATA \
        --train-split "data/hdm05/2version/splits/2folds20_80split_2-class122.txt" \
        --valid-split "data/hdm05/2version/splits/2folds20_80split_1-class122.txt" \
        --test-split "data/hdm05/2version/splits/2folds20_80split_1-class122.txt" \
        --body-model hdm05-torso \
        --fps 12 \
        --limit 5 \
        --every-n 100
done

# for RUN in ${PKUMMD_RUNS[@]}; do
#     DATA=$(basename $(dirname $(dirname "${RUN}")))
#     DATA="data/pku-mmd/${DATA}"

#     python reconstruct.py \
#         $RUN \
#         $DATA \
#         --train-split "data/pku-mmd/CS_train_objects_messif-lines.txt" \
#         --valid-split "data/pku-mmd/CS_test_objects_messif-lines.txt" \
#         --test-split "data/pku-mmd/CS_test_objects_messif-lines.txt" \
#         --body-model pku-mmd \
#         --fps 30 \
#         --limit 5 \
#         --every-n 100
# done