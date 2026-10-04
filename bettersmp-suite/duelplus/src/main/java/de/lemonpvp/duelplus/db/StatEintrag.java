package de.lemonpvp.duelplus.db;

import java.util.UUID;

/** Eine Zeile aus duelplus_stats - Sieg/Niederlage/Unentschieden-Zaehler eines einzelnen Spielers, fuer /duel stats und /duel top. */
public record StatEintrag(UUID uuid, String name, int siege, int niederlagen, int unentschieden) {

    public int gesamt() {
        return siege + niederlagen + unentschieden;
    }

    /** Siegquote in Prozent, gerundet - 0 statt einer Division durch 0, solange noch keine einzige Runde gezaehlt wurde. */
    public int siegquote() {
        return gesamt() == 0 ? 0 : Math.round(100f * siege / gesamt());
    }
}
