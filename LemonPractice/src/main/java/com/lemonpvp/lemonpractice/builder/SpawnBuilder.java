package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * Builds a tropical void spawn island centred at world (0, 64, 0).
 *
 * Structure overview
 * ------------------
 *  - Elliptical floating island  (±28 x / ±26 z radius), grass cap, dirt body, stone core
 *  - Central sandstone pavilion  (12×12 base, 4 sandstone_pillar columns, sandstone roof)
 *  - Two jungle huts             at (−18, 64, 0) and (+18, 64, 0)
 *  - Rope bridge                 oak_fence + spruce_planks, y=65, x=−12..+12, z=0
 *  - Water pool                  3×3 water source at (−1..1, 64, −17..−15), stone_brick rim
 *  - Bamboo clusters             8 fixed positions within 20-block radius
 *  - Sea lanterns                embedded in pavilion floor (4 positions)
 *  - Lanterns                    atop fence posts at bridge ends
 *
 * All placements are deterministic (no RNG).
 */
public class SpawnBuilder extends BuildHelper {

    public SpawnBuilder(LemonPractice plugin, org.bukkit.World world) {
        super(plugin, world);
    }

    @Override
    public void build() {
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .fastMode(true)
                .build()) {

            buildIslandBase(editSession);
            buildPavilion(editSession);
            buildHuts(editSession);
            buildBridge(editSession);
            buildWaterPool(editSession);
            buildBamboo(editSession);
            buildLighting(editSession);

        } catch (Exception e) {
            plugin.getLogger().severe("[SpawnBuilder] Error building spawn: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Island base – elliptical platform with natural-ish height variation
    // -------------------------------------------------------------------------
    private void buildIslandBase(EditSession es) {
        for (int x = -28; x <= 28; x++) {
            for (int z = -28; z <= 28; z++) {
                // Ellipse test: (x/28)^2 + (z/26)^2 <= 1
                double ex = (double) x / 28.0;
                double ez = (double) z / 26.0;
                if (ex * ex + ez * ez > 1.0) continue;

                // Slight height bump towards centre for a natural mound look.
                // heightOffset lowers the surface by 0..2 at the edges.
                int distSq = x * x + z * z;
                int heightOffset = (distSq > 400) ? 1 : 0; // flat centre, slight drop at edge
                int surfaceY = 64 - heightOffset;

                // Grass cap
                block(es, x, surfaceY, z, BlockTypes.GRASS_BLOCK);
                // Dirt body (2 layers below grass)
                fill(es, x, surfaceY - 2, z, x, surfaceY - 1, z, BlockTypes.DIRT);
                // Stone core
                fill(es, x, 55, z, x, surfaceY - 3, z, BlockTypes.STONE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Central 12×12 sandstone pavilion, centred at (0,64,0)
    // -------------------------------------------------------------------------
    private void buildPavilion(EditSession es) {
        // Floor: sandstone 10×10 at y=64 (x=−5..5, z=−5..5)
        fill(es, -5, 64, -5, 5, 64, 5, BlockTypes.SANDSTONE);

        // Sea lanterns embedded in floor (replace 4 tiles)
        block(es, -3, 64, -3, BlockTypes.SEA_LANTERN);
        block(es,  3, 64, -3, BlockTypes.SEA_LANTERN);
        block(es, -3, 64,  3, BlockTypes.SEA_LANTERN);
        block(es,  3, 64,  3, BlockTypes.SEA_LANTERN);

        // 4 sandstone_pillar columns from y=65 to y=72 at corners (±5, ±5)
        for (int[] corner : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
            column(es, corner[0], corner[1], 65, 72, BlockTypes.CHISELED_SANDSTONE);
        }

        // Walls at x=±5 and z=±5 between pillars, y=65..70 (leave open top for light)
        for (int y = 65; y <= 70; y++) {
            for (int i = -4; i <= 4; i++) {
                block(es, i, y, -5, BlockTypes.SANDSTONE);  // south wall
                block(es, i, y,  5, BlockTypes.SANDSTONE);  // north wall
                block(es, -5, y, i, BlockTypes.SANDSTONE);  // west wall
                block(es,  5, y, i, BlockTypes.SANDSTONE);  // east wall
            }
        }

        // Central raised platform at y=65 (3×3)
        fill(es, -1, 65, -1, 1, 65, 1, BlockTypes.SANDSTONE);
        block(es, 0, 66, 0, BlockTypes.CHISELED_SANDSTONE); // centrepiece accent

        // Roof: sandstone slab layer at y=73 covering the full 12×12 (x=−6..6, z=−6..6)
        fill(es, -6, 73, -6, 6, 73, 6, BlockTypes.SANDSTONE);
        // Inner ceiling (underside of roof, one slab in)
        fill(es, -5, 72, -5, 5, 72, 5, BlockTypes.SANDSTONE_SLAB);

        // Doorways: clear a 2-wide opening on each of the 4 walls
        for (int dy = 65; dy <= 67; dy++) {
            // South opening (z=−5, x=−1..1)
            for (int dx = -1; dx <= 1; dx++) {
                block(es, dx, dy, -5, BlockTypes.AIR);
                block(es, dx, dy,  5, BlockTypes.AIR);
                block(es, -5, dy, dx, BlockTypes.AIR);
                block(es,  5, dy, dx, BlockTypes.AIR);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Two jungle huts at x=±18, z=0
    // -------------------------------------------------------------------------
    private void buildHuts(EditSession es) {
        buildSingleHut(es, -18, 64, 0);
        buildSingleHut(es,  18, 64, 0);
    }

    private void buildSingleHut(EditSession es, int cx, int baseY, int cz) {
        int x1 = cx - 2, x2 = cx + 2;
        int z1 = cz - 2, z2 = cz + 2;

        // Floor: jungle_planks
        fill(es, x1, baseY, z1, x2, baseY, z2, BlockTypes.JUNGLE_PLANKS);

        // Walls: jungle_log, y=65..68, hollow
        for (int y = baseY + 1; y <= baseY + 4; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (x == x1 || x == x2 || z == z1 || z == z2) {
                        block(es, x, y, z, BlockTypes.JUNGLE_LOG);
                    }
                }
            }
        }

        // Front doorway: clear 1-wide, 2-tall in the wall facing inward (towards z=0 bridge)
        int doorZ = (cz <= 0) ? z2 : z1; // wall facing the bridge
        int doorX = cx;
        block(es, doorX, baseY + 1, doorZ, BlockTypes.AIR);
        block(es, doorX, baseY + 2, doorZ, BlockTypes.AIR);

        // Leaf roof: 5×5 at y=baseY+5, overhang of 1
        fill(es, x1 - 1, baseY + 5, z1 - 1, x2 + 1, baseY + 5, z2 + 1, BlockTypes.JUNGLE_LEAVES);
        // Second layer, smaller
        fill(es, x1, baseY + 6, z1, x2, baseY + 6, z2, BlockTypes.JUNGLE_LEAVES);
    }

    // -------------------------------------------------------------------------
    // Rope bridge: oak_fence posts + spruce_planks decking, y=65, z=0, x=−12..12
    // -------------------------------------------------------------------------
    private void buildBridge(EditSession es) {
        for (int x = -12; x <= 12; x++) {
            // Plank decking
            block(es, x, 65, 0, BlockTypes.SPRUCE_PLANKS);
            // Fence railing on both sides (z=−1 and z=+1)
            block(es, x, 65, -1, BlockTypes.OAK_FENCE);
            block(es, x, 65,  1, BlockTypes.OAK_FENCE);
            // Fence post cap railing at +1 height on every 3rd block
            if (x % 3 == 0) {
                block(es, x, 66, -1, BlockTypes.OAK_FENCE);
                block(es, x, 66,  1, BlockTypes.OAK_FENCE);
            }
        }

        // Support posts hanging below the bridge at x=−9, −6, −3, 0, 3, 6, 9
        for (int x : new int[]{-9, -6, -3, 0, 3, 6, 9}) {
            block(es, x, 64, 0, BlockTypes.OAK_FENCE);
        }

        // Lanterns at bridge entry/exit posts
        block(es, -12, 67, -1, BlockTypes.LANTERN);
        block(es, -12, 67,  1, BlockTypes.LANTERN);
        block(es,  12, 67, -1, BlockTypes.LANTERN);
        block(es,  12, 67,  1, BlockTypes.LANTERN);
    }

    // -------------------------------------------------------------------------
    // Water pool at roughly (0, 64, −16): 3×3 water with stone_brick rim
    // -------------------------------------------------------------------------
    private void buildWaterPool(EditSession es) {
        int poolX = 0, poolY = 63, poolZ = -16;

        // Stone_brick rim at y=64 (5×5 ring)
        for (int x = poolX - 2; x <= poolX + 2; x++) {
            for (int z = poolZ - 2; z <= poolZ + 2; z++) {
                if (x == poolX - 2 || x == poolX + 2 || z == poolZ - 2 || z == poolZ + 2) {
                    block(es, x, 64, z, BlockTypes.STONE_BRICKS);
                }
            }
        }

        // Water source blocks inside the rim at y=63 (3×3 depression)
        fill(es, poolX - 1, poolY, poolZ - 1, poolX + 1, poolY, poolZ + 1, BlockTypes.WATER);
        // Stone_brick floor under water
        fill(es, poolX - 1, poolY - 1, poolZ - 1, poolX + 1, poolY - 1, poolZ + 1, BlockTypes.STONE_BRICKS);
    }

    // -------------------------------------------------------------------------
    // Bamboo clusters – 8 fixed positions within 20-block radius
    // -------------------------------------------------------------------------
    private void buildBamboo(EditSession es) {
        int[][] clusters = {
            {-12, 10}, {12, 10}, {-10, -12}, {10, -12},
            {-20,  5}, {20,  5}, { -8,  18}, { 8,  18}
        };
        // Heights for variety
        int[] heights = {4, 5, 4, 5, 3, 4, 5, 4};

        for (int i = 0; i < clusters.length; i++) {
            int cx = clusters[i][0];
            int cz = clusters[i][1];
            int h  = heights[i];

            // Main stalk
            column(es, cx, cz, 65, 65 + h, BlockTypes.BAMBOO);
            // Adjacent stalks at +1 offsets
            column(es, cx + 1, cz, 65, 65 + h - 1, BlockTypes.BAMBOO);
            column(es, cx, cz + 1, 65, 65 + h - 1, BlockTypes.BAMBOO);

            // Fern at base
            block(es, cx - 1, 65, cz, BlockTypes.FERN);
            block(es, cx, 65, cz - 1, BlockTypes.FERN);
        }
    }

    // -------------------------------------------------------------------------
    // Lighting – sea lanterns already in pavilion floor; add torch-style lanterns
    // on bridge is done in buildBridge(); add a few extra around island.
    // -------------------------------------------------------------------------
    private void buildLighting(EditSession es) {
        // Lantern posts near hut entrances
        block(es, -15, 65, 2, BlockTypes.OAK_FENCE);
        block(es, -15, 66, 2, BlockTypes.LANTERN);
        block(es,  15, 65, 2, BlockTypes.OAK_FENCE);
        block(es,  15, 66, 2, BlockTypes.LANTERN);

        // Lantern near water pool
        block(es, 3, 65, -16, BlockTypes.OAK_FENCE);
        block(es, 3, 66, -16, BlockTypes.LANTERN);

        // Sea lanterns on pavilion roof corners
        block(es, -6, 74, -6, BlockTypes.SEA_LANTERN);
        block(es,  6, 74, -6, BlockTypes.SEA_LANTERN);
        block(es, -6, 74,  6, BlockTypes.SEA_LANTERN);
        block(es,  6, 74,  6, BlockTypes.SEA_LANTERN);
    }
}
