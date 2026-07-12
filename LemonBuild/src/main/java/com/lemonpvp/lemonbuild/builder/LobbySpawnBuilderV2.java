package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import org.bukkit.World;

/**
 * The "Grand Citrus" lobby (v2) — part 2 and orchestration.
 *
 * <p>Layout (island r≈95, deck Y=64): a tiered sandstone citadel at spawn,
 * the queue cathedral N, kit forge E, market bazaar S, hall of fame W,
 * parkour spire NE, colosseum NW, lemon orchard SE, observatory SW, four
 * floating satellite islands on the far diagonals with rope bridges, plus
 * sky halos, lantern chains, balloons and a full path network.</p>
 *
 * <p>{@code /aowbuildlobby} builds this; {@code /aowbuildlobby classic}
 * rebuilds the previous lobby ({@link LobbySpawnBuilder}), which is kept
 * fully functional as the fallback the user asked for.</p>
 */
public class LobbySpawnBuilderV2 extends LobbyV2Base {

    public LobbySpawnBuilderV2(World world) {
        super(world);
    }

    @Override
    public void build() {
        try (EditSession es = WorldEdit.getInstance()
                .newEditSessionBuilder().world(weWorld()).maxBlocks(-1).build()) {

            clearSiteV2(es);

            // Terrain
            terrainIsland(es);
            terrainCliffs(es);
            satelliteIslands(es);
            undersideRoots(es);

            // Center
            citadelBase(es);
            citadelFountain(es);
            citadelPillars(es);
            citadelCrown(es);

            // Buildings on the cardinal axes
            queueCathedral(es);      // N
            kitForge(es);            // E
            marketBazaar(es);        // S
            hallOfFame(es);          // W

            // Corner landmarks
            parkourSpire(es);        // NE
            colosseum(es);           // NW
            lemonOrchard(es);        // SE
            observatory(es);         // SW

            // Connections + sky + edge
            pathNetwork(es);
            lampPosts(es);
            skyWork(es);
            lanternChains(es);
            balloons(es);
            edgeRail(es);
            waterfalls(es);

            // Final detail pass
            benchesAndPlanters(es);
            deckSpeckle(es);
            welcomeArch(es);
            mascotStatue(es);

            // "Even better" pass
            marqueeSign(es);
            citadelUpgrade(es);
            cathedralUpgrade(es);
            forgeUpgrade(es);
            bazaarUpgrade(es);
            hallUpgrade(es);
            rimBalconies(es);
            skyClouds(es);
            greenUpgrade(es);
            undersideGeode(es);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // South: the Market Bazaar — vaulted hall + colored stalls + gazebo
    // ────────────────────────────────────────────────────────────────────────

    private void marketBazaar(EditSession es) {
        int bz = CZ + 62;
        fill(es, CX - 16, Y, bz - 9, CX + 16, Y, bz + 9, "smooth_sandstone");
        // Open hall: pillars carrying a barrel-vault roof (no walls — market feel).
        for (int px = -14; px <= 14; px += 7) {
            for (int pz = -7; pz <= 7; pz += 14) {
                column(es, CX + px, bz + pz, Y + 1, Y + 6, OAK_LOG_Y);
                set(es, CX + px, Y + 7, bz + pz, "oak_fence");
            }
        }
        // Vault: three stepped arcs of terracotta.
        for (int a = 0; a < 3; a++) {
            int y = Y + 7 + a;
            int half = 16 - a * 3;
            fill(es, CX - half, y, bz - 8, CX + half, y, bz + 8,
                    a == 2 ? TERRA_O : TERRA_Y);
            fill(es, CX - half + 2, y, bz - 6, CX + half - 2, y, bz + 6, "air");
        }
        fill(es, CX - 10, Y + 9, bz - 8, CX + 10, Y + 9, bz + 8, TERRA_O);
        fill(es, CX - 8, Y + 10, bz - 8, CX + 8, Y + 10, bz + 8, "air");
        fill(es, CX - 4, Y + 10, bz - 8, CX + 4, Y + 10, bz + 8, "orange_stained_glass"); // skylight

        // Six stalls with alternating canopies.
        for (int i = 0; i < 6; i++) {
            int sx = CX - 12 + i * 5;
            boolean yellow = i % 2 == 0;
            String wool = yellow ? "yellow_wool" : "lime_wool";
            String cloth = yellow ? "yellow_carpet" : "lime_carpet";
            fill(es, sx, Y + 1, bz + 6, sx + 2, Y + 1, bz + 7, "barrel[facing=up]");
            fill(es, sx, Y + 2, bz + 6, sx + 2, Y + 2, bz + 7, cloth);
            column(es, sx, bz + 8, Y + 1, Y + 3, "oak_fence");
            column(es, sx + 2, bz + 8, Y + 1, Y + 3, "oak_fence");
            fill(es, sx, Y + 4, bz + 5, sx + 2, Y + 4, bz + 8, wool);
            set(es, sx + 1, Y + 3, bz + 7, H_LANTERN);
        }
        // Central display: the daily-reward gazebo (gold dome on quartz).
        tube(es, CX, Y + 1, bz - 3, 3.2, 4.2, 4, "quartz_pillar[axis=y]");
        fill(es, CX - 4, Y + 1, bz - 3, CX + 4, Y + 1, bz - 3, "air");  // opening
        disk(es, CX, Y + 5, bz - 3, 4.6, GOLD);
        sphere(es, CX, Y + 7, bz - 3, 2.6, GLASS_Y);
        set(es, CX, Y + 6, bz - 3, "beacon");
        set(es, CX, Y + 1, bz - 3, "chest[facing=south]");
        ring(es, CX, Y + 1, bz - 3, 4.4, 5.2, "yellow_carpet");
    }

    // ────────────────────────────────────────────────────────────────────────
    // West: the Hall of Fame — colonnade, amethyst wall, champion plinths
    // ────────────────────────────────────────────────────────────────────────

    private void hallOfFame(EditSession es) {
        int bx = CX - 62;
        fill(es, bx - 9, Y, CZ - 15, bx + 9, Y, CZ + 15, "polished_diorite");
        // Backdrop wall (west side): amethyst face framed in quartz.
        fill(es, bx - 9, Y + 1, CZ - 14, bx - 9, Y + 9, CZ + 14, "quartz_bricks");
        fill(es, bx - 9, Y + 2, CZ - 12, bx - 9, Y + 8, CZ + 12, "amethyst_block");
        for (int wz = -12; wz <= 12; wz += 4) {
            set(es, bx - 9, Y + 5, CZ + wz, "budding_amethyst");
            set(es, bx - 8, Y + 5, CZ + wz, "amethyst_cluster[facing=east]");
        }
        fill(es, bx - 9, Y + 10, CZ - 14, bx - 9, Y + 10, CZ + 14, "quartz_slab[type=bottom]");

        // Colonnade on the open (east) side.
        for (int cz = -12; cz <= 12; cz += 6) {
            column(es, bx + 8, CZ + cz, Y + 1, Y + 7, "quartz_pillar[axis=y]");
            set(es, bx + 8, Y + 8, CZ + cz, "chiseled_quartz_block");
        }
        fill(es, bx + 7, Y + 9, CZ - 13, bx + 9, Y + 9, CZ + 13, "smooth_quartz_slab[type=bottom]");

        // Five champion plinths: gold pedestal + glass case + floating crown.
        for (int i = 0; i < 5; i++) {
            int pz = CZ - 12 + i * 6;
            fill(es, bx - 4, Y + 1, pz - 1, bx - 2, Y + 1, pz + 1, "polished_blackstone");
            set(es, bx - 3, Y + 2, pz, GOLD);
            set(es, bx - 3, Y + 3, pz, "glass");
            set(es, bx - 3, Y + 4, pz, i == 2 ? "dragon_head[rotation=12]" : GLASS_Y);
            set(es, bx - 3, Y + 1, pz - 2, "quartz_stairs[facing=south]");
            set(es, bx - 3, Y + 1, pz + 2, "quartz_stairs[facing=north]");
            set(es, bx - 4, Y + 5, pz, "end_rod[facing=up]");
        }
        // Floor inlay: podium 1-2-3 in front of the middle plinths.
        fill(es, bx + 1, Y + 1, CZ - 1, bx + 3, Y + 1, CZ + 1, GOLD);
        fill(es, bx + 1, Y + 2, CZ - 1, bx + 3, Y + 2, CZ + 1, "smooth_quartz_slab[type=bottom]");
        set(es, bx + 2, Y + 1, CZ - 4, "iron_block");
        set(es, bx + 2, Y + 1, CZ + 4, "waxed_copper_block");
    }

    // ────────────────────────────────────────────────────────────────────────
    // NE: the Parkour Spire — cherry/copper landmark tower + floating pads
    // ────────────────────────────────────────────────────────────────────────

    private void parkourSpire(EditSession es) {
        int tx = CX + 48, tz = CZ - 48;
        // Trunk.
        cyl(es, tx, Y, tz, 5.4, 34, CHERRY_PLANK);
        tube(es, tx, Y, tz, 5.4, 6.2, 34, CHERRY_LOG_Y);
        // Window band spiral.
        for (int h = 2; h < 32; h += 2) {
            double a = h * 0.5;
            int wx = tx + (int) Math.round(Math.cos(a) * 6);
            int wz2 = tz + (int) Math.round(Math.sin(a) * 6);
            set(es, wx, Y + h, wz2, "pink_stained_glass");
            set(es, wx, Y + h + 1, wz2, "pink_stained_glass");
        }
        // Copper crown roof.
        disk(es, tx, Y + 34, tz, 7.5, COPPER_OX);
        cone(es, tx, Y + 35, tz, 7, 8, false, COPPER);
        set(es, tx, Y + 43, tz, "lightning_rod[facing=up]");
        // Balcony ring.
        ring(es, tx, Y + 30, tz, 6.2, 8.2, CHERRY_PLANK);
        ring(es, tx, Y + 31, tz, 7.8, 8.4, "cherry_fence");
        // Spiral of floating parkour pads winding up around the tower.
        for (int i = 0; i < 18; i++) {
            double a = i * 0.7;
            double r = 11 + (i % 3) * 2;
            int px = tx + (int) Math.round(Math.cos(a) * r);
            int pz = tz + (int) Math.round(Math.sin(a) * r);
            int py = Y + 2 + (i * 3) / 2;
            String pad = (i % 3 == 0) ? CONCRETE_Y : (i % 3 == 1) ? CONCRETE_L : "smooth_quartz";
            fill(es, px - 1, py, pz - 1, px + 1, py, pz + 1, pad);
            if (i % 4 == 0) set(es, px, py + 1, pz, LANTERN);
        }
        // Start platform + trophy at the balcony.
        fill(es, tx - 2, Y + 1, tz + 9, tx + 2, Y + 1, tz + 11, "smooth_quartz");
        set(es, tx, Y + 2, tz + 10, "yellow_banner[rotation=8]");
        set(es, tx, Y + 32, tz, GOLD);
        set(es, tx, Y + 33, tz, "end_rod[facing=up]");
    }

    // ────────────────────────────────────────────────────────────────────────
    // NW: the Colosseum — sandstone show-arena with tiered seating
    // ────────────────────────────────────────────────────────────────────────

    private void colosseum(EditSession es) {
        int ax = CX - 48, az = CZ - 48;
        // Pit floor: red sand ring pattern.
        disk(es, ax, Y, az, 14, "red_sand");
        disk(es, ax, Y, az, 6, "sand");
        ring(es, ax, Y, az, 6, 7, TERRA_O);
        // Arena wall.
        tube(es, ax, Y + 1, az, 13.2, 14.4, 3, "cut_sandstone");
        // Tiered seating (three rings, rising outward).
        for (int t = 0; t < 3; t++) {
            double inner = 14.4 + t * 2;
            tube(es, ax, Y + 1, az, inner, inner + 2, 2 + t, "smooth_sandstone");
            ring(es, ax, Y + 2 + t, az, inner, inner + 2, t % 2 == 0 ? "yellow_carpet" : "lime_carpet");
        }
        // Outer colonnade with flame bowls.
        for (int i = 0; i < 12; i++) {
            double a = i * (Math.PI / 6);
            int px = ax + (int) Math.round(Math.cos(a) * 21);
            int pz = az + (int) Math.round(Math.sin(a) * 21);
            column(es, px, pz, Y + 1, Y + 6, "sandstone_wall");
            set(es, px, Y + 7, pz, "chiseled_sandstone");
            if (i % 2 == 0) set(es, px, Y + 8, pz, "campfire[lit=true]");
            else set(es, px, Y + 8, pz, LANTERN);
        }
        // Two entry gates (E toward spawn, S).
        fill(es, ax + 13, Y + 1, az - 1, ax + 20, Y + 3, az + 1, "air");
        fill(es, ax - 1, Y + 1, az + 13, ax + 1, Y + 3, az + 20, "air");
        set(es, ax + 14, Y + 4, az, "sandstone_stairs[facing=west,half=top]");
        set(es, ax, Y + 4, az + 14, "sandstone_stairs[facing=north,half=top]");
        // Center show-piece: crossed banners.
        set(es, ax, Y + 1, az, "chiseled_sandstone");
        set(es, ax, Y + 2, az, "yellow_banner[rotation=4]");
    }

    // ────────────────────────────────────────────────────────────────────────
    // SE: the Lemon Orchard — grove, hedges, pond, beehives, picnic corner
    // ────────────────────────────────────────────────────────────────────────

    private void lemonOrchard(EditSession es) {
        int ox = CX + 48, oz = CZ + 48;
        // Ground dressing.
        scatter(es, ox, Y, oz, 20, 0.35, 301L, MOSS, GRASS, PODZOL);
        scatter(es, ox, Y + 1, oz, 19, 0.16, 302L,
                "short_grass", "dandelion", "poppy", "oxeye_daisy");
        // Hedge border (broken circle).
        for (int i = 0; i < 48; i++) {
            double a = i * (Math.PI * 2 / 48);
            if (i % 8 == 0) continue;                       // gaps as entrances
            int hx = ox + (int) Math.round(Math.cos(a) * 20);
            int hz = oz + (int) Math.round(Math.sin(a) * 20);
            set(es, hx, Y + 1, hz, LEAVES);
            if (i % 3 == 0) set(es, hx, Y + 2, hz, LEAVES_FLW);
        }
        // Lemon trees on a loose grid.
        int[][] trees = {{-10, -8}, {8, -12}, {12, 6}, {-6, 10}, {0, -2}, {-14, 2}};
        for (int i = 0; i < trees.length; i++) {
            int txx = ox + trees[i][0], tzz = oz + trees[i][1];
            int h = 4 + (i % 2);
            column(es, txx, tzz, Y + 1, Y + h, OAK_LOG_Y);
            sphere(es, txx, Y + h + 2, tzz, 3.4, LEAVES);
            scatter(es, txx, Y + h + 2, tzz, 3, 0.45, 320L + i, CONCRETE_Y, LEAVES_FLW);
            scatter(es, txx, Y + h + 1, tzz, 3, 0.3, 330L + i, CONCRETE_Y);
        }
        // Pond with lily pads.
        disk(es, ox - 2, Y, oz + 14, 5, "water");
        ring(es, ox - 2, Y, oz + 14, 5, 6, STONE);
        set(es, ox - 3, Y + 1, oz + 13, "lily_pad");
        set(es, ox - 1, Y + 1, oz + 15, "lily_pad");
        set(es, ox - 4, Y + 1, oz + 16, "lily_pad");
        // Beehives + picnic corner.
        set(es, ox + 14, Y + 2, oz - 3, "bee_nest[facing=west]");
        set(es, ox + 14, Y + 1, oz - 3, OAK_LOG_Y);
        fill(es, ox + 10, Y + 1, oz + 10, ox + 12, Y + 1, oz + 10, "oak_stairs[facing=south]");
        set(es, ox + 11, Y + 1, oz + 12, "oak_planks");
        set(es, ox + 11, Y + 2, oz + 12, "cake");
        set(es, ox + 11, Y + 1, oz + 11, "yellow_carpet");
    }

    // ────────────────────────────────────────────────────────────────────────
    // SW: the Observatory — domed tower with a copper telescope + star deck
    // ────────────────────────────────────────────────────────────────────────

    private void observatory(EditSession es) {
        int tx = CX - 48, tz = CZ + 48;
        // Tower body.
        cyl(es, tx, Y, tz, 6.4, 16, DS_BRICK);
        tube(es, tx, Y, tz, 6.4, 7.2, 16, BLACK_POL);
        // Doorway toward spawn.
        fill(es, tx + 6, Y + 1, tz - 1, tx + 7, Y + 3, tz + 1, "air");
        // Window portholes.
        for (int h = 4; h <= 12; h += 4) {
            set(es, tx - 7, Y + h, tz, GLASS_L);
            set(es, tx, Y + h, tz - 7, GLASS_Y);
            set(es, tx, Y + h, tz + 7, GLASS_Y);
        }
        // Dome with an open observation slit.
        hollowSphere(es, tx, Y + 18, tz, 7.2, 1.2, "polished_deepslate");
        fill(es, tx - 1, Y + 18, tz - 7, tx + 1, Y + 25, tz, "air");    // slit
        fill(es, tx - 8, Y + 11, tz - 8, tx + 8, Y + 13, tz + 8, "air"); // keep interior clear
        disk(es, tx, Y + 16, tz, 6.2, DARK_PLANK);                       // observation floor
        // Copper telescope poking through the slit (diagonal barrel).
        for (int i = 0; i < 6; i++) {
            set(es, tx, Y + 17 + i, tz - 2 - i, COPPER);
            set(es, tx, Y + 17 + i, tz - 3 - i, i == 5 ? "tinted_glass" : COPPER_OX);
        }
        set(es, tx, Y + 16, tz - 2, "lodestone");
        // Star deck: dark platform with "constellation" lights.
        disk(es, tx, Y + 1, tz + 12, 6, "polished_blackstone");
        scatter(es, tx, Y + 1, tz + 12, 5.5, 0.18, 401L, SEA_LANTERN, "verdant_froglight");
        ring(es, tx, Y + 1, tz + 12, 5.6, 6.4, "polished_blackstone_wall");
        // Spiral stairs hint inside (ladder column).
        column(es, tx + 5, tz, Y + 1, Y + 15, "ladder[facing=west]");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Paths, lamps, sky, edge
    // ────────────────────────────────────────────────────────────────────────

    /** Patterned walkways: axes to the four buildings + a ring boulevard. */
    private void pathNetwork(EditSession es) {
        // Axis paths (5 wide): gravel core, deepslate borders, plank center line.
        for (int d = 24; d <= 52; d++) {
            axisPathSlice(es, CX, CZ - d, true);
            axisPathSlice(es, CX, CZ + d, true);
            axisPathSlice(es, CX - d, CZ, false);
            axisPathSlice(es, CX + d, CZ, false);
        }
        // Ring boulevard at r=40 connecting the corner landmarks.
        ring(es, CX, Y, CZ, 39, 41, "dirt_path");
        ring(es, CX, Y, CZ, 38.4, 39, COBBLED_DS);
        ring(es, CX, Y, CZ, 41, 41.6, COBBLED_DS);
        // Diagonal spurs from ring to the corner builds.
        int[][] diags = {{48, -48}, {-48, -48}, {48, 48}, {-48, 48}};
        for (int[] dxz : diags) {
            double len = Math.sqrt(dxz[0] * dxz[0] + dxz[1] * dxz[1]);
            double ux = dxz[0] / len, uz = dxz[1] / len;
            for (int s = 29; s <= (int) len - 16; s++) {
                int px = CX + (int) Math.round(ux * s);
                int pz = CZ + (int) Math.round(uz * s);
                set(es, px, Y, pz, "dirt_path");
                set(es, px + 1, Y, pz, "gravel");
                set(es, px - 1, Y, pz, "gravel");
            }
        }
    }

    private void axisPathSlice(EditSession es, int x, int z, boolean northSouth) {
        if (northSouth) {
            set(es, x - 2, Y, z, COBBLED_DS);
            set(es, x - 1, Y, z, "gravel");
            set(es, x, Y, z, (z % 4 == 0) ? OAK_PLANK : "dirt_path");
            set(es, x + 1, Y, z, "gravel");
            set(es, x + 2, Y, z, COBBLED_DS);
        } else {
            set(es, x, Y, z - 2, COBBLED_DS);
            set(es, x, Y, z - 1, "gravel");
            set(es, x, Y, z, (x % 4 == 0) ? OAK_PLANK : "dirt_path");
            set(es, x, Y, z + 1, "gravel");
            set(es, x, Y, z + 2, COBBLED_DS);
        }
    }

    /** Twin-lantern posts along the ring boulevard. */
    private void lampPosts(EditSession es) {
        for (int i = 0; i < 16; i++) {
            double a = i * (Math.PI / 8) + Math.PI / 16;
            int px = CX + (int) Math.round(Math.cos(a) * 44);
            int pz = CZ + (int) Math.round(Math.sin(a) * 44);
            column(es, px, pz, Y + 1, Y + 4, "polished_blackstone_wall");
            set(es, px, Y + 5, pz, "polished_blackstone");
            set(es, px - 1, Y + 5, pz, H_LANTERN);
            set(es, px + 1, Y + 5, pz, H_LANTERN);
            set(es, px, Y + 6, pz, "lightning_rod[facing=up]");
        }
    }

    /** Halo rings + the floating lemon sun, higher and bigger than v1. */
    private void skyWork(EditSession es) {
        ring(es, CX, Y + 46, CZ, 30, 31, GLASS_Y);
        ring(es, CX, Y + 52, CZ, 22, 23, GLASS_L);
        ring(es, CX, Y + 58, CZ, 14, 15, GLASS_Y);
        // Lemon sun NE high above the orchard quadrant.
        int sx = CX + 34, sy = Y + 54, sz = CZ + 34;
        sphere(es, sx, sy, sz, 6.5, CONCRETE_Y);
        hollowSphere(es, sx, sy, sz, 8.2, 1.0, GLASS_Y);
        sphere(es, sx, sy, sz, 2.5, "glowstone");
        set(es, sx, sy + 9, sz, "end_rod[facing=up]");
        // Sun rays.
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI / 4);
            int rx = sx + (int) Math.round(Math.cos(a) * 10);
            int rz = sz + (int) Math.round(Math.sin(a) * 10);
            set(es, rx, sy, rz, LIGHT_Y);
        }
    }

    /** Catenary lantern chains from the citadel crown to each axis building. */
    private void lanternChains(EditSession es) {
        int[][] targets = {{0, -58}, {0, 58}, {58, 0}, {-58, 0}};
        for (int[] t : targets) {
            for (int s = 1; s <= 9; s++) {
                double f = s / 10.0;
                int px = CX + (int) Math.round(t[0] * f);
                int pz = CZ + (int) Math.round(t[1] * f);
                int py = Y + 16 - (int) Math.round(Math.sin(f * Math.PI) * 4);
                set(es, px, py, pz, CHAIN);
                set(es, px, py - 1, pz, s % 2 == 0 ? H_LANTERN : GLASS_Y);
            }
        }
    }

    /** Two hot-air balloons drifting over the rim. */
    private void balloons(EditSession es) {
        balloon(es, CX - 30, Y + 34, CZ + 62, "yellow_wool", "orange_wool");
        balloon(es, CX + 62, Y + 40, CZ - 30, "lime_wool", "yellow_wool");
    }

    private void balloon(EditSession es, int bx, int by, int bz, String main, String stripe) {
        sphere(es, bx, by, bz, 5.2, main);
        for (int h = -5; h <= 5; h += 2) {
            ring(es, bx, by + h, bz, Math.sqrt(Math.max(0, 27 - h * h)) - 0.6,
                    Math.sqrt(Math.max(0, 27 - h * h)) + 0.4, stripe);
        }
        fill(es, bx - 1, by - 9, bz - 1, bx + 1, by - 8, bz + 1, DARK_PLANK);   // basket
        fill(es, bx - 1, by - 7, bz - 1, bx + 1, by - 7, bz + 1, "air");
        set(es, bx - 1, by - 7, bz - 1, CHAIN);
        set(es, bx + 1, by - 7, bz - 1, CHAIN);
        set(es, bx - 1, by - 7, bz + 1, CHAIN);
        set(es, bx + 1, by - 7, bz + 1, CHAIN);
        set(es, bx, by - 8, bz, "campfire[lit=true]");
    }

    /** Rim railing with glow accents, broken at the four bridges. */
    private void edgeRail(EditSession es) {
        for (int i = 0; i < 180; i++) {
            double a = i * (Math.PI * 2 / 180);
            int deg = (int) Math.round(Math.toDegrees(a));
            // Leave gaps near the diagonal bridges (45/135/225/315 ±6°).
            int m = ((deg % 90) + 90) % 90;
            if (m >= 39 && m <= 51) continue;
            int x = CX + (int) Math.round(Math.cos(a) * 91);
            int z = CZ + (int) Math.round(Math.sin(a) * 91);
            set(es, x, Y + 1, z, "mossy_stone_brick_wall");
            if (i % 12 == 0) {
                set(es, x, Y + 2, z, SEA_LANTERN);
            } else if (i % 6 == 0) {
                set(es, x, Y + 2, z, "torch");
            }
        }
    }

    /** Two waterfalls pouring off the rim into the void. */
    private void waterfalls(EditSession es) {
        int[][] spots = {{88, 20}, {-88, -20}};
        for (int[] s : spots) {
            int wx = CX + s[0], wz = CZ + s[1];
            fill(es, wx - 2, Y, wz - 1, wx + 2, Y, wz + 1, STONE);
            fill(es, wx - 1, Y, wz, wx + 1, Y, wz, "water");
            fill(es, wx - 1, Y - 1, wz, wx + 1, Y - 10, wz, "water");
            set(es, wx - 2, Y + 1, wz - 1, "mossy_cobblestone");
            set(es, wx + 2, Y + 1, wz + 1, "mossy_cobblestone");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Detail pass
    // ────────────────────────────────────────────────────────────────────────

    /** Benches + flower planters facing the citadel on all four axes. */
    private void benchesAndPlanters(EditSession es) {
        int[][] spots = {{30, 6}, {30, -6}, {-30, 6}, {-30, -6}};
        for (int[] s : spots) {
            // E/W benches
            set(es, CX + s[0], Y + 1, CZ + s[1], "oak_stairs[facing=" + (s[1] > 0 ? "north" : "south") + "]");
            set(es, CX + s[0] + 1, Y + 1, CZ + s[1], "oak_stairs[facing=" + (s[1] > 0 ? "north" : "south") + "]");
            // N/S benches (mirrored onto the other axis)
            set(es, CX + s[1], Y + 1, CZ + s[0], "oak_stairs[facing=" + (s[1] > 0 ? "west" : "east") + "]");
            set(es, CX + s[1], Y + 1, CZ + s[0] + 1, "oak_stairs[facing=" + (s[1] > 0 ? "west" : "east") + "]");
        }
        // Planter boxes at the citadel stair mouths.
        int[][] planters = {{26, 4}, {26, -4}, {-26, 4}, {-26, -4}};
        for (int[] p : planters) {
            set(es, CX + p[0], Y + 1, CZ + p[1], MOSS);
            set(es, CX + p[0], Y + 2, CZ + p[1], "flowering_azalea");
            set(es, CX + p[1], Y + 1, CZ + p[0], MOSS);
            set(es, CX + p[1], Y + 2, CZ + p[0], "azalea");
        }
    }

    /** Subtle deck speckle between the paths so the lawn isn't uniform. */
    private void deckSpeckle(EditSession es) {
        scatter(es, CX, Y + 1, CZ, 88, 0.015, 501L, "glow_lichen[down=true]");
        scatter(es, CX, Y + 1, CZ, 60, 0.02, 502L, "moss_carpet");
        scatter(es, CX, Y + 1, CZ, 86, 0.008, 503L, "pink_petals", "torchflower");
    }

    /** Welcome arch straddling the north path just outside the citadel. */
    private void welcomeArch(EditSession es) {
        int z = CZ - 28;
        column(es, CX - 4, z, Y + 1, Y + 6, "quartz_pillar[axis=y]");
        column(es, CX + 4, z, Y + 1, Y + 6, "quartz_pillar[axis=y]");
        fill(es, CX - 4, Y + 7, z, CX + 4, Y + 7, z, TERRA_Y);
        fill(es, CX - 2, Y + 8, z, CX + 2, Y + 8, z, CONCRETE_L);
        set(es, CX, Y + 9, z, GOLD);
        set(es, CX - 3, Y + 6, z, "yellow_wall_banner[facing=south]");
        set(es, CX + 3, Y + 6, z, "lime_wall_banner[facing=south]");
        set(es, CX - 4, Y + 7, z + 1, H_LANTERN);
        set(es, CX + 4, Y + 7, z + 1, H_LANTERN);
    }

    /** The lemon mascot: a cheeky concrete lemon with a gold crown, SE lawn. */
    private void mascotStatue(EditSession es) {
        int mx = CX + 28, mz = CZ + 28;
        disk(es, mx, Y + 1, mz, 3.6, "smooth_quartz");
        ellipsoid(es, mx, Y + 5, mz, 2.6, 3.4, 2.6, CONCRETE_Y);
        set(es, mx, Y + 9, mz, CONCRETE_L);                       // stem
        set(es, mx - 1, Y + 6, mz - 2, "polished_blackstone");    // eyes
        set(es, mx + 1, Y + 6, mz - 2, "polished_blackstone");
        set(es, mx, Y + 4, mz - 3, TERRA_O);                      // smile hint
        // Crown.
        ring(es, mx, Y + 10, mz, 1.2, 2.0, GOLD);
        set(es, mx, Y + 11, mz, "end_rod[facing=up]");
        // Lawn ring.
        ring(es, mx, Y + 1, mz, 3.6, 4.4, "yellow_carpet");
    }

    // ────────────────────────────────────────────────────────────────────────
    // "Even better" pass
    // ────────────────────────────────────────────────────────────────────────

    /** 5x5 block font used by the marquee — each letter as 5 row strings. */
    private static final java.util.Map<Character, String[]> FONT = java.util.Map.of(
            'L', new String[]{"X....", "X....", "X....", "X....", "XXXXX"},
            'E', new String[]{"XXXXX", "X....", "XXXX.", "X....", "XXXXX"},
            'M', new String[]{"X...X", "XX.XX", "X.X.X", "X...X", "X...X"},
            'O', new String[]{".XXX.", "X...X", "X...X", "X...X", ".XXX."},
            'N', new String[]{"X...X", "XX..X", "X.X.X", "X..XX", "X...X"},
            'P', new String[]{"XXXX.", "X...X", "XXXX.", "X....", "X...."},
            'V', new String[]{"X...X", "X...X", "X...X", ".X.X.", "..X.."});

    /** Giant LEMONPVP marquee floating above the cathedral, facing spawn. */
    private void marqueeSign(EditSession es) {
        String text = "LEMONPVP";
        int width = text.length() * 6 - 1;               // 5 wide + 1 gap
        int x0 = CX - width / 2;
        int yTop = Y + 42;
        int z = CZ - 66;
        // Backboard.
        fill(es, x0 - 2, yTop - 6, z + 1, x0 + width + 1, yTop + 1, z + 1, "polished_blackstone");
        for (int lx = x0 - 2; lx <= x0 + width + 1; lx += 4) {
            set(es, lx, yTop + 2, z + 1, H_LANTERN);
        }
        // Letters.
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = FONT.get(text.charAt(i));
            if (glyph == null) continue;
            int lx0 = x0 + i * 6;
            String color = (i % 2 == 0) ? CONCRETE_Y : CONCRETE_L;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    if (glyph[row].charAt(col) == 'X') {
                        set(es, lx0 + col, yTop - row, z, color);
                    }
                }
            }
        }
        // Support chains down to the cathedral spires.
        column(es, x0 - 2, z + 1, Y + 30, yTop - 7, CHAIN);
        column(es, x0 + width + 1, z + 1, Y + 30, yTop - 7, CHAIN);
    }

