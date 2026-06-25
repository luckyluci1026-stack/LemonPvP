package com.lemonpvp.lemoncore.lemonlang.runtime;

/**
 * Sentinel exception thrown by the {@code stop} and {@code cancel} action
 * verbs to abort the current LemonLang body execution.
 * It is caught by the interpreter's top-level runner and is intentional.
 */
public final class StopExecution extends RuntimeException {

    public StopExecution() {
        super(null, null, true, false); // no message, no cause, suppress, no stacktrace
    }
}
