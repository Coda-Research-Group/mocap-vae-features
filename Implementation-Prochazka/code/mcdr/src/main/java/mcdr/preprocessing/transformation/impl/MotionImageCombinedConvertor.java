package mcdr.preprocessing.transformation.impl;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import mcdr.sequence.SequenceMocap;
import messif.utility.Convertor;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class MotionImageCombinedConvertor implements Convertor<SequenceMocap<?>, BufferedImage> {

    // motion image convertors that are used to create a combined image
    private final List<? extends MotionImageConvertor> motionImageConvertors;

    //************ Constructors ************//
    /**
     * Creates a new instance of {@link MotionImageCombinedConvertor}.
     *
     * @param motionImageConvertors motion image convertors that are used to
     * create a combined image
     */
    public MotionImageCombinedConvertor(List<? extends MotionImageConvertor> motionImageConvertors) {
        this.motionImageConvertors = motionImageConvertors;
    }

    //************ Implemented interface Convertor ************//
    @Override
    public BufferedImage convert(SequenceMocap<?> sequence) {
        int imageWidth = 0;
        int imageHeight = 0;
        List<BufferedImage> subImages = new ArrayList<>(motionImageConvertors.size());
        for (MotionImageConvertor convertor : motionImageConvertors) {
            BufferedImage image = convertor.convert(sequence);
            subImages.add(image);
            imageWidth = Math.max(imageWidth, image.getWidth());
            imageHeight += image.getHeight();
        }

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        int imageHeightIdx = 0;
        for (BufferedImage subImage : subImages) {
            g2.drawImage(subImage, 0, imageHeightIdx, null);
            imageHeightIdx += subImage.getHeight();
        }
        return image;
    }

    @Override
    public Class<? extends BufferedImage> getDestinationClass() {
        return BufferedImage.class;
    }
}
