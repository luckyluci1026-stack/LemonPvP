package de.lemonpvp.duelplus.session;

import net.kyori.adventure.bossbar.BossBar;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Set<UUID> unentschiedenZustimmung = ConcurrentHashMap.newKeySet();
    private volatile long letzterTrefferMillis = System.currentTimeMillis();
    private volatile boolean inaktivitaetsWarnungGezeigt = false;
    private volatile BossBar bossBar;
    private volatile boolean ploetzlicherTodGezeigt = false;

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
        this.letzterTrefferMillis = System.currentTimeMillis();
    }

    /**
     * Von ArenaGuardListener bei jedem verarbeiteten Treffer aufgerufen -
     * Basis fuer die Camping-Erkennung UND die Aufgabe-bei-Inaktivitaet:
     * JEDER Treffer setzt beides komplett zurueck, egal wie weit die Uhr
     * schon gelaufen war.
     */
    public void treffer() {
        this.letzterTrefferMillis = System.currentTimeMillis();
        this.inaktivitaetsWarnungGezeigt = false;
    }

    public long millisSeitLetztemTreffer() {
        return System.currentTimeMillis() - letzterTrefferMillis;
    }

    /** true, sobald die 10-Minuten-Warnung schon rausgegangen ist - verhindert Mehrfachversand. */
    public boolean inaktivitaetsWarnungGezeigt() {
        return inaktivitaetsWarnungGezeigt;
    }

    public void inaktivitaetsWarnungSetzen() {
        this.inaktivitaetsWarnungGezeigt = true;
    }

    /** Traegt spieler als /draw-zustimmend ein - true, wenn danach BEIDE zugestimmt haben. */
    public boolean unentschiedenZustimmen(UUID spieler) {
        unentschiedenZustimmung.add(spieler);
        return unentschiedenZustimmung.contains(spielerA) && unentschiedenZustimmung.contains(spielerB);
    }

    public boolean hatUnentschiedenVorgeschlagen(UUID spieler) {
        return unentschiedenZustimmung.contains(spieler);
    }

    /** Boss-Bar dieser Session (zeigt den Grenz-Fortschritt waehrend des Kampfes) - null vor Kampfbeginn. */
    public BossBar bossBar() {
        return bossBar;
    }

    public void bossBarSetzen(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    /** true, sobald der Ploetzlicher-Tod-Moment (Grenze am Minimum) schon einmal ausgeloest wurde - verhindert Mehrfachversand. */
    public boolean ploetzlicherTodGezeigt() {
        return ploetzlicherTodGezeigt;
    }

    public void ploetzlicherTodSetzen() {
        this.ploetzlicherTodGezeigt = true;
    }
}
