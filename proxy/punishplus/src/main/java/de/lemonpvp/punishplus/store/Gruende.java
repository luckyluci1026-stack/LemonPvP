package de.lemonpvp.punishplus.store;

import de.lemonpvp.punishplus.util.Durations;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Vorgefertigte Gruende aus bans.yml - Text + Dauer (Dauer gilt nur fuer
 * /offend, /punish ist immer dauerhaft, siehe PunishManager).
 */
public final class Gruende {

    private final Path folder;
    private final Logger log;
    private final Map<String, Grund> gruende = new LinkedHashMap<>();

    public Gruende(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
    }

    public void load() {
        gruende.clear();
        Path file = folder.resolve("bans.yml");
        try {
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }
            if (Files.notExists(file)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("bans.yml")) {
                    if (in != null) {
                        Files.copy(in, file);
                        log.info("bans.yml angelegt: {}", file.toAbsolutePath());
                    }
                }
            }
            if (Files.notExists(file)) {
                return;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Object parsed = new Yaml().load(reader);
                if (!(parsed instanceof Map<?, ?> root)) {
                    return;
                }
                Object rawGruende = root.get("gruende");
                if (!(rawGruende instanceof Map<?, ?> map)) {
                    return;
                }
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String id = String.valueOf(entry.getKey());
                    if (!(entry.getValue() instanceof Map<?, ?> daten)) {
                        continue;
                    }
                    Object textRoh = daten.get("text");
                    String text = textRoh == null ? id : String.valueOf(textRoh);
                    Object dauerRoh = daten.get("dauer");
                    long dauer = Durations.parse(dauerRoh == null ? "0" : String.valueOf(dauerRoh));
                    gruende.put(id.toUpperCase(Locale.ROOT), new Grund(id, text, dauer));
                }
            }
        } catch (IOException ex) {
            log.error("bans.yml konnte nicht gelesen werden - keine Gruende verfuegbar", ex);
        }
    }

    public Optional<Grund> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(gruende.get(id.toUpperCase(Locale.ROOT)));
    }

    public String liste() {
        return String.join(", ", gruende.keySet());
    }
}
