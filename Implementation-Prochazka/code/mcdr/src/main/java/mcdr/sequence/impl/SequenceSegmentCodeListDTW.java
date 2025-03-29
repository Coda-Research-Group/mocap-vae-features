package mcdr.sequence.impl;

import mcdr.objects.ObjectMocapPose;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import mcdr.objects.impl.ObjectSegmentCodeList;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.SequenceSegmentCodeList;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDist;
import smf.sequences.Sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tomas Drkos, FI MU Brno, Czech Republic
 */
public class SequenceSegmentCodeListDTW extends SequenceSegmentCodeList<ObjectSegmentCodeList> {

    // DTW distance function
    private static final DistanceFunction<Sequence<List<ObjectSegmentCodeList>>> dtwDistFunction = new DTWSequenceDist<>();

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceSegmentCodeListDTW}.
     *
     * @param poses list of poses representing this sequence
     */
    public SequenceSegmentCodeListDTW(List<ObjectSegmentCodeList> poses) {
        super((Class) List.class, dtwDistFunction, poses);
    }

    /**
     * Creates a new instance of {@link SequenceSegmentCodeListDTW}.
     *
     * @param SCLs list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceSegmentCodeListDTW(List<ObjectSegmentCodeList> SCLs, SequenceSegmentCodeListDTW originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, dtwDistFunction, SCLs, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceSegmentCodeListDTW} loaded
     * from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceSegmentCodeListDTW(BufferedReader stream) throws IOException {
        super((Class) List.class, dtwDistFunction, ObjectSegmentCodeList.class, stream);
    }

    //************ Factory methods ************//
    /**
     * Creates a new instance of {@link SequenceSegmentCodeListDTW} from any
     * existing mocap sequence (of any kind).
     *
     * @param sequence input sequence to be converted
     * @return converted sequence
     */
    public static SequenceSegmentCodeListDTW create(SequenceSegmentCodeList<?> sequence) {
        List<ObjectSegmentCodeList> SLCodes = new ArrayList<>(sequence.getSequenceLength());
        for (ObjectSegmentCodeList SLCode : sequence.getObjects()) {
            SLCodes.add(new ObjectSegmentCodeList(SLCode.getQuantizedDimensions()));
        }
        SequenceSegmentCodeListDTW rtv = new SequenceSegmentCodeListDTW(SLCodes);
        rtv.setObjectKey(sequence.getObjectKey());
        return rtv;
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceSegmentCodeListDTW} loaded
     * from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceSegmentCodeListDTW(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, dtwDistFunction, SequenceSegmentCodeListDTW.class, input, serializator);
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

    @Override
    public LocalAbstractObject getSequence() {
        return null;
    }

    @Override
    public LocalAbstractObject getSequenceItem(int i) {
        return null;
    }
}
