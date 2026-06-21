package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.HatType;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HatManager {

    private final LemonCosmetics plugin;

    public HatManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    /** Unlocks hat for player (adds to owned, saves to DB). */
    public CompletableFuture<Void> unlockHat(UUID uuid, String hatId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedHats().add(hatId);
        return plugin.getDatabase().saveHat(uuid, hatId, false);
    }

    /** Equips hat: puts invisible leather helmet with CMD in head slot. */
    public CompletableFuture<Void> equipHat(UUID uuid, String hatId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setEquippedHatId(hatId);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) applyHat(player, hatId);
        });

        return plugin.getDatabase().setEquippedHat(uuid, hatId);
    }

    /** Unequips hat: removes helmet, clears CMD item. */
    public CompletableFuture<Void> unequipHat(UUID uuid) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setEquippedHatId(null);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) removeHat(player);
        });

        return plugin.getDatabase().clearEquippedHat(uuid);
    }

    /** Applies the hat item to the player's head slot. Main thread only. */
    public void applyHat(Player player, String hatId) {
        HatType hat = HatType.fromId(hatId).orElse(null);
        if (hat == null) return;

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        if (!(helmet.getItemMeta() instanceof LeatherArmorMeta meta)) return;
        meta.setColor(Color.fromRGB(0, 0, 0));
        meta.setCustomModelData(hat.customModelData);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE);
        helmet.setItemMeta(meta);

        player.getInventory().setHelmet(helmet);
    }

    /** Removes the hat from the player's head slot if it is one of our CMD helmets. */
    public void removeHat(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.LEATHER_HELMET) return;
        if (!helmet.hasItemMeta()) return;
        if (!helmet.getItemMeta().hasCustomModelData()) return;
        int cmd = helmet.getItemMeta().getCustomModelData();
        for (HatType hat : HatType.values()) {
            if (hat.customModelData == cmd) {
                player.getInventory().setHelmet(null);
                return;
            }
        }
    }

    /** Restores the equipped hat for an online player (call on join). */
    public void restoreHat(Player player) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        if (cosmetics == null) return;
        String hatId = cosmetics.getEquippedHatId();
        if (hatId != null) applyHat(player, hatId);
    }

    /** Buy a hat using coins. */
    public CompletableFuture<Boolean> buyHat(UUID uuid, String hatId) {
        HatType hat = HatType.fromId(hatId).orElse(null);
        if (hat == null) return CompletableFuture.completedFuture(false);

        return plugin.getCosmeticsManager().canAfford(uuid, hat.price).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            var lc = plugin.getCosmeticsManager().getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, hat.price, "cosmetics:hat:" + hatId, null)
                    .thenCompose(v -> unlockHat(uuid, hatId))
                    .thenApply(v -> true);
        });
    }
}
