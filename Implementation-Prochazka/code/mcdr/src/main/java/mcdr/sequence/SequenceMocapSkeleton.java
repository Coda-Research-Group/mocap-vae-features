package mcdr.sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import mcdr.objects.ObjectMocapPose;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import smf.sequences.IndexableSequence;
import smf.sequences.Sequence;

/**
 *
 * @param <I> class of the sequence item
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class SequenceMocapSkeleton<I extends ObjectMocapPose> extends SequenceMocap<I> implements IndexableSequence<List<I>> {

    //************ Attributes ************//
    // class id for serialization
    private static final long serialVersionUID = 1L;
    // skeleton proportions
    protected float[] boneLengths;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMocapSkeleton}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param distFunction distance function comparing items of sequences of
     * this type {@link #sequenceDataClass}
     * @param poses list of poses representing this sequence
     * @param boneLengths skeleton proportions
     */
    public SequenceMocapSkeleton(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, List<I> poses, float[] boneLengths) {
        super(sequenceDataClass, distFunction, poses);
        this.boneLengths = boneLengths;
    }

    /**
     * Creates a new instance of {@link SequenceMocapSkeleton}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param distFunction distance function comparing items of sequences of
     * this type {@link #sequenceDataClass}
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapSkeleton(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, List<I> poses, SequenceMocapSkeleton<I> originalSequence, int offset, boolean storeOrigSeq) {
        super(sequenceDataClass, distFunction, poses, originalSequence, offset, storeOrigSeq);
        this.boneLengths = (originalSequence == null || originalSequence.boneLengths == null) ? null : Arrays.copyOfRange(originalSequence.boneLengths, 0, originalSequence.boneLengths.length);
    }

    /**
     * Creates a new instance of {@link SequenceMocapSkeleton} loaded from the
     * stream. As soon as the sequence is loaded, the identification of this
     * sequence is propagated to all the poses and also the position (i.e., the
     * frame number) of each pose is set.
     *
     * @param sequenceDataClass class of the sequence data
     * @param distFunction distance function comparing items of sequences of
     * this type {@link #sequenceDataClass}
     * @param poseClass class of the sequence item
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapSkeleton(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, Class<I> poseClass, BufferedReader stream) throws IOException {
        super(sequenceDataClass, distFunction, poseClass, stream);

        // Reads skeleton proportions
        this.boneLengths = ObjectMocapPose.parseFloatArray(stream.readLine());
    }

    //************ Abstract methods ************//
    /**
     * Returns the weight of static properties against dynamic features (within
     * interval [0, 1]).
     *
     * @return weight of static properties against dynamic features (within
     * interval [0, 1])
     */
    protected abstract float getStaticPropertiesWeight();

    /**
     * Returns a maximal possible distance for static properties. This distance
     * is used to normalize the distance of static properties. In case this
     * distance is smaller than the actually computed distance, the result
     * distance is set to 1.0.
     *
     * @return a maximal possible distance for static properties
     */
    public abstract float getMaxDistanceStaticProperties();

    /**
     * Returns a maximal possible distance for dynamic properties. This distance
     * is used to normalize the distance of dynamic properties. In case this
     * distance is smaller than the actually computed distance, the result
     * distance is set to 1.0.
     *
     * @return a maximal possible distance for dynamic properties
     */
    public abstract float getMaxDistanceDynamicProperties();

    //************ Methods ************//
    /**
     * Returns skeleton proportions.
     *
     * @return skeleton proportions
     */
    public float[] getBoneLengths() {
        return boneLengths;
    }

    /**
     * Computes the distance of static proportions.
     *
     * @param obj the object to compute distance to
     * @return distance of static proportions
     */
    protected float getDistanceStaticProperties(LocalAbstractObject obj) {
        float[] objBoneLengths = ((SequenceMocapSkeleton) obj).boneLengths;
        float staticPropDist = 0f;
        for (int i = 0; i < boneLengths.length; i++) {
            staticPropDist += Math.abs(boneLengths[i] - objBoneLengths[i]);
        }
        return staticPropDist;
    }

    /**
     * Computes the distance of dynamic proportions.
     *
     * @param obj the object to compute distance to
     * @param metaDistances the array that is filled with the distances of the
     * respective encapsulated objects, if it is not <tt>null</tt>
     * @param distThreshold the threshold value on the distance
     * @return distance of dynamic proportions
     */
    protected float getDistanceDynamicProperties(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        return super.getDistanceImpl(obj, metaDistances, distThreshold);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        if (!(obj instanceof SequenceMocapSkeleton)) {
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        }

        // Computes the partial distance of dynamic proportions
        float dynPropDist = getDistanceDynamicProperties(obj, metaDistances, distThreshold);

        // Aggregated distance
        if (dynPropDist == LocalAbstractObject.UNKNOWN_DISTANCE) {
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        }

        float staticPropDist = getDistanceStaticProperties(obj);
        return getStaticPropertiesWeight() * (staticPropDist / Math.max(staticPropDist, getMaxDistanceStaticProperties())) // static properties
                + (1f - getStaticPropertiesWeight()) * (dynPropDist / Math.max(dynPropDist, getMaxDistanceDynamicProperties())); // dynamic properties
    }

    @Override
    public int getSize() {
        return super.getSize() + (boneLengths.length * Float.SIZE / 8);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        ObjectMocapPose.writeFloatArray(stream, boneLengths);
        stream.write("\n".getBytes());
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMocapSkeleton} loaded from the
     * binary input buffer.
     *
     * @param sequenceDataClass class of the sequence data
     * @param distFunction distance function comparing items of sequences of
     * this type {@link #sequenceDataClass}
     * @param sequenceClass class of this sequence
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapSkeleton(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, Class<? extends SequenceMocapSkeleton<I>> sequenceClass, BinaryInput input, BinarySerializator serializator) throws IOException {
        super(sequenceDataClass, distFunction, sequenceClass, input, serializator);
        this.boneLengths = serializator.readFloatArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator)
                + serializator.write(output, boneLengths);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator)
                + serializator.getBinarySize(boneLengths);
    }
}
