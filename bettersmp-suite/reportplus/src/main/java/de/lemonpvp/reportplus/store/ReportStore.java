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

/**
 * Spieler-Meldungen (/report) - eigene reports.yml statt Datenbank-Schema
 * fuer eine Handvoll Felder pro Meldung.
 */
public final class ReportStore {

    private final JavaPlugin plugin;
    private File datei;
    private YamlConfiguration daten;

    public ReportStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        datei = new File(plugin.getDataFolder(), "reports.yml");
        daten = YamlConfiguration.loadConfiguration(datei);
    }

    public ReportEntry anlegen(UUID reporter, String reporterName, UUID ziel, String zielName, String kategorie) {
        int id = daten.getInt("naechste-id", 1);
        daten.set("naechste-id", id + 1);
        String pfad = "eintraege." + id;
        long zeit = System.currentTimeMillis();
        daten.set(pfad + ".reporter", reporter.toString());
        daten.set(pfad + ".reporterName", reporterName);
        daten.set(pfad + ".ziel", ziel.toString());
        daten.set(pfad + ".zielName", zielName);
        daten.set(pfad + ".kategorie", kategorie);
        daten.set(pfad + ".zeit", zeit);
        daten.set(pfad + ".status", "OFFEN");
        speichern();
        return new ReportEntry(id, reporter, reporterName, ziel, zielName, kategorie, zeit, "OFFEN");
    }

    public List<ReportEntry> offene() {
        List<ReportEntry> raus = new ArrayList<>();
        for (ReportEntry eintrag : alle()) {
            if (eintrag.offen()) {
                raus.add(eintrag);
            }
        }
        raus.sort((a, b) -> Long.compare(b.zeit(), a.zeit()));
        return raus;
    }

    public Optional<ReportEntry> get(int id) {
        for (ReportEntry eintrag : alle()) {
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

    private List<ReportEntry> alle() {
        List<ReportEntry> raus = new ArrayList<>();
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
                raus.add(new ReportEntry(
                        Integer.parseInt(key),
                        UUID.fromString(eintrag.getString("reporter", "")),
                        eintrag.getString("reporterName", "?"),
                        UUID.fromString(eintrag.getString("ziel", "")),
                        eintrag.getString("zielName", "?"),
                        eintrag.getString("kategorie", "SONSTIGES"),
                        eintrag.getLong("zeit", 0L),
                        eintrag.getString("status", "OFFEN")));
            } catch (IllegalArgumentException ignored) {
                // Kaputte Zeile (z.B. von Hand editiert) - einfach ueberspringen.
            }
        }
        return raus;
    }

    private void speichern() {
        try {
            daten.save(datei);
        } catch (IOException e) {
            plugin.getLogger().warning("reports.yml liess sich nicht speichern: " + e.getMessage());
        }
    }
}
