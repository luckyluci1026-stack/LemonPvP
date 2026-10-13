package dev.lemonpvp.runtime.lifecycle;

import java.util.List;

/** A lifecycle step failed. Carries detail lines for the operator. */
final class LifecycleException extends Exception {

    private final List<String> details;

    LifecycleException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    LifecycleException(String message) {
        this(message, List.of());
    }

    List<String> details() {
        return this.details;
    }
}
