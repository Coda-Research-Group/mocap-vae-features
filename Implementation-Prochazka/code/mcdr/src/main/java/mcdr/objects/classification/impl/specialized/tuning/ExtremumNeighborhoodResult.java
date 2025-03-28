package mcdr.objects.classification.impl.specialized.tuning;

import mcdr.objects.impl.Extremum;
import mcdr.objects.impl.ObjectBodyPart;

/**
 * The result of an extremum neighborhood classification method.
 * 
 * @author David Procházka
 */
record ExtremumNeighborhoodResult(
        ObjectBodyPart bodyPart,
        int jointIndex,
        int axisIndex,
        Extremum extremum,
        float percentage,
        float performance
) {
}
