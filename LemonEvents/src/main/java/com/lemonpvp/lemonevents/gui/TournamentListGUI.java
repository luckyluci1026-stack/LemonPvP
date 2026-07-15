package com.lemonpvp.lemonevents.gui;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.Tournament;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Owner sub-menu listing every tournament with a single click to advance its
 * stage: open a SIGNUP tournament's qualification, or close a QUALIFICATION to
 * finals. FINALS/ENDED show finalists / champion. Crowning the champion stays on
 * the command ({@code /tournament champion}) since it needs a player pick.
 */
public class TournamentListGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int BACK_SLOT = 49;

    private final LemonEvents plugin;
    private final Player owner;
    private final NamespacedKey idKey;
    private Inventory inv;
    private boolean registered;

    public TournamentListGUI(LemonEvents plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
        this.idKey = new NamespacedKey(plugin, "tournament_id");
    }

    public void open() {
        inv = Bukkit.createInventory(null, 54, MM.deserialize(
                "<!italic><gradient:#fffb00:#ffa751><bold>Tournaments</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        owner.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), -1);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);

        List<Tournament> list = new ArrayList<>(plugin.getTournamentManager().all());
        list.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        int slot = 0;
        for (Tournament t : list) {
            if (slot >= 45) break;
            inv.setItem(slot++, tournamentItem(t));
        }

        inv.setItem(BACK_SLOT, named(Material.ARROW, "<gray>← Back", List.of(), -1));
    }

    private ItemStack tournamentItem(Tournament t) {
        Material mat = switch (t.getState()) {
            case SIGNUP -> Material.WRITABLE_BOOK;
            case QUALIFICATION -> Material.DIAMOND_SWORD;
            case FINALS -> Material.GOLDEN_APPLE;
            case ENDED -> Material.GOLD_BLOCK;
        };
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Gamemode: <white>" + t.getGamemode());
        lore.add("<gray>State: <yellow>" + t.getState());
        if (t.getState() == Tournament.State.QUALIFICATION && t.getEndAt() > 0) {
            long hrs = Math.max(0, (t.getEndAt() - System.currentTimeMillis()) / 3_600_000L);
            lore.add("<gray>Ends in: <white>~" + hrs + "h");
        }
        if (t.getChampion() != null) lore.add("<gray>Champion: <gold>" + nameOf(t.getChampion()));
        lore.add("");
        switch (t.getState()) {
            case SIGNUP -> lore.add("<green>► Click: open qualification ("
                    + plugin.getTournamentManager().defaultDays() + "d)");
            case QUALIFICATION -> lore.add("<green>► Click: close & lock finalists");
            case FINALS -> lore.add("<dark_gray>Crown via /tournament champion " + t.getId() + " <player>");
            case ENDED -> lore.add("<dark_gray>Finished");
        }
        return named(mat, "<white><bold>#" + t.getId() + " " + t.getName(), lore, t.getId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(owner.getUniqueId())) return;
        e.setCancelled(true);

        if (e.getRawSlot() == BACK_SLOT) { unregister(); new OwnerPanelGUI(plugin, p).open(); return; }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        Integer id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.INTEGER);
        if (id == null) return;

        Tournament t = plugin.getTournamentManager().get(id);
        if (t == null) return;
        switch (t.getState()) {
            case SIGNUP -> {
                if (plugin.getTournamentManager().openQualification(id, plugin.getTournamentManager().defaultDays()))
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
            }
            case QUALIFICATION -> {
                plugin.getTournamentManager().closeToFinals(id, plugin.getTournamentManager().finalsSize());
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
            }
            default -> { /* nothing to advance */ }
        }
        render();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(owner.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }

    private String nameOf(java.util.UUID id) {
        var p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }

    private ItemStack named(Material mat, String name, List<String> lore, int tournamentId) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + name));
            if (!lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            if (tournamentId >= 0) meta.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, tournamentId);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
}
