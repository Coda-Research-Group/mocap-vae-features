#!/bin/bash

set -e

HDM05_RUNS=(    
    runs/class130*/lightning_logs/version_*
)
PKUMMD_RUNS=(
    runs/actions*/lightning_logs/version_*
)

# for RUN in ${HDM05_RUNS[@]}; do
#     DATA=$(basename $(dirname $(dirname "${RUN}")))
#     DATA="data/hdm05/${DATA}"

#     python reconstruct.py \
#         $RUN \
#         $DATA \
#         --body-model hdm05 \
#         --fps 12 \
#         --limit 5 \
#         --every-n 100
# done

# for RUN in ${HDM05_RUNS[@]}; do
    DATA="/home/drking/Documents/Bakalarka/data/raw/class130-actions-segment80_shift16-coords_normPOS-fps12.data"
    RUN="/home/drking/Documents/Bakalarka/data/runs/version_0"

    python reconstruct.py \
        $RUN \
        $DATA \
        --train-split /home/drking/Documents/Bakalarka/data/runs/version_0/train_ids.txt.gz \
        --valid-split /home/drking/Documents/Bakalarka/data/runs/version_0/valid_ids.txt.gz \
        --test-split /home/drking/Documents/Bakalarka/data/runs/version_0/test_ids.txt.gz \
        --body-model hdm05 \
        --fps 10 \
        --limit 5 \
        --every-n 100
# done