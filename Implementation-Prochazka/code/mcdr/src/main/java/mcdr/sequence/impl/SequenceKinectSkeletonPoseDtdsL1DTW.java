package mcdr.sequence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseDtdsL1;
import mcdr.sequence.SequenceMocapSkeleton;
import messif.objects.DistanceFunction;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import smf.modules.distance.DTWSequenceDist;
import smf.sequences.Sequence;

/**
 *
 * @author Jakub Valcik, xvalcik@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceKinectSkeletonPoseDtdsL1DTW extends SequenceMocapSkeleton<ObjectMocapPoseDtdsL1> {

    // DTW distance function
    private static final DistanceFunction<Sequence<List<ObjectMocapPoseDtdsL1>>> dtwDistFunction = new DTWSequenceDist<>();

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceKinectSkeletonPoseDtdsL1DTW}.
     *
     * @param poses list of poses representing this sequence
     * @param boneLengths skeleton proportions
     */
    public SequenceKinectSkeletonPoseDtdsL1DTW(List<ObjectMocapPoseDtdsL1> poses, float[] boneLengths) {
        super((Class) List.class, dtwDistFunction, poses, boneLengths);
    }

    /**
     * Creates a new instance of {@link SequenceKinectSkeletonPoseDtdsL1DTW}.
     *
     * @param poses list of poses representing this sequence
     * @param originalSequence the {@link #originalSequence} that the
     * {@link #sequenceData} comes from - can be null
     * @param offset locator of the original sequence, if this is a subsequence
     * @param storeOrigSeq indicates whether the original sequence will be
     * stored, or not - it will be null
     */
    public SequenceKinectSkeletonPoseDtdsL1DTW(List<ObjectMocapPoseDtdsL1> poses, SequenceKinectSkeletonPoseDtdsL1DTW originalSequence, int offset, boolean storeOrigSeq) {
        super((Class) List.class, dtwDistFunction, poses, originalSequence, offset, storeOrigSeq);
    }

    /**
     * Creates a new instance of {@link SequenceKinectSkeletonPoseDtdsL1DTW}
     * loaded from the stream.
     *
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceKinectSkeletonPoseDtdsL1DTW(BufferedReader stream) throws IOException {
        super((Class) List.class, dtwDistFunction, ObjectMocapPoseDtdsL1.class, stream);
    }

    //************ Overrided class SequenceMocapSkeleton ************//
    @Override
    protected float getStaticPropertiesWeight() {
        return 0.0f;
    }

    @Override
    public float getMaxDistanceStaticProperties() {
        return 0.26226896f; // mean distance in Kinect
    }

    @Override
    public float getMaxDistanceDynamicProperties() {
        return 51.205635f; // mean distance in Kinect
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceKinectSkeletonPoseDtdsL1DTW}
     * loaded from the binary input buffer.
     *
     * @param input buffer from which the sequence is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected SequenceKinectSkeletonPoseDtdsL1DTW(BinaryInput input, BinarySerializator serializator) throws IOException {
        super((Class) List.class, dtwDistFunction, SequenceKinectSkeletonPoseDtdsL1DTW.class, input, serializator);
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
