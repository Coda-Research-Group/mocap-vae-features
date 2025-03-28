package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMotionWordSoftAssignment extends ObjectMotionWord {

    // class id for serialization
    private static final long serialVersionUID = 1L;
    // maximum number of parts of motion words that are compared
    public static int maxPartsToMatch = Integer.MAX_VALUE;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link ObjectMotionWordSoftAssignment}.
     *
     * @param data the data content of the new object
     */
    public ObjectMotionWordSoftAssignment(long[] data) {
        super(data);
    }

    /**
     * Creates a new instance of {@link ObjectMotionWordSoftAssignment}.
     *
     * @param locatorURI the locator URI for the new object
     * @param data the data content of the new object
     */
    public ObjectMotionWordSoftAssignment(String locatorURI, long[] data) {
        super(locatorURI, data);
    }

    /**
     * Creates a new instance of {@link ObjectMotionWordSoftAssignment} from
     * text stream.
     *
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the
     * stream
     */
    public ObjectMotionWordSoftAssignment(BufferedReader stream) throws EOFException, IOException {
        super(stream);
    }

    //****************** Implemented class LocalAbstractObject ******************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject lao, float f) {
        long[] objData = ((ObjectMotionWordSoftAssignment) lao).data;

        // soft assignment
        boolean found1 = false;
        for (int i = 0; i < Math.min(objData.length, maxPartsToMatch); i++) {
            if (data[0] == objData[i]) {
                found1 = true;
            }
        }
        boolean found2 = false;
        for (int i = 0; i < Math.min(data.length, maxPartsToMatch); i++) {
            if (objData[0] == data[i]) {
                found2 = true;
            }
        }
//        return (found1) ? 0f : 1f; // Q
        return (found1 || found2) ? 0f : 1f; // Q|D
//        return (found1 && found2) ? 0f : 1f; // Q&D
    }
}
