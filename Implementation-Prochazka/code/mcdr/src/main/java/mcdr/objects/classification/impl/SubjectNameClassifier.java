package mcdr.objects.classification.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SubjectNameClassifier extends SequenceClassifier {

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link SubjectNameClassifier}.
     *
     * @param sequenceToSubjectIdList list of maps of sequence id with the
     * corresponding subject id
     */
    public SubjectNameClassifier(List<Map<String, String>> sequenceToSubjectIdList) {
        super(new HashMap<String,String>(), false);

        // Creates the map that associates the sequence id with the subject id
        for (Map<String, String> sequenceToSubjectId : sequenceToSubjectIdList) {
            List<String> categories = new ArrayList<>();
            categories.add(sequenceToSubjectId.get("subject_id"));
            objectToCategoriesMap.put(sequenceToSubjectId.get("id"), categories);
        }
    }
}
