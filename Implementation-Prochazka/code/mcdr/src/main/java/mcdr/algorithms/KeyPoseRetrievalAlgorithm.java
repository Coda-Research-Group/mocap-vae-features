package mcdr.algorithms;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import mcdr.objects.ObjectMocapPose;
import mcdr.objects.utils.ObjectMocapPoseRootCollection;
import mcdr.objects.utils.ObjectMocapPoseRootCollection.RootRankedAbstractObject;
import mcdr.objects.utils.SequenceMocapCollection;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.operations.AnswerType;
import messif.operations.Approximate;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.data.BulkInsertOperation;
import messif.operations.query.ApproxRangeQueryOperation;
import messif.operations.query.RangeQueryOperation;
import messif.utility.reflection.NoSuchInstantiatorException;
import smf.exceptions.StorageException;
import smf.modules.SequenceStorage;
import smf.sequences.IndexableSequence;

/**
 *
 * @param <T> type of the sequence data, usually a static array of a primitive
 * type or {@link java.util.List}
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class KeyPoseRetrievalAlgorithm<T, O extends IndexableSequence<T>> extends AbstractRetrievalAlgorithm<T, O> {

    //************ Query parameters ************//
    // parameter string for a fixed distance between neighboring query key poses
    public static final String PARAM_KEY_POSE_DISTANCE = "key_pose_distance";
    // parameter string for controlling the admissible distance between two subsequence key poses  
    public static final String PARAM_STIFFNESS = "stiffness";
    // parameter string for a threshold for similarity of two poses
    public static final String PARAM_POSE_SIMILARITY_THRESHOLD = "pose_similarity_threshold";
    //************ Algorithm attributes ************//
    // index for poses of all sequences
    protected final Algorithm poseIndexAlgorithm;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link KeyPoseRetrievalAlgorithm}.
     *
     * @param sequenceClass class of sequence
     * @param sequenceDataClass class of the sequence data, usually a static
     * array of a primitive type or {@link java.util.List}
     * @param sequenceStorage storage for managing sequences
     * @param poseIndexAlgorithm algorithm for indexing poses
     * @throws NoSuchInstantiatorException when an error during creating a
     * sequence factory occurs
     */
    @Algorithm.AlgorithmConstructor(description = "Key-Pose Retrieval Algorithm", arguments = {"sequence class", "sequence data class", "sequence storage", "algorithm for indexing poses"})
    public KeyPoseRetrievalAlgorithm(Class<O> sequenceClass, Class<T> sequenceDataClass, SequenceStorage<T, O> sequenceStorage, Algorithm poseIndexAlgorithm) throws NoSuchInstantiatorException {
        super("Key-Pose Algorithm", sequenceClass, sequenceDataClass, sequenceStorage);
        this.poseIndexAlgorithm = poseIndexAlgorithm;

        // Enables algorithm to execute operations at background
        this.poseIndexAlgorithm.setOperationsThreadPool(Executors.newFixedThreadPool(256));
    }

    //************ Methods ************//
    /**
     * Sequentially scans the predefined interval within the list of poses and
     * returns the distance and index of pose which is the most similar to the
     * query pose.
     *
     * @param queryPose pose to which the most similar pose is retrieved
     * @param poses list of poses from which the most similar pose is retrieved
     * @param fromIndex start index of the interval within the list of poses
     * @param toIndex end index of the interval within the list of poses
     * @return the distance and index of pose which is the most similar to the
     * query pose
     */
    private Entry<Float, Integer> getMostSimilarPose(LocalAbstractObject queryPose, O poses, int fromIndex, int toIndex) {
        float minDist = Float.MAX_VALUE;
        int poseIndex = -1;
        for (int i = fromIndex; i < toIndex; i++) {
            float dist = poses.getSequenceItem(i).getDistance(queryPose);
            if (minDist > dist) {
                minDist = dist;
                poseIndex = i;
            }
        }
        if (poseIndex == -1) {
            minDist = Float.NaN;
        }
        return new AbstractMap.SimpleEntry<>(minDist, poseIndex);
    }

    //************ Overrided class AbstractRetrievalAlgorithm ************//
    /**
     * Inserts a bulk of motion sequences. Each motion sequence is divided to
     * poses and each pose is additionally indexed.
     *
     * @param operation operation to be processed
     * @throws StorageException when an error during inserting sequences into
     * the sequence storage occurs
     */
    @Override
    public void insert(BulkInsertOperation operation) throws StorageException {
        long startTime = System.currentTimeMillis();
        int totalPoseCount = 0;

        for (O seq : (List<O>) operation.getInsertedObjects()) {
            int poseCount = seq.getSequenceLength();
            totalPoseCount += poseCount;
            List<LocalAbstractObject> poseList = new ArrayList<>(poseCount);
            for (int i = 0; i < poseCount; i++) {
                poseList.add(seq.getSequenceItem(i));
            }

            // Indexes poses of the current motion sequence
            try {
                poseIndexAlgorithm.executeOperation(new BulkInsertOperation(poseList));
            } catch (NoSuchElementException | AlgorithmMethodException | NoSuchMethodException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            // Stores the original sequence
            sequenceStorage.insertSequence(seq);

        }

        long finishTime = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO, "Inserting processed | sequence count: {0}, frame count: {1}, time: {2} ms", new Object[]{operation.getNumberInsertedObjects(), totalPoseCount, finishTime});
    }

    @Override
    protected void processQuery(RankingSingleQueryOperation operation, float answerSimilarityThreshold, int maxAnswerCount, float allowedRelativeOverlap) {
        long startTime = System.currentTimeMillis();

        // Compulsory query parameters
        Integer keyPoseDistance = operation.getParameter(PARAM_KEY_POSE_DISTANCE, Integer.class);
        Float stiffness = operation.getParameter(PARAM_STIFFNESS, Float.class);
        Float poseSimilarityThreshold = operation.getParameter(PARAM_POSE_SIMILARITY_THRESHOLD, Float.class);
        if (keyPoseDistance == null || stiffness == null || poseSimilarityThreshold == null) {
            LOGGER.log(Level.SEVERE, "Compulsory query parameters '" + PARAM_KEY_POSE_DISTANCE + "', '" + PARAM_STIFFNESS + "', or '" + PARAM_POSE_SIMILARITY_THRESHOLD + "' have not been specified!");
            return;
        }

        // Executes the range query for each query key pose to identify the set of similar poses
        O q = (O) operation.getQueryObject();
        int queryKeyPoseCount = q.getSequenceLength() / keyPoseDistance;
        final int intervalStart = (int) (keyPoseDistance * stiffness);
        final int intervalEnd = (int) (keyPoseDistance / stiffness);
        List<Future<? extends RankingSingleQueryOperation>> keyPoseOperations = new ArrayList<>(queryKeyPoseCount);
        for (int rootNo = 0; rootNo < queryKeyPoseCount; rootNo++) {
            int queryKeyPoseIndex = rootNo * keyPoseDistance;

            // Creates either the precise or approximate range query operation
            RangeQueryOperation keyPoseOperation;
            if (!(operation instanceof Approximate)) {
                keyPoseOperation = new RangeQueryOperation(q.getSequenceItem(queryKeyPoseIndex), poseSimilarityThreshold, AnswerType.ORIGINAL_OBJECTS);
            } else {
                Approximate approxOperation = (Approximate) operation;
                keyPoseOperation = new ApproxRangeQueryOperation(q.getSequenceItem(queryKeyPoseIndex), poseSimilarityThreshold, AnswerType.ORIGINAL_OBJECTS, approxOperation.getLocalSearchParam(), approxOperation.getLocalSearchType(), approxOperation.getRadiusGuaranteed());
            }
            keyPoseOperations.add(poseIndexAlgorithm.backgroundExecuteOperation(keyPoseOperation));
        }

        // Waits for executed operations and filters retrieved poses
        ObjectMocapPoseRootCollection poseCollection = new ObjectMocapPoseRootCollection(2 * keyPoseDistance);
        for (int rootNo = 0; rootNo < queryKeyPoseCount; rootNo++) {

            // Sets the root number which the retrieved poses are associated with
            poseCollection.setRootNo(rootNo);

            try {

                // Waits for the executed operation
                RankingSingleQueryOperation keyPoseOperation = keyPoseOperations.get(rootNo).get();

                // Filters retrieved poses
                for (Iterator<RankedAbstractObject> keyPoseOperationAnswerIterator = keyPoseOperation.getAnswer(); keyPoseOperationAnswerIterator.hasNext();) {
                    poseCollection.add(keyPoseOperationAnswerIterator.next());
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        int poseCount = poseCollection.getObjectCountWithDuplicates();
        int uniquePoseCount = poseCollection.getObjectCount();

        // Merges the identified sets of similar poses together and filters out (ignores) close and less-relevant poses
        Map<String, Collection<RootRankedAbstractObject>> sequencePoseMap;
        if (maxAnswerCount == Integer.MAX_VALUE) {
            sequencePoseMap = poseCollection.filterOverlappingObjects();
        } else {
            sequencePoseMap = poseCollection.filterOverlappingObjects(maxAnswerCount * queryKeyPoseCount);
        }

        // Filters retrieved subsequences
        SequenceMocapCollection sequenceFilter = new SequenceMocapCollection(allowedRelativeOverlap);

        // Identifies a single subsequence for each non-filtered pose similar to some query key pose
        int candidateAnswerCount = 0;
        float candidateAnswerCumulativeDist = 0f;
        for (Entry<String, Collection<RootRankedAbstractObject>> sequenceRootPoseEntry : sequencePoseMap.entrySet()) {
            String sequenceId = sequenceRootPoseEntry.getKey();
            Collection<RootRankedAbstractObject> sequenceRootPoses = sequenceRootPoseEntry.getValue();
            candidateAnswerCount += sequenceRootPoses.size();
            O dataSequence = sequenceStorage.getSequence(sequenceId);

            // Identifies a single subsequence for each non-filtered pose similar to some query key pose within the qiven sequence
            int dataPoseCount = dataSequence.getSequenceLength();
            for (RootRankedAbstractObject rootPose : sequenceRootPoses) {
                int rootNo = rootPose.getRootNumber();
                int rootPoseIndex = ((ObjectMocapPose) rootPose.getObject()).getFrameNo();
                float dist = rootPose.getDistance();

                // Checks whether there is enough frames before and after the root pose to identify all the left and right key poses
                if (rootPoseIndex >= rootNo * intervalStart && rootPoseIndex < dataPoseCount - (queryKeyPoseCount - rootNo) * intervalStart) {

                    // Locates left key poses
                    Entry<Float, Integer> rpi = new AbstractMap.SimpleEntry<>(Float.NaN, rootPoseIndex);
                    for (int f = rootNo - 1; f >= 0; f--) {
                        rpi = getMostSimilarPose(q.getSequenceItem(f * keyPoseDistance), dataSequence, Math.max(f * intervalStart, rpi.getValue() - intervalEnd), rpi.getValue() - intervalStart + 1);
                        dist += rpi.getKey();
                    }
                    int firstPoseIndex = rpi.getValue();

                    // Locates right key poses
                    rpi = new AbstractMap.SimpleEntry<>(Float.NaN, rootPoseIndex);
                    for (int f = rootNo + 1; f < queryKeyPoseCount; f++) {
                        rpi = getMostSimilarPose(q.getSequenceItem(f * keyPoseDistance), dataSequence, rpi.getValue() + intervalStart, Math.min(dataPoseCount - (queryKeyPoseCount - f - 1) * intervalStart, rpi.getValue() + intervalEnd + 1));
                        dist += rpi.getKey();
                    }
                    int lastPoseIndex = rpi.getValue();

                    // Adds the subsequence to the operation
                    O answer = getSubsequence(dataSequence, firstPoseIndex, lastPoseIndex + 1);
//                    dist = answerSubsequence.getSequence().getDistance(operation.getQueryObject());
                    dist = dist / (float) queryKeyPoseCount;
                    candidateAnswerCumulativeDist += dist;
                    if (dist <= answerSimilarityThreshold) {
                        sequenceFilter.add(new RankedAbstractObject(answer.getSequence(), dist));
                    }
                }
            }
        }

        // Filters retrieved subsequences and adds the relevant ones to the operation answer
        sequenceFilter.mergeAndfilterOverlappingObjects(operation);

        long finishTime = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO,
                "Query processed | query sequence locator: {0}, query offset: {1}, length: {2}, key-pose count: {3}, pose count: {4}, unique pose count: {5}, candidate answer count: {6}, average candidate answer distance: {7}, answer count: {8}, time: {9} ms",
                new Object[]{q.getOriginalSequenceLocator(), q.getOffset(), q.getSequenceLength(), queryKeyPoseCount, poseCount, uniquePoseCount, candidateAnswerCount, (candidateAnswerCumulativeDist / candidateAnswerCount), operation.getAnswerCount(), finishTime});
    }
}
