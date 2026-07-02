package com.lemonpvp.lemonfailover;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Monitors the primary proxy via periodic TCP connect checks and drives the
 * {@link FailoverState} state machine with hysteresis:
 * <ul>
 *   <li>{@code failure-threshold} consecutive failed checks → {@link FailoverState#ACTIVE}</li>
 *   <li>{@code recovery-threshold} consecutive successful checks → {@link FailoverState#STANDBY}</li>
 * </ul>
 *
 * <p>State transitions log, message online players and (optionally) post a
 * Discord webhook. An admin can pin the state with {@code /failover active|standby};
 * {@code /failover auto} hands control back to the monitor.</p>
 *
 * <p>Threading: the check runs on Velocity's async scheduler (a blocking socket
 * connect there is fine). {@code state} and {@code manualOverride} are volatile
 * so event handlers read them consistently; the counters are only touched on the
 * scheduler thread.</p>
 */
public final class PrimaryMonitor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonFailover plugin;
    private final ProxyServer proxy;
    private final Logger logger;

    private volatile FailoverState state = FailoverState.STANDBY;
    private volatile boolean manualOverride = false;
    private volatile boolean lastReachable = false;

    private int consecutiveFailures = 0;
    private int consecutiveSuccesses = 0;

    private ScheduledTask task;

    public PrimaryMonitor(LemonFailover plugin) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
        this.logger = plugin.getLogger();
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    public void start() {
        switch (plugin.getConfig().getStartMode()) {
            case "active" -> state = FailoverState.ACTIVE;
            case "auto"   -> { /* stay STANDBY; the monitor converges quickly */ }
            default        -> state = FailoverState.STANDBY;
        }
        schedule();
        logger.info("[LemonFailover] Monitoring primary {}:{} — start state {}.",
                plugin.getConfig().getPrimaryHost(), plugin.getConfig().getPrimaryPort(), state);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    /** Re-schedules with the (possibly changed) interval after a config reload. */
    public void reschedule() {
        if (task != null) task.cancel();
        schedule();
    }

    private void schedule() {
        task = proxy.getScheduler().buildTask(plugin, this::tick)
                .delay(0, TimeUnit.MILLISECONDS)
                .repeat(plugin.getConfig().getCheckIntervalMs(), TimeUnit.MILLISECONDS)
                .schedule();
    }

    // ── Check loop ───────────────────────────────────────────────────────────

    private void tick() {
        FailoverConfig cfg = plugin.getConfig();
        boolean reachable = probe(cfg);
        lastReachable = reachable;

        if (reachable) {
            consecutiveSuccesses++;
            consecutiveFailures = 0;
            if (!manualOverride && state == FailoverState.ACTIVE
                    && consecutiveSuccesses >= cfg.getRecoveryThreshold()) {
                demoteToStandby(false);
            }
        } else {
            consecutiveFailures++;
            consecutiveSuccesses = 0;
            if (!manualOverride && state == FailoverState.STANDBY
                    && consecutiveFailures >= cfg.getFailureThreshold()) {
                promoteToActive(false);
            }
        }
    }

    private boolean probe(FailoverConfig cfg) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(cfg.getPrimaryHost(), cfg.getPrimaryPort()),
                    cfg.getConnectTimeoutMs());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Transitions ──────────────────────────────────────────────────────────

    private void promoteToActive(boolean manual) {
        if (state == FailoverState.ACTIVE) return;
        state = FailoverState.ACTIVE;
        consecutiveFailures = 0;
        consecutiveSuccesses = 0;
        FailoverConfig cfg = plugin.getConfig();
        logger.warn("[LemonFailover] Primary {}:{} unreachable — switching to ACTIVE (failover){}.",
                cfg.getPrimaryHost(), cfg.getPrimaryPort(), manual ? " [manual]" : "");
        broadcast(cfg.getFailoverActivated());
        plugin.getWebhook().send(cfg.getDiscordWebhook(),
                ":rotating_light: **Failover activated** — the primary proxy is unreachable. "
                        + "The backup proxy is now serving players.");
    }

    private void demoteToStandby(boolean manual) {
        if (state == FailoverState.STANDBY) return;
        state = FailoverState.STANDBY;
        consecutiveFailures = 0;
        consecutiveSuccesses = 0;
        FailoverConfig cfg = plugin.getConfig();
        logger.info("[LemonFailover] Primary reachable again — returning to STANDBY{}.",
                manual ? " [manual]" : "");
        broadcast(cfg.getPrimaryRestored());
        plugin.getWebhook().send(cfg.getDiscordWebhook(),
                ":white_check_mark: **Primary restored** — the backup proxy returned to standby.");

        if (cfg.isKickOnRecovery()) {
            Component reason = MM.deserialize(cfg.getKickedOnRecovery());
            for (Player p : proxy.getAllPlayers()) {
                if (p.hasPermission(cfg.getBypassPermission())) continue;
                p.disconnect(reason);
            }
        }
    }

    private void broadcast(String miniMessage) {
        if (miniMessage == null || miniMessage.isBlank()) return;
        Component msg = MM.deserialize(miniMessage);
        for (Player p : proxy.getAllPlayers()) p.sendMessage(msg);
    }

    // ── Admin control ────────────────────────────────────────────────────────

    /** Pins ACTIVE until {@link #resumeAuto()}. */
    public void forceActive() {
        manualOverride = true;
        promoteToActive(true);
    }

    /** Pins STANDBY until {@link #resumeAuto()}. */
    public void forceStandby() {
        manualOverride = true;
        demoteToStandby(true);
    }

    /** Returns control to the automatic monitor. */
    public void resumeAuto() {
        manualOverride = false;
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public FailoverState getState()     { return state; }
    public boolean isManualOverride()   { return manualOverride; }
    public boolean isPrimaryReachable() { return lastReachable; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public int getConsecutiveSuccesses(){ return consecutiveSuccesses; }
}
