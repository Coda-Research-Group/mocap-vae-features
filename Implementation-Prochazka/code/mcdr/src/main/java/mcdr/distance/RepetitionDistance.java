package mcdr.distance;

import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import mcdr.sequence.impl.SequenceMotionWordsCompositeAutoTuning;
import messif.objects.DistanceFunction;
import smf.sequences.DistanceAllowsNonEquilength;
import smf.sequences.Sequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@code d_{repeated}} distance.
 * 
 * @author David Procházka
 */
public class RepetitionDistance implements DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>>, DistanceAllowsNonEquilength {

    /**
     * How many times should the shorter sequence be repeated
     */
    private static final int REPETITIONS = 2;

    private final DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction;

    public RepetitionDistance(DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction) {
        this.distanceFunction = distanceFunction;
    }

    @Override
    public float getDistance(Sequence<List<ObjectMotionWordCompositeAutoTuning>> lhs, Sequence<List<ObjectMotionWordCompositeAutoTuning>> rhs) {
        var shorterSequence = lhs.getSequenceLength() <= rhs.getSequenceLength() ? lhs : rhs;
        var longerSequence = lhs.getSequenceLength() > rhs.getSequenceLength() ? lhs : rhs;

        var shorterDataRepeated = new ArrayList<ObjectMotionWordCompositeAutoTuning>();
        for (int i = 0; i < REPETITIONS; i++) {
            shorterDataRepeated.addAll(shorterSequence.getSequenceData());
        }
        var shorterSequenceRepeated = new SequenceMotionWordsCompositeAutoTuning(shorterDataRepeated);

        float repeatedSequenceDistance = distanceFunction.getDistance(shorterSequenceRepeated, longerSequence);
        float originalDistance = distanceFunction.getDistance(shorterSequence, longerSequence);

        return repeatedSequenceDistance < originalDistance ? Float.POSITIVE_INFINITY : originalDistance;
    }

    @Override
    public Class<? extends Sequence<List<ObjectMotionWordCompositeAutoTuning>>> getDistanceObjectClass() {
        return (Class) Sequence.class;
    }

    @Override
    public String toString() {
        return "RepetitionDistance{" + distanceFunction + '}';
    }
}
