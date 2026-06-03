package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockTypes;

public class ArenaBuilder extends BuildHelper {

    private static final int AY = 64;

    public ArenaBuilder(LemonPractice plugin, org.bukkit.World world) {
        super(plugin, world);
    }

    @Override
    public void build() {
        buildJungleTempleArena();
        buildBeachArena();
        buildBambooArena();
        buildMangroveArena();
        buildVolcanoArena();
        buildCoralArena();
        buildRuinsArena();
        buildCliffsideArena();
        buildJungleFortressFFA();
        buildIslandFFA();
        buildPyramidFFA();
        buildMangroveBayFFA();
        buildVolcanoHighlandsFFA();
    }

    private EditSession newSession() {
        return WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world)).fastMode(true).build();
    }

    // =========================================================================
    // ARENA 1 — JUNGLE TEMPLE   cx=300, cz=0   (130x130)
    // =========================================================================
    private void buildJungleTempleArena() {
        final int cx = 300, cz = 0;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Podzol patches
            int[] px = {-40,-20,10,30,-50,50,-35,35,0,45,-45,15};
            int[] pz = {30,-40,50,-20,10,-30,45,-45,40,-15,-5,20};
            for (int i = 0; i < px.length; i++)
                fill(es, cx+px[i]-2, AY, cz+pz[i]-2, cx+px[i]+2, AY, cz+pz[i]+2, BlockTypes.PODZOL);
            // Cross paths
            fill(es, cx-1, AY, cz-65, cx+1, AY, cz+65, BlockTypes.COARSE_DIRT);
            fill(es, cx-65, AY, cz-1, cx+65, AY, cz+1, BlockTypes.COARSE_DIRT);
            // Temple 3-step base
            fill(es, cx-12, AY+1, cz-12, cx+12, AY+1, cz+12, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-10, AY+2, cz-10, cx+10, AY+2, cz+10, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-8,  AY+3, cz-8,  cx+8,  AY+3, cz+8,  BlockTypes.STONE_BRICKS);
            // Temple walls hollow
            fillHollow(es, cx-8, AY+3, cz-8, cx+8, AY+15, cz+8, BlockTypes.STONE_BRICKS, BlockTypes.AIR);
            // Chiseled accents every 4 blocks
            for (int y = AY+3; y <= AY+15; y += 4) {
                for (int x = cx-8; x <= cx+8; x += 4) {
                    block(es, x, y, cz-8, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, x, y, cz+8, BlockTypes.CHISELED_STONE_BRICKS);
                }
                for (int z = cz-8; z <= cz+8; z += 4) {
                    block(es, cx-8, y, z, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, cx+8, y, z, BlockTypes.CHISELED_STONE_BRICKS);
                }
            }
            // 4 gateways
            for (int y = AY+3; y <= AY+9; y++) {
                fill(es, cx-2, y, cz-8, cx+2, y, cz-8, BlockTypes.AIR);
                fill(es, cx-2, y, cz+8, cx+2, y, cz+8, BlockTypes.AIR);
                fill(es, cx-8, y, cz-2, cx-8, y, cz+2, BlockTypes.AIR);
                fill(es, cx+8, y, cz-2, cx+8, y, cz+2, BlockTypes.AIR);
            }
            // Temple floor lanterns
            for (int x = cx-6; x <= cx+6; x += 6)
                for (int z = cz-6; z <= cz+6; z += 6)
                    block(es, x, AY+3, z, BlockTypes.SEA_LANTERN);
            // Step roof
            fill(es, cx-8, AY+16, cz-8, cx+8, AY+16, cz+8, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-6, AY+17, cz-6, cx+6, AY+17, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-4, AY+18, cz-4, cx+4, AY+18, cz+4, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, cx, AY+19, cz, BlockTypes.SEA_LANTERN);
            // 4 interior jungle columns
            int[][] cols = {{cx-5,cz-5},{cx+4,cz-5},{cx-5,cz+4},{cx+4,cz+4}};
            for (int[] c : cols) {
                fill(es, c[0], AY+4, c[1], c[0]+1, AY+14, c[1]+1, BlockTypes.JUNGLE_LOG);
                fill(es, c[0]-2, AY+14, c[1]-2, c[0]+3, AY+16, c[1]+3, BlockTypes.JUNGLE_LEAVES);
            }
            // 8 outer totem pillars at r=50
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int tx = cx + (int)Math.round(50 * Math.cos(a));
                int tz = cz + (int)Math.round(50 * Math.sin(a));
                for (int y = AY+1; y <= AY+10; y++)
                    block(es, tx, y, tz, (y%2==0) ? BlockTypes.JUNGLE_LOG : BlockTypes.ACACIA_LOG);
                fill(es, tx-1, AY+11, tz-1, tx+1, AY+13, tz+1, BlockTypes.JUNGLE_LEAVES);
            }
            // Perimeter wall mossy cobblestone 4 tall
            for (int y = AY+1; y <= AY+4; y++) {
                fill(es, cx-63, y, cz-63, cx+63, y, cz-63, BlockTypes.MOSSY_COBBLESTONE);
                fill(es, cx-63, y, cz+63, cx+63, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
                fill(es, cx-63, y, cz-63, cx-63, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
                fill(es, cx+63, y, cz-63, cx+63, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
            }
            // Moss patches on wall
            for (int wx = cx-57; wx <= cx+57; wx += 6) {
                fill(es, wx, AY+2, cz-63, wx+1, AY+3, cz-63, BlockTypes.MOSS_BLOCK);
                fill(es, wx, AY+2, cz+63, wx+1, AY+3, cz+63, BlockTypes.MOSS_BLOCK);
            }
            // 12 bamboo clusters
            int[] bx2 = {-48,-30,-55,20,48,-25,55,-42,38,-15,42,-38};
            int[] bz2 = {20,48,-35,-50,38,55,-20,-5,55,-30,-45,45};
            for (int i = 0; i < 12; i++) {
                int h = 4 + (i % 3);
                for (int dx = 0; dx <= 1; dx++)
                    fill(es, cx+bx2[i]+dx, AY+1, cz+bz2[i], cx+bx2[i]+dx, AY+h, cz+bz2[i], BlockTypes.BAMBOO);
            }
            // 6 fallen logs
            int[] lx = {-55,-40,25,45,-30,10};
            int[] lz = {-30,50,-55,20,-5,58};
            for (int i = 0; i < 6; i++)
                fill(es, cx+lx[i], AY+1, cz+lz[i], cx+lx[i]+6, AY+1, cz+lz[i], BlockTypes.OAK_LOG);
            // 16 lantern posts
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                int llx = cx + (int)Math.round(40 * Math.cos(a));
                int llz = cz + (int)Math.round(40 * Math.sin(a));
                column(es, llx, llz, AY+1, AY+3, BlockTypes.OAK_FENCE);
                block(es, llx, AY+4, llz, BlockTypes.LANTERN);
            }
            // 4 water pools
            int[][] pools = {{cx+35,cz+35},{cx-37,cz+35},{cx+35,cz-37},{cx-37,cz-37}};
            for (int[] p : pools) {
                fill(es, p[0]-2, AY-1, p[1]-2, p[0]+2, AY-1, p[1]+2, BlockTypes.STONE);
                fill(es, p[0]-2, AY,   p[1]-2, p[0]+2, AY,   p[1]+2, BlockTypes.WATER);
            }
            // Hanging roots on wall top
            for (int x = cx-8; x <= cx+8; x += 2) {
                block(es, x, AY+16, cz-8, BlockTypes.HANGING_ROOTS);
                block(es, x, AY+16, cz+8, BlockTypes.HANGING_ROOTS);
            }
            // Spawn pads
            int[][] sp = {{cx,cz-45},{cx,cz+45},{cx-45,cz},{cx+45,cz}};
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.MOSSY_STONE_BRICKS);
        }
    }

    // =========================================================================
    // ARENA 2 — TROPICAL BEACH   cx=-300, cz=0   (130x130)
    // =========================================================================
    private void buildBeachArena() {
        final int cx = -300, cz = 0;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            // Ocean moat (outer 15 ring)
            fill(es, cx-65, 62, cz-65, cx+65, 63, cz+65, BlockTypes.SAND);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.WATER);
            // Island floor
            fill(es, cx-50, 62, cz-50, cx+50, 63, cz+50, BlockTypes.SANDSTONE);
            fill(es, cx-50, AY, cz-50, cx+50, AY, cz+50, BlockTypes.SAND);
            // Raised grass areas
            fill(es, cx-28, AY+1, cz-18, cx-6, AY+1, cz+18, BlockTypes.GRASS_BLOCK);
            fill(es, cx+6,  AY+1, cz-18, cx+28, AY+1, cz+18, BlockTypes.GRASS_BLOCK);
            fill(es, cx-18, AY+1, cz+27, cx+18, AY+1, cz+44, BlockTypes.GRASS_BLOCK);
            // Sand wall around island
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-50, y, cz-50, cx+50, y, cz-50, BlockTypes.SANDSTONE);
                fill(es, cx-50, y, cz+50, cx+50, y, cz+50, BlockTypes.SANDSTONE);
                fill(es, cx-50, y, cz-50, cx-50, y, cz+50, BlockTypes.SANDSTONE);
                fill(es, cx+50, y, cz-50, cx+50, y, cz+50, BlockTypes.SANDSTONE);
            }
            // 4 gateways
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-2, y, cz-50, cx+2, y, cz-50, BlockTypes.AIR);
                fill(es, cx-2, y, cz+50, cx+2, y, cz+50, BlockTypes.AIR);
                fill(es, cx-50, y, cz-2, cx-50, y, cz+2, BlockTypes.AIR);
                fill(es, cx+50, y, cz-2, cx+50, y, cz+2, BlockTypes.AIR);
            }
            // CUT_SANDSTONE stripes every 8 blocks
            for (int x = cx-48; x <= cx+48; x += 8)
                fill(es, x, AY, cz-48, x+1, AY, cz+48, BlockTypes.CUT_SANDSTONE);
            // Central platform
            fill(es, cx-7, AY+1, cz-7, cx+7, AY+2, cz+7, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-7, AY+1, cz-7, cx+7, AY+1, cz+7, BlockTypes.CHISELED_SANDSTONE);
            // Steps 4 sides
            for (int s = 0; s < 2; s++) {
                fill(es, cx-8-s, AY+s, cz-7, cx-8-s, AY+s, cz+7, BlockTypes.SANDSTONE);
                fill(es, cx+8+s, AY+s, cz-7, cx+8+s, AY+s, cz+7, BlockTypes.SANDSTONE);
                fill(es, cx-7, AY+s, cz-8-s, cx+7, AY+s, cz-8-s, BlockTypes.SANDSTONE);
                fill(es, cx-7, AY+s, cz+8+s, cx+7, AY+s, cz+8+s, BlockTypes.SANDSTONE);
            }
            // 4 sandstone arches at r=35
            int[][] archPos = {{cx,cz-35},{cx,cz+35},{cx-35,cz},{cx+35,cz}};
            for (int[] a : archPos) {
                fill(es, a[0]-4, AY+1, a[1], a[0]-4, AY+5, a[1], BlockTypes.SANDSTONE);
                fill(es, a[0]+4, AY+1, a[1], a[0]+4, AY+5, a[1], BlockTypes.SANDSTONE);
                fill(es, a[0]-4, AY+5, a[1], a[0]+4, AY+5, a[1], BlockTypes.CHISELED_SANDSTONE);
            }
            // 16 palm trees
            int[] palmX = {-42,-30,-18,-5,8,22,36,42,-40,-28,-12,6,20,35,-45,40};
            int[] palmZ = {-42,-28,-40,-45,-38,-42,-30,-45,42,38,44,42,38,42,-10,10};
            for (int i = 0; i < 16; i++) {
                if (Math.abs(palmX[i]) > 47 || Math.abs(palmZ[i]) > 47) continue;
                int px2 = cx + palmX[i], pz2 = cz + palmZ[i];
                column(es, px2, pz2, AY+1, AY+8, BlockTypes.JUNGLE_LOG);
                fill(es, px2-2, AY+8, pz2-2, px2+2, AY+9, pz2+2, BlockTypes.JUNGLE_LEAVES);
                fill(es, px2-1, AY+10, pz2-1, px2+1, AY+10, pz2+1, BlockTypes.JUNGLE_LEAVES);
                block(es, px2, AY+11, pz2, BlockTypes.JUNGLE_LEAVES);
            }
            // Scattered dead bush / grass
            for (int i = 0; i < 40; i++) {
                int sx = cx - 48 + (i*13+7) % 97;
                int sz = cz - 48 + (i*17+3) % 97;
                block(es, sx, AY+1, sz, (i%3==0) ? BlockTypes.DEAD_BUSH : BlockTypes.SHORT_GRASS);
            }
            // Coral in moat
            com.sk89q.worldedit.world.block.BlockType[] corals = {
                BlockTypes.BRAIN_CORAL_BLOCK, BlockTypes.TUBE_CORAL_BLOCK,
                BlockTypes.HORN_CORAL_BLOCK, BlockTypes.BUBBLE_CORAL_BLOCK, BlockTypes.FIRE_CORAL_BLOCK
            };
            for (int i = 0; i < 20; i++) {
                int rx = cx - 63 + (i*11+5) % 127;
                int rz = cz - 63 + (i*7+9)  % 127;
                if (Math.abs(rx-cx) >= 50 || Math.abs(rz-cz) >= 50)
                    block(es, rx, 62, rz, corals[i % corals.length]);
            }
            // Spawn pads
            int[][] sp = {{cx,cz-38},{cx,cz+38},{cx-38,cz},{cx+38,cz}};
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.SANDSTONE);
        }
    }

    // =========================================================================
    // ARENA 3 — BAMBOO FOREST   cx=0, cz=300   (130x130)
    // =========================================================================
    private void buildBambooArena() {
        final int cx = 0, cz = 300;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Moss patches
            int[] mx = {-50,-30,-55,40,55,-20,30,-45,20,-55,45,-10};
            int[] mz = {30,-50,10,-40,30,-55,55,-45,-30,5,-10,50};
            for (int i = 0; i < mx.length; i++)
                fill(es, cx+mx[i]-2, AY, cz+mz[i]-2, cx+mx[i]+2, AY, cz+mz[i]+2, BlockTypes.MOSS_BLOCK);
            // 4 paths (3-wide bamboo planks)
            fill(es, cx-1, AY, cz-65, cx+1, AY, cz-13, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-1, AY, cz+13, cx+1, AY, cz+65, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-65, AY, cz-1, cx-13, AY, cz+1, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx+13, AY, cz-1, cx+65, AY, cz+1, BlockTypes.BAMBOO_PLANKS);
            // Central clearing
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz+12, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz-11, BlockTypes.STONE_BRICKS);
            fill(es, cx-12, AY, cz+11, cx+12, AY, cz+12, BlockTypes.STONE_BRICKS);
            fill(es, cx-12, AY, cz-11, cx-11, AY, cz+11, BlockTypes.STONE_BRICKS);
            fill(es, cx+11, AY, cz-11, cx+12, AY, cz+11, BlockTypes.STONE_BRICKS);
            // Central shrine
            int[][] shr = {{cx-4,cz-4},{cx+3,cz-4},{cx-4,cz+3},{cx+3,cz+3}};
            for (int[] s : shr)
                fill(es, s[0], AY+1, s[1], s[0]+1, AY+9, s[1]+1, BlockTypes.CHERRY_LOG);
            fill(es, cx-6, AY+10, cz-6, cx+7, AY+12, cz+7, BlockTypes.CHERRY_LEAVES);
            fill(es, cx-4, AY+13, cz-4, cx+5, AY+13, cz+5, BlockTypes.CHERRY_LEAVES);
            // Bamboo block wall 6 tall
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-63, y, cz-63, cx+63, y, cz-63, BlockTypes.BAMBOO_BLOCK);
                fill(es, cx-63, y, cz+63, cx+63, y, cz+63, BlockTypes.BAMBOO_BLOCK);
                fill(es, cx-63, y, cz-63, cx-63, y, cz+63, BlockTypes.BAMBOO_BLOCK);
                fill(es, cx+63, y, cz-63, cx+63, y, cz+63, BlockTypes.BAMBOO_BLOCK);
            }
            // Gate openings
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-2, y, cz-63, cx+2, y, cz-63, BlockTypes.AIR);
                fill(es, cx-2, y, cz+63, cx+2, y, cz+63, BlockTypes.AIR);
                fill(es, cx-63, y, cz-2, cx-63, y, cz+2, BlockTypes.AIR);
                fill(es, cx+63, y, cz-2, cx+63, y, cz+2, BlockTypes.AIR);
            }
            // Dense bamboo stalks in outer ring
            for (int bx = cx-62; bx <= cx+62; bx += 2) {
                for (int bz3 = cz-62; bz3 <= cz+62; bz3 += 2) {
                    if (Math.abs(bx-cx) <= 13 && Math.abs(bz3-cz) <= 13) continue;
                    if (Math.abs(bx-cx) <= 2 || Math.abs(bz3-cz) <= 2) continue;
                    int h = 4 + ((bx + bz3 + 200) % 4);
                    column(es, bx, bz3, AY+1, AY+h, BlockTypes.BAMBOO);
                }
            }
            // 8 cherry trees mid-ring
            for (int i = 0; i < 8; i++) {
                double a = (i + 0.5) * Math.PI / 4;
                int tx = cx + (int)Math.round(38 * Math.cos(a));
                int tz = cz + (int)Math.round(38 * Math.sin(a));
                column(es, tx, tz, AY+1, AY+5, BlockTypes.CHERRY_LOG);
                fill(es, tx-1, AY+6, tz-1, tx+1, AY+7, tz+1, BlockTypes.CHERRY_LEAVES);
            }
            // Lantern posts on paths
            for (int off : new int[]{-50,-38,-26,20,32,44}) {
                column(es, cx, cz+off, AY+1, AY+3, BlockTypes.BAMBOO_FENCE);
                block(es, cx, AY+4, cz+off, BlockTypes.LANTERN);
                column(es, cx+off, cz, AY+1, AY+3, BlockTypes.BAMBOO_FENCE);
                block(es, cx+off, AY+4, cz, BlockTypes.LANTERN);
            }
            // 4 bamboo gazebos at diagonal r=35
            int[][] gaz = {{cx-25,cz-25},{cx+24,cz-25},{cx-25,cz+24},{cx+24,cz+24}};
            for (int[] g : gaz) {
                fill(es, g[0]-2, AY+1, g[1]-2, g[0]+2, AY+1, g[1]+2, BlockTypes.BAMBOO_PLANKS);
                for (int[] c2 : new int[][]{{g[0]-2,g[1]-2},{g[0]+2,g[1]-2},{g[0]-2,g[1]+2},{g[0]+2,g[1]+2}})
                    column(es, c2[0], c2[1], AY+2, AY+5, BlockTypes.BAMBOO_BLOCK);
                fill(es, g[0]-2, AY+5, g[1]-2, g[0]+2, AY+5, g[1]+2, BlockTypes.BAMBOO_PLANKS);
            }
            // Spawn pads on paths
            int[][] sp = {{cx,cz-40},{cx,cz+40},{cx-40,cz},{cx+40,cz}};
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.BAMBOO_PLANKS);
        }
    }

    // =========================================================================
    // ARENA 4 — MANGROVE SWAMP   cx=0, cz=-300   (130x130)
    // =========================================================================
    private void buildMangroveArena() {
        final int cx = 0, cz = -300;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            // West half: water
            fill(es, cx-65, 61, cz-65, cx+10, 61, cz+65, BlockTypes.MUD);
            fill(es, cx-65, 62, cz-65, cx+10, AY, cz+65, BlockTypes.WATER);
            // East half: land
            fill(es, cx+10, 62, cz-65, cx+65, 63, cz+65, BlockTypes.MUD);
            fill(es, cx+10, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Land bridge
            fill(es, cx-2, AY+1, cz-65, cx+2, AY+1, cz+65, BlockTypes.PACKED_MUD);
            // 3 docks spanning the water
            for (int dz2 : new int[]{cz-30, cz, cz+30}) {
                fill(es, cx-65, AY+2, dz2-1, cx+10, AY+2, dz2+1, BlockTypes.MANGROVE_PLANKS);
                for (int x = cx-64; x <= cx+9; x++) {
                    block(es, x, AY+3, dz2-1, BlockTypes.MANGROVE_FENCE);
                    block(es, x, AY+3, dz2+1, BlockTypes.MANGROVE_FENCE);
                }
                for (int x = cx-60; x <= cx; x += 10) {
                    column(es, x, dz2, AY+2, AY+4, BlockTypes.MANGROVE_FENCE);
                    block(es, x, AY+5, dz2, BlockTypes.LANTERN);
                }
            }
            // Mangrove log posts in water
            for (int i = 0; i < 30; i++) {
                int ppx = cx - 62 + (i*11+5) % 70;
                int ppz = cz - 62 + (i*7+3)  % 127;
                if (ppx < cx+9) column(es, ppx, ppz, 61, AY+3, BlockTypes.MANGROVE_LOG);
            }
            // 12 mangrove trees on east
            int[] tmx = {15,22,30,40,50,60,18,35,48,25,55,45};
            int[] tmz = {-50,-20,10,-35,30,-50,55,-55,50,-5,-15,55};
            for (int i = 0; i < 12; i++) {
                int tx = cx+tmx[i], tz = cz+tmz[i];
                column(es, tx, tz, AY+1, AY+7, BlockTypes.MANGROVE_LOG);
                fill(es, tx-2, AY+7, tz-2, tx+2, AY+9, tz+2, BlockTypes.MANGROVE_LEAVES);
                fill(es, tx-1, AY+10, tz-1, tx+1, AY+10, tz+1, BlockTypes.MANGROVE_LEAVES);
            }
            // East perimeter wall (moss block)
            for (int y = AY+1; y <= AY+4; y++) {
                fill(es, cx+10, y, cz-65, cx+65, y, cz-65, BlockTypes.MOSS_BLOCK);
                fill(es, cx+10, y, cz+65, cx+65, y, cz+65, BlockTypes.MOSS_BLOCK);
                fill(es, cx+65, y, cz-65, cx+65, y, cz+65, BlockTypes.MOSS_BLOCK);
            }
            // Gateways
            for (int y = AY+1; y <= AY+4; y++) {
                fill(es, cx+20, y, cz-65, cx+24, y, cz-65, BlockTypes.AIR);
                fill(es, cx+20, y, cz+65, cx+24, y, cz+65, BlockTypes.AIR);
                fill(es, cx+65, y, cz-2,  cx+65, y, cz+2,  BlockTypes.AIR);
            }
            // Lily pads in water
            for (int i = 0; i < 25; i++) {
                int llx = cx - 62 + (i*13+7) % 70;
                int llz = cz - 62 + (i*9+5)  % 127;
                if (llx < cx+8) block(es, llx, AY, llz, BlockTypes.LILY_PAD);
            }
            // 4 MUD_BRICKS observation platforms
            int[][] plat = {{cx+40,cz-40},{cx+55,cz+35},{cx+30,cz+50},{cx+55,cz-10}};
            for (int[] p : plat) {
                fill(es, p[0]-2, AY+1, p[1]-2, p[0]+2, AY+1, p[1]+2, BlockTypes.MUD_BRICKS);
                fill(es, p[0]-2, AY+2, p[1]-2, p[0]+2, AY+3, p[1]-2, BlockTypes.MUD_BRICKS);
                fill(es, p[0]-2, AY+2, p[1]+2, p[0]+2, AY+3, p[1]+2, BlockTypes.MUD_BRICKS);
            }
            // Spawn pads
            int[][] sp = {{cx,cz-38},{cx,cz+38},{cx+45,cz-30},{cx+45,cz+30}};
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.PACKED_MUD);
        }
    }

    // =========================================================================
    // ARENA 5 — VOLCANIC ISLAND   cx=220, cz=-220   (130x130)
    // =========================================================================
    private void buildVolcanoArena() {
        final int cx = 220, cz = -220;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Podzol near volcano
            disk(es, cx, AY, cz, 22, BlockTypes.PODZOL);
            // Volcano cone using decreasing disks
            for (int y = 0; y <= 18; y++) {
                int r = 10 - (y / 2);
                if (r < 1) r = 1;
                disk(es, cx, AY+y, cz, r, BlockTypes.BASALT);
                disk(es, cx, AY+y, cz, Math.max(1, r-2), BlockTypes.POLISHED_BASALT);
            }
            // Magma rim and glowstone caldera
            ring(es, cx, AY+18, cz, 2, 4, BlockTypes.MAGMA_BLOCK);
            ring(es, cx, AY+19, cz, 1, 3, BlockTypes.MAGMA_BLOCK);
            fill(es, cx-2, AY+18, cz-2, cx+2, AY+18, cz+2, BlockTypes.GLOWSTONE);
            // 4 diagonal lava channels (magma block, 2-wide)
            int[][] chanDir = {{1,1},{1,-1},{-1,1},{-1,-1}};
            for (int[] d : chanDir) {
                for (int r = 12; r <= 55; r++) {
                    int cx2 = cx + d[0]*r, cz2 = cz + d[1]*r;
                    block(es, cx2,   AY, cz2,   BlockTypes.MAGMA_BLOCK);
                    block(es, cx2+d[0], AY, cz2+d[1], BlockTypes.MAGMA_BLOCK);
                }
            }
            // 4 elevated platforms (5x5 polished basalt at y=AY+6)
            int[][] platPos = {{cx-38,cz-38},{cx+38,cz-38},{cx-38,cz+38},{cx+38,cz+38}};
            for (int[] p : platPos) {
                fill(es, p[0]-2, AY, p[1]-2, p[0]+2, AY+6, p[1]+2, BlockTypes.BASALT);
                fill(es, p[0]-2, AY+6, p[1]-2, p[0]+2, AY+6, p[1]+2, BlockTypes.POLISHED_BASALT);
            }
            // 18 jungle trees in ring
            for (int i = 0; i < 18; i++) {
                double a = i * Math.PI * 2 / 18;
                int r = 35 + (i % 3) * 5;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 62 || Math.abs(tz-cz) > 62) continue;
                int th = 5 + (i % 4);
                column(es, tx, tz, AY+1, AY+th, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, AY+th, tz-2, tx+2, AY+th+2, tz+2, BlockTypes.JUNGLE_LEAVES);
            }
            // 8 dark oak trees in outer ring
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int tx = cx + (int)Math.round(55 * Math.cos(a));
                int tz = cz + (int)Math.round(55 * Math.sin(a));
                if (Math.abs(tx-cx) > 62 || Math.abs(tz-cz) > 62) continue;
                column(es, tx, tz, AY+1, AY+5, BlockTypes.DARK_OAK_LOG);
                fill(es, tx-1, AY+5, tz-1, tx+1, AY+6, tz+1, BlockTypes.DARK_OAK_LEAVES);
            }
            // Perimeter wall: blackstone 5 tall, basalt columns every 8
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-63, y, cz-63, cx+63, y, cz-63, BlockTypes.BLACKSTONE);
                fill(es, cx-63, y, cz+63, cx+63, y, cz+63, BlockTypes.BLACKSTONE);
                fill(es, cx-63, y, cz-63, cx-63, y, cz+63, BlockTypes.BLACKSTONE);
                fill(es, cx+63, y, cz-63, cx+63, y, cz+63, BlockTypes.BLACKSTONE);
            }
            for (int wx = cx-56; wx <= cx+56; wx += 8) {
                fill(es, wx, AY+1, cz-63, wx, AY+7, cz-63, BlockTypes.BASALT);
                fill(es, wx, AY+1, cz+63, wx, AY+7, cz+63, BlockTypes.BASALT);
            }
            for (int wz = cz-56; wz <= cz+56; wz += 8) {
                fill(es, cx-63, AY+1, wz, cx-63, AY+7, wz, BlockTypes.BASALT);
                fill(es, cx+63, AY+1, wz, cx+63, AY+7, wz, BlockTypes.BASALT);
            }
            // 4 gateways
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-2, y, cz-63, cx+2, y, cz-63, BlockTypes.AIR);
                fill(es, cx-2, y, cz+63, cx+2, y, cz+63, BlockTypes.AIR);
                fill(es, cx-63, y, cz-2, cx-63, y, cz+2, BlockTypes.AIR);
                fill(es, cx+63, y, cz-2, cx+63, y, cz+2, BlockTypes.AIR);
            }
            // Soul sand ring around volcano
            ring(es, cx, AY, cz, 13, 16, BlockTypes.SOUL_SAND);
            // 12 bamboo clusters in outer ring
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                int bx2 = cx + (int)Math.round(48 * Math.cos(a));
                int bz3 = cz + (int)Math.round(48 * Math.sin(a));
                if (Math.abs(bx2-cx) > 62 || Math.abs(bz3-cz) > 62) continue;
                int h = 4 + (i % 3);
                column(es, bx2, bz3, AY+1, AY+h, BlockTypes.BAMBOO);
                column(es, bx2+1, bz3, AY+1, AY+h-1, BlockTypes.BAMBOO);
            }
            // Spawn pads
            int[][] sp = {{cx,cz-48},{cx,cz+48},{cx-48,cz},{cx+48,cz}};
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.POLISHED_BASALT);
        }
    }

    // =========================================================================
    // ARENA 6 — CORAL REEF   cx=-220, cz=-220   (130x130)
    // =========================================================================
    private void buildCoralArena() {
        final int cx = -220, cz = -220;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            // Sand/stone base
            fill(es, cx-65, 62, cz-65, cx+65, 63, cz+65, BlockTypes.SAND);
            // Water fills the arena floor
            fill(es, cx-55, AY, cz-55, cx+55, AY+1, cz+55, BlockTypes.WATER);
            // Dry perimeter path (10-wide ring)
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.SAND);
            fill(es, cx-55, AY, cz-55, cx+55, AY, cz+55, BlockTypes.WATER);
            // Perimeter stone wall 6 tall
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-63, y, cz-63, cx+63, y, cz-63, BlockTypes.STONE_BRICKS);
                fill(es, cx-63, y, cz+63, cx+63, y, cz+63, BlockTypes.STONE_BRICKS);
                fill(es, cx-63, y, cz-63, cx-63, y, cz+63, BlockTypes.STONE_BRICKS);
                fill(es, cx+63, y, cz-63, cx+63, y, cz+63, BlockTypes.STONE_BRICKS);
            }
            // 4 gateways
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-2, y, cz-63, cx+2, y, cz-63, BlockTypes.AIR);
                fill(es, cx-2, y, cz+63, cx+2, y, cz+63, BlockTypes.AIR);
                fill(es, cx-63, y, cz-2, cx-63, y, cz+2, BlockTypes.AIR);
                fill(es, cx+63, y, cz-2, cx+63, y, cz+2, BlockTypes.AIR);
            }
            // 4 coral islands (15x10 sand mound at y=65 with grass top)
            int[][] islands = {{cx-30,cz-30},{cx+30,cz-30},{cx-30,cz+30},{cx+30,cz+30}};
            for (int[] isl : islands) {
                fill(es, isl[0]-7, AY, isl[1]-5, isl[0]+7, AY+1, isl[1]+5, BlockTypes.SAND);
                fill(es, isl[0]-5, AY+2, isl[1]-3, isl[0]+5, AY+2, isl[1]+3, BlockTypes.GRASS_BLOCK);
                // 2 palm trees on each island
                for (int dx : new int[]{-3, 3}) {
                    column(es, isl[0]+dx, isl[1], AY+3, AY+9, BlockTypes.JUNGLE_LOG);
                    fill(es, isl[0]+dx-1, AY+9, isl[1]-1, isl[0]+dx+1, AY+10, isl[1]+1, BlockTypes.JUNGLE_LEAVES);
                }
            }
            // Coral scatter at y=62 (ocean floor)
            com.sk89q.worldedit.world.block.BlockType[] corals = {
                BlockTypes.BRAIN_CORAL_BLOCK, BlockTypes.TUBE_CORAL_BLOCK,
                BlockTypes.FIRE_CORAL_BLOCK, BlockTypes.HORN_CORAL_BLOCK, BlockTypes.BUBBLE_CORAL_BLOCK
            };
            for (int i = 0; i < 40; i++) {
                int rx = cx - 53 + (i*13+7) % 107;
                int rz = cz - 53 + (i*11+3) % 107;
                block(es, rx, 62, rz, corals[i % corals.length]);
            }
            // Central prismarine structure
            fill(es, cx-5, AY+2, cz-5, cx+5, AY+2, cz+5, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx-5, AY+1, cz-5, cx+5, AY+1, cz+5, BlockTypes.DARK_PRISMARINE);
            for (int[] c2 : new int[][]{{cx-5,cz-5},{cx+5,cz-5},{cx-5,cz+5},{cx+5,cz+5}}) {
                column(es, c2[0], c2[1], AY+3, AY+10, BlockTypes.SEA_LANTERN);
            }
            fill(es, cx-5, AY+10, cz-5, cx+5, AY+10, cz+5, BlockTypes.DARK_PRISMARINE);
            // 8 sea lantern pillars in water
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int px2 = cx + (int)Math.round(40 * Math.cos(a));
                int pz2 = cz + (int)Math.round(40 * Math.sin(a));
                fill(es, px2, 62, pz2, px2+1, AY+5, pz2+1, BlockTypes.SEA_LANTERN);
            }
            // Lily pads
            for (int i = 0; i < 30; i++) {
                int rx = cx - 53 + (i*17+5) % 107;
                int rz = cz - 53 + (i*7+11) % 107;
                block(es, rx, AY+1, rz, BlockTypes.LILY_PAD);
            }
            // 4 oak log mast/deck structures (shipwreck style)
            int[][] decks = {{cx-45,cz-10},{cx+43,cz+10},{cx-10,cz+44},{cx+10,cz-44}};
            for (int[] d : decks) {
                fill(es, d[0]-2, AY+1, d[1]-1, d[0]+2, AY+1, d[1]+1, BlockTypes.OAK_PLANKS);
                column(es, d[0], d[1], AY+2, AY+9, BlockTypes.OAK_LOG);
            }
            // Stepping stone platforms at midpoints
            int[][] steps = {{cx,cz-35},{cx,cz+35},{cx-35,cz},{cx+35,cz}};
            for (int[] s : steps)
                fill(es, s[0]-3, AY, s[1]-3, s[0]+3, AY, s[1]+3, BlockTypes.PRISMARINE);
            // Spawn pads on islands
            for (int[] isl : islands)
                fill(es, isl[0]-1, AY+2, isl[1]-1, isl[0]+1, AY+2, isl[1]+1, BlockTypes.PRISMARINE);
        }
    }

    // =========================================================================
    // ARENA 7 — OVERGROWN RUINS   cx=220, cz=220   (130x130)
    // =========================================================================
    private void buildRuinsArena() {
        final int cx = 220, cz = 220;
        try (EditSession es = newSession()) {
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.MOSS_BLOCK);
            // Mossy stone bricks patches
            for (int i = 0; i < 20; i++) {
                int rx = cx - 60 + (i*13+7) % 121;
                int rz = cz - 60 + (i*11+3) % 121;
                fill(es, rx-2, AY, rz-2, rx+2, AY, rz+2, BlockTypes.MOSSY_STONE_BRICKS);
            }
            // Ancient road N/S (5-wide coarse dirt)
            fill(es, cx-2, AY, cz-65, cx+2, AY, cz+65, BlockTypes.COARSE_DIRT);
            // Central ruined temple 25x25 footprint
            fillHollow(es, cx-12, AY+1, cz-12, cx+12, AY+8, cz+12, BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
            // Cracked upper section
            fill(es, cx-12, AY+7, cz-12, cx+12, AY+9, cz+12, BlockTypes.CRACKED_STONE_BRICKS);
            // Chiseled accents
            for (int y = AY+1; y <= AY+7; y += 3) {
                for (int wx = cx-12; wx <= cx+12; wx += 4) {
                    block(es, wx, y, cz-12, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, wx, y, cz+12, BlockTypes.CHISELED_STONE_BRICKS);
                }
                for (int wz = cz-12; wz <= cz+12; wz += 4) {
                    block(es, cx-12, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, cx+12, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                }
            }
            // 4 large gateway holes 5x5
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-2, y, cz-12, cx+2, y, cz-12, BlockTypes.AIR);
                fill(es, cx-2, y, cz+12, cx+2, y, cz+12, BlockTypes.AIR);
                fill(es, cx-12, y, cz-2, cx-12, y, cz+2, BlockTypes.AIR);
                fill(es, cx+12, y, cz-2, cx+12, y, cz+2, BlockTypes.AIR);
            }
            // Temple floor
            fill(es, cx-11, AY, cz-11, cx+11, AY, cz+11, BlockTypes.STONE_BRICKS);
            // Shroomlight grid inside temple
            for (int x = cx-8; x <= cx+8; x += 5)
                for (int z = cz-8; z <= cz+8; z += 5)
                    block(es, x, AY, z, BlockTypes.SHROOMLIGHT);
            // 4 partially-collapsed towers (5x5, 10-14 tall, missing walls)
            int[][] towerPos = {{cx-45,cz-45},{cx+43,cz-45},{cx-45,cz+43},{cx+43,cz+43}};
            int[] towerH = {12, 10, 14, 11};
            for (int i = 0; i < 4; i++) {
                int tx = towerPos[i][0], tz = towerPos[i][1];
                int th = towerH[i];
                fillHollow(es, tx-2, AY+1, tz-2, tx+2, AY+th, tz+2,
                        BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
                // Missing wall section
                for (int y = AY+1; y <= AY+th/2; y++)
                    fill(es, tx+2, y, tz-1, tx+2, y, tz+1, BlockTypes.AIR);
                fill(es, tx-2, AY+th+1, tz-2, tx+2, AY+th+1, tz+2, BlockTypes.MOSS_BLOCK);
            }
            // 12 large jungle trees
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                int r = 28 + (i % 4) * 6;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 62 || Math.abs(tz-cz) > 62) continue;
                int h = 8 + (i % 5);
                column(es, tx, tz, AY+1, AY+h, BlockTypes.JUNGLE_LOG);
                column(es, tx+1, tz, AY+1, AY+h, BlockTypes.JUNGLE_LOG);
                fill(es, tx-3, AY+h-1, tz-3, tx+4, AY+h+3, tz+4, BlockTypes.JUNGLE_LEAVES);
            }
            // 20 medium oak trees
            for (int i = 0; i < 20; i++) {
                double a = (i + 0.5) * Math.PI / 10;
                int r = 45 + (i % 3) * 7;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 62 || Math.abs(tz-cz) > 62) continue;
                column(es, tx, tz, AY+1, AY+5, BlockTypes.OAK_LOG);
                fill(es, tx-2, AY+5, tz-2, tx+2, AY+7, tz+2, BlockTypes.OAK_LEAVES);
            }
            // 6 sunken pits with iron bars grate
            int[] pitX = {-30, 30,-50, 50,-20, 20};
            int[] pitZ = {-30,-30, 10,-10, 50, 50};
            for (int i = 0; i < 6; i++) {
                int px2 = cx+pitX[i], pz2 = cz+pitZ[i];
                fill(es, px2-1, AY-2, pz2-1, px2+1, AY-1, pz2+1, BlockTypes.STONE);
                fill(es, px2-1, AY,   pz2-1, px2+1, AY,   pz2+1, BlockTypes.IRON_BARS);
            }
            // 8 rubble piles
            int[] rX = {-55,55,-40,40,-55,55,-25,25};
            int[] rZ = {-55,-55,55,55,0,0,-55,55};
            for (int i = 0; i < 8; i++)
                fill(es, cx+rX[i]-1, AY+1, cz+rZ[i]-1, cx+rX[i]+1, AY+2, cz+rZ[i]+1, BlockTypes.CRACKED_STONE_BRICKS);
            // Ancient well
            fill(es, cx+25, AY+1, cz-25, cx+29, AY+4, cz-21, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+26, AY+1, cz-24, cx+28, AY, cz-22, BlockTypes.STONE);
            fill(es, cx+26, AY+1, cz-24, cx+28, AY+1, cz-22, BlockTypes.WATER);
            // Outer perimeter: mossy cobblestone 4 tall, broken every 8
            for (int y = AY+1; y <= AY+4; y++) {
                for (int wx = cx-63; wx <= cx+63; wx++) {
                    if ((wx - (cx-63)) % 8 >= 2) {
                        block(es, wx, y, cz-63, BlockTypes.MOSSY_COBBLESTONE);
                        block(es, wx, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
                    }
                }
                for (int wz = cz-63; wz <= cz+63; wz++) {
                    if ((wz - (cz-63)) % 8 >= 2) {
                        block(es, cx-63, y, wz, BlockTypes.MOSSY_COBBLESTONE);
                        block(es, cx+63, y, wz, BlockTypes.MOSSY_COBBLESTONE);
                    }
                }
            }
            // 8 fallen logs
            int[] flX = {-55,-40,25,45,-30,10,-50,40};
            int[] flZ = {-30,50,-55,20,-5,58,30,-40};
            for (int i = 0; i < 8; i++)
                fill(es, cx+flX[i], AY+1, cz+flZ[i], cx+flX[i]+(5+i%4), AY+1, cz+flZ[i], BlockTypes.JUNGLE_LOG);
            // Spawn pads
            int[][] sp = {{cx,cz-48},{cx,cz+48},{cx-48,cz},{cx+48,cz}};
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.MOSSY_STONE_BRICKS);
        }
    }

    // =========================================================================
    // ARENA 8 — TROPICAL CLIFFSIDE   cx=-220, cz=220   (130x130)
    // =========================================================================
    private void buildCliffsideArena() {
        final int cx = -220, cz = 220;
        try (EditSession es = newSession()) {
            // Lower tier (west, cx-65..cx-15, y=64)
            fill(es, cx-65, 59, cz-65, cx-15, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx-15, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Middle tier (cx-14..cx+14, y=72)
            fill(es, cx-14, 59, cz-65, cx+14, 71, cz+65, BlockTypes.STONE);
            fill(es, cx-14, AY+8, cz-65, cx+14, AY+8, cz+65, BlockTypes.GRASS_BLOCK);
            // Upper tier (east, cx+15..cx+65, y=80)
            fill(es, cx+15, 59, cz-65, cx+65, 79, cz+65, BlockTypes.STONE);
            fill(es, cx+15, AY+16, cz-65, cx+65, AY+16, cz+65, BlockTypes.GRASS_BLOCK);
            // Cliff faces (stone faces)
            fill(es, cx-15, AY+1, cz-65, cx-15, AY+8, cz+65, BlockTypes.STONE);
            fill(es, cx+15, AY+9, cz-65, cx+15, AY+16, cz+65, BlockTypes.STONE);
            // 3 stone staircases (1-wide steps) connecting tiers at z offsets
            for (int z : new int[]{cz-30, cz, cz+30}) {
                // Lower to middle (y=64..72, x=cx-16..cx-14)
                for (int s = 0; s <= 8; s++)
                    block(es, cx-15-s, AY+s, z, BlockTypes.STONE_BRICKS);
                // Middle to upper (y=72..80, x=cx+14..cx+16)
                for (int s = 0; s <= 8; s++)
                    block(es, cx+14+s, AY+8+s, z, BlockTypes.STONE_BRICKS);
            }
            // 8 jungle trees on lower tier
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int tx = cx - 40 + (int)Math.round(20 * Math.cos(a));
                int tz2 = cz + (int)Math.round(30 * Math.sin(a));
                tx = Math.max(cx-63, Math.min(cx-16, tx));
                column(es, tx, tz2, AY+1, AY+6, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, AY+6, tz2-2, tx+2, AY+8, tz2+2, BlockTypes.JUNGLE_LEAVES);
            }
            // 6 acacia trees on middle tier
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                int tx = cx + (int)Math.round(8 * Math.cos(a));
                int tz2 = cz + (int)Math.round(28 * Math.sin(a));
                tx = Math.max(cx-13, Math.min(cx+13, tx));
                column(es, tx, tz2, AY+9, AY+13, BlockTypes.ACACIA_LOG);
                fill(es, tx-2, AY+13, tz2-2, tx+2, AY+14, tz2+2, BlockTypes.ACACIA_LEAVES);
                fill(es, tx-1, AY+15, tz2-1, tx+1, AY+15, tz2+1, BlockTypes.ACACIA_LEAVES);
            }
            // 4 dark oak trees on upper tier
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2;
                int tx = cx + 38 + (int)Math.round(15 * Math.cos(a));
                int tz2 = cz + (int)Math.round(25 * Math.sin(a));
                tx = Math.max(cx+16, Math.min(cx+63, tx));
                column(es, tx, tz2, AY+17, AY+22, BlockTypes.DARK_OAK_LOG);
                fill(es, tx-2, AY+22, tz2-2, tx+2, AY+24, tz2+2, BlockTypes.DARK_OAK_LEAVES);
            }
            // 2 waterfalls
            for (int y = AY+8; y >= AY+1; y--)
                block(es, cx-14, y, cz, BlockTypes.WATER);
            for (int y = AY+16; y >= AY+9; y--)
                block(es, cx+15, y, cz, BlockTypes.WATER);
            // Pools at base of waterfalls
            fill(es, cx-16, AY-1, cz-2, cx-12, AY-1, cz+2, BlockTypes.STONE);
            fill(es, cx-16, AY,   cz-2, cx-12, AY,   cz+2, BlockTypes.WATER);
            fill(es, cx+13, AY+7, cz-2, cx+17, AY+7, cz+2, BlockTypes.STONE);
            fill(es, cx+13, AY+8, cz-2, cx+17, AY+8, cz+2, BlockTypes.WATER);
            // N/S walls on each tier
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-65, y, cz-63, cx-15, y, cz-63, BlockTypes.STONE_BRICKS);
                fill(es, cx-65, y, cz+63, cx-15, y, cz+63, BlockTypes.STONE_BRICKS);
                fill(es, cx+15, y+8, cz-63, cx+65, y+8, cz-63, BlockTypes.STONE_BRICKS);
                fill(es, cx+15, y+8, cz+63, cx+65, y+8, cz+63, BlockTypes.STONE_BRICKS);
            }
            // 4 gateways
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-45, y, cz-63, cx-41, y, cz-63, BlockTypes.AIR);
                fill(es, cx-45, y, cz+63, cx-41, y, cz+63, BlockTypes.AIR);
                fill(es, cx+35, y+8, cz-63, cx+39, y+8, cz-63, BlockTypes.AIR);
                fill(es, cx+35, y+8, cz+63, cx+39, y+8, cz+63, BlockTypes.AIR);
            }
            // Lantern posts per tier
            for (int i = 0; i < 6; i++) {
                int lz = cz - 50 + i * 20;
                column(es, cx-50, lz, AY+1, AY+3, BlockTypes.OAK_FENCE);
                block(es, cx-50, AY+4, lz, BlockTypes.LANTERN);
                column(es, cx, lz, AY+9, AY+11, BlockTypes.OAK_FENCE);
                block(es, cx, AY+12, lz, BlockTypes.LANTERN);
                column(es, cx+45, lz, AY+17, AY+19, BlockTypes.OAK_FENCE);
                block(es, cx+45, AY+20, lz, BlockTypes.LANTERN);
            }
            // Spawn pads: 2 per tier
            fill(es, cx-50-1, AY, cz-1, cx-50+1, AY, cz+1, BlockTypes.STONE_BRICKS);
            fill(es, cx-35-1, AY, cz-1, cx-35+1, AY, cz+1, BlockTypes.STONE_BRICKS);
            fill(es, cx-1, AY+8, cz-10-1, cx+1, AY+8, cz-10+1, BlockTypes.STONE_BRICKS);
            fill(es, cx-1, AY+8, cz+10-1, cx+1, AY+8, cz+10+1, BlockTypes.STONE_BRICKS);
            fill(es, cx+40-1, AY+16, cz-1, cx+40+1, AY+16, cz+1, BlockTypes.STONE_BRICKS);
            fill(es, cx+55-1, AY+16, cz-1, cx+55+1, AY+16, cz+1, BlockTypes.STONE_BRICKS);
        }
    }

    // =========================================================================
    // FFA 1 — JUNGLE FORTRESS   cx=500, cz=0   (150x150)
    // =========================================================================
    private void buildJungleFortressFFA() {
        final int cx = 500, cz = 0;
        try (EditSession es = newSession()) {
            fill(es, cx-75, 59, cz-75, cx+75, 63, cz+75, BlockTypes.STONE);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.GRASS_BLOCK);
            // Coarse dirt paths grid
            fill(es, cx-2, AY, cz-75, cx+2, AY, cz+75, BlockTypes.COARSE_DIRT);
            fill(es, cx-75, AY, cz-2, cx+75, AY, cz+2, BlockTypes.COARSE_DIRT);
            // Outer perimeter wall: mossy stone bricks 8 tall
            for (int y = AY+1; y <= AY+8; y++) {
                fill(es, cx-73, y, cz-73, cx+73, y, cz-73, BlockTypes.MOSSY_STONE_BRICKS);
                fill(es, cx-73, y, cz+73, cx+73, y, cz+73, BlockTypes.MOSSY_STONE_BRICKS);
                fill(es, cx-73, y, cz-73, cx-73, y, cz+73, BlockTypes.MOSSY_STONE_BRICKS);
                fill(es, cx+73, y, cz-73, cx+73, y, cz+73, BlockTypes.MOSSY_STONE_BRICKS);
            }
            // Jungle log buttresses every 12 blocks
            for (int wx = cx-61; wx <= cx+61; wx += 12) {
                fill(es, wx-1, AY+1, cz-73, wx+1, AY+10, cz-73, BlockTypes.JUNGLE_LOG);
                fill(es, wx-1, AY+1, cz+73, wx+1, AY+10, cz+73, BlockTypes.JUNGLE_LOG);
            }
            for (int wz = cz-61; wz <= cz+61; wz += 12) {
                fill(es, cx-73, AY+1, wz-1, cx-73, AY+10, wz+1, BlockTypes.JUNGLE_LOG);
                fill(es, cx+73, AY+1, wz-1, cx+73, AY+10, wz+1, BlockTypes.JUNGLE_LOG);
            }
            // 4 gatehouses (15x15, 20 tall, hollow) at corners
            int[][] ghPos = {{cx-60,cz-60},{cx+58,cz-60},{cx-60,cz+58},{cx+58,cz+58}};
            for (int[] g : ghPos) {
                fillHollow(es, g[0]-7, AY+1, g[1]-7, g[0]+7, AY+20, g[1]+7,
                        BlockTypes.STONE_BRICKS, BlockTypes.AIR);
                block(es, g[0], AY+21, g[1], BlockTypes.BEACON);
                fill(es, g[0]-1, AY+21, g[1]-1, g[0]+1, AY+21, g[1]+1, BlockTypes.IRON_BLOCK);
            }
            // 4 gateways in perimeter wall (7-wide, 10-tall)
            for (int y = AY+1; y <= AY+10; y++) {
                fill(es, cx-3, y, cz-73, cx+3, y, cz-73, BlockTypes.AIR);
                fill(es, cx-3, y, cz+73, cx+3, y, cz+73, BlockTypes.AIR);
                fill(es, cx-73, y, cz-3, cx-73, y, cz+3, BlockTypes.AIR);
                fill(es, cx+73, y, cz-3, cx+73, y, cz+3, BlockTypes.AIR);
            }
            // Inner courtyard wall (cx±40)
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-40, y, cz-40, cx+40, y, cz-40, BlockTypes.MOSSY_STONE_BRICKS);
                fill(es, cx-40, y, cz+40, cx+40, y, cz+40, BlockTypes.MOSSY_STONE_BRICKS);
                fill(es, cx-40, y, cz-40, cx-40, y, cz+40, BlockTypes.MOSSY_STONE_BRICKS);
                fill(es, cx+40, y, cz-40, cx+40, y, cz+40, BlockTypes.MOSSY_STONE_BRICKS);
            }
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-3, y, cz-40, cx+3, y, cz-40, BlockTypes.AIR);
                fill(es, cx-3, y, cz+40, cx+3, y, cz+40, BlockTypes.AIR);
                fill(es, cx-40, y, cz-3, cx-40, y, cz+3, BlockTypes.AIR);
                fill(es, cx+40, y, cz-3, cx+40, y, cz+3, BlockTypes.AIR);
            }
            // Inner courtyard floor
            fill(es, cx-39, AY, cz-39, cx+39, AY, cz+39, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-39, AY, cz-39, cx+39, AY, cz-38, BlockTypes.JUNGLE_LOG);
            fill(es, cx-39, AY, cz+38, cx+39, AY, cz+39, BlockTypes.JUNGLE_LOG);
            // Central keep 20x20, 16 tall
            fillHollow(es, cx-10, AY+1, cz-10, cx+10, AY+16, cz+10,
                    BlockTypes.STONE_BRICKS, BlockTypes.AIR);
            // 4 IRON_BARS arrow slits in walls
            for (int y = AY+4; y <= AY+12; y += 4) {
                for (int wx = cx-73; wx <= cx+73; wx += 8) {
                    block(es, wx, y, cz-73, BlockTypes.IRON_BARS);
                    block(es, wx, y, cz+73, BlockTypes.IRON_BARS);
                }
                for (int wz = cz-73; wz <= cz+73; wz += 8) {
                    block(es, cx-73, y, wz, BlockTypes.IRON_BARS);
                    block(es, cx+73, y, wz, BlockTypes.IRON_BARS);
                }
            }
            // 4 elevated walkways (3-wide OAK_PLANKS at y=AY+8) connecting gatehouses
            fill(es, cx-60, AY+8, cz-73, cx+60, AY+8, cz-73, BlockTypes.OAK_PLANKS);
            fill(es, cx-60, AY+8, cz+73, cx+60, AY+8, cz+73, BlockTypes.OAK_PLANKS);
            // 30 jungle trees inside fortress
            for (int i = 0; i < 30; i++) {
                double a = i * Math.PI * 2 / 30;
                int r = 18 + (i % 5) * 9;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 70 || Math.abs(tz-cz) > 70) continue;
                if (Math.abs(tx-cx) <= 10 && Math.abs(tz-cz) <= 10) continue;
                int h = 5 + (i % 6);
                column(es, tx, tz, AY+1, AY+h, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, AY+h, tz-2, tx+2, AY+h+2, tz+2, BlockTypes.JUNGLE_LEAVES);
            }
            // 24 lantern posts
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                int r = 30 + (i % 3) * 15;
                int lx = cx + (int)Math.round(r * Math.cos(a));
                int lz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(lx-cx) > 71 || Math.abs(lz-cz) > 71) continue;
                column(es, lx, lz, AY+1, AY+3, BlockTypes.OAK_FENCE);
                block(es, lx, AY+4, lz, BlockTypes.LANTERN);
            }
            // 8 spawn pads around inner compound
            int[][] sp = {
                {cx,cz-50},{cx,cz+50},{cx-50,cz},{cx+50,cz},
                {cx-28,cz-28},{cx+28,cz-28},{cx-28,cz+28},{cx+28,cz+28}
            };
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.MOSSY_STONE_BRICKS);
        }
    }

    // =========================================================================
    // FFA 2 — TROPICAL ISLAND   cx=-500, cz=0   (150x150)
    // =========================================================================
    private void buildIslandFFA() {
        final int cx = -500, cz = 0;
        try (EditSession es = newSession()) {
            fill(es, cx-75, 59, cz-75, cx+75, 63, cz+75, BlockTypes.STONE);
            // Ocean moat (outer 20 ring): water y=60..64
            fill(es, cx-75, 59, cz-75, cx+75, 63, cz+75, BlockTypes.SAND);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.WATER);
            // Island interior (cx±55)
            fill(es, cx-55, 62, cz-55, cx+55, 63, cz+55, BlockTypes.SANDSTONE);
            fill(es, cx-55, AY, cz-55, cx+55, AY, cz+55, BlockTypes.SAND);
            // 4 large grass zones
            fill(es, cx-40, AY+1, cz-40, cx-10, AY+1, cz-10, BlockTypes.GRASS_BLOCK);
            fill(es, cx+10, AY+1, cz-40, cx+40, AY+1, cz-10, BlockTypes.GRASS_BLOCK);
            fill(es, cx-40, AY+1, cz+10, cx-10, AY+1, cz+40, BlockTypes.GRASS_BLOCK);
            fill(es, cx+10, AY+1, cz+10, cx+40, AY+1, cz+40, BlockTypes.GRASS_BLOCK);
            // Central arena sandstone 30x30 at y=65
            fill(es, cx-15, AY+1, cz-15, cx+15, AY+1, cz+15, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-15, AY+1, cz-15, cx+15, AY+1, cz-14, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx-15, AY+1, cz+14, cx+15, AY+1, cz+15, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx-15, AY+1, cz-14, cx-14, AY+1, cz+14, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx+14, AY+1, cz-14, cx+15, AY+1, cz+14, BlockTypes.CHISELED_SANDSTONE);
            // CUT_SANDSTONE stripes
            for (int x = cx-13; x <= cx+13; x += 6)
                fill(es, x, AY+1, cz-13, x+1, AY+1, cz+13, BlockTypes.CUT_SANDSTONE);
            // Sandstone walls around centre 6 tall
            for (int y = AY+2; y <= AY+7; y++) {
                fill(es, cx-15, y, cz-15, cx+15, y, cz-15, BlockTypes.SANDSTONE);
                fill(es, cx-15, y, cz+15, cx+15, y, cz+15, BlockTypes.SANDSTONE);
                fill(es, cx-15, y, cz-15, cx-15, y, cz+15, BlockTypes.SANDSTONE);
                fill(es, cx+15, y, cz-15, cx+15, y, cz+15, BlockTypes.SANDSTONE);
            }
            // Sandstone slab merlons on wall top
            for (int wx = cx-14; wx <= cx+14; wx += 2) {
                block(es, wx, AY+8, cz-15, BlockTypes.SANDSTONE);
                block(es, wx, AY+8, cz+15, BlockTypes.SANDSTONE);
            }
            // 6 gateways
            for (int y = AY+2; y <= AY+7; y++) {
                fill(es, cx-3, y, cz-15, cx+3, y, cz-15, BlockTypes.AIR);
                fill(es, cx-3, y, cz+15, cx+3, y, cz+15, BlockTypes.AIR);
                fill(es, cx-15, y, cz-3, cx-15, y, cz+3, BlockTypes.AIR);
                fill(es, cx+15, y, cz-3, cx+15, y, cz+3, BlockTypes.AIR);
            }
            // 8 pillars at r=20
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int px2 = cx + (int)Math.round(20 * Math.cos(a));
                int pz2 = cz + (int)Math.round(20 * Math.sin(a));
                fill(es, px2, AY+1, pz2, px2+1, AY+10, pz2+1, BlockTypes.SMOOTH_SANDSTONE);
                block(es, px2, AY+11, pz2, BlockTypes.GLOWSTONE);
            }
            // 24 palm trees on island
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                int r = 28 + (i % 4) * 7;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 53 || Math.abs(tz-cz) > 53) continue;
                column(es, tx, tz, AY+1, AY+8, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, AY+8, tz-2, tx+2, AY+9, tz+2, BlockTypes.JUNGLE_LEAVES);
                fill(es, tx-1, AY+10, tz-1, tx+1, AY+10, tz+1, BlockTypes.JUNGLE_LEAVES);
            }
            // 4 wooden pier docks into ocean
            int[][] dockDir = {{cx,cz-55},{cx,cz+55},{cx-55,cz},{cx+55,cz}};
            int[][] dockExt = {{0,-15},{0,15},{-15,0},{15,0}};
            for (int i = 0; i < 4; i++) {
                int dx = dockExt[i][0], dz2 = dockExt[i][1];
                int startX = dockDir[i][0], startZ = dockDir[i][1];
                for (int step = 0; step <= 15; step++) {
                    int x = startX + dx/15*step, z = startZ + dz2/15*step;
                    fill(es, x-1, AY+1, z-1, x+1, AY+1, z+1, BlockTypes.MANGROVE_PLANKS);
                    column(es, x-1, z-1, 60, AY, BlockTypes.MANGROVE_LOG);
                }
            }
            // Inland lagoon
            fill(es, cx-8, AY-1, cz-45, cx+8, AY-1, cz-35, BlockTypes.STONE);
            fill(es, cx-8, AY,   cz-45, cx+8, AY,   cz-35, BlockTypes.WATER);
            // Dead bush/grass scatter
            for (int i = 0; i < 40; i++) {
                int sx = cx - 53 + (i*13+7) % 107;
                int sz = cz - 53 + (i*11+3) % 107;
                block(es, sx, AY+1, sz, (i%3==0) ? BlockTypes.DEAD_BUSH : BlockTypes.SHORT_GRASS);
            }
            // 8 spawn pads
            int[][] sp = {
                {cx,cz-40},{cx,cz+40},{cx-40,cz},{cx+40,cz},
                {cx-28,cz-28},{cx+28,cz-28},{cx-28,cz+28},{cx+28,cz+28}
            };
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.SANDSTONE);
        }
    }

    // =========================================================================
    // FFA 3 — STEP PYRAMID   cx=0, cz=500   (150x150)
    // =========================================================================
    private void buildPyramidFFA() {
        final int cx = 0, cz = 500;
        try (EditSession es = newSession()) {
            fill(es, cx-75, 59, cz-75, cx+75, 63, cz+75, BlockTypes.STONE);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.GRASS_BLOCK);
            // Podzol near pyramid
            disk(es, cx, AY, cz, 30, BlockTypes.PODZOL);
            // Moss outer ring
            for (int i = 0; i < 30; i++) {
                int rx = cx - 70 + (i*13+7) % 141;
                int rz = cz - 70 + (i*11+3) % 141;
                if (Math.abs(rx-cx) > 30 || Math.abs(rz-cz) > 30)
                    fill(es, rx-2, AY, rz-2, rx+2, AY, rz+2, BlockTypes.MOSS_BLOCK);
            }
            // 6-tier pyramid (centred at cx,cz)
            com.sk89q.worldedit.world.block.BlockType[] tierMats = {
                BlockTypes.SANDSTONE, BlockTypes.SANDSTONE, BlockTypes.STONE_BRICKS,
                BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.CHISELED_STONE_BRICKS, BlockTypes.SMOOTH_SANDSTONE
            };
            int[] tierSizes = {30, 25, 20, 15, 10, 5};
            int baseY = AY;
            for (int tier = 0; tier < 6; tier++) {
                int s = tierSizes[tier];
                fill(es, cx-s, baseY, cz-s, cx+s, baseY+2, cz+s, tierMats[tier]);
                baseY += 3;
            }
            block(es, cx, baseY, cz, BlockTypes.BEACON);
            fill(es, cx-1, baseY-1, cz-1, cx+1, baseY-1, cz+1, BlockTypes.IRON_BLOCK);
            block(es, cx, baseY+1, cz, BlockTypes.GLOWSTONE);
            // 4 staircase paths on faces
            for (int tier = 0; tier < 6; tier++) {
                int s = tierSizes[tier];
                int y = AY + tier * 3;
                // North face
                fill(es, cx-1, y, cz-s, cx+1, y+3, cz-s+3, BlockTypes.STONE_BRICKS);
            }
            // 40 jungle trees surrounding pyramid
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI / 20;
                int r = 38 + (i % 5) * 8;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 73 || Math.abs(tz-cz) > 73) continue;
                int h = 6 + (i % 6);
                boolean big = i % 5 == 0;
                if (big) {
                    fill(es, tx, AY+1, tz, tx+1, AY+h, tz+1, BlockTypes.JUNGLE_LOG);
                    fill(es, tx-3, AY+h-1, tz-3, tx+4, AY+h+3, tz+4, BlockTypes.JUNGLE_LEAVES);
                } else {
                    column(es, tx, tz, AY+1, AY+h, BlockTypes.JUNGLE_LOG);
                    fill(es, tx-2, AY+h, tz-2, tx+2, AY+h+2, tz+2, BlockTypes.JUNGLE_LEAVES);
                }
            }
            // 20 oak trees
            for (int i = 0; i < 20; i++) {
                double a = (i + 0.5) * Math.PI / 10;
                int r = 55 + (i % 3) * 6;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 73 || Math.abs(tz-cz) > 73) continue;
                column(es, tx, tz, AY+1, AY+5, BlockTypes.OAK_LOG);
                fill(es, tx-2, AY+5, tz-2, tx+2, AY+7, tz+2, BlockTypes.OAK_LEAVES);
            }
            // 4 temple entrance arches at pyramid base cardinal sides
            int[][] archP = {{cx,cz-30},{cx,cz+30},{cx-30,cz},{cx+30,cz}};
            for (int[] a : archP) {
                fillHollow(es, a[0]-3, AY+1, a[1]-1, a[0]+3, AY+6, a[1]+1,
                        BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
            }
            // 8 ruin clusters in jungle
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int rx = cx + (int)Math.round(60 * Math.cos(a));
                int rz = cz + (int)Math.round(60 * Math.sin(a));
                if (Math.abs(rx-cx) > 72 || Math.abs(rz-cz) > 72) continue;
                fill(es, rx-2, AY+1, rz-2, rx+2, AY+4, rz+2, BlockTypes.CRACKED_STONE_BRICKS);
            }
            // 2 water streams N/S from pyramid base
            for (int z = cz-62; z <= cz-31; z++)
                block(es, cx, AY+1, z, BlockTypes.WATER);
            for (int z = cz+31; z <= cz+62; z++)
                block(es, cx, AY+1, z, BlockTypes.WATER);
            // 8 spawn pads
            int[][] sp = {
                {cx,cz-55},{cx,cz+55},{cx-55,cz},{cx+55,cz},
                {cx-38,cz-38},{cx+38,cz-38},{cx-38,cz+38},{cx+38,cz+38}
            };
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.STONE_BRICKS);
        }
    }

    // =========================================================================
    // FFA 4 — MANGROVE BAY   cx=0, cz=-500   (150x150)
    // =========================================================================
    private void buildMangroveBayFFA() {
        final int cx = 0, cz = -500;
        try (EditSession es = newSession()) {
            fill(es, cx-75, 59, cz-75, cx+75, 63, cz+75, BlockTypes.STONE);
            // Water fills most of arena
            fill(es, cx-75, 61, cz-75, cx+75, 61, cz+75, BlockTypes.MUD);
            fill(es, cx-75, 62, cz-75, cx+75, AY, cz+75, BlockTypes.WATER);
            // 5 land masses 20x20 at y=64
            int[][] lands = {{cx,cz},{cx-40,cz-35},{cx+40,cz-35},{cx-40,cz+35},{cx+40,cz+35}};
            for (int[] l : lands) {
                fill(es, l[0]-10, 62, l[1]-10, l[0]+10, 63, l[1]+10, BlockTypes.MUD);
                fill(es, l[0]-10, AY, l[1]-10, l[0]+10, AY, l[1]+10, BlockTypes.GRASS_BLOCK);
                fill(es, l[0]-8, AY, l[1]-8, l[0]+8, AY, l[1]+8, BlockTypes.MUD_BRICKS);
                // Packed mud shore
                fill(es, l[0]-12, AY, l[1]-12, l[0]+12, AY+1, l[1]-10, BlockTypes.PACKED_MUD);
                fill(es, l[0]-12, AY, l[1]+10, l[0]+12, AY+1, l[1]+12, BlockTypes.PACKED_MUD);
                fill(es, l[0]-12, AY, l[1]-10, l[0]-10, AY+1, l[1]+10, BlockTypes.PACKED_MUD);
                fill(es, l[0]+10, AY, l[1]-10, l[0]+12, AY+1, l[1]+10, BlockTypes.PACKED_MUD);
            }
            // Dock network connecting land masses (5-wide planks)
            // Central to each corner land
            for (int i = 1; i < 5; i++) {
                int lx1 = lands[0][0], lz1 = lands[0][1];
                int lx2 = lands[i][0], lz2 = lands[i][1];
                int steps = 20;
                for (int s = 0; s <= steps; s++) {
                    int dx = lx1 + (lx2-lx1)*s/steps;
                    int dz2 = lz1 + (lz2-lz1)*s/steps;
                    fill(es, dx-2, AY+2, dz2-2, dx+2, AY+2, dz2+2, BlockTypes.MANGROVE_PLANKS);
                }
            }
            // Dock posts
            for (int i = 0; i < 50; i++) {
                int ppx = cx - 72 + (i*11+5) % 145;
                int ppz = cz - 72 + (i*7+3) % 145;
                // Only in water areas
                boolean onLand = false;
                for (int[] l : lands) {
                    if (Math.abs(ppx-l[0]) <= 12 && Math.abs(ppz-l[1]) <= 12) { onLand = true; break; }
                }
                if (!onLand) column(es, ppx, ppz, 61, AY+3, BlockTypes.MANGROVE_LOG);
            }
            // Mangrove fence railings on main docks
            for (int[] l : lands) {
                for (int dz2 = l[1]-2; dz2 <= l[1]+2; dz2++) {
                    block(es, l[0]-2, AY+3, dz2, BlockTypes.MANGROVE_FENCE);
                    block(es, l[0]+2, AY+3, dz2, BlockTypes.MANGROVE_FENCE);
                }
            }
            // 20 mangrove trees
            for (int i = 0; i < 20; i++) {
                double a = i * Math.PI / 10;
                int r = 20 + (i % 5) * 10;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 73 || Math.abs(tz-cz) > 73) continue;
                column(es, tx, tz, 62, AY+7, BlockTypes.MANGROVE_LOG);
                fill(es, tx-2, AY+7, tz-2, tx+2, AY+9, tz+2, BlockTypes.MANGROVE_LEAVES);
                fill(es, tx-1, AY+10, tz-1, tx+1, AY+10, tz+1, BlockTypes.MANGROVE_LEAVES);
            }
            // Central hub platform 20x20 at y=67
            fill(es, cx-10, AY+2, cz-10, cx+10, AY+3, cz+10, BlockTypes.MANGROVE_PLANKS);
            for (int[] c2 : new int[][]{{cx-9,cz-9},{cx+9,cz-9},{cx-9,cz+9},{cx+9,cz+9}})
                column(es, c2[0], c2[1], 62, AY+2, BlockTypes.MANGROVE_LOG);
            // 50 lily pads in water
            for (int i = 0; i < 50; i++) {
                int llx = cx - 72 + (i*13+7) % 145;
                int llz = cz - 72 + (i*9+5) % 145;
                boolean onLand = false;
                for (int[] l : lands) {
                    if (Math.abs(llx-l[0]) <= 12 && Math.abs(llz-l[1]) <= 12) { onLand = true; break; }
                }
                if (!onLand) block(es, llx, AY, llz, BlockTypes.LILY_PAD);
            }
            // SEA_LANTERN underwater grid every 10 blocks
            for (int x = cx-70; x <= cx+70; x += 10)
                for (int z = cz-70; z <= cz+70; z += 10)
                    block(es, x, 61, z, BlockTypes.SEA_LANTERN);
            // Lantern posts on docks
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                int r = 25 + (i%3)*15;
                int lx = cx + (int)Math.round(r * Math.cos(a));
                int lz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(lx-cx) > 72 || Math.abs(lz-cz) > 72) continue;
                column(es, lx, lz, AY+2, AY+4, BlockTypes.MANGROVE_FENCE);
                block(es, lx, AY+5, lz, BlockTypes.LANTERN);
            }
            // 8 spawn pads on land masses and central hub
            int[][] sp = {
                {cx,cz},{cx-40,cz-35},{cx+40,cz-35},{cx-40,cz+35},{cx+40,cz+35},
                {cx-5,cz-5},{cx+5,cz+5},{cx,cz+5}
            };
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.PACKED_MUD);
        }
    }

    // =========================================================================
    // FFA 5 — VOLCANO HIGHLANDS   cx=400, cz=400   (150x150)
    // =========================================================================
    private void buildVolcanoHighlandsFFA() {
        final int cx = 400, cz = 400;
        try (EditSession es = newSession()) {
            fill(es, cx-75, 59, cz-75, cx+75, 63, cz+75, BlockTypes.STONE);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.GRASS_BLOCK);
            // Basalt and podzol patches
            for (int i = 0; i < 20; i++) {
                int rx = cx - 70 + (i*13+7) % 141;
                int rz = cz - 70 + (i*11+3) % 141;
                fill(es, rx-2, AY, rz-2, rx+2, AY, rz+2, (i%2==0) ? BlockTypes.BASALT : BlockTypes.PODZOL);
            }
            // Mega volcano: 40-wide base, 30 tall, decreasing disks
            for (int y = 0; y <= 30; y++) {
                int r = 20 - (y * 20 / 30);
                if (r < 1) r = 1;
                disk(es, cx, AY+y, cz, r, BlockTypes.BASALT);
                disk(es, cx, AY+y, cz, Math.max(1, r-3), BlockTypes.POLISHED_BASALT);
                if (y > 20) disk(es, cx, AY+y, cz, Math.max(1, r-1), BlockTypes.BLACKSTONE);
            }
            // Magma rim + glowstone caldera
            ring(es, cx, AY+28, cz, 2, 5, BlockTypes.MAGMA_BLOCK);
            ring(es, cx, AY+29, cz, 1, 4, BlockTypes.MAGMA_BLOCK);
            fill(es, cx-3, AY+28, cz-3, cx+3, AY+28, cz+3, BlockTypes.GLOWSTONE);
            // 4 lava channels (3-wide magma block, diagonal)
            for (int[] d : new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}) {
                for (int r = 22; r <= 65; r++) {
                    fill(es, cx+d[0]*r-1, AY, cz+d[1]*r-1,
                             cx+d[0]*r+1, AY, cz+d[1]*r+1, BlockTypes.MAGMA_BLOCK);
                }
            }
            // 3 concentric retaining walls at r=35,50,65
            for (int wallR : new int[]{35, 50, 65}) {
                int wh = (wallR == 65) ? 6 : 4;
                for (int y = AY+1; y <= AY+wh; y++) {
                    ring(es, cx, y, cz, wallR-1, wallR+1, BlockTypes.STONE_BRICKS);
                }
            }
            // Inner terrace (r<35): podzol + 15 jungle trees
            disk(es, cx, AY, cz, 34, BlockTypes.PODZOL);
            for (int i = 0; i < 15; i++) {
                double a = i * Math.PI * 2 / 15;
                int r = 24 + (i%3)*4;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 34 || Math.abs(tz-cz) > 34) continue;
                column(es, tx, tz, AY+1, AY+5, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, AY+5, tz-2, tx+2, AY+7, tz+2, BlockTypes.JUNGLE_LEAVES);
            }
            // Soul sand ring around volcano
            ring(es, cx, AY, cz, 22, 25, BlockTypes.SOUL_SAND);
            // Middle terrace (r=35..50): 20 acacia trees
            for (int i = 0; i < 20; i++) {
                double a = (i + 0.5) * Math.PI / 10;
                int r = 39 + (i%4)*3;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                column(es, tx, tz, AY+1, AY+5, BlockTypes.ACACIA_LOG);
                fill(es, tx-2, AY+5, tz-2, tx+2, AY+6, tz+2, BlockTypes.ACACIA_LEAVES);
                fill(es, tx-3, AY+6, tz-3, tx+3, AY+6, tz+3, BlockTypes.ACACIA_LEAVES);
            }
            // Outer terrace (r=50..65): 10 dark oak + 15 bamboo clusters
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5;
                int tx = cx + (int)Math.round(56 * Math.cos(a));
                int tz = cz + (int)Math.round(56 * Math.sin(a));
                if (Math.abs(tx-cx) > 73 || Math.abs(tz-cz) > 73) continue;
                column(es, tx, tz, AY+1, AY+6, BlockTypes.DARK_OAK_LOG);
                fill(es, tx-2, AY+6, tz-2, tx+2, AY+8, tz+2, BlockTypes.DARK_OAK_LEAVES);
            }
            for (int i = 0; i < 15; i++) {
                double a = (i + 0.5) * Math.PI * 2 / 15;
                int r = 58 + (i%3)*4;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 73 || Math.abs(tz-cz) > 73) continue;
                int h = 5 + (i%3);
                column(es, tx, tz, AY+1, AY+h, BlockTypes.BAMBOO);
                column(es, tx+1, tz, AY+1, AY+h-1, BlockTypes.BAMBOO);
            }
            // Outer perimeter wall: basalt 6 tall
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-73, y, cz-73, cx+73, y, cz-73, BlockTypes.BASALT);
                fill(es, cx-73, y, cz+73, cx+73, y, cz+73, BlockTypes.BASALT);
                fill(es, cx-73, y, cz-73, cx-73, y, cz+73, BlockTypes.BASALT);
                fill(es, cx+73, y, cz-73, cx+73, y, cz+73, BlockTypes.BASALT);
            }
            // 4 gateways
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-3, y, cz-73, cx+3, y, cz-73, BlockTypes.AIR);
                fill(es, cx-3, y, cz+73, cx+3, y, cz+73, BlockTypes.AIR);
                fill(es, cx-73, y, cz-3, cx-73, y, cz+3, BlockTypes.AIR);
                fill(es, cx+73, y, cz-3, cx+73, y, cz+3, BlockTypes.AIR);
            }
            // 8 basalt observation platforms at mid-wall
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int px2 = cx + (int)Math.round(68 * Math.cos(a));
                int pz2 = cz + (int)Math.round(68 * Math.sin(a));
                if (Math.abs(px2-cx) > 72 || Math.abs(pz2-cz) > 72) continue;
                fill(es, px2-3, AY, pz2-3, px2+3, AY+8, pz2+3, BlockTypes.POLISHED_BASALT);
                fill(es, px2-3, AY+8, pz2-3, px2+3, AY+8, pz2+3, BlockTypes.POLISHED_BASALT);
            }
            // Shroomlight floor grid inner terrace
            for (int x = cx-30; x <= cx+30; x += 6)
                for (int z = cz-30; z <= cz+30; z += 6)
                    block(es, x, AY, z, BlockTypes.SHROOMLIGHT);
            // 16 lantern posts across terraces
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                int r = 30 + (i%3)*14;
                int lx = cx + (int)Math.round(r * Math.cos(a));
                int lz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(lx-cx) > 72 || Math.abs(lz-cz) > 72) continue;
                column(es, lx, lz, AY+1, AY+3, BlockTypes.OAK_FENCE);
                block(es, lx, AY+4, lz, BlockTypes.LANTERN);
            }
            // 6 rope bridge OAK_PLANKS sections crossing lava channels
            for (int[] d : new int[][]{{1,1},{1,-1},{-1,1}}) {
                for (int r = 25; r <= 32; r++) {
                    fill(es, cx+d[0]*r-1, AY+1, cz+d[1]*r-1,
                             cx+d[0]*r+1, AY+1, cz+d[1]*r+1, BlockTypes.OAK_PLANKS);
                }
            }
            // 8 spawn pads
            int[][] sp = {
                {cx,cz-45},{cx,cz+45},{cx-45,cz},{cx+45,cz},
                {cx-30,cz-30},{cx+30,cz-30},{cx-30,cz+30},{cx+30,cz+30}
            };
            for (int[] s : sp)
                fill(es, s[0]-1, AY, s[1]-1, s[0]+1, AY, s[1]+1, BlockTypes.POLISHED_BASALT);
        }
    }
}
