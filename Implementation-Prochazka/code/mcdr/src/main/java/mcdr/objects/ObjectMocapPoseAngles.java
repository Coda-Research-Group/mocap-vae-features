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
public abstract class ObjectMocapPoseAngles extends ObjectMocapPose {

    //************ Attributes ************//
    // angle descriptors extracted from the captured joint coordinates
    protected float[] angles;
    // weights of individual angles
    protected static final float[] ANGLES_WEIGHTS = new float[]{
        // left leg
        2.0f, // lhipjoint1
        2.0f, // lhipjoint2
        1.0f, // lfemur1
        0.0f, // ltibia1
        // right leg
        2.0f, // rhipjoint1
        2.0f, // rhipjoint2
        1.0f, // rfemur1
        0.0f, // rtibia1
        // body
        2.0f, // lowerback1
        2.0f, // lowerback2
        2.0f, // upperback1
        2.0f, // upperback2
        1.0f, // thorax1
        1.0f, // thorax2
        0.5f, // lowerneck1
        0.5f, // lowerneck2
        0.5f, // upperneck1
        0.5f, // upperneck2
        0.0f, // head1
        0.0f, // head2
        // left arm
        2.0f, // lclavicle1
        2.0f, // lclavicle2
        1.0f, // lhumerus1
        0.0f, // lwrist1
        // right arm
        2.0f, // rclavicle1
        2.0f, // rclavicle2
        1.0f, // rhumerus1
        0.0f // rwrist1
    };

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseAngles}.
     *
     * @param jointCoordinates captured XYZ coordinates of joints
     * @param angles angle descriptors extracted from the captured joint
     * coordinates
     */
    public ObjectMocapPoseAngles(float[][] jointCoordinates, float[] angles) {
        super(jointCoordinates);
        this.angles = new float[angles.length];
        System.arraycopy(angles, 0, this.angles, 0, angles.length);
    }

    /**
     * Creates a new instance of {@link ObjectMocapPoseAngles} loaded from
     * stream.
     *
     * @param stream stream from which the pose is read
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    public ObjectMocapPoseAngles(BufferedReader stream) throws IOException {
        super(stream);

        // Reads angles
        angles = parseFloatArray(stream.readLine());
    }

    //************ Methods ************//
    /**
     * Returns the angle descriptor.
     *
     * @return the angle descriptor
     */
    public float[] getAngles() {
        return angles;
    }

    //************ Overrided class ObjectMocapPose ************//
    @Override
    public ObjectMocapPose duplicate() {
        ObjectMocapPoseAngles pose;
        try {
            pose = getClass().getConstructor(float[][].class, float[].class).newInstance(new Object[]{jointCoordinates.clone(), angles.clone()});
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
        pose.setFrameNo(frameNo);
        return pose;
    }

    //************ Overrided class LocalAbstractObject ************//
    @Override
    public int getSize() {
        return super.getSize() + (angles.length * Float.SIZE / 8);
    }

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectMocapPoseAngles)) {
            return false;
        }
        return Arrays.equals(((ObjectMocapPoseAngles) obj).angles, angles);
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(angles);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);

        // Angles
        writeFloatArray(stream, angles);
        stream.write('\n');
    }

    //************ Implemented interface BinarySerializable ************//
    /**
     * Creates a new instance of {@link ObjectMocapPoseAngles} loaded from the
     * binary input buffer.
     *
     * @param input buffer from which the pose is read
     * @param serializator the serializator used to read objects
     * @throws IOException when an error appears during reading from the given
     * stream (e.g., when EOF of the given stream is reached)
     */
    protected ObjectMocapPoseAngles(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        angles = serializator.readFloatArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int rtv = super.binarySerialize(output, serializator);
        rtv += serializator.write(output, angles);
        return rtv;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int rtv = super.getBinarySize(serializator);
        rtv += serializator.getBinarySize(angles);
        return rtv;
    }
}
