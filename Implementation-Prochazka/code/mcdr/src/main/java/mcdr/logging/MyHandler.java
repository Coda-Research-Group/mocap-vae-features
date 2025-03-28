package mcdr.logging;

import java.util.logging.ConsoleHandler;

/**
 * @author David Procházka
 */
public class MyHandler extends ConsoleHandler {

    public MyHandler() {
        setFormatter(new MyFormatter());
    }
}
