package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.lemonpvp.lemoncosmetics.model.TagType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TagsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Key FONT_TAGS = Key.key("lemonpvp", "tags");
    private static final Key FONT_CAPS = Key.key("lemonpvp", "default");

    // 4-row chest (36 slots): tags in rows 1-2, back button row 3
    private static final int[] TAG_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 28, 29, 30};
    private static final int BACK_SLOT = 31;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    public TagsGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 36,
                MM.deserialize("<gradient:#fffb00:#00ff00>Tags</gradient>"));

        ItemStack filler = filler();
        for (int i = 0; i < 36; i++) inventory.setItem(i, filler);

        renderTags();
        inventory.setItem(BACK_SLOT, backButton());

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.openInventory(inventory);
    }

    private void renderTags() {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        TagType[] tags = TagType.values();
        for (int i = 0; i < tags.length && i < TAG_SLOTS.length; i++) {
            inventory.setItem(TAG_SLOTS[i], buildTagItem(tags[i], cosmetics));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        event.setCancelled(true);

        int slot = event.getSlot();
        if (slot == BACK_SLOT) {
            unregister();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }

        for (int i = 0; i < TAG_SLOTS.length; i++) {
            if (slot == TAG_SLOTS[i]) {
                handleTagClick(clicker, TagType.values()[i]);
                return;
            }
        }
    }

    private void handleTagClick(Player clicker, TagType tag) {
        boolean hasPermission = clicker.hasPermission(tag.permission());
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        boolean ownedInDb = cosmetics != null && cosmetics.ownsTag(tag.id);
        boolean owned = hasPermission || ownedInDb;

        if (tag.isPermissionOnly() && !hasPermission) {
            clicker.sendMessage(MM.deserialize("<red>This tag requires a special permission."));
            return;
        }

        if (!owned) {
            plugin.getTagManager().buyTag(clicker.getUniqueId(), tag.id)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            clicker.sendMessage(MM.deserialize("<green>Purchased tag <yellow>"
                                    + tag.displayName + "</yellow>!"));
                            renderTags();
                        } else {
                            clicker.sendMessage(MM.deserialize("<red>You cannot afford this tag. It costs <gold>"
                                    + tag.price + " coins</gold>."));
                        }
                    }));
            return;
        }

        String equippedId = cosmetics != null ? cosmetics.getEquippedTagId() : null;
        if (tag.id.equals(equippedId)) {
            plugin.getTagManager().unequipTag(clicker.getUniqueId())
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        clicker.sendMessage(MM.deserialize("<yellow>Tag unequipped."));
                        renderTags();
                    }));
        } else {
            plugin.getTagManager().equipTag(clicker.getUniqueId(), tag.id)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        clicker.sendMessage(MM.deserialize("<green>Equipped tag <yellow>"
                                + tag.displayName + "</yellow>!"));
                        renderTags();
                    }));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        if (!event.getInventory().equals(inventory)) return;
        unregister();
    }

    private void unregister() {
        if (registered) { HandlerList.unregisterAll(this); registered = false; }
    }

    private ItemStack buildTagItem(TagType tag, PlayerCosmetics cosmetics) {
        boolean hasPermission = player.hasPermission(tag.permission());
        boolean ownedInDb = cosmetics != null && cosmetics.ownsTag(tag.id);
        boolean owned = hasPermission || ownedInDb;
        boolean equipped = owned && tag.id.equals(cosmetics != null ? cosmetics.getEquippedTagId() : null);

        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(buildTagNameComponent(tag));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (tag.isPermissionOnly()) {
            if (owned) {
                if (equipped) {
                    lore.add(MM.deserialize("<bold><green>✔ ᴇQᴜɪᴘᴘᴇᴅ</green></bold>"));
                    lore.add(MM.deserialize("<gray>Click to unequip"));
                } else {
                    lore.add(MM.deserialize("<green>ᴏᴡɴᴇᴅ</green>"));
                    lore.add(MM.deserialize("<gray>Click to equip"));
                }
            } else {
                lore.add(MM.deserialize("<gray>ᴘᴇʀᴍɪssɪᴏɴ ᴏɴʟʏ</gray>"));
            }
        } else {
            if (owned) {
                if (equipped) {
                    lore.add(MM.deserialize("<bold><green>✔ ᴇQᴜɪᴘᴘᴇᴅ</green></bold>"));
                    lore.add(MM.deserialize("<gray>Click to unequip"));
                } else {
                    lore.add(MM.deserialize("<green>ᴏᴡɴᴇᴅ</green>"));
                    lore.add(MM.deserialize("<gray>Click to equip"));
                }
            } else {
                lore.add(MM.deserialize("<gold>ᴘʀɪᴄᴇ: <yellow>" + tag.price + " ᴄᴏɪɴs</yellow></gold>"));
                lore.add(MM.deserialize("<red>ʟᴏᴄᴋᴇᴅ</red>"));
                lore.add(MM.deserialize("<gray>Click to purchase"));
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component buildTagNameComponent(TagType tag) {
        Component emoji = Component.text(String.valueOf(tag.emoji))
                .font(FONT_TAGS)
                .color(TextColor.color(tag.colorStart));

        String txt = tag.displayName;
        int n = txt.length();
        Component textPart = Component.empty();
        int r1 = (tag.colorStart >> 16) & 0xFF, g1 = (tag.colorStart >> 8) & 0xFF, b1 = tag.colorStart & 0xFF;
        int r2 = (tag.colorEnd >> 16) & 0xFF, g2 = (tag.colorEnd >> 8) & 0xFF, b2 = tag.colorEnd & 0xFF;
        for (int i = 0; i < n; i++) {
            float t = n > 1 ? (float) i / (n - 1) : 0f;
            int r = Math.round(r1 + (r2 - r1) * t);
            int g = Math.round(g1 + (g2 - g1) * t);
            int b = Math.round(b1 + (b2 - b1) * t);
            textPart = textPart.append(
                    Component.text(String.valueOf(txt.charAt(i)))
                            .font(FONT_CAPS)
                            .color(TextColor.color(r, g, b))
            );
        }
        return emoji.append(Component.text(" ")).append(textPart);
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(MM.deserialize("<gray>Back")); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); pane.setItemMeta(meta); }
        return pane;
    }
}
