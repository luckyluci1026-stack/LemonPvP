package com.lemonpvp.lemonpractice.duel;

import com.lemonpvp.lemonpractice.model.Arena;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

public class DuelGame {

    private final UUID player1Uuid;
    private final UUID player2Uuid;
    private final String player1Name;
    private final String player2Name;
    private final String gamemode;
    private final Arena arena;
    private DuelState state = DuelState.WAITING;
    private UUID winnerUuid;
    private long startTime;
    private int eloChangeP1;
    private int eloChangeP2;
    private Scoreboard scoreboard;
    private Scoreboard scoreboard2;

    public DuelGame(UUID p1, String p1Name, UUID p2, String p2Name, String gamemode, Arena arena) {
        this.player1Uuid = p1;
        this.player1Name = p1Name;
        this.player2Uuid = p2;
        this.player2Name = p2Name;
        this.gamemode = gamemode;
        this.arena = arena;
    }

    public UUID getPlayer1Uuid() { return player1Uuid; }
    public UUID getPlayer2Uuid() { return player2Uuid; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public String getGamemode() { return gamemode; }
    public Arena getArena() { return arena; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }
    public UUID getWinnerUuid() { return winnerUuid; }
    public void setWinnerUuid(UUID winnerUuid) { this.winnerUuid = winnerUuid; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public int getEloChangeP1() { return eloChangeP1; }
    public void setEloChangeP1(int v) { this.eloChangeP1 = v; }
    public int getEloChangeP2() { return eloChangeP2; }
    public void setEloChangeP2(int v) { this.eloChangeP2 = v; }

    public boolean isParticipant(UUID uuid) {
        return player1Uuid.equals(uuid) || player2Uuid.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1Uuid.equals(uuid)) return player2Uuid;
        if (player2Uuid.equals(uuid)) return player1Uuid;
        return null;
    }

    public boolean isDraw() { return winnerUuid == null; }
    public Scoreboard getScoreboard() { return scoreboard; }
    public void setScoreboard(Scoreboard scoreboard) { this.scoreboard = scoreboard; }
    public Scoreboard getScoreboard2() { return scoreboard2; }
    public void setScoreboard2(Scoreboard scoreboard2) { this.scoreboard2 = scoreboard2; }

    public int getDurationSeconds() {
        if (startTime == 0) return 0;
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }
}
