package de.lemonpvp.smpproxy.ban;

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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wer beim Proxy nicht mehr reinkommt.
 *
 * Das ist bewusst eine eigene, kleine Ablage direkt im Proxy und nicht
 * an BetterSMPs eigenes /gban angehaengt (das ueber MariaDB geteilt
 * werden KANN, aber nicht muss): Ein Netzwerkbann soll wirken, bevor der
 * Spieler ueberhaupt einen Server sieht - auch die Lobby, auch wenn kein
 * einziger Bukkit-Server gerade laeuft. Dafuer darf der Proxy nicht von
 * einer Datenbank abhaengen, die er selbst gar nicht spricht.
 *
 * Zwei Dateien im Plugin-Ordner:
 *   bans.yml       - UUID -> {name, reason, by, at, until}
 *   spieler.yml     - Name (klein) -> UUID, fuer jeden, der sich je
 *                      verbunden hat. Ohne sie liesse sich ein Offline-
 *                      Spieler nur per UUID bannen, und die kennt kaum
 *                      ein Lehrer auswendig.
 */
public final class BanStore {

    private final Path folder;
    private final Logger log;

    private final Map<UUID, BanEntry> bans = new ConcurrentHashMap<>();
    /** Name klein geschrieben -> {UUID, zuletzt gesehener Name in echter Schreibweise}. */
    private final Map<String, UUID> bekannt = new ConcurrentHashMap<>();
    private final Map<UUID, String> anzeigename = new ConcurrentHashMap<>();

    public BanStore(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
    }

    public void load() {
        bans.clear();
        loadBans();
        bekannt.clear();
        anzeigename.clear();
        loadKnown();
    }

    // ------------------------------------------------------------ bans.yml

    private Path bansFile() {
        return folder.resolve("bans.yml");
    }

