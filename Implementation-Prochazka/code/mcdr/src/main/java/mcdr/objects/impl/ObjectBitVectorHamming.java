package mcdr.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectBitVectorHamming extends LocalAbstractObject implements BinarySerializable {

    // class id for serialization
    private static final long serialVersionUID = 160707L;

    // bit array representing not-zero values of the original 4,096-dim vector from the neural network
    private final BitSet data;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link ObjectBitVectorHamming}.
     *
     * @param data the data content of the new object
     */
    public ObjectBitVectorHamming(BitSet data) {
        this.data = data;
    }

    /**
     * Creates a new instance of {@link ObjectBitVectorHamming}.
     *
     * @param locatorURI the locator URI for the new object
     * @param data the data content of the new object
     */
    public ObjectBitVectorHamming(String locatorURI, BitSet data) {
        super(locatorURI);
        this.data = data;
    }

    /**
     * Creates a new instance of {@link ObjectBitVectorHamming} from text
     * stream.
     *
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the
     * stream
     * @throws NumberFormatException if a line read from the stream does not
     * consist of '0' and '1' characters
     */
    public ObjectBitVectorHamming(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        String line = readObjectComments(stream);
        this.data = parseBitVector(line);
    }

    /**
     * Creates a new instance of {@link ObjectBitVectorHamming} from the
     * existing float array by transforming its values into '0' and '1' bits.
     * Firstly, all the array values are normalized according to the vector size
     * concept. Then, if the normalized value is less or equal to a given
     * threshold, the '0' bit is set. Otherwise, the '1' bit is set.
     *
     * @param values the float array whose values are transformed into bits
     * @param zeroBitThreshold value [0, 1] according to which the bit is set to
     * '0' or '1'
     */
    public ObjectBitVectorHamming(float[] values, float zeroBitThreshold) {
        this.data = new BitSet(values.length);
        double normValue = 0f;
        for (int i = 0; i < values.length; i++) {
            normValue += values[i] * values[i];
        }
        normValue = Math.sqrt(normValue);
        for (int i = 0; i < values.length; i++) {
            if (values[i] / normValue > zeroBitThreshold) {
                this.data.set(i);
            }
        }
    }

    //****************** Methods ******************//
    /**
     * Parses the bits from the '0'/'1' string.
     *
     * @param line string of '0'/'1' characters
     * @return the parsed bits
     * @throws EOFException
     */
    public static BitSet parseBitVector(String line) throws EOFException {
        if (line == null) {
            throw new EOFException();
        }
        BitSet rtv = new BitSet(line.length());
        for (int i = 0; i < line.length(); i++) {
            switch (line.charAt(i)) {
                case '0':
                    break;
                case '1':
                    rtv.set(i);
                    break;
                default:
                    throw new NumberFormatException("Not supported character '" + line.charAt(i) + "'! Supported characters are '0' and '1' only.");
            }
        }
        return rtv;
    }

    /**
     * Writes the bits into the stream.
     *
     * @param data bits to be written into the stream
     * @param stream stream to which the bits are written
     * @throws IOException
     */
    public static void writeBitVector(BitSet data, OutputStream stream) throws IOException {
        for (int i = 0; i < data.size(); i++) {
            stream.write(data.get(i) ? "1".getBytes() : "0".getBytes());
        }
        stream.write("\n".getBytes());
    }

    //****************** Implemented class LocalAbstractObject ******************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject lao, float f) {
        BitSet objData = ((ObjectBitVectorHamming) lao).data;
        BitSet clonnedData = (BitSet) data.clone();
        clonnedData.xor(objData);
        return clonnedData.cardinality();
    }

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public boolean dataEquals(Object o) {
        BitSet objData = ((ObjectBitVectorHamming) o).data;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) != objData.get(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int dataHashCode() {
        return data.hashCode();
    }

    @Override
    protected void writeData(OutputStream out) throws IOException {
        writeBitVector(data, out);
    }

    //****************** Implemented interface BinarySerializable ******************//
    /**
     * Creates a new instance of {@link ObjectBitVectorHamming} loaded from
     * binary input buffer.
     *
     * @param input the buffer to read the ObjectBitVectorHamming from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectBitVectorHamming(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        byte[] byteArray = serializator.readByteArray(input);
        this.data = new BitSet(byteArray.length);
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] == 1) {
                this.data.set(i);
            }
        }
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        byte[] byteArray = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            byteArray[i] = data.get(i) ? (byte) 1 : (byte) 0;
        }
        return super.binarySerialize(output, serializator)
                + serializator.write(output, byteArray);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(data);
    }
}
