package mcdr.test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import mcda.commons.constants.LandmarkConstant;
import mcdr.objects.ObjectMocapPose;
import mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import messif.objects.keys.AbstractObjectKey;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceNTUProcessor {

    //************ Constants ************//
    // mocap skeleton kinematic tree  -- map of "to" - "from" pairs of indexes (copies Kinect coord from "from" index to Vicon coord "to" index)
    public static final Map<Integer, Integer> MOCAP_KINECT2_SAMEJOINTS_MAP = Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
        {
            //
            put(LandmarkConstant.LANDMARK_LTOES_ID, LandmarkConstant.LANDMARK_LFOOT_ID);
            put(LandmarkConstant.LANDMARK_RTOES_ID, LandmarkConstant.LANDMARK_RFOOT_ID);
            put(LandmarkConstant.LANDMARK_UPPERBACK_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID);
            put(LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID);
            put(LandmarkConstant.LANDMARK_LRADIUS_ID, LandmarkConstant.LANDMARK_LWRIST_ID);
            put(LandmarkConstant.LANDMARK_RRADIUS_ID, LandmarkConstant.LANDMARK_RWRIST_ID);
        }
    });

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // data params
        final boolean filterNoisySequences = true;
        final boolean filterNoisySequencesUsingGT = true;
        final float filterSpreadRatio = 0.9f;
        final boolean filterInteractions = false;
//        final float filterNotTrackedJointsSequencesRatio = 0.1f;
        final float filterNotTrackedJointsSequencesRatio = Float.MAX_VALUE;
        final boolean setNotTrackedJointsAsNaNCoords = false;
        final boolean isAnnotationAvailableInLocator = true;
//        final File inputNTUFolder = new File("g:/datasets/mocap/NTU/data-challenge16/");
        final File inputNTUFolder = new File("y:/datasets/mocap/NTU/data/");
//        final String inputFileRegex = ".*\\.skeleton";
//        final String inputFileRegex = "S001C...P...R...A...\\.skeleton";
        final String inputFileRegex = "S00.C00.P001R001A01.\\.skeleton";
        final String inputRestrictedSkeletonsFile = "y:/datasets/mocap/NTU/samples_with_missing_skeletons.txt";
//        final String outputSequenceFile = null;
        final String outputSequenceFile = "d:/datasets/mocap/NTU/objects-annotations_S001_filtered" + filterSpreadRatio + "GT-coords_normP.data";
        final String outputMotionImageFolder = null;
//        final String outputMotionImageFolder = "d:/datasets/mocap/NTU/motion_images/tmp/";
        final String outputMotionImageFile = null;
