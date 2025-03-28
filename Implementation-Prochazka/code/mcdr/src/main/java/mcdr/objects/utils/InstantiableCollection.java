package mcdr.objects.utils;

import messif.objects.AbstractObject;
import messif.objects.util.RankedSortedCollection;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public interface InstantiableCollection {

    /**
     * Creates a new ranked sorted collection for the specific query object.
     *
     * @param <E> type of ranked sorted collection
     * @param queryObject query object
     * @return a new ranked sorted collection for the specific query object
     */
    public <E extends RankedSortedCollection> E instantiate(AbstractObject queryObject);
}
