package dev.lemonpvp.runtime.lifecycle;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger handed to modules. Prefixes records with the module id and forwards them to the
 * runtime logger. Anonymous (not registered in the global {@code LogManager}) so it does
 * not outlive the module.
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
        super.log(record);
    }
}
