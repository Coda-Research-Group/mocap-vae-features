package mcdr.metrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mcdr.test.utils.ObjectMgmt;
import mcdr.test.utils.SequenceMocapMgmt;
import messif.objects.util.RankedAbstractObject;
import messif.operations.query.KNNQueryOperation;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class AnnotationMetrics {

    public Map<String, BitSet> parseAnnotationsLSMB19(String fileName, int sequenceLength) throws IOException {
        Map<String, BitSet> rtv = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = br.readLine();
        while (line != null) {
            String category = line.split("_")[3];
            String[] columns = line.split("\t");

            if (rtv.get(category) == null) {
                rtv.put(category, new BitSet(sequenceLength));
            }
            updateAnnotations(rtv, category, Integer.valueOf(columns[1]), Integer.valueOf(columns[2]));
            line = br.readLine();
        }
        return rtv;
    }

    /**
     * Map associating the sequence ID and the ground-truth map that keeps the
     * BitSet ground truth for each class.
     *
     * @param fileName
     * @param sequences
     * @return
     * @throws IOException
     */
    public Map<String, Map<String, BitSet>> parseAnnotationsHDM05(String fileName, SequenceMocapMgmt sequences) throws IOException {
        Map<String, Map<String, BitSet>> sequencesGT = new HashMap<>();

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = br.readLine();
        while (line != null) {
            String[] locatorArray = line.split("_");
            String seqId = locatorArray[0];
            Map<String, BitSet> seqGT = sequencesGT.get(seqId);
            if (seqGT == null) {
                seqGT = new HashMap<>();
                sequencesGT.put(seqId, seqGT);
            }
            String classId = locatorArray[1];
            if (seqGT.get(classId) == null) {
                seqGT.put(classId, new BitSet(sequences.getSequence(seqId).getSequenceLength()));
            }
            updateAnnotations(seqGT, classId, Integer.valueOf(locatorArray[2]), Integer.valueOf(locatorArray[3]));
            line = br.readLine();
        }
        return sequencesGT;
    }

    public static Map<String, Integer> getClassGTLengths(Collection<Map<String, BitSet>> gt) {
        Map<String, Integer> rtv = new HashMap<>();
        for (Map<String, BitSet> seqGT : gt) {
            for (Map.Entry<String, BitSet> seqGTEntry : seqGT.entrySet()) {
                Integer count = rtv.get(seqGTEntry.getKey());
                if (count == null) {
                    count = 0;
                }
                count += seqGTEntry.getValue().cardinality();
                rtv.put(seqGTEntry.getKey(), count);
            }
        }
        return rtv;
    }

    /**
     * Returns the evaluation result between the specified segment and the
     * ground-truth segment with the highest IoU (intersection over union on
     * frames). Assumes that at least one frame is between consecutive
     * ground-truth annotations within the same class. Otherwise, touching or
     * overlapping annotations are considered as a single ground-truth
     * annotation.
     *
     * @param gt
     * @param fromIdx
     * @param length
     * @return evaluation result between the provided segment and the
     * highest-IoU ground-truth segment; null if no ground-truth overlapping
     * segment exists
     */
    public static EvaluationResult getBestMatchingAnnotation(BitSet gt, int fromIdx, int length) {

        // Returns null if the ground truth is not defined for the specific class.
        if (gt == null) {
            return null;
        }

        List<Map.Entry<Integer, Integer>> overlappingAnnotations = new LinkedList<>();
        int from = gt.nextSetBit(Math.max(0, gt.previousClearBit(fromIdx)));
        while (from != -1 && from < gt.length() && from <= fromIdx + length - 1) {
            int to = gt.nextClearBit(from);
            if (to == -1) {
                to = gt.length();
            }
            to--;
            overlappingAnnotations.add(new AbstractMap.SimpleEntry<>(from, to));
            from = gt.nextSetBit(to + 1);
        }
        if (overlappingAnnotations.isEmpty()) {
            return null;
        }
        float highestIoU = 0f;
        int highestIoUAnnotationIdx = -1;
        EvaluationResult[] results = new EvaluationResult[overlappingAnnotations.size()];
        for (int i = 0; i < overlappingAnnotations.size(); i++) {
            Map.Entry<Integer, Integer> annotation = overlappingAnnotations.get(i);
            int fromIdx1 = fromIdx;
            int toIdx1 = fromIdx + length - 1;
            int fromIdx2 = annotation.getKey();
            int toIdx2 = annotation.getValue();

            int commonFrameCount = Math.min(toIdx1, toIdx2) - Math.max(fromIdx1, fromIdx2) + 1;
            int unionFrameCount = Math.max(toIdx1, toIdx2) - Math.min(fromIdx1, fromIdx2) + 1;
            float currentIoU = (float) commonFrameCount / unionFrameCount;
            results[i] = new EvaluationResult(commonFrameCount, toIdx1 - fromIdx1 + 1, toIdx2 - fromIdx2 + 1);

            // Checks for the highest IoU
            if (highestIoU <= currentIoU) {
                highestIoU = currentIoU;
                highestIoUAnnotationIdx = i;
            }
        }
        return results[highestIoUAnnotationIdx];
    }

    public Map<String, BitSet> initializeAnnotations(Set<String> categories, int sequenceLength) {
        Map<String, BitSet> rtv = new HashMap<>(categories.size());
        for (String category : categories) {
            rtv.put(category, new BitSet(sequenceLength));
        }
        return rtv;
    }

    public Set<String> getAllCategories(Map<String, BitSet> gt, Map<String, BitSet> annotations) {
        Set<String> rtv = new HashSet<>();
        if (gt != null) {
            for (String category : gt.keySet()) {
                rtv.add(category);
            }
        }
        if (annotations != null) {
            for (String category : annotations.keySet()) {
                rtv.add(category);
            }
        }
        return rtv;
    }

    public boolean updateAnnotations(Map<String, BitSet> annotations, String annotationCategory, int annotationOffset, int annotationLength) {
        BitSet annotation = annotations.get(annotationCategory);
        if (annotation == null) {
            return false;
        }
        annotation.set(annotationOffset, annotationOffset + annotationLength, true);
        return true;
    }

    public EvaluationResult evaluateAnnotation(String categoryToEvaluate, Map<String, BitSet> gt, Map<String, BitSet> annotations) {
        int relevantBitCount = 0;
        int answerBitCount = 0;
        int gtBitCount = 0;
        BitSet gtAnnotation = gt.get(categoryToEvaluate);
        BitSet annotation = annotations.get(categoryToEvaluate);

        if (gtAnnotation != null) {
            gtBitCount += gtAnnotation.cardinality();
            if (annotation != null) {
                BitSet result = (BitSet) gtAnnotation.clone();
                result.and(annotation);
                relevantBitCount += result.cardinality();
            }
        }
        if (annotation != null) {
            answerBitCount += annotation.cardinality();
        }
        return new EvaluationResult(relevantBitCount, answerBitCount, gtBitCount);
    }

    public EvaluationResult evaluateAnnotationsMicro(Set<String> categoriesToEvaluate, Map<String, BitSet> gt, Map<String, BitSet> annotations) {
        int totalRelevantBitCount = 0;
        int totalAnswerBitCount = 0;
        int totalGTBitCount = 0;
        for (String category : categoriesToEvaluate) {
            EvaluationResult result = evaluateAnnotation(category, gt, annotations);
            totalRelevantBitCount += result.relevantBitCount;
            totalAnswerBitCount += result.answerBitCount;
            totalGTBitCount += result.gtBitCount;
        }
        return new EvaluationResult(totalRelevantBitCount, totalAnswerBitCount, totalGTBitCount);
    }

    public static void printStatistics(Map<String, Map<String, BitSet>> sequencesGT, List<KNNQueryOperation> ops, int maxK) {
        Map<String, Integer> classGTLengths = getClassGTLengths(sequencesGT.values());

        // Evaluation statistics
        EvaluationResult[][] results = new EvaluationResult[ops.size()][maxK];
        for (int qIdx = 0; qIdx < ops.size(); qIdx++) {
            String queryCateogry = ObjectMgmt.parseObjectCategoryId(ops.get(qIdx).getQueryObject());

            int t = 0;
            Iterator<RankedAbstractObject> raoIt = ops.get(qIdx).getAnswer();
            while (raoIt.hasNext()) {
                RankedAbstractObject rao = raoIt.next();
                String candidateParentSeqId = ObjectMgmt.parseObjectParentSequenceId(rao.getObject());
                int candidateOffset = ObjectMgmt.parseObjectOffset(rao.getObject());
                int candidateLength = ObjectMgmt.parseObjectLength(rao.getObject());
                results[qIdx][t] = getBestMatchingAnnotation(sequencesGT.get(candidateParentSeqId).get(queryCateogry), candidateOffset, candidateLength);
                t++;
            }
        }

        System.out.println("  k\tPrecisionC\tRecallC\t\tF1C\t\tPrecisionGT\tRecallGT");
        final int queryCount = ops.size();
        for (int k = 1; k <= maxK; k++) {

            float precisionCandidateSum = 0f;
            float recallCandidateSum = 0f;
            float f1CandidateSum = 0f;
            float precisionGTSum = 0;
            float recallGTSum = 0;
            for (int qIdx = 0; qIdx < queryCount; qIdx++) {
                int queryRelevantBitCount = 0;
                int queryAnswerBitCount = 0;
                for (int kLocal = 0; kLocal < k; kLocal++) {
                    if (results[qIdx][kLocal] != null) {
                        precisionCandidateSum += results[qIdx][kLocal].getPrecision();
                        recallCandidateSum += results[qIdx][kLocal].getRecall();
                        f1CandidateSum += results[qIdx][kLocal].getF1();

                        queryAnswerBitCount += results[qIdx][kLocal].answerBitCount;
                        queryRelevantBitCount += results[qIdx][kLocal].relevantBitCount;
                    }
                }
                if (queryAnswerBitCount > 0f) {
                    precisionGTSum += (float) queryRelevantBitCount / queryAnswerBitCount;
                }
                recallGTSum += (float) queryRelevantBitCount / classGTLengths.get(ObjectMgmt.parseObjectCategoryId(ops.get(qIdx).getQueryObject()));
            }

            System.out.print("k=" + k); // threshold (k)
            System.out.print("\t" + (precisionCandidateSum / (queryCount * k))); // candidate precision
            System.out.print("\t" + (recallCandidateSum / (queryCount * k))); // candidate recall
            System.out.print("\t" + (f1CandidateSum / (queryCount * k))); // candidate F1

            System.out.print("\t" + ((float) precisionGTSum / queryCount)); // gt precision
            System.out.print("\t" + ((float) recallGTSum / queryCount)); // gt recall

            System.out.println();
        }
    }

    public static class EvaluationResult {

        public final int relevantBitCount;
        public final int answerBitCount;
        public final int gtBitCount;

        public EvaluationResult(int relevantBitCount, int answerBitCount, int gtBitCount) {
            this.relevantBitCount = relevantBitCount;
            this.answerBitCount = answerBitCount;
            this.gtBitCount = gtBitCount;
        }

        public float getRecall() {
            if (gtBitCount == 0) {
                return 1f;
            } else {
                return (float) relevantBitCount / gtBitCount;
            }
        }

        public float getPrecision() {
            if (answerBitCount == 0) {
                return 1f;
            } else {
                return (float) relevantBitCount / answerBitCount;
            }
        }

        public float getF1() {
            float recall = getRecall();
            float precision = getPrecision();
            if (recall == 0f && precision == 0f) {
                return 0f;
            } else {
                return 2 * recall * precision / (recall + precision);
            }
        }

        public void printStatistics() {
            System.out.println("  Recall: " + getRecall());
            System.out.println("  Precision: " + getPrecision());
            System.out.println("  F1: " + getF1());
        }
    }
}
