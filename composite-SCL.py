import os
import sys
import argparse
import gzip # Keep import in case needed later

def parse_object_key(header_line):
    """Extracts the object key ID from the first header line."""
    if not header_line or not header_line.startswith("#objectKey"):
        raise ValueError(f"Line is not a valid object key header: {header_line.strip()}")
    parts = header_line.strip().split()
    if len(parts) < 3:
        raise ValueError(f"Object key header line format error: {header_line.strip()}")
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


def concatenate_multivector_files(input_folder, output_file):
    """
    Concatenates multi-vector objects from specific files in a defined order.

    Reads corresponding objects (including all their vectors) from input files,
    validates keys and vector counts, and writes the concatenated object
    data (all vectors from file 1, then file 2, etc.) to the output file.
    """
    body_parts_order = ['legL', 'legR', 'torso', 'handL', 'handR']
    num_files = len(body_parts_order)
    base_filename_pattern = "predictions_model=hdm05-{}.data"
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
                    # Only read count header if key header was present
                    if current_object_key_headers[i]:
                        current_vec_count_headers[i] = infile.readline()
                    else:
                        # If key_header is empty, count_header should also be empty on clean EOF
                        current_vec_count_headers[i] = "" # Assign empty string explicitly

                # --- Check for End Of File ---
                # Check if the *first* file seems to have ended (empty key header)
                if not current_object_key_headers[0]:
                    # Now, verify if *all* files ended together
                    all_ended = all(not h for h in current_object_key_headers)
                    if all_ended:
                        print(f"Clean end of all files reached after {object_count} objects.")
                        break # Exit the main while loop successfully
                    else:
                        # Find which files ended and which didn't
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
                        # Check for partial reads (e.g., key header present but count header missing)
                        if not current_object_key_headers[i] or not current_vec_count_headers[i]:
                             raise ValueError(f"Incomplete header read for object {object_count+1} "
                                              f"in file {os.path.basename(input_filepaths[i])}")

                        object_keys[i] = parse_object_key(current_object_key_headers[i])
                        vector_counts[i], vector_types[i] = parse_vector_count_header(current_vec_count_headers[i])
                except ValueError as e:
                    # Add more context to the parsing error
                    raise ValueError(f"{e} (processing object {object_count+1})")

                # --- Validate Headers (Object Key and Vector Count) ---
                first_key = object_keys[0]
                if not all(key == first_key for key in object_keys):
                    raise ValueError(f"Error: Mismatched object keys for object {object_count+1}: {object_keys}. "
                                     "Files may be corrupted or out of sync.")
                first_count = vector_counts[0]
                if not all(count == first_count for count in vector_counts):
                     print(f"Warning: Mismatched vector counts for object ID {first_key} (object {object_count+1}): {vector_counts}.", file=sys.stderr)
                     # Consider if this should be a hard error:
                     # raise ValueError(f"Error: Mismatched vector counts for object ID {first_key} (object {object_count+1}): {vector_counts}.")

                num_vectors_per_object = first_count # Use count from first file

                # --- Read and store data vectors for the current object ---
                # (The rest of the loop: reading data, writing output - remains the same)
                # ... (rest of the while loop) ...
                object_data_lines = [[] for _ in range(num_files)]
                for vec_idx in range(num_vectors_per_object):
                    for file_idx, infile in enumerate(input_files):
                        data_line = infile.readline()
                        if not data_line:
                            raise EOFError(f"Error: Unexpected end of file in '{os.path.basename(input_filepaths[file_idx])}' "
                                           f"while reading vector {vec_idx+1}/{num_vectors_per_object} "
                                           f"for object ID {first_key} (object {object_count+1}).")
                        object_data_lines[file_idx].append(data_line.strip())

                # --- Write combined output for the object ---
                outfile.write(current_object_key_headers[0]) # Write key header from first file

                total_concatenated_vectors = num_vectors_per_object * num_files
                # Write vector count header (using total count and type from first file)
                outfile.write(f"{total_concatenated_vectors};{vector_types[0]}\n")

                # Write data lines file by file
                for file_idx in range(num_files):
                    for data_line in object_data_lines[file_idx]:
                        outfile.write(data_line + "\n")

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
    elif not eof_reached_on_any:
         print("Warning: No objects processed. Input files might be empty or headers incorrect.", file=sys.stderr)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Concatenate multi-vector objects from specific hdm05 data files.")
    parser.add_argument("input_folder",
                        help="Path to the folder containing the input .data files.")
    parser.add_argument("-o", "--output", default="combined_predictions_hdm05_multivector.data",
                        help="Name for the output concatenated file (default: combined_predictions_hdm05_multivector.data)")

    args = parser.parse_args()

    concatenate_multivector_files(args.input_folder, args.output)