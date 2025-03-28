package mcdr.preprocessing.transformation.impl;

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
public class NormalizationOfPositionConvertor<O extends SequenceMocap<?>> extends SequenceMocapConvertor<O> {

    // decides whether to move all the poses according to the fixed shift calculated in the first pose, or center each pose independently so that the root has position [0, 0, 0]
    private final boolean centerByFirstPoseOnly;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link NormalizationOfPositionConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @throws messif.utility.reflection.NoSuchInstantiatorException
     */
    public NormalizationOfPositionConvertor(Class<O> sequenceClass) throws NoSuchInstantiatorException {
        super(sequenceClass);
        this.centerByFirstPoseOnly = false;
    }

    /**
     * Creates a new instance of {@link NormalizationOfPositionConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @param centerByFirstPoseOnly decides whether to move all the poses
     * according to the fixed shift calculated in the first pose, or center each
     * pose independently so that the root has position [0, 0, 0]
     * @throws messif.utility.reflection.NoSuchInstantiatorException
     */
    public NormalizationOfPositionConvertor(Class<O> sequenceClass, boolean centerByFirstPoseOnly) throws NoSuchInstantiatorException {
        super(sequenceClass);
        this.centerByFirstPoseOnly = centerByFirstPoseOnly;
    }

    //************ Implemented interface Convertor ************//
    /**
     * Centers the skeleton in each pose by moving the root to [0, 0, 0].
     *
     * @param sequence to be normalized
     * @return normalized sequence
     */
    @Override
    public O convert(O sequence) {
        O ts = (O) sequence.duplicate();

        float rootX = Float.NaN;
        float rootY = Float.NaN;
        float rootZ = Float.NaN;
        for (int i = 0; i < ts.getObjectCount(); i++) {
            ObjectMocapPose o = ts.getObject(i);

            // Pose normalization - moves the root joint to [0, 0, 0]
            float[][] coords = o.getJointCoordinates();
            if (i == 0 || !centerByFirstPoseOnly) {
                rootX = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_ROOT_ID)][0];
                rootY = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_ROOT_ID)][1];
                rootZ = coords[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_ROOT_ID)][2];
            }
            for (float[] coord : coords) {
                coord[0] = coord[0] - rootX;
                coord[1] = coord[1] - rootY;
                coord[2] = coord[2] - rootZ;
            }

        }
        return ts;
    }
}
