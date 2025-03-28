package mcdr.algorithms;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.algorithms.Algorithm;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.AbstractOperation;
import messif.operations.AnswerType;
import messif.operations.RankingSingleQueryOperation;
import messif.operations.data.BulkInsertOperation;
import messif.operations.query.GetAllObjectsQueryOperation;
import messif.operations.query.GetObjectByLocatorOperation;
import messif.operations.query.GetObjectsByLocatorsOperation;
import messif.operations.query.GetRandomObjectsQueryOperation;
import messif.operations.query.KNNQueryOperation;
import messif.operations.query.RangeQueryOperation;
import messif.utility.reflection.NoSuchInstantiatorException;
import smf.exceptions.StorageException;
import smf.modules.SequenceStorage;
import smf.sequences.IndexableSequence;
import smf.sequences.Sequence;
import smf.sequences.SequenceFactory;

/**
 *
 * @param <T> type of the sequence data, usually a static array of a primitive
 * type or {@link java.util.List}
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class AbstractRetrievalAlgorithm<T, O extends IndexableSequence<T>> extends Algorithm {

    //************ Query parameters ************//
    // parameter string controlling the admissible overlap between two retrieved subsequences
    public static final String PARAM_ALLOWED_RELATIVE_OVERLAP = "allowed_relative_overlap";
    // parameter string defining the sequence locator
    public static final String PARAM_SEQ_LOCATOR = "seq_locator";
    // parameter string defining the locator, offset, and length of specific subsequences
    public static final String PARAM_SUBSEQS = "subseqs";
    //************ Sequence properties ************//
    // class of sequence
    protected final Class<O> sequenceClass;
    // class of the sequence data, usually a static array of a primitive type or {@link java.util.List}
    protected final Class<T> sequenceDataClass;
    // factory for creating sequences
    protected transient SequenceFactory<T, O> sequenceFactory;
    // whole sequence storage
    protected transient SequenceStorage<T, O> sequenceStorage;
    //************ Other attributes ************//
    // serial class ID for serialization
    private static final long serialVersionUID = 1l;
    // logger
    protected static final Logger LOGGER = Logger.getLogger("mcdr");

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link AbstractRetrievalAlgorithm}.
     *
     * @param algorithmName algorithm name
     * @param sequenceClass class of sequence
     * @param sequenceDataClass class of the sequence data, usually a static
     * array of a primitive type or {@link java.util.List}
     * @param sequenceStorage storage for managing sequences
     * @throws NoSuchInstantiatorException when an error during creating a
     * sequence factory occurs
     */
    public AbstractRetrievalAlgorithm(String algorithmName, Class<O> sequenceClass, Class<T> sequenceDataClass, SequenceStorage<T, O> sequenceStorage) throws NoSuchInstantiatorException {
        super(algorithmName);
        this.sequenceClass = sequenceClass;
        this.sequenceDataClass = sequenceDataClass;
        this.sequenceFactory = new SequenceFactory<>(sequenceClass, sequenceDataClass);
        this.sequenceStorage = sequenceStorage;
    }

    //************ Abstract methods ************//
    /**
     * Inserts a bulk of motion sequences.
     *
     * @param operation operation to be processed
     * @throws StorageException when an error during inserting sequences into
     * the sequence storage occurs
     */
    public abstract void insert(BulkInsertOperation operation) throws StorageException;

    /**
     * Processes a query operation.
     *
     * @param operation operation to be processed
     * @param answerSimilarityThreshold maximum allowed similarity threshold for
     * the query and result subsequence
     * @param maxAnswerCount maximum number of returned subsequences
     * @param allowedRelativeOverlap maximum relative overlap between two
     * subsequences
     */
    protected abstract void processQuery(RankingSingleQueryOperation operation, float answerSimilarityThreshold, int maxAnswerCount, float allowedRelativeOverlap);

    //************ Methods ************//
    /**
     * Returns all sequences from the sequence storage.
     *
     * @param operation operation to which all the sequences are added
     */
    public void getAllSequences(GetAllObjectsQueryOperation operation) {
        AbstractObjectIterator<LocalAbstractObject> it = sequenceStorage.getAllObjects();
        while (it.hasNext()) {
            operation.addToAnswer(it.next());
        }
        operation.endOperation();
    }

    /**
     * Creates a new sequence instance corresponding to the subsequence of the
     * given sequence.
     *
     * @param sequence sequence from which the subsequence is created
     * @param from initial index of the subsequence element to be copied,
     * inclusive
     * @param to final index of the subsequence element to be copied, exclusive
     * @return a new sequence instance or <code>null</code> in case an error
     * during creating the subsequence has occurred
     */
    public O getSubsequence(O sequence, int from, int to) {
        try {
            return sequenceFactory.create(sequence, from, to, false);
        } catch (InvocationTargetException ex) {
            return null;
        }
    }

    /**
     * Creates a new sequence instance corresponding to the subsequence of the
     * sequence determined by the specified locator.
     *
     * @param sequenceLocator locator determining the sequence from which the
     * subsequence is created
     * @param from initial index of the subsequence element to be copied,
     * inclusive
     * @param to final index of the subsequence element to be copied, exclusive
     * @return a new sequence instance or <code>null</code> in case an error
     * during creating the subsequence has occurred
     */
    public O getSubsequence(String sequenceLocator, int from, int to) {
        O sequence = sequenceStorage.getSequence(sequenceLocator);
        if (sequence == null) {
            return null;
        }
        return getSubsequence(sequence, from, to);
    }

    /**
     * Returns the subsequence from the sequence storage. The subsequence is
     * identified by its parent-sequence locator, offset
     * {@link Sequence#PARAM_SEQ_OFFSET}, and length
     * {@link Sequence#PARAM_SEQ_LENGTH}. If the offset or length is not
     * defined, the entire sequence is returned.
     *
     * @param operation operation to be processed
     * @throws StorageException when an error during searching the sequence
     * storage occurs
     */
    public void getSubsequenceByLocator(GetObjectByLocatorOperation operation) throws StorageException {
        Integer offset = operation.getParameter(Sequence.PARAM_SEQ_OFFSET, Integer.class);
        Integer length = operation.getParameter(Sequence.PARAM_SEQ_LENGTH, Integer.class);
        if (offset == null || length == null) {
            operation.addToAnswer(sequenceStorage.getSequence(operation.getLocator()).getSequence());
        } else {
            operation.addToAnswer(sequenceStorage.getSubsequence(operation.getLocator(), offset, offset + length).getSequence());
        }
        operation.endOperation();
    }

    /**
     * Returns the subsequences from the sequence storage. The subsequences are
     * identified by their parent-sequence locator, offset
     * {@link Sequence#PARAM_SEQ_OFFSET}, and length
     * {@link Sequence#PARAM_SEQ_LENGTH} defined in the {@link #PARAM_SUBSEQS}
     * list. If this list is not defined, the entire sequences specified by
     * their operation locators are returned.
     *
     * @param operation operation to be processed
     * @throws StorageException when an error during searching the sequence
     * storage occurs
     */
    public void getSubsequencesByLocators(GetObjectsByLocatorsOperation operation) throws StorageException {
        List<Map<String, Serializable>> offsetLengthParams = operation.getParameter(PARAM_SUBSEQS, List.class);

        // If the subsequence list is not defined, the entire sequences specified by their locators are returned
        if (offsetLengthParams == null) {
            for (String locator : operation.getLocators()) {
                operation.addToAnswer(sequenceStorage.getSequence(locator).getSequence());
            }
        } else {
            for (Map<String, Serializable> offsetLengthParam : offsetLengthParams) {
                String locator = String.valueOf(offsetLengthParam.get(PARAM_SEQ_LOCATOR));
                Integer offset = (Integer) offsetLengthParam.get(Sequence.PARAM_SEQ_OFFSET);
                Integer length = (Integer) offsetLengthParam.get(Sequence.PARAM_SEQ_LENGTH);
                if (locator == null || offset == null || length == null) {
                    LOGGER.log(Level.SEVERE, "Parameters '" + PARAM_SEQ_LOCATOR + "', '" + Sequence.PARAM_SEQ_OFFSET + "' or '" + Sequence.PARAM_SEQ_LENGTH + "' have not been specified!");
                }
                operation.addToAnswer(sequenceStorage.getSubsequence(locator, offset, offset + length).getSequence());
            }
        }
        operation.endOperation();
    }

    /**
     * Retrieves a batch of randomly chosen subsequences from the sequence
     * storage. The minimal and maximal length of retrieved subsequences depends
     * on the operation parameters {@link Sequence#PARAM_MIN_SEQ_LENGTH} and
     * {@link Sequence#PARAM_MAX_SEQ_LENGTH}. If such parameters are not
     * defined, the entire sequences are returned.
     *
     * @param operation operation to be processed
     */
    public void getRandomSubsequences(GetRandomObjectsQueryOperation operation) {

        // Retrieves random sequences
        GetRandomObjectsQueryOperation randomSequenceOperation = new GetRandomObjectsQueryOperation(operation.getCount(), AnswerType.ORIGINAL_OBJECTS);
        sequenceStorage.processQuery(randomSequenceOperation);

        boolean setParentSeq = operation.getParameter(Sequence.PARAM_SET_PARENT_SEQ, Boolean.class, true);
        Integer minLength = operation.getParameter(Sequence.PARAM_MIN_SEQ_LENGTH, Integer.class);
        Integer maxLength = operation.getParameter(Sequence.PARAM_MAX_SEQ_LENGTH, Integer.class);

        Iterator<AbstractObject> iterator = randomSequenceOperation.getAnswer();
        if (minLength == null || maxLength == null) {

            // Returns the entire sequences (the same sequence can be returned just once)
            Set<String> locators = new HashSet<>();
            while (iterator.hasNext()) {
                AbstractObject o = iterator.next();
                if (locators.add(o.getLocatorURI())) {
                    operation.addToAnswer(o);
                }
            }
        } else {

            // Returns random subsequences
            Random random = new Random();
            while (iterator.hasNext()) {
                O randomSequence = (O) iterator.next();
                int randomSequenceLength = randomSequence.getSequenceLength();
                int randomLength = (randomSequenceLength < minLength) ? randomSequenceLength : Math.min(maxLength, minLength + random.nextInt(randomSequenceLength - minLength + 1));
                int randomOffset = random.nextInt(randomSequenceLength - randomLength + 1);
                try {
                    operation.addToAnswer(sequenceFactory.create(randomSequence, randomOffset, randomOffset + randomLength, setParentSeq).getSequence());
                } catch (InvocationTargetException | IllegalArgumentException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
        operation.endOperation();
    }

    /**
     * Processes {@link KNNQueryOperation}.
     *
     * @param <T> type of operation to be processed
     * @param operation operation to be processed
     */
    public <T extends KNNQueryOperation> void processOperation(T operation) {
        float allowedRelativeOverlap = operation.getParameter(PARAM_ALLOWED_RELATIVE_OVERLAP, Float.class, 0f);
        processQuery(operation, operation.getAnswerThreshold(), operation.getK(), allowedRelativeOverlap);
        operation.endOperation();
    }

    /**
     * Processes {@link RangeQueryOperation}.
     *
     * @param <T> type of operation to be processed
     * @param operation operation to be processed
     */
    public <T extends RangeQueryOperation> void processOperation(T operation) {
        float allowedRelativeOverlap = operation.getParameter(PARAM_ALLOWED_RELATIVE_OVERLAP, Float.class, 0f);
        processQuery(operation, operation.getRadius(), (operation.getAnswerMaximalCapacity() < Integer.MAX_VALUE) ? operation.getAnswerMaximalCapacity() : Integer.MAX_VALUE, allowedRelativeOverlap);
        operation.endOperation();
    }

    //************ Static methods ************//
    /**
     * Adds the map of parameters to the specified operation.
     *
     * @param <E> type of operation
     * @param operation operation to which the map of parameters is added
     * @param params map of parameters
     * @return operation with added parameters
     */
    public static <E extends AbstractOperation> E setOperationParams(E operation, Map<String, Serializable> params) {
        for (Map.Entry<String, Serializable> param : params.entrySet()) {
            operation.setParameter(param.getKey(), param.getValue());
        }
        return operation;
    }
}
