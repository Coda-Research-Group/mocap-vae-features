package mcdr.preprocessing.transformation.impl;

import java.util.AbstractMap;
import java.util.Map;
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
public class NormalizationOfSkeletonSize<O extends SequenceMocap<?>> extends SequenceMocapConvertor<O> {

    // skeleton proportions to be set (key denotes the pair of Ids of joints determining the specific bone and value represents the target bone length)
    private final Map<Map.Entry<Integer, Integer>, Float> boneLengthMap;
    // kinematic model of the human body (key denotes the joint Id and value represents Ids of descendant joints)
    private final Map<Integer, int[]> kinematicTree;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link NormalizationOfPositionConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @param boneLengthMap skeleton proportions to be set (key denotes the pair
     * of Ids of joints determining the specific bone and value represents the
     * target bone length)
     * @param kinematicTree kinematic model of the human body (key denotes the
     * joint Id and value represents Ids of descendant joints)
     * @throws messif.utility.reflection.NoSuchInstantiatorException
     */
    public NormalizationOfSkeletonSize(Class<O> sequenceClass, Map<Map.Entry<Integer, Integer>, Float> boneLengthMap, Map<Integer, int[]> kinematicTree) throws NoSuchInstantiatorException {
        super(sequenceClass);
        this.boneLengthMap = boneLengthMap;
        this.kinematicTree = kinematicTree;
    }

    //************ Methods ************//
    private void adjustJointLocations(ObjectMocapPose o, int parentJoint, int childJoint, Map<Map.Entry<Integer, Integer>, Float> boneLengthMap, Map<Integer, int[]> kinematicTree) {
        float[][] coords = o.getJointCoordinates();
        int parentJointPos = LandmarkConstant.getLandmarkPos(parentJoint);
        int childJointPos = LandmarkConstant.getLandmarkPos(childJoint);

        // Computes the ratio between the original and supplied bone length
        float averageBoneLength = boneLengthMap.get(new AbstractMap.SimpleEntry<>(parentJoint, childJoint));
        float originalBoneLength = (float) Math.sqrt(
                Math.pow(coords[parentJointPos][0] - coords[childJointPos][0], 2)
                + Math.pow(coords[parentJointPos][1] - coords[childJointPos][1], 2)
                + Math.pow(coords[parentJointPos][2] - coords[childJointPos][2], 2)
        );
        float boneLengthRatio = (originalBoneLength == 0f) ? 0f : averageBoneLength / originalBoneLength;

        // Adjusts the joint coordinates of the child joint
        float childJointNewX = coords[parentJointPos][0] + boneLengthRatio * (coords[childJointPos][0] - coords[parentJointPos][0]);
        float childJointNewY = coords[parentJointPos][1] + boneLengthRatio * (coords[childJointPos][1] - coords[parentJointPos][1]);
        float childJointNewZ = coords[parentJointPos][2] + boneLengthRatio * (coords[childJointPos][2] - coords[parentJointPos][2]);

        // Computes the adjustment difference (to shift children joints)
        float diffX = childJointNewX - coords[childJointPos][0];
        float diffY = childJointNewY - coords[childJointPos][1];
        float diffZ = childJointNewZ - coords[childJointPos][2];

        // Assigns new coordinates
        coords[childJointPos][0] = childJointNewX;
        coords[childJointPos][1] = childJointNewY;
        coords[childJointPos][2] = childJointNewZ;

        // Shifts coordinates of children joints
        adjustChildJointLocations(o, childJoint, diffX, diffY, diffZ, kinematicTree);

        // Recursive call to children
        int[] childChildJoints = kinematicTree.get(childJoint);
        if (childChildJoints != null) {
            for (int childChildJoint : childChildJoints) {
                adjustJointLocations(o, childJoint, childChildJoint, boneLengthMap, kinematicTree);
            }
        }
    }

    private void adjustChildJointLocations(ObjectMocapPose o, int parentJoint, float diffX, float diffY, float diffZ, Map<Integer, int[]> kinematicTree) {
        float[][] coords = o.getJointCoordinates();
        int[] childJoints = kinematicTree.get(parentJoint);
        if (childJoints != null) {
            for (int childJoint : childJoints) {
                int childJointPos = LandmarkConstant.getLandmarkPos(childJoint);
                coords[childJointPos][0] = coords[childJointPos][0] + diffX;
                coords[childJointPos][1] = coords[childJointPos][1] + diffY;
                coords[childJointPos][2] = coords[childJointPos][2] + diffZ;
                adjustChildJointLocations(o, childJoint, diffX, diffY, diffZ, kinematicTree);
            }
        }
    }

    //************ Implemented interface Convertor ************//
    /**
     * Normalizes skeleton proportions in each pose by replacing the
     * specific-sized skeleton.
     *
     * @param sequence to be normalized
     * @return normalized sequence
     */
    @Override
    public O convert(O sequence) {
        O ts = (O) sequence.duplicate();

        for (int i = 0; i < ts.getObjectCount(); i++) {
            ObjectMocapPose o = ts.getObject(i);
            for (int childJoint : kinematicTree.get(LandmarkConstant.LANDMARK_ROOT_ID)) {
                adjustJointLocations(o, LandmarkConstant.LANDMARK_ROOT_ID, childJoint, boneLengthMap, kinematicTree);
            }
        }
        return ts;
    }
}
