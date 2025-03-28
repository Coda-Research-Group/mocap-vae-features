/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary.impl;

import cz.muni.fi.disa.similarityoperators.cover.BallRepresentation;
import cz.muni.fi.disa.similarityoperators.cover.CoverRank;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mcdr.objects.impl.ObjectMotionWord;
import messif.objects.LocalAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.utility.ClusteringUtils;
import messif.utility.FileUtils;

/**
 * Vocabulary based on snake representations -- list of ball covering regions around center objects.
 * 
 * The center objects are read from the passed file (e.g. pivots-kmedoids-100.data) and initialized a ball region for each.
 * The covering radius is then obtained by getting objects falling into each region and computing the max distance.
 * The objects are read from files named cluster-99.data, where the number is the zero-based index to the file with center objects.
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SnakeVocabulary {
    private final List<BallRepresentation> balls;

    /** Create a list of balls for snake representation
     * 
     * @param centers file with center objects
     * @param path path to *.data files containing objects around each center from the 'centers' file or to the file containing radii (e.g. pivots-kmedoids-3000-radii.txt)
     * @param objClass class of object to be read from hull files
     */
    public SnakeVocabulary(String centers, String path, Class<LocalAbstractObject> objClass) {
        balls = new ArrayList<>();
        init(centers, path, objClass);
    }

    private void init(String centers, String path, Class<LocalAbstractObject> objClass) {
        // Get radii (init BallRepresentations)
        final File fPath = new File(path);
        if (fPath.isFile()) {
            // Get ball centers
            final StreamGenericAbstractObjectIterator<LocalAbstractObject> iterCenter = ClusteringUtils.openDB(objClass, centers);
            Pattern p = Pattern.compile("^(\\d+(?:\\.\\d+))\\s+(\\d+)$");    // <radius><space><object_in_cluster>
            try {
                BufferedReader r = new BufferedReader(new FileReader(fPath));
                r.lines().forEachOrdered((line) -> {
                    if (line.isEmpty())
                        return;
                    final LocalAbstractObject center = iterCenter.next();
                    Matcher m = p.matcher(line);
                    if (m.matches()) {
                        float rad = Float.parseFloat(m.group(1));
                        final BallRepresentation b = new BallRepresentation(Collections.singletonList(center)); // createBall(centerObjs.get(centerIdx), f, objClass);
                        b.setCenter(center);
                        b.setRadius(rad);
                        balls.add(b);
                    } else {
                        System.err.println("Cannot parse radii file's line: " + line);
                    }
                });
            } catch (FileNotFoundException | NumberFormatException ex) {
                System.err.println("Cannot read radii file: " + fPath.getName());
            }
            
        } else { // Dir
            File[] files = fPath.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".data");
                }
            });
            Arrays.sort(files);

            // Get ball centers
            StreamGenericAbstractObjectIterator<LocalAbstractObject> iterCenter = ClusteringUtils.openDB(objClass, centers);
            List<LocalAbstractObject> centerObjs = new ArrayList<>();
            while (iterCenter.hasNext())
                centerObjs.add(iterCenter.next());

            Pattern p = Pattern.compile("cluster-([0-9]+)\\.\\w+", Pattern.CASE_INSENSITIVE);
            for (File f : files) {
                Matcher m = p.matcher(f.getName());
                if (m.matches()) {
                    try {
                        int centerIdx = Integer.parseInt(m.group(1));
                        final BallRepresentation b = createBall(centerObjs.get(centerIdx), f, objClass);
                        balls.add(b);
                        System.err.println("Initializing ball " + centerIdx + " with data from file " + f.getName() + ", radius " + b.getAverageRadius());
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Bad file name " + f.getName(), ex);
                    }
                } else {
                   System.err.println("Unknown file that cannot be processed: " + f.getName());
                }
            }
        }
    }

    private BallRepresentation createBall(LocalAbstractObject center, File dataFile, Class<LocalAbstractObject> objClass) {
        StreamGenericAbstractObjectIterator<LocalAbstractObject> iter = ClusteringUtils.openDB(objClass, dataFile.getAbsolutePath());
        List<LocalAbstractObject> data = new ArrayList<>();
//        LocalAbstractObject centerFromData = null;
        while (iter.hasNext()) {
            final LocalAbstractObject o = iter.next();
            data.add(o);
//            if (o.getLocatorURI().equals(center.getLocatorURI()))
//                centerFromData = center;
        }
        
        BallRepresentation repr = new BallRepresentation(data);
        repr.setCenter(center);
        return repr;
    }
    
    /** Convert the passed object to a motion word.
     * @param o object to quantize
     * @param maxWords maximum number of hulls to identify in quantization
     * @return motion word or null if no hull covers the passed object
     */
    public ObjectMotionWord quantize(LocalAbstractObject o, int maxWords) {
        CoverRankMW[] ranks = new CoverRankMW[balls.size()];
        int mws = 0;
        for (int i = 0; i < balls.size(); i++) {
            BallRepresentation b = balls.get(i);
            CoverRank rank = b.getExternalCoverRank(o);
            if (rank.isCovered()) {
                ranks[mws++] = new CoverRankMW(i, rank);
            }
        }
        if (mws == 0) {
            System.err.println("# ERROR: Failed to quantize the object " + o.getLocatorURI() + ". No hull covers it!!!");
            return null;
        }
        CoverRankMW[] ranksSorted = Arrays.copyOf(ranks, mws);
        Arrays.sort(ranksSorted);
        long[] ids = new long[Math.min(maxWords, mws)];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ranksSorted[i].mwID;
        }
        return new ObjectMotionWord(o.getLocatorURI(), ids);
    }
}
