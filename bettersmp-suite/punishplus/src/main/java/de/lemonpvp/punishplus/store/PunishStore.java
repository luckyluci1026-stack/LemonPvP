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
 * Wer gerade gesperrt ist - eigene gesperrt.yml statt Datenbank-Schema.
 *
 * Weder /offend noch /punish lassen sich per Befehl aufheben - nur von
 * Hand in dieser Datei editieren, danach /punishplus reload.
 */
public final class PunishStore {

    private final JavaPlugin plugin;
    private File datei;
    private final Map<UUID, PunishRecord> gesperrt = new ConcurrentHashMap<>();

    public PunishStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

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

    public PunishRecord sperren(UUID spieler, String spielerName, Grund grund, String art,
                                 long dauerMillis, String ausfuehrer) {
        long jetzt = System.currentTimeMillis();
        long bis = dauerMillis <= 0 ? 0L : jetzt + dauerMillis;
        PunishRecord record = new PunishRecord(spieler, spielerName, grund.id(), grund.text(),
                art, jetzt, bis, ausfuehrer);
        gesperrt.put(spieler, record);
        speichern();
        return record;
    }

    /** Aktive Sperre, falls vorhanden - raeumt abgelaufene automatisch weg. */
    public Optional<PunishRecord> aktiv(UUID spieler) {
        PunishRecord record = gesperrt.get(spieler);
        if (record == null) {
            return Optional.empty();
        }
        if (!record.aktiv()) {
            gesperrt.remove(spieler);
            speichern();
            return Optional.empty();
        }
        return Optional.of(record);
    }

    private void speichern() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PunishRecord record : gesperrt.values()) {
            String pfad = "eintraege." + record.spieler();
            yaml.set(pfad + ".name", record.spielerName());
            yaml.set(pfad + ".grundId", record.grundId());
            yaml.set(pfad + ".grundText", record.grundText());
            yaml.set(pfad + ".art", record.art());
            yaml.set(pfad + ".von", record.von());
            yaml.set(pfad + ".bis", record.bis());
            yaml.set(pfad + ".ausfuehrer", record.ausfuehrer());
        }
        try {
            yaml.save(datei);
        } catch (IOException e) {
            plugin.getLogger().warning("gesperrt.yml liess sich nicht speichern: " + e.getMessage());
        }
    }
}
