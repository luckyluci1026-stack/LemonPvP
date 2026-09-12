package de.lemonpvp.reportplus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Laedt messages.yml, ersetzt Platzhalter-Paare (key, value, key, value ...) und %prefix%. */
public final class Msgs {

    private final JavaPlugin plugin;
    private final YamlConfiguration messages;
    private final YamlConfiguration defaults;

    public Msgs(JavaPlugin plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("messages.yml")) {
            this.defaults = in == null ? new YamlConfiguration()
                    : YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("messages.yml im Jar fehlt", e);
        }
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
        return MiniMessage.miniMessage().deserialize(text);
    }

    public void send(CommandSender to, String path, String... replacements) {
        to.sendMessage(format(path, replacements));
    }
}
