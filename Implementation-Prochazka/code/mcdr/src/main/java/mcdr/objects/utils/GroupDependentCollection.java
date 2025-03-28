package mcdr.objects.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.operations.RankingQueryOperation;
import messif.utility.SortedCollection;

/**
 * Divides objects (e.g., poses or subsequences) to groups (e.g., sequences)
 * according to their group id and filters overlapping objects within each group
 * separately in order to retain more relevant ones.
 *
 * @param <T> type of object that extends the ranked object
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class GroupDependentCollection<T extends RankedAbstractObject> extends RankedSortedCollection {

    //************ Attributes ************//
    // map keeping association between the group id (e.g., object locator) and map of objects associating the object location id (e.g., pose frame number) with the object alone
    private final Map<String, Map<String, T>> groupMap;
    // comparator that determines sequential order of objects within a single group
    protected final Comparator<T> objectOrderComparator;
    // relative admissible overlap between two envelopes
    protected final float allowedRelativeOverlap;
    // statistics of number of all objects that have been added so far
    private int objectCountWithDuplicates = 0;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link GroupDependentCollection}.
     *
     * @param objectOrderComparator comparator that determines sequential order
     * of objects within a single group
     */
    public GroupDependentCollection(Comparator<T> objectOrderComparator) {
        this(objectOrderComparator, 0f);
    }

    /**
     * Creates a new instance of {@link GroupDependentCollection}.
     *
     * @param objectOrderComparator comparator that determines sequential order
     * of objects within a single group
     * @param allowedRelativeOverlap relative admissible overlap between two
     * envelopes
     */
    public GroupDependentCollection(Comparator<T> objectOrderComparator, float allowedRelativeOverlap) {
        super();
        this.groupMap = new HashMap<>();
        this.objectOrderComparator = objectOrderComparator;
        this.allowedRelativeOverlap = allowedRelativeOverlap;
    }

    //************ Abstract methods ************//
    /**
     * Returns the object group id.
     *
     * @param o object from which the group id is extracted
     * @return the object group id
     */
    protected abstract String getObjectGroupId(T o);

    /**
     * Returns the object location id (e.g., pose frame number) within a single
     * group.
     *
     * @param o object from which the location id is extracted
     * @return the object location id (e.g., pose frame number) within a single
     * group
     */
    protected abstract String getObjectLocationId(T o);

    /**
     * Creates a bounding envelope of the object.
     *
     * @param o object from which the envelope is created
     * @return envelope of the object
     */
    protected abstract ObjectEnvelope getObjectEnvelope(AbstractObject o);

    //************ Methods ************//
    /**
     * Creates a bounding envelope of two existing overlapping envelopes. Two
     * envelopes are considered as overlapping if their relative overlap is
     * higher than an admissible threshold <tt>allowedRelativeOverlap</tt>. For
     * example, if allowedRelativeOverlap=0.3, the envelopes are not overlapping
     * when they have overlap of at most 30% with respect to the second envelope
     * (oe2) size.
     *
     * @param oe1 first object envelope
     * @param oe2 second object envelope
     * @return <tt>null</tt> if the envelopes do not overlap
     */
    protected ObjectEnvelope getObjectEnvelope(ObjectEnvelope oe1, ObjectEnvelope oe2) {

        // Checks whether the envelopes do not overlap
        if (Math.max(oe1.startIndex, oe2.startIndex) > Math.min(oe1.endIndex, oe2.endIndex)) {
            return null;
        }

        // Checks the allowed relative overlap
        int absoluteOverlap = Math.min(oe1.endIndex, oe2.endIndex) - Math.max(oe1.startIndex, oe2.startIndex) + 1;
        float relativeOverlap = absoluteOverlap / (float) (oe2.endIndex - oe2.startIndex + 1);
        if (relativeOverlap <= allowedRelativeOverlap) {
            return null;
        }
        return new ObjectEnvelope(Math.min(oe1.startIndex, oe2.startIndex), Math.max(oe1.endIndex, oe2.endIndex));
    }

    /**
     * Returns the number of groups to which objects are assigned.
     *
     * @return the number of groups
     */
    public int getGroupCount() {
        return groupMap.size();
    }

    /**
     * Returns the number of all objects (with different location id within a
     * single group) that have been added so far.
     *
     * @return the number of all objects (with different location id within a
     * single group) that have been added so far
     */
    public int getObjectCount() {
        int rtv = 0;
        for (Map<String, T> groupObjects : groupMap.values()) {
            rtv += groupObjects.size();
        }
        return rtv;
    }

    /**
     * Returns the number of all objects that have been added so far.
     *
     * @return the number of all objects that have been added so far
     */
    public int getObjectCountWithDuplicates() {
        return objectCountWithDuplicates;
    }

    /**
     * Returns the map associating the group id with the objects belonging to
     * this group.
     *
     * @return the map associating the group id with the objects belonging to
     * this group
     */
    public Map<String, Collection<T>> getGroupObjects() {
        Map<String, Collection<T>> rtv = new HashMap<>(groupMap.size());
        for (Entry<String, Map<String, T>> groupObjectsEntry : groupMap.entrySet()) {
            rtv.put(groupObjectsEntry.getKey(), groupObjectsEntry.getValue().values());
        }
        return rtv;
    }

    /**
     * Merges overlapping objects within each group into envelopes.
     *
     * @return groups of merged envelopes
     */
    public Map<String, Collection<ObjectEnvelope>> mergeOverlappingObjects() {
        Map<String, Collection<ObjectEnvelope>> groupEnvelopesMap = new HashMap<>(groupMap.size());

        // Each group is examined separately
        for (Map.Entry<String, Map<String, T>> group : groupMap.entrySet()) {
            Collection<ObjectEnvelope> groupEnvelopes = new LinkedList<>();

            // Sorts all group objects
            SortedCollection<T> objects = new SortedCollection<>(group.getValue().size(), objectOrderComparator);
            objects.addAll(group.getValue().values());

            // List of non-overlapping group objects
            Iterator<T> iterator = objects.iterator();
            ObjectEnvelope lastObjectEnvelope = null;
            while (iterator.hasNext()) {
                T co = iterator.next(); // current examined object
                ObjectEnvelope coe = getObjectEnvelope(co.getObject()); // current examined object envelope

                if (lastObjectEnvelope == null) {
                    lastObjectEnvelope = coe;
                } else {
                    ObjectEnvelope oe = getObjectEnvelope(lastObjectEnvelope, coe);

                    // Decides whether last two consecutive envelopes do not overlap
                    if (oe == null) {
                        groupEnvelopes.add(lastObjectEnvelope);
                        lastObjectEnvelope = coe;
                    } else {
                        lastObjectEnvelope = oe;
                    }
                }
            }
            if (lastObjectEnvelope != null) {
                groupEnvelopes.add(lastObjectEnvelope);
            }

            // Overlapping objects within the examined group are merged into a single envelope
            groupEnvelopesMap.put(group.getKey(), groupEnvelopes);
        }
        return groupEnvelopesMap;
    }

    /**
     * Filters overlapping objects within each group separately and retains
     * those with the smallest distance.
     *
     * @return groups of non-overlapping ranked objects
     */
    public Map<String, Collection<T>> filterOverlappingObjects() {
        Map<String, Collection<T>> rtv = new HashMap<>(groupMap.size());

        // Each group is examined separately
        for (Map.Entry<String, Map<String, T>> group : groupMap.entrySet()) {

            // Sorts all group objects
            SortedCollection<T> objects = new SortedCollection<>(group.getValue().size(), objectOrderComparator);
            objects.addAll(group.getValue().values());

            // List of non-overlapping group objects
            Collection<T> retainedObjects = new LinkedList<>();

            Iterator<T> iterator = objects.iterator();
            if (iterator.hasNext()) {
                T po; // previous examined object
                T co = iterator.next(); // current examined object
                ObjectEnvelope poe; // previous examined object envelope
                ObjectEnvelope coe = getObjectEnvelope(co.getObject()); // current examined object envelope

                // If only a single object exists, it is retained
                if (!iterator.hasNext()) {
                    retainedObjects.add(co);
                }

                while (iterator.hasNext()) {
                    po = co;
                    co = iterator.next();
                    poe = coe;
                    coe = getObjectEnvelope(co.getObject());

                    // Skips the consecutive non-overlapping objects
                    ObjectEnvelope oe;
                    while ((oe = getObjectEnvelope(coe, poe)) == null && iterator.hasNext()) {
                        retainedObjects.add(po);
                        po = co;
                        co = iterator.next();
                        poe = coe;
                        coe = getObjectEnvelope(co.getObject());
                    }

                    // Decides whether last two consecutive objects do not overlap
                    if (oe == null) {
                        retainedObjects.add(po);
                        retainedObjects.add(co);
                    } else {
                        Queue<T> overlappingObjects = new SortedCollection<>();
                        overlappingObjects.add(po);

                        // Adds next overlapping objects
                        while ((oe = getObjectEnvelope(oe, coe)) != null && iterator.hasNext()) {
                            overlappingObjects.add(co);
                            co = iterator.next();
                            coe = getObjectEnvelope(co.getObject());
                        }
                        if (oe != null) {
                            overlappingObjects.add(co);
                        } else if (!iterator.hasNext()) {
                            retainedObjects.add(co);
                        }

                        // Identifies less-ranked overlapping objects
                        List<T> locallyRetainedObjects = new LinkedList<>();
                        Iterator<T> overlappingObjectIterator = overlappingObjects.iterator();
                        while (overlappingObjectIterator.hasNext()) {
                            T overlappingObject = overlappingObjectIterator.next();
                            boolean filterObject = false;
                            for (T nonFilteredObject : locallyRetainedObjects) {
                                if (getObjectEnvelope(getObjectEnvelope(overlappingObject.getObject()), getObjectEnvelope(nonFilteredObject.getObject())) != null) {
                                    filterObject = true;
                                    break;
                                }
                            }
                            if (!filterObject) {
                                locallyRetainedObjects.add(overlappingObject);
                            }
                        }
                        retainedObjects.addAll(locallyRetainedObjects);
                    }
                }
            }

            // Retains only non-overlapping objects within the examined group
            rtv.put(group.getKey(), retainedObjects);
        }
        return rtv;
    }

    /**
     * Filters overlapping objects within each group separately. If the group
     * contains more objects than the <code>maxObjectCount</code> parameter, the
     * remaining less relevant objects (i.e., the objects with a higher
     * distance) are removed.
     *
     * @param maxObjectCount maximum number of objects within each group
     * @return groups of non-overlapping ranked objects
     */
    public Map<String, Collection<T>> filterOverlappingObjects(int maxObjectCount) {

        // Filters overlapping objects
        Map<String, Collection<T>> rtv = filterOverlappingObjects();

        for (Map.Entry<String, Collection<T>> group : rtv.entrySet()) {
            int groupSize = group.getValue().size();

            // Removes remaining less-relevant objects
            if (groupSize > maxObjectCount) {
                Collection<T> retainedObjects = new LinkedList<>();
                SortedCollection<T> objectsSortedByDistance = new SortedCollection<>(groupSize);
                objectsSortedByDistance.addAll(group.getValue());
                Iterator<T> iterator = objectsSortedByDistance.iterator(0, maxObjectCount);
                while (iterator.hasNext()) {
                    retainedObjects.add(iterator.next());
                }
                rtv.put(group.getKey(), retainedObjects);
            }
        }
        return rtv;
    }

    /**
     * Filters overlapping objects within each group separately. Then it merges
     * all non-overlapping objects and adds them to the query operation answer.
     *
     * @param operation query operation to which the non-overlapping objects are
     * added
     */
    public void mergeAndfilterOverlappingObjects(RankingQueryOperation operation) {

        // Filters overlapping objects
        Map<String, Collection<T>> rtv = filterOverlappingObjects();

        // Adds non-overlapping objects to the query answer
        for (Collection<T> groupObjectMap : rtv.values()) {
            for (T ro : groupObjectMap) {
                operation.addToAnswer(ro.getObject(), ro.getDistance(), null);
            }
        }
    }

    //************ Overrided class RankedSortedCollection ************//
    /**
     * Adds the object to the specific group according to the object group id.
     * If the object with the same location id already exists in the specific
     * group, the more-relevant object (i.e., the object with a smaller
     * distance) is retained.
     *
     * @param e object to be added
     * @return false object is never added to the answer
     */
    @Override
    public boolean add(RankedAbstractObject e) {
        T obj = (T) e;

        // Determines the object group
        Map<String, T> groupObjects;
        String objectGroupId = getObjectGroupId(obj);
        groupObjects = groupMap.get(objectGroupId);
        if (groupObjects == null) {
            groupObjects = new HashMap<>();
            groupMap.put(objectGroupId, groupObjects);
        }

        // Adds the object to the group if there is no more-relevant object with the same location id
        String objLocationId = getObjectLocationId(obj);
        T previousObject = groupObjects.get(objLocationId);
        if (previousObject == null || previousObject.getDistance() > obj.getDistance()) {
            groupObjects.put(objLocationId, obj);
        }

        objectCountWithDuplicates++;
        return false;
    }

    //************ Classes ************//
    /**
     * Static class defining a bounding envelope of the object by its start and
     * end position.
     */
    public static final class ObjectEnvelope {

        //************ Attributes ************//
        // start index of this envelope
        public final int startIndex;
        // end index of this envelope
        public final int endIndex;

        //************ Constructors ************//
        /**
         * Creates a new instance of {@link ObjectEnvelope}.
         *
         * @param startIndex start index of this envelope
         * @param endIndex end index of this envelope
         */
        ObjectEnvelope(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        //************ Overrided class Object ************//
        @Override
        public String toString() {
            return "[" + startIndex + ", " + endIndex + "], size: " + (endIndex - startIndex + 1);
        }
    }
}
