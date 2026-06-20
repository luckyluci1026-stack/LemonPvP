package com.lemonpvp.lemonqueue.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code config.yml} from the plugin data directory, copying the bundled
 * default on first run. All lookups are null-safe with sensible fallbacks so a
 * partial config never crashes the proxy.
 */
public class QueueConfig {

    private String limboServer = "limbo";
    private String defaultTarget = "lobby";
    private String bypassPermission = "lemonqueue.bypass";
    private String motd = "";

    private long processInterval = 1000L;
    private long updateInterval = 250L;
    private int sendBatch = 5;

    // Animated title/subtitle shown while waiting in the limbo.
    private boolean titleEnabled = true;

    // Built-in language ("de"/"en") and resolved message bundle.
    private String language = "de";
    private Messages messages = Messages.load("de", null);

    private final Map<String, Integer> maxPlayers = new LinkedHashMap<>();
    private final Map<String, Integer> priorities = new LinkedHashMap<>();

    public static QueueConfig load(Path dataDir, Logger logger) {
        QueueConfig cfg = new QueueConfig();
        try {
            Files.createDirectories(dataDir);
            Path file = dataDir.resolve("config.yml");
            if (Files.notExists(file)) {
                try (InputStream in = QueueConfig.class.getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) Files.copy(in, file);
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                cfg.apply(new Yaml().load(in));
            }
        } catch (IOException e) {
            logger.warn("[LemonQueue] Could not load config.yml, using defaults: {}", e.getMessage());
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    private void apply(Object raw) {
        if (!(raw instanceof Map<?, ?> root)) return;
        Map<String, Object> map = (Map<String, Object>) root;

        limboServer      = str(map, "limbo-server", limboServer);
        defaultTarget    = str(map, "default-target", defaultTarget);
        bypassPermission = str(map, "bypass-permission", bypassPermission);
        motd             = str(map, "motd", motd);
        processInterval  = lng(map, "process-interval", processInterval);
        updateInterval   = lng(map, "update-interval", updateInterval);
        sendBatch        = (int) lng(map, "send-batch", sendBatch);
        language         = str(map, "language", language);
        titleEnabled     = bool(map, "show-title", titleEnabled);
        messages         = Messages.load(language, map.get("messages"));

        if (map.get("max-players") instanceof Map<?, ?> mp) {
            for (Map.Entry<?, ?> e : mp.entrySet()) {
                maxPlayers.put(String.valueOf(e.getKey()), toInt(e.getValue(), 100));
            }
        }
        if (map.get("priorities") instanceof Map<?, ?> pr) {
            for (Map.Entry<?, ?> e : pr.entrySet()) {
                priorities.put(String.valueOf(e.getKey()), toInt(e.getValue(), 0));
            }
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public String getLimboServer()      { return limboServer; }
    public String getDefaultTarget()    { return defaultTarget; }
    public String getBypassPermission() { return bypassPermission; }
    public String getMotd()             { return motd; }
    public long getProcessInterval()    { return processInterval; }
    public long getUpdateInterval()     { return updateInterval; }
    public int getSendBatch()           { return Math.max(1, sendBatch); }
    public boolean isTitleEnabled()     { return titleEnabled; }
    public String getLanguage()         { return language; }
    public Messages getMessages()       { return messages; }
    public Map<String, Integer> getPriorities() { return priorities; }

    /** Max players for a target server; {@link Integer#MAX_VALUE} if uncapped. */
    public int getMaxPlayers(String server) {
        return maxPlayers.getOrDefault(server, Integer.MAX_VALUE);
    }

    // ── Parsing helpers ──────────────────────────────────────────────────

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v != null ? String.valueOf(v) : def;
    }

    private static long lng(Map<String, Object> m, String k, long def) {
        return toLong(m.get(k), def);
    }

    private static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        if (v instanceof Boolean b) return b;
        if (v != null) return Boolean.parseBoolean(v.toString().trim());
        return def;
    }

    private static int toInt(Object o, int def) {
        return (int) toLong(o, def);
    }

    private static long toLong(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        if (o != null) {
            try { return Long.parseLong(o.toString().trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
