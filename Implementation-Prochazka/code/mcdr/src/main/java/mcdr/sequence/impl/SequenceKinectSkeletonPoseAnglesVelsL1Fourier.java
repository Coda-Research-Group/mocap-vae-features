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
public class SequenceKinectSkeletonPoseAnglesVelsL1Fourier extends SequenceMocapSkeletonFourier<ObjectMocapPoseAnglesVelsL1> {

    //************ Attributes ************//
    // weights for individual harmonics
    protected static final float[] harmonicsWeights = {0.15871948f, 1.0f, 0.2750309f, 0.0f, 0.0f, 0.5449986f, 0.020352831f, 0.0f, 0.0f, 0.4878175f};
    // weights for individual joints
    protected static final float[] jointWeights = {1f, 1f, 1f, 1f, 1f, Float.NaN, 1f, 1f, 1f, 1f, Float.NaN, 1f, Float.NaN, 1f, 1f, Float.NaN, 1f, 1f, 1f, 1f, Float.NaN, 1f, 1f, 1f, 1f, 1f, 1f, Float.NaN, 1f, 1f, 1f};

    //****************** Constructors ******************//
    /**
     * Creates a new instance of
     * {@link SequenceKinectSkeletonPoseAnglesVelsL1Fourier}.
     *
     * @param poses list of poses representing this sequence
     * @param boneLengths skeleton proportions
     * @param harmonics harmonics
     */
    public SequenceKinectSkeletonPoseAnglesVelsL1Fourier(List<ObjectMocapPoseAnglesVelsL1> poses, float[] boneLengths, float[][] harmonics) {
        super((Class) List.class, poses, boneLengths, harmonics);
    }

    /**
     * Creates a new instance of
     * {@link SequenceKinectSkeletonPoseAnglesVelsL1Fourier}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceKinectSkeletonPoseAnglesVelsL1Fourier(List<ObjectMocapPoseAnglesVelsL1> poses, SequenceMocapSkeletonPoseAnglesVelsL1Fourier originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of
     * {@link SequenceKinectSkeletonPoseAnglesVelsL1Fourier} loaded from the
     * stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceKinectSkeletonPoseAnglesVelsL1Fourier(BufferedReader stream) throws IOException {
        super((Class) List.class, ObjectMocapPoseAnglesVelsL1.class, stream);
    }

    //************ Overrided class SequenceMocapSkeleton ************//
    @Override
    protected float getStaticPropertiesWeight() {
        return 0.5f;
    }

    @Override
    public float getMaxDistanceStaticProperties() {
        return 0.26226896f; // mean distance in Kinect
    }

    @Override
    public float getMaxDistanceDynamicProperties() {
        return 5.736656f; // mean distance in Kinect
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
     * {@link SequenceKinectSkeletonPoseAnglesVelsL1Fourier} loaded from the
     * binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceKinectSkeletonPoseAnglesVelsL1Fourier(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, SequenceMocapSkeletonPoseAnglesVelsL1Fourier.class, input, serializator);
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
