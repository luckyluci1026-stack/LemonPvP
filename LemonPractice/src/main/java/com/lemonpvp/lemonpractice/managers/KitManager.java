package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Gamemode;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class KitManager {

    private final LemonPractice plugin;
    // player uuid -> (gamemode -> PlayerKit)
    // Accessed from both async DB callbacks and the main thread — must be concurrent.
    private final Map<UUID, Map<String, PlayerKit>> cache = new ConcurrentHashMap<>();

    public KitManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Load / save from DB
    // -----------------------------------------------------------------------

    public CompletableFuture<PlayerKit> loadKit(UUID playerUuid, String gamemode) {
        return plugin.getDatabase().loadKit(playerUuid, gamemode).thenApply(slotData -> {
            if (slotData == null || slotData.isEmpty()) return null;

            PlayerKit kit = new PlayerKit(playerUuid, gamemode);
            for (Map.Entry<Integer, String> entry : slotData.entrySet()) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(entry.getValue());
                    ItemStack item = ItemStack.deserializeBytes(bytes);
                    kit.setSlot(entry.getKey(), item);
                } catch (Exception e) {
                    plugin.getLogger().warning("[KitManager] Failed to deserialize item for slot "
                            + entry.getKey() + " of " + playerUuid + " (" + gamemode + "): " + e.getMessage());
                }
            }

            cache.computeIfAbsent(playerUuid, u -> new ConcurrentHashMap<>()).put(gamemode.toLowerCase(), kit);
            return kit;
        });
    }

    public CompletableFuture<Void> saveKit(UUID playerUuid, String gamemode, PlayerKit kit) {
        // Update cache first
        cache.computeIfAbsent(playerUuid, u -> new ConcurrentHashMap<>()).put(gamemode.toLowerCase(), kit);

        // Snapshot + serialize the current slots up front so the async write is stable.
        Map<Integer, String> serialized = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : kit.getSlots().entrySet()) {
            try {
                byte[] bytes = entry.getValue().serializeAsBytes();
                serialized.put(entry.getKey(), Base64.getEncoder().encodeToString(bytes));
            } catch (Exception e) {
                plugin.getLogger().warning("[KitManager] Failed to serialize item for slot "
                        + entry.getKey() + " of " + playerUuid + ": " + e.getMessage());
            }
        }

        // Delete the existing rows FIRST, then insert the current arrangement. This
        // prevents slots that became empty (e.g. an item moved elsewhere) from
        // leaving a stale DB row that would reload as a duplicate item later.
        return plugin.getDatabase().deleteKit(playerUuid, gamemode).thenCompose(v -> {
            CompletableFuture<?>[] futures = serialized.entrySet().stream()
                    .map(e -> plugin.getDatabase().saveKitSlot(playerUuid, gamemode, e.getKey(), e.getValue()))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures);
        });
    }

    // -----------------------------------------------------------------------
    // Cache retrieval
    // -----------------------------------------------------------------------

    public PlayerKit getKit(UUID playerUuid, String gamemode) {
        Map<String, PlayerKit> playerKits = cache.get(playerUuid);
        if (playerKits == null) return null;
        return playerKits.get(gamemode.toLowerCase());
    }

    // -----------------------------------------------------------------------
    // Default kit from kits.yml
    // -----------------------------------------------------------------------

    /** Bukkit PlayerInventory armor slot indices (so {@code setItem} equips them). */
    public static final int ARMOR_FEET = 36, ARMOR_LEGS = 37, ARMOR_CHEST = 38, ARMOR_HEAD = 39;

    /**
     * Reads the preset (default) kit from the "kits" section of kits.yml.
     *
     * <p>Preferred structured layout per gamemode:
     * <pre>
     * kits:
     *   sword:
     *     hotbar:
     *       0: {material: DIAMOND_SWORD, enchantments: {SHARPNESS: 5}}
     *     armor:
     *       head: {material: DIAMOND_HELMET}
     *       chest/legs/feet: ...
     * </pre>
     * Hotbar entries map to inventory slots 0–8; armor maps to the player
     * inventory's armor slots ({@link #ARMOR_HEAD} etc.) so applying the kit with
     * {@code setItem} equips it. A legacy flat {@code <slot>: {...}} layout is
     * still accepted for backward compatibility.
     */
    /** Sentinel UUID under which admin-edited preset kits are stored (DB-shared, wins over kits.yml). */
    public static final UUID ADMIN_KIT_UUID = new UUID(0, 0);

    public PlayerKit getDefaultKit(String gamemode) {
        // An admin-edited preset (persisted under the sentinel UUID, shared across
        // all duel servers via the DB) overrides the kits.yml default.
        PlayerKit admin = getKit(ADMIN_KIT_UUID, gamemode);
        if (admin != null && !admin.getSlots().isEmpty()) {
            PlayerKit copy = new PlayerKit(ADMIN_KIT_UUID, gamemode);
            admin.getSlots().forEach((slot, item) -> copy.setSlot(slot, item.clone()));
            return copy;
        }

        ConfigurationSection kitsSection = plugin.getKitsConfig().getConfigurationSection("kits");
        if (kitsSection == null) {
            plugin.getLogger().warning("[KitManager] 'kits' section missing from kits.yml");
            return null;
        }

        ConfigurationSection gm = kitsSection.getConfigurationSection(gamemode.toLowerCase());
        if (gm == null) return null;

        // sentinel UUID 0 marks a default/preset kit
        PlayerKit kit = new PlayerKit(new UUID(0, 0), gamemode);

        ConfigurationSection hotbar = gm.getConfigurationSection("hotbar");
        ConfigurationSection invSec = gm.getConfigurationSection("inventory");
        ConfigurationSection armor = gm.getConfigurationSection("armor");
        boolean structured = hotbar != null || invSec != null || armor != null;

        if (hotbar != null) {
            for (String slotKey : hotbar.getKeys(false)) {
                int slot;
                try { slot = Integer.parseInt(slotKey); } catch (NumberFormatException e) { continue; }
                if (slot < 0 || slot > 8) continue;
                ItemStack item = buildItem(hotbar.getConfigurationSection(slotKey), gamemode);
                if (item != null) kit.setSlot(slot, item);
            }
        }
        // Backup items in the main inventory (slots 9–35), e.g. refill pots/steak.
        if (invSec != null) {
            for (String slotKey : invSec.getKeys(false)) {
                int slot;
                try { slot = Integer.parseInt(slotKey); } catch (NumberFormatException e) { continue; }
                if (slot < 9 || slot > 35) continue;
                ItemStack item = buildItem(invSec.getConfigurationSection(slotKey), gamemode);
                if (item != null) kit.setSlot(slot, item);
            }
        }
        if (armor != null) {
            putArmor(kit, armor.getConfigurationSection("head"), ARMOR_HEAD, gamemode);
            putArmor(kit, armor.getConfigurationSection("chest"), ARMOR_CHEST, gamemode);
            putArmor(kit, armor.getConfigurationSection("legs"), ARMOR_LEGS, gamemode);
            putArmor(kit, armor.getConfigurationSection("feet"), ARMOR_FEET, gamemode);
        }

        // Legacy flat layout: numeric slot keys directly under the gamemode.
        if (!structured) {
            for (String slotKey : gm.getKeys(false)) {
                int slot;
                try { slot = Integer.parseInt(slotKey); } catch (NumberFormatException e) { continue; }
                ItemStack item = buildItem(gm.getConfigurationSection(slotKey), gamemode);
                if (item != null) kit.setSlot(slot, item);
            }
        }

        return kit;
    }

    /** The server-defined preset kit for a gamemode (an alias of the default kit). */
    public PlayerKit getPresetKit(String gamemode) {
        return getDefaultKit(gamemode);
    }

    /** True for the player-inventory armor slot indices (36–39). */
    public static boolean isArmorSlot(int slot) {
        return slot >= ARMOR_FEET && slot <= ARMOR_HEAD;
    }

    private void putArmor(PlayerKit kit, ConfigurationSection sec, int slot, String gamemode) {
        ItemStack item = buildItem(sec, gamemode);
        if (item != null) kit.setSlot(slot, item);
    }

    private ItemStack buildItem(ConfigurationSection sec, String gamemode) {
        if (sec == null) return null;
        String materialName = sec.getString("material");
        if (materialName == null) return null;

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("[KitManager] Unknown material '" + materialName
                    + "' in kit for " + gamemode);
            return null;
        }

        ItemStack item = new ItemStack(material, sec.getInt("amount", 1));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (sec.contains("name")) meta.setDisplayName(sec.getString("name"));
            if (sec.contains("custom-model-data")) meta.setCustomModelData(sec.getInt("custom-model-data"));
            if (sec.contains("unbreakable")) meta.setUnbreakable(sec.getBoolean("unbreakable", false));
            // Potions: `potion: STRONG_HEALING` sets the vanilla base type on any
            // potion/splash/lingering/tipped-arrow item (PotionType enum names).
            if (sec.contains("potion") && meta instanceof org.bukkit.inventory.meta.PotionMeta pm) {
                String typeName = String.valueOf(sec.getString("potion")).toUpperCase();
                try {
                    pm.setBasePotionType(org.bukkit.potion.PotionType.valueOf(typeName));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[KitManager] Unknown potion type '" + typeName
                            + "' in kit for " + gamemode);
                }
            }
            item.setItemMeta(meta);
        }

        ConfigurationSection enchSection = sec.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (String enchKey : enchSection.getKeys(false)) {
                Enchantment enchantment = resolveEnchantment(enchKey);
                if (enchantment != null) {
                    item.addUnsafeEnchantment(enchantment, enchSection.getInt(enchKey));
                } else {
                    plugin.getLogger().warning("[KitManager] Unknown enchantment '" + enchKey
                            + "' in kit for " + gamemode);
                }
            }
        }

        return item;
    }

    /**
     * Resolves an enchantment from kits.yml. Names use the modern namespaced keys
     * (e.g. {@code SHARPNESS}, {@code PROTECTION}), so we try the minecraft key
     * first and only then fall back to the legacy by-name lookup.
     */
    @SuppressWarnings("deprecation")
    private Enchantment resolveEnchantment(String key) {
        Enchantment byKey = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
        if (byKey != null) return byKey;
        return Enchantment.getByName(key.toUpperCase());
    }

    // -----------------------------------------------------------------------
    // Effective kit (player custom > default)
    // -----------------------------------------------------------------------

    public PlayerKit getEffectiveKit(UUID playerUuid, String gamemode) {
        PlayerKit custom = getKit(playerUuid, gamemode);
        if (custom != null && !custom.getSlots().isEmpty()) return custom;
        return getDefaultKit(gamemode);
    }

    // -----------------------------------------------------------------------
    // Cache eviction (call on player quit)
    // -----------------------------------------------------------------------

    public void evict(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    /**
     * Warms the cache by loading the player's saved kit for every gamemode from
     * the database (async). Call on join so a player's saved arrangement is
     * applied in duels/FFA — the cache is per-JVM, so each server must load it.
     */
    public void preload(UUID playerUuid) {
        for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
            loadKit(playerUuid, gm.getId());
        }
    }

    // -----------------------------------------------------------------------
    // Admin preset kits (stored under ADMIN_KIT_UUID, shared via the DB)
    // -----------------------------------------------------------------------

    /** Loads every admin-edited preset into cache on enable so getDefaultKit sees them. */
    public void preloadAdminKits() {
        for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
            loadKit(ADMIN_KIT_UUID, gm.getId());
        }
    }

    /** True if a custom admin preset exists for this gamemode (overriding kits.yml). */
    public boolean hasAdminKit(String gamemode) {
        PlayerKit k = getKit(ADMIN_KIT_UUID, gamemode);
        return k != null && !k.getSlots().isEmpty();
    }

    /** Last shared admin-kit version this server has applied (for the sync poll). */
    private volatile long lastKitVersion = 0;

    /** Saves an admin preset (persisted + cached), then bumps the shared version so every server reloads. */
    public CompletableFuture<Void> saveAdminKit(String gamemode, PlayerKit kit) {
        return saveKit(ADMIN_KIT_UUID, gamemode, kit)
                .thenCompose(v -> plugin.getDatabase().bumpKitVersion())
                .thenAccept(newVer -> { if (newVer > 0) lastKitVersion = newVer; });
    }

    /** Deletes the admin preset (reverting to kits.yml), then bumps the shared version. */
    public CompletableFuture<Void> deleteAdminKit(String gamemode) {
        clearCached(ADMIN_KIT_UUID, gamemode);
        return plugin.getDatabase().deleteKit(ADMIN_KIT_UUID, gamemode)
                .thenCompose(v -> plugin.getDatabase().bumpKitVersion())
                .thenAccept(newVer -> { if (newVer > 0) lastKitVersion = newVer; });
    }

    /**
     * Starts the network-wide admin-kit sync: every 2 s this server checks the
     * shared version and, if another server bumped it (a /kitadmin edit), reloads
     * the presets from the DB into cache. Robust even for empty servers — no
     * player-tied plugin messaging. Call once on enable.
     */
    public void startAdminKitSync() {
        plugin.getDatabase().getKitVersion().thenAccept(v -> lastKitVersion = v); // seed with current
        org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
            plugin.getDatabase().getKitVersion().thenAccept(dbVer -> {
                if (dbVer > lastKitVersion) {
                    lastKitVersion = dbVer;
                    // loadKit is async + caches in a ConcurrentHashMap — safe off-thread.
                    for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
                        loadKit(ADMIN_KIT_UUID, gm.getId());
                    }
                    plugin.getLogger().info("[KitManager] Admin presets reloaded from network (v" + dbVer + ").");
                }
            }), 40L, 40L);
    }

    /** Removes a single (uuid, gamemode) entry from the cache. */
    public void clearCached(UUID uuid, String gamemode) {
        Map<String, PlayerKit> m = cache.get(uuid);
        if (m != null) m.remove(gamemode.toLowerCase());
    }
}
