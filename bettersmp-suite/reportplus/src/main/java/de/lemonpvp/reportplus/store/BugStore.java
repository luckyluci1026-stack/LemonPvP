package de.lemonpvp.reportplus.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bugmeldungen (/bugreport) - eigene bugreports.yml, gleiches Muster wie ReportStore. */
public final class BugStore {

    private final JavaPlugin plugin;
    private File datei;
    private YamlConfiguration daten;

    public BugStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        datei = new File(plugin.getDataFolder(), "bugreports.yml");
        daten = YamlConfiguration.loadConfiguration(datei);
    }

    public BugEntry anlegen(UUID reporter, String reporterName, String kategorie, String text) {
        int id = daten.getInt("naechste-id", 1);
        daten.set("naechste-id", id + 1);
        String pfad = "eintraege." + id;
        long zeit = System.currentTimeMillis();
        daten.set(pfad + ".reporter", reporter.toString());
        daten.set(pfad + ".reporterName", reporterName);
        daten.set(pfad + ".kategorie", kategorie);
        daten.set(pfad + ".text", text);
        daten.set(pfad + ".zeit", zeit);
        daten.set(pfad + ".status", "OFFEN");
        speichern();
        return new BugEntry(id, reporter, reporterName, kategorie, text, zeit, "OFFEN");
    }

    public List<BugEntry> offene() {
        List<BugEntry> raus = new ArrayList<>();
        for (BugEntry eintrag : alle()) {
            if (eintrag.offen()) {
                raus.add(eintrag);
            }
        }
        raus.sort((a, b) -> Long.compare(b.zeit(), a.zeit()));
        return raus;
    }

    public Optional<BugEntry> get(int id) {
        for (BugEntry eintrag : alle()) {
            if (eintrag.id() == id) {
                return Optional.of(eintrag);
            }
        }
        return Optional.empty();
    }

    public boolean setzeStatus(int id, String status) {
        ConfigurationSection sec = daten.getConfigurationSection("eintraege." + id);
        if (sec == null) {
            return false;
        }
        sec.set("status", status);
        speichern();
        return true;
    }

    private List<BugEntry> alle() {
        List<BugEntry> raus = new ArrayList<>();
        ConfigurationSection sec = daten.getConfigurationSection("eintraege");
        if (sec == null) {
            return raus;
        }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection eintrag = sec.getConfigurationSection(key);
            if (eintrag == null) {
                continue;
            }
            try {
                raus.add(new BugEntry(
                        Integer.parseInt(key),
                        UUID.fromString(eintrag.getString("reporter", "")),
                        eintrag.getString("reporterName", "?"),
                        eintrag.getString("kategorie", "SONSTIGES"),
                        eintrag.getString("text", ""),
                        eintrag.getLong("zeit", 0L),
                        eintrag.getString("status", "OFFEN")));
            } catch (IllegalArgumentException ignored) {
                // Kaputte Zeile - ueberspringen.
            }
        }
        return raus;
    }

    private void speichern() {
        try {
            daten.save(datei);
        } catch (IOException e) {
            plugin.getLogger().warning("bugreports.yml liess sich nicht speichern: " + e.getMessage());
        }
    }
}
