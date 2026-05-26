package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * Builds "Citadel Aeternum" — LemonPvP's detailed floating-island practice hub.
 *
 * Layout (top-down, centred at world 0,64,0)
 * ─────────────────────────────────────────
 *   Bow Portal   (0,  64, -22)  — dark-oak / stone theme
 *   Totem Portal (16, 64, -16)  — jungle / nature theme
 *   Sword Portal (-22,64,  0)   — quartz / iron theme
 *   Crystal Portal(22, 64,  0)  — end-stone / amethyst theme
 *   Mace Portal  (0,  64,  22)  — nether-brick / fire theme
 *
 *   Island ellipse : rx=44, rz=38
 *   Central tower  : 13×13 base, 36 blocks tall, polished-deepslate + quartz
 *   Perimeter wall : follows ellipse at ~87% radius, stone-brick with merlons
 *   Watchtowers    : four 5×5 stone-brick turrets near island corners
 *   Forest zone    : NE quadrant — oak, dark-oak, birch, cherry trees
 *   Water gardens  : SW quadrant — two pools, stream, waterfall
 *   Underground cave: y=47-57, accessible via hidden trapdoor shaft
 */
public class SpawnBuilder extends BuildHelper {

    // ── Island geometry ─────────────────────────────────────────────────────────
    private static final int RX = 44;   // island X half-width
    private static final int RZ = 38;   // island Z half-depth
    private static final int SY = 64;   // surface Y

    public SpawnBuilder(LemonPractice plugin, org.bukkit.World world) {
        super(plugin, world);
    }

    // ============================================================================
    //  ENTRY POINT
    // ============================================================================

