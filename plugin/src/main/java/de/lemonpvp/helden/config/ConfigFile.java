package de.lemonpvp.helden.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/** Eine zusaetzliche YAML-Datei im Pluginordner, die aus dem Jar vorbefuellt wird. */
public final class ConfigFile {

    private final Plugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration configuration;

    public ConfigFile(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration get() {
        if (configuration == null) {
            reload();
        }
        return configuration;
    }

    public void save() {
        if (configuration == null) {
            return;
        }
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Konnte " + name + " nicht speichern.", exception);
        }
    }

    public File file() {
        return file;
    }

    public String name() {
        return name;
    }
}
