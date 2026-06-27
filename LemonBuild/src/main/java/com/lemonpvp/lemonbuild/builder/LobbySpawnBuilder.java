package com.lemonpvp.lemonbuild.builder;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.World;

/**
 * LobbySpawnBuilder - "LemonPvP Grand Hub". Massive, ultra-detailed floating
 * lobby island centered on world (0, 64, 0). Requires FastAsyncWorldEdit or
 * WorldEdit. Fully deterministic via {@link BuildHelper#noise}. Composed of 20
 * independent districts (underside, terrain, plaza, a giant central tree, four
 * themed portal gateways, a ring boulevard, four themed quarters, waterfalls,
 * perimeter towers, satellite islands, sky decor and a global lighting pass).
 */
public class LobbySpawnBuilder extends BuildHelper {

    private static final int R  = 64;  // island radius
    private static final int SY = 64;  // grass surface y

    public LobbySpawnBuilder(World world) {
        super(world);
    }

    @Override
    public void build() {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession es = WorldEdit.getInstance().newEditSession(weWorld)) {
            buildUnderside(es);
            buildTerrain(es);
            buildPlaza(es);
            buildGrandTreeTrunk(es);
            buildGrandTreeBranches(es);
            buildGrandTreeCanopy(es);
            buildPortalNorth(es);
            buildPortalSouth(es);
            buildPortalEast(es);
            buildPortalWest(es);
            buildRingRoad(es);
            buildGardenQuarter(es);
            buildMarketQuarter(es);
            buildMonumentQuarter(es);
            buildAmphitheaterQuarter(es);
            buildWaterfalls(es);
            buildRailingTowers(es);
            buildSatelliteIslands(es);
            buildSkyDecor(es);
            buildLighting(es);
        }
    }

    private void buildUnderside(EditSession es) {
    // ---- Inverted layered cone forming the island bottom ----
    // Top of the underside sits just below the grass surface (y=63),
    // tapering down to a rocky tip near y=28.
    final int topY = 63;
    final int tipY = 28;
    long seedA = 91271L, seedB = 44037L, seedC = 70513L, seedD = 18829L;

    // Material palette per depth band (top = lighter, deep = darker).
    String[] shallow = {"stone", "andesite", "cobblestone", "tuff"};
    String[] mid = {"tuff", "andesite", "deepslate", "cobbled_deepslate"};
    String[] deep = {"deepslate", "cobbled_deepslate", "tuff", "polished_deepslate"};

    // Build the solid disk shells, shrinking radius as we descend.
    for (int y = topY; y >= tipY; y--) {
        double t = (double) (topY - y) / (double) (topY - tipY); // 0..1 downward
        // Cone profile: radius shrinks with depth, eased for a bulbous belly.
        double baseR = 62.0;
        double ease = 1.0 - Math.pow(t, 1.45);
        double r = baseR * ease;
        if (r < 1.5) r = 1.5;

        // Choose palette by band.
        String[] pal;
        if (t < 0.33) pal = shallow;
        else if (t < 0.66) pal = mid;
        else pal = deep;

        // Lay each ring of this shell as a noise-textured disk slice.
        buildUndersideShell(es, y, r, pal, t, seedA, seedB);
    }

    // ---- Noise-textured surface patches on the visible underbelly ----
    buildUndersidePatches(es, topY, tipY, seedC);

    // ---- Hanging dripstone stalactites of varied length ----
    buildUndersideDripstone(es, topY, tipY, seedB, seedD);

    // ---- Amethyst geode clusters tucked into the rock ----
    buildUndersideAmethyst(es, topY, tipY, seedC, seedA);

    // ---- Glowing exposed ore veins ----
    buildUndersideOreVeins(es, topY, tipY, seedD, seedB);

    // ---- Long dangling hanging_roots, densest near the tip ----
    buildUndersideRoots(es, topY, tipY, seedA, seedC);

    // ---- A glowstone-cored tip lantern for soft underlight ----
    buildUndersideTip(es, tipY);
}

private void buildUndersideShell(EditSession es, int y, double r, String[] pal,
                                 double t, long seedA, long seedB) {
    int ri = (int) Math.ceil(r);
    for (int dx = -ri; dx <= ri; dx++) {
        for (int dz = -ri; dz <= ri; dz++) {
            double d2 = dx * dx + dz * dz;
            if (d2 > r * r) continue;
            // Only place the OUTER shell of each slice (keep interior solid but
            // cheaper) — interior fill near edge plus a thin core.
            double edge = r - Math.sqrt(d2);
            // Carve a slightly noisy outer boundary so the cone isn't perfectly round.
            double n = noise(dx, dz, seedA);
            if (edge < 0.0) continue;
            if (edge > 2.5 + n * 2.0) {
                // interior — fill sparsely to save work but keep solidity on rim slices
                if (edge > 6.0) continue;
            }
            // Pick block from palette via layered noise for an organic mottle.
            double m = noise(dx * 2 + y, dz * 2 - y, seedB);
            int idx = (int) (m * pal.length);
            if (idx >= pal.length) idx = pal.length - 1;
            String id = pal[idx];
            // Occasional smooth_basalt / blackstone speckle deep down.
            if (t > 0.7 && noise(dx - y, dz + y, seedA) > 0.88) {
                id = "blackstone";
            } else if (t < 0.25 && noise(dx + y, dz - y, seedB) > 0.9) {
                id = "mossy_cobblestone";
            }
            set(es, dx, y, dz, id);
        }
    }
}

private void buildUndersidePatches(EditSession es, int topY, int tipY, long seed) {
    // Scatter raised/recessed mottled patches across the broad upper underbelly
    // to break up the layered cone with texture.
    for (int i = 0; i < 60; i++) {
        double a = noise(i, i * 3, seed) * Math.PI * 2.0;
        double rad = 8.0 + noise(i * 5, i, seed) * 48.0;
        int cx = (int) (Math.cos(a) * rad);
        int cz = (int) (Math.sin(a) * rad);
        // Depth follows the cone so patches hug the surface.
        double t = rad / 62.0;
        int py = (int) (topY - 1 - t * (topY - tipY) * 0.85);
        if (py < tipY + 2) py = tipY + 2;
        double pr = 2.0 + noise(i * 7, i * 2, seed) * 3.5;
        String[] opts = {"tuff", "andesite", "gravel", "cobbled_deepslate", "calcite"};
        int oi = (int) (noise(i, i, seed) * opts.length);
        if (oi >= opts.length) oi = opts.length - 1;
        disk(es, cx, py, cz, pr, opts[oi]);
        if (noise(i * 2, i * 9, seed) > 0.6) {
            disk(es, cx, py - 1, cz, pr * 0.6, opts[oi]);
        }
    }
}

private void buildUndersideDripstone(EditSession es, int topY, int tipY, long seedA, long seedB) {
    // Hanging stalactites of varied length, more frequent toward center/tip.
    for (int i = 0; i < 140; i++) {
        double a = noise(i, i * 2 + 1, seedA) * Math.PI * 2.0;
        double rad = noise(i * 3, i, seedB) * 60.0;
        int x = (int) (Math.cos(a) * rad);
        int z = (int) (Math.sin(a) * rad);
        double t = rad / 62.0;
        // Surface y at this radius (where the cone bottom is).
        int surfY = (int) (topY - t * (topY - tipY) * 0.95);
        if (surfY <= tipY + 1) continue;
        // Length grows toward the tip; vary by noise.
        double centerBias = 1.0 - t;
        int len = 1 + (int) (noise(x, z, seedA) * (4.0 + centerBias * 9.0));
        int bottom = surfY - len;
        if (bottom < tipY) bottom = tipY;
        // Build a tapering stalactite: tip at bottom, base merging with rock.
        for (int y = surfY; y >= bottom; y--) {
            int fromTip = y - bottom;
            // The tip block is the pointed dripstone tip; above are middle frustums.
            String state;
            if (y == bottom) {
                state = "pointed_dripstone[thickness=tip,vertical_direction=down]";
            } else if (fromTip == 1) {
                state = "pointed_dripstone[thickness=frustum,vertical_direction=down]";
            } else if (y == surfY) {
                state = "pointed_dripstone[thickness=base,vertical_direction=down]";
            } else {
                state = "pointed_dripstone[thickness=middle,vertical_direction=down]";
            }
            set(es, x, y, z, state);
        }
        // Occasionally fatten the base with a dripstone_block plug.
        if (len >= 5 && noise(x + 1, z - 1, seedB) > 0.5) {
            set(es, x, surfY, z, "dripstone_block");
            set(es, x, surfY - 1, z, "dripstone_block");
        }
    }
}

private void buildUndersideAmethyst(EditSession es, int topY, int tipY, long seedA, long seedB) {
    // Amethyst geode pockets: budding amethyst inside calcite/amethyst_block shells.
    for (int i = 0; i < 14; i++) {
        double a = noise(i * 4 + 2, i, seedA) * Math.PI * 2.0;
        double rad = 6.0 + noise(i, i * 6, seedB) * 44.0;
        int cx = (int) (Math.cos(a) * rad);
        int cz = (int) (Math.sin(a) * rad);
        double t = rad / 62.0;
        int cy = (int) (topY - 2 - t * (topY - tipY) * 0.8);
        if (cy < tipY + 3) cy = tipY + 3;
        double gr = 2.0 + noise(i * 3, i * 2, seedA) * 1.5;
        // Outer calcite crust, inner amethyst block, hollow core.
        sphere(es, cx, cy, cz, gr + 1.0, "calcite");
        sphere(es, cx, cy, cz, gr, "amethyst_block");
        sphere(es, cx, cy, cz, gr - 1.0, "air");
        // Stud the lower interior with budding amethyst clusters pointing down.
        for (int j = 0; j < 10; j++) {
            double ja = noise(j, i * 2 + j, seedB) * Math.PI * 2.0;
            double jr = (gr - 1.2) * noise(j * 3, i + j, seedA);
            int bx = cx + (int) (Math.cos(ja) * jr);
            int bz = cz + (int) (Math.sin(ja) * jr);
            int by = cy - (int) (gr - 1.0);
            String bud;
            double sel = noise(bx, bz, seedB);
            if (sel > 0.75) bud = "amethyst_cluster[facing=down]";
            else if (sel > 0.5) bud = "large_amethyst_bud[facing=down]";
            else bud = "small_amethyst_bud[facing=down]";
            set(es, bx, by, bz, bud);
        }
    }
}

private void buildUndersideOreVeins(EditSession es, int topY, int tipY, long seedA, long seedB) {
    // Glowing exposed ore veins: streaks of ore that read as lit pockets.
    String[] ores = {
        "deepslate_gold_ore", "deepslate_diamond_ore", "deepslate_emerald_ore",
        "deepslate_lapis_ore", "deepslate_redstone_ore[lit=true]", "glowstone",
        "shroomlight", "deepslate_copper_ore"
    };
    for (int i = 0; i < 36; i++) {
        double a = noise(i * 2 + 5, i, seedA) * Math.PI * 2.0;
        double rad = 4.0 + noise(i, i * 4, seedB) * 50.0;
        int cx = (int) (Math.cos(a) * rad);
        int cz = (int) (Math.sin(a) * rad);
        double t = rad / 62.0;
        int cy = (int) (topY - 2 - t * (topY - tipY) * 0.85);
        if (cy < tipY + 1) cy = tipY + 1;
        int oi = (int) (noise(i * 3, i, seedA) * ores.length);
        if (oi >= ores.length) oi = ores.length - 1;
        String ore = ores[oi];
        // A short jagged vein walk.
        int x = cx, y = cy, z = cz;
        int steps = 3 + (int) (noise(i, i * 2, seedB) * 5.0);
        for (int s = 0; s < steps; s++) {
            set(es, x, y, z, ore);
            // small blob around each step
            if (noise(x + s, z - s, seedA) > 0.5) set(es, x + 1, y, z, ore);
            if (noise(x - s, z + s, seedB) > 0.5) set(es, x, y, z + 1, ore);
            if (noise(x, z, seedA) > 0.7) set(es, x, y - 1, z, ore);
            // wander
            x += (noise(x * 2, z, seedA) > 0.5) ? 1 : -1;
            z += (noise(x, z * 2, seedB) > 0.5) ? 1 : -1;
            if (noise(x, z, seedA) > 0.6) y -= 1;
        }
        // A faint glow source embedded so the vein "glows".
        if (oi == 5 || oi == 6 || noise(cx, cz, seedB) > 0.7) {
            set(es, cx, cy, cz, "glowstone");
        }
    }
}

private void buildUndersideRoots(EditSession es, int topY, int tipY, long seedA, long seedB) {
    // Long dangling hanging_roots, very dense near the tapering tip.
    for (int i = 0; i < 200; i++) {
        double a = noise(i, i * 3 + 7, seedA) * Math.PI * 2.0;
        // Bias radius toward the center so roots cluster near the tip.
        double rn = noise(i * 2, i, seedB);
        double rad = rn * rn * 58.0;
        int x = (int) (Math.cos(a) * rad);
        int z = (int) (Math.sin(a) * rad);
        double t = rad / 62.0;
        int surfY = (int) (topY - t * (topY - tipY) * 0.95);
        if (surfY <= tipY + 1) continue;
        double centerBias = 1.0 - t;
        // Longer strands near the center.
        int len = 1 + (int) (noise(x, z, seedA) * (3.0 + centerBias * 12.0));
        int bottom = surfY - 1 - len;
        if (bottom < tipY - 3) bottom = tipY - 3;
        // Anchor with a moss/rooted block then trail hanging_roots downward.
        set(es, x, surfY, z, "rooted_dirt");
        for (int y = surfY - 1; y >= bottom; y--) {
            set(es, x, y, z, "hanging_roots");
        }
        // Occasionally swap a strand to a glowing spore/cave-vine for accent.
        if (noise(x + 3, z - 3, seedB) > 0.85) {
            for (int y = surfY - 1; y >= bottom; y--) {
                String cv = (y == bottom)
                    ? "cave_vines[berries=true]"
                    : "cave_vines_plant[berries=true]";
                set(es, x, y, z, cv);
            }
        }
    }
}

private void buildUndersideTip(EditSession es, int tipY) {
    // The very tip: a small glowing core wrapped in deepslate + dripstone,
    // casting warm light up into the roots.
    int cx = 0, cz = 0;
    sphere(es, cx, tipY + 1, cz, 3.0, "deepslate");
    sphere(es, cx, tipY + 1, cz, 2.0, "amethyst_block");
    set(es, cx, tipY + 1, cz, "glowstone");
    set(es, cx, tipY, cz, "shroomlight");
    // A final long pointed dripstone hanging from the absolute tip.
    for (int y = tipY - 1; y >= tipY - 6; y--) {
        String state;
        if (y == tipY - 6) state = "pointed_dripstone[thickness=tip,vertical_direction=down]";
        else if (y == tipY - 5) state = "pointed_dripstone[thickness=frustum,vertical_direction=down]";
        else state = "pointed_dripstone[thickness=middle,vertical_direction=down]";
        set(es, cx, y, cz, state);
    }
    // A few hanging chains with lanterns ringing the tip for ambiance.
    for (int k = 0; k < 6; k++) {
        double a = (Math.PI * 2.0 / 6.0) * k;
        int lx = cx + (int) Math.round(Math.cos(a) * 4.0);
        int lz = cz + (int) Math.round(Math.sin(a) * 4.0);
        column(es, lx, lz, tipY, tipY + 2, "chain[axis=y]");
        set(es, lx, tipY - 1, lz, "lantern[hanging=true]");
    }
}

    private void buildTerrain(EditSession es) {
    // ===== Top surface of the floating island =====
    // Disk radius ~64, organic grass surface at y=64 (some y=65 from rolling noise),
    // sub-layers of dirt/coarse_dirt/rooted_dirt, moss & podzol patches,
    // noise-wobbled coastline, and a few stone/andesite outcrops.
    final long SEED = 0x10BB7_5EEDL;
    final double baseR = R;

    for (int x = -R - 4; x <= R + 4; x++) {
        for (int z = -R - 4; z <= R + 4; z++) {
            double d = Math.sqrt((double) (x * x + z * z));

            // --- Wobbled coastline: noise modulates the effective radius ---
            double angWobble = noise(x * 2, z * 2, SEED) * 7.0 - 3.5;
            double coarseWobble = noise(x, z, SEED + 11) * 4.0 - 2.0;
            double edgeR = baseR + angWobble + coarseWobble;
            if (d > edgeR) continue;

            // --- Rolling surface height via layered noise (mostly 64, some 65) ---
            double h1 = noise(x, z, SEED + 101);
            double h2 = noise(x * 3, z * 3, SEED + 202);
            double roll = h1 * 0.7 + h2 * 0.3;
            int topY = SY;
            // Lift gentle mounds toward interior, but flatten near the coast.
            double interior = Math.max(0.0, 1.0 - (d / baseR));
            if (roll > 0.62 && interior > 0.18) topY = SY + 1;
            if (roll > 0.80 && interior > 0.35) topY = SY + 1;

            // --- Coastal bevel: lower the rim a touch for an organic edge ---
            double rimFrac = d / edgeR;
            if (rimFrac > 0.93) topY = SY - 1;
            if (rimFrac > 0.985) topY = SY - 2;

            // --- Pick the surface block by biome-ish noise patches ---
            double patch = noise(x, z, SEED + 303);
            double patch2 = noise(x * 2, z * 2, SEED + 404);
            String surface = "grass_block";
            if (patch > 0.84) {
                surface = "moss_block";
            } else if (patch < 0.16) {
                surface = (patch2 < 0.5) ? "podzol" : "coarse_dirt";
            } else if (patch2 > 0.90) {
                surface = "moss_block";
            }

            // --- Stone / andesite outcrops: rare clustered bumps that pierce grass ---
            double rock = noise(x, z, SEED + 505);
            double rockDetail = noise(x * 4, z * 4, SEED + 606);
            boolean outcrop = rock > 0.90 && interior > 0.22;
            if (outcrop) {
                int rockH = topY + 1 + (rock > 0.955 ? 2 : (rock > 0.93 ? 1 : 0));
                String rockId = (rockDetail < 0.33) ? "andesite"
                        : (rockDetail < 0.62) ? "stone"
                        : (rockDetail < 0.82) ? "cobblestone" : "mossy_cobblestone";
                buildTerrainColumnLayers(es, x, z, topY);
                column(es, x, z, topY, rockH, rockId);
                // a little moss creeps onto the lower rock
                if (rockDetail > 0.5) set(es, x, topY, z, "mossy_cobblestone");
                continue;
            }

            // --- Place the surface block ---
            set(es, x, topY, z, surface);
            // Some grass blocks at the very top of mounds get short_grass above for life
            if (topY == SY + 1 && surface.equals("grass_block")) {
                double deco = noise(x, z, SEED + 707);
                if (deco > 0.55) {
                    set(es, x, topY + 1, z,
                            (deco > 0.92) ? "poppy"
                                    : (deco > 0.86) ? "dandelion"
                                    : (deco > 0.78) ? "tall_grass[half=lower]" : "short_grass");
                }
            } else if (surface.equals("grass_block")) {
                double deco = noise(x * 2, z * 2, SEED + 808);
                if (deco > 0.80) {
                    set(es, x, topY + 1, z,
                            (deco > 0.965) ? "cornflower"
                                    : (deco > 0.93) ? "azure_bluet"
                                    : (deco > 0.88) ? "oxeye_daisy" : "short_grass");
                }
            } else if (surface.equals("podzol")) {
                double deco = noise(x * 3, z * 3, SEED + 909);
                if (deco > 0.88) set(es, x, topY + 1, z, (deco > 0.95) ? "brown_mushroom" : "fern");
            } else if (surface.equals("moss_block")) {
                double deco = noise(x * 2, z * 2, SEED + 121);
                if (deco > 0.90) set(es, x, topY + 1, z, "moss_carpet");
            }

            // --- Sub-surface layering down to y=60 ---
            buildTerrainColumnLayers(es, x, z, topY);
        }
    }

    // ===== Carved features on the surface: a couple of small ponds & clearings =====
    buildTerrainPond(es, -28, 22, 6.5, SEED + 1300);
    buildTerrainPond(es, 34, -18, 5.0, SEED + 1400);
    buildTerrainClearing(es, 18, 30, 7.0, SEED + 1500);

    // ===== Edge accent: scattered overhanging moss/roots just inside the coast =====
    buildTerrainCoastAccents(es, SEED + 1700);
}

// Fills the vertical sub-layers from just under the surface down to y=60.
private void buildTerrainColumnLayers(EditSession es, int x, int z, int topY) {
    final long SEED = 0x10BB7_5EEDL;
    for (int y = topY - 1; y >= 60; y--) {
        int depth = topY - y;
        double n = noise(x, z * 2 + y, SEED + 2100 + y);
        String id;
        if (depth <= 1) {
            // immediate sub-soil: mostly dirt, some coarse/rooted
            id = (n > 0.85) ? "coarse_dirt" : (n > 0.72) ? "rooted_dirt" : "dirt";
        } else if (depth == 2) {
            id = (n > 0.78) ? "rooted_dirt" : "dirt";
        } else {
            // deepest visible layer near y=60: dirt grading into stone outcrops
            id = (n > 0.88) ? "stone" : (n > 0.80) ? "andesite" : (n > 0.62) ? "rooted_dirt" : "dirt";
        }
        set(es, x, y, z, id);
    }
}

// A shallow water pond pressed gently into the surface with a beach rim.
private void buildTerrainPond(EditSession es, int cx, int cz, double r, long seed) {
    for (int x = (int) (cx - r - 2); x <= cx + r + 2; x++) {
        for (int z = (int) (cz - r - 2); z <= cz + r + 2; z++) {
            int dx = x - cx, dz = z - cz;
            double d = Math.sqrt((double) (dx * dx + dz * dz));
            double wob = noise(x, z, seed) * 2.2 - 1.1;
            double rr = r + wob;
            if (d > rr + 2.2) continue;
            // Keep ponds on the island only.
            if (!inDisk(x, z, R - 4)) continue;

            if (d <= rr - 0.6) {
                // water basin: dig one block down, fill with water, mud floor
                set(es, x, SY, z, "water");
                set(es, x, SY - 1, z, "mud");
                set(es, x, SY - 2, z, "clay");
            } else if (d <= rr + 0.8) {
                // shoreline ring of sand / gravel
                double s = noise(x * 2, z * 2, seed + 7);
                set(es, x, SY, z, (s > 0.6) ? "gravel" : "sand");
                set(es, x, SY - 1, z, "dirt");
            } else {
                // damp grassy rim with reeds
                set(es, x, SY, z, "grass_block");
                double rd = noise(x, z, seed + 13);
                if (rd > 0.86) set(es, x, SY + 1, z, "short_grass");
            }
        }
    }
}

// A flattened grassy clearing with podzol heart and a ring of decorative flora.
private void buildTerrainClearing(EditSession es, int cx, int cz, double r, long seed) {
    for (int x = (int) (cx - r - 1); x <= cx + r + 1; x++) {
        for (int z = (int) (cz - r - 1); z <= cz + r + 1; z++) {
            int dx = x - cx, dz = z - cz;
            double d = Math.sqrt((double) (dx * dx + dz * dz));
            double wob = noise(x, z, seed) * 1.6 - 0.8;
            if (d > r + wob) continue;
            if (!inDisk(x, z, R - 3)) continue;

            String top = (d < r * 0.45) ? "podzol" : "grass_block";
            set(es, x, SY, z, top);
            set(es, x, SY - 1, z, "dirt");
            double deco = noise(x * 2, z * 2, seed + 21);
            if (top.equals("podzol")) {
                if (deco > 0.90) set(es, x, SY + 1, z, "red_mushroom");
                else if (deco > 0.80) set(es, x, SY + 1, z, "fern");
            } else if (deco > 0.88) {
                set(es, x, SY + 1, z, (deco > 0.95) ? "poppy" : "short_grass");
            }
        }
    }
    // A lone mossy boulder as a focal point in the clearing.
    set(es, cx, SY + 1, cz, "mossy_cobblestone");
    set(es, cx + 1, SY, cz, "mossy_cobblestone");
    set(es, cx, SY, cz + 1, "moss_block");
}

// Scatters moss, roots and hanging accents just inside the wobbled coastline.
private void buildTerrainCoastAccents(EditSession es, long seed) {
    for (int x = -R; x <= R; x++) {
        for (int z = -R; z <= R; z++) {
            double d = Math.sqrt((double) (x * x + z * z));
            double wob = noise(x * 2, z * 2, 0x10BB7_5EEDL) * 7.0 - 3.5;
            double edgeR = R + wob;
            if (d > edgeR || d < edgeR - 3.5) continue;

            double a = noise(x, z, seed);
            if (a > 0.82) {
                // mossy lip on the rim grass
                set(es, x, SY, z, (a > 0.93) ? "moss_block" : "grass_block");
                double hang = noise(x, z, seed + 5);
                if (hang > 0.7) set(es, x, SY - 2, z, "hanging_roots");
            } else if (a < 0.10) {
                // exposed stony edge
                set(es, x, SY, z, "andesite");
            }
        }
    }
}

    private void buildPlaza(EditSession es) {
    final int cx = 0, cz = 0;
    final int floorY = SY;          // grass surface / plaza floor at y=64
    final long SEED = 0x10BBL;

    // ------------------------------------------------------------------
    // 1) Base ground: a clean solid foundation across the whole ring so
    //    there are no holes under the mandala. Use polished_andesite as
    //    the structural sub-floor, then carve the decorative top on it.
    // ------------------------------------------------------------------
    for (int x = -24; x <= 24; x++) {
        for (int z = -24; z <= 24; z++) {
            int d2 = x * x + z * z;
            if (d2 < 8 * 8 || d2 > 24 * 24) continue; // only our ring 8..24
            // structural support one block below the floor
            set(es, x, floorY - 1, z, "polished_andesite");
        }
    }

    // ------------------------------------------------------------------
    // 2) Concentric decorative rings forming the mandala. Each radius
    //    band gets a base material, with noise-driven speckle variation
    //    so the floor reads as hand-laid stone rather than flat fill.
    // ------------------------------------------------------------------
    // Ordered outer->inner so inner rings can overwrite shared edges.
    buildPlazaRingBand(es, cx, cz, floorY, 22.0, 24.0, "polished_blackstone_bricks", "cracked_polished_blackstone_bricks", SEED + 1);
    buildPlazaRingBand(es, cx, cz, floorY, 20.0, 22.0, "smooth_quartz", "quartz_block", SEED + 2);
    buildPlazaRingBand(es, cx, cz, floorY, 18.0, 20.0, "polished_andesite", "andesite", SEED + 3);
    buildPlazaRingBand(es, cx, cz, floorY, 16.0, 18.0, "polished_blackstone_bricks", "chiseled_polished_blackstone", SEED + 4);
    buildPlazaRingBand(es, cx, cz, floorY, 14.0, 16.0, "smooth_quartz", "chiseled_quartz_block", SEED + 5);
    buildPlazaRingBand(es, cx, cz, floorY, 12.0, 14.0, "polished_andesite", "polished_diorite", SEED + 6);
    buildPlazaRingBand(es, cx, cz, floorY, 10.0, 12.0, "polished_blackstone_bricks", "cracked_polished_blackstone_bricks", SEED + 7);

    // ------------------------------------------------------------------
    // 3) Thin accent ring lines — crisp single-block borders between the
    //    bands give the mandala definition.
    // ------------------------------------------------------------------
    ring(es, cx, floorY, cz, 21.5, 22.0, "chiseled_polished_blackstone");
    ring(es, cx, floorY, cz, 17.5, 18.0, "chiseled_polished_blackstone");
    ring(es, cx, floorY, cz, 13.5, 14.0, "chiseled_polished_blackstone");
    ring(es, cx, floorY, cz, 19.5, 20.0, "smooth_quartz");
    ring(es, cx, floorY, cz, 15.5, 16.0, "smooth_quartz");

    // ------------------------------------------------------------------
    // 4) Sea_lantern accents set flush in the floor as a dotted ring,
    //    plus a sparse scatter so light bleeds up through the stone.
    // ------------------------------------------------------------------
    buildPlazaLanternDots(es, cx, cz, floorY, 21.0, 16, 0.0);
    buildPlazaLanternDots(es, cx, cz, floorY, 13.0, 12, Math.PI / 12.0);
    // sparse flush sea_lantern speckle in the mid bands
    for (int x = -22; x <= 22; x++) {
        for (int z = -22; z <= 22; z++) {
            int d2 = x * x + z * z;
            if (d2 < 11 * 11 || d2 > 22 * 22) continue;
            double n = noise(x, z, SEED + 42);
            if (n > 0.965) set(es, x, floorY, z, "sea_lantern");
        }
    }

    // ------------------------------------------------------------------
    // 5) Four cardinal inlays — diamond/petal motifs along N/S/E/W axes,
    //    radiating spokes that tie the rings together.
    // ------------------------------------------------------------------
    buildPlazaSpoke(es, cx, cz, floorY, 0, 1, SEED + 10);   // north (+z)
    buildPlazaSpoke(es, cx, cz, floorY, 0, -1, SEED + 11);  // south (-z)
    buildPlazaSpoke(es, cx, cz, floorY, 1, 0, SEED + 12);   // east  (+x)
    buildPlazaSpoke(es, cx, cz, floorY, -1, 0, SEED + 13);  // west  (-x)

    // diagonal minor inlays in smooth_quartz to break the symmetry softly
    buildPlazaDiagonal(es, cx, cz, floorY, 1, 1, SEED + 20);
    buildPlazaDiagonal(es, cx, cz, floorY, 1, -1, SEED + 21);
    buildPlazaDiagonal(es, cx, cz, floorY, -1, 1, SEED + 22);
    buildPlazaDiagonal(es, cx, cz, floorY, -1, -1, SEED + 23);

    // ------------------------------------------------------------------
    // 6) Low circular fountain rim around the inner edge of our region
    //    (radius ~9), with water inside, a stepped lip and lanterns.
    // ------------------------------------------------------------------
    buildPlazaFountainRim(es, cx, cz, floorY);
}

// One radial band of the mandala floor with noise-driven material variation.
private void buildPlazaRingBand(EditSession es, int cx, int cz, int y,
                                double inner, double outer,
                                String base, String accent, long seed) {
    int ri = (int) Math.ceil(outer) + 1;
    double in2 = inner * inner;
    double out2 = outer * outer;
    for (int x = -ri; x <= ri; x++) {
        for (int z = -ri; z <= ri; z++) {
            double d2 = x * x + z * z;
            if (d2 < in2 || d2 > out2) continue;
            double n = noise(cx + x, cz + z, seed);
            String id = base;
            if (n > 0.82) id = accent;
            else if (n < 0.07) id = "chiseled_polished_blackstone";
            set(es, cx + x, y, cz + z, id);
            // support directly under each placed tile
            set(es, cx + x, y - 1, cz + z, "polished_andesite");
        }
    }
}

// Evenly-spaced flush sea_lantern dots around a ring, with a small
// polished_blackstone frame around each for contrast.
private void buildPlazaLanternDots(EditSession es, int cx, int cz, int y,
                                   double radius, int count, double phase) {
    for (int i = 0; i < count; i++) {
        double a = phase + (2.0 * Math.PI * i) / count;
        int x = cx + (int) Math.round(Math.cos(a) * radius);
        int z = cz + (int) Math.round(Math.sin(a) * radius);
        set(es, x, y, z, "sea_lantern");
        // four-way frame in chiseled blackstone
        set(es, x + 1, y, z, "polished_blackstone_bricks");
        set(es, x - 1, y, z, "polished_blackstone_bricks");
        set(es, x, y, z + 1, "polished_blackstone_bricks");
        set(es, x, y, z - 1, "polished_blackstone_bricks");
        // a small standing lantern accent above some dots
        if (i % 2 == 0) set(es, x, y + 1, z, "lantern");
    }
}

