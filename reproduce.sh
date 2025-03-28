#!/bin/bash

set -e

LATENT_DIMS="256,128,64,32,16,8"
BETAS="0.1,10"
BODY_MODELS="hdm05"

# HDM05 experiments
python train.py --multirun exp=hdm05/fold1 latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS}
python train.py --multirun exp=hdm05/fold2 latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS}
python train.py --multirun exp=hdm05/all   latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS}

LATENT_DIMS="64,32,16,8"
BETAS="0.1,1,10"
BODY_MODELS="hdm05-torso,hdm05-handL,hdm05-handR,hdm05-legL,hdm05-legR"

# HDM05 experiments
python train.py --multirun exp=hdm05/fold1 latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS}
python train.py --multirun exp=hdm05/fold2 latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS}
python train.py --multirun exp=hdm05/all   latent_dim=${LATENT_DIMS} beta=${BETAS} body_model=${BODY_MODELS}



# # PKU-MMD experiments
# python train.py --multirun exp=pku-mmd/cs  latent_dim=${LATENT_DIMS} beta=${BETAS}
# python train.py --multirun exp=pku-mmd/cv  latent_dim=${LATENT_DIMS} beta=${BETAS}
# python train.py --multirun exp=pku-mmd/all latent_dim=${LATENT_DIMS} beta=${BETAS}