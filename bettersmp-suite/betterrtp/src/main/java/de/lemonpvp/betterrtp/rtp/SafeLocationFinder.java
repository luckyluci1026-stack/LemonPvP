package de.lemonpvp.betterrtp.rtp;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Findet vollständig asynchron eine sichere Zufalls-Position.
 *
 * Ablauf pro Versuch:
 *  1. Zufallspunkt im Ring [min,max] um das Zentrum bestimmen
 *  2. Chunk asynchron laden (Paper: getChunkAtAsync -> IO/Gen off-main)
 *  3. ChunkSnapshot auf dem Main-Thread ziehen (günstig)
 *  4. Snapshot im ForkJoinPool scannen (thread-sicher, keine Bukkit-Welt-Zugriffe)
 *  5. Treffer -> Location; sonst nächster Versuch
 *
 * So bleibt der Main-Thread frei von schwerer Block-Iteration (Ziel: 20 TPS).
 */
public final class SafeLocationFinder {

    /** Boden-Materialien, auf denen man NICHT sicher landet. */
    private static final Set<Material> UNSAFE_FLOOR = EnumSet.of(
            Material.LAVA, Material.WATER, Material.FIRE, Material.SOUL_FIRE,
            Material.CACTUS, Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW, Material.WITHER_ROSE,
            Material.POINTED_DRIPSTONE, Material.COBWEB);

    private static final int INVALID = Integer.MIN_VALUE;

    private final Plugin plugin;

    public SafeLocationFinder(Plugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Location> find(World world, Profile profile, int maxAttempts) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        attempt(world, profile, maxAttempts, result);
        return result;
    }

    private void attempt(World world, Profile p, int remaining, CompletableFuture<Location> result) {
        if (remaining <= 0) {
            result.complete(null);
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble() * Math.PI * 2;
        // sqrt für flächengleiche Verteilung im Ring
        double t = rng.nextDouble();
        double dist = Math.sqrt(t * (p.maxRadius() * (double) p.maxRadius()
                - p.minRadius() * (double) p.minRadius()) + p.minRadius() * (double) p.minRadius());
        int x = p.centerX() + (int) Math.round(Math.cos(angle) * dist);
        int z = p.centerZ() + (int) Math.round(Math.sin(angle) * dist);

        if (p.respectWorldborder() && !insideBorder(world, x, z)) {
            retry(world, p, remaining, result);
            return;
        }

        world.getChunkAtAsync(x >> 4, z >> 4, true).thenAccept(chunk -> {
            // Main-Thread: günstigen Snapshot ziehen
            ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, true, false);
            final int fx = x;
            final int fz = z;
            CompletableFuture.supplyAsync(() -> scan(snapshot, world, fx, fz, p))
                    .thenAccept(loc -> {
                        if (loc != null) {
                            result.complete(loc);
                        } else {
                            retry(world, p, remaining, result);
                        }
                    });
        }).exceptionally(ex -> {
            retry(world, p, remaining, result);
            return null;
        });
    }

    private void retry(World world, Profile p, int remaining, CompletableFuture<Location> result) {
        plugin.getServer().getScheduler().runTask(plugin,
                () -> attempt(world, p, remaining - 1, result));
    }

    private boolean insideBorder(World world, int x, int z) {
        var border = world.getWorldBorder();
        double half = border.getSize() / 2.0 - 8;
        double cx = border.getCenter().getX();
        double cz = border.getCenter().getZ();
        return Math.abs(x - cx) <= half && Math.abs(z - cz) <= half;
    }

    /** Reine CPU-Arbeit auf dem Snapshot - thread-sicher, ohne Bukkit-Welt-Zugriff. */
    private Location scan(ChunkSnapshot snap, World world, int worldX, int worldZ, Profile p) {
        int lx = worldX & 15;
        int lz = worldZ & 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        int floorY = (world.getEnvironment() == World.Environment.NETHER)
                ? findNetherFloor(snap, lx, lz, minY, maxY)
                : snap.getHighestBlockYAt(lx, lz);

        if (floorY == INVALID || floorY <= minY || floorY >= maxY - 2) {
            return null;
        }

        Material floor = snap.getBlockType(lx, floorY, lz);
        if (!floor.isSolid() || UNSAFE_FLOOR.contains(floor)) {
            return null;
        }
        if (!passable(snap.getBlockType(lx, floorY + 1, lz))
                || !passable(snap.getBlockType(lx, floorY + 2, lz))) {
            return null;
        }
        if (!p.blacklistBiomes().isEmpty()) {
            String biome = biomeKey(snap, lx, floorY, lz);
            if (biome != null && p.blacklistBiomes().contains(biome)) {
                return null;
            }
        }
        return new Location(world, worldX + 0.5, floorY + 1, worldZ + 0.5);
    }

    private int findNetherFloor(ChunkSnapshot snap, int lx, int lz, int minY, int maxY) {
        int start = Math.min(maxY - 3, 120);
        for (int y = start; y > minY + 1; y--) {
            Material floor = snap.getBlockType(lx, y, lz);
            if (floor.isSolid() && !UNSAFE_FLOOR.contains(floor)
                    && passable(snap.getBlockType(lx, y + 1, lz))
                    && passable(snap.getBlockType(lx, y + 2, lz))) {
                return y;
            }
        }
        return INVALID;
    }

    private boolean passable(Material material) {
        return !material.isSolid() && material != Material.LAVA && material != Material.WATER;
    }

    private String biomeKey(ChunkSnapshot snap, int lx, int y, int lz) {
        try {
            return snap.getBiome(lx, y, lz).getKey().getKey();
        } catch (Throwable t) {
            return null;
        }
    }
}
