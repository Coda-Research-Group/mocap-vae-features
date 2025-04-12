import numpy as np
from pathlib import Path


def split_motion_data(input_file, body_model):

    PATH = PATH = Path(f'/home/drking/Documents/bakalarka/data/{body_model}/parts')

    data = np.load(input_file)
    sample_ids, subsequences = data['sample_ids'], data['subsequences']
    
    if body_model == 'hdm05':
        legs_l = [1, 2, 3, 4, 5]
        legs_r = [6, 7, 8, 9, 10]
        torso = [11, 12, 13, 14, 15, 16]
        hands_l = [17, 18, 19, 20, 21, 22, 23]
        hands_r = [24, 25, 26, 27, 28, 29, 30]
    
    if body_model == 'pku-mmd':
        legs_l = [12, 13, 14, 15]
        legs_r = [16, 17, 18, 19]
        torso = [0, 1, 20, 2, 3]
        hands_l = [4, 5, 6, 7, 21, 22]
        hands_r = [8, 9, 10, 11, 23, 24]
    
    else:
        print("Unrecognized body model.")
        exit

    data_legs_l  = []
    data_legs_r  = []
    data_torso   = []
    data_hands_l = []
    data_hands_r = []
    for seq in subsequences:
        data_legs_l.append(seq[:, legs_l, :])
        data_legs_r.append(seq[:, legs_r, :])
        data_torso.append(seq[:, torso, :])
        data_hands_l.append(seq[:, hands_l, :])
        data_hands_r.append(seq[:, hands_r, :])
        
    np.savez(PATH / "motion_legs_l.npz", sample_ids=sample_ids, subsequences=data_legs_l)
    np.savez(PATH / "motion_legs_r.npz", sample_ids=sample_ids, subsequences=data_legs_r)
    np.savez(PATH / "motion_torso.npz", sample_ids=sample_ids, subsequences=data_torso)
    np.savez(PATH / "motion_hands_l.npz", sample_ids=sample_ids, subsequences=data_hands_l)
    np.savez(PATH / "motion_hands_r.npz", sample_ids=sample_ids, subsequences=data_hands_r)
    
    print("Data splitted and saved successfully!")

original_PATH = '/home/drking/Documents/bakalarka/data/hdm05/class130-actions-segment80_shift16-coords_normPOS-fps12.npz'
split_motion_data(original_PATH, 'hdm05')

original_PATH = '/home/drking/Documents/bakalarka/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data'
split_motion_data(original_PATH, 'pku-mmd')