// A cardinal spoke: a quartz diamond-spine running outward along an
// axis, dotted with sea_lantern gems and blackstone trim.
private void buildPlazaSpoke(EditSession es, int cx, int cz, int y,
                             int dx, int dz, long seed) {
    for (int r = 9; r <= 24; r++) {
        int x = cx + dx * r;
        int z = cz + dz * r;
        // central spine
        set(es, x, y, z, "smooth_quartz");
        // widen into a diamond petal in the mid section
        int half;
        if (r < 12) half = 0;
        else if (r < 16) half = 1;
        else if (r < 20) half = 2;
        else half = 1;
        // perpendicular direction
        int px = dz, pz = dx;
        for (int w = 1; w <= half; w++) {
            String id = (w == half) ? "polished_blackstone_bricks" : "smooth_quartz";
            set(es, x + px * w, y, z + pz * w, id);
            set(es, x - px * w, y, z - pz * w, id);
        }
        // sea_lantern gems at petal centers
        if (r == 14 || r == 18 || r == 22) {
            set(es, x, y, z, "sea_lantern");
        }
        // noise speckle of chiseled stone along the spine
        if (noise(x, z, seed) > 0.88) set(es, x, y, z, "chiseled_polished_blackstone");
    }
    // a small lantern beacon near the outer tip of each spoke
    int tx = cx + dx * 23, tz = cz + dz * 23;
    set(es, tx, y + 1, tz, "lantern");
}

// Subtle diagonal inlay lines connecting the rings on 45-degree axes.
private void buildPlazaDiagonal(EditSession es, int cx, int cz, int y,
                                int sx, int sz, long seed) {
    for (int r = 9; r <= 23; r++) {
        // walk roughly along the diagonal using integer scaling
        int x = cx + (int) Math.round(sx * r * 0.7071);
        int z = cz + (int) Math.round(sz * r * 0.7071);
        double n = noise(x, z, seed);
        String id = (n > 0.6) ? "polished_andesite" : "smooth_quartz";
        if (r % 4 == 0) id = "chiseled_polished_blackstone";
        set(es, x, y, z, id);
        if (r == 16) set(es, x, y, z, "sea_lantern");
    }
}

// Low circular fountain rim at the inner boundary of the region.
// Inner radius ~8 = water basin; rim at ~8.5-9.5 with a raised lip.
private void buildPlazaFountainRim(EditSession es, int cx, int cz, int y) {
    double rimInner = 8.5;
    double rimOuter = 9.7;

    // basin floor & water: fill the inner disk one block deep so water
    // sits flush with surrounding plaza. (Inner of our region edge.)
    int br = 9;
    for (int x = -br; x <= br; x++) {
        for (int z = -br; z <= br; z++) {
            double d2 = x * x + z * z;
            if (d2 > 8.3 * 8.3) continue;
            // dark basin bottom
            set(es, cx + x, y - 1, cz + z, "polished_blackstone");
            // water pool sitting in the recess
            set(es, cx + x, y, cz + z, "water");
        }
    }

    // the raised stone rim: a low wall ring one block high around the pool
    ring(es, cx, y, cz, rimInner, rimOuter, "polished_blackstone_bricks");
    ring(es, cx, y + 1, cz, rimInner, rimOuter, "polished_blackstone_bricks");
    // smooth_quartz capstone on top of the rim for a clean lip
    ring(es, cx, y + 2, cz, rimInner, rimOuter, "smooth_quartz");

    // chiseled accent studs + lanterns evenly around the rim crown
    int posts = 12;
    double rimMid = (rimInner + rimOuter) / 2.0;
    for (int i = 0; i < posts; i++) {
        double a = (2.0 * Math.PI * i) / posts;
        int x = cx + (int) Math.round(Math.cos(a) * rimMid);
        int z = cz + (int) Math.round(Math.sin(a) * rimMid);
        set(es, x, y + 2, z, "chiseled_polished_blackstone");
        if (i % 2 == 0) {
            // a short post with a hanging-style lantern crown
            set(es, x, y + 3, z, "polished_blackstone_brick_wall");
            set(es, x, y + 4, z, "sea_lantern");
            set(es, x, y + 5, z, "chain[axis=y]");
            set(es, x, y + 6, z, "lantern[hanging=true]");
        } else {
            set(es, x, y + 3, z, "lantern");
        }
    }

    // sea_lantern accents set flush in the rim crown between posts
    for (int i = 0; i < posts; i++) {
        double a = (2.0 * Math.PI * (i + 0.5)) / posts;
        int x = cx + (int) Math.round(Math.cos(a) * rimMid);
        int z = cz + (int) Math.round(Math.sin(a) * rimMid);
        set(es, x, y + 2, z, "sea_lantern");
    }

    // inner water-edge trim: a single quartz ring just inside the rim
    ring(es, cx, y, cz, 7.6, 8.3, "smooth_quartz");
}

    private void buildGrandTreeTrunk(EditSession es) {
    final int cx = 0, cz = 0;
    final long S = 0x5EEDC0FFEEL;

    // ---- 0. Reinforce the ground plate right under the tree so roots have something to bite into ----
    for (int dx = -10; dx <= 10; dx++) {
        for (int dz = -10; dz <= 10; dz++) {
            if (!inDisk(dx, dz, 10)) continue;
            double n = noise(cx + dx, cz + dz, S ^ 0x11);
            // a shallow mound of dirt/coarse dirt bulging up around the base
            int rim = (int) Math.round(1.5 - 0.18 * Math.sqrt(dx * dx + dz * dz) + n * 1.2);
            if (rim > 0) {
                String top = n < 0.30 ? "podzol" : (n < 0.62 ? "grass_block" : "coarse_dirt");
                for (int yy = 1; yy <= rim; yy++) {
                    set(es, cx + dx, SY + yy, cz + dz, yy == rim ? top : "dirt");
                }
            }
        }
    }

    // ---- 1. Flared buttress roots diving out in 8 directions ----
    // Each root sweeps from high on the trunk base down and out into the ground mound.
    double[] dirs = {0, 45, 90, 135, 180, 225, 270, 315};
    for (int i = 0; i < dirs.length; i++) {
        double a = Math.toRadians(dirs[i]);
        double ca = Math.cos(a), sa = Math.sin(a);
        // root anchor on trunk
        int ax = (int) Math.round(ca * 2.0);
        int az = (int) Math.round(sa * 2.0);
        int ay = SY + 7;
        // root tip flung out into the mound and below the surface
        int tx = (int) Math.round(ca * 9.5);
        int tz = (int) Math.round(sa * 9.5);
        int ty = SY - 2;
        // thick flared root
        thickLine(es, cx + ax, ay, cz + az, cx + tx, ty, cz + tz, 1.9, "oak_log[axis=y]");
        // a knee bump partway (organic)
        int kx = (int) Math.round(ca * 5.5);
        int kz = (int) Math.round(sa * 5.5);
        thickLine(es, cx + kx, SY + 4, cz + kz, cx + kx, SY + 1, cz + kz, 1.4, "oak_wood");
        // little secondary fork
        double a2 = a + Math.toRadians(18);
        int fx = (int) Math.round(Math.cos(a2) * 8.0);
        int fz = (int) Math.round(Math.sin(a2) * 8.0);
        thickLine(es, cx + kx, SY + 3, cz + kz, cx + fx, SY - 1, cz + fz, 1.1, "oak_log[axis=y]");
    }

    // ---- 2. Main tapering trunk, y 64..96, radius ~4 down to ~2, with bark noise ----
    int yBase = SY;            // 64
    int yTop = SY + 32;        // 96
    for (int y = yBase; y <= yTop; y++) {
        double t = (double) (y - yBase) / (yTop - yBase); // 0..1 up the trunk
        double r = 4.2 - 2.2 * t;                          // taper 4.2 -> 2.0
        // gentle organic sway of the trunk centerline
        double swayX = 1.4 * Math.sin(t * Math.PI * 1.3);
        double swayZ = 1.1 * Math.cos(t * Math.PI * 1.1);
        int tcx = cx + (int) Math.round(swayX);
        int tcz = cz + (int) Math.round(swayZ);
        int ir = (int) Math.ceil(r) + 1;
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                double wobble = (noise(tcx + dx, tcz + dz + y, S ^ 0x77) - 0.5) * 1.1;
                if (dist > r + wobble) continue;
                int wx = tcx + dx, wz = tcz + dz;
                double bn = noise(wx * 3, wz * 3 + y * 2, S ^ 0xBEE);
                String wood;
                if (bn < 0.18) wood = "oak_wood";                 // bark patches
                else if (bn > 0.90) wood = "stripped_oak_log[axis=y]"; // weathered streaks
                else wood = "oak_log[axis=y]";
                set(es, wx, y, wz, wood);
            }
        }
    }

    // ---- 3. Hollow out the heart of the trunk lower down (a cozy interior) ----
    for (int y = SY + 1; y <= SY + 14; y++) {
        double t = (double) (y - SY) / 14.0;
        double hr = 2.3 - 1.0 * t; // hollow shrinks as it rises
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) <= hr) {
                    set(es, cx + dx, y, cz + dz, "air");
                }
            }
        }
    }
    // doorway hollow facing south
    fill(es, cx - 1, SY + 1, cz + 3, cx + 1, SY + 4, cz + 5, "air");
    // warm interior floor + glow
    disk(es, cx, SY, cz, 2.4, "stripped_oak_wood");
    set(es, cx, SY + 1, cz, "lantern");
    set(es, cx - 2, SY + 5, cz, "shroomlight");
    set(es, cx + 2, SY + 5, cz, "shroomlight");

    // ---- 4. Buttress webbing between roots at the very base (skirt fins) ----
    for (int i = 0; i < dirs.length; i++) {
        double a = Math.toRadians(dirs[i] + 22.5);
        double ca = Math.cos(a), sa = Math.sin(a);
        for (int step = 3; step <= 7; step++) {
            int fx = cx + (int) Math.round(ca * step);
            int fz = cz + (int) Math.round(sa * step);
            int h = (int) Math.round(4.5 - step * 0.45 + noise(fx, fz, S ^ 0x33) * 1.5);
            for (int yy = 1; yy <= h; yy++) {
                set(es, fx, SY + yy, fz, "oak_wood");
            }
        }
    }

    // ---- 5. Bee nest tucked into the trunk facing east, around mid-height ----
    int bnx = cx + 3, bny = SY + 18, bnz = cz;
    set(es, bnx, bny, bnz, "bee_nest[facing=east,honey_level=5]");
    // a few honey drips and supporting comb feel
    set(es, bnx, bny - 1, bnz, "honey_block");
    set(es, bnx + 1, bny, bnz, "oak_leaves[persistent=true]");
    set(es, bnx, bny, bnz + 1, "oak_leaves[persistent=true]");
    set(es, bnx, bny, bnz - 1, "oak_leaves[persistent=true]");

    // ---- 6. Knots / hollows scattered up the trunk (dark eyes) ----
    int[][] knots = {{-4, SY + 10, 1}, {3, SY + 22, -2}, {-2, SY + 26, 3}, {4, SY + 14, 2}};
    for (int[] k : knots) {
        set(es, cx + k[0], k[1], cz + k[2], "oak_leaves[persistent=true]");
        set(es, cx + k[0], k[1] - 1, cz + k[2], "spore_blossom");
    }

    // ---- 7. Mossy / azalea / fungal detailing around the base mound ----
    for (int dx = -10; dx <= 10; dx++) {
        for (int dz = -10; dz <= 10; dz++) {
            if (!inDisk(dx, dz, 10)) continue;
            int wx = cx + dx, wz = cz + dz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double n = noise(wx, wz, S ^ 0xA5A5);
            if (dist < 3.0) continue; // keep clear near hollow entrance core
            // find approximate surface y from the mound logic
            int surf = SY + Math.max(0, (int) Math.round(1.5 - 0.18 * dist + noise(wx, wz, S ^ 0x11) * 1.2));
            // moss carpet patches
            if (n > 0.55 && n < 0.72) {
                set(es, wx, surf, wz, "moss_block");
                if (noise(wx, wz, S ^ 0x99) > 0.6) set(es, wx, surf + 1, wz, "moss_carpet");
            }
            // azalea / flowering azalea bushes
            if (n >= 0.72 && n < 0.80) {
                set(es, wx, surf + 1, wz, noise(wx, wz, S ^ 0xC4) > 0.5 ? "flowering_azalea" : "azalea");
            }
            // mushrooms and sprigs
            if (n >= 0.80 && n < 0.86) {
                String[] sprigs = {"red_mushroom", "brown_mushroom", "fern", "short_grass", "oak_sapling"};
                int idx = (int) (noise(wx * 2, wz * 2, S ^ 0xD3) * sprigs.length);
                if (idx >= sprigs.length) idx = sprigs.length - 1;
                set(es, wx, surf + 1, wz, sprigs[idx]);
            }
            // glow-lichen-like accents on exposed root wood (lanterns sparsely)
            if (n >= 0.96 && dist > 5.0) {
                set(es, wx, surf + 1, wz, "lantern");
            }
        }
    }

    // ---- 8. Hanging vines & a couple of climbing chains for atmosphere ----
    for (int i = 0; i < 6; i++) {
        double a = Math.toRadians(i * 60 + 15);
        int vx = cx + (int) Math.round(Math.cos(a) * 3.0);
        int vz = cz + (int) Math.round(Math.sin(a) * 3.0);
        int len = 3 + (int) (noise(vx, vz, S ^ 0xE7) * 4);
        for (int j = 0; j < len; j++) {
            set(es, vx, SY + 20 - j, vz, "vine[up=true]");
        }
    }
    // a lantern chain dangling inside the hollow doorway
    set(es, cx, SY + 5, cz + 4, "chain[axis=y]");
    set(es, cx, SY + 4, cz + 4, "lantern[hanging=true]");

    // ---- 9. Crown transition: where trunk meets canopy region above (top y~96) ----
    // splay a few thick limbs outward so the canopy region has anchors to grab
    double[] limbDirs = {30, 110, 200, 290};
    for (double ld : limbDirs) {
        double a = Math.toRadians(ld);
        int lx = cx + (int) Math.round(Math.cos(a) * 6.0);
        int lz = cz + (int) Math.round(Math.sin(a) * 6.0);
        thickLine(es, cx, SY + 28, cz, lx, SY + 32, lz, 1.6, "oak_log[axis=y]");
        // a tuft of leaves at each limb tip to soften the top edge of this region
        sphere(es, lx, SY + 32, lz, 2.2, "oak_leaves[persistent=true]");
    }
    // small leaf cap at trunk top so the seam into the canopy region reads organically
    sphere(es, cx, SY + 32, cz, 3.0, "oak_leaves[persistent=true]");
}

    private void buildGrandTreeBranches(EditSession es) {
    // Center of the great tree trunk top, where the major limbs split off.
    final int cx = 0, cz = 0;
    final long SEED = 982451653L;

    // Reinforce the trunk core through this slice so branches grow from solid wood.
    for (int y = 80; y <= 92; y++) {
        double rad = 4.6 - (y - 80) * 0.16;
        if (rad < 2.2) rad = 2.2;
        disk(es, cx, y, cz, rad, "oak_wood");
    }
    // A knot of darker bark where limbs fan out.
    for (int y = 86; y <= 90; y++) {
        ring(es, cx, y, cz, 3.0, 4.4, "stripped_oak_wood");
    }

    // ---- Major limbs: 12 thick branches spiraling outward and upward. ----
    final int LIMBS = 12;
    for (int i = 0; i < LIMBS; i++) {
        double baseAngle = (Math.PI * 2.0 / LIMBS) * i;
        // Spiral: angle grows with height so limbs twist as they rise.
        double n0 = noise(i * 37, i * 53, SEED);
        double n1 = noise(i * 71, i * 13, SEED + 7);
        double n2 = noise(i * 17, i * 91, SEED + 19);

        int startY = 84 + (int) Math.round(n0 * 5.0);   // 84..89 staggered origins
        double startR = 2.4 + n1 * 1.2;
        int sx = cx + (int) Math.round(Math.cos(baseAngle) * startR);
        int sz = cz + (int) Math.round(Math.sin(baseAngle) * startR);
        int sy = startY;

        // The limb is built in 3 segments that arc upward and outward, twisting.
        double radius = 2.6 + n2 * 0.8;            // base thickness 2.6..3.4
        double angle = baseAngle;
        int px = sx, py = sy, pz = sz;
        int dist = 0;

        int segs = 3;
        for (int s = 0; s < segs; s++) {
            // Twist the heading a bit each segment (spiral).
            double twist = 0.45 + noise(i * 23 + s * 5, s * 29, SEED + 31) * 0.55;
            angle += twist;
            double horiz = 9.0 + noise(i * 11 + s, s * 7, SEED + 41) * 5.0; // reach per seg
            int rise = 4 + (int) Math.round(noise(i * 9 + s * 3, s, SEED + 53) * 5.0);

            int ex = px + (int) Math.round(Math.cos(angle) * horiz);
            int ez = pz + (int) Math.round(Math.sin(angle) * horiz);
            int ey = py + rise;
            if (ey > 101) ey = 101;

            double r0 = radius;
            double r1 = radius * (0.62 - s * 0.06); // taper outward
            if (r1 < 1.0) r1 = 1.0;
            // thickLine takes a single radius; approximate taper by midpoint split.
            int mx = (px + ex) / 2, my = (py + ey) / 2, mz = (pz + ez) / 2;
            thickLine(es, px, py, pz, mx, my, mz, r0, "oak_wood");
            thickLine(es, mx, my, mz, ex, ey, ez, r1, "oak_wood");

            // Bark variation: speckle stripped wood along the upper segments.
            if (s >= 1 && noise(ex * 3, ez * 3, SEED + 61) > 0.55) {
                thickLine(es, mx, my, mz, ex, ey, ez, Math.max(0.8, r1 - 0.8), "stripped_oak_wood");
            }

            radius = r1;
            px = ex; py = ey; pz = ez;
            dist += (int) horiz;

            // ---- Secondary twigs branching off the joints. ----
            if (s >= 1) {
                int twigs = 2 + (int) Math.round(noise(i + s, s * 13, SEED + 67) * 2.0);
                for (int t = 0; t < twigs; t++) {
                    double ta = angle + (t - twigs / 2.0) * 0.8
                            + noise(i * 5 + t, s * 3 + t, SEED + 73) * 0.6;
                    double tl = 4.0 + noise(t * 9, s * 5 + i, SEED + 79) * 4.0;
                    int tx = px + (int) Math.round(Math.cos(ta) * tl);
                    int tz = pz + (int) Math.round(Math.sin(ta) * tl);
                    int ty = py + 1 + (int) Math.round(noise(t * 3, i + s, SEED + 83) * 3.0);
                    if (ty > 102) ty = 102;
                    double tr = Math.max(0.8, radius * 0.55);
                    thickLine(es, px, py, pz, tx, ty, tz, tr, "oak_wood");

                    // Leaf cluster at twig tip.
                    double lr = 2.2 + noise(tx, tz, SEED + 89) * 1.4;
                    sphere(es, tx, ty, tz, lr, "oak_leaves[persistent=true]");
                    // Hollow a touch of variation: sprinkle flowering leaves.
                    if (noise(tx * 2, tz * 2, SEED + 97) > 0.6) {
                        sphere(es, tx, ty, tz, lr - 1.0, "flowering_azalea_leaves[persistent=true]");
                    }
                    // A hanging lantern under some leaf clusters.
                    if (noise(tx, tz * 3, SEED + 101) > 0.7 && ty - 2 >= 80) {
                        set(es, tx, ty - 2, tz, "chain[axis=y]");
                        set(es, tx, ty - 3, tz, "lantern[hanging=true]");
                    }
                }
            }
        }

        // ---- Branch tip: a generous leaf canopy with layered colors. ----
        int tipx = px, tipy = py, tipz = pz;
        double canopy = 3.2 + noise(tipx, tipz, SEED + 103) * 1.8;
        sphere(es, tipx, tipy, tipz, canopy, "oak_leaves[persistent=true]");
        sphere(es, tipx, tipy + 1, tipz, canopy * 0.7, "oak_leaves[persistent=true]");
        // Inner accent layer for depth.
        sphere(es, tipx, tipy, tipz, canopy - 1.4, "azalea_leaves[persistent=true]");
        // Glow core hidden inside dense canopies.
        if (canopy > 4.0) set(es, tipx, tipy, tipz, "sea_lantern");

        // Glowing fruit / berries scattered on the canopy surface.
        scatter(es, tipx, tipy + (int) canopy, tipz, canopy * 0.8, 0.18,
                SEED + i * 13L, "oak_leaves[persistent=true]", "glow_lichen");

        // ---- Two of the strongest, lowest limbs carry small platforms. ----
        if (i == 0 || i == 6) {
            buildGrandTreeBranchesPlatform(es, tipx, tipy, tipz, SEED + i * 211L);
        }
    }

    // A few extra delicate high twigs from the very crown for silhouette detail.
    for (int k = 0; k < 6; k++) {
        double a = (Math.PI * 2.0 / 6) * k + 0.3;
        double n = noise(k * 41, k * 67, SEED + 131);
        int ex = cx + (int) Math.round(Math.cos(a) * (5 + n * 4));
        int ez = cz + (int) Math.round(Math.sin(a) * (5 + n * 4));
        int ey = 95 + (int) Math.round(n * 6);
        if (ey > 102) ey = 102;
        thickLine(es, cx, 90, cz, ex, ey, ez, 1.2, "oak_wood");
        sphere(es, ex, ey, ez, 2.0 + n, "oak_leaves[persistent=true]");
    }
}

private void buildGrandTreeBranchesPlatform(EditSession es, int cx, int cy, int cz, long seed) {
    // A small wooden viewing platform cradled in a thick branch fork.
    int py = cy - 2;
    if (py < 80) py = 80;
    double pr = 4.0;

    // Support brackets of wood beneath.
    for (int a = 0; a < 4; a++) {
        double ang = Math.PI / 2 * a;
        int bx = cx + (int) Math.round(Math.cos(ang) * pr);
        int bz = cz + (int) Math.round(Math.sin(ang) * pr);
        thickLine(es, cx, py - 3, cz, bx, py, bz, 1.0, "oak_wood");
    }

    // Plank deck (disk of slabs/planks) with a trimmed edge.
    disk(es, cx, py, cz, pr, "oak_planks");
    ring(es, cx, py, cz, pr - 0.8, pr + 0.2, "stripped_oak_wood");

    // Fence railing around the rim.
    int steps = 16;
    for (int s = 0; s < steps; s++) {
        double ang = (Math.PI * 2.0 / steps) * s;
        int rx = cx + (int) Math.round(Math.cos(ang) * (pr - 0.3));
        int rz = cz + (int) Math.round(Math.sin(ang) * (pr - 0.3));
        set(es, rx, py + 1, rz, "oak_fence");
        // Lanterns on every fourth post.
        if (s % 4 == 0) set(es, rx, py + 2, rz, "lantern[hanging=false]");
    }

    // A small leafy parasol arching over the platform for shade.
    for (int y = py + 2; y <= py + 4; y++) {
        double lr = pr - (y - (py + 2)) * 1.2;
        if (lr > 1.0) disk(es, cx, y, cz, lr, "oak_leaves[persistent=true]");
    }
    set(es, cx, py + 1, cz, "sea_lantern");

    // A scattering of decorative blocks for warmth and variation.
    scatter(es, cx, py + 1, cz, pr - 1.5, 0.25, seed,
            "oak_planks", "moss_block", "potted_oak_sapling");
}

    private void buildGrandTreeCanopy(EditSession es) {
    // ============================================================
    // GRAND TREE CANOPY  (region y 86..110, centered at 0,*,0)
    // Multi-blob overlapping leaf canopy with glow pockets,
    // hanging chains+lanterns, draping vines, and treehouses.
    // ============================================================
    final int yLo = 86, yHi = 110;
    final long SEED = 0x5EED_CA50L; // base seed for noise variation

    // ----- 1) Big overlapping canopy blobs --------------------
    // Each blob: center x,z,y and radius. Generated in a rough
    // cluster so the silhouette reads as one enormous canopy.
    int[][] blobs = {
        //  cx,  cy,  cz,  r
        {   0,  98,   0, 26 },
        {  20,  96,  10, 18 },
        { -22,  95, -8, 19 },
        {  10,  94, -22, 17 },
        { -14,  97,  20, 18 },
        {  28,  92, -14, 14 },
        { -30,  93,  12, 15 },
        {   6, 102,   6, 16 },
        { -8, 101, -10, 15 },
        {  18, 100,  22, 13 },
        { -24, 100, -22, 13 },
        {  34,  95,   6, 11 },
        { -34,  94,  -4, 11 },
        {   2,  90, -30, 12 },
        {  -4,  91,  30, 12 },
    };

    // Lay down the base leaf masses as oak leaves.
    for (int[] b : blobs) {
        sphere(es, b[0], b[1], b[2], b[3], "oak_leaves[persistent=true]");
    }

    // ----- 2) Azalea accent shells over noise-selected blobs ---
    // Wrap a thin outer shell of azalea / flowering azalea leaves
    // on some blobs to give color variation on the surface.
    for (int i = 0; i < blobs.length; i++) {
        int[] b = blobs[i];
        double n = noise(b[0] * 7 + i, b[2] * 5 - i, SEED + 11);
        if (n > 0.55) {
            String acc = (n > 0.78)
                ? "flowering_azalea_leaves[persistent=true]"
                : "azalea_leaves[persistent=true]";
            hollowSphere(es, b[0], b[1], b[2], b[3] + 0.4, 1.4, acc);
        }
    }

    // ----- 3) Organic surface scribble of azalea patches -------
    // Scatter small azalea clumps across the canopy crown so the
    // accent does not look like clean shells.
    for (int[] b : blobs) {
        for (int k = 0; k < 6; k++) {
            double a = noise(b[0] + k * 13, b[2] - k * 7, SEED + 23) * Math.PI * 2.0;
            double rr = b[3] * (0.55 + 0.4 * noise(b[2] + k, b[0] - k, SEED + 31));
            int px = b[0] + (int) Math.round(Math.cos(a) * rr);
            int pz = b[2] + (int) Math.round(Math.sin(a) * rr);
            int py = b[1] + (int) Math.round((noise(px, pz, SEED + 41) - 0.4) * b[3] * 0.6);
            if (py < yLo || py > yHi) continue;
            String acc = (noise(px, pz, SEED + 53) > 0.5)
                ? "flowering_azalea_leaves[persistent=true]"
                : "azalea_leaves[persistent=true]";
            sphere(es, px, py, pz, 2.0 + noise(px, pz, SEED + 61) * 1.5, acc);
        }
    }

    // ----- 4) Glow pockets buried inside the canopy ------------
    // Carve small glowing cores so light leaks through the leaves.
    int[][] glow = {
        {   0,  98,   0 }, {  18,  95,   9 }, { -20,  95, -6 },
        {   9,  93, -19 }, { -12,  96,  18 }, {   5, 101,   5 },
        {  -7, 100, -9 }, {  30,  93,   4 }, { -31,  93,  -2 },
        {  16,  99,  20 }, { -22,  99, -20 }, {   1,  89, -27 },
    };
    for (int i = 0; i < glow.length; i++) {
        int[] g = glow[i];
        String core = (noise(g[0], g[2], SEED + 71) > 0.5) ? "shroomlight" : "glowstone";
        sphere(es, g[0], g[1], g[2], 1.6, core);
        // soft halo of leaves kept so the glow stays diffuse
        if (noise(g[0] + 3, g[2] - 3, SEED + 73) > 0.4) {
            set(es, g[0], g[1] + 2, g[2], "shroomlight");
        }
    }

    // ----- 5) Hanging chains + lanterns under glow pockets -----
    for (int i = 0; i < glow.length; i++) {
        int[] g = glow[i];
        if (noise(g[0] - 5, g[2] + 5, SEED + 81) < 0.45) continue;
        int top = g[1] - 2;
        int len = 2 + (int) Math.round(noise(g[0], g[2], SEED + 83) * 4);
        int bot = top - len;
        if (bot < yLo) bot = yLo;
        for (int y = bot + 1; y <= top; y++) {
            set(es, g[0], y, g[2], "chain[axis=y]");
        }
        set(es, g[0], bot, g[2], "lantern[hanging=true]");
    }

    // ----- 6) Vine strands draping down off the underside ------
    buildGrandTreeCanopyVines(es, blobs, yLo, SEED);

    // ----- 7) Branch armature linking the lower blobs ---------
    // A few oak_log branches that thread through the canopy and
    // give the treehouses something to "sit" on.
    int[][] branchPairs = {
        {   0,  90,   0,  20,  88,  10 },
        {   0,  90,   0, -22,  88,  -8 },
        {   0,  90,   0,  10,  88, -22 },
        {   0,  90,   0, -14,  90,  20 },
        {  20,  88,  10,  30,  87,   2 },
        { -22,  88,  -8, -30,  87,   6 },
    };
    for (int[] p : branchPairs) {
        thickLine(es, p[0], p[1], p[2], p[3], p[4], p[5], 1.4, "oak_log[axis=y]");
    }

    // ----- 8) Treehouse platforms among the branches ----------
    buildGrandTreeCanopyTreehouse(es, 20, 89, 10, 5, 4, SEED + 101);
    buildGrandTreeCanopyTreehouse(es, -22, 89, -8, 4, 4, SEED + 211);
    buildGrandTreeCanopyTreehouse(es, -10, 92, 20, 4, 3, SEED + 307);

    // ----- 9) Final crown sparkle of flowering azalea ---------
    // Topmost dusting so the canopy peak catches the eye.
    for (int x = -36; x <= 36; x += 3) {
        for (int z = -36; z <= 36; z += 3) {
            if (!inDisk(x, z, 36)) continue;
            double n = noise(x, z, SEED + 401);
            int peak = yHi - (int) Math.round(n * 6);
            if (n > 0.72 && peak >= yLo && peak <= yHi) {
                set(es, x, peak, z, "flowering_azalea_leaves[persistent=true]");
            }
        }
    }
}

// Drape vine strands down from the lowest leaf masses.
private void buildGrandTreeCanopyVines(EditSession es, int[][] blobs, int yLo, long seed) {
    for (int[] b : blobs) {
        // sample points around the lower hemisphere edge of each blob
        int strands = 5 + (int) Math.round(noise(b[0], b[2], seed + 5) * 4);
        for (int s = 0; s < strands; s++) {
            double a = noise(b[0] + s * 17, b[2] - s * 9, seed + 9) * Math.PI * 2.0;
            double rr = b[3] * (0.7 + 0.25 * noise(b[2] - s, b[0] + s, seed + 13));
            int px = b[0] + (int) Math.round(Math.cos(a) * rr);
            int pz = b[2] + (int) Math.round(Math.sin(a) * rr);
            int top = b[1] - (int) Math.round(b[3] * 0.6);
            int len = 3 + (int) Math.round(noise(px, pz, seed + 17) * 6);
            int bot = top - len;
            if (bot < yLo) bot = yLo;
            // pick a side face for the vine so it stays vertical
            double fa = noise(px - 3, pz + 3, seed + 19);
            String face = (fa < 0.25) ? "vine[north=true]"
                        : (fa < 0.5)  ? "vine[south=true]"
                        : (fa < 0.75) ? "vine[east=true]"
                        :               "vine[west=true]";
            for (int y = bot; y <= top; y++) {
                set(es, px, y, pz, face);
            }
        }
    }
}

