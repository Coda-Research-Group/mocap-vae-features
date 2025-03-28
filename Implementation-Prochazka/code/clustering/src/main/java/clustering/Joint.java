package clustering;

/**
 * Set of constants used for identifying each joint.
 * The joint index in an array (list) is equal to joint id minus 1.
 * Originally {@code mcda.commons.constants.LandmarkConstant} in
 * <a href="https://gitlab.fi.muni.cz/disa/public/motion/mcda-commons">https://gitlab.fi.muni.cz/disa/public/motion/mcda-commons</a>.
 *
 * @author kuba
 */
public enum Joint {

    ROOT(1), // střed pánve
    LHIPJOINT(2), // levá kyčel
    LFEMUR(3), // levé koleno
    LTIBIA(4), // levý kotník
    LFOOT(5), // levý nárt
    LTOES(6), // prsty levé nohy
    RHIPJOINT(7), // pravá kyčel
    RFEMUR(8), // pravé koleno
    RTIBIA(9), // pravý kotník
    RFOOT(10), // pravý nárt
    RTOES(11), // prsty pravé nohy
    LOWERBACK(12), // trup (níže)
    UPPERBACK(13), // trup (výše)
    THORAX(14), // trup (hrudník)
    LOWERNECK(15), // krk (níže)
    UPPERNECK(16), // krk (výše)
    HEAD(17), // hlava
    LCLAVICLE(18), // levé rameno
    LHUMERUS(19), // levý loket
    LRADIUS(20), // levé zápěstí
    LWRIST(21), // levá dlaň
    LHAND(22), // levá dlaň (u prstů)
    LFINGERS(23), // prst na levé ruce
    LTHUMB(24), // palec na levé ruce
    RCLAVICLE(25), // pravé rameno
    RHUMERUS(26), // pravý loket
    RRADIUS(27), // pravé zápěstí
    RWRIST(28), // pravá dlaň
    RHAND(29), // pravá dlaň (u prstů)
    RFINGERS(30), // prst na pravé ruce
    RTHUMB(31); // palec na pravé ruce

    private final int id;

    Joint(int jointId) {
        id = jointId;
    }

    /**
     * Returns the list (array) position of the joint given by {@code jointId}.
     * The conversion is naive, it does not check if the joint id is valid.
     *
     * @param jointId the joint id
     * @return list (array) position of the given joint
     */
    public static int getJointIndex(int jointId) {
        return jointId - 1;
    }
}
