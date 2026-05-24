package com.lemonpvp.lemontraining.practice;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.model.PracticeSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class TotemPractice extends AbstractPractice {

    private static final int TOTEM_COUNT = 36;
    private static final Random RANDOM = new Random();

    public TotemPractice(LemonTraining plugin, Player player, PracticeSession session) {
        super(plugin, player, session);
    }

    @Override
    public void start() {
        Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.TOTEM);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.getInventory().clear();
        refillTotems();
        applyPracticeEffects();
        giveLeaveItem();

        // Anvil timer: every 60 ticks spawn an anvil above the player
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            Location playerLoc = player.getLocation();
            double offsetX = (RANDOM.nextDouble() * 10) - 5;
            double offsetZ = (RANDOM.nextDouble() * 10) - 5;
            Location spawnLoc = playerLoc.clone().add(offsetX, 20, offsetZ);
            FallingBlock fb = player.getWorld().spawnFallingBlock(spawnLoc, Material.ANVIL.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(false);
        }, 60L, 60L));

        // Totem check: every 20 ticks refill if 0 totems remain
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                    count += item.getAmount();
                }
            }
            if (count == 0) {
                refillTotems();
            }
        }, 20L, 20L));

        // Boundary check every 5 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.getArenaManager().isInsideArena(player.getLocation(), PracticeMode.TOTEM)) {
                Location spawnBack = plugin.getArenaManager().getSpawnLocation(PracticeMode.TOTEM);
                if (spawnBack != null) player.teleport(spawnBack);
            }
        }, 5L, 5L));
    }

    public void refillTotems() {
        // Fill first 8 slots (0-7) with totems (leaving slot 8 for leave item)
        for (int i = 0; i < 8; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
        // Also fill offhand
        player.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
    }

    @Override
    public void end() {
        cancelTasks();
        player.getInventory().clear();
        sendToLobby();
    }
}
