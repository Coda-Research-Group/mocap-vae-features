package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.classification.impl.ClassificationResult;
import mcdr.objects.classification.impl.ObjectMultiCategoryClassifier;
import mcdr.objects.impl.Extremum;
import mcdr.objects.impl.ObjectBodyPart;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2Filtered;
import mcdr.objects.impl.ObjectMotionWordComposite;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTWFiltered;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning;
import mcdr.sequence.impl.SequenceMotionWordsCompositeAutoTuning;
import mcdr.test.utils.ObjectMgmt;
import messif.objects.classification.ClassificationException;
import messif.objects.impl.MetaObjectArray;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static mcdr.objects.impl.BodyPartConfiguration.JOINT_DIM;

/**
 * Encapsulates the selection of parameters for the extremum neighborhood classification method.
 * 
 * @author David Procházka
 */
final class ExtremumNeighborhoodSelector {

    private static final List<Float> PERCENTAGES = List.of(0.1F, 0.2F, 0.3F, 0.4F, 0.5F, 0.6F, 0.7F, 0.8F, 0.9F, 1.0F);
    private static final double PERFECT_CLASSIFICATION_PERFORMANCE_THRESHOLD = 99.99;

    private final ObjectMgmt dataMgmt;
    private final ObjectMgmt queryMgmt;
    private final ObjectMgmt originalActionMgmt;
    private final ObjectMultiCategoryClassifier classifier;
    private final int k;

    ExtremumNeighborhoodSelector(
            ObjectMgmt dataMgmt,
            ObjectMgmt queryMgmt,
            ObjectMgmt originalActionMgmt,
            ObjectMultiCategoryClassifier classifier,
            int k
    ) {
        this.dataMgmt = dataMgmt;
        this.queryMgmt = queryMgmt;
        this.originalActionMgmt = originalActionMgmt;
        this.classifier = classifier;
        this.k = k;
    }

    /**
     * Sets values of the static distance properties.
     *
     * @param originalActionMgmt action management
     * @param jointIndex         joint index
     * @param axisIndex          axis index
     * @param percentage         percentage
     * @param extremum           the type of extremum to use
     * @param bodyPart           body part
     */
    private static void setStaticDistanceProperties(ObjectMgmt originalActionMgmt, int jointIndex, int axisIndex, float percentage, Extremum extremum, ObjectBodyPart bodyPart) {
        SequenceMotionWordsCompositeAutoTuning.distanceFunction = new SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning<>();
        ObjectMocapPoseCoordsL2Filtered.jointIds = bodyPart.jointIds();

        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.originalActionMgmt = originalActionMgmt;
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.axisIndex = axisIndex;
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.jointIndex = jointIndex;
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.percentage = percentage;
        SequenceMocapPoseCoordsL2DTWFilteredExtremaNeighborhoodAutoTuning.extremum = extremum;
    }

    /**
     * Returns the PCA's principal components.
     *
     * @param matrix the matrix
     * @return the principal components
     */
    private static EigenDecomposition runPCA(RealMatrix matrix) {
        var covariance = new Covariance(matrix);
        var covarianceMatrix = covariance.getCovarianceMatrix();
        return new EigenDecomposition(covarianceMatrix);
    }

    /**
     * Fills in the matrix with action poses.
     *
     * @param matrix  the PCA matrix
     * @param actions the actions
     */
    private static void fillIn(RealMatrix matrix, Iterable<? extends SequenceMocapPoseCoordsL2DTWFiltered> actions) {
        int rowIndex = 0;

        for (var action : actions) {
            for (var pose : action.getObjects()) {
                for (int jointIndex = 0; jointIndex < ObjectMotionWordComposite.getJointCount(); jointIndex++) {
                    float[] joint = pose.getJointCoordinates()[jointIndex];

                    for (int axisIndex = 0; axisIndex < JOINT_DIM; axisIndex++) {
                        float value = joint[axisIndex];
                        int columnIndex = jointIndex * JOINT_DIM + axisIndex;

                        matrix.setEntry(rowIndex, columnIndex, value);
                    }
                }

                rowIndex++;
            }
        }
    }

