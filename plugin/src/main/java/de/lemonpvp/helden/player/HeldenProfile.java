package de.lemonpvp.helden.player;

import java.util.UUID;

/** Persistenter Spielerdatensatz des Projekts. */
public final class HeldenProfile {

    private final UUID uuid;
    private String name;

    private String heroId;
    private String teamId;
    private long heroSelectedAt;

    private int lives;
    private boolean fallen;

    private int kills;
    private int deaths;
    private int assists;
    private int killStreak;
    private int bestKillStreak;

    private int coins;
    private long lastSeen;

    public HeldenProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name == null ? uuid.toString().substring(0, 8) : name;
    }

    public void name(String name) {
        this.name = name;
    }

    public String heroId() {
        return heroId;
    }

    public void heroId(String heroId) {
        this.heroId = heroId;
    }

    public boolean hasHero() {
        return heroId != null && !heroId.isEmpty();
    }

    public String teamId() {
        return teamId;
    }

    public void teamId(String teamId) {
        this.teamId = teamId;
    }

    public boolean hasTeam() {
        return teamId != null && !teamId.isEmpty();
    }

    public long heroSelectedAt() {
        return heroSelectedAt;
    }

    public void heroSelectedAt(long heroSelectedAt) {
        this.heroSelectedAt = heroSelectedAt;
    }

    public int lives() {
        return lives;
    }

    public void lives(int lives) {
        this.lives = Math.max(0, lives);
    }

    public boolean fallen() {
        return fallen;
    }

    public void fallen(boolean fallen) {
        this.fallen = fallen;
    }

    public int kills() {
        return kills;
    }

    public void kills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        kills++;
        killStreak++;
        if (killStreak > bestKillStreak) {
            bestKillStreak = killStreak;
        }
    }

    public int deaths() {
        return deaths;
    }

    public void deaths(int deaths) {
        this.deaths = deaths;
    }

    public void addDeath() {
        deaths++;
        killStreak = 0;
    }

    public int assists() {
        return assists;
    }

    public void addAssist() {
        assists++;
    }

    public void assists(int assists) {
        this.assists = assists;
    }

    public int killStreak() {
        return killStreak;
    }

    public void killStreak(int killStreak) {
        this.killStreak = killStreak;
    }

    public int bestKillStreak() {
        return bestKillStreak;
    }

    public void bestKillStreak(int bestKillStreak) {
        this.bestKillStreak = bestKillStreak;
    }

    public int coins() {
        return coins;
    }

    public void coins(int coins) {
        this.coins = Math.max(0, coins);
    }

    public long lastSeen() {
        return lastSeen;
    }

    public void lastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    /** Kill/Death-Verhaeltnis, auf zwei Nachkommastellen gerundet. */
    public String kd() {
        if (deaths == 0) {
            return String.format(java.util.Locale.ROOT, "%.2f", (double) kills);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", (double) kills / (double) deaths);
    }
}
