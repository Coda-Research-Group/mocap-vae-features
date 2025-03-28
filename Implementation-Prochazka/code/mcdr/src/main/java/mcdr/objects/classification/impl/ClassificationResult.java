package mcdr.objects.classification.impl;

import mcdr.test.utils.ObjectCategoryMgmt.Category;

import java.util.List;
import java.util.Map;

/**
 * The result of classification.
 * 
 * @author David Procházka
 */
public record ClassificationResult(
        Map<Category, List<ObjectClassificationResult>> classificationResultPerCategory,
        float performance
) {
}
