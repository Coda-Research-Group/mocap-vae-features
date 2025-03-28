package mcdr.distance;

import messif.objects.DistanceFunction;
import smf.sequences.DistanceAllowsNonEquilength;
import smf.sequences.Sequence;

/**
 * @author David Procházka
 */
public class DTWDistance<T> implements DistanceFunction<Sequence<T>>, DistanceAllowsNonEquilength {

    @Override
    public float getDistance(Sequence<T> lhs, Sequence<T> rhs) {
        float[][] opt = new float[lhs.getSequenceLength()][rhs.getSequenceLength()];

        opt[0][0] = lhs.getPiecewiseDist(0, rhs, 0);

        for (int i = 1; i < lhs.getSequenceLength(); i++) {
            opt[i][0] = opt[i - 1][0] + lhs.getPiecewiseDist(i, rhs, 0);
        }

        for (int j = 1; j < rhs.getSequenceLength(); j++) {
            opt[0][j] = opt[0][j - 1] + lhs.getPiecewiseDist(0, rhs, j);
        }

        for (int i = 1; i < lhs.getSequenceLength(); i++) {
            for (int j = 1; j < rhs.getSequenceLength(); j++) {
                opt[i][j] = lhs.getPiecewiseDist(i, rhs, j) +
                        Math.min(
                                opt[i - 1][j],
                                Math.min(
                                        opt[i][j - 1],
                                        opt[i - 1][j - 1]
                                )
                        );
            }
        }

        return opt[lhs.getSequenceLength() - 1][rhs.getSequenceLength() - 1];
    }

    @Override
    public Class<? extends Sequence<T>> getDistanceObjectClass() {
        return (Class) Sequence.class;
    }

    @Override
    public String toString() {
        return "DTWDistance";
    }
}
