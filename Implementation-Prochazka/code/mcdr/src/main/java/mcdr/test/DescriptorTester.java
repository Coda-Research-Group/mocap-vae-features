package mcdr.test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.objects.impl.ObjectMotionWordNMatches;
import mcdr.objects.impl.ObjectMotionWordSoftAssignment;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.impl.*;
import mcdr.test.utils.ObjectCategoryMgmt;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.operations.RankingQueryOperation;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.query.KNNQueryOperation;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class DescriptorTester {

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Class<? extends SequenceMocap<?>> objectClass = SequenceMocapPoseCoordsL2DTW.class; // coords
//        Class<? extends SequenceMocap<?>> objectClass = SequenceMocapPoseCoordsL2DTWSegments.class;
        Class<? extends LocalAbstractObject> objectClass = SequenceMotionWordsDTW.class;
//        Class<? extends LocalAbstractObject> objectClass = SequenceMotionWordsNMatchesDTW.class;
//        Class<? extends LocalAbstractObject> objectClass = SequenceMotionWordsSoftAssignmentDTW.class;
//        Class<? extends LocalAbstractObject> objectClass = SequenceMotionWordsNGramsJaccard.class;
        Class<? extends LocalAbstractObject> objectClass = SequenceSegmentCodeListDTW.class;

        ObjectMotionWordNMatches.nMatches = 1;
        ObjectMotionWordNMatches.maxPartsToMatch = 4;
        ObjectMotionWordSoftAssignment.maxPartsToMatch = 6;
//        ObjectMotionWordSoftAssignment.maxPartsToMatch = Integer.MAX_VALUE;
        SequenceMotionWordsNGramsJaccard.nGramSize = 1;

        // retrieval params
        final boolean includeExactMatchInResult = false;
        // values of k to be evaluated
//        int[] fixedKsToEvaluate = new int[]{};
        int[] fixedKsToEvaluate = new int[]{4};
//        int[] fixedKsToEvaluate = IntStream.range(1, 21).toArray();
//        int[] fixedKsToEvaluate = new int[]{1, 3, 5, 10, 15, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        final String[] ignoredCategoryIds = null;
//        final String[] ignoredCategoryIds = new String[]{"56", "57", "58", "59", "60", "61", "138", "139"}; // HDM05-122
        final boolean includeMatchFromTheSameSequenceInResult = true;
        final boolean evaluateQueriesIndependently = true; // indicates whether each query is evaluated independently, or only one multi-object query is constructed for each category
        final boolean restrictDataObjectsByQueries = false;
        final boolean parseDataCategoriesFromOverlappingQueries = false;

        // Printing global variables
        System.out.println("===== GLOBAL PARAMS =====");
        printStaticClassVariables(objectClass);
        System.out.println();

        final String objectLocatorRegExQueryMgmt = null;
//        final String objectLocatorRegExQueryMgmt = ".*-M_.*"; // PKU-MMD CV-test
//        final String objectLocatorRegExQueryMgmt = "(0291-L|0291-M|0291-R|0292-L|0292-M|0292-R|0293-L|0293-M|0293-R|0294-L|0294-M|0294-R|0295-L|0295-M|0295-R|0296-L|0296-M|0296-R|0297-L|0297-M|0297-R|0298-L|0298-M|0298-R|0299-L|0299-M|0299-R|0300-L|0300-M|0300-R|0301-L|0301-M|0301-R|0302-L|0302-M|0302-R|0303-L|0303-M|0303-R|0304-L|0304-M|0304-R|0305-L|0305-M|0305-R|0306-L|0306-M|0306-R|0307-L|0307-M|0307-R|0308-L|0308-M|0308-R|0309-L|0309-M|0309-R|0310-L|0310-M|0310-R|0311-L|0311-M|0311-R|0312-L|0312-M|0312-R|0313-L|0313-M|0313-R|0314-L|0314-M|0314-R|0315-L|0315-M|0315-R|0316-L|0316-M|0316-R|0317-L|0317-M|0317-R|0318-L|0318-M|0318-R|0319-L|0319-M|0319-R|0320-L|0320-M|0320-R|0321-L|0321-M|0321-R|0322-L|0322-M|0322-R|0323-L|0323-M|0323-R|0324-L|0324-M|0324-R|0325-L|0325-M|0325-R|0326-L|0326-M|0326-R|0327-L|0327-M|0327-R|0328-L|0328-M|0328-R|0329-L|0329-M|0329-R|0330-L|0330-M|0330-R|0331-L|0331-M|0331-R|0332-L|0332-M|0332-R|0333-L|0333-M|0333-R|0334-L|0334-M|0334-R)_.*"; // PKU-MMD CS-test
        final String objectLocatorRegExDataMgmt = null;
//        final String objectLocatorRegExDataMgmt = ".*-(L|R)_.*"; // PKU-MMD CV-train
//        final String objectLocatorRegExDataMgmt = "(0002-L|0002-M|0002-R|0003-L|0003-M|0003-R|0004-L|0004-M|0004-R|0005-L|0005-M|0005-R|0006-L|0006-M|0006-R|0007-L|0007-M|0007-R|0008-L|0008-M|0008-R|0009-L|0009-M|0009-R|0010-L|0010-M|0010-R|0011-L|0011-M|0011-R|0012-L|0012-M|0012-R|0013-L|0013-M|0013-R|0014-L|0014-M|0014-R|0015-L|0015-M|0015-R|0016-L|0016-M|0016-R|0017-L|0017-M|0017-R|0018-L|0018-M|0018-R|0019-L|0019-M|0019-R|0020-L|0020-M|0020-R|0021-L|0021-M|0021-R|0022-L|0022-M|0022-R|0023-L|0023-M|0023-R|0024-L|0024-M|0024-R|0025-L|0025-M|0025-R|0026-L|0026-M|0026-R|0027-L|0027-M|0027-R|0028-L|0028-M|0028-R|0029-L|0029-M|0029-R|0030-L|0030-M|0030-R|0031-L|0031-M|0031-R|0032-L|0032-M|0032-R|0033-L|0033-M|0033-R|0034-L|0034-M|0034-R|0035-L|0035-M|0035-R|0036-L|0036-M|0036-R|0037-L|0037-M|0037-R|0038-L|0038-M|0038-R|0039-L|0039-M|0039-R|0040-L|0040-M|0040-R|0041-L|0041-M|0041-R|0042-L|0042-M|0042-R|0043-L|0043-M|0043-R|0044-L|0044-M|0044-R|0045-L|0045-M|0045-R|0046-L|0046-M|0046-R|0047-L|0047-M|0047-R|0048-L|0048-M|0048-R|0049-L|0049-M|0049-R|0050-L|0050-M|0050-R|0051-L|0051-M|0051-R|0052-L|0052-M|0052-R|0053-L|0053-M|0053-R|0054-L|0054-M|0054-R|0055-L|0055-M|0055-R|0056-L|0056-M|0056-R|0057-L|0057-M|0057-R|0058-L|0058-M|0058-R|0059-L|0059-M|0059-R|0060-M|0060-R|0061-L|0061-M|0061-R|0062-L|0062-M|0062-R|0063-L|0063-M|0063-R|0064-L|0064-M|0064-R|0065-L|0065-M|0065-R|0066-L|0066-M|0066-R|0067-L|0067-M|0067-R|0068-L|0068-M|0068-R|0069-L|0069-M|0069-R|0070-L|0070-M|0070-R|0071-L|0071-M|0071-R|0072-L|0072-M|0072-R|0073-L|0073-M|0073-R|0074-L|0074-M|0074-R|0075-L|0075-M|0075-R|0076-L|0076-M|0076-R|0077-L|0077-M|0077-R|0078-L|0078-M|0078-R|0079-L|0079-M|0079-R|0080-L|0080-M|0080-R|0081-L|0081-M|0081-R|0082-L|0082-M|0082-R|0083-L|0083-M|0083-R|0084-L|0084-M|0084-R|0085-L|0085-M|0085-R|0086-L|0086-M|0086-R|0087-L|0087-M|0087-R|0088-L|0088-M|0088-R|0089-L|0089-M|0089-R|0090-L|0090-M|0090-R|0091-L|0091-M|0091-R|0092-L|0092-M|0092-R|0093-L|0093-M|0093-R|0094-L|0094-M|0094-R|0095-L|0095-M|0095-R|0096-L|0096-M|0096-R|0097-L|0097-M|0097-R|0098-L|0098-M|0098-R|0099-L|0099-M|0099-R|0100-L|0100-M|0100-R|0101-L|0101-M|0101-R|0102-L|0102-M|0102-R|0103-L|0103-M|0103-R|0104-L|0104-M|0104-R|0105-L|0105-M|0105-R|0106-L|0106-M|0106-R|0107-L|0107-M|0107-R|0108-L|0108-M|0108-R|0109-L|0109-M|0109-R|0110-L|0110-M|0110-R|0111-L|0111-M|0111-R|0112-L|0112-M|0112-R|0113-L|0113-M|0113-R|0114-L|0114-M|0114-R|0115-L|0115-M|0115-R|0116-L|0116-M|0116-R|0117-L|0117-M|0117-R|0118-L|0118-M|0118-R|0119-L|0119-M|0119-R|0120-L|0120-M|0120-R|0121-L|0121-M|0121-R|0122-L|0122-M|0122-R|0123-L|0123-M|0123-R|0124-L|0124-M|0124-R|0125-L|0125-M|0125-R|0126-L|0126-M|0126-R|0127-L|0127-M|0127-R|0128-L|0128-M|0128-R|0129-L|0129-M|0129-R|0130-L|0130-M|0130-R|0131-L|0131-M|0131-R|0132-L|0132-M|0132-R|0133-L|0133-M|0133-R|0134-L|0134-M|0134-R|0135-L|0135-M|0135-R|0136-L|0136-M|0136-R|0137-L|0137-M|0137-R|0138-L|0138-M|0138-R|0139-L|0139-M|0139-R|0140-L|0140-M|0140-R|0141-L|0141-M|0141-R|0142-L|0142-M|0142-R|0143-L|0143-M|0143-R|0144-L|0144-M|0144-R|0145-L|0145-M|0145-R|0146-L|0146-M|0146-R|0147-L|0147-M|0147-R|0148-L|0148-M|0148-R|0149-L|0149-M|0149-R|0150-L|0150-M|0150-R|0151-L|0151-M|0151-R|0152-L|0152-M|0152-R|0153-L|0153-M|0153-R|0154-L|0154-M|0154-R|0155-L|0155-M|0155-R|0156-L|0156-M|0156-R|0157-L|0157-M|0157-R|0158-L|0158-M|0158-R|0159-L|0159-M|0159-R|0160-L|0160-M|0160-R|0161-L|0161-M|0161-R|0162-L|0162-M|0162-R|0163-L|0163-M|0163-R|0164-L|0164-M|0164-R|0165-L|0165-M|0165-R|0166-L|0166-M|0166-R|0167-L|0167-M|0167-R|0168-L|0168-M|0168-R|0169-L|0169-M|0169-R|0170-L|0170-M|0170-R|0171-L|0171-M|0171-R|0172-L|0172-M|0172-R|0173-L|0173-M|0173-R|0174-L|0174-M|0174-R|0175-L|0175-M|0175-R|0176-L|0176-M|0176-R|0177-L|0177-M|0177-R|0178-L|0178-M|0178-R|0179-L|0179-M|0179-R|0180-L|0180-M|0180-R|0181-L|0181-M|0181-R|0182-L|0182-M|0182-R|0183-L|0183-M|0183-R|0184-L|0184-M|0184-R|0185-L|0185-M|0185-R|0186-L|0186-M|0186-R|0187-L|0187-M|0187-R|0188-L|0188-M|0188-R|0189-L|0189-M|0189-R|0190-L|0190-M|0190-R|0191-L|0191-M|0191-R|0192-L|0192-M|0192-R|0193-L|0193-M|0193-R|0194-L|0194-M|0194-R|0195-L|0195-M|0195-R|0196-L|0196-M|0196-R|0197-L|0197-M|0197-R|0198-L|0198-M|0198-R|0199-L|0199-M|0199-R|0200-L|0200-M|0200-R|0201-L|0201-M|0201-R|0202-L|0202-M|0202-R|0203-L|0203-M|0203-R|0204-L|0204-M|0204-R|0205-L|0205-M|0205-R|0206-L|0206-M|0206-R|0207-L|0207-M|0207-R|0208-L|0208-M|0208-R|0209-L|0209-M|0209-R|0210-L|0210-M|0210-R|0211-L|0211-M|0211-R|0212-L|0212-M|0212-R|0213-L|0213-M|0213-R|0214-L|0214-M|0214-R|0215-L|0215-M|0215-R|0216-L|0216-M|0216-R|0217-L|0217-M|0217-R|0218-L|0218-M|0218-R|0219-L|0219-M|0219-R|0220-L|0220-M|0220-R|0221-L|0221-M|0221-R|0222-L|0222-M|0222-R|0223-L|0223-M|0223-R|0224-L|0224-M|0224-R|0225-L|0225-M|0225-R|0226-L|0226-M|0226-R|0227-L|0227-M|0227-R|0228-L|0228-M|0228-R|0229-L|0229-M|0229-R|0230-L|0230-M|0230-R|0231-L|0231-M|0231-R|0232-L|0232-M|0232-R|0233-L|0233-M|0233-R|0234-L|0234-M|0234-R|0235-L|0235-M|0235-R|0236-L|0236-M|0236-R|0237-L|0237-M|0237-R|0238-L|0238-M|0238-R|0239-L|0239-M|0239-R|0240-L|0240-M|0240-R|0241-L|0241-M|0241-R|0242-L|0242-M|0242-R|0243-L|0243-M|0243-R|0244-L|0244-M|0244-R|0245-L|0245-M|0245-R|0246-L|0246-M|0246-R|0251-L|0251-M|0251-R|0252-L|0252-M|0252-R|0253-L|0253-M|0253-R|0254-L|0254-M|0254-R|0255-L|0255-M|0255-R|0256-L|0256-M|0256-R|0257-L|0257-M|0257-R|0258-L|0258-M|0258-R|0259-L|0259-M|0259-R|0260-L|0260-M|0260-R|0261-L|0261-M|0261-R|0262-L|0262-M|0262-R|0263-L|0263-M|0263-R|0264-L|0264-M|0264-R|0265-L|0265-M|0265-R|0266-L|0266-M|0266-R|0267-L|0267-M|0267-R|0268-L|0268-M|0268-R|0269-L|0269-M|0269-R|0270-L|0270-M|0270-R|0271-L|0271-M|0271-R|0272-L|0272-M|0272-R|0273-L|0273-M|0273-R|0274-L|0274-M|0274-R|0275-L|0275-M|0275-R|0276-L|0276-M|0276-R|0277-L|0277-M|0277-R|0278-L|0278-M|0278-R|0279-L|0279-M|0279-R|0280-L|0280-M|0280-R|0281-L|0281-M|0281-R|0282-L|0282-M|0282-R|0283-L|0283-M|0283-R|0284-L|0284-M|0284-R|0285-L|0285-M|0285-R|0286-L|0286-M|0286-R|0287-L|0287-M|0287-R|0288-L|0288-M|0288-R|0289-L|0289-M|0289-R|0290-L|0290-M|0290-R|0335-L|0335-M|0335-R|0336-L|0336-M|0336-R|0337-L|0337-M|0337-R|0338-L|0338-M|0338-R|0339-L|0339-M|0339-R|0340-L|0340-M|0340-R|0341-L|0341-M|0341-R|0342-L|0342-M|0342-R|0343-L|0343-M|0343-R|0344-L|0344-M|0344-R|0345-L|0345-M|0345-R|0346-L|0346-M|0346-R|0347-L|0347-M|0347-R|0348-L|0348-M|0348-R|0349-L|0349-M|0349-R|0350-L|0350-M|0350-R|0351-L|0351-M|0351-R|0352-L|0352-M|0352-R|0353-L|0353-M|0353-R|0354-L|0354-M|0354-R|0355-L|0355-M|0355-R|0356-L|0356-M|0356-R|0357-L|0357-M|0357-R|0358-L|0358-M|0358-R|0359-L|0359-M|0359-R|0360-L|0360-M|0360-R|0361-L|0361-M|0361-R|0362-L|0362-M|0362-R|0363-L|0363-M|0363-R|0364-L|0364-M|0364-R)_.*"; // PKU-MMD CS-train

//        final String batchNamePrefix = "e:/datasets/mocap/hdm05/";
//        final String batchNamePrefix = "y:/datasets/mocap/hdm05/motion_words/quantized/";
        final String batchNamePrefix = "/home/drking/Documents/bakalarka/mocap-vae-features/";

        for (String batchNameFile : new String[]{
//            "hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-pivots-kmedoids-350.data"
//            "hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-overlays5-pivots-kmedoids-350.data"
//            "hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-pivots-kmedoids-350-softassign-D20K6.data"
//            "hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-pivots-kmedoids-350-softassign-D20K6.data"
//            "hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12-quantized-pivots1000-maxlvl1-leaf240-random-filtered.data"
            
//            "kmeans-1000-softD2K20.data", "kmeans-1000-softD4K20.data",
//            "kmeans-1500-softD2K20.data", "kmeans-1500-softD4K20.data",
//            "kmeans-3000-softD2K20.data", "kmeans-3000-softD4K20.data",
//            "kmeans-350-softD2K20.data", "kmeans-350-softD4K20.data",
//            "kmeans-500-softD2K20.data", "kmeans-500-softD4K20.data",
//            "kmeans-750-softD2K20.data", "kmeans-750-softD4K20.data"
//            "hdm05-class130-actions-coords_normPOS-fps12-quantized-gt-hull-optimized-center.data"
//            "Implementation-Prochazka/data/leave-one-out/KMedoidsFastPAM--kmeans.k_400.composite"
//            "runs/hdm05/all/beta=1,latent_dim=256/lightning_logs/version_0/predictions.data.gz"
//            "data/hdm05/2version/class130-actions-segment80_shift16-coords_normPOS-fps12.data"
//            "data/hdm05/2version/class130-actions-coords_normPOS-fps12.data"
            "data/hdm05/quantized/predictions.data"

        }) {
            final String batchName = batchNamePrefix + batchNameFile;
            System.out.println("===== NEW EXPERIMENT: " + batchName + " =====");

            final String queryFile = batchName;
            final String dataFile = batchName;

            // structures
            ObjectCategoryMgmt categoryMgmt = new ObjectCategoryMgmt("/home/drking/Documents/bakalarka/mocap-vae-features/Implementation-Prochazka/data/leave-one-out/category_description_short.txt");
            ObjectMgmt queryMgmt = new ObjectMgmt(categoryMgmt);
            ObjectMgmt dataMgmt = new ObjectMgmt(categoryMgmt);

            // queries
            System.out.println("Queries:");
            queryMgmt.read(objectClass, queryFile, objectLocatorRegExQueryMgmt, ignoredCategoryIds, null, null, true);

            // data
            System.out.println("Data:");
            dataMgmt.read(objectClass, dataFile, objectLocatorRegExDataMgmt, ignoredCategoryIds, (!restrictDataObjectsByQueries) ? null : queryMgmt.getParentSequenceIds(), (parseDataCategoriesFromOverlappingQueries) ? queryMgmt : null, true);
//            dataMgmt.storeRandomObjects("d:/temp/2D-CS-P-randomActionClassSelection10.data", 10);

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

    public static void printStaticClassVariables(Class<?> clazz) throws IllegalArgumentException, IllegalAccessException {
        System.out.println("Static params of class " + clazz.getName() + ":");
        for (Field field : clazz.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                System.out.println("  " + field.getName() + " = " + field.get(null).toString());
            }
        }
    }

    private static Map<ObjectCategoryMgmt.Category, List<RankingQueryOperation>> mergeAnswers(Map<ObjectCategoryMgmt.Category, List<RankingQueryOperation>> answerFrom, Map<ObjectCategoryMgmt.Category, List<RankingQueryOperation>> answerTo) {
        for (Map.Entry<ObjectCategoryMgmt.Category, List<RankingQueryOperation>> opsEntry : answerTo.entrySet()) {
            for (RankingQueryOperation op : opsEntry.getValue()) {
                RankingQueryOperation opFrom = null;
                String locTo = ((KNNQueryOperation) op).getQueryObject().getLocatorURI();
                for (RankingQueryOperation opTmp : answerFrom.get(opsEntry.getKey())) {
                    String locFrom = ((KNNQueryOperation) opTmp).getQueryObject().getLocatorURI();
                    if (locTo.equals(locFrom)) {
                        opFrom = opTmp;
                        break;
                    }
                }
                Iterator<RankedAbstractObject> answerIt = opFrom.getAnswer();
                while (answerIt.hasNext()) {
                    RankedAbstractObject rao = answerIt.next();
                    op.addToAnswer(rao.getObject(), rao.getDistance(), null);
                }
            }
        }
        return answerTo;
    }

}
