package de.lemonpvp.flfac.check;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.config.CheckSettings;
import de.lemonpvp.flfac.data.PlayerData;

/**
 * Base class for every FLFAC check. Detection logic lives in the subclasses;
 * shared plumbing (settings access, flagging) lives here.
 */
public abstract class Check {

    protected final FLFAC plugin;
    private final CheckType type;

    protected Check(FLFAC plugin, CheckType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public CheckType getType() {
        return type;
    }

    protected CheckSettings settings() {
        return plugin.getConfigManager().getCheck(type);
    }

    public boolean isEnabled() {
        return settings().isEnabled();
    }

    /** Raises a violation with extra human-readable detail (shown in verbose/hover). */
    protected void flag(PlayerData data, String details) {
        plugin.getViolationManager().flag(this, data, details);
    }
}
