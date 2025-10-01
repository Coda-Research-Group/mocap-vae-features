import numpy as np
from scipy.spatial.distance import pdist, squareform
import random
import os

def evaluate_unsupervised_subset(file_path, subset_size, sim_percentile=0.5, dissim_percentile=40):
    """
    Parses a large dataset, takes a random subset, and evaluates unsupervised
    precision/recall using percentile-based pseudo-ground truth.

    Args:
        file_path (str): The path to the data file.
        subset_size (int): The number of objects to sample for evaluation.
        sim_percentile (float): The percentile for the similarity threshold (T_sim).
        dissim_percentile (float): The percentile for the dissimilarity threshold (T_dissim).

    Returns:
        dict: A dictionary containing tp, fp, fn, tn, precision, recall, and thresholds.
    """
    # 1. Parse Data and Create the Subset
    vectors = []
    
    # First pass: Get all object keys and their corresponding vectors
    with open(file_path, 'r') as f:
        all_vectors = []
        current_vector = None
        for line in f:
            if line.startswith('#objectKey'):
                if current_vector is not None:
                    all_vectors.append(current_vector)
                current_vector = None
            else:
                try:
                    current_vector = [float(x) for x in line.strip().split(',')]
                except ValueError:
                    # Handle cases where the line might be empty or malformed
                    continue
        if current_vector is not None:
            all_vectors.append(current_vector)
    
    if len(all_vectors) < subset_size:
        print("Warning: Dataset size is smaller than the requested subset size. Using the full dataset.")
        subset_vectors = all_vectors
    else:
        subset_vectors = random.sample(all_vectors, subset_size)
    
    data_vectors = np.array(subset_vectors)
    
    # 2. Calculate Cosine Distance Matrix for the Subset
    # Use pdist to calculate pairwise distances in a condensed format
    pairwise_distances = pdist(data_vectors, metric='cosine')
    
    # Use squareform to convert the condensed matrix to a square one
    distance_matrix = squareform(pairwise_distances)

    # 3. Define GT and Calculate TP, FP, FN, TN
    # Get a flattened, sorted list of all unique distances (excluding self-distances)
    upper_triangle = distance_matrix[np.triu_indices(distance_matrix.shape[0], k=1)]
    
    # Determine the similarity and dissimilarity thresholds based on percentiles
    t_sim = np.percentile(upper_triangle, sim_percentile)
    t_dissim = np.percentile(upper_triangle, dissim_percentile)
    
    # Initialize the counts for the confusion matrix
    tp, fp, fn, tn = 0, 0, 0, 0

    # Iterate through all unique pairs (upper triangle of the matrix)
    num_objects = distance_matrix.shape[0]
    for i in range(num_objects):
        for j in range(i + 1, num_objects):
            distance = distance_matrix[i, j]

            # Define the pseudo-ground truth (GT)
            is_gt_similar = (distance <= t_sim)
            is_gt_dissimilar = (distance >= t_dissim)

            # Ignore pairs in the grey zone
            if not is_gt_similar and not is_gt_dissimilar:
                continue

            # Define the model's "prediction"
            is_predicted_matching = (distance <= t_sim)
            
            # Update the counts based on the GT and prediction
            if is_gt_similar:
                if is_predicted_matching:
                    tp += 1
                else:
                    fn += 1
            elif is_gt_dissimilar:
                if is_predicted_matching:
                    fp += 1
                else:
                    tn += 1

    # Calculate precision and recall
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0
    
    return {
        'tp': tp,
        'fp': fp,
        'fn': fn,
        'tn': tn,
        'precision': precision,
        'recall': recall,
        'T_sim': t_sim,
        'T_dissim': t_dissim
    }

datapath = '/home/drking/Documents/Bakalarka/data/_SCL-segmented-actions-all/hdm05/all/lat_dim=256_beta=0.1/predictions_segmented_model=hdm05.data'

if not os.path.exists(datapath):
    print(f"ERROR: path {datapath} does not exists!")

results = evaluate_unsupervised_subset(
    file_path=datapath,
    subset_size=10000,
    sim_percentile=0.5,
    dissim_percentile=40
)

print("\nEvaluation Results on a Subset:")
print(f"Subset Size: 5000 objects")
print(f"Similarity Threshold (T_sim): {results['T_sim']:.4f}")
print(f"Dissimilarity Threshold (T_dissim): {results['T_dissim']:.4f}")
print(f"True Positives (TP): {results['tp']}")
print(f"False Positives (FP): {results['fp']}")
print(f"False Negatives (FN): {results['fn']}")
print(f"True Negatives (TN): {results['tn']}")
print(f"Precision: {results['precision']:.4f}")
print(f"Recall: {results['recall']:.4f}")