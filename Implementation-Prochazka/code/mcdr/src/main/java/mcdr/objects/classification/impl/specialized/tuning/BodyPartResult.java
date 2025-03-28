package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.impl.ObjectBodyPart;
import mcdr.objects.impl.ObjectMotionWordComposite;
import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import messif.objects.DistanceFunction;
import smf.sequences.Sequence;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * The result of a body part classification method.
 * 
 * @author David Procházka
 */
record BodyPartResult(
        ObjectBodyPart primaryBodyPart,
        ObjectBodyPart secondaryBodyPart,
        DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction,
        BiPredicate<ObjectMotionWordComposite, ObjectMotionWordComposite> matchingFunction,
        float performance
) {
    BodyPartResult(ObjectBodyPart primaryBodyPart, DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction, float performance) {
        this(primaryBodyPart, null, distanceFunction, primaryBodyPart::match, performance);
    }

    BodyPartResult(ObjectBodyPart primaryBodyPart, ObjectBodyPart secondaryBodyPart, DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction, float performance) {
        this(primaryBodyPart, secondaryBodyPart, distanceFunction, ObjectMotionWordComposite.matchAtLeastOne(List.of(primaryBodyPart, secondaryBodyPart)), performance);
    }

    @Override
    public String toString() {
        return "BodyPartResult{" +
                "primaryBodyPart=" + primaryBodyPart +
                ", secondaryBodyPart=" + secondaryBodyPart +
                ", distanceFunction=" + distanceFunction +
                ", performance=" + performance +
                '}';
    }
}
