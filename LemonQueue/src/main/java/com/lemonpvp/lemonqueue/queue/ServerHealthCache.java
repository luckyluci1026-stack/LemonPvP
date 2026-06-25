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
 * <p>Pings each watched server every {@value #PING_INTERVAL_S} seconds on the
 * Velocity scheduler (never blocks the event thread). Results are stored in a
 * volatile map so event handlers can check online state without blocking.
 *
 * <p>Log spam is suppressed: a state change (online→offline or offline→online)
 * is logged exactly once; repeated failures in the same offline window are silent.
 */
public final class ServerHealthCache {

    private static final int PING_INTERVAL_S = 10;

    private final ProxyServer proxy;
    private final Logger logger;
    private final String[] watched;

    /** true = online, false = offline, absent = unknown (not yet pinged). */
    private final Map<String, Boolean> status = new ConcurrentHashMap<>();

    private ScheduledTask task;

    public ServerHealthCache(ProxyServer proxy, Logger logger, String... watched) {
        this.proxy = proxy;
        this.logger = logger;
        this.watched = watched;
    }

    public void start(Object plugin) {
        task = proxy.getScheduler()
                .buildTask(plugin, this::pingAll)
                .repeat(PING_INTERVAL_S, TimeUnit.SECONDS)
                .schedule();
        // Run immediately so the first join decision has fresh data.
        pingAll();
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    /**
     * Returns {@code true} if the server was online at the last ping.
     * Returns {@code true} (optimistic) if the server has never been pinged yet,
     * so that the very first join attempt is not blocked for no reason.
     */
    public boolean isOnline(String serverName) {
        return status.getOrDefault(serverName, true);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void pingAll() {
        for (String name : watched) {
            Optional<RegisteredServer> opt = proxy.getServer(name);
            if (opt.isEmpty()) {
                // Not registered in velocity.toml — treat as always offline.
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
        if (Boolean.FALSE.equals(prev)) {
            logger.info("[LemonQueue] Server '{}' ist wieder erreichbar.", name);
        }
    }

    private void markOffline(String name) {
        Boolean prev = status.put(name, false);
        if (!Boolean.FALSE.equals(prev)) {
            // Only log the first time we detect the server going down.
            logger.warn("[LemonQueue] Server '{}' ist nicht erreichbar — Spieler werden umgeleitet.", name);
        }
    }
}
