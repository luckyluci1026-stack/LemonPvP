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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HttpApiManager {

    private static final Gson GSON = new Gson();

    private final LemonCore plugin;
    private final Logger log;
    private HttpServer server;

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
        String apiKey = plugin.getConfig().getString("http-api.key", "");

        if (apiKey.isEmpty()) {
            log.warning("[HttpAPI] No API key configured! Set http-api.key in config.yml to enable the HTTP API.");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.createContext("/api/player/",     ex -> handlePlayerRoot(ex, apiKey));
            server.createContext("/api/server/stats", ex -> handleStats(ex, apiKey));
            server.createContext("/api/console",     ex -> handleConsole(ex, apiKey));
            server.start();
            log.info("[HttpAPI] Started on port " + port);
        } catch (IOException e) {
            log.severe("[HttpAPI] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            log.info("[HttpAPI] Stopped.");
        }
    }

    // ─── Auth ───────────────────────────────────────────────────────────────

    private boolean authenticate(HttpExchange ex, String apiKey) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        return auth != null && auth.equals("Bearer " + apiKey);
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
                case "ban"   -> handleBan(ex, playerName, body);
                case "unban" -> handleUnban(ex, playerName);
                case "coins" -> handleCoins(ex, playerName, body);
                case "rank"  -> handleRank(ex, playerName, body);
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
                User user = lp.getUserManager().loadUser(uuid).join();
                if (user != null) rank = user.getPrimaryGroup();
            } catch (Exception ignored) {}
        }
        obj.addProperty("rank", rank);

        // Ban
        plugin.getBanManager().getActiveBan(uuid).thenAccept(ban -> {
            obj.addProperty("banned", ban != null);
            if (ban != null) {
                obj.addProperty("ban_reason", ban.getReason());
                if (ban.getExpiresAt() != null)
                    obj.addProperty("ban_expires", ban.getExpiresAt().toString());
            }
        }).join();

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

        plugin.getBanManager().banPlayer(uuid, name, reason, null, "StaffPanel", durationSecs)
            .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null)
                    online.kick(net.kyori.adventure.text.Component.text("Du wurdest gebannt: " + reason));
            })).join();

        send(ex, 200, ok());
    }

    // ─── POST /api/player/{name}/unban ──────────────────────────────────────

    private void handleUnban(HttpExchange ex, String name) throws IOException {
        UUID uuid = getUuidByName(name);
        if (uuid == null) { send(ex, 404, error("Player not found: " + name)); return; }
        plugin.getBanManager().unban(name).join();
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

        plugin.getPlayerDataManager().loadPlayer(uuid, name).thenAccept(data -> {
            switch (action) {
                case "add"    -> plugin.getPlayerDataManager().addCoins(uuid, amount, "staff-panel", null);
                case "remove" -> plugin.getPlayerDataManager().removeCoins(uuid, amount, "staff-panel", null);
                case "set"    -> {
                    long current = data.getCoins();
                    long diff = amount - current;
                    if (diff > 0) plugin.getPlayerDataManager().addCoins(uuid, diff, "staff-panel", null);
                    else if (diff < 0) plugin.getPlayerDataManager().removeCoins(uuid, -diff, "staff-panel", null);
                }
            }
        }).join();

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
            User user = lp.getUserManager().loadUser(uuid).join();
            if (user == null) { send(ex, 404, error("LuckPerms user not found")); return; }
            user.data().clear(node -> node instanceof InheritanceNode);
            user.data().add(InheritanceNode.builder(rank).build());
            lp.getUserManager().saveUser(user).join();
            send(ex, 200, ok());
        } catch (Exception e) {
            send(ex, 500, error("LuckPerms error: " + e.getMessage()));
        }
    }

    // ─── GET /api/server/stats ───────────────────────────────────────────────

    private void handleStats(HttpExchange ex, String key) throws IOException {
        if (!authenticate(ex, key)) { send(ex, 401, error("Unauthorized")); return; }
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, error("Method not allowed")); return; }

        double[] tps = Bukkit.getTPS();
        long uptime = (System.currentTimeMillis() - plugin.getStartTimeMs()) / 1000L;

        JsonObject obj = new JsonObject();
        obj.addProperty("online",         Bukkit.getOnlinePlayers().size());
        obj.addProperty("max",            Bukkit.getMaxPlayers());
        obj.addProperty("tps",            Math.round(tps[0] * 10.0) / 10.0);
        obj.addProperty("uptime_seconds", uptime);
        obj.addProperty("version",        Bukkit.getVersion());

        send(ex, 200, GSON.toJson(obj));
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

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
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
