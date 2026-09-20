package de.lemonpvp.duelplus.session;

import java.util.UUID;

/** Eine laufende Arena-Session - nur auf dem Duels-Server im Speicher, waehrend ein Duell aktiv ist. */
public final class DuellSession {

    private final String duellId;
    private final String arenaName;
    private final UUID spielerA;
    private final String spielerAName;
    private final String spielerAServer;
    private final UUID spielerB;
    private final String spielerBName;
    private final String spielerBServer;
    private volatile boolean kampfLaeuft = false;

    public DuellSession(String duellId, String arenaName,
                         UUID spielerA, String spielerAName, String spielerAServer,
                         UUID spielerB, String spielerBName, String spielerBServer) {
        this.duellId = duellId;
        this.arenaName = arenaName;
        this.spielerA = spielerA;
        this.spielerAName = spielerAName;
        this.spielerAServer = spielerAServer;
        this.spielerB = spielerB;
        this.spielerBName = spielerBName;
        this.spielerBServer = spielerBServer;
    }

    public String duellId() {
        return duellId;
    }

    public String arenaName() {
        return arenaName;
    }

    public UUID spielerA() {
        return spielerA;
    }

    public UUID spielerB() {
        return spielerB;
    }

    public UUID gegnerVon(UUID spieler) {
        return spieler.equals(spielerA) ? spielerB : spielerA;
    }

    public String gegnerNameVon(UUID spieler) {
        return spieler.equals(spielerA) ? spielerBName : spielerAName;
    }

    /** Herkunftsserver (SMP/Lobby/...), auf den dieser Spieler nach dem Duell zurueck soll. */
    public String herkunftsServerVon(UUID spieler) {
        return spieler.equals(spielerA) ? spielerAServer : spielerBServer;
    }

    public boolean enthaelt(UUID spieler) {
        return spieler.equals(spielerA) || spieler.equals(spielerB);
    }

    /** Ab wann Schaden wirklich zaehlt (Countdown vorbei, noch keiner besiegt). */
    public boolean kampfLaeuft() {
        return kampfLaeuft;
    }

    public void kampfStarten() {
        this.kampfLaeuft = true;
    }
}