    /**
     * Extracts the most significant pose-joint-axis index from the PCA's first component.
     *
     * @param principalComponents the PCA result
     * @return the most significant pose-joint-axis index
     */
    private static int extractPoseJoinAxisIndex(EigenDecomposition principalComponents) {
        return principalComponents
                .getEigenvector(0)
                .map(Math::abs)
                .getMaxIndex();
    }

    /**
     * Counts the number of poses in the list of actions.
     *
     * @param actions the list of actions
     * @return the total number of poses
     */
    private static int countPoses(List<SequenceMocapPoseCoordsL2DTWFiltered> actions) {
        return actions
                .stream()
                .mapToInt(MetaObjectArray::getObjectCount)
                .sum();
    }

    /**
     * Selects the best-performing extremum neighborhood result.
     *
     * @return the best-performing extremum neighborhood result
     * @throws ClassificationException if the classification fails
     */
    ExtremumNeighborhoodResult select() throws ClassificationException {
        var matrix = createPCAMatrix();
        var principalComponents = runPCA(matrix);

        int poseJointAxisIndex = extractPoseJoinAxisIndex(principalComponents);
        int jointIndex = poseJointAxisIndex / JOINT_DIM;
        int axisIndex = poseJointAxisIndex % JOINT_DIM;
        var bodyPart = ObjectMotionWordComposite.getBodyPart(jointIndex);

        var results = evaluate(jointIndex, axisIndex, bodyPart);

        return Collections.max(results, Comparator.comparing(ExtremumNeighborhoodResult::performance));
    }

    /**
     * Evaluates all joint-axis combinations (percentages + extrema) on the given body part.
     *
     * @param jointIndex the joint index
     * @param axisIndex  the axis index
     * @param bodyPart   the body part
     * @return the list of results
     * @throws ClassificationException if the classification fails
     */
    private List<ExtremumNeighborhoodResult> evaluate(int jointIndex, int axisIndex, ObjectBodyPart bodyPart) throws ClassificationException {
        var results = new ArrayList<ExtremumNeighborhoodResult>(PERCENTAGES.size() * Extremum.values().length);

        for (float percentage : PERCENTAGES) {
            for (var extremum : Extremum.values()) {
                var result = evaluate(jointIndex, axisIndex, bodyPart, percentage, extremum);

                if (result.performance() > PERFECT_CLASSIFICATION_PERFORMANCE_THRESHOLD) {
                    return List.of(result);
                }

                results.add(result);
            }
        }

        return results;
    }

    /**
     * Evaluate the performance of the given parameters.
     *
     * @param jointIndex the joint index
     * @param axisIndex  the axis index
     * @param bodyPart   the body part
     * @param percentage the percentage
     * @param extremum   the extremum
     * @return the result
     * @throws ClassificationException if the classification fails
     */
    private ExtremumNeighborhoodResult evaluate(int jointIndex, int axisIndex, ObjectBodyPart bodyPart, float percentage, Extremum extremum) throws ClassificationException {
        setStaticDistanceProperties(originalActionMgmt, jointIndex, axisIndex, percentage, extremum, bodyPart);

        var knnQueries = dataMgmt.executeKNNQueries(queryMgmt, k);
        ClassificationResult result = dataMgmt.evaluateClassificationWithClassificationResult(classifier, knnQueries);

        return new ExtremumNeighborhoodResult(
                bodyPart,
                jointIndex,
                axisIndex,
                extremum,
                percentage,
                result.performance()
        );
    }

    /**
     * Creates PCA matrix containing every pose from every action.
     * Each row corresponds to a single pose.
     *
     * @return PCA matrix containing every pose from every action
     */
    private RealMatrix createPCAMatrix() {
        var actions = getOriginalActions();

        int poseCount = countPoses(actions);
        int poseDimensionality = ObjectMotionWordComposite.getPoseDimensionality();

        var matrix = MatrixUtils.createRealMatrix(poseCount, poseDimensionality);

        fillIn(matrix, actions);

        return matrix;
    }

    /**
     * Retrieves a list of original actions.
     *
     * @return a list of original actions
     */
    private List<SequenceMocapPoseCoordsL2DTWFiltered> getOriginalActions() {
        return originalActionMgmt
                .getObjects()
                .stream()
                .map(SequenceMocapPoseCoordsL2DTWFiltered.class::cast)
                .toList();
    }
}
