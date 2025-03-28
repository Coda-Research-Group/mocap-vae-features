package mcdr.preprocessing.transformation.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import mcdr.objects.ObjectMocapPose;
import mcdr.sequence.SequenceMocap;
import messif.utility.reflection.NoSuchInstantiatorException;
import mcdr.preprocessing.transformation.SequenceMocapConvertor;
import messif.objects.keys.AbstractObjectKey;
import smf.sequences.SequenceFactory;

/**
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class FPSConvertor<O extends SequenceMocap<?>> extends SequenceMocapConvertor<O> {

    // factory for creating sequences
    protected final SequenceFactory<List<?>, O> sequenceFactory;
    // original fps rate
    private final int originalFPSRate;
    // reduced fps rate
    private final int reducedFPSRate;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link FPSConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @param originalFPSRate original fps rate
     * @param reducedFPSRate reduced fps rate
     * @throws NoSuchInstantiatorException
     */
    public FPSConvertor(Class<O> sequenceClass, int originalFPSRate, int reducedFPSRate) throws NoSuchInstantiatorException {
        super(sequenceClass);
        this.sequenceFactory = new SequenceFactory<>(sequenceClass, (Class) List.class);
        this.originalFPSRate = originalFPSRate;
        this.reducedFPSRate = reducedFPSRate;
    }

    //************ Implemented interface Convertor ************//
    /**
     * Reduce the frame-per-second rate by ignoring frames regularly.
     *
     * @param sequence sequence whose quality is reduced
     * @return modified sequence with a reduced fps rate
     */
    @Override
    public O convert(O sequence) {
        if (originalFPSRate > reducedFPSRate && reducedFPSRate > 0) {
            List<ObjectMocapPose> poses = new ArrayList<>();
            int index = 0;
            while (index < sequence.getSequenceLength()) {
                poses.add(sequence.getObject(index));
                index += originalFPSRate / reducedFPSRate;
            }
            try {
                O convertedSequence = sequenceFactory.create(poses, sequence, 0, false);
                convertedSequence.setObjectKey(new AbstractObjectKey(sequence.getLocatorURI()));
                return convertedSequence;
            } catch (InvocationTargetException ex) {
                return null;
            }
        }
        return sequence;
    }
}
