package de.lemonpvp.smpproxy.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Liest die config.yml. Beim ersten Start wird die mitgelieferte Vorlage
 * in den Plugin-Ordner kopiert, damit man alles bequem anpassen kann.
 */
public final class ProxyConfig {

    private final Path folder;
    private final Logger log;

    private Map<String, Object> root = new LinkedHashMap<>();

    /** Domain (klein geschrieben) -> Servername */
    private final Map<String, String> domains = new LinkedHashMap<>();
    /** Stern-Einträge wie "*.lemon-servers.de" -> Servername */
    private final Map<String, String> wildcards = new LinkedHashMap<>();

    public ProxyConfig(Path folder, Logger log) {
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
                root = parsed instanceof Map ? castMap(parsed) : new LinkedHashMap<>();
            }
        } catch (IOException ex) {
            log.error("config.yml konnte nicht gelesen werden - benutze Standardwerte", ex);
            root = new LinkedHashMap<>();
        }
        buildRoutes();
    }

    private void buildRoutes() {
        domains.clear();
        wildcards.clear();
        Object raw = root.get("domains");
        if (!(raw instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String host = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT).trim();
            String server = String.valueOf(entry.getValue()).trim();
            if (host.isEmpty() || server.isEmpty()) {
                continue;
            }
            if (host.startsWith("*.")) {
                wildcards.put(host.substring(1), server);
            } else if (host.equals("*")) {
                wildcards.put("", server);
            } else {
                domains.put(host, server);
            }
        }
    }

    // ------------------------------------------------------------------
    //  Routing
    // ------------------------------------------------------------------

    /** Liefert den Server zu einer Domain, sonst den Standardserver. */
    public String serverFor(String host) {
        String clean = normalise(host);
        if (clean != null && !clean.isEmpty()) {
            String direct = domains.get(clean);
            if (direct != null) {
                return direct;
            }
            for (Map.Entry<String, String> entry : wildcards.entrySet()) {
                String suffix = entry.getKey();
                if (suffix.isEmpty() || clean.endsWith(suffix)) {
                    return entry.getValue();
                }
            }
        }
        return defaultServer();
    }

    /**
     * Macht aus dem, was der Client geschickt hat, einen sauberen Hostnamen:
     * Kleinbuchstaben, ohne Port, ohne Punkt am Ende und ohne den
     * FML-Anhang von Forge-Clients.
     */
    public static String normalise(String host) {
        if (host == null) {
            return null;
        }
        String clean = host.trim().toLowerCase(Locale.ROOT);
        int nul = clean.indexOf('\0');
        if (nul >= 0) {
            clean = clean.substring(0, nul);
        }
        int colon = clean.indexOf(':');
        if (colon >= 0) {
            clean = clean.substring(0, colon);
        }
        while (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    public Map<String, String> domains() {
        return domains;
    }

    public String defaultServer() {
        return string("default-server", "");
    }

    public String limbo() {
        return string("limbo", "");
    }

    // ------------------------------------------------------------------
    //  Überwachung
    // ------------------------------------------------------------------

    public int pingInterval() {
        return Math.max(1, integer("monitor.interval", 5));
    }

    public int pingTimeout() {
        return Math.max(1, integer("monitor.timeout", 3));
    }

    public boolean pingOnStart() {
        return bool("monitor.check-on-start", true);
    }

    public boolean autoReturn() {
        return bool("auto-return.enabled", true);
    }

    public int returnConfirmations() {
        return Math.max(1, integer("auto-return.confirmations", 3));
    }

    public int returnCooldown() {
        return Math.max(1, integer("auto-return.cooldown", 5));
    }

    public boolean returnNotice() {
        return bool("auto-return.show-notice", true);
    }

    // ------------------------------------------------------------------
    //  Befehle
    // ------------------------------------------------------------------

    public boolean hubEnabled() {
        return bool("commands.hub-enabled", true);
    }

    public List<String> hubAliases() {
        return strings("commands.hub-aliases", List.of("hub", "lobby"));
    }

    public boolean serverShortcuts() {
        return bool("commands.server-shortcuts", true);
    }

    // ------------------------------------------------------------------
    //  Nachrichten
    // ------------------------------------------------------------------

    public String prefix() {
        return string("messages.prefix", "");
    }

    public String message(String key) {
        return string("messages." + key, "");
    }

    // ------------------------------------------------------------------
    //  Zugriff auf verschachtelte Werte ("a.b.c")
    // ------------------------------------------------------------------

    private Object get(String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }

    private String string(String path, String fallback) {
        Object value = get(path);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private int integer(String path, int fallback) {
        Object value = get(path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // Standardwert benutzen
            }
        }
        return fallback;
    }

    private boolean bool(String path, boolean fallback) {
        Object value = get(path);
        if (value instanceof Boolean b) {
            return b;
        }
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private List<String> strings(String path, List<String> fallback) {
        Object value = get(path);
        if (value instanceof List<?> list && !list.isEmpty()) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
