package com.lemonpvp.lemoncore.discord;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.BanRecord;
import com.lemonpvp.lemoncore.managers.MuteRecord;
import com.lemonpvp.lemoncore.util.TextUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordWebhookManager {

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        String adminUrl  = getUrl("bans");
        String publicUrl = getUrl("public");
        if (adminUrl == null && publicUrl == null) return;

        // Public webhook does NOT need the Discord username — send immediately.
        if (publicUrl != null) {
            String duration = ban.isPermanent() ? "Permanent" : "Temporary";
            String desc = "**" + ban.username + "** has been banned.\n"
                    + "• Reason: " + ban.reason + "\n"
                    + "• Duration: " + duration;
            sendWithRetry(publicUrl, buildPublicEmbed("🔨 Player Banned", desc, 0xFF0000));
        }

        // Admin webhook optionally includes Discord username.
        // .exceptionally(ex -> null) ensures the chain always completes even if the
        // Discord-link lookup fails, so the webhook is never silently dropped.
        if (adminUrl != null) {
            final String url = adminUrl;
            plugin.getDiscordLinkManager()
                    .getDiscordUsername(ban.uuid)
                    .exceptionally(ex -> null)
                    .thenAccept(discordUser -> {
                        String duration = ban.isPermanent()
                                ? "Permanent"
                                : TextUtil.formatDuration(ban.getRemainingSeconds());
                        String by = ban.bannerName != null ? ban.bannerName : "Unknown";
                        StringBuilder desc = new StringBuilder()
                                .append(ban.username).append(" has been banned.\n")
                                .append("• Banned by: ").append(by).append("\n")
                                .append("• Duration: ").append(duration).append("\n")
                                .append("• Reason: ").append(ban.reason).append("\n")
                                .append("• Ban ID: ").append(ban.id);
                        if (discordUser != null) desc.append("\n• Discord: ").append(discordUser);
                        sendWithRetry(url, buildEmbed("🔨 Banned", desc.toString(), 0xFF0000));
                    });
        }
    }

    public void sendMute(MuteRecord mute) {
        String adminUrl  = getUrl("mutes");
        String publicUrl = getUrl("public");
        if (adminUrl == null && publicUrl == null) return;

        // Public webhook — send immediately, no Discord lookup needed.
        if (publicUrl != null) {
            String duration = mute.isPermanent() ? "Permanent" : "Temporary";
            String desc = "**" + mute.username + "** has been muted.\n"
                    + "• Reason: " + mute.reason + "\n"
                    + "• Duration: " + duration;
            sendWithRetry(publicUrl, buildPublicEmbed("🔇 Player Muted", desc, 0xFFA500));
        }

        // Admin webhook — Discord username is optional.
        if (adminUrl != null) {
            final String url = adminUrl;
            plugin.getDiscordLinkManager()
                    .getDiscordUsername(mute.uuid)
                    .exceptionally(ex -> null)
                    .thenAccept(discordUser -> {
                        String duration = mute.isPermanent()
                                ? "Permanent"
                                : TextUtil.formatDuration(mute.getRemainingSeconds());
                        String by = mute.muterName != null ? mute.muterName : "Unknown";
                        StringBuilder desc = new StringBuilder()
                                .append(mute.username).append(" has been muted.\n")
                                .append("• Muted by: ").append(by).append("\n")
                                .append("• Duration: ").append(duration).append("\n")
                                .append("• Reason: ").append(mute.reason);
                        if (discordUser != null) desc.append("\n• Discord: ").append(discordUser);
                        sendWithRetry(url, buildEmbed("🔇 Muted", desc.toString(), 0xFFA500));
                    });
        }
    }

    public void sendUnban(String playerName, String adminName, String banId) {
        String url = getUrl("unbans");
        if (url == null) return;

        String description = playerName + " has been unbanned.\n"
                + "• Unbanned by: " + adminName + "\n"
                + "• Original Ban ID: " + banId;

        sendWithRetry(url, buildEmbed("✅ Unbanned", description, 0x00FF00));
    }

    public void sendUnmute(String playerName, String adminName) {
        String url = getUrl("unmutes");
        if (url == null) return;

        String description = playerName + " has been unmuted.\n"
                + "• Unmuted by: " + adminName;

        sendWithRetry(url, buildEmbed("✅ Unmuted", description, 0x00FF00));
    }

    /**
     * A positive gameplay moment worth celebrating publicly — a tournament
     * champion, a win-streak milestone, and similar. No Discord-link lookup
     * (unlike the moderation webhooks), since these never need to name staff.
     * No-op if {@code discord_webhooks.highlights} isn't configured.
     */
    public void sendHighlight(String title, String description) {
        String url = getUrl("highlights");
        if (url == null) return;
        sendWithRetry(url, buildPublicEmbed(title, description, 0xFACC15));
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
                + "\"title\":"       + esc(title)       + ","
                + "\"description\":" + esc(description)  + ","
                + "\"color\":"       + color             + ","
                + "\"footer\":{\"text\":" + esc(footer)  + "}"
                + "}]}";
    }

    private String buildPublicEmbed(String title, String description, int color) {
        String footer = "LemonPvP • " + LocalDate.now().format(DATE_ONLY_FMT);
        return "{\"embeds\":[{"
                + "\"title\":"       + esc(title)       + ","
                + "\"description\":" + esc(description)  + ","
                + "\"color\":"       + color             + ","
                + "\"footer\":{\"text\":" + esc(footer)  + "}"
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

        // Use ofString() so Discord's error body is visible in warnings.
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    int status = resp.statusCode();
                    if (status < 200 || status >= 300) {
                        plugin.getLogger().warning(
                                "[Discord] Webhook returned HTTP " + status
                                + " — " + resp.body());
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
