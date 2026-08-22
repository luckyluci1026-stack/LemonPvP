package de.lemonpvp.helden.player;

import java.util.UUID;

/** Persistenter Spielerdatensatz des Projekts. */
public final class HeldenProfile {

    private final UUID uuid;
    private String name;

    /** Verbleibende Herzen inklusive Link-Herz. 0 = ausgeschieden. */
    private int hearts;
    private boolean eliminated;
    private long eliminatedAt;

    /** Spieler, an dem das eigene Link-Herz haengt. */
    private UUID linkPartner;

    private int kills;
    private int pvpDeaths;
    private int naturalDeaths;
    private long lastSeen;

    public HeldenProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name == null || name.isEmpty() ? uuid.toString().substring(0, 8) : name;
    }

    public void name(String name) {
        this.name = name;
    }

    public int hearts() {
        return hearts;
    }

    public void hearts(int hearts) {
        this.hearts = Math.max(0, hearts);
    }

    public boolean eliminated() {
        return eliminated;
    }

    public void eliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public long eliminatedAt() {
        return eliminatedAt;
    }

    public void eliminatedAt(long eliminatedAt) {
        this.eliminatedAt = eliminatedAt;
    }

    public UUID linkPartner() {
        return linkPartner;
    }

    public void linkPartner(UUID linkPartner) {
        this.linkPartner = linkPartner;
    }

    public boolean hasLinkPartner() {
        return linkPartner != null;
    }

    public int kills() {
        return kills;
    }

    public void kills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        kills++;
    }

    public int pvpDeaths() {
        return pvpDeaths;
    }

    public void pvpDeaths(int pvpDeaths) {
        this.pvpDeaths = pvpDeaths;
    }

    public void addPvpDeath() {
        pvpDeaths++;
    }

    public int naturalDeaths() {
        return naturalDeaths;
    }

    public void naturalDeaths(int naturalDeaths) {
        this.naturalDeaths = naturalDeaths;
    }

    public void addNaturalDeath() {
        naturalDeaths++;
    }

    public long lastSeen() {
        return lastSeen;
    }

    public void lastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
