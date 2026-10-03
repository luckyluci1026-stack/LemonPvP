package de.lemonpvp.duelplus.util;

import de.lemonpvp.bettersmp.api.BetterSMPApi;
import org.bukkit.Bukkit;

import java.util.UUID;

public final class KampfPruefung {

    public long restMillis(UUID spieler) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterSMP")) {
            return 0L;
        }
        return BetterSmp.restMillis(spieler);
    }

    public static long sekunden(long millis) {
        return Math.max(1L, (millis + 999L) / 1000L);
    }

    private static final class BetterSmp {
        private static long restMillis(UUID spieler) {
            return BetterSMPApi.getRemainingCombatMillis(spieler);
        }
    }
}
