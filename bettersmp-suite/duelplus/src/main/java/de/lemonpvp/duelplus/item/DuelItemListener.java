package de.lemonpvp.duelplus.item;

import de.lemonpvp.duelplus.DuelPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Gibt den Duell-Gegenstand beim Beitreten aus (SMP/Lobby - nicht auf
 * dem Duels-Server, dort ist DuelPlus nur fuer die Kaempfe selbst da)
 * und oeffnet bei Rechtsklick eine einfache Spieler-Auswahl.
 *
 * Der Gegenstand wird ueber eine PersistentDataContainer-Markierung
 * erkannt, nicht ueber seinen Anzeigenamen - robuster, falls der Name
 * in der config.yml mal geaendert wird oder zufaellig mit einem
 * anderen Item uebereinstimmt.
 */
public final class DuelItemListener implements Listener {

    private final DuelPlus plugin;
    private final NamespacedKey markierung;

    public DuelItemListener(DuelPlus plugin) {
        this.plugin = plugin;
        this.markierung = new NamespacedKey(plugin, "duell-gegenstand");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("gegenstand.aktiv", true)
                || !plugin.getConfig().getBoolean("gegenstand.beim-ankommen", true)
                || !event.getPlayer().hasPermission("duelplus.use")) {
            return;
        }
        int slot = plugin.getConfig().getInt("gegenstand.slot", 1);
        event.getPlayer().getInventory().setItem(slot, gegenstandBauen());
        plugin.msgs().send(event.getPlayer(), "item-received", "slot", String.valueOf(slot + 1));
    }

    private ItemStack gegenstandBauen() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gegenstand.material", "IRON_SWORD"));
        if (material == null) {
            material = Material.IRON_SWORD;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm(plugin.getConfig().getString("gegenstand.name", "Duell"))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String zeile : plugin.getConfig().getStringList("gegenstand.beschreibung")) {
            lore.add(mm(zeile).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(markierung, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean istDuellGegenstand(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(markierung, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimBenutzen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!istDuellGegenstand(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        if (!event.getPlayer().hasPermission("duelplus.use")) {
            plugin.msgs().send(event.getPlayer(), "no-permission");
            return;
        }
        auswahlOeffnen(event.getPlayer());
    }

    private void auswahlOeffnen(Player spieler) {
        List<Player> ziele = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(spieler)) {
                ziele.add(online);
            }
        }
        if (ziele.isEmpty()) {
            plugin.msgs().send(spieler, "gui-empty");
            return;
        }
        int plaetze = Math.min(54, Math.max(9, ((ziele.size() - 1) / 9 + 1) * 9));
        AuswahlHolder holder = new AuswahlHolder();
        Inventory gui = Bukkit.createInventory(holder, plaetze, mm(plugin.msgs().raw("gui-title")));
        holder.inventory = gui;
        for (Player ziel : ziele) {
            ItemStack kopf = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) kopf.getItemMeta();
            meta.setOwningPlayer(ziel);
            meta.displayName(mm("<yellow>" + ziel.getName()).decoration(TextDecoration.ITALIC, false));
            kopf.setItemMeta(meta);
            gui.addItem(kopf);
        }
        spieler.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void beimGuiKlick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AuswahlHolder)) {
            return;
        }
        event.setCancelled(true);
        ItemStack geklickt = event.getCurrentItem();
        if (geklickt == null || geklickt.getType() != Material.PLAYER_HEAD
                || !(event.getWhoClicked() instanceof Player spieler)) {
            return;
        }
        if (!(geklickt.getItemMeta() instanceof SkullMeta skull) || skull.getOwningPlayer() == null) {
            return;
        }
        spieler.closeInventory();
        plugin.anfragen().anfordern(spieler, skull.getOwningPlayer().getName());
    }

    private Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
    }

    private static final class AuswahlHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
