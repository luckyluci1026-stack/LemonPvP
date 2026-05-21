package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;

public abstract class BuildHelper {

    protected final LemonPractice plugin;
    protected final org.bukkit.World world;

    public BuildHelper(LemonPractice plugin, org.bukkit.World world) {
        this.plugin = plugin;
        this.world = world;
    }

    /**
     * Fill a solid 3D box with the given block type.
     */
    protected void fill(EditSession es, int x1, int y1, int z1,
                        int x2, int y2, int z2, BlockType type) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    try {
                        es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState());
                    } catch (WorldEditException e) {
                        // Suppress per-block exceptions to avoid aborting the whole build
                    }
                }
            }
        }
    }

    /**
     * Fill a hollow 3D box: outer shell uses wallType, interior is set to airType.
     * When the box dimensions are 1 in any axis the hollow logic still works correctly.
     */
    protected void fillHollow(EditSession es, int x1, int y1, int z1,
                              int x2, int y2, int z2,
                              BlockType wallType, BlockType airType) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = (x == minX || x == maxX
                            || y == minY || y == maxY
                            || z == minZ || z == maxZ);
                    BlockType place = onWall ? wallType : airType;
                    try {
                        es.setBlock(BlockVector3.at(x, y, z), place.getDefaultState());
                    } catch (WorldEditException e) {
                        // Suppress
                    }
                }
            }
        }
    }

    /**
     * Place a single block.
     */
    protected void block(EditSession es, int x, int y, int z, BlockType type) {
        try {
            es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState());
        } catch (WorldEditException e) {
            // Suppress
        }
    }

    /**
     * Place an approximate filled sphere centred at (cx, cy, cz) with the given radius.
     */
    protected void sphere(EditSession es, int cx, int cy, int cz, int radius, BlockType type) {
        double r2 = (double) radius * radius;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double dx = x - cx;
                    double dy = y - cy;
                    double dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2) {
                        try {
                            es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState());
                        } catch (WorldEditException e) {
                            // Suppress
                        }
                    }
                }
            }
        }
    }

    /**
     * Place a vertical column from yBottom to yTop (inclusive) at (x, z).
     */
    protected void column(EditSession es, int x, int z, int yBottom, int yTop, BlockType type) {
        int lo = Math.min(yBottom, yTop);
        int hi = Math.max(yBottom, yTop);
        for (int y = lo; y <= hi; y++) {
            try {
                es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState());
            } catch (WorldEditException e) {
                // Suppress
            }
        }
    }

    /**
     * Subclasses implement their structure construction here.
     */
    public abstract void build();
}
