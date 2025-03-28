package mcdr.test.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectCategoryMgmt {

    // map associating the category ID with the category object
    private final Map<String, Category> categoryIdMap = new HashMap<>();
    // map associating the category ID with its description
    private final Map<String, String> categoryIdDescriptionMap = new HashMap<>();
    // map associating the index with the category
    private final Map<Integer, Category> categoryIndexMap = new HashMap<>();
    // generator of unique indexes for individual categories
    private int categoryIndexGenerator = 0;

    //************ Constructors ************//

    /**
     * Creates a new instance of {@link ObjectCategoryMgmt}.
     *
     * @throws IOException
     */
    public ObjectCategoryMgmt() throws IOException {
        this(null);
    }

    /**
     * Creates a new instance of {@link ObjectCategoryMgmt}.
     *
     * @param categoryDescriptionFile name of the CSV-like file containing IDs of categories along with their
     *                                descriptions
     * @throws IOException
     */
    public ObjectCategoryMgmt(String categoryDescriptionFile) throws IOException {

        // Category descriptions
        if (categoryDescriptionFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(categoryDescriptionFile));
            String line = br.readLine();
            while (line != null) {
                String[] categoryDesc = line.split(";");
                categoryIdDescriptionMap.put(categoryDesc[0], categoryDesc[1]);
                line = br.readLine();
            }
            br.close();
        }
    }

    //************ Methods ************//

    /**
     * Adds the category specified by its ID into the category list. If the
     * category with the specified ID already exists, it is just returned.
     *
     * @param categoryId ID of the category to be added
     * @return category of the specified ID
     */
    public Category getOrCreateObjectCategory(String categoryId) {
        Category category = categoryIdMap.get(categoryId);
        if (category == null) {
            category = new Category(categoryId, categoryIdDescriptionMap.get(categoryId), categoryIndexGenerator++);
            categoryIdMap.put(categoryId, category);
            categoryIndexMap.put(category.index, category);
        }
        return category;
    }

    /**
     * Returns the category specified by its ID.
     *
     * @param id ID of the category to be returned
     * @return category specified by its ID
     */
    public Category getCategoryById(String id) {
        return categoryIdMap.get(id);
    }

    /**
     * Returns the category specified by its index.
     *
     * @param index index of the category to be returned
     * @return category specified by its index
     */
    public Category getCategoryByIndex(int index) {
        return categoryIndexMap.get(index);
    }

    /**
     * Returns the number of categories.
     *
     * @return the number of categories
     */
    public int getCategoryCount() {
        return categoryIdMap.size();
    }

    //************ Classes ************//

    /**
     * Object keeping the information about the category.
     */
    public static class Category {

        public final String id;
        public final String description;
        public final int index;

        public Category(String id, String description, int index) {
            this.id = id;
            this.description = description;
            this.index = index;
        }

        /**
         * Returns a formatted list of the supplied {@link Category} IDs.
         *
         * @param categories a set of categories
         * @return a formatted list of the supplied {@link Category} IDs
         */
        public static String format(Set<? extends Category> categories) {
            return categories.stream()
                             .map(Category::getId)
                             .toList()
                             .toString();
        }


        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return id.equals(((Category) obj).id);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
