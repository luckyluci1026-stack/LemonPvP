package com.lemonpvp.lemonfailover;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Loads {@code config.yml} from the plugin data directory, copying the bundled
 * default on first run. All lookups are null-safe with sensible fallbacks so a
 * partial config never crashes the proxy.
 */
public final class FailoverConfig {

    private String primaryHost = "127.0.0.1";
    private int primaryPort = 25565;
    private long checkIntervalMs = 3000L;
    private int connectTimeoutMs = 2000;
    private int failureThreshold = 3;
    private int recoveryThreshold = 3;

    private boolean denyLogins = true;
    private boolean kickOnRecovery = false;

    private String bypassPermission = "lemonfailover.bypass";
    private String startMode = "standby";

    private String motdStandby = "";
    private String motdActive = "";

    private String denyStandby = "<red>The network is running on the main proxy.";
    private String kickedOnRecovery = "<yellow>The main proxy is back online. Please reconnect.";
    private String failoverActivated = "<red>Failover active — the main proxy is unreachable.";
    private String primaryRestored = "<green>The main proxy is back online.";

    private String discordWebhook = "";

    public static FailoverConfig load(Path dataDir, Logger logger) {
        FailoverConfig cfg = new FailoverConfig();
        try {
            Files.createDirectories(dataDir);
            Path file = dataDir.resolve("config.yml");
            if (Files.notExists(file)) {
                try (InputStream in = FailoverConfig.class.getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) Files.copy(in, file);
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                cfg.apply(new Yaml().load(in));
            }
        } catch (Exception e) {
            // Catches IO problems AND SnakeYAML's runtime YAMLException — a config
            // typo must never leave the failover plugin silently disabled.
            logger.warn("[LemonFailover] Could not load config.yml, using defaults: {}", e.getMessage());
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    private void apply(Object raw) {
        if (!(raw instanceof Map<?, ?> root)) return;
        Map<String, Object> map = (Map<String, Object>) root;

        if (map.get("primary") instanceof Map<?, ?> p) {
            Map<String, Object> pm = (Map<String, Object>) p;
            primaryHost      = str(pm, "host", primaryHost);
            primaryPort      = (int) lng(pm, "port", primaryPort);
            checkIntervalMs  = Math.max(500L, lng(pm, "check-interval-ms", checkIntervalMs));
            connectTimeoutMs = (int) Math.max(200L, lng(pm, "connect-timeout-ms", connectTimeoutMs));
            failureThreshold = (int) Math.max(1L, lng(pm, "failure-threshold", failureThreshold));
            recoveryThreshold = (int) Math.max(1L, lng(pm, "recovery-threshold", recoveryThreshold));
        }

        if (map.get("standby") instanceof Map<?, ?> s) {
            Map<String, Object> sm = (Map<String, Object>) s;
            denyLogins     = bool(sm, "deny-logins", denyLogins);
            kickOnRecovery = bool(sm, "kick-on-recovery", kickOnRecovery);
        }

        bypassPermission = str(map, "bypass-permission", bypassPermission);
        startMode        = str(map, "start-mode", startMode).toLowerCase(Locale.ROOT);

        if (map.get("motd") instanceof Map<?, ?> m) {
            Map<String, Object> mm = (Map<String, Object>) m;
            motdStandby = str(mm, "standby", motdStandby);
            motdActive  = str(mm, "active", motdActive);
        }

        if (map.get("messages") instanceof Map<?, ?> msg) {
            Map<String, Object> mg = (Map<String, Object>) msg;
            denyStandby       = str(mg, "deny-standby", denyStandby);
            kickedOnRecovery  = str(mg, "kicked-on-recovery", kickedOnRecovery);
            failoverActivated = str(mg, "failover-activated", failoverActivated);
            primaryRestored   = str(mg, "primary-restored", primaryRestored);
        }

        discordWebhook = str(map, "discord-webhook", discordWebhook);
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public String getPrimaryHost()     { return primaryHost; }
    public int getPrimaryPort()        { return primaryPort; }
    public long getCheckIntervalMs()   { return checkIntervalMs; }
    public int getConnectTimeoutMs()   { return connectTimeoutMs; }
    public int getFailureThreshold()   { return failureThreshold; }
    public int getRecoveryThreshold()  { return recoveryThreshold; }
    public boolean isDenyLogins()      { return denyLogins; }
    public boolean isKickOnRecovery()  { return kickOnRecovery; }
    public String getBypassPermission(){ return bypassPermission; }
    public String getStartMode()       { return startMode; }
    public String getMotdStandby()     { return motdStandby; }
    public String getMotdActive()      { return motdActive; }
    public String getDenyStandby()     { return denyStandby; }
    public String getKickedOnRecovery(){ return kickedOnRecovery; }
    public String getFailoverActivated(){ return failoverActivated; }
    public String getPrimaryRestored() { return primaryRestored; }
    public String getDiscordWebhook()  { return discordWebhook; }

    // ── Parsing helpers ──────────────────────────────────────────────────

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v != null ? String.valueOf(v) : def;
    }

    private static long lng(Map<String, Object> m, String k, long def) {
        Object o = m.get(k);
        if (o instanceof Number n) return n.longValue();
        if (o != null) {
            try { return Long.parseLong(o.toString().trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        if (v instanceof Boolean b) return b;
        if (v != null) return Boolean.parseBoolean(v.toString().trim());
        return def;
    }
}
