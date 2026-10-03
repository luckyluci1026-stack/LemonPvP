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
    /**
     * Eine Kugel trifft.
     *
     * Hier passiert mehr, als es aussieht - und das aus zwei Gründen:
     *
     * 1. **Unverwundbarkeit.** Nach einem Treffer ist ein Ziel zehn Ticks
     *    lang unverwundbar. Die Pistole darf aber alle acht Ticks schießen,
     *    und die Schrotflinte schickt acht Kugeln gleichzeitig los. Über den
     *    normalen Pfeilweg käme also jeder zweite Schuss gar nicht an und von
     *    acht Schrotkugeln genau eine - die Waffe fühlte sich an, als mache
     *    sie keinen Schaden. Deshalb setzt die Kugel die Sperre zurück und
     *    trägt ihren Schaden selbst ein.
     *
     * 2. **Verlässlicher Wert.** Vanilla rechnet Pfeilschaden mal
     *    Fluggeschwindigkeit: aus {@code damage: 5.0} wurden bei Tempo 3.6
     *    achtzehn Punkte. Was in der Datei steht, soll aber genau das sein,
     *    was ankommt.
     *
     * Dazu ein Trefferzeichen für den Schützen - ohne Rückmeldung weiß man
     * bei einer unsichtbaren Kugel nie, ob man getroffen hat.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        org.bukkit.entity.Projectile shot = event.getEntity();
        if (!shot.getPersistentDataContainer().has(
                de.lemonpvp.smpcontent.ability.Abilities.KUGEL,
                org.bukkit.persistence.PersistentDataType.BYTE)) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity ziel) {
            treffer(shot, ziel);
            // Vanilla soll den Pfeiltreffer nicht noch einmal abrechnen.
            // Beim Treffer auf einen Block hat das Abbrechen keine Wirkung -
            // dort räumt gleich das remove() auf.
            event.setCancelled(true);
        } else {
            shot.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                    shot.getLocation(), 4, 0.05, 0.05, 0.05, 0.01);
        }
        shot.remove();
    }

    private void treffer(org.bukkit.entity.Projectile shot, LivingEntity ziel) {
        Double schaden = shot.getPersistentDataContainer().get(
                de.lemonpvp.smpcontent.ability.Abilities.KUGEL_SCHADEN,
                org.bukkit.persistence.PersistentDataType.DOUBLE);
        if (schaden == null || schaden <= 0) {
            return;
        }
        // Die Sperre aufheben, sonst schluckt sie den Schaden. Beides ist
        // nötig: noDamageTicks lässt den Treffer überhaupt durch,
        // lastDamage sorgt dafür, dass nicht nur die Differenz zählt.
        ziel.setNoDamageTicks(0);
        ziel.setLastDamage(0);
        org.bukkit.entity.Entity schuetze = shot.getShooter() instanceof org.bukkit.entity.Entity e
                ? e : null;
        if (schuetze != null) {
            ziel.damage(schaden, schuetze);
        } else {
            ziel.damage(schaden);
        }
        ziel.getWorld().spawnParticle(org.bukkit.Particle.CRIT,
                ziel.getLocation().add(0, ziel.getHeight() * 0.6, 0),
                8, 0.2, 0.2, 0.2, 0.15);
        if (shot.getShooter() instanceof Player p) {
            p.playSound(p, org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 1.6f);
        }
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
