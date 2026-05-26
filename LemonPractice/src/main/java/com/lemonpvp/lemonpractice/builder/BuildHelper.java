package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;

public abstract class BuildHelper {

    protected final LemonPractice plugin;
    protected final org.bukkit.World world;

    public BuildHelper(LemonPractice plugin, org.bukkit.World world) {
        this.plugin = plugin;
        this.world = world;
    }

    /** Fill a solid 3D box. */
    protected void fill(EditSession es, int x1, int y1, int z1,
                        int x2, int y2, int z2, BlockType type) {
        if (type == null) return;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    try { es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState()); }
                    catch (WorldEditException ignored) {}
    }

    /** Fill a hollow 3D box. */
    protected void fillHollow(EditSession es, int x1, int y1, int z1,
                              int x2, int y2, int z2,
                              BlockType wall, BlockType air) {
        if (wall == null) return;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = x == minX || x == maxX || y == minY
                                  || y == maxY || z == minZ || z == maxZ;
                    BlockType t = onWall ? wall : air;
                    if (t == null) continue;
                    try { es.setBlock(BlockVector3.at(x, y, z), t.getDefaultState()); }
                    catch (WorldEditException ignored) {}
                }
    }

    /** Place a single block (null-safe). */
    protected void block(EditSession es, int x, int y, int z, BlockType type) {
        if (type == null) return;
        try { es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState()); }
        catch (WorldEditException ignored) {}
    }

    /** Place a block using a full Minecraft block-state string, e.g.
     *  {@code "oak_stairs[facing=north,half=bottom]"}. */
    protected void blockState(EditSession es, int x, int y, int z, String stateString) {
        try {
            ParserContext ctx = new ParserContext();
            ctx.setWorld(BukkitAdapter.adapt(world));
            ctx.setRestricted(false);
            BlockStateHolder<?> state = WorldEdit.getInstance()
                    .getBlockFactory().parseFromInput(stateString, ctx);
            es.setBlock(BlockVector3.at(x, y, z), state);
        } catch (Exception ignored) {}
    }

    /** Filled sphere centred at (cx, cy, cz). */
    protected void sphere(EditSession es, int cx, int cy, int cz, int r, BlockType type) {
        if (type == null) return;
        double r2 = (double) r * r;
        for (int x = cx - r; x <= cx + r; x++)
            for (int y = cy - r; y <= cy + r; y++)
                for (int z = cz - r; z <= cz + r; z++) {
                    double dx = x-cx, dy = y-cy, dz = z-cz;
                    if (dx*dx + dy*dy + dz*dz <= r2)
                        try { es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState()); }
                        catch (WorldEditException ignored) {}
                }
    }

    /** Vertical column from yBottom to yTop inclusive. */
    protected void column(EditSession es, int x, int z, int yBottom, int yTop, BlockType type) {
        if (type == null) return;
        int lo = Math.min(yBottom, yTop), hi = Math.max(yBottom, yTop);
        for (int y = lo; y <= hi; y++)
            try { es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState()); }
            catch (WorldEditException ignored) {}
    }

    /** Filled horizontal disk at Y, centred at (cx, cz) with given radius. */
    protected void disk(EditSession es, int cx, int y, int cz, int radius, BlockType type) {
        if (type == null) return;
        double r2 = (double) radius * radius;
        for (int x = cx - radius; x <= cx + radius; x++)
            for (int z = cz - radius; z <= cz + radius; z++) {
                double dx = x - cx, dz = z - cz;
                if (dx*dx + dz*dz <= r2) block(es, x, y, z, type);
            }
    }

    /** Hollow ring (annulus) at Y. inner and outer are radii. */
    protected void ring(EditSession es, int cx, int y, int cz,
                        int inner, int outer, BlockType type) {
        if (type == null) return;
        double in2 = (double) inner * inner, out2 = (double) outer * outer;
        for (int x = cx - outer; x <= cx + outer; x++)
            for (int z = cz - outer; z <= cz + outer; z++) {
                double dx = x - cx, dz = z - cz, d2 = dx*dx + dz*dz;
                if (d2 >= in2 && d2 <= out2) block(es, x, y, z, type);
            }
    }

    public abstract void build();
}