// Small wooden treehouse platform with fence railings + a lantern.
private void buildGrandTreeCanopyTreehouse(EditSession es, int cx, int cy, int cz,
                                           int halfX, int halfZ, long seed) {
    int x1 = cx - halfX, x2 = cx + halfX;
    int z1 = cz - halfZ, z2 = cz + halfZ;
    int floor = cy;

    // Floor planks with a touch of noise-driven slab/plank mix.
    for (int x = x1; x <= x2; x++) {
        for (int z = z1; z <= z2; z++) {
            String mat = (noise(x, z, seed + 1) > 0.5)
                ? "oak_planks" : "spruce_planks";
            set(es, x, floor, z, mat);
        }
    }

    // Support post down through the canopy from one corner.
    int postX = (noise(cx, cz, seed + 2) > 0.5) ? x1 : x2;
    int postZ = (noise(cz, cx, seed + 3) > 0.5) ? z1 : z2;
    column(es, postX, postZ, floor - 5, floor - 1, "oak_log[axis=y]");

    // Fence railing around the perimeter (leave one gap as access).
    int gapX = cx; // open the railing near center of one edge
    for (int x = x1; x <= x2; x++) {
        if (x != gapX) {
            set(es, x, floor + 1, z1, "oak_fence");
            set(es, x, floor + 1, z2, "oak_fence");
        }
    }
    for (int z = z1; z <= z2; z++) {
        set(es, x1, floor + 1, z, "oak_fence");
        set(es, x2, floor + 1, z, "oak_fence");
    }

    // Corner lantern posts on top of the fence corners.
    set(es, x1, floor + 2, z1, "oak_fence");
    set(es, x2, floor + 2, z2, "oak_fence");
    set(es, x1, floor + 3, z1, "lantern[hanging=false]");
    set(es, x2, floor + 3, z2, "lantern[hanging=false]");

    // A cozy centerpiece: lantern hanging under a small canopy nub.
    set(es, cx, floor + 4, cz, "oak_leaves[persistent=true]");
    set(es, cx, floor + 3, cz, "chain[axis=y]");
    set(es, cx, floor + 2, cz, "lantern[hanging=true]");

    // Scatter a couple of azalea pots / accents on the deck.
    if (noise(cx + 7, cz - 7, seed + 4) > 0.4) {
        set(es, x1 + 1, floor + 1, z2 - 1, "flowering_azalea_leaves[persistent=true]");
    }
    if (noise(cx - 7, cz + 7, seed + 5) > 0.4) {
        set(es, x2 - 1, floor + 1, z1 + 1, "azalea_leaves[persistent=true]");
    }
}

    private void buildPortalNorth(EditSession es) {
    // North gateway pavilion centered near (0,64,-50). The "Practice/Queue" gate.
    int cx = 0;
    int cz = -50;
    int floorY = SY; // grass surface
    long seed = 770451L;

    buildPortalNorthFoundation(es, cx, cz, floorY, seed);
    buildPortalNorthSteps(es, cx, cz, floorY);
    buildPortalNorthPortal(es, cx, cz, floorY, seed);
    buildPortalNorthCanopy(es, cx, cz, floorY, seed);
    buildPortalNorthBanners(es, cx, cz, floorY);
    buildPortalNorthLanterns(es, cx, cz, floorY);
    buildPortalNorthDetail(es, cx, cz, floorY, seed);
}

private void buildPortalNorthFoundation(EditSession es, int cx, int cz, int floorY, long seed) {
    // Raised stone-brick plaza platform that the pavilion sits on.
    int hx = 11; // half-width in x
    int hz = 9;  // half-depth in z
    // Solid base block beneath surface for structural support.
    fill(es, cx - hx, floorY - 3, cz - hz, cx + hx, floorY - 1, cz + hz, "stone_bricks");
    // Smooth-quartz top floor with a polished trim border.
    fill(es, cx - hx, floorY, cz - hz, cx + hx, floorY, cz + hz, "smooth_quartz");
    walls(es, cx - hx, floorY, cz - hz, cx + hx, floorY, cz + hz, "chiseled_quartz_block");
    // Inner decorative inlay using noise to scatter polished blackstone accents.
    for (int x = cx - hx + 1; x <= cx + hx - 1; x++) {
        for (int z = cz - hz + 1; z <= cz + hz - 1; z++) {
            double n = noise(x, z, seed);
            if (n > 0.86) {
                set(es, x, floorY, z, "polished_blackstone");
            } else if (n > 0.80) {
                set(es, x, floorY, z, "quartz_bricks");
            }
        }
    }
    // A central runway carpet of light_blue terracotta leading to the portal.
    for (int z = cz - hz + 1; z <= cz + 4; z++) {
        set(es, cx - 1, floorY, z, "light_blue_terracotta");
        set(es, cx, floorY, z, "light_blue_concrete");
        set(es, cx + 1, floorY, z, "light_blue_terracotta");
    }
    // Underside taper so the platform reads as floating tiers.
    fill(es, cx - hx + 2, floorY - 4, cz - hz + 2, cx + hx - 2, floorY - 4, cz + hz - 2, "smooth_stone");
    fill(es, cx - hx + 4, floorY - 5, cz - hz + 4, cx + hx - 4, floorY - 5, cz + hz - 4, "stone_bricks");
    fill(es, cx - hx + 6, floorY - 6, cz - hz + 6, cx + hx - 6, floorY - 6, cz + hz - 6, "polished_andesite");
}

private void buildPortalNorthSteps(EditSession es, int cx, int cz, int floorY) {
    // Grand staircase descending toward the island center (south, +z side).
    int baseZ = cz + 9; // front edge of platform
    for (int i = 0; i < 4; i++) {
        int sz = baseZ + 1 + i;
        int y = floorY - i;
        for (int x = cx - 5; x <= cx + 5; x++) {
            set(es, x, y, sz, "quartz_stairs[facing=south,half=bottom]");
        }
        // side cheek blocks for solidity
        set(es, cx - 6, y, sz, "smooth_quartz");
        set(es, cx + 6, y, sz, "smooth_quartz");
        // riser fill beneath
        fill(es, cx - 6, y - 1, sz, cx + 6, floorY - 4, sz, "stone_bricks");
    }
    // Newel posts flanking the stair top with lanterns.
    for (int s = -1; s <= 1; s += 2) {
        int px = cx + s * 6;
        column(es, px, baseZ, floorY, floorY + 2, "chiseled_quartz_block");
        set(es, px, floorY + 3, baseZ, "quartz_slab[type=bottom]");
        set(es, px, floorY + 4, baseZ, "sea_lantern");
        set(es, px, floorY + 5, baseZ, "quartz_slab[type=bottom]");
    }
}

private void buildPortalNorthPortal(EditSession es, int cx, int cz, int floorY, long seed) {
    // The grand framed portal. It faces south (toward island center) at z = cz - 1.
    int pz = cz - 1;
    int innerHalf = 2;   // glass core half-width (5 wide)
    int innerTop = floorY + 7;
    int frameTop = floorY + 9;

    // Obsidian / crying_obsidian outer frame with noise-driven veining.
    for (int x = cx - innerHalf - 1; x <= cx + innerHalf + 1; x++) {
        for (int y = floorY; y <= frameTop; y++) {
            boolean border = (x == cx - innerHalf - 1) || (x == cx + innerHalf + 1)
                    || (y == floorY) || (y == frameTop);
            if (!border) continue;
            double n = noise(x * 3, y * 5, seed);
            String mat = n > 0.55 ? "crying_obsidian" : "obsidian";
            set(es, x, y, pz, mat);
        }
    }
    // Thicken the frame one block deep behind for a chunky portal jamb.
    for (int x = cx - innerHalf - 1; x <= cx + innerHalf + 1; x++) {
        set(es, x, floorY, pz - 1, "obsidian");
        set(es, x, frameTop, pz - 1, "obsidian");
        column(es, x, pz - 1, floorY + 1, frameTop - 1,
                (x == cx - innerHalf - 1 || x == cx + innerHalf + 1) ? "obsidian" : "polished_blackstone");
    }

    // Glowing light_blue glass core, layered with white shimmer.
    for (int x = cx - innerHalf; x <= cx + innerHalf; x++) {
        for (int y = floorY + 1; y <= innerTop; y++) {
            double n = noise(x * 7, y * 9, seed + 13);
            String core = n > 0.78 ? "light_blue_stained_glass"
                    : (n > 0.30 ? "light_blue_stained_glass" : "white_stained_glass");
            set(es, x, y, pz, core);
            // depth: a second pane behind keeps it solid-looking from both sides
            set(es, x, y, pz - 1, "light_blue_stained_glass");
        }
    }
    // Sea lantern halo embedded in the jamb for the portal glow.
    for (int y = floorY + 2; y <= innerTop - 1; y += 2) {
        set(es, cx - innerHalf - 1, y, pz - 1, "sea_lantern");
        set(es, cx + innerHalf + 1, y, pz - 1, "sea_lantern");
    }
    // Chiseled keystone and finials on the arch.
    set(es, cx, frameTop, pz, "chiseled_quartz_block");
    set(es, cx, frameTop + 1, pz, "quartz_stairs[facing=south,half=top]");
    set(es, cx - innerHalf - 1, frameTop + 1, pz, "quartz_stairs[facing=south,half=top]");
    set(es, cx + innerHalf + 1, frameTop + 1, pz, "quartz_stairs[facing=south,half=top]");
    // Corner crying_obsidian buttress columns reinforcing the gate.
    for (int s = -1; s <= 1; s += 2) {
        int px = cx + s * (innerHalf + 2);
        column(es, px, pz, floorY, floorY + 6, "polished_blackstone_bricks");
        set(es, px, floorY + 7, pz, "polished_blackstone_brick_wall");
        set(es, px, floorY + 8, pz, "crying_obsidian");
        set(es, px, floorY, pz, "chiseled_polished_blackstone");
    }
}

private void buildPortalNorthCanopy(EditSession es, int cx, int cz, int floorY, long seed) {
    // Quartz pillared canopy roof over the portal.
    int hx = 7;
    int hz = 5;
    int pillarTop = floorY + 9;
    int roofBase = floorY + 10;

    // Four corner clusters of quartz pillars.
    int[][] corners = {
        {cx - hx, cz - hz}, {cx + hx, cz - hz},
        {cx - hx, cz + hz}, {cx + hx, cz + hz}
    };
    for (int[] c : corners) {
        int px = c[0];
        int pz = c[1];
        column(es, px, pz, floorY, pillarTop, "quartz_pillar[axis=y]");
        // base molding
        set(es, px, floorY, pz, "chiseled_quartz_block");
        set(es, px, floorY + 1, pz, "quartz_bricks");
        // capital
        set(es, px, pillarTop, pz, "chiseled_quartz_block");
        // little buttress slabs around the foot
        set(es, px + (px < cx ? 1 : -1), floorY, pz, "quartz_slab[type=bottom]");
        set(es, px, floorY, pz + (pz < cz ? 1 : -1), "quartz_slab[type=bottom]");
    }
    // Mid pillars along the long edges for a colonnade feel.
    for (int x = cx - hx + 4; x <= cx + hx - 4; x += 4) {
        column(es, x, cz - hz, floorY, pillarTop, "quartz_pillar[axis=y]");
        column(es, x, cz + hz, floorY, pillarTop, "quartz_pillar[axis=y]");
        set(es, x, pillarTop, cz - hz, "chiseled_quartz_block");
        set(es, x, pillarTop, cz + hz, "chiseled_quartz_block");
    }

    // Architrave: a continuous beam ring atop the pillars.
    fill(es, cx - hx, roofBase, cz - hz, cx + hx, roofBase, cz - hz, "smooth_quartz");
    fill(es, cx - hx, roofBase, cz + hz, cx + hx, roofBase, cz + hz, "smooth_quartz");
    fill(es, cx - hx, roofBase, cz - hz, cx - hx, roofBase, cz + hz, "smooth_quartz");
    fill(es, cx + hx, roofBase, cz - hz, cx + hx, roofBase, cz + hz, "smooth_quartz");
    // Decorative quartz-brick frieze just below the beam.
    walls(es, cx - hx, roofBase - 1, cz - hz, cx + hx, roofBase - 1, cz + hz, "quartz_bricks");

    // Stepped pyramidal roof in quartz stairs, four sloping faces.
    int layers = hz; // rises to a peak
    for (int i = 0; i <= layers; i++) {
        int y = roofBase + 1 + i;
        int ix = hx - i;
        int iz = hz - i;
        if (ix < 0 || iz < 0) break;
        // North & south slopes (stairs facing inward over z)
        for (int x = cx - ix; x <= cx + ix; x++) {
            set(es, x, y, cz - iz, "quartz_stairs[facing=south,half=bottom]");
            set(es, x, y, cz + iz, "quartz_stairs[facing=north,half=bottom]");
        }
        // East & west slopes
        for (int z = cz - iz + 1; z <= cz + iz - 1; z++) {
            set(es, cx - ix, y, z, "quartz_stairs[facing=east,half=bottom]");
            set(es, cx + ix, y, z, "quartz_stairs[facing=west,half=bottom]");
        }
        // Fill the inner roof plate so there are no holes when viewed from below.
        if (ix >= 1 && iz >= 1) {
            fill(es, cx - ix + 1, y - 1, cz - iz + 1, cx + ix - 1, y - 1, cz + iz - 1, "smooth_quartz");
        }
        // Subtle blue glow gem embedded near the ridge using noise.
        if (i >= 2 && noise(i * 11, y * 3, seed + 7) > 0.6) {
            set(es, cx, y - 1, cz, "sea_lantern");
        }
    }
    // Ridge crest and finial.
    int peakY = roofBase + 1 + layers;
    set(es, cx, peakY, cz, "chiseled_quartz_block");
    set(es, cx, peakY + 1, cz, "quartz_slab[type=bottom]");
    set(es, cx, peakY + 2, cz, "end_rod[facing=up]");
    set(es, cx, peakY + 3, cz, "light_blue_stained_glass");
    // Eave lanterns hanging from the four corners of the architrave.
    for (int[] c : corners) {
        set(es, c[0], roofBase - 2, c[1], "chain[axis=y]");
        set(es, c[0], roofBase - 3, c[1], "lantern[hanging=true]");
    }
}

private void buildPortalNorthBanners(EditSession es, int cx, int cz, int floorY) {
    // Banner-like wool accents draped between the front pillars, framing the gate.
    int hx = 7;
    int frontZ = cz + 5; // along the south colonnade beam
    int topY = floorY + 8;
    // Two tall vertical banners of alternating wool with a trim edge.
    int[] bxs = {cx - 4, cx + 4};
    for (int bx : bxs) {
        for (int y = floorY + 2; y <= topY; y++) {
            String wool = ((y - floorY) % 3 == 0) ? "light_blue_wool"
                    : ((y - floorY) % 3 == 1 ? "white_wool" : "cyan_wool");
            set(es, bx, y, frontZ, wool);
        }
        // banner crossbar + crest
        set(es, bx, topY + 1, frontZ, "dark_oak_slab[type=bottom]");
        set(es, bx, topY + 2, frontZ, "lantern[hanging=true]");
        // pointed tail at the bottom
        set(es, bx, floorY + 1, frontZ, "light_blue_carpet");
    }
    // Bunting of wool strung across the front beam.
    for (int x = cx - hx + 1; x <= cx + hx - 1; x++) {
        String w = (x % 2 == 0) ? "light_blue_wool" : "white_wool";
        set(es, x, floorY + 9, frontZ, w);
    }
    // Heraldic shields (concrete + glass) flanking the inner portal.
    for (int s = -1; s <= 1; s += 2) {
        int px = cx + s * 4;
        set(es, px, floorY + 4, cz - 1, "cyan_concrete");
        set(es, px, floorY + 5, cz - 1, "light_blue_concrete");
        set(es, px, floorY + 6, cz - 1, "white_concrete");
    }
}

private void buildPortalNorthLanterns(EditSession es, int cx, int cz, int floorY) {
    // Perimeter lamp posts around the plaza for warm lighting.
    int[][] spots = {
        {cx - 9, cz - 7}, {cx + 9, cz - 7},
        {cx - 9, cz + 7}, {cx + 9, cz + 7},
        {cx - 9, cz}, {cx + 9, cz}
    };
    for (int[] sp : spots) {
        int px = sp[0];
        int pz = sp[1];
        column(es, px, pz, floorY + 1, floorY + 3, "polished_blackstone_wall");
        set(es, px, floorY + 4, pz, "chiseled_quartz_block");
        set(es, px, floorY + 5, pz, "soul_lantern[hanging=false]");
        // little arms
        set(es, px, floorY + 4, pz + 1, "polished_blackstone_slab[type=top]");
    }
    // Floor-flush sea lantern tiles glowing along the runway edges.
    for (int z = cz + 4; z >= cz - 6; z -= 2) {
        set(es, cx - 3, floorY, z, "sea_lantern");
        set(es, cx + 3, floorY, z, "sea_lantern");
    }
    // Hanging lanterns under the canopy interior.
    for (int s = -1; s <= 1; s += 2) {
        set(es, cx + s * 3, floorY + 8, cz, "chain[axis=y]");
        set(es, cx + s * 3, floorY + 7, cz, "lantern[hanging=true]");
    }
}

private void buildPortalNorthDetail(EditSession es, int cx, int cz, int floorY, long seed) {
    // Final layer of organic trim, planters, and wear variation.
    int hx = 11;
    int hz = 9;
    // Wall railing around the open south face of the plaza.
    for (int x = cx - hx; x <= cx + hx; x++) {
        if (x >= cx - 6 && x <= cx + 6) continue; // gap for the stairs
        set(es, x, floorY + 1, cz + hz, "polished_blackstone_brick_wall");
        if (noise(x, cz + hz, seed + 3) > 0.7) {
            set(es, x, floorY + 2, cz + hz, "lantern[hanging=false]");
        }
    }
    // Side railings along east and west edges.
    for (int z = cz - hz; z <= cz + hz; z++) {
        set(es, cx - hx, floorY + 1, z, "polished_blackstone_brick_wall");
        set(es, cx + hx, floorY + 1, z, "polished_blackstone_brick_wall");
    }
    // Corner planters with foliage for life.
    int[][] planters = {
        {cx - hx + 1, cz - hz + 1}, {cx + hx - 1, cz - hz + 1},
        {cx - hx + 1, cz + hz - 1}, {cx + hx - 1, cz + hz - 1}
    };
    for (int[] p : planters) {
        set(es, p[0], floorY + 1, p[1], "mossy_cobblestone_wall");
        set(es, p[0], floorY + 2, p[1], "flower_pot");
        scatter(es, p[0], floorY + 2, p[1], 1.2, 0.4, seed + 21, "azure_bluet", "oxeye_daisy", "lily_of_the_valley");
    }
    // Mossy / cracked wear scattered into the floor for an aged look.
    for (int x = cx - hx + 1; x <= cx + hx - 1; x++) {
        for (int z = cz - hz + 1; z <= cz + hz - 1; z++) {
            double n = noise(x * 2, z * 2, seed + 99);
            if (n > 0.93) {
                set(es, x, floorY, z, "cracked_stone_bricks");
            } else if (n < 0.04) {
                set(es, x, floorY, z, "mossy_stone_bricks");
            }
        }
    }
    // A couple of decorative quartz benches facing the portal.
    for (int s = -1; s <= 1; s += 2) {
        int bx = cx + s * 5;
        set(es, bx, floorY + 1, cz + 6, "quartz_stairs[facing=north,half=bottom]");
        set(es, bx + (s > 0 ? -1 : 1), floorY + 1, cz + 6, "quartz_stairs[facing=north,half=bottom]");
        set(es, bx, floorY + 2, cz + 7, "quartz_slab[type=bottom]");
    }
    // Glowing edge accent lights tucked under the platform lip (visible from below/sides).
    for (int x = cx - hx; x <= cx + hx; x += 3) {
        set(es, x, floorY - 1, cz - hz, "shroomlight");
        set(es, x, floorY - 1, cz + hz, "shroomlight");
    }
    for (int z = cz - hz; z <= cz + hz; z += 3) {
        set(es, cx - hx, floorY - 1, z, "shroomlight");
        set(es, cx + hx, floorY - 1, z, "shroomlight");
    }
}

    // ── South Gateway Pavilion — "Events" gate (warm theme) ────────────────────
    // Centered around (0, 64, 50). Mirrors the north gate with a warm palette:
    // deepslate/blackstone frame, orange/red stained-glass core, prismarine &
    // sea-lantern roofed canopy, broad steps and lantern posts.
    private void buildPortalSouth(EditSession es) {
        final int cx = 0;          // centered on the south axis
        final int cz = 50;         // gateway center Z
        final int baseY = SY;      // grass surface
        buildPortalSouthFoundation(es, cx, cz, baseY);
        buildPortalSouthSteps(es, cx, cz, baseY);
        buildPortalSouthPillars(es, cx, cz, baseY);
        buildPortalSouthGate(es, cx, cz, baseY);
        buildPortalSouthCanopy(es, cx, cz, baseY);
        buildPortalSouthLanterns(es, cx, cz, baseY);
        buildPortalSouthBanners(es, cx, cz, baseY);
    }

    // Raised stone platform with warm trim and a noisy weathered floor.
    private void buildPortalSouthFoundation(EditSession es, int cx, int cz, int baseY) {
        // A rounded plaza disk under the pavilion, two courses deep.
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (!buildPortalSouthInArea(dx, dz, 9.0)) continue;
                int x = cx + dx, z = cz + dz;
                double n = noise(x, z, 7701L);
                // Sub-floor: deepslate base bound for stability.
                set(es, x, baseY - 1, z, "deepslate");
                set(es, x, baseY - 2, z, n < 0.5 ? "deepslate" : "cobbled_deepslate");
                // Surface paving — warm blackstone/deepslate mosaic.
                String floor;
                double d2 = dx * dx + dz * dz;
                if (d2 < 9) {
                    floor = "polished_blackstone_bricks";
                } else if (n < 0.18) {
                    floor = "cracked_polished_blackstone_bricks";
                } else if (n < 0.40) {
                    floor = "polished_deepslate";
                } else if (n < 0.78) {
                    floor = "polished_blackstone";
                } else {
                    floor = "chiseled_polished_blackstone";
                }
                set(es, x, baseY, z, floor);
            }
        }
        // Glowing trim ring just inside the rim — warm gilded accents.
        ring(es, cx, baseY, cz, 7.4, 8.2, "polished_blackstone_brick_slab[type=top]");
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12.0;
            int x = cx + (int) Math.round(Math.cos(a) * 7.0);
            int z = cz + (int) Math.round(Math.sin(a) * 7.0);
            set(es, x, baseY, z, (i % 2 == 0) ? "gilded_blackstone" : "shroomlight");
        }
        // Central inlay — a warm sea-lantern cross set flush in the floor.
        set(es, cx, baseY, cz, "sea_lantern");
        for (int o = 1; o <= 2; o++) {
            set(es, cx + o, baseY, cz, "orange_terracotta");
            set(es, cx - o, baseY, cz, "orange_terracotta");
            set(es, cx, baseY, cz + o, "orange_terracotta");
            set(es, cx, baseY, cz - o, "orange_terracotta");
        }
    }

    // Broad descending steps on the outward (south) face leading off the plaza.
    private void buildPortalSouthSteps(EditSession es, int cx, int cz, int baseY) {
        // Three cascading step courses fanning toward +Z (away from island center).
        for (int s = 1; s <= 3; s++) {
            int z = cz + 7 + s;
            int y = baseY - s + 1;
            int half = 5 - (s - 1);
            for (int dx = -half; dx <= half; dx++) {
                int x = cx + dx;
                set(es, x, y, z, "blackstone_stairs[facing=north,half=bottom]");
                set(es, x, y - 1, z, "polished_blackstone");
            }
            // Side cheek blocks to frame the stair run.
            set(es, cx - half - 1, y, z, "polished_blackstone_brick_wall");
            set(es, cx + half + 1, y, z, "polished_blackstone_brick_wall");
        }
        // Short approach steps on the inward (north) face toward the hub center.
        for (int s = 1; s <= 2; s++) {
            int z = cz - 7 - s;
            int y = baseY - s + 1;
            for (int dx = -4; dx <= 4; dx++) {
                set(es, cx + dx, y, z, "blackstone_stairs[facing=south,half=bottom]");
                set(es, cx + dx, y - 1, z, "polished_blackstone");
            }
        }
    }

    // Four corner pillars of the pavilion: deepslate cores, blackstone trim,
    // chains rising into the canopy.
    private void buildPortalSouthPillars(EditSession es, int cx, int cz, int baseY) {
        int[][] corners = {
                { cx - 6, cz - 6 }, { cx + 6, cz - 6 },
                { cx - 6, cz + 6 }, { cx + 6, cz + 6 }
        };
        int top = baseY + 9;
        for (int[] c : corners) {
            int px = c[0], pz = c[1];
            // Pillar shaft with banded trim.
            for (int y = baseY + 1; y <= top; y++) {
                String id;
                int rel = y - baseY;
                if (rel == 1) id = "chiseled_polished_blackstone";
                else if (rel % 4 == 0) id = "gilded_blackstone";
                else id = (rel % 2 == 0) ? "polished_deepslate" : "polished_blackstone_bricks";
                set(es, px, y, pz, id);
            }
            // Decorative wall caps and base flare.
            set(es, px, baseY + 1, pz, "deepslate_tile_wall");
            set(es, px, top, pz, "chiseled_polished_blackstone");
            // Lantern brackets jutting inward toward the gate.
            int inx = px + (px < cx ? 1 : -1);
            int inz = pz + (pz < cz ? 1 : -1);
            set(es, inx, top, inz, "polished_blackstone_brick_slab[type=bottom]");
            set(es, inx, top - 1, inz, "lantern[hanging=true]");
            // Hanging chain into the canopy.
            set(es, px, top + 1, pz, "chain[axis=y]");
        }
    }

    // The portal gate proper: tall deepslate/blackstone arch framing a glowing
    // orange/red stained-glass core (the "Events" portal surface).
    private void buildPortalSouthGate(EditSession es, int cx, int cz, int baseY) {
        // The gate stands on the cz plane, opening along the X axis (3 wide, 5 tall).
        int top = baseY + 6;
        // Frame posts at x = ±2.
        for (int side : new int[]{ -2, 2 }) {
            int x = cx + side;
            for (int y = baseY; y <= top; y++) {
                String id = (y == baseY) ? "crying_obsidian"
                          : (y == top)   ? "chiseled_deepslate"
                          : (y % 2 == 0) ? "polished_deepslate"
                          : "deepslate_bricks";
                set(es, x, y, cz, id);
            }
        }
        // Lintel across the top + a raised keystone.
        for (int dx = -2; dx <= 2; dx++) {
            set(es, cx + dx, top, cz, "polished_deepslate");
        }
        set(es, cx, top + 1, cz, "chiseled_deepslate");
        set(es, cx, top + 2, cz, "soul_lantern");
        // Glass core — warm gradient from orange (bottom) to red (top).
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = baseY + 1; y < top; y++) {
                int x = cx + dx;
                int rel = y - baseY;
                double n = noise(x, y * 31 + 5, 9912L);
                String glass;
                if (rel <= 1) glass = "orange_stained_glass";
                else if (rel == 2) glass = (n < 0.5) ? "orange_stained_glass" : "red_stained_glass";
                else if (rel == 3) glass = (n < 0.35) ? "orange_stained_glass" : "red_stained_glass";
                else glass = "red_stained_glass";
                set(es, x, y, cz, glass);
            }
        }
        // Glowing inner light strip behind the glass for an active-portal look.
        for (int y = baseY + 1; y < top; y += 2) {
            set(es, cx, y, cz - 1, "shroomlight");
        }
        // Front threshold slab — a warm step at the portal foot.
        for (int dx = -1; dx <= 1; dx++) {
            set(es, cx + dx, baseY, cz + 1, "polished_blackstone_brick_slab[type=top]");
            set(es, cx + dx, baseY, cz - 1, "polished_blackstone_brick_slab[type=top]");
        }
        // Side buttresses connecting gate posts to corner pillars.
        for (int side : new int[]{ -1, 1 }) {
            int x = cx + side * 4;
            set(es, x, baseY + 1, cz, "deepslate_brick_wall");
            set(es, x, baseY + 2, cz, "deepslate_brick_wall");
            set(es, x, baseY + 3, cz, "deepslate_tiles");
        }
    }

    // Prismarine-and-sea-lantern hip roof spanning the four pillars.
    private void buildPortalSouthCanopy(EditSession es, int cx, int cz, int baseY) {
        int eaveY = baseY + 10;   // first roof course sits just above the pillars
        // Beam frame tying the four pillar tops together.
        for (int dx = -6; dx <= 6; dx++) {
            set(es, cx + dx, eaveY, cz - 6, "dark_prismarine");
            set(es, cx + dx, eaveY, cz + 6, "dark_prismarine");
        }
        for (int dz = -6; dz <= 6; dz++) {
            set(es, cx - 6, eaveY, cz + dz, "dark_prismarine");
            set(es, cx + 6, eaveY, cz + dz, "dark_prismarine");
        }
        // Stepped pyramidal roof of prismarine bricks with sea-lantern glow.
        for (int layer = 0; layer <= 6; layer++) {
            int y = eaveY + layer;
            int half = 7 - layer;
            if (half < 0) break;
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    // Only the outer ring of each layer (hollow stepped roof).
                    boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
                    if (!edge && layer != 6) continue;
                    int x = cx + dx, z = cz + dz;
                    double n = noise(x, z, 4400L + layer);
                    String id;
                    if (layer == 6) {
                        id = "sea_lantern";
                    } else if ((dx + dz + layer) % 5 == 0) {
                        id = "sea_lantern";
                    } else if (n < 0.30) {
                        id = "dark_prismarine";
                    } else if (n < 0.62) {
                        id = "prismarine_bricks";
                    } else {
                        id = "prismarine";
                    }
                    set(es, x, y, z, id);
                }
            }
            // Eave stairs flaring out the lowest two courses for a hip-roof look.
            if (layer <= 1) {
                int eh = half;
                for (int dx = -eh; dx <= eh; dx++) {
                    set(es, cx + dx, y, cz - eh - 1, "prismarine_brick_stairs[facing=south,half=bottom]");
                    set(es, cx + dx, y, cz + eh + 1, "prismarine_brick_stairs[facing=north,half=bottom]");
                }
                for (int dz = -eh; dz <= eh; dz++) {
                    set(es, cx - eh - 1, y, cz + dz, "prismarine_brick_stairs[facing=east,half=bottom]");
                    set(es, cx + eh + 1, y, cz + dz, "prismarine_brick_stairs[facing=west,half=bottom]");
                }
            }
        }
        // Roof apex finial — a small glowing spire.
        int apex = eaveY + 6;
        set(es, cx, apex, cz, "sea_lantern");
        set(es, cx, apex + 1, cz, "dark_prismarine_slab[type=bottom]");
        set(es, cx, apex + 2, cz, "sea_lantern");
        set(es, cx, apex + 3, cz, "end_rod");
        // Underside glow studs so the canopy lights the plaza below.
        for (int dx = -4; dx <= 4; dx += 4) {
            for (int dz = -4; dz <= 4; dz += 4) {
                set(es, cx + dx, eaveY - 1, cz + dz, "sea_lantern");
            }
        }
    }

    // Lantern posts and hanging soul-fire accents around the approach.
    private void buildPortalSouthLanterns(EditSession es, int cx, int cz, int baseY) {
        // Standing lantern posts flanking the outer steps.
        int[][] posts = {
                { cx - 6, cz + 8 }, { cx + 6, cz + 8 },
                { cx - 8, cz - 6 }, { cx + 8, cz - 6 }
        };
        for (int[] p : posts) {
            int px = p[0], pz = p[1];
            column(es, px, pz, baseY + 1, baseY + 3, "deepslate_brick_wall");
            set(es, px, baseY + 4, pz, "polished_blackstone_brick_slab[type=bottom]");
            set(es, px, baseY + 3, pz, "soul_lantern");
        }
        // Hanging lanterns from the eave beam pointing inward.
        for (int dx = -4; dx <= 4; dx += 8) {
            set(es, cx + dx, baseY + 9, cz, "chain[axis=y]");
            set(es, cx + dx, baseY + 8, cz, "lantern[hanging=true]");
        }
        // Warm ember scatter — orange carpet flecks on the plaza edge as embers.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (!buildPortalSouthInArea(dx, dz, 8.6)) continue;
                if (buildPortalSouthInArea(dx, dz, 6.5)) continue;
                int x = cx + dx, z = cz + dz;
                if (Math.abs(dx) <= 1 && dz > 0) continue;  // keep the south path clear
                double n = noise(x, z, 31337L);
                if (n > 0.93) set(es, x, baseY + 1, z, "orange_carpet");
                else if (n > 0.89) set(es, x, baseY + 1, z, "soul_torch");
            }
        }
    }

    // Decorative hanging banners and signage flanking the gate ("Events").
    private void buildPortalSouthBanners(EditSession es, int cx, int cz, int baseY) {
        // Tall orange banners on the inner faces of the gate posts.
        for (int side : new int[]{ -3, 3 }) {
            int x = cx + side;
            set(es, x, baseY + 5, cz, "orange_wall_banner[facing=south]");
            set(es, x, baseY + 4, cz, "red_wall_banner[facing=south]");
        }
        // A keystone glow over the lintel and a pair of framing chains.
        set(es, cx - 2, baseY + 7, cz, "chain[axis=y]");
        set(es, cx + 2, baseY + 7, cz, "chain[axis=y]");
        // Crowning beacon-like marker visible from afar above the apex.
        int beaconY = baseY + 17;
        set(es, cx, beaconY, cz, "orange_stained_glass");
        set(es, cx, beaconY + 1, cz, "shroomlight");
    }

    // Local circular-area test (avoids clobbering the class-wide inDisk semantics).
    private boolean buildPortalSouthInArea(int dx, int dz, double r) {
        return dx * dx + dz * dz <= r * r;
    }

    private void buildPortalEast(EditSession es) {
    // East gateway pavilion centered around (50, 64, 0). The "Training" gate.
    // Nature-themed arch of mossy_stone_bricks + oak, green stained glass core,
    // leafy canopy, flower boxes, and lanterns.
    int cx = 50;        // pavilion center x
    int cz = 0;         // pavilion center z
    int baseY = SY;     // grass surface

    buildPortalEastFoundation(es, cx, cz, baseY);
    buildPortalEastPlaza(es, cx, cz, baseY);
    buildPortalEastPillars(es, cx, cz, baseY);
    buildPortalEastArch(es, cx, cz, baseY);
    buildPortalEastCore(es, cx, cz, baseY);
    buildPortalEastCanopy(es, cx, cz, baseY);
    buildPortalEastFlowerBoxes(es, cx, cz, baseY);
    buildPortalEastLanterns(es, cx, cz, baseY);
    buildPortalEastStairs(es, cx, cz, baseY);
}

