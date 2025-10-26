#!/usr/bin/env python3
import os
import gzip
import argparse

def filter_data_by_id_prefix(ids_filepath, data_filepath, output_filepath):
    valid_prefixes = set()
    try:
        with open(ids_filepath, 'r') as f:
            for line in f:
                prefix = line.strip()
                if prefix:
                    valid_prefixes.add(prefix)
    except IOError as e:
        print(f"Error reading IDs file '{ids_filepath}': {e}")
        return

    if not valid_prefixes:
        print("No valid prefixes found in the IDs file. Exiting.")
        return

    is_gzip = data_filepath.endswith(".gz")
    opener = gzip.open if is_gzip else open
    read_mode = 'rt' if is_gzip else 'r'

    filtered_data_count = 0
    try:
        with opener(data_filepath, read_mode) as infile, open(output_filepath, 'w') as outfile:
            current_block = []
            is_valid_block = False

            for line in infile:
                if line.startswith('#objectKey'):
                    if is_valid_block and current_block:
                        outfile.writelines(current_block)
                        filtered_data_count += 1

                    current_block = [line]
                    is_valid_block = False

                    parts = line.strip().split()
                    if len(parts) >= 3:
                        full_data_id = parts[2]
                        last_underscore_index = full_data_id.rfind('_')
                        data_prefix = full_data_id[:last_underscore_index] if last_underscore_index != -1 else full_data_id
                        if data_prefix in valid_prefixes:
                            is_valid_block = True
                elif current_block:
                    current_block.append(line)

            if is_valid_block and current_block:
                outfile.writelines(current_block)
                filtered_data_count += 1

    except IOError as e:
        print(f"Error processing data file: {e}")
        return

    print(f"\nFiltering complete.")
    print(f"Data file processed: {data_filepath}")
    print(f"Filtered blocks saved to: {output_filepath}")
    print(f"Total blocks filtered and saved: {filtered_data_count}")

def main():
    parser = argparse.ArgumentParser(description="Filter data blocks by matching ID prefixes.")
    parser.add_argument("ids_file", help="Path to file containing valid ID prefixes")
    parser.add_argument("data_file", help="Path to input data file (.data or .data.gz)")
    parser.add_argument("output_file", help="Path to save filtered output")
    args = parser.parse_args()

    filter_data_by_id_prefix(args.ids_file, args.data_file, args.output_file)

if __name__ == "__main__":
    main()
