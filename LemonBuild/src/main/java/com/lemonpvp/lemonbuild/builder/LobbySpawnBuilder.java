package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import org.bukkit.World;

/**
 * LemonPvP hub island — modern practice-server look (FlowPvP / mcpvp.club
 * style): ONE clean floating island, bright white base palette with
 * lemon-yellow and lime accents, clearly zoned:
 *
 * <pre>
 *                 N — QUEUE PLAZA (grand golden arch)
 *   W — LEADERBOARD WALL          E — KIT-EDITOR PAVILION
 *                 S — SHOP & DAILY CORNER
 * </pre>
 *
 * Centre: circular spawn dais under a floating "lemon sun" (glass sphere with
 * a glowing core). Everything is decorative — queueing, kit editing, shop and
 * leaderboards run through the GUIs/holograms; the zones give them a physical
 * home so staff can place NPCs/holograms exactly there.
 *
 * <p>Build origin is (0, 64, 0); the island spans roughly radius 50 with four
 * satellite plazas. The site pass first clears BOTH the previous hub build AND
 * the legacy tropical FFA arena strip that used to run from x=0 to x=880 in
 * this world (the "battlefield" look). Deterministic — rebuilding yields the
 * identical island.</p>
 */
public class LobbySpawnBuilder extends BuildHelper {

    private static final int CX = 0;
    private static final int Y  = 64;   // deck height
    private static final int CZ = 0;

    // Palette (all 1.21-safe ids)
    private static final String DECK        = "calcite";
    private static final String DECK_ALT    = "smooth_quartz";
    private static final String DECK_RIM    = "quartz_bricks";
    private static final String ACCENT      = "yellow_concrete";
    private static final String ACCENT_SOFT = "lime_concrete";
    private static final String GLASS_Y     = "yellow_stained_glass";
    private static final String GLASS_L     = "lime_stained_glass";
    private static final String LIGHT_Y     = "ochre_froglight";
    private static final String LIGHT_L     = "verdant_froglight";
    private static final String PILLAR      = "quartz_pillar[axis=y]";
    private static final String CHISEL      = "chiseled_quartz_block";
    private static final String DARK        = "smooth_blackstone";
    private static final String GOLD        = "gold_block";
    private static final String GILDED      = "gilded_blackstone";
    private static final String LANTERN     = "lantern[hanging=false]";
    private static final String H_LANTERN   = "lantern[hanging=true]";
    private static final String SEA_LANTERN = "sea_lantern";
    private static final String LEAVES      = "azalea_leaves[persistent=true]";
    private static final String LEAVES_FLOW = "flowering_azalea_leaves[persistent=true]";
    private static final String WOOD        = "stripped_oak_log[axis=y]";
    private static final String PANE        = "white_stained_glass_pane";

    public LobbySpawnBuilder(World world) {
        super(world);
    }

