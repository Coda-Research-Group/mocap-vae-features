/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.motionvocabulary.impl;

import cz.muni.fi.disa.similarityoperators.cover.CoverRank;

/**
 * {@link CoverRank} extended with the motion word ID.
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
class CoverRankMW extends CoverRank {
    long mwID; // motion word

    public CoverRankMW(long mwID, CoverRank rank) {
        super(rank.isCovered(), rank.getRank());
        this.mwID = mwID;
    }
    
}
