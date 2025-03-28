package mcdr.objects.utils;

import java.util.Comparator;
import mcdr.objects.ObjectMocapPose;
import mcdr.objects.utils.ObjectMocapPoseRootCollection.RootRankedAbstractObject;
import messif.objects.AbstractObject;
import messif.objects.util.RankedAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMocapPoseRootCollection extends GroupDependentCollection<RootRankedAbstractObject> {

    //************ Attributes ************//
    // root number which the new poses are associated with
    private int rootNo = -1;
    // two poses distant up to this threshold (in frames) are considered as overlapping
    private final int frameOverlappingThreshold;
    // comparator of poses on the basis of their position (i.e., frame number) within a sequence
    private static final Comparator<RootRankedAbstractObject> FRAME_COMPARATOR = new Comparator<RootRankedAbstractObject>() {
        @Override
        public int compare(RootRankedAbstractObject o1, RootRankedAbstractObject o2) {
            return Integer.compare(((ObjectMocapPose) o1.getObject()).getFrameNo(), ((ObjectMocapPose) o2.getObject()).getFrameNo());
        }
    };

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseRootCollection}.
     *
     * @param frameOverlappingThreshold threshold determining the overlap of two
     * poses
     */
    public ObjectMocapPoseRootCollection(int frameOverlappingThreshold) {
        super(FRAME_COMPARATOR);
        this.frameOverlappingThreshold = frameOverlappingThreshold;
    }

    //************ Methods ************//
    /**
     * Sets the root number.
     *
     * @param rootNo root number
     */
    public void setRootNo(int rootNo) {
        this.rootNo = rootNo;
    }

    //************ Overrided class GroupDependentCollection ************//
    @Override
    protected String getObjectGroupId(RootRankedAbstractObject o) {
        return o.getObject().getLocatorURI();
    }

    @Override
    protected String getObjectLocationId(RootRankedAbstractObject o) {
        return Integer.toString(((ObjectMocapPose) o.getObject()).getFrameNo());
    }

    @Override
    protected ObjectEnvelope getObjectEnvelope(AbstractObject o) {
        int oFrame = ((ObjectMocapPose) o).getFrameNo();
        return new ObjectEnvelope(oFrame, oFrame + frameOverlappingThreshold - 1);
    }

    @Override
    public boolean add(RankedAbstractObject e) {
        return super.add(new RootRankedAbstractObject(e, rootNo));
    }

    //************ Classes ************//
    /**
     * Static class encapsulating a ranked object and root number.
     */
    public static final class RootRankedAbstractObject extends RankedAbstractObject {

        //************ Attributes ************//
        // root number
        private final int rootNo;

        //************ Constructors ************//
        /**
         * Creates a new instance of {@link RootRankedAbstractObject}.
         *
         * @param ro ranked object
         * @param rootNo root number
         */
        RootRankedAbstractObject(RankedAbstractObject ro, int rootNo) {
            super(ro.getObject(), ro.getDistance());
            this.rootNo = rootNo;
        }

        //************ Methods ************//
        /**
         * Returns the root number.
         *
         * @return the root number
         */
        public int getRootNumber() {
            return rootNo;
        }
    }
}
