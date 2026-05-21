package com.lemonpvp.lemonpractice.model;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FFAArena {
    private int id;
    private String name;
    private String gamemode;
    private String worldName;
    private final List<Location> spawnPoints = new ArrayList<>();
    private int regionX1, regionY1, regionZ1;
    private int regionX2, regionY2, regionZ2;
    private int playerCount = 0;

    private static final Random random = new Random();

    public FFAArena(int id, String name, String gamemode) {
        this.id = id;
        this.name = name;
        this.gamemode = gamemode;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getGamemode() { return gamemode; }
    public void setGamemode(String gamemode) { this.gamemode = gamemode; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public List<Location> getSpawnPoints() { return spawnPoints; }
    public void addSpawnPoint(Location loc) { spawnPoints.add(loc); }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public void incrementPlayers() { playerCount++; }
    public void decrementPlayers() { if (playerCount > 0) playerCount--; }

    public Location getRandomSpawn() {
        if (spawnPoints.isEmpty()) return null;
        return spawnPoints.get(random.nextInt(spawnPoints.size()));
    }

    public int getRegionX1() { return regionX1; }
    public void setRegionX1(int v) { regionX1 = v; }
    public int getRegionY1() { return regionY1; }
    public void setRegionY1(int v) { regionY1 = v; }
    public int getRegionZ1() { return regionZ1; }
    public void setRegionZ1(int v) { regionZ1 = v; }
    public int getRegionX2() { return regionX2; }
    public void setRegionX2(int v) { regionX2 = v; }
    public int getRegionY2() { return regionY2; }
    public void setRegionY2(int v) { regionY2 = v; }
    public int getRegionZ2() { return regionZ2; }
    public void setRegionZ2(int v) { regionZ2 = v; }
}
