package mcdr.objects.impl;

import java.util.Set;

import static mcda.commons.constants.LandmarkConstant.*;

/**
 * Dataset-dependent configuration of body parts.
 * Specifies a body part position inside Composite MW.
 * Each body part consists of a set of joints. 
 *
 * @author David Procházka
 */
public enum BodyPartConfiguration {
    HDM05(
            31,
            Set.of(
                    new ObjectBodyPart(0, "TORSO", Set.of(LANDMARK_LOWERBACK_ID, LANDMARK_UPPERBACK_ID, LANDMARK_THORAX_ID, LANDMARK_LOWERNECK_ID, LANDMARK_UPPERNECK_ID, LANDMARK_HEAD_ID)),
                    new ObjectBodyPart(1, "LEFT_ARM", Set.of(LANDMARK_LCLAVICLE_ID, LANDMARK_LHUMERUS_ID, LANDMARK_LRADIUS_ID, LANDMARK_LWRIST_ID, LANDMARK_LHAND_ID, LANDMARK_LFINGERS_ID, LANDMARK_LTHUMB_ID)),
                    new ObjectBodyPart(2, "LEFT_LEG", Set.of(LANDMARK_LHIPJOINT_ID, LANDMARK_LFEMUR_ID, LANDMARK_LTIBIA_ID, LANDMARK_LFOOT_ID, LANDMARK_LTOES_ID)),
                    new ObjectBodyPart(3, "RIGHT_ARM", Set.of(LANDMARK_RCLAVICLE_ID, LANDMARK_RHUMERUS_ID, LANDMARK_RRADIUS_ID, LANDMARK_RWRIST_ID, LANDMARK_RHAND_ID, LANDMARK_RFINGERS_ID, LANDMARK_RTHUMB_ID)),
                    new ObjectBodyPart(4, "RIGHT_LEG", Set.of(LANDMARK_RHIPJOINT_ID, LANDMARK_RFEMUR_ID, LANDMARK_RTIBIA_ID, LANDMARK_RFOOT_ID, LANDMARK_RTOES_ID))
            ),
            ""
    ),
    HDM05_EXTENDED(
            31,
            Set.of(
                    new ObjectBodyPart(0, "TORSO", Set.of(12, 13, 14, 15, 16, 17)),
                    new ObjectBodyPart(3, "LEFT_ARM", Set.of(18, 19, 20, 21, 22, 23, 24)),
                    new ObjectBodyPart(4, "LEFT_LEG", Set.of(2, 3, 4, 5, 6)),
                    new ObjectBodyPart(5, "RIGHT_ARM", Set.of(25, 26, 27, 28, 29, 30, 31)),
                    new ObjectBodyPart(7, "RIGHT_LEG", Set.of(7, 8, 9, 10, 11)),
                    // Relations
                    new ObjectBodyPart(6, "RH+LH", Set.of()), // 28,29,30,31,21,22,23,24
                    new ObjectBodyPart(1, "HEAD+LH", Set.of()), // 16,17,21,22,23,24
                    new ObjectBodyPart(2, "HEAD+RH", Set.of()) // 16,17,28,29,30,31
            ),
            "-extended"
    ),
    PKU_MMD(
            25,
            Set.of(
                    new ObjectBodyPart(0, "LEFT_LEG", Set.of(13, 14, 15, 16)),
                    new ObjectBodyPart(1, "RIGHT_LEG", Set.of(17, 18, 19, 20)),
                    new ObjectBodyPart(2, "TORSO", Set.of(2, 3, 4, 21)),
                    new ObjectBodyPart(3, "LEFT_ARM", Set.of(5, 6, 7, 8, 22, 23)),
                    new ObjectBodyPart(4, "RIGHT_ARM", Set.of(9, 10, 11, 12, 24, 25))
            ),
            ""
    ),
    PKU_MMD_EXTENDED(
            25,
            Set.of(
                    new ObjectBodyPart(1, "LEFT_LEG", Set.of(13, 14, 15, 16)),
                    new ObjectBodyPart(2, "RIGHT_LEG", Set.of(17, 18, 19, 20)),
                    new ObjectBodyPart(3, "TORSO", Set.of(2, 3, 4, 21)),
                    new ObjectBodyPart(6, "LEFT_ARM", Set.of(5, 6, 7, 8, 22, 23)),
                    new ObjectBodyPart(7, "RIGHT_ARM", Set.of(9, 10, 11, 12, 24, 25)),
                    // Relations
                    new ObjectBodyPart(0, "RH+LH", Set.of()),
                    new ObjectBodyPart(5, "HEAD+LH", Set.of()),
                    new ObjectBodyPart(4, "HEAD+RH", Set.of())
            ),
            "-extended"
    );

    /**
     * Dimensionality of a single joint.
     */
    public static final int JOINT_DIM = 3;

    private final int jointCount;
    private final Set<ObjectBodyPart> bodyParts;

    private final String fileAppendix;

    BodyPartConfiguration(int jointCount, Set<ObjectBodyPart> bodyParts, String fileAppendix) {
        this.jointCount = jointCount;
        this.bodyParts = bodyParts;
        this.fileAppendix = fileAppendix;
    }

    int getJointCount() {
        return jointCount;
    }

    public Set<ObjectBodyPart> getBodyParts() {
        return bodyParts;
    }

    public String getFileAppendix() {
        return fileAppendix;
    }
}
