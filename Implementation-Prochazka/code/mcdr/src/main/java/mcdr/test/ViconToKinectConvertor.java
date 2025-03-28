/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mcdr.test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import mcdr.sequence.SequenceMocap;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import messif.objects.util.StreamGenericAbstractObjectIterator;

/**
 * Converts the poses in VICON format to KINECT-V2 format by ignoring the 6 joints listed in {@link SequenceNTUProcessor#MOCAP_KINECT2_SAMEJOINTS_MAP}.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class ViconToKinectConvertor {
    
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        if (args.length == 0) {
            System.err.println("Usage: java " + ViconToKinectConvertor.class.getName() + " <file_name_input> <file_name_output>");
            System.err.println(" Reads in the input file, omits the 6 joints and writes the result to the output file (it is overwritten).");
            System.exit(1);
            return;
        }
        Class<? extends SequenceMocap<?>> objectClass = SequenceMocapPoseCoordsL2DTW.class; // coords
        
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(args[1], false));
        
        StreamGenericAbstractObjectIterator dataIter = new StreamGenericAbstractObjectIterator<>(objectClass, args[0]);
        System.out.println("Reading: " + dataIter.getFileName());
        System.out.println("Writing: " + args[1]);
        int cnt = 0;
        while (dataIter.hasNext()) {
            final SequenceMocapPoseCoordsL2DTW objVicon = (SequenceMocapPoseCoordsL2DTW)dataIter.next();
            List<ObjectMocapPoseCoordsL2> posesKinect = new ArrayList<>(objVicon.getObjectCount());
            for (ObjectMocapPoseCoordsL2 pose : objVicon.getObjects()) {
                final float[][] coordsVicon = pose.getJointCoordinates();
                float[][] coordsKinect = new float[coordsVicon.length - 6][];
                int kinect = 0;
                for (int vicon = 0; vicon < coordsVicon.length; vicon++) {
                    if (!SequenceNTUProcessor.MOCAP_KINECT2_SAMEJOINTS_MAP.containsKey(vicon)) {
                        coordsKinect[kinect] = coordsVicon[vicon];
                        kinect++;
                    }
                }
                posesKinect.add(new ObjectMocapPoseCoordsL2(coordsKinect));
            }
            SequenceMocapPoseCoordsL2DTW objKinect = new SequenceMocapPoseCoordsL2DTW(posesKinect);
            objKinect.setObjectKey(objVicon.getObjectKey());
            objKinect.write(out);
            cnt++;
        }
        out.flush();
        out.close();
        System.out.println("Converted " + cnt + " objects.");
    }
}
