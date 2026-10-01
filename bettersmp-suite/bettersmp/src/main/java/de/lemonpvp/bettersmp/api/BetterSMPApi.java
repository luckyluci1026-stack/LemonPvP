package de.lemonpvp.bettersmp.api;

import de.lemonpvp.bettersmp.combat.CombatManager;
import de.lemonpvp.bettersmp.report.ReportManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Statische API für andere Plugins (z.B. Lifesteal+).
 */
public final class BetterSMPApi {

    private static CombatManager combat;
    private static ReportManager reports;

    private BetterSMPApi() {
    }

    /** Wird von BetterSMP beim Start gesetzt. */
    public static void init(CombatManager manager) {
        combat = manager;
    }

    public static void initReports(ReportManager manager) {
        reports = manager;
    }

    public static void gegnerMerken(UUID spieler, UUID gegner, String gegnerName) {
        if (reports != null) {
            reports.letzteGegner().merken(spieler, gegner, gegnerName);
        }
    }

    public static boolean isInCombat(UUID player) {
        return combat != null && combat.isTagged(player);
    }

    public static @Nullable UUID getCombatOpponent(UUID player) {
        return combat == null ? null : combat.opponent(player);
    }

    /** Verbleibende Kampfzeit in Millisekunden (0 = nicht im Kampf). */
    public static long getRemainingCombatMillis(UUID player) {
        return combat == null ? 0L : combat.remainingMillis(player);
    }
}
