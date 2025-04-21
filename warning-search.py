import os
import sys

def find_warning_logs(start_folder):
    """
    Recursively finds all 'log.txt' files within a starting folder
    that contain the string "WARNING" and returns their absolute paths.

    Args:
        start_folder (str): The absolute path to the folder to start searching from.

    Returns:
        list: A list of absolute paths to 'log.txt' files containing "WARNING".
              Returns an empty list if none are found or if the start_folder is invalid.
    """
    found_files = []

    # Ensure the starting folder exists and is a directory
    if not os.path.isdir(start_folder):
        print(f"Error: Starting folder '{start_folder}' not found or is not a directory.", file=sys.stderr)
        return found_files

    print(f"Searching for 'log.txt' files containing 'WARNING' in '{start_folder}' and its subdirectories...")

    # Walk through the directory tree
    for dirpath, dirnames, filenames in os.walk(start_folder):
        if 'log.txt' in filenames:
            log_file_path = os.path.join(dirpath, 'log.txt')
            try:
                # Open the file and check for "WARNING"
                # Use encoding='utf-8' and handle potential errors
                with open(log_file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    if "WARNING" in content:
                        # Get the absolute path and add it to the list
                        absolute_path = os.path.abspath(log_file_path)
                        found_files.append(absolute_path)
                        # Optional: print as soon as found
                        # print(f"  Found match: {absolute_path}") 

            except IOError as e:
                print(f"Warning: Could not read file '{log_file_path}'. Error: {e}", file=sys.stderr)
            except Exception as e:
                print(f"Warning: An unexpected error occurred processing file '{log_file_path}'. Error: {e}", file=sys.stderr)

    return found_files

# --- Script Execution ---

start_directory = "/storage/brno12-cerit/home/drking/experiments/SCL-segmented-actions/pku-mmd/cv" 


warning_files = find_warning_logs(start_directory)

if warning_files:
    print("\n--- Found log.txt files containing 'WARNING' ---")
    for file_path in warning_files:
        print(file_path)
    print("--- End of list ---")
else:
    print(f"\nNo 'log.txt' files containing 'WARNING' were found in '{start_directory}'.")