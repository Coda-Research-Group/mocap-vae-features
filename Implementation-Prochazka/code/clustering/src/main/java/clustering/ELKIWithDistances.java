package clustering;

import de.lmu.ifi.dbs.elki.application.ELKILauncher;
import de.lmu.ifi.dbs.elki.gui.minigui.MiniGUI;

/**
 * {@link ELKIWithDistances} provides ELKI MiniGUI access to the distance functions
 * located in {@code ./distance} folder.
 * The GUI starts when no command line arguments are supplied.
 * Running {@link #main(String[])} directly from IDEA should make the corresponding distances accessible.
 * Building and running the app as {@code jar} from CLI makes some GUI options invisible but still functional.
 * These options should be typed in directly.
 * This seems to be correct behavior, see
 * <a href="https://elki-project.github.io/tutorial/distance_functions#basic-distance-function">
 * Writing a custom distance function</a>.
 */
public final class ELKIWithDistances {

    private ELKIWithDistances() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            MiniGUI.main(args);
        } else {
            ELKILauncher.main(args);
        }
    }
}
