package de.lemonpvp.smplobby.selector;

import de.lemonpvp.smplobby.SMPLobby;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Klicks in der Serverauswahl. */
public final class SelectorListener implements Listener {

    private final SMPLobby plugin;

    public SelectorListener(SMPLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void beimKlick(InventoryClickEvent ereignis) {
        if (!(ereignis.getInventory().getHolder() instanceof SelectorGui.Holder holder)) {
            return;
        }
        // Immer abbrechen, auch beim Klick ins eigene Inventar: Sonst
        // koennte man Gegenstaende aus der Schnellleiste in das Fenster
        // schieben und sie waeren weg, sobald es zugeht.
        ereignis.setCancelled(true);
        if (!(ereignis.getWhoClicked() instanceof Player spieler)) {
            return;
        }
        if (!holder.getInventory().equals(ereignis.getClickedInventory())) {
            return;
        }
        String server = holder.ziele.get(ereignis.getSlot());
        if (server == null) {
            return;
        }
        spieler.closeInventory();

        if (server.equalsIgnoreCase(plugin.proxy().eigenerName())) {
            spieler.sendMessage(plugin.msgs().format("waehler-schon-da"));
            return;
        }
        if (!plugin.proxy().verbinde(spieler, server)) {
            spieler.sendMessage(plugin.msgs().format("waehler-kein-proxy"));
            return;
        }
        spieler.sendMessage(plugin.msgs().format("waehler-verbinde", "server", server));
        spieler.playSound(Sound.sound(Key.key("minecraft:ui.button.click"),
                Sound.Source.MASTER, 0.7f, 1.2f));
    }
}
