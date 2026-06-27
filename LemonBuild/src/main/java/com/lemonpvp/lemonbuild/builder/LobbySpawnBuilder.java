package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.World;

/**
 * LobbySpawnBuilder — "LemonPvP Hub".
 *
 * <p>Erzeugt eine schwebende, detailreiche Lobby-Insel zentriert auf Weltmitte
 * (0, 64, 0). Benötigt FastAsyncWorldEdit oder WorldEdit. Vollständig
 * deterministisch ({@link BuildHelper#noise}) — wiederholtes Bauen erzeugt
 * exakt dasselbe Ergebnis.
 *
 * <h2>Aufbau</h2>
 * <ul>
 *   <li>Schwebende Insel-Unterseite (umgedrehter Kegel) mit Stein/Andesit/Tuff/
 *       Deepslate-Textur und hängenden Tropfstein-Stalaktiten</li>
 *   <li>Organische Gras-Oberfläche mit wabbeligem Rand (Noise)</li>
 *   <li>Riesiger, verzweigter Eichen-Baum als Mittelpunkt (Wurzeln, dicker
 *       Stamm, mehrere Äste, mehrlagige Blätter-Krone, Leucht-Akzente,
 *       Bienennest und hängende Laternen)</li>
 *   <li>Konzentrische Plaza aus poliertem Blackstone & Quarz</li>
 *   <li>4 Quarz-Wege (N/S/O/W) zu 4 Obsidian-Portalen</li>
 *   <li>4 kleine Zier-Bäume auf den Diagonalen</li>
 *   <li>Blumen- & Gras-Garten (deterministisch gestreut)</li>
 *   <li>4 Wasserfälle, die über den Rand ins Leere stürzen</li>
 *   <li>Zaun-Geländer mit Laternen-Pfosten</li>
 * </ul>
 */
public class LobbySpawnBuilder extends BuildHelper {

    private static final int R  = 40;  // Insel-Radius
    private static final int SY = 64;  // Boden-Y (Gras-Oberfläche)

    private static final int PORTAL_DIST = 34;

    public LobbySpawnBuilder(World world) {
        super(world);
    }

