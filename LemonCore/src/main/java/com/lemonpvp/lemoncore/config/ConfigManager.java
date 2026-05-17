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

    public String getDiscord() {
        return get("discord.invite", "https://discord.gg/lemonpvp");
    }
}
