package mcdr.test;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor;
import mcdr.sequence.SequenceMocap;
import mcdr.preprocessing.segmentation.impl.RegularSegmentConvertor;
import mcdr.preprocessing.transformation.impl.FPSConvertor;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFloatVectorL1;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.StreamGenericAbstractObjectIterator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapSegmentation {

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // params
        final boolean storeSegmentsIndividuallyToFile = true;
        final boolean normalizeInputSequences = false;
        final boolean extractSegmentFeatures = false;

        // HDM05
//        final int originalSegmentFPS = 120;
//        final int convertedSegmentFPS = 12;
//        final int segmentSize = 20;
//        final float segmentShiftRatio = 1.0f;
//        final int segmentShiftInitial = 0;
        // PKU-MMD
        final int originalSegmentFPS = 30;
        final int convertedSegmentFPS = 30;
        final int segmentSize = 20;
        final float segmentShiftRatio = 0.2f;
        final int segmentShiftInitial = 0;

        // data params
//        final Class<? extends SequenceMocap<?>> sequenceClass = SequenceMocapPoseAnglesL1CircleDTW.class;
        final Class<? extends SequenceMocap<?>> sequenceClass = SequenceMocapPoseCoordsL2DTW.class;
        final Class<? extends SequenceMocap<?>> sequenceClassStoreSegmentsIndividuallyToFile = null;

//        // HDM05
////        final String sequenceFile = "e:/datasets/mocap/hdm05/objects-annotations-specific-coords.data";
//        final String sequenceFile = "y:/datasets/mocap/hdm05/objects-annotations-specific-coords_normPOS.data";
//        final String sequenceFile = "e:/datasets/mocap/cmu/objects-sequences-coords_normPOS.data";
//        final String sequenceFile = "y:/datasets/mocap/hdm05/objects-sequences_annotated_specific-coords.data";
//        final String outputFeatureFile = "d:/temp/hdm05-annotations_specific-segment" + segmentSize + "_shift" + (segmentSize * segmentShiftRatio) + "_initialshift" + segmentShiftInitial + "-coords_normPOS-fps12" + ".data";
//        final String outputFeatureFile = "d:/temp/hdm05-annotations_specific-sequence_segment" + segmentSize + "_shift" + (segmentSize * segmentShiftRatio) + "_initialshift" + segmentShiftInitial + "-coords_normPOS-fps12" + ".data";
//        final String outputFeatureFile = "e:/datasets/mocap/cmu-sequences_segment" + segmentSize + "_shift" + (segmentSize * segmentShiftRatio) + "-coords_normPOS" + ".data";
        // PKU-MMD
//        final String sequenceFile = "y:/datasets/mocap/PKU-MMD/skeleton3D/actions/single-subject/actions-single-subject-all-POS.data";
//        final String outputFeatureFile = "d:/temp/PKUMMD-annotations_singlesubject-segment" + segmentSize + "_shift" + (segmentSize * segmentShiftRatio) + "_initialshift" + segmentShiftInitial + "-coords_normPOS-fps" + convertedSegmentFPS + ".data";
//        final String sequenceFile = "y:/datasets/mocap/PKU-MMD/skeleton3D/sequences/messif/sequences-single-subject-all-POS.data";
        final String sequenceFile = "y:/datasets/mocap/NTU/objects-annotations_filtered0.9GT-coords_nonorm.data";
        final String outputFeatureFile = "c:/temp/NTU-sequences_singlesubject-segment" + segmentSize + "_shift" + (segmentSize * segmentShiftRatio) + "_initialshift" + segmentShiftInitial + "-coords_normPOS-fps" + convertedSegmentFPS + ".data";

        final CaffeObjectMotionImageSocketExtractor caffeObjectExtractor = CaffeObjectMotionImageSocketExtractor.createHDM05Extractor(sequenceClass);

        // Transforming the sequences
        FileOutputStream fos = new FileOutputStream(outputFeatureFile);
        StreamGenericAbstractObjectIterator sequenceIterator = new StreamGenericAbstractObjectIterator<>(sequenceClass, sequenceFile);
        int sequenceCount = 0;
        float minCoordValue = Float.MAX_VALUE;
        float maxCoordValue = Float.MIN_VALUE;
        float minCoordValueConverted = Float.MAX_VALUE;
        float maxCoordValueConverted = Float.MIN_VALUE;
        float[][] extremalJointCoordValues = new float[31][2];
        for (int j = 0; j < extremalJointCoordValues.length; j++) {
            extremalJointCoordValues[j][0] = Float.MAX_VALUE;
            extremalJointCoordValues[j][1] = Float.MIN_VALUE;
        }
        int segmentCount = 0;
        while (sequenceIterator.hasNext()) {
            SequenceMocap<?> sequence = (SequenceMocap<?>) sequenceIterator.next();
            minCoordValue = Math.min(sequence.getExtremalCoordValue(true), minCoordValue);
            maxCoordValue = Math.max(sequence.getExtremalCoordValue(false), maxCoordValue);

            // Converts the sequence only if the segment feature extraction is needed
            SequenceMocap<?> sequenceConverted = sequence;
            if (normalizeInputSequences || extractSegmentFeatures) {
                sequenceConverted = caffeObjectExtractor.normalizeSequence(sequence.duplicate());
            }
            minCoordValueConverted = Math.min(sequenceConverted.getExtremalCoordValue(true), minCoordValueConverted);
            maxCoordValueConverted = Math.max(sequenceConverted.getExtremalCoordValue(false), maxCoordValueConverted);

            for (int j = 0; j < sequenceConverted.getJointCount(); j++) {
                extremalJointCoordValues[j][0] = Math.min(sequenceConverted.getExtremalJointCoordValue(true, j), extremalJointCoordValues[j][0]);
                extremalJointCoordValues[j][1] = Math.max(sequenceConverted.getExtremalJointCoordValue(false, j), extremalJointCoordValues[j][1]);
            }

            FPSConvertor fpsConvertor = new FPSConvertor(sequenceClass, originalSegmentFPS, convertedSegmentFPS);
            RegularSegmentConvertor segmentProcessor = new RegularSegmentConvertor<>(sequenceClass, segmentSize, segmentShiftRatio, segmentShiftInitial, false);
            List<SequenceMocap<?>> segments = segmentProcessor.convert(sequenceConverted);

            // If the sequence is too short for segmentation, the whole sequence is considered as a single segment
            if (segments.isEmpty()) {
                segments.add(sequenceConverted);
            }

            List<ObjectFloatVectorL1> poses = new ArrayList<>();
            segmentCount += segments.size();
            int seqSegmentNo = 0;
            for (SequenceMocap<?> segment : segments) {
                AbstractObjectKey segmentObjectKey = new AbstractObjectKey(sequence.getLocatorURI() + "_" + seqSegmentNo);
                LocalAbstractObject segmentObjectToStore = segment;

                // Segment feature extraction
                if (extractSegmentFeatures) {
                    segmentObjectToStore = caffeObjectExtractor.extractObject(caffeObjectExtractor.generateMotionImage(segment.duplicate()), segmentObjectKey);
                } else {
                    if (originalSegmentFPS != convertedSegmentFPS) {
                        segmentObjectToStore = fpsConvertor.convert(segment.duplicate());
                    }
                    segmentObjectToStore.setObjectKey(segmentObjectKey);
                }

                // Stores the segment
                if (storeSegmentsIndividuallyToFile) {
                    segmentObjectToStore.write(fos);
                } else {
                    poses.add((ObjectFloatVectorL1) segmentObjectToStore);
                }

                seqSegmentNo++;
            }

            if (!storeSegmentsIndividuallyToFile) {
                LocalAbstractObject newSequence = sequenceClassStoreSegmentsIndividuallyToFile.getConstructor(LocalAbstractObject[].class).newInstance(poses);
                newSequence.setObjectKey(sequence.getObjectKey());
                newSequence.write(fos);
            }
            sequenceCount++;
        }
        fos.close();
        System.out.println("Sequence count: " + sequenceCount + "; segment count: " + segmentCount);
        System.out.println("  minCoordValue: " + minCoordValue);
        System.out.println("  maxCoordValue: " + maxCoordValue);
        System.out.println("  minCoordValueConverted: " + minCoordValueConverted);
        System.out.println("  maxCoordValueConverted: " + maxCoordValueConverted);
        System.out.println("  minMaxJointCoordValuesConverted: ");
        for (int j = 0; j < extremalJointCoordValues.length; j++) {
            System.out.print("  minMaxJointCoordValuesConverted: " + Arrays.toString(extremalJointCoordValues[j]));
        }
        System.out.println();
    }
}
