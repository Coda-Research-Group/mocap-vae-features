package mcdr.test;

import java.io.File;
import javax.imageio.ImageIO;
import mcdr.preprocessing.segmentation.SegmentConvertor;
import mcdr.sequence.KinematicTree;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.impl.SequenceMocapPoseAnglesL1CircleDTW;
import mcdr.preprocessing.segmentation.impl.RegularSegmentConvertor;
import mcdr.preprocessing.transformation.impl.FPSConvertor;
import mcdr.preprocessing.transformation.impl.MotionImageConvertor;
import mcdr.preprocessing.transformation.impl.NormalizationOfOrientationConvertor;
import mcdr.preprocessing.transformation.impl.NormalizationOfPositionConvertor;
import mcdr.preprocessing.transformation.impl.NormalizationOfSkeletonSize;
import mcdr.test.utils.SequenceMocapMgmt;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapProcessor {

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // data params
        Class<? extends SequenceMocap<?>> sequenceClass = SequenceMocapPoseAnglesL1CircleDTW.class;
        final String sequenceFile = "d:/datasets/mocap/hdm05/objects-sequences-anglesext.data";
//        final String sequenceFile = "d:/datasets/mocap/hdm05/objects-annotations-specific-anglesext.data";
        final String outputImgFolder = "d:/datasets/mocap/hdm05/motion_images-segmentation/tmp/";

        // segmentation params
        int segmentSize = 45;
        float segmentShiftRatio = 1f;
        int initialSegmentShift = 0;
        boolean trimLastSegment = false;
        // normalization params
        final boolean rotateByFirstPoseOnly = false;
        // fps params
        final int originalFPSRate = 120;
        final int reducedFPSRate = 12;

        SequenceMocapMgmt sequenceMgmt = new SequenceMocapMgmt();
        NormalizationOfPositionConvertor normalizationOfPositionConvertor = new NormalizationOfPositionConvertor(sequenceClass);
        NormalizationOfOrientationConvertor normalizationOfOrientationConvertor = new NormalizationOfOrientationConvertor(sequenceClass, rotateByFirstPoseOnly);
        NormalizationOfSkeletonSize normalizationOfSkeletonSize = new NormalizationOfSkeletonSize(sequenceClass, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        FPSConvertor fpsConvertor = new FPSConvertor(sequenceClass, originalFPSRate, reducedFPSRate);

        // Reading sequences
        sequenceMgmt.read(sequenceClass, sequenceFile);

        // Decreasing a fps rate of the sequences
        sequenceMgmt.convert(fpsConvertor);

        // Normalizing the sequences
        sequenceMgmt.convert(normalizationOfPositionConvertor);
        sequenceMgmt.convert(normalizationOfOrientationConvertor);
        sequenceMgmt.convert(normalizationOfSkeletonSize);

        // Segmenting and transforming the sequences
        float minCoordValue = sequenceMgmt.getExtremalCoordValue(true);
        float maxCoordValue = sequenceMgmt.getExtremalCoordValue(false);
        SegmentConvertor<SequenceMocap<?>> segmentConvertor = new RegularSegmentConvertor(sequenceClass, segmentSize, segmentShiftRatio, initialSegmentShift, trimLastSegment);
        MotionImageConvertor imageConvertor = new MotionImageConvertor(minCoordValue, maxCoordValue);

        // Generates segments, converts them into images and stores them on the disk
        for (SequenceMocap<?> sequence : sequenceMgmt.getSequences()) {
            for (SequenceMocap<?> segment : segmentConvertor.convert(sequence)) {
                ImageIO.write(imageConvertor.convert(segment), "png", new File(outputImgFolder + segment.getLocatorURI() + ".png"));
            }
        }
    }
}
