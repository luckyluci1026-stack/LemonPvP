package com.lemonpvp.lemoncore.gui;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.config.ConfigManager.OffendReason;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Staff reason-picker for {@code /offend <player>}: one tile per configured
 * offense category (each with its own ban duration) — click to ban the target
 * with that reason. Opened by {@link com.lemonpvp.lemoncore.commands.admin.OffendCommand}.
 */
public class OffendGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int SIZE = 36;
    private static final int HEAD_SLOT = 4;
    private static final int[] REASON_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final LemonCore plugin;
    private final Player staff;
    private final UUID targetUuid;
    private final String targetName;
    private final List<OffendReason> reasons;
    private final Map<Integer, OffendReason> slotMap = new HashMap<>();

    private Inventory inv;
    private boolean registered;
    private boolean busy; // one ban per opening — ignore further clicks

    public OffendGUI(LemonCore plugin, Player staff, UUID targetUuid, String targetName) {
        this.plugin = plugin;
        this.staff = staff;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reasons = plugin.getConfigManager().getOffendReasons();
    }

    public void open() {
        inv = Bukkit.createInventory(null, SIZE, MM.deserialize(
                "<!italic><gradient:#ff5555:#ffaa00><bold>Offend</bold></gradient> <dark_gray>» <white>" + targetName));

        ItemStack border = pane();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, border);
        inv.setItem(HEAD_SLOT, buildHead());

        for (int i = 0; i < reasons.size() && i < REASON_SLOTS.length; i++) {
            OffendReason r = reasons.get(i);
            inv.setItem(REASON_SLOTS[i], buildReasonItem(r));
            slotMap.put(REASON_SLOTS[i], r);
        }

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        staff.playSound(staff.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
        staff.openInventory(inv);
    }

    private ItemStack buildHead() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid)); } catch (Exception ignored) {}
            meta.displayName(MM.deserialize("<!italic><red><bold>" + targetName));
            meta.lore(List.of(
                    MM.deserialize("<!italic><gray>Pick the offense below to ban this player."),
                    MM.deserialize("<!italic><dark_gray>Close the menu to cancel.")));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack buildReasonItem(OffendReason r) {
        Material mat = Material.matchMaterial(r.icon());
        if (mat == null) mat = Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><yellow><bold>" + r.display()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><gray>Duration: <white>" + formatDuration(r.durationSeconds())));
            lore.add(Component.empty());
            lore.add(MM.deserialize("<!italic><red>► Click to ban <white>" + targetName));
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** 2592000 → "30d", 90000 → "1d 1h", 0 → "permanent". */
    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "permanent";
        long d = seconds / 86_400, h = (seconds % 86_400) / 3_600, m = (seconds % 3_600) / 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0 && d == 0) sb.append(m).append("m ");
        if (sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(staff.getUniqueId())) return;
        e.setCancelled(true);

        OffendReason reason = slotMap.get(e.getRawSlot());
        if (reason == null || busy) return;
        busy = true;

        UUID staffUuid = p.getUniqueId();
        plugin.getBanManager()
                .banPlayer(targetUuid, targetName, reason.display(), staffUuid, p.getName(), reason.durationSeconds())
                .thenAccept(ban -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player s = Bukkit.getPlayer(staffUuid);
                    if (ban == null) {
                        busy = false;
                        if (s != null) s.sendMessage(MM.deserialize(
                                "<red>Ban failed for <yellow>" + targetName + " <red>— a database error occurred."));
                        return;
                    }
                    if (s != null) {
                        s.closeInventory();
                        s.sendMessage(plugin.getMessagesManager().get("ban.success",
                                "player", targetName, "reason", reason.display()));
                        s.playSound(s.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.3f);
                    }
                    Player t = Bukkit.getPlayer(targetUuid);
                    if (t != null) plugin.getListenerManager().performBanKick(t, ban);
                }));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (inv == null || !e.getInventory().equals(inv)) return;
        if (!e.getPlayer().getUniqueId().equals(staff.getUniqueId())) return;
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }
}
