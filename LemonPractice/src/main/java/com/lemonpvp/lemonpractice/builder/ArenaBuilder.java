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
