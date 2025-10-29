import numpy as np
from pathlib import Path
import sys

def normalize_part_data(input_file_path, output_file_path):
    """
    Loads motion data FOR A SINGLE BODY PART from an .npz file,
    normalizes each sequence so its first joint is at the origin (0,0,0)
    for all frames, and saves the result to a new .npz file.

    Args:
        input_file_path (str or Path): Path to the input .npz file for the part
                                      (e.g., motion_legs_l.npz).
                                      Expected keys: 'sample_ids', 'subsequences'.
                                      'subsequences' contains numpy arrays for only ONE part.
        output_file_path (str or Path): Path to save the normalized .npz file.
    """
    input_file_path = Path(input_file_path)
    output_file_path = Path(output_file_path)

    print(f"Normalizing NPZ file: {input_file_path}")

    # --- Load Data from NPZ file ---
    if not input_file_path.exists():
        print(f"Error: Input NPZ file not found at {input_file_path}")
        return
    try:
        # allow_pickle=True is often needed for npz files containing object arrays
        data = np.load(input_file_path, allow_pickle=True)
        # Verify required keys exist
        if 'sample_ids' not in data or 'subsequences' not in data:
             print(f"Error: Required keys 'sample_ids' or 'subsequences' not found in {input_file_path}")
             return
        sample_ids = data['sample_ids']
        # 'subsequences' should be a numpy object array or list of numpy arrays
        # where each element has shape (num_frames, num_joints_in_THIS_part, 3)
        subsequences = data['subsequences']
        print(f"Loaded {len(subsequences)} sequences for this part.")

    except Exception as e:
        print(f"Error loading data from {input_file_path}: {e}")
        return

    # --- Normalize Sequences (using NumPy operations) ---
    normalized_subsequences = []
    skipped_count = 0
    for i, seq in enumerate(subsequences):
        # Basic validation for each sequence (should be a numpy array)
        if not isinstance(seq, np.ndarray) or seq.ndim != 3 or seq.shape[-1] != 3:
            print(f"Warning: Skipping sequence index {i} due to unexpected format/shape: {getattr(seq, 'shape', 'N/A')}")
            skipped_count += 1
            continue
        # Check if there are any joints in this part's data for this sequence
        # seq.shape[1] is the number of joints in this part
        if seq.shape[1] == 0:
             print(f"Warning: Skipping sequence index {i} as it contains no joints.")
             skipped_count += 1
             continue

        # --- Core NumPy Normalization Logic ---
        # Get the first joint (index 0 of the second dimension) as reference
        # Slice as seq[:, 0:1, :] to keep the dimension for broadcasting.
        # Shape becomes: (num_frames, 1, 3)
        reference_joint_coords = seq[:, 0:1, :]

        # Subtract the reference joint coordinates from all joints in the sequence
        # NumPy broadcasts automatically: (num_frames, num_joints_in_part, 3) - (num_frames, 1, 3)
        normalized_seq = seq - reference_joint_coords
        # The first joint becomes [0,0,0] because original_joint_0 - original_joint_0 = 0
        # ------------------------------------

        normalized_subsequences.append(normalized_seq)

    if skipped_count > 0:
         print(f"Warning: Skipped {skipped_count} sequences due to format/shape issues.")

    if not normalized_subsequences:
         print("Error: No valid sequences were processed. Nothing to save.")
         return

    # --- Save Normalized Data to a new NPZ file ---
    # Note: This simple version assumes you want all original sample_ids saved.
    # If skipping sequences means you need to filter sample_ids, add that logic here.
    try:
        # Convert the list of arrays into a NumPy object array
        normalized_sequences_object_array = np.array(normalized_subsequences, dtype=object)

        # Save using np.savez
        np.savez(output_file_path,
                            sample_ids=sample_ids,
                            subsequences=normalized_sequences_object_array)
        print(f"Saved normalized data ({len(normalized_subsequences)} sequences) to {output_file_path}")
    except Exception as e:
        print(f"Error saving normalized data to {output_file_path}: {e}")

# --- How to Use ---

# 1. Specify the body model you are working with (used for path construction)
body_model = 'cs'  # Or 'pku-mmd'

# 2. Define the path where your SPLIT part files ARE LOCATED
#    (These are the .npz outputs of your first script)
input_base_path = Path(f'/home/drking/Documents/Bakalarka/data/data/pku-mmd/parts/{body_model}/')

# 3. Define where you want to SAVE the NORMALIZED part files
output_base_path = Path(f'/home/drking/Documents/Bakalarka/data/data/pku-mmd/normilized/{body_model}/')
output_base_path.mkdir(parents=True, exist_ok=True) # Create the output directory

# 4. List the base names of the part files you want to normalize
parts_to_normalize = ['legs_l', 'legs_r', 'torso', 'hands_l', 'hands_r']

# 5. Loop through the parts and run the normalization function
print(f"--- Starting Normalization for {body_model} NPZ files ---")
for part_name in parts_to_normalize:
    # Construct input and output paths for each part's NPZ file
    input_npz = input_base_path / f"motion_{part_name}.npz"
    output_npz = output_base_path / f"motion_{part_name}_norm.npz" # Saved in parts_norm dir

    # Call the function to normalize this specific part NPZ file
    normalize_part_data(input_npz, output_npz)
    print("-" * 20) # Separator

print(f"--- Normalization complete for {body_model} ---")

# === Optional: Run for the other model ===
# print(f"\n--- Starting Normalization for pku-mmd NPZ files ---")
# body_model_2 = 'pku-mmd'
# input_base_path_2 = Path(f'/home/drking/Documents/bakalarka/data/{body_model_2}/parts')
# output_base_path_2 = Path(f'/home/drking/Documents/bakalarka/data/{body_model_2}/parts_norm')
# output_base_path_2.mkdir(parents=True, exist_ok=True)
# for part_name in parts_to_normalize:
#     input_npz_2 = input_base_path_2 / f"motion_{part_name}.npz"
#     output_npz_2 = output_base_path_2 / f"motion_{part_name}_norm.npz"
#     normalize_part_data(input_npz_2, output_npz_2)
#     print("-" * 20)
# print(f"--- Normalization complete for {body_model_2} ---")