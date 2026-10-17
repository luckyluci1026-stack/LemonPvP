package dev.paperswap.runtime.paper;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Anonymous logger that prefixes messages and forwards them to the server logger. Records
 * carry the server logger's name, which Paper's log layout prints without a {@code [name]}
 * prefix, instead of {@code null}.
 */
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
        if (record.getLoggerName() == null && getParent() != null) {
            record.setLoggerName(getParent().getName());
        }
        super.log(record);
    }
}