    @Override
    public void build() {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession es = WorldEdit.getInstance().newEditSession(weWorld)) {
            buildUnderside(es);
            buildTerrain(es);
            buildPlaza(es);
            buildPaths(es);
            buildPortals(es);
            buildCentralTree(es);
            buildSmallTrees(es);
            buildGardens(es);
            buildWaterfalls(es);
            buildRailing(es);
            buildLighting(es);
        }
    }

    // ── Schwebende Unterseite ──────────────────────────────────────────────────

    private void buildUnderside(EditSession es) {
        int depth = 22;
        for (int h = 0; h < depth; h++) {
            int y = SY - 1 - h;                         // direkt unter der Oberfläche abwärts
            double t = 1.0 - (double) h / (depth - 1);  // 1 oben → 0 an der Spitze
            double r = (R - 2) * t;
            if (r < 0.6) { set(es, 0, y, 0, "dripstone_block"); continue; }
            int ri = (int) Math.ceil(r);
            for (int x = -ri; x <= ri; x++)
                for (int z = -ri; z <= ri; z++) {
                    if (!inDisk(x, z, r)) continue;
                    double n = noise(x, z, 100L + h);
                    String id = n < 0.55 ? "stone"
                              : n < 0.80 ? "andesite"
                              : n < 0.92 ? "tuff"
                              : "deepslate";
                    set(es, x, y, z, id);
                }
        }
        // Hängende Stalaktiten an der Spitze
        int tipY = SY - depth;
        for (int[] p : new int[][]{{0, 0}, {3, 1}, {-3, -1}, {1, -3}, {-2, 2}, {2, -2}}) {
            int len = 3 + (int) (noise(p[0], p[1], 5) * 4);
            column(es, p[0], p[1], tipY - len, tipY + 1, "dripstone_block");
            set(es, p[0], tipY - len, p[1], "pointed_dripstone[vertical_direction=down]");
        }
    }

    // ── Gras-Oberfläche (organischer Rand) ─────────────────────────────────────

    private void buildTerrain(EditSession es) {
        for (int x = -R - 3; x <= R + 3; x++)
            for (int z = -R - 3; z <= R + 3; z++) {
                double edge = R + (noise(x, z, 7) * 3.0 - 1.5);  // Rand wabbelt ±
                if (!inDisk(x, z, edge)) continue;
                set(es, x, SY,     z, "grass_block");
                set(es, x, SY - 1, z, "dirt");
                set(es, x, SY - 2, z, "dirt");
                set(es, x, SY - 3, z, noise(x, z, 11) < 0.5 ? "dirt" : "coarse_dirt");
            }
    }

    // ── Plaza (konzentrische Ringe um den Baum) ────────────────────────────────

    private void buildPlaza(EditSession es) {
        ring(es, 0, SY, 0,  0, 14, "polished_blackstone_bricks");
        ring(es, 0, SY, 0,  6,  7, "smooth_quartz");
        ring(es, 0, SY, 0,  9, 10, "chiseled_polished_blackstone");
        ring(es, 0, SY, 0, 13, 14, "polished_andesite");
        // Akzent-Laternen auf dem mittleren Ring
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * 11);
            int z = (int) Math.round(Math.sin(a) * 11);
            set(es, x, SY, z, "sea_lantern");
        }
    }

    // ── Wege (N / S / O / W) ───────────────────────────────────────────────────

    private void buildPaths(EditSession es) {
        for (int i = 14; i <= PORTAL_DIST + 1; i++) {
            for (int off = -1; off <= 1; off++) {
                set(es,  i, SY,  off, "smooth_quartz");
                set(es, -i, SY,  off, "smooth_quartz");
                set(es, off, SY,  i, "smooth_quartz");
                set(es, off, SY, -i, "smooth_quartz");
            }
            // Bordüre
            for (int side : new int[]{-2, 2}) {
                set(es,  i, SY, side, "polished_andesite");
                set(es, -i, SY, side, "polished_andesite");
                set(es, side, SY,  i, "polished_andesite");
                set(es, side, SY, -i, "polished_andesite");
            }
        }
    }

    // ── Portale ────────────────────────────────────────────────────────────────

    private void buildPortals(EditSession es) {
        buildPortal(es,  0, -PORTAL_DIST, true);   // Nord
        buildPortal(es,  0,  PORTAL_DIST, true);   // Süd
        buildPortal(es,  PORTAL_DIST,  0, false);  // Ost
        buildPortal(es, -PORTAL_DIST,  0, false);  // West
    }

    /** Obsidian-Portal (3 breit innen, 5 hoch) mit leuchtendem Glas-Kern. */
    private void buildPortal(EditSession es, int cx, int cz, boolean alongX) {
        int top = SY + 5;
        // Rahmen-Pfosten links/rechts
        for (int side : new int[]{-2, 2}) {
            int x = alongX ? cx + side : cx;
            int z = alongX ? cz : cz + side;
            column(es, x, z, SY, top, "obsidian");
            set(es, x, SY,  z, "crying_obsidian");
            set(es, x, top, z, "crying_obsidian");
        }
        // Querbalken oben/unten
        for (int inner = -1; inner <= 1; inner++) {
            int x = alongX ? cx + inner : cx;
            int z = alongX ? cz : cz + inner;
            set(es, x, top, z, "obsidian");
            // leuchtender Portal-Kern
            for (int y = SY + 1; y < top; y++)
                set(es, x, y, z, "light_blue_stained_glass");
        }
        // Krönung
        set(es, cx, top + 1, cz, "sea_lantern");
        // Trittstufe innen
        int sx = alongX ? cx : (cx > 0 ? cx - 1 : cx + 1);
        int sz = alongX ? (cz > 0 ? cz - 1 : cz + 1) : cz;
        for (int inner = -2; inner <= 2; inner++) {
            int x = alongX ? cx + inner : sx;
            int z = alongX ? sz : cz + inner;
            set(es, x, SY, z, "smooth_quartz");
        }
    }

    // ── Zentraler Riesen-Baum ──────────────────────────────────────────────────

    private void buildCentralTree(EditSession es) {
        int base = SY;
        int trunkTop = base + 18;   // y = 82

        // Wurzeln, die in den Boden greifen
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int rx = (int) Math.round(Math.cos(a) * 6);
            int rz = (int) Math.round(Math.sin(a) * 6);
            thickLine(es, 0, base + 1, 0, rx, base, rz, 1.3, "oak_wood");
        }

        // Stamm (verjüngt sich nach oben)
        cyl(es, 0, base,      0, 2.6, 14, "oak_log[axis=y]");   // base .. base+13
        cyl(es, 0, base + 14, 0, 1.8,  5, "oak_log[axis=y]");   // base+14 .. base+18

        // Äste
        int[][] tips = {
                {11, trunkTop + 2,  0}, {-11, trunkTop + 1,  0},
                { 0, trunkTop + 2, 11}, {  0, trunkTop + 1,-11},
                { 8, trunkTop + 4,  8}, { -8, trunkTop + 3, -8},
                { 8, trunkTop + 3, -8}, { -8, trunkTop + 4,  8}
        };
        int branchStart = base + 11;
        for (int[] t : tips)
            thickLine(es, 0, branchStart, 0, t[0], t[1], t[2], 1.1, "oak_wood");

        // Krone — große Top-Kugel + Blätter an jedem Astende
        leafBlob(es, 0, trunkTop + 5, 0, 7);
        for (int[] t : tips) leafBlob(es, t[0], t[1], t[2], 4);

        // Leucht-Akzente in der Krone
        for (int[] g : new int[][]{{0, trunkTop + 5, 0}, {4, trunkTop + 4, 2},
                                    {-3, trunkTop + 3, -3}, {2, trunkTop + 6, -2}})
            set(es, g[0], g[1], g[2], "shroomlight");

        // Bienennest + hängende Laternen unter den Astenden
        set(es, 3, base + 8, 0, "bee_nest[facing=east]");
        for (int[] t : tips) set(es, t[0], t[1] - 3, t[2], "lantern[hanging=true]");
    }

    /** Blätter-Klumpen: Eichenlaub-Kugel mit blühendem Azaleen-Akzent. */
    private void leafBlob(EditSession es, int cx, int cy, int cz, int r) {
        sphere(es, cx, cy, cz, r, "oak_leaves[persistent=true]");
        sphere(es, cx, cy + 1, cz, r - 2.0, "flowering_azalea_leaves");
    }

    // ── Kleine Zier-Bäume (Diagonalen) ─────────────────────────────────────────

    private void buildSmallTrees(EditSession es) {
        for (int[] p : new int[][]{{22, 22}, {-22, 22}, {22, -22}, {-22, -22}}) {
            int tx = p[0], tz = p[1];
            column(es, tx, tz, SY, SY + 5, "oak_log[axis=y]");
            sphere(es, tx, SY + 6, tz, 3, "oak_leaves[persistent=true]");
            sphere(es, tx, SY + 8, tz, 2, "flowering_azalea_leaves");
            set(es, tx, SY + 4, tz, "lantern[hanging=true]");
        }
    }

    // ── Garten (deterministisch gestreute Blumen & Gräser) ─────────────────────

    private void buildGardens(EditSession es) {
        String[] flowers = {"poppy", "dandelion", "cornflower", "oxeye_daisy",
                            "azure_bluet", "allium"};
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++) {
                if (!inDisk(x, z, R - 2)) continue;
                double d2 = x * x + z * z;
                if (d2 < 15 * 15) continue;                       // Plaza freihalten
                if ((Math.abs(x) <= 2 || Math.abs(z) <= 2)) continue; // Wege freihalten
                // unter Zier-Bäumen freihalten
                boolean nearTree = false;
                for (int[] p : new int[][]{{22, 22}, {-22, 22}, {22, -22}, {-22, -22}})
                    if (Math.abs(x - p[0]) <= 3 && Math.abs(z - p[1]) <= 3) { nearTree = true; break; }
                if (nearTree) continue;

                double n = noise(x, z, 42);
                if (n > 0.86)      set(es, x, SY + 1, z, flowers[(int) (noise(x, z, 9) * flowers.length) % flowers.length]);
                else if (n > 0.55) set(es, x, SY + 1, z, noise(x, z, 3) < 0.3 ? "fern" : "short_grass");
            }
    }

    // ── Wasserfälle (über den Rand) ────────────────────────────────────────────

    private void buildWaterfalls(EditSession es) {
        for (int[] d : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
            int ex = d[0] * (R - 3);
            int ez = d[1] * (R - 3);
            // kleines Quell-Becken in der Oberfläche
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    set(es, ex + dx, SY, ez + dz, "water");
            // Kaskade an der Unterseite hinab
            column(es, ex, ez, SY - 12, SY, "water");
        }
    }

    // ── Geländer ───────────────────────────────────────────────────────────────

    private void buildRailing(EditSession es) {
        ring(es, 0, SY + 1, 0, R - 2, R - 1, "oak_fence");
        // Pfosten mit Laternen alle 45°
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = (int) Math.round(Math.cos(a) * (R - 2));
            int z = (int) Math.round(Math.sin(a) * (R - 2));
            column(es, x, z, SY + 1, SY + 2, "oak_fence");
            set(es, x, SY + 3, z, "lantern");
        }
    }

    // ── Beleuchtung ────────────────────────────────────────────────────────────

    private void buildLighting(EditSession es) {
        // Gleichmäßiges Sea-Lantern-Gitter im Gras (unauffällig, gegen Mob-Spawns)
        for (int x = -R; x <= R; x += 8)
            for (int z = -R; z <= R; z += 8)
                if (inDisk(x, z, R - 3) && x * x + z * z > 15 * 15)
                    set(es, x, SY, z, "sea_lantern");
        // Glüh-Akzente an der Insel-Spitze unten
        set(es, 0, SY - 23, 0, "shroomlight");
    }

    // ── Helfer ───────────────────────────────────────────────────────────────

    private boolean inDisk(int dx, int dz, double r) {
        return dx * dx + dz * dz <= r * r;
    }
}
