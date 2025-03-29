package mcdr.objects.impl;

import mcdr.objects.ObjectMocapPose;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

import static mcdr.objects.ObjectMocapPose.parseFloatArray;
import static mcdr.objects.ObjectMocapPose.parseFloatArray2d;

/**
 *
 * @author Tomas Drkos, FI MU Brno, Czech Republic
 */
public class ObjectSegmentCodeList extends LocalAbstractObject implements BinarySerializable {

    // class serial number for serialization
    private static final long serialVersionUID = 681518377921342678L;
    private float[] quantizedDimensions;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectSegmentCodeList}.
     *
     * @param quantizedDimensions captured floats of quantized data
     */
    public ObjectSegmentCodeList(float[] quantizedDimensions) {
        this.quantizedDimensions = quantizedDimensions;
    }

    /**
     * Creates a new instance of {@link ObjectSegmentCodeList} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectSegmentCodeList(BufferedReader stream) throws IOException {

        // Reads comments
        String line = readObjectComments(stream);

        // Reads quantized vectors
        this.quantizedDimensions = parseFloatArray(line);
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's coordinates
        float[] objCoords = ((ObjectSegmentCodeList) obj).getQuantizedDimensions();
        float[] coords = getQuantizedDimensions();

        // L2 distance computation (Euklidian distance)
        float rtv = 0f;
        for (int j = 0; j < coords.length; j++) {
            rtv += (float) Math.pow(coords[j] - objCoords[j], 2f);

        }
        return (float) Math.sqrt(rtv);
    }

    @Override
    public float getMaxDistance() {
        return Float.MAX_VALUE;
    }

    @Override
    public int getSize() {
        return 0;
    }

    public float getDistanceNoRootSquare(LocalAbstractObject obj) {
        // Get access to the other object's coordinates
        float[] objCoords = ((ObjectSegmentCodeList) obj).getQuantizedDimensions();
        float[] coords = getQuantizedDimensions();

        // L2 distance computation (Euklidian distance)
        float rtv = 0f;
        for (int j = 0; j < coords.length; j++) {
            rtv += (float) Math.pow(coords[j] - objCoords[j], 2f);

        }
        return rtv;
    }

    @Override
    public boolean dataEquals(Object o) {
        if (o == this)
            return true;
        if (o == null || !(o instanceof ObjectSegmentCodeList))
            return false;
        
        float[] coordsM = getQuantizedDimensions();
        float[] coordsO = ((ObjectSegmentCodeList)o).getQuantizedDimensions();
        if (coordsM.length != coordsO.length)
            return false;
        return Arrays.equals(coordsM, coordsO);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(getQuantizedDimensions());
    }

    @Override
    protected void writeData(OutputStream outputStream) throws IOException {

    }

    @Override
    public int getBinarySize(BinarySerializator binarySerializator) {
        return 0;
    }

    @Override
    public int binarySerialize(BinaryOutput binaryOutput, BinarySerializator binarySerializator) throws IOException {
        return 0;
    }

    public float[] getQuantizedDimensions() {
        return quantizedDimensions;
    }

    public static float[] parseFloatArray(String line) {
        if (line.isEmpty()) {
            return new float[0];
        }
        String[] arrayStr = line.trim().split("; ");
        float[] floatArray = new float[arrayStr.length];
        for (int i = 0; i < arrayStr.length; i++) {
            floatArray[i] = Float.parseFloat(arrayStr[i]);
        }
        return floatArray;
    }

    public ObjectSegmentCodeList duplicate() {
        return new ObjectSegmentCodeList(quantizedDimensions);
    }
}
