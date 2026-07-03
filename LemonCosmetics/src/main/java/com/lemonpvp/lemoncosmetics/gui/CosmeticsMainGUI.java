package com.lemonpvp.lemoncosmetics.gui;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.ArrowTrailType;
import com.lemonpvp.lemoncosmetics.model.KillEffectType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.lemonpvp.lemoncosmetics.model.TagType;
import net.kyori.adventure.text.Component;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsMainGUI implements Listener {

    static final MiniMessage MM = MiniMessage.miniMessage();

    // Category slots — six categories in row 1 (slots 10–15)
    private static final int TRIMS_SLOT   = 10;
    private static final int TRAILS_SLOT  = 11;
    private static final int EFFECTS_SLOT = 12;
    private static final int DEATH_SLOT   = 13;
    private static final int WINS_SLOT    = 14;
    private static final int TAGS_SLOT    = 15;
    private static final int PROFILE_SLOT = 4;

    private final LemonCosmetics plugin;
    private final Player player;
    private Inventory inventory;
    private boolean registered = false;

    // Reusable filler items
    private static final ItemStack BLACK_FILLER = buildFiller(Material.BLACK_STAINED_GLASS_PANE);
    private static final ItemStack GRAY_FILLER  = buildFiller(Material.GRAY_STAINED_GLASS_PANE);

    public CosmeticsMainGUI(LemonCosmetics plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27,
                MM.deserialize("<!italic><gradient:#fffb00:#00ff00>ᴄᴏsᴍᴇᴛɪᴄs</gradient>"));

        fillBorders();

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());

        inventory.setItem(PROFILE_SLOT,  buildProfileItem(cosmetics));
        inventory.setItem(TRIMS_SLOT,    buildTrimsItem(cosmetics));
        inventory.setItem(TRAILS_SLOT,   buildTrailsItem(cosmetics));
        inventory.setItem(EFFECTS_SLOT,  buildEffectsItem(cosmetics));
        inventory.setItem(DEATH_SLOT,    buildDeathEffectsItem(cosmetics));
        inventory.setItem(WINS_SLOT,     buildWinEffectsItem(cosmetics));
        inventory.setItem(TAGS_SLOT,     buildTagsItem(cosmetics));

        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void fillBorders() {
        // Row 0 and Row 2: full black border
        for (int i = 0; i < 9; i++) inventory.setItem(i, BLACK_FILLER);
        for (int i = 18; i < 27; i++) inventory.setItem(i, BLACK_FILLER);
        // Row 1: black edges around the six categories (slots 10–15)
        inventory.setItem(9,  BLACK_FILLER);
        inventory.setItem(16, BLACK_FILLER);
        inventory.setItem(17, BLACK_FILLER);
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildProfileItem(PlayerCosmetics cosmetics) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        if (skull == null) return head;

        skull.setOwningPlayer(player);
        skull.displayName(MM.deserialize("<!italic><gradient:#fffb00:#00ff00>" + player.getName() + "</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Coins — read from LemonCore cache (synchronous)
        long coins = getCachedCoins();
        lore.add(MM.deserialize("<!italic><gray>Coins: <gold>" + formatNumber(coins) + " ⭐"));

        lore.add(Component.empty());

        if (cosmetics != null) {
            lore.add(MM.deserialize("<!italic><gray>Tags: <white>" + cosmetics.getOwnedTags().size()
                    + "<gray>/<white>" + TagType.values().length));
            lore.add(MM.deserialize("<!italic><gray>Trails: <white>" + cosmetics.getOwnedTrails().size()
                    + "<gray>/<white>" + ArrowTrailType.values().length));
            lore.add(MM.deserialize("<!italic><gray>Effects: <white>" + cosmetics.getOwnedEffects().size()
                    + "<gray>/<white>" + KillEffectType.values().length));
            lore.add(MM.deserialize("<!italic><gray>Death FX: <white>" + cosmetics.getOwnedDeathEffects().size()
                    + "<gray>/<white>" + com.lemonpvp.lemoncosmetics.model.DeathEffectType.values().length));
            lore.add(MM.deserialize("<!italic><gray>Win FX: <white>" + cosmetics.getOwnedWinEffects().size()
                    + "<gray>/<white>" + com.lemonpvp.lemoncosmetics.model.WinEffectType.values().length));
            int totalPatterns = plugin.getArmorTrimManager().getAllPatternIds().size();
            lore.add(MM.deserialize("<!italic><gray>Trims: <white>" + cosmetics.getOwnedPatterns().size()
                    + "<gray>/<white>" + totalPatterns));
        } else {
            lore.add(MM.deserialize("<!italic><gray>Loading data..."));
        }

        skull.lore(lore);
        head.setItemMeta(skull);
        return head;
    }

    private ItemStack buildTrimsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#4fc3f7:#0288d1>Armor Trims</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Customize your armor with"));
        lore.add(MM.deserialize("<!italic><gray>unique trims."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int totalPatterns = plugin.getArmorTrimManager().getAllPatternIds().size();
            lore.add(MM.deserialize("<!italic><dark_gray>Patterns: <white>" + cosmetics.getOwnedPatterns().size()
                    + "<dark_gray>/<white>" + totalPatterns));
            lore.add(MM.deserialize("<!italic><dark_gray>Materials: <white>" + cosmetics.getOwnedMaterials().size()));
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Click to open"));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTrailsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#ab47bc:#7b1fa2>Arrow Trails</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Leave a colorful effect"));
        lore.add(MM.deserialize("<!italic><gray>behind your arrows."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedTrails().size();
            int total = ArrowTrailType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Owned: <white>" + owned + "<dark_gray>/<white>" + total));
            String activeTrail = cosmetics.getActiveTrailId();
            if (activeTrail != null) {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <light_purple>" + activeTrail));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <gray>None"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Click to open"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEffectsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#ff7043:#bf360c>Kill Effects</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Trigger an effect when"));
        lore.add(MM.deserialize("<!italic><gray>you eliminate a player."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedEffects().size();
            int total = KillEffectType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Owned: <white>" + owned + "<dark_gray>/<white>" + total));
            String activeEffect = cosmetics.getActiveEffectId();
            if (activeEffect != null) {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <red>" + activeEffect));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <gray>None"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Click to open"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDeathEffectsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#b0bec5:#37474f>Death Effects</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Go out in style — an effect"));
        lore.add(MM.deserialize("<!italic><gray>plays where you fall."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedDeathEffects().size();
            int total = com.lemonpvp.lemoncosmetics.model.DeathEffectType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Owned: <white>" + owned + "<dark_gray>/<white>" + total));
            String active = cosmetics.getActiveDeathEffectId();
            if (active != null) {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <red>" + active));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <gray>None"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Click to open"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildWinEffectsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#ffd700:#ff8f00>Win Effects</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Celebrate your duel wins"));
        lore.add(MM.deserialize("<!italic><gray>in style."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedWinEffects().size();
            int total = com.lemonpvp.lemoncosmetics.model.WinEffectType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Owned: <white>" + owned + "<dark_gray>/<white>" + total));
            String active = cosmetics.getActiveWinEffectId();
            if (active != null) {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <gold>" + active));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Active: <gray>None"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Click to open"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTagsItem(PlayerCosmetics cosmetics) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize("<!italic><gradient:#69f0ae:#00bfa5>Tags</gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><gray>Display a tag as a suffix"));
        lore.add(MM.deserialize("<!italic><gray>behind your name."));
        lore.add(Component.empty());

        if (cosmetics != null) {
            int owned = cosmetics.getOwnedTags().size();
            int total = TagType.values().length;
            lore.add(MM.deserialize("<!italic><dark_gray>Owned: <white>" + owned + "<dark_gray>/<white>" + total));
            String equippedTag = cosmetics.getEquippedTagId();
            if (equippedTag != null) {
                TagType t = TagType.fromId(equippedTag).orElse(null);
                lore.add(MM.deserialize("<!italic><dark_gray>Equipped: <reset>"
                        + (t != null ? t.render : equippedTag)));
            } else {
                lore.add(MM.deserialize("<!italic><dark_gray>Equipped: <gray>None"));
            }
        }

        lore.add(Component.empty());
        lore.add(MM.deserialize("<!italic><aqua>➜ Click to open"));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private long getCachedCoins() {
        com.lemonpvp.lemoncosmetics.managers.CosmeticsManager cm = plugin.getCosmeticsManager();
        com.lemonpvp.lemoncore.LemonCore lc = cm.getLemonCore();
        if (lc == null) return 0;
        com.lemonpvp.lemoncore.managers.PlayerData pd =
                lc.getPlayerDataManager().getCached(player.getUniqueId());
        return pd != null ? pd.getCoins() : 0;
    }

    private String formatNumber(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private static ItemStack buildFiller(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;

        switch (event.getSlot()) {
            case TRIMS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new TrimPatternGUI(plugin, player).open();
            }
            case TRAILS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new ArrowTrailsGUI(plugin, player).open();
            }
            case EFFECTS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new KillEffectsGUI(plugin, player).open();
            }
            case DEATH_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new DeathEffectsGUI(plugin, player).open();
            }
            case WINS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new WinEffectsGUI(plugin, player).open();
            }
            case TAGS_SLOT -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                unregister();
                new TagsGUI(plugin, player).open();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
        unregister();
    }

    private void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }
}
