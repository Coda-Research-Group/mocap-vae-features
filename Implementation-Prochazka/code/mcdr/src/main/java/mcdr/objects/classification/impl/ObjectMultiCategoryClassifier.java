package mcdr.objects.classification.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import messif.objects.AbstractObject;
import messif.objects.classification.ClassificationException;
import messif.objects.classification.Classifier;
import messif.objects.util.RankedAbstractObject;
import messif.utility.Parametric;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMultiCategoryClassifier implements Classifier<Iterator<? extends RankedAbstractObject>, String> {

    //************ Attributes ************//
    // special characters dividing the object locator into logical parts: "sequenceId_categoryId_offset_length"
    private static final String LOCATOR_REG_EXP = "_|\\.";
    // special characters dividing the object category part into individual categories: "category1-category2-category3"
    private static final String LOCATOR_CATEGORY_REG_EXP = "-";
    // map associating the object id with the categories it belongs to
    protected final Map<String, List<String>> objectToCategoriesMap;
    // determines whether to also consider the object distance for classification
    protected final boolean weightObjectsByDistance;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMultiCategoryClassifier}.
     */
    public ObjectMultiCategoryClassifier() {
        this.objectToCategoriesMap = null;
        this.weightObjectsByDistance = true;
    }

    /**
     * Creates a new instance of {@link ObjectMultiCategoryClassifier}.
     *
     * @param weightObjectsByDistance determines whether to also consider the
     * object distance for classification
     */
    public ObjectMultiCategoryClassifier(boolean weightObjectsByDistance) {
        this.objectToCategoriesMap = null;
        this.weightObjectsByDistance = weightObjectsByDistance;
    }

    /**
     * Creates a new instance of {@link ObjectMultiCategoryClassifier}.
     *
     * @param objectToCategoriesMap map associating the object id with the
     * categories it belongs to
     */
    public ObjectMultiCategoryClassifier(Map<String, List<String>> objectToCategoriesMap) {
        this.objectToCategoriesMap = objectToCategoriesMap;
        this.weightObjectsByDistance = true;
    }

    /**
     * Creates a new instance of {@link ObjectMultiCategoryClassifier}.
     *
     * @param objectToCategoriesMapFile file which contains the CSV-like (i.e.,
     * key;value) map associating the object id with the category (multiple
     * lines of the same object simulate the multi-categorization)
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public ObjectMultiCategoryClassifier(File objectToCategoriesMapFile) throws FileNotFoundException, IOException {
        this.objectToCategoriesMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(objectToCategoriesMapFile));
        String line = br.readLine();
        while (line != null) {
            String[] lineSplit = line.split(";");
            List<String> categories = objectToCategoriesMap.get(lineSplit[0]);
            if (categories == null) {
                categories = new ArrayList<>();
                objectToCategoriesMap.put(lineSplit[0], categories);
            }
            categories.add(lineSplit[1]);
            line = br.readLine();
        }
        br.close();
        this.weightObjectsByDistance = true;
    }

    //************ Methods ************//
    /**
     * Returns the map associating the object id with its categories.
     *
     * @return map associating the object id with its categories
     */
    public Map<String, List<String>> getObjectToCategoriesMap() {
        return objectToCategoriesMap;
    }

    /**
     * Returns id of the given object. It is implemented as the object
     * locatorURI.
     *
     * @param o object
     * @return id of the given object
     */
    public String getObjectId(AbstractObject o) {
        return o.getLocatorURI();
    }

    /**
     * Returns IDs of categories to which the given object belongs.
     *
     * @param o object
     * @return IDs of categories to which the given object belongs
     */
    public List<String> getObjectCategories(AbstractObject o) {
        if (objectToCategoriesMap != null) {
            return objectToCategoriesMap.get(getObjectId(o));
        }
        return Arrays.asList(o.getLocatorURI().split(LOCATOR_REG_EXP)[1].split(LOCATOR_CATEGORY_REG_EXP));
    }

    //************ Implemented interface Classifier ************//
    @Override
    public ObjectClassification classify(Iterator<? extends RankedAbstractObject> object, Parametric parameters) throws ClassificationException {
        if (object == null) {
            return new ObjectClassification(null);
        }

        // distance to the most farthest object
        float maxDistance = Float.MIN_VALUE;

        // computes the max distance and converts the iterator into list
        List<RankedAbstractObject> objects = new ArrayList<>();
        while (object.hasNext()) {
            RankedAbstractObject o = object.next();
            objects.add(o);
            if (o.getDistance() > maxDistance) {
                maxDistance = o.getDistance();
            }
        }
        if (maxDistance <= 0f) {
            maxDistance = 1f;
        } else {
            maxDistance *= 1.1f;
        }

        // classification holder - keys represent categories, values represent the confidences
        Map<String, Float> confidenceMap = new HashMap<>();
        float confidenceSum = 0f;
        for (RankedAbstractObject o : objects) {
            for (String category : getObjectCategories(o.getObject())) {
                Float confidence = confidenceMap.get(category);
                float objectConfidenceValue = (weightObjectsByDistance) ? 1f - (o.getDistance() / maxDistance) : 1f;
                if (confidence == null) {
                    confidence = objectConfidenceValue;
                } else {
                    confidence += objectConfidenceValue;
                }
                confidenceMap.put(category, confidence);
                confidenceSum += objectConfidenceValue;
            }
        }

        // normalizes confidence values to [0, 1]
        for (Map.Entry<String, Float> entry : confidenceMap.entrySet()) {
            entry.setValue(entry.getValue() / confidenceSum);
        }
        return new ObjectClassification(confidenceMap);
    }

    @Override
    public Class<? extends String> getCategoriesClass() {
        return String.class;
    }

}
