package dev.lemonpvp.runtime.commands;

/**
 * Permission nodes of the {@code /fork} command. The platform registers them with these
 * defaults: {@link #BASE}, {@link #MODULE_VIEW} and {@link #LIVE_VIEW} for operators;
 * {@link #RELOAD}, {@link #MODULE_MANAGE} and {@link #LIVE_UPDATE} for nobody (server
 * console only unless granted explicitly).
 */
public final class RuntimePermissions {

    /** {@code /fork}, {@code info}, {@code version}, {@code status}. */
    public static final String BASE = "lemon.runtime.command";
    /** {@code /fork reload} (runtime configuration). */
    public static final String RELOAD = "lemon.runtime.command.reload";
    /** {@code module list|info|validate|diagnostics}. */
    public static final String MODULE_VIEW = "lemon.runtime.module.view";
    /** {@code module load|enable|disable|reload|unload|config} - executes code. */
    public static final String MODULE_MANAGE = "lemon.runtime.module.manage";
    /** {@code live status|scan|plugins|modules|check|history} and {@code live patch status}. */
    public static final String LIVE_VIEW = "lemon.runtime.live.view";
    /** {@code live update|rollback} and {@code live patch probe} - change running code. */
    public static final String LIVE_UPDATE = "lemon.runtime.live.update";

    private RuntimePermissions() {
    }
}
