package de.lemonpvp.punishplus.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard-Speicherung: eigene gesperrt.yml, nur fuer DIESEN Server.
 *
 * Weder /offend noch /punish lassen sich per Befehl aufheben - nur von
 * Hand in dieser Datei editieren, danach /punishplus reload.
 */
public final class YamlPunishRepository implements PunishRepository {

    private final JavaPlugin plugin;
    private File datei;
    private final Map<UUID, PunishRecord> gesperrt = new ConcurrentHashMap<>();

    public YamlPunishRepository(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        datei = new File(plugin.getDataFolder(), "gesperrt.yml");
        gesperrt.clear();
        if (!datei.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(datei);
        ConfigurationSection sec = yaml.getConfigurationSection("eintraege");
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection eintrag = sec.getConfigurationSection(key);
            if (eintrag == null) {
                continue;
            }
            try {
                UUID id = UUID.fromString(key);
                gesperrt.put(id, new PunishRecord(
                        id,
                        eintrag.getString("name", "?"),
                        eintrag.getString("grundId", "?"),
                        eintrag.getString("grundText", "?"),
                        eintrag.getString("art", "PUNISH"),
                        eintrag.getLong("von", 0L),
                        eintrag.getLong("bis", 0L),
                        eintrag.getString("ausfuehrer", "Konsole")));
            } catch (IllegalArgumentException ignored) {
                // Kaputte Zeile (z.B. von Hand editiert) - ueberspringen.
            }
        }
    }

    @Override
    public void speichern(PunishRecord record) {
        gesperrt.put(record.spieler(), record);
        schreibeDatei();
    }

    @Override
    public Optional<PunishRecord> aktiv(UUID spieler) {
        PunishRecord record = gesperrt.get(spieler);
        if (record == null) {
            return Optional.empty();
        }
        if (!record.aktiv()) {
            gesperrt.remove(spieler);
            schreibeDatei();
            return Optional.empty();
        }
        return Optional.of(record);
    }

    private void schreibeDatei() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PunishRecord eintrag : gesperrt.values()) {
            String pfad = "eintraege." + eintrag.spieler();
            yaml.set(pfad + ".name", eintrag.spielerName());
            yaml.set(pfad + ".grundId", eintrag.grundId());
            yaml.set(pfad + ".grundText", eintrag.grundText());
            yaml.set(pfad + ".art", eintrag.art());
            yaml.set(pfad + ".von", eintrag.von());
            yaml.set(pfad + ".bis", eintrag.bis());
            yaml.set(pfad + ".ausfuehrer", eintrag.ausfuehrer());
        }
        try {
            yaml.save(datei);
        } catch (IOException e) {
            plugin.getLogger().warning("gesperrt.yml liess sich nicht speichern: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        // Keine Ressourcen zu schliessen - reine Datei-IO.
    }
}