// ---- Foundation: a rooted stone platform fused into the island edge ----
private void buildPortalEastFoundation(EditSession es, int cx, int cz, int baseY) {
    // Layered circular base sinking below the grass for a planted look.
    disk(es, cx, baseY - 1, cz, 9.5, "mossy_cobblestone");
    disk(es, cx, baseY - 2, cz, 8.5, "mossy_cobblestone");
    disk(es, cx, baseY - 3, cz, 7.0, "cobblestone");
    disk(es, cx, baseY - 4, cz, 5.0, "stone");
    // Hanging roots / tendrils descending toward the void.
    for (int i = 0; i < 7; i++) {
        double a = (Math.PI * 2.0 * i) / 7.0;
        int rx = cx + (int) Math.round(Math.cos(a) * 6.0);
        int rz = cz + (int) Math.round(Math.sin(a) * 6.0);
        int depth = 3 + (int) Math.round(noise(rx, rz, 8801L) * 5.0);
        column(es, rx, rz, baseY - depth, baseY - 4, "stone");
        set(es, rx, baseY - depth - 1, rz, "hanging_roots");
    }
}

// ---- Plaza floor: mossy brick mandala with organic moss speckle ----
private void buildPortalEastPlaza(EditSession es, int cx, int cz, int baseY) {
    disk(es, cx, baseY, cz, 8.0, "mossy_stone_bricks");
    ring(es, cx, baseY, cz, 6.0, 7.2, "stone_bricks");
    ring(es, cx, baseY, cz, 3.2, 4.0, "cracked_stone_bricks");
    disk(es, cx, baseY, cz, 2.6, "polished_andesite");
    // Organic moss / grass speckle over the plaza for a reclaimed feel.
    for (int dx = -8; dx <= 8; dx++) {
        for (int dz = -8; dz <= 8; dz++) {
            if (!inDisk(dx, dz, 7.8)) continue;
            double n = noise(cx + dx, cz + dz, 4242L);
            if (n > 0.82) set(es, cx + dx, baseY, cz + dz, "moss_block");
            else if (n > 0.74) set(es, cx + dx, baseY, cz + dz, "mossy_cobblestone");
        }
    }
    // Compass inlay at the center pointing east toward the gate.
    set(es, cx, baseY, cz, "emerald_block");
    for (int i = 1; i <= 2; i++) set(es, cx + i, baseY, cz, "lime_terracotta");
}

// ---- Four corner pillars framing the gate, wrapped in vines ----
private void buildPortalEastPillars(EditSession es, int cx, int cz, int baseY) {
    int[][] corners = {{-3, -5}, {-3, 5}, {3, -5}, {3, 5}};
    for (int[] c : corners) {
        int px = cx + c[0];
        int pz = cz + c[1];
        // Stone-brick shaft with mossy banding.
        for (int y = 0; y <= 6; y++) {
            String mat = (y % 3 == 0) ? "mossy_stone_bricks" : "stone_bricks";
            set(es, px, baseY + y, pz, mat);
        }
        // Oak log inner accent core where space allows.
        set(es, px, baseY + 2, pz, "chiseled_stone_bricks");
        set(es, px, baseY + 5, pz, "chiseled_stone_bricks");
        // Capital + decorative top.
        set(es, px, baseY + 7, pz, "mossy_stone_brick_wall");
        set(es, px, baseY + 8, pz, "lantern");
        // Vines clinging to the outward faces.
        int vx = (c[1] > 0) ? 1 : -1;
        for (int y = 1; y <= 5; y++) {
            if (noise(px, pz + y, 991L) > 0.4) {
                String face = (vx > 0) ? "south" : "north";
                set(es, px, baseY + y, pz + vx, "vine[" + face + "=true]");
            }
        }
    }
}

// ---- The grand arch: mossy stone bricks + oak, stepped sweep ----
private void buildPortalEastArch(EditSession es, int cx, int cz, int baseY) {
    // Two heavy uprights at the gate mouth (the side facing inward, x=cx-3).
    int gx = cx - 3;
    for (int side = -1; side <= 1; side += 2) {
        int gz = cz + side * 4;
        // Massive layered upright.
        for (int y = 0; y <= 9; y++) {
            String mat = (y == 0 || y == 9) ? "chiseled_stone_bricks"
                    : ((y % 2 == 0) ? "mossy_stone_bricks" : "stone_bricks");
            set(es, gx, baseY + y, gz, mat);
            set(es, gx, baseY + y, gz + side, mat);
        }
        // Oak log corner posts wrapping the stone.
        column(es, gx - 1, gz, baseY, baseY + 9, "oak_log[axis=y]");
        // Stripped oak trim band.
        set(es, gx - 1, baseY + 4, gz, "stripped_oak_log[axis=y]");
    }
    // Semicircular arch sweep connecting the two uprights over z.
    double archR = 4.0;
    int archCY = baseY + 9;
    for (int dz = -5; dz <= 5; dz++) {
        double inside = archR * archR - dz * dz;
        if (inside < 0) continue;
        int rise = (int) Math.round(Math.sqrt(inside));
        int ay = archCY + rise - 1;
        // Voussoir stones along the curve.
        String mat = (Math.abs(dz) % 2 == 0) ? "mossy_stone_bricks" : "stone_bricks";
        set(es, gx, ay, cz + dz, mat);
        set(es, gx, ay + 1, cz + dz, "chiseled_stone_bricks");
        // Oak frame on the inner face of the curve.
        set(es, gx - 1, ay, cz + dz, "oak_log[axis=x]");
    }
    // Keystone crown at the apex.
    set(es, gx, archCY + (int) archR, cz, "chiseled_stone_bricks");
    set(es, gx, archCY + (int) archR + 1, cz, "mossy_stone_brick_wall");
    set(es, gx, archCY + (int) archR + 2, cz, "lantern[hanging=true]");
    // Carved oak gate-sign banner beneath the apex.
    set(es, gx - 1, archCY - 1, cz, "stripped_oak_wood");
    set(es, gx - 1, archCY - 1, cz - 1, "oak_wall_sign[facing=west]");
    set(es, gx - 1, archCY - 1, cz + 1, "oak_wall_sign[facing=west]");
}

// ---- Green stained-glass core: the glowing nature portal ----
private void buildPortalEastCore(EditSession es, int cx, int cz, int baseY) {
    int gx = cx - 3;
    // Fill the gate opening with layered green glass for a luminous core.
    for (int y = 1; y <= 8; y++) {
        for (int dz = -3; dz <= 3; dz++) {
            double edge = Math.abs(dz) + Math.abs(y - 4) * 0.6;
            if (edge > 4.2) continue;
            String glass;
            double n = noise(gx + dz, baseY + y, 7007L);
            if (edge < 2.0) glass = (n > 0.5) ? "lime_stained_glass" : "green_stained_glass";
            else glass = (n > 0.6) ? "green_stained_glass" : "lime_stained_glass";
            set(es, gx, baseY + y, cz + dz, glass);
        }
    }
    // Glowing veins woven through the glass.
    for (int y = 2; y <= 7; y += 2) {
        set(es, gx, baseY + y, cz, "sea_lantern");
    }
    // Light bloom behind the glass (one block west) so it reads as glowing.
    for (int y = 2; y <= 6; y += 2) {
        if (noise(gx, baseY + y, 333L) > 0.5)
            set(es, gx - 1, baseY + y, cz, "glowstone");
    }
}

// ---- Leafy canopy: a living roof of oak leaves draped over the pavilion ----
private void buildPortalEastCanopy(EditSession es, int cx, int cz, int baseY) {
    int topY = baseY + 10;
    // Oak beam frame spanning the pillars.
    for (int dz = -5; dz <= 5; dz++) {
        set(es, cx - 3, topY, cz + dz, "oak_log[axis=z]");
        set(es, cx + 3, topY, cz + dz, "oak_log[axis=z]");
    }
    for (int dx = -3; dx <= 3; dx++) {
        set(es, cx + dx, topY, cz - 5, "oak_log[axis=x]");
        set(es, cx + dx, topY, cz + 5, "oak_log[axis=x]");
    }
    // Domed leaf canopy with organic noise thickness.
    for (int dx = -5; dx <= 5; dx++) {
        for (int dz = -6; dz <= 6; dz++) {
            double d = Math.sqrt(dx * dx * 1.4 + dz * dz);
            if (d > 6.2) continue;
            int h = (int) Math.round(3.0 - d * 0.4);
            for (int k = 0; k <= h; k++) {
                int ly = topY + 1 + k;
                double n = noise(cx + dx, cz + dz + k * 13, 5151L);
                if (k == h && n < 0.35) continue; // ragged organic top
                set(es, cx + dx, ly, cz + dz, "oak_leaves[persistent=true]");
            }
        }
    }
    // Crowning central foliage burst.
    sphere(es, cx, topY + 4, cz, 2.6, "oak_leaves[persistent=true]");
    set(es, cx, topY + 6, cz, "oak_leaves[persistent=true]");
    // Hanging leaf vines dripping from the canopy underside.
    for (int i = 0; i < 14; i++) {
        double a = (Math.PI * 2.0 * i) / 14.0;
        int hx = cx + (int) Math.round(Math.cos(a) * 4.5);
        int hz = cz + (int) Math.round(Math.sin(a) * 5.5);
        int len = 1 + (int) Math.round(noise(hx, hz, 6262L) * 3.0);
        for (int k = 0; k < len; k++) {
            set(es, hx, topY - k, hz, "oak_leaves[persistent=true]");
        }
    }
}

// ---- Flower boxes ringing the plaza with seasonal blooms ----
private void buildPortalEastFlowerBoxes(EditSession es, int cx, int cz, int baseY) {
    String[] flowers = {"poppy", "dandelion", "oxeye_daisy", "cornflower",
            "azure_bluet", "allium", "lily_of_the_valley", "blue_orchid"};
    // Planter boxes at eight points around the outer ring.
    for (int i = 0; i < 8; i++) {
        double a = (Math.PI * 2.0 * i) / 8.0;
        int bx = cx + (int) Math.round(Math.cos(a) * 6.5);
        int bz = cz + (int) Math.round(Math.sin(a) * 6.5);
        // Stripped oak trough.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    set(es, bx, baseY + 1, bz, "rooted_dirt");
                } else {
                    set(es, bx + dx, baseY + 1, bz + dz, "stripped_oak_wood");
                }
            }
        }
        // Bloom (deterministic variety) and occasional tall fern.
        long fi = (long) (noise(bx, bz, 1212L) * 7.999);
        set(es, bx, baseY + 2, bz, flowers[(int) fi]);
        if (noise(bx, bz, 9090L) > 0.6) {
            set(es, bx, baseY + 2, bz, "fern");
        }
    }
    // Scattered low foliage across the mossy edges of the plaza.
    scatter(es, cx, baseY + 1, cz, 7.5, 0.06, 3030L,
            "short_grass", "fern", "poppy", "dandelion", "oxeye_daisy", "air");
}

// ---- Lanterns: warm hanging light woven through the structure ----
private void buildPortalEastLanterns(EditSession es, int cx, int cz, int baseY) {
    int topY = baseY + 10;
    // Hanging lanterns on chains under the canopy beams.
    int[][] spots = {{-2, -4}, {2, -4}, {-2, 4}, {2, 4}, {0, -5}, {0, 5}};
    for (int[] s : spots) {
        int lx = cx + s[0];
        int lz = cz + s[1];
        set(es, lx, topY - 1, lz, "chain[axis=y]");
        set(es, lx, topY - 2, lz, "lantern[hanging=true]");
    }
    // Soul-lantern pair flanking the green core for cool contrast.
    set(es, cx - 3, baseY + 1, cz - 4, "soul_lantern");
    set(es, cx - 3, baseY + 1, cz + 4, "soul_lantern");
    // Ground path lanterns on short oak fence posts leading to the gate.
    for (int i = 1; i <= 3; i++) {
        int px = cx + 4 + i;
        set(es, px, baseY + 1, cz - 3, "oak_fence");
        set(es, px, baseY + 2, cz - 3, "lantern");
        set(es, px, baseY + 1, cz + 3, "oak_fence");
        set(es, px, baseY + 2, cz + 3, "lantern");
    }
}

// ---- Approach stairs spilling from the gate toward the island center ----
private void buildPortalEastStairs(EditSession es, int cx, int cz, int baseY) {
    // Broad steps descending westward (toward island center) from the plaza.
    for (int step = 0; step < 5; step++) {
        int sx = cx - 8 - step;
        int sy = baseY - step;
        for (int dz = -3; dz <= 3; dz++) {
            set(es, sx, sy, cz + dz, "mossy_stone_brick_stairs[facing=east]");
            set(es, sx, sy - 1, cz + dz, "stone_bricks");
        }
        // Mossy edge railing posts.
        set(es, sx, sy + 1, cz - 4, "mossy_stone_brick_wall");
        set(es, sx, sy + 1, cz + 4, "mossy_stone_brick_wall");
        if (step % 2 == 0) {
            set(es, sx, sy + 2, cz - 4, "lantern");
            set(es, sx, sy + 2, cz + 4, "lantern");
        }
    }
    // Welcoming flower runners along the stair flanks.
    for (int step = 0; step < 5; step++) {
        int sx = cx - 8 - step;
        if (noise(sx, cz, 7474L) > 0.4)
            set(es, sx, baseY - step + 1, cz - 5, "azure_bluet");
        if (noise(sx, cz + 99, 7474L) > 0.4)
            set(es, sx, baseY - step + 1, cz + 5, "cornflower");
    }
}

    private void buildPortalWest(EditSession es) {
    // West gateway pavilion centered around (-50, 64, 0).
    // Regal quartz-and-gold arch with a white/light_blue glass core,
    // domed quartz roof, columns and lanterns.
    int cx = -50;
    int cz = 0;
    int baseY = SY; // grass surface

    buildPortalWestPlatform(es, cx, cz, baseY);
    buildPortalWestColumns(es, cx, cz, baseY);
    buildPortalWestArch(es, cx, cz, baseY);
    buildPortalWestGateCore(es, cx, cz, baseY);
    buildPortalWestDome(es, cx, cz, baseY);
    buildPortalWestLighting(es, cx, cz, baseY);
}

// Tiered circular platform / dais leading up to the gate.
private void buildPortalWestPlatform(EditSession es, int cx, int cz, int baseY) {
    // Three descending stone-trim tiers below grass level for a grounded base.
    disk(es, cx, baseY, cz, 13.0, "smooth_quartz");
    ring(es, cx, baseY, cz, 11.5, 13.0, "polished_diorite");
    disk(es, cx, baseY - 1, cz, 14.0, "quartz_bricks");
    ring(es, cx, baseY - 1, cz, 13.0, 14.0, "chiseled_quartz_block");
    disk(es, cx, baseY - 2, cz, 15.0, "smooth_quartz");
    ring(es, cx, baseY - 2, cz, 14.0, 15.0, "polished_diorite");
    disk(es, cx, baseY - 3, cz, 16.0, "quartz_bricks");

    // Inlaid gold compass cross on the floor with light_blue accents.
    for (int d = -10; d <= 10; d++) {
        String mat = (Math.abs(d) % 3 == 0) ? "sea_lantern" : "yellow_concrete";
        if (Math.abs(d) <= 11) {
            set(es, cx + d, baseY, cz, mat);
            set(es, cx, baseY, cz + d, mat);
        }
    }
    // Center medallion.
    disk(es, cx, baseY, cz, 2.4, "chiseled_quartz_block");
    set(es, cx, baseY, cz, "gold_block");
    ring(es, cx, baseY, cz, 2.4, 3.2, "light_blue_concrete");

    // Subtle organic weathering / moss-free quartz speckle using noise.
    for (int dx = -13; dx <= 13; dx++) {
        for (int dz = -13; dz <= 13; dz++) {
            if (inDisk(dx, dz, 13.0) && !inDisk(dx, dz, 3.2)) {
                double n = noise(cx + dx, cz + dz, 7711L);
                if (n > 0.86) set(es, cx + dx, baseY, cz + dz, "smooth_quartz");
                else if (n < 0.07) set(es, cx + dx, baseY, cz + dz, "polished_diorite");
            }
        }
    }

    // Approach steps on the west face (facing east, toward island center).
    for (int s = 0; s < 4; s++) {
        int sx = cx - 13 - s;
        fill(es, sx, baseY - 1 - s, cz - 4, sx, baseY - 1 - s, cz + 4,
                "quartz_stairs[facing=east,half=bottom]");
        fill(es, sx, baseY - 2 - s, cz - 4, sx, baseY - 2 - s, cz + 4, "smooth_quartz");
    }

    // Low decorative balustrade ring around the dais edge.
    for (int a = 0; a < 360; a += 12) {
        double rad = Math.toRadians(a);
        int px = cx + (int) Math.round(Math.cos(rad) * 12.0);
        int pz = cz + (int) Math.round(Math.sin(rad) * 12.0);
        // Leave an opening on the east side (gate approach from island center).
        if (px > cx + 9) continue;
        set(es, px, baseY + 1, pz, "quartz_wall");
        if (a % 36 == 0) {
            set(es, px, baseY + 2, pz, "chiseled_quartz_block");
            set(es, px, baseY + 3, pz, "gold_block");
            set(es, px, baseY + 4, pz, "sea_lantern");
        }
    }
}

// Four major columns framing the gate, plus paired flanking pillars.
private void buildPortalWestColumns(EditSession es, int cx, int cz, int baseY) {
    int[][] cols = {
            {cx + 4, cz - 6}, {cx + 4, cz + 6},
            {cx - 4, cz - 6}, {cx - 4, cz + 6}
    };
    for (int[] c : cols) {
        buildPortalWestOneColumn(es, c[0], c[1], baseY, 13);
    }
    // Smaller flanking pillars further out for grandeur.
    int[][] minis = {
            {cx + 8, cz - 9}, {cx + 8, cz + 9},
            {cx - 8, cz - 9}, {cx - 8, cz + 9}
    };
    for (int[] m : minis) {
        buildPortalWestOneColumn(es, m[0], m[1], baseY, 9);
    }
}

// A single fluted quartz column with gold banding and a lantern-topped capital.
private void buildPortalWestOneColumn(EditSession es, int px, int pz, int baseY, int h) {
    // Base plinth.
    fill(es, px - 1, baseY + 1, pz - 1, px + 1, baseY + 1, pz + 1, "chiseled_quartz_block");
    fill(es, px - 1, baseY + 2, pz - 1, px + 1, baseY + 2, pz + 1, "smooth_quartz");
    // Shaft with periodic gold rings.
    for (int y = baseY + 3; y <= baseY + h; y++) {
        int rel = y - (baseY + 3);
        String shaft = (rel % 4 == 3) ? "gold_block" : "quartz_pillar[axis=y]";
        set(es, px, y, pz, shaft);
        // Fluting on the four sides.
        set(es, px + 1, y, pz, "quartz_bricks");
        set(es, px - 1, y, pz, "quartz_bricks");
        set(es, px, y, pz + 1, "quartz_bricks");
        set(es, px, y, pz - 1, "quartz_bricks");
    }
    // Capital.
    int cap = baseY + h + 1;
    fill(es, px - 1, cap, pz - 1, px + 1, cap, pz + 1, "smooth_quartz");
    fill(es, px - 1, cap + 1, pz - 1, px + 1, cap + 1, pz + 1, "chiseled_quartz_block");
    set(es, px, cap + 2, pz, "sea_lantern");
    set(es, px, cap + 3, pz, "gold_block");
    set(es, px, cap + 4, pz, "lantern[hanging=false]");
    // Hanging lanterns on the capital corners.
    set(es, px + 1, cap, pz + 1, "chain[axis=y]");
    set(es, px + 1, cap - 1, pz + 1, "lantern[hanging=true]");
    set(es, px - 1, cap, pz - 1, "chain[axis=y]");
    set(es, px - 1, cap - 1, pz - 1, "lantern[hanging=true]");
}

// The grand arch spanning between the inner columns (across the Z axis).
private void buildPortalWestArch(EditSession es, int cx, int cz, int baseY) {
    int top = baseY + 13;     // springing height
    double archR = 7.0;       // radius of the semicircle
    int archCenterY = top;    // arch springs from column tops
    // The arch is built in the X=cx plane, curving over the Z span (-6..6).
    for (int dz = -7; dz <= 7; dz++) {
        // Semicircle: for each z offset, find the rise.
        double inside = archR * archR - (double) dz * dz;
        if (inside < 0) continue;
        int rise = (int) Math.round(Math.sqrt(inside));
        int y = archCenterY + rise - 1;
        int zz = cz + dz;
        // Voussoir ring (two blocks thick for solidity).
        set(es, cx, y, zz, "chiseled_quartz_block");
        set(es, cx, y + 1, zz, "gold_block");
        set(es, cx + 1, y, zz, "smooth_quartz");
        set(es, cx - 1, y, zz, "smooth_quartz");
        // Underside trim.
        if (rise > 1) {
            set(es, cx, y - 1, zz, "quartz_bricks");
        }
    }
    // Keystone emphasis at the top.
    int keyY = archCenterY + 7;
    set(es, cx, keyY, cz, "gold_block");
    set(es, cx, keyY + 1, cz, "chiseled_quartz_block");
    set(es, cx, keyY + 2, cz, "sea_lantern");
    set(es, cx + 1, keyY, cz, "gold_block");
    set(es, cx - 1, keyY, cz, "gold_block");

    // Entablature / lintel band connecting the inner columns at springing level.
    for (int dz = -6; dz <= 6; dz++) {
        set(es, cx, top, cz + dz, "smooth_quartz");
        set(es, cx, top - 1, cz + dz, "chiseled_quartz_block");
        set(es, cx + 1, top - 1, cz + dz, "quartz_bricks");
        set(es, cx - 1, top - 1, cz + dz, "quartz_bricks");
        if (dz % 2 == 0) set(es, cx, top + 1, cz + dz, "gold_block");
    }
    // Cornice running between the column pairs along X too.
    for (int dx = -4; dx <= 4; dx++) {
        set(es, cx + dx, top, cz - 6, "smooth_quartz");
        set(es, cx + dx, top, cz + 6, "smooth_quartz");
        set(es, cx + dx, top + 1, cz - 6, "quartz_slab[type=bottom]");
        set(es, cx + dx, top + 1, cz + 6, "quartz_slab[type=bottom]");
    }
}

// The luminous white/light_blue glass gate core within the arch.
private void buildPortalWestGateCore(EditSession es, int cx, int cz, int baseY) {
    int top = baseY + 13;
    double archR = 7.0;
    // Fill the arched opening with a layered glass curtain in the X=cx plane.
    for (int dz = -6; dz <= 6; dz++) {
        double inside = archR * archR - (double) dz * dz;
        int rise = inside < 0 ? 0 : (int) Math.round(Math.sqrt(inside));
        int yTop = top + rise - 2;
        int zz = cz + dz;
        for (int y = baseY + 1; y <= yTop; y++) {
            double n = noise(zz * 5, y * 5, 4242L);
            String glass;
            if (Math.abs(dz) <= 2 && y >= baseY + 4 && y <= baseY + 11) {
                // Bright central panel.
                glass = (n > 0.6) ? "light_blue_stained_glass" : "white_stained_glass";
            } else {
                glass = (n > 0.7) ? "white_stained_glass"
                        : (n > 0.4 ? "light_blue_stained_glass" : "tinted_glass");
            }
            set(es, cx, y, zz, glass);
        }
    }
    // Glowing core seam down the center to read as a "portal" energy line.
    for (int y = baseY + 1; y <= top + 4; y++) {
        if (y % 3 == 0) set(es, cx, y, cz, "sea_lantern");
        else set(es, cx, y, cz, "light_blue_stained_glass");
    }
    // Mullions: vertical and horizontal gold bars dividing the glass.
    for (int y = baseY + 1; y <= top + 4; y += 3) {
        for (int dz = -6; dz <= 6; dz++) {
            double inside = archR * archR - (double) dz * dz;
            int rise = inside < 0 ? 0 : (int) Math.round(Math.sqrt(inside));
            if (y <= top + rise - 2) set(es, cx, y, cz + dz, "gold_block");
        }
    }
    for (int dz = -6; dz <= 6; dz += 3) {
        column(es, cx, cz + dz, baseY + 1, top - 1, "gold_block");
    }
    // Threshold step under the gate.
    fill(es, cx, baseY + 1, cz - 3, cx, baseY + 1, cz + 3, "polished_diorite");
    fill(es, cx + 1, baseY + 1, cz - 3, cx + 1, baseY + 1, cz + 3,
            "quartz_stairs[facing=west,half=bottom]");
    fill(es, cx - 1, baseY + 1, cz - 3, cx - 1, baseY + 1, cz + 3,
            "quartz_stairs[facing=east,half=bottom]");
}

// The domed quartz roof crowning the pavilion.
private void buildPortalWestDome(EditSession es, int cx, int cz, int baseY) {
    int domeBase = baseY + 16;
    int domeCenterY = domeBase;
    double rx = 9.0, ry = 8.0, rz = 11.0;
    // Solid drum ring the dome sits on.
    tube(es, cx, domeBase - 1, cz, 8.5, 9.5, 2, "chiseled_quartz_block");
    ring(es, cx, domeBase, cz, 7.5, 9.5, "gold_block");

    // Hollow dome shell using a half-ellipsoid, carved by hand for control.
    for (int dx = -10; dx <= 10; dx++) {
        for (int dz = -12; dz <= 12; dz++) {
            for (int dy = 0; dy <= 9; dy++) {
                double v = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                if (v <= 1.0 && v >= 0.74) {
                    int x = cx + dx, y = domeCenterY + dy, z = cz + dz;
                    double n = noise(x, z, 9091L);
                    String mat;
                    // Gold ribs along principal meridians.
                    if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) mat = "gold_block";
                    else if (n > 0.85) mat = "chiseled_quartz_block";
                    else if (n < 0.12) mat = "smooth_quartz";
                    else mat = "quartz_block";
                    set(es, x, y, z, mat);
                }
            }
        }
    }
    // Oculus crown and finial.
    int crown = domeCenterY + 8;
    disk(es, cx, crown, cz, 2.2, "gold_block");
    set(es, cx, crown, cz, "sea_lantern");
    column(es, cx, cz, crown + 1, crown + 3, "chiseled_quartz_block");
    set(es, cx, crown + 4, cz, "gold_block");
    set(es, cx, crown + 5, cz, "sea_lantern");
    set(es, cx, crown + 6, cz, "lantern[hanging=false]");

    // Pendant chains hanging from the dome eaves.
    for (int a = 0; a < 360; a += 45) {
        double rad = Math.toRadians(a);
        int hx = cx + (int) Math.round(Math.cos(rad) * 8.0);
        int hz = cz + (int) Math.round(Math.sin(rad) * 10.0);
        set(es, hx, domeBase - 1, hz, "chain[axis=y]");
        set(es, hx, domeBase - 2, hz, "chain[axis=y]");
        set(es, hx, domeBase - 3, hz, "lantern[hanging=true]");
    }
}

