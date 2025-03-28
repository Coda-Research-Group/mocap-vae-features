package clustering;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author David Procházka
 */
final class Util {

    private Util() {
    }

    /**
     * Opens {@code file} and returns all lines from the file.
     * In the event of {@code IOException} the program exits with exit code 1.
     * Enables use of some methods as method references.
     *
     * @param file the file to be read
     * @return all lines from the file
     */
    static List<String> openFileAndReadAllLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
