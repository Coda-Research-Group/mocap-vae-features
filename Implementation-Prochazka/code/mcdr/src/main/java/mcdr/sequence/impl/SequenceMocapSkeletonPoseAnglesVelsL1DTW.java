package mcdr.sequence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseAnglesVelsL1;
import mcdr.sequence.SequenceMocapSkeleton;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDistNormalized;
import smf.sequences.Sequence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapSkeletonPoseAnglesVelsL1DTW extends SequenceMocapSkeleton<ObjectMocapPoseAnglesVelsL1> {

    // DTW distance function
    private static final DistanceFunction<Sequence<List<ObjectMocapPoseAnglesVelsL1>>> dtwDistFunction = new DTWSequenceDistNormalized<List<ObjectMocapPoseAnglesVelsL1>>();

    //****************** Constructors ******************//
    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1DTW}.
     *
     * @param poses list of poses representing this sequence
     * @param boneLengths skeleton proportions
     */
    public SequenceMocapSkeletonPoseAnglesVelsL1DTW(List<ObjectMocapPoseAnglesVelsL1> poses, float[] boneLengths) {
        super((Class) List.class, dtwDistFunction, poses, boneLengths);
    }

    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1DTW}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceMocapSkeletonPoseAnglesVelsL1DTW(List<ObjectMocapPoseAnglesVelsL1> poses, SequenceMocapSkeletonPoseAnglesVelsL1DTW originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, dtwDistFunction, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1DTW} loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocapSkeletonPoseAnglesVelsL1DTW(BufferedReader stream) throws IOException {
        super((Class) List.class, dtwDistFunction, ObjectMocapPoseAnglesVelsL1.class, stream);
    }

    //************ Overrided class SequenceMocapSkeleton ************//
    @Override
    protected float getStaticPropertiesWeight() {
        return 0.2f;
    }

    @Override
    public float getMaxDistanceStaticProperties() {
        return 6.472839f;
    }

    @Override
    public float getMaxDistanceDynamicProperties() {
        return 1f;
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of
     * {@link SequenceMocapSkeletonPoseAnglesVelsL1DTW} loaded from the binary
     * input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceMocapSkeletonPoseAnglesVelsL1DTW(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, dtwDistFunction, SequenceMocapSkeletonPoseAnglesVelsL1DTW.class, input, serializator);
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