    /** Citadel: gold tier trims, corner braziers, orbiting glass rings. */
    private void citadelUpgrade(EditSession es) {
        // Thin gold trim on each tier lip.
        for (int i = 0; i < 44; i++) {
            double a = i * (Math.PI * 2 / 44);
            if (i % 2 == 0) continue;
            set(es, CX + (int) Math.round(Math.cos(a) * 22), Y + 1,
                    CZ + (int) Math.round(Math.sin(a) * 22), GILDED);
        }
        // Four braziers between the stair mouths on tier 1.
        int[][] br = {{16, 16}, {-16, 16}, {16, -16}, {-16, -16}};
        for (int[] b : br) {
            int bx = CX + b[0], bz = CZ + b[1];
            set(es, bx, Y + 2, bz, "polished_blackstone_wall");
            set(es, bx, Y + 3, bz, "polished_blackstone_slab[type=bottom]");
            set(es, bx, Y + 4, bz, "campfire[lit=true]");
        }
        // Two extra orbit rings around the crown at offset heights.
        ring(es, CX, Y + 17, CZ, 8.5, 9.2, GLASS_L);
        ring(es, CX, Y + 23, CZ, 7.0, 7.7, GLASS_Y);
    }

    /** Cathedral: rose window, buttresses, golden approach. */
    private void cathedralUpgrade(EditSession es) {
        int bz = CZ - 62;
        // Rose window on the south gable above the arch (vertical, hand-drawn —
        // ring() only paints horizontal annuli).
        set(es, CX, Y + 10, bz + 7, GLASS_Y);
        set(es, CX - 1, Y + 10, bz + 7, GLASS_L);
        set(es, CX + 1, Y + 10, bz + 7, GLASS_L);
        set(es, CX, Y + 11, bz + 7, GLASS_L);
        set(es, CX, Y + 9, bz + 7, GLASS_L);
        set(es, CX - 2, Y + 10, bz + 7, GOLD);
        set(es, CX + 2, Y + 10, bz + 7, GOLD);
        set(es, CX, Y + 12, bz + 7, GOLD);
        // Buttresses along the nave sides.
        for (int bxo = -9; bxo <= 9; bxo += 6) {
            set(es, CX + bxo, Y + 1, bz + 8, DS_BRICK);
            set(es, CX + bxo, Y + 2, bz + 8, DS_STAIR_S);
            set(es, CX + bxo, Y + 1, bz - 8, DS_BRICK);
            set(es, CX + bxo, Y + 2, bz - 8, DS_STAIR_N);
        }
        // Golden runner from the welcome arch to the portal.
        for (int d = 30; d <= 54; d += 2) {
            set(es, CX, Y, CZ - d, GILDED);
        }
    }

