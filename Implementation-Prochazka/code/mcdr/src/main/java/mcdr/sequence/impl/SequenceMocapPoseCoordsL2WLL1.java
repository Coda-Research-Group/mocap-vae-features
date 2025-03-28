package mcdr.sequence.impl;

import mcdr.sequence.SequenceMocap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapPoseCoordsL2WLL1 extends SequenceMocap<ObjectMocapPoseCoordsL2> {

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2WLL1}.
     *
     * @param poses list of poses representing this sequence
     */
    public SequenceMocapPoseCoordsL2WLL1(List<ObjectMocapPoseCoordsL2> poses) {
        super((Class) List.class, null, poses);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2WLL1}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapPoseCoordsL2WLL1(List<ObjectMocapPoseCoordsL2> poses, SequenceMocapPoseCoordsL2WLL1 originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, null, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2WLL1} loaded
     * from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapPoseCoordsL2WLL1(BufferedReader stream) throws IOException {
        super((Class) List.class, null, ObjectMocapPoseCoordsL2.class, stream);
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMocapPoseCoordsL2WLL1} loaded
     * from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapPoseCoordsL2WLL1(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, null, SequenceMocapPoseCoordsL2WLL1.class, input, serializator);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        if (!(obj instanceof SequenceMocapPoseCoordsL2WLL1)) {
            return Float.NaN;
        }
        SequenceMocapPoseCoordsL2WLL1 objSequence = (SequenceMocapPoseCoordsL2WLL1) obj;

        int poseCountToCompare = Math.min(getSequenceLength(), objSequence.getSequenceLength());
        float rtv = 0f;
        for (int i = 0; i < poseCountToCompare; i++) {
            rtv += getObject(i).getDistance(objSequence.getObject(i));
        }
        return rtv / (float) poseCountToCompare;
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
