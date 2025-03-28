package mcdr.distance;

import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import mcdr.sequence.impl.SequenceMotionWordsCompositeAutoTuning;
import messif.objects.DistanceFunction;
import smf.sequences.DistanceAllowsNonEquilength;
import smf.sequences.Sequence;

/**
 * Implementation of a supervised matching on the global level in the two-stage classification framework.
 * 
 * @author David Procházka
 */
public class SupervisedLevenshteinDistance<T> implements DistanceFunction<Sequence<T>>, DistanceAllowsNonEquilength {

    @Override
    public float getDistance(Sequence<T> lhs, Sequence<T> rhs) {
        var left = (SequenceMotionWordsCompositeAutoTuning) lhs;
        var right = (SequenceMotionWordsCompositeAutoTuning) rhs;

        if (left.bodyPart == null && right.bodyPart == null) {
            throw new IllegalArgumentException("No body part specified");
        }

        if (left.bodyPart == null) {
            ObjectMotionWordCompositeAutoTuning.matchingFunction = right.bodyPart::match;
        } else if (right.bodyPart == null) {
            ObjectMotionWordCompositeAutoTuning.matchingFunction = left.bodyPart::match;
        } else {
            ObjectMotionWordCompositeAutoTuning.matchingFunction = (a, b) -> left.bodyPart.match(a, b) && right.bodyPart.match(a, b);
        }

        return new LevenshteinDistance<T>().getDistance(lhs, rhs);
    }

    @Override
    public Class<? extends Sequence<T>> getDistanceObjectClass() {
        return (Class) Sequence.class;
    }

    @Override
    public String toString() {
        return "SupervisedLevenshteinDistance";
    }
}
