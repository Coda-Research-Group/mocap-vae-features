/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import messif.objects.impl.ObjectFloatVectorNeuralNetworkL2;
import messif.objects.keys.DimensionObjectKey;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.pivotselection.RandomPivotChooser;
import messif.utility.ClusteringUtils;
import messif.utility.VoronoiPartitioning;

/**
 * Builds a vocabulary based on the passed parameters. Run without params to get help.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class BuildVocabulary {
    
    private void help() {
        System.out.println("Usage: " + getClass().getSimpleName() + " <pivot_file> <database_file>"); 
        System.out.println("     <pivot_file> -- contains pivots (centers of clusters)");
        System.out.println("     <database_file> -- objects are read from and assigned to Voronoi cells. The cell's ID is assigned to the object.");
        System.out.println("   Initializes Voronoi partitioning using the passed pivots and converts all database objects");
        System.out.println("   by setting the DimensionObjectKey to them. The result objects are printed to stdout.");
    }
    
    /**
     * Builds a vocabulary based on the passed parameters. Run without params to get help.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        BuildVocabulary c = new BuildVocabulary();
        if (args.length == 0) {
            c.help();
            return;
        }
        
        MotionVocabulary.createVocabulary(args[1], args[0], null);
    }
    
}
