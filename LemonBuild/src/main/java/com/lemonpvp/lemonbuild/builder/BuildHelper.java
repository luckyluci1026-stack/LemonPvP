package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Powerful, string-based geometry toolkit for programmatic WorldEdit/FAWE builds.
 *
 * <p>Every placement takes a Minecraft block id (no {@code minecraft:} prefix
 * needed) which may carry block-states, e.g. {@code "oak_log[axis=y]"},
 * {@code "oak_stairs[facing=north,half=top]"}, {@code "lantern[hanging=true]"}.
 * Parsed states are cached, so reusing the same id string is cheap even across
 * millions of blocks.
 *
 * <p>All methods are null/parse-safe: an unknown id is parsed once, logged once,
 * then skipped silently for the rest of the build.
 */
public abstract class BuildHelper {

    protected final World world;
    private com.sk89q.worldedit.world.World weWorld;
    private final Map<String, BlockStateHolder<?>> cache = new HashMap<>();
    private final java.util.Set<String> warned = new java.util.HashSet<>();

    protected BuildHelper(World world) {
        this.world = world;
    }

    /** Lazily-adapted WE world handle (set once a build starts). */
    protected com.sk89q.worldedit.world.World weWorld() {
        if (weWorld == null) weWorld = BukkitAdapter.adapt(world);
        return weWorld;
    }

    // ── Core placement ──────────────────────────────────────────────────────

    /** Place a single block by id (state string supported). */
    protected void set(EditSession es, int x, int y, int z, String id) {
        BlockStateHolder<?> state = parse(id);
        if (state == null) return;
        try { es.setBlock(BlockVector3.at(x, y, z), state); }
        catch (WorldEditException ignored) {}
    }

    private BlockStateHolder<?> parse(String id) {
        if (id == null) return null;
        BlockStateHolder<?> cached = cache.get(id);
        if (cached != null) return cached;
        if (warned.contains(id)) return null;
        try {
            ParserContext ctx = new ParserContext();
            ctx.setWorld(weWorld());
            ctx.setRestricted(false);
            ctx.setTryLegacy(true);
            BlockStateHolder<?> state = WorldEdit.getInstance()
                    .getBlockFactory().parseFromInput(id, ctx).toImmutableState();
            cache.put(id, state);
            return state;
        } catch (Exception e) {
            warned.add(id);
            return null;
        }
    }

    // ── Boxes ───────────────────────────────────────────────────────────────

