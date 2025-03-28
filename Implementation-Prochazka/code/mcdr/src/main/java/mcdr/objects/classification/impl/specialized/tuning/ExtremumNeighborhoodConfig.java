package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.impl.ObjectMocapPoseCoordsL2Filtered;
import mcdr.objects.impl.ObjectMotionWordComposite;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning;
import mcdr.sequence.impl.SequenceMotionWordsCompositeAutoTuning;
import mcdr.test.utils.ObjectCategoryMgmt.Category;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.LocalAbstractObject;
import messif.operations.RankingSingleQueryOperation;

import java.util.Set;

/**
 * Configuration of an extremum neighborhood classifier.
 *
 * @author David Procházka
 */
final class ExtremumNeighborhoodConfig extends Config<ExtremumNeighborhoodResult> {

    private final ObjectMgmt originalActionMgmt;

    ExtremumNeighborhoodConfig(Category invocationCategory, Set<Category> classificationCategories, int k, ObjectMgmt dataMgmt, ExtremumNeighborhoodResult result, ObjectMgmt originalActionMgmt) {
        super(invocationCategory, classificationCategories, k, dataMgmt, result);
        this.originalActionMgmt = originalActionMgmt;
    }

    @Override
    public RankingSingleQueryOperation evaluateQuery(LocalAbstractObject queryObject) {
        var bodyPart = ObjectMotionWordComposite.getBodyPart(getResult().jointIndex());

        ObjectMocapPoseCoordsL2Filtered.jointIds = bodyPart.jointIds();
        SequenceMotionWordsCompositeAutoTuning.distanceFunction = new SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning<>();
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.originalActionMgmt = originalActionMgmt;
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.axisIndex = getResult().axisIndex();
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.jointIndex = getResult().jointIndex();
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.extremum = getResult().extremum();
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.percentage = getResult().percentage();

        return evaluate(queryObject);
    }

    @Override
    public String toString() {
        return "ExtremaNeighborhoodConfig{" +
                "result=" + getResult() +
                '}';
    }
}
