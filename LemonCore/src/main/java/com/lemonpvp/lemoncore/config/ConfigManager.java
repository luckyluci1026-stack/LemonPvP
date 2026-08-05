package com.lemonpvp.lemoncore.config;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final LemonCore plugin;

    public ConfigManager(LemonCore plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public String get(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    public long getLong(String path, long def) {
        return plugin.getConfig().getLong(path, def);
    }

    public boolean getBool(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }

    /** Fallback when a configured ban duration cannot be parsed (7 days). */
    private static final long DEFAULT_BAN_SECONDS  = 7L * 86400L;
    /** Fallback when a configured mute duration cannot be parsed (1 day). */
    private static final long DEFAULT_MUTE_SECONDS = 86400L;

    /**
     * Reads a duration from config, warning and falling back when it is malformed.
     *
     * <p>Without this, an unquoted numeric YAML value (e.g. {@code hacking: 30} meaning
     * "30 days") parses to 0, which the plugin reads as <em>permanent</em> — a silent
     * escalation from a temporary ban to a permanent one.
     */
    private long duration(String raw, String configKey, long fallbackSeconds) {
        long parsed = TextUtil.parseDuration(raw);
        if (parsed == TextUtil.INVALID_DURATION) {
            plugin.getLogger().warning("[Config] " + configKey + ": '" + raw + "' is not a valid"
                    + " duration (expected e.g. 7d, 12h, 30m or permanent). Using "
                    + TextUtil.formatDuration(fallbackSeconds) + " instead.");
            return fallbackSeconds;
        }
        return parsed;
    }

    public long getBanDuration(String reason) {
        if (reason == null) {
            return duration(get("ban.default-duration", "7d"), "ban.default-duration", DEFAULT_BAN_SECONDS);
        }
        String lower = reason.toLowerCase();
        Map<String, Object> reasons = plugin.getConfig().getConfigurationSection("ban.reasons") != null
                ? plugin.getConfig().getConfigurationSection("ban.reasons").getValues(false)
                : new HashMap<>();
        for (Map.Entry<String, Object> entry : reasons.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase()) && !entry.getKey().equals("default")) {
                return duration(entry.getValue().toString(),
                        "ban.reasons." + entry.getKey(), DEFAULT_BAN_SECONDS);
            }
        }
        Object def = reasons.get("default");
        if (def != null) return duration(def.toString(), "ban.reasons.default", DEFAULT_BAN_SECONDS);
        return duration(get("ban.default-duration", "7d"), "ban.default-duration", DEFAULT_BAN_SECONDS);
    }

    public long getMuteDuration(String reason) {
        if (reason == null) {
            return duration(get("mute.default-duration", "1d"), "mute.default-duration", DEFAULT_MUTE_SECONDS);
        }
        String lower = reason.toLowerCase();
        Map<String, Object> reasons = plugin.getConfig().getConfigurationSection("mute.reasons") != null
                ? plugin.getConfig().getConfigurationSection("mute.reasons").getValues(false)
                : new HashMap<>();
        for (Map.Entry<String, Object> entry : reasons.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase()) && !entry.getKey().equals("default")) {
                return duration(entry.getValue().toString(),
                        "mute.reasons." + entry.getKey(), DEFAULT_MUTE_SECONDS);
            }
        }
        Object def = reasons.get("default");
        if (def != null) return duration(def.toString(), "mute.reasons.default", DEFAULT_MUTE_SECONDS);
        return duration(get("mute.default-duration", "1d"), "mute.default-duration", DEFAULT_MUTE_SECONDS);
    }

    /** Duration (seconds) for the /offend quick-ban, from config "offend.duration". */
    public long getOffendDuration() {
        return duration(get("offend.duration", "7d"), "offend.duration", DEFAULT_BAN_SECONDS);
    }

    /** Fixed reason for the /offend quick-ban. */
    public String getOffendReason() {
        return get("offend.reason", "Rule violation");
    }

    /** A selectable /offend ban reason: id, display text, duration and GUI icon. */
    public record OffendReason(String id, String display, long durationSeconds, String icon) {}

    /**
     * The selectable /offend ban reasons. Configurable under
     * {@code offend.reasons.<id>: {name, duration, icon}}; when the section is
     * absent, a built-in set of standard offense categories is used (the last
     * entry falls back to the legacy single offend.reason/duration pair).
     */
    public java.util.List<OffendReason> getOffendReasons() {
        java.util.List<OffendReason> out = new java.util.ArrayList<>();
        var section = plugin.getConfig().getConfigurationSection("offend.reasons");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                var s = section.getConfigurationSection(id);
                if (s == null) continue;
                out.add(new OffendReason(id.toLowerCase(),
                        s.getString("name", id),
                        duration(s.getString("duration", "7d"),
                                "offend.reasons." + id + ".duration", DEFAULT_BAN_SECONDS),
                        s.getString("icon", "PAPER")));
            }
        }
        if (out.isEmpty()) {
            out.add(new OffendReason("hacking", "Unfair Advantage (Hacking)",
                    TextUtil.parseDuration("30d"), "DIAMOND_SWORD"));
            out.add(new OffendReason("chat", "Chat Abuse",
                    TextUtil.parseDuration("7d"), "WRITABLE_BOOK"));
            out.add(new OffendReason("teaming", "Teaming / Cross-Teaming",
                    TextUtil.parseDuration("7d"), "LEAD"));
            out.add(new OffendReason("bugabuse", "Bug Abuse",
                    TextUtil.parseDuration("14d"), "REDSTONE"));
            out.add(new OffendReason("griefing", "Griefing / Trolling",
                    TextUtil.parseDuration("14d"), "TNT"));
            out.add(new OffendReason("evasion", "Ban Evasion (Alt Account)",
                    TextUtil.parseDuration("30d"), "SKELETON_SKULL"));
            out.add(new OffendReason("other", getOffendReason(), getOffendDuration(), "PAPER"));
        }
        return out;
    }

    /** Fixed reason for the /punish permanent ban. */
    public String getPunishReason() {
        return get("punish.reason", "Severe rule violation");
    }

    public String getDiscord() {
        return get("discord.invite", "https://discord.gg/lemonpvp");
    }
}
