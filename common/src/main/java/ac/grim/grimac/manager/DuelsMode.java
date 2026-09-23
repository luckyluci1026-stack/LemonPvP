/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * BuckSMPAC with its hands behind its back: it watches, flags, alerts and
 * bans, but it never touches what the player is doing.
 *
 * <h2>Why a duel server needs this</h2>
 *
 * <p>Two things here can eat a hit. A check can cancel the attack packet
 * outright, and a setback — the rubber-band back to a known-good position —
 * makes {@code Reach} drop every attack until the client catches up, because
 * a player mid-teleport could otherwise land hits from anywhere.</p>
 *
 * <p>Both are the right call on an SMP. On a duel server they are not: a
 * dropped hit decides a match, and the player it was taken from has no way
 * to tell a cheat mitigation from a bad tick. They just know they hit and
 * nothing happened.</p>
 *
 * <h2>What it does and does not turn off</h2>
 *
 * <p>Detection is untouched. Every check still runs, still flags, still
 * alerts, and punishments.yml still bans. What stops is intervention:
 * {@code shouldModifyPackets()} is false for every check, so none of them
 * cancels anything, and violation setbacks do not fire.</p>
 *
 * <p>Resynchronisation setbacks are deliberately left alone. Those are not
 * punishment — they repair a genuine desync between client and server, and
 * switching them off trades dropped hits for a player standing somewhere
 * the server disagrees with.</p>
 *
 * <p>The cost is honest and worth stating: a reach cheat that would have had
 * its hits cancelled now lands them. It is still flagged and still banned at
 * the configured threshold — it simply gets to hit until then.</p>
 */
@UtilityClass
public class DuelsMode {

    private static final String CONFIG_KEY = "duels-mode";

    /**
     * Read on every packet through {@code shouldModifyPackets()}, so it is
     * cached rather than looked up. Refreshed from {@code Check.reload}, which
     * runs for every check on every config reload.
     */
    private static volatile boolean enabled;
    private static volatile boolean announced;

    public static boolean isOn() {
        return enabled;
    }

    /** Picks up a change to the config. Cheap and idempotent; called a lot. */
    public static void refresh(@NotNull ConfigManager config) {
        boolean now;
        try {
            now = config.getBooleanElse(CONFIG_KEY, false);
        } catch (Throwable ignored) {
            return; // Config not readable yet; keep whatever we had.
        }

        if (now != enabled) {
            enabled = now;
            announced = false;
        }
        if (enabled && !announced) {
            announced = true;
            LogUtil.info("BuckSMPAC - Duels Edition. No check will cancel a packet and no violation setback "
                    + "will fire, so a hit is never taken away from a player. Detection, alerts and bans all "
                    + "still work. Turn this off (" + CONFIG_KEY + ") on servers that are not duels.");
        }
    }
}
