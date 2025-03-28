package mcdr.objects.utils;

import java.util.Comparator;
import mcdr.sequence.SequenceMocap;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapCollection extends GroupDependentCollection<RankedAbstractObject> {

    //************ Attributes ************//
    // comparator of subsequences on the basis of their position (i.e., offset) within a parent sequence
    private static final Comparator<RankedAbstractObject> OFFSET_COMPARATOR = new Comparator<RankedAbstractObject>() {
        @Override
        public int compare(RankedAbstractObject o1, RankedAbstractObject o2) {
            return Integer.compare(((SequenceMocap) o1.getObject()).getOffset(), ((SequenceMocap) o2.getObject()).getOffset());
        }
    };

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link SequenceMocapCollection}.
     *
     * @param allowedRelativeOverlap relative admissible overlap between two
     * subsequences
     */
    public SequenceMocapCollection(float allowedRelativeOverlap) {
        super(OFFSET_COMPARATOR, allowedRelativeOverlap);
    }

    //************ Overrided class GroupDependentCollection ************//
    @Override
    protected String getObjectGroupId(RankedAbstractObject o) {
        return ((SequenceMocap) o.getObject()).getOriginalSequenceLocator();
    }

    @Override
    protected String getObjectLocationId(RankedAbstractObject o) {
        SequenceMocap obj = (SequenceMocap) o.getObject();
        return obj.getOffset() + "-" + obj.getSequenceLength();
    }

    @Override
    protected ObjectEnvelope getObjectEnvelope(AbstractObject o) {
        SequenceMocap oSeq = (SequenceMocap) o;
        return new ObjectEnvelope(oSeq.getOffset(), oSeq.getOffset() + oSeq.getSequenceLength() - 1);
    }
}
