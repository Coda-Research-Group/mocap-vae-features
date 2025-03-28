package mcdr.sequence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseAnglesVelsL1;
import mcdr.sequence.SequenceMocapSkeletonFourier;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapSkeletonPoseAnglesVelsL1Fourier extends SequenceMocapSkeletonFourier<ObjectMocapPoseAnglesVelsL1> {

    //************ Attributes ************//
    // weights for individual harmonics
    protected static final float[] harmonicsWeights = {0f, 0.50804543f, 0.9545017f, 0.96526504f, 0f, 0.39345175f, 0.55711454f, 0.11797442f, 0f, 0.38257912f};
    // weights for individual joints
    protected static final float[] jointWeights = {0.0f, 0.15099218f, 0.0f, 1.0f, 1.0f, 0.249821f, 0.8430854f, 0.03244391f, 0.090462804f, 0.0f, 0.7425105f, 1.0f, 0.0f, 1.0f, 0.23882334f, 1.0f, 1.0f, 0.219864f, 0.4409706f, 1.0f, 1.0f};

    //****************** Constructors ******************//
    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1Fourier}.
     *
     * @param poses list of poses representing this sequence
     * @param boneLengths skeleton proportions
     * @param harmonics harmonics
     */
    public SequenceMocapSkeletonPoseAnglesVelsL1Fourier(List<ObjectMocapPoseAnglesVelsL1> poses, float[] boneLengths, float[][] harmonics) {
        super((Class) List.class, poses, boneLengths, harmonics);
    }

    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1Fourier}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapSkeletonPoseAnglesVelsL1Fourier(List<ObjectMocapPoseAnglesVelsL1> poses, SequenceMocapSkeletonPoseAnglesVelsL1Fourier originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1Fourier} loaded from the
     * stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapSkeletonPoseAnglesVelsL1Fourier(BufferedReader stream) throws IOException {
        super((Class) List.class, ObjectMocapPoseAnglesVelsL1.class, stream);
    }

    //************ Overrided class SequenceMocapSkeleton ************//
    @Override
    protected float getStaticPropertiesWeight() {
        return 0.2f;
    }

    @Override
    public float getMaxDistanceStaticProperties() {
        return 7.352042f; // mean distance in CMU/HDM05
    }

    @Override
    public float getMaxDistanceDynamicProperties() {
        return 373.78607f; // mean distance in CMU/HDM05
    }

    //************ Overrided class SequenceMocapSkeletonFourier ************//
    @Override
    public float[] getHarmonicsWeights() {
        return harmonicsWeights;
    }

    @Override
    public float[] getJointWeights() {
        return jointWeights;
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1Fourier} loaded from the
     * binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapSkeletonPoseAnglesVelsL1Fourier(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, SequenceMocapSkeletonPoseAnglesVelsL1Fourier.class, input, serializator);
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
