package dev.lemonpvp.runtime.lifecycle;

import java.time.Duration;
import java.util.List;

/**
 * Outcome of an administrative operation. Never reports success unless the operation
 * completed; partial outcomes are failures with details.
 *
 * @param success  whether the operation completed as requested
 * @param action   operation name, e.g. {@code enable}
 * @param moduleId target module
 * @param message  one-line summary
 * @param details  additional lines (warnings, cleanup problems, rollback outcome)
 * @param duration wall-clock duration
 */
public record OperationResult(boolean success, String action, String moduleId, String message, List<String> details,
                              Duration duration) {
    public OperationResult {
        details = List.copyOf(details);
    }

    static OperationResult ok(String action, String id, String message, List<String> details, long startNanos) {
        return new OperationResult(true, action, id, message, details, Duration.ofNanos(System.nanoTime() - startNanos));
    }

    static OperationResult fail(String action, String id, String message, List<String> details, long startNanos) {
        return new OperationResult(false, action, id, message, details, Duration.ofNanos(System.nanoTime() - startNanos));
    }
}
