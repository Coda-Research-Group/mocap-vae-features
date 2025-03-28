package mcdr.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.test.utils.ObjectCategoryMgmt;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;
import messif.operations.RankingQueryOperation;
import messif.operations.RankingSingleQueryOperation;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectRetrievalTester {

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Class<? extends LocalAbstractObject> objectClass = ObjectFloatVectorNeuralNetworkL2.class;

        final String queryFile = "y:/datasets/mocap/hdm05/features/AlexNet-4096D/MTAP18/class122-2folds/objects-annotations_specific-caffe100p_specific122-2fold_1_test.data";
        final String dataFile = "y:/datasets/mocap/hdm05/features/AlexNet-4096D/MTAP18/class122-2folds/objects-annotations_specific-caffe100p_specific122-2fold_1_train.data";

        // retrieval params
        final boolean includeExactMatchInResult = false;
        // values of k to be evaluated
//        int[] fixedKsToEvaluate = new int[]{};
        int[] fixedKsToEvaluate = new int[]{1, 3, 4, 5, 10};
//        int[] fixedKsToEvaluate = IntStream.range(1, 11).toArray();
        // ids of categories to be ignored
        final String[] ignoredCategoryIds = null;
//        final String[] ignoredCategoryIds = new String[]{"56", "57", "58", "59", "60", "61", "138", "139"}; // HDM05-122
        final boolean includeMatchFromTheSameSequenceInResult = true;
        final boolean evaluateQueriesIndependently = true; // indicates whether each query is evaluated independently, or only one multi-object query is constructed for each category
        final boolean restrictDataObjectsByQueries = false;
        final boolean parseDataCategoriesFromOverlappingQueries = false;

        // structures
        ObjectCategoryMgmt categoryMgmt = new ObjectCategoryMgmt("y:/datasets/mocap/hdm05/meta/category_description_short.txt");
        ObjectMgmt queryMgmt = new ObjectMgmt(categoryMgmt);
        ObjectMgmt dataMgmt = new ObjectMgmt(categoryMgmt);

        // queries
        System.out.println("Queries:");
        queryMgmt.read(objectClass, queryFile, null, ignoredCategoryIds, null, null, true);

        // data
        System.out.println("Data:");
        dataMgmt.read(objectClass, dataFile, null, ignoredCategoryIds, (!restrictDataObjectsByQueries) ? null : queryMgmt.getParentSequenceIds(), (parseDataCategoriesFromOverlappingQueries) ? queryMgmt : null, true);

        // Querying
        Integer maxK = (fixedKsToEvaluate.length == 0) ? null : Arrays.stream(fixedKsToEvaluate).summaryStatistics().getMax();
        System.out.println("maxK = " + maxK);
        long startTime = System.currentTimeMillis();
        Map<ObjectCategoryMgmt.Category, List<RankingSingleQueryOperation>> origCategoryOperationsMap = dataMgmt.executeKNNQueries(queryMgmt, maxK, includeExactMatchInResult, includeMatchFromTheSameSequenceInResult);
        System.out.println("Querying time: " + ((System.currentTimeMillis() - startTime) / 1000f) + " s");

        // Classifier
        ObjectMultiCategoryClassifier objectClassifier = new ObjectMultiCategoryClassifier(true);

        // Evaluation
        if (maxK == null) {
            dataMgmt.evaluateRetrieval(origCategoryOperationsMap, evaluateQueriesIndependently, false, false);
            dataMgmt.evaluateClassification(objectClassifier, origCategoryOperationsMap, 1, false, false);
        } else {
            for (int k : fixedKsToEvaluate) {
                System.out.println("Search evaluation (k=" + k + "):");

                Map<ObjectCategoryMgmt.Category, List<RankingSingleQueryOperation>> categoryOperationsMap = ObjectMgmt.cloneCategorizedRankingOperations(origCategoryOperationsMap, k);
                dataMgmt.evaluateRetrieval(categoryOperationsMap, evaluateQueriesIndependently, false, false);

                // evaluation of classification
                float[][] confMatrix = dataMgmt.evaluateClassification(objectClassifier, categoryOperationsMap, 1, false, false);
//                dataMgmt.saveConfusionMatrixToFile(confMatrix, confMatrixFile, true);
            }
        }

    }
}
