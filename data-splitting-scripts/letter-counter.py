import os

def filter_data_by_header_and_length(input_filepath, output_filepath, 
                                     metadata_line="8;mcdr.objects.ObjectMocapPose", 
                                     target_pose_count=8):
    """
    Reads the input file and writes only those object blocks to the output file 
    that contain the specific metadata header AND have exactly 8 pose lines.

    Args:
        input_filepath (str): Path to the large data file (.data).
        output_filepath (str): Path to save the filtered data.
        metadata_line (str): The specific metadata line that must be present.
        target_pose_count (int): The required number of poses (8).
    """
    if not os.path.exists(input_filepath):
        print(f"Error: Input file not found at path: {input_filepath}")
        return

    filtered_blocks_count = 0
    
    # State tracking variables for the current object block
    current_block = []
    current_pose_count = 0
    has_metadata_line = False

    try:
        with open(input_filepath, 'r') as infile, open(output_filepath, 'w') as outfile:
            for line_number, line in enumerate(infile, 1):
                cleaned_line = line.strip()

                # Always collect the line for the current block until a new block starts
                current_block.append(line)

                # 1. Start of a new object block
                if cleaned_line.startswith("#objectKey"):
                    
                    # --- Process the PREVIOUS object block ---
                    # Check if the previous block was valid and should be written
                    if current_block:
                        # The check must be against the state *before* processing the current line
                        # We use the length of the list, minus the current line we just appended,
                        # to evaluate the state of the object that just finished.
                        
                        # CRITICAL: We only evaluate the object if it wasn't the very first line of the file.
                        if line_number > 1:
                            # 1. Check if the metadata was present
                            # 2. Check if the pose count was exactly 8
                            if has_metadata_line and current_pose_count == target_pose_count:
                                # Write the block *excluding* the current objectKey line (which starts the new block)
                                outfile.writelines(current_block[:-1])
                                filtered_blocks_count += 1
                        
                    # --- Reset for the NEW object block ---
                    current_block = [line] # Start the new block with the objectKey line
                    has_metadata_line = False
                    current_pose_count = 0
                    
                # 2. Check for the specific metadata line
                elif cleaned_line == metadata_line:
                    has_metadata_line = True
                
                # 3. Count the pose/frame lines
                elif not cleaned_line.startswith('#') and cleaned_line:
                    # If it's not a header line and not a blank line, assume it's a pose line.
                    current_pose_count += 1
            
            # 4. Final check for the LAST object block in the file
            # The current_block list holds the last block, including its objectKey line
            if current_block:
                if has_metadata_line and current_pose_count == target_pose_count:
                    outfile.writelines(current_block)
                    filtered_blocks_count += 1
                
    except IOError as e:
        print(f"An I/O error occurred: {e}")
        return

    print(f"\nFiltering complete.")
    print(f"Input file processed: {input_filepath}")
    print(f"Output file saved to: {output_filepath}")
    print(f"Total blocks filtered and saved: {filtered_blocks_count} (Must meet both criteria: header AND {target_pose_count} poses).")


# ----------------------------------------------------------------
# --- Configuration ---
# ⚠️ CHANGE THESE TO YOUR ACTUAL FILE PATHS ⚠️
INPUT_FILE = '/home/drking/Documents/Bakalarka/data/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cs-train'
OUTPUT_FILE = '/home/drking/Documents/Bakalarka/data/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data-cs-train2'
# ----------------------------------------------------------------

# --- Run the script ---
filter_data_by_header_and_length(INPUT_FILE, OUTPUT_FILE)