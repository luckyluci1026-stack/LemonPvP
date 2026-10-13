package dev.lemonpvp.runtime.api;

/**
 * Lifecycle state of one loaded module instance.
 *
 * <pre>
 * DISCOVERED -&gt; VALIDATED -&gt; LOADED -&gt; ENABLING -&gt; ENABLED -&gt; DISABLING -&gt; DISABLED -&gt; UNLOADED
 *                                         ^                                      |
 *                                         +--------------------------------------+  (re-enable)
 * any non-terminal state -&gt; FAILED -&gt; UNLOADED
 * </pre>
 */
public enum ModuleState {
    /** Artifact found and descriptor parsed. */
    DISCOVERED,
    /** Integrity, compatibility and dependencies checked. */
    VALIDATED,
    /** Class loader created and module class instantiated ({@code onLoad} done). */
    LOADED,
    /** {@code onEnable} is running. */
    ENABLING,
    /** Module is active. */
    ENABLED,
    /** {@code onDisable} and resource cleanup are running. */
    DISABLING,
    /** Module inactive, classes still loaded; can be enabled again. */
    DISABLED,
    /** Class loader closed; the instance can no longer be used. Terminal. */
    UNLOADED,
    /** A lifecycle step failed; see diagnostics. Only {@link #UNLOADED} may follow. */
    FAILED
}