    @Override
    public void build() {
        try (EditSession es = WorldEdit.getInstance()
                .newEditSessionBuilder().world(weWorld()).maxBlocks(-1).build()) {

            clearSite(es);

            mainIsland(es);
            deckPattern(es);
            centerDais(es);
            lemonSun(es);

            queuePlaza(es);        // north
            kitPavilion(es);       // east
            leaderboardWall(es);   // west
            shopCorner(es);        // south

            lemonTrees(es);
            edgeRail(es);
            underside(es);
            floatingIslets(es);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Site clearing
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Wipes the previous hub AND the legacy tropical FFA arena strip (five
     * 80x80 arenas from x=0..880, z=0..79 plus their bases) that made the
     * lobby look like a battlefield.
     */
    private void clearSite(EditSession es) {
        // Hub area (old grand-hub island was radius ~64).
        fill(es, CX - 80, Y - 30, CZ - 80, CX + 80, Y + 70, CZ + 80, "air");
        // Legacy FFA arena strip east of spawn.
        fill(es, CX + 80, Y - 12, CZ - 40, CX + 900, Y + 40, CZ + 120, "air");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Main island deck
    // ────────────────────────────────────────────────────────────────────────

    private void mainIsland(EditSession es) {
        // Main circular deck, 2 thick, radius 34, with a quartz-brick rim.
        disk(es, CX, Y - 1, CZ, 34, DECK);
        disk(es, CX, Y,     CZ, 34, DECK);
        ring(es, CX, Y,     CZ, 32.5, 34, DECK_RIM);

        // Four broad walkways to the zone plazas (N/E/S/W).
        fill(es, CX - 4, Y - 1, CZ - 46, CX + 4, Y, CZ - 30, DECK);   // N
        fill(es, CX + 30, Y - 1, CZ - 4, CX + 46, Y, CZ + 4, DECK);   // E
        fill(es, CX - 4, Y - 1, CZ + 30, CX + 4, Y, CZ + 46, DECK);   // S
        fill(es, CX - 46, Y - 1, CZ - 4, CX - 30, Y, CZ + 4, DECK);   // W

        // Zone plazas (round pads at the four walkway ends).
        int[][] pads = {{0, -50}, {50, 0}, {0, 50}, {-50, 0}};
        for (int[] p : pads) {
            disk(es, CX + p[0], Y - 1, CZ + p[1], 10, DECK);
            disk(es, CX + p[0], Y,     CZ + p[1], 10, DECK);
            ring(es, CX + p[0], Y,     CZ + p[1], 8.5, 10, DECK_RIM);
        }
    }

    /** Inlaid pattern: concentric accent rings + glowing walkway centre lines. */
    private void deckPattern(EditSession es) {
        ring(es, CX, Y, CZ, 26.5, 27.5, ACCENT);
        ring(es, CX, Y, CZ, 17.5, 18.5, ACCENT_SOFT);
        fill(es, CX, Y, CZ - 45, CX, Y, CZ - 30, LIGHT_Y);
        fill(es, CX + 30, Y, CZ, CX + 45, Y, CZ, LIGHT_Y);
        fill(es, CX, Y, CZ + 30, CX, Y, CZ + 45, LIGHT_Y);
        fill(es, CX - 45, Y, CZ, CX - 30, Y, CZ, LIGHT_Y);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Centre: spawn dais + floating lemon sun
    // ────────────────────────────────────────────────────────────────────────

    private void centerDais(EditSession es) {
        // Three-step circular dais.
        disk(es, CX, Y + 1, CZ, 10, DECK_ALT);
        disk(es, CX, Y + 2, CZ, 7,  DECK_ALT);
        disk(es, CX, Y + 3, CZ, 4.5, CHISEL);
        ring(es, CX, Y + 1, CZ, 9,   10,  ACCENT);
        ring(es, CX, Y + 2, CZ, 6,   7,   ACCENT_SOFT);
        ring(es, CX, Y + 3, CZ, 3.5, 4.5, GOLD);
        // Glowing centre pad players spawn on.
        disk(es, CX, Y + 3, CZ, 1.5, SEA_LANTERN);

        // Eight slim pillars around the dais carrying lanterns.
        int[][] p = {{14,0},{-14,0},{0,14},{0,-14},{10,10},{-10,10},{10,-10},{-10,-10}};
        for (int[] q : p) {
            column(es, CX + q[0], CZ + q[1], Y + 1, Y + 5, PILLAR);
            set(es, CX + q[0], Y + 6, CZ + q[1], CHISEL);
            set(es, CX + q[0], Y + 7, CZ + q[1], LANTERN);
        }
    }

    /** The floating "lemon sun": glass sphere + glowing core + lime leaf ring. */
    private void lemonSun(EditSession es) {
        int sy = Y + 16;
        hollowSphere(es, CX, sy, CZ, 6.5, 1.4, GLASS_Y);
        sphere(es, CX, sy, CZ, 3.0, LIGHT_Y);
        ring(es, CX, sy, CZ, 7.5, 8.5, GLASS_L);
        set(es, CX, sy + 7, CZ, WOOD);
        set(es, CX, sy + 8, CZ, LEAVES);
        set(es, CX + 1, sy + 8, CZ, LEAVES_FLOW);
        set(es, CX, sy - 8, CZ, H_LANTERN);
        set(es, CX + 4, sy - 7, CZ + 4, H_LANTERN);
        set(es, CX - 4, sy - 7, CZ - 4, H_LANTERN);
    }

    // ────────────────────────────────────────────────────────────────────────
    // North: QUEUE PLAZA — grand golden double arch
    // ────────────────────────────────────────────────────────────────────────

    private void queuePlaza(EditSession es) {
        int pz = CZ - 50;
        ring(es, CX, Y, pz, 5.5, 6.5, ACCENT);
        disk(es, CX, Y, pz, 1.5, LIGHT_Y);

        // Nested arches (portal look) at the plaza's far edge.
        archX(es, CX, Y + 1, pz - 6, 7, 9, DARK);
        archX(es, CX, Y + 1, pz - 6, 5, 7, GOLD);
        // Glass "energy" fill inside the inner arch.
        fill(es, CX - 4, Y + 1, pz - 6, CX + 4, Y + 6, pz - 6, GLASS_Y);
        fill(es, CX - 2, Y + 1, pz - 6, CX + 2, Y + 4, pz - 6, GLASS_L);

        // Flanking pillars with froglight caps.
        for (int dx : new int[]{-8, 8}) {
            column(es, CX + dx, pz + 4, Y + 1, Y + 4, PILLAR);
            set(es, CX + dx, Y + 5, pz + 4, LIGHT_L);
        }
    }

    /** Solid arch in the XY-plane at depth z: legs at cx±(inner..outer). */
    private void archX(EditSession es, int cx, int y0, int z, int inner, int outer, String id) {
        int h = outer;
        fill(es, cx - outer, y0, z, cx - inner, y0 + h, z, id);
        fill(es, cx + inner, y0, z, cx + outer, y0 + h, z, id);
        fill(es, cx - outer, y0 + h, z, cx + outer, y0 + h + (outer - inner), z, id);
    }

    // ────────────────────────────────────────────────────────────────────────
    // East: KIT-EDITOR PAVILION — open rotunda with a smithing-corner feel
    // ────────────────────────────────────────────────────────────────────────

    private void kitPavilion(EditSession es) {
        int px = CX + 50;
        ring(es, px, Y, CZ, 5.5, 6.5, ACCENT_SOFT);
        disk(es, px, Y, CZ, 1.5, LIGHT_L);

        // Six columns carrying a glass dome.
        int[][] cols = {{6,0},{-6,0},{3,5},{-3,5},{3,-5},{-3,-5}};
        for (int[] c : cols) {
            column(es, px + c[0], CZ + c[1], Y + 1, Y + 5, WOOD);
            set(es, px + c[0], Y + 6, CZ + c[1], CHISEL);
        }
        ring(es, px, Y + 6, CZ, 5.0, 7.0, GLASS_L);
        ring(es, px, Y + 7, CZ, 2.5, 5.0, GLASS_Y);
        disk(es, px, Y + 8, CZ, 2.5, GLASS_L);
        set(es, px, Y + 9, CZ, GOLD);
        set(es, px, Y + 5, CZ, H_LANTERN);

        // Workbench furniture — this is where the Kit Editor NPC/sign belongs.
        set(es, px - 2, Y + 1, CZ - 2, "anvil[facing=east]");
        set(es, px + 2, Y + 1, CZ - 2, "smithing_table");
        set(es, px - 2, Y + 1, CZ + 2, "fletching_table");
        set(es, px + 2, Y + 1, CZ + 2, "grindstone[face=floor,facing=north]");
    }

    // ────────────────────────────────────────────────────────────────────────
    // West: LEADERBOARD WALL — dark panel backdrop for holograms
    // ────────────────────────────────────────────────────────────────────────

    private void leaderboardWall(EditSession es) {
        int px = CX - 50;
        ring(es, px, Y, CZ, 5.5, 6.5, ACCENT);
        disk(es, px, Y, CZ, 1.5, LIGHT_Y);

        // Three dark panels with gold trim, facing the walkway (east).
        for (int i = -1; i <= 1; i++) {
            int zc = CZ + i * 8;
            int xw = px - 6 - Math.abs(i);   // outer panels angle back slightly
            fill(es, xw, Y + 1, zc - 3, xw, Y + 8, zc + 3, DARK);
            fill(es, xw, Y + 1, zc - 3, xw, Y + 1, zc + 3, GILDED);
            fill(es, xw, Y + 8, zc - 3, xw, Y + 8, zc + 3, GILDED);
            // Inset glass strip — the hologram floats in front of this.
            fill(es, xw, Y + 4, zc - 2, xw, Y + 5, zc + 2, "tinted_glass");
            set(es, xw, Y + 9, zc, LANTERN);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // South: SHOP & DAILY CORNER — two market stalls
    // ────────────────────────────────────────────────────────────────────────

    private void shopCorner(EditSession es) {
        int pz = CZ + 50;
        ring(es, CX, Y, pz, 5.5, 6.5, ACCENT_SOFT);
        disk(es, CX, Y, pz, 1.5, LIGHT_L);

        stall(es, CX - 6, pz, true);   // shop stall
        stall(es, CX + 6, pz, false);  // daily-reward stall

        set(es, CX - 1, Y + 1, pz + 3, "barrel[facing=up]");
        set(es, CX + 1, Y + 1, pz + 3, "chest[facing=north]");
    }

    /** A 5x5 market stall: counter, corner poles, striped awning, lantern. */
    private void stall(EditSession es, int sx, int sz, boolean yellowFirst) {
        String a = yellowFirst ? "yellow_wool" : "white_wool";
        String b = yellowFirst ? "white_wool" : "lime_wool";
        fill(es, sx - 2, Y + 1, sz - 1, sx + 2, Y + 1, sz - 1, DARK);
        column(es, sx - 2, sz - 2, Y + 1, Y + 4, WOOD);
        column(es, sx + 2, sz - 2, Y + 1, Y + 4, WOOD);
        column(es, sx - 2, sz + 2, Y + 1, Y + 4, WOOD);
        column(es, sx + 2, sz + 2, Y + 1, Y + 4, WOOD);
        for (int x = sx - 2; x <= sx + 2; x++) {
            String id = ((x - sx) % 2 == 0) ? a : b;
            fill(es, x, Y + 5, sz - 2, x, Y + 5, sz + 2, id);
        }
        set(es, sx, Y + 4, sz, H_LANTERN);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Greenery: four stylised lemon trees on the deck diagonals
    // ────────────────────────────────────────────────────────────────────────

    private void lemonTrees(EditSession es) {
        int d = 22;
        lemonTree(es, CX + d, CZ + d);
        lemonTree(es, CX - d, CZ + d);
        lemonTree(es, CX + d, CZ - d);
        lemonTree(es, CX - d, CZ - d);
    }

    private void lemonTree(EditSession es, int tx, int tz) {
        ring(es, tx, Y, tz, 2.0, 3.0, DECK_RIM);
        disk(es, tx, Y, tz, 2.0, "moss_block");
        column(es, tx, tz, Y + 1, Y + 5, WOOD);
        sphere(es, tx, Y + 7, tz, 3.4, LEAVES);
        sphere(es, tx, Y + 7, tz, 1.2, LEAVES_FLOW);
        // Glowing "lemons" tucked into the canopy.
        set(es, tx + 2, Y + 6, tz + 1, LIGHT_Y);
        set(es, tx - 2, Y + 7, tz - 1, LIGHT_Y);
        set(es, tx + 1, Y + 8, tz - 2, LIGHT_Y);
        set(es, tx - 1, Y + 6, tz + 2, LIGHT_Y);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Edge rail, underside, floating decor islets
    // ────────────────────────────────────────────────────────────────────────

    /** Low glass rail with lantern posts; walkway mouths stay open. */
    private void edgeRail(EditSession es) {
        for (int deg = 0; deg < 360; deg++) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 33.5);
            int z = CZ + (int) Math.round(Math.sin(rad) * 33.5);
            if (Math.abs(x - CX) <= 6 && (z < CZ - 25 || z > CZ + 25)) continue;
            if (Math.abs(z - CZ) <= 6 && (x < CX - 25 || x > CX + 25)) continue;
            set(es, x, Y + 1, z, PANE);
            if (deg % 30 == 0) {
                set(es, x, Y + 2, z, CHISEL);
                set(es, x, Y + 3, z, LANTERN);
            }
        }
    }

    /** Layered inverted cone underside so the island reads as floating rock. */
    private void underside(EditSession es) {
        disk(es, CX, Y - 2, CZ, 30, DECK);
        disk(es, CX, Y - 3, CZ, 26, "tuff");
        disk(es, CX, Y - 4, CZ, 21, "tuff");
        disk(es, CX, Y - 5, CZ, 16, "deepslate");
        disk(es, CX, Y - 6, CZ, 11, "deepslate");
        disk(es, CX, Y - 7, CZ, 7,  "deepslate");
        disk(es, CX, Y - 8, CZ, 4,  "deepslate");
        set(es, CX, Y - 9, CZ, SEA_LANTERN);
        for (int deg = 0; deg < 360; deg += 45) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 28);
            int z = CZ + (int) Math.round(Math.sin(rad) * 28);
            set(es, x, Y - 3, z, H_LANTERN);
        }
        int[][] pads = {{0, -50}, {50, 0}, {0, 50}, {-50, 0}};
        for (int[] pad : pads) {
            disk(es, CX + pad[0], Y - 2, CZ + pad[1], 8, DECK);
            disk(es, CX + pad[0], Y - 3, CZ + pad[1], 5, "tuff");
            disk(es, CX + pad[0], Y - 4, CZ + pad[1], 3, "deepslate");
        }
    }

    /** Small floating decor islets with mini-trees around the hub. */
    private void floatingIslets(EditSession es) {
        islet(es, CX + 30, Y + 10, CZ - 34);
        islet(es, CX - 34, Y + 8,  CZ + 30);
        islet(es, CX + 38, Y + 14, CZ + 26);
        islet(es, CX - 28, Y + 13, CZ - 38);
    }

    private void islet(EditSession es, int ix, int iy, int iz) {
        disk(es, ix, iy,     iz, 3.5, "moss_block");
        disk(es, ix, iy - 1, iz, 2.5, "tuff");
        disk(es, ix, iy - 2, iz, 1.2, "deepslate");
        column(es, ix, iz, iy + 1, iy + 2, WOOD);
        sphere(es, ix, iy + 4, iz, 2.2, LEAVES);
        set(es, ix + 1, iy + 4, iz, LIGHT_Y);
        set(es, ix, iy - 3, iz, H_LANTERN);
    }
}