    @Override
    public void build() {
        try (EditSession es = WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .fastMode(true)
                .build()) {

            // ① terrain (must come first — everything else overwrites it)
            buildIslandBase(es);
            buildIslandUnderbelly(es);

            // ② hub structures
            buildCentralPlaza(es);
            buildMainTower(es);
            buildTowerInterior(es);

            // ③ practice portals
            buildCrystalPortal(es);
            buildSwordPortal(es);
            buildMacePortal(es);
            buildBowPortal(es);
            buildTotemPortal(es);

            // ④ connectivity
            buildGrandStaircase(es);
            buildPathways(es);
            buildBridges(es);

            // ⑤ nature
            buildForestZone(es);
            buildWaterGardens(es);
            buildVegetation(es);

            // ⑥ fortifications
            buildPerimeterWalls(es);
            buildWatchtowers(es);

            // ⑦ lighting & polish
            buildLampPosts(es);
            buildUndergroundCave(es);
            buildFinalAccents(es);

        } catch (Exception e) {
            plugin.getLogger().severe("[SpawnBuilder] Build error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    //  ①  TERRAIN
    // ============================================================================

    private void buildIslandBase(EditSession es) {
        for (int x = -RX; x <= RX; x++) {
            for (int z = -RZ; z <= RZ; z++) {
                double ex = (double) x / RX;
                double ez = (double) z / RZ;
                double t  = ex * ex + ez * ez;
                if (t > 1.0) continue;

                // Island bottom: deepest at centre (y=42), tapers to y=61 at edge
                int bottomY = 42 + (int) (19.0 * t);

                // ── Surface block ──────────────────────────────────────────────
                if (t > 0.88) {
                    block(es, x, SY, z, BlockTypes.COARSE_DIRT);
                } else if (t > 0.76 && (Math.abs(x) % 5 == 0 || Math.abs(z) % 6 == 0)) {
                    block(es, x, SY, z, BlockTypes.COARSE_DIRT);
                } else if (t > 0.60 && (x + z) % 7 == 0) {
                    block(es, x, SY, z, BlockTypes.DIRT);
                } else {
                    block(es, x, SY, z, BlockTypes.GRASS_BLOCK);
                }

                // ── Subsurface ─────────────────────────────────────────────────
                fill(es, x, SY - 2, z, x, SY - 1, z, BlockTypes.DIRT);

                int stoneTop    = SY - 3;
                int stoneBottom = Math.max(bottomY + 6, SY - 11);
                if (stoneTop >= stoneBottom)
                    fill(es, x, stoneBottom, z, x, stoneTop, z, BlockTypes.STONE);

                int deepTop    = stoneBottom - 1;
                int deepBottom = bottomY + 3;
                if (deepTop >= deepBottom)
                    fill(es, x, deepBottom, z, x, deepTop, z, BlockTypes.DEEPSLATE);

                // ── Bottom rim ─────────────────────────────────────────────────
                block(es, x, bottomY + 2, z, BlockTypes.COBBLED_DEEPSLATE);
                block(es, x, bottomY + 1, z, BlockTypes.GRAVEL);
                block(es, x, bottomY,     z, BlockTypes.COBBLED_DEEPSLATE);
            }
        }
    }

    private void buildIslandUnderbelly(EditSession es) {
        // Stalactite clusters hanging from the underside
        int[][] clusters = {
            { 0,  0,  9}, {16, 10,  7}, {-16,  8,  6}, {10,-16,  7},
            {-10,-18,  6}, {22,  4,  5}, {-22, -4,  5}, { 6, 22,  7},
            { -6,-22,  5}, {18,-10,  6}, {-18, 12,  5}, { 8,-28,  4},
            {-8,  28,  4}, {28,  -4, 4}, {-28,  4,  4}
        };
        for (int[] c : clusters) {
            int cx = c[0], cz = c[1], length = c[2];
            double ex = (double) cx / RX, ez = (double) cz / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.78) continue;
            int bottomY = 42 + (int) (19.0 * t);
            for (int i = 0; i < length; i++) {
                int y      = bottomY - i;
                int spread = Math.max(0, (length - i - 1) / 2);
                fill(es, cx - spread, y, cz - spread,
                         cx + spread, y, cz + spread, BlockTypes.DEEPSLATE);
            }
        }

        // Gravel/stone patches on the underside surface
        for (int x = -22; x <= 22; x += 4) {
            for (int z = -20; z <= 20; z += 5) {
                double ex = (double) x / RX, ez = (double) z / RZ;
                double t  = ex * ex + ez * ez;
                if (t > 0.58) continue;
                int bottomY = 42 + (int) (19.0 * t);
                block(es, x, bottomY + 2, z, BlockTypes.GRAVEL);
                if ((x + z) % 8 == 0)
                    block(es, x, bottomY + 3, z, BlockTypes.TUFF);
            }
        }

        // Cobbled_deepslate patches at edge overhang
        for (int angle = 0; angle < 360; angle += 5) {
            double rad = Math.toRadians(angle);
            int wx = (int) Math.round((RX - 4) * Math.cos(rad));
            int wz = (int) Math.round((RZ - 4) * Math.sin(rad));
            double ex = (double) wx / RX, ez = (double) wz / RZ;
            double t  = ex * ex + ez * ez;
            int bottomY = 42 + (int) (19.0 * t);
            block(es, wx, bottomY + 4, wz, BlockTypes.COBBLED_DEEPSLATE);
            block(es, wx, bottomY + 5, wz, BlockTypes.STONE);
        }
    }

    // ============================================================================
    //  ②  HUB STRUCTURES
    // ============================================================================

    private void buildCentralPlaza(EditSession es) {
        // Circular polished-deepslate plaza, radius 13
        for (int x = -13; x <= 13; x++) {
            for (int z = -13; z <= 13; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 13.0) continue;

                boolean onQuartzRing = dist >= 9.5 && dist <= 10.5;
                if (onQuartzRing) {
                    block(es, x, SY, z, BlockTypes.QUARTZ_BRICKS);
                } else if (Math.abs(x) <= 7 && Math.abs(z) <= 7) {
                    // Inner area (under/around tower): smooth polished deepslate
                    block(es, x, SY, z, BlockTypes.POLISHED_DEEPSLATE);
                } else {
                    // Outer ring: checkerboard pattern
                    boolean alt = (Math.abs(x) + Math.abs(z)) % 2 == 0;
                    block(es, x, SY, z,
                          alt ? BlockTypes.POLISHED_DEEPSLATE : BlockTypes.DEEPSLATE_TILES);
                }
            }
        }

        // Second quartz accent ring at radius 6
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist >= 5.8 && dist <= 6.5)
                    block(es, x, SY, z, BlockTypes.CHISELED_QUARTZ_BLOCK);
            }
        }

        // 8 decorative pillars at radius ~11
        for (int a = 0; a < 8; a++) {
            double rad = Math.toRadians(a * 45.0);
            int px = (int) Math.round(11 * Math.cos(rad));
            int pz = (int) Math.round(11 * Math.sin(rad));
            block(es, px, SY,     pz, BlockTypes.DEEPSLATE_BRICKS);
            column(es, px, pz, SY + 1, SY + 4, BlockTypes.QUARTZ_PILLAR);
            block(es, px, SY + 5, pz, BlockTypes.POLISHED_DEEPSLATE);
            block(es, px, SY + 6, pz, BlockTypes.LANTERN);
            // Chain draping between adjacent pillars mid-point (decorative)
            double rad2 = Math.toRadians(a * 45.0 + 22.5);
            int mx = (int) Math.round(10 * Math.cos(rad2));
            int mz = (int) Math.round(10 * Math.sin(rad2));
            block(es, mx, SY + 5, mz, BlockTypes.CHAIN);
        }

        // Step ring around plaza edge (transitions island→plaza)
        for (int a = 0; a < 360; a += 2) {
            double rad = Math.toRadians(a);
            int sx = (int) Math.round(13.6 * Math.cos(rad));
            int sz = (int) Math.round(13.6 * Math.sin(rad));
            double ex = (double) sx / RX, ez = (double) sz / RZ;
            if (ex * ex + ez * ez > 0.96) continue;
            block(es, sx, SY, sz, BlockTypes.STONE_BRICKS);
        }

        // Central fountain between tower south face and plaza edge
        buildCentralFountain(es);
    }

    private void buildCentralFountain(EditSession es) {
        // Circular fountain basin west of tower: centre at (-10, SY, 0)
        int fx = -10, fz = 0;

        // Stone-brick rim, 5×5
        for (int x = fx - 2; x <= fx + 2; x++) {
            for (int z = fz - 2; z <= fz + 2; z++) {
                boolean edge = x == fx - 2 || x == fx + 2 || z == fz - 2 || z == fz + 2;
                if (edge) {
                    block(es, x, SY,     z, BlockTypes.STONE_BRICKS);
                    block(es, x, SY + 1, z, BlockTypes.STONE_BRICK_WALL);
                } else {
                    block(es, x, SY - 1, z, BlockTypes.WATER);
                    block(es, x, SY - 2, z, BlockTypes.PRISMARINE);
                    block(es, x, SY,     z, BlockTypes.AIR);
                }
            }
        }

        // Prismarine glow beneath water
        block(es, fx, SY - 2, fz, BlockTypes.SEA_LANTERN);

        // Central small pillar with water-source on top
        block(es, fx, SY,     fz, BlockTypes.POLISHED_ANDESITE);
        block(es, fx, SY + 1, fz, BlockTypes.WATER);

        // Rim corner accents
        for (int[] c : new int[][]{{fx-2,fz-2},{fx+2,fz-2},{fx-2,fz+2},{fx+2,fz+2}}) {
            block(es, c[0], SY + 2, c[1], BlockTypes.CHISELED_STONE_BRICKS);
        }
    }

    private void buildMainTower(EditSession es) {
        // 13×13 footprint (x=-6..6, z=-6..6), 36 blocks from SY up
        // Material: polished deepslate main walls, quartz accents, chiseled deepslate details

        // ── Foundation slab at SY ────────────────────────────────────────────
        fill(es, -6, SY, -6, 6, SY, 6, BlockTypes.DEEPSLATE_BRICKS);
        // Raised quartz border at SY on the foundation perimeter
        for (int x = -6; x <= 6; x++) {
            block(es, x, SY + 1, -6, BlockTypes.CHISELED_DEEPSLATE);
            block(es, x, SY + 1,  6, BlockTypes.CHISELED_DEEPSLATE);
        }
        for (int z = -5; z <= 5; z++) {
            block(es, -6, SY + 1, z, BlockTypes.CHISELED_DEEPSLATE);
            block(es,  6, SY + 1, z, BlockTypes.CHISELED_DEEPSLATE);
        }

        // ── Lower body  y = SY+2 .. SY+12  (2-block thick hollow walls) ────
        for (int y = SY + 2; y <= SY + 12; y++) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    boolean outer = (x == -6 || x == 6 || z == -6 || z == 6);
                    boolean inner = (x == -5 || x == 5 || z == -5 || z == 5);
                    if (outer) {
                        boolean quartz = (x + z + y) % 5 == 0;
                        block(es, x, y, z, quartz ? BlockTypes.QUARTZ_BRICKS
                                                  : BlockTypes.POLISHED_DEEPSLATE);
                    } else if (inner) {
                        block(es, x, y, z, BlockTypes.DEEPSLATE_BRICKS);
                    }
                }
            }
        }

        // Lower-body windows: 1×2 tinted glass on each face at x/z = -3, 0, +3
        for (int wx : new int[]{-3, 0, 3}) {
            for (int wy = SY + 5; wy <= SY + 6; wy++) {
                block(es, wx, wy, -6, BlockTypes.TINTED_GLASS);
                block(es, wx, wy,  6, BlockTypes.TINTED_GLASS);
                block(es, -6, wy, wx, BlockTypes.TINTED_GLASS);
                block(es,  6, wy, wx, BlockTypes.TINTED_GLASS);
            }
        }

        // Entryways: 3-wide × 3-tall openings on south (z=6) and north (z=-6)
        for (int ey = SY + 2; ey <= SY + 4; ey++) {
            for (int ex = -1; ex <= 1; ex++) {
                block(es, ex, ey,  6, BlockTypes.AIR);
                block(es, ex, ey, -6, BlockTypes.AIR);
            }
        }

        // ── Mid body  y = SY+13 .. SY+22  (1-block thick, quartz + deepslate) ─
        for (int y = SY + 13; y <= SY + 22; y++) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    if (x == -6 || x == 6 || z == -6 || z == 6) {
                        boolean quartz = (x + z) % 4 == 0;
                        block(es, x, y, z, quartz ? BlockTypes.CHISELED_QUARTZ_BLOCK
                                                  : BlockTypes.POLISHED_DEEPSLATE);
                    }
                }
            }
        }

        // Large 3-wide × 5-tall arched windows on mid body
        for (int wy = SY + 14; wy <= SY + 19; wy++) {
            for (int wx = -1; wx <= 1; wx++) {
                block(es, wx, wy, -6, BlockTypes.TINTED_GLASS);
                block(es, wx, wy,  6, BlockTypes.TINTED_GLASS);
                block(es, -6, wy, wx, BlockTypes.TINTED_GLASS);
                block(es,  6, wy, wx, BlockTypes.TINTED_GLASS);
            }
        }
        // Arched glass top: centre only at highest row
        block(es, 0, SY + 20, -6, BlockTypes.TINTED_GLASS);
        block(es, 0, SY + 20,  6, BlockTypes.TINTED_GLASS);
        block(es, -6, SY + 20, 0, BlockTypes.TINTED_GLASS);
        block(es,  6, SY + 20, 0, BlockTypes.TINTED_GLASS);

        // ── Upper tier  y = SY+23 .. SY+27 (narrows to 11×11 with quartz trim) ─
        for (int y = SY + 23; y <= SY + 27; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (x == -5 || x == 5 || z == -5 || z == 5) {
                        block(es, x, y, z, BlockTypes.DEEPSLATE_BRICKS);
                    }
                }
            }
        }

        // ── Observation floor at SY+27 ──────────────────────────────────────
        fill(es, -4, SY + 27, -4, 4, SY + 27, 4, BlockTypes.POLISHED_DEEPSLATE);
        // Quartz accent tiles in corners
        for (int[] corner : new int[][]{{-3,-3},{3,-3},{-3,3},{3,3}}) {
            block(es, corner[0], SY + 27, corner[1], BlockTypes.QUARTZ_BRICKS);
        }
        // Iron-bar railing
        for (int i = -5; i <= 5; i++) {
            block(es, i, SY + 28, -5, BlockTypes.IRON_BARS);
            block(es, i, SY + 28,  5, BlockTypes.IRON_BARS);
        }
        for (int i = -4; i <= 4; i++) {
            block(es, -5, SY + 28, i, BlockTypes.IRON_BARS);
            block(es,  5, SY + 28, i, BlockTypes.IRON_BARS);
        }

        // ── Battlements (merlons + crenels) at upper tier top ───────────────
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (x == -5 || x == 5 || z == -5 || z == 5) {
                    if ((x + z) % 2 == 0) {
                        block(es, x, SY + 28, z, BlockTypes.DEEPSLATE_BRICKS);
                        block(es, x, SY + 29, z, BlockTypes.DEEPSLATE_TILES);
                    }
                }
            }
        }

        // ── Four corner turrets: 3×3, rise from SY+1 to SY+33 ──────────────
        for (int[] c : new int[][]{{-6,-6},{6,-6},{-6,6},{6,6}}) {
            int tx = c[0], tz = c[1];
            int ox = tx < 0 ? 2 : -2, oz = tz < 0 ? 2 : -2;  // offsets toward centre
            for (int y = SY + 1; y <= SY + 33; y++) {
                for (int dx = 0; dx <= 2; dx++) {
                    for (int dz = 0; dz <= 2; dz++) {
                        int bx = tx + dx * (tx < 0 ? 1 : -1);
                        int bz = tz + dz * (tz < 0 ? 1 : -1);
                        boolean outer = dx == 0 || dx == 2 || dz == 0 || dz == 2;
                        if (outer) {
                            block(es, bx, y, bz, y % 4 == 0
                                    ? BlockTypes.QUARTZ_BRICKS
                                    : BlockTypes.CHISELED_DEEPSLATE);
                        }
                    }
                }
            }
            // Turret cap
            int capX = tx + (tx < 0 ? 1 : -1);
            int capZ = tz + (tz < 0 ? 1 : -1);
            fill(es, capX - 1, SY + 34, capZ - 1, capX + 1, SY + 34, capZ + 1,
                    BlockTypes.DEEPSLATE_BRICKS);
            block(es, capX, SY + 35, capZ, BlockTypes.LANTERN);
        }

        // ── Spire: y = SY+29 .. SY+38 ───────────────────────────────────────
        // Profile shrinks each 2 blocks
        int[][] spireProfile = {{-2,-2,2,2},{-2,-2,2,2},{-1,-1,1,1},{-1,-1,1,1},{0,0,0,0},{0,0,0,0}};
        for (int i = 0; i < spireProfile.length; i++) {
            int y = SY + 30 + i;
            int[] s = spireProfile[i];
            if (s[0] == s[2]) {
                column(es, s[0], s[1], y, y, BlockTypes.QUARTZ_PILLAR);
            } else {
                fill(es, s[0], y, s[1], s[2], y, s[3], BlockTypes.QUARTZ_PILLAR);
            }
        }
        // Tip: sea lantern on top of quartz pillar
        column(es, 0, 0, SY + 36, SY + 37, BlockTypes.QUARTZ_PILLAR);
        block(es, 0, SY + 38, 0, BlockTypes.SEA_LANTERN);
        block(es, 0, SY + 39, 0, BlockTypes.CHAIN);
        block(es, 0, SY + 40, 0, BlockTypes.LANTERN);
    }

    private void buildTowerInterior(EditSession es) {
        // ── Ground floor interior ────────────────────────────────────────────
        fill(es, -4, SY + 1, -4, 4, SY + 1, 4, BlockTypes.POLISHED_DEEPSLATE);

        // Corner sea-lanterns flush with floor
        block(es, -3, SY + 1, -3, BlockTypes.SEA_LANTERN);
        block(es,  3, SY + 1, -3, BlockTypes.SEA_LANTERN);
        block(es, -3, SY + 1,  3, BlockTypes.SEA_LANTERN);
        block(es,  3, SY + 1,  3, BlockTypes.SEA_LANTERN);

        // Central load-bearing pillar y+2..y+12
        column(es, 0, 0, SY + 2, SY + 12, BlockTypes.QUARTZ_PILLAR);
        block(es, 0, SY + 13, 0, BlockTypes.SEA_LANTERN);

        // 4 secondary pillars at ±3
        for (int[] pos : new int[][]{{-3,0},{3,0},{0,-3},{0,3}}) {
            column(es, pos[0], pos[1], SY + 2, SY + 9, BlockTypes.POLISHED_DEEPSLATE);
            block(es, pos[0], SY + 10, pos[1], BlockTypes.GLOWSTONE);
        }

        // Spiral staircase: one step per 4 ticks, rotates around central pillar
        for (int step = 0; step < 24; step++) {
            int y  = SY + 2 + step;
            int quad = step % 4;
            int[] sx = {-4, 4,  4, -4};
            int[] sz = {-4,-4,  4,  4};
            block(es, sx[quad], y, sz[quad], BlockTypes.POLISHED_DEEPSLATE);
            // Landing slab beside the stair
            int[] lx = {-3, 3,  3, -3};
            int[] lz = {-4,-3,  4,  3};
            block(es, lx[quad], y, lz[quad], BlockTypes.DEEPSLATE_TILES);
        }

        // ── Second floor landing at SY+13 ────────────────────────────────────
        fill(es, -3, SY + 13, -3, 3, SY + 13, 3, BlockTypes.DEEPSLATE_BRICKS);
        block(es, 0, SY + 14, 0, BlockTypes.SEA_LANTERN);

        // ── Observation floor details (SY+27) already placed in buildMainTower ──
        // Add central throne-like structure on obs floor
        fill(es, -1, SY + 28, -1, 1, SY + 28, 1, BlockTypes.QUARTZ_BRICKS);
        block(es,  0, SY + 29,  0, BlockTypes.BEACON);
        // Beacon base: iron blocks 3×3 at SY+27
        for (int bx = -1; bx <= 1; bx++)
            for (int bz = -1; bz <= 1; bz++)
                block(es, bx, SY + 27, bz, BlockTypes.IRON_BLOCK);
    }

    // ============================================================================
    //  ③  PRACTICE PORTALS
    // ============================================================================

    /**
     * Builds a freestanding 7-wide × 9-tall arch centred at (cx, SY, cz).
     * axisX=true  → arch aperture spans along X (player walks north/south through it).
     * axisX=false → arch aperture spans along Z (player walks east/west through it).
     */
    private void portalArch(EditSession es, int cx, int cz, boolean axisX,
                             BlockType pillar, BlockType arch, BlockType floor) {
        for (int off = -3; off <= 3; off++) {
            int ax = axisX ? cx + off : cx;
            int az = axisX ? cz       : cz + off;

            for (int y = SY; y <= SY + 9; y++) {
                boolean leftEdge  = off == -3;
                boolean rightEdge = off ==  3;

                if (leftEdge || rightEdge) {
                    // Full-height solid pillar, alternating materials every 2
                    block(es, ax, y, az, y % 2 == 0 ? pillar : arch);
                } else if (y == SY) {
                    block(es, ax, y, az, floor);
                } else if (y <= SY + 5) {
                    block(es, ax, y, az, BlockTypes.AIR);  // open walkway
                } else {
                    // Arch profile: wider openings at lower arch rows
                    int archRow = y - (SY + 6);            // 0..3
                    int halfOpen = 3 - archRow;            // 3,2,1,0
                    if (Math.abs(off) > halfOpen) {
                        block(es, ax, y, az, y % 2 == 0 ? arch : pillar);
                    } else {
                        block(es, ax, y, az, BlockTypes.AIR);
                    }
                }
            }
        }
        // Capstone + hanging lantern
        int topY = SY + 10;
        block(es, cx, topY,     cz, arch);
        block(es, cx, topY + 1, cz, BlockTypes.CHAIN);
        block(es, cx, topY + 2, cz, BlockTypes.LANTERN);
    }

    /** Crystal Practice Portal — East  (x=22, z=0).  End Stone / Amethyst theme */
    private void buildCrystalPortal(EditSession es) {
        int cx = 22, cz = 0;

        // Platform pad 7×7
        fill(es, cx - 3, SY, cz - 3, cx + 3, SY, cz + 3, BlockTypes.END_STONE_BRICKS);
        // Purpur inset border
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 3, BlockTypes.PURPUR_BLOCK);
            block(es, x, SY, cz + 3, BlockTypes.PURPUR_BLOCK);
        }
        for (int z = cz - 2; z <= cz + 2; z++) {
            block(es, cx - 3, SY, z, BlockTypes.PURPUR_BLOCK);
            block(es, cx + 3, SY, z, BlockTypes.PURPUR_BLOCK);
        }

        // Arch (faces Z-axis — player walks east through portal on X axis)
        portalArch(es, cx, cz, false,
                BlockTypes.PURPUR_PILLAR, BlockTypes.END_STONE_BRICKS,
                BlockTypes.END_STONE_BRICKS);

        // Amethyst accent blocks flanking pillar bases
        block(es, cx, SY + 1, cz - 3, BlockTypes.AMETHYST_BLOCK);
        block(es, cx, SY + 1, cz + 3, BlockTypes.AMETHYST_BLOCK);
        block(es, cx, SY + 2, cz - 3, BlockTypes.AMETHYST_CLUSTER);
        block(es, cx, SY + 2, cz + 3, BlockTypes.AMETHYST_CLUSTER);

        // Flanking obelisks
        column(es, cx - 6, cz - 2, SY + 1, SY + 6, BlockTypes.PURPUR_BLOCK);
        column(es, cx - 6, cz + 2, SY + 1, SY + 6, BlockTypes.PURPUR_BLOCK);
        block(es, cx - 6, SY + 7, cz - 2, BlockTypes.SEA_LANTERN);
        block(es, cx - 6, SY + 7, cz + 2, BlockTypes.SEA_LANTERN);

        // Glowstone header strip above arch
        fill(es, cx, SY + 11, cz - 3, cx, SY + 11, cz + 3, BlockTypes.GLOWSTONE);
        block(es, cx, SY + 12, cz, BlockTypes.AMETHYST_BLOCK);

        // Approach stepping stones toward plaza
        for (int x = cx - 4; x >= cx - 7; x--) {
            for (int z = cz - 1; z <= cz + 1; z++)
                block(es, x, SY, z, BlockTypes.POLISHED_DEEPSLATE);
        }
    }

    /** Sword Practice Portal — West  (x=-22, z=0).  Quartz / Iron theme */
    private void buildSwordPortal(EditSession es) {
        int cx = -22, cz = 0;

        fill(es, cx - 3, SY, cz - 3, cx + 3, SY, cz + 3, BlockTypes.SMOOTH_QUARTZ);
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 3, BlockTypes.QUARTZ_BRICKS);
            block(es, x, SY, cz + 3, BlockTypes.QUARTZ_BRICKS);
        }

        portalArch(es, cx, cz, false,
                BlockTypes.QUARTZ_PILLAR, BlockTypes.QUARTZ_BRICKS,
                BlockTypes.SMOOTH_QUARTZ);

        block(es, cx, SY + 1, cz - 3, BlockTypes.IRON_BLOCK);
        block(es, cx, SY + 1, cz + 3, BlockTypes.IRON_BLOCK);

        column(es, cx + 6, cz - 2, SY + 1, SY + 6, BlockTypes.QUARTZ_PILLAR);
        column(es, cx + 6, cz + 2, SY + 1, SY + 6, BlockTypes.QUARTZ_PILLAR);
        block(es, cx + 6, SY + 7, cz - 2, BlockTypes.SEA_LANTERN);
        block(es, cx + 6, SY + 7, cz + 2, BlockTypes.SEA_LANTERN);

        fill(es, cx, SY + 11, cz - 3, cx, SY + 11, cz + 3, BlockTypes.GLOWSTONE);
        block(es, cx, SY + 12, cz, BlockTypes.IRON_BLOCK);

        for (int x = cx + 4; x <= cx + 7; x++) {
            for (int z = cz - 1; z <= cz + 1; z++)
                block(es, x, SY, z, BlockTypes.POLISHED_DEEPSLATE);
        }
    }

    /** Mace Practice Portal — South  (x=0, z=22).  Nether Brick / Fire theme */
    private void buildMacePortal(EditSession es) {
        int cx = 0, cz = 22;

        fill(es, cx - 3, SY, cz - 3, cx + 3, SY, cz + 3, BlockTypes.NETHER_BRICKS);
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx - 3, SY, z, BlockTypes.RED_NETHER_BRICKS);
            block(es, cx + 3, SY, z, BlockTypes.RED_NETHER_BRICKS);
        }

        portalArch(es, cx, cz, true,
                BlockTypes.RED_NETHER_BRICKS, BlockTypes.NETHER_BRICKS,
                BlockTypes.NETHER_BRICKS);

        block(es, cx - 3, SY + 1, cz, BlockTypes.MAGMA_BLOCK);
        block(es, cx + 3, SY + 1, cz, BlockTypes.MAGMA_BLOCK);

        column(es, cx - 2, cz + 6, SY + 1, SY + 6, BlockTypes.RED_NETHER_BRICKS);
        column(es, cx + 2, cz + 6, SY + 1, SY + 6, BlockTypes.RED_NETHER_BRICKS);
        block(es, cx - 2, SY + 7, cz + 6, BlockTypes.GLOWSTONE);
        block(es, cx + 2, SY + 7, cz + 6, BlockTypes.GLOWSTONE);

        fill(es, cx - 3, SY + 11, cz, cx + 3, SY + 11, cz, BlockTypes.GLOWSTONE);
        block(es, cx, SY + 12, cz, BlockTypes.MAGMA_BLOCK);

        // Soul sand / soul soil accent
        block(es, cx - 4, SY, cz + 2, BlockTypes.SOUL_SAND);
        block(es, cx + 4, SY, cz + 2, BlockTypes.SOUL_SAND);
        block(es, cx - 4, SY, cz - 2, BlockTypes.SOUL_SOIL);
        block(es, cx + 4, SY, cz - 2, BlockTypes.SOUL_SOIL);

        for (int z = cz - 4; z >= cz - 7; z--) {
            for (int x = cx - 1; x <= cx + 1; x++)
                block(es, x, SY, z, BlockTypes.POLISHED_DEEPSLATE);
        }
    }

    /** Bow Practice Portal — North  (x=0, z=-22).  Dark Oak / Mossy Stone theme */
    private void buildBowPortal(EditSession es) {
        int cx = 0, cz = -22;

        fill(es, cx - 3, SY, cz - 3, cx + 3, SY, cz + 3, BlockTypes.MOSSY_STONE_BRICKS);
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx - 3, SY, z, BlockTypes.MOSSY_COBBLESTONE);
            block(es, cx + 3, SY, z, BlockTypes.MOSSY_COBBLESTONE);
        }

        portalArch(es, cx, cz, true,
                BlockTypes.DARK_OAK_LOG, BlockTypes.MOSSY_COBBLESTONE,
                BlockTypes.MOSSY_STONE_BRICKS);

        block(es, cx - 3, SY + 1, cz, BlockTypes.OAK_LOG);
        block(es, cx + 3, SY + 1, cz, BlockTypes.OAK_LOG);

        column(es, cx - 2, cz - 6, SY + 1, SY + 6, BlockTypes.DARK_OAK_LOG);
        column(es, cx + 2, cz - 6, SY + 1, SY + 6, BlockTypes.DARK_OAK_LOG);
        block(es, cx - 2, SY + 7, cz - 6, BlockTypes.LANTERN);
        block(es, cx + 2, SY + 7, cz - 6, BlockTypes.LANTERN);

        // Leaf overhang above arch
        fill(es, cx - 2, SY + 13, cz - 2, cx + 2, SY + 13, cz + 2, BlockTypes.DARK_OAK_LEAVES);
        fill(es, cx - 1, SY + 14, cz - 1, cx + 1, SY + 14, cz + 1, BlockTypes.DARK_OAK_LEAVES);

        fill(es, cx - 3, SY + 11, cz, cx + 3, SY + 11, cz, BlockTypes.GLOWSTONE);
        block(es, cx, SY + 12, cz, BlockTypes.DARK_OAK_LOG);

        for (int z = cz + 4; z <= cz + 7; z++) {
            for (int x = cx - 1; x <= cx + 1; x++)
                block(es, x, SY, z, BlockTypes.POLISHED_DEEPSLATE);
        }
    }

    /** Totem Practice Portal — NE  (x=16, z=-16).  Jungle / Nature theme */
    private void buildTotemPortal(EditSession es) {
        int cx = 16, cz = -16;

        fill(es, cx - 3, SY, cz - 3, cx + 3, SY, cz + 3, BlockTypes.JUNGLE_PLANKS);
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 3, BlockTypes.JUNGLE_LOG);
            block(es, x, SY, cz + 3, BlockTypes.JUNGLE_LOG);
        }
        for (int z = cz - 2; z <= cz + 2; z++) {
            block(es, cx - 3, SY, z, BlockTypes.JUNGLE_LOG);
            block(es, cx + 3, SY, z, BlockTypes.JUNGLE_LOG);
        }

        portalArch(es, cx, cz, true,
                BlockTypes.JUNGLE_LOG, BlockTypes.MOSSY_STONE_BRICKS,
                BlockTypes.JUNGLE_PLANKS);

        block(es, cx - 3, SY + 1, cz, BlockTypes.JUNGLE_LOG);
        block(es, cx + 3, SY + 1, cz, BlockTypes.JUNGLE_LOG);

        column(es, cx - 2, cz - 6, SY + 1, SY + 6, BlockTypes.JUNGLE_LOG);
        column(es, cx + 2, cz - 6, SY + 1, SY + 6, BlockTypes.JUNGLE_LOG);
        block(es, cx - 2, SY + 7, cz - 6, BlockTypes.LANTERN);
        block(es, cx + 2, SY + 7, cz - 6, BlockTypes.LANTERN);

        // Jungle leaf canopy over arch
        fill(es, cx - 3, SY + 13, cz - 3, cx + 3, SY + 13, cz + 3, BlockTypes.JUNGLE_LEAVES);
        fill(es, cx - 2, SY + 14, cz - 2, cx + 2, SY + 14, cz + 2, BlockTypes.JUNGLE_LEAVES);
        fill(es, cx - 1, SY + 15, cz - 1, cx + 1, SY + 15, cz + 1, BlockTypes.JUNGLE_LEAVES);

        fill(es, cx - 3, SY + 11, cz, cx + 3, SY + 11, cz, BlockTypes.GLOWSTONE);

        // Diagonal approach path (NE to SW diagonal)
        for (int i = 1; i <= 5; i++) {
            int px = cx - i, pz = cz + i;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz, BlockTypes.POLISHED_DEEPSLATE);
                block(es, px, SY, pz + d, BlockTypes.POLISHED_DEEPSLATE);
            }
        }
    }

    // ============================================================================
    //  ④  CONNECTIVITY
    // ============================================================================

    private void buildGrandStaircase(EditSession es) {
        // 3-wide stone staircase descending from plaza level to the surrounding island
        // on each of the 4 cardinal approaches (before the portals)

        // East staircase at x=14: descend from SY down 3 blocks to the east
        for (int step = 0; step < 3; step++) {
            int bx = 14 + step;
            for (int z = -2; z <= 2; z++) {
                block(es, bx, SY - step, z, BlockTypes.STONE_BRICKS);
            }
        }
        // West staircase at x=-14
        for (int step = 0; step < 3; step++) {
            int bx = -14 - step;
            for (int z = -2; z <= 2; z++) {
                block(es, bx, SY - step, z, BlockTypes.STONE_BRICKS);
            }
        }
        // South staircase at z=14
        for (int step = 0; step < 3; step++) {
            int bz = 14 + step;
            for (int x = -2; x <= 2; x++) {
                block(es, x, SY - step, bz, BlockTypes.STONE_BRICKS);
            }
        }
        // North staircase at z=-14
        for (int step = 0; step < 3; step++) {
            int bz = -14 - step;
            for (int x = -2; x <= 2; x++) {
                block(es, x, SY - step, bz, BlockTypes.STONE_BRICKS);
            }
        }

        // Staircase railings: oak fence posts at step edges
        for (int step = 0; step < 3; step++) {
            int bx = 14 + step;
            column(es, bx, -2, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
            column(es, bx,  2, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
        }
        for (int step = 0; step < 3; step++) {
            int bx = -14 - step;
            column(es, bx, -2, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
            column(es, bx,  2, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
        }
    }

    private void buildPathways(EditSession es) {
        // 3-wide stone-brick paths, plaza edge → portal pad
        // East  (x=14..21)
        for (int x = 14; x <= 21; x++)
            for (int z = -1; z <= 1; z++) block(es, x, SY, z, BlockTypes.STONE_BRICKS);

        // West  (x=-14..-21)
        for (int x = -14; x >= -21; x--)
            for (int z = -1; z <= 1; z++) block(es, x, SY, z, BlockTypes.STONE_BRICKS);

        // South (z=14..21)
        for (int z = 14; z <= 21; z++)
            for (int x = -1; x <= 1; x++) block(es, x, SY, z, BlockTypes.STONE_BRICKS);

        // North (z=-14..-21)
        for (int z = -14; z >= -21; z--)
            for (int x = -1; x <= 1; x++) block(es, x, SY, z, BlockTypes.STONE_BRICKS);

        // NE diagonal → Totem Portal  (staggered 3-wide)
        for (int i = 0; i <= 6; i++) {
            int px = 10 + i, pz = -10 - i;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
        }

        // Cobblestone kerb strips along path edges  (±2 from centre)
        for (int x = 14; x <= 21; x++) {
            block(es, x, SY, -2, BlockTypes.COBBLESTONE);
            block(es, x, SY,  2, BlockTypes.COBBLESTONE);
        }
        for (int x = -14; x >= -21; x--) {
            block(es, x, SY, -2, BlockTypes.COBBLESTONE);
            block(es, x, SY,  2, BlockTypes.COBBLESTONE);
        }
        for (int z = 14; z <= 21; z++) {
            block(es, -2, SY, z, BlockTypes.COBBLESTONE);
            block(es,  2, SY, z, BlockTypes.COBBLESTONE);
        }
        for (int z = -14; z >= -21; z--) {
            block(es, -2, SY, z, BlockTypes.COBBLESTONE);
            block(es,  2, SY, z, BlockTypes.COBBLESTONE);
        }

        // Mossy accent tiles every 4 blocks along paths
        for (int x = 16; x <= 21; x += 4) {
            block(es, x, SY, 0, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, -x, SY, 0, BlockTypes.MOSSY_STONE_BRICKS);
        }
        for (int z = 16; z <= 21; z += 4) {
            block(es, 0, SY, z, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, 0, SY, -z, BlockTypes.MOSSY_STONE_BRICKS);
        }
    }

    private void buildBridges(EditSession es) {
        // ── Bridge 1: east path crosses small stream at x=12, z=-6..6 ──────
        for (int z = -6; z <= 6; z++) {
            block(es, 12, SY, z, BlockTypes.SPRUCE_PLANKS);
        }
        for (int z = -6; z <= 6; z++) {
            if (Math.abs(z) > 1) {  // railing only outside main path
                block(es, 12, SY + 1, z, BlockTypes.OAK_FENCE);
                block(es, 12, SY + 2, z, BlockTypes.OAK_FENCE);
            }
        }
        // Bridge support columns
        block(es, 12, SY - 1, -4, BlockTypes.OAK_LOG);
        block(es, 12, SY - 1,  0, BlockTypes.OAK_LOG);
        block(es, 12, SY - 1,  4, BlockTypes.OAK_LOG);
        // Bridge end lanterns
        block(es, 12, SY + 3, -6, BlockTypes.LANTERN);
        block(es, 12, SY + 3,  6, BlockTypes.LANTERN);

        // ── Bridge 2: north path crosses stream at z=-12, x=-6..6 ──────────
        for (int x = -6; x <= 6; x++) {
            block(es, x, SY, -12, BlockTypes.SPRUCE_PLANKS);
        }
        for (int x = -6; x <= 6; x++) {
            if (Math.abs(x) > 1) {
                block(es, x, SY + 1, -12, BlockTypes.OAK_FENCE);
                block(es, x, SY + 2, -12, BlockTypes.OAK_FENCE);
            }
        }
        block(es, -4, SY - 1, -12, BlockTypes.OAK_LOG);
        block(es,  0, SY - 1, -12, BlockTypes.OAK_LOG);
        block(es,  4, SY - 1, -12, BlockTypes.OAK_LOG);
        block(es, -6, SY + 3, -12, BlockTypes.LANTERN);
        block(es,  6, SY + 3, -12, BlockTypes.LANTERN);

        // ── Small rope-style bridge over west stream at x=-12 ───────────────
        for (int z = -4; z <= 4; z++) {
            block(es, -12, SY, z, BlockTypes.SPRUCE_PLANKS);
            if (Math.abs(z) > 1) {
                column(es, -12, z, SY + 1, SY + 2, BlockTypes.OAK_FENCE);
            }
        }
        block(es, -12, SY - 1, 0, BlockTypes.OAK_LOG);
        block(es, -12, SY + 3, -4, BlockTypes.LANTERN);
        block(es, -12, SY + 3,  4, BlockTypes.LANTERN);
    }

    // ============================================================================
    //  ⑤  NATURE
    // ============================================================================

    private void buildForestZone(EditSession es) {
        // NE quadrant — x=18..40, z=-36..2
        buildOakTree    (es,  24, -26, 7);
        buildOakTree    (es,  32, -16, 6);
        buildOakTree    (es,  38,  -6, 8);
        buildOakTree    (es,  26,   0, 5);
        buildDarkOakTree(es,  20, -12, 7);
        buildDarkOakTree(es,  34, -26, 8);
        buildBirchTree  (es,  18, -34, 9);
        buildBirchTree  (es,  28, -20, 7);
        buildBirchTree  (es,  36,   2, 6);
        buildCherryTree (es,  22, -30, 6);
        buildCherryTree (es,  30,  -8, 7);
        buildCherryTree (es,  40, -18, 5);

        // Undergrowth ferns
        int[][] ferns = {
            {25,-24},{31,-18},{37,-11},{23, -9},
            {32,-28},{35, -4},{21,-16},{29, -5},
            {39,-22},{27,-32},{24,  0},{36,-14}
        };
        for (int[] f : ferns) {
            double ex = (double)f[0]/RX, ez = (double)f[1]/RZ;
            if (ex*ex + ez*ez > 0.9) continue;
            block(es, f[0],     SY + 1, f[1],     BlockTypes.FERN);
            block(es, f[0] + 1, SY + 1, f[1],     BlockTypes.FERN);
            block(es, f[0],     SY + 1, f[1] + 1, BlockTypes.SHORT_GRASS);
        }

        // Moss patches at tree bases
        int[][] moss = {{24,-27},{33,-15},{38,-8},{27,1},{21,-11},{35,-26}};
        for (int[] m : moss) {
            fill(es, m[0]-1, SY, m[1]-1, m[0]+1, SY, m[1]+1, BlockTypes.MOSS_BLOCK);
            fill(es, m[0]-1, SY+1, m[1]-1, m[0]+1, SY+1, m[1]+1, BlockTypes.MOSS_CARPET);
        }

        // Mushrooms
        block(es, 26, SY + 1, -15, BlockTypes.RED_MUSHROOM);
        block(es, 35, SY + 1, -22, BlockTypes.BROWN_MUSHROOM);
        block(es, 29, SY + 1,  -7, BlockTypes.RED_MUSHROOM);
        block(es, 22, SY + 1, -28, BlockTypes.BROWN_MUSHROOM);

        // Azalea bushes on forest edge
        block(es, 18, SY + 1,  -8, BlockTypes.AZALEA);
        block(es, 19, SY + 1,  -6, BlockTypes.FLOWERING_AZALEA);
        block(es, 30, SY + 1,   2, BlockTypes.AZALEA);
        block(es, 24, SY + 1, -16, BlockTypes.FLOWERING_AZALEA);
    }

    private void buildOakTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY + 1, SY + h, BlockTypes.OAK_LOG);
        int top = SY + h;
        // Bottom leaf layer r=3
        for (int x = tx-3; x <= tx+3; x++)
            for (int z = tz-3; z <= tz+3; z++)
                if ((x-tx)*(x-tx)+(z-tz)*(z-tz) <= 10)
                    block(es, x, top, z, BlockTypes.OAK_LEAVES);
        // Mid layer r=2
        for (int x = tx-2; x <= tx+2; x++)
            for (int z = tz-2; z <= tz+2; z++)
                if ((x-tx)*(x-tx)+(z-tz)*(z-tz) <= 6)
                    block(es, x, top+1, z, BlockTypes.OAK_LEAVES);
        // Top puff
        for (int x = tx-1; x <= tx+1; x++)
            for (int z = tz-1; z <= tz+1; z++)
                block(es, x, top+2, z, BlockTypes.OAK_LEAVES);
        block(es, tx, top+3, tz, BlockTypes.OAK_LEAVES);
        // Hanging leaf detail
        block(es, tx-1, top-1, tz,   BlockTypes.OAK_LEAVES);
        block(es, tx,   top-1, tz+1, BlockTypes.OAK_LEAVES);
    }

    private void buildDarkOakTree(EditSession es, int tx, int tz, int h) {
        // 2×2 trunk
        for (int ox = 0; ox <= 1; ox++)
            for (int oz = 0; oz <= 1; oz++)
                column(es, tx+ox, tz+oz, SY+1, SY+h, BlockTypes.DARK_OAK_LOG);
        int top = SY + h;
        // Wide flat canopy r=5, 2 layers
        for (int x = tx-5; x <= tx+5; x++)
            for (int z = tz-5; z <= tz+5; z++)
                if ((x-tx-0.5)*(x-tx-0.5)+(z-tz-0.5)*(z-tz-0.5) <= 28)
                    block(es, x, top, z, BlockTypes.DARK_OAK_LEAVES);
        for (int x = tx-3; x <= tx+3; x++)
            for (int z = tz-3; z <= tz+3; z++)
                if ((x-tx-0.5)*(x-tx-0.5)+(z-tz-0.5)*(z-tz-0.5) <= 14)
                    block(es, x, top+1, z, BlockTypes.DARK_OAK_LEAVES);
        // Branch logs
        block(es, tx-2, top, tz,   BlockTypes.DARK_OAK_LOG);
        block(es, tx+3, top, tz,   BlockTypes.DARK_OAK_LOG);
        block(es, tx,   top, tz-2, BlockTypes.DARK_OAK_LOG);
        block(es, tx+1, top, tz+3, BlockTypes.DARK_OAK_LOG);
    }

    private void buildBirchTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY+1, SY+h, BlockTypes.BIRCH_LOG);
        int top = SY + h;
        // Narrow upright canopy
        for (int y = top-1; y <= top+2; y++) {
            int r = (y <= top) ? 2 : 1;
            for (int x = tx-r; x <= tx+r; x++)
                for (int z = tz-r; z <= tz+r; z++)
                    if (Math.abs(x-tx)+Math.abs(z-tz) <= r+1)
                        block(es, x, y, z, BlockTypes.BIRCH_LEAVES);
        }
        block(es, tx, top+3, tz, BlockTypes.BIRCH_LEAVES);
    }

    private void buildCherryTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY+1, SY+h, BlockTypes.CHERRY_LOG);
        int top = SY + h;
        // Fluffy round cherry canopy
        for (int dy = -1; dy <= 2; dy++) {
            int r = dy == -1 ? 2 : dy == 0 ? 3 : dy == 1 ? 3 : 2;
            for (int x = tx-r; x <= tx+r; x++)
                for (int z = tz-r; z <= tz+r; z++)
                    if ((x-tx)*(x-tx)+(z-tz)*(z-tz) <= r*r)
                        block(es, x, top+dy, z, BlockTypes.CHERRY_LEAVES);
        }
        block(es, tx, top+3, tz, BlockTypes.CHERRY_LEAVES);
        // Diagonal branch logs
        block(es, tx+1, top-1, tz,   BlockTypes.CHERRY_LOG);
        block(es, tx-1, top-1, tz+1, BlockTypes.CHERRY_LOG);
    }

    private void buildWaterGardens(EditSession es) {
        // SW quadrant — x=-16..-36, z=8..32

        // ── Main garden pool: 9×7 centred at (-24, 63, 20) ──────────────────
        int px = -24, pz = 20;
        for (int x = px-4; x <= px+4; x++) {
            for (int z = pz-3; z <= pz+3; z++) {
                boolean edge = x==px-4||x==px+4||z==pz-3||z==pz+3;
                if (edge) {
                    block(es, x, SY,     z, BlockTypes.STONE_BRICKS);
                    block(es, x, SY + 1, z, BlockTypes.STONE_BRICK_WALL);
                } else {
                    block(es, x, SY - 1, z, BlockTypes.WATER);
                    block(es, x, SY - 2, z, BlockTypes.PRISMARINE);
                    block(es, x, SY,     z, BlockTypes.AIR);
                }
            }
        }

        // Lily pads
        for (int[] l : new int[][]{{px-2,pz-1},{px,pz+1},{px+2,pz},{px-1,pz+2},{px+3,pz-1}})
            block(es, l[0], SY, l[1], BlockTypes.LILY_PAD);

        // Sea lantern in pool floor
        block(es, px,   SY-2, pz,   BlockTypes.SEA_LANTERN);
        block(es, px-2, SY-2, pz+1, BlockTypes.PRISMARINE_BRICKS);
        block(es, px+2, SY-2, pz-1, BlockTypes.PRISMARINE_BRICKS);

        // Prism column fountain in pool centre
        block(es, px, SY - 1, pz, BlockTypes.PRISMARINE);
        block(es, px, SY,     pz, BlockTypes.WATER);

        // Mossy stone rim corners
        for (int[] c : new int[][]{{px-4,pz-3},{px+4,pz-3},{px-4,pz+3},{px+4,pz+3}})
            block(es, c[0], SY+2, c[1], BlockTypes.MOSSY_STONE_BRICKS);

        // ── Small secondary pool: 5×5 at (-30, 63, 28) ──────────────────────
        int sx = -30, sz = 28;
        for (int x = sx-2; x <= sx+2; x++) {
            for (int z = sz-2; z <= sz+2; z++) {
                boolean edge = x==sx-2||x==sx+2||z==sz-2||z==sz+2;
                if (edge) {
                    block(es, x, SY, z, BlockTypes.MOSSY_STONE_BRICKS);
                } else {
                    block(es, x, SY-1, z, BlockTypes.WATER);
                    block(es, x, SY-2, z, BlockTypes.DARK_PRISMARINE);
                    block(es, x, SY,   z, BlockTypes.AIR);
                }
            }
        }
        block(es, sx, SY-2, sz, BlockTypes.SEA_LANTERN);

        // ── Stream: main pool → secondary pool ──────────────────────────────
        for (int x = px; x >= sx; x--) {
            int curZ = pz + 4 + (int)(((sx - x) / (double)(sx - px)) * (sz - pz - 4));
            block(es, x, SY-1, curZ,   BlockTypes.WATER);
            block(es, x, SY-2, curZ,   BlockTypes.STONE_BRICKS);
            block(es, x, SY,   curZ,   BlockTypes.AIR);
        }

        // ── Waterfall from elevated point into main pool ─────────────────────
        for (int y = SY; y >= SY-4; y--) {
            block(es, -36, y, 16, BlockTypes.WATER);
            block(es, -36, y, 17, BlockTypes.WATER);
        }
        block(es, -36, SY+1, 16, BlockTypes.STONE_BRICKS);
        block(es, -36, SY+1, 17, BlockTypes.STONE_BRICKS);

        // ── Decorative rocks ─────────────────────────────────────────────────
        BlockType[] rMat = {BlockTypes.MOSSY_COBBLESTONE, BlockTypes.COBBLESTONE,
                            BlockTypes.STONE, BlockTypes.ANDESITE};
        int[][] rocks = {{px-5,pz+1},{px-5,pz-2},{px+5,pz},{sx-3,sz+3},{sx+3,sz-3}};
        for (int i = 0; i < rocks.length; i++) {
            block(es, rocks[i][0],   SY+1, rocks[i][1],   rMat[i % rMat.length]);
            block(es, rocks[i][0]+1, SY+1, rocks[i][1],   rMat[(i+1)%rMat.length]);
            block(es, rocks[i][0],   SY+2, rocks[i][1],   rMat[(i+2)%rMat.length]);
        }

        // Trees near garden
        buildOakTree(es, -20, 24, 5);
        buildBirchTree(es, -32, 14, 6);
        buildCherryTree(es, -26, 30, 5);
    }

    private void buildVegetation(EditSession es) {
        // Flower patch 1 — oxeye daisy, east corridor
        for (int[] p : new int[][]{{15,-5},{16,-6},{17,-4},{18,-5},{16,-3},{15,-7}})
            block(es, p[0], SY+1, p[1], BlockTypes.OXEYE_DAISY);

        // Flower patch 2 — cornflower, west
        for (int[] p : new int[][]{{-16,8},{-18,6},{-17,10},{-19,8},{-15,9}})
            block(es, p[0], SY+1, p[1], BlockTypes.CORNFLOWER);

        // Flower patch 3 — dandelion + allium, south
        for (int[] p : new int[][]{{10,14},{12,15},{8,16},{11,17},{9,13}}) {
            block(es, p[0],   SY+1, p[1], BlockTypes.DANDELION);
            block(es, p[0]+1, SY+1, p[1], BlockTypes.ALLIUM);
        }

        // Flower patch 4 — blue orchid + lily near water
        for (int[] p : new int[][]{{-18,14},{-20,16},{-22,12},{-16,18}}) {
            block(es, p[0],   SY+1, p[1], BlockTypes.BLUE_ORCHID);
            block(es, p[0]-1, SY+1, p[1], BlockTypes.LILY_OF_THE_VALLEY);
        }

        // Flower patch 5 — poppy + azure_bluet, north island
        for (int[] p : new int[][]{{10,-28},{12,-30},{8,-26},{14,-28}}) {
            double ex = (double)p[0]/RX, ez = (double)p[1]/RZ;
            if (ex*ex+ez*ez > 0.9) continue;
            block(es, p[0],   SY+1, p[1], BlockTypes.POPPY);
            block(es, p[0]+1, SY+1, p[1], BlockTypes.AZURE_BLUET);
        }

        // Short grass tufts scattered
        int[][] grassPos = {
            {20,14},{-18,-10},{-10,26},{30, 4},{-30,-6},{10,-28},
            {38,-16},{-38, 12},{5,30},{-5,-30},{40,0},{-40,0},
            {15, 8},{-12,-8},{22, 16},{-22,-14},{4,34},{-4,-34}
        };
        for (int[] g : grassPos) {
            double ex = (double)g[0]/RX, ez = (double)g[1]/RZ;
            if (ex*ex+ez*ez > 0.90) continue;
            block(es, g[0],   SY+1, g[1],   BlockTypes.SHORT_GRASS);
            block(es, g[0]+1, SY+1, g[1],   BlockTypes.SHORT_GRASS);
            block(es, g[0],   SY+1, g[1]+1, BlockTypes.FERN);
        }

        // Azalea line on forest/water border
        int[][] azaleas = {{18,-8},{20,5},{-12,16},{-8,24},{30,-4},{-24,8}};
        for (int[] a : azaleas) {
            block(es, a[0],   SY+1, a[1],   BlockTypes.AZALEA);
            block(es, a[0]+1, SY+1, a[1]+1, BlockTypes.FLOWERING_AZALEA);
        }

        // Spore blossoms on underside of island overhangs (bottom face)
        int[][] blossoms = {{26,-22},{-20,18},{20,22},{-26,-14},{8,-34},{-8,34}};
        for (int[] b : blossoms) {
            double ex = (double)b[0]/RX, ez = (double)b[1]/RZ;
            if (ex*ex+ez*ez > 0.88) continue;
            block(es, b[0], SY-1, b[1], BlockTypes.SPORE_BLOSSOM);
        }

        // Hanging roots at underside mid-section
        int[][] hRoots = {{6,8},{-6,10},{12,4},{-10,6},{8,-6},{-12,-4},{4,16},{-4,-14}};
        for (int[] hr : hRoots) {
            double ex = (double)hr[0]/RX, ez = (double)hr[1]/RZ;
            double t  = ex*ex+ez*ez;
            if (t > 0.68) continue;
            int botY = 42 + (int)(19.0*t);
            block(es, hr[0], botY+4, hr[1], BlockTypes.HANGING_ROOTS);
            block(es, hr[0], botY+5, hr[1], BlockTypes.HANGING_ROOTS);
        }

        // Big dripleafs near water gardens
        block(es, -18, SY+1, 18, BlockTypes.BIG_DRIPLEAF);
        block(es, -22, SY+1, 14, BlockTypes.BIG_DRIPLEAF);
        block(es, -28, SY+1, 22, BlockTypes.BIG_DRIPLEAF);
    }

    // ============================================================================
    //  ⑥  FORTIFICATIONS
    // ============================================================================

    private void buildPerimeterWalls(EditSession es) {
        // Stone-brick wall follows island ellipse at ~87% radius
        double wRX = 38.0, wRZ = 33.0;
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int wx = (int) Math.round(wRX * Math.cos(rad));
            int wz = (int) Math.round(wRZ * Math.sin(rad));
            double ex = (double)wx/RX, ez = (double)wz/RZ;
            if (ex*ex+ez*ez > 0.93) continue;

            block(es, wx, SY,     wz, BlockTypes.STONE_BRICKS);
            block(es, wx, SY + 1, wz, BlockTypes.STONE_BRICKS);
            block(es, wx, SY + 2, wz, BlockTypes.STONE_BRICKS);
            if (angle % 5 < 3) {
                block(es, wx, SY + 3, wz, BlockTypes.STONE_BRICKS);
                block(es, wx, SY + 4, wz, BlockTypes.STONE_BRICK_WALL);
            }
            if (angle % 9 == 0)
                block(es, wx, SY + 1, wz, BlockTypes.MOSSY_STONE_BRICKS);
        }

        // Clearings aligned with each portal direction (5-wide gaps)
        // East portal gap (x~38, z~0)
        for (int gz = -3; gz <= 3; gz++)
            for (int gy = SY; gy <= SY+4; gy++)
                for (int gx = 36; gx <= 40; gx++)
                    block(es, gx, gy, gz, BlockTypes.AIR);

        // West portal gap
        for (int gz = -3; gz <= 3; gz++)
            for (int gy = SY; gy <= SY+4; gy++)
                for (int gx = -40; gx <= -36; gx++)
                    block(es, gx, gy, gz, BlockTypes.AIR);

        // South portal gap
        for (int gx = -3; gx <= 3; gx++)
            for (int gy = SY; gy <= SY+4; gy++)
                for (int gz = 31; gz <= 35; gz++)
                    block(es, gx, gy, gz, BlockTypes.AIR);

        // North portal gap
        for (int gx = -3; gx <= 3; gx++)
            for (int gy = SY; gy <= SY+4; gy++)
                for (int gz = -35; gz <= -31; gz++)
                    block(es, gx, gy, gz, BlockTypes.AIR);

        // NE totem gap (diagonal — approximate)
        for (int gy = SY; gy <= SY+4; gy++) {
            for (int d = -3; d <= 3; d++) {
                block(es, 28+d, gy, -24, BlockTypes.AIR);
                block(es, 28,   gy, -24+d, BlockTypes.AIR);
            }
        }
    }

    private void buildWatchtowers(EditSession es) {
        buildSingleWatchtower(es,  32,  26);
        buildSingleWatchtower(es, -32,  26);
        buildSingleWatchtower(es,  32, -26);
        buildSingleWatchtower(es, -32, -26);
    }

    private void buildSingleWatchtower(EditSession es, int cx, int cz) {
        double ex = (double)cx/RX, ez = (double)cz/RZ;
        if (ex*ex+ez*ez > 0.91) return;

        // ── 5×5 hollow body, y=SY..SY+16 ─────────────────────────────────────
        for (int y = SY; y <= SY+16; y++) {
            for (int x = cx-2; x <= cx+2; x++) {
                for (int z = cz-2; z <= cz+2; z++) {
                    if (x==cx-2||x==cx+2||z==cz-2||z==cz+2) {
                        block(es, x, y, z, y%4==0
                                ? BlockTypes.CHISELED_STONE_BRICKS
                                : BlockTypes.STONE_BRICKS);
                    }
                }
            }
        }

        // Doorway inward (facing origin)
        int doorX = cx + (cx < 0 ? 2 : -2);
        block(es, doorX, SY+1, cz, BlockTypes.AIR);
        block(es, doorX, SY+2, cz, BlockTypes.AIR);

        // ── Interior floor SY+1 ───────────────────────────────────────────────
        fill(es, cx-1, SY+1, cz-1, cx+1, SY+1, cz+1, BlockTypes.POLISHED_DEEPSLATE);
        block(es, cx, SY+2, cz, BlockTypes.SEA_LANTERN);

        // ── Arrow slits at y=SY+6..7, each face ──────────────────────────────
        for (int wy = SY+6; wy <= SY+7; wy++) {
            block(es, cx,     wy, cz-2, BlockTypes.AIR);
            block(es, cx,     wy, cz+2, BlockTypes.AIR);
            block(es, cx-2,   wy, cz,   BlockTypes.AIR);
            block(es, cx+2,   wy, cz,   BlockTypes.AIR);
        }

        // ── Upper platform at SY+16 ───────────────────────────────────────────
        fill(es, cx-2, SY+16, cz-2, cx+2, SY+16, cz+2, BlockTypes.POLISHED_DEEPSLATE);
        for (int i = cx-2; i <= cx+2; i++) {
            block(es, i, SY+17, cz-2, BlockTypes.IRON_BARS);
            block(es, i, SY+17, cz+2, BlockTypes.IRON_BARS);
        }
        for (int i = cz-1; i <= cz+1; i++) {
            block(es, cx-2, SY+17, i, BlockTypes.IRON_BARS);
            block(es, cx+2, SY+17, i, BlockTypes.IRON_BARS);
        }

        // ── Corner crenellations + lanterns ───────────────────────────────────
        for (int[] c : new int[][]{{cx-2,cz-2},{cx+2,cz-2},{cx-2,cz+2},{cx+2,cz+2}}) {
            column(es, c[0], c[1], SY+17, SY+19, BlockTypes.STONE_BRICKS);
            block(es, c[0], SY+20, c[1], BlockTypes.LANTERN);
        }

        // ── Dark-oak pyramid roof ─────────────────────────────────────────────
        fill(es, cx-2, SY+21, cz-2, cx+2, SY+21, cz+2, BlockTypes.DARK_OAK_PLANKS);
        fill(es, cx-1, SY+22, cz-1, cx+1, SY+22, cz+1, BlockTypes.DARK_OAK_PLANKS);
        block(es, cx,   SY+23, cz,   BlockTypes.DARK_OAK_LOG);
        block(es, cx,   SY+24, cz,   BlockTypes.LANTERN);
    }

    // ============================================================================
    //  ⑦  LIGHTING
    // ============================================================================

    private void buildLampPosts(EditSession es) {
        // Path-side lamp posts — 2 per side on each of the 4 main paths
        int[][] lamps = {
            // east path
            { 15, 3},{ 15,-3},{ 20, 3},{ 20,-3},
            // west path
            {-15, 3},{-15,-3},{-20, 3},{-20,-3},
            // south path
            {  3, 15},{-3, 15},{  3, 20},{-3, 20},
            // north path
            {  3,-15},{-3,-15},{  3,-20},{-3,-20}
        };
        for (int[] lp : lamps) {
            double ex = (double)lp[0]/RX, ez = (double)lp[1]/RZ;
            if (ex*ex+ez*ez > 0.96) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }

        // Plaza ring lamps at radius 15, every 45°
        for (int a = 22; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            int lx = (int) Math.round(15 * Math.cos(rad));
            int lz = (int) Math.round(15 * Math.sin(rad));
            double ex=(double)lx/RX, ez=(double)lz/RZ;
            if (ex*ex+ez*ez > 0.95) continue;
            column(es, lx, lz, SY+1, SY+4, BlockTypes.OAK_FENCE);
            block(es, lx, SY+5, lz, BlockTypes.LANTERN);
        }

        // Perimeter wall torches (every ~18°)
        for (int a = 9; a < 360; a += 18) {
            double rad = Math.toRadians(a);
            int wx = (int) Math.round(38.5 * Math.cos(rad));
            int wz = (int) Math.round(33.5 * Math.sin(rad));
            double ex=(double)wx/RX, ez=(double)wz/RZ;
            if (ex*ex+ez*ez > 0.97) continue;
            block(es, wx, SY+5, wz, BlockTypes.LANTERN);
        }

        // Plaza pillar chain-lanterns (already placed pillar tops at SY+6,
        // add chain from SY+8 down to SY+6 between alternating pillars)
        for (int a = 0; a < 8; a++) {
            double rad = Math.toRadians(a * 45.0);
            int px = (int) Math.round(11 * Math.cos(rad));
            int pz = (int) Math.round(11 * Math.sin(rad));
            block(es, px, SY+7, pz, BlockTypes.CHAIN);
            block(es, px, SY+8, pz, BlockTypes.CHAIN);
        }

        // Soul lanterns near nether portal for ambience
        block(es, -2, SY+1, 22, BlockTypes.SOUL_LANTERN);
        block(es,  2, SY+1, 22, BlockTypes.SOUL_LANTERN);

        // Additional lanterns near water gardens
        column(es, -20, 20, SY+1, SY+3, BlockTypes.OAK_FENCE);
        block(es, -20, SY+4, 20, BlockTypes.LANTERN);
        column(es, -28, 10, SY+1, SY+3, BlockTypes.OAK_FENCE);
        block(es, -28, SY+4, 10, BlockTypes.LANTERN);
    }

    // ============================================================================
    //  ⑧  UNDERGROUND CAVE
    // ============================================================================

    private void buildUndergroundCave(EditSession es) {
        int caY = 52;    // cave centre Y

        // ── Carve ellipsoidal chamber ─────────────────────────────────────────
        for (int x = -15; x <= 15; x++) {
            for (int y = caY-6; y <= caY+5; y++) {
                for (int z = -15; z <= 15; z++) {
                    double dx = (double)x/15.0, dy = (double)(y-caY)/5.5, dz = (double)z/15.0;
                    if (dx*dx+dy*dy+dz*dz <= 1.0)
                        block(es, x, y, z, BlockTypes.CAVE_AIR);
                }
            }
        }

        // ── Cave floor: gravel + stone ────────────────────────────────────────
        for (int x = -13; x <= 13; x++) {
            for (int z = -13; z <= 13; z++) {
                if ((double)x/13*(double)x/13 + (double)z/13*(double)z/13 <= 1.0) {
                    block(es, x, caY-6, z, BlockTypes.GRAVEL);
                    block(es, x, caY-7, z, BlockTypes.STONE);
                    if ((x*31+z*17) % 7 == 0) block(es, x, caY-6, z, BlockTypes.COBBLESTONE);
                }
            }
        }

        // ── Stalactite clusters ───────────────────────────────────────────────
        int[][] stalactites = {
            {4,4,4},{-4,-4,5},{6,-3,3},{-6,4,3},{0,8,5},
            {8,0,4},{-8,-2,4},{2,-9,3},{-2,9,4},{5,7,3},
            {-5,-7,3},{9,-7,3},{-9,7,4},{0,0,6}
        };
        for (int[] s : stalactites) {
            for (int i = 0; i < s[2]; i++)
                block(es, s[0], caY+5-i, s[1], BlockTypes.DEEPSLATE);
        }

        // Stalagmites from floor
        int[][] stalagmites = {{3,6},{-5,5},{7,-3},{-7,4},{1,-8},{-1,9},{6,8},{-6,-9}};
        for (int[] st : stalagmites) {
            int ht = 1 + (Math.abs(st[0]+st[1]) % 3);
            for (int i = 0; i < ht; i++)
                block(es, st[0], caY-6+i, st[1], BlockTypes.STONE);
        }

        // ── Ore veins on cave walls (deterministic) ───────────────────────────
        BlockType[] oreTypes = {
            BlockTypes.DEEPSLATE_IRON_ORE, BlockTypes.DEEPSLATE_GOLD_ORE,
            BlockTypes.DEEPSLATE_COAL_ORE, BlockTypes.DEEPSLATE_LAPIS_ORE,
            BlockTypes.DEEPSLATE_DIAMOND_ORE
        };
        int[][] oreSeeds = {
            {-13, caY+1,  3, 0}, {11, caY-1, -9, 1}, {-9, caY+3, 10, 2},
            { 7,  caY+2,  13, 3},{-12, caY-2, -7, 4}, {13, caY, 5, 2},
            {-7,  caY+4, -10, 1},{ 5,  caY-3,  9, 3}
        };
        for (int[] o : oreSeeds) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int hash = (o[0]+dx)*31+(o[1])*17+(o[2]+dz)*7;
                    if (hash % 3 != 0) continue;
                    block(es, o[0]+dx, o[1], o[2]+dz, oreTypes[o[3]]);
                }
            }
        }

        // ── Cave lighting ─────────────────────────────────────────────────────
        block(es,  0, caY+4,  0, BlockTypes.GLOWSTONE);
        block(es,  7, caY+4,  7, BlockTypes.SHROOMLIGHT);
        block(es, -7, caY+4, -7, BlockTypes.SHROOMLIGHT);
        block(es,  9, caY+2, -5, BlockTypes.GLOWSTONE);
        block(es, -9, caY+2,  5, BlockTypes.GLOWSTONE);
        block(es,  4, caY-5, -7, BlockTypes.SEA_LANTERN);
        block(es, -4, caY-5,  8, BlockTypes.SEA_LANTERN);
        block(es,  0, caY-5,  0, BlockTypes.GLOWSTONE);

        // ── Underground pool ──────────────────────────────────────────────────
        fill(es, -7, caY-6, -8, -4, caY-6, -5, BlockTypes.WATER);
        fill(es, -7, caY-7, -8, -4, caY-7, -5, BlockTypes.DARK_PRISMARINE);
        block(es, -5, caY-7, -6, BlockTypes.SEA_LANTERN);

        // ── Glowing mushroom patch ─────────────────────────────────────────────
        fill(es, 4, caY-6, 6, 8, caY-6, 10, BlockTypes.MYCELIUM);
        for (int[] m : new int[][]{{5,7},{6,9},{8,7},{5,10}})
            block(es, m[0], caY-5, m[1], BlockTypes.BROWN_MUSHROOM);

        // ── Access shaft from surface → cave ─────────────────────────────────
        // Hidden at (18, ?, 2) in the forest zone
        for (int y = caY+6; y < SY; y++) {
            block(es, 17, y, 2, BlockTypes.CAVE_AIR);
            block(es, 18, y, 2, BlockTypes.CAVE_AIR);
            block(es, 17, y, 3, BlockTypes.CAVE_AIR);
            block(es, 18, y, 3, BlockTypes.CAVE_AIR);
        }
        // Oak fence ladder rungs every 2 blocks
        for (int y = caY+7; y < SY-1; y += 2)
            block(es, 17, y, 2, BlockTypes.OAK_FENCE);

        // Shaft wall lining: stone bricks
        for (int y = caY+6; y < SY; y++) {
            block(es, 16, y, 2, BlockTypes.STONE_BRICKS);
            block(es, 19, y, 2, BlockTypes.STONE_BRICKS);
            block(es, 17, y, 1, BlockTypes.STONE_BRICKS);
            block(es, 18, y, 4, BlockTypes.STONE_BRICKS);
        }

        // Iron trapdoor covering the entrance (flush with SY+1)
        block(es, 17, SY+1, 2, BlockTypes.IRON_TRAPDOOR);
        block(es, 18, SY+1, 2, BlockTypes.OAK_PLANKS);
        // Mossy stone "disguise" around trapdoor
        fill(es, 16, SY, 1, 19, SY, 4, BlockTypes.MOSS_BLOCK);
        block(es, 17, SY, 2, BlockTypes.AIR);  // clear over shaft opening
        block(es, 18, SY, 2, BlockTypes.AIR);
        block(es, 17, SY, 3, BlockTypes.AIR);
        block(es, 18, SY, 3, BlockTypes.AIR);
    }

    // ============================================================================
    //  ⑨  FINAL ACCENTS
    // ============================================================================

    private void buildFinalAccents(EditSession es) {
        // ── Decorative boulders ───────────────────────────────────────────────
        BlockType[] bMat = {BlockTypes.ANDESITE, BlockTypes.MOSSY_COBBLESTONE,
                            BlockTypes.STONE, BlockTypes.POLISHED_ANDESITE, BlockTypes.GRANITE};
        int[][] boulders = {
            {-18,-26},{26,18},{-30,10},{38,-20},{-16,30},
            { 14, 28},{-34,-14},{20,-30},{40,14},{-26,-28},
            { 10, 32},{-10,-32},{36, 16},{-36,-16},{-38,8}
        };
        for (int i = 0; i < boulders.length; i++) {
            int bx = boulders[i][0], bz = boulders[i][1];
            double ex = (double)bx/RX, ez = (double)bz/RZ;
            if (ex*ex+ez*ez > 0.87) continue;
            block(es, bx,   SY+1, bz,   bMat[i%bMat.length]);
            block(es, bx+1, SY+1, bz,   bMat[(i+1)%bMat.length]);
            block(es, bx,   SY+1, bz+1, bMat[(i+2)%bMat.length]);
            block(es, bx,   SY+2, bz,   bMat[(i+3)%bMat.length]);
        }

        // ── Amethyst crystal cluster near Crystal Portal ──────────────────────
        int[][] crystalPos = {{20,-5},{21,4},{24,-3},{19,5},{23,-6},{25,2}};
        for (int[] c : crystalPos) {
            block(es, c[0], SY+1, c[1], BlockTypes.AMETHYST_BLOCK);
            if ((c[0]+c[1]) % 2 == 0) block(es, c[0], SY+2, c[1], BlockTypes.AMETHYST_CLUSTER);
        }
        // Budding amethyst buried slightly
        block(es, 22, SY, -1, BlockTypes.BUDDING_AMETHYST);
        block(es, 22, SY,  1, BlockTypes.BUDDING_AMETHYST);

        // ── Nether wart accent near Mace Portal ───────────────────────────────
        int[][] warts = {{4,18},{-4,18},{3,20},{-2,20},{5,22},{-5,22},{4,25},{-3,25}};
        for (int[] w : warts) block(es, w[0], SY+1, w[1], BlockTypes.NETHER_WART_BLOCK);

        // ── Copper oxidation accent near Sword Portal ─────────────────────────
        BlockType[] copperMats = {BlockTypes.CUT_COPPER, BlockTypes.WEATHERED_CUT_COPPER,
                                   BlockTypes.OXIDIZED_CUT_COPPER, BlockTypes.COPPER_BLOCK};
        int[][] copper = {{-18,5},{-20,7},{-19,9},{-17,6},{-21,-4},{-18,-6},{-20,-3},{-16,-7}};
        for (int i = 0; i < copper.length; i++)
            block(es, copper[i][0], SY+1, copper[i][1], copperMats[i%copperMats.length]);

        // ── Information / sign pillars flanking each portal entry ─────────────
        int[][] lorePillars = {{19,0},{-19,0},{0,19},{0,-19}};
        for (int[] lp : lorePillars) {
            column(es, lp[0], lp[1], SY+1, SY+5, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, lp[0], SY+6, lp[1], BlockTypes.SEA_LANTERN);
            block(es, lp[0], SY+7, lp[1], BlockTypes.CHAIN);
            block(es, lp[0], SY+8, lp[1], BlockTypes.LANTERN);
        }

        // ── Edge flowers ──────────────────────────────────────────────────────
        BlockType[] edgeFlowers = {
            BlockTypes.POPPY, BlockTypes.BLUE_ORCHID, BlockTypes.LILY_OF_THE_VALLEY,
            BlockTypes.AZURE_BLUET, BlockTypes.DANDELION, BlockTypes.CORNFLOWER,
            BlockTypes.ALLIUM, BlockTypes.OXEYE_DAISY
        };
        int[][] efPos = {
            {40,-2},{-40,2},{2,36},{-2,-36},{38,10},{-38,-10},{10,34},{-10,-34},
            {36,18},{-36,-18},{18,34},{-18,-34},{42,0},{-42,0},{0,36},{0,-36}
        };
        for (int i = 0; i < efPos.length; i++) {
            int fx = efPos[i][0], fz = efPos[i][1];
            double ex = (double)fx/RX, ez = (double)fz/RZ;
            if (ex*ex+ez*ez > 0.96) continue;
            block(es, fx, SY+1, fz, edgeFlowers[i%edgeFlowers.length]);
        }

        // ── Glowstone seam in island core visible from cave ───────────────────
        int[][] glowVeins = {{6,4},{-6,-4},{4,-6},{-4,6},{0,10},{10,0},{-10,0},{0,-10}};
        for (int[] gv : glowVeins) {
            for (int y = 47; y <= 56; y += 3) {
                if ((gv[0]+gv[1]+y) % 6 == 0)
                    block(es, gv[0], y, gv[1], BlockTypes.GLOWSTONE);
            }
        }

        // ── Beacon base buried under tower for atmospheric beacon beam ─────────
        fill(es, -1, SY-2, -1, 1, SY-2, 1, BlockTypes.IRON_BLOCK);
        block(es,  0, SY-1,  0, BlockTypes.BEACON);

        // ── Sea lanterns on perimeter wall top at 4 cardinal points ───────────
        block(es,  38,  SY+5,  0, BlockTypes.SEA_LANTERN);
        block(es, -38,  SY+5,  0, BlockTypes.SEA_LANTERN);
        block(es,   0,  SY+5, 33, BlockTypes.SEA_LANTERN);
        block(es,   0,  SY+5,-33, BlockTypes.SEA_LANTERN);

        // ── Tuff accent blocks randomly dotting island surface near edges ──────
        int[][] tuffSpots = {
            {38,-10},{-38,8},{14,-32},{-14,30},{34,20},{-34,-20},
            {24,-34},{-24,32},{40,6},{-40,-6}
        };
        for (int[] t : tuffSpots) {
            double ex=(double)t[0]/RX, ez=(double)t[1]/RZ;
            if (ex*ex+ez*ez > 0.9) continue;
            block(es, t[0], SY+1, t[1], BlockTypes.TUFF);
        }
    }
}
