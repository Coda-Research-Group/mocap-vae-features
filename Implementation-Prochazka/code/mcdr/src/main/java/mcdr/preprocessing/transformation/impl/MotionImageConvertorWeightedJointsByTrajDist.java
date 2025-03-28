package mcdr.preprocessing.transformation.impl;

import java.util.Arrays;
import mcda.commons.constants.LandmarkConstant;
import mcdr.sequence.SequenceMocap;
import mcdr.test.utils.SequenceMocapMgmt;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MotionImageConvertorWeightedJointsByTrajDist extends MotionImageConvertor {

    // minimal height of a single joint in number of pixels
    private final int minJointHeight;
    // precomputed heights of joints over all dataset sequences
    private final int[] jointHeights;

    //************ Constructors ************//
    /**
     * Creates a new instance of
     * {@link MotionImageConvertorWeightedJointsByTrajDist}. The joints are
     * weighted locally based on a single sequence only.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     * @param minJointHeight minimal height of a single joint in number of
     * pixels
     */
    public MotionImageConvertorWeightedJointsByTrajDist(float originalMinCoordValue, float originalMaxCoordValue, int minJointHeight) {
        super(originalMinCoordValue, originalMaxCoordValue, null, false);
        this.minJointHeight = minJointHeight;
        this.jointHeights = null;
    }

    /**
     * Creates a new instance of
     * {@link MotionImageConvertorWeightedJointsByTrajDist}. The joints are
     * weighted globally over all dataset sequences.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     * @param minJointHeight minimal height of a single joint in number of
     * pixels
     * @param sequenceMgmt sequences from which weights of joints are determined
     *
     */
    public MotionImageConvertorWeightedJointsByTrajDist(float originalMinCoordValue, float originalMaxCoordValue, int minJointHeight, SequenceMocapMgmt sequenceMgmt) {
        super(originalMinCoordValue, originalMaxCoordValue, null, false);
        this.minJointHeight = minJointHeight;
        this.jointHeights = new int[LandmarkConstant.LANDMARK_COUNT];
        float[] jointTrajDists = new float[LandmarkConstant.LANDMARK_COUNT];
        Arrays.fill(jointTrajDists, 0f);
        float totalJointTrajDist = 0f;
        for (SequenceMocap<?> sequence : sequenceMgmt.getSequences()) {
            for (int jointIdx = 0; jointIdx < jointTrajDists.length; jointIdx++) {
                float trajLength = sequence.computeJointTrajectoryDist(jointIdx);
                jointTrajDists[jointIdx] += trajLength;
                totalJointTrajDist += trajLength;
            }
        }
        System.out.println("Joint weights:");
        for (int jointIdx = 0; jointIdx < jointTrajDists.length; jointIdx++) {
            this.jointHeights[jointIdx] = (int) Math.floor(minJointHeight + (DEFAULT_IMAGE_HEIGHT - minJointHeight * this.jointHeights.length) * jointTrajDists[jointIdx] / totalJointTrajDist);
            System.out.println("  " + this.jointHeights[jointIdx]);
        }
    }

    //************ Overrided class MotionImageConvertor ************//
    @Override
    protected int[] getJointHeights(SequenceMocap<?> sequence) {
        if (jointHeights != null) {
            return jointHeights;
        }

        int[] sequenceJointHeights = new int[sequence.getJointCount()];
        float[] jointTrajDists = new float[sequence.getJointCount()];
        float totalJointTrajDist = 0f;
        for (int jointIdx = 0; jointIdx < jointTrajDists.length; jointIdx++) {
            jointTrajDists[jointIdx] = sequence.computeJointTrajectoryDist(jointIdx);
            totalJointTrajDist += jointTrajDists[jointIdx];
        }
        System.out.println("Joint heights (" + sequence.getLocatorURI() + "):");
        for (int jointIdx = 0; jointIdx < jointTrajDists.length; jointIdx++) {
            sequenceJointHeights[jointIdx] = (int) Math.floor(minJointHeight + (DEFAULT_IMAGE_HEIGHT - minJointHeight * sequenceJointHeights.length) * jointTrajDists[jointIdx] / totalJointTrajDist);
            System.out.println("  " + jointIdx + ": " + sequenceJointHeights[jointIdx]);
        }
        return sequenceJointHeights;
    }
}
