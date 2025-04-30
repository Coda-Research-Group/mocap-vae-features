#!/bin/bash
#PBS -l walltime=24:0:0
#PBS -l select=1:ncpus=4:mem=32gb:scratch_local=50gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

# avail_classes 

# SequenceMotionWordsDTW.class;
# SequenceMotionWordsNMatchesDTW.class;
# SequenceMotionWordsSoftAssignmentDTW.class;
# SequenceMotionWordsNGramsJaccard.class;
# SequenceSegmentCodeListDTW.class;

WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single.sh"
WORKER_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-norm.sh"

for EXP in "cv" "cs"; do
    for DIM in "64" "32" "16" "8" "4"; do
        for BETA in "0.1" "1" "10"; do

            JOB_NAME="vae_${EXP}_${DIM}_${BETA}"
            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_DATA=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH}"

            JOB_NAME="vae_${EXP}_${DIM}_${BETA}_norm"
            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_DATA=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH_NORM}"

        done
    done
done

