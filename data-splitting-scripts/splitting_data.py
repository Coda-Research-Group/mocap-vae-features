import numpy as np
import argparse
import re
from pathlib import Path
from tqdm import tqdm


# --- UTILITY FUNCTIONS ---

def split_subsequences_from_data_file(data_path):
    """Parses a .data file into individual subsequence lines."""
    subsequence_lines = []
    with open(data_path, 'r') as lines:
        for line in lines:
            if line.lstrip().startswith("#objectKey"):
                if subsequence_lines:
                    yield subsequence_lines
                    subsequence_lines = []
            subsequence_lines.append(line.strip())

        if subsequence_lines:
            yield subsequence_lines

def parse_pose_line(line):
    """Parses a single line of pose data into a NumPy array."""
    # Modified to handle both space/comma/semicolon separation
    values = re.split(';|,| ', line)
    values = [float(v) for v in values if v]  # Filter out empty strings
    values = np.array(values, dtype=np.float32)
    values = values.reshape(-1, 3)
    return values

def convert_subsequence_to_numpy(subsequence_lines):
    """Converts a list of lines for one subsequence into a NumPy array."""
    header = subsequence_lines[:2]
    data = subsequence_lines[2:]

    sample_id = header[0].split(' ')[-1]

    # Parse and stack pose data
    data = map(parse_pose_line, data)
    data = np.stack(list(data))

    # Basic check using the frame count from the header
    try:
        expected_frames = int(header[1].split(';')[0])
        assert len(data) == expected_frames, f'Error parsing data: expected {expected_frames} frames, got {len(data)}'
    except (IndexError, ValueError):
        # Handle cases where header might be malformed or missing frame count
        pass

    return sample_id, data


def normalize_sequence_length(sequence, max_length):
    """Pads/crops a sequence to a fixed length (max_length)."""
    pad = max_length - len(sequence)
    if pad > 0:  # pad small sequences
        # Pad evenly on both sides, 'edge' mode repeats the first/last frame
        pad = ((pad // 2, -(-pad // 2)), (0, 0), (0, 0))
        sequence = np.pad(sequence, pad, mode='edge')
    
    sequence = sequence[:max_length]  # crop big sequences
    return sequence

def get_body_parts_indices(body_model):
    """Get body part indices for the specified body model."""
    if body_model == 'hdm05':
        # Indices are 1-based in the original source, converted to 0-based here
        return {
            'torso': [10, 11, 12, 13, 14, 15], # Corresponds to 11-16
            'handL': [16, 17, 18, 19, 20, 21, 22], # Corresponds to 17-23
            'handR': [23, 24, 25, 26, 27, 28, 29], # Corresponds to 24-30
            'legL': [0, 1, 2, 3, 4], # Corresponds to 1-5
            'legR': [5, 6, 7, 8, 9], # Corresponds to 6-10
        }
    elif body_model == 'pku-mmd':
        # Indices are 0-based
        return {
            'torso': [0, 1, 2, 3, 20],
            'handL': [4, 5, 6, 7, 21, 22],
            'handR': [8, 9, 10, 11, 23, 24],
            'legL': [12, 13, 14, 15],
            'legR': [16, 17, 18, 19],
        }
    else:
        raise ValueError(f"Unrecognized body model: {body_model}. Supported models: 'hdm05', 'pku-mmd'")


# --- I/O AND MAIN LOGIC ---

def load_from_data_file(file_path: Path):
    """Load sequences from a .data text file."""
    subsequences = split_subsequences_from_data_file(file_path)
    subsequences = map(convert_subsequence_to_numpy, tqdm(subsequences, desc=f"Parsing {file_path.name}"))
    sample_ids, subsequences = zip(*list(subsequences))
    
    # NOTE: subsequences is a list of arrays with potentially different lengths
    return np.array(sample_ids), list(subsequences)

def save_to_data_file(file_path: Path, base_sample_ids, subsequences):
    """Save sequences to a .data text file."""
    
    with open(file_path, 'w', encoding='utf-8') as f:
        for i, key in enumerate(base_sample_ids):
            seq = subsequences[i]
            
            f.write(f"#objectKey messif.objects.keys.AbstractObjectKey {key}\n")
            f.write(f"{seq.shape[0]};mcdr.objects.ObjectMocapPose\n")
            
            for frame in seq:
                # Format joints coordinates (x,y,z) with high precision, separated by semicolon
                frame_lines = [",".join([f"{c:.8f}" for c in joint_coords]) for joint_coords in frame]
                f.write(";".join(frame_lines) + "\n")


def save_to_npz_file(file_path: Path, sample_ids, subsequences):
    """
    Save sequences to an .npz file, applying padding/cropping to ensure 
    a homogeneous array, making it compatible with MoCapDataModule.
    """
    
    if not subsequences:
        seq_array = np.array([])
        max_length = 0
    else:
        # 1. Find the max length across all sequences in this body part
        max_length = max(len(s) for s in subsequences)
        print(f"    [NPZ] Fixed Subsequence Length for save: {max_length}")
        
        # 2. Apply padding/cropping to standardize length
        padded_subsequences = [normalize_sequence_length(s, max_length) for s in subsequences]
        
        # 3. Stack them into a single, homogeneous array (Shape N x T x J x 3)
        # THIS STEP ELIMINATES dtype=object, ensuring compatibility.
        seq_array = np.stack(padded_subsequences)
    
    # Save the IDs and the homogeneous sequences array
    np.savez_compressed(
        file_path, 
        sample_ids=np.array(sample_ids), 
        subsequences=seq_array
    )


def split_motion_data(input_file, output_dir, body_model):
    """Split motion data into body parts and save as .data and .npz files."""
    
    input_file = Path(input_file)
    output_dir = Path(output_dir)
    
    if not input_file.exists():
        raise FileNotFoundError(f"Input file not found: {input_file}")
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Load data (sequences are of variable length at this point)
    print(f"Loading data from {input_file.name}...")
    sample_ids, subsequences = load_from_data_file(input_file)
    num_sequences = len(sample_ids)
    print(f"Loaded {num_sequences} sequences.")
    
    # Get body parts indices
    body_parts = get_body_parts_indices(body_model)
    
    # Split sequences by body part
    print(f"Splitting sequences using {body_model} body model...")
    data_parts = {part_name: [] for part_name in body_parts.keys()}
    
    for seq in tqdm(subsequences, desc="Splitting sequences"):
        for part_name, indices in body_parts.items():
            # Slice the joints for the body part
            data_parts[part_name].append(seq[:, indices, :])
    
    # Save each part as .data and .npz file
    print(f"Saving body parts to {output_dir}...")
    
    for part_name, part_data in data_parts.items():
        data_path = output_dir / f"motion_{part_name}.data"
        npz_path = output_dir / f"motion_{part_name}.npz"
        
        print(f"\nProcessing {part_name}...")
        
        # 1. Save to .data file (maintains variable length)
        save_to_data_file(data_path, sample_ids, part_data)
        print(f"  ✓ {data_path.name} [data].")
        
        # 2. Save to .npz file (pads/stacks to fixed length)
        save_to_npz_file(npz_path, sample_ids, part_data)
        print(f"  ✓ {npz_path.name} [npz].")
        
        
    print(f"\n✓ Successfully split {num_sequences} sequences into {len(body_parts)} body parts!")

# --- ENTRY POINT ---

def main():
    parser = argparse.ArgumentParser(
        description='Split motion capture data into body parts and save as compatible .npz files',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python splitting_data.py input.data output_dir/ pku-mmd
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
        help='Directory where .data and .npz files will be saved'
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