package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.World;

public abstract class BuildHelper {

    protected final World world;

    protected BuildHelper(World world) {
        this.world = world;
    }

    protected void fill(EditSession es,
                        int x1, int y1, int z1,
                        int x2, int y2, int z2, BlockType type) {
        if (type == null) return;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    set(es, x, y, z, type);
    }

    protected void block(EditSession es, int x, int y, int z, BlockType type) {
        set(es, x, y, z, type);
    }

    protected void disk(EditSession es, int cx, int y, int cz, int radius, BlockType type) {
        if (type == null) return;
        double r2 = (double) radius * radius;
        for (int x = cx - radius; x <= cx + radius; x++)
            for (int z = cz - radius; z <= cz + radius; z++) {
                double dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz <= r2) set(es, x, y, z, type);
            }
    }

    protected void ring(EditSession es, int cx, int y, int cz,
                        int inner, int outer, BlockType type) {
        if (type == null) return;
        double in2 = (double) inner * inner, out2 = (double) outer * outer;
        for (int x = cx - outer; x <= cx + outer; x++)
            for (int z = cz - outer; z <= cz + outer; z++) {
                double dx = x - cx, dz = z - cz, d2 = dx * dx + dz * dz;
                if (d2 >= in2 && d2 <= out2) set(es, x, y, z, type);
            }
    }

    protected void column(EditSession es, int x, int z, int yBottom, int yTop, BlockType type) {
        if (type == null) return;
        for (int y = Math.min(yBottom, yTop); y <= Math.max(yBottom, yTop); y++)
            set(es, x, y, z, type);
    }

    protected void sphere(EditSession es, int cx, int cy, int cz, int r, BlockType type) {
        if (type == null) return;
        double r2 = (double) r * r;
        for (int x = cx - r; x <= cx + r; x++)
            for (int y = cy - r; y <= cy + r; y++)
                for (int z = cz - r; z <= cz + r; z++) {
                    double dx = x - cx, dy = y - cy, dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2) set(es, x, y, z, type);
                }
    }

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

    private void set(EditSession es, int x, int y, int z, BlockType type) {
        if (type == null) return;
        try { es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState()); }
        catch (WorldEditException ignored) {}
    }

    protected static boolean inDisk(int x, int z, int r) {
        return (double) x * x + (double) z * z <= (double) r * r;
    }

    public abstract void build();
}
