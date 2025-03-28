/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import mcdr.objects.impl.ObjectMocapPoseCoordsL2Filtered;
import mcdr.objects.impl.ObjectMotionWord;
import mcdr.sequence.SequenceMotionWords;
import mcdr.sequence.impl.SequenceMotionWordsDTW;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.motionvocabulary.impl.HullCenterVocabulary;
import messif.motionvocabulary.impl.HullVocabulary;
import messif.motionvocabulary.impl.SnakeVocabulary;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.ObjectFeatureQuantized;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.keys.DimensionObjectKey;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.operations.query.QuantizeOperation;
import messif.pivotselection.KMeansPivotChooser;
import messif.pivotselection.RandomPivotChooser;
import messif.statistics.Statistics;
import messif.utility.ClusteringUtils;
import messif.utility.DistanceMatrix;
import messif.utility.MotionIdentification;
import messif.utility.NearestNeighborConsistency;
import messif.utility.RandIndex;
import messif.utility.RecursiveVoronoiPartitioning;
import messif.utility.SilhouetteIndex;
import messif.utility.VoronoiPartitioning;
import mindex.algorithms.MultipleMIndexAlgorithm;
import mindex.processors.SoftQuantizeOperationNavigationProcessor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class MotionVocabulary {
    private static String DB_WHOLE_MOTIONS = "hdm05-annotations_specific-1fold_130classes.data";
    private static String DB_SEGMENTS80_SHIFT16 = "hdm05-annotations_specific-segment80_shift16-1fold_130classes.data";
    
    private static String OBJECT_CLASS_NEURAL_NET = "messif.objects.impl.ObjectFloatVectorNeuralNetworkL2";

    private static Options opts = createOptions();
    
    private static Options createOptions() {
        Options opts = new Options();
        // General setting dataset and object class
        opts.addOption(Option.builder("d").longOpt("dataset").desc("file name of the dataset to read.")
                        .required().hasArg().argName("PATH").build());
        opts.addOption(Option.builder("c").longOpt("class").desc("object class to work with in the dataset and pivots (default is " + OBJECT_CLASS_NEURAL_NET + ").")
                        .hasArg().argName("CLASS").build());
        // Specify what partitioning to load (classic Voronoi (-v) or M-index variant)
        opts.addOption(Option.builder("v").longOpt("voronoi").desc("prepare Voronoi clustering based on the passed pivots in the argument; this option can be passed multiple times. You may use --threads to increase parallelism.")
                        .hasArgs().argName("PATH").build());
        opts.addOption(Option.builder().longOpt("mindex").desc("load M-index from the passed bin file; this option can be passed multiple times.")
                        .hasArgs().argName("PATH").build());
        opts.addOption(Option.builder().longOpt("mmindex").desc("load Multiple M-index from the passed bin file; this option can be passed multiple times.")
                        .hasArgs().argName("PATH").build());
        opts.addOption(Option.builder().longOpt("hull").desc("load hulls from files in the passed path; this option can be passed multiple times. Optional parameter: k-closest.")
                        .hasArgs().argName("PATH").build());
        opts.addOption(Option.builder().longOpt("balls").desc("load balls from the center file and files in the passed path; this option can be passed multiple times.")
                        .hasArgs().argName("PATH").build());
        // Statistics
        opts.addOption("s", "stats", false, "compute Silhouette and Unsupervised Adjusted Rand Index for the Voronoi partitioning (option -v is required here).");
        opts.addOption(Option.builder().longOpt("thresholds").desc("comma-separated values of distance, where each pair forms similar and dissimilar thresholds.")
                        .hasArg().argName("THRESHOLDS").type(String.class).build());
        opts.addOption(Option.builder().longOpt("hkmeans-pivots").desc("comma-separated list of pivot counts per level in hierarchical k-means/k-medoids.")
                        .hasArg().argName("HK-PIVOTS").type(String.class).build());
        opts.addOption(Option.builder().longOpt("threshold-similar").desc("unsupervised ARI requires similarity threshold on distance.")
                        .hasArg().argName("DISTANCE").type(Number.class).build());
        opts.addOption(Option.builder().longOpt("threshold-dissimilar").desc("unsupervised ARI requires dissimilarity threshold on distance.")
                        .hasArg().argName("DISTANCE").type(Number.class).build());
        opts.addOption(Option.builder().longOpt("silhouette").desc("Compute Silhouette metric").build());
        opts.addOption(Option.builder().longOpt("rand-index-adjusted-unsupervised").desc("Compute Unsupervised Adjusted Rand index").build());
        opts.addOption(Option.builder().longOpt("rand-index-unsupervised").desc("Compute Unsupervised Rand index").build());
        opts.addOption(Option.builder().longOpt("1nn-consistency").desc("Compute 1NN correct assignment (1NN should be in the same cluster").build());
        
        // Build vocabulary
        opts.addOption("b", "build", false, "build Motion Vocabulary using the Voronoi partitioning (option -v is required here).\nThe output object has DimensionObjectKey set. It carries the visual words.");
        opts.addOption(null, "quantize", false, "build Motion Vocabulary using the Voronoi partitioning (option -v/-mindex/-mmindex is required here).\nThe output object is ObjectFeatureQuantized and it is the visual word(s).");
        opts.addOption(Option.builder("o").longOpt("output").desc("file name where the built vocabulary is stored (new content is appended to the file).")
                        .hasArg().argName("PATH").build());
        opts.addOption(null, "tosequence", false, "if present, MotionWords are packed into SequenceMotionWords and this object is returned in quantization.");
        opts.addOption(Option.builder().longOpt("k-closest").desc("number of closest cell to report as motion words (aka soft-assignment); default is 1.")
                        .hasArg().argName("NUMBER").type(Number.class).build());
        opts.addOption(Option.builder().longOpt("soft-assign").desc("setting of soft-assignment: "
                                + "RaaaKbb for ratio of 'aaa' on distances between 1NN and xNN to use to detect a close cell (value must be in [0.0-1.0]), max assigned words is 'bb'; "
                                + "DaaaKbb for exact difference of 'aaa' on distances between 1NN and xNN to use to detect a close cell, max assigned words is 'bb'.")
                        .hasArg().argName("SOFTRATIO").type(String.class).build());
        
        // Parallelism (not supported by all functions!)
        opts.addOption(Option.builder().longOpt("threads").desc("Number of threads to be used in computations.")
                        .hasArg().argName("NUMBER").type(Number.class).build());
        
        // Clusters by Ground Truth
        opts.addOption("gt", "ground-truth", false, "Split the input database into files titled by class ID that has been extracted from object keys. The files are stored under the output directory which is specified by the option -o or -output.");
        
        // Composite MW -- joint filter
        opts.addOption(Option.builder("filter").longOpt("composite-mw-filter").desc("comma-separated list of joint  used during vocabulary creation")
                        .hasArg().argName("NUMBERS").build());

        return opts;
    }
    
    private static String dataSet;
    private static Class<LocalAbstractObject> objClass;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        try {
            CommandLine cmd = new DefaultParser().parse(opts, args);

            // Composite MW
            processFilterOption(cmd);

            // Process command line args
            dataSet = cmd.getOptionValue('d');
            // Object class
            objClass = (Class<LocalAbstractObject>)Class.forName(cmd.getOptionValue('c', OBJECT_CLASS_NEURAL_NET));

            // Print stats
            if (cmd.hasOption('s')) {
                if (cmd.getOptionValues('v') == null)
                    throw new ParseException("Missing argument to option -v");
                for (String pivotFile : cmd.getOptionValues('v')) {
                    String hkPivots = (String)cmd.getParsedOptionValue("hkmeans-pivots");
                    computeStats(cmd, dataSet, pivotFile, (String)cmd.getParsedOptionValue("thresholds"), hkPivots);
                }
                return;
            }
            
            // Build vocabulary
            if (cmd.hasOption('b')) {
                if (cmd.getOptionValues('v') == null)
                    throw new ParseException("Missing argument to option -v");
                for (String pivotFile : cmd.getOptionValues('v'))
                    try { createVocabulary(dataSet, pivotFile, cmd.getOptionValue("output")); }
                    catch (IOException ex) {
                        System.err.println("# ERROR: Failed to create vocabulary for pivots " + pivotFile);
                        System.err.println(ex.getMessage());
                    }
                return;
            }
            if (cmd.hasOption("quantize")) {
                if (cmd.getOptionValues('v') == null && cmd.getOptionValue("mindex") == null && cmd.getOptionValue("mmindex") == null
                        && cmd.getOptionValue("hull") == null && cmd.getOptionValue("balls") == null)
                    throw new ParseException("Missing argument to option -v or --mindex or --mmindex or -hull or -balls");
                if (cmd.getOptionValues('v') != null) {
                    if (cmd.hasOption("threads")) {
                        int threads = ((Number)cmd.getParsedOptionValue("threads")).intValue();
                        VoronoiPartitioning.PARALLEL_DISTANCE_COMPUTING = threads;// setParallelDistanceComputing(threads);
                        System.out.println(new Date().toString() + ": Voronoi executor threads: " + threads);
                        Statistics.disableGlobally();
                    }
                    for (String pivotFile : cmd.getOptionValues('v'))
                        try { createVocabularyAsFeature(dataSet, pivotFile, (Number)cmd.getParsedOptionValue("k-closest"), (String)cmd.getParsedOptionValue("soft-assign"), 
                                                cmd.hasOption("tosequence"), cmd.getOptionValue("output")); }
                        catch (IOException ex) {
                            System.err.println("# ERROR: Failed to create vocabulary for pivots " + pivotFile);
                            System.err.println(ex.getMessage());
                        }
                }
                if (cmd.getOptionValues("mindex") != null) {
                    for (String mindexBin : cmd.getOptionValues("mindex")) {
                        try { quantizeByIndex(dataSet, mindexBin, (Number)cmd.getParsedOptionValue("k-closest"), (String)cmd.getParsedOptionValue("soft-assign"),
                                              cmd.hasOption("tosequence"), cmd.getOptionValue("output")); }
                        catch (IOException | NullPointerException | ClassNotFoundException | AlgorithmMethodException | NoSuchMethodException ex) {
                            System.err.println("# ERROR: Failed to load m-index vocabulary from " + mindexBin);
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        }
                    }
                }
                if (cmd.getOptionValues("mmindex") != null) {
                    for (String bin : cmd.getOptionValues("mmindex")) {
                        try { quantizeByMultipleIndex(dataSet, bin, (Number)cmd.getParsedOptionValue("k-closest"),
                                              cmd.hasOption("tosequence"), cmd.getOptionValue("output")); }
                        catch (IOException | NullPointerException | ClassNotFoundException | AlgorithmMethodException | CloneNotSupportedException | NoSuchMethodException ex) {
                            System.err.println("# ERROR: Failed to load mm-index vocabulary from " + bin);
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        }
                    }
                }
                if (cmd.getOptionValues("hull") != null) {
                    if (cmd.hasOption("threads")) {
                        int threads = ((Number)cmd.getParsedOptionValue("threads")).intValue();
                        HullVocabulary.PARALLEL_COMPUTING = threads;
                        System.out.println(new Date().toString() + ": HullVocabulary threads: " + threads);
                        Statistics.disableGlobally();
                    }
                    for (String path : cmd.getOptionValues("hull")) {
                        try { 
                            String[] pair = path.split(":");
                            HullVocabulary voc;
                            if (pair.length == 1)
                                voc = new HullVocabulary(path, objClass);
                            else
                                voc = new HullCenterVocabulary(pair[0], pair[1], objClass);
                            quantizeByHulls(dataSet, voc, (Number)cmd.getParsedOptionValue("k-closest"), cmd.hasOption("tosequence"), cmd.getOptionValue("output")); 
                        } catch (IOException ex) {
                            System.err.println("# ERROR: Failed to create hull vocabulary from hull files on path " + path);
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        }
                    }
                }
                if (cmd.getOptionValues("balls") != null) {
                    for (String param : cmd.getOptionValues("balls")) {
                        String[] pair = param.split(":");
                        try { quantizeByBalls(dataSet, pair[0], pair[1], (Number)cmd.getParsedOptionValue("k-closest"), cmd.hasOption("tosequence"), cmd.getOptionValue("output")); }
                        catch (IOException ex) {
                            System.err.println("# ERROR: Failed to create snake (balls) vocabulary from center file " + pair[0] + " and data files on path " + pair[1]);
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        }
                    }
                }
                return;
            }
            // Partition data into clusters based on class ID
            if (cmd.hasOption("gt")) {
                if (cmd.getOptionValue("output") == null)
                    System.err.println("Cannot split into class files. No output directory (-o) given.");
                else {
                    splitByGroundTruth(dataSet, cmd.getOptionValue("output"));
                }
            }
            
        //selectTruePivots(DB_WHOLE_MOTIONS);
       
        } catch (ParseException ex) {
            System.out.println(ex.getMessage()); System.out.println();
            HelpFormatter help = new HelpFormatter();
            help.setWidth(150);
            help.printHelp(MotionVocabulary.class.getSimpleName(), "Tools to experiment with Motion Vocabulary.\n\n", opts, "", true);
            
        } catch (ClassNotFoundException | ClassCastException ex) {
            System.out.println(ex.getMessage());
            Logger.getLogger(MotionVocabulary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Processes the {@code filter} option.
     * Parses the inputted joints IDs and sets them to the joint filter 
     * {@link ObjectMocapPoseCoordsL2Filtered#jointIds}.
     *
     * @param cmd the command line arguments
     * @author David Procházka
     */
    private static void processFilterOption(CommandLine cmd) {
        String inputIds = cmd.getOptionValue("filter");
        
        ObjectMocapPoseCoordsL2Filtered.jointIds = parseIds(inputIds);

        System.out.println("Supplied joint IDs: " + ObjectMocapPoseCoordsL2Filtered.jointIds);
    }

    /**
     * Parses the command line joints IDs and returns them as a set of integers.
     * If the input is {@code null}, returns an empty set.
     * 
     * @param ids string of comma separated joints IDs
     * @return set of integer joint IDs
     * @author David Procházka
     */
    private static Set<Integer> parseIds(String ids) {
        if (ids == null) {
            return Collections.emptySet();
        }
        
        return Arrays
                .stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private static void selectTruePivots(String dataFile) {
        try {
            Map<Integer,AbstractObjectList<LocalAbstractObject>> clusters = new HashMap<>();
            
            StreamGenericAbstractObjectIterator<ObjectFloatVectorNeuralNetworkL2> iter = ClusteringUtils.openDatabaseNeuralNet(dataFile);
            while (iter.hasNext()) {
                ObjectFloatVectorNeuralNetworkL2 o = iter.next();
                Integer classId = MotionIdentification.getMotionClassID(o.getLocatorURI());
                AbstractObjectList<LocalAbstractObject> lst = clusters.get(classId);
                if (lst == null) {
                    lst = new AbstractObjectList<>();
                    clusters.put(classId, lst);
                }
                lst.add(o);
            }
            iter.close();

            BufferedOutputStream outputFile = new BufferedOutputStream(System.out);
            KMeansPivotChooser.PIVOTS_SAMPLE_SIZE = Integer.MAX_VALUE;
            
            int pivotId = 999000;
            for (AbstractObjectList<LocalAbstractObject> cluster : clusters.values()) {
                int classId = MotionIdentification.getMotionClassID(cluster.get(0).getLocatorURI());
                //KMeansPivotChooser.CenterThread thread = new KMeansPivotChooser.SelectClustroidThread(cluster, cluster.get(0));
                KMeansPivotChooser.CenterThread thread = new KMeansPivotChooser.ComputeCentroidThread(cluster, cluster.get(0));
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LocalAbstractObject clustroid = thread.getClustroid();
                clustroid.setObjectKey(new AbstractObjectKey(MotionIdentification.createMotionLocator(""+pivotId, classId)));
                clustroid.write(outputFile);
                pivotId++;
            }
            outputFile.close();
            
        } catch (IOException ex) {
            Logger.getLogger(MotionVocabulary.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /** Set the Vocabulary's word ID in the object key (DimensionKey instance) */
    public static void createVocabulary(String databaseFile, String pivotFile, String outputFile) throws IOException {
        // Fake pivot chooser (its implementation is not used -- it only encapsulates the pivos passed in the parameter)
        RandomPivotChooser chooser = new RandomPivotChooser();
        // Read pivots
        StreamGenericAbstractObjectIterator<LocalAbstractObject> iter = ClusteringUtils.openDB(objClass, pivotFile);//openDatabaseNeuralNet(pivotFile);
        while (iter.hasNext())
            chooser.addPivot(iter.next());
        iter.close();

        // Output to print transformed objects
        BufferedOutputStream output = new BufferedOutputStream((outputFile == null) ? System.out : new FileOutputStream(outputFile, true));
        
        // Do Voronoi partitioning
        iter = ClusteringUtils.openDB(objClass, databaseFile);//openDatabaseNeuralNet(databaseFile);
        while (iter.hasNext()) {
            LocalAbstractObject obj = iter.next();
            int partId = VoronoiPartitioning.getClosestPivotIndex(chooser, obj);
            obj.setObjectKey(new DimensionObjectKey(obj.getLocatorURI(), new int[] { partId }));
            obj.write(output);
        }
        output.close();
    }
    
    /** Converts the object to {@link ObjectFeatureQuantized}.
     * @param databaseFile data object to quantize
     * @param pivotFile list of pivots used to create Voronoi partitioning
     * @param kClosestCells number of words to report to each object (k-nearest cells (aka pivots here))
     * @param softAssignment soft assignment settings (takes precedence over kClosestCells), format DxxKyy -- D distance threshold xx on difference between distances to the pivots; K max. number of yy nearest pivots/cells to get.
     * @param toSequences if false, {@link ObjectMotionWord} is produced; if true, {@link SequenceMotionWords} is produced (depends on the order of individual objects in the operation!)
     */
    public static void createVocabularyAsFeature(String databaseFile, String pivotFile, Number kClosestCells, String softAssignment,
                                                boolean toSequences, String outputFile) throws IOException {
        // Fake pivot chooser (its implementation is not used -- it only encapsulates the pivos passed in the parameter)
        RandomPivotChooser chooser = new RandomPivotChooser();
        // Read pivots
        StreamGenericAbstractObjectIterator<LocalAbstractObject> iter = ClusteringUtils.openDB(objClass, pivotFile);//openDatabaseNeuralNet(pivotFile);
        while (iter.hasNext())
            chooser.addPivot(iter.next());
        iter.close();

        if (softAssignment == null)
            softAssignment = "D0K" + ((kClosestCells == null) ? 1 : kClosestCells.intValue());
        
        // Output to print transformed objects
        System.out.println("Writing output to " + ((outputFile == null) ? "stdout" : outputFile));
        BufferedOutputStream output = new BufferedOutputStream((outputFile == null) ? System.out : new FileOutputStream(outputFile, true));
        String lastMotion = null;
        List<ObjectMotionWord> words = new ArrayList<>();
        // Do Voronoi partitioning
        int cnt = 0;
        iter = ClusteringUtils.openDB(objClass, databaseFile);//openDatabaseNeuralNet(databaseFile);
        while (iter.hasNext()) {
            QuantizeOperation oper = new QuantizeOperation(iter, 1);
            setOperationForSoftQuantization(softAssignment, oper);
            ObjectFeatureQuantized objQuantized = evaluateQuantizeOperation(oper, chooser);
            cnt++;
            
//            LocalAbstractObject obj = oper.getQueryObject(0);
//            oper.getParameter(SoftQuantizeOperationNavigationProcessor.PARAM_DIST, Float.class, 0f)
//            cnt++;
//            long[] partIds = VoronoiPartitioning.getKClosestPivotsIndexes(chooser, 
//                                    oper.getParameter(SoftQuantizeOperationNavigationProcessor.PARAM_MAX, Integer.class, 0) + 1, 
//                                    obj);
//            ObjectFeatureQuantized objQuantized = new ObjectFeatureQuantized(0, 0, 0, 0, partIds);
//            objQuantized.setObjectKey(obj.getObjectKey());

            ObjectMotionWord word = convertFeatureToWord(objQuantized);
            if (toSequences) {  // pack to sequences
                String currMotion = MotionIdentification.stripSegmentFromLocator(word.getLocatorURI());
                if (lastMotion != null && !lastMotion.equals(currMotion)) {
                    // New sequence, so send it to output!
                    printSequence(lastMotion, words, output);
                    // Start new sequence
                    words.clear();
                }
                words.add(word);
                lastMotion = currMotion;
            } else {    // output individual words
                word.write(output);
            }
            if (cnt % 1000 == 0)
                System.out.println(new Date().toString() + ": Objects quantized: " + cnt);
        }
        if (lastMotion != null)
            printSequence(lastMotion, words, output);
        output.close();
    }
    
    /** Converts the object to {@link ObjectFeatureQuantized}.
     * @param databaseFile data object to quantize
     * @param mindexBin binary serialized file of M-Index
     * @param kClosestCells NOT USED!!! number of words to report to each object (k-nearest cells (aka pivots here)) -- not used here yet (to be implemented in M-index's query...)
     * @param toSequences if false, {@link ObjectMotionWord} is produced; if true, {@link SequenceMotionWords} is produced (depends on the order of individual objects in the operation!)
     */
    public static void quantizeByIndex(String databaseFile, String mindexBin, Number kClosestCells, String softAssignment, 
                                        boolean toSequences, String outputFile) 
                            throws IOException, NullPointerException, ClassNotFoundException, AlgorithmMethodException, NoSuchMethodException {
        Algorithm mindex = Algorithm.restoreFromFile(mindexBin);
        
        StreamGenericAbstractObjectIterator<LocalAbstractObject> dbIter = ClusteringUtils.openDB(objClass, databaseFile);
        
        // Output to print transformed objects
        BufferedOutputStream output = new BufferedOutputStream((outputFile == null) ? System.out : new FileOutputStream(outputFile, true));
        String lastMotion = null;
        List<ObjectMotionWord> words = new ArrayList<>();
        while (dbIter.hasNext()) {
            QuantizeOperation oper = new QuantizeOperation(dbIter, 1);
            setOperationForSoftQuantization(softAssignment, oper);
            
            mindex.executeOperation(oper);
            // Process the quantized object
            ObjectFeatureQuantized[] objs = toArrayOfFeatures(oper);
            ObjectMotionWord word = convertFeaturesToWord(objs);
//            // Process the quantized object (Version original: one-word per object)
//            RankedAbstractObject rankedObj = oper.getAnswer().next();
//            ObjectFeatureQuantized obj = (ObjectFeatureQuantized)rankedObj.getObject();
//            ObjectMotionWord word = convertFeatureToWord(obj);
            if (toSequences) {  // pack to sequences
                String currMotion = MotionIdentification.stripSegmentFromLocator(word.getLocatorURI());
                if (lastMotion != null && !lastMotion.equals(currMotion)) {
                    // New sequence, so send it to output!
                    printSequence(lastMotion, words, output);
                    // Start new sequence
                    words.clear();
                }
                words.add(word);
                lastMotion = currMotion;
            } else {    // output individual words
                word.write(output);
            }
        }
        if (lastMotion != null)
            printSequence(lastMotion, words, output);
        output.close();
    }

    private static void setOperationForSoftQuantization(String softAssignment, QuantizeOperation oper) throws NumberFormatException {
        char softType = softAssignment.charAt(0);
        int posOfK = softAssignment.toUpperCase().indexOf('K');
        double softParam = Double.parseDouble(softAssignment.substring(1,posOfK));
        int maxK = Integer.parseInt(softAssignment.substring(posOfK + 1));
        
        if (softAssignment != null && softAssignment.length() >= 4) {
            oper.setParameter(SoftQuantizeOperationNavigationProcessor.PARAM_TYPE, SoftQuantizeOperationNavigationProcessor.PARAM_TYPE_SOFT);
            if (softType == 'd' || softType == 'D')
                oper.setParameter(SoftQuantizeOperationNavigationProcessor.PARAM_DIST, softParam);
            else if (softType == 'R' || softType == 'r')
                oper.setParameter(SoftQuantizeOperationNavigationProcessor.PARAM_RATIO, softParam);
            oper.setParameter(SoftQuantizeOperationNavigationProcessor.PARAM_MAX, maxK - 1);
        }
    }

    /** Converts the object to {@link ObjectFeatureQuantized}.
     * @param databaseFile data object to quantize
     * @param bin binary serialized file of M-Index
     * @param kClosestCells NOT USED!!! number of words to report to each object (k-nearest cells (aka pivots here)) -- not used here yet (to be implemented in M-index's query...)
     * @param toSequences if false, {@link ObjectMotionWord} is produced; if true, {@link SequenceMotionWords} is produced (depends on the order of individual objects in the operation!)
     */
    public static void quantizeByMultipleIndex(String databaseFile, String bin, Number kClosestCells, boolean toSequences, String outputFile) throws IOException, NullPointerException, ClassNotFoundException, AlgorithmMethodException, NoSuchMethodException, CloneNotSupportedException {
        MultipleMIndexAlgorithm mmindex = (MultipleMIndexAlgorithm)Algorithm.restoreFromFile(bin);
        
        StreamGenericAbstractObjectIterator<LocalAbstractObject> dbIter = ClusteringUtils.openDB(objClass, databaseFile);
        
        // Output to print transformed objects
        BufferedOutputStream output = new BufferedOutputStream((outputFile == null) ? System.out : new FileOutputStream(outputFile, true));
        String lastMotion = null;
        List<ObjectMotionWord> words = new ArrayList<>();
        while (dbIter.hasNext()) {
            LocalAbstractObject objToQuantize = dbIter.next();
            QuantizeOperation[] opers = new QuantizeOperation[mmindex.getAlgorithmsCount()];
            opers[0] = new QuantizeOperation(new LocalAbstractObject[] { objToQuantize });
            for (int i = 1; i < mmindex.getAlgorithmsCount(); i++)
                opers[i] = new QuantizeOperation(new LocalAbstractObject[] { objToQuantize.clone() });
            // Execute them
            for (int i = 0; i < mmindex.getAlgorithmsCount(); i++)
                mmindex.getAlgorithm(i).executeOperation(opers[i]);
            // Process the quantized object
            ObjectFeatureQuantized[] objs = toArrayOfFeatures(opers);
            ObjectMotionWord word = convertFeaturesToWord(objs);
            if (toSequences) {  // pack to sequences
                String currMotion = MotionIdentification.stripSegmentFromLocator(word.getLocatorURI());
                if (lastMotion != null && !lastMotion.equals(currMotion)) {
                    // New sequence, so send it to output!
                    printSequence(lastMotion, words, output);
                    // Start new sequence
                    words.clear();
                }
                words.add(word);
                lastMotion = currMotion;
            } else {    // output individual words
                word.write(output);
            }
        }
        if (lastMotion != null)
            printSequence(lastMotion, words, output);
        output.close();
    }
    
    /** Converts the object to {@link ObjectFeatureQuantized}.
     * @param databaseFile data object to quantize
     * @param path path to *.hull files to create hulls from
     * @param kClosestCells number of words to report to each object (k-nearest cells (aka pivots here)), default is {@link Integer#MAX_VALUE}.
     * @param toSequences if false, {@link ObjectMotionWord} is produced; if true, {@link SequenceMotionWords} is produced (depends on the order of individual objects in the operation!)
     */
    public static void quantizeByHulls(String databaseFile, HullVocabulary voc, Number kClosestCells, boolean toSequences, String outputFile) 
                            throws IOException {
        
        StreamGenericAbstractObjectIterator<LocalAbstractObject> dbIter = ClusteringUtils.openDB(objClass, databaseFile);
        
        int maxWords = (kClosestCells != null) ? kClosestCells.intValue() : Integer.MAX_VALUE;
        
        // Output to print transformed objects
        BufferedOutputStream output = new BufferedOutputStream((outputFile == null) ? System.out : new FileOutputStream(outputFile, true));
        String lastMotion = null;
        List<ObjectMotionWord> words = new ArrayList<>();
        while (dbIter.hasNext()) {
            final LocalAbstractObject o = dbIter.next();
            ObjectMotionWord word = voc.quantize(o, maxWords);
            if (word == null)
                continue;

            if (toSequences) {  // pack to sequences
                String currMotion = MotionIdentification.stripSegmentFromLocator(word.getLocatorURI());
                if (lastMotion != null && !lastMotion.equals(currMotion)) {
                    // New sequence, so send it to output!
                    printSequence(lastMotion, words, output);
                    // Start new sequence
                    words.clear();
                }
                words.add(word);
                lastMotion = currMotion;
            } else {    // output individual words
                word.write(output);
            }
        }
        if (lastMotion != null)
            printSequence(lastMotion, words, output);
        output.close();
    }    

    /** Converts the object to {@link ObjectFeatureQuantized}.
     * @param databaseFile data object to quantize
     * @param path path to *.hull files to create hulls from
     * @param kClosestCells number of words to report to each object (k-nearest cells (aka pivots here)), default is {@link Integer#MAX_VALUE}.
     * @param toSequences if false, {@link ObjectMotionWord} is produced; if true, {@link SequenceMotionWords} is produced (depends on the order of individual objects in the operation!)
     */
    public static void quantizeByBalls(String databaseFile, String centers, String path, Number kClosestCells, boolean toSequences, String outputFile) 
                            throws IOException {
        System.out.println("Snake Vocabulary: centers " + centers + ", path " + path);
        SnakeVocabulary voc = new SnakeVocabulary(centers, path, objClass);
        
        StreamGenericAbstractObjectIterator<LocalAbstractObject> dbIter = ClusteringUtils.openDB(objClass, databaseFile);
        
        int maxWords = (kClosestCells != null) ? kClosestCells.intValue() : Integer.MAX_VALUE;
        
        // Output to print transformed objects
        BufferedOutputStream output = new BufferedOutputStream((outputFile == null) ? System.out : new FileOutputStream(outputFile, true));
        String lastMotion = null;
        List<ObjectMotionWord> words = new ArrayList<>();
        while (dbIter.hasNext()) {
            final LocalAbstractObject o = dbIter.next();
            ObjectMotionWord word = voc.quantize(o, maxWords);
            if (word == null)
                continue;

            if (toSequences) {  // pack to sequences
                String currMotion = MotionIdentification.stripSegmentFromLocator(word.getLocatorURI());
                if (lastMotion != null && !lastMotion.equals(currMotion)) {
                    // New sequence, so send it to output!
                    printSequence(lastMotion, words, output);
                    // Start new sequence
                    words.clear();
                }
                words.add(word);
                lastMotion = currMotion;
            } else {    // output individual words
                word.write(output);
            }
        }
        if (lastMotion != null)
            printSequence(lastMotion, words, output);
        output.close();
    }    

    private static void printSequence(String locator, List<ObjectMotionWord> words, BufferedOutputStream output) throws IOException {
        //SequenceMotionWords<ObjectMotionWord> seq = new SequenceMotionWords<ObjectMotionWord>((Class<List<ObjectMotionWord>>)words.getClass(), null, words);
        SequenceMotionWordsDTW seq = new SequenceMotionWordsDTW(words);
        seq.setObjectKey(new AbstractObjectKey(locator));
        seq.write(output);
    }
    
    
    
    private static void computeStats(CommandLine cmd, String dataFile, String pivotFile, String thresholdsStr, String hkPivotCounts) throws ParseException {
        float[] thresholds = RandIndex.parseThresholds(thresholdsStr);
        
        System.out.println("Data file: " + dataFile);
        System.out.println("Pivot file: " + pivotFile);
        System.out.println("Object class: " + objClass.getName());
        System.out.println("Thresholds: " + thresholdsStr);
        if (hkPivotCounts != null)
            System.out.println("Hierarchical k-means - pivots per level: " + hkPivotCounts);
        try {
            //AbstractPivotChooser chooser = new KMeansPivotChooser();
            AbstractObjectList<LocalAbstractObject> database = new AbstractObjectList<>();
            //Map<Integer,AbstractObjectList<LocalAbstractObject>> clusters = new HashMap<>();

            // Read database
            StreamGenericAbstractObjectIterator<LocalAbstractObject> iter = ClusteringUtils.openDB(objClass, dataFile);//openDatabaseNeuralNet(dataFile);
            while (iter.hasNext()) {
                LocalAbstractObject o = iter.next();
                database.add(o);
//                Integer classId = MotionIdentification.getMotionClassID(o.getLocatorURI());
//                AbstractObjectList<LocalAbstractObject> lst = clusters.get(classId);
//                if (lst == null) {
//                    lst = new AbstractObjectList<>();
//                    clusters.put(classId, lst);
//                }
//                lst.add(o);
            }
            iter.close();
            
            // Read pivots
//            iter = ClusteringUtils.openDB(objClass, pivotFile);//openDatabaseNeuralNet(pivotFile);
//            while (iter.hasNext())
//                chooser.addPivot(iter.next());
//            iter.close();
            
            // Do Voronoi partitioning
            //VoronoiPartitioning.Result partitions = VoronoiPartitioning.doVoronoiPartitioning(chooser, database);
            iter = ClusteringUtils.openDB(objClass, pivotFile);
            VoronoiPartitioning.Result partitions;
            if (hkPivotCounts == null)
                partitions = VoronoiPartitioning.doVoronoiPartitioning(iter, -1, database);
            else
                partitions = RecursiveVoronoiPartitioning.doVoronoiPartitioning(iter, hkPivotCounts, database);
            iter.close();
            
            // Distance matrix
            DistanceMatrix dm = new DistanceMatrix(database);
            
            // 1NN consistency in cluster assignment
            if (cmd.hasOption("1nn-consistency"))
                NearestNeighborConsistency.consistencyNN(database, partitions.getClusterIds());

            // Compute Silhouette
            if (cmd.hasOption("silhouette"))
                SilhouetteIndex.compute(dm, partitions.getClusterIds(), partitions.getParts().size());

            // Rand indexes
            if (cmd.hasOption("rand-index-unsupervised") || cmd.hasOption("rand-index-adjusted-unsupervised")) {
                // Get Ground Truth & random clusterings
    //            VoronoiPartitioning.Result gtClustering = ClusteringUtils.getGroundTruthClustering(ClusteringUtils.openDB(objClass, dataFile));
                short[] randClustering = RandIndex.randomClustering(database.size(), partitions.getParts().size());
    //            short[] randClustering2 = randomClustering(database.size(), partitions.getParts().size());
                dm.performanceTest();
                RandIndex.computeUnsupervisedAdjustedAndOriginalRandIndex(dm, partitions.getClusterIds(), partitions.getParts().size(), 
                           randClustering, partitions.getParts().size(), thresholds);
            }


// DBG: all other combinations
//            System.out.println("random");
//            SilhouetteIndex.compute(database, randClustering, partitions.getParts().size());
//            System.out.println("gt");
//            SilhouetteIndex.compute(database, gtClustering.getClusterIds(), gtClustering.getParts().size());
            
            // Compute Rand index
//            RandIndex.CategoryExtractor motionCategory = new RandIndex.CategoryExtractor() {
//                @Override
//                public Integer getCategoryFromLocator(String locator) {
//                    return MotionIdentification.getMotionClassID(locator);
//                }
//            };
//            RandIndex.computeRandIndex(database, partitions.getClusterIds(), partitions.getParts().size(), motionCategory);
// DBG: all other combinations
//            System.out.println("random");
//            RandIndex.computeRandIndex(database, randClustering, partitions.getParts().size()), motionCategory;
//            System.out.println("gt");
//            RandIndex.computeRandIndex(database, gtClustering.getClusterIds(), gtClustering.getParts().size(), motionCategory);

            // Compute Adjusted Rand index
            //System.out.println("clustering vs. gt");
//            RandIndex.computeAdjustedRandIndex(database, partitions.getClusterIds(), partitions.getParts().size(), 
//                                                gtClustering.getClusterIds(), gtClustering.getParts().size());
// DBG: all other combinations
//            System.out.println("clustering vs. random");
//            RandIndex.computeAdjustedRandIndex(database, partitions.getClusterIds(), partitions.getParts().size(), 
//                                                randClustering, partitions.getParts().size());
//            System.out.println("random vs. gt");
//            RandIndex.computeAdjustedRandIndex(database, randClustering, partitions.getParts().size(), 
//                                                gtClustering.getClusterIds(), gtClustering.getParts().size());
//            System.out.println("gt vs. gt");
//            RandIndex.computeAdjustedRandIndex(database, gtClustering.getClusterIds(), gtClustering.getParts().size(), 
//                                                gtClustering.getClusterIds(), gtClustering.getParts().size());
  
            // Unsupervised Rand Index
//            if (cmd.hasOption("rand-index-unsupervised")) {
//                //if (thresholdSimilar == null)
//                //    throw new ParseException("Missing argument to stats: --threshold-similar");
//                RandIndex.computeUnsupervisedRandIndex(database, partitions.getClusterIds(), partitions.getParts().size(), thresholdSim, thresholdDis);
//            }
// DBG: all other combinations
//            System.out.println("random");
//            RandIndex.computeUnsupervisedRandIndex(database, randClustering, partitions.getParts().size(), thresholdSim, thresholdDis);
//            System.out.println("gt");
//            RandIndex.computeUnsupervisedRandIndex(database, gtClustering.getClusterIds(), gtClustering.getParts().size(), thresholdSim, thresholdDis);

            // Unsupervised Adjusted Rand Index
//            if (cmd.hasOption("rand-index-adjusted-unsupervised"))
//                RandIndex.computeUnsupervisedAdjustedRandIndex(database, partitions.getClusterIds(), partitions.getParts().size(), thresholdSim, thresholdDis,
//                                                               randClustering, partitions.getParts().size());
// DBG: all other combinations
//            System.out.println("random");
//            RandIndex.computeUnsupervisedAdjustedRandIndex(database, randClustering, partitions.getParts().size(), thresholdSim, thresholdDis,
//                                                            randClustering2, partitions.getParts().size());
//            System.out.println("gt");
//            RandIndex.computeUnsupervisedAdjustedRandIndex(database, gtClustering.getClusterIds(), gtClustering.getParts().size(), thresholdSim, thresholdDis,
//                                                           randClustering, partitions.getParts().size());

            // Unsupervised DISA index (like unsupervised Adjusted Rand Index, where the number of true negatives is limited to the number of true positives)
//            RandIndex.computeUnsupervisedDisaIndex(database, partitions.getClusterIds(), partitions.getParts().size(), thresholdSim, thresholdDis);
// DBG: all other combinations
//            System.out.println("random");
//            RandIndex.computeUnsupervisedDisaIndex(database, randClustering, partitions.getParts().size(), thresholdSim, thresholdDis);
//            System.out.println("gt");
//            RandIndex.computeUnsupervisedDisaIndex(database, gtClustering.getClusterIds(), gtClustering.getParts().size(), thresholdSim, thresholdDis);

        } catch (IOException ex) {
            Logger.getLogger(MotionVocabulary.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    private static final float THRESHOLD_SIMILAR = 14.0f;
    private static final float THRESHOLD_DISSIMILAR = 27.0f;

    private static ObjectMotionWord convertFeatureToWord(ObjectFeatureQuantized obj) {
        return new ObjectMotionWord(obj.getLocatorURI(), obj.getKeys());
    }

    private static ObjectMotionWord convertFeaturesToWord(ObjectFeatureQuantized[] features) {
        int cnt = 0;
        for (ObjectFeatureQuantized of : features)
            cnt += of.getKeys().length;
        
        long keys[] = new long[cnt];
        int start = 0;
        for (ObjectFeatureQuantized of : features) {
            int len = of.getKeys().length;
            System.arraycopy(of.getKeys(), 0, keys, start, len);
            start += len;
        }
        
        return new ObjectMotionWord(features[0].getLocatorURI(), keys);
    }

    private static ObjectFeatureQuantized[] toArrayOfFeatures(QuantizeOperation[] opers) {
        ObjectFeatureQuantized[] res = new ObjectFeatureQuantized[opers.length];
        for (int i = 0; i < opers.length; i++)
            res[i] = (ObjectFeatureQuantized)opers[i].getAnswer().next().getObject();
        return res;
    }

    private static ObjectFeatureQuantized[] toArrayOfFeatures(QuantizeOperation oper) {
        ObjectFeatureQuantized[] res = new ObjectFeatureQuantized[oper.getAnswerCount()];
        int i = 0;
        for (Iterator<RankedAbstractObject> iterator = oper.getAnswer(); iterator.hasNext();) {
            RankedAbstractObject next = iterator.next();
            res[i++] = (ObjectFeatureQuantized)next.getObject();
        }
        return res;
    }

    /** Filter the identified (closest Voronoi cells) by the distance difference from the closest Voronoi cell */
    private static ObjectFeatureQuantized evaluateQuantizeOperation(QuantizeOperation oper, RandomPivotChooser pivots) {
        Double distTh = oper.getParameter(SoftQuantizeOperationNavigationProcessor.PARAM_DIST, Double.class);
        float distThreshold = (distTh == null) ? 0f : distTh.floatValue();
        int maxK = oper.getParameter(SoftQuantizeOperationNavigationProcessor.PARAM_MAX, Integer.class, 0) + 1;
        AbstractObjectKey key = oper.getQueryObject(0).getObjectKey();
        
        final VoronoiPartitioning.DistanceAndPivotIndexPair[] pairs = VoronoiPartitioning.orderPivotsByDistance(pivots, oper.getQueryObject(0));
        float distClosest = pairs[0].distance;
        long[] partIds = new long[maxK];
        int curPart = -1;
        for (VoronoiPartitioning.DistanceAndPivotIndexPair p : pairs) {
            if (p.distance - distClosest <= distThreshold) {
                partIds[++curPart] = p.index;
                if (curPart + 1 == maxK)
                    break;
            }
        }
        if (curPart + 1 < maxK)
            partIds = Arrays.copyOf(partIds, curPart + 1);
        ObjectFeatureQuantized objQuantized = new ObjectFeatureQuantized(0, 0, 0, 0, partIds);
        objQuantized.setObjectKey(key);
        return objQuantized;
    }

    private static void splitByGroundTruth(String dataSet, String outDir) {
        File dir = new File(outDir);
        dir.mkdirs();
        VoronoiPartitioning.Result clustering = ClusteringUtils.getGroundTruthClustering(ClusteringUtils.openDB(objClass, dataSet));
        final Iterator<Integer> classes = clustering.getPartsClasses().iterator();
        final Iterator<VoronoiPartitioning.VoronoiPartition> parts = clustering.getParts().iterator();
        while (classes.hasNext()) {
            File outF = new File(outDir, "class-" + classes.next() + ".data");
            try (OutputStream out = new FileOutputStream(outF)) {
                final Iterator<LocalAbstractObject> objIter = parts.next().getMembers();
                while (objIter.hasNext())
                    objIter.next().write(out);
            } catch (IOException ex) {
                System.err.println("# ERROR: Failed to write to file " + outF.getAbsolutePath());
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
    
}
