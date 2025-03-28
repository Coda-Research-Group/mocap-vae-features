# Script for merging categories of the HDM05 dataset.
# The source dataset is loaded, the category labels are replaced using the mapping below, and the result is written to stdout.
#
# Input: class130-actions-segment80_shift16-coords_normPOS-fps12.data (HDM05-130, 130 categories)
# Output: class130-actions-segment80_shift16-coords_normPOS-fps12.data-cho2014 (HDM05-65, 65 categories)

INPUT_FILENAME = "class130-actions-segment80_shift16-coords_normPOS-fps12.data"

# https://arxiv.org/pdf/1306.3874.pdf
mapping = {
    138: 136,
    139: 136,
    130: 129,
    132: 131,
    114: 113,
    115: 113,
    116: 113,
    142: 141,
    143: 141,
    145: 144,
    146: 144,
    148: 147,
    149: 147,
    63: 62,
    45: 42,
    46: 42,
    43: 42,
    44: 42,
    67: 58,
    59: 58,
    68: 58,
    120: 119,
    78: 77,
    80: 79,
    84: 83,
    92: 91,
    88: 87,
    90: 89,
    82: 81,
    86: 85,
    108: 107,
    106: 105,
    102: 101,
    100: 99,
    110: 109,
    104: 103,
    51: 47,
    50: 47,
    48: 47,
    49: 47,
    40: 38,
    41: 38,
    39: 38,
    112: 111,
    34: 32,
    35: 32,
    33: 32,
    118: 117,
    98: 97,
    94: 93,
    37: 30,
    36: 30,
    31: 30,
    25: 24,
    27: 26,
    64: 56,
    57: 56,
    65: 56,
    54: 52,
    55: 52,
    53: 52,
    66: 60,
    61: 60,
    137: 60,
    29: 28,
}

with open(INPUT_FILENAME) as file:
    for line in file:
        if line.startswith("#objectKey messif.objects.keys.AbstractObjectKey"):
            split = line.split("_")
            category_id = int(split[1])

            if category_id in mapping:
                line = "_".join((split[0], str(mapping[category_id]), *split[2:]))

        print(line, end="")
