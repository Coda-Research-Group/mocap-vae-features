package mcdr.sequence.impl;

import mcdr.distance.LevenshteinDistance;
import mcdr.objects.impl.ObjectBodyPart;
import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import mcdr.sequence.SequenceMotionWords;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.sequences.Sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Modification of Composite MW sequence distance function.
 * The main difference is that the distance function can be specified dynamically.
 * 
 * @author David Procházka
 */
public class SequenceMotionWordsCompositeAutoTuning extends SequenceMotionWords<ObjectMotionWordCompositeAutoTuning> {

    /**
     * Used in supervised Composite MW -- represents the body part selected for the sequence's category
     */
    public ObjectBodyPart bodyPart;

    public static DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction = new LevenshteinDistance<>();

    /**
     * Creates a new instance of {@link SequenceMotionWordsCompositeAutoTuning}.
     *
     * @param motionWords list of motion words representing this sequence
     */
    public SequenceMotionWordsCompositeAutoTuning(List<ObjectMotionWordCompositeAutoTuning> motionWords) {
        super((Class) List.class, motionWords);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsCompositeAutoTuning}.
     *
     * @param motionWords      list of motion words representing this sequence
     * @param originalSequence the originalSequence that the sequenceData comes from - can be null
     * @param offset           locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq     indicates whether the original sequence will be stored, or not - it will be null
     */
    public SequenceMotionWordsCompositeAutoTuning(List<ObjectMotionWordCompositeAutoTuning> motionWords, SequenceMotionWordsCompositeAutoTuning originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, motionWords, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsCompositeAutoTuning} loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    public SequenceMotionWordsCompositeAutoTuning(BufferedReader stream) throws IOException {
        super((Class) List.class, ObjectMotionWordCompositeAutoTuning.class, stream);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsCompositeAutoTuning} loaded from the binary input buffer.
     *
     * @param input        buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMotionWordsCompositeAutoTuning(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, SequenceMotionWordsCompositeAutoTuning.class, input, serializator);
    }

    @Override
    public DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> getDistanceFunction() {
        return distanceFunction;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
