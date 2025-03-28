package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectMotionWord extends LocalAbstractObject implements BinarySerializable {

    // class id for serialization
    private static final long serialVersionUID = 1L;
    // IDs of clusters to which this motion word belongs
    protected long[] data;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link ObjectMotionWord}.
     *
     * @param data the data content of the new object
     */
    public ObjectMotionWord(long[] data) {
        this.data = data;
    }

    /**
     * Creates a new instance of {@link ObjectMotionWord}.
     *
     * @param locatorURI the locator URI for the new object
     * @param data the data content of the new object
     */
    public ObjectMotionWord(String locatorURI, long[] data) {
        super(locatorURI);
        this.data = data;
    }

    /**
     * Creates a new instance of {@link ObjectMotionWord} from text stream.
     *
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the
     * stream
     */
    public ObjectMotionWord(BufferedReader stream) throws EOFException, IOException {
        String line = readObjectComments(stream);
        String[] IDs = line.trim().split("[, ]+");
        this.data = new long[IDs.length];
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] = Long.parseLong(IDs[i]);
        }
    }

    //****************** Methods ******************//
    /**
     * Returns true if the specified motion word matches with this motion word.
     *
     * @param obj specified motion word
     * @return true if the specified motion word matches with this motion word
     */
    public boolean match(ObjectMotionWord obj) {
        return !(getDistance(obj) == 1f);
    }

    //****************** Implemented class LocalAbstractObject ******************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject lao, float f) {
        return (dataEquals(lao)) ? 0f : 1f;
    }

    @Override
    public int getSize() {
        return data.length * Long.SIZE / 8;
    }

    @Override
    public boolean dataEquals(Object o) {
        long[] objData = ((ObjectMotionWord) o).data;
        for (int i = 0; i < data.length; i++) {
            if (data[i] != objData[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int dataHashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    protected void writeData(OutputStream out) throws IOException {
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                out.write(",".getBytes());
            }
            out.write(Long.toString(data[i]).getBytes());
        }
        out.write("\n".getBytes());
    }

    //****************** Implemented interface BinarySerializable ******************//
    /**
     * Creates a new instance of {@link ObjectMotionWord} loaded from binary
     * input buffer.
     *
     * @param input the buffer to read the ObjectBitVectorHamming from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectMotionWord(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.data = serializator.readLongArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator)
                + serializator.write(output, this.data);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator)
                + serializator.getBinarySize(this.data);
    }
}