    /** Solid filled box (inclusive bounds). */
    protected void fill(EditSession es, int x1, int y1, int z1,
                        int x2, int y2, int z2, String id) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    set(es, x, y, z, id);
    }

    /** Hollow box shell (all 6 faces). */
    protected void hollowBox(EditSession es, int x1, int y1, int z1,
                             int x2, int y2, int z2, String id) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ)
                        set(es, x, y, z, id);
                }
    }

    /** Four vertical walls only (no floor/ceiling). */
    protected void walls(EditSession es, int x1, int y1, int z1,
                         int x2, int y2, int z2, String id) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) { set(es, x, y, minZ, id); set(es, x, y, maxZ, id); }
            for (int z = minZ; z <= maxZ; z++) { set(es, minX, y, z, id); set(es, maxX, y, z, id); }
        }
    }

    // ── Columns & lines ─────────────────────────────────────────────────────

    /** Vertical column (inclusive). */
    protected void column(EditSession es, int x, int z, int y0, int y1, String id) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) set(es, x, y, z, id);
    }

    /** 1-thick 3D line (Bresenham). */
    protected void line(EditSession es, int x0, int y0, int z0,
                        int x1, int y1, int z1, String id) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, sz = z0 < z1 ? 1 : -1;
        int dm = Math.max(dx, Math.max(dy, dz));
        int x = x0, y = y0, z = z0;
        int ex = dm / 2, ey = dm / 2, ez = dm / 2;
        for (int i = 0; i <= dm; i++) {
            set(es, x, y, z, id);
            ex -= dx; if (ex < 0) { ex += dm; x += sx; }
            ey -= dy; if (ey < 0) { ey += dm; y += sy; }
            ez -= dz; if (ez < 0) { ez += dm; z += sz; }
        }
    }

    /** Thick 3D line built from spheres of radius {@code r} along the path. */
    protected void thickLine(EditSession es, int x0, int y0, int z0,
                             int x1, int y1, int z1, double r, String id) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, sz = z0 < z1 ? 1 : -1;
        int dm = Math.max(dx, Math.max(dy, dz));
        int x = x0, y = y0, z = z0;
        int ex = dm / 2, ey = dm / 2, ez = dm / 2;
        for (int i = 0; i <= dm; i++) {
            sphere(es, x, y, z, r, id);
            ex -= dx; if (ex < 0) { ex += dm; x += sx; }
            ey -= dy; if (ey < 0) { ey += dm; y += sy; }
            ez -= dz; if (ez < 0) { ez += dm; z += sz; }
        }
    }

    // ── Disks, rings, cylinders ─────────────────────────────────────────────

    /** Filled horizontal disk at height y. */
    protected void disk(EditSession es, int cx, int y, int cz, double r, String id) {
        double r2 = r * r;
        int ri = (int) Math.ceil(r);
        for (int x = cx - ri; x <= cx + ri; x++)
            for (int z = cz - ri; z <= cz + ri; z++) {
                double dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz <= r2) set(es, x, y, z, id);
            }
    }

    /** Horizontal annulus (ring) at height y, between radii inner..outer. */
    protected void ring(EditSession es, int cx, int y, int cz,
                        double inner, double outer, String id) {
        double in2 = inner * inner, out2 = outer * outer;
        int ro = (int) Math.ceil(outer);
        for (int x = cx - ro; x <= cx + ro; x++)
            for (int z = cz - ro; z <= cz + ro; z++) {
                double dx = x - cx, dz = z - cz, d2 = dx * dx + dz * dz;
                if (d2 >= in2 && d2 <= out2) set(es, x, y, z, id);
            }
    }

    /** Filled vertical cylinder, base at y0, given height. */
    protected void cyl(EditSession es, int cx, int y0, int cz, double r, int height, String id) {
        for (int h = 0; h < height; h++) disk(es, cx, y0 + h, cz, r, id);
    }

    /** Hollow vertical cylinder (tube) between radii inner..outer. */
    protected void tube(EditSession es, int cx, int y0, int cz,
                        double inner, double outer, int height, String id) {
        for (int h = 0; h < height; h++) ring(es, cx, y0 + h, cz, inner, outer, id);
    }

    // ── Spheres & ellipsoids ────────────────────────────────────────────────

    /** Filled sphere. */
    protected void sphere(EditSession es, int cx, int cy, int cz, double r, String id) {
        double r2 = r * r;
        int ri = (int) Math.ceil(r);
        for (int x = cx - ri; x <= cx + ri; x++)
            for (int y = cy - ri; y <= cy + ri; y++)
                for (int z = cz - ri; z <= cz + ri; z++) {
                    double dx = x - cx, dy = y - cy, dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2) set(es, x, y, z, id);
                }
    }

    /** Hollow sphere shell of given wall thickness. */
    protected void hollowSphere(EditSession es, int cx, int cy, int cz,
                                double r, double thickness, String id) {
        double out2 = r * r, in = r - thickness, in2 = in * in;
        int ri = (int) Math.ceil(r);
        for (int x = cx - ri; x <= cx + ri; x++)
            for (int y = cy - ri; y <= cy + ri; y++)
                for (int z = cz - ri; z <= cz + ri; z++) {
                    double dx = x - cx, dy = y - cy, dz = z - cz, d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 <= out2 && d2 >= in2) set(es, x, y, z, id);
                }
    }

    /** Filled ellipsoid with independent radii. */
    protected void ellipsoid(EditSession es, int cx, int cy, int cz,
                             double rx, double ry, double rz, String id) {
        int rix = (int) Math.ceil(rx), riy = (int) Math.ceil(ry), riz = (int) Math.ceil(rz);
        for (int x = cx - rix; x <= cx + rix; x++)
            for (int y = cy - riy; y <= cy + riy; y++)
                for (int z = cz - riz; z <= cz + riz; z++) {
                    double dx = (x - cx) / rx, dy = (y - cy) / ry, dz = (z - cz) / rz;
                    if (dx * dx + dy * dy + dz * dz <= 1.0) set(es, x, y, z, id);
                }
    }

    // ── Cones & pyramids ────────────────────────────────────────────────────

    /** Cone, base at y0. {@code inverted} flips the taper (tip at bottom). */
    protected void cone(EditSession es, int cx, int y0, int cz,
                        double baseR, int height, boolean inverted, String id) {
        for (int h = 0; h < height; h++) {
            double t = (double) h / Math.max(1, height - 1);
            double r = inverted ? baseR * t : baseR * (1.0 - t);
            disk(es, cx, y0 + h, cz, Math.max(0, r), id);
        }
    }

    /** Square pyramid, base half-width {@code baseHalf} at y0. */
    protected void pyramid(EditSession es, int cx, int y0, int cz,
                           int baseHalf, boolean inverted, String id) {
        for (int h = 0; h <= baseHalf; h++) {
            int half = inverted ? h : (baseHalf - h);
            fill(es, cx - half, y0 + h, cz - half, cx + half, y0 + h, cz + half, id);
        }
    }

    // ── Deterministic scatter (no Math.random — reproducible builds) ─────────

    /**
     * Sprinkles ids across a disk at height y with given density (0..1).
     * Deterministic per (cx,cz,seed); rebuilding yields identical results.
     * Picks among the supplied ids round-robin by hash.
     */
    protected void scatter(EditSession es, int cx, int y, int cz,
                           double r, double density, long seed, String... ids) {
        if (ids.length == 0) return;
        double r2 = r * r;
        int ri = (int) Math.ceil(r);
        for (int x = cx - ri; x <= cx + ri; x++)
            for (int z = cz - ri; z <= cz + ri; z++) {
                double dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz > r2) continue;
                long h = hash(x, z, seed);
                double v = (h & 0xFFFF) / 65535.0;
                if (v < density) set(es, x, y, z, ids[(int) ((h >>> 16) % ids.length)]);
            }
    }

    /** Deterministic 0..1 noise for a column (use for height variation etc.). */
    protected double noise(int x, int z, long seed) {
        return (hash(x, z, seed) & 0xFFFF) / 65535.0;
    }

    private long hash(int x, int z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 33); h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33); h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return h & 0x7FFFFFFFFFFFFFFFL;
    }

    public abstract void build();
}
