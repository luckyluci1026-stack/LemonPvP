package com.lemonpvp.lemontraining.practice;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.gui.SwordDifficultyGUI;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.model.PracticeSession;
import com.lemonpvp.lemontraining.model.SwordDifficulty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class SwordPractice extends AbstractPractice {

    private UUID zombieUUID;
    private SwordDifficulty difficulty;

    public SwordPractice(LemonTraining plugin, Player player, PracticeSession session) {
        super(plugin, player, session);
    }

    @Override
    public void start() {
        // Open difficulty GUI first; actual practice starts via startWithDifficulty()
        Bukkit.getScheduler().runTask(plugin, () ->
                new SwordDifficultyGUI(plugin, this).open(player));
    }

    public void startWithDifficulty(SwordDifficulty diff) {
        this.difficulty = diff;
        session.setSwordDifficulty(diff);

        Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.SWORD);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.getInventory().clear();

        // Diamond Sword, unbreakable, no enchants
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            sword.setItemMeta(meta);
        }
        player.getInventory().setItem(0, sword);

        applyPracticeEffects();
        giveLeaveItem();

        spawnZombie(spawn, diff);

        // Boundary check every 5 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.getArenaManager().isInsideArena(player.getLocation(), PracticeMode.SWORD)) {
                Location spawnBack = plugin.getArenaManager().getSpawnLocation(PracticeMode.SWORD);
                if (spawnBack != null) player.teleport(spawnBack);
            }
        }, 5L, 5L));
    }

    private void spawnZombie(Location center, SwordDifficulty diff) {
        if (center == null) return;
        Location loc = center.clone().add(3, 0, 0);
        Zombie zombie = (Zombie) center.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        switch (diff) {
            case EASY -> {
                zombie.setAI(false);
                zombie.setCustomName("Training Dummy");
                zombie.setCustomNameVisible(true);
            }
            case MEDIUM -> {
                zombie.setAI(true);
                zombie.setCustomName("Training Dummy (Medium)");
                zombie.setCustomNameVisible(true);
                zombie.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
            }
            case NORMAL -> {
                zombie.setAI(true);
                zombie.setCustomName("Training Dummy (Normal)");
                zombie.setCustomNameVisible(true);
            }
        }

        zombie.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 254, false, false));

        NamespacedKey key = new NamespacedKey(plugin, "training_zombie");
        zombie.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        zombieUUID = zombie.getUniqueId();
    }

    public UUID getZombieUUID() {
        return zombieUUID;
    }

    public SwordDifficulty getDifficulty() {
        return difficulty;
    }

    public void respawnZombie() {
        if (difficulty == null) return;
        Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.SWORD);
        spawnZombie(spawn, difficulty);
    }

    @Override
    public void end() {
        if (zombieUUID != null) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(zombieUUID);
            if (entity != null) entity.remove();
        }
        cancelTasks();
        player.getInventory().clear();
        sendToLobby();
    }
}
