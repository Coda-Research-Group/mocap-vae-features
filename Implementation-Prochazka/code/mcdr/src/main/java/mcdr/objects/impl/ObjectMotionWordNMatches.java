package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMotionWordNMatches extends ObjectMotionWord {

    // class id for serialization
    private static final long serialVersionUID = 1L;
    // number of parts of motion words that have to be matched in order to consider a pair of motion words to be the same
    public static int nMatches = 1;
    // maximum number of parts of motion words that are compared
    public static int maxPartsToMatch = Integer.MAX_VALUE;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link ObjectMotionWordNMatches}.
     *
     * @param data the data content of the new object
     */
    public ObjectMotionWordNMatches(long[] data) {
        super(data);
    }

    /**
     * Creates a new instance of {@link ObjectMotionWordNMatches}.
     *
     * @param locatorURI the locator URI for the new object
     * @param data the data content of the new object
     */
    public ObjectMotionWordNMatches(String locatorURI, long[] data) {
        super(locatorURI, data);
    }

    /**
     * Creates a new instance of {@link ObjectMotionWordNMatches} from text
     * stream.
     *
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the
     * stream
     */
    public ObjectMotionWordNMatches(BufferedReader stream) throws EOFException, IOException {
        super(stream);
    }

    //****************** Implemented class LocalAbstractObject ******************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject lao, float f) {
        long[] objData = ((ObjectMotionWordNMatches) lao).data;
        int matchCount = 0;
        for (int i = 0; i < Math.min(data.length, maxPartsToMatch); i++) {
            if (data[i] == objData[i]) {
                matchCount++;
            }
        }
        return (matchCount >= nMatches) ? 0f : 1f;
    }
}
