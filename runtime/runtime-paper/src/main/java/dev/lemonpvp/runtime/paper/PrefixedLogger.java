package dev.lemonpvp.runtime.paper;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Anonymous logger that prefixes messages and forwards them to the server logger. */
final class PrefixedLogger extends Logger {

    private final String prefix;

    PrefixedLogger(String prefix, Logger parent) {
        super(null, null);
        this.prefix = prefix;
        setParent(parent);
        setUseParentHandlers(true);
    }

    @Override
    public void log(LogRecord record) {
        record.setMessage(this.prefix + record.getMessage());
        super.log(record);
    }
}
