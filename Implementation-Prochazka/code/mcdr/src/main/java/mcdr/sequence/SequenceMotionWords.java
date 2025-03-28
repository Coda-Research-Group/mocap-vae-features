package mcdr.sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import mcdr.objects.impl.ObjectMotionWord;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectArray;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import smf.sequences.Sequence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 * @param <I> class of motion word implementation
 */
public abstract class SequenceMotionWords<I extends ObjectMotionWord> extends MetaObjectArray implements Sequence<List<I>> {

    //************ Attributes ************//
    // class id for serialization
    private static final long serialVersionUID = 1L;
    // class of the sequence data
    private final Class<List<I>> sequenceDataClass;
    // the {@link #originalSequence} that the {@link #sequenceData} comes from - can be null
    private final SequenceMotionWords<I> originalSequence;
    // locator of the original sequence, if this is a subsequence
    protected final String originalSequenceLocator;
    // offset in the {@link #originalSequence} that the {@link #sequenceData} comes from
    private final int originalOffset;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMotionWords}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param motionWords list of motion words representing this sequence
     */
    public SequenceMotionWords(Class<List<I>> sequenceDataClass, List<I> motionWords) {
        this(sequenceDataClass, motionWords, null, -1, false);
    }

    /**
     * Creates a new instance of {@link SequenceMotionWords}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param motionWords list of motion words representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMotionWords(Class<List<I>> sequenceDataClass, List<I> motionWords,
            SequenceMotionWords<I> originalSequence, int offset, boolean storeOrigSeq) {
        super((String) null, motionWords);
        this.sequenceDataClass = sequenceDataClass;
        this.originalSequence = storeOrigSeq ? originalSequence : null;
        this.originalSequenceLocator = (originalSequence == null) ? null : originalSequence.getLocatorURI();
        this.originalOffset = offset;
    }

    /**
     * Creates a new instance of {@link SequenceMotionWords} loaded from the
     * stream.
     *
     * @param sequenceDataClass class of the sequence data this type
     * {@link #sequenceDataClass}
     * @param motionWordClass class of the sequence item
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMotionWords(Class<List<I>> sequenceDataClass, Class<I> motionWordClass, BufferedReader stream) throws IOException {
        super(stream, motionWordClass);
        this.sequenceDataClass = sequenceDataClass;
        this.originalSequence = null;
        this.originalSequenceLocator = null;
        this.originalOffset = -1;
    }

    //************ Abstract methods ************//
    /**
     * Returns distance function comparing items of sequences of this type
     * {@link #sequenceDataClass}.
     *
     * @return distance function comparing items of sequences of this type
     * {@link #sequenceDataClass}
     */
    public abstract DistanceFunction<Sequence<List<I>>> getDistanceFunction();

    //************ Overrided class MetaObjectArray ************//
    @Override
    public final List<I> getObjects() {
        return (List<I>) super.getObjects();
    }

    @Override
    public I getObject(int index) throws IndexOutOfBoundsException {
        return (I) super.getObject(index);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        if (!(obj instanceof SequenceMotionWords)) {
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        }
        SequenceMotionWords<I> objSequence = (SequenceMotionWords<I>) obj;
        return objSequence.getDistanceFunction().getDistance(this, objSequence);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        stream.write((getObjectCount() + ";mcdr.objects.impl.ObjectMotionWord\n").getBytes());
        super.writeData(stream);
    }

    //************ Implemented interface Sequence ************//
    @Override
    public List<I> getSequenceData() {
        return getObjects();
    }

    @Override
    public int getSequenceLength() {
        return getObjectCount();
    }

    @Override
    public Class<? extends List<I>> getSequenceDataClass() {
        return sequenceDataClass;
    }

    @Override
    public List<I> getSubsequenceData(int from, int to) {
        return getObjects().subList(from, to);
    }

    @Override
    public Sequence<? extends List<I>> getOriginalSequence() {
        return originalSequence;
    }

    @Override
    public int getOffset() {
        return originalOffset;
    }

    @Override
    public String getOriginalSequenceLocator() {
        return originalSequenceLocator;
    }

    @Override
    public float getPiecewiseDist(int thisPieceOffset, Sequence<List<I>> other, int otherPieceOffset) {
        return getObjects().get(thisPieceOffset).getDistance(other.getSequenceData().get(otherPieceOffset));
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMotionWords} loaded from the
     * binary input buffer.
     *
     * @param sequenceDataClass class of the sequence data
     * @param sequenceClass class of this sequence
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMotionWords(Class<List<I>> sequenceDataClass, Class<? extends SequenceMotionWords<I>> sequenceClass, BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.sequenceDataClass = sequenceDataClass;
        this.originalSequence = serializator.readObject(input, sequenceClass);
        this.originalSequenceLocator = serializator.readString(input);
        this.originalOffset = serializator.readInt(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator)
                + serializator.write(output, originalSequence)
                + serializator.write(output, originalSequenceLocator)
                + serializator.write(output, originalOffset);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator)
                + serializator.getBinarySize(originalSequence)
                + serializator.getBinarySize(originalSequenceLocator)
                + serializator.getBinarySize(originalOffset);
    }

    //************ Overrided class Object ************//
    @Override
    public int hashCode() {
        return getLocatorURI().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SequenceMotionWords<?> other = (SequenceMotionWords<?>) obj;
        return this.hashCode() == other.hashCode();
    }
}
