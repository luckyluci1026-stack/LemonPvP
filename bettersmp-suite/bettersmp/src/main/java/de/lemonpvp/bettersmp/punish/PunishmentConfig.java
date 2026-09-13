package de.lemonpvp.bettersmp.punish;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.util.Durations;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lädt bans.yml und mutes.yml (Gründe, Dauern, Screens).
 */
public final class PunishmentConfig {

    /** Ein konfigurierter Grund. durationMillis = 0 -> permanent. */
    public record Reason(String key, long durationMillis, String display, List<String> screen) {
    }

    private final BetterSMP plugin;
    private YamlConfiguration bans;
    private YamlConfiguration mutes;

    public PunishmentConfig(BetterSMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.bans = load("bans.yml");
        this.mutes = load("mutes.yml");
    }

    private YamlConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // ---------------- Bans ----------------

    public Set<String> banReasonKeys() {
        ConfigurationSection sec = bans.getConfigurationSection("reasons");
        return sec == null ? Set.of() : sec.getKeys(false);
    }

    public Reason banReason(String key) {
        return reason(bans, key);
    }

    public List<String> banDefaultScreen() {
        return bans.getStringList("settings.default-screen");
    }

    public String banPermanentWord() {
        return bans.getString("settings.permanent-word", "Permanent");
    }

    // ---------------- Mutes ----------------

    public Set<String> muteReasonKeys() {
        ConfigurationSection sec = mutes.getConfigurationSection("reasons");
        return sec == null ? Set.of() : sec.getKeys(false);
    }

    public Reason muteReason(String key) {
        return reason(mutes, key);
    }

    public List<String> muteMessage() {
        return mutes.getStringList("settings.message");
    }

    public String mutePermanentWord() {
        return mutes.getString("settings.permanent-word", "Permanent");
    }

    // ---------------- gemeinsam ----------------

    private Reason reason(YamlConfiguration cfg, String key) {
        ConfigurationSection sec = cfg.getConfigurationSection("reasons." + key.toLowerCase(Locale.ROOT));
        if (sec == null) {
            return null;
        }
        long millis = Durations.parse(sec.getString("duration", "perm"));
        String display = sec.getString("display", key);
        List<String> screen = sec.contains("screen") ? sec.getStringList("screen") : null;
        return new Reason(key.toLowerCase(Locale.ROOT), millis, display, screen);
    }
}
