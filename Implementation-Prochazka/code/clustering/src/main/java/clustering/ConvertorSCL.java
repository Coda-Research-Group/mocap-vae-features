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
        // Ensure vector dimension is provided when parsing medoids
        if (MedoidParsing.vectorDim <= 0) {
            throw new IllegalArgumentException("Missing or invalid required option: '--vector-dim=<positive_integer>' must be provided when parsing medoids.");
        }

        Path startPath = Paths.get(MedoidParsing.elkiClusteringFolder);
        if (!Files.isDirectory(startPath)) {
            throw new IOException("Provided ELKI clustering folder path is not a directory: " + MedoidParsing.elkiClusteringFolder);
        }

        // Use try-with-resources for the stream
        try (Stream<Path> paths = Files.walk(startPath)) {
            paths
                    .filter(Files::isRegularFile)
                    // Find the specific cluster output file within each sub-folder
                    .filter(p -> p.getFileName().toString().equals(ELKI_CLUSTER_FILE_NAME))
                    .map(this::parseMedoidVector) // Use the new vector parsing method
                    .forEach(System.out::println); // Print each medoid vector to standard output
        }
    }

    /**
     * Converts ELKI clustering file (output from the initial clustering run)
     * from the ELKI clustering format (with metadata) to a simpler ELKI format
     * (likely just vectors with labels), suitable for input to the next clustering step (k=1 medoid finding).
     * This function seems less relevant now if the goal is just the final medoid vectors,
     * but kept for compatibility with the script's workflow.
     */
    private void convertElkiClusteringFileToElkiFormat() {
        List<String> lines = Util.openFileAndReadAllLines(Paths.get(ElkiConversion.elkiClusteringFile));
        String elkiFormat = convertElkiClusteringFileToElkiFormat(lines);
        System.out.println(elkiFormat);
    }

    // --- Helper Methods for Medoid Vector Parsing ---

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
        // Find the line in the file corresponding to the medoid ID
        String medoidLineInELKIFormat = parseObjectLine(lines, medoidId);
        // Convert that line into the desired float vector output format
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
                .map(ELKI_CLUSTERING_FILE_MEDOID_ID::matcher) // Try to match the medoid ID pattern
                .filter(Matcher::matches)                    // Keep only successful matches
                .map(matcher -> matcher.group(1))            // Extract the ID (group 1)
                .findFirst()                                 // Get the first match
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
        String prefixToFind = ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE_PREFIX + objectId + " "; // Add space for robustness
        return elkiClusteringFileLines
                .stream()
                .filter(line -> line.startsWith(prefixToFind)) // Find the line starting with "ID=..."
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find object line starting with '" + prefixToFind + "' in the input file."));
    }

    // --- The core conversion logic ---

    /**
     * Converts a line representing an object from the ELKI clustering output format
     * into the desired MESSIF-like format containing the raw float vector.
     * Assumes the input line format is roughly: "ID=<id> <float1> <float2> ... <floatN> label<label_suffix>"
     *
     * @param elkiObjectLine The single line from the ELKI output file representing the medoid object.
     * @param vectorDim      The expected dimensionality of the float vector.
     * @return A string representing the object in the MESSIF vector format.
     * @throws IllegalArgumentException if the line format doesn't match expectations or the number of floats is wrong.
     */
    private String convertElkiObjectLineToVectorMessifFormat(String elkiObjectLine, int vectorDim) {
        // Split the line into parts based on spaces
        String[] chunks = elkiObjectLine.trim().split("\\s+"); // Use regex \\s+ for any whitespace

        // More specific check (can be adapted if format is slightly different, e.g. includes cluster id)
        if (chunks.length != vectorDim + 3) {
            System.err.printf(
                    "Warning: Expected exactly %d parts (ID + sequence length + %d vector dimensions + label), but found %d parts. Assuming vector is dimensions [1 to %d] and last part is label. Line: '%s'%n",
                    vectorDim + 3, vectorDim, chunks.length, vectorDim, elkiObjectLine);
            // Decide how to handle this: proceed cautiously or throw error? Let's proceed for now.
        }


        // --- Construct the output string ---
        var builder = new StringBuilder();

        // 1. Extract Label (last chunk, remove "label" prefix)
        String lastChunk = chunks[chunks.length - 1];
        String label = ELKI_LABEL_PREFIX.matcher(lastChunk).replaceFirst("");

        // 2. Build Header
        builder.append("#objectKey messif.objects.keys.AbstractObjectKey ").append(label);
        builder.append('\n'); // Newline after header

        // 3. Extract Vector components
        // Vector values are expected to be from index 1 up to index vectorDim (inclusive)
        int vectorStartIndex = 2;
        int vectorEndIndex = vectorStartIndex + vectorDim; // Exclusive index for copyOfRange

        if (vectorEndIndex > chunks.length -1) { // Check if we overrun before the label
            throw new IllegalArgumentException(String.format(
                    "Error parsing object line: Not enough parts for vector dimension %d before the label. Found %d parts total. Line: '%s'",
                    vectorDim, chunks.length, elkiObjectLine));
        }

        // 4. Build sequence counter.
        builder.append(String.format("%d;messif.objects.impl.ObjectFloatVectorCosine", 1));
        builder.append('\n');

        String[] vectorComponents = Arrays.copyOfRange(chunks, vectorStartIndex, vectorEndIndex);

        // 5. Append vector components, space-separated
        builder.append(String.join(" ", vectorComponents));

        // No trailing newline needed usually for the vector data line itself


        return builder.toString();
    }

    // --- Conversion from ELKI clustering format to plain ELKI format (kept for script compatibility) ---

    /**
     * Converts an ELKI clustering file (potentially with metadata and multiple objects per line based on original format)
     * to a simpler ELKI format (likely one vector per line with label).
     * NOTE: The implementation `convertElkiClusteringObjectToElkiFormat` might need review
     * if the input `elkiClusteringFile` has a complex structure not handled here.
     *
     * @param elkiClusteringFileLines the ELKI clustering file represented as a collection of file lines
     * @return the file content in a simpler ELKI format (vectors + labels)
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
     * Converts a single object line from ELKI clustering format to a simpler ELKI format.
     * **WARNING:** This function's logic seems based on the *original* 3Dx31 data assumption (parsing `numberOfPoses`, using commas).
     * It likely needs to be rewritten if the input to *this* conversion step is already just 256D vectors.
     * If the input files to `--convert-elki-clustering-file-to-elki-format` are already just
     * `<value1> <value2> ... <valueN> label<label>` then this function might not be needed or should just pass the line through.
     * Assuming for now it might still receive the complex format sometimes.
     *
     * @param elkiClusteringObject the ELKI clustering object line
     * @return the object in ELKI format (potentially simplified vector + label)
     */
    private String convertElkiClusteringObjectToElkiFormat(String elkiClusteringObject) {
        // THIS IMPLEMENTATION IS SUSPECT - Assumes old format based on parseFloat(chunks[1]) etc.
        // It should likely be simplified if the input here is already vector-based.
        // For now, returning a placeholder or trying a simplified conversion.

        String[] chunks = elkiClusteringObject.trim().split("\\s+");
        if (chunks.length < 2) return ""; // Need at least ID and label

        // Simple assumption: Output all chunks except the ID, keep spaces, keep label
        // ID=id val1 val2 ... valN labelSuffix -> val1 val2 ... valN labelSuffix
        // This is a guess - the required output format for the k=1 KMedoids step needs clarification.
        if (chunks[0].startsWith(ELKI_CLUSTERING_OBJECT_VALID_OBJECT_LINE_PREFIX)) {
            // Copy all chunks starting from index 1 (after ID=...)
            String[] valueAndLabelChunks = Arrays.copyOfRange(chunks, 1, chunks.length);
            return String.join(" ", valueAndLabelChunks);
        } else {
            // Line doesn't start with ID=, maybe it's already in the right format?
            // Or maybe it's an unexpected line. Return empty/log warning.
            System.err.println("Warning: Unexpected line format in convertElkiClusteringObjectToElkiFormat: " + elkiClusteringObject);
            return "";
        }

        /* // Original complex logic (likely incorrect for vector input)
        var builder = new StringBuilder(elkiClusteringObject.length());
        String[] chunks = elkiClusteringObject.split(" ");

        // This assumes chunks[1] is numberOfPoses - probably wrong for 256D vector input
        // int numberOfPoses = (int) parseFloat(chunks[1]);
        // builder.append(numberOfPoses); // This doesn't make sense for a single vector line
        // builder.append(' ');

        // This assumes data starts at index 2 and uses commas - likely wrong
        String[] delimitedValues = Arrays.copyOfRange(chunks, 2, chunks.length - 1);
        builder.append(String.join(", ", delimitedValues)); // Using comma separator? Check ELKI input format needs
        builder.append(' ');

        String label = chunks[chunks.length - 1];
        builder.append(label);

        return builder.toString();
        */
    }

    // --- Picocli Command Execution ---

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
            return 1; // Indicate error
        } catch (IllegalArgumentException e) {
            System.err.println("An input error occurred: " + e.getMessage());
            // e.printStackTrace(System.err); // Optional: full stack trace
            return 1; // Indicate error
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1; // Indicate error
        }

        System.err.println("Operation completed successfully.");
        return 0; // Indicate success
    }

    // --- Picocli Option Classes ---

    /**
     * Groups the mutually exclusive command options.
     */
    static class CliOptions {
        // Need to reference the static inner classes directly
        @ArgGroup(validate = false) // Let individual groups handle validation
                ElkiConversion elkiConversion;

        @ArgGroup(validate = false) // Let individual groups handle validation
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
                required = true // Required if the parent ArgGroup is chosen
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
                required = true // Required if the parent ArgGroup is chosen
        )
        static String elkiClusteringFolder;

        @Option(
                names = "--vector-dim",
                description = "Dimensionality of the float vectors being processed and outputted.",
                // Make it required only when parsing medoids. Handled explicitly in parseMedoidsFromElkiClusteringFolder.
                required = false // Cannot be required=true here due to mutual exclusion with the other ArgGroup
                // We perform manual check within the relevant method instead.
        )
        // Needs to be static to be accessed from static context or passed around
        static int vectorDim = -1; // Default to invalid, checked later
    }

    // --- Utility Class (Placeholder - assumes it exists and works) ---
    static class Util {
        /**
         * Placeholder for a utility method to read all lines from a file.
         * Implement proper error handling (try-with-resources, catch IOException).
         */
        public static List<String> openFileAndReadAllLines(Path filePath) {
            try {
                return Files.readAllLines(filePath);
            } catch (IOException e) {
                // Rethrow as a runtime exception or handle more gracefully
                throw new RuntimeException("Failed to read file: " + filePath, e);
            }
        }
    }
}