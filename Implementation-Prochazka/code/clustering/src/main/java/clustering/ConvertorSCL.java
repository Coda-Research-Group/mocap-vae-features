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
import java.util.stream.Stream; // Added for Files.walk

// Removed unused import: import static java.lang.Float.parseFloat;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * {@link ConvertorSCL} converts between various formats of ELKI and potentially outputs medoids as float vectors.
 * See README.md for the project terminology.
 * <p>
 * Modified to output medoids found by ELKI as raw float vectors in a simple MESSIF-like format,
 * with configurable vector dimensionality.
 *
 * @Author Tomas Drkos
 */
@Command(
        name = "convertor",
        description = "Converts between various formats of ELKI and outputs medoids as float vectors. See README.md for the project terminology.",
        mixinStandardHelpOptions = true
)
public final class ConvertorSCL implements Callable<Integer> {

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
     * Assumed to be within the folder specified by --elki-clustering-folder.
     */
    private static final String ELKI_CLUSTER_FILE_NAME = "cluster.txt";

    /**
     * A prefix of a line used to recognize if the line contains ELKI clustering object or not.
     */
    private static final String ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE_PREFIX = "ID=";


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

    // --- Main Entry Point ---
    public static void main(String[] args) {
        // Use try-with-resources for CommandLine if it implements AutoCloseable, otherwise standard execute
        int exitCode = new CommandLine(new ConvertorSCL()).execute(args);
        System.exit(exitCode);
    }

    // --- Core Logic Methods ---

    /**
     * Parses ELKI clustering folder (result of k-medoids clustering with k=1 on each cluster) and
     * outputs list of medoids (one for each cluster) as float vectors in MESSIF-like format.
     * Requires --vector-dim to be set.
     *
     * @throws IOException if an I/O error is thrown when accessing the folder
     */
    private void parseMedoidsFromElkiClusteringFolder() throws IOException {

        if (MedoidParsing.vectorDim <= 0) {
            throw new IllegalArgumentException("Missing or invalid required option: '--vector-dim=<positive_integer>' must be provided when parsing medoids.");
        }

        Path startPath = Paths.get(MedoidParsing.elkiClusteringFolder);
        if (!Files.isDirectory(startPath)) {
            throw new IOException("Provided ELKI clustering folder path is not a directory: " + MedoidParsing.elkiClusteringFolder);
        }

        try (Stream<Path> paths = Files.walk(startPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(ELKI_CLUSTER_FILE_NAME))
                    .map(this::parseMedoidVector)
                    .forEach(System.out::println);
        }
    }

    /**
     * Converts ELKI clustering file (output from the initial clustering run)
     * from the ELKI clustering format (with metadata) to a MESSIF format accepted by ObjectFloatVectorCosine.
     *
     */
    private void convertElkiClusteringFileToElkiFormat() {
        List<String> lines = Util.openFileAndReadAllLines(Paths.get(ElkiConversion.elkiClusteringFile));
        String elkiFormat = convertElkiClusteringFileToElkiFormat(lines);
        System.out.println(elkiFormat);
    }


    /**
     * Parses a single ELKI clustering file (result of k=1 KMedoids)
     * and returns the cluster medoid as a float vector in MESSIF-like format.
     *
     * @param elkiClusteringFile the ELKI clustering file (e.g., cluster.txt) represented as a path to the file
     * @return the medoid as a float vector string in MESSIF-like format
     */
    private String parseMedoidVector(Path elkiClusteringFile) {
        List<String> lines = Util.openFileAndReadAllLines(elkiClusteringFile);
        String medoidId = parseMedoidId(lines);
        String medoidLineInELKIFormat = parseObjectLine(lines, medoidId);
        String medoidInVectorFormat = convertElkiObjectLineToVectorMessifFormat(medoidLineInELKIFormat, MedoidParsing.vectorDim);

        return medoidInVectorFormat;
    }

