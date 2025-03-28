package mcdr.objects.classification.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import messif.objects.classification.ClassificationWithConfidence;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectClassification implements ClassificationWithConfidence<String> {

    //************ Attributes ************//
    // classification holder - keys represent categories, values represent the confidences
    private final Map<String, Float> confidenceMap;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectClassification}.
     *
     * @param confidenceMap classification holder - keys represent categories,
     * values represent the confidences
     */
    public ObjectClassification(Map<String, Float> confidenceMap) {
        this.confidenceMap = confidenceMap;
    }

    //************ Methods ************//
    /**
     * Returns sorted classification holder - keys represent categories, values
     * represent the confidences
     *
     * @return sorted classification holder - keys represent categories, values
     * represent the confidences
     */
    public SortedSet<Map.Entry<String, Float>> getSortedClassificationEntries() {
        if (confidenceMap == null) {
            return new TreeSet<>();
        }

        // Sorts the entries by their confidence value in decreasing order
        SortedSet<Map.Entry<String, Float>> sortedEntries = new TreeSet<>(
                new Comparator<Map.Entry<String, Float>>() {
                    @Override
                    public int compare(Map.Entry<String, Float> e1, Map.Entry<String, Float> e2) {
                        if (!e1.getValue().equals(e2.getValue())) {
                            return -e1.getValue().compareTo(e2.getValue());
                        } else {
                            return e1.getKey().compareTo(e2.getKey());
                        }

                    }
                }
        );
        sortedEntries.addAll(confidenceMap.entrySet());
        return sortedEntries;
    }

    //************ Implemented interface Classification ************//
    @Override
    public Class<? extends String> getStoredClass() {
        return String.class;
    }

    @Override
    public int size() {
        return confidenceMap.size();
    }

    @Override
    public boolean contains(String category) throws NullPointerException {
        return confidenceMap.containsKey(category);
    }

    @Override
    public Iterator<String> iterator() {
        return confidenceMap.keySet().iterator();
    }

    //************ Implemented interface ClassificationWithConfidence ************//
    @Override
    public float getConfidence(String category) throws NoSuchElementException {
        return confidenceMap.get(category);
    }

    @Override
    public float getLowestConfidence() {
        return 0f;
    }

    @Override
    public float getHighestConfidence() {
        return 1f;
    }

}
