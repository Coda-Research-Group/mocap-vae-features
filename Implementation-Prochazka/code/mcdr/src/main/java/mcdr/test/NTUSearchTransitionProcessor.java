package mcdr.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import mcda.commons.constants.LandmarkConstant;
import mcdr.objects.ObjectMocapPose;
import mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor;
import static mcdr.objects.extraction.CaffeObjectMotionImageSocketExtractor.createSequenceConvertors;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import mcdr.preprocessing.transformation.SequenceMocapConvertor;
import mcdr.preprocessing.transformation.impl.NormalizationOfOrientationConvertor;
import mcdr.sequence.KinematicTree;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import mcdr.test.utils.ObjectCategoryMgmt;
import mcdr.test.utils.ObjectMgmt;
import mcdr.test.utils.SequenceMocapMgmt;
import messif.objects.LocalAbstractObject;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.pivotselection.KMeansPivotChooser;
import messif.utility.reflection.NoSuchInstantiatorException;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class NTUSearchTransitionProcessor {

    //************ Constants ************//
    // abandoning non-existing joints within the Kinect2 skeleton model with respect to the Vicon model
    public static final float[] KINECT2_JOINT_WEIGHTS;

    static {
        KINECT2_JOINT_WEIGHTS = new float[LandmarkConstant.LANDMARK_COUNT];
        Arrays.fill(KINECT2_JOINT_WEIGHTS, 1f);

        // Abandoning the Vicon joints that are not present in the Kinect2 model
        for (int jointToAbandon : SequenceNTUProcessor.MOCAP_KINECT2_SAMEJOINTS_MAP.keySet()) {
            KINECT2_JOINT_WEIGHTS[LandmarkConstant.getLandmarkPos(jointToAbandon)] = 0f;
        }

        try {
            // PaSn normalization convertors
            sequenceConvertorsPaOaSn = createSequenceConvertors(SequenceMocapPoseCoordsL2DTW.class, 30, 30, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
        } catch (NoSuchInstantiatorException ex) {
            sequenceConvertorsPaOaSn = null;
            System.err.println(ex.toString());
        }
    }

    // PaSn normalization convertors
    public static List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertorsPaOaSn;

    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        final float clusteroidSimilarityThreshold = 8.5f;
        final float interpolationRatio = 2f;

        final Class<? extends SequenceMocap<?>> sequenceClass = SequenceMocapPoseCoordsL2DTW.class;
        final Class<? extends ObjectMocapPose> poseClass = ObjectMocapPoseCoordsL2.class;
//        final String dataSequenceFile = "c:/fi/datasets/mocap/NTU/objects-annotations_filtered0.9GT-coords_normPOS.data";
//        final String testSequenceFile = "c:/fi/datasets/mocap/NTU/objects-test_annotations_filtered0.9GT-coords_normPOS.data";
//        final String pivotPoseFile = "c:/fi/datasets/mocap/NTU/objects_pivot_poses_50k-coords_normPOS.data";
//        final String combinedSeqFile = "c:/fi/datasets/mocap/NTU/combinedSeq.data";
//        final String clusteroidPoseFile = "c:/fi/datasets/mocap/NTU/objects_clusteroid_poses_1k-coords_normPOS.data";
        final String dataSequenceFile = "d:/datasets/mocap/NTU/objects-annotations_filtered0.9GT-coords_normPOS.data";
        final String testSequenceFile = "d:/datasets/mocap/NTU/objects-test_annotations_filtered0.9GT-coords_normPS.data";
        final String pivotPoseFile = "d:/datasets/mocap/NTU/objects_pivot_poses_50k-coords_normPOS.data";
        final String combinedSeqFile = "d:/datasets/mocap/NTU/combinedSeq.data";
        final String clusteroidPoseFile = "d:/datasets/mocap/NTU/objects_clusteroid_poses_1k-coords_normPOS.data";
        final String[] ignoredCategoryIds = new String[]{"50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60"}; // NTU without interactions

        // Caffe extractor
        final CaffeObjectMotionImageSocketExtractor caffeObjectExtractor = CaffeObjectMotionImageSocketExtractor.createNTUExtractor(sequenceClass);

        System.out.println("Data sequences:");
        SequenceMocapMgmt dataSequenceMgmt = new SequenceMocapMgmt();
//        dataSequenceMgmt.read(sequenceClass, dataSequenceFile, ".*3179_16_1873_166.*", ignoredCategoryIds, null, null, true);
//        dataSequenceMgmt.read(sequenceClass, dataSequenceFile);

//        List<SequenceMocapConvertor<SequenceMocap<?>>> sequenceConvertors = createSequenceConvertors(sequenceClass, 30, 30, true, false, true, false, true, KinematicTree.BONE_LENGTH_MAP_HDM05, KinematicTree.KINEMATIC_TREE_VICON);
//        for (SequenceMocapConvertor<SequenceMocap<?>> convertor : sequenceConvertors) {
//            dataSequenceMgmt.convert(convertor);
//        }
//        ObjectMgmt randomSequences = new ObjectMgmt(new ObjectCategoryMgmt());
//        randomSequences.read(sequenceClass, dataSequenceFile);
//        randomSequences.storeRandomObjects(testSequenceFile, 3);
        SequenceMocapMgmt testSequenceMgmt = new SequenceMocapMgmt();
        testSequenceMgmt.read(sequenceClass, testSequenceFile);

//        selectTransitionPoses(dataSequenceMgmt, 50000, pivotPoseFile);
//        selectClusteroids(pivotPoseFile, poseClass, 1000, clusteroidPoseFile);
        List<ObjectMocapPose> graphPoseClusteroids = (List<ObjectMocapPose>) loadObjects(clusteroidPoseFile, poseClass);
        List<List<Integer>> clusteroidGraph = generateClusteroidGraph(graphPoseClusteroids, clusteroidSimilarityThreshold);
//        for (int i = 0; i <= 20; i++) {
//            computeGraphPath(graphPoseClusteroids, clusteroidGraph, graphPoseClusteroids.get(i), graphPoseClusteroids.get(i * 3));
//        }
//        List<Integer> clusteroidPath = computeGraphPath(graphPoseClusteroids, clusteroidGraph, graphPoseClusteroids.get(1), graphPoseClusteroids.get(3));

        List<SequenceMocap<?>> testSequences = new ArrayList<>(testSequenceMgmt.getSequences());
        SequenceMocap<?> combinedSeq = null;
        for (int i = 1; i < 50; i++) {
            SequenceMocap<?> nextAction = testSequences.get(i);
            combinedSeq = (combinedSeq == null) ? testSequences.get(0) : generateTransition(sequenceClass, poseClass, graphPoseClusteroids, clusteroidGraph, combinedSeq, nextAction, interpolationRatio);
        }
        FileOutputStream os = new FileOutputStream(combinedSeqFile);
        combinedSeq.write(os);
        os.close();
    }

    public static void selectTransitionPoses(SequenceMocapMgmt sequenceMgmt, int count, String file) throws IOException {
        List<ObjectMocapPose> allPoses = new ArrayList<>();
        for (SequenceMocap<?> seq : sequenceMgmt.getSequences()) {
            allPoses.addAll(seq.getObjects());
        }

        List<ObjectMocapPose> selectedPoses = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count && !allPoses.isEmpty(); i++) {
            int index = random.nextInt(allPoses.size());
            selectedPoses.add(allPoses.get(index));
            allPoses.remove(index);
        }

        FileOutputStream os = new FileOutputStream(file);
        for (ObjectMocapPose pose : selectedPoses) {
            pose.write(os);
        }
        os.close();
    }

    public static void selectClusteroids(String pivotFile, Class<? extends LocalAbstractObject> pivotClass, int k, String outputfile) throws IOException {
        StreamGenericAbstractObjectIterator objIterator = new StreamGenericAbstractObjectIterator<>(pivotClass, pivotFile);
        KMeansPivotChooser pivotChooser = new KMeansPivotChooser();
        pivotChooser.registerSampleProvider(objIterator);
        pivotChooser.selectPivot(k);

        System.out.println("Size of clusters: " + pivotChooser.getClusters().size());
        System.out.println("Size of 1st cluster: " + pivotChooser.getClusters().get(0).size());

        FileOutputStream os = new FileOutputStream(outputfile);
        for (int i = 0; i < k; i++) {
            LocalAbstractObject pivot = pivotChooser.getPivot(i);
//            pivot.chainDestroy();
            pivot.write(os, false);
        }
        os.close();
    }

    public static List<List<Integer>> generateClusteroidGraph(List<ObjectMocapPose> clusteroids, float neighborSimilarityLimit) throws IOException, NoSuchInstantiatorException {
        float maxDist = Float.MIN_VALUE;
        float minDist = Float.MAX_VALUE;
        float sumDist = 0f;
        float[][] dists = new float[clusteroids.size()][clusteroids.size()];
        for (int i = 0; i < clusteroids.size(); i++) {
            for (int j = i; j < clusteroids.size(); j++) {
                dists[i][j] = clusteroids.get(i).getDistance(clusteroids.get(j));
                dists[j][i] = dists[i][j];
                if (i != j) {
                    maxDist = Math.max(maxDist, dists[i][j]);
                    minDist = Math.min(minDist, dists[i][j]);
                    sumDist += dists[i][j];
                }
            }
        }
        System.out.println("maxDist: " + maxDist);
        System.out.println("minDist: " + minDist);
        System.out.println("avgDist: " + (sumDist / (clusteroids.size() * (clusteroids.size() - 1) / 2)));

        List<List<Integer>> clusteroidGraph = new ArrayList<>();
        for (int i = 0; i < clusteroids.size(); i++) {
            List<Integer> clusteroidNeighbors = new ArrayList<>();
            for (int j = 0; j < clusteroids.size(); j++) {
                if (i != j && dists[i][j] <= neighborSimilarityLimit) {
                    clusteroidNeighbors.add(j);
                }
            }
            clusteroidGraph.add(clusteroidNeighbors);
        }

        System.out.println("Graph:");
        System.out.println("  size: " + clusteroidGraph.size());
        int totalNeighborCountDuplicated = 0;
        List<Float> neighborCountsDuplicated = new ArrayList<>(clusteroidGraph.size());
        for (List<Integer> neighbors : clusteroidGraph) {
            totalNeighborCountDuplicated += neighbors.size();
            neighborCountsDuplicated.add((float) neighbors.size());
        }
        System.out.println("  edge count: " + (totalNeighborCountDuplicated / 2));
        System.out.println("  average degree: " + ((float) totalNeighborCountDuplicated / clusteroidGraph.size()));
        System.out.println("  mean degree: " + (ObjectMgmt.computeMeanValue(neighborCountsDuplicated)));

        System.out.println("Angles of graph poses: ");
        final NormalizationOfOrientationConvertor orientationNormalization = new NormalizationOfOrientationConvertor(SequenceMocapPoseCoordsL2DTW.class, true);
        for (ObjectMocapPose pose : clusteroids) {
            System.out.print(orientationNormalization.getHipsRotationAngle(pose) + ", ");
        }
        System.out.println();

        return clusteroidGraph;
    }

    public static List<Integer> computeGraphPath(List<ObjectMocapPose> clusteroids, List<List<Integer>> clusteroidGraph, ObjectMocapPose poseFrom, ObjectMocapPose poseTo) {
        Random random = new Random();
        int poseFromClusteroidIndex = getNearestPoses(clusteroids, poseFrom).first().clusterIndex;
        SortedSet<RankedClusteroid> poseToClusteroidSorted = getNearestPoses(clusteroids, poseTo);
        Iterator<RankedClusteroid> poseToClusteroidSortedIt = poseToClusteroidSorted.iterator();
        List<Integer> clusteroidPath = new ArrayList<>();

        boolean pathFound = false;
        int nearestToClusterIndex = -1;
        while (!pathFound && poseToClusteroidSortedIt.hasNext()) {
            nearestToClusterIndex++;
            int poseToClusteroidIndex = poseToClusteroidSortedIt.next().clusterIndex;
            int currentClusteroidIndex = poseFromClusteroidIndex;

            boolean[] visitedClusteroids = new boolean[clusteroids.size()];
            Arrays.fill(visitedClusteroids, false);
            visitedClusteroids[poseFromClusteroidIndex] = true;
            clusteroidPath.clear();
            clusteroidPath.add(poseFromClusteroidIndex);

            while (currentClusteroidIndex != poseToClusteroidIndex && !clusteroidPath.isEmpty()) {
                List<Integer> candClusteroids = new ArrayList<>();
                for (Integer candClusteroid : clusteroidGraph.get(currentClusteroidIndex)) {
                    if (!visitedClusteroids[candClusteroid]) {
                        candClusteroids.add(candClusteroid);
                    }
                }

                // Back-tracking
                if (candClusteroids.isEmpty()) {
                    clusteroidPath.remove(clusteroidPath.size() - 1);

                    if (clusteroidPath.isEmpty()) {
                        currentClusteroidIndex = -1;
                    } else {
                        currentClusteroidIndex = clusteroidPath.get(clusteroidPath.size() - 1);
                    }
                } else {
                    if (candClusteroids.contains(poseToClusteroidIndex)) {
                        currentClusteroidIndex = poseToClusteroidIndex;
                    } else {
                        currentClusteroidIndex = candClusteroids.get(random.nextInt(candClusteroids.size()));
                    }
                    clusteroidPath.add(currentClusteroidIndex);
                    visitedClusteroids[currentClusteroidIndex] = true;
                }
            }

            if (currentClusteroidIndex == poseToClusteroidIndex) {
                pathFound = true;
                if (nearestToClusterIndex > 0) {
                    clusteroidPath.add(poseToClusteroidSorted.first().clusterIndex);
                }
            }
        }

        // Stats
        System.out.print("<" + poseFromClusteroidIndex + ", " + poseToClusteroidSorted.first().clusterIndex + ">: ");
        if (pathFound) {
            System.out.println("(length=" + (clusteroidPath.size() - 1) + ", nearestToClusterIndex=" + nearestToClusterIndex + ") " + Arrays.toString(clusteroidPath.toArray()));
        } else {
            System.out.println("no path found!");
        }

        return clusteroidPath;
    }

    private static SortedSet<RankedClusteroid> getNearestPoses(List<ObjectMocapPose> clusteroids, LocalAbstractObject pose) {
        SortedSet<RankedClusteroid> rtv = new TreeSet<>();
        for (int i = 0; i < clusteroids.size(); i++) {
            float dist = pose.getDistance(clusteroids.get(i));
            rtv.add(new RankedClusteroid(i, dist));
        }
        return rtv;
    }

    public static List<? extends LocalAbstractObject> loadObjects(String objectFile, Class<? extends LocalAbstractObject> objectClass) throws IOException {
        StreamGenericAbstractObjectIterator objIterator = new StreamGenericAbstractObjectIterator<>(objectClass, objectFile);
        List<LocalAbstractObject> objects = new ArrayList<>();
        while (objIterator.hasNext()) {
            objects.add(objIterator.next());
        }
        return objects;
    }

    public static SequenceMocap<?> generateTransition(Class<? extends SequenceMocap<?>> sequenceClass, Class<? extends ObjectMocapPose> poseClass, List<ObjectMocapPose> clusteroids, List<List<Integer>> clusteroidGraph, SequenceMocap<?> action1, SequenceMocap<?> action2, float interpolationRatio) throws NoSuchInstantiatorException {
        ObjectMocapPose poseFromGraphGeneration = CaffeObjectMotionImageSocketExtractor.normalizeSequence(action1.duplicate(), sequenceConvertorsPaOaSn).getObject(action1.getSequenceLength() - 1);
        ObjectMocapPose poseToGraphGeneration = CaffeObjectMotionImageSocketExtractor.normalizeSequence(action2.duplicate(), sequenceConvertorsPaOaSn).getObject(0);

        ObjectMocapPose poseFrom = action1.getObject(action1.getSequenceLength() - 1);
        ObjectMocapPose poseTo = action2.getObject(0);

        final NormalizationOfOrientationConvertor orientationNormalization = new NormalizationOfOrientationConvertor(sequenceClass, true);
        float action1LastRotationAngle = orientationNormalization.getHipsRotationAngle(action1.getObject(action1.getSequenceLength() - 1));

        // Interpolated path
        List<Integer> graphPath = computeGraphPath(clusteroids, clusteroidGraph, poseFromGraphGeneration, poseToGraphGeneration);
        System.out.println("Intermediate clusteroid poses: " + (graphPath.size() - 2));
        List<ObjectMocapPose> interpolatedPoses = new ArrayList<>();
        if (graphPath.size() == 1) {
            interpolatedPoses.addAll(interpolatePoses(poseClass, poseFrom, poseTo, interpolationRatio));
        } else {
            for (int i = 1; i < graphPath.size(); i++) {
                ObjectMocapPose interpolationPoseFrom = (i == 1) ? poseFrom : clusteroids.get(graphPath.get(i - 1)).duplicate();
                ObjectMocapPose interpolationPoseTo = (i == graphPath.size() - 1) ? poseTo : clusteroids.get(graphPath.get(i)).duplicate();
                interpolatedPoses.addAll(interpolatePoses(poseClass, interpolationPoseFrom, interpolationPoseTo, interpolationRatio));
                if (i < graphPath.size() - 1) {
                    interpolatedPoses.add(clusteroids.get(graphPath.get(i)).duplicate());
                }
            }
        }

        System.out.println("Angles of interpolated poses: ");
        for (ObjectMocapPose o : interpolatedPoses) {
            System.out.print(orientationNormalization.getHipsRotationAngle(o) + ", ");
        }
        System.out.println();

        // Rotates interpolated poses
        orientationNormalization.rotateByAngle(interpolatedPoses, -action1LastRotationAngle);

        // Rotates action2 poses
        float interpolatedLastRotationAngle = orientationNormalization.getHipsRotationAngle(interpolatedPoses.get(interpolatedPoses.size() - 1));
        float action2FirstRotationAngle = orientationNormalization.getHipsRotationAngle(action2.getObject(0));
        List<ObjectMocapPose> action2Poses = new ArrayList<>();
        action2Poses.addAll(action2.getObjects());
        orientationNormalization.rotateByAngle(action2Poses, action2FirstRotationAngle - interpolatedLastRotationAngle);

        // Concatenated sequence
        List<ObjectMocapPose> poses = new ArrayList<>();
        poses.addAll(action1.getObjects());
        poses.addAll(interpolatedPoses);
        System.out.println("Action interpolated poses: " + action1.getSequenceLength() + "-" + (poses.size())
                + "; angles: [" + action1LastRotationAngle + ", " + orientationNormalization.getHipsRotationAngle(interpolatedPoses.get(0)) + "], ["
                + interpolatedLastRotationAngle + ", " + orientationNormalization.getHipsRotationAngle(action2Poses.get(0)) + "]"
        );
        poses.addAll(action2Poses);
        try {
            SequenceMocap<?> seq = sequenceClass.getConstructor(List.class).newInstance(new Object[]{poses});
//            seq.setObjectKey(new AbstractObjectKey(action1.getLocatorURI() + "-" + action2.getLocatorURI()));
            seq.setObjectKey(new AbstractObjectKey("XXX"));
            return seq;
        } catch (Exception e) {
            return null;
        }
    }

