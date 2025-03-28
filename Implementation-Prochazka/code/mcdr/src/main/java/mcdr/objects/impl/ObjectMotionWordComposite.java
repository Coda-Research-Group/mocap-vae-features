package mcdr.objects.impl;

import messif.objects.LocalAbstractObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Composite MW. 
 * The position of a body part inside Composite MW and 
 * the mapping of joints to body parts is specified in {@link BodyPartConfiguration}.
 * 
 * @author David Procházka
 */
public class ObjectMotionWordComposite extends ObjectMotionWord {

    @Serial
    private static final long serialVersionUID = 42L;

    public static BodyPartConfiguration bodyPartConfiguration;

    /**
     * Creates a new instance.
     *
     * @param data the data content of the new object
     */
    public ObjectMotionWordComposite(long[] data) {
        super(data);
    }

    /**
     * Creates a new instance.
     *
     * @param locatorURI the locator URI for the new object
     * @param data       the data content of the new object
     */
    public ObjectMotionWordComposite(String locatorURI, long[] data) {
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
    public ObjectMotionWordComposite(BufferedReader stream) throws EOFException, IOException {
        super(stream);
    }

    public static boolean matchAtLeastTwo(ObjectMotionWordComposite lhs, ObjectMotionWordComposite rhs) {
        return matchAtLeast(2, lhs, rhs);
    }

    private static boolean matchAtLeast(int numberOfMatches, ObjectMotionWordComposite lhs, ObjectMotionWordComposite rhs) {
        return getBodyParts().stream()
                             .filter(bodyPart -> bodyPart.match(lhs, rhs))
                             .count() >= numberOfMatches;
    }

    public static BiPredicate<ObjectMotionWordComposite, ObjectMotionWordComposite> matchAtLeastOne(List<ObjectBodyPart> bodyParts) {
        return (lhs, rhs) -> bodyParts.stream()
                                      .anyMatch(bodyPart -> bodyPart.match(lhs, rhs));
    }

    /**
     * Returns the body part containing the specified joint index.
     *
     * @param jointIndex the joint index
     * @return the body part containing the specified joint index
     */
    public static ObjectBodyPart getBodyPart(int jointIndex) {
        int jointID = jointIndex + 1;

        return getBodyParts().stream()
                             .filter(bodyPart -> bodyPart.jointIds()
                                                         .contains(jointID))
                             .findFirst()
                             .orElseThrow(() -> new IllegalArgumentException("Body part with joint index " + jointIndex + " does not exist."));
    }

    /**
     * Returns pose dimensionality.
     *
     * @return pose dimensionality
     */
    public static int getPoseDimensionality() {
        return getJointCount() * BodyPartConfiguration.JOINT_DIM;
    }

    /**
     * Returns number of joints.
     *
     * @return number of joints
     */
    public static int getJointCount() {
        return bodyPartConfiguration.getJointCount();
    }

    /**
     * Returns set of all body parts.
     *
     * @return set of all body parts
     */
    public static Set<ObjectBodyPart> getBodyParts() {
        return bodyPartConfiguration.getBodyParts();
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject lao, float f) {
        return matchAtLeastTwo(this, (ObjectMotionWordComposite) lao) ? 0.0f : 1.0f;
    }
}
