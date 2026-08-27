package com.lemonpvp.lemonpractice.builder;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
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
        // Extra texture/detail passes run after all base terrain is placed
        buildAllArenaExtraDetail();
        buildAllFFAExtraDetail();
        buildAllCentreFeatures();
        buildAllBorderDecorations();
        buildAllHeightVariation();
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
            // Ground base
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Podzol patches
            fill(es, cx-50, AY, cz+18, cx-46, AY, cz+22, BlockTypes.PODZOL);
            fill(es, cx-34, AY, cz-44, cx-30, AY, cz-40, BlockTypes.PODZOL);
            fill(es, cx-17, AY, cz+48, cx-13, AY, cz+52, BlockTypes.PODZOL);
            fill(es, cx+8,  AY, cz-30, cx+12, AY, cz-26, BlockTypes.PODZOL);
            fill(es, cx+26, AY, cz+40, cx+30, AY, cz+44, BlockTypes.PODZOL);
            fill(es, cx+42, AY, cz-17, cx+46, AY, cz-13, BlockTypes.PODZOL);
            fill(es, cx-57, AY, cz-7,  cx-53, AY, cz-3,  BlockTypes.PODZOL);
            fill(es, cx+53, AY, cz+33, cx+57, AY, cz+37, BlockTypes.PODZOL);
            fill(es, cx-42, AY, cz-52, cx-38, AY, cz-48, BlockTypes.PODZOL);
            fill(es, cx+18, AY, cz-37, cx+22, AY, cz-33, BlockTypes.PODZOL);
            fill(es, cx-27, AY, cz+53, cx-23, AY, cz+57, BlockTypes.PODZOL);
            fill(es, cx+36, AY, cz+8,  cx+40, AY, cz+12, BlockTypes.PODZOL);
            fill(es, cx-52, AY, cz+28, cx-48, AY, cz+32, BlockTypes.PODZOL);
            fill(es, cx+48, AY, cz-50, cx+52, AY, cz-46, BlockTypes.PODZOL);
            fill(es, cx-12, AY, cz-50, cx-8,  AY, cz-46, BlockTypes.PODZOL);
            fill(es, cx+28, AY, cz-12, cx+32, AY, cz-8,  BlockTypes.PODZOL);
            // N/S and E/W cross paths
            fill(es, cx-1, AY, cz-65, cx+1, AY, cz-16, BlockTypes.COARSE_DIRT);
            fill(es, cx-1, AY, cz+16, cx+1, AY, cz+65, BlockTypes.COARSE_DIRT);
            fill(es, cx-65, AY, cz-1, cx-16, AY, cz+1, BlockTypes.COARSE_DIRT);
            fill(es, cx+16, AY, cz-1, cx+65, AY, cz+1, BlockTypes.COARSE_DIRT);
            // Diagonal paths to corners
            for (int r = 16; r <= 52; r++) {
                block(es, cx+r, AY, cz+r, BlockTypes.COARSE_DIRT);
                block(es, cx-r, AY, cz+r, BlockTypes.COARSE_DIRT);
                block(es, cx+r, AY, cz-r, BlockTypes.COARSE_DIRT);
                block(es, cx-r, AY, cz-r, BlockTypes.COARSE_DIRT);
            }
            // 5-TIER PYRAMID
            fill(es, cx-15, AY+1,  cz-15, cx+15, AY+3,  cz+15, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-11, AY+4,  cz-11, cx+11, AY+6,  cz+11, BlockTypes.STONE_BRICKS);
            fill(es, cx-8,  AY+7,  cz-8,  cx+8,  AY+9,  cz+8,  BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-5,  AY+10, cz-5,  cx+5,  AY+12, cz+5,  BlockTypes.CHISELED_STONE_BRICKS);
            fill(es, cx-2,  AY+13, cz-2,  cx+2,  AY+15, cz+2,  BlockTypes.STONE_BRICKS);
            fill(es, cx-1,  AY+16, cz-1,  cx+1,  AY+16, cz+1,  BlockTypes.IRON_BLOCK);
            block(es, cx, AY+17, cz, BlockTypes.BEACON);
            block(es, cx, AY+18, cz, BlockTypes.GLOWSTONE);
            // Hollow tier 1
            fillHollow(es, cx-14, AY+1, cz-14, cx+14, AY+3, cz+14,
                    BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
            // Chiseled accents on tier 1 wall corners
            for (int y = AY+1; y <= AY+3; y++) {
                block(es, cx-15, y, cz-15, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+15, y, cz-15, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-15, y, cz+15, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+15, y, cz+15, BlockTypes.CHISELED_STONE_BRICKS);
            }
            // Chiseled strips every 5 blocks on tier 1 walls
            for (int y = AY+1; y <= AY+3; y++) {
                for (int wx = cx-10; wx <= cx+10; wx += 5) {
                    block(es, wx, y, cz-15, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, wx, y, cz+15, BlockTypes.CHISELED_STONE_BRICKS);
                }
                for (int wz = cz-10; wz <= cz+10; wz += 5) {
                    block(es, cx-15, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                    block(es, cx+15, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                }
            }
            // Gateway openings tier 1
            fill(es, cx-2, AY+1, cz-15, cx+2, AY+3, cz-15, BlockTypes.AIR);
            fill(es, cx-2, AY+1, cz+15, cx+2, AY+3, cz+15, BlockTypes.AIR);
            fill(es, cx-15, AY+1, cz-2, cx-15, AY+3, cz+2, BlockTypes.AIR);
            fill(es, cx+15, AY+1, cz-2, cx+15, AY+3, cz+2, BlockTypes.AIR);
            // Gateway openings tier 2
            fill(es, cx-1, AY+4, cz-11, cx+1, AY+6, cz-11, BlockTypes.AIR);
            fill(es, cx-1, AY+4, cz+11, cx+1, AY+6, cz+11, BlockTypes.AIR);
            fill(es, cx-11, AY+4, cz-1, cx-11, AY+6, cz+1, BlockTypes.AIR);
            fill(es, cx+11, AY+4, cz-1, cx+11, AY+6, cz+1, BlockTypes.AIR);
            // Pyramid floor
            fill(es, cx-13, AY, cz-13, cx+13, AY, cz+13, BlockTypes.STONE_BRICKS);
            for (int fx = cx-12; fx <= cx+12; fx += 4)
                for (int fz = cz-12; fz <= cz+12; fz += 4)
                    block(es, fx, AY, fz, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, cx-10, AY, cz-10, BlockTypes.SEA_LANTERN);
            block(es, cx+10, AY, cz-10, BlockTypes.SEA_LANTERN);
            block(es, cx-10, AY, cz+10, BlockTypes.SEA_LANTERN);
            block(es, cx+10, AY, cz+10, BlockTypes.SEA_LANTERN);
            block(es, cx-6,  AY, cz-6,  BlockTypes.SHROOMLIGHT);
            block(es, cx+6,  AY, cz-6,  BlockTypes.SHROOMLIGHT);
            block(es, cx-6,  AY, cz+6,  BlockTypes.SHROOMLIGHT);
            block(es, cx+6,  AY, cz+6,  BlockTypes.SHROOMLIGHT);
            // 4 interior jungle columns 2x2
            fill(es, cx-12, AY+1, cz-12, cx-11, AY+18, cz-11, BlockTypes.JUNGLE_LOG);
            fill(es, cx+11, AY+1, cz-12, cx+12, AY+18, cz-11, BlockTypes.JUNGLE_LOG);
            fill(es, cx-12, AY+1, cz+11, cx-11, AY+18, cz+12, BlockTypes.JUNGLE_LOG);
            fill(es, cx+11, AY+1, cz+11, cx+12, AY+18, cz+12, BlockTypes.JUNGLE_LOG);
            fill(es, cx-14, AY+15, cz-14, cx-9,  AY+18, cz-9,  BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+9,  AY+15, cz-14, cx+14, AY+18, cz-9,  BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-14, AY+15, cz+9,  cx-9,  AY+18, cz+14, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+9,  AY+15, cz+9,  cx+14, AY+18, cz+14, BlockTypes.JUNGLE_LEAVES);
            // Central altar
            fill(es, cx-2, AY+7, cz-2, cx+2, AY+8, cz+2, BlockTypes.OBSIDIAN);
            fill(es, cx-1, AY+9, cz-1, cx+1, AY+9, cz+1, BlockTypes.GLOWSTONE);
            block(es, cx, AY+10, cz, BlockTypes.SEA_LANTERN);
            // Underground altar chamber
            fill(es, cx-10, AY-8, cz-10, cx+10, AY-1, cz+10, BlockTypes.STONE);
            fillHollow(es, cx-9, AY-7, cz-9, cx+9, AY-1, cz+9,
                    BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
            fill(es, cx-9, AY-8, cz-9, cx+9, AY-8, cz+9, BlockTypes.STONE_BRICKS);
            fill(es, cx-3, AY-6, cz-3, cx+3, AY-5, cz+3, BlockTypes.OBSIDIAN);
            fill(es, cx-2, AY-4, cz-2, cx+2, AY-4, cz+2, BlockTypes.GLOWSTONE);
            block(es, cx, AY-3, cz, BlockTypes.SEA_LANTERN);
            column(es, cx-7, cz-7, AY-7, AY-2, BlockTypes.MOSSY_STONE_BRICKS);
            column(es, cx+7, cz-7, AY-7, AY-2, BlockTypes.MOSSY_STONE_BRICKS);
            column(es, cx-7, cz+7, AY-7, AY-2, BlockTypes.MOSSY_STONE_BRICKS);
            column(es, cx+7, cz+7, AY-7, AY-2, BlockTypes.MOSSY_STONE_BRICKS);
            for (int y = AY-7; y <= AY-3; y += 2) {
                block(es, cx-9, y, cz-4, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-9, y, cz,   BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-9, y, cz+4, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+9, y, cz-4, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+9, y, cz,   BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+9, y, cz+4, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-4, y, cz-9, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx,   y, cz-9, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+4, y, cz-9, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-4, y, cz+9, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx,   y, cz+9, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+4, y, cz+9, BlockTypes.CHISELED_STONE_BRICKS);
            }
            // Staircase to underground
            for (int s = 0; s <= 8; s++)
                block(es, cx-15+s, AY-s, cz-13, BlockTypes.MOSSY_STONE_BRICKS);
            // 4 CORNER GUARDIAN TOWERS (9x9 hollow, 22 tall)
            int[][] towers = {
                {cx-52, cz-52}, {cx+50, cz-52},
                {cx-52, cz+50}, {cx+50, cz+50}
            };
            for (int[] tc : towers) {
                fillHollow(es, tc[0]-4, AY+1, tc[1]-4, tc[0]+4, AY+22, tc[1]+4,
                        BlockTypes.STONE_BRICKS, BlockTypes.AIR);
                for (int y = AY+1; y <= AY+22; y += 7) {
                    for (int wx = tc[0]-4; wx <= tc[0]+4; wx++) {
                        block(es, wx, y, tc[1]-4, BlockTypes.CHISELED_STONE_BRICKS);
                        block(es, wx, y, tc[1]+4, BlockTypes.CHISELED_STONE_BRICKS);
                    }
                    for (int wz = tc[1]-4; wz <= tc[1]+4; wz++) {
                        block(es, tc[0]-4, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                        block(es, tc[0]+4, y, wz, BlockTypes.CHISELED_STONE_BRICKS);
                    }
                }
                // Arrow slits
                block(es, tc[0], AY+5,  tc[1]-4, BlockTypes.AIR);
                block(es, tc[0], AY+12, tc[1]-4, BlockTypes.AIR);
                block(es, tc[0], AY+18, tc[1]-4, BlockTypes.AIR);
                block(es, tc[0], AY+5,  tc[1]+4, BlockTypes.AIR);
                block(es, tc[0], AY+12, tc[1]+4, BlockTypes.AIR);
                block(es, tc[0], AY+18, tc[1]+4, BlockTypes.AIR);
                block(es, tc[0]-4, AY+5,  tc[1], BlockTypes.AIR);
                block(es, tc[0]-4, AY+12, tc[1], BlockTypes.AIR);
                block(es, tc[0]-4, AY+18, tc[1], BlockTypes.AIR);
                block(es, tc[0]+4, AY+5,  tc[1], BlockTypes.AIR);
                block(es, tc[0]+4, AY+12, tc[1], BlockTypes.AIR);
                block(es, tc[0]+4, AY+18, tc[1], BlockTypes.AIR);
                fill(es, tc[0]-3, AY, tc[1]-3, tc[0]+3, AY, tc[1]+3, BlockTypes.STONE_BRICKS);
                fill(es, tc[0]-3, AY+11, tc[1]-3, tc[0]+3, AY+11, tc[1]+3, BlockTypes.OAK_PLANKS);
                fill(es, tc[0]-1, AY+11, tc[1]-1, tc[0]+1, AY+11, tc[1]+1, BlockTypes.AIR);
                fill(es, tc[0]-4, AY+23, tc[1]-4, tc[0]+4, AY+23, tc[1]+4, BlockTypes.MOSSY_STONE_BRICKS);
                for (int wx = tc[0]-4; wx <= tc[0]+4; wx += 2) {
                    block(es, wx, AY+24, tc[1]-4, BlockTypes.STONE_BRICKS);
                    block(es, wx, AY+24, tc[1]+4, BlockTypes.STONE_BRICKS);
                }
                for (int wz = tc[1]-4; wz <= tc[1]+4; wz += 2) {
                    block(es, tc[0]-4, AY+24, wz, BlockTypes.STONE_BRICKS);
                    block(es, tc[0]+4, AY+24, wz, BlockTypes.STONE_BRICKS);
                }
                block(es, tc[0]-4, AY+25, tc[1]-4, BlockTypes.SEA_LANTERN);
                block(es, tc[0]+4, AY+25, tc[1]-4, BlockTypes.SEA_LANTERN);
                block(es, tc[0]-4, AY+25, tc[1]+4, BlockTypes.SEA_LANTERN);
                block(es, tc[0]+4, AY+25, tc[1]+4, BlockTypes.SEA_LANTERN);
                block(es, tc[0],   AY+25, tc[1],   BlockTypes.LANTERN);
                fill(es, tc[0]-1, AY+1, tc[1]+4, tc[0]+1, AY+3, tc[1]+4, BlockTypes.AIR);
                fill(es, tc[0]+4, AY+1, tc[1]-1, tc[0]+4, AY+3, tc[1]+1, BlockTypes.AIR);
            }
            // 4 STONE OBELISKS at r=45
            fill(es, cx-2, AY+1, cz-47, cx+2, AY+2, cz-43, BlockTypes.STONE_BRICKS);
            fill(es, cx-1, AY+3, cz-46, cx+1, AY+10, cz-44, BlockTypes.STONE_BRICKS);
            block(es, cx, AY+11, cz-45, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, cx, AY+12, cz-45, BlockTypes.STONE_BRICKS);
            block(es, cx, AY+13, cz-45, BlockTypes.SEA_LANTERN);
            for (int y = AY+3; y <= AY+10; y += 2) {
                block(es, cx-1, y, cz-46, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+1, y, cz-46, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-1, y, cz-44, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+1, y, cz-44, BlockTypes.CHISELED_STONE_BRICKS);
            }
            fill(es, cx-2, AY+1, cz+43, cx+2, AY+2, cz+47, BlockTypes.STONE_BRICKS);
            fill(es, cx-1, AY+3, cz+44, cx+1, AY+10, cz+46, BlockTypes.STONE_BRICKS);
            block(es, cx, AY+11, cz+45, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, cx, AY+12, cz+45, BlockTypes.STONE_BRICKS);
            block(es, cx, AY+13, cz+45, BlockTypes.SEA_LANTERN);
            for (int y = AY+3; y <= AY+10; y += 2) {
                block(es, cx-1, y, cz+44, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+1, y, cz+44, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-1, y, cz+46, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+1, y, cz+46, BlockTypes.CHISELED_STONE_BRICKS);
            }
            fill(es, cx-47, AY+1, cz-2, cx-43, AY+2, cz+2, BlockTypes.STONE_BRICKS);
            fill(es, cx-46, AY+3, cz-1, cx-44, AY+10, cz+1, BlockTypes.STONE_BRICKS);
            block(es, cx-45, AY+11, cz, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, cx-45, AY+12, cz, BlockTypes.STONE_BRICKS);
            block(es, cx-45, AY+13, cz, BlockTypes.SEA_LANTERN);
            for (int y = AY+3; y <= AY+10; y += 2) {
                block(es, cx-46, y, cz-1, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-46, y, cz+1, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-44, y, cz-1, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-44, y, cz+1, BlockTypes.CHISELED_STONE_BRICKS);
            }
            fill(es, cx+43, AY+1, cz-2, cx+47, AY+2, cz+2, BlockTypes.STONE_BRICKS);
            fill(es, cx+44, AY+3, cz-1, cx+46, AY+10, cz+1, BlockTypes.STONE_BRICKS);
            block(es, cx+45, AY+11, cz, BlockTypes.CHISELED_STONE_BRICKS);
            block(es, cx+45, AY+12, cz, BlockTypes.STONE_BRICKS);
            block(es, cx+45, AY+13, cz, BlockTypes.SEA_LANTERN);
            for (int y = AY+3; y <= AY+10; y += 2) {
                block(es, cx+44, y, cz-1, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+44, y, cz+1, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+46, y, cz-1, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+46, y, cz+1, BlockTypes.CHISELED_STONE_BRICKS);
            }
            // 20 MASSIVE 2x2 JUNGLE TREES
            fill(es, cx-50, AY+1, cz-30, cx-49, AY+10, cz-29, BlockTypes.JUNGLE_LOG);
            fill(es, cx-53, AY+8,  cz-33, cx-46, AY+12, cz-26, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-52, AY+13, cz-32, cx-47, AY+14, cz-27, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-51, AY+15, cz-31, cx-48, AY+16, cz-28, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-36, AY+1, cz-50, cx-35, AY+12, cz-49, BlockTypes.JUNGLE_LOG);
            fill(es, cx-39, AY+10, cz-53, cx-32, AY+14, cz-46, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-38, AY+15, cz-52, cx-33, AY+16, cz-47, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+35, AY+1,  cz-45, cx+36, AY+13, cz-44, BlockTypes.JUNGLE_LOG);
            fill(es, cx+32, AY+11, cz-48, cx+39, AY+15, cz-41, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+33, AY+16, cz-47, cx+38, AY+17, cz-42, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+50, AY+1,  cz-20, cx+51, AY+10, cz-19, BlockTypes.JUNGLE_LOG);
            fill(es, cx+47, AY+8,  cz-23, cx+54, AY+12, cz-16, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+48, AY+13, cz-22, cx+53, AY+14, cz-17, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-46, AY+1,  cz+30, cx-45, AY+11, cz+31, BlockTypes.JUNGLE_LOG);
            fill(es, cx-49, AY+9,  cz+27, cx-42, AY+13, cz+34, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-48, AY+14, cz+28, cx-43, AY+15, cz+33, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-30, AY+1,  cz+45, cx-29, AY+12, cz+46, BlockTypes.JUNGLE_LOG);
            fill(es, cx-33, AY+10, cz+42, cx-26, AY+14, cz+49, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-32, AY+15, cz+43, cx-27, AY+16, cz+48, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-55, AY+1,  cz+10, cx-54, AY+8,  cz+11, BlockTypes.JUNGLE_LOG);
            fill(es, cx-58, AY+6,  cz+7,  cx-51, AY+10, cz+14, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-57, AY+11, cz+8,  cx-52, AY+12, cz+13, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+44, AY+1,  cz+20, cx+45, AY+10, cz+21, BlockTypes.JUNGLE_LOG);
            fill(es, cx+41, AY+8,  cz+17, cx+48, AY+12, cz+24, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+42, AY+13, cz+18, cx+47, AY+14, cz+23, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+28, AY+1,  cz+42, cx+29, AY+13, cz+43, BlockTypes.JUNGLE_LOG);
            fill(es, cx+25, AY+11, cz+39, cx+32, AY+15, cz+46, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+26, AY+16, cz+40, cx+31, AY+17, cz+45, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+55, AY+1,  cz+50, cx+56, AY+9,  cz+51, BlockTypes.JUNGLE_LOG);
            fill(es, cx+52, AY+7,  cz+47, cx+59, AY+11, cz+54, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+53, AY+12, cz+48, cx+58, AY+13, cz+53, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-42, AY+1,  cz-5,  cx-41, AY+11, cz-4,  BlockTypes.JUNGLE_LOG);
            fill(es, cx-45, AY+9,  cz-8,  cx-38, AY+13, cz-1,  BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-44, AY+14, cz-7,  cx-39, AY+15, cz-2,  BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+40, AY+1,  cz-6,  cx+41, AY+10, cz-5,  BlockTypes.JUNGLE_LOG);
            fill(es, cx+37, AY+8,  cz-9,  cx+44, AY+12, cz-2,  BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+38, AY+13, cz-8,  cx+43, AY+14, cz-3,  BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-5,  AY+1,  cz-55, cx-4,  AY+12, cz-54, BlockTypes.JUNGLE_LOG);
            fill(es, cx-8,  AY+10, cz-58, cx-1,  AY+14, cz-51, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-7,  AY+15, cz-57, cx-2,  AY+16, cz-52, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+4,  AY+1,  cz+53, cx+5,  AY+11, cz+54, BlockTypes.JUNGLE_LOG);
            fill(es, cx+1,  AY+9,  cz+50, cx+8,  AY+13, cz+57, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+2,  AY+14, cz+51, cx+7,  AY+15, cz+56, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-22, AY+1,  cz+55, cx-21, AY+9,  cz+56, BlockTypes.JUNGLE_LOG);
            fill(es, cx-25, AY+7,  cz+52, cx-18, AY+11, cz+59, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx-24, AY+12, cz+53, cx-19, AY+13, cz+58, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+18, AY+1,  cz+57, cx+19, AY+10, cz+58, BlockTypes.JUNGLE_LOG);
            fill(es, cx+15, AY+8,  cz+54, cx+22, AY+12, cz+61, BlockTypes.JUNGLE_LEAVES);
            fill(es, cx+16, AY+13, cz+55, cx+21, AY+14, cz+60, BlockTypes.JUNGLE_LEAVES);
            // 8 medium single-trunk trees inner ring
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + Math.PI / 8;
                int tx = cx + (int)Math.round(30 * Math.cos(a));
                int tz = cz + (int)Math.round(30 * Math.sin(a));
                int h  = AY + 6 + (i % 4);
                column(es, tx, tz, AY+1, h, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, h, tz-2, tx+2, h+2, tz+2, BlockTypes.JUNGLE_LEAVES);
                fill(es, tx-1, h+3, tz-1, tx+1, h+3, tz+1, BlockTypes.JUNGLE_LEAVES);
            }
            // 2 REFLECTING POOLS
            fill(es, cx-32, AY-1, cz-5, cx-18, AY-1, cz+5, BlockTypes.STONE);
            fill(es, cx-32, AY,   cz-5, cx-18, AY,   cz+5, BlockTypes.WATER);
            fill(es, cx-33, AY+1, cz-6, cx-17, AY+1, cz-6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-33, AY+1, cz+6, cx-17, AY+1, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-33, AY+1, cz-6, cx-33, AY+1, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-17, AY+1, cz-6, cx-17, AY+1, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, cx-33, AY+2, cz-6, BlockTypes.SEA_LANTERN);
            block(es, cx-17, AY+2, cz-6, BlockTypes.SEA_LANTERN);
            block(es, cx-33, AY+2, cz+6, BlockTypes.SEA_LANTERN);
            block(es, cx-17, AY+2, cz+6, BlockTypes.SEA_LANTERN);
            block(es, cx-29, AY, cz-2, BlockTypes.LILY_PAD);
            block(es, cx-25, AY, cz+3, BlockTypes.LILY_PAD);
            block(es, cx-21, AY, cz-3, BlockTypes.LILY_PAD);
            block(es, cx-27, AY, cz+1, BlockTypes.LILY_PAD);
            fill(es, cx+18, AY-1, cz-5, cx+32, AY-1, cz+5, BlockTypes.STONE);
            fill(es, cx+18, AY,   cz-5, cx+32, AY,   cz+5, BlockTypes.WATER);
            fill(es, cx+17, AY+1, cz-6, cx+33, AY+1, cz-6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+17, AY+1, cz+6, cx+33, AY+1, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+17, AY+1, cz-6, cx+17, AY+1, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+33, AY+1, cz-6, cx+33, AY+1, cz+6, BlockTypes.MOSSY_STONE_BRICKS);
            block(es, cx+17, AY+2, cz-6, BlockTypes.SEA_LANTERN);
            block(es, cx+33, AY+2, cz-6, BlockTypes.SEA_LANTERN);
            block(es, cx+17, AY+2, cz+6, BlockTypes.SEA_LANTERN);
            block(es, cx+33, AY+2, cz+6, BlockTypes.SEA_LANTERN);
            block(es, cx+21, AY, cz+2, BlockTypes.LILY_PAD);
            block(es, cx+25, AY, cz-3, BlockTypes.LILY_PAD);
            block(es, cx+29, AY, cz+3, BlockTypes.LILY_PAD);
            block(es, cx+23, AY, cz-1, BlockTypes.LILY_PAD);
            // Water channels N/S
            for (int r = 16; r <= 62; r++) {
                block(es, cx, AY, cz+r, BlockTypes.WATER);
                block(es, cx, AY, cz-r, BlockTypes.WATER);
            }
            // BAMBOO CLUSTERS (24)
            column(es, cx-55, cz+15, AY+1, AY+6, BlockTypes.BAMBOO);
            column(es, cx-54, cz+16, AY+1, AY+5, BlockTypes.BAMBOO);
            column(es, cx-53, cz+15, AY+1, AY+4, BlockTypes.BAMBOO);
            column(es, cx-48, cz+40, AY+1, AY+7, BlockTypes.BAMBOO);
            column(es, cx-49, cz+41, AY+1, AY+6, BlockTypes.BAMBOO);
            column(es, cx-35, cz-28, AY+1, AY+5, BlockTypes.BAMBOO);
            column(es, cx-34, cz-29, AY+1, AY+4, BlockTypes.BAMBOO);
            column(es, cx-22, cz-50, AY+1, AY+8, BlockTypes.BAMBOO);
            column(es, cx-21, cz-51, AY+1, AY+7, BlockTypes.BAMBOO);
            column(es, cx-10, cz+55, AY+1, AY+6, BlockTypes.BAMBOO);
            column(es, cx-9,  cz+56, AY+1, AY+5, BlockTypes.BAMBOO);
            column(es, cx+12, cz-48, AY+1, AY+7, BlockTypes.BAMBOO);
            column(es, cx+13, cz-47, AY+1, AY+6, BlockTypes.BAMBOO);
            column(es, cx+26, cz+38, AY+1, AY+5, BlockTypes.BAMBOO);
            column(es, cx+27, cz+39, AY+1, AY+4, BlockTypes.BAMBOO);
            column(es, cx+40, cz-20, AY+1, AY+8, BlockTypes.BAMBOO);
            column(es, cx+41, cz-21, AY+1, AY+7, BlockTypes.BAMBOO);
            column(es, cx+55, cz+5,  AY+1, AY+5, BlockTypes.BAMBOO);
            column(es, cx+56, cz+4,  AY+1, AY+4, BlockTypes.BAMBOO);
            column(es, cx-52, cz-30, AY+1, AY+6, BlockTypes.BAMBOO);
            column(es, cx-51, cz-31, AY+1, AY+5, BlockTypes.BAMBOO);
            column(es, cx+38, cz+55, AY+1, AY+7, BlockTypes.BAMBOO);
            column(es, cx+39, cz+56, AY+1, AY+6, BlockTypes.BAMBOO);
            column(es, cx-45, cz+55, AY+1, AY+5, BlockTypes.BAMBOO);
            // 10 FALLEN LOGS
            fill(es, cx-55, AY+1, cz-30, cx-49, AY+1, cz-30, BlockTypes.JUNGLE_LOG);
            fill(es, cx-40, AY+1, cz+50, cx-33, AY+1, cz+50, BlockTypes.JUNGLE_LOG);
            fill(es, cx+25, AY+1, cz-55, cx+32, AY+1, cz-55, BlockTypes.JUNGLE_LOG);
            fill(es, cx+45, AY+1, cz+20, cx+52, AY+1, cz+20, BlockTypes.JUNGLE_LOG);
            fill(es, cx-30, AY+1, cz-5,  cx-24, AY+1, cz-5,  BlockTypes.JUNGLE_LOG);
            fill(es, cx+10, AY+1, cz+58, cx+19, AY+1, cz+58, BlockTypes.JUNGLE_LOG);
            fill(es, cx-50, AY+1, cz+30, cx-45, AY+1, cz+30, BlockTypes.JUNGLE_LOG);
            fill(es, cx+40, AY+1, cz-40, cx+47, AY+1, cz-40, BlockTypes.JUNGLE_LOG);
            fill(es, cx-22, AY+1, cz+48, cx-16, AY+1, cz+48, BlockTypes.JUNGLE_LOG);
            fill(es, cx+35, AY+1, cz-48, cx+43, AY+1, cz-48, BlockTypes.JUNGLE_LOG);
            // HANGING ROOTS on tier 1
            for (int wx = cx-14; wx <= cx+14; wx += 3) {
                block(es, wx, AY+4, cz-15, BlockTypes.HANGING_ROOTS);
                block(es, wx, AY+4, cz+15, BlockTypes.HANGING_ROOTS);
            }
            for (int wz = cz-14; wz <= cz+14; wz += 3) {
                block(es, cx-15, AY+4, wz, BlockTypes.HANGING_ROOTS);
                block(es, cx+15, AY+4, wz, BlockTypes.HANGING_ROOTS);
            }
            // 32 LANTERN POSTS (inner r=20, outer r=40)
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                int lx = cx + (int)Math.round(20 * Math.cos(a));
                int lz = cz + (int)Math.round(20 * Math.sin(a));
                column(es, lx, lz, AY+1, AY+3, BlockTypes.OAK_FENCE);
                block(es, lx, AY+4, lz, BlockTypes.LANTERN);
            }
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8 + Math.PI / 16;
                int lx = cx + (int)Math.round(40 * Math.cos(a));
                int lz = cz + (int)Math.round(40 * Math.sin(a));
                column(es, lx, lz, AY+1, AY+3, BlockTypes.OAK_FENCE);
                block(es, lx, AY+4, lz, BlockTypes.LANTERN);
            }
            // VEGETATION
            for (int i = 0; i < 50; i++) {
                int sx = cx - 63 + (i * 17 + 7) % 127;
                int sz = cz - 63 + (i * 13 + 3) % 127;
                if (Math.abs(sx-cx) <= 16 && Math.abs(sz-cz) <= 16) continue;
                block(es, sx, AY+1, sz, (i % 4 == 0) ? BlockTypes.FERN : BlockTypes.SHORT_GRASS);
            }
            for (int i = 0; i < 20; i++) {
                int sx = cx - 63 + (i * 23 + 11) % 127;
                int sz = cz - 63 + (i * 19 + 7)  % 127;
                block(es, sx, AY+1, sz, BlockTypes.DEAD_BUSH);
            }
            // PERIMETER WALL — mossy cobblestone 7 tall + buttresses
            for (int y = AY+1; y <= AY+7; y++) {
                fill(es, cx-63, y, cz-63, cx+63, y, cz-63, BlockTypes.MOSSY_COBBLESTONE);
                fill(es, cx-63, y, cz+63, cx+63, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
                fill(es, cx-63, y, cz-63, cx-63, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
                fill(es, cx+63, y, cz-63, cx+63, y, cz+63, BlockTypes.MOSSY_COBBLESTONE);
            }
            for (int wx = cx-62; wx <= cx+62; wx += 8) {
                fill(es, wx, AY+2, cz-63, wx+1, AY+5, cz-63, BlockTypes.MOSS_BLOCK);
                fill(es, wx, AY+2, cz+63, wx+1, AY+5, cz+63, BlockTypes.MOSS_BLOCK);
            }
            for (int wz = cz-62; wz <= cz+62; wz += 8) {
                fill(es, cx-63, AY+2, wz, cx-63, AY+5, wz+1, BlockTypes.MOSS_BLOCK);
                fill(es, cx+63, AY+2, wz, cx+63, AY+5, wz+1, BlockTypes.MOSS_BLOCK);
            }
            for (int wx = cx-63; wx <= cx+63; wx += 4) {
                block(es, wx, AY+8, cz-63, BlockTypes.MOSSY_COBBLESTONE);
                block(es, wx, AY+8, cz+63, BlockTypes.MOSSY_COBBLESTONE);
            }
            for (int wz = cz-63; wz <= cz+63; wz += 4) {
                block(es, cx-63, AY+8, wz, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx+63, AY+8, wz, BlockTypes.MOSSY_COBBLESTONE);
            }
            for (int wx = cx-56; wx <= cx+56; wx += 14) {
                fill(es, wx-1, AY+1, cz-63, wx+1, AY+10, cz-63, BlockTypes.JUNGLE_LOG);
                fill(es, wx-1, AY+1, cz+63, wx+1, AY+10, cz+63, BlockTypes.JUNGLE_LOG);
            }
            for (int wz = cz-56; wz <= cz+56; wz += 14) {
                fill(es, cx-63, AY+1, wz-1, cx-63, AY+10, wz+1, BlockTypes.JUNGLE_LOG);
                fill(es, cx+63, AY+1, wz-1, cx+63, AY+10, wz+1, BlockTypes.JUNGLE_LOG);
            }
            fill(es, cx-2, AY+1, cz-63, cx+2, AY+7, cz-63, BlockTypes.AIR);
            fill(es, cx-2, AY+1, cz+63, cx+2, AY+7, cz+63, BlockTypes.AIR);
            fill(es, cx-63, AY+1, cz-2, cx-63, AY+7, cz+2, BlockTypes.AIR);
            fill(es, cx+63, AY+1, cz-2, cx+63, AY+7, cz+2, BlockTypes.AIR);
            fill(es, cx-3, AY+8, cz-63, cx+3, AY+8, cz-63, BlockTypes.STONE_BRICKS);
            fill(es, cx-3, AY+8, cz+63, cx+3, AY+8, cz+63, BlockTypes.STONE_BRICKS);
            fill(es, cx-63, AY+8, cz-3, cx-63, AY+8, cz+3, BlockTypes.STONE_BRICKS);
            fill(es, cx+63, AY+8, cz-3, cx+63, AY+8, cz+3, BlockTypes.STONE_BRICKS);
            column(es, cx-5, cz-63, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx-5, AY+4, cz-63, BlockTypes.LANTERN);
            column(es, cx+5, cz-63, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx+5, AY+4, cz-63, BlockTypes.LANTERN);
            column(es, cx-5, cz+63, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx-5, AY+4, cz+63, BlockTypes.LANTERN);
            column(es, cx+5, cz+63, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx+5, AY+4, cz+63, BlockTypes.LANTERN);
            column(es, cx-63, cz-5, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx-63, AY+4, cz-5, BlockTypes.LANTERN);
            column(es, cx-63, cz+5, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx-63, AY+4, cz+5, BlockTypes.LANTERN);
            column(es, cx+63, cz-5, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx+63, AY+4, cz-5, BlockTypes.LANTERN);
            column(es, cx+63, cz+5, AY+1, AY+3, BlockTypes.OAK_FENCE);
            block(es,  cx+63, AY+4, cz+5, BlockTypes.LANTERN);
            // 8 TOTEM POLES r=55
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int tx = cx + (int)Math.round(55 * Math.cos(a));
                int tz = cz + (int)Math.round(55 * Math.sin(a));
                if (Math.abs(tx-cx) > 62 || Math.abs(tz-cz) > 62) continue;
                for (int y = AY+1; y <= AY+8; y++)
                    block(es, tx, y, tz, (y % 2 == 0) ? BlockTypes.JUNGLE_LOG : BlockTypes.ACACIA_LOG);
                fill(es, tx-1, AY+9, tz-1, tx+1, AY+11, tz+1, BlockTypes.JUNGLE_LEAVES);
                block(es, tx, AY+12, tz, BlockTypes.SEA_LANTERN);
            }
            // ── EXTENDED GROUND TEXTURING (dense podzol / coarse mix) ─────
            // Spiralling terracotta-style floor inlays near pyramid base
            for (int r = 16; r <= 35; r++) {
                for (double a = 0; a < 2*Math.PI; a += 0.25) {
                    int ix = cx + (int)Math.round(r * Math.cos(a));
                    int iz = cz + (int)Math.round(r * Math.sin(a));
                    if ((r + (int)(a*2)) % 4 == 0)
                        block(es, ix, AY, iz, BlockTypes.STONE_BRICKS);
                    else if ((r + (int)(a*2)) % 4 == 1)
                        block(es, ix, AY, iz, BlockTypes.MOSSY_STONE_BRICKS);
                }
            }

            // ── ADDITIONAL PODZOL MICRO-PATCHES ───────────────────────────
            for (int i = 0; i < 60; i++) {
                int px = cx - 60 + (i * 19 + 5) % 121;
                int pz = cz - 60 + (i * 13 + 9) % 121;
                fill(es, px, AY, pz, px+1, AY, pz+1, BlockTypes.PODZOL);
            }

            // ── MOSSY COBBLESTONE ROAD (crossroads repaving) ──────────────
            fill(es, cx-63, AY+1, cz-1, cx-16, AY+1, cz+1, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+16, AY+1, cz-1, cx+63, AY+1, cz+1, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx-1, AY+1, cz-63, cx+1, AY+1, cz-16, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx-1, AY+1, cz+16, cx+1, AY+1, cz+63, BlockTypes.MOSSY_COBBLESTONE);

            // ── JUNGLE FLOOR SCATTER (third pass) ─────────────────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 60 + (i * 53 + 31) % 121;
                int sz = cz - 60 + (i * 47 + 23) % 121;
                if (Math.abs(sx-cx) <= 16 && Math.abs(sz-cz) <= 16) continue;
                int r = i % 8;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.STONE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else             block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
            }

            // ── REFLECTING POOLS (two side canals near pyramid) ───────────
            fill(es, cx+16, 63, cz-8, cx+30, 63, cz+8, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+16, AY, cz-8, cx+30, AY, cz+8, BlockTypes.WATER);
            fill(es, cx-30, 63, cz-8, cx-16, 63, cz+8, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-30, AY, cz-8, cx-16, AY, cz+8, BlockTypes.WATER);
            fill(es, cx-8, 63, cz+16, cx+8, 63, cz+30, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-8, AY, cz+16, cx+8, AY, cz+30, BlockTypes.WATER);
            fill(es, cx-8, 63, cz-30, cx+8, 63, cz-16, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-8, AY, cz-30, cx+8, AY, cz-16, BlockTypes.WATER);

            // ── CHISELED STONE BRICK INLAY at pool edges ──────────────────
            for (int d = -8; d <= 8; d++) {
                block(es, cx+16, AY+1, cz+d, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+30, AY+1, cz+d, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-16, AY+1, cz+d, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx-30, AY+1, cz+d, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+d, AY+1, cz+16, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+d, AY+1, cz+30, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+d, AY+1, cz-16, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx+d, AY+1, cz-30, BlockTypes.CHISELED_STONE_BRICKS);
            }

            // ── 24 EXTRA JUNGLE TREES in outer zone ───────────────────────
            int[][] extraTrees = {
                {cx-58,cz- 8,10},{cx+58,cz+ 8,11},{cx-58,cz+ 8,9},{cx+58,cz-8,10},
                {cx- 8,cz-58,12},{cx+ 8,cz+58,10},{cx- 8,cz+58,9},{cx+8,cz-58,11},
                {cx-55,cz-40, 9},{cx+55,cz+40,10},{cx+40,cz-55,9},{cx-40,cz+55,10},
                {cx-55,cz+40, 8},{cx+55,cz-40,10},{cx-40,cz-55,9},{cx+40,cz+55,11},
                {cx-62,cz-55, 8},{cx+62,cz+55,9},{cx+55,cz-62, 9},{cx-55,cz+62,8},
                {cx-50,cz+58, 9},{cx+50,cz-58,10},{cx+58,cz+50,8},{cx-58,cz-50,9}
            };
            for (int[] t : extraTrees) {
                int tx = t[0], tz = t[1], th = t[2];
                column(es, tx, tz, AY+1, AY+th, BlockTypes.JUNGLE_LOG);
                disk(es, tx, AY+th+1, tz, 5, BlockTypes.JUNGLE_LEAVES);
                disk(es, tx, AY+th+2, tz, 4, BlockTypes.JUNGLE_LEAVES);
                disk(es, tx, AY+th+3, tz, 3, BlockTypes.JUNGLE_LEAVES);
                disk(es, tx, AY+th+4, tz, 2, BlockTypes.JUNGLE_LEAVES);
                block(es, tx, AY+th+5, tz, BlockTypes.JUNGLE_LEAVES);
                block(es, tx, AY, tz, BlockTypes.PODZOL);
            }

            // ── OBSIDIAN ALTAR STEPS down to underground ──────────────────
            fill(es, cx-3, AY, cz-3, cx+3, AY, cz+3, BlockTypes.OBSIDIAN);
            fill(es, cx-2, 63, cz-2, cx+2, 63, cz+2, BlockTypes.OBSIDIAN);
            fill(es, cx-1, 62, cz-1, cx+1, 62, cz+1, BlockTypes.OBSIDIAN);
            fill(es, cx-1, 56, cz-1, cx+1, 58, cz+1, BlockTypes.OBSIDIAN);

            // ── FOURTH SCATTER PASS (ultra-fine mossy texture) ────────────
            for (int i = 0; i < 280; i++) {
                int sx = cx - 62 + (i * 61 + 37) % 125;
                int sz = cz - 62 + (i * 57 + 31) % 125;
                int r  = i % 6;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CRACKED_STONE_BRICKS);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else             block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
            }

            // 4 SPAWN PADS
            fill(es, cx-1, AY, cz-48, cx+1, AY, cz-46, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-1, AY, cz+46, cx+1, AY, cz+48, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-48, AY, cz-1, cx-46, AY, cz+1, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+46, AY, cz-1, cx+48, AY, cz+1, BlockTypes.MOSSY_STONE_BRICKS);
        }
    }

    // =========================================================================
    // ARENA 2 — TROPICAL BEACH   cx=-300, cz=0   (130x130)
    // =========================================================================
    private void buildBeachArena() {
        final int cx = -300, cz = 0;
        try (EditSession es = newSession()) {
            // Stone base + sand + water moat
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, 62, cz-65, cx+65, 63, cz+65, BlockTypes.SAND);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.WATER);
            // Island sandstone base (cx±50)
            fill(es, cx-50, 62, cz-50, cx+50, 63, cz+50, BlockTypes.SANDSTONE);
            fill(es, cx-50, AY, cz-50, cx+50, AY, cz+50, BlockTypes.SAND);
            // 4 grass zones
            fill(es, cx-38, AY+1, cz-28, cx-10, AY+1, cz-5,  BlockTypes.GRASS_BLOCK);
            fill(es, cx+10, AY+1, cz-28, cx+38, AY+1, cz-5,  BlockTypes.GRASS_BLOCK);
            fill(es, cx-38, AY+1, cz+5,  cx-10, AY+1, cz+28, BlockTypes.GRASS_BLOCK);
            fill(es, cx+10, AY+1, cz+5,  cx+38, AY+1, cz+28, BlockTypes.GRASS_BLOCK);
            // CUT_SANDSTONE floor stripes
            for (int x = cx-48; x <= cx+48; x += 10)
                fill(es, x, AY, cz-48, x, AY, cz+48, BlockTypes.CUT_SANDSTONE);
            // Central sandstone platform (16x16)
            fill(es, cx-8, AY+1, cz-8, cx+8, AY+2, cz+8, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-8, AY+1, cz-8, cx+8, AY+1, cz+8, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx-8, AY+1, cz-9,  cx+8, AY+1, cz-9,  BlockTypes.SANDSTONE);
            fill(es, cx-8, AY+1, cz+9,  cx+8, AY+1, cz+9,  BlockTypes.SANDSTONE);
            fill(es, cx-9, AY+1, cz-8,  cx-9, AY+1, cz+8,  BlockTypes.SANDSTONE);
            fill(es, cx+9, AY+1, cz-8,  cx+9, AY+1, cz+8,  BlockTypes.SANDSTONE);
            // 4 sandstone pillar arches r=28
            fill(es, cx-4, AY+1, cz-28, cx-4, AY+7, cz-28, BlockTypes.SANDSTONE);
            fill(es, cx+4, AY+1, cz-28, cx+4, AY+7, cz-28, BlockTypes.SANDSTONE);
            fill(es, cx-4, AY+7, cz-28, cx+4, AY+7, cz-28, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx-4, AY+1, cz+28, cx-4, AY+7, cz+28, BlockTypes.SANDSTONE);
            fill(es, cx+4, AY+1, cz+28, cx+4, AY+7, cz+28, BlockTypes.SANDSTONE);
            fill(es, cx-4, AY+7, cz+28, cx+4, AY+7, cz+28, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx-28, AY+1, cz-4, cx-28, AY+7, cz-4, BlockTypes.SANDSTONE);
            fill(es, cx-28, AY+1, cz+4, cx-28, AY+7, cz+4, BlockTypes.SANDSTONE);
            fill(es, cx-28, AY+7, cz-4, cx-28, AY+7, cz+4, BlockTypes.CHISELED_SANDSTONE);
            fill(es, cx+28, AY+1, cz-4, cx+28, AY+7, cz-4, BlockTypes.SANDSTONE);
            fill(es, cx+28, AY+1, cz+4, cx+28, AY+7, cz+4, BlockTypes.SANDSTONE);
            fill(es, cx+28, AY+7, cz-4, cx+28, AY+7, cz+4, BlockTypes.CHISELED_SANDSTONE);
            // LIGHTHOUSE (NE at cx+32, cz-32, 24 tall)
            fill(es, cx+30, AY+1, cz-34, cx+34, AY+3, cz-30, BlockTypes.SANDSTONE);
            fill(es, cx+31, AY+4,  cz-33, cx+33, AY+14, cz-31, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx+31, AY+15, cz-33, cx+33, AY+17, cz-31, BlockTypes.CUT_SANDSTONE);
            // Shaft windows
            block(es, cx+32, AY+6,  cz-33, BlockTypes.AIR);
            block(es, cx+32, AY+10, cz-33, BlockTypes.AIR);
            block(es, cx+32, AY+6,  cz-31, BlockTypes.AIR);
            block(es, cx+32, AY+10, cz-31, BlockTypes.AIR);
            block(es, cx+31, AY+6,  cz-32, BlockTypes.AIR);
            block(es, cx+31, AY+10, cz-32, BlockTypes.AIR);
            block(es, cx+33, AY+6,  cz-32, BlockTypes.AIR);
            block(es, cx+33, AY+10, cz-32, BlockTypes.AIR);
            // Balcony
            fill(es, cx+30, AY+18, cz-34, cx+34, AY+18, cz-30, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx+31, AY+18, cz-33, cx+33, AY+18, cz-31, BlockTypes.AIR);
            fill(es, cx+30, AY+19, cz-34, cx+34, AY+19, cz-34, BlockTypes.OAK_FENCE);
            fill(es, cx+30, AY+19, cz-30, cx+34, AY+19, cz-30, BlockTypes.OAK_FENCE);
            fill(es, cx+30, AY+19, cz-34, cx+30, AY+19, cz-30, BlockTypes.OAK_FENCE);
            fill(es, cx+34, AY+19, cz-34, cx+34, AY+19, cz-30, BlockTypes.OAK_FENCE);
            // Lantern housing
            fill(es, cx+31, AY+20, cz-33, cx+33, AY+22, cz-31, BlockTypes.SMOOTH_SANDSTONE);
            block(es, cx+32, AY+23, cz-32, BlockTypes.GLOWSTONE);
            block(es, cx+32, AY+24, cz-32, BlockTypes.SEA_LANTERN);
            // Staircase on outside of lighthouse
            for (int s = 0; s <= 18; s++)
                block(es, cx+30, AY+1+s, cz-34-1, BlockTypes.STONE_BRICKS);
            // 6 BEACH HUTS
            int[][] hutPos = {
                {cx-38, cz-15}, {cx-38, cz+15},
                {cx+36, cz-15}, {cx+36, cz+15},
                {cx-10, cz-40}, {cx+8,  cz+38}
            };
            for (int[] hp : hutPos) {
                fill(es, hp[0]-2, AY, hp[1]-2, hp[0]+2, AY, hp[1]+2, BlockTypes.SANDSTONE);
                fillHollow(es, hp[0]-2, AY+1, hp[1]-2, hp[0]+2, AY+4, hp[1]+2,
                        BlockTypes.SANDSTONE, BlockTypes.AIR);
                fill(es, hp[0]-3, AY+5, hp[1]-3, hp[0]+3, AY+5, hp[1]+3, BlockTypes.CUT_SANDSTONE);
                fill(es, hp[0]-2, AY+6, hp[1], hp[0]+2, AY+6, hp[1], BlockTypes.SMOOTH_SANDSTONE);
                fill(es, hp[0], AY+1, hp[1]-2, hp[0], AY+3, hp[1]-2, BlockTypes.AIR);
                block(es, hp[0]-2, AY+2, hp[1], BlockTypes.AIR);
                block(es, hp[0]+2, AY+2, hp[1], BlockTypes.AIR);
                block(es, hp[0], AY+4, hp[1], BlockTypes.LANTERN);
            }
            // SANDCASTLE (4-tier pyramid at cx-20, cz+35)
            fill(es, cx-23, AY+1, cz+32, cx-17, AY+1, cz+38, BlockTypes.SAND);
            fill(es, cx-22, AY+2, cz+33, cx-18, AY+2, cz+37, BlockTypes.SAND);
            fill(es, cx-21, AY+3, cz+34, cx-19, AY+3, cz+36, BlockTypes.SAND);
            block(es, cx-20, AY+4, cz+35, BlockTypes.SAND);
            fill(es, cx-24, AY-1, cz+31, cx-16, AY-1, cz+39, BlockTypes.STONE);
            fill(es, cx-24, AY,   cz+31, cx-16, AY,   cz+39, BlockTypes.WATER);
            fill(es, cx-22, AY,   cz+33, cx-18, AY,   cz+37, BlockTypes.SAND);
            // SHIPWRECK HULL
            fill(es, cx+15, AY-1, cz+18, cx+42, AY,   cz+18, BlockTypes.OAK_LOG);
            fill(es, cx+15, AY,   cz+20, cx+42, AY+1, cz+20, BlockTypes.OAK_PLANKS);
            fill(es, cx+15, AY,   cz+22, cx+42, AY+1, cz+22, BlockTypes.OAK_PLANKS);
            fill(es, cx+15, AY,   cz+24, cx+42, AY,   cz+24, BlockTypes.OAK_PLANKS);
            fill(es, cx+15, AY+1, cz+18, cx+15, AY+4, cz+24, BlockTypes.OAK_LOG);
            fill(es, cx+42, AY+1, cz+18, cx+42, AY+4, cz+24, BlockTypes.OAK_LOG);
            fill(es, cx+15, AY+5, cz+18, cx+15, AY+5, cz+24, BlockTypes.OAK_LOG);
            fill(es, cx+42, AY+5, cz+18, cx+42, AY+5, cz+24, BlockTypes.OAK_LOG);
            column(es, cx+28, cz+20, AY+2, AY+14, BlockTypes.OAK_LOG);
            fill(es, cx+25, AY+8,  cz+20, cx+31, AY+8,  cz+20, BlockTypes.OAK_PLANKS);
            fill(es, cx+25, AY+12, cz+20, cx+31, AY+12, cz+20, BlockTypes.OAK_PLANKS);
            // 24 PALM TREES
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                int r  = 32 + (i % 4) * 5;
                int tx = cx + (int)Math.round(r * Math.cos(a));
                int tz = cz + (int)Math.round(r * Math.sin(a));
                if (Math.abs(tx-cx) > 48 || Math.abs(tz-cz) > 48) continue;
                int h = AY + 7 + (i % 3);
                column(es, tx, tz, AY+1, h, BlockTypes.JUNGLE_LOG);
                fill(es, tx-2, h, tz-2, tx+2, h+1, tz+2, BlockTypes.JUNGLE_LEAVES);
                fill(es, tx-1, h+2, tz-1, tx+1, h+2, tz+1, BlockTypes.JUNGLE_LEAVES);
                block(es, tx, h+3, tz, BlockTypes.JUNGLE_LEAVES);
            }
            // CORAL in moat
            com.sk89q.worldedit.world.block.BlockType[] corals = {
                BlockTypes.BRAIN_CORAL_BLOCK, BlockTypes.TUBE_CORAL_BLOCK,
                BlockTypes.HORN_CORAL_BLOCK,  BlockTypes.BUBBLE_CORAL_BLOCK,
                BlockTypes.FIRE_CORAL_BLOCK
            };
            for (int i = 0; i < 25; i++) {
                int rx = cx - 63 + (i * 11 + 5) % 127;
                int rz = cz - 63 + (i *  7 + 9) % 127;
                if (Math.abs(rx-cx) >= 50 || Math.abs(rz-cz) >= 50)
                    block(es, rx, 62, rz, corals[i % corals.length]);
            }
            // Dead bush / grass scatter
            for (int i = 0; i < 40; i++) {
                int sx = cx - 48 + (i * 13 + 7) % 97;
                int sz = cz - 48 + (i * 11 + 3) % 97;
                block(es, sx, AY+1, sz, (i % 3 == 0) ? BlockTypes.DEAD_BUSH : BlockTypes.SHORT_GRASS);
            }
            // Sand wall around island (cx±50)
            for (int y = AY+1; y <= AY+5; y++) {
                fill(es, cx-50, y, cz-50, cx+50, y, cz-50, BlockTypes.SANDSTONE);
                fill(es, cx-50, y, cz+50, cx+50, y, cz+50, BlockTypes.SANDSTONE);
                fill(es, cx-50, y, cz-50, cx-50, y, cz+50, BlockTypes.SANDSTONE);
                fill(es, cx+50, y, cz-50, cx+50, y, cz+50, BlockTypes.SANDSTONE);
            }
            // Sandstone merlons
            for (int wx = cx-50; wx <= cx+50; wx += 4) {
                block(es, wx, AY+6, cz-50, BlockTypes.SMOOTH_SANDSTONE);
                block(es, wx, AY+6, cz+50, BlockTypes.SMOOTH_SANDSTONE);
            }
            for (int wz = cz-50; wz <= cz+50; wz += 4) {
                block(es, cx-50, AY+6, wz, BlockTypes.SMOOTH_SANDSTONE);
                block(es, cx+50, AY+6, wz, BlockTypes.SMOOTH_SANDSTONE);
            }
            // 4 gateway openings in sand wall
            fill(es, cx-2, AY+1, cz-50, cx+2, AY+5, cz-50, BlockTypes.AIR);
            fill(es, cx-2, AY+1, cz+50, cx+2, AY+5, cz+50, BlockTypes.AIR);
            fill(es, cx-50, AY+1, cz-2, cx-50, AY+5, cz+2, BlockTypes.AIR);
            fill(es, cx+50, AY+1, cz-2, cx+50, AY+5, cz+2, BlockTypes.AIR);
            // 4 sand wall corner towers
            fill(es, cx-52, AY+1, cz-52, cx-48, AY+8, cz-48, BlockTypes.SANDSTONE);
            fill(es, cx+48, AY+1, cz-52, cx+52, AY+8, cz-48, BlockTypes.SANDSTONE);
            fill(es, cx-52, AY+1, cz+48, cx-48, AY+8, cz+52, BlockTypes.SANDSTONE);
            fill(es, cx+48, AY+1, cz+48, cx+52, AY+8, cz+52, BlockTypes.SANDSTONE);
            block(es, cx-50, AY+9, cz-50, BlockTypes.GLOWSTONE);
            block(es, cx+50, AY+9, cz-50, BlockTypes.GLOWSTONE);
            block(es, cx-50, AY+9, cz+50, BlockTypes.GLOWSTONE);
            block(es, cx+50, AY+9, cz+50, BlockTypes.GLOWSTONE);
            // Glowstone pillars at r=22
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int px = cx + (int)Math.round(22 * Math.cos(a));
                int pz = cz + (int)Math.round(22 * Math.sin(a));
                fill(es, px, AY+1, pz, px+1, AY+8, pz+1, BlockTypes.SMOOTH_SANDSTONE);
                block(es, px, AY+9, pz, BlockTypes.GLOWSTONE);
            }
            // Pier dock south (into water, 15 blocks)
            fill(es, cx-1, AY+1, cz+50, cx+1, AY+1, cz+65, BlockTypes.OAK_PLANKS);
            for (int pz2 = cz+50; pz2 <= cz+65; pz2 += 4) {
                column(es, cx-1, pz2, 60, AY, BlockTypes.OAK_LOG);
                column(es, cx+1, pz2, 60, AY, BlockTypes.OAK_LOG);
            }
            // Driftwood logs on beach
            fill(es, cx-48, AY+1, cz-38, cx-41, AY+1, cz-38, BlockTypes.OAK_LOG);
            fill(es, cx+38, AY+1, cz+42, cx+45, AY+1, cz+42, BlockTypes.OAK_LOG);
            fill(es, cx-35, AY+1, cz+44, cx-28, AY+1, cz+44, BlockTypes.OAK_LOG);
            fill(es, cx+30, AY+1, cz-42, cx+37, AY+1, cz-42, BlockTypes.OAK_LOG);
            // ── EXTENDED BEACH TEXTURING ──────────────────────────────────
            // Wet sand near waterline (darker patches)
            fill(es, cx-60, AY, cz-45, cx+60, AY, cz-38, BlockTypes.RED_SAND);
            fill(es, cx-60, AY, cz+38, cx+60, AY, cz+45, BlockTypes.RED_SAND);
            // Dried kelp seaweed patches on sand
            for (int i = 0; i < 50; i++) {
                int sx = cx - 55 + (i * 19 + 7) % 111;
                int sz = cz - 55 + (i * 13 + 5) % 111;
                block(es, sx, AY+1, sz, BlockTypes.DEAD_BUSH);
            }
            // Stone pebbles (gravel micro-patches)
            for (int i = 0; i < 80; i++) {
                int sx = cx - 60 + (i * 23 + 11) % 121;
                int sz = cz - 60 + (i * 17 + 7)  % 121;
                if (i % 2 == 0) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else            block(es, sx, AY, sz, BlockTypes.STONE);
            }

            // ── SANDSTONE TIDE POOLS ──────────────────────────────────────
            fill(es, cx-55, 63, cz-20, cx-42, 63, cz- 8, BlockTypes.SAND);
            fill(es, cx-55, AY, cz-20, cx-42, AY, cz- 8, BlockTypes.WATER);
            block(es, cx-52, AY, cz-16, BlockTypes.BRAIN_CORAL_BLOCK);
            block(es, cx-50, AY, cz-14, BlockTypes.TUBE_CORAL_BLOCK);
            fill(es, cx+42, 63, cz+ 8, cx+55, 63, cz+20, BlockTypes.SAND);
            fill(es, cx+42, AY, cz+ 8, cx+55, AY, cz+20, BlockTypes.WATER);
            block(es, cx+45, AY, cz+12, BlockTypes.FIRE_CORAL_BLOCK);

            // ── SAND DUNE ROWS ────────────────────────────────────────────
            for (int row = 0; row <= 3; row++) {
                int yOff = row / 2;
                fill(es, cx-62+row*2, AY+yOff, cz-62+row*2,
                        cx+62-row*2, AY+yOff, cz-60+row*2, BlockTypes.SAND);
                fill(es, cx-62+row*2, AY+yOff, cz+60-row*2,
                        cx+62-row*2, AY+yOff, cz+62-row*2, BlockTypes.SAND);
            }

            // ── SEA GLASS (light blue/cyan terracotta inlay) ──────────────
            fill(es, cx-12, AY, cz-30, cx+12, AY, cz-22, BlockTypes.CYAN_TERRACOTTA);
            fill(es, cx-12, AY, cz+22, cx+12, AY, cz+30, BlockTypes.CYAN_TERRACOTTA);
            fill(es, cx+22, AY, cz-12, cx+30, AY, cz+12, BlockTypes.CYAN_TERRACOTTA);
            fill(es, cx-30, AY, cz-12, cx-22, AY, cz+12, BlockTypes.CYAN_TERRACOTTA);

            // ── TROPICAL FLOWER CARPET ────────────────────────────────────
            for (int i = 0; i < 100; i++) {
                int fx = cx - 55 + (i * 31 + 13) % 111;
                int fz = cz - 55 + (i * 29 + 11) % 111;
                int r = i % 5;
                if      (r == 0) block(es, fx, AY+1, fz, BlockTypes.DANDELION);
                else if (r == 1) block(es, fx, AY+1, fz, BlockTypes.POPPY);
                else if (r == 2) block(es, fx, AY+1, fz, BlockTypes.ORANGE_TULIP);
                else if (r == 3) block(es, fx, AY+1, fz, BlockTypes.PINK_TULIP);
                else             block(es, fx, AY+1, fz, BlockTypes.SHORT_GRASS);
            }

            // ── THIRD SCATTER PASS (granular beach texture) ───────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 60 + (i * 67 + 43) % 121;
                int sz = cz - 60 + (i * 61 + 41) % 121;
                int r  = i % 8;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.SMOOTH_SANDSTONE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.RED_SANDSTONE);
                else             block(es, sx, AY, sz, BlockTypes.CYAN_TERRACOTTA);
            }

            // ── 4 SPAWN PADS ──────────────────────────────────────────────
            fill(es, cx-1, AY, cz-38, cx+1, AY, cz-36, BlockTypes.SANDSTONE);
            fill(es, cx-1, AY, cz+36, cx+1, AY, cz+38, BlockTypes.SANDSTONE);
            fill(es, cx-38, AY, cz-1, cx-36, AY, cz+1, BlockTypes.SANDSTONE);
            fill(es, cx+36, AY, cz-1, cx+38, AY, cz+1, BlockTypes.SANDSTONE);
        }
    }

    // =========================================================================
    // ARENA 3 — BAMBOO FOREST   cx=0, cz=300   (130x130)
    // =========================================================================
    private void buildBambooArena() {
        final int cx = 0, cz = 300;
        try (EditSession es = newSession()) {
            // Ground base
            fill(es, cx-65, 59, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);
            // Moss block patches scattered
            fill(es, cx-50, AY, cz+30, cx-46, AY, cz+34, BlockTypes.MOSS_BLOCK);
            fill(es, cx-30, AY, cz-50, cx-26, AY, cz-46, BlockTypes.MOSS_BLOCK);
            fill(es, cx-55, AY, cz+10, cx-51, AY, cz+14, BlockTypes.MOSS_BLOCK);
            fill(es, cx+40, AY, cz-40, cx+44, AY, cz-36, BlockTypes.MOSS_BLOCK);
            fill(es, cx+55, AY, cz+30, cx+59, AY, cz+34, BlockTypes.MOSS_BLOCK);
            fill(es, cx-20, AY, cz+55, cx-16, AY, cz+59, BlockTypes.MOSS_BLOCK);
            fill(es, cx+20, AY, cz-55, cx+24, AY, cz-51, BlockTypes.MOSS_BLOCK);
            fill(es, cx-45, AY, cz-45, cx-41, AY, cz-41, BlockTypes.MOSS_BLOCK);
            fill(es, cx+45, AY, cz+45, cx+49, AY, cz+49, BlockTypes.MOSS_BLOCK);
            fill(es, cx-10, AY, cz-60, cx-6,  AY, cz-56, BlockTypes.MOSS_BLOCK);
            fill(es, cx+10, AY, cz+58, cx+14, AY, cz+62, BlockTypes.MOSS_BLOCK);
            fill(es, cx+50, AY, cz-10, cx+54, AY, cz-6,  BlockTypes.MOSS_BLOCK);
            // Main bamboo planks paths (3 wide)
            fill(es, cx-1, AY, cz-65, cx+1, AY, cz-14, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-1, AY, cz+14, cx+1, AY, cz+65, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-65, AY, cz-1, cx-14, AY, cz+1, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx+14, AY, cz-1, cx+65, AY, cz+1, BlockTypes.BAMBOO_PLANKS);
            // Central clearing floor
            fill(es, cx-13, AY, cz-13, cx+13, AY, cz+13, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-13, AY, cz-13, cx+13, AY, cz-12, BlockTypes.STONE_BRICKS);
            fill(es, cx-13, AY, cz+12, cx+13, AY, cz+13, BlockTypes.STONE_BRICKS);
            fill(es, cx-13, AY, cz-12, cx-12, AY, cz+12, BlockTypes.STONE_BRICKS);
            fill(es, cx+12, AY, cz-12, cx+13, AY, cz+12, BlockTypes.STONE_BRICKS);
            // 3-STORY PAGODA
            // Level 1: 12×12 hollow
            fillHollow(es, cx-6, AY+1, cz-6, cx+6, AY+4, cz+6,
                    BlockTypes.CHERRY_LOG, BlockTypes.AIR);
            // Cherry leaf roof overhang level 1
            fill(es, cx-7, AY+5, cz-7, cx+7, AY+5, cz+7, BlockTypes.CHERRY_LEAVES);
            fill(es, cx-6, AY+6, cz-6, cx+6, AY+6, cz+6, BlockTypes.CHERRY_LEAVES);
            // Level 2: 8×8 hollow on top
            fillHollow(es, cx-4, AY+6, cz-4, cx+4, AY+9, cz+4,
                    BlockTypes.BAMBOO_BLOCK, BlockTypes.AIR);
            fill(es, cx-5, AY+10, cz-5, cx+5, AY+10, cz+5, BlockTypes.CHERRY_LEAVES);
            fill(es, cx-4, AY+11, cz-4, cx+4, AY+11, cz+4, BlockTypes.CHERRY_LEAVES);
            // Level 3: 4×4 hollow on top
            fillHollow(es, cx-2, AY+11, cz-2, cx+2, AY+14, cz+2,
                    BlockTypes.CHERRY_LOG, BlockTypes.AIR);
            fill(es, cx-3, AY+15, cz-3, cx+3, AY+15, cz+3, BlockTypes.CHERRY_LEAVES);
            fill(es, cx-2, AY+16, cz-2, cx+2, AY+16, cz+2, BlockTypes.CHERRY_LEAVES);
            fill(es, cx-1, AY+17, cz-1, cx+1, AY+17, cz+1, BlockTypes.CHERRY_LEAVES);
            block(es, cx, AY+18, cz, BlockTypes.LANTERN);
            // Pagoda interior floor and lighting
            fill(es, cx-5, AY, cz-5, cx+5, AY, cz+5, BlockTypes.BAMBOO_PLANKS);
            block(es, cx-4, AY, cz-4, BlockTypes.SHROOMLIGHT);
            block(es, cx+4, AY, cz-4, BlockTypes.SHROOMLIGHT);
            block(es, cx-4, AY, cz+4, BlockTypes.SHROOMLIGHT);
            block(es, cx+4, AY, cz+4, BlockTypes.SHROOMLIGHT);
            // 4 gateway openings in pagoda level 1
            fill(es, cx-1, AY+1, cz-6, cx+1, AY+4, cz-6, BlockTypes.AIR);
            fill(es, cx-1, AY+1, cz+6, cx+1, AY+4, cz+6, BlockTypes.AIR);
            fill(es, cx-6, AY+1, cz-1, cx-6, AY+4, cz+1, BlockTypes.AIR);
            fill(es, cx+6, AY+1, cz-1, cx+6, AY+4, cz+1, BlockTypes.AIR);
            // 4 corner bamboo columns on pagoda exterior
            column(es, cx-7, cz-7, AY+1, AY+5, BlockTypes.BAMBOO_BLOCK);
            column(es, cx+7, cz-7, AY+1, AY+5, BlockTypes.BAMBOO_BLOCK);
            column(es, cx-7, cz+7, AY+1, AY+5, BlockTypes.BAMBOO_BLOCK);
            column(es, cx+7, cz+7, AY+1, AY+5, BlockTypes.BAMBOO_BLOCK);
            // KOI POND (east of center)
            fill(es, cx+18, AY-1, cz-6, cx+34, AY-1, cz+6, BlockTypes.STONE);
            fill(es, cx+18, AY,   cz-6, cx+34, AY,   cz+6, BlockTypes.WATER);
            fill(es, cx+17, AY+1, cz-7, cx+35, AY+1, cz-7, BlockTypes.STONE_BRICKS);
            fill(es, cx+17, AY+1, cz+7, cx+35, AY+1, cz+7, BlockTypes.STONE_BRICKS);
            fill(es, cx+17, AY+1, cz-7, cx+17, AY+1, cz+7, BlockTypes.STONE_BRICKS);
            fill(es, cx+35, AY+1, cz-7, cx+35, AY+1, cz+7, BlockTypes.STONE_BRICKS);
            // Koi pond lily pads
            block(es, cx+20, AY, cz-3, BlockTypes.LILY_PAD);
            block(es, cx+23, AY, cz+2, BlockTypes.LILY_PAD);
            block(es, cx+26, AY, cz-4, BlockTypes.LILY_PAD);
            block(es, cx+29, AY, cz+3, BlockTypes.LILY_PAD);
            block(es, cx+32, AY, cz-1, BlockTypes.LILY_PAD);
            // Corner lanterns on koi pond
            block(es, cx+17, AY+2, cz-7, BlockTypes.LANTERN);
            block(es, cx+35, AY+2, cz-7, BlockTypes.LANTERN);
            block(es, cx+17, AY+2, cz+7, BlockTypes.LANTERN);
            block(es, cx+35, AY+2, cz+7, BlockTypes.LANTERN);
            // STONE BRIDGE over koi pond
            fill(es, cx+17, AY+2, cz-1, cx+35, AY+2, cz+1, BlockTypes.STONE_BRICKS);
            for (int bx = cx+17; bx <= cx+35; bx += 3) {
                block(es, bx, AY+3, cz-1, BlockTypes.OAK_FENCE);
                block(es, bx, AY+3, cz+1, BlockTypes.OAK_FENCE);
            }
            // ZEN GARDEN (west of center, sand + stone lines)
            fill(es, cx-34, AY, cz-8, cx-18, AY, cz+8, BlockTypes.SAND);
            for (int zx = cx-33; zx <= cx-19; zx += 3)
                fill(es, zx, AY+1, cz-7, zx, AY+1, cz+7, BlockTypes.STONE);
            fill(es, cx-34, AY+1, cz-8, cx-18, AY+1, cz-8, BlockTypes.STONE_BRICKS);
            fill(es, cx-34, AY+1, cz+8, cx-18, AY+1, cz+8, BlockTypes.STONE_BRICKS);
            fill(es, cx-34, AY+1, cz-8, cx-34, AY+1, cz+8, BlockTypes.STONE_BRICKS);
            fill(es, cx-18, AY+1, cz-8, cx-18, AY+1, cz+8, BlockTypes.STONE_BRICKS);
            // Stepping stones in zen garden
            block(es, cx-28, AY+1, cz, BlockTypes.STONE_BRICKS);
            block(es, cx-25, AY+1, cz, BlockTypes.STONE_BRICKS);
            block(es, cx-22, AY+1, cz, BlockTypes.STONE_BRICKS);
            // DENSE BAMBOO in outer ring
            for (int bx = cx-62; bx <= cx+62; bx += 2) {
                for (int bz2 = cz-62; bz2 <= cz+62; bz2 += 2) {
                    if (Math.abs(bx-cx) <= 14 && Math.abs(bz2-cz) <= 14) continue;
                    if (Math.abs(bx-cx) <= 2 || Math.abs(bz2-cz) <= 2) continue;
                    if (Math.abs(bx-cx) >= 18 && Math.abs(bx-cx) <= 35 && Math.abs(bz2-cz) <= 8) continue;
                    int h = 4 + ((bx + bz2 + 200) % 5);
                    column(es, bx, bz2, AY+1, AY+h, BlockTypes.BAMBOO);
                }
            }
            // 8 CHERRY TREES in mid-ring at r=35
            for (int i = 0; i < 8; i++) {
                double a = (i + 0.5) * Math.PI / 4;
                int tx = cx + (int)Math.round(38 * Math.cos(a));
                int tz = cz + (int)Math.round(38 * Math.sin(a));
                if (Math.abs(tx-cx) > 62 || Math.abs(tz-cz) > 62) continue;
                int h = AY + 5 + (i % 4);
                column(es, tx, tz, AY+1, h, BlockTypes.CHERRY_LOG);
                fill(es, tx-2, h,   tz-2, tx+2, h+1, tz+2, BlockTypes.CHERRY_LEAVES);
                fill(es, tx-1, h+2, tz-1, tx+1, h+2, tz+1, BlockTypes.CHERRY_LEAVES);
                block(es, tx, h+3, tz, BlockTypes.CHERRY_LEAVES);
            }
            // 24 LANTERN POSTS on paths (every 10 blocks)
            for (int off : new int[]{-50,-40,-30,-20,20,30,40,50}) {
                column(es, cx, cz+off, AY+1, AY+3, BlockTypes.BAMBOO_FENCE);
                block(es, cx, AY+4, cz+off, BlockTypes.LANTERN);
                column(es, cx+off, cz, AY+1, AY+3, BlockTypes.BAMBOO_FENCE);
                block(es, cx+off, AY+4, cz, BlockTypes.LANTERN);
            }
            // 4 BAMBOO GAZEBOS at diagonal r=35
            int[][] gaz = {
                {cx-25, cz-25}, {cx+24, cz-25},
                {cx-25, cz+24}, {cx+24, cz+24}
            };
            for (int[] g : gaz) {
                fill(es, g[0]-2, AY+1, g[1]-2, g[0]+2, AY+1, g[1]+2, BlockTypes.BAMBOO_PLANKS);
                column(es, g[0]-2, g[1]-2, AY+2, AY+5, BlockTypes.BAMBOO_BLOCK);
                column(es, g[0]+2, g[1]-2, AY+2, AY+5, BlockTypes.BAMBOO_BLOCK);
                column(es, g[0]-2, g[1]+2, AY+2, AY+5, BlockTypes.BAMBOO_BLOCK);
                column(es, g[0]+2, g[1]+2, AY+2, AY+5, BlockTypes.BAMBOO_BLOCK);
                fill(es, g[0]-2, AY+5, g[1]-2, g[0]+2, AY+5, g[1]+2, BlockTypes.BAMBOO_PLANKS);
                block(es, g[0], AY+6, g[1], BlockTypes.LANTERN);
            }
            // BAMBOO BLOCK PERIMETER WALL (6 tall)
            for (int y = AY+1; y <= AY+6; y++) {
                fill(es, cx-63, y, cz-63, cx+63, y, cz-63, BlockTypes.BAMBOO_BLOCK);
                fill(es, cx-63, y, cz+63, cx+63, y, cz+63, BlockTypes.BAMBOO_BLOCK);
                fill(es, cx-63, y, cz-63, cx-63, y, cz+63, BlockTypes.BAMBOO_BLOCK);
                fill(es, cx+63, y, cz-63, cx+63, y, cz+63, BlockTypes.BAMBOO_BLOCK);
            }
            // Moss patches on wall
            for (int wx = cx-62; wx <= cx+62; wx += 7) {
                fill(es, wx, AY+2, cz-63, wx+1, AY+5, cz-63, BlockTypes.MOSS_BLOCK);
                fill(es, wx, AY+2, cz+63, wx+1, AY+5, cz+63, BlockTypes.MOSS_BLOCK);
            }
            for (int wz = cz-62; wz <= cz+62; wz += 7) {
                fill(es, cx-63, AY+2, wz, cx-63, AY+5, wz+1, BlockTypes.MOSS_BLOCK);
                fill(es, cx+63, AY+2, wz, cx+63, AY+5, wz+1, BlockTypes.MOSS_BLOCK);
            }
            // Gate openings
            fill(es, cx-2, AY+1, cz-63, cx+2, AY+6, cz-63, BlockTypes.AIR);
            fill(es, cx-2, AY+1, cz+63, cx+2, AY+6, cz+63, BlockTypes.AIR);
            fill(es, cx-63, AY+1, cz-2, cx-63, AY+6, cz+2, BlockTypes.AIR);
            fill(es, cx+63, AY+1, cz-2, cx+63, AY+6, cz+2, BlockTypes.AIR);
            // Wall cap merlons
            for (int wx = cx-63; wx <= cx+63; wx += 4) {
                block(es, wx, AY+7, cz-63, BlockTypes.BAMBOO_BLOCK);
                block(es, wx, AY+7, cz+63, BlockTypes.BAMBOO_BLOCK);
            }
            for (int wz = cz-63; wz <= cz+63; wz += 4) {
                block(es, cx-63, AY+7, wz, BlockTypes.BAMBOO_BLOCK);
                block(es, cx+63, AY+7, wz, BlockTypes.BAMBOO_BLOCK);
            }
            // VEGETATION
            for (int i = 0; i < 40; i++) {
                int sx = cx - 60 + (i * 19 + 7) % 121;
                int sz = cz - 60 + (i * 13 + 5) % 121;
                block(es, sx, AY+1, sz, (i % 3 == 0) ? BlockTypes.FERN : BlockTypes.SHORT_GRASS);
            }
            // ── EXTENDED BAMBOO FOREST DETAIL ────────────────────────────
            // Dense bamboo grove: more stalks scattered
            for (int i = 0; i < 80; i++) {
                int bx = cx - 60 + (i * 23 + 7) % 121;
                int bz = cz - 60 + (i * 19 + 5) % 121;
                int bh = 4 + (i % 5);
                column(es, bx, bz, AY+1, AY+bh, BlockTypes.BAMBOO_BLOCK);
            }

            // ── CHERRY BLOSSOM PATCHES ────────────────────────────────────
            int[][] cherries = {
                {cx-48,cz+48},{cx+48,cz-48},{cx-48,cz-48},{cx+48,cz+48},
                {cx-52,cz+10},{cx+52,cz-10},{cx-10,cz-52},{cx+10,cz+52}
            };
            for (int[] c : cherries) {
                int chx = c[0], chz = c[1];
                column(es, chx, chz, AY+1, AY+6, BlockTypes.CHERRY_LOG);
                disk(es, chx, AY+7, chz, 4, BlockTypes.CHERRY_LEAVES);
                disk(es, chx, AY+8, chz, 3, BlockTypes.CHERRY_LEAVES);
                disk(es, chx, AY+9, chz, 2, BlockTypes.CHERRY_LEAVES);
                block(es, chx, AY+10, chz, BlockTypes.CHERRY_LEAVES);
            }

            // ── ZEN GARDEN RAKED GRAVEL PATTERNS ─────────────────────────
            for (int gr = -25; gr <= 25; gr += 3) {
                fill(es, cx-25, AY+1, cz+gr, cx+25, AY+1, cz+gr, BlockTypes.GRAVEL);
            }
            // Circular ring on the gravel
            for (int dr = 8; dr <= 22; dr += 7) {
                for (double a = 0; a < 2*Math.PI; a += 0.3) {
                    int ix = cx + (int)Math.round(dr * Math.cos(a));
                    int iz = cz + (int)Math.round(dr * Math.sin(a));
                    block(es, ix, AY+1, iz, BlockTypes.STONE);
                }
            }

            // ── ADDITIONAL WATER FEATURES (koi pool) ─────────────────────
            fill(es, cx+30, 63, cz-15, cx+45, 63, cz, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+30, AY, cz-15, cx+45, AY, cz, BlockTypes.WATER);
            // Lily pad markers (stone for visual contrast)
            for (int lp = 0; lp < 6; lp++) {
                int lpx = cx+30 + (lp * 5 + 2) % 15;
                int lpz = cz-15 + (lp * 7 + 3) % 15;
                block(es, lpx, AY+1, lpz, BlockTypes.MOSS_BLOCK);
            }
            // Second pool W side
            fill(es, cx-45, 63, cz, cx-30, 63, cz+15, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-45, AY, cz, cx-30, AY, cz+15, BlockTypes.WATER);

            // ── COBBLESTONE STONE LANTERN PATH ───────────────────────────
            for (int step = -45; step <= 45; step += 6) {
                block(es, cx, AY+1, cz+step, BlockTypes.STONE_BRICKS);
                column(es, cx+1, cz+step, AY+1, AY+2, BlockTypes.OAK_FENCE);
                block(es, cx+1, AY+3, cz+step, BlockTypes.LANTERN);
            }

            // ── GROUND TEXTURE (third scatter pass) ───────────────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 60 + (i * 71 + 47) % 121;
                int sz = cz - 60 + (i * 67 + 43) % 121;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.BAMBOO_PLANKS);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.BAMBOO_MOSAIC);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else             block(es, sx, AY, sz, BlockTypes.GRAVEL);
            }

            // ── BAMBOO PLANK PAVILION FLOOR ───────────────────────────────
            fill(es, cx-15, AY+1, cz-15, cx+15, AY+1, cz+15, BlockTypes.BAMBOO_PLANKS);
            // Mosaic border
            for (int d = -15; d <= 15; d++) {
                block(es, cx+d, AY+1, cz-15, BlockTypes.BAMBOO_MOSAIC);
                block(es, cx+d, AY+1, cz+15, BlockTypes.BAMBOO_MOSAIC);
                block(es, cx-15, AY+1, cz+d, BlockTypes.BAMBOO_MOSAIC);
                block(es, cx+15, AY+1, cz+d, BlockTypes.BAMBOO_MOSAIC);
            }

            // ── FOURTH SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 260; i++) {
                int sx = cx - 60 + (i * 79 + 53) % 121;
                int sz = cz - 60 + (i * 73 + 47) % 121;
                int r  = i % 7;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.BAMBOO_MOSAIC);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.BAMBOO_PLANKS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CHERRY_PLANKS);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else             block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
            }

            // 4 SPAWN PADS
            fill(es, cx-1, AY, cz-42, cx+1, AY, cz-40, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-1, AY, cz+40, cx+1, AY, cz+42, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx-42, AY, cz-1, cx-40, AY, cz+1, BlockTypes.BAMBOO_PLANKS);
            fill(es, cx+40, AY, cz-1, cx+42, AY, cz+1, BlockTypes.BAMBOO_PLANKS);
        }
    }

    // =========================================================================
    // ARENA 4 — MANGROVE SWAMP   cx=0, cz=-300   (130x130)
    // Richly textured swamp ground: mud, muddy mangrove roots, clay, gravel,
    // moss, coarse dirt, water pools — plus dense mangrove canopy
    // =========================================================================
    private void buildMangroveArena() {
        final int cx = 0, cz = -300;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-65, 58, cz-65, cx+65, 63, cz+65, BlockTypes.DIRT);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.MUD);

            // ── MUDDY MANGROVE ROOT zones ────────────────────────────────
            fill(es, cx-32, AY, cz-32, cx-12, AY, cz-12, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+ 8, AY, cz+12, cx+28, AY, cz+32, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-22, AY, cz+18, cx- 4, AY, cz+38, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+32, AY, cz-42, cx+52, AY, cz-22, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-52, AY, cz+ 8, cx-36, AY, cz+28, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-15, AY, cz-55, cx+ 5, AY, cz-40, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+38, AY, cz+38, cx+55, AY, cz+55, BlockTypes.MUDDY_MANGROVE_ROOTS);

            // ── MOSS BLOCK patches ────────────────────────────────────────
            fill(es, cx-62, AY, cz-62, cx-48, AY, cz-48, BlockTypes.MOSS_BLOCK);
            fill(es, cx+48, AY, cz+48, cx+62, AY, cz+62, BlockTypes.MOSS_BLOCK);
            fill(es, cx-38, AY, cz+42, cx-20, AY, cz+58, BlockTypes.MOSS_BLOCK);
            fill(es, cx+20, AY, cz-58, cx+38, AY, cz-42, BlockTypes.MOSS_BLOCK);
            fill(es, cx+ 2, AY, cz-65, cx+18, AY, cz-52, BlockTypes.MOSS_BLOCK);
            fill(es, cx-18, AY, cz+52, cx- 2, AY, cz+65, BlockTypes.MOSS_BLOCK);

            // ── CLAY patches (shore of water pools) ──────────────────────
            fill(es, cx-28, AY, cz+ 2, cx-12, AY, cz+18, BlockTypes.CLAY);
            fill(es, cx+12, AY, cz-18, cx+28, AY, cz- 2, BlockTypes.CLAY);
            fill(es, cx-62, AY, cz-30, cx-46, AY, cz-14, BlockTypes.CLAY);
            fill(es, cx+46, AY, cz+14, cx+62, AY, cz+30, BlockTypes.CLAY);
            fill(es, cx- 8, AY, cz+55, cx+ 8, AY, cz+65, BlockTypes.CLAY);
            fill(es, cx- 8, AY, cz-65, cx+ 8, AY, cz-55, BlockTypes.CLAY);

            // ── GRAVEL shallow bottom ─────────────────────────────────────
            fill(es, cx-10, AY, cz-10, cx+10, AY, cz+10, BlockTypes.GRAVEL);  // centre
            fill(es, cx-52, AY, cz-62, cx-32, AY, cz-48, BlockTypes.GRAVEL);
            fill(es, cx+32, AY, cz+48, cx+52, AY, cz+62, BlockTypes.GRAVEL);
            fill(es, cx+42, AY, cz-18, cx+58, AY, cz- 4, BlockTypes.GRAVEL);
            fill(es, cx-58, AY, cz+ 4, cx-42, AY, cz+18, BlockTypes.GRAVEL);

            // ── COARSE DIRT elevated mounds ───────────────────────────────
            fill(es, cx-42, AY,   cz-62, cx-26, AY+1, cz-46, BlockTypes.COARSE_DIRT);
            fill(es, cx+26, AY,   cz+46, cx+42, AY+1, cz+62, BlockTypes.COARSE_DIRT);
            fill(es, cx-62, AY,   cz+26, cx-46, AY+1, cz+42, BlockTypes.COARSE_DIRT);
            fill(es, cx+46, AY,   cz-42, cx+62, AY+1, cz-26, BlockTypes.COARSE_DIRT);
            fill(es, cx-20, AY,   cz-62, cx- 4, AY+1, cz-50, BlockTypes.COARSE_DIRT);
            fill(es, cx+ 4, AY,   cz+50, cx+20, AY+1, cz+62, BlockTypes.COARSE_DIRT);

            // ── WATER POOLS with clay/gravel floors ──────────────────────
            // Central murky pool
            fill(es, cx-12, 63, cz-12, cx+12, 63, cz+12, BlockTypes.CLAY);
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz+12, BlockTypes.WATER);
            // NW pool
            fill(es, cx-54, 63, cz-62, cx-33, 63, cz-46, BlockTypes.GRAVEL);
            fill(es, cx-54, AY, cz-62, cx-33, AY, cz-46, BlockTypes.WATER);
            // SE pool
            fill(es, cx+33, 63, cz+46, cx+54, 63, cz+62, BlockTypes.GRAVEL);
            fill(es, cx+33, AY, cz+46, cx+54, AY, cz+62, BlockTypes.WATER);
            // NE pool
            fill(es, cx+42, 63, cz-20, cx+58, 63, cz- 6, BlockTypes.CLAY);
            fill(es, cx+42, AY, cz-20, cx+58, AY, cz- 6, BlockTypes.WATER);
            // SW pool
            fill(es, cx-58, 63, cz+ 6, cx-42, 63, cz+20, BlockTypes.CLAY);
            fill(es, cx-58, AY, cz+ 6, cx-42, AY, cz+20, BlockTypes.WATER);

            // ── MANGROVE ROOTS vertical (aerial root columns) ─────────────
            int[][] rootCols = {
                {cx-20,cz-20},{cx-22,cz-18},{cx-18,cz-22},{cx-24,cz-24},
                {cx+18,cz+20},{cx+20,cz+18},{cx+22,cz+22},{cx+16,cz+24},
                {cx+38,cz-32},{cx+40,cz-30},{cx+36,cz-34},{cx+42,cz-28},
                {cx-32,cz+38},{cx-30,cz+40},{cx-34,cz+36},{cx-28,cz+42},
                {cx+ 2,cz-48},{cx+ 6,cz-46},{cx- 2,cz-50},{cx+ 4,cz-44},
                {cx-48,cz+20},{cx-46,cz+22},{cx-50,cz+18},{cx-44,cz+24},
                {cx+48,cz- 4},{cx+50,cz- 2},{cx+46,cz- 6},{cx+52,cz+ 2},
                {cx- 6,cz+58},{cx- 2,cz+56},{cx+  2,cz+60},{cx- 8,cz+54}
            };
            for (int[] rc : rootCols) {
                int rx = rc[0], rz = rc[1];
                block(es, rx, AY,   rz, BlockTypes.MANGROVE_ROOTS);
                column(es, rx, rz, AY+1, AY+5, BlockTypes.MANGROVE_LOG);
                disk(es, rx, AY+6, rz, 4, BlockTypes.MANGROVE_LEAVES);
                disk(es, rx, AY+7, rz, 3, BlockTypes.MANGROVE_LEAVES);
                disk(es, rx, AY+8, rz, 2, BlockTypes.MANGROVE_LEAVES);
                block(es, rx, AY+9, rz, BlockTypes.MANGROVE_LEAVES);
                // prop roots spreading out
                block(es, rx+2, AY, rz,   BlockTypes.MANGROVE_ROOTS);
                block(es, rx-2, AY, rz,   BlockTypes.MANGROVE_ROOTS);
                block(es, rx,   AY, rz+2, BlockTypes.MANGROVE_ROOTS);
                block(es, rx,   AY, rz-2, BlockTypes.MANGROVE_ROOTS);
                block(es, rx+1, AY, rz+1, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx-1, AY, rz-1, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx+1, AY, rz-1, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx-1, AY, rz+1, BlockTypes.MUDDY_MANGROVE_ROOTS);
            }

            // ── SCATTERED SURFACE DETAIL (200 random-ish blocks) ──────────
            for (int i = 0; i < 240; i++) {
                int sx = cx - 60 + (i * 31 + 7) % 121;
                int sz = cz - 60 + (i * 19 + 3) % 121;
                int r  = i % 8;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MANGROVE_ROOTS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MUD);
                else             block(es, sx, AY, sz, BlockTypes.DIRT);
            }

            // ── PERIMETER LOG WALL ────────────────────────────────────────
            fillHollow(es, cx-65, AY+1, cz-65, cx+65, AY+5, cz+65,
                    BlockTypes.MANGROVE_LOG, BlockTypes.AIR);
            fill(es, cx-65, AY+5, cz-65, cx+65, AY+5, cz+65,
                    BlockTypes.MANGROVE_PLANKS);
            // Corner moss pillars
            for (int y = AY+1; y <= AY+9; y++) {
                block(es, cx-65, y, cz-65, BlockTypes.MOSS_BLOCK);
                block(es, cx+65, y, cz-65, BlockTypes.MOSS_BLOCK);
                block(es, cx-65, y, cz+65, BlockTypes.MOSS_BLOCK);
                block(es, cx+65, y, cz+65, BlockTypes.MOSS_BLOCK);
            }
            // Mangrove root buttresses on walls every 15 blocks
            for (int d = -45; d <= 45; d += 15) {
                for (int y = AY+1; y <= AY+4; y++) {
                    block(es, cx+d, y, cz-65, BlockTypes.MANGROVE_ROOTS);
                    block(es, cx+d, y, cz+65, BlockTypes.MANGROVE_ROOTS);
                    block(es, cx-65, y, cz+d, BlockTypes.MANGROVE_ROOTS);
                    block(es, cx+65, y, cz+d, BlockTypes.MANGROVE_ROOTS);
                }
            }

            // ── UNDERGROUND MANGROVE CAVE (sunken hollow beneath central pool) ──
            fill(es, cx-10, 57, cz-10, cx+10, 62, cz+10, BlockTypes.AIR);
            fill(es, cx-10, 56, cz-10, cx+10, 56, cz+10, BlockTypes.MUD);
            fill(es, cx-10, 57, cz-10, cx+10, 57, cz+10, BlockTypes.CLAY);
            // Cave roof drips
            for (int dx = -9; dx <= 9; dx += 3)
                for (int dz2 = -9; dz2 <= 9; dz2 += 3)
                    block(es, cx+dx, 62, cz+dz2, BlockTypes.MUD);

            // ── TIDAL RIDGES (low muddy levees separating water zones) ─────
            fill(es, cx-30, AY+1, cz-2, cx-15, AY+1, cz+2, BlockTypes.MUD);
            fill(es, cx+15, AY+1, cz-2, cx+30, AY+1, cz+2, BlockTypes.MUD);
            fill(es, cx-2, AY+1, cz-30, cx+2, AY+1, cz-15, BlockTypes.MUD);
            fill(es, cx-2, AY+1, cz+15, cx+2, AY+1, cz+30, BlockTypes.MUD);
            // Muddy mangrove roots topping the ridges
            fill(es, cx-30, AY+2, cz-1, cx-15, AY+2, cz+1, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+15, AY+2, cz-1, cx+30, AY+2, cz+1, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-1, AY+2, cz-30, cx+1, AY+2, cz-15, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-1, AY+2, cz+15, cx+1, AY+2, cz+30, BlockTypes.MUDDY_MANGROVE_ROOTS);

            // ── MOSS LOG BRIDGES over water pools ─────────────────────────
            // Over central pool
            fill(es, cx-15, AY+1, cz,   cx+15, AY+1, cz,   BlockTypes.MANGROVE_LOG);
            fill(es, cx,    AY+1, cz-15, cx,    AY+1, cz+15, BlockTypes.MANGROVE_LOG);
            // Over NW pool
            fill(es, cx-54, AY+1, cz-55, cx-33, AY+1, cz-55, BlockTypes.MANGROVE_LOG);
            // Over SE pool
            fill(es, cx+33, AY+1, cz+55, cx+54, AY+1, cz+55, BlockTypes.MANGROVE_LOG);

            // ── STACKED ROOT PILLARS (aerial root curtains) ────────────────
            for (int col = -55; col <= 55; col += 18) {
                for (int y = AY+1; y <= AY+4; y++) {
                    block(es, cx+col, y, cz-60, BlockTypes.MANGROVE_ROOTS);
                    block(es, cx+col, y, cz+60, BlockTypes.MANGROVE_ROOTS);
                }
            }
            for (int col = -55; col <= 55; col += 18) {
                for (int y = AY+1; y <= AY+4; y++) {
                    block(es, cx-60, y, cz+col, BlockTypes.MANGROVE_ROOTS);
                    block(es, cx+60, y, cz+col, BlockTypes.MANGROVE_ROOTS);
                }
            }

            // ── CLAY STEPPING STONES across water channels ─────────────────
            for (int step = -12; step <= 12; step += 4) {
                block(es, cx+step, AY+1, cz-55, BlockTypes.CLAY);
                block(es, cx+step, AY+1, cz+55, BlockTypes.CLAY);
                block(es, cx-55, AY+1, cz+step, BlockTypes.CLAY);
                block(es, cx+55, AY+1, cz+step, BlockTypes.CLAY);
            }

            // ── SECOND SCATTER PASS (fine-grain texture variety) ──────────
            for (int i = 0; i < 280; i++) {
                int sx = cx - 58 + (i * 23 + 5) % 117;
                int sz = cz - 58 + (i * 17 + 9) % 117;
                int r  = i % 7;
                if      (r == 0) block(es, sx, AY+1, sz, BlockTypes.SHORT_GRASS);
                else if (r == 1) block(es, sx, AY+1, sz, BlockTypes.FERN);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.CLAY);
                else             block(es, sx, AY, sz, BlockTypes.GRAVEL);
            }

            // ── MANGROVE PLANK DOCK extending into central pool ───────────
            fill(es, cx-2, AY+1, cz-14, cx+2, AY+1, cz-2, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-2, AY+1, cz+2,  cx+2, AY+1, cz+14, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-14, AY+1, cz-2, cx-2, AY+1, cz+2, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx+2, AY+1, cz-2, cx+14, AY+1, cz+2, BlockTypes.MANGROVE_PLANKS);
            // Dock support posts
            for (int dp = -12; dp <= 12; dp += 4) {
                block(es, cx+dp, AY, cz-14, BlockTypes.MANGROVE_LOG);
                block(es, cx+dp, AY, cz+14, BlockTypes.MANGROVE_LOG);
                block(es, cx-14, AY, cz+dp, BlockTypes.MANGROVE_LOG);
                block(es, cx+14, AY, cz+dp, BlockTypes.MANGROVE_LOG);
            }

            // ── GRAVEL DELTA FANS at water channel outlets ─────────────────
            for (int fan = 0; fan <= 4; fan++) {
                int fw = fan;
                fill(es, cx-fw, AY, cz-65+fan, cx+fw, AY, cz-65+fan, BlockTypes.GRAVEL);
                fill(es, cx-fw, AY, cz+65-fan, cx+fw, AY, cz+65-fan, BlockTypes.GRAVEL);
                fill(es, cx-65+fan, AY, cz-fw, cx-65+fan, AY, cz+fw, BlockTypes.GRAVEL);
                fill(es, cx+65-fan, AY, cz-fw, cx+65-fan, AY, cz+fw, BlockTypes.GRAVEL);
            }

            // ── RAISED MUD ISLANDS with mangrove clusters ─────────────────
            fill(es, cx+25, AY,   cz-50, cx+40, AY+1, cz-38, BlockTypes.MUD);
            fill(es, cx+25, AY+2, cz-50, cx+40, AY+2, cz-38, BlockTypes.MOSS_BLOCK);
            column(es, cx+32, cz-44, AY+3, AY+8, BlockTypes.MANGROVE_LOG);
            disk(es, cx+32, AY+9, cz-44, 3, BlockTypes.MANGROVE_LEAVES);
            disk(es, cx+32, AY+10, cz-44, 2, BlockTypes.MANGROVE_LEAVES);
            block(es, cx+32, AY+11, cz-44, BlockTypes.MANGROVE_LEAVES);
            fill(es, cx-40, AY,   cz+38, cx-25, AY+1, cz+50, BlockTypes.MUD);
            fill(es, cx-40, AY+2, cz+38, cx-25, AY+2, cz+50, BlockTypes.MOSS_BLOCK);
            column(es, cx-32, cz+44, AY+3, AY+8, BlockTypes.MANGROVE_LOG);
            disk(es, cx-32, AY+9, cz+44, 3, BlockTypes.MANGROVE_LEAVES);

            // ── SUBMERGED CLAY SHELF (shallow estuary) ────────────────────
            fill(es, cx-60, 62, cz-5, cx-20, 62, cz+5, BlockTypes.CLAY);
            fill(es, cx-60, AY, cz-5, cx-20, AY, cz+5, BlockTypes.WATER);
            fill(es, cx+20, 62, cz-5, cx+60, 62, cz+5, BlockTypes.CLAY);
            fill(es, cx+20, AY, cz-5, cx+60, AY, cz+5, BlockTypes.WATER);
            fill(es, cx-5, 62, cz-60, cx+5, 62, cz-20, BlockTypes.CLAY);
            fill(es, cx-5, AY, cz-60, cx+5, AY, cz-20, BlockTypes.WATER);
            fill(es, cx-5, 62, cz+20, cx+5, 62, cz+60, BlockTypes.CLAY);
            fill(es, cx-5, AY, cz+20, cx+5, AY, cz+60, BlockTypes.WATER);

            // ── MUDFLAT HUMMOCK ROWS ──────────────────────────────────────
            for (int hum = -55; hum <= 55; hum += 11) {
                block(es, cx+hum, AY+1, cz-55, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, cx+hum, AY+1, cz+55, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, cx-55, AY+1, cz+hum, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, cx+55, AY+1, cz+hum, BlockTypes.MUDDY_MANGROVE_ROOTS);
            }

            // ── FOURTH SCATTER PASS ───────────────────────────────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 58 + (i * 89 + 61) % 117;
                int sz = cz - 58 + (i * 83 + 57) % 117;
                int r  = i % 8;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MANGROVE_ROOTS);
                else             block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-1, AY, cz-42, cx+1, AY, cz-40, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-1, AY, cz+40, cx+1, AY, cz+42, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-42, AY, cz-1, cx-40, AY, cz+1, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx+40, AY, cz-1, cx+42, AY, cz+1, BlockTypes.MANGROVE_PLANKS);
        }
    }

    // =========================================================================
    // ARENA 5 — VOLCANIC ISLAND   cx=220, cz=-220   (130x130)
    // Ash-grey textured ground: basalt, blackstone, magma, netherrack,
    // obsidian, gray concrete powder, tuff — lava pools, ash dunes
    // =========================================================================
    private void buildVolcanoArena() {
        final int cx = 220, cz = -220;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-65, 58, cz-65, cx+65, 63, cz+65, BlockTypes.BASALT);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.BLACKSTONE);

            // ── TUFF grey stone patches ───────────────────────────────────
            fill(es, cx-50, AY, cz-50, cx-20, AY, cz-20, BlockTypes.TUFF);
            fill(es, cx+20, AY, cz+20, cx+50, AY, cz+50, BlockTypes.TUFF);
            fill(es, cx-55, AY, cz+20, cx-25, AY, cz+50, BlockTypes.TUFF);
            fill(es, cx+25, AY, cz-50, cx+55, AY, cz-20, BlockTypes.TUFF);
            fill(es, cx-15, AY, cz+45, cx+15, AY, cz+65, BlockTypes.TUFF);
            fill(es, cx-15, AY, cz-65, cx+15, AY, cz-45, BlockTypes.TUFF);

            // ── POLISHED BASALT columns and surface ───────────────────────
            fill(es, cx-20, AY, cz-20, cx+20, AY, cz+20, BlockTypes.POLISHED_BASALT);
            fill(es, cx-65, AY, cz-20, cx-40, AY, cz+20, BlockTypes.POLISHED_BASALT);
            fill(es, cx+40, AY, cz-20, cx+65, AY, cz+20, BlockTypes.POLISHED_BASALT);
            fill(es, cx-20, AY, cz-65, cx+20, AY, cz-40, BlockTypes.POLISHED_BASALT);
            fill(es, cx-20, AY, cz+40, cx+20, AY, cz+65, BlockTypes.POLISHED_BASALT);

            // ── MAGMA BLOCK veins ─────────────────────────────────────────
            // radial veins from centre
            for (int r = 0; r <= 25; r++) {
                block(es, cx+r,  AY, cz,    BlockTypes.MAGMA_BLOCK);
                block(es, cx-r,  AY, cz,    BlockTypes.MAGMA_BLOCK);
                block(es, cx,    AY, cz+r,  BlockTypes.MAGMA_BLOCK);
                block(es, cx,    AY, cz-r,  BlockTypes.MAGMA_BLOCK);
                block(es, cx+r,  AY, cz+r,  BlockTypes.MAGMA_BLOCK);
                block(es, cx-r,  AY, cz-r,  BlockTypes.MAGMA_BLOCK);
            }
            fill(es, cx-30, AY, cz-5, cx-15, AY, cz+5, BlockTypes.MAGMA_BLOCK);
            fill(es, cx+15, AY, cz-5, cx+30, AY, cz+5, BlockTypes.MAGMA_BLOCK);
            fill(es, cx-5,  AY, cz-30, cx+5, AY, cz-15, BlockTypes.MAGMA_BLOCK);
            fill(es, cx-5,  AY, cz+15, cx+5, AY, cz+30, BlockTypes.MAGMA_BLOCK);

            // ── OBSIDIAN outcroppings ─────────────────────────────────────
            fill(es, cx-10, AY, cz-10, cx+10, AY, cz+10, BlockTypes.OBSIDIAN);
            fill(es, cx-10, AY+1, cz-10, cx+10, AY+2, cz+10, BlockTypes.CRYING_OBSIDIAN);
            // scattered obsidian shards
            int[][] obSpots = {
                {cx-45,cz-30},{cx+45,cz+30},{cx-30,cz+45},{cx+30,cz-45},
                {cx-55,cz-5}, {cx+55,cz+5}, {cx-5, cz-55},{cx+5, cz+55},
                {cx-38,cz-52},{cx+38,cz+52},{cx-52,cz+38},{cx+52,cz-38}
            };
            for (int[] ob : obSpots) {
                block(es, ob[0], AY, ob[1], BlockTypes.OBSIDIAN);
                block(es, ob[0], AY+1, ob[1], BlockTypes.OBSIDIAN);
                block(es, ob[0]+1, AY, ob[1], BlockTypes.OBSIDIAN);
                block(es, ob[0], AY, ob[1]+1, BlockTypes.OBSIDIAN);
                block(es, ob[0]+1, AY+1, ob[1]+1, BlockTypes.CRYING_OBSIDIAN);
            }

            // ── NETHERRACK ash-field patches ─────────────────────────────
            fill(es, cx-42, AY, cz-62, cx-28, AY, cz-48, BlockTypes.NETHERRACK);
            fill(es, cx+28, AY, cz+48, cx+42, AY, cz+62, BlockTypes.NETHERRACK);
            fill(es, cx+48, AY, cz-42, cx+62, AY, cz-28, BlockTypes.NETHERRACK);
            fill(es, cx-62, AY, cz+28, cx-48, AY, cz+42, BlockTypes.NETHERRACK);

            // ── GRAY CONCRETE POWDER ash dunes ───────────────────────────
            fill(es, cx-65, AY,   cz-65, cx-50, AY+1, cz-50, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+50, AY,   cz+50, cx+65, AY+1, cz+65, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+50, AY,   cz-65, cx+65, AY+1, cz-50, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-65, AY,   cz+50, cx-50, AY+1, cz+65, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-20, AY,   cz-65, cx+20, AY+2, cz-55, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-20, AY,   cz+55, cx+20, AY+2, cz+65, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-65, AY,   cz-20, cx-55, AY+2, cz+20, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+55, AY,   cz-20, cx+65, AY+2, cz+20, BlockTypes.GRAY_CONCRETE_POWDER);

            // ── LAVA POOLS ────────────────────────────────────────────────
            // Central caldera
            fill(es, cx-8,  62, cz-8,  cx+8,  62, cz+8,  BlockTypes.MAGMA_BLOCK);
            fill(es, cx-8,  AY, cz-8,  cx+8,  AY, cz+8,  BlockTypes.LAVA);
            // Smaller lava vents
            fill(es, cx-50, 63, cz-50, cx-38, 63, cz-38, BlockTypes.BASALT);
            fill(es, cx-50, AY, cz-50, cx-38, AY, cz-38, BlockTypes.LAVA);
            fill(es, cx+38, 63, cz+38, cx+50, 63, cz+50, BlockTypes.BASALT);
            fill(es, cx+38, AY, cz+38, cx+50, AY, cz+50, BlockTypes.LAVA);
            fill(es, cx+40, 63, cz-50, cx+50, 63, cz-40, BlockTypes.BASALT);
            fill(es, cx+40, AY, cz-50, cx+50, AY, cz-40, BlockTypes.LAVA);
            fill(es, cx-50, 63, cz+40, cx-40, 63, cz+50, BlockTypes.BASALT);
            fill(es, cx-50, AY, cz+40, cx-40, AY, cz+50, BlockTypes.LAVA);

            // ── BASALT COLUMNS (eruption pillars) ─────────────────────────
            int[][] pillars = {
                {cx-35,cz-35,8},{cx+35,cz+35,7},{cx+35,cz-35,9},{cx-35,cz+35,6},
                {cx-55,cz,   5},{cx+55,cz,   5},{cx,cz-55,    6},{cx,cz+55,   4},
                {cx-20,cz-50,4},{cx+20,cz+50,4},{cx+50,cz-20, 5},{cx-50,cz+20,5},
                {cx-28,cz+28,6},{cx+28,cz-28,6}
            };
            for (int[] p : pillars) {
                int px = p[0], pz = p[1], ph = p[2];
                column(es, px, pz, AY+1, AY+ph, BlockTypes.BASALT);
                block(es, px, AY+ph+1, pz, BlockTypes.MAGMA_BLOCK);
                // ring of polished basalt at base
                disk(es, px, AY, pz, 2, BlockTypes.POLISHED_BASALT);
            }

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 250; i++) {
                int sx = cx - 60 + (i * 29 + 11) % 121;
                int sz = cz - 60 + (i * 23 + 7)  % 121;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.POLISHED_BASALT);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.GRAY_CONCRETE_POWDER);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.OBSIDIAN);
                else             block(es, sx, AY, sz, BlockTypes.CRYING_OBSIDIAN);
            }

            // ── PERIMETER BLACKSTONE WALL ─────────────────────────────────
            fillHollow(es, cx-65, AY+1, cz-65, cx+65, AY+6, cz+65,
                    BlockTypes.POLISHED_BLACKSTONE_BRICKS, BlockTypes.AIR);
            // Gilded accents
            for (int d = -45; d <= 45; d += 15) {
                for (int y = AY+2; y <= AY+5; y++) {
                    block(es, cx+d, y, cz-65, BlockTypes.GILDED_BLACKSTONE);
                    block(es, cx+d, y, cz+65, BlockTypes.GILDED_BLACKSTONE);
                    block(es, cx-65, y, cz+d, BlockTypes.GILDED_BLACKSTONE);
                    block(es, cx+65, y, cz+d, BlockTypes.GILDED_BLACKSTONE);
                }
            }
            // Corner obsidian towers
            for (int y = AY+1; y <= AY+10; y++) {
                for (int dx = -1; dx <= 1; dx++) for (int dz2 = -1; dz2 <= 1; dz2++) {
                    block(es, cx-65+dx, y, cz-65+dz2, BlockTypes.OBSIDIAN);
                    block(es, cx+65+dx, y, cz-65+dz2, BlockTypes.OBSIDIAN);
                    block(es, cx-65+dx, y, cz+65+dz2, BlockTypes.OBSIDIAN);
                    block(es, cx+65+dx, y, cz+65+dz2, BlockTypes.OBSIDIAN);
                }
            }

            // ── LAVA OVERFLOW CHANNELS from caldera ───────────────────────
            // N channel
            for (int r = 9; r <= 30; r++) {
                block(es, cx, AY, cz-r, BlockTypes.LAVA);
                block(es, cx, 63, cz-r, BlockTypes.BASALT);
            }
            // S channel
            for (int r = 9; r <= 30; r++) {
                block(es, cx, AY, cz+r, BlockTypes.LAVA);
                block(es, cx, 63, cz+r, BlockTypes.BASALT);
            }
            // E channel
            for (int r = 9; r <= 30; r++) {
                block(es, cx+r, AY, cz, BlockTypes.LAVA);
                block(es, cx+r, 63, cz, BlockTypes.BASALT);
            }
            // W channel
            for (int r = 9; r <= 30; r++) {
                block(es, cx-r, AY, cz, BlockTypes.LAVA);
                block(es, cx-r, 63, cz, BlockTypes.BASALT);
            }

            // ── BASALT RUBBLE FIELDS (cooled lava flow textures) ───────────
            for (int i = 0; i < 120; i++) {
                int rx = cx - 60 + (i * 31 + 13) % 121;
                int rz = cz - 60 + (i * 43 + 17) % 121;
                if (i % 3 == 0)      block(es, rx, AY+1, rz, BlockTypes.BASALT);
                else if (i % 3 == 1) block(es, rx, AY+1, rz, BlockTypes.BLACKSTONE);
                else                 block(es, rx, AY+1, rz, BlockTypes.POLISHED_BASALT);
            }

            // ── CRACKED NETHER BRICK veins (ancient heat) ─────────────────
            fill(es, cx-35, AY, cz+30, cx-20, AY, cz+45, BlockTypes.CRACKED_NETHER_BRICKS);
            fill(es, cx+20, AY, cz-45, cx+35, AY, cz-30, BlockTypes.CRACKED_NETHER_BRICKS);
            fill(es, cx+30, AY, cz+20, cx+45, AY, cz+35, BlockTypes.CRACKED_NETHER_BRICKS);
            fill(es, cx-45, AY, cz-35, cx-30, AY, cz-20, BlockTypes.CRACKED_NETHER_BRICKS);

            // ── ASH DUNE DETAIL (stacked powder layers at corners) ─────────
            for (int d = 0; d <= 4; d++) {
                fill(es, cx-65+d, AY+d, cz-65+d, cx-60+d, AY+d, cz-60+d,
                        BlockTypes.GRAY_CONCRETE_POWDER);
                fill(es, cx+60-d, AY+d, cz+60-d, cx+65-d, AY+d, cz+65-d,
                        BlockTypes.GRAY_CONCRETE_POWDER);
            }

            // ── SOUL SOIL patches (ancient burn zones) ─────────────────────
            fill(es, cx-62, AY, cz-10, cx-50, AY, cz+10, BlockTypes.SOUL_SOIL);
            fill(es, cx+50, AY, cz-10, cx+62, AY, cz+10, BlockTypes.SOUL_SOIL);
            fill(es, cx-10, AY, cz-62, cx+10, AY, cz-50, BlockTypes.SOUL_SOIL);
            fill(es, cx-10, AY, cz+50, cx+10, AY, cz+62, BlockTypes.SOUL_SOIL);

            // ── SOUL SAND pockets near lava ───────────────────────────────
            fill(es, cx-8, AY, cz-30, cx+8, AY, cz-22, BlockTypes.SOUL_SAND);
            fill(es, cx-8, AY, cz+22, cx+8, AY, cz+30, BlockTypes.SOUL_SAND);
            fill(es, cx-30, AY, cz-8, cx-22, AY, cz+8, BlockTypes.SOUL_SAND);
            fill(es, cx+22, AY, cz-8, cx+30, AY, cz+8, BlockTypes.SOUL_SAND);

            // ── SECOND SCATTER PASS ───────────────────────────────────────
            for (int i = 0; i < 260; i++) {
                int sx = cx - 58 + (i * 47 + 19) % 117;
                int sz = cz - 58 + (i * 37 + 13) % 117;
                int r  = i % 8;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SOUL_SOIL);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.SOUL_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CRACKED_NETHER_BRICKS);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else             block(es, sx, AY, sz, BlockTypes.GRAY_CONCRETE_POWDER);
            }

            // ── VOLCANIC RIDGE LINES (raised blackstone ridges) ───────────
            for (int ridge = -50; ridge <= 50; ridge += 25) {
                fill(es, cx+ridge-1, AY+1, cz-60, cx+ridge+1, AY+3, cz+60,
                        BlockTypes.BLACKSTONE);
                fill(es, cx-60, AY+1, cz+ridge-1, cx+60, AY+3, cz+ridge+1,
                        BlockTypes.BLACKSTONE);
            }
            // Basalt caps on ridges
            for (int ridge = -50; ridge <= 50; ridge += 25) {
                for (int d = -60; d <= 60; d += 5) {
                    block(es, cx+ridge, AY+4, cz+d, BlockTypes.BASALT);
                    block(es, cx+d, AY+4, cz+ridge, BlockTypes.BASALT);
                }
            }

            // ── PUMICE FIELD (gray concrete elevation detail) ──────────────
            fill(es, cx-20, AY+1, cz-60, cx+20, AY+2, cz-50, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-20, AY+1, cz+50, cx+20, AY+2, cz+60, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-60, AY+1, cz-20, cx-50, AY+2, cz+20, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+50, AY+1, cz-20, cx+60, AY+2, cz+20, BlockTypes.GRAY_CONCRETE_POWDER);

            // ── OBSIDIAN CRATERS ──────────────────────────────────────────
            int[][] craters = {
                {cx-48,cz-48,3},{cx+48,cz+48,3},{cx+48,cz-48,2},{cx-48,cz+48,2}
            };
            for (int[] cr : craters) {
                disk(es, cr[0], AY, cr[1], cr[2]+1, BlockTypes.OBSIDIAN);
                fill(es, cr[0]-cr[2], AY+1, cr[1]-cr[2],
                        cr[0]+cr[2], AY+2, cr[1]+cr[2], BlockTypes.AIR);
                fill(es, cr[0]-cr[2], AY, cr[1]-cr[2],
                        cr[0]+cr[2], AY, cr[1]+cr[2], BlockTypes.LAVA);
            }

            // ── THIRD SCATTER PASS (volcanic debris) ──────────────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 58 + (i * 89 + 61) % 117;
                int sz = cz - 58 + (i * 83 + 59) % 117;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.POLISHED_BASALT);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.OBSIDIAN);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.SOUL_SAND);
                else             block(es, sx, AY, sz, BlockTypes.GRAY_CONCRETE_POWDER);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-1, AY, cz-42, cx+1, AY, cz-40, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx-1, AY, cz+40, cx+1, AY, cz+42, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx-42, AY, cz-1, cx-40, AY, cz+1, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx+40, AY, cz-1, cx+42, AY, cz+1, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    // =========================================================================
    // ARENA 6 — CORAL REEF   cx=-220, cz=-220   (130x130)
    // Vibrant shallow-water floor: sand, red sand, gravel, clay, prismarine,
    // sea lanterns, coral blocks, dead coral, blue ice — highly colourful
    // =========================================================================
    private void buildCoralArena() {
        final int cx = -220, cz = -220;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-65, 58, cz-65, cx+65, 63, cz+65, BlockTypes.SAND);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.SAND);

            // ── RED SAND patches ──────────────────────────────────────────
            fill(es, cx-50, AY, cz-50, cx-25, AY, cz-25, BlockTypes.RED_SAND);
            fill(es, cx+25, AY, cz+25, cx+50, AY, cz+50, BlockTypes.RED_SAND);
            fill(es, cx-50, AY, cz+25, cx-25, AY, cz+50, BlockTypes.RED_SAND);
            fill(es, cx+25, AY, cz-50, cx+50, AY, cz-25, BlockTypes.RED_SAND);
            fill(es, cx-15, AY, cz-65, cx+15, AY, cz-50, BlockTypes.RED_SAND);
            fill(es, cx-15, AY, cz+50, cx+15, AY, cz+65, BlockTypes.RED_SAND);
            fill(es, cx-65, AY, cz-15, cx-50, AY, cz+15, BlockTypes.RED_SAND);
            fill(es, cx+50, AY, cz-15, cx+65, AY, cz+15, BlockTypes.RED_SAND);

            // ── GRAVEL patches (deep channel floors) ─────────────────────
            fill(es, cx-20, AY, cz-20, cx+20, AY, cz+20, BlockTypes.GRAVEL);
            fill(es, cx-62, AY, cz-10, cx-40, AY, cz+10, BlockTypes.GRAVEL);
            fill(es, cx+40, AY, cz-10, cx+62, AY, cz+10, BlockTypes.GRAVEL);
            fill(es, cx-10, AY, cz-62, cx+10, AY, cz-40, BlockTypes.GRAVEL);
            fill(es, cx-10, AY, cz+40, cx+10, AY, cz+62, BlockTypes.GRAVEL);

            // ── PRISMARINE seafloor regions ───────────────────────────────
            fill(es, cx-35, AY, cz-15, cx-15, AY, cz+ 5, BlockTypes.PRISMARINE);
            fill(es, cx+15, AY, cz+ 5, cx+35, AY, cz+25, BlockTypes.PRISMARINE);
            fill(es, cx-35, AY, cz+15, cx-15, AY, cz+35, BlockTypes.PRISMARINE);
            fill(es, cx+15, AY, cz-35, cx+35, AY, cz-15, BlockTypes.PRISMARINE);
            fill(es, cx-62, AY, cz-62, cx-45, AY, cz-45, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx+45, AY, cz+45, cx+62, AY, cz+62, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx+45, AY, cz-62, cx+62, AY, cz-45, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx-62, AY, cz+45, cx-45, AY, cz+62, BlockTypes.PRISMARINE_BRICKS);

            // ── DARK PRISMARINE deep sections ─────────────────────────────
            fill(es, cx-15, AY, cz-15, cx+15, AY, cz+15, BlockTypes.DARK_PRISMARINE);
            fill(es, cx-38, AY, cz-38, cx-22, AY, cz-22, BlockTypes.DARK_PRISMARINE);
            fill(es, cx+22, AY, cz+22, cx+38, AY, cz+38, BlockTypes.DARK_PRISMARINE);

            // ── SEA LANTERN grid (glow under "water") ────────────────────
            for (int gx = cx-60; gx <= cx+60; gx += 12) {
                for (int gz = cz-60; gz <= cz+60; gz += 12) {
                    block(es, gx, AY, gz, BlockTypes.SEA_LANTERN);
                }
            }
            // Extra sea lanterns on dark prismarine
            for (int d = -12; d <= 12; d += 6) {
                block(es, cx+d, AY, cz, BlockTypes.SEA_LANTERN);
                block(es, cx, AY, cz+d, BlockTypes.SEA_LANTERN);
            }

            // ── CORAL BLOCK patches (rich colour) ─────────────────────────
            fill(es, cx-60, AY, cz-30, cx-48, AY, cz-18, BlockTypes.BRAIN_CORAL_BLOCK);
            fill(es, cx+48, AY, cz+18, cx+60, AY, cz+30, BlockTypes.BRAIN_CORAL_BLOCK);
            fill(es, cx-30, AY, cz+48, cx-18, AY, cz+60, BlockTypes.TUBE_CORAL_BLOCK);
            fill(es, cx+18, AY, cz-60, cx+30, AY, cz-48, BlockTypes.TUBE_CORAL_BLOCK);
            fill(es, cx-60, AY, cz+18, cx-48, AY, cz+30, BlockTypes.FIRE_CORAL_BLOCK);
            fill(es, cx+48, AY, cz-30, cx+60, AY, cz-18, BlockTypes.FIRE_CORAL_BLOCK);
            fill(es, cx+18, AY, cz+48, cx+30, AY, cz+60, BlockTypes.HORN_CORAL_BLOCK);
            fill(es, cx-30, AY, cz-60, cx-18, AY, cz-48, BlockTypes.HORN_CORAL_BLOCK);
            fill(es, cx-20, AY, cz+20, cx- 8, AY, cz+35, BlockTypes.BUBBLE_CORAL_BLOCK);
            fill(es, cx+ 8, AY, cz-35, cx+20, AY, cz-20, BlockTypes.BUBBLE_CORAL_BLOCK);
            fill(es, cx+20, AY, cz-35, cx+35, AY, cz-20, BlockTypes.BRAIN_CORAL_BLOCK);
            fill(es, cx-35, AY, cz+20, cx-20, AY, cz+35, BlockTypes.FIRE_CORAL_BLOCK);

            // ── DEAD CORAL patches (contrast) ─────────────────────────────
            fill(es, cx-45, AY, cz+ 5, cx-30, AY, cz+20, BlockTypes.DEAD_BRAIN_CORAL_BLOCK);
            fill(es, cx+30, AY, cz-20, cx+45, AY, cz- 5, BlockTypes.DEAD_BRAIN_CORAL_BLOCK);
            fill(es, cx+32, AY, cz+32, cx+45, AY, cz+45, BlockTypes.DEAD_TUBE_CORAL_BLOCK);
            fill(es, cx-45, AY, cz-45, cx-32, AY, cz-32, BlockTypes.DEAD_TUBE_CORAL_BLOCK);

            // ── BLUE ICE shallow frozen areas ─────────────────────────────
            fill(es, cx-65, AY, cz-65, cx-55, AY, cz-55, BlockTypes.BLUE_ICE);
            fill(es, cx+55, AY, cz+55, cx+65, AY, cz+65, BlockTypes.BLUE_ICE);
            fill(es, cx-10, AY, cz+ 5, cx+10, AY+1, cz+20, BlockTypes.PACKED_ICE);

            // ── CLAY shelf ────────────────────────────────────────────────
            fill(es, cx-25, AY, cz-62, cx-10, AY, cz-50, BlockTypes.CLAY);
            fill(es, cx+10, AY, cz+50, cx+25, AY, cz+62, BlockTypes.CLAY);
            fill(es, cx+50, AY, cz-25, cx+62, AY, cz-10, BlockTypes.CLAY);
            fill(es, cx-62, AY, cz+10, cx-50, AY, cz+25, BlockTypes.CLAY);

            // ── SCATTERED TEXTURE detail ──────────────────────────────────
            for (int i = 0; i < 260; i++) {
                int sx = cx - 60 + (i * 37 + 13) % 121;
                int sz = cz - 60 + (i * 29 +  7) % 121;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.BRAIN_CORAL_BLOCK);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.TUBE_CORAL_BLOCK);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.FIRE_CORAL_BLOCK);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.HORN_CORAL_BLOCK);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.BUBBLE_CORAL_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else             block(es, sx, AY, sz, BlockTypes.GRAVEL);
            }

            // ── PRISMARINE PERIMETER WALL ─────────────────────────────────
            fillHollow(es, cx-65, AY+1, cz-65, cx+65, AY+6, cz+65,
                    BlockTypes.PRISMARINE_BRICKS, BlockTypes.AIR);
            // Sea-lantern inserts every 10 blocks along wall
            for (int d = -50; d <= 50; d += 10) {
                block(es, cx+d, AY+3, cz-65, BlockTypes.SEA_LANTERN);
                block(es, cx+d, AY+3, cz+65, BlockTypes.SEA_LANTERN);
                block(es, cx-65, AY+3, cz+d, BlockTypes.SEA_LANTERN);
                block(es, cx+65, AY+3, cz+d, BlockTypes.SEA_LANTERN);
            }
            // Dark prismarine corner towers
            for (int y = AY+1; y <= AY+9; y++) {
                block(es, cx-65, y, cz-65, BlockTypes.DARK_PRISMARINE);
                block(es, cx+65, y, cz-65, BlockTypes.DARK_PRISMARINE);
                block(es, cx-65, y, cz+65, BlockTypes.DARK_PRISMARINE);
                block(es, cx+65, y, cz+65, BlockTypes.DARK_PRISMARINE);
            }

            // ── WATER TIDAL POOLS with warm coral inside ──────────────────
            // NW shallow pool
            fill(es, cx-65, 63, cz-30, cx-52, 63, cz-18, BlockTypes.GRAVEL);
            fill(es, cx-65, AY, cz-30, cx-52, AY, cz-18, BlockTypes.WATER);
            block(es, cx-62, AY, cz-26, BlockTypes.BRAIN_CORAL_BLOCK);
            block(es, cx-60, AY, cz-24, BlockTypes.FIRE_CORAL_BLOCK);
            block(es, cx-58, AY, cz-22, BlockTypes.TUBE_CORAL_BLOCK);
            // SE shallow pool
            fill(es, cx+52, 63, cz+18, cx+65, 63, cz+30, BlockTypes.GRAVEL);
            fill(es, cx+52, AY, cz+18, cx+65, AY, cz+30, BlockTypes.WATER);
            block(es, cx+55, AY, cz+22, BlockTypes.HORN_CORAL_BLOCK);
            block(es, cx+57, AY, cz+24, BlockTypes.BUBBLE_CORAL_BLOCK);
            block(es, cx+60, AY, cz+26, BlockTypes.BRAIN_CORAL_BLOCK);
            // NE pool
            fill(es, cx+52, 63, cz-30, cx+65, 63, cz-18, BlockTypes.GRAVEL);
            fill(es, cx+52, AY, cz-30, cx+65, AY, cz-18, BlockTypes.WATER);
            // SW pool
            fill(es, cx-65, 63, cz+18, cx-52, 63, cz+30, BlockTypes.GRAVEL);
            fill(es, cx-65, AY, cz+18, cx-52, AY, cz+30, BlockTypes.WATER);

            // ── KELP BEDS on gravel floor ─────────────────────────────────
            for (int kg = -60; kg <= 60; kg += 8) {
                block(es, cx+kg, AY, cz-5, BlockTypes.GRAVEL);
                block(es, cx-5, AY, cz+kg, BlockTypes.GRAVEL);
            }

            // ── PACKED ICE / BLUE ICE RIDGES ──────────────────────────────
            fill(es, cx-20, AY, cz-65, cx+20, AY, cz-58, BlockTypes.PACKED_ICE);
            fill(es, cx-20, AY, cz+58, cx+20, AY, cz+65, BlockTypes.PACKED_ICE);
            fill(es, cx-65, AY, cz-20, cx-58, AY, cz+20, BlockTypes.PACKED_ICE);
            fill(es, cx+58, AY, cz-20, cx+65, AY, cz+20, BlockTypes.PACKED_ICE);
            fill(es, cx-25, AY+1, cz-65, cx-15, AY+1, cz-58, BlockTypes.BLUE_ICE);
            fill(es, cx+15, AY+1, cz+58, cx+25, AY+1, cz+65, BlockTypes.BLUE_ICE);

            // ── RAISED SANDSTONE REEF RIDGES ──────────────────────────────
            fill(es, cx-58, AY+1, cz-58, cx-42, AY+2, cz-42, BlockTypes.SANDSTONE);
            fill(es, cx+42, AY+1, cz+42, cx+58, AY+2, cz+58, BlockTypes.SANDSTONE);
            fill(es, cx+42, AY+1, cz-58, cx+58, AY+2, cz-42, BlockTypes.SANDSTONE);
            fill(es, cx-58, AY+1, cz+42, cx-42, AY+2, cz+58, BlockTypes.SANDSTONE);
            // Prismarine caps
            fill(es, cx-58, AY+3, cz-58, cx-42, AY+3, cz-42, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx+42, AY+3, cz+42, cx+58, AY+3, cz+58, BlockTypes.PRISMARINE_BRICKS);

            // ── SECOND SCATTER PASS (fine coral texture) ──────────────────
            for (int i = 0; i < 280; i++) {
                int sx = cx - 58 + (i * 53 + 27) % 117;
                int sz = cz - 58 + (i * 47 + 23) % 117;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.DEAD_BRAIN_CORAL_BLOCK);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.DEAD_TUBE_CORAL_BLOCK);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.DEAD_FIRE_CORAL_BLOCK);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.DARK_PRISMARINE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else             block(es, sx, AY, sz, BlockTypes.CLAY);
            }

            // ── PRISMARINE ARCH BRIDGES (spanning wide pools) ─────────────
            // E-W arch span over tidal channels
            for (int y = AY+1; y <= AY+4; y++) {
                block(es, cx-62, y, cz, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx+62, y, cz, BlockTypes.PRISMARINE_BRICKS);
            }
            fill(es, cx-62, AY+5, cz, cx+62, AY+5, cz, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx, AY+5, cz-62, cx, AY+5, cz+62, BlockTypes.PRISMARINE_BRICKS);

            // ── CORAL TOWER PILLARS ───────────────────────────────────────
            int[][] cTowers = {
                {cx-50,cz-50},{cx+50,cz+50},{cx+50,cz-50},{cx-50,cz+50},
                {cx-30,cz-60},{cx+30,cz+60},{cx+60,cz-30},{cx-60,cz+30}
            };
            for (int[] ct : cTowers) {
                column(es, ct[0], ct[1], AY+1, AY+5, BlockTypes.PRISMARINE_BRICKS);
                block(es, ct[0], AY+6, ct[1], BlockTypes.SEA_LANTERN);
                disk(es, ct[0], AY, ct[1], 2, BlockTypes.DARK_PRISMARINE);
            }

            // ── VARIEGATED SAND/CORAL FLOOR STRIPS ────────────────────────
            for (int strip = -60; strip <= 60; strip += 8) {
                for (int d = -60; d <= 60; d++) {
                    if ((strip + d) % 2 == 0)
                        block(es, cx+strip, AY, cz+d, BlockTypes.SAND);
                    else
                        block(es, cx+strip, AY, cz+d, BlockTypes.RED_SAND);
                }
            }

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 58 + (i * 97 + 67) % 117;
                int sz = cz - 58 + (i * 89 + 61) % 117;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.BRAIN_CORAL_BLOCK);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.TUBE_CORAL_BLOCK);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.FIRE_CORAL_BLOCK);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.HORN_CORAL_BLOCK);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.BUBBLE_CORAL_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.DARK_PRISMARINE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.SAND);
                else             block(es, sx, AY, sz, BlockTypes.CLAY);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-1, AY, cz-42, cx+1, AY, cz-40, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx-1, AY, cz+40, cx+1, AY, cz+42, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx-42, AY, cz-1, cx-40, AY, cz+1, BlockTypes.PRISMARINE_BRICKS);
            fill(es, cx+40, AY, cz-1, cx+42, AY, cz+1, BlockTypes.PRISMARINE_BRICKS);
        }
    }

    // =========================================================================
    // ARENA 7 — OVERGROWN RUINS   cx=220, cz=220   (130x130)
    // Ancient crumbled stonework richly textured: stone bricks, mossy bricks,
    // cracked bricks, deepslate, cobblestone, moss, dirt, ferns — layered ruin
    // =========================================================================
    private void buildRuinsArena() {
        final int cx = 220, cz = 220;
        try (EditSession es = newSession()) {
            // ── BASE GROUND ──────────────────────────────────────────────
            fill(es, cx-65, 58, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.GRASS_BLOCK);

            // ── STONE BRICK foundation patches ────────────────────────────
            fill(es, cx-50, AY, cz-50, cx-25, AY, cz-25, BlockTypes.STONE_BRICKS);
            fill(es, cx+25, AY, cz+25, cx+50, AY, cz+50, BlockTypes.STONE_BRICKS);
            fill(es, cx-50, AY, cz+25, cx-25, AY, cz+50, BlockTypes.STONE_BRICKS);
            fill(es, cx+25, AY, cz-50, cx+50, AY, cz-25, BlockTypes.STONE_BRICKS);
            fill(es, cx-15, AY, cz-15, cx+15, AY, cz+15, BlockTypes.STONE_BRICKS);
            fill(es, cx-65, AY, cz-15, cx-40, AY, cz+15, BlockTypes.STONE_BRICKS);
            fill(es, cx+40, AY, cz-15, cx+65, AY, cz+15, BlockTypes.STONE_BRICKS);
            fill(es, cx-15, AY, cz-65, cx+15, AY, cz-40, BlockTypes.STONE_BRICKS);
            fill(es, cx-15, AY, cz+40, cx+15, AY, cz+65, BlockTypes.STONE_BRICKS);

            // ── MOSSY STONE BRICK overlaid on top of stone brick ──────────
            fill(es, cx-48, AY, cz-48, cx-28, AY, cz-28, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+28, AY, cz+28, cx+48, AY, cz+48, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-48, AY, cz+28, cx-28, AY, cz+48, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx+28, AY, cz-48, cx+48, AY, cz-28, BlockTypes.MOSSY_STONE_BRICKS);

            // ── CRACKED STONE BRICKS (decay detail) ──────────────────────
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz+12, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx-62, AY, cz-12, cx-42, AY, cz+12, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx+42, AY, cz-12, cx+62, AY, cz+12, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx-12, AY, cz-62, cx+12, AY, cz-42, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx-12, AY, cz+42, cx+12, AY, cz+62, BlockTypes.CRACKED_STONE_BRICKS);
            // scattered cracked patches
            fill(es, cx-38, AY, cz-18, cx-28, AY, cz- 8, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx+28, AY, cz+ 8, cx+38, AY, cz+18, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx+ 8, AY, cz-38, cx+18, AY, cz-28, BlockTypes.CRACKED_STONE_BRICKS);
            fill(es, cx-18, AY, cz+28, cx- 8, AY, cz+38, BlockTypes.CRACKED_STONE_BRICKS);

            // ── DEEPSLATE TILES (ancient floor layer) ────────────────────
            fill(es, cx-22, AY, cz-22, cx-12, AY, cz-12, BlockTypes.DEEPSLATE_TILES);
            fill(es, cx+12, AY, cz+12, cx+22, AY, cz+22, BlockTypes.DEEPSLATE_TILES);
            fill(es, cx+12, AY, cz-22, cx+22, AY, cz-12, BlockTypes.DEEPSLATE_TILES);
            fill(es, cx-22, AY, cz+12, cx-12, AY, cz+22, BlockTypes.DEEPSLATE_TILES);
            fill(es, cx-65, AY, cz-65, cx-50, AY, cz-50, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx+50, AY, cz+50, cx+65, AY, cz+65, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx+50, AY, cz-65, cx+65, AY, cz-50, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx-65, AY, cz+50, cx-50, AY, cz+65, BlockTypes.COBBLED_DEEPSLATE);

            // ── COBBLESTONE rubble zones ──────────────────────────────────
            fill(es, cx-40, AY, cz-62, cx-20, AY, cz-48, BlockTypes.COBBLESTONE);
            fill(es, cx+20, AY, cz+48, cx+40, AY, cz+62, BlockTypes.COBBLESTONE);
            fill(es, cx+48, AY, cz-40, cx+62, AY, cz-20, BlockTypes.COBBLESTONE);
            fill(es, cx-62, AY, cz+20, cx-48, AY, cz+40, BlockTypes.COBBLESTONE);
            fill(es, cx- 6, AY, cz-30, cx+ 6, AY, cz-22, BlockTypes.COBBLESTONE);
            fill(es, cx- 6, AY, cz+22, cx+ 6, AY, cz+30, BlockTypes.COBBLESTONE);
            fill(es, cx-30, AY, cz- 6, cx-22, AY, cz+ 6, BlockTypes.COBBLESTONE);
            fill(es, cx+22, AY, cz- 6, cx+30, AY, cz+ 6, BlockTypes.COBBLESTONE);

            // ── MOSSY COBBLESTONE (heavy moss) ───────────────────────────
            fill(es, cx-30, AY, cz-60, cx-20, AY, cz-50, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+20, AY, cz+50, cx+30, AY, cz+60, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+50, AY, cz-30, cx+60, AY, cz-20, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx-60, AY, cz+20, cx-50, AY, cz+30, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx-25, AY, cz+ 5, cx-15, AY, cz+15, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+15, AY, cz-15, cx+25, AY, cz- 5, BlockTypes.MOSSY_COBBLESTONE);

            // ── MOSS BLOCK carpets ────────────────────────────────────────
            fill(es, cx-62, AY, cz-62, cx-52, AY, cz-52, BlockTypes.MOSS_BLOCK);
            fill(es, cx+52, AY, cz+52, cx+62, AY, cz+62, BlockTypes.MOSS_BLOCK);
            fill(es, cx+52, AY, cz-62, cx+62, AY, cz-52, BlockTypes.MOSS_BLOCK);
            fill(es, cx-62, AY, cz+52, cx-52, AY, cz+62, BlockTypes.MOSS_BLOCK);
            fill(es, cx-18, AY, cz-45, cx- 8, AY, cz-35, BlockTypes.MOSS_BLOCK);
            fill(es, cx+ 8, AY, cz+35, cx+18, AY, cz+45, BlockTypes.MOSS_BLOCK);

            // ── DIRT/GRASS patches (nature reclaiming) ────────────────────
            fill(es, cx-35, AY, cz-35, cx-25, AY, cz-25, BlockTypes.DIRT);
            fill(es, cx+25, AY, cz+25, cx+35, AY, cz+35, BlockTypes.DIRT);
            fill(es, cx-35, AY, cz+25, cx-25, AY, cz+35, BlockTypes.DIRT);
            fill(es, cx+25, AY, cz-35, cx+35, AY, cz-25, BlockTypes.DIRT);
            fill(es, cx- 8, AY, cz-55, cx+ 8, AY, cz-45, BlockTypes.GRASS_BLOCK);
            fill(es, cx- 8, AY, cz+45, cx+ 8, AY, cz+55, BlockTypes.GRASS_BLOCK);
            fill(es, cx-55, AY, cz- 8, cx-45, AY, cz+ 8, BlockTypes.GRASS_BLOCK);
            fill(es, cx+45, AY, cz- 8, cx+55, AY, cz+ 8, BlockTypes.GRASS_BLOCK);

            // ── POLISHED DEEPSLATE central floor ─────────────────────────
            fill(es, cx-10, AY, cz-10, cx+10, AY, cz+10, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx- 2, AY, cz-30, cx+ 2, AY, cz-12, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx- 2, AY, cz+12, cx+ 2, AY, cz+30, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx-30, AY, cz- 2, cx-12, AY, cz+ 2, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx+12, AY, cz- 2, cx+30, AY, cz+ 2, BlockTypes.POLISHED_DEEPSLATE);

            // ── RAISED WALL REMNANTS (collapsed ruin walls) ──────────────
            int[][] walls = {
                {cx-50,AY+1,cz-50,cx-35,AY+4,cz-50},
                {cx+35,AY+1,cz+50,cx+50,AY+4,cz+50},
                {cx-50,AY+1,cz+35,cx-50,AY+4,cz+50},
                {cx+50,AY+1,cz-50,cx+50,AY+4,cz-35},
                {cx-20,AY+1,cz-62,cx+20,AY+3,cz-58},
                {cx-20,AY+1,cz+58,cx+20,AY+3,cz+62},
                {cx-62,AY+1,cz-20,cx-58,AY+3,cz+20},
                {cx+58,AY+1,cz-20,cx+62,AY+3,cz+20}
            };
            for (int[] w : walls) {
                fill(es, w[0], w[1], w[2], w[3], w[4], w[5], BlockTypes.MOSSY_STONE_BRICKS);
            }
            // Crumbled tops
            for (int[] w : walls) {
                block(es, w[0]+1, w[4], w[2], BlockTypes.CRACKED_STONE_BRICKS);
                block(es, w[3]-1, w[4], w[5], BlockTypes.CRACKED_STONE_BRICKS);
            }

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 260; i++) {
                int sx = cx - 60 + (i * 41 + 17) % 121;
                int sz = cz - 60 + (i * 31 + 11) % 121;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CRACKED_STONE_BRICKS);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.DEEPSLATE_TILES);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.COBBLED_DEEPSLATE);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.DIRT);
                else             block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
            }

            // ── PERIMETER RUINED WALL ─────────────────────────────────────
            fillHollow(es, cx-65, AY+1, cz-65, cx+65, AY+5, cz+65,
                    BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.AIR);
            // Crumbled gaps — replace some segments with cracked/cobble
            for (int d = -60; d <= 60; d += 8) {
                if (Math.abs(d) % 16 == 0) {
                    for (int y = AY+3; y <= AY+5; y++) {
                        block(es, cx+d, y, cz-65, BlockTypes.AIR);
                        block(es, cx+d, y, cz+65, BlockTypes.AIR);
                        block(es, cx-65, y, cz+d, BlockTypes.AIR);
                        block(es, cx+65, y, cz+d, BlockTypes.AIR);
                    }
                }
                block(es, cx+d, AY+2, cz-65, BlockTypes.CRACKED_STONE_BRICKS);
                block(es, cx+d, AY+2, cz+65, BlockTypes.CRACKED_STONE_BRICKS);
                block(es, cx-65, AY+2, cz+d, BlockTypes.CRACKED_STONE_BRICKS);
                block(es, cx+65, AY+2, cz+d, BlockTypes.CRACKED_STONE_BRICKS);
            }
            // Mossy corner towers
            for (int y = AY+1; y <= AY+8; y++) {
                block(es, cx-65, y, cz-65, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx+65, y, cz-65, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx-65, y, cz+65, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx+65, y, cz+65, BlockTypes.MOSSY_COBBLESTONE);
            }

            // ── UNDERGROUND RUIN CHAMBER ──────────────────────────────────
            fill(es, cx-8, 57, cz-8, cx+8, 62, cz+8, BlockTypes.AIR);
            fill(es, cx-8, 56, cz-8, cx+8, 56, cz+8, BlockTypes.POLISHED_DEEPSLATE);
            fillHollow(es, cx-8, 57, cz-8, cx+8, 62, cz+8,
                    BlockTypes.DEEPSLATE_TILES, BlockTypes.AIR);
            // Old chest-room floor
            fill(es, cx-6, 57, cz-6, cx+6, 57, cz+6, BlockTypes.DEEPSLATE_BRICKS);
            // Cracked ceiling
            for (int dc = -7; dc <= 7; dc += 3)
                for (int dz2 = -7; dz2 <= 7; dz2 += 3)
                    block(es, cx+dc, 62, cz+dz2, BlockTypes.CRACKED_DEEPSLATE_BRICKS);

            // ── CHISELED DEEPSLATE accents on ruin walls ─────────────────
            int[][] ruinWallAccents = {
                {cx-50,cz-50},{cx+50,cz+50},{cx+50,cz-50},{cx-50,cz+50},
                {cx-50,cz},   {cx+50,cz},   {cx,cz-50},   {cx,cz+50},
                {cx-35,cz+45},{cx+35,cz-45},{cx+45,cz+35},{cx-45,cz-35}
            };
            for (int[] acc : ruinWallAccents) {
                block(es, acc[0], AY+1, acc[1], BlockTypes.CHISELED_DEEPSLATE);
                block(es, acc[0], AY+2, acc[1], BlockTypes.CHISELED_DEEPSLATE);
            }

            // ── FERN AND MOSS GROUND COVER on stone areas ─────────────────
            for (int i = 0; i < 150; i++) {
                int fx = cx - 55 + (i * 23 + 7) % 111;
                int fz = cz - 55 + (i * 17 + 5) % 111;
                if (i % 2 == 0) block(es, fx, AY+1, fz, BlockTypes.FERN);
                else            block(es, fx, AY+1, fz, BlockTypes.SHORT_GRASS);
            }

            // ── DEEPSLATE BRICK COURTYARD (central ruin floor) ────────────
            fill(es, cx-8, AY, cz-8, cx+8, AY, cz+8, BlockTypes.DEEPSLATE_BRICKS);
            // Chiseled pattern
            for (int dx = -6; dx <= 6; dx += 3)
                for (int dz2 = -6; dz2 <= 6; dz2 += 3)
                    block(es, cx+dx, AY, cz+dz2, BlockTypes.CHISELED_DEEPSLATE);

            // ── COBBLED DEEPSLATE RUBBLE MOUNDS ───────────────────────────
            fill(es, cx-22, AY+1, cz+28, cx-12, AY+2, cz+38, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx+12, AY+1, cz-38, cx+22, AY+2, cz-28, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx+28, AY+1, cz+12, cx+38, AY+2, cz+22, BlockTypes.COBBLED_DEEPSLATE);
            fill(es, cx-38, AY+1, cz-22, cx-28, AY+2, cz-12, BlockTypes.COBBLED_DEEPSLATE);
            // Cap with mossy
            fill(es, cx-22, AY+3, cz+28, cx-12, AY+3, cz+38, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+12, AY+3, cz-38, cx+22, AY+3, cz-28, BlockTypes.MOSSY_COBBLESTONE);

            // ── SECOND SCATTER PASS (detailed rubble texture) ─────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 58 + (i * 59 + 29) % 117;
                int sz = cz - 58 + (i * 53 + 23) % 117;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.DEEPSLATE_TILES);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.DEEPSLATE_BRICKS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.COBBLED_DEEPSLATE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.POLISHED_DEEPSLATE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.CRACKED_DEEPSLATE_BRICKS);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.CHISELED_DEEPSLATE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.CRACKED_STONE_BRICKS);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else             block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
            }

            // ── COLLAPSED AQUEDUCT REMNANT ────────────────────────────────
            // Ancient water channel running E-W through ruins
            fill(es, cx-65, AY+2, cz-2, cx+65, AY+4, cz+2, BlockTypes.MOSSY_STONE_BRICKS);
            fill(es, cx-63, AY+2, cz-1, cx+63, AY+2, cz+1, BlockTypes.AIR);
            fill(es, cx-63, AY+3, cz-1, cx+63, AY+3, cz+1, BlockTypes.WATER);
            // Broken sections of aqueduct
            for (int seg = -50; seg <= 50; seg += 20) {
                fill(es, cx+seg, AY+2, cz-2, cx+seg+4, AY+4, cz+2, BlockTypes.AIR);
                block(es, cx+seg+2, AY+1, cz, BlockTypes.CRACKED_STONE_BRICKS);
            }

            // ── OVERGROWN GARDEN (moss+stone patchwork) ────────────────────
            fill(es, cx-32, AY, cz+18, cx-16, AY, cz+32, BlockTypes.GRASS_BLOCK);
            fill(es, cx-30, AY, cz+20, cx-18, AY, cz+30, BlockTypes.MOSS_BLOCK);
            for (int gfx = cx-28; gfx <= cx-20; gfx += 3)
                for (int gfz = cz+22; gfz <= cz+28; gfz += 3)
                    block(es, gfx, AY+1, gfz, BlockTypes.FERN);

            // ── STONE BRICK FLAGSTONE PLAZA ───────────────────────────────
            fill(es, cx-10, AY+1, cz-10, cx+10, AY+1, cz+10, BlockTypes.STONE_BRICKS);
            for (int dc = -8; dc <= 8; dc += 2)
                for (int dz2 = -8; dz2 <= 8; dz2 += 2)
                    if ((dc + dz2) % 4 == 0)
                        block(es, cx+dc, AY+1, cz+dz2, BlockTypes.CRACKED_STONE_BRICKS);

            // ── DEEPSLATE VEINS in ground ─────────────────────────────────
            for (int i = 0; i < 90; i++) {
                int vx = cx - 60 + (i * 29 + 11) % 121;
                int vz = cz - 60 + (i * 23 + 7)  % 121;
                block(es, vx, AY, vz, BlockTypes.DEEPSLATE);
            }

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 58 + (i * 101 + 71) % 117;
                int sz = cz - 58 + (i * 97 + 67)  % 117;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CRACKED_STONE_BRICKS);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.DEEPSLATE_TILES);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.DIRT);
                else             block(es, sx, AY, sz, BlockTypes.POLISHED_DEEPSLATE);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-1, AY, cz-42, cx+1, AY, cz-40, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx-1, AY, cz+40, cx+1, AY, cz+42, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx-42, AY, cz-1, cx-40, AY, cz+1, BlockTypes.POLISHED_DEEPSLATE);
            fill(es, cx+40, AY, cz-1, cx+42, AY, cz+1, BlockTypes.POLISHED_DEEPSLATE);
        }
    }

    // =========================================================================
    // ARENA 8 — CLIFFSIDE   cx=-220, cz=220   (130x130)
    // Stratified rock face: calcite, tuff, andesite, granite, diorite,
    // terracotta bands, gravel, stone — dramatic elevation changes
    // =========================================================================
    private void buildCliffsideArena() {
        final int cx = -220, cz = 220;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-65, 55, cz-65, cx+65, 63, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz+65, BlockTypes.STONE);

            // ── TUFF base layer ───────────────────────────────────────────
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz-20, BlockTypes.TUFF);
            fill(es, cx-65, AY, cz+20, cx+65, AY, cz+65, BlockTypes.TUFF);
            fill(es, cx-65, AY, cz-20, cx-20, AY, cz+20, BlockTypes.TUFF);
            fill(es, cx+20, AY, cz-20, cx+65, AY, cz+20, BlockTypes.TUFF);

            // ── CALCITE white cliff bands ─────────────────────────────────
            fill(es, cx-50, AY, cz-60, cx-30, AY, cz-40, BlockTypes.CALCITE);
            fill(es, cx+30, AY, cz+40, cx+50, AY, cz+60, BlockTypes.CALCITE);
            fill(es, cx+40, AY, cz-60, cx+60, AY, cz-40, BlockTypes.CALCITE);
            fill(es, cx-60, AY, cz+40, cx-40, AY, cz+60, BlockTypes.CALCITE);
            fill(es, cx-15, AY, cz-65, cx+15, AY, cz-55, BlockTypes.CALCITE);
            fill(es, cx-15, AY, cz+55, cx+15, AY, cz+65, BlockTypes.CALCITE);
            fill(es, cx-65, AY, cz-15, cx-55, AY, cz+15, BlockTypes.CALCITE);
            fill(es, cx+55, AY, cz-15, cx+65, AY, cz+15, BlockTypes.CALCITE);

            // ── ANDESITE mid-cliff band ───────────────────────────────────
            fill(es, cx-65, AY, cz-45, cx+65, AY, cz-35, BlockTypes.ANDESITE);
            fill(es, cx-65, AY, cz+35, cx+65, AY, cz+45, BlockTypes.ANDESITE);
            fill(es, cx-45, AY, cz-35, cx-35, AY, cz+35, BlockTypes.ANDESITE);
            fill(es, cx+35, AY, cz-35, cx+45, AY, cz+35, BlockTypes.ANDESITE);

            // ── GRANITE warm tones ────────────────────────────────────────
            fill(es, cx-35, AY, cz-35, cx-15, AY, cz-15, BlockTypes.GRANITE);
            fill(es, cx+15, AY, cz+15, cx+35, AY, cz+35, BlockTypes.GRANITE);
            fill(es, cx-35, AY, cz+15, cx-15, AY, cz+35, BlockTypes.GRANITE);
            fill(es, cx+15, AY, cz-35, cx+35, AY, cz-15, BlockTypes.GRANITE);
            fill(es, cx-62, AY, cz-62, cx-50, AY, cz-50, BlockTypes.POLISHED_GRANITE);
            fill(es, cx+50, AY, cz+50, cx+62, AY, cz+62, BlockTypes.POLISHED_GRANITE);
            fill(es, cx+50, AY, cz-62, cx+62, AY, cz-50, BlockTypes.POLISHED_GRANITE);
            fill(es, cx-62, AY, cz+50, cx-50, AY, cz+62, BlockTypes.POLISHED_GRANITE);

            // ── DIORITE white-grey zones ──────────────────────────────────
            fill(es, cx-30, AY, cz-62, cx-15, AY, cz-50, BlockTypes.DIORITE);
            fill(es, cx+15, AY, cz+50, cx+30, AY, cz+62, BlockTypes.DIORITE);
            fill(es, cx+50, AY, cz-30, cx+62, AY, cz-15, BlockTypes.DIORITE);
            fill(es, cx-62, AY, cz+15, cx-50, AY, cz+30, BlockTypes.DIORITE);

            // ── TERRACOTTA colour bands (exposed iron-rich strata) ─────────
            fill(es, cx-15, AY, cz-15, cx+15, AY, cz+15, BlockTypes.TERRACOTTA);
            fill(es, cx-12, AY, cz-12, cx+12, AY+1, cz+12, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx- 8, AY, cz- 8, cx+ 8, AY+2, cz+ 8, BlockTypes.RED_TERRACOTTA);
            fill(es, cx- 5, AY, cz- 5, cx+ 5, AY+3, cz+ 5, BlockTypes.BROWN_TERRACOTTA);
            fill(es, cx- 2, AY, cz- 2, cx+ 2, AY+4, cz+ 2, BlockTypes.WHITE_TERRACOTTA);
            // Side terracotta bands
            fill(es, cx-65, AY,   cz-30, cx-55, AY+2, cz-20, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx+55, AY,   cz+20, cx+65, AY+2, cz+30, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx+55, AY,   cz-30, cx+65, AY+2, cz-20, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx-65, AY,   cz+20, cx-55, AY+2, cz+30, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx-30, AY,   cz-65, cx-20, AY+2, cz-55, BlockTypes.LIGHT_BLUE_TERRACOTTA);
            fill(es, cx+20, AY,   cz+55, cx+30, AY+2, cz+65, BlockTypes.LIGHT_BLUE_TERRACOTTA);
            fill(es, cx+20, AY,   cz-65, cx+30, AY+2, cz-55, BlockTypes.PURPLE_TERRACOTTA);
            fill(es, cx-30, AY,   cz+55, cx-20, AY+2, cz+65, BlockTypes.PURPLE_TERRACOTTA);

            // ── GRAVEL scree slopes ───────────────────────────────────────
            fill(es, cx-25, AY, cz+18, cx+25, AY, cz+28, BlockTypes.GRAVEL);
            fill(es, cx-25, AY, cz-28, cx+25, AY, cz-18, BlockTypes.GRAVEL);
            fill(es, cx+18, AY, cz-25, cx+28, AY, cz+25, BlockTypes.GRAVEL);
            fill(es, cx-28, AY, cz-25, cx-18, AY, cz+25, BlockTypes.GRAVEL);
            fill(es, cx-55, AY, cz-55, cx-44, AY, cz-44, BlockTypes.GRAVEL);
            fill(es, cx+44, AY, cz+44, cx+55, AY, cz+55, BlockTypes.GRAVEL);
            fill(es, cx+44, AY, cz-55, cx+55, AY, cz-44, BlockTypes.GRAVEL);
            fill(es, cx-55, AY, cz+44, cx-44, AY, cz+55, BlockTypes.GRAVEL);

            // ── RAISED CLIFF LEDGES (terrain elevation) ───────────────────
            fill(es, cx-65, AY+1, cz-65, cx-50, AY+3, cz-50, BlockTypes.STONE);
            fill(es, cx-65, AY+3, cz-65, cx-55, AY+5, cz-55, BlockTypes.CALCITE);
            fill(es, cx+50, AY+1, cz+50, cx+65, AY+3, cz+65, BlockTypes.STONE);
            fill(es, cx+55, AY+3, cz+55, cx+65, AY+5, cz+65, BlockTypes.CALCITE);
            fill(es, cx+50, AY+1, cz-65, cx+65, AY+3, cz-50, BlockTypes.STONE);
            fill(es, cx+55, AY+3, cz-65, cx+65, AY+5, cz-55, BlockTypes.TUFF);
            fill(es, cx-65, AY+1, cz+50, cx-50, AY+3, cz+65, BlockTypes.STONE);
            fill(es, cx-65, AY+3, cz+55, cx-55, AY+5, cz+65, BlockTypes.TUFF);
            // Mid ledges
            fill(es, cx-35, AY+1, cz-55, cx-20, AY+2, cz-45, BlockTypes.ANDESITE);
            fill(es, cx+20, AY+1, cz+45, cx+35, AY+2, cz+55, BlockTypes.ANDESITE);
            fill(es, cx+45, AY+1, cz-35, cx+55, AY+2, cz-20, BlockTypes.DIORITE);
            fill(es, cx-55, AY+1, cz+20, cx-45, AY+2, cz+35, BlockTypes.DIORITE);

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 260; i++) {
                int sx = cx - 60 + (i * 43 + 19) % 121;
                int sz = cz - 60 + (i * 37 + 13) % 121;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.STONE);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CALCITE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.ANDESITE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.GRANITE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.DIORITE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.TERRACOTTA);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else             block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
            }

            // ── PERIMETER STONE WALL ──────────────────────────────────────
            fillHollow(es, cx-65, AY+1, cz-65, cx+65, AY+6, cz+65,
                    BlockTypes.STONE, BlockTypes.AIR);
            // Calcite stripe through middle of wall
            for (int d = -65; d <= 65; d++) {
                block(es, cx+d, AY+4, cz-65, BlockTypes.CALCITE);
                block(es, cx+d, AY+4, cz+65, BlockTypes.CALCITE);
                block(es, cx-65, AY+4, cz+d, BlockTypes.CALCITE);
                block(es, cx+65, AY+4, cz+d, BlockTypes.CALCITE);
            }
            // Corner terracotta towers
            for (int y = AY+1; y <= AY+9; y++) {
                block(es, cx-65, y, cz-65, BlockTypes.ORANGE_TERRACOTTA);
                block(es, cx+65, y, cz-65, BlockTypes.ORANGE_TERRACOTTA);
                block(es, cx-65, y, cz+65, BlockTypes.RED_TERRACOTTA);
                block(es, cx+65, y, cz+65, BlockTypes.RED_TERRACOTTA);
            }

            // ── IRON ORE / RAW IRON BLOCKS in cliff faces ─────────────────
            fill(es, cx-65, AY, cz-42, cx-58, AY+2, cz-35, BlockTypes.IRON_ORE);
            fill(es, cx+58, AY, cz+35, cx+65, AY+2, cz+42, BlockTypes.IRON_ORE);
            fill(es, cx+58, AY, cz-42, cx+65, AY+2, cz-35, BlockTypes.IRON_ORE);
            fill(es, cx-65, AY, cz+35, cx-58, AY+2, cz+42, BlockTypes.IRON_ORE);

            // ── POLISHED GRANITE ledge edges ──────────────────────────────
            // Ledge rim details
            for (int d = -60; d <= 60; d += 6) {
                block(es, cx+d, AY+3, cz-65, BlockTypes.POLISHED_GRANITE);
                block(es, cx+d, AY+3, cz+65, BlockTypes.POLISHED_GRANITE);
                block(es, cx-65, AY+3, cz+d, BlockTypes.POLISHED_GRANITE);
                block(es, cx+65, AY+3, cz+d, BlockTypes.POLISHED_GRANITE);
            }

            // ── CALCITE STALACTITES (cliff overhangs) ─────────────────────
            for (int stl = -55; stl <= 55; stl += 10) {
                block(es, cx+stl, AY+5, cz-65, BlockTypes.CALCITE);
                block(es, cx+stl, AY+6, cz-65, BlockTypes.CALCITE);
                block(es, cx+stl, AY+5, cz+65, BlockTypes.CALCITE);
                block(es, cx+stl, AY+6, cz+65, BlockTypes.CALCITE);
                block(es, cx-65, AY+5, cz+stl, BlockTypes.CALCITE);
                block(es, cx-65, AY+6, cz+stl, BlockTypes.CALCITE);
                block(es, cx+65, AY+5, cz+stl, BlockTypes.CALCITE);
                block(es, cx+65, AY+6, cz+stl, BlockTypes.CALCITE);
            }

            // ── POLISHED DIORITE PATHS (carved into cliff) ────────────────
            fill(es, cx-65, AY+1, cz-3, cx-20, AY+1, cz+3, BlockTypes.POLISHED_DIORITE);
            fill(es, cx+20, AY+1, cz-3, cx+65, AY+1, cz+3, BlockTypes.POLISHED_DIORITE);
            fill(es, cx-3, AY+1, cz-65, cx+3, AY+1, cz-20, BlockTypes.POLISHED_DIORITE);
            fill(es, cx-3, AY+1, cz+20, cx+3, AY+1, cz+65, BlockTypes.POLISHED_DIORITE);

            // ── GRAVEL SCREE STREAMS (falling debris) ─────────────────────
            for (int s = 0; s <= 8; s++) {
                block(es, cx-65+s, AY+s/2, cz-40+s*2, BlockTypes.GRAVEL);
                block(es, cx+65-s, AY+s/2, cz+40-s*2, BlockTypes.GRAVEL);
                block(es, cx-40+s*2, AY+s/2, cz+65-s, BlockTypes.GRAVEL);
                block(es, cx+40-s*2, AY+s/2, cz-65+s, BlockTypes.GRAVEL);
            }

            // ── SECOND SCATTER PASS ───────────────────────────────────────
            for (int i = 0; i < 280; i++) {
                int sx = cx - 58 + (i * 61 + 31) % 117;
                int sz = cz - 58 + (i * 53 + 27) % 117;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.CALCITE);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.ANDESITE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.POLISHED_ANDESITE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.GRANITE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.POLISHED_GRANITE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.DIORITE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.POLISHED_DIORITE);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else             block(es, sx, AY, sz, BlockTypes.IRON_ORE);
            }

            // ── CLIFF FACE VERTICAL STRATA (layered height bands) ─────────
            // West cliff face: vertical coloured bands
            BlockType[] clifLayers = {
                BlockTypes.CALCITE, BlockTypes.ANDESITE,
                BlockTypes.GRANITE,  BlockTypes.TUFF, BlockTypes.DIORITE
            };
            for (int y = AY+1; y <= AY+10; y++) {
                int layer = (y - AY - 1) % clifLayers.length;
                fill(es, cx-65, y, cz-65, cx-65, y, cz+65, clifLayers[layer]);
                fill(es, cx+65, y, cz-65, cx+65, y, cz+65, clifLayers[layer]);
            }

            // ── ROCK POOLS at cliff base ──────────────────────────────────
            fill(es, cx-62, 63, cz-35, cx-52, 63, cz-25, BlockTypes.GRAVEL);
            fill(es, cx-62, AY, cz-35, cx-52, AY, cz-25, BlockTypes.WATER);
            fill(es, cx+52, 63, cz+25, cx+62, 63, cz+35, BlockTypes.GRAVEL);
            fill(es, cx+52, AY, cz+25, cx+62, AY, cz+35, BlockTypes.WATER);
            fill(es, cx+52, 63, cz-35, cx+62, 63, cz-25, BlockTypes.GRAVEL);
            fill(es, cx+52, AY, cz-35, cx+62, AY, cz-25, BlockTypes.WATER);
            fill(es, cx-62, 63, cz+25, cx-52, 63, cz+35, BlockTypes.GRAVEL);
            fill(es, cx-62, AY, cz+25, cx-52, AY, cz+35, BlockTypes.WATER);

            // ── WIND-CARVED STONE ARCHES ──────────────────────────────────
            for (int y = AY+1; y <= AY+6; y++) {
                block(es, cx-60, y, cz, BlockTypes.ANDESITE);
                block(es, cx+60, y, cz, BlockTypes.ANDESITE);
                block(es, cx, y, cz-60, BlockTypes.GRANITE);
                block(es, cx, y, cz+60, BlockTypes.GRANITE);
            }
            block(es, cx-60, AY+7, cz, BlockTypes.CALCITE);
            block(es, cx+60, AY+7, cz, BlockTypes.CALCITE);
            block(es, cx, AY+7, cz-60, BlockTypes.CALCITE);
            block(es, cx, AY+7, cz+60, BlockTypes.CALCITE);

            // ── MULTI-COLOR TERRACOTTA CLIFF STRIPES ─────────────────────
            for (int strip = -55; strip <= 55; strip += 8) {
                block(es, cx-65, AY, cz+strip, BlockTypes.ORANGE_TERRACOTTA);
                block(es, cx+65, AY, cz+strip, BlockTypes.RED_TERRACOTTA);
                block(es, cx+strip, AY, cz-65, BlockTypes.YELLOW_TERRACOTTA);
                block(es, cx+strip, AY, cz+65, BlockTypes.WHITE_TERRACOTTA);
            }

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 58 + (i * 107 + 73) % 117;
                int sz = cz - 58 + (i * 101 + 67) % 117;
                int r  = i % 11;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.CALCITE);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.ANDESITE);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.GRANITE);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.DIORITE);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.TERRACOTTA);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.RED_TERRACOTTA);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.STONE);
                else              block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-1, AY, cz-42, cx+1, AY, cz-40, BlockTypes.POLISHED_ANDESITE);
            fill(es, cx-1, AY, cz+40, cx+1, AY, cz+42, BlockTypes.POLISHED_ANDESITE);
            fill(es, cx-42, AY, cz-1, cx-40, AY, cz+1, BlockTypes.POLISHED_ANDESITE);
            fill(es, cx+40, AY, cz-1, cx+42, AY, cz+1, BlockTypes.POLISHED_ANDESITE);
        }
    }

    // =========================================================================
    // FFA 1 — JUNGLE FORTRESS   cx=500, cz=0   (150x150)
    // Dense jungle biome: grass, podzol, jungle planks, bamboo mosaic,
    // mossy blocks, coarse dirt, vines patches, fern texture — vast 150x150
    // =========================================================================
    private void buildJungleFortressFFA() {
        final int cx = 500, cz = 0;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-75, 55, cz-75, cx+75, 63, cz+75, BlockTypes.DIRT);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.GRASS_BLOCK);

            // ── PODZOL forest floor (large areas) ─────────────────────────
            fill(es, cx-60, AY, cz-60, cx-30, AY, cz-30, BlockTypes.PODZOL);
            fill(es, cx+30, AY, cz+30, cx+60, AY, cz+60, BlockTypes.PODZOL);
            fill(es, cx-60, AY, cz+30, cx-30, AY, cz+60, BlockTypes.PODZOL);
            fill(es, cx+30, AY, cz-60, cx+60, AY, cz-30, BlockTypes.PODZOL);
            fill(es, cx-18, AY, cz-18, cx+18, AY, cz+18, BlockTypes.PODZOL);
            fill(es, cx-75, AY, cz-18, cx-40, AY, cz+18, BlockTypes.PODZOL);
            fill(es, cx+40, AY, cz-18, cx+75, AY, cz+18, BlockTypes.PODZOL);
            fill(es, cx-18, AY, cz-75, cx+18, AY, cz-40, BlockTypes.PODZOL);
            fill(es, cx-18, AY, cz+40, cx+18, AY, cz+75, BlockTypes.PODZOL);

            // ── COARSE DIRT clearing paths ────────────────────────────────
            fill(es, cx-75, AY, cz- 3, cx+75, AY, cz+ 3, BlockTypes.COARSE_DIRT);
            fill(es, cx- 3, AY, cz-75, cx+ 3, AY, cz+75, BlockTypes.COARSE_DIRT);
            // Diagonal paths
            for (int r = 5; r <= 65; r++) {
                block(es, cx+r, AY, cz+r, BlockTypes.COARSE_DIRT);
                block(es, cx-r, AY, cz+r, BlockTypes.COARSE_DIRT);
                block(es, cx+r, AY, cz-r, BlockTypes.COARSE_DIRT);
                block(es, cx-r, AY, cz-r, BlockTypes.COARSE_DIRT);
            }

            // ── JUNGLE PLANKS platforms (jungle settlement remnants) ──────
            fill(es, cx-45, AY, cz-45, cx-28, AY, cz-28, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx+28, AY, cz+28, cx+45, AY, cz+45, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx+28, AY, cz-45, cx+45, AY, cz-28, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-45, AY, cz+28, cx-28, AY, cz+45, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz+12, BlockTypes.JUNGLE_PLANKS);

            // ── BAMBOO MOSAIC panels ──────────────────────────────────────
            fill(es, cx-65, AY, cz-75, cx-50, AY, cz-62, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx+50, AY, cz+62, cx+65, AY, cz+75, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx+62, AY, cz-75, cx+75, AY, cz-62, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx-75, AY, cz+62, cx-62, AY, cz+75, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx-28, AY, cz-75, cx-14, AY, cz-60, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx+14, AY, cz+60, cx+28, AY, cz+75, BlockTypes.BAMBOO_MOSAIC);

            // ── BAMBOO PLANKS floor strips ────────────────────────────────
            for (int d = -70; d <= 70; d += 20) {
                fill(es, cx+d, AY, cz-75, cx+d+2, AY, cz+75, BlockTypes.BAMBOO_PLANKS);
                fill(es, cx-75, AY, cz+d, cx+75, AY, cz+d+2, BlockTypes.BAMBOO_PLANKS);
            }

            // ── MOSS BLOCK heavy coverage ─────────────────────────────────
            fill(es, cx-75, AY, cz-75, cx-62, AY, cz-62, BlockTypes.MOSS_BLOCK);
            fill(es, cx+62, AY, cz+62, cx+75, AY, cz+75, BlockTypes.MOSS_BLOCK);
            fill(es, cx+62, AY, cz-75, cx+75, AY, cz-62, BlockTypes.MOSS_BLOCK);
            fill(es, cx-75, AY, cz+62, cx-62, AY, cz+75, BlockTypes.MOSS_BLOCK);
            fill(es, cx-40, AY, cz-20, cx-25, AY, cz- 5, BlockTypes.MOSS_BLOCK);
            fill(es, cx+25, AY, cz+ 5, cx+40, AY, cz+20, BlockTypes.MOSS_BLOCK);
            fill(es, cx+25, AY, cz-20, cx+40, AY, cz- 5, BlockTypes.MOSS_BLOCK);
            fill(es, cx-40, AY, cz+ 5, cx-25, AY, cz+20, BlockTypes.MOSS_BLOCK);

            // ── MOSSY COBBLESTONE ruins ───────────────────────────────────
            fill(es, cx-22, AY, cz-22, cx-14, AY, cz-14, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+14, AY, cz+14, cx+22, AY, cz+22, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx+14, AY, cz-22, cx+22, AY, cz-14, BlockTypes.MOSSY_COBBLESTONE);
            fill(es, cx-22, AY, cz+14, cx-14, AY, cz+22, BlockTypes.MOSSY_COBBLESTONE);

            // ── JUNGLE LOG trees (large canopy) ───────────────────────────
            int[][] trees = {
                {cx-55,cz-55,9},{cx+55,cz+55,11},{cx+55,cz-55,10},{cx-55,cz+55,8},
                {cx-35,cz-55,8},{cx+35,cz+55, 9},{cx+55,cz-35,10},{cx-55,cz+35,9},
                {cx-70,cz,   7},{cx+70,cz,    8},{cx,cz-70,    9},{cx,cz+70,   7},
                {cx-25,cz-68,8},{cx+25,cz+68, 9},{cx+68,cz-25,10},{cx-68,cz+25,8},
                {cx-48,cz+ 8,9},{cx+48,cz- 8,10},{cx+ 8,cz-48, 8},{cx- 8,cz+48,9},
                {cx-70,cz-45,7},{cx+70,cz+45, 8},{cx+45,cz-70, 9},{cx-45,cz+70,8},
                {cx-70,cz+20,8},{cx+70,cz-20, 9},{cx-20,cz-70, 8},{cx+20,cz+70,9}
            };
            for (int[] t : trees) {
                int tx = t[0], tz = t[1], th = t[2];
                column(es, tx, tz, AY+1, AY+th, BlockTypes.JUNGLE_LOG);
                disk(es, tx, AY+th+1, tz, 5, BlockTypes.JUNGLE_LEAVES);
                disk(es, tx, AY+th+2, tz, 4, BlockTypes.JUNGLE_LEAVES);
                disk(es, tx, AY+th+3, tz, 3, BlockTypes.JUNGLE_LEAVES);
                disk(es, tx, AY+th+4, tz, 2, BlockTypes.JUNGLE_LEAVES);
                block(es, tx, AY+th+5, tz, BlockTypes.JUNGLE_LEAVES);
                // Jungle vines (represented as roots on base)
                block(es, tx, AY, tz, BlockTypes.PODZOL);
                block(es, tx+1, AY, tz, BlockTypes.PODZOL);
                block(es, tx-1, AY, tz, BlockTypes.PODZOL);
                block(es, tx, AY, tz+1, BlockTypes.PODZOL);
                block(es, tx, AY, tz-1, BlockTypes.PODZOL);
            }

            // ── WATER FEATURE (river through arena) ──────────────────────
            fill(es, cx- 3, 63, cz-75, cx+ 3, 63, cz+75, BlockTypes.CLAY);
            fill(es, cx- 3, AY, cz-75, cx+ 3, AY, cz+75, BlockTypes.WATER);

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 70 + (i * 47 + 23) % 141;
                int sz = cz - 70 + (i * 43 + 17) % 141;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.JUNGLE_PLANKS);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.BAMBOO_MOSAIC);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.BAMBOO_PLANKS);
                else             block(es, sx, AY, sz, BlockTypes.MUD);
            }

            // ── PERIMETER JUNGLE WALL ─────────────────────────────────────
            fillHollow(es, cx-75, AY+1, cz-75, cx+75, AY+7, cz+75,
                    BlockTypes.JUNGLE_LOG, BlockTypes.AIR);
            fill(es, cx-75, AY+7, cz-75, cx+75, AY+7, cz+75, BlockTypes.JUNGLE_PLANKS);
            // Moss patches on wall face
            for (int d = -65; d <= 65; d += 8) {
                for (int y = AY+2; y <= AY+5; y += 2) {
                    block(es, cx+d, y, cz-75, BlockTypes.MOSS_BLOCK);
                    block(es, cx+d, y, cz+75, BlockTypes.MOSS_BLOCK);
                    block(es, cx-75, y, cz+d, BlockTypes.MOSS_BLOCK);
                    block(es, cx+75, y, cz+d, BlockTypes.MOSS_BLOCK);
                }
            }
            // Corner towers
            for (int y = AY+1; y <= AY+12; y++) {
                block(es, cx-75, y, cz-75, BlockTypes.JUNGLE_LOG);
                block(es, cx+75, y, cz-75, BlockTypes.JUNGLE_LOG);
                block(es, cx-75, y, cz+75, BlockTypes.JUNGLE_LOG);
                block(es, cx+75, y, cz+75, BlockTypes.JUNGLE_LOG);
            }

            // ── UNDERGROUND JUNGLE GROTTO ──────────────────────────────────
            fill(es, cx-15, 57, cz-15, cx+15, 63, cz+15, BlockTypes.AIR);
            fill(es, cx-15, 56, cz-15, cx+15, 56, cz+15, BlockTypes.PODZOL);
            fillHollow(es, cx-15, 57, cz-15, cx+15, 63, cz+15,
                    BlockTypes.JUNGLE_LOG, BlockTypes.AIR);
            // Root ceiling
            for (int dc = -13; dc <= 13; dc += 4)
                for (int dz2 = -13; dz2 <= 13; dz2 += 4)
                    block(es, cx+dc, 63, cz+dz2, BlockTypes.JUNGLE_LEAVES);

            // ── BAMBOO THICKETS (16 spread clusters) ─────────────────────
            int[][] bambooClumps = {
                {cx-62,cz-30},{cx-65,cz-20},{cx-60,cz-10},
                {cx+62,cz+30},{cx+65,cz+20},{cx+60,cz+10},
                {cx-30,cz-62},{cx-20,cz-65},{cx-10,cz-60},
                {cx+30,cz+62},{cx+20,cz+65},{cx+10,cz+60},
                {cx-62,cz+30},{cx-65,cz+20},{cx-60,cz+10},
                {cx+62,cz-30},{cx+65,cz-20},{cx+60,cz-10},
                {cx-30,cz+62},{cx-20,cz+65},{cx-10,cz+60},
                {cx+30,cz-62},{cx+20,cz-65},{cx+10,cz-60}
            };
            for (int[] bc : bambooClumps) {
                // Dense bamboo clump using column to represent bamboo stalks
                for (int db = -2; db <= 2; db++)
                    for (int dz2 = -2; dz2 <= 2; dz2++)
                        column(es, bc[0]+db, bc[1]+dz2, AY+1, AY + 5 + (db+dz2+4)%4,
                                BlockTypes.BAMBOO_PLANKS);
            }

            // ── FALLEN LOG FEATURES ────────────────────────────────────────
            int[][] logs = {
                {cx-50,cz-10,cx-35,cz-10},{cx+35,cz+10,cx+50,cz+10},
                {cx-10,cz-50,cx-10,cz-35},{cx+10,cz+35,cx+10,cz+50},
                {cx-65,cz-65,cx-52,cz-65},{cx+52,cz+65,cx+65,cz+65},
                {cx+52,cz-65,cx+65,cz-65},{cx-65,cz+52,cx-65,cz+65}
            };
            for (int[] l : logs) {
                int lx1=l[0],lz1=l[1],lx2=l[2],lz2=l[3];
                int steps = Math.max(Math.abs(lx2-lx1), Math.abs(lz2-lz1));
                for (int s = 0; s <= steps; s++) {
                    int lx = lx1 + (lx2-lx1)*s/Math.max(steps,1);
                    int lz = lz1 + (lz2-lz1)*s/Math.max(steps,1);
                    block(es, lx, AY+1, lz, BlockTypes.JUNGLE_LOG);
                    block(es, lx, AY, lz, BlockTypes.PODZOL);
                }
            }

            // ── GIANT MUSHROOM CAPS (jungle overgrowth) ───────────────────
            int[][] mushroomCaps = {
                {cx-45,cz+55,5},{cx+45,cz-55,4},{cx+55,cz+45,5},{cx-55,cz-45,4},
                {cx-70,cz+65,3},{cx+70,cz-65,4},{cx-65,cz+70,3},{cx+65,cz-70,4}
            };
            for (int[] mc : mushroomCaps) {
                int mx = mc[0], mz = mc[1], mr = mc[2];
                column(es, mx, mz, AY+1, AY+mr+2, BlockTypes.MUSHROOM_STEM);
                disk(es, mx, AY+mr+3, mz, mr+2, BlockTypes.RED_MUSHROOM_BLOCK);
                disk(es, mx, AY+mr+2, mz, mr, BlockTypes.RED_MUSHROOM_BLOCK);
            }

            // ── COCOA JUNGLE WALL PATCHES ─────────────────────────────────
            for (int dp = -60; dp <= 60; dp += 8) {
                block(es, cx+dp, AY+2, cz-75, BlockTypes.JUNGLE_LOG);
                block(es, cx+dp, AY+4, cz+75, BlockTypes.JUNGLE_LOG);
                block(es, cx-75, AY+2, cz+dp, BlockTypes.JUNGLE_LOG);
                block(es, cx+75, AY+4, cz+dp, BlockTypes.JUNGLE_LOG);
            }

            // ── VINED MOSSY CLIFF FACE (west wall) ────────────────────────
            for (int y = AY+1; y <= AY+7; y += 2)
                for (int d = -70; d <= 70; d += 6)
                    block(es, cx-75, y, cz+d, BlockTypes.MOSS_BLOCK);

            // ── ELEVATED TREEHOUSE PLATFORMS ──────────────────────────────
            fill(es, cx-52, AY+8, cz-52, cx-42, AY+8, cz-42, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx+42, AY+8, cz+42, cx+52, AY+8, cz+52, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx+42, AY+8, cz-52, cx+52, AY+8, cz-42, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-52, AY+8, cz+42, cx-42, AY+8, cz+52, BlockTypes.JUNGLE_PLANKS);
            // Bamboo mosaic rails
            for (int d = -52; d <= -42; d++) {
                block(es, cx+d, AY+9, cz-52, BlockTypes.BAMBOO_MOSAIC);
                block(es, cx+d, AY+9, cz-42, BlockTypes.BAMBOO_MOSAIC);
            }
            // Platform support columns
            column(es, cx-52, cz-52, AY+1, AY+7, BlockTypes.JUNGLE_LOG);
            column(es, cx-42, cz-42, AY+1, AY+7, BlockTypes.JUNGLE_LOG);
            column(es, cx+42, cz+42, AY+1, AY+7, BlockTypes.JUNGLE_LOG);
            column(es, cx+52, cz+52, AY+1, AY+7, BlockTypes.JUNGLE_LOG);

            // ── LARGE FERN GROUND COVER ───────────────────────────────────
            for (int i = 0; i < 200; i++) {
                int sx = cx - 70 + (i * 41 + 11) % 141;
                int sz = cz - 70 + (i * 37 + 7)  % 141;
                if (i % 3 == 0)      block(es, sx, AY+1, sz, BlockTypes.LARGE_FERN);
                else if (i % 3 == 1) block(es, sx, AY+1, sz, BlockTypes.FERN);
                else                 block(es, sx, AY+1, sz, BlockTypes.SHORT_GRASS);
            }

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 280; i++) {
                int sx = cx - 68 + (i * 71 + 43) % 137;
                int sz = cz - 68 + (i * 67 + 41) % 137;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.JUNGLE_PLANKS);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.BAMBOO_MOSAIC);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else             block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
            }

            // ── JUNGLE RIVER BED DETAIL ────────────────────────────────────
            // Sandy bottom under river
            fill(es, cx-2, 62, cz-75, cx+2, 62, cz+75, BlockTypes.GRAVEL);
            // River bank pebbles
            for (int riv = -70; riv <= 70; riv += 3) {
                block(es, cx-4, AY+1, cz+riv, BlockTypes.STONE);
                block(es, cx+4, AY+1, cz+riv, BlockTypes.STONE);
                block(es, cx-5, AY, cz+riv, BlockTypes.GRAVEL);
                block(es, cx+5, AY, cz+riv, BlockTypes.GRAVEL);
            }

            // ── GIANT JUNGLE FERN CLUSTERS ────────────────────────────────
            for (int i = 0; i < 80; i++) {
                int fx = cx - 72 + (i * 31 + 13) % 145;
                int fz = cz - 72 + (i * 23 + 11) % 145;
                block(es, fx, AY+1, fz, BlockTypes.LARGE_FERN);
                block(es, fx+1, AY+1, fz, BlockTypes.FERN);
                block(es, fx, AY+1, fz+1, BlockTypes.FERN);
            }

            // ── HANGING VINES ON WALL (vine blocks on wall face) ──────────
            for (int vy = AY+2; vy <= AY+6; vy += 2)
                for (int vd = -70; vd <= 70; vd += 5)
                    block(es, cx+vd, vy, cz-75, BlockTypes.JUNGLE_LEAVES);

            // ── COCOA BEAN PATCHES (brown terracotta) ─────────────────────
            for (int i = 0; i < 40; i++) {
                int bx = cx - 65 + (i * 41 + 17) % 131;
                int bz = cz - 65 + (i * 37 + 13) % 131;
                block(es, bx, AY, bz, BlockTypes.BROWN_MUSHROOM_BLOCK);
            }

            // ── ADDITIONAL TREE PLATFORM DETAIL ───────────────────────────
            // Ladders/access points on platform columns (oak fence)
            for (int y = AY+1; y <= AY+7; y++) {
                block(es, cx-52, y, cz-50, BlockTypes.OAK_FENCE);
                block(es, cx+50, y, cz+52, BlockTypes.OAK_FENCE);
                block(es, cx+50, y, cz-52, BlockTypes.OAK_FENCE);
                block(es, cx-52, y, cz+50, BlockTypes.OAK_FENCE);
            }

            // ── MOSS CARPET BLANKETS (wide moss sheets) ───────────────────
            fill(es, cx-75, AY+1, cz-35, cx-60, AY+1, cz-20, BlockTypes.MOSS_BLOCK);
            fill(es, cx+60, AY+1, cz+20, cx+75, AY+1, cz+35, BlockTypes.MOSS_BLOCK);
            fill(es, cx-35, AY+1, cz+60, cx-20, AY+1, cz+75, BlockTypes.MOSS_BLOCK);
            fill(es, cx+20, AY+1, cz-75, cx+35, AY+1, cz-60, BlockTypes.MOSS_BLOCK);

            // ── FOURTH SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 68 + (i * 97 + 67) % 137;
                int sz = cz - 68 + (i * 89 + 61) % 137;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.JUNGLE_PLANKS);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.BAMBOO_PLANKS);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else             block(es, sx, AY, sz, BlockTypes.STONE);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-2, AY, cz-52, cx+2, AY, cz-49, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-2, AY, cz+49, cx+2, AY, cz+52, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-52, AY, cz-2, cx-49, AY, cz+2, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx+49, AY, cz-2, cx+52, AY, cz+2, BlockTypes.JUNGLE_PLANKS);
            fill(es, cx-38, AY, cz-38, cx-35, AY, cz-35, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx+35, AY, cz+35, cx+38, AY, cz+38, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx+35, AY, cz-38, cx+38, AY, cz-35, BlockTypes.BAMBOO_MOSAIC);
            fill(es, cx-38, AY, cz+35, cx-35, AY, cz+38, BlockTypes.BAMBOO_MOSAIC);
        }
    }

    // =========================================================================
    // FFA 2 — ISLAND RESORT   cx=-500, cz=0   (150x150)
    // Tropical island: white sand, orange sand, sandstone, coral blocks,
    // terracotta, sea lanterns, blue ice lagoon, tropical trees, rich shoreline
    // =========================================================================
    private void buildIslandFFA() {
        final int cx = -500, cz = 0;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-75, 55, cz-75, cx+75, 63, cz+75, BlockTypes.SANDSTONE);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.SAND);

            // ── RED SAND inner island ─────────────────────────────────────
            fill(es, cx-55, AY, cz-55, cx+55, AY, cz+55, BlockTypes.RED_SAND);
            fill(es, cx-40, AY, cz-40, cx+40, AY, cz+40, BlockTypes.SAND);
            fill(es, cx-25, AY, cz-25, cx+25, AY, cz+25, BlockTypes.RED_SAND);
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz+12, BlockTypes.SAND);

            // ── SANDSTONE rocky outcrops ──────────────────────────────────
            fill(es, cx-65, AY, cz-65, cx-50, AY, cz-50, BlockTypes.SANDSTONE);
            fill(es, cx+50, AY, cz+50, cx+65, AY, cz+65, BlockTypes.SANDSTONE);
            fill(es, cx+50, AY, cz-65, cx+65, AY, cz-50, BlockTypes.SANDSTONE);
            fill(es, cx-65, AY, cz+50, cx-50, AY, cz+65, BlockTypes.SANDSTONE);
            fill(es, cx-65, AY, cz-20, cx-55, AY, cz+20, BlockTypes.SANDSTONE);
            fill(es, cx+55, AY, cz-20, cx+65, AY, cz+20, BlockTypes.SANDSTONE);
            fill(es, cx-20, AY, cz-65, cx+20, AY, cz-55, BlockTypes.SANDSTONE);
            fill(es, cx-20, AY, cz+55, cx+20, AY, cz+65, BlockTypes.SANDSTONE);
            // Smooth sandstone
            fill(es, cx-50, AY, cz-30, cx-35, AY, cz-15, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx+35, AY, cz+15, cx+50, AY, cz+30, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx+35, AY, cz-30, cx+50, AY, cz-15, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-50, AY, cz+15, cx-35, AY, cz+30, BlockTypes.SMOOTH_SANDSTONE);

            // ── TERRACOTTA coloured tiles ─────────────────────────────────
            fill(es, cx-12, AY, cz-40, cx+12, AY, cz-28, BlockTypes.LIME_TERRACOTTA);
            fill(es, cx-12, AY, cz+28, cx+12, AY, cz+40, BlockTypes.LIME_TERRACOTTA);
            fill(es, cx+28, AY, cz-12, cx+40, AY, cz+12, BlockTypes.CYAN_TERRACOTTA);
            fill(es, cx-40, AY, cz-12, cx-28, AY, cz+12, BlockTypes.CYAN_TERRACOTTA);
            fill(es, cx-30, AY, cz-30, cx-18, AY, cz-18, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx+18, AY, cz+18, cx+30, AY, cz+30, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx+18, AY, cz-30, cx+30, AY, cz-18, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx-30, AY, cz+18, cx-18, AY, cz+30, BlockTypes.ORANGE_TERRACOTTA);

            // ── PRISMARINE lagoon floor ────────────────────────────────────
            fill(es, cx-38, AY, cz-38, cx-22, AY, cz-22, BlockTypes.PRISMARINE);
            fill(es, cx+22, AY, cz+22, cx+38, AY, cz+38, BlockTypes.PRISMARINE);
            fill(es, cx+22, AY, cz-38, cx+38, AY, cz-22, BlockTypes.PRISMARINE);
            fill(es, cx-38, AY, cz+22, cx-22, AY, cz+38, BlockTypes.PRISMARINE);

            // ── CORAL BLOCK patches (tropical reef) ──────────────────────
            fill(es, cx-72, AY, cz-35, cx-60, AY, cz-22, BlockTypes.BRAIN_CORAL_BLOCK);
            fill(es, cx+60, AY, cz+22, cx+72, AY, cz+35, BlockTypes.BRAIN_CORAL_BLOCK);
            fill(es, cx-72, AY, cz+22, cx-60, AY, cz+35, BlockTypes.TUBE_CORAL_BLOCK);
            fill(es, cx+60, AY, cz-35, cx+72, AY, cz-22, BlockTypes.TUBE_CORAL_BLOCK);
            fill(es, cx-35, AY, cz-72, cx-22, AY, cz-60, BlockTypes.FIRE_CORAL_BLOCK);
            fill(es, cx+22, AY, cz+60, cx+35, AY, cz+72, BlockTypes.FIRE_CORAL_BLOCK);
            fill(es, cx+22, AY, cz-72, cx+35, AY, cz-60, BlockTypes.HORN_CORAL_BLOCK);
            fill(es, cx-35, AY, cz+60, cx-22, AY, cz+72, BlockTypes.HORN_CORAL_BLOCK);

            // ── SEA LANTERNS under water spots ────────────────────────────
            for (int gx = cx-70; gx <= cx+70; gx += 15) {
                for (int gz = cz-70; gz <= cz+70; gz += 15) {
                    block(es, gx, AY, gz, BlockTypes.SEA_LANTERN);
                }
            }

            // ── BLUE ICE central lagoon ────────────────────────────────────
            fill(es, cx-10, AY, cz-10, cx+10, AY, cz+10, BlockTypes.BLUE_ICE);
            fill(es, cx- 6, 63, cz- 6, cx+ 6, 63, cz+ 6, BlockTypes.SEA_LANTERN);
            fill(es, cx- 6, AY+1, cz- 6, cx+ 6, AY+1, cz+ 6, BlockTypes.WATER);

            // ── GRAVEL shoreline ──────────────────────────────────────────
            fill(es, cx-75, AY, cz-10, cx-65, AY, cz+10, BlockTypes.GRAVEL);
            fill(es, cx+65, AY, cz-10, cx+75, AY, cz+10, BlockTypes.GRAVEL);
            fill(es, cx-10, AY, cz-75, cx+10, AY, cz-65, BlockTypes.GRAVEL);
            fill(es, cx-10, AY, cz+65, cx+10, AY, cz+75, BlockTypes.GRAVEL);

            // ── PALM TREES (spruce log + jungle leaves variant) ───────────
            int[][] palms = {
                {cx-60,cz-20},{cx-60,cz+20},{cx+60,cz-20},{cx+60,cz+20},
                {cx-20,cz-60},{cx+20,cz-60},{cx-20,cz+60},{cx+20,cz+60},
                {cx-55,cz- 5},{cx+55,cz+ 5},{cx- 5,cz-55},{cx+ 5,cz+55},
                {cx-45,cz-45},{cx+45,cz+45},{cx+45,cz-45},{cx-45,cz+45},
                {cx-68,cz-45},{cx+68,cz+45},{cx+68,cz-45},{cx-68,cz+45},
                {cx-45,cz-68},{cx+45,cz+68},{cx+45,cz-68},{cx-45,cz+68}
            };
            for (int[] p : palms) {
                int px = p[0], pz = p[1];
                column(es, px, pz, AY+1, AY+7, BlockTypes.JUNGLE_LOG);
                disk(es, px, AY+8, pz, 4, BlockTypes.JUNGLE_LEAVES);
                disk(es, px, AY+9, pz, 3, BlockTypes.JUNGLE_LEAVES);
                disk(es, px, AY+10, pz, 2, BlockTypes.JUNGLE_LEAVES);
                block(es, px, AY+11, pz, BlockTypes.JUNGLE_LEAVES);
                // Coconuts
                block(es, px+1, AY+8, pz, BlockTypes.MELON);
                block(es, px, AY+8, pz+1, BlockTypes.MELON);
                // Sand base
                disk(es, px, AY, pz, 2, BlockTypes.SAND);
            }

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 330; i++) {
                int sx = cx - 70 + (i * 53 + 29) % 141;
                int sz = cz - 70 + (i * 41 + 23) % 141;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.BRAIN_CORAL_BLOCK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.TUBE_CORAL_BLOCK);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else             block(es, sx, AY, sz, BlockTypes.GRAVEL);
            }

            // ── PERIMETER SANDSTONE WALL ──────────────────────────────────
            fillHollow(es, cx-75, AY+1, cz-75, cx+75, AY+6, cz+75,
                    BlockTypes.SANDSTONE, BlockTypes.AIR);
            // Chiseled sandstone decorations
            for (int d = -65; d <= 65; d += 10) {
                block(es, cx+d, AY+3, cz-75, BlockTypes.CHISELED_SANDSTONE);
                block(es, cx+d, AY+3, cz+75, BlockTypes.CHISELED_SANDSTONE);
                block(es, cx-75, AY+3, cz+d, BlockTypes.CHISELED_SANDSTONE);
                block(es, cx+75, AY+3, cz+d, BlockTypes.CHISELED_SANDSTONE);
            }
            // Sea lantern top cap
            fill(es, cx-75, AY+6, cz-75, cx+75, AY+6, cz+75, BlockTypes.SEA_LANTERN);
            // Corner prismarine towers
            for (int y = AY+1; y <= AY+10; y++) {
                block(es, cx-75, y, cz-75, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx+75, y, cz-75, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx-75, y, cz+75, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx+75, y, cz+75, BlockTypes.PRISMARINE_BRICKS);
            }

            // ── UNDERWATER CORAL GARDEN (sunken lagoon centre) ────────────
            fill(es, cx-8, 61, cz-8, cx+8, 61, cz+8, BlockTypes.SAND);
            fill(es, cx-8, 62, cz-8, cx+8, 62, cz+8, BlockTypes.SAND);
            fill(es, cx-8, AY, cz-8, cx+8, AY, cz+8, BlockTypes.WATER);
            // Coral on lagoon floor
            for (int dc = -6; dc <= 6; dc += 3)
                for (int dz2 = -6; dz2 <= 6; dz2 += 3) {
                    int t = (dc + dz2 + 12) % 5;
                    if      (t == 0) block(es, cx+dc, 62, cz+dz2, BlockTypes.BRAIN_CORAL_BLOCK);
                    else if (t == 1) block(es, cx+dc, 62, cz+dz2, BlockTypes.TUBE_CORAL_BLOCK);
                    else if (t == 2) block(es, cx+dc, 62, cz+dz2, BlockTypes.FIRE_CORAL_BLOCK);
                    else if (t == 3) block(es, cx+dc, 62, cz+dz2, BlockTypes.HORN_CORAL_BLOCK);
                    else             block(es, cx+dc, 62, cz+dz2, BlockTypes.BUBBLE_CORAL_BLOCK);
                }

            // ── TIDAL CHANNELS across the island ─────────────────────────
            fill(es, cx-75, 63, cz-4, cx+75, 63, cz+4, BlockTypes.SAND);
            fill(es, cx-75, AY, cz-4, cx+75, AY, cz+4, BlockTypes.WATER);
            fill(es, cx-4, 63, cz-75, cx+4, 63, cz+75, BlockTypes.SAND);
            fill(es, cx-4, AY, cz-75, cx+4, AY, cz+75, BlockTypes.WATER);

            // ── MULTI-LAYER BEACH DUNES ────────────────────────────────────
            // Outer dunes (ring 1)
            for (int d = -70; d <= 70; d += 4) {
                block(es, cx+d, AY+1, cz-72, BlockTypes.SAND);
                block(es, cx+d, AY+1, cz+72, BlockTypes.SAND);
                block(es, cx-72, AY+1, cz+d, BlockTypes.SAND);
                block(es, cx+72, AY+1, cz+d, BlockTypes.SAND);
            }
            // Inner dunes (ring 2)
            for (int d = -62; d <= 62; d += 5) {
                block(es, cx+d, AY+2, cz-64, BlockTypes.RED_SAND);
                block(es, cx+d, AY+2, cz+64, BlockTypes.RED_SAND);
                block(es, cx-64, AY+2, cz+d, BlockTypes.RED_SAND);
                block(es, cx+64, AY+2, cz+d, BlockTypes.RED_SAND);
            }

            // ── SANDSTONE REEF OUTCROPS ───────────────────────────────────
            int[][] reefs = {
                {cx-55,cz-30,3},{cx+55,cz+30,4},{cx+30,cz-55,3},{cx-30,cz+55,4},
                {cx-55,cz+30,3},{cx+55,cz-30,4},{cx-30,cz-55,3},{cx+30,cz+55,4},
                {cx-70,cz,   2},{cx+70,cz,   3},{cx,cz-70,   3},{cx,cz+70,  2}
            };
            for (int[] rf : reefs) {
                fill(es, rf[0]-2, AY, rf[1]-2, rf[0]+2, AY+rf[2], rf[1]+2,
                        BlockTypes.SANDSTONE);
                fill(es, rf[0]-1, AY+rf[2]+1, rf[1]-1, rf[0]+1, AY+rf[2]+1, rf[1]+1,
                        BlockTypes.BRAIN_CORAL_BLOCK);
            }

            // ── ORANGE TERRACOTTA ISLAND INTERIOR STRIPES ─────────────────
            for (int strip = -50; strip <= 50; strip += 10) {
                fill(es, cx+strip, AY, cz-55, cx+strip+2, AY, cz+55,
                        BlockTypes.ORANGE_TERRACOTTA);
            }
            for (int strip = -50; strip <= 50; strip += 10) {
                fill(es, cx-55, AY, cz+strip, cx+55, AY, cz+strip+2,
                        BlockTypes.YELLOW_TERRACOTTA);
            }

            // ── LIME TERRACOTTA LAGOON SHORE ──────────────────────────────
            fill(es, cx-20, AY, cz-22, cx+20, AY, cz-18, BlockTypes.LIME_TERRACOTTA);
            fill(es, cx-20, AY, cz+18, cx+20, AY, cz+22, BlockTypes.LIME_TERRACOTTA);
            fill(es, cx+18, AY, cz-20, cx+22, AY, cz+20, BlockTypes.LIME_TERRACOTTA);
            fill(es, cx-22, AY, cz-20, cx-18, AY, cz+20, BlockTypes.LIME_TERRACOTTA);

            // ── TROPICAL FLOWER BEDS ──────────────────────────────────────
            for (int i = 0; i < 120; i++) {
                int fx = cx - 70 + (i * 37 + 13) % 141;
                int fz = cz - 70 + (i * 31 + 11) % 141;
                if (i % 4 == 0)      block(es, fx, AY+1, fz, BlockTypes.DANDELION);
                else if (i % 4 == 1) block(es, fx, AY+1, fz, BlockTypes.POPPY);
                else if (i % 4 == 2) block(es, fx, AY+1, fz, BlockTypes.ORANGE_TULIP);
                else                 block(es, fx, AY+1, fz, BlockTypes.SHORT_GRASS);
            }

            // ── RAISED PRISMARINE GAZEBO PLATFORM ─────────────────────────
            fill(es, cx-30, AY+3, cz-30, cx-20, AY+3, cz-20, BlockTypes.PRISMARINE_BRICKS);
            for (int y = AY+1; y <= AY+2; y++) {
                block(es, cx-30, y, cz-30, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx-20, y, cz-30, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx-30, y, cz-20, BlockTypes.PRISMARINE_BRICKS);
                block(es, cx-20, y, cz-20, BlockTypes.PRISMARINE_BRICKS);
            }
            fill(es, cx+20, AY+3, cz+20, cx+30, AY+3, cz+30, BlockTypes.PRISMARINE_BRICKS);

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 68 + (i * 73 + 47) % 137;
                int sz = cz - 68 + (i * 67 + 43) % 137;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.LIME_TERRACOTTA);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else             block(es, sx, AY, sz, BlockTypes.CLAY);
            }

            // ── COLOURED CORAL TIDAL GARDEN ───────────────────────────────
            // Scattered living coral in shallow areas around island
            int[][] cGarden = {
                {cx-68,cz-20},{cx-68,cz+20},{cx+68,cz-20},{cx+68,cz+20},
                {cx-20,cz-68},{cx+20,cz-68},{cx-20,cz+68},{cx+20,cz+68},
                {cx-55,cz-55},{cx+55,cz+55},{cx+55,cz-55},{cx-55,cz+55},
                {cx-68,cz-50},{cx+68,cz+50},{cx+68,cz-50},{cx-68,cz+50}
            };
            BlockType[] cgTypes = {
                BlockTypes.BRAIN_CORAL_BLOCK, BlockTypes.TUBE_CORAL_BLOCK,
                BlockTypes.FIRE_CORAL_BLOCK,  BlockTypes.HORN_CORAL_BLOCK,
                BlockTypes.BUBBLE_CORAL_BLOCK
            };
            for (int i2 = 0; i2 < cGarden.length; i2++) {
                int cgx = cGarden[i2][0], cgz = cGarden[i2][1];
                fill(es, cgx-3, AY, cgz-3, cgx+3, AY, cgz+3, BlockTypes.SAND);
                fill(es, cgx-3, AY, cgz-3, cgx+3, AY, cgz+3, BlockTypes.WATER);
                disk(es, cgx, AY, cgz, 3, cgTypes[i2 % cgTypes.length]);
                block(es, cgx, AY+1, cgz, BlockTypes.SEA_LANTERN);
            }

            // ── BEACH GRASS PLANTING (short grass on elevated sand) ───────
            for (int i2 = 0; i2 < 120; i2++) {
                int gx = cx - 70 + (i2 * 43 + 19) % 141;
                int gz = cz - 70 + (i2 * 37 + 13) % 141;
                block(es, gx, AY+1, gz, (i2 % 3 == 0) ? BlockTypes.SHORT_GRASS : BlockTypes.FERN);
            }

            // ── PRISMARINE LIGHTHOUSE COLUMN ──────────────────────────────
            column(es, cx-68, cz+68, AY+1, AY+12, BlockTypes.PRISMARINE_BRICKS);
            block(es, cx-68, AY+13, cz+68, BlockTypes.SEA_LANTERN);
            disk(es, cx-68, AY+12, cz+68, 2, BlockTypes.PRISMARINE_BRICKS);
            column(es, cx+68, cz-68, AY+1, AY+10, BlockTypes.PRISMARINE_BRICKS);
            block(es, cx+68, AY+11, cz-68, BlockTypes.SEA_LANTERN);

            // ── RAINBOW TERRACOTTA FLOOR RINGS ────────────────────────────
            BlockType[] ringColors = {
                BlockTypes.RED_TERRACOTTA, BlockTypes.ORANGE_TERRACOTTA,
                BlockTypes.YELLOW_TERRACOTTA, BlockTypes.LIME_TERRACOTTA,
                BlockTypes.CYAN_TERRACOTTA, BlockTypes.BLUE_TERRACOTTA
            };
            for (int ri = 5; ri <= 30; ri += 5) {
                BlockType rc = ringColors[(ri / 5 - 1) % ringColors.length];
                for (double a = 0; a < 2 * Math.PI; a += 0.15) {
                    int rx = cx + (int) Math.round(ri * Math.cos(a));
                    int rz = cz + (int) Math.round(ri * Math.sin(a));
                    block(es, rx, AY, rz, rc);
                }
            }

            // ── FOURTH SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 320; i++) {
                int sx = cx - 68 + (i * 101 + 71) % 137;
                int sz = cz - 68 + (i * 97 + 67)  % 137;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.CYAN_TERRACOTTA);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.BLUE_TERRACOTTA);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else             block(es, sx, AY, sz, BlockTypes.LIME_TERRACOTTA);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-2, AY, cz-52, cx+2, AY, cz-49, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-2, AY, cz+49, cx+2, AY, cz+52, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-52, AY, cz-2, cx-49, AY, cz+2, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx+49, AY, cz-2, cx+52, AY, cz+2, BlockTypes.SMOOTH_SANDSTONE);
            fill(es, cx-38, AY, cz-38, cx-35, AY, cz-35, BlockTypes.SEA_LANTERN);
            fill(es, cx+35, AY, cz+35, cx+38, AY, cz+38, BlockTypes.SEA_LANTERN);
            fill(es, cx+35, AY, cz-38, cx+38, AY, cz-35, BlockTypes.SEA_LANTERN);
            fill(es, cx-38, AY, cz+35, cx-35, AY, cz+38, BlockTypes.SEA_LANTERN);
        }
    }

    // =========================================================================
    // FFA 3 — STEP PYRAMID   cx=0, cz=500   (150x150)
    // Grand desert arena: cut sandstone, red sandstone, terracotta bands,
    // sand dunes, coarse dirt paths, warm colour palette — massive 6-tier pyramid
    // =========================================================================
    private void buildPyramidFFA() {
        final int cx = 0, cz = 500;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-75, 55, cz-75, cx+75, 63, cz+75, BlockTypes.SANDSTONE);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.SAND);

            // ── TERRACOTTA COLOUR BANDS (desert strata) ───────────────────
            // Outer ring: red/orange
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz-65, BlockTypes.RED_TERRACOTTA);
            fill(es, cx-75, AY, cz+65, cx+75, AY, cz+75, BlockTypes.RED_TERRACOTTA);
            fill(es, cx-75, AY, cz-65, cx-65, AY, cz+65, BlockTypes.RED_TERRACOTTA);
            fill(es, cx+65, AY, cz-65, cx+75, AY, cz+65, BlockTypes.RED_TERRACOTTA);
            // Second ring: orange
            fill(es, cx-65, AY, cz-65, cx+65, AY, cz-55, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx-65, AY, cz+55, cx+65, AY, cz+65, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx-65, AY, cz-55, cx-55, AY, cz+55, BlockTypes.ORANGE_TERRACOTTA);
            fill(es, cx+55, AY, cz-55, cx+65, AY, cz+55, BlockTypes.ORANGE_TERRACOTTA);
            // Third ring: yellow
            fill(es, cx-55, AY, cz-55, cx+55, AY, cz-45, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx-55, AY, cz+45, cx+55, AY, cz+55, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx-55, AY, cz-45, cx-45, AY, cz+45, BlockTypes.YELLOW_TERRACOTTA);
            fill(es, cx+45, AY, cz-45, cx+55, AY, cz+45, BlockTypes.YELLOW_TERRACOTTA);
            // Fourth ring: white
            fill(es, cx-45, AY, cz-45, cx+45, AY, cz-35, BlockTypes.WHITE_TERRACOTTA);
            fill(es, cx-45, AY, cz+35, cx+45, AY, cz+45, BlockTypes.WHITE_TERRACOTTA);
            fill(es, cx-45, AY, cz-35, cx-35, AY, cz+35, BlockTypes.WHITE_TERRACOTTA);
            fill(es, cx+35, AY, cz-35, cx+45, AY, cz+35, BlockTypes.WHITE_TERRACOTTA);
            // Fifth ring: light grey
            fill(es, cx-35, AY, cz-35, cx+35, AY, cz-25, BlockTypes.LIGHT_GRAY_TERRACOTTA);
            fill(es, cx-35, AY, cz+25, cx+35, AY, cz+35, BlockTypes.LIGHT_GRAY_TERRACOTTA);
            fill(es, cx-35, AY, cz-25, cx-25, AY, cz+25, BlockTypes.LIGHT_GRAY_TERRACOTTA);
            fill(es, cx+25, AY, cz-25, cx+35, AY, cz+25, BlockTypes.LIGHT_GRAY_TERRACOTTA);
            // Centre: sandstone
            fill(es, cx-25, AY, cz-25, cx+25, AY, cz+25, BlockTypes.SANDSTONE);

            // ── RED SANDSTONE patches and paths ───────────────────────────
            fill(es, cx-75, AY, cz-25, cx-55, AY, cz+25, BlockTypes.RED_SANDSTONE);
            fill(es, cx+55, AY, cz-25, cx+75, AY, cz+25, BlockTypes.RED_SANDSTONE);
            fill(es, cx-25, AY, cz-75, cx+25, AY, cz-55, BlockTypes.RED_SANDSTONE);
            fill(es, cx-25, AY, cz+55, cx+25, AY, cz+75, BlockTypes.RED_SANDSTONE);
            // Diagonal paths
            for (int r = 20; r <= 65; r++) {
                block(es, cx+r, AY, cz+r, BlockTypes.SMOOTH_RED_SANDSTONE);
                block(es, cx-r, AY, cz+r, BlockTypes.SMOOTH_RED_SANDSTONE);
                block(es, cx+r, AY, cz-r, BlockTypes.SMOOTH_RED_SANDSTONE);
                block(es, cx-r, AY, cz-r, BlockTypes.SMOOTH_RED_SANDSTONE);
            }

            // ── COARSE DIRT paths (N/S/E/W cross) ─────────────────────────
            fill(es, cx-75, AY, cz-2, cx-25, AY, cz+2, BlockTypes.COARSE_DIRT);
            fill(es, cx+25, AY, cz-2, cx+75, AY, cz+2, BlockTypes.COARSE_DIRT);
            fill(es, cx-2, AY, cz-75, cx+2, AY, cz-25, BlockTypes.COARSE_DIRT);
            fill(es, cx-2, AY, cz+25, cx+2, AY, cz+75, BlockTypes.COARSE_DIRT);

            // ── SAND DUNES at corners ─────────────────────────────────────
            fill(es, cx-75, AY, cz-75, cx-60, AY+2, cz-60, BlockTypes.SAND);
            fill(es, cx+60, AY, cz+60, cx+75, AY+2, cz+75, BlockTypes.SAND);
            fill(es, cx+60, AY, cz-75, cx+75, AY+2, cz-60, BlockTypes.SAND);
            fill(es, cx-75, AY, cz+60, cx-60, AY+2, cz+75, BlockTypes.SAND);
            fill(es, cx-65, AY, cz-40, cx-50, AY+1, cz-25, BlockTypes.SAND);
            fill(es, cx+50, AY, cz+25, cx+65, AY+1, cz+40, BlockTypes.SAND);
            fill(es, cx+50, AY, cz-40, cx+65, AY+1, cz-25, BlockTypes.SAND);
            fill(es, cx-65, AY, cz+25, cx-50, AY+1, cz+40, BlockTypes.SAND);

            // ── 6-TIER STEP PYRAMID ───────────────────────────────────────
            fill(es, cx-22, AY+1, cz-22, cx+22, AY+4, cz+22, BlockTypes.SANDSTONE);
            fill(es, cx-17, AY+5, cz-17, cx+17, AY+8, cz+17, BlockTypes.CUT_SANDSTONE);
            fill(es, cx-12, AY+9, cz-12, cx+12, AY+12, cz+12, BlockTypes.SANDSTONE);
            fill(es, cx- 8, AY+13,cz- 8, cx+ 8, AY+16, cz+ 8, BlockTypes.CUT_SANDSTONE);
            fill(es, cx- 4, AY+17,cz- 4, cx+ 4, AY+20, cz+ 4, BlockTypes.SANDSTONE);
            fill(es, cx- 1, AY+21,cz- 1, cx+ 1, AY+24, cz+ 1, BlockTypes.GOLD_BLOCK);
            block(es, cx, AY+25, cz, BlockTypes.BEACON);
            // Hollow each tier
            fillHollow(es, cx-21, AY+1, cz-21, cx+21, AY+4, cz+21,
                    BlockTypes.SANDSTONE, BlockTypes.AIR);
            fillHollow(es, cx-16, AY+5, cz-16, cx+16, AY+8, cz+16,
                    BlockTypes.CUT_SANDSTONE, BlockTypes.AIR);
            fillHollow(es, cx-11, AY+9, cz-11, cx+11, AY+12, cz+11,
                    BlockTypes.SANDSTONE, BlockTypes.AIR);
            // Red sandstone accent bands on tier edges
            for (int y = AY+1; y <= AY+24; y += 4) {
                for (int d = -22; d <= 22; d++) {
                    if (y <= AY+4) {
                        block(es, cx+d, y, cz-22, BlockTypes.RED_SANDSTONE);
                        block(es, cx+d, y, cz+22, BlockTypes.RED_SANDSTONE);
                        block(es, cx-22, y, cz+d, BlockTypes.RED_SANDSTONE);
                        block(es, cx+22, y, cz+d, BlockTypes.RED_SANDSTONE);
                    }
                }
            }

            // ── BURIED SAND COLUMNS ────────────────────────────────────────
            int[][] cols = {
                {cx-55,cz-55,5},{cx+55,cz+55,4},{cx+55,cz-55,6},{cx-55,cz+55,5},
                {cx-70,cz,   3},{cx+70,cz,   4},{cx,cz-70,   5},{cx,cz+70,  3},
                {cx-50,cz+30,4},{cx+50,cz-30,4},{cx+30,cz-50,5},{cx-30,cz+50,4}
            };
            for (int[] c : cols) {
                column(es, c[0], c[1], AY+1, AY+c[2], BlockTypes.CUT_SANDSTONE);
                block(es, c[0], AY+c[2]+1, c[1], BlockTypes.CHISELED_SANDSTONE);
            }

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 330; i++) {
                int sx = cx - 70 + (i * 59 + 31) % 141;
                int sz = cz - 70 + (i * 47 + 29) % 141;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.RED_SANDSTONE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.CUT_SANDSTONE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.RED_TERRACOTTA);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.WHITE_TERRACOTTA);
                else             block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
            }

            // ── PERIMETER SANDSTONE WALL ──────────────────────────────────
            fillHollow(es, cx-75, AY+1, cz-75, cx+75, AY+7, cz+75,
                    BlockTypes.SANDSTONE, BlockTypes.AIR);
            fill(es, cx-75, AY+7, cz-75, cx+75, AY+7, cz+75, BlockTypes.CHISELED_SANDSTONE);
            // Red sandstone accent strips
            for (int d = -70; d <= 70; d += 5) {
                block(es, cx+d, AY+4, cz-75, BlockTypes.RED_SANDSTONE);
                block(es, cx+d, AY+4, cz+75, BlockTypes.RED_SANDSTONE);
                block(es, cx-75, AY+4, cz+d, BlockTypes.RED_SANDSTONE);
                block(es, cx+75, AY+4, cz+d, BlockTypes.RED_SANDSTONE);
            }
            // Corner gold towers
            for (int y = AY+1; y <= AY+12; y++) {
                block(es, cx-75, y, cz-75, BlockTypes.GOLD_BLOCK);
                block(es, cx+75, y, cz-75, BlockTypes.GOLD_BLOCK);
                block(es, cx-75, y, cz+75, BlockTypes.GOLD_BLOCK);
                block(es, cx+75, y, cz+75, BlockTypes.GOLD_BLOCK);
            }

            // ── UNDERGROUND CRYPT BENEATH PYRAMID ─────────────────────────
            fill(es, cx-18, 56, cz-18, cx+18, 62, cz+18, BlockTypes.AIR);
            fill(es, cx-18, 55, cz-18, cx+18, 55, cz+18, BlockTypes.SANDSTONE);
            fillHollow(es, cx-18, 56, cz-18, cx+18, 62, cz+18,
                    BlockTypes.CUT_SANDSTONE, BlockTypes.AIR);
            // Crypt floor pattern
            fill(es, cx-16, 56, cz-16, cx+16, 56, cz+16, BlockTypes.CHISELED_SANDSTONE);
            for (int dc = -14; dc <= 14; dc += 4)
                for (int dz2 = -14; dz2 <= 14; dz2 += 4)
                    block(es, cx+dc, 56, cz+dz2, BlockTypes.SMOOTH_SANDSTONE);
            // Crypt ceiling hieroglyphs
            for (int dc = -16; dc <= 16; dc += 3)
                block(es, cx+dc, 62, cz, BlockTypes.CHISELED_SANDSTONE);
            for (int dz2 = -16; dz2 <= 16; dz2 += 3)
                block(es, cx, 62, cz+dz2, BlockTypes.CHISELED_SANDSTONE);

            // ── SANDSTONE OBELISKS at the four corners of the plaza ────────
            int[][] obelisks = {
                {cx-60,cz-60,10},{cx+60,cz+60,10},{cx+60,cz-60,10},{cx-60,cz+60,10},
                {cx-42,cz,    8},{cx+42,cz,    8},{cx,cz-42,    8},{cx,cz+42,   8}
            };
            for (int[] ob : obelisks) {
                int ox = ob[0], oz = ob[1], oh = ob[2];
                fill(es, ox-1, AY+1, oz-1, ox+1, AY+oh, oz+1, BlockTypes.SANDSTONE);
                block(es, ox, AY+oh+1, oz, BlockTypes.CHISELED_SANDSTONE);
                block(es, ox, AY+oh+2, oz, BlockTypes.GOLD_BLOCK);
                disk(es, ox, AY, oz, 3, BlockTypes.SMOOTH_SANDSTONE);
            }

            // ── SAND DUNE FIELD (stacked ridges) ─────────────────────────
            for (int ridge = 0; ridge <= 5; ridge++) {
                int offset = ridge * 10;
                fill(es, cx-75+offset, AY, cz-75+offset, cx-70+offset, AY+ridge, cz+75-offset,
                        BlockTypes.SAND);
                fill(es, cx+70-offset, AY, cz-75+offset, cx+75-offset, AY+ridge, cz+75-offset,
                        BlockTypes.SAND);
            }

            // ── TERRACOTTA WIND-CARVED ARCHES ─────────────────────────────
            for (int arch = -60; arch <= 60; arch += 30) {
                for (int y = AY+1; y <= AY+5; y++) {
                    block(es, cx+arch, y, cz-75, BlockTypes.RED_TERRACOTTA);
                    block(es, cx+arch, y, cz+75, BlockTypes.RED_TERRACOTTA);
                }
                block(es, cx+arch, AY+6, cz-75, BlockTypes.CHISELED_SANDSTONE);
                block(es, cx+arch, AY+6, cz+75, BlockTypes.CHISELED_SANDSTONE);
            }

            // ── GOLDEN INLAY FLOOR LINES ──────────────────────────────────
            for (int d = -70; d <= 70; d += 7) {
                block(es, cx+d, AY, cz,   BlockTypes.GOLD_BLOCK);
                block(es, cx,   AY, cz+d, BlockTypes.GOLD_BLOCK);
            }
            for (int d = -50; d <= 50; d += 7) {
                block(es, cx+d, AY, cz+d, BlockTypes.GOLD_BLOCK);
                block(es, cx+d, AY, cz-d, BlockTypes.GOLD_BLOCK);
            }

            // ── COARSE DIRT CARAVAN TRACKS ────────────────────────────────
            fill(es, cx-75, AY+1, cz-8, cx-25, AY+1, cz+8, BlockTypes.COARSE_DIRT);
            fill(es, cx+25, AY+1, cz-8, cx+75, AY+1, cz+8, BlockTypes.COARSE_DIRT);
            fill(es, cx-8, AY+1, cz-75, cx+8, AY+1, cz-25, BlockTypes.COARSE_DIRT);
            fill(es, cx-8, AY+1, cz+25, cx+8, AY+1, cz+75, BlockTypes.COARSE_DIRT);

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 310; i++) {
                int sx = cx - 68 + (i * 79 + 53) % 137;
                int sz = cz - 68 + (i * 73 + 47) % 137;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.CUT_SANDSTONE);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.CHISELED_SANDSTONE);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.SMOOTH_SANDSTONE);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.RED_SANDSTONE);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else             block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-2, AY, cz-52, cx+2, AY, cz-49, BlockTypes.GOLD_BLOCK);
            fill(es, cx-2, AY, cz+49, cx+2, AY, cz+52, BlockTypes.GOLD_BLOCK);
            fill(es, cx-52, AY, cz-2, cx-49, AY, cz+2, BlockTypes.GOLD_BLOCK);
            fill(es, cx+49, AY, cz-2, cx+52, AY, cz+2, BlockTypes.GOLD_BLOCK);
        }
    }

    // =========================================================================
    // FFA 4 — MANGROVE BAY   cx=0, cz=-500   (150x150)
    // Sprawling tidal bay: mud, clay, mangrove roots, moss, gravel, river clay,
    // large water channels, dense mangrove stands — massive open wetland feel
    // =========================================================================
    private void buildMangroveBayFFA() {
        final int cx = 0, cz = -500;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-75, 55, cz-75, cx+75, 63, cz+75, BlockTypes.DIRT);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.MUD);

            // ── MUDDY MANGROVE ROOTS vast swaths ─────────────────────────
            fill(es, cx-70, AY, cz-70, cx-35, AY, cz-35, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+35, AY, cz+35, cx+70, AY, cz+70, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-70, AY, cz+35, cx-35, AY, cz+70, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+35, AY, cz-70, cx+70, AY, cz-35, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-20, AY, cz-20, cx+20, AY, cz+20, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-75, AY, cz-20, cx-45, AY, cz+20, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx+45, AY, cz-20, cx+75, AY, cz+20, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-20, AY, cz-75, cx+20, AY, cz-45, BlockTypes.MUDDY_MANGROVE_ROOTS);
            fill(es, cx-20, AY, cz+45, cx+20, AY, cz+75, BlockTypes.MUDDY_MANGROVE_ROOTS);

            // ── CLAY tidal flats ──────────────────────────────────────────
            fill(es, cx-35, AY, cz-35, cx-15, AY, cz-15, BlockTypes.CLAY);
            fill(es, cx+15, AY, cz+15, cx+35, AY, cz+35, BlockTypes.CLAY);
            fill(es, cx+15, AY, cz-35, cx+35, AY, cz-15, BlockTypes.CLAY);
            fill(es, cx-35, AY, cz+15, cx-15, AY, cz+35, BlockTypes.CLAY);
            fill(es, cx-75, AY, cz-35, cx-60, AY, cz-15, BlockTypes.CLAY);
            fill(es, cx+60, AY, cz+15, cx+75, AY, cz+35, BlockTypes.CLAY);
            fill(es, cx-35, AY, cz-75, cx-15, AY, cz-60, BlockTypes.CLAY);
            fill(es, cx+15, AY, cz+60, cx+35, AY, cz+75, BlockTypes.CLAY);
            fill(es, cx+60, AY, cz-35, cx+75, AY, cz-15, BlockTypes.CLAY);
            fill(es, cx-75, AY, cz+15, cx-60, AY, cz+35, BlockTypes.CLAY);
            fill(es, cx+15, AY, cz-75, cx+35, AY, cz-60, BlockTypes.CLAY);
            fill(es, cx-35, AY, cz+60, cx-15, AY, cz+75, BlockTypes.CLAY);

            // ── GRAVEL channels and shoals ────────────────────────────────
            fill(es, cx-75, AY, cz- 8, cx+75, AY, cz+ 8, BlockTypes.GRAVEL);
            fill(es, cx- 8, AY, cz-75, cx+ 8, AY, cz+75, BlockTypes.GRAVEL);
            fill(es, cx-50, AY, cz-50, cx-38, AY, cz-38, BlockTypes.GRAVEL);
            fill(es, cx+38, AY, cz+38, cx+50, AY, cz+50, BlockTypes.GRAVEL);
            fill(es, cx+38, AY, cz-50, cx+50, AY, cz-38, BlockTypes.GRAVEL);
            fill(es, cx-50, AY, cz+38, cx-38, AY, cz+50, BlockTypes.GRAVEL);

            // ── MOSS BLOCK elevated hummocks ─────────────────────────────
            fill(es, cx-75, AY, cz-75, cx-62, AY+1, cz-62, BlockTypes.MOSS_BLOCK);
            fill(es, cx+62, AY, cz+62, cx+75, AY+1, cz+75, BlockTypes.MOSS_BLOCK);
            fill(es, cx+62, AY, cz-75, cx+75, AY+1, cz-62, BlockTypes.MOSS_BLOCK);
            fill(es, cx-75, AY, cz+62, cx-62, AY+1, cz+75, BlockTypes.MOSS_BLOCK);
            fill(es, cx-30, AY, cz+48, cx-15, AY+1, cz+62, BlockTypes.MOSS_BLOCK);
            fill(es, cx+15, AY, cz-62, cx+30, AY+1, cz-48, BlockTypes.MOSS_BLOCK);
            fill(es, cx+48, AY, cz+15, cx+62, AY+1, cz+30, BlockTypes.MOSS_BLOCK);
            fill(es, cx-62, AY, cz-30, cx-48, AY+1, cz-15, BlockTypes.MOSS_BLOCK);

            // ── COARSE DIRT higher ground islands ─────────────────────────
            fill(es, cx-48, AY, cz-62, cx-30, AY+1, cz-48, BlockTypes.COARSE_DIRT);
            fill(es, cx+30, AY, cz+48, cx+48, AY+1, cz+62, BlockTypes.COARSE_DIRT);
            fill(es, cx+48, AY, cz-62, cx+62, AY+1, cz-48, BlockTypes.COARSE_DIRT);
            fill(es, cx-62, AY, cz+48, cx-48, AY+1, cz+62, BlockTypes.COARSE_DIRT);

            // ── WATER CHANNELS ────────────────────────────────────────────
            // Main tidal channel (N-S)
            fill(es, cx-10, 63, cz-75, cx+10, 63, cz+75, BlockTypes.CLAY);
            fill(es, cx-10, AY, cz-75, cx+10, AY, cz+75, BlockTypes.WATER);
            // Cross channel (E-W)
            fill(es, cx-75, 63, cz-10, cx+75, 63, cz+10, BlockTypes.CLAY);
            fill(es, cx-75, AY, cz-10, cx+75, AY, cz+10, BlockTypes.WATER);
            // Mangrove pools (NW, NE, SW, SE)
            fill(es, cx-65, 63, cz-65, cx-45, 63, cz-48, BlockTypes.GRAVEL);
            fill(es, cx-65, AY, cz-65, cx-45, AY, cz-48, BlockTypes.WATER);
            fill(es, cx+45, 63, cz+48, cx+65, 63, cz+65, BlockTypes.GRAVEL);
            fill(es, cx+45, AY, cz+48, cx+65, AY, cz+65, BlockTypes.WATER);
            fill(es, cx+45, 63, cz-65, cx+65, 63, cz-48, BlockTypes.GRAVEL);
            fill(es, cx+45, AY, cz-65, cx+65, AY, cz-48, BlockTypes.WATER);
            fill(es, cx-65, 63, cz+48, cx-45, 63, cz+65, BlockTypes.GRAVEL);
            fill(es, cx-65, AY, cz+48, cx-45, AY, cz+65, BlockTypes.WATER);

            // ── MANGROVE TREE CLUSTERS ─────────────────────────────────────
            int[][] mTrees = {
                {cx-55,cz-20},{cx-58,cz-15},{cx-52,cz-25},{cx-55,cz-28},
                {cx+55,cz+20},{cx+58,cz+15},{cx+52,cz+25},{cx+55,cz+28},
                {cx-20,cz-55},{cx-15,cz-58},{cx-25,cz-52},{cx-28,cz-55},
                {cx+20,cz+55},{cx+15,cz+58},{cx+25,cz+52},{cx+28,cz+55},
                {cx-55,cz+20},{cx-58,cz+15},{cx-52,cz+25},{cx-55,cz+28},
                {cx+55,cz-20},{cx+58,cz-15},{cx+52,cz-25},{cx+55,cz-28},
                {cx+20,cz-55},{cx+15,cz-58},{cx+25,cz-52},{cx+28,cz-55},
                {cx-20,cz+55},{cx-15,cz+58},{cx-25,cz+52},{cx-28,cz+55},
                {cx-40,cz-40},{cx-42,cz-38},{cx-38,cz-42},{cx-35,cz-45},
                {cx+40,cz+40},{cx+42,cz+38},{cx+38,cz+42},{cx+35,cz+45},
                {cx+40,cz-40},{cx+42,cz-38},{cx+38,cz-42},{cx+35,cz-45},
                {cx-40,cz+40},{cx-42,cz+38},{cx-38,cz+42},{cx-35,cz+45}
            };
            for (int[] t : mTrees) {
                int tx = t[0], tz = t[1];
                block(es, tx, AY, tz, BlockTypes.MANGROVE_ROOTS);
                column(es, tx, tz, AY+1, AY+6, BlockTypes.MANGROVE_LOG);
                disk(es, tx, AY+7, tz, 4, BlockTypes.MANGROVE_LEAVES);
                disk(es, tx, AY+8, tz, 3, BlockTypes.MANGROVE_LEAVES);
                disk(es, tx, AY+9, tz, 2, BlockTypes.MANGROVE_LEAVES);
                block(es, tx, AY+10, tz, BlockTypes.MANGROVE_LEAVES);
                // Prop roots
                block(es, tx+2, AY, tz,   BlockTypes.MANGROVE_ROOTS);
                block(es, tx-2, AY, tz,   BlockTypes.MANGROVE_ROOTS);
                block(es, tx,   AY, tz+2, BlockTypes.MANGROVE_ROOTS);
                block(es, tx,   AY, tz-2, BlockTypes.MANGROVE_ROOTS);
            }

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 350; i++) {
                int sx = cx - 70 + (i * 61 + 37) % 141;
                int sz = cz - 70 + (i * 53 + 31) % 141;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.MANGROVE_ROOTS);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.DIRT);
                else             block(es, sx, AY, sz, BlockTypes.SAND);
            }

            // ── PERIMETER MANGROVE WALL ───────────────────────────────────
            fillHollow(es, cx-75, AY+1, cz-75, cx+75, AY+7, cz+75,
                    BlockTypes.MANGROVE_LOG, BlockTypes.AIR);
            fill(es, cx-75, AY+7, cz-75, cx+75, AY+7, cz+75,
                    BlockTypes.MANGROVE_PLANKS);
            // Moss patches on wall
            for (int d = -65; d <= 65; d += 10) {
                for (int y = AY+2; y <= AY+6; y += 2) {
                    block(es, cx+d, y, cz-75, BlockTypes.MOSS_BLOCK);
                    block(es, cx+d, y, cz+75, BlockTypes.MOSS_BLOCK);
                    block(es, cx-75, y, cz+d, BlockTypes.MOSS_BLOCK);
                    block(es, cx+75, y, cz+d, BlockTypes.MOSS_BLOCK);
                }
            }
            // Corner root towers
            for (int y = AY+1; y <= AY+12; y++) {
                block(es, cx-75, y, cz-75, BlockTypes.MANGROVE_ROOTS);
                block(es, cx+75, y, cz-75, BlockTypes.MANGROVE_ROOTS);
                block(es, cx-75, y, cz+75, BlockTypes.MANGROVE_ROOTS);
                block(es, cx+75, y, cz+75, BlockTypes.MANGROVE_ROOTS);
            }

            // ── DEEP TIDAL BASIN (larger sunken area) ─────────────────────
            fill(es, cx-18, 60, cz-18, cx+18, 63, cz+18, BlockTypes.CLAY);
            fill(es, cx-18, AY, cz-18, cx+18, AY, cz+18, BlockTypes.WATER);
            // Prismarine border around basin
            for (int d = -18; d <= 18; d++) {
                block(es, cx+d, AY+1, cz-18, BlockTypes.PRISMARINE);
                block(es, cx+d, AY+1, cz+18, BlockTypes.PRISMARINE);
                block(es, cx-18, AY+1, cz+d, BlockTypes.PRISMARINE);
                block(es, cx+18, AY+1, cz+d, BlockTypes.PRISMARINE);
            }

            // ── CLAY DELTA FORMATIONS ──────────────────────────────────────
            // River deltas fanning into the sea
            for (int fan = 0; fan <= 5; fan++) {
                int fw = fan;
                fill(es, cx-fw, AY, cz-74+fan, cx+fw, AY, cz-70+fan, BlockTypes.CLAY);
                fill(es, cx-fw, AY, cz+70-fan, cx+fw, AY, cz+74-fan, BlockTypes.CLAY);
                fill(es, cx-74+fan, AY, cz-fw, cx-70+fan, AY, cz+fw, BlockTypes.CLAY);
                fill(es, cx+70-fan, AY, cz-fw, cx+74-fan, AY, cz+fw, BlockTypes.CLAY);
            }

            // ── MANGROVE LOG RAFT PLATFORM ────────────────────────────────
            fill(es, cx-12, AY+2, cz-12, cx+12, AY+2, cz+12, BlockTypes.MANGROVE_LOG);
            fill(es, cx-10, AY+3, cz-10, cx+10, AY+3, cz+10, BlockTypes.MANGROVE_PLANKS);
            // Raft post anchors
            column(es, cx-10, cz-10, AY, AY+1, BlockTypes.MANGROVE_LOG);
            column(es, cx+10, cz-10, AY, AY+1, BlockTypes.MANGROVE_LOG);
            column(es, cx-10, cz+10, AY, AY+1, BlockTypes.MANGROVE_LOG);
            column(es, cx+10, cz+10, AY, AY+1, BlockTypes.MANGROVE_LOG);

            // ── SILT BANK ISLANDS (elevated mud islands) ──────────────────
            fill(es, cx-55, AY, cz-10, cx-42, AY+2, cz+10, BlockTypes.MUD);
            fill(es, cx+42, AY, cz-10, cx+55, AY+2, cz+10, BlockTypes.MUD);
            fill(es, cx-10, AY, cz-55, cx+10, AY+2, cz-42, BlockTypes.MUD);
            fill(es, cx-10, AY, cz+42, cx+10, AY+2, cz+55, BlockTypes.MUD);
            // Moss top on islands
            fill(es, cx-55, AY+3, cz-10, cx-42, AY+3, cz+10, BlockTypes.MOSS_BLOCK);
            fill(es, cx+42, AY+3, cz-10, cx+55, AY+3, cz+10, BlockTypes.MOSS_BLOCK);
            fill(es, cx-10, AY+3, cz-55, cx+10, AY+3, cz-42, BlockTypes.MOSS_BLOCK);
            fill(es, cx-10, AY+3, cz+42, cx+10, AY+3, cz+55, BlockTypes.MOSS_BLOCK);

            // ── ROOT ARCH BRIDGES over channels ───────────────────────────
            // N-S bridge
            for (int bx = -4; bx <= 4; bx++) {
                block(es, cx+bx, AY+2, cz-12, BlockTypes.MANGROVE_LOG);
                block(es, cx+bx, AY+2, cz+12, BlockTypes.MANGROVE_LOG);
            }
            for (int bz2 = -4; bz2 <= 4; bz2++) {
                block(es, cx-12, AY+2, cz+bz2, BlockTypes.MANGROVE_LOG);
                block(es, cx+12, AY+2, cz+bz2, BlockTypes.MANGROVE_LOG);
            }

            // ── SALT MARSH SEDGE GRASS ────────────────────────────────────
            for (int i = 0; i < 200; i++) {
                int sx = cx - 70 + (i * 43 + 17) % 141;
                int sz = cz - 70 + (i * 37 + 13) % 141;
                if (i % 3 == 0) block(es, sx, AY+1, sz, BlockTypes.SHORT_GRASS);
                else if (i % 3 == 1) block(es, sx, AY+1, sz, BlockTypes.FERN);
                else block(es, sx, AY+1, sz, BlockTypes.LARGE_FERN);
            }

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 300; i++) {
                int sx = cx - 68 + (i * 83 + 59) % 137;
                int sz = cz - 68 + (i * 79 + 53) % 137;
                int r  = i % 9;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.MANGROVE_ROOTS);
                else             block(es, sx, AY, sz, BlockTypes.SAND);
            }

            // ── TIDAL FLAT MICRO-RIDGES ────────────────────────────────────
            for (int rid = -70; rid <= 70; rid += 7) {
                fill(es, cx+rid, AY+1, cz-70, cx+rid, AY+1, cz+70, BlockTypes.MUDDY_MANGROVE_ROOTS);
                fill(es, cx-70, AY+1, cz+rid, cx+70, AY+1, cz+rid, BlockTypes.MUDDY_MANGROVE_ROOTS);
            }

            // ── FLOATING MANGROVE LEAF CANOPY LAYER ───────────────────────
            for (int i2 = 0; i2 < 100; i2++) {
                int lx = cx - 60 + (i2 * 29 + 11) % 121;
                int lz = cz - 60 + (i2 * 23 + 7)  % 121;
                if (i2 % 2 == 0) block(es, lx, AY+12, lz, BlockTypes.MANGROVE_LEAVES);
            }

            // ── ESTUARY SAND BARS ─────────────────────────────────────────
            fill(es, cx-12, AY+1, cz-72, cx+12, AY+1, cz-65, BlockTypes.SAND);
            fill(es, cx-12, AY+1, cz+65, cx+12, AY+1, cz+72, BlockTypes.SAND);
            fill(es, cx-72, AY+1, cz-12, cx-65, AY+1, cz+12, BlockTypes.SAND);
            fill(es, cx+65, AY+1, cz-12, cx+72, AY+1, cz+12, BlockTypes.SAND);

            // ── PEAT BOG SECTIONS ──────────────────────────────────────────
            fill(es, cx+30, AY, cz-55, cx+50, AY+1, cz-35, BlockTypes.DIRT);
            fill(es, cx+30, AY+2, cz-55, cx+50, AY+2, cz-35, BlockTypes.PODZOL);
            fill(es, cx-50, AY, cz+35, cx-30, AY+1, cz+55, BlockTypes.DIRT);
            fill(es, cx-50, AY+2, cz+35, cx-30, AY+2, cz+55, BlockTypes.PODZOL);

            // ── FOURTH SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 340; i++) {
                int sx = cx - 68 + (i * 107 + 73) % 137;
                int sz = cz - 68 + (i * 101 + 67) % 137;
                int r  = i % 10;
                if      (r == 0) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r == 1) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r == 2) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r == 3) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r == 4) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 5) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r == 6) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 7) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r == 8) block(es, sx, AY, sz, BlockTypes.MANGROVE_ROOTS);
                else             block(es, sx, AY, sz, BlockTypes.DIRT);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-2, AY, cz-52, cx+2, AY, cz-49, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-2, AY, cz+49, cx+2, AY, cz+52, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-52, AY, cz-2, cx-49, AY, cz+2, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx+49, AY, cz-2, cx+52, AY, cz+2, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-38, AY, cz-38, cx-35, AY, cz-35, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx+35, AY, cz+35, cx+38, AY, cz+38, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx+35, AY, cz-38, cx+38, AY, cz-35, BlockTypes.MANGROVE_PLANKS);
            fill(es, cx-38, AY, cz+35, cx-35, AY, cz+38, BlockTypes.MANGROVE_PLANKS);
        }
    }

    // =========================================================================
    // FFA 5 — VOLCANO HIGHLANDS   cx=400, cz=400   (150x150)
    // Vast volcanic highland: blackstone, basalt, polished basalt, tuff,
    // netherrack, magma, obsidian, gray/black concrete powder — multiple
    // lava lakes, basalt pillars, ash plains, gilded blackstone veins
    // =========================================================================
    private void buildVolcanoHighlandsFFA() {
        final int cx = 400, cz = 400;
        try (EditSession es = newSession()) {
            // ── BASE SUBSTRATE ──────────────────────────────────────────────
            fill(es, cx-75, 52, cz-75, cx+75, 63, cz+75, BlockTypes.BASALT);
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz+75, BlockTypes.BLACKSTONE);

            // ── POLISHED BASALT wide bands ─────────────────────────────────
            fill(es, cx-75, AY, cz-75, cx+75, AY, cz-55, BlockTypes.POLISHED_BASALT);
            fill(es, cx-75, AY, cz+55, cx+75, AY, cz+75, BlockTypes.POLISHED_BASALT);
            fill(es, cx-75, AY, cz-55, cx-55, AY, cz+55, BlockTypes.POLISHED_BASALT);
            fill(es, cx+55, AY, cz-55, cx+75, AY, cz+55, BlockTypes.POLISHED_BASALT);
            // Inner polished ring
            fill(es, cx-40, AY, cz-40, cx+40, AY, cz-30, BlockTypes.POLISHED_BASALT);
            fill(es, cx-40, AY, cz+30, cx+40, AY, cz+40, BlockTypes.POLISHED_BASALT);
            fill(es, cx-40, AY, cz-30, cx-30, AY, cz+30, BlockTypes.POLISHED_BASALT);
            fill(es, cx+30, AY, cz-30, cx+40, AY, cz+30, BlockTypes.POLISHED_BASALT);

            // ── TUFF grey highland patches ────────────────────────────────
            fill(es, cx-55, AY, cz-55, cx-35, AY, cz-35, BlockTypes.TUFF);
            fill(es, cx+35, AY, cz+35, cx+55, AY, cz+55, BlockTypes.TUFF);
            fill(es, cx+35, AY, cz-55, cx+55, AY, cz-35, BlockTypes.TUFF);
            fill(es, cx-55, AY, cz+35, cx-35, AY, cz+55, BlockTypes.TUFF);
            fill(es, cx-20, AY, cz-20, cx+20, AY, cz+20, BlockTypes.TUFF);
            fill(es, cx-75, AY, cz-20, cx-55, AY, cz+20, BlockTypes.TUFF);
            fill(es, cx+55, AY, cz-20, cx+75, AY, cz+20, BlockTypes.TUFF);
            fill(es, cx-20, AY, cz-75, cx+20, AY, cz-55, BlockTypes.TUFF);
            fill(es, cx-20, AY, cz+55, cx+20, AY, cz+75, BlockTypes.TUFF);

            // ── NETHERRACK ash fields ─────────────────────────────────────
            fill(es, cx-35, AY, cz-35, cx-15, AY, cz-15, BlockTypes.NETHERRACK);
            fill(es, cx+15, AY, cz+15, cx+35, AY, cz+35, BlockTypes.NETHERRACK);
            fill(es, cx+15, AY, cz-35, cx+35, AY, cz-15, BlockTypes.NETHERRACK);
            fill(es, cx-35, AY, cz+15, cx-15, AY, cz+35, BlockTypes.NETHERRACK);
            fill(es, cx-75, AY, cz-75, cx-58, AY, cz-58, BlockTypes.NETHERRACK);
            fill(es, cx+58, AY, cz+58, cx+75, AY, cz+75, BlockTypes.NETHERRACK);
            fill(es, cx+58, AY, cz-75, cx+75, AY, cz-58, BlockTypes.NETHERRACK);
            fill(es, cx-75, AY, cz+58, cx-58, AY, cz+75, BlockTypes.NETHERRACK);

            // ── MAGMA BLOCK radial flows ───────────────────────────────────
            for (int r = 1; r <= 28; r++) {
                block(es, cx+r,  AY, cz,    BlockTypes.MAGMA_BLOCK);
                block(es, cx-r,  AY, cz,    BlockTypes.MAGMA_BLOCK);
                block(es, cx,    AY, cz+r,  BlockTypes.MAGMA_BLOCK);
                block(es, cx,    AY, cz-r,  BlockTypes.MAGMA_BLOCK);
                if (r % 2 == 0) {
                    block(es, cx+r, AY, cz+r, BlockTypes.MAGMA_BLOCK);
                    block(es, cx-r, AY, cz-r, BlockTypes.MAGMA_BLOCK);
                    block(es, cx+r, AY, cz-r, BlockTypes.MAGMA_BLOCK);
                    block(es, cx-r, AY, cz+r, BlockTypes.MAGMA_BLOCK);
                }
            }
            // Wide magma patches mid-field
            fill(es, cx-45, AY, cz- 8, cx-28, AY, cz+ 8, BlockTypes.MAGMA_BLOCK);
            fill(es, cx+28, AY, cz- 8, cx+45, AY, cz+ 8, BlockTypes.MAGMA_BLOCK);
            fill(es, cx- 8, AY, cz-45, cx+ 8, AY, cz-28, BlockTypes.MAGMA_BLOCK);
            fill(es, cx- 8, AY, cz+28, cx+ 8, AY, cz+45, BlockTypes.MAGMA_BLOCK);

            // ── GRAY CONCRETE POWDER ash dunes ────────────────────────────
            fill(es, cx-75, AY,   cz-75, cx-60, AY+2, cz-60, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+60, AY,   cz+60, cx+75, AY+2, cz+75, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+60, AY,   cz-75, cx+75, AY+2, cz-60, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-75, AY,   cz+60, cx-60, AY+2, cz+75, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-25, AY,   cz-75, cx+25, AY+3, cz-65, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-25, AY,   cz+65, cx+25, AY+3, cz+75, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx-75, AY,   cz-25, cx-65, AY+3, cz+25, BlockTypes.GRAY_CONCRETE_POWDER);
            fill(es, cx+65, AY,   cz-25, cx+75, AY+3, cz+25, BlockTypes.GRAY_CONCRETE_POWDER);
            // Light grey ash
            fill(es, cx-20, AY, cz-75, cx-10, AY+1, cz-65, BlockTypes.LIGHT_GRAY_CONCRETE_POWDER);
            fill(es, cx+10, AY, cz+65, cx+20, AY+1, cz+75, BlockTypes.LIGHT_GRAY_CONCRETE_POWDER);
            fill(es, cx+65, AY, cz-20, cx+75, AY+1, cz-10, BlockTypes.LIGHT_GRAY_CONCRETE_POWDER);
            fill(es, cx-75, AY, cz+10, cx-65, AY+1, cz+20, BlockTypes.LIGHT_GRAY_CONCRETE_POWDER);

            // ── OBSIDIAN spires and flows ─────────────────────────────────
            fill(es, cx-12, AY, cz-12, cx+12, AY, cz+12, BlockTypes.OBSIDIAN);
            fill(es, cx-12, AY+1, cz-12, cx+12, AY+3, cz+12, BlockTypes.CRYING_OBSIDIAN);
            int[][] obsSpires = {
                {cx-52,cz-12},{cx+52,cz+12},{cx-12,cz-52},{cx+12,cz+52},
                {cx-52,cz+12},{cx+52,cz-12},{cx+12,cz-52},{cx-12,cz+52},
                {cx-62,cz-30},{cx+62,cz+30},{cx-30,cz-62},{cx+30,cz+62},
                {cx+62,cz-30},{cx-62,cz+30},{cx+30,cz-62},{cx-30,cz+62}
            };
            for (int[] sp : obsSpires) {
                column(es, sp[0], sp[1], AY+1, AY+5, BlockTypes.OBSIDIAN);
                block(es, sp[0], AY+6, sp[1], BlockTypes.CRYING_OBSIDIAN);
                disk(es, sp[0], AY, sp[1], 2, BlockTypes.OBSIDIAN);
            }

            // ── GILDED BLACKSTONE mineral veins ───────────────────────────
            for (int i = 0; i < 60; i++) {
                int gx = cx - 60 + (i * 37 + 11) % 121;
                int gz = cz - 60 + (i * 29 +  7) % 121;
                block(es, gx, AY, gz, BlockTypes.GILDED_BLACKSTONE);
            }

            // ── MULTIPLE LAVA LAKES ───────────────────────────────────────
            // Central caldera (deep)
            fill(es, cx-10, 61, cz-10, cx+10, 61, cz+10, BlockTypes.MAGMA_BLOCK);
            fill(es, cx-10, AY, cz-10, cx+10, AY, cz+10, BlockTypes.LAVA);
            // Peripheral lava pools
            fill(es, cx-62, 63, cz-62, cx-48, 63, cz-48, BlockTypes.BASALT);
            fill(es, cx-62, AY, cz-62, cx-48, AY, cz-48, BlockTypes.LAVA);
            fill(es, cx+48, 63, cz+48, cx+62, 63, cz+62, BlockTypes.BASALT);
            fill(es, cx+48, AY, cz+48, cx+62, AY, cz+62, BlockTypes.LAVA);
            fill(es, cx+48, 63, cz-62, cx+62, 63, cz-48, BlockTypes.BASALT);
            fill(es, cx+48, AY, cz-62, cx+62, AY, cz-48, BlockTypes.LAVA);
            fill(es, cx-62, 63, cz+48, cx-48, 63, cz+62, BlockTypes.BASALT);
            fill(es, cx-62, AY, cz+48, cx-48, AY, cz+62, BlockTypes.LAVA);
            // Mid-field vents
            fill(es, cx-40, 63, cz- 5, cx-30, 63, cz+ 5, BlockTypes.BLACKSTONE);
            fill(es, cx-40, AY, cz- 5, cx-30, AY, cz+ 5, BlockTypes.LAVA);
            fill(es, cx+30, 63, cz- 5, cx+40, 63, cz+ 5, BlockTypes.BLACKSTONE);
            fill(es, cx+30, AY, cz- 5, cx+40, AY, cz+ 5, BlockTypes.LAVA);
            fill(es, cx- 5, 63, cz-40, cx+ 5, 63, cz-30, BlockTypes.BLACKSTONE);
            fill(es, cx- 5, AY, cz-40, cx+ 5, AY, cz-30, BlockTypes.LAVA);
            fill(es, cx- 5, 63, cz+30, cx+ 5, 63, cz+40, BlockTypes.BLACKSTONE);
            fill(es, cx- 5, AY, cz+30, cx+ 5, AY, cz+40, BlockTypes.LAVA);

            // ── BASALT PILLAR FIELD ────────────────────────────────────────
            int[][] bPillars = {
                {cx-55,cz-35,12},{cx+55,cz+35,10},{cx+35,cz-55,11},{cx-35,cz+55,9},
                {cx-55,cz+35,10},{cx+55,cz-35,12},{cx-35,cz-55,9}, {cx+35,cz+55,11},
                {cx-70,cz,    8},{cx+70,cz,    8},{cx,cz-70,    9},{cx,cz+70,  8},
                {cx-30,cz-68,7},{cx+30,cz+68,7},{cx+68,cz-30, 8},{cx-68,cz+30,7},
                {cx-48,cz,   6},{cx+48,cz,   7},{cx,cz-48,   7},{cx,cz+48,  6},
                {cx-68,cz-48,9},{cx+68,cz+48,10},{cx+48,cz-68,8},{cx-48,cz+68,9}
            };
            for (int[] p : bPillars) {
                int px = p[0], pz2 = p[1], ph = p[2];
                column(es, px, pz2, AY+1, AY+ph, BlockTypes.BASALT);
                block(es, px, AY+ph+1, pz2, BlockTypes.MAGMA_BLOCK);
                disk(es, px, AY, pz2, 3, BlockTypes.POLISHED_BASALT);
                // Rubble at base
                block(es, px+2, AY, pz2,   BlockTypes.BLACKSTONE);
                block(es, px-2, AY, pz2,   BlockTypes.BLACKSTONE);
                block(es, px,   AY, pz2+2, BlockTypes.BLACKSTONE);
                block(es, px,   AY, pz2-2, BlockTypes.BLACKSTONE);
            }

            // ── SCATTERED SURFACE DETAIL ──────────────────────────────────
            for (int i = 0; i < 380; i++) {
                int sx = cx - 70 + (i * 67 + 41) % 141;
                int sz = cz - 70 + (i * 59 + 37) % 141;
                int r  = i % 11;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.POLISHED_BASALT);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.OBSIDIAN);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.CRYING_OBSIDIAN);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.GRAY_CONCRETE_POWDER);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.GILDED_BLACKSTONE);
                else              block(es, sx, AY, sz, BlockTypes.POLISHED_BLACKSTONE);
            }

            // ── PERIMETER BLACKSTONE FORTRESS WALL ────────────────────────
            fillHollow(es, cx-75, AY+1, cz-75, cx+75, AY+8, cz+75,
                    BlockTypes.POLISHED_BLACKSTONE_BRICKS, BlockTypes.AIR);
            // Magma block base row
            fill(es, cx-75, AY+1, cz-75, cx+75, AY+1, cz+75,
                    BlockTypes.MAGMA_BLOCK);
            // Crying obsidian accent strip
            for (int d = -75; d <= 75; d++) {
                block(es, cx+d, AY+5, cz-75, BlockTypes.CRYING_OBSIDIAN);
                block(es, cx+d, AY+5, cz+75, BlockTypes.CRYING_OBSIDIAN);
                block(es, cx-75, AY+5, cz+d, BlockTypes.CRYING_OBSIDIAN);
                block(es, cx+75, AY+5, cz+d, BlockTypes.CRYING_OBSIDIAN);
            }
            // Gilded blackstone window slots every 12 blocks
            for (int d = -60; d <= 60; d += 12) {
                block(es, cx+d, AY+3, cz-75, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx+d, AY+4, cz-75, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx+d, AY+3, cz+75, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx+d, AY+4, cz+75, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx-75, AY+3, cz+d, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx-75, AY+4, cz+d, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx+75, AY+3, cz+d, BlockTypes.GILDED_BLACKSTONE);
                block(es, cx+75, AY+4, cz+d, BlockTypes.GILDED_BLACKSTONE);
            }
            // Obsidian corner bastions
            for (int y = AY+1; y <= AY+14; y++) {
                for (int dx = -1; dx <= 1; dx++) for (int dz2 = -1; dz2 <= 1; dz2++) {
                    block(es, cx-75+dx, y, cz-75+dz2, BlockTypes.OBSIDIAN);
                    block(es, cx+75+dx, y, cz-75+dz2, BlockTypes.OBSIDIAN);
                    block(es, cx-75+dx, y, cz+75+dz2, BlockTypes.OBSIDIAN);
                    block(es, cx+75+dx, y, cz+75+dz2, BlockTypes.OBSIDIAN);
                }
            }
            // Top row: magma cap
            fill(es, cx-75, AY+8, cz-75, cx+75, AY+8, cz+75, BlockTypes.MAGMA_BLOCK);

            // ── DEEP LAVA CALDERA CHAMBER ─────────────────────────────────
            fill(es, cx-12, 58, cz-12, cx+12, 63, cz+12, BlockTypes.AIR);
            fill(es, cx-12, 57, cz-12, cx+12, 57, cz+12, BlockTypes.MAGMA_BLOCK);
            fill(es, cx-12, 58, cz-12, cx+12, 58, cz+12, BlockTypes.LAVA);
            fillHollow(es, cx-12, 59, cz-12, cx+12, 63, cz+12,
                    BlockTypes.BASALT, BlockTypes.AIR);
            // Obsidian rim around caldera
            for (int dc = -11; dc <= 11; dc++) {
                block(es, cx+dc, AY+1, cz-12, BlockTypes.OBSIDIAN);
                block(es, cx+dc, AY+1, cz+12, BlockTypes.OBSIDIAN);
                block(es, cx-12, AY+1, cz+dc, BlockTypes.OBSIDIAN);
                block(es, cx+12, AY+1, cz+dc, BlockTypes.OBSIDIAN);
            }

            // ── LAVA RIVER NETWORK ────────────────────────────────────────
            // Main E-W lava river
            fill(es, cx-75, 63, cz-5, cx+75, 63, cz+5, BlockTypes.BASALT);
            fill(es, cx-75, AY, cz-5, cx+75, AY, cz+5, BlockTypes.LAVA);
            // N-S lava river
            fill(es, cx-5, 63, cz-75, cx+5, 63, cz+75, BlockTypes.BASALT);
            fill(es, cx-5, AY, cz-75, cx+5, AY, cz+75, BlockTypes.LAVA);

            // ── ANCIENT NETHER BRICK RUIN on NE plateau ───────────────────
            fill(es, cx+40, AY, cz-55, cx+55, AY+3, cz-40, BlockTypes.NETHER_BRICKS);
            fillHollow(es, cx+40, AY, cz-55, cx+55, AY+3, cz-40,
                    BlockTypes.NETHER_BRICKS, BlockTypes.AIR);
            fill(es, cx+40, AY, cz-55, cx+55, AY, cz-40, BlockTypes.CRACKED_NETHER_BRICKS);
            // Cracked detail
            for (int dc = cx+41; dc <= cx+54; dc += 3)
                block(es, dc, AY+3, cz-55, BlockTypes.CRACKED_NETHER_BRICKS);
            for (int dc = cx+41; dc <= cx+54; dc += 3)
                block(es, dc, AY+3, cz-40, BlockTypes.CRACKED_NETHER_BRICKS);

            // ── BLACKSTONE RIDGE FIELD ────────────────────────────────────
            int[][] ridges = {
                {cx-70,cz-50,cx-60,cz-40,3},{cx+60,cz+40,cx+70,cz+50,3},
                {cx+60,cz-50,cx+70,cz-40,2},{cx-70,cz+40,cx-60,cz+50,2},
                {cx-50,cz-70,cx-40,cz-60,3},{cx+40,cz+60,cx+50,cz+70,3},
                {cx+40,cz-70,cx+50,cz-60,2},{cx-50,cz+60,cx-40,cz+70,2}
            };
            for (int[] rg : ridges) {
                fill(es, rg[0], AY, rg[1], rg[2], AY+rg[4], rg[3],
                        BlockTypes.BLACKSTONE);
                fill(es, rg[0], AY+rg[4]+1, rg[1], rg[2], AY+rg[4]+1, rg[3],
                        BlockTypes.POLISHED_BASALT);
            }

            // ── SOUL SAND VALLEY SECTION ──────────────────────────────────
            fill(es, cx-40, AY, cz+40, cx-20, AY, cz+60, BlockTypes.SOUL_SAND);
            fill(es, cx+20, AY, cz-60, cx+40, AY, cz-40, BlockTypes.SOUL_SAND);
            fill(es, cx-40, AY+1, cz+40, cx-20, AY+1, cz+60, BlockTypes.SOUL_SOIL);
            fill(es, cx+20, AY+1, cz-60, cx+40, AY+1, cz-40, BlockTypes.SOUL_SOIL);

            // ── WARPED NETHER WART BLOCK patches ─────────────────────────
            fill(es, cx-65, AY, cz-30, cx-50, AY, cz-15, BlockTypes.WARPED_WART_BLOCK);
            fill(es, cx+50, AY, cz+15, cx+65, AY, cz+30, BlockTypes.WARPED_WART_BLOCK);

            // ── SHROOMLIGHT veins along basalt columns ────────────────────
            for (int shr = -60; shr <= 60; shr += 20) {
                block(es, cx+shr, AY+3, cz-75, BlockTypes.SHROOMLIGHT);
                block(es, cx+shr, AY+3, cz+75, BlockTypes.SHROOMLIGHT);
                block(es, cx-75, AY+3, cz+shr, BlockTypes.SHROOMLIGHT);
                block(es, cx+75, AY+3, cz+shr, BlockTypes.SHROOMLIGHT);
            }

            // ── ADDITIONAL MAGMA BLOCK VENT FIELD ─────────────────────────
            for (int i = 0; i < 160; i++) {
                int mx2 = cx - 70 + (i * 43 + 17) % 141;
                int mz2 = cz - 70 + (i * 37 + 13) % 141;
                if (i % 4 == 0) block(es, mx2, AY, mz2, BlockTypes.MAGMA_BLOCK);
            }

            // ── THIRD SCATTER PASS ────────────────────────────────────────
            for (int i = 0; i < 350; i++) {
                int sx = cx - 68 + (i * 89 + 61) % 137;
                int sz = cz - 68 + (i * 83 + 59) % 137;
                int r  = i % 12;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.POLISHED_BASALT);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.SOUL_SAND);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.SOUL_SOIL);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.OBSIDIAN);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.GILDED_BLACKSTONE);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.WARPED_WART_BLOCK);
                else              block(es, sx, AY, sz, BlockTypes.GRAY_CONCRETE_POWDER);
            }

            // ── SPAWN PADS ────────────────────────────────────────────────
            fill(es, cx-2, AY, cz-52, cx+2, AY, cz-49, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx-2, AY, cz+49, cx+2, AY, cz+52, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx-52, AY, cz-2, cx-49, AY, cz+2, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx+49, AY, cz-2, cx+52, AY, cz+2, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
            fill(es, cx-38, AY, cz-38, cx-35, AY, cz-35, BlockTypes.GILDED_BLACKSTONE);
            fill(es, cx+35, AY, cz+35, cx+38, AY, cz+38, BlockTypes.GILDED_BLACKSTONE);
            fill(es, cx+35, AY, cz-38, cx+38, AY, cz-35, BlockTypes.GILDED_BLACKSTONE);
            fill(es, cx-38, AY, cz+35, cx-35, AY, cz+38, BlockTypes.GILDED_BLACKSTONE);

            // ── FOURTH SCATTER PASS — fine nether debris field ────────────
            for (int i = 0; i < 400; i++) {
                int sx = cx - 70 + (i * 101 + 71) % 141;
                int sz = cz - 70 + (i * 97  + 67) % 141;
                int r  = i % 14;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.CRIMSON_NYLIUM);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.WARPED_NYLIUM);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.POLISHED_BLACKSTONE);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.CHISELED_POLISHED_BLACKSTONE);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.SMOOTH_BASALT);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.SOUL_SAND);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.SOUL_SOIL);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.GILDED_BLACKSTONE);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.OBSIDIAN);
                else              block(es, sx, AY, sz, BlockTypes.CRYING_OBSIDIAN);
            }

            // ── CRIMSON/WARPED BIOME ZONES ─────────────────────────────────
            // Crimson zone — south-east quadrant
            fill(es, cx+15, AY, cz+15, cx+65, AY, cz+65, BlockTypes.CRIMSON_NYLIUM);
            fill(es, cx+25, AY, cz+25, cx+55, AY, cz+55, BlockTypes.NETHERRACK);
            for (int i = 0; i < 60; i++) {
                int cx2 = cx + 15 + (i * 31 + 7)  % 51;
                int cz2 = cz + 15 + (i * 29 + 11) % 51;
                if (i % 3 == 0) block(es, cx2, AY, cz2, BlockTypes.CRIMSON_NYLIUM);
                else            block(es, cx2, AY, cz2, BlockTypes.NETHERRACK);
            }
            // Warped zone — north-west quadrant
            fill(es, cx-65, AY, cz-65, cx-15, AY, cz-15, BlockTypes.WARPED_NYLIUM);
            fill(es, cx-55, AY, cz-55, cx-25, AY, cz-25, BlockTypes.NETHERRACK);
            for (int i = 0; i < 60; i++) {
                int cx2 = cx - 65 + (i * 37 + 13) % 51;
                int cz2 = cz - 65 + (i * 41 + 17) % 51;
                if (i % 3 == 0) block(es, cx2, AY, cz2, BlockTypes.WARPED_NYLIUM);
                else            block(es, cx2, AY, cz2, BlockTypes.NETHERRACK);
            }

            // ── POLISHED BASALT PILLAR FIELD ──────────────────────────────
            for (int i = 0; i < 36; i++) {
                int px = cx - 60 + (i * 17 + 5) % 121;
                int pz = cz - 60 + (i * 19 + 3) % 121;
                int ph = 2 + (i % 5);
                column(es, px, pz, AY, AY + ph, BlockTypes.POLISHED_BASALT);
                block(es, px, AY + ph + 1, pz, BlockTypes.SHROOMLIGHT);
            }

            // ── LAVA SHORELINE CRACKS ──────────────────────────────────────
            for (int i = 0; i < 40; i++) {
                int lx = cx - 70 + (i * 23 + 9)  % 141;
                int lz = cz - 70 + (i * 19 + 13) % 141;
                if ((lx < cx - 60 || lx > cx + 60) || (lz < cz - 60 || lz > cz + 60)) {
                    block(es, lx, AY - 1, lz, BlockTypes.LAVA);
                    block(es, lx, AY,     lz, BlockTypes.OBSIDIAN);
                }
            }

            // ── GOLD VEIN OUTCROPS ─────────────────────────────────────────
            for (int i = 0; i < 30; i++) {
                int gx = cx - 65 + (i * 53 + 31) % 131;
                int gz = cz - 65 + (i * 47 + 29) % 131;
                block(es, gx, AY,     gz, BlockTypes.GOLD_BLOCK);
                block(es, gx, AY + 1, gz, BlockTypes.RAW_GOLD_BLOCK);
            }

            // ── ANCIENT DEBRIS SCATTER ────────────────────────────────────
            for (int i = 0; i < 20; i++) {
                int ax = cx - 60 + (i * 61 + 43) % 121;
                int az = cz - 60 + (i * 59 + 41) % 121;
                block(es, ax, AY, az, BlockTypes.ANCIENT_DEBRIS);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 1 (JUNGLE TEMPLE) EXTRA PASSES
    // =========================================================================

    private void buildArena1ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — jungle floor micro-variation ─────────
            for (int i = 0; i < 500; i++) {
                int sx = cx - 60 + (i * 107 + 79) % 121;
                int sz = cz - 60 + (i * 103 + 73) % 121;
                int r  = i % 16;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.JUNGLE_LEAVES);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.OAK_LEAVES);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.ROOTED_DIRT);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.MOSS_CARPET);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.JUNGLE_LOG);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.STRIPPED_JUNGLE_LOG);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.JUNGLE_PLANKS);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else if (r == 14) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else              block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
            }

            // ── TEMPLE TERRACE RING ───────────────────────────────────────
            ring(es, cx, AY, cz, 12, 15, BlockTypes.MOSSY_STONE_BRICKS);
            ring(es, cx, AY, cz, 20, 23, BlockTypes.COBBLESTONE);
            ring(es, cx, AY, cz, 28, 31, BlockTypes.MOSS_BLOCK);
            ring(es, cx, AY, cz, 36, 39, BlockTypes.MOSSY_COBBLESTONE);

            // ── LARGE JUNGLE FERN MOUNDS ──────────────────────────────────
            for (int i = 0; i < 18; i++) {
                int fx = cx - 55 + (i * 59 + 37) % 111;
                int fz = cz - 55 + (i * 61 + 41) % 111;
                fill(es, fx - 1, AY, fz - 1, fx + 1, AY + 1, fz + 1, BlockTypes.MOSS_BLOCK);
                block(es, fx, AY + 2, fz, BlockTypes.OAK_LEAVES);
            }

            // ── STONE BRICK PATHWAY — diagonal cross ──────────────────────
            for (int t = -45; t <= 45; t++) {
                block(es, cx + t, AY, cz + t, BlockTypes.STONE_BRICKS);
                block(es, cx + t, AY, cz - t, BlockTypes.MOSSY_STONE_BRICKS);
            }

            // ── JUNGLE TREE ROOTS (surface ridges) ────────────────────────
            for (int r = 0; r < 12; r++) {
                int rx = cx - 40 + (r * 37 + 7) % 81;
                int rz = cz - 40 + (r * 41 + 11) % 81;
                fill(es, rx - 2, AY, rz, rx + 2, AY, rz, BlockTypes.JUNGLE_LOG);
                fill(es, rx, AY, rz - 2, rx, AY, rz + 2, BlockTypes.JUNGLE_LOG);
                block(es, rx, AY + 1, rz, BlockTypes.JUNGLE_LEAVES);
            }

            // ── MOSSY COBBLESTONE RUBBLE FIELDS ──────────────────────────
            for (int i = 0; i < 80; i++) {
                int rx = cx - 60 + (i * 73 + 53) % 121;
                int rz = cz - 60 + (i * 71 + 51) % 121;
                block(es, rx, AY, rz, i % 3 == 0 ? BlockTypes.MOSSY_COBBLESTONE : BlockTypes.COBBLESTONE);
                if (i % 5 == 0) block(es, rx, AY + 1, rz, BlockTypes.MOSS_CARPET);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 2 (BEACH) EXTRA PASSES
    // =========================================================================

    private void buildArena2ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — beach sand micro-variation ───────────
            for (int i = 0; i < 500; i++) {
                int sx = cx - 60 + (i * 109 + 81) % 121;
                int sz = cz - 60 + (i * 113 + 83) % 121;
                int r  = i % 14;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.RED_SANDSTONE);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.WHITE_TERRACOTTA);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.LIGHT_BLUE_TERRACOTTA);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.CYAN_TERRACOTTA);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.SMOOTH_SANDSTONE);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.CUT_SANDSTONE);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.CHISELED_SANDSTONE);
                else              block(es, sx, AY, sz, BlockTypes.CALCITE);
            }

            // ── SHELL RING PATTERNS ───────────────────────────────────────
            ring(es, cx, AY, cz, 10, 13, BlockTypes.WHITE_TERRACOTTA);
            ring(es, cx, AY, cz, 18, 21, BlockTypes.ORANGE_TERRACOTTA);
            ring(es, cx, AY, cz, 26, 29, BlockTypes.YELLOW_TERRACOTTA);

            // ── DUNE RIDGES — perpendicular waves ─────────────────────────
            for (int dz = -60; dz <= 60; dz += 12) {
                for (int dx = -60; dx <= 60; dx++) {
                    int h = (int)(Math.sin(dx * Math.PI / 20.0) * 1.5 + 1.5);
                    for (int y = AY + 1; y <= AY + h; y++)
                        block(es, cx + dx, y, cz + dz, BlockTypes.SAND);
                }
            }

            // ── COASTAL ROCK FORMATIONS ───────────────────────────────────
            for (int i = 0; i < 20; i++) {
                int rx = cx - 55 + (i * 53 + 29) % 111;
                int rz = cz - 55 + (i * 47 + 23) % 111;
                sphere(es, rx, AY + 1, rz, 2, BlockTypes.STONE);
                block(es, rx, AY + 3, rz, BlockTypes.GRAVEL);
            }

            // ── TIDAL POOLS — shallow water patches ───────────────────────
            for (int i = 0; i < 15; i++) {
                int tx = cx - 45 + (i * 61 + 37) % 91;
                int tz = cz - 45 + (i * 67 + 41) % 91;
                fill(es, tx - 2, AY - 1, tz - 2, tx + 2, AY - 1, tz + 2, BlockTypes.WATER);
                fill(es, tx - 2, AY,     tz - 2, tx + 2, AY,     tz + 2, BlockTypes.PRISMARINE);
            }

            // ── STARFISH PATTERN TERRACOTTA ───────────────────────────────
            for (int i = 0; i < 25; i++) {
                int stx = cx - 50 + (i * 79 + 57) % 101;
                int stz = cz - 50 + (i * 83 + 61) % 101;
                block(es, stx,     AY, stz,     BlockTypes.RED_TERRACOTTA);
                block(es, stx + 1, AY, stz,     BlockTypes.RED_TERRACOTTA);
                block(es, stx - 1, AY, stz,     BlockTypes.RED_TERRACOTTA);
                block(es, stx,     AY, stz + 1, BlockTypes.RED_TERRACOTTA);
                block(es, stx,     AY, stz - 1, BlockTypes.RED_TERRACOTTA);
                block(es, stx,     AY, stz,     BlockTypes.PINK_TERRACOTTA);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 3 (BAMBOO FOREST) EXTRA PASSES
    // =========================================================================

    private void buildArena3ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — forest floor diversity ───────────────
            for (int i = 0; i < 500; i++) {
                int sx = cx - 60 + (i * 113 + 89) % 121;
                int sz = cz - 60 + (i * 107 + 83) % 121;
                int r  = i % 15;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.BAMBOO_MOSAIC);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.BAMBOO_PLANKS);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.MOSS_CARPET);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.ROOTED_DIRT);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.DIRT_PATH);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.CHERRY_LEAVES);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.OAK_LEAVES);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.BIRCH_LEAVES);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.STRIPPED_BAMBOO_BLOCK);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.BAMBOO_BLOCK);
                else              block(es, sx, AY, sz, BlockTypes.OAK_LOG);
            }

            // ── STONE LANTERN PATHS ───────────────────────────────────────
            for (int t = -45; t <= 45; t += 5) {
                block(es, cx + t, AY,     cz, BlockTypes.STONE_BRICKS);
                block(es, cx + t, AY + 1, cz, BlockTypes.LANTERN);
                block(es, cx, AY,     cz + t, BlockTypes.STONE_BRICKS);
                block(es, cx, AY + 1, cz + t, BlockTypes.LANTERN);
            }

            // ── CHERRY BLOSSOM CARPET ─────────────────────────────────────
            for (int i = 0; i < 80; i++) {
                int bx = cx - 55 + (i * 67 + 43) % 111;
                int bz = cz - 55 + (i * 71 + 47) % 111;
                block(es, bx, AY, bz, BlockTypes.CHERRY_LEAVES);
                if (i % 4 == 0) block(es, bx, AY + 1, bz, BlockTypes.PINK_PETALS);
            }

            // ── ZEN STEPPING STONE GRID ───────────────────────────────────
            for (int gx = -48; gx <= 48; gx += 8) {
                for (int gz = -48; gz <= 48; gz += 8) {
                    block(es, cx + gx, AY, cz + gz, BlockTypes.STONE);
                    block(es, cx + gx, AY + 1, cz + gz, BlockTypes.MOSS_CARPET);
                }
            }

            // ── KITE BAMBOO GROVES ────────────────────────────────────────
            for (int i = 0; i < 24; i++) {
                int tx = cx - 50 + (i * 41 + 19) % 101;
                int tz = cz - 50 + (i * 43 + 23) % 101;
                for (int h = 0; h <= 5; h++)
                    block(es, tx, AY + h, tz, BlockTypes.BAMBOO_BLOCK);
                block(es, tx, AY + 6, tz, BlockTypes.BAMBOO_MOSAIC);
            }

            // ── WATER FEATURE STREAMS ─────────────────────────────────────
            for (int wt = -30; wt <= 30; wt++) {
                block(es, cx + wt, AY - 1, cz + 20, BlockTypes.WATER);
                block(es, cx + wt, AY - 1, cz - 20, BlockTypes.WATER);
                block(es, cx + 20, AY - 1, cz + wt, BlockTypes.WATER);
                block(es, cx - 20, AY - 1, cz + wt, BlockTypes.WATER);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — FFA 1 (JUNGLE FORTRESS) EXTRA PASSES
    // =========================================================================

    private void buildFFA1ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — lush fortress floor ─────────────────
            for (int i = 0; i < 600; i++) {
                int sx = cx - 70 + (i * 107 + 79) % 141;
                int sz = cz - 70 + (i * 103 + 73) % 141;
                int r  = i % 18;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.PODZOL);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.ROOTED_DIRT);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.JUNGLE_LEAVES);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.OAK_LEAVES);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.JUNGLE_PLANKS);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.JUNGLE_LOG);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.BAMBOO_MOSAIC);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.MOSS_CARPET);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.CRACKED_STONE_BRICKS);
                else if (r == 14) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r == 15) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 16) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else              block(es, sx, AY, sz, BlockTypes.DIRT_PATH);
            }

            // ── FORTRESS COURTYARD TERRACE RINGS ─────────────────────────
            ring(es, cx, AY, cz, 15, 18, BlockTypes.MOSSY_STONE_BRICKS);
            ring(es, cx, AY, cz, 25, 28, BlockTypes.STONE_BRICKS);
            ring(es, cx, AY, cz, 38, 41, BlockTypes.COBBLESTONE);
            ring(es, cx, AY, cz, 52, 55, BlockTypes.MOSSY_COBBLESTONE);

            // ── VINE-COVERED WALLS (horizontal spread) ────────────────────
            for (int v = -65; v <= 65; v += 5) {
                block(es, cx + v, AY + 1, cz - 65, BlockTypes.MOSSY_STONE_BRICKS);
                block(es, cx + v, AY + 2, cz - 65, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx + v, AY + 1, cz + 65, BlockTypes.MOSSY_STONE_BRICKS);
                block(es, cx + v, AY + 2, cz + 65, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx - 65, AY + 1, cz + v, BlockTypes.MOSSY_STONE_BRICKS);
                block(es, cx - 65, AY + 2, cz + v, BlockTypes.MOSSY_COBBLESTONE);
                block(es, cx + 65, AY + 1, cz + v, BlockTypes.MOSSY_STONE_BRICKS);
                block(es, cx + 65, AY + 2, cz + v, BlockTypes.MOSSY_COBBLESTONE);
            }

            // ── JUNGLE FLOOR RIDGES ───────────────────────────────────────
            for (int rg = -60; rg <= 60; rg += 15) {
                for (int t = -60; t <= 60; t++) {
                    if (Math.abs(t) % 7 < 3)
                        block(es, cx + t, AY + 1, cz + rg, BlockTypes.PODZOL);
                }
            }

            // ── ANCIENT STONE CIRCLES ─────────────────────────────────────
            for (int c = 0; c < 5; c++) {
                int ocx = cx - 40 + (c * 67 + 23) % 81;
                int ocz = cz - 40 + (c * 71 + 27) % 81;
                ring(es, ocx, AY, ocz, 4, 6, BlockTypes.MOSSY_STONE_BRICKS);
                block(es, ocx, AY + 1, ocz, BlockTypes.LANTERN);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — FFA 2 (ISLAND RESORT) EXTRA PASSES
    // =========================================================================

    private void buildFFA2ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — tropical island floor ────────────────
            for (int i = 0; i < 600; i++) {
                int sx = cx - 70 + (i * 113 + 87) % 141;
                int sz = cz - 70 + (i * 109 + 83) % 141;
                int r  = i % 17;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.SMOOTH_SANDSTONE);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.WHITE_TERRACOTTA);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.LIME_TERRACOTTA);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.CYAN_TERRACOTTA);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.LIGHT_BLUE_TERRACOTTA);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.BLUE_TERRACOTTA);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.PRISMARINE_BRICKS);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.DARK_PRISMARINE);
                else if (r == 14) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 15) block(es, sx, AY, sz, BlockTypes.CALCITE);
                else              block(es, sx, AY, sz, BlockTypes.DIORITE);
            }

            // ── CORAL REEF RINGS ──────────────────────────────────────────
            ring(es, cx, AY, cz, 20, 23, BlockTypes.PRISMARINE);
            ring(es, cx, AY, cz, 32, 35, BlockTypes.PRISMARINE_BRICKS);
            ring(es, cx, AY, cz, 44, 47, BlockTypes.DARK_PRISMARINE);
            ring(es, cx, AY, cz, 55, 58, BlockTypes.SEA_LANTERN);

            // ── TROPICAL WAVE RIDGES ──────────────────────────────────────
            for (int wv = -60; wv <= 60; wv += 10) {
                for (int t = -65; t <= 65; t++) {
                    int h = (int)(Math.abs(Math.sin(t * Math.PI / 25.0)) * 1.5);
                    block(es, cx + t, AY + h, cz + wv, BlockTypes.SAND);
                }
            }

            // ── ISLAND PALM CIRCLES ───────────────────────────────────────
            for (int i = 0; i < 12; i++) {
                int px = cx - 55 + (i * 89 + 61) % 111;
                int pz = cz - 55 + (i * 97  + 67) % 111;
                fill(es, px - 2, AY, pz - 2, px + 2, AY, pz + 2, BlockTypes.SAND);
                column(es, px, pz, AY + 1, AY + 6, BlockTypes.JUNGLE_LOG);
                disk(es, px, AY + 7, pz, 3, BlockTypes.OAK_LEAVES);
            }

            // ── SEA GLASS TERRACOTTA PATCHES ─────────────────────────────
            for (int i = 0; i < 40; i++) {
                int gx = cx - 60 + (i * 61 + 43) % 121;
                int gz = cz - 60 + (i * 67 + 47) % 121;
                int r  = i % 5;
                BlockType col = switch (r) {
                    case 0 -> BlockTypes.CYAN_STAINED_GLASS;
                    case 1 -> BlockTypes.LIGHT_BLUE_STAINED_GLASS;
                    case 2 -> BlockTypes.BLUE_STAINED_GLASS;
                    case 3 -> BlockTypes.GREEN_STAINED_GLASS;
                    default -> BlockTypes.WHITE_STAINED_GLASS;
                };
                block(es, gx, AY, gz, col);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — FFA 3 (STEP PYRAMID) EXTRA PASSES
    // =========================================================================

    private void buildFFA3ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — desert floor diversity ───────────────
            for (int i = 0; i < 600; i++) {
                int sx = cx - 70 + (i * 101 + 73) % 141;
                int sz = cz - 70 + (i * 97  + 71) % 141;
                int r  = i % 16;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.RED_SAND);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.SANDSTONE);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.RED_SANDSTONE);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.SMOOTH_SANDSTONE);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.SMOOTH_RED_SANDSTONE);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.CHISELED_SANDSTONE);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.CUT_SANDSTONE);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.ORANGE_TERRACOTTA);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.RED_TERRACOTTA);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.YELLOW_TERRACOTTA);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.BROWN_TERRACOTTA);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.WHITE_TERRACOTTA);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.TERRACOTTA);
                else if (r == 14) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else              block(es, sx, AY, sz, BlockTypes.CALCITE);
            }

            // ── PYRAMID APPROACH ROADS — 4 cardinal causeways ─────────────
            for (int t = -70; t <= -18; t++) {
                fill(es, cx - 3, AY, cz + t, cx + 3, AY, cz + t, BlockTypes.SANDSTONE);
                fill(es, cx - 3, AY, cz - t, cx + 3, AY, cz - t, BlockTypes.SANDSTONE);
                fill(es, cx + t, AY, cz - 3, cx + t, AY, cz + 3, BlockTypes.SANDSTONE);
                fill(es, cx - t, AY, cz - 3, cx - t, AY, cz + 3, BlockTypes.SANDSTONE);
            }

            // ── HIEROGLYPH TERRACOTTA LINES ───────────────────────────────
            BlockType[] hierColors = {
                BlockTypes.ORANGE_TERRACOTTA, BlockTypes.YELLOW_TERRACOTTA,
                BlockTypes.RED_TERRACOTTA,    BlockTypes.BROWN_TERRACOTTA,
                BlockTypes.WHITE_TERRACOTTA,  BlockTypes.TERRACOTTA
            };
            for (int row = -60; row <= 60; row += 10) {
                for (int col = -65; col <= 65; col++) {
                    int colorIdx = ((col + 65) / 4) % hierColors.length;
                    block(es, cx + col, AY, cz + row, hierColors[colorIdx]);
                }
            }

            // ── SANDSTONE CRATER RINGS ────────────────────────────────────
            ring(es, cx, AY, cz, 18, 21, BlockTypes.SMOOTH_SANDSTONE);
            ring(es, cx, AY, cz, 30, 33, BlockTypes.RED_SANDSTONE);
            ring(es, cx, AY, cz, 44, 47, BlockTypes.CHISELED_SANDSTONE);

            // ── DESERT ROCK OUTCROPS ──────────────────────────────────────
            for (int i = 0; i < 25; i++) {
                int rx = cx - 60 + (i * 53 + 31) % 121;
                int rz = cz - 60 + (i * 59 + 37) % 121;
                sphere(es, rx, AY + 1, rz, 2, BlockTypes.SANDSTONE);
                block(es, rx, AY + 3, rz, BlockTypes.CHISELED_SANDSTONE);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — FFA 4 (MANGROVE BAY) EXTRA PASSES
    // =========================================================================

    private void buildFFA4ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — bayou floor diversity ────────────────
            for (int i = 0; i < 600; i++) {
                int sx = cx - 70 + (i * 103 + 79) % 141;
                int sz = cz - 70 + (i * 107 + 83) % 141;
                int r  = i % 15;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.DIRT);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.ROOTED_DIRT);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.MOSS_CARPET);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.PACKED_MUD);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.MUD_BRICKS);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.MANGROVE_PLANKS);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.MANGROVE_LOG);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.STRIPPED_MANGROVE_LOG);
                else              block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
            }

            // ── TIDAL CHANNEL NETWORK ─────────────────────────────────────
            for (int t = -65; t <= 65; t++) {
                block(es, cx + t, AY - 1, cz + 10, BlockTypes.WATER);
                block(es, cx + t, AY - 1, cz - 10, BlockTypes.WATER);
                block(es, cx + 10, AY - 1, cz + t, BlockTypes.WATER);
                block(es, cx - 10, AY - 1, cz + t, BlockTypes.WATER);
            }
            // Channel banks
            for (int t = -65; t <= 65; t++) {
                block(es, cx + t, AY, cz + 11, BlockTypes.MUD);
                block(es, cx + t, AY, cz - 11, BlockTypes.MUD);
                block(es, cx + 11, AY, cz + t, BlockTypes.MUD);
                block(es, cx - 11, AY, cz + t, BlockTypes.MUD);
            }

            // ── MANGROVE ROOT CLUSTERS ────────────────────────────────────
            for (int i = 0; i < 30; i++) {
                int rx = cx - 60 + (i * 43 + 19) % 121;
                int rz = cz - 60 + (i * 47 + 23) % 121;
                block(es, rx - 1, AY,     rz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx + 1, AY,     rz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx,     AY,     rz - 1, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx,     AY,     rz + 1, BlockTypes.MUDDY_MANGROVE_ROOTS);
                block(es, rx,     AY + 1, rz, BlockTypes.MANGROVE_LOG);
                block(es, rx,     AY + 2, rz, BlockTypes.MANGROVE_LEAVES);
            }

            // ── MUD BRICK TERRACE RINGS ───────────────────────────────────
            ring(es, cx, AY, cz, 20, 23, BlockTypes.MUD_BRICKS);
            ring(es, cx, AY, cz, 35, 38, BlockTypes.PACKED_MUD);
            ring(es, cx, AY, cz, 50, 53, BlockTypes.MUD_BRICKS);

            // ── PEAT BOG PATCHES ──────────────────────────────────────────
            for (int i = 0; i < 20; i++) {
                int bx = cx - 55 + (i * 57 + 33) % 111;
                int bz = cz - 55 + (i * 61 + 37) % 111;
                fill(es, bx - 2, AY, bz - 2, bx + 2, AY, bz + 2, BlockTypes.MUD);
                fill(es, bx - 1, AY - 1, bz - 1, bx + 1, AY - 1, bz + 1, BlockTypes.WATER);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 4 (MANGROVE SWAMP) EXTRA PASSES
    // =========================================================================

    private void buildArena4ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — swamp floor richness ─────────────────
            for (int i = 0; i < 450; i++) {
                int sx = cx - 60 + (i * 109 + 83) % 121;
                int sz = cz - 60 + (i * 113 + 89) % 121;
                int r  = i % 14;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.MUD);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.CLAY);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.PACKED_MUD);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.ROOTED_DIRT);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.MUDDY_MANGROVE_ROOTS);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.MANGROVE_LOG);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.MANGROVE_LEAVES);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.COARSE_DIRT);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else              block(es, sx, AY, sz, BlockTypes.MUD_BRICKS);
            }

            // ── SWAMP WATER RING NETWORK ──────────────────────────────────
            ring(es, cx, AY - 1, cz, 15, 17, BlockTypes.WATER);
            ring(es, cx, AY - 1, cz, 28, 30, BlockTypes.WATER);
            ring(es, cx, AY - 1, cz, 42, 44, BlockTypes.WATER);
            ring(es, cx, AY, cz, 15, 17, BlockTypes.MUD);
            ring(es, cx, AY, cz, 28, 30, BlockTypes.MUD);
            ring(es, cx, AY, cz, 42, 44, BlockTypes.MUD);

            // ── FLOATING LOG BRIDGES ──────────────────────────────────────
            for (int bt = -55; bt <= 55; bt++) {
                if (Math.abs(bt) > 14 && Math.abs(bt) < 16)
                    block(es, cx + bt, AY, cz, BlockTypes.MANGROVE_LOG);
            }
            for (int bt = -55; bt <= 55; bt++) {
                if (Math.abs(bt) > 14 && Math.abs(bt) < 16)
                    block(es, cx, AY, cz + bt, BlockTypes.MANGROVE_LOG);
            }

            // ── CYPRESS KNEE CLUSTERS ─────────────────────────────────────
            for (int i = 0; i < 20; i++) {
                int kx = cx - 50 + (i * 47 + 21) % 101;
                int kz = cz - 50 + (i * 53 + 27) % 101;
                column(es, kx, kz, AY, AY + 2, BlockTypes.STRIPPED_MANGROVE_LOG);
                block(es, kx, AY + 3, kz, BlockTypes.MANGROVE_LEAVES);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 5 (VOLCANIC ISLAND) EXTRA PASSES
    // =========================================================================

    private void buildArena5ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — volcano terrain richness ─────────────
            for (int i = 0; i < 450; i++) {
                int sx = cx - 60 + (i * 107 + 79) % 121;
                int sz = cz - 60 + (i * 103 + 71) % 121;
                int r  = i % 13;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.BASALT);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.BLACKSTONE);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.POLISHED_BASALT);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.MAGMA_BLOCK);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.NETHERRACK);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.OBSIDIAN);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.SMOOTH_BASALT);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.SOUL_SAND);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.SOUL_SOIL);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.GILDED_BLACKSTONE);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.DEEPSLATE);
                else              block(es, sx, AY, sz, BlockTypes.COBBLED_DEEPSLATE);
            }

            // ── RADIAL LAVA CRACK PATTERN ─────────────────────────────────
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                for (int dist = 8; dist <= 55; dist++) {
                    int lx = cx + (int)(Math.cos(rad) * dist);
                    int lz = cz + (int)(Math.sin(rad) * dist);
                    if (dist % 12 == 0) block(es, lx, AY - 1, lz, BlockTypes.LAVA);
                    else                block(es, lx, AY,     lz, BlockTypes.BLACKSTONE);
                }
            }

            // ── OBSIDIAN SPIRE RING ────────────────────────────────────────
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                int spx = cx + (int)(Math.cos(rad) * 35);
                int spz = cz + (int)(Math.sin(rad) * 35);
                int h = 3 + (angle / 30) % 4;
                column(es, spx, spz, AY, AY + h, BlockTypes.OBSIDIAN);
                block(es, spx, AY + h + 1, spz, BlockTypes.MAGMA_BLOCK);
            }

            // ── IGNEOUS BOULDER FIELD ─────────────────────────────────────
            for (int i = 0; i < 20; i++) {
                int bx = cx - 55 + (i * 59 + 37) % 111;
                int bz = cz - 55 + (i * 61 + 41) % 111;
                sphere(es, bx, AY + 1, bz, 2, BlockTypes.BASALT);
                block(es, bx, AY + 3, bz, BlockTypes.POLISHED_BASALT);
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 6 (CORAL REEF) EXTRA PASSES
    // =========================================================================

    private void buildArena6ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — reef floor diversity ─────────────────
            for (int i = 0; i < 450; i++) {
                int sx = cx - 60 + (i * 103 + 73) % 121;
                int sz = cz - 60 + (i * 107 + 79) % 121;
                int r  = i % 15;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.SAND);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.PRISMARINE);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.PRISMARINE_BRICKS);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.DARK_PRISMARINE);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.SEA_LANTERN);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.BLUE_ICE);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.PACKED_ICE);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.CALCITE);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.DIORITE);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.CYAN_TERRACOTTA);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.LIGHT_BLUE_TERRACOTTA);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.BLUE_TERRACOTTA);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.WHITE_TERRACOTTA);
                else              block(es, sx, AY, sz, BlockTypes.SANDSTONE);
            }

            // ── PRISMARINE CONCENTRIC RINGS ───────────────────────────────
            ring(es, cx, AY, cz, 12, 14, BlockTypes.PRISMARINE_BRICKS);
            ring(es, cx, AY, cz, 22, 24, BlockTypes.DARK_PRISMARINE);
            ring(es, cx, AY, cz, 32, 34, BlockTypes.SEA_LANTERN);
            ring(es, cx, AY, cz, 42, 44, BlockTypes.PRISMARINE);
            ring(es, cx, AY, cz, 52, 54, BlockTypes.DARK_PRISMARINE);

            // ── CORAL HEAD FORMATIONS ─────────────────────────────────────
            BlockType[] coralColors = {
                BlockTypes.CYAN_TERRACOTTA, BlockTypes.LIGHT_BLUE_TERRACOTTA,
                BlockTypes.BLUE_TERRACOTTA, BlockTypes.WHITE_TERRACOTTA,
                BlockTypes.PRISMARINE
            };
            for (int i = 0; i < 30; i++) {
                int hx = cx - 50 + (i * 67 + 43) % 101;
                int hz = cz - 50 + (i * 71 + 47) % 101;
                int colorIdx = i % coralColors.length;
                sphere(es, hx, AY + 1, hz, 2, coralColors[colorIdx]);
                block(es, hx, AY + 3, hz, BlockTypes.SEA_LANTERN);
            }

            // ── SEA LANTERN GRID ──────────────────────────────────────────
            for (int gx = -48; gx <= 48; gx += 12) {
                for (int gz = -48; gz <= 48; gz += 12) {
                    block(es, cx + gx, AY, cz + gz, BlockTypes.SEA_LANTERN);
                }
            }

            // ── WAVE STRIPE TEXTURING ─────────────────────────────────────
            for (int wv = -60; wv <= 60; wv += 8) {
                for (int t = -60; t <= 60; t++) {
                    if (Math.abs(t) % 4 < 2)
                        block(es, cx + t, AY, cz + wv, BlockTypes.PRISMARINE);
                    else
                        block(es, cx + t, AY, cz + wv, BlockTypes.DARK_PRISMARINE);
                }
            }
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 7 (OVERGROWN RUINS) EXTRA PASSES
    // =========================================================================

    private void buildArena7ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — ruin floor richness ──────────────────
            for (int i = 0; i < 450; i++) {
                int sx = cx - 60 + (i * 97  + 71) % 121;
                int sz = cz - 60 + (i * 101 + 73) % 121;
                int r  = i % 16;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.STONE_BRICKS);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.MOSSY_STONE_BRICKS);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.CRACKED_STONE_BRICKS);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.CHISELED_STONE_BRICKS);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.MOSSY_COBBLESTONE);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.MOSS_BLOCK);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.MOSS_CARPET);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.DEEPSLATE);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.COBBLED_DEEPSLATE);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.DEEPSLATE_BRICKS);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.CRACKED_DEEPSLATE_BRICKS);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.STONE);
                else if (r == 13) block(es, sx, AY, sz, BlockTypes.ANDESITE);
                else if (r == 14) block(es, sx, AY, sz, BlockTypes.GRASS_BLOCK);
                else              block(es, sx, AY, sz, BlockTypes.PODZOL);
            }

            // ── ROMAN ROAD CROSS ──────────────────────────────────────────
            for (int t = -55; t <= 55; t++) {
                block(es, cx + t, AY, cz, BlockTypes.CHISELED_STONE_BRICKS);
                block(es, cx, AY, cz + t, BlockTypes.CHISELED_STONE_BRICKS);
                if (Math.abs(t) % 6 == 0) {
                    block(es, cx + t, AY + 1, cz, BlockTypes.STONE_BRICKS);
                    block(es, cx, AY + 1, cz + t, BlockTypes.STONE_BRICKS);
                }
            }

            // ── COLLAPSED WALL REMNANTS ────────────────────────────────────
            for (int i = 0; i < 12; i++) {
                int wx = cx - 50 + (i * 41 + 13) % 101;
                int wz = cz - 50 + (i * 43 + 17) % 101;
                int wh = 1 + (i % 4);
                fill(es, wx, AY, wz, wx + 3, AY + wh, wz, BlockTypes.CRACKED_STONE_BRICKS);
                fill(es, wx, AY, wz, wx, AY + wh, wz + 3, BlockTypes.MOSSY_STONE_BRICKS);
            }

            // ── DEEPSLATE MOSAIC INLAYS ───────────────────────────────────
            ring(es, cx, AY, cz, 15, 17, BlockTypes.DEEPSLATE_BRICKS);
            ring(es, cx, AY, cz, 28, 30, BlockTypes.COBBLED_DEEPSLATE);
            ring(es, cx, AY, cz, 42, 44, BlockTypes.DEEPSLATE_BRICKS);
        }
    }

    // =========================================================================
    // DETAIL ENHANCEMENT — ARENA 8 (CLIFFSIDE) EXTRA PASSES
    // =========================================================================

    private void buildArena8ExtraDetail(int cx, int cz) {
        try (EditSession es = newSession()) {
            // ── FIFTH SCATTER PASS — cliff terrain diversity ───────────────
            for (int i = 0; i < 450; i++) {
                int sx = cx - 60 + (i * 101 + 79) % 121;
                int sz = cz - 60 + (i * 97  + 73) % 121;
                int r  = i % 14;
                if      (r ==  0) block(es, sx, AY, sz, BlockTypes.CALCITE);
                else if (r ==  1) block(es, sx, AY, sz, BlockTypes.TUFF);
                else if (r ==  2) block(es, sx, AY, sz, BlockTypes.DIORITE);
                else if (r ==  3) block(es, sx, AY, sz, BlockTypes.POLISHED_DIORITE);
                else if (r ==  4) block(es, sx, AY, sz, BlockTypes.ANDESITE);
                else if (r ==  5) block(es, sx, AY, sz, BlockTypes.POLISHED_ANDESITE);
                else if (r ==  6) block(es, sx, AY, sz, BlockTypes.GRANITE);
                else if (r ==  7) block(es, sx, AY, sz, BlockTypes.POLISHED_GRANITE);
                else if (r ==  8) block(es, sx, AY, sz, BlockTypes.STONE);
                else if (r ==  9) block(es, sx, AY, sz, BlockTypes.GRAVEL);
                else if (r == 10) block(es, sx, AY, sz, BlockTypes.WHITE_TERRACOTTA);
                else if (r == 11) block(es, sx, AY, sz, BlockTypes.LIGHT_GRAY_TERRACOTTA);
                else if (r == 12) block(es, sx, AY, sz, BlockTypes.GRAY_TERRACOTTA);
                else              block(es, sx, AY, sz, BlockTypes.COBBLESTONE);
            }

            // ── GEOLOGICAL STRATA LAYERS ──────────────────────────────────
            BlockType[] strata = {
                BlockTypes.CALCITE, BlockTypes.TUFF,    BlockTypes.ANDESITE,
                BlockTypes.GRANITE, BlockTypes.DIORITE, BlockTypes.STONE
            };
            for (int band = -60; band <= 60; band += 10) {
                int colorIdx = ((band + 60) / 10) % strata.length;
                for (int t = -60; t <= 60; t++)
                    block(es, cx + t, AY, cz + band, strata[colorIdx]);
            }

            // ── CLIFF FACE LEDGES ─────────────────────────────────────────
            for (int ly = 1; ly <= 8; ly += 2) {
                fill(es, cx - 60, AY + ly, cz - 60, cx - 60, AY + ly, cz + 60, BlockTypes.CALCITE);
                fill(es, cx + 60, AY + ly, cz - 60, cx + 60, AY + ly, cz + 60, BlockTypes.TUFF);
                if (ly % 4 == 1) {
                    fill(es, cx - 61, AY + ly, cz - 60, cx - 61, AY + ly, cz + 60, BlockTypes.GRAVEL);
                    fill(es, cx + 61, AY + ly, cz - 60, cx + 61, AY + ly, cz + 60, BlockTypes.GRAVEL);
                }
            }

            // ── ROCK POOL DEPRESSIONS ─────────────────────────────────────
            for (int i = 0; i < 15; i++) {
                int rx = cx - 45 + (i * 59 + 37) % 91;
                int rz = cz - 45 + (i * 61 + 41) % 91;
                disk(es, rx, AY - 1, rz, 3, BlockTypes.WATER);
                ring(es, rx, AY, rz, 2, 3, BlockTypes.GRAVEL);
            }

            // ── TERRACOTTA BAND INLAYS ────────────────────────────────────
            BlockType[] tcBands = {
                BlockTypes.WHITE_TERRACOTTA, BlockTypes.LIGHT_GRAY_TERRACOTTA,
                BlockTypes.GRAY_TERRACOTTA,  BlockTypes.BROWN_TERRACOTTA
            };
            for (int row = -55; row <= 55; row += 14) {
                int bi = ((row + 55) / 14) % tcBands.length;
                for (int t = -55; t <= 55; t++)
                    block(es, cx + t, AY, cz + row, tcBands[bi]);
            }
        }
    }

    // =========================================================================
    // UNIVERSAL MICRO-DETAIL — applied to all arenas for maximum texture depth
    // =========================================================================

    /** Peppers every arena floor with fine-grained single-block accents that
     *  are deterministic (no Random) and add visual noise without repeating
     *  any macro pattern already laid by the per-arena builders. */
    private void buildMicroDetail(int cx, int cz, int halfSize,
                                  BlockType primary, BlockType secondary,
                                  BlockType accent1, BlockType accent2,
                                  BlockType accent3) {
        try (EditSession es = newSession()) {
            // Fine noise pass A — prime 127
            for (int i = 0; i < 300; i++) {
                int sx = cx - halfSize + (i * 127 + 11) % (halfSize * 2 + 1);
                int sz = cz - halfSize + (i * 131 + 13) % (halfSize * 2 + 1);
                int r  = i % 5;
                if      (r == 0) block(es, sx, AY, sz, primary);
                else if (r == 1) block(es, sx, AY, sz, secondary);
                else if (r == 2) block(es, sx, AY, sz, accent1);
                else if (r == 3) block(es, sx, AY, sz, accent2);
                else             block(es, sx, AY, sz, accent3);
            }
            // Fine noise pass B — prime 137
            for (int i = 0; i < 300; i++) {
                int sx = cx - halfSize + (i * 137 + 17) % (halfSize * 2 + 1);
                int sz = cz - halfSize + (i * 139 + 19) % (halfSize * 2 + 1);
                int r  = i % 5;
                if      (r == 0) block(es, sx, AY, sz, accent3);
                else if (r == 1) block(es, sx, AY, sz, accent1);
                else if (r == 2) block(es, sx, AY, sz, primary);
                else if (r == 3) block(es, sx, AY, sz, accent2);
                else             block(es, sx, AY, sz, secondary);
            }
        }
    }

    // =========================================================================
    // PUBLIC ENTRY-POINTS — call these from ArenaManager after building
    // =========================================================================

    /** Run every extra-detail method for all duel arenas. */
    public void buildAllArenaExtraDetail() {
        buildArena1ExtraDetail(300,   0);
        buildArena2ExtraDetail(-300,  0);
        buildArena3ExtraDetail(0,   300);
        buildArena4ExtraDetail(0,  -300);
        buildArena5ExtraDetail(220, -220);
        buildArena6ExtraDetail(-220, -220);
        buildArena7ExtraDetail(220,  220);
        buildArena8ExtraDetail(-220,  220);
        // Universal micro-detail per arena
        buildMicroDetail(300,   0,   60, BlockTypes.JUNGLE_LOG,   BlockTypes.PODZOL,         BlockTypes.MOSS_BLOCK,   BlockTypes.STONE_BRICKS, BlockTypes.GRASS_BLOCK);
        buildMicroDetail(-300,  0,   60, BlockTypes.SAND,         BlockTypes.SANDSTONE,       BlockTypes.GRAVEL,       BlockTypes.CALCITE,      BlockTypes.WHITE_TERRACOTTA);
        buildMicroDetail(0,   300,   60, BlockTypes.BAMBOO_MOSAIC,BlockTypes.MOSS_BLOCK,      BlockTypes.CHERRY_LEAVES,BlockTypes.GRASS_BLOCK,  BlockTypes.DIRT);
        buildMicroDetail(0,  -300,   60, BlockTypes.MUD,          BlockTypes.MUDDY_MANGROVE_ROOTS, BlockTypes.CLAY,   BlockTypes.MANGROVE_LOG, BlockTypes.MOSS_BLOCK);
        buildMicroDetail(220, -220,  60, BlockTypes.BASALT,       BlockTypes.BLACKSTONE,      BlockTypes.MAGMA_BLOCK,  BlockTypes.TUFF,         BlockTypes.NETHERRACK);
        buildMicroDetail(-220,-220,  60, BlockTypes.PRISMARINE,   BlockTypes.DARK_PRISMARINE, BlockTypes.SEA_LANTERN,  BlockTypes.SAND,         BlockTypes.GRAVEL);
        buildMicroDetail(220,  220,  60, BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.COBBLESTONE, BlockTypes.DEEPSLATE_BRICKS, BlockTypes.MOSS_BLOCK, BlockTypes.STONE);
        buildMicroDetail(-220, 220,  60, BlockTypes.CALCITE,      BlockTypes.TUFF,            BlockTypes.ANDESITE,     BlockTypes.GRANITE,      BlockTypes.DIORITE);
    }

    /** Run every extra-detail method for all FFA arenas. */
    public void buildAllFFAExtraDetail() {
        buildFFA1ExtraDetail(500,   0);
        buildFFA2ExtraDetail(-500,  0);
        buildFFA3ExtraDetail(0,   500);
        buildFFA4ExtraDetail(0,  -500);
        // Universal micro-detail per FFA
        buildMicroDetail(500,   0, 70, BlockTypes.JUNGLE_LOG,   BlockTypes.PODZOL,          BlockTypes.MOSS_BLOCK,       BlockTypes.STONE_BRICKS,     BlockTypes.GRASS_BLOCK);
        buildMicroDetail(-500,  0, 70, BlockTypes.SAND,         BlockTypes.PRISMARINE,       BlockTypes.SANDSTONE,        BlockTypes.WHITE_TERRACOTTA,  BlockTypes.GRAVEL);
        buildMicroDetail(0,   500, 70, BlockTypes.SANDSTONE,    BlockTypes.RED_SANDSTONE,    BlockTypes.ORANGE_TERRACOTTA,BlockTypes.TERRACOTTA,        BlockTypes.CALCITE);
        buildMicroDetail(0,  -500, 70, BlockTypes.MUD,          BlockTypes.CLAY,             BlockTypes.PACKED_MUD,       BlockTypes.MANGROVE_LOG,      BlockTypes.MOSS_BLOCK);
        buildMicroDetail(400, 400,  70, BlockTypes.BLACKSTONE,  BlockTypes.BASALT,           BlockTypes.MAGMA_BLOCK,      BlockTypes.OBSIDIAN,          BlockTypes.SOUL_SAND);
    }

    // =========================================================================
    // SPAWN BORDER DECORATIONS — polished edges for every arena spawn pad zone
    // =========================================================================

    /** Adds a decorative trim ring around the arena perimeter at ground level
     *  using the given border block, and places corner accent blocks using the
     *  accent type. Half-size is the radius from centre to wall. */
    private void buildSpawnBorderDecoration(int cx, int cz, int halfSize,
                                            BlockType border, BlockType corner,
                                            BlockType midMarker) {
        try (EditSession es = newSession()) {
            int h = halfSize;
            // Four sides — perimeter trim
            for (int t = -h; t <= h; t++) {
                block(es, cx + t, AY, cz - h, border);
                block(es, cx + t, AY, cz + h, border);
                block(es, cx - h, AY, cz + t, border);
                block(es, cx + h, AY, cz + t, border);
            }
            // Four corners — accent blocks
            block(es, cx - h, AY, cz - h, corner);
            block(es, cx + h, AY, cz - h, corner);
            block(es, cx - h, AY, cz + h, corner);
            block(es, cx + h, AY, cz + h, corner);
            // Mid-point markers on each side
            block(es, cx,     AY, cz - h, midMarker);
            block(es, cx,     AY, cz + h, midMarker);
            block(es, cx - h, AY, cz,     midMarker);
            block(es, cx + h, AY, cz,     midMarker);
            // Inner accent ring (2 blocks inside)
            for (int t = -h + 2; t <= h - 2; t++) {
                block(es, cx + t, AY, cz - h + 2, border);
                block(es, cx + t, AY, cz + h - 2, border);
                block(es, cx - h + 2, AY, cz + t, border);
                block(es, cx + h - 2, AY, cz + t, border);
            }
        }
    }

    /** Call once after all arenas are built to add border decorations. */
    public void buildAllBorderDecorations() {
        // Duel arenas (half-size 65)
        buildSpawnBorderDecoration(300,   0, 65, BlockTypes.MOSSY_STONE_BRICKS,         BlockTypes.CHISELED_STONE_BRICKS, BlockTypes.STONE_BRICKS);
        buildSpawnBorderDecoration(-300,  0, 65, BlockTypes.SANDSTONE,                  BlockTypes.CHISELED_SANDSTONE,    BlockTypes.SMOOTH_SANDSTONE);
        buildSpawnBorderDecoration(0,   300, 65, BlockTypes.BAMBOO_MOSAIC,              BlockTypes.BAMBOO_PLANKS,         BlockTypes.STRIPPED_BAMBOO_BLOCK);
        buildSpawnBorderDecoration(0,  -300, 65, BlockTypes.MUD_BRICKS,                BlockTypes.PACKED_MUD,            BlockTypes.MANGROVE_PLANKS);
        buildSpawnBorderDecoration(220, -220, 65, BlockTypes.POLISHED_BLACKSTONE_BRICKS,BlockTypes.GILDED_BLACKSTONE,    BlockTypes.CHISELED_POLISHED_BLACKSTONE);
        buildSpawnBorderDecoration(-220,-220, 65, BlockTypes.PRISMARINE_BRICKS,         BlockTypes.SEA_LANTERN,           BlockTypes.DARK_PRISMARINE);
        buildSpawnBorderDecoration(220,  220, 65, BlockTypes.DEEPSLATE_BRICKS,          BlockTypes.CHISELED_DEEPSLATE,   BlockTypes.COBBLED_DEEPSLATE);
        buildSpawnBorderDecoration(-220, 220, 65, BlockTypes.POLISHED_ANDESITE,         BlockTypes.POLISHED_DIORITE,      BlockTypes.CALCITE);
        // FFA arenas (half-size 75)
        buildSpawnBorderDecoration(500,   0, 75, BlockTypes.JUNGLE_PLANKS,             BlockTypes.JUNGLE_LOG,            BlockTypes.STRIPPED_JUNGLE_LOG);
        buildSpawnBorderDecoration(-500,  0, 75, BlockTypes.PRISMARINE,                BlockTypes.SEA_LANTERN,           BlockTypes.DARK_PRISMARINE);
        buildSpawnBorderDecoration(0,   500, 75, BlockTypes.SMOOTH_RED_SANDSTONE,      BlockTypes.CHISELED_SANDSTONE,    BlockTypes.CUT_SANDSTONE);
        buildSpawnBorderDecoration(0,  -500, 75, BlockTypes.MUD_BRICKS,               BlockTypes.PACKED_MUD,            BlockTypes.MANGROVE_LOG);
        buildSpawnBorderDecoration(400,  400, 75, BlockTypes.POLISHED_BLACKSTONE_BRICKS,BlockTypes.CHISELED_POLISHED_BLACKSTONE, BlockTypes.GILDED_BLACKSTONE);
    }

    // =========================================================================
    // TERRAIN HEIGHT VARIATION — raised ridges and depression bowls
    // =========================================================================

    /** Adds subtle height bumps (+1 block) scattered across the arena floor to
     *  break up the perfectly flat surface and give a more organic feel. */
    private void buildHeightVariation(int cx, int cz, int halfSize,
                                      BlockType raisedBlock, BlockType depressionBlock) {
        try (EditSession es = newSession()) {
            int sz2 = halfSize * 2 + 1;
            // Raised ridge rows running east-west
            for (int i = 0; i < 8; i++) {
                int rz = cz - halfSize + (i * (sz2 / 8));
                for (int t = -halfSize + 5; t <= halfSize - 5; t++) {
                    if ((t + i * 7) % 9 < 5)
                        block(es, cx + t, AY + 1, rz, raisedBlock);
                }
            }
            // Depression dots (one block lower, filled with block)
            for (int i = 0; i < 80; i++) {
                int dx = cx - halfSize + (i * 113 + 29) % sz2;
                int dz = cz - halfSize + (i * 107 + 31) % sz2;
                block(es, dx, AY - 1, dz, depressionBlock);
            }
        }
    }

    /** Apply height variation to every arena for organic terrain feel. */
    public void buildAllHeightVariation() {
        buildHeightVariation(300,    0, 60, BlockTypes.PODZOL,        BlockTypes.WATER);
        buildHeightVariation(-300,   0, 60, BlockTypes.SAND,          BlockTypes.WATER);
        buildHeightVariation(0,    300, 60, BlockTypes.BAMBOO_MOSAIC, BlockTypes.WATER);
        buildHeightVariation(0,   -300, 60, BlockTypes.MUD,           BlockTypes.WATER);
        buildHeightVariation(220,  -220, 60, BlockTypes.BASALT,       BlockTypes.LAVA);
        buildHeightVariation(-220, -220, 60, BlockTypes.PRISMARINE,   BlockTypes.WATER);
        buildHeightVariation(220,   220, 60, BlockTypes.MOSSY_STONE_BRICKS, BlockTypes.WATER);
        buildHeightVariation(-220,  220, 60, BlockTypes.CALCITE,      BlockTypes.WATER);
        buildHeightVariation(500,    0, 70, BlockTypes.JUNGLE_LOG,    BlockTypes.WATER);
        buildHeightVariation(-500,   0, 70, BlockTypes.SAND,          BlockTypes.WATER);
        buildHeightVariation(0,    500, 70, BlockTypes.SANDSTONE,     BlockTypes.WATER);
        buildHeightVariation(0,   -500, 70, BlockTypes.MUD,           BlockTypes.WATER);
        buildHeightVariation(400,   400, 70, BlockTypes.BLACKSTONE,   BlockTypes.LAVA);
    }

    // =========================================================================
    // CENTRE FEATURE — decorative centrepiece disk for each arena
    // =========================================================================

    /** Builds a small decorative disk at the exact centre of each arena so
     *  the middle of the map has a clear visual landmark and tactile texture. */
    private void buildCentreFeature(int cx, int cz,
                                    BlockType inner, BlockType mid, BlockType outer) {
        try (EditSession es = newSession()) {
            disk(es, cx, AY, cz, 3, inner);
            ring(es, cx, AY, cz, 3, 5, mid);
            ring(es, cx, AY, cz, 5, 7, outer);
            // Raised centre block
            block(es, cx, AY + 1, cz, inner);
        }
    }

    /** Apply centre features to all arenas. */
    public void buildAllCentreFeatures() {
        buildCentreFeature(300,   0,  BlockTypes.MOSSY_STONE_BRICKS,  BlockTypes.STONE_BRICKS,           BlockTypes.COBBLESTONE);
        buildCentreFeature(-300,  0,  BlockTypes.SMOOTH_SANDSTONE,    BlockTypes.SANDSTONE,              BlockTypes.RED_SANDSTONE);
        buildCentreFeature(0,   300,  BlockTypes.BAMBOO_MOSAIC,       BlockTypes.STRIPPED_BAMBOO_BLOCK,  BlockTypes.BAMBOO_PLANKS);
        buildCentreFeature(0,  -300,  BlockTypes.PACKED_MUD,          BlockTypes.MUD_BRICKS,             BlockTypes.MUD);
        buildCentreFeature(220, -220, BlockTypes.GILDED_BLACKSTONE,   BlockTypes.POLISHED_BLACKSTONE_BRICKS, BlockTypes.BLACKSTONE);
        buildCentreFeature(-220,-220, BlockTypes.SEA_LANTERN,         BlockTypes.PRISMARINE_BRICKS,      BlockTypes.DARK_PRISMARINE);
        buildCentreFeature(220,  220, BlockTypes.CHISELED_DEEPSLATE,  BlockTypes.DEEPSLATE_BRICKS,       BlockTypes.COBBLED_DEEPSLATE);
        buildCentreFeature(-220, 220, BlockTypes.POLISHED_DIORITE,    BlockTypes.POLISHED_ANDESITE,      BlockTypes.CALCITE);
        buildCentreFeature(500,   0,  BlockTypes.STRIPPED_JUNGLE_LOG, BlockTypes.JUNGLE_PLANKS,          BlockTypes.JUNGLE_LOG);
        buildCentreFeature(-500,  0,  BlockTypes.SEA_LANTERN,         BlockTypes.PRISMARINE,             BlockTypes.DARK_PRISMARINE);
        buildCentreFeature(0,   500,  BlockTypes.CHISELED_SANDSTONE,  BlockTypes.SMOOTH_RED_SANDSTONE,   BlockTypes.RED_SANDSTONE);
        buildCentreFeature(0,  -500,  BlockTypes.MUD_BRICKS,          BlockTypes.PACKED_MUD,             BlockTypes.MANGROVE_LOG);
        buildCentreFeature(400,  400, BlockTypes.CHISELED_POLISHED_BLACKSTONE, BlockTypes.GILDED_BLACKSTONE, BlockTypes.POLISHED_BLACKSTONE_BRICKS);
    }
}
