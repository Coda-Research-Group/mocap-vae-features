package mcdr.objects.impl;

import messif.objects.LocalAbstractObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serial;
import java.util.function.BiPredicate;

/**
 * Modification of Composite MW used in the two-stage classification framework.
 * The main difference is that the matching function can be specified dynamically ({@link ObjectMotionWordCompositeAutoTuning#matchingFunction}).
 *
 * @author David Procházka
 */
public class ObjectMotionWordCompositeAutoTuning extends ObjectMotionWordComposite {

    @Serial
    private static final long serialVersionUID = 42L;

    /**
     * Matching function
     */
    public static BiPredicate<ObjectMotionWordComposite, ObjectMotionWordComposite> matchingFunction;

    /**
     * Creates a new instance.
     *
     * @param data the data content of the new object
     */
    public ObjectMotionWordCompositeAutoTuning(long[] data) {
        super(data);
    }

    /**
     * Creates a new instance.
     *
     * @param locatorURI the locator URI for the new object
     * @param data       the data content of the new object
     */
    public ObjectMotionWordCompositeAutoTuning(String locatorURI, long[] data) {
        super(locatorURI, data);
    }

    /**
     * Creates a new instance from text stream.
     *
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException  if there was an I/O error during reading from the
     *                      stream
     */
    public ObjectMotionWordCompositeAutoTuning(BufferedReader stream) throws EOFException, IOException {
        super(stream);
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject lao, float f) {
        return matchingFunction.test(this, (ObjectMotionWordCompositeAutoTuning) lao) ? 0.0f : 1.0f;
    }

    /**
     * Returns composite MW element based on the specified body part.
     *
     * @param bodyPart the body part
     * @return composite MW element based on the specified body part
     */
    public long getElement(ObjectBodyPart bodyPart) {
        return data[bodyPart.index()];
    }
}
