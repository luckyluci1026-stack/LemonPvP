package dev.paperswap.runtime.metrics;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks conditions the runtime cannot repair on its own. When a module could not be
 * cleaned up completely the runtime records the reason here and recommends a controlled
 * server restart instead of pretending everything is fine.
 */
public final class RuntimeHealth {

    /**
     * A reason why a restart is recommended.
     *
     * @param moduleId affected module or {@code null}
     * @param reason   description
     * @param at       time it was recorded
     */
    public record Issue(String moduleId, String reason, Instant at) {
    }

    private final List<Issue> issues = new CopyOnWriteArrayList<>();

    /**
     * @param moduleId affected module
     * @param reason   description
     */
    public void recommendRestart(String moduleId, String reason) {
        this.issues.add(new Issue(moduleId, reason, Instant.now()));
    }

    /** @return whether a restart is recommended */
    public boolean restartRecommended() {
        return !this.issues.isEmpty();
    }

    /** @return recorded issues, oldest first */
    public List<Issue> issues() {
        return List.copyOf(this.issues);
    }
}