// Final accent lighting layer: sea_lantern glow nodes and lantern clusters.
private void buildPortalWestLighting(EditSession es, int cx, int cz, int baseY) {
    // Glow-stone-free uplighting embedded around the dais perimeter.
    for (int a = 0; a < 360; a += 30) {
        double rad = Math.toRadians(a);
        int gx = cx + (int) Math.round(Math.cos(rad) * 10.0);
        int gz = cz + (int) Math.round(Math.sin(rad) * 10.0);
        set(es, gx, baseY, gz, "sea_lantern");
        // Small quartz post with a lantern, with noise-driven height variation.
        double n = noise(gx, gz, 3131L);
        int h = 2 + (int) Math.round(n * 2);
        column(es, gx, gz, baseY + 1, baseY + h, "quartz_wall");
        set(es, gx, baseY + h + 1, gz, "lantern[hanging=false]");
    }
    // Light_blue glass lamp-globes mounted on the mini-pillars' outer faces.
    int[][] globes = {
            {cx + 8, cz - 9}, {cx + 8, cz + 9},
            {cx - 8, cz - 9}, {cx - 8, cz + 9}
    };
    for (int[] g : globes) {
        set(es, g[0], baseY + 11, g[1], "sea_lantern");
        set(es, g[0], baseY + 12, g[1], "light_blue_stained_glass");
    }
    // Ground-flush runway of lights guiding from the island center to the gate.
    for (int dx = -3; dx >= -12; dx -= 3) {
        set(es, cx + 0, baseY, cz, "sea_lantern");
        set(es, cx + 0, baseY, cz - 3, "sea_lantern");
        set(es, cx + 0, baseY, cz + 3, "sea_lantern");
    }
    // A pair of standing braziers flanking the gate threshold.
    for (int side = -1; side <= 1; side += 2) {
        int bz = cz + side * 5;
        column(es, cx + 5, bz, baseY + 1, baseY + 3, "chiseled_quartz_block");
        set(es, cx + 5, baseY + 4, bz, "gold_block");
        set(es, cx + 5, baseY + 5, bz, "sea_lantern");
        set(es, cx + 5, baseY + 6, bz, "fire");
    }
}

    private void buildRingRoad(EditSession es) {
    // Grand circular boulevard at radius ~30..36, plus four radial avenues to the gates.
    final int cx = 0, cz = 0;
    final double roadInner = 30.0;
    final double roadOuter = 36.0;
    final long seed = 0x10BBEEEDL;

    // ---- 1. Base ground layer beneath the boulevard so nothing floats ----
    // A slightly thicker stone bed under the ring, with stone_bricks foundation.
    for (int x = -38; x <= 38; x++) {
        for (int z = -38; z <= 38; z++) {
            double d = Math.sqrt(x * (double) x + z * (double) z);
            if (d >= roadInner - 1.5 && d <= roadOuter + 1.5) {
                // foundation
                set(es, x, SY - 2, z, "stone");
                set(es, x, SY - 1, z, "stone_bricks");
            }
        }
    }

    // ---- 2. The boulevard surface: polished_andesite with noise-broken trim ----
    for (int x = -38; x <= 38; x++) {
        for (int z = -38; z <= 38; z++) {
            double d = Math.sqrt(x * (double) x + z * (double) z);
            if (d >= roadInner && d <= roadOuter) {
                double n = noise(x, z, seed);
                String surface;
                if (d <= roadInner + 1.0 || d >= roadOuter - 1.0) {
                    // curbs handled separately; inner band trim
                    surface = "stone_bricks";
                } else if (n < 0.08) {
                    surface = "cracked_stone_bricks";
                } else if (n < 0.16) {
                    surface = "andesite";
                } else {
                    surface = "polished_andesite";
                }
                set(es, x, SY, z, surface);
            }
        }
    }

    // ---- 3. Smooth_stone curbs on both edges of the ring ----
    ring(es, cx, SY, cz, roadInner - 0.6, roadInner + 0.4, "smooth_stone");
    ring(es, cx, SY, cz, roadOuter - 0.4, roadOuter + 0.6, "smooth_stone");
    // raised curb lip (slabs) for a finished edge
    ring(es, cx, SY + 1, cz, roadInner - 0.6, roadInner + 0.2, "smooth_stone_slab[type=bottom]");
    ring(es, cx, SY + 1, cz, roadOuter - 0.2, roadOuter + 0.6, "smooth_stone_slab[type=bottom]");

    // ---- 4. Stone_brick trim line down the middle of the boulevard ----
    ring(es, cx, SY, cz, 32.5, 33.5, "stone_bricks");
    // decorative chiseled accents along the centre, noise-spaced
    for (int a = 0; a < 360; a += 6) {
        double rad = Math.toRadians(a);
        int tx = (int) Math.round(33.0 * Math.cos(rad));
        int tz = (int) Math.round(33.0 * Math.sin(rad));
        if (noise(tx, tz, seed + 7) < 0.5) {
            set(es, tx, SY, tz, "chiseled_stone_bricks");
        }
    }

    // ---- 5. Four radial avenues out to the portals (N, E, S, W) ----
    // Each avenue is a straight paved strip from the inner ring out to radius ~64.
    buildRingRoadAvenue(es, 0, 1, seed);   // +Z (south)
    buildRingRoadAvenue(es, 0, -1, seed);  // -Z (north)
    buildRingRoadAvenue(es, 1, 0, seed);   // +X (east)
    buildRingRoadAvenue(es, -1, 0, seed);  // -X (west)

    // ---- 6. Lamp posts regularly spaced around the ring (oak/iron alternating) ----
    int lampCount = 16;
    for (int i = 0; i < lampCount; i++) {
        double rad = (Math.PI * 2.0 / lampCount) * i;
        // place just inside the outer curb
        int lx = (int) Math.round((roadOuter - 1.5) * Math.cos(rad));
        int lz = (int) Math.round((roadOuter - 1.5) * Math.sin(rad));
        boolean iron = (i % 2 == 0);
        buildRingRoadLamp(es, lx, lz, iron);
        // matching lamp on the inner edge
        int ix = (int) Math.round((roadInner + 1.5) * Math.cos(rad));
        int iz = (int) Math.round((roadInner + 1.5) * Math.sin(rad));
        buildRingRoadLamp(es, ix, iz, !iron);
    }

    // ---- 7. Benches between lamp posts (stairs + slab seat) ----
    int benchCount = 8;
    for (int i = 0; i < benchCount; i++) {
        double rad = (Math.PI * 2.0 / benchCount) * i + (Math.PI / benchCount);
        int bx = (int) Math.round((roadOuter - 1.5) * Math.cos(rad));
        int bz = (int) Math.round((roadOuter - 1.5) * Math.sin(rad));
        // orient bench facing the centre based on dominant axis
        boolean alongZ = Math.abs(Math.cos(rad)) > Math.abs(Math.sin(rad));
        buildRingRoadBench(es, bx, bz, alongZ);
    }

    // ---- 8. Decorative planters interspersed around the ring ----
    int planterCount = 8;
    for (int i = 0; i < planterCount; i++) {
        double rad = (Math.PI * 2.0 / planterCount) * i + (Math.PI / (planterCount * 2));
        int px = (int) Math.round((roadInner + 1.8) * Math.cos(rad));
        int pz = (int) Math.round((roadInner + 1.8) * Math.sin(rad));
        buildRingRoadPlanter(es, px, pz, seed + i * 13L);
    }
}

private void buildRingRoadAvenue(EditSession es, int dx, int dz, long seed) {
    // Build a paved avenue from radius 28 out to radius 64 in direction (dx,dz).
    // Width 5 (perpendicular offset -2..2).
    for (int r = 28; r <= 64; r++) {
        for (int w = -3; w <= 3; w++) {
            int x = dx * r + (dz != 0 ? w : 0);
            int z = dz * r + (dx != 0 ? w : 0);
            int adx = x, adz = z;
            if (!inDisk(adx, adz, R + 1)) continue;
            // foundation
            set(es, x, SY - 2, z, "stone");
            set(es, x, SY - 1, z, "stone_bricks");
            double n = noise(x, z, seed + 3);
            String surface;
            if (Math.abs(w) == 3) {
                surface = "smooth_stone"; // curb
            } else if (Math.abs(w) == 2) {
                surface = "stone_bricks"; // trim
            } else if (n < 0.1) {
                surface = "andesite";
            } else if (n < 0.18) {
                surface = "cracked_stone_bricks";
            } else {
                surface = "polished_andesite";
            }
            set(es, x, SY, z, surface);
        }
        // raised slab lip on the curbs
        if (r % 1 == 0) {
            int lxA = dx * r + (dz != 0 ? 3 : 0);
            int lzA = dz * r + (dx != 0 ? 3 : 0);
            int lxB = dx * r + (dz != 0 ? -3 : 0);
            int lzB = dz * r + (dx != 0 ? -3 : 0);
            if (inDisk(lxA, lzA, R + 1)) set(es, lxA, SY + 1, lzA, "smooth_stone_slab[type=bottom]");
            if (inDisk(lxB, lzB, R + 1)) set(es, lxB, SY + 1, lzB, "smooth_stone_slab[type=bottom]");
        }
        // lamp posts every 8 blocks along the avenue
        if (r % 8 == 0 && r >= 36) {
            int lx1 = dx * r + (dz != 0 ? 2 : 0);
            int lz1 = dz * r + (dx != 0 ? 2 : 0);
            int lx2 = dx * r + (dz != 0 ? -2 : 0);
            int lz2 = dz * r + (dx != 0 ? -2 : 0);
            if (inDisk(lx1, lz1, R)) buildRingRoadLamp(es, lx1, lz1, (r / 8) % 2 == 0);
            if (inDisk(lx2, lz2, R)) buildRingRoadLamp(es, lx2, lz2, (r / 8) % 2 == 1);
        }
    }
}

private void buildRingRoadLamp(EditSession es, int x, int z, boolean iron) {
    // Lamp post: base, column, lantern hung on chain, with side brackets.
    String post = iron ? "iron_bars" : "oak_fence";
    String baseBlock = iron ? "polished_andesite" : "oak_log[axis=y]";
    // decorative base ring
    set(es, x, SY + 1, z, baseBlock);
    set(es, x, SY + 1, z, baseBlock);
    // skirt slabs around the base
    set(es, x + 1, SY + 1, z, "stone_brick_slab[type=bottom]");
    set(es, x - 1, SY + 1, z, "stone_brick_slab[type=bottom]");
    set(es, x, SY + 1, z + 1, "stone_brick_slab[type=bottom]");
    set(es, x, SY + 1, z - 1, "stone_brick_slab[type=bottom]");
    // column
    column(es, x, z, SY + 2, SY + 5, post);
    // crossbar / cap
    set(es, x, SY + 6, z, baseBlock);
    // chain + hanging lantern
    set(es, x, SY + 6, z, "chain[axis=y]");
    set(es, x, SY + 5, z, "lantern[hanging=true]");
    // top finial
    set(es, x, SY + 7, z, iron ? "lantern[hanging=false]" : "sea_lantern");
}

private void buildRingRoadBench(EditSession es, int x, int z, boolean alongZ) {
    // Bench: a run of stairs as the backrest + slabs as the seat, 3 long.
    if (alongZ) {
        // bench runs along Z, faces toward lower X (centre side determined loosely)
        for (int o = -1; o <= 1; o++) {
            set(es, x, SY + 1, z + o, "oak_slab[type=bottom]");
            set(es, x + 1, SY + 1, z + o, "oak_stairs[facing=west,half=bottom]");
        }
        // arm rests
        set(es, x, SY + 2, z - 1, "oak_fence");
        set(es, x, SY + 2, z + 1, "oak_fence");
    } else {
        for (int o = -1; o <= 1; o++) {
            set(es, x + o, SY + 1, z, "oak_slab[type=bottom]");
            set(es, x + o, SY + 1, z + 1, "oak_stairs[facing=north,half=bottom]");
        }
        set(es, x - 1, SY + 2, z, "oak_fence");
        set(es, x + 1, SY + 2, z, "oak_fence");
    }
}

private void buildRingRoadPlanter(EditSession es, int x, int z, long seed) {
    // 3x3 raised planter box with brick walls, dirt, and noise-varied foliage.
    walls(es, x - 1, SY + 1, z - 1, x + 1, SY + 1, z + 1, "mossy_stone_bricks");
    // corner posts
    set(es, x - 1, SY + 2, z - 1, "stone_brick_wall");
    set(es, x + 1, SY + 2, z - 1, "stone_brick_wall");
    set(es, x - 1, SY + 2, z + 1, "stone_brick_wall");
    set(es, x + 1, SY + 2, z + 1, "stone_brick_wall");
    // soil + planting
    for (int ox = -1; ox <= 1; ox++) {
        for (int oz = -1; oz <= 1; oz++) {
            if (ox == 0 && oz == 0) {
                // centre gets a small shrub/sapling
                set(es, x, SY + 1, z, "dirt");
                double n = noise(x, z, seed);
                if (n < 0.4) {
                    set(es, x, SY + 2, z, "oak_leaves[persistent=true]");
                    set(es, x, SY + 3, z, "oak_leaves[persistent=true]");
                } else if (n < 0.7) {
                    set(es, x, SY + 2, z, "azalea");
                } else {
                    set(es, x, SY + 2, z, "flowering_azalea");
                }
            } else {
                set(es, x + ox, SY + 1, z + oz, "grass_block");
                double n = noise(x + ox * 31, z + oz * 17, seed + 5);
                if (n < 0.25) {
                    set(es, x + ox, SY + 2, z + oz, "poppy");
                } else if (n < 0.45) {
                    set(es, x + ox, SY + 2, z + oz, "dandelion");
                } else if (n < 0.6) {
                    set(es, x + ox, SY + 2, z + oz, "short_grass");
                }
            }
        }
    }
}

    private void buildGardenQuarter(EditSession es) {
    // ===== NE botanical garden quarter: centered ~ (34,64,-34) =====
    final int CX = 34, CZ = -34;
    final long SEED = 770341L;

    // ---- 1. Ground regrade: lush layered grass + podzol/dirt variation ----
    for (int dx = -18; dx <= 18; dx++) {
        for (int dz = -18; dz <= 18; dz++) {
            int x = CX + dx, z = CZ + dz;
            // stay on the island footprint
            if (!inDisk(x, z, R - 1)) continue;
            double n = noise(x, z, SEED);
            String top = "grass_block";
            if (n < 0.10) top = "podzol";
            else if (n < 0.16) top = "coarse_dirt";
            else if (n > 0.93) top = "moss_block";
            set(es, x, SY, z, top);
            // soil layers below for thickness
            fill(es, x, SY - 3, z, x, SY - 1, z, "dirt");
        }
    }

    // ---- 2. Gravel path network: a cross + a ring connecting features ----
    buildGardenQuarterPath(es, CX, CZ, SEED);

    // ---- 3. Geometric flower beds: four square beds framed by hedges ----
    int[][] beds = {
        { CX - 9, CZ - 9 },
        { CX + 8, CZ - 9 },
        { CX - 9, CZ + 8 },
        { CX + 8, CZ + 8 }
    };
    String[] flowers = {
        "poppy", "dandelion", "cornflower", "oxeye_daisy", "allium", "azure_bluet"
    };
    for (int b = 0; b < beds.length; b++) {
        buildGardenQuarterFlowerBed(es, beds[b][0], beds[b][1], 3, flowers, SEED + b * 31L);
    }

    // ---- 4. Trimmed leaf hedges between beds (low geometric walls) ----
    buildGardenQuarterHedges(es, CX, CZ, SEED);

    // ---- 5. Koi pond (offset to one corner of the quarter) ----
    buildGardenQuarterPond(es, CX - 10, CZ + 11, SEED + 555L);

    // ---- 6. Central quartz gazebo with domed roof ----
    buildGardenQuarterGazebo(es, CX, CZ, SEED + 99L);

    // ---- 7. Ornamental azalea trees scattered at corners ----
    buildGardenQuarterAzalea(es, CX + 13, CZ - 13, SEED + 7L);
    buildGardenQuarterAzalea(es, CX - 14, CZ - 3, SEED + 17L);
    buildGardenQuarterAzalea(es, CX + 2, CZ + 14, SEED + 27L);
    buildGardenQuarterAzalea(es, CX + 13, CZ + 12, SEED + 37L);

    // ---- 8. Edge planters / lanterns lining the outer rim ----
    buildGardenQuarterRimDecor(es, CX, CZ, SEED + 808L);
}

private void buildGardenQuarterPath(EditSession es, int cx, int cz, long seed) {
    // Cross path through center (to gazebo) using gravel with mossy speckle
    for (int d = -16; d <= 16; d++) {
        for (int w = -1; w <= 1; w++) {
            int[][] cells = { { cx + d, cz + w }, { cx + w, cz + d } };
            for (int[] c : cells) {
                int x = c[0], z = c[1];
                if (!inDisk(x, z, R - 1)) continue;
                double n = noise(x, z, seed);
                String pave = "gravel";
                if (n < 0.18) pave = "cobblestone";
                else if (n > 0.88) pave = "mossy_cobblestone";
                else if (n > 0.80) pave = "andesite";
                set(es, x, SY, z, pave);
            }
        }
    }
    // Decorative ring path around the gazebo
    for (int a = 0; a < 360; a += 3) {
        double rad = Math.toRadians(a);
        int rr = 6;
        int x = cx + (int) Math.round(Math.cos(rad) * rr);
        int z = cz + (int) Math.round(Math.sin(rad) * rr);
        if (!inDisk(x, z, R - 1)) continue;
        double n = noise(x, z, seed + 3L);
        set(es, x, SY, z, n > 0.7 ? "mossy_cobblestone" : "stone_bricks");
    }
}

private void buildGardenQuarterFlowerBed(EditSession es, int bx, int bz, int half, String[] flowers, long seed) {
    // Raised bed border of stripped logs + grass interior + dense flowers
    for (int dx = -half - 1; dx <= half + 1; dx++) {
        for (int dz = -half - 1; dz <= half + 1; dz++) {
            int x = bx + dx, z = bz + dz;
            if (!inDisk(x, z, R - 1)) continue;
            boolean border = (Math.abs(dx) == half + 1 || Math.abs(dz) == half + 1);
            if (border) {
                set(es, x, SY, z, "stripped_oak_log[axis=y]");
                set(es, x, SY + 1, z, "stripped_oak_log[axis=y]");
            } else {
                set(es, x, SY, z, "grass_block");
                double n = noise(x, z, seed);
                if (n > 0.20) {
                    // pick a flower deterministically
                    int idx = (int) (noise(x * 3, z * 7, seed + 11L) * flowers.length) % flowers.length;
                    if (idx < 0) idx = 0;
                    String f = flowers[idx];
                    set(es, x, SY + 1, z, f);
                } else if (n > 0.10) {
                    set(es, x, SY + 1, z, "short_grass");
                }
            }
        }
    }
    // corner lantern posts on the bed border
    int[][] corners = {
        { bx - half - 1, bz - half - 1 }, { bx + half + 1, bz - half - 1 },
        { bx - half - 1, bz + half + 1 }, { bx + half + 1, bz + half + 1 }
    };
    for (int[] c : corners) {
        if (!inDisk(c[0], c[1], R - 1)) continue;
        set(es, c[0], SY + 2, c[1], "oak_fence");
        set(es, c[0], SY + 3, c[1], "lantern[hanging=true]");
    }
}

private void buildGardenQuarterHedges(EditSession es, int cx, int cz, long seed) {
    // Geometric low hedges forming an inner square frame around the gazebo
    int half = 11;
    for (int d = -half; d <= half; d++) {
        int[][] edges = {
            { cx + d, cz - half }, { cx + d, cz + half },
            { cx - half, cz + d }, { cx + half, cz + d }
        };
        for (int[] e : edges) {
            int x = e[0], z = e[1];
            if (!inDisk(x, z, R - 1)) continue;
            // leave gaps where the cross path crosses
            if (Math.abs(x - cx) <= 1 || Math.abs(z - cz) <= 1) continue;
            double n = noise(x, z, seed);
            int h = n > 0.7 ? 3 : 2;
            for (int yy = 0; yy < h; yy++) {
                String leaf = (noise(x, z + yy, seed + 5L) > 0.85)
                        ? "flowering_azalea_leaves[persistent=true]"
                        : "oak_leaves[persistent=true]";
                set(es, x, SY + 1 + yy, z, leaf);
            }
        }
    }
}

private void buildGardenQuarterPond(EditSession es, int px, int pz, long seed) {
    double r = 5.0;
    // dig the basin & line it
    for (int dx = -6; dx <= 6; dx++) {
        for (int dz = -6; dz <= 6; dz++) {
            int x = px + dx, z = pz + dz;
            if (!inDisk(x, z, R - 1)) continue;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist <= r) {
                // basin floor lined with stone/clay, water on top
                set(es, x, SY - 2, z, "stone");
                set(es, x, SY - 1, z, dist < r - 1.5 ? "blue_terracotta" : "clay");
                set(es, x, SY, z, "water");
            } else if (dist <= r + 1.2) {
                // mossy stone rim
                double n = noise(x, z, seed);
                set(es, x, SY, z, n > 0.5 ? "mossy_cobblestone" : "cobblestone");
                if (n > 0.85) set(es, x, SY + 1, z, "cobblestone_wall");
            }
        }
    }
    // lily pads + sea grass + the "koi" represented as colored accents under water edge
    for (int dx = -5; dx <= 5; dx++) {
        for (int dz = -5; dz <= 5; dz++) {
            int x = px + dx, z = pz + dz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > r - 0.5) continue;
            double n = noise(x * 5, z * 5, seed + 13L);
            if (n > 0.82) set(es, x, SY + 1, z, "lily_pad");
            else if (n < 0.10) set(es, x, SY - 1, z, "orange_concrete"); // koi glimmer
            else if (n < 0.16) set(es, x, SY - 1, z, "white_concrete");
        }
    }
    // a tiny arched stone bridge over the pond
    for (int t = -5; t <= 5; t++) {
        int x = px + t, z = pz;
        if (!inDisk(x, z, R - 1)) continue;
        int lift = (Math.abs(t) >= 4) ? 0 : 1;
        set(es, x, SY + lift, z, "stone_brick_slab[type=top]");
        if (Math.abs(t) == 5) set(es, x, SY + 1, z, "cobblestone_wall");
    }
}

private void buildGardenQuarterGazebo(EditSession es, int cx, int cz, long seed) {
    int baseY = SY;
    double floorR = 4.0;
    // raised quartz platform
    disk(es, cx, baseY, cz, floorR + 0.5, "smooth_quartz");
    ring(es, cx, baseY, cz, floorR - 0.5, floorR + 0.6, "quartz_bricks");
    // step up to the gazebo floor
    disk(es, cx, baseY + 1, cz, floorR - 0.8, "quartz_block");
    // six pillars around the perimeter
    int pillars = 6;
    for (int i = 0; i < pillars; i++) {
        double a = Math.toRadians(i * (360.0 / pillars));
        int x = cx + (int) Math.round(Math.cos(a) * floorR);
        int z = cz + (int) Math.round(Math.sin(a) * floorR);
        column(es, x, z, baseY + 1, baseY + 5, "quartz_pillar[axis=y]");
        // capital + base trim
        set(es, x, baseY + 1, z, "chiseled_quartz_block");
        set(es, x, baseY + 5, z, "chiseled_quartz_block");
        // hanging lantern between pillars
        set(es, x, baseY + 4, z, "quartz_block");
    }
    // entablature ring at top of pillars
    ring(es, cx, baseY + 6, cz, floorR - 0.3, floorR + 0.6, "smooth_quartz");
    // domed roof: stacked shrinking rings of quartz stairs -> sea_lantern apex
    int domeBase = baseY + 6;
    double[] radii = { 4.3, 3.6, 2.8, 1.9, 1.0 };
    String[] domeMat = {
        "quartz_stairs[half=bottom]", "smooth_quartz", "quartz_bricks",
        "smooth_quartz", "chiseled_quartz_block"
    };
    for (int layer = 0; layer < radii.length; layer++) {
        int y = domeBase + 1 + layer;
        ring(es, cx, y, cz, Math.max(0, radii[layer] - 1.2), radii[layer], domeMat[layer]);
    }
    // apex finial + light
    set(es, cx, domeBase + radii.length + 1, cz, "sea_lantern");
    set(es, cx, domeBase + radii.length + 2, cz, "quartz_slab[type=bottom]");
    set(es, cx, domeBase + 1, cz, "sea_lantern"); // ceiling light under dome

    // central decorative feature: small fountain basin
    set(es, cx, baseY + 1, cz, "quartz_slab[type=top]");
    // benches inside (quartz stairs facing center on 4 sides)
    set(es, cx + 2, baseY + 1, cz, "quartz_stairs[facing=west,half=bottom]");
    set(es, cx - 2, baseY + 1, cz, "quartz_stairs[facing=east,half=bottom]");
    set(es, cx, baseY + 1, cz + 2, "quartz_stairs[facing=north,half=bottom]");
    set(es, cx, baseY + 1, cz - 2, "quartz_stairs[facing=south,half=bottom]");
    // hanging lanterns from entablature
    for (int i = 0; i < pillars; i++) {
        double a = Math.toRadians(i * (360.0 / pillars) + 30);
        int x = cx + (int) Math.round(Math.cos(a) * (floorR - 1));
        int z = cz + (int) Math.round(Math.sin(a) * (floorR - 1));
        set(es, x, baseY + 5, z, "lantern[hanging=true]");
    }
}

private void buildGardenQuarterAzalea(EditSession es, int x, int z, long seed) {
    if (!inDisk(x, z, R - 1)) return;
    // small ornamental azalea tree: short twisted log trunk + flowering canopy
    int trunkH = 2 + (int) (noise(x, z, seed) * 2);
    set(es, x, SY, z, "rooted_dirt");
    for (int yy = 1; yy <= trunkH; yy++) {
        set(es, x, SY + yy, z, "oak_log[axis=y]");
    }
    int top = SY + trunkH;
    // canopy as a small flowering-azalea blob
    for (int dx = -2; dx <= 2; dx++) {
        for (int dz = -2; dz <= 2; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
                int lx = x + dx, ly = top + dy, lz = z + dz;
                double dist = Math.sqrt(dx * dx + (dy - 1) * (dy - 1) * 1.3 + dz * dz);
                if (dist > 2.3) continue;
                double n = noise(lx + dy, lz + dy, seed + 3L);
                String leaf = n > 0.55
                        ? "flowering_azalea_leaves[persistent=true]"
                        : "azalea_leaves[persistent=true]";
                set(es, lx, ly, lz, leaf);
            }
        }
    }
    // a few moss & bushes at the base
    set(es, x + 1, SY, z, "moss_block");
    set(es, x, SY, z + 1, "moss_block");
}

private void buildGardenQuarterRimDecor(EditSession es, int cx, int cz, long seed) {
    // Outer ornamental planters + lantern posts following the island rim arc
    for (int a = -10; a <= 80; a += 8) {
        double rad = Math.toRadians(a);
        int rr = R - 4;
        int x = (int) Math.round(Math.cos(rad) * rr);
        int z = (int) Math.round(Math.sin(rad) * rr);
        // only keep those that land in our NE quadrant footprint
        if (x < cx - 20 || x > cx + 20 || z < cz - 20 || z > cz + 20) continue;
        if (!inDisk(x, z, R - 1)) continue;
        // planter box of mossy bricks with a flower / bush on top
        set(es, x, SY + 1, z, "mossy_stone_bricks");
        double n = noise(x, z, seed);
        if (n > 0.66) set(es, x, SY + 2, z, "azalea_bush");
        else if (n > 0.33) set(es, x, SY + 2, z, "allium");
        else set(es, x, SY + 2, z, "oxeye_daisy");
        // tall lantern post beside the planter
        if (noise(x, z, seed + 5L) > 0.5) {
            column(es, x, z + 1, SY + 1, SY + 3, "cobblestone_wall");
            set(es, x, SY + 4, z + 1, "lantern[hanging=true]");
        }
    }
    // small leaf topiary balls at two focal corners
    int[][] topi = { { cx + 15, cz - 8 }, { cx - 8, cz + 15 } };
    for (int[] t : topi) {
        if (!inDisk(t[0], t[1], R - 1)) continue;
        column(es, t[0], t[1], SY + 1, SY + 2, "oak_log[axis=y]");
        sphere(es, t[0], SY + 4, t[1], 1.8, "oak_leaves[persistent=true]");
        set(es, t[0], SY + 4, t[1], "flowering_azalea_leaves[persistent=true]");
    }
}

    private void buildMarketQuarter(EditSession es) {
    // ===== NW quadrant market quarter, centered ~(-34,64,-34) =====
    final int cx = -34, cz = -34;

    // ---------- Ground prep: tamp surface, lay plaza base ----------
    for (int x = cx - 18; x <= cx + 18; x++) {
        for (int z = cz - 18; z <= cz + 18; z++) {
            if (!inDisk(x, z, R - 2)) continue;
            double n = noise(x, z, 70001L);
            // patchy dirt/coarse under the market floor
            if (n < 0.18) set(es, x, SY, z, "coarse_dirt");
            else if (n < 0.30) set(es, x, SY, z, "podzol");
        }
    }

    // ---------- Cobblestone path network ----------
    // Main spine running NW->SE through the quarter
    buildMarketQuarterPath(es, cx - 15, cz - 15, cx + 15, cz + 15, 2.0);
    // Cross street
    buildMarketQuarterPath(es, cx - 14, cz + 8, cx + 14, cz - 8, 1.6);
    // Path to the well
    buildMarketQuarterPath(es, cx, cz, cx + 2, cz - 12, 1.4);

    // ---------- Central well ----------
    buildMarketQuarterWell(es, cx + 2, cz - 10);

    // ---------- Row of merchant stalls along the spine ----------
    // Place several stalls with varied awning colors and facing
    String[][] palettes = {
        {"red_wool", "red_terracotta"},
        {"yellow_wool", "orange_terracotta"},
        {"white_wool", "light_blue_terracotta"},
        {"green_wool", "lime_terracotta"},
        {"magenta_wool", "purple_terracotta"},
    };
    int sIdx = 0;
    for (int i = 0; i < 4; i++) {
        int sx = cx - 12 + i * 8;
        int sz = cz - 6;
        buildMarketQuarterStall(es, sx, sz, true, palettes[sIdx % palettes.length]);
        sIdx++;
    }
    for (int i = 0; i < 3; i++) {
        int sx = cx - 8 + i * 9;
        int sz = cz + 9;
        buildMarketQuarterStall(es, sx, sz, false, palettes[sIdx % palettes.length]);
        sIdx++;
    }

    // ---------- Lantern-string posts along the spine ----------
    int[][] postLine = {
        {cx - 14, cz - 14}, {cx - 6, cz - 6}, {cx + 2, cz + 2}, {cx + 10, cz + 10}, {cx + 14, cz + 14}
    };
    for (int p = 0; p < postLine.length; p++) {
        int px = postLine[p][0], pz = postLine[p][1];
        column(es, px, pz, SY + 1, SY + 5, "spruce_fence");
        set(es, px, SY + 5, pz, "spruce_fence");
        set(es, px, SY + 6, pz, "lantern[hanging=true]");
    }
    // Strings of chains + lanterns between consecutive posts
    for (int p = 0; p + 1 < postLine.length; p++) {
        buildMarketQuarterLanternString(es,
            postLine[p][0], postLine[p][1],
            postLine[p + 1][0], postLine[p + 1][1], SY + 5);
    }

    // ---------- Stacks of barrels and crates around the plaza ----------
    int[][] stacks = {
        {cx - 13, cz - 9}, {cx - 4, cz - 10}, {cx + 6, cz - 9},
        {cx - 11, cz + 12}, {cx + 1, cz + 13}, {cx + 11, cz + 5},
        {cx + 13, cz - 2}, {cx - 15, cz + 2}
    };
    for (int s = 0; s < stacks.length; s++) {
        long seed = 88000L + s * 131L;
        buildMarketQuarterCrateStack(es, stacks[s][0], stacks[s][1], seed);
    }

    // ---------- Hay bales scattered as seating / cargo ----------
    int[][] hay = {
        {cx - 9, cz - 12}, {cx - 8, cz - 12}, {cx - 8, cz - 11},
        {cx + 9, cz + 11}, {cx + 10, cz + 11},
        {cx - 14, cz + 6}, {cx + 14, cz + 1}
    };
    for (int h = 0; h < hay.length; h++) {
        int hx = hay[h][0], hz = hay[h][1];
        String axis = (noise(hx, hz, 4242L) < 0.5) ? "x" : "z";
        set(es, hx, SY + 1, hz, "hay_block[axis=" + axis + "]");
        if (noise(hx, hz, 9090L) > 0.55) {
            set(es, hx, SY + 2, hz, "hay_block[axis=y]");
        }
    }

    // ---------- Scattered decorative ground clutter ----------
    scatter(es, cx, SY + 1, cz, 16.0, 0.04, 55501L,
        "grass", "grass", "poppy", "dandelion", "air");

    // ---------- A couple of decorative trees / market greenery ----------
    buildMarketQuarterTree(es, cx - 16, cz - 2);
    buildMarketQuarterTree(es, cx + 4, cz + 16);

    // ---------- Banner-style trim flags on a tall central mast ----------
    buildMarketQuarterMast(es, cx - 1, cz + 1);
}

private void buildMarketQuarterPath(EditSession es, int x0, int z0, int x1, int z1, double r) {
    // Walk a thick cobble line at surface level, with mossy/gravel variation
    int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
    if (steps == 0) steps = 1;
    for (int s = 0; s <= steps; s++) {
        double t = (double) s / steps;
        int px = (int) Math.round(x0 + (x1 - x0) * t);
        int pz = (int) Math.round(z0 + (z1 - z0) * t);
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                int wx = px + dx, wz = pz + dz;
                if (!inDisk(wx, wz, R - 2)) continue;
                double n = noise(wx, wz, 31337L);
                String id;
                if (n < 0.15) id = "mossy_cobblestone";
                else if (n < 0.28) id = "gravel";
                else if (n < 0.40) id = "cobblestone_slab[type=top]";
                else id = "cobblestone";
                set(es, wx, SY, wz, id);
            }
        }
    }
}

