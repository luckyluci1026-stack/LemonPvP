package com.lemonpvp.lemonpractice.gui;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.tournament.Tournament;
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
 * The click-driven face of {@code /tournament}: every tournament as an item.
 * Left-click signs up, right-click prints the standings (or finalists once the
 * finals are locked). Admins additionally shift-left-click a SIGNUP tournament
 * to open its qualification and shift-right-click a QUALIFICATION to close it
 * to finals — the create/champion flows stay on the command.
 */
public class TournamentGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String ADMIN = "lemonpractice.admin.tournament";
    private static final int CLOSE_SLOT = 49;

    private final LemonPractice plugin;
    private final Player viewer;
    private final NamespacedKey idKey;
    private Inventory inv;
    private boolean registered;

    public TournamentGUI(LemonPractice plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.idKey = new NamespacedKey(plugin, "tournament_id");
    }

    public void open() {
        inv = Bukkit.createInventory(null, 54, MM.deserialize(
                "<!italic><gradient:#fffb00:#ffa751><bold>Tournaments</bold></gradient>"));
        render();
        if (!registered) { Bukkit.getPluginManager().registerEvents(this, plugin); registered = true; }
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        viewer.openInventory(inv);
    }

    private void render() {
        ItemStack pane = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), -1);
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close", List.of(), -1));

        List<Tournament> list = new ArrayList<>(plugin.getTournamentManager().all());
        list.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        if (list.isEmpty()) {
            inv.setItem(22, named(Material.GRAY_DYE, "<gray><bold>No Tournaments", List.of(
                    "<dark_gray>Nothing planned right now — check back!"), -1));
            return;
        }
        int slot = 0;
        for (Tournament t : list) {
            if (slot >= 45) break;
            inv.setItem(slot++, tournamentItem(t));
        }
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
        if (t.getState() == Tournament.State.QUALIFICATION) {
            long hrs = Math.max(0, t.qualificationRemaining(System.currentTimeMillis()) / 3_600_000L);
            lore.add("<gray>Qualification ends in: <white>~" + hrs + "h");
        }
        if (t.getChampion() != null) lore.add("<gray>Champion: <gold>" + nameOf(t.getChampion()));
        lore.add("");
        switch (t.getState()) {
            case SIGNUP, QUALIFICATION -> {
                lore.add("<green>► Left-click: sign up");
                lore.add("<aqua>► Right-click: standings");
            }
            case FINALS -> lore.add("<aqua>► Right-click: finalists");
            case ENDED -> lore.add("<aqua>► Right-click: final standings");
        }
        if (viewer.hasPermission(ADMIN)) {
            if (t.getState() == Tournament.State.SIGNUP)
                lore.add("<gold>► Shift-left: open qualification ("
                        + plugin.getTournamentManager().defaultDays() + "d)");
            if (t.getState() == Tournament.State.QUALIFICATION)
                lore.add("<gold>► Shift-right: close & lock finalists");
        }
        return named(mat, "<white><bold>#" + t.getId() + " " + t.getName(), lore, t.getId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(viewer.getUniqueId())) return;
        e.setCancelled(true);

        if (e.getRawSlot() == CLOSE_SLOT) { p.closeInventory(); return; }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        Integer id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.INTEGER);
        if (id == null) return;
        Tournament t = plugin.getTournamentManager().get(id);
        if (t == null) return;

        // Admin stage advances on deliberate shift-clicks.
        if (p.hasPermission(ADMIN) && e.isShiftClick()) {
            if (e.isLeftClick() && t.getState() == Tournament.State.SIGNUP) {
                if (plugin.getTournamentManager().openQualification(id, plugin.getTournamentManager().defaultDays()))
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
                render();
            } else if (e.isRightClick() && t.getState() == Tournament.State.QUALIFICATION) {
                plugin.getTournamentManager().closeToFinals(id, plugin.getTournamentManager().finalsSize());
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
                render();
            }
            return;
        }

        if (e.isRightClick()) {
            showStandings(p, t);
        } else if (t.getState() == Tournament.State.SIGNUP || t.getState() == Tournament.State.QUALIFICATION) {
            plugin.getTournamentManager().signup(p.getUniqueId(), id).thenAccept(ok ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (Boolean.TRUE.equals(ok)) {
                            msg(p, "<green>Signed up for <white>" + t.getName()
                                    + "<green>! Every ranked <white>" + t.getGamemode() + " <green>win counts.");
                            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
                        } else {
                            msg(p, "<gray>You're already signed up.");
                        }
                    }));
        }
    }

    /** Prints standings (or finalists once locked) to chat, keeping the GUI open. */
    private void showStandings(Player p, Tournament t) {
        boolean finals = t.getState() == Tournament.State.FINALS || t.getState() == Tournament.State.ENDED;
        var future = finals ? plugin.getTournamentManager().finalists(t.getId())
                            : plugin.getTournamentManager().standings(t.getId());
        future.thenAccept(rows -> Bukkit.getScheduler().runTask(plugin, () -> {
            msg(p, "<gradient:#fffb00:#00ff00><bold>" + t.getName() + "</bold></gradient> <dark_gray>» "
                    + (finals ? "finalists" : "standings"));
            if (rows == null || rows.isEmpty()) { msg(p, "<gray>Nothing recorded yet."); return; }
            int rank = 1;
            for (var s : rows) {
                if (rank > 15) break;
                msg(p, "<gray>#" + (rank++) + " <white>" + nameOf(s.uuid()) + " <dark_gray>(" + s.wins() + " wins)");
            }
        }));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private String nameOf(UUID id) {
        var p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }

    private void msg(Player p, String mini) { p.sendMessage(MM.deserialize("<!italic>" + mini)); }

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
