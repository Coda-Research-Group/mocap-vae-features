package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import mcdr.objects.ObjectMocapPose;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMocapPoseCoordsL2 extends ObjectMocapPose {

    // class serial number for serialization
    private static final long serialVersionUID = 681518377921342678L;
    
    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseCoordsL2}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     */
    public ObjectMocapPoseCoordsL2(float[][] jointCoordinates) {
        super(jointCoordinates);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseCoordsL2} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseCoordsL2(BufferedReader stream) throws IOException {
        super(stream);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's coordinates
        float[][] objCoords = ((ObjectMocapPose) obj).getJointCoordinates();
        float[][] coords = getJointCoordinates();

        // L2 distance computation
        float rtv = 0f;
        for (int j = 0; j < coords.length; j++) {
            for (int i = 0; i < 3; i++) {
                rtv += Math.pow(coords[j][i] - objCoords[j][i], 2f);
            }
        }
        return (float) Math.sqrt(rtv);
    }

    @Override
    public float getMaxDistance() {
        return Float.MAX_VALUE;
    }
    
    public float getDistanceNoRootSquare(LocalAbstractObject obj) {
        // Get access to the other object's coordinates
        float[][] objCoords = ((ObjectMocapPose) obj).getJointCoordinates();
        float[][] coords = getJointCoordinates();

        // L2 distance computation
        float rtv = 0f;
        for (int j = 0; j < coords.length; j++) {
            for (int i = 0; i < 3; i++) {
                float diff = coords[j][i] - objCoords[j][i];
                rtv += diff * diff;
                //rtv += Math.pow(coords[j][i] - objCoords[j][i], 2f);
            }
        }
        return rtv;
    }

    @Override
    public boolean dataEquals(Object o) {
        if (o == this)
            return true;
        if (o == null || !(o instanceof ObjectMocapPoseCoordsL2))
            return false;
        
        float[][] coordsM = getJointCoordinates();
        float[][] coordsO = ((ObjectMocapPoseCoordsL2)o).getJointCoordinates();
        if (coordsM.length != coordsO.length)
            return false;
        for (int i = 0; i < coordsM.length; i++) {
            if (!Arrays.equals(coordsM[i], coordsO[i]))
                return false;
        }
        return true;
    }

    @Override
    public int dataHashCode() {
        float[][] coords = getJointCoordinates();        
        int result = 1;
        for (float[] joint : coords)
            result = 31 * result + Arrays.hashCode(joint);
        return result;
    }
}
