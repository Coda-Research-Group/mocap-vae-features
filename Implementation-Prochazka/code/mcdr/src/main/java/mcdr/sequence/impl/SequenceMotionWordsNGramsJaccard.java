package mcdr.sequence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import mcdr.objects.impl.ObjectMotionWordNMatches;
import mcdr.sequence.SequenceMotionWords;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.sequences.Sequence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMotionWordsNGramsJaccard extends SequenceMotionWords<ObjectMotionWordNMatches> {

    // size of n-grams generated from a sequence of motion words
    public static int nGramSize = 3;
    // set of the generated n-grams
    protected List<MotionWordNGram> nGrams;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMotionWordsNGramsJaccard}.
     *
     * @param motionWords list of motion words representing this sequence
     */
    public SequenceMotionWordsNGramsJaccard(List<ObjectMotionWordNMatches> motionWords) {
        super((Class) List.class, motionWords);
        generateNGrams();
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsNGramsJaccard}.
     *
     * @param motionWords list of motion words representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMotionWordsNGramsJaccard(List<ObjectMotionWordNMatches> motionWords, SequenceMotionWordsNGramsJaccard originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, motionWords, originalSequence, offset, storeOrigSeq);
        generateNGrams();
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsNGramsJaccard} loaded
     * from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMotionWordsNGramsJaccard(BufferedReader stream) throws IOException {
        super((Class) List.class, ObjectMotionWordNMatches.class, stream);
        generateNGrams();
    }

    //************ Methods ************//
    private void generateNGrams() {
        nGrams = new LinkedList<>();
        for (int i = 0; i <= getObjectCount() - nGramSize; i++) {
            nGrams.add(new MotionWordNGram(getObjects().subList(i, i + nGramSize)));
        }
        if (nGrams.isEmpty()) {
            nGrams.add(new MotionWordNGram(getObjects()));
        }
    }

    //************ Implemented class SequenceMotionWords ************//
    @Override
    public DistanceFunction<Sequence<List<ObjectMotionWordNMatches>>> getDistanceFunction() {
        return null;
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        if (!(obj instanceof SequenceMotionWordsNGramsJaccard)) {
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        }
        List<MotionWordNGram> objNGrams = ((SequenceMotionWordsNGramsJaccard) obj).nGrams;
        int intersection = 0;

        // (intersection: n * m, union: n * m)
//        for (MotionWordNGram nGram1 : nGrams) {
//            for (MotionWordNGram nGram2 : objNGrams) {
//                if (nGram1.equals(nGram2)) {
//                    intersection++;
//                }
//            }
//        }
//        int union = nGrams.size() * objNGrams.size();

        // (intersection: min(n, m)->max(m, n) & single MW matching only, union: m + n - intersection)
        List<MotionWordNGram> nGramsMin = nGrams;
        List<MotionWordNGram> nGramsMax = objNGrams;
        if (nGramsMin.size() > nGramsMax.size()) {
            nGramsMin = objNGrams;
            nGramsMax = nGrams;
        }
        for (MotionWordNGram nGram1 : nGramsMin) {
            for (MotionWordNGram nGram2 : nGramsMax) {
                if (nGram1.equals(nGram2)) {
                    intersection++;
                    break;
                }
            }
        }
        int union = nGrams.size() + objNGrams.size() - intersection;

        // (intersection: n->m + m->n & single MW matching only, union: m + n)
//        for (MotionWordNGram nGram1 : nGrams) {
//            for (MotionWordNGram nGram2 : objNGrams) {
//                if (nGram1.equals(nGram2)) {
//                    intersection++;
//                    break;
//                }
//            }
//        }
//        for (MotionWordNGram nGram1 : objNGrams) {
//            for (MotionWordNGram nGram2 : nGrams) {
//                if (nGram1.equals(nGram2)) {
//                    intersection++;
//                    break;
//                }
//            }
//        }
//        int union = nGrams.size() + objNGrams.size();

        return 1f - ((float) intersection / union);
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMotionWordsNGramsJaccard} loaded
     * from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMotionWordsNGramsJaccard(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, SequenceMotionWordsNGramsJaccard.class, input, serializator);
        generateNGrams();
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }

    public static class MotionWordNGram {

        // array keeping motion words of this n-gram
        ObjectMotionWordNMatches[] nGram;

        //****************** Constructors ******************//
        /**
         * Creates a new instance of {@link MotionWordNGram}.
         *
         * @param motionWords list of motion words representing this n-gram
         */
        public MotionWordNGram(List<ObjectMotionWordNMatches> motionWords) {
            nGram = new ObjectMotionWordNMatches[motionWords.size()];
            for (int i = 0; i < motionWords.size(); i++) {
                nGram[i] = motionWords.get(i);
            }
        }

        //************ Overrided class Object ************//
        @Override
        public boolean equals(Object obj) {
            ObjectMotionWordNMatches[] nGramObj = ((MotionWordNGram) obj).nGram;
            if (nGram.length != nGramObj.length) {
                return false;
            }
            for (int i = 0; i < nGram.length; i++) {
                if (nGram[i].getDistance(nGramObj[i]) != 0f) {
                    return false;
                }
            }
            return true;
        }
    }
}
