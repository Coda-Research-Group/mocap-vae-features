package mcdr.objects.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class OverlappingAnnotationRemovalCollection extends RankedSortedCollection implements InstantiableCollection {

    // indicates whether objects overlapping with the query object should be removed
    protected final boolean checksOverlapWithQueryObject;
    // indicates whether overlapping objects in the query answer should be removed
    protected final boolean checksOverlapWithQueryAnswer;
    // query object parent sequence id
    protected final String qParentSequenceId;
    // query object start index
    protected final int qStartIndex;
    // query object end index
    protected final int qEndIndex;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link OverlappingAnnotationRemovalCollection}.
     *
     */
    public OverlappingAnnotationRemovalCollection() {
        this.checksOverlapWithQueryObject = false;
        this.checksOverlapWithQueryAnswer = true;
        this.qParentSequenceId = null;
        this.qStartIndex = -1;
        this.qEndIndex = -1;
    }

    /**
     * Creates a new instance of {@link OverlappingAnnotationRemovalCollection}.
     *
     * @param q query object
     * @param checksOverlapWithQueryAnswer indicates whether overlapping objects
     * in the query answer should be removed
     */
    public OverlappingAnnotationRemovalCollection(AbstractObject q, boolean checksOverlapWithQueryAnswer) {
        this.checksOverlapWithQueryObject = true;
        this.checksOverlapWithQueryAnswer = checksOverlapWithQueryAnswer;
        this.qParentSequenceId = ObjectMgmt.parseObjectParentSequenceId(q);
        this.qStartIndex = ObjectMgmt.parseObjectOffset(q);
        this.qEndIndex = this.qStartIndex + ObjectMgmt.parseObjectLength(q) - 1;
    }

    //************ Overrided class RankedSortedCollection ************//
    /**
     * Adds the object in case it either is not overlapping with another object,
     * or has a smaller distance than all overlapping objects that are removed
     * from the answer.
     *
     * @param e object to be added
     * @return true if the object is added
     */
    @Override
    public boolean add(RankedAbstractObject e) {
        Set<String> overlappingObjectLocators = new HashSet<>();
        String eParentSequenceId = ObjectMgmt.parseObjectParentSequenceId(e.getObject());
        final int eStartIndex = ObjectMgmt.parseObjectOffset(e.getObject());
        final int eEndIndex = eStartIndex + ObjectMgmt.parseObjectLength(e.getObject()) - 1;

        // Checks overlaps with the query object
        if (checksOverlapWithQueryObject) {
            if (qParentSequenceId.equals(eParentSequenceId) && Math.max(qStartIndex, eStartIndex) <= Math.min(qEndIndex, eEndIndex)) {
                return false;
            }
        }

        // Checks overlaps with the query answer
        if (checksOverlapWithQueryAnswer) {
            Iterator<RankedAbstractObject> objIt = iterator();
            while (objIt.hasNext()) {
                RankedAbstractObject rao = objIt.next();
                AbstractObject o = rao.getObject();

                String oParentSequenceId = ObjectMgmt.parseObjectParentSequenceId(o);
                int oStartIndex = ObjectMgmt.parseObjectOffset(o);
                int oEndIndex = oStartIndex + ObjectMgmt.parseObjectLength(o) - 1;

                if ((eParentSequenceId.equals(oParentSequenceId)) && (Math.max(eStartIndex, oStartIndex) <= Math.min(eEndIndex, oEndIndex))) {
                    if (rao.getDistance() <= e.getDistance()) {
                        return false;
                    } else {
                        overlappingObjectLocators.add(o.getLocatorURI());
                    }
                }
            }

            // Removes the overlapping objects
            if (!overlappingObjectLocators.isEmpty()) {
                objIt = iterator();
                while (objIt.hasNext()) {
                    RankedAbstractObject rao = objIt.next();
                    if (overlappingObjectLocators.contains(rao.getObject().getLocatorURI())) {
                        objIt.remove();
                    }
                }
            }
        }
        return super.add(e);
    }

    //************ Overrided class RankedSortedCollection ************//
    @Override
    public OverlappingAnnotationRemovalCollection instantiate(AbstractObject queryObject) {
        return new OverlappingAnnotationRemovalCollection(queryObject, this.checksOverlapWithQueryAnswer);
    }

}
