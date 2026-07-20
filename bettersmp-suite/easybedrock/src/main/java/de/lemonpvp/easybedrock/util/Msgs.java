package de.lemonpvp.easybedrock.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Laedt messages.yml (mit Jar-Fallback) und rendert MiniMessage-Nachrichten. */
public final class Msgs {

    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private YamlConfiguration defaults;

    public Msgs(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        var res = plugin.getResource("messages.yml");
        this.defaults = res == null
                ? new YamlConfiguration()
                : YamlConfiguration.loadConfiguration(new InputStreamReader(res, StandardCharsets.UTF_8));
    }

    public String raw(String path) {
        String value = messages.getString(path, defaults.getString(path));
        return value == null ? path : value;
    }

    public Component format(String path, String... replacements) {
        String text = raw(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        text = text.replace("%prefix%", raw("prefix"));
        return Text.mm(text);
    }

    public void send(CommandSender to, String path, String... replacements) {
        to.sendMessage(format(path, replacements));
    }

    public void broadcast(String path, String... replacements) {
        org.bukkit.Bukkit.getServer().sendMessage(format(path, replacements));
    }
}
