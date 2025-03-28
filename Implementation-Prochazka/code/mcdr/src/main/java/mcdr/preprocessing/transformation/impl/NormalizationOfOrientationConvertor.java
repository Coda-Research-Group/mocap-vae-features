package mcdr.preprocessing.transformation.impl;

import java.util.ArrayList;
import java.util.List;
import mcda.commons.constants.LandmarkConstant;
import mcdr.objects.ObjectMocapPose;
import mcdr.sequence.SequenceMocap;
import messif.utility.reflection.NoSuchInstantiatorException;
import mcdr.preprocessing.transformation.SequenceMocapConvertor;

/**
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class NormalizationOfOrientationConvertor<O extends SequenceMocap<?>> extends SequenceMocapConvertor<O> {

    // decides whether to rotate all the poses according to the fixed angle calculated in the first pose, or rotate each pose independently so that they face a fixed direction
    private final boolean rotateByFirstPoseOnly;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link NormalizationOfPositionConvertor}.
     *
     * @param rotateByFirstPoseOnly decides whether to rotate all the poses
     * according to the fixed angle calculated in the first pose, or rotate each
     * pose independently so that they face a fixed direction
     * @param sequenceClass class of the sequence
     * @throws messif.utility.reflection.NoSuchInstantiatorException
     */
    public NormalizationOfOrientationConvertor(Class<O> sequenceClass, boolean rotateByFirstPoseOnly) throws NoSuchInstantiatorException {
        super(sequenceClass);
        this.rotateByFirstPoseOnly = rotateByFirstPoseOnly;
    }

    //************ Methods ************//
    /**
     * Returns the angle according to which the skeleton has to be rotated so
     * that the subject faces a fixed position.
     *
     * @param o pose in which the rotation angle is calculated
     * @return the angle according to which the skeleton has to be rotated so
     * that the subject faces a fixed position
     */
    public float getHipsRotationAngle(ObjectMocapPose o) {
        float[][] coords = o.getJointCoordinates();
        float leftHipX = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHIPJOINT_ID)][0];
        float leftHipZ = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHIPJOINT_ID)][2];
        float rightHipX = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHIPJOINT_ID)][0];
        float rightHipZ = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHIPJOINT_ID)][2];
        return (leftHipZ - rightHipZ == 0f) ? 0f : (float) Math.atan((leftHipX - rightHipX) / (leftHipZ - rightHipZ));
    }

    /**
     * Rotates the pose according to the given angle around the Y-axis.
     *
     * @param o pose to be rotated
     * @param angle the angle according to which the skeleton is rotated
     */
    public void rotatePoseByY(ObjectMocapPose o, float angle) {
        float cosPhi = (float) Math.cos(angle);
        float sinPhi = (float) Math.sin(angle);
        for (float[] coord : o.getJointCoordinates()) {
            float newX = cosPhi * coord[0] - sinPhi * coord[2];
            float newZ = sinPhi * coord[0] + cosPhi * coord[2];
            coord[0] = newX;
            coord[2] = newZ;
        }
    }

    /**
     * Rotates the skeleton in each pose by a specified angle.
     *
     * @param sequence sequence to be normalized
     * @param angle angle of rotation
     * @return normalized sequence
     */
    public O rotateByAngle(O sequence, float angle) {
        O ts = (O) sequence.duplicate();
        for (int i = 0; i < ts.getObjectCount(); i++) {
            ObjectMocapPose o = ts.getObject(i);
            rotatePoseByY(o, angle);
        }
        return ts;
    }

    /**
     * Rotates the skeleton in each pose by a specified angle.
     *
     * @param poses list of poses to be normalized
     * @param angle angle of rotation
     * @return normalized poses
     */
    public List<? extends ObjectMocapPose> rotateByAngle(List<? extends ObjectMocapPose> poses, float angle) {
        List<ObjectMocapPose> rtv = new ArrayList<>(poses.size());
        for (ObjectMocapPose pose : poses) {
            ObjectMocapPose o = pose.duplicate();
            rotatePoseByY(o, angle);
            rtv.add(o);
        }
        return rtv;
    }

    /**
     * Aligns the rotated pose.
     *
     * @param o rotated pose
     */
    private void alignRotatedPose(ObjectMocapPose o) {
        float[][] coords = o.getJointCoordinates();

        // We want left hip to be positive z coordinate and right hip negative
        if (!rotateByFirstPoseOnly && coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHIPJOINT_ID)][2] > coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHIPJOINT_ID)][2]) {
            rotatePoseByY(o, (float) Math.PI);
        }

        // Side view
        rotatePoseByY(o, (float) Math.PI / 2);
    }

    //************ Implemented interface Convertor ************//
    /**
     * Rotates the skeleton so that it faces a fixed angle.
     *
     * @param sequence to be normalized
     * @return normalized sequence
     */
    @Override
    public O convert(O sequence) {
        O ts = (O) sequence.duplicate();

        float phi = Float.NaN;
        for (int i = 0; i < ts.getObjectCount(); i++) {
            ObjectMocapPose o = ts.getObject(i);

            // Computes the rotation angle
            if (!rotateByFirstPoseOnly || i == 0) {
                phi = getHipsRotationAngle(o);
            }

            // Rotates the pose
            rotatePoseByY(o, phi);

            alignRotatedPose(o);
        }
        return ts;
    }
}
