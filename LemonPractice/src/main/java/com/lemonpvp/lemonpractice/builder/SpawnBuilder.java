package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * SpawnBuilder — "Citadel Aeternum"
 *
 * <p>A grand 180×214 floating island practice hub centred at world origin (0, 64, 0).
 * The island ellipse has half-radii RX=90 (east–west) and RZ=107 (north–south),
 * with the surface floor at Y=64 (SY).
 *
 * <h2>Portal Locations</h2>
 * <ul>
 *   <li>Crystal  — East,       (55, 64,   0)</li>
 *   <li>Sword    — West,      (-55, 64,   0)</li>
 *   <li>Mace     — South,     (  0, 64,  65)</li>
 *   <li>Bow      — North,     (  0, 64, -65)</li>
 *   <li>Totem    — NE,        ( 38, 64, -45)</li>
 *   <li>Axe      — NW,        (-38, 64, -45)</li>
 *   <li>Trident  — SE,        ( 38, 64,  55)</li>
 *   <li>Shield   — SW,        (-38, 64,  55)</li>
 * </ul>
 *
 * <h2>Themed Zones</h2>
 * <ul>
 *   <li>Forest    — NE quadrant</li>
 *   <li>Water     — SW quadrant</li>
 *   <li>Arena     — NW quadrant</li>
 *   <li>Ruins     — SE quadrant</li>
 * </ul>
 *
 * <p>Build order: island terrain → central structures → portals →
 * connectivity → zones → fortifications → lighting → details.
 */
public class SpawnBuilder extends BuildHelper {

    // ── Island geometry constants ────────────────────────────────────────────────
    private static final int RX = 90;   // island east-west half-radius
    private static final int RZ = 107;  // island north-south half-radius
    private static final int SY = 64;   // surface Y level

    // ============================================================================
    //  Constructor
    // ============================================================================

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

            // ─── Terrain ───────────────────────────────────────────────────────
            buildIslandBase(es);
            buildIslandUnderbelly(es);

            // ─── Central structures ────────────────────────────────────────────
            buildCentralPlaza(es);
            buildCentralFountains(es);
            buildMainTower(es);
            buildTowerInterior(es);
            buildSecondaryHubs(es);

            // ─── Portals ───────────────────────────────────────────────────────
            buildCrystalPortal(es);
            buildSwordPortal(es);
            buildMacePortal(es);
            buildBowPortal(es);
            buildTotemPortal(es);
            buildAxePortal(es);
            buildTridentPortal(es);
            buildShieldPortal(es);

            // ─── Connectivity ──────────────────────────────────────────────────
            buildGrandStaircase(es);
            buildRingRoad(es);
            buildPathways(es);
            buildBridges(es);

            // ─── Zones ─────────────────────────────────────────────────────────
            buildForestZone(es);
            buildWaterGardens(es);
            buildArenaZone(es);
            buildRuinsZone(es);
            buildVegetation(es);

            // ─── Fortifications ────────────────────────────────────────────────
            buildPerimeterWalls(es);
            buildWatchtowers(es);

