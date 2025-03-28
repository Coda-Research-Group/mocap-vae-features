package mcdr.preprocessing.transformation.impl;

import mcdr.sequence.SequenceMocap;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MotionImageConvertorWeightedJointsByCoordRange extends MotionImageConvertor {

    // minimal height of a single joint in number of pixels
    private final int minJointHeight;

    //************ Constructors ************//
    /**
     * Creates a new instance of
     * {@link MotionImageConvertorWeightedJointsByCoordRange}.
     *
     * @param extremalJointCoordAxisValues minimum/maximum coordinate values of
     * each joint and axis before quantization [joint, x/y/z axis,
     * minimum/maximum]
     * @param minJointHeight minimal height of a single joint in number of
     * pixels
     */
    public MotionImageConvertorWeightedJointsByCoordRange(float[][][] extremalJointCoordAxisValues, int minJointHeight) {
        super(extremalJointCoordAxisValues, null, false);
        this.minJointHeight = minJointHeight;
    }

    //************ Overrided class MotionImageConvertor ************//
    @Override
    protected int[] getJointHeights(SequenceMocap<?> sequence) {
        int[] jointHeights = new int[sequence.getJointCount()];
        float[][] jointRanges = new float[sequence.getJointCount()][2];
        float totalJointRange = 0f;
        for (int jointIdx = 0; jointIdx < extremalJointCoordAxisValues.length; jointIdx++) {
            jointRanges[jointIdx][0] = Float.MAX_VALUE;
            jointRanges[jointIdx][1] = Float.MIN_VALUE;
            for (int j = 0; j < 3; j++) {
                jointRanges[jointIdx][0] = Math.min(jointRanges[jointIdx][0], extremalJointCoordAxisValues[jointIdx][j][0]);
                jointRanges[jointIdx][1] = Math.max(jointRanges[jointIdx][1], extremalJointCoordAxisValues[jointIdx][j][1]);
            }
            totalJointRange += jointRanges[jointIdx][1] - jointRanges[jointIdx][0];
        }
        for (int jointIdx = 0; jointIdx < jointRanges.length; jointIdx++) {
            jointHeights[jointIdx] = (int) Math.floor(minJointHeight + (DEFAULT_IMAGE_HEIGHT - minJointHeight * jointHeights.length) * ((jointRanges[jointIdx][1] - jointRanges[jointIdx][0]) / totalJointRange));
        }
        return jointHeights;
    }
}
