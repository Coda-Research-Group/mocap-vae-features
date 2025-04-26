import argparse
import numpy as np
import time
import sys
import os
import random

# Check for NumPy installation
try:
    import numpy as np
except ImportError:
    print("Error: NumPy library not found.")
    print("Please install it using: pip install numpy")
    sys.exit(1)

# Check for SciPy installation (optional but preferred for pdist)
try:
    from scipy.spatial.distance import pdist
    use_scipy = True
except ImportError:
    print("Warning: SciPy not found. Pairwise distances will be calculated using a slower NumPy loop.")
    print("         Consider installing SciPy: pip install scipy")
    use_scipy = False

def calculate_distances_numpy_loop(data_subset_np):
    """Calculates pairwise Euclidean distances using a NumPy loop (if SciPy is unavailable)."""
    n_subset = data_subset_np.shape[0]
    distances = []
    print(f"Calculating pairwise distances for subset ({n_subset} points) using NumPy loop...")
    start_time = time.time()
    for i in range(n_subset):
        for j in range(i + 1, n_subset):
            dist = np.sqrt(np.sum((data_subset_np[i] - data_subset_np[j])**2))
            distances.append(dist)
    end_time = time.time()
    print(f"NumPy loop distance calculation took {end_time - start_time:.2f} seconds.")
    return np.array(distances)

def load_and_sample_data(filepath, subset_size=None, required_dim=None, random_seed=None):
    """
    Loads numerical data line by line, checks dimensionality, and samples if needed.

    Args:
        filepath (str): Path to the input data file.
        subset_size (int, optional): The desired size of the random subset.
                                     If None or >= total data points, use all data.
        required_dim (int, optional): If specified, enforce this dimensionality.
        random_seed (int, optional): Seed for random sampling reproducibility.

    Returns:
        tuple: (np.ndarray or None, int or None)
               - NumPy array of the (sampled) data, or None if loading fails.
               - Detected dimensionality, or None if loading fails.
    """
    all_segments_list = []
    detected_dimensionality = None
    print(f"Attempting to load data from: '{filepath}'")
    if not os.path.exists(filepath):
        print(f"Error: File not found at '{filepath}'")
        return None, None

    line_count = 0
    data_lines_processed = 0
    skipped_lines = 0
    dimension_mismatch = False

    try:
        with open(filepath, 'r') as f:
            for line_num, line in enumerate(f):
                line_count += 1
                line = line.strip()

                if not line or line.startswith('#'):
                    skipped_lines += 1
                    continue

                try:
                    segment_vector = [float(x) for x in line.split(',')]
                    current_dimensionality = len(segment_vector)

                    if detected_dimensionality is None:
                        detected_dimensionality = current_dimensionality
                        print(f"Detected vector dimensionality: {detected_dimensionality}")
                        if required_dim is not None and detected_dimensionality != required_dim:
                            print(f"Error: Detected dimension {detected_dimensionality} does not match required dimension {required_dim}.")
                            dimension_mismatch = True
                            break
                    elif current_dimensionality != detected_dimensionality:
                        print(f"Error: Line {line_num + 1} has {current_dimensionality} dimensions, but expected {detected_dimensionality}.")
                        dimension_mismatch = True
                        break

                    all_segments_list.append(segment_vector)
                    data_lines_processed += 1

                except ValueError as ve:
                    print(f"Warning: Skipping line {line_num + 1} due to conversion error: {ve}")
                    skipped_lines += 1

        print(f"\nFile parsing summary:")
        print(f"- Total lines read: {line_count}")
        print(f"- Data lines processed: {data_lines_processed}")
        print(f"- Comment/empty/skipped lines: {skipped_lines}")

        if dimension_mismatch:
            print("\nError: Inconsistent or incorrect vector dimensions found. Cannot proceed.")
            return None, detected_dimensionality
        if not all_segments_list:
            print("\nError: No valid data vectors found in the file.")
            return None, detected_dimensionality

        # --- Subsampling Step ---
        n_total_points = len(all_segments_list)
        print(f"\nTotal valid data points loaded: {n_total_points}")

        final_data_list = all_segments_list
        using_subset = False

        if subset_size is not None and subset_size > 0 and subset_size < n_total_points:
            print(f"Sampling a subset of size {subset_size}...")
            using_subset = True
            if random_seed is not None:
                print(f"Using random seed: {random_seed}")
                random.seed(random_seed)
            # Sample *indices* first, then select from the list
            sampled_indices = random.sample(range(n_total_points), subset_size)
            final_data_list = [all_segments_list[i] for i in sampled_indices]
            print(f"Selected {len(final_data_list)} points for the subset.")
        elif subset_size is not None and subset_size >= n_total_points:
            print("Subset size requested is >= total points. Using all data.")
        elif subset_size is None or subset_size <= 0:
             print("No valid subset size requested. Using all data.")
        else:
             print("Using all data.")


        # --- Convert the final list (full or subset) into a 2D NumPy array ---
        try:
            data_np = np.array(final_data_list, dtype=np.float64)
            print(f"\nConverted data {'(subset)' if using_subset else '(full)'} into NumPy array.")
            print(f"Shape of final data array: {data_np.shape}")
            return data_np, detected_dimensionality
        except MemoryError:
             print("\nError: Not enough memory to convert even the selected data/subset into a NumPy array.", file=sys.stderr)
             return None, detected_dimensionality


    except Exception as e:
        print(f"\nAn unexpected error occurred while reading or processing the file: {e}")
        return None, None


