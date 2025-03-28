package mcdr.sequence.impl;

import mcdr.distance.LevenshteinDistance;
import mcdr.objects.impl.ObjectMotionWordSoftAssignment;
import mcdr.sequence.SequenceMotionWords;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.sequences.Sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * @author David Procházka
 */
public class SequenceMotionWordsSoftAssignmentLevenshtein extends SequenceMotionWords<ObjectMotionWordSoftAssignment> {

    private static final DistanceFunction<Sequence<List<ObjectMotionWordSoftAssignment>>> distanceFunction = new LevenshteinDistance<>();

    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentLevenshtein}.
     *
     * @param motionWords list of motion words representing this sequence
     */
    public SequenceMotionWordsSoftAssignmentLevenshtein(List<ObjectMotionWordSoftAssignment> motionWords) {
        super((Class) List.class, motionWords);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentLevenshtein}.
     *
     * @param motionWords      list of motion words representing this sequence
     * @param originalSequence the originalSequence that the sequenceData comes from - can be null
     * @param offset           locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq     indicates whether the original sequence will be stored, or not - it will be null
     */
    public SequenceMotionWordsSoftAssignmentLevenshtein(List<ObjectMotionWordSoftAssignment> motionWords, SequenceMotionWordsSoftAssignmentLevenshtein originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, motionWords, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentLevenshtein} loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    public SequenceMotionWordsSoftAssignmentLevenshtein(BufferedReader stream) throws IOException {
        super((Class) List.class, ObjectMotionWordSoftAssignment.class, stream);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentLevenshtein} loaded from the binary input buffer.
     *
     * @param input        buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMotionWordsSoftAssignmentLevenshtein(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, SequenceMotionWordsSoftAssignmentLevenshtein.class, input, serializator);
    }

    @Override
    public DistanceFunction<Sequence<List<ObjectMotionWordSoftAssignment>>> getDistanceFunction() {
        return distanceFunction;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