    /** Forge: entry awning, ingot stacks, quench cauldron, tool wall. */
    private void forgeUpgrade(EditSession es) {
        int bx = CX + 62;
        // Awning over the west entrance.
        fill(es, bx - 12, Y + 5, CZ - 2, bx - 9, Y + 5, CZ + 2, "dark_oak_slab[type=bottom]");
        column(es, bx - 12, CZ - 2, Y + 1, Y + 4, "dark_oak_fence");
        column(es, bx - 12, CZ + 2, Y + 1, Y + 4, "dark_oak_fence");
        set(es, bx - 11, Y + 4, CZ, H_LANTERN);
        // Ingot stacks beside the door.
        set(es, bx - 10, Y + 1, CZ - 4, "iron_block");
        set(es, bx - 10, Y + 2, CZ - 4, "raw_iron_block");
        set(es, bx - 10, Y + 1, CZ + 4, "waxed_copper_block");
        set(es, bx - 9, Y + 1, CZ - 5, "raw_copper_block");
        // Quench barrel + tool wall inside.
        set(es, bx + 1, Y + 1, CZ + 8, "water_cauldron[level=3]");
        fill(es, bx - 1, Y + 3, CZ + 9, bx + 2, Y + 3, CZ + 9, "iron_bars");
        set(es, bx, Y + 4, CZ + 9, "wall_torch[facing=north]");
    }

