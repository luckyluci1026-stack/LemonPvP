package com.lemonpvp.lemontraining.practice;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.model.PracticeSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class CrystalPractice extends AbstractPractice {

    private UUID zombieUUID;

    public CrystalPractice(LemonTraining plugin, Player player, PracticeSession session) {
        super(plugin, player, session);
    }

    @Override
    public void start() {
        Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.CRYSTAL);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.getInventory().clear();

        equipCrystalKit();
        applyPracticeEffects();
        // No leave item in inventory for crystal practice

        spawnZombie(spawn);

        // Boundary check every 5 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.getArenaManager().isInsideArena(player.getLocation(), PracticeMode.CRYSTAL)) {
                Location spawnBack = plugin.getArenaManager().getSpawnLocation(PracticeMode.CRYSTAL);
                if (spawnBack != null) player.teleport(spawnBack);
            }
        }, 5L, 5L));
    }

    private void equipCrystalKit() {
        ConfigurationSection kit = plugin.getConfig().getConfigurationSection("crystal-kit");

        Material helmet = parseMaterial(kit, "helmet", Material.DIAMOND_HELMET);
        Material chestplate = parseMaterial(kit, "chestplate", Material.DIAMOND_CHESTPLATE);
        Material leggings = parseMaterial(kit, "leggings", Material.DIAMOND_LEGGINGS);
        Material boots = parseMaterial(kit, "boots", Material.DIAMOND_BOOTS);
        Material swordMat = parseMaterial(kit, "sword", Material.DIAMOND_SWORD);
        int totemCount = kit != null ? kit.getInt("totem-count", 16) : 16;

        player.getInventory().setHelmet(new ItemStack(helmet));
        player.getInventory().setChestplate(new ItemStack(chestplate));
        player.getInventory().setLeggings(new ItemStack(leggings));
        player.getInventory().setBoots(new ItemStack(boots));
        player.getInventory().setItem(0, new ItemStack(swordMat));

        // Fill rest of hotbar with totems
        int totems = totemCount;
        for (int i = 1; i <= 8 && totems > 0; i++) {
            int give = Math.min(totems, 1);
            player.getInventory().setItem(i, new ItemStack(Material.TOTEM_OF_UNDYING, give));
            totems -= give;
        }
        player.getInventory().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
    }

    private Material parseMaterial(ConfigurationSection sec, String key, Material fallback) {
        if (sec == null) return fallback;
        String val = sec.getString(key);
        if (val == null) return fallback;
        try {
            return Material.valueOf(val.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private void spawnZombie(Location center) {
        if (center == null) return;
        Location loc = center.clone().add(3, 0, 0);
        Zombie zombie = (Zombie) center.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        zombie.setAI(true);
        zombie.setCustomName("Crystal Dummy");
        zombie.setCustomNameVisible(true);
        zombie.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 254, false, false));

        // Visual: wear diamond armor
        EntityEquipment eq = zombie.getEquipment();
        if (eq != null) {
            eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            eq.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            eq.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        }

        NamespacedKey key = new NamespacedKey(plugin, "training_zombie");
        zombie.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        zombieUUID = zombie.getUniqueId();
    }

    public UUID getZombieUUID() {
        return zombieUUID;
    }

    public void onZombieDeath(Zombie zombie) {
        // Respawn the zombie immediately since it died via /kill or other means
        zombieUUID = null;
        zombie.remove();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.CRYSTAL);
            spawnZombie(spawn);
        }, 1L);
    }

    @Override
    public void end() {
        if (zombieUUID != null) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(zombieUUID);
            if (entity != null) entity.remove();
        }
        zombieUUID = null;
        cancelTasks();
        player.getInventory().clear();
        sendToLobby();
    }
}
