package com.lemonpvp.lemoncore.config;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {

    private final LemonCore plugin;
    private FileConfiguration messages;
    private File file;

    public MessagesManager(LemonCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String path) {
        String prefix = messages.getString("prefix", "<bold><gradient:#fffb00:#00ff00>LemonPvP</gradient></bold> <dark_gray>»</dark_gray>");
        String val = messages.getString(path, "&cMessage not found: " + path);
        return val.replace("{prefix}", prefix);
    }

    public Component get(String path) {
        return TextUtil.parse(getRaw(path));
    }

    public Component get(String path, Map<String, String> placeholders) {
        String raw = getRaw(path);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }
        return TextUtil.parse(raw);
    }

    public Component get(String path, String key, String value) {
        return get(path, Map.of(key, value));
    }

    public Component get(String path, String k1, String v1, String k2, String v2) {
        Map<String, String> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return get(path, map);
    }

    public Component get(String path, String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return get(path, map);
    }
}
