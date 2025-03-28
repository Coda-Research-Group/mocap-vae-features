package mcdr.preprocessing.transformation.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import mcda.commons.constants.LandmarkConstant;
import mcdr.objects.ObjectMocapPose;
import mcdr.sequence.SequenceMocap;
import mcdr.test.utils.SequenceMocapMgmt;
import messif.utility.Convertor;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MotionImageConvertor implements Convertor<SequenceMocap<?>, BufferedImage> {

    //************ Constants ************//
    // image height
    protected static final int DEFAULT_IMAGE_HEIGHT = 256;
    // minimum quantized coordinate value of any joint
    protected static final float CUBE_MIN_COORD_VALUE = 0f;
    // maximum quantized coordinate value of any joint after quantization
    protected static final float CUBE_MAX_COORD_VALUE = 1f;
    // indicates whether to create white borders around the image of about 13% - it corresponds, e.g., to the original image of 227x227 pixels with border of 29 pixels (the resulting image has 256x256 pixels)
    protected static final boolean CREATE_WHITE_BORDER = false;
    // indicates whether to finally resize the image to 256x256 pixels
    protected static final boolean SCALE_TO_FIXED_SIZE = false;

    //************ Attributes ************//
    // minimum/maximum coordinate values of each joint and axis before quantization [joint, x/y/z axis, minimum/maximum]
    protected final float[][][] extremalJointCoordAxisValues;
    // indicates whether extremal coordinate values of sequence are computed for each joint independently
    protected final boolean extremalCoordsSequenceJoints;
    // indicates whether extremal coordinate values of sequence are computed for each axis independently
    protected final boolean extremalCoordsSequenceAxis;
    // fixed image width - shorter movements are prolonged by "white spaces", longer movements are truncated (if it is set to null, the image width is set to the movement length)
    protected final Integer fixedImageWidth;
    // indicates whether the beginning movement position is shifted about a random number of frames (the fixedImageWidth parameter must be set to a not-null value)
    protected final boolean initialRandomShift;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link MotionImageConvertor}.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     */
    public MotionImageConvertor(float originalMinCoordValue, float originalMaxCoordValue) {
        this(originalMinCoordValue, originalMaxCoordValue, null, false);
    }

    /**
     * Creates a new instance of {@link MotionImageConvertor}.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     * @param fixedImageWidth fixed image width - shorter movements are
     * prolonged by "white spaces", longer movements are truncated (if it is set
     * to null, the image width is set to the movement length)
     */
    public MotionImageConvertor(float originalMinCoordValue, float originalMaxCoordValue, Integer fixedImageWidth) {
        this(originalMinCoordValue, originalMaxCoordValue, fixedImageWidth, false);
    }

    /**
     * Creates a new instance of {@link MotionImageConvertor}.
     *
     * @param originalMinCoordValue minimum coordinate value of any joint before
     * quantization
     * @param originalMaxCoordValue maximum coordinate value of any joint before
     * quantization
     * @param fixedImageWidth fixed image width - shorter movements are
     * prolonged by "white spaces", longer movements are truncated (if it is set
     * to null, the image width is set to the movement length)
     * @param initialRandomShift indicates whether the beginning movement
     * position is shifted about a random number of frames (the fixedImageWidth
     * parameter must be set to a not-null value)
     */
    public MotionImageConvertor(float originalMinCoordValue, float originalMaxCoordValue, Integer fixedImageWidth, boolean initialRandomShift) {
        this.extremalJointCoordAxisValues = new float[LandmarkConstant.LANDMARK_COUNT][3][2];
        for (int i = 0; i < LandmarkConstant.LANDMARK_COUNT; i++) {
            for (int j = 0; j < 3; j++) {
                this.extremalJointCoordAxisValues[i][j][0] = originalMinCoordValue;
                this.extremalJointCoordAxisValues[i][j][1] = originalMaxCoordValue;
            }
        }
        this.extremalCoordsSequenceJoints = false;
        this.extremalCoordsSequenceAxis = false;
        this.fixedImageWidth = fixedImageWidth;
        this.initialRandomShift = initialRandomShift;
    }

    /**
     * Creates a new instance of {@link MotionImageConvertor} that computes
     * extremal joint coordinate values independently for each sequence.
     *
     * @param extremalCoordsSequenceJoints indicates whether extremal coordinate
     * values of sequence are computed for each joint independently
     * @param extremalCoordsSequenceAxis indicates whether extremal coordinate
     * values of sequence are computed for each axis independently
     * @param fixedImageWidth fixed image width - shorter movements are
     * prolonged by "white spaces", longer movements are truncated (if it is set
     * to null, the image width is set to the movement length)
     * @param initialRandomShift indicates whether the beginning movement
     * position is shifted about a random number of frames (the fixedImageWidth
     * parameter must be set to a not-null value)
     */
    public MotionImageConvertor(boolean extremalCoordsSequenceJoints, boolean extremalCoordsSequenceAxis, Integer fixedImageWidth, boolean initialRandomShift) {
        this.extremalJointCoordAxisValues = null;
        this.extremalCoordsSequenceJoints = extremalCoordsSequenceJoints;
        this.extremalCoordsSequenceAxis = extremalCoordsSequenceAxis;
        this.fixedImageWidth = fixedImageWidth;
        this.initialRandomShift = initialRandomShift;
    }

    /**
     * Creates a new instance of {@link MotionImageConvertor}.
     *
     * @param extremalJointCoordAxisValues minimum/maximum (third matrix
     * dimension) coordinate value of individual joints (first matrix dimension)
     * for the x/y/z axis (second matrix dimension) before quantization
     * @param fixedImageWidth fixed image width - shorter movements are
     * prolonged by "white spaces", longer movements are truncated (if it is set
     * to null, the image width is set to the movement length)
     * @param initialRandomShift indicates whether the beginning movement
     * position is shifted about a random number of frames (the fixedImageWidth
     * parameter must be set to a not-null value)
     */
    public MotionImageConvertor(float[][][] extremalJointCoordAxisValues, Integer fixedImageWidth, boolean initialRandomShift) {
        this.extremalJointCoordAxisValues = extremalJointCoordAxisValues;
        this.extremalCoordsSequenceJoints = false;
        this.extremalCoordsSequenceAxis = false;
        this.fixedImageWidth = fixedImageWidth;
        this.initialRandomShift = initialRandomShift;
    }

    //************ Methods ************//
    protected float[][] quantizePoseCoords(ObjectMocapPose pose, float[][][] sequenceExtremalJointCoordAxisValues) {
        float newRange = CUBE_MAX_COORD_VALUE - CUBE_MIN_COORD_VALUE;
        float[][] cubeCoords = new float[pose.getJointCoordinates().length][3];
        for (int i = 0; i < pose.getJointCoordinates().length; i++) {
            for (int j = 0; j < 3; j++) {
                float oldMin = sequenceExtremalJointCoordAxisValues[i][j][0];
                float oldMax = sequenceExtremalJointCoordAxisValues[i][j][1];
                float oldRange = oldMax - oldMin;
                cubeCoords[i][j] = (((pose.getJointCoordinates()[i][j] - oldMin) * newRange) / oldRange) + CUBE_MIN_COORD_VALUE;
            }
        }
        return cubeCoords;
    }

    protected List<float[][]> transformSequenceToRGBCube(SequenceMocap<?> sequence) {
        float[][][] sequenceExtremalJointCoordAxisValues = extremalJointCoordAxisValues;

        // Computes extremal joint coordinate values based on the specified sequence
        if (sequenceExtremalJointCoordAxisValues == null) {
            sequenceExtremalJointCoordAxisValues = new float[LandmarkConstant.LANDMARK_COUNT][3][2];

            // sequence (no joints/no axes)
            if (!extremalCoordsSequenceJoints && !extremalCoordsSequenceAxis) {
                float minValue = sequence.getExtremalCoordValue(true);
                float maxValue = sequence.getExtremalCoordValue(false);
                for (int i = 0; i < LandmarkConstant.LANDMARK_COUNT; i++) {
                    for (int j = 0; j < 3; j++) {
                        sequenceExtremalJointCoordAxisValues[i][j][0] = minValue;
                        sequenceExtremalJointCoordAxisValues[i][j][1] = maxValue;
                    }
                }
            }

            // joints/no axes
            if (extremalCoordsSequenceJoints && !extremalCoordsSequenceAxis) {
                for (int i = 0; i < LandmarkConstant.LANDMARK_COUNT; i++) {
                    float minValue = sequence.getExtremalJointCoordValue(true, i);
                    float maxValue = sequence.getExtremalJointCoordValue(false, i);
                    for (int j = 0; j < 3; j++) {
                        sequenceExtremalJointCoordAxisValues[i][j][0] = minValue;
                        sequenceExtremalJointCoordAxisValues[i][j][1] = maxValue;
                    }
                }
            }

            // no joints/axes
            if (!extremalCoordsSequenceJoints && extremalCoordsSequenceAxis) {
                for (int j = 0; j < 3; j++) {
                    float minValue = sequence.getExtremalAxisCoordValue(true, j);
                    float maxValue = sequence.getExtremalAxisCoordValue(false, j);
                    for (int i = 0; i < LandmarkConstant.LANDMARK_COUNT; i++) {
                        sequenceExtremalJointCoordAxisValues[i][j][0] = minValue;
                        sequenceExtremalJointCoordAxisValues[i][j][1] = maxValue;
                    }
                }
            }

            // joints/axes
            if (extremalCoordsSequenceJoints && extremalCoordsSequenceAxis) {
                for (int i = 0; i < LandmarkConstant.LANDMARK_COUNT; i++) {
                    for (int j = 0; j < 3; j++) {
                        sequenceExtremalJointCoordAxisValues[i][j][0] = sequence.getExtremalJointAxisCoordValue(true, i, j);
                        sequenceExtremalJointCoordAxisValues[i][j][1] = sequence.getExtremalJointAxisCoordValue(false, i, j);
                    }
                }
            }
        }

        List<float[][]> stripeImages = new ArrayList<>(sequence.getSequenceLength());
        for (int frameIdx = 0; frameIdx < sequence.getSequenceLength(); frameIdx++) {
            ObjectMocapPose o = sequence.getObject(frameIdx);
            stripeImages.add(quantizePoseCoords(o, sequenceExtremalJointCoordAxisValues));
        }
        return stripeImages;
    }

    /**
     * Returns heights of individual joints (in pixels) for the input sequence.
     *
     * @param sequence sequence for which heights of joints are determined
     * @return heights of individual joints (in pixels) for the input sequence
     */
    protected int[] getJointHeights(SequenceMocap<?> sequence) {
        int[] jointHeights = new int[sequence.getJointCount()];
        Arrays.fill(jointHeights, DEFAULT_IMAGE_HEIGHT / sequence.getJointCount());
        return jointHeights;
    }

    /**
     * Creates a new image by scaling the input image based on specified
     * proportions.
     *
     * @param image image to be scaled
     * @param newWidth new image width
     * @param newHeight new image height
     * @return a new scaled image
     */
    public BufferedImage scaleImage(BufferedImage image, int newWidth, int newHeight) {
        BufferedImage rtv = null;
        if (image != null) {
            rtv = new BufferedImage(newWidth, newHeight, image.getType());
            Graphics2D g2 = rtv.createGraphics();
            g2.drawRenderedImage(image, AffineTransform.getScaleInstance((double) newWidth / image.getWidth(), (double) newHeight / image.getHeight()));
        }
        return rtv;
    }

    //************ Factory methods  ************//
    public static MotionImageConvertor createAxisMotionImageConvertor(SequenceMocapMgmt sequenceMgmt) {
        float[][][] extremalCoordValues = new float[LandmarkConstant.LANDMARK_COUNT][3][2];
        for (int j = 0; j < 3; j++) {
            extremalCoordValues[0][j][0] = sequenceMgmt.getExtremalAxisCoordValue(true, j);
            extremalCoordValues[0][j][1] = sequenceMgmt.getExtremalAxisCoordValue(false, j);
        }
        for (int jointIdx = 1; jointIdx < LandmarkConstant.LANDMARK_COUNT; jointIdx++) {
            for (int j = 0; j < 3; j++) {
                extremalCoordValues[jointIdx][j][0] = extremalCoordValues[0][j][0];
                extremalCoordValues[jointIdx][j][1] = extremalCoordValues[0][j][1];
            }
        }
        return new MotionImageConvertor(extremalCoordValues, null, false);
    }

    public static MotionImageConvertor createJointMotionImageConvertor(SequenceMocapMgmt sequenceMgmt) {
        float[][][] extremalCoordValues = new float[LandmarkConstant.LANDMARK_COUNT][3][2];
        for (int jointIdx = 0; jointIdx < LandmarkConstant.LANDMARK_COUNT; jointIdx++) {
            float minJointValue = sequenceMgmt.getExtremalJointCoordValue(true, jointIdx);
            float maxJointValue = sequenceMgmt.getExtremalJointCoordValue(false, jointIdx);
            for (int j = 0; j < 3; j++) {
                extremalCoordValues[jointIdx][j][0] = minJointValue;
                extremalCoordValues[jointIdx][j][1] = maxJointValue;
            }
        }
        return new MotionImageConvertor(extremalCoordValues, null, false);
    }

    public static MotionImageConvertor createSegmentAxisMotionImageConvertor(SequenceMocapMgmt sequenceMgmt) {
        final int[][] jointSegments = new int[][]{
            new int[]{LandmarkConstant.LANDMARK_LHIPJOINT_ID, LandmarkConstant.LANDMARK_LFEMUR_ID, LandmarkConstant.LANDMARK_LTIBIA_ID, LandmarkConstant.LANDMARK_LFOOT_ID, LandmarkConstant.LANDMARK_LTOES_ID},
            new int[]{LandmarkConstant.LANDMARK_RHIPJOINT_ID, LandmarkConstant.LANDMARK_RFEMUR_ID, LandmarkConstant.LANDMARK_RTIBIA_ID, LandmarkConstant.LANDMARK_RFOOT_ID, LandmarkConstant.LANDMARK_RTOES_ID},
            new int[]{LandmarkConstant.LANDMARK_LCLAVICLE_ID, LandmarkConstant.LANDMARK_LHUMERUS_ID, LandmarkConstant.LANDMARK_LRADIUS_ID, LandmarkConstant.LANDMARK_LWRIST_ID, LandmarkConstant.LANDMARK_LHAND_ID, LandmarkConstant.LANDMARK_LFINGERS_ID, LandmarkConstant.LANDMARK_LTHUMB_ID},
            new int[]{LandmarkConstant.LANDMARK_RCLAVICLE_ID, LandmarkConstant.LANDMARK_RHUMERUS_ID, LandmarkConstant.LANDMARK_RRADIUS_ID, LandmarkConstant.LANDMARK_RWRIST_ID, LandmarkConstant.LANDMARK_RHAND_ID, LandmarkConstant.LANDMARK_RFINGERS_ID, LandmarkConstant.LANDMARK_RTHUMB_ID},
            new int[]{LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_LOWERBACK_ID, LandmarkConstant.LANDMARK_UPPERBACK_ID, LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_UPPERNECK_ID, LandmarkConstant.LANDMARK_HEAD_ID}
        };

        float[][][] extremalCoordValues = new float[LandmarkConstant.LANDMARK_COUNT][3][2];
        for (int[] jointSegment : jointSegments) {
            for (int j = 0; j < 3; j++) {
                float minJointSegmentAxisValue = Float.MAX_VALUE;
                float maxJointSegmentAxisValue = Float.MIN_VALUE;
                for (int jointID : jointSegment) {
                    int jointIdx = LandmarkConstant.getLandmarkPos(jointID);
                    for (SequenceMocap<?> sequence : sequenceMgmt.getSequences()) {
                        minJointSegmentAxisValue = Math.min(minJointSegmentAxisValue, sequence.getExtremalJointAxisCoordValue(true, jointIdx, j));
                        maxJointSegmentAxisValue = Math.max(maxJointSegmentAxisValue, sequence.getExtremalJointAxisCoordValue(false, jointIdx, j));
                    }
                }
                for (int jointID : jointSegment) {
                    int jointIdx = LandmarkConstant.getLandmarkPos(jointID);
                    extremalCoordValues[jointIdx][j][0] = minJointSegmentAxisValue;
                    extremalCoordValues[jointIdx][j][1] = maxJointSegmentAxisValue;
                }
            }
        }
        return new MotionImageConvertor(extremalCoordValues, null, false);
    }

    //************ Implemented interface Convertor ************//
    @Override
    public BufferedImage convert(SequenceMocap<?> sequence) {
        List<float[][]> stripeImages = transformSequenceToRGBCube(sequence);
        BufferedImage image = new BufferedImage((fixedImageWidth == null) ? stripeImages.size() : fixedImageWidth, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int initialFrameShift = !(initialRandomShift && fixedImageWidth != null) ? 0 : new Random().nextInt(Math.max(1, image.getWidth() - stripeImages.size() + 1));
        Graphics2D g2 = image.createGraphics();
        int[] jointHeights = getJointHeights(sequence);
        for (int frame = 0; frame < ((fixedImageWidth == null) ? stripeImages.size() : Math.min(stripeImages.size(), fixedImageWidth)); frame++) {
            float[][] stripeImage = stripeImages.get(frame);
            int jointHeightPos = 0;
            for (int jointIdx = 0; jointIdx < stripeImage.length; jointIdx++) {
                if (Float.isNaN(stripeImage[jointIdx][0]) || Float.isNaN(stripeImage[jointIdx][1]) || Float.isNaN(stripeImage[jointIdx][2])) {
                    g2.setColor(new Color(0f, 0f, 0f));
                } else {
                    g2.setColor(new Color(stripeImage[jointIdx][0], stripeImage[jointIdx][1], stripeImage[jointIdx][2]));
                }
                g2.fillRect(initialFrameShift + frame, jointHeightPos, 1, jointHeights[jointIdx]);
                jointHeightPos += jointHeights[jointIdx];
            }
        }

        // Adds the white border around the image
        if (CREATE_WHITE_BORDER) {
            float enlargementRatio = (float) 256 / 227;
            BufferedImage borderImage = new BufferedImage(Math.round(image.getWidth() * enlargementRatio), Math.round(image.getHeight() * enlargementRatio), image.getType());
            Graphics2D g2borderImage = borderImage.createGraphics();
            g2borderImage.setColor(Color.WHITE);
            g2borderImage.fillRect(0, 0, borderImage.getWidth(), borderImage.getHeight());
            g2borderImage.drawImage(image, (borderImage.getWidth() - image.getWidth()) / 2, (borderImage.getHeight() - image.getHeight()) / 2, null);
        }

        // Scales the image
        if (SCALE_TO_FIXED_SIZE) {
            image = scaleImage(image, 256, 256);
        }

        return image;
    }

    @Override
    public Class<? extends BufferedImage> getDestinationClass() {
        return BufferedImage.class;
    }
}
