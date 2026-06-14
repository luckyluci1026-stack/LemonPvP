package de.lemonpvp.flfac.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Cached, typed view over a single check's configuration section. Reading from
 * here avoids touching the YAML tree on every packet/tick.
 */
public final class CheckSettings {

    private final boolean enabled;
    private final int alertVl;
    private final double maxVl;
    private final double vlAdd;
    private final ConfigurationSection section;

    public CheckSettings(ConfigurationSection section) {
        this.section = section;
        this.enabled = section == null || section.getBoolean("enabled", true);
        this.alertVl = section == null ? 5 : section.getInt("alert-vl", 5);
        this.maxVl = section == null ? 60.0D : section.getDouble("max-vl", 60.0D);
        this.vlAdd = section == null ? 1.0D : section.getDouble("vl-add", 1.0D);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getAlertVl() {
        return alertVl;
    }

    public double getMaxVl() {
        return maxVl;
    }

    public double getVlAdd() {
        return vlAdd;
    }

    public double getDouble(String key, double def) {
        return section == null ? def : section.getDouble(key, def);
    }

    public int getInt(String key, int def) {
        return section == null ? def : section.getInt(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return section == null ? def : section.getBoolean(key, def);
    }
}
