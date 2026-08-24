package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The geometry primitives the builders are written against, backed by {@link LobbyCanvas}
 * instead of a WorldEdit {@code EditSession}.
 *
 * <p>Mirrors {@link BuildHelper} method-for-method so a builder can be moved across by changing
 * only its types. {@link BuildHelper} stays as-is for the WorldEdit-based arena builders.
 *
 * <p>Blocks are looked up by name at runtime through {@link #mat(String)} rather than referenced
 * as {@code Material.X} constants. That deliberately preserves the old behaviour — WorldEdit's
 * {@code BlockTypes.X} also yielded null for a block this server version doesn't know, and every
 * primitive below already treats null as "skip". It also means a block renamed between Minecraft
 * versions degrades to a gap instead of failing the whole build.
 */
public abstract class VanillaBuildHelper {

    protected final LemonPractice plugin;
    protected final org.bukkit.World world;

    /** Blocks renamed across versions: try each name in order. */
    private static final Map<String, String[]> ALIASES = Map.of(
            // "chain" became "iron_chain" when copper chains were added.
            "CHAIN", new String[]{"CHAIN", "IRON_CHAIN"}
    );

    private static final Map<String, Material> RESOLVED = new HashMap<>();
    private static final Set<String> UNRESOLVED = new LinkedHashSet<>();

    public VanillaBuildHelper(LemonPractice plugin, org.bukkit.World world) {
        this.plugin = plugin;
        this.world = world;
    }

    /**
     * Resolves a vanilla block name to a {@link Material}, or null when this server version
     * has no such block. Results are cached, so the per-call cost is a map lookup.
     */
    protected static Material mat(String name) {
        Material cached = RESOLVED.get(name);
        if (cached != null) return cached;
        if (UNRESOLVED.contains(name)) return null;
        for (String candidate : ALIASES.getOrDefault(name, new String[]{name})) {
            Material m = Material.getMaterial(candidate);
            if (m != null && m.isBlock()) {
                RESOLVED.put(name, m);
                return m;
            }
        }
        UNRESOLVED.add(name);
        return null;
    }

    /** Block names this server version did not recognise, for a post-build report. */
    protected static Set<String> unresolvedNames() {
        return java.util.Collections.unmodifiableSet(UNRESOLVED);
    }

    /** Fill a solid 3D box. */
    protected void fill(LobbyCanvas c, int x1, int y1, int z1,
                        int x2, int y2, int z2, Material type) {
        if (type == null) return;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    c.set(x, y, z, type);
    }

    /** Fill a hollow 3D box. */
    protected void fillHollow(LobbyCanvas c, int x1, int y1, int z1,
                              int x2, int y2, int z2,
                              Material wall, Material air) {
        if (wall == null) return;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    boolean onWall = x == minX || x == maxX || y == minY
                                  || y == maxY || z == minZ || z == maxZ;
                    Material t = onWall ? wall : air;
                    if (t == null) continue;
                    c.set(x, y, z, t);
                }
    }

    /** Place a single block (null-safe). */
    protected void block(LobbyCanvas c, int x, int y, int z, Material type) {
        c.set(x, y, z, type);
    }

    /** Place a block using a full Minecraft block-state string, e.g.
     *  {@code "oak_stairs[facing=north,half=bottom]"}. */
    protected void blockState(LobbyCanvas c, int x, int y, int z, String stateString) {
        c.setState(x, y, z, stateString);
    }

    /** Filled sphere centred at (cx, cy, cz). */
    protected void sphere(LobbyCanvas c, int cx, int cy, int cz, int r, Material type) {
        if (type == null) return;
        double r2 = (double) r * r;
        for (int x = cx - r; x <= cx + r; x++)
            for (int y = cy - r; y <= cy + r; y++)
                for (int z = cz - r; z <= cz + r; z++) {
                    double dx = x - cx, dy = y - cy, dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2) c.set(x, y, z, type);
                }
    }

    /** Vertical column from yBottom to yTop inclusive. */
    protected void column(LobbyCanvas c, int x, int z, int yBottom, int yTop, Material type) {
        if (type == null) return;
        int lo = Math.min(yBottom, yTop), hi = Math.max(yBottom, yTop);
        for (int y = lo; y <= hi; y++) c.set(x, y, z, type);
    }

    /** Filled horizontal disk at Y, centred at (cx, cz) with given radius. */
    protected void disk(LobbyCanvas c, int cx, int y, int cz, int radius, Material type) {
        if (type == null) return;
        double r2 = (double) radius * radius;
        for (int x = cx - radius; x <= cx + radius; x++)
            for (int z = cz - radius; z <= cz + radius; z++) {
                double dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz <= r2) c.set(x, y, z, type);
            }
    }

    /** Hollow ring (annulus) at Y. inner and outer are radii. */
    protected void ring(LobbyCanvas c, int cx, int y, int cz,
                        int inner, int outer, Material type) {
        if (type == null) return;
        double in2 = (double) inner * inner, out2 = (double) outer * outer;
        for (int x = cx - outer; x <= cx + outer; x++)
            for (int z = cz - outer; z <= cz + outer; z++) {
                double dx = x - cx, dz = z - cz, d2 = dx * dx + dz * dz;
                if (d2 >= in2 && d2 <= out2) c.set(x, y, z, type);
            }
    }

    public abstract void build();
}
