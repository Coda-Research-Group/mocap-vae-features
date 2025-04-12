import os
import sys
import re

def replace_mocap_pose(filepath):
    """
    Goes through a file and replaces specific text patterns.

    Args:
        filepath (str): The path to the file to be processed.
    """
    try:
        with open(filepath, 'r') as f:
            content = f.read()

        # Your custom replacements:
        # new_content = content.replace('mcdr.objects.ObjectMocapPose', 'messif.objects.impl.ObjectFloatVectorCosine')
        # new_content = content.replace('1;messif.objects.impl.ObjectFloatVectorCosine', 'messif.objects.impl.ObjectFloatVectorCosine')
        # new_content = content.replace('messif.objects.impl.ObjectFloatVectorCosine', '1;mcdr.sequence.impl.SequenceSegmentCodeListDTW')
        # new_content = re.sub(r'(-?\d+(?:\.\d+)?)[ \t]+(?=-?\d)', r'\1,', content)
        new_content = content.replace('1;messif.objects.impl.ObjectFloatVectorCosine\n', '')

        with open(filepath, 'w') as f:
            f.write(new_content)

        print(f"✔ Processed: {filepath}")

    except FileNotFoundError:
        print(f"✖ File not found: {filepath}")
    except Exception as e:
        print(f"✖ Error processing {filepath}: {e}")

def process_path(path):
    """
    If path is a file, process it.
    If path is a directory, process all files recursively.
    """
    if os.path.isfile(path):
        replace_mocap_pose(path)
    elif os.path.isdir(path):
        for root, _, files in os.walk(path):
            for file in files:
                full_path = os.path.join(root, file)
                replace_mocap_pose(full_path)
    else:
        print(f"✖ Invalid path: {path}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python replace_script.py <file_or_folder_path>")
        sys.exit(1)

    input_path = sys.argv[1]
    process_path(input_path)