private void buildMarketQuarterWell(EditSession es, int cx, int cz) {
    // Stone ring base
    ring(es, cx, SY, cz, 1.4, 2.6, "cobblestone");
    ring(es, cx, SY + 1, cz, 1.4, 2.3, "stone_bricks");
    // inner water shaft
    cyl(es, cx, SY, cz, 1.3, 1, "water");
    set(es, cx, SY, cz, "water");
    // four corner posts holding a little roof
    int[][] corners = {{cx - 1, cz - 1}, {cx + 1, cz - 1}, {cx - 1, cz + 1}, {cx + 1, cz + 1}};
    for (int[] c : corners) {
        column(es, c[0], c[1], SY + 1, SY + 4, "spruce_log[axis=y]");
    }
    // ring beam
    walls(es, cx - 1, SY + 4, cz - 1, cx + 1, SY + 4, cz + 1, "spruce_fence");
    // peaked roof from stairs
    for (int dx = -2; dx <= 2; dx++) {
        for (int dz = -2; dz <= 2; dz++) {
            int wx = cx + dx, wz = cz + dz;
            int dist = Math.max(Math.abs(dx), Math.abs(dz));
            int ry = SY + 5 + (2 - dist);
            String face;
            if (Math.abs(dx) >= Math.abs(dz)) face = dx < 0 ? "east" : "west";
            else face = dz < 0 ? "south" : "north";
            if (dist == 2) set(es, wx, ry, wz, "dark_oak_stairs[facing=" + face + ",half=bottom]");
            else if (dist == 1) set(es, wx, ry, wz, "dark_oak_planks");
        }
    }
    set(es, cx, SY + 8, cz, "dark_oak_slab[type=bottom]");
    // hanging bucket apparatus
    set(es, cx, SY + 4, cz, "spruce_fence");
    set(es, cx, SY + 3, cz, "chain[axis=y]");
    set(es, cx, SY + 2, cz, "cauldron[level=2]");
    // lanterns on the corner posts
    for (int[] c : corners) {
        set(es, c[0], SY + 3, c[1], "lantern[hanging=true]");
    }
}

private void buildMarketQuarterStall(EditSession es, int cx, int cz, boolean facingNorth, String[] palette) {
    String wool = palette[0];
    String terra = palette[1];
    String stairColor = wool.replace("_wool", "");
    // stall footprint 5 wide x 3 deep
    int hw = 2; // half width
    int depth = 2;
    // corner support logs
    int z0 = cz - (facingNorth ? 0 : depth);
    int z1 = cz + (facingNorth ? depth : 0);
    for (int sx = cx - hw; sx <= cx + hw; sx += hw * 2) {
        column(es, sx, z0, SY + 1, SY + 4, "oak_log[axis=y]");
        column(es, sx, z1, SY + 1, SY + 4, "oak_log[axis=y]");
    }
    // back wall (planks) at far side
    int backZ = facingNorth ? z1 : z0;
    for (int sx = cx - hw; sx <= cx + hw; sx++) {
        column(es, sx, backZ, SY + 1, SY + 3, "oak_planks");
    }
    // counter (front) with slab tabletop
    int frontZ = facingNorth ? z0 : z1;
    for (int sx = cx - hw; sx <= cx + hw; sx++) {
        set(es, sx, SY + 1, frontZ, "oak_planks");
        set(es, sx, SY + 2, frontZ, "oak_slab[type=top]");
    }
    // top beams
    line(es, cx - hw, SY + 4, z0, cx + hw, SY + 4, z0, "oak_log[axis=x]");
    line(es, cx - hw, SY + 4, z1, cx + hw, SY + 4, z1, "oak_log[axis=x]");
    line(es, cx - hw, SY + 4, z0, cx - hw, SY + 4, z1, "oak_fence");
    line(es, cx + hw, SY + 4, z0, cx + hw, SY + 4, z1, "oak_fence");

    // ----- striped awning made of stairs + wool, sloping toward front -----
    String awnFace = facingNorth ? "north" : "south";
    for (int sx = cx - hw - 1; sx <= cx + hw + 1; sx++) {
        // stripe alternates wool color vs terracotta
        boolean alt = ((sx - (cx - hw - 1)) % 2 == 0);
        String stripeStair = alt ? (stairColor + "_stairs") : "stripped_oak_wood";
        // overhang row out front
        int oz = facingNorth ? (z0 - 1) : (z1 + 1);
        set(es, sx, SY + 4, oz, (alt ? (stairColor + "_stairs[facing=" + awnFace + ",half=bottom]")
                                      : terra));
        // awning ridge over the structure
        set(es, sx, SY + 5, cz, alt ? wool : terra);
    }
    // ridge cap row
    for (int sx = cx - hw - 1; sx <= cx + hw + 1; sx++) {
        boolean alt = ((sx - (cx - hw - 1)) % 2 == 0);
        set(es, sx, SY + 6, cz, alt ? terra : wool);
    }

    // ----- merchandise on the counter -----
    for (int sx = cx - hw; sx <= cx + hw; sx++) {
        double n = noise(sx, frontZ, 1700L + cx);
        String good;
        if (n < 0.22) good = "barrel[facing=up]";
        else if (n < 0.40) good = "hay_block[axis=y]";
        else if (n < 0.55) good = "melon";
        else if (n < 0.70) good = "pumpkin";
        else if (n < 0.82) good = "flower_pot";
        else continue;
        set(es, sx, SY + 3, frontZ, good);
    }
    // a chest tucked behind the counter
    set(es, cx, SY + 1, backZ - (facingNorth ? -1 : 1) * 0 + (facingNorth ? -1 : 1), "chest");
    // hanging lantern under the awning
    set(es, cx - hw, SY + 4, cz, "lantern[hanging=true]");
    set(es, cx + hw, SY + 4, cz, "lantern[hanging=true]");
}

private void buildMarketQuarterLanternString(EditSession es, int x0, int z0, int x1, int z1, int y) {
    int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
    if (steps == 0) return;
    for (int s = 1; s < steps; s++) {
        double t = (double) s / steps;
        int px = (int) Math.round(x0 + (x1 - x0) * t);
        int pz = (int) Math.round(z0 + (z1 - z0) * t);
        // gentle sag in the middle
        double sag = Math.sin(t * Math.PI);
        int sy = y - (int) Math.round(sag * 1.4);
        set(es, px, sy, pz, "chain[axis=y]");
        // periodic hanging lantern
        if (s % 3 == 0) {
            set(es, px, sy - 1, pz, "lantern[hanging=true]");
        }
    }
}

private void buildMarketQuarterCrateStack(EditSession es, int cx, int cz, long seed) {
    int h = 1 + (int) Math.floor(noise(cx, cz, seed) * 3); // 1..3 tall
    for (int y = 0; y < h; y++) {
        double n = noise(cx + y, cz, seed + 7);
        String id;
        if (n < 0.35) id = "barrel[facing=up]";
        else if (n < 0.55) id = "chest";
        else if (n < 0.80) id = "oak_planks";
        else id = "hay_block[axis=y]";
        set(es, cx, SY + 1 + y, cz, id);
    }
    // maybe a neighbor barrel
    if (noise(cx, cz, seed + 99) > 0.5) {
        set(es, cx + 1, SY + 1, cz, "barrel[facing=up]");
        if (noise(cx, cz, seed + 100) > 0.6) {
            set(es, cx + 1, SY + 2, cz, "barrel[facing=up]");
        }
    }
    if (noise(cx, cz, seed + 199) > 0.55) {
        set(es, cx, SY + 1, cz + 1, "chest");
    }
    // a lantern perched on tall stacks
    if (h >= 3) set(es, cx, SY + 1 + h, cz, "lantern[hanging=false]");
}

private void buildMarketQuarterTree(EditSession es, int cx, int cz) {
    if (!inDisk(cx, cz, R - 3)) return;
    int trunk = 4 + (int) Math.floor(noise(cx, cz, 6161L) * 2);
    column(es, cx, cz, SY + 1, SY + trunk, "oak_log[axis=y]");
    int top = SY + trunk;
    // leaf canopy clusters
    sphere(es, cx, top, cz, 2.6, "oak_leaves[persistent=true]");
    sphere(es, cx, top - 1, cz, 3.0, "oak_leaves[persistent=true]");
    sphere(es, cx + 1, top, cz, 2.0, "oak_leaves[persistent=true]");
    // a couple of hanging lanterns from the boughs
    set(es, cx + 2, top - 2, cz, "lantern[hanging=true]");
    set(es, cx - 2, top - 1, cz + 1, "lantern[hanging=true]");
    // re-set trunk top in case sphere overwrote
    set(es, cx, top, cz, "oak_log[axis=y]");
}

private void buildMarketQuarterMast(EditSession es, int cx, int cz) {
    // tall central festival mast with radiating bunting
    column(es, cx, cz, SY + 1, SY + 9, "stripped_spruce_log[axis=y]");
    set(es, cx, SY + 9, cz, "spruce_fence");
    set(es, cx, SY + 10, cz, "lantern[hanging=false]");
    // four guy-lines of chains down to anchor points with banners (wool)
    int[][] anchors = {{cx + 5, cz}, {cx - 5, cz}, {cx, cz + 5}, {cx, cz - 5}};
    String[] flagColors = {"red_wool", "yellow_wool", "blue_wool", "lime_wool"};
    for (int a = 0; a < anchors.length; a++) {
        int ax = anchors[a][0], az = anchors[a][1];
        buildMarketQuarterLanternString(es, cx, cz, ax, az, SY + 8);
        // little anchor post + banner color
        column(es, ax, az, SY + 1, SY + 3, "oak_fence");
        set(es, ax, SY + 4, az, flagColors[a]);
        set(es, ax, SY + 3, az, "lantern[hanging=true]");
    }
}

    private void buildMonumentQuarter(EditSession es) {
    // SE quadrant monument quarter centered ~ (34,64,34)
    final int cx = 34, cz = 34;

    // ---- Foundation cleanup: a polished_blackstone plaza disk on the grass ----
    buildMonumentQuarterPlaza(es, cx, cz);

    // ---- Reflecting pool ring surrounding the pedestal ----
    buildMonumentQuarterPool(es, cx, cz);

    // ---- Grand multi-step pedestal ----
    buildMonumentQuarterPedestal(es, cx, cz);

    // ---- Central tiered obelisk ----
    buildMonumentQuarterObelisk(es, cx, cz);

    // ---- Braziers at the four diagonal corners of the plaza ----
    buildMonumentQuarterBrazier(es, cx + 13, cz + 13);
    buildMonumentQuarterBrazier(es, cx - 13, cz + 13);
    buildMonumentQuarterBrazier(es, cx + 13, cz - 13);
    buildMonumentQuarterBrazier(es, cx - 13, cz - 13);

    // ---- Banner poles around the pool perimeter ----
    buildMonumentQuarterBannerPole(es, cx + 10, cz, "red");
    buildMonumentQuarterBannerPole(es, cx - 10, cz, "blue");
    buildMonumentQuarterBannerPole(es, cx, cz + 10, "yellow");
    buildMonumentQuarterBannerPole(es, cx, cz - 10, "white");
}

private void buildMonumentQuarterPlaza(EditSession es, int cx, int cz) {
    // Layered circular plaza floor with trim rings and noise speckle.
    final int y = SY;
    // Base structural under-layer so nothing floats over the island edge.
    disk(es, cx, y - 1, cz, 16.5, "polished_blackstone");
    disk(es, cx, y, cz, 16.0, "polished_blackstone");

    // Decorative concentric rings on the plaza top surface.
    ring(es, cx, y, cz, 14.0, 16.0, "polished_blackstone_bricks");
    ring(es, cx, y, cz, 11.5, 12.5, "chiseled_polished_blackstone");
    ring(es, cx, y, cz, 9.0, 9.6, "gilded_blackstone");

    // Quartz inlay paths from center toward each cardinal edge of the plaza.
    for (int d = -16; d <= 16; d++) {
        // along +/- x and +/- z axes
        if (inDisk(d, 0, 16.0)) {
            set(es, cx + d, y, cz, "smooth_quartz");
            set(es, cx, y, cz + d, "smooth_quartz");
        }
        // diagonal inlay
        int ad = Math.abs(d);
        if (inDisk(d, d, 16.0)) {
            set(es, cx + d, y, cz + d, "quartz_bricks");
            set(es, cx + d, y, cz - d, "quartz_bricks");
        }
        if (ad > 0) {
            // thin gold edge along the diagonals
        }
    }

    // Organic weathering speckle of cracked/normal variants using noise.
    for (int dx = -16; dx <= 16; dx++) {
        for (int dz = -16; dz <= 16; dz++) {
            if (!inDisk(dx, dz, 15.8)) continue;
            double n = noise(cx + dx, cz + dz, 91237L);
            if (n > 0.86) set(es, cx + dx, y, cz + dz, "cracked_polished_blackstone_bricks");
            else if (n < 0.06) set(es, cx + dx, y, cz + dz, "polished_blackstone_brick_slab[type=top]");
        }
    }
}

private void buildMonumentQuarterPool(EditSession es, int cx, int cz) {
    // Reflecting pool: a sunken water ring with a quartz rim and lantern posts.
    final int y = SY;
    // Carve the pool basin (ring band between r=6.2 and r=8.6).
    for (int dx = -9; dx <= 9; dx++) {
        for (int dz = -9; dz <= 9; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r >= 6.2 && r <= 8.6) {
                set(es, cx + dx, y, cz + dz, "water");
                set(es, cx + dx, y - 1, cz + dz, "prismarine_bricks");
                // glow source under the water for a shimmering reflecting effect
                double n = noise(cx + dx, cz + dz, 5521L);
                if (n > 0.78) set(es, cx + dx, y - 1, cz + dz, "sea_lantern");
            }
        }
    }
    // Inner and outer polished rim around the water.
    ring(es, cx, y, cz, 8.6, 9.0, "smooth_quartz");
    ring(es, cx, y, cz, 5.8, 6.2, "smooth_quartz");
    ring(es, cx, y + 1, cz, 8.6, 9.0, "quartz_slab[type=bottom]");
    ring(es, cx, y + 1, cz, 5.8, 6.2, "quartz_slab[type=bottom]");

    // Small lantern posts at eight points around the outer rim.
    for (int k = 0; k < 8; k++) {
        double ang = Math.PI * 2 * k / 8.0;
        int px = cx + (int) Math.round(Math.cos(ang) * 8.8);
        int pz = cz + (int) Math.round(Math.sin(ang) * 8.8);
        set(es, px, y, pz, "quartz_pillar[axis=y]");
        set(es, px, y + 1, pz, "quartz_pillar[axis=y]");
        set(es, px, y + 2, pz, "sea_lantern");
        set(es, px, y + 3, pz, "quartz_slab[type=bottom]");
    }
}

private void buildMonumentQuarterPedestal(EditSession es, int cx, int cz) {
    // Grand multi-step square pedestal rising from the plaza center.
    final int y = SY;
    // Four broad steps, each smaller, made of alternating stone/quartz trim.
    int[] halfs = {5, 4, 3, 2};
    String[] core = {
        "polished_blackstone_bricks",
        "smooth_stone",
        "polished_blackstone_bricks",
        "smooth_quartz"
    };
    String[] trim = {
        "chiseled_polished_blackstone",
        "smooth_stone_slab[type=top]",
        "gilded_blackstone",
        "chiseled_quartz_block"
    };
    int by = y + 1;
    for (int s = 0; s < halfs.length; s++) {
        int h = halfs[s];
        // solid step block
        fill(es, cx - h, by, cz - h, cx + h, by, cz + h, core[s]);
        // edge trim band on the perimeter of this step's top
        walls(es, cx - h, by, cz - h, cx + h, by, cz + h, trim[s]);
        by++;
    }

    // Stair fringe around the bottom step for a softened approach.
    int h0 = halfs[0];
    String[] dir = {"north", "south", "east", "west"};
    for (int t = -h0; t <= h0; t++) {
        set(es, cx + t, y + 1, cz - h0 - 1, "polished_blackstone_stairs[facing=south,half=bottom]");
        set(es, cx + t, y + 1, cz + h0 + 1, "polished_blackstone_stairs[facing=north,half=bottom]");
        set(es, cx - h0 - 1, y + 1, cz + t, "polished_blackstone_stairs[facing=east,half=bottom]");
        set(es, cx + h0 + 1, y + 1, cz + t, "polished_blackstone_stairs[facing=west,half=bottom]");
    }

    // Corner accent lanterns on the topmost step.
    int ht = halfs[halfs.length - 1];
    int topY = by; // one above last step
    set(es, cx - ht, topY, cz - ht, "lantern");
    set(es, cx + ht, topY, cz - ht, "lantern");
    set(es, cx - ht, topY, cz + ht, "lantern");
    set(es, cx + ht, topY, cz + ht, "lantern");
}

private void buildMonumentQuarterObelisk(EditSession es, int cx, int cz) {
    // Central tiered stone/quartz obelisk rising tall above the pedestal.
    final int baseY = SY + 5; // sits on top of the pedestal steps
    // Wide footing block.
    fill(es, cx - 2, baseY, cz - 2, cx + 2, baseY + 1, cz + 2, "smooth_quartz");
    walls(es, cx - 2, baseY, cz - 2, cx + 2, baseY + 1, cz + 2, "chiseled_quartz_block");

    // Main shaft: tapering tiers from 3x3 to 1x1.
    int y = baseY + 2;
    int[] tierHeights = {6, 6, 5, 4};
    int half = 1; // 3x3 footprint (half=1)
    for (int tier = 0; tier < tierHeights.length; tier++) {
        int th = tierHeights[tier];
        String shell = (tier % 2 == 0) ? "polished_blackstone_bricks" : "smooth_quartz";
        String pillar = (tier % 2 == 0) ? "polished_blackstone" : "quartz_pillar[axis=y]";
        for (int dy = 0; dy < th; dy++) {
            // outer shell walls
            walls(es, cx - half, y + dy, cz - half, cx + half, y + dy, cz + half, shell);
            // glowing core for inner light bleed at intervals
            if (dy % 3 == 1 && half >= 1) {
                set(es, cx, y + dy, cz, "shroomlight");
            } else {
                set(es, cx, y + dy, cz, pillar);
            }
            // vertical noise-driven gilded accents on the faces
            double n = noise(cx, y + dy + tier, 7741L);
            if (n > 0.7 && half == 1) {
                set(es, cx + half, y + dy, cz, "gilded_blackstone");
                set(es, cx - half, y + dy, cz, "gilded_blackstone");
            }
        }
        // tier cap / cornice between tiers
        int cy = y + th;
        int chalf = half + 1;
        for (int dx = -chalf; dx <= chalf; dx++) {
            for (int dz = -chalf; dz <= chalf; dz++) {
                if (Math.abs(dx) == chalf || Math.abs(dz) == chalf) {
                    set(es, cx + dx, cy, cz + dz, "smooth_quartz_slab[type=bottom]");
                }
            }
        }
        y = cy + 1;
        if (half > 0 && tier >= 1) half = Math.max(0, half - 0); // keep stable footprint
    }

    // Final pyramidal cap on top of the shaft.
    int capBase = y;
    set(es, cx, capBase, cz, "chiseled_quartz_block");
    set(es, cx + 1, capBase, cz, "smooth_quartz_slab[type=bottom]");
    set(es, cx - 1, capBase, cz, "smooth_quartz_slab[type=bottom]");
    set(es, cx, capBase, cz + 1, "smooth_quartz_slab[type=bottom]");
    set(es, cx, capBase, cz - 1, "smooth_quartz_slab[type=bottom]");
    set(es, cx, capBase + 1, cz, "quartz_pillar[axis=y]");
    set(es, cx, capBase + 2, cz, "sea_lantern");
    set(es, cx, capBase + 3, cz, "gold_block");
    // crowning beacon-like glow and chains as guy-wires for drama
    set(es, cx, capBase + 4, cz, "lightning_rod[facing=up]");

    // Decorative hanging chains from the cornice corners down the shaft.
    int chTop = baseY + 2;
    int chBot = baseY + 2 + tierHeights[0] - 2;
    column(es, cx + 2, cz + 2, chTop, chBot, "chain[axis=y]");
    column(es, cx - 2, cz + 2, chTop, chBot, "chain[axis=y]");
    column(es, cx + 2, cz - 2, chTop, chBot, "chain[axis=y]");
    column(es, cx - 2, cz - 2, chTop, chBot, "chain[axis=y]");
}

private void buildMonumentQuarterBrazier(EditSession es, int bx, int bz) {
    // Standing brazier: stone column, netherrack fire bowl caged by iron bars.
    final int y = SY + 1;
    // Stepped stone base.
    fill(es, bx - 1, y, bz - 1, bx + 1, y, bz + 1, "polished_blackstone_bricks");
    set(es, bx - 1, y, bz - 1, "polished_blackstone_brick_stairs[facing=north,half=bottom]");
    set(es, bx + 1, y, bz + 1, "polished_blackstone_brick_stairs[facing=south,half=bottom]");
    // Central pillar.
    column(es, bx, bz, y + 1, y + 3, "polished_blackstone_wall");
    // Fire bowl.
    set(es, bx, y + 4, bz, "netherrack");
    set(es, bx, y + 5, bz, "fire");
    // Iron bar cage around the bowl.
    set(es, bx + 1, y + 4, bz, "iron_bars[east=true,west=true]");
    set(es, bx - 1, y + 4, bz, "iron_bars[east=true,west=true]");
    set(es, bx, y + 4, bz + 1, "iron_bars[north=true,south=true]");
    set(es, bx, y + 4, bz - 1, "iron_bars[north=true,south=true]");
    set(es, bx + 1, y + 5, bz, "iron_bars[east=true,west=true]");
    set(es, bx - 1, y + 5, bz, "iron_bars[east=true,west=true]");
    set(es, bx, y + 5, bz + 1, "iron_bars[north=true,south=true]");
    set(es, bx, y + 5, bz - 1, "iron_bars[north=true,south=true]");
    // Glowing trim and a lantern hung beneath.
    set(es, bx, y + 6, bz, "iron_bars[north=true,south=true,east=true,west=true]");
    set(es, bx, y + 1, bz, "lantern[hanging=true]");
}

private void buildMonumentQuarterBannerPole(EditSession es, int px, int pz, String color) {
    // Tall banner pole: stone foot, dark oak shaft, chains and a colored banner top.
    final int y = SY + 1;
    set(es, px, y, pz, "chiseled_polished_blackstone");
    column(es, px, pz, y + 1, y + 7, "dark_oak_fence");
    // Cross-arm near the top for a hung look.
    set(es, px + 1, y + 6, pz, "dark_oak_fence");
    set(es, px - 1, y + 6, pz, "dark_oak_fence");
    set(es, px + 1, y + 6, pz, "chain[axis=y]");
    // Banner on top.
    set(es, px, y + 8, pz, color + "_wall_banner[facing=south]");
    // Hanging colored wool drape and lanterns flanking the foot.
    set(es, px + 1, y + 5, pz, color + "_wool");
    set(es, px - 1, y + 5, pz, color + "_wool");
    set(es, px + 1, y + 4, pz, color + "_carpet");
    set(es, px - 1, y + 4, pz, color + "_carpet");
    set(es, px, y, pz - 1, "lantern");
    set(es, px, y, pz + 1, "lantern");
}

    private void buildAmphitheaterQuarter(EditSession es) {
    // Center of the amphitheater bowl in the SW quadrant.
    final int cx = -34, cz = 34, cy = SY; // grass surface y = 64
    // The bowl opens toward the stage (toward +x,+z corner of the quadrant / island center).
    // Seating tiers descend toward a small raised dark_oak stage with a back wall.

    buildAmphitheaterQuarterFoundation(es, cx, cz, cy);
    buildAmphitheaterQuarterSeating(es, cx, cz, cy);
    buildAmphitheaterQuarterAisles(es, cx, cz, cy);
    buildAmphitheaterQuarterStage(es, cx, cz, cy);
    buildAmphitheaterQuarterSconces(es, cx, cz, cy);
}

// Carve a solid stone footing beneath the bowl so tiers never float, with noisy under-rock.
private void buildAmphitheaterQuarterFoundation(EditSession es, int cx, int cz, int cy) {
    double rOuter = 17.5;
    for (int dx = -18; dx <= 18; dx++) {
        for (int dz = -18; dz <= 18; dz++) {
            if (!inDisk(dx, dz, rOuter)) continue;
            int x = cx + dx, z = cz + dz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            // Floor of the bowl sits 4 below surface at the rim, climbs as it nears center stage.
            int floorY = cy - 5;
            // Solid footing block under everything
            double n = noise(x, z, 70011L);
            int botY = floorY - 3 - (int) Math.round(n * 4.0);
            fill(es, x, botY, z, x, floorY, z, "stone");
            // Mix in andesite / cobble veins for texture in the footing top
            double v = noise(x * 2, z * 2, 70013L);
            if (v > 0.78) set(es, x, floorY, z, "andesite");
            else if (v < 0.16) set(es, x, floorY, z, "cobblestone");
            // Hanging underside stalactite-ish edge near the rim
            if (dist > rOuter - 2.0 && noise(x, z, 70017L) > 0.6) {
                column(es, x, z, botY - 2 - (int) Math.round(noise(x, z, 70019L) * 2.0), botY - 1, "stone");
            }
        }
    }
}

// Stepped half-bowl of smooth_stone/quartz seating tiers descending toward the stage corner.
private void buildAmphitheaterQuarterSeating(EditSession es, int cx, int cz, int cy) {
    // We build concentric arcs of seating. Outer = highest, inner = lowest near stage.
    // Stage is at the high (+x,+z) inner corner; seats wrap the opposite arc.
    int tiers = 7;
    for (int t = 0; t < tiers; t++) {
        double rIn = 5.0 + t * 1.9;
        double rOut = rIn + 1.9;
        int seatTop = cy + t;            // each tier one block taller outward
        int riserBot = cy + t - 1;
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < rIn || dist > rOut) continue;
                // Only the half-bowl facing the stage: keep the arc on the -x,-z side
                // (seats look toward the stage in the +x,+z inner corner).
                double ang = Math.atan2(dz, dx); // -pi..pi
                // Keep roughly the 180-degree fan opposite the stage direction.
                // Stage direction ~ +x,+z (ang ~ +0.785). Opposite ~ -2.356.
                double rel = Math.atan2(Math.sin(ang - (-2.356)), Math.cos(ang - (-2.356)));
                if (Math.abs(rel) > 1.95) continue; // ~223 degree fan of seating
                int x = cx + dx, z = cz + dz;
                // Seat surface: alternate quartz / smooth_stone bands per tier for trim
                String seat = (t % 2 == 0) ? "smooth_stone" : "smooth_quartz";
                // Edge of each ring gets a polished trim lip
                boolean lip = (dist >= rOut - 1.0);
                String top = lip ? ((t % 2 == 0) ? "smooth_quartz_slab" : "smooth_stone_slab") : seat;
                // Riser (vertical face) below the seat
                fill(es, x, cy - 2, z, x, riserBot, z, (t % 2 == 0) ? "stone_bricks" : "polished_andesite");
                set(es, x, seatTop, z, seat);
                if (lip) set(es, x, seatTop + 1, z, top);
                // Occasional chiseled accent in the riser band
                if (noise(x, z, 80021L + t) > 0.85) set(es, x, riserBot, z, "chiseled_stone_bricks");
            }
        }
    }
    // Outer crown wall ringing the very top tier as a backing rail.
    for (int dx = -18; dx <= 18; dx++) {
        for (int dz = -18; dz <= 18; dz++) {
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 17.0 || dist > 17.9) continue;
            double ang = Math.atan2(dz, dx);
            double rel = Math.atan2(Math.sin(ang - (-2.356)), Math.cos(ang - (-2.356)));
            if (Math.abs(rel) > 1.95) continue;
            int x = cx + dx, z = cz + dz;
            int base = cy + (tiers - 1);
            fill(es, x, cy - 2, z, x, base, z, "stone_bricks");
            set(es, x, base + 1, z, "smooth_stone_slab");
            // Crenellation / wall caps with periodic columns
            if (noise(x, z, 80031L) > 0.55) set(es, x, base + 2, z, "stone_brick_wall");
        }
    }
}

// Two radial aisle stairways cutting through the seating for access.
private void buildAmphitheaterQuarterAisles(EditSession es, int cx, int cz, int cy) {
    // Aisle directions, given as angle offsets from the seating fan center (-2.356).
    double[] aisleAng = { -2.356 - 1.1, -2.356, -2.356 + 1.1 };
    for (double a : aisleAng) {
        double dirx = Math.cos(a), dirz = Math.sin(a);
        for (int step = 5; step <= 17; step++) {
            double fx = dirx * step, fz = dirz * step;
            int x = cx + (int) Math.round(fx);
            int z = cz + (int) Math.round(fz);
            // Tier index roughly corresponds to step distance.
            int t = (int) Math.floor((step - 5.0) / 1.9);
            if (t < 0) t = 0;
            int stepY = cy + t;
            // Clear seating above and lay stair blocks, widening to 3 wide.
            for (int w = -1; w <= 1; w++) {
                int wx = x + (int) Math.round(-dirz * w);
                int wz = z + (int) Math.round(dirx * w);
                fill(es, wx, stepY, wz, wx, stepY + 2, wz, "air");
                // Stair facing inward (toward lower center). Use polished blackstone stairs for contrast.
                String facing = aisleFacing(dirx, dirz);
                set(es, wx, stepY, wz, "polished_andesite_stairs[" + facing + ",half=bottom]");
                set(es, wx, stepY - 1, wz, "polished_andesite");
                if (w == 0 && (step % 3 == 0)) {
                    // Aisle lantern post in the center runner
                    set(es, wx, stepY + 1, wz, "lantern");
                }
            }
        }
    }
}

// Returns a facing= state for aisle stairs descending toward the bowl center.
private String aisleFacing(double dirx, double dirz) {
    // Stairs should face the descent direction (toward center = -dir).
    if (Math.abs(dirx) >= Math.abs(dirz)) {
        return dirx > 0 ? "facing=west" : "facing=east";
    } else {
        return dirz > 0 ? "facing=north" : "facing=south";
    }
}

// Small raised dark_oak stage with a back wall and dramatic hidden lighting.
private void buildAmphitheaterQuarterStage(EditSession es, int cx, int cz, int cy) {
    // Stage sits at the inner +x,+z corner, in front of the lowest tier.
    int sx = cx + 6, sz = cz + 6; // stage center
    int sy = cy - 2;              // stage deck a bit below surface (the pit floor)
    // Stage platform
    for (int dx = -5; dx <= 5; dx++) {
        for (int dz = -5; dz <= 5; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > 8) continue; // chamfered rectangle
            int x = sx + dx, z = sz + dz;
            // Support pillars down to footing
            fill(es, x, cy - 5, z, x, sy - 1, z, "dark_oak_log[axis=y]");
            // Plank deck with subtle parquet noise
            double n = noise(x, z, 90041L);
            String deck = n > 0.82 ? "dark_oak_log[axis=y]" : "dark_oak_planks";
            set(es, x, sy, z, deck);
            // Edge trim slabs around the rim
            if (Math.abs(dx) == 5 || Math.abs(dz) == 5 || Math.abs(dx) + Math.abs(dz) == 8) {
                set(es, x, sy + 1, z, "dark_oak_slab");
            }
        }
    }
    // Front-of-stage step lip down into the pit (skirting)
    for (int dx = -5; dx <= 5; dx++) {
        int x = sx + dx, z = sz - 6;
        set(es, x, sy, z, "dark_oak_stairs[facing=north,half=bottom]");
    }
    for (int dz = -5; dz <= 5; dz++) {
        int x = sx - 6, z = sz + dz;
        set(es, x, sy, z, "dark_oak_stairs[facing=west,half=bottom]");
    }

    // Back wall behind the stage (on the +x,+z side), tall, with framed lighting.
    int wallH = 9;
    for (int d = -6; d <= 6; d++) {
        // Two wall faces meeting at the corner behind the stage.
        int xA = sx + 6, zA = sz + d;          // wall facing the seats (+x side)
        int xB = sx + d, zB = sz + 6;          // wall facing the seats (+z side)
        buildAmphitheaterQuarterWallColumn(es, xA, zA, sy, wallH, d, 90051L);
        buildAmphitheaterQuarterWallColumn(es, xB, zB, sy, wallH, d, 90057L);
    }
    // Corner post where the two walls meet
    column(es, sx + 6, sz + 6, sy, sy + wallH + 1, "polished_blackstone_bricks");
    set(es, sx + 6, sy + wallH + 2, sz + 6, "sea_lantern");

    // Hidden stage lighting: sea_lantern/glowstone behind glass set into the lower wall.
    for (int d = -5; d <= 5; d++) {
        if (d % 2 != 0) continue;
        // Light wells in wall A
        int xA = sx + 6, zA = sz + d;
        set(es, xA - 1, sy + 2, zA, noise(xA, zA, 90061L) > 0.5 ? "sea_lantern" : "glowstone");
        set(es, xA, sy + 2, zA, "glass");
        // Light wells in wall B
        int xB = sx + d, zB = sz + 6;
        set(es, xB, sy + 2, zB - 1, noise(xB, zB, 90063L) > 0.5 ? "sea_lantern" : "glowstone");
        set(es, xB, sy + 2, zB, "glass");
    }

    // Stage floor wash lighting at the deck front edge (uplights under slab lip).
    for (int dx = -4; dx <= 4; dx += 2) {
        set(es, sx + dx, sy - 1, sz - 5, "glowstone");
        set(es, sx + dx, sy, sz - 5, "glass");
    }
    // A central backdrop banner accent
    set(es, sx + 5, sy + 4, sz, "sea_lantern");
    set(es, sx, sy + 4, sz + 5, "sea_lantern");
}

