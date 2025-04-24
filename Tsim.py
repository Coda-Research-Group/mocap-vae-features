import numpy as np
from scipy.spatial.distance import pdist # Efficiently computes pairwise distances
import os # To check if the file exists

# --- 1. Configuration: SET THIS VALUE ---
# Replace with the actual path to your text data file
text_file_path = '/home/drking/Documents/bakalarka/data/pku-test/lat_dim=256_beta=1/predictions_segmented_model=pku-mmd.data-cv-train' # <--- CHANGE THIS FILE PATH

# --- 2. Load Data from Text File ---
all_segments_list = [] # Use a standard Python list first to collect vectors
flattened_segments = None # Initialize variable for the final NumPy array
expected_dimensionality = None # To store the dimensionality of the first vector found

print(f"Attempting to load data from: '{text_file_path}'")
if not os.path.exists(text_file_path):
    print(f"Error: File not found at '{text_file_path}'")
    print("Please check the file path.")
else:
    try:
        line_count = 0
        data_lines_processed = 0
        skipped_lines = 0
        dimension_mismatch = False

        with open(text_file_path, 'r') as f:
            for line_num, line in enumerate(f):
                line_count += 1
                line = line.strip() # Remove leading/trailing whitespace

                # Skip empty lines and lines starting with '#' (comments)
                if not line or line.startswith('#'):
                    skipped_lines += 1
                    continue

                # Assume this is a data line: comma-separated floats
                try:
                    # Split by comma and convert each part to float
                    segment_vector = [float(x) for x in line.split(',')]
                    current_dimensionality = len(segment_vector)

                    # Check dimensionality consistency
                    if expected_dimensionality is None:
                        # This is the first data vector found, store its dimension
                        expected_dimensionality = current_dimensionality
                        print(f"Detected vector dimensionality from first data line: {expected_dimensionality}")
                    elif current_dimensionality != expected_dimensionality:
                        print(f"Error: Line {line_num + 1} has {current_dimensionality} dimensions, but expected {expected_dimensionality}.")
                        dimension_mismatch = True
                        break # Stop processing if dimensions don't match

                    all_segments_list.append(segment_vector)
                    data_lines_processed += 1

                except ValueError as ve:
                    print(f"Warning: Skipping line {line_num + 1} due to conversion error (not comma-separated numbers?): {ve}")
                    print(f"   Content snippet: '{line[:80]}...'") # Print start of problematic line
                    skipped_lines += 1

        print(f"\nFile parsing summary:")
        print(f"- Total lines read: {line_count}")
        print(f"- Data lines processed: {data_lines_processed}")
        print(f"- Comment/empty/skipped lines: {skipped_lines}")

        if dimension_mismatch:
            print("\nError: Inconsistent vector dimensions found. Cannot proceed.")
            flattened_segments = None
        elif not all_segments_list:
            print("\nError: No valid data vectors found in the file.")
            flattened_segments = None
        else:
            # --- Convert the list of vectors into a 2D NumPy array ---
            flattened_segments = np.array(all_segments_list)
            print(f"\nSuccessfully loaded and converted data into NumPy array.")
            print(f"Shape of flattened segments array: {flattened_segments.shape}")
            # Check if the shape matches expectations (e.g., 256 dimensions)
            if flattened_segments.shape[1] != 256:
                print(f"Warning: Detected dimensionality is {flattened_segments.shape[1]}, not 256 as potentially expected.")


    except Exception as e:
        print(f"\nAn unexpected error occurred while reading or processing the file: {e}")
        flattened_segments = None

# --- 3. Calculate T_sim (only if data was loaded and prepared successfully) ---
T_sim = None # Initialize T_sim
if flattened_segments is not None:
    # Ensure you have at least two segments to compare
    if flattened_segments.shape[0] < 2:
        print("\nError: Need at least two segments to calculate pairwise distances.")
    else:
        print(f"\nCalculating pairwise distances for {flattened_segments.shape[0]} segments...")
        # 'pdist' computes the distance between all unique pairs of points (rows).
        # Default metric is 'euclidean'.
        try:
            pairwise_distances = pdist(flattened_segments, metric='euclidean')
            print(f"Successfully calculated {len(pairwise_distances)} pairwise distances.")

            # Calculate the 0.5th percentile of these distances.
            percentile_value = 0.5
            print(f"Calculating the {percentile_value}th percentile...")
            T_sim = np.percentile(pairwise_distances, percentile_value)

            print(f"\n---> Similarity Threshold (T_sim): {T_sim:.6f}")

            # Optional verification
            num_similar_pairs = np.sum(pairwise_distances <= T_sim)
            expected_num = len(pairwise_distances) * (percentile_value / 100.0)
            print(f"      Number of pairs with distance <= T_sim: {num_similar_pairs}")
            print(f"      Expected number based on percentile: ~{expected_num:.2f}")

        except MemoryError:
            print("\nError: Ran out of memory trying to calculate pairwise distances.")
            print("This can happen if you have a very large number of segments.")
            print("Consider using a subset of your data or exploring approximate methods.")
            T_sim = None
        except Exception as e:
            print(f"\nAn unexpected error occurred during distance calculation or percentile: {e}")
            T_sim = None
else:
    print("\nData loading or preparation failed. Cannot proceed with T_sim calculation.")


# --- 4. Final Output and Considerations ---
if T_sim is not None:
    print(f"\nThe calculated similarity threshold T_sim is: {T_sim:.6f}")
else:
    print("\nCould not calculate T_sim due to errors listed above.")

print("\nImportant Considerations:")
print("- Input Data Format: Assumes a text file where each line is either a comment (#...), empty, or contains comma-separated floating point numbers representing one segment vector.")
print("- Segment Length/Dimensionality: Critically assumes all data lines (vectors) have the *same number of comma-separated values* (same dimensionality). The script checks this.")
print("- Distance Metric: Euclidean distance was used. Change `metric` in `pdist` if needed.")
print("- Memory: Pairwise distance calculation can be memory-intensive for large numbers of segments.")