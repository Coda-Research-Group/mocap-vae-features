#!/bin/bash

# --- Configuration ---
# Define the parameter options
EXPS=("cs" "cv")
DIMS=("64" "32" "16" "8" "4")
BETAS=("0.1" "1" "10")

# Path to the worker script that will be qsub'd
WORKER_SCRIPT_PATH="/storage/brno12-cerit/home/drking/experiments/mocap-vae-features/single_job.sh" # Adjust if needed

# Base directory for PBS output/error logs (ensure this exists!)
PBS_LOG_BASE_DIR="/storage/brno12-cerit/home/drking/experiments/pbs"
mkdir -p "${PBS_LOG_BASE_DIR}"

# --- Submission Loop ---
echo "Starting submission process..."

for EXP in "${EXPS[@]}"; do
    for DIM in "${DIMS[@]}"; do
        for BETA in "${BETAS[@]}"; do

            # Define a descriptive job name
            JOB_NAME="vae_${EXP}_${DIM}_${BETA}"

            # Define unique output/error log paths for the worker job
            # STDOUT_PATH="${PBS_LOG_BASE_DIR}/${JOB_NAME}.%j.stdout" # %j will be replaced by PBS job ID
            # STDERR_PATH="${PBS_LOG_BASE_DIR}/${JOB_NAME}.%j.stderr"

            echo "Submitting job for EXP=${EXP}, DIM=${DIM}, BETA=${BETA}"

            # Submit the worker script using qsub
            # -N: Job Name
            # -v: Pass variables to the job's environment
            qsub \
                -N "${JOB_NAME}" \
                -v "PASSED_EXP=${EXP},PASSED_DIM=${DIM},PASSED_BETA=${BETA}" \
                "${WORKER_SCRIPT_PATH}"

            # Optional: Add a small delay if you are submitting many jobs
            sleep 10

        done
    done
done

echo "All jobs submitted."