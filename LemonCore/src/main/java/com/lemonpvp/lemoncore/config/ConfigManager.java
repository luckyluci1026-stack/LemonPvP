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

    public long getBanDuration(String reason) {
        if (reason == null) return TextUtil.parseDuration(get("ban.default-duration", "7d"));
        String lower = reason.toLowerCase();
        Map<String, Object> reasons = plugin.getConfig().getConfigurationSection("ban.reasons") != null
                ? plugin.getConfig().getConfigurationSection("ban.reasons").getValues(false)
                : new HashMap<>();
        for (Map.Entry<String, Object> entry : reasons.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase()) && !entry.getKey().equals("default")) {
                return TextUtil.parseDuration(entry.getValue().toString());
            }
        }
        Object def = reasons.get("default");
        return TextUtil.parseDuration(def != null ? def.toString() : get("ban.default-duration", "7d"));
    }

    public long getMuteDuration(String reason) {
        if (reason == null) return TextUtil.parseDuration(get("mute.default-duration", "1d"));
        String lower = reason.toLowerCase();
        Map<String, Object> reasons = plugin.getConfig().getConfigurationSection("mute.reasons") != null
                ? plugin.getConfig().getConfigurationSection("mute.reasons").getValues(false)
                : new HashMap<>();
        for (Map.Entry<String, Object> entry : reasons.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase()) && !entry.getKey().equals("default")) {
                return TextUtil.parseDuration(entry.getValue().toString());
            }
        }
        Object def = reasons.get("default");
        return TextUtil.parseDuration(def != null ? def.toString() : get("mute.default-duration", "1d"));
    }

    /** Duration (seconds) for the /offend quick-ban, from config "offend.duration". */
    public long getOffendDuration() {
        return TextUtil.parseDuration(get("offend.duration", "7d"));
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
                        TextUtil.parseDuration(s.getString("duration", "7d")),
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
