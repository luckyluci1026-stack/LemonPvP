package com.lemonpvp.lemoncore.discord;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.BanRecord;
import com.lemonpvp.lemoncore.managers.MuteRecord;
import com.lemonpvp.lemoncore.util.TextUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordWebhookManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final LemonCore plugin;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    public DiscordWebhookManager(LemonCore plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LemonCore-Webhook");
            t.setDaemon(true);
            return t;
        });
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    // -------------------------------------------------------------------------
    // Public send methods
    // -------------------------------------------------------------------------

    public void sendBan(BanRecord ban) {
        String url = getUrl("bans");
        if (url == null) return;

        String duration = ban.isPermanent() ? "Permanent" : TextUtil.formatDuration(ban.getRemainingSeconds());
        String by = ban.bannerName != null ? ban.bannerName : "Unknown";
        String description = ban.username + " has been banned.\n"
                + "• Banned by: " + by + "\n"
                + "• Duration: " + duration + "\n"
                + "• Reason: " + ban.reason + "\n"
                + "• Ban ID: " + ban.id;

        sendWithRetry(url, buildEmbed("Banned", description, 0xFF0000));
    }

    public void sendMute(MuteRecord mute) {
        String url = getUrl("mutes");
        if (url == null) return;

        String duration = mute.isPermanent() ? "Permanent" : TextUtil.formatDuration(mute.getRemainingSeconds());
        String by = mute.muterName != null ? mute.muterName : "Unknown";
        String description = mute.username + " has been muted.\n"
                + "• Muted by: " + by + "\n"
                + "• Duration: " + duration + "\n"
                + "• Reason: " + mute.reason;

        sendWithRetry(url, buildEmbed("Muted", description, 0xFFA500));
    }

    public void sendUnban(String playerName, String adminName, String banId) {
        String url = getUrl("unbans");
        if (url == null) return;

        String description = playerName + " has been unbanned.\n"
                + "• Unbanned by: " + adminName + "\n"
                + "• Original Ban ID: " + banId;

        sendWithRetry(url, buildEmbed("Unbanned", description, 0x00FF00));
    }

    public void sendUnmute(String playerName, String adminName) {
        String url = getUrl("unmutes");
        if (url == null) return;

        String description = playerName + " has been unmuted.\n"
                + "• Unmuted by: " + adminName;

        sendWithRetry(url, buildEmbed("Unmuted", description, 0x00FF00));
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private String getUrl(String key) {
        String url = plugin.getConfig().getString("discord_webhooks." + key, "");
        if (url == null || url.isBlank() || url.contains("/...")) return null;
        return url;
    }

    private String buildEmbed(String title, String description, int color) {
        String footer = "LemonPvP • " + LocalDateTime.now().format(DATE_FMT);
        return "{\"embeds\":[{"
                + "\"title\":" + esc(title) + ","
                + "\"description\":" + esc(description) + ","
                + "\"color\":" + color + ","
                + "\"footer\":{\"text\":" + esc(footer) + "}"
                + "}]}";
    }

    private static String esc(String value) {
        if (value == null) return "\"\"";
        return "\""
                + value.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t")
                + "\"";
    }

    private void sendWithRetry(String url, String json) {
        post(url, json).thenAccept(ok -> {
            if (!ok) {
                scheduler.schedule(
                        () -> post(url, json).thenAccept(retryOk -> {
                            if (!retryOk) {
                                plugin.getLogger().warning(
                                        "[Discord] Webhook failed after retry: " + url);
                            }
                        }),
                        3, TimeUnit.SECONDS);
            }
        });
    }

    private CompletableFuture<Boolean> post(String url, String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(resp -> {
                    int status = resp.statusCode();
                    if (status < 200 || status >= 300) {
                        plugin.getLogger().warning(
                                "[Discord] Webhook returned HTTP " + status);
                        return false;
                    }
                    return true;
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning(
                            "[Discord] Webhook request failed: " + ex.getMessage());
                    return false;
                });
    }
}
