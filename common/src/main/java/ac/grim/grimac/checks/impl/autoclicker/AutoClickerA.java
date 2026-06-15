/*
 * This file is part of FLFAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * FLFAC custom addition.
 */
package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * FLFAC custom AutoClicker check.
 *
 * <p>Measures clicks-per-second and the consistency of the time between clicks
 * from arm-swing animations. Continuous block breaking also swings the arm, so
 * those swings are ignored to avoid false positives while mining.</p>
 *
 * <p>Flags on impossible CPS or on inhumanly low variance between clicks (real
 * humans never click at an almost perfectly constant interval).</p>
 *
 * <p>This check is experimental, so it is disabled until you set
 * {@code experimental-checks: true} in the config - test it before relying on
 * it for punishments.</p>
 */
@CheckData(name = "Autoclicker", configName = "Autoclicker", stableKey = "flfac.autoclicker.cps",
        description = "Clicked at an inhuman rate or with robotic consistency",
        decay = 0.05, experimental = true)
public class AutoClickerA extends Check implements PacketCheck {

    private final Deque<Long> clicks = new ArrayDeque<>();
    private boolean digging;

    private int maxCps = 16;
    private double minConstantDeviationMs = 1.2;
    private int minSamples = 8;

    public AutoClickerA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            if (action == DiggingAction.START_DIGGING) {
                digging = true;
            } else if (action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.CANCELLED_DIGGING) {
                digging = false;
            }
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) {
            return;
        }
        // Block breaking swings the arm continuously - never count it as clicking.
        if (digging) {
            return;
        }

        long now = System.currentTimeMillis();
        clicks.addLast(now);
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }

        int cps = clicks.size();
        if (cps > maxCps) {
            flag("cps=" + cps + " > " + maxCps);
            return;
        }

        if (cps >= minSamples) {
            double deviation = intervalDeviation();
            if (deviation < minConstantDeviationMs) {
                flag("cps=" + cps + " deviation=" + String.format("%.2f", deviation) + "ms");
            }
        }
    }

    private double intervalDeviation() {
        if (clicks.size() < 2) {
            return Double.MAX_VALUE;
        }
        Long[] times = clicks.toArray(new Long[0]);
        int n = times.length - 1;
        double sum = 0.0;
        for (int i = 1; i < times.length; i++) {
            sum += times[i] - times[i - 1];
        }
        double mean = sum / n;
        double variance = 0.0;
        for (int i = 1; i < times.length; i++) {
            double diff = (times[i] - times[i - 1]) - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / n);
    }

    @Override
    public void onReload(ConfigManager config) {
        maxCps = config.getIntElse(getConfigName() + ".max-cps", 16);
        minConstantDeviationMs = config.getDoubleElse(getConfigName() + ".constant-deviation-ms", 1.2);
        minSamples = config.getIntElse(getConfigName() + ".min-samples", 8);
    }
}
