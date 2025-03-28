package mcdr.objects.classification.impl.specialized;

import messif.objects.util.RankedAbstractObject;

import java.util.Iterator;

/**
 * Classification answer from a specialized classifier.
 * 
 * @author David Procházka
 */
public record SpecializedClassificationAnswer(
        Iterator<RankedAbstractObject> kNNAnswer,
        String categoryID,
        float confidence
) {
}
