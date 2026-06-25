package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.World;

/**
 * LobbySpawnBuilder — "LemonPvP Hub"
 *
 * <p>Erzeugt eine schwebende, kreisförmige Lobby-Insel zentriert bei
 * Weltmitte (0, 64, 0). Benötigt FastAsyncWorldEdit oder WorldEdit.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>Basalt-Körper (Y 58–63), Radius {@value #R}</li>
 *   <li>Polished-Blackstone-Bricks-Boden (Y 64)</li>
 *   <li>Smooth-Quartz-Kreuzwege (3 breit, N/S/E/W)</li>
 *   <li>Sea-Lantern-Gitter (alle 6 Blöcke) als Beleuchtung</li>
 *   <li>7×7 Quartz-Spawn-Plattform (Y 65) in der Mitte</li>
 *   <li>4 Quartz-Säulen (±4, ±4), Y 65–74 + Glas-Baldachin</li>
 *   <li>Portal-Tore an N/S/E/W (Abstand 28)</li>
 *   <li>Polished-Blackstone-Geländer am Rand</li>
 *   <li>4 Eichenbäume an den Diagonalen (±20, ±20)</li>
 * </ul>
 *
 * <h2>Portal-Koordinaten (Mittelpunkte)</h2>
 * <ul>
 *   <li>Nord (–Z):  (  0, 64, –28)</li>
 *   <li>Süd  (+Z):  (  0, 64, +28)</li>
 *   <li>Ost  (+X):  (+28, 64,   0)</li>
 *   <li>West (–X):  (–28, 64,   0)</li>
 * </ul>
 */
public class LobbySpawnBuilder extends BuildHelper {

    private static final int R  = 32;  // Insel-Radius
    private static final int SY = 64;  // Boden-Y

    public LobbySpawnBuilder(World world) {
        super(world);
    }

