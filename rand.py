import itertools
import sys
from pathlib import Path

def count_all_differences(file1_path, file2_path):
    """
    Compares two files line by line and returns the total count of differences.

    Args:
        file1_path (str): Path to the first file.
        file2_path (str): Path to the second file.

    Returns:
        int: The total number of differing lines (or -1 if a file is not found).
    """
    try:
        f1 = open(file1_path, 'r')
        f2 = open(file2_path, 'r')
    except FileNotFoundError as e:
        print(f"🛑 Error: {e}")
        return -1

    print(f"Counting differences between '{Path(file1_path).name}' and '{Path(file2_path).name}'...")
    
    diff_count = 0
    
    # Use zip_longest to compare files even if they have different lengths.
    # fillvalue=None treats lines present in one file but not the other as a difference.
    for line1, line2 in itertools.zip_longest(f1, f2, fillvalue=None):
        
        # Strip whitespace for robust comparison, handling None if EOF is reached
        line1_stripped = line1.strip() if line1 is not None else None
        line2_stripped = line2.strip() if line2 is not None else None
        
        # Increment the count if the stripped contents are not equal
        if line1_stripped != line2_stripped:
            diff_count += 1

    f1.close()
    f2.close()
    
    return diff_count

# ----------------------------------------------------------------------
# Usage Example
# ----------------------------------------------------------------------
if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python script_name.py <file1.data> <file2.data>")
        sys.exit(1)

    file_a = sys.argv[1]
    file_b = sys.argv[2]
    
    total_diffs = count_all_differences(file_a, file_b)
    
    if total_diffs != -1:
        print("\n--- ✅ Final Result ---")
        if total_diffs == 0:
            print("The files are **absolutely identical** (0 differences found).")
        else:
            print(f"Total number of differences found: **{total_diffs}**.")