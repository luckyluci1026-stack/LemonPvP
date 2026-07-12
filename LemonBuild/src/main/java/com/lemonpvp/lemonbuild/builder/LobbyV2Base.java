package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import org.bukkit.World;

/**
 * Part 1 of the "Grand Citrus" lobby (v2): terrain, the central citadel, the
 * queue cathedral (north) and the kit forge (east). Part 2 — the remaining
 * buildings, sky work, paths and the detail pass — lives in
 * {@link LobbySpawnBuilderV2}, which also orchestrates the build.
 *
 * <p>The previous lobby ({@link LobbySpawnBuilder}) is intentionally kept
 * fully working as a fallback: {@code /aowbuildlobby classic} rebuilds it.</p>
 *
 * <p>Everything is deterministic (scatter uses fixed seeds), so rebuilding
 * yields the identical lobby.</p>
 */
public abstract class LobbyV2Base extends BuildHelper {

    protected static final int CX = 0;
    protected static final int Y  = 64;   // deck height
    protected static final int CZ = 0;

    // ── Terrain palette ─────────────────────────────────────────────────────
    protected static final String GRASS      = "grass_block";
    protected static final String MOSS       = "moss_block";
    protected static final String PODZOL     = "podzol";
    protected static final String ROOT_DIRT  = "rooted_dirt";
    protected static final String STONE      = "stone";
    protected static final String ANDESITE   = "andesite";
    protected static final String TUFF       = "tuff";
    protected static final String DEEPSLATE  = "deepslate";
    protected static final String COBBLED_DS = "cobbled_deepslate";

    // ── Citadel palette (warm sandstone + lemon) ────────────────────────────
    protected static final String SAND_SMOOTH = "smooth_sandstone";
    protected static final String SAND_CUT    = "cut_sandstone";
    protected static final String SAND_CHIS   = "chiseled_sandstone";
    protected static final String SAND_STAIR_N = "smooth_sandstone_stairs[facing=north]";
    protected static final String SAND_STAIR_S = "smooth_sandstone_stairs[facing=south]";
    protected static final String SAND_STAIR_E = "smooth_sandstone_stairs[facing=east]";
    protected static final String SAND_STAIR_W = "smooth_sandstone_stairs[facing=west]";
    protected static final String SAND_SLAB    = "smooth_sandstone_slab[type=bottom]";
    protected static final String TERRA_Y      = "yellow_terracotta";
    protected static final String TERRA_O      = "orange_terracotta";
    protected static final String CONCRETE_Y   = "yellow_concrete";
    protected static final String CONCRETE_L   = "lime_concrete";
    protected static final String GLASS_Y      = "yellow_stained_glass";
    protected static final String GLASS_L      = "lime_stained_glass";
    protected static final String GOLD         = "gold_block";
    protected static final String GILDED       = "gilded_blackstone";
    protected static final String LIGHT_Y      = "ochre_froglight";
    protected static final String LIGHT_L      = "verdant_froglight";
    protected static final String SEA_LANTERN  = "sea_lantern";
    protected static final String LANTERN      = "lantern[hanging=false]";
    protected static final String H_LANTERN    = "lantern[hanging=true]";
    protected static final String CHAIN        = "chain[axis=y]";
    protected static final String BULB         = "waxed_copper_bulb[lit=true]";

    // ── Cathedral palette (deepslate + gold) ────────────────────────────────
    protected static final String DS_BRICK   = "deepslate_bricks";
    protected static final String DS_TILE    = "deepslate_tiles";
    protected static final String DS_CHIS    = "chiseled_deepslate";
    protected static final String DS_WALL    = "deepslate_brick_wall";
    protected static final String BLACK_POL  = "polished_blackstone";
    protected static final String BLACK_BRK  = "polished_blackstone_bricks";
    protected static final String DS_STAIR_N = "deepslate_brick_stairs[facing=north]";
    protected static final String DS_STAIR_S = "deepslate_brick_stairs[facing=south]";
    protected static final String DS_STAIR_E = "deepslate_brick_stairs[facing=east]";
    protected static final String DS_STAIR_W = "deepslate_brick_stairs[facing=west]";

