package mcdr.objects.utils;

import java.util.Comparator;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class RankedSortedCollectionDistHashcode extends RankedSortedCollection {

    //************ Attributes ************//
    // comparator of subsequences on the basis of their position (i.e., offset) within a parent sequence
    private static final Comparator<RankedAbstractObject> DIST_AND_HASHCODE_COMPARATOR = new Comparator<RankedAbstractObject>() {
        @Override
        public int compare(RankedAbstractObject o1, RankedAbstractObject o2) {
            int comp = Float.compare(o1.getDistance(), o2.getDistance());
            if (comp != 0) {
                return comp;
            } else {
                return Integer.compare(o1.getObject().hashCode(), o2.getObject().hashCode());
            }
        }
    };

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link RankedSortedCollectionDistHashcode}.
     */
    public RankedSortedCollectionDistHashcode() {
        super(DEFAULT_INITIAL_CAPACITY, UNLIMITED_CAPACITY, DIST_AND_HASHCODE_COMPARATOR);
    }

}
