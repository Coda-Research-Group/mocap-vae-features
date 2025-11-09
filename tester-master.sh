#!/bin/bash
#PBS -l walltime=2:0:0
#PBS -l select=1:ncpus=4:mem=8gb
#PBS -o /dev/null
#PBS -e /dev/null


JDK_PATH='/storage/brno12-cerit/home/drking/jdk-21.0.7/bin/java'

WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single.sh"
WORKER_SCRIPT_PATH_NORM="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-norm.sh"
WORKER_SCRIPT_PATH_FULL="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-hdm05.sh"
WORKER_SCRIPT_PATH_PART="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-hdm05-parts.sh"
WORKER_SCRIPT_PATH_SCL="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-scl.sh"
WORKER_SCRIPT_PATH_MO="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-MO.sh"
WORKER_SCRIPT_PATH_MO_PKU="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-MO-pku.sh"
WORKER_SCRIPT_PATH_BASE="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/tester-single-base.sh"

# for EXP in "cs"; do
#     for DIM in "64" "32" "16" "8" "4"; do
#         for BETA in "0.1" "1" "10"; do

#             JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}"
#             echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
#             qsub \
#                 -N "${JOB_NAME}" \
#                 -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#                 "${WORKER_SCRIPT_PATH}"

#             JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}_norm"
#             echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"
#             qsub \
#                 -N "${JOB_NAME}" \
#                 -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#                 "${WORKER_SCRIPT_PATH_NORM}"

#         done
#     done
# done

# for EXP in "all"; do
#     for DIM in "32" "64"; do
#         for BETA in "0.1" "1" "10"; do
#             for ITER in 3; do

#                 JOB_NAME="evaluation_${EXP}_${ITER}_all"
#                 qsub \
#                     -N "${JOB_NAME}" \
#                     -v "PASSED_ITER=${ITER},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#                     "${WORKER_SCRIPT_PATH}"

#                 # JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}_cv"
#                 # echo "Submitting job for EXP=cv, DIM=${DIM}, BETA=${BETA}"
#                 # qsub \
#                 #     -N "${JOB_NAME}" \
#                 #     -v "PASSED_EXP=cv,PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#                 #     "${WORKER_SCRIPT_PATH_MO_PKU}"

#                 # JOB_NAME="evaluation_${EXP}_${DIM}_${BETA}_cs"
#                 # echo "Submitting job for EXP=cs, DIM=${DIM}, BETA=${BETA}"
#                 # qsub \
#                 #     -N "${JOB_NAME}" \
#                 #     -v "PASSED_EXP=cs,PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
#                 #     "${WORKER_SCRIPT_PATH_MO_PKU}"

#             done 
#         done
#     done
# done

for ITER in 1 2 3 4 5; do 
    for BETA in "0.1" "1"; do 
        for DIM in 32 64; do 

            JOB_NAME="evaluation_${DIM}_${BETA}_${ITER}_all"

            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_ITER=${ITER},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH_FULL}"

        done
    done
done

for ITER in 3; do 
    for BETA in "0.1" "1"; do 
        for DIM in 8 16; do
            for PART in "hdm05-handR" "hdm05-handL" "hdm05-legR" "hdm05-legL" "hdm05-torso"; do

                JOB_NAME="evaluation_${DIM}_${BETA}_${ITER}_${PART}"

                qsub \
                    -N "${JOB_NAME}" \
                    -v "PASSED_ITER=${ITER},PASSED_DIM=${DIM},PASSED_BETA=${BETA},PASSED_PART=${PART}" \
                    "${WORKER_SCRIPT_PATH_PART}"

            done
        done
    done
done


# for BETA in "0.1" "1"; do
#     for DIM in 8 16; do
#         qsub \
#             -v "DIM=${DIM},BETA=${BETA}" \
#             "${WORKER_SCRIPT_PATH_MO}"

#     done
# done