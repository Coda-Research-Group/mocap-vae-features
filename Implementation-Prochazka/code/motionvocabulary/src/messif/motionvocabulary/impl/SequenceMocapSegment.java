/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary.impl;

import cz.muni.fi.disa.similarityoperators.cover.CoverRank;
import cz.muni.fi.disa.similarityoperators.cover.HullRepresentation;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import mcdr.objects.impl.ObjectMocapPoseCoordsL2;
import mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW;
import messif.objects.LocalAbstractObject;
import messif.objects.impl.MetaObjectArray;
import messif.utility.HullRepresentationAsLocalAbstractObject;

/**
 * Sequence of MoCap segments (segment is a sequence of poses, e.g., {@link SequenceMocapPoseCoordsL2DTW}).
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class SequenceMocapSegment extends MetaObjectArray {
    public static final Class SEGMENT_CLASS = SequenceMocapPoseCoordsL2DTW.class;
    //public static final Class SEGMENT_CLASS = ObjectMocapPoseCoordsL2.class;
    
    public SequenceMocapSegment(String locatorURI, LocalAbstractObject... objects) {
        super(locatorURI, objects);
        if (objects.length > 0 && !SEGMENT_CLASS.isAssignableFrom(objects[0].getClass()))
            throw new IllegalArgumentException("SequenceMocapSegment must be passed with objects of class " + SEGMENT_CLASS.getCanonicalName());
    }

    public SequenceMocapSegment(String locatorURI, Collection<? extends LocalAbstractObject> objects) {
        super(locatorURI, objects);
        if (!objects.isEmpty() && !SEGMENT_CLASS.isAssignableFrom(objects.iterator().next().getClass()))
            throw new IllegalArgumentException("SequenceMocapSegment must be passed with objects of class " + SEGMENT_CLASS.getCanonicalName());
    }

    public SequenceMocapSegment(BufferedReader stream) throws IOException {
        super(stream, SEGMENT_CLASS);
    }

//    private SequenceMocapPoseCoordsL2DTW getSegment(int index) throws IndexOutOfBoundsException {
//        return super.getObject(index); //To change body of generated methods, choose Tools | Templates.
//    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        if (!(obj instanceof HullRepresentationAsLocalAbstractObject))
            return LocalAbstractObject.MAX_DISTANCE;
        HullRepresentation hull = ((HullRepresentationAsLocalAbstractObject)obj).getHull();
        return getDistanceImplByCounting(hull);
//        return getDistanceImplByWeighting(hull);
    }
    
    private float getDistanceImplByWeighting(HullRepresentation hull) {
        //        getExternalCoverRank
        float dist = 0;
        for (LocalAbstractObject o : getObjects()) {
            CoverRank rank = hull.getExternalCoverRank(o);
            dist += rank.getRank();
//            if (rank.isCovered()) {
//                dist += rank.getRank();
//            } else {
//                dist += 1;
//            }
        }
        if (getObjectCount() != 0)
            return dist / (float)getObjectCount();
        else
            return 999f;  // just to signal it is against an empty query object.        
    }

    private float getDistanceImplByCounting(HullRepresentation hull) {
        int covered = 0;
        for (LocalAbstractObject o : getObjects()) {
            if (hull.isExternalCovered(o))
                covered++;
        }
        if (getObjectCount() != 0)
            return 1f - ((float)covered / (float)getObjectCount());
        else
            return 999f;  // just to signal it is against an empty query object.
    }
}