//    public static float getAngleDifference(float angle1, float angle2) {
//    }
    public static List<ObjectMocapPose> interpolatePoses(Class<? extends ObjectMocapPose> poseClass, ObjectMocapPose poseFrom, ObjectMocapPose poseTo, float interpolationRatio) {
        final int jointCount = poseFrom.getJointCoordinates().length;
        float poseDist = poseFrom.getDistance(poseTo);
        int poseCount = Math.round(poseDist / interpolationRatio);
        System.out.println("  Interpolated poses (dist=" + poseDist + "): " + poseCount);
        List<ObjectMocapPose> poses = new ArrayList<>(poseCount);

        // Differences between consecutive poses
        float[][] poseCoordsDiffs = new float[jointCount][3];
        for (int j = 0; j < jointCount; j++) {
            for (int i = 0; i < 3; i++) {
                poseCoordsDiffs[j][i] = (poseTo.getJointCoordinates()[j][i] - poseFrom.getJointCoordinates()[j][i]) / (poseCount + 1f);
            }
        }

        for (int f = 1; f <= poseCount; f++) {
            float[][] poseCoords = new float[jointCount][3];
            for (int j = 0; j < jointCount; j++) {
                for (int i = 0; i < 3; i++) {
                    poseCoords[j][i] = poseFrom.getJointCoordinates()[j][i] + f * poseCoordsDiffs[j][i];
                }
            }
            try {
                poses.add(poseClass.getConstructor(float[][].class).newInstance(new Object[]{poseCoords}));
            } catch (Exception e) {
            }
        }
        return poses;
    }

    public static class RankedClusteroid implements Comparable<RankedClusteroid> {

        final int clusterIndex;
        final float distance;

        public RankedClusteroid(int clusterIndex, float distance) {
            this.clusterIndex = clusterIndex;
            this.distance = distance;
        }

        @Override
        public int compareTo(RankedClusteroid o) {
            int rtv = Float.compare(distance, o.distance);
            if (rtv == 0) {
                rtv = Integer.compare(clusterIndex, o.clusterIndex);
            }
            return rtv;
        }
    }

}
