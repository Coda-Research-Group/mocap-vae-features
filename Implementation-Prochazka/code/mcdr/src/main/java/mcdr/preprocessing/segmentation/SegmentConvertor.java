package mcdr.preprocessing.segmentation;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import mcdr.sequence.SequenceMocap;
import messif.utility.Convertor;
import messif.utility.reflection.NoSuchInstantiatorException;
import smf.sequences.SequenceFactory;

/**
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class SegmentConvertor<O extends SequenceMocap<?>> implements Convertor<O, List<O>> {

    // factory for creating sequences
    protected final SequenceFactory<List<?>, O> sequenceFactory;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link SegmentConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @throws NoSuchInstantiatorException
     */
    public SegmentConvertor(Class<O> sequenceClass) throws NoSuchInstantiatorException {
        this.sequenceFactory = new SequenceFactory<>(sequenceClass, (Class) List.class);
    }

    //************ Methods ************//
    /**
     * Creates a new sequence instance corresponding to the subsequence of the
     * given sequence.
     *
     * @param sequence sequence from which the subsequence is created
     * @param fromIndex initial index of the subsequence element to be copied,
     * inclusive
     * @param toIndex final index of the subsequence element to be copied,
     * exclusive
     * @return a new sequence instance or <code>null</code> in case an error
     * during creating the subsequence occurs
     */
    public O getSubsequence(O sequence, int fromIndex, int toIndex) {
        try {
            return sequenceFactory.create(sequence, fromIndex, toIndex, false);
        } catch (InvocationTargetException ex) {
            return null;
        }
    }

    //************ Implemented interface Convertor ************//
    /**
     * Splits the sequence into segments.
     *
     * @param sequence sequence to be split
     * @return list of segments
     */
    @Override
    public abstract List<O> convert(O sequence);

    @Override
    public Class<? extends List<O>> getDestinationClass() {
        return (Class) List.class;
    }
}