    // ── Forge palette (brick + copper) ──────────────────────────────────────
    protected static final String BRICKS      = "bricks";
    protected static final String GRANITE_POL = "polished_granite";
    protected static final String COPPER      = "waxed_cut_copper";
    protected static final String COPPER_OX   = "waxed_oxidized_cut_copper";
    protected static final String COPPER_STAIR_N = "waxed_oxidized_cut_copper_stairs[facing=north]";
    protected static final String COPPER_STAIR_S = "waxed_oxidized_cut_copper_stairs[facing=south]";
    protected static final String COPPER_STAIR_E = "waxed_oxidized_cut_copper_stairs[facing=east]";
    protected static final String COPPER_STAIR_W = "waxed_oxidized_cut_copper_stairs[facing=west]";

    // ── Wood ────────────────────────────────────────────────────────────────
    protected static final String OAK_LOG_Y  = "stripped_oak_log[axis=y]";
    protected static final String OAK_LOG_X  = "stripped_oak_log[axis=x]";
    protected static final String OAK_LOG_Z  = "stripped_oak_log[axis=z]";
    protected static final String DARK_LOG_Y = "stripped_dark_oak_log[axis=y]";
    protected static final String OAK_PLANK  = "oak_planks";
    protected static final String DARK_PLANK = "dark_oak_planks";
    protected static final String CHERRY_PLANK = "cherry_planks";
    protected static final String CHERRY_LOG_Y  = "stripped_cherry_log[axis=y]";
    protected static final String OAK_FENCE  = "oak_fence";
    protected static final String LEAVES     = "azalea_leaves[persistent=true]";
    protected static final String LEAVES_FLW = "flowering_azalea_leaves[persistent=true]";

