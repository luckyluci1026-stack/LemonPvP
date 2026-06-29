package com.lemonpvp.lemonlobby.gui;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.managers.DailyRewardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 7-day daily-reward calendar. Past days show as claimed, today's reward is
 * clickable (when available), future days are locked.
 */
public class DailyRewardGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int[] DAY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int CLOSE_SLOT = 22;

    private final LemonLobby plugin;
    private final Player player;
    private Inventory inv;
    private boolean registered;

    public DailyRewardGUI(LemonLobby plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inv = Bukkit.createInventory(null, 27,
                MM.deserialize("<!italic><gradient:#fffb00:#ff8f00><bold>Dᴀɪʟʏ Rᴇᴡᴀʀᴅs</bold></gradient>"));
        ItemStack filler = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        render();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inv);
    }

    private void render() {
        DailyRewardManager mgr = plugin.getDailyRewardManager();
        int activeDay   = mgr.cycleDay(mgr.effectiveStreak(player.getUniqueId()));
        boolean claimable = mgr.canClaim(player.getUniqueId());

        for (int i = 0; i < DAY_SLOTS.length; i++) {
            int day = i + 1;
            State state;
            if (claimable) {
                state = day < activeDay ? State.CLAIMED : day == activeDay ? State.AVAILABLE : State.LOCKED;
            } else {
                state = day <= activeDay ? State.CLAIMED : State.LOCKED;
            }
            inv.setItem(DAY_SLOTS[i], buildDayItem(day, state, mgr));
        }
        inv.setItem(CLOSE_SLOT, named(Material.BARRIER, "<red>Close"));
    }

    private enum State { CLAIMED, AVAILABLE, LOCKED }

    private ItemStack buildDayItem(int day, State state, DailyRewardManager mgr) {
        int apples = mgr.appleRewardForDay(day);
        boolean booster = mgr.boosterTierForDay(day) != null;

        Material mat = switch (state) {
            case CLAIMED   -> Material.LIME_STAINED_GLASS_PANE;
            case AVAILABLE -> booster ? Material.NETHER_STAR : Material.GOLD_INGOT;
            case LOCKED    -> Material.GRAY_STAINED_GLASS_PANE;
        };
        String nameColor = switch (state) {
            case CLAIMED   -> "<green>";
            case AVAILABLE -> "<gradient:#fffb00:#ff8f00>";
            case LOCKED    -> "<dark_gray>";
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MM.deserialize("<!italic><bold>" + nameColor + "Day " + day
                + (booster ? " ★" : "")));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Reward: <green>" + apples + " ✿ apples"));
        if (booster) lore.add(MM.deserialize("<!italic><gray>+ <gold>a Booster!"));
        lore.add(Component.empty());
        lore.add(switch (state) {
            case CLAIMED   -> MM.deserialize("<!italic><green>✔ Claimed");
            case AVAILABLE -> MM.deserialize("<!italic><yellow>► Click to claim!");
            case LOCKED    -> MM.deserialize("<!italic><dark_gray>🔒 Locked");
        });
        meta.lore(lore);
        if (state == State.AVAILABLE) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;
        e.setCancelled(true);

        if (e.getRawSlot() == CLOSE_SLOT) { player.closeInventory(); return; }

        boolean isDaySlot = false;
        for (int s : DAY_SLOTS) if (s == e.getRawSlot()) { isDaySlot = true; break; }
        if (!isDaySlot) return;

        DailyRewardManager mgr = plugin.getDailyRewardManager();
        if (!mgr.canClaim(player.getUniqueId())) {
            player.sendActionBar(MM.deserialize("<!italic><red>Already claimed — come back tomorrow!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        int day = mgr.claim(player);
        if (day < 0) return;

        int apples = mgr.appleRewardForDay(day);
        boolean booster = mgr.boosterTierForDay(day) != null;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        player.showTitle(Title.title(
                MM.deserialize("<!italic><gradient:#fffb00:#ff8f00><bold>Daily Reward!"),
                MM.deserialize("<!italic><green>+" + apples + " ✿" + (booster ? " <gold>+ Booster!" : "")
                        + " <gray>(Day " + day + ")"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))));
        render();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack named(Material mat, String mini) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<!italic>" + mini)); item.setItemMeta(meta); }
        return item;
    }
}