// One vertical column of the stage back wall, with banding and a battlement cap.
private void buildAmphitheaterQuarterWallColumn(EditSession es, int x, int z, int baseY, int h, int d, long seed) {
    for (int y = 0; y <= h; y++) {
        int wy = baseY + y;
        String mat;
        if (y == 0) mat = "polished_blackstone";
        else if (y == h) mat = "chiseled_polished_blackstone";
        else if ((y % 3 == 0)) mat = "polished_blackstone_brick_wall".equals("") ? "polished_blackstone" : "polished_blackstone_bricks";
        else {
            double n = noise(x + y, z, seed);
            mat = n > 0.85 ? "chiseled_polished_blackstone"
                : n < 0.12 ? "gilded_blackstone"
                : "polished_blackstone_bricks";
        }
        set(es, x, wy, z, mat);
    }
    // Battlement cap with periodic wall posts
    if (Math.floorMod(d, 2) == 0) {
        set(es, x, baseY + h + 1, z, "polished_blackstone_brick_wall");
        if (Math.floorMod(d, 4) == 0) set(es, x, baseY + h + 2, z, "polished_blackstone_brick_wall");
    } else {
        set(es, x, baseY + h + 1, z, "polished_blackstone_slab[type=bottom]");
    }
}

// Torch/lantern sconces ringing the seating crown and along aisle rails.
private void buildAmphitheaterQuarterSconces(EditSession es, int cx, int cz, int cy) {
    int tiers = 7;
    // Lantern posts spaced around the outer crown ring.
    for (int deg = 0; deg < 360; deg += 18) {
        double a = Math.toRadians(deg);
        double rel = Math.atan2(Math.sin(a - (-2.356)), Math.cos(a - (-2.356)));
        if (Math.abs(rel) > 1.9) continue; // only along the seating fan
        double r = 17.4;
        int x = cx + (int) Math.round(Math.cos(a) * r);
        int z = cz + (int) Math.round(Math.sin(a) * r);
        int base = cy + (tiers - 1) + 2;
        // Stone-brick post topped with a lantern and a chain dangling a glow
        column(es, x, z, base, base + 2, "stone_brick_wall");
        set(es, x, base + 3, z, "lantern");
        // Alternate: a brazier with fire-safe glow (use lantern + sea_lantern accent)
        if (noise(x, z, 95071L) > 0.6) {
            set(es, x, base + 2, z, "sea_lantern");
            set(es, x, base + 3, z, "campfire");
        }
    }
    // Inner pit-edge wall sconces: chains + hanging lanterns over the front rows.
    double rPit = 5.4;
    for (int deg = 0; deg < 360; deg += 22) {
        double a = Math.toRadians(deg);
        double rel = Math.atan2(Math.sin(a - (-2.356)), Math.cos(a - (-2.356)));
        if (Math.abs(rel) > 1.9) continue;
        int x = cx + (int) Math.round(Math.cos(a) * rPit);
        int z = cz + (int) Math.round(Math.sin(a) * rPit);
        int top = cy + 2;
        set(es, x, top, z, "chain[axis=y]");
        set(es, x, top - 1, z, "lantern[hanging=true]");
    }
    // Scatter a few decorative glow accents into the footing rim for ambience.
    scatter(es, cx, cy - 4, cz, 16.0, 0.02, 95081L, "sea_lantern", "glowstone");
}

    private void buildWaterfalls(EditSession es) {
    // Four dramatic waterfalls on the diagonal island-edge points.
    int[][] diag = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
    long[] seeds = {7001L, 7002L, 7003L, 7004L};
    for (int i = 0; i < diag.length; i++) {
        int dx = diag[i][0];
        int dz = diag[i][1];
        // Edge lip point (just inside the island rim).
        int ex = (int) Math.round(dx * (R - 4) * 0.70710678);
        int ez = (int) Math.round(dz * (R - 4) * 0.70710678);
        buildWaterfallsBasin(es, ex, ez, dx, dz, seeds[i]);
        buildWaterfallsCurtain(es, ex, ez, dx, dz, seeds[i]);
        buildWaterfallsMossyRocks(es, ex, ez, dx, dz, seeds[i] + 500L);
    }
    // Meandering surface stream winding inward toward the NE pond.
    buildWaterfallsStream(es);
}

/** Carved stone basin at the waterfall lip: sunken pool ringed with stone trim. */
private void buildWaterfallsBasin(EditSession es, int ex, int ez, int dx, int dz, long seed) {
    double rOuter = 4.4;
    double rInner = 2.6;
    // Carve a shallow bowl and line it with varied stone.
    for (int x = ex - 5; x <= ex + 5; x++) {
        for (int z = ez - 5; z <= ez + 5; z++) {
            double d = Math.hypot(x - ex, z - ez);
            if (d > rOuter + 0.6) continue;
            double n = noise(x, z, seed);
            // Floor of the basin one block below the surface.
            if (d <= rInner) {
                String floor = n < 0.45 ? "stone"
                        : n < 0.70 ? "cobblestone"
                        : n < 0.88 ? "andesite"
                        : "mossy_cobblestone";
                set(es, x, SY - 1, z, floor);
                set(es, x, SY, z, "water");
            } else if (d <= rOuter) {
                // Stone rim wall around the pool.
                String rim = n < 0.40 ? "stone_bricks"
                        : n < 0.62 ? "mossy_stone_bricks"
                        : n < 0.82 ? "cobblestone"
                        : "cracked_stone_bricks";
                set(es, x, SY, z, rim);
                set(es, x, SY - 1, z, "stone");
                // Occasional raised trim blocks for a carved feel.
                if (d > rOuter - 0.8 && noise(x, z, seed + 11L) > 0.74) {
                    set(es, x, SY + 1, z, n < 0.5 ? "cobblestone_wall" : "mossy_cobblestone_wall");
                }
            }
        }
    }
    // Spillway: a notch in the rim facing outward where water pours over the edge.
    int sx = ex + dx;
    int sz = ez + dz;
    for (int step = 0; step <= 3; step++) {
        int wx = ex + dx * step;
        int wz = ez + dz * step;
        set(es, wx, SY, wz, "water");
        set(es, wx, SY - 1, wz, "smooth_stone");
        // low side guards so the channel reads cleanly
        set(es, wx + dz, SY, wz + dx, "mossy_stone_bricks");
        set(es, wx - dz, SY, wz - dx, "mossy_stone_bricks");
    }
    // Decorative lip block and a lantern post beside the basin.
    set(es, sx, SY + 1, sz, "chiseled_stone_bricks");
    set(es, ex - dz * 4, SY + 1, ez - dx * 4, "cobblestone_wall");
    set(es, ex - dz * 4, SY + 2, ez - dx * 4, "lantern");
}

/** Water source curtain cascading off the island lip down into the void. */
private void buildWaterfallsCurtain(EditSession es, int ex, int ez, int dx, int dz, long seed) {
    // Start at the outer edge of the basin and pour outward + down.
    int lipX = ex + dx * 4;
    int lipZ = ez + dz * 4;
    int fallDepth = 18;
    // The falling column widens slightly as it descends (organic spread).
    for (int h = 0; h <= fallDepth; h++) {
        int y = SY - h;
        double spread = 1.0 + (h / (double) fallDepth) * 2.2;
        int span = (int) Math.round(spread);
        for (int s = -span; s <= span; s++) {
            // Curtain runs perpendicular to the fall direction.
            int wx = lipX + (-dz) * s;
            int wz = lipZ + (dx) * s;
            // Noise gives the curtain a torn, irregular silhouette near the edges.
            double n = noise(wx, wz, seed + h * 3L);
            if (Math.abs(s) == span && n < 0.45) continue;
            set(es, wx, y, wz, "water");
            // Wet stone backing behind the curtain near the top for structure.
            if (h <= 3) {
                int bx = wx + dx;
                int bz = wz + dz;
                if (noise(bx, bz, seed + 99L) > 0.3) {
                    set(es, bx, y, bz, n < 0.5 ? "stone" : "mossy_cobblestone");
                }
            }
        }
    }
    // Source-block guarantee at the very lip so the cascade always flows.
    set(es, lipX, SY, lipZ, "water");
    set(es, lipX + (-dz), SY, lipZ + dx, "water");
    set(es, lipX + dz, SY, lipZ + (-dx), "water");
    // Misty glow stones tucked into the cliff beside the fall.
    set(es, lipX + dz * 2, SY - 5, lipZ - dx * 2, "glowstone");
    set(es, lipX - dz * 2, SY - 9, lipZ + dx * 2, "glowstone");
    // A few hanging dripstone tips trailing the water for drama.
    for (int k = 1; k <= 3; k++) {
        int hy = SY - 4 * k;
        set(es, lipX + dx, hy, lipZ + dz, "dripstone_block");
        set(es, lipX + dx, hy - 1, lipZ + dz, "pointed_dripstone[vertical_direction=down]");
    }
}

/** Mossy rocks and overgrowth clustered around the waterfall lip. */
private void buildWaterfallsMossyRocks(EditSession es, int ex, int ez, int dx, int dz, long seed) {
    String[] rocks = {"mossy_cobblestone", "stone", "andesite", "mossy_stone_bricks", "moss_block"};
    for (int c = 0; c < 6; c++) {
        double a = noise(c * 13, c * 7, seed) * Math.PI * 2.0;
        double rad = 5.0 + noise(c, c * 3, seed + 5L) * 2.5;
        int rx = ex + (int) Math.round(Math.cos(a) * rad);
        int rz = ez + (int) Math.round(Math.sin(a) * rad);
        // Skip rocks that would land out toward the void notch.
        if ((rx - ex) * dx > 3 && (rz - ez) * dz > 3) continue;
        double rr = 1.2 + noise(rx, rz, seed + 21L) * 1.3;
        int ri = (int) Math.ceil(rr);
        for (int bx = -ri; bx <= ri; bx++) {
            for (int bz = -ri; bz <= ri; bz++) {
                for (int by = 0; by <= ri; by++) {
                    if (bx * bx + bz * bz + by * by > rr * rr) continue;
                    double n = noise(rx + bx, rz + bz, seed + by * 4L);
                    String id = rocks[(int) (n * rocks.length) % rocks.length];
                    set(es, rx + bx, SY + by, rz + bz, id);
                }
            }
        }
        // Crown some rocks with moss carpet, azalea, or glow lichen accents.
        double cn = noise(rx, rz, seed + 31L);
        if (cn > 0.66) set(es, rx, SY + ri + 1, rz, "moss_carpet");
        else if (cn > 0.40) set(es, rx, SY + ri + 1, rz, "azalea_bush");
        if (noise(rx, rz, seed + 41L) > 0.8) {
            set(es, rx, SY + ri, rz, "glowstone");
        }
    }
}

/** Meandering surface stream with stone banks winding inward toward the NE pond. */
private void buildWaterfallsStream(EditSession es) {
    // NE corner edge start, curving inward toward a pond near the NE inner area.
    int startX = (int) Math.round((R - 6) * 0.70710678);
    int startZ = (int) Math.round((R - 6) * 0.70710678);
    int pondX = 18;
    int pondZ = 18;
    int steps = 40;
    int prevX = startX, prevZ = startZ;
    for (int t = 0; t <= steps; t++) {
        double f = t / (double) steps;
        // Linear interpolation toward the pond, plus a sine + noise meander.
        double baseX = startX + (pondX - startX) * f;
        double baseZ = startZ + (pondZ - startZ) * f;
        double wob = Math.sin(f * Math.PI * 3.0) * 4.0 * (1.0 - f);
        double nWob = (noise(t * 17, t * 5, 7777L) - 0.5) * 5.0 * (1.0 - f * 0.6);
        // Offset perpendicular to the inward direction (which is roughly -1,-1).
        int cx = (int) Math.round(baseX + (wob + nWob) * 0.70710678);
        int cz = (int) Math.round(baseZ - (wob + nWob) * 0.70710678);
        // Carve the water channel (width 1, occasionally 2 for variation).
        boolean wide = noise(cx, cz, 8888L) > 0.6;
        for (int wx = -1; wx <= (wide ? 1 : 0); wx++) {
            for (int wz = -1; wz <= (wide ? 1 : 0); wz++) {
                if (Math.abs(wx) + Math.abs(wz) > 1) continue;
                set(es, cx + wx, SY, cz + wz, "water");
                set(es, cx + wx, SY - 1, cz + wz, "smooth_stone");
            }
        }
        // Stone banks on both sides of the channel.
        for (int side = -2; side <= 2; side++) {
            if (Math.abs(side) < 1) continue;
            int bxp = cx + side;
            int bzp = cz + side;
            double bn = noise(bxp, cz, 9999L);
            String bank = bn < 0.45 ? "cobblestone"
                    : bn < 0.70 ? "mossy_cobblestone"
                    : bn < 0.88 ? "stone" : "andesite";
            // Only place bank where it is not water and adjacent to the channel.
            if (Math.abs(side) == 2) {
                set(es, bxp, SY, cz, bank);
                set(es, cx, SY, bzp, bank);
            }
        }
        // Occasional flat stepping stones bridging the stream.
        if (t % 11 == 5) {
            set(es, cx, SY, cz, "stone_brick_slab[type=bottom]");
        }
        // Sprinkle moss carpet and ferns along the banks for greenery.
        if (noise(cx, cz, 6543L) > 0.72) {
            set(es, cx + 2, SY + 1, cz, "fern");
        }
        if (noise(cz, cx, 3210L) > 0.74) {
            set(es, cx, SY + 1, cz - 2, "moss_carpet");
        }
        prevX = cx;
        prevZ = cz;
    }
    // The NE pond where the stream pools: a small organic basin.
    double pr = 4.5;
    for (int x = pondX - 6; x <= pondX + 6; x++) {
        for (int z = pondZ - 6; z <= pondZ + 6; z++) {
            double d = Math.hypot(x - pondX, z - pondZ);
            double edge = pr + (noise(x, z, 4321L) * 1.6 - 0.8);
            if (d > edge) continue;
            set(es, x, SY, z, "water");
            set(es, x, SY - 1, z, noise(x, z, 1234L) < 0.5 ? "clay" : "gravel");
            // Mossy stone ring just outside the pond rim.
            if (d > edge - 1.0) {
                set(es, x, SY - 1, z, "mossy_cobblestone");
            }
        }
    }
    // Lily pads and a glow accent on the pond.
    set(es, pondX + 2, SY + 1, pondZ - 1, "lily_pad");
    set(es, pondX - 1, SY + 1, pondZ + 2, "lily_pad");
    set(es, pondX, SY - 1, pondZ, "sea_lantern");
}

    private void buildRailingTowers(EditSession es) {
    // ===========================================================
    // PERIMETER RING: ornamental wall + railing + lantern posts
    // ===========================================================
    final long SEED = 778451L;

    // The structural rim band. We work on a ring of radius ~59..62.
    // Base ornamental wall sits just inside the rim, with stair trim,
    // a fence/cobblestone-wall railing, and periodic lantern posts.

    // ---- 1) Foundation lip under the rim (organic, slightly ragged) ----
    for (int x = -R - 2; x <= R + 2; x++) {
        for (int z = -R - 2; z <= R + 2; z++) {
            int d2 = x * x + z * z;
            double d = Math.sqrt(d2);
            if (d >= 57.0 && d <= 62.6) {
                double n = noise(x, z, SEED);
                // top capstone surface at grass level
                String cap = (n > 0.78) ? "mossy_stone_bricks"
                        : (n > 0.5) ? "stone_bricks" : "polished_andesite";
                set(es, x, SY, z, cap);
                // body of the rim band going down a few blocks
                int depth = 3 + (int) Math.round(noise(x + 31, z - 17, SEED) * 3.0);
                for (int dy = 1; dy <= depth; dy++) {
                    double bn = noise(x * 2 + dy, z * 2 - dy, SEED + 9);
                    String body = (bn > 0.7) ? "cobblestone"
                            : (bn > 0.4) ? "stone_bricks" : "andesite";
                    set(es, x, SY - dy, z, body);
                }
                // hanging undergrowth / drips for the floating-island look
                if (d <= 60.5 && noise(x - 9, z + 14, SEED + 3) > 0.86) {
                    int hang = 1 + (int) Math.round(noise(x, z, SEED + 4) * 3.0);
                    column(es, x, z, SY - depth - hang, SY - depth - 1, "cobblestone");
                    if (noise(x + 5, z, SEED + 7) > 0.6) {
                        set(es, x, SY - depth - hang - 1, z, "stone_brick_wall");
                    }
                }
            }
        }
    }

    // ---- 2) Low ornamental wall ring (raised 1 block above cap) ----
    ring(es, 0, SY + 1, 0, 59.0, 61.4, "stone_bricks");
    // trim course of chiseled accents woven through the wall
    for (int x = -R - 1; x <= R + 1; x++) {
        for (int z = -R - 1; z <= R + 1; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d >= 59.0 && d <= 61.4) {
                double n = noise(x + 13, z + 21, SEED + 11);
                if (n > 0.72) set(es, x, SY + 1, z, "chiseled_stone_bricks");
                else if (n < 0.12) set(es, x, SY + 1, z, "mossy_stone_bricks");
            }
        }
    }

    // ---- 3) Railing on top of the wall: alternating fence + cobble wall ----
    // Walk the ring by angle so spacing is even and dense.
    int railSteps = 360;
    for (int i = 0; i < railSteps; i++) {
        double a = (i / (double) railSteps) * Math.PI * 2.0;
        double rr = 60.3;
        int x = (int) Math.round(Math.cos(a) * rr);
        int z = (int) Math.round(Math.sin(a) * rr);
        // skip gate openings on the four cardinal axes (gates handled elsewhere)
        boolean nearGate = (Math.abs(x) < 4 && z > 0)   // +Z gate
                || (Math.abs(x) < 4 && z < 0)            // -Z gate
                || (Math.abs(z) < 4 && x > 0)            // +X gate
                || (Math.abs(z) < 4 && x < 0);           // -X gate
        if (nearGate) continue;
        String rail = (i % 2 == 0) ? "oak_fence" : "cobblestone_wall";
        set(es, x, SY + 2, z, rail);
        // every so often add a second tier of fence for visual rhythm
        if (i % 12 == 0) {
            set(es, x, SY + 3, z, "oak_fence");
        }
    }

    // ---- 4) Periodic lantern posts around the rim ----
    int posts = 28;
    for (int i = 0; i < posts; i++) {
        double a = (i / (double) posts) * Math.PI * 2.0 + 0.11;
        double rr = 60.0;
        int x = (int) Math.round(Math.cos(a) * rr);
        int z = (int) Math.round(Math.sin(a) * rr);
        boolean nearGate = (Math.abs(x) < 6 && Math.abs(z) > 56)
                || (Math.abs(z) < 6 && Math.abs(x) > 56);
        if (nearGate) continue;
        buildRailingTowersLanternPost(es, x, z);
    }

    // ---- 5) Four corner watchtowers (between the cardinal gates) ----
    // Gates are on the axes; towers go on the diagonals at radius ~58.
    double tr = 57.0;
    int[][] corners = new int[4][2];
    double[] cang = {Math.PI * 0.25, Math.PI * 0.75, Math.PI * 1.25, Math.PI * 1.75};
    for (int c = 0; c < 4; c++) {
        int tx = (int) Math.round(Math.cos(cang[c]) * tr);
        int tz = (int) Math.round(Math.sin(cang[c]) * tr);
        corners[c][0] = tx;
        corners[c][1] = tz;
        buildRailingTowersWatchtower(es, tx, tz, SEED + c * 101L);
    }
}

// -----------------------------------------------------------------
// Lantern post: stone base, fence pole, chain, hanging lantern, wall trim
// -----------------------------------------------------------------
private void buildRailingTowersLanternPost(EditSession es, int x, int z) {
    set(es, x, SY + 1, z, "polished_andesite");
    set(es, x, SY + 2, z, "stone_brick_wall");
    set(es, x, SY + 3, z, "oak_fence");
    set(es, x, SY + 4, z, "oak_fence");
    set(es, x, SY + 5, z, "stone_brick_wall");
    // crossbar arms reaching out with hanging lanterns
    set(es, x, SY + 6, z, "chiseled_stone_bricks");
    set(es, x, SY + 5, z, "chain[axis=y]");
    set(es, x, SY + 4, z, "lantern[hanging=true]");
    // glow accent at base
    if (noise(x, z, 1234L) > 0.5) {
        set(es, x, SY + 6, z, "sea_lantern");
    }
}

// -----------------------------------------------------------------
// Round stone-brick watchtower: tapered shaft, windows, battlement
// gallery, conical roof, lantern crown.
// -----------------------------------------------------------------
private void buildRailingTowersWatchtower(EditSession es, int cx, int cz, long seed) {
    int baseY = SY;
    int shaftTop = SY + 18;     // top of the main shaft
    double rOuter = 5.0;
    double rInner = 3.4;

    // ---- 0) Foundation plinth flaring out below the surface ----
    for (int dy = 0; dy <= 5; dy++) {
        double rr = rOuter + 1.6 - dy * 0.25;
        disk(es, cx, baseY - dy, cz, rr, (dy % 2 == 0) ? "stone_bricks" : "cobblestone");
    }
    // ragged hanging roots beneath the tower
    for (int k = 0; k < 14; k++) {
        double a = (k / 14.0) * Math.PI * 2.0;
        int hx = cx + (int) Math.round(Math.cos(a) * (rOuter - 0.5));
        int hz = cz + (int) Math.round(Math.sin(a) * (rOuter - 0.5));
        int hang = 2 + (int) Math.round(noise(hx, hz, seed) * 4.0);
        column(es, hx, hz, baseY - 5 - hang, baseY - 6, "cobblestone");
        set(es, hx, baseY - 5 - hang - 1, hz, "stone_brick_wall");
    }

    // ---- 1) Solid floor disk ----
    disk(es, cx, baseY, cz, rOuter, "stone_bricks");
    disk(es, cx, baseY + 1, cz, rInner, "polished_andesite");

    // ---- 2) Tapered hollow shaft built tube by tube with weathering ----
    for (int y = baseY + 1; y <= shaftTop; y++) {
        int rel = y - baseY;
        double taper = rOuter - rel * 0.06;       // gentle inward taper
        double inner = rInner - rel * 0.04;
        tube(es, cx, y, cz, inner, taper, 1, "stone_bricks");
    }
    // weathering pass: mossy / cracked speckle on the outer skin
    for (int y = baseY + 1; y <= shaftTop; y++) {
        for (int ang = 0; ang < 360; ang += 12) {
            double a = Math.toRadians(ang);
            int rel = y - baseY;
            double taper = rOuter - rel * 0.06;
            int wx = cx + (int) Math.round(Math.cos(a) * taper);
            int wz = cz + (int) Math.round(Math.sin(a) * taper);
            double n = noise(wx + y, wz - y, seed + 5);
            if (n > 0.82) set(es, wx, wz == 0 ? wz : wz, y, "mossy_stone_bricks");
            else if (n < 0.08) set(es, wx, wz, y, "cracked_stone_bricks");
        }
    }

    // ---- 3) Decorative banding courses ----
    for (int y = baseY + 4; y <= shaftTop; y += 5) {
        int rel = y - baseY;
        double taper = rOuter - rel * 0.06;
        tube(es, cx, y, cz, taper - 0.6, taper + 0.15, 1, "chiseled_stone_bricks");
    }

    // ---- 4) Windows: arched openings on 4 faces at two levels ----
    int[] winY = {baseY + 6, baseY + 12};
    double[] faces = {0.0, Math.PI * 0.5, Math.PI, Math.PI * 1.5};
    for (int wy : winY) {
        int rel = wy - baseY;
        double taper = rOuter - rel * 0.06;
        for (double fa : faces) {
            for (int dh = -1; dh <= 1; dh++) {
                double a = fa + dh * 0.12;
                for (int yy = wy; yy <= wy + 1; yy++) {
                    int wx = cx + (int) Math.round(Math.cos(a) * taper);
                    int wz = cz + (int) Math.round(Math.sin(a) * taper);
                    set(es, wx, yy, wz, "air");
                }
            }
            // glass pane + lit sill for warmth
            int gx = cx + (int) Math.round(Math.cos(fa) * taper);
            int gz = cz + (int) Math.round(Math.sin(fa) * taper);
            set(es, gx, wy, gz, "brown_stained_glass_pane");
            set(es, gx, wy - 1, gz, "stone_brick_slab");
            set(es, gx, wy + 2, gz, "stone_brick_stairs[half=bottom]");
        }
        // inner glow so windows read at night
        set(es, cx, wy, cz, "lantern[hanging=false]");
    }

    // ---- 5) Machicolation / battlement gallery overhang at shaft top ----
    int galY = shaftTop + 1;
    double galR = rOuter + 0.7;
    // corbel ring jutting out
    ring(es, cx, shaftTop, cz, rOuter - 0.4, galR, "stone_brick_stairs[half=top]");
    tube(es, cx, galY, cz, rInner, galR, 1, "stone_bricks");
    disk(es, cx, galY, cz, rInner, "polished_andesite");
    // crenellations: merlons every other angular step
    int merlonSteps = 24;
    for (int i = 0; i < merlonSteps; i++) {
        double a = (i / (double) merlonSteps) * Math.PI * 2.0;
        int mx = cx + (int) Math.round(Math.cos(a) * galR);
        int mz = cz + (int) Math.round(Math.sin(a) * galR);
        if (i % 2 == 0) {
            set(es, mx, galY + 1, mz, "stone_bricks");
            set(es, mx, galY + 2, mz, "stone_brick_wall");
        } else {
            set(es, mx, galY + 1, mz, "stone_brick_wall");
        }
    }

    // ---- 6) Upper drum supporting the roof ----
    int drumBase = galY + 1;
    int drumTop = drumBase + 4;
    for (int y = drumBase; y <= drumTop; y++) {
        tube(es, cx, y, cz, rInner - 0.3, rInner + 0.9, 1, "stone_bricks");
    }
    // small upper windows in the drum
    for (double fa : new double[]{Math.PI * 0.25, Math.PI * 0.75, Math.PI * 1.25, Math.PI * 1.75}) {
        int wx = cx + (int) Math.round(Math.cos(fa) * (rInner + 0.9));
        int wz = cz + (int) Math.round(Math.sin(fa) * (rInner + 0.9));
        set(es, wx, drumBase + 1, wz, "air");
        set(es, wx, drumBase + 1, wz, "brown_stained_glass_pane");
    }

    // ---- 7) Conical roof of stairs + spire ----
    int roofBase = drumTop + 1;
    double roofR = rInner + 1.6;
    // layered ring cone using stairs facing inward, stepping up
    int roofH = 7;
    for (int layer = 0; layer < roofH; layer++) {
        double rr = roofR - layer * (roofR / (roofH + 1.0));
        int ry = roofBase + layer;
        ring(es, cx, ry, cz, Math.max(0.0, rr - 1.0), rr, "deepslate_tile_stairs[half=bottom]");
        ring(es, cx, ry, cz, 0.0, Math.max(0.0, rr - 1.0), "polished_deepslate");
    }
    // sealed apex
    cone(es, cx, roofBase + roofH - 1, cz, roofR * 0.35, 3, false, "polished_deepslate");

    // ---- 8) Lantern crown / beacon on top ----
    int spireBase = roofBase + roofH + 1;
    column(es, cx, cz, spireBase, spireBase + 2, "polished_blackstone");
    set(es, cx, spireBase + 3, cz, "chain[axis=y]");
    set(es, cx, spireBase + 4, cz, "sea_lantern");
    set(es, cx, spireBase + 5, cz, "lantern[hanging=true]");
    // four hanging corner lanterns under the gallery for ambiance
    for (double fa : faces) {
        int rel = galY - baseY;
        int lx = cx + (int) Math.round(Math.cos(fa) * (galR));
        int lz = cz + (int) Math.round(Math.sin(fa) * (galR));
        set(es, lx, galY, lz, "chain[axis=y]");
        set(es, lx, galY - 1, lz, "lantern[hanging=true]");
    }

    // ---- 9) Doorway facing island center ----
    double inward = Math.atan2(-cz, -cx);
    for (int dh = -1; dh <= 1; dh++) {
        double a = inward + dh * 0.12;
        int dxp = cx + (int) Math.round(Math.cos(a) * rOuter);
        int dzp = cz + (int) Math.round(Math.sin(a) * rOuter);
        set(es, dxp, baseY + 1, dzp, "air");
        set(es, dxp, baseY + 2, dzp, "air");
    }
    int dx0 = cx + (int) Math.round(Math.cos(inward) * rOuter);
    int dz0 = cz + (int) Math.round(Math.sin(inward) * rOuter);
    set(es, dx0, baseY + 3, dz0, "stone_brick_stairs[half=top]");
    set(es, dx0, baseY, dz0, "polished_andesite");
}

    // ── Schwebende Satelliten-Inseln (Orbit um die Hauptinsel) ──────────────────
//
// Fuenf kleine Akzent-Inseln rund um den Hauptrand (Radius ~80..95, y 62..70),
// jede mit Gras-Oberseite, eigenem Mini-Baum bzw. Struktur, und einer
// geschwungenen Holzbruecke (oak_planks-Deck, oak_fence-Gelaender, Ketten-
// Aufhaengung) zurueck zum Rand der Hauptinsel. Vollstaendig deterministisch.
private void buildSatelliteIslands(EditSession es) {
    // Polar-Platzierung: Winkel (Grad), Orbit-Radius, Oberseiten-Y, Insel-Radius, Variante
    int[][] specs = {
            {  20, 82, 65, 9, 0 },   // Baum-Insel
            {  92, 88, 67, 8, 1 },   // Laternen-Hain
            { 156, 80, 63, 10, 2 },  // Steinturm
            { 230, 94, 66, 7, 3 },   // Azaleen-Garten
            { 308, 86, 69, 9, 0 },   // Baum-Insel (hoch)
    };
    for (int i = 0; i < specs.length; i++) {
        int[] s = specs[i];
        double a = Math.toRadians(s[0]);
        int cx = (int) Math.round(Math.cos(a) * s[1]);
        int cz = (int) Math.round(Math.sin(a) * s[1]);
        int topY = s[2];
        double r = s[3];
        long seed = 7000L + i * 131L;

        buildSatelliteIslandsBody(es, cx, topY, cz, r, seed);
        switch (s[4]) {
            case 0 -> buildSatelliteIslandsTree(es, cx, topY, cz, seed);
            case 1 -> buildSatelliteIslandsLanternGrove(es, cx, topY, cz, seed);
            case 2 -> buildSatelliteIslandsTower(es, cx, topY, cz, seed);
            case 3 -> buildSatelliteIslandsAzaleaGarden(es, cx, topY, cz, r, seed);
            default -> buildSatelliteIslandsTree(es, cx, topY, cz, seed);
        }
        buildSatelliteIslandsRailing(es, cx, topY, cz, r);

        // Bruecke vom Rand der Hauptinsel zum Inselrand
        int rimX = (int) Math.round(Math.cos(a) * (R - 1));
        int rimZ = (int) Math.round(Math.sin(a) * (R - 1));
        buildSatelliteIslandsBridge(es, rimX, SY, rimZ, cx, topY, cz, r, seed);
    }
}

