package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import mcda.commons.constants.LandmarkConstant;
import mcda.commons.math.VectMath;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author Jakub Valcik, xvalcik@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMocapPoseDtdsL1 extends ObjectMocapPoseCoordsL2 {

    private static final int[] LANDMARK_COUPLES = new int[]{
        LandmarkConstant.LANDMARK_COUPLE_LHIPJOINT_RFEMUR_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHIPJOINT_RTIBIA_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHIPJOINT_RFOOT_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHUMERUS_RHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHUMERUS_RHUMERUS_ID,
        LandmarkConstant.LANDMARK_COUPLE_RHIPJOINT_RTIBIA_ID,
        LandmarkConstant.LANDMARK_COUPLE_RHIPJOINT_RFOOT_ID,
        LandmarkConstant.LANDMARK_COUPLE_LCLAVICLE_RHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHAND_RHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHAND_RHUMERUS_ID,
        LandmarkConstant.LANDMARK_COUPLE_RFOOT_LHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_RFOOT_RHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_LFOOT_RHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_RHIPJOINT_RHAND_ID,
        LandmarkConstant.LANDMARK_COUPLE_LHIPJOINT_LHAND_ID};

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseCoordsL2}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     */
    public ObjectMocapPoseDtdsL1(float[][] jointCoordinates) {
        super(jointCoordinates);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseCoordsL2} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseDtdsL1(BufferedReader stream) throws IOException {
        super(stream);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {

        // Get access to the other object's coordinates
        ObjectMocapPoseDtdsL1 other = ((ObjectMocapPoseDtdsL1) obj);

        // L1 distance computation
        float dist = 0f;
        for (int couple : LANDMARK_COUPLES) {
            dist += Math.abs(jointDistance(couple) - other.jointDistance(couple));
        }
        return dist;
    }

    private float jointDistance(int landmarkCoupleId) {
        float[][] coords = getJointCoordinates();
        float[] j1 = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_COUPLE_MAP.get(landmarkCoupleId)[0])];
        float[] j2 = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_COUPLE_MAP.get(landmarkCoupleId)[1])];
        return VectMath.distance(j1, j2);
    }
}
