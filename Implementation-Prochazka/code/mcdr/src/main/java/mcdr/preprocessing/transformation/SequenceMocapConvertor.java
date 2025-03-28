package mcdr.preprocessing.transformation;

import mcdr.sequence.SequenceMocap;
import messif.utility.Convertor;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class SequenceMocapConvertor<O extends SequenceMocap<?>> implements Convertor<O, O> {

    // type of sequence
    private final Class<O> sequenceClass;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link SegmentConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @throws NoSuchInstantiatorException
     */
    public SequenceMocapConvertor(Class<O> sequenceClass) throws NoSuchInstantiatorException {
        this.sequenceClass = sequenceClass;
    }

    //************ Implemented interface Convertor ************//
    @Override
    public Class<O> getDestinationClass() {
        return sequenceClass;
    }
}
