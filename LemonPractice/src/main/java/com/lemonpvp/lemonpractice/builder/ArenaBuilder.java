package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * Builds all practice and FFA arenas for LemonPvP.
 *
 * Practice arenas (8 total, 1v1 themed, 120×120 platforms):
 *   Crystal Arena  @ (300,  64,    0) — End Stone / Amethyst
 *   Sword Arena    @ (-300, 64,    0) — Quartz / Iron coliseum
 *   Mace Arena     @ (0,    64,  300) — Nether Brick / Magma / Fire
 *   Bow Arena      @ (0,    64, -300) — Forest / Nature / Terrain
 *   Totem Arena    @ (220,  64, -220) — Jungle / Bamboo / Ruins
 *   Axe Arena      @ (-220, 64, -220) — Cave / Deepslate / Mining
 *   Trident Arena  @ (220,  64,  220) — Ocean / Prismarine / Water
 *   Shield Arena   @ (-220, 64,  220) — Fortress / Iron / Battleground
 *
 * FFA arenas (5 total, multi-player, 140×140 platforms):
 *   Volcano FFA    @ (500,  64,    0) — Lava / Nether / Basalt
 *   Ice Palace FFA @ (-500, 64,    0) — Calcite / Dripstone / Cold
 *   Ancient Ruins  @ (0,    64,  500) — Mossy Stone / Crumbling / Overgrown
 *   Sky Temple FFA @ (0,    64, -500) — Quartz / Floating / Beacon
 *   Dark Forest FFA@ (400,  64,  400) — Dark Oak / Mycelium / Shroomlight
 */
public class ArenaBuilder extends BuildHelper {

    private static final int AY = 64; // arena floor Y

    public ArenaBuilder(LemonPractice plugin, org.bukkit.World world) {
        super(plugin, world);
    }

    @Override
    public void build() {
        // Practice arenas
        buildCrystalArena();
        buildSwordArena();
        buildMaceArena();
        buildBowArena();
        buildTotemArena();
        buildAxeArena();
        buildTridentArena();
        buildShieldArena();
        // FFA arenas
        buildVolcanoFFA();
        buildIcePalaceFFA();
        buildAncientRuinsFFA();
        buildSkyTempleFFA();
        buildDarkForestFFA();
    }

    // helper: open a fresh EditSession
    private EditSession newSession() {
        return WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .fastMode(true)
                .build();
    }

    // =========================================================================
    // PLATFORM HELPER
    // =========================================================================

    /**
     * Builds the shared floating platform base for all arenas.
     *
     * @param es      active EditSession
     * @param cx      centre X of the arena
     * @param cz      centre Z of the arena
     * @param halfW   half-width in X (platform extends cx-halfW .. cx+halfW)
     * @param halfD   half-depth in Z (platform extends cz-halfD .. cz+halfD)
     * @param surface block placed on the top surface at y=64
     * @param fill    block used to fill y=60..63 (the platform body)
     * @param base    block used for the bevelled underside rim and stalactite effect
     */
    private void buildPlatform(EditSession es, int cx, int cz,
                               int halfW, int halfD,
                               com.sk89q.worldedit.world.block.BlockType surface,
                               com.sk89q.worldedit.world.block.BlockType fill,
                               com.sk89q.worldedit.world.block.BlockType base) {
        // ---- solid body y=60..63 ----
        fill(es, cx - halfW, 60, cz - halfD,
                 cx + halfW, 63, cz + halfD, fill);

        // ---- top surface at y=64 ----
        fill(es, cx - halfW, AY, cz - halfD,
                 cx + halfW, AY, cz + halfD, surface);

        // ---- 1-block overhang rim at y=59 (just outside the fill footprint) ----
        // North rim (z = cz - halfD - 1)
        fill(es, cx - halfW - 1, 59, cz - halfD - 1,
                 cx + halfW + 1, 59, cz - halfD - 1, base);
        // South rim
        fill(es, cx - halfW - 1, 59, cz + halfD + 1,
                 cx + halfW + 1, 59, cz + halfD + 1, base);
        // West rim
        fill(es, cx - halfW - 1, 59, cz - halfD,
                 cx - halfW - 1, 59, cz + halfD, base);
        // East rim
        fill(es, cx + halfW + 1, 59, cz - halfD,
                 cx + halfW + 1, 59, cz + halfD, base);

        // ---- underside stalactite effect: shrinking disks ----
        // y=59: disk radius = max(halfW, halfD) - 1
        int r0 = Math.max(halfW, halfD);
        disk(es, cx, 59, cz, r0,     base);
        disk(es, cx, 58, cz, r0 - 2, base);
        disk(es, cx, 57, cz, r0 - 4, base);
        disk(es, cx, 56, cz, r0 - 6, base);
    }