            // ─── Details & lighting ────────────────────────────────────────────
            buildLampPosts(es);
            buildUndergroundCaves(es);
            buildFinalAccents(es);

        } catch (Exception e) {
            plugin.getLogger().severe("[SpawnBuilder] Build failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    //  TERRAIN
    // ============================================================================

    /**
     * Lays the main island surface and layered cross-section for every column
     * that falls within the island ellipse.  The island tapers from a thick
     * centre (bottom at Y≈40) to thin edges (bottom at Y≈61), producing a
     * natural floating-island silhouette.
     */
    private void buildIslandBase(EditSession es) {
        for (int x = -RX; x <= RX; x++) {
            for (int z = -RZ; z <= RZ; z++) {

                // Ellipse membership test
                double ex = (double) x / RX;
                double ez = (double) z / RZ;
                double t  = ex * ex + ez * ez;
                if (t > 1.0) continue;

                // Bottom Y: centre sits deepest, edge sits shallowest
                int bottomY = 40 + (int) (21.0 * t);

                // ── Surface block – varies with edge proximity and pattern ────
                BlockType surface;
                if (t > 0.90) {
                    // Outermost fringe – coarse dirt cliff edge
                    surface = BlockTypes.COARSE_DIRT;
                } else if (t > 0.78 && (Math.abs(x) % 6 == 0 || Math.abs(z) % 7 == 0)) {
                    // Grid stripe pattern near the rim
                    surface = BlockTypes.COARSE_DIRT;
                } else if (t > 0.62 && (x + z) % 8 == 0) {
                    // Diagonal accent dots in the mid-ring
                    surface = BlockTypes.DIRT;
                } else {
                    surface = BlockTypes.GRASS_BLOCK;
                }
                block(es, x, SY, z, surface);

                // ── Dirt sub-surface: 2 layers below the grass ───────────────
                fill(es, x, SY - 2, z, x, SY - 1, z, BlockTypes.DIRT);

                // ── Stone layer ───────────────────────────────────────────────
                int stoneTop    = SY - 3;
                int stoneBottom = Math.max(bottomY + 7, SY - 16);
                if (stoneTop >= stoneBottom) {
                    fill(es, x, stoneBottom, z, x, stoneTop, z, BlockTypes.STONE);
                }

                // ── Deepslate layer beneath stone ─────────────────────────────
                int deepTop    = stoneBottom - 1;
                int deepBottom = bottomY + 4;
                if (deepTop >= deepBottom) {
                    fill(es, x, deepBottom, z, x, deepTop, z, BlockTypes.DEEPSLATE);
                }

                // ── Visible bottom rim – 4-layer decorative underside ─────────
                block(es, x, bottomY + 3, z, BlockTypes.COBBLED_DEEPSLATE);
                block(es, x, bottomY + 2, z, BlockTypes.COBBLESTONE);
                block(es, x, bottomY + 1, z, BlockTypes.GRAVEL);
                block(es, x, bottomY,     z, BlockTypes.COBBLED_DEEPSLATE);
            }
        }
    }

    /**
     * Decorates the underside of the island with stalactite clusters,
     * gravel and tuff texture patches, cobbled-deepslate perimeter overhangs,
     * and andesite/tuff vein streaks – all visible when looking up from below.
     */
    private void buildIslandUnderbelly(EditSession es) {

        // ── Stalactite cluster positions (30 hand-placed clusters across
        //    the full interior of the 90×107 ellipse) ──────────────────────────
        int[][] clusterDefs = {
            // {cx, cz, length}  — spread to cover all quadrants
            {  0,   0, 12}, { 18,  14, 10}, {-20,  12,  9}, { 20, -18,  8},
            {-22, -16,  9}, { 36,   8,  7}, {-38, -10,  8}, { 10,  30,  9},
            {-12, -32,  7}, { 30, -22,  8}, {-28,  26,  7}, { 16, -44,  6},
            {-14,  46,  6}, { 50,  -8,  5}, {-52,   6,  5}, { 42,  32,  6},
            {-44, -28,  6}, { 28,  50,  5}, {-26, -50,  5}, { 60,  18,  4},
            {-58, -20,  4}, { 22,  66,  4}, {-24, -68,  4}, { 70,   0,  3},
            {-72,   0,  3}, {  0,  80,  4}, {  0, -82,  4}, { 46, -60,  4},
            {-48,  58,  5}, { 58, -44,  4}
        };

        for (int[] c : clusterDefs) {
            int cx = c[0], cz = c[1], length = c[2];
            double ex = (double) cx / RX;
            double ez = (double) cz / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.80) continue;  // only place inside the 80% ellipse

            int bottomY = 40 + (int) (21.0 * t);

            // Taper downward: wider at the attachment point, narrow at tip
            for (int i = 0; i < length; i++) {
                int y      = bottomY - i;
                int spread = Math.max(0, (length - i - 1) / 2);
                fill(es,
                     cx - spread, y, cz - spread,
                     cx + spread, y, cz + spread,
                     BlockTypes.DEEPSLATE);
                // Occasional cobbled-deepslate crack on the surface of the stalactite
                if (i == 1 && spread > 0) {
                    block(es, cx - spread, y, cz,           BlockTypes.COBBLED_DEEPSLATE);
                    block(es, cx + spread, y, cz,           BlockTypes.COBBLED_DEEPSLATE);
                    block(es, cx,          y, cz - spread,  BlockTypes.COBBLED_DEEPSLATE);
                    block(es, cx,          y, cz + spread,  BlockTypes.COBBLED_DEEPSLATE);
                }
            }
        }

        // ── Gravel and tuff texture patches across underside surface ──────────
        for (int x = -RX + 5; x <= RX - 5; x += 5) {
            for (int z = -RZ + 6; z <= RZ - 6; z += 6) {
                double ex = (double) x / RX;
                double ez = (double) z / RZ;
                double t  = ex * ex + ez * ez;
                if (t > 0.76) continue;
                int bottomY = 40 + (int) (21.0 * t);

                // Gravel on bottom rim surface
                block(es, x, bottomY + 3, z, BlockTypes.GRAVEL);
                // Tuff occasional accent
                if ((x + z) % 11 == 0) {
                    block(es, x, bottomY + 3, z, BlockTypes.TUFF);
                }
                // Andesite patches mid-underside
                if ((x * 3 + z * 7) % 13 == 0) {
                    block(es, x, bottomY + 5, z, BlockTypes.ANDESITE);
                }
            }
        }

        // ── Cobbled-deepslate overhangs at perimeter – every 4° ───────────────
        for (int angle = 0; angle < 360; angle += 4) {
            double rad   = Math.toRadians(angle);
            int    wx    = (int) Math.round((RX - 6) * Math.cos(rad));
            int    wz    = (int) Math.round((RZ - 6) * Math.sin(rad));
            double ex    = (double) wx / RX;
            double ez    = (double) wz / RZ;
            double t     = ex * ex + ez * ez;
            int    botY  = 40 + (int) (21.0 * t);

            block(es, wx,     botY + 4, wz,     BlockTypes.COBBLED_DEEPSLATE);
            block(es, wx,     botY + 5, wz,     BlockTypes.STONE);
            block(es, wx + 1, botY + 4, wz,     BlockTypes.COBBLED_DEEPSLATE);
            block(es, wx - 1, botY + 4, wz,     BlockTypes.COBBLED_DEEPSLATE);
            block(es, wx,     botY + 4, wz + 1, BlockTypes.COBBLED_DEEPSLATE);
            block(es, wx,     botY + 4, wz - 1, BlockTypes.COBBLED_DEEPSLATE);
        }

        // ── Tuff + andesite veins at ~12 scattered positions ──────────────────
        int[][] veinSeeds = {
            { 14,  20}, {-16, -18}, { 30, -10}, {-32,  12},
            {  6,  52}, { -8, -54}, { 48,  24}, {-46, -26},
            { 62, -14}, {-60,  16}, { 24,  70}, {-22, -72}
        };
        for (int vi = 0; vi < veinSeeds.length; vi++) {
            int vx = veinSeeds[vi][0], vz = veinSeeds[vi][1];
            double ex = (double) vx / RX;
            double ez = (double) vz / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.82) continue;
            int botY = 40 + (int) (21.0 * t);

            BlockType veinMat = (vi % 2 == 0) ? BlockTypes.TUFF : BlockTypes.ANDESITE;
            // Place a 3-block diagonal vein on the underside
            for (int i = -2; i <= 2; i++) {
                block(es, vx + i,     botY + 5, vz,     veinMat);
                block(es, vx,         botY + 5, vz + i, veinMat);
                block(es, vx + i,     botY + 6, vz + i, veinMat);
            }
            // Occasional polished-andesite geode-like accent
            if (vi % 3 == 0) {
                block(es, vx, botY + 7, vz, BlockTypes.POLISHED_ANDESITE);
            }
        }
    }

    // ============================================================================
    //  CENTRAL STRUCTURES
    // ============================================================================

    /**
     * Builds the central plaza: a polished-deepslate disk of radius 20 at SY,
     * with concentric decorative rings, 12 ornamental pillars, and step rings
     * transitioning down to island level.
     */
    private void buildCentralPlaza(EditSession es) {

        // ── Full plaza disk fill – radius 20 ─────────────────────────────────
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                double dist = Math.sqrt((double)(x * x + z * z));
                if (dist > 20.0) continue;

                BlockType tile;
                if (dist < 10.0) {
                    // Inner solid zone
                    tile = BlockTypes.POLISHED_DEEPSLATE;
                } else if (dist >= 10.5 && dist <= 11.5) {
                    // Chiseled accent ring
                    tile = BlockTypes.CHISELED_DEEPSLATE;
                } else if (dist >= 16.5 && dist <= 17.5) {
                    // Quartz decorative ring
                    tile = BlockTypes.QUARTZ_BRICKS;
                } else {
                    // Outer checkerboard (r=12 to r=20)
                    boolean alt = (Math.abs(x) + Math.abs(z)) % 2 == 0;
                    tile = alt ? BlockTypes.POLISHED_DEEPSLATE : BlockTypes.DEEPSLATE_TILES;
                }
                block(es, x, SY, z, tile);
            }
        }

        // ── 12 ornamental pillars evenly spaced at r=17, every 30° ───────────
        for (int pillarIndex = 0; pillarIndex < 12; pillarIndex++) {
            double rad = Math.toRadians(pillarIndex * 30.0);
            int px = (int) Math.round(17.0 * Math.cos(rad));
            int pz = (int) Math.round(17.0 * Math.sin(rad));

            // Base block
            block(es, px, SY,     pz, BlockTypes.DEEPSLATE_BRICKS);
            // 5-tall quartz pillar shaft
            column(es, px, pz, SY + 1, SY + 5, BlockTypes.QUARTZ_PILLAR);
            // Capital
            block(es, px, SY + 6, pz, BlockTypes.POLISHED_DEEPSLATE);
            // Lantern on top
            block(es, px, SY + 7, pz, BlockTypes.LANTERN);

            // Chains between alternate pillar midpoints
            if (pillarIndex % 2 == 0) {
                double radNext = Math.toRadians((pillarIndex + 1) * 30.0);
                int mx = (int) Math.round(16.5 * Math.cos(radNext - Math.toRadians(15)));
                int mz = (int) Math.round(16.5 * Math.sin(radNext - Math.toRadians(15)));
                block(es, mx, SY + 4, mz, BlockTypes.CHAIN);
                block(es, mx, SY + 5, mz, BlockTypes.CHAIN);
            }
        }

        // ── Step ring at r=20.8 (outer transition) ────────────────────────────
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int sx = (int) Math.round(20.8 * Math.cos(rad));
            int sz = (int) Math.round(20.8 * Math.sin(rad));
            // Boundary check: must still be on island
            double ex = (double) sx / RX;
            double ez = (double) sz / RZ;
            if (ex * ex + ez * ez > 0.95) continue;
            block(es, sx, SY, sz, BlockTypes.STONE_BRICKS);
        }

        // ── Secondary step ring at r=21.8, offset -1Y ─────────────────────────
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int sx = (int) Math.round(21.8 * Math.cos(rad));
            int sz = (int) Math.round(21.8 * Math.sin(rad));
            double ex = (double) sx / RX;
            double ez = (double) sz / RZ;
            if (ex * ex + ez * ez > 0.95) continue;
            block(es, sx, SY - 1, sz, BlockTypes.COBBLESTONE);
        }
    }

    /**
     * Places four themed fountains at (±16, SY, 0) and (0, SY, ±16), each with
     * a 5×5 basin, themed rim, water interior, prismarine floor, sea-lantern,
     * a central polished-andesite post with water on top, and mossy corner
     * accents.  Each fountain uses a slightly different aesthetic.
     */
    private void buildCentralFountains(EditSession es) {

        // ── Fountain 1: East (16, SY, 0) — Smooth-quartz / elegant theme ─────
        {
            int fx = 16, fz = 0;
            for (int x = fx - 2; x <= fx + 2; x++) {
                for (int z = fz - 2; z <= fz + 2; z++) {
                    boolean edge = (x == fx - 2 || x == fx + 2 || z == fz - 2 || z == fz + 2);
                    if (edge) {
                        block(es, x, SY,     z, BlockTypes.SMOOTH_QUARTZ);
                        block(es, x, SY + 1, z, BlockTypes.STONE_BRICK_WALL);
                    } else {
                        block(es, x, SY - 1, z, BlockTypes.WATER);
                        block(es, x, SY - 2, z, BlockTypes.PRISMARINE);
                        block(es, x, SY,     z, BlockTypes.AIR);
                    }
                }
            }
            block(es, fx, SY - 2, fz, BlockTypes.SEA_LANTERN);
            block(es, fx, SY,     fz, BlockTypes.POLISHED_ANDESITE);
            block(es, fx, SY + 1, fz, BlockTypes.WATER);
            // Mossy corner accents
            block(es, fx - 2, SY + 2, fz - 2, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, fx + 2, SY + 2, fz - 2, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, fx - 2, SY + 2, fz + 2, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, fx + 2, SY + 2, fz + 2, BlockTypes.MOSSY_STONE_BRICKS);
        }

        // ── Fountain 2: West (-16, SY, 0) — Stone-bricks / classic theme ─────
        {
            int fx = -16, fz = 0;
            for (int x = fx - 2; x <= fx + 2; x++) {
                for (int z = fz - 2; z <= fz + 2; z++) {
                    boolean edge = (x == fx - 2 || x == fx + 2 || z == fz - 2 || z == fz + 2);
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
            block(es, fx, SY - 2, fz, BlockTypes.SEA_LANTERN);
            block(es, fx, SY,     fz, BlockTypes.POLISHED_ANDESITE);
            block(es, fx, SY + 1, fz, BlockTypes.WATER);
            block(es, fx - 2, SY + 2, fz - 2, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, fx + 2, SY + 2, fz - 2, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, fx - 2, SY + 2, fz + 2, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, fx + 2, SY + 2, fz + 2, BlockTypes.MOSSY_STONE_BRICKS);
        }

        // ── Fountain 3: North (0, SY, -16) — Mossy-stone-bricks / nature theme ─
        {
            int fx = 0, fz = -16;
            for (int x = fx - 2; x <= fx + 2; x++) {
                for (int z = fz - 2; z <= fz + 2; z++) {
                    boolean edge = (x == fx - 2 || x == fx + 2 || z == fz - 2 || z == fz + 2);
                    if (edge) {
                        block(es, x, SY,     z, BlockTypes.MOSSY_STONE_BRICKS);
                        block(es, x, SY + 1, z, BlockTypes.STONE_BRICK_WALL);
                    } else {
                        block(es, x, SY - 1, z, BlockTypes.WATER);
                        block(es, x, SY - 2, z, BlockTypes.PRISMARINE);
                        block(es, x, SY,     z, BlockTypes.AIR);
                    }
                }
            }
            block(es, fx, SY - 2, fz, BlockTypes.SEA_LANTERN);
            block(es, fx, SY,     fz, BlockTypes.POLISHED_ANDESITE);
            block(es, fx, SY + 1, fz, BlockTypes.WATER);
            block(es, fx - 2, SY + 2, fz - 2, BlockTypes.MOSSY_COBBLESTONE);
            block(es, fx + 2, SY + 2, fz - 2, BlockTypes.MOSSY_COBBLESTONE);
            block(es, fx - 2, SY + 2, fz + 2, BlockTypes.MOSSY_COBBLESTONE);
            block(es, fx + 2, SY + 2, fz + 2, BlockTypes.MOSSY_COBBLESTONE);
        }

        // ── Fountain 4: South (0, SY, 16) — Deepslate-bricks / dark theme ────
        {
            int fx = 0, fz = 16;
            for (int x = fx - 2; x <= fx + 2; x++) {
                for (int z = fz - 2; z <= fz + 2; z++) {
                    boolean edge = (x == fx - 2 || x == fx + 2 || z == fz - 2 || z == fz + 2);
                    if (edge) {
                        block(es, x, SY,     z, BlockTypes.DEEPSLATE_BRICKS);
                        block(es, x, SY + 1, z, BlockTypes.STONE_BRICK_WALL);
                    } else {
                        block(es, x, SY - 1, z, BlockTypes.WATER);
                        block(es, x, SY - 2, z, BlockTypes.PRISMARINE);
                        block(es, x, SY,     z, BlockTypes.AIR);
                    }
                }
            }
            block(es, fx, SY - 2, fz, BlockTypes.SEA_LANTERN);
            block(es, fx, SY,     fz, BlockTypes.POLISHED_ANDESITE);
            block(es, fx, SY + 1, fz, BlockTypes.WATER);
            block(es, fx - 2, SY + 2, fz - 2, BlockTypes.CHISELED_DEEPSLATE);
            block(es, fx + 2, SY + 2, fz - 2, BlockTypes.CHISELED_DEEPSLATE);
            block(es, fx - 2, SY + 2, fz + 2, BlockTypes.CHISELED_DEEPSLATE);
            block(es, fx + 2, SY + 2, fz + 2, BlockTypes.CHISELED_DEEPSLATE);
        }
    }

    /**
     * Builds the main central tower: 19×19 footprint (x=−9..9, z=−9..9),
     * 55 blocks tall.  Includes a multi-layered body, arched windows,
     * entryways, observation deck, battlements, four corner turrets,
     * and a tapering quartz spire.
     */
    private void buildMainTower(EditSession es) {

        // ── Foundation at SY ─────────────────────────────────────────────────
        fill(es, -9, SY, -9, 9, SY, 9, BlockTypes.DEEPSLATE_BRICKS);
        // Chiseled border at SY+1 on perimeter
        for (int x = -9; x <= 9; x++) {
            block(es, x, SY + 1, -9, BlockTypes.CHISELED_DEEPSLATE);
            block(es, x, SY + 1,  9, BlockTypes.CHISELED_DEEPSLATE);
        }
        for (int z = -8; z <= 8; z++) {
            block(es, -9, SY + 1, z, BlockTypes.CHISELED_DEEPSLATE);
            block(es,  9, SY + 1, z, BlockTypes.CHISELED_DEEPSLATE);
        }

        // ── Lower body: y = SY+2 to SY+18, 2-block-thick hollow walls ────────
        for (int y = SY + 2; y <= SY + 18; y++) {
            for (int x = -9; x <= 9; x++) {
                for (int z = -9; z <= 9; z++) {
                    boolean isOuter = (x == -9 || x == 9 || z == -9 || z == 9);
                    boolean isInner = (x == -8 || x == 8 || z == -8 || z == 8);
                    if (isOuter) {
                        // Occasional quartz accent block on the outer wall
                        boolean quartzAccent = ((x + z + y) % 5 == 0);
                        block(es, x, y, z,
                              quartzAccent ? BlockTypes.QUARTZ_BRICKS
                                           : BlockTypes.POLISHED_DEEPSLATE);
                    } else if (isInner) {
                        block(es, x, y, z, BlockTypes.DEEPSLATE_BRICKS);
                    }
                }
            }
        }

        // Lower body: 3 windows per face, 1×2 tinted-glass, at y+6..y+7
        //   North (z=-9) and South (z=9) faces — openings at x=-3,0,+3
        for (int wx : new int[]{-3, 0, 3}) {
            block(es, wx, SY + 6,  -9, BlockTypes.TINTED_GLASS);
            block(es, wx, SY + 7,  -9, BlockTypes.TINTED_GLASS);
            block(es, wx, SY + 6,   9, BlockTypes.TINTED_GLASS);
            block(es, wx, SY + 7,   9, BlockTypes.TINTED_GLASS);
        }
        //   East (x=9) and West (x=-9) faces — openings at z=-3,0,+3
        for (int wz : new int[]{-3, 0, 3}) {
            block(es,  9, SY + 6, wz, BlockTypes.TINTED_GLASS);
            block(es,  9, SY + 7, wz, BlockTypes.TINTED_GLASS);
            block(es, -9, SY + 6, wz, BlockTypes.TINTED_GLASS);
            block(es, -9, SY + 7, wz, BlockTypes.TINTED_GLASS);
        }

        // Lower body: entryways 3-wide × 4-tall on North (z=-9) and South (z=9)
        for (int ey = SY + 2; ey <= SY + 5; ey++) {
            for (int ex = -1; ex <= 1; ex++) {
                block(es, ex, ey, -9, BlockTypes.AIR);
                block(es, ex, ey,  9, BlockTypes.AIR);
            }
        }

        // ── Mid body: y = SY+19 to SY+33, 1-block-thick walls ────────────────
        for (int y = SY + 19; y <= SY + 33; y++) {
            for (int x = -9; x <= 9; x++) {
                for (int z = -9; z <= 9; z++) {
                    if (x == -9 || x == 9 || z == -9 || z == 9) {
                        boolean quartzAlt = ((x + z) % 4 == 0);
                        block(es, x, y, z,
                              quartzAlt ? BlockTypes.CHISELED_QUARTZ_BLOCK
                                        : BlockTypes.POLISHED_DEEPSLATE);
                    }
                }
            }
        }

        // Mid body: large 3-wide × 7-tall arched windows on each face, y+20..y+26
        //   North and South
        for (int wy = SY + 20; wy <= SY + 26; wy++) {
            for (int wx = -1; wx <= 1; wx++) {
                block(es, wx, wy, -9, BlockTypes.TINTED_GLASS);
                block(es, wx, wy,  9, BlockTypes.TINTED_GLASS);
                block(es, -9, wy, wx, BlockTypes.TINTED_GLASS);
                block(es,  9, wy, wx, BlockTypes.TINTED_GLASS);
            }
        }
        // Arched top centre-only at SY+27 on mid body
        block(es,  0, SY + 27, -9, BlockTypes.TINTED_GLASS);
        block(es,  0, SY + 27,  9, BlockTypes.TINTED_GLASS);
        block(es, -9, SY + 27,  0, BlockTypes.TINTED_GLASS);
        block(es,  9, SY + 27,  0, BlockTypes.TINTED_GLASS);

        // ── Upper tier: y = SY+34 to SY+40, narrows to 17×17 (±8) ───────────
        for (int y = SY + 34; y <= SY + 40; y++) {
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    if (x == -8 || x == 8 || z == -8 || z == 8) {
                        block(es, x, y, z, BlockTypes.DEEPSLATE_BRICKS);
                    }
                }
            }
        }

        // ── Observation deck at SY+40 ─────────────────────────────────────────
        fill(es, -7, SY + 40, -7, 7, SY + 40, 7, BlockTypes.POLISHED_DEEPSLATE);
        // Quartz accent tiles at corners
        block(es, -6, SY + 40, -6, BlockTypes.QUARTZ_BRICKS);
        block(es,  6, SY + 40, -6, BlockTypes.QUARTZ_BRICKS);
        block(es, -6, SY + 40,  6, BlockTypes.QUARTZ_BRICKS);
        block(es,  6, SY + 40,  6, BlockTypes.QUARTZ_BRICKS);
        // Iron-bars railing at SY+41 on perimeter (±7)
        for (int i = -7; i <= 7; i++) {
            block(es,  i, SY + 41, -7, BlockTypes.IRON_BARS);
            block(es,  i, SY + 41,  7, BlockTypes.IRON_BARS);
            block(es, -7, SY + 41,  i, BlockTypes.IRON_BARS);
            block(es,  7, SY + 41,  i, BlockTypes.IRON_BARS);
        }

        // ── Battlements at SY+41 – merlons every other block ─────────────────
        for (int i = -8; i <= 8; i++) {
            // North face
            if ((i + 8) % 2 == 0) {
                block(es,  i, SY + 41, -8, BlockTypes.DEEPSLATE_BRICKS);
                block(es,  i, SY + 42, -8, BlockTypes.DEEPSLATE_TILES);
            }
            // South face
            if ((i + 8) % 2 == 0) {
                block(es,  i, SY + 41,  8, BlockTypes.DEEPSLATE_BRICKS);
                block(es,  i, SY + 42,  8, BlockTypes.DEEPSLATE_TILES);
            }
        }
        for (int j = -7; j <= 7; j++) {
            // West face
            if ((j + 7) % 2 == 0) {
                block(es, -8, SY + 41, j, BlockTypes.DEEPSLATE_BRICKS);
                block(es, -8, SY + 42, j, BlockTypes.DEEPSLATE_TILES);
            }
            // East face
            if ((j + 7) % 2 == 0) {
                block(es,  8, SY + 41, j, BlockTypes.DEEPSLATE_BRICKS);
                block(es,  8, SY + 42, j, BlockTypes.DEEPSLATE_TILES);
            }
        }

        // ── 4 corner turrets: 4×4 hollow, placed at corners offset outward ────
        //    Positions relative to tower corners at (±9, ±9)
        int[][] turretCorners = {{-9, -9}, {9, -9}, {-9, 9}, {9, 9}};
        int[]   turretOffsetX  = {-1,       1,       -1,      1};
        int[]   turretOffsetZ  = {-1,      -1,        1,      1};

        for (int tc = 0; tc < 4; tc++) {
            int baseTX = turretCorners[tc][0] + turretOffsetX[tc];
            int baseTZ = turretCorners[tc][1] + turretOffsetZ[tc];

            for (int y = SY + 1; y <= SY + 50; y++) {
                for (int dx = 0; dx <= 3; dx++) {
                    for (int dz = 0; dz <= 3; dz++) {
                        // Map dx/dz to absolute coordinates using offsets
                        int bx = baseTX + dx * turretOffsetX[tc];
                        int bz = baseTZ + dz * turretOffsetZ[tc];
                        boolean isTurretWall = (dx == 0 || dx == 3 || dz == 0 || dz == 3);
                        if (isTurretWall) {
                            block(es, bx, y, bz,
                                  y % 4 == 0 ? BlockTypes.QUARTZ_BRICKS
                                             : BlockTypes.CHISELED_DEEPSLATE);
                        }
                    }
                }
            }
            // Turret cap at SY+51
            int capX = baseTX + turretOffsetX[tc];
            int capZ = baseTZ + turretOffsetZ[tc];
            fill(es, capX - 1, SY + 51, capZ - 1, capX + 1, SY + 51, capZ + 1,
                 BlockTypes.POLISHED_DEEPSLATE);
            block(es, capX, SY + 52, capZ, BlockTypes.LANTERN);
        }

        // ── Spire: y = SY+42 to SY+55, tapering quartz profile ───────────────
        //  SY+42..43 — 5×5 base footprint
        fill(es, -2, SY + 42, -2,  2, SY + 43,  2, BlockTypes.QUARTZ_PILLAR);
        //  SY+44..47 — 3×3
        fill(es, -1, SY + 44, -1,  1, SY + 47,  1, BlockTypes.QUARTZ_PILLAR);
        //  SY+48..54 — 1×1 shaft
        column(es, 0, 0, SY + 48, SY + 54, BlockTypes.QUARTZ_PILLAR);
        // Tip
        block(es, 0, SY + 55, 0, BlockTypes.SEA_LANTERN);
        block(es, 0, SY + 56, 0, BlockTypes.CHAIN);
        block(es, 0, SY + 57, 0, BlockTypes.LANTERN);
    }

    /**
     * Furnishes the inside of the main tower: polished-deepslate floors,
     * sea-lantern uplights at corners, a central QUARTZ_PILLAR load-bearing
     * column, secondary columns, a spiral staircase, multiple landing floors,
     * an iron-block beacon base on the observation deck, and bookshelf
     * decorations on interior walls.
     */
    private void buildTowerInterior(EditSession es) {

        // ── Ground floor: polished-deepslate fill ±7 ─────────────────────────
        fill(es, -7, SY + 1, -7, 7, SY + 1, 7, BlockTypes.POLISHED_DEEPSLATE);

        // 4 corner sea-lanterns embedded in the floor
        block(es, -6, SY + 1, -6, BlockTypes.SEA_LANTERN);
        block(es,  6, SY + 1, -6, BlockTypes.SEA_LANTERN);
        block(es, -6, SY + 1,  6, BlockTypes.SEA_LANTERN);
        block(es,  6, SY + 1,  6, BlockTypes.SEA_LANTERN);

        // ── Central load-bearing QUARTZ_PILLAR column ─────────────────────────
        column(es, 0, 0, SY + 2, SY + 18, BlockTypes.QUARTZ_PILLAR);
        block(es, 0, SY + 19, 0, BlockTypes.SEA_LANTERN);

        // ── 4 secondary columns at (±5, 0) and (0, ±5) ───────────────────────
        for (int[] colPos : new int[][]{{-5, 0}, {5, 0}, {0, -5}, {0, 5}}) {
            column(es, colPos[0], colPos[1], SY + 2, SY + 10, BlockTypes.POLISHED_DEEPSLATE);
            block(es, colPos[0], SY + 11, colPos[1], BlockTypes.SEA_LANTERN);
        }

        // ── Spiral staircase – 36 steps wrapping the interior ─────────────────
        // Steps cycle through 4 corner positions (NW, NE, SE, SW) while
        // incrementing y by 1 each step
        int[] spiralX = {-6, 6,  6, -6};
        int[] spiralZ = {-6, -6, 6,  6};
        for (int step = 0; step < 36; step++) {
            int quadrant = step % 4;
            int y        = SY + 2 + step;
            block(es, spiralX[quadrant], y, spiralZ[quadrant],
                  BlockTypes.POLISHED_DEEPSLATE);
            // Landing-slab approach block adjacent to the step
            int lx = spiralX[quadrant] + (spiralX[quadrant] < 0 ? 1 : -1);
            int lz = spiralZ[quadrant] + (spiralZ[quadrant] < 0 ? 1 : -1);
            block(es, lx, y, lz, BlockTypes.DEEPSLATE_TILES);
        }

        // ── Second floor landing at SY+19 ────────────────────────────────────
        fill(es, -6, SY + 19, -6, 6, SY + 19, 6, BlockTypes.DEEPSLATE_BRICKS);
        // Clear interior of second floor (air)
        fill(es, -5, SY + 19, -5, 5, SY + 19, 5, BlockTypes.POLISHED_DEEPSLATE);
        block(es, 0, SY + 20, 0, BlockTypes.SEA_LANTERN);

        // ── Third floor landing at SY+33 ─────────────────────────────────────
        fill(es, -6, SY + 33, -6, 6, SY + 33, 6, BlockTypes.DEEPSLATE_TILES);
        fill(es, -5, SY + 33, -5, 5, SY + 33, 5, BlockTypes.POLISHED_DEEPSLATE);
        block(es, 0, SY + 34, 0, BlockTypes.SEA_LANTERN);

        // ── Observation deck: 3×3 iron-block beacon base at SY+40 ─────────────
        fill(es, -1, SY + 40, -1, 1, SY + 40, 1, BlockTypes.IRON_BLOCK);
        block(es, 0, SY + 41, 0, BlockTypes.BEACON);

        // ── Bookshelf decorations on interior walls at SY+4..5 ───────────────
        //    North inner face (z = -8)
        for (int bx : new int[]{-4, -2, 0, 2, 4}) {
            block(es, bx, SY + 4, -8, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, bx, SY + 5, -8, BlockTypes.CHISELED_STONE_BRICKS);
        }
        //    South inner face (z = +8)
        for (int bx : new int[]{-4, -2, 0, 2, 4}) {
            block(es, bx, SY + 4, 8, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, bx, SY + 5, 8, BlockTypes.CHISELED_STONE_BRICKS);
        }
        //    West inner face (x = -8)
        for (int bz : new int[]{-4, -2, 0, 2, 4}) {
            block(es, -8, SY + 4, bz, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, -8, SY + 5, bz, BlockTypes.CHISELED_STONE_BRICKS);
        }
        //    East inner face (x = +8)
        for (int bz : new int[]{-4, -2, 0, 2, 4}) {
            block(es, 8, SY + 4, bz, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, 8, SY + 5, bz, BlockTypes.CHISELED_STONE_BRICKS);
        }

        // Accent sea-lanterns above bookshelf rows
        block(es,  0, SY + 6, -8, BlockTypes.SEA_LANTERN);
        block(es,  0, SY + 6,  8, BlockTypes.SEA_LANTERN);
        block(es, -8, SY + 6,  0, BlockTypes.SEA_LANTERN);
        block(es,  8, SY + 6,  0, BlockTypes.SEA_LANTERN);
    }

    /**
     * Places 4 mini-hub plazas on cardinal directions at radius ~32, each with
     * a 7×7 polished-deepslate pad, corner columns, and a central sea-lantern.
     * Also places 4 directional signpost obelisks at radius ~40 pointing toward
     * the nearest portal.
     */
    private void buildSecondaryHubs(EditSession es) {

        // ── 4 mini-hub positions ──────────────────────────────────────────────
        int[][] hubPositions = {{32, 0}, {-32, 0}, {0, 32}, {0, -32}};

        for (int[] hub : hubPositions) {
            int hx = hub[0], hz = hub[1];
            // Boundary check
            double ex = (double) hx / RX;
            double ez = (double) hz / RZ;
            if (ex * ex + ez * ez > 0.90) continue;

            // 7×7 pad
            fill(es, hx - 3, SY, hz - 3, hx + 3, SY, hz + 3,
                 BlockTypes.POLISHED_DEEPSLATE);
            // Checkerboard accent tiles
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if ((Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                        block(es, hx + dx, SY, hz + dz, BlockTypes.DEEPSLATE_TILES);
                    }
                }
            }

            // 4-tall chiseled-stone-bricks corner columns
            for (int[] corner : new int[][]{{-3,-3},{3,-3},{-3,3},{3,3}}) {
                column(es, hx + corner[0], hz + corner[1],
                       SY + 1, SY + 4, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, hx + corner[0], SY + 5, hz + corner[1],
                      BlockTypes.SEA_LANTERN);
            }

            // Central sea-lantern
            block(es, hx, SY + 1, hz, BlockTypes.SEA_LANTERN);
            // Quartz pillar central post
            column(es, hx, hz, SY + 1, SY + 3, BlockTypes.QUARTZ_PILLAR);
            block(es, hx, SY + 4, hz, BlockTypes.SEA_LANTERN);
        }

        // ── 4 directional signpost obelisks at radius ~40 ─────────────────────
        int[][] obeliskPositions = {{40, 0}, {-40, 0}, {0, 40}, {0, -40}};

        for (int[] ob : obeliskPositions) {
            int ox = ob[0], oz = ob[1];
            double ex = (double) ox / RX;
            double ez = (double) oz / RZ;
            if (ex * ex + ez * ez > 0.92) continue;

            // 3-tall chiseled column obelisk
            block(es, ox, SY,     oz, BlockTypes.DEEPSLATE_BRICKS);
            block(es, ox, SY + 1, oz, BlockTypes.CHISELED_DEEPSLATE);
            block(es, ox, SY + 2, oz, BlockTypes.CHISELED_DEEPSLATE);
            block(es, ox, SY + 3, oz, BlockTypes.POLISHED_DEEPSLATE);
            block(es, ox, SY + 4, oz, BlockTypes.LANTERN);
            // Small 3×3 base platform
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        block(es, ox + dx, SY, oz + dz, BlockTypes.STONE_BRICKS);
                    }
                }
            }
        }
    }

    // ============================================================================
    //  PORTAL HELPER
    // ============================================================================

    /**
     * Builds a 9-wide × 12-tall freestanding arch centred at (cx, SY, cz).
     *
     * <p>When {@code axisX} is {@code true} the arch spans along X (players
     * walk north/south through the opening); when {@code false} it spans along Z
     * (players walk east/west).
     *
     * <p>Structure per off-column (off = −4 to +4):
     * <ul>
     *   <li>off == ±4 → solid pillar alternating pillar/arch material every 2Y</li>
     *   <li>y == SY   → floor block</li>
     *   <li>y ≤ SY+6  → AIR (open walkway)</li>
     *   <li>y &gt; SY+6 → arch profile: archRow=y−(SY+7), halfOpen=4−archRow;
     *       if |off| &gt; halfOpen → solid, else AIR</li>
     * </ul>
     * Capstone + chain + lantern placed above the crown.
     */
    private void portalArch(EditSession es, int cx, int cz, boolean axisX,
                            com.sk89q.worldedit.world.block.BlockType pillar,
                            com.sk89q.worldedit.world.block.BlockType arch,
                            com.sk89q.worldedit.world.block.BlockType floor) {

        for (int off = -4; off <= 4; off++) {
            // Resolve absolute coordinates for this column
            int ax = axisX ? cx + off : cx;
            int az = axisX ? cz       : cz + off;

            for (int y = SY; y <= SY + 12; y++) {
                boolean isEdgeCol = (off == -4 || off == 4);

                if (isEdgeCol) {
                    // Full-height solid pillar, alternating material every 2 blocks
                    block(es, ax, y, az, y % 2 == 0 ? pillar : arch);

                } else if (y == SY) {
                    // Floor level
                    block(es, ax, y, az, floor);

                } else if (y <= SY + 6) {
                    // Open walkway interior
                    block(es, ax, y, az, BlockTypes.AIR);

                } else {
                    // Arch profile: rows close inward toward the crown
                    int archRow  = y - (SY + 7);   // 0 at first arch row, 5 at top
                    int halfOpen = 4 - archRow;     // how many cols are open each side
                    if (halfOpen < 0) halfOpen = 0;

                    if (Math.abs(off) > halfOpen) {
                        // Solid arch material
                        block(es, ax, y, az, y % 2 == 0 ? arch : pillar);
                    } else {
                        block(es, ax, y, az, BlockTypes.AIR);
                    }
                }
            }
        }

        // ── Capstone at SY+13, chain at SY+14, lantern at SY+15 ─────────────
        block(es, cx, SY + 13, cz, arch);
        block(es, cx, SY + 14, cz, BlockTypes.CHAIN);
        block(es, cx, SY + 15, cz, BlockTypes.LANTERN);
    }

    // ============================================================================
    //  PORTALS
    // ============================================================================

    /**
     * Crystal Portal — East (cx=55, cz=0).
     * End-stone / amethyst theme with a purpur arch and flanking obelisks.
     * Approach path runs west from x=48 to the pad.
     */
    private void buildCrystalPortal(EditSession es) {
        int cx = 55, cz = 0;

        // ── 9×9 pad with PURPUR_BLOCK border ──────────────────────────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.END_STONE_BRICKS);
        for (int x = cx - 4; x <= cx + 4; x++) {
            block(es, x, SY, cz - 4, BlockTypes.PURPUR_BLOCK);
            block(es, x, SY, cz + 4, BlockTypes.PURPUR_BLOCK);
        }
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx - 4, SY, z, BlockTypes.PURPUR_BLOCK);
            block(es, cx + 4, SY, z, BlockTypes.PURPUR_BLOCK);
        }

        // ── Arch (axisX=false → spans Z, player walks east through X) ─────────
        portalArch(es, cx, cz, false,
                   BlockTypes.PURPUR_PILLAR,
                   BlockTypes.END_STONE_BRICKS,
                   BlockTypes.END_STONE_BRICKS);

        // ── Amethyst accents at both pillar bases ─────────────────────────────
        for (int dz : new int[]{-4, 4}) {
            block(es, cx, SY + 1, cz + dz, BlockTypes.AMETHYST_BLOCK);
            block(es, cx, SY + 2, cz + dz, BlockTypes.AMETHYST_CLUSTER);
            block(es, cx, SY + 3, cz + dz, BlockTypes.BUDDING_AMETHYST);
        }

        // ── Flanking obelisks (PURPUR_BLOCK columns, 8-tall, SEA_LANTERN caps)
        //    placed at cx−9, cz±4
        for (int dz : new int[]{-4, 4}) {
            column(es, cx - 9, cz + dz, SY + 1, SY + 8, BlockTypes.PURPUR_PILLAR);
            block(es,  cx - 9, SY + 9,  cz + dz, BlockTypes.SEA_LANTERN);
        }

        // ── Glowstone header strip above arch ─────────────────────────────────
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx, SY + 16, z, BlockTypes.GLOWSTONE);
        }
        // Amethyst-block accent row above glowstone
        for (int z = cz - 2; z <= cz + 2; z++) {
            block(es, cx, SY + 17, z, BlockTypes.AMETHYST_BLOCK);
        }
        block(es, cx, SY + 18, cz, BlockTypes.AMETHYST_CLUSTER);

        // ── Approach path 3-wide (z=−1..1), x=48..54 ─────────────────────────
        for (int px = 48; px <= 54; px++) {
            for (int pz = -1; pz <= 1; pz++) {
                block(es, px, SY, pz, BlockTypes.POLISHED_DEEPSLATE);
            }
        }
        // Kerb stones
        for (int px = 48; px <= 54; px++) {
            block(es, px, SY, -2, BlockTypes.COBBLESTONE);
            block(es, px, SY,  2, BlockTypes.COBBLESTONE);
        }
    }

    /**
     * Sword Portal — West (cx=−55, cz=0).
     * Smooth-quartz / iron theme with QUARTZ_PILLAR arch and iron-block accents.
     * Approach path runs east from x=−48 toward the ring road.
     */
    private void buildSwordPortal(EditSession es) {
        int cx = -55, cz = 0;

        // ── 9×9 pad: SMOOTH_QUARTZ fill + QUARTZ_BRICKS border ───────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.SMOOTH_QUARTZ);
        for (int x = cx - 4; x <= cx + 4; x++) {
            block(es, x, SY, cz - 4, BlockTypes.QUARTZ_BRICKS);
            block(es, x, SY, cz + 4, BlockTypes.QUARTZ_BRICKS);
        }
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx - 4, SY, z, BlockTypes.QUARTZ_BRICKS);
            block(es, cx + 4, SY, z, BlockTypes.QUARTZ_BRICKS);
        }

        // ── Arch (axisX=false → spans Z) ──────────────────────────────────────
        portalArch(es, cx, cz, false,
                   BlockTypes.QUARTZ_PILLAR,
                   BlockTypes.QUARTZ_BRICKS,
                   BlockTypes.SMOOTH_QUARTZ);

        // ── IRON_BLOCK accents at pillar bases ────────────────────────────────
        for (int dz : new int[]{-4, 4}) {
            block(es, cx, SY + 1, cz + dz, BlockTypes.IRON_BLOCK);
            block(es, cx, SY + 2, cz + dz, BlockTypes.IRON_BARS);
            block(es, cx, SY + 3, cz + dz, BlockTypes.IRON_BLOCK);
        }

        // ── Flanking QUARTZ_PILLAR obelisks at cx+9, cz±4 ────────────────────
        for (int dz : new int[]{-4, 4}) {
            column(es, cx + 9, cz + dz, SY + 1, SY + 8, BlockTypes.QUARTZ_PILLAR);
            block(es,  cx + 9, SY + 9,  cz + dz, BlockTypes.SEA_LANTERN);
        }

        // ── Glowstone header ──────────────────────────────────────────────────
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx, SY + 16, z, BlockTypes.GLOWSTONE);
        }
        block(es, cx, SY + 17, cz, BlockTypes.IRON_BLOCK);
        block(es, cx, SY + 18, cz, BlockTypes.CHAIN);
        block(es, cx, SY + 19, cz, BlockTypes.LANTERN);

        // ── Approach path 3-wide (z=−1..1), x=−48..−54 ───────────────────────
        for (int px = -48; px >= -54; px--) {
            for (int pz = -1; pz <= 1; pz++) {
                block(es, px, SY, pz, BlockTypes.POLISHED_DEEPSLATE);
            }
        }
        for (int px = -48; px >= -54; px--) {
            block(es, px, SY, -2, BlockTypes.COBBLESTONE);
            block(es, px, SY,  2, BlockTypes.COBBLESTONE);
        }
    }

    /**
     * Mace Portal — South (cx=0, cz=65).
     * Nether-bricks / fire theme: RED_NETHER_BRICKS pad border, magma accents,
     * soul-sand / soul-soil scatter, flanking obelisks, glowstone header.
     * Approach path runs north from z=58 to the pad.
     */
    private void buildMacePortal(EditSession es) {
        int cx = 0, cz = 65;

        // ── 9×9 pad: NETHER_BRICKS + RED_NETHER_BRICKS border ─────────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.NETHER_BRICKS);
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx - 4, SY, z, BlockTypes.RED_NETHER_BRICKS);
            block(es, cx + 4, SY, z, BlockTypes.RED_NETHER_BRICKS);
        }
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 4, BlockTypes.RED_NETHER_BRICKS);
            block(es, x, SY, cz + 4, BlockTypes.RED_NETHER_BRICKS);
        }

        // ── Arch (axisX=true → spans X, player walks south through Z) ─────────
        portalArch(es, cx, cz, true,
                   BlockTypes.RED_NETHER_BRICKS,
                   BlockTypes.NETHER_BRICKS,
                   BlockTypes.NETHER_BRICKS);

        // ── MAGMA_BLOCK accents at pillar bases ───────────────────────────────
        for (int dx : new int[]{-4, 4}) {
            block(es, cx + dx, SY + 1, cz, BlockTypes.MAGMA_BLOCK);
            block(es, cx + dx, SY + 2, cz, BlockTypes.NETHER_BRICK_WALL);
            block(es, cx + dx, SY + 3, cz, BlockTypes.MAGMA_BLOCK);
        }

        // ── Soul-sand / soul-soil scatter near base ───────────────────────────
        block(es, cx - 6, SY, cz + 2, BlockTypes.SOUL_SAND);
        block(es, cx + 6, SY, cz + 2, BlockTypes.SOUL_SAND);
        block(es, cx - 5, SY, cz - 2, BlockTypes.SOUL_SOIL);
        block(es, cx + 5, SY, cz - 2, BlockTypes.SOUL_SOIL);
        block(es, cx - 7, SY, cz,     BlockTypes.SOUL_SAND);
        block(es, cx + 7, SY, cz,     BlockTypes.SOUL_SOIL);

        // ── Flanking RED_NETHER_BRICKS obelisks ──────────────────────────────
        for (int dx : new int[]{-4, 4}) {
            column(es, cx + dx, cz + 9, SY + 1, SY + 8, BlockTypes.RED_NETHER_BRICKS);
            block(es,  cx + dx, SY + 9, cz + 9, BlockTypes.GLOWSTONE);
        }

        // ── Glowstone header ──────────────────────────────────────────────────
        for (int x = cx - 4; x <= cx + 4; x++) {
            block(es, x, SY + 16, cz, BlockTypes.GLOWSTONE);
        }
        block(es, cx, SY + 17, cz, BlockTypes.MAGMA_BLOCK);
        block(es, cx, SY + 18, cz, BlockTypes.NETHER_BRICKS);

        // ── Approach path 3-wide (x=−1..1), z=58..64 ─────────────────────────
        for (int pz = 58; pz <= 64; pz++) {
            for (int px = -1; px <= 1; px++) {
                block(es, px, SY, pz, BlockTypes.POLISHED_DEEPSLATE);
            }
        }
        for (int pz = 58; pz <= 64; pz++) {
            block(es, -2, SY, pz, BlockTypes.COBBLESTONE);
            block(es,  2, SY, pz, BlockTypes.COBBLESTONE);
        }
    }

    /**
     * Bow Portal — North (cx=0, cz=−65).
     * Mossy-stone / dark-oak theme: mossy pad, dark-oak log arch, leaf canopy
     * overhead, flanking dark-oak obelisks, glowstone header.
     * Approach path runs south from z=−58 toward the ring road.
     */
    private void buildBowPortal(EditSession es) {
        int cx = 0, cz = -65;

        // ── 9×9 pad: MOSSY_STONE_BRICKS + MOSSY_COBBLESTONE border ──────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.MOSSY_STONE_BRICKS);
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx - 4, SY, z, BlockTypes.MOSSY_COBBLESTONE);
            block(es, cx + 4, SY, z, BlockTypes.MOSSY_COBBLESTONE);
        }
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 4, BlockTypes.MOSSY_COBBLESTONE);
            block(es, x, SY, cz + 4, BlockTypes.MOSSY_COBBLESTONE);
        }

        // ── Arch (axisX=true → spans X, player walks north through Z) ─────────
        portalArch(es, cx, cz, true,
                   BlockTypes.DARK_OAK_LOG,
                   BlockTypes.MOSSY_COBBLESTONE,
                   BlockTypes.MOSSY_STONE_BRICKS);

        // ── OAK_LOG accents at pillar bases ───────────────────────────────────
        for (int dx : new int[]{-4, 4}) {
            block(es, cx + dx, SY + 1, cz, BlockTypes.OAK_LOG);
            block(es, cx + dx, SY + 2, cz, BlockTypes.OAK_PLANKS);
            block(es, cx + dx, SY + 3, cz, BlockTypes.OAK_LOG);
        }

        // ── Flanking DARK_OAK_LOG obelisks ────────────────────────────────────
        for (int dx : new int[]{-4, 4}) {
            column(es, cx + dx, cz - 9, SY + 1, SY + 8, BlockTypes.DARK_OAK_LOG);
            block(es,  cx + dx, SY + 9, cz - 9, BlockTypes.LANTERN);
        }

        // ── Dark-oak leaf canopy (5×5 at SY+16, 3×3 at SY+17) ───────────────
        fill(es, cx - 2, SY + 16, cz - 2, cx + 2, SY + 16, cz + 2, BlockTypes.DARK_OAK_LEAVES);
        fill(es, cx - 1, SY + 17, cz - 1, cx + 1, SY + 17, cz + 1, BlockTypes.DARK_OAK_LEAVES);
        block(es, cx, SY + 18, cz, BlockTypes.DARK_OAK_LEAVES);

        // ── Glowstone header ──────────────────────────────────────────────────
        for (int x = cx - 4; x <= cx + 4; x++) {
            block(es, x, SY + 16, cz, BlockTypes.GLOWSTONE);
        }
        block(es, cx, SY + 17, cz, BlockTypes.DARK_OAK_LOG);

        // ── Approach path 3-wide (x=−1..1), z=−58..−64 ──────────────────────
        for (int pz = -58; pz >= -64; pz--) {
            for (int px = -1; px <= 1; px++) {
                block(es, px, SY, pz, BlockTypes.POLISHED_DEEPSLATE);
            }
        }
        for (int pz = -58; pz >= -64; pz--) {
            block(es, -2, SY, pz, BlockTypes.COBBLESTONE);
            block(es,  2, SY, pz, BlockTypes.COBBLESTONE);
        }
    }

    /**
     * Totem Portal — NE (cx=38, cz=−45).
     * Jungle / nature theme: jungle-planks pad, jungle-log arch, moss-block
     * key blocks, triple-layer leaf canopy, flanking jungle-log obelisks,
     * and a 7-step diagonal approach path toward the SW.
     */
    private void buildTotemPortal(EditSession es) {
        int cx = 38, cz = -45;

        // ── 9×9 pad: JUNGLE_PLANKS + JUNGLE_LOG border ───────────────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.JUNGLE_PLANKS);
        for (int x = cx - 4; x <= cx + 4; x++) {
            block(es, x, SY, cz - 4, BlockTypes.JUNGLE_LOG);
            block(es, x, SY, cz + 4, BlockTypes.JUNGLE_LOG);
        }
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx - 4, SY, z, BlockTypes.JUNGLE_LOG);
            block(es, cx + 4, SY, z, BlockTypes.JUNGLE_LOG);
        }

        // ── Arch (axisX=true → spans X) ───────────────────────────────────────
        portalArch(es, cx, cz, true,
                   BlockTypes.JUNGLE_LOG,
                   BlockTypes.MOSS_BLOCK,
                   BlockTypes.JUNGLE_PLANKS);

        // ── 3-layer jungle-leaves canopy over arch ────────────────────────────
        fill(es, cx - 4, SY + 16, cz - 4, cx + 4, SY + 16, cz + 4, BlockTypes.JUNGLE_LEAVES);
        fill(es, cx - 3, SY + 17, cz - 3, cx + 3, SY + 17, cz + 3, BlockTypes.JUNGLE_LEAVES);
        fill(es, cx - 2, SY + 18, cz - 2, cx + 2, SY + 18, cz + 2, BlockTypes.JUNGLE_LEAVES);

        // ── Flanking JUNGLE_LOG obelisks ──────────────────────────────────────
        for (int dx : new int[]{-4, 4}) {
            column(es, cx + dx, cz - 9, SY + 1, SY + 8, BlockTypes.JUNGLE_LOG);
            block(es,  cx + dx, SY + 9, cz - 9, BlockTypes.LANTERN);
        }

        // ── Diagonal approach path NE→SW, 7 steps, 3-wide ────────────────────
        for (int step = 1; step <= 7; step++) {
            int px = cx - step;
            int pz = cz + step;
            // 3-wide perpendicular to diagonal (offset ±1 on both axes)
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
            // Occasional mossy accent
            if (step % 3 == 0) {
                block(es, px, SY, pz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }
    }

    /**
     * Axe Portal — NW (cx=−38, cz=−45).
     * Spruce / deepslate theme: spruce-planks pad, spruce-log arch,
     * cobbled-deepslate accents, moss-carpet scatter, flanking spruce-log
     * obelisks with lantern caps, 7-step diagonal approach from SE.
     */
    private void buildAxePortal(EditSession es) {
        int cx = -38, cz = -45;

        // ── 9×9 pad: SPRUCE_PLANKS + SPRUCE_LOG border ───────────────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.SPRUCE_PLANKS);
        for (int x = cx - 4; x <= cx + 4; x++) {
            block(es, x, SY, cz - 4, BlockTypes.SPRUCE_LOG);
            block(es, x, SY, cz + 4, BlockTypes.SPRUCE_LOG);
        }
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx - 4, SY, z, BlockTypes.SPRUCE_LOG);
            block(es, cx + 4, SY, z, BlockTypes.SPRUCE_LOG);
        }

        // ── Arch (axisX=true → spans X) ───────────────────────────────────────
        portalArch(es, cx, cz, true,
                   BlockTypes.SPRUCE_LOG,
                   BlockTypes.COBBLED_DEEPSLATE,
                   BlockTypes.SPRUCE_PLANKS);

        // ── MOSSY_COBBLESTONE accents at both pillar bases ────────────────────
        for (int dx : new int[]{-4, 4}) {
            block(es, cx + dx, SY + 1, cz, BlockTypes.MOSSY_COBBLESTONE);
            block(es, cx + dx, SY + 2, cz, BlockTypes.MOSSY_COBBLESTONE);
            block(es, cx + dx, SY + 3, cz, BlockTypes.COBBLED_DEEPSLATE);
        }

        // ── MOSS_CARPET scattered around base ────────────────────────────────
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 7) continue;
                if ((dx + dz) % 3 == 0) {
                    block(es, cx + dx, SY + 1, cz + dz, BlockTypes.MOSS_CARPET);
                }
            }
        }

        // ── Flanking SPRUCE_LOG obelisks with LANTERN caps ───────────────────
        for (int dx : new int[]{-4, 4}) {
            column(es, cx + dx, cz - 9, SY + 1, SY + 8, BlockTypes.SPRUCE_LOG);
            block(es,  cx + dx, SY + 9, cz - 9, BlockTypes.LANTERN);
        }

        // ── Diagonal approach path SE→NW (from cx+8, cz+8 direction), 7 steps, 3-wide ─
        for (int step = 1; step <= 7; step++) {
            int px = cx + step;
            int pz = cz + step;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
            if (step % 3 == 0) {
                block(es, px, SY, pz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }
    }

    /**
     * Trident Portal — SE (cx=38, cz=55).
     * Prismarine theme: prismarine-bricks pad, dark-prismarine border,
     * sea-lantern accents at pillar bases, flanking prismarine obelisks
     * with sea-lantern caps, glowstone header, prismarine accent row.
     * Approach path runs west from x=31 to x=37 (z=54..56).
     */
    private void buildTridentPortal(EditSession es) {
        int cx = 38, cz = 55;

        // ── 9×9 pad: PRISMARINE_BRICKS + DARK_PRISMARINE border ──────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.PRISMARINE_BRICKS);
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx - 4, SY, z, BlockTypes.DARK_PRISMARINE);
            block(es, cx + 4, SY, z, BlockTypes.DARK_PRISMARINE);
        }
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 4, BlockTypes.DARK_PRISMARINE);
            block(es, x, SY, cz + 4, BlockTypes.DARK_PRISMARINE);
        }

        // ── Arch (axisX=false → spans Z, player walks east) ──────────────────
        portalArch(es, cx, cz, false,
                   BlockTypes.PRISMARINE_BRICKS,
                   BlockTypes.DARK_PRISMARINE,
                   BlockTypes.PRISMARINE_BRICKS);

        // ── SEA_LANTERN accents at pillar bases (instead of standard) ─────────
        for (int dz : new int[]{-4, 4}) {
            block(es, cx, SY + 1, cz + dz, BlockTypes.SEA_LANTERN);
            block(es, cx, SY + 2, cz + dz, BlockTypes.PRISMARINE_BRICKS);
            block(es, cx, SY + 3, cz + dz, BlockTypes.SEA_LANTERN);
        }

        // ── Flanking PRISMARINE obelisks with SEA_LANTERN caps ───────────────
        for (int dz : new int[]{-4, 4}) {
            column(es, cx - 9, cz + dz, SY + 1, SY + 8, BlockTypes.PRISMARINE);
            block(es,  cx - 9, SY + 9,  cz + dz, BlockTypes.SEA_LANTERN);
        }

        // ── Glowstone header + PRISMARINE accent ──────────────────────────────
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx, SY + 16, z, BlockTypes.GLOWSTONE);
        }
        for (int z = cz - 3; z <= cz + 3; z++) {
            block(es, cx, SY + 17, z, BlockTypes.PRISMARINE);
        }
        block(es, cx, SY + 18, cz, BlockTypes.SEA_LANTERN);

        // ── Approach path (z=54..56, x=31..37) ───────────────────────────────
        for (int px = 31; px <= 37; px++) {
            for (int pz = cz - 1; pz <= cz + 1; pz++) {
                block(es, px, SY, pz, BlockTypes.PRISMARINE_BRICKS);
            }
        }
        for (int px = 31; px <= 37; px++) {
            block(es, px, SY, cz - 2, BlockTypes.DARK_PRISMARINE);
            block(es, px, SY, cz + 2, BlockTypes.DARK_PRISMARINE);
        }
    }

    /**
     * Shield Portal — SW (cx=−38, cz=55).
     * Oxidized-copper theme: CUT_COPPER pad, OXIDIZED_CUT_COPPER border,
     * WEATHERED_CUT_COPPER accents, COPPER_BLOCK obelisks with LANTERN caps.
     * Approach path from x=−31 to x=−37.
     */
    private void buildShieldPortal(EditSession es) {
        int cx = -38, cz = 55;

        // ── 9×9 pad: CUT_COPPER + OXIDIZED_CUT_COPPER border ─────────────────
        fill(es, cx - 4, SY, cz - 4, cx + 4, SY, cz + 4, BlockTypes.CUT_COPPER);
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx - 4, SY, z, BlockTypes.OXIDIZED_CUT_COPPER);
            block(es, cx + 4, SY, z, BlockTypes.OXIDIZED_CUT_COPPER);
        }
        for (int x = cx - 3; x <= cx + 3; x++) {
            block(es, x, SY, cz - 4, BlockTypes.OXIDIZED_CUT_COPPER);
            block(es, x, SY, cz + 4, BlockTypes.OXIDIZED_CUT_COPPER);
        }

        // ── Arch (axisX=false → spans Z, player walks west) ──────────────────
        portalArch(es, cx, cz, false,
                   BlockTypes.COPPER_BLOCK,
                   BlockTypes.OXIDIZED_COPPER,
                   BlockTypes.CUT_COPPER);

        // ── WEATHERED_CUT_COPPER accents at pillar bases ──────────────────────
        for (int dz : new int[]{-4, 4}) {
            block(es, cx, SY + 1, cz + dz, BlockTypes.WEATHERED_CUT_COPPER);
            block(es, cx, SY + 2, cz + dz, BlockTypes.COPPER_BLOCK);
            block(es, cx, SY + 3, cz + dz, BlockTypes.WEATHERED_CUT_COPPER);
        }

        // ── Flanking COPPER_BLOCK obelisks with LANTERN caps ──────────────────
        for (int dz : new int[]{-4, 4}) {
            column(es, cx + 9, cz + dz, SY + 1, SY + 8, BlockTypes.COPPER_BLOCK);
            block(es,  cx + 9, SY + 9,  cz + dz, BlockTypes.LANTERN);
        }

        // ── Accent details above arch ─────────────────────────────────────────
        for (int z = cz - 4; z <= cz + 4; z++) {
            block(es, cx, SY + 16, z, BlockTypes.OXIDIZED_COPPER);
        }
        for (int z = cz - 2; z <= cz + 2; z++) {
            block(es, cx, SY + 17, z, BlockTypes.WEATHERED_COPPER);
        }
        block(es, cx, SY + 18, cz, BlockTypes.COPPER_BLOCK);
        block(es, cx, SY + 19, cz, BlockTypes.CHAIN);
        block(es, cx, SY + 20, cz, BlockTypes.LANTERN);

        // ── Approach path x=−31..−37, 3-wide z=cz−1..cz+1 ───────────────────
        for (int px = -31; px >= -37; px--) {
            for (int pz = cz - 1; pz <= cz + 1; pz++) {
                block(es, px, SY, pz, BlockTypes.CUT_COPPER);
            }
        }
        for (int px = -31; px >= -37; px--) {
            block(es, px, SY, cz - 2, BlockTypes.OXIDIZED_CUT_COPPER);
            block(es, px, SY, cz + 2, BlockTypes.OXIDIZED_CUT_COPPER);
        }
    }

    // ============================================================================
    //  CONNECTIVITY
    // ============================================================================

    /**
     * Places 5-wide stone-brick staircases descending 4 steps outward from the
     * central plaza edge on each of the 4 cardinal sides.  Each staircase has
     * oak-fence railings, 2-tall, on both sides.
     */
    private void buildGrandStaircase(EditSession es) {

        // ── East staircase: base at x=21, descends eastward ──────────────────
        for (int step = 0; step < 4; step++) {
            int bx = 21 + step;
            for (int z = -2; z <= 2; z++) {
                block(es, bx, SY - step, z, BlockTypes.STONE_BRICKS);
            }
            // Oak fence railings on z=±2 edges
            column(es, bx, -3, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
            column(es, bx,  3, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
        }

        // ── West staircase: base at x=−21, descends westward ─────────────────
        for (int step = 0; step < 4; step++) {
            int bx = -21 - step;
            for (int z = -2; z <= 2; z++) {
                block(es, bx, SY - step, z, BlockTypes.STONE_BRICKS);
            }
            column(es, bx, -3, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
            column(es, bx,  3, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
        }

        // ── South staircase: base at z=21, descends southward ────────────────
        for (int step = 0; step < 4; step++) {
            int bz = 21 + step;
            for (int x = -2; x <= 2; x++) {
                block(es, x, SY - step, bz, BlockTypes.STONE_BRICKS);
            }
            column(es, -3, bz, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
            column(es,  3, bz, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
        }

        // ── North staircase: base at z=−21, descends northward ───────────────
        for (int step = 0; step < 4; step++) {
            int bz = -21 - step;
            for (int x = -2; x <= 2; x++) {
                block(es, x, SY - step, bz, BlockTypes.STONE_BRICKS);
            }
            column(es, -3, bz, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
            column(es,  3, bz, SY - step + 1, SY - step + 2, BlockTypes.OAK_FENCE);
        }
    }

    /**
     * Builds a 3-wide slightly elliptical stone-brick ring road at approximately
     * radius 28 (east-west) × 34 (north-south).  Includes cobblestone kerbs on
     * path edges and mossy accent tiles every 15°.
     */
    private void buildRingRoad(EditSession es) {
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            // Elliptical ring: rx≈28, rz≈34
            int  ringX = (int) Math.round(28.0 * Math.cos(rad));
            int  ringZ = (int) Math.round(34.0 * Math.sin(rad));

            // Ensure within island
            double ex = (double) ringX / RX;
            double ez = (double) ringZ / RZ;
            if (ex * ex + ez * ez > 0.92) continue;

            // 3-wide path segment: perpendicular offset directions
            double perpCos = -Math.sin(rad); // perpendicular to radial
            double perpSin =  Math.cos(rad);

            for (int lane = -1; lane <= 1; lane++) {
                int lx = ringX + (int) Math.round(lane * perpCos);
                int lz = ringZ + (int) Math.round(lane * perpSin);
                boolean isMossy = (angle % 15 == 0 && lane == 0);
                block(es, lx, SY, lz,
                      isMossy ? BlockTypes.MOSSY_STONE_BRICKS : BlockTypes.STONE_BRICKS);
            }

            // Cobblestone kerb at path edges (lane ±2)
            for (int kerb : new int[]{-2, 2}) {
                int kx = ringX + (int) Math.round(kerb * perpCos);
                int kz = ringZ + (int) Math.round(kerb * perpSin);
                double kex = (double) kx / RX;
                double kez = (double) kz / RZ;
                if (kex * kex + kez * kez <= 0.94) {
                    block(es, kx, SY, kz, BlockTypes.COBBLESTONE);
                }
            }
        }
    }

    /**
     * Places 3-wide STONE_BRICKS radial paths from the ring road out to each of
     * the 8 portal approach areas, plus 3-wide paths from the central plaza to
     * the ring road on all 4 cardinal directions.  Cobblestone kerbs flank each
     * path and mossy accent tiles appear every 6 blocks.
     */
    private void buildPathways(EditSession es) {

        // ── Cardinal paths: plaza (r≈21) to ring road (r≈28) ─────────────────

        // East path (x=21..28, z=−1..1)
        for (int x = 22; x <= 28; x++) {
            for (int z = -1; z <= 1; z++) {
                block(es, x, SY, z,
                      (x % 6 == 0 && z == 0) ? BlockTypes.MOSSY_STONE_BRICKS
                                              : BlockTypes.STONE_BRICKS);
            }
            block(es, x, SY, -2, BlockTypes.COBBLESTONE);
            block(es, x, SY,  2, BlockTypes.COBBLESTONE);
        }

        // West path (x=−22..−28, z=−1..1)
        for (int x = -22; x >= -28; x--) {
            for (int z = -1; z <= 1; z++) {
                block(es, x, SY, z,
                      (Math.abs(x) % 6 == 0 && z == 0) ? BlockTypes.MOSSY_STONE_BRICKS
                                                        : BlockTypes.STONE_BRICKS);
            }
            block(es, x, SY, -2, BlockTypes.COBBLESTONE);
            block(es, x, SY,  2, BlockTypes.COBBLESTONE);
        }

        // South path (z=22..34, x=−1..1)
        for (int z = 22; z <= 34; z++) {
            for (int x = -1; x <= 1; x++) {
                block(es, x, SY, z,
                      (z % 6 == 0 && x == 0) ? BlockTypes.MOSSY_STONE_BRICKS
                                             : BlockTypes.STONE_BRICKS);
            }
            block(es, -2, SY, z, BlockTypes.COBBLESTONE);
            block(es,  2, SY, z, BlockTypes.COBBLESTONE);
        }

        // North path (z=−22..−34, x=−1..1)
        for (int z = -22; z >= -34; z--) {
            for (int x = -1; x <= 1; x++) {
                block(es, x, SY, z,
                      (Math.abs(z) % 6 == 0 && x == 0) ? BlockTypes.MOSSY_STONE_BRICKS
                                                        : BlockTypes.STONE_BRICKS);
            }
            block(es, -2, SY, z, BlockTypes.COBBLESTONE);
            block(es,  2, SY, z, BlockTypes.COBBLESTONE);
        }

        // ── Radial paths from ring road to each portal approach ───────────────

        // Crystal (E): x=29..47, z=−1..1
        for (int x = 29; x <= 47; x++) {
            for (int z = -1; z <= 1; z++) {
                block(es, x, SY, z, (x % 6 == 0 && z == 0) ? BlockTypes.MOSSY_STONE_BRICKS
                                                             : BlockTypes.STONE_BRICKS);
            }
            block(es, x, SY, -2, BlockTypes.COBBLESTONE);
            block(es, x, SY,  2, BlockTypes.COBBLESTONE);
        }

        // Sword (W): x=−29..−47, z=−1..1
        for (int x = -29; x >= -47; x--) {
            for (int z = -1; z <= 1; z++) {
                block(es, x, SY, z, (Math.abs(x) % 6 == 0 && z == 0)
                                    ? BlockTypes.MOSSY_STONE_BRICKS : BlockTypes.STONE_BRICKS);
            }
            block(es, x, SY, -2, BlockTypes.COBBLESTONE);
            block(es, x, SY,  2, BlockTypes.COBBLESTONE);
        }

        // Mace (S): z=35..57, x=−1..1
        for (int z = 35; z <= 57; z++) {
            for (int x = -1; x <= 1; x++) {
                block(es, x, SY, z, (z % 6 == 0 && x == 0) ? BlockTypes.MOSSY_STONE_BRICKS
                                                             : BlockTypes.STONE_BRICKS);
            }
            block(es, -2, SY, z, BlockTypes.COBBLESTONE);
            block(es,  2, SY, z, BlockTypes.COBBLESTONE);
        }

        // Bow (N): z=−35..−57, x=−1..1
        for (int z = -35; z >= -57; z--) {
            for (int x = -1; x <= 1; x++) {
                block(es, x, SY, z, (Math.abs(z) % 6 == 0 && x == 0)
                                    ? BlockTypes.MOSSY_STONE_BRICKS : BlockTypes.STONE_BRICKS);
            }
            block(es, -2, SY, z, BlockTypes.COBBLESTONE);
            block(es,  2, SY, z, BlockTypes.COBBLESTONE);
        }

        // Totem (NE diagonal): 7-step path from ring to portal approach
        // Rings at ~28, portal at (38, -45); step diagonally
        for (int step = 0; step <= 9; step++) {
            int px = 29 + step;
            int pz = -35 - step;
            double ex = (double) px / RX;
            double ez = (double) pz / RZ;
            if (ex * ex + ez * ez > 0.96) continue;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
            if (step % 6 == 0) {
                block(es, px, SY, pz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }

        // Axe (NW diagonal): ring to portal
        for (int step = 0; step <= 9; step++) {
            int px = -29 - step;
            int pz = -35 - step;
            double ex = (double) px / RX;
            double ez = (double) pz / RZ;
            if (ex * ex + ez * ez > 0.96) continue;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
            if (step % 6 == 0) {
                block(es, px, SY, pz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }

        // Trident (SE diagonal): ring to portal
        for (int step = 0; step <= 9; step++) {
            int px = 29 + step;
            int pz = 35 + step;
            double ex = (double) px / RX;
            double ez = (double) pz / RZ;
            if (ex * ex + ez * ez > 0.96) continue;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
            if (step % 6 == 0) {
                block(es, px, SY, pz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }

        // Shield (SW diagonal): ring to portal
        for (int step = 0; step <= 9; step++) {
            int px = -29 - step;
            int pz = 35 + step;
            double ex = (double) px / RX;
            double ez = (double) pz / RZ;
            if (ex * ex + ez * ez > 0.96) continue;
            for (int d = -1; d <= 1; d++) {
                block(es, px + d, SY, pz,     BlockTypes.STONE_BRICKS);
                block(es, px,     SY, pz + d, BlockTypes.STONE_BRICKS);
            }
            if (step % 6 == 0) {
                block(es, px, SY, pz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }
    }

    /**
     * Places 5 wooden bridges across gaps and water features on the island,
     * each approximately 10–15 blocks long, 3-wide SPRUCE_PLANKS deck with
     * 2-tall OAK_FENCE railings, OAK_LOG support columns every 4 blocks below,
     * and LANTERN decorations at each bridge end.
     *
     * <ul>
     *   <li>Bridge 1 – East approach, x=26, spanning z=−8..8</li>
     *   <li>Bridge 2 – North approach, z=−26, spanning x=−6..6</li>
     *   <li>Bridge 3 – West approach, x=−26, spanning z=−6..6</li>
     *   <li>Bridge 4 – South approach, z=26, spanning x=−6..6</li>
     *   <li>Bridge 5 – NE diagonal bridge over a small scenic gap</li>
     * </ul>
     */
    private void buildBridges(EditSession es) {

        // ── Bridge 1: East approach at x=26, spanning z=−8..8 (17 blocks long)
        {
            int bx = 26;
            // 3-wide deck: z=−8..8, x=bx−1..bx+1
            for (int z = -8; z <= 8; z++) {
                for (int dx = -1; dx <= 1; dx++) {
                    block(es, bx + dx, SY, z, BlockTypes.SPRUCE_PLANKS);
                }
            }
            // OAK_FENCE railings on both long sides (x=bx−1 and x=bx+1), 2-tall
            for (int z = -8; z <= 8; z++) {
                block(es, bx - 1, SY + 1, z, BlockTypes.OAK_FENCE);
                block(es, bx - 1, SY + 2, z, BlockTypes.OAK_FENCE);
                block(es, bx + 1, SY + 1, z, BlockTypes.OAK_FENCE);
                block(es, bx + 1, SY + 2, z, BlockTypes.OAK_FENCE);
            }
            // OAK_LOG support columns below deck every 4 blocks along z
            for (int z = -8; z <= 8; z += 4) {
                block(es, bx, SY - 1, z, BlockTypes.OAK_LOG);
                block(es, bx, SY - 2, z, BlockTypes.OAK_LOG);
                block(es, bx, SY - 3, z, BlockTypes.OAK_LOG);
            }
            // Lanterns at both ends of the bridge
            block(es, bx - 1, SY + 3, -8, BlockTypes.LANTERN);
            block(es, bx + 1, SY + 3, -8, BlockTypes.LANTERN);
            block(es, bx - 1, SY + 3,  8, BlockTypes.LANTERN);
            block(es, bx + 1, SY + 3,  8, BlockTypes.LANTERN);
            // Extra cobblestone kerb blocks at bridge entry/exit
            for (int dx = -1; dx <= 1; dx++) {
                block(es, bx + dx, SY,  9, BlockTypes.COBBLESTONE);
                block(es, bx + dx, SY, -9, BlockTypes.COBBLESTONE);
            }
        }

        // ── Bridge 2: North approach at z=−26, spanning x=−6..6 (13 blocks) ──
        {
            int bz = -26;
            for (int x = -6; x <= 6; x++) {
                for (int dz = -1; dz <= 1; dz++) {
                    block(es, x, SY, bz + dz, BlockTypes.SPRUCE_PLANKS);
                }
            }
            // Railings along north and south long sides
            for (int x = -6; x <= 6; x++) {
                block(es, x, SY + 1, bz - 1, BlockTypes.OAK_FENCE);
                block(es, x, SY + 2, bz - 1, BlockTypes.OAK_FENCE);
                block(es, x, SY + 1, bz + 1, BlockTypes.OAK_FENCE);
                block(es, x, SY + 2, bz + 1, BlockTypes.OAK_FENCE);
            }
            // Support columns below every 4 blocks along x
            for (int x = -6; x <= 6; x += 4) {
                block(es, x, SY - 1, bz, BlockTypes.OAK_LOG);
                block(es, x, SY - 2, bz, BlockTypes.OAK_LOG);
                block(es, x, SY - 3, bz, BlockTypes.OAK_LOG);
            }
            // Lanterns at both short ends
            block(es, -6, SY + 3, bz - 1, BlockTypes.LANTERN);
            block(es, -6, SY + 3, bz + 1, BlockTypes.LANTERN);
            block(es,  6, SY + 3, bz - 1, BlockTypes.LANTERN);
            block(es,  6, SY + 3, bz + 1, BlockTypes.LANTERN);
            // Entry kerbs
            for (int dz = -1; dz <= 1; dz++) {
                block(es, -7, SY, bz + dz, BlockTypes.COBBLESTONE);
                block(es,  7, SY, bz + dz, BlockTypes.COBBLESTONE);
            }
        }

        // ── Bridge 3: West approach at x=−26, spanning z=−6..6 (13 blocks) ───
        {
            int bx = -26;
            for (int z = -6; z <= 6; z++) {
                for (int dx = -1; dx <= 1; dx++) {
                    block(es, bx + dx, SY, z, BlockTypes.SPRUCE_PLANKS);
                }
            }
            // Railings on x=bx−1 and x=bx+1
            for (int z = -6; z <= 6; z++) {
                block(es, bx - 1, SY + 1, z, BlockTypes.OAK_FENCE);
                block(es, bx - 1, SY + 2, z, BlockTypes.OAK_FENCE);
                block(es, bx + 1, SY + 1, z, BlockTypes.OAK_FENCE);
                block(es, bx + 1, SY + 2, z, BlockTypes.OAK_FENCE);
            }
            // Support columns every 4 along z
            for (int z = -6; z <= 6; z += 4) {
                block(es, bx, SY - 1, z, BlockTypes.OAK_LOG);
                block(es, bx, SY - 2, z, BlockTypes.OAK_LOG);
                block(es, bx, SY - 3, z, BlockTypes.OAK_LOG);
            }
            // Lanterns at short ends
            block(es, bx - 1, SY + 3, -6, BlockTypes.LANTERN);
            block(es, bx + 1, SY + 3, -6, BlockTypes.LANTERN);
            block(es, bx - 1, SY + 3,  6, BlockTypes.LANTERN);
            block(es, bx + 1, SY + 3,  6, BlockTypes.LANTERN);
            // Entry kerbs
            for (int dx = -1; dx <= 1; dx++) {
                block(es, bx + dx, SY, -7, BlockTypes.COBBLESTONE);
                block(es, bx + dx, SY,  7, BlockTypes.COBBLESTONE);
            }
        }

        // ── Bridge 4: South approach at z=26, spanning x=−6..6 (13 blocks) ───
        {
            int bz = 26;
            for (int x = -6; x <= 6; x++) {
                for (int dz = -1; dz <= 1; dz++) {
                    block(es, x, SY, bz + dz, BlockTypes.SPRUCE_PLANKS);
                }
            }
            // Railings along both long sides
            for (int x = -6; x <= 6; x++) {
                block(es, x, SY + 1, bz - 1, BlockTypes.OAK_FENCE);
                block(es, x, SY + 2, bz - 1, BlockTypes.OAK_FENCE);
                block(es, x, SY + 1, bz + 1, BlockTypes.OAK_FENCE);
                block(es, x, SY + 2, bz + 1, BlockTypes.OAK_FENCE);
            }
            // Support columns every 4 along x
            for (int x = -6; x <= 6; x += 4) {
                block(es, x, SY - 1, bz, BlockTypes.OAK_LOG);
                block(es, x, SY - 2, bz, BlockTypes.OAK_LOG);
                block(es, x, SY - 3, bz, BlockTypes.OAK_LOG);
            }
            // Lanterns at short ends
            block(es, -6, SY + 3, bz - 1, BlockTypes.LANTERN);
            block(es, -6, SY + 3, bz + 1, BlockTypes.LANTERN);
            block(es,  6, SY + 3, bz - 1, BlockTypes.LANTERN);
            block(es,  6, SY + 3, bz + 1, BlockTypes.LANTERN);
            // Entry kerbs
            for (int dz = -1; dz <= 1; dz++) {
                block(es, -7, SY, bz + dz, BlockTypes.COBBLESTONE);
                block(es,  7, SY, bz + dz, BlockTypes.COBBLESTONE);
            }
        }

        // ── Bridge 5: NE diagonal bridge over a small scenic gap ─────────────
        //    Runs from approximately (26, SY, −20) to (36, SY, −30),
        //    10 diagonal steps, 3-wide SPRUCE_PLANKS, OAK_FENCE railings,
        //    OAK_LOG supports mid-span, LANTERN ends.
        {
            int startX = 26, startZ = -20;
            int steps  = 10;

            for (int step = 0; step <= steps; step++) {
                int dx = startX + step;
                int dz = startZ - step;

                // Check within island
                double ex = (double) dx / RX;
                double ez = (double) dz / RZ;
                if (ex * ex + ez * ez > 0.94) continue;

                // 3-wide perpendicular to the NE diagonal (offset on the SE–NW axis)
                // The diagonal is (1,−1), perpendicular is (1,1)/√2
                block(es, dx,     SY, dz,     BlockTypes.SPRUCE_PLANKS);
                block(es, dx + 1, SY, dz + 1, BlockTypes.SPRUCE_PLANKS);
                block(es, dx - 1, SY, dz - 1, BlockTypes.SPRUCE_PLANKS);

                // Railings on outer edges
                block(es, dx + 1, SY + 1, dz + 1, BlockTypes.OAK_FENCE);
                block(es, dx + 1, SY + 2, dz + 1, BlockTypes.OAK_FENCE);
                block(es, dx - 1, SY + 1, dz - 1, BlockTypes.OAK_FENCE);
                block(es, dx - 1, SY + 2, dz - 1, BlockTypes.OAK_FENCE);

                // Support columns every 4 steps
                if (step % 4 == 2) {
                    block(es, dx, SY - 1, dz, BlockTypes.OAK_LOG);
                    block(es, dx, SY - 2, dz, BlockTypes.OAK_LOG);
                    block(es, dx, SY - 3, dz, BlockTypes.OAK_LOG);
                }
            }

            // Lanterns at each end of the diagonal bridge
            block(es, startX - 1, SY + 3, startZ - 1, BlockTypes.LANTERN);
            block(es, startX + 1, SY + 3, startZ + 1, BlockTypes.LANTERN);
            block(es, startX + steps - 1, SY + 3, startZ - steps - 1, BlockTypes.LANTERN);
            block(es, startX + steps + 1, SY + 3, startZ - steps + 1, BlockTypes.LANTERN);
        }
    }

    // ============================================================================
    //  SECTION 5 — NATURE / FOREST ZONE
    // ============================================================================

    /**
     * Builds the NE-quadrant forest zone with oak, dark-oak, birch, cherry and
     * jungle trees, fern/grass clusters, moss patches, mushrooms, azalea pairs,
     * podzol under jungle trees, rooted-dirt under dark-oaks, bamboo clusters,
     * hanging roots on the underside, and a detailed forest clearing.
     */
    private void buildForestZone(EditSession es) {

        // ── Oak trees ─────────────────────────────────────────────────────────
        int[][] oakPos = {
            {35,-20,7},{45,-35,8},{55,-50,9},{42,-65,7},{62,-40,8},
            {52,-80,9},{70,-25,7},{78,-55,6},{35,-90,8}
        };
        for (int[] p : oakPos) {
            int tx = p[0], tz = p[1], h = p[2];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz <= 0.88) buildOakTree(es, tx, tz, h);
        }

        // ── Dark-oak trees ─────────────────────────────────────────────────────
        int[][] darkOakPos = {
            {48,-28,8},{60,-55,9},{38,-72,8},{72,-38,8},{65,-78,9},{50,-45,7}
        };
        for (int[] p : darkOakPos) {
            int tx = p[0], tz = p[1], h = p[2];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz <= 0.88) buildDarkOakTree(es, tx, tz, h);
        }

        // ── Birch trees ───────────────────────────────────────────────────────
        int[][] birchPos = {
            {40,-18,9},{58,-32,10},{75,-48,8},{44,-88,9},{68,-62,8}
        };
        for (int[] p : birchPos) {
            int tx = p[0], tz = p[1], h = p[2];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz <= 0.88) buildBirchTree(es, tx, tz, h);
        }

        // ── Cherry trees ──────────────────────────────────────────────────────
        int[][] cherryPos = {
            {53,-22,6},{46,-58,7},{80,-32,6},{56,-75,7}
        };
        for (int[] p : cherryPos) {
            int tx = p[0], tz = p[1], h = p[2];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz <= 0.88) buildCherryTree(es, tx, tz, h);
        }

        // ── Jungle trees ──────────────────────────────────────────────────────
        int[][] junglePos = {
            {62,-18,18},{72,-60,20},{42,-48,22}
        };
        for (int[] p : junglePos) {
            int tx = p[0], tz = p[1], h = p[2];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz <= 0.88) buildJungleTree(es, tx, tz, h);
        }

        // ── Fern and SHORT_GRASS scatter ──────────────────────────────────────
        int[] xVals = {35,40,45,50,55,60,65,70,75,80};
        int[] zVals = {-15,-25,-35,-45,-55,-65,-75,-85,-95};
        for (int tx : xVals) {
            for (int tz : zVals) {
                double fx = (double)tx/RX, fz = (double)tz/RZ;
                if (fx*fx + fz*fz > 0.88) continue;
                block(es, tx,     SY+1, tz,   BlockTypes.SHORT_GRASS);
                block(es, tx+1,   SY+1, tz,   BlockTypes.FERN);
                block(es, tx,     SY+1, tz+1, BlockTypes.SHORT_GRASS);
            }
        }

        // ── Moss patches (3×3 MOSS_BLOCK + MOSS_CARPET on top) at 12 positions ─
        int[][] mossPos = {
            {36,-21},{46,-36},{56,-51},{43,-66},{63,-41},
            {53,-81},{71,-26},{79,-56},{49,-29},{61,-56},
            {39,-73},{73,-39}
        };
        for (int[] m : mossPos) {
            int mx = m[0], mz = m[1];
            double fx = (double)mx/RX, fz = (double)mz/RZ;
            if (fx*fx + fz*fz > 0.88) continue;
            fill(es, mx-1, SY, mz-1, mx+1, SY, mz+1, BlockTypes.MOSS_BLOCK);
            fill(es, mx-1, SY+1, mz-1, mx+1, SY+1, mz+1, BlockTypes.MOSS_CARPET);
        }

        // ── Mushrooms (10 scattered positions) ───────────────────────────────
        int[][] mushroomPos = {
            {37,-23},{47,-38},{57,-53},{44,-68},{64,-43},
            {54,-83},{72,-28},{80,-58},{50,-31},{62,-58}
        };
        for (int i = 0; i < mushroomPos.length; i++) {
            int mx = mushroomPos[i][0], mz = mushroomPos[i][1];
            double fx = (double)mx/RX, fz = (double)mz/RZ;
            if (fx*fx + fz*fz > 0.88) continue;
            if (i % 2 == 0) {
                block(es, mx, SY+1, mz, BlockTypes.RED_MUSHROOM);
            } else {
                block(es, mx, SY+1, mz, BlockTypes.BROWN_MUSHROOM);
            }
        }

        // ── Azalea / FLOWERING_AZALEA pairs (8 positions on forest edges) ─────
        int[][] azaleaPos = {
            {34,-19},{44,-34},{54,-49},{41,-64},
            {61,-39},{51,-79},{69,-24},{77,-54}
        };
        for (int i = 0; i < azaleaPos.length; i++) {
            int ax = azaleaPos[i][0], az = azaleaPos[i][1];
            double fx = (double)ax/RX, fz = (double)az/RZ;
            if (fx*fx + fz*fz > 0.88) continue;
            block(es, ax,   SY+1, az,   BlockTypes.AZALEA);
            block(es, ax+1, SY+1, az,   BlockTypes.FLOWERING_AZALEA);
        }

        // ── PODZOL patches under each jungle tree base ────────────────────────
        for (int[] p : junglePos) {
            int tx = p[0], tz = p[1];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz > 0.88) continue;
            fill(es, tx-2, SY, tz-2, tx+2, SY, tz+2, BlockTypes.PODZOL);
        }

        // ── ROOTED_DIRT patches under each dark-oak base ──────────────────────
        for (int[] p : darkOakPos) {
            int tx = p[0], tz = p[1];
            double fx = (double)tx/RX, fz = (double)tz/RZ;
            if (fx*fx + fz*fz > 0.88) continue;
            fill(es, tx-1, SY-1, tz-1, tx+1, SY-1, tz+1, BlockTypes.ROOTED_DIRT);
        }

        // ── Bamboo clusters at 3 positions ────────────────────────────────────
        int[][] bambooPos = {{62,-22},{70,-48},{44,-80}};
        for (int[] b : bambooPos) {
            int bx = b[0], bz = b[1];
            double fx = (double)bx/RX, fz = (double)bz/RZ;
            if (fx*fx + fz*fz > 0.88) continue;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    column(es, bx+dx, bz+dz, SY+1, SY+7, BlockTypes.BAMBOO);
                }
            }
        }

        // ── HANGING_ROOTS on island underside in NE quadrant ─────────────────
        int[][] hangRootPos = {
            {38,-22},{48,-32},{58,-42},{68,-52},{78,-62},
            {43,-72},{53,-52},{63,-32},{73,-42},{83,-52}
        };
        for (int[] hr : hangRootPos) {
            int hrx = hr[0], hrz = hr[1];
            double fx = (double)hrx/RX, fz = (double)hrz/RZ;
            if (fx*fx + fz*fz > 0.80) continue;
            double t = fx*fx + fz*fz;
            int bottomY = 40 + (int)(21.0 * t);
            block(es, hrx, bottomY+4, hrz, BlockTypes.HANGING_ROOTS);
            block(es, hrx, bottomY+5, hrz, BlockTypes.HANGING_ROOTS);
        }

        // ── Forest clearing at (58,-32) ───────────────────────────────────────
        int clx = 58, clz = -32;
        // Clear 9×9 to SHORT_GRASS at SY
        for (int x = clx-4; x <= clx+4; x++) {
            for (int z = clz-4; z <= clz+4; z++) {
                block(es, x, SY, z, BlockTypes.SHORT_GRASS);
            }
        }
        // Stone ring at dist 3.8..4.5 from centre
        for (int x = clx-5; x <= clx+5; x++) {
            for (int z = clz-5; z <= clz+5; z++) {
                double dist = Math.sqrt((double)((x-clx)*(x-clx)+(z-clz)*(z-clz)));
                if (dist >= 3.8 && dist <= 4.5) {
                    block(es, x, SY, z, BlockTypes.STONE_BRICKS);
                }
            }
        }
        // 4 GLOWSTONE on ring at N/S/E/W
        block(es, clx,   SY, clz-4, BlockTypes.GLOWSTONE);
        block(es, clx,   SY, clz+4, BlockTypes.GLOWSTONE);
        block(es, clx-4, SY, clz,   BlockTypes.GLOWSTONE);
        block(es, clx+4, SY, clz,   BlockTypes.GLOWSTONE);
        // 4 CHISELED_STONE_BRICKS at SY+1 inside ring as seats
        block(es, clx-2, SY+1, clz,   BlockTypes.CHISELED_STONE_BRICKS);
        block(es, clx+2, SY+1, clz,   BlockTypes.CHISELED_STONE_BRICKS);
        block(es, clx,   SY+1, clz-2, BlockTypes.CHISELED_STONE_BRICKS);
        block(es, clx,   SY+1, clz+2, BlockTypes.CHISELED_STONE_BRICKS);
        // MOSSY_COBBLESTONE scatter outside ring
        int[] mossOffsets = {-5,-5, -5,5, 5,-5, 5,5, -6,0, 6,0, 0,-6, 0,6};
        for (int i = 0; i < mossOffsets.length; i += 2) {
            block(es, clx+mossOffsets[i], SY, clz+mossOffsets[i+1], BlockTypes.MOSSY_COBBLESTONE);
        }
        // OAK_LOG post (3 tall) + LANTERN at cardinal points outside ring (dist≈5.5)
        int[][] cardinalPosts = {{clx,clz-6},{clx,clz+6},{clx-6,clz},{clx+6,clz}};
        for (int[] cp : cardinalPosts) {
            double fx2 = (double)cp[0]/RX, fz2 = (double)cp[1]/RZ;
            if (fx2*fx2 + fz2*fz2 > 0.88) continue;
            column(es, cp[0], cp[1], SY+1, SY+3, BlockTypes.OAK_LOG);
            block(es, cp[0], SY+4, cp[1], BlockTypes.LANTERN);
        }
    }

    /**
     * Places a single oak tree: trunk column, layered leaf rings, hanging leaves,
     * and two branch log extensions.
     */
    private void buildOakTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY+1, SY+h, BlockTypes.OAK_LOG);
        int top = SY + h;

        // Bottom leaf ring: r²<=18 at top
        for (int x = tx-4; x <= tx+4; x++) {
            for (int z = tz-4; z <= tz+4; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 18) {
                    block(es, x, top, z, BlockTypes.OAK_LEAVES);
                }
            }
        }
        // Mid ring: r²<=10 at top+1
        for (int x = tx-3; x <= tx+3; x++) {
            for (int z = tz-3; z <= tz+3; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 10) {
                    block(es, x, top+1, z, BlockTypes.OAK_LEAVES);
                }
            }
        }
        // Upper ring: r²<=5 at top+2
        for (int x = tx-2; x <= tx+2; x++) {
            for (int z = tz-2; z <= tz+2; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 5) {
                    block(es, x, top+2, z, BlockTypes.OAK_LEAVES);
                }
            }
        }
        // Small ring: r²<=2 at top+3
        for (int x = tx-1; x <= tx+1; x++) {
            for (int z = tz-1; z <= tz+1; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 2) {
                    block(es, x, top+3, z, BlockTypes.OAK_LEAVES);
                }
            }
        }
        // Single leaf at top+4
        block(es, tx, top+4, tz, BlockTypes.OAK_LEAVES);
        // Hanging leaves at top-1 offset by (±2,0) and (0,±2)
        block(es, tx+2, top-1, tz,   BlockTypes.OAK_LEAVES);
        block(es, tx-2, top-1, tz,   BlockTypes.OAK_LEAVES);
        block(es, tx,   top-1, tz+2, BlockTypes.OAK_LEAVES);
        block(es, tx,   top-1, tz-2, BlockTypes.OAK_LEAVES);
        // 2 log branch extensions at top-1
        block(es, tx+1, top-1, tz,   BlockTypes.OAK_LOG);
        block(es, tx-1, top-1, tz+1, BlockTypes.OAK_LOG);
    }

    /**
     * Places a single dark-oak tree: 2×2 trunk, wide layered canopy, branch logs,
     * moss patches and rooted-dirt at base.
     */
    private void buildDarkOakTree(EditSession es, int tx, int tz, int h) {
        // 2×2 trunk
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                column(es, tx+dx, tz+dz, SY+1, SY+h, BlockTypes.DARK_OAK_LOG);
            }
        }
        int top = SY + h;
        double cx = tx + 0.5, cz2 = tz + 0.5;

        // Wide layer 1 (top): r²<=40 from centre (tx+0.5, tz+0.5)
        for (int x = tx-6; x <= tx+6; x++) {
            for (int z = tz-6; z <= tz+6; z++) {
                double ddx = x - cx, ddz = z - cz2;
                if (ddx*ddx + ddz*ddz <= 40.0) {
                    block(es, x, top, z, BlockTypes.DARK_OAK_LEAVES);
                }
            }
        }
        // Layer 2 (top+1): r²<=25
        for (int x = tx-5; x <= tx+5; x++) {
            for (int z = tz-5; z <= tz+5; z++) {
                double ddx = x - cx, ddz = z - cz2;
                if (ddx*ddx + ddz*ddz <= 25.0) {
                    block(es, x, top+1, z, BlockTypes.DARK_OAK_LEAVES);
                }
            }
        }
        // Layer 3 (top+2): r²<=12
        for (int x = tx-4; x <= tx+4; x++) {
            for (int z = tz-4; z <= tz+4; z++) {
                double ddx = x - cx, ddz = z - cz2;
                if (ddx*ddx + ddz*ddz <= 12.0) {
                    block(es, x, top+2, z, BlockTypes.DARK_OAK_LEAVES);
                }
            }
        }
        // Top cap (top+3): r²<=4
        for (int x = tx-2; x <= tx+2; x++) {
            for (int z = tz-2; z <= tz+2; z++) {
                double ddx = x - cx, ddz = z - cz2;
                if (ddx*ddx + ddz*ddz <= 4.0) {
                    block(es, x, top+3, z, BlockTypes.DARK_OAK_LEAVES);
                }
            }
        }
        // Branch logs
        block(es, tx-3, top, tz,   BlockTypes.DARK_OAK_LOG);
        block(es, tx+4, top, tz+1, BlockTypes.DARK_OAK_LOG);
        block(es, tx,   top, tz-3, BlockTypes.DARK_OAK_LOG);
        block(es, tx+1, top, tz+4, BlockTypes.DARK_OAK_LOG);
        // MOSS_BLOCK patches 3×3 around each trunk corner, MOSS_CARPET on top
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                fill(es, tx+dx-1, SY, tz+dz-1, tx+dx+1, SY, tz+dz+1, BlockTypes.MOSS_BLOCK);
                fill(es, tx+dx-1, SY+1, tz+dz-1, tx+dx+1, SY+1, tz+dz+1, BlockTypes.MOSS_CARPET);
            }
        }
        // ROOTED_DIRT in 2×2 at base on SY-1
        fill(es, tx, SY-1, tz, tx+1, SY-1, tz+1, BlockTypes.ROOTED_DIRT);
    }

    /**
     * Places a single birch tree: trunk column, leaf layers using Manhattan
     * distance, and hanging leaf blocks.
     */
    private void buildBirchTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY+1, SY+h, BlockTypes.BIRCH_LOG);
        int top = SY + h;

        // Leaf layers: dy=-2..3 from top, varying radius
        for (int dy = -2; dy <= 3; dy++) {
            int r;
            if (dy <= -1)       r = 1;
            else if (dy <= 1)   r = 2;
            else                r = 1;
            for (int x = tx-(r+1); x <= tx+(r+1); x++) {
                for (int z = tz-(r+1); z <= tz+(r+1); z++) {
                    if (Math.abs(x-tx) + Math.abs(z-tz) <= r+1) {
                        block(es, x, top+dy+2, z, BlockTypes.BIRCH_LEAVES);
                    }
                }
            }
        }
        // Top single leaf at top+5
        block(es, tx, top+5, tz, BlockTypes.BIRCH_LEAVES);
        // 2 hanging leaf blocks below canopy
        block(es, tx+2, top-1, tz,   BlockTypes.BIRCH_LEAVES);
        block(es, tx,   top-1, tz-2, BlockTypes.BIRCH_LEAVES);
    }

    /**
     * Places a single cherry tree: trunk column, circular leaf layers, top cap,
     * and diagonal branch logs.
     */
    private void buildCherryTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY+1, SY+h, BlockTypes.CHERRY_LOG);
        int top = SY + h;

        // Leaf layers: dy=-2..3
        for (int dy = -2; dy <= 3; dy++) {
            int r;
            if (dy == -2)               r = 2;
            else if (dy == -1 || dy == 0) r = 3;
            else if (dy == 1)           r = 3;
            else                        r = 2;
            for (int x = tx-r; x <= tx+r; x++) {
                for (int z = tz-r; z <= tz+r; z++) {
                    int ddx = x-tx, ddz = z-tz;
                    if (ddx*ddx + ddz*ddz <= r*r) {
                        block(es, x, top+dy, z, BlockTypes.CHERRY_LEAVES);
                    }
                }
            }
        }
        // Top cap at top+4
        block(es, tx,   top+4, tz,   BlockTypes.CHERRY_LEAVES);
        block(es, tx+1, top+4, tz,   BlockTypes.CHERRY_LEAVES);
        block(es, tx-1, top+4, tz,   BlockTypes.CHERRY_LEAVES);
        block(es, tx,   top+4, tz+1, BlockTypes.CHERRY_LEAVES);
        block(es, tx,   top+4, tz-1, BlockTypes.CHERRY_LEAVES);
        // Diagonal branch logs
        block(es, tx+1, top-2, tz,   BlockTypes.CHERRY_LOG);
        block(es, tx-1, top-2, tz+1, BlockTypes.CHERRY_LOG);
        block(es, tx+1, top-1, tz-1, BlockTypes.CHERRY_LOG);
    }

    /**
     * Places a single jungle tree: tall trunk column, wide circular canopy,
     * sparse hanging leaves, podzol patch at base, rooted-dirt underlay.
     */
    private void buildJungleTree(EditSession es, int tx, int tz, int h) {
        column(es, tx, tz, SY+1, SY+h, BlockTypes.JUNGLE_LOG);
        int top = SY + h;

        // Canopy at top: r²<=30
        for (int x = tx-6; x <= tx+6; x++) {
            for (int z = tz-6; z <= tz+6; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 30) block(es, x, top, z, BlockTypes.JUNGLE_LEAVES);
            }
        }
        // top+1: r²<=20
        for (int x = tx-5; x <= tx+5; x++) {
            for (int z = tz-5; z <= tz+5; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 20) block(es, x, top+1, z, BlockTypes.JUNGLE_LEAVES);
            }
        }
        // top+2: r²<=12
        for (int x = tx-4; x <= tx+4; x++) {
            for (int z = tz-4; z <= tz+4; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 12) block(es, x, top+2, z, BlockTypes.JUNGLE_LEAVES);
            }
        }
        // top+3: r²<=5
        for (int x = tx-2; x <= tx+2; x++) {
            for (int z = tz-2; z <= tz+2; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 5) block(es, x, top+3, z, BlockTypes.JUNGLE_LEAVES);
            }
        }
        // top+4: single leaf
        block(es, tx, top+4, tz, BlockTypes.JUNGLE_LEAVES);
        // 4 branch logs at top-3
        block(es, tx+2, top-3, tz,   BlockTypes.JUNGLE_LOG);
        block(es, tx-2, top-3, tz,   BlockTypes.JUNGLE_LOG);
        block(es, tx,   top-3, tz+2, BlockTypes.JUNGLE_LOG);
        block(es, tx,   top-3, tz-2, BlockTypes.JUNGLE_LOG);
        // Hanging leaves at top-1: r²<=25 but only if (x+z)%3==0 (sparse)
        for (int x = tx-5; x <= tx+5; x++) {
            for (int z = tz-5; z <= tz+5; z++) {
                int dx = x-tx, dz = z-tz;
                if (dx*dx + dz*dz <= 25 && (x+z)%3 == 0) {
                    block(es, x, top-1, z, BlockTypes.JUNGLE_LEAVES);
                }
            }
        }
        // PODZOL patch under base
        fill(es, tx-2, SY, tz-2, tx+2, SY, tz+2, BlockTypes.PODZOL);
        // ROOTED_DIRT under podzol at SY-1
        fill(es, tx-2, SY-1, tz-2, tx+2, SY-1, tz+2, BlockTypes.ROOTED_DIRT);
    }

    // ============================================================================
    //  SECTION — WATER GARDENS
    // ============================================================================

    /**
     * Builds the SW-quadrant water gardens: three ornate pools, two streams,
     * two waterfalls, decorative rocks, lamp posts, dripleaf plants, and
     * three feature trees.
     */
    private void buildWaterGardens(EditSession es) {

        // ── Main pool centred at (-40,SY,45), 13×11 ──────────────────────────
        {
            int px = -40, pz = 45;
            // Outer rim: x=-46..-34, z=39..51 — place edge blocks
            for (int x = px-6; x <= px+6; x++) {
                for (int z = pz-6; z <= pz+6; z++) {
                    boolean isEdge = (x == px-6 || x == px+6 || z == pz-6 || z == pz+6);
                    boolean isCorner = (x == px-6 || x == px+6) && (z == pz-6 || z == pz+6);
                    if (isEdge) {
                        block(es, x, SY,   z, BlockTypes.STONE_BRICKS);
                        block(es, x, SY+1, z, BlockTypes.STONE_BRICK_WALL);
                        if (isCorner) {
                            block(es, x, SY+2, z, BlockTypes.CHISELED_STONE_BRICKS);
                        }
                    } else {
                        // Interior
                        block(es, x, SY,   z, BlockTypes.AIR);
                        block(es, x, SY-1, z, BlockTypes.WATER);
                        block(es, x, SY-2, z, BlockTypes.PRISMARINE);
                    }
                }
            }
            // SEA_LANTERN at centre floor
            block(es, px, SY-2, pz, BlockTypes.SEA_LANTERN);
            // PRISMARINE_BRICKS central area
            fill(es, px-2, SY-2, pz-2, px+2, SY-2, pz+2, BlockTypes.PRISMARINE_BRICKS);
            // Restore SEA_LANTERN after fill
            block(es, px, SY-2, pz, BlockTypes.SEA_LANTERN);
            // Central fountain column
            block(es, px, SY-1, pz, BlockTypes.PRISMARINE);
            block(es, px, SY,   pz, BlockTypes.WATER);
            // Lily pads
            block(es, px-4, SY, pz-3, BlockTypes.LILY_PAD);
            block(es, px-2, SY, pz+3, BlockTypes.LILY_PAD);
            block(es, px+2, SY, pz-2, BlockTypes.LILY_PAD);
            block(es, px+4, SY, pz+2, BlockTypes.LILY_PAD);
            block(es, px-1, SY, pz-4, BlockTypes.LILY_PAD);
            block(es, px-3, SY, pz+4, BlockTypes.LILY_PAD);
            block(es, px+3, SY, pz-1, BlockTypes.LILY_PAD);
            block(es, px-1, SY, pz+5, BlockTypes.LILY_PAD);
            // Mossy corners at wall top+2
            block(es, px-6, SY+2, pz-6, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, px+6, SY+2, pz-6, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, px-6, SY+2, pz+6, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, px+6, SY+2, pz+6, BlockTypes.MOSSY_STONE_BRICKS);
        }

        // ── Secondary pool centred at (-60,SY,62), 7×7 ───────────────────────
        {
            int px = -60, pz = 62;
            for (int x = px-3; x <= px+3; x++) {
                for (int z = pz-3; z <= pz+3; z++) {
                    boolean isEdge = (x == px-3 || x == px+3 || z == pz-3 || z == pz+3);
                    if (isEdge) {
                        block(es, x, SY,   z, BlockTypes.MOSSY_STONE_BRICKS);
                        block(es, x, SY+1, z, BlockTypes.STONE_BRICK_WALL);
                    } else {
                        block(es, x, SY,   z, BlockTypes.AIR);
                        block(es, x, SY-1, z, BlockTypes.WATER);
                        block(es, x, SY-2, z, BlockTypes.DARK_PRISMARINE);
                    }
                }
            }
            block(es, px, SY-2, pz, BlockTypes.SEA_LANTERN);
            // 4 lily pads
            block(es, px-1, SY, pz-1, BlockTypes.LILY_PAD);
            block(es, px+1, SY, pz+1, BlockTypes.LILY_PAD);
            block(es, px-1, SY, pz+1, BlockTypes.LILY_PAD);
            block(es, px+1, SY, pz-1, BlockTypes.LILY_PAD);
        }

        // ── Tertiary pool centred at (-52,SY,80), 5×5 ────────────────────────
        {
            int px = -52, pz = 80;
            double efx = (double)px/RX, efz = (double)pz/RZ;
            if (efx*efx + efz*efz <= 0.94) {
                for (int x = px-2; x <= px+2; x++) {
                    for (int z = pz-2; z <= pz+2; z++) {
                        boolean isEdge = (x == px-2 || x == px+2 || z == pz-2 || z == pz+2);
                        if (isEdge) {
                            block(es, x, SY, z, BlockTypes.PRISMARINE_BRICKS);
                        } else {
                            block(es, x, SY,   z, BlockTypes.AIR);
                            block(es, x, SY-1, z, BlockTypes.WATER);
                            block(es, x, SY-2, z, BlockTypes.PRISMARINE);
                        }
                    }
                }
                block(es, px, SY-2, pz, BlockTypes.SEA_LANTERN);
            }
        }

        // ── Stream 1 (main→secondary): x from -40 to -60 ─────────────────────
        for (int x = -40; x >= -60; x--) {
            int curZ = 51 + (int)(((x + 40) / 20.0) * 11);
            double sfx = (double)x/RX, sfz = (double)curZ/RZ;
            if (sfx*sfx + sfz*sfz > 0.95) continue;
            block(es, x, SY-1, curZ, BlockTypes.WATER);
            block(es, x, SY-2, curZ, BlockTypes.STONE_BRICKS);
            block(es, x, SY,   curZ, BlockTypes.AIR);
            // Kerb stones
            double sfx2 = (double)x/RX, sfz2a = (double)(curZ-1)/RZ, sfz2b = (double)(curZ+1)/RZ;
            if (sfx2*sfx2 + sfz2a*sfz2a <= 0.95) block(es, x, SY, curZ-1, BlockTypes.MOSSY_STONE_BRICKS);
            if (sfx2*sfx2 + sfz2b*sfz2b <= 0.95) block(es, x, SY, curZ+1, BlockTypes.MOSSY_STONE_BRICKS);
        }

        // ── Stream 2 (secondary→tertiary): z from 62 to 80 ───────────────────
        for (int z = 62; z <= 80; z++) {
            int sx = -60 + (int)(((z - 62) / 18.0) * 8);
            double sfx = (double)sx/RX, sfz = (double)z/RZ;
            if (sfx*sfx + sfz*sfz > 0.95) continue;
            block(es, sx, SY-1, z, BlockTypes.WATER);
            block(es, sx, SY-2, z, BlockTypes.STONE_BRICKS);
            block(es, sx, SY,   z, BlockTypes.AIR);
        }

        // ── Waterfall 1 at (-74,_,32) ─────────────────────────────────────────
        {
            double wfx = (double)(-74)/RX, wfz = (double)32/RZ;
            if (wfx*wfx + wfz*wfz <= 0.95) {
                for (int y = SY; y >= SY-5; y--) {
                    block(es, -74, y, 32, BlockTypes.WATER);
                    block(es, -74, y, 33, BlockTypes.WATER);
                    block(es, -75, y, 32, BlockTypes.STONE_BRICKS);
                    block(es, -75, y, 33, BlockTypes.STONE_BRICKS);
                }
            }
        }

        // ── Waterfall 2 at (-70,_,54) ─────────────────────────────────────────
        {
            double wfx = (double)(-70)/RX, wfz = (double)54/RZ;
            if (wfx*wfx + wfz*wfz <= 0.95) {
                for (int y = SY; y >= SY-5; y--) {
                    block(es, -70, y, 54, BlockTypes.WATER);
                    block(es, -70, y, 55, BlockTypes.WATER);
                    block(es, -71, y, 54, BlockTypes.STONE_BRICKS);
                    block(es, -71, y, 55, BlockTypes.STONE_BRICKS);
                }
            }
        }

        // ── Decorative rock piles (8 positions near pools) ───────────────────
        int[][] rockPos = {
            {-47,42},{-33,42},{-47,48},{-33,48},
            {-63,60},{-57,60},{-63,64},{-57,64}
        };
        BlockType[] rockMats = {
            BlockTypes.MOSSY_COBBLESTONE, BlockTypes.COBBLESTONE, BlockTypes.STONE,
            BlockTypes.MOSSY_COBBLESTONE, BlockTypes.COBBLESTONE, BlockTypes.STONE,
            BlockTypes.MOSSY_COBBLESTONE, BlockTypes.COBBLESTONE
        };
        for (int i = 0; i < rockPos.length; i++) {
            int rx = rockPos[i][0], rz = rockPos[i][1];
            double rfx = (double)rx/RX, rfz = (double)rz/RZ;
            if (rfx*rfx + rfz*rfz > 0.94) continue;
            block(es, rx,   SY+1, rz,   rockMats[i]);
            block(es, rx+1, SY+1, rz,   rockMats[(i+1)%rockMats.length]);
            block(es, rx,   SY+2, rz,   rockMats[(i+2)%rockMats.length]);
        }

        // ── Lamp posts near garden (4 posts at garden corners) ───────────────
        int[][] lampPos = {{-35,38},{-47,38},{-35,52},{-47,52}};
        for (int[] lp : lampPos) {
            double lfx = (double)lp[0]/RX, lfz = (double)lp[1]/RZ;
            if (lfx*lfx + lfz*lfz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }

        // ── BIG_DRIPLEAF at 8 positions near water edges ──────────────────────
        int[][] dripleafPos = {
            {-43,41},{-37,41},{-43,49},{-37,49},
            {-59,61},{-61,63},{-59,65},{-53,79}
        };
        for (int[] dp : dripleafPos) {
            double dfx = (double)dp[0]/RX, dfz = (double)dp[1]/RZ;
            if (dfx*dfx + dfz*dfz > 0.94) continue;
            block(es, dp[0], SY+1, dp[1], BlockTypes.BIG_DRIPLEAF);
        }

        // ── SMALL_DRIPLEAF at 4 positions in stream areas ────────────────────
        block(es, -45, SY, 53, BlockTypes.SMALL_DRIPLEAF);
        block(es, -47, SY, 56, BlockTypes.SMALL_DRIPLEAF);
        block(es, -55, SY, 66, BlockTypes.SMALL_DRIPLEAF);
        block(es, -57, SY, 70, BlockTypes.SMALL_DRIPLEAF);

        // ── Feature trees ─────────────────────────────────────────────────────
        {
            double fx = (double)(-34)/RX, fz = (double)52/RZ;
            if (fx*fx + fz*fz <= 0.88) buildOakTree(es, -34, 52, 5);
        }
        {
            double fx = (double)(-55)/RX, fz = (double)32/RZ;
            if (fx*fx + fz*fz <= 0.88) buildBirchTree(es, -55, 32, 6);
        }
        {
            double fx = (double)(-44)/RX, fz = (double)70/RZ;
            if (fx*fx + fz*fz <= 0.88) buildCherryTree(es, -44, 70, 5);
        }
    }

    // ============================================================================
    //  SECTION — ARENA ZONE
    // ============================================================================

    /**
     * Builds the NW-quadrant octagonal arena centred at (-50,SY,-50): perimeter
     * walls, interior floor, arched entrances, corner pillars, centre marker,
     * decorative posts, iron-bar dividers, alcoves, spectator ledge, approach
     * paths, lamp posts, and exterior moss scatter.
     */
    private void buildArenaZone(EditSession es) {
        int acx = -50, acz = -50;

        // ── Octagon walls (radius 32) ──────────────────────────────────────────
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int wx = acx + (int)(32 * Math.cos(rad));
            int wz = acz + (int)(32 * Math.sin(rad));
            double efx = (double)wx/RX, efz = (double)wz/RZ;
            if (efx*efx + efz*efz > 0.96) continue;
            block(es, wx, SY,   wz, BlockTypes.STONE_BRICKS);
            block(es, wx, SY+1, wz, BlockTypes.STONE_BRICKS);
            block(es, wx, SY+2, wz, BlockTypes.STONE_BRICKS);
            // Merlons every 5° (3 of 5 filled)
            if (angle % 5 < 3) {
                block(es, wx, SY+3, wz, BlockTypes.STONE_BRICKS);
                block(es, wx, SY+4, wz, BlockTypes.STONE_BRICK_WALL);
            }
            // Mossy accent every 9°
            if (angle % 9 == 0) {
                block(es, wx, SY+1, wz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }

        // ── Interior floor: POLISHED_DEEPSLATE (dist from centre <=31) ────────
        for (int x = acx-31; x <= acx+31; x++) {
            for (int z = acz-31; z <= acz+31; z++) {
                int dx = x-acx, dz = z-acz;
                if (dx*dx + dz*dz <= 31*31) {
                    block(es, x, SY, z, BlockTypes.POLISHED_DEEPSLATE);
                }
            }
        }

        // ── 4 Arched entrances (AIR) ──────────────────────────────────────────
        // North: z=-82..-78, x=-51..-49
        for (int z = acz-32; z <= acz-28; z++) {
            for (int x = acx-1; x <= acx+1; x++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // South: z=-18..-22
        for (int z = acz+28; z <= acz+32; z++) {
            for (int x = acx-1; x <= acx+1; x++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // East: x=-18..-22
        for (int x = acx+28; x <= acx+32; x++) {
            for (int z = acz-1; z <= acz+1; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // West: x=-78..-82
        for (int x = acx-32; x <= acx-28; x++) {
            for (int z = acz-1; z <= acz+1; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }

        // ── 8 Corner pillars at every 45° on octagon ──────────────────────────
        for (int a = 0; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            int px2 = acx + (int)(31 * Math.cos(rad));
            int pz2 = acz + (int)(31 * Math.sin(rad));
            double efx = (double)px2/RX, efz = (double)pz2/RZ;
            if (efx*efx + efz*efz > 0.96) continue;
            column(es, px2, pz2, SY, SY+6, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, px2, SY+7, pz2, BlockTypes.GLOWSTONE);
        }

        // ── Centre marker ─────────────────────────────────────────────────────
        fill(es, acx-2, SY, acz-2, acx+2, SY, acz+2, BlockTypes.QUARTZ_BRICKS);
        block(es, acx, SY,   acz, BlockTypes.SEA_LANTERN);
        block(es, acx, SY+1, acz, BlockTypes.CHAIN);
        block(es, acx, SY+2, acz, BlockTypes.LANTERN);

        // ── 4 Decorative posts inside arena ───────────────────────────────────
        int[][] innerPosts = {{acx-14,acz},{acx+14,acz},{acx,acz-14},{acx,acz+14}};
        for (int[] ip : innerPosts) {
            column(es, ip[0], ip[1], SY+1, SY+4, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, ip[0], SY+5, ip[1], BlockTypes.CHAIN);
            block(es, ip[0], SY+6, ip[1], BlockTypes.LANTERN);
        }

        // ── Iron-bar divider lines (crossing through centre) ──────────────────
        // Along x-axis (z=acz, x from acx-10 to acx+10)
        for (int x = acx-10; x <= acx+10; x++) {
            block(es, x, SY+1, acz, BlockTypes.IRON_BARS);
        }
        // Along z-axis (x=acx, z from acz-10 to acz+10)
        for (int z = acz-10; z <= acz+10; z++) {
            block(es, acx, SY+1, z, BlockTypes.IRON_BARS);
        }

        // ── East alcove at (-20,_,-50) ────────────────────────────────────────
        {
            int ax = acx+30, az = acz;
            double efx = (double)ax/RX, efz = (double)az/RZ;
            if (efx*efx + efz*efz <= 0.97) {
                for (int x = ax-1; x <= ax+1; x++)
                    for (int y = SY; y <= SY+2; y++)
                        for (int z = az-1; z <= az+1; z++)
                            block(es, x, y, z, BlockTypes.CAVE_AIR);
                block(es, ax, SY, az,   BlockTypes.CRAFTING_TABLE);
                block(es, ax, SY, az+1, BlockTypes.BARREL);
            }
        }
        // ── West alcove at (-80,_,-50) ────────────────────────────────────────
        {
            int ax = acx-30, az = acz;
            double efx = (double)ax/RX, efz = (double)az/RZ;
            if (efx*efx + efz*efz <= 0.97) {
                for (int x = ax-1; x <= ax+1; x++)
                    for (int y = SY; y <= SY+2; y++)
                        for (int z = az-1; z <= az+1; z++)
                            block(es, x, y, z, BlockTypes.CAVE_AIR);
                block(es, ax, SY, az, BlockTypes.CHEST);
            }
        }

        // ── Spectator ring (SY+3, 1 block outward from perimeter) ─────────────
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int wx = acx + (int)(33 * Math.cos(rad));
            int wz = acz + (int)(33 * Math.sin(rad));
            double efx = (double)wx/RX, efz = (double)wz/RZ;
            if (efx*efx + efz*efz > 0.97) continue;
            block(es, wx, SY+3, wz, BlockTypes.STONE_BRICKS);
        }

        // ── Approach paths from each entrance (4×15 STONE_BRICKS) ─────────────
        // North approach (z from acz-33 to acz-47)
        for (int z = acz-33; z >= acz-47; z--) {
            for (int x = acx-2; x <= acx+2; x++) {
                double efx = (double)x/RX, efz = (double)z/RZ;
                if (efx*efx + efz*efz > 0.97) continue;
                block(es, x, SY, z, BlockTypes.STONE_BRICKS);
            }
        }
        // South approach
        for (int z = acz+33; z <= acz+47; z++) {
            for (int x = acx-2; x <= acx+2; x++) {
                double efx = (double)x/RX, efz = (double)z/RZ;
                if (efx*efx + efz*efz > 0.97) continue;
                block(es, x, SY, z, BlockTypes.STONE_BRICKS);
            }
        }
        // East approach
        for (int x = acx+33; x <= acx+47; x++) {
            for (int z = acz-2; z <= acz+2; z++) {
                double efx = (double)x/RX, efz = (double)z/RZ;
                if (efx*efx + efz*efz > 0.97) continue;
                block(es, x, SY, z, BlockTypes.STONE_BRICKS);
            }
        }
        // West approach
        for (int x = acx-33; x >= acx-47; x--) {
            for (int z = acz-2; z <= acz+2; z++) {
                double efx = (double)x/RX, efz = (double)z/RZ;
                if (efx*efx + efz*efz > 0.97) continue;
                block(es, x, SY, z, BlockTypes.STONE_BRICKS);
            }
        }

        // ── 4 Lamp posts at arena entrance corners ────────────────────────────
        int[][] arenaLamps = {{acx-2,acz-35},{acx+2,acz-35},{acx-2,acz+35},{acx+2,acz+35}};
        for (int[] al : arenaLamps) {
            double efx = (double)al[0]/RX, efz = (double)al[1]/RZ;
            if (efx*efx + efz*efz > 0.97) continue;
            column(es, al[0], al[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, al[0], SY+4, al[1], BlockTypes.LANTERN);
        }

        // ── Moss and SHORT_GRASS outside arena (20 positions) ─────────────────
        int[][] outsideMoss = {
            {acx+33,acz+3},{acx+33,acz-3},{acx-33,acz+3},{acx-33,acz-3},
            {acx+3,acz+33},{acx-3,acz+33},{acx+3,acz-33},{acx-3,acz-33},
            {acx+24,acz+24},{acx-24,acz+24},{acx+24,acz-24},{acx-24,acz-24},
            {acx+35,acz+10},{acx+35,acz-10},{acx-35,acz+10},{acx-35,acz-10},
            {acx+10,acz+35},{acx-10,acz+35},{acx+10,acz-35},{acx-10,acz-35}
        };
        for (int[] om : outsideMoss) {
            double efx = (double)om[0]/RX, efz = (double)om[1]/RZ;
            if (efx*efx + efz*efz > 0.97) continue;
            block(es, om[0],   SY+1, om[1],   BlockTypes.MOSS_CARPET);
            block(es, om[0]+1, SY+1, om[1],   BlockTypes.SHORT_GRASS);
            block(es, om[0],   SY+1, om[1]+1, BlockTypes.SHORT_GRASS);
        }
    }

    // ============================================================================
    //  SECTION — RUINS ZONE
    // ============================================================================

    /**
     * Builds the SE-quadrant ruins zone with five distinct ruin structures, zone
     * debris scatter, and three feature trees.
     */
    private void buildRuinsZone(EditSession es) {

        // ── Ruin 1 — Collapsed tower at (55,SY,45) ───────────────────────────
        {
            int rx = 55, rz = 45;
            // 7×7 base fill at SY
            fill(es, rx-3, SY, rz-3, rx+3, SY, rz+3, BlockTypes.STONE_BRICKS);
            // Hollow walls SY+1..SY+8
            for (int y = SY+1; y <= SY+8; y++) {
                for (int x = rx-3; x <= rx+3; x++) {
                    for (int z = rz-3; z <= rz+3; z++) {
                        boolean isWall = (x == rx-3 || x == rx+3 || z == rz-3 || z == rz+3);
                        if (isWall) {
                            // Mix MOSSY_STONE_BRICKS on pattern
                            if ((x + z) % 3 == 0) {
                                block(es, x, y, z, BlockTypes.MOSSY_STONE_BRICKS);
                            } else {
                                block(es, x, y, z, BlockTypes.CRACKED_STONE_BRICKS);
                            }
                        }
                    }
                }
            }
            // Collapsed south wall (remove top of south face)
            for (int y = SY+5; y <= SY+8; y++) {
                for (int x = rx-3; x <= rx+3; x++) {
                    block(es, x, y, rz+3, BlockTypes.AIR);
                }
            }
            // Debris blocks (20 scattered around exterior)
            int[][] debrisOff = {
                {4,0},{-4,0},{0,4},{0,-4},{4,2},{-4,2},{4,-2},{-4,-2},
                {2,4},{-2,4},{2,-4},{-2,-4},{5,1},{-5,1},{5,-1},{-5,-1},
                {1,5},{-1,5},{1,-5},{-1,-5}
            };
            BlockType[] debrisMats = {
                BlockTypes.COBBLESTONE, BlockTypes.MOSSY_COBBLESTONE,
                BlockTypes.CRACKED_STONE_BRICKS, BlockTypes.COBBLESTONE
            };
            for (int i = 0; i < debrisOff.length; i++) {
                int dbx = rx+debrisOff[i][0], dbz = rz+debrisOff[i][1];
                double dfx = (double)dbx/RX, dfz = (double)dbz/RZ;
                if (dfx*dfx + dfz*dfz > 0.94) continue;
                block(es, dbx, SY+1, dbz, debrisMats[i % debrisMats.length]);
            }
            // MOSS_BLOCK 5×5 patch around base
            fill(es, rx-2, SY, rz-2, rx+2, SY, rz+2, BlockTypes.MOSS_BLOCK);
            // Interior: CRACKED_STONE_BRICKS rubble on floor
            fill(es, rx-2, SY+1, rz-2, rx+2, SY+1, rz+2, BlockTypes.CRACKED_STONE_BRICKS);
        }

        // ── Ruin 2 — Broken archway at (40,SY,68) ────────────────────────────
        {
            int rx = 40, rz = 68;
            // Platform
            fill(es, rx-3, SY, rz-3, rx+3, SY, rz+3, BlockTypes.MOSSY_STONE_BRICKS);
            // Left pillar
            column(es, rx-3, rz-3, SY+1, SY+5, BlockTypes.STONE_BRICKS);
            // Right pillar
            column(es, rx+3, rz-3, SY+1, SY+5, BlockTypes.STONE_BRICKS);
            // Intact left arch section (left half only)
            fill(es, rx-3, SY+6, rz-3, rx-1, SY+6, rz-3, BlockTypes.MOSSY_COBBLESTONE);
            // Right arch: already air
            // Pillar tops: CHISELED_STONE_BRICKS
            block(es, rx-3, SY+6, rz-3, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, rx+3, SY+6, rz-3, BlockTypes.CHISELED_STONE_BRICKS);
            // Debris: 8 scattered blocks on ground
            int[][] arch2Debris = {
                {rx-4,rz-4},{rx+4,rz-4},{rx-4,rz+2},{rx+4,rz+2},
                {rx-5,rz},{rx+5,rz},{rx,rz-5},{rx,rz+4}
            };
            BlockType[] archDebrisMats = {
                BlockTypes.CRACKED_STONE_BRICKS, BlockTypes.COBBLESTONE,
                BlockTypes.CRACKED_STONE_BRICKS, BlockTypes.MOSSY_COBBLESTONE,
                BlockTypes.COBBLESTONE, BlockTypes.CRACKED_STONE_BRICKS,
                BlockTypes.MOSSY_COBBLESTONE, BlockTypes.COBBLESTONE
            };
            for (int i = 0; i < arch2Debris.length; i++) {
                double dfx = (double)arch2Debris[i][0]/RX, dfz = (double)arch2Debris[i][1]/RZ;
                if (dfx*dfx + dfz*dfz > 0.94) continue;
                block(es, arch2Debris[i][0], SY+1, arch2Debris[i][1], archDebrisMats[i]);
            }
        }

        // ── Ruin 3 — Sunken chamber at (65,SY-3,32) ──────────────────────────
        {
            int rx = 65, rz = 32;
            // Clear 9×9 area 3 blocks deep
            fill(es, rx-4, SY-3, rz-4, rx+4, SY, rz+4, BlockTypes.CAVE_AIR);
            // Restore floor
            fill(es, rx-4, SY-3, rz-4, rx+4, SY-3, rz+4, BlockTypes.STONE_BRICKS);
            // South wall (4 tall): z=rz+4
            fill(es, rx-4, SY-3, rz+4, rx+4, SY, rz+4, BlockTypes.CRACKED_STONE_BRICKS);
            // West wall (4 tall): x=rx-4
            fill(es, rx-4, SY-3, rz-4, rx-4, SY, rz+4, BlockTypes.CRACKED_STONE_BRICKS);
            // North wall (2 tall): z=rz-4
            fill(es, rx-4, SY-3, rz-4, rx+4, SY-2, rz-4, BlockTypes.MOSSY_STONE_BRICKS);
            // East wall (2 tall): x=rx+4
            fill(es, rx+4, SY-3, rz-4, rx+4, SY-2, rz+4, BlockTypes.MOSSY_STONE_BRICKS);
            // GRAVEL scatter on floor
            for (int x = rx-3; x <= rx+3; x += 2) {
                for (int z = rz-3; z <= rz+3; z += 2) {
                    block(es, x, SY-3, z, BlockTypes.GRAVEL);
                }
            }
            // Hidden chest alcove
            fill(es, rx-1, SY-3, rz-4, rx+1, SY-1, rz-4, BlockTypes.STONE_BRICKS);
            block(es, rx, SY-2, rz-4, BlockTypes.CHEST);
            // 4 DEEPSLATE_COAL_ORE in north wall
            block(es, rx-2, SY-2, rz-4, BlockTypes.DEEPSLATE_COAL_ORE);
            block(es, rx+2, SY-2, rz-4, BlockTypes.DEEPSLATE_COAL_ORE);
            block(es, rx-1, SY-1, rz-4, BlockTypes.DEEPSLATE_COAL_ORE);
            block(es, rx+1, SY-1, rz-4, BlockTypes.DEEPSLATE_COAL_ORE);
        }

        // ── Ruin 4 — Fallen obelisk at (72,SY,72) ────────────────────────────
        {
            int rx = 72, rz = 72;
            double rfx = (double)rx/RX, rfz = (double)rz/RZ;
            if (rfx*rfx + rfz*rfz <= 0.94) {
                // Base
                fill(es, rx-2, SY, rz-2, rx+2, SY, rz+2, BlockTypes.CHISELED_STONE_BRICKS);
                // Stub column (3 tall)
                column(es, rx, rz, SY+1, SY+3, BlockTypes.STONE_BRICKS);
                // Fallen blocks on ground
                block(es, rx+1, SY+1, rz+3, BlockTypes.STONE_BRICKS);
                block(es, rx+2, SY+1, rz+4, BlockTypes.STONE_BRICKS);
                block(es, rx+3, SY+1, rz+5, BlockTypes.MOSSY_COBBLESTONE);
                // 8 debris blocks in 6-block radius
                int[][] ob4Debris = {
                    {rx+4,rz+1},{rx-4,rz+1},{rx+4,rz-1},{rx-4,rz-1},
                    {rx+1,rz+4},{rx-1,rz+4},{rx+1,rz-4},{rx-1,rz-4}
                };
                for (int[] dd : ob4Debris) {
                    double ddfx = (double)dd[0]/RX, ddfz = (double)dd[1]/RZ;
                    if (ddfx*ddfx + ddfz*ddfz > 0.94) continue;
                    block(es, dd[0], SY+1, dd[1], BlockTypes.COBBLESTONE);
                }
                // MOSS_CARPET patches around base
                for (int x = rx-3; x <= rx+3; x++) {
                    for (int z = rz-3; z <= rz+3; z++) {
                        if ((x+z) % 2 == 0) block(es, x, SY+1, z, BlockTypes.MOSS_CARPET);
                    }
                }
            }
        }

        // ── Ruin 5 — Overgrown garden at (48,SY,82) ──────────────────────────
        {
            int rx = 48, rz = 82;
            double rfx = (double)rx/RX, rfz = (double)rz/RZ;
            if (rfx*rfx + rfz*rfz <= 0.94) {
                // MOSS_BLOCK floor and MOSS_CARPET on top
                fill(es, rx-5, SY,   rz-5, rx+5, SY,   rz+4, BlockTypes.MOSS_BLOCK);
                fill(es, rx-5, SY+1, rz-5, rx+5, SY+1, rz+4, BlockTypes.MOSS_CARPET);
                // 4 corner columns (2-tall CRACKED_STONE_BRICKS)
                int[][] r5corners = {{rx-5,rz-5},{rx+5,rz-5},{rx-5,rz+4},{rx+5,rz+4}};
                for (int[] cc : r5corners) {
                    column(es, cc[0], cc[1], SY+1, SY+2, BlockTypes.CRACKED_STONE_BRICKS);
                }
                // Plants
                block(es, rx-4, SY+2, rz-4, BlockTypes.AZALEA);
                block(es, rx+4, SY+2, rz-4, BlockTypes.AZALEA);
                block(es, rx-4, SY+2, rz+3, BlockTypes.FLOWERING_AZALEA);
                block(es, rx+4, SY+2, rz+3, BlockTypes.FLOWERING_AZALEA);
                block(es, rx-1, SY+2, rz-2, BlockTypes.RED_MUSHROOM);
                block(es, rx+2, SY+2, rz+2, BlockTypes.RED_MUSHROOM);
                block(es, rx,   SY+2, rz-3, BlockTypes.BROWN_MUSHROOM);
                block(es, rx+3, SY+2, rz+1, BlockTypes.BROWN_MUSHROOM);
                // STONE_BRICK_WALL remnant on N side
                fill(es, rx-5, SY+1, rz-5, rx+1, SY+2, rz-5, BlockTypes.STONE_BRICK_WALL);
            }
        }

        // ── Random zone debris scatter (15 blocks in SE quadrant) ─────────────
        int[][] zoneDebrisPos = {
            {50,28},{55,38},{60,48},{65,58},{70,68},
            {45,42},{52,55},{58,65},{63,75},{68,45},
            {73,35},{48,72},{56,32},{64,52},{72,62}
        };
        BlockType[] zoneDebrisMats = {
            BlockTypes.CRACKED_STONE_BRICKS, BlockTypes.MOSSY_COBBLESTONE,
            BlockTypes.COBBLESTONE, BlockTypes.CRACKED_STONE_BRICKS,
            BlockTypes.MOSSY_COBBLESTONE
        };
        for (int i = 0; i < zoneDebrisPos.length; i++) {
            int dbx = zoneDebrisPos[i][0], dbz = zoneDebrisPos[i][1];
            double dfx = (double)dbx/RX, dfz = (double)dbz/RZ;
            if (dfx*dfx + dfz*dfz > 0.94) continue;
            block(es, dbx, SY+1, dbz, zoneDebrisMats[i % zoneDebrisMats.length]);
        }

        // ── Zone trees ────────────────────────────────────────────────────────
        {
            double fx = (double)36/RX, fz = (double)22/RZ;
            if (fx*fx + fz*fz <= 0.88) buildOakTree(es, 36, 22, 5);
        }
        {
            double fx = (double)64/RX, fz = (double)88/RZ;
            if (fx*fx + fz*fz <= 0.88) buildOakTree(es, 64, 88, 6);
        }
        {
            double fx = (double)46/RX, fz = (double)60/RZ;
            if (fx*fx + fz*fz <= 0.88) buildDarkOakTree(es, 46, 60, 7);
        }
    }

    // ============================================================================
    //  SECTION — VEGETATION
    // ============================================================================

    /**
     * Scatters vegetation across the island: SHORT_GRASS and FERN clusters,
     * six themed flower patches, azalea transition lines, spore blossoms,
     * hanging roots, and dripleaf plants near water zones.
     */
    private void buildVegetation(EditSession es) {

        // ── SHORT_GRASS + FERN scatter across whole island ────────────────────
        for (int x = -85; x <= 85; x += 9) {
            for (int z = -100; z <= 100; z += 10) {
                // Skip near plaza
                if (Math.abs(x) < 22 && Math.abs(z) < 22) continue;
                // Skip near portals (within ±5 of portal positions)
                boolean nearPortal = false;
                int[][] portalCentres = {
                    {55,0},{-55,0},{0,65},{0,-65},{38,-45},{-38,-45},{38,55},{-38,55}
                };
                for (int[] pc : portalCentres) {
                    if (Math.abs(x-pc[0]) <= 5 && Math.abs(z-pc[1]) <= 5) {
                        nearPortal = true; break;
                    }
                }
                if (nearPortal) continue;
                double fx = (double)x/RX, fz = (double)z/RZ;
                if (fx*fx + fz*fz > 0.91) continue;
                block(es, x,   SY+1, z,   BlockTypes.SHORT_GRASS);
                block(es, x+1, SY+1, z,   BlockTypes.FERN);
                block(es, x,   SY+1, z+1, BlockTypes.SHORT_GRASS);
            }
        }

        // ── Flower patch 1 — OXEYE_DAISY, east corridor ───────────────────────
        int[][] flowers1 = {
            {35,-8},{38,-5},{41,-3},{44,0},{47,3},{40,6},{43,-7},{36,4}
        };
        for (int[] f : flowers1) {
            double fx = (double)f[0]/RX, fz = (double)f[1]/RZ;
            if (fx*fx + fz*fz <= 0.91) block(es, f[0], SY+1, f[1], BlockTypes.OXEYE_DAISY);
        }

        // ── Flower patch 2 — CORNFLOWER, west area ────────────────────────────
        int[][] flowers2 = {
            {-35,-6},{-38,-3},{-41,0},{-44,3},{-47,-5},{-40,5},{-43,-8},{-36,7}
        };
        for (int[] f : flowers2) {
            double fx = (double)f[0]/RX, fz = (double)f[1]/RZ;
            if (fx*fx + fz*fz <= 0.91) block(es, f[0], SY+1, f[1], BlockTypes.CORNFLOWER);
        }

        // ── Flower patch 3 — DANDELION + ALLIUM, south area ──────────────────
        int[][] flowers3 = {
            {-12,32},{-8,36},{-4,40},{0,44},{4,48},{8,52},{12,35},{-10,45},
            {6,38},{-6,50}
        };
        for (int i = 0; i < flowers3.length; i++) {
            int[] f = flowers3[i];
            double fx = (double)f[0]/RX, fz = (double)f[1]/RZ;
            if (fx*fx + fz*fz > 0.91) continue;
            block(es, f[0], SY+1, f[1], (i%2==0) ? BlockTypes.DANDELION : BlockTypes.ALLIUM);
        }

        // ── Flower patch 4 — BLUE_ORCHID + LILY_OF_THE_VALLEY, near water ────
        int[][] flowers4 = {
            {-32,14},{-36,18},{-40,22},{-44,26},{-48,20},{-34,24},{-38,16},{-42,28}
        };
        for (int i = 0; i < flowers4.length; i++) {
            int[] f = flowers4[i];
            double fx = (double)f[0]/RX, fz = (double)f[1]/RZ;
            if (fx*fx + fz*fz > 0.91) continue;
            block(es, f[0], SY+1, f[1], (i%2==0) ? BlockTypes.BLUE_ORCHID : BlockTypes.LILY_OF_THE_VALLEY);
        }

        // ── Flower patch 5 — POPPY + AZURE_BLUET, north area ─────────────────
        int[][] flowers5 = {
            {-12,-32},{-8,-36},{-4,-40},{0,-44},{4,-48},{8,-42},{12,-34},{-10,-48}
        };
        for (int i = 0; i < flowers5.length; i++) {
            int[] f = flowers5[i];
            double fx = (double)f[0]/RX, fz = (double)f[1]/RZ;
            if (fx*fx + fz*fz > 0.91) continue;
            block(es, f[0], SY+1, f[1], (i%2==0) ? BlockTypes.POPPY : BlockTypes.AZURE_BLUET);
        }

        // ── Flower patch 6 — mixed near ruins ─────────────────────────────────
        int[][] flowers6 = {
            {32,14},{38,20},{44,26},{50,22},{56,18},{42,28}
        };
        BlockType[] mixed6 = {
            BlockTypes.DANDELION, BlockTypes.POPPY,
            BlockTypes.ALLIUM, BlockTypes.CORNFLOWER,
            BlockTypes.AZURE_BLUET, BlockTypes.OXEYE_DAISY
        };
        for (int i = 0; i < flowers6.length; i++) {
            int[] f = flowers6[i];
            double fx = (double)f[0]/RX, fz = (double)f[1]/RZ;
            if (fx*fx + fz*fz <= 0.91) block(es, f[0], SY+1, f[1], mixed6[i]);
        }

        // ── Azalea transition line (10 pairs at biome boundaries) ─────────────
        int[][] azaleaLine = {
            {22,-22},{18,-26},{14,-30},{10,-34},{22,22},
            {18,26},{-22,-22},{-18,-26},{-14,-30},{-10,-34}
        };
        for (int i = 0; i < azaleaLine.length; i++) {
            int ax = azaleaLine[i][0], az = azaleaLine[i][1];
            double fx = (double)ax/RX, fz = (double)az/RZ;
            if (fx*fx + fz*fz > 0.91) continue;
            block(es, ax,   SY+1, az, BlockTypes.AZALEA);
            block(es, ax+1, SY+1, az, BlockTypes.FLOWERING_AZALEA);
        }

        // ── SPORE_BLOSSOM at 10 island underside positions ────────────────────
        int[][] sporPos = {
            {5,-10},{-5,10},{10,5},{-10,-5},{15,0},
            {-15,0},{0,15},{0,-15},{8,-8},{-8,8}
        };
        for (int[] sp : sporPos) {
            int spx = sp[0], spz = sp[1];
            double fx = (double)spx/RX, fz = (double)spz/RZ;
            double t = fx*fx + fz*fz;
            if (t > 0.85) continue;
            block(es, spx, SY-1, spz, BlockTypes.SPORE_BLOSSOM);
        }

        // ── HANGING_ROOTS at 12 underside positions ───────────────────────────
        int[][] hangPos2 = {
            {20,-30},{-20,30},{30,20},{-30,-20},{25,25},{-25,-25},
            {35,-15},{-35,15},{15,35},{-15,-35},{40,0},{-40,0}
        };
        for (int[] hp : hangPos2) {
            int hpx = hp[0], hpz = hp[1];
            double fx = (double)hpx/RX, fz = (double)hpz/RZ;
            double t = fx*fx + fz*fz;
            if (t > 0.80) continue;
            int botY = 40 + (int)(21.0 * t);
            block(es, hpx, botY+4, hpz, BlockTypes.HANGING_ROOTS);
            block(es, hpx, botY+5, hpz, BlockTypes.HANGING_ROOTS);
        }

        // ── BIG_DRIPLEAF at 10 positions near water zones ─────────────────────
        int[][] bigDrip = {
            {-32,42},{-34,48},{-38,54},{-42,60},{-46,66},
            {-50,70},{-54,52},{-44,40},{-48,46},{-56,58}
        };
        for (int[] bd : bigDrip) {
            double fx = (double)bd[0]/RX, fz = (double)bd[1]/RZ;
            if (fx*fx + fz*fz > 0.91) continue;
            block(es, bd[0], SY+1, bd[1], BlockTypes.BIG_DRIPLEAF);
        }

        // ── SMALL_DRIPLEAF at 6 positions near streams ────────────────────────
        int[][] smallDrip = {
            {-42,50},{-44,54},{-48,58},{-50,62},{-52,66},{-54,70}
        };
        for (int[] sd : smallDrip) {
            double fx = (double)sd[0]/RX, fz = (double)sd[1]/RZ;
            if (fx*fx + fz*fz > 0.91) continue;
            block(es, sd[0], SY, sd[1], BlockTypes.SMALL_DRIPLEAF);
        }
    }

    // ============================================================================
    //  SECTION 6 — FORTIFICATIONS
    // ============================================================================

    /**
     * Builds the outer perimeter wall following the island ellipse at radii
     * wRX=78, wRZ=93, with merlons, mossy accents, and cleared gaps for all
     * 8 portals and 4 ring-road crossings.
     */
    private void buildPerimeterWalls(EditSession es) {
        // ── Wall ring ──────────────────────────────────────────────────────────
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int wx = (int)(78 * Math.cos(rad));
            int wz = (int)(93 * Math.sin(rad));
            double ex = wx / 90.0, ez = wz / 107.0;
            if (ex*ex + ez*ez > 0.94) continue;
            block(es, wx, SY,   wz, BlockTypes.STONE_BRICKS);
            block(es, wx, SY+1, wz, BlockTypes.STONE_BRICKS);
            block(es, wx, SY+2, wz, BlockTypes.STONE_BRICKS);
            if (angle % 5 < 3) {
                block(es, wx, SY+3, wz, BlockTypes.STONE_BRICKS);
                block(es, wx, SY+4, wz, BlockTypes.STONE_BRICK_WALL);
            }
            if (angle % 9 == 0) {
                block(es, wx, SY+1, wz, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }

        // ── Clear wall gaps for portals ────────────────────────────────────────
        // Crystal (E): x≈77, z=0
        for (int x = 75; x <= 80; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Sword (W): x≈-77, z=0
        for (int x = -80; x <= -75; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Mace (S): z≈90, x=0
        for (int z = 88; z <= 93; z++) {
            for (int x = -4; x <= 4; x++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Bow (N): z≈-90, x=0
        for (int z = -93; z <= -88; z++) {
            for (int x = -4; x <= 4; x++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Totem (NE): ~(55,-65) — clear 5×5 around wall intersection
        for (int x = 53; x <= 57; x++) {
            for (int z = -67; z <= -63; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Axe (NW): ~(-55,-65)
        for (int x = -57; x <= -53; x++) {
            for (int z = -67; z <= -63; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Trident (SE): ~(55,72)
        for (int x = 53; x <= 57; x++) {
            for (int z = 70; z <= 74; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Shield (SW): ~(-55,72)
        for (int x = -57; x <= -53; x++) {
            for (int z = 70; z <= 74; z++) {
                for (int y = SY; y <= SY+4; y++) block(es, x, y, z, BlockTypes.AIR);
            }
        }
        // Ring road crossings (4 cardinal, 3-wide gaps at r≈28 on wall)
        // East crossing at x≈28, z=0
        for (int x = 27; x <= 29; x++) {
            for (int y = SY; y <= SY+4; y++) block(es, x, y, 0, BlockTypes.AIR);
        }
        // West crossing
        for (int x = -29; x <= -27; x++) {
            for (int y = SY; y <= SY+4; y++) block(es, x, y, 0, BlockTypes.AIR);
        }
        // South crossing
        for (int z = 27; z <= 29; z++) {
            for (int y = SY; y <= SY+4; y++) block(es, 0, y, z, BlockTypes.AIR);
        }
        // North crossing
        for (int z = -29; z <= -27; z++) {
            for (int y = SY; y <= SY+4; y++) block(es, 0, y, z, BlockTypes.AIR);
        }
    }

    /**
     * Places six watchtowers at strategic positions around the island, each
     * built by buildSingleWatchtower().
     */
    private void buildWatchtowers(EditSession es) {
        buildSingleWatchtower(es,  65,  50);
        buildSingleWatchtower(es, -65,  50);
        buildSingleWatchtower(es,  65, -60);
        buildSingleWatchtower(es, -65, -60);
        buildSingleWatchtower(es,   0,  95);
        buildSingleWatchtower(es,   0, -95);
    }

    /**
     * Constructs a single 7×7 watchtower with 20-block-tall walls, doorway,
     * interior features, observation platform, roof, and corner crenellations.
     */
    private void buildSingleWatchtower(EditSession es, int cx, int cz) {
        // Bounds check
        double efx = (double)cx/RX, efz = (double)cz/RZ;
        if (efx*efx + efz*efz > 0.92) return;

        // ── Walls: 7×7 hollow, SY..SY+20 ────────────────────────────────────
        for (int y = SY; y <= SY+20; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean isWall = (dx == -3 || dx == 3 || dz == -3 || dz == 3);
                    if (isWall) {
                        BlockType wmat = (y % 4 == 0) ? BlockTypes.CHISELED_STONE_BRICKS : BlockTypes.STONE_BRICKS;
                        block(es, cx+dx, y, cz+dz, wmat);
                    }
                }
            }
        }

        // ── Doorway on the inward face (facing origin) ────────────────────────
        // Determine which face is inward (facing origin)
        int doorX = cx, doorZ = cz;
        if (Math.abs(cx) >= Math.abs(cz)) {
            // Faces are on ±x; inward face is toward origin
            int faceX = (cx > 0) ? cx - 3 : cx + 3;
            for (int y = SY; y <= SY+3; y++) {
                for (int dz = -1; dz <= 1; dz++) {
                    block(es, faceX, y, cz+dz, BlockTypes.AIR);
                }
            }
        } else {
            int faceZ = (cz > 0) ? cz - 3 : cz + 3;
            for (int y = SY; y <= SY+3; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    block(es, cx+dx, y, faceZ, BlockTypes.AIR);
                }
            }
        }

        // ── Interior floor at SY+1 ────────────────────────────────────────────
        fill(es, cx-2, SY+1, cz-2, cx+2, SY+1, cz+2, BlockTypes.POLISHED_DEEPSLATE);
        // SEA_LANTERN at (cx, SY+2, cz)
        block(es, cx, SY+2, cz, BlockTypes.SEA_LANTERN);

        // ── Arrow slits at y=SY+8, SY+9 on each face centre ──────────────────
        block(es, cx,   SY+8, cz-3, BlockTypes.AIR);
        block(es, cx,   SY+9, cz-3, BlockTypes.AIR);
        block(es, cx,   SY+8, cz+3, BlockTypes.AIR);
        block(es, cx,   SY+9, cz+3, BlockTypes.AIR);
        block(es, cx-3, SY+8, cz,   BlockTypes.AIR);
        block(es, cx-3, SY+9, cz,   BlockTypes.AIR);
        block(es, cx+3, SY+8, cz,   BlockTypes.AIR);
        block(es, cx+3, SY+9, cz,   BlockTypes.AIR);

        // ── Upper platform at SY+20 ───────────────────────────────────────────
        fill(es, cx-3, SY+20, cz-3, cx+3, SY+20, cz+3, BlockTypes.POLISHED_DEEPSLATE);

        // ── IRON_BARS railing at SY+21 on perimeter ───────────────────────────
        for (int d = -3; d <= 3; d++) {
            block(es, cx+d, SY+21, cz-3, BlockTypes.IRON_BARS);
            block(es, cx+d, SY+21, cz+3, BlockTypes.IRON_BARS);
            block(es, cx-3, SY+21, cz+d, BlockTypes.IRON_BARS);
            block(es, cx+3, SY+21, cz+d, BlockTypes.IRON_BARS);
        }

        // ── Corner crenellations: column SY+21..SY+23, LANTERN at SY+24 ──────
        int[][] twCorners = {{cx-3,cz-3},{cx+3,cz-3},{cx-3,cz+3},{cx+3,cz+3}};
        for (int[] tc : twCorners) {
            column(es, tc[0], tc[1], SY+21, SY+23, BlockTypes.STONE_BRICKS);
            block(es, tc[0], SY+24, tc[1], BlockTypes.LANTERN);
        }

        // ── Roof ──────────────────────────────────────────────────────────────
        fill(es, cx-3, SY+22, cz-3, cx+3, SY+22, cz+3, BlockTypes.DARK_OAK_PLANKS);
        fill(es, cx-2, SY+23, cz-2, cx+2, SY+23, cz+2, BlockTypes.DARK_OAK_PLANKS);
        fill(es, cx-1, SY+24, cz-1, cx+1, SY+24, cz+1, BlockTypes.DARK_OAK_PLANKS);
        block(es, cx, SY+25, cz, BlockTypes.OAK_LOG);
        block(es, cx, SY+26, cz, BlockTypes.LANTERN);

        // ── Ladder rungs (OAK_FENCE) inside wall every 2y from SY+2..SY+18 ───
        for (int y = SY+2; y <= SY+18; y += 2) {
            block(es, cx-2, y, cz-2, BlockTypes.OAK_FENCE);
        }
    }

    // ============================================================================
    //  SECTION 7 — LIGHTING
    // ============================================================================

    /**
     * Places all lamp posts (OAK_FENCE column 3-tall + LANTERN on top) across
     * the island: portal paths, ring road, plaza ring, perimeter wall lanterns,
     * water garden lamps, arena perimeter, and soul lanterns flanking Mace portal.
     */
    private void buildLampPosts(EditSession es) {

        // ── Helper: place a lamp post at (lx,lz) ─────────────────────────────
        // (inline below for each group)

        // ── Portal path lamps (24 total, 3 per portal path) ──────────────────
        // Crystal path (east)
        int[][] crystalLamps = {{47,-2},{47,2},{52,-2},{52,2},{57,-2},{57,2}};
        for (int[] lp : crystalLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Sword path (west)
        int[][] swordLamps = {{-47,-2},{-47,2},{-52,-2},{-52,2},{-57,-2},{-57,2}};
        for (int[] lp : swordLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Mace path (south)
        int[][] maceLamps = {{-2,47},{2,47},{-2,55},{2,55},{-2,60},{2,60}};
        for (int[] lp : maceLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Bow path (north)
        int[][] bowLamps = {{-2,-47},{2,-47},{-2,-55},{2,-55},{-2,-60},{2,-60}};
        for (int[] lp : bowLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Totem path (NE diagonal)
        int[][] totemLamps = {{32,-38},{33,-39},{34,-40},{35,-41},{36,-42},{37,-43}};
        for (int[] lp : totemLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Axe path (NW diagonal)
        int[][] axeLamps = {{-32,-38},{-33,-39},{-34,-40},{-35,-41},{-36,-42},{-37,-43}};
        for (int[] lp : axeLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Trident path (SE diagonal)
        int[][] tridentLamps = {{32,48},{33,49},{34,50},{35,51},{36,52},{37,53}};
        for (int[] lp : tridentLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }
        // Shield path (SW diagonal)
        int[][] shieldLamps = {{-32,48},{-33,49},{-34,50},{-35,51},{-36,52},{-37,53}};
        for (int[] lp : shieldLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }

        // ── Ring-road lamps (16 total, every ~22.5°, r≈30/35) ─────────────────
        for (int ai = 0; ai < 16; ai++) {
            double rad = Math.toRadians(ai * 22.5);
            int lx = (int)(30 * Math.cos(rad));
            int lz = (int)(35 * Math.sin(rad));
            double fx = (double)lx/RX, fz = (double)lz/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lx, lz, SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lx, SY+4, lz, BlockTypes.LANTERN);
        }

        // ── Plaza ring lamps (8 total, r=24, every 45°) ──────────────────────
        for (int ai = 0; ai < 8; ai++) {
            double rad = Math.toRadians(22 + ai * 45.0);
            int lx = (int)(24 * Math.cos(rad));
            int lz = (int)(24 * Math.sin(rad));
            double fx = (double)lx/RX, fz = (double)lz/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lx, lz, SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lx, SY+4, lz, BlockTypes.LANTERN);
        }

        // ── Perimeter wall lanterns (20 total, every 18°) ─────────────────────
        for (int ai = 0; ai < 20; ai++) {
            double rad = Math.toRadians(9 + ai * 18.0);
            int wx = (int)(79 * Math.cos(rad));
            int wz = (int)(94 * Math.sin(rad));
            double fx = (double)wx/RX, fz = (double)wz/RZ;
            if (fx*fx + fz*fz > 0.97) continue;
            block(es, wx, SY+5, wz, BlockTypes.LANTERN);
        }

        // ── Water garden lamps (fence posts 3-tall) ───────────────────────────
        int[][] waterGardenLamps = {{-34,40},{-34,50},{-48,40},{-48,50}};
        for (int[] lp : waterGardenLamps) {
            double fx = (double)lp[0]/RX, fz = (double)lp[1]/RZ;
            if (fx*fx + fz*fz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }

        // ── Arena perimeter lamps (6, every 60° around arena at r=35 from (-50,-50)) ─
        for (int ai = 0; ai < 6; ai++) {
            double rad = Math.toRadians(ai * 60.0);
            int lx = -50 + (int)(35 * Math.cos(rad));
            int lz = -50 + (int)(35 * Math.sin(rad));
            double fx = (double)lx/RX, fz = (double)lz/RZ;
            if (fx*fx + fz*fz > 0.97) continue;
            column(es, lx, lz, SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lx, SY+4, lz, BlockTypes.LANTERN);
        }

        // ── Soul lanterns flanking Mace portal ────────────────────────────────
        block(es, -3, SY+1, 58, BlockTypes.SOUL_LANTERN);
        block(es,  3, SY+1, 58, BlockTypes.SOUL_LANTERN);
        block(es, -3, SY+1, 60, BlockTypes.SOUL_LANTERN);
        block(es,  3, SY+1, 60, BlockTypes.SOUL_LANTERN);
    }

    // ============================================================================
    //  SECTION 8 — UNDERGROUND CAVES
    // ============================================================================

    /**
     * Carves and decorates the main underground cave system, eastern cave,
     * an amethyst geode, and two disguised surface access shafts.
     */
    private void buildUndergroundCaves(EditSession es) {

        // ════════════════════════════════════════════════════════════════════════
        //  MAIN CAVE — centre (0, 51, 0)
        // ════════════════════════════════════════════════════════════════════════

        // Carve ellipsoidal cave volume
        for (int x = -22; x <= 22; x++) {
            for (int y = 44; y <= 58; y++) {
                for (int z = -22; z <= 22; z++) {
                    double dx = x / 22.0, dy = (y - 51) / 7.0, dz = z / 22.0;
                    if (dx*dx + dy*dy + dz*dz <= 1.0) {
                        block(es, x, y, z, BlockTypes.CAVE_AIR);
                    }
                }
            }
        }

        // Cave floor: gravel, stone, cobblestone spots
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                double dx = x / 20.0, dz = z / 20.0;
                if (dx*dx + dz*dz <= 1.0) {
                    block(es, x, 44, z, BlockTypes.GRAVEL);
                    block(es, x, 43, z, BlockTypes.STONE);
                    if ((x * 31 + z * 17) % 7 == 0) {
                        block(es, x, 44, z, BlockTypes.COBBLESTONE);
                    }
                }
            }
        }

        // Stalactites (18 hardcoded positions)
        int[][] stalPos = {
            {0,0,8},{6,2,6},{-6,-2,7},{8,-6,5},{-8,6,6},{4,10,5},
            {-4,-10,7},{10,4,4},{-10,-4,5},{12,-8,4},{-12,8,5},
            {2,-14,6},{-2,14,5},{14,2,4},{-14,-2,4},{8,14,3},
            {-8,-14,3},{16,0,3}
        };
        for (int[] s : stalPos) {
            int scx = s[0], scz = s[1], slen = s[2];
            double sdx = scx/22.0, sdz = scz/22.0;
            if (sdx*sdx + sdz*sdz > 0.80) continue;
            for (int i = 0; i < slen; i++) {
                int sy2 = 58 - i;
                int spread = Math.max(0, (slen - i - 1) / 2);
                fill(es, scx-spread, sy2, scz-spread, scx+spread, sy2, scz+spread, BlockTypes.DEEPSLATE);
            }
        }

        // Stalagmites (10 hardcoded positions)
        int[][] stalagPos = {
            {5,8},{-5,-8},{9,-5},{-9,5},{3,-12},
            {-3,12},{12,3},{-12,-3},{0,15},{15,0}
        };
        for (int[] sg : stalagPos) {
            int sgx = sg[0], sgz = sg[1];
            double sdx = sgx/22.0, sdz = sgz/22.0;
            if (sdx*sdx + sdz*sdz > 0.80) continue;
            int sgh = 1 + ((Math.abs(sgx) + Math.abs(sgz)) % 3);
            for (int i = 0; i < sgh; i++) {
                block(es, sgx, 44+i, sgz, BlockTypes.STONE);
            }
        }

        // Ore veins (10 positions)
        BlockType[] oreTypes = {
            BlockTypes.DEEPSLATE_IRON_ORE, BlockTypes.DEEPSLATE_GOLD_ORE,
            BlockTypes.DEEPSLATE_COAL_ORE, BlockTypes.DEEPSLATE_LAPIS_ORE,
            BlockTypes.DEEPSLATE_DIAMOND_ORE, BlockTypes.DEEPSLATE_EMERALD_ORE,
            BlockTypes.DEEPSLATE_IRON_ORE, BlockTypes.DEEPSLATE_GOLD_ORE,
            BlockTypes.DEEPSLATE_COAL_ORE, BlockTypes.DEEPSLATE_LAPIS_ORE
        };
        int[][] orePos = {
            {-20,50,6},{-20,52,-5},{20,50,5},{20,52,-8},{0,55,20},
            {0,55,-20},{-15,48,15},{15,48,-15},{-10,46,-18},{10,46,18}
        };
        for (int i = 0; i < orePos.length; i++) {
            int ox = orePos[i][0], oy = orePos[i][1], oz = orePos[i][2];
            fill(es, ox-1, oy, oz-1, ox+1, oy, oz+1, oreTypes[i]);
        }

        // Cave lighting
        block(es,  0, 57,  0, BlockTypes.GLOWSTONE);
        block(es,  8, 57,  8, BlockTypes.SHROOMLIGHT);
        block(es, -8, 57, -8, BlockTypes.SHROOMLIGHT);
        block(es, 10, 57, -6, BlockTypes.GLOWSTONE);
        block(es,-10, 57,  6, BlockTypes.GLOWSTONE);
        block(es,  5, 44, -8, BlockTypes.SEA_LANTERN);
        block(es, -5, 44,  9, BlockTypes.SEA_LANTERN);
        block(es,  0, 44,  0, BlockTypes.GLOWSTONE);

        // Underground pool at (-14,44,-12)
        fill(es, -14, 44, -12, -8, 44, -7, BlockTypes.WATER);
        fill(es, -14, 43, -12, -8, 43, -7, BlockTypes.DARK_PRISMARINE);
        block(es, -11, 43, -9, BlockTypes.SEA_LANTERN);

        // Mushroom patch
        fill(es, 8, 44, 8, 14, 44, 14, BlockTypes.MYCELIUM);
        block(es,  9, 45,  9, BlockTypes.BROWN_MUSHROOM);
        block(es, 11, 45, 11, BlockTypes.BROWN_MUSHROOM);
        block(es, 13, 45, 13, BlockTypes.BROWN_MUSHROOM);
        block(es, 10, 45, 12, BlockTypes.BROWN_MUSHROOM);
        block(es, 12, 45, 10, BlockTypes.BROWN_MUSHROOM);
        block(es,  9, 45, 13, BlockTypes.BROWN_MUSHROOM);

        // SCULK patches on walls at 6 positions
        int[][] sculkPos = {
            {-22,50,8},{-22,52,-5},{22,50,5},{22,52,-8},{0,55,22},{0,55,-22}
        };
        for (int[] sp : sculkPos) {
            block(es, sp[0], sp[1], sp[2], BlockTypes.SCULK);
        }

        // ════════════════════════════════════════════════════════════════════════
        //  EASTERN CAVE — centre (52, 50, 0)
        // ════════════════════════════════════════════════════════════════════════

        // Carve
        for (int x = 40; x <= 64; x++) {
            for (int y = 46; y <= 55; y++) {
                for (int z = -12; z <= 12; z++) {
                    double dx = (x-52)/12.0, dy = (y-50)/5.0, dz = z/12.0;
                    if (dx*dx + dy*dy + dz*dz <= 1.0) {
                        block(es, x, y, z, BlockTypes.CAVE_AIR);
                    }
                }
            }
        }
        // Floor: GRAVEL at y=46
        for (int x = 40; x <= 64; x++) {
            for (int z = -12; z <= 12; z++) {
                double dx = (x-52)/12.0, dz = z/12.0;
                if (dx*dx + dz*dz <= 1.0) {
                    block(es, x, 46, z, BlockTypes.GRAVEL);
                }
            }
        }
        // 8 stalactites in east cave (hardcoded)
        int[][] eStalPos = {
            {44,46,4},{48,-4,3},{52,6,4},{56,-6,3},{60,4,3},{44,-4,3},{52,-8,4},{58,8,3}
        };
        for (int[] es2 : eStalPos) {
            int scx = es2[0], scz = es2[1], slen = es2[2];
            for (int i = 0; i < slen; i++) {
                int sy2 = 55 - i;
                int spread = Math.max(0, (slen - i - 1) / 2);
                fill(es, scx-spread, sy2, scz-spread, scx+spread, sy2, scz+spread, BlockTypes.DEEPSLATE);
            }
        }
        // 5 stalagmites
        int[][] eStalagPos = {{46,4},{50,-6},{54,8},{58,-4},{62,2}};
        for (int[] sg : eStalagPos) {
            int sgx = sg[0], sgz = sg[1];
            int sgh = 1 + ((Math.abs(sgx) + Math.abs(sgz)) % 3);
            for (int i = 0; i < sgh; i++) {
                block(es, sgx, 46+i, sgz, BlockTypes.STONE);
            }
        }
        // 4 ore veins
        block(es, 42, 50, 5, BlockTypes.DEEPSLATE_IRON_ORE);
        block(es, 48, 52,-6, BlockTypes.DEEPSLATE_GOLD_ORE);
        block(es, 56, 50, 7, BlockTypes.DEEPSLATE_COAL_ORE);
        block(es, 62, 52,-5, BlockTypes.DEEPSLATE_DIAMOND_ORE);
        // Lighting
        block(es, 52, 55, 0, BlockTypes.GLOWSTONE);
        block(es, 44, 55, 4, BlockTypes.SEA_LANTERN);
        block(es, 60, 55,-4, BlockTypes.GLOWSTONE);
        block(es, 52, 46, 0, BlockTypes.SEA_LANTERN);
        // Pool
        fill(es, 58, 46, -2, 62, 46, 2, BlockTypes.WATER);

        // ════════════════════════════════════════════════════════════════════════
        //  AMETHYST GEODE at (45, 52, 5)
        // ════════════════════════════════════════════════════════════════════════
        sphere(es, 45, 52, 5, 4, BlockTypes.DEEPSLATE);
        sphere(es, 45, 52, 5, 3, BlockTypes.AMETHYST_BLOCK);
        block(es, 45, 52, 5, BlockTypes.BUDDING_AMETHYST);
        block(es, 46, 52, 5, BlockTypes.AMETHYST_CLUSTER);
        block(es, 44, 52, 5, BlockTypes.AMETHYST_CLUSTER);
        block(es, 45, 53, 5, BlockTypes.AMETHYST_CLUSTER);
        block(es, 45, 51, 5, BlockTypes.AMETHYST_CLUSTER);

        // ════════════════════════════════════════════════════════════════════════
        //  ACCESS SHAFT 1 at (22,_,6)
        // ════════════════════════════════════════════════════════════════════════
        // Clear shaft
        for (int y = 58; y <= SY-1; y++) {
            block(es, 22, y, 6, BlockTypes.CAVE_AIR);
            block(es, 23, y, 6, BlockTypes.CAVE_AIR);
            block(es, 22, y, 7, BlockTypes.CAVE_AIR);
            block(es, 23, y, 7, BlockTypes.CAVE_AIR);
        }
        // Stone-bricks lining
        for (int y = 58; y <= SY-1; y++) {
            block(es, 21, y, 6, BlockTypes.STONE_BRICKS);
            block(es, 24, y, 6, BlockTypes.STONE_BRICKS);
            block(es, 22, y, 5, BlockTypes.STONE_BRICKS);
            block(es, 22, y, 8, BlockTypes.STONE_BRICKS);
            block(es, 23, y, 5, BlockTypes.STONE_BRICKS);
            block(es, 23, y, 8, BlockTypes.STONE_BRICKS);
            block(es, 21, y, 7, BlockTypes.STONE_BRICKS);
            block(es, 24, y, 7, BlockTypes.STONE_BRICKS);
        }
        // Ladder rungs (OAK_FENCE) every 2y
        for (int y = 59; y <= SY-2; y += 2) {
            block(es, 22, y, 6, BlockTypes.OAK_FENCE);
        }
        // Top
        block(es, 22, SY+1, 6, BlockTypes.IRON_TRAPDOOR);
        block(es, 23, SY+1, 6, BlockTypes.OAK_PLANKS);
        // Moss disguise
        fill(es, 20, SY, 4, 25, SY, 9, BlockTypes.MOSS_BLOCK);
        block(es, 22, SY, 6, BlockTypes.AIR);
        block(es, 23, SY, 6, BlockTypes.AIR);
        block(es, 22, SY, 7, BlockTypes.AIR);
        block(es, 23, SY, 7, BlockTypes.AIR);

        // ════════════════════════════════════════════════════════════════════════
        //  ACCESS SHAFT 2 at (-20,_,-8)
        // ════════════════════════════════════════════════════════════════════════
        for (int y = 58; y <= SY-1; y++) {
            block(es, -20, y, -8, BlockTypes.CAVE_AIR);
            block(es, -21, y, -8, BlockTypes.CAVE_AIR);
            block(es, -20, y, -9, BlockTypes.CAVE_AIR);
            block(es, -21, y, -9, BlockTypes.CAVE_AIR);
        }
        for (int y = 58; y <= SY-1; y++) {
            block(es, -19, y, -8,  BlockTypes.STONE_BRICKS);
            block(es, -22, y, -8,  BlockTypes.STONE_BRICKS);
            block(es, -20, y, -7,  BlockTypes.STONE_BRICKS);
            block(es, -20, y, -10, BlockTypes.STONE_BRICKS);
            block(es, -21, y, -7,  BlockTypes.STONE_BRICKS);
            block(es, -21, y, -10, BlockTypes.STONE_BRICKS);
            block(es, -19, y, -9,  BlockTypes.STONE_BRICKS);
            block(es, -22, y, -9,  BlockTypes.STONE_BRICKS);
        }
        for (int y = 59; y <= SY-2; y += 2) {
            block(es, -20, y, -8, BlockTypes.OAK_FENCE);
        }
        block(es, -20, SY+1, -8, BlockTypes.OAK_TRAPDOOR);
        block(es, -21, SY+1, -8, BlockTypes.OAK_PLANKS);
        // Moss disguise (slightly different shape)
        fill(es, -22, SY, -11, -18, SY, -6, BlockTypes.MOSS_BLOCK);
        block(es, -20, SY, -8, BlockTypes.AIR);
        block(es, -21, SY, -8, BlockTypes.AIR);
        block(es, -20, SY, -9, BlockTypes.AIR);
        block(es, -21, SY, -9, BlockTypes.AIR);
    }

    // ============================================================================
    //  SECTION 9 — FINAL ACCENTS + EASTER EGGS
    // ============================================================================

    /**
     * Places final decorative accents across the island and embeds all 12 easter
     * eggs.  This method is intentionally long to achieve maximum detail density.
     */
    private void buildFinalAccents(EditSession es) {

        // ── Decorative boulders (20 clusters) ─────────────────────────────────
        int[][] boulderPos = {
            {-25,-85},{60,25},{-70,30},{45,-70},{80,-40},
            {-45,78},{30,85},{-80,-55},{72,65},{-30,-75},
            {55,55},{-60,-85},{82,10},{-82,-10},{25,78},
            {-25,-78},{70,-65},{-70,65},{50,-30},{-50,30}
        };
        BlockType[] boulderMats = {
            BlockTypes.ANDESITE, BlockTypes.MOSSY_COBBLESTONE,
            BlockTypes.STONE,    BlockTypes.GRANITE,
            BlockTypes.POLISHED_ANDESITE
        };
        for (int i = 0; i < boulderPos.length; i++) {
            int bx = boulderPos[i][0], bz = boulderPos[i][1];
            double bfx = bx/90.0, bfz = bz/107.0;
            if (bfx*bfx + bfz*bfz > 0.87) continue;
            block(es, bx,   SY+1, bz,   boulderMats[ i    % boulderMats.length]);
            block(es, bx+1, SY+1, bz,   boulderMats[(i+1) % boulderMats.length]);
            block(es, bx,   SY+1, bz+1, boulderMats[(i+2) % boulderMats.length]);
            block(es, bx,   SY+2, bz,   boulderMats[(i+3) % boulderMats.length]);
        }

        // ── Amethyst geode cluster near Crystal portal (x=48..58, z=-6..6) ───
        int[][] amethystAccent = {
            {50,-4},{52,-2},{54,0},{56,2},{50,2},{52,-4},{54,4},{56,-2}
        };
        for (int[] ap : amethystAccent) {
            block(es, ap[0], SY+2, ap[1], BlockTypes.AMETHYST_BLOCK);
        }
        block(es, 51, SY+2, -3, BlockTypes.BUDDING_AMETHYST);
        block(es, 53, SY+2,  1, BlockTypes.BUDDING_AMETHYST);
        block(es, 55, SY+2, -1, BlockTypes.BUDDING_AMETHYST);
        block(es, 57, SY+2,  3, BlockTypes.BUDDING_AMETHYST);
        block(es, 50, SY+3, -2, BlockTypes.AMETHYST_CLUSTER);
        block(es, 52, SY+3,  0, BlockTypes.AMETHYST_CLUSTER);
        block(es, 54, SY+3,  2, BlockTypes.AMETHYST_CLUSTER);
        block(es, 56, SY+3, -4, BlockTypes.AMETHYST_CLUSTER);
        block(es, 53, SY+3,  3, BlockTypes.AMETHYST_CLUSTER);
        block(es, 55, SY+3, -3, BlockTypes.AMETHYST_CLUSTER);

        // ── Nether accent near Mace portal ────────────────────────────────────
        int[][] netherAccent = {
            {-6,57},{-4,58},{-6,60},{-4,62},{-6,64},{-4,66},{-6,68},{-4,56},
            {6,57},{4,58}
        };
        for (int[] na : netherAccent) {
            double nfx = (double)na[0]/RX, nfz = (double)na[1]/RZ;
            if (nfx*nfx + nfz*nfz > 0.94) continue;
            block(es, na[0], SY+1, na[1], BlockTypes.NETHER_WART_BLOCK);
        }
        // SOUL_SAND at 4 positions
        block(es, -7, SY, 59, BlockTypes.SOUL_SAND);
        block(es,  7, SY, 59, BlockTypes.SOUL_SAND);
        block(es, -7, SY, 63, BlockTypes.SOUL_SAND);
        block(es,  7, SY, 63, BlockTypes.SOUL_SAND);

        // ── Copper theme near Sword portal (x=-48..-62, z=-8..8) ─────────────
        int[][] copperAccent = {
            {-50,-6},{-52,-4},{-54,-2},{-56,0},{-58,2},{-60,4},{-48,4},{-50,6},
            {-54,6},{-58,-6}
        };
        BlockType[] copperMats = {
            BlockTypes.CUT_COPPER, BlockTypes.WEATHERED_CUT_COPPER,
            BlockTypes.OXIDIZED_CUT_COPPER, BlockTypes.COPPER_BLOCK,
            BlockTypes.WEATHERED_COPPER, BlockTypes.OXIDIZED_COPPER
        };
        for (int i = 0; i < copperAccent.length; i++) {
            double cfx = (double)copperAccent[i][0]/RX, cfz = (double)copperAccent[i][1]/RZ;
            if (cfx*cfx + cfz*cfz > 0.94) continue;
            block(es, copperAccent[i][0], SY+1, copperAccent[i][1], copperMats[i % copperMats.length]);
        }

        // ── Ocean accent near Trident portal (x=30..46, z=48..64) ───────────
        int[][] oceanAccent = {
            {32,50},{34,52},{36,54},{38,56},{40,58},{42,60},{44,62},{30,56},
            {32,60},{36,48}
        };
        BlockType[] oceanMats = {
            BlockTypes.PRISMARINE, BlockTypes.DARK_PRISMARINE,
            BlockTypes.PRISMARINE_BRICKS, BlockTypes.PRISMARINE
        };
        for (int i = 0; i < oceanAccent.length; i++) {
            double ofx = (double)oceanAccent[i][0]/RX, ofz = (double)oceanAccent[i][1]/RZ;
            if (ofx*ofx + ofz*ofz > 0.94) continue;
            block(es, oceanAccent[i][0], SY+1, oceanAccent[i][1], oceanMats[i % oceanMats.length]);
        }

        // ── Lore pillars (8 positions) ────────────────────────────────────────
        int[][] lorePillarPos = {
            {28,0},{-28,0},{0,30},{0,-30},{20,20},{-20,20},{20,-20},{-20,-20}
        };
        for (int[] lp : lorePillarPos) {
            double lfx = (double)lp[0]/RX, lfz = (double)lp[1]/RZ;
            if (lfx*lfx + lfz*lfz > 0.94) continue;
            column(es, lp[0], lp[1], SY+1, SY+5, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, lp[0], SY+6, lp[1], BlockTypes.SEA_LANTERN);
            block(es, lp[0], SY+7, lp[1], BlockTypes.CHAIN);
            block(es, lp[0], SY+8, lp[1], BlockTypes.LANTERN);
        }

        // ── Edge flowers (20 positions near island perimeter) ─────────────────
        int[][] edgeFlowers = {
            {85,0},{-85,0},{0,100},{0,-100},{60,75},{-60,75},{60,-75},{-60,-75},
            {80,45},{-80,45},{80,-45},{-80,-45},{45,95},{-45,95},{45,-95},{-45,-95},
            {70,65},{-70,65},{70,-65},{-70,-65}
        };
        BlockType[] flowerMix = {
            BlockTypes.POPPY, BlockTypes.BLUE_ORCHID,
            BlockTypes.LILY_OF_THE_VALLEY, BlockTypes.CORNFLOWER,
            BlockTypes.DANDELION, BlockTypes.ALLIUM
        };
        for (int i = 0; i < edgeFlowers.length; i++) {
            int efx2 = edgeFlowers[i][0], efz2 = edgeFlowers[i][1];
            double ex2 = (double)efx2/RX, ez2 = (double)efz2/RZ;
            if (ex2*ex2 + ez2*ez2 > 0.94) continue;
            block(es, efx2, SY+1, efz2, flowerMix[i % flowerMix.length]);
        }

        // ── Glowstone seam (12 positions near cave) ───────────────────────────
        int[][] glowSeam = {
            {6,4},{-6,-4},{4,-6},{-4,6},{0,12},{12,0},
            {-12,0},{0,-12},{8,-8},{-8,8},{10,5},{-10,-5}
        };
        for (int[] gs : glowSeam) {
            int gsx = gs[0], gsz = gs[1];
            for (int y = 47; y <= 53; y += 3) {
                if ((gsx + gsz + y) % 6 == 0) {
                    block(es, gsx, y, gsz, BlockTypes.GLOWSTONE);
                }
            }
        }

        // ── Hidden beacon under tower ─────────────────────────────────────────
        fill(es, -1, SY-3, -1, 1, SY-3, 1, BlockTypes.IRON_BLOCK);
        block(es, 0, SY-2, 0, BlockTypes.BEACON);

        // ── Sea lanterns on perimeter wall at 4 cardinal tops ─────────────────
        block(es,  78, SY+5,  0, BlockTypes.SEA_LANTERN);
        block(es, -78, SY+5,  0, BlockTypes.SEA_LANTERN);
        block(es,   0, SY+5, 90, BlockTypes.SEA_LANTERN);
        block(es,   0, SY+5,-90, BlockTypes.SEA_LANTERN);

        // ── Tuff accent spots (15 positions near island edges) ────────────────
        int[][] tuffPos = {
            {82,10},{82,-10},{-82,10},{-82,-10},{0,104},{0,-104},
            {60,85},{-60,85},{60,-85},{-60,-85},{85,35},{-85,35},
            {85,-35},{-85,-35},{0,95}
        };
        for (int[] tp : tuffPos) {
            double tfx = (double)tp[0]/RX, tfz = (double)tp[1]/RZ;
            if (tfx*tfx + tfz*tfz > 0.97) continue;
            block(es, tp[0], SY+1, tp[1], BlockTypes.TUFF);
        }

        // ── Cracked stone on perimeter wall (10 positions) ────────────────────
        int[][] crackedWallPos = {
            {76,14},{76,-14},{-76,14},{-76,-14},
            {14,92},{-14,92},{14,-92},{-14,-92},
            {72,42},{-72,42}
        };
        for (int[] cw : crackedWallPos) {
            double cwx = (double)cw[0]/RX, cwz = (double)cw[1]/RZ;
            if (cwx*cwx + cwz*cwz > 0.97) continue;
            block(es, cw[0], SY+2, cw[1], BlockTypes.CRACKED_STONE_BRICKS);
        }

        // ── Hanging tower lanterns ────────────────────────────────────────────
        block(es, 0, SY+45, 0, BlockTypes.CHAIN);
        block(es, 0, SY+44, 0, BlockTypes.LANTERN);
        block(es,  5, SY+30,  5, BlockTypes.CHAIN);
        block(es,  5, SY+29,  5, BlockTypes.LANTERN);
        block(es, -5, SY+30,  5, BlockTypes.CHAIN);
        block(es, -5, SY+29,  5, BlockTypes.LANTERN);
        block(es,  5, SY+30, -5, BlockTypes.CHAIN);
        block(es,  5, SY+29, -5, BlockTypes.LANTERN);
        block(es, -5, SY+30, -5, BlockTypes.CHAIN);
        block(es, -5, SY+29, -5, BlockTypes.LANTERN);

        // ════════════════════════════════════════════════════════════════════════
        //  EASTER EGGS
        // ════════════════════════════════════════════════════════════════════════

        // ── Easter Egg #1: Pixel Duck — hidden near NW perimeter ──────────────
        // 3×4 GLOWSTONE (yellow) / MAGMA_BLOCK (orange) pixel-art duck face at SY+1
        {
            int ex1 = -82, ez1 = -3;
            double e1x = (double)ex1/RX, e1z = (double)ez1/RZ;
            if (e1x*e1x + e1z*e1z <= 0.97) {
                // Row 0 (duck face top — yellow outline)
                block(es, ex1,   SY+1, ez1,   BlockTypes.GLOWSTONE);
                block(es, ex1+1, SY+1, ez1,   BlockTypes.GLOWSTONE);
                block(es, ex1+2, SY+1, ez1,   BlockTypes.GLOWSTONE);
                // Row 1 (eyes with orange pupils)
                block(es, ex1,   SY+1, ez1+1, BlockTypes.MAGMA_BLOCK);
                block(es, ex1+1, SY+1, ez1+1, BlockTypes.GLOWSTONE);
                block(es, ex1+2, SY+1, ez1+1, BlockTypes.MAGMA_BLOCK);
                // Row 2 (beak — orange centre)
                block(es, ex1,   SY+1, ez1+2, BlockTypes.GLOWSTONE);
                block(es, ex1+1, SY+1, ez1+2, BlockTypes.MAGMA_BLOCK);
                block(es, ex1+2, SY+1, ez1+2, BlockTypes.GLOWSTONE);
                // Row 3 (chin — yellow bottom)
                block(es, ex1,   SY+1, ez1+3, BlockTypes.GLOWSTONE);
                block(es, ex1+1, SY+1, ez1+3, BlockTypes.GLOWSTONE);
                block(es, ex1+2, SY+1, ez1+3, BlockTypes.GLOWSTONE);
            }
        }

        // ── Easter Egg #2: Secret Nerd Room — accessible from main cave ───────
        {
            // Carve 5×5×4 room at (-8, SY-4, -8) (i.e. around y=46..43 area below surface)
            fill(es, -10, 42, -10, -6, 45, -6, BlockTypes.CAVE_AIR);
            // Line walls with BOOKSHELF
            for (int y = 42; y <= 45; y++) {
                for (int x = -10; x <= -6; x++) {
                    for (int z = -10; z <= -6; z++) {
                        boolean isWall = (x == -10 || x == -6 || z == -10 || z == -6 || y == 45);
                        if (isWall) block(es, x, y, z, BlockTypes.BOOKSHELF);
                    }
                }
            }
            // Floor: OBSIDIAN
            fill(es, -9, 42, -9, -7, 42, -7, BlockTypes.OBSIDIAN);
            // Ceiling: 2×2 GLOWSTONE
            block(es, -8, 45, -8, BlockTypes.GLOWSTONE);
            block(es, -7, 45, -8, BlockTypes.GLOWSTONE);
            block(es, -8, 45, -7, BlockTypes.GLOWSTONE);
            block(es, -7, 45, -7, BlockTypes.GLOWSTONE);
            // Furniture
            block(es, -9, 43, -9, BlockTypes.CRAFTING_TABLE);
            block(es, -7, 43, -9, BlockTypes.CAULDRON);
            block(es, -9, 43, -7, BlockTypes.CHEST);
            block(es, -7, 43, -7, BlockTypes.BARREL);
            // Access tunnel from main cave at (-8,46,-8) going south 2 blocks
            block(es, -8, 46, -8, BlockTypes.CAVE_AIR);
            block(es, -8, 45, -8, BlockTypes.CAVE_AIR);
        }

        // ── Easter Egg #3: LemonPvP Heart — visible from above in SW garden ───
        {
            int ax = 0, az = 80;
            // Row 0 (az=80): x=-4,-3,-1,0,1,3,4
            for (int hx : new int[]{-4,-3,-1,0,1,3,4}) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az, BlockTypes.MAGMA_BLOCK);
            }
            // Row 1 (az=81): x=-5..-5..5
            for (int hx = -5; hx <= 5; hx++) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az+1)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az+1, BlockTypes.MAGMA_BLOCK);
            }
            // Row 2 (az=82)
            for (int hx = -5; hx <= 5; hx++) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az+2)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az+2, BlockTypes.MAGMA_BLOCK);
            }
            // Row 3 (az=83): x=-4..4
            for (int hx = -4; hx <= 4; hx++) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az+3)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az+3, BlockTypes.MAGMA_BLOCK);
            }
            // Row 4 (az=84): x=-3..3
            for (int hx = -3; hx <= 3; hx++) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az+4)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az+4, BlockTypes.MAGMA_BLOCK);
            }
            // Row 5 (az=85): x=-2..2
            for (int hx = -2; hx <= 2; hx++) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az+5)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az+5, BlockTypes.MAGMA_BLOCK);
            }
            // Row 6 (az=86): x=-1..1
            for (int hx = -1; hx <= 1; hx++) {
                double hfx = (double)(ax+hx)/RX, hfz = (double)(az+6)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hx, SY+1, az+6, BlockTypes.MAGMA_BLOCK);
            }
            // Row 7 (az=87): x=0
            {
                double hfx = (double)ax/RX, hfz = (double)(az+7)/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax, SY+1, az+7, BlockTypes.MAGMA_BLOCK);
            }
            // NETHER_WART_BLOCK border 1 block outside all magma positions
            int[][] heartBorderOffsets = {
                {-5,80},{5,80},{-6,81},{6,81},{-6,82},{6,82},
                {-5,83},{5,83},{-4,84},{4,84},{-3,85},{3,85},
                {-2,86},{2,86},{-1,87},{1,87},{0,88}
            };
            for (int[] hb : heartBorderOffsets) {
                double hfx = (double)(ax+hb[0])/RX, hfz = (double)hb[1]/RZ;
                if (hfx*hfx + hfz*hfz <= 0.97) block(es, ax+hb[0], SY+1, hb[1], BlockTypes.NETHER_WART_BLOCK);
            }
        }

        // ── Easter Egg #4: Void Mirror — deepest secret ───────────────────────
        fill(es, -3, 25, -3, 3, 25, 3, BlockTypes.OBSIDIAN);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    block(es, x, 25, z, BlockTypes.CRYING_OBSIDIAN);
                }
            }
        }

        // ── Easter Egg #5: Tiny Villager Village — hidden in SE ruins ─────────
        {
            // House 1 at (75,_,52)
            double h1fx = 75.0/RX, h1fz = 52.0/RZ;
            if (h1fx*h1fx + h1fz*h1fz <= 0.97) {
                // 3×3 walls SY..SY+2
                for (int y = SY; y <= SY+2; y++) {
                    for (int x = 74; x <= 76; x++) {
                        for (int z = 51; z <= 53; z++) {
                            if (x==74||x==76||z==51||z==53) block(es, x, y, z, BlockTypes.STONE_BRICKS);
                        }
                    }
                }
                // Door gap on south face
                block(es, 75, SY, 53, BlockTypes.AIR);
                block(es, 75, SY+1, 53, BlockTypes.AIR);
                // Roof
                fill(es, 74, SY+3, 51, 76, SY+3, 53, BlockTypes.DARK_OAK_PLANKS);
            }
            // House 2 at (80,_,52)
            double h2fx = 80.0/RX, h2fz = 52.0/RZ;
            if (h2fx*h2fx + h2fz*h2fz <= 0.97) {
                for (int y = SY; y <= SY+2; y++) {
                    for (int x = 79; x <= 81; x++) {
                        for (int z = 51; z <= 53; z++) {
                            if (x==79||x==81||z==51||z==53) block(es, x, y, z, BlockTypes.STONE_BRICKS);
                        }
                    }
                }
                block(es, 80, SY, 53, BlockTypes.AIR);
                block(es, 80, SY+1, 53, BlockTypes.AIR);
                fill(es, 79, SY+3, 51, 81, SY+3, 53, BlockTypes.DARK_OAK_PLANKS);
            }
            // House 3 at (78,_,57)
            double h3fx = 78.0/RX, h3fz = 57.0/RZ;
            if (h3fx*h3fx + h3fz*h3fz <= 0.97) {
                for (int y = SY; y <= SY+2; y++) {
                    for (int x = 77; x <= 79; x++) {
                        for (int z = 56; z <= 58; z++) {
                            if (x==77||x==79||z==56||z==58) block(es, x, y, z, BlockTypes.STONE_BRICKS);
                        }
                    }
                }
                block(es, 78, SY, 58, BlockTypes.AIR);
                block(es, 78, SY+1, 58, BlockTypes.AIR);
                fill(es, 77, SY+3, 56, 79, SY+3, 58, BlockTypes.DARK_OAK_PLANKS);
                // Tiny well at (76,_,57)
                double wfx = 76.0/RX, wfz = 57.0/RZ;
                if (wfx*wfx + wfz*wfz <= 0.97) {
                    block(es, 75, SY+1, 56, BlockTypes.STONE_BRICK_WALL);
                    block(es, 77, SY+1, 56, BlockTypes.STONE_BRICK_WALL);
                    block(es, 75, SY+1, 58, BlockTypes.STONE_BRICK_WALL);
                    block(es, 77, SY+1, 58, BlockTypes.STONE_BRICK_WALL);
                    block(es, 76, SY,   57, BlockTypes.WATER);
                }
            }
            // OAK_FENCE perimeter around village
            for (int x = 73; x <= 83; x++) {
                double pfx = (double)x/RX, pfz = 50.0/RZ;
                if (pfx*pfx + pfz*pfz <= 0.97) block(es, x, SY+1, 50, BlockTypes.OAK_FENCE);
                pfz = 60.0/RZ;
                if (pfx*pfx + pfz*pfz <= 0.97) block(es, x, SY+1, 60, BlockTypes.OAK_FENCE);
            }
            for (int z = 51; z <= 59; z++) {
                double pfx = 73.0/RX, pfz = (double)z/RZ;
                if (pfx*pfx + pfz*pfz <= 0.97) block(es, 73, SY+1, z, BlockTypes.OAK_FENCE);
                pfx = 83.0/RX;
                if (pfx*pfx + pfz*pfz <= 0.97) block(es, 83, SY+1, z, BlockTypes.OAK_FENCE);
            }
            // OAK_LOG fence posts + LANTERN at corners
            int[][] villageLamps = {{73,50},{83,50},{73,60},{83,60}};
            for (int[] vl : villageLamps) {
                double vlx = (double)vl[0]/RX, vlz = (double)vl[1]/RZ;
                if (vlx*vlx + vlz*vlz > 0.97) continue;
                block(es, vl[0], SY+1, vl[1], BlockTypes.OAK_LOG);
                block(es, vl[0], SY+2, vl[1], BlockTypes.LANTERN);
            }
        }

        // ── Easter Egg #6: Floating Arrow — giant "YOU ARE HERE" marker ───────
        {
            // Shaft: 3-wide (x=-1..1) from y=SY+62 down to SY+70
            for (int y = SY+62; y <= SY+70; y++) {
                for (int x = -1; x <= 1; x++) {
                    block(es, x, y, 0, BlockTypes.QUARTZ_BRICKS);
                }
            }
            // Arrowhead at SY+70: 9-wide (x=-4..4) but 1 thick (z=0)
            for (int x = -4; x <= 4; x++) {
                block(es, x, SY+70, 0, BlockTypes.QUARTZ_BRICKS);
            }
            // Tapering: x=-3..3 at SY+69
            for (int x = -3; x <= 3; x++) {
                block(es, x, SY+69, 0, BlockTypes.QUARTZ_BRICKS);
            }
            // x=-2..2 at SY+68
            for (int x = -2; x <= 2; x++) {
                block(es, x, SY+68, 0, BlockTypes.QUARTZ_BRICKS);
            }
        }

        // ── Easter Egg #7: Underground Rave Room ─────────────────────────────
        {
            // Floor: SEA_LANTERN 9×9 at y=40
            fill(es, -4, 40, -4, 4, 40, 4, BlockTypes.SEA_LANTERN);
            // Walls: TINTED_GLASS (4 sides, 3 tall)
            for (int y = 40; y <= 42; y++) {
                for (int i = -4; i <= 4; i++) {
                    block(es,  i, y, -4, BlockTypes.TINTED_GLASS);
                    block(es,  i, y,  4, BlockTypes.TINTED_GLASS);
                    block(es, -4, y,  i, BlockTypes.TINTED_GLASS);
                    block(es,  4, y,  i, BlockTypes.TINTED_GLASS);
                }
            }
            // Ceiling: GLOWSTONE 9×9 at y=43
            fill(es, -4, 43, -4, 4, 43, 4, BlockTypes.GLOWSTONE);
            // Corner pillars: SHROOMLIGHT at each corner y=40..43
            for (int y = 40; y <= 43; y++) {
                block(es, -4, y, -4, BlockTypes.SHROOMLIGHT);
                block(es,  4, y, -4, BlockTypes.SHROOMLIGHT);
                block(es, -4, y,  4, BlockTypes.SHROOMLIGHT);
                block(es,  4, y,  4, BlockTypes.SHROOMLIGHT);
            }
            // Outer shell: BLACKSTONE
            fill(es, -5, 39, -5, 5, 44, 5, BlockTypes.BLACKSTONE);
            // Override interior back to correct blocks
            fill(es, -4, 40, -4, 4, 43, 4, BlockTypes.CAVE_AIR);
            // Redo floor/ceiling/walls after shell
            fill(es, -4, 40, -4, 4, 40, 4, BlockTypes.SEA_LANTERN);
            fill(es, -4, 43, -4, 4, 43, 4, BlockTypes.GLOWSTONE);
            for (int y = 41; y <= 42; y++) {
                for (int i = -4; i <= 4; i++) {
                    block(es,  i, y, -4, BlockTypes.TINTED_GLASS);
                    block(es,  i, y,  4, BlockTypes.TINTED_GLASS);
                    block(es, -4, y,  i, BlockTypes.TINTED_GLASS);
                    block(es,  4, y,  i, BlockTypes.TINTED_GLASS);
                }
            }
            for (int y = 40; y <= 43; y++) {
                block(es, -4, y, -4, BlockTypes.SHROOMLIGHT);
                block(es,  4, y, -4, BlockTypes.SHROOMLIGHT);
                block(es, -4, y,  4, BlockTypes.SHROOMLIGHT);
                block(es,  4, y,  4, BlockTypes.SHROOMLIGHT);
            }
            // Access shaft from main cave downward
            block(es, 2, 44, 0, BlockTypes.CAVE_AIR);
            block(es, 2, 43, 0, BlockTypes.CAVE_AIR);
        }

        // ── Easter Egg #8: Giant Chess King Crown atop spire ─────────────────
        {
            // Column y=SY+58..SY+62: 3-wide hollow (x=-1..1, z=-1..1)
            for (int y = SY+58; y <= SY+62; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        boolean isWall = (x==-1||x==1||z==-1||z==1);
                        if (isWall) block(es, x, y, z, BlockTypes.CHISELED_QUARTZ_BLOCK);
                    }
                }
            }
            // Crown ring at SY+63: full 3×3
            fill(es, -1, SY+63, -1, 1, SY+63, 1, BlockTypes.CHISELED_QUARTZ_BLOCK);
            // 5 prongs at SY+64: (-1,0),(1,0),(0,-1),(0,1),(0,0)
            block(es, -1, SY+64,  0, BlockTypes.CHISELED_QUARTZ_BLOCK);
            block(es,  1, SY+64,  0, BlockTypes.CHISELED_QUARTZ_BLOCK);
            block(es,  0, SY+64, -1, BlockTypes.CHISELED_QUARTZ_BLOCK);
            block(es,  0, SY+64,  1, BlockTypes.CHISELED_QUARTZ_BLOCK);
            block(es,  0, SY+64,  0, BlockTypes.CHISELED_QUARTZ_BLOCK);
        }

        // ── Easter Egg #9: Derp Face on east wall ────────────────────────────
        {
            int dfx = 78;
            // Eyes: looking up at SY+3
            block(es, dfx, SY+3, -2, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, dfx, SY+3,  2, BlockTypes.CHISELED_STONE_BRICKS);
            // Left eye derp (looking down-right): at SY+2
            block(es, dfx, SY+2, -2, BlockTypes.CHISELED_STONE_BRICKS);
            // Mouth (open): row at SY+1: z=-2,-1,0,1,2
            block(es, dfx, SY+1, -2, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, dfx, SY+1, -1, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, dfx, SY+1,  0, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, dfx, SY+1,  1, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, dfx, SY+1,  2, BlockTypes.CHISELED_STONE_BRICKS);
        }

        // ── Easter Egg #10: The Counting Room ────────────────────────────────
        {
            // Carve room
            fill(es, 37, SY-4, 77, 43, SY-2, 83, BlockTypes.CAVE_AIR);
            // Line walls with CHISELED_STONE_BRICKS
            for (int y = SY-4; y <= SY-2; y++) {
                for (int x = 37; x <= 43; x++) {
                    for (int z = 77; z <= 83; z++) {
                        boolean isWall = (x==37||x==43||z==77||z==83||y==SY-2);
                        if (isWall) block(es, x, y, z, BlockTypes.CHISELED_STONE_BRICKS);
                    }
                }
            }
            // Floor: POLISHED_DEEPSLATE
            fill(es, 38, SY-4, 78, 42, SY-4, 82, BlockTypes.POLISHED_DEEPSLATE);
            // Ceiling: SEA_LANTERN strips
            block(es, 39, SY-2, 80, BlockTypes.SEA_LANTERN);
            block(es, 41, SY-2, 80, BlockTypes.SEA_LANTERN);
            // Ascending quartz staircase (i=0..9): diagonal effect
            for (int i = 0; i <= 9; i++) {
                double cfx = (double)(37+i)/RX, cfz = 80.0/RZ;
                if (cfx*cfx + cfz*cfz > 0.97) continue;
                block(es, 37+i, SY-4+i, 80, BlockTypes.QUARTZ_BRICKS);
            }
            // Access: 1×2 tunnel from surface going down
            for (int y = SY-1; y >= SY-4; y--) {
                block(es, 40, y, SY, BlockTypes.CAVE_AIR);
                block(es, 40, y, SY+1, BlockTypes.CAVE_AIR);
            }
        }

        // ── Easter Egg #11: LemonPvP "L" logo at island underside (y=38) ──────
        {
            // Vertical bar of "L"
            block(es, 0, 38, -2, BlockTypes.GLOWSTONE);
            block(es, 0, 38, -1, BlockTypes.GLOWSTONE);
            block(es, 0, 38,  0, BlockTypes.GLOWSTONE);
            block(es, 0, 38,  1, BlockTypes.GLOWSTONE);
            block(es, 0, 38,  2, BlockTypes.GLOWSTONE);
            // Horizontal bar (bottom of L)
            block(es, 1, 38,  2, BlockTypes.GLOWSTONE);
            block(es, 2, 38,  2, BlockTypes.GLOWSTONE);
        }

        // ── Easter Egg #12: Sealed Time Capsule — OBSIDIAN chest chamber ──────
        {
            fill(es, -1, 34, -1, 1, 36, 1, BlockTypes.OBSIDIAN);
            block(es, 0, 35, 0, BlockTypes.CHEST);
        }

        // Additional accent calls
        buildDetailedIslandEdges(es);
        buildSceneryDetails(es);
        buildSkyStructures(es);
        buildIslandCliffs(es);
        buildMineralDeposits(es);
        buildPortalPlazas(es);
        buildAmbientDetail(es);
        buildNightAmbience(es);
        buildDetailedTowerExterior(es);
        buildSurfaceMicroTerrain(es);
        buildWelcomeArch(es);
        buildEasterEggsExtra(es);

        // ── Easter Egg Summary ────────────────────────────────────────────────
        // #1  Pixel Duck       @ (-82,SY+1,-3)     — NW perimeter, pixel art
        // #2  Nerd Room        @ (-8,46,-8)         — hidden cave room with books
        // #3  LemonPvP Heart   @ (0,SY+1,80..87)   — heart pixel art in SW
        // #4  Void Mirror      @ (0,25,0)           — OBSIDIAN mirror deepest layer
        // #5  Tiny Village     @ (75..80,SY,52..57) — 3 miniature houses in SE
        // #6  Floating Arrow   @ (0,SY+62,0)        — quartz arrow above spawn
        // #7  Underground Rave @ (0,40,0)           — glowing room at island bottom
        // #8  Chess Crown      @ (0,SY+58,0)        — king crown atop spire
        // #9  Derp Face        @ (78,SY+2,0)        — face on east perimeter wall
        // #10 Counting Room    @ (40,SY-4,80)       — ascending quartz staircase
        // #11 LemonPvP Logo    @ (0,38,0)           — glowstone L at island base
        // #12 Time Capsule     @ (0,35,0)           — sealed obsidian chest chamber
        // #13 Star Map         @ (0,SY+80,0)        — overhead constellation of sea lanterns
        // #14 The Aquarium     @ (30,SY-3,85)       — glass prismarine fish tank
        // #15 Gravity Pillar   @ (-85,SY+1,0)       — pillar floating 3 blocks above ground
        // #16 Mirror Plaza     @ (0,SY,0) y=28      — deep reflection of plaza in obsidian
        // #17 Mossy Skull      @ (-72,SY+1,-72)     — 5×5×4 skull carved in stone
        // #18 The Library      @ (0,SY-6,-40)       — subterranean room with bookcases
        // #19 Compass Rose     @ (0,SY+1,0)         — hidden under plaza floor
        // #20 The Gold Vault   @ (-5,42,5)          — gold-block-lined sealed vault
    }

    // ============================================================================
    //  EXTRA ACCENT METHODS
    // ============================================================================

    private void buildDetailedIslandEdges(EditSession es) {
        // ── Extra coarse-dirt/gravel fringe at the very rim ───────────────────
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            for (double r = 0.88; r <= 0.97; r += 0.03) {
                int wx = (int) Math.round(RX * r * Math.cos(rad));
                int wz = (int) Math.round(RZ * r * Math.sin(rad));
                double ex = (double) wx / RX, ez = (double) wz / RZ;
                double t  = ex * ex + ez * ez;
                if (t > 0.98 || t < 0.80) continue;
                if ((angle + (int)(r * 10)) % 4 == 0)
                    block(es, wx, SY, wz, BlockTypes.COARSE_DIRT);
                if ((angle * 7 + (int)(r * 13)) % 11 == 0)
                    block(es, wx, SY, wz, BlockTypes.GRAVEL);
            }
        }

        // ── Mossy ledges around outer ellipse ─────────────────────────────────
        for (int angle = 0; angle < 360; angle += 3) {
            double rad = Math.toRadians(angle);
            int wx = (int) Math.round((RX - 2) * Math.cos(rad));
            int wz = (int) Math.round((RZ - 2) * Math.sin(rad));
            double ex = (double) wx / RX, ez = (double) wz / RZ;
            if (ex * ex + ez * ez > 0.96) continue;
            if (angle % 7 < 3) block(es, wx, SY + 1, wz, BlockTypes.MOSS_CARPET);
            if (angle % 11 == 0) block(es, wx, SY + 1, wz, BlockTypes.SHORT_GRASS);
        }

        // ── Cobblestone/andesite cliff bands on steep face segments ───────────
        for (int angle = 20; angle < 360; angle += 25) {
            double rad = Math.toRadians(angle);
            int cx = (int) Math.round((RX - 6) * Math.cos(rad));
            int cz = (int) Math.round((RZ - 6) * Math.sin(rad));
            double ex = (double) cx / RX, ezv = (double) cz / RZ;
            double t  = ex * ex + ezv * ezv;
            if (t > 0.88) continue;
            int bottomY = 40 + (int)(21.0 * t);
            for (int dy = 0; dy <= 5; dy++) {
                int by = bottomY + dy + 4;
                if (by >= SY) break;
                if (dy % 2 == 0)
                    block(es, cx, by, cz, BlockTypes.COBBLESTONE);
                else
                    block(es, cx, by, cz, BlockTypes.ANDESITE);
            }
        }

        // ── Dangling stalactite 'teeth' along the south edge ──────────────────
        for (int x = -70; x <= 70; x += 6) {
            double ex = (double) x / RX, ez = (double) 95 / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.96) continue;
            int bottomY = 40 + (int)(21.0 * t);
            int len = 3 + Math.abs(x) % 4;
            for (int i = 0; i < len; i++)
                block(es, x, bottomY - i, 95, BlockTypes.DEEPSLATE);
        }

        // ── Similar teeth on the north edge ──────────────────────────────────
        for (int x = -70; x <= 70; x += 6) {
            double ex = (double) x / RX, ez = (double)(-95) / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.96) continue;
            int bottomY = 40 + (int)(21.0 * t);
            int len = 3 + Math.abs(x) % 4;
            for (int i = 0; i < len; i++)
                block(es, x, bottomY - i, -95, BlockTypes.DEEPSLATE);
        }

        // ── East and West teeth ───────────────────────────────────────────────
        for (int z = -90; z <= 90; z += 7) {
            double ez = (double) z / RZ;
            for (int sx : new int[]{88, -88}) {
                double ex = (double) sx / RX;
                double t  = ex * ex + ez * ez;
                if (t > 0.97) continue;
                int bottomY = 40 + (int)(21.0 * t);
                int len = 3 + Math.abs(z) % 5;
                for (int i = 0; i < len; i++)
                    block(es, sx, bottomY - i, z, BlockTypes.COBBLED_DEEPSLATE);
            }
        }

        // ── Extra tuff/andesite scatter on edge coarse-dirt areas ─────────────
        for (int angle = 0; angle < 360; angle += 2) {
            double rad = Math.toRadians(angle);
            int wx = (int) Math.round((RX - 8) * Math.cos(rad));
            int wz = (int) Math.round((RZ - 8) * Math.sin(rad));
            double ex = (double) wx / RX, ez = (double) wz / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.90 || t < 0.72) continue;
            if ((wx * 3 + wz * 7 + angle) % 17 == 0)
                block(es, wx, SY + 1, wz, BlockTypes.TUFF);
            if ((wx * 11 + wz * 5 + angle) % 19 == 0)
                block(es, wx, SY + 1, wz, BlockTypes.ANDESITE);
        }

        // ── Hanging ferns on underside overhang lips ──────────────────────────
        for (int angle = 0; angle < 360; angle += 8) {
            double rad = Math.toRadians(angle);
            int wx = (int) Math.round((RX - 10) * Math.cos(rad));
            int wz = (int) Math.round((RZ - 10) * Math.sin(rad));
            double ex = (double) wx / RX, ez = (double) wz / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.85) continue;
            int bottomY = 40 + (int)(21.0 * t);
            if (angle % 16 < 8) block(es, wx, bottomY + 6, wz, BlockTypes.HANGING_ROOTS);
            if (angle % 24 == 0) block(es, wx, bottomY + 5, wz, BlockTypes.HANGING_ROOTS);
        }

        // ── Gravel/dirt ravines cutting into island top near outer rim ─────────
        for (int angle = 45; angle < 360; angle += 90) {
            double rad = Math.toRadians(angle);
            int rx2 = (int) Math.round((RX - 14) * Math.cos(rad));
            int rz2 = (int) Math.round((RZ - 14) * Math.sin(rad));
            double ex = (double) rx2 / RX, ez = (double) rz2 / RZ;
            if (ex * ex + ez * ez > 0.86) continue;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz2 = -1; dz2 <= 1; dz2++) {
                    block(es, rx2 + dx, SY,     rz2 + dz2, BlockTypes.GRAVEL);
                    block(es, rx2 + dx, SY - 1, rz2 + dz2, BlockTypes.GRAVEL);
                    block(es, rx2 + dx, SY - 2, rz2 + dz2, BlockTypes.COARSE_DIRT);
                }
            }
        }
    }

    private void buildSceneryDetails(EditSession es) {
        // ── Small stone walls connecting landscape features ────────────────────
        // Low stone-brick walls (1-2 blocks tall) in various island areas

        // North garden low wall at z=-28 from x=-12..12
        for (int x = -12; x <= 12; x++) {
            block(es, x, SY,     -28, BlockTypes.STONE_BRICKS);
            block(es, x, SY + 1, -28, BlockTypes.STONE_BRICK_WALL);
            if (Math.abs(x) % 4 == 0) block(es, x, SY + 2, -28, BlockTypes.STONE_BRICKS);
        }
        // Short wall east of arena at x=-20, z=-42..-58
        for (int z = -42; z >= -58; z--) {
            block(es, -20, SY,     z, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, -20, SY + 1, z, BlockTypes.STONE_BRICK_WALL);
        }

        // ── Extra tree at mid-ring between portals ────────────────────────────
        buildOakTree(es, -14, -32, 6);
        buildBirchTree(es, 14,  32, 7);
        buildCherryTree(es, -14, 32, 5);
        buildOakTree(es, 14, -32, 6);
        buildDarkOakTree(es, 0, -40, 8);
        buildOakTree(es, -28, 0, 5);
        buildBirchTree(es, 28, 0, 7);
        buildOakTree(es, 0, 35, 5);

        // ── Extra boulder fields ───────────────────────────────────────────────
        int[][] extraBoulders = {
            {-55,-18},{-62, 5},{-48,-30},{-40,-58},{-72,-10},
            { 55, 18},{ 62,-5},{ 48, 30},{ 40, 58},{ 72, 10},
            {-20,-55},{ 20, 55},{-55, 40},{ 55,-40},{ 0,-80},
            {  0, 80},{-80, 0},{ 80,  0},{-60,-60},{ 60, 60}
        };
        com.sk89q.worldedit.world.block.BlockType[] bMat2 = {
            BlockTypes.STONE, BlockTypes.ANDESITE, BlockTypes.GRANITE,
            BlockTypes.DIORITE, BlockTypes.MOSSY_COBBLESTONE, BlockTypes.COBBLESTONE
        };
        for (int i = 0; i < extraBoulders.length; i++) {
            int bx = extraBoulders[i][0], bz = extraBoulders[i][1];
            double ex = (double) bx / RX, ez = (double) bz / RZ;
            if (ex * ex + ez * ez > 0.88) continue;
            block(es, bx,     SY + 1, bz,     bMat2[i % 6]);
            block(es, bx + 1, SY + 1, bz,     bMat2[(i + 1) % 6]);
            block(es, bx,     SY + 1, bz + 1, bMat2[(i + 2) % 6]);
            if (i % 3 == 0) block(es, bx, SY + 2, bz, bMat2[(i + 3) % 6]);
        }

        // ── Flower beds around secondary hub plazas ────────────────────────────
        BlockType[] flowerRing = {
            BlockTypes.OXEYE_DAISY, BlockTypes.CORNFLOWER, BlockTypes.POPPY,
            BlockTypes.DANDELION, BlockTypes.BLUE_ORCHID, BlockTypes.ALLIUM
        };
        int[][] hubCentres = {{32,0},{-32,0},{0,32},{0,-32}};
        for (int[] hc : hubCentres) {
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                int fx = hc[0] + (int)(6 * Math.cos(rad));
                int fz = hc[1] + (int)(6 * Math.sin(rad));
                double ex = (double) fx / RX, ez = (double) fz / RZ;
                if (ex * ex + ez * ez > 0.90) continue;
                block(es, fx, SY + 1, fz, flowerRing[(a / 30) % 6]);
            }
        }

        // ── Decorative cauldrons near portal platforms ─────────────────────────
        // Crystal portal cauldron
        block(es, 58, SY + 1, 3, BlockTypes.CAULDRON);
        block(es, 58, SY + 1, -3, BlockTypes.CAULDRON);
        // Sword portal
        block(es, -58, SY + 1, 3, BlockTypes.CAULDRON);
        block(es, -58, SY + 1, -3, BlockTypes.CAULDRON);
        // Mace portal
        block(es, 3, SY + 1, 68, BlockTypes.CAULDRON);
        block(es, -3, SY + 1, 68, BlockTypes.CAULDRON);
        // Bow portal
        block(es, 3, SY + 1, -68, BlockTypes.CAULDRON);
        block(es, -3, SY + 1, -68, BlockTypes.CAULDRON);

        // ── Barrel/chest clusters at ring-road crossings ───────────────────────
        int[][] roadNodes = {{28,0},{-28,0},{0,34},{0,-34}};
        for (int[] rn : roadNodes) {
            block(es, rn[0] + 3, SY + 1, rn[1],     BlockTypes.BARREL);
            block(es, rn[0] - 3, SY + 1, rn[1],     BlockTypes.BARREL);
            block(es, rn[0],     SY + 1, rn[1] + 3, BlockTypes.CHEST);
        }

        // ── Cobblestone steps descending at island rim gaps ────────────────────
        for (int angle = 0; angle < 360; angle += 60) {
            double rad = Math.toRadians(angle);
            int base = 0;
            for (int step = 0; step < 4; step++) {
                double r2 = (0.93 + step * 0.01);
                int wx = (int) Math.round(RX * r2 * Math.cos(rad));
                int wz = (int) Math.round(RZ * r2 * Math.sin(rad));
                double ex2 = (double) wx / RX, ez2 = (double) wz / RZ;
                if (ex2 * ex2 + ez2 * ez2 > 0.99) continue;
                block(es, wx, SY - step, wz, BlockTypes.COBBLESTONE);
            }
        }

        // ── Hanging chain decorations under tower balconies ────────────────────
        for (int bx = -8; bx <= 8; bx += 4) {
            block(es, bx, SY + 24, 9,  BlockTypes.CHAIN);
            block(es, bx, SY + 23, 9,  BlockTypes.CHAIN);
            block(es, bx, SY + 22, 9,  BlockTypes.LANTERN);
            block(es, bx, SY + 24, -9, BlockTypes.CHAIN);
            block(es, bx, SY + 23, -9, BlockTypes.CHAIN);
            block(es, bx, SY + 22, -9, BlockTypes.LANTERN);
        }
        for (int bz = -8; bz <= 8; bz += 4) {
            block(es, 9,  SY + 24, bz, BlockTypes.CHAIN);
            block(es, 9,  SY + 23, bz, BlockTypes.CHAIN);
            block(es, 9,  SY + 22, bz, BlockTypes.LANTERN);
            block(es, -9, SY + 24, bz, BlockTypes.CHAIN);
            block(es, -9, SY + 23, bz, BlockTypes.CHAIN);
            block(es, -9, SY + 22, bz, BlockTypes.LANTERN);
        }

        // ── Spruce-plank walkway from ring road to each watchtower ────────────
        int[][] wtPos = {{65,50},{-65,50},{65,-60},{-65,-60},{0,95},{0,-95}};
        for (int[] wt : wtPos) {
            double ex2 = (double) wt[0] / RX, ez2 = (double) wt[1] / RZ;
            if (ex2 * ex2 + ez2 * ez2 > 0.92) continue;
            // Straight walkway from ring road (scaled r≈28 direction) to watchtower
            int steps = 20;
            for (int s = 0; s < steps; s++) {
                int wx2 = wt[0] / Math.abs(wt[0] == 0 ? 1 : wt[0]) * (28 + s * (Math.abs(wt[0]) - 28) / steps);
                // Simplified: just draw along dominant axis
                double frac = (double) s / steps;
                int px = (int)(wt[0] * frac);
                int pz = (int)(wt[1] * frac);
                if (Math.abs(px) < 22 && Math.abs(pz) < 22) continue;
                double epx = (double) px / RX, epz = (double) pz / RZ;
                if (epx * epx + epz * epz > 0.91) continue;
                block(es, px,     SY, pz,     BlockTypes.SPRUCE_PLANKS);
                block(es, px + (wt[1] == 0 ? 0 : 1), SY, pz + (wt[0] == 0 ? 0 : 1), BlockTypes.SPRUCE_PLANKS);
            }
        }
    }

    private void buildSkyStructures(EditSession es) {
        // ── 4 floating mini-islands above main island ─────────────────────────
        // Each is a small disk of GRASS_BLOCK/DIRT/STONE with a tree or structure

        // Floating island #1 — NE above forest, at (50, SY+25, -55)
        int fi1x = 50, fi1y = SY + 25, fi1z = -55;
        disk(es, fi1x, fi1y,     fi1z, 5, BlockTypes.GRASS_BLOCK);
        disk(es, fi1x, fi1y - 1, fi1z, 4, BlockTypes.DIRT);
        disk(es, fi1x, fi1y - 2, fi1z, 3, BlockTypes.STONE);
        disk(es, fi1x, fi1y - 3, fi1z, 2, BlockTypes.DEEPSLATE);
        block(es, fi1x, fi1y - 4, fi1z, BlockTypes.COBBLED_DEEPSLATE);
        buildOakTree(es, fi1x, fi1z, 5);  // offset Y is already baked into fi1y
        // Note: tree builds from SY — needs placement offset. Place manually:
        column(es, fi1x, fi1z, fi1y + 1, fi1y + 5, BlockTypes.OAK_LOG);
        for (int dx = -3; dx <= 3; dx++)
            for (int dz2 = -3; dz2 <= 3; dz2++)
                if (dx*dx+dz2*dz2 <= 10)
                    block(es, fi1x+dx, fi1y+5, fi1z+dz2, BlockTypes.OAK_LEAVES);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz2 = -2; dz2 <= 2; dz2++)
                if (dx*dx+dz2*dz2 <= 6)
                    block(es, fi1x+dx, fi1y+6, fi1z+dz2, BlockTypes.OAK_LEAVES);
        block(es, fi1x, fi1y+7, fi1z, BlockTypes.OAK_LEAVES);
        // Chain tethers from this island down to perimeter wall
        for (int cy = fi1y - 5; cy >= SY + 8; cy--)
            if ((cy + fi1x) % 5 == 0)
                block(es, fi1x, cy, fi1z, BlockTypes.CHAIN);

        // Floating island #2 — SW above water gardens, at (-50, SY+22, 60)
        int fi2x = -50, fi2y = SY + 22, fi2z = 60;
        disk(es, fi2x, fi2y,     fi2z, 4, BlockTypes.GRASS_BLOCK);
        disk(es, fi2x, fi2y - 1, fi2z, 3, BlockTypes.DIRT);
        disk(es, fi2x, fi2y - 2, fi2z, 2, BlockTypes.STONE);
        block(es, fi2x, fi2y - 3, fi2z, BlockTypes.COBBLED_DEEPSLATE);
        column(es, fi2x, fi2z, fi2y + 1, fi2y + 6, BlockTypes.CHERRY_LOG);
        for (int dy = -1; dy <= 3; dy++) {
            int r = (dy == -1) ? 2 : (dy <= 1) ? 3 : (dy == 2) ? 2 : 1;
            for (int dx = -r; dx <= r; dx++)
                for (int dz2 = -r; dz2 <= r; dz2++)
                    if (dx*dx+dz2*dz2 <= r*r)
                        block(es, fi2x+dx, fi2y+6+dy, fi2z+dz2, BlockTypes.CHERRY_LEAVES);
        }
        for (int cy = fi2y - 4; cy >= SY + 8; cy--)
            if ((cy + fi2x) % 4 == 0)
                block(es, fi2x, cy, fi2z, BlockTypes.CHAIN);

        // Floating island #3 — above arena, at (-50, SY+30, -50)
        int fi3x = -50, fi3y = SY + 30, fi3z = -50;
        disk(es, fi3x, fi3y,     fi3z, 6, BlockTypes.POLISHED_DEEPSLATE);
        disk(es, fi3x, fi3y - 1, fi3z, 5, BlockTypes.DEEPSLATE_BRICKS);
        disk(es, fi3x, fi3y - 2, fi3z, 4, BlockTypes.DEEPSLATE);
        disk(es, fi3x, fi3y - 3, fi3z, 3, BlockTypes.COBBLED_DEEPSLATE);
        disk(es, fi3x, fi3y - 4, fi3z, 2, BlockTypes.COBBLED_DEEPSLATE);
        block(es, fi3x, fi3y - 5, fi3z, BlockTypes.DEEPSLATE);
        // Small shrine on top
        fill(es, fi3x-2, fi3y+1, fi3z-2, fi3x+2, fi3y+1, fi3z+2, BlockTypes.POLISHED_DEEPSLATE);
        column(es, fi3x, fi3z, fi3y+2, fi3y+5, BlockTypes.QUARTZ_PILLAR);
        block(es, fi3x, fi3y+6, fi3z, BlockTypes.SEA_LANTERN);
        block(es, fi3x, fi3y+7, fi3z, BlockTypes.CHAIN);
        block(es, fi3x, fi3y+8, fi3z, BlockTypes.LANTERN);
        // Iron-bar railing around disk edge
        for (int a = 0; a < 360; a += 20) {
            double rad = Math.toRadians(a);
            int px = fi3x + (int)(6 * Math.cos(rad));
            int pz = fi3z + (int)(6 * Math.sin(rad));
            column(es, px, pz, fi3y+1, fi3y+2, BlockTypes.IRON_BARS);
        }
        for (int cy = fi3y - 6; cy >= SY + 25; cy--)
            block(es, fi3x, cy, fi3z, BlockTypes.CHAIN);

        // Floating island #4 — above ruins, at (60, SY+20, 55)
        int fi4x = 60, fi4y = SY + 20, fi4z = 55;
        disk(es, fi4x, fi4y,     fi4z, 4, BlockTypes.MOSSY_COBBLESTONE);
        disk(es, fi4x, fi4y - 1, fi4z, 3, BlockTypes.COBBLESTONE);
        disk(es, fi4x, fi4y - 2, fi4z, 2, BlockTypes.STONE);
        block(es, fi4x, fi4y - 3, fi4z, BlockTypes.COBBLED_DEEPSLATE);
        // Ruined mini-structure on top
        fill(es, fi4x-1, fi4y+1, fi4z-1, fi4x+1, fi4y+3, fi4z+1, BlockTypes.CRACKED_STONE_BRICKS);
        block(es, fi4x, fi4y+2, fi4z, BlockTypes.CAVE_AIR);  // hollow
        block(es, fi4x, fi4y+4, fi4z, BlockTypes.MOSSY_COBBLESTONE);
        for (int cy = fi4y - 4; cy >= SY + 10; cy--)
            if ((cy + fi4z) % 6 == 0)
                block(es, fi4x, cy, fi4z, BlockTypes.CHAIN);

        // ── Skybridge between floating island #1 and #3 ───────────────────────
        // Bridge from (fi1x,fi1y,fi1z) to (fi3x,fi3y,fi3z) — diagonal plank bridge
        int steps = 25;
        for (int s = 0; s <= steps; s++) {
            double frac = (double) s / steps;
            int bx = fi1x + (int)((fi3x - fi1x) * frac);
            int by = fi1y + (int)((fi3y - fi1y) * frac);
            int bz = fi1z + (int)((fi3z - fi1z) * frac);
            block(es, bx, by, bz, BlockTypes.SPRUCE_PLANKS);
            block(es, bx + 1, by, bz, BlockTypes.SPRUCE_PLANKS);
            if (s % 4 == 0) {
                block(es, bx, by + 1, bz, BlockTypes.OAK_FENCE);
                block(es, bx + 1, by + 1, bz, BlockTypes.OAK_FENCE);
            }
        }

        // ── Giant hanging lantern array above plaza ────────────────────────────
        // 8 chains descending from y=SY+55 to SY+40, radius 12
        for (int a = 0; a < 8; a++) {
            double rad = Math.toRadians(a * 45.0);
            int lx = (int) Math.round(12 * Math.cos(rad));
            int lz = (int) Math.round(12 * Math.sin(rad));
            for (int cy = SY + 40; cy <= SY + 55; cy++)
                block(es, lx, cy, lz, BlockTypes.CHAIN);
            block(es, lx, SY + 39, lz, BlockTypes.LANTERN);
            block(es, lx, SY + 38, lz, BlockTypes.GLOWSTONE);
        }
        // Central mega lantern
        fill(es, -1, SY + 52, -1, 1, SY + 52, 1, BlockTypes.GLOWSTONE);
        block(es, 0, SY + 51, 0, BlockTypes.SEA_LANTERN);
        block(es, 0, SY + 53, 0, BlockTypes.CHAIN);
        block(es, 0, SY + 54, 0, BlockTypes.LANTERN);
    }

    private void buildIslandCliffs(EditSession es) {
        // ── Exposed cliff faces with layered geology ──────────────────────────
        // Simulate geological strata on steeper edge sections

        // North cliff (z≈-100 to -95): expose layers
        for (int x = -60; x <= 60; x += 2) {
            double ex = (double) x / RX, ez = (double)(-98) / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.97) continue;
            int bottomY = 40 + (int)(21.0 * t);
            // Stone strata bands
            for (int y = bottomY + 4; y <= SY - 2; y++) {
                int stratum = (y - bottomY) % 8;
                if (stratum < 3)      block(es, x, y, -98, BlockTypes.DEEPSLATE);
                else if (stratum < 5) block(es, x, y, -98, BlockTypes.STONE);
                else if (stratum < 6) block(es, x, y, -98, BlockTypes.GRAVEL);
                else                  block(es, x, y, -98, BlockTypes.COBBLESTONE);
            }
            // Cliff face plants
            if (x % 8 == 0) block(es, x, SY - 1, -98, BlockTypes.HANGING_ROOTS);
            if (x % 12 == 0) block(es, x, SY - 3, -98, BlockTypes.MOSS_CARPET);
        }

        // South cliff
        for (int x = -60; x <= 60; x += 2) {
            double ex = (double) x / RX, ez = (double)(98) / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.97) continue;
            int bottomY = 40 + (int)(21.0 * t);
            for (int y = bottomY + 4; y <= SY - 2; y++) {
                int stratum = (y - bottomY) % 6;
                if (stratum < 2)      block(es, x, y, 98, BlockTypes.COBBLED_DEEPSLATE);
                else if (stratum < 4) block(es, x, y, 98, BlockTypes.STONE);
                else                  block(es, x, y, 98, BlockTypes.ANDESITE);
            }
        }

        // East cliff
        for (int z = -80; z <= 80; z += 2) {
            double ex = (double)(88) / RX, ez = (double) z / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.97) continue;
            int bottomY = 40 + (int)(21.0 * t);
            for (int y = bottomY + 4; y <= SY - 2; y++) {
                int stratum = (y - bottomY) % 7;
                if (stratum < 2)      block(es, 88, y, z, BlockTypes.DEEPSLATE);
                else if (stratum < 4) block(es, 88, y, z, BlockTypes.TUFF);
                else if (stratum < 5) block(es, 88, y, z, BlockTypes.GRAVEL);
                else                  block(es, 88, y, z, BlockTypes.COBBLESTONE);
            }
        }

        // West cliff
        for (int z = -80; z <= 80; z += 2) {
            double ex = (double)(-88) / RX, ez = (double) z / RZ;
            double t  = ex * ex + ez * ez;
            if (t > 0.97) continue;
            int bottomY = 40 + (int)(21.0 * t);
            for (int y = bottomY + 4; y <= SY - 2; y++) {
                int stratum = (y - bottomY) % 5;
                if (stratum < 2)      block(es, -88, y, z, BlockTypes.COBBLED_DEEPSLATE);
                else if (stratum < 3) block(es, -88, y, z, BlockTypes.SMOOTH_STONE);
                else                  block(es, -88, y, z, BlockTypes.STONE);
            }
        }

        // ── Clifftop edge decorations ─────────────────────────────────────────
        for (int angle = 15; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            int ex2 = (int) Math.round((RX - 3) * Math.cos(rad));
            int ez2 = (int) Math.round((RZ - 3) * Math.sin(rad));
            double ex = (double) ex2 / RX, ez = (double) ez2 / RZ;
            if (ex * ex + ez * ez > 0.95) continue;
            if (angle % 30 < 15) {
                block(es, ex2, SY + 1, ez2, BlockTypes.TUFF);
            } else {
                block(es, ex2, SY + 1, ez2, BlockTypes.SHORT_GRASS);
            }
            if (angle % 45 == 0) {
                column(es, ex2, ez2, SY + 1, SY + 2, BlockTypes.ANDESITE);
                block(es, ex2, SY + 3, ez2, BlockTypes.MOSSY_COBBLESTONE);
            }
        }

        // ── Lava-like magma veins on steepest underside segments ──────────────
        for (int angle = 30; angle < 360; angle += 60) {
            double rad = Math.toRadians(angle);
            int cx = (int) Math.round((RX - 15) * Math.cos(rad));
            int cz = (int) Math.round((RZ - 15) * Math.sin(rad));
            double ex = (double) cx / RX, ezv = (double) cz / RZ;
            double t  = ex * ex + ezv * ezv;
            if (t > 0.78) continue;
            int bottomY = 40 + (int)(21.0 * t);
            // Magma block veins on underside
            for (int i = 0; i <= 4; i++) {
                block(es, cx, bottomY + i, cz, BlockTypes.MAGMA_BLOCK);
                if (i < 3) {
                    block(es, cx + 1, bottomY + i, cz, BlockTypes.MAGMA_BLOCK);
                    block(es, cx, bottomY + i, cz + 1, BlockTypes.MAGMA_BLOCK);
                }
            }
        }
    }

    private void buildMineralDeposits(EditSession es) {
        // Surface and near-surface mineral outcroppings visible across the island

        // Amethyst surface geodes (beyond the crystal portal zone)
        int[][] amethystSurface = {
            {42,-60},{-42,60},{60,42},{-60,-42},{72,-30},{-72,30}
        };
        for (int[] a : amethystSurface) {
            double ex = (double) a[0] / RX, ez = (double) a[1] / RZ;
            if (ex * ex + ez * ez > 0.88) continue;
            block(es, a[0],     SY + 1, a[1],     BlockTypes.AMETHYST_BLOCK);
            block(es, a[0] + 1, SY + 1, a[1],     BlockTypes.AMETHYST_CLUSTER);
            block(es, a[0],     SY + 1, a[1] + 1, BlockTypes.AMETHYST_BLOCK);
            block(es, a[0] + 1, SY + 2, a[1] + 1, BlockTypes.BUDDING_AMETHYST);
            block(es, a[0] - 1, SY + 1, a[1],     BlockTypes.AMETHYST_CLUSTER);
        }

        // Diorite/granite outcrops
        int[][] dioriteSpots = {
            {-48,-22},{48,22},{-22,48},{22,-48},{70,8},{-70,-8},
            {38,70},{-38,-70},{60,-42},{-60,42}
        };
        for (int i = 0; i < dioriteSpots.length; i++) {
            int bx = dioriteSpots[i][0], bz = dioriteSpots[i][1];
            double ex = (double) bx / RX, ez = (double) bz / RZ;
            if (ex * ex + ez * ez > 0.87) continue;
            com.sk89q.worldedit.world.block.BlockType mat = (i % 2 == 0) ? BlockTypes.DIORITE : BlockTypes.GRANITE;
            block(es, bx, SY + 1, bz, mat);
            block(es, bx + 1, SY + 1, bz, BlockTypes.POLISHED_ANDESITE);
            block(es, bx, SY + 2, bz, mat);
            if (i % 3 == 0) block(es, bx - 1, SY + 1, bz, mat);
        }

        // Calcite/dripstone outcrops (cave aesthetic on surface)
        int[][] calciteSpots = {
            {-15,60},{15,-60},{60,-15},{-60,15},{0,68},{0,-68},{68,0},{-68,0}
        };
        for (int[] c : calciteSpots) {
            double ex = (double) c[0] / RX, ez = (double) c[1] / RZ;
            if (ex * ex + ez * ez > 0.86) continue;
            block(es, c[0], SY + 1, c[1], BlockTypes.CALCITE);
            block(es, c[0] + 1, SY + 1, c[1], BlockTypes.POINTED_DRIPSTONE);
            block(es, c[0], SY + 1, c[1] + 1, BlockTypes.DRIPSTONE_BLOCK);
            block(es, c[0] - 1, SY + 2, c[1], BlockTypes.CALCITE);
        }

        // Basalt columns poking through surface
        int[][] basaltCols = {
            {55,-18},{-55,18},{18,62},{-18,-62},{75,30},{-75,-30},{42,-75},{-42,75}
        };
        for (int[] b : basaltCols) {
            double ex = (double) b[0] / RX, ez = (double) b[1] / RZ;
            if (ex * ex + ez * ez > 0.88) continue;
            column(es, b[0], b[1], SY + 1, SY + 3, BlockTypes.BASALT);
            block(es, b[0], SY + 4, b[1], BlockTypes.POLISHED_BASALT);
            block(es, b[0] + 1, SY + 1, b[1], BlockTypes.SMOOTH_BASALT);
            block(es, b[0], SY + 1, b[1] + 1, BlockTypes.BASALT);
        }

        // Mud/packed-mud patches in lower-lying areas near water
        int[][] mudPatches = {
            {-30,40},{-38,30},{-28,55},{-45,48},{-20,62},{-50,35}
        };
        for (int[] m : mudPatches) {
            double ex = (double) m[0] / RX, ez = (double) m[1] / RZ;
            if (ex * ex + ez * ez > 0.85) continue;
            fill(es, m[0]-1, SY, m[1]-1, m[0]+1, SY, m[1]+1, BlockTypes.MUD_BRICKS);
            block(es, m[0], SY + 1, m[1], BlockTypes.PACKED_MUD);
        }

        // Nether quartz ore veins on cliff faces (decorative)
        int[][] quartzVeins = {{-88,15},{-88,-15},{88,15},{88,-15},{0,-100},{0,100}};
        for (int[] q : quartzVeins) {
            double ex = (double) q[0] / RX, ez = (double) q[1] / RZ;
            if (ex * ex + ez * ez > 0.99) continue;
            for (int y = SY - 10; y <= SY - 4; y += 2)
                block(es, q[0], y, q[1], BlockTypes.NETHER_QUARTZ_ORE);
        }
    }

    // ============================================================================
    //  EXTRA EASTER EGGS  (#13 – #20)
    // ============================================================================

    private void buildEasterEggsExtra(EditSession es) {

        // ── Easter Egg #13: Star Map ─────────────────────────────────────────
        // Constellation of SEA_LANTERN "stars" at y=SY+80, forming Orion's Belt
        // 3 lanterns in a diagonal line above spawn
        block(es, -6, SY + 80,  -6, BlockTypes.SEA_LANTERN);
        block(es,  0, SY + 80,   0, BlockTypes.SEA_LANTERN);
        block(es,  6, SY + 80,   6, BlockTypes.SEA_LANTERN);
        // Shoulder stars
        block(es, -10, SY + 82,  -2, BlockTypes.GLOWSTONE);
        block(es,  10, SY + 82,  -2, BlockTypes.GLOWSTONE);
        // Belt clasp
        block(es,   0, SY + 83,   4, BlockTypes.GLOWSTONE);
        // Foot stars
        block(es,  -8, SY + 78, 10, BlockTypes.SEA_LANTERN);
        block(es,   8, SY + 78, 10, BlockTypes.SEA_LANTERN);
        // Small secondary stars scattered around
        block(es,  20, SY + 85,  15, BlockTypes.SEA_LANTERN);
        block(es, -20, SY + 85, -15, BlockTypes.SEA_LANTERN);
        block(es,  15, SY + 82, -20, BlockTypes.GLOWSTONE);
        block(es, -15, SY + 82,  20, BlockTypes.GLOWSTONE);
        block(es,   5, SY + 88,  -8, BlockTypes.SEA_LANTERN);
        block(es,  -5, SY + 88,   8, BlockTypes.SEA_LANTERN);
        // Milky Way strip — 14 lanterns in a wide arc at SY+78..82
        int[] mwX = {-30,-25,-20,-15,-10, -5,  0,  5, 10, 15, 20, 25, 30, 35};
        int[] mwZ = {-18,-14,-10, -6, -2,  2,  4,  6,  8, 10, 12, 14, 16, 18};
        int[] mwY = { 78, 79, 80, 81, 82, 82, 82, 81, 80, 79, 78, 78, 79, 80};
        for (int i = 0; i < mwX.length; i++)
            block(es, mwX[i], SY + mwY[i], mwZ[i], BlockTypes.SEA_LANTERN);

        // ── Easter Egg #14: The Aquarium ─────────────────────────────────────
        // Glass prismarine fish tank at (30, SY-3, 85) — below island surface in SE
        int ax = 30, ay = SY - 6, az = 85;
        // Carve room
        fill(es, ax-3, ay, az-3, ax+3, ay+4, az+3, BlockTypes.CAVE_AIR);
        // Glass walls
        for (int y2 = ay; y2 <= ay + 4; y2++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == -3 || dx == 3 || dz == -3 || dz == 3 || y2 == ay || y2 == ay + 4) {
                        boolean isGlass = (y2 > ay && y2 < ay + 4 && dx != -3 && dx != 3 && dz != -3 && dz != 3);
                        if (!isGlass) block(es, ax+dx, y2, az+dz, BlockTypes.PRISMARINE_BRICKS);
                        else block(es, ax+dx, y2, az+dz, BlockTypes.TINTED_GLASS);
                    }
                }
            }
        }
        // Fill with water
        fill(es, ax-2, ay+1, az-2, ax+2, ay+3, az+2, BlockTypes.WATER);
        // Floor detail
        fill(es, ax-2, ay, az-2, ax+2, ay, az+2, BlockTypes.DARK_PRISMARINE);
        block(es, ax,   ay, az,   BlockTypes.SEA_LANTERN);
        block(es, ax-1, ay, az-1, BlockTypes.PRISMARINE);
        block(es, ax+1, ay, az+1, BlockTypes.PRISMARINE);
        // Lily pads on water surface
        block(es, ax-1, ay+4, az,   BlockTypes.LILY_PAD);
        block(es, ax+1, ay+4, az-1, BlockTypes.LILY_PAD);
        // Access trapdoor from surface above
        for (int y2 = ay + 5; y2 < SY; y2++)
            block(es, ax, y2, az, BlockTypes.CAVE_AIR);
        block(es, ax, SY, az, BlockTypes.OAK_TRAPDOOR);
        block(es, ax, SY + 1, az, BlockTypes.MOSS_BLOCK);

        // ── Easter Egg #15: Gravity Pillar ──────────────────────────────────
        // A pillar of QUARTZ_BLOCK floating 3 blocks above ground near Axe portal
        int gpx = -84, gpz = 0;
        column(es, gpx, gpz, SY + 4, SY + 12, BlockTypes.QUARTZ_BLOCK);
        // The "floating" gap — air between y=SY+1..SY+3
        for (int y2 = SY + 1; y2 <= SY + 3; y2++)
            block(es, gpx, y2, gpz, BlockTypes.AIR);
        // Glowstone base on ground
        block(es, gpx, SY + 1, gpz, BlockTypes.GLOWSTONE);
        // Chains ascending from pillar bottom as if defying physics
        for (int y2 = SY + 2; y2 <= SY + 3; y2++)
            block(es, gpx, y2, gpz, BlockTypes.CHAIN);
        // Cap with sea lantern
        block(es, gpx, SY + 13, gpz, BlockTypes.SEA_LANTERN);
        block(es, gpx, SY + 14, gpz, BlockTypes.CHAIN);
        block(es, gpx, SY + 15, gpz, BlockTypes.LANTERN);
        // Flowers around base
        block(es, gpx + 1, SY + 1, gpz,     BlockTypes.OXEYE_DAISY);
        block(es, gpx - 1, SY + 1, gpz,     BlockTypes.CORNFLOWER);
        block(es, gpx,     SY + 1, gpz + 1, BlockTypes.DANDELION);
        block(es, gpx,     SY + 1, gpz - 1, BlockTypes.POPPY);

        // ── Easter Egg #16: Mirror Plaza ────────────────────────────────────
        // At y=28 directly below plaza — an obsidian mirror image
        int mirrorY = 28;
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 20.0) continue;
                if (dist >= 16.5 && dist <= 17.5)
                    block(es, x, mirrorY, z, BlockTypes.CRYING_OBSIDIAN);
                else if (dist >= 10.5 && dist <= 11.5)
                    block(es, x, mirrorY, z, BlockTypes.OBSIDIAN);
                else
                    block(es, x, mirrorY, z, BlockTypes.OBSIDIAN);
            }
        }
        // 12 glowstone "pillars" at mirror ring positions
        for (int a = 0; a < 12; a++) {
            double rad = Math.toRadians(a * 30.0);
            int px = (int) Math.round(17 * Math.cos(rad));
            int pz = (int) Math.round(17 * Math.sin(rad));
            block(es, px, mirrorY + 1, pz, BlockTypes.GLOWSTONE);
        }

        // ── Easter Egg #17: Mossy Skull ─────────────────────────────────────
        // 5×4×5 stone skull carved near NW perimeter at (-72,SY,-72)
        int skx = -72, sky = SY, skz = -72;
        double ex17 = (double) skx / RX, ez17 = (double) skz / RZ;
        if (ex17 * ex17 + ez17 * ez17 <= 0.87) {
            // Skull base
            fill(es, skx-2, sky+1, skz-2, skx+2, sky+4, skz+2, BlockTypes.MOSSY_COBBLESTONE);
            // Eye sockets (AIR)
            block(es, skx-1, sky+3, skz-2, BlockTypes.CAVE_AIR);
            block(es, skx+1, sky+3, skz-2, BlockTypes.CAVE_AIR);
            block(es, skx-1, sky+3, skz+2, BlockTypes.CAVE_AIR);
            block(es, skx+1, sky+3, skz+2, BlockTypes.CAVE_AIR);
            // Glow in eye sockets
            block(es, skx-1, sky+3, skz-1, BlockTypes.GLOWSTONE);
            block(es, skx+1, sky+3, skz-1, BlockTypes.GLOWSTONE);
            // Nose cavity
            block(es, skx, sky+2, skz-2, BlockTypes.CAVE_AIR);
            // Mouth row
            for (int mx = skx-2; mx <= skx+2; mx++)
                block(es, mx, sky+1, skz-2, BlockTypes.CAVE_AIR);
            // Skull dome top
            fill(es, skx-1, sky+5, skz-1, skx+1, sky+5, skz+1, BlockTypes.MOSSY_COBBLESTONE);
            block(es, skx, sky+6, skz, BlockTypes.MOSSY_COBBLESTONE);
            // Mossy accent around base
            fill(es, skx-3, sky, skz-3, skx+3, sky, skz+3, BlockTypes.MOSS_BLOCK);
        }

        // ── Easter Egg #18: The Library ─────────────────────────────────────
        // Subterranean room at (0,SY-6,-40) — accessible from bow portal's path underside
        int lbx = 0, lby = SY - 8, lbz = -40;
        // Carve room 9×5×7
        fill(es, lbx-4, lby, lbz-3, lbx+4, lby+4, lbz+3, BlockTypes.CAVE_AIR);
        // Stone-brick walls
        fillHollow(es, lbx-4, lby, lbz-3, lbx+4, lby+4, lbz+3,
                   BlockTypes.CHISELED_STONE_BRICKS, BlockTypes.CAVE_AIR);
        // Bookshelf walls interior
        for (int y2 = lby + 1; y2 <= lby + 3; y2++) {
            for (int z2 = lbz-2; z2 <= lbz+2; z2++) {
                block(es, lbx-3, y2, z2, BlockTypes.BOOKSHELF);
                block(es, lbx+3, y2, z2, BlockTypes.BOOKSHELF);
            }
        }
        for (int y2 = lby + 1; y2 <= lby + 3; y2++) {
            for (int x2 = lbx-3; x2 <= lbx+3; x2++) {
                block(es, x2, y2, lbz-2, BlockTypes.BOOKSHELF);
            }
        }
        // Floor: polished deepslate
        fill(es, lbx-3, lby, lbz-2, lbx+3, lby, lbz+2, BlockTypes.POLISHED_DEEPSLATE);
        // Ceiling: glowstone strips
        fill(es, lbx-2, lby+4, lbz-1, lbx+2, lby+4, lbz+1, BlockTypes.GLOWSTONE);
        // Furniture
        block(es, lbx-2, lby+1, lbz,   BlockTypes.CRAFTING_TABLE);
        block(es, lbx+2, lby+1, lbz,   BlockTypes.BARREL);
        block(es, lbx,   lby+1, lbz+2, BlockTypes.CHEST);
        block(es, lbx,   lby+1, lbz-1, BlockTypes.CAULDRON);
        // Central reading table (slab stand-in)
        fill(es, lbx-1, lby+1, lbz-1, lbx+1, lby+1, lbz+1, BlockTypes.CHISELED_STONE_BRICKS);
        block(es, lbx, lby+2, lbz, BlockTypes.SEA_LANTERN);
        // Access tunnel from bow portal path below (z=-65 to z=-40, 1 block wide, at y=lby+2)
        for (int z2 = lbz - 3; z2 >= -60; z2--)
            block(es, 0, lby + 2, z2, BlockTypes.CAVE_AIR);

        // ── Easter Egg #19: Compass Rose ────────────────────────────────────
        // Hidden under the plaza floor at y=SY-1, visible only if floor is broken
        // N arm: IRON_BLOCK column at z=-5..0
        for (int z2 = -5; z2 <= 0; z2++)
            block(es, 0, SY - 1, z2, BlockTypes.IRON_BLOCK);
        // S arm
        for (int z2 = 0; z2 <= 5; z2++)
            block(es, 0, SY - 1, z2, BlockTypes.IRON_BLOCK);
        // E arm
        for (int x2 = 0; x2 <= 5; x2++)
            block(es, x2, SY - 1, 0, BlockTypes.IRON_BLOCK);
        // W arm
        for (int x2 = -5; x2 <= 0; x2++)
            block(es, x2, SY - 1, 0, BlockTypes.IRON_BLOCK);
        // Diagonal NE arm (GOLD → use GLOWSTONE)
        for (int i = 1; i <= 3; i++)
            block(es, i, SY - 1, -i, BlockTypes.GLOWSTONE);
        // Diagonal SW arm
        for (int i = 1; i <= 3; i++)
            block(es, -i, SY - 1, i, BlockTypes.GLOWSTONE);
        // Diagonal NW
        for (int i = 1; i <= 3; i++)
            block(es, -i, SY - 1, -i, BlockTypes.SEA_LANTERN);
        // Diagonal SE
        for (int i = 1; i <= 3; i++)
            block(es, i, SY - 1, i, BlockTypes.SEA_LANTERN);
        // Centre jewel
        block(es, 0, SY - 1, 0, BlockTypes.AMETHYST_BLOCK);

        // ── Easter Egg #20: The Gold Vault ──────────────────────────────────
        // Sealed room at (-5,42,5) — inside island body near main cave
        int gvx = -5, gvy = 42, gvz = 5;
        // Outer obsidian shell 5×5×5
        fillHollow(es, gvx-2, gvy-2, gvz-2, gvx+2, gvy+2, gvz+2,
                   BlockTypes.OBSIDIAN, BlockTypes.CAVE_AIR);
        // Inner gold lining on floor+ceiling (use GLOWSTONE as gold-visual)
        fill(es, gvx-1, gvy-2, gvz-1, gvx+1, gvy-2, gvz+1, BlockTypes.GLOWSTONE);
        fill(es, gvx-1, gvy+2, gvz-1, gvx+1, gvy+2, gvz+1, BlockTypes.GLOWSTONE);
        // Walls lined with IRON_BLOCK (like a bank vault)
        for (int y2 = gvy-1; y2 <= gvy+1; y2++) {
            block(es, gvx-2, y2, gvz, BlockTypes.IRON_BLOCK);
            block(es, gvx+2, y2, gvz, BlockTypes.IRON_BLOCK);
            block(es, gvx, y2, gvz-2, BlockTypes.IRON_BLOCK);
            block(es, gvx, y2, gvz+2, BlockTypes.IRON_BLOCK);
        }
        // Central chest on platform
        block(es, gvx, gvy-1, gvz, BlockTypes.IRON_BLOCK);
        block(es, gvx, gvy,   gvz, BlockTypes.CHEST);
        // Glowstone ceiling lights
        block(es, gvx-1, gvy+2, gvz-1, BlockTypes.GLOWSTONE);
        block(es, gvx+1, gvy+2, gvz+1, BlockTypes.GLOWSTONE);
        // Truly sealed — only reachable through the main cave
        // (cave AIR region overlaps at the vault walls — discoverable!)
    }

    // ============================================================================
    //  PORTAL PLAZA ELABORATIONS
    // ============================================================================

    /** Expands every portal platform with themed courtyard decorations. */
    private void buildPortalPlazas(EditSession es) {

        // ── Crystal Portal courtyard (E, cx=55, cz=0) ─────────────────────────
        // Amethyst-cluster garden ring at r=5 around platform centre
        for (int a = 0; a < 360; a += 20) {
            double rad = Math.toRadians(a);
            int px = 55 + (int)(5 * Math.cos(rad));
            int pz =  0 + (int)(5 * Math.sin(rad));
            double ex = (double)px/RX, ez = (double)pz/RZ;
            if (ex*ex+ez*ez > 0.94) continue;
            block(es, px, SY+1, pz, BlockTypes.AMETHYST_BLOCK);
            if (a % 40 == 0) block(es, px, SY+2, pz, BlockTypes.AMETHYST_CLUSTER);
        }
        // Purpur obelisk pair flanking approach path at (48,SY,±4)
        column(es, 48, -4, SY+1, SY+7, BlockTypes.PURPUR_PILLAR);
        column(es, 48,  4, SY+1, SY+7, BlockTypes.PURPUR_PILLAR);
        block(es, 48, SY+8, -4, BlockTypes.END_STONE_BRICKS);
        block(es, 48, SY+8,  4, BlockTypes.END_STONE_BRICKS);
        block(es, 48, SY+9, -4, BlockTypes.SEA_LANTERN);
        block(es, 48, SY+9,  4, BlockTypes.SEA_LANTERN);
        // End-stone wall low barrier around portal pad (3 sides)
        for (int z2 = -5; z2 <= 5; z2++) {
            block(es, 52, SY+1, z2, BlockTypes.END_STONE_BRICKS);
        }
        for (int x2 = 52; x2 <= 58; x2++) {
            block(es, x2, SY+1, -5, BlockTypes.END_STONE_BRICKS);
            block(es, x2, SY+1,  5, BlockTypes.END_STONE_BRICKS);
        }
        // Amethyst geode pillars at 4 corners of pad
        for (int[] c : new int[][]{{52,-5},{58,-5},{52,5},{58,5}}) {
            column(es, c[0], c[1], SY+2, SY+4, BlockTypes.AMETHYST_BLOCK);
            block(es, c[0], SY+5, c[1], BlockTypes.AMETHYST_CLUSTER);
        }
        // END_STONE_BRICKS checkerboard courtyard 10×10 behind portal (x=62..72)
        for (int x2 = 62; x2 <= 72; x2++) {
            for (int z2 = -5; z2 <= 5; z2++) {
                double ex2 = (double)x2/RX, ez2 = (double)z2/RZ;
                if (ex2*ex2+ez2*ez2 > 0.92) continue;
                block(es, x2, SY, z2, (x2+z2)%2==0 ? BlockTypes.END_STONE_BRICKS : BlockTypes.PURPUR_BLOCK);
            }
        }

        // ── Sword Portal courtyard (W, cx=-55, cz=0) ──────────────────────────
        // Quartz pillar row along north side of pad
        for (int z2 = -5; z2 <= 5; z2 += 2) {
            column(es, -52, z2, SY+1, SY+4, BlockTypes.QUARTZ_PILLAR);
            block(es, -52, SY+5, z2, BlockTypes.QUARTZ_BRICKS);
        }
        // Iron block fortification corners
        for (int[] c : new int[][]{{-52,-5},{-58,-5},{-52,5},{-58,5}}) {
            fill(es, c[0]-1, SY+1, c[1]-1, c[0]+1, SY+3, c[1]+1, BlockTypes.IRON_BLOCK);
            block(es, c[0], SY+4, c[1], BlockTypes.SEA_LANTERN);
        }
        // Smooth-quartz extended courtyard
        for (int x2 = -62; x2 >= -72; x2--) {
            for (int z2 = -5; z2 <= 5; z2++) {
                double ex2 = (double)x2/RX, ez2 = (double)z2/RZ;
                if (ex2*ex2+ez2*ez2 > 0.92) continue;
                block(es, x2, SY, z2, (x2+z2)%2==0 ? BlockTypes.SMOOTH_QUARTZ : BlockTypes.QUARTZ_BRICKS);
            }
        }
        // Iron-bar fence line at portal's back
        for (int z2 = -6; z2 <= 6; z2++)
            block(es, -62, SY+1, z2, BlockTypes.IRON_BARS);

        // ── Mace Portal courtyard (S, cx=0, cz=65) ────────────────────────────
        // Nether-brick wall low barrier 3 sides
        for (int x2 = -5; x2 <= 5; x2++) {
            block(es, x2, SY+1, 62, BlockTypes.NETHER_BRICKS);
        }
        for (int z2 = 62; z2 <= 70; z2++) {
            block(es, -5, SY+1, z2, BlockTypes.RED_NETHER_BRICKS);
            block(es,  5, SY+1, z2, BlockTypes.RED_NETHER_BRICKS);
        }
        // Magma-block ground pockets (2×2 each)
        for (int[] m : new int[][]{{-4,62},{4,62},{-4,70},{4,70}}) {
            fill(es, m[0]-1, SY, m[1]-1, m[0]+1, SY, m[1]+1, BlockTypes.MAGMA_BLOCK);
        }
        // Soul-sand courtyard behind portal
        for (int z2 = 70; z2 <= 80; z2++) {
            for (int x2 = -5; x2 <= 5; x2++) {
                double ex2 = (double)x2/RX, ez2 = (double)z2/RZ;
                if (ex2*ex2+ez2*ez2 > 0.92) continue;
                block(es, x2, SY, z2, (x2+z2)%3==0 ? BlockTypes.SOUL_SOIL : BlockTypes.SOUL_SAND);
            }
        }
        // Glowstone corner torches
        for (int[] c : new int[][]{{-5,62},{5,62},{-5,70},{5,70}}) {
            column(es, c[0], c[1], SY+2, SY+4, BlockTypes.RED_NETHER_BRICKS);
            block(es, c[0], SY+5, c[1], BlockTypes.GLOWSTONE);
        }

        // ── Bow Portal courtyard (N, cx=0, cz=-65) ────────────────────────────
        // Mossy-stone barrier 3 sides
        for (int x2 = -5; x2 <= 5; x2++)
            block(es, x2, SY+1, -62, BlockTypes.MOSSY_STONE_BRICKS);
        for (int z2 = -62; z2 >= -70; z2--) {
            block(es, -5, SY+1, z2, BlockTypes.MOSSY_COBBLESTONE);
            block(es,  5, SY+1, z2, BlockTypes.MOSSY_COBBLESTONE);
        }
        // Dark-oak log border posts every 2 blocks
        for (int z2 = -62; z2 >= -70; z2 -= 4) {
            column(es, -6, z2, SY+1, SY+4, BlockTypes.DARK_OAK_LOG);
            column(es,  6, z2, SY+1, SY+4, BlockTypes.DARK_OAK_LOG);
            block(es, -6, SY+5, z2, BlockTypes.LANTERN);
            block(es,  6, SY+5, z2, BlockTypes.LANTERN);
        }
        // Leaf canopy behind portal
        for (int z2 = -70; z2 >= -78; z2--) {
            for (int x2 = -5; x2 <= 5; x2++) {
                double ex2=(double)x2/RX, ez2=(double)z2/RZ;
                if (ex2*ex2+ez2*ez2>0.93) continue;
                block(es, x2, SY, z2, BlockTypes.MOSSY_STONE_BRICKS);
            }
        }
        fill(es, -4, SY+8, -72, 4, SY+8, -66, BlockTypes.DARK_OAK_LEAVES);
        fill(es, -3, SY+9, -72, 3, SY+9, -66, BlockTypes.DARK_OAK_LEAVES);

        // ── Totem Portal courtyard (NE, cx=38, cz=-45) ────────────────────────
        // Jungle-log fence perimeter
        for (int a = 0; a < 360; a += 20) {
            double rad = Math.toRadians(a);
            int px = 38 + (int)(7 * Math.cos(rad));
            int pz = -45 + (int)(7 * Math.sin(rad));
            double ex2=(double)px/RX, ez2=(double)pz/RZ;
            if (ex2*ex2+ez2*ez2>0.90) continue;
            block(es, px, SY+1, pz, BlockTypes.JUNGLE_LOG);
        }
        // Bamboo grove south of portal
        for (int bx = 34; bx <= 42; bx += 3) {
            for (int bz = -38; bz <= -32; bz += 3) {
                double ex2=(double)bx/RX, ez2=(double)bz/RZ;
                if (ex2*ex2+ez2*ez2>0.88) continue;
                column(es, bx, bz, SY+1, SY+8, BlockTypes.BAMBOO);
            }
        }
        // Jungle-leaves overhead canopy at SY+12
        fill(es, 34, SY+12, -50, 42, SY+12, -40, BlockTypes.JUNGLE_LEAVES);
        fill(es, 35, SY+13, -49, 41, SY+13, -41, BlockTypes.JUNGLE_LEAVES);
        // Hanging roots below canopy
        for (int hx = 34; hx <= 42; hx += 2)
            for (int hz = -50; hz <= -40; hz += 2)
                block(es, hx, SY+11, hz, BlockTypes.HANGING_ROOTS);

        // ── Axe Portal courtyard (NW, cx=-38, cz=-45) ─────────────────────────
        // Spruce-plank low platform extension
        for (int x2 = -44; x2 >= -52; x2--) {
            for (int z2 = -50; z2 <= -40; z2++) {
                double ex2=(double)x2/RX, ez2=(double)z2/RZ;
                if (ex2*ex2+ez2*ez2>0.90) continue;
                block(es, x2, SY, z2, BlockTypes.SPRUCE_PLANKS);
            }
        }
        // Spruce-fence perimeter of extension
        for (int x2 = -44; x2 >= -52; x2--) {
            double ex2=(double)x2/RX, ez2=(double)(-50)/RZ;
            if (ex2*ex2+ez2*ez2<=0.90) block(es, x2, SY+1, -50, BlockTypes.SPRUCE_FENCE);
            ez2=(double)(-40)/RZ;
            if (ex2*ex2+ez2*ez2<=0.90) block(es, x2, SY+1, -40, BlockTypes.SPRUCE_FENCE);
        }
        // Snow-accent blocks (use WHITE: SMOOTH_QUARTZ as stand-in for snow carpet)
        for (int[] s : new int[][]{{-45,-46},{-47,-43},{-50,-48},{-42,-42},{-49,-41}})
            block(es, s[0], SY+1, s[1], BlockTypes.SMOOTH_QUARTZ);
        // Cobblestone posts
        for (int[] p : new int[][]{{-44,-50},{-52,-50},{-44,-40},{-52,-40}}) {
            double ex2=(double)p[0]/RX, ez2=(double)p[1]/RZ;
            if (ex2*ex2+ez2*ez2>0.90) continue;
            column(es, p[0], p[1], SY+1, SY+4, BlockTypes.COBBLESTONE);
            block(es, p[0], SY+5, p[1], BlockTypes.LANTERN);
        }

        // ── Trident Portal courtyard (SE, cx=38, cz=55) ───────────────────────
        // Prismarine dock extending from portal south
        for (int z2 = 60; z2 <= 72; z2++) {
            for (int x2 = 34; x2 <= 42; x2++) {
                double ex2=(double)x2/RX, ez2=(double)z2/RZ;
                if (ex2*ex2+ez2*ez2>0.90) continue;
                block(es, x2, SY, z2, BlockTypes.PRISMARINE_BRICKS);
            }
        }
        // Dark-prismarine border
        for (int z2 = 60; z2 <= 72; z2++) {
            double ez2=(double)z2/RZ;
            double ex2a=(double)34/RX, ex2b=(double)42/RX;
            if (ex2a*ex2a+ez2*ez2<=0.90) block(es, 34, SY+1, z2, BlockTypes.DARK_PRISMARINE);
            if (ex2b*ex2b+ez2*ez2<=0.90) block(es, 42, SY+1, z2, BlockTypes.DARK_PRISMARINE);
        }
        // Sea-lantern posts at dock corners
        for (int[] c : new int[][]{{34,60},{42,60},{34,72},{42,72}}) {
            double ex2=(double)c[0]/RX, ez2=(double)c[1]/RZ;
            if (ex2*ex2+ez2*ez2>0.90) continue;
            column(es, c[0], c[1], SY+1, SY+4, BlockTypes.PRISMARINE_BRICKS);
            block(es, c[0], SY+5, c[1], BlockTypes.SEA_LANTERN);
        }
        // Water pools alongside dock (simulating ocean)
        fill(es, 43, SY-1, 62, 48, SY-1, 70, BlockTypes.WATER);
        fill(es, 43, SY-2, 62, 48, SY-2, 70, BlockTypes.DARK_PRISMARINE);
        fill(es, 43, SY,   62, 48, SY,   70, BlockTypes.AIR);
        block(es, 45, SY-2, 66, BlockTypes.SEA_LANTERN);
        // Lily pads on water
        for (int[] l : new int[][]{{43,63},{44,67},{46,64},{47,69},{45,62}})
            block(es, l[0], SY, l[1], BlockTypes.LILY_PAD);

        // ── Shield Portal courtyard (SW, cx=-38, cz=55) ───────────────────────
        // Oxidized copper terrace
        for (int x2 = -44; x2 >= -52; x2--) {
            for (int z2 = 50; z2 <= 62; z2++) {
                double ex2=(double)x2/RX, ez2=(double)z2/RZ;
                if (ex2*ex2+ez2*ez2>0.90) continue;
                com.sk89q.worldedit.world.block.BlockType mat;
                if ((x2+z2)%4==0)       mat = BlockTypes.OXIDIZED_CUT_COPPER;
                else if ((x2+z2)%4==1)  mat = BlockTypes.WEATHERED_CUT_COPPER;
                else if ((x2+z2)%4==2)  mat = BlockTypes.CUT_COPPER;
                else                    mat = BlockTypes.COPPER_BLOCK;
                block(es, x2, SY, z2, mat);
            }
        }
        // Copper fence posts
        for (int[] cp : new int[][]{{-44,50},{-52,50},{-44,62},{-52,62}}) {
            double ex2=(double)cp[0]/RX, ez2=(double)cp[1]/RZ;
            if (ex2*ex2+ez2*ez2>0.90) continue;
            column(es, cp[0], cp[1], SY+1, SY+5, BlockTypes.COPPER_BLOCK);
            block(es, cp[0], SY+6, cp[1], BlockTypes.LANTERN);
        }
        // Weathered-copper walls
        for (int x2 = -44; x2 >= -52; x2--) {
            double ex2=(double)x2/RX;
            double ez2a=(double)50/RZ, ez2b=(double)62/RZ;
            if (ex2*ex2+ez2a*ez2a<=0.90) block(es,x2,SY+1,50,BlockTypes.WEATHERED_COPPER);
            if (ex2*ex2+ez2b*ez2b<=0.90) block(es,x2,SY+1,62,BlockTypes.WEATHERED_COPPER);
        }
        for (int z2 = 50; z2 <= 62; z2++) {
            double ez2=(double)z2/RZ;
            double ex2a=(double)(-52)/RX;
            if (ex2a*ex2a+ez2*ez2<=0.90) block(es,-52,SY+1,z2,BlockTypes.OXIDIZED_COPPER);
        }
    }

    // ============================================================================
    //  AMBIENT DETAIL — fills dead space with micro-features
    // ============================================================================

    /** Scatters ambient detail across the mid-ring and open areas of the island. */
    private void buildAmbientDetail(EditSession es) {

        // ── Scattered crafting stations at ring-road nodes ─────────────────────
        int[][] craftStations = {{30,-6},{-30,6},{6,36},{-6,-36},{30,6},{-30,-6}};
        for (int[] cs : craftStations) {
            double ex=(double)cs[0]/RX, ez=(double)cs[1]/RZ;
            if (ex*ex+ez*ez > 0.88) continue;
            block(es, cs[0], SY+1, cs[1],   BlockTypes.CRAFTING_TABLE);
            block(es, cs[0]+1, SY+1, cs[1], BlockTypes.BARREL);
        }

        // ── Extra hanging lantern arches between secondary hub plazas ──────────
        // East hub (32,0) to ring road mid — chain arch
        for (int x = 26; x <= 32; x++) {
            int archY = SY + 6 - Math.abs(x - 29);
            block(es, x, archY, 0, BlockTypes.CHAIN);
            if (x == 26 || x == 32) {
                block(es, x, SY+7, 0, BlockTypes.CHAIN);
                block(es, x, SY+8, 0, BlockTypes.LANTERN);
            }
        }
        // West hub (-32,0)
        for (int x = -32; x <= -26; x++) {
            int archY = SY + 6 - Math.abs(x + 29);
            block(es, x, archY, 0, BlockTypes.CHAIN);
            if (x == -32 || x == -26) {
                block(es, x, SY+7, 0, BlockTypes.CHAIN);
                block(es, x, SY+8, 0, BlockTypes.LANTERN);
            }
        }
        // North hub (0,-32)
        for (int z = -32; z <= -26; z++) {
            int archY = SY + 6 - Math.abs(z + 29);
            block(es, 0, archY, z, BlockTypes.CHAIN);
            if (z == -32 || z == -26) {
                block(es, 0, SY+7, z, BlockTypes.CHAIN);
                block(es, 0, SY+8, z, BlockTypes.LANTERN);
            }
        }
        // South hub (0,32)
        for (int z = 26; z <= 32; z++) {
            int archY = SY + 6 - Math.abs(z - 29);
            block(es, 0, archY, z, BlockTypes.CHAIN);
            if (z == 26 || z == 32) {
                block(es, 0, SY+7, z, BlockTypes.CHAIN);
                block(es, 0, SY+8, z, BlockTypes.LANTERN);
            }
        }

        // ── Mossy patches in shaded areas under trees ─────────────────────────
        int[][] mossUnderTrees = {
            {35,-22},{45,-37},{55,-52},{42,-67},{62,-42},{52,-82},
            {70,-27},{78,-57},{35,-92},{48,-30},{60,-57},{38,-74}
        };
        for (int[] m : mossUnderTrees) {
            double ex=(double)m[0]/RX, ez=(double)m[1]/RZ;
            if (ex*ex+ez*ez > 0.89) continue;
            fill(es, m[0]-2, SY, m[1]-2, m[0]+2, SY, m[1]+2, BlockTypes.MOSS_BLOCK);
            fill(es, m[0]-2, SY+1, m[1]-2, m[0]+2, SY+1, m[1]+2, BlockTypes.MOSS_CARPET);
        }

        // ── Additional fern rings around watchtower bases ─────────────────────
        int[][] wtBases = {{65,50},{-65,50},{65,-60},{-65,-60},{0,95},{0,-95}};
        for (int[] wt : wtBases) {
            double ex=(double)wt[0]/RX, ez=(double)wt[1]/RZ;
            if (ex*ex+ez*ez > 0.92) continue;
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                int fx = wt[0] + (int)(5 * Math.cos(rad));
                int fz = wt[1] + (int)(5 * Math.sin(rad));
                double ex2=(double)fx/RX, ez2=(double)fz/RZ;
                if (ex2*ex2+ez2*ez2 > 0.92) continue;
                block(es, fx, SY+1, fz, a%60==0 ? BlockTypes.FERN : BlockTypes.SHORT_GRASS);
            }
        }

        // ── Prismarine feature pools at ring-road corners ─────────────────────
        int[][] rPools = {{20,25},{-20,-25},{20,-25},{-20,25}};
        for (int[] rp : rPools) {
            double ex=(double)rp[0]/RX, ez=(double)rp[1]/RZ;
            if (ex*ex+ez*ez > 0.85) continue;
            // 3×3 pool
            fill(es, rp[0]-1, SY-1, rp[1]-1, rp[0]+1, SY-1, rp[1]+1, BlockTypes.WATER);
            fill(es, rp[0]-1, SY-2, rp[1]-1, rp[0]+1, SY-2, rp[1]+1, BlockTypes.PRISMARINE);
            fill(es, rp[0]-1, SY,   rp[1]-1, rp[0]+1, SY,   rp[1]+1, BlockTypes.AIR);
            block(es, rp[0], SY-2, rp[1], BlockTypes.SEA_LANTERN);
            for (int x2=rp[0]-1;x2<=rp[0]+1;x2++) {
                block(es, x2, SY, rp[1]-1, BlockTypes.STONE_BRICKS);
                block(es, x2, SY, rp[1]+1, BlockTypes.STONE_BRICKS);
            }
            block(es, rp[0]-1, SY, rp[1], BlockTypes.STONE_BRICKS);
            block(es, rp[0]+1, SY, rp[1], BlockTypes.STONE_BRICKS);
        }

        // ── Signpost obelisks at island cardinal extremes ─────────────────────
        // These point to portals from the perimeter
        int[][] extremes = {{82,0},{-82,0},{0,100},{0,-100}};
        for (int[] ep : extremes) {
            double ex=(double)ep[0]/RX, ez=(double)ep[1]/RZ;
            if (ex*ex+ez*ez > 0.96) continue;
            column(es, ep[0], ep[1], SY+1, SY+5, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, ep[0], SY+6, ep[1], BlockTypes.SEA_LANTERN);
            block(es, ep[0], SY+7, ep[1], BlockTypes.CHAIN);
            block(es, ep[0], SY+8, ep[1], BlockTypes.LANTERN);
        }

        // ── Waterfall cascade on tower north face (decorative) ─────────────────
        // Water ribbon cascading down tower exterior at z=-9
        for (int y = SY + 20; y >= SY + 2; y--) {
            if ((y - SY) % 5 == 0)
                block(es, 0, y, -9, BlockTypes.WATER);
        }
        // Catch pool at base
        block(es, 0, SY - 1, -12, BlockTypes.WATER);
        block(es, 0, SY - 2, -12, BlockTypes.PRISMARINE);
        fill(es, -1, SY, -14, 1, SY, -11, BlockTypes.STONE_BRICKS);
        block(es, 0, SY, -12, BlockTypes.AIR);
        block(es, 0, SY - 1, -12, BlockTypes.WATER);

        // ── Hedge rows (DARK_OAK_LEAVES) separating zones ────────────────────
        // Hedge between forest and arena zones at x=0, z=-15..-30
        for (int z = -18; z >= -28; z--) {
            block(es, 0, SY+1, z, BlockTypes.DARK_OAK_LEAVES);
            block(es, 0, SY+2, z, BlockTypes.DARK_OAK_LEAVES);
            block(es, 1, SY+1, z, BlockTypes.DARK_OAK_LEAVES);
        }
        // Hedge between water and ruins at z=10, x=-5..5
        for (int x = -8; x <= 8; x++) {
            block(es, x, SY+1, 12, BlockTypes.OAK_LEAVES);
            block(es, x, SY+2, 12, BlockTypes.OAK_LEAVES);
        }

        // ── Extra lamp posts in mid-island open areas ─────────────────────────
        int[][] extraLamps = {
            {-5,42},{5,42},{-5,-42},{5,-42},{42,5},{42,-5},{-42,5},{-42,-5},
            {-16,-32},{16,-32},{-16,32},{16,32},{32,-16},{32,16},{-32,-16},{-32,16}
        };
        for (int[] lp : extraLamps) {
            double ex=(double)lp[0]/RX, ez=(double)lp[1]/RZ;
            if (ex*ex+ez*ez > 0.90) continue;
            column(es, lp[0], lp[1], SY+1, SY+3, BlockTypes.OAK_FENCE);
            block(es, lp[0], SY+4, lp[1], BlockTypes.LANTERN);
        }

        // ── Birch-plank gazebo at (0, SY, -30) ────────────────────────────────
        // 5×5 roof of BIRCH_LEAVES on OAK_FENCE posts at corners, between N hub and bow path
        for (int[] corner : new int[][]{{-2,-32},{2,-32},{-2,-28},{2,-28}}) {
            column(es, corner[0], corner[1], SY+1, SY+4, BlockTypes.OAK_FENCE);
        }
        fill(es, -3, SY+5, -33, 3, SY+5, -27, BlockTypes.BIRCH_LEAVES);
        fill(es, -2, SY+6, -32, 2, SY+6, -28, BlockTypes.BIRCH_LEAVES);
        block(es, 0, SY+7, -30, BlockTypes.BIRCH_LEAVES);
        fill(es, -2, SY, -32, 2, SY, -28, BlockTypes.SPRUCE_PLANKS);

        // ── Mirror gazebo at (0, SY, 30) — same structure south side ──────────
        for (int[] corner : new int[][]{{-2,28},{2,28},{-2,32},{2,32}}) {
            column(es, corner[0], corner[1], SY+1, SY+4, BlockTypes.OAK_FENCE);
        }
        fill(es, -3, SY+5, 27, 3, SY+5, 33, BlockTypes.CHERRY_LEAVES);
        fill(es, -2, SY+6, 28, 2, SY+6, 32, BlockTypes.CHERRY_LEAVES);
        block(es, 0, SY+7, 30, BlockTypes.CHERRY_LEAVES);
        fill(es, -2, SY, 28, 2, SY, 32, BlockTypes.SPRUCE_PLANKS);
    }

    // ============================================================================
    //  NIGHT AMBIENCE — dense glow source scatter for atmospheric lighting
    // ============================================================================

    /** Places shroomlights, sea lanterns and glowstone throughout the island so
     *  the spawn looks alive at night and from a distance. */
    private void buildNightAmbience(EditSession es) {
        // ── Sea-lantern discs embedded in the main tower at each floor ────────
        // Ground floor ring at SY+1 just inside inner wall
        for (int a = 0; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            int lx = (int)(6 * Math.cos(rad));
            int lz = (int)(6 * Math.sin(rad));
            block(es, lx, SY + 1, lz, BlockTypes.SEA_LANTERN);
        }
        // Mid body at SY+21 inset just inside
        for (int a = 22; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            int lx = (int)(5 * Math.cos(rad));
            int lz = (int)(5 * Math.sin(rad));
            block(es, lx, SY + 21, lz, BlockTypes.SEA_LANTERN);
        }
        // Observation deck at SY+39
        for (int a = 0; a < 360; a += 60) {
            double rad = Math.toRadians(a);
            int lx = (int)(4 * Math.cos(rad));
            int lz = (int)(4 * Math.sin(rad));
            block(es, lx, SY + 39, lz, BlockTypes.GLOWSTONE);
        }

        // ── Shroomlight scatter in forest zone ────────────────────────────────
        int[][] shroomPos = {
            {38,-18},{50,-32},{65,-48},{42,-65},{72,-26},{56,-80},{78,-52},
            {44,-90},{35,-42},{60,-22},{48,-55},{70,-38},{55,-72},{40,-28}
        };
        for (int[] s : shroomPos) {
            double ex=(double)s[0]/RX, ez=(double)s[1]/RZ;
            if (ex*ex+ez*ez > 0.87) continue;
            // Place shroomlight on the underside of tree canopies (at ground level if no tree)
            block(es, s[0], SY+1, s[1], BlockTypes.SHROOMLIGHT);
        }

        // ── Glowstone veins exposed on perimeter wall top ─────────────────────
        for (int a = 5; a < 360; a += 36) {
            double rad = Math.toRadians(a);
            int wx = (int)(78 * Math.cos(rad));
            int wz = (int)(93 * Math.sin(rad));
            double ex=(double)wx/RX, ez=(double)wz/RZ;
            if (ex*ex+ez*ez > 0.95) continue;
            block(es, wx, SY+5, wz, BlockTypes.GLOWSTONE);
        }

        // ── Sea lanterns mounted flush in watchtower walls ────────────────────
        int[][] wtw = {{65,50},{-65,50},{65,-60},{-65,-60},{0,95},{0,-95}};
        for (int[] wt : wtw) {
            double ex=(double)wt[0]/RX, ez=(double)wt[1]/RZ;
            if (ex*ex+ez*ez > 0.93) continue;
            // One sea lantern on each face of the watchtower at y=SY+11
            block(es, wt[0],   SY+11, wt[1]-3, BlockTypes.SEA_LANTERN);
            block(es, wt[0],   SY+11, wt[1]+3, BlockTypes.SEA_LANTERN);
            block(es, wt[0]-3, SY+11, wt[1],   BlockTypes.SEA_LANTERN);
            block(es, wt[0]+3, SY+11, wt[1],   BlockTypes.SEA_LANTERN);
        }

        // ── Glow accents inside arena zone ────────────────────────────────────
        // Glowstone embedded in arena floor at cardinal points inside octagon
        for (int a = 0; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            int px = -50 + (int)(18 * Math.cos(rad));
            int pz = -50 + (int)(18 * Math.sin(rad));
            block(es, px, SY, pz, BlockTypes.SEA_LANTERN);
        }
        // Arena ceiling chains with lanterns
        for (int a = 0; a < 360; a += 90) {
            double rad = Math.toRadians(a);
            int px = -50 + (int)(8 * Math.cos(rad));
            int pz = -50 + (int)(8 * Math.sin(rad));
            block(es, px, SY+5, pz, BlockTypes.CHAIN);
            block(es, px, SY+4, pz, BlockTypes.LANTERN);
        }

        // ── Floating glow orbs near each portal (lantern on chain) ───────────
        int[][] portalGlow = {
            {55,0,7},{-55,0,7},{0,65,7},{0,-65,7},{38,-45,7},{-38,-45,7},{38,55,7},{-38,55,7}
        };
        for (int[] pg : portalGlow) {
            double ex=(double)pg[0]/RX, ez=(double)pg[1]/RZ;
            if (ex*ex+ez*ez > 0.92) continue;
            block(es, pg[0], SY+pg[2]+1, pg[1], BlockTypes.CHAIN);
            block(es, pg[0], SY+pg[2]+2, pg[1], BlockTypes.CHAIN);
            block(es, pg[0], SY+pg[2]+3, pg[1], BlockTypes.GLOWSTONE);
        }

        // ── Underground cave glow accents (extra) ─────────────────────────────
        // Extra glowstone and shroomlight spots in cave ceiling
        for (int[] gs : new int[][]{{-5,50,10},{8,56,-8},{-12,55,12},{6,56,16},{-16,54,-10}}) {
            block(es, gs[0], gs[1], gs[2], BlockTypes.SHROOMLIGHT);
        }
        // Glowstone stripe at cave waterfall source in eastern cave
        block(es, 52, 54,  0, BlockTypes.GLOWSTONE);
        block(es, 52, 53,  0, BlockTypes.GLOWSTONE);
        block(es, 56, 54,  4, BlockTypes.SEA_LANTERN);
        block(es, 56, 54, -4, BlockTypes.SEA_LANTERN);

        // ── Beacon beam base (extra iron-block layer) ─────────────────────────
        // Widen the beacon base one more layer to 5×5 of iron
        fill(es, -2, SY-4, -2, 2, SY-4, 2, BlockTypes.IRON_BLOCK);

        // ── Perimeter wall glow strip at base ─────────────────────────────────
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a);
            int wx = (int)(78 * Math.cos(rad));
            int wz = (int)(93 * Math.sin(rad));
            double ex=(double)wx/RX, ez=(double)wz/RZ;
            if (ex*ex+ez*ez > 0.95) continue;
            if (a % 18 == 0) block(es, wx, SY-1, wz, BlockTypes.SEA_LANTERN);
        }
    }

    // ============================================================================
    //  TOWER EXTERIOR DETAIL — decorative banding, window recesses, buttresses
    // ============================================================================

    /** Adds buttresses, gargoyle posts, decorative banding and window-frame
     *  detail to the exterior of the main tower. */
    private void buildDetailedTowerExterior(EditSession es) {

        // ── 4 flying buttresses, one per face ─────────────────────────────────
        // Each buttress: a diagonal arm from the tower wall to the plaza at radius 16
        // North buttress: z=-9 face, slants to z=-16
        for (int step = 0; step <= 6; step++) {
            int bz = -(9 + step);
            int by = SY + 10 - step;
            fill(es, -1, by, bz, 1, by, bz, BlockTypes.DEEPSLATE_BRICKS);
        }
        block(es, 0, SY+4, -15, BlockTypes.CHISELED_DEEPSLATE);
        column(es, 0, -15, SY+1, SY+4, BlockTypes.DEEPSLATE_BRICKS);

        // South buttress
        for (int step = 0; step <= 6; step++) {
            int bz = 9 + step;
            int by = SY + 10 - step;
            fill(es, -1, by, bz, 1, by, bz, BlockTypes.DEEPSLATE_BRICKS);
        }
        block(es, 0, SY+4, 15, BlockTypes.CHISELED_DEEPSLATE);
        column(es, 0, 15, SY+1, SY+4, BlockTypes.DEEPSLATE_BRICKS);

        // East buttress
        for (int step = 0; step <= 6; step++) {
            int bx = 9 + step;
            int by = SY + 10 - step;
            fill(es, bx, by, -1, bx, by, 1, BlockTypes.DEEPSLATE_BRICKS);
        }
        block(es, 15, SY+4, 0, BlockTypes.CHISELED_DEEPSLATE);
        column(es, 15, 0, SY+1, SY+4, BlockTypes.DEEPSLATE_BRICKS);

        // West buttress
        for (int step = 0; step <= 6; step++) {
            int bx = -(9 + step);
            int by = SY + 10 - step;
            fill(es, bx, by, -1, bx, by, 1, BlockTypes.DEEPSLATE_BRICKS);
        }
        block(es, -15, SY+4, 0, BlockTypes.CHISELED_DEEPSLATE);
        column(es, -15, 0, SY+1, SY+4, BlockTypes.DEEPSLATE_BRICKS);

        // ── Decorative banding: quartz_bricks strip around tower at every 7y ──
        for (int y = SY + 7; y <= SY + 35; y += 7) {
            for (int x = -9; x <= 9; x++) {
                block(es, x, y, -9, BlockTypes.QUARTZ_BRICKS);
                block(es, x, y,  9, BlockTypes.QUARTZ_BRICKS);
            }
            for (int z = -8; z <= 8; z++) {
                block(es, -9, y, z, BlockTypes.QUARTZ_BRICKS);
                block(es,  9, y, z, BlockTypes.QUARTZ_BRICKS);
            }
        }

        // ── Gargoyle posts at upper-tier corners (SY+34) ──────────────────────
        for (int[] gc : new int[][]{{-9,-9},{9,-9},{-9,9},{9,9}}) {
            block(es, gc[0], SY+34, gc[1], BlockTypes.CHISELED_DEEPSLATE);
            block(es, gc[0], SY+35, gc[1], BlockTypes.DEEPSLATE_BRICKS);
            block(es, gc[0], SY+36, gc[1], BlockTypes.CHISELED_DEEPSLATE);
            block(es, gc[0], SY+37, gc[1], BlockTypes.SEA_LANTERN);
            // "Wing" stubs
            int ox = gc[0] < 0 ? -1 : 1, oz = gc[1] < 0 ? -1 : 1;
            block(es, gc[0]+ox, SY+35, gc[1],    BlockTypes.DEEPSLATE_BRICKS);
            block(es, gc[0],    SY+35, gc[1]+oz, BlockTypes.DEEPSLATE_BRICKS);
        }

        // ── Window recesses: 1-deep alcoves around tinted glass windows ───────
        // Lower body windows on north face (z=-9, x=-3,0,3) at y=SY+5..6
        for (int wx : new int[]{-3, 0, 3}) {
            block(es, wx, SY+5, -10, BlockTypes.POLISHED_DEEPSLATE);
            block(es, wx, SY+6, -10, BlockTypes.POLISHED_DEEPSLATE);
            block(es, wx-1, SY+5, -9, BlockTypes.CHISELED_DEEPSLATE);
            block(es, wx+1, SY+5, -9, BlockTypes.CHISELED_DEEPSLATE);
            block(es, wx-1, SY+6, -9, BlockTypes.CHISELED_DEEPSLATE);
            block(es, wx+1, SY+6, -9, BlockTypes.CHISELED_DEEPSLATE);
            block(es, wx,   SY+7, -9, BlockTypes.CHISELED_DEEPSLATE);
        }
        // South face
        for (int wx : new int[]{-3, 0, 3}) {
            block(es, wx, SY+5, 10, BlockTypes.POLISHED_DEEPSLATE);
            block(es, wx, SY+6, 10, BlockTypes.POLISHED_DEEPSLATE);
            block(es, wx-1, SY+5, 9, BlockTypes.CHISELED_DEEPSLATE);
            block(es, wx+1, SY+5, 9, BlockTypes.CHISELED_DEEPSLATE);
        }
        // East face
        for (int wz : new int[]{-3, 0, 3}) {
            block(es, 10, SY+5, wz, BlockTypes.POLISHED_DEEPSLATE);
            block(es, 10, SY+6, wz, BlockTypes.POLISHED_DEEPSLATE);
        }
        // West face
        for (int wz : new int[]{-3, 0, 3}) {
            block(es, -10, SY+5, wz, BlockTypes.POLISHED_DEEPSLATE);
            block(es, -10, SY+6, wz, BlockTypes.POLISHED_DEEPSLATE);
        }

        // ── Arched window frames on mid-body (SY+14..20) ─────────────────────
        // North face arch frame
        for (int y = SY+14; y <= SY+20; y++) {
            block(es, -2, y, -10, BlockTypes.CHISELED_DEEPSLATE);
            block(es,  2, y, -10, BlockTypes.CHISELED_DEEPSLATE);
        }
        block(es, -1, SY+21, -10, BlockTypes.CHISELED_DEEPSLATE);
        block(es,  1, SY+21, -10, BlockTypes.CHISELED_DEEPSLATE);
        block(es,  0, SY+22, -10, BlockTypes.QUARTZ_BRICKS);
        // South face arch frame
        for (int y = SY+14; y <= SY+20; y++) {
            block(es, -2, y, 10, BlockTypes.CHISELED_DEEPSLATE);
            block(es,  2, y, 10, BlockTypes.CHISELED_DEEPSLATE);
        }
        block(es, -1, SY+21, 10, BlockTypes.CHISELED_DEEPSLATE);
        block(es,  1, SY+21, 10, BlockTypes.CHISELED_DEEPSLATE);
        block(es,  0, SY+22, 10, BlockTypes.QUARTZ_BRICKS);

        // ── Spire cross-bracing at quartz pillar (SY+44..SY+53) ──────────────
        for (int y = SY+44; y <= SY+53; y += 3) {
            // 4 CHAIN "braces" extending out 2 from spire centre
            block(es,  2, y, 0, BlockTypes.CHAIN);
            block(es, -2, y, 0, BlockTypes.CHAIN);
            block(es, 0, y,  2, BlockTypes.CHAIN);
            block(es, 0, y, -2, BlockTypes.CHAIN);
            // QUARTZ_BRICKS node at each end
            if (y % 6 == 0) {
                block(es,  3, y, 0, BlockTypes.QUARTZ_BRICKS);
                block(es, -3, y, 0, BlockTypes.QUARTZ_BRICKS);
                block(es, 0, y,  3, BlockTypes.QUARTZ_BRICKS);
                block(es, 0, y, -3, BlockTypes.QUARTZ_BRICKS);
            }
        }

        // ── Exterior wall moss lines (age effect) ─────────────────────────────
        // Scattered MOSSY_STONE_BRICKS-look: place moss blocks at random wall positions
        for (int y = SY+2; y <= SY+18; y++) {
            if ((y * 7) % 11 == 0) {
                block(es, -9, y, (y % 5) - 2, BlockTypes.MOSSY_COBBLESTONE);
                block(es,  9, y, 2-(y % 5),   BlockTypes.MOSSY_COBBLESTONE);
            }
            if ((y * 11) % 13 == 0) {
                block(es, (y % 5)-2, y, -9, BlockTypes.MOSS_CARPET);
                block(es, 2-(y%5),   y,  9, BlockTypes.MOSS_CARPET);
            }
        }
    }

    // ============================================================================
    //  ISLAND SURFACE MICRO-TERRAIN — extra noise, paths, pits and detail fills
    // ============================================================================

    /** Adds final micro-terrain variation: potholes, surface veins, dirt trails,
     *  decorative pit rings, and scattered dripstone on the open island surface. */
    private void buildSurfaceMicroTerrain(EditSession es) {

        // ── Shallow potholes scattered mid-island (1-block deep) ─────────────
        int[][] potholes = {
            { 18,-42},{ 22,-50},{-24, 45},{-18, 38},{ 44,-18},{ 38,-24},
            {-44, 22},{-38, 16},{ 52, 20},{ 48, 14},{-50,-18},{-46,-12},
            { 20, 44},{ 16, 50},{-20,-44},{-14,-50},{ 62,-8},{-60,  8},
            { 10,-58},{ -8, 62},{  6, 68},{ -4,-68},{ 70,-45},{-68, 46}
        };
        for (int[] ph : potholes) {
            double ex=(double)ph[0]/RX, ez=(double)ph[1]/RZ;
            if (ex*ex+ez*ez > 0.88) continue;
            block(es, ph[0],   SY,     ph[1],   BlockTypes.COARSE_DIRT);
            block(es, ph[0]+1, SY,     ph[1],   BlockTypes.GRAVEL);
            block(es, ph[0],   SY,     ph[1]+1, BlockTypes.GRAVEL);
            block(es, ph[0]-1, SY - 1, ph[1],   BlockTypes.CAVE_AIR);
            block(es, ph[0],   SY - 1, ph[1]-1, BlockTypes.CAVE_AIR);
        }

        // ── Stone vein outcrops on flat open areas (5-block linear runs) ──────
        int[][] veins = {
            { 32,-60,  0}, {-32, 62,  0}, { 58,-20,  1}, {-56, 22,  1},
            { 20,-76,  0}, {-20, 78,  0}, { 68,-28,  1}, {-66, 30,  1},
            { 46, 36,  0}, {-44,-34,  0}, { 74, 12,  1}, {-72,-10,  1}
        };
        for (int[] v : veins) {
            double ex=(double)v[0]/RX, ez=(double)v[1]/RZ;
            if (ex*ex+ez*ez > 0.88) continue;
            for (int i = 0; i < 5; i++) {
                int vx = v[0] + (v[2]==0 ? i : 0);
                int vz = v[1] + (v[2]==1 ? i : 0);
                block(es, vx, SY+1, vz, (i % 2 == 0) ? BlockTypes.ANDESITE : BlockTypes.STONE);
            }
        }

        // ── Dripstone columns on open plateau areas (NW mid-section) ─────────
        int[][] dripCols = {
            {-30,-25},{-35,-30},{-28,-38},{-42,-22},{-38,-36},{-26,-44},
            {-48,-28},{-32,-50},{-44,-42},{-22,-55},{-52,-16},{-36,-56}
        };
        for (int[] dc : dripCols) {
            double ex=(double)dc[0]/RX, ez=(double)dc[1]/RZ;
            if (ex*ex+ez*ez > 0.88) continue;
            int h = 1 + Math.abs((dc[0]+dc[1]) % 3);
            for (int i = 0; i < h; i++)
                block(es, dc[0], SY+1+i, dc[1], BlockTypes.POINTED_DRIPSTONE);
            block(es, dc[0], SY, dc[1], BlockTypes.DRIPSTONE_BLOCK);
        }

        // ── Dirt "trail" connecting main structures informally ─────────────────
        // Worn dirt path from forest clearing (58,-32) toward ruins (55,45): diagonal
        int trailSteps = 40;
        for (int s = 0; s <= trailSteps; s++) {
            double frac = (double)s / trailSteps;
            int tx2 = 58 + (int)((55-58)*frac);
            int tz2 = -32 + (int)((45+32)*frac);
            double ex=(double)tx2/RX, ez=(double)tz2/RZ;
            if (ex*ex+ez*ez > 0.90) continue;
            block(es, tx2,   SY, tz2,   BlockTypes.COARSE_DIRT);
            if (s % 3 != 0) block(es, tx2+1, SY, tz2, BlockTypes.DIRT);
        }

        // ── Calcite rings marking invisible ley-lines across island ───────────
        // 3 rings of calcite blocks at surface level as mysterious ground markings
        for (int a = 0; a < 360; a += 15) {
            double rad = Math.toRadians(a);
            // Ring 1: radius 35
            int r1x = (int)(35 * Math.cos(rad)), r1z = (int)(35 * Math.sin(rad));
            double ex1=(double)r1x/RX, ez1=(double)r1z/RZ;
            if (ex1*ex1+ez1*ez1 <= 0.87) block(es, r1x, SY+1, r1z, BlockTypes.CALCITE);
            // Ring 2: radius 55
            int r2x = (int)(55 * Math.cos(rad)), r2z = (int)(58 * Math.sin(rad));
            double ex2=(double)r2x/RX, ez2=(double)r2z/RZ;
            if (ex2*ex2+ez2*ez2 <= 0.87) block(es, r2x, SY+1, r2z, BlockTypes.CALCITE);
        }

        // ── Mud patches in lower areas near water features ─────────────────────
        int[][] mudAreas = {
            {-25,42},{-30,38},{-22,50},{-35,45},{-28,52},
            {-42,35},{-40,48},{-20,58},{-32,56},{-45,40}
        };
        for (int[] ma : mudAreas) {
            double ex=(double)ma[0]/RX, ez=(double)ma[1]/RZ;
            if (ex*ex+ez*ez > 0.85) continue;
            block(es, ma[0], SY, ma[1], BlockTypes.PACKED_MUD);
            if ((ma[0]+ma[1]) % 3 == 0)
                block(es, ma[0]+1, SY, ma[1], BlockTypes.MUD_BRICKS);
        }

        // ── Basalt spike field east of arena entrance ─────────────────────────
        int[][] basaltField = {
            {-20,-42},{-24,-46},{-18,-50},{-22,-54},{-26,-44},{-16,-46},
            {-20,-56},{-24,-38},{-28,-52},{-14,-50},{-22,-62},{-18,-38}
        };
        for (int[] bf : basaltField) {
            double ex=(double)bf[0]/RX, ez=(double)bf[1]/RZ;
            if (ex*ex+ez*ez > 0.87) continue;
            int h = 1 + Math.abs((bf[0]+bf[1]) % 4);
            column(es, bf[0], bf[1], SY+1, SY+h, BlockTypes.BASALT);
            block(es, bf[0], SY+h+1, bf[1], BlockTypes.SMOOTH_BASALT);
        }

        // ── Smooth-stone pavement patches near portals ────────────────────────
        int[][] smoothPatches = {
            {50,-2},{50,2},{-50,-2},{-50,2},{-2,58},{2,58},{-2,-58},{2,-58}
        };
        for (int[] sp : smoothPatches) {
            double ex=(double)sp[0]/RX, ez=(double)sp[1]/RZ;
            if (ex*ex+ez*ez > 0.90) continue;
            fill(es, sp[0]-2, SY, sp[1]-2, sp[0]+2, SY, sp[1]+2, BlockTypes.SMOOTH_STONE);
        }
    }

    // ============================================================================
    //  WELCOME ARCH — grand entrance arch above the main north staircase
    // ============================================================================

    /** Builds a grand 13-wide × 11-tall welcome arch directly above the central
     *  plaza's north exit staircase, framing the view toward the Bow portal. */
    private void buildWelcomeArch(EditSession es) {
        int cx = 0, cz = -22;

        // Left pillar: x=-6, z=cz, y=SY+1..SY+11
        column(es, cx - 6, cz, SY + 1, SY + 11, BlockTypes.DEEPSLATE_BRICKS);
        // Right pillar: x=+6
        column(es, cx + 6, cz, SY + 1, SY + 11, BlockTypes.DEEPSLATE_BRICKS);

        // Alternating quartz accent every 3rd block
        for (int y = SY + 1; y <= SY + 11; y += 3) {
            block(es, cx - 6, y, cz, BlockTypes.CHISELED_DEEPSLATE);
            block(es, cx + 6, y, cz, BlockTypes.CHISELED_DEEPSLATE);
        }

        // Arch crown: spans from x=-6 to x=+6 at y=SY+12, curved profile
        int[] archProfile = {SY+12, SY+13, SY+13, SY+14, SY+14, SY+13, SY+13, SY+12};
        for (int i = 0; i < 8; i++) {
            int ax = cx - 4 + i;
            block(es, ax, archProfile[i], cz, BlockTypes.QUARTZ_BRICKS);
        }
        // Crown keystone
        block(es, cx, SY + 15, cz, BlockTypes.CHISELED_QUARTZ_BLOCK);
        block(es, cx, SY + 16, cz, BlockTypes.CHAIN);
        block(es, cx, SY + 17, cz, BlockTypes.LANTERN);

        // Wing blocks filling above pillar tops below arch curve
        block(es, cx - 5, SY + 12, cz, BlockTypes.DEEPSLATE_BRICKS);
        block(es, cx + 5, SY + 12, cz, BlockTypes.DEEPSLATE_BRICKS);

        // Pillar base plinths (3×1×3 POLISHED_DEEPSLATE pads at SY)
        fill(es, cx-7, SY, cz-1, cx-5, SY, cz+1, BlockTypes.POLISHED_DEEPSLATE);
        fill(es, cx+5, SY, cz-1, cx+7, SY, cz+1, BlockTypes.POLISHED_DEEPSLATE);

        // Flaming chain-lanterns flanking the arch at y=SY+6 protruding outward 2
        block(es, cx - 8, SY + 7, cz, BlockTypes.CHAIN);
        block(es, cx - 8, SY + 6, cz, BlockTypes.LANTERN);
        block(es, cx + 8, SY + 7, cz, BlockTypes.CHAIN);
        block(es, cx + 8, SY + 6, cz, BlockTypes.LANTERN);

        // Banner-post stand-ins: OAK_FENCE + IRON_BARS flag substitute
        column(es, cx - 9, cz, SY + 1, SY + 5, BlockTypes.OAK_FENCE);
        block(es, cx - 9, SY + 6, cz, BlockTypes.IRON_BARS);
        block(es, cx - 9, SY + 7, cz, BlockTypes.IRON_BARS);
        column(es, cx + 9, cz, SY + 1, SY + 5, BlockTypes.OAK_FENCE);
        block(es, cx + 9, SY + 6, cz, BlockTypes.IRON_BARS);
        block(es, cx + 9, SY + 7, cz, BlockTypes.IRON_BARS);

        // Mirror arch above south staircase (cz=+22)
        int scz = 22;
        column(es, cx - 6, scz, SY + 1, SY + 11, BlockTypes.DEEPSLATE_BRICKS);
        column(es, cx + 6, scz, SY + 1, SY + 11, BlockTypes.DEEPSLATE_BRICKS);
        for (int y = SY + 1; y <= SY + 11; y += 3) {
            block(es, cx - 6, y, scz, BlockTypes.CHISELED_DEEPSLATE);
            block(es, cx + 6, y, scz, BlockTypes.CHISELED_DEEPSLATE);
        }
        for (int i = 0; i < 8; i++) {
            int ax = cx - 4 + i;
            block(es, ax, archProfile[i], scz, BlockTypes.QUARTZ_BRICKS);
        }
        block(es, cx, SY + 15, scz, BlockTypes.CHISELED_QUARTZ_BLOCK);
        block(es, cx, SY + 16, scz, BlockTypes.CHAIN);
        block(es, cx, SY + 17, scz, BlockTypes.LANTERN);
        fill(es, cx-7, SY, scz-1, cx-5, SY, scz+1, BlockTypes.POLISHED_DEEPSLATE);
        fill(es, cx+5, SY, scz-1, cx+7, SY, scz+1, BlockTypes.POLISHED_DEEPSLATE);
        block(es, cx - 8, SY + 7, scz, BlockTypes.CHAIN);
        block(es, cx - 8, SY + 6, scz, BlockTypes.LANTERN);
        block(es, cx + 8, SY + 7, scz, BlockTypes.CHAIN);
        block(es, cx + 8, SY + 6, scz, BlockTypes.LANTERN);

        // Matching east/west arches above east (cz=0,cx=22) and west (cx=-22) exits
        for (int sign : new int[]{1, -1}) {
            int ecx = sign * 22, ecz = 0;
            column(es, ecx, ecz - 6, SY + 1, SY + 11, BlockTypes.DEEPSLATE_BRICKS);
            column(es, ecx, ecz + 6, SY + 1, SY + 11, BlockTypes.DEEPSLATE_BRICKS);
            for (int y = SY + 1; y <= SY + 11; y += 3) {
                block(es, ecx, y, ecz - 6, BlockTypes.CHISELED_DEEPSLATE);
                block(es, ecx, y, ecz + 6, BlockTypes.CHISELED_DEEPSLATE);
            }
            for (int i = 0; i < 8; i++) {
                int az = ecz - 4 + i;
                block(es, ecx, archProfile[i], az, BlockTypes.QUARTZ_BRICKS);
            }
            block(es, ecx, SY + 15, ecz, BlockTypes.CHISELED_QUARTZ_BLOCK);
            block(es, ecx, SY + 16, ecz, BlockTypes.CHAIN);
            block(es, ecx, SY + 17, ecz, BlockTypes.LANTERN);
            // Flanking post lanterns either side of each E/W arch
            block(es, ecx, SY + 7, ecz - 8, BlockTypes.CHAIN);
            block(es, ecx, SY + 6, ecz - 8, BlockTypes.LANTERN);
            block(es, ecx, SY + 7, ecz + 8, BlockTypes.CHAIN);
            block(es, ecx, SY + 6, ecz + 8, BlockTypes.LANTERN);
            // Plinth pads at arch base
            fill(es, ecx - 1, SY, ecz - 7, ecx + 1, SY, ecz - 5, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, ecx - 1, SY, ecz + 5, ecx + 1, SY, ecz + 7, BlockTypes.POLISHED_DEEPSLATE);
            // Tuff corner accent on each plinth
            block(es, ecx, SY + 1, ecz - 7, BlockTypes.TUFF);
            block(es, ecx, SY + 1, ecz + 7, BlockTypes.TUFF);
        }
    }

    // ============================================================================
    //  BUILD MANIFEST
    // ============================================================================
    //
    //  Total methods called from build():
    //   buildIslandBase            buildIslandUnderbelly
    //   buildCentralPlaza          buildCentralFountains
    //   buildMainTower             buildTowerInterior
    //   buildSecondaryHubs
    //   buildCrystalPortal         buildSwordPortal
    //   buildMacePortal            buildBowPortal
    //   buildTotemPortal           buildAxePortal
    //   buildTridentPortal         buildShieldPortal
    //   buildGrandStaircase        buildRingRoad
    //   buildPathways              buildBridges
    //   buildForestZone            buildWaterGardens
    //   buildArenaZone             buildRuinsZone
    //   buildVegetation
    //   buildPerimeterWalls        buildWatchtowers
    //   buildLampPosts             buildUndergroundCaves
    //   buildFinalAccents  ──► calls:
    //       buildDetailedIslandEdges   buildSceneryDetails
    //       buildSkyStructures         buildIslandCliffs
    //       buildMineralDeposits       buildPortalPlazas
    //       buildAmbientDetail         buildNightAmbience
    //       buildDetailedTowerExterior buildSurfaceMicroTerrain
    //       buildWelcomeArch           buildEasterEggsExtra
    //
    //  Easter eggs (20 total):
    //   #1  Pixel Duck       #2  Nerd Room         #3  LemonPvP Heart
    //   #4  Void Mirror      #5  Tiny Village       #6  Floating Arrow
    //   #7  Underground Rave #8  Chess Crown        #9  Derp Face
    //   #10 Counting Room    #11 LemonPvP Logo      #12 Time Capsule
    //   #13 Star Map         #14 The Aquarium       #15 Gravity Pillar
    //   #16 Mirror Plaza     #17 Mossy Skull        #18 The Library
    //   #19 Compass Rose     #20 The Gold Vault
    //
}
