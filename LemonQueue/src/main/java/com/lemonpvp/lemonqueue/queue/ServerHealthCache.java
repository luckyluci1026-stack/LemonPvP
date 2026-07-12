package com.lemonpvp.lemonqueue.queue;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Async health monitor for registered Velocity backend servers.
 *
 * <p>Pings each watched server on the Velocity scheduler (never blocks the
 * event thread). Results are stored in a volatile map so event handlers can
 * check online state without blocking.
 *
 * <p>Adaptive intervals: known-online servers are pinged every
 * {@value #ONLINE_PING_S} seconds; known-offline servers only every
 * {@value #OFFLINE_PING_S} seconds so that repeated failures do not flood
 * Velocity's internal network layer (and its logs) while a backend is down.
 *
 * <p>Log spam is suppressed: a state change (online→offline or offline→online)
 * is logged exactly once; repeated failures in the same offline window are silent.
 */
public final class ServerHealthCache {

    /** Ping interval while a server is online (or state unknown). */
    private static final int ONLINE_PING_S  = 5;
    /** Reduced ping interval while a server is known offline. */
    private static final int OFFLINE_PING_S = 30;

    private final ProxyServer proxy;
    private final Logger logger;
    private final String[] watched;

    /** true = online, false = offline, absent = unknown (not yet pinged). */
    private final Map<String, Boolean> status     = new ConcurrentHashMap<>();
    /** Epoch-ms at which the next ping for each server is due. */
    private final Map<String, Long>    nextPingAt = new ConcurrentHashMap<>();

    private ScheduledTask task;

    public ServerHealthCache(ProxyServer proxy, Logger logger, String... watched) {
        this.proxy   = proxy;
        this.logger  = logger;
        this.watched = watched;
    }

    /**
     * Starts the repeating ping task. The first execution is scheduled
     * immediately (delay = 0) so the cache is populated before the first
     * player join event arrives — all on the Velocity scheduler, never
     * blocking the calling thread.
     */
    public void start(Object plugin) {
        task = proxy.getScheduler()
                .buildTask(plugin, this::pingAll)
                .delay(0, TimeUnit.MILLISECONDS)
                .repeat(ONLINE_PING_S, TimeUnit.SECONDS)
                .schedule();
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    /**
     * Returns {@code true} if the server was online at the last ping.
     * Returns {@code false} (pessimistic) when the server has not been
     * pinged yet — the very first scheduled ping fires with delay=0 so
     * this window is negligibly short.
     */
    public boolean isOnline(String serverName) {
        return Boolean.TRUE.equals(status.get(serverName));
    }

    /**
     * Returns {@code true} only when the last ping EXPLICITLY failed. Unknown
     * servers (never pinged — e.g. queue targets outside the watched set)
     * return {@code false}, so callers treat them as reachable instead of
     * skipping their queues forever.
     */
    public boolean isKnownOffline(String serverName) {
        return Boolean.FALSE.equals(status.get(serverName));
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void pingAll() {
        long now = System.currentTimeMillis();
        for (String name : watched) {
            // Skip if the next allowed ping for this server is still in the future.
            if (now < nextPingAt.getOrDefault(name, 0L)) continue;

            Optional<RegisteredServer> opt = proxy.getServer(name);
            if (opt.isEmpty()) {
                markOffline(name);
                continue;
            }
            RegisteredServer server = opt.get();
            server.ping().whenComplete((result, err) -> {
                if (err != null) {
                    markOffline(name);
                } else {
                    markOnline(name);
                }
            });
        }
    }

    private void markOnline(String name) {
        Boolean prev = status.put(name, true);
        nextPingAt.put(name, System.currentTimeMillis() + ONLINE_PING_S * 1_000L);
        if (Boolean.FALSE.equals(prev)) {
            logger.info("[LemonQueue] Server '{}' ist wieder erreichbar.", name);
        }
    }

    private void markOffline(String name) {
        Boolean prev = status.put(name, false);
        // Back off heavily when the server stays offline to avoid hammering
        // Velocity's network stack and its internal connection-failure logs.
        nextPingAt.put(name, System.currentTimeMillis() + OFFLINE_PING_S * 1_000L);
        if (!Boolean.FALSE.equals(prev)) {
            logger.warn("[LemonQueue] Server '{}' ist nicht erreichbar — Spieler werden umgeleitet.", name);
        }
    }
}
