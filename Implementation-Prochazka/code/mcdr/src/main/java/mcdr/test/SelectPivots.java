package mcdr.test;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import mcdr.objects.impl.ObjectMocapPoseCoordsL2Filtered;
import messif.objects.util.AbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.pivotselection.AbstractPivotChooser;
import messif.pivotselection.KMeansPivotChooser;
import messif.statistics.StatisticCounter;
import messif.utility.Convert;
//import messif.utility.VoronoiPartitioning;


/**
 * Select the pivots using some chooser and settings.
 * Based on {@code https://gitlab.fi.muni.cz/disa/public/messif-utils/-/blob/master/src/main/java/SelectPivots.java}.
 * This version has a joint filter.
 * 
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class SelectPivots {

    static int numberOfPivots = 20;
    static String pivotChooserString = "messif.pivotselection.IncrementalIDistanceChooser";
    static String objectString = "messif.objects.impl.MetaObjectSAPIRWeightedDist2";
    static int repeat = 1;
    static boolean deleteFilters = true;
    static boolean useAllSampleInChooser = false;
    static String hkmeansPivotsPerLevel = null;
    static boolean hkmeansComputed = false;

    /** Formats the usage string */
    private static void usage() {
        System.err.println("Usage: SelectPivots [parameters]");
        System.err.println("Parameters:");
        System.err.println("            -h [--help]         - show this help");
        System.err.println("            -outfile <file>     - file name to append the selected pivots to, if not specified, System.out is used.");
        System.err.println("            -cls <object_class> - metric-object class to be used, default: " + objectString);
        System.err.println("            -pc <pivot_chooser> - default class " + pivotChooserString);
        System.err.println("                                  Any pivot chooser can be used but it must have a zero-parameter or a list-of-objects-parameter constructor.");
        System.err.println("            -hkmeans-pivots <pvt_cnt_1st_lvl,pvt_cnt_2nd_lvl,...>  - pivots to select per each hierarchical k-means level.");
        System.err.println("            -hkmeans-center-computed   - a flag that instructs not to choose mediods but rather computed centers.");
        System.err.println("            -np <#_of_pivots>   - number of pivots/clusters to select, default: " + numberOfPivots);
        System.err.println("            -pcuseall           - all sample file objects are used in the pivot selection; only KMeansPivotChooser is supported now.");

        System.err.println("            -r <repeat_count>   - repeat the seletion given times (result is as if this class has been executed given times) (default: 1");
        System.err.println("            -partfiles          - create part files by appending the iteration number to the filename given in -outfile parameter");
        
        System.err.println("            -sf <sample_file>   - file with sample objects (if not given, objects are read from stdin)");
        System.err.println("            -sfc <sample_size>  - count of sample objects (if not given, all objects from <sample_file> will be read)");

        System.err.println("            -pf <pivots_file>   - file with preselected pivots (first -np objects taken)");
        System.err.println("            -ip <pivots_file>   - file with the inital pivots (e.g. for K-Means chooser) (first -np objects taken)");

        System.err.println("            -diameter <distance> - diameter of clusters produced");

        System.err.println("            -printradii <radii_file>  - do voronoi partitioning with the selected pivots and print covering radii of the partitions");

        System.err.println("            -retainfilters      - use this switch to avoid deleting all filters associated with the objects.");
        
        System.err.println("            -kmeans-max-iters <cnt>  - maximum number of iterations of k-means/medoid pivot chooser, 0 for printing radii only when preselected pivots are given.");
        
//        System.err.println();
//        System.err.println("    Send SIGHUP to the process to dump currently selected pivots to a file");
    }
    
    public static void main(String args[]) {
        // DAVID: joint filter
        String joints = args[0];
        var split = joints.split(",");
        var integers = Arrays.stream(split).map(Integer::parseInt).collect(Collectors.toSet());
        System.err.println("JOINTS: " + integers);
        ObjectMocapPoseCoordsL2Filtered.jointIds = integers;
        args = Arrays.copyOfRange(args, 1,args.length);

        Class<? extends LocalAbstractObject> objectClass = null;
        String sampleFileName = null;
        int sampleSize = Integer.MAX_VALUE;
        String pivotsFileName = null;
        String initPivotsFileName = null;
        String outputPivotsFile = null;
        String radiusFile = null;
        float clusterDiameter = 0f;
        boolean partFiles = false;
        
        StringBuffer strbuf = new StringBuffer("The 'Select pivots' program started with arguments: ");
        for (int i = 0; i<args.length; i++)
            strbuf.append(args[i]+", ");
        System.err.println(strbuf.toString());
        
        // print usage
        if ((args.length == 0) || ((args[0].equals("-h")) || (args[0].equals("--help")))) {
            usage();
            return;
        }
        
        // parse and handle the options and commands
        for (int i = 0; i < args.length; ) {
            boolean err = false;
            // the object class for this M-Chord system
            if (args[i].equals("-cls")) {
                if (++i >= args.length)
                    err = true;
                else
                    objectClass = selectObjectClass(args[i]);
            }
            
            // number of pivots
            else if (args[i].equals("-np")) {
                if (++i >= args.length)
                    err = true;
                else
                    numberOfPivots = Integer.parseInt(args[i]);
            }
            // file with sample data (strings)
            else if (args[i].equals("-sf")) {
                if (++i >= args.length)
                    err = true;
                else
                    sampleFileName = args[i];
            }
            // size of sample data
            else if (args[i].equals("-sfc")) {
                if (++i >= args.length)
                    err = true;
                else
                    sampleSize = Integer.parseInt(args[i]);
            }
            // file with preselected pivots (pivot file)
            else if (args[i].equals("-pf")) {
                if (++i >= args.length)
                    err = true;
                else
                    pivotsFileName = args[i];
            }
            // file with initial pivots
            else if (args[i].equals("-ip")) {
                if (++i >= args.length)
                    err = true;
                else
                    initPivotsFileName = args[i];
            }
            // output file specification
            else if (args[i].equals("-outfile")) {
                if (++i >= args.length)
                    err = true;
                else
                    outputPivotsFile = args[i];
            }
            // radius file specification
            else if (args[i].equals("-printradii")) {
                if (++i >= args.length)
                    err = true;
                else
                    radiusFile = args[i];
            }
            // pivot chooser string
            else if (args[i].equals("-pc")) {
                if (++i >= args.length)
                    err = true;
                else
                    pivotChooserString = args[i];
            }
            // Limit of chooser cleared to all data sample
            else if (args[i].equals("-pcuseall")) {
                useAllSampleInChooser = true;
            }
            // Repeat the whole process of selection
            else if (args[i].equals("-r")) {
                if (++i >= args.length)
                    err = true;
                else
                    repeat = Integer.parseInt(args[i]);
            }
            // K-means pivot chooser parameter
            else if (args[i].equals("-kmeans-max-iters")) {
                if (++i >= args.length)
                    err = true;
                else
                    KMeansPivotChooser.MAX_ITERATIONS = Integer.parseInt(args[i]);
            }
            // Hierarchical K-means param
            else if (args[i].equals("-hkmeans-pivots")) {
                if (++i >= args.length)
                    err = true;
                else
                    hkmeansPivotsPerLevel = args[i];
            }
            else if (args[i].equals("-hkmeans-center-computed")) {
                hkmeansComputed = true;
            }
            // Do not delete object filters flag
            else if (args[i].equals("-retainfilters")) {
                deleteFilters = false;
            }
            // Diameter of the clusters produced
            else if (args[i].equals("-diameter")) {
                if (++i >= args.length)
                    err = true;
                else
                    clusterDiameter = Float.parseFloat(args[i]);
            }
            else if (args[i].equals("-partfiles")) {
                partFiles = true;
            }
            // else: unknown option
            else {
                System.err.println("Unknown option/command '"+args[i]+"'");
                System.err.println();
                usage();
                return;
            }
            
            if (err) {
                System.err.println("Missing argument to " + args[i]);
                System.err.println();
                usage();
                return;
            }
            
            i++;
        } // end of parameters parsing
        

        // if object class has not been given, use default
        if (objectClass == null) {
            objectClass = selectObjectClass(objectString);
            if (objectClass == null)
                return;
        }

        // read the preselected pivots if specified
        AbstractObjectList<LocalAbstractObject> preselectedPivots = null;
        LocalAbstractObject pivotObj = null;
        if (pivotsFileName != null) {
            preselectedPivots = new AbstractObjectList<LocalAbstractObject>(numberOfPivots);
            try {
                // Open sample stream
                StreamGenericAbstractObjectIterator<LocalAbstractObject> iterator = new StreamGenericAbstractObjectIterator<LocalAbstractObject>(
                        objectClass, // Class of objects in file
                        pivotsFileName // File name
                        );
                
                int p = 0;
                while (iterator.hasNext() && p++ < numberOfPivots) {
                    pivotObj = iterator.next();
                    preselectedPivots.add(pivotObj);
                }
            } catch (IOException e) {
                e.printStackTrace(); return;
            }
        }

        // read the initial pivots if specified
        AbstractObjectList<LocalAbstractObject> initialPivots = null;
        if (initPivotsFileName != null) {
            initialPivots = new AbstractObjectList<LocalAbstractObject>(numberOfPivots);
            try {
                // Open sample stream
                StreamGenericAbstractObjectIterator<LocalAbstractObject> iterator = new StreamGenericAbstractObjectIterator<LocalAbstractObject>(
                        objectClass, // Class of objects in file
                        initPivotsFileName // File name
                        );
                
                int p = 0;
                while (iterator.hasNext() && p++ < numberOfPivots) {
                    pivotObj = iterator.next();
                    initialPivots.add(pivotObj);
                }
            } catch (IOException e) {
                e.printStackTrace(); return;
            }
        }

        // Open sample stream
        StreamGenericAbstractObjectIterator<LocalAbstractObject> iterator;
        try {
            iterator = new StreamGenericAbstractObjectIterator<LocalAbstractObject>(objectClass, sampleFileName);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace(); return;
        } catch (IOException ex) {
            ex.printStackTrace(); return;
        }


        for (int iter = 0; iter < repeat; iter++) {
            System.err.println("Iteration " + (iter+1));

            // create a set of sample data objects
            AbstractObjectList<LocalAbstractObject> sampleObjects = new AbstractObjectList<LocalAbstractObject>(1000);
            LocalAbstractObject sampleObj = null;

            for (int i = 0; iterator.hasNext() && i < sampleSize; i++) {
                sampleObj = iterator.next();
                sampleObjects.add(sampleObj);
                //if (i % 1000 == 0)
                //    System.err.println("Number of objects read: " + i + ", id: " + sampleObj.getLocatorURI());
            }
            //System.err.println("Sample set read into the memory");

            String fNameOutputPivots = outputPivotsFile;
            String fNameRadius = radiusFile;
            if (partFiles) {
                if (fNameOutputPivots != null)
                    fNameOutputPivots += iter;
                if (fNameRadius != null)
                    fNameRadius += iter;
            }

            selectPivots(initialPivots, sampleObjects, preselectedPivots, fNameOutputPivots, clusterDiameter, fNameRadius);
        }
    }

    private static boolean selectPivots(AbstractObjectList<LocalAbstractObject> initialPivots, AbstractObjectList<LocalAbstractObject> sampleObjects,
                                        AbstractObjectList<LocalAbstractObject> preselectedPivots, String outputPivotsFile, float clusterDiameter,
                                        String radiusFile) throws IndexOutOfBoundsException {
        // Select the pivots
        AbstractPivotChooser chooser = null;
        try {
            long startTime = System.currentTimeMillis();
            StatisticCounter.getStatistics("DistanceComputations").reset();
            Class<AbstractPivotChooser> chooserClass = Convert.getClassForName(pivotChooserString, AbstractPivotChooser.class);
            System.out.println("Using pivot chooser: " + chooserClass.toString());
            try {
                if (useAllSampleInChooser && KMeansPivotChooser.class.isAssignableFrom(chooserClass))
                    KMeansPivotChooser.PIVOTS_SAMPLE_SIZE = sampleObjects.size();

                if (initialPivots != null)
                    chooser = chooserClass.getConstructor(AbstractObjectList.class).newInstance(initialPivots);
                else if (clusterDiameter > 0f)
                    chooser = chooserClass.getConstructor(float.class).newInstance(clusterDiameter);
                else if (hkmeansPivotsPerLevel != null)
                    chooser = chooserClass.getConstructor(String.class, boolean.class).newInstance(hkmeansPivotsPerLevel, hkmeansComputed);
                else
                    chooser = chooserClass.getConstructor().newInstance();
            } catch (InstantiationException ex) {
                Logger.getLogger(SelectPivots.class.getName()).log(Level.SEVERE, null, ex);
                throw new ClassNotFoundException("Cannot instantiate pivot chooser (zero-parameter or AbstractObjectList-parametrized constructor failed): " + chooserClass.toString());
            } catch (IllegalAccessException ex) {
                Logger.getLogger(SelectPivots.class.getName()).log(Level.SEVERE, null, ex);
                throw new ClassNotFoundException("Cannot instantiate pivot chooser (zero-parameter or AbstractObjectList-parametrized constructor failed): " + chooserClass.toString());
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(SelectPivots.class.getName()).log(Level.SEVERE, null, ex);
                throw new ClassNotFoundException("Cannot instantiate pivot chooser (zero-parameter or AbstractObjectList-parametrized constructor failed): " + chooserClass.toString());
            } catch (InvocationTargetException ex) {
                Logger.getLogger(SelectPivots.class.getName()).log(Level.SEVERE, null, ex);
                throw new ClassNotFoundException("Cannot instantiate pivot chooser (zero-parameter or AbstractObjectList-parametrized constructor failed): " + chooserClass.toString());
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(SelectPivots.class.getName()).log(Level.SEVERE, null, ex);
                throw new ClassNotFoundException("Cannot instantiate pivot chooser (zero-parameter or AbstractObjectList-parametrized constructor failed): " + chooserClass.toString());
            } catch (SecurityException ex) {
                Logger.getLogger(SelectPivots.class.getName()).log(Level.SEVERE, null, ex);
                throw new ClassNotFoundException("Cannot instantiate pivot chooser (zero-parameter or AbstractObjectList-parametrized constructor failed): " + chooserClass.toString());
            }
            chooser.registerSampleProvider(sampleObjects);
            if (preselectedPivots != null) {
                for (LocalAbstractObject pivot : preselectedPivots) {
                    chooser.addPivot(pivot);
                }
            }
            //System.err.println("Selecting '" + numberOfPivots + "' pivots" + ((preselectedPivots == null) ? "" : " (" + preselectedPivots.size() + " of them already selected)"));
            System.err.println("Selecting pivots..." + ((preselectedPivots == null) ? "" : " (" + preselectedPivots.size() + " already selected)"));
//XX            registerUnixSignal(outputPivotsFile, chooser);
            chooser.selectPivot(numberOfPivots - ((preselectedPivots == null) ? 0 : preselectedPivots.size()));
            dumpSelectedPivotsToFile(outputPivotsFile, chooser);

            // If radii must be computed, do voronoi partitioning and output the radii to the separate file
            if (radiusFile != null) {
                System.err.println("Computing covering radii...");
//                BufferedOutputStream file = new BufferedOutputStream(new FileOutputStream(radiusFile, true));
                throw new IllegalArgumentException("VOROTNOI PARATIINGON");
//                for (VoronoiPartitioning.VoronoiPartition p : VoronoiPartitioning.doVoronoiPartitioning(chooser, sampleObjects).getParts())
//                    file.write(String.format(Locale.ENGLISH, "%f\t%d\n", p.getCoveringRadius(), p.size()).getBytes());
//                file.close();
            }

            System.err.println("Number of pivots selected: " + chooser.size());
            long overallTime = System.currentTimeMillis() - startTime;
            System.err.println(StatisticCounter.printStatistics("DistanceComputations"));
            System.err.println("Overall time: " + overallTime / 1000 + " s");
            System.err.println("Distance comp. time less then: " + ((double) overallTime) / ((double) (StatisticCounter.getStatistics("DistanceComputations").get())) + " ms");
        } catch (IOException f) {
            System.err.println("Error writing output file");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    private static void dumpSelectedPivotsToFile(String outputPivotsFile, AbstractPivotChooser chooser) throws IOException, IndexOutOfBoundsException, FileNotFoundException {
        BufferedOutputStream outputFile = new BufferedOutputStream((outputPivotsFile == null) ? System.out : new FileOutputStream(outputPivotsFile, true));
        // the main pivot selecting code
        for (int i = 0; i < chooser.size(); i++) {
            LocalAbstractObject p = chooser.getPivot(i);
            if (deleteFilters)
                p.chainDestroy();
            p.write(outputFile);
        }
        outputFile.close();
    }


    static private Class<? extends LocalAbstractObject> selectObjectClass(String className) {
        Class<? extends LocalAbstractObject> objectClass = null;
        try {
            if (!className.contains("."))
                className = "messif.objects.impl." + className;
            objectClass = Convert.getClassForName(className, LocalAbstractObject.class);
        } catch (ClassNotFoundException e) {
            System.err.println("Unknown class "+className);
            usage();
        } catch (ClassCastException e) {
            System.err.println("Class "+className+" must extend LocalAbstractObject");
            usage();
        }
        if (!LocalAbstractObject.class.isAssignableFrom(objectClass)) {
            System.err.println("Class "+className+" is not subclass of messif.objects.LocalAbstractObject");
            usage();
            objectClass = null;
        }
        return objectClass;
    }

//    public static final String SIGHUP = "HUP";
//    private static void registerUnixSignal(final String outputPivotsFile, final AbstractPivotChooser chooser) {
//        sun.misc.Signal.handle(new sun.misc.Signal(SIGHUP), (signal) -> {
//            try {
//                // THIS DOES NOT WORK SINCE THE CHOOSER MAY UPDATE preselectedPivots WHEN THEY FINISH THE SELECTION!
//                dumpSelectedPivotsToFile(outputPivotsFile, chooser);
//            } catch (IOException | IndexOutOfBoundsException ex) { } // Ignore
//        });
//    }
}
