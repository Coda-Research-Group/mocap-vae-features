import numpy as np
from pathlib import Path

def npz_to_data(npz_file: str, data_file: str):
    """
    Convert .npz mocap sequences to .data text format.
    
    Parameters:
        npz_file : str
            Path to the .npz file containing 'sample_ids' and 'subsequences'.
        data_file : str
            Output path for the .data file.
    """
    npz_file = Path(npz_file)
    data_file = Path(data_file)

    data = np.load(npz_file, allow_pickle=True)
    sample_ids = data['sample_ids']
    subsequences = data['subsequences']

    with open(data_file, 'w', encoding='utf-8') as f:
        for i, sid in enumerate(sample_ids):
            seq = subsequences[i]  # shape: T x J x 3
            f.write(f"#objectKey messif.objects.keys.AbstractObjectKey {sid}\n")
            f.write(f"{seq.shape[0]};mcdr.objects.ObjectMocapPose\n")
            for frame in seq:
                frame_str = ["{:.6f},{:.6f},{:.6f}".format(*frame[j]) for j in range(frame.shape[0])]
                f.write("; ".join(frame_str) + "\n")

    print(f".data file saved to {data_file}")

# Example usage
npz_to_data(
    "/home/drking/Documents/Bakalarka/data/data/pku-mmd/parts_norm/motion_torso_norm.npz",
    "/home/drking/Documents/Bakalarka/data/data/pku-mmd/parts_norm/motion_torso_norm.data"
)
