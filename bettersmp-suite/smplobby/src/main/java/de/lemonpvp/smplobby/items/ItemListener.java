package de.lemonpvp.smplobby.items;

import de.lemonpvp.smplobby.SMPLobby;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/**
 * Was passiert, wenn jemand auf einen Lobby-Gegenstand klickt.
 *
 * Es reicht nicht, den Rechtsklick abzufangen: Wer den Kompass ins
 * Inventar zieht oder in die zweite Hand nimmt, haette ihn sonst
 * irgendwann nicht mehr in der Schnellleiste - und damit auch keinen
 * Weg mehr auf einen anderen Server.
 */
public final class ItemListener implements Listener {

    private final SMPLobby plugin;

    public ItemListener(SMPLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void beimKlick(PlayerInteractEvent ereignis) {
        // Nur die Haupthand, sonst laeuft alles doppelt: Minecraft meldet
        // den Klick fuer beide Haende.
        if (ereignis.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action was = ereignis.getAction();
        if (was != Action.RIGHT_CLICK_AIR && was != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        String aktion = plugin.items().aktionVon(ereignis.getItem());
        if (aktion == null) {
            return;
        }
        ereignis.setCancelled(true);
        Player spieler = ereignis.getPlayer();

        switch (aktion) {
            case LobbyItems.AKTION_WAEHLER -> plugin.waehler().oeffne(spieler);
            case LobbyItems.AKTION_VERSTECKEN -> {
                if (plugin.hide().aktiv()) {
                    plugin.hide().schalte(spieler);
                }
            }
            case LobbyItems.AKTION_REGELN -> {
                List<String> regeln = plugin.msgs().liste("regeln");
                for (String zeile : regeln) {
                    spieler.sendMessage(de.lemonpvp.smplobby.util.Text.mm(zeile));
                }
            }
            default -> {
                // "keine" oder etwas Unbekanntes: nichts tun, aber den
                // Klick trotzdem geschluckt haben.
            }
        }
    }

    /** Die Lobby-Gegenstaende bleiben, wo sie sind. */
    @EventHandler
    public void beimVerschieben(InventoryClickEvent ereignis) {
        if (!(ereignis.getWhoClicked() instanceof Player spieler)) {
            return;
        }
        if (spieler.hasPermission("smplobby.bauen")) {
            return;
        }
        if (plugin.items().aktionVon(ereignis.getCurrentItem()) != null
                || plugin.items().aktionVon(ereignis.getCursor()) != null) {
            ereignis.setCancelled(true);
        }
    }

    @EventHandler
    public void beimHandwechsel(PlayerSwapHandItemsEvent ereignis) {
        if (ereignis.getPlayer().hasPermission("smplobby.bauen")) {
            return;
        }
        if (plugin.items().aktionVon(ereignis.getOffHandItem()) != null
                || plugin.items().aktionVon(ereignis.getMainHandItem()) != null) {
            ereignis.setCancelled(true);
        }
    }
}
