#!/bin/bash

# TODO: replace LOGIN with your login

SUPPORTED_SCRIPTS=('single.sh')
SCRIPT_DIR='/storage/brno12-cerit/home/drking/experiments/mocap-vae-features'
PBS_LOGS_DIR='/storage/brno12-cerit/home/drking/experiments/pbs'

# Check that there is only one argument
#if [[ "$#" -ne 1 ]]; then
   # echo >&2 "Illegal number of parameters"
  #  echo >&2 "Usage: $0 single.sh"
 #   exit 1
#fi

SCRIPT="single.sh"                # e.g. search.sh
SCRIPT_NAME="${SCRIPT%.*}" # e.g. search

# Check that the argument is a valid script name
# shellcheck disable=SC2076
if [[ ! " ${SUPPORTED_SCRIPTS[*]} " =~ " ${SCRIPT} " ]]; then
    echo >&2 "Illegal script name"
    exit 2
fi

# Retrieve commit hash
cd "${REPO_DIR}" || {
    echo >&2 "Repository directory ${REPO_DIR} does not exist!"
    exit 3
}

COMMIT=$(git rev-parse --short HEAD)

# Stdout and stderr from PBS after the process finishes are placed into $PBS_LOGS_DIR
cd "${PBS_LOGS_DIR}" || {
    echo >&2 "PBS log directory ${PBS_LOGS_DIR} does not exist!"
    exit 4
}

for EXP in "fold1" "fold2" "all"; do
    qsub -N "experiment_${EXP}" -v EXPERIMENT=${EXP} "${SCRIPT_DIR}/single.sh"
done
