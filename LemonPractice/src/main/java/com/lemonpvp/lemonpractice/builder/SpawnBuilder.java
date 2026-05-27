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
