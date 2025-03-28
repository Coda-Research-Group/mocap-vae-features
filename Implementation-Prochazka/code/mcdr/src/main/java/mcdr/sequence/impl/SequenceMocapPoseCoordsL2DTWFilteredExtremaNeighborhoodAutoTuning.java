package mcdr.sequence.impl;

import mcdr.distance.DTWDistance;
import mcdr.objects.impl.Extremum;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2Filtered;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.DistanceFunction;
import smf.sequences.DistanceAllowsNonEquilength;
import smf.sequences.Sequence;

import java.util.List;

import static mcdr.objects.impl.Extremum.MAXIMUM;
import static mcdr.objects.impl.Extremum.MINIMUM;

/**
 * Implementation of the distance function used by the extremum neighborhood classification
 * in the two-stage classification framework.
 *
 * @author David Procházka
 */
public class SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning<T> implements DistanceFunction<Sequence<T>>, DistanceAllowsNonEquilength {

    public static ObjectMgmt originalActionMgmt;
    public static int jointIndex = -1;
    public static int axisIndex = -1;
    public static float percentage = -1;
    public static Extremum extremum;

    /**
     * Returns a list of objects around extrema (based on {@code indexOfExtrema}).
     * Each objects is above/below (based on {@link #extremum}) {@code thresholdDistance}.
     * The produced list is a consecutive sublist of {@code sequence} objects.
     *
     * @param sequence          the sequence of objects
     * @param thresholdDistance the threshold distance, which all objects should satisfy
     * @param indexOfExtrema    the index of extrema from the sequence
     * @return a list of objects around extrema
     */
    private static List<ObjectMocapPoseCoordsL2Filtered> getNeighborhoodAroundExtrema(SequenceMocapPoseCoordsL2DTWFiltered sequence, float thresholdDistance, int indexOfExtrema) {
        int leftIndex = indexOfExtrema;
        int rightIndex = indexOfExtrema;

        while (leftIndex > 0 && isObjectWithinDistance(sequence.getObject(leftIndex - 1), thresholdDistance)) {
            leftIndex--;
        }
        while (rightIndex < sequence.getObjectCount() && isObjectWithinDistance(sequence.getObject(rightIndex), thresholdDistance)) {
            rightIndex++;
        }

        return sequence.getObjects().subList(leftIndex, rightIndex);
    }

    private static boolean isObjectWithinDistance(ObjectMocapPoseCoordsL2Filtered object, float thresholdDistance) {
        var objectDistance = object.getJointCoordinates()[jointIndex][axisIndex];

        return switch (extremum) {
            case MAXIMUM -> objectDistance >= thresholdDistance;
            case MINIMUM -> objectDistance <= thresholdDistance;
        };
    }

    private static float calculateThresholdDistance(SequenceMocapPoseCoordsL2DTWFiltered sequence) {
        float thresholdDistance = sequence.getExtremalJointAxisCoordValue(extremum, jointIndex, axisIndex);
        float sequenceRange = Math.abs(sequence.getExtremalJointAxisCoordValue(MAXIMUM, jointIndex, axisIndex) - sequence.getExtremalJointAxisCoordValue(MINIMUM, jointIndex, axisIndex));

        return thresholdDistance + switch (extremum) {
            case MAXIMUM -> -percentage * sequenceRange;
            case MINIMUM -> percentage * sequenceRange;
        };
    }

    @Override
    public float getDistance(Sequence<T> leftMWSequence, Sequence<T> rightMWSequence) {
        var lhsSequenceLocatorURI = ((SequenceMotionWordsCompositeAutoTuning) leftMWSequence).getLocatorURI();
        var rhsSequenceLocatorURI = ((SequenceMotionWordsCompositeAutoTuning) rightMWSequence).getLocatorURI();

        var lhsSequence = (SequenceMocapPoseCoordsL2DTWFiltered) originalActionMgmt.getObject(lhsSequenceLocatorURI);
        var rhsSequence = (SequenceMocapPoseCoordsL2DTWFiltered) originalActionMgmt.getObject(rhsSequenceLocatorURI);

        float lhsThresholdDistance = calculateThresholdDistance(lhsSequence);
        float rhsThresholdDistance = calculateThresholdDistance(rhsSequence);

        int lhsExtremaIndex = lhsSequence.getExtremalJointAxisCoordValueIndex(extremum, jointIndex, axisIndex);
        int rhsExtremaIndex = rhsSequence.getExtremalJointAxisCoordValueIndex(extremum, jointIndex, axisIndex);

        var lhsNeighborhood = getNeighborhoodAroundExtrema(lhsSequence, lhsThresholdDistance, lhsExtremaIndex);
        var rhsNeighborhood = getNeighborhoodAroundExtrema(rhsSequence, rhsThresholdDistance, rhsExtremaIndex);

        var lhsSequenceNeighborhood = new SequenceMocapPoseCoordsL2DTWFiltered(lhsNeighborhood);
        var rhsSequenceNeighborhood = new SequenceMocapPoseCoordsL2DTWFiltered(rhsNeighborhood);

        return new DTWDistance<List<ObjectMocapPoseCoordsL2Filtered>>().getDistance(lhsSequenceNeighborhood, rhsSequenceNeighborhood);
    }

    @Override
    public Class<? extends Sequence<T>> getDistanceObjectClass() {
        return (Class) Sequence.class;
    }
}
