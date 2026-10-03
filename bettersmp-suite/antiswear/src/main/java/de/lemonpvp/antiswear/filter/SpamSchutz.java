package de.lemonpvp.antiswear.filter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpamSchutz {

    public enum Ergebnis { OK, ZU_SCHNELL, WIEDERHOLUNG }

    private static final int WIEDERHOLUNG_AB_ZEICHEN = 5;

    private static final class Verlauf {
        private final Deque<Long> zeiten = new ArrayDeque<>();
        private String letzte = "";
        private long letzteZeit;
    }

    private final Map<UUID, Verlauf> verlaeufe = new ConcurrentHashMap<>();
    private int maxNachrichten = 5;
    private long zeitraumMillis = 8_000L;
    private long wiederholungMillis = 30_000L;

    public void konfigurieren(int maxNachrichten, long zeitraumMillis, long wiederholungMillis) {
        this.maxNachrichten = maxNachrichten;
        this.zeitraumMillis = zeitraumMillis;
        this.wiederholungMillis = wiederholungMillis;
    }

    public Ergebnis pruefen(UUID spieler, String text, long jetzt) {
        Verlauf verlauf = verlaeufe.computeIfAbsent(spieler, schluessel -> new Verlauf());
        String vergleich = vergleichsText(text);
        synchronized (verlauf) {
            while (!verlauf.zeiten.isEmpty() && jetzt - verlauf.zeiten.peekFirst() > zeitraumMillis) {
                verlauf.zeiten.pollFirst();
            }
            if (maxNachrichten > 0 && verlauf.zeiten.size() >= maxNachrichten) {
                return Ergebnis.ZU_SCHNELL;
            }
            if (wiederholungMillis > 0 && vergleich.length() >= WIEDERHOLUNG_AB_ZEICHEN
                    && vergleich.equals(verlauf.letzte) && jetzt - verlauf.letzteZeit < wiederholungMillis) {
                return Ergebnis.WIEDERHOLUNG;
            }
            verlauf.zeiten.addLast(jetzt);
            verlauf.letzte = vergleich;
            verlauf.letzteZeit = jetzt;
            return Ergebnis.OK;
        }
    }

    public void vergessen(UUID spieler) {
        verlaeufe.remove(spieler);
    }

    private static String vergleichsText(String text) {
        StringBuilder ergebnis = new StringBuilder();
        for (String token : Normalisierer.tokens(text)) {
            ergebnis.append(Normalisierer.zusammenziehen(Normalisierer.laeufe(Normalisierer.skelett(token))));
        }
        return ergebnis.toString();
    }
}
