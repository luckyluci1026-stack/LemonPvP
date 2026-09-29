package de.lemonpvp.fastshop.auktion;

import de.lemonpvp.fastshop.FastShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuktionsListener implements Listener {

    private final FastShop plugin;

    public AuktionsListener(FastShop plugin) {
        this.plugin = plugin;
    }

    private AuktionsHaus haus() {
        return plugin.auktionen();
    }

    private AuktionsMenus menus() {
        return plugin.auktionsMenus();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimKlick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AuktionsMenus.Menue menue)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player spieler) || event.getClickedInventory() == null) {
            return;
        }
        if (menue instanceof AuktionsMenus.Verkaufen verkaufen) {
            verkaufenKlick(spieler, verkaufen, event.getClickedInventory(), event.getInventory(), event.getSlot());
            return;
        }
        if (!event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (menue instanceof AuktionsMenus.Uebersicht uebersicht) {
            uebersichtKlick(spieler, uebersicht, slot);
        } else if (menue instanceof AuktionsMenus.Eigene eigene) {
            eigeneKlick(spieler, eigene, slot);
        } else if (menue instanceof AuktionsMenus.Bestaetigung bestaetigung) {
            bestaetigungKlick(spieler, bestaetigung, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void beimZiehen(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AuktionsMenus.Menue) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (spieler.isOnline()) {
                haus().beimJoin(spieler);
            }
        }, 40L);
    }

    private void uebersichtKlick(Player spieler, AuktionsMenus.Uebersicht menue, int slot) {
        AuktionsMenus.Ansicht ansicht = menue.ansicht();
        if (slot < AuktionsMenus.PRO_SEITE) {
            UUID id = menue.angebotAuf(slot);
            if (id == null) {
                return;
            }
            Angebot angebot = haus().angebot(id);
            if (angebot == null || !angebot.kaufbar(System.currentTimeMillis())) {
                haus().senden(spieler, "ah-gone");
                ton(spieler, false);
                menus().uebersichtOeffnen(spieler, ansicht);
                return;
            }
            if (angebot.verkaeufer().equals(spieler.getUniqueId())) {
                haus().senden(spieler, "ah-own");
                ton(spieler, false);
                return;
            }
            menus().bestaetigungOeffnen(spieler, angebot, ansicht);
            return;
        }
        switch (slot) {
            case AuktionsMenus.ZURUECK -> menus().uebersichtOeffnen(spieler, ansicht.mitSeite(ansicht.seite() - 1));
            case AuktionsMenus.WEITER -> menus().uebersichtOeffnen(spieler, ansicht.mitSeite(ansicht.seite() + 1));
            case AuktionsMenus.SORTIERUNG -> menus().uebersichtOeffnen(spieler, new AuktionsMenus.Ansicht(0,
                    ansicht.sortierung().naechste(), ansicht.filter(), ansicht.suche()));
            case AuktionsMenus.FILTER -> menus().uebersichtOeffnen(spieler, new AuktionsMenus.Ansicht(0,
                    ansicht.sortierung(), ansicht.filter().naechster(), ansicht.suche()));
            case AuktionsMenus.SUCHE -> plugin.auktionsDialoge().suche(spieler, ansicht);
            case AuktionsMenus.GUTHABEN -> menus().uebersichtOeffnen(spieler, ansicht);
            case AuktionsMenus.EIGENE -> menus().eigeneOeffnen(spieler, ansicht);
            case AuktionsMenus.VERKAUFEN -> menus().verkaufenOeffnen(spieler, ansicht,
                    spieler.getInventory().getHeldItemSlot(), 0, false);
            case AuktionsMenus.SCHLIESSEN -> spieler.closeInventory();
            default -> {
            }
        }
    }

    private void verkaufenKlick(Player spieler, AuktionsMenus.Verkaufen menue, Inventory geklickt, Inventory oben, int slot) {
        if (geklickt.equals(spieler.getInventory())) {
            if (slot >= 0 && slot <= AuktionsHaus.LETZTER_INVENTAR_PLATZ) {
                menus().verkaufenWaehlen(spieler, menue, slot);
                klick(spieler);
            }
            return;
        }
        if (!geklickt.equals(oben)) {
            return;
        }
        double schritt = AuktionsMenus.schrittAuf(slot);
        if (schritt != 0) {
            menus().verkaufenPreisAendern(spieler, menue, menue.preis() + schritt);
            klick(spieler);
            return;
        }
        switch (slot) {
            case AuktionsMenus.VERKAUF_ZURUECK -> menus().uebersichtOeffnen(spieler, menue.ansicht());
            case AuktionsMenus.VERKAUF_EINTIPPEN -> plugin.auktionsDialoge().preis(spieler, menue);
            case AuktionsMenus.VERKAUF_ANBIETEN -> anbieten(spieler, menue);
            case AuktionsMenus.VERKAUF_SCHLIESSEN -> spieler.closeInventory();
            default -> {
            }
        }
    }

    private void anbieten(Player spieler, AuktionsMenus.Verkaufen menue) {
        ItemStack vorschau = menue.vorschau();
        if (vorschau == null) {
            haus().senden(spieler, "ah-select-item");
            ton(spieler, false);
            return;
        }
        switch (haus().anbieten(spieler, menue.slot(), vorschau, menue.preis())) {
            case ANGEBOTEN -> {
                haus().senden(spieler, "ah-listed", "amount", String.valueOf(vorschau.getAmount()),
                        "item", AuktionsHaus.itemText(vorschau), "price", plugin.economy().format(menue.preis()),
                        "hours", String.valueOf(haus().dauerStunden()));
                ton(spieler, true);
                menus().eigeneOeffnen(spieler, menue.ansicht());
                return;
            }
            case VERAENDERT -> haus().senden(spieler, "ah-changed");
            case NICHTS_IN_DER_HAND -> haus().senden(spieler, "ah-select-item");
            case PREIS_AUSSERHALB -> haus().senden(spieler, "ah-price-range",
                    "min", plugin.economy().format(haus().minPreis()), "max", plugin.economy().format(haus().maxPreis()));
            case ZU_VIELE -> haus().senden(spieler, "ah-limit", "max", String.valueOf(haus().maxAngebote()));
            case KEINE_WIRTSCHAFT -> haus().senden(spieler, "ah-no-economy");
        }
        ton(spieler, false);
        menus().verkaufenWaehlen(spieler, menue, menue.slot());
    }

    private void klick(Player spieler) {
        if (plugin.getConfig().getBoolean("settings.sounds", true)) {
            spieler.playSound(net.kyori.adventure.sound.Sound.sound(net.kyori.adventure.key.Key.key("minecraft:ui.button.click"),
                    net.kyori.adventure.sound.Sound.Source.MASTER, 0.5f, 1.4f));
        }
    }

    private void eigeneKlick(Player spieler, AuktionsMenus.Eigene menue, int slot) {
        if (slot == AuktionsMenus.EIGENE_ZURUECK) {
            menus().uebersichtOeffnen(spieler, menue.ansicht());
            return;
        }
        UUID id = menue.angebotAuf(slot);
        if (id == null) {
            return;
        }
        Angebot angebot = haus().angebot(id);
        switch (haus().zuruecknehmen(spieler, id)) {
            case ZURUECK -> {
                haus().senden(spieler, "ah-cancelled", "amount", String.valueOf(angebot.menge()),
                        "item", AuktionsHaus.itemText(angebot.item()));
                ton(spieler, true);
            }
            case KEIN_PLATZ -> {
                haus().senden(spieler, "ah-inventory-full");
                ton(spieler, false);
            }
            case NICHT_MEHR_DA -> {
                haus().senden(spieler, "ah-gone");
                ton(spieler, false);
            }
        }
        menus().eigeneOeffnen(spieler, menue.ansicht());
    }

    private void bestaetigungKlick(Player spieler, AuktionsMenus.Bestaetigung menue, int slot) {
        if (AuktionsMenus.istAbbrechen(slot)) {
            menus().uebersichtOeffnen(spieler, menue.ansicht());
            return;
        }
        if (!AuktionsMenus.istKaufen(slot)) {
            return;
        }
        Angebot angebot = haus().angebot(menue.angebot());
        AuktionsHaus.KaufErgebnis ergebnis = haus().kaufen(spieler, menue.angebot());
        switch (ergebnis) {
            case GEKAUFT -> {
                haus().senden(spieler, "ah-bought", "amount", String.valueOf(angebot.menge()),
                        "item", AuktionsHaus.itemText(angebot.item()), "seller", angebot.verkaeuferName(),
                        "price", plugin.economy().format(angebot.preis()));
                ton(spieler, true);
            }
            case ZU_WENIG_GELD -> {
                haus().senden(spieler, "ah-no-money", "price", plugin.economy().format(angebot.preis()),
                        "balance", plugin.economy().format(plugin.economy().balance(spieler)));
                ton(spieler, false);
            }
            case KEIN_PLATZ -> {
                haus().senden(spieler, "ah-inventory-full");
                ton(spieler, false);
            }
            case EIGENES -> {
                haus().senden(spieler, "ah-own");
                ton(spieler, false);
            }
            case KEINE_WIRTSCHAFT -> {
                haus().senden(spieler, "ah-no-economy");
                ton(spieler, false);
            }
            case NICHT_MEHR_DA -> {
                haus().senden(spieler, "ah-gone");
                ton(spieler, false);
            }
        }
        menus().uebersichtOeffnen(spieler, menue.ansicht());
    }

    private void ton(Player spieler, boolean erfolg) {
        if (!plugin.getConfig().getBoolean("settings.sounds", true)) {
            return;
        }
        String schluessel = erfolg ? "minecraft:entity.experience_orb.pickup" : "minecraft:entity.villager.no";
        spieler.playSound(net.kyori.adventure.sound.Sound.sound(net.kyori.adventure.key.Key.key(schluessel),
                net.kyori.adventure.sound.Sound.Source.MASTER, 0.7f, erfolg ? 1.2f : 1.0f));
    }
}
