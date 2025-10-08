package mcdr.evaluation;

import cz.muni.fi.disa.similarityoperators.cover.HullRepresentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import mcdr.test.utils.ObjectCategoryMgmt;
import mcdr.test.utils.ObjectMgmt;
import messif.motionvocabulary.impl.HullCenterVocabulary;
import messif.motionvocabulary.impl.HullVocabulary;
import messif.motionvocabulary.impl.SequenceMocapSegment;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.RankingQueryOperation;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.query.KNNQueryOperation;
import messif.utility.FileUtils;
import messif.utility.HullRepresentationAsLocalAbstractObject;
import messif.utility.MotionIdentification;

/**
 *
 * @author Derived from the code by Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class AppEvaluatorOnWholeClasses {

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final boolean includeExactMatchInResult = false;
        final boolean includeMatchFromTheSameSequenceInResult = true;
        final boolean evaluateQueriesIndependently = true; // indicates whether each query is evaluated independently, or only one multi-object query is constructed for each category
        //int[] fixedKsToEvaluate = new int[]{1, 2, 3, 4, 5, 10, 15, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        int[] fixedKsToEvaluate = new int[]{};
//        String queryFile = "Y:/datasets/mocap/hdm05/segmentation/class130-actions-segment80_shift16-coords_normPOS-fps12.data";
//        String hullDir = "K:/research/motion-words/hulls/hdm05-sgm80sh16-actions-hulls";
//        String queryFile = "Y:/datasets/mocap/hdm05/segmentation/class130-actions-segment20_shift20-coords_normPOS-fps12.data";
//        String hullDir = "K:/research/motion-words/hulls/hdm05-sgm20sh20-actions-hulls";
//        Class<SequenceMocapPoseCoordsL2DTW> segmentClass = SequenceMocapPoseCoordsL2DTW.class;
        
//        String hullDir = "K:/research/motion-words/hulls/hdm05-actions-hulls-closest";
        //String queryFile = "Y:/datasets/mocap/hdm05/class130-actions-coords_normPOS-fps12.data";
        //String hullDir = "K:/research/motion-words/hulls/hdm05-actions-hulls-on-poses";
        
        Class<SequenceMocapPoseCoordsL2DTW> segmentClass = SequenceMocapPoseCoordsL2DTW.class;
        String queryFile = "/home/drking/Documents/Bakalarka/data/class130-actions-segment80_shift16-coords_normPOS-fps12.data";
        String hullDir = "/home/drking/Documents/Bakalarka/data/hdm05-gt-by-class-actions-segment80_shift16-coords_normPOS-fps12"; // Needs: readHullsWithCenters or readHulls
        
        // structures
        ObjectCategoryMgmt categoryMgmt = new ObjectCategoryMgmt("/home/drking/Documents/Bakalarka/data/category_description.txt");
        
        //Class<SequenceMocapPoseCoordsL2DTW> segmentClass = SequenceMocapPoseCoordsL2DTW.class;
        // Read queries
        ObjectMgmt queryMgmt = new ObjectMgmt(categoryMgmt);
        readSegmentQueriesAsActions(queryMgmt, queryFile, segmentClass, false);
//        readQueriesAsActions(queryMgmt, queryFile, segmentClass);

        // Read data (hulls)
        System.out.println("Data:");
        ObjectMgmt dataMgmt = new ObjectMgmt(categoryMgmt);
//        readHulls(dataMgmt, hullDir, segmentClass); // ObjectMocapPoseCoordsL2.class);//segmentClass);
        readHullsWithCenters(dataMgmt, hullDir, segmentClass); // ObjectMocapPoseCoordsL2.class);//segmentClass);
                
        // Querying
        Integer maxK = (fixedKsToEvaluate.length == 0) ? null : Arrays.stream(fixedKsToEvaluate).summaryStatistics().getMax();
        System.out.println("maxK = " + maxK);
        long startTime = System.currentTimeMillis();
        Map<ObjectCategoryMgmt.Category, List<RankingSingleQueryOperation>> origCategoryOperationsMap
                = dataMgmt.executeKNNQueries(queryMgmt, maxK, includeExactMatchInResult, includeMatchFromTheSameSequenceInResult);
//        Map<ObjectCategoryMgmt.Category, List<RankingQueryOperation>> origCategoryOperationsMap
//                = dataMgmt.executeKNNQueries(queryMgmt, maxK, 
//                                        maxK, new HullRerankingCollection(null), 
//                                        includeExactMatchInResult, includeMatchFromTheSameSequenceInResult);
        System.out.println("Querying time: " + ((System.currentTimeMillis() - startTime) / 1000f) + " s");

        // Dump distances to the 1NN and lastNN:
        System.out.println("***** Distances for 1st and last object in each query:");
        for (Map.Entry<ObjectCategoryMgmt.Category, List<RankingSingleQueryOperation>> e : origCategoryOperationsMap.entrySet()) {
            System.out.println("*** Category: " + e.getKey().id + " " + e.getKey().description);
            for (RankingSingleQueryOperation ro : e.getValue()) {
                KNNQueryOperation op = (KNNQueryOperation)ro;
                float distFirst = ro.getAnswer().next().getDistance();
                float distLast = ro.getAnswerDistance();
                System.out.println(" query " + op.getQueryObject().getLocatorURI() + ": [" + distFirst + ", " + distLast + "]");
                Iterator<RankedAbstractObject> ans = ro.getAnswer();
                while (ans.hasNext()) {
                    final RankedAbstractObject o = ans.next();
                    System.out.println("     " + o.getDistance() + " - " + o.getObject().getLocatorURI());
                }
            }
        }
        
        // Classifier
        ObjectMultiCategoryClassifier objectClassifier = new ObjectMultiCategoryClassifier(true);
//              ObjectMultiCategoryClassifier objectClassifier = new ObjectTrainingSampleRatioClassifier(dataMgmt);

        // Evaluation
        if (maxK == null) {
            dataMgmt.evaluateRetrieval(origCategoryOperationsMap, evaluateQueriesIndependently, false, false);
            dataMgmt.evaluateClassification(objectClassifier, origCategoryOperationsMap, 1, false, false);
        } else {
            for (int k : fixedKsToEvaluate) {
                System.out.println("Search evaluation (k=" + k + "):");

                Map<ObjectCategoryMgmt.Category, List<RankingSingleQueryOperation>> categoryOperationsMap = ObjectMgmt.cloneCategorizedRankingOperations(origCategoryOperationsMap, k);
                dataMgmt.evaluateRetrieval(categoryOperationsMap, evaluateQueriesIndependently, true, false);

                // evaluation of classification
                float[][] confMatrix = dataMgmt.evaluateClassification(objectClassifier, categoryOperationsMap, 1, false, false);
//                dataMgmt.saveConfusionMatrixToFile(confMatrix, confMatrixFile, true);
            }
        }
    }

    /** Init queries from the file of individual segments of action of motions. So a query action is combined from multiple segments here to make one motion sequence. */
    private static <E extends LocalAbstractObject> void readSegmentQueriesAsMotions(ObjectMgmt queryMgmt, String queryFile, Class<E> queryFileClass) {
        final StreamGenericAbstractObjectIterator<? extends LocalAbstractObject> queryIter = FileUtils.openDB(queryFileClass, queryFile);
        List<E> sgms = new ArrayList<>();
        String action = null;
        while (queryIter.hasNext()) {
            final E sgm = (E)queryIter.next();
            final Matcher m = MotionIdentification.parseMotionLocator(sgm.getLocatorURI());
            String seqId = m.group(1);
            int actId = Integer.parseInt(m.group(2));
            String curAction = String.format("%s_%d", seqId, actId);
            if (action == null || !action.equals(curAction)) {
                if (action != null) {       // Create new query object, but do not care about its category
                    if (sgms.size() <= 2)
                        System.out.println("  Omitting query object " + action + " due to few segments: " + sgms.size());
                    else
                        queryMgmt.addObject(new SequenceMocapSegment(action, sgms), null, null);
                    sgms.clear();
                }
                action = curAction;
            }
            sgms.add(sgm);
        }
        if (action != null) {       // Create new query object, but do not care about its category
            if (sgms.size() <= 2)
                System.out.println("  Ommiting query object " + action + " due to few segments: " + sgms.size());
            else
                queryMgmt.addObject(new SequenceMocapSegment(action, sgms), null, null);
        }
    }

    /** Init queries from the file of individual segments of actions. So a query action is combined from multiple segments here. There can be multiple action within one motion. */
    private static <E extends LocalAbstractObject> void readSegmentQueriesAsActions(ObjectMgmt queryMgmt, String queryFile, Class<E> queryFileClass, boolean omitOneSeqmentActions) {
        final StreamGenericAbstractObjectIterator<? extends LocalAbstractObject> queryIter = FileUtils.openDB(queryFileClass, queryFile);
        List<E> sgms = new ArrayList<>();
        String action = null;
        while (queryIter.hasNext()) {
            final E sgm = (E)queryIter.next();
            final Matcher m = MotionIdentification.parseMotionLocator(sgm.getLocatorURI());
            String seqId = m.group(1);
            int actId = Integer.parseInt(m.group(2));
            String curAction = String.format("%s_%d_%s_%s", seqId, actId, m.group(3), m.group(4));
            if (action == null || !action.equals(curAction)) {
                if (action != null) {       // Create new query object, but do not care about its category
                    if (omitOneSeqmentActions && sgms.size() <= 2)
                        System.out.println("  Omitting query object " + action + " due to few segments: " + sgms.size());
                    else
                        queryMgmt.addObject(new SequenceMocapSegment(action, sgms), null, null);
                    sgms.clear();
                }
                action = curAction;
            }
            sgms.add(sgm);
        }
        if (action != null) {       // Create new query object, but do not care about its category
            if (sgms.size() <= 2)
                System.out.println("  Ommiting query object " + action + " due to few segments: " + sgms.size());
            else
                queryMgmt.addObject(new SequenceMocapSegment(action, sgms), null, null);
        }
    }

    /** Queries are read from the file as is -- i.e. the file already contains the actions */
    private static <E extends MetaObject> void readActionQueries(ObjectMgmt queryMgmt, String queryFile, Class<E> queryFileClass) {
        final StreamGenericAbstractObjectIterator<? extends LocalAbstractObject> queryIter = FileUtils.openDB(queryFileClass, queryFile);
        while (queryIter.hasNext()) {
            final E sgm = (E)queryIter.next();
            queryMgmt.addObject(new SequenceMocapSegment(sgm.getLocatorURI(), sgm.getObjects()), null, null);
        }
    }

    private static void readHulls(ObjectMgmt dataMgmt, String hullDir, Class<? extends LocalAbstractObject> segmentClass) {
        HullVocabulary voc = new HullVocabulary(hullDir, segmentClass);
        for (HullRepresentation hull : voc.getHulls()) {
            if (hull != null)
                dataMgmt.addObject(new HullRepresentationAsLocalAbstractObject(hull), null, null);
        }
    }

    private static void readHullsWithCenters(ObjectMgmt dataMgmt, String hullDir, Class<? extends LocalAbstractObject> segmentClass) {
        HullVocabulary voc = new HullCenterVocabulary(hullDir, hullDir, segmentClass);
        for (HullRepresentation hull : voc.getHulls()) {
            if (hull != null)
                dataMgmt.addObject(new HullRepresentationAsLocalAbstractObject(hull), null, null);
        }
    }

}
