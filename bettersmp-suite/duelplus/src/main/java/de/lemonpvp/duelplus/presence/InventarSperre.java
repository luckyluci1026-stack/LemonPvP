package de.lemonpvp.duelplus.presence;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventarSperre implements Listener {

    private static final long HINWEIS_ABSTAND_MILLIS = 2_000L;

    private final DuelPlus plugin;
    private final Map<UUID, Long> gesperrtBis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> letzterHinweis = new ConcurrentHashMap<>();

    public InventarSperre(DuelPlus plugin) {
        this.plugin = plugin;
    }

    public void sperren(UUID spieler, long millis) {
        gesperrtBis.merge(spieler, System.currentTimeMillis() + millis, Math::max);
    }

    public void freigebenWennNichtsOffen(UUID spieler) {
        Long stand = gesperrtBis.get(spieler);
        if (stand == null) {
            return;
        }
        plugin.db().offenesErgebnis(spieler).thenAccept(offen -> {
            if (!offen) {
                gesperrtBis.remove(spieler, stand);
            }
        });
    }

    public boolean gesperrt(UUID spieler) {
        Long bis = gesperrtBis.get(spieler);
        if (bis == null) {
            return false;
        }
        if (bis <= System.currentTimeMillis()) {
            gesperrtBis.remove(spieler, bis);
            return false;
        }
        return true;
    }

    private void abfangen(Entity wer, Cancellable event) {
        if (!(wer instanceof Player spieler) || !gesperrt(spieler.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        long jetzt = System.currentTimeMillis();
        Long zuletzt = letzterHinweis.get(spieler.getUniqueId());
        if (zuletzt == null || jetzt - zuletzt >= HINWEIS_ABSTAND_MILLIS) {
            letzterHinweis.put(spieler.getUniqueId(), jetzt);
            spieler.sendActionBar(plugin.msgs().format("inventory-locked"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimDroppen(PlayerDropItemEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimAufheben(EntityPickupItemEvent event) {
        abfangen(event.getEntity(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimKlicken(InventoryClickEvent event) {
        abfangen(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimZiehen(InventoryDragEvent event) {
        abfangen(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimPlatzieren(BlockPlaceEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimBenutzen(PlayerInteractEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimBenutzenAnEntity(PlayerInteractEntityEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimRuestungsstaender(PlayerArmorStandManipulateEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimEssen(PlayerItemConsumeEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimHandTauschen(PlayerSwapHandItemsEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimBefehl(PlayerCommandPreprocessEvent event) {
        abfangen(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimSchaden(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player spieler && gesperrt(spieler.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void beimTod(PlayerDeathEvent event) {
        if (gesperrt(event.getPlayer().getUniqueId())) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        gesperrtBis.remove(event.getPlayer().getUniqueId());
        letzterHinweis.remove(event.getPlayer().getUniqueId());
    }
}
