package mcdr.test.utils;

import mcdr.objects.classification.impl.ClassificationResult;
import mcdr.objects.classification.impl.ObjectClassification;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.objects.classification.impl.specialized.SpecializedClassifier;
import mcdr.objects.classification.impl.ObjectClassificationResult;
import mcdr.objects.utils.InstantiableCollection;
import mcdr.objects.utils.RankedSortedCollectionDistHashcode;
import mcdr.test.utils.ObjectCategoryMgmt.Category;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.classification.ClassificationException;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.AnswerType;
import messif.operations.RankingQueryOperation;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.query.KNNQueryOperation;
import messif.utility.Parametric;
import messif.utility.ParametricBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMgmt {

    // special characters dividing the object locator into logical parts: "sequenceId_categoryId_offset_length"
    private static final String LOCATOR_REG_EXP = "_|\\.";
    // global manager of categories
    private final ObjectCategoryMgmt categoryMgmt;
    // map associating the specific object with the categories it belongs to
    private final Map<LocalAbstractObject, List<Category>> objectCategoriesMap = new HashMap<>();
    // map associating the specific category with objects that belongs to this category
    private final Map<Category, List<LocalAbstractObject>> categoryObjectsMap = new HashMap<>();

    /**
     * Creates a new instance of {@link ObjectMgmt}.
     *
     * @param categoryMgmt global manager of categories
     */
    public ObjectMgmt(ObjectCategoryMgmt categoryMgmt) {
        this.categoryMgmt = categoryMgmt;
    }

    /**
     * Parses the ID of the parent sequence of the specified object.
     *
     * @param o object to be parsed
     * @return ID of the parent sequence of the specified object
     */
    public static String parseObjectParentSequenceId(AbstractObject o) {
        return parseObjectParentSequenceId(o.getLocatorURI());
    }

    /**
     * Parses the ID of the parent sequence of the specified object.
     *
     * @param locator locator of object to be parsed
     * @return ID of the parent sequence of the specified object
     */
    public static String parseObjectParentSequenceId(String locator) {
        return locator.split(LOCATOR_REG_EXP)[0];
    }

    /**
     * Parses the category to which the specified object belongs.
     *
     * @param o object to be parsed
     * @return the category to which the specified object belongs
     */
    public static String parseObjectCategoryId(AbstractObject o) {
        return parseObjectCategoryId(o.getLocatorURI());
    }

    /**
     * Parses the category to which the specified object belongs.
     *
     * @param locator locator of object to be parsed
     * @return the category to which the specified object belongs
     */
    public static String parseObjectCategoryId(String locator) {
        return locator.split(LOCATOR_REG_EXP)[1];
    }

    /**
     * Parses the beginning frame of the specified object within the parent
     * sequence.
     *
     * @param o object to be parsed
     * @return the beginning frame of the specified object within the parent
     * sequence
     */
    public static int parseObjectOffset(AbstractObject o) {
        return parseObjectOffset(o.getLocatorURI());
    }

    /**
     * Parses the beginning frame of the specified object within the parent
     * sequence.
     *
     * @param locator locator of object to be parsed
     * @return the beginning frame of the specified object within the parent
     * sequence
     */
    public static int parseObjectOffset(String locator) {
        String[] locatorParts = locator.split(LOCATOR_REG_EXP);
        if (locatorParts.length < 3) {
            return 0;
        }
        return (Integer.parseInt(locatorParts[2]));
    }

    /**
     * Parses the length of the specified object (in number of frames).
     *
     * @param o object to be parsed
     * @return the length of the specified object (in number of frames)
     */
    public static int parseObjectLength(AbstractObject o) {
        return parseObjectLength(o.getLocatorURI());
    }

    /**
     * Parses the length of the specified object (in number of frames).
     *
     * @param locator locator of object to be parsed
     * @return the length of the specified object (in number of frames)
     */
    public static int parseObjectLength(String locator) {
        return Integer.parseInt(locator.split(LOCATOR_REG_EXP)[3]);
    }

    public static List<Float> asList(float[] values) {
        List<Float> rtv = new ArrayList<>();
        for (float value : values) {
            rtv.add(value);
        }
        return rtv;
    }

    /**
     * Computes the average value of values within the specified list.
     *
     * @param values list of values whose average is computed
     * @return the average value of values within the specified list
     */
    public static float computeAvgValue(List<Float> values) {
        float sumValue = 0f;
        for (float value : values) {
            sumValue += value;
        }
        return sumValue / values.size();
    }

    /**
     * Computes the mean value of values within the specified list.
     *
     * @param values list of values whose mean is computed
     * @return the mean value of values within the specified list
     */
    public static float computeMeanValue(List<Float> values) {
        Float[] arrayValues = new Float[values.size()];
        values.toArray(arrayValues);
        Arrays.sort(arrayValues);
        return arrayValues[arrayValues.length / 2];
    }

    /**
     * Computes the standard deviation of values within the specified list.
     *
     * @param values list of values whose standard deviation is computed
     * @return the standard deviation of values within the specified list
     */
    public static float computeStddevValue(List<Float> values) {
        float avgValue = computeAvgValue(values);
        float sumDiffSquareValue = 0f;
        for (float value : values) {
            sumDiffSquareValue += (avgValue - value) * (avgValue - value);
        }
        return (float) Math.sqrt(sumDiffSquareValue / values.size());
    }

    public static Map<Category, List<RankingSingleQueryOperation>> cloneCategorizedRankingOperations(Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap, int maxK) throws CloneNotSupportedException {
        Map<Category, List<RankingSingleQueryOperation>> rtv = new HashMap<>();
        // Iterating over individual categories
        for (Map.Entry<Category, List<RankingSingleQueryOperation>> categoryOperationsEntry : categoryOperationsMap.entrySet()) {
            Category queryCategory = categoryOperationsEntry.getKey();
            List<RankingSingleQueryOperation> categoryOperations = new ArrayList<>();
            for (RankingSingleQueryOperation origOp : categoryOperationsEntry.getValue()) {
                RankingSingleQueryOperation op = origOp.clone(false);
                op.resetAnswer();
                Iterator<RankedAbstractObject> objIt = origOp.getAnswer(0, maxK);
                while (objIt.hasNext()) {
                    RankedAbstractObject rao = objIt.next();
                    op.addToAnswer(rao.getObject(), rao.getDistance(), null);
                }
                categoryOperations.add(op);
            }
            rtv.put(queryCategory, categoryOperations);
        }
        return rtv;
    }

    /**
     * Prints information about the kNN classification answer.
     *
     * @param answer the kNN query operation answer
     * @author David Procházka
     */
    private static void printClassificationAnswer(Iterator<RankedAbstractObject> answer) {
        int objectAnswerIndex = 0;

        while (answer.hasNext()) {
            var rankedObject = answer.next();
            var object = (LocalAbstractObject) rankedObject.getObject();

            System.out.println("      answer " + objectAnswerIndex + ": " + object.getLocatorURI() + ": " + rankedObject.getDistance());

            objectAnswerIndex++;
        }
    }

    public ObjectCategoryMgmt getCategoryMgmt() {
        return categoryMgmt;
    }

    /**
     * Returns the category specified by its ID.
     *
     * @param id ID of the category to be returned
     * @return category specified by its ID
     */
    public Category getCategoryById(String id) {
        return categoryMgmt.getCategoryById(id);
    }

    /**
     * Returns the set of all loaded objects.
     *
     * @return the set of all loaded objects
     */
    public Set<LocalAbstractObject> getObjects() {
        return objectCategoriesMap.keySet();
    }

    /**
     * Returns the set of locators of all loaded objects.
     *
     * @return the set of locators of all loaded objects
     */
    public Set<String> getLocators() {
        HashSet<String> result = new HashSet<>();
        for (LocalAbstractObject lao : objectCategoriesMap.keySet()) {
            result.add(lao.getLocatorURI());
        }
        return result;
    }

    /**
     * Returns the object specified by its locator.
     *
     * @param locatorURI specification of the object
     * @return null if the object is not present
     */
    public LocalAbstractObject getObject(String locatorURI) {
        for (LocalAbstractObject o : getObjects()) {
            if (o.getLocatorURI().equals(locatorURI)) {
                return o;
            }
        }
        return null;
    }

    /**
     * Returns the objects whose locator matches the specified regular
     * expression.
     *
     * @param locatorURIRE regular expression of the locator
     * @return list of the objects whose locator matches the specified regular
     * expression
     */
    public List<LocalAbstractObject> getObjects(String locatorURIRE) {
        List<LocalAbstractObject> rtv = new ArrayList<>();
        for (LocalAbstractObject o : getObjects()) {
            if (o.getLocatorURI().matches(locatorURIRE)) {
                rtv.add(o);
            }
        }
        return rtv;
    }

    /**
     * Returns the number of all objects.
     *
     * @return the number of all objects
     */
    public int getObjectCount() {
        return objectCategoriesMap.size();
    }

    /**
     * Returns the set of all categories of objects.
     *
     * @return the set of all categories of objects
     */
    public Set<Category> getCategories() {
        return categoryObjectsMap.keySet();
    }

    /**
     * Returns the objects and their associated categories.
     *
     * @return the objects and their associated categories
     */
    public Map<LocalAbstractObject, List<Category>> getObjectCategories() {
        return objectCategoriesMap;
    }

    /**
     * Returns the categories to which the specified object belongs.
     *
     * @param o object whose categories are returned
     * @return the categories to which the specified object belongs
     */
    public List<Category> getObjectCategories(LocalAbstractObject o) {
        return objectCategoriesMap.get(o);
    }

    /**
     * Returns the number of objects contained in the specified category.
     *
     * @param category category to which the objects belong
     * @return the number of objects contained in the specified category
     */
    public int getObjectCountInCategory(Category category) {
        List<LocalAbstractObject> objects = categoryObjectsMap.get(category);
        return (objects == null) ? 0 : objects.size();
    }

    /**
     * Returns map associating the specific category with objects that belongs
     * to this category.
     *
     * @return map associating the specific category with objects that belongs
     * to this category
     */
    public Map<Category, List<LocalAbstractObject>> getCategoryObjects() {
        return categoryObjectsMap;
    }

    /**
     * Returns map associating the parent sequence with its objects.
     *
     * @return map associating the parent sequence with its objects
     */
    public Map<String, List<LocalAbstractObject>> getParentSequenceObjects() {
        Map<String, List<LocalAbstractObject>> rtv = new HashMap<>();
        for (LocalAbstractObject o : getObjects()) {
            String seqId = parseObjectParentSequenceId(o);
            List<LocalAbstractObject> objects = rtv.get(seqId);
            if (objects == null) {
                objects = new ArrayList<>();
                rtv.put(seqId, objects);
            }
            objects.add(o);
        }
        return rtv;
    }

    /**
     * Returns IDs of parent sequences of all objects.
     *
     * @return IDs of parent sequences of all objects
     */
    public Set<String> getParentSequenceIds() {
        Set<String> parentSequenceIds = new HashSet<>();
        for (LocalAbstractObject o : getObjects()) {
            parentSequenceIds.add(parseObjectParentSequenceId(o));
        }
        return parentSequenceIds;
    }

    public void addObject(LocalAbstractObject o, Set<Category> objectCategories) {
        objectCategoriesMap.put(o, new ArrayList<>(objectCategories));

        // Category to objects association
        for (Category category : objectCategories) {
            List<LocalAbstractObject> objects = categoryObjectsMap.get(category);
            if (objects == null) {
                objects = new ArrayList<>();
                categoryObjectsMap.put(category, objects);
            }
            objects.add(o);
        }
    }

    public Set<Category> addObject(LocalAbstractObject o, String[] ignoredCategoryIds, ObjectMgmt objectMgmtToCategoryParser) {

        // Assigning categories to the loaded objects
        Set<Category> objectCategories = new HashSet<>();
        if (objectMgmtToCategoryParser == null) {
            String objectLocatorCategoryId = parseObjectCategoryId(o);
            if (ignoredCategoryIds != null && Arrays.asList(ignoredCategoryIds).contains(objectLocatorCategoryId)) {
                return null;
            }
            objectCategories.add(categoryMgmt.getOrCreateObjectCategory(objectLocatorCategoryId));
        } else {
            String oParentSequenceId = parseObjectParentSequenceId(o);
            int oOffset = parseObjectOffset(o);
            int oEndIndex = oOffset + parseObjectLength(o) - 1;
            for (Map.Entry<LocalAbstractObject, List<Category>> entry : objectMgmtToCategoryParser.objectCategoriesMap.entrySet()) {
                int qOffset = parseObjectOffset(entry.getKey());
                int qEndIndex = qOffset + parseObjectLength(entry.getKey()) - 1;

                // Overlapping check
                if (parseObjectParentSequenceId(entry.getKey()).equals(oParentSequenceId) && Math.max(qOffset, oOffset) <= Math.min(qEndIndex, oEndIndex)) {
                    objectCategories.addAll(entry.getValue());
                }
            }
        }

        // Object to categories association
        objectCategoriesMap.put(o, new ArrayList<>(objectCategories));

        // Category to objects association
        for (Category category : objectCategories) {
            List<LocalAbstractObject> objects = categoryObjectsMap.get(category);
            if (objects == null) {
                objects = new ArrayList<>();
                categoryObjectsMap.put(category, objects);
            }
            objects.add(o);
        }
        return objectCategories;
    }

    /**
     * Loads objects from a file, parses their categories and appends them to
     * the categorized object list.
     *
     * @param objectClass class of objects to be loaded
     * @param objectFile  file from which objects are loaded
     * @throws IOException
     */
    public void read(Class<? extends LocalAbstractObject> objectClass, String objectFile) throws IOException {
        read(objectClass, objectFile, null, null, null, null, false);
    }

    /**
     * Loads objects from a file, parses their categories and appends them to
     * the categorized object list.
     *
     * @param objectClass                class of objects to be loaded
     * @param objectFile                 file from which objects are loaded
     * @param objectLocatorRegEx         regular expression to which object locators are
     *                                   matched and only the matched objects are read
     * @param ignoredCategoryIds         array of IDs of categories whose objects are
     *                                   ignored (if it is set to null, no objects are ignored)
     * @param restrictedParentSeqIds     set of IDs of sequences whose objects are
     *                                   not ignored (if it is set to null, no objects are ignored)
     * @param objectMgmtToCategoryParser annotated objects which are used to
     *                                   assign their categories to loaded overlapping objects - the loaded object
     *                                   obtains the categories of all overlapping annotated objects (if it is set
     *                                   to null, the object category is parsed from the object locator)
     * @param parseObjectLength          indicates whether to parse the sequence length
     *                                   from the object locator, which is considered to have the following form:
     *                                   "sequenceId_categoryId_offset_length"
     * @throws IOException
     */
    public void read(Class<? extends LocalAbstractObject> objectClass, String objectFile, String objectLocatorRegEx, String[] ignoredCategoryIds, Set<String> restrictedParentSeqIds, ObjectMgmt objectMgmtToCategoryParser, boolean parseObjectLength) throws IOException {
        read(objectClass, objectFile, objectLocatorRegEx, ignoredCategoryIds, restrictedParentSeqIds, null, objectMgmtToCategoryParser, parseObjectLength);
    }

    /**
     * Loads objects from a file, parses their categories and appends them to
     * the categorized object list.
     *
     * @param objectClass                class of objects to be loaded
     * @param objectFile                 file from which objects are loaded
     * @param objectLocatorRegEx         regular expression to which object locators are
     *                                   matched and only the matched objects are read
     * @param ignoredCategoryIds         array of IDs of categories whose objects are
     *                                   ignored (if it is set to null, no objects are ignored)
     * @param restrictedParentSeqIds     set of IDs of sequences whose objects are
     *                                   not ignored (if it is set to null, no objects are ignored)
     * @param locatorIds                 if it is not set to null, only objects having the same
     *                                   locators are read
     * @param objectMgmtToCategoryParser annotated objects which are used to
     *                                   assign their categories to loaded overlapping objects - the loaded object
     *                                   obtains the categories of all overlapping annotated objects (if it is set
     *                                   to null, the object category is parsed from the object locator)
     * @param parseObjectLength          indicates whether to parse the sequence length
     *                                   from the object locator, which is considered to have the following form:
     *                                   "sequenceId_categoryId_offset_length"
     * @throws IOException
     */
    public void read(Class<? extends LocalAbstractObject> objectClass, String objectFile, String objectLocatorRegEx, String[] ignoredCategoryIds, Set<String> restrictedParentSeqIds, Set<String> locatorIds, ObjectMgmt objectMgmtToCategoryParser, boolean parseObjectLength) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.print("Reading objects (" + (new File(objectFile)).getName() + ")...");

        StreamGenericAbstractObjectIterator objIterator = new StreamGenericAbstractObjectIterator<>(objectClass, objectFile);
        int objectCount = 0;
        int totalObjectLength = 0;
        while (objIterator.hasNext()) {
            LocalAbstractObject o = objIterator.next();

            // Excludes objects having restricted locators and ids different as the set of restricted ones
            if ((objectLocatorRegEx == null || o.getLocatorURI().matches(objectLocatorRegEx))
                    && (restrictedParentSeqIds == null || restrictedParentSeqIds.contains(parseObjectParentSequenceId(o)))
                    && (locatorIds == null || locatorIds.contains(o.getLocatorURI()))) {

                // Excludes objects belonging to the ignored classes
                if (addObject(o, ignoredCategoryIds, objectMgmtToCategoryParser) != null) {
                    objectCount++;
                    totalObjectLength += (!parseObjectLength) ? 0 : parseObjectLength(o);
                }
            }
        }
        System.out.println(" " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("  object count: " + objectCount + ((!parseObjectLength) ? "" : "; average length: " + ((float) totalObjectLength / objectCount)));
        System.out.println("  category count: " + getCategories().size());
    }

    public void storeRandomObjects(String fileName, int categoryObjectsCount) throws IOException {
        OutputStream os = new FileOutputStream(new File(fileName));
        Random random = new Random();
        int storedObjectCount = 0;
        for (List<LocalAbstractObject> categoryObjects : categoryObjectsMap.values()) {
            if (categoryObjects.size() <= categoryObjectsCount) {
                for (LocalAbstractObject o : categoryObjects) {
                    o.write(os);
                }
                storedObjectCount += categoryObjects.size();
            } else {
                List<Integer> randomObjectIndexes = new ArrayList<>(categoryObjects.size());
                for (int i = 0; i < categoryObjects.size(); i++) {
                    randomObjectIndexes.add(i);
                }
                for (int i = 0; i < categoryObjectsCount; i++) {
                    int randomIdx = random.nextInt(randomObjectIndexes.size());
                    categoryObjects.get(randomObjectIndexes.get(randomIdx)).write(os);
                    randomObjectIndexes.remove(randomIdx);
                }
                storedObjectCount += categoryObjectsCount;
            }
        }
        os.close();
        System.out.println("Stored objects: " + storedObjectCount);
    }

    /**
     * Wrapper of {@link #executeKNNQueries(ObjectMgmt, Integer, Integer, InstantiableCollection, boolean, boolean, boolean)}. 
     *
     * @param queryMgmt queries which are evaluated
     * @param fixedK    fixed number of objects which are returned to each query
     *                  (if it is set to null, this number is adaptively determined for each query based on
     *                  the number of objects contained in the category to which the query object belongs)
     * @return map associating the category and its evaluated query operations
     * @author David Procházka
     */
    public Map<Category, List<RankingSingleQueryOperation>> executeKNNQueries(ObjectMgmt queryMgmt, Integer fixedK) {
        return executeKNNQueries(queryMgmt, fixedK, null, null, false, true, false);
    }

    /**
     * Wrapper of {@link #executeKNNQueries(ObjectMgmt, Integer, Integer, InstantiableCollection, boolean, boolean, boolean)}.
     *
     * @param queryMgmt                               queries which are evaluated
     * @param fixedK                                  fixed number of objects which are returned to each query
     *                                                (if it is set to null, this number is adaptively determined for each
     *                                                query based on the number of objects contained in the category to which
     *                                                the query object belongs)
     * @param includeExactMatchInResult               indicates whether the query object can
     *                                                be contained in the answer (it is controlled by comparing object
     *                                                locators)
     * @param includeMatchFromTheSameSequenceInResult indicates whether an
     *                                                object coming from the same sequence can be added to the query answer
     * @return map associating the category and its evaluated query operations
     * @author David Procházka
     */
    public Map<Category, List<RankingSingleQueryOperation>> executeKNNQueries(ObjectMgmt queryMgmt, Integer fixedK, boolean includeExactMatchInResult, boolean includeMatchFromTheSameSequenceInResult) {
        return executeKNNQueries(queryMgmt, fixedK, null, null, includeExactMatchInResult, includeMatchFromTheSameSequenceInResult, true);
    }

    /**
     * Creates and executes kNN queries based on the specified set of
     * categorized query objects. Each object is supposed to contain its locator
     * in the following form: "sequenceId_categoryId_offset_length".
     *
     * @param queryMgmt                               queries which are evaluated
     * @param fixedK                                  fixed number of objects which are returned to each query
     *                                                (if it is set to null, this number is adaptively determined for each
     *                                                query based on the number of objects contained in the category to which
     *                                                the query object belongs)
     * @param fixedKReranking                         fixed number of objects which are used to re-rank
     *                                                the existing query (if it is set to null, this number is adaptively
     *                                                determined for each query based on the number of objects contained in the
     *                                                category to which the query object belongs)
     * @param rankedSortedCollection                  collection which is used for re-ranking the
     *                                                objects (if it is set to null, no re-ranking is used)
     * @param includeExactMatchInResult               indicates whether the query object can
     *                                                be contained in the answer (it is controlled by comparing object
     *                                                locators)
     * @param includeMatchFromTheSameSequenceInResult indicates whether an
     *                                                object coming from the same sequence can be added to the query answer
     * @param printBasicStats                         indicates whether basic process information should be printed
     * @return map associating the category and its evaluated query operations
     */
    public Map<Category, List<RankingSingleQueryOperation>> executeKNNQueries(ObjectMgmt queryMgmt, Integer fixedK, Integer fixedKReranking, InstantiableCollection rankedSortedCollection, boolean includeExactMatchInResult, boolean includeMatchFromTheSameSequenceInResult, boolean printBasicStats) {
        long startTime = System.currentTimeMillis();
        if (printBasicStats) {
            System.out.println("Querying...");
        }

        // Keeps associations between the category and its executed query operations
        Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap = new HashMap<>();

        // Executing queries over individual categories
        int fixedKSum = 0;
        int totalQueryIdx = 0;
        for (Map.Entry<Category, List<LocalAbstractObject>> categoryQueryObjectsEntry : queryMgmt.categoryObjectsMap.entrySet()) {
            Category queryCategory = categoryQueryObjectsEntry.getKey();
            List<RankingSingleQueryOperation> categoryOperations = new ArrayList<>(getObjectCountInCategory(queryCategory));

            // Executing each query within the category
            final int k = (fixedK != null) ? fixedK : Math.max(1, getObjectCountInCategory(queryCategory) - ((includeExactMatchInResult) ? 0 : 1));
            fixedKSum += k;
            final int rerankingK = (fixedKReranking != null) ? fixedKReranking : Math.max(1, getObjectCountInCategory(queryCategory) - ((includeExactMatchInResult) ? 0 : 1));
            for (LocalAbstractObject q : categoryQueryObjectsEntry.getValue()) {

                // Operation construction
                KNNQueryOperation op = new KNNQueryOperation(q, k, false, AnswerType.ORIGINAL_OBJECTS, new RankedSortedCollectionDistHashcode());

                // Querying
                for (Map.Entry<LocalAbstractObject, List<Category>> objectCategoriesEntry : objectCategoriesMap.entrySet()) {
                    LocalAbstractObject o = objectCategoriesEntry.getKey();
                    if ((includeMatchFromTheSameSequenceInResult || !parseObjectParentSequenceId(q).equals(parseObjectParentSequenceId(o)))
                            && (includeExactMatchInResult || !q.getLocatorURI().equals(o.getLocatorURI()))) {
                        float dist = q.getDistance(o);
                        op.addToAnswer(o, dist, null);
                    }
                }

                // Re-ranking
                if (rankedSortedCollection != null) {
                    KNNQueryOperation rop = new KNNQueryOperation(q, rerankingK, AnswerType.ORIGINAL_OBJECTS);
                    rop.setAnswerCollection(rankedSortedCollection.instantiate(q));
                    for (Iterator<AbstractObject> answerIt = op.getAnswerObjects(); answerIt.hasNext(); ) {
                        rop.addToAnswer((LocalAbstractObject) answerIt.next());
                    }
                    op = rop;
                }
                categoryOperations.add(op);

                // Progress
                if (queryMgmt.getObjectCount() >= 10 && (totalQueryIdx + 1) % (queryMgmt.getObjectCount() / 10) == 0) {
                    if (printBasicStats) {
                        System.out.println("  " + Math.round((float) (totalQueryIdx + 1) / queryMgmt.getObjectCount() * 100) + "%" + " (" + (System.currentTimeMillis() - startTime) + " ms)");
                    }
                }
                totalQueryIdx++;
            }
            categoryOperationsMap.put(queryCategory, categoryOperations);
        }

        if (printBasicStats) {
            System.out.println("kNN search (k=" + ((fixedK == null) ? "adaptive=" + ((float) fixedKSum / queryMgmt.categoryObjectsMap.size()) : fixedK) + ")");
        }

        return categoryOperationsMap;
    }

    /**
     * Evaluates the retrieval accuracy of answered and categorized similarity
     * queries. Each object is supposed to contain its locator in the following
     * form: "sequenceId_categoryId_offset_length".
     *
     * @param categoryOperationsMap        map associating the category and its
     *                                     executed (already answered) query operations
     * @param evaluateQueriesIndependently indicates whether each query is
     *                                     evaluated independently, or only one multi-object query is constructed
     *                                     for each category
     * @param printCategoryStats           if true, the accuracy of each category is
     *                                     displayed
     * @param printQueryStats              if true, objects and their distances in the answer
     *                                     are displayed for each query
     * @return map associating the category and its evaluated query operations
     * @throws java.lang.CloneNotSupportedException
     */
    public Map<Category, List<RankingSingleQueryOperation>> evaluateRetrieval(Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap, boolean evaluateQueriesIndependently, boolean printCategoryStats, boolean printQueryStats) throws CloneNotSupportedException {
        int queryCount = 0;
        int totalTruePositives = 0;
        int totalAnswerCount = 0;
        int totalTruePositiveIndex = 0;
        float categoryPrecisionSum = 0f;

        // Evaluating queries over individual categories
        for (Map.Entry<Category, List<RankingSingleQueryOperation>> categoryOperationsEntry : categoryOperationsMap.entrySet()) {
            Category queryCategory = categoryOperationsEntry.getKey();

            // Takes operations for the specific category
            List<RankingSingleQueryOperation> categoryOperations;
            if (evaluateQueriesIndependently) {
                categoryOperations = categoryOperationsEntry.getValue();
            } else {
                categoryOperations = new ArrayList<>();
                Iterator<RankingSingleQueryOperation> opIt = categoryOperationsEntry.getValue().iterator();
                if (opIt.hasNext()) {

                    // Creates a new answer collection and adds the originally retrieved objects
                    RankingSingleQueryOperation categoryOp = opIt.next().clone(false);

                    // Adds the retrieved objects of second and other operations
                    while (opIt.hasNext()) {
                        RankingSingleQueryOperation rqo = opIt.next();
                        for (Iterator<RankedAbstractObject> it = rqo.getAnswer(); it.hasNext(); ) {
                            RankedAbstractObject ro = it.next();
                            categoryOp.addToAnswer(ro.getObject(), ro.getDistance(), null);
                        }
                    }
                    categoryOperations.add(categoryOp);
                }
            }
            queryCount += categoryOperations.size();
            int categoryTruePositives = 0;
            int categoryAnswerCount = 0;

            for (RankingQueryOperation rqo : categoryOperations) {
                if (printQueryStats && rqo instanceof KNNQueryOperation) {
                    System.out.println("  query object: " + ((KNNQueryOperation) rqo).getQueryObject().getLocatorURI());
                }
                Iterator<RankedAbstractObject> it = rqo.getAnswer();
                int objectIndex = 0;
                while (it.hasNext()) {
                    RankedAbstractObject rao = it.next();
                    LocalAbstractObject o = (LocalAbstractObject) rao.getObject();
                    if (getObjectCategories(o).contains(queryCategory)) {
                        categoryTruePositives++;
                        totalTruePositiveIndex += objectIndex;
                    }
                    if (printQueryStats) {
                        System.out.println("    " + objectIndex + ": " + o.getLocatorURI() + ": " + rao.getDistance());
                    }
                    objectIndex++;
                }
                if (printQueryStats) {
                    System.out.println();
                }
                categoryAnswerCount += rqo.getAnswerCount();
            }
            totalTruePositives += categoryTruePositives;
            totalAnswerCount += categoryAnswerCount;
            float categoryPrecision = (float) categoryTruePositives / categoryAnswerCount;
            categoryPrecisionSum += categoryPrecision;
            if (printCategoryStats) {
                System.out.println("  " + queryCategory.description + " (ID=" + queryCategory.id + ", count=" + getObjectCountInCategory(queryCategory) + "): " + (categoryPrecision * 100f) + "%");
            }
        }
        System.out.println("  precision over objects and categories: " + ((float) totalTruePositives / totalAnswerCount * 100f) + "\t" + (categoryPrecisionSum / categoryOperationsMap.size() * 100f));
        System.out.println("  avg position of all true positives: " + ((float) totalTruePositiveIndex / totalTruePositives) + " (true positives=" + totalTruePositives + ", queries=" + queryCount + ")");
        return categoryOperationsMap;
    }

    /**
     * Classifies the objects returned from the retrieval process.
     *
     * @param objectClassifier      object classifier
     * @param categoryOperationsMap map associating the category and its
     *                              evaluated query operations
     * @param benevolenceK          number of the most ranked classes between which are
     *                              considered to evaluate the query precision (usually set to 1)
     * @param printCategoryStats    if true, the accuracy of each category is
     *                              displayed
     * @param printQueryStats       if true, classes and their relevance are displayed
     *                              for each query
     * @return confusion matrix for classification of categories
     * @throws ClassificationException
     */
    public float[][] evaluateClassification(ObjectMultiCategoryClassifier objectClassifier, Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap, int benevolenceK, boolean printCategoryStats, boolean printQueryStats) throws ClassificationException {
        System.out.println("Classifying...");

        // Evaluating executed queries
        int totalTruePositives = 0;
        int totalAnswerCount = 0;
        int totalFirstTruePositiveIndex = 0;
        int totalClassificationSize = 0;
        int totalClassificationSizeBenevolence = 0;
        float categoryPrecisionSum = 0f;

        // Confusion matrix
        float[][] confMatrix = new float[categoryMgmt.getCategoryCount()][categoryMgmt.getCategoryCount()];
        for (float[] floatArray : confMatrix) {
            Arrays.fill(floatArray, 0);
        }

        // Evaluating queries over individual categories
        for (Map.Entry<Category, List<RankingSingleQueryOperation>> categoryOperationsEntry : categoryOperationsMap.entrySet()) {
            Category queryCategory = categoryOperationsEntry.getKey();

            int categoryTruePositives = 0;

            // Takes operations for the specific category
            for (RankingQueryOperation op : categoryOperationsEntry.getValue()) {
                int classifiedCategoryIndex = 0;
                if (printQueryStats) {
                    System.out.print("    ");
                    if (op instanceof KNNQueryOperation) {
                        System.out.print("query object: " + ((KNNQueryOperation) op).getQueryObject().getLocatorURI() + ", ");
                    }
                    System.out.println("query category: " + queryCategory.description + " (ID=" + queryCategory.id + ")");
                }

                // Adds the query object as a parameter to a classifier
                Map<String, LocalAbstractObject> queryParamsMap = new HashMap<>();
                Parametric queryParams = new ParametricBase(queryParamsMap);
                if (op instanceof KNNQueryOperation) {
                    queryParamsMap.put("queryObject", ((KNNQueryOperation) op).getQueryObject());
                }

                String benevolentClassifiedCategoryId = null;
                ObjectClassification objectClassification = objectClassifier.classify(op.getAnswer(), queryParams);
                totalClassificationSize += objectClassification.size();
                totalClassificationSizeBenevolence += Math.min(benevolenceK, objectClassification.size());
                Iterator<Map.Entry<String, Float>> classificationEntryIt = objectClassification.getSortedClassificationEntries().iterator();
                while (classificationEntryIt.hasNext() && classifiedCategoryIndex < benevolenceK) {
                    Map.Entry<String, Float> classificationEntry = classificationEntryIt.next();

                    // Confusion matrix
                    if (classifiedCategoryIndex == 0) {
                        benevolentClassifiedCategoryId = classificationEntry.getKey();
                    }

                    if (!classificationEntry.getKey().equals(queryCategory.id)) {
                        if (printQueryStats) {
                            System.out.println("     classification " + classifiedCategoryIndex + ": " + classificationEntry.getKey() + " -> " + classificationEntry.getValue());
                            printClassificationAnswer(op.getAnswer());
                        }
                    }

                    if (classificationEntry.getKey().equals(queryCategory.id)) {
                        benevolentClassifiedCategoryId = classificationEntry.getKey();
                        categoryTruePositives++;
                        totalFirstTruePositiveIndex += classifiedCategoryIndex;
                        break;
                    }
                    classifiedCategoryIndex++;
                }
                confMatrix[queryCategory.index][categoryMgmt.getCategoryById(benevolentClassifiedCategoryId).index] += 1f;
            }

            totalTruePositives += categoryTruePositives;
            totalAnswerCount += categoryOperationsEntry.getValue().size();
            float categoryPrecision = (float) categoryTruePositives / categoryOperationsEntry.getValue().size();
            categoryPrecisionSum += categoryPrecision;
            if (printCategoryStats) {
                System.out.println("  Category: " + queryCategory.description + " (ID=" + queryCategory.id + ", test count=" + categoryOperationsEntry.getValue().size() + ", training count=" + getObjectCountInCategory(queryCategory) + "): " + (categoryPrecision * 100f) + "%");
            }
        }
        System.out.println("  classification precision over objects and categories: " + ((float) totalTruePositives / totalAnswerCount * 100f) + "\t" + (categoryPrecisionSum / categoryOperationsMap.size() * 100f));
        System.out.println("  avg position of true positives: " + ((float) totalFirstTruePositiveIndex / totalTruePositives) + " (true positives=" + totalTruePositives + ", queries=" + totalAnswerCount + ")");
        System.out.println("  avg classification size: " + ((float) totalClassificationSize / totalAnswerCount) + " (within benevolence size=" + ((float) totalClassificationSizeBenevolence / totalAnswerCount) + "), avg class precision: ");

        return confMatrix;
    }

    /**
     * Saves the confusion matrix into a text file.
     *
     * @param confMatrix      confusion matrix
     * @param fileName        name of the file into which the confusion matrix is saved
     * @param normalizeValues indicates whether the values in the confusion
     *                        matrix should be normalized
     * @throws java.io.IOException
     */
    public void saveConfusionMatrixToFile(float[][] confMatrix, String fileName, boolean normalizeValues) throws IOException {
        if (fileName != null) {
            DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
            FileWriter out = new FileWriter(fileName, StandardCharsets.UTF_8);
            for (int i = 0; i < confMatrix.length; i++) {
                Category queryCategory = categoryMgmt.getCategoryByIndex(i);
                int queryCategoryTrainingObjectCount = getObjectCountInCategory(queryCategory);
                int queryCategoryTestObjectCount = -1;
                if (normalizeValues) {
                    queryCategoryTestObjectCount = 0;
                    for (int j = 0; j < confMatrix.length; j++) {
                        queryCategoryTestObjectCount += confMatrix[i][j];
                    }
                }
                for (int j = 0; j < confMatrix.length; j++) {
                    Category classifiedCategory = categoryMgmt.getCategoryByIndex(j);
                    out.write(i + ";" + queryCategory.description + " (TEST=" + queryCategoryTestObjectCount + ", TRAINING=" + queryCategoryTrainingObjectCount + ");" + j + ";" + classifiedCategory.description + ";" + df.format(normalizeValues ? confMatrix[i][j] / queryCategoryTestObjectCount : confMatrix[i][j]) + "\n");
                }
            }
            out.close();
        }
    }

    /**
     * Analyzes similarity distances by pairing objects with the specified query
     * ones. The objects are paired based on their locators.
     *
     * @param queryMgmt          queries which are paired
     * @param printCategoryStats if true, the analysis is displayed for each
     *                           category
     */
    public void evaluatePairedSimilarityDists(ObjectMgmt queryMgmt, boolean printCategoryStats) {
        long startTime = System.currentTimeMillis();
        System.out.println("Computing distances...");

        List<Float> totalPairedDists = new ArrayList<>();
        Float[] categoryAvgPairedDists = new Float[getCategories().size()];
        Arrays.fill(categoryAvgPairedDists, Float.NaN);

        // Paires data-to-query objects based on their locators
        for (Map.Entry<Category, List<LocalAbstractObject>> categoryObjectsEntry : categoryObjectsMap.entrySet()) {
            Category category = categoryObjectsEntry.getKey();
            List<Float> categoryPairedDists = new ArrayList<>();
            for (LocalAbstractObject o : categoryObjectsEntry.getValue()) {
                LocalAbstractObject pairedQueryObject = null;
                for (LocalAbstractObject q : queryMgmt.getObjects()) {
                    if (o.getLocatorURI().equals(q.getLocatorURI())) {
                        pairedQueryObject = q;
                        break;
                    }
                }
                if (pairedQueryObject != null) {
                    float dist = o.getDistance(pairedQueryObject);
                    categoryPairedDists.add(dist);
                }
            }
            totalPairedDists.addAll(categoryPairedDists);

            // Average paired distance within a single category
            categoryAvgPairedDists[category.index] = computeAvgValue(categoryPairedDists);

            if (printCategoryStats) {
                System.out.println("  " + category.description + " (ID=" + category.id + ", paired object count=" + categoryPairedDists.size() + "):"
                        + " " + (categoryAvgPairedDists[category.index]) + "\t" + (computeStddevValue(categoryPairedDists)));
            }
        }
        System.out.println((System.currentTimeMillis() - startTime) + "ms");
        System.out.println("  paired distances (avg over categories, stddev over categories, avg over all pairs, stddev over all pairs):");
        System.out.println("  " + computeAvgValue(Arrays.asList(categoryAvgPairedDists)) + "\t" + computeStddevValue(Arrays.asList(categoryAvgPairedDists)) + "\t" + computeAvgValue(totalPairedDists) + "\t" + computeStddevValue(totalPairedDists));
    }

    /**
     * Creates a copy of {@code this} but containing only objects belonging to
     * one of the specified classification categories.
     *
     * @param classificationCategories the classification categories
     * @return a copy containing only objects belonging to one of the specified classification categories.
     * @author David Procházka
     */
    public ObjectMgmt copy(Set<Category> classificationCategories) {
        var objectMgmt = new ObjectMgmt(categoryMgmt);
        var categoryObjects = categoryObjectsMap;

        for (var category : classificationCategories) {
            if (categoryObjects.containsKey(category)) {
                for (var object : categoryObjects.get(category)) {
                    objectMgmt.addObject(object, null, null);
                }
            }
        }

        return objectMgmt;
    }

    /**
     * Wrapper of {@link #evaluateClassificationWithClassificationResult(ObjectMultiCategoryClassifier, Map, int, boolean, boolean, boolean)}.
     *
     * @param objectClassifier      object classifier
     * @param categoryOperationsMap map associating the category and its evaluated query operations
     * @return classification result per category per object
     * @throws ClassificationException when the classification fails
     * @author David Procházka
     */
    public ClassificationResult evaluateClassificationWithClassificationResult(ObjectMultiCategoryClassifier objectClassifier, Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap) throws ClassificationException {
        return evaluateClassificationWithClassificationResult(objectClassifier, categoryOperationsMap, 1, false, false, false);
    }

    /**
     * Evaluates the global classification in the two-stage classification framework.
     * Based on {@link #evaluateClassification(ObjectMultiCategoryClassifier, Map, int, boolean, boolean)}.
     *
     * @param objectClassifier      object classifier
     * @param categoryOperationsMap map associating the category and its evaluated query operations
     * @param benevolenceK          number of the most ranked classes between which are considered to evaluate
     *                              the query precision (usually set to 1)
     * @param printCategoryStats    if true, the accuracy of each category is displayed
     * @param printQueryStats       if true, classes and their relevance are displayed for each query
     * @return classification result per category per object
     * @throws ClassificationException when the classification fails
     * @author David Procházka
     */
    public ClassificationResult evaluateClassificationWithClassificationResult(ObjectMultiCategoryClassifier objectClassifier, Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap, int benevolenceK, boolean printBasicStats, boolean printCategoryStats, boolean printQueryStats) throws ClassificationException {
        if (printBasicStats) {
            System.out.println("Classifying...");
        }

        var classificationResultPerCategory = new HashMap<Category, List<ObjectClassificationResult>>(categoryOperationsMap.size());

        // Evaluating executed queries
        int totalTruePositives = 0;
        int totalAnswerCount = 0;
        int totalFirstTruePositiveIndex = 0;
        int totalClassificationSize = 0;
        int totalClassificationSizeBenevolence = 0;
        float categoryPrecisionSum = 0f;

        // Evaluating queries over individual categories
        for (var categoryOperationsEntry : categoryOperationsMap.entrySet()) {
            Category queryCategory = categoryOperationsEntry.getKey();

            int categoryTruePositives = 0;

            // Takes operations for the specific category
            for (RankingSingleQueryOperation op : categoryOperationsEntry.getValue()) {
                int classifiedCategoryIndex = 0;
                var queryObject = op.getQueryObject();

                if (printQueryStats) {
                    System.out.print("    ");
                    System.out.print("query object: " + queryObject.getLocatorURI() + ", ");
                    System.out.println("query category: " + queryCategory.description + " (ID=" + queryCategory.id + ")");
                }

                // Adds the query object as a parameter to a classifier
                Map<String, LocalAbstractObject> queryParamsMap = new HashMap<>();
                Parametric queryParams = new ParametricBase(queryParamsMap);
                queryParamsMap.put("queryObject", queryObject);

                ObjectClassification objectClassification = objectClassifier.classify(op.getAnswer(), queryParams);
                totalClassificationSize += objectClassification.size();
                totalClassificationSizeBenevolence += Math.min(benevolenceK, objectClassification.size());
                Iterator<Map.Entry<String, Float>> classificationEntryIt = objectClassification.getSortedClassificationEntries().iterator();
                while (classificationEntryIt.hasNext() && classifiedCategoryIndex < benevolenceK) {
                    Map.Entry<String, Float> classificationEntry = classificationEntryIt.next();

                    var classifiedCategoryId = classificationEntry.getKey();
                    var confidence = classificationEntry.getValue();

                    classificationResultPerCategory
                            .computeIfAbsent(queryCategory, x -> new ArrayList<>())
                            .add(new ObjectClassificationResult(
                                    queryObject,
                                    categoryMgmt.getCategoryById(classifiedCategoryId),
                                    confidence,
                                    classifiedCategoryId.equals(queryCategory.id)
                            ));

                    if (!classifiedCategoryId.equals(queryCategory.id)) {
                        if (printQueryStats) {
                            System.out.println("     classification " + classifiedCategoryIndex + ": " + classifiedCategoryId + " -> " + confidence);
                            printClassificationAnswer(op.getAnswer());
                        }
                    }

                    if (classifiedCategoryId.equals(queryCategory.id)) {
                        categoryTruePositives++;
                        totalFirstTruePositiveIndex += classifiedCategoryIndex;
                        break;
                    }
                    classifiedCategoryIndex++;
                }
            }

            totalTruePositives += categoryTruePositives;
            totalAnswerCount += categoryOperationsEntry.getValue().size();
            float categoryPrecision = (float) categoryTruePositives / categoryOperationsEntry.getValue().size();
            categoryPrecisionSum += categoryPrecision;

            if (printCategoryStats) {
                System.out.println("  Category: " + queryCategory.description + " (ID=" + queryCategory.id + ", test count=" + categoryOperationsEntry.getValue().size() + ", training count=" + getObjectCountInCategory(queryCategory) + "): " + (categoryPrecision * 100f) + "%");
            }
        }

        float precisionOverObjects = (float) totalTruePositives / totalAnswerCount * 100f;
        if (printBasicStats) {
            System.out.println("  classification precision over objects and categories: " + precisionOverObjects + "\t" + (categoryPrecisionSum / categoryOperationsMap.size() * 100f));
            System.out.println("  avg position of true positives: " + ((float) totalFirstTruePositiveIndex / totalTruePositives) + " (true positives=" + totalTruePositives + ", queries=" + totalAnswerCount + ")");
            System.out.println("  avg classification size: " + ((float) totalClassificationSize / totalAnswerCount) + " (within benevolence size=" + ((float) totalClassificationSizeBenevolence / totalAnswerCount) + "), avg class precision: ");
        }

        return new ClassificationResult(classificationResultPerCategory, precisionOverObjects);
    }

    /**
     * Evaluates the two-stage classification framework.
     * Based on {@link #evaluateClassification(ObjectMultiCategoryClassifier, Map, int, boolean, boolean)}.
     *
     * @param objectClassifier       object classifier
     * @param categoryOperationsMap  map associating the category and its
     *                               evaluated query operations
     * @param benevolenceK           number of the most ranked classes between which are
     *                               considered to evaluate the query precision (usually set to 1)
     * @param printCategoryStats     if true, the accuracy of each category is
     *                               displayed
     * @param printQueryStats        if true, classes and their relevance are displayed
     *                               for each query
     * @param specializedClassifiers specialized classifiers
     * @param confidenceThreshold    confidence threshold
     * @return classification result per category per object
     * @throws ClassificationException when classification fails
     * @author David Procházka
     */
    public ClassificationResult evaluateTwoStageClassificationWithClassificationResult(ObjectMultiCategoryClassifier objectClassifier, Map<Category, List<RankingSingleQueryOperation>> categoryOperationsMap, int benevolenceK, boolean printBasicStats, boolean printCategoryStats, boolean printQueryStats, Map<String, SpecializedClassifier> specializedClassifiers, float confidenceThreshold) throws ClassificationException {
        if (printBasicStats) {
            System.out.println("Classifying...");
        }

        var classificationResultPerCategory = new HashMap<Category, List<ObjectClassificationResult>>(categoryOperationsMap.size());

        // Evaluating executed queries
        int totalTruePositives = 0;
        int totalAnswerCount = 0;
        int totalFirstTruePositiveIndex = 0;
        int totalClassificationSize = 0;
        int totalClassificationSizeBenevolence = 0;
        float categoryPrecisionSum = 0f;

        int totalCorrectSpecializedClassifications = 0;
        int totalIncorrectSpecializedClassifications = 0;

        // Confusion matrix
        float[][] confMatrix = new float[categoryMgmt.getCategoryCount()][categoryMgmt.getCategoryCount()];
        for (float[] floatArray : confMatrix) {
            Arrays.fill(floatArray, 0);
        }

        // Evaluating queries over individual categories
        for (var categoryOperationsEntry : categoryOperationsMap.entrySet()) {
            Category queryCategory = categoryOperationsEntry.getKey();

            int categoryTruePositives = 0;

            // Takes operations for the specific category
            for (var op : categoryOperationsEntry.getValue()) {
                int classifiedCategoryIndex = 0;
                var queryObject = op.getQueryObject();

                if (printQueryStats) {
                    System.out.print("    ");
                    System.out.print("query object: " + queryObject.getLocatorURI() + ", ");
                    System.out.println("query category: " + queryCategory.description + " (ID=" + queryCategory.id + ")");
                }


                // Adds the query object as a parameter to a classifier
                Map<String, LocalAbstractObject> queryParamsMap = new HashMap<>();
                Parametric queryParams = new ParametricBase(queryParamsMap);
                queryParamsMap.put("queryObject", queryObject);

                String benevolentClassifiedCategoryId = null;
                var objectClassification = objectClassifier.classify(op.getAnswer(), queryParams);
                totalClassificationSize += objectClassification.size();
                totalClassificationSizeBenevolence += Math.min(benevolenceK, objectClassification.size());
                var classificationEntryIt = objectClassification.getSortedClassificationEntries().iterator();
                while (classificationEntryIt.hasNext() && classifiedCategoryIndex < benevolenceK) {
                    var classificationEntry = classificationEntryIt.next();

                    var classifiedCategoryId = classificationEntry.getKey();
                    var confidence = classificationEntry.getValue();

                    var invokedSpecializedClassifier = false;
                    final var globalClassifiedCategoryId = classifiedCategoryId;
                    final var globalConfidence = confidence;
                    Iterator<RankedAbstractObject> specializedAnswer = Collections.emptyIterator();

                    // Check if there exists a specialized classifier for this category.
                    if (globalConfidence < confidenceThreshold && specializedClassifiers.containsKey(classifiedCategoryId)) {
                        var classifier = specializedClassifiers.get(classifiedCategoryId);
                        var answer = classifier.classify(queryObject);

                        specializedAnswer = answer.kNNAnswer();
                        classifiedCategoryId = answer.categoryID();
                        confidence = answer.confidence();

                        invokedSpecializedClassifier = true;
                    }

                    classificationResultPerCategory
                            .computeIfAbsent(queryCategory, x -> new ArrayList<>())
                            .add(new ObjectClassificationResult(
                                    queryObject,
                                    categoryMgmt.getCategoryById(classifiedCategoryId),
                                    confidence,
                                    classifiedCategoryId.equals(queryCategory.id)
                            ));

                    // Confusion matrix
                    if (classifiedCategoryIndex == 0) {
                        benevolentClassifiedCategoryId = classifiedCategoryId;
                    }

                    if (!classifiedCategoryId.equals(queryCategory.id)) {
                        if (invokedSpecializedClassifier) {
                            totalIncorrectSpecializedClassifications++;
                        }

                        if (printQueryStats) {
                            System.out.println("     global classification " + classifiedCategoryIndex + ": " + globalClassifiedCategoryId + " -> " + globalConfidence);
                            printClassificationAnswer(op.getAnswer());

                            if (invokedSpecializedClassifier) {
                                System.out.println("     specialized classification " + classifiedCategoryIndex + ": " + classifiedCategoryId + " -> " + confidence);
                                printClassificationAnswer(specializedAnswer);
                            }
                        }
                    }

                    if (classifiedCategoryId.equals(queryCategory.id)) {
                        if (invokedSpecializedClassifier) {
                            totalCorrectSpecializedClassifications++;
                        }

                        benevolentClassifiedCategoryId = classifiedCategoryId;
                        categoryTruePositives++;
                        totalFirstTruePositiveIndex += classifiedCategoryIndex;
                        break;
                    }
                    classifiedCategoryIndex++;
                }
                confMatrix[queryCategory.index][categoryMgmt.getCategoryById(benevolentClassifiedCategoryId).index] += 1f;
            }

            totalTruePositives += categoryTruePositives;
            totalAnswerCount += categoryOperationsEntry.getValue().size();
            float categoryPrecision = (float) categoryTruePositives / categoryOperationsEntry.getValue().size();
            categoryPrecisionSum += categoryPrecision;

            if (printCategoryStats) {
                System.out.println("  Category: " + queryCategory.description + " (ID=" + queryCategory.id + ", test count=" + categoryOperationsEntry.getValue().size() + ", training count=" + getObjectCountInCategory(queryCategory) + "): " + (categoryPrecision * 100f) + "%");
            }
        }

        float precisionOverObjects = (float) totalTruePositives / totalAnswerCount * 100f;
        if (printBasicStats) {
            System.out.println("  classification precision over objects and categories: " + precisionOverObjects + "\t" + (categoryPrecisionSum / categoryOperationsMap.size() * 100f));
            System.out.println("  avg position of true positives: " + ((float) totalFirstTruePositiveIndex / totalTruePositives) + " (true positives=" + totalTruePositives + ", queries=" + totalAnswerCount + ")");
            System.out.println("  avg classification size: " + ((float) totalClassificationSize / totalAnswerCount) + " (within benevolence size=" + ((float) totalClassificationSizeBenevolence / totalAnswerCount) + "), avg class precision: ");

            System.out.println("  correct specialized classifications: " + totalCorrectSpecializedClassifications);
            System.out.println("  incorrect specialized classifications: " + totalIncorrectSpecializedClassifications);
        }

        return new ClassificationResult(classificationResultPerCategory, precisionOverObjects);
    }
}
