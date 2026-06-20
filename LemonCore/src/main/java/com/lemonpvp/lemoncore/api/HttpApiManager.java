package com.lemonpvp.lemoncore.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class HttpApiManager {

    private static final Gson GSON = new Gson();

    private final LemonCore plugin;
    private final Logger log;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public HttpApiManager(LemonCore plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public void start() {
        if (!plugin.getConfig().getBoolean("http-api.enabled", false)) {
            log.info("[HttpAPI] Disabled in config.");
            return;
        }

        int port = plugin.getConfig().getInt("http-api.port", 8080);
        String bindAddress = plugin.getConfig().getString("http-api.bind", "127.0.0.1");
        String apiKey = plugin.getConfig().getString("http-api.key", "");

        if (apiKey.isEmpty()) {
            log.warning("[HttpAPI] No API key configured! Set http-api.key in config.yml to enable the HTTP API.");
            return;
        }
        if ("change-me-in-production".equals(apiKey)) {
            log.warning("[HttpAPI] http-api.key is still the default value — change it before exposing the API.");
        }

        try {
            server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);
            server.createContext("/api/player/",     ex -> route(ex, e -> handlePlayerRoot(e, apiKey)));
            server.createContext("/api/server/stats", ex -> route(ex, e -> handleStats(e, apiKey)));
            server.createContext("/api/console",     ex -> route(ex, e -> handleConsole(e, apiKey)));
            server.start();
            log.info("[HttpAPI] Started on " + bindAddress + ":" + port);
        } catch (IOException e) {
            log.severe("[HttpAPI] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            log.info("[HttpAPI] Stopped.");
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    // ─── Request wrapper ──────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Route { void handle(HttpExchange ex) throws IOException; }

    /** Carries an HTTP status so handlers can fail cleanly instead of resetting the socket. */
    private static class ApiException extends IOException {
        final int status;
        ApiException(int status, String message) { super(message); this.status = status; }
    }

    /**
     * Wraps every handler so any thrown exception still produces a proper HTTP
     * response and the exchange is always closed. Without this, an exception
     * (e.g. a timeout) would leave the client with a dropped connection.
     */
    private void route(HttpExchange ex, Route handler) {
        try {
            handler.handle(ex);
        } catch (ApiException e) {
            trySend(ex, e.status, error(e.getMessage()));
        } catch (Exception e) {
            log.warning("[HttpAPI] Handler error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            trySend(ex, 500, error("Internal server error"));
        } finally {
            ex.close();
        }
    }

    private void trySend(HttpExchange ex, int code, String body) {
        try { send(ex, code, body); } catch (IOException ignored) {}
    }

    // ─── Auth ───────────────────────────────────────────────────────────────

    private boolean authenticate(HttpExchange ex, String apiKey) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null) return false;
        // Constant-time comparison to avoid leaking key bytes via response timing
        return java.security.MessageDigest.isEqual(
                auth.getBytes(StandardCharsets.UTF_8),
                ("Bearer " + apiKey).getBytes(StandardCharsets.UTF_8));
    }

    // ─── Route dispatcher ────────────────────────────────────────────────────

    private void handlePlayerRoot(HttpExchange ex, String key) throws IOException {
        if (!authenticate(ex, key)) { send(ex, 401, error("Unauthorized")); return; }

        String path = ex.getRequestURI().getPath();
        String[] parts = path.replaceAll("^/+|/+$", "").split("/");
        // parts: ["api","player",name] or ["api","player",name,action]
        if (parts.length < 3) { send(ex, 400, error("Missing player name")); return; }

        String playerName = parts[2];
        String action = parts.length >= 4 ? parts[3] : null;
        String method = ex.getRequestMethod().toUpperCase();

        if ("GET".equals(method) && action == null) {
            handleGetPlayer(ex, playerName);
        } else if ("POST".equals(method) && action != null) {
            String body = readBody(ex);
            switch (action) {
                case "ban"    -> handleBan(ex, playerName, body);
                case "unban"  -> handleUnban(ex, playerName);
                case "mute"   -> handleMute(ex, playerName, body);
                case "unmute" -> handleUnmute(ex, playerName);
                case "coins"  -> handleCoins(ex, playerName, body);
                case "rank"   -> handleRank(ex, playerName, body);
                default -> send(ex, 400, error("Unknown action: " + action));
            }
        } else {
            send(ex, 405, error("Method not allowed"));
        }
    }

    // ─── GET /api/player/{name} ─────────────────────────────────────────────

    private void handleGetPlayer(HttpExchange ex, String name) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }

        PlayerData data = plugin.getPlayerDataManager().getCached(uuid);
        boolean online = Bukkit.getPlayer(uuid) != null;

        JsonObject obj = new JsonObject();
        obj.addProperty("uuid",   uuid.toString());
        obj.addProperty("name",   name);
        obj.addProperty("online", online);
        obj.addProperty("coins",  data != null ? data.getCoins() : 0);

        JsonObject elo = new JsonObject();
        if (data != null) {
            for (Map.Entry<String, Integer> e : data.getElo().entrySet()) {
                elo.addProperty(e.getKey(), e.getValue());
            }
        }
        obj.add("elo", elo);

        // Rank
        String rank = "default";
        LuckPerms lp = plugin.getLuckPerms();
        if (lp != null) {
            try {
                User user = joinWithTimeout(lp.getUserManager().loadUser(uuid));
                if (user != null) rank = user.getPrimaryGroup();
            } catch (Exception ignored) {}
        }
        obj.addProperty("rank", rank);

        // Ban
        joinWithTimeout(plugin.getBanManager().getActiveBan(uuid).thenAccept(ban -> {
            obj.addProperty("banned", ban != null);
            if (ban != null) {
                obj.addProperty("ban_reason", ban.reason);
                if (ban.expires != null)
                    obj.addProperty("ban_expires", ban.expires.toString());
            }
        }));

        // Mute
        joinWithTimeout(plugin.getMuteManager().getActiveMute(uuid).thenAccept(mute -> {
            obj.addProperty("muted", mute != null);
            if (mute != null) {
                obj.addProperty("mute_reason", mute.reason);
                if (!mute.isPermanent() && mute.expires != null)
                    obj.addProperty("mute_expires", mute.expires.toString());
            }
        }));

        send(ex, 200, GSON.toJson(obj));
    }

    // ─── POST /api/player/{name}/ban ────────────────────────────────────────

    private void handleBan(HttpExchange ex, String name, String body) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }

        JsonObject parsed = parseJson(body);
        String reason   = getStr(parsed, "reason", "Staff Panel");
        String duration = getStr(parsed, "duration", "permanent");
        long durationSecs = parseDuration(duration);

        joinWithTimeout(plugin.getBanManager().banPlayer(uuid, name, reason, null, "StaffPanel", durationSecs)
            .thenAccept(ban -> {
                if (ban == null) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player online = Bukkit.getPlayer(uuid);
                    // Route through performBanKick so the proxy gets the PlayerBanning
                    // signal (no limbo evasion) and the kick screen matches messages.yml.
                    if (online != null) plugin.getListenerManager().performBanKick(online, ban);
                });
            }));

        send(ex, 200, ok());
    }

    // ─── POST /api/player/{name}/unban ──────────────────────────────────────

    private void handleUnban(HttpExchange ex, String name) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }
        joinWithTimeout(plugin.getBanManager().unban(name));
        send(ex, 200, ok());
    }

    // ─── POST /api/player/{name}/mute ───────────────────────────────────────

    private void handleMute(HttpExchange ex, String name, String body) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }

        JsonObject parsed = parseJson(body);
        String reason   = getStr(parsed, "reason", "Staff Panel");
        String duration = getStr(parsed, "duration", "permanent");
        long durationSecs = parseDuration(duration);

        joinWithTimeout(plugin.getMuteManager().mutePlayer(uuid, name, reason, null, "StaffPanel", durationSecs)
            .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null)
                    online.sendMessage(net.kyori.adventure.text.Component.text("Du wurdest stummgeschaltet: " + reason));
            })));

        send(ex, 200, ok());
    }

    // ─── POST /api/player/{name}/unmute ─────────────────────────────────────

    private void handleUnmute(HttpExchange ex, String name) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }
        joinWithTimeout(plugin.getMuteManager().unmute(name));
        send(ex, 200, ok());
    }

    // ─── POST /api/player/{name}/coins ──────────────────────────────────────

    private void handleCoins(HttpExchange ex, String name, String body) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }

        JsonObject parsed = parseJson(body);
        if (parsed == null) { send(ex, 400, error("Invalid JSON")); return; }
        long amount = getLong(parsed, "amount", 0L);
        String action = getStr(parsed, "action", "add");

        switch (action) {
            case "add"    -> joinWithTimeout(plugin.getPlayerDataManager().addCoins(uuid, amount, "staff-panel", null));
            case "remove" -> joinWithTimeout(plugin.getPlayerDataManager().removeCoins(uuid, amount, "staff-panel", null));
            case "set"    -> joinWithTimeout(plugin.getPlayerDataManager().setCoins(uuid, amount, null));
            default       -> { send(ex, 400, error("Unknown action: " + action)); return; }
        }

        send(ex, 200, ok());
    }

    // ─── POST /api/player/{name}/rank ───────────────────────────────────────

    private void handleRank(HttpExchange ex, String name, String body) throws IOException {
        LuckPerms lp = plugin.getLuckPerms();
        if (lp == null) { send(ex, 503, error("LuckPerms not available")); return; }

        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }

        JsonObject parsed = parseJson(body);
        if (parsed == null) { send(ex, 400, error("Invalid JSON")); return; }
        String rank = getStr(parsed, "rank", null);
        if (rank == null) { send(ex, 400, error("rank required")); return; }

        try {
            User user = joinWithTimeout(lp.getUserManager().loadUser(uuid));
            if (user == null) { send(ex, 404, error("LuckPerms user not found")); return; }
            user.data().clear(node -> node instanceof InheritanceNode);
            user.data().add(InheritanceNode.builder(rank).build());
            joinWithTimeout(lp.getUserManager().saveUser(user));
            send(ex, 200, ok());
        } catch (Exception e) {
            send(ex, 500, error("LuckPerms error: " + e.getMessage()));
        }
    }

    // ─── GET /api/server/stats ───────────────────────────────────────────────

    private void handleStats(HttpExchange ex, String key) throws IOException {
        if (!authenticate(ex, key)) { send(ex, 401, error("Unauthorized")); return; }
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, error("Method not allowed")); return; }

        long uptime = (System.currentTimeMillis() - plugin.getStartTimeMs()) / 1000L;
        String version = Bukkit.getVersion();

        // Bukkit.getTPS() / getOnlinePlayers() / getMaxPlayers() are main-thread-only
        CompletableFuture<JsonObject> mainFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                double[] tps = Bukkit.getTPS();
                JsonObject obj = new JsonObject();
                obj.addProperty("online",         Bukkit.getOnlinePlayers().size());
                obj.addProperty("max",            Bukkit.getMaxPlayers());
                obj.addProperty("tps",            Math.round(tps[0] * 10.0) / 10.0);
                obj.addProperty("uptime_seconds", uptime);
                obj.addProperty("version",        version);
                mainFuture.complete(obj);
            } catch (Exception e) {
                mainFuture.completeExceptionally(e);
            }
        });

        send(ex, 200, GSON.toJson(joinWithTimeout(mainFuture)));
    }

    // ─── POST /api/console ───────────────────────────────────────────────────

    private void handleConsole(HttpExchange ex, String key) throws IOException {
        if (!authenticate(ex, key)) { send(ex, 401, error("Unauthorized")); return; }
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, error("Method not allowed")); return; }

        JsonObject parsed = parseJson(readBody(ex));
        if (parsed == null) { send(ex, 400, error("Invalid JSON")); return; }
        String command = getStr(parsed, "command", null);
        if (command == null || command.isBlank()) { send(ex, 400, error("command required")); return; }

        StringBuilder output = new StringBuilder();
        java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord r) { output.append(r.getMessage()).append('\n'); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        Bukkit.getLogger().addHandler(handler);
        try {
            CompletableFuture<Void> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } finally {
                    future.complete(null);
                }
            });
            try { future.get(5, TimeUnit.SECONDS); } catch (Exception ignored) {}
        } finally {
            Bukkit.getLogger().removeHandler(handler);
        }

        JsonObject result = new JsonObject();
        result.addProperty("output", output.toString().trim());
        send(ex, 200, GSON.toJson(result));
    }

    // ─── Utility ────────────────────────────────────────────────────────────

    private UUID getUuidByName(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p.getUniqueId();
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        return op != null ? op.getUniqueId() : null;
    }

    private static final int MAX_BODY_BYTES = 8192;
    private static final long FUTURE_TIMEOUT_SECS = 5;

    private <T> T joinWithTimeout(CompletableFuture<T> future) throws IOException {
        try {
            return future.get(FUTURE_TIMEOUT_SECS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ApiException(504, "Operation timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(503, "Operation interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.warning("[HttpAPI] Async operation failed: "
                    + (cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : e.getMessage()));
            throw new ApiException(500, "Operation failed");
        }
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            byte[] buf = is.readNBytes(MAX_BODY_BYTES + 1);
            if (buf.length > MAX_BODY_BYTES) throw new ApiException(413, "Request body too large");
            return new String(buf, StandardCharsets.UTF_8);
        }
    }

    private JsonObject parseJson(String body) {
        if (body == null || body.isBlank()) return new JsonObject();
        try { return JsonParser.parseString(body).getAsJsonObject(); }
        catch (Exception e) { return null; }
    }

    private String getStr(JsonObject obj, String key, String def) {
        if (obj == null || !obj.has(key)) return def;
        JsonElement el = obj.get(key);
        return el.isJsonNull() ? def : el.getAsString();
    }

    private long getLong(JsonObject obj, String key, long def) {
        if (obj == null || !obj.has(key)) return def;
        try { return obj.get(key).getAsLong(); }
        catch (Exception e) { return def; }
    }

    private long parseDuration(String d) {
        if (d == null || d.equalsIgnoreCase("permanent")) return -1L;
        try {
            if (d.endsWith("d")) return Long.parseLong(d.replace("d","")) * 86400L;
            if (d.endsWith("h")) return Long.parseLong(d.replace("h","")) * 3600L;
            if (d.endsWith("m")) return Long.parseLong(d.replace("m","")) * 60L;
            return Long.parseLong(d);
        } catch (NumberFormatException e) { return -1L; }
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private String ok() {
        JsonObject o = new JsonObject(); o.addProperty("ok", true); return GSON.toJson(o);
    }

    private String error(String msg) {
        JsonObject o = new JsonObject(); o.addProperty("error", msg); return GSON.toJson(o);
    }
}
