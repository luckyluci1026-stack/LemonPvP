package de.lemonpvp.duelplus.db;

import java.util.UUID;

public record ReplayEintrag(String id, UUID spielerA, String spielerAName, UUID spielerB, String spielerBName,
                            String arena, long start, long dauerMillis, long groesse, UUID gewinner, int ergebnis,
                            boolean gemeldet, long behaltenBis, String server) {

    public boolean beteiligt(UUID spieler) {
        return spieler.equals(spielerA) || spieler.equals(spielerB);
    }

    public String kurzId() {
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
