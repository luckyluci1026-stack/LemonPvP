package de.lemonpvp.punishplus;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liest die config.yml (nur Nachrichten/Prefix - die Gruende stehen in
 * bans.yml, siehe Gruende.java). Gleiches Vorbild wie SMPProxys eigene
 * ProxyConfig, hier ohne den Routing-Kram, den PunishPlus nicht braucht.
 */
public final class Config {

    private final Path folder;
    private final Logger log;

    private Map<String, Object> root = new LinkedHashMap<>();

    public Config(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
    }

    public void load() {
        Path file = folder.resolve("config.yml");
        try {
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }
            if (Files.notExists(file)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, file);
                        log.info("config.yml angelegt: {}", file.toAbsolutePath());
                    }
                }
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Object parsed = new Yaml().load(reader);
                root = parsed instanceof Map<?, ?> ? castMap(parsed) : new LinkedHashMap<>();
            }
        } catch (IOException ex) {
            log.error("config.yml konnte nicht gelesen werden - benutze Standardwerte", ex);
            root = new LinkedHashMap<>();
        }
    }

    public String prefix() {
        return string("messages.prefix", "");
    }

    public String message(String key) {
        return string("messages." + key, "");
    }

    /** Reines Wort, kein Nachrichten-Baustein - wird in andere Texte eingesetzt. */
    public String permanentWord() {
        return string("messages.permanent-word", "für immer");
    }

    private Object get(String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    private String string(String path, String fallback) {
        Object value = get(path);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
