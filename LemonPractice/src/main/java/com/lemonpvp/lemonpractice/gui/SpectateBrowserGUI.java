package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelGame;
import com.lemonpvp.lemonpractice.duel.DuelState;
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
import java.util.UUID;

/**
 * The match browser behind {@code /spectate}: every running duel as an item
 * (fighters, gamemode, elapsed time) — click to watch it live from the arena's
 * spectator spawn. Games are addressed by a participant UUID so a match that
 * ends between render and click simply no-ops.
 */
public class SpectateBrowserGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int CLOSE_SLOT = 49;

    private final LemonPractice plugin;
    private final Player viewer;
    private final NamespacedKey gameKey;
    private Inventory inv;
    private boolean registered;

    public SpectateBrowserGUI(LemonPractice plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.gameKey = new NamespacedKey(plugin, "spectate_target");
    }

    public void open() {
        inv = Bukkit.createInventory(null, 54, MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>Live Matches</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        viewer.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), null);
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of(), null));

        List<DuelGame> games = plugin.getDuelManager().getActiveGames().stream()
                .filter(g -> g.getState() == DuelState.FIGHTING)
                .toList();
        if (games.isEmpty()) {
            inv.setItem(22, named(Material.GRAY_DYE, "<gray><bold>No Live Matches", List.of(
                    "<dark_gray>Nobody is fighting right now — queue up!"), null));
            return;
        }
        int slot = 0;
        for (DuelGame g : games) {
            if (slot >= 45) break;
            int mins = g.getDurationSeconds() / 60, secs = g.getDurationSeconds() % 60;
            inv.setItem(slot++, named(Material.DIAMOND_SWORD,
                    "<white><bold>" + g.getPlayer1Name() + " <gray>vs <white><bold>" + g.getPlayer2Name(),
                    List.of(
                            "<gray>Gamemode: <white>" + g.getGamemode(),
                            "<gray>Duration: <white>" + String.format("%d:%02d", mins, secs),
                            "",
                            "<green>► Click to spectate"),
                    g.getPlayer1Uuid()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(viewer.getUniqueId())) return;
        e.setCancelled(true);
        if (e.getRawSlot() == CLOSE_SLOT) { p.closeInventory(); return; }

        ItemStack clicked = e.getCurrentItem();
        ItemMeta meta = clicked != null ? clicked.getItemMeta() : null;
        String raw = meta != null ? meta.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING) : null;
        if (raw == null) return;

        DuelGame game;
        try {
            game = plugin.getDuelManager().getDuel(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (game == null || game.getState() != DuelState.FIGHTING) {
            p.sendMessage(MM.deserialize("<!italic><gray>That match just ended."));
            render();
            return;
        }
        unregister();
        p.closeInventory();
        plugin.getDuelSpectateManager().spectate(p, game);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        unregister();
    }

    private void unregister() { if (registered) { HandlerList.unregisterAll(this); registered = false; } }

    private ItemStack named(Material mat, String name, List<String> lore, UUID target) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic>" + name));
            if (!lore.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : lore) l.add(MM.deserialize("<!italic>" + s));
                meta.lore(l);
            }
            if (target != null) meta.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, target.toString());
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
}
