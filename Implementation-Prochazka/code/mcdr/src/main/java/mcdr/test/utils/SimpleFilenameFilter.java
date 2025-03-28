package mcdr.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SimpleFilenameFilter implements FilenameFilter {

    // set with filenames to retain
    Set<String> filesToRetain = new HashSet<>();

    //************ Constructors ************//
    /**
     * Constructs a new instance of {@link SimpleFilenameFilter}.
     *
     * @param filesToRetain filename in which rows are names of files to retain
     * @throws java.io.IOException
     */
    public SimpleFilenameFilter(String filesToRetain) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filesToRetain));
        String line = br.readLine();
        while (line != null) {
            this.filesToRetain.add(line);
            line = br.readLine();
        }
        br.close();
    }

    //************ Implemented interface FilenameFilter ************//
    @Override
    public boolean accept(File dir, String name) {
        return filesToRetain.contains(name);
    }

}
