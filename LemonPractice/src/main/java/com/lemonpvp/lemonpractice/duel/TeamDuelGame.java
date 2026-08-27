package com.lemonpvp.lemonpractice.duel;

import com.lemonpvp.lemonpractice.model.Arena;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A 2v2 team duel: two duos in one arena. Kept separate from the 1v1
 * {@link DuelGame} so the core ranked path stays untouched — team matches are
 * casual (no ELO/streaks). A team loses when both members are dead or gone.
 */
public class TeamDuelGame {

    private final Set<UUID> team1 = new LinkedHashSet<>();
    private final Set<UUID> team2 = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final String gamemode;
    private final Arena arena;
    private DuelState state = DuelState.WAITING;
    private long startTime;

    public TeamDuelGame(Set<UUID> team1, Set<UUID> team2, String gamemode, Arena arena) {
        this.team1.addAll(team1);
        this.team2.addAll(team2);
        this.alive.addAll(team1);
        this.alive.addAll(team2);
        this.gamemode = gamemode;
        this.arena = arena;
    }

    public Set<UUID> getTeam1() { return team1; }
    public Set<UUID> getTeam2() { return team2; }
    public Set<UUID> getAlive() { return alive; }
    public String getGamemode() { return gamemode; }
    public Arena getArena() { return arena; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public boolean isParticipant(UUID uuid) {
        return team1.contains(uuid) || team2.contains(uuid);
    }

    /** True if both players are on the same team. */
    public boolean sameTeam(UUID a, UUID b) {
        return (team1.contains(a) && team1.contains(b)) || (team2.contains(a) && team2.contains(b));
    }

    /** 1 or 2 for a participant, 0 otherwise. */
    public int teamOf(UUID uuid) {
        if (team1.contains(uuid)) return 1;
        if (team2.contains(uuid)) return 2;
        return 0;
    }

    /** True if no member of the given team is still alive. */
    public boolean teamEliminated(int team) {
        Set<UUID> members = team == 1 ? team1 : team2;
        for (UUID u : members) if (alive.contains(u)) return false;
        return true;
    }

    public int getDurationSeconds() {
        if (startTime == 0) return 0;
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }
}
