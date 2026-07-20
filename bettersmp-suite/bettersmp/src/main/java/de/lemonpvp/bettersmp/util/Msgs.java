package de.lemonpvp.bettersmp.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Laedt messages.yml (mit Fallback auf die im Jar mitgelieferten Standards)
 * und rendert Nachrichten mit %platzhalter%-Ersetzung als MiniMessage.
 */
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

    /** Ersetzt Platzhalter-Paare (key, value, key, value ...) und %prefix%. */
    public Component format(String path, String... replacements) {
        String text = raw(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        text = text.replace("%prefix%", raw("prefix"));
        text = text.replace("%brand%", plugin.getConfig().getString("brand", "SMP"));
        return Text.mm(text);
    }

    public void send(CommandSender to, String path, String... replacements) {
        to.sendMessage(format(path, replacements));
    }

    public void broadcast(String path, String... replacements) {
        Bukkit.getServer().sendMessage(format(path, replacements));
    }

    public static void safeSave(YamlConfiguration cfg, File file) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Konnte " + file + " nicht speichern", e);
        }
    }
}
