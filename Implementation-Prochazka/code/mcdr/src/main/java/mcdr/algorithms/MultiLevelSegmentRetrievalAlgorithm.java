package mcdr.algorithms;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import mcdr.objects.utils.SequenceMocapCollection;
import mcdr.preprocessing.segmentation.SegmentConvertor;
import mcdr.preprocessing.segmentation.impl.RegularSegmentConvertor;
import mcdr.sequence.SequenceMocap;
import mcdr.test.utils.ObjectMgmt;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.data.BulkInsertOperation;
import messif.utility.reflection.NoSuchInstantiatorException;
import smf.exceptions.StorageException;
import smf.modules.SequenceStorage;
import static mcdr.algorithms.AbstractRetrievalAlgorithm.LOGGER;
import mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor;
import static mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor.createSequenceConvertors;
import mcdr.preprocessing.transformation.SequenceMocapConvertor;
import mcdr.preprocessing.transformation.impl.MotionImageConvertor;
import mcdr.sequence.KinematicTree;
import messif.algorithms.impl.ParallelSequentialScan;
import messif.operations.AnswerType;
import messif.operations.QueryOperation;
import messif.operations.query.KNNQueryOperation;
import smf.sequences.SequenceFactory;

/**
 *
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MultiLevelSegmentRetrievalAlgorithm<O extends SequenceMocap<?>> extends AbstractRetrievalAlgorithm<List<?>, O> {

    //************ Constants ************//
    // number of threds that are used to sequentially search a bucket
    public static final int THREAD_COUNT = 8;
    // string defining the stiffness parameter
    public static final String PARAM_STIFFNESS = "stiffness";
    //************ Attributes ************//
    // covering factor
    protected final float coveringFactor;
    // multi-level segmentation structure
    private final List<SegmentationLevel> segmentationLevels;
    // extractor of 4,096-feature-vector objects
    protected transient CaffeObjectMotionImageSocketExtractor caffeObjectExtractor;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link MultiLevelSegmentRetrievalAlgorithm}.
     *
     * @param sequenceClass class of sequence
     * @param sequenceDataClass class of the sequence data
     * @param sequenceStorage storage for managing sequences
     * @param minQueryLength minimum length of any possible query
     * @param maxQueryLength maximum length of any possible query
     * @param coveringFactor determines the overlap of segments
     * @param originalFPSRate fps rate of provided data
     * @param reducedFPSRate fps rate to which the provided data are converted
     * @param minCoordValue minimum value of x, y, z coordinate of any possible
     * pose
     * @param maxCoordValue maximum value of x, y, z coordinate of any possible
     * pose
     * @param caffeObjectExtractorHost host on which the segment-image extractor
     * is executed
     * @param caffeObjectExtractorPort port on which the segment-image extractor
     * is executed
     * @throws NoSuchInstantiatorException when an error during creating a
     * sequence factory occurs
     * @throws messif.algorithms.AlgorithmMethodException
     */
    @Algorithm.AlgorithmConstructor(description = "MultiLevelSegmentRetrievalAlgorithm", arguments = {"sequence class", "sequence data class", "sequence storage", "minQueryLength", "maxQueryLength", "coveringFactor", "originalFPSRate", "reducedFPSRate", "minCoordValue", "maxCoordValue", "caffeObjectExtractorHost", "caffeObjectExtractorPort"})
    public MultiLevelSegmentRetrievalAlgorithm(Class<O> sequenceClass, Class<List<?>> sequenceDataClass, SequenceStorage<List<?>, O> sequenceStorage, int minQueryLength, int maxQueryLength, float coveringFactor, int originalFPSRate, int reducedFPSRate, float minCoordValue, float maxCoordValue, String caffeObjectExtractorHost, int caffeObjectExtractorPort) throws NoSuchInstantiatorException, AlgorithmMethodException {
        super("MultiLevelSegmentRetrievalAlgorithm", sequenceClass, sequenceDataClass, sequenceStorage);

        // Checks parameters
        if (minQueryLength <= 0 || maxQueryLength < minQueryLength) {
            throw new AlgorithmMethodException("Query size limits are not valid!");
        }
        if (coveringFactor <= 0f || coveringFactor > 1f) {
            throw new AlgorithmMethodException("Covering factor has to be within (0, 1]!");
        }
        this.coveringFactor = coveringFactor;

        // Creates an extractor of caffe objects through motion images (with PaOaSn normalization)
        this.caffeObjectExtractor = createCaffeObjectExtractor(originalFPSRate, reducedFPSRate, minCoordValue, maxCoordValue, caffeObjectExtractorHost, caffeObjectExtractorPort);

        // Defines the multi-level segmentation structure
        this.segmentationLevels = new LinkedList<>();
        int currentSegmentLength = (int) Math.floor(minQueryLength / (1f - coveringFactor));
        segmentationLevels.add(new SegmentationLevel(currentSegmentLength, (int) Math.floor(currentSegmentLength * coveringFactor), new RegularSegmentConvertor<>(sequenceClass, currentSegmentLength, coveringFactor, 0, true)));
        while (currentSegmentLength * (1f + coveringFactor) < maxQueryLength) {
            currentSegmentLength = (int) Math.floor(currentSegmentLength * (1f + coveringFactor) / (1f - coveringFactor));
            segmentationLevels.add(new SegmentationLevel(currentSegmentLength, (int) Math.floor(currentSegmentLength * coveringFactor), new RegularSegmentConvertor<>(sequenceClass, currentSegmentLength, coveringFactor, 0, true)));
        }
    }

    //************ Methods ************//
    /**
     * Creates an extractor of caffe objects through motion images (with PaOaSn
     * normalization).
     */
    private CaffeObjectMotionImageSocketExtractor createCaffeObjectExtractor(int originalFPSRate, int reducedFPSRate, float minCoordValue, float maxCoordValue, String caffeObjectExtractorHost, int caffeObjectExtractorPort) throws NoSuchInstantiatorException {
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, originalFPSRate, reducedFPSRate, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        MotionImageConvertor motionImageConvertor = new MotionImageConvertor(minCoordValue, maxCoordValue);
        return new CaffeObjectMotionImageSocketExtractor(sequenceConvertors, motionImageConvertor, caffeObjectExtractorHost, caffeObjectExtractorPort);
    }

    /**
     * Logs the multi-level segmentation structure.
     */
    public final void logSegmentManagerStructure() {
        String structureString = "{";
        for (SegmentationLevel segmentationLevel : segmentationLevels) {
            structureString += " [length=" + segmentationLevel.segmentLength + "; shift=" + segmentationLevel.segmentShift + "; count=" + segmentationLevel.getSegmentCount() + "]";
        }
        structureString += " }";
        LOGGER.log(Level.INFO, "Multi-level segmentation structure: {0}", structureString);
    }

    //************ Overrided class AbstractRetrievalAlgorithm ************//
    @Override
    public void insert(BulkInsertOperation operation) throws StorageException {
        long startTime = System.currentTimeMillis();
        int totalSequenceLength = 0;

        // Normalizes the sequence and partitions it into segments whose features are stored within the multi-level segmentation structure
        for (O sequence : (List<O>) operation.getInsertedObjects()) {
            int sequenceLength = sequence.getSequenceLength();
            totalSequenceLength += sequenceLength;

            // Normalizes the sequence, partitions it into segments, extracts and stores the feature for each segment
            LOGGER.log(Level.INFO, "Indexing sequence: {0} | sequence length: {1}", new Object[]{sequence.getLocatorURI(), sequence.getSequenceLength()});
            for (SegmentationLevel segmentationLevel : segmentationLevels) {
                List<O> segments = segmentationLevel.segmentIdentifier.convert(sequence);
                LOGGER.log(Level.INFO, "  segment length: {0} | segment count: {1}", new Object[]{segmentationLevel.segmentLength, segments.size()});

                // Extracts the segment features
                List<LocalAbstractObject> segmentObjects = new LinkedList<>();
                for (O segment : segments) {
                    try {
                        segmentObjects.add(caffeObjectExtractor.extractObject(segment.duplicate()));
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, ex.toString());
                    }
                }

                // Stores the segment features into the storage
                BulkInsertOperation segmentObjectsOperation = new BulkInsertOperation(segmentObjects);
                segmentationLevel.segmentStorage.insert(segmentObjectsOperation);
            }
        }

        // Stores the original sequences
        insertOriginalSequences(operation, false);

        // Logs the segmentation structure
        logSegmentManagerStructure();

        LOGGER.log(Level.INFO, "Inserting processed | sequence count: {0}; total frame count: {1}; average frame count: {2}; time: {3} ms", new Object[]{operation.getNumberInsertedObjects(), totalSequenceLength, (float) totalSequenceLength / operation.getNumberInsertedObjects(), (System.currentTimeMillis() - startTime)});
    }

    /**
     * Stores original sequence data into the sequence storage.
     *
     * @param operation original sequences
     * @param logStructure indicates whether the segmentation structure should
     * be logged
     * @throws StorageException
     */
    public void insertOriginalSequences(BulkInsertOperation operation, boolean logStructure) throws StorageException {
        for (O sequence : (List<O>) operation.getInsertedObjects()) {
            sequenceStorage.insertSequence(sequence);
        }
        if (logStructure) {
            logSegmentManagerStructure();
        }
        LOGGER.log(Level.INFO, "Inserting original sequence data | sequence count: {0}", new Object[]{operation.getNumberInsertedObjects()});
    }

    @Override
    protected void processQuery(RankingSingleQueryOperation operation, float answerSimilarityThreshold, int maxAnswerCount, float allowedRelativeOverlap) {
        long startTime = System.currentTimeMillis();

        // Query params
        float stiffness = operation.getParameter(PARAM_STIFFNESS, float.class, 1f);
        if (stiffness <= 0f || stiffness > 1f) {
            LOGGER.log(Level.SEVERE, "Stiffness has to be within (0, 1]!");
            return;
        }

        // Extracts the query object
        O querySequence = (O) operation.getQueryObject();
        int querySequenceLength = querySequence.getSequenceLength();
        LocalAbstractObject q;
        try {
            q = caffeObjectExtractor.extractObject(querySequence);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Query object cannot be extracted: {0}", ex.toString());
            return;
        }

        // Filter of overlapping subsequences
        SequenceMocapCollection sequenceFilter = new SequenceMocapCollection(allowedRelativeOverlap);

        // Searches corresponding levels
        for (SegmentationLevel segmentationLevel : segmentationLevels) {
            if (querySequenceLength >= segmentationLevel.segmentLength * (1f - coveringFactor) * stiffness
                    && querySequenceLength <= segmentationLevel.segmentLength * (1f + coveringFactor) * (1f / stiffness)) {

                // Searches the level storage to obtain the most similar segments
                QueryOperation<RankedAbstractObject> segmentationLevelOperation = new KNNQueryOperation(q, (int) Math.ceil(maxAnswerCount * 1f / coveringFactor), AnswerType.ORIGINAL_OBJECTS);
                Iterator<? extends RankedAbstractObject> segmentationLevelAnswerIt;
                try {
                    segmentationLevelAnswerIt = segmentationLevel.segmentStorage.getQueryAnswer(segmentationLevelOperation);
                } catch (AlgorithmMethodException | NoSuchMethodException ex) {
                    LOGGER.log(Level.SEVERE, "Error during the accessing the segment storage: {0}", ex.toString());
                    return;
                }

                // Adds the retrieved subsequences to the filter
                while (segmentationLevelAnswerIt.hasNext()) {
                    RankedAbstractObject rao = segmentationLevelAnswerIt.next();

                    // Crops the subsequence
                    String subsequenceName = ObjectMgmt.parseObjectParentSequenceId(rao.getObject());
                    int subsequenceOffset = ObjectMgmt.parseObjectOffset(rao.getObject());
                    int subsequenceLength = ObjectMgmt.parseObjectLength(rao.getObject());
                    O subsequence = getSubsequence(subsequenceName, subsequenceOffset, subsequenceOffset + subsequenceLength);

                    sequenceFilter.add(new RankedAbstractObject(subsequence, rao.getDistance()));
                }
            }
        }

        // Filters ovelapping subsequences and adds the most similar ones to the operation answer
        sequenceFilter.mergeAndfilterOverlappingObjects(operation);

        LOGGER.log(Level.INFO,
                "Query processed (k={5}, radius={6}) | query sequence locator: {0}, query offset: {1}, length: {2}, answer count: {3}, time: {4} ms",
                new Object[]{querySequence.getOriginalSequenceLocator(), querySequence.getOffset(), querySequence.getSequenceLength(), operation.getAnswerCount(), (System.currentTimeMillis() - startTime), maxAnswerCount, answerSimilarityThreshold});
    }

    //************ Deserialization ************//
    /**
     * Initializes the sequence storage after deserialization and insert already
     * indexed sequences.
     *
     * @param originalFPSRate fps rate of provided data
     * @param reducedFPSRate fps rate to which the provided data are converted
     * @param minCoordValue minimum value of x, y, z coordinate of any possible
     * pose
     * @param maxCoordValue maximum value of x, y, z coordinate of any possible
     * pose
     * @param caffeObjectExtractorHost host on which the segment-image extractor
     * is executed
     * @param caffeObjectExtractorPort port on which the segment-image extractor
     * is executed
     * @param sequenceStorage storage for managing sequences
     * @throws NoSuchInstantiatorException
     */
    public void initializeAfterDeserialization(int originalFPSRate, int reducedFPSRate, float minCoordValue, float maxCoordValue, String caffeObjectExtractorHost, int caffeObjectExtractorPort, SequenceStorage<List<?>, O> sequenceStorage) throws NoSuchInstantiatorException {
        this.sequenceFactory = new SequenceFactory<>(sequenceClass, sequenceDataClass);
        this.sequenceStorage = sequenceStorage;

        // Creates segmentation covertors
        for (SegmentationLevel segmentationLevel : segmentationLevels) {
            segmentationLevel.segmentIdentifier = new RegularSegmentConvertor<>(sequenceClass, segmentationLevel.segmentLength, coveringFactor, 0, true);
        }

        // Creates object extractor
        this.caffeObjectExtractor = createCaffeObjectExtractor(originalFPSRate, reducedFPSRate, minCoordValue, maxCoordValue, caffeObjectExtractorHost, caffeObjectExtractorPort);
    }

    /**
     * Class encapsulating information about a single segmentation level
     */
    protected class SegmentationLevel implements Serializable {

        // fixed length of segments
        private final int segmentLength;
        // fixed shift of segmetns
        private final int segmentShift;
        // segmentation method
        private transient SegmentConvertor<O> segmentIdentifier;
        // segment storage
        private final ParallelSequentialScan segmentStorage;

        //************ Constructors ************//
        /**
         * Creates a new instance of {@link SegmentationLevel}.
         *
         * @param segmentLength fixed length of segments
         * @param segmentShift fixed shift of segmetns
         * @param segmentIdentifier segmentation method
         */
        public SegmentationLevel(int segmentLength, int segmentShift, SegmentConvertor<O> segmentIdentifier) {
            this.segmentLength = segmentLength;
            this.segmentShift = segmentShift;
            this.segmentIdentifier = segmentIdentifier;
            this.segmentStorage = new ParallelSequentialScan(THREAD_COUNT);
        }

        //************ Methods ************//
        /**
         * Returns the number of segments stored within the storage.
         *
         * @return the number of segments stored within the storage
         */
        public int getSegmentCount() {
            try {
                return segmentStorage.getObjectCount();
            } catch (AlgorithmMethodException ex) {
                LOGGER.log(Level.SEVERE, ex.toString());
                return -1;
            }
        }
    }
}
