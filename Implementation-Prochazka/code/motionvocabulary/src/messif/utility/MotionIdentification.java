/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class MotionIdentification {
    //                                         0002-L_38     _919         _61          _0
    /** Pattern to parse motion locator -- sequenceID_classID_actionOffset_actionLength_segmentNo */
    private static final Pattern LOCATOR_PATTERN =  Pattern.compile("([^_]+)_([0-9]+)_([0-9]+)_([0-9]+)(?:_([0-9]+))?");
    /** Pattern to parse motion locator -- sequenceID_segmentNo */
    private static final Pattern LOCATOR_PATTERN2 = Pattern.compile("([^_]+)_([0-9]+)");
    private static final String MOTION_FORMAT = "%s_%d_%d_%d";
    private static final String SEGMENT_FORMAT = MOTION_FORMAT + "_%d";

    public static String createMotionLocator(String sequenceId, int classId, int actionOffset, int actionLength) {
        // <sequenceID>_<classID>_<actionOffset>_<actionLength>_<segmentNo>
        return String.format(MOTION_FORMAT, sequenceId, classId, actionOffset, actionLength);
    }
    
    public static String createMotionLocator(String sequenceId, int classId) {
        return createMotionLocator(sequenceId, classId, 0, 0);
    }
    
    public static String createSegmentLocator(String sequenceId, int classId, int actionOffset, int actionLength, int segmentNo) {
        // <sequenceID>_<classID>_<actionOffset>_<actionLength>_<segmentNo>
        return String.format(SEGMENT_FORMAT, sequenceId, classId, actionOffset, actionLength, segmentNo);
    }
    
    public static String stripSegmentFromLocator(String locator) {
        Matcher m = parseMotionLocator(locator);
        try {
            if (m.groupCount() >= 4)
                return createMotionLocator(m.group(1), Integer.parseInt(m.group(2)), 
                                           Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
            else
                return createMotionLocator(m.group(1), 0);    // no class information
        } catch (NumberFormatException | NullPointerException ex) {
            System.err.println("Cannot parse the object locator: " + locator);
            return null;
        }
    }
    
    public static Integer getMotionClassID(String locator) {
        Matcher m = parseMotionLocator(locator);
        try {
            if (m.groupCount() >= 4)
                return Integer.parseInt(m.group(2));
            else
                return null; // not available
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }
            
    public static Matcher parseMotionLocator(String locator) {
        Matcher m;
        // <sequenceID>_<classID>_<actionOffset>_<actionLength>_<segmentNo>
        // seqmentNo can be ommitted!
        // Example: #objectKey messif.objects.keys.AbstractObjectKey 3136_103_280_178_2
        // znamena, ze se jedna v poradi o 3. segment (cislovano od nuly) 
        // extrahovany z akce, ktera patri do kategorie ID=103. 
        // Tato akce pochazi z puvodni dlouhe sekvence s ID=3136, konkretne zacina na frame 280 a trva 178 framu.
        m = LOCATOR_PATTERN.matcher(locator);
        if (m.matches())
            return m;
        
        // <sequenceID>_<segmentNo> E.g. 999_66
        m = LOCATOR_PATTERN2.matcher(locator);
        return (m.matches()) ? m : null;   // groups are numbered from 1 !!!!
    }
    
    
}
