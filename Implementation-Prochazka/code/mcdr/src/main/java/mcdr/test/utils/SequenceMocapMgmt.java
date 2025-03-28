package mcdr.test.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mcdr.sequence.SequenceMocap;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.utility.Convertor;
import messif.utility.reflection.NoSuchInstantiatorException;
import smf.sequences.SequenceFactory;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapMgmt {

    // map of loaded sequences in the form of <locator, sequence>
    private final Map<String, SequenceMocap<?>> sequences = new HashMap<>();

    //************ Methods ************//
    /**
     * Returns the collection of loaded sequences.
     *
     * @return the collection of loaded sequences
     */
    public Collection<SequenceMocap<?>> getSequences() {
        return sequences.values();
    }

    /**
     * Returns the sequence specified by its locator.
     *
     * @param sequenceLocator locator of the sequence to be returned
     * @return the sequence specified by its locator
     */
    public SequenceMocap<?> getSequence(String sequenceLocator) {
        return sequences.get(sequenceLocator);
    }

    /**
     * Creates a new sequence instance corresponding to the subsequence of the
     * sequence determined by the specified locator.
     *
     * @param <O> type of sequence
     * @param sequenceClass class of sequence
     * @param sequenceLocator locator determining the sequence from which the
     * subsequence is created
     * @param from initial index of the subsequence element to be copied,
     * inclusive
     * @param to final index of the subsequence element to be copied, exclusive
     * @return a new sequence instance or <code>null</code> in case an error
     * during creating the subsequence has occurred
     */
    public <O extends SequenceMocap<?>> SequenceMocap<?> getSubsequence(Class<O> sequenceClass, String sequenceLocator, int from, int to) {
        SequenceMocap<?> sequence = getSequence(sequenceLocator);
        if (sequence == null) {
            return null;
        }
        O subsequence;
        try {
            SequenceFactory<List<?>, O> sequenceFactory = new SequenceFactory<>(sequenceClass, (Class) List.class);
            subsequence = sequenceFactory.create(sequence, from, to, false);
        } catch (NoSuchInstantiatorException | InvocationTargetException ex) {
            return null;
        }
        subsequence.setObjectKey(new AbstractObjectKey(sequenceLocator + "_xxx_" + from + "_" + (to - from)));
        return subsequence;
    }

    /**
     * Loads sequences from a file and appends them to the sequence list.
     *
     * @param sequenceClass class of sequences to be loaded
     * @param sequenceFile file from which sequences are loaded
     * @throws IOException
     */
    public void read(Class<? extends SequenceMocap<?>> sequenceClass, String sequenceFile) throws IOException {
        read(sequenceClass, sequenceFile, null);
    }

    /**
     * Loads sequences from a file and appends them to the sequence list.
     *
     * @param sequenceClass class of sequences to be loaded
     * @param sequenceFile file from which sequences are loaded
     * @param sequenceLocatorRegEx regular expression to which sequence locators
     * are matched
     * @throws IOException
     */
    public void read(Class<? extends SequenceMocap<?>> sequenceClass, String sequenceFile, String sequenceLocatorRegEx) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.print("  reading objects...");

        // Reads sequence objects
        StreamGenericAbstractObjectIterator sequenceIterator = new StreamGenericAbstractObjectIterator<>(sequenceClass, sequenceFile);
        int totalSequenceCount = 0;
        int totalSequenceLength = 0;
        while (sequenceIterator.hasNext()) {
            SequenceMocap<?> sequence = (SequenceMocap<?>) sequenceIterator.next();
            if (sequenceLocatorRegEx == null || sequence.getLocatorURI().matches(sequenceLocatorRegEx)) {
                SequenceMocap<?> sameSequence = sequences.put(sequence.getLocatorURI(), sequence);
                if (sameSequence != null) {
                    System.out.println(" !! The sequence with locator " + sameSequence.getLocatorURI() + " has been replaced by the sequence with the same locator.");
                }
                totalSequenceCount++;
                totalSequenceLength += sequence.getSequenceLength();
            }
        }

        System.out.println(" " + (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("  sequence count: " + totalSequenceCount + "; average length: " + ((float) totalSequenceLength / totalSequenceCount));
    }

    /**
     * Stores sequences whose locators match a given regular expression, to a
     * file.
     *
     * @param sequenceFile file from which sequences are loaded
     * @throws IOException
     */
    public void store(String sequenceFile) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.print("  storing objects...");

        // Reads sequence objects
        FileOutputStream fos = new FileOutputStream(sequenceFile);
        for (SequenceMocap<?> sequence : getSequences()) {
            sequence.write(fos);
        }
        fos.close();

        System.out.println(" " + (System.currentTimeMillis() - startTime) + " ms");
    }

    /**
     * Transforms all the loaded sequences in the past using by the specified
     * convertor.
     *
     * @param convertor convertor applied to the loaded sequences
     * @throws Exception
     */
    public void convert(Convertor<SequenceMocap<?>, SequenceMocap<?>> convertor) throws Exception {
        List<SequenceMocap<?>> transformedSequences = new ArrayList<>(sequences.size());
        for (SequenceMocap<?> sequence : sequences.values()) {
            transformedSequences.add(convertor.convert(sequence));
        }
        sequences.clear();
        for (SequenceMocap<?> sequence : transformedSequences) {
            sequences.put(sequence.getLocatorURI(), sequence);
        }
    }

    /**
     * Returns either the minimum, or maximum coordinate xyz value of any joint.
     * The minimum is returned when the parameter is set to true, otherwise the
     * maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @return the minimum/maximum coordinate xyz value of any joint
     */
    public float getExtremalCoordValue(boolean minValue) {
        float extremalValue = (minValue) ? Float.MAX_VALUE : Float.MIN_VALUE;
        for (SequenceMocap<?> sequence : sequences.values()) {
            for (int i = 0; i < sequence.getObjectCount(); i++) {
                float coordValue = sequence.getExtremalCoordValue(minValue);
                if ((minValue) ? coordValue < extremalValue : coordValue > extremalValue) {
                    extremalValue = coordValue;
                }
            }
        }
        return extremalValue;
    }

    /**
     * Returns either the minimum, or maximum coordinate xyz value of the
     * specified joint. The minimum is returned when the parameter is set to
     * true, otherwise the maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @param jointIdx index of the joint whose the minimum/maximum coordinate
     * xyz value is determined
     * @return the minimum/maximum coordinate xyz value of the specified joint
     */
    public float getExtremalJointCoordValue(boolean minValue, int jointIdx) {
        float extremalValue = (minValue) ? Float.MAX_VALUE : Float.MIN_VALUE;
        for (SequenceMocap<?> sequence : sequences.values()) {
            float coordValue = sequence.getExtremalJointCoordValue(minValue, jointIdx);
            if ((minValue) ? coordValue < extremalValue : coordValue > extremalValue) {
                extremalValue = coordValue;
            }
        }
        return extremalValue;
    }

    /**
     * Returns either the minimum, or maximum coordinate value of the specified
     * axis x/y/z. The minimum is returned when the parameter is set to true,
     * otherwise the maximum is returned.
     *
     * @param minValue decides whether the minimum or maximum coordinate value
     * is returned
     * @param axisIdx index of the x/y/z axis whose the minimum/maximum
     * coordinate value is determined
     * @return the minimum/maximum coordinate value of the specified axis
     */
    public float getExtremalAxisCoordValue(boolean minValue, int axisIdx) {
        float extremalValue = (minValue) ? Float.MAX_VALUE : Float.MIN_VALUE;
        for (SequenceMocap<?> sequence : sequences.values()) {
            float coordValue = sequence.getExtremalAxisCoordValue(minValue, axisIdx);
            if ((minValue) ? coordValue < extremalValue : coordValue > extremalValue) {
                extremalValue = coordValue;
            }
        }
        return extremalValue;
    }
}
