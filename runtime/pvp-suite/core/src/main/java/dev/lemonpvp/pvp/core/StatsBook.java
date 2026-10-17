package dev.lemonpvp.pvp.core;

import dev.lemonpvp.pvp.api.PlayerStats;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * All PvP statistics in memory. Main thread only (events and commands run there).
 * Plain Java without server code, so the rules are unit-tested (StatsBookTest).
 */
final class StatsBook {

    /** Best first: rating, then kills, then name. */
    static final Comparator<PlayerStats> RANKING = Comparator.comparingInt(PlayerStats::rating).reversed()
            .thenComparing(Comparator.comparingInt(PlayerStats::kills).reversed())
            .thenComparing(s -> s.name().toLowerCase(Locale.ROOT));

    /**
     * Result of a kill.
     *
     * @param killer       killer after the kill
     * @param victim       victim after the death
     * @param ratingChange points moved from the victim to the killer
     * @param endedStreak  the victim's streak before this death
     */
    record Kill(PlayerStats killer, PlayerStats victim, int ratingChange, int endedStreak) {
    }

    private final Map<UUID, PlayerStats> players = new HashMap<>();
    private int kFactor;
    private boolean dirty;

    /** @param kFactor Elo K-factor (see {@link Elo#change}) */
    StatsBook(int kFactor) {
        this.kFactor = kFactor;
    }

    /** @param kFactor new K-factor (configuration reload) */
    void kFactor(int kFactor) {
        this.kFactor = kFactor;
    }

    /**
     * Counts a kill: the killer's kills and streak go up, the victim's deaths go up and
     * its streak ends; rating moves from the victim to the killer.
     */
    Kill recordKill(UUID killerId, String killerName, UUID victimId, String victimName) {
        if (killerId.equals(victimId)) {
            throw new IllegalArgumentException("a player cannot kill themselves");
        }
        PlayerStats killer = get(killerId, killerName);
        PlayerStats victim = get(victimId, victimName);
        int change = Elo.change(killer.rating(), victim.rating(), this.kFactor);
        int streak = killer.streak() + 1;
        PlayerStats newKiller = new PlayerStats(killerId, killerName, killer.kills() + 1, killer.deaths(), streak,
                Math.max(streak, killer.bestStreak()), killer.rating() + change);
        PlayerStats newVictim = new PlayerStats(victimId, victimName, victim.kills(), victim.deaths() + 1, 0,
                victim.bestStreak(), Math.max(0, victim.rating() - change));
        this.players.put(killerId, newKiller);
        this.players.put(victimId, newVictim);
        this.dirty = true;
        return new Kill(newKiller, newVictim, change, victim.streak());
    }

    /**
     * Counts a death without a killing player (fall, lava, ...): the streak ends, the
     * rating stays.
     *
     * @return the streak that ended
     */
    int recordDeath(UUID id, String name) {
        PlayerStats s = get(id, name);
        this.players.put(id, new PlayerStats(id, name, s.kills(), s.deaths() + 1, 0, s.bestStreak(), s.rating()));
        this.dirty = true;
        return s.streak();
    }

    /** Updates the stored name of a known player (players can change their name). */
    void seen(UUID id, String name) {
        PlayerStats s = this.players.get(id);
        if (s != null && !s.name().equals(name)) {
            this.players.put(id, new PlayerStats(id, name, s.kills(), s.deaths(), s.streak(), s.bestStreak(), s.rating()));
            this.dirty = true;
        }
    }

    Optional<PlayerStats> stats(UUID id) {
        return Optional.ofNullable(this.players.get(id));
    }

    Optional<PlayerStats> statsByName(String name) {
        return this.players.values().stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst();
    }

    List<PlayerStats> top(int limit) {
        return this.players.values().stream().sorted(RANKING).limit(Math.max(0, limit)).toList();
    }

    int rank(UUID id) {
        PlayerStats s = this.players.get(id);
        if (s == null) {
            return 0;
        }
        return (int) this.players.values().stream().filter(o -> RANKING.compare(o, s) < 0).count() + 1;
    }

    /** @return whether the player had statistics */
    boolean reset(UUID id) {
        boolean removed = this.players.remove(id) != null;
        this.dirty |= removed;
        return removed;
    }

    /** @return number of players removed */
    int resetAll() {
        int n = this.players.size();
        this.players.clear();
        this.dirty |= n > 0;
        return n;
    }

    /** Replaces everything with loaded statistics (not dirty afterwards). */
    void load(Collection<PlayerStats> loaded) {
        this.players.clear();
        loaded.forEach(s -> this.players.put(s.id(), s));
        this.dirty = false;
    }

    /** @return a copy of all statistics, e.g. for saving on another thread */
    List<PlayerStats> snapshot() {
        return new ArrayList<>(this.players.values());
    }

    int size() {
        return this.players.size();
    }

    boolean dirty() {
        return this.dirty;
    }

    void markSaved() {
        this.dirty = false;
    }

    /** Called when saving failed, so the next autosave tries again. */
    void markDirty() {
        this.dirty = true;
    }

    private PlayerStats get(UUID id, String name) {
        PlayerStats s = this.players.get(id);
        return s != null ? s : new PlayerStats(id, name, 0, 0, 0, 0, Elo.START);
    }
}