    /**
     * Returns medoid ID from the metadata comments in the ELKI clustering file.
     *
     * @param elkiClusteringFileLines the ELKI clustering file represented as a collection of file lines
     * @return the medoid ID string
     * @throws IllegalArgumentException if the medoid ID comment line is not found
     */
    private String parseMedoidId(Collection<String> elkiClusteringFileLines) {
        return elkiClusteringFileLines
                .stream()
                .map(ELKI_CLUSTERING_FILE_MEDOID_ID::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find '# Cluster Medoid: <id>' line in the input file."));
    }

    /**
     * Returns the specific line corresponding to the object with ID equal to the {@code objectId} in {@code lines}.
     * Assumes the line starts with "ID=<objectId>".
     *
     * @param elkiClusteringFileLines the ELKI clustering file represented as a collection of file lines
     * @param objectId                the object ID to find
     * @return the full line for the object in the ELKI clustering format
     * @throws IllegalArgumentException if a line for the specified objectId is not found
     */
    private String parseObjectLine(Collection<String> elkiClusteringFileLines, String objectId) {
        String prefixToFind = ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE_PREFIX + objectId + " ";
        return elkiClusteringFileLines
                .stream()
                .filter(line -> line.startsWith(prefixToFind))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find object line starting with '" + prefixToFind + "' in the input file."));
    }


    /**
     * Converts a line representing an object from the ELKI clustering output format
     * into the desired MESSIF-like format containing the raw float vector.
     * Assumes the input line format is roughly: " #objectMessif... ID=<id> <float1>,<float2>, ... ,<floatN>"
     *
     * @param elkiObjectLine The single line from the ELKI output file representing the medoid object.
     * @param vectorDim      The expected dimensionality of the float vector.
     * @return A string representing the object in the MESSIF vector format.
     * @throws IllegalArgumentException if the line format doesn't match expectations or the number of floats is wrong.
     */
    private String convertElkiObjectLineToVectorMessifFormat(String elkiObjectLine, int vectorDim) {
        String[] chunks = elkiObjectLine.trim().split("\\s+");

        // The constant 3 is based on sperated objects, that are not vectors.
        if (chunks.length != vectorDim + 3) {
            System.err.printf(
                    "Warning: Expected exactly %d parts (ID + sequence length + %d vector dimensions + label), but found %d parts. Assuming vector is dimensions [1 to %d] and last part is label. Line: '%s'%n",
                    vectorDim + 3, vectorDim, chunks.length, vectorDim, elkiObjectLine);
        }


        var builder = new StringBuilder();

        String lastChunk = chunks[chunks.length - 1];
        String label = ELKI_LABEL_PREFIX.matcher(lastChunk).replaceFirst("");

        builder.append("#objectKey messif.objects.keys.AbstractObjectKey ").append(label);
        builder.append('\n');

        int vectorStartIndex = 2;
        int vectorEndIndex = vectorStartIndex + vectorDim;

        if (vectorEndIndex > chunks.length -1) {
            throw new IllegalArgumentException(String.format(
                    "Error parsing object line: Not enough parts for vector dimension %d before the label. Found %d parts total. Line: '%s'",
                    vectorDim, chunks.length, elkiObjectLine));
        }

         // if object/sequence needed this part of header
//        builder.append(String.format("%d;messif.objects.impl.ObjectFloatVectorCosine", 1));
//        builder.append('\n');

        String[] vectorComponents = Arrays.copyOfRange(chunks, vectorStartIndex, vectorEndIndex);

        builder.append(String.join(",", vectorComponents));



        return builder.toString();
    }


    /**
     * Converts an ELKI clustering file to a MESSIF format.
     *
     * @param elkiClusteringFileLines the ELKI clustering file represented as a collection of file lines
     * @return the file content in a MESSIF format (vectors + labels)
     */
    private String convertElkiClusteringFileToElkiFormat(Collection<String> elkiClusteringFileLines) {
        return elkiClusteringFileLines
                .stream()
                // Filter only lines that likely represent data objects (start with ID=...)
                .filter(line -> line.trim().startsWith(ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE_PREFIX))
                .map(this::convertElkiClusteringObjectToElkiFormat) // Convert each valid line
                .filter(s -> !s.isEmpty()) // Filter out potentially empty results if conversion fails
                .collect(Collectors.joining("\n")); // Join lines with newline
    }

    /**
     * Converts a single object line from ELKI clustering format to a MESSIF format.
     *
     * @param elkiClusteringObject the ELKI clustering object line
     * @return the object in MESSIF format (potentially simplified vector + label)
     */
    private String convertElkiClusteringObjectToElkiFormat(String elkiClusteringObject) {

        String[] chunks = elkiClusteringObject.trim().split("\\s+");
        if (chunks.length < 2) return "";

        if (chunks[0].startsWith(ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE_PREFIX)) {
            String[] valueAndLabelChunks = Arrays.copyOfRange(chunks, 1, chunks.length);
            return String.join(" ", valueAndLabelChunks);
        } else {
            System.err.println("Warning: Unexpected line format in convertElkiClusteringObjectToElkiFormat: " + elkiClusteringObject);
            return "";
        }
    }

    @Override
    public Integer call() {
        try {
            // Execute the chosen operation based on CLI flags
            if (cliOptions.elkiConversion != null && ElkiConversion.shouldConvertElkiClusteringFileToElkiFormat) {
                System.err.println("Executing: Convert ELKI Clustering File to ELKI Format");
                convertElkiClusteringFileToElkiFormat();
            } else if (cliOptions.medoidParsing != null && MedoidParsing.shouldParseMedoidsFromElkiClusteringFolder) {
                System.err.println("Executing: Parse Medoids from ELKI Clustering Folder (Outputting Vectors)");
                parseMedoidsFromElkiClusteringFolder();
            } else {
                // This case should not happen due to ArgGroup(multiplicity = "1")
                System.err.println("Error: No valid operation specified.");
                CommandLine.usage(this, System.err);
                return 1;
            }
        } catch (IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        } catch (IllegalArgumentException e) {
            System.err.println("An input error occurred: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }

        System.err.println("Operation completed successfully.");
        return 0;
    }


    /**
     * Groups the mutually exclusive command options.
     */
    static class CliOptions {
        @ArgGroup(validate = false)
                ElkiConversion elkiConversion;

        @ArgGroup(validate = false)
        MedoidParsing medoidParsing;
    }

    /**
     * Options related to converting an ELKI clustering file to ELKI format.
     */
    static class ElkiConversion {
        @Option(
                names = "--convert-elki-clustering-file-to-elki-format",
                description = "Convert ELKI clustering file (output of main clustering) to a simpler ELKI format " +
                        "(suitable for k=1 medoid finding input).",
                required = true
        )
        // Make it non-static if accessed from instance methods, or keep static if only used statically
        static boolean shouldConvertElkiClusteringFileToElkiFormat;

        @Option(
                names = "--elki-clustering-file",
                description = "Path to the input ELKI clustering file (output from initial clustering).",
                required = true
        )
        static String elkiClusteringFile;
    }

    /**
     * Options related to parsing medoids from the k=1 ELKI output folders.
     */
    static class MedoidParsing {
        @Option(
                names = "--parse-medoids-from-elki-clustering-folder",
                description = "Takes path to ELKI clustering folder (containing k=1 KMedoids results for each cluster) and " +
                        "outputs the identified medoid for each cluster as a float vector in MESSIF-like format.",
                required = true
        )
        static boolean shouldParseMedoidsFromElkiClusteringFolder;

        @Option(
                names = "--elki-clustering-folder",
                description = "Path to the parent ELKI clustering folder containing subfolders (e.g., cluster_0, cluster_1) " +
                        "each with a " + ELKI_CLUSTER_FILE_NAME + " from a k=1 KMedoids run.",
                required = true
        )
        static String elkiClusteringFolder;

        @Option(
                names = "--vector-dim",
                description = "Dimensionality of the float vectors being processed and outputted.",
                required = false
        )
        static int vectorDim = -1;
    }

    static class Util {
        /**
         * Placeholder for a utility method to read all lines from a file.
         * Implement proper error handling (try-with-resources, catch IOException).
         */
        public static List<String> openFileAndReadAllLines(Path filePath) {
            try {
                return Files.readAllLines(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + filePath, e);
            }
        }
    }
}