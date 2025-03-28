package mcdr.objects.impl;

import mcda.commons.constants.LandmarkConstant;
import mcdr.objects.ObjectMocapPose;
import messif.objects.LocalAbstractObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serial;
import java.util.Set;

import static mcdr.objects.impl.BodyPartConfiguration.JOINT_DIM;

/**
 * Based on {@link ObjectMocapPoseCoordsL2} but only a subset of joints is used for the L2 distance computation.
 * 
 * @author David Procházka
 */
public class ObjectMocapPoseCoordsL2Filtered extends ObjectMocapPoseCoordsL2 {

    @Serial
    private static final long serialVersionUID = 42L;

    /**
     * Set of joint IDs used during the L2 distance computation.
     */
    public static Set<Integer> jointIds;

    /**
     * Creates a new instance of {@link ObjectMocapPoseCoordsL2Filtered}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     */
    public ObjectMocapPoseCoordsL2Filtered(float[][] jointCoordinates) {
        super(jointCoordinates);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseCoordsL2Filtered} loaded from stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseCoordsL2Filtered(BufferedReader stream) throws IOException {
        super(stream);
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        float[][] jointCoordinates = getJointCoordinates();
        float[][] otherJointCoordinates = ((ObjectMocapPose) obj).getJointCoordinates();

        // L2 distance computation only with specified joints
        float sum = 0.0f;

        for (var jointId : jointIds) {
            var jointIndex = LandmarkConstant.getLandmarkPos(jointId);

            for (int axis = 0; axis < JOINT_DIM; axis++) {
                sum += Math.pow(jointCoordinates[jointIndex][axis] - otherJointCoordinates[jointIndex][axis], 2f);
            }
        }

        return (float) Math.sqrt(sum);
    }
}
