package de.lemonpvp.smpcontent.listener;

import de.lemonpvp.smpcontent.SMPContent;
import de.lemonpvp.smpcontent.content.CustomEntry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Löst die Spezialfähigkeiten aus: Rechtsklick, Linksklick, Treffer und Kill.
 * Der Auslöser "held" (dauerhaft, solange man das Item hält) läuft als
 * eigener Zeitgeber im Hauptplugin.
 */
public final class AbilityListener implements Listener {

    private final SMPContent plugin;

    public AbilityListener(SMPContent plugin) {
        this.plugin = plugin;
    }

    /**
     * Eine Kugel verschwindet, wenn sie irgendwo ankommt.
     *
     * Ein Pfeil bleibt sonst eine Minute lang im Boden stecken. Wer mit der
     * Pistole auf den Boden hält, steht danach vor einem Igel aus Pfeilen -
     * im Kreativmodus fällt das am meisten auf, weil man dort endlos
     * schießen kann. An der Einschlagstelle bleibt ein kleiner Rauchpunkt.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        org.bukkit.entity.Projectile shot = event.getEntity();
        if (!shot.getPersistentDataContainer().has(
                de.lemonpvp.smpcontent.ability.Abilities.KUGEL,
                org.bukkit.persistence.PersistentDataType.BYTE)) {
            return;
        }
        shot.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                shot.getLocation(), 4, 0.05, 0.05, 0.05, 0.01);
        shot.remove();
    }

    /**
     * WICHTIG: hier ohne ignoreCancelled.
     *
     * Bukkit setzt bei einem Klick in die LUFT von sich aus
     * useInteractedBlock = DENY (es gibt ja keinen Block), und
     * isCancelled() liefert genau dann true. Das Event gilt also schon vor
     * jedem Plugin als abgebrochen - mit ignoreCancelled = true kämen nur
     * Klicks auf Blöcke an. Ob die Benutzung wirklich verboten wurde,
     * steht stattdessen in useItemInHand().
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        String trigger = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> "right-click";
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> "left-click";
            default -> null;
        };
        if (trigger == null) {
            return;
        }
        CustomEntry entry = entryOf(event.getItem());
        if (entry == null) {
            return;
        }
        // Eigene Blöcke sollen sich weiterhin normal setzen lassen
        if (entry.block()) {
            return;
        }

        // Geduckt zuerst: so kann ein Item zwei verschiedene Sachen können.
        // Gibt es dafür nichts, gilt der normale Auslöser.
        boolean fired = false;
        if (event.getPlayer().isSneaking()) {
            fired = plugin.abilities().run(event.getPlayer(), entry, "sneak-" + trigger, null);
        }
        if (!fired) {
            fired = plugin.abilities().run(event.getPlayer(), entry, trigger, null);
        }
        if (fired && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Nur die Kiste/Tür nicht öffnen - das Item selbst bleibt nutzbar
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    /** Q drücken - praktisch für einen Wurf oder eine Fernwirkung. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        CustomEntry entry = entryOf(event.getItemDrop().getItemStack());
        if (entry != null && !entry.block()
                && plugin.abilities().run(event.getPlayer(), entry, "drop", null)) {
            // Das Item bleibt in der Hand, wenn eine Fähigkeit ausgelöst hat
            event.setCancelled(true);
        }
    }

    /** F drücken (Hände tauschen). */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        CustomEntry entry = entryOf(event.getOffHandItem());
        if (entry != null && !entry.block()
                && plugin.abilities().run(event.getPlayer(), entry, "swap", null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        CustomEntry entry = entryOf(player.getInventory().getItemInMainHand());
        if (entry != null && !entry.block()) {
            plugin.abilities().run(player, entry, "hit", victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        CustomEntry entry = entryOf(killer.getInventory().getItemInMainHand());
        if (entry != null && !entry.block()) {
            plugin.abilities().run(killer, entry, "kill", event.getEntity());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.abilities().forget(event.getPlayer().getUniqueId());
    }

    private CustomEntry entryOf(ItemStack item) {
        String id = plugin.registry().idOf(item);
        return id == null ? null : plugin.registry().get(id);
    }
}
