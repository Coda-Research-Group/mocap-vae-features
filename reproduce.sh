#!/bin/bash

set -e

#Dimenze stredu autoencoderu
LATENT_DIMS="32"


BETAS="1"

# HDM05 experiments
python train.py --multirun exp=hdm05/fold1 latent_dim=${LATENT_DIMS} beta=${BETAS}
# python3 train.py --multirun exp=hdm05/fold2 latent_dim=${LATENT_DIMS} beta=${BETAS}
# python3 train.py --multirun exp=hdm05/all   latent_dim=${LATENT_DIMS} beta=${BETAS}

# PKU-MMD experiments
# python train.py --multirun exp=pku-mmd/cs  latent_dim=${LATENT_DIMS} beta=${BETAS}
# python train.py --multirun exp=pku-mmd/cv  latent_dim=${LATENT_DIMS} beta=${BETAS}
# python train.py --multirun exp=pku-mmd/all latent_dim=${LATENT_DIMS} beta=${BETAS}