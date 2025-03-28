/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mcdr.evaluation;

import cz.muni.fi.disa.similarityoperators.cover.HullRepresentation;
import mcdr.objects.utils.InstantiableCollection;
import messif.motionvocabulary.impl.SequenceMocapSegment;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.util.RankedAbstractObject;
import messif.objects.util.RankedSortedCollection;
import messif.utility.HullRepresentationAsLocalAbstractObject;

/**
 * Rerank hulls retreived by an operation using the distance function on original data, e.g. DTW.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class HullRerankingCollection extends RankedSortedCollection implements InstantiableCollection {
    private SequenceMocapSegment query;

    public HullRerankingCollection(AbstractObject queryObject) {
        this.query = (SequenceMocapSegment)queryObject;
    }
    
    @Override
    public synchronized boolean add(RankedAbstractObject e) {
        if (!(e.getObject() instanceof HullRepresentationAsLocalAbstractObject))
            return false;
        HullRepresentation hull = ((HullRepresentationAsLocalAbstractObject)e.getObject()).getHull();
        float dist = 0f;
        
        for (LocalAbstractObject o : query.getObjects()) {
            float min = Float.MAX_VALUE;
            for (LocalAbstractObject ho : hull.getHull()) {
                float d = o.getDistance(ho);
                if (d < min)
                    min = d;
            }
            dist += min;
        }
        return super.add(new RankedAbstractObject(e.getObject(), dist));
    }

    @Override
    public <E extends RankedSortedCollection> E instantiate(AbstractObject queryObject) {
        return (E) new HullRerankingCollection(queryObject);
    }
    
}
