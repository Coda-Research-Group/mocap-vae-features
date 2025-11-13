#!/bin/bash
#PBS -l walltime=12:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/usr/bin/java'


for K in "350" ; do

    rm -f "/home/drking/Documents/Bakalarka/data/test/pku/results/results.txt"


    COMMAND="${JDK_PATH} -jar /home/drking/Documents/Bakalarka/mocap-vae-features/evaluator.jar \
-fp /home/drking/Documents/Bakalarka/data/test/pku/MWs/MWs.D0K1 \
-k 4 \
-cs \
-dd /home/drking/Documents/Bakalarka/data/data/hdm05/category_description.txt \
"
    mkdir -p "/home/drking/Documents/Bakalarka/data/test/pku/results/"
    eval "${COMMAND}" >> "/home/drking/Documents/Bakalarka/data/test/pku/results/results.txt"


    COMMAND="${JDK_PATH} -jar /home/drking/Documents/Bakalarka/mocap-vae-features/evaluator.jar \
-fp /home/drking/Documents/Bakalarka/data/test/pku/MWs/MWs.D0K1 \
-dd /home/drking/Documents/Bakalarka/data/data/hdm05/category_description.txt \
-cs \
"
    eval "${COMMAND}" >> "/home/drking/Documents/Bakalarka/data/test/pku/results/results.txt"

done


# for K in 10 20 35 50 60 80 100 150 200 250 300 350 400 500 750 1000 1250 1500 1750 2000 2500; do

#     rm "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt"

#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/full/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K} \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# -k 4 \
# "
#     mkdir -p "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/lat_dim=${DIM}_beta=${BETA}/${K}/"
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt"

#     COMMAND="${JDK_PATH} -jar /storage/brno12-cerit/home/drking/experiments/mocap-vae-features/evaluator.jar \
# -fp /storage/brno12-cerit/home/drking/experiments/MWs/hdm05/full/model=hdm05_lat-dim=${DIM}_beta=${BETA}/${ITER}/KMeansPivotChooser--kmeans.k_${K} \
# -dd /storage/brno12-cerit/home/drking/data/hdm05/category_description.txt \
# "
#     eval "${COMMAND}" >> "/storage/brno12-cerit/home/drking/experiments/results/hdm05/full/lat_dim=${DIM}_beta=${BETA}/${K}/results-${ITER}.txt"

# done
