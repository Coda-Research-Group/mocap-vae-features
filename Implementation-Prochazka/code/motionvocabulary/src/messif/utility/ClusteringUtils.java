/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.StreamGenericAbstractObjectIterator;

/**
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class ClusteringUtils {
    public static StreamGenericAbstractObjectIterator<ObjectFloatVectorNeuralNetworkL2> openDatabaseNeuralNet(String file) {
        return openDB(ObjectFloatVectorNeuralNetworkL2.class, file);
    }
    
    public static StreamGenericAbstractObjectIterator<SequenceMocapPoseCoordsL2DTW> openDatabasePoseCoords(String file) {
        return openDB(SequenceMocapPoseCoordsL2DTW.class, file);
    }
    
    public static <E extends LocalAbstractObject> StreamGenericAbstractObjectIterator<E> openDB(Class<E> clazz, String file) {
        try {
            return new StreamGenericAbstractObjectIterator(clazz, file);
        } catch (IllegalArgumentException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static <E extends LocalAbstractObject> List<E> readObjects(Class<E> clazz, String file) {
        StreamGenericAbstractObjectIterator<E> iter = openDB(clazz, file);
        List<E> objs = new ArrayList<>();
        while (iter.hasNext())
            objs.add(iter.next());
        return objs;
    }
    
    /** 
     * Forms clustering based on the ground truth data specified in object keys.
     * 
     * @param dbIter iterator over database object (aka open file)
     * @return partitions
     */
    public static VoronoiPartitioning.Result getGroundTruthClustering(StreamGenericAbstractObjectIterator<LocalAbstractObject> dbIter) {
        try {
            short clusterNumber = 0;
            Map<Integer,VoronoiPartitioning.VoronoiPartition> clusters = new LinkedHashMap<>();
            Map<Integer,Short> classToClusterId = new HashMap<>();
            List<Short> objectToClusterId = new ArrayList<>();
            
            // Read database
            while (dbIter.hasNext()) {
                LocalAbstractObject o = dbIter.next();
                Integer classId = MotionIdentification.getMotionClassID(o.getLocatorURI());
                
                // Add o to the correct cluster
                VoronoiPartitioning.VoronoiPartition part = clusters.get(classId);
                if (part == null) {
                    part = new VoronoiPartitioning.VoronoiPartition(null);
                    clusters.put(classId, part);
                    classToClusterId.put(classId, clusterNumber++); // set class to cluster id mapping
                }
                part.addMember(o);
                // Assign cluster ID to the object
                objectToClusterId.add(classToClusterId.get(classId));
            }
            dbIter.close();
            
            short[] arr = new short[objectToClusterId.size()];
            int i = 0;
            for (Short v : objectToClusterId)
                arr[i++] = v;
            return new VoronoiPartitioning.Result(clusters.values(), clusters.keySet(), arr);
        } catch (IOException ex) {
            Logger.getLogger(ClusteringUtils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }


    /** TODO:
     * Computes clustering based on the similarity/dissimilarity thresholds. It is a form of agglomerative clustering where the cluster
     * diameters is monitored.
     * 
     * @param dbIter iterator over database object (aka open file)
     * @param similarityDistanceThrehold objects distant up to this threshold (inclusively) are expected to be in the same class 
     * @param dissimilarityDistanceThreshold objects distant more than this threshold (exclusively) are expected to be in different classes.
     * @return partitions
     */
    public static VoronoiPartitioning.Result getDistanceBasedGroundTruthClustering(StreamGenericAbstractObjectIterator<LocalAbstractObject> dbIter,
                        float similarityDistanceThrehold, float dissimilarityDistanceThreshold) {
        short clusterNumber = 0;
        Map<Integer,VoronoiPartitioning.VoronoiPartition> clusters = new HashMap<>();
        Map<Integer,Short> classToClusterId = new HashMap<>();
        List<Short> objectToClusterId = new ArrayList<>();
        int grayZone = 0;

        // Read-in data
        AbstractObjectList<LocalAbstractObject> database = new AbstractObjectList<LocalAbstractObject>(dbIter);

        // Prepare distance matrix
        float[][] distances = new float[database.size()][database.size()];
        for (int i = 0; i < database.size(); i++) {
            LocalAbstractObject o1 = database.get(i);
            //int cat1 = MotionIdentification.getMotionClassID(o1.getLocatorURI());
            distances[i][i] = 0;
            for (int j = i + 1; j < database.size(); j++) {
                LocalAbstractObject o2 = database.get(j);
                distances[i][j] = distances[j][i] = o1.getDistance(o2);
            }
        }


//                if (d <= similarityDistanceThrehold) {
//                    if (cluster1 == cluster2)
//                        tp++;
//                    else
//                        fn++;
//                } else if (d > dissimilarityDistanceThreshold) {
//                    if (cluster1 != cluster2)
//                        tn++;
//                    else
//                        fp++;
//                } else // ignore the distance between the thresholds -- we cannot distiguish them without the real GT.
//                    grayZone++;

//        // Read database
//        while (dbIter.hasNext()) {
//            ObjectFloatVectorNeuralNetworkL2 o = dbIter.next();
//
//            float dist = 
//
//            // Add o to the correct cluster
//            VoronoiPartitioning.VoronoiPartition part = clusters.get(classId);
//            if (part == null) {
//                part = new VoronoiPartitioning.VoronoiPartition(null);
//                clusters.put(classId, part);
//                classToClusterId.put(classId, clusterNumber++); // set class to cluster id mapping
//            }
//            part.addMember(o);
//            // Assign cluster ID to the object
//            objectToClusterId.add(classToClusterId.get(classId));
//        }
//        dbIter.close();

        short[] arr = new short[objectToClusterId.size()];
        int i = 0;
        for (Short v : objectToClusterId)
            arr[i++] = v;
        return new VoronoiPartitioning.Result(clusters.values(), arr);
    }

    
}
