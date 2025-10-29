import numpy as np
from pathlib import Path
import sys

# Define a tolerance for floating point comparison
TOLERANCE = 1e-6

def load_data_from_npz(file_path):
    """Loads subsequences from an NPZ file, handling the object array nature."""
    file_path = Path(file_path)
    if not file_path.exists():
        print(f"Error: File not found at {file_path}")
        return None
    try:
        # allow_pickle=True is necessary for loading object arrays (sequences of varying length)
        data = np.load(file_path, allow_pickle=True)
        if 'subsequences' not in data:
            print(f"Error: Key 'subsequences' not found in {file_path}")
            return None
        # Convert the object array back to a list of arrays for easier iteration
        return data['subsequences']
    except Exception as e:
        print(f"Error loading data from {file_path}: {e}")
        return None

def verify_rigid_translation(original_file_path, normalized_file_path, joint_index_a=1, joint_index_b=2):
    """
    Checks if the normalization process maintained rigid body constraints,
    meaning all joints were moved by the same vector.

    This is verified by checking if the distance between two non-pivot joints
    is preserved across the original and normalized sequences.

    Args:
        original_file_path (str or Path): Path to the .npz file BEFORE normalization.
        normalized_file_path (str or Path): Path to the .npz file AFTER normalization.
        joint_index_a (int): Index of the first non-pivot joint to compare (must be > 0).
        joint_index_b (int): Index of the second non-pivot joint to compare (must be > 0).

    Returns:
        bool: True if the transformation is rigid (distances are preserved), False otherwise.
    """
    print(f"\n--- Verifying Integrity of Normalization ---")
    print(f"Comparing distances between Joint {joint_index_a} and Joint {joint_index_b}.")

    # 1. Load Data
    orig_sequences = load_data_from_npz(original_file_path)
    norm_sequences = load_data_from_npz(normalized_file_path)

    if orig_sequences is None or norm_sequences is None:
        return False

    if len(orig_sequences) != len(norm_sequences):
        print(f"Error: Sequence counts mismatch. Original: {len(orig_sequences)}, Normalized: {len(norm_sequences)}")
        return False
    
    total_sequences = len(orig_sequences)
    mismatched_sequences = 0
    checked_frames = 0
    
    # 2. Iterate and Check
    for i in range(total_sequences):
        orig_seq = orig_sequences[i]
        norm_seq = norm_sequences[i]

        # Basic size check
        if orig_seq.shape != norm_seq.shape:
            # This is a critical failure if shapes change during normalization
            print(f"Failure in sequence {i}: Shapes mismatch (Original: {orig_seq.shape}, Normalized: {norm_seq.shape})")
            mismatched_sequences += 1
            continue

        # Check if the chosen joint indices are valid for this sequence
        if orig_seq.shape[1] <= max(joint_index_a, joint_index_b):
            continue # Skip sequences that don't have enough joints

        # Iterate through every frame in the sequence
        for frame_idx in range(orig_seq.shape[0]):
            checked_frames += 1
            
            # Extract coordinates for the frame
            orig_pose = orig_seq[frame_idx]
            norm_pose = norm_seq[frame_idx]

            # Calculate the L2-distance between joint A and joint B in the ORIGINAL pose
            # Distance = || Pose[A] - Pose[B] ||
            dist_orig = np.linalg.norm(orig_pose[joint_index_a] - orig_pose[joint_index_b])
            
            # Calculate the L2-distance between joint A and joint B in the NORMALIZED pose
            dist_norm = np.linalg.norm(norm_pose[joint_index_a] - norm_pose[joint_index_b])

            # Check if the distances are preserved (must be equal for a rigid translation)
            if not np.allclose(dist_orig, dist_norm, atol=TOLERANCE):
                print(f"❌ MISMATCH found in Sequence {i}, Frame {frame_idx}!")
                print(f"Original distance: {dist_orig:.6f}")
                print(f"Normalized distance: {dist_norm:.6f}")
                mismatched_sequences += 1
                # Once a mismatch is found, we can break and report failure for this sequence
                break 

    # 3. Report Results
    if checked_frames == 0:
        print("⚠️ Warning: No frames were checked (all sequences were too short or empty).")
        return False
        
    if mismatched_sequences == 0:
        print(f"✅ SUCCESS: Verified {total_sequences} sequences ({checked_frames} frames).")
        print("The normalization is a **rigid translation**. All relative joint distances were preserved.")
        return True
    else:
        print(f"❌ FAILURE: {mismatched_sequences} sequences showed non-rigid transformation.")
        print("This indicates that the joints were NOT all moved by the same pivot vector.")
        return False

# --- Example Usage ---

# Define the paths for the files you want to check
# NOTE: Replace these with your actual file paths for a real test.
ORIGINAL_NPZ = Path('/home/drking/Documents/Bakalarka/data/data/pku-mmd/parts/cv/motion_legs_l.npz')
NORMALIZED_NPZ = Path('/home/drking/Documents/Bakalarka/data/data/pku-mmd/normilized/cv/motion_legs_l_norm.npz')

# Run the verification function
# Joint 1 and Joint 2 are chosen arbitrarily, but they must exist and not be the pivot (index 0).
is_rigid = verify_rigid_translation(ORIGINAL_NPZ, NORMALIZED_NPZ, joint_index_a=1, joint_index_b=2)

print(f"\nFinal Result: Normalization Integrity {'PASSED' if is_rigid else 'FAILED'}")