package mcdr.preprocessing.transformation.impl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import mcda.commons.constants.LandmarkConstant;
import mcdr.objects.ObjectMocapPose;
import static mcdr.preprocessing.transformation.impl.MotionImageConvertor.DEFAULT_IMAGE_HEIGHT;
import mcdr.sequence.KinematicTree;
import mcdr.sequence.SequenceMocap;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MotionImageConvertorWithBones extends MotionImageConvertor {

    protected static final List<Map.Entry<Integer, Integer>> ORDERED_BONE_LIST = Collections.unmodifiableList(new ArrayList<Map.Entry<Integer, Integer>>() {
        {
            // left leg
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LTOES_ID, LandmarkConstant.LANDMARK_LFOOT_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LFOOT_ID, LandmarkConstant.LANDMARK_LTIBIA_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LTIBIA_ID, LandmarkConstant.LANDMARK_LFEMUR_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LFEMUR_ID, LandmarkConstant.LANDMARK_LHIPJOINT_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHIPJOINT_ID, LandmarkConstant.LANDMARK_ROOT_ID));
            // right leg
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_RHIPJOINT_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHIPJOINT_ID, LandmarkConstant.LANDMARK_RFEMUR_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RFEMUR_ID, LandmarkConstant.LANDMARK_RTIBIA_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RTIBIA_ID, LandmarkConstant.LANDMARK_RFOOT_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RFOOT_ID, LandmarkConstant.LANDMARK_RTOES_ID));
            // torso + head
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_LOWERBACK_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERBACK_ID, LandmarkConstant.LANDMARK_UPPERBACK_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_UPPERBACK_ID, LandmarkConstant.LANDMARK_THORAX_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_UPPERNECK_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_UPPERNECK_ID, LandmarkConstant.LANDMARK_HEAD_ID));
            // left hand
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LFINGERS_ID, LandmarkConstant.LANDMARK_LHAND_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LTHUMB_ID, LandmarkConstant.LANDMARK_LHAND_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHAND_ID, LandmarkConstant.LANDMARK_LWRIST_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LWRIST_ID, LandmarkConstant.LANDMARK_LRADIUS_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LRADIUS_ID, LandmarkConstant.LANDMARK_LHUMERUS_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHUMERUS_ID, LandmarkConstant.LANDMARK_LCLAVICLE_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LCLAVICLE_ID, LandmarkConstant.LANDMARK_THORAX_ID));
            // right hand
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_RCLAVICLE_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RCLAVICLE_ID, LandmarkConstant.LANDMARK_RHUMERUS_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHUMERUS_ID, LandmarkConstant.LANDMARK_RRADIUS_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RRADIUS_ID, LandmarkConstant.LANDMARK_RWRIST_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RWRIST_ID, LandmarkConstant.LANDMARK_RHAND_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHAND_ID, LandmarkConstant.LANDMARK_RTHUMB_ID));
            add(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHAND_ID, LandmarkConstant.LANDMARK_RFINGERS_ID));
        }
    });
    protected static final int[] ORIGINAL_JOINT_HEIGHTS;
    protected static final int TOTAL_ORIGINAL_JOINT_HEIGHT;
    protected static final int MIN_BONE_POINT_COUNT = 0;

    static {
        ORIGINAL_JOINT_HEIGHTS = new int[LandmarkConstant.LANDMARK_COUNT];
        Arrays.fill(ORIGINAL_JOINT_HEIGHTS, 1);
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_ROOT_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHIPJOINT_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LFEMUR_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LTIBIA_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LFOOT_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LTOES_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHIPJOINT_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RFEMUR_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RTIBIA_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RFOOT_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RTOES_ID)] = 10;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LOWERBACK_ID)] = 2;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_UPPERBACK_ID)] = 2;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_THORAX_ID)] = 2;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LOWERNECK_ID)] = 2;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_UPPERNECK_ID)] = 2;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_HEAD_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LCLAVICLE_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHUMERUS_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LRADIUS_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LWRIST_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHAND_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LFINGERS_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LTHUMB_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RCLAVICLE_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHUMERUS_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RRADIUS_ID)] = 3;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RWRIST_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHAND_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RFINGERS_ID)] = 1;
//        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RTHUMB_ID)] = 1;

        int originalJointHeight = 0;
        for (int i = 0; i < ORIGINAL_JOINT_HEIGHTS.length; i++) {
            originalJointHeight += ORIGINAL_JOINT_HEIGHTS[i];
        }
        TOTAL_ORIGINAL_JOINT_HEIGHT = originalJointHeight;
    }

    private final List<Map.Entry<Map.Entry<Integer, Integer>, Integer>> bonePointCountList = new ArrayList<>(ORDERED_BONE_LIST.size());
    private final int[] jointCubeIndexes = new int[LandmarkConstant.LANDMARK_COUNT];
    private int[] pointHeights;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link MotionImageConvertorWithBones}.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     * @param boneLengthMap average bone lengths
     */
    public MotionImageConvertorWithBones(float originalMinCoordValue, float originalMaxCoordValue, Map<Map.Entry<Integer, Integer>, Float> boneLengthMap) {
        super(originalMinCoordValue, originalMaxCoordValue);
        setupBoneLengths(boneLengthMap);
    }

    /**
     * Creates a new instance of {@link MotionImageConvertorWithBones} that
     * computes extremal joint coordinate values independently for each
     * sequence.
     *
     * @param extremalCoordsSequenceJoints indicates whether extremal coordinate
     * values of sequence are computed for each joint independently
     * @param extremalCoordsSequenceAxis indicates whether extremal coordinate
     * values of sequence are computed for each axis independently
     * @param fixedImageWidth fixed image width - shorter movements are
     * prolonged by "white spaces", longer movements are truncated (if it is set
     * to null, the image width is set to the movement length)
     * @param initialRandomShift indicates whether the beginning movement
     * position is shifted about a random number of frames (the fixedImageWidth
     * parameter must be set to a not-null value)
     * @param boneLengthMap average bone lengths
     */
    public MotionImageConvertorWithBones(boolean extremalCoordsSequenceJoints, boolean extremalCoordsSequenceAxis, Integer fixedImageWidth, boolean initialRandomShift, Map<Map.Entry<Integer, Integer>, Float> boneLengthMap) {
        super(extremalCoordsSequenceJoints, extremalCoordsSequenceAxis, fixedImageWidth, initialRandomShift);
        setupBoneLengths(boneLengthMap);
    }

    /**
     * Initializes structures keeping information about bones.
     *
     * @param boneLengthMap average bone lengths
     */
    private void setupBoneLengths(Map<Map.Entry<Integer, Integer>, Float> boneLengthMap) {
        float remainingBoneLength = 0f;
        for (Map.Entry<Integer, Integer> boneJointPair : ORDERED_BONE_LIST) {
            remainingBoneLength += KinematicTree.getBoneLength(boneLengthMap, boneJointPair);
        }
        int remainingPointCount = DEFAULT_IMAGE_HEIGHT - TOTAL_ORIGINAL_JOINT_HEIGHT - (MIN_BONE_POINT_COUNT * ORDERED_BONE_LIST.size());

        int totalBonePointCount = 0;
        for (Map.Entry<Integer, Integer> boneJointPair : ORDERED_BONE_LIST) {
            float boneLength = KinematicTree.getBoneLength(boneLengthMap, boneJointPair);
            int bonePointCount = (int) Math.floor((boneLength / remainingBoneLength) * remainingPointCount);
            bonePointCountList.add(new AbstractMap.SimpleEntry<>(boneJointPair, bonePointCount + MIN_BONE_POINT_COUNT));
            remainingBoneLength -= boneLength;
            remainingPointCount -= bonePointCount;
            totalBonePointCount += bonePointCount + MIN_BONE_POINT_COUNT;
        }
        // Calculates indexes of original joints within a motion image
        Arrays.fill(jointCubeIndexes, Integer.MIN_VALUE);
        int index = 0;
        for (Map.Entry<Map.Entry<Integer, Integer>, Integer> bonePointCountEntry : bonePointCountList) {
            int joint1Idx = LandmarkConstant.getLandmarkPos(bonePointCountEntry.getKey().getKey());
            int joint2Idx = LandmarkConstant.getLandmarkPos(bonePointCountEntry.getKey().getValue());
            if (jointCubeIndexes[joint1Idx] == Integer.MIN_VALUE) {
                jointCubeIndexes[joint1Idx] = index;
                index++;
            }
            index += bonePointCountEntry.getValue();
            if (jointCubeIndexes[joint2Idx] == Integer.MIN_VALUE) {
                jointCubeIndexes[joint2Idx] = index;
                index++;
            }
        }

        // Assigns heights of bone points (including joints) within a motion image
        pointHeights = new int[LandmarkConstant.LANDMARK_COUNT + totalBonePointCount];
        Arrays.fill(pointHeights, 1);
        for (int i = 0; i < LandmarkConstant.LANDMARK_COUNT; i++) {
            pointHeights[jointCubeIndexes[i]] = ORIGINAL_JOINT_HEIGHTS[i];
        }
    }

    //************ Overrided class MotionImageConvertor ************//
    @Override
    protected float[][] quantizePoseCoords(ObjectMocapPose pose, float[][][] sequenceExtremalJointCoordAxisValues) {

        // Quantizies original joint coordinats
        float[][] quantizedPoseCoords = super.quantizePoseCoords(pose, sequenceExtremalJointCoordAxisValues);

        // Quantized coordinates of all points including joints
        float[][] cubeCoords = new float[pointHeights.length][3];

        // Quantizies bone point coordinats and creates a stripe
        int pointIdx = 0;
        for (Map.Entry<Map.Entry<Integer, Integer>, Integer> bonePointCountEntry : bonePointCountList) {
            int joint1 = LandmarkConstant.getLandmarkPos(bonePointCountEntry.getKey().getKey());
            int joint2 = LandmarkConstant.getLandmarkPos(bonePointCountEntry.getKey().getValue());

            // Source bone joint
            if (jointCubeIndexes[joint1] == pointIdx) {
                cubeCoords[pointIdx] = quantizedPoseCoords[joint1];
                pointIdx++;
            }

            // Bone points
            for (int i = 0; i < bonePointCountEntry.getValue(); i++) {
                for (int j = 0; j < 3; j++) {
                    float diff = (quantizedPoseCoords[joint2][j] - quantizedPoseCoords[joint1][j]) / (bonePointCountEntry.getValue() + 1f);
                    cubeCoords[pointIdx][j] = quantizedPoseCoords[joint1][j] + diff * (i + 1);
                }
                pointIdx++;
            }

            // Target bone joint
            if (jointCubeIndexes[joint2] == pointIdx) {
                cubeCoords[pointIdx] = quantizedPoseCoords[joint2];
                pointIdx++;
            }
        }

        return cubeCoords;
    }

    @Override
    protected int[] getJointHeights(SequenceMocap<?> sequence) {
        return pointHeights;
    }
}
