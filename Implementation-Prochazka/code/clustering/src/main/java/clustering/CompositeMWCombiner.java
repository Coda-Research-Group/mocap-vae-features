package clustering;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author David Procházka
 */
@CommandLine.Command(
        name = "compositeMWCombiner",
        description = "Combines multiple Hard MW sequence files into one. " +
                "Each Hard MW sequence file corresponds to a body part." +
                "The produced file contains Composite MWs while retaining the position of Hard MW across multiple Composite MWs. " +
                "Single body part (Hard MW) is always on the same position (index) in each Composite MW. " +
                "The ordering is defined by the names of the input files.",
        mixinStandardHelpOptions = true
)
public final class CompositeMWCombiner implements Callable<Integer> {

    @CommandLine.Option(
            required = true,
            names = "--sequenceFolder",
            description = "Path to a folder containing one Hard MW sequence file per body part."
    )
    private String sequenceFolder;

    private CompositeMWCombiner() {
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new CompositeMWCombiner()).execute(args));
    }

    private static List<String> combineSequenceFiles(List<? extends List<String>> sequenceFiles) {
        var combinedFile = new ArrayList<String>();
        int numberOfLines = sequenceFiles.get(0).size();

        for (int lineNumber = 0; lineNumber < numberOfLines; lineNumber++) {
            List<String> line = collectSameLineAcrossFiles(sequenceFiles, lineNumber);
            String result = combineLine(line);

            combinedFile.add(result);
        }

        return combinedFile;
    }

    private static List<String> collectSameLineAcrossFiles(List<? extends List<String>> files, int lineNumber) {
        return files
                .stream()
                .map(sequenceFile -> sequenceFile.get(lineNumber))
                .toList();
    }

    private static String combineLine(List<String> line) {
        if (isHeader(line)) {
            return line.get(0);
        } else {
            return String.join(",", line);
        }
    }

    private static void checkValidity(List<? extends List<String>> sequenceFiles) {
        int numberOfLines = sequenceFiles.get(0).size();

        if (!doFilesHaveSameNumberOfLines(sequenceFiles, numberOfLines)) {
            throw new IllegalArgumentException("All files must have the same number of lines.");
        }
    }

    private static boolean isHeader(List<String> lines) {
        return lines
                .stream()
                .allMatch(line -> line.startsWith("#objectKey") || line.endsWith("mcdr.objects.impl.ObjectMotionWord"));
    }

    private static boolean doFilesHaveSameNumberOfLines(Collection<? extends List<String>> files, int numberOfLines) {
        return files
                .stream()
                .allMatch(file -> file.size() == numberOfLines);
    }

    @Override
    public Integer call() throws IOException {
        try (var paths = Files.walk(Paths.get(sequenceFolder))) {
            List<List<String>> sequenceFiles = paths
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(filePath -> filePath.getFileName().toString()))
                    .map(Util::openFileAndReadAllLines)
                    .toList();

            checkValidity(sequenceFiles);

            List<String> combinedFile = combineSequenceFiles(sequenceFiles);

            combinedFile.forEach(System.out::println);
        }

        return 0;
    }
}
