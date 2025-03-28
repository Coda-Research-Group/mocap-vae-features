package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.test.utils.ObjectCategoryMgmt.Category;

import java.util.Set;

/**
 * Captures the association between an invocation category and a set of classification categories.
 *
 * @author David Procházka
 */
record CategoryAssociation(
        Category invocationCategory,
        Set<Category> classificationCategories
) {
    @Override
    public String toString() {
        return invocationCategory + " -> " + classificationCategories;
    }
}
