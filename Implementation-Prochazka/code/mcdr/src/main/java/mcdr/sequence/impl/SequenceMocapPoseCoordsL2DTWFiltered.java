package mcdr.sequence.impl;

import mcdr.objects.impl.ObjectMocapPoseCoordsL2Filtered;
import mcdr.sequence.SequenceMocap;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDist;
import smf.sequences.Sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Based on {@link SequenceMocapPoseCoordsL2DTW} but uses joint filter.
 * 
 * @author David Procházka
 */
public class SequenceMocapPoseCoordsL2DTWFiltered extends SequenceMocap<ObjectMocapPoseCoordsL2Filtered> {

    private static final DistanceFunction<Sequence<List<ObjectMocapPoseCoordsL2Filtered>>> distanceFunction = new DTWSequenceDist<>();

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTWFiltered}.
     *
     * @param poses list of poses representing this sequence
     */
    public SequenceMocapPoseCoordsL2DTWFiltered(List<ObjectMocapPoseCoordsL2Filtered> poses) {
        super((Class) List.class, distanceFunction, poses);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTWFiltered}.
     *
     * @param poses            list of poses representing this sequence
     * @param originalSequence the {@code originalSequence} that the {@code sequenceData} comes from - can be null
     * @param offset           locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq     indicates whether the original sequence will be stored, or not - it will be null
     */
    public SequenceMocapPoseCoordsL2DTWFiltered(List<ObjectMocapPoseCoordsL2Filtered> poses, SequenceMocapPoseCoordsL2DTWFiltered originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, distanceFunction, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTWFiltered} loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapPoseCoordsL2DTWFiltered(BufferedReader stream) throws IOException {
        super((Class) List.class, distanceFunction, ObjectMocapPoseCoordsL2Filtered.class, stream);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTWFiltered} loaded from the binary input buffer.
     *
     * @param input        buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given stream
     *                     (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapPoseCoordsL2DTWFiltered(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, distanceFunction, SequenceMocapPoseCoordsL2DTWFiltered.class, input, serializator);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}

