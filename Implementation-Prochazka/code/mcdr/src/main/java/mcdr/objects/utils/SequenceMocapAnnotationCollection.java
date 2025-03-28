package mcdr.objects.utils;

import java.util.Comparator;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapAnnotationCollection extends GroupDependentCollection<RankedAbstractObject> {

    //************ Attributes ************//
    // comparator of subsequences on the basis of their position (i.e., offset) within a parent sequence
    private static final Comparator<RankedAbstractObject> OFFSET_COMPARATOR = new Comparator<RankedAbstractObject>() {
        @Override
        public int compare(RankedAbstractObject o1, RankedAbstractObject o2) {
            return Integer.compare(ObjectMgmt.parseObjectOffset(o1.getObject()), ObjectMgmt.parseObjectOffset(o2.getObject()));
        }
    };

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link SequenceMocapAnnotationCollection}.
     *
     */
    public SequenceMocapAnnotationCollection() {
        super(OFFSET_COMPARATOR, 0f);
    }

    //************ Overrided class GroupDependentCollection ************//
    @Override
    protected String getObjectGroupId(RankedAbstractObject o) {
        return ObjectMgmt.parseObjectParentSequenceId(o.getObject());
    }

    @Override
    protected String getObjectLocationId(RankedAbstractObject o) {
        return ObjectMgmt.parseObjectOffset(o.getObject()) + "-" + ObjectMgmt.parseObjectLength(o.getObject());
    }

    @Override
    protected ObjectEnvelope getObjectEnvelope(AbstractObject o) {
        int offset = ObjectMgmt.parseObjectOffset(o);
        return new ObjectEnvelope(offset, offset + ObjectMgmt.parseObjectLength(o) - 1 + 1);
    }
}
