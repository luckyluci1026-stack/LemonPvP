package de.lemonpvp.reportplus.store;

import java.util.UUID;

/** Eine Bugmeldung. status ist "OFFEN", "ERLEDIGT" oder "VERWORFEN". */
public record BugEntry(int id, UUID reporter, String reporterName, String kategorie, String text,
                        long zeit, String status) {

    public boolean offen() {
        return "OFFEN".equals(status);
    }
}
