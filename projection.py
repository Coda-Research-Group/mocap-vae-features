import numpy as np
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
import plotly.express as px
import pandas as pd
import sys

def load_data_from_file(filepath):
    """
    Loads high-dimensional data from a specified file path.
    The function handles a file format where lines starting with '#' are
    skipped and comma-separated floating-point values are parsed.

    Args:
        filepath (str): The path to the data file.

    Returns:
        tuple: A tuple containing the data array and generated labels.
    """
    data = []
    with open(filepath, 'r') as f:
        for line in f:
            # Skip comment/metadata lines starting with '#'
            if line.strip().startswith('#'):
                continue
            
            # Parse the data from the line
            try:
                features = [float(x) for x in line.strip().split(',')]
                data.append(features)
            except ValueError:
                # Handle lines that may not contain valid float data
                print(f"Skipping invalid line: {line.strip()}")

    if not data:
        print("No valid data found in the file.")
        return np.array([]), np.array([])
    
    # Since the file format does not include labels, we create a generic
    # label for each data point for plotting purposes.
    n_samples = len(data)
    labels = np.array([f'Data Point {i+1}' for i in range(n_samples)])
    
    return np.array(data), labels

def generate_high_dimensional_data(n_samples=500, n_features=64, n_clusters=4):
    """
    Generates a synthetic high-dimensional dataset with distinct clusters.
    
    Args:
        n_samples (int): The total number of data points to generate.
        n_features (int): The dimensionality of the data.
        n_clusters (int): The number of distinct clusters.

    Returns:
        tuple: A tuple containing the data array and the cluster labels.
    """
    data = []
    labels = []
    
    # Generate random cluster centers
    centers = np.random.rand(n_clusters, n_features) * 10
    
    # Create data points around each center
    for i in range(n_samples):
        cluster_idx = np.random.randint(0, n_clusters)
        point = centers[cluster_idx] + np.random.randn(n_features) * 0.5
        data.append(point)
        labels.append(f'Cluster {cluster_idx}')
        
    return np.array(data), np.array(labels)

def reduce_dimensions_and_plot(data, labels, method='PCA', n_components=2):
    """
    Applies a dimensionality reduction method and creates a 2D or 3D plot.
    The plot is saved to an HTML file instead of being displayed directly.

    Args:
        data (np.array): The high-dimensional data.
        labels (np.array): The cluster labels for coloring the plot.
        method (str): The dimensionality reduction method ('PCA' or 't-SNE').
        n_components (int): The number of dimensions to reduce to (2 or 3).
    """
    if n_components not in [2, 3]:
        raise ValueError("n_components must be either 2 or 3 for visualization.")

    print(f"Applying {method} to reduce data from {data.shape[1]}D to {n_components}D...")
    
    if method == 'PCA':
        reducer = PCA(n_components=n_components)
        reduced_data = reducer.fit_transform(data)
        
        # Calculate explained variance for PCA
        explained_variance = reducer.explained_variance_ratio_
        explained_variance_str = [f'{v*100:.2f}%' for v in explained_variance]
        print(f"Explained variance ratios for PCA components: {explained_variance_str}")
        
    elif method == 't-SNE':
        # Using PCA as a pre-processing step for t-SNE for better performance
        # on high-dimensional data
        # We reduce to 50 dimensions first if the original data is higher
        n_components_pca = min(50, data.shape[1])
        pca_50 = PCA(n_components=n_components_pca)
        pca_result_50 = pca_50.fit_transform(data)
        
        # NOTE: The 'n_iter' parameter has been replaced by 'n_iter_without_progress'
        # in newer versions of scikit-learn.
        reducer = TSNE(n_components=n_components, perplexity=30, n_iter_without_progress=300, random_state=42)
        reduced_data = reducer.fit_transform(pca_result_50)
        
    else:
        raise ValueError("Method must be 'PCA' or 't-SNE'")

    # Create a DataFrame for easy plotting
    if n_components == 2:
        df = pd.DataFrame(reduced_data, columns=[f'{method} Component 1', f'{method} Component 2'])
        df['Label'] = labels
        fig = px.scatter(
            df, 
            x=f'{method} Component 1', 
            y=f'{method} Component 2',
            color='Label',
            title=f'2D Projection with {method}',
            labels={'color': 'Cluster'}
        )
    else: # n_components == 3
        df = pd.DataFrame(reduced_data, columns=[f'{method} Component 1', f'{method} Component 2', f'{method} Component 3'])
        df['Label'] = labels
        fig = px.scatter_3d(
            df, 
            x=f'{method} Component 1', 
            y=f'{method} Component 2', 
            z=f'{method} Component 3',
            color='Label',
            title=f'3D Projection with {method}',
            labels={'color': 'Cluster'}
        )
    
    # Save the plot to an HTML file instead of displaying it.
    output_filename = f'projection_{method.lower()}_{n_components}d.html'
    fig.write_html(output_filename)
    print(f"Plot saved to {output_filename}")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python3 dimensionality_reduction.py <path_to_data_file>")
        sys.exit(1)

    data_file = sys.argv[1]
    
    try:
        data_high_dim, labels = load_data_from_file(data_file)
        
        if data_high_dim.size == 0:
            print(f"Could not load data from '{data_file}'. Exiting.")
        else:
            # Project and plot using PCA to 2D (as requested)
            reduce_dimensions_and_plot(data_high_dim, labels, method='PCA', n_components=2)
            
            # Project and plot using t-SNE to 2D
            reduce_dimensions_and_plot(data_high_dim, labels, method='t-SNE', n_components=2)
            
    except FileNotFoundError:
        print(f"Error: The file '{data_file}' was not found. Please make sure the path is correct.")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")