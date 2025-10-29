#!/usr/bin/env python3

import gzip
import sys

def extract_object_keys(filename):
    """Extract objectKey lines from a file (handles both regular and .gz files)."""
    keys = []
    try:
        # Check if file is gzipped
        if filename.endswith('.gz'):
            with gzip.open(filename, 'rt', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line.startswith('#objectKey'):
                        keys.append(line)
        else:
            with open(filename, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line.startswith('#objectKey'):
                        keys.append(line)
    except FileNotFoundError:
        print(f"Error: File '{filename}' not found.")
        return None
    except Exception as e:
        print(f"Error reading '{filename}': {e}")
        return None
    
    return keys

def compare_order(file1, file2):
    """Compare the order of objectKey lines in two files."""
    keys1 = extract_object_keys(file1)
    keys2 = extract_object_keys(file2)
    
    if keys1 is None or keys2 is None:
        return
    
    print(f"File 1: {file1}")
    print(f"  Found {len(keys1)} objectKey lines\n")
    
    print(f"File 2: {file2}")
    print(f"  Found {len(keys2)} objectKey lines\n")
    
    if keys1 == keys2:
        print("✓ The objectKey lines are in the SAME order in both files.")
    else:
        print("✗ The objectKey lines are NOT in the same order.\n")
        
        # Show differences
        max_len = max(len(keys1), len(keys2))
        print("Comparison (first difference shown):")
        for i in range(max_len):
            key1 = keys1[i] if i < len(keys1) else "(missing)"
            key2 = keys2[i] if i < len(keys2) else "(missing)"
            
            if key1 != key2:
                print(f"\nPosition {i}:")
                print(f"  File 1: {key1}")
                print(f"  File 2: {key2}")
                break

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script.py <file1> <file2>")
        print("  Supports both regular files and .gz compressed files")
        sys.exit(1)
    
    file1 = sys.argv[1]
    file2 = sys.argv[2]
    
    compare_order(file1, file2)
    