def calculate_tsim(data_np, percentile_value=0.5):
    """
    Calculates the specified percentile of pairwise Euclidean distances.

    Args:
        data_np (np.ndarray): The data array (potentially a subset).
        percentile_value (float): The percentile to calculate (e.g., 0.5 for 0.5th).

    Returns:
        float or None: The calculated T_sim value, or None if an error occurs.
    """
    T_sim = None
    if data_np is None:
        print("\nNo data available for T_sim calculation.")
        return None

    if data_np.shape[0] < 2:
        print("\nError: Need at least two data points to calculate pairwise distances.")
        return None

    print(f"\nCalculating pairwise distances for {data_np.shape[0]} points...")
    try:
        if use_scipy:
            # Use SciPy's pdist (faster and more memory efficient for this part)
            pairwise_distances = pdist(data_np, metric='cosine')
        else:
            # Fallback to NumPy loop (slower)
            pairwise_distances = calculate_distances_numpy_loop(data_np)

        print(f"Successfully calculated {len(pairwise_distances)} pairwise distances.")

        # Calculate the specified percentile of these distances.
        print(f"Calculating the {percentile_value}th percentile...")
        T_sim = np.percentile(pairwise_distances, percentile_value)

        print(f"\n---> Similarity Threshold (T_sim) from {'subset' if data_np.shape[0] < 500000 else 'data'}: {T_sim:.6f}") # Adjust threshold for subset size if needed

        # Optional verification
        num_similar_pairs = np.sum(pairwise_distances <= T_sim)
        expected_num = len(pairwise_distances) * (percentile_value / 100.0)
        print(f"      Number of pairs with distance <= T_sim: {num_similar_pairs}")
        print(f"      Expected number based on percentile: ~{expected_num:.2f}")

    except MemoryError:
        print("\nError: Ran out of memory trying to calculate pairwise distances (even on the subset?).")
        print("Consider using a smaller subset size.")
        T_sim = None
    except Exception as e:
        print(f"\nAn unexpected error occurred during distance calculation or percentile: {e}")
        T_sim = None

    return T_sim


def main():
    parser = argparse.ArgumentParser(
        description="Calculate the T_sim threshold (percentile of pairwise distances) "
                    "from a large dataset by using a random subset.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument("input_file",
                        help="Path to the input data file (one vector per line, comma separated).")
    parser.add_argument("-s", "--subset_size", type=int, default=50000,
                        help="Number of data points to randomly sample for calculation. "
                             "If 0 or >= total points, use all data.")
    parser.add_argument("-p", "--percentile", type=float, default=0.5,
                        help="The percentile of pairwise distances to calculate (e.g., 0.5 for 0.5th).")
    parser.add_argument("--dim", type=int, default=None,
                        help="Optional: Enforce a specific vector dimensionality.")
    parser.add_argument("--seed", type=int, default=None,
                        help="Optional: Random seed for subset sampling reproducibility.")

    args = parser.parse_args()

    # 1. Load Data and Sample
    data_subset_np, dimensionality = load_and_sample_data(
        args.input_file,
        args.subset_size,
        args.dim,
        args.seed
    )

    # 2. Calculate T_sim on the subset
    T_sim = calculate_tsim(data_subset_np, args.percentile)

    # 3. Final Output
    print("\n" + "="*30)
    if T_sim is not None:
        print(f"Final Estimated T_sim ({args.percentile}th percentile): {T_sim:.6f}")
        if data_subset_np is not None and data_subset_np.shape[0] < 500000: # Check if subset was actually used
             print(f"(Calculated using a subset of size {data_subset_np.shape[0]})")
    else:
        print("Failed to calculate T_sim due to errors.")
    print("="*30)

if __name__ == "__main__":
    main()
