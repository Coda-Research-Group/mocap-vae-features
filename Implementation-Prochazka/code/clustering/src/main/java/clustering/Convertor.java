package clustering;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Float.parseFloat;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * {@link Convertor} converts between various formats of ELKI and MESSIF. See README.md for the project terminology.
 */
@Command(
        name = "convertor",
        description = "Converts between various formats of ELKI and MESSIF. See README.md for the project terminology.",
        mixinStandardHelpOptions = true
)
public final class Convertor implements Callable<Integer> {

    /**
     * Matches a line in metadata in the ELKI clustering file (created as result of k-medoids clustering).
     * The line contains a medoid ID which can be subsequently used for finding the corresponding object in the file.
     */
    private static final Pattern ELKI_CLUSTERING_FILE_MEDOID_ID = Pattern.compile("^# Cluster Medoid: (\\d+)$");

    /**
     * Matches "label" prefix created as result of ELKI clustering.
     * The label appears at the end of each object line in ELKI clustering file.
     * The pattern is used for removing such prefix, e.g. {@code label3136_101_1708_81_0 -> 3136_101_1708_81_0}
     */
    private static final Pattern ELKI_LABEL_PREFIX = Pattern.compile("^label");

    /**
     * The filename of the single cluster produced by k-medoids clustering with {@code k=1}.
     */
    private static final String ELKI_CLUSTER_FILE_NAME = "cluster.txt";

    /**
     * A prefix of a line used to recognize if the line contains ELKI clustering object or not.
     */
    private static final String ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE = "ID=";

    /**
     * Why 3? Index 0 ~ ID, index 1 ~ number of poses, and the last index ~ AbstractObjectKey with "label" prefix.
     * See ELKI clustering format.
     */
    private static final int METADATA_LENGTH = 3;

    private static final int JOINT_COUNT = 31;
    private static final int JOINT_DIM = 3;
    private static final int POSE_DIM = JOINT_COUNT * JOINT_DIM;

