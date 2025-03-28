package mcdr.objects.classification.impl.specialized;

import mcdr.test.utils.ObjectCategoryMgmt.Category;
import messif.objects.LocalAbstractObject;
import messif.operations.RankingSingleQueryOperation;

/**
 * Configuration of a specialized classifier.
 * 
 * @author David Procházka
 */
public interface SpecializedClassifierConfig {

    /**
     * Evaluates a single kNN query for the supplied query objects.
     *
     * @param queryObject the query object
     * @return the result of the kNN query
     */
    RankingSingleQueryOperation evaluateQuery(LocalAbstractObject queryObject);

    /**
     * Returns an information about this configuration.
     *
     * @return an information about this configuration
     */
    String format();

    /**
     * Returns {@link Category} ID for which this classifier config should be invoked.
     *
     * @return {@link Category} ID for which this classifier config should be invoked
     */
    String getInvocationCategoryId();
}
