package mcdr.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author David Procházka
 */
public class MyFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }
}
