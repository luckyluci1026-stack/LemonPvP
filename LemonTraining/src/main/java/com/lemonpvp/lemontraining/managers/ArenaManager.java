package com.lemonpvp.lemontraining.managers;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ArenaManager {

    private final LemonTraining plugin;
    private final List<BlockVector3> bowTargetPositions = new ArrayList<>();

    public ArenaManager(LemonTraining plugin) {
        this.plugin = plugin;
    }

    public void buildArenas() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (PracticeMode mode : PracticeMode.values()) {
                try {
                    buildArena(mode);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to build arena for " + mode, e);
                }
            }
        });
    }

    private void buildArena(PracticeMode mode) {
        String key = mode.name().toLowerCase();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("arenas." + key);
        if (sec == null) return;

        String worldName = sec.getString("world", "training_world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found, skipping arena build for " + mode);
            return;
        }

        int cx = sec.getInt("center-x", 0);
        int cy = sec.getInt("center-y", 64);
        int cz = sec.getInt("center-z", 0);

        // Check if floor block already exists
        Location center = new Location(world, cx, cy, cz);
        if (world.getBlockAt(cx, cy - 1, cz).getType() != org.bukkit.Material.AIR) {
            // Arena already built
            if (mode == PracticeMode.BOW) {
                populateBowTargets(sec, cx, cy, cz);
            }
            return;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld).maxBlocks(-1).build()) {

            switch (mode) {
                case TOTEM -> buildTotemArena(editSession, cx, cy, cz, sec.getInt("size", 20));
                case BOW -> buildBowArena(editSession, cx, cy, cz, sec, worldName);
                case MACE -> buildMaceArena(editSession, cx, cy, cz, sec.getInt("size", 20));
                case SWORD -> buildSwordArena(editSession, cx, cy, cz, sec.getInt("size", 20));
                case CRYSTAL -> buildCrystalArena(editSession, cx, cy, cz, sec.getInt("size", 20));
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error building " + mode + " arena", e);
        }

        if (mode == PracticeMode.BOW) {
            populateBowTargets(sec, cx, cy, cz);
        }
    }

    // --- Totem Arena: 20x20 floor + 3-high walls, hollow ---
    private void buildTotemArena(EditSession es, int cx, int cy, int cz, int size) throws Exception {
        int half = size / 2;
        int floorY = cy - 1;

        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                es.setBlock(BlockVector3.at(x, floorY, z),
                        BlockTypes.STONE_BRICKS.getDefaultState());
            }
        }

        // Walls: north/south (z edges), east/west (x edges), 3 high
        for (int wallY = cy; wallY <= cy + 2; wallY++) {
            for (int x = cx - half; x <= cx + half; x++) {
                es.setBlock(BlockVector3.at(x, wallY, cz - half),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
                es.setBlock(BlockVector3.at(x, wallY, cz + half),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
            }
            for (int z = cz - half + 1; z <= cz + half - 1; z++) {
                es.setBlock(BlockVector3.at(cx - half, wallY, z),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
                es.setBlock(BlockVector3.at(cx + half, wallY, z),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
            }
        }
    }

    // --- Bow Arena: 30x10 shooting range with TARGET blocks ---
    private void buildBowArena(EditSession es, int cx, int cy, int cz,
                                ConfigurationSection sec, String worldName) throws Exception {
        int sizeX = sec.getInt("size-x", 30);
        int sizeZ = sec.getInt("size-z", 10);
        int halfX = sizeX / 2;
        int halfZ = sizeZ / 2;
        int floorY = cy - 1;

        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                es.setBlock(BlockVector3.at(x, floorY, z),
                        BlockTypes.SMOOTH_STONE.getDefaultState());
            }
        }

        // Target blocks along the far end at various heights
        // Targets placed at x offsets from cx-halfX+10 through cx-halfX+25, at z=cz+halfZ-1
        int targetZ = cz + halfZ - 2;
        int[] xOffsets = {cx - halfX + 5, cx - halfX + 10, cx - halfX + 15, cx - halfX + 20, cx - halfX + 25};
        int[] yOffsets = {cy + 1, cy + 3, cy + 5};

        bowTargetPositions.clear();
        for (int xo : xOffsets) {
            for (int yo : yOffsets) {
                es.setBlock(BlockVector3.at(xo, yo, targetZ),
                        BlockTypes.TARGET.getDefaultState());
                bowTargetPositions.add(BlockVector3.at(xo, yo, targetZ));
            }
        }
    }

    private void populateBowTargets(ConfigurationSection sec, int cx, int cy, int cz) {
        int sizeX = sec.getInt("size-x", 30);
        int sizeZ = sec.getInt("size-z", 10);
        int halfX = sizeX / 2;
        int halfZ = sizeZ / 2;
        int targetZ = cz + halfZ - 2;
        int[] xOffsets = {cx - halfX + 5, cx - halfX + 10, cx - halfX + 15, cx - halfX + 20, cx - halfX + 25};
        int[] yOffsets = {cy + 1, cy + 3, cy + 5};

        bowTargetPositions.clear();
        for (int xo : xOffsets) {
            for (int yo : yOffsets) {
                bowTargetPositions.add(BlockVector3.at(xo, yo, targetZ));
            }
        }
    }

    // --- Mace Arena: 20x20 with quartz floor + 3 platforms ---
    private void buildMaceArena(EditSession es, int cx, int cy, int cz, int size) throws Exception {
        int half = size / 2;
        int floorY = cy - 1;

        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                es.setBlock(BlockVector3.at(x, floorY, z),
                        BlockTypes.QUARTZ_BLOCK.getDefaultState());
            }
        }

        // Three 5x5 platforms at heights y+5, y+10, y+15
        int[][] platformOffsets = {{-5, -5}, {0, 5}, {5, -3}};
        int[] heights = {cy + 5, cy + 10, cy + 15};

        for (int i = 0; i < 3; i++) {
            int px = cx + platformOffsets[i][0];
            int pz = cz + platformOffsets[i][1];
            int py = heights[i];
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    es.setBlock(BlockVector3.at(px + dx, py, pz + dz),
                            BlockTypes.QUARTZ_BLOCK.getDefaultState());
                }
            }
        }
    }

    // --- Sword Arena: 20x20 floor + 3-high stone walls ---
    private void buildSwordArena(EditSession es, int cx, int cy, int cz, int size) throws Exception {
        int half = size / 2;
        int floorY = cy - 1;

        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                es.setBlock(BlockVector3.at(x, floorY, z),
                        BlockTypes.STONE.getDefaultState());
            }
        }

        for (int wallY = cy; wallY <= cy + 2; wallY++) {
            for (int x = cx - half; x <= cx + half; x++) {
                es.setBlock(BlockVector3.at(x, wallY, cz - half),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
                es.setBlock(BlockVector3.at(x, wallY, cz + half),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
            }
            for (int z = cz - half + 1; z <= cz + half - 1; z++) {
                es.setBlock(BlockVector3.at(cx - half, wallY, z),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
                es.setBlock(BlockVector3.at(cx + half, wallY, z),
                        BlockTypes.STONE_BRICK_WALL.getDefaultState());
            }
        }
    }

    // --- Crystal Arena: 20x20 obsidian floor + 3-high walls ---
    private void buildCrystalArena(EditSession es, int cx, int cy, int cz, int size) throws Exception {
        int half = size / 2;
        int floorY = cy - 1;

        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                es.setBlock(BlockVector3.at(x, floorY, z),
                        BlockTypes.OBSIDIAN.getDefaultState());
            }
        }

        for (int wallY = cy; wallY <= cy + 2; wallY++) {
            for (int x = cx - half; x <= cx + half; x++) {
                es.setBlock(BlockVector3.at(x, wallY, cz - half),
                        BlockTypes.OBSIDIAN.getDefaultState());
                es.setBlock(BlockVector3.at(x, wallY, cz + half),
                        BlockTypes.OBSIDIAN.getDefaultState());
            }
            for (int z = cz - half + 1; z <= cz + half - 1; z++) {
                es.setBlock(BlockVector3.at(cx - half, wallY, z),
                        BlockTypes.OBSIDIAN.getDefaultState());
                es.setBlock(BlockVector3.at(cx + half, wallY, z),
                        BlockTypes.OBSIDIAN.getDefaultState());
            }
        }
    }

    // --- Public API ---

    public Location getSpawnLocation(PracticeMode mode) {
        String key = mode.name().toLowerCase();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("arenas." + key);
        if (sec == null) return null;

        String worldName = sec.getString("world", "training_world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = sec.getDouble("center-x", 0);
        double y = sec.getDouble("center-y", 64);
        double z = sec.getDouble("center-z", 0);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public boolean isInsideArena(Location loc, PracticeMode mode) {
        String key = mode.name().toLowerCase();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("arenas." + key);
        if (sec == null) return false;

        String worldName = sec.getString("world", "training_world");
        World world = Bukkit.getWorld(worldName);
        if (world == null || !world.equals(loc.getWorld())) return false;

        double cx = sec.getDouble("center-x", 0);
        double cz = sec.getDouble("center-z", 0);

        double halfX, halfZ;
        if (mode == PracticeMode.BOW) {
            halfX = sec.getInt("size-x", 30) / 2.0;
            halfZ = sec.getInt("size-z", 10) / 2.0;
        } else {
            double half = sec.getInt("size", 20) / 2.0;
            halfX = half;
            halfZ = half;
        }

        return loc.getX() >= cx - halfX - 1 && loc.getX() <= cx + halfX + 1
                && loc.getZ() >= cz - halfZ - 1 && loc.getZ() <= cz + halfZ + 1;
    }

    public List<BlockVector3> getBowTargetPositions() {
        return new ArrayList<>(bowTargetPositions);
    }
}
