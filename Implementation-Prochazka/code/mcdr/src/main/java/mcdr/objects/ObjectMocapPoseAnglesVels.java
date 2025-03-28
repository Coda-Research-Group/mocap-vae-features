package mcdr.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public abstract class ObjectMocapPoseAnglesVels extends ObjectMocapPoseAngles {

    //************ Attributes ************//
    // absolute velocities extracted for specific joints
    protected float[] velocities;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesVels}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     * @param angles angle descriptors extracted from the captured joint
     * coordinates
     * @param velocities absolute velocities extracted for specific joints
     */
    public ObjectMocapPoseAnglesVels(float[][] jointCoordinates, float[] angles, float[] velocities) {
        super(jointCoordinates, angles);
        this.velocities = new float[velocities.length];
        System.arraycopy(velocities, 0, this.velocities, 0, velocities.length);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesVels} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseAnglesVels(BufferedReader stream) throws IOException {
        super(stream);

        // Reads velocities
        velocities = parseFloatArray(stream.readLine());
    }

    //************ Methods ************//
    /**
     * Returns the velocity descriptor.
     *
     * @return the velocity descriptor
     */
    public float[] getVelocities() {
        return velocities;
    }

    //************ Overrided class ObjectMocapPose ************//
    @Override
    public ObjectMocapPose duplicate() {
        ObjectMocapPoseAnglesVels pose;
        try {
            pose = getClass().getConstructor(float[][].class, float[].class, float[].class).newInstance(new Object[]{jointCoordinates.clone(), angles.clone(), velocities.clone()});
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
        pose.setFrameNo(frameNo);
        return pose;
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    public int getSize() {
        return super.getSize() + (velocities.length * Float.SIZE / 8);
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectMocapPoseAnglesVels)) {
            return false;
        }
        return Arrays.equals(((ObjectMocapPoseAnglesVels) obj).velocities, velocities);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(velocities);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);

        // Velocities
        ObjectMocapPose.writeFloatArray(stream, velocities);
        stream.write('\n');
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseAnglesVels} loaded from
     * the binary input buffer.
     *
     * @param input buffer from which the pose is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected ObjectMocapPoseAnglesVels(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        velocities = serializator.readFloatArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int rtv = super.binarySerialize(output, serializator);
        rtv += serializator.write(output, velocities);
        return rtv;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int rtv = super.getBinarySize(serializator);
        rtv += serializator.getBinarySize(velocities);
        return rtv;
    }
}
