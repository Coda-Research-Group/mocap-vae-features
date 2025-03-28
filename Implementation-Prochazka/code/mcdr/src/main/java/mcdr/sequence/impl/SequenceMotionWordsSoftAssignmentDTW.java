package mcdr.sequence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import mcdr.objects.impl.ObjectMotionWordSoftAssignment;
import mcdr.sequence.SequenceMotionWords;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDist;
import smf.sequences.Sequence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMotionWordsSoftAssignmentDTW extends SequenceMotionWords<ObjectMotionWordSoftAssignment> {

    // DTW distance function
    private static final DistanceFunction<Sequence<List<ObjectMotionWordSoftAssignment>>> dtwDistFunction = new DTWSequenceDist<>();

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentDTW}.
     *
     * @param motionWords list of motion words representing this sequence
     */
    public SequenceMotionWordsSoftAssignmentDTW(List<ObjectMotionWordSoftAssignment> motionWords) {
        super((Class) List.class, motionWords);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentDTW}.
     *
     * @param motionWords list of motion words representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMotionWordsSoftAssignmentDTW(List<ObjectMotionWordSoftAssignment> motionWords, SequenceMotionWordsSoftAssignmentDTW originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, motionWords, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentDTW}
     * loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMotionWordsSoftAssignmentDTW(BufferedReader stream) throws IOException {
        super((Class) List.class, ObjectMotionWordSoftAssignment.class, stream);
    }

    //************ Implemented class SequenceMotionWords ************//
    @Override
    public DistanceFunction<Sequence<List<ObjectMotionWordSoftAssignment>>> getDistanceFunction() {
        return dtwDistFunction;
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMotionWordsSoftAssignmentDTW}
     * loaded from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMotionWordsSoftAssignmentDTW(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, SequenceMotionWordsSoftAssignmentDTW.class, input, serializator);
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
