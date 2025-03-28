package mcdr.objects.extraction;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import mcdr.preprocessing.transformation.SequenceMocapConvertor;
import mcdr.preprocessing.transformation.impl.FPSConvertor;
import mcdr.preprocessing.transformation.impl.MotionImageCombinedConvertor;
import mcdr.preprocessing.transformation.impl.MotionImageConvertor;
import mcdr.preprocessing.transformation.impl.MotionImageConvertorWeightedJointsByTrajDist;
import mcdr.preprocessing.transformation.impl.NormalizationOfOrientationConvertor;
import mcdr.preprocessing.transformation.impl.NormalizationOfPositionConvertor;
import mcdr.preprocessing.transformation.impl.NormalizationOfSkeletonSize;
import mcdr.sequence.KinematicTree;
import mcdr.sequence.SequenceMocap;
import mcdr.test.utils.SequenceMocapMgmt;
import messif.objects.extraction.Extractor;
import messif.objects.extraction.ExtractorDataSource;
import messif.objects.extraction.Extractors;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;
import messif.objects.keys.AbstractObjectKey;
import messif.utility.Convertor;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 * Extracts a caffe object from an input sequence by normalizing the sequence,
 * transforming the normalized sequence into the motion image, and extracting
 * the 4,096D feature vector using the deep convolutional neural network
 * listening on a provided host/port.
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class CaffeObjectMotionImageSocketExtractor {

    // host machine name which the neural-network listens to
    private static final String HOST_NAME = "cybela9.fi.muni.cz";
    // port on the host machine which the neural-network listens to
    private static final int PORT_NUMBER = 23399;
    // convertors for normalizing the sequence
    private final List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors;
    // convertor for transforming a sequence into a 2D motion image
    private final Convertor<SequenceMocap<?>, BufferedImage> motionImageConvertor;
    // extractor connected to a provided host/port
    private final Extractor<ObjectFloatVectorNeuralNetworkL2> caffeObjectExtractor;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link CaffeObjectMotionImageSocketExtractor}.
     *
     * @param sequenceConvertors convertors for normalizing the sequence
     * @param motionImageConvertor convertor for transforming a sequence into a
     * 2D motion image
     * @param caffeObjectExtractorHost host machine which the neural-network
     * listens to
     * @param caffeObjectExtractorPort port which the neural-network listens to
     * @throws NoSuchInstantiatorException
     */
    public CaffeObjectMotionImageSocketExtractor(List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors, Convertor<SequenceMocap<?>, BufferedImage> motionImageConvertor, String caffeObjectExtractorHost, int caffeObjectExtractorPort) throws NoSuchInstantiatorException {
        this.sequenceConvertors = sequenceConvertors;
        this.motionImageConvertor = motionImageConvertor;
        this.caffeObjectExtractor = Extractors.createSocketExtractor(ObjectFloatVectorNeuralNetworkL2.class, caffeObjectExtractorHost, new int[]{caffeObjectExtractorPort}, 0);
    }

    //************ Static methods ************//
    /**
     * Normalizes the input sequence by all specified convertors.
     *
     * @param sequence sequence to be normalized
     * @param sequenceConvertors convertors for normalizing the sequence
     *
     * @return normalized input sequence
     */
    public static SequenceMocap<?> normalizeSequence(SequenceMocap<?> sequence, List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors) {
        SequenceMocap<?> normalizedSequence = sequence;
        for (SequenceMocapConvertor<SequenceMocap<?>> sequenceConvertor : sequenceConvertors) {
            try {
                normalizedSequence = sequenceConvertor.convert(normalizedSequence);
            } catch (Exception ex) {
                System.err.println(ex.toString());
                return null;
            }
        }
        return normalizedSequence;
    }

    //************ Methods ************//
    /**
     * Normalizes the input sequence by all convertors loaded in advance.
     *
     * @param sequence sequence to be normalized
     * @return normalized input sequence
     */
    public SequenceMocap<?> normalizeSequence(SequenceMocap<?> sequence) {
        return normalizeSequence(sequence, this.sequenceConvertors);
    }

    /**
     * Generates a motion image from the input sequence, which should be already
     * normalized.
     *
     * @param normalizedSequence input sequence from which the motion image is
     * generated
     * @return generated motion image from the input sequence
     * @throws Exception
     */
    public BufferedImage generateMotionImage(SequenceMocap<?> normalizedSequence) throws Exception {
        return motionImageConvertor.convert(normalizedSequence);
    }

    /**
     * Extracts the 4,096D feature vector for an input sequence that is first
     * normalized and transformed into the motion image. The feature vector is
     * encapsulated within the {@link ObjectFloatVectorNeuralNetworkL2} object.
     *
     * @param sequence sequence used to extract its 4,096D feature vector
     * @return object encapsulating the extracted 4,096D feature vector
     * @throws Exception
     */
    public ObjectFloatVectorNeuralNetworkL2 extractObject(SequenceMocap<?> sequence) throws Exception {
        return extractObject(generateMotionImage(normalizeSequence(sequence)), sequence.getObjectKey());
    }

    /**
     * Extracts the 4,096D feature vector for an input motion image. The feature
     * vector is encapsulated within the
     * {@link ObjectFloatVectorNeuralNetworkL2} object.
     *
     * @param sequenceImage motion image used to extract its 4,096D feature
     * vector
     * @param objectKey object key that is assigned to the returned object
     * @return object encapsulating the extracted 4,096D feature vector
     * @throws Exception
     */
    public ObjectFloatVectorNeuralNetworkL2 extractObject(BufferedImage sequenceImage, AbstractObjectKey objectKey) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(sequenceImage, "png", baos);
        baos.flush();
        ObjectFloatVectorNeuralNetworkL2 extractedObject = caffeObjectExtractor.extract(new ExtractorDataSource(baos.toByteArray()));
        extractedObject.setObjectKey(objectKey);
        return extractedObject;
    }

    /**
     * Extracts the 4,096D feature vector for each motion image within an input
     * folder. The feature vector is encapsulated within the
     * {@link ObjectFloatVectorNeuralNetworkL2} object. All the extracted
     * objects are written into an output file.
     *
     * @param inputMotionImageFolder folder containing motion images
     * @param outputFilename output file to which the extracted objects are
     * written
     * @param filenameFilter filter that processes only such motion images that
     * satisfy the filter condition
     * @throws Exception
     */
    public void extractObjects(String inputMotionImageFolder, String outputFilename, FilenameFilter filenameFilter) throws Exception {
        int objectCount = 0;
        FileOutputStream stream = new FileOutputStream(outputFilename);
        for (File file : new File(inputMotionImageFolder).listFiles(filenameFilter)) {
            if (file.isFile()) {
                String[] filenameSplit = file.getName().split("\\|/");
                extractObject(ImageIO.read(file), new AbstractObjectKey(filenameSplit[filenameSplit.length - 1])).write(stream);
                objectCount++;
            }
        }
        stream.close();
        System.out.println("Extracted object count: " + objectCount);
    }

    //************ Factory methods ************//
    /**
     * Creates a list of sequence converters based on specified paramteres.
     *
     * @param sequenceClass class of the sequence
     * @param originalFPSRate original fps rate
     * @param reducedFPSRate reduced fps rate
     * @param normalizeByPosition indicates whether the position-normalization
     * convertor is created
     * @param centerByFirstPoseOnly decides whether to move all the poses
     * according to the fixed shift calculated in the first pose, or center each
     * pose independently so that the root has position [0, 0, 0]
     * @param normalizeByOrientation indicates whether the position-orientation
     * convertor is created
     * @param rotateByFirstPoseOnly decides whether to rotate all the poses
     * according to the fixed angle calculated in the first pose, or rotate each
     * pose independently so that they face a fixed direction
     * @param normalizeBySkeletonSize indicates whether the
     * skeleton-size-normalization convertor is created
     * @param boneLengthMap skeleton proportions to be set (key denotes the pair
     * of Ids of joints determining the specific bone and value represents the
     * target bone length)
     * @param kinematicTree kinematic model of the human body (key denotes the
     * joint Id and value represents Ids of descendant joints)
     * @return a list of sequence converters based on specified paramteres
     * @throws NoSuchInstantiatorException
     */
    public static List<SequenceMocapConvertor<SequenceMocap<?>>> createSequenceConvertors(Class<? extends SequenceMocap<?>> sequenceClass, int originalFPSRate, int reducedFPSRate, boolean normalizeByPosition, boolean centerByFirstPoseOnly, boolean normalizeByOrientation, boolean rotateByFirstPoseOnly, boolean normalizeBySkeletonSize, Map<Map.Entry<Integer, Integer>, Float> boneLengthMap, Map<Integer, int[]> kinematicTree) throws NoSuchInstantiatorException {
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = new LinkedList<>();
        sequenceConvertors.add(new FPSConvertor(sequenceClass, originalFPSRate, reducedFPSRate));
        if (normalizeByPosition) {
            sequenceConvertors.add(new NormalizationOfPositionConvertor(sequenceClass, centerByFirstPoseOnly));
        }
        if (normalizeByOrientation) {
            sequenceConvertors.add(new NormalizationOfOrientationConvertor(sequenceClass, rotateByFirstPoseOnly));
        }
        if (normalizeBySkeletonSize) {
            sequenceConvertors.add(new NormalizationOfSkeletonSize(sequenceClass, boneLengthMap, kinematicTree));
        }
        return sequenceConvertors;
    }

    public static CaffeObjectMotionImageSocketExtractor createHDM05Extractor(Class<? extends SequenceMocap<?>> sequenceClass) throws NoSuchInstantiatorException {
        // PaOaSn normalization
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, 120, 120, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        // HDM05 (objects-sequences-anglesext.data): minCoordValueNormalized: -20.350546, maxCoordValueNormalized: 21.727161, minCoordValue: -54.161297, maxCoordValue: 56.740623
        MotionImageConvertor motionImageConvertor = new MotionImageConvertor(-20.350546f, 21.727161f);
        return new CaffeObjectMotionImageSocketExtractor(sequenceConvertors, motionImageConvertor, HOST_NAME, PORT_NUMBER);
    }

    public static CaffeObjectMotionImageSocketExtractor createHDM05WeightedJointsByTrajDistDatasetExtractor(Class<? extends SequenceMocap<?>> sequenceClass, SequenceMocapMgmt sequenceMgmt) throws NoSuchInstantiatorException, Exception {
        // PaOaSn normalization
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, 120, 120, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        for (SequenceMocapConvertor<SequenceMocap<?>> convertor : sequenceConvertors) {
            sequenceMgmt.convert(convertor);
        }
        // HDM05 (objects-sequences-anglesext.data): minCoordValueNormalized: -20.350546, maxCoordValueNormalized: 21.727161, minCoordValue: -54.161297, maxCoordValue: 56.740623
        MotionImageConvertor motionImageConvertor = new MotionImageConvertorWeightedJointsByTrajDist(-20.350546f, 21.727161f, 0, sequenceMgmt);
        return new CaffeObjectMotionImageSocketExtractor(sequenceConvertors, motionImageConvertor, HOST_NAME, PORT_NUMBER);
    }

    public static CaffeObjectMotionImageSocketExtractor createHDM05CombinedImageExtractor(Class<? extends SequenceMocap<?>> sequenceClass, SequenceMocapMgmt sequenceMgmt) throws NoSuchInstantiatorException, Exception {
        // PaOaSn normalization
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, 120, 120, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        for (SequenceMocapConvertor<SequenceMocap<?>> convertor : sequenceConvertors) {
            sequenceMgmt.convert(convertor);
        }
        List<MotionImageConvertor> motionImageConvertors = new ArrayList<>();
        motionImageConvertors.add(MotionImageConvertor.createAxisMotionImageConvertor(sequenceMgmt));
        motionImageConvertors.add(new MotionImageConvertor(true, true, null, false));
        return new CaffeObjectMotionImageSocketExtractor(sequenceConvertors, new MotionImageCombinedConvertor(motionImageConvertors), HOST_NAME, PORT_NUMBER);
    }

    public static CaffeObjectMotionImageSocketExtractor createNTUExtractor(Class<? extends SequenceMocap<?>> sequenceClass) throws NoSuchInstantiatorException {
        // PaOaSn normalization convertors
//        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, 30, 30, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        // NTU dataset (PaOaSn normalization): minCoordValue=-21.342054, maxCoordValue=20.033237
        // PaSn normalization convertors
        // NTU dataset (PaOaSn normalization): minCoordValue=-19.65231, maxCoordValue=19.168163
        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, 30, 30, true, false, false, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        MotionImageConvertor motionImageConvertor = new MotionImageConvertor(-21.342054f, 20.033237f);
        return new CaffeObjectMotionImageSocketExtractor(sequenceConvertors, motionImageConvertor, HOST_NAME, PORT_NUMBER);
    }
}