//        final String outputMotionImageFile = "d:/datasets/mocap/NTU/motion_images/objects-test_filtered" + filterSpreadRatio + "-coords_normPOS-caffe.data";
        final Class<? extends SequenceMocap<?>> outputSequenceClass = SequenceMocapPoseCoordsL2DTW.class;
        final Class<? extends ObjectMocapPose> outputPoseClass = ObjectMocapPoseCoordsL2.class;

        // Caffe extractor
        final CaffeObjectMotionImageSocketExtractor caffeObjectExtractor = CaffeObjectMotionImageSocketExtractor.createNTUExtractor(SequenceMocapPoseCoordsL2DTW.class);

        // Reads restricted sequence files
        Set<String> restrictedSkeletons = new HashSet<>();
        if (inputRestrictedSkeletonsFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(inputRestrictedSkeletonsFile));
            String line = br.readLine();
            while (line != null) {
                restrictedSkeletons.add(line);
                line = br.readLine();
            }
            br.close();
        }

        OutputStream outputSequenceStream = (outputSequenceFile == null) ? null : new FileOutputStream(outputSequenceFile);
        OutputStream outputMotionImageStream = (outputMotionImageFile == null) ? null : new FileOutputStream(outputMotionImageFile);
        int sequenceFileCount = 0;
        int sequenceCount = 0;
        int totalSequenceLength = 0;
        int totalSequenceOverFileLength = 0;
        int filteredSequenceCount = 0;
        int filteredSequenceCountNonInteractionsFailed = 0;
        int filteredSequenceCountInteractionsFailed = 0;
        double avgJointSpreadsNonFiltered[] = new double[3];
        double avgJointSpreadsFiltered[] = new double[3];
        float minCoordValue = Float.MAX_VALUE;
        float maxCoordValue = Float.MIN_VALUE;
        for (File file : inputNTUFolder.listFiles()) {
            String fileName = file.getName();
            final String annotationClassId = (!isAnnotationAvailableInLocator) ? null : fileName.substring(17, 20);
            if (fileName.matches(inputFileRegex) && !restrictedSkeletons.contains(fileName.split("\\.")[0])
                    && (!isAnnotationAvailableInLocator || !filterInteractions || Integer.parseInt(annotationClassId) < 50)) {

                BufferedReader br = new BufferedReader(new FileReader(file));
                int poseCount = Integer.parseInt(br.readLine());
                Map<String, SequenceProp> subjectSequencePropMap = new HashMap<>();

                for (int f = 0; f < poseCount; f++) {
                    int subjectCount = Integer.parseInt(br.readLine());

                    for (int subject = 0; subject < subjectCount; subject++) {
                        String subjectId = br.readLine().split(" ")[0];
                        int subjectJointCountStatusNotTracked = 0;
                        int subjectJointCountStatusAll = 0;

                        int jointCount = Integer.parseInt(br.readLine());
                        float[][] jointCoords = new float[LandmarkConstant.LANDMARK_COUNT][3];
                        for (int j = 0; j < jointCount; j++) {
                            String[] lineSplit = br.readLine().split(" ");
                            int jointTrackingState = Integer.parseInt(lineSplit[11]);
                            for (int i = 0; i < 3; i++) {
                                int jointIdx = LandmarkConstant.getLandmarkPos(LandmarkConstant.KINECT2_MOCAP_MAP.get(j));
                                if (setNotTrackedJointsAsNaNCoords && jointTrackingState != 2) {
                                    jointCoords[jointIdx][i] = Float.NaN;
                                } else {
                                    jointCoords[jointIdx][i] = Float.parseFloat(lineSplit[i]);
                                }
                            }
                            if (jointTrackingState != 2) {
                                subjectJointCountStatusNotTracked++;
                            }
                            subjectJointCountStatusAll++;
                        }
                        for (Map.Entry<Integer, Integer> sameJointMapEntry : MOCAP_KINECT2_SAMEJOINTS_MAP.entrySet()) {
                            for (int i = 0; i < 3; i++) {
                                jointCoords[LandmarkConstant.getLandmarkPos(sameJointMapEntry.getKey())][i] = jointCoords[LandmarkConstant.getLandmarkPos(sameJointMapEntry.getValue())][i];
                            }
                        }

                        ObjectMocapPose pose = outputPoseClass.getConstructor(float[][].class).newInstance(new Object[]{jointCoords});
                        pose.setFrameNo(f);

                        SequenceProp subjectSequenceProp = subjectSequencePropMap.get(subjectId);
                        if (subjectSequenceProp == null) {
                            subjectSequenceProp = new SequenceProp();
                            subjectSequenceProp.subjectId = subjectId;
                            subjectSequencePropMap.put(subjectId, subjectSequenceProp);
                        }
                        subjectSequenceProp.poses.add(pose);
                        subjectSequenceProp.jointCountStatusAll += subjectJointCountStatusAll;
                        subjectSequenceProp.jointCountStatusNotTracked += subjectJointCountStatusNotTracked;
                    }
                }

                sequenceCount += subjectSequencePropMap.size();
                float highestJointSpreadRatio = Float.MIN_VALUE;
                int highestSequenceLength = 0;
                int combinedSequenceFrameNoMin = Integer.MAX_VALUE;
                int combinedSequenceFrameNoMax = Integer.MIN_VALUE;
                for (SequenceProp sequenceProp : subjectSequencePropMap.values()) {

                    // Creates a sequence object
                    SequenceMocap<?> sequence = outputSequenceClass.getConstructor(List.class).newInstance(sequenceProp.poses);
                    sequenceProp.sequence = sequence;
                    sequence.setObjectKey(new AbstractObjectKey(fileName.substring(0, fileName.indexOf(".")) + "_" + (!isAnnotationAvailableInLocator ? "XXX" : annotationClassId) + "_" + sequence.getObject(0).getFrameNo() + "_" + sequence.getSequenceLength() + "_" + sequenceProp.subjectId));
                    combinedSequenceFrameNoMin = Math.min(combinedSequenceFrameNoMin, sequence.getObject(0).getFrameNo());
                    combinedSequenceFrameNoMax = Math.max(combinedSequenceFrameNoMax, sequence.getObject(0).getFrameNo() + sequence.getSequenceLength() - 1);

                    // Normalizes the sequence
                    sequenceProp.sequenceConverted = caffeObjectExtractor.normalizeSequence(sequence);

                    // Computes spreads of joints
                    float minX = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE;
                    float minY = Float.MAX_VALUE;
                    float maxY = Float.MIN_VALUE;
                    for (ObjectMocapPose o : sequenceProp.sequenceConverted.getObjects()) {
                        for (float[] coords : o.getJointCoordinates()) {
                            minX = Math.min(coords[0], minX);
                            maxX = Math.max(coords[0], maxX);
                            minY = Math.min(coords[1], minY);
                            maxY = Math.max(coords[1], maxY);
                            for (int j = 0; j < 3; j++) {
                                minCoordValue = Math.min(coords[j], minCoordValue);
                                maxCoordValue = Math.max(coords[j], maxCoordValue);
                            }
                        }
                    }
                    sequenceProp.jointSpreadX = maxX - minX;
                    sequenceProp.jointSpreadY = maxY - minY;
                    sequenceProp.jointSpreadRatio = sequenceProp.jointSpreadX / sequenceProp.jointSpreadY;
                    highestJointSpreadRatio = Math.max(highestJointSpreadRatio, sequenceProp.jointSpreadRatio);
                    highestSequenceLength = Math.max(highestSequenceLength, sequenceProp.sequence.getSequenceLength());
                }

                // Filters out the sequence if it has a low number of properly tracked joints
                Set<String> subjectsToRemove = new HashSet<>();
                for (SequenceProp sequenceProp : subjectSequencePropMap.values()) {
                    if ((float) sequenceProp.jointCountStatusNotTracked / sequenceProp.jointCountStatusAll > filterNotTrackedJointsSequencesRatio) {
                        subjectsToRemove.add(sequenceProp.subjectId);
                    }
                }
                for (String subjectId : subjectsToRemove) {
                    subjectSequencePropMap.remove(subjectId);
                }

                // Filters out the sequence if it is noisy (e.g., chair and table)
                if (filterNoisySequences) {

                    // Sorts sequences by a filtering coefficient
                    TreeMap<Float, SequenceProp> sequencePropMapToFilter = new TreeMap<>();
                    for (SequenceProp sequenceProp : subjectSequencePropMap.values()) {
                        float filteringCoefficient = sequenceProp.jointSpreadRatio / highestJointSpreadRatio
                                + (1f - (float) sequenceProp.sequence.getSequenceLength() / highestSequenceLength);
                        sequencePropMapToFilter.put(filteringCoefficient, sequenceProp);
                    }

                    // Number of sequences to retain according to the ground truth
                    int sequenceCountToRetain = 1;
                    if (isAnnotationAvailableInLocator && filterNoisySequencesUsingGT) {
                        if (isAnnotationAvailableInLocator && Integer.parseInt(annotationClassId) >= 50) {
                            sequenceCountToRetain = 2;
                        }
                    } else if (sequencePropMapToFilter.size() > 1) {
                        Iterator<SequenceProp> sequencePropIt = sequencePropMapToFilter.values().iterator();
                        sequencePropIt.next();

                        // If the second sequence satisfies the spread properties, it is retained
                        SequenceProp sequenceProp = sequencePropIt.next();
                        if (sequenceProp.jointSpreadX * filterSpreadRatio <= sequenceProp.jointSpreadY) {
                            sequenceCountToRetain = 2;
                        }
//                        System.out.println("Num: " + sequencePropMapToFilter.size());
                    }

                    // Removes the sequences to be filtered (i.e., not retained)
                    int sequenceIndex = 0;
                    for (SequenceProp sequenceProp : sequencePropMapToFilter.values()) {
                        if (sequenceIndex < sequenceCountToRetain) {
                            avgJointSpreadsNonFiltered[0] += sequenceProp.jointSpreadX;
                            avgJointSpreadsNonFiltered[1] += sequenceProp.jointSpreadY;
                            avgJointSpreadsNonFiltered[2] += sequenceProp.jointSpreadRatio;
                        } else {
                            avgJointSpreadsFiltered[0] += sequenceProp.jointSpreadX;
                            avgJointSpreadsFiltered[1] += sequenceProp.jointSpreadY;
                            avgJointSpreadsFiltered[2] += sequenceProp.jointSpreadRatio;
                            subjectSequencePropMap.remove(sequenceProp.subjectId);
                            filteredSequenceCount++;
                        }
                        sequenceIndex++;
                    }

                    // Statistics
                    if (isAnnotationAvailableInLocator) {
                        if (Integer.parseInt(annotationClassId) < 50) {
                            filteredSequenceCountNonInteractionsFailed += Math.abs(1 - subjectSequencePropMap.size());
                        } else {
                            filteredSequenceCountInteractionsFailed += Math.abs(2 - subjectSequencePropMap.size());
                        }
                    }
                }

                // Statistics
                sequenceFileCount++;
                int maxSequenceLength = 0;
                for (SequenceProp sequenceProp : subjectSequencePropMap.values()) {
                    totalSequenceLength += sequenceProp.sequence.getSequenceLength();
                    maxSequenceLength = Math.max(maxSequenceLength, sequenceProp.sequence.getSequenceLength());
                }
                totalSequenceOverFileLength += maxSequenceLength;

                // Stores the sequences
                if (outputSequenceStream != null) {
                    for (SequenceProp sequenceProp : subjectSequencePropMap.values()) {
                        sequenceProp.sequence.write(outputSequenceStream);
                    }
                }

                // Stores the motion images/extracted features
                if (subjectSequencePropMap.size() > 0 && (outputMotionImageFolder != null || outputMotionImageStream != null)) {
                    List<BufferedImage> motionImages = new ArrayList<>(subjectSequencePropMap.size());
                    int combinedImageWidth = 0;
                    int combinedImageHeight = 0;
                    for (SequenceProp sequenceProp : subjectSequencePropMap.values()) {
                        BufferedImage motionImage = caffeObjectExtractor.generateMotionImage(sequenceProp.sequenceConverted);
                        combinedImageWidth += motionImage.getWidth();
                        combinedImageHeight = Math.max(combinedImageHeight, motionImage.getHeight());
                        motionImages.add(motionImage);
                    }

                    // Generates the combined (interactioon) image
                    int motionImageSepSize = 10;
                    combinedImageWidth += (subjectSequencePropMap.size() - 1) * motionImageSepSize;
                    BufferedImage combinedImage = new BufferedImage(combinedImageWidth, combinedImageHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = combinedImage.createGraphics();
                    g2.setColor(Color.white);
                    g2.fillRect(0, 0, combinedImageWidth, combinedImageWidth);
                    int motionImageStartX = 0;
                    for (BufferedImage motionImage : motionImages) {
                        g2.drawImage(motionImage, motionImageStartX, 0, null);
                        motionImageStartX += motionImage.getWidth() + motionImageSepSize;
                    }

                    AbstractObjectKey combinedSequenceKey = new AbstractObjectKey(fileName.substring(0, fileName.indexOf(".")) + "_" + (!isAnnotationAvailableInLocator ? "XXX" : annotationClassId) + "_" + combinedSequenceFrameNoMin + "_" + (combinedSequenceFrameNoMax - combinedSequenceFrameNoMin + 1));
                    if (outputMotionImageFolder != null) {
                        ImageIO.write(combinedImage, "png", new File(outputMotionImageFolder + combinedSequenceKey.getLocatorURI() + ".png"));
                    }
                    if (outputMotionImageStream != null) {
                        caffeObjectExtractor.extractObject(combinedImage, combinedSequenceKey).write(outputMotionImageStream);
                    }

                }
                br.close();
            }
        }
        if (outputSequenceStream != null) {
            outputSequenceStream.close();
        }
        if (outputMotionImageStream != null) {
            outputMotionImageStream.close();
        }

        System.out.println("Sequence count: " + sequenceCount);
        System.out.println("  sequence file count: " + sequenceFileCount);
        System.out.println("  totalSequenceLength: " + totalSequenceLength);
        System.out.println("  totalSequenceOverFileLength: " + totalSequenceOverFileLength);
        System.out.println("  minCoordValue=" + minCoordValue + ", maxCoordValue=" + maxCoordValue);
        System.out.println("Average joint-locations spread values:");
        int nonFilteredSequenceCount = sequenceCount - filteredSequenceCount;
        System.out.println("  X: " + (avgJointSpreadsNonFiltered[0] / nonFilteredSequenceCount));
        System.out.println("  Y: " + (avgJointSpreadsNonFiltered[1] / nonFilteredSequenceCount));
        System.out.println("  X/Y: " + (avgJointSpreadsNonFiltered[2] / nonFilteredSequenceCount));
        if (filterNoisySequences) {
            System.out.println("Filtered sequence count: " + filteredSequenceCount);
            System.out.println("Filtered sequence count (non-interactions fails): " + filteredSequenceCountNonInteractionsFailed);
            System.out.println("Filtered sequence count (interactions fails): " + filteredSequenceCountInteractionsFailed);
            System.out.println("Average filtered joint-locations spread values:");
            System.out.println("  X: " + (avgJointSpreadsFiltered[0] / filteredSequenceCount));
            System.out.println("  Y: " + (avgJointSpreadsFiltered[1] / filteredSequenceCount));
            System.out.println("  X/Y: " + (avgJointSpreadsFiltered[2] / filteredSequenceCount));
        }
    }

    private static class SequenceProp {
        String subjectId;
        List<ObjectMocapPose> poses = new ArrayList<>();
        SequenceMocap<?> sequence;
        SequenceMocap<?> sequenceConverted;
        float jointSpreadX;
        float jointSpreadY;
        float jointSpreadRatio;
        int jointCountStatusNotTracked = 0;
        int jointCountStatusAll = 0;
    }
}
