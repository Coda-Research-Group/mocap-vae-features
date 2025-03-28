package mcdr.objects.classification.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectClassifier extends ObjectMultiCategoryClassifier {

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectClassifier}.
     *
     * @param objectToCategoryMap map associating the object id with the
     * category
     */
    public ObjectClassifier(Map<String, String> objectToCategoryMap) {
        super(new HashMap<String, List<String>>(objectToCategoryMap.size()));
        for (Map.Entry<String, String> objectCategoryEntry : objectToCategoryMap.entrySet()) {
            List<String> categoryList = new ArrayList<>(1);
            categoryList.add(objectCategoryEntry.getValue());
            objectToCategoriesMap.put(objectCategoryEntry.getKey(), categoryList);
        }
    }

    /**
     * Creates a new instance of {@link ObjectClassifier}.
     *
     * @param objectToCategoryMapFile file which contains the CSV-like (i.e.,
     * key;value) map associating the object id with the category
     * @throws java.io.FileNotFoundException
     */
    public ObjectClassifier(File objectToCategoryMapFile) throws FileNotFoundException, IOException {
        super(objectToCategoryMapFile);
    }

    //************ Methods ************//
    /**
     * Returns the map associating the object id with the category.
     *
     * @return map associating the object id with the category
     */
    public Map<String, String> getObjectToCategoryMap() {
        Map<String, String> rtv = new HashMap<>();
        for (Map.Entry<String, List<String>> objectToCategoriesEntry : objectToCategoriesMap.entrySet()) {
            rtv.put(objectToCategoriesEntry.getKey(), objectToCategoriesEntry.getValue().get(0));
        }
        return rtv;
    }
}