    @SuppressWarnings("unchecked")
    private void loadBans() {
        Path file = bansFile();
        if (Files.notExists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object parsed = new Yaml().load(reader);
            if (!(parsed instanceof Map<?, ?> root)) {
                return;
            }
            for (Map.Entry<?, ?> entry : root.entrySet()) {
                UUID id;
                try {
                    id = UUID.fromString(String.valueOf(entry.getKey()));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (!(entry.getValue() instanceof Map)) {
                    continue;
                }
                Map<String, Object> daten = (Map<String, Object>) entry.getValue();
                bans.put(id, new BanEntry(
                        id,
                        String.valueOf(daten.getOrDefault("name", "?")),
                        String.valueOf(daten.getOrDefault("reason", "Kein Grund angegeben")),
                        String.valueOf(daten.getOrDefault("by", "?")),
                        toLong(daten.get("at")),
                        toLong(daten.get("until"))));
            }
        } catch (IOException ex) {
            log.error("bans.yml konnte nicht gelesen werden - Netzwerkbanns sind bis zum " +
                    "naechsten Neustart nicht bekannt", ex);
        }
    }

    private void saveBans() {
        Map<String, Object> root = new LinkedHashMap<>();
        for (BanEntry ban : bans.values()) {
            Map<String, Object> daten = new LinkedHashMap<>();
            daten.put("name", ban.name());
            daten.put("reason", ban.reason());
            daten.put("by", ban.by());
            daten.put("at", ban.at());
            daten.put("until", ban.until());
            root.put(ban.player().toString(), daten);
        }
        write(bansFile(), root, "Netzwerkbanns - von /netban erzeugt und gelesen.\n"
                + "Von Hand editieren geht, wird aber beim naechsten /netban ueberschrieben.");
    }

    // ------------------------------------------------------------ spieler.yml

    private Path knownFile() {
        return folder.resolve("spieler.yml");
    }

    @SuppressWarnings("unchecked")
    private void loadKnown() {
        Path file = knownFile();
        if (Files.notExists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object parsed = new Yaml().load(reader);
            if (!(parsed instanceof Map<?, ?> root)) {
                return;
            }
            for (Map.Entry<?, ?> entry : root.entrySet()) {
                UUID id;
                try {
                    id = UUID.fromString(String.valueOf(entry.getValue()));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                String name = String.valueOf(entry.getKey());
                bekannt.put(name.toLowerCase(Locale.ROOT), id);
                anzeigename.put(id, name);
            }
        } catch (IOException ex) {
            log.warn("spieler.yml konnte nicht gelesen werden - Bann per Namen " +
                    "funktioniert erst, sobald sich der Spieler wieder verbindet.", ex);
        }
    }

    private void saveKnown() {
        Map<String, Object> root = new LinkedHashMap<>();
        // Anzeigename (richtige Gross-/Kleinschreibung) als Schluessel -
        // beim Lesen wird ohnehin klein verglichen.
        for (Map.Entry<UUID, String> entry : anzeigename.entrySet()) {
            root.put(entry.getValue(), entry.getKey().toString());
        }
        write(knownFile(), root, "Jeder Name, der sich hier je verbunden hat -> UUID.\n"
                + "Damit /netban <Name> auch offline funktioniert. Wird bei jedem Login "
                + "aktualisiert, nicht von Hand pflegen.");
    }

    /** Bei jedem Login aufrufen - haelt spieler.yml aktuell. */
    public void remember(UUID id, String name) {
        String klein = name.toLowerCase(Locale.ROOT);
        UUID vorher = bekannt.put(klein, id);
        String vorherName = anzeigename.put(id, name);
        if (!id.equals(vorher) || !name.equals(vorherName)) {
            saveKnown();
        }
    }

    /** UUID zu einem Namen - nur, wenn sich diese Person hier schon einmal verbunden hat. */
    public Optional<UUID> uuidFuer(String name) {
        return Optional.ofNullable(bekannt.get(name.toLowerCase(Locale.ROOT)));
    }

    public String nameFuer(UUID id) {
        return anzeigename.getOrDefault(id, id.toString());
    }

    // ------------------------------------------------------------ Banns

    public Optional<BanEntry> aktiv(UUID id) {
        BanEntry ban = bans.get(id);
        if (ban == null) {
            return Optional.empty();
        }
        if (!ban.active()) {
            // Abgelaufen: gleich aufraeumen, statt bei jedem Login neu zu pruefen.
            bans.remove(id);
            saveBans();
            return Optional.empty();
        }
        return Optional.of(ban);
    }

    public void ban(UUID id, String name, String reason, String by, long durationMillis) {
        long until = durationMillis <= 0 ? 0 : System.currentTimeMillis() + durationMillis;
        bans.put(id, new BanEntry(id, name, reason, by, System.currentTimeMillis(), until));
        saveBans();
    }

    /** @return true, wenn wirklich ein Bann aufgehoben wurde. */
    public boolean unban(UUID id) {
        boolean weg = bans.remove(id) != null;
        if (weg) {
            saveBans();
        }
        return weg;
    }

    public Map<UUID, BanEntry> alle() {
        return Map.copyOf(bans);
    }

    // ------------------------------------------------------------ Kleinkram

    private static long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException | NullPointerException ex) {
            return 0L;
        }
    }

    private void write(Path file, Map<String, Object> root, String kopf) {
        try {
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }
            DumperOptions optionen = new DumperOptions();
            optionen.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            String kommentar = kopf.lines()
                    .map(zeile -> "# " + zeile)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("") + "\n\n";
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(kommentar);
                new Yaml(optionen).dump(root, writer);
            }
        } catch (IOException ex) {
            log.error("{} konnte nicht geschrieben werden - Aenderung ist nur bis zum " +
                    "naechsten Neustart gueltig!", file.getFileName(), ex);
        }
    }

    /**
     * Beim allerersten Start eine Vorlage anlegen, damit im Ordner steht,
     * was diese Dateien sind - genau wie bei config.yml.
     */
    public void ensureFiles() {
        if (Files.notExists(bansFile())) {
            write(bansFile(), new LinkedHashMap<>(), "Netzwerkbanns - von /netban erzeugt und gelesen.\n"
                    + "Von Hand editieren geht, wird aber beim naechsten /netban ueberschrieben.");
        }
        if (Files.notExists(knownFile())) {
            write(knownFile(), new LinkedHashMap<>(), "Jeder Name, der sich hier je verbunden hat -> UUID.\n"
                    + "Damit /netban <Name> auch offline funktioniert. Wird bei jedem Login "
                    + "aktualisiert, nicht von Hand pflegen.");
        }
    }

}
