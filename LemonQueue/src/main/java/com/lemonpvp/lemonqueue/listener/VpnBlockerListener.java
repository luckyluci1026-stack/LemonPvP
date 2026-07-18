package com.lemonpvp.lemonqueue.listener;

import com.lemonpvp.lemonqueue.LemonQueue;
import com.lemonpvp.lemonqueue.config.QueueConfig;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks VPN/proxy/hosting IPs at the proxy handshake, before a player ever
 * reaches a backend. Lookups go to the free ip-api.com endpoint (proxy+hosting
 * flags), results are cached for hours per IP, and everything fails OPEN — if
 * the API is down or slow, players are let in rather than locked out. Private
 * and whitelisted addresses are never checked.
 */
public class VpnBlockerListener {

    private static final long CACHE_TTL_MS = 6 * 60 * 60 * 1000L; // 6h
    private static final Duration LOOKUP_TIMEOUT = Duration.ofMillis(1500);

    private record CacheEntry(boolean blocked, long expiresAt) {}

    private final LemonQueue plugin;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(LOOKUP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public VpnBlockerListener(LemonQueue plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        QueueConfig cfg = plugin.getConfig();
        if (!cfg.isVpnBlockerEnabled()) return null;

        InetAddress addr = event.getConnection().getRemoteAddress().getAddress();
        String ip = addr.getHostAddress();
        if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) return null;
        if (cfg.getVpnWhitelist().contains(ip)) return null;

        CacheEntry cached = cache.get(ip);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            if (cached.blocked()) deny(event, cfg);
            return null;
        }

        // Fresh lookup off the netty thread; the event waits for the task.
        return EventTask.async(() -> {
            boolean blocked = lookup(ip, cfg.isVpnBlockHosting());
            cache.put(ip, new CacheEntry(blocked, System.currentTimeMillis() + CACHE_TTL_MS));
            if (blocked) deny(event, cfg);
        });
    }

    private void deny(PreLoginEvent event, QueueConfig cfg) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                MiniMessage.miniMessage().deserialize(cfg.getVpnKickMessage())));
    }

    /**
     * ip-api.com free endpoint (HTTP only on the free tier, 45 req/min — the
     * cache keeps us far under that). Naive JSON field scan avoids a JSON dep.
     */
    private boolean lookup(String ip, boolean blockHosting) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,proxy,hosting"))
                    .timeout(LOOKUP_TIMEOUT)
                    .GET().build();
            String body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
            if (body == null || !body.contains("\"status\":\"success\"")) return false; // fail open
            boolean proxy = body.contains("\"proxy\":true");
            boolean hosting = body.contains("\"hosting\":true");
            return proxy || (blockHosting && hosting);
        } catch (Exception e) {
            return false; // fail open — never lock players out because an API is down
        }
    }
}
