/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary.impl;

import cz.muni.fi.disa.similarityoperators.cover.HullCenterRepresentation;
import cz.muni.fi.disa.similarityoperators.cover.HullRepresentation;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.objects.LocalAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.utility.ClusteringUtils;

/**
 * Vocabulary based on hulls that have their centers assigned.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class HullCenterVocabulary extends HullVocabulary {
    
    /** Create a list of hulls with their centers
     * 
     * @param centers file with center objects or a directory where files of name "cluster-NNN.center" or "class-NNN.center" are read
     * @param path path to files with hulls objects; files must have .hull extension
     * @param objClass class of object to be read from hull files
     */
    public HullCenterVocabulary(String centers, String path, Class<? extends LocalAbstractObject> objClass) {
        super(path, objClass);
        initCenters(centers, (Class<LocalAbstractObject>)objClass);
    }

    private void initCenters(String centers, Class<LocalAbstractObject> objClass) {
        if (new File(centers).isFile()) {
            StreamGenericAbstractObjectIterator<LocalAbstractObject> iterCenter = ClusteringUtils.openDB(objClass, centers);
            int centerIdx = 0;
            while (iterCenter.hasNext()) {
                LocalAbstractObject center = iterCenter.next();
                HullCenterRepresentation h = (HullCenterRepresentation)hulls[centerIdx++];
                if (h != null)      // Skip non-existing hulls
                    h.setCenter(center);
            }
        } else {
            File[] files = new File(centers).listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".center");
                }
            });
            Pattern p = Pattern.compile("(?:cluster|class)-([0-9]+)\\.\\w+", Pattern.CASE_INSENSITIVE);
            for (File f : files) {
                Matcher m = p.matcher(f.getName());
                if (m.matches()) {
                    try {
                        int idx = Integer.parseInt(m.group(1));
                        StreamGenericAbstractObjectIterator<LocalAbstractObject> iterCenter = ClusteringUtils.openDB(objClass, f.getAbsolutePath());
                        HullCenterRepresentation h = (HullCenterRepresentation)hulls[idx];
                        h.setCenter(iterCenter.next());
                        iterCenter.close();
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Bad file name " + f.getName(), ex);
                    } catch (IOException ex) {
                        throw new IllegalArgumentException("Cannot read an object from the file " + f.getName(), ex);
                    }
                } else {
                   System.err.println("Unknown file that cannot be processed: " + f.getName());
                }
            }
        }
    }

    @Override
    protected HullRepresentation instantiateHull(List<LocalAbstractObject> hullObjs) {
        HullCenterRepresentation repr = new HullCenterRepresentation(hullObjs);
        repr.setHullObjects(hullObjs);
        return repr;
    }

}
