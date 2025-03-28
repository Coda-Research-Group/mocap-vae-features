package mcdr.test.mw;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import mcdr.metrics.AnnotationMetrics;
import mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor;
import static mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor.createSequenceConvertors;
import mcdr.objects.utils.OverlappingAnnotationRemovalCollection;
import mcdr.preprocessing.transformation.SequenceMocapConvertor;
import mcdr.sequence.KinematicTree;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.SequenceMotionWords;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import mcdr.sequence.impl.SequenceMotionWordsDTW;
import mcdr.test.utils.ObjectCategoryMgmt;
import mcdr.test.utils.ObjectMgmt;
import mcdr.test.utils.SequenceMocapMgmt;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.AbstractObjectKey;
import messif.operations.query.KNNQueryOperation;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SubseqSearchSequentialDTW {

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        AnnotationMetrics evaluator = new AnnotationMetrics();

        //************ Params ************//
        final Class<? extends SequenceMocap<?>> origSeqClass = SequenceMocapPoseCoordsL2DTW.class;
        final Class<? extends SequenceMotionWords<?>> mwClass = SequenceMotionWordsDTW.class;
        final String gtFile = "d:/experiments/mw_search/ground_truth-sequence_actions.txt";
        final String origSequenceFile = "e:/datasets/mocap/HDM05/objects-sequences_annotated_specific-coords.data";
        final String origQueryFile = "e:/datasets/mocap/HDM05/objects-annotations-specific-coords.data";
        final String mwQueryFile = "y:/datasets/mocap/hdm05/motion_words/quantized/hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-pivots-kmedoids-350.data";
        final String mwSequenceFile = "y:/datasets/mocap/hdm05/motion_words/quantized/sequences/hdm05-sequences_annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-kmedoids350.data";

        final int segmentSize = 80;
        final float segmentShiftRatio = 0.2f;

        final int maxK = 50;
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(origSeqClass, 120, 12, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);

        //************ Queries ************//
        ObjectCategoryMgmt categoryMgmt = new ObjectCategoryMgmt();
        ObjectMgmt queryMgmt = new ObjectMgmt(categoryMgmt);
        queryMgmt.read(mwClass, mwQueryFile);
//        queryMgmt.read(mwClass, mwQueryFile, "3136_104_280_238", null, null, null, false);
//        queryMgmt.storeRandomObjects(queryFile + ".sampledRand" + sampledQueriesPerCategory, sampledQueriesPerCategory);
        ObjectMgmt origQueryMgmt = new ObjectMgmt(categoryMgmt);
        origQueryMgmt.read(origSeqClass, origQueryFile);

        //************ Data sequences ************//
        SequenceMocapMgmt origSeqMgmt = new SequenceMocapMgmt();
        origSeqMgmt.read(origSeqClass, origSequenceFile);
        ObjectMgmt mwSeqMgmt = new ObjectMgmt(categoryMgmt);
        mwSeqMgmt.read(mwClass, mwSequenceFile);

        //************ Stream GT ************//
        Map<String, Map<String, BitSet>> sequencesGT = evaluator.parseAnnotationsHDM05(gtFile, origSeqMgmt);

        //************ Evaluation ************//
        long startTime = System.currentTimeMillis();
        List<KNNQueryOperation> ops = new ArrayList<>(queryMgmt.getObjectCount());

        for (LocalAbstractObject querySeq : queryMgmt.getObjects()) {
            System.out.println("Processing query: " + querySeq.getLocatorURI());
            int totalCandidateCount = 0;

            SequenceMotionWordsDTW mwQuerySeq = (SequenceMotionWordsDTW) querySeq;
            int mwQueryLength = mwQuerySeq.getSequenceLength();

            KNNQueryOperation op = new KNNQueryOperation(mwQuerySeq, maxK);
            op.setAnswerCollection(new OverlappingAnnotationRemovalCollection());
            ops.add(op);

            for (LocalAbstractObject dataSeq : mwSeqMgmt.getObjects()) {
                int seqCandidateCount = 0;
                SequenceMotionWordsDTW mwDataSeq = (SequenceMotionWordsDTW) dataSeq;
                String dataSeqId = ObjectMgmt.parseObjectParentSequenceId(mwDataSeq);
                int origDataSeqLength = origSeqMgmt.getSequence(dataSeqId).getSequenceLength();

                int mwI = 0;
                while (mwI + mwQueryLength <= mwDataSeq.getSequenceLength()) {
                    SequenceMotionWordsDTW candidate = new SequenceMotionWordsDTW(mwDataSeq.getSubsequenceData(mwI, mwI + mwQueryLength));

                    int candidateOffset = mwI * Math.round(segmentSize * segmentShiftRatio);
                    int candidateLength = Math.min(((mwQueryLength - 1) * Math.round(segmentSize * segmentShiftRatio)) + segmentSize, origDataSeqLength - candidateOffset);
                    candidate.setObjectKey(new AbstractObjectKey(dataSeqId + "_0_" + candidateOffset + "_" + candidateLength));

                    float dist = mwQuerySeq.getDistance(candidate);
                    op.addToAnswer(candidate, dist, null);

                    mwI++;
                    seqCandidateCount++;
                }
                totalCandidateCount += seqCandidateCount;

//                System.out.println("  * processed sequence " + dataSeq.getLocatorURI() + " with " + seqCandidateCount + " candidates");
            }
            System.out.println(" * total candidate count: " + totalCandidateCount);
        }
        System.out.println("Total processing time: " + (System.currentTimeMillis() - startTime) + "ms");

        // Evaluation statistics
        System.out.println("Processed queries: " + queryMgmt.getObjectCount());
        AnnotationMetrics.printStatistics(sequencesGT, ops, maxK);

        // Original data processing
        final int stepSize = Math.round(segmentSize * segmentShiftRatio);

        startTime = System.currentTimeMillis();
        ops = new ArrayList<>(origQueryMgmt.getObjectCount());
        for (LocalAbstractObject querySeq : origQueryMgmt.getObjects()) {
            System.out.println("Processing query: " + querySeq.getLocatorURI());
            int totalCandidateCount = 0;

            SequenceMocapPoseCoordsL2DTW origQuerySeq = (SequenceMocapPoseCoordsL2DTW) querySeq;
            int queryLength = origQuerySeq.getSequenceLength();
            SequenceMocap<?> origQuerySeqConverted = CaffeObjectMotionImageSocketExtractor.normalizeSequence(origQuerySeq.duplicate(), sequenceConvertors);

            KNNQueryOperation op = new KNNQueryOperation(origQuerySeq, maxK);
            op.setAnswerCollection(new OverlappingAnnotationRemovalCollection());
            ops.add(op);

            for (SequenceMocap<?> dataSeq : origSeqMgmt.getSequences()) {
                int seqCandidateCount = 0;
                SequenceMocapPoseCoordsL2DTW origDataSeq = (SequenceMocapPoseCoordsL2DTW) dataSeq;
                String dataSeqId = ObjectMgmt.parseObjectParentSequenceId(origDataSeq);

                int i = 0;
                while (i + queryLength <= origDataSeq.getSequenceLength()) {
                    SequenceMocapPoseCoordsL2DTW candidate = new SequenceMocapPoseCoordsL2DTW(origDataSeq.getSubsequenceData(i, i + queryLength));

                    candidate.setObjectKey(new AbstractObjectKey(dataSeqId + "_0_" + i + "_" + queryLength));

//                    float dist = origQuerySeq.getDistance(candidate);
                    float dist = origQuerySeqConverted.getDistance(CaffeObjectMotionImageSocketExtractor.normalizeSequence(candidate.duplicate(), sequenceConvertors));
                    op.addToAnswer(candidate, dist, null);

                    i += stepSize;
                    seqCandidateCount++;
                }
                totalCandidateCount += seqCandidateCount;

//                System.out.println("  * processed sequence " + dataSeq.getLocatorURI() + " with " + seqCandidateCount + " candidates");
            }
            System.out.println(" * total candidate count: " + totalCandidateCount);
        }
        System.out.println("Total processing time: " + (System.currentTimeMillis() - startTime) + "ms");

        // Evaluation statistics
        System.out.println("Processed queries: " + origQueryMgmt.getObjectCount());
        AnnotationMetrics.printStatistics(sequencesGT, ops, maxK);
    }

}