    /** Bazaar: flag lines between stalls, crate piles, produce stand. */
    private void bazaarUpgrade(EditSession es) {
        int bz = CZ + 62;
        // Flag line strung across the hall front.
        for (int fx = -13; fx <= 13; fx++) {
            int fy = Y + 6 - (Math.abs(fx) % 3 == 0 ? 0 : 1);
            if ((fx + 13) % 2 == 0) {
                set(es, CX + fx, fy, bz - 10, (fx % 4 == 0) ? "yellow_wool" : "lime_wool");
            }
        }
        // Crate piles at the hall corners.
        fill(es, CX - 15, Y + 1, bz + 6, CX - 14, Y + 1, bz + 7, "barrel[facing=up]");
        set(es, CX - 15, Y + 2, bz + 7, "barrel[facing=up]");
        fill(es, CX + 14, Y + 1, bz + 6, CX + 15, Y + 1, bz + 7, "barrel[facing=up]");
        set(es, CX + 14, Y + 2, bz + 6, "chest[facing=west]");
        // Produce stand: melons/pumpkins under a small canopy.
        set(es, CX - 14, Y + 1, bz - 6, "melon");
        set(es, CX - 13, Y + 1, bz - 6, "pumpkin");
        set(es, CX - 14, Y + 2, bz - 6, "hay_block");
        fill(es, CX - 15, Y + 4, bz - 7, CX - 12, Y + 4, bz - 5, "orange_wool");
    }

