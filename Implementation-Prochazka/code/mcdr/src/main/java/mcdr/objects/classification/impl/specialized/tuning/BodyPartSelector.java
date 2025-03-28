package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.classification.impl.ClassificationResult;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.objects.impl.ObjectBodyPart;
import mcdr.objects.impl.ObjectMotionWordComposite;
import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import mcdr.sequence.impl.SequenceMotionWordsCompositeAutoTuning;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.DistanceFunction;
import messif.objects.classification.ClassificationException;
import smf.sequences.Sequence;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Encapsulates the selection of primary and secondary body parts.
 *
 * @author David Procházka
 */
final class BodyPartSelector {

    private final ObjectMgmt dataMgmt;
    private final ObjectMgmt queryMgmt;
    private final ObjectMultiCategoryClassifier classifier;
    private final int k;

    BodyPartSelector(
            ObjectMgmt dataMgmt,
            ObjectMgmt queryMgmt,
            ObjectMultiCategoryClassifier classifier,
            int k
    ) {
        this.dataMgmt = dataMgmt;
        this.queryMgmt = queryMgmt;
        this.classifier = classifier;
        this.k = k;
    }

    /**
     * Returns information about the selected primary body part.
     *
     * @param selectFrom       set of body parts to select the primary body part from
     * @param distanceFunction Composite MW distance function
     * @return information about the selected primary body part
     * @throws ClassificationException if there was an error during the classification
     */
    BodyPartResult selectPrimaryBodyPart(Set<ObjectBodyPart> selectFrom, DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction) throws ClassificationException {
        Map<ObjectBodyPart, Float> evaluationResult = evaluateBodyParts(selectFrom, bodyPart -> bodyPart::match, distanceFunction);
        Entry<ObjectBodyPart, Float> bestPerformingResult = Collections.max(evaluationResult.entrySet(), Entry.comparingByValue());

        ObjectBodyPart primary = bestPerformingResult.getKey();
        float performance = bestPerformingResult.getValue();

        return new BodyPartResult(
                primary,
                distanceFunction,
                performance
        );
    }

    /**
     * Returns information about the selected secondary body part.
     * The primary body part should not belong to the set from which the secondary body part is chosen.
     *
     * @param primaryBodyPart  previously selected primary body part
     * @param selectFrom       set of body parts to select the secondary body part from
     * @param distanceFunction Composite MW distance function
     * @return information about the selected secondary body part
     * @throws ClassificationException if there was an error during the classification
     */
    BodyPartResult selectSecondaryBodyPart(ObjectBodyPart primaryBodyPart, Set<ObjectBodyPart> selectFrom, DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction) throws ClassificationException {
        Map<ObjectBodyPart, Float> evaluationResult = evaluateBodyParts(selectFrom, bodyPart -> ObjectMotionWordComposite.matchAtLeastOne(List.of(primaryBodyPart, bodyPart)), distanceFunction);
        Entry<ObjectBodyPart, Float> bestPerformingResult = Collections.max(evaluationResult.entrySet(), Entry.comparingByValue());

        ObjectBodyPart secondary = bestPerformingResult.getKey();
        float performance = bestPerformingResult.getValue();

        return new BodyPartResult(
                primaryBodyPart,
                secondary,
                distanceFunction,
                performance
        );
    }

    private Map<ObjectBodyPart, Float> evaluateBodyParts(Set<ObjectBodyPart> selectFrom, Function<? super ObjectBodyPart, ? extends BiPredicate<ObjectMotionWordComposite, ObjectMotionWordComposite>> matchingFunctionForBodyPart, DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction) throws ClassificationException {
        var bodyPartResults = new HashMap<ObjectBodyPart, Float>(selectFrom.size());

        for (var bodyPart : selectFrom) {
            float classificationPerformance = evaluateBodyPart(matchingFunctionForBodyPart.apply(bodyPart), distanceFunction);

            bodyPartResults.put(bodyPart, classificationPerformance);
        }

        return bodyPartResults;
    }

    private float evaluateBodyPart(BiPredicate<ObjectMotionWordComposite, ObjectMotionWordComposite> matchingFunction, DistanceFunction<Sequence<List<ObjectMotionWordCompositeAutoTuning>>> distanceFunction) throws ClassificationException {
        SequenceMotionWordsCompositeAutoTuning.distanceFunction = distanceFunction;
        ObjectMotionWordCompositeAutoTuning.matchingFunction = matchingFunction;

        var knnQueries = dataMgmt.executeKNNQueries(queryMgmt, k);
        ClassificationResult result = dataMgmt.evaluateClassificationWithClassificationResult(classifier, knnQueries);

        return result.performance();
    }
}
