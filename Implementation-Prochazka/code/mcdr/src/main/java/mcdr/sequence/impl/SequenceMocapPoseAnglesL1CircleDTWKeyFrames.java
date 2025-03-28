package mcdr.sequence.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class SequenceMocapPoseAnglesL1CircleDTWKeyFrames extends SequenceMocapPoseAnglesL1CircleDTW {

    private Map<String, int[]> keyFramesMap = new HashMap<String, int[]>();

    public SequenceMocapPoseAnglesL1CircleDTWKeyFrames(BufferedReader stream) throws IOException {
        super(stream);
        int keyFrameApproachCount = Integer.valueOf(stream.readLine());
        for (int i = 0; i < keyFrameApproachCount; i++) {
            String[] approachString = stream.readLine().split(";");
            String[] keyFramesString = approachString[1].split(",");
            int[] keyFrames = new int[keyFramesString.length];
            for (int j = 0; j < keyFrames.length; j++) {
                keyFrames[j] = Integer.valueOf(keyFramesString[j]);
            }
            keyFramesMap.put(approachString[0], keyFrames);
        }
    }

    public Map<String, int[]> getKeyFramesMap() {
        return keyFramesMap;
    }
}
