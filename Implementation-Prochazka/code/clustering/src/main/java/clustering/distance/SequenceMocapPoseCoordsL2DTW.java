package clustering.distance;

import clustering.Joint;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries.AbstractEditDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Interprets a single ELKI vector of floats as a sequence of poses and calls respective distance functions from
 * MCDR framework, compared by DTW. Each pose is a list of 3-D coordinates of joints, compared by L2.
 *
 * Enables the user to specify which joints should be used in the clustering.
 * Originally {@code elki.messif.SequenceMocapPoseCoordsL2DTW} in
 * <a href="https://gitlab.fi.muni.cz/disa/public/elki-messif">https://gitlab.fi.muni.cz/disa/public/elki-messif</a>.
 *
 * @author Derived from the code by Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMocapPoseCoordsL2DTW extends AbstractEditDistanceFunction {

    private static final int JOINT_COUNT = 31;
    private static final int JOINT_DIM = 3;
    private static final int POSE_DIM = JOINT_COUNT * JOINT_DIM;

    /**
     * Indicates if an index should be used for clustering (true) or not (false).
     * These indices are used in {@link #deltaPose(NumberVector, int, NumberVector, int)} to filter which
     * corresponding values are used during distance computations.
     */
    private final BitSet jointIndexMask;

    public SequenceMocapPoseCoordsL2DTW(BitSet jointIndexMask) {
        super(Double.POSITIVE_INFINITY);

        this.jointIndexMask = jointIndexMask;
    }

    /**
     * Compute the delta of two poses -- square of L2.
     *
     * @return sum of squared differences
     */
    private double deltaPose(NumberVector v1, int poseStartV1, NumberVector v2, int poseStartV2) {
        // L2 distance computation
        double sum = 0.0;
        int stop = poseStartV1 + POSE_DIM;
        int index = 0;

        while (poseStartV1 < stop) {
            if (jointIndexMask.get(index)) {
                double diff = v1.doubleValue(poseStartV1) - v2.doubleValue(poseStartV2);
                sum += diff * diff;
            }

            poseStartV1++;
            poseStartV2++;
            index++;
        }

        return FastMath.sqrt(sum);
    }

    @Override
    public double distance(NumberVector v1, NumberVector v2) {
        // Number of poses is in the first coordinate, since it can vary
        int poses1 = v1.intValue(0);
        int poses2 = v2.intValue(0);

        // Dimensionality, and last valid value in second vector:
        int dim1 = v1.getDimensionality(), dim2 = v2.getDimensionality();
        if (dim1 != poses1 * POSE_DIM + 1 || dim2 != poses2 * POSE_DIM + 1) {
            throw new IllegalArgumentException("Incorrect data -- dimensionality of vector is wrong.");
        }

        // Current and previous columns of the matrix
        double[] buf = new double[poses2 << 1];
        Arrays.fill(buf, Double.POSITIVE_INFINITY);

        // Fill first row:
        firstRow(buf, v1, v2, poses2);

        // Active buffer offsets (cur = read, nxt = write)
        int cur = 0, nxt = poses2;
        // Fill remaining rows:
        int startI = POSE_DIM + 1;
        for (int i = 1; i < poses1; i++) {
            int startJ = 1;
            for (int j = 0; j < poses2; j++) {
                // Value in previous row (must exist, may be infinite):
                double min = buf[cur + j];
                // Diagonal:
                if (j > 0) {
                    double pij = buf[cur + j - 1];
                    min = (pij < min) ? pij : min;
                    // Previous in same row:
                    if (j > 0) {
                        double pj = buf[nxt + j - 1];
                        min = (pj < min) ? pj : min;
                    }
                }
                // Write:
                buf[nxt + j] = min + deltaPose(v1, startI, v2, startJ);
                startJ += POSE_DIM;
            }
            startI += POSE_DIM;
            // Swap buffer positions:
            cur = poses2 - cur;
            nxt = poses2 - nxt;
        }

        return buf[cur + poses2 - 1];
    }

    /**
     * Fill the first row.
     *
     * @param buf  Buffer
     * @param v1   First vector
     * @param v2   Second vector
     * @param dim2 Dimensionality of second
     */
    private void firstRow(double[] buf, NumberVector v1, NumberVector v2, int dim2) {
        // First cell:
        buf[0] = deltaPose(v1, 1, v2, 1);
        // Fill remaining part of buffer:
        for (int j = 1, start = POSE_DIM + 1; j < dim2; j++, start += POSE_DIM) {
            buf[j] = buf[j - 1] + deltaPose(v1, 1, v2, start);
        }
    }

    @Override
    public <T extends NumberVector> DistanceQuery<T> instantiate(Relation<T> relation) {
        return super.instantiate(relation);
    }

    public static class Parameterizer extends AbstractParameterizer {

        /**
         * Option ID for joint ids.
         * The user specifies which joint ids should be used for clustering.
         * Conversion to {@link BitSet} takes place to speed up the processing.
         * See {@link SequenceMocapPoseCoordsL2DTW#jointIndexMask}.
         */
        private static final OptionID USED_JOINT_IDS = new OptionID(
                "clustering.distance.SequenceMocapPoseCoordsL2DTW.usedJointIds",
                "Specifies which joint ids (in [1, 31] range) should be used for clustering."
        );

        private int[] usedJointIds;

        @Override
        protected SequenceMocapPoseCoordsL2DTW makeInstance() {
            var jointIndicesMask = new BitSet(POSE_DIM);

            // If no id was specified use all joint ids.
            if (usedJointIds == null) {
                jointIndicesMask.set(0, POSE_DIM);
            }

            // If some ids were specified, disable everything and enable only those specified.
            if (usedJointIds != null) {
                jointIndicesMask.clear(0, POSE_DIM);

                for (int jointId : usedJointIds) {
                    int jointIndex = Joint.getJointIndex(jointId);

                    // Enable every 3D coordinate of jointIndex
                    jointIndicesMask.set(jointIndex * JOINT_DIM);
                    jointIndicesMask.set(jointIndex * JOINT_DIM + 1);
                    jointIndicesMask.set(jointIndex * JOINT_DIM + 2);
                }
            }

            return new SequenceMocapPoseCoordsL2DTW(jointIndicesMask);
        }

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);

            var usedJointIds = new IntListParameter(USED_JOINT_IDS, true);
            if (config.grab(usedJointIds)) {
                this.usedJointIds = usedJointIds.getValue();
            }
        }
    }
}
