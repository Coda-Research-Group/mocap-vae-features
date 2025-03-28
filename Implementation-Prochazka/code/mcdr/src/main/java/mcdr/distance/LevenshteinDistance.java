package mcdr.distance;

import messif.objects.DistanceFunction;
import smf.sequences.DistanceAllowsNonEquilength;
import smf.sequences.Sequence;

/**
 * @author David Procházka
 */
public class LevenshteinDistance<T> implements DistanceFunction<Sequence<T>>, DistanceAllowsNonEquilength {

    private static final float DELETE_WEIGHT = 1.0f;
    private static final float INSERT_WEIGHT = 1.0f;

    @Override
    public float getDistance(Sequence<T> lhs, Sequence<T> rhs) {
        float[][] opt = new float[lhs.getSequenceLength() + 1][rhs.getSequenceLength() + 1];

        opt[0][0] = 0;

        for (int i = 1; i <= lhs.getSequenceLength(); i++) {
            opt[i][0] = i;
        }

        for (int j = 1; j <= rhs.getSequenceLength(); j++) {
            opt[0][j] = j;
        }

        for (int i = 1; i <= lhs.getSequenceLength(); i++) {
            for (int j = 1; j <= rhs.getSequenceLength(); j++) {
                opt[i][j] = Math.min(
                        opt[i - 1][j - 1] + lhs.getPiecewiseDist(i - 1, rhs, j - 1),
                        Math.min(
                                opt[i - 1][j] + DELETE_WEIGHT,
                                opt[i][j - 1] + INSERT_WEIGHT
                        )
                );
            }
        }

        return opt[lhs.getSequenceLength()][rhs.getSequenceLength()];
    }

    @Override
    public Class<? extends Sequence<T>> getDistanceObjectClass() {
        return (Class) Sequence.class;
    }

    @Override
    public String toString() {
        return "LevenshteinDistance";
    }
}
