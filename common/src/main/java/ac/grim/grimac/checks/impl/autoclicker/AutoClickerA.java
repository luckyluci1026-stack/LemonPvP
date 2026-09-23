/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;

/**
 * BuckSMPAC custom AutoClicker check.
 *
 * <p>Measures how fast and how regularly a player <i>attacks</i>, and flags a
 * rate no hand can produce or a rhythm no hand can hold.</p>
 *
 * <h2>Why it counts attacks and not arm swings</h2>
 *
 * <p>An arm swing on its own is ambiguous: mining swings the arm once per client
 * tick for as long as the button is held, and clicking at the air is both
 * harmless and extremely common during warmup. Counting raw swings therefore
 * forces an "is the player mining right now?" state machine, and any such state
 * machine is itself a bypass — a client that starts a dig and never finishes it
 * looks like it is mining for the rest of the session, and a single left-click
 * on an instant-break block does the same thing by accident.
 *
 * <p>A swing paired with an entity attack in the same client tick has no such
 * ambiguity. One left-click cannot both mine a block and attack an entity, so
 * mining is excluded structurally — no state to desync, nothing to latch. It
 * also narrows the check to the only click rate that carries an advantage in
 * combat.</p>
 *
 * <h2>Why it measures in client ticks and not wall-clock time</h2>
 *
 * <p>Packets are timestamped when the server receives them, which is not when
 * the player sent them. A lag spike, a GC pause or a TCP burst delivers a
 * backlog all at once, so a wall-clock window sees a legitimate player at an
 * impossible rate with intervals microseconds apart — the exact shape this check
 * is looking for.
 *
 * <p>The window is therefore anchored to the player's own tick clock: a vanilla
 * client sends one movement packet per tick, 20 per second. A burst delivers the
 * queued movement packets alongside the queued attacks, so both counters advance
 * together and {@code attacks per tick} is unchanged. The rate signal is immune
 * to network conditions by construction.
 *
 * <p>The consistency signal cannot be made immune the same way — it reads arrival
 * timestamps directly — so it is instead <i>gated</i>: it only runs over a window
 * whose movement packets themselves arrived on a regular cadence, and only when
 * the mean gap between attacks is humanly plausible. A burst fails both gates.</p>
 *
 * <h2>Flag pacing</h2>
 *
 * <p>Each signal must hold for {@code sustained-windows} consecutive windows
 * before it flags, and flags at most once per window. A single bad window cannot
 * walk a player up the punishment ladder.</p>
 *
 * <p>This check is experimental, so it stays inert until {@code experimental-checks:
 * true} is set in the config. Run it in alert-only mode on a live server and tune
 * the thresholds against real players before wiring it to a kick or a ban.</p>
 */
@CheckData(name = "Autoclicker", configName = "Autoclicker", stableKey = "bucksmpac.autoclicker.cps",
        description = "Attacked at an inhuman rate or with robotic consistency",
        decay = 0.05, experimental = true)
public class AutoClickerA extends Check implements PacketCheck {

    /** Movement packets a vanilla client sends per second. */
    private static final int TICKS_PER_SECOND = 20;

    /**
     * Shortest mean gap between attacks the consistency test will look at.
     * 20 ms is 50 attacks per second — already far past what a hand can do, so
     * anything tighter is a delivery artifact rather than a click pattern.
     */
    private static final double MIN_PLAUSIBLE_INTERVAL_MS = 20.0;

    private static final int MAX_SAMPLES = 128;

    // --- config ---
    private double maxAttackCps = 20.0;
    private double constantDeviationMs = 3.0;
    private int minSamples = 12;
    private int windowTicks = 40;
    private int sustainedWindows = 2;
    private double maxNetJitterMs = 12.0;

    // --- current window ---
    private int ticksInWindow;
    private int attacksInWindow;

    /** Arrival time of each counted attack; feeds the consistency test. */
    private final long[] attackNanos = new long[MAX_SAMPLES];
    private int attackSampleCount;

    /** Arrival time of each movement packet; feeds the network-jitter gate. */
    private final long[] tickNanos = new long[MAX_SAMPLES];
    private int tickSampleCount;

    // --- current client tick ---
    private boolean swungThisTick;
    private boolean attackedThisTick;
    private long swingNanoThisTick;

    // --- consecutive violating windows, per signal ---
    private int rateStreak;
    private int constancyStreak;

