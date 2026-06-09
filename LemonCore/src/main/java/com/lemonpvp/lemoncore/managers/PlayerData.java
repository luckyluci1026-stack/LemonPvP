package com.lemonpvp.lemoncore.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String username;
    private long coins;
    private int kills;
    private int deaths;
    private int killstreak;
    private int bestKillstreak;
    private String nick;
    private String hiddenName;
    private boolean recordingMode;

    // Settings
    private boolean publicChat = true;
    private boolean partyInvites = true;
    private boolean messagesEnabled = true;
    private boolean fastCrystals = false;

    // ELO per gamemode
    private final Map<String, Integer> elo = new HashMap<>();

    // Friends (loaded separately)
    private final java.util.Set<UUID> friends = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public long getCoins() { return coins; }
    public void setCoins(long coins) { this.coins = coins; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public int getKillstreak() { return killstreak; }
    public void setKillstreak(int killstreak) { this.killstreak = killstreak; }
    public int getBestKillstreak() { return bestKillstreak; }
    public void setBestKillstreak(int bestKillstreak) { this.bestKillstreak = bestKillstreak; }
    public String getNick() { return nick; }
    public void setNick(String nick) { this.nick = nick; }
    public String getHiddenName() { return hiddenName; }
    public void setHiddenName(String hiddenName) { this.hiddenName = hiddenName; }
    public boolean isRecordingMode() { return recordingMode; }
    public void setRecordingMode(boolean recordingMode) { this.recordingMode = recordingMode; }
    public boolean isPublicChat() { return publicChat; }
    public void setPublicChat(boolean publicChat) { this.publicChat = publicChat; }
    public boolean isPartyInvites() { return partyInvites; }
    public void setPartyInvites(boolean partyInvites) { this.partyInvites = partyInvites; }
    public boolean isMessagesEnabled() { return messagesEnabled; }
    public void setMessagesEnabled(boolean messagesEnabled) { this.messagesEnabled = messagesEnabled; }
    public boolean isFastCrystals() { return fastCrystals; }
    public void setFastCrystals(boolean fastCrystals) { this.fastCrystals = fastCrystals; }
    public Map<String, Integer> getElo() { return elo; }
    public int getEloFor(String gamemode) { return elo.getOrDefault(gamemode, 1000); }
    public void setEloFor(String gamemode, int value) { elo.put(gamemode, value); }
    public java.util.Set<UUID> getFriends() { return friends; }
    public boolean isFriend(UUID uuid) { return friends.contains(uuid); }
    public void addFriend(UUID uuid) { friends.add(uuid); }
    public void removeFriend(UUID uuid) { friends.remove(uuid); }

    public double getKDRatio() {
        if (deaths == 0) return kills;
        return Math.round((double) kills / deaths * 100.0) / 100.0;
    }

    public String getDisplayName() {
        if (hiddenName != null) return hiddenName;
        if (nick != null) return nick;
        return username;
    }
}
