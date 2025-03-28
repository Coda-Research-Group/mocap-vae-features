package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.impl.ObjectMotionWordCompositeAutoTuning;
import mcdr.sequence.impl.SequenceMotionWordsCompositeAutoTuning;
import mcdr.test.utils.ObjectCategoryMgmt.Category;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.LocalAbstractObject;
import messif.operations.RankingSingleQueryOperation;

import java.util.Set;

/**
 * Configuration of a body part classifier.
 * 
 * @author David Procházka
 */
final class BodyPartConfig extends Config<BodyPartResult> {

    BodyPartConfig(Category invocationCategory, Set<Category> classificationCategories, int k, ObjectMgmt dataMgmt, BodyPartResult result) {
        super(invocationCategory, classificationCategories, k, dataMgmt, result);
    }

    @Override
    public RankingSingleQueryOperation evaluateQuery(LocalAbstractObject queryObject) {
        SequenceMotionWordsCompositeAutoTuning.distanceFunction = getResult().distanceFunction();
        ObjectMotionWordCompositeAutoTuning.matchingFunction = getResult().matchingFunction();

        return evaluate(queryObject);
    }

    @Override
    public String toString() {
        return "BodyPartConfig{" +
                "result=" + getResult() +
                '}';
    }
}
