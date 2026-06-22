package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.lemonpvp.lemoncosmetics.model.TagType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles unlocking, buying, equipping and rendering cosmetic chat/tablist tags.
 *
 * <p>The equipped tag's rendered MiniMessage string is pushed into LemonCore's
 * {@code PlayerData.tagDisplay} so that LemonCore's chat listener and tablist
 * manager can render it as a name suffix.</p>
 */
public class TagManager {

    private final LemonCosmetics plugin;

    public TagManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Access checks
    // -----------------------------------------------------------------------

    /**
     * Whether the player may equip this tag: either they own a buyable tag, or
     * they hold the tag's permission node (covers permission-only tags and free
     * grants of buyable tags).
     */
    public boolean canUse(Player player, TagType tag) {
        if (player.hasPermission(tag.permission())) return true;
        if (!tag.isBuyable()) return false; // permission-only and no permission
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        return cosmetics != null && cosmetics.ownsTag(tag.id);
    }

    // -----------------------------------------------------------------------
    // Unlock / buy
    // -----------------------------------------------------------------------

    /** Adds the tag to the player's owned set (cache + DB) without equipping it. */
    public CompletableFuture<Void> unlockTag(UUID uuid, String tagId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedTags().add(tagId);
        return plugin.getDatabase().saveTag(uuid, tagId, false);
    }

    /** Buys a tag with coins. Returns {@code false} if not buyable or unaffordable. */
    public CompletableFuture<Boolean> buyTag(UUID uuid, String tagId) {
        TagType tag = TagType.fromId(tagId).orElse(null);
        if (tag == null || !tag.isBuyable()) return CompletableFuture.completedFuture(false);

        return plugin.getCosmeticsManager().canAfford(uuid, tag.price).thenCompose(affordable -> {
            if (!affordable) return CompletableFuture.completedFuture(false);
            var lc = plugin.getCosmeticsManager().getLemonCore();
            if (lc == null) return CompletableFuture.completedFuture(false);
            return lc.getPlayerDataManager()
                    .removeCoins(uuid, tag.price, "cosmetics:tag:" + tagId, null)
                    .thenCompose(v -> unlockTag(uuid, tagId))
                    .thenApply(v -> true);
        });
    }

    // -----------------------------------------------------------------------
    // Equip / unequip
    // -----------------------------------------------------------------------

    public CompletableFuture<Void> equipTag(UUID uuid, String tagId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setEquippedTagId(tagId);

        return plugin.getDatabase().setEquippedTag(uuid, tagId)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) applyTagDisplay(p);
                }));
    }

    public CompletableFuture<Void> unequipTag(UUID uuid) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setEquippedTagId(null);

        return plugin.getDatabase().clearEquippedTag(uuid)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) applyTagDisplay(p);
                }));
    }

    // -----------------------------------------------------------------------
    // Display push to LemonCore
    // -----------------------------------------------------------------------

    /**
     * Pushes the equipped tag's rendered string into LemonCore's PlayerData so
     * chat and tablist can render it. Clears the display when no tag is equipped
     * or the player no longer has access to it.
     */
    public void applyTagDisplay(Player player) {
        var lc = plugin.getCosmeticsManager().getLemonCore();
        if (lc == null) return;
        var pd = lc.getPlayerDataManager().getCached(player.getUniqueId());
        if (pd == null) return;

        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        String equippedId = cosmetics != null ? cosmetics.getEquippedTagId() : null;
        TagType tag = equippedId != null ? TagType.fromId(equippedId).orElse(null) : null;

        if (tag != null && canUse(player, tag)) {
            pd.setTagDisplay(tag.render);
        } else {
            pd.setTagDisplay(null);
        }
    }

    /**
     * Restores the equipped tag on join. Because LemonCore's PlayerData may load
     * slightly after cosmetics, this retries a few times until it is cached.
     */
    public void restoreTag(Player player) {
        scheduleApply(player.getUniqueId(), 0);
    }

    private void scheduleApply(UUID uuid, int attempt) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        var lc = plugin.getCosmeticsManager().getLemonCore();
        if (lc != null && lc.getPlayerDataManager().getCached(uuid) != null) {
            applyTagDisplay(player);
            return;
        }
        if (attempt >= 10) return; // give up after ~5s
        Bukkit.getScheduler().runTaskLater(plugin, () -> scheduleApply(uuid, attempt + 1), 10L);
    }
}
