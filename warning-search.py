import os
import sys

def find_warning_logs(start_folder):

    found_files = []

    if not os.path.isdir(start_folder):
        print(f"Error: Starting folder '{start_folder}' not found or is not a directory.", file=sys.stderr)
        return found_files

    print(f"Searching for 'log.txt' files containing 'WARNING' in '{start_folder}' and its subdirectories...")

    for dirpath, dirnames, filenames in os.walk(start_folder):
        if 'log.txt' in filenames:
            log_file_path = os.path.join(dirpath, 'log.txt')
            try:
                with open(log_file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    if "WARNING" in content:
                        absolute_path = os.path.abspath(log_file_path)
                        found_files.append(absolute_path)

            except IOError as e:
                print(f"Warning: Could not read file '{log_file_path}'. Error: {e}", file=sys.stderr)
            except Exception as e:
                print(f"Warning: An unexpected error occurred processing file '{log_file_path}'. Error: {e}", file=sys.stderr)

    return found_files

start_directory = "/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv" 
warning_files = find_warning_logs(start_directory)

if warning_files:
    print("\n--- Found log.txt files containing 'WARNING' ---")
    for file_path in warning_files:
        print(file_path)
    print("--- End of list ---")
else:
    print(f"\nNo 'log.txt' files containing 'WARNING' were found in '{start_directory}'.")