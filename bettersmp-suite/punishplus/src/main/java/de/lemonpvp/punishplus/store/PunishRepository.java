package de.lemonpvp.punishplus.store;

import java.util.Optional;
import java.util.UUID;

/**
 * Wo aktive Sperren herkommen - austauschbar zwischen einer eigenen
 * gesperrt.yml (Standard, pro Server) und einer geteilten MariaDB
 * (optional, netzwerkweit). PunishManager und LoginListener kennen nur
 * dieses Interface, nicht welche der beiden gerade aktiv ist.
 */
public interface PunishRepository {

    void load();

    /**
     * Speichert eine bereits fertig gebaute Sperre. Feuert bei der
     * MariaDB-Variante asynchron - der Aufrufer (PunishManager) baut den
     * PunishRecord vorher rein lokal, damit Kick/Nachrichten nicht auf
     * die Datenbank warten muessen.
     */
    void speichern(PunishRecord record);

    /**
     * Aktive Sperre, falls vorhanden - raeumt abgelaufene automatisch weg.
     * Blockierend: nur aus bereits-asynchronem Kontext aufrufen (siehe
     * LoginListener, AsyncPlayerPreLoginEvent laeuft nicht im Haupt-Thread).
     */
    Optional<PunishRecord> aktiv(UUID spieler);

    /** Verbindung/Ressourcen sauber schliessen (z.B. beim Server-Stop). */
    void shutdown();
}
