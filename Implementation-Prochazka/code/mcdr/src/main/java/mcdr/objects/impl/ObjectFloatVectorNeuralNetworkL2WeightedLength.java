package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import static mcdr.test.utils.ObjectMgmt.parseObjectLength;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectFloatVectorNeuralNetworkL2WeightedLength extends ObjectFloatVectorNeuralNetworkL2 {

    //************ Constructors ************//
    /**
     * Creates a new instance of
     * {@link ObjectFloatVectorNeuralNetworkL2WeightedLength} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectFloatVectorNeuralNetworkL2WeightedLength(BufferedReader stream) throws IOException {
        super(stream);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        int length = parseObjectLength(this);
        int objLength = parseObjectLength(obj);
        return super.getDistanceImpl(obj, distThreshold) * (2f - (float) Math.pow((float) Math.min(length, objLength) / Math.max(length, objLength), 0.5f));
    }
}
