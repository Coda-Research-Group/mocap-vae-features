import argparse
import random
import re

def sample_subset(train_path, output_path, scenario, sample_size=50000):
    # Determine which camera to select based on scenario
    camera = "M" if scenario == "cs" else "R"
    print(f"Reading TRAIN file: {train_path}")
    print(f"Scenario '{scenario}' -> sampling camera '{camera}'")
    print(f"Desired sample size: {sample_size}")

    # Regex to extract camera ID (L/M/R) after the first dash
    camera_regex = re.compile(r"#objectKey\s+messif\.objects\.keys\.AbstractObjectKey\s+\d{4}-([LMR])_")

    objects = []
    current_object = []
    current_camera = None

    with open(train_path, "r") as f:
        for line in f:
            if line.startswith("#objectKey"):
                # If previous object belongs to the correct camera, store it
                if current_camera == camera and current_object:
                    objects.append("".join(current_object))
                current_object = [line]
                match = camera_regex.search(line)
                current_camera = match.group(1) if match else None
            else:
                current_object.append(line)

        # Handle last object
        if current_camera == camera and current_object:
            objects.append("".join(current_object))

    print(f"Total matching objects with camera '{camera}' found in train file: {len(objects)}")

    if not objects:
        print("WARNING: no matching objects found! Check your camera IDs or input file.")
        return

    # Randomly sample up to desired count
    sampled = random.sample(objects, min(sample_size, len(objects)))

    print(f"Writing sampled {len(sampled)} objects to {output_path} ...")
    with open(output_path, "w") as out:
        for obj in sampled:
            out.write(obj)
    print("Done.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create subset of PKU-MMD data for KMedoids PAM clustering.")
    parser.add_argument("train", help="Path to the train data file (.data-train)")
    parser.add_argument("--output", required=True, help="Output file for the subset")
    parser.add_argument("--scenario", choices=["cs", "cv"], required=True, help="Scenario: cs (cross-subject) or cv (cross-view)")
    parser.add_argument("--size", type=int, default=50000, help="Subset size")
    args = parser.parse_args()

    sample_subset(args.train, args.output, args.scenario, args.size)
