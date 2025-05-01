CategoryToActionsMap = dict[str, list[list[str]]]


def write_output_to_file(filename: str, values: CategoryToActionsMap) -> None:
    with open(
        filename,
        "w",
    ) as output_file:
        for category in values.keys():
            for action in values[category]:
                output_file.write("".join(action))


def create_category_action_mapping(
    filename: str, actions_to_keep: set[str]
) -> CategoryToActionsMap:
    categories: CategoryToActionsMap = {}

    with open(filename) as file:
        category_id = ""
        action: list[str] = []

        for line in file:
            if line.startswith("#objectKey messif.objects.keys.AbstractObjectKey"):
                # Starting a new action, store the previous one
                if category_id != "":
                    if (
                        "_".join(action[0].split(" ")[2].strip().split("_")[:4])
                        in actions_to_keep
                    ):
                        if category_id in categories:
                            categories[category_id].append(action)
                        else:
                            categories[category_id] = [action]

                    action = []

                category_id = line.split("_")[1]
                action = [line]
            else:
                action.append(line)

        # Last action
        if "_".join(action[0].split(" ")[2].strip().split("_")[:4]) in actions_to_keep:
            if category_id in categories:
                categories[category_id].append(action)
            else:
                categories[category_id] = [action]

    return categories


def create_data_files(actions_file: str, data_file: str, fold_count: int) -> None:
    number_of_splits = 5

    for split in range(number_of_splits):
        for fold in range(fold_count):
            actions_to_keep: set[str] = set()

            with open(f"{actions_file}/{split}/{fold}.ids") as file:
                for line in file:
                    actions_to_keep.add(line.strip())

            fold_data = create_category_action_mapping(data_file, actions_to_keep)

            write_output_to_file(f"{data_file}-split{split}-fold{fold}", fold_data)


def create_data_files_pku(actions_file: str, data_file: str) -> None:
    actions_to_keep: set[str] = set()

    with open(actions_file) as file:
        for line in file:
            actions_to_keep.add(line.strip())

    fold_data = create_category_action_mapping(data_file, actions_to_keep)

    write_output_to_file(f"{data_file}-cs-train", fold_data)


# create_data_files(
#     "/home/drking/Documents/bakalarka/data/SCL/folds-id/hdm05",
# #     "/Users/david/Developer/SDIPR/datasets/folds-id/hdm05/65",
#     #
#     "/home/drking/Documents/bakalarka/data/SCL/folds-data/predictions_segmented_dim=256_beta=1_modelhdm05.data",
# #     "/Users/david/Developer/SDIPR/datasets/folds-data/hdm05/65/class130-actions-segment80_shift16-coords_normPOS-fps12.data-cho2014",
#
#     2,
# #     10,
# )

create_data_files_pku(
    "/home/drking/Documents/bakalarka/data/pku-mmd/splits/CS_train_objects_messif-lines.txt",
    "/home/drking/Documents/bakalarka/data/pku-mmd/actions_singlesubject-segment24_shift4.8_initialshift0-coords_normPOS-fps10.data",
)
