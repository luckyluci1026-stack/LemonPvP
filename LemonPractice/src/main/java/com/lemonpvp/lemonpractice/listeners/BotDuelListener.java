package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Glue for bot fallback duels: finishes the match on either side dying or the
 * player quitting, keeps the bot locked onto its own opponent, and stops
 * bystanders from interfering with someone else's bot.
 */
public class BotDuelListener implements Listener {

    private final LemonPractice plugin;

    public BotDuelListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // Player died → bot wins.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getBotDuelManager().isInBotDuel(player.getUniqueId())) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        plugin.getBotDuelManager().endForPlayer(player.getUniqueId(), false, false);
        // Get them back on their feet quickly; the manager sends them to the lobby.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.isDead()) {
                player.spigot().respawn();
            }
        }, 20L);
    }

    // Bot died → player wins.
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        if (!plugin.getBotDuelManager().isBot(uuid)) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        plugin.getBotDuelManager().endForBot(uuid, true);
    }

    // Quit mid-match → silent cleanup.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBotDuelManager().endForPlayer(event.getPlayer().getUniqueId(), false, true);
    }

    // The bot only ever targets its own opponent.
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        UUID botUuid = event.getEntity().getUniqueId();
        UUID owner = plugin.getBotDuelManager().ownerOfBot(botUuid);
        if (owner == null) return;
        if (!(event.getTarget() instanceof Player target) || !target.getUniqueId().equals(owner)) {
            event.setCancelled(true);
        }
    }

    // Mirror damage flashes onto the packet player model (v2 visuals).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBotDamaged(org.bukkit.event.entity.EntityDamageEvent event) {
        if (plugin.getBotDuelManager().isBot(event.getEntity().getUniqueId())) {
            plugin.getBotDuelManager().notifyBotDamaged(event.getEntity().getUniqueId());
        }
    }

    // Bystanders can't hit someone else's bot; the bot can't hit bystanders.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Someone hitting a bot that isn't theirs.
        UUID victimBotOwner = plugin.getBotDuelManager().ownerOfBot(event.getEntity().getUniqueId());
        if (victimBotOwner != null && event.getDamager() instanceof Player damager
                && !damager.getUniqueId().equals(victimBotOwner)) {
            event.setCancelled(true);
            return;
        }
        // A bot hitting anyone but its own opponent.
        UUID damagerBotOwner = plugin.getBotDuelManager().ownerOfBot(event.getDamager().getUniqueId());
        if (damagerBotOwner != null && event.getEntity() instanceof Player victim
                && !victim.getUniqueId().equals(damagerBotOwner)) {
            event.setCancelled(true);
        }
    }
}
