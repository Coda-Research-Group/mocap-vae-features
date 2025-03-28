package mcdr.sequence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import mcdr.objects.ObjectMocapPose;
import mcdr.objects.impl.Extremum;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectArray;
import messif.objects.keys.AbstractObjectKey;
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
public abstract class SequenceMocap<I extends ObjectMocapPose> extends MetaObjectArray implements IndexableSequence<List<I>> {

    //************ Attributes ************//
    // class id for serialization
    private static final long serialVersionUID = 1L;
    // class of the sequence data
    private final Class<List<I>> sequenceDataClass;
    // distance function comparing items of sequences of this type {@link #sequenceDataClass}
    @Deprecated
    protected transient DistanceFunction<Sequence<List<I>>> distFunction;
    // the {@link #originalSequence} that the {@link #sequenceData} comes from - can be null
    private final SequenceMocap<I> originalSequence;
    // locator of the original sequence, if this is a subsequence
    protected final String originalSequenceLocator;
    // offset in the {@link #originalSequence} that the {@link #sequenceData} comes from
    private final int originalOffset;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link SequenceMocap}.
     *
     * @param sequenceDataClass class of the sequence data
     * @param distFunction distance function comparing items of sequences of
     * this type {@link #sequenceDataClass}
     * @param poses list of poses representing this sequence
     */
    public SequenceMocap(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, List<I> poses) {
        this(sequenceDataClass, distFunction, poses, null, -1, false);
    }

    /**
     * Creates a new instance of {@link SequenceMocap}.
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
    public SequenceMocap(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, List<I> poses, SequenceMocap<I> originalSequence, int offset, boolean storeOrigSeq) {
        super((String) null, poses);
        this.sequenceDataClass = sequenceDataClass;
        this.distFunction = distFunction;
        this.originalSequence = storeOrigSeq ? originalSequence : null;
        this.originalSequenceLocator = (originalSequence == null) ? null : originalSequence.getLocatorURI();
        this.originalOffset = offset;
    }

    /**
     * Creates a new instance of {@link SequenceMocap} loaded from the stream.
     * As soon as the sequence is loaded, the identification of this sequence is
     * propagated to all the poses and also the position (i.e., the frame
     * number) of each pose is set.
     *
     * @param sequenceDataClass class of the sequence data
     * @param distFunction distance function comparing items of sequences of
     * this type {@link #sequenceDataClass}
     * @param poseClass class of the sequence item
     * @param stream stream from which the sequence is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public SequenceMocap(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, Class<I> poseClass, BufferedReader stream) throws IOException {
        super(stream, poseClass);
        this.sequenceDataClass = sequenceDataClass;
        this.distFunction = distFunction;
        this.originalSequence = null;
        this.originalSequenceLocator = null;
        this.originalOffset = -1;

        // Sets the sequence identification and frame number to all the poses
        setPoseKeyAndNumber();
    }

    //************ Methods ************//
    /**
     * Sets the sequence identification and frame number to all the poses.
     */
    protected final void setPoseKeyAndNumber() {
        AbstractObjectKey sequenceKey = getObjectKey();
        int frameNo = 0;
        for (I pose : getObjects()) {
            pose.setObjectKey(sequenceKey);
            pose.setFrameNo(frameNo);
            frameNo++;
        }
    }

    /**
     * Returns id of this motion. The sequence id is considered as the starting
     * part of sequence locator up to the first occurrence of character '_'.
     *
     * @return id of this motion
     */
    public String getSequenceId() {
        // Parses the sequence identification
        if (getLocatorURI() != null) {
            return getLocatorURI().split("_")[0];
        } else {
            return (originalSequenceLocator == null) ? null : originalSequenceLocator.split("_")[0];
        }
    }

    /**
     * Returns the number of joints the body model consists of.
     *
     * @return the number of body model joints
     */
    public int getJointCount() {
        return (getObjectCount() == 0) ? -1 : getObject(0).getJointCoordinates().length;
    }

    /**
     * Returns distance function comparing items of sequences of this type
     * {@link #sequenceDataClass}.
     *
     * @return distance function comparing items of sequences of this type
     * {@link #sequenceDataClass}
     */
    public DistanceFunction<Sequence<List<I>>> getDistanceFunction() {
        return distFunction;
    }

    /**
     * Creates the clone of this sequence.
     *
     * @return clonned sequence
     */
    public SequenceMocap<I> duplicate() {
        ArrayList<ObjectMocapPose> poseList = new ArrayList<>(getObjectCount());
        for (ObjectMocapPose pose : getObjects()) {
            poseList.add(pose.duplicate());
        }
        SequenceMocap<I> sequence;
        try {
            sequence = getClass().getConstructor(List.class, getClass(), int.class, boolean.class).newInstance(new Object[]{poseList, null, getOffset(), false});
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
        sequence.setObjectKey(new AbstractObjectKey(getLocatorURI()));
        return sequence;
    }

    //************ Trajectory Processing Methods ************//
    /**
     * Computes the L2 distance between two coordinates.
     *
     * @param c1 first coordinate [x, y, z]
     * @param c2 second coordinate [x, y, z]
     * @return the L2 distance between two coordinates
     */
    public static float getJointsDistL2(float[] c1, float[] c2) {
        float rtv = 0f;
        for (int i = 0; i < c1.length; i++) {
            rtv += Math.pow(c1[i] - c2[i], 2f);
        }
        return (float) Math.sqrt(rtv);
    }

    /**
     * Computes the total distance of a given trajectory.
     *
     * @param jointIndex index of joint that determines the given trajectory
     * @return the total distance of a given trajectory
     */
    public float computeJointTrajectoryDist(int jointIndex) {
        return computeJointTrajectoryDist(jointIndex, 0, getObjectCount());
    }

    /**
     * Computes the distance of a given trajectory within a given frame
     * interval.
     *
     * @param jointIndex index of joint that determines the given trajectory
     * @param fromFrame index of frame from which the trajectory distance is
     * computed (inclusive)
     * @param toFrame index of frame to which the trajectory distance is
     * computed (exclusive)
     * @return the distance of a given trajectory within a given frame interval
     */
    public float computeJointTrajectoryDist(int jointIndex, int fromFrame, int toFrame) {
        float rtv = 0f;

        // computes the trajectory distance of the specific joint
        for (int f = fromFrame; f < toFrame - 1; f++) {
            rtv += getJointsDistL2(getObject(f).getJointCoordinates()[jointIndex], getObject(f + 1).getJointCoordinates()[jointIndex]);
        }
        return rtv;
    }

    /**
     * Computes the distance between the two most distance coordinates of a
     * given trajectory.
     *
     * @param jointIndex index of joint that determines the given trajectory
     * @return the distance between the two most distance coordinates of a given
     * trajectory
     */
    public float computeJointTrajectoryMinMaxDist(int jointIndex) {
        float maxDist = Float.MIN_VALUE;
        for (int f = 0; f < getObjectCount(); f++) {
            for (int f2 = f + 1; f2 < getObjectCount(); f2++) {
                float dist = getJointsDistL2(getObject(f).getJointCoordinates()[jointIndex], getObject(f2).getJointCoordinates()[jointIndex]);
                if (maxDist < dist) {
                    maxDist = dist;
                }
            }
        }
        return maxDist;
    }

    /**
     * Computes the centroid coordinate of a given trajectory.
     *
     * @param jointIndex index of joint that determines the given trajectory
     * @return the centroid coordinate of a given trajectory
     */
    public float[] computeJointTrajectoryCentroid(int jointIndex) {
        float[] rtv = new float[3];
        Arrays.fill(rtv, 0f);

        // computes the trajectory centroid of the specific joint
        for (int f = 0; f < getObjectCount(); f++) {
            for (int i = 0; i < 3; i++) {
                rtv[i] += getObject(f).getJointCoordinates()[jointIndex][i];
            }
        }
        for (int i = 0; i < 3; i++) {
            rtv[i] /= getObjectCount();
        }
        return rtv;
    }

    /**
     * Computes the standard deviation of distances among the joint coordinates
     * and their centroid for a given trajectory.
     *
     * @param jointIndex index of joint that determines the given trajectory
     * @return the standard deviation of distances among the joint coordinates
     * and their centroid for a given trajectory
     */
    public float computeJointTrajectoryCentroidDeviation(int jointIndex) {
        float rtv = 0f;

        // computes the trajectory centroid of the specific joint
        float[] centroid = computeJointTrajectoryCentroid(jointIndex);

        for (int f = 0; f < getObjectCount(); f++) {
            float dist = getJointsDistL2(centroid, getObject(f).getJointCoordinates()[jointIndex]);
            rtv += dist * dist;
        }
        return (float) Math.sqrt(rtv / getObjectCount());
    }

    /**
     * Computes the total distance of a given trajectory on a given axis.
     *
     * @param jointIndex index of joint that determines the given trajectory
     * @param axisIndex index of axis (x=0, y=1, z=1)
     * @return the total distance of a given trajectory on a given axis
     */
    public float computeJointAxisTrajectoryDist(int jointIndex, int axisIndex) {
        float rtv = 0f;

        // computes the trajectory distance of the specific joint on the specific axis
        for (int f = 0; f < getObjectCount() - 1; f++) {
            rtv += Math.abs(getObject(f).getJointCoordinates()[jointIndex][axisIndex] - getObject(f + 1).getJointCoordinates()[jointIndex][axisIndex]);
        }
        return rtv;
    }

    /**
     * Returns either the minimum, or maximum coordinate xyz value of any joint.
     * The minimum is returned when the parameter is set to true, otherwise the
     * maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @return the minimum/maximum coordinate xyz value of any joint
     */
    public float getExtremalCoordValue(boolean minValue) {
        float extremalValue = (minValue) ? Float.MAX_VALUE : Float.MIN_VALUE;
        for (int i = 0; i < getObjectCount(); i++) {
            for (float[] coords : getObject(i).getJointCoordinates()) {
                for (int j = 0; j < coords.length; j++) {
                    if (!Float.isNaN(coords[j])) {
                        if ((minValue) ? coords[j] < extremalValue : coords[j] > extremalValue) {
                            extremalValue = coords[j];
                        }
                    }
                }
            }
        }
        return extremalValue;
    }

    /**
     * Returns either the minimum, or maximum coordinate xyz value of the
     * specified joint. The minimum is returned when the parameter is set to
     * true, otherwise the maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @param jointIdx index of the joint whose the minimum/maximum coordinate
     * xyz value is determined
     * @return the minimum/maximum coordinate xyz value of the specified joint
     */
    public float getExtremalJointCoordValue(boolean minValue, int jointIdx) {
        float extremalValue = (minValue) ? Float.MAX_VALUE : Float.MIN_VALUE;
        for (int i = 0; i < getObjectCount(); i++) {
            for (float coordValue : getObject(i).getJointCoordinates()[jointIdx]) {
                if (!Float.isNaN(coordValue)) {
                    if ((minValue) ? coordValue < extremalValue : coordValue > extremalValue) {
                        extremalValue = coordValue;
                    }
                }
            }
        }
        return extremalValue;
    }

    /**
     * Returns either the minimum, or maximum coordinate value of the specified
     * axis. The minimum is returned when the parameter is set to true,
     * otherwise the maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @param axisIdx index of the axis (x=0, y=1, z=2)
     * @return the minimum/maximum coordinate value of the specified axis and
     * joint
     */
    public float getExtremalAxisCoordValue(boolean minValue, int axisIdx) {
        float extremalValue = (minValue) ? Float.MAX_VALUE : Float.MIN_VALUE;
        for (int i = 0; i < getObjectCount(); i++) {
            for (float[] jointCoordinate : getObject(i).getJointCoordinates()) {
                float coordValue = jointCoordinate[axisIdx];
                if (!Float.isNaN(coordValue)) {
                    if ((minValue) ? coordValue < extremalValue : coordValue > extremalValue) {
                        extremalValue = coordValue;
                    }
                }
            }
        }
        return extremalValue;
    }

    /**
     * Returns either the minimum, or maximum coordinate value of the specified
     * axis and joint. The minimum is returned when the parameter is set to
     * true, otherwise the maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @param jointIdx index of the joint whose the minimum/maximum coordinate
     * xyz value is determined
     * @param axisIdx index of the axis (x=0, y=1, z=2)
     * @return the minimum/maximum coordinate value of the specified axis and
     * joint
     */
    public float getExtremalJointAxisCoordValue(boolean minValue, int jointIdx, int axisIdx) {
        float extremalValue = (minValue) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        for (int i = 0; i < getObjectCount(); i++) {
            float coordValue = getObject(i).getJointCoordinates()[jointIdx][axisIdx];
            if (!Float.isNaN(coordValue)) {
                if ((minValue) ? coordValue < extremalValue : coordValue > extremalValue) {
                    extremalValue = coordValue;
                }
            }
        }
        return extremalValue;
    }

    /**
     * Returns either the minimum, or maximum coordinate value of the specified axis and joint.
     * Based on {@link #getExtremalJointAxisCoordValue(boolean, int, int)}.
     *
     * @param extremum decides whether the minimum or maximum coordinate value is returned
     * @param jointIdx index of the joint whose the minimum/maximum coordinate xyz value is determined
     * @param axisIdx  index of the axis (x=0, y=1, z=2)
     * @return the minimum/maximum coordinate value of the specified axis and joint
     * @author David Procházka
     */
    public float getExtremalJointAxisCoordValue(Extremum extremum, int jointIdx, int axisIdx) {
        var values = getObjects()
                .stream()
                .map(object -> object.getJointCoordinates()[jointIdx][axisIdx])
                .filter(value -> !Float.isNaN(value))
                .toList();

        return switch (extremum) {
            case MAXIMUM -> Collections.max(values);
            case MINIMUM -> Collections.min(values);
        };
    }

    /**
     * Returns either the minimum, or maximum coordinate value index of the specified axis and joint.
     * Based on {@link #getExtremalJointAxisCoordValue(boolean, int, int)}.
     *
     * @param extremum decides whether the minimum or maximum coordinate value index is returned
     * @param jointIdx index of the joint whose the minimum/maximum coordinate xyz value is determined
     * @param axisIdx  index of the axis (x=0, y=1, z=2)
     * @return the minimum/maximum coordinate value index of the specified axis and joint
     * @author David Procházka
     */
    public int getExtremalJointAxisCoordValueIndex(Extremum extremum, int jointIdx, int axisIdx) {
        var indexToValueMapping = IntStream
                .range(0, getObjectCount())
                .mapToObj(i -> Map.entry(i, getObject(i).getJointCoordinates()[jointIdx][axisIdx]))
                .filter(entry -> !Float.isNaN(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return switch (extremum) {
            case MAXIMUM -> Collections.max(indexToValueMapping.entrySet(), Map.Entry.comparingByValue())
                                       .getKey();
            case MINIMUM -> Collections.min(indexToValueMapping.entrySet(), Map.Entry.comparingByValue())
                                       .getKey();
        };
    }

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
        if (!(obj instanceof SequenceMocap)) {
            return LocalAbstractObject.UNKNOWN_DISTANCE;
        }
        SequenceMocap<I> objSequence = (SequenceMocap<I>) obj;
        return objSequence.getDistanceFunction().getDistance(this, objSequence);
    }

    @Override
    public int getSize() {
        if (objects.length == 0) {
            return 0;
        } else {
            return objects[0].getSize() * objects.length;
        }
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof SequenceMocap)) {
            return false;
        }
        SequenceMocap<I> o = (SequenceMocap<I>) obj;
        if (getSequenceLength() != o.getSequenceLength()) {
            return false;
        }
        for (int i = 0; i < getSequenceLength(); i++) {
            if (!objects[i].dataEquals(o.objects[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(objects);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        stream.write((getObjectCount() + ";mcdr.objects.ObjectMocapPose\n").getBytes());
        super.writeData(stream);
    }

    //************ Implemented interface IndexableSequence ************//
    @Override
    public LocalAbstractObject getSequence() {
        return this;
    }

    @Override
    public LocalAbstractObject getSequenceItem(int offset) {
        return getObjects().get(offset);
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link SequenceMocap} loaded from the binary
     * input buffer.
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
    protected SequenceMocap(Class<List<I>> sequenceDataClass, DistanceFunction<Sequence<List<I>>> distFunction, Class<? extends SequenceMocap<I>> sequenceClass, BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.sequenceDataClass = sequenceDataClass;
        this.distFunction = distFunction;
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
    public Class<List<I>> getSequenceDataClass() {
        return sequenceDataClass;
    }

    @Override
    public List<I> getSubsequenceData(int from, int to) {
        return getObjects().subList(from, to);
    }

    @Override
    public Sequence<List<I>> getOriginalSequence() {
        return originalSequence;
    }

    @Override
    public int getOffset() {
        return originalOffset;
    }

    @Override
    public String getOriginalSequenceLocator() {
        return (originalSequenceLocator != null) ? originalSequenceLocator : getLocatorURI();
    }

    @Override
    public float getPiecewiseDist(int thisPieceOffset, Sequence<List<I>> other, int otherPieceOffset) {
        return getObjects().get(thisPieceOffset).getDistance(other.getSequenceData().get(otherPieceOffset));
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        String rtv = " (locator: ";
        if (getObjectKey() == null) {
            rtv += "null";
        } else {
            rtv += getObjectKey().toString();
        }
        rtv += ", length: " + getSequenceLength() + ")";
        return rtv;
    }
}
