package mcdr.sequence.impl;

import mcdr.sequence.SequenceMocap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseAnglesL1Circle;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDistNormalized;
import smf.sequences.Sequence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapPoseAnglesL1CircleDTW extends SequenceMocap<ObjectMocapPoseAnglesL1Circle> {

    // DTW distance function
    private static final DistanceFunction<Sequence<List<ObjectMocapPoseAnglesL1Circle>>> dtwDistFunction = new DTWSequenceDistNormalized<List<ObjectMocapPoseAnglesL1Circle>>();

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseAnglesL1CircleDTW}.
     *
     * @param poses list of poses representing this sequence
     */
    public SequenceMocapPoseAnglesL1CircleDTW(List<ObjectMocapPoseAnglesL1Circle> poses) {
        super((Class) List.class, dtwDistFunction, poses);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseAnglesL1CircleDTW}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapPoseAnglesL1CircleDTW(List<ObjectMocapPoseAnglesL1Circle> poses, SequenceMocapPoseAnglesL1CircleDTW originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, dtwDistFunction, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseAnglesL1CircleDTW}
     * loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapPoseAnglesL1CircleDTW(BufferedReader stream) throws IOException {
        super((Class) List.class, dtwDistFunction, ObjectMocapPoseAnglesL1Circle.class, stream);
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseAnglesL1CircleDTW}
     * loaded from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapPoseAnglesL1CircleDTW(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, dtwDistFunction, SequenceMocapPoseAnglesL1CircleDTW.class, input, serializator);
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
