package com.lemonpvp.lemonpractice.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class Arena {
    private int id;
    private String name;
    private String worldName;
    private Location spawn1;
    private Location spawn2;
    private Location spawnSpec;
    private int regionX1, regionY1, regionZ1;
    private int regionX2, regionY2, regionZ2;
    private boolean active = true;
    private boolean inUse = false;
    /** True for config-defined vanilla biome worlds: no schematic, reset = block rollback, not persisted to DB. */
    private boolean vanilla = false;
    private String schematicPath;
    private String dupeGroup;
    private final List<String> boundGamemodes = new ArrayList<>();

    public Arena(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public Location getSpawnSpec() { return spawnSpec; }
    public void setSpawnSpec(Location spawnSpec) { this.spawnSpec = spawnSpec; }
    public int getRegionX1() { return regionX1; }
    public void setRegionX1(int v) { this.regionX1 = v; }
    public int getRegionY1() { return regionY1; }
    public void setRegionY1(int v) { this.regionY1 = v; }
    public int getRegionZ1() { return regionZ1; }
    public void setRegionZ1(int v) { this.regionZ1 = v; }
    public int getRegionX2() { return regionX2; }
    public void setRegionX2(int v) { this.regionX2 = v; }
    public int getRegionY2() { return regionY2; }
    public void setRegionY2(int v) { this.regionY2 = v; }
    public int getRegionZ2() { return regionZ2; }
    public void setRegionZ2(int v) { this.regionZ2 = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
    public boolean isVanilla() { return vanilla; }
    public void setVanilla(boolean vanilla) { this.vanilla = vanilla; }
    public String getSchematicPath() { return schematicPath; }
    public void setSchematicPath(String schematicPath) { this.schematicPath = schematicPath; }
    public String getDupeGroup() { return dupeGroup; }
    public void setDupeGroup(String dupeGroup) { this.dupeGroup = dupeGroup; }
    public List<String> getBoundGamemodes() { return boundGamemodes; }
    public boolean isBoundTo(String gamemode) { return boundGamemodes.contains(gamemode.toLowerCase()); }
    public void addGamemodeBind(String gamemode) { if (!boundGamemodes.contains(gamemode.toLowerCase())) boundGamemodes.add(gamemode.toLowerCase()); }

    public boolean isFullyConfigured() {
        return spawn1 != null && spawn2 != null && worldName != null;
    }
}
