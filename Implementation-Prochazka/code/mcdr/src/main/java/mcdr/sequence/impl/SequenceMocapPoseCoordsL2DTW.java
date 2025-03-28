package mcdr.sequence.impl;

import mcdr.sequence.SequenceMocap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import mcdr.objects.ObjectMocapPose;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDist;
import smf.sequences.Sequence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapPoseCoordsL2DTW extends SequenceMocap<ObjectMocapPoseCoordsL2> {

    // DTW distance function
    private static final DistanceFunction<Sequence<List<ObjectMocapPoseCoordsL2>>> dtwDistFunction = new DTWSequenceDist<>();

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTW}.
     *
     * @param poses list of poses representing this sequence
     */
    public SequenceMocapPoseCoordsL2DTW(List<ObjectMocapPoseCoordsL2> poses) {
        super((Class) List.class, dtwDistFunction, poses);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTW}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapPoseCoordsL2DTW(List<ObjectMocapPoseCoordsL2> poses, SequenceMocapPoseCoordsL2DTW originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, dtwDistFunction, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTW} loaded
     * from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapPoseCoordsL2DTW(BufferedReader stream) throws IOException {
        super((Class) List.class, dtwDistFunction, ObjectMocapPoseCoordsL2.class, stream);
    }

    //************ Factory methods ************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTW} from any
     * existing mocap sequence (of any kind).
     *
     * @param sequence input sequence to be converted
     * @return converted sequence
     */
    public static SequenceMocapPoseCoordsL2DTW create(SequenceMocap<?> sequence) {
        List<ObjectMocapPoseCoordsL2> poses = new ArrayList<>(sequence.getSequenceLength());
        for (ObjectMocapPose pose : sequence.getObjects()) {
            poses.add(new ObjectMocapPoseCoordsL2(pose.getJointCoordinates()));
        }
        SequenceMocapPoseCoordsL2DTW rtv = new SequenceMocapPoseCoordsL2DTW(poses);
        rtv.setObjectKey(sequence.getObjectKey());
        return rtv;
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2DTW} loaded
     * from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapPoseCoordsL2DTW(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, dtwDistFunction, SequenceMocapPoseCoordsL2DTW.class, input, serializator);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.distFunction = dtwDistFunction;
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
