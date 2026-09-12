package de.lemonpvp.punishplus.store;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wer gerade gesperrt ist - eigene gesperrt.yml im Plugin-Ordner.
 *
 * Laeuft direkt am Proxy: ein einziger Prozess fuer das ganze Netzwerk,
 * deshalb reicht eine einfache Datei fuer echte Netzwerkweite ohne
 * Datenbank - anders als eine Paper-Version, die auf jedem Server
 * einzeln liefe und ihren Zustand erst irgendwie teilen muesste.
 *
 * Weder /offend noch /punish lassen sich per Befehl aufheben - nur von
 * Hand in dieser Datei editieren, danach /punishplus reload.
 */
public final class PunishStore {

    private final Path folder;
    private final Logger log;
    private final Map<UUID, PunishRecord> gesperrt = new ConcurrentHashMap<>();

    public PunishStore(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
    }

    private Path datei() {
        return folder.resolve("gesperrt.yml");
    }

    public void load() {
        gesperrt.clear();
        Path file = datei();
        if (Files.notExists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object parsed = new Yaml().load(reader);
            if (!(parsed instanceof Map<?, ?> root)) {
                return;
            }
            Object rawEintraege = root.get("eintraege");
            if (!(rawEintraege instanceof Map<?, ?> eintraege)) {
                return;
            }
            for (Map.Entry<?, ?> entry : eintraege.entrySet()) {
                UUID id;
                try {
                    id = UUID.fromString(String.valueOf(entry.getKey()));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (!(entry.getValue() instanceof Map<?, ?> daten)) {
                    continue;
                }
                gesperrt.put(id, new PunishRecord(
                        id,
                        stringOrDefault(daten, "name", "?"),
                        stringOrDefault(daten, "grundId", "?"),
                        stringOrDefault(daten, "grundText", "?"),
                        stringOrDefault(daten, "art", "PUNISH"),
                        longOrDefault(daten, "von"),
                        longOrDefault(daten, "bis"),
                        stringOrDefault(daten, "ausfuehrer", "Konsole")));
            }
        } catch (IOException ex) {
            log.error("gesperrt.yml konnte nicht gelesen werden - bestehende Sperren sind bis " +
                    "zum naechsten Neustart nicht bekannt", ex);
        }
    }

    public void speichern(PunishRecord record) {
        gesperrt.put(record.spieler(), record);
        schreibeDatei();
    }

    /** Aktive Sperre, falls vorhanden - raeumt abgelaufene automatisch weg. */
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
        Map<String, Object> eintraege = new LinkedHashMap<>();
        for (PunishRecord record : gesperrt.values()) {
            Map<String, Object> daten = new LinkedHashMap<>();
            daten.put("name", record.spielerName());
            daten.put("grundId", record.grundId());
            daten.put("grundText", record.grundText());
            daten.put("art", record.art());
            daten.put("von", record.von());
            daten.put("bis", record.bis());
            daten.put("ausfuehrer", record.ausfuehrer());
            eintraege.put(record.spieler().toString(), daten);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("eintraege", eintraege);
        try {
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }
            DumperOptions optionen = new DumperOptions();
            optionen.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            try (Writer writer = Files.newBufferedWriter(datei(), StandardCharsets.UTF_8)) {
                new Yaml(optionen).dump(root, writer);
            }
        } catch (IOException ex) {
            log.error("gesperrt.yml konnte nicht gespeichert werden - Aenderung ist nur bis " +
                    "zum naechsten Neustart gueltig!", ex);
        }
    }

    private static String stringOrDefault(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static long longOrDefault(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException | NullPointerException ex) {
            return 0L;
        }
    }
}
