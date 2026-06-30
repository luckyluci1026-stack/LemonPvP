package com.lemonpvp.lemonpractice.listeners;

import com.lemonpvp.lemonpractice.LemonPractice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class FFAListener implements Listener {

    private final LemonPractice plugin;

    public FFAListener(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Lethal hit interception — no death screen; run a death-cam then respawn
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!plugin.getFfaManager().isInFfa(victim.getUniqueId())) return;

        // Already in the death-cam flow (spectator): swallow any stray damage.
        if (plugin.getFfaManager().isDying(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Would this hit be lethal? Account for absorption hearts.
        double remaining = victim.getHealth() + victim.getAbsorptionAmount() - event.getFinalDamage();
        if (remaining > 0.0) return; // survivable — let it through

        // Respect totems: let vanilla pop the totem instead of intercepting.
        if (hasTotem(victim)) return;

        event.setCancelled(true);
        plugin.getFfaManager().handleLethal(victim, resolveKiller(event));
    }

    private boolean hasTotem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return (main != null && main.getType() == Material.TOTEM_OF_UNDYING)
                || (off != null && off.getType() == Material.TOTEM_OF_UNDYING);
    }

    private Player resolveKiller(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent ed) {
            if (ed.getDamager() instanceof Player p) return p;
            if (ed.getDamager() instanceof Projectile proj) {
                ProjectileSource src = proj.getShooter();
                if (src instanceof Player sp) return sp;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Death (fallback only — combat deaths are intercepted above)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getFfaManager().isInFfa(player.getUniqueId())) {
            return;
        }

        // The intercepted death-cam path already handled this player.
        if (plugin.getFfaManager().isDying(player.getUniqueId())) {
            event.deathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = player.getKiller();
        if (killer != null) {
            plugin.getFfaManager().handleKill(killer, player);
        }

        // The victim's killstreak ends on death
        plugin.getFfaManager().resetKillstreak(player.getUniqueId());

        // Auto-respawn after the configured delay
        UUID deathUuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(deathUuid);
            if (p != null && p.isDead()) {
                p.spigot().respawn();
            }
        }, plugin.getConfig().getInt("ffa.respawn-delay-ticks", 60));
    }

    // -------------------------------------------------------------------------
    // Respawn — drop the player back inside the current roaming zone
    // -------------------------------------------------------------------------

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getFfaManager().isInFfa(player.getUniqueId())) {
            return;
        }

        Location spawn = plugin.getFfaManager().getRespawnLocation();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }

        // Re-equip the FFA kit, restore health and re-apply the border one tick
        // after respawn (inventory is cleared on death). Single respawn path.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getFfaManager().isInFfa(player.getUniqueId())) {
                plugin.getFfaManager().respawnEquip(player);
            }
        }, 1L);
    }

    // -------------------------------------------------------------------------
    // Damage — FFA players may only damage each other; non-FFA players are safe
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID damagerUuid = damager.getUniqueId();
        UUID victimUuid = victim.getUniqueId();

        // Cancel damage if either player is spectating
        if (plugin.getSpectatorManager().isSpectating(damagerUuid)
                || plugin.getSpectatorManager().isSpectating(victimUuid)) {
            event.setCancelled(true);
            return;
        }

        boolean damagerInFfa = plugin.getFfaManager().isInFfa(damagerUuid);
        boolean victimInFfa = plugin.getFfaManager().isInFfa(victimUuid);

        // If exactly one is in the FFA — cancel to prevent griefing.
        if (damagerInFfa != victimInFfa) {
            event.setCancelled(true);
        }
        // Both in FFA (or neither) — allow normal handling.
    }

    // -------------------------------------------------------------------------
    // Hunger — no hunger outside of an active FFA / duel
    // -------------------------------------------------------------------------

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean inFfa = plugin.getFfaManager().isInFfa(uuid);
        boolean inDuel = plugin.getDuelManager().isInDuel(uuid);

        if (!inFfa && !inDuel) {
            event.setCancelled(true);
        }
    }
}
