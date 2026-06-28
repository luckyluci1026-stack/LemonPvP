package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.lemonpvp.lemoncosmetics.model.TagType;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TagsGUI implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Interior slots used to lay out tags (rows 1–3 of a 54-slot menu). */
    private static final int[] TAG_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int UNEQUIP_SLOT = 48;
    private static final int BACK_SLOT    = 49;
    private static final int PREVIEW_SLOT = 4;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    /** Maps inventory slot -> tag shown there, for click handling. */
    private final Map<Integer, TagType> slotToTag = new HashMap<>();

    public TagsGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 54,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>ᴛᴀɢs</gradient>"));

        fillBorders();
        render();

        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    private void render() {
        slotToTag.clear();
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());

        TagType[] tags = TagType.values();
        for (int i = 0; i < tags.length && i < TAG_SLOTS.length; i++) {
            int slot = TAG_SLOTS[i];
            slotToTag.put(slot, tags[i]);
            inventory.setItem(slot, buildTagItem(tags[i], cosmetics));
        }

        inventory.setItem(PREVIEW_SLOT, buildPreviewItem(cosmetics));
        inventory.setItem(UNEQUIP_SLOT, buildUnequipItem(cosmetics));
        inventory.setItem(BACK_SLOT, buildBackButton());
    }

    private ItemStack buildPreviewItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>Preview</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        String equippedId = cosmetics != null ? cosmetics.getEquippedTagId() : null;
        TagType equipped = equippedId != null ? TagType.fromId(equippedId).orElse(null) : null;
        if (equipped != null) {
            lore.add(MM.deserialize("<!italic><gray>" + player.getName()
                    + " <reset>" + equipped.render));
        } else {
            lore.add(MM.deserialize("<!italic><gray>" + player.getName()
                    + " <dark_gray>(no tag)"));
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><dark_gray>This is how your name looks in chat."));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTagItem(TagType tag, PlayerCosmetics cosmetics) {
        boolean hasAccess = plugin.getTagManager().canUse(player, tag);
        boolean equipped = cosmetics != null && tag.id.equals(cosmetics.getEquippedTagId());

        ItemStack item = new ItemStack(hasAccess ? tag.icon : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic>" + tag.render));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Preview: <reset>" + tag.render));
        lore.add(Component.empty());

        if (equipped) {
            lore.add(MM.deserialize("<!italic><aqua>✔ Equipped"));
            lore.add(MM.deserialize("<!italic><gray>Click to remove"));
        } else if (hasAccess) {
            lore.add(MM.deserialize("<!italic><green>✔ Unlocked"));
            lore.add(MM.deserialize("<!italic><gray>Click to equip"));
        } else if (tag.isBuyable()) {
            lore.add(MM.deserialize("<!italic><gold>Price: <yellow>" + tag.price + " ⭐"));
            lore.add(MM.deserialize("<!italic><gray>Click to buy"));
        } else {
            lore.add(MM.deserialize("<!italic><red>🔒 Permission only"));
            lore.add(MM.deserialize("<!italic><dark_gray>" + tag.permission()));
        }

        meta.lore(lore);
        if (equipped) meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildUnequipItem(PlayerCosmetics cosmetics) {
        boolean hasTag = cosmetics != null && cosmetics.getEquippedTagId() != null;
        ItemStack item = new ItemStack(hasTag ? Material.BARRIER : Material.STRUCTURE_VOID);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><red>Remove tag"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(MM.deserialize(hasTag
                    ? "<!italic><gray>Click to remove your tag."
                    : "<!italic><dark_gray>You don't have a tag equipped."));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize("<!italic><gray>← Back"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorders() {
        ItemStack black = filler(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray  = filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            boolean edge = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;
            inventory.setItem(i, edge ? black : gray);
        }
    }

    private ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
        return item;
    }

    // -----------------------------------------------------------------------
    // Click handling
    // -----------------------------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.getUniqueId().equals(player.getUniqueId())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        event.setCancelled(true);

        int slot = event.getSlot();

        if (slot == BACK_SLOT) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 0.9f);
            unregister();
            new CosmeticsMainGUI(plugin, player).open();
            return;
        }

        if (slot == UNEQUIP_SLOT) {
            handleUnequip(clicker);
            return;
        }

        TagType tag = slotToTag.get(slot);
        if (tag != null) handleTagClick(clicker, tag);
    }

    private void handleUnequip(Player clicker) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        if (cosmetics == null || cosmetics.getEquippedTagId() == null) {
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        UUID uuid = clicker.getUniqueId();
        plugin.getTagManager().unequipTag(uuid)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) return;
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.9f);
                    p.sendMessage(MM.deserialize("<yellow>Your tag has been removed."));
                    render();
                }));
    }

    private void handleTagClick(Player clicker, TagType tag) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(clicker.getUniqueId());
        UUID uuid = clicker.getUniqueId();
        boolean hasAccess = plugin.getTagManager().canUse(clicker, tag);

        // Not owned and not permitted -> attempt purchase (only if buyable)
        if (!hasAccess) {
            if (!tag.isBuyable()) {
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                clicker.sendMessage(MM.deserialize("<red>This tag is only available with permission."));
                return;
            }
            plugin.getTagManager().buyTag(uuid, tag.id)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) return;
                        if (success) {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                            p.sendMessage(MM.deserialize("<green>Tag purchased: <reset>" + tag.render
                                    + " <green>for <gold>" + tag.price + " ⭐<green>!"));
                            render();
                        } else {
                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            p.sendMessage(MM.deserialize("<red>You can't afford this tag. "
                                    + "<gray>Price: <gold>" + tag.price + " ⭐"));
                        }
                    }));
            return;
        }

        // Owned/permitted: toggle equip/unequip
        boolean equipped = cosmetics != null && tag.id.equals(cosmetics.getEquippedTagId());
        if (equipped) {
            plugin.getTagManager().unequipTag(uuid)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) return;
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.9f);
                        p.sendMessage(MM.deserialize("<yellow>Your tag has been removed."));
                        render();
                    }));
        } else {
            plugin.getTagManager().equipTag(uuid, tag.id)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null) return;
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                        p.sendMessage(MM.deserialize("<green>Tag equipped: <reset>" + tag.render));
                        render();
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
}
