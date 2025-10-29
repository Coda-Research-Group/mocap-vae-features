import numpy as np
from pathlib import Path
import argparse
import re

def load_from_data_file(file_path: Path):
    """Load sequences from a .data text file."""
    sample_ids = []
    subsequences = []
    
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        
        # Look for object key
        if line.startswith('#objectKey'):
            # Extract the key (last part of the line)
            key = line.split()[-1]
            sample_ids.append(key)
            i += 1
            
            # Next line should be the frame count
            if i < len(lines):
                meta_line = lines[i].strip()
                frame_count = int(meta_line.split(';')[0])
                i += 1
                
                # Read frames
                frames = []
                for _ in range(frame_count):
                    if i < len(lines):
                        frame_line = lines[i].strip()
                        # Parse joints: each joint is "x,y,z" separated by ";"
                        joints_str = frame_line.split(';')
                        joints = []
                        for joint_str in joints_str:
                            if joint_str:
                                coords = [float(x) for x in joint_str.split(',')]
                                joints.append(coords)
                        frames.append(joints)
                        i += 1
                
                # Convert to numpy array: shape (num_frames, num_joints, 3)
                subsequences.append(np.array(frames))
        else:
            i += 1
    
    return np.array(sample_ids), subsequences

def save_to_data_file(file_path: Path, base_sample_ids, subsequences, part_name):
    """
    Save sequences to a .data text file, preserving the original sample IDs.
    """
    
    with open(file_path, 'w', encoding='utf-8') as f:
        for i, key in enumerate(base_sample_ids):
            seq = subsequences[i]
            
            # Write the original ID to the file
            f.write(f"#objectKey messif.objects.keys.AbstractObjectKey {key}\n")
            f.write(f"{seq.shape[0]};mcdr.objects.ObjectMocapPose\n")
            
            for frame in seq:
                # Ensure frame has the correct shape for processing
                if frame.ndim == 1:
                    # If frame is flat (e.g., (3,) for one joint), reshape it to (1, 3)
                    frame = np.expand_dims(frame, axis=0)
                    
                frame_lines = ["{:.6f},{:.6f},{:.6f}".format(*frame[j]) for j in range(frame.shape[0])]
                f.write(";".join(frame_lines) + "\n")
            
def save_to_npz_file(file_path: Path, sample_ids, subsequences):
    """
    Save sequences to an .npz file (compressed NumPy format).
    The IDs and sequences are stored as parallel arrays.
    
    Updated to use keys 'sample_ids' and 'subsequences' as required by the user's loader.
    """
    
    # Convert subsequences list (of arrays) to a NumPy object array for saving
    # The 'dtype=object' is necessary because the arrays might have different lengths.
    seq_array = np.array(subsequences, dtype=object)
    
    # Save the IDs and the sequences into a compressed .npz archive
    # Changed keys from 'ids' and 'sequences' to 'sample_ids' and 'subsequences'
    np.savez_compressed(
        file_path, 
        sample_ids=np.array(sample_ids), 
        subsequences=seq_array
    )


def get_body_parts_indices(body_model):
    """Get body part indices for the specified body model."""
    if body_model == 'hdm05':
        return {
            'legs_l': [1, 2, 3, 4, 5],
            'legs_r': [6, 7, 8, 9, 10],
            'torso': [11, 12, 13, 14, 15, 16],
            'hands_l': [17, 18, 19, 20, 21, 22, 23],
            'hands_r': [24, 25, 26, 27, 28, 29, 30]
        }
    elif body_model == 'pku-mmd':
        return {
            'legs_l': [12, 13, 14, 15],
            'legs_r': [16, 17, 18, 19],
            'torso': [0, 1, 20, 2, 3],
            'hands_l': [4, 5, 6, 7, 21, 22],
            'hands_r': [8, 9, 10, 11, 23, 24]
        }
    else:
        raise ValueError(f"Unrecognized body model: {body_model}. Supported models: 'hdm05', 'pku-mmd'")

def split_motion_data(input_file, output_dir, body_model):
    """
    Split motion data into body parts and save as .data and .npz files.
    
    Args:
        input_file: Path to input .data file
        output_dir: Directory where .data files will be saved
        body_model: Body model type ('hdm05' or 'pku-mmd')
    """
    # Convert to Path objects
    input_file = Path(input_file)
    output_dir = Path(output_dir)
    
    # Validate input file
    if not input_file.exists():
        raise FileNotFoundError(f"Input file not found: {input_file}")
    
    # Check if file has .data extension or .data-cv-* pattern
    valid_patterns = ['.data', '.data-cv-train', '.data-cs-train']
    is_valid = any(input_file.name.endswith(pattern) for pattern in valid_patterns)
    
    if not is_valid:
        raise ValueError(f"Input file must be a .data file (or .data-cv-train/test), got: {input_file.name}")
    
    # Create output directory
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Load data
    print(f"Loading data from {input_file.name}...")
    sample_ids, subsequences = load_from_data_file(input_file)
    num_sequences = len(sample_ids)
    print(f"Loaded {num_sequences} sequences with {subsequences[0].shape[1]} joints each")
    
    # Get body parts indices
    body_parts = get_body_parts_indices(body_model)
    
    # Split sequences by body part
    print(f"Splitting sequences using {body_model} body model...")
    data_parts = {part_name: [] for part_name in body_parts.keys()}
    
    for seq in subsequences:
        for part_name, indices in body_parts.items():
            data_parts[part_name].append(seq[:, indices, :])
    
    # Save each part as .data and .npz file
    print(f"Saving body parts to {output_dir}...")
    
    for part_name, part_data in data_parts.items():
        data_path = output_dir / f"motion_{part_name}.data"
        npz_path = output_dir / f"motion_{part_name}.npz"
        
        # 1. Save to .data file
        save_to_data_file(data_path, sample_ids, part_data, part_name)
        
        # 2. Save to .npz file
        save_to_npz_file(npz_path, sample_ids, part_data)
        
        num_joints = part_data[0].shape[1]
        
        # Print status
        print(f"  ✓ {data_path.name} ({num_joints} joints) [data].")
        print(f"  ✓ {npz_path.name} [npz].")
    
    print(f"\n✓ Successfully split {num_sequences} sequences into {len(body_parts)} body parts!")

def main():
    parser = argparse.ArgumentParser(
        description='Split motion capture data into body parts and save as .data and .npz files',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python splitting_data.py input.data output_dir/ pku-mmd
  python splitting_data.py data.data-cv-train parts/ hdm05
        """
    )
    parser.add_argument(
        'input_file',
        type=str,
        help='Path to input .data file'
    )
    parser.add_argument(
        'output_dir',
        type=str,
        help='Directory where .data files will be saved'
    )
    parser.add_argument(
        'body_model',
        type=str,
        choices=['hdm05', 'pku-mmd'],
        help='Body model type (hdm05 or pku-mmd)'
    )
    
    args = parser.parse_args()
    
    try:
        split_motion_data(args.input_file, args.output_dir, args.body_model)
    except Exception as e:
        print(f"Error: {e}")
        return 1
    
    return 0

if __name__ == '__main__':
    exit(main())
