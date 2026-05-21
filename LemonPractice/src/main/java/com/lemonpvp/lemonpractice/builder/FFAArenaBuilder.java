package com.lemonpvp.lemonpractice.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * Five 80×80 tropical FFA arenas positioned along the X axis.
 *
 *   Arena 1 – Jungle Ruins         originX=0
 *   Arena 2 – Mangrove Swamp Bay   originX=200
 *   Arena 3 – Bamboo Fortress      originX=400
 *   Arena 4 – Tropical Beach Cove  originX=600
 *   Arena 5 – Ancient Overgrown Pyramid  originX=800
 *
 * All bases sit at originY=64. Each arena is 80×80 (x: origin..origin+79, z: 0..79).
 *
 * All methods are static – call them with an active EditSession and an origin coordinate.
 */
public class FFAArenaBuilder {

    private FFAArenaBuilder() {}

    // =========================================================================
    // Shared low-level helpers (package-private static so inner classes can use)
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
                    placeBlock(es, x, y, z, type);
    }

    static void column(EditSession es, int x, int z, int yBottom, int yTop,
                       com.sk89q.worldedit.world.block.BlockType type) {
        int lo = Math.min(yBottom, yTop), hi = Math.max(yBottom, yTop);
        for (int y = lo; y <= hi; y++) placeBlock(es, x, y, z, type);
    }

    static void placeBlock(EditSession es, int x, int y, int z,
                           com.sk89q.worldedit.world.block.BlockType type) {
        try {
            es.setBlock(BlockVector3.at(x, y, z), type.getDefaultState());
        } catch (WorldEditException ignored) {}
    }

    // =========================================================================
    // Convenience: flat grass base (shared pattern for each arena)
    // =========================================================================
    static void grassBase(EditSession es, int ox, int oy, int oz) {
        // Surface grass layer
        fill(es, ox, oy, oz, ox + 79, oy, oz + 79, BlockTypes.GRASS_BLOCK);
        // Dirt underneath
        fill(es, ox, oy - 1, oz, ox + 79, oy - 1, oz + 79, BlockTypes.DIRT);
        // Stone core
        fill(es, ox, oy - 8, oz, ox + 79, oy - 2, oz + 79, BlockTypes.STONE);
    }

    // =========================================================================
    // ARENA 1 – Jungle Ruins   (originX = 0)
    // =========================================================================
    public static class JungleRuins {
        public static void build(EditSession es, int ox, int oy, int oz) {
            grassBase(es, ox, oy, oz);
            buildTemple(es, ox, oy, oz);
            buildFallenTrees(es, ox, oy, oz);
            buildLeafClusters(es, ox, oy, oz);
            buildScatteredRubble(es, ox, oy, oz);
        }

        private static void buildTemple(EditSession es, int ox, int oy, int oz) {
            // 20×20 footprint centred in the arena (x=30..49, z=30..49)
            int tx = ox + 30, tz = oz + 30;

            // Floor: mossy_stone_bricks
            fill(es, tx, oy, tz, tx + 19, oy, tz + 19, BlockTypes.MOSSY_STONE_BRICKS);

            // Outer walls – varying height to simulate collapse: 4–7 blocks
            int[] wallHeights = {7, 6, 5, 7, 4, 5, 6, 7, 5, 4, 6, 7, 5, 4, 7, 6, 5, 7, 6, 5};
            for (int i = 0; i < 20; i++) {
                int h = wallHeights[i % wallHeights.length];
                // North wall (tz)
                fill(es, tx + i, oy + 1, tz,     tx + i, oy + h, tz,     BlockTypes.STONE_BRICKS);
                // South wall (tz+19)
                fill(es, tx + i, oy + 1, tz + 19, tx + i, oy + h, tz + 19, BlockTypes.STONE_BRICKS);
                // West wall (tx)
                fill(es, tx,     oy + 1, tz + i, tx,     oy + h, tz + i, BlockTypes.STONE_BRICKS);
                // East wall (tx+19)
                fill(es, tx + 19, oy + 1, tz + i, tx + 19, oy + h, tz + i, BlockTypes.STONE_BRICKS);
            }

            // Mossy cobblestone patches replacing some wall blocks for variety
            for (int i = 2; i <= 18; i += 4) {
                fill(es, tx + i, oy + 2, tz,     tx + i + 1, oy + 4, tz,
                        BlockTypes.MOSSY_COBBLESTONE);
                fill(es, tx + i, oy + 2, tz + 19, tx + i + 1, oy + 4, tz + 19,
                        BlockTypes.MOSSY_COBBLESTONE);
            }

            // Interior jungle_log columns (4 positions)
            for (int[] pos : new int[][]{{4, 4}, {4, 15}, {15, 4}, {15, 15}}) {
                column(es, tx + pos[0], tz + pos[1], oy + 1, oy + 5, BlockTypes.JUNGLE_LOG);
            }

            // Gateway openings (clear 3-wide gaps in each wall at midpoint)
            for (int dy = oy + 1; dy <= oy + 4; dy++) {
                for (int di = -1; di <= 1; di++) {
                    placeBlock(es, tx + 10 + di, dy, tz,      BlockTypes.AIR);
                    placeBlock(es, tx + 10 + di, dy, tz + 19, BlockTypes.AIR);
                    placeBlock(es, tx,      dy, tz + 10 + di, BlockTypes.AIR);
                    placeBlock(es, tx + 19, dy, tz + 10 + di, BlockTypes.AIR);
                }
            }

            // Cracked stone bricks for added ruin detail
            fill(es, tx + 5, oy + 1, tz + 5, tx + 6, oy + 3, tz + 6,
                    BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, tx + 13, oy + 1, tz + 13, tx + 14, oy + 3, tz + 14,
                    BlockTypes.CRACKED_STONE_BRICKS);
        }

        private static void buildFallenTrees(EditSession es, int ox, int oy, int oz) {
            // Horizontal oak logs as fallen trees (running along Z axis)
            for (int z = oz + 10; z <= oz + 15; z++) {
                placeBlock(es, ox + 10, oy + 1, z, BlockTypes.OAK_LOG);
            }
            for (int z = oz + 60; z <= oz + 66; z++) {
                placeBlock(es, ox + 65, oy + 1, z, BlockTypes.OAK_LOG);
            }
            for (int x = ox + 55; x <= ox + 60; x++) {
                placeBlock(es, x, oy + 1, oz + 20, BlockTypes.OAK_LOG);
            }
        }

        private static void buildLeafClusters(EditSession es, int ox, int oy, int oz) {
            int[][] spots = {
                {ox + 8,  oz + 8},  {ox + 70, oz + 10},
                {ox + 12, oz + 65}, {ox + 68, oz + 70},
                {ox + 25, oz + 55}, {ox + 55, oz + 25}
            };
            for (int[] s : spots) {
                // Small sphere of leaves radius=2
                for (int dx = -2; dx <= 2; dx++)
                    for (int dy = 0; dy <= 3; dy++)
                        for (int dz = -2; dz <= 2; dz++) {
                            double dist = Math.sqrt(dx * dx + (dy - 1.5) * (dy - 1.5) + dz * dz);
                            if (dist <= 2.2)
                                placeBlock(es, s[0] + dx, oy + 3 + dy, s[1] + dz,
                                        BlockTypes.JUNGLE_LEAVES);
                        }
            }
        }

        private static void buildScatteredRubble(EditSession es, int ox, int oy, int oz) {
            int[][] rubble = {
                {ox + 15, oz + 45}, {ox + 60, oz + 50},
                {ox + 22, oz + 22}, {ox + 58, oz + 60},
                {ox + 5,  oz + 72}, {ox + 72, oz + 8}
            };
            for (int[] r : rubble) {
                placeBlock(es, r[0],     oy + 1, r[1],     BlockTypes.STONE_BRICKS);
                placeBlock(es, r[0] + 1, oy + 1, r[1],     BlockTypes.MOSSY_COBBLESTONE);
                placeBlock(es, r[0],     oy + 1, r[1] + 1, BlockTypes.CRACKED_STONE_BRICKS);
                placeBlock(es, r[0] + 1, oy + 2, r[1] + 1, BlockTypes.STONE_BRICKS);
            }
        }
    }

    // =========================================================================
    // ARENA 2 – Mangrove Swamp Bay   (originX = 200)
    // =========================================================================
    public static class MangroveSwampBay {
        public static void build(EditSession es, int ox, int oy, int oz) {
            buildGround(es, ox, oy, oz);
            buildWaterSection(es, ox, oy, oz);
            buildMangrovePostsAndBridges(es, ox, oy, oz);
        }

        private static void buildGround(EditSession es, int ox, int oy, int oz) {
            // Eastern half (x=40..79): mangrove grass over dirt/stone
            fill(es, ox + 40, oy, oz, ox + 79, oy, oz + 79, BlockTypes.GRASS_BLOCK);
            fill(es, ox + 40, oy - 1, oz, ox + 79, oy - 1, oz + 79, BlockTypes.DIRT);
            fill(es, ox + 40, oy - 8, oz, ox + 79, oy - 2, oz + 79, BlockTypes.STONE);

            // Mud under the water section floor
            fill(es, ox, oy - 1, oz, ox + 39, oy - 1, oz + 79, BlockTypes.MUD);
            fill(es, ox, oy - 8, oz, ox + 39, oy - 2, oz + 79, BlockTypes.STONE);
        }

        private static void buildWaterSection(EditSession es, int ox, int oy, int oz) {
            // Water column: y=oy-1 to oy+1 so it fills up to surface height
            fill(es, ox, oy - 1, oz, ox + 39, oy + 1, oz + 79, BlockTypes.WATER);
            // Mud floor under water
            fill(es, ox, oy - 2, oz, ox + 39, oy - 2, oz + 79, BlockTypes.MUD);
        }

        private static void buildMangrovePostsAndBridges(EditSession es, int ox, int oy, int oz) {
            // Mangrove log posts rising from y=oy-1 to oy+3 in water, every 8 blocks
            for (int z = oz + 8; z <= oz + 72; z += 8) {
                for (int x = ox + 4; x <= ox + 36; x += 8) {
                    column(es, x, z, oy - 1, oy + 3, BlockTypes.MANGROVE_LOG);
                }
            }

            // Mangrove_planks bridges 2-wide at y=oy+2 connecting posts along Z rows
            int[] bridgeZs = {oz + 8, oz + 24, oz + 40, oz + 56, oz + 72};
            for (int bz : bridgeZs) {
                fill(es, ox + 4, oy + 2, bz, ox + 36, oy + 2, bz,     BlockTypes.MANGROVE_PLANKS);
                fill(es, ox + 4, oy + 2, bz + 1, ox + 36, oy + 2, bz + 1, BlockTypes.MANGROVE_PLANKS);
            }

            // Connecting east bank to water platforms at y=oy+2
            for (int x = ox + 36; x <= ox + 40; x++) {
                fill(es, x, oy + 2, oz + 8, x, oy + 2, oz + 8 + 1, BlockTypes.MANGROVE_PLANKS);
                fill(es, x, oy + 2, oz + 40, x, oy + 2, oz + 41, BlockTypes.MANGROVE_PLANKS);
                fill(es, x, oy + 2, oz + 72, x, oy + 2, oz + 73, BlockTypes.MANGROVE_PLANKS);
            }

            // Lanterns on post tops
            for (int z = oz + 8; z <= oz + 72; z += 16) {
                for (int x = ox + 4; x <= ox + 36; x += 16) {
                    placeBlock(es, x, oy + 4, z, BlockTypes.LANTERN);
                }
            }
        }
    }

    // =========================================================================
    // ARENA 3 – Bamboo Fortress   (originX = 400)
    // =========================================================================
    public static class BambooFortress {
        public static void build(EditSession es, int ox, int oy, int oz) {
            grassBase(es, ox, oy, oz);
            buildCompoundWalls(es, ox, oy, oz);
            buildCompoundFloor(es, ox, oy, oz);
            buildBambooClusters(es, ox, oy, oz);
            buildCornerPosts(es, ox, oy, oz);
        }

        private static void buildCompoundWalls(EditSession es, int ox, int oy, int oz) {
            // Compound: inner open area x=ox+15..ox+64, z=oz+15..oz+64
            // Walls are 3 wide and 6 tall starting at oy+1
            int wx1 = ox + 14, wx2 = ox + 65;
            int wz1 = oz + 14, wz2 = oz + 65;

            // Fill the wall bands
            for (int y = oy + 1; y <= oy + 6; y++) {
                // North wall band (z=wz1 to wz1+2)
                fill(es, wx1, y, wz1, wx2, y, wz1 + 2, BlockTypes.BAMBOO_BLOCK);
                // South wall band (z=wz2-2 to wz2)
                fill(es, wx1, y, wz2 - 2, wx2, y, wz2,   BlockTypes.BAMBOO_BLOCK);
                // West wall band (x=wx1 to wx1+2)
                fill(es, wx1, y, wz1, wx1 + 2, y, wz2,   BlockTypes.BAMBOO_BLOCK);
                // East wall band (x=wx2-2 to wx2)
                fill(es, wx2 - 2, y, wz1, wx2, y, wz2,   BlockTypes.BAMBOO_BLOCK);
            }

            // Gate openings (clear 4-wide at mid of each wall)
            for (int y = oy + 1; y <= oy + 4; y++) {
                for (int di = -2; di <= 1; di++) {
                    // North gate
                    fill(es, ox + 39 + di, y, wz1, ox + 39 + di, y, wz1 + 2, BlockTypes.AIR);
                    // South gate
                    fill(es, ox + 39 + di, y, wz2 - 2, ox + 39 + di, y, wz2, BlockTypes.AIR);
                    // West gate
                    fill(es, wx1, y, oz + 39 + di, wx1 + 2, y, oz + 39 + di, BlockTypes.AIR);
                    // East gate
                    fill(es, wx2 - 2, y, oz + 39 + di, wx2, y, oz + 39 + di, BlockTypes.AIR);
                }
            }
        }

        private static void buildCompoundFloor(EditSession es, int ox, int oy, int oz) {
            // Bamboo planks floor inside compound (innermost 50×50)
            fill(es, ox + 17, oy, oz + 17, ox + 62, oy, oz + 62, BlockTypes.BAMBOO_PLANKS);
        }

        private static void buildBambooClusters(EditSession es, int ox, int oy, int oz) {
            int[][] positions = {
                {ox + 5,  oz + 5},  {ox + 73, oz + 5},
                {ox + 5,  oz + 73}, {ox + 73, oz + 73},
                {ox + 25, oz + 25}, {ox + 55, oz + 25},
                {ox + 25, oz + 55}, {ox + 55, oz + 55}
            };
            int[] stalks = {5, 4, 4, 5, 3, 4, 5, 4};
            for (int i = 0; i < positions.length; i++) {
                int bx = positions[i][0], bz = positions[i][1];
                for (int dx = 0; dx <= 1; dx++)
                    for (int dz = 0; dz <= 1; dz++)
                        column(es, bx + dx, bz + dz, oy + 1, oy + stalks[i], BlockTypes.BAMBOO);
            }
        }

        private static void buildCornerPosts(EditSession es, int ox, int oy, int oz) {
            int[][] corners = {
                {ox + 14, oz + 14}, {ox + 65, oz + 14},
                {ox + 14, oz + 65}, {ox + 65, oz + 65}
            };
            for (int[] c : corners) {
                // Thick 2×2 cherry_wood fence post
                for (int dx = 0; dx <= 1; dx++)
                    for (int dz = 0; dz <= 1; dz++) {
                        column(es, c[0] + dx, c[1] + dz, oy + 1, oy + 8, BlockTypes.CHERRY_LOG);
                        placeBlock(es, c[0] + dx, oy + 9, c[1] + dz, BlockTypes.LANTERN);
                    }
            }
        }
    }

    // =========================================================================
    // ARENA 4 – Tropical Beach Cove   (originX = 600)
    // =========================================================================
    public static class TropicalBeachCove {
        public static void build(EditSession es, int ox, int oy, int oz) {
            buildBeach(es, ox, oy, oz);
            buildOcean(es, ox, oy, oz);
            buildPalmTrees(es, ox, oy, oz);
            buildCoralDecor(es, ox, oy, oz);
        }

        private static void buildBeach(EditSession es, int ox, int oy, int oz) {
            // Western half: sand beach
            fill(es, ox, oy, oz, ox + 39, oy, oz + 79, BlockTypes.SAND);
            fill(es, ox, oy - 1, oz, ox + 39, oy - 1, oz + 79, BlockTypes.SANDSTONE);
            fill(es, ox, oy - 8, oz, ox + 39, oy - 2, oz + 79, BlockTypes.STONE);
        }

        private static void buildOcean(EditSession es, int ox, int oy, int oz) {
            // Eastern half: shallow ocean, 3 water layers (y=oy-1..oy+1)
            fill(es, ox + 40, oy - 2, oz, ox + 79, oy - 2, oz + 79, BlockTypes.SAND);
            fill(es, ox + 40, oy - 1, oz, ox + 79, oy + 1, oz + 79, BlockTypes.WATER);
            fill(es, ox + 40, oy - 8, oz, ox + 79, oy - 3, oz + 79, BlockTypes.STONE);
        }

        private static void buildPalmTrees(EditSession es, int ox, int oy, int oz) {
            int[][] trunks = {
                {ox + 8,  oz + 15}, {ox + 8,  oz + 40},
                {ox + 8,  oz + 65}, {ox + 20, oz + 25},
                {ox + 20, oz + 55}, {ox + 30, oz + 10}
            };
            for (int[] t : trunks) {
                // Trunk: jungle_log y=oy+1..oy+8
                column(es, t[0], t[1], oy + 1, oy + 8, BlockTypes.JUNGLE_LOG);
                // Leaf canopy: 3×3×2 at y=oy+9 and oy+10
                fill(es, t[0] - 1, oy + 9, t[1] - 1, t[0] + 1, oy + 10, t[1] + 1,
                        BlockTypes.JUNGLE_LEAVES);
                // Top tuft
                placeBlock(es, t[0], oy + 11, t[1], BlockTypes.JUNGLE_LEAVES);
            }
        }

        private static void buildCoralDecor(EditSession es, int ox, int oy, int oz) {
            int[][] coralSpots = {
                {ox + 45, oz + 15}, {ox + 55, oz + 30},
                {ox + 60, oz + 50}, {ox + 70, oz + 20},
                {ox + 48, oz + 65}, {ox + 72, oz + 60}
            };
            com.sk89q.worldedit.world.block.BlockType[] coralTypes = {
                BlockTypes.BRAIN_CORAL_BLOCK,
                BlockTypes.TUBE_CORAL_BLOCK,
                BlockTypes.FIRE_CORAL_BLOCK,
                BlockTypes.HORN_CORAL_BLOCK,
                BlockTypes.BUBBLE_CORAL_BLOCK,
                BlockTypes.BRAIN_CORAL_BLOCK
            };
            for (int i = 0; i < coralSpots.length; i++) {
                int cx = coralSpots[i][0], cz = coralSpots[i][1];
                // Place coral cluster on ocean floor
                placeBlock(es, cx,     oy - 1, cz,     coralTypes[i]);
                placeBlock(es, cx + 1, oy - 1, cz,     coralTypes[(i + 1) % coralTypes.length]);
                placeBlock(es, cx,     oy - 1, cz + 1, coralTypes[(i + 2) % coralTypes.length]);
            }
        }
    }

    // =========================================================================
    // ARENA 5 – Ancient Overgrown Pyramid   (originX = 800)
    // =========================================================================
    public static class AncientPyramid {
        public static void build(EditSession es, int ox, int oy, int oz) {
            grassBase(es, ox, oy, oz);
            buildPyramid(es, ox, oy, oz);
            buildOvergrowth(es, ox, oy, oz);
        }

        private static void buildPyramid(EditSession es, int ox, int oy, int oz) {
            // Pyramid centred at ox+39, oz+39
            int cx = ox + 39, cz = oz + 39;

            // Level 5 (base): 50×50, y=oy..oy+2   (half=25 → x: cx-25..cx+24)
            buildPyramidLevel(es, cx, oy,      cz, 25, BlockTypes.SANDSTONE,
                    BlockTypes.STONE_BRICKS);
            // Level 4: 40×40, y=oy+3..oy+5
            buildPyramidLevel(es, cx, oy + 3,  cz, 20, BlockTypes.SANDSTONE,
                    BlockTypes.MOSSY_STONE_BRICKS);
            // Level 3: 30×30, y=oy+6..oy+8
            buildPyramidLevel(es, cx, oy + 6,  cz, 15, BlockTypes.SANDSTONE,
                    BlockTypes.STONE_BRICKS);
            // Level 2: 20×20, y=oy+9..oy+11
            buildPyramidLevel(es, cx, oy + 9,  cz, 10, BlockTypes.CUT_SANDSTONE,
                    BlockTypes.STONE_BRICKS);
            // Level 1 (top): 10×10, y=oy+12..oy+14
            buildPyramidLevel(es, cx, oy + 12, cz,  5, BlockTypes.SMOOTH_SANDSTONE,
                    BlockTypes.STONE_BRICKS);
        }

        /**
         * Place one pyramid level as a 2*half × 2*half slab from y to y+2.
         * Every 6th block along the perimeter is swapped with the accent type.
         */
        private static void buildPyramidLevel(EditSession es,
                                              int cx, int baseY, int cz, int half,
                                              com.sk89q.worldedit.world.block.BlockType main,
                                              com.sk89q.worldedit.world.block.BlockType accent) {
            for (int x = cx - half; x < cx + half; x++) {
                for (int z = cz - half; z < cz + half; z++) {
                    boolean isAccent = ((x + z) % 7 == 0);
                    com.sk89q.worldedit.world.block.BlockType type = isAccent ? accent : main;
                    fill(es, x, baseY, z, x, baseY + 2, z, type);
                }
            }
        }

        private static void buildOvergrowth(EditSession es, int ox, int oy, int oz) {
            int cx = ox + 39, cz = oz + 39;

            // Jungle leaves patches on pyramid corners at each level
            int[][] levelData = {
                {25, oy + 3}, {20, oy + 6}, {15, oy + 9}, {10, oy + 12}
            };
            for (int[] ld : levelData) {
                int h = ld[0], topY = ld[1];
                int[][] corners = {
                    {cx - h, cz - h}, {cx + h - 1, cz - h},
                    {cx - h, cz + h - 1}, {cx + h - 1, cz + h - 1}
                };
                for (int[] c : corners) {
                    fill(es, c[0] - 1, topY, c[1] - 1, c[0] + 1, topY + 1, c[1] + 1,
                            BlockTypes.JUNGLE_LEAVES);
                }
            }

            // Ground-level leaf clusters around base
            int[][] groundClusters = {
                {cx - 30, cz - 30}, {cx + 28, cz - 30},
                {cx - 30, cz + 28}, {cx + 28, cz + 28},
                {cx - 35, cz},      {cx + 33, cz},
                {cx,      cz - 35}, {cx,      cz + 33}
            };
            for (int[] gc : groundClusters) {
                // 3-block radius sphere of leaves at oy+1..oy+4
                for (int dx = -2; dx <= 2; dx++)
                    for (int dy = 0; dy <= 3; dy++)
                        for (int dz = -2; dz <= 2; dz++) {
                            double d = Math.sqrt(dx * dx + (dy - 1.5) * (dy - 1.5) + dz * dz);
                            if (d <= 2.5)
                                placeBlock(es, gc[0] + dx, oy + 1 + dy, gc[1] + dz,
                                        BlockTypes.JUNGLE_LEAVES);
                        }
            }

            // Moss stone patches on the base level surface
            for (int x = cx - 24; x < cx + 24; x += 5) {
                for (int z = cz - 24; z < cz + 24; z += 7) {
                    placeBlock(es, x, oy + 3, z, BlockTypes.MOSSY_STONE_BRICKS);
                }
            }
        }
    }

    // =========================================================================
    // Entry point – build all 5 arenas
    // =========================================================================
    public static void buildAll(EditSession es, int baseY) {
        JungleRuins.build(es,        0, baseY, 0);
        MangroveSwampBay.build(es,  200, baseY, 0);
        BambooFortress.build(es,    400, baseY, 0);
        TropicalBeachCove.build(es, 600, baseY, 0);
        AncientPyramid.build(es,    800, baseY, 0);
    }
}