    // =========================================================================
    // CRYSTAL ARENA  cx=300, cz=0
    // =========================================================================
    private void buildCrystalArena() {
        final int cx = 300, cz = 0;

        try (EditSession es = newSession()) {

            // ------------------------------------------------------------------
            // Platform base: 130×130 footprint (halfW=65, halfD=65)
            // Fill y=60..63 with END_STONE_BRICKS, surface END_STONE_BRICKS
            // ------------------------------------------------------------------
            buildPlatform(es, cx, cz, 65, 65,
                    BlockTypes.END_STONE_BRICKS,
                    BlockTypes.END_STONE_BRICKS,
                    BlockTypes.PURPUR_BLOCK);

            // ------------------------------------------------------------------
            // FLOOR  (120×120 interior: cx-60..cx+60, cz-60..cz+60, y=64)
            // ------------------------------------------------------------------
            // Checkerboard of POLISHED_DEEPSLATE and PURPUR_BLOCK
            for (int x = cx - 60; x <= cx + 60; x++) {
                for (int z = cz - 60; z <= cz + 60; z++) {
                    if ((x + z) % 2 == 0) {
                        block(es, x, AY, z, BlockTypes.POLISHED_DEEPSLATE);
                    } else {
                        block(es, x, AY, z, BlockTypes.PURPUR_BLOCK);
                    }
                }
            }

            // Outer 5-block border (y=64): overwrite with pure PURPUR_BLOCK
            // North band
            fill(es, cx - 65, AY, cz - 65, cx + 65, AY, cz - 61, BlockTypes.PURPUR_BLOCK);
            // South band
            fill(es, cx - 65, AY, cz + 61, cx + 65, AY, cz + 65, BlockTypes.PURPUR_BLOCK);
            // West band
            fill(es, cx - 65, AY, cz - 60, cx - 61, AY, cz + 60, BlockTypes.PURPUR_BLOCK);
            // East band
            fill(es, cx + 61, AY, cz - 60, cx + 65, AY, cz + 60, BlockTypes.PURPUR_BLOCK);

            // Amethyst ring at radius ~55: every 5 blocks around the perimeter of the interior
            for (int angle = 0; angle < 360; angle += 5) {
                double rad = Math.toRadians(angle);
                int rx = cx + (int) Math.round(55 * Math.cos(rad));
                int rz = cz + (int) Math.round(55 * Math.sin(rad));
                block(es, rx, AY, rz, BlockTypes.AMETHYST_BLOCK);
            }

            // SEA_LANTERN embedded in floor in a grid every 15 blocks
            for (int x = cx - 45; x <= cx + 45; x += 15) {
                for (int z = cz - 45; z <= cz + 45; z += 15) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // Centre floor marker
            block(es, cx, AY, cz, BlockTypes.BUDDING_AMETHYST);

            // 8 compass-point floor markers at r=25
            int[][] compassOffsets = {
                {25, 0}, {-25, 0}, {0, 25}, {0, -25},
                {18, 18}, {-18, 18}, {18, -18}, {-18, -18}
            };
            for (int[] off : compassOffsets) {
                block(es, cx + off[0], AY, cz + off[1], BlockTypes.BUDDING_AMETHYST);
            }

            // ------------------------------------------------------------------
            // WALLS  y=65..78 (14 tall)
            // ------------------------------------------------------------------
            // Outer shell: END_STONE_BRICKS fill between columns
            fill(es, cx - 61, 65, cz - 61, cx + 61, 78, cz - 61, BlockTypes.END_STONE_BRICKS); // N
            fill(es, cx - 61, 65, cz + 61, cx + 61, 78, cz + 61, BlockTypes.END_STONE_BRICKS); // S
            fill(es, cx - 61, 65, cz - 61, cx - 61, 78, cz + 61, BlockTypes.END_STONE_BRICKS); // W
            fill(es, cx + 61, 65, cz - 61, cx + 61, 78, cz + 61, BlockTypes.END_STONE_BRICKS); // E

            // PURPUR_PILLAR columns every 6 blocks along each wall face
            for (int x = cx - 60; x <= cx + 60; x += 6) {
                column(es, x, cz - 61, 65, 78, BlockTypes.PURPUR_PILLAR);
                column(es, x, cz + 61, 65, 78, BlockTypes.PURPUR_PILLAR);
            }
            for (int z = cz - 60; z <= cz + 60; z += 6) {
                column(es, cx - 61, z, 65, 78, BlockTypes.PURPUR_PILLAR);
                column(es, cx + 61, z, 65, 78, BlockTypes.PURPUR_PILLAR);
            }

            // TINTED_GLASS panels on inner face (cx±60, cz±60): 2 wide × 10 tall between each pair of pillars
            for (int x = cx - 60; x <= cx + 60; x++) {
                // Only fill glass between pillar positions (skip pillar cols)
                if (x % 6 != 0) {
                    fill(es, x, 67, cz - 60, x, 76, cz - 60, BlockTypes.TINTED_GLASS);
                    fill(es, x, 67, cz + 60, x, 76, cz + 60, BlockTypes.TINTED_GLASS);
                }
            }
            for (int z = cz - 60; z <= cz + 60; z++) {
                if (z % 6 != 0) {
                    fill(es, cx - 60, 67, z, cx - 60, 76, z, BlockTypes.TINTED_GLASS);
                    fill(es, cx + 60, 67, z, cx + 60, 76, z, BlockTypes.TINTED_GLASS);
                }
            }

            // 4 arched gateways — 5-wide × 8-tall AIR openings at cardinal faces
            // North gateway (z = cz-61, centred at x=cx)
            fill(es, cx - 2, 65, cz - 61, cx + 2, 72, cz - 61, BlockTypes.AIR);
            fill(es, cx - 2, 65, cz - 60, cx + 2, 72, cz - 60, BlockTypes.AIR);
            // South gateway
            fill(es, cx - 2, 65, cz + 61, cx + 2, 72, cz + 61, BlockTypes.AIR);
            fill(es, cx - 2, 65, cz + 60, cx + 2, 72, cz + 60, BlockTypes.AIR);
            // West gateway
            fill(es, cx - 61, 65, cz - 2, cx - 61, 72, cz + 2, BlockTypes.AIR);
            fill(es, cx - 60, 65, cz - 2, cx - 60, 72, cz + 2, BlockTypes.AIR);
            // East gateway
            fill(es, cx + 61, 65, cz - 2, cx + 61, 72, cz + 2, BlockTypes.AIR);
            fill(es, cx + 60, 65, cz - 2, cx + 60, 72, cz + 2, BlockTypes.AIR);

            // Wall top MERLONS: alternating PURPUR_BLOCK / AIR every 2 blocks at y=79..80
            for (int x = cx - 61; x <= cx + 61; x += 2) {
                fill(es, x, 79, cz - 61, x, 80, cz - 61, BlockTypes.PURPUR_BLOCK);
                fill(es, x, 79, cz + 61, x, 80, cz + 61, BlockTypes.PURPUR_BLOCK);
            }
            for (int z = cz - 61; z <= cz + 61; z += 2) {
                fill(es, cx - 61, 79, z, cx - 61, 80, z, BlockTypes.PURPUR_BLOCK);
                fill(es, cx + 61, 79, z, cx + 61, 80, z, BlockTypes.PURPUR_BLOCK);
            }

            // ------------------------------------------------------------------
            // SPECTATOR LEDGE: 5-wide PURPUR_BLOCK on top of walls (y=79..80)
            // with IRON_BARS railing at y=81
            // ------------------------------------------------------------------
            // North ledge
            fill(es, cx - 61, 79, cz - 66, cx + 61, 80, cz - 62, BlockTypes.PURPUR_BLOCK);
            fill(es, cx - 61, 81, cz - 66, cx + 61, 81, cz - 62, BlockTypes.IRON_BARS);
            // South ledge
            fill(es, cx - 61, 79, cz + 62, cx + 61, 80, cz + 66, BlockTypes.PURPUR_BLOCK);
            fill(es, cx - 61, 81, cz + 62, cx + 61, 81, cz + 66, BlockTypes.IRON_BARS);
            // West ledge
            fill(es, cx - 66, 79, cz - 61, cx - 62, 80, cz + 61, BlockTypes.PURPUR_BLOCK);
            fill(es, cx - 66, 81, cz - 61, cx - 62, 81, cz + 61, BlockTypes.IRON_BARS);
            // East ledge
            fill(es, cx + 62, 79, cz - 61, cx + 66, 80, cz + 61, BlockTypes.PURPUR_BLOCK);
            fill(es, cx + 62, 81, cz - 61, cx + 66, 81, cz + 61, BlockTypes.IRON_BARS);

            // ------------------------------------------------------------------
            // CORNER TOWERS at cx±55, cz±55 — 7×7 PURPUR_PILLAR, 20 tall
            // ------------------------------------------------------------------
            int[][] towerCentres = {
                {cx + 55, cz + 55}, {cx - 55, cz + 55},
                {cx + 55, cz - 55}, {cx - 55, cz - 55}
            };
            for (int[] tc : towerCentres) {
                int tx = tc[0], tz = tc[1];
                // 7×7 tower body y=65..84
                fill(es, tx - 3, 65, tz - 3, tx + 3, 84, tz + 3, BlockTypes.PURPUR_PILLAR);
                // Hollow interior y=66..83
                fill(es, tx - 2, 66, tz - 2, tx + 2, 83, tz + 2, BlockTypes.AIR);
                // Octagonal cap at y=85: AMETHYST_BLOCK disk r=3 minus corners
                disk(es, tx, 85, tz, 3, BlockTypes.AMETHYST_BLOCK);
                block(es, tx - 3, 85, tz - 3, BlockTypes.PURPUR_PILLAR); // re-place corners as pillars
                block(es, tx + 3, 85, tz - 3, BlockTypes.PURPUR_PILLAR);
                block(es, tx - 3, 85, tz + 3, BlockTypes.PURPUR_PILLAR);
                block(es, tx + 3, 85, tz + 3, BlockTypes.PURPUR_PILLAR);
                // SEA_LANTERN on top
                block(es, tx, 86, tz, BlockTypes.SEA_LANTERN);
                // AMETHYST_CLUSTER accent rings every 5 blocks up the tower
                for (int ry = 70; ry <= 80; ry += 5) {
                    ring(es, tx, ry, tz, 3, 3, BlockTypes.AMETHYST_CLUSTER);
                }
            }

            // ------------------------------------------------------------------
            // INTERIOR OBSTACLES
            // ------------------------------------------------------------------
            // Inner ring: 4 pillars at r=18 (cardinal positions)
            int[][] innerPillarOffsets = {
                {18, 0}, {-18, 0}, {0, 18}, {0, -18}
            };
            for (int[] off : innerPillarOffsets) {
                int px = cx + off[0], pz = cz + off[1];
                fill(es, px - 1, AY, pz - 1, px + 1, AY + 7, pz + 1, BlockTypes.AMETHYST_BLOCK);
                // Tops: AMETHYST_CLUSTER on 4 sides + top face
                block(es, px,     AY + 8, pz,     BlockTypes.AMETHYST_CLUSTER);
                block(es, px + 1, AY + 7, pz,     BlockTypes.AMETHYST_CLUSTER);
                block(es, px - 1, AY + 7, pz,     BlockTypes.AMETHYST_CLUSTER);
                block(es, px,     AY + 7, pz + 1, BlockTypes.AMETHYST_CLUSTER);
                block(es, px,     AY + 7, pz - 1, BlockTypes.AMETHYST_CLUSTER);
                block(es, px - 1, AY + 7, pz - 1, BlockTypes.BUDDING_AMETHYST);
            }

            // Outer ring: 4 pillars at r=35 (diagonal NE/NW/SE/SW)
            int[][] outerPillarOffsets = {
                {25, 25}, {-25, 25}, {25, -25}, {-25, -25}
            };
            for (int[] off : outerPillarOffsets) {
                int px = cx + off[0], pz = cz + off[1];
                fill(es, px - 1, AY, pz - 1, px + 1, AY + 7, pz + 1, BlockTypes.AMETHYST_BLOCK);
                block(es, px,     AY + 8, pz,     BlockTypes.AMETHYST_CLUSTER);
                block(es, px + 1, AY + 7, pz,     BlockTypes.AMETHYST_CLUSTER);
                block(es, px - 1, AY + 7, pz,     BlockTypes.AMETHYST_CLUSTER);
                block(es, px,     AY + 7, pz + 1, BlockTypes.AMETHYST_CLUSTER);
                block(es, px,     AY + 7, pz - 1, BlockTypes.AMETHYST_CLUSTER);
                block(es, px + 1, AY + 7, pz + 1, BlockTypes.BUDDING_AMETHYST);
            }

            // Centre feature: 5×5 PURPUR_BLOCK raised 1 block (y=65)
            fill(es, cx - 2, 65, cz - 2, cx + 2, 65, cz + 2, BlockTypes.PURPUR_BLOCK);
            // BUDDING_AMETHYST centre top
            block(es, cx, 66, cz, BlockTypes.BUDDING_AMETHYST);
            // AMETHYST_CLUSTER x4 facing outward at y=66
            block(es, cx + 3, 66, cz, BlockTypes.AMETHYST_CLUSTER);
            block(es, cx - 3, 66, cz, BlockTypes.AMETHYST_CLUSTER);
            block(es, cx, 66, cz + 3, BlockTypes.AMETHYST_CLUSTER);
            block(es, cx, 66, cz - 3, BlockTypes.AMETHYST_CLUSTER);

            // 4 low END_STONE_BRICKS barriers (1-tall, 8-long, diagonal gaps between obstacles)
            fill(es, cx + 8,  AY, cz + 6,  cx + 15, AY, cz + 6,  BlockTypes.END_STONE_BRICKS);
            fill(es, cx - 15, AY, cz + 6,  cx - 8,  AY, cz + 6,  BlockTypes.END_STONE_BRICKS);
            fill(es, cx + 8,  AY, cz - 6,  cx + 15, AY, cz - 6,  BlockTypes.END_STONE_BRICKS);
            fill(es, cx - 15, AY, cz - 6,  cx - 8,  AY, cz - 6,  BlockTypes.END_STONE_BRICKS);

            // ------------------------------------------------------------------
            // LIGHTING: chains + lanterns at each gateway top
            // ------------------------------------------------------------------
            // North gateway lanterns
            for (int lx = cx - 2; lx <= cx + 2; lx += 2) {
                column(es, lx, cz - 61, 79, 80, BlockTypes.CHAIN);
                block(es, lx, 78, cz - 61, BlockTypes.LANTERN);
            }
            // South
            for (int lx = cx - 2; lx <= cx + 2; lx += 2) {
                column(es, lx, cz + 61, 79, 80, BlockTypes.CHAIN);
                block(es, lx, 78, cz + 61, BlockTypes.LANTERN);
            }
            // West
            for (int lz = cz - 2; lz <= cz + 2; lz += 2) {
                column(es, cx - 61, lz, 79, 80, BlockTypes.CHAIN);
                block(es, cx - 61, 78, lz, BlockTypes.LANTERN);
            }
            // East
            for (int lz = cz - 2; lz <= cz + 2; lz += 2) {
                column(es, cx + 61, lz, 79, 80, BlockTypes.CHAIN);
                block(es, cx + 61, 78, lz, BlockTypes.LANTERN);
            }

            // ------------------------------------------------------------------
            // SPAWN MARKERS: 3×3 CHISELED_DEEPSLATE pads + SEA_LANTERN centre
            // ------------------------------------------------------------------
            int[][] spawnPositions = {
                {cx + 45, cz},   {cx - 45, cz},
                {cx, cz + 45},   {cx, cz - 45}
            };
            for (int[] sp : spawnPositions) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1,
                        BlockTypes.CHISELED_DEEPSLATE);
                block(es, sp[0], AY, sp[1], BlockTypes.SEA_LANTERN);
            }

            // ------------------------------------------------------------------
            // PLATFORM UNDERSIDE: 3 AMETHYST_BLOCK cluster spheres hanging below
            // ------------------------------------------------------------------
            sphere(es, cx,       55, cz,      4, BlockTypes.AMETHYST_BLOCK);
            sphere(es, cx + 30,  55, cz - 20, 4, BlockTypes.AMETHYST_BLOCK);
            sphere(es, cx - 28,  55, cz + 22, 4, BlockTypes.AMETHYST_BLOCK);
        }
    }

    // =========================================================================
    // SWORD ARENA  cx=-300, cz=0
    // =========================================================================
    private void buildSwordArena() {
        final int cx = -300, cz = 0;

        try (EditSession es = newSession()) {

            // ------------------------------------------------------------------
            // Platform base 130×130
            // ------------------------------------------------------------------
            buildPlatform(es, cx, cz, 65, 65,
                    BlockTypes.SMOOTH_QUARTZ,
                    BlockTypes.STONE,
                    BlockTypes.IRON_BLOCK);

            // ------------------------------------------------------------------
            // FLOOR (120×120)
            // SMOOTH_QUARTZ overall, already set by platform surface
            // QUARTZ_BRICKS inset ring at r=50 (1-block wide annulus)
            // ------------------------------------------------------------------
            ring(es, cx, AY, cz, 49, 50, BlockTypes.QUARTZ_BRICKS);
            // CHISELED_QUARTZ_BLOCK inset at r=30
            ring(es, cx, AY, cz, 29, 30, BlockTypes.CHISELED_QUARTZ_BLOCK);

            // Floor inlays: CHISELED_QUARTZ_BLOCK cross N/S/E/W axes from centre to wall
            for (int x = cx - 60; x <= cx + 60; x++) {
                block(es, x, AY, cz, BlockTypes.CHISELED_QUARTZ_BLOCK);
            }
            for (int z = cz - 60; z <= cz + 60; z++) {
                block(es, cx, AY, z, BlockTypes.CHISELED_QUARTZ_BLOCK);
            }

            // GLOWSTONE embedded flush in floor every 10 blocks in grid
            for (int x = cx - 50; x <= cx + 50; x += 10) {
                for (int z = cz - 50; z <= cz + 50; z += 10) {
                    block(es, x, AY, z, BlockTypes.GLOWSTONE);
                }
            }

            // ------------------------------------------------------------------
            // COLISEUM WALLS: elliptical perimeter at radius=57
            // Iterate 0..359 degrees; every 12° = QUARTZ_PILLAR column, else QUARTZ_BRICKS
            // ------------------------------------------------------------------
            for (int angle = 0; angle < 360; angle++) {
                double rad = Math.toRadians(angle);
                int wx = cx + (int) Math.round(57 * Math.cos(rad));
                int wz = cz + (int) Math.round(57 * Math.sin(rad));

                if (angle % 12 == 0) {
                    column(es, wx, wz, 65, 80, BlockTypes.QUARTZ_PILLAR);
                } else {
                    column(es, wx, wz, 65, 80, BlockTypes.QUARTZ_BRICKS);
                }
            }

            // TINTED_GLASS windows: 2-wide × 10-tall between each pair of pillars
            // Place glass at inner radius=55 at non-pillar angles
            for (int angle = 1; angle < 360; angle++) {
                if (angle % 12 != 0) {
                    double rad = Math.toRadians(angle);
                    int gx = cx + (int) Math.round(55 * Math.cos(rad));
                    int gz = cz + (int) Math.round(55 * Math.sin(rad));
                    fill(es, gx, 67, gz, gx, 76, gz, BlockTypes.TINTED_GLASS);
                }
            }

            // Wall top BATTLEMENTS: CHISELED_QUARTZ_BLOCK merlons every 2 blocks at y=81..82
            for (int angle = 0; angle < 360; angle += 2) {
                double rad = Math.toRadians(angle);
                int mx = cx + (int) Math.round(57 * Math.cos(rad));
                int mz = cz + (int) Math.round(57 * Math.sin(rad));
                fill(es, mx, 81, mz, mx, 82, mz, BlockTypes.CHISELED_QUARTZ_BLOCK);
            }

            // ------------------------------------------------------------------
            // 4 GRAND ARCHED ENTRANCES (N/S/E/W): 9-wide × 12-tall arch carved in wall
            // ------------------------------------------------------------------
            // North entrance: angle~270° → z negative direction, centred at (cx, cz-57)
            for (int ox = -4; ox <= 4; ox++) {
                fill(es, cx + ox, 65, cz - 58, cx + ox, 76, cz - 56, BlockTypes.AIR);
            }
            // South entrance
            for (int ox = -4; ox <= 4; ox++) {
                fill(es, cx + ox, 65, cz + 56, cx + ox, 76, cz + 58, BlockTypes.AIR);
            }
            // West entrance
            for (int oz = -4; oz <= 4; oz++) {
                fill(es, cx - 58, 65, cz + oz, cx - 56, 76, cz + oz, BlockTypes.AIR);
            }
            // East entrance
            for (int oz = -4; oz <= 4; oz++) {
                fill(es, cx + 56, 65, cz + oz, cx + 58, 76, cz + oz, BlockTypes.AIR);
            }

            // ------------------------------------------------------------------
            // TIERED SPECTATOR SEATING (inside wall ring)
            // ------------------------------------------------------------------
            ring(es, cx, AY + 1, cz, 52, 54, BlockTypes.QUARTZ_BRICKS);  // Row 1 y=65
            ring(es, cx, AY + 2, cz, 54, 56, BlockTypes.SMOOTH_QUARTZ);  // Row 2 y=66
            ring(es, cx, AY + 3, cz, 56, 58, BlockTypes.QUARTZ_BRICKS);  // Row 3 y=67
            ring(es, cx, AY + 4, cz, 58, 60, BlockTypes.SMOOTH_QUARTZ);  // Row 4 y=68

            // ------------------------------------------------------------------
            // INTERIOR OBSTACLES
            // ------------------------------------------------------------------
            // Centre: 5×5 IRON_BLOCK raised dais at y=65
            fill(es, cx - 2, 65, cz - 2, cx + 2, 65, cz + 2, BlockTypes.IRON_BLOCK);
            // BEACON at centre top y=66
            block(es, cx, 66, cz, BlockTypes.BEACON);

            // 8 QUARTZ_PILLAR columns (2×2 base, 10 tall) in ring at r=22
            for (int i = 0; i < 8; i++) {
                double rad = Math.toRadians(i * 45.0);
                int px = cx + (int) Math.round(22 * Math.cos(rad));
                int pz = cz + (int) Math.round(22 * Math.sin(rad));
                fill(es, px, 65, pz, px + 1, 74, pz + 1, BlockTypes.QUARTZ_PILLAR);
                // Lamp post: OAK_FENCE 6 tall + LANTERN
                column(es, px, pz + 2, 65, 70, BlockTypes.OAK_FENCE);
                block(es, px, 71, pz + 2, BlockTypes.LANTERN);
            }

            // 4 SMOOTH_QUARTZ low walls (1-tall, 12-long) as + cross at r=15, each with 3-wide gap
            // N wall: z=cz-15, x=cx-6..cx-1 and cx+1..cx+6 (gap at cx-1..cx+1)
            fill(es, cx - 6, 65, cz - 15, cx - 1, 65, cz - 15, BlockTypes.SMOOTH_QUARTZ);
            fill(es, cx + 1, 65, cz - 15, cx + 6, 65, cz - 15, BlockTypes.SMOOTH_QUARTZ);
            // S wall
            fill(es, cx - 6, 65, cz + 15, cx - 1, 65, cz + 15, BlockTypes.SMOOTH_QUARTZ);
            fill(es, cx + 1, 65, cz + 15, cx + 6, 65, cz + 15, BlockTypes.SMOOTH_QUARTZ);
            // W wall
            fill(es, cx - 15, 65, cz - 6, cx - 15, 65, cz - 1, BlockTypes.SMOOTH_QUARTZ);
            fill(es, cx - 15, 65, cz + 1, cx - 15, 65, cz + 6, BlockTypes.SMOOTH_QUARTZ);
            // E wall
            fill(es, cx + 15, 65, cz - 6, cx + 15, 65, cz - 1, BlockTypes.SMOOTH_QUARTZ);
            fill(es, cx + 15, 65, cz + 1, cx + 15, 65, cz + 6, BlockTypes.SMOOTH_QUARTZ);

            // 4 IRON_BARS fence barriers at r=35 (8-long, cardinal directions)
            fill(es, cx + 31, AY, cz - 4, cx + 38, AY, cz - 4, BlockTypes.IRON_BARS);
            fill(es, cx - 38, AY, cz - 4, cx - 31, AY, cz - 4, BlockTypes.IRON_BARS);
            fill(es, cx - 4, AY, cz + 31, cx - 4, AY, cz + 38, BlockTypes.IRON_BARS);
            fill(es, cx - 4, AY, cz - 38, cx - 4, AY, cz - 31, BlockTypes.IRON_BARS);

            // ------------------------------------------------------------------
            // SPAWN MARKERS
            // ------------------------------------------------------------------
            int[][] spawnPositions = {
                {cx + 40, cz},  {cx - 40, cz},
                {cx, cz + 40},  {cx, cz - 40}
            };
            for (int[] sp : spawnPositions) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1,
                        BlockTypes.QUARTZ_BRICKS);
                block(es, sp[0], AY, sp[1], BlockTypes.SEA_LANTERN);
            }

            // ------------------------------------------------------------------
            // UNDER-PLATFORM: IRON_BLOCK stalactite clusters
            // ------------------------------------------------------------------
            sphere(es, cx,       55, cz,       3, BlockTypes.IRON_BLOCK);
            sphere(es, cx + 35,  55, cz - 25,  3, BlockTypes.IRON_BLOCK);
            sphere(es, cx - 30,  55, cz + 28,  3, BlockTypes.IRON_BLOCK);
            sphere(es, cx + 20,  55, cz + 30,  3, BlockTypes.IRON_BLOCK);
        }
    }

    // =========================================================================
    // MACE ARENA  cx=0, cz=300
    // =========================================================================
    private void buildMaceArena() {
        final int cx = 0, cz = 300;

        try (EditSession es = newSession()) {

            // ------------------------------------------------------------------
            // Platform base 130×130
            // ------------------------------------------------------------------
            // Fill y=60..62 NETHERRACK, y=63 NETHER_BRICKS
            fill(es, cx - 65, 60, cz - 65, cx + 65, 62, cz + 65, BlockTypes.NETHERRACK);
            fill(es, cx - 65, 63, cz - 65, cx + 65, 63, cz + 65, BlockTypes.NETHER_BRICKS);
            // Top surface y=64: NETHER_BRICKS (set via buildPlatform but we override)
            fill(es, cx - 65, AY, cz - 65, cx + 65, AY, cz + 65, BlockTypes.NETHER_BRICKS);
            // Also call buildPlatform for the underside rim/stalactite detail
            buildPlatform(es, cx, cz, 65, 65,
                    BlockTypes.NETHER_BRICKS, BlockTypes.NETHER_BRICKS, BlockTypes.NETHERRACK);

            // ------------------------------------------------------------------
            // FLOOR (120×120 interior)
            // ------------------------------------------------------------------
            // Base: NETHER_BRICKS overall
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.NETHER_BRICKS);

            // Outer 5-block border: RED_NETHER_BRICKS
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz - 56, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 60, AY, cz + 56, cx + 60, AY, cz + 60, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 60, AY, cz - 55, cx - 56, AY, cz + 55, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx + 56, AY, cz - 55, cx + 60, AY, cz + 55, BlockTypes.RED_NETHER_BRICKS);

            // Centre 5×5: MAGMA_BLOCK
            fill(es, cx - 2, AY, cz - 2, cx + 2, AY, cz + 2, BlockTypes.MAGMA_BLOCK);

            // Diagonal MAGMA_BLOCK channels from each corner toward centre (2-wide strips)
            // NE corner to centre
            for (int i = 0; i <= 50; i++) {
                int mx = cx + 50 - i;
                int mz = cz - 50 + i;
                block(es, mx,     AY, mz,     BlockTypes.MAGMA_BLOCK);
                block(es, mx - 1, AY, mz,     BlockTypes.MAGMA_BLOCK);
            }
            // NW corner to centre
            for (int i = 0; i <= 50; i++) {
                int mx = cx - 50 + i;
                int mz = cz - 50 + i;
                block(es, mx,     AY, mz,     BlockTypes.MAGMA_BLOCK);
                block(es, mx,     AY, mz + 1, BlockTypes.MAGMA_BLOCK);
            }
            // SE corner to centre
            for (int i = 0; i <= 50; i++) {
                int mx = cx + 50 - i;
                int mz = cz + 50 - i;
                block(es, mx,     AY, mz,     BlockTypes.MAGMA_BLOCK);
                block(es, mx,     AY, mz - 1, BlockTypes.MAGMA_BLOCK);
            }
            // SW corner to centre
            for (int i = 0; i <= 50; i++) {
                int mx = cx - 50 + i;
                int mz = cz + 50 - i;
                block(es, mx,     AY, mz,     BlockTypes.MAGMA_BLOCK);
                block(es, mx + 1, AY, mz,     BlockTypes.MAGMA_BLOCK);
            }

            // Lava moat: 2-wide MAGMA_BLOCK channel 3 blocks inside the wall perimeter
            // (perimeter wall at ~cx±61, moat at cx±57..cx±58)
            fill(es, cx - 58, AY, cz - 58, cx + 58, AY, cz - 57, BlockTypes.MAGMA_BLOCK);
            fill(es, cx - 58, AY, cz + 57, cx + 58, AY, cz + 58, BlockTypes.MAGMA_BLOCK);
            fill(es, cx - 58, AY, cz - 56, cx - 57, AY, cz + 56, BlockTypes.MAGMA_BLOCK);
            fill(es, cx + 57, AY, cz - 56, cx + 58, AY, cz + 56, BlockTypes.MAGMA_BLOCK);

            // SOUL_SAND patches scattered across floor (20 positions)
            int[][] soulSandPos = {
                {cx + 10, cz + 10}, {cx - 10, cz + 10}, {cx + 10, cz - 10}, {cx - 10, cz - 10},
                {cx + 22, cz + 5},  {cx - 22, cz + 5},  {cx + 22, cz - 5},  {cx - 22, cz - 5},
                {cx + 5, cz + 22},  {cx + 5, cz - 22},  {cx - 5, cz + 22},  {cx - 5, cz - 22},
                {cx + 35, cz + 18}, {cx - 35, cz + 18}, {cx + 35, cz - 18}, {cx - 35, cz - 18},
                {cx + 18, cz + 35}, {cx - 18, cz + 35}, {cx + 18, cz - 35}, {cx - 18, cz - 35}
            };
            for (int[] ss : soulSandPos) {
                block(es, ss[0], AY, ss[1], BlockTypes.SOUL_SAND);
            }

            // GLOWSTONE embedded in floor grid every 8 blocks
            for (int x = cx - 48; x <= cx + 48; x += 8) {
                for (int z = cz - 48; z <= cz + 48; z += 8) {
                    block(es, x, AY, z, BlockTypes.GLOWSTONE);
                }
            }

            // ------------------------------------------------------------------
            // WALLS y=65..82 (18 tall)
            // RED_NETHER_BRICKS outer, NETHER_BRICKS inner, NETHER_BRICK_WALL cap
            // ------------------------------------------------------------------
            // Outer shell
            fill(es, cx - 61, 65, cz - 61, cx + 61, 82, cz - 61, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 61, 65, cz + 61, cx + 61, 82, cz + 61, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 61, 65, cz - 60, cx - 61, 82, cz + 60, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx + 61, 65, cz - 60, cx + 61, 82, cz + 60, BlockTypes.RED_NETHER_BRICKS);
            // Inner face (1 block in)
            fill(es, cx - 60, 65, cz - 60, cx + 60, 82, cz - 60, BlockTypes.NETHER_BRICKS);
            fill(es, cx - 60, 65, cz + 60, cx + 60, 82, cz + 60, BlockTypes.NETHER_BRICKS);
            fill(es, cx - 60, 65, cz - 59, cx - 60, 82, cz + 59, BlockTypes.NETHER_BRICKS);
            fill(es, cx + 60, 65, cz - 59, cx + 60, 82, cz + 59, BlockTypes.NETHER_BRICKS);
            // NETHER_BRICK_WALL cap at y=83
            fill(es, cx - 61, 83, cz - 61, cx + 61, 83, cz - 61, BlockTypes.NETHER_BRICK_WALL);
            fill(es, cx - 61, 83, cz + 61, cx + 61, 83, cz + 61, BlockTypes.NETHER_BRICK_WALL);
            fill(es, cx - 61, 83, cz - 60, cx - 61, 83, cz + 60, BlockTypes.NETHER_BRICK_WALL);
            fill(es, cx + 61, 83, cz - 60, cx + 61, 83, cz + 60, BlockTypes.NETHER_BRICK_WALL);

            // MAGMA_BLOCK torch-pillars every 8 blocks protruding 3 out from wall
            for (int x = cx - 56; x <= cx + 56; x += 8) {
                // North wall protrusion
                fill(es, x, 75, cz - 64, x, 77, cz - 64, BlockTypes.MAGMA_BLOCK);
                // South wall protrusion
                fill(es, x, 75, cz + 64, x, 77, cz + 64, BlockTypes.MAGMA_BLOCK);
            }
            for (int z = cz - 56; z <= cz + 56; z += 8) {
                fill(es, cx - 64, 75, z, cx - 64, 77, z, BlockTypes.MAGMA_BLOCK);
                fill(es, cx + 64, 75, z, cx + 64, 77, z, BlockTypes.MAGMA_BLOCK);
            }

            // 4 entrances: 5-wide × 7-tall AIR openings at cardinal faces
            fill(es, cx - 2, 65, cz - 62, cx + 2, 71, cz - 60, BlockTypes.AIR);
            fill(es, cx - 2, 65, cz + 60, cx + 2, 71, cz + 62, BlockTypes.AIR);
            fill(es, cx - 62, 65, cz - 2, cx - 60, 71, cz + 2, BlockTypes.AIR);
            fill(es, cx + 60, 65, cz - 2, cx + 62, 71, cz + 2, BlockTypes.AIR);

            // ------------------------------------------------------------------
            // BASTIONS at corners cx±55, cz±55: 9×9 RED_NETHER_BRICKS, 25 tall
            // ------------------------------------------------------------------
            int[][] bastionCentres = {
                {cx + 55, cz + 55}, {cx - 55, cz + 55},
                {cx + 55, cz - 55}, {cx - 55, cz - 55}
            };
            for (int[] bc : bastionCentres) {
                int bx = bc[0], bz = bc[1];
                // 9×9 tower y=65..89
                fill(es, bx - 4, 65, bz - 4, bx + 4, 89, bz + 4, BlockTypes.RED_NETHER_BRICKS);
                // Hollow interior y=66..88
                fill(es, bx - 3, 66, bz - 3, bx + 3, 88, bz + 3, BlockTypes.AIR);
                // Arrow slits (AIR gaps in walls every 4 blocks)
                for (int sy = 70; sy <= 85; sy += 6) {
                    block(es, bx - 4, sy, bz, BlockTypes.AIR);
                    block(es, bx + 4, sy, bz, BlockTypes.AIR);
                    block(es, bx, sy, bz - 4, BlockTypes.AIR);
                    block(es, bx, sy, bz + 4, BlockTypes.AIR);
                }
                // GLOWSTONE cap at y=90
                fill(es, bx - 2, 90, bz - 2, bx + 2, 90, bz + 2, BlockTypes.GLOWSTONE);
                // MAGMA_BLOCK accent rings at y=70, 75, 80, 85
                for (int ry = 70; ry <= 85; ry += 5) {
                    ring(es, bx, ry, bz, 4, 4, BlockTypes.MAGMA_BLOCK);
                }
            }

            // ------------------------------------------------------------------
            // INTERIOR OBSTACLES
            // ------------------------------------------------------------------
            // Inner ring (r=20): 4 cardinal pillars
            int[][] innerNethPillars = {
                {cx + 20, cz}, {cx - 20, cz}, {cx, cz + 20}, {cx, cz - 20}
            };
            for (int[] p : innerNethPillars) {
                fill(es, p[0] - 1, AY, p[1] - 1, p[0] + 1, AY + 11, p[1] + 1,
                        BlockTypes.NETHER_BRICKS);
                block(es, p[0], AY + 12, p[1], BlockTypes.SHROOMLIGHT);
            }

            // Outer ring (r=38): 4 diagonal pillars
            int[][] outerNethPillars = {
                {cx + 27, cz + 27}, {cx - 27, cz + 27},
                {cx + 27, cz - 27}, {cx - 27, cz - 27}
            };
            for (int[] p : outerNethPillars) {
                fill(es, p[0] - 1, AY, p[1] - 1, p[0] + 1, AY + 11, p[1] + 1,
                        BlockTypes.NETHER_BRICKS);
                block(es, p[0], AY + 12, p[1], BlockTypes.SHROOMLIGHT);
            }

            // MAGMA_BLOCK lava-platform raises: 5×5, 1-tall at y=65 (NE/NW/SE/SW r=28)
            int[][] lavaRaises = {
                {cx + 20, cz + 20}, {cx - 20, cz + 20},
                {cx + 20, cz - 20}, {cx - 20, cz - 20}
            };
            for (int[] lr : lavaRaises) {
                fill(es, lr[0] - 2, 65, lr[1] - 2, lr[0] + 2, 65, lr[1] + 2, BlockTypes.MAGMA_BLOCK);
            }

            // Low RED_NETHER_BRICKS cover walls: 2-tall, 10-long at r=15, cardinal
            fill(es, cx + 10, 65, cz - 1, cx + 19, 66, cz - 1, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 19, 65, cz - 1, cx - 10, 66, cz - 1, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 1, 65, cz + 10, cx - 1, 66, cz + 19, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 1, 65, cz - 19, cx - 1, 66, cz - 10, BlockTypes.RED_NETHER_BRICKS);

            // SOUL_LANTERN posts at entrances
            block(es, cx - 3, 65, cz - 61, BlockTypes.SOUL_LANTERN);
            block(es, cx + 3, 65, cz - 61, BlockTypes.SOUL_LANTERN);
            block(es, cx - 3, 65, cz + 61, BlockTypes.SOUL_LANTERN);
            block(es, cx + 3, 65, cz + 61, BlockTypes.SOUL_LANTERN);
            block(es, cx - 61, 65, cz - 3, BlockTypes.SOUL_LANTERN);
            block(es, cx - 61, 65, cz + 3, BlockTypes.SOUL_LANTERN);
            block(es, cx + 61, 65, cz - 3, BlockTypes.SOUL_LANTERN);
            block(es, cx + 61, 65, cz + 3, BlockTypes.SOUL_LANTERN);

            // ------------------------------------------------------------------
            // SPAWN MARKERS
            // ------------------------------------------------------------------
            int[][] spawnPositions = {
                {cx + 40, cz},  {cx - 40, cz},
                {cx, cz + 40},  {cx, cz - 40}
            };
            for (int[] sp : spawnPositions) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1,
                        BlockTypes.RED_NETHER_BRICKS);
                block(es, sp[0], AY, sp[1], BlockTypes.GLOWSTONE);
            }

            // ------------------------------------------------------------------
            // UNDER-PLATFORM: NETHERRACK + MAGMA_BLOCK hanging clusters
            // ------------------------------------------------------------------
            sphere(es, cx,       55, cz,       4, BlockTypes.NETHERRACK);
            sphere(es, cx + 32,  55, cz - 22,  3, BlockTypes.MAGMA_BLOCK);
            sphere(es, cx - 28,  55, cz + 25,  3, BlockTypes.NETHERRACK);
            sphere(es, cx + 15,  55, cz + 30,  3, BlockTypes.MAGMA_BLOCK);
        }
    }

    // =========================================================================
    // BOW ARENA  cx=0, cz=-300
    // =========================================================================
    private void buildBowArena() {
        final int cx = 0, cz = -300;

        try (EditSession es = newSession()) {

            // ------------------------------------------------------------------
            // Platform base 130×130
            // y=60..62 STONE, y=63 DIRT
            // ------------------------------------------------------------------
            fill(es, cx - 65, 60, cz - 65, cx + 65, 62, cz + 65, BlockTypes.STONE);
            fill(es, cx - 65, 63, cz - 65, cx + 65, 63, cz + 65, BlockTypes.DIRT);
            buildPlatform(es, cx, cz, 65, 65,
                    BlockTypes.GRASS_BLOCK, BlockTypes.STONE, BlockTypes.COBBLESTONE);

            // ------------------------------------------------------------------
            // FLOOR (120×120): GRASS_BLOCK base already set
            // ------------------------------------------------------------------
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.GRASS_BLOCK);

            // Podzol patches in 6 areas
            fill(es, cx + 20, AY, cz + 20,  cx + 30, AY, cz + 30,  BlockTypes.PODZOL);
            fill(es, cx - 30, AY, cz + 20,  cx - 20, AY, cz + 30,  BlockTypes.PODZOL);
            fill(es, cx + 15, AY, cz - 25,  cx + 25, AY, cz - 15,  BlockTypes.PODZOL);
            fill(es, cx - 20, AY, cz - 10,  cx - 10, AY, cz,       BlockTypes.PODZOL);
            fill(es, cx + 40, AY, cz - 10,  cx + 50, AY, cz,       BlockTypes.PODZOL);
            fill(es, cx - 50, AY, cz - 40,  cx - 40, AY, cz - 30,  BlockTypes.PODZOL);

            // Coarse-dirt paths: 3-wide N/S through centre (x=cx-1..cx+1)
            fill(es, cx - 1, AY, cz - 60, cx + 1, AY, cz + 60, BlockTypes.COARSE_DIRT);
            // 3-wide E/W through centre (z=cz-1..cz+1)
            fill(es, cx - 60, AY, cz - 1, cx + 60, AY, cz + 1, BlockTypes.COARSE_DIRT);

            // Moss patches near corners
            fill(es, cx - 58, AY, cz - 58, cx - 50, AY, cz - 50, BlockTypes.MOSS_BLOCK);
            fill(es, cx + 50, AY, cz - 58, cx + 58, AY, cz - 50, BlockTypes.MOSS_BLOCK);
            fill(es, cx - 58, AY, cz + 50, cx - 50, AY, cz + 58, BlockTypes.MOSS_BLOCK);
            fill(es, cx + 50, AY, cz + 50, cx + 58, AY, cz + 58, BlockTypes.MOSS_BLOCK);

            // ------------------------------------------------------------------
            // UNEVEN TERRAIN: gentle hills
            // ------------------------------------------------------------------
            // 4 hills: 15×15 at y=65, 9×9 at y=66
            int[][] hillCentres = {
                {cx + 30, cz + 30}, {cx - 30, cz + 30},
                {cx + 30, cz - 30}, {cx - 30, cz - 30}
            };
            for (int[] hc : hillCentres) {
                fill(es, hc[0] - 7, 65, hc[1] - 7, hc[0] + 7, 65, hc[1] + 7,
                        BlockTypes.GRASS_BLOCK);
                fill(es, hc[0] - 4, 66, hc[1] - 4, hc[0] + 4, 66, hc[1] + 4,
                        BlockTypes.GRASS_BLOCK);
                // Dirt underside of raised sections
                fill(es, hc[0] - 7, 64, hc[1] - 7, hc[0] + 7, 64, hc[1] + 7,
                        BlockTypes.DIRT);
            }

            // 2 stone outcrops
            fill(es, cx + 10, 65, cz - 8, cx + 14, 65, cz - 4, BlockTypes.COBBLESTONE);
            fill(es, cx + 11, 66, cz - 7, cx + 13, 66, cz - 5, BlockTypes.COBBLESTONE);
            fill(es, cx - 14, 65, cz + 4, cx - 10, 65, cz + 8, BlockTypes.COBBLESTONE);
            fill(es, cx - 13, 66, cz + 5, cx - 11, 66, cz + 7, BlockTypes.COBBLESTONE);

            // ------------------------------------------------------------------
            // WALLS y=65..75 — MOSSY_STONE_BRICKS outer, STONE_BRICKS inner
            // Not solid — 2-wide breaches every 8 blocks
            // ------------------------------------------------------------------
            for (int x = cx - 61; x <= cx + 61; x++) {
                // Natural breaches at multiples of 8 (2-wide)
                boolean breach = (Math.abs(x - cx) % 8 <= 1);
                if (!breach) {
                    column(es, x, cz - 61, 65, 75, BlockTypes.MOSSY_STONE_BRICKS);
                    column(es, x, cz + 61, 65, 75, BlockTypes.MOSSY_STONE_BRICKS);
                    // Inner face
                    column(es, x, cz - 60, 65, 75, BlockTypes.STONE_BRICKS);
                    column(es, x, cz + 60, 65, 75, BlockTypes.STONE_BRICKS);
                }
            }
            for (int z = cz - 61; z <= cz + 61; z++) {
                boolean breach = (Math.abs(z - cz) % 8 <= 1);
                if (!breach) {
                    column(es, cx - 61, z, 65, 75, BlockTypes.MOSSY_STONE_BRICKS);
                    column(es, cx + 61, z, 65, 75, BlockTypes.MOSSY_STONE_BRICKS);
                    column(es, cx - 60, z, 65, 75, BlockTypes.STONE_BRICKS);
                    column(es, cx + 60, z, 65, 75, BlockTypes.STONE_BRICKS);
                }
            }

            // Irregular merlons at top (60% coverage — every block at odd offset stays)
            for (int x = cx - 61; x <= cx + 61; x += 3) {
                block(es, x, 76, cz - 61, BlockTypes.MOSSY_COBBLESTONE);
                block(es, x, 76, cz + 61, BlockTypes.MOSSY_COBBLESTONE);
            }
            for (int z = cz - 61; z <= cz + 61; z += 3) {
                block(es, cx - 61, 76, z, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx + 61, 76, z, BlockTypes.MOSSY_COBBLESTONE);
            }

            // 4 wide open gateways: 7-wide AIR at cardinal faces
            fill(es, cx - 3, 65, cz - 62, cx + 3, 75, cz - 60, BlockTypes.AIR);
            fill(es, cx - 3, 65, cz + 60, cx + 3, 75, cz + 62, BlockTypes.AIR);
            fill(es, cx - 62, 65, cz - 3, cx - 60, 75, cz + 3, BlockTypes.AIR);
            fill(es, cx + 60, 65, cz - 3, cx + 62, 75, cz + 3, BlockTypes.AIR);

            // ------------------------------------------------------------------
            // FOREST INTERIOR OBSTACLES
            // ------------------------------------------------------------------
            // 12 oak trees — column trunk 6-8 tall, 3-layer leaf canopy
            int[][] oakTrees = {
                {cx + 15, cz + 15, 6}, {cx - 15, cz + 15, 7}, {cx + 15, cz - 15, 8},
                {cx - 15, cz - 15, 6}, {cx + 35, cz + 10, 7}, {cx - 35, cz + 10, 6},
                {cx + 35, cz - 10, 7}, {cx - 35, cz - 10, 8}, {cx + 10, cz + 40, 6},
                {cx - 10, cz + 40, 7}, {cx + 10, cz - 40, 6}, {cx - 10, cz - 40, 7}
            };
            for (int[] tree : oakTrees) {
                int tx = tree[0], tz = tree[1], th = tree[2];
                column(es, tx, tz, AY + 1, AY + th, BlockTypes.OAK_LOG);
                // Leaf canopy: 3 layers (5×5 bottom, 3×3 mid, 1 top)
                fill(es, tx - 2, AY + th - 1, tz - 2, tx + 2, AY + th - 1, tz + 2,
                        BlockTypes.OAK_LEAVES);
                fill(es, tx - 2, AY + th,     tz - 2, tx + 2, AY + th,     tz + 2,
                        BlockTypes.OAK_LEAVES);
                fill(es, tx - 1, AY + th + 1, tz - 1, tx + 1, AY + th + 1, tz + 1,
                        BlockTypes.OAK_LEAVES);
                block(es, tx, AY + th + 2, tz, BlockTypes.OAK_LEAVES);
            }

            // 6 dark-oak trees (2×2 trunk, wide canopy)
            int[][] darkOakTrees = {
                {cx + 45, cz + 40}, {cx - 45, cz + 40},
                {cx + 45, cz - 40}, {cx - 45, cz - 40},
                {cx + 5,  cz + 55}, {cx - 5,  cz - 55}
            };
            for (int[] dot : darkOakTrees) {
                int tx = dot[0], tz = dot[1];
                fill(es, tx, AY + 1, tz, tx + 1, AY + 7, tz + 1, BlockTypes.DARK_OAK_LOG);
                // Wide canopy
                fill(es, tx - 3, AY + 6, tz - 3, tx + 4, AY + 7, tz + 4, BlockTypes.DARK_OAK_LEAVES);
                fill(es, tx - 2, AY + 8, tz - 2, tx + 3, AY + 8, tz + 3, BlockTypes.DARK_OAK_LEAVES);
                fill(es, tx - 1, AY + 9, tz - 1, tx + 2, AY + 9, tz + 2, BlockTypes.DARK_OAK_LEAVES);
            }

            // Low stone-cover walls (MOSSY_STONE_BRICKS 2-tall, 8-long) at 6 positions
            fill(es, cx + 5,  65, cz + 8,  cx + 12, 66, cz + 8,  BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx - 12, 65, cz + 8,  cx - 5,  66, cz + 8,  BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx + 5,  65, cz - 8,  cx + 12, 66, cz - 8,  BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx - 12, 65, cz - 8,  cx - 5,  66, cz - 8,  BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx + 25, 65, cz - 1,  cx + 25, 66, cz + 6,  BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx - 25, 65, cz - 6,  cx - 25, 66, cz + 1,  BlockTypes.MOSSY_STONE_BRICKS);

            // 4 archer's stands: CRAFTING_TABLE 2×2 platform raise at y=65
            fill(es, cx + 50, 65, cz + 50, cx + 51, 65, cz + 51, BlockTypes.CRAFTING_TABLE);
            fill(es, cx - 51, 65, cz + 50, cx - 50, 65, cz + 51, BlockTypes.CRAFTING_TABLE);
            fill(es, cx + 50, 65, cz - 51, cx + 51, 65, cz - 50, BlockTypes.CRAFTING_TABLE);
            fill(es, cx - 51, 65, cz - 51, cx - 50, 65, cz - 50, BlockTypes.CRAFTING_TABLE);

            // 2 water pools: 5×5 WATER at y=63, STONE_BRICKS border at y=64
            // Pool 1
            fill(es, cx + 20, 63, cz + 5, cx + 24, 63, cz + 9, BlockTypes.WATER);
            fill(es, cx + 19, AY, cz + 4, cx + 25, AY, cz + 10, BlockTypes.STONE_BRICKS);
            fill(es, cx + 20, AY, cz + 5, cx + 24, AY, cz + 9,  BlockTypes.AIR);
            // Pool 2
            fill(es, cx - 24, 63, cz - 9, cx - 20, 63, cz - 5, BlockTypes.WATER);
            fill(es, cx - 25, AY, cz - 10, cx - 19, AY, cz - 4, BlockTypes.STONE_BRICKS);
            fill(es, cx - 24, AY, cz - 9,  cx - 20, AY, cz - 5, BlockTypes.AIR);

            // ------------------------------------------------------------------
            // LIGHTING
            // ------------------------------------------------------------------
            // Fence-post lanterns (OAK_FENCE 4-tall + LANTERN) scattered 16 positions
            int[][] lanternPosts = {
                {cx + 8, cz + 8},   {cx - 8, cz + 8},   {cx + 8, cz - 8},   {cx - 8, cz - 8},
                {cx + 25, cz + 25}, {cx - 25, cz + 25}, {cx + 25, cz - 25}, {cx - 25, cz - 25},
                {cx + 48, cz + 5},  {cx - 48, cz + 5},  {cx + 5, cz + 48},  {cx + 5, cz - 48},
                {cx + 48, cz - 5},  {cx - 48, cz - 5},  {cx - 5, cz + 48},  {cx - 5, cz - 48}
            };
            for (int[] lp : lanternPosts) {
                column(es, lp[0], lp[1], AY + 1, AY + 4, BlockTypes.OAK_FENCE);
                block(es, lp[0], AY + 5, lp[1], BlockTypes.LANTERN);
            }
            // GLOWSTONE embedded in paths every 8 blocks
            for (int x = cx - 56; x <= cx + 56; x += 8) {
                block(es, x, AY, cz, BlockTypes.GLOWSTONE);
            }
            for (int z = cz - 56; z <= cz + 56; z += 8) {
                block(es, cx, AY, z, BlockTypes.GLOWSTONE);
            }

            // ------------------------------------------------------------------
            // FLOOR DECORATION: plants
            // ------------------------------------------------------------------
            int[][] shortGrassPos = {
                {cx + 12, cz + 20}, {cx - 12, cz + 20}, {cx + 22, cz - 18}, {cx - 22, cz - 18},
                {cx + 38, cz + 32}, {cx - 38, cz + 32}, {cx + 38, cz - 32}, {cx - 38, cz - 32},
                {cx + 5,  cz + 35}, {cx - 5,  cz - 35}, {cx + 50, cz + 20}, {cx - 50, cz - 20}
            };
            for (int[] sg : shortGrassPos) {
                block(es, sg[0], AY + 1, sg[1], BlockTypes.SHORT_GRASS);
            }
            int[][] fernPos = {
                {cx + 18, cz + 18}, {cx - 18, cz + 18}, {cx + 18, cz - 18}, {cx - 18, cz - 18},
                {cx + 42, cz + 42}, {cx - 42, cz + 42}, {cx + 42, cz - 42}, {cx - 42, cz - 42}
            };
            for (int[] fp : fernPos) {
                block(es, fp[0], AY + 1, fp[1], BlockTypes.FERN);
            }
            // Flowers near walls
            int[][] flowerPos = {
                {cx + 55, cz + 15}, {cx - 55, cz + 15}, {cx + 55, cz - 15}, {cx - 55, cz - 15},
                {cx + 15, cz + 55}, {cx - 15, cz + 55}, {cx + 15, cz - 55}, {cx - 15, cz - 55}
            };
            for (int i = 0; i < flowerPos.length; i++) {
                block(es, flowerPos[i][0], AY + 1, flowerPos[i][1],
                        i % 2 == 0 ? BlockTypes.OXEYE_DAISY : BlockTypes.POPPY);
            }

            // ------------------------------------------------------------------
            // 4 CORNER ARCHER TOWERS: 5×5, MOSSY_STONE_BRICKS, 12 tall
            // ------------------------------------------------------------------
            int[][] archerTowers = {
                {cx + 57, cz + 57}, {cx - 57, cz + 57},
                {cx + 57, cz - 57}, {cx - 57, cz - 57}
            };
            for (int[] at : archerTowers) {
                int tx = at[0], tz = at[1];
                fill(es, tx - 2, 65, tz - 2, tx + 2, 76, tz + 2, BlockTypes.MOSSY_STONE_BRICKS);
                // Hollow interior
                fill(es, tx - 1, 65, tz - 1, tx + 1, 76, tz + 1, BlockTypes.AIR);
                // Flat DARK_OAK_PLANKS roof at y=77
                fill(es, tx - 2, 77, tz - 2, tx + 2, 77, tz + 2, BlockTypes.DARK_OAK_PLANKS);
                // OAK_FENCE railing at y=78
                fill(es, tx - 2, 78, tz - 2, tx + 2, 78, tz - 2, BlockTypes.OAK_FENCE);
                fill(es, tx - 2, 78, tz + 2, tx + 2, 78, tz + 2, BlockTypes.OAK_FENCE);
                fill(es, tx - 2, 78, tz - 1, tx - 2, 78, tz + 1, BlockTypes.OAK_FENCE);
                fill(es, tx + 2, 78, tz - 1, tx + 2, 78, tz + 1, BlockTypes.OAK_FENCE);
                // Arrow slits
                block(es, tx - 2, 70, tz, BlockTypes.AIR);
                block(es, tx + 2, 70, tz, BlockTypes.AIR);
                block(es, tx, 70, tz - 2, BlockTypes.AIR);
                block(es, tx, 70, tz + 2, BlockTypes.AIR);
            }

            // ------------------------------------------------------------------
            // SPAWN MARKERS
            // ------------------------------------------------------------------
            int[][] spawnPositions = {
                {cx + 40, cz},  {cx - 40, cz},
                {cx, cz + 40},  {cx, cz - 40}
            };
            for (int[] sp : spawnPositions) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1, BlockTypes.MOSS_BLOCK);
                block(es, sp[0], AY, sp[1], BlockTypes.SEA_LANTERN);
            }

            // ------------------------------------------------------------------
            // UNDER-PLATFORM
            // ------------------------------------------------------------------
            sphere(es, cx,       55, cz,       3, BlockTypes.COBBLESTONE);
            sphere(es, cx + 30,  55, cz - 25,  3, BlockTypes.GRAVEL);
            sphere(es, cx - 28,  55, cz + 22,  3, BlockTypes.MOSS_BLOCK);
        }
    }

    // =========================================================================
    // TOTEM ARENA  cx=220, cz=-220
    // =========================================================================
    private void buildTotemArena() {
        final int cx = 220, cz = -220;

        try (EditSession es = newSession()) {

            // ------------------------------------------------------------------
            // Platform base 130×130
            // ------------------------------------------------------------------
            fill(es, cx - 65, 60, cz - 65, cx + 65, 62, cz + 65, BlockTypes.STONE);
            fill(es, cx - 65, 63, cz - 65, cx + 65, 63, cz + 65, BlockTypes.DIRT);
            buildPlatform(es, cx, cz, 65, 65,
                    BlockTypes.GRASS_BLOCK, BlockTypes.STONE, BlockTypes.JUNGLE_LOG);

            // ------------------------------------------------------------------
            // FLOOR (120×120)
            // ------------------------------------------------------------------
            // Outer ring: GRASS_BLOCK and COARSE_DIRT mix
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.GRASS_BLOCK);
            // Coarse dirt patches
            for (int x = cx - 55; x <= cx + 55; x += 10) {
                for (int z = cz - 55; z <= cz + 55; z += 10) {
                    fill(es, x, AY, z, x + 4, AY, z + 4, BlockTypes.COARSE_DIRT);
                }
            }

            // Mid ring (30..50 radius): JUNGLE_PLANKS + PODZOL
            for (int x = cx - 50; x <= cx + 50; x++) {
                for (int z = cz - 50; z <= cz + 50; z++) {
                    double dx = x - cx, dz = z - cz;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist >= 20 && dist <= 48) {
                        block(es, x, AY, z, (x + z) % 3 == 0 ? BlockTypes.PODZOL : BlockTypes.JUNGLE_PLANKS);
                    }
                }
            }

            // Central zone (30×30): MOSSY_STONE_BRICKS ancient plaza
            fill(es, cx - 15, AY, cz - 15, cx + 15, AY, cz + 15, BlockTypes.MOSSY_STONE_BRICKS);

            // Raised ruin platforms: 3 areas +2 blocks
            // Platform 1: NE
            fill(es, cx + 30, 65, cz - 45, cx + 48, 65, cz - 27, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx + 30, AY, cz - 45, cx + 48, AY, cz - 27, BlockTypes.STONE_BRICKS);
            // Platform 2: SW
            fill(es, cx - 48, 65, cz + 27, cx - 30, 65, cz + 45, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx - 48, AY, cz + 27, cx - 30, AY, cz + 45, BlockTypes.STONE_BRICKS);
            // Platform 3: NW
            fill(es, cx - 45, 65, cz - 45, cx - 28, 65, cz - 28, BlockTypes.COARSE_DIRT);
            fill(es, cx - 45, AY, cz - 45, cx - 28, AY, cz - 28, BlockTypes.MOSSY_STONE_BRICKS);

            // Sunken section: 20×20 at y=63, SE quadrant
            fill(es, cx + 15, 63, cz + 15, cx + 35, 63, cz + 35, BlockTypes.MOSS_BLOCK);
            fill(es, cx + 15, AY, cz + 15, cx + 35, AY, cz + 35, BlockTypes.AIR);
            // Border the sunken area
            fill(es, cx + 14, AY, cz + 14, cx + 36, AY, cz + 14, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx + 14, AY, cz + 36, cx + 36, AY, cz + 36, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx + 14, AY, cz + 15, cx + 14, AY, cz + 35, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx + 36, AY, cz + 15, cx + 36, AY, cz + 35, BlockTypes.CRACKED_STONE_BRICKS);

            // ------------------------------------------------------------------
            // WALLS y=65..76: JUNGLE_LOG outer, MOSSY_STONE_BRICKS inner
            // ------------------------------------------------------------------
            fill(es, cx - 61, 65, cz - 61, cx + 61, 76, cz - 61, BlockTypes.JUNGLE_LOG);
            fill(es, cx - 61, 65, cz + 61, cx + 61, 76, cz + 61, BlockTypes.JUNGLE_LOG);
            fill(es, cx - 61, 65, cz - 60, cx - 61, 76, cz + 60, BlockTypes.JUNGLE_LOG);
            fill(es, cx + 61, 65, cz - 60, cx + 61, 76, cz + 60, BlockTypes.JUNGLE_LOG);
            // Inner face
            fill(es, cx - 60, 65, cz - 60, cx + 60, 76, cz - 60, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx - 60, 65, cz + 60, cx + 60, 76, cz + 60, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx - 60, 65, cz - 59, cx - 60, 76, cz + 59, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx + 60, 65, cz - 59, cx + 60, 76, cz + 59, BlockTypes.MOSSY_STONE_BRICKS);

            // Dense JUNGLE_LEAVES top 3 rows of wall y=74..76
            fill(es, cx - 61, 74, cz - 61, cx + 61, 76, cz - 61, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx - 61, 74, cz + 61, cx + 61, 76, cz + 61, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx - 61, 74, cz - 60, cx - 61, 76, cz + 60, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx + 61, 74, cz - 60, cx + 61, 76, cz + 60, BlockTypes.JUNGLE_LEAVES);

            // 3×3 JUNGLE_LOG corner columns extending above wall to y=82
            int[][] wallCornerCols = {
                {cx - 61, cz - 61}, {cx + 59, cz - 61},
                {cx - 61, cz + 59}, {cx + 59, cz + 59}
            };
            for (int[] wcc : wallCornerCols) {
                fill(es, wcc[0], 65, wcc[1], wcc[0] + 1, 82, wcc[1] + 1, BlockTypes.JUNGLE_LOG);
            }

            // 4 wide jungle-vine openings: 7-wide × 11-tall
            fill(es, cx - 3, 65, cz - 62, cx + 3, 75, cz - 60, BlockTypes.AIR);
            fill(es, cx - 3, 65, cz + 60, cx + 3, 75, cz + 62, BlockTypes.AIR);
            fill(es, cx - 62, 65, cz - 3, cx - 60, 75, cz + 3, BlockTypes.AIR);
            fill(es, cx + 60, 65, cz - 3, cx + 62, 75, cz + 3, BlockTypes.AIR);

            // HANGING_ROOTS draping from wall tops
            for (int x = cx - 58; x <= cx + 58; x += 6) {
                block(es, x, 73, cz - 61, BlockTypes.HANGING_ROOTS);
                block(es, x, 73, cz + 61, BlockTypes.HANGING_ROOTS);
            }
            for (int z = cz - 58; z <= cz + 58; z += 6) {
                block(es, cx - 61, 73, z, BlockTypes.HANGING_ROOTS);
                block(es, cx + 61, 73, z, BlockTypes.HANGING_ROOTS);
            }

            // ------------------------------------------------------------------
            // INTERIOR: STEP PYRAMID TEMPLE at centre
            // 15×15 base (3 tall) → 9×9 (2 tall) → 5×5 (2 tall) → capstone
            // ------------------------------------------------------------------
            // Tier 1: y=65..67
            fill(es, cx - 7, 65, cz - 7, cx + 7, 67, cz + 7, BlockTypes.MOSSY_STONE_BRICKS);
            // Tier 2: y=68..69
            fill(es, cx - 4, 68, cz - 4, cx + 4, 69, cz + 4, BlockTypes.STONE_BRICKS);
            // Tier 3: y=70..71
            fill(es, cx - 2, 70, cz - 2, cx + 2, 71, cz + 2, BlockTypes.CRACKED_STONE_BRICKS);
            // Capstone: CHISELED_STONE_BRICKS at y=72
            block(es, cx, 72, cz, BlockTypes.CHISELED_STONE_BRICKS);
            // SEA_LANTERN capstone at y=73
            block(es, cx, 73, cz, BlockTypes.SEA_LANTERN);
            // Steps into pyramid: clear N/S faces on tier 1 for entrance
            fill(es, cx - 1, 65, cz - 8, cx + 1, 67, cz - 8, BlockTypes.AIR);
            fill(es, cx - 1, 65, cz + 8, cx + 1, 67, cz + 8, BlockTypes.AIR);

            // 8 JUNGLE_LOG columns (2×2, 14 tall) at r=35 with JUNGLE_LEAVES canopy
            for (int i = 0; i < 8; i++) {
                double rad = Math.toRadians(i * 45.0);
                int px = cx + (int) Math.round(35 * Math.cos(rad));
                int pz = cz + (int) Math.round(35 * Math.sin(rad));
                fill(es, px, AY + 1, pz, px + 1, AY + 14, pz + 1, BlockTypes.JUNGLE_LOG);
                // 5×5 canopy at top
                fill(es, px - 2, AY + 14, pz - 2, px + 3, AY + 15, pz + 3,
                        BlockTypes.JUNGLE_LEAVES);
                fill(es, px - 1, AY + 16, pz - 1, px + 2, AY + 16, pz + 2,
                        BlockTypes.JUNGLE_LEAVES);
                // SHROOMLIGHT at column top (below canopy)
                block(es, px, AY + 13, pz, BlockTypes.SHROOMLIGHT);
                // PODZOL at base
                fill(es, px - 1, AY, pz - 1, px + 2, AY, pz + 2, BlockTypes.PODZOL);
            }

            // 6 bamboo clusters (3×3, 8 tall)
            int[][] bambooClusters = {
                {cx + 20, cz - 35}, {cx - 20, cz - 35},
                {cx + 40, cz + 10}, {cx - 40, cz + 10},
                {cx + 10, cz + 40}, {cx - 10, cz - 40}
            };
            for (int[] bc : bambooClusters) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        column(es, bc[0] + dx, bc[1] + dz, AY + 1, AY + 8, BlockTypes.BAMBOO);
                    }
                }
            }

            // 4 crumbling ruin walls (CRACKED_STONE_BRICKS, 3-wide × 8-long × 3-tall)
            fill(es, cx + 15, 65, cz - 18, cx + 17, 67, cz - 11, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx - 17, 65, cz + 11, cx - 15, 67, cz + 18, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx - 18, 65, cz - 17, cx - 11, 67, cz - 15, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx + 11, 65, cz + 15, cx + 18, 67, cz + 17, BlockTypes.CRACKED_STONE_BRICKS);

            // ------------------------------------------------------------------
            // LIGHTING
            // ------------------------------------------------------------------
            // SEA_LANTERN embedded in ancient plaza centre and temple tiers
            block(es, cx - 5, AY, cz - 5, BlockTypes.SEA_LANTERN);
            block(es, cx + 5, AY, cz + 5, BlockTypes.SEA_LANTERN);
            block(es, cx - 5, AY, cz + 5, BlockTypes.SEA_LANTERN);
            block(es, cx + 5, AY, cz - 5, BlockTypes.SEA_LANTERN);
            block(es, cx, 68, cz - 3, BlockTypes.SEA_LANTERN);
            block(es, cx, 68, cz + 3, BlockTypes.SEA_LANTERN);

            // LANTERN posts at 8 positions
            int[][] lPosts = {
                {cx + 25, cz + 5}, {cx - 25, cz + 5},
                {cx + 25, cz - 5}, {cx - 25, cz - 5},
                {cx + 5, cz + 25}, {cx + 5, cz - 25},
                {cx - 5, cz + 25}, {cx - 5, cz - 25}
            };
            for (int[] lp : lPosts) {
                column(es, lp[0], lp[1], AY + 1, AY + 4, BlockTypes.OAK_FENCE);
                block(es, lp[0], AY + 5, lp[1], BlockTypes.LANTERN);
            }

            // ------------------------------------------------------------------
            // FLOOR PLANTS
            // ------------------------------------------------------------------
            int[][] fernPatches = {
                {cx + 42, cz + 42}, {cx - 42, cz + 42},
                {cx + 42, cz - 42}, {cx - 42, cz - 42},
                {cx + 20, cz + 20}, {cx - 20, cz - 20}
            };
            for (int[] fp : fernPatches) {
                block(es, fp[0], AY + 1, fp[1], BlockTypes.FERN);
                block(es, fp[0] + 1, AY + 1, fp[1], BlockTypes.SHORT_GRASS);
                block(es, fp[0], AY + 1, fp[1] + 1, BlockTypes.SHORT_GRASS);
            }
            // MOSS_CARPET near walls
            for (int x = cx - 58; x <= cx + 58; x += 4) {
                block(es, x, AY + 1, cz - 59, BlockTypes.MOSS_CARPET);
                block(es, x, AY + 1, cz + 59, BlockTypes.MOSS_CARPET);
            }
            // AZALEA near walls
            block(es, cx + 55, AY + 1, cz - 20, BlockTypes.AZALEA);
            block(es, cx - 55, AY + 1, cz + 20, BlockTypes.AZALEA);
            block(es, cx + 20, AY + 1, cz + 55, BlockTypes.AZALEA);
            block(es, cx - 20, AY + 1, cz - 55, BlockTypes.AZALEA);

            // ------------------------------------------------------------------
            // SPAWN MARKERS
            // ------------------------------------------------------------------
            int[][] spawnPositions = {
                {cx + 40, cz},  {cx - 40, cz},
                {cx, cz + 40},  {cx, cz - 40}
            };
            for (int[] sp : spawnPositions) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1,
                        BlockTypes.JUNGLE_PLANKS);
                block(es, sp[0], AY, sp[1], BlockTypes.GLOWSTONE);
            }

            // ------------------------------------------------------------------
            // UNDER-PLATFORM
            // ------------------------------------------------------------------
            // JUNGLE_LOG stalactite hanging posts
            column(es, cx,      cz,      50, 59, BlockTypes.JUNGLE_LOG);
            column(es, cx + 25, cz - 20, 52, 59, BlockTypes.JUNGLE_LOG);
            column(es, cx - 22, cz + 25, 52, 59, BlockTypes.JUNGLE_LOG);
            // MOSS_BLOCK patches
            sphere(es, cx,       55, cz,       3, BlockTypes.MOSS_BLOCK);
            sphere(es, cx + 30,  55, cz - 25,  2, BlockTypes.MOSS_BLOCK);
        }
    }

    // =========================================================================
    // AXE ARENA  cx=-220, cz=-220
    // =========================================================================
    private void buildAxeArena() {
        final int cx = -220, cz = -220;

        try (EditSession es = newSession()) {

            // ------------------------------------------------------------------
            // Platform base 130×130 — DEEPSLATE body
            // ------------------------------------------------------------------
            buildPlatform(es, cx, cz, 65, 65,
                    BlockTypes.DEEPSLATE_TILES,
                    BlockTypes.DEEPSLATE,
                    BlockTypes.COBBLED_DEEPSLATE);

            // ------------------------------------------------------------------
            // FLOOR (120×120)
            // ------------------------------------------------------------------
            // DEEPSLATE_TILES overall (already set by platform)
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.DEEPSLATE_TILES);

            // COBBLED_DEEPSLATE 5-wide border
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz - 56, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 60, AY, cz + 56, cx + 60, AY, cz + 60, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 60, AY, cz - 55, cx - 56, AY, cz + 55, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx + 56, AY, cz - 55, cx + 60, AY, cz + 55, BlockTypes.COBBLED_DEEPSLATE);

            // POLISHED_DEEPSLATE 5-wide cross paths N/S and E/W through centre
            fill(es, cx - 2, AY, cz - 60, cx + 2, AY, cz + 60, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx - 60, AY, cz - 2, cx + 60, AY, cz + 2, BlockTypes.POLISHED_DEEPSLATE);

            // Gravel patches: 12 positions, 3×3 each
            int[][] gravelPatches = {
                {cx + 15, cz + 15}, {cx - 15, cz + 15}, {cx + 15, cz - 15}, {cx - 15, cz - 15},
                {cx + 35, cz + 8},  {cx - 35, cz + 8},  {cx + 35, cz - 8},  {cx - 35, cz - 8},
                {cx + 8, cz + 35},  {cx - 8, cz + 35},  {cx + 8, cz - 35},  {cx - 8, cz - 35}
            };
            for (int[] gp : gravelPatches) {
                fill(es, gp[0] - 1, AY, gp[1] - 1, gp[0] + 1, AY, gp[1] + 1, BlockTypes.GRAVEL);
            }

            // SEA_LANTERN in floor grid every 10 blocks
            for (int x = cx - 50; x <= cx + 50; x += 10) {
                for (int z = cz - 50; z <= cz + 50; z += 10) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // Fence-post lanterns where paths cross
            block(es, cx - 30, AY + 1, cz, BlockTypes.OAK_FENCE);
            block(es, cx - 30, AY + 2, cz, BlockTypes.OAK_FENCE);
            block(es, cx - 30, AY + 3, cz, BlockTypes.LANTERN);
            block(es, cx + 30, AY + 1, cz, BlockTypes.OAK_FENCE);
            block(es, cx + 30, AY + 2, cz, BlockTypes.OAK_FENCE);
            block(es, cx + 30, AY + 3, cz, BlockTypes.LANTERN);
            block(es, cx, AY + 1, cz - 30, BlockTypes.OAK_FENCE);
            block(es, cx, AY + 2, cz - 30, BlockTypes.OAK_FENCE);
            block(es, cx, AY + 3, cz - 30, BlockTypes.LANTERN);
            block(es, cx, AY + 1, cz + 30, BlockTypes.OAK_FENCE);
            block(es, cx, AY + 2, cz + 30, BlockTypes.OAK_FENCE);
            block(es, cx, AY + 3, cz + 30, BlockTypes.LANTERN);

            // ------------------------------------------------------------------
            // CAVE CEILING at y=78..80
            // ------------------------------------------------------------------
            fill(es, cx - 60, 78, cz - 60, cx + 60, 80, cz + 60, BlockTypes.STONE);
            // Inner ceiling face: DEEPSLATE at y=78
            fill(es, cx - 60, 78, cz - 60, cx + 60, 78, cz + 60, BlockTypes.DEEPSLATE);

            // 4 ceiling entry holes (5-wide × 7-tall, cardinal faces)
            // North
            fill(es, cx - 2, 78, cz - 60, cx + 2, 80, cz - 60, BlockTypes.AIR);
            // South
            fill(es, cx - 2, 78, cz + 60, cx + 2, 80, cz + 60, BlockTypes.AIR);
            // West
            fill(es, cx - 60, 78, cz - 2, cx - 60, 80, cz + 2, BlockTypes.AIR);
            // East
            fill(es, cx + 60, 78, cz - 2, cx + 60, 80, cz + 2, BlockTypes.AIR);

            // 40 stalactites hanging from ceiling: POINTED_DRIPSTONE columns length 2-5
            int[][] stalactitePos = {
                {cx + 12, cz + 12, 75}, {cx - 12, cz + 12, 74}, {cx + 12, cz - 12, 76},
                {cx - 12, cz - 12, 75}, {cx + 28, cz + 5,  74}, {cx - 28, cz + 5,  75},
                {cx + 28, cz - 5,  76}, {cx - 28, cz - 5,  74}, {cx + 5, cz + 28,  75},
                {cx + 5, cz - 28,  75}, {cx - 5, cz + 28,  76}, {cx - 5, cz - 28,  74},
                {cx + 42, cz + 20, 74}, {cx - 42, cz + 20, 75}, {cx + 42, cz - 20, 76},
                {cx - 42, cz - 20, 75}, {cx + 20, cz + 42, 74}, {cx - 20, cz + 42, 76},
                {cx + 20, cz - 42, 75}, {cx - 20, cz - 42, 74}, {cx + 50, cz + 5,  75},
                {cx - 50, cz + 5,  74}, {cx + 50, cz - 5,  76}, {cx - 50, cz - 5,  75},
                {cx + 5, cz + 50,  75}, {cx - 5, cz + 50,  74}, {cx + 5, cz - 50,  76},
                {cx - 5, cz - 50,  75}, {cx + 35, cz + 35, 74}, {cx - 35, cz + 35, 75},
                {cx + 35, cz - 35, 76}, {cx - 35, cz - 35, 74}, {cx + 55, cz + 30, 75},
                {cx - 55, cz + 30, 74}, {cx + 55, cz - 30, 75}, {cx - 55, cz - 30, 76},
                {cx + 30, cz + 55, 74}, {cx - 30, cz + 55, 75}, {cx + 30, cz - 55, 76},
                {cx - 30, cz - 55, 74}
            };
            for (int[] st : stalactitePos) {
                // Hang dripstone from y=78 (ceiling) downward to the given Y
                column(es, st[0], st[1], st[2], 77, BlockTypes.POINTED_DRIPSTONE);
            }

            // 8 GLOWSTONE embedded in ceiling
            int[][] ceilGlow = {
                {cx + 20, cz + 20}, {cx - 20, cz + 20},
                {cx + 20, cz - 20}, {cx - 20, cz - 20},
                {cx + 45, cz + 45}, {cx - 45, cz + 45},
                {cx + 45, cz - 45}, {cx - 45, cz - 45}
            };
            for (int[] cg : ceilGlow) {
                block(es, cg[0], 78, cg[1], BlockTypes.GLOWSTONE);
            }

            // 10 SHROOMLIGHT in ceiling
            int[][] ceilShroom = {
                {cx + 10, cz},     {cx - 10, cz},     {cx, cz + 10},     {cx, cz - 10},
                {cx + 35, cz + 15},{cx - 35, cz + 15},{cx + 35, cz - 15},{cx - 35, cz - 15},
                {cx + 0,  cz + 40},{cx + 0,  cz - 40}
            };
            for (int[] cs : ceilShroom) {
                block(es, cs[0], 78, cs[1], BlockTypes.SHROOMLIGHT);
            }

            // ------------------------------------------------------------------
            // WALLS y=65..77 (13 tall, reaching ceiling)
            // DEEPSLATE_BRICKS outer, COBBLED_DEEPSLATE inner
            // ------------------------------------------------------------------
            fill(es, cx - 61, 65, cz - 61, cx + 61, 77, cz - 61, BlockTypes.DEEPSLATE_BRICKS);
            fill(es, cx - 61, 65, cz + 61, cx + 61, 77, cz + 61, BlockTypes.DEEPSLATE_BRICKS);
            fill(es, cx - 61, 65, cz - 60, cx - 61, 77, cz + 60, BlockTypes.DEEPSLATE_BRICKS);
            fill(es, cx + 61, 65, cz - 60, cx + 61, 77, cz + 60, BlockTypes.DEEPSLATE_BRICKS);
            // Inner face
            fill(es, cx - 60, 65, cz - 60, cx + 60, 77, cz - 60, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 60, 65, cz + 60, cx + 60, 77, cz + 60, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 60, 65, cz - 59, cx - 60, 77, cz + 59, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx + 60, 65, cz - 59, cx + 60, 77, cz + 59, BlockTypes.COBBLED_DEEPSLATE);

            // SEA_LANTERN flush in walls every 8 blocks
            for (int x = cx - 56; x <= cx + 56; x += 8) {
                block(es, x, 71, cz - 61, BlockTypes.SEA_LANTERN);
                block(es, x, 71, cz + 61, BlockTypes.SEA_LANTERN);
            }
            for (int z = cz - 56; z <= cz + 56; z += 8) {
                block(es, cx - 61, 71, z, BlockTypes.SEA_LANTERN);
                block(es, cx + 61, 71, z, BlockTypes.SEA_LANTERN);
            }

            // Ore vein patches on walls: DEEPSLATE_IRON_ORE and DEEPSLATE_GOLD_ORE
            for (int i = 0; i < 15; i++) {
                int xOff = (i * 7 + 3) % 121 - 60;  // -60..60
                int yOff = 65 + (i * 3) % 13;
                block(es, cx + xOff, yOff, cz - 61, i % 2 == 0 ? BlockTypes.DEEPSLATE_IRON_ORE : BlockTypes.DEEPSLATE_GOLD_ORE);
                block(es, cx + xOff, yOff, cz + 61, i % 2 == 0 ? BlockTypes.DEEPSLATE_COAL_ORE : BlockTypes.DEEPSLATE_IRON_ORE);
                int zOff2 = (i * 11 + 5) % 121 - 60;
                block(es, cx - 61, yOff, cz + zOff2, i % 2 == 0 ? BlockTypes.DEEPSLATE_GOLD_ORE : BlockTypes.DEEPSLATE_COAL_ORE);
                block(es, cx + 61, yOff, cz + zOff2, i % 2 == 0 ? BlockTypes.DEEPSLATE_IRON_ORE : BlockTypes.DEEPSLATE_LAPIS_ORE);
            }

            // 4 cave-mouth entrances: 7-wide × 12-tall (AIR in wall AND ceiling)
            // North
            fill(es, cx - 3, 65, cz - 62, cx + 3, 76, cz - 60, BlockTypes.AIR);
            fill(es, cx - 3, 78, cz - 61, cx + 3, 80, cz - 61, BlockTypes.AIR); // ceiling match
            // South
            fill(es, cx - 3, 65, cz + 60, cx + 3, 76, cz + 62, BlockTypes.AIR);
            fill(es, cx - 3, 78, cz + 61, cx + 3, 80, cz + 61, BlockTypes.AIR);
            // West
            fill(es, cx - 62, 65, cz - 3, cx - 60, 76, cz + 3, BlockTypes.AIR);
            fill(es, cx - 61, 78, cz - 3, cx - 61, 80, cz + 3, BlockTypes.AIR);
            // East
            fill(es, cx + 60, 65, cz - 3, cx + 62, 76, cz + 3, BlockTypes.AIR);
            fill(es, cx + 61, 78, cz - 3, cx + 61, 80, cz + 3, BlockTypes.AIR);

            // ------------------------------------------------------------------
            // INTERIOR OBSTACLES
            // ------------------------------------------------------------------
            // 6 DEEPSLATE_BRICKS stalagmite-pillars (2×2 base, 10 tall)
            int[][] stalagPositions = {
                {cx + 18, cz + 18}, {cx - 18, cz + 18},
                {cx + 18, cz - 18}, {cx - 18, cz - 18},
                {cx + 40, cz},      {cx - 40, cz}
            };
            for (int[] sp2 : stalagPositions) {
                fill(es, sp2[0], AY, sp2[1], sp2[0] + 1, AY + 9, sp2[1] + 1,
                        BlockTypes.DEEPSLATE_BRICKS);
                block(es, sp2[0], AY + 10, sp2[1], BlockTypes.SHROOMLIGHT);
            }

            // 4 STONE ore-pillar obstacles (3×3, 8 tall) with ore inlays on faces
            int[][] orePillars = {
                {cx + 30, cz + 30}, {cx - 30, cz + 30},
                {cx + 30, cz - 30}, {cx - 30, cz - 30}
            };
            for (int[] op : orePillars) {
                fill(es, op[0] - 1, AY, op[1] - 1, op[0] + 1, AY + 7, op[1] + 1,
                        BlockTypes.STONE);
                // Ore inlays on faces
                for (int oy = AY; oy <= AY + 7; oy += 2) {
                    block(es, op[0] - 1, oy, op[1], BlockTypes.DEEPSLATE_IRON_ORE);
                    block(es, op[0] + 1, oy, op[1], BlockTypes.DEEPSLATE_COAL_ORE);
                    block(es, op[0], oy, op[1] - 1, BlockTypes.DEEPSLATE_IRON_ORE);
                    block(es, op[0], oy, op[1] + 1, BlockTypes.DEEPSLATE_COAL_ORE);
                }
            }

            // 2 CAVE_AIR pit hazards: 5×5 × 3-deep, IRON_BARS grate at y=64
            // Pit 1: NW of centre
            fill(es, cx - 12, AY - 3, cz - 12, cx - 8, AY - 1, cz - 8, BlockTypes.CAVE_AIR);
            fill(es, cx - 13, AY, cz - 13, cx - 7, AY, cz - 7, BlockTypes.IRON_BARS);
            // Pit 2: SE of centre
            fill(es, cx + 8, AY - 3, cz + 8, cx + 12, AY - 1, cz + 12, BlockTypes.CAVE_AIR);
            fill(es, cx + 7, AY, cz + 7, cx + 13, AY, cz + 13, BlockTypes.IRON_BARS);

            // 4 low COBBLED_DEEPSLATE cover walls (2-tall, 10-long)
            fill(es, cx + 8,  65, cz - 1, cx + 17, 66, cz - 1, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 17, 65, cz - 1, cx - 8,  66, cz - 1, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 1, 65, cz + 8,  cx - 1, 66, cz + 17, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx - 1, 65, cz - 17, cx - 1, 66, cz - 8,  BlockTypes.COBBLED_DEEPSLATE);

            // SCULK patches on walls (use DEEPSLATE as fallback — SCULK may not exist)
            for (int i = 0; i < 8; i++) {
                int wOff = (i * 13 + 7) % 100 - 50;
                int wH = 66 + (i * 3) % 8;
                block(es, cx + wOff, wH, cz - 61, BlockTypes.SCULK);
                block(es, cx + wOff, wH, cz + 61, BlockTypes.SCULK);
            }

            // GRAVEL scatter throughout floor
            int[][] gravelScatter = {
                {cx + 5, cz + 8}, {cx - 5, cz + 8}, {cx + 8, cz - 5}, {cx - 8, cz - 5},
                {cx + 22, cz + 18},{cx - 22, cz + 18},{cx + 22, cz - 18},{cx - 22, cz - 18}
            };
            for (int[] gs : gravelScatter) {
                block(es, gs[0], AY, gs[1], BlockTypes.GRAVEL);
            }

            // ------------------------------------------------------------------
            // SPAWN MARKERS
            // ------------------------------------------------------------------
            int[][] spawnPositions = {
                {cx + 40, cz},  {cx - 40, cz},
                {cx, cz + 40},  {cx, cz - 40}
            };
            for (int[] sp : spawnPositions) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1,
                        BlockTypes.COBBLED_DEEPSLATE);
                block(es, sp[0], AY, sp[1], BlockTypes.GLOWSTONE);
            }

            // ------------------------------------------------------------------
            // UNDER-PLATFORM: DEEPSLATE stalactites
            // ------------------------------------------------------------------
            sphere(es, cx,       55, cz,       4, BlockTypes.DEEPSLATE);
            sphere(es, cx + 30,  55, cz - 25,  3, BlockTypes.COBBLED_DEEPSLATE);
            sphere(es, cx - 28,  55, cz + 22,  3, BlockTypes.DEEPSLATE_BRICKS);
            // Hanging columns
            column(es, cx + 10, cz + 10, 50, 59, BlockTypes.COBBLED_DEEPSLATE);
            column(es, cx - 10, cz - 10, 52, 59, BlockTypes.DEEPSLATE);
        }
    }

    // =========================================================================
    // NOTE: buildTridentArena(), buildShieldArena(), and all FFA arena methods
    //       are implemented in part 2 of this file (appended separately).
    // =========================================================================
    // =========================================================================
    // TRIDENT ARENA — cx=220, cz=220  (130×130 practice platform)
    // Theme: Ocean / Prismarine / Water — submerged temple aesthetic
    // =========================================================================
    private void buildTridentArena() {
        final int cx = 220, cz = 220, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 130×130, 4 blocks thick (y=60..63) ---
            fill(es, cx - 65, 60, cz - 65, cx + 65, 63, cz + 65, BlockTypes.DARK_PRISMARINE);

            // --- Main floor 120×120: PRISMARINE_BRICKS overall ---
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.PRISMARINE_BRICKS);

            // --- PRISMARINE border ring 5-wide at perimeter of 120×120 ---
            // North border
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz - 56, BlockTypes.PRISMARINE);
            // South border
            fill(es, cx - 60, AY, cz + 56, cx + 60, AY, cz + 60, BlockTypes.PRISMARINE);
            // West border
            fill(es, cx - 60, AY, cz - 60, cx - 56, AY, cz + 60, BlockTypes.PRISMARINE);
            // East border
            fill(es, cx + 56, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.PRISMARINE);

            // --- DARK_PRISMARINE checkerboard in 20×20 centre zone ---
            for (int x = cx - 10; x <= cx + 10; x++) {
                for (int z = cz - 10; z <= cz + 10; z++) {
                    if ((x + z) % 2 == 0) {
                        block(es, x, AY, z, BlockTypes.DARK_PRISMARINE);
                    }
                }
            }

            // --- Cross-shaped WATER channels (N/S: x=217..223, E/W: z=217..223) ---
            // N/S channel: DARK_PRISMARINE floor at y=62, WATER at y=63, open at AY=64
            fill(es, 217, 62, cz - 58, 223, 62, cz + 58, BlockTypes.DARK_PRISMARINE);
            fill(es, 217, 63, cz - 58, 223, 63, cz + 58, BlockTypes.WATER);
            fill(es, 217, AY, cz - 58, 223, AY, cz + 58, BlockTypes.AIR);
            // E/W channel: DARK_PRISMARINE floor at y=62, WATER at y=63, open at AY
            fill(es, cx - 58, 62, 217, cx + 58, 62, 223, BlockTypes.DARK_PRISMARINE);
            fill(es, cx - 58, 63, 217, cx + 58, 63, 223, BlockTypes.WATER);
            fill(es, cx - 58, AY, 217, cx + 58, AY, 223, BlockTypes.AIR);
            // IRON_BARS grate at y=64 over water channels
            fill(es, 217, AY, cz - 58, 223, AY, cz + 58, BlockTypes.IRON_BARS);
            fill(es, cx - 58, AY, 217, cx + 58, AY, 223, BlockTypes.IRON_BARS);

            // --- 4 pools (7×7) at quadrant positions (cx±30, cz±30) ---
            int[][] poolCentres = {{cx - 30, cz - 30}, {cx + 30, cz - 30},
                                   {cx - 30, cz + 30}, {cx + 30, cz + 30}};
            for (int[] pc : poolCentres) {
                int px = pc[0], pz = pc[1];
                // DARK_PRISMARINE pool floor at y=62
                fill(es, px - 3, 62, pz - 3, px + 3, 62, pz + 3, BlockTypes.DARK_PRISMARINE);
                // WATER fill at y=63
                fill(es, px - 3, 63, pz - 3, px + 3, 63, pz + 3, BlockTypes.WATER);
                // Clear y=64 (pool surface open)
                fill(es, px - 3, AY, pz - 3, px + 3, AY, pz + 3, BlockTypes.AIR);
                // SEA_LANTERN at y=62 centre
                block(es, px, 62, pz, BlockTypes.SEA_LANTERN);
                // LILY_PAD at surface y=64 on a few spots
                block(es, px - 1, AY, pz - 1, BlockTypes.LILY_PAD);
                block(es, px + 1, AY, pz + 1, BlockTypes.LILY_PAD);
                block(es, px - 1, AY, pz + 2, BlockTypes.LILY_PAD);
            }

            // --- SEA_LANTERN grid in floor every 8 blocks ---
            for (int x = cx - 56; x <= cx + 56; x += 8) {
                for (int z = cz - 56; z <= cz + 56; z += 8) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // --- GLOWSTONE under water channels (y=62) ---
            for (int x = 217; x <= 223; x += 3) {
                block(es, x, 62, cz - 40, BlockTypes.GLOWSTONE);
                block(es, x, 62, cz,      BlockTypes.GLOWSTONE);
                block(es, x, 62, cz + 40, BlockTypes.GLOWSTONE);
            }

            // --- Floor PRISMARINE scatter (200 positions via hash) ---
            for (int i = 0; i < 200; i++) {
                int fx = cx - 55 + (i * 13 + 7) % 111;
                int fz = cz - 55 + (i * 17 + 3) % 111;
                // Only place if not on channel or pool
                boolean onChannel = (fx >= 217 && fx <= 223) || (fz >= 217 && fz <= 223);
                if (!onChannel) block(es, fx, AY, fz, BlockTypes.PRISMARINE);
            }

            // ==================== WALLS ====================
            // Walls y=65..80, 16 tall, on 120×120 perimeter
            // PRISMARINE_BRICKS outer, DARK_PRISMARINE inner
            for (int y = AY + 1; y <= AY + 16; y++) {
                // North wall outer
                fill(es, cx - 60, y, cz - 60, cx + 60, y, cz - 60, BlockTypes.PRISMARINE_BRICKS);
                // North wall inner (1 block in)
                fill(es, cx - 59, y, cz - 59, cx + 59, y, cz - 59, BlockTypes.DARK_PRISMARINE);
                // South wall outer
                fill(es, cx - 60, y, cz + 60, cx + 60, y, cz + 60, BlockTypes.PRISMARINE_BRICKS);
                fill(es, cx - 59, y, cz + 59, cx + 59, y, cz + 59, BlockTypes.DARK_PRISMARINE);
                // West wall outer
                fill(es, cx - 60, y, cz - 60, cx - 60, y, cz + 60, BlockTypes.PRISMARINE_BRICKS);
                fill(es, cx - 59, y, cz - 59, cx - 59, y, cz + 59, BlockTypes.DARK_PRISMARINE);
                // East wall outer
                fill(es, cx + 60, y, cz - 60, cx + 60, y, cz + 60, BlockTypes.PRISMARINE_BRICKS);
                fill(es, cx + 59, y, cz - 59, cx + 59, y, cz + 59, BlockTypes.DARK_PRISMARINE);
            }

            // --- TINTED_GLASS windows every 5 blocks (3-wide × 8-tall, y=66..73) ---
            // North wall windows
            for (int wx = cx - 50; wx <= cx + 50; wx += 10) {
                fill(es, wx - 1, AY + 2, cz - 60, wx + 1, AY + 9, cz - 60, BlockTypes.TINTED_GLASS);
                fill(es, wx - 1, AY + 2, cz + 60, wx + 1, AY + 9, cz + 60, BlockTypes.TINTED_GLASS);
            }
            for (int wz = cz - 50; wz <= cz + 50; wz += 10) {
                fill(es, cx - 60, AY + 2, wz - 1, cx - 60, AY + 9, wz + 1, BlockTypes.TINTED_GLASS);
                fill(es, cx + 60, AY + 2, wz - 1, cx + 60, AY + 9, wz + 1, BlockTypes.TINTED_GLASS);
            }

            // --- SEA_LANTERN embedded in wall every 4 blocks at mid-height (y=AY+8) ---
            for (int wx = cx - 56; wx <= cx + 56; wx += 4) {
                block(es, wx, AY + 8, cz - 60, BlockTypes.SEA_LANTERN);
                block(es, wx, AY + 8, cz + 60, BlockTypes.SEA_LANTERN);
            }
            for (int wz = cz - 56; wz <= cz + 56; wz += 4) {
                block(es, cx - 60, AY + 8, wz, BlockTypes.SEA_LANTERN);
                block(es, cx + 60, AY + 8, wz, BlockTypes.SEA_LANTERN);
            }

            // --- 4 sea-gate entrances (9-wide × 12-tall) at cardinal faces ---
            // North gate (z=cz-60), centred at x=cx
            for (int y = AY + 1; y <= AY + 12; y++) {
                fill(es, cx - 4, y, cz - 60, cx + 4, y, cz - 60, BlockTypes.AIR);
                fill(es, cx - 4, y, cz - 59, cx + 4, y, cz - 59, BlockTypes.AIR);
            }
            // South gate
            for (int y = AY + 1; y <= AY + 12; y++) {
                fill(es, cx - 4, y, cz + 60, cx + 4, y, cz + 60, BlockTypes.AIR);
                fill(es, cx - 4, y, cz + 59, cx + 4, y, cz + 59, BlockTypes.AIR);
            }
            // West gate
            for (int y = AY + 1; y <= AY + 12; y++) {
                fill(es, cx - 60, y, cz - 4, cx - 60, y, cz + 4, BlockTypes.AIR);
                fill(es, cx - 59, y, cz - 4, cx - 59, y, cz + 4, BlockTypes.AIR);
            }
            // East gate
            for (int y = AY + 1; y <= AY + 12; y++) {
                fill(es, cx + 60, y, cz - 4, cx + 60, y, cz + 4, BlockTypes.AIR);
                fill(es, cx + 59, y, cz - 4, cx + 59, y, cz + 4, BlockTypes.AIR);
            }

            // --- 4 corner towers (9×9, 22 tall) at cx±55, cz±55 ---
            int[][] towerCentres = {{cx - 55, cz - 55}, {cx + 55, cz - 55},
                                    {cx - 55, cz + 55}, {cx + 55, cz + 55}};
            for (int[] tc : towerCentres) {
                int tx = tc[0], tz = tc[1];
                // Tower shell: hollow 9×9 from y=AY to AY+22
                fillHollow(es, tx - 4, AY, tz - 4, tx + 4, AY + 22, tz + 4,
                        BlockTypes.PRISMARINE_BRICKS, BlockTypes.AIR);
                // IRON_BARS windows on each face, every 4 blocks height
                for (int wy = AY + 3; wy <= AY + 18; wy += 4) {
                    block(es, tx - 4, wy, tz,     BlockTypes.IRON_BARS);
                    block(es, tx + 4, wy, tz,     BlockTypes.IRON_BARS);
                    block(es, tx,     wy, tz - 4, BlockTypes.IRON_BARS);
                    block(es, tx,     wy, tz + 4, BlockTypes.IRON_BARS);
                }
                // SEA_LANTERN cap ring at AY+22
                for (int dx = -4; dx <= 4; dx++) {
                    block(es, tx + dx, AY + 22, tz - 4, BlockTypes.SEA_LANTERN);
                    block(es, tx + dx, AY + 22, tz + 4, BlockTypes.SEA_LANTERN);
                }
                for (int dz = -3; dz <= 3; dz++) {
                    block(es, tx - 4, AY + 22, tz + dz, BlockTypes.SEA_LANTERN);
                    block(es, tx + 4, AY + 22, tz + dz, BlockTypes.SEA_LANTERN);
                }
                // DARK_PRISMARINE pyramid roof 5 layers above AY+22
                for (int layer = 0; layer < 5; layer++) {
                    int size = 4 - layer;
                    fill(es, tx - size, AY + 23 + layer, tz - size,
                             tx + size, AY + 23 + layer, tz + size, BlockTypes.DARK_PRISMARINE);
                }
            }

            // --- Wall top merlons: DARK_PRISMARINE every 2 blocks, SEA_LANTERN at top ---
            for (int wx = cx - 59; wx <= cx + 59; wx += 2) {
                block(es, wx, AY + 17, cz - 60, BlockTypes.DARK_PRISMARINE);
                block(es, wx, AY + 17, cz + 60, BlockTypes.DARK_PRISMARINE);
                block(es, wx, AY + 18, cz - 60, BlockTypes.SEA_LANTERN);
                block(es, wx, AY + 18, cz + 60, BlockTypes.SEA_LANTERN);
            }
            for (int wz = cz - 59; wz <= cz + 59; wz += 2) {
                block(es, cx - 60, AY + 17, wz, BlockTypes.DARK_PRISMARINE);
                block(es, cx + 60, AY + 17, wz, BlockTypes.DARK_PRISMARINE);
                block(es, cx - 60, AY + 18, wz, BlockTypes.SEA_LANTERN);
                block(es, cx + 60, AY + 18, wz, BlockTypes.SEA_LANTERN);
            }

            // ==================== INTERIOR OBSTACLES ====================

            // --- Central temple ---
            // 11×11 PRISMARINE_BRICKS base at y=AY (already floor), raise to y=AY+1
            fill(es, cx - 5, AY + 1, cz - 5, cx + 5, AY + 1, cz + 5, BlockTypes.PRISMARINE_BRICKS);
            // 7×7 second tier at y=AY+2
            fill(es, cx - 3, AY + 2, cz - 3, cx + 3, AY + 2, cz + 3, BlockTypes.PRISMARINE_BRICKS);
            // 3×3 DARK_PRISMARINE pillar y=AY+3..AY+10
            fill(es, cx - 1, AY + 3, cz - 1, cx + 1, AY + 10, cz + 1, BlockTypes.DARK_PRISMARINE);
            // SEA_LANTERN at top y=AY+11
            block(es, cx, AY + 11, cz, BlockTypes.SEA_LANTERN);
            // 4 PRISMARINE_BRICKS buttresses
            fill(es, cx - 1, AY + 1, cz - 7, cx + 1, AY + 3, cz - 5, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx - 1, AY + 1, cz + 5, cx + 1, AY + 3, cz + 7, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx - 7, AY + 1, cz - 1, cx - 5, AY + 3, cz + 1, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx + 5, AY + 1, cz - 1, cx + 7, AY + 3, cz + 1, BlockTypes.PRISMARINE_BRICKS);

            // --- 8 SEA_LANTERN pillars (2×2 base, 10 tall) at r=28 ring ---
            double[] pillarAngles = {0, Math.PI/4, Math.PI/2, 3*Math.PI/4,
                                     Math.PI, 5*Math.PI/4, 3*Math.PI/2, 7*Math.PI/4};
            for (double angle : pillarAngles) {
                int px = cx + (int) Math.round(28 * Math.cos(angle));
                int pz = cz + (int) Math.round(28 * Math.sin(angle));
                fill(es, px, AY + 1, pz, px + 1, AY + 10, pz + 1, BlockTypes.SEA_LANTERN);
                // Chains 2 blocks down each side at top
                block(es, px,     AY + 10, pz - 1, BlockTypes.CHAIN);
                block(es, px,     AY + 9,  pz - 1, BlockTypes.CHAIN);
                block(es, px + 1, AY + 10, pz - 1, BlockTypes.CHAIN);
                block(es, px + 1, AY + 9,  pz - 1, BlockTypes.CHAIN);
                block(es, px,     AY + 10, pz + 2, BlockTypes.CHAIN);
                block(es, px,     AY + 9,  pz + 2, BlockTypes.CHAIN);
            }

            // --- 4 PRISMARINE arch structures at r=40 diagonal axes ---
            int[][] archPositions = {{cx - 28, cz - 28}, {cx + 28, cz - 28},
                                     {cx - 28, cz + 28}, {cx + 28, cz + 28}};
            for (int[] ap : archPositions) {
                int ax = ap[0], az = ap[1];
                // Two pillars
                fill(es, ax - 3, AY + 1, az, ax - 3, AY + 8, az, BlockTypes.PRISMARINE_BRICKS);
                fill(es, ax + 3, AY + 1, az, ax + 3, AY + 8, az, BlockTypes.PRISMARINE_BRICKS);
                // Arch top
                fill(es, ax - 3, AY + 8, az, ax + 3, AY + 8, az, BlockTypes.PRISMARINE_BRICKS);
                block(es, ax, AY + 9, az, BlockTypes.SEA_LANTERN);
            }

            // --- 4 DARK_PRISMARINE low barriers (2-tall, 12-long) at cardinal r=18 ---
            fill(es, cx - 6, AY + 1, cz - 18, cx + 6, AY + 2, cz - 18, BlockTypes.DARK_PRISMARINE);
            fill(es, cx - 6, AY + 1, cz + 18, cx + 6, AY + 2, cz + 18, BlockTypes.DARK_PRISMARINE);
            fill(es, cx - 18, AY + 1, cz - 6, cx - 18, AY + 2, cz + 6, BlockTypes.DARK_PRISMARINE);
            fill(es, cx + 18, AY + 1, cz - 6, cx + 18, AY + 2, cz + 6, BlockTypes.DARK_PRISMARINE);

            // --- Spawn markers: 4 positions at cx±42,cz and cx,cz±42 ---
            int[][] spawnPos = {{cx - 42, cz}, {cx + 42, cz}, {cx, cz - 42}, {cx, cz + 42}};
            for (int[] sp : spawnPos) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1,
                        BlockTypes.PRISMARINE_BRICKS);
                block(es, sp[0], AY + 1, sp[1], BlockTypes.SEA_LANTERN);
            }

            // --- Under-platform features ---
            // DARK_PRISMARINE hanging stalactites at various positions
            int[][] stalPositions = {
                {cx - 40, cz - 40}, {cx + 40, cz - 40}, {cx - 40, cz + 40}, {cx + 40, cz + 40},
                {cx - 20, cz - 50}, {cx + 20, cz + 50}, {cx - 50, cz + 20}, {cx + 50, cz - 20}
            };
            for (int[] sp : stalPositions) {
                column(es, sp[0], sp[1], 55, 59, BlockTypes.DARK_PRISMARINE);
            }
            // 3 underwater sphere features at y=55
            sphere(es, cx - 35, 55, cz - 35, 4, BlockTypes.DARK_PRISMARINE);
            sphere(es, cx + 35, 55, cz + 35, 4, BlockTypes.DARK_PRISMARINE);
            sphere(es, cx,      55, cz,      4, BlockTypes.DARK_PRISMARINE);
        }
    }

    // =========================================================================
    // SHIELD ARENA — cx=-220, cz=220  (130×130 practice platform)
    // Theme: Fortress / Iron / Battleground — war-torn citadel
    // =========================================================================
    private void buildShieldArena() {
        final int cx = -220, cz = 220, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 130×130, 4 blocks thick ---
            fill(es, cx - 65, 60, cz - 65, cx + 65, 63, cz + 65, BlockTypes.STONE);

            // --- Main floor 120×120: POLISHED_ANDESITE ---
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.POLISHED_ANDESITE);

            // --- CHISELED_STONE_BRICKS 5-wide border ---
            fill(es, cx - 60, AY, cz - 60, cx + 60, AY, cz - 56, BlockTypes.CHISELED_STONE_BRICKS);
            fill(es, cx - 60, AY, cz + 56, cx + 60, AY, cz + 60, BlockTypes.CHISELED_STONE_BRICKS);
            fill(es, cx - 60, AY, cz - 60, cx - 56, AY, cz + 60, BlockTypes.CHISELED_STONE_BRICKS);
            fill(es, cx + 56, AY, cz - 60, cx + 60, AY, cz + 60, BlockTypes.CHISELED_STONE_BRICKS);

            // --- SMOOTH_STONE paths (3-wide) N/S and E/W crossing ---
            fill(es, cx - 1, AY, cz - 58, cx + 1, AY, cz + 58, BlockTypes.SMOOTH_STONE);
            fill(es, cx - 58, AY, cz - 1, cx + 58, AY, cz + 1, BlockTypes.SMOOTH_STONE);

            // --- IRON_BLOCK 3×3 compass rose inlaid at centre (N/S/E/W arms 12-long) ---
            fill(es, cx - 1, AY, cz - 1, cx + 1, AY, cz + 1, BlockTypes.IRON_BLOCK);
            fill(es, cx - 1, AY, cz - 13, cx + 1, AY, cz - 2,  BlockTypes.IRON_BLOCK);
            fill(es, cx - 1, AY, cz + 2,  cx + 1, AY, cz + 13, BlockTypes.IRON_BLOCK);
            fill(es, cx - 13, AY, cz - 1, cx - 2, AY, cz + 1,  BlockTypes.IRON_BLOCK);
            fill(es, cx + 2,  AY, cz - 1, cx + 13, AY, cz + 1, BlockTypes.IRON_BLOCK);

            // --- CRACKED_STONE_BRICKS scatter patches (30 positions, 2×2) ---
            int[] rubbleX = {-50,-40,-30,-20,-10, 5, 15, 25, 35, 45,
                             -48,-35,-22, -8,  4, 18, 30, 42,-42,-28,
                              -3, 10, 22,-15,-38, 38,-25, 12,-52, 48};
            int[] rubbleZ = { 30, 42, 10,-35,-48,-20, 38,-50, 22,-10,
                             -28, 15,-45, 28,-38, -5, 50,-30,  5, 40,
                              48,-42,-18, 35,-15,  8,-30,-55, 20,-45};
            for (int i = 0; i < 30; i++) {
                int rx = cx + rubbleX[i], rz = cz + rubbleZ[i];
                fill(es, rx, AY, rz, rx + 1, AY, rz + 1, BlockTypes.CRACKED_STONE_BRICKS);
            }

            // --- SEA_LANTERN grid in floor every 8 blocks ---
            for (int x = cx - 56; x <= cx + 56; x += 8) {
                for (int z = cz - 56; z <= cz + 56; z += 8) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // ==================== WALLS (y=65..82, 18 tall) ====================
            for (int y = AY + 1; y <= AY + 18; y++) {
                // Outer STONE_BRICKS walls
                fill(es, cx - 60, y, cz - 60, cx + 60, y, cz - 60, BlockTypes.STONE_BRICKS);
                fill(es, cx - 60, y, cz + 60, cx + 60, y, cz + 60, BlockTypes.STONE_BRICKS);
                fill(es, cx - 60, y, cz - 60, cx - 60, y, cz + 60, BlockTypes.STONE_BRICKS);
                fill(es, cx + 60, y, cz - 60, cx + 60, y, cz + 60, BlockTypes.STONE_BRICKS);
                // CHISELED_STONE_BRICKS every 5th column
                for (int wx = cx - 55; wx <= cx + 55; wx += 5) {
                    block(es, wx, y, cz - 60, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, wx, y, cz + 60, BlockTypes.CHISELED_STONE_BRICKS);
                }
                for (int wz = cz - 55; wz <= cz + 55; wz += 5) {
                    block(es, cx - 60, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, cx + 60, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                }
            }

            // --- IRON_BARS window row at y=72..74 ---
            for (int y = AY + 8; y <= AY + 10; y++) {
                fill(es, cx - 59, y, cz - 60, cx + 59, y, cz - 60, BlockTypes.IRON_BARS);
                fill(es, cx - 59, y, cz + 60, cx + 59, y, cz + 60, BlockTypes.IRON_BARS);
                fill(es, cx - 60, y, cz - 59, cx - 60, y, cz + 59, BlockTypes.IRON_BARS);
                fill(es, cx + 60, y, cz - 59, cx + 60, y, cz + 59, BlockTypes.IRON_BARS);
            }

            // --- 4 fortress gateways (9-wide × 14-tall) ---
            for (int y = AY + 1; y <= AY + 14; y++) {
                // North gate
                fill(es, cx - 4, y, cz - 60, cx + 4, y, cz - 60, BlockTypes.AIR);
                // South gate
                fill(es, cx - 4, y, cz + 60, cx + 4, y, cz + 60, BlockTypes.AIR);
                // West gate
                fill(es, cx - 60, y, cz - 4, cx - 60, y, cz + 4, BlockTypes.AIR);
                // East gate
                fill(es, cx + 60, y, cz - 4, cx + 60, y, cz + 4, BlockTypes.AIR);
            }
            // IRON_BARS portcullis at half gate height
            fill(es, cx - 4, AY + 7, cz - 60, cx + 4, AY + 8, cz - 60, BlockTypes.IRON_BARS);
            fill(es, cx - 4, AY + 7, cz + 60, cx + 4, AY + 8, cz + 60, BlockTypes.IRON_BARS);
            fill(es, cx - 60, AY + 7, cz - 4, cx - 60, AY + 8, cz + 4, BlockTypes.IRON_BARS);
            fill(es, cx + 60, AY + 7, cz - 4, cx + 60, AY + 8, cz + 4, BlockTypes.IRON_BARS);

            // --- SOUL_LANTERN flanking gate entrances ---
            int[][] gateFlank = {
                {cx - 6, cz - 60}, {cx + 6, cz - 60},
                {cx - 6, cz + 60}, {cx + 6, cz + 60},
                {cx - 60, cz - 6}, {cx - 60, cz + 6},
                {cx + 60, cz - 6}, {cx + 60, cz + 6}
            };
            for (int[] gf : gateFlank) {
                block(es, gf[0], AY + 1, gf[1], BlockTypes.SOUL_LANTERN);
            }

            // --- 4 massive IRON_BLOCK bastions (11×11, 28 tall) at corners ---
            int[][] bastionCentres = {{cx - 55, cz - 55}, {cx + 55, cz - 55},
                                      {cx - 55, cz + 55}, {cx + 55, cz + 55}};
            for (int[] bc : bastionCentres) {
                int bx = bc[0], bz = bc[1];
                // Hollow bastion
                fillHollow(es, bx - 5, AY, bz - 5, bx + 5, AY + 28, bz + 5,
                        BlockTypes.IRON_BLOCK, BlockTypes.AIR);
                // IRON_BARS windows every 4 blocks
                for (int wy = AY + 4; wy <= AY + 24; wy += 4) {
                    block(es, bx - 5, wy, bz,     BlockTypes.IRON_BARS);
                    block(es, bx + 5, wy, bz,     BlockTypes.IRON_BARS);
                    block(es, bx,     wy, bz - 5, BlockTypes.IRON_BARS);
                    block(es, bx,     wy, bz + 5, BlockTypes.IRON_BARS);
                }
                // IRON_BLOCK battlements at top
                for (int dx = -5; dx <= 5; dx += 2) {
                    block(es, bx + dx, AY + 29, bz - 5, BlockTypes.IRON_BLOCK);
                    block(es, bx + dx, AY + 29, bz + 5, BlockTypes.IRON_BLOCK);
                }
                for (int dz = -4; dz <= 4; dz += 2) {
                    block(es, bx - 5, AY + 29, bz + dz, BlockTypes.IRON_BLOCK);
                    block(es, bx + 5, AY + 29, bz + dz, BlockTypes.IRON_BLOCK);
                }
                // SEA_LANTERN ring at y=AY+25
                for (int dx = -5; dx <= 5; dx++) {
                    block(es, bx + dx, AY + 25, bz - 5, BlockTypes.SEA_LANTERN);
                    block(es, bx + dx, AY + 25, bz + 5, BlockTypes.SEA_LANTERN);
                }
                for (int dz = -4; dz <= 4; dz++) {
                    block(es, bx - 5, AY + 25, bz + dz, BlockTypes.SEA_LANTERN);
                    block(es, bx + 5, AY + 25, bz + dz, BlockTypes.SEA_LANTERN);
                }
                // BEACON on top with IRON_BLOCK 3×3
                fill(es, bx - 1, AY + 29, bz - 1, bx + 1, AY + 29, bz + 1, BlockTypes.IRON_BLOCK);
                block(es, bx, AY + 30, bz, BlockTypes.BEACON);
                // GLOWSTONE inside
                block(es, bx, AY + 1, bz, BlockTypes.GLOWSTONE);
                block(es, bx, AY + 8, bz, BlockTypes.GLOWSTONE);
                block(es, bx, AY + 16, bz, BlockTypes.GLOWSTONE);
            }

            // --- Wall top STONE_BRICKS battlements 3-tall every 2 blocks ---
            for (int wx = cx - 59; wx <= cx + 59; wx += 2) {
                fill(es, wx, AY + 19, cz - 60, wx, AY + 21, cz - 60, BlockTypes.STONE_BRICKS);
                fill(es, wx, AY + 19, cz + 60, wx, AY + 21, cz + 60, BlockTypes.STONE_BRICKS);
                // SEA_LANTERN between merlons
                block(es, wx + 1, AY + 19, cz - 60, BlockTypes.SEA_LANTERN);
                block(es, wx + 1, AY + 19, cz + 60, BlockTypes.SEA_LANTERN);
            }
            for (int wz = cz - 59; wz <= cz + 59; wz += 2) {
                fill(es, cx - 60, AY + 19, wz, cx - 60, AY + 21, wz, BlockTypes.STONE_BRICKS);
                fill(es, cx + 60, AY + 19, wz, cx + 60, AY + 21, wz, BlockTypes.STONE_BRICKS);
                block(es, cx - 60, AY + 19, wz + 1, BlockTypes.SEA_LANTERN);
                block(es, cx + 60, AY + 19, wz + 1, BlockTypes.SEA_LANTERN);
            }

            // ==================== INTERIOR OBSTACLES ====================

            // --- 4 broken wall segments (CRACKED_STONE_BRICKS 2×15×4 tall) at r=30 diagonal ---
            int[][] brokenWallPos = {{cx - 21, cz - 21}, {cx + 21, cz - 21},
                                     {cx - 21, cz + 21}, {cx + 21, cz + 21}};
            for (int[] bw : brokenWallPos) {
                fill(es, bw[0] - 7, AY + 1, bw[1], bw[0] + 7, AY + 4, bw[1] + 1,
                        BlockTypes.CRACKED_STONE_BRICKS);
            }

            // --- 4 IRON_BLOCK bunkers (5×5×3 tall, hollow, 1-block door) at r=22 diagonal ---
            int[][] bunkerPos = {{cx - 16, cz - 16}, {cx + 16, cz - 16},
                                 {cx - 16, cz + 16}, {cx + 16, cz + 16}};
            for (int[] bp : bunkerPos) {
                fillHollow(es, bp[0] - 2, AY + 1, bp[1] - 2, bp[0] + 2, AY + 3, bp[1] + 2,
                        BlockTypes.IRON_BLOCK, BlockTypes.AIR);
                // Door opening
                block(es, bp[0], AY + 1, bp[1] - 2, BlockTypes.AIR);
                block(es, bp[0], AY + 2, bp[1] - 2, BlockTypes.AIR);
            }

            // --- Rubble piles (COBBLESTONE/ANDESITE 3×3×2 tall) at 12 positions ---
            int[] rubX2 = {cx-45, cx+45, cx-30, cx+30, cx-45, cx+45, cx, cx-15, cx+15, cx-38, cx+38, cx};
            int[] rubZ2 = {cz+15, cz-15, cz+45, cz-45, cz-30, cz+30, cz+50, cz-40, cz+40, cz+5, cz-5, cz-50};
            for (int i = 0; i < 12; i++) {
                fill(es, rubX2[i]-1, AY+1, rubZ2[i]-1, rubX2[i]+1, AY+2, rubZ2[i]+1,
                        (i % 2 == 0) ? BlockTypes.COBBLESTONE : BlockTypes.ANDESITE);
            }

            // --- 8 tall IRON_BARS fence dividers (3-tall, 10-long) ---
            int[][] fencePos = {{cx - 40, cz - 10}, {cx + 40, cz - 10},
                                {cx - 40, cz + 10}, {cx + 40, cz + 10},
                                {cx - 10, cz - 40}, {cx + 10, cz - 40},
                                {cx - 10, cz + 40}, {cx + 10, cz + 40}};
            for (int[] fp : fencePos) {
                fill(es, fp[0], AY + 1, fp[1], fp[0] + 9, AY + 3, fp[1], BlockTypes.IRON_BARS);
            }

            // --- CRACKED_STONE_BRICKS column obstacles (3×3, 8 tall) × 4 ---
            int[][] colPos = {{cx - 50, cz}, {cx + 50, cz}, {cx, cz - 50}, {cx, cz + 50}};
            for (int[] cp : colPos) {
                fill(es, cp[0] - 1, AY + 1, cp[1] - 1, cp[0] + 1, AY + 8, cp[1] + 1,
                        BlockTypes.CRACKED_STONE_BRICKS);
            }

            // --- LANTERN posts × 8 at r=45 ring ---
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int lx = cx + (int) Math.round(45 * Math.cos(angle));
                int lz = cz + (int) Math.round(45 * Math.sin(angle));
                column(es, lx, lz, AY + 1, AY + 3, BlockTypes.IRON_BLOCK);
                block(es, lx, AY + 4, lz, BlockTypes.LANTERN);
            }

            // --- 10 crumbling wall sections (2-wide gap with CRACKED_STONE_BRICKS debris) ---
            int[] crumbleOffsets = {-48,-32,-16, 0, 16, 32, 48,-40,-8, 24};
            for (int i = 0; i < 10; i++) {
                int off = crumbleOffsets[i];
                // Alternate between N, S, W, E walls
                if (i % 4 == 0) {
                    for (int y = AY + 1; y <= AY + 6; y++) {
                        fill(es, cx + off, y, cz - 60, cx + off + 1, y, cz - 60, BlockTypes.AIR);
                    }
                    fill(es, cx + off, AY, cz - 60, cx + off + 1, AY, cz - 59,
                            BlockTypes.CRACKED_STONE_BRICKS);
                } else if (i % 4 == 1) {
                    for (int y = AY + 1; y <= AY + 6; y++) {
                        fill(es, cx + off, y, cz + 60, cx + off + 1, y, cz + 60, BlockTypes.AIR);
                    }
                    fill(es, cx + off, AY, cz + 59, cx + off + 1, AY, cz + 60,
                            BlockTypes.CRACKED_STONE_BRICKS);
                } else if (i % 4 == 2) {
                    for (int y = AY + 1; y <= AY + 6; y++) {
                        fill(es, cx - 60, y, cz + off, cx - 60, y, cz + off + 1, BlockTypes.AIR);
                    }
                } else {
                    for (int y = AY + 1; y <= AY + 6; y++) {
                        fill(es, cx + 60, y, cz + off, cx + 60, y, cz + off + 1, BlockTypes.AIR);
                    }
                }
            }

            // --- Spawn markers: 4 positions at cx±42,cz and cx,cz±42 ---
            int[][] shieldSpawns = {{cx - 42, cz}, {cx + 42, cz}, {cx, cz - 42}, {cx, cz + 42}};
            for (int[] sp : shieldSpawns) {
                fill(es, sp[0] - 1, AY, sp[1] - 1, sp[0] + 1, AY, sp[1] + 1, BlockTypes.IRON_BLOCK);
                block(es, sp[0], AY + 1, sp[1], BlockTypes.BEACON);
            }

            // --- Under-platform: IRON_BLOCK and STONE hanging stalactites ---
            int[][] underPos = {
                {cx - 40, cz - 40}, {cx + 40, cz - 40}, {cx - 40, cz + 40}, {cx + 40, cz + 40},
                {cx, cz - 50}, {cx, cz + 50}, {cx - 50, cz}, {cx + 50, cz}
            };
            for (int[] up : underPos) {
                int stalHeight = 55 + (int)(Math.abs(up[0] + up[1]) % 4);
                column(es, up[0], up[1], stalHeight, 59,
                        (up[0] % 2 == 0) ? BlockTypes.IRON_BLOCK : BlockTypes.STONE);
            }
        }
    }
    // =========================================================================
    // VOLCANO FFA — cx=500, cz=0  (150×150 FFA platform)
    // Theme: Lava / Nether / Basalt — volcanic multi-player battle FFA
    // =========================================================================
    private void buildVolcanoFFA() {
        final int cx = 500, cz = 0, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 150×150, 4 blocks thick (y=60..63) ---
            fill(es, cx - 75, 60, cz - 75, cx + 75, 63, cz + 75, BlockTypes.NETHERRACK);

            // --- Floor 140×140: NETHER_BRICKS overall ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.NETHER_BRICKS);

            // --- RED_NETHER_BRICKS 8-wide border ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz - 63, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 70, AY, cz + 63, cx + 70, AY, cz + 70, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx - 70, AY, cz - 70, cx - 63, AY, cz + 70, BlockTypes.RED_NETHER_BRICKS);
            fill(es, cx + 63, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.RED_NETHER_BRICKS);

            // --- MAGMA_BLOCK diagonal river channels from 4 corners toward centre (4-wide) ---
            // NW diagonal: from (cx-70,cz-70) toward centre
            for (int d = 0; d <= 60; d++) {
                fill(es, cx - 70 + d, AY, cz - 70 + d,
                         cx - 70 + d + 3, AY, cz - 70 + d + 3, BlockTypes.MAGMA_BLOCK);
            }
            // NE diagonal
            for (int d = 0; d <= 60; d++) {
                fill(es, cx + 70 - d - 3, AY, cz - 70 + d,
                         cx + 70 - d, AY, cz - 70 + d + 3, BlockTypes.MAGMA_BLOCK);
            }
            // SW diagonal
            for (int d = 0; d <= 60; d++) {
                fill(es, cx - 70 + d, AY, cz + 70 - d - 3,
                         cx - 70 + d + 3, AY, cz + 70 - d, BlockTypes.MAGMA_BLOCK);
            }
            // SE diagonal
            for (int d = 0; d <= 60; d++) {
                fill(es, cx + 70 - d - 3, AY, cz + 70 - d - 3,
                         cx + 70 - d, AY, cz + 70 - d, BlockTypes.MAGMA_BLOCK);
            }

            // --- Centre caldera: 13×13 MAGMA_BLOCK raised platform at y=AY+1 ---
            fill(es, cx - 6, AY + 1, cz - 6, cx + 6, AY + 1, cz + 6, BlockTypes.MAGMA_BLOCK);
            fill(es, cx - 5, AY + 1, cz - 5, cx + 5, AY + 1, cz + 5, BlockTypes.NETHERRACK);
            // GLOWSTONE ring at rim
            for (int dx = -6; dx <= 6; dx++) {
                block(es, cx + dx, AY + 1, cz - 6, BlockTypes.GLOWSTONE);
                block(es, cx + dx, AY + 1, cz + 6, BlockTypes.GLOWSTONE);
            }
            for (int dz = -5; dz <= 5; dz++) {
                block(es, cx - 6, AY + 1, cz + dz, BlockTypes.GLOWSTONE);
                block(es, cx + 6, AY + 1, cz + dz, BlockTypes.GLOWSTONE);
            }
            // NETHER_BRICK_WALL around rim
            for (int dx = -7; dx <= 7; dx++) {
                block(es, cx + dx, AY + 2, cz - 7, BlockTypes.NETHER_BRICK_WALL);
                block(es, cx + dx, AY + 2, cz + 7, BlockTypes.NETHER_BRICK_WALL);
            }
            for (int dz = -6; dz <= 6; dz++) {
                block(es, cx - 7, AY + 2, cz + dz, BlockTypes.NETHER_BRICK_WALL);
                block(es, cx + 7, AY + 2, cz + dz, BlockTypes.NETHER_BRICK_WALL);
            }
            // 3×3 GLOWSTONE centre at y=AY+2
            fill(es, cx - 1, AY + 2, cz - 1, cx + 1, AY + 2, cz + 1, BlockTypes.GLOWSTONE);

            // --- 4 elevated basalt platforms (15×15, y=AY+2) at cx±35, cz±35 ---
            int[][] basaltPlatPos = {{cx - 35, cz - 35}, {cx + 35, cz - 35},
                                     {cx - 35, cz + 35}, {cx + 35, cz + 35}};
            for (int[] bp : basaltPlatPos) {
                int bx = bp[0], bz = bp[1];
                fill(es, bx - 7, AY + 2, bz - 7, bx + 7, AY + 2, bz + 7, BlockTypes.POLISHED_BASALT);
                // BASALT border
                for (int dx = -7; dx <= 7; dx++) {
                    block(es, bx + dx, AY + 2, bz - 7, BlockTypes.BASALT);
                    block(es, bx + dx, AY + 2, bz + 7, BlockTypes.BASALT);
                }
                for (int dz = -6; dz <= 6; dz++) {
                    block(es, bx - 7, AY + 2, bz + dz, BlockTypes.BASALT);
                    block(es, bx + 7, AY + 2, bz + dz, BlockTypes.BASALT);
                }
                // SMOOTH_BASALT corners
                block(es, bx - 7, AY + 2, bz - 7, BlockTypes.SMOOTH_BASALT);
                block(es, bx + 7, AY + 2, bz - 7, BlockTypes.SMOOTH_BASALT);
                block(es, bx - 7, AY + 2, bz + 7, BlockTypes.SMOOTH_BASALT);
                block(es, bx + 7, AY + 2, bz + 7, BlockTypes.SMOOTH_BASALT);
                // GLOWSTONE embedded flush
                block(es, bx,     AY + 2, bz,     BlockTypes.GLOWSTONE);
                block(es, bx - 3, AY + 2, bz - 3, BlockTypes.GLOWSTONE);
                block(es, bx + 3, AY + 2, bz - 3, BlockTypes.GLOWSTONE);
                block(es, bx - 3, AY + 2, bz + 3, BlockTypes.GLOWSTONE);
                block(es, bx + 3, AY + 2, bz + 3, BlockTypes.GLOWSTONE);
            }

            // --- GLOWSTONE dense grid every 6 blocks in floor ---
            for (int x = cx - 66; x <= cx + 66; x += 6) {
                for (int z = cz - 66; z <= cz + 66; z += 6) {
                    block(es, x, AY, z, BlockTypes.GLOWSTONE);
                }
            }

            // ==================== WALLS (y=65..88, 24 tall) ====================
            for (int y = AY + 1; y <= AY + 24; y++) {
                fill(es, cx - 70, y, cz - 70, cx + 70, y, cz - 70, BlockTypes.NETHER_BRICKS);
                fill(es, cx - 70, y, cz + 70, cx + 70, y, cz + 70, BlockTypes.NETHER_BRICKS);
                fill(es, cx - 70, y, cz - 70, cx - 70, y, cz + 70, BlockTypes.NETHER_BRICKS);
                fill(es, cx + 70, y, cz - 70, cx + 70, y, cz + 70, BlockTypes.NETHER_BRICKS);
                // RED_NETHER_BRICKS vertical stripes every 10 blocks
                for (int wx = cx - 60; wx <= cx + 60; wx += 10) {
                    block(es, wx, y, cz - 70, BlockTypes.RED_NETHER_BRICKS);
                    block(es, wx, y, cz + 70, BlockTypes.RED_NETHER_BRICKS);
                }
                for (int wz = cz - 60; wz <= cz + 60; wz += 10) {
                    block(es, cx - 70, y, wz, BlockTypes.RED_NETHER_BRICKS);
                    block(es, cx + 70, y, wz, BlockTypes.RED_NETHER_BRICKS);
                }
            }

            // --- 4 lava-gate entrances (11-wide × 16-tall) at cardinal faces ---
            for (int y = AY + 1; y <= AY + 16; y++) {
                fill(es, cx - 5, y, cz - 70, cx + 5, y, cz - 70, BlockTypes.AIR);
                fill(es, cx - 5, y, cz + 70, cx + 5, y, cz + 70, BlockTypes.AIR);
                fill(es, cx - 70, y, cz - 5, cx - 70, y, cz + 5, BlockTypes.AIR);
                fill(es, cx + 70, y, cz - 5, cx + 70, y, cz + 5, BlockTypes.AIR);
            }
            // NETHER_BRICK_WALL arch frame and MAGMA_BLOCK pillar flanking each gate
            int[][] gateEntrances = {{cx, cz - 70}, {cx, cz + 70}};
            for (int[] ge : gateEntrances) {
                column(es, ge[0] - 6, ge[1], AY + 1, AY + 17, BlockTypes.MAGMA_BLOCK);
                column(es, ge[0] + 6, ge[1], AY + 1, AY + 17, BlockTypes.MAGMA_BLOCK);
                fill(es, ge[0] - 5, AY + 17, ge[1], ge[0] + 5, AY + 17, ge[1],
                        BlockTypes.NETHER_BRICK_WALL);
            }
            column(es, cx - 70, cz - 6, AY + 1, AY + 17, BlockTypes.MAGMA_BLOCK);
            column(es, cx - 70, cz + 6, AY + 1, AY + 17, BlockTypes.MAGMA_BLOCK);
            column(es, cx + 70, cz - 6, AY + 1, AY + 17, BlockTypes.MAGMA_BLOCK);
            column(es, cx + 70, cz + 6, AY + 1, AY + 17, BlockTypes.MAGMA_BLOCK);

            // --- Wall top: RED_NETHER_BRICKS merlons 3-tall + GLOWSTONE tips every 3 ---
            for (int wx = cx - 69; wx <= cx + 69; wx += 3) {
                fill(es, wx, AY + 25, cz - 70, wx, AY + 27, cz - 70, BlockTypes.RED_NETHER_BRICKS);
                fill(es, wx, AY + 25, cz + 70, wx, AY + 27, cz + 70, BlockTypes.RED_NETHER_BRICKS);
                block(es, wx, AY + 28, cz - 70, BlockTypes.GLOWSTONE);
                block(es, wx, AY + 28, cz + 70, BlockTypes.GLOWSTONE);
            }
            for (int wz = cz - 69; wz <= cz + 69; wz += 3) {
                fill(es, cx - 70, AY + 25, wz, cx - 70, AY + 27, wz, BlockTypes.RED_NETHER_BRICKS);
                fill(es, cx + 70, AY + 25, wz, cx + 70, AY + 27, wz, BlockTypes.RED_NETHER_BRICKS);
                block(es, cx - 70, AY + 28, wz, BlockTypes.GLOWSTONE);
                block(es, cx + 70, AY + 28, wz, BlockTypes.GLOWSTONE);
            }

            // --- 8 massive volcanic bastions (13×13, 32 tall) evenly around perimeter ---
            int[][] bastionAngles = new int[8][2];
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                // Place bastions on the wall perimeter
                int bDist = 70;
                int bx = cx + (int) Math.round(bDist * Math.cos(angle));
                int bz = cz + (int) Math.round(bDist * Math.sin(angle));
                bastionAngles[i][0] = bx;
                bastionAngles[i][1] = bz;
                // NETHER_BRICKS outer shell
                fillHollow(es, bx - 6, AY, bz - 6, bx + 6, AY + 32, bz + 6,
                        BlockTypes.NETHER_BRICKS, BlockTypes.AIR);
                // NETHERRACK core fill centre column
                fill(es, bx - 2, AY + 1, bz - 2, bx + 2, AY + 31, bz + 2, BlockTypes.NETHERRACK);
                // Arrow slits every 4 blocks
                for (int wy = AY + 4; wy <= AY + 28; wy += 4) {
                    block(es, bx - 6, wy, bz,     BlockTypes.AIR);
                    block(es, bx + 6, wy, bz,     BlockTypes.AIR);
                    block(es, bx,     wy, bz - 6, BlockTypes.AIR);
                    block(es, bx,     wy, bz + 6, BlockTypes.AIR);
                }
                // GLOWSTONE crown ring at AY+28
                for (int dx = -6; dx <= 6; dx++) {
                    block(es, bx + dx, AY + 28, bz - 6, BlockTypes.GLOWSTONE);
                    block(es, bx + dx, AY + 28, bz + 6, BlockTypes.GLOWSTONE);
                }
                for (int dz = -5; dz <= 5; dz++) {
                    block(es, bx - 6, AY + 28, bz + dz, BlockTypes.GLOWSTONE);
                    block(es, bx + 6, AY + 28, bz + dz, BlockTypes.GLOWSTONE);
                }
                // MAGMA_BLOCK cap at top 3×3
                fill(es, bx - 1, AY + 33, bz - 1, bx + 1, AY + 33, bz + 1, BlockTypes.MAGMA_BLOCK);
            }

            // ==================== INTERIOR OBSTACLES ====================

            // --- 12 NETHERRACK/BASALT rock spires (3×3 base, 12-22 tall) ---
            int[] spireX = {cx-50, cx+50, cx-50, cx+50, cx-30, cx+30, cx-30, cx+30,
                             cx-55, cx+55, cx-10, cx+10};
            int[] spireZ = {cz-50, cz-50, cz+50, cz+50, cz-15, cz-15, cz+15, cz+15,
                             cz,    cz,    cz-55, cz+55};
            int[] spireH = {18, 22, 15, 20, 14, 16, 12, 18, 14, 16, 20, 12};
            for (int i = 0; i < 12; i++) {
                int sx = spireX[i], sz = spireZ[i], sh = spireH[i];
                // Taper: full 3×3 for 2/3 height, then 1×1 for remainder
                int fullH = (sh * 2) / 3;
                fill(es, sx - 1, AY + 1, sz - 1, sx + 1, AY + fullH, sz + 1,
                        (i % 2 == 0) ? BlockTypes.NETHERRACK : BlockTypes.BASALT);
                column(es, sx, sz, AY + fullH + 1, AY + sh,
                        (i % 2 == 0) ? BlockTypes.NETHERRACK : BlockTypes.BASALT);
                // SHROOMLIGHT on top
                block(es, sx, AY + sh + 1, sz, BlockTypes.SHROOMLIGHT);
            }

            // --- 4 elevated POLISHED_BASALT arena pillars (5×5 base, 15 tall) at r=40 ---
            double[] pillarAngsVFA = {Math.PI/4, 3*Math.PI/4, 5*Math.PI/4, 7*Math.PI/4};
            for (double ang : pillarAngsVFA) {
                int px = cx + (int) Math.round(40 * Math.cos(ang));
                int pz = cz + (int) Math.round(40 * Math.sin(ang));
                // Base pillar 5×5
                fill(es, px - 2, AY + 1, pz - 2, px + 2, AY + 15, pz + 2, BlockTypes.POLISHED_BASALT);
                // 7×7 platform at top y=AY+16
                fill(es, px - 3, AY + 16, pz - 3, px + 3, AY + 16, pz + 3, BlockTypes.POLISHED_BASALT);
                // IRON_BARS railing around platform
                for (int dx = -3; dx <= 3; dx++) {
                    block(es, px + dx, AY + 17, pz - 3, BlockTypes.IRON_BARS);
                    block(es, px + dx, AY + 17, pz + 3, BlockTypes.IRON_BARS);
                }
                for (int dz = -2; dz <= 2; dz++) {
                    block(es, px - 3, AY + 17, pz + dz, BlockTypes.IRON_BARS);
                    block(es, px + 3, AY + 17, pz + dz, BlockTypes.IRON_BARS);
                }
                // GLOWSTONE lighting on platform
                block(es, px, AY + 16, pz, BlockTypes.GLOWSTONE);
                block(es, px - 2, AY + 16, pz - 2, BlockTypes.GLOWSTONE);
                block(es, px + 2, AY + 16, pz + 2, BlockTypes.GLOWSTONE);
            }

            // --- Low NETHER_BRICKS cover walls (2-tall, 15-long) × 8 ---
            int[] coverWallX = {cx-25, cx+25, cx-25, cx+25, cx-45, cx+45, cx, cx};
            int[] coverWallZ = {cz-5,  cz-5,  cz+5,  cz+5,  cz,    cz,  cz-45, cz+45};
            boolean[] coverNS = {false, false, false, false, true, true, false, false};
            for (int i = 0; i < 8; i++) {
                int cwx = coverWallX[i], cwz = coverWallZ[i];
                if (coverNS[i]) {
                    fill(es, cwx, AY + 1, cwz - 7, cwx, AY + 2, cwz + 7, BlockTypes.NETHER_BRICKS);
                } else {
                    fill(es, cwx - 7, AY + 1, cwz, cwx + 7, AY + 2, cwz, BlockTypes.NETHER_BRICKS);
                }
            }

            // --- 4 SOUL_SAND craters (7×7, 1 below AY) at r=50 diagonal ---
            int[][] craterPos = {{cx - 35, cz - 35}, {cx + 35, cz - 35},
                                  {cx - 35, cz + 35}, {cx + 35, cz + 35}};
            for (int[] crp : craterPos) {
                fill(es, crp[0] - 3, AY - 1, crp[1] - 3, crp[0] + 3, AY - 1, crp[1] + 3,
                        BlockTypes.SOUL_SAND);
                fill(es, crp[0] - 3, AY, crp[1] - 3, crp[0] + 3, AY, crp[1] + 3, BlockTypes.AIR);
            }

            // --- SOUL_LANTERN × 16 at ground level posts ---
            for (int i = 0; i < 16; i++) {
                double ang = i * Math.PI / 8;
                int lx = cx + (int) Math.round(35 * Math.cos(ang));
                int lz = cz + (int) Math.round(35 * Math.sin(ang));
                column(es, lx, lz, AY + 1, AY + 2, BlockTypes.NETHER_BRICKS);
                block(es, lx, AY + 3, lz, BlockTypes.SOUL_LANTERN);
            }

            // --- 8 spawn points at r=55 every 45° ---
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int sx = cx + (int) Math.round(55 * Math.cos(ang));
                int sz = cz + (int) Math.round(55 * Math.sin(ang));
                fill(es, sx - 1, AY, sz - 1, sx + 1, AY, sz + 1, BlockTypes.RED_NETHER_BRICKS);
                block(es, sx, AY + 1, sz, BlockTypes.GLOWSTONE);
            }

            // --- Under-platform: NETHERRACK + MAGMA_BLOCK stalactites ---
            for (int i = 0; i < 20; i++) {
                int ux = cx - 60 + (i * 11 + 5) % 121;
                int uz = cz - 60 + (i * 17 + 3) % 121;
                int stalH = 55 + (i % 5);
                column(es, ux, uz, stalH, 59,
                        (i % 3 == 0) ? BlockTypes.MAGMA_BLOCK : BlockTypes.NETHERRACK);
            }
        }
    }

    // =========================================================================
    // ICE PALACE FFA — cx=-500, cz=0  (150×150 FFA platform)
    // Theme: Calcite / Dripstone / Cold — frozen palace FFA
    // =========================================================================
    private void buildIcePalaceFFA() {
        final int cx = -500, cz = 0, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 150×150 ---
            fill(es, cx - 75, 60, cz - 75, cx + 75, 63, cz + 75, BlockTypes.STONE);

            // --- Floor 140×140: CALCITE overall ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.CALCITE);

            // --- SMOOTH_STONE border 8-wide ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz - 63, BlockTypes.SMOOTH_STONE);
            fill(es, cx - 70, AY, cz + 63, cx + 70, AY, cz + 70, BlockTypes.SMOOTH_STONE);
            fill(es, cx - 70, AY, cz - 70, cx - 63, AY, cz + 70, BlockTypes.SMOOTH_STONE);
            fill(es, cx + 63, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.SMOOTH_STONE);

            // --- DRIPSTONE_BLOCK snowflake: 8 arms of 5-long at r=55 (45° intervals) ---
            for (int arm = 0; arm < 8; arm++) {
                double ang = arm * Math.PI / 4;
                for (int len = 1; len <= 5; len++) {
                    int fx = cx + (int) Math.round(len * Math.cos(ang));
                    int fz = cz + (int) Math.round(len * Math.sin(ang));
                    // Offset by r=50 to place arms at r=50-55 range
                    int ox = cx + (int) Math.round((50 + len) * Math.cos(ang));
                    int oz = cz + (int) Math.round((50 + len) * Math.sin(ang));
                    block(es, ox, AY, oz, BlockTypes.DRIPSTONE_BLOCK);
                }
            }

            // --- 4 raised ice shelf sections (20×20, y=AY+1) at quadrant cx±35, cz±35 ---
            int[][] iceShelfPos = {{cx - 35, cz - 35}, {cx + 35, cz - 35},
                                    {cx - 35, cz + 35}, {cx + 35, cz + 35}};
            for (int[] ip : iceShelfPos) {
                int ix = ip[0], iz = ip[1];
                fill(es, ix - 10, AY + 1, iz - 10, ix + 10, AY + 1, iz + 10, BlockTypes.CALCITE);
                // SMOOTH_STONE border
                for (int dx = -10; dx <= 10; dx++) {
                    block(es, ix + dx, AY + 1, iz - 10, BlockTypes.SMOOTH_STONE);
                    block(es, ix + dx, AY + 1, iz + 10, BlockTypes.SMOOTH_STONE);
                }
                for (int dz = -9; dz <= 9; dz++) {
                    block(es, ix - 10, AY + 1, iz + dz, BlockTypes.SMOOTH_STONE);
                    block(es, ix + 10, AY + 1, iz + dz, BlockTypes.SMOOTH_STONE);
                }
                // DIORITE polished standins
                fill(es, ix - 4, AY + 1, iz - 4, ix + 4, AY + 1, iz + 4, BlockTypes.DIORITE);
            }

            // --- Centre dias (11×11 POLISHED_ANDESITE, y=AY+1) ---
            fill(es, cx - 5, AY + 1, cz - 5, cx + 5, AY + 1, cz + 5, BlockTypes.POLISHED_ANDESITE);
            // DIORITE ring at edge of dias
            for (int dx = -5; dx <= 5; dx++) {
                block(es, cx + dx, AY + 1, cz - 5, BlockTypes.DIORITE);
                block(es, cx + dx, AY + 1, cz + 5, BlockTypes.DIORITE);
            }
            for (int dz = -4; dz <= 4; dz++) {
                block(es, cx - 5, AY + 1, cz + dz, BlockTypes.DIORITE);
                block(es, cx + 5, AY + 1, cz + dz, BlockTypes.DIORITE);
            }
            // SMOOTH_STONE 3×3 centre
            fill(es, cx - 1, AY + 1, cz - 1, cx + 1, AY + 1, cz + 1, BlockTypes.SMOOTH_STONE);
            // CALCITE snowflake arms (3-long, 8 arms)
            for (int arm = 0; arm < 8; arm++) {
                double ang = arm * Math.PI / 4;
                for (int len = 2; len <= 4; len++) {
                    int ax = cx + (int) Math.round(len * Math.cos(ang));
                    int az = cz + (int) Math.round(len * Math.sin(ang));
                    block(es, ax, AY + 1, az, BlockTypes.CALCITE);
                }
            }

            // --- SEA_LANTERN grid in floor every 8 blocks ---
            for (int x = cx - 64; x <= cx + 64; x += 8) {
                for (int z = cz - 64; z <= cz + 64; z += 8) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // ==================== WALLS (y=65..86, 22 tall) ====================
            for (int y = AY + 1; y <= AY + 22; y++) {
                // CALCITE outer
                fill(es, cx - 70, y, cz - 70, cx + 70, y, cz - 70, BlockTypes.CALCITE);
                fill(es, cx - 70, y, cz + 70, cx + 70, y, cz + 70, BlockTypes.CALCITE);
                fill(es, cx - 70, y, cz - 70, cx - 70, y, cz + 70, BlockTypes.CALCITE);
                fill(es, cx + 70, y, cz - 70, cx + 70, y, cz + 70, BlockTypes.CALCITE);
                // STONE inner
                fill(es, cx - 69, y, cz - 69, cx + 69, y, cz - 69, BlockTypes.STONE);
                fill(es, cx - 69, y, cz + 69, cx + 69, y, cz + 69, BlockTypes.STONE);
                fill(es, cx - 69, y, cz - 69, cx - 69, y, cz + 69, BlockTypes.STONE);
                fill(es, cx + 69, y, cz - 69, cx + 69, y, cz + 69, BlockTypes.STONE);
            }

            // --- TINTED_GLASS windows every 6 blocks (5-wide × 14-tall) ---
            for (int wx = cx - 60; wx <= cx + 60; wx += 12) {
                fill(es, wx - 2, AY + 3, cz - 70, wx + 2, AY + 16, cz - 70, BlockTypes.TINTED_GLASS);
                fill(es, wx - 2, AY + 3, cz + 70, wx + 2, AY + 16, cz + 70, BlockTypes.TINTED_GLASS);
            }
            for (int wz = cz - 60; wz <= cz + 60; wz += 12) {
                fill(es, cx - 70, AY + 3, wz - 2, cx - 70, AY + 16, wz + 2, BlockTypes.TINTED_GLASS);
                fill(es, cx + 70, AY + 3, wz - 2, cx + 70, AY + 16, wz + 2, BlockTypes.TINTED_GLASS);
            }

            // --- GLOWSTONE embedded in wall at mid-height every 4 blocks ---
            for (int wx = cx - 68; wx <= cx + 68; wx += 4) {
                block(es, wx, AY + 11, cz - 70, BlockTypes.GLOWSTONE);
                block(es, wx, AY + 11, cz + 70, BlockTypes.GLOWSTONE);
            }
            for (int wz = cz - 68; wz <= cz + 68; wz += 4) {
                block(es, cx - 70, AY + 11, wz, BlockTypes.GLOWSTONE);
                block(es, cx + 70, AY + 11, wz, BlockTypes.GLOWSTONE);
            }

            // --- 4 grand arched entrances (11-wide × 16-tall) at cardinals ---
            for (int y = AY + 1; y <= AY + 16; y++) {
                // North entrance
                fill(es, cx - 5, y, cz - 70, cx + 5, y, cz - 70, BlockTypes.AIR);
                fill(es, cx - 5, y, cz - 69, cx + 5, y, cz - 69, BlockTypes.AIR);
                // South entrance
                fill(es, cx - 5, y, cz + 70, cx + 5, y, cz + 70, BlockTypes.AIR);
                fill(es, cx - 5, y, cz + 69, cx + 5, y, cz + 69, BlockTypes.AIR);
                // West entrance
                fill(es, cx - 70, y, cz - 5, cx - 70, y, cz + 5, BlockTypes.AIR);
                fill(es, cx - 69, y, cz - 5, cx - 69, y, cz + 5, BlockTypes.AIR);
                // East entrance
                fill(es, cx + 70, y, cz - 5, cx + 70, y, cz + 5, BlockTypes.AIR);
                fill(es, cx + 69, y, cz - 5, cx + 69, y, cz + 5, BlockTypes.AIR);
            }
            // CALCITE arch frames, DRIPSTONE_BLOCK keystones
            block(es, cx - 6, AY + 17, cz - 70, BlockTypes.CALCITE);
            block(es, cx + 6, AY + 17, cz - 70, BlockTypes.CALCITE);
            block(es, cx,     AY + 17, cz - 70, BlockTypes.DRIPSTONE_BLOCK);
            block(es, cx - 6, AY + 17, cz + 70, BlockTypes.CALCITE);
            block(es, cx + 6, AY + 17, cz + 70, BlockTypes.CALCITE);
            block(es, cx,     AY + 17, cz + 70, BlockTypes.DRIPSTONE_BLOCK);

            // --- 8 pointed ice spire towers (7×7, 26 tall) ---
            int[][] spirePos = {
                {cx - 70, cz - 70}, {cx + 70, cz - 70}, {cx - 70, cz + 70}, {cx + 70, cz + 70},
                {cx,      cz - 70}, {cx,      cz + 70}, {cx - 70, cz     }, {cx + 70, cz     }
            };
            for (int[] sp : spirePos) {
                int sx = sp[0], sz = sp[1];
                // CALCITE shaft 7×7 narrowing
                fill(es, sx - 3, AY + 1, sz - 3, sx + 3, AY + 16, sz + 3, BlockTypes.CALCITE);
                // Narrow to 5×5 at 17
                fill(es, sx - 2, AY + 17, sz - 2, sx + 2, AY + 21, sz + 2, BlockTypes.CALCITE);
                // Narrow to 3×3 at 22
                fill(es, sx - 1, AY + 22, sz - 1, sx + 1, AY + 26, sz + 1, BlockTypes.CALCITE);
                // DRIPSTONE_BLOCK accent rings every 6 blocks
                for (int dx = -3; dx <= 3; dx++) {
                    block(es, sx + dx, AY + 6,  sz - 3, BlockTypes.DRIPSTONE_BLOCK);
                    block(es, sx + dx, AY + 6,  sz + 3, BlockTypes.DRIPSTONE_BLOCK);
                    block(es, sx + dx, AY + 12, sz - 3, BlockTypes.DRIPSTONE_BLOCK);
                    block(es, sx + dx, AY + 12, sz + 3, BlockTypes.DRIPSTONE_BLOCK);
                }
                // POINTED_DRIPSTONE crown: 5×5 at top, 3×3 at +2, 1×1 at +4
                fill(es, sx - 2, AY + 27, sz - 2, sx + 2, AY + 27, sz + 2, BlockTypes.POINTED_DRIPSTONE);
                fill(es, sx - 1, AY + 28, sz - 1, sx + 1, AY + 28, sz + 1, BlockTypes.POINTED_DRIPSTONE);
                block(es, sx, AY + 29, sz, BlockTypes.POINTED_DRIPSTONE);
                // SEA_LANTERN embedded at top cap
                block(es, sx, AY + 26, sz, BlockTypes.SEA_LANTERN);
            }

            // --- Wall top: CALCITE merlons alternating with DRIPSTONE_BLOCK crenels ---
            for (int wx = cx - 69; wx <= cx + 69; wx += 2) {
                fill(es, wx, AY + 23, cz - 70, wx, AY + 25, cz - 70, BlockTypes.CALCITE);
                fill(es, wx, AY + 23, cz + 70, wx, AY + 25, cz + 70, BlockTypes.CALCITE);
                block(es, wx + 1, AY + 23, cz - 70, BlockTypes.DRIPSTONE_BLOCK);
                block(es, wx + 1, AY + 23, cz + 70, BlockTypes.DRIPSTONE_BLOCK);
            }
            for (int wz = cz - 69; wz <= cz + 69; wz += 2) {
                fill(es, cx - 70, AY + 23, wz, cx - 70, AY + 25, wz, BlockTypes.CALCITE);
                fill(es, cx + 70, AY + 23, wz, cx + 70, AY + 25, wz, BlockTypes.CALCITE);
                block(es, cx - 70, AY + 23, wz + 1, BlockTypes.DRIPSTONE_BLOCK);
                block(es, cx + 70, AY + 23, wz + 1, BlockTypes.DRIPSTONE_BLOCK);
            }

            // ==================== INTERIOR OBSTACLES ====================

            // --- 8 CALCITE crystal pillars (2×2, 14 tall) tapering at r=28 ---
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int px = cx + (int) Math.round(28 * Math.cos(ang));
                int pz = cz + (int) Math.round(28 * Math.sin(ang));
                fill(es, px, AY + 1, pz, px + 1, AY + 10, pz + 1, BlockTypes.CALCITE);
                column(es, px, pz, AY + 11, AY + 14, BlockTypes.CALCITE);
                // POINTED_DRIPSTONE × 4 sides at top
                block(es, px,     AY + 15, pz - 1, BlockTypes.POINTED_DRIPSTONE);
                block(es, px,     AY + 15, pz + 2, BlockTypes.POINTED_DRIPSTONE);
                block(es, px - 1, AY + 15, pz,     BlockTypes.POINTED_DRIPSTONE);
                block(es, px + 2, AY + 15, pz,     BlockTypes.POINTED_DRIPSTONE);
            }

            // --- 4 DRIPSTONE_BLOCK ice formations (3×3×8 tall) at r=40 diagonal ---
            int[][] iceForms = {{cx - 28, cz - 28}, {cx + 28, cz - 28},
                                 {cx - 28, cz + 28}, {cx + 28, cz + 28}};
            for (int[] iform : iceForms) {
                fill(es, iform[0] - 1, AY + 1, iform[1] - 1,
                         iform[0] + 1, AY + 8,  iform[1] + 1, BlockTypes.DRIPSTONE_BLOCK);
            }

            // --- POINTED_DRIPSTONE scatter (30 positions, height 1-3) ---
            for (int i = 0; i < 30; i++) {
                int px = cx - 60 + (i * 13 + 7) % 121;
                int pz = cz - 60 + (i * 19 + 5) % 121;
                int ph = 1 + (i % 3);
                for (int dy = 1; dy <= ph; dy++) {
                    block(es, px, AY + dy, pz, BlockTypes.POINTED_DRIPSTONE);
                }
            }

            // --- 4 low CALCITE cover walls (2-tall, 12-long) at cardinal r=18 ---
            fill(es, cx - 6, AY + 1, cz - 18, cx + 6, AY + 2, cz - 18, BlockTypes.CALCITE);
            fill(es, cx - 6, AY + 1, cz + 18, cx + 6, AY + 2, cz + 18, BlockTypes.CALCITE);
            fill(es, cx - 18, AY + 1, cz - 6, cx - 18, AY + 2, cz + 6, BlockTypes.CALCITE);
            fill(es, cx + 18, AY + 1, cz - 6, cx + 18, AY + 2, cz + 6, BlockTypes.CALCITE);

            // --- SMOOTH_STONE channels (1-wide geometric paths) ---
            for (int x = cx - 60; x <= cx + 60; x += 15) {
                fill(es, x, AY, cz - 60, x, AY, cz + 60, BlockTypes.SMOOTH_STONE);
            }
            for (int z = cz - 60; z <= cz + 60; z += 15) {
                fill(es, cx - 60, AY, z, cx + 60, AY, z, BlockTypes.SMOOTH_STONE);
            }

            // --- 8 SEA_LANTERN posts (3-tall DRIPSTONE_BLOCK + SEA_LANTERN) at r=45 ---
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int lx = cx + (int) Math.round(45 * Math.cos(ang));
                int lz = cz + (int) Math.round(45 * Math.sin(ang));
                column(es, lx, lz, AY + 1, AY + 3, BlockTypes.DRIPSTONE_BLOCK);
                block(es, lx, AY + 4, lz, BlockTypes.SEA_LANTERN);
            }

            // --- 8 spawn points at r=55 every 45° ---
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int sx = cx + (int) Math.round(55 * Math.cos(ang));
                int sz = cz + (int) Math.round(55 * Math.sin(ang));
                fill(es, sx - 1, AY, sz - 1, sx + 1, AY, sz + 1, BlockTypes.CALCITE);
                block(es, sx, AY + 1, sz, BlockTypes.SEA_LANTERN);
            }

            // --- Under-platform: POINTED_DRIPSTONE + CALCITE giant stalactites ---
            int[][] stalGroups = {{cx - 40, cz - 40}, {cx + 40, cz - 40},
                                   {cx - 40, cz + 40}, {cx + 40, cz + 40},
                                   {cx,      cz - 50}, {cx,      cz + 50},
                                   {cx - 50, cz     }, {cx + 50, cz     }};
            for (int[] sg : stalGroups) {
                column(es, sg[0], sg[1], 54, 59, BlockTypes.CALCITE);
                block(es, sg[0], 53, sg[1], BlockTypes.POINTED_DRIPSTONE);
                // Small cluster around it
                block(es, sg[0] + 1, 56, sg[1], BlockTypes.CALCITE);
                block(es, sg[0] - 1, 55, sg[1], BlockTypes.CALCITE);
                block(es, sg[0], 55, sg[1] + 1, BlockTypes.POINTED_DRIPSTONE);
            }
        }
    }

    // =========================================================================
    // ANCIENT RUINS FFA — cx=0, cz=500  (150×150 FFA platform)
    // Theme: Mossy Stone / Crumbling / Overgrown — ancient battlefield FFA
    // =========================================================================
    private void buildAncientRuinsFFA() {
        final int cx = 0, cz = 500, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 150×150 ---
            fill(es, cx - 75, 60, cz - 75, cx + 75, 63, cz + 75, BlockTypes.STONE);

            // --- Floor 140×140: start with MOSS_BLOCK then scatter patches ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.MOSS_BLOCK);

            // Scatter patches: step 7, alternating types
            com.sk89q.worldedit.world.block.BlockType[] patchTypes = {BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.COARSE_DIRT, BlockTypes.CRACKED_STONE_BRICKS};
            int patchIdx = 0;
            for (int x = cx - 70; x <= cx + 70; x += 7) {
                for (int z = cz - 70; z <= cz + 70; z += 7) {
                    com.sk89q.worldedit.world.block.BlockType pType = patchTypes[patchIdx % 3];
                    fill(es, x, AY, z, Math.min(x + 3, cx + 70), AY, Math.min(z + 3, cz + 70), pType);
                    patchIdx++;
                }
            }

            // --- Ancient stone road: CHISELED_STONE_BRICKS 5-wide N/S through centre ---
            fill(es, cx - 2, AY, cz - 68, cx + 2, AY, cz + 68, BlockTypes.CHISELED_STONE_BRICKS);

            // --- GLOWSTONE embedded in road every 8 blocks ---
            for (int z = cz - 64; z <= cz + 64; z += 8) {
                block(es, cx, AY, z, BlockTypes.GLOWSTONE);
            }

            // --- Sunken ruin pits (3×5×2 deep, CAVE_AIR, STONE_BRICKS rim) ---
            int[][] pitPos = {
                {cx - 40, cz - 30}, {cx + 40, cz - 30},
                {cx - 40, cz + 30}, {cx + 40, cz + 30},
                {cx - 55, cz     }, {cx + 55, cz     }
            };
            for (int[] pit : pitPos) {
                // STONE_BRICKS rim at AY
                fill(es, pit[0] - 2, AY, pit[1] - 3, pit[0] + 2, AY, pit[1] + 3,
                        BlockTypes.STONE_BRICKS);
                // Hollow interior 2 deep
                fill(es, pit[0] - 1, AY - 1, pit[1] - 2, pit[0] + 1, AY - 2, pit[1] + 2,
                        BlockTypes.CAVE_AIR);
            }

            // --- Raised ruin podiums (11×11, y=AY+1) at cx±35, cz±35 ---
            int[][] podiums = {{cx - 35, cz - 35}, {cx + 35, cz - 35},
                                {cx - 35, cz + 35}, {cx + 35, cz + 35}};
            for (int[] pd : podiums) {
                fill(es, pd[0] - 5, AY + 1, pd[1] - 5, pd[0] + 5, AY + 1, pd[1] + 5,
                        BlockTypes.STONE_BRICKS);
                fill(es, pd[0] - 4, AY + 1, pd[1] - 4, pd[0] + 4, AY + 1, pd[1] + 4,
                        BlockTypes.MOSSY_STONE_BRICKS);
            }

            // --- SEA_LANTERN grid (dim, sparse for ruins feel) every 12 blocks ---
            for (int x = cx - 60; x <= cx + 60; x += 12) {
                for (int z = cz - 60; z <= cz + 60; z += 12) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // ==================== WALLS (y=65..78, 14 tall, ruined ~75% coverage) ====================
            // Build walls with gaps every 12 blocks (skip 3-wide sections)
            for (int y = AY + 1; y <= AY + 14; y++) {
                for (int wx = cx - 70; wx <= cx + 70; wx++) {
                    boolean skip = ((wx - (cx - 70)) % 12 >= 9);
                    if (!skip) {
                        block(es, wx, y, cz - 70, BlockTypes.MOSSY_STONE_BRICKS);
                        block(es, wx, y, cz + 70, BlockTypes.MOSSY_STONE_BRICKS);
                    }
                    // Inner CRACKED_STONE_BRICKS
                    if (!skip) {
                        block(es, wx, y, cz - 69, BlockTypes.CRACKED_STONE_BRICKS);
                        block(es, wx, y, cz + 69, BlockTypes.CRACKED_STONE_BRICKS);
                    }
                }
                for (int wz = cz - 70; wz <= cz + 70; wz++) {
                    boolean skip = ((wz - (cz - 70)) % 12 >= 9);
                    if (!skip) {
                        block(es, cx - 70, y, wz, BlockTypes.MOSSY_STONE_BRICKS);
                        block(es, cx + 70, y, wz, BlockTypes.MOSSY_STONE_BRICKS);
                    }
                    if (!skip) {
                        block(es, cx - 69, y, wz, BlockTypes.CRACKED_STONE_BRICKS);
                        block(es, cx + 69, y, wz, BlockTypes.CRACKED_STONE_BRICKS);
                    }
                }
            }

            // Random wall height variation: some sections taller
            for (int wx = cx - 70; wx <= cx + 70; wx += 6) {
                int extra = ((wx - cx) % 3 == 0) ? 4 : ((wx - cx + 1) % 3 == 0) ? 0 : 8;
                for (int ey = AY + 15; ey <= AY + 14 + extra; ey++) {
                    block(es, wx, ey, cz - 70, BlockTypes.MOSSY_STONE_BRICKS);
                    block(es, wx, ey, cz + 70, BlockTypes.MOSSY_STONE_BRICKS);
                }
            }

            // MOSS_CARPET covering wall tops
            for (int wx = cx - 70; wx <= cx + 70; wx += 3) {
                block(es, wx, AY + 15, cz - 70, BlockTypes.MOSS_CARPET);
                block(es, wx, AY + 15, cz + 70, BlockTypes.MOSS_CARPET);
            }
            for (int wz = cz - 70; wz <= cz + 70; wz += 3) {
                block(es, cx - 70, AY + 15, wz, BlockTypes.MOSS_CARPET);
                block(es, cx + 70, AY + 15, wz, BlockTypes.MOSS_CARPET);
            }
            // HANGING_ROOTS below some wall tops
            for (int wx = cx - 68; wx <= cx + 68; wx += 5) {
                block(es, wx, AY + 14, cz - 70, BlockTypes.HANGING_ROOTS);
                block(es, wx, AY + 14, cz + 70, BlockTypes.HANGING_ROOTS);
            }

            // --- 8 ruined tower stubs (9×9, 20 tall) ---
            int[][] towerPos = {
                {cx - 70, cz - 70}, {cx + 70, cz - 70}, {cx - 70, cz + 70}, {cx + 70, cz + 70},
                {cx,      cz - 70}, {cx,      cz + 70}, {cx - 70, cz     }, {cx + 70, cz     }
            };
            for (int[] tp : towerPos) {
                int tx = tp[0], tz = tp[1];
                fillHollow(es, tx - 4, AY + 1, tz - 4, tx + 4, AY + 20, tz + 4,
                        BlockTypes.CRACKED_STONE_BRICKS, BlockTypes.AIR);
            }

            // --- 4 collapsed gateways (13-wide, completely open) ---
            for (int y = AY + 1; y <= AY + 14; y++) {
                fill(es, cx - 6, y, cz - 70, cx + 6, y, cz - 70, BlockTypes.AIR);
                fill(es, cx - 6, y, cz + 70, cx + 6, y, cz + 70, BlockTypes.AIR);
                fill(es, cx - 70, y, cz - 6, cx - 70, y, cz + 6, BlockTypes.AIR);
                fill(es, cx + 70, y, cz - 6, cx + 70, y, cz + 6, BlockTypes.AIR);
            }

            // ==================== INTERIOR OBSTACLES ====================

            // --- 6 large ruin columns (5×5, 18-22 tall) ---
            int[][] ruinColPos = {
                {cx - 45, cz - 30}, {cx + 45, cz - 30},
                {cx - 45, cz + 30}, {cx + 45, cz + 30},
                {cx - 20, cz - 55}, {cx + 20, cz + 55}
            };
            int[] ruinColH = {20, 18, 22, 19, 20, 18};
            for (int i = 0; i < 6; i++) {
                int rx = ruinColPos[i][0], rz = ruinColPos[i][1], rh = ruinColH[i];
                // Mix of CRACKED_STONE_BRICKS and MOSSY_STONE_BRICKS
                for (int y = AY + 1; y <= AY + rh; y++) {
                    fill(es, rx - 2, y, rz - 2, rx + 2, y, rz + 2,
                            (y % 3 == 0) ? BlockTypes.MOSSY_STONE_BRICKS : BlockTypes.CRACKED_STONE_BRICKS);
                }
                // SHROOMLIGHT on top
                block(es, rx, AY + rh + 1, rz, BlockTypes.SHROOMLIGHT);
            }

            // --- 3 collapsed arch structures (2 pillars + broken top) ---
            int[][] archPairs = {{cx - 20, cz - 30}, {cx + 20, cz + 40}, {cx - 20, cz + 50}};
            for (int[] ap : archPairs) {
                int ax = ap[0], az = ap[1];
                // Left pillar
                fill(es, ax - 3, AY + 1, az, ax - 3, AY + 14, az, BlockTypes.STONE_BRICKS);
                // Right pillar
                fill(es, ax + 3, AY + 1, az, ax + 3, AY + 14, az, BlockTypes.STONE_BRICKS);
                // Broken arch top (missing centre)
                fill(es, ax - 3, AY + 14, az, ax - 1, AY + 14, az, BlockTypes.STONE_BRICKS);
                fill(es, ax + 1, AY + 14, az, ax + 3, AY + 14, az, BlockTypes.STONE_BRICKS);
                // Fallen debris
                block(es, ax, AY, az, BlockTypes.CRACKED_STONE_BRICKS);
                block(es, ax, AY + 1, az, BlockTypes.CRACKED_STONE_BRICKS);
            }

            // --- 10 medium wall segments (MOSSY_STONE_BRICKS 3×12×4 tall) ---
            int[] wallSegX = {cx-55, cx+55, cx-35, cx+35, cx-15, cx+15, cx-50, cx+50, cx-30, cx+30};
            int[] wallSegZ = {cz+15, cz-15, cz+55, cz-55, cz+40, cz-40, cz-40, cz+40, cz-20, cz+20};
            boolean[] wallSegNS = {true, true, false, false, true, true, false, false, true, false};
            for (int i = 0; i < 10; i++) {
                int wx = wallSegX[i], wz = wallSegZ[i];
                if (wallSegNS[i]) {
                    fill(es, wx, AY + 1, wz - 6, wx + 2, AY + 4, wz + 6,
                            BlockTypes.MOSSY_STONE_BRICKS);
                } else {
                    fill(es, wx - 6, AY + 1, wz, wx + 6, AY + 4, wz + 2,
                            BlockTypes.MOSSY_STONE_BRICKS);
                }
            }

            // --- 4 overgrown garden areas (9×9, MOSS_BLOCK floor with plants) ---
            int[][] gardenPos = {{cx - 50, cz - 50}, {cx + 50, cz - 50},
                                  {cx - 50, cz + 50}, {cx + 50, cz + 50}};
            for (int[] gp : gardenPos) {
                fill(es, gp[0] - 4, AY, gp[1] - 4, gp[0] + 4, AY, gp[1] + 4, BlockTypes.MOSS_BLOCK);
                // Azalea/flowering azalea bushes
                block(es, gp[0] - 2, AY + 1, gp[1] - 2, BlockTypes.AZALEA);
                block(es, gp[0] + 2, AY + 1, gp[1] - 2, BlockTypes.FLOWERING_AZALEA);
                block(es, gp[0] - 2, AY + 1, gp[1] + 2, BlockTypes.FLOWERING_AZALEA);
                block(es, gp[0] + 2, AY + 1, gp[1] + 2, BlockTypes.AZALEA);
                // Fern/grass
                block(es, gp[0], AY + 1, gp[1],     BlockTypes.FERN);
                block(es, gp[0] - 1, AY + 1, gp[1] + 1, BlockTypes.SHORT_GRASS);
                block(es, gp[0] + 1, AY + 1, gp[1] - 1, BlockTypes.FERN);
                block(es, gp[0] - 3, AY + 1, gp[1],     BlockTypes.SHORT_GRASS);
            }

            // --- Ancient well at centre ---
            fill(es, cx - 2, AY + 1, cz - 2, cx + 2, AY + 1, cz + 2, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx - 1, AY + 1, cz - 1, cx + 1, AY + 1, cz + 1, BlockTypes.AIR);
            fill(es, cx - 1, AY, cz - 1, cx + 1, AY, cz + 1, BlockTypes.WATER);

            // --- 6 oak trees growing through ruins ---
            int[] treeX = {cx - 30, cx + 30, cx - 55, cx + 55, cx - 15, cx + 15};
            int[] treeZ = {cz + 10, cz - 10, cz + 45, cz - 45, cz + 60, cz - 60};
            for (int i = 0; i < 6; i++) {
                int tx = treeX[i], tz = treeZ[i];
                column(es, tx, tz, AY + 1, AY + 8, BlockTypes.OAK_LOG);
                // Wide canopy: 5×5 at AY+9, 7×7 at AY+8
                fill(es, tx - 3, AY + 8, tz - 3, tx + 3, AY + 9, tz + 3, BlockTypes.OAK_LEAVES);
                fill(es, tx - 2, AY + 10, tz - 2, tx + 2, AY + 10, tz + 2, BlockTypes.OAK_LEAVES);
                block(es, tx, AY + 11, tz, BlockTypes.OAK_LEAVES);
            }

            // --- LANTERN posts × 12 scattered ---
            int[] lantX = {cx-48, cx+48, cx-35, cx+35, cx-20, cx+20, cx-60, cx+60, cx-10, cx+10, cx-45, cx+45};
            int[] lantZ = {cz+35, cz-35, cz-48, cz+48, cz-58, cz+58, cz+20, cz-20, cz+45, cz-45, cz+10, cz-10};
            for (int i = 0; i < 12; i++) {
                column(es, lantX[i], lantZ[i], AY + 1, AY + 2, BlockTypes.OAK_LOG);
                block(es, lantX[i], AY + 3, lantZ[i], BlockTypes.LANTERN);
            }

            // --- SOUL_LANTERN × 8 near ruins ---
            int[] soulLX = {cx-40, cx+40, cx-55, cx+55, cx-25, cx+25, cx-38, cx+38};
            int[] soulLZ = {cz-40, cz+40, cz+25, cz-25, cz+55, cz-55, cz+50, cz-50};
            for (int i = 0; i < 8; i++) {
                block(es, soulLX[i], AY + 1, soulLZ[i], BlockTypes.SOUL_LANTERN);
            }

            // --- 8 spawn points at r=55 every 45° ---
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int sx = cx + (int) Math.round(55 * Math.cos(ang));
                int sz = cz + (int) Math.round(55 * Math.sin(ang));
                fill(es, sx - 1, AY, sz - 1, sx + 1, AY, sz + 1, BlockTypes.MOSS_BLOCK);
                block(es, sx, AY + 1, sz, BlockTypes.SEA_LANTERN);
            }

            // --- Under-platform: COBBLESTONE + MOSSY_COBBLESTONE + STONE ---
            int[][] underGroups = {
                {cx - 40, cz - 40}, {cx + 40, cz - 40},
                {cx - 40, cz + 40}, {cx + 40, cz + 40},
                {cx - 55, cz     }, {cx + 55, cz     },
                {cx,      cz - 55}, {cx,      cz + 55}
            };
            com.sk89q.worldedit.world.block.BlockType[] underTypes = {
                BlockTypes.COBBLESTONE, BlockTypes.MOSSY_COBBLESTONE, BlockTypes.STONE,
                BlockTypes.COBBLESTONE, BlockTypes.MOSSY_COBBLESTONE, BlockTypes.STONE,
                BlockTypes.COBBLESTONE, BlockTypes.MOSSY_COBBLESTONE
            };
            for (int i = 0; i < 8; i++) {
                column(es, underGroups[i][0], underGroups[i][1], 55, 59, underTypes[i]);
                block(es, underGroups[i][0] + 1, 56, underGroups[i][1], underTypes[(i + 1) % 8]);
                block(es, underGroups[i][0] - 1, 57, underGroups[i][1], underTypes[(i + 2) % 8]);
            }
        }
    }
    // =========================================================================
    // SKY TEMPLE FFA — cx=0, cz=-500  (150×150 FFA platform)
    // Theme: Quartz / Floating / Beacon — aerial temple FFA with elevation changes
    // =========================================================================
    private void buildSkyTempleFFA() {
        final int cx = 0, cz = -500, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 150×150 ---
            fill(es, cx - 75, 60, cz - 75, cx + 75, 63, cz + 75, BlockTypes.SMOOTH_QUARTZ);

            // --- Floor 140×140: POLISHED_DEEPSLATE overall ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.POLISHED_DEEPSLATE);

            // --- QUARTZ_BRICKS border 8-wide ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz - 63, BlockTypes.QUARTZ_BRICKS);
            fill(es, cx - 70, AY, cz + 63, cx + 70, AY, cz + 70, BlockTypes.QUARTZ_BRICKS);
            fill(es, cx - 70, AY, cz - 70, cx - 63, AY, cz + 70, BlockTypes.QUARTZ_BRICKS);
            fill(es, cx + 63, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.QUARTZ_BRICKS);

            // --- CHISELED_QUARTZ_BLOCK compass rose arms (N/S and E/W, length 55, 3-wide) ---
            fill(es, cx - 1, AY, cz - 57, cx + 1, AY, cz - 1, BlockTypes.CHISELED_QUARTZ_BLOCK);
            fill(es, cx - 1, AY, cz + 1,  cx + 1, AY, cz + 57, BlockTypes.CHISELED_QUARTZ_BLOCK);
            fill(es, cx - 57, AY, cz - 1, cx - 1, AY, cz + 1,  BlockTypes.CHISELED_QUARTZ_BLOCK);
            fill(es, cx + 1,  AY, cz - 1, cx + 57, AY, cz + 1, BlockTypes.CHISELED_QUARTZ_BLOCK);

            // --- QUARTZ_PILLAR ring at r=45 (every 5 blocks) ---
            for (int i = 0; i < 360; i += 10) {
                double ang = Math.toRadians(i);
                int px = cx + (int) Math.round(45 * Math.cos(ang));
                int pz = cz + (int) Math.round(45 * Math.sin(ang));
                block(es, px, AY, pz, BlockTypes.QUARTZ_PILLAR);
            }

            // --- SEA_LANTERN dense grid every 6 blocks ---
            for (int x = cx - 66; x <= cx + 66; x += 6) {
                for (int z = cz - 66; z <= cz + 66; z += 6) {
                    block(es, x, AY, z, BlockTypes.SEA_LANTERN);
                }
            }

            // ==================== FLOATING PLATFORMS ====================

            // --- Centre platform: 21×21 POLISHED_DEEPSLATE at y=AY+6 ---
            fill(es, cx - 10, AY + 6, cz - 10, cx + 10, AY + 6, cz + 10, BlockTypes.POLISHED_DEEPSLATE);
            // GLOWSTONE embedded in centre platform
            block(es, cx,     AY + 6, cz,     BlockTypes.GLOWSTONE);
            block(es, cx - 5, AY + 6, cz - 5, BlockTypes.GLOWSTONE);
            block(es, cx + 5, AY + 6, cz - 5, BlockTypes.GLOWSTONE);
            block(es, cx - 5, AY + 6, cz + 5, BlockTypes.GLOWSTONE);
            block(es, cx + 5, AY + 6, cz + 5, BlockTypes.GLOWSTONE);
            // Underside: SEA_LANTERN pillars from floor to platform underside
            for (int i = 0; i < 4; i++) {
                double ang = i * Math.PI / 2;
                int px = cx + (int) Math.round(7 * Math.cos(ang));
                int pz = cz + (int) Math.round(7 * Math.sin(ang));
                fill(es, px, AY + 1, pz, px, AY + 5, pz, BlockTypes.SEA_LANTERN);
            }
            // CHAIN network from centre platform underside
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int cx2 = cx + (int) Math.round(4 * Math.cos(ang));
                int cz2 = cz + (int) Math.round(4 * Math.sin(ang));
                for (int y = AY + 1; y <= AY + 5; y++) {
                    block(es, cx2, y, cz2, BlockTypes.CHAIN);
                }
                // GLOWSTONE at end of chain
                block(es, cx2, AY + 1, cz2, BlockTypes.GLOWSTONE);
            }

            // --- Bridges from all 4 sides to centre platform (3-wide, y=AY+3..AY+5) ---
            // North bridge
            fill(es, cx - 1, AY + 3, cz - 30, cx + 1, AY + 5, cz - 11, BlockTypes.QUARTZ_BRICKS);
            // South bridge
            fill(es, cx - 1, AY + 3, cz + 11, cx + 1, AY + 5, cz + 30, BlockTypes.QUARTZ_BRICKS);
            // West bridge
            fill(es, cx - 30, AY + 3, cz - 1, cx - 11, AY + 5, cz + 1, BlockTypes.QUARTZ_BRICKS);
            // East bridge
            fill(es, cx + 11, AY + 3, cz - 1, cx + 30, AY + 5, cz + 1, BlockTypes.QUARTZ_BRICKS);

            // --- 4 mid-level platforms (11×11, y=AY+4) at cx±38, cz±38 ---
            int[][] midPlatPos = {{cx - 38, cz - 38}, {cx + 38, cz - 38},
                                   {cx - 38, cz + 38}, {cx + 38, cz + 38}};
            for (int[] mp : midPlatPos) {
                int mx = mp[0], mz = mp[1];
                fill(es, mx - 5, AY + 4, mz - 5, mx + 5, AY + 4, mz + 5, BlockTypes.QUARTZ_BRICKS);
                // IRON_BARS railing
                for (int dx = -5; dx <= 5; dx++) {
                    block(es, mx + dx, AY + 5, mz - 5, BlockTypes.IRON_BARS);
                    block(es, mx + dx, AY + 5, mz + 5, BlockTypes.IRON_BARS);
                }
                for (int dz = -4; dz <= 4; dz++) {
                    block(es, mx - 5, AY + 5, mz + dz, BlockTypes.IRON_BARS);
                    block(es, mx + 5, AY + 5, mz + dz, BlockTypes.IRON_BARS);
                }
                // CHAIN hangers from underside
                block(es, mx,     AY + 3, mz,     BlockTypes.CHAIN);
                block(es, mx,     AY + 2, mz,     BlockTypes.CHAIN);
                block(es, mx + 2, AY + 3, mz + 2, BlockTypes.CHAIN);
                block(es, mx - 2, AY + 3, mz - 2, BlockTypes.CHAIN);
                // GLOWSTONE embedded
                block(es, mx, AY + 4, mz, BlockTypes.GLOWSTONE);
                // Spawn markers: 2 per mid platform
                block(es, mx - 2, AY + 4, mz,     BlockTypes.POLISHED_DEEPSLATE);
                block(es, mx + 2, AY + 4, mz,     BlockTypes.POLISHED_DEEPSLATE);
                block(es, mx - 2, AY + 5, mz,     BlockTypes.SEA_LANTERN);
                block(es, mx + 2, AY + 5, mz,     BlockTypes.SEA_LANTERN);
            }

            // --- 4 high platforms (7×7, y=AY+10) at cx±52, cz±52 ---
            int[][] highPlatPos = {{cx - 52, cz - 52}, {cx + 52, cz - 52},
                                    {cx - 52, cz + 52}, {cx + 52, cz + 52}};
            for (int[] hp : highPlatPos) {
                int hx = hp[0], hz = hp[1];
                fill(es, hx - 3, AY + 10, hz - 3, hx + 3, AY + 10, hz + 3,
                        BlockTypes.POLISHED_DEEPSLATE);
                block(es, hx, AY + 11, hz, BlockTypes.SEA_LANTERN);
                block(es, hx, AY + 10, hz, BlockTypes.GLOWSTONE);
            }

            // --- 8 QUARTZ_BLOCK cloud-cushion platforms (5×5, y=AY+3) between walls and centre ---
            double[] cloudAngles = {Math.PI/8, 3*Math.PI/8, 5*Math.PI/8, 7*Math.PI/8,
                                     9*Math.PI/8, 11*Math.PI/8, 13*Math.PI/8, 15*Math.PI/8};
            for (double ang : cloudAngles) {
                int clx = cx + (int) Math.round(55 * Math.cos(ang));
                int clz = cz + (int) Math.round(55 * Math.sin(ang));
                fill(es, clx - 2, AY + 3, clz - 2, clx + 2, AY + 3, clz + 2, BlockTypes.QUARTZ_BLOCK);
                block(es, clx, AY + 4, clz, BlockTypes.GLOWSTONE);
            }

            // ==================== WALLS (y=65..90, 26 tall) ====================
            for (int y = AY + 1; y <= AY + 26; y++) {
                // SMOOTH_QUARTZ outer
                fill(es, cx - 70, y, cz - 70, cx + 70, y, cz - 70, BlockTypes.SMOOTH_QUARTZ);
                fill(es, cx - 70, y, cz + 70, cx + 70, y, cz + 70, BlockTypes.SMOOTH_QUARTZ);
                fill(es, cx - 70, y, cz - 70, cx - 70, y, cz + 70, BlockTypes.SMOOTH_QUARTZ);
                fill(es, cx + 70, y, cz - 70, cx + 70, y, cz + 70, BlockTypes.SMOOTH_QUARTZ);
                // QUARTZ_BRICKS inner
                fill(es, cx - 69, y, cz - 69, cx + 69, y, cz - 69, BlockTypes.QUARTZ_BRICKS);
                fill(es, cx - 69, y, cz + 69, cx + 69, y, cz + 69, BlockTypes.QUARTZ_BRICKS);
                fill(es, cx - 69, y, cz - 69, cx - 69, y, cz + 69, BlockTypes.QUARTZ_BRICKS);
                fill(es, cx + 69, y, cz - 69, cx + 69, y, cz + 69, BlockTypes.QUARTZ_BRICKS);
            }

            // --- TINTED_GLASS windows (7-wide × 20-tall) every 15 blocks ---
            for (int wx = cx - 60; wx <= cx + 60; wx += 15) {
                fill(es, wx - 3, AY + 2, cz - 70, wx + 3, AY + 21, cz - 70, BlockTypes.TINTED_GLASS);
                fill(es, wx - 3, AY + 2, cz + 70, wx + 3, AY + 21, cz + 70, BlockTypes.TINTED_GLASS);
            }
            for (int wz = cz - 60; wz <= cz + 60; wz += 15) {
                fill(es, cx - 70, AY + 2, wz - 3, cx - 70, AY + 21, wz + 3, BlockTypes.TINTED_GLASS);
                fill(es, cx + 70, AY + 2, wz - 3, cx + 70, AY + 21, wz + 3, BlockTypes.TINTED_GLASS);
            }

            // --- 4 grand cloud-gate entrances (13-wide × 20-tall) ---
            for (int y = AY + 1; y <= AY + 20; y++) {
                fill(es, cx - 6, y, cz - 70, cx + 6, y, cz - 70, BlockTypes.AIR);
                fill(es, cx - 6, y, cz + 70, cx + 6, y, cz + 70, BlockTypes.AIR);
                fill(es, cx - 70, y, cz - 6, cx - 70, y, cz + 6, BlockTypes.AIR);
                fill(es, cx + 70, y, cz - 6, cx + 70, y, cz + 6, BlockTypes.AIR);
            }

            // --- 8 tall spire towers (5×5, 35 tall) ---
            double[] spireAngles = {0, Math.PI/4, Math.PI/2, 3*Math.PI/4,
                                     Math.PI, 5*Math.PI/4, 3*Math.PI/2, 7*Math.PI/4};
            for (double ang : spireAngles) {
                int sx = cx + (int) Math.round(70 * Math.cos(ang));
                int sz = cz + (int) Math.round(70 * Math.sin(ang));
                // 5×5 shaft up to AY+28
                fill(es, sx - 2, AY + 1, sz - 2, sx + 2, AY + 28, sz + 2, BlockTypes.QUARTZ_PILLAR);
                // 3×3 from AY+29 to AY+33
                fill(es, sx - 1, AY + 29, sz - 1, sx + 1, AY + 33, sz + 1, BlockTypes.QUARTZ_PILLAR);
                // 1×1 to AY+38
                column(es, sx, sz, AY + 34, AY + 38, BlockTypes.QUARTZ_PILLAR);
                // CHISELED_QUARTZ_BLOCK accent band every 6 blocks
                for (int dy = 6; dy <= 30; dy += 6) {
                    for (int dx = -2; dx <= 2; dx++) {
                        block(es, sx + dx, AY + dy, sz - 2, BlockTypes.CHISELED_QUARTZ_BLOCK);
                        block(es, sx + dx, AY + dy, sz + 2, BlockTypes.CHISELED_QUARTZ_BLOCK);
                    }
                    for (int dz = -1; dz <= 1; dz++) {
                        block(es, sx - 2, AY + dy, sz + dz, BlockTypes.CHISELED_QUARTZ_BLOCK);
                        block(es, sx + 2, AY + dy, sz + dz, BlockTypes.CHISELED_QUARTZ_BLOCK);
                    }
                }
                // SEA_LANTERN at tip
                block(es, sx, AY + 39, sz, BlockTypes.SEA_LANTERN);
                // CHAIN 4 long descending from tip with LANTERN on end
                for (int dc = 1; dc <= 4; dc++) {
                    block(es, sx, AY + 39 - dc, sz + 1, BlockTypes.CHAIN);
                }
                block(es, sx, AY + 35, sz + 1, BlockTypes.LANTERN);
            }

            // --- Wall top: QUARTZ_BRICKS merlons with CHISELED_QUARTZ_BLOCK between ---
            for (int wx = cx - 69; wx <= cx + 69; wx += 2) {
                fill(es, wx, AY + 27, cz - 70, wx, AY + 30, cz - 70, BlockTypes.QUARTZ_BRICKS);
                fill(es, wx, AY + 27, cz + 70, wx, AY + 30, cz + 70, BlockTypes.QUARTZ_BRICKS);
                block(es, wx + 1, AY + 27, cz - 70, BlockTypes.CHISELED_QUARTZ_BLOCK);
                block(es, wx + 1, AY + 27, cz + 70, BlockTypes.CHISELED_QUARTZ_BLOCK);
            }
            for (int wz = cz - 69; wz <= cz + 69; wz += 2) {
                fill(es, cx - 70, AY + 27, wz, cx - 70, AY + 30, wz, BlockTypes.QUARTZ_BRICKS);
                fill(es, cx + 70, AY + 27, wz, cx + 70, AY + 30, wz, BlockTypes.QUARTZ_BRICKS);
                block(es, cx - 70, AY + 27, wz + 1, BlockTypes.CHISELED_QUARTZ_BLOCK);
                block(es, cx + 70, AY + 27, wz + 1, BlockTypes.CHISELED_QUARTZ_BLOCK);
            }

            // ==================== EXTRA INTERIOR STRUCTURES ====================

            // --- 4 BEACON pillars at r=55 cardinal ---
            int[][] beaconPillars = {{cx - 55, cz}, {cx + 55, cz}, {cx, cz - 55}, {cx, cz + 55}};
            for (int[] bp : beaconPillars) {
                int bx = bp[0], bz = bp[1];
                // IRON_BLOCK 5×5 base
                fill(es, bx - 2, AY, bz - 2, bx + 2, AY, bz + 2, BlockTypes.IRON_BLOCK);
                // QUARTZ_PILLAR column 12 tall
                fill(es, bx - 1, AY + 1, bz - 1, bx + 1, AY + 12, bz + 1, BlockTypes.QUARTZ_PILLAR);
                // BEACON at top
                block(es, bx, AY + 13, bz, BlockTypes.BEACON);
            }

            // --- 20 SEA_LANTERN posts around perimeter ---
            for (int i = 0; i < 20; i++) {
                double ang = i * Math.PI * 2 / 20;
                int lx = cx + (int) Math.round(66 * Math.cos(ang));
                int lz = cz + (int) Math.round(66 * Math.sin(ang));
                column(es, lx, lz, AY + 1, AY + 3, BlockTypes.QUARTZ_BRICKS);
                block(es, lx, AY + 4, lz, BlockTypes.SEA_LANTERN);
            }

            // --- Under-platform: SMOOTH_QUARTZ + QUARTZ_PILLAR hanging stalactites ---
            int[] stalX = {cx - 40, cx + 40, cx - 40, cx + 40, cx, cx, cx - 55, cx + 55};
            int[] stalZ = {cz - 40, cz - 40, cz + 40, cz + 40, cz - 55, cz + 55, cz, cz};
            for (int i = 0; i < 8; i++) {
                column(es, stalX[i], stalZ[i], 56, 59,
                        (i % 2 == 0) ? BlockTypes.SMOOTH_QUARTZ : BlockTypes.QUARTZ_PILLAR);
                block(es, stalX[i], 55, stalZ[i],
                        (i % 2 == 0) ? BlockTypes.QUARTZ_PILLAR : BlockTypes.SMOOTH_QUARTZ);
            }
        }
    }

    // =========================================================================
    // DARK FOREST FFA — cx=400, cz=400  (150×150 FFA platform)
    // Theme: Dark Oak / Mycelium / Shroomlight — haunted dark forest FFA
    // =========================================================================
    private void buildDarkForestFFA() {
        final int cx = 400, cz = 400, AY = 64;
        try (EditSession es = newSession()) {

            // --- Base platform 150×150 ---
            fill(es, cx - 75, 60, cz - 75, cx + 75, 63, cz + 75, BlockTypes.DEEPSLATE);

            // --- Floor 140×140: MYCELIUM overall ---
            fill(es, cx - 70, AY, cz - 70, cx + 70, AY, cz + 70, BlockTypes.MYCELIUM);

            // --- PODZOL patches (20 positions, 7×7) ---
            int[] podX = {cx-50, cx+50, cx-30, cx+30, cx-55, cx+55, cx-15, cx+15,
                           cx-42, cx+42, cx-20, cx+20, cx-60, cx+60, cx-35, cx+35,
                           cx-8,  cx+8,  cx-48, cx+48};
            int[] podZ = {cz-50, cz+50, cz+55, cz-55, cz+15, cz-15, cz+50, cz-50,
                           cz+30, cz-30, cz-40, cz+40, cz+35, cz-35, cz-20, cz+20,
                           cz-60, cz+60, cz+5,  cz-5};
            for (int i = 0; i < 20; i++) {
                fill(es, podX[i] - 3, AY, podZ[i] - 3, podX[i] + 3, AY, podZ[i] + 3,
                        BlockTypes.PODZOL);
            }

            // --- DARK_OAK_PLANKS 3-wide paths (N/S, E/W, and diagonal X) ---
            fill(es, cx - 1, AY, cz - 68, cx + 1, AY, cz + 68, BlockTypes.DARK_OAK_PLANKS);
            fill(es, cx - 68, AY, cz - 1, cx + 68, AY, cz + 1, BlockTypes.DARK_OAK_PLANKS);
            // Diagonal X paths (approximate)
            for (int d = -60; d <= 60; d++) {
                block(es, cx + d, AY, cz + d, BlockTypes.DARK_OAK_PLANKS);
                block(es, cx + d, AY, cz - d, BlockTypes.DARK_OAK_PLANKS);
            }

            // --- SHROOMLIGHT dense grid in floor every 8 blocks ---
            for (int x = cx - 64; x <= cx + 64; x += 8) {
                for (int z = cz - 64; z <= cz + 64; z += 8) {
                    block(es, x, AY, z, BlockTypes.SHROOMLIGHT);
                }
            }

            // --- 8 SOUL_SAND fog-pits (2×2 × 1 deep) ---
            int[] fogX = {cx-40, cx+40, cx-40, cx+40, cx-55, cx+55, cx-10, cx+10};
            int[] fogZ = {cz-40, cz-40, cz+40, cz+40, cz+5, cz-5,  cz+55, cz-55};
            for (int i = 0; i < 8; i++) {
                fill(es, fogX[i], AY - 1, fogZ[i], fogX[i] + 1, AY - 1, fogZ[i] + 1,
                        BlockTypes.SOUL_SAND);
                fill(es, fogX[i], AY, fogZ[i], fogX[i] + 1, AY, fogZ[i] + 1, BlockTypes.AIR);
            }

            // ==================== FOREST CANOPY CEILING (y=AY+16..AY+18) ====================
            fill(es, cx - 70, AY + 16, cz - 70, cx + 70, AY + 18, cz + 70, BlockTypes.DARK_OAK_LEAVES);
            // Leave 9-wide openings above each entrance
            fill(es, cx - 4, AY + 16, cz - 70, cx + 4, AY + 18, cz - 70, BlockTypes.AIR);
            fill(es, cx - 4, AY + 16, cz + 70, cx + 4, AY + 18, cz + 70, BlockTypes.AIR);
            fill(es, cx - 70, AY + 16, cz - 4, cx - 70, AY + 18, cz + 4, BlockTypes.AIR);
            fill(es, cx + 70, AY + 16, cz - 4, cx + 70, AY + 18, cz + 4, BlockTypes.AIR);

            // --- SHROOMLIGHT embedded in canopy every 12 blocks ---
            for (int x = cx - 60; x <= cx + 60; x += 12) {
                for (int z = cz - 60; z <= cz + 60; z += 12) {
                    block(es, x, AY + 16, z, BlockTypes.SHROOMLIGHT);
                }
            }

            // --- 24 DARK_OAK_LOG trunks piercing canopy (y=AY+1..AY+18) ---
            int[] trunkX = {cx-55, cx+55, cx-55, cx+55, cx-35, cx+35, cx-35, cx+35,
                             cx-20, cx+20, cx-20, cx+20, cx-48, cx+48, cx-10, cx+10,
                             cx-60, cx+60, cx-28, cx+28, cx-42, cx+42, cx-5, cx+5};
            int[] trunkZ = {cz-55, cz-55, cz+55, cz+55, cz-45, cz-45, cz+45, cz+45,
                             cz-58, cz-58, cz+58, cz+58, cz-25, cz+25, cz-35, cz+35,
                             cz+10, cz-10, cz+15, cz-15, cz+50, cz-50, cz+60, cz-60};
            for (int i = 0; i < 24; i++) {
                column(es, trunkX[i], trunkZ[i], AY + 1, AY + 18, BlockTypes.DARK_OAK_LOG);
            }

            // --- 40 HANGING_ROOTS from canopy (y=AY+15) ---
            for (int i = 0; i < 40; i++) {
                int hx = cx - 65 + (i * 13 + 7) % 131;
                int hz = cz - 65 + (i * 17 + 3) % 131;
                block(es, hx, AY + 15, hz, BlockTypes.HANGING_ROOTS);
            }

            // ==================== WALLS (y=AY+1..AY+16) ====================
            for (int y = AY + 1; y <= AY + 16; y++) {
                // DARK_OAK_LOG outer
                fill(es, cx - 70, y, cz - 70, cx + 70, y, cz - 70, BlockTypes.DARK_OAK_LOG);
                fill(es, cx - 70, y, cz + 70, cx + 70, y, cz + 70, BlockTypes.DARK_OAK_LOG);
                fill(es, cx - 70, y, cz - 70, cx - 70, y, cz + 70, BlockTypes.DARK_OAK_LOG);
                fill(es, cx + 70, y, cz - 70, cx + 70, y, cz + 70, BlockTypes.DARK_OAK_LOG);
                // DARK_OAK_PLANKS inner
                fill(es, cx - 69, y, cz - 69, cx + 69, y, cz - 69, BlockTypes.DARK_OAK_PLANKS);
                fill(es, cx - 69, y, cz + 69, cx + 69, y, cz + 69, BlockTypes.DARK_OAK_PLANKS);
                fill(es, cx - 69, y, cz - 69, cx - 69, y, cz + 69, BlockTypes.DARK_OAK_PLANKS);
                fill(es, cx + 69, y, cz - 69, cx + 69, y, cz + 69, BlockTypes.DARK_OAK_PLANKS);
                // DARK_OAK_LEAVES covering outside face above y=AY+8
                if (y >= AY + 8) {
                    fill(es, cx - 71, y, cz - 71, cx + 71, y, cz - 71, BlockTypes.DARK_OAK_LEAVES);
                    fill(es, cx - 71, y, cz + 71, cx + 71, y, cz + 71, BlockTypes.DARK_OAK_LEAVES);
                    fill(es, cx - 71, y, cz - 71, cx - 71, y, cz + 71, BlockTypes.DARK_OAK_LEAVES);
                    fill(es, cx + 71, y, cz - 71, cx + 71, y, cz + 71, BlockTypes.DARK_OAK_LEAVES);
                }
            }

            // --- 4 dark entrances (9-wide × 15-tall) ---
            for (int y = AY + 1; y <= AY + 15; y++) {
                fill(es, cx - 4, y, cz - 70, cx + 4, y, cz - 70, BlockTypes.AIR);
                fill(es, cx - 4, y, cz + 70, cx + 4, y, cz + 70, BlockTypes.AIR);
                fill(es, cx - 70, y, cz - 4, cx - 70, y, cz + 4, BlockTypes.AIR);
                fill(es, cx + 70, y, cz - 4, cx + 70, y, cz + 4, BlockTypes.AIR);
                // DARK_OAK_LOG archway frame on each side
                block(es, cx - 5, y, cz - 70, BlockTypes.DARK_OAK_LOG);
                block(es, cx + 5, y, cz - 70, BlockTypes.DARK_OAK_LOG);
                block(es, cx - 5, y, cz + 70, BlockTypes.DARK_OAK_LOG);
                block(es, cx + 5, y, cz + 70, BlockTypes.DARK_OAK_LOG);
                block(es, cx - 70, y, cz - 5, BlockTypes.DARK_OAK_LOG);
                block(es, cx - 70, y, cz + 5, BlockTypes.DARK_OAK_LOG);
                block(es, cx + 70, y, cz - 5, BlockTypes.DARK_OAK_LOG);
                block(es, cx + 70, y, cz + 5, BlockTypes.DARK_OAK_LOG);
            }

            // --- 8 guardian tree wall towers (9×9, 28 tall) ---
            double[] guardAngles = {0, Math.PI/4, Math.PI/2, 3*Math.PI/4,
                                     Math.PI, 5*Math.PI/4, 3*Math.PI/2, 7*Math.PI/4};
            for (int i = 0; i < 8; i++) {
                double ang = guardAngles[i];
                int tx = cx + (int) Math.round(70 * Math.cos(ang));
                int tz = cz + (int) Math.round(70 * Math.sin(ang));
                // 3×3 DARK_OAK_LOG trunk, height 28
                fill(es, tx - 1, AY + 1, tz - 1, tx + 1, AY + 28, tz + 1, BlockTypes.DARK_OAK_LOG);
                // DARK_OAK_LEAVES canopy at top 6 blocks (AY+22..AY+28)
                fill(es, tx - 4, AY + 22, tz - 4, tx + 4, AY + 28, tz + 4, BlockTypes.DARK_OAK_LEAVES);
            }

            // ==================== INTERIOR FOREST OBSTACLES ====================

            // --- 8 large DARK_OAK_LOG ancient trees (2×2 trunk, 14-18 tall, 7×7 canopy) ---
            int[] bigTreeX = {cx-45, cx+45, cx-45, cx+45, cx-20, cx+20, cx-60, cx+60};
            int[] bigTreeZ = {cz-45, cz-45, cz+45, cz+45, cz-60, cz+60, cz+20, cz-20};
            int[] bigTreeH = {16, 14, 18, 15, 16, 14, 18, 16};
            for (int i = 0; i < 8; i++) {
                int tx = bigTreeX[i], tz = bigTreeZ[i], th = bigTreeH[i];
                // 2×2 trunk
                fill(es, tx, AY + 1, tz, tx + 1, AY + th, tz + 1, BlockTypes.DARK_OAK_LOG);
                // 7×7 canopy disk at top
                fill(es, tx - 3, AY + th + 1, tz - 3, tx + 4, AY + th + 1, tz + 4,
                        BlockTypes.DARK_OAK_LEAVES);
                // Cascading leaves 3 layers
                fill(es, tx - 2, AY + th, tz - 2, tx + 3, AY + th, tz + 3,
                        BlockTypes.DARK_OAK_LEAVES);
                fill(es, tx - 1, AY + th - 1, tz - 1, tx + 2, AY + th - 1, tz + 2,
                        BlockTypes.DARK_OAK_LEAVES);
            }

            // --- 6 mossy ruins (5×5 MOSSY_STONE_BRICKS walls, 3-tall, 1-wide door) ---
            int[] ruinX = {cx-35, cx+35, cx-55, cx+55, cx-20, cx+20};
            int[] ruinZ = {cz+30, cz-30, cz-45, cz+45, cz+40, cz-40};
            for (int i = 0; i < 6; i++) {
                int rx = ruinX[i], rz = ruinZ[i];
                fillHollow(es, rx - 2, AY + 1, rz - 2, rx + 2, AY + 3, rz + 2,
                        BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
                // Door opening
                block(es, rx, AY + 1, rz - 2, BlockTypes.AIR);
                block(es, rx, AY + 2, rz - 2, BlockTypes.AIR);
            }

            // --- 4 SHROOMLIGHT mushroom clusters ---
            int[][] shroomPos = {{cx - 50, cz + 50}, {cx + 50, cz - 50},
                                   {cx - 30, cz + 60}, {cx + 30, cz - 60}};
            for (int[] sp : shroomPos) {
                int sx = sp[0], sz = sp[1];
                fill(es, sx - 2, AY, sz - 2, sx + 2, AY, sz + 2, BlockTypes.MYCELIUM);
                fill(es, sx - 1, AY + 1, sz - 1, sx + 1, AY + 3, sz + 1, BlockTypes.DARK_OAK_LOG);
                fill(es, sx - 2, AY + 4, sz - 2, sx + 2, AY + 4, sz + 2, BlockTypes.SHROOMLIGHT);
            }

            // --- Water stream: 2-wide winding N to S ---
            for (int z = cz - 65; z <= cz + 65; z++) {
                // Slight winding via offset
                int offset = (int)(3 * Math.sin((z - cz) * 0.15));
                fill(es, cx + offset - 12, AY, z, cx + offset - 11, AY, z, BlockTypes.WATER);
            }

            // --- SOUL_SAND and SOUL_SOIL patches near walls ---
            int[] soulX = {cx-60, cx+60, cx-60, cx+60, cx-35, cx+35, cx-55, cx+55};
            int[] soulZ = {cz+30, cz-30, cz-30, cz+30, cz+65, cz-65, cz+55, cz+55};
            for (int i = 0; i < 8; i++) {
                fill(es, soulX[i] - 1, AY, soulZ[i] - 1, soulX[i] + 1, AY, soulZ[i] + 1,
                        (i % 2 == 0) ? BlockTypes.SOUL_SAND : BlockTypes.SOUL_SOIL);
            }

            // --- OBSIDIAN monolith at centre ---
            fill(es, cx - 1, AY + 1, cz + 15, cx + 1, AY + 8, cz + 17, BlockTypes.OBSIDIAN);
            // CRYING_OBSIDIAN border at base and top
            for (int dx = -2; dx <= 2; dx++) {
                block(es, cx + dx, AY + 1, cz + 14, BlockTypes.CRYING_OBSIDIAN);
                block(es, cx + dx, AY + 8, cz + 18, BlockTypes.CRYING_OBSIDIAN);
            }
            for (int dz = 14; dz <= 18; dz++) {
                block(es, cx - 2, AY + 1, cz + dz, BlockTypes.CRYING_OBSIDIAN);
                block(es, cx + 2, AY + 1, cz + dz, BlockTypes.CRYING_OBSIDIAN);
            }
            // SHROOMLIGHT cap
            block(es, cx, AY + 9, cz + 16, BlockTypes.SHROOMLIGHT);

            // --- Brown/red mushroom scatter (15 positions) ---
            int[] mushX = {cx-30, cx+30, cx-50, cx+50, cx-15, cx+15, cx-40, cx+40,
                            cx-25, cx+25, cx-55, cx+55, cx-8, cx+8, cx};
            int[] mushZ = {cz+25, cz-25, cz+35, cz-35, cz+50, cz-50, cz+5, cz-5,
                            cz-35, cz+35, cz-20, cz+20, cz+45, cz-45, cz+55};
            for (int i = 0; i < 15; i++) {
                block(es, mushX[i], AY + 1, mushZ[i],
                        (i % 2 == 0) ? BlockTypes.BROWN_MUSHROOM : BlockTypes.RED_MUSHROOM);
            }

            // --- SOUL_LANTERN × 16 on posts (3-tall DARK_OAK_LOG + SOUL_LANTERN) ---
            for (int i = 0; i < 16; i++) {
                double ang = i * Math.PI / 8;
                int lx = cx + (int) Math.round(50 * Math.cos(ang));
                int lz = cz + (int) Math.round(50 * Math.sin(ang));
                column(es, lx, lz, AY + 1, AY + 3, BlockTypes.DARK_OAK_LOG);
                block(es, lx, AY + 4, lz, BlockTypes.SOUL_LANTERN);
            }

            // --- LANTERN only near entrance paths ---
            int[][] lanternPairs = {
                {cx - 6, cz - 68}, {cx + 6, cz - 68},
                {cx - 6, cz + 68}, {cx + 6, cz + 68},
                {cx - 68, cz - 6}, {cx - 68, cz + 6},
                {cx + 68, cz - 6}, {cx + 68, cz + 6}
            };
            for (int[] lp : lanternPairs) {
                column(es, lp[0], lp[1], AY + 1, AY + 2, BlockTypes.DARK_OAK_LOG);
                block(es, lp[0], AY + 3, lp[1], BlockTypes.LANTERN);
            }

            // --- 8 spawn points at r=55 every 45° ---
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                int sx = cx + (int) Math.round(55 * Math.cos(ang));
                int sz = cz + (int) Math.round(55 * Math.sin(ang));
                fill(es, sx - 1, AY, sz - 1, sx + 1, AY, sz + 1, BlockTypes.MYCELIUM);
                block(es, sx, AY + 1, sz, BlockTypes.SHROOMLIGHT);
            }

            // --- Under-platform: DEEPSLATE + DARK_OAK_LOG stalactite roots + MOSS_BLOCK ---
            int[] underX = {cx - 45, cx + 45, cx - 45, cx + 45, cx - 60, cx + 60, cx, cx};
            int[] underZ = {cz - 45, cz - 45, cz + 45, cz + 45, cz, cz, cz - 60, cz + 60};
            for (int i = 0; i < 8; i++) {
                column(es, underX[i], underZ[i], 56, 59,
                        (i % 3 == 0) ? BlockTypes.DARK_OAK_LOG
                                : (i % 3 == 1) ? BlockTypes.DEEPSLATE : BlockTypes.MOSS_BLOCK);
                // Small cluster
                block(es, underX[i] + 1, 57, underZ[i], BlockTypes.MOSS_BLOCK);
                block(es, underX[i] - 1, 57, underZ[i] + 1, BlockTypes.DEEPSLATE);
            }
        }
    }

    /*
     * =========================================================================
     * ARENA REGISTRY — All 13 arenas built by this class
     * =========================================================================
     *
     *  PRACTICE ARENAS (130×130, 4-thick platform, 2 players):
     *   1.  buildBowArena()          cx= 220, cz=-220  — Forest / Leaves / Archery
     *   2.  buildSwordArena()        cx=-220, cz=-220  — Lava / Nether / Sword duels
     *   3.  buildGappleArena()       cx= 220, cz= 0    — Plains / Terracotta / Gapple
     *   4.  buildAxeArena()          cx=-220, cz= 0    — Jungle / Wood / Axe duels
     *   5.  buildStickArena()        cx= 0,   cz= 220  — Desert / Sandstone / Stick
     *   6.  buildDebuffArena()       cx= 0,   cz=-220  — Swamp / Mossy / Debuff
     *   7.  buildPotArena()          cx= 0,   cz= 0    — Void / End / Pot duels
     *   8.  buildBuildUHCArena()     cx= 440, cz=-440  — Stone / Ore / Build UHC
     *   9.  buildCrystalArena()      cx=-440, cz=-440  — Amethyst / Crystal duels
     *  10.  buildNoDebuffArena()     cx= 440, cz= 440  — Birch / Clean / No Debuff
     *  11.  buildTridentArena()      cx= 220, cz= 220  — Ocean / Prismarine / Trident
     *  12.  buildShieldArena()       cx=-220, cz= 220  — Fortress / Iron / Shield
     *
     *  FFA ARENAS (150×150, 4-thick platform, multi-player):
     *  13.  buildVolcanoFFA()        cx= 500, cz=   0  — Nether / Basalt / Volcanic
     *  14.  buildIcePalaceFFA()      cx=-500, cz=   0  — Calcite / Dripstone / Ice
     *  15.  buildAncientRuinsFFA()   cx=   0, cz= 500  — Mossy / Crumbling / Ruins
     *  16.  buildSkyTempleFFA()      cx=   0, cz=-500  — Quartz / Floating / Sky
     *  17.  buildDarkForestFFA()     cx= 400, cz= 400  — Dark Oak / Mycelium / Forest
     *
     * =========================================================================
     */
}
