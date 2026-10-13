package dev.lemonpvp.runtime.lifecycle;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger handed to modules. Prefixes records with the module id and forwards them to the
 * runtime logger. Anonymous (not registered in the global {@code LogManager}) so it does
 * not outlive the module; records carry the name of the nearest named parent instead, so
 * server log layouts that print the logger name do not show {@code null}.
 */
final class ModuleLogger extends Logger {

    private final String prefix;

    ModuleLogger(String moduleId, Logger parent) {
        super(null, null);
        this.prefix = "[" + moduleId + "] ";
        setParent(parent);
        setUseParentHandlers(true);
    }

    @Override
    public void log(LogRecord record) {
        record.setMessage(this.prefix + record.getMessage());
        if (record.getLoggerName() == null) {
            record.setLoggerName(namedParent(this));
        }
        super.log(record);
    }

    /**
     * Name of the nearest ancestor that has one.
     *
     * @param logger logger to start from
     * @return the name, or {@code ""} (the root logger's name) if no ancestor has one
     */
    static String namedParent(Logger logger) {
        for (Logger l = logger.getParent(); l != null; l = l.getParent()) {
            if (l.getName() != null) {
                return l.getName();
            }
        }
        return "";
    }
}
