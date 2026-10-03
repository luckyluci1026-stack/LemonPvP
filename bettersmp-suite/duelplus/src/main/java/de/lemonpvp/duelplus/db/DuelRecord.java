package de.lemonpvp.duelplus.db;

import java.util.UUID;

/**
 * Eine Zeile aus duelplus_duelle - begleitet den kompletten Lebenszyklus
 * von der Herausforderung bis zum Ergebnis. spielerA/B sind bewusst
 * neutral benannt (nicht "herausforderer/ziel"): sobald angenommen,
 * sind beide gleichberechtigte Duell-Teilnehmer.
 */
public record DuelRecord(
        String id,
        UUID spielerA, String spielerAName, String spielerAServer,
        UUID spielerB, String spielerBName, String spielerBServer,
        String status,
        String arenaWelt,
        UUID gewinner,
        long erstellt,
        boolean aBearbeitet,
        boolean bBearbeitet,
        boolean zielGezeigt) {

    public static final String WARTEND = "WARTEND";
    public static final String ANGENOMMEN = "ANGENOMMEN";
    public static final String ABGELEHNT = "ABGELEHNT";
    public static final String ABGELAUFEN = "ABGELAUFEN";
    public static final String AKTIV = "AKTIV";
    public static final String BEENDET = "BEENDET";
}
