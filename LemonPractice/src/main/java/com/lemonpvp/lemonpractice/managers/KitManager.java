package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class KitManager {

    private final LemonPractice plugin;
    // player uuid -> (gamemode -> PlayerKit)
    private final Map<UUID, Map<String, PlayerKit>> cache = new HashMap<>();

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

            cache.computeIfAbsent(playerUuid, u -> new HashMap<>()).put(gamemode.toLowerCase(), kit);
            return kit;
        });
    }

    public CompletableFuture<Void> saveKit(UUID playerUuid, String gamemode, PlayerKit kit) {
        // Update cache first
        cache.computeIfAbsent(playerUuid, u -> new HashMap<>()).put(gamemode.toLowerCase(), kit);

        // Persist each slot asynchronously
        CompletableFuture<?>[] futures = kit.getSlots().entrySet().stream()
                .map(entry -> {
                    try {
                        byte[] bytes = entry.getValue().serializeAsBytes();
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        return plugin.getDatabase().saveKitSlot(playerUuid, gamemode, entry.getKey(), base64);
                    } catch (Exception e) {
                        plugin.getLogger().warning("[KitManager] Failed to serialize item for slot "
                                + entry.getKey() + " of " + playerUuid + ": " + e.getMessage());
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
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

    /**
     * Reads a default kit from the "kits" section of kits.yml.
     * Expected structure per gamemode:
     * <pre>
     * kits:
     *   sword:
     *     0:
     *       material: DIAMOND_SWORD
     *       enchantments:
     *         SHARPNESS: 5
     *     ...
     * </pre>
     */
    public PlayerKit getDefaultKit(String gamemode) {
        ConfigurationSection kitsSection = plugin.getKitsConfig().getConfigurationSection("kits");
        if (kitsSection == null) {
            plugin.getLogger().warning("[KitManager] 'kits' section missing from kits.yml");
            return null;
        }

        ConfigurationSection gamemodeSection = kitsSection.getConfigurationSection(gamemode.toLowerCase());
        if (gamemodeSection == null) return null;

        // UUID.fromString("00000000-0000-0000-0000-000000000000") as a sentinel for default kits
        PlayerKit kit = new PlayerKit(new UUID(0, 0), gamemode);

        for (String slotKey : gamemodeSection.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(slotKey);
            } catch (NumberFormatException e) {
                continue;
            }

            ConfigurationSection itemSection = gamemodeSection.getConfigurationSection(slotKey);
            if (itemSection == null) continue;

            String materialName = itemSection.getString("material");
            if (materialName == null) continue;

            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("[KitManager] Unknown material '" + materialName
                        + "' in default kit for " + gamemode + " slot " + slot);
                continue;
            }

            ItemStack item = new ItemStack(material, itemSection.getInt("amount", 1));
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (itemSection.contains("name")) {
                    meta.setDisplayName(itemSection.getString("name"));
                }
                if (itemSection.contains("custom-model-data")) {
                    meta.setCustomModelData(itemSection.getInt("custom-model-data"));
                }
                if (itemSection.contains("unbreakable")) {
                    meta.setUnbreakable(itemSection.getBoolean("unbreakable", false));
                }
                item.setItemMeta(meta);
            }

            ConfigurationSection enchSection = itemSection.getConfigurationSection("enchantments");
            if (enchSection != null) {
                for (String enchKey : enchSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByName(enchKey.toUpperCase());
                    if (enchantment != null) {
                        item.addUnsafeEnchantment(enchantment, enchSection.getInt(enchKey));
                    } else {
                        plugin.getLogger().warning("[KitManager] Unknown enchantment '" + enchKey
                                + "' in default kit for " + gamemode);
                    }
                }
            }

            kit.setSlot(slot, item);
        }

        return kit;
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
}
