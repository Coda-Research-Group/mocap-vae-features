#!/bin/bash
#PBS -l walltime=2:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single.sh"
WORKER_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-norm.sh"
WORKER_SCRIPT_PATH_FULL="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-full.sh"
WORKER_SCRIPT_PATH_SCL="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-scl.sh"

for EXP in "cs" "cv"; do
    for DIM in "64" "32" "16" "8" "4"; do
        for BETA in "0.1" "1" "10"; do

            JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}"
            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH}"

            JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}_norm"
            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH_NORM}"

        done
    done
done

for EXP in "cs" "cv"; do
    for DIM in "256" "128" "64" "32" "16" "8" ; do
        for BETA in "0.1" "1" "10"; do

            JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}_full"
            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH_FULL}"

        done
    done
done

# for EXP in "hdm05/all" "pku-mmd/cv" "pku-mmd/cs"; do
    # for DIM in "256" "128" "64" "32" "16" "8" ; do
    #     for BETA in "0.1" "1" "10"; do

    #         JOB_NAME="evaluation_${DIM}_${BETA}_scl"
    #         echo "Submitting job for DIM=${DIM}, BETA=${BETA}"
    #         qsub \
    #             -N "${JOB_NAME}" \
    #             -v "PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
    #             "${WORKER_SCRIPT_PATH_SCL}"

    #     done
    # done
# done