    /**
     * Specifies the exposed CLI functionality via options.
     * Forces the CLI options to be mutually exclusive, and to be specified exactly once.
     * Simply put, there are two exclusive options,
     * once an option is selected it then requires one additional parameter (path to a file or folder).
     * See static inner class {@link CliOptions} for those options.
     * <p>
     * https://picocli.info/#_mutually_exclusive_options
     */
    @ArgGroup(multiplicity = "1")
    CliOptions cliOptions;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Convertor()).execute(args));
    }

    /**
     * Parses ELKI clustering folder (result of k-medoids clustering) and
     * outputs list of medoids (one for each cluster) in MESSIF format.
     *
     * @throws IOException if an I/O error is thrown when accessing the folder
     */
    private void parseMedoidsFromElkiClusteringFolder() throws IOException {
        try (var paths = Files.walk(Paths.get(MedoidParsing.elkiClusteringFolder))) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(ELKI_CLUSTER_FILE_NAME))
                    .map(this::parseMedoid)
                    .forEach(System.out::println);
        }
    }

    /**
     * Converts ELKI clustering file from the ELKI clustering format to ELKI format.
     * The ELKI format is ready for extraction of cluster medoid by ELKI k-medoids clustering.
     */
    private void convertElkiClusteringFileToElkiFormat() {
        List<String> lines = Util.openFileAndReadAllLines(Paths.get(ElkiConversion.elkiClusteringFile));
        String elkiFormat = convertElkiClusteringFileToElkiFormat(lines);

        System.out.println(elkiFormat);
    }

    /**
     * Parses a file in the ELKI clustering format and returns the cluster medoid in MESSIF format.
     *
     * @param elkiClusteringFile the ELKI clustering file represented as a path to the file
     * @return the medoid in MESSIF format
     */
    private String parseMedoid(Path elkiClusteringFile) {
        List<String> lines = Util.openFileAndReadAllLines(elkiClusteringFile);
        String medoidId = parseMedoidId(lines);
        String medoidInELKIFormat = parseObject(lines, medoidId);
        String medoidInMESSIFFormat = convertElkiClusteringObjectToMessifFormat(medoidInELKIFormat);

        return medoidInMESSIFFormat;
    }

    /**
     * Returns medoid ID from the content of the ELKI clustering file.
     *
     * @param elkiClusteringFile the ELKI clustering file represented as a collection of file lines
     * @return the medoid ID
     */
    private String parseMedoidId(Collection<String> elkiClusteringFile) {
        return elkiClusteringFile
                .stream()
                .map(ELKI_CLUSTERING_FILE_MEDOID_ID::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The file does not contain cluster medoid"));
    }

    /**
     * Returns the first object with ID equal to the {@code objectId} in {@code lines}. Assumes ELKI clustering format.
     *
     * @param elkiClusteringFile the ELKI clustering file represented as a collection of file lines
     * @param objectId           the object ID
     * @return the object with the ID in the ELKI clustering format
     */
    private String parseObject(Collection<String> elkiClusteringFile, String objectId) {
        return elkiClusteringFile
                .stream()
                .filter(line -> line.startsWith(ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE + objectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The file does not contain medoid specified by the medoid ID"));
    }

    /**
     * Converts the ELKI clustering object from the ELKI clustering format into the MESSIF format.
     *
     * @param elkiClusteringObject the ELKI clustering object
     * @return the object in the MESSIF format
     */
    private String convertElkiClusteringObjectToMessifFormat(String elkiClusteringObject) {
        var builder = new StringBuilder("#objectKey messif.objects.keys.AbstractObjectKey ");
        String[] chunks = elkiClusteringObject.split(" ");

        // Remove "label" prefix, e.g. label3136_101_1708_81_0 -> 3136_101_1708_81_0
        String lastChunk = chunks[chunks.length - 1];
        String label = ELKI_LABEL_PREFIX.matcher(lastChunk).replaceFirst("");
        builder.append(label);
        builder.append('\n');

        int numberOfPoses = (int) parseFloat(chunks[1]);
        builder.append(numberOfPoses);

        // Check if there is an appropriate number of poses and coordinates
        if (chunks.length - METADATA_LENGTH != numberOfPoses * POSE_DIM) {
            throw new IllegalArgumentException("Incorrect number of poses");
        }

        builder.append(";mcdr.objects.ObjectMocapPose\n");

        for (int poseIndex = 0; poseIndex < numberOfPoses; poseIndex++) {
            // Move by 2 because of ID (index 0) and number of poses (index 1)
            int indexOfFirstJoinCoordinateForPose = 2 + poseIndex * POSE_DIM;

            for (int joinIndex = 0; joinIndex < JOINT_COUNT; joinIndex++) {
                // Add 3D coordinates for this join
                builder.append(chunks[indexOfFirstJoinCoordinateForPose + joinIndex * JOINT_DIM]);
                builder.append(", ");
                builder.append(chunks[indexOfFirstJoinCoordinateForPose + joinIndex * JOINT_DIM + 1]);
                builder.append(", ");
                builder.append(chunks[indexOfFirstJoinCoordinateForPose + joinIndex * JOINT_DIM + 2]);

                // Do not add semicolon to the last join
                if (joinIndex != JOINT_COUNT - 1) {
                    builder.append("; ");
                }
            }

            // Do not add new line to the last pose
            if (poseIndex != numberOfPoses - 1) {
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    /**
     * Converts ELKI clustering file from the ELKI clustering format to ELKI format.
     *
     * @param elkiClusteringFile the ELKI clustering file represented as a collection of file lines
     * @return the file content in ELKI format
     */
    private String convertElkiClusteringFileToElkiFormat(Collection<String> elkiClusteringFile) {
        return elkiClusteringFile
                .stream()
                .filter(line -> line.startsWith(ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE))
                .map(this::convertElkiClusteringObjectToElkiFormat)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Converts ELKI clustering object from the ELKI clustering format to ELKI format.
     *
     * @param elkiClusteringObject the ELKI clustering object
     * @return the object in ELKI format
     */
    private String convertElkiClusteringObjectToElkiFormat(String elkiClusteringObject) {
        var builder = new StringBuilder(elkiClusteringObject.length());
        String[] chunks = elkiClusteringObject.split(" ");

        int numberOfPoses = (int) parseFloat(chunks[1]);
        builder.append(numberOfPoses);
        builder.append(' ');

        String[] delimitedValues = Arrays.copyOfRange(chunks, 2, chunks.length - 1);
        builder.append(String.join(", ", delimitedValues));
        builder.append(' ');

        String label = chunks[chunks.length - 1];
        builder.append(label);

        return builder.toString();
    }

    @Override
    public Integer call() {
        try {
            if (ElkiConversion.shouldConvertElkiClusteringFileToElkiFormat) {
                convertElkiClusteringFileToElkiFormat();
            }

            if (MedoidParsing.shouldParseMedoidsFromElkiClusteringFolder) {
                parseMedoidsFromElkiClusteringFolder();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    /**
     * The mutually exclusive functionality options, the main CLI functionality of this class.
     */
    private static class CliOptions {
        @ArgGroup(multiplicity = "1", exclusive = false)
        private ElkiConversion elkiConversion;

        @ArgGroup(multiplicity = "1", exclusive = false)
        private MedoidParsing medoidParsing;
    }

    private static class ElkiConversion {
        @Option(
                names = "--convert-elki-clustering-file-to-elki-format",
                description = "Convert ELKI clustering file to ELKI format. " +
                        "The ELKI format is ready for extraction of cluster medoid by ELKI k-medoids clustering.",
                required = true
        )
        private static boolean shouldConvertElkiClusteringFileToElkiFormat;

        @Option(names = "--elki-clustering-file", description = "Path to ELKI clustering file", required = true)
        private static String elkiClusteringFile;
    }

    private static class MedoidParsing {
        @Option(
                names = "--parse-medoids-from-elki-clustering-folder",
                description = "Takes path to ELKI clustering folder (result of k-medoids clustering) and " +
                        "outputs list of medoids (one for each cluster) in MESSIF format.",
                required = true
        )
        private static boolean shouldParseMedoidsFromElkiClusteringFolder;

        @Option(names = "--elki-clustering-folder", description = "Path to ELKI clustering folder", required = true)
        private static String elkiClusteringFolder;
    }
}
