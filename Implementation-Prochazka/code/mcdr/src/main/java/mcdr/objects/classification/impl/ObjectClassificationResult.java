package mcdr.objects.classification.impl;

import mcdr.test.utils.ObjectCategoryMgmt.Category;
import messif.objects.LocalAbstractObject;

/**
 * Holds information about the classification result of a single object.
 *
 * @author David Procházka
 */
public record ObjectClassificationResult(
        LocalAbstractObject object,
        Category classifiedCategory,
        float confidence,
        boolean classifiedCorrectly
) {

    /**
     * Returns true whether the object was misclassified.
     *
     * @return true whether the object was misclassified
     */
    public boolean isMisclassified() {
        return !classifiedCorrectly;
    }
}
