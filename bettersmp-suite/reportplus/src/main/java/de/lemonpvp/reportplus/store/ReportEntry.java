package de.lemonpvp.reportplus.store;

import java.util.UUID;

/** Eine Spieler-Meldung. status ist "OFFEN", "ERLEDIGT" oder "VERWORFEN". */
public record ReportEntry(int id, UUID reporter, String reporterName, UUID ziel, String zielName,
                           String kategorie, long zeit, String status) {

    public boolean offen() {
        return "OFFEN".equals(status);
    }
}
