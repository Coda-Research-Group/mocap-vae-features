package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.classification.impl.specialized.SpecializedClassifierConfig;
import mcdr.test.utils.ObjectCategoryMgmt.Category;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.LocalAbstractObject;
import messif.operations.RankingSingleQueryOperation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration of a specialized classifier.
 *
 * @author David Procházka
 */
abstract sealed class Config<T> implements SpecializedClassifierConfig permits BodyPartConfig, ExtremumNeighborhoodConfig {

    private final int k;
    private final ObjectMgmt dataMgmt;
    private final T result;
    private final Category invocationCategory;
    private final Set<? extends Category> classificationCategories;

    Config(Category invocationCategory, Set<? extends Category> classificationCategories, int k, ObjectMgmt dataMgmt, T result) {
        this.invocationCategory = invocationCategory;
        this.classificationCategories = Collections.unmodifiableSet(classificationCategories);
        this.k = k;
        this.dataMgmt = dataMgmt;
        this.result = result;
    }

    /**
     * Returns the only kNN query answer of the only category in the map.
     *
     * @param kNNAnswer the kNN query answer
     * @return the only kNN query answer of the only category in the map
     */
    private static RankingSingleQueryOperation extractQueryAnswer(Map<Category, ? extends List<RankingSingleQueryOperation>> kNNAnswer) {
        return kNNAnswer
                .entrySet()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find the query's category"))
                .getValue()
                .get(0);
    }

    @Override
    public String format() {
        return "Category " + invocationCategory + " distinguishes between categories " + Category.format(classificationCategories) + " using " + this;
    }

    @Override
    public String getInvocationCategoryId() {
        return invocationCategory.getId();
    }

    RankingSingleQueryOperation evaluate(LocalAbstractObject queryObject) {
        var queryMgmt = new ObjectMgmt(dataMgmt.getCategoryMgmt());
        queryMgmt.addObject(queryObject, null, null);

        var kNNAnswer = dataMgmt.executeKNNQueries(queryMgmt, k);

        return extractQueryAnswer(kNNAnswer);
    }

    T getResult() {
        return result;
    }
}
