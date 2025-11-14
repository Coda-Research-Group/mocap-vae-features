import argparse
import random
import re

def sample_subset(train_path, output_path, sample_size=50000):
    print(f"Reading TRAIN file: {train_path}")
    print(f"Desired sample size: {sample_size}")

    # Regex to detect start of a new object
    object_key_regex = re.compile(r"#objectKey\s+messif\.objects\.keys\.AbstractObjectKey")

    objects = []
    current_object = []

    with open(train_path, "r") as f:
        for line in f:
            if object_key_regex.search(line):
                # Save previous object if it exists
                if current_object:
                    objects.append("".join(current_object))
                current_object = [line]
            else:
                current_object.append(line)

        # Save last object
        if current_object:
            objects.append("".join(current_object))

    print(f"Total objects found: {len(objects)}")

    if not objects:
        print("ERROR: No objects found in the dataset.")
        return

    # Random sampling
    sampled = random.sample(objects, min(sample_size, len(objects)))

    print(f"Writing {len(sampled)} sampled objects to {output_path} ...")
    with open(output_path, "w") as out:
        for obj in sampled:
            out.write(obj)
    print("Done.\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create subset of PKU-MMD data for KMedoids PAM clustering.")
    parser.add_argument("train", help="Path to the train data file (.data-train)")
    parser.add_argument("--output", required=True, help="Output file for the subset")
    parser.add_argument("--size", type=int, default=60000, help="Subset size")
    args = parser.parse_args()

    sample_subset(args.train, args.output, args.size)
