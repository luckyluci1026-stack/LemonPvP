package de.lemonpvp.punishplus.store;

import java.util.UUID;

/**
 * Eine aktive Sperre. bis &lt;= 0 heisst dauerhaft (immer bei art ==
 * PUNISH, bei OFFEND je nach konfigurierter Dauer des Grundes).
 */
public record PunishRecord(UUID spieler, String spielerName, String grundId, String grundText,
                            String art, long von, long bis, String ausfuehrer) {

    public boolean aktiv() {
        return bis <= 0 || bis > System.currentTimeMillis();
    }

    public boolean dauerhaft() {
        return bis <= 0;
    }
}
