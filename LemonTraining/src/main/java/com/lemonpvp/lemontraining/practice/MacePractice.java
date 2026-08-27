package com.lemonpvp.lemontraining.practice;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.model.PracticeSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MacePractice extends AbstractPractice {

    private final List<UUID> spawnedZombies = new ArrayList<>();

    public MacePractice(LemonTraining plugin, Player player, PracticeSession session) {
        super(plugin, player, session);
    }

    @Override
    public void start() {
        Location spawn = plugin.getArenaManager().getSpawnLocation(PracticeMode.MACE);
        if (spawn != null) {
            player.teleport(spawn);
        }
        player.getInventory().clear();

        // Slot 0: Mace with Wind Burst I + Density 5, unbreakable
        ItemStack windBurstMace = new ItemStack(Material.MACE);
        ItemMeta wbMeta = windBurstMace.getItemMeta();
        if (wbMeta != null) {
            wbMeta.displayName(MM.deserialize("<aqua>Wind Burst Mace</aqua>"));
            wbMeta.addEnchant(Enchantment.WIND_BURST, 1, true);
            wbMeta.addEnchant(Enchantment.DENSITY, 5, true);
            wbMeta.setUnbreakable(true);
            windBurstMace.setItemMeta(wbMeta);
        }
        player.getInventory().setItem(0, windBurstMace);

        // Slot 1: Mace with Breach IV, unbreakable
        ItemStack breachMace = new ItemStack(Material.MACE);
        ItemMeta bmMeta = breachMace.getItemMeta();
        if (bmMeta != null) {
            bmMeta.displayName(MM.deserialize("<light_purple>Breach Mace</light_purple>"));
            bmMeta.addEnchant(Enchantment.BREACH, 4, true);
            bmMeta.setUnbreakable(true);
            breachMace.setItemMeta(bmMeta);
        }
        player.getInventory().setItem(1, breachMace);

        // Slot 2: Diamond Sword, unbreakable
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.setUnbreakable(true);
            sword.setItemMeta(swordMeta);
        }
        player.getInventory().setItem(2, sword);

        // Slots 3–8: Wind Charge (64 each, leave slot 8 for leave item temporarily)
        for (int i = 3; i <= 7; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.WIND_CHARGE, 64));
        }

        applyPracticeEffects();
        giveLeaveItem();

        // Spawn 3 training dummy zombies
        spawnDummies(spawn);

        // Wind charge refill task every 20 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            for (int i = 3; i <= 7; i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot == null || slot.getType() != Material.WIND_CHARGE) {
                    player.getInventory().setItem(i, new ItemStack(Material.WIND_CHARGE, 64));
                } else if (slot.getAmount() < 64) {
                    slot.setAmount(64);
                }
            }
        }, 20L, 20L));

        // Boundary check every 5 ticks
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.getArenaManager().isInsideArena(player.getLocation(), PracticeMode.MACE)) {
                Location spawnBack = plugin.getArenaManager().getSpawnLocation(PracticeMode.MACE);
                if (spawnBack != null) player.teleport(spawnBack);
            }
        }, 5L, 5L));
    }

    private void spawnDummies(Location center) {
        if (center == null) return;
        double[][] offsets = {{3, 0}, {-3, 0}, {0, 3}};
        for (double[] offset : offsets) {
            Location loc = center.clone().add(offset[0], 0, offset[1]);
            Zombie zombie = (Zombie) center.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
            zombie.setAI(false);
            zombie.setCustomName("Training Dummy");
            zombie.setCustomNameVisible(true);
            zombie.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 254, false, false));
            NamespacedKey key = new NamespacedKey(plugin, "training_zombie");
            zombie.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            spawnedZombies.add(zombie.getUniqueId());
        }
    }

    public void onWindChargeUse() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            refillWindCharges();
        }, 1L);
    }

    public void refillWindCharges() {
        if (!player.isOnline()) return;
        for (int i = 3; i <= 7; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType() != Material.WIND_CHARGE || slot.getAmount() < 64) {
                player.getInventory().setItem(i, new ItemStack(Material.WIND_CHARGE, 64));
            }
        }
    }

    public List<UUID> getSpawnedZombies() {
        return spawnedZombies;
    }

    @Override
    public void end() {
        // Remove spawned zombies
        for (UUID uuid : spawnedZombies) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        spawnedZombies.clear();
        cancelTasks();
        player.getInventory().clear();
        sendToLobby();
    }
}