    /** Hall of fame: reflecting pool + statue spotlights. */
    private void hallUpgrade(EditSession es) {
        int bx = CX - 62;
        // Reflecting pool in front of the plinth row.
        fill(es, bx + 4, Y, CZ - 8, bx + 6, Y, CZ + 8, "water");
        fill(es, bx + 3, Y, CZ - 9, bx + 3, Y, CZ + 9, "quartz_bricks");
        fill(es, bx + 7, Y, CZ - 9, bx + 7, Y, CZ + 9, "quartz_bricks");
        set(es, bx + 5, Y, CZ, SEA_LANTERN);
        // Spotlights washing the amethyst wall.
        for (int pz = -12; pz <= 12; pz += 6) {
            set(es, bx - 6, Y + 1, CZ + pz, "polished_blackstone_slab[type=bottom]");
            set(es, bx - 6, Y + 2, CZ + pz, "end_rod[facing=west]");
        }
    }

    /** Four rim balconies jutting over the void at the cardinal points. */
    private void rimBalconies(EditSession es) {
        int[][] dirs = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int bx = CX + d[0] * 96, bz = CZ + d[1] * 96;
            disk(es, bx, Y, bz, 4.6, DARK_PLANK);
            ring(es, bx, Y + 1, bz, 3.8, 4.8, "dark_oak_fence");
            // Opening back toward the island.
            fill(es, bx - d[0] * 4 - 1, Y + 1, bz - d[1] * 4 - 1,
                    bx - d[0] * 3 + 1, Y + 1, bz - d[1] * 3 + 1, "air");
            // Bench + spyglass tripod.
            set(es, bx, Y + 1, bz + (d[0] != 0 ? 2 : 0) , "dark_oak_stairs[facing="
                    + (d[0] != 0 ? "north" : d[1] > 0 ? "south" : "north") + "]");
            set(es, bx + d[0], Y + 1, bz + d[1], "end_rod[facing=up]");
            set(es, bx + d[0], Y + 2, bz + d[1], "grindstone[face=floor,facing=north]");
            set(es, bx - d[0], Y + 2, bz - d[1], H_LANTERN);
            set(es, bx - d[0], Y + 3, bz - d[1], CHAIN);
        }
    }

    /** Puffy wool clouds drifting at different heights. */
    private void skyClouds(EditSession es) {
        int[][] clouds = {{-52, 30, 70}, {40, 38, -66}, {-64, 44, -20}, {24, 48, 78}, {70, 36, 24}};
        for (int i = 0; i < clouds.length; i++) {
            int cx = CX + clouds[i][0], cy = Y + clouds[i][1], cz = CZ + clouds[i][2];
            ellipsoid(es, cx, cy, cz, 5 + i % 3, 1.6, 3 + (i + 1) % 3, "white_wool");
            ellipsoid(es, cx + 3, cy + 1, cz + 1, 3, 1.2, 2, "white_wool");
        }
    }

    /** Accent trees, cliff vines and flower drifts across the lawn quadrants. */
    private void greenUpgrade(EditSession es) {
        // Birch + cherry accent trees at hand-picked lawn spots.
        int[][] birch = {{-34, 22}, {22, -36}, {-20, -40}};
        for (int[] t : birch) {
            int txx = CX + t[0], tzz = CZ + t[1];
            column(es, txx, tzz, Y + 1, Y + 5, "birch_log[axis=y]");
            sphere(es, txx, Y + 7, tzz, 3.0, "birch_leaves[persistent=true]");
        }
        int[][] cherry = {{36, 20}, {-24, 34}};
        for (int[] t : cherry) {
            int txx = CX + t[0], tzz = CZ + t[1];
            column(es, txx, tzz, Y + 1, Y + 5, "cherry_log[axis=y]");
            sphere(es, txx, Y + 7, tzz, 3.4, "cherry_leaves[persistent=true]");
            scatter(es, txx, Y + 1, tzz, 4, 0.4, 601L + t[0], "pink_petals");
        }
        // Vines spilling over the cliff rim.
        for (int i = 0; i < 30; i++) {
            double a = i * (Math.PI * 2 / 30) + 0.05;
            int x = CX + (int) Math.round(Math.cos(a) * 94);
            int z = CZ + (int) Math.round(Math.sin(a) * 94);
            int len = 2 + (i % 3);
            column(es, x, z, Y - len, Y - 1, i % 2 == 0 ? "weeping_vines" : "cave_vines");
        }
        // Flower drifts.
        scatter(es, CX - 30, Y + 1, CZ - 30, 10, 0.25, 611L, "allium", "azure_bluet");
        scatter(es, CX + 30, Y + 1, CZ + 30, 10, 0.25, 612L, "cornflower", "oxeye_daisy");
    }

    /** A hidden amethyst geode pocket in the underside spike. */
    private void undersideGeode(EditSession es) {
        int gy = Y - 18;
        hollowSphere(es, CX + 8, gy, CZ - 6, 6.5, 1.2, "smooth_basalt");
        hollowSphere(es, CX + 8, gy, CZ - 6, 5.3, 1.2, "calcite");
        hollowSphere(es, CX + 8, gy, CZ - 6, 4.1, 1.2, "amethyst_block");
        // Open the bottom so it's visible from below.
        fill(es, CX + 5, gy - 7, CZ - 9, CX + 11, gy - 4, CZ - 3, "air");
        set(es, CX + 8, gy - 3, CZ - 6, "amethyst_cluster[facing=down]");
        set(es, CX + 6, gy - 3, CZ - 7, "medium_amethyst_bud[facing=down]");
        set(es, CX + 10, gy - 3, CZ - 5, "amethyst_cluster[facing=down]");
    }
}
