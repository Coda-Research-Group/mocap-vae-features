package mcdr.preprocessing.segmentation.impl;

import java.util.ArrayList;
import java.util.List;
import mcdr.sequence.SequenceMocap;
import messif.objects.keys.AbstractObjectKey;
import messif.utility.reflection.NoSuchInstantiatorException;
import mcdr.preprocessing.segmentation.SegmentConvertor;
import mcdr.test.utils.ObjectMgmt;

/**
 * @param <O> type of sequence
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class RegularSegmentConvertor<O extends SequenceMocap<?>> extends SegmentConvertor<O> {

    // default segment size (with eventual exception of the last segment)
    private final int segmentSize;
    // relative number of poses between two consecutive segments with respect to the segment size (default value 1.0 means that segments are immediately concatenated)
    private final float segmentShiftRatio;
    // number of poses which are ignored before the first segment begins
    private final int initialSegmentShift;
    // indicates whether the last segment can be cropped (default value is false)
    private final boolean trimLastSegment;
    // same category of the segment as of the parent sequence
    private final boolean sameSegmentCategoryAsSequenceCategory;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link RegularSegmentConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @param segmentSize default segment size (with eventual exception of the
     * last segment)
     * @throws NoSuchInstantiatorException
     */
    public RegularSegmentConvertor(Class<O> sequenceClass, int segmentSize) throws NoSuchInstantiatorException {
        this(sequenceClass, segmentSize, 1f, 0, false, false);
    }

    /**
     * Creates a new instance of {@link RegularSegmentConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @param segmentSize default segment size (with eventual exception of the
     * last segment)
     * @param segmentShiftRatio relative number of poses between two consecutive
     * segments with respect to the segment size (default value 1.0 means that
     * segments are immediately concatenated)
     * @param initialSegmentShift number of poses which are ignored before the
     * first segment begins
     * @param trimLastSegment indicates whether the last segment can be cropped
     * (default value is false)
     * @throws NoSuchInstantiatorException
     */
    public RegularSegmentConvertor(Class<O> sequenceClass, int segmentSize, float segmentShiftRatio, int initialSegmentShift, boolean trimLastSegment) throws NoSuchInstantiatorException {
        this(sequenceClass, segmentSize, segmentShiftRatio, initialSegmentShift, trimLastSegment, false);
    }

    /**
     * Creates a new instance of {@link RegularSegmentConvertor}.
     *
     * @param sequenceClass class of the sequence
     * @param segmentSize default segment size (with eventual exception of the
     * last segment)
     * @param segmentShiftRatio relative number of poses between two consecutive
     * segments with respect to the segment size (default value 1.0 means that
     * segments are immediately concatenated)
     * @param initialSegmentShift number of poses which are ignored before the
     * first segment begins
     * @param trimLastSegment indicates whether the last segment can be cropped
     * (default value is false)
     * @param sameSegmentCategoryAsSequenceCategory indicates whether category
     * of the segment is set the same as the category of its parent sequence
     * @throws NoSuchInstantiatorException
     */
    public RegularSegmentConvertor(Class<O> sequenceClass, int segmentSize, float segmentShiftRatio, int initialSegmentShift, boolean trimLastSegment, boolean sameSegmentCategoryAsSequenceCategory) throws NoSuchInstantiatorException {
        super(sequenceClass);
        this.segmentSize = segmentSize;
        this.segmentShiftRatio = segmentShiftRatio;
        this.initialSegmentShift = initialSegmentShift;
        this.trimLastSegment = trimLastSegment;
        this.sameSegmentCategoryAsSequenceCategory = sameSegmentCategoryAsSequenceCategory;
    }

    //************ Implemented class SegmentConvertor ************//
    @Override
    public List<O> convert(O sequence) {
        List<O> segments = new ArrayList<>();
        int fromIndex = initialSegmentShift;
        while (fromIndex < sequence.getSequenceLength()) {
            int toIndex = Math.min(fromIndex + segmentSize, sequence.getSequenceLength());

            // Checks whether the whole segment (in the default size) can be generated
            if (trimLastSegment || toIndex - fromIndex == segmentSize) {

                // Generates the segment
                String segmentCategory = (sameSegmentCategoryAsSequenceCategory) ? ObjectMgmt.parseObjectCategoryId(sequence) : sequence.getSequenceId() + "-" + fromIndex + "-" + (toIndex - fromIndex);
                O segment = getSubsequence(sequence, fromIndex, toIndex);
                segment.setObjectKey(new AbstractObjectKey(sequence.getSequenceId() + "_" + segmentCategory + "_" + fromIndex + "_" + (toIndex - fromIndex)));
                segments.add(segment);
            }

            // Computes the beginning index of the following segment
            fromIndex += Math.floor(segmentSize * segmentShiftRatio);
        }
        return segments;
    }
}