    protected LobbyV2Base(World world) {
        super(world);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Site clearing — covers v1, v2 and the legacy FFA strip so both lobby
    // versions can be swapped with a single rebuild.
    // ────────────────────────────────────────────────────────────────────────

    protected void clearSiteV2(EditSession es) {
        fill(es, CX - 120, Y - 44, CZ - 120, CX + 120, Y + 96, CZ + 120, "air");
        fill(es, CX + 120, Y - 12, CZ - 40, CX + 900, Y + 40, CZ + 120, "air");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Terrain: layered island, cliff rim, satellite islands, hanging roots
    // ────────────────────────────────────────────────────────────────────────

    /** Main island: grass cap over stratified stone tapering into a root spike. */
    protected void terrainIsland(EditSession es) {
        disk(es, CX, Y, CZ, 95, GRASS);
        disk(es, CX, Y - 1, CZ, 94, "dirt");
        disk(es, CX, Y - 2, CZ, 92, STONE);
        disk(es, CX, Y - 3, CZ, 89, STONE);
        disk(es, CX, Y - 4, CZ, 85, ANDESITE);
        disk(es, CX, Y - 5, CZ, 80, ANDESITE);
        disk(es, CX, Y - 6, CZ, 74, TUFF);
        disk(es, CX, Y - 7, CZ, 67, TUFF);
        disk(es, CX, Y - 8, CZ, 59, DEEPSLATE);
        disk(es, CX, Y - 9, CZ, 50, DEEPSLATE);
        disk(es, CX, Y - 10, CZ, 41, DEEPSLATE);
        cone(es, CX, Y - 34, CZ, 32, 24, true, DEEPSLATE);

        // Grass-top texture: moss/podzol patches + rooted dirt freckles.
        scatter(es, CX, Y, CZ, 92, 0.16, 101L, MOSS, PODZOL);
        scatter(es, CX, Y, CZ, 92, 0.05, 102L, ROOT_DIRT);
        // Grasses and flowers one above the deck (plants sit ON the grass cap).
        scatter(es, CX, Y + 1, CZ, 90, 0.10, 103L,
                "short_grass", "fern", "dandelion", "oxeye_daisy", "cornflower");
    }

    /** Rugged cliff rim: broken stone teeth just outside the grass edge. */
    protected void terrainCliffs(EditSession es) {
        for (int i = 0; i < 72; i++) {
            double a = i * (Math.PI * 2 / 72);
            int x = CX + (int) Math.round(Math.cos(a) * 93);
            int z = CZ + (int) Math.round(Math.sin(a) * 93);
            int h = 1 + (i * 7 % 3);            // deterministic 1..3
            String id = (i % 3 == 0) ? ANDESITE : (i % 3 == 1) ? STONE : TUFF;
            column(es, x, z, Y, Y + h, id);
            if (i % 4 == 0) set(es, x, Y + h + 1, z, "cobblestone_wall");
        }
    }

    /** Four floating satellite islands on the diagonals, linked by rope bridges. */
    protected void satelliteIslands(EditSession es) {
        int[][] pos = {{78, -78}, {-78, -78}, {78, 78}, {-78, 78}};
        for (int i = 0; i < pos.length; i++) {
            int ix = CX + pos[i][0], iz = CZ + pos[i][1];
            int iy = Y + 6 + (i % 2) * 4;   // alternate heights
            disk(es, ix, iy, iz, 11, GRASS);
            disk(es, ix, iy - 1, iz, 10, "dirt");
            disk(es, ix, iy - 2, iz, 8, STONE);
            cone(es, ix, iy - 9, iz, 7, 7, true, DEEPSLATE);
            scatter(es, ix, iy, iz, 10, 0.2, 200L + i, MOSS, PODZOL);
            scatter(es, ix, iy + 1, iz, 9, 0.12, 210L + i, "short_grass", "poppy", "dandelion");
            // A small lemon tree on each islet.
            column(es, ix, iz, iy + 1, iy + 4, OAK_LOG_Y);
            sphere(es, ix, iy + 6, iz, 3.2, LEAVES);
            scatter(es, ix, iy + 6, iz, 3, 0.5, 220L + i, CONCRETE_Y, LEAVES_FLW);
            set(es, ix, iy + 9, iz, LANTERN);
            ropeBridge(es, ix, iy, iz);
        }
    }

    /** Rope bridge from a satellite islet back toward the main island rim. */
    private void ropeBridge(EditSession es, int ix, int iy, int iz) {
        // Walk from the islet edge toward the center until the main rim (r=93).
        double dx = CX - ix, dz = CZ - iz;
        double len = Math.sqrt(dx * dx + dz * dz);
        double ux = dx / len, uz = dz / len;
        int steps = (int) (len - 93 - 8);     // islet edge (~8) to main rim
        for (int s = 0; s <= steps; s++) {
            int bx = ix + (int) Math.round(ux * (8 + s));
            int bz = iz + (int) Math.round(uz * (8 + s));
            // Sag: dip toward the middle of the span, land at deck height Y.
            double t = steps == 0 ? 0 : (double) s / steps;
            int by = (int) Math.round(iy * (1 - t) + Y * t - Math.sin(t * Math.PI) * 2);
            set(es, bx, by, bz, DARK_PLANK);
            if (s % 2 == 0) {
                set(es, bx, by + 1, bz, OAK_FENCE);
            }
            if (s % 6 == 3) set(es, bx, by + 2, bz, LANTERN);
        }
    }

    /** Hanging deepslate roots + amethyst veins under the island. */
    protected void undersideRoots(EditSession es) {
        for (int i = 0; i < 40; i++) {
            double a = i * (Math.PI * 2 / 40);
            double r = 30 + (i * 13 % 45);
            int x = CX + (int) Math.round(Math.cos(a) * r);
            int z = CZ + (int) Math.round(Math.sin(a) * r);
            int top = Y - 8 - (int) (r / 12);
            int len = 3 + (i * 5 % 6);
            column(es, x, z, top - len, top, i % 4 == 0 ? COBBLED_DS : DEEPSLATE);
            if (i % 4 == 0) set(es, x, top - len - 1, z, "amethyst_cluster[facing=down]");
            else if (i % 4 == 2) set(es, x, top - len - 1, z, "hanging_roots");
        }
        // Glowing ore veins peeking out of the underside.
        for (int i = 0; i < 26; i++) {
            double a = i * 2.399963;            // golden angle — even spread
            double r = 12 + (i * 17 % 60);
            int x = CX + (int) Math.round(Math.cos(a) * r);
            int z = CZ + (int) Math.round(Math.sin(a) * r);
            set(es, x, Y - 6 - (int) (r / 10), z, i % 2 == 0 ? "glowstone" : "shroomlight");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Central citadel: three tiers, four grand stairs, fountain ring, crown
    // ────────────────────────────────────────────────────────────────────────

    /** Tiered spawn citadel in warm sandstone with lemon inlays. */
    protected void citadelBase(EditSession es) {
        // Tier 1 (r=22, +1)
        disk(es, CX, Y + 1, CZ, 22, SAND_SMOOTH);
        ring(es, CX, Y + 1, CZ, 21.2, 22, SAND_CUT);
        // Tier 2 (r=15, +2)
        disk(es, CX, Y + 2, CZ, 15, SAND_SMOOTH);
        ring(es, CX, Y + 2, CZ, 14.2, 15, TERRA_Y);
        // Tier 3 (r=8, +3) — the spawn platform proper
        disk(es, CX, Y + 3, CZ, 8, SAND_SMOOTH);
        ring(es, CX, Y + 3, CZ, 7.2, 8, SAND_CHIS);
        disk(es, CX, Y + 3, CZ, 3, "polished_diorite");
        set(es, CX, Y + 3, CZ, GOLD);

        // Lemon compass inlay on tier 1.
        for (int d = 4; d <= 20; d++) {
            String inlay = (d % 4 < 2) ? CONCRETE_Y : CONCRETE_L;
            set(es, CX + d, Y + 1, CZ, inlay);
            set(es, CX - d, Y + 1, CZ, inlay);
            set(es, CX, Y + 1, CZ + d, inlay);
            set(es, CX, Y + 1, CZ - d, inlay);
        }

        // Four grand staircases (N/S/E/W), 5 wide, descending outward from tier 1.
        for (int w = -2; w <= 2; w++) {
            set(es, CX + w, Y + 1, CZ - 23, SAND_STAIR_S);
            set(es, CX + w, Y + 1, CZ + 23, SAND_STAIR_N);
            set(es, CX - 23, Y + 1, CZ + w, SAND_STAIR_E);
            set(es, CX + 23, Y + 1, CZ + w, SAND_STAIR_W);
            // tier2 steps
            set(es, CX + w, Y + 2, CZ - 16, SAND_STAIR_S);
            set(es, CX + w, Y + 2, CZ + 16, SAND_STAIR_N);
            set(es, CX - 16, Y + 2, CZ + w, SAND_STAIR_E);
            set(es, CX + 16, Y + 2, CZ + w, SAND_STAIR_W);
            // tier3 steps
            set(es, CX + w, Y + 3, CZ - 9, SAND_STAIR_S);
            set(es, CX + w, Y + 3, CZ + 9, SAND_STAIR_N);
            set(es, CX - 9, Y + 3, CZ + w, SAND_STAIR_E);
            set(es, CX + 9, Y + 3, CZ + w, SAND_STAIR_W);
        }
    }

    /** Ring fountain between tier 1 and 2 with four water jets. */
    protected void citadelFountain(EditSession es) {
        ring(es, CX, Y + 2, CZ, 17.4, 19.4, SAND_CUT);   // basin wall
        ring(es, CX, Y + 2, CZ, 17.8, 19.0, "water");    // water fill
        ring(es, CX, Y + 1, CZ, 17.8, 19.0, GLASS_L);    // glowing basin floor accent
        // Jets on the diagonals.
        int[][] jets = {{13, 13}, {-13, 13}, {13, -13}, {-13, -13}};
        for (int[] j : jets) {
            int jx = CX + j[0], jz = CZ + j[1];
            column(es, jx, jz, Y + 2, Y + 5, SAND_CHIS);
            set(es, jx, Y + 6, jz, SEA_LANTERN);
            set(es, jx, Y + 7, jz, "water");
        }
    }

    /** Eight pillars around tier 2 carrying a golden ring + lantern chains. */
    protected void citadelPillars(EditSession es) {
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI / 4) + Math.PI / 8;
            int px = CX + (int) Math.round(Math.cos(a) * 12);
            int pz = CZ + (int) Math.round(Math.sin(a) * 12);
            column(es, px, pz, Y + 3, Y + 10, "quartz_pillar[axis=y]");
            set(es, px, Y + 11, pz, SAND_CHIS);
            set(es, px, Y + 12, pz, GOLD);
            set(es, px, Y + 2, pz, GILDED);
        }
        ring(es, CX, Y + 13, CZ, 11.2, 12.8, TERRA_Y);   // connecting crown ring
        ring(es, CX, Y + 13, CZ, 12.8, 13.4, SAND_SLAB); // eave lip
        for (int i = 0; i < 16; i++) {
            double a = i * (Math.PI / 8);
            int px = CX + (int) Math.round(Math.cos(a) * 12);
            int pz = CZ + (int) Math.round(Math.sin(a) * 12);
            set(es, px, Y + 12, pz, H_LANTERN);
        }
    }

    /** The floating lemon-crystal crown above spawn: glass shell + gold core. */
    protected void citadelCrown(EditSession es) {
        int cy = Y + 20;
        hollowSphere(es, CX, cy, CZ, 5.2, 1.1, GLASS_Y);
        sphere(es, CX, cy, CZ, 2.4, GOLD);
        set(es, CX, cy, CZ, "beacon");
        // Petal fins on four sides.
        for (int i = 0; i < 4; i++) {
            double a = i * (Math.PI / 2);
            int fx = CX + (int) Math.round(Math.cos(a) * 6);
            int fz = CZ + (int) Math.round(Math.sin(a) * 6);
            column(es, fx, fz, cy - 1, cy + 1, GLASS_L);
        }
        column(es, CX, CZ, cy + 6, cy + 8, "end_rod[facing=up]");
        column(es, CX, CZ, Y + 14, cy - 6, CHAIN);     // suspended from... the sky
        // Slow-turn illusion: staggered gold studs orbiting the shell.
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI / 4);
            set(es, CX + (int) Math.round(Math.cos(a) * 7), cy + ((i % 2 == 0) ? 1 : -1),
                    CZ + (int) Math.round(Math.sin(a) * 7), GILDED);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // North: the Queue Cathedral — twin-towered gate hall, glowing portal
    // ────────────────────────────────────────────────────────────────────────

    protected void queueCathedral(EditSession es) {
        int bz = CZ - 62;                     // building center line (north)
        // Foundation slab.
        fill(es, CX - 17, Y, bz - 9, CX + 17, Y, bz + 9, DS_TILE);
        fill(es, CX - 17, Y + 1, bz - 9, CX + 17, Y + 1, bz + 9, "air");

        // Nave shell (hollow) with tiled roof gable.
        hollowBox(es, CX - 12, Y + 1, bz - 7, CX + 12, Y + 12, bz + 7, DS_BRICK);
        fill(es, CX - 12, Y + 1, bz + 7, CX + 12, Y + 12, bz + 7, DS_BRICK); // south face
        // Gable roof (stepped).
        for (int step = 0; step < 6; step++) {
            int y = Y + 12 + step;
            fill(es, CX - 12 + step * 2, y, bz - 8, CX - 10 + step * 2, y, bz + 8, DS_STAIR_E);
            fill(es, CX + 10 - step * 2, y, bz - 8, CX + 12 - step * 2, y, bz + 8, DS_STAIR_W);
        }
        fill(es, CX - 1, Y + 17, bz - 8, CX + 1, Y + 17, bz + 8, DS_CHIS);   // ridge

        // Grand entry arch on the south face (toward spawn), 5 wide 7 high.
        fill(es, CX - 2, Y + 1, bz + 7, CX + 2, Y + 6, bz + 7, "air");
        set(es, CX - 3, Y + 6, bz + 7, DS_STAIR_E);
        set(es, CX + 3, Y + 6, bz + 7, DS_STAIR_W);
        fill(es, CX - 1, Y + 7, bz + 7, CX + 1, Y + 7, bz + 7, DS_CHIS);
        set(es, CX, Y + 8, bz + 7, GOLD);

        // Twin towers flanking the entrance.
        for (int side = -1; side <= 1; side += 2) {
            int tx = CX + side * 15;
            hollowBox(es, tx - 3, Y + 1, bz + 3, tx + 3, Y + 22, bz + 9, DS_BRICK);
            // Corner chisel trim.
            column(es, tx - 3, bz + 3, Y + 1, Y + 22, BLACK_BRK);
            column(es, tx + 3, bz + 3, Y + 1, Y + 22, BLACK_BRK);
            column(es, tx - 3, bz + 9, Y + 1, Y + 22, BLACK_BRK);
            column(es, tx + 3, bz + 9, Y + 1, Y + 22, BLACK_BRK);
            // Glass slit windows.
            for (int wy = Y + 4; wy <= Y + 18; wy += 4) {
                set(es, tx, wy, bz + 9, GLASS_Y);
                set(es, tx, wy + 1, bz + 9, GLASS_Y);
            }
            // Spire.
            pyramid(es, tx, Y + 23, bz + 6, 4, false, DS_TILE);
            set(es, tx, Y + 28, bz + 6, GOLD);
            set(es, tx, Y + 29, bz + 6, "end_rod[facing=up]");
            // Wall banners beside the arch.
            set(es, tx - side * 4, Y + 5, bz + 8, "yellow_wall_banner[facing=south]");
        }

        // Interior: glowing queue portal ring at the far wall + pew rows.
        tube(es, CX, Y + 2, bz - 5, 2.2, 3.2, 1, GOLD);          // flat ring on floor
        for (int h = 0; h < 5; h++) {
            // standing portal ring (vertical) — approximated with columns/arcs
            set(es, CX - 3, Y + 1 + h, bz - 6, h == 4 ? GOLD : DS_CHIS);
            set(es, CX + 3, Y + 1 + h, bz - 6, h == 4 ? GOLD : DS_CHIS);
        }
        fill(es, CX - 2, Y + 5, bz - 6, CX + 2, Y + 5, bz - 6, GOLD);
        fill(es, CX - 2, Y + 1, bz - 6, CX + 2, Y + 4, bz - 6, GLASS_L);
        set(es, CX, Y + 3, bz - 5, SEA_LANTERN);
        // Carpet runner + pews.
        fill(es, CX - 1, Y + 1, bz - 4, CX + 1, Y + 1, bz + 6, "yellow_carpet");
        for (int row = 0; row < 3; row++) {
            int rz = bz - 1 + row * 3;
            fill(es, CX - 8, Y + 1, rz, CX - 3, Y + 1, rz, "dark_oak_stairs[facing=north]");
            fill(es, CX + 3, Y + 1, rz, CX + 8, Y + 1, rz, "dark_oak_stairs[facing=north]");
        }
        // Chandeliers.
        for (int cxo = -6; cxo <= 6; cxo += 6) {
            set(es, CX + cxo, Y + 11, bz, CHAIN);
            set(es, CX + cxo, Y + 10, bz, H_LANTERN);
        }
        // Lawn banners leading up to the cathedral.
        for (int d = 30; d <= 50; d += 5) {
            set(es, CX - 4, Y + 1, CZ - d, "yellow_banner[rotation=0]");
            set(es, CX + 4, Y + 1, CZ - d, "lime_banner[rotation=0]");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // East: the Kit Forge — brick smithy with copper roof and working corner
    // ────────────────────────────────────────────────────────────────────────

    protected void kitForge(EditSession es) {
        int bx = CX + 62;
        // Foundation + floor.
        fill(es, bx - 9, Y, CZ - 11, bx + 9, Y, CZ + 11, GRANITE_POL);
        // Walls: brick with granite pilasters and dark-oak framing.
        hollowBox(es, bx - 8, Y + 1, CZ - 10, bx + 8, Y + 8, CZ + 10, BRICKS);
        for (int px = -8; px <= 8; px += 4) {
            column(es, bx + px, CZ - 10, Y + 1, Y + 8, GRANITE_POL);
            column(es, bx + px, CZ + 10, Y + 1, Y + 8, GRANITE_POL);
        }
        column(es, bx - 8, CZ - 10, Y + 1, Y + 9, DARK_LOG_Y);
        column(es, bx + 8, CZ - 10, Y + 1, Y + 9, DARK_LOG_Y);
        column(es, bx - 8, CZ + 10, Y + 1, Y + 9, DARK_LOG_Y);
        column(es, bx + 8, CZ + 10, Y + 1, Y + 9, DARK_LOG_Y);

        // Entrance facing spawn (west face), 3 wide.
        fill(es, bx - 8, Y + 1, CZ - 1, bx - 8, Y + 4, CZ + 1, "air");
        set(es, bx - 8, Y + 5, CZ, "brick_stairs[facing=west,half=top]");
        set(es, bx - 8, Y + 3, CZ - 2, "wall_torch[facing=west]");
        set(es, bx - 8, Y + 3, CZ + 2, "wall_torch[facing=west]");

        // Lava-glass window strips on the north/south faces.
        for (int wz = -6; wz <= 6; wz += 6) {
            fill(es, bx + wz - 1, Y + 3, CZ - 10, bx + wz + 1, Y + 4, CZ - 10, "orange_stained_glass");
            fill(es, bx + wz - 1, Y + 3, CZ + 10, bx + wz + 1, Y + 4, CZ + 10, "orange_stained_glass");
        }

        // Copper hip roof.
        for (int step = 0; step < 5; step++) {
            int y = Y + 9 + step;
            fill(es, bx - 9 + step * 2, y, CZ - 11 + step, bx - 7 + step * 2, y, CZ + 11 - step, COPPER_STAIR_E);
            fill(es, bx + 7 - step * 2, y, CZ - 11 + step, bx + 9 - step * 2, y, CZ + 11 - step, COPPER_STAIR_W);
            fill(es, bx - 7 + step * 2, y, CZ - 11 + step, bx + 7 - step * 2, y, CZ - 9 + step, COPPER_STAIR_S);
            fill(es, bx - 7 + step * 2, y, CZ + 9 - step, bx + 7 - step * 2, y, CZ + 11 - step, COPPER_STAIR_N);
        }
        fill(es, bx - 1, Y + 13, CZ - 3, bx + 1, Y + 13, CZ + 3, COPPER_OX);   // ridge cap

        // Chimney with embers.
        hollowBox(es, bx + 4, Y + 9, CZ - 8, bx + 6, Y + 16, CZ - 6, BRICKS);
        set(es, bx + 5, Y + 16, CZ - 7, "campfire[lit=true]");

        // Interior: the working forge corner.
        set(es, bx + 5, Y + 1, CZ + 7, "blast_furnace[facing=west]");
        set(es, bx + 5, Y + 1, CZ + 5, "blast_furnace[facing=west]");
        set(es, bx + 5, Y + 1, CZ + 6, "smithing_table");
        set(es, bx + 3, Y + 1, CZ + 7, "anvil[facing=north]");
        set(es, bx + 3, Y + 1, CZ + 5, "grindstone[face=floor,facing=north]");
        fill(es, bx + 4, Y + 1, CZ - 7, bx + 6, Y + 1, CZ - 5, "lava");        // ember pit
        fill(es, bx + 3, Y + 1, CZ - 4, bx + 6, Y + 1, CZ - 4, "iron_bars");    // safety rail
        fill(es, bx + 3, Y + 1, CZ - 8, bx + 3, Y + 1, CZ - 5, "iron_bars");
        // Weapon racks: item frames abstracted as chiseled bookshelves + swords motif.
        fill(es, bx - 7, Y + 2, CZ + 9, bx - 3, Y + 4, CZ + 9, "chiseled_bookshelf[facing=north]");
        set(es, bx, Y + 1, CZ, "yellow_carpet");
        // Armor-stand style plinths (visual): chiseled quartz + glass helm.
        for (int i = -1; i <= 1; i++) {
            set(es, bx - 4 + i * 3, Y + 1, CZ - 8, "chiseled_quartz_block");
            set(es, bx - 4 + i * 3, Y + 2, CZ - 8, GLASS_Y);
        }
        // Hanging sign lanterns at the door.
        set(es, bx - 9, Y + 6, CZ, H_LANTERN);
    }
}
