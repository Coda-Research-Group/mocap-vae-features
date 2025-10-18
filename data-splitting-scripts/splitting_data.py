import numpy as np
from pathlib import Path

def save_to_data_file(file_path: Path, sample_ids, subsequences):
    """Save sequences to a .data text file compatible with your DTW matcher."""
    with open(file_path, 'w', encoding='utf-8') as f:
        for i, key in enumerate(sample_ids):
            seq = subsequences[i]
            f.write(f"#objectKey messif.objects.keys.AbstractObjectKey {key}\n")
            f.write(f"{seq.shape[0]};mcdr.objects.ObjectMocapPose\n")
            for frame in seq:
                frame_lines = ["{:.6f},{:.6f},{:.6f}".format(*frame[j]) for j in range(frame.shape[0])]
                f.write("; ".join(frame_lines) + "\n")

def split_motion_data(input_file, body_model):
    PATH = Path(f'/home/drking/Documents/bakalarka/data/{body_model}/parts')
    PATH.mkdir(parents=True, exist_ok=True)

    data = np.load(input_file, allow_pickle=True)
    sample_ids, subsequences = data['sample_ids'], data['subsequences']

    # Define body parts indices
    if body_model == 'hdm05':
        legs_l = [1, 2, 3, 4, 5]
        legs_r = [6, 7, 8, 9, 10]
        torso = [11, 12, 13, 14, 15, 16]
        hands_l = [17, 18, 19, 20, 21, 22, 23]
        hands_r = [24, 25, 26, 27, 28, 29, 30]
    elif body_model == 'pku-mmd':
        legs_l = [12, 13, 14, 15]
        legs_r = [16, 17, 18, 19]
        torso = [0, 1, 20, 2, 3]
        hands_l = [4, 5, 6, 7, 21, 22]
        hands_r = [8, 9, 10, 11, 23, 24]
    else:
        raise ValueError("Unrecognized body model.")

    # Split sequences
    data_parts = {
        "legs_l": [],
        "legs_r": [],
        "torso": [],
        "hands_l": [],
        "hands_r": []
    }
    for seq in subsequences:
        data_parts["legs_l"].append(seq[:, legs_l, :])
        data_parts["legs_r"].append(seq[:, legs_r, :])
        data_parts["torso"].append(seq[:, torso, :])
        data_parts["hands_l"].append(seq[:, hands_l, :])
        data_parts["hands_r"].append(seq[:, hands_r, :])

    # Save each part as both .npz and .data
    for part_name, part_data in data_parts.items():
        npz_path = PATH / f"motion_{part_name}.npz"
        data_path = PATH / f"motion_{part_name}.data"
        np.savez(npz_path, sample_ids=sample_ids, subsequences=part_data)
        save_to_data_file(data_path, sample_ids, part_data)

    print("Data split and saved successfully as .npz and .data!")


# Example usage
# original_PATH_hdm05 = '/home/drking/Documents/bakalarka/data/hdm05/class130-actions-segment80_shift16-coords_normPOS-fps12.npz'
# split_motion_data(original_PATH_hdm05, 'hdm05')

original_PATH_pku = '/home/drking/Documents/Bakalarka/data/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.npz'
split_motion_data(original_PATH_pku, 'pku-mmd')
