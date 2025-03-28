package mcdr.objects.classification.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import mcdr.sequence.SequenceMocap;
import messif.objects.AbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceClassifier extends ObjectClassifier {

    //************ Attributes ************//
    // determines whether the sequence id is set according to either locatorURI, or getSequenceId method
    protected final boolean sequenceIdAsLocatorURI;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link SequenceClassifier}.
     *
     * @param sequenceToCategoryMap map associating the sequence id with the
     * category
     * @param sequenceIdAsLocatorURI determines whether the sequence id is set
     * according to either locatorURI, or getSequenceId method
     */
    public SequenceClassifier(Map<String, String> sequenceToCategoryMap, boolean sequenceIdAsLocatorURI) {
        super(sequenceToCategoryMap);
        this.sequenceIdAsLocatorURI = sequenceIdAsLocatorURI;
    }

    /**
     * Creates a new instance of {@link SequenceClassifier}.
     *
     * @param sequenceToCategoryMapFile file which contains the CSV-like (i.e.,
     * key;value) map associating the sequence id with the category
     * @param sequenceIdAsLocatorURI determines whether the sequence id is set
     * @throws java.io.FileNotFoundException
     */
    public SequenceClassifier(File sequenceToCategoryMapFile, boolean sequenceIdAsLocatorURI) throws FileNotFoundException, IOException {
        super(sequenceToCategoryMapFile);
        this.sequenceIdAsLocatorURI = sequenceIdAsLocatorURI;
    }

    //************ Implemented class ObjectClassifier ************//
    @Override
    public String getObjectId(AbstractObject o) {
        if (!(o instanceof SequenceMocap<?>)) {
            return null;
        }
        return sequenceIdAsLocatorURI ? o.getLocatorURI() : ((SequenceMocap<?>) o).getSequenceId();
    }
}
