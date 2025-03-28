package mcdr.objects.impl;

import java.util.Set;

/**
 * Composite MW's body part. 
 * 
 * @author David Procházka
 */
public record ObjectBodyPart(
        int index,
        String name,
        Set<Integer> jointIds
) {

    public boolean match(ObjectMotionWordComposite lhs, ObjectMotionWordComposite rhs) {
        return lhs.data[index] == rhs.data[index];
    }

    @Override
    public String toString() {
        return name;
    }
}