    @Override
    public void build() {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession es = WorldEdit.getInstance().newEditSession(weWorld)) {
            buildBody(es);
            buildFloor(es);
            buildLighting(es);
            buildCenterPlatform(es);
            buildPillarsAndCanopy(es);
            buildPaths(es);
            buildPortalFrames(es);
            buildEdgeRailing(es);
            buildTrees(es);
        }
    }

    // ── Insel-Körper ──────────────────────────────────────────────────────────

    private void buildBody(EditSession es) {
        for (int y = SY - 6; y < SY; y++) {
            int depth = SY - y;
            int r = R - (depth / 2);
            disk(es, 0, y, 0, Math.max(r, R - 3), BlockTypes.BASALT);
        }
    }

    // ── Boden ─────────────────────────────────────────────────────────────────

    private void buildFloor(EditSession es) {
        // Hauptboden
        disk(es, 0, SY, 0, R, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
        // Innerer Zierring
        ring(es, 0, SY, 0, 26, 28, BlockTypes.CHISELED_POLISHED_BLACKSTONE);
        // Kreuzwege (3 breit, smooth quartz, N/S/E/W)
        for (int i = -R; i <= R; i++) {
            for (int off = -1; off <= 1; off++) {
                // E–W
                if (inDisk(i, off, R)) block(es, i, SY, off, BlockTypes.SMOOTH_QUARTZ);
                // N–S
                if (inDisk(off, i, R)) block(es, off, SY, i, BlockTypes.SMOOTH_QUARTZ);
            }
        }
    }

    // ── Sea-Lantern-Beleuchtung ───────────────────────────────────────────────

    private void buildLighting(EditSession es) {
        for (int x = -R; x <= R; x += 6)
            for (int z = -R; z <= R; z += 6)
                if (inDisk(x, z, R - 2) && (Math.abs(x) > 2 || Math.abs(z) > 2))
                    block(es, x, SY, z, BlockTypes.SEA_LANTERN);
    }

    // ── Spawn-Plattform ───────────────────────────────────────────────────────

    private void buildCenterPlatform(EditSession es) {
        // 7×7 Smooth Quartz auf Y=65
        fill(es, -3, SY + 1, -3, 3, SY + 1, 3, BlockTypes.SMOOTH_QUARTZ);
        // Mittlere Sea Lantern als Spawn-Markierung
        block(es, 0, SY + 1, 0, BlockTypes.SEA_LANTERN);
        // Ecken: voller Quartz-Block
        for (int[] c : new int[][]{{-3, -3}, {-3, 3}, {3, -3}, {3, 3}})
            block(es, c[0], SY + 1, c[1], BlockTypes.QUARTZ_BLOCK);
    }

    // ── Säulen + Glas-Baldachin ───────────────────────────────────────────────

    private void buildPillarsAndCanopy(EditSession es) {
        for (int px : new int[]{-4, 4}) {
            for (int pz : new int[]{-4, 4}) {
                block(es, px, SY,      pz, BlockTypes.QUARTZ_BLOCK);
                for (int y = SY + 1; y <= SY + 9; y++)
                    blockState(es, px, y, pz, "quartz_pillar[axis=y]");
                block(es, px, SY + 10, pz, BlockTypes.QUARTZ_BLOCK);
            }
        }
        // Glas-Baldachin auf Y=SY+10 zwischen den 4 Säulen
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++)
                block(es, x, SY + 10, z, BlockTypes.GLASS_PANE);
    }

    // ── Wege-Bordüre ─────────────────────────────────────────────────────────

    private void buildPaths(EditSession es) {
        // Polished-Andesite-Bordüre links/rechts des Kreuzwegs
        for (int i = 5; i <= R - 2; i++) {
            for (int side : new int[]{-2, 2}) {
                if (inDisk(i,  side, R)) block(es, i,  SY, side, BlockTypes.POLISHED_ANDESITE);
                if (inDisk(-i, side, R)) block(es, -i, SY, side, BlockTypes.POLISHED_ANDESITE);
                if (inDisk(side, i,  R)) block(es, side, SY, i,  BlockTypes.POLISHED_ANDESITE);
                if (inDisk(side, -i, R)) block(es, side, SY, -i, BlockTypes.POLISHED_ANDESITE);
            }
        }
    }

    // ── Portal-Tore (N / S / E / W) ──────────────────────────────────────────

    private void buildPortalFrames(EditSession es) {
        buildGate(es,  0, -28, true);   // Nord
        buildGate(es,  0,  28, true);   // Süd
        buildGate(es,  28,  0, false);  // Ost
        buildGate(es, -28,  0, false);  // West
    }

    /**
     * Baut ein Quartz-Tor aus 3 Säulen + Querbalken.
     * @param nsAxis true = Tor läuft entlang der X-Achse (Nord/Süd-Tor)
     */
    private void buildGate(EditSession es, int cx, int cz, boolean nsAxis) {
        for (int off : new int[]{-2, 0, 2}) {
            int x = nsAxis ? cx + off : cx;
            int z = nsAxis ? cz        : cz + off;
            for (int y = SY; y <= SY + 5; y++)
                blockState(es, x, y, z, "quartz_pillar[axis=y]");
        }
        // Querbalken oben
        if (nsAxis) fill(es, cx - 2, SY + 5, cz, cx + 2, SY + 5, cz, BlockTypes.QUARTZ_BLOCK);
        else        fill(es, cx, SY + 5, cz - 2, cx, SY + 5, cz + 2, BlockTypes.QUARTZ_BLOCK);
        // Laterne oben
        block(es, cx, SY + 6, cz, BlockTypes.SEA_LANTERN);
        // Trittstufe davor (zum Eintreten)
        int xStep = nsAxis ? cx        : (cx > 0 ? cx - 1 : cx + 1);
        int zStep = nsAxis ? (cz > 0 ? cz - 1 : cz + 1) : cz;
        if (nsAxis) fill(es, cx - 2, SY + 1, zStep, cx + 2, SY + 1, zStep, BlockTypes.SMOOTH_QUARTZ);
        else        fill(es, xStep, SY + 1, cz - 2, xStep, SY + 1, cz + 2, BlockTypes.SMOOTH_QUARTZ);
    }

    // ── Rand-Geländer ─────────────────────────────────────────────────────────

    private void buildEdgeRailing(EditSession es) {
        ring(es, 0, SY + 1, 0, R - 1, R, BlockTypes.POLISHED_BLACKSTONE_WALL);
        ring(es, 0, SY + 2, 0, R - 1, R, BlockTypes.POLISHED_BLACKSTONE_WALL);
    }

    // ── Eichenbäume (Diagonalen) ──────────────────────────────────────────────

    private void buildTrees(EditSession es) {
        for (int[] p : new int[][]{{20, 20}, {-20, 20}, {20, -20}, {-20, -20}}) {
            int tx = p[0], tz = p[1];
            for (int y = SY; y <= SY + 4; y++)
                blockState(es, tx, y, tz, "oak_log[axis=y]");
            sphere(es, tx, SY + 6, tz, 3, null); // nur Blätter unten
            addLeafSphere(es, tx, SY + 6, tz, 3);
            addLeafSphere(es, tx, SY + 8, tz, 1);
        }
    }

    private void addLeafSphere(EditSession es, int cx, int cy, int cz, int r) {
        double r2 = (double) r * r;
        for (int x = cx - r; x <= cx + r; x++)
            for (int y = cy - r; y <= cy + r; y++)
                for (int z = cz - r; z <= cz + r; z++) {
                    double dx = x - cx, dy = y - cy, dz = z - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2)
                        blockState(es, x, y, z, "oak_leaves[persistent=true]");
                }
    }
}
