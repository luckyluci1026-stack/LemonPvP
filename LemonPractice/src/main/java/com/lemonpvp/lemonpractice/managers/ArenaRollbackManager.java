package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heals vanilla duel worlds after a match. While a duel runs in a world, the
 * {@link com.lemonpvp.lemonpractice.listeners.ArenaRollbackListener} records the
 * ORIGINAL block state of every block that changes (broken, placed, exploded,
 * burnt, liquid-flowed). {@link #restore(String)} puts them all back — so a
 * natural biome world stays reusable duel after duel without any schematic.
 *
 * <p>One duel per world (world == arena), so the log is keyed by world name.
 * A per-world cap bounds memory on pathological cases (huge TNT spam); once
 * exceeded, further changes in that world aren't tracked and a warning is
 * logged (the arena still resets what it captured up to the cap).</p>
 */
public class ArenaRollbackManager {

    /** Max tracked block changes per world before we stop recording (memory guard). */
    private static final int MAX_PER_WORLD = 60_000;

    private final LemonPractice plugin;
    /** world name → (packed block position → original BlockData). LinkedHashMap keeps insertion order. */
    private final Map<String, Map<Long, BlockData>> logs = new ConcurrentHashMap<>();
    private final java.util.Set<String> capped = ConcurrentHashMap.newKeySet();

    public ArenaRollbackManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    private static long key(int x, int y, int z) {
        // Pack into a long: 26 bits x, 12 bits y, 26 bits z (world coords fit easily).
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    /** True if this world is being tracked for rollback (a vanilla arena world). */
    public boolean isTracked(String worldName) {
        return logs.containsKey(worldName);
    }

    /** Starts (or clears) tracking for a world. Called when a vanilla arena is registered. */
    public void track(String worldName) {
        logs.put(worldName, new LinkedHashMap<>());
        capped.remove(worldName);
    }

    /**
     * Records the original state of a block that is ABOUT to change. Call BEFORE
     * the change is applied. No-op if the world isn't tracked or the block's
     * position was already recorded (we keep the earliest = truly-original state).
     */
    public void record(Block block) {
        record(block, block.getBlockData());
    }

    /**
     * Records an explicit original state for a block's position — used when the
     * block has ALREADY changed by the time we observe it (e.g. BlockPlaceEvent,
     * where {@code block.getBlockData()} is the newly-placed block and the real
     * original comes from {@code getBlockReplacedState()}).
     */
    public void record(Block block, BlockData original) {
        String world = block.getWorld().getName();
        Map<Long, BlockData> log = logs.get(world);
        if (log == null || capped.contains(world)) return;
        long k = key(block.getX(), block.getY(), block.getZ());
        if (log.containsKey(k)) return; // keep the earliest = truly-original state
        if (log.size() >= MAX_PER_WORLD) {
            capped.add(world);
            plugin.getLogger().warning("[ArenaRollback] " + world + " hit the "
                    + MAX_PER_WORLD + "-block cap — remaining changes this match won't roll back.");
            return;
        }
        log.put(k, original);
    }

    /**
     * Restores every recorded block in the world to its original state, then
     * clears the log (world stays tracked for the next duel). Main-thread only.
     */
    public void restore(String worldName) {
        Map<Long, BlockData> log = logs.get(worldName);
        if (log == null || log.isEmpty()) { capped.remove(worldName); return; }
        World world = Bukkit.getWorld(worldName);
        if (world == null) { log.clear(); return; }

        // Snapshot + clear first so any events fired by our own setBlockData
        // (shouldn't, since we suppress physics) can't re-enter and re-record.
        Map<Long, BlockData> snapshot = new LinkedHashMap<>(log);
        log.clear();
        capped.remove(worldName);

        int restored = 0;
        for (Map.Entry<Long, BlockData> e : snapshot.entrySet()) {
            long k = e.getKey();
            int x = (int) ((k >>> 38) & 0x3FFFFFF);
            int y = (int) ((k >>> 26) & 0xFFF);
            int z = (int) (k & 0x3FFFFFF);
            // Sign-extend: x/z from bit 25 (26-bit), y from bit 11 (12-bit, Y can be -64).
            x = (x << 6) >> 6;
            z = (z << 6) >> 6;
            y = (y << 20) >> 20;
            Location loc = new Location(world, x, y, z);
            loc.getBlock().setBlockData(e.getValue(), false); // false = no physics cascade
            restored++;
        }
        if (restored > 0) {
            plugin.getLogger().fine("[ArenaRollback] Restored " + restored + " blocks in " + worldName);
        }
    }

    /** Stops tracking a world entirely (plugin disable). */
    public void untrack(String worldName) {
        logs.remove(worldName);
        capped.remove(worldName);
    }

    public void shutdown() {
        for (String w : new java.util.ArrayList<>(logs.keySet())) {
            restore(w);
            untrack(w);
        }
    }
}
