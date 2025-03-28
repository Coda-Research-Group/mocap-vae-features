package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import mcdr.objects.ObjectMocapPoseAngles;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMocapPoseAnglesL2Circle extends ObjectMocapPoseAngles {

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesL2Circle}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     * @param angles angle descriptors extracted from the captured joint
     * coordinates
     */
    public ObjectMocapPoseAnglesL2Circle(float[][] jointCoordinates, float[] angles) {
        super(jointCoordinates, angles);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesL2Circle} loaded
     * from stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseAnglesL2Circle(BufferedReader stream) throws IOException {
        super(stream);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's angles
        float[] objAngles = ((ObjectMocapPoseAngles) obj).getAngles();

        // The number of dimensions must be the same
        if (objAngles.length != angles.length) {
            return MAX_DISTANCE;
        }

        // L2 circle distance computation
        float rtv = 0f;
        for (int i = 0; i < angles.length; i++) {
            float diff = Math.abs(angles[i] - objAngles[i]);
            diff = Math.min(diff, 360 - diff);
            rtv += ANGLES_WEIGHTS[i] * diff * diff;
        }
        return (float) Math.sqrt(rtv);
    }

    @Override
    public float getMaxDistance() {
        return angles.length * 180;
    }
}
