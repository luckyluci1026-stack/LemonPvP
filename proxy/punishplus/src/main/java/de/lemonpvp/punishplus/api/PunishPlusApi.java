package de.lemonpvp.punishplus.api;

import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.punishplus.PunishManager;

/**
 * Statische API fuer andere Velocity-Plugins im selben Proxy-Prozess.
 * init() setzt PunishPlus beim eigenen Start - Aufrufe davor oder ganz
 * ohne das Plugin liefern einfach false statt eine NullPointerException.
 *
 * Nur online Spieler als Ziel, genau wie /offend und /punish selbst
 * (siehe PunishManager) - so nutzen Befehl und API exakt denselben Weg.
 */
public final class PunishPlusApi {

    private static PunishManager manager;

    private PunishPlusApi() {
    }

    /** Wird von PunishPlus beim Start gesetzt. */
    public static void init(PunishManager m) {
        manager = m;
    }

    /** @return true, wenn die temporaere Sperre angewendet wurde. */
    public static boolean offend(Player spieler, String grundId, String ausfuehrer) {
        return manager != null && manager.offend(spieler, grundId, ausfuehrer) == PunishManager.Ergebnis.OK;
    }

    /** @return true, wenn die dauerhafte Sperre angewendet wurde. */
    public static boolean punish(Player spieler, String grundId, String ausfuehrer) {
        return manager != null && manager.punish(spieler, grundId, ausfuehrer) == PunishManager.Ergebnis.OK;
    }
}
