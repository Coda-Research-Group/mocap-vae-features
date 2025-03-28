/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary.impl;

import cz.muni.fi.disa.similarityoperators.cover.CoverRank;
import cz.muni.fi.disa.similarityoperators.cover.HullRepresentation;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mcdr.objects.impl.ObjectMotionWord;
import messif.objects.LocalAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.utility.ClusteringUtils;

/**
 * Initializes a list of hulls to allow quantizing data objects by their inclusion in hulls.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class HullVocabulary {
    /** Set the number of threads to used to decide presence in hulls. May not be used by all functions here, so check their respective docs. */
    public static int PARALLEL_COMPUTING = 1;

    protected HullRepresentation[] hulls;

    /** Create a list of hulls
     * 
     * @param path path to files with hulls objects; files must have .hull extension
     * @param objClass class of object to be read from hull files
     */
    public HullVocabulary(String path, Class<? extends LocalAbstractObject> objClass) {
        initHulls(path, (Class<LocalAbstractObject>)objClass);
    }

    private void initHulls(String path, Class<LocalAbstractObject> objClass) {
        File[] files = new File(path).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                final boolean suffix = name.endsWith(".hull");
                if (suffix && name.contains("_105."))
                    System.out.println("HULL FILE: " + name);
                return suffix;
            }
        });

        hulls = new HullRepresentation[files.length];

        int idx = -1;
        Pattern p = Pattern.compile("(?:cluster|class|action)[_-]([0-9]+)(_[0-9]+)?(_[0-9]+)?(_[0-9]+)?\\.\\w+", Pattern.CASE_INSENSITIVE);
        for (File f : files) {
            Matcher m = p.matcher(f.getName());
            if (m.matches()) {
                try {
                    HullRepresentation h = createHull(f, objClass);
                    if (m.group(2) != null) {
                        h.setLocatorID(m.group(1) + m.group(2));
                        idx++;
                    } else {
                        idx = Integer.parseInt(m.group(1));
                        h.setLocatorID("hull-" + f.getName());
                    }
                    setHull(idx, h);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Bad file name " + f.getName(), ex);
                }
            } else {
               System.err.println("Unknown file that cannot be processed: " + f.getName());
            }
        }
    }

    private void setHull(int idx, HullRepresentation h) {
        if (idx >= hulls.length)
            hulls = Arrays.copyOf(hulls, idx + 1);
        hulls[idx] = h;
    }
    
    private HullRepresentation createHull(File hullFile, Class<LocalAbstractObject> objClass) {
        StreamGenericAbstractObjectIterator<LocalAbstractObject> iter = ClusteringUtils.openDB(objClass, hullFile.getAbsolutePath());
        List<LocalAbstractObject> hullObjs = new ArrayList<>();
        while (iter.hasNext())
            hullObjs.add(iter.next());
        
        return instantiateHull(hullObjs);
    }

    protected HullRepresentation instantiateHull(List<LocalAbstractObject> hullObjs) {
        HullRepresentation repr = new HullRepresentation(hullObjs);     // We do not select hull objects here, we rather take existing ones (already selected).
        repr.setHullObjects(hullObjs);
        return repr;
    }
    
    public HullRepresentation[] getHulls() {
        return hulls;
    }
    
    /** Convert the passed object to a motion word.
     * @param o object to quantize
     * @param maxWords maximum number of hulls to identify in quantization
     * @return motion word or null if no hull covers the passed object
     */
    public ObjectMotionWord quantize(LocalAbstractObject o, int maxWords) {
        final CoverRankMW[] ranks = new CoverRankMW[hulls.length];
        final AtomicInteger mws = new AtomicInteger();
        if (PARALLEL_COMPUTING <= 1) {
            for (int i = 0; i < hulls.length; i++) {
                HullRepresentation h = hulls[i];
                if (h == null)
                    continue;
                CoverRank rank = h.getExternalCoverRank(o);
                if (rank.isCovered())
                    ranks[mws.getAndIncrement()] = new CoverRankMW(i, rank);
            }
        } else {
            List<Thread> computingThreads = new ArrayList<>();
            for (int from = 0; from < hulls.length; from += PARALLEL_COMPUTING) {
                int to = from + PARALLEL_COMPUTING;
                if (to > hulls.length)
                    to = hulls.length;
                Thread t = new Thread(new HullComputingRunnable(ranks, mws, from, to, o));
                t.start();
                computingThreads.add(t);
                //computingThreads.add(threadPool.submit());
            }
            // Wait for completion
            try {
                for (Thread t : computingThreads)
                    t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mws.get() == 0) {
            System.err.println("# ERROR: Failed to quantize the object " + o.getLocatorURI() + ". No hull covers it!!!");
            return null;
        }
        System.err.println("# INFO: " + o.getLocatorURI() + " mws=" + mws.get());
        CoverRankMW[] ranksSorted = Arrays.copyOf(ranks, mws.get());
        Arrays.sort(ranksSorted);
        long[] ids = new long[Math.min(maxWords, mws.get())];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ranksSorted[i].mwID;
        }
        return new ObjectMotionWord(o.getLocatorURI(), ids);
    }

    private class HullComputingRunnable implements Runnable {
        final CoverRankMW[] ranks;
        final AtomicInteger mws;
        final private int from;
        final private int to;
        final private LocalAbstractObject o;

        public HullComputingRunnable(CoverRankMW[] ranks, AtomicInteger mws, int from, int to, LocalAbstractObject o) {
            this.ranks = ranks;
            this.mws = mws;
            this.from = from;
            this.to = to;
            this.o = o;
        }

        @Override
        public void run() {
            for (int i = from; i < to; i++) {
                HullRepresentation h = hulls[i];
                if (h == null)
                    continue;
                CoverRank rank = h.getExternalCoverRank(o);
                if (rank.isCovered())
                    ranks[mws.getAndIncrement()] = new CoverRankMW(i, rank);
            }
        }
    }
}
