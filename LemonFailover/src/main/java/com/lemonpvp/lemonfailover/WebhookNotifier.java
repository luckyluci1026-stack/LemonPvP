package com.lemonpvp.lemonfailover;

import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fire-and-forget Discord webhook poster for failover alerts. Uses the JDK
 * {@link HttpClient} (no extra dependencies). Silently no-ops when no webhook
 * URL is configured, and never throws into the caller.
 */
public final class WebhookNotifier {

    private final Logger logger;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WebhookNotifier(Logger logger) {
        this.logger = logger;
    }

    /** Posts {@code message} as a Discord webhook (async). No-op if url is blank. */
    public void send(String url, String message) {
        if (url == null || url.isBlank()) return;
        try {
            String body = "{\"content\":\"" + escape(message) + "\"}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        logger.warn("[LemonFailover] Discord webhook failed: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.warn("[LemonFailover] Discord webhook error: {}", e.getMessage());
        }
    }

    /** Minimal JSON string escaping for the webhook payload. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
