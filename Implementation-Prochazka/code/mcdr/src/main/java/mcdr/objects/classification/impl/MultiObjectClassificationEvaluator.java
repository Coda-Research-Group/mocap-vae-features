package mcdr.objects.classification.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MultiObjectClassificationEvaluator {

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link MultiObjectClassificationEvaluator}.
     */
    public MultiObjectClassificationEvaluator() {
    }

    //************ Methods ************//
    /**
     * Returns reversed modality weight.
     *
     * @param modalityWeight to be reversed
     * @return reversed modality weight
     */
    public static float getReverseModalityWeight(float modalityWeight) {
        return 1f - modalityWeight;
    }

    /**
     * Aggregates individual classifications into a result classification.
     *
     * @param weightedClassifications list of evaluated object classifications
     * along with their weights
     * @return aggregated classification
     */
    public ObjectClassification evaluate(List<Entry<? extends ObjectClassification, Float>> weightedClassifications) {

        // Weight check
        float[] weights = new float[weightedClassifications.size()];
        float accumWeight = 0f;
        for (int i = 0; i < weightedClassifications.size(); i++) {
            Float weight = weightedClassifications.get(i).getValue();
            weights[i] = (weight == null || weight < 0f) ? 0f : weight;
            accumWeight += weights[i];
        }

        // Normalizes weights to interval [0, 1]
        if (accumWeight > 0f) {
            for (int i = 0; i < weightedClassifications.size(); i++) {
                weights[i] /= accumWeight;
            }
        }

        // Aggregates classifications
        Map<String, Float> confidenceMap = new HashMap<>();
        for (int i = 0; i < weightedClassifications.size(); i++) {
            ObjectClassification classification = weightedClassifications.get(i).getKey();
            SortedSet<Entry<String, Float>> classificationEntries = classification.getSortedClassificationEntries();
            if (classificationEntries != null) {
                for (Entry<String, Float> entry : classificationEntries) {
                    Float objectConfidence = confidenceMap.get(entry.getKey());
                    if (objectConfidence == null) {
                        objectConfidence = 0f;
                    }
                    objectConfidence += entry.getValue() * weights[i];
                    confidenceMap.put(entry.getKey(), objectConfidence);
                }
            }
        }

        // Normalizes confidences of objects
        float totalConfidence = 0f;
        for (float confidence : confidenceMap.values()) {
            totalConfidence += confidence;
        }
        if (totalConfidence == 0f) {
            confidenceMap.clear();
        } else {
            for (Entry<String, Float> entry : confidenceMap.entrySet()) {
                confidenceMap.put(entry.getKey(), entry.getValue() / totalConfidence);
            }
        }

        return new ObjectClassification(confidenceMap);
    }
}
