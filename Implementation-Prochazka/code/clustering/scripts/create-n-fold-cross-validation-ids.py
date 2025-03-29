from itertools import chain
import os
from random import shuffle

import numpy as np

Actions = set[str]
CategoryToActionsMap = dict[str, list[str]]


def extract_actions_ids(filename: str) -> Actions:
    action_ids: Actions = set()

    with open(filename) as file:
        for line in file:
            if line.startswith("#objectKey messif.objects.keys.AbstractObjectKey "):
                split = line.split(" ")
                action_id = split[2].strip()

                action_ids.add(action_id)

    return action_ids


def create_category_to_action_mapping(actions: Actions) -> CategoryToActionsMap:
    categories: CategoryToActionsMap = {}

    for action in actions:
        sequence_id, category, from_index, length = action.split("_")

        categories.setdefault(category, []).append(action)

    return categories


def create_folds(
    categories: CategoryToActionsMap, fold_count: int
) -> list[tuple[int, CategoryToActionsMap]]:
    for category in categories.keys():
        shuffle(categories[category])

    folds: list[CategoryToActionsMap] = [{} for _ in range(fold_count)]

    for category in categories.keys():
        category_folds = np.array_split(
            np.asarray(categories[category], dtype=object), fold_count
        )

        # To keep the folds somewhat balanced (as `array_split` produces, e.g. 2,2,2,1,1,1,1,1)
        shuffle(category_folds)

        for i, category_fold in enumerate(category_folds):
            folds[i][category] = category_fold.tolist()

    return [(ith, fold) for (ith, fold) in enumerate(folds)]


def convert_to_set_of_actions(categories: CategoryToActionsMap) -> Actions:
    return set(chain(*categories.values()))


def write_output_to_file(filename: str, actions: Actions) -> None:
    with open(
        filename,
        "w",
    ) as output_file:
        for action in actions:
            output_file.write(action)
            output_file.write("\n")


if __name__ == "__main__":
    number_of_splits = 5
    number_of_folds = 10

    for split in range(number_of_splits):
        # Expects the MESSIF format
        action_ids = extract_actions_ids(
            "/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/2version/class130-actions-coords_normPOS-fps12.data"
#             "/Users/david/Developer/SDIPR/datasets/folds-id/hdm05/65/class130-actions-coords_normPOS-fps12.data-cho2014"
        )

        mapping = create_category_to_action_mapping(action_ids)

        folds = create_folds(mapping, 2)
#         folds = create_folds(mapping, number_of_folds)

        folder = f"/home/drking/Documents/bakalarka/mocap-vae-features/data/hdm05/toMWs/{split}"
#         folder = f"/Users/david/Developer/SDIPR/datasets/folds-id/hdm05/65/{split}"
        os.makedirs(folder)

        for i, fold in folds:
            actions = convert_to_set_of_actions(fold)

            write_output_to_file(
                f"{folder}/{i}.ids",
                actions,
            )
