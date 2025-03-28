package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import mcdr.objects.ObjectMocapPoseAnglesVels;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMocapPoseAnglesVelsL1 extends ObjectMocapPoseAnglesVels {

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesVelsL1}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     * @param angles angle descriptors extracted from the captured joint
     * coordinates
     * @param velocities absolute velocities extracted for specific joints
     */
    public ObjectMocapPoseAnglesVelsL1(float[][] jointCoordinates, float[] angles, float[] velocities) {
        super(jointCoordinates, angles, velocities);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesVelsL1} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseAnglesVelsL1(BufferedReader stream) throws IOException {
        super(stream);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's angles
        float[] objVelocities = ((ObjectMocapPoseAnglesVelsL1) obj).velocities;

        // The number of dimensions must be the same
        if (objVelocities.length != velocities.length) {
            return MAX_DISTANCE;
        }

        // L1 distance computation
        float rtv = 0f;
        for (int i = 0; i < velocities.length; i++) {
            rtv += Math.abs(velocities[i] - objVelocities[i]);
        }
        return rtv;
    }

    @Override
    public float getMaxDistance() {
        return velocities.length * 1000;
    }
}
