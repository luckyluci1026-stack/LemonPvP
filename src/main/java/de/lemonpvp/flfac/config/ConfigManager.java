package de.lemonpvp.flfac.config;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.check.CheckType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads and caches everything from config.yml so the rest of the plugin can
 * read settings without touching the YAML tree on hot paths.
 */
public final class ConfigManager {

    private final FLFAC plugin;

    // Branding
    private String prefixText;
    private List<String> prefixGradient;
    private List<String> playerGradient;
    private List<String> checkGradient;
    private List<String> accentGradient;
    private boolean startupBanner;

    // Performance
    private double minTps;
    private boolean lagCompensation;
    private int highPingThreshold;

    // Alerts
    private boolean alertsEnabledByDefault;
    private boolean alertSoundEnabled;
    private String alertSoundName;
    private float alertSoundVolume;
    private float alertSoundPitch;
    private long alertCooldownMs;
    private String alertFormat;

    // Violations
    private double decayPerSecond;
    private boolean resetOnQuit;

    // Punishments
    private boolean punishmentsEnabled;
    private final TreeMap<Integer, String> globalLadder = new TreeMap<>();
    private final Map<CheckType, TreeMap<Integer, String>> checkLadders = new EnumMap<>(CheckType.class);

    // Checks
    private final Map<CheckType, CheckSettings> checkSettings = new EnumMap<>(CheckType.class);

    public ConfigManager(FLFAC plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        // Branding
        prefixText = c.getString("branding.prefix-text", "FLFAC");
        prefixGradient = stringList(c, "branding.prefix-gradient", Arrays.asList("#fffb00", "#00ff00"));
        playerGradient = stringList(c, "branding.player-gradient", Arrays.asList("#ffffff", "#bdbdbd"));
        checkGradient = stringList(c, "branding.check-gradient", Arrays.asList("#ff5e00", "#ffd000"));
        accentGradient = stringList(c, "branding.accent-gradient", Arrays.asList("#00ff95", "#00d4ff"));
        startupBanner = c.getBoolean("branding.startup-banner", true);

        // Performance
        minTps = c.getDouble("performance.min-tps", 16.0D);
        lagCompensation = c.getBoolean("performance.lag-compensation", true);
        highPingThreshold = c.getInt("performance.high-ping-threshold", 150);

        // Alerts
        alertsEnabledByDefault = c.getBoolean("alerts.enabled-by-default", true);
        alertSoundEnabled = c.getBoolean("alerts.sound.enabled", true);
        alertSoundName = c.getString("alerts.sound.name", "BLOCK_NOTE_BLOCK_PLING");
        alertSoundVolume = (float) c.getDouble("alerts.sound.volume", 0.6D);
        alertSoundPitch = (float) c.getDouble("alerts.sound.pitch", 1.4D);
        alertCooldownMs = c.getLong("alerts.cooldown-ms", 800L);
        alertFormat = c.getString("alerts.format",
                "%player% <gray>failed</gray> %check% <gray>x</gray>%vl%");

        // Violations
        decayPerSecond = c.getDouble("violations.decay-per-second", 0.25D);
        resetOnQuit = c.getBoolean("violations.reset-on-quit", true);

        // Punishments
        punishmentsEnabled = c.getBoolean("punishments.enabled", true);
        globalLadder.clear();
        loadLadder(c.getConfigurationSection("punishments.ladder"), globalLadder);

        // Checks
        checkSettings.clear();
        checkLadders.clear();
        ConfigurationSection checks = c.getConfigurationSection("checks");
        for (CheckType type : CheckType.values()) {
            ConfigurationSection section = checks == null ? null : checks.getConfigurationSection(type.getConfigKey());
            checkSettings.put(type, new CheckSettings(section));
            if (section != null) {
                TreeMap<Integer, String> ladder = new TreeMap<>();
                loadLadder(section.getConfigurationSection("punishments"), ladder);
                if (!ladder.isEmpty()) {
                    checkLadders.put(type, ladder);
                }
            }
        }
    }

    private void loadLadder(ConfigurationSection section, TreeMap<Integer, String> target) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                target.put(Integer.parseInt(key), section.getString(key));
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Invalid punishment threshold '" + key + "' (must be a number).");
            }
        }
    }

    private List<String> stringList(FileConfiguration c, String path, List<String> def) {
        List<String> list = c.getStringList(path);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>(def);
        }
        return list;
    }

    public CheckSettings getCheck(CheckType type) {
        return checkSettings.get(type);
    }

    /** Returns the effective punishment ladder for a check (per-check overrides global). */
    public TreeMap<Integer, String> getLadder(CheckType type) {
        TreeMap<Integer, String> specific = checkLadders.get(type);
        return specific != null ? specific : globalLadder;
    }

    // ----- getters -----
    public String getPrefixText() { return prefixText; }
    public List<String> getPrefixGradient() { return prefixGradient; }
    public List<String> getPlayerGradient() { return playerGradient; }
    public List<String> getCheckGradient() { return checkGradient; }
    public List<String> getAccentGradient() { return accentGradient; }
    public boolean isStartupBanner() { return startupBanner; }

    public double getMinTps() { return minTps; }
    public boolean isLagCompensation() { return lagCompensation; }
    public int getHighPingThreshold() { return highPingThreshold; }

    public boolean isAlertsEnabledByDefault() { return alertsEnabledByDefault; }
    public boolean isAlertSoundEnabled() { return alertSoundEnabled; }
    public String getAlertSoundName() { return alertSoundName; }
    public float getAlertSoundVolume() { return alertSoundVolume; }
    public float getAlertSoundPitch() { return alertSoundPitch; }
    public long getAlertCooldownMs() { return alertCooldownMs; }
    public String getAlertFormat() { return alertFormat; }

    public double getDecayPerSecond() { return decayPerSecond; }
    public boolean isResetOnQuit() { return resetOnQuit; }

    public boolean isPunishmentsEnabled() { return punishmentsEnabled; }
}
