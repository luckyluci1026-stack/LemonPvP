package de.lemonpvp.punishplus.store;

import de.lemonpvp.punishplus.util.Durations;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Laedt die vorgefertigten Gruende aus bans.yml. */
public final class Gruende {

    private final JavaPlugin plugin;
    private File datei;
    private final Map<String, Grund> gruende = new LinkedHashMap<>();

    public Gruende(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        datei = new File(plugin.getDataFolder(), "bans.yml");
        if (!datei.exists()) {
            plugin.saveResource("bans.yml", false);
        }
        gruende.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(datei);
        ConfigurationSection sec = yaml.getConfigurationSection("gruende");
        if (sec == null) {
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection eintrag = sec.getConfigurationSection(id);
            if (eintrag == null) {
                continue;
            }
            String text = eintrag.getString("text", id);
            long dauer = Durations.parse(eintrag.getString("dauer", "perm"));
            String idGross = id.toUpperCase(Locale.ROOT);
            gruende.put(idGross, new Grund(idGross, text, dauer));
        }
    }

    public Optional<Grund> get(String id) {
        return Optional.ofNullable(gruende.get(id.toUpperCase(Locale.ROOT)));
    }

    public String liste() {
        return String.join(", ", gruende.keySet());
    }
}
