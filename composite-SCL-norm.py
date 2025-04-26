import os
import sys
import argparse
import gzip # Keep import in case needed later

def parse_object_key(header_line):
    """Extracts the object key ID from the first header line."""
    if not header_line or not header_line.startswith("#objectKey"):
        raise ValueError(f"Line is not a valid object key header: {header_line.strip()}")
    parts = header_line.strip().split()
    # Corrected check: needs at least 3 parts (#objectKey, type, ID)
    if len(parts) < 3:
        raise ValueError(f"Object key header line format error (expected at least 3 parts): {header_line.strip()}")
    return parts[-1] # Assume the last part is the unique ID

def parse_vector_count_header(header_line):
    """Extracts the vector count and type from the second header line."""
    if not header_line or ';' not in header_line:
         raise ValueError(f"Line is not a valid vector count header: {header_line.strip()}")
    parts = header_line.strip().split(';', 1)
    try:
        count = int(parts[0])
        type_str = parts[1] if len(parts) > 1 else ""
        return count, type_str
    except (ValueError, IndexError) as e:
        raise ValueError(f"Vector count header line format error ('{header_line.strip()}'): {e}")

def concatenate_horizontally(input_folder, output_file):
    """
    Concatenates corresponding vectors horizontally from specific files.

    For each object, reads the Nth vector from all input files, joins them
    into a single wider vector, and writes it. The number of vectors per
    object remains the same, but their dimension increases by 5x.
    """
    body_parts_order = ['legL', 'legR', 'torso', 'handL', 'handR']
    num_files = len(body_parts_order)
    # Corrected pattern based on previous error
    base_filename_pattern = "predictions_model_norm=pku-mmd-{}.data"
    input_filenames = [base_filename_pattern.format(part) for part in body_parts_order]
    input_filepaths = [os.path.join(input_folder, fname) for fname in input_filenames]

    # --- Check if all input files exist ---
    missing_files = [fp for fp in input_filepaths if not os.path.isfile(fp)]
    if missing_files:
        print(f"Error: The following input files were not found in '{input_folder}':", file=sys.stderr)
        for mf in missing_files:
            print(f"- {os.path.basename(mf)}", file=sys.stderr)
        sys.exit(1)

    print("Input files to process:")
    for i, fp in enumerate(input_filepaths):
        print(f"  {i+1}. {os.path.basename(fp)}")
    print(f"Output file: {output_file}")

    input_files = [] # To store file handles for finally block
    try:
        # --- Open all input files and the output file simultaneously ---
        input_files = [open(fp, 'rt', encoding='utf-8') for fp in input_filepaths]
        with open(output_file, 'wt', encoding='utf-8') as outfile:

            object_count = 0
            while True:
                current_object_key_headers = [None] * num_files
                current_vec_count_headers = [None] * num_files

                # --- Attempt to read headers for the next object from all files ---
                for i, infile in enumerate(input_files):
                    current_object_key_headers[i] = infile.readline()
                    if current_object_key_headers[i]:
                        current_vec_count_headers[i] = infile.readline()
                    else:
                        current_vec_count_headers[i] = ""

                # --- Check for End Of File ---
                if not current_object_key_headers[0]:
                    all_ended = all(not h for h in current_object_key_headers)
                    if all_ended:
                        print(f"Clean end of all files reached after {object_count} objects.")
                        break
                    else:
                        ended_indices = [idx for idx, h in enumerate(current_object_key_headers) if not h]
                        active_indices = [idx for idx, h in enumerate(current_object_key_headers) if h]
                        ended_files = [os.path.basename(input_filepaths[i]) for i in ended_indices]
                        active_files = [os.path.basename(input_filepaths[i]) for i in active_indices]
                        raise EOFError(f"Error: File(s) {ended_files} ended prematurely "
                                       f"while file(s) {active_files} still have data "
                                       f"(processing object {object_count+1}). Files have different numbers of objects.")

                # --- If we are here, all files still had headers. Now parse and validate. ---
                object_keys = [None] * num_files
                vector_counts = [0] * num_files
                vector_types = [""] * num_files
                try:
                    for i in range(num_files):
                        if not current_object_key_headers[i] or not current_vec_count_headers[i]:
                             raise ValueError(f"Incomplete header read for object {object_count+1} "
                                              f"in file {os.path.basename(input_filepaths[i])}")
                        object_keys[i] = parse_object_key(current_object_key_headers[i])
                        vector_counts[i], vector_types[i] = parse_vector_count_header(current_vec_count_headers[i])
                except ValueError as e:
                    raise ValueError(f"{e} (processing object {object_count+1})")

                # --- Validate Headers (Object Key and Vector Count) ---
                first_key = object_keys[0]
                if not all(key == first_key for key in object_keys):
                    raise ValueError(f"Error: Mismatched object keys for object {object_count+1}: {object_keys}.")
                first_count = vector_counts[0]
                if not all(count == first_count for count in vector_counts):
                     # This check is now very important for horizontal concatenation
                     raise ValueError(f"Error: Mismatched vector counts for object ID {first_key} "
                                      f"(object {object_count+1}): {vector_counts}. Cannot combine horizontally.")

                num_vectors_per_object = first_count # The number of vectors the final object will have

                # --- Write Output Headers ---
                outfile.write(current_object_key_headers[0]) # Write key header from first file
                # Write vector count header - **using the ORIGINAL count**
                outfile.write(f"{num_vectors_per_object};{vector_types[0]}\n")

                # --- Process Vectors Horizontally ---
                for vec_idx in range(num_vectors_per_object):
                    vector_parts = [None] * num_files
                    # Read the vec_idx-th vector from each file
                    for file_idx, infile in enumerate(input_files):
                        data_line = infile.readline()
                        if not data_line:
                            raise EOFError(f"Error: Unexpected end of file in '{os.path.basename(input_filepaths[file_idx])}' "
                                           f"while reading vector {vec_idx+1}/{num_vectors_per_object} "
                                           f"for object ID {first_key} (object {object_count+1}). "
                                           f"Header count ({num_vectors_per_object}) might be wrong for this file.")
                        vector_parts[file_idx] = data_line.strip()

                    # Concatenate the parts horizontally
                    combined_vector_line = ",".join(vector_parts)
                    outfile.write(combined_vector_line + "\n")

                object_count += 1
                if object_count % 100 == 0: # Progress indicator
                     print(f"Processed {object_count} objects...")

    except (EOFError, ValueError, IOError) as e:
        print(f"\nAn error occurred: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        # --- Ensure all input files are closed ---
        for infile in input_files:
            if infile and not infile.closed:
                infile.close()
        print(f"Closed input files.")

    if object_count > 0 :
        print(f"Successfully processed {object_count} objects.")
        print(f"Created concatenated file: {output_file}")
    elif 'all_ended' in locals() and all_ended: # Check if loop finished due to clean EOF
         print("Input files seem empty or contain no valid objects.")
    elif not ('all_ended' in locals()): # Error likely occurred before first EOF check
        print("Processing stopped before finishing.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Horizontally concatenate corresponding vectors from specific hdm05 data files.")
    parser.add_argument("input_folder",
                        help="Path to the folder containing the input .data files.")
    parser.add_argument("-o", "--output", default="combined_hdm05_horizontal.data",
                        help="Name for the output concatenated file (default: combined_hdm05_horizontal.data)")

    args = parser.parse_args()

    concatenate_horizontally(args.input_folder, args.output) # Renamed function for clarity