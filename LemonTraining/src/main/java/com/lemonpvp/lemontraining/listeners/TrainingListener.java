package com.lemonpvp.lemontraining.listeners;

import com.lemonpvp.lemontraining.LemonTraining;
import com.lemonpvp.lemontraining.model.PracticeMode;
import com.lemonpvp.lemontraining.practice.*;
import com.sk89q.worldedit.math.BlockVector3;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class TrainingListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonTraining plugin;
    private final NamespacedKey zombieKey;
    private final NamespacedKey leaveKey;

    public TrainingListener(LemonTraining plugin) {
        this.plugin = plugin;
        this.zombieKey = new NamespacedKey(plugin, "training_zombie");
        this.leaveKey  = new NamespacedKey(plugin, "leave");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Start pending practice mode (arrived from lobby selection)
        if (plugin.getPracticeManager().hasPending(uuid)) {
            PracticeMode mode = plugin.getPracticeManager().pollPending(uuid);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getPracticeManager().startPractice(player, mode);
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getPracticeManager().isInSession(uuid)) {
            plugin.getPracticeManager().endPractice(uuid, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getPracticeManager().isInSession(player.getUniqueId())) return;
        // Cancel fall damage
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Cancel training zombie attacks on player
        if (!(event.getEntity() instanceof Player player)) return;
        Entity attacker = event.getDamager();
        if (isTrainingZombie(attacker)) {
            event.setCancelled(true);
        }
        // Also: stop PvP between players in sessions
        if (attacker instanceof Player attackingPlayer) {
            if (plugin.getPracticeManager().isInSession(player.getUniqueId())
                    || plugin.getPracticeManager().isInSession(attackingPlayer.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!isTrainingZombie(zombie)) return;
        event.setDroppedExp(0);
        event.getDrops().clear();

        // Check all active sessions for a zombie that matches
        for (UUID uuid : new java.util.HashSet<>(plugin.getPracticeManager().activeSessions().keySet())) {
            AbstractPractice practice = plugin.getPracticeManager().getSession(uuid);
            if (practice instanceof SwordPractice sp && zombie.getUniqueId().equals(sp.getZombieUUID())) {
                // Respawn zombie after 1 tick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getPracticeManager().isInSession(uuid)) {
                        sp.respawnZombie();
                    }
                }, 1L);
            } else if (practice instanceof CrystalPractice cp && zombie.getUniqueId().equals(cp.getZombieUUID())) {
                cp.onZombieDeath(zombie);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        if (!plugin.getPracticeManager().isInSession(uuid)) return;

        AbstractPractice practice = plugin.getPracticeManager().getSession(uuid);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        Location spawn = null;
        if (practice != null) {
            spawn = plugin.getArenaManager().getSpawnLocation(practice.getSession().getMode());
        }
        final Location respawnLoc = spawn;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                if (respawnLoc != null) player.teleport(respawnLoc);
                player.setHealth(player.getMaxHealth());
                // Refill totems if in totem practice
                if (practice instanceof TotemPractice tp) {
                    tp.refillTotems();
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (!plugin.getPracticeManager().isInSession(player.getUniqueId())) return;

        AbstractPractice practice = plugin.getPracticeManager().getSession(player.getUniqueId());
        if (!(practice instanceof BowPractice bowPractice)) return;

        // Check if hit block is a TARGET at a bow target position
        if (event.getHitBlock() == null || event.getHitBlock().getType() != Material.TARGET) return;

        Location hitLoc = event.getHitBlock().getLocation();
        BlockVector3 hitVec = BlockVector3.at(hitLoc.getBlockX(), hitLoc.getBlockY(), hitLoc.getBlockZ());

        if (plugin.getArenaManager().getBowTargetPositions().contains(hitVec)) {
            bowPractice.onTargetHit(hitVec);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fb)) return;
        if (fb.getBlockData().getMaterial() == Material.ANVIL) {
            // Prevent the anvil from forming as a block
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!plugin.getPracticeManager().isInSession(uuid)) return;

        // Delegate to TrainingGUI / SwordDifficultyGUI
        AbstractPractice practice = plugin.getPracticeManager().getSession(uuid);
        if (practice instanceof SwordPractice sp) {
            // SwordDifficultyGUI click is handled by its own handler registered via TrainingListener
            // Just cancel shift-clicks on armor/hotbar in active session
        }

        // Prevent moving leave item or practice items
        org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.hasItemMeta()) {
            if (clicked.getItemMeta().getPersistentDataContainer().has(leaveKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        org.bukkit.inventory.ItemStack item = event.getItem();

        // Handle SwordDifficultyGUI clicks (opened inventory handles it)
        // Handle TrainingGUI clicks
        AbstractPractice practice = plugin.getPracticeManager().getSession(uuid);

        if (item == null) return;

        // Leave item click
        if (item.getType() == Material.RED_DYE && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(leaveKey, PersistentDataType.BYTE)
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            if (plugin.getPracticeManager().isInSession(uuid)) {
                plugin.getPracticeManager().endPractice(uuid, true);
                player.sendMessage(MM.deserialize(
                        plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("leave", "<yellow>You left practice.")));
            }
            return;
        }

        // Wind Charge refill in Mace practice
        if (item.getType() == Material.WIND_CHARGE && practice instanceof MacePractice mp) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Bukkit.getScheduler().runTaskLater(plugin, mp::refillWindCharges, 1L);
            }
        }

        // Bow practice: refill arrows after shooting
        if ((item.getType() == Material.BOW || item.getType() == Material.CROSSBOW)
                && practice instanceof BowPractice bp) {
            Bukkit.getScheduler().runTaskLater(plugin, bp::refillArrows, 1L);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getPracticeManager().isInSession(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean isTrainingZombie(Entity entity) {
        if (!(entity instanceof Zombie zombie)) return false;
        return zombie.getPersistentDataContainer().has(zombieKey, PersistentDataType.BYTE);
    }
}
