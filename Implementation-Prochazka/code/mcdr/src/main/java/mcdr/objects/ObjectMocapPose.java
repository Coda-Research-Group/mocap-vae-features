package mcdr.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class ObjectMocapPose extends LocalAbstractObject implements BinarySerializable {

    //************ Attributes ************//
    // class id for serialization
    private static final long serialVersionUID = 1L;
    // identification (position) of frame at which this pose occurs
    protected int frameNo = -1;
    // captured XYZ coordinates of joints
    protected final float[][] jointCoordinates;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPose}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     */
    public ObjectMocapPose(float[][] jointCoordinates) {
        this.jointCoordinates = new float[jointCoordinates.length][3];
        for (int i = 0; i < jointCoordinates.length; i++) {
            System.arraycopy(jointCoordinates[i], 0, this.jointCoordinates[i], 0, 3);
        }
    }

    /**
     * Creates a new instance of {@link ObjectMocapPose} loaded from stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPose(BufferedReader stream) throws IOException {

        // Reads comments
        String line = readObjectComments(stream);

        // Reads joint coordinates
        this.jointCoordinates = parseFloatArray2d(line);
    }

    //************ Methods ************//
    /**
     * Returns the identification of parent sequence of this pose.
     *
     * @return the identification of parent sequence of this pose
     */
    public String getSequenceId() {
        return getLocatorURI();
    }

    /**
     * Returns the position (frame) of this pose within the parent sequence.
     *
     * @return the position (frame) of this pose within the parent sequence
     */
    public int getFrameNo() {
        return frameNo;
    }

    /**
     * Sets the position (frame) of this pose within the parent sequence.
     *
     * @param frameNo the position (frame) of this pose within the parent
     * sequence
     */
    public void setFrameNo(int frameNo) {
        this.frameNo = frameNo;
    }

    /**
     * Returns XYZ coordinates of all landmarks.
     *
     * @return XYZ coordinates of all landmarks
     */
    public float[][] getJointCoordinates() {
        return jointCoordinates;
    }

    /**
     * Parses a float array from the string.
     *
     * @param line string from which the float array is parsed
     * @return float array parsed from the string
     */
    public static float[] parseFloatArray(String line) {
        if (line.isEmpty()) {
            return new float[0];
        }
        String[] arrayStr = line.trim().split(", ");
        float[] floatArray = new float[arrayStr.length];
        for (int i = 0; i < arrayStr.length; i++) {
            if (arrayStr[i].trim().isEmpty() || arrayStr[i].trim().equals("NaN") || arrayStr[i].trim().equals("null")) {
                floatArray[i] = Float.NaN;
            } else {
                floatArray[i] = Float.parseFloat(arrayStr[i]);
            }
        }
        return floatArray;
    }

    /**
     * Parses a two-dimensional float array from the string.
     *
     * @param line string from which the float array is parsed
     * @return two-dimensional float array parsed from the string
     */
    public static float[][] parseFloatArray2d(String line) {
        if (line.isEmpty()) {
            return new float[0][0];
        }
        String[] arrayStr = line.trim().split("; ");
        float[][] floatArray = new float[arrayStr.length][];
        for (int i = 0; i < arrayStr.length; i++) {
            floatArray[i] = parseFloatArray(arrayStr[i]);
        }
        return floatArray;
    }

    /**
     * Writes a float array into the output stream.
     *
     * @param stream stream to which the float array is written
     * @param floatArray input float array
     * @throws IOException when an error appears during writing data to the
     * given stream
     */
    public static void writeFloatArray(OutputStream stream, float[] floatArray) throws IOException {
        for (int i = 0; i < floatArray.length; i++) {
            if (i > 0) {
                stream.write(", ".getBytes());
            }
            stream.write(String.valueOf(floatArray[i]).getBytes());
        }
    }

    /**
     * Writes a two-dimensional float array into the output stream.
     *
     * @param stream stream to which the float array is written
     * @param floatArray input float array
     * @throws IOException when an error appears during writing data to the
     * given stream
     */
    public static void writeFloatArray(OutputStream stream, float[][] floatArray) throws IOException {
        for (int i = 0; i < floatArray.length; i++) {
            if (i > 0) {
                stream.write("; ".getBytes());
            }
            writeFloatArray(stream, floatArray[i]);
        }
    }

    /**
     * Creates the clone of this pose.
     *
     * @return clonned pose
     */
    public ObjectMocapPose duplicate() {
        ObjectMocapPose pose;
        try {
            pose = getClass().getConstructor(float[][].class).newInstance(new Object[]{jointCoordinates.clone()});
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
        pose.setFrameNo(frameNo);
        return pose;
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    public int getSize() {
        return Integer.SIZE / 8 + (jointCoordinates.length * 3) * Float.SIZE / 8;
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {

        // Joint coordinates
        writeFloatArray(stream, jointCoordinates);
        stream.write('\n');

    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link ObjectMocapPose} loaded from the binary
     * input buffer.
     *
     * @param input buffer from which the pose is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected ObjectMocapPose(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.frameNo = serializator.readInt(input);
        this.jointCoordinates = new float[serializator.readInt(input)][3];
        for (int i = 0; i < jointCoordinates.length; i++) {
            this.jointCoordinates[i] = serializator.readFloatArray(input);
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int rtv = super.binarySerialize(output, serializator);
        rtv += serializator.write(output, frameNo);
        rtv += serializator.write(output, jointCoordinates.length);
        for (float[] jointCoordinate : jointCoordinates) {
            rtv += serializator.write(output, jointCoordinate);
        }
        return rtv;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int rtv = super.getBinarySize(serializator);
        rtv += serializator.getBinarySize(frameNo);
        rtv += serializator.getBinarySize(jointCoordinates.length);
        for (float[] jointCoordinate : jointCoordinates) {
            rtv += serializator.getBinarySize(jointCoordinate);
        }
        return rtv;
    }

    //************ Overrided class Object ************//
    @Override
    public String toString() {
        return super.toString() + " (frame: " + frameNo + ")";
    }
}
