package de.lemonpvp.duelplus.db;

/**
 * Eine Zeile aus duelplus_zuschauer_anfrage - der "ich will zuschauen"-
 * Wunsch eines Spielers, geschrieben auf SEINEM Herkunftsserver, gelesen
 * und geloescht beim Ankommen auf dem Duels-Server (siehe
 * ZuschauerManager). Gleiches Hin-und-Abholen-Muster wie der
 * Inventar-Transport bei einem echten Duell (siehe DuelDatabase.
 * snapshotHolenUndLoeschen).
 */
public record ZuschauerAnfrage(String arenaWelt, String herkunftServer) {
}