    public AutoClickerA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.ANIMATION) {
            // One click is one swing. Keeping the first timestamp of the tick
            // stops a client from shifting the rhythm with padding swings.
            if (!swungThisTick) {
                swungThisTick = true;
                swingNanoThisTick = System.nanoTime();
            }
            return;
        }

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == InteractAction.ATTACK) {
                attackedThisTick = true;
            }
            return;
        }

        // 1.7/1.8 clients send attacks on their own packet type.
        if (type == PacketType.Play.Client.ATTACK) {
            attackedThisTick = true;
            return;
        }

        if (type == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
                // A respawn ends the fight; don't carry half a window across it.
                resetAll();
            }
            return;
        }

        // Movement packets are the clock. isTickPacket() already drops teleports
        // and the 1.17 duplicate, so this advances once per real client tick.
        if (isTickPacket(type)) {
            onClientTick();
        }
    }

    private void onClientTick() {
        if (tickSampleCount < MAX_SAMPLES) {
            tickNanos[tickSampleCount++] = System.nanoTime();
        }
        ticksInWindow++;

        if (swungThisTick && attackedThisTick) {
            attacksInWindow++;
            if (attackSampleCount < MAX_SAMPLES) {
                attackNanos[attackSampleCount++] = swingNanoThisTick;
            }
        }
        swungThisTick = false;
        attackedThisTick = false;

        if (ticksInWindow >= windowTicks) {
            evaluateWindow();
        }
    }

    private void evaluateWindow() {
        // Ticks, not seconds: a burst inflates both counts, so the ratio holds.
        final double cps = attacksInWindow * (double) TICKS_PER_SECOND / ticksInWindow;

        boolean flagged = false;
        if (cps > maxAttackCps) {
            if (++rateStreak >= sustainedWindows) {
                rateStreak = 0;
                flagged = flag("attack-cps=" + oneDecimal(cps) + " > " + oneDecimal(maxAttackCps)
                        + " sustained over " + sustainedWindows + "x" + windowTicks + " client ticks");
            }
        } else {
            rateStreak = 0;
        }

        if (!flagged) {
            flagged = evaluateConstancy(cps);
        }

        if (!flagged) reward();
        resetWindow();
    }

    /** @return true if the consistency signal flagged this window. */
    private boolean evaluateConstancy(double cps) {
        if (attackSampleCount < minSamples) {
            constancyStreak = 0;
            return false;
        }

        final double netJitterMs = ClickSampleStats.stdDevIntervalMillis(tickNanos, tickSampleCount);
        final double meanMs = ClickSampleStats.meanIntervalMillis(attackNanos, attackSampleCount);
        final double deviationMs = ClickSampleStats.stdDevIntervalMillis(attackNanos, attackSampleCount);

        // The player's own movement packets are the reference for what this
        // connection's delivery timing looks like. If those arrived raggedly,
        // the attack timings carry the same noise and prove nothing.
        final boolean connectionStable = !Double.isNaN(netJitterMs) && netJitterMs <= maxNetJitterMs;
        // A flushed backlog arrives microseconds apart and reads as flawlessly
        // constant. Real clicking never does.
        final boolean plausibleSpacing = !Double.isNaN(meanMs) && meanMs >= MIN_PLAUSIBLE_INTERVAL_MS;
        final boolean robotic = !Double.isNaN(deviationMs) && deviationMs < constantDeviationMs;

        if (!connectionStable || !plausibleSpacing || !robotic) {
            constancyStreak = 0;
            return false;
        }

        if (++constancyStreak < sustainedWindows) return false;

        constancyStreak = 0;
        return flag("attack-cps=" + oneDecimal(cps)
                + " deviation=" + oneDecimal(deviationMs) + "ms < " + oneDecimal(constantDeviationMs) + "ms"
                + " (net jitter " + oneDecimal(netJitterMs) + "ms, n=" + attackSampleCount + ")");
    }

    private void resetWindow() {
        ticksInWindow = 0;
        attacksInWindow = 0;
        attackSampleCount = 0;
        tickSampleCount = 0;
        swungThisTick = false;
        attackedThisTick = false;
    }

    /** Window state plus the cross-window streaks. */
    private void resetAll() {
        resetWindow();
        rateStreak = 0;
        constancyStreak = 0;
    }

    /** One-decimal rendering without String.format; only runs on the flag path. */
    private static String oneDecimal(double value) {
        long tenths = Math.round(value * 10);
        return tenths / 10 + "." + Math.abs(tenths % 10);
    }

    @Override
    public void onReload(ConfigManager config) {
        final String base = getConfigName();
        maxAttackCps = config.getDoubleElse(base + ".max-attack-cps", 20.0);
        constantDeviationMs = config.getDoubleElse(base + ".constant-deviation-ms", 3.0);
        minSamples = Math.max(2, config.getIntElse(base + ".min-samples", 12));
        windowTicks = Math.min(MAX_SAMPLES, Math.max(10, config.getIntElse(base + ".window-ticks", 40)));
        sustainedWindows = Math.max(1, config.getIntElse(base + ".sustained-windows", 2));
        maxNetJitterMs = config.getDoubleElse(base + ".max-net-jitter-ms", 12.0);
        resetAll();
    }
}
