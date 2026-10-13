package dev.lemonpvp.runtime.commands;

/**
 * Permission nodes of the {@code /fork} command. The platform registers them with these
 * defaults: {@link #BASE} and {@link #MODULE_VIEW} for operators, {@link #RELOAD} and
 * {@link #MODULE_MANAGE} for nobody (console only unless granted explicitly).
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

    private RuntimePermissions() {
    }
}
