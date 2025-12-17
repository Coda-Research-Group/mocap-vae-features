# Quantization of Auto-Encoded Human Motion Features

This repository contains the source code, scripts, and auxiliary files for the thesis "Quantization of Auto-Encoded Human Motion Features."

The project extends an existing Variational Autoencoder (VAE) architecture for human motion data by incorporating quantization and evaluating the resulting "Motion Word" (MW) vocabulary.

---

## 🌳 Repository Branches

This repository is organized into three distinct branches to separate the development stages:

* **`main`**: The full development branch. This contains the complete source code, all features, and the final implementation of the project.
* **`demo-pipeline`**: A specialized branch containing the `demo_pipeline` folder and associated scripts.
* **`original-repository`**: The state of the connected repository before any new coding for this thesis began. This serves as a clean baseline.

---

## 🚀 Setup and Running the Demo Pipeline

To run the provided demo pipeline, please follow these steps:

### Prerequisites

1.  **Dependencies:** Install all necessary Python dependencies listed in the `requirements.txt` file:
    ```bash
    pip install -r requirements.txt
    ```
2.  **Java:** Install a Java Development Kit (JDK) environment. This is required for running the evaluator.
3.  **Repository Structure:** This entire repository must be cloned/downloaded.

### Running the Test Pipeline (`demo_pipeline`)

The `demo_pipeline` folder contains a small dataset and two test scripts.

1.  **Configure `train.py`:**
    * Edit line 261 to specify the correct **repository filepath**.
    * Edit line 297, adjusting it based on your **CUDA GPU availability** and configuration.

2.  **Configure Pipeline Scripts:**
    * In the pipeline scripts within `demo_pipeline` (`local-pipeline.sh` and `metacentrum-pipeline.sh`), fill in all **TODOs**.
    * Crucially, edit the repository path and the **Java source path** to point to the correct locations on your system.

3.  **Execution:** Run the desired pipeline script:
    * `local-pipeline.sh` (for local execution)
    * `metacentrum-pipeline.sh` (for execution on a Metacentrum cluster)

---

## 📂 Project Structure

This section details the purpose of the primary folders in the repository:

| Folder Name | Description |
| :--- | :--- |
| `clustering` | Contains the **run scripts for clustering** the learned latent features to generate the Motion Word (MW) vocabulary. |
| `data-splitting-scripts` | Includes scripts for **decomposition of human body into body parts** and for performing the standard **train/test splitting** of the dataset. |
| `demo_pipeline` | Contains a small dataset and two `.sh` scripts (`local-pipeline.sh`, `metacentrum-pipeline.sh`) serving as **test pipelines**. **Note:** The dataset `demo_pipeline/data/class130-actions-segment80_shift16-coords_normPOS-fps12.npz` is too large for this repository. It is available for download in the [Thesis Attachments](**Will be adjusted after publishing**). |
| `experiments` | Stores **Hydra parallelization files** used for managing and running large batches of experiments efficiently. |
| `Implementation-Prochazka` | Contains three **Java repositories** that have been adapted from the original diploma thesis by Procházka. |
| `quantization` | Holds the core **run scripts for clustering, MW vocabulary transition, and evaluation** of the quantized features. |
| `SCL-quality` | Includes scripts dedicated to calculating **Geometric Similarity Preserving measures** used to assess the quality of the quantized space. |
| `vae-scripts` | Scripts used for **parallel training** of the VAE models, primarily configured for the **Metacentrum** environment. |
| `src` | Standard folder for common source code modules and utilities. |

---

## 📝 Additional Files and Contributions

* **`requirements.txt`**: Lists all required Python packages for the project.
* **`evaluator.jar`**: The compiled Java evaluation utility.
* **Other Add-on Scripts**: The repository contains various supplementary scripts (`*.sh`, `*.py`, `*.ipynb`) for specific tasks like data conversion, plotting, and reproducibility.
* **Original Repository**: This work builds upon an original VAE implementation. The contributions of this thesis (the MW repository) can be observed by comparing the code difference (diff) between the original repository (branch: original-repository) and this repository (branch main), which is available on GitHub (URL: **<https://github.com/Coda-Research-Group/mocap-vae-features>**).