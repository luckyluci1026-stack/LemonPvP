package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import org.bukkit.World;

/**
 * LemonPvP hub island — modern practice-server look (FlowPvP / mcpvp.club
 * style), grand edition. ONE clean floating island in a bright white palette
 * with lemon-yellow and lime accents, clearly zoned:
 *
 * <pre>
 *                 N — QUEUE PLAZA (grand golden arch)
 *   W — LEADERBOARD WALL          E — KIT-EDITOR PAVILION
 *                 S — SHOP & DAILY CORNER
 * </pre>
 *
 * Around the core deck runs a full promenade ring (pergolas, benches, gardens,
 * crescent ponds) connected by eight bridges; four crystal spires mark the
 * diagonals, a golden sky halo floats above the glass "lemon sun", a floating
 * parkour ring circles the whole island, light-falls pour off the rim and
 * amethyst stalactites hang beneath. All decorative — queueing, kit editing,
 * shop and leaderboards run through the GUIs/holograms; the zones give them a
 * physical home.
 *
 * <p>Build origin is (0, 64, 0); the composition spans roughly radius 62. The
 * site pass first clears BOTH the previous hub build AND the legacy tropical
 * FFA arena strip (x=0..880) that made spawn look like a battlefield.
 * Deterministic — rebuilding yields the identical island.</p>
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
    private static final String BEAM_X      = "stripped_oak_log[axis=x]";
    private static final String BEAM_Z      = "stripped_oak_log[axis=z]";
    private static final String PANE        = "white_stained_glass_pane";
    private static final String CHAIN       = "chain[axis=y]";
    private static final String BULB        = "waxed_copper_bulb[lit=true]";
    private static final String MOSS        = "moss_block";
    private static final String CARPET      = "moss_carpet";

    public LobbySpawnBuilder(World world) {
        super(world);
    }

    @Override
    public void build() {
        try (EditSession es = WorldEdit.getInstance()
                .newEditSessionBuilder().world(weWorld()).maxBlocks(-1).build()) {

            clearSite(es);

            // Core island
            mainIsland(es);
            deckPattern(es);
            centerDais(es);
            lemonSun(es);
            skyHalo(es);

            // Outer promenade ring + its life
            promenade(es);
            pergolas(es);
            gardens(es);
            ponds(es);
            crystalSpires(es);

            // Zone plazas
            queuePlaza(es);        // north
            kitPavilion(es);       // east
            leaderboardWall(es);   // west
            shopCorner(es);        // south

            // Greenery + edges + sky + depth
            lemonTrees(es);
            edgeRail(es);
            underside(es);
            underCrystals(es);
            lightFalls(es);
            floatingIslets(es);
            parkourRing(es);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Site clearing
    // ────────────────────────────────────────────────────────────────────────

    /** Wipes the previous hub AND the legacy tropical FFA arena strip. */
    private void clearSite(EditSession es) {
        fill(es, CX - 100, Y - 34, CZ - 100, CX + 100, Y + 80, CZ + 100, "air");
        fill(es, CX + 100, Y - 12, CZ - 40, CX + 900, Y + 40, CZ + 120, "air");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Core island deck
    // ────────────────────────────────────────────────────────────────────────

    private void mainIsland(EditSession es) {
        disk(es, CX, Y - 1, CZ, 34, DECK);
        disk(es, CX, Y,     CZ, 34, DECK);
        ring(es, CX, Y,     CZ, 32.5, 34, DECK_RIM);

        // Four broad walkways to the zone plazas (N/E/S/W).
        fill(es, CX - 4, Y - 1, CZ - 62, CX + 4, Y, CZ - 30, DECK);   // N
        fill(es, CX + 30, Y - 1, CZ - 4, CX + 62, Y, CZ + 4, DECK);   // E
        fill(es, CX - 4, Y - 1, CZ + 30, CX + 4, Y, CZ + 62, DECK);   // S
        fill(es, CX - 62, Y - 1, CZ - 4, CX - 30, Y, CZ + 4, DECK);   // W

        // Zone plazas (round pads past the promenade).
        int[][] pads = {{0, -62}, {62, 0}, {0, 62}, {-62, 0}};
        for (int[] p : pads) {
            disk(es, CX + p[0], Y - 1, CZ + p[1], 11, DECK);
            disk(es, CX + p[0], Y,     CZ + p[1], 11, DECK);
            ring(es, CX + p[0], Y,     CZ + p[1], 9.5, 11, DECK_RIM);
        }
    }

    /** Inlaid pattern: accent rings, compass star, glowing walkway lines. */
    private void deckPattern(EditSession es) {
        ring(es, CX, Y, CZ, 26.5, 27.5, ACCENT);
        ring(es, CX, Y, CZ, 17.5, 18.5, ACCENT_SOFT);
        // Compass star: thin diagonal accent rays between the walkways.
        for (int i = 12; i <= 26; i++) {
            set(es, CX + i, Y, CZ + i, ACCENT_SOFT);
            set(es, CX - i, Y, CZ + i, ACCENT_SOFT);
            set(es, CX + i, Y, CZ - i, ACCENT_SOFT);
            set(es, CX - i, Y, CZ - i, ACCENT_SOFT);
        }
        // Glowing centre lines along all four walkways.
        fill(es, CX, Y, CZ - 58, CX, Y, CZ - 30, LIGHT_Y);
        fill(es, CX + 30, Y, CZ, CX + 58, Y, CZ, LIGHT_Y);
        fill(es, CX, Y, CZ + 30, CX, Y, CZ + 58, LIGHT_Y);
        fill(es, CX - 58, Y, CZ, CX - 30, Y, CZ, LIGHT_Y);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Centre: spawn dais + lemon sun + sky halo
    // ────────────────────────────────────────────────────────────────────────

    private void centerDais(EditSession es) {
        disk(es, CX, Y + 1, CZ, 10, DECK_ALT);
        disk(es, CX, Y + 2, CZ, 7,  DECK_ALT);
        disk(es, CX, Y + 3, CZ, 4.5, CHISEL);
        ring(es, CX, Y + 1, CZ, 9,   10,  ACCENT);
        ring(es, CX, Y + 2, CZ, 6,   7,   ACCENT_SOFT);
        ring(es, CX, Y + 3, CZ, 3.5, 4.5, GOLD);
        disk(es, CX, Y + 3, CZ, 1.5, SEA_LANTERN);

        int[][] p = {{14,0},{-14,0},{0,14},{0,-14},{10,10},{-10,10},{10,-10},{-10,-10}};
        for (int[] q : p) {
            column(es, CX + q[0], CZ + q[1], Y + 1, Y + 5, PILLAR);
            set(es, CX + q[0], Y + 6, CZ + q[1], CHISEL);
            set(es, CX + q[0], Y + 7, CZ + q[1], LANTERN);
        }
    }

    /** Lemon sun + two orbiting mini-lemons. */
    private void lemonSun(EditSession es) {
        int sy = Y + 16;
        hollowSphere(es, CX, sy, CZ, 6.5, 1.4, GLASS_Y);
        sphere(es, CX, sy, CZ, 3.0, LIGHT_Y);
        ring(es, CX, sy, CZ, 7.5, 8.5, GLASS_L);
        set(es, CX, sy + 7, CZ, WOOD);
        set(es, CX, sy + 8, CZ, LEAVES);
        set(es, CX + 1, sy + 8, CZ, LEAVES_FLOW);
        set(es, CX, sy - 8, CZ, H_LANTERN);

        // Orbiting mini-lemons (opposite sides, different heights).
        hollowSphere(es, CX + 13, sy + 3, CZ + 6, 2.6, 1.2, GLASS_Y);
        set(es, CX + 13, sy + 3, CZ + 6, LIGHT_Y);
        hollowSphere(es, CX - 12, sy - 2, CZ - 8, 2.2, 1.2, GLASS_L);
        set(es, CX - 12, sy - 2, CZ - 8, LIGHT_L);
    }

    /** Golden halo high above the sun with hanging chain lanterns. */
    private void skyHalo(EditSession es) {
        int hy = Y + 34;
        ring(es, CX, hy, CZ, 18.5, 20, GLASS_Y);
        ring(es, CX, hy + 1, CZ, 19, 19.8, GOLD);
        for (int deg = 0; deg < 360; deg += 30) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 19);
            int z = CZ + (int) Math.round(Math.sin(rad) * 19);
            column(es, x, z, hy - 3, hy - 1, CHAIN);
            set(es, x, hy - 4, z, H_LANTERN);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Promenade ring (radius 38..48) + eight bridges
    // ────────────────────────────────────────────────────────────────────────

    private void promenade(EditSession es) {
        // Ring deck, 2 thick, with rims on both edges and an accent centre line.
        ring(es, CX, Y - 1, CZ, 38, 48, DECK);
        ring(es, CX, Y,     CZ, 38, 48, DECK);
        ring(es, CX, Y,     CZ, 38,   39, DECK_RIM);
        ring(es, CX, Y,     CZ, 47,   48, DECK_RIM);
        ring(es, CX, Y,     CZ, 42.6, 43.4, ACCENT);

        // Eight bridges: four are the main walkways (already there), four
        // diagonal foot bridges from the core deck to the promenade.
        int[][] diag = {{1,1},{-1,1},{1,-1},{-1,-1}};
        for (int[] d : diag) {
            // Bridge from r=32 to r=40 along the diagonal, 3 wide.
            for (int r = 23; r <= 29; r++) {
                int x = CX + d[0] * r, z = CZ + d[1] * r;
                fill(es, x - 1, Y - 1, z - 1, x + 1, Y, z + 1, DECK);
            }
            // Lantern posts at both bridge mouths.
            int bx = CX + d[0] * 24, bz = CZ + d[1] * 24;
            set(es, bx + d[1] * 2, Y + 1, bz - d[0] * 2, LANTERN);
            set(es, bx - d[1] * 2, Y + 1, bz + d[0] * 2, LANTERN);
        }

        // Glow studs along the promenade centre.
        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 43);
            int z = CZ + (int) Math.round(Math.sin(rad) * 43);
            set(es, x, Y, z, LIGHT_Y);
        }
    }

    /** Eight pergolas with benches spaced around the promenade. */
    private void pergolas(EditSession es) {
        for (int deg = 22; deg < 360; deg += 45) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 43);
            int z = CZ + (int) Math.round(Math.sin(rad) * 43);
            pergola(es, x, z);
        }
    }

    private void pergola(EditSession es, int px, int pz) {
        // Four posts + slatted roof + vines/leaves, bench block inside.
        column(es, px - 2, pz - 2, Y + 1, Y + 4, WOOD);
        column(es, px + 2, pz - 2, Y + 1, Y + 4, WOOD);
        column(es, px - 2, pz + 2, Y + 1, Y + 4, WOOD);
        column(es, px + 2, pz + 2, Y + 1, Y + 4, WOOD);
        for (int x = px - 2; x <= px + 2; x++) set(es, x, Y + 5, pz - 2, BEAM_X);
        for (int x = px - 2; x <= px + 2; x++) set(es, x, Y + 5, pz + 2, BEAM_X);
        for (int z = pz - 2; z <= pz + 2; z++) set(es, px - 2, Y + 5, z, BEAM_Z);
        for (int z = pz - 2; z <= pz + 2; z++) set(es, px + 2, Y + 5, z, BEAM_Z);
        fill(es, px - 1, Y + 5, pz - 1, px + 1, Y + 5, pz + 1, LEAVES);
        set(es, px, Y + 5, pz, LEAVES_FLOW);
        // Bench: two quartz slabs.
        set(es, px - 1, Y + 1, pz, "smooth_quartz_slab[type=bottom]");
        set(es, px + 1, Y + 1, pz, "smooth_quartz_slab[type=bottom]");
        set(es, px, Y + 4, pz, H_LANTERN);
    }

    /** Flower gardens tucked between core deck and promenade (deterministic). */
    private void gardens(EditSession es) {
        int[][] spots = {{0, -36}, {36, 0}, {0, 36}, {-36, 0}};
        long seed = 4242L;
        for (int[] s : spots) {
            int gx = CX + s[0], gz = CZ + s[1];
            // Moss bed patch beside each walkway mouth (two per axis).
            for (int side : new int[]{-1, 1}) {
                int px = gx + (s[1] != 0 ? side * 8 : 0);
                int pz = gz + (s[0] != 0 ? side * 8 : 0);
                disk(es, px, Y, pz, 3.2, MOSS);
                scatter(es, px, Y + 1, pz, 3.0, 0.45, seed++,
                        "dandelion", "oxeye_daisy", CARPET, "short_grass");
            }
        }
    }

    /** Two crescent koi ponds with glowing glass bottoms and pickles. */
    private void ponds(EditSession es) {
        pond(es, CX + 20, CZ - 20);
        pond(es, CX - 20, CZ + 20);
    }

    private void pond(EditSession es, int px, int pz) {
        // Bowl: rim ring, glass-light bottom, one water layer.
        ring(es, px, Y, pz, 4.0, 5.2, DECK_RIM);
        disk(es, px, Y - 1, pz, 4.0, GLASS_L);
        set(es, px, Y - 2, pz, SEA_LANTERN);
        disk(es, px, Y, pz, 4.0, "water");
        // Pickles + lily accents (waterlogged plants).
        set(es, px + 2, Y, pz + 1, "sea_pickle[pickles=3,waterlogged=true]");
        set(es, px - 1, Y, pz - 2, "sea_pickle[pickles=2,waterlogged=true]");
        set(es, px + 1, Y + 1, pz - 1, "lily_pad");
        set(es, px - 2, Y + 1, pz + 2, "lily_pad");
    }

    /** Four tapering crystal spires on the promenade diagonals. */
    private void crystalSpires(EditSession es) {
        int d = 30; // on the diagonal (r≈42) between pergolas
        spire(es, CX + d, CZ + d);
        spire(es, CX - d, CZ + d);
        spire(es, CX + d, CZ - d);
        spire(es, CX - d, CZ - d);
    }

    private void spire(EditSession es, int sx, int sz) {
        // Base.
        disk(es, sx, Y + 1, sz, 3.2, CHISEL);
        ring(es, sx, Y + 1, sz, 2.2, 3.2, GOLD);
        // Tapering quartz shaft with a glowing glass core.
        cyl(es, sx, Y + 2, sz, 2.2, 6, DECK_ALT);
        cyl(es, sx, Y + 8, sz, 1.6, 6, DECK_ALT);
        column(es, sx, sz, Y + 2, Y + 17, GLASS_Y);
        cyl(es, sx, Y + 14, sz, 1.0, 3, GLASS_Y);
        // Copper-bulb studs up the shaft + gold cap + beacon-style tip light.
        set(es, sx + 2, Y + 4, sz, BULB);
        set(es, sx - 2, Y + 6, sz, BULB);
        set(es, sx, Y + 9, sz + 2, BULB);
        set(es, sx, Y + 11, sz - 2, BULB);
        set(es, sx, Y + 17, sz, GOLD);
        set(es, sx, Y + 18, sz, LIGHT_Y);
        set(es, sx, Y + 19, sz, "end_rod[facing=up]");
    }

    // ────────────────────────────────────────────────────────────────────────
    // North: QUEUE PLAZA — grand golden double arch
    // ────────────────────────────────────────────────────────────────────────

    private void queuePlaza(EditSession es) {
        int pz = CZ - 62;
        ring(es, CX, Y, pz, 6.5, 7.5, ACCENT);
        disk(es, CX, Y, pz, 1.5, LIGHT_Y);

        archX(es, CX, Y + 1, pz - 7, 8, 11, DARK);
        archX(es, CX, Y + 1, pz - 7, 6, 8, GOLD);
        fill(es, CX - 5, Y + 1, pz - 7, CX + 5, Y + 7, pz - 7, GLASS_Y);
        fill(es, CX - 3, Y + 1, pz - 7, CX + 3, Y + 5, pz - 7, GLASS_L);
        // Arch crown lights.
        set(es, CX, Y + 13, pz - 7, LIGHT_Y);
        set(es, CX - 6, Y + 12, pz - 7, LANTERN);
        set(es, CX + 6, Y + 12, pz - 7, LANTERN);

        for (int dx : new int[]{-9, 9}) {
            column(es, CX + dx, pz + 5, Y + 1, Y + 4, PILLAR);
            set(es, CX + dx, Y + 5, pz + 5, LIGHT_L);
        }
    }

    private void archX(EditSession es, int cx, int y0, int z, int inner, int outer, String id) {
        int h = outer;
        fill(es, cx - outer, y0, z, cx - inner, y0 + h, z, id);
        fill(es, cx + inner, y0, z, cx + outer, y0 + h, z, id);
        fill(es, cx - outer, y0 + h, z, cx + outer, y0 + h + (outer - inner), z, id);
    }

    // ────────────────────────────────────────────────────────────────────────
    // East: KIT-EDITOR PAVILION
    // ────────────────────────────────────────────────────────────────────────

    private void kitPavilion(EditSession es) {
        int px = CX + 62;
        ring(es, px, Y, CZ, 6.5, 7.5, ACCENT_SOFT);
        disk(es, px, Y, CZ, 1.5, LIGHT_L);

        int[][] cols = {{7,0},{-7,0},{4,6},{-4,6},{4,-6},{-4,-6}};
        for (int[] c : cols) {
            column(es, px + c[0], CZ + c[1], Y + 1, Y + 6, WOOD);
            set(es, px + c[0], Y + 7, CZ + c[1], CHISEL);
        }
        ring(es, px, Y + 7, CZ, 5.5, 8.0, GLASS_L);
        ring(es, px, Y + 8, CZ, 3.0, 5.5, GLASS_Y);
        disk(es, px, Y + 9, CZ, 3.0, GLASS_L);
        set(es, px, Y + 10, CZ, GOLD);
        set(es, px, Y + 6, CZ, H_LANTERN);

        set(es, px - 2, Y + 1, CZ - 2, "anvil[facing=east]");
        set(es, px + 2, Y + 1, CZ - 2, "smithing_table");
        set(es, px - 2, Y + 1, CZ + 2, "fletching_table");
        set(es, px + 2, Y + 1, CZ + 2, "grindstone[face=floor,facing=north]");
        // Weapon-rack wall behind the pavilion.
        fill(es, px + 8, Y + 1, CZ - 3, px + 8, Y + 4, CZ + 3, DARK);
        fill(es, px + 8, Y + 2, CZ - 2, px + 8, Y + 3, CZ + 2, GILDED);
        set(es, px + 8, Y + 5, CZ, LANTERN);
    }

    // ────────────────────────────────────────────────────────────────────────
    // West: LEADERBOARD WALL
    // ────────────────────────────────────────────────────────────────────────

    private void leaderboardWall(EditSession es) {
        int px = CX - 62;
        ring(es, px, Y, CZ, 6.5, 7.5, ACCENT);
        disk(es, px, Y, CZ, 1.5, LIGHT_Y);

        for (int i = -1; i <= 1; i++) {
            int zc = CZ + i * 9;
            int xw = px - 7 - Math.abs(i);
            fill(es, xw, Y + 1, zc - 3, xw, Y + 9, zc + 3, DARK);
            fill(es, xw, Y + 1, zc - 3, xw, Y + 1, zc + 3, GILDED);
            fill(es, xw, Y + 9, zc - 3, xw, Y + 9, zc + 3, GILDED);
            fill(es, xw, Y + 4, zc - 2, xw, Y + 6, zc + 2, "tinted_glass");
            set(es, xw, Y + 10, zc, LANTERN);
            // Podium blocks in front of each panel (1st/2nd/3rd feel).
            set(es, xw + 2, Y + 1, zc, i == 0 ? GOLD : (i == -1 ? "iron_block" : "waxed_copper_block"));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // South: SHOP & DAILY CORNER
    // ────────────────────────────────────────────────────────────────────────

    private void shopCorner(EditSession es) {
        int pz = CZ + 62;
        ring(es, CX, Y, pz, 6.5, 7.5, ACCENT_SOFT);
        disk(es, CX, Y, pz, 1.5, LIGHT_L);

        stall(es, CX - 6, pz, true);
        stall(es, CX + 6, pz, false);
        set(es, CX - 1, Y + 1, pz + 4, "barrel[facing=up]");
        set(es, CX + 1, Y + 1, pz + 4, "chest[facing=north]");
        // Mini apple-tree nod to the LemonLobby economy.
        column(es, CX, pz + 7, Y + 1, Y + 3, WOOD);
        sphere(es, CX, Y + 5, pz + 7, 2.2, LEAVES_FLOW);
        set(es, CX + 1, Y + 4, pz + 7, LIGHT_Y);
    }

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
    // Greenery, edges, depth
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
        disk(es, tx, Y, tz, 2.0, MOSS);
        column(es, tx, tz, Y + 1, Y + 5, WOOD);
        sphere(es, tx, Y + 7, tz, 3.4, LEAVES);
        sphere(es, tx, Y + 7, tz, 1.2, LEAVES_FLOW);
        set(es, tx + 2, Y + 6, tz + 1, LIGHT_Y);
        set(es, tx - 2, Y + 7, tz - 1, LIGHT_Y);
        set(es, tx + 1, Y + 8, tz - 2, LIGHT_Y);
        set(es, tx - 1, Y + 6, tz + 2, LIGHT_Y);
    }

    /** Glass rail around the PROMENADE edge; walkway mouths stay open. */
    private void edgeRail(EditSession es) {
        for (int deg = 0; deg < 360; deg++) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 47.5);
            int z = CZ + (int) Math.round(Math.sin(rad) * 47.5);
            if (Math.abs(x - CX) <= 6 && (z < CZ - 40 || z > CZ + 40)) continue;
            if (Math.abs(z - CZ) <= 6 && (x < CX - 40 || x > CX + 40)) continue;
            set(es, x, Y + 1, z, PANE);
            if (deg % 20 == 0) {
                set(es, x, Y + 2, z, CHISEL);
                set(es, x, Y + 3, z, LANTERN);
            }
        }
        // Inner rail on the promenade's inner edge (gap ring between decks).
        for (int deg = 0; deg < 360; deg += 2) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 38.5);
            int z = CZ + (int) Math.round(Math.sin(rad) * 38.5);
            if (Math.abs(x - CX) <= 6 || Math.abs(z - CZ) <= 6) continue;      // walkways
            if (Math.abs(Math.abs(x - CX) - Math.abs(z - CZ)) <= 3) continue;  // diagonal bridges
            set(es, x, Y + 1, z, PANE);
        }
    }

    /** Layered floating-rock undersides for all decks. */
    private void underside(EditSession es) {
        // Core island.
        disk(es, CX, Y - 2, CZ, 30, DECK);
        disk(es, CX, Y - 3, CZ, 26, "tuff");
        disk(es, CX, Y - 4, CZ, 21, "tuff");
        disk(es, CX, Y - 5, CZ, 16, "deepslate");
        disk(es, CX, Y - 6, CZ, 11, "deepslate");
        disk(es, CX, Y - 7, CZ, 7,  "deepslate");
        disk(es, CX, Y - 8, CZ, 4,  "deepslate");
        set(es, CX, Y - 9, CZ, SEA_LANTERN);
        // Promenade ring underside.
        ring(es, CX, Y - 2, CZ, 39, 47, "tuff");
        ring(es, CX, Y - 3, CZ, 41, 45, "deepslate");
        // Plaza pads.
        int[][] pads = {{0, -62}, {62, 0}, {0, 62}, {-62, 0}};
        for (int[] pad : pads) {
            disk(es, CX + pad[0], Y - 2, CZ + pad[1], 9, DECK);
            disk(es, CX + pad[0], Y - 3, CZ + pad[1], 6, "tuff");
            disk(es, CX + pad[0], Y - 4, CZ + pad[1], 3, "deepslate");
        }
    }

    /** Amethyst stalactites + chain drops hanging beneath the island. */
    private void underCrystals(EditSession es) {
        int[][] spots = {{12, 6}, {-10, 14}, {6, -16}, {-14, -8}, {20, -2}, {-2, 20}};
        int i = 0;
        for (int[] s : spots) {
            int x = CX + s[0], z = CZ + s[1];
            int len = 2 + (i % 3);
            column(es, x, z, Y - 6 - len, Y - 6, "amethyst_block");
            set(es, x, Y - 7 - len, z, "amethyst_cluster[facing=down]");
            i++;
        }
        for (int deg = 0; deg < 360; deg += 60) {
            double rad = Math.toRadians(deg);
            int x = CX + (int) Math.round(Math.cos(rad) * 43);
            int z = CZ + (int) Math.round(Math.sin(rad) * 43);
            column(es, x, z, Y - 5, Y - 3, CHAIN);
            set(es, x, Y - 6, z, H_LANTERN);
        }
    }

    /** Light-falls: glowing glass columns pouring off the promenade rim. */
    private void lightFalls(EditSession es) {
        int[][] falls = {{34, 34}, {-34, 34}, {34, -34}, {-34, -34}};
        for (int[] f : falls) {
            // Normalise onto the rim diagonal (r≈48/√2 ≈ 34).
            int x = CX + f[0], z = CZ + f[1];
            fill(es, x, Y - 10, z, x, Y, z, GLASS_Y);
            fill(es, x + (f[0] > 0 ? 1 : -1), Y - 8, z, x + (f[0] > 0 ? 1 : -1), Y - 1, z, GLASS_L);
            set(es, x, Y - 11, z, LIGHT_Y);
            set(es, x, Y - 12, z, "end_rod[facing=down]");
        }
    }

    /** Small floating decor islets around the hub. */
    private void floatingIslets(EditSession es) {
        islet(es, CX + 42, Y + 12, CZ - 48);
        islet(es, CX - 48, Y + 9,  CZ + 42);
        islet(es, CX + 52, Y + 16, CZ + 36);
        islet(es, CX - 38, Y + 15, CZ - 52);
        islet(es, CX + 10, Y + 26, CZ + 44);
        islet(es, CX - 12, Y + 24, CZ - 46);
    }

    private void islet(EditSession es, int ix, int iy, int iz) {
        disk(es, ix, iy,     iz, 3.5, MOSS);
        disk(es, ix, iy - 1, iz, 2.5, "tuff");
        disk(es, ix, iy - 2, iz, 1.2, "deepslate");
        column(es, ix, iz, iy + 1, iy + 2, WOOD);
        sphere(es, ix, iy + 4, iz, 2.2, LEAVES);
        set(es, ix + 1, iy + 4, iz, LIGHT_Y);
        set(es, ix, iy - 3, iz, H_LANTERN);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Parkour ring — 20 floating pads circling the island
    // ────────────────────────────────────────────────────────────────────────

    private void parkourRing(EditSession es) {
        // Start/finish platform off the NE promenade edge.
        int sx = CX + 38, sz = CZ - 38;
        fill(es, sx - 2, Y + 3, sz - 2, sx + 2, Y + 3, sz + 2, DECK_ALT);
        ring(es, sx, Y + 3, sz, 1.8, 2.8, ACCENT);
        set(es, sx, Y + 4, sz, LANTERN);

        // 20 pads spiral around the island (r≈56, heights bobbing ±3).
        for (int i = 0; i < 20; i++) {
            double ang = Math.toRadians(-45 + i * (360.0 / 20));
            int px = CX + (int) Math.round(Math.cos(ang) * 56);
            int pz2 = CZ + (int) Math.round(Math.sin(ang) * 56);
            int py = Y + 4 + (int) Math.round(Math.sin(i * 1.1) * 3);
            String id = (i % 5 == 4) ? LIGHT_Y : (i % 2 == 0 ? DECK_ALT : ACCENT_SOFT);
            fill(es, px - 1, py, pz2 - 1, px + 1, py, pz2 + 1, id);
            if (i % 5 == 4) set(es, px, py + 1, pz2, "end_rod[facing=up]");
        }
    }
}
