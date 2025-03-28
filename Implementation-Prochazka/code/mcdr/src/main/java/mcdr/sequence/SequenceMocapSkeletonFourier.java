package mcdr.sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import mcdr.objects.ObjectMocapPose;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @param <I> class of the sequence item
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class SequenceMocapSkeletonFourier<I extends ObjectMocapPose> extends SequenceMocapSkeleton<I> {

    //************ Attributes ************//
    // class id for serialization
    private static final long serialVersionUID = 1L;
    // harmonics
    protected float[][] harmonics;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMocapSkeletonFourier}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param poses list of poses representing this sequence
     * @param boneLengths skeleton proportions
     * @param harmonics harmonics
     */
    public SequenceMocapSkeletonFourier(Class<List<I>> sequenceDataClass, List<I> poses, float[] boneLengths, float[][] harmonics) {
        super(sequenceDataClass, null, poses, boneLengths);
        this.harmonics = harmonics;
    }

    /**
     * Creates a new instance of {@link SequenceMocapSkeletonFourier}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapSkeletonFourier(Class<List<I>> sequenceDataClass, List<I> poses, SequenceMocapSkeletonFourier<I> originalSequence, int offset, boolean storeOrigSeq) {
        super(sequenceDataClass, null, poses, originalSequence, offset, storeOrigSeq);
        this.harmonics = (originalSequence == null || originalSequence.harmonics == null) ? null : Arrays.copyOfRange(originalSequence.harmonics, 0, originalSequence.harmonics.length);
    }

    /**
     * Creates a new instance of {@link SequenceMocapSkeletonFourier} loaded
     * from the stream. As soon as the sequence is loaded, the identification of
     * this sequence is propagated to all the poses and also the position (i.e.,
     * the frame number) of each pose is set.
     *
     * @param sequenceDataClass class of the sequence data
     * @param poseClass class of the sequence item
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapSkeletonFourier(Class<List<I>> sequenceDataClass, Class<I> poseClass, BufferedReader stream) throws IOException {
        super(sequenceDataClass, null, poseClass, stream);

        // Reads harmonics
        this.harmonics = ObjectMocapPose.parseFloatArray2d(stream.readLine());
    }

    //************ Abstract methods ************//
    /**
     * Returns harmonics weights.
     *
     * @return harmonics weights
     */
    public abstract float[] getHarmonicsWeights();

    /**
     * Returns joint weights for harmonics computation.
     *
     * @return joint weights for harmonics computation
     */
    public abstract float[] getJointWeights();

    //************ Methods ************//
    /**
     * Returns harmonics.
     *
     * @return harmonics
     */
    public float[][] getHarmonics() {
        return harmonics;
    }

    //************ Overrided class SequenceMocapSkeleton ************//
    @Override
    protected float getDistanceDynamicProperties(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        float[][] objHarmonics = ((SequenceMocapSkeletonFourier) obj).harmonics;

        float dynPropDist = 0f;
        for (int i = 0; i < harmonics.length; i++) {
            for (int j = 0; j < harmonics[i].length; j++) {
                float dist = getJointWeights()[i] * (getHarmonicsWeights()[j] * Math.abs(harmonics[i][j] - objHarmonics[i][j]));
                if (!Float.isNaN(dist)) {
                    dynPropDist += dist;
                }
            }
        }
        return dynPropDist;
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    public int getSize() {
        return super.getSize() + (harmonics.length * harmonics[0].length * Float.SIZE / 8);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        ObjectMocapPose.writeFloatArray(stream, harmonics);
        stream.write("\n".getBytes());
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMocapSkeletonFourier} loaded
     * from the binary input buffer.
     *
     * @param sequenceDataClass class of the sequence data
     * @param sequenceClass class of this sequence
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapSkeletonFourier(Class<List<I>> sequenceDataClass, Class<? extends SequenceMocapSkeleton<I>> sequenceClass, BinaryInput input, BinarySerializator serializator) throws IOException {
        super(sequenceDataClass, null, sequenceClass, input, serializator);
        this.harmonics = new float[serializator.readInt(input)][];
        for (int i = 0; i < this.harmonics.length; i++) {
            this.harmonics[i] = serializator.readFloatArray(input);
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int rtv = super.binarySerialize(output, serializator);
        rtv += serializator.write(output, harmonics.length);
        for (float[] harmonic : harmonics) {
            rtv += serializator.write(output, harmonic);
        }
        return rtv;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int rtv = super.getBinarySize(serializator);
        rtv += serializator.getBinarySize(harmonics.length);
        for (float[] harmonic : harmonics) {
            rtv += serializator.getBinarySize(harmonic);
        }
        return rtv;
    }
}