/** Insel-Koerper: organische Gras-Oberseite + umgekehrter Stein-Kegel + Tropfstein. */
private void buildSatelliteIslandsBody(EditSession es, int cx, int topY, int cz, double r, long seed) {
    int ri = (int) Math.ceil(r) + 2;
    // Oberseite mit wabbeligem Rand
    for (int x = cx - ri; x <= cx + ri; x++)
        for (int z = cz - ri; z <= cz + ri; z++) {
            int dx = x - cx, dz = z - cz;
            double edge = r + (noise(x, z, seed + 1) * 2.4 - 1.2);
            if (dx * dx + dz * dz > edge * edge) continue;
            set(es, x, topY, z, "grass_block");
            set(es, x, topY - 1, z, "dirt");
            set(es, x, topY - 2, z, noise(x, z, seed + 2) < 0.5 ? "dirt" : "coarse_dirt");
        }
    // Umgekehrter Fels-Kegel nach unten
    int depth = (int) Math.round(r) + 6;
    for (int h = 1; h <= depth; h++) {
        int y = topY - 2 - h;
        double t = 1.0 - (double) h / depth;
        double cr = r * t;
        if (cr < 0.7) { set(es, cx, y, cz, "dripstone_block"); continue; }
        int cri = (int) Math.ceil(cr);
        for (int x = cx - cri; x <= cx + cri; x++)
            for (int z = cz - cri; z <= cz + cri; z++) {
                int dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz > cr * cr) continue;
                double n = noise(x, z, seed + 30L + h);
                String id = n < 0.50 ? "stone"
                          : n < 0.74 ? "andesite"
                          : n < 0.88 ? "tuff"
                          : "deepslate";
                set(es, x, y, z, id);
            }
    }
    // Haengende Stalaktiten an der Unterseite
    int tipY = topY - 2 - depth;
    for (int k = 0; k < 4; k++) {
        int sx = cx + (int) Math.round((noise(k, 0, seed + 70) - 0.5) * r);
        int sz = cz + (int) Math.round((noise(0, k, seed + 71) - 0.5) * r);
        int len = 2 + (int) (noise(sx, sz, seed + 72) * 3);
        column(es, sx, sz, tipY + 1 - len, tipY + 2, "dripstone_block");
        set(es, sx, tipY + 1 - len, sz, "pointed_dripstone[vertical_direction=down]");
    }
    // Glueh-Akzent an der Spitze
    set(es, cx, tipY, cz, "shroomlight");
}

/** Mini-Eichenbaum mit Azaleen-Krone und haengender Laterne. */
private void buildSatelliteIslandsTree(EditSession es, int cx, int topY, int cz, long seed) {
    int base = topY + 1;
    int th = 4 + (int) (noise(cx, cz, seed + 5) * 3);   // Stammhoehe 4..6
    column(es, cx, cz, base, base + th, "oak_log[axis=y]");
    int crownY = base + th + 1;
    sphere(es, cx, crownY, cz, 3.2, "oak_leaves[persistent=true]");
    sphere(es, cx, crownY + 1, cz, 2.1, "flowering_azalea_leaves");
    // ein paar Glueh-Beeren in der Krone
    set(es, cx + 1, crownY, cz, "shroomlight");
    set(es, cx, crownY - 1, cz + 1, "lantern[hanging=true]");
    // Wurzelanlauf
    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
        set(es, cx + d[0], base, cz + d[1], "oak_wood");
    // verstreute Blumen am Fuss
    buildSatelliteIslandsFlora(es, cx, topY, cz, 4.0, seed + 9);
}

/** Hain aus Laternen-Pfosten auf einem Mosaik-Boden. */
private void buildSatelliteIslandsLanternGrove(EditSession es, int cx, int topY, int cz, long seed) {
    // poliertes Boden-Mosaik
    disk(es, cx, topY, cz, 4.0, "polished_andesite");
    ring(es, cx, topY, cz, 2.0, 2.6, "smooth_quartz");
    set(es, cx, topY, cz, "chiseled_quartz_block");
    // vier Laternen-Pfosten in den Ecken
    for (int[] d : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
        int px = cx + d[0], pz = cz + d[1];
        column(es, px, pz, topY + 1, topY + 3, "oak_fence");
        set(es, px, topY + 4, pz, "lantern[hanging=true]");
    }
    // Zentral-Leuchtfeuer
    column(es, cx, cz, topY + 1, topY + 2, "oak_fence");
    set(es, cx, topY + 3, cz, "sea_lantern");
    buildSatelliteIslandsFlora(es, cx, topY, cz, 5.5, seed + 14);
}

/** Kleiner Stein-Turm mit Zinnen und Leucht-Kern. */
private void buildSatelliteIslandsTower(EditSession es, int cx, int topY, int cz, long seed) {
    int h = 7 + (int) (noise(cx, cz, seed + 6) * 3);   // 7..9
    int top = topY + h;
    // Sockel
    disk(es, cx, topY, cz, 3.4, "cobblestone");
    // Turmschacht (hohl)
    tube(es, cx, topY + 1, cz, 1.6, 2.6, h, "stone_bricks");
    // Akzent-Baender
    ring(es, cx, topY + 2, cz, 1.6, 2.6, "chiseled_stone_bricks");
    ring(es, cx, top - 1, cz, 1.6, 2.6, "chiseled_stone_bricks");
    // Leucht-Kern innen
    column(es, cx, cz, topY + 1, top, "sea_lantern");
    // Zinnen-Kranz oben
    for (int i = 0; i < 8; i++) {
        double a = Math.PI * 2 * i / 8;
        int bx = cx + (int) Math.round(Math.cos(a) * 2.4);
        int bz = cz + (int) Math.round(Math.sin(a) * 2.4);
        set(es, bx, top + 1, bz, "stone_brick_wall");
    }
    // Fenster-Schlitze
    for (int[] d : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}})
        set(es, cx + d[0], topY + 3, cz + d[1], "glass_pane");
    // Tor
    set(es, cx + 2, topY + 1, cz, "oak_door[half=lower,facing=east]");
    set(es, cx + 2, topY + 2, cz, "oak_door[half=upper,facing=east]");
    buildSatelliteIslandsFlora(es, cx, topY, cz, 4.0, seed + 16);
}

/** Azaleen-Garten: bluehende Busche, Moos und Lampions. */
private void buildSatelliteIslandsAzaleaGarden(EditSession es, int cx, int topY, int cz, double r, long seed) {
    // Moos-Teppich
    int ri = (int) Math.ceil(r);
    for (int x = cx - ri; x <= cx + ri; x++)
        for (int z = cz - ri; z <= cz + ri; z++) {
            int dx = x - cx, dz = z - cz;
            if (dx * dx + dz * dz > (r - 1) * (r - 1)) continue;
            if (noise(x, z, seed + 21) < 0.45) set(es, x, topY, z, "moss_block");
        }
    // bluehende Azaleen-Busche
    for (int[] d : new int[][]{{2, 1}, {-2, -1}, {1, -2}, {-1, 2}, {0, 0}}) {
        int bx = cx + d[0], bz = cz + d[1];
        column(es, bx, bz, topY + 1, topY + 2, "oak_log[axis=y]");
        sphere(es, bx, topY + 3, bz, 1.8, "flowering_azalea_leaves");
    }
    // kleines Wasser-Becken
    set(es, cx, topY, cz, "water");
    // Lampions am Rand
    for (int[] d : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
        column(es, cx + d[0], cz + d[1], topY + 1, topY + 2, "oak_fence");
        set(es, cx + d[0], topY + 3, cz + d[1], "lantern[hanging=true]");
    }
    buildSatelliteIslandsFlora(es, cx, topY, cz, r - 1.0, seed + 24);
}

/** Deterministisch gestreute Blumen, Farne und Graeser auf der Insel-Oberseite. */
private void buildSatelliteIslandsFlora(EditSession es, int cx, int topY, int cz, double r, long seed) {
    String[] flowers = {"poppy", "dandelion", "cornflower", "oxeye_daisy",
                        "azure_bluet", "lily_of_the_valley", "allium"};
    int ri = (int) Math.ceil(r);
    for (int x = cx - ri; x <= cx + ri; x++)
        for (int z = cz - ri; z <= cz + ri; z++) {
            int dx = x - cx, dz = z - cz;
            if (dx * dx + dz * dz > r * r) continue;
            double n = noise(x, z, seed);
            if (n > 0.88)
                set(es, x, topY + 1, z, flowers[(int) (noise(x, z, seed + 3) * flowers.length) % flowers.length]);
            else if (n > 0.58)
                set(es, x, topY + 1, z, noise(x, z, seed + 4) < 0.3 ? "fern" : "short_grass");
        }
}

/** Eichenzaun-Gelaender mit Eck-Laternen rund um den Insel-Rand. */
private void buildSatelliteIslandsRailing(EditSession es, int cx, int topY, int cz, double r) {
    double rail = r - 0.5;
    ring(es, cx, topY + 1, cz, rail - 1.0, rail, "oak_fence");
    for (int i = 0; i < 6; i++) {
        double a = Math.PI * 2 * i / 6;
        int px = cx + (int) Math.round(Math.cos(a) * rail);
        int pz = cz + (int) Math.round(Math.sin(a) * rail);
        column(es, px, pz, topY + 1, topY + 2, "oak_fence");
        set(es, px, topY + 3, pz, "lantern");
    }
}

/**
 * Geschwungene Holzbruecke: durchhaengendes oak_planks-Deck (Katenoiden-Bogen),
 * oak_fence-Gelaender beidseitig und chain-Aufhaengung nach oben.
 */
private void buildSatelliteIslandsBridge(EditSession es, int x0, int y0, int z0,
                                         int x1, int y1, int z1, double r, long seed) {
    // Ziel knapp innerhalb des Inselrandes ansteuern
    double ddx = x1 - x0, ddz = z1 - z0;
    double len = Math.sqrt(ddx * ddx + ddz * ddz);
    if (len < 1) return;
    double ux = ddx / len, uz = ddz / len;
    int steps = (int) Math.round(len);
    // Bogen-Geometrie
    int yMin = Math.min(y0, y1);
    double sag = 2.0;        // Durchhang in der Mitte
    double crown = 2.5;      // Erhebung der Bruecken-Mitte ueber Endpunkte

    int prevDeckY = y0;
    for (int i = 0; i <= steps; i++) {
        double tt = (double) i / steps;
        double px = x0 + ddx * tt;
        double pz = z0 + ddz * tt;
        // sanfter Bogen: hoch in der Mitte, an den Enden auf Endhoehe interpoliert
        double endY = y0 + (y1 - y0) * tt;
        double arch = Math.sin(Math.PI * tt) * crown - Math.sin(Math.PI * tt) * sag * 0.0;
        int deckY = (int) Math.round(endY + arch);
        prevDeckY = deckY;
        int bx = (int) Math.round(px);
        int bz = (int) Math.round(pz);
        // Quer-Richtung (senkrecht zur Bruecke)
        double nx = -uz, nz = ux;

        // Deck (3 breit)
        for (int w = -1; w <= 1; w++) {
            int dx = (int) Math.round(bx + nx * w);
            int dz = (int) Math.round(bz + nz * w);
            set(es, dx, deckY, dz, "oak_planks");
            // Unterzug
            if (w == 0) set(es, dx, deckY - 1, dz, "oak_log[axis=y]");
        }
        // Gelaender beidseitig (w = -2 / +2), nicht ganz an den Enden
        if (i > 0 && i < steps) {
            for (int side : new int[]{-2, 2}) {
                int dx = (int) Math.round(bx + nx * side);
                int dz = (int) Math.round(bz + nz * side);
                set(es, dx, deckY, dz, "oak_fence");
                // alle paar Felder ein Laternen-Pfosten
                if (i % 4 == 0) {
                    set(es, dx, deckY + 1, dz, "oak_fence");
                    set(es, dx, deckY + 2, dz, "lantern[hanging=true]");
                }
            }
        }
        // Ketten-Aufhaengung: von Deckmitte nach oben in einem Bogen
        if (i % 2 == 0) {
            double susp = Math.sin(Math.PI * tt) * 3.0 + 2.0;
            int chTop = deckY + (int) Math.round(susp);
            column(es, bx, bz, deckY + 1, chTop, "chain[axis=y]");
            set(es, bx, chTop, bz, "lantern[hanging=true]");
        }
    }
    // Pfeiler/Verankerung am Insel-Ende unter dem Deck
    column(es, x1, z1, y1 - 5, y1 - 1, "oak_log[axis=y]");
    // Anschluss-Plattform am Hauptinsel-Rand
    for (int w = -1; w <= 1; w++) {
        int ax = (int) Math.round(x0 - uz * w);
        int az = (int) Math.round(z0 + ux * w);
        set(es, ax, y0, az, "oak_planks");
    }
}

    private void buildSkyDecor(EditSession es) {
    // Airspace above the island, y 100..132.
    // Sparse, tasteful floating ambiance: hanging sky-lantern clusters on chains,
    // a couple of hot-air balloons, and drifting white concrete cloud wisps.
    buildSkyDecorLanternClusters(es);
    buildSkyDecorBalloons(es);
    buildSkyDecorClouds(es);
}

// ---------------------------------------------------------------------------
// Hanging sky-lantern clusters: groups of lanterns dangling on short chains
// from invisible anchor points scattered across the airspace.
// ---------------------------------------------------------------------------
private void buildSkyDecorLanternClusters(EditSession es) {
    // Anchor points for clusters, kept within island radius (~50 to stay inset).
    int[][] anchors = {
        { 28, 120,  10 },
        { -22, 124, -18 },
        { 6, 128, -34 },
        { -38, 116, 14 },
        { 16, 122, 36 },
        { -8, 130, 30 },
        { 40, 118, -6 },
        { -30, 126, -40 }
    };
    for (int a = 0; a < anchors.length; a++) {
        int ax = anchors[a][0];
        int ay = anchors[a][1];
        int az = anchors[a][2];
        long seed = 9100L + a * 37L;
        if (!inDisk(ax, az, 52)) continue;
        buildSkyDecorOneCluster(es, ax, ay, az, seed);
    }
}

private void buildSkyDecorOneCluster(EditSession es, int ax, int ay, int az, long seed) {
    // Decorative supporting "branch" frame the lanterns hang from.
    set(es, ax, ay, az, "stripped_oak_log[axis=y]");
    int span = 2 + (int) Math.floor(noise(ax, az, seed) * 2.0); // 2..3
    line(es, ax - span, ay, az, ax + span, ay, az, "chain[axis=x]");
    line(es, ax, ay, az - span, ax, ay, az + span, "chain[axis=z]");

    // Number of dangling lanterns in this cluster.
    int count = 5 + (int) Math.floor(noise(ax + 3, az - 5, seed + 1) * 4.0); // 5..8
    for (int i = 0; i < count; i++) {
        // Spread lantern hang points around the small frame.
        double ang = (Math.PI * 2.0 * i) / count + noise(i * 13, az, seed + 7) * 0.9;
        double rad = 0.6 + noise(ax + i, az + i, seed + 11) * (double) span;
        int lx = ax + (int) Math.round(Math.cos(ang) * rad);
        int lz = az + (int) Math.round(Math.sin(ang) * rad);
        // Chain length varies for an organic dangling look.
        int chainLen = 2 + (int) Math.floor(noise(lx, lz, seed + 19) * 4.0); // 2..5
        int top = ay - 1;
        int bottom = top - chainLen + 1;
        if (bottom < 101) bottom = 101;
        // Hang the chain.
        for (int y = top; y >= bottom; y--) {
            set(es, lx, y, lz, "chain[axis=y]");
        }
        // Lantern at the very bottom.
        set(es, lx, bottom - 1, lz, "lantern[hanging=true]");
    }

    // A central, slightly larger soul-lantern accent for color contrast.
    if (noise(ax - 2, az + 2, seed + 23) > 0.45) {
        int cLen = 3 + (int) Math.floor(noise(ax, az, seed + 29) * 3.0);
        int top = ay - 1;
        int bottom = top - cLen + 1;
        if (bottom < 101) bottom = 101;
        for (int y = top; y >= bottom; y--) {
            set(es, ax, y, az, "chain[axis=y]");
        }
        set(es, ax, bottom - 1, az, "soul_lantern[hanging=true]");
    }
}

// ---------------------------------------------------------------------------
// Hot-air balloons: colored wool/concrete envelope spheres with a hanging
// basket and connecting ropes (chains).
// ---------------------------------------------------------------------------
private void buildSkyDecorBalloons(EditSession es) {
    // Each: cx, cz, envelope-center-y, radius, color index.
    int[][] balloons = {
        { -16, 22, 126, 6, 0 },
        { 34, -24, 122, 5, 1 },
        { 4, 44, 130, 5, 2 }
    };
    String[] mainColors = {
        "red_concrete", "yellow_concrete", "light_blue_concrete"
    };
    String[] stripeColors = {
        "white_concrete", "orange_concrete", "white_concrete"
    };
    for (int b = 0; b < balloons.length; b++) {
        int cx = balloons[b][0];
        int cz = balloons[b][2 - 2 + 1]; // placeholder safe index
        cz = balloons[b][1];
        int cy = balloons[b][2];
        int r = balloons[b][3];
        int ci = balloons[b][4];
        if (!inDisk(cx, cz, 50)) continue;
        buildSkyDecorOneBalloon(es, cx, cy, cz, r,
            mainColors[ci], stripeColors[ci], 9500L + b * 53L);
    }
}

private void buildSkyDecorOneBalloon(EditSession es, int cx, int cy, int cz,
                                     int r, String main, String stripe, long seed) {
    // Envelope: a teardrop-ish ellipsoid (taller than wide).
    ellipsoid(es, cx, cy, cz, r, r + 1, r, main);
    // Vertical accent stripes by overwriting thin meridian columns.
    int stripes = 6;
    for (int s = 0; s < stripes; s++) {
        double ang = (Math.PI * 2.0 * s) / stripes;
        int sx = cx + (int) Math.round(Math.cos(ang) * r);
        int sz = cz + (int) Math.round(Math.sin(ang) * r);
        // Only color the outer-most shell column at this meridian.
        for (int y = cy - r; y <= cy + r + 1; y++) {
            int dy = y - cy;
            // Stay roughly on the ellipsoid surface band.
            double t = 1.0 - (double) (dy * dy) / (double) ((r + 1) * (r + 1));
            if (t <= 0) continue;
            double ringR = r * Math.sqrt(t);
            int ex = cx + (int) Math.round(Math.cos(ang) * ringR);
            int ez = cz + (int) Math.round(Math.sin(ang) * ringR);
            set(es, ex, y, ez, stripe);
        }
    }
    // Top crown cap accent.
    set(es, cx, cy + r + 2, cz, stripe);
    set(es, cx, cy + r + 1, cz, stripe);

    // Bottom mouth opening hint (darker rim).
    int mouthY = cy - r;
    ring(es, cx, mouthY, cz, 1.0, 2.4, "stripped_oak_log[axis=y]");

    // Ropes from envelope mouth down to the basket corners.
    int basketTop = mouthY - 4;
    int half = 1; // basket is 3x3 (half=1)
    int[][] corners = {
        { cx - half, cz - half }, { cx + half, cz - half },
        { cx - half, cz + half }, { cx + half, cz + half }
    };
    for (int[] c : corners) {
        for (int y = mouthY - 1; y >= basketTop + 2; y--) {
            set(es, c[0], y, c[1], "chain[axis=y]");
        }
    }

    // The basket: a small hollow box of stripped logs / fence trim.
    int bTop = basketTop + 1;
    int bBottom = basketTop - 2;
    if (bBottom < 100) bBottom = 100;
    hollowBox(es, cx - half, bBottom, cz - half, cx + half, bTop, cz + half,
        "stripped_oak_log[axis=y]");
    // Solid floor for the basket.
    fill(es, cx - half, bBottom, cz - half, cx + half, bBottom, cz + half,
        "oak_planks");
    // Fence rim around the top edge for a woven look.
    walls(es, cx - half, bTop + 1, cz - half, cx + half, bTop + 1, cz + half,
        "oak_fence");

    // A little glow inside the basket so balloons read at night.
    set(es, cx, bBottom + 1, cz, "lantern");

    // Burner flame accent just above basket (subtle, deterministic).
    if (noise(cx, cz, seed) > 0.3) {
        set(es, cx, bTop + 1, cz, "shroomlight");
    }
}

// ---------------------------------------------------------------------------
// Drifting cloud wisps: soft, flattened white concrete blobs scattered high
// in the airspace, layered with concrete_powder for a fluffy edge.
// ---------------------------------------------------------------------------
private void buildSkyDecorClouds(EditSession es) {
    // Each wisp: cx, cz, cy, length scale.
    int[][] wisps = {
        { 20, -8, 131, 7 },
        { -26, 16, 129, 6 },
        { 0, 30, 132, 8 },
        { 36, 28, 128, 5 },
        { -14, -36, 130, 6 }
    };
    for (int w = 0; w < wisps.length; w++) {
        int cx = wisps[w][0];
        int cz = wisps[w][1];
        int cy = wisps[w][2];
        int len = wisps[w][3];
        if (!inDisk(cx, cz, 54)) continue;
        buildSkyDecorOneCloud(es, cx, cy, cz, len, 9800L + w * 61L);
    }
}

private void buildSkyDecorOneCloud(EditSession es, int cx, int cy, int cz,
                                   int len, long seed) {
    // A wisp is a chain of overlapping flattened ellipsoids along a drift axis.
    int blobs = 3 + (int) Math.floor(noise(cx, cz, seed) * 3.0); // 3..5
    // Drift direction (mostly horizontal, slight diagonal).
    double dirX = Math.cos(noise(cx + 5, cz - 5, seed + 3) * Math.PI * 2.0);
    double dirZ = Math.sin(noise(cx - 7, cz + 7, seed + 9) * Math.PI * 2.0);
    for (int i = 0; i < blobs; i++) {
        double frac = (double) i / (double) (blobs - 1) - 0.5;
        int bx = cx + (int) Math.round(dirX * frac * len);
        int bz = cz + (int) Math.round(dirZ * frac * len);
        int by = cy + (int) Math.round(noise(bx, bz, seed + 13) * 2.0 - 1.0);
        // Flattened: wide and shallow.
        double rx = 2.0 + noise(bx, bz, seed + 17) * 2.5;
        double rz = 2.0 + noise(bx + 3, bz - 3, seed + 21) * 2.5;
        double ry = 1.0 + noise(bx - 4, bz + 4, seed + 27) * 0.6;
        ellipsoid(es, bx, by, bz, rx, ry, rz, "white_concrete");
        // Soft fluffy fringe of snow_block / concrete_powder on the edges.
        if (noise(bx, bz, seed + 31) > 0.4) {
            ellipsoid(es, bx, by + 1, bz, rx * 0.6, ry * 0.5, rz * 0.6,
                "white_concrete_powder");
        }
    }
    // Sparse fluffy bumps scattered along the top surface for organic texture.
    for (int s = 0; s < len; s++) {
        double ang = (Math.PI * 2.0 * s) / len + noise(s * 11, cz, seed + 41);
        double rad = noise(cx + s, cz - s, seed + 43) * (len * 0.4);
        int px = cx + (int) Math.round(Math.cos(ang) * rad);
        int pz = cz + (int) Math.round(Math.sin(ang) * rad);
        int py = cy + 1 + (int) Math.floor(noise(px, pz, seed + 47) * 2.0);
        if (py > 132) py = 132;
        if (noise(px, pz, seed + 53) > 0.5) {
            set(es, px, py, pz, "white_concrete_powder");
        }
    }
}

    private void buildLighting(EditSession es) {
    // ===========================================================
    // GLOBAL LIGHTING PASS  (runs LAST)
    // Goal: even ambient illumination across the island surface to
    // suppress mob spawns, plus warm under-glow on the underside.
    // We work flush into existing geometry where possible, only
    // replacing air / common natural blocks so we don't smash detail.
    // ===========================================================

    final long SEED = 91247L;

    // ---- 1. EVEN SEA_LANTERN GRID FLUSH INTO THE SURFACE --------
    // A regular grid keeps light levels uniform (anti-spawn). We embed
    // the lantern one block below the surface so its top sits flush
    // with grass/path at y = SY-1, glowing through transparent gaps and
    // lighting the walkable layer above. Skip the plaza center so we
    // never stack lights on the showcase area.
    buildLightingSurfaceGrid(es, SEED);

    // ---- 2. PERIMETER & RADIAL AVENUE LANTERNS ------------------
    // Extra hanging lanterns trace the avenues radiating from center
    // and a soft ring near the island rim, giving directional warmth.
    buildLightingAvenues(es, SEED);

    // ---- 3. ACCENT RING OF SEA LANTERNS AT MID RADIUS -----------
    buildLightingAccentRings(es, SEED);

    // ---- 4. UNDERSIDE WARM GLOW ---------------------------------
    // Glowstone + shroomlight scattered on the rocky underside for the
    // classic warm under-glow seen from below the floating island.
    buildLightingUnderside(es, SEED);
}

// ------------------------------------------------------------------
// Even surface grid of recessed sea_lanterns.
// ------------------------------------------------------------------
private void buildLightingSurfaceGrid(EditSession es, long seed) {
    final int STEP = 7;          // grid spacing -> overlapping light radius
    final int plazaR = 14;       // keep the plaza center clear
    for (int x = -R; x <= R; x += STEP) {
        for (int z = -R; z <= R; z += STEP) {
            // jitter the grid slightly so it reads organic, not mechanical
            int jx = (int) Math.round((noise(x, z, seed) - 0.5) * 2.0);
            int jz = (int) Math.round((noise(x + 31, z - 17, seed) - 0.5) * 2.0);
            int px = x + jx;
            int pz = z + jz;
            if (!inDisk(px, pz, R - 2)) continue;
            // avoid stacking on the plaza center
            if (inDisk(px, pz, plazaR)) continue;

            // Recess the lantern just under the top soil. Sea lanterns
            // emit full light and double as anti-spawn flooring.
            set(es, px, SY - 1, pz, "sea_lantern");

            // Occasionally cap with a flush light source visible from
            // above where the grid lands on a path rather than grass.
            double pick = noise(px - 9, pz + 23, seed);
            if (pick > 0.72) {
                set(es, px, SY, pz, "sea_lantern");
            } else if (pick > 0.50) {
                // small lantern post for visual interest on open lawn
                set(es, px, SY, pz, "lantern");
            }
        }
    }
}

// ------------------------------------------------------------------
// Avenue + rim hanging lanterns.
// ------------------------------------------------------------------
private void buildLightingAvenues(EditSession es, long seed) {
    // Eight radial avenues from center outward.
    int spokes = 8;
    for (int s = 0; s < spokes; s++) {
        double ang = (Math.PI * 2.0 * s) / spokes;
        double dx = Math.cos(ang);
        double dz = Math.sin(ang);
        for (int rDist = 16; rDist <= R - 4; rDist += 5) {
            int px = (int) Math.round(dx * rDist);
            int pz = (int) Math.round(dz * rDist);
            if (!inDisk(px, pz, R - 2)) continue;

            // A small post with a hanging lantern: chain + lantern flush
            // glow plus a sea_lantern base recessed for spawn-proofing.
            set(es, px, SY - 1, pz, "sea_lantern");
            // post made of stripped log with a top lantern
            int h = 2 + (int) Math.round(noise(px, pz, seed) * 1.0);
            for (int y = 0; y < h; y++) {
                set(es, px, SY + y, pz, "deepslate_tile_wall");
            }
            set(es, px, SY + h, pz, "sea_lantern");
            set(es, px, SY + h + 1, pz, "lantern[hanging=true]");
        }
    }

    // Soft rim ring of lanterns just inside the edge.
    double rimR = R - 4;
    int rimCount = 64;
    for (int i = 0; i < rimCount; i++) {
        double ang = (Math.PI * 2.0 * i) / rimCount;
        int px = (int) Math.round(Math.cos(ang) * rimR);
        int pz = (int) Math.round(Math.sin(ang) * rimR);
        if (!inDisk(px, pz, R - 1)) continue;
        set(es, px, SY - 1, pz, "sea_lantern");
        if (noise(px, pz, seed + 5) > 0.4) {
            set(es, px, SY, pz, "lantern");
        }
    }
}

// ------------------------------------------------------------------
// Concentric accent rings of sea_lanterns embedded in the ground.
// ------------------------------------------------------------------
private void buildLightingAccentRings(EditSession es, long seed) {
    double[] radii = { 24.0, 38.0, 52.0 };
    for (double rr : radii) {
        int segments = (int) Math.round(rr * 1.6);
        for (int i = 0; i < segments; i++) {
            double ang = (Math.PI * 2.0 * i) / segments;
            int px = (int) Math.round(Math.cos(ang) * rr);
            int pz = (int) Math.round(Math.sin(ang) * rr);
            if (!inDisk(px, pz, R - 2)) continue;
            // alternate sea_lantern and recessed glow for a dashed look
            double n = noise(px, pz, seed + 11);
            if (n > 0.35) {
                set(es, px, SY - 1, pz, "sea_lantern");
            }
            if (n > 0.85) {
                set(es, px, SY, pz, "end_rod[facing=up]");
            }
        }
    }
}

// ------------------------------------------------------------------
// Underside warm glow: glowstone + shroomlight clusters hanging on
// the rocky belly of the floating island.
// ------------------------------------------------------------------
private void buildLightingUnderside(EditSession es, long seed) {
    // The underside is roughly a cone tapering down from the rim. We
    // approximate its depth at a given radius and seed warm lights into
    // the rock there, only where solid-ish (we just place; the rock
    // already exists from earlier regions).
    final int STEP = 9;
    for (int x = -R; x <= R; x += STEP) {
        for (int z = -R; z <= R; z += STEP) {
            int jx = (int) Math.round((noise(x + 7, z, seed + 3) - 0.5) * 4.0);
            int jz = (int) Math.round((noise(x, z + 7, seed + 3) - 0.5) * 4.0);
            int px = x + jx;
            int pz = z + jz;
            double d = Math.sqrt(px * (double) px + pz * (double) pz);
            if (d > R - 3) continue;

            // Estimate underside depth: deeper toward center (cone belly).
            double t = 1.0 - (d / (double) R);           // 0 at rim, 1 center
            int depth = (int) Math.round(2 + t * 22.0);  // how far below SY-1
            int by = SY - 2 - depth;

            // Place a warm light embedded in the belly rock.
            double choose = noise(px, pz, seed + 19);
            String warm = choose > 0.5 ? "shroomlight" : "glowstone";
            set(es, px, by, pz, warm);

            // Small hanging cluster below for drip-glow.
            double cl = noise(px - 5, pz + 5, seed + 23);
            if (cl > 0.6) {
                set(es, px, by - 1, pz, warm);
            }
            if (cl > 0.82) {
                set(es, px, by - 2, pz, "shroomlight");
                // a dangling chain + lantern for extra atmosphere
                set(es, px, by - 3, pz, "chain[axis=y]");
                set(es, px, by - 4, pz, "lantern[hanging=true]");
            }
        }
    }

    // A denser warm core directly beneath (but not on) the plaza, kept
    // well below the surface so we never stack on the plaza center.
    int coreBase = SY - 26;
    for (int i = 0; i < 40; i++) {
        double ang = noise(i, i * 3, seed + 31) * Math.PI * 2.0;
        double rad = noise(i * 7, i, seed + 37) * 12.0;
        int px = (int) Math.round(Math.cos(ang) * rad);
        int pz = (int) Math.round(Math.sin(ang) * rad);
        int py = coreBase - (int) Math.round(noise(px, pz, seed + 41) * 6.0);
        set(es, px, py, pz, "shroomlight");
        if (noise(px + 2, pz - 2, seed + 43) > 0.6) {
            set(es, px, py - 1, pz, "glowstone");
        }
    }
}

    private boolean inDisk(int dx, int dz, double r) {
        return dx * dx + dz * dz <= r * r;
    }
}
