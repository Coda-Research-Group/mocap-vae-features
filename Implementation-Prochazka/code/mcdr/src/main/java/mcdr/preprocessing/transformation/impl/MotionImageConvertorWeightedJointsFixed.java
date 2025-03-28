package mcdr.preprocessing.transformation.impl;

import mcda.commons.constants.LandmarkConstant;
import mcdr.sequence.SequenceMocap;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MotionImageConvertorWeightedJointsFixed extends MotionImageConvertor {

    // predefined joint heights
    public static final int[] ORIGINAL_JOINT_HEIGHTS = new int[LandmarkConstant.LANDMARK_COUNT];

    static {
        // legs146_hands_110
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_ROOT_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHIPJOINT_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LFEMUR_ID)] = 12;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LTIBIA_ID)] = 16;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LFOOT_ID)] = 20;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LTOES_ID)] = 20;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHIPJOINT_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RFEMUR_ID)] = 12;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RTIBIA_ID)] = 16;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RFOOT_ID)] = 20;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RTOES_ID)] = 20;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LOWERBACK_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_UPPERBACK_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_THORAX_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LOWERNECK_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_UPPERNECK_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_HEAD_ID)] = 0;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LCLAVICLE_ID)] = 3;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHUMERUS_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LRADIUS_ID)] = 7;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LWRIST_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LHAND_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LFINGERS_ID)] = 15;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_LTHUMB_ID)] = 15;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RCLAVICLE_ID)] = 3;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHUMERUS_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RRADIUS_ID)] = 7;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RWRIST_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RHAND_ID)] = 5;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RFINGERS_ID)] = 15;
        ORIGINAL_JOINT_HEIGHTS[LandmarkConstant.getLandmarkPos(LandmarkConstant.LANDMARK_RTHUMB_ID)] = 15;
    }

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link MotionImageConvertorWeightedJointsFixed}
     * with predefined weights of joints.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     */
    public MotionImageConvertorWeightedJointsFixed(float originalMinCoordValue, float originalMaxCoordValue) {
        super(originalMinCoordValue, originalMaxCoordValue, null, false);
    }

    //************ Overrided class MotionImageConvertor ************//
    @Override
    protected int[] getJointHeights(SequenceMocap<?> sequence) {
        return ORIGINAL_JOINT_HEIGHTS;
    }
}
