package mcdr.objects.classification.impl.specialized;

import mcdr.objects.classification.impl.ObjectClassification;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import messif.objects.LocalAbstractObject;
import messif.objects.classification.ClassificationException;
import messif.utility.Parametric;
import messif.utility.ParametricBase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David Procházka
 */
public record SpecializedClassifierImpl(
        SpecializedClassifierConfig config
) implements SpecializedClassifier {

    private static final ObjectMultiCategoryClassifier classifier = new ObjectMultiCategoryClassifier(true);

    @Override
    public SpecializedClassificationAnswer classify(LocalAbstractObject queryObject) throws ClassificationException {
        var kNNAnswer = config.evaluateQuery(queryObject)
                              .getAnswer();

        var queryParams = new ParametricBase(new HashMap<>(Map.of("queryObject", queryObject)));
        var objectClassification = classifier.classify(kNNAnswer, queryParams);
        var highestConfidenceEntry = objectClassification.getSortedClassificationEntries()
                                                         .first();

        return new SpecializedClassificationAnswer(
                kNNAnswer,
                highestConfidenceEntry.getKey(),
                highestConfidenceEntry.getValue()
        );
    }

    @Override
    public ObjectClassification classify(LocalAbstractObject queryObject, Parametric queryParameters) throws ClassificationException {
        return classifier.classify(
                config.evaluateQuery(queryObject)
                      .getAnswer(),
                queryParameters
        );
    }

    @Override
    public Class<String> getCategoriesClass() {
        return String.class;
    }
}
