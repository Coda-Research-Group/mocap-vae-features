def create_clustering_files(
    data_file: str, cluster_folder: str, filename: str, splits: int, fold_count: int
) -> None:
    for split in range(splits):
        for fold in range(fold_count):
            print(split, fold)

            action_lines_to_cluster = []

            folds = [
                str(cluster_fold)
                for cluster_fold in range(fold_count)
                if cluster_fold != fold
            ]

            for f in folds:
                print(f"{data_file}/{filename}-split{split}-fold{f}")
                with open(f"{data_file}/{filename}-split{split}-fold{f}") as file:
                    for line in file:
                        action_lines_to_cluster.append(line)

            fs = ",".join(folds)

            print(f"{cluster_folder}/{filename}-split{split}-fold{fs}")
            with open(
                f"{cluster_folder}/{filename}-split{split}-fold{fs}", "w"
            ) as file:
                for line in action_lines_to_cluster:
                    file.write(line)


create_clustering_files(
    # "/Users/david/Developer/SDIPR/datasets/folds-data/hdm05/65",
    "/home/drking/Documents/bakalarka/data/SCL/folds-data",
    #
    # "/Users/david/Developer/SDIPR/datasets/folds-cluster/hdm05/65",
    "/home/drking/Documents/bakalarka/data/SCL/folds-cluster",
    #
    # "class130-actions-segment80_shift16-coords_normPOS-fps12.data-cho2014",
    "predictions_segmented_dim=256_beta=1_modelhdm05.data",
    #
    # 5,
    5,
    # 10,
    2,
)
