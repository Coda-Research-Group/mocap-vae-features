package mcdr.sequence;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import mcda.commons.constants.LandmarkConstant;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class KinematicTree {

    //************ Constants ************//
    // mocap skeleton kinematic trees
    public static final Map<Integer, int[]> KINEMATIC_TREE_VICON = Collections.unmodifiableMap(new HashMap<Integer, int[]>() {
        {
            // root
            put(LandmarkConstant.LANDMARK_ROOT_ID, new int[]{LandmarkConstant.LANDMARK_LHIPJOINT_ID, LandmarkConstant.LANDMARK_RHIPJOINT_ID, LandmarkConstant.LANDMARK_LOWERBACK_ID});
            // left leg
            put(LandmarkConstant.LANDMARK_LHIPJOINT_ID, new int[]{LandmarkConstant.LANDMARK_LFEMUR_ID});
            put(LandmarkConstant.LANDMARK_LFEMUR_ID, new int[]{LandmarkConstant.LANDMARK_LTIBIA_ID});
            put(LandmarkConstant.LANDMARK_LTIBIA_ID, new int[]{LandmarkConstant.LANDMARK_LFOOT_ID});
            put(LandmarkConstant.LANDMARK_LFOOT_ID, new int[]{LandmarkConstant.LANDMARK_LTOES_ID});
            // right leg
            put(LandmarkConstant.LANDMARK_RHIPJOINT_ID, new int[]{LandmarkConstant.LANDMARK_RFEMUR_ID});
            put(LandmarkConstant.LANDMARK_RFEMUR_ID, new int[]{LandmarkConstant.LANDMARK_RTIBIA_ID});
            put(LandmarkConstant.LANDMARK_RTIBIA_ID, new int[]{LandmarkConstant.LANDMARK_RFOOT_ID});
            put(LandmarkConstant.LANDMARK_RFOOT_ID, new int[]{LandmarkConstant.LANDMARK_RTOES_ID});
            // body
            put(LandmarkConstant.LANDMARK_LOWERBACK_ID, new int[]{LandmarkConstant.LANDMARK_UPPERBACK_ID});
            put(LandmarkConstant.LANDMARK_UPPERBACK_ID, new int[]{LandmarkConstant.LANDMARK_THORAX_ID});
            put(LandmarkConstant.LANDMARK_THORAX_ID, new int[]{LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_LCLAVICLE_ID, LandmarkConstant.LANDMARK_RCLAVICLE_ID});
            // head
            put(LandmarkConstant.LANDMARK_LOWERNECK_ID, new int[]{LandmarkConstant.LANDMARK_UPPERNECK_ID});
            put(LandmarkConstant.LANDMARK_UPPERNECK_ID, new int[]{LandmarkConstant.LANDMARK_HEAD_ID});
            // left hand
            put(LandmarkConstant.LANDMARK_LCLAVICLE_ID, new int[]{LandmarkConstant.LANDMARK_LHUMERUS_ID});
            put(LandmarkConstant.LANDMARK_LHUMERUS_ID, new int[]{LandmarkConstant.LANDMARK_LRADIUS_ID});
            put(LandmarkConstant.LANDMARK_LRADIUS_ID, new int[]{LandmarkConstant.LANDMARK_LWRIST_ID});
            put(LandmarkConstant.LANDMARK_LWRIST_ID, new int[]{LandmarkConstant.LANDMARK_LHAND_ID});
            put(LandmarkConstant.LANDMARK_LHAND_ID, new int[]{LandmarkConstant.LANDMARK_LFINGERS_ID, LandmarkConstant.LANDMARK_LTHUMB_ID});
            // right hand
            put(LandmarkConstant.LANDMARK_RCLAVICLE_ID, new int[]{LandmarkConstant.LANDMARK_RHUMERUS_ID});
            put(LandmarkConstant.LANDMARK_RHUMERUS_ID, new int[]{LandmarkConstant.LANDMARK_RRADIUS_ID});
            put(LandmarkConstant.LANDMARK_RRADIUS_ID, new int[]{LandmarkConstant.LANDMARK_RWRIST_ID});
            put(LandmarkConstant.LANDMARK_RWRIST_ID, new int[]{LandmarkConstant.LANDMARK_RHAND_ID});
            put(LandmarkConstant.LANDMARK_RHAND_ID, new int[]{LandmarkConstant.LANDMARK_RFINGERS_ID, LandmarkConstant.LANDMARK_RTHUMB_ID});
        }
    });
    public static final Map<Integer, int[]> KINEMATIC_TREE_KINECT_SBU = Collections.unmodifiableMap(new HashMap<Integer, int[]>() {
        {
            // root
            put(LandmarkConstant.LANDMARK_ROOT_ID, new int[]{LandmarkConstant.LANDMARK_LHIPJOINT_ID, LandmarkConstant.LANDMARK_RHIPJOINT_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID});
            // left leg
            put(LandmarkConstant.LANDMARK_LHIPJOINT_ID, new int[]{LandmarkConstant.LANDMARK_LFEMUR_ID});
            put(LandmarkConstant.LANDMARK_LFEMUR_ID, new int[]{LandmarkConstant.LANDMARK_LFOOT_ID});
            // right leg
            put(LandmarkConstant.LANDMARK_RHIPJOINT_ID, new int[]{LandmarkConstant.LANDMARK_RFEMUR_ID});
            put(LandmarkConstant.LANDMARK_RFEMUR_ID, new int[]{LandmarkConstant.LANDMARK_RFOOT_ID});
            // body and head
            put(LandmarkConstant.LANDMARK_LOWERNECK_ID, new int[]{LandmarkConstant.LANDMARK_LCLAVICLE_ID, LandmarkConstant.LANDMARK_RCLAVICLE_ID, LandmarkConstant.LANDMARK_HEAD_ID});
            // left hand
            put(LandmarkConstant.LANDMARK_LCLAVICLE_ID, new int[]{LandmarkConstant.LANDMARK_LHUMERUS_ID});
            put(LandmarkConstant.LANDMARK_LHUMERUS_ID, new int[]{LandmarkConstant.LANDMARK_LHAND_ID});
            // right hand
            put(LandmarkConstant.LANDMARK_RCLAVICLE_ID, new int[]{LandmarkConstant.LANDMARK_RHUMERUS_ID});
            put(LandmarkConstant.LANDMARK_RHUMERUS_ID, new int[]{LandmarkConstant.LANDMARK_RHAND_ID});
        }
    });

    // average bone lengths (hdm05-annotations_granular)
    public static final Map<Map.Entry<Integer, Integer>, Float> BONE_LENGTH_MAP_HDM05 = Collections.unmodifiableMap(new HashMap<Map.Entry<Integer, Integer>, Float>() {
        {
            // root
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_LHIPJOINT_ID), 2.543501380603047f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_RHIPJOINT_ID), 2.5126940658848125f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_LOWERBACK_ID), 2.5017620441061577f);
            // left leg
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHIPJOINT_ID, LandmarkConstant.LANDMARK_LFEMUR_ID), 7.284727156718086f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LFEMUR_ID, LandmarkConstant.LANDMARK_LTIBIA_ID), 7.64281386942891f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LTIBIA_ID, LandmarkConstant.LANDMARK_LFOOT_ID), 2.6043844665952554f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LFOOT_ID, LandmarkConstant.LANDMARK_LTOES_ID), 1.2688729662882572f);
            // right leg
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHIPJOINT_ID, LandmarkConstant.LANDMARK_RFEMUR_ID), 7.218216160656986f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RFEMUR_ID, LandmarkConstant.LANDMARK_RTIBIA_ID), 7.7566819871983546f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RTIBIA_ID, LandmarkConstant.LANDMARK_RFOOT_ID), 2.6204722551533446f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RFOOT_ID, LandmarkConstant.LANDMARK_RTOES_ID), 1.278444161859015f);
            // body
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERBACK_ID, LandmarkConstant.LANDMARK_UPPERBACK_ID), 2.5179998874762304f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_UPPERBACK_ID, LandmarkConstant.LANDMARK_THORAX_ID), 2.548056433355524f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID), 1.593017434223303f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_LCLAVICLE_ID), 4.241735159773716f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_THORAX_ID, LandmarkConstant.LANDMARK_RCLAVICLE_ID), 4.135910149113503f);
            // head
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_UPPERNECK_ID), 1.586592146089965f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_UPPERNECK_ID, LandmarkConstant.LANDMARK_HEAD_ID), 1.6327515421804597f);
            // left hand
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LCLAVICLE_ID, LandmarkConstant.LANDMARK_LHUMERUS_ID), 4.960169749528744f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHUMERUS_ID, LandmarkConstant.LANDMARK_LRADIUS_ID), 3.348122913539783f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LRADIUS_ID, LandmarkConstant.LANDMARK_LWRIST_ID), 1.6740705857494595f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LWRIST_ID, LandmarkConstant.LANDMARK_LHAND_ID), 1.1628882849635536f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHAND_ID, LandmarkConstant.LANDMARK_LFINGERS_ID), 0.9375520580314908f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHAND_ID, LandmarkConstant.LANDMARK_LTHUMB_ID), 1.100589042826531f);
            // right hand
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RCLAVICLE_ID, LandmarkConstant.LANDMARK_RHUMERUS_ID), 4.9356708048766285f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHUMERUS_ID, LandmarkConstant.LANDMARK_RRADIUS_ID), 3.2188073119858966f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RRADIUS_ID, LandmarkConstant.LANDMARK_RWRIST_ID), 1.6094016466518672f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RWRIST_ID, LandmarkConstant.LANDMARK_RHAND_ID), 1.2606159766573524f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHAND_ID, LandmarkConstant.LANDMARK_RFINGERS_ID), 1.0163383988757946f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHAND_ID, LandmarkConstant.LANDMARK_RTHUMB_ID), 1.193712289999817f);
        }
    });

    // average bone lengths (SBUKinectInteraction: class8-coords_normP_sc10.data)
    public static final Map<Map.Entry<Integer, Integer>, Float> BONE_LENGTH_MAP_SBU = Collections.unmodifiableMap(new HashMap<Map.Entry<Integer, Integer>, Float>() {
        {
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_LHIPJOINT_ID), 1.3073217238858519f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_RHIPJOINT_ID), 1.3586025931025454f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_ROOT_ID, LandmarkConstant.LANDMARK_LOWERNECK_ID), 1.1414332073165787f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHIPJOINT_ID, LandmarkConstant.LANDMARK_LFEMUR_ID), 2.271602464952464f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LFEMUR_ID, LandmarkConstant.LANDMARK_LFOOT_ID), 2.183553993254936f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHIPJOINT_ID, LandmarkConstant.LANDMARK_RFEMUR_ID), 2.2766065383835907f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RFEMUR_ID, LandmarkConstant.LANDMARK_RFOOT_ID), 2.185770045849235f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_HEAD_ID), 1.1281709939761289f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_LCLAVICLE_ID), 1.05725933870726f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LOWERNECK_ID, LandmarkConstant.LANDMARK_RCLAVICLE_ID), 1.0572625390698542f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LCLAVICLE_ID, LandmarkConstant.LANDMARK_LHUMERUS_ID), 1.572805455226227f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_LHUMERUS_ID, LandmarkConstant.LANDMARK_LHAND_ID), 1.480956713621483f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RCLAVICLE_ID, LandmarkConstant.LANDMARK_RHUMERUS_ID), 1.6370022750216289f);
            put(new AbstractMap.SimpleEntry<>(LandmarkConstant.LANDMARK_RHUMERUS_ID, LandmarkConstant.LANDMARK_RHAND_ID), 1.4428716514507685f);
        }
    });

    //************ Methods ************//
    /**
     * Returns the bone length specified by indexes of surrounding joints.
     *
     * @param boneLengthMap map with lengths of bones
     * @param boneSurroundingJoints pair of indexes of surrouding joints
     * @return the bone length specified by indexes of surrounding joints
     */
    public static final Float getBoneLength(Map<Map.Entry<Integer, Integer>, Float> boneLengthMap, Map.Entry<Integer, Integer> boneSurroundingJoints) {
        Float rtv = null;
        int joint1 = boneSurroundingJoints.getKey();
        int joint2 = boneSurroundingJoints.getValue();
        for (Map.Entry<Map.Entry<Integer, Integer>, Float> boneLengthEntry : boneLengthMap.entrySet()) {
            int currentJoint1 = boneLengthEntry.getKey().getKey();
            int currentJoint2 = boneLengthEntry.getKey().getValue();
            if ((joint1 == currentJoint1 && joint2 == currentJoint2) || (joint1 == currentJoint2 && joint2 == currentJoint1)) {
                rtv = boneLengthEntry.getValue();
                break;
            }
        }
        return rtv;
    }
}
