package com.lemonpvp.lemonpractice.builder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.EnumMap;
import java.util.Map;

/**
 * A plain-Bukkit block writer, used in place of a WorldEdit {@code EditSession}.
 *
 * <p>The builders were written against FastAsyncWorldEdit, which cannot load on this network
 * (its jars are compiled for a newer Java than the servers run). This writes the same blocks
 * through the vanilla API instead, so the lobby can actually be built.
 *
 * <p>Two things keep it fast enough to be practical:
 * <ul>
 *   <li>physics are suppressed ({@code setBlockData(data, false)}) — without that, every
 *       placement would trigger neighbour updates and the build would take minutes;</li>
 *   <li>one {@link BlockData} instance is cached per {@link Material}, so a build placing
 *       millions of blocks allocates a handful of objects rather than millions.</li>
 * </ul>
 *
 * <p><b>Main thread only.</b> Bukkit block writes are not thread-safe; callers must not run a
 * build asynchronously.
 */
public final class LobbyCanvas {

    private final World world;
    private final Map<Material, BlockData> dataCache = new EnumMap<>(Material.class);
    private final int minY;
    private final int maxY;
    private long placed;
    private long skipped;

    public LobbyCanvas(World world) {
        this.world = world;
        this.minY = world.getMinHeight();
        this.maxY = world.getMaxHeight();
    }

    /** Places a block, ignoring nulls and anything outside the world's build height. */
    public void set(int x, int y, int z, Material type) {
        if (type == null) { skipped++; return; }
        if (y < minY || y >= maxY) { skipped++; return; }
        BlockData data = dataCache.computeIfAbsent(type, Material::createBlockData);
        world.getBlockAt(x, y, z).setBlockData(data, false);
        placed++;
    }

    /**
     * Places a block from a vanilla block-state string such as
     * {@code "oak_stairs[facing=north,half=bottom]"} — the same syntax WorldEdit accepted,
     * which {@link Bukkit#createBlockData(String)} parses natively.
     */
    public void setState(int x, int y, int z, String stateString) {
        if (stateString == null || stateString.isBlank()) { skipped++; return; }
        if (y < minY || y >= maxY) { skipped++; return; }
        try {
            world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(stateString), false);
            placed++;
        } catch (IllegalArgumentException e) {
            skipped++; // unknown block or malformed state — same silent skip WorldEdit gave us
        }
    }

    /** How many blocks were actually written. */
    public long placed() { return placed; }

    /** How many writes were skipped (unknown material, out of height range, bad state). */
    public long skipped() { return skipped; }

    public World world() { return world; }
}
