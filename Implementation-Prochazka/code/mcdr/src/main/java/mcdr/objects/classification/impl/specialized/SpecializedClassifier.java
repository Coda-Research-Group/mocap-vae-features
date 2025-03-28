package mcdr.objects.classification.impl.specialized;

import messif.objects.LocalAbstractObject;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifier;

/**
 * Specialized classifier.
 *
 * @author David Procházka
 */
public interface SpecializedClassifier extends Classifier<LocalAbstractObject, String> {

    /**
     * Classifies the given query object.
     *
     * @param queryObject the query object
     * @return the classification answer
     * @throws ClassificationException when the classification fails
     */
    SpecializedClassificationAnswer classify(LocalAbstractObject queryObject) throws ClassificationException;

    /**
     * Returns the configuration of this specialized classifier.
     * 
     * @return the configuration of this specialized classifier
     */
    SpecializedClassifierConfig config();
}
