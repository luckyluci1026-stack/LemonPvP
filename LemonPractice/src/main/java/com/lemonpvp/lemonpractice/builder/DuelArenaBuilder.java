package com.lemonpvp.lemonpractice.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * Eight 40×40 symmetrical tropical duel arenas.
 *
 *   Arena 1 – Jungle Temple Duel       ox=1100
 *   Arena 2 – Mangrove Dock            ox=1250
 *   Arena 3 – Bamboo Garden            ox=1400
 *   Arena 4 – Tropical Cliffside       ox=1550
 *   Arena 5 – Ancient Ruins Clearing   ox=1700
 *   Arena 6 – Beachside Dueling Pit    ox=1850
 *   Arena 7 – Floating Island Showdown ox=2000
 *   Arena 8 – Bamboo Forest Shrine     ox=2150
 *
 * Each arena origin is its north-west corner at (ox, oy, oz).
 * All arenas are 40 blocks wide (X) and 40 blocks deep (Z), base at oy=64.
 */
public class DuelArenaBuilder {

    private DuelArenaBuilder() {}

    // =========================================================================
    // Shared helpers
    // =========================================================================

    static void fill(EditSession es,
                     int x1, int y1, int z1,
                     int x2, int y2, int z2,
                     com.sk89q.worldedit.world.block.BlockType type) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    place(es, x, y, z, type);
    }

    static void column(EditSession es, int x, int z, int yBottom, int yTop,
                       com.sk89q.worldedit.world.block.BlockType type) {
        int lo = Math.min(yBottom, yTop), hi = Math.max(yBottom, yTop);
        for (int y = lo; y <= hi; y++) place(es, x, y, z, type);
    }

    static void place(EditSession es, int x, int y, int z,
                      com.sk89q.worldedit.world.block.BlockType type) {
        try {
            es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState());
        } catch (WorldEditException ignored) {}
    }

    /** Standard 40×40 grass/dirt/stone floor. */
    static void flatFloor(EditSession es, int ox, int oy, int oz,
                          com.sk89q.worldedit.world.block.BlockType surface) {
        fill(es, ox, oy, oz, ox + 39, oy, oz + 39, surface);
        fill(es, ox, oy - 1, oz, ox + 39, oy - 1, oz + 39, BlockTypes.DIRT);
        fill(es, ox, oy - 8, oz, ox + 39, oy - 2, oz + 39, BlockTypes.STONE);
    }

    // =========================================================================
    // ARENA 1 – Jungle Temple Duel   (ox=1100)
    // =========================================================================
    public static void buildJungleTempleDuel(EditSession es, int ox, int oy, int oz) {
        flatFloor(es, ox, oy, oz, BlockTypes.GRASS_BLOCK);

        // Central 6×6 sandstone raised platform at y=oy+1
        // Centred at ox+17, oz+17 → x=ox+14..ox+19, z=oz+14..oz+19
        fill(es, ox + 14, oy + 1, oz + 14, ox + 25, oy + 1, oz + 25, BlockTypes.SANDSTONE);

        // 4 jungle_log corner pillars, height 6 (y=oy+1..oy+6)
        int[][] pillars = {
            {ox + 1,  oz + 1},
            {ox + 38, oz + 1},
            {ox + 1,  oz + 38},
            {ox + 38, oz + 38}
        };
        for (int[] p : pillars) {
            column(es, p[0], p[1], oy + 1, oy + 6, BlockTypes.JUNGLE_LOG);
        }

        // Mossy_cobblestone wall fragments 3-high: north edge (z=oz..oz+2), x=ox+5..ox+34
        fill(es, ox + 5, oy + 1, oz,     ox + 34, oy + 3, oz,     BlockTypes.MOSSY_COBBLESTONE);
        fill(es, ox + 5, oy + 1, oz + 2, ox + 34, oy + 3, oz + 2, BlockTypes.MOSSY_COBBLESTONE);
        // South edge (z=oz+37..oz+39)
        fill(es, ox + 5, oy + 1, oz + 37, ox + 34, oy + 3, oz + 37, BlockTypes.MOSSY_COBBLESTONE);
        fill(es, ox + 5, oy + 1, oz + 39, ox + 34, oy + 3, oz + 39, BlockTypes.MOSSY_COBBLESTONE);

        // Cracked stone bricks accent on platform step corners
        place(es, ox + 14, oy + 1, oz + 14, BlockTypes.CRACKED_STONE_BRICKS);
        place(es, ox + 25, oy + 1, oz + 14, BlockTypes.CRACKED_STONE_BRICKS);
        place(es, ox + 14, oy + 1, oz + 25, BlockTypes.CRACKED_STONE_BRICKS);
        place(es, ox + 25, oy + 1, oz + 25, BlockTypes.CRACKED_STONE_BRICKS);

        // Leaf tufts on pillar tops
        for (int[] p : pillars) {
            fill(es, p[0] - 1, oy + 7, p[1] - 1, p[0] + 1, oy + 8, p[1] + 1,
                    BlockTypes.JUNGLE_LEAVES);
        }
    }

    // =========================================================================
    // ARENA 2 – Mangrove Dock   (ox=1250)
    // =========================================================================
    public static void buildMangroveDock(EditSession es, int ox, int oy, int oz) {
        // Eastern 25 cols: grass/mud floor  (x=ox+15..ox+39)
        fill(es, ox + 15, oy, oz, ox + 39, oy, oz + 39, BlockTypes.GRASS_BLOCK);
        fill(es, ox + 15, oy - 1, oz, ox + 39, oy - 1, oz + 39, BlockTypes.MUD);
        fill(es, ox + 15, oy - 8, oz, ox + 39, oy - 2, oz + 39, BlockTypes.STONE);

        // Western 15 cols: water (x=ox..ox+14), mud bed, stone below
        fill(es, ox, oy - 2, oz, ox + 14, oy - 2, oz + 39, BlockTypes.MUD);
        fill(es, ox, oy - 1, oz, ox + 14, oy + 1, oz + 39, BlockTypes.WATER);
        fill(es, ox, oy - 8, oz, ox + 14, oy - 3, oz + 39, BlockTypes.STONE);

        // Mangrove_log posts: every 4 blocks in x (ox+2, ox+6, ox+10, ox+14), every 8 in z
        for (int x = ox + 2; x <= ox + 14; x += 4) {
            for (int z = oz + 4; z <= oz + 36; z += 8) {
                column(es, x, z, oy - 1, oy + 3, BlockTypes.MANGROVE_LOG);
            }
        }

        // Mangrove_planks dock at y=oy+2, running N-S (all z, x=ox..ox+14)
        fill(es, ox, oy + 2, oz, ox + 14, oy + 2, oz + 39, BlockTypes.MANGROVE_PLANKS);

        // Connecting plank ramp from dock level to ground level at x=ox+14..ox+16
        fill(es, ox + 15, oy + 1, oz, ox + 16, oy + 1, oz + 39, BlockTypes.MANGROVE_PLANKS);
        fill(es, ox + 17, oy,     oz, ox + 17, oy,     oz + 39, BlockTypes.MANGROVE_PLANKS);

        // Fence railing on dock edges (z=oz, z=oz+39)
        for (int x = ox; x <= ox + 14; x++) {
            column(es, x, oz,      oy + 2, oy + 4, BlockTypes.MANGROVE_FENCE);
            column(es, x, oz + 39, oy + 2, oy + 4, BlockTypes.MANGROVE_FENCE);
        }

        // Lanterns on post tops
        for (int x = ox + 2; x <= ox + 14; x += 8) {
            for (int z = oz + 4; z <= oz + 36; z += 16) {
                place(es, x, oy + 4, z, BlockTypes.LANTERN);
            }
        }
    }

    // =========================================================================
    // ARENA 3 – Bamboo Garden   (ox=1400)
    // =========================================================================
    public static void buildBambooGarden(EditSession es, int ox, int oy, int oz) {
        flatFloor(es, ox, oy, oz, BlockTypes.GRASS_BLOCK);

        // Bamboo clusters at 4 corners (3–5 stalks each, 2×2 cluster)
        int[][] corners = {
            {ox + 2,  oz + 2},
            {ox + 36, oz + 2},
            {ox + 2,  oz + 36},
            {ox + 36, oz + 36}
        };
        int[] heights = {5, 4, 5, 4};
        for (int i = 0; i < corners.length; i++) {
            int bx = corners[i][0], bz = corners[i][1], h = heights[i];
            for (int dx = 0; dx <= 1; dx++)
                for (int dz = 0; dz <= 1; dz++)
                    column(es, bx + dx, bz + dz, oy + 1, oy + h, BlockTypes.BAMBOO);
            // Third stalk offset
            column(es, bx + 2, bz, oy + 1, oy + h - 1, BlockTypes.BAMBOO);
        }

        // Cherry leaves at symmetrical mid-edge positions
        int[][] cherryPos = {
            {ox + 19, oz + 2},  {ox + 19, oz + 37},
            {ox + 2,  oz + 19}, {ox + 37, oz + 19}
        };
        for (int[] cp : cherryPos) {
            column(es, cp[0], cp[1], oy + 1, oy + 4, BlockTypes.CHERRY_LOG);
            fill(es, cp[0] - 1, oy + 5, cp[1] - 1, cp[0] + 1, oy + 6, cp[1] + 1,
                    BlockTypes.CHERRY_LEAVES);
        }

        // Central 3×3 stone_slab circle at y=oy (replace the grass surface)
        fill(es, ox + 18, oy, oz + 18, ox + 20, oy, oz + 20, BlockTypes.STONE_SLAB);
        // Extend with a 5×1 cross of slabs (decorative)
        place(es, ox + 19, oy, oz + 17, BlockTypes.STONE_SLAB);
        place(es, ox + 19, oy, oz + 21, BlockTypes.STONE_SLAB);
        place(es, ox + 17, oy, oz + 19, BlockTypes.STONE_SLAB);
        place(es, ox + 21, oy, oz + 19, BlockTypes.STONE_SLAB);

        // Scattered ferns as decoration
        int[][] ferns = {
            {ox + 8, oz + 8}, {ox + 30, oz + 8}, {ox + 8, oz + 30}, {ox + 30, oz + 30},
            {ox + 15, oz + 5}, {ox + 24, oz + 34}
        };
        for (int[] f : ferns) {
            place(es, f[0], oy + 1, f[1], BlockTypes.FERN);
        }
    }

    // =========================================================================
    // ARENA 4 – Tropical Cliffside   (ox=1550)
    // =========================================================================
    public static void buildTropicalCliffside(EditSession es, int ox, int oy, int oz) {
        // Player 1 side: z=oz..oz+17, elevated at oy+3
        int p1Top = oy + 3;
        fill(es, ox, p1Top, oz, ox + 39, p1Top, oz + 17, BlockTypes.GRASS_BLOCK);
        fill(es, ox, p1Top - 1, oz, ox + 39, p1Top - 1, oz + 17, BlockTypes.DIRT);
        fill(es, ox, p1Top - 8, oz, ox + 39, p1Top - 2, oz + 17, BlockTypes.STONE);

        // Stone retaining walls on the outer edge of P1 side
        fill(es, ox, oy + 1, oz, ox + 39, oy + 3, oz, BlockTypes.STONE);       // north wall
        fill(es, ox, oy + 1, oz, ox, oy + 3, oz + 17, BlockTypes.STONE);       // west wall
        fill(es, ox + 39, oy + 1, oz, ox + 39, oy + 3, oz + 17, BlockTypes.STONE); // east wall

        // Drop area: z=oz+18..oz+21, water cushion at y=oy
        fill(es, ox, oy - 2, oz + 18, ox + 39, oy - 2, oz + 21, BlockTypes.STONE);
        fill(es, ox, oy - 1, oz + 18, ox + 39, oy,     oz + 21, BlockTypes.WATER);

        // Cliff face: stone wall dropping from p1Top down to oy on both sides of the gap
        fill(es, ox, oy + 1, oz + 17, ox + 39, p1Top - 1, oz + 17, BlockTypes.STONE);
        fill(es, ox, oy + 1, oz + 22, ox + 39, p1Top - 1, oz + 22, BlockTypes.STONE);

        // Player 2 side: z=oz+22..oz+39, mirror elevation at oy+3
        fill(es, ox, p1Top, oz + 22, ox + 39, p1Top, oz + 39, BlockTypes.GRASS_BLOCK);
        fill(es, ox, p1Top - 1, oz + 22, ox + 39, p1Top - 1, oz + 39, BlockTypes.DIRT);
        fill(es, ox, p1Top - 8, oz + 22, ox + 39, p1Top - 2, oz + 39, BlockTypes.STONE);

        // Stone walls outer edge of P2 side
        fill(es, ox, oy + 1, oz + 39, ox + 39, oy + 3, oz + 39, BlockTypes.STONE); // south wall
        fill(es, ox, oy + 1, oz + 22, ox, oy + 3, oz + 39, BlockTypes.STONE);
        fill(es, ox + 39, oy + 1, oz + 22, ox + 39, oy + 3, oz + 39, BlockTypes.STONE);

        // Jungle tree accents on each side
        column(es, ox + 5, oz + 5, p1Top + 1, p1Top + 6, BlockTypes.JUNGLE_LOG);
        fill(es, ox + 4, p1Top + 7, oz + 4, ox + 6, p1Top + 9, oz + 6, BlockTypes.JUNGLE_LEAVES);

        column(es, ox + 5, oz + 34, p1Top + 1, p1Top + 6, BlockTypes.JUNGLE_LOG);
        fill(es, ox + 4, p1Top + 7, oz + 33, ox + 6, p1Top + 9, oz + 35, BlockTypes.JUNGLE_LEAVES);
    }

    // =========================================================================
    // ARENA 5 – Ancient Ruins Clearing   (ox=1700)
    // =========================================================================
    public static void buildAncientRuinsClearing(EditSession es, int ox, int oy, int oz) {
        flatFloor(es, ox, oy, oz, BlockTypes.GRASS_BLOCK);

        // Stone_brick ruin clusters in all 4 quadrants
        buildRuinCluster(es, ox + 3,  oy, oz + 3,  7, 4);
        buildRuinCluster(es, ox + 28, oy, oz + 3,  7, 3);
        buildRuinCluster(es, ox + 3,  oy, oz + 28, 7, 4);
        buildRuinCluster(es, ox + 28, oy, oz + 28, 7, 3);

        // Central cleared oval – replace surface with mossy_stone_bricks
        for (int x = ox + 12; x <= ox + 27; x++) {
            for (int z = oz + 14; z <= oz + 25; z++) {
                double dx = (x - (ox + 19.5)) / 8.0;
                double dz = (z - (oz + 19.5)) / 6.0;
                if (dx * dx + dz * dz <= 1.0) {
                    place(es, x, oy, z, BlockTypes.MOSSY_STONE_BRICKS);
                }
            }
        }

        // Cracked bricks scattered randomly (deterministic pattern)
        for (int i = 0; i < 40; i++) {
            int x = ox + (i * 7 + 3) % 40;
            int z = oz + (i * 11 + 5) % 40;
            place(es, x, oy, z, BlockTypes.CRACKED_STONE_BRICKS);
        }
    }

    private static void buildRuinCluster(EditSession es, int x1, int oy, int z1, int size, int maxH) {
        // Partial walls: north and west sides with randomised heights
        int[] rowH = {maxH, maxH - 1, maxH, maxH - 2, maxH - 1, maxH, maxH - 1};
        for (int i = 0; i < size; i++) {
            int h = rowH[i % rowH.length];
            // North wall
            fill(es, x1 + i, oy + 1, z1, x1 + i, oy + h, z1, BlockTypes.STONE_BRICKS);
            // West wall
            fill(es, x1, oy + 1, z1 + i, x1, oy + h, z1 + i, BlockTypes.STONE_BRICKS);
        }
        // Mossy variant on some blocks
        fill(es, x1 + 2, oy + 1, z1, x1 + 3, oy + 2, z1, BlockTypes.MOSSY_STONE_BRICKS);
        fill(es, x1, oy + 1, z1 + 2, x1, oy + 2, z1 + 3, BlockTypes.MOSSY_STONE_BRICKS);
        // Cracked corner cap
        place(es, x1, oy + maxH, z1, BlockTypes.CRACKED_STONE_BRICKS);
    }

    // =========================================================================
    // ARENA 6 – Beachside Dueling Pit   (ox=1850)
    // =========================================================================
    public static void buildBeachsideDuelingPit(EditSession es, int ox, int oy, int oz) {
        // Sand floor
        fill(es, ox, oy, oz, ox + 39, oy, oz + 39, BlockTypes.SAND);
        fill(es, ox, oy - 1, oz, ox + 39, oy - 1, oz + 39, BlockTypes.SANDSTONE);
        fill(es, ox, oy - 8, oz, ox + 39, oy - 2, oz + 39, BlockTypes.STONE);

        // Sandstone wall border, 2 high, all around
        // North/south walls
        fill(es, ox, oy + 1, oz, ox + 39, oy + 2, oz, BlockTypes.SANDSTONE_WALL);
        fill(es, ox, oy + 1, oz + 39, ox + 39, oy + 2, oz + 39, BlockTypes.SANDSTONE_WALL);
        // West/east walls
        fill(es, ox, oy + 1, oz, ox, oy + 2, oz + 39, BlockTypes.SANDSTONE_WALL);
        fill(es, ox + 39, oy + 1, oz, ox + 39, oy + 2, oz + 39, BlockTypes.SANDSTONE_WALL);

        // Openings: 3-wide on each wall (remove walls at midpoint)
        for (int y = oy + 1; y <= oy + 2; y++) {
            // North opening
            for (int x = ox + 18; x <= ox + 20; x++) place(es, x, y, oz,      BlockTypes.AIR);
            for (int x = ox + 18; x <= ox + 20; x++) place(es, x, y, oz + 39, BlockTypes.AIR);
            // West/east openings
            for (int z = oz + 18; z <= oz + 20; z++) place(es, ox,      y, z, BlockTypes.AIR);
            for (int z = oz + 18; z <= oz + 20; z++) place(es, ox + 39, y, z, BlockTypes.AIR);
        }

        // Central 8×8 stone_slab floor at y=oy (ox+16..ox+23, oz+16..oz+23)
        fill(es, ox + 16, oy, oz + 16, ox + 23, oy, oz + 23, BlockTypes.STONE_SLAB);

        // Decorative sandstone pillar columns at wall corners
        int[][] wallCorners = {
            {ox,      oz},      {ox + 38, oz},
            {ox,      oz + 38}, {ox + 38, oz + 38}
        };
        for (int[] wc : wallCorners) {
            column(es, wc[0], wc[1], oy + 1, oy + 4, BlockTypes.SANDSTONE_PILLAR);
        }
    }

    // =========================================================================
    // ARENA 7 – Floating Island Showdown   (ox=2000)
    // =========================================================================
    public static void buildFloatingIslandShowdown(EditSession es, int ox, int oy, int oz) {
        // Two islands: void between them (no fill there)
        // Island 1: centred at oz+8, footprint z=oz+1..oz+15, x=ox+13..ox+27 (15×15)
        buildIsland(es, ox + 13, oy, oz + 1);
        // Island 2: centred at oz+32, footprint z=oz+25..oz+39, x=ox+13..ox+27 (15×15)
        buildIsland(es, ox + 13, oy, oz + 25);

        // Decorative fence safety rails on island edges facing the void
        // Island 1 south edge (z=oz+15)
        for (int x = ox + 13; x <= ox + 27; x++) {
            column(es, x, oz + 15, oy + 1, oy + 2, BlockTypes.OAK_FENCE);
        }
        // Island 2 north edge (z=oz+25)
        for (int x = ox + 13; x <= ox + 27; x++) {
            column(es, x, oz + 25, oy + 1, oy + 2, BlockTypes.OAK_FENCE);
        }

        // Jungle tree on each island
        int midX = ox + 20;
        column(es, midX, oz + 5,  oy + 1, oy + 6, BlockTypes.JUNGLE_LOG);
        fill(es, midX - 1, oy + 7, oz + 4,  midX + 1, oy + 9, oz + 6,  BlockTypes.JUNGLE_LEAVES);

        column(es, midX, oz + 35, oy + 1, oy + 6, BlockTypes.JUNGLE_LOG);
        fill(es, midX - 1, oy + 7, oz + 34, midX + 1, oy + 9, oz + 36, BlockTypes.JUNGLE_LEAVES);
    }

    /** Place a 15×15 floating island at (x1, oy, z1) with stone core and grass cap. */
    private static void buildIsland(EditSession es, int x1, int oy, int z1) {
        fill(es, x1, oy - 9, z1, x1 + 14, oy - 1, z1 + 14, BlockTypes.STONE);
        fill(es, x1, oy,     z1, x1 + 14, oy,     z1 + 14, BlockTypes.GRASS_BLOCK);
        // Taper the stone underneath so it looks like a floating island
        fill(es, x1 + 2, oy - 11, z1 + 2, x1 + 12, oy - 10, z1 + 12, BlockTypes.STONE);
        fill(es, x1 + 4, oy - 13, z1 + 4, x1 + 10, oy - 12, z1 + 10, BlockTypes.STONE);
    }

    // =========================================================================
    // ARENA 8 – Bamboo Forest Shrine   (ox=2150)
    // =========================================================================
    public static void buildBambooForestShrine(EditSession es, int ox, int oy, int oz) {
        flatFloor(es, ox, oy, oz, BlockTypes.GRASS_BLOCK);

        // Dense bamboo forest in the outer 10-block ring (every other x/z)
        for (int x = ox; x <= ox + 39; x++) {
            for (int z = oz; z <= oz + 39; z++) {
                // Only in the outer 10-block band
                boolean inOuter = (x < ox + 10 || x > ox + 29 || z < oz + 10 || z > oz + 29);
                if (inOuter && (x + z) % 2 == 0) {
                    int height = 3 + ((x * 3 + z * 7) % 4); // deterministic 3–6 height
                    column(es, x, z, oy + 1, oy + height, BlockTypes.BAMBOO);
                }
            }
        }

        // Central 20×20 shrine: stone_bricks floor (x=ox+10..ox+29, z=oz+10..oz+29)
        fill(es, ox + 10, oy, oz + 10, ox + 29, oy, oz + 29, BlockTypes.STONE_BRICKS);

        // 4 stone pillars, height 5, at inner corners of shrine
        int[][] pillarPos = {
            {ox + 11, oz + 11}, {ox + 28, oz + 11},
            {ox + 11, oz + 28}, {ox + 28, oz + 28}
        };
        for (int[] p : pillarPos) {
            fill(es, p[0], oy + 1, p[1], p[0] + 1, oy + 5, p[1] + 1, BlockTypes.STONE_BRICKS);
            // Lantern on each pillar top
            place(es, p[0],     oy + 6, p[1],     BlockTypes.LANTERN);
            place(es, p[0] + 1, oy + 6, p[1],     BlockTypes.LANTERN);
            place(es, p[0],     oy + 6, p[1] + 1, BlockTypes.LANTERN);
            place(es, p[0] + 1, oy + 6, p[1] + 1, BlockTypes.LANTERN);
        }

        // Stone_bricks accent ring inside shrine (inner border, 1 block thick)
        for (int x = ox + 10; x <= ox + 29; x++) {
            place(es, x, oy, oz + 10, BlockTypes.MOSSY_STONE_BRICKS);
            place(es, x, oy, oz + 29, BlockTypes.MOSSY_STONE_BRICKS);
        }
        for (int z = oz + 10; z <= oz + 29; z++) {
            place(es, ox + 10, oy, z, BlockTypes.MOSSY_STONE_BRICKS);
            place(es, ox + 29, oy, z, BlockTypes.MOSSY_STONE_BRICKS);
        }

        // Cherry blossom trees in each arena corner
        int[][] cherryCorners = {
            {ox + 1,  oz + 1},
            {ox + 36, oz + 1},
            {ox + 1,  oz + 36},
            {ox + 36, oz + 36}
        };
        for (int[] cc : cherryCorners) {
            column(es, cc[0], cc[1], oy + 1, oy + 5, BlockTypes.CHERRY_LOG);
            fill(es, cc[0] - 1, oy + 6, cc[1] - 1, cc[0] + 1, oy + 7, cc[1] + 1,
                    BlockTypes.CHERRY_LEAVES);
            place(es, cc[0], oy + 8, cc[1], BlockTypes.CHERRY_LEAVES);
        }
    }

    // =========================================================================
    // Entry point – build all 8 duel arenas
    // =========================================================================
    public static void buildAll(EditSession es, int baseY) {
        buildJungleTempleDuel      (es, 1100, baseY, 0);
        buildMangroveDock          (es, 1250, baseY, 0);
        buildBambooGarden          (es, 1400, baseY, 0);
        buildTropicalCliffside     (es, 1550, baseY, 0);
        buildAncientRuinsClearing  (es, 1700, baseY, 0);
        buildBeachsideDuelingPit   (es, 1850, baseY, 0);
        buildFloatingIslandShowdown(es, 2000, baseY, 0);
        buildBambooForestShrine    (es, 2150, baseY, 0);
    }
}
