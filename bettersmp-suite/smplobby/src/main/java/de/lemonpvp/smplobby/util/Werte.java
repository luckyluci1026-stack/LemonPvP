package de.lemonpvp.smplobby.util;

import java.util.Map;

/**
 * Werte aus der Konfiguration herausholen, ohne sich zu verrennen.
 *
 * `getMapList()` liefert `List<Map<?, ?>>` - Wildcards, bei denen der
 * Wertetyp unbekannt ist. Deshalb geht `map.getOrDefault(schluessel,
 * "ersatz")` NICHT: Der Ersatzwert muesste vom selben unbekannten Typ
 * sein wie die Werte in der Map, und ein String ist das nicht.
 *
 * Der Compiler sagt dazu „String kann nicht in capture#1 von ?
 * konvertiert werden", was beim ersten Mal niemand versteht. Statt das
 * an jeder Fundstelle einzeln zu umschiffen, steht es hier einmal
 * richtig: erst `get()`, dann selbst auf null pruefen.
 */
public final class Werte {

    private Werte() {
    }

    /** Ein Text aus der Map - oder der Ersatz, wenn nichts dasteht. */
    public static String text(Map<?, ?> map, String schluessel, String ersatz) {
        Object wert = map.get(schluessel);
        return wert == null ? ersatz : String.valueOf(wert);
    }

    /** Eine Zahl aus der Map. Auch "3" als Text wird angenommen. */
    public static int zahl(Map<?, ?> map, String schluessel, int ersatz) {
        Object wert = map.get(schluessel);
        if (wert instanceof Number nummer) {
            return nummer.intValue();
        }
        if (wert == null) {
            return ersatz;
        }
        try {
            return Integer.parseInt(String.valueOf(wert).trim());
        } catch (NumberFormatException fehler) {
            return ersatz;
        }
    }

    /**
     * Eine Liste von Zeilen aus der Map.
     *
     * Steht dort ein einzelner Text statt einer Liste, wird er als
     * einzeilige Liste genommen - das ist der haeufigste Tippfehler in
     * einer Konfiguration und kein Grund, gar nichts anzuzeigen.
     */
    public static java.util.List<String> zeilen(Map<?, ?> map, String schluessel) {
        Object wert = map.get(schluessel);
        if (wert instanceof java.util.List<?> liste) {
            java.util.List<String> raus = new java.util.ArrayList<>(liste.size());
            for (Object eintrag : liste) {
                raus.add(String.valueOf(eintrag));
            }
            return raus;
        }
        return wert == null ? java.util.List.of() : java.util.List.of(String.valueOf(wert));
    }
}
