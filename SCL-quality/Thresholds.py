import numpy as np
from scipy.spatial.distance import pdist, squareform

def parse_data(file_path):
    """
    Parses the data from the specified file path into a NumPy array.
    
    Args:
        file_path (str): The path to the data file.
    
    Returns:
        np.ndarray: A NumPy array where each row is an object's vector.
    """
    vectors = []
    with open(file_path, 'r') as f:
        for line in f:
            # Skip lines that are just object keys
            if line.startswith('#objectKey'):
                continue
            
            # Split the comma-separated values and convert to float
            vector = [float(x) for x in line.strip().split(',')]
            vectors.append(vector)
            
    return np.array(vectors)

def calculate_cosine_distance_matrix(data_vectors):
    """
    Calculates the pairwise cosine distance matrix for the given vectors.
    
    Args:
        data_vectors (np.ndarray): A NumPy array of data vectors.
    
    Returns:
        np.ndarray: A square, symmetric matrix of pairwise cosine distances.
    """
    # Use pdist to calculate pairwise distances in a condensed format
    # The metric 'cosine' is what you specified
    pairwise_distances = pdist(data_vectors, metric='cosine')
    
    # Use squareform to convert the condensed matrix to a square one
    distance_matrix = squareform(pairwise_distances)
    
    return distance_matrix


def evaluate_unsupervised_with_pseudo_gt(distance_matrix, sim_percentile=0.5, dissim_percentile=40):
    """
    Calculates precision, recall, and the confusion matrix components
    using a pseudo-ground truth based on distance percentiles.
    
    Args:
        distance_matrix (np.ndarray): The pairwise distance matrix.
        sim_percentile (float): The percentile for the similarity threshold (T_sim).
        dissim_percentile (float): The percentile for the dissimilarity threshold (T_dissim).
    
    Returns:
        dict: A dictionary containing tp, fp, fn, tn, precision, and recall.
    """
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

            # Define the model's "prediction" (a simple threshold-based classification)
            # A common approach is to use T_sim as the classification threshold
            # as it represents the "boundary" of similar items.
            is_predicted_matching = (distance <= t_sim)
            
            # Update the counts based on the GT and prediction
            if is_gt_similar:
                if is_predicted_matching:
                    tp += 1 # True Positive: Truly similar, predicted matching
                else:
                    fn += 1 # False Negative: Truly similar, predicted non-matching
            elif is_gt_dissimilar:
                if is_predicted_matching:
                    fp += 1 # False Positive: Truly dissimilar, predicted matching
                else:
                    tn += 1 # True Negative: Truly dissimilar, predicted non-matching

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

# Load your data
data_vectors = parse_data('/home/drking/Documents/Bakalarka/data/_SCL-segmented-actions-all/hdm05/all/lat_dim=256_beta=0.1/predictions_segmented_model=hdm05.data')

# Calculate the distance matrix
distance_matrix = calculate_cosine_distance_matrix(data_vectors)

print("Data Vectors Shape:", data_vectors.shape)
print("\nCosine Distance Matrix:")
print(distance_matrix)

# Example usage with the dummy data from the first code block
evaluation_results = evaluate_unsupervised_with_pseudo_gt(distance_matrix, sim_percentile=0.5, dissim_percentile=40)

print("\nEvaluation Results:")
print(f"Similarity Threshold (T_sim): {evaluation_results['T_sim']:.4f}")
print(f"Dissimilarity Threshold (T_dissim): {evaluation_results['T_dissim']:.4f}")
print(f"True Positives (TP): {evaluation_results['tp']}")
print(f"False Positives (FP): {evaluation_results['fp']}")
print(f"False Negatives (FN): {evaluation_results['fn']}")
print(f"True Negatives (TN): {evaluation_results['tn']}")
print(f"Precision: {evaluation_results['precision']:.4f}")
print(f"Recall: {evaluation_results['recall']:.4f}")