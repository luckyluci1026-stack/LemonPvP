package de.lemonpvp.lobbylock;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Die einzelnen Sperren aus der config.yml - jede fuer sich abschaltbar.
 * lobbylock.bypass (Standard: nur OP) geht ueber alles hier.
 */
public final class LockListener implements Listener {

    private final LobbyLock plugin;
    private final Map<UUID, Long> letzteWarnung = new ConcurrentHashMap<>();

    public LockListener(LobbyLock plugin) {
        this.plugin = plugin;
    }

    private boolean zustaendig(Player spieler) {
        List<String> welten = plugin.getConfig().getStringList("worlds");
        return welten.isEmpty() || welten.contains(spieler.getWorld().getName());
    }

    private boolean gesperrt(Player spieler, String schluessel) {
        return plugin.getConfig().getBoolean(schluessel, true)
                && zustaendig(spieler)
                && !spieler.hasPermission("lobbylock.bypass");
    }

    private void warnen(Player spieler) {
        if (!plugin.getConfig().getBoolean("feedback.enabled", true)) {
            return;
        }
        long jetzt = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("feedback.cooldown-seconds", 2) * 1000L;
        Long letzte = letzteWarnung.get(spieler.getUniqueId());
        if (letzte != null && jetzt - letzte < cooldownMs) {
            return;
        }
        letzteWarnung.put(spieler.getUniqueId(), jetzt);
        spieler.sendActionBar(LobbyLock.mm(plugin.getConfig().getString("feedback.message",
                "<red>Das ist hier nicht erlaubt.</red>")));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimDroppen(PlayerDropItemEvent event) {
        if (gesperrt(event.getPlayer(), "block-drop")) {
            event.setCancelled(true);
            warnen(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimOffhandTausch(PlayerSwapHandItemsEvent event) {
        if (gesperrt(event.getPlayer(), "block-offhand-swap")) {
            event.setCancelled(true);
            warnen(event.getPlayer());
        }
    }

    /**
     * Nur Klicks im eigenen Inventar sperren - Klicks in einem fremden
     * GUI (Serverwaehler etc.) sollen davon unberuehrt bleiben, egal
     * welches andere Plugin es oeffnet.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimKlicken(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player spieler)) {
            return;
        }
        Inventory geklickt = event.getClickedInventory();
        if (geklickt != null && geklickt.equals(spieler.getInventory())
                && gesperrt(spieler, "block-inventory-move")) {
            event.setCancelled(true);
            warnen(spieler);
        }
    }

    /** Wie beimKlicken, nur fuers Ziehen ueber mehrere Slots. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimZiehen(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player spieler)
                || !gesperrt(spieler, "block-inventory-move")) {
            return;
        }
        int obereGroesse = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= obereGroesse) {
                event.setCancelled(true);
                warnen(spieler);
                return;
            }
        }
    }

    /**
     * Tueren, Falltueren und Schilder ueber den Materialnamen erkennen
     * statt jede Holzart einzeln aufzuzaehlen - vanilla benennt sie
     * durchgaengig "..._DOOR", "..._TRAPDOOR", "..._SIGN".
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void beimInteragieren(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Material typ = event.getClickedBlock().getType();
        String schluessel;
        if (typ.name().endsWith("_DOOR")) {
            schluessel = "block-doors";
        } else if (typ.name().endsWith("_TRAPDOOR")) {
            schluessel = "block-trapdoors";
        } else if (typ.name().endsWith("SIGN")) {
            schluessel = "block-signs";
        } else {
            return;
        }
        if (gesperrt(event.getPlayer(), schluessel)) {
            event.setCancelled(true);
            warnen(event.getPlayer());
        }
    }

    /** Kein Grund, alte Cooldowns fuer laengst abgemeldete Spieler zu behalten. */
    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        letzteWarnung.remove(event.getPlayer().getUniqueId());
    }
}
