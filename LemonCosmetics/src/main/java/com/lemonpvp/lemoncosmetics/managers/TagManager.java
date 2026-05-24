package com.lemonpvp.lemoncosmetics.managers;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import com.lemonpvp.lemoncosmetics.model.PlayerCosmetics;
import com.lemonpvp.lemoncosmetics.model.TagType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TagManager {

    private static final Key FONT_TAGS    = Key.key("lemonpvp", "tags");
    private static final Key FONT_CAPS    = Key.key("lemonpvp", "default");
    private static final double TAG_HEIGHT = 0.52;

    private final LemonCosmetics plugin;
    private final Map<UUID, TextDisplay> displays  = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> phases   = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public TagManager(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Every 2 ticks: move displays + advance animation
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L);
    }

    public void stop() {
        if (updateTask != null) updateTask.cancel();
        for (TextDisplay d : displays.values()) {
            if (d.isValid()) d.remove();
        }
        displays.clear();
        phases.clear();
    }

    private void tick() {
        for (Map.Entry<UUID, TextDisplay> entry : displays.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            TextDisplay display = entry.getValue();
            if (player == null || !player.isOnline() || !display.isValid()) {
                display.remove();
                displays.remove(entry.getKey());
                phases.remove(entry.getKey());
                continue;
            }
            // Teleport display to stay above player's head
            display.teleport(getTagLocation(player));

            // Update animated text
            PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
            if (cosmetics == null) continue;
            String tagId = cosmetics.getEquippedTagId();
            TagType tag = TagType.fromId(tagId).orElse(null);
            if (tag == null) continue;

            AtomicInteger phase = phases.computeIfAbsent(entry.getKey(), k -> new AtomicInteger(0));
            int p = phase.getAndAdd(3) % 100;
            display.text(buildTagText(tag, p));
        }
    }

    private Location getTagLocation(Player player) {
        return player.getEyeLocation().add(0, TAG_HEIGHT, 0);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void showTag(Player player, TagType tag) {
        removeTag(player.getUniqueId());
        phases.put(player.getUniqueId(), new AtomicInteger(0));
        TextDisplay display = player.getWorld().spawn(getTagLocation(player), TextDisplay.class, d -> {
            d.text(buildTagText(tag, 0));
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setSeeThrough(false);
            d.setViewRange(64f);
        });
        displays.put(player.getUniqueId(), display);
    }

    public void removeTag(UUID uuid) {
        TextDisplay d = displays.remove(uuid);
        if (d != null && d.isValid()) d.remove();
        phases.remove(uuid);
    }

    public void restoreTag(Player player) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(player.getUniqueId());
        if (cosmetics == null) return;
        String tagId = cosmetics.getEquippedTagId();
        TagType tag = TagType.fromId(tagId).orElse(null);
        if (tag != null) showTag(player, tag);
    }

    // -------------------------------------------------------------------------
    // Tag ownership / equip
    // -------------------------------------------------------------------------

    public CompletableFuture<Void> unlockTag(UUID uuid, String tagId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.getOwnedTags().add(tagId);
        return plugin.getDatabase().saveTag(uuid, tagId, false);
    }

    public CompletableFuture<Void> equipTag(UUID uuid, String tagId) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setEquippedTagId(tagId);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && isLobby()) {
                TagType tag = TagType.fromId(tagId).orElse(null);
                if (tag != null) showTag(player, tag);
            }
        });

        return plugin.getDatabase().setEquippedTag(uuid, tagId);
    }

    public CompletableFuture<Void> unequipTag(UUID uuid) {
        PlayerCosmetics cosmetics = plugin.getCosmeticsManager().getPlayerCosmetics(uuid);
        if (cosmetics != null) cosmetics.setEquippedTagId(null);

        Bukkit.getScheduler().runTask(plugin, () -> removeTag(uuid));

        return plugin.getDatabase().clearEquippedTag(uuid);
    }

    public CompletableFuture<Boolean> buyTag(UUID uuid, String tagId) {
        TagType tag = TagType.fromId(tagId).orElse(null);
        if (tag == null || tag.isPermissionOnly()) return CompletableFuture.completedFuture(false);

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

    // -------------------------------------------------------------------------
    // Text building
    // -------------------------------------------------------------------------

    private Component buildTagText(TagType tag, int phase) {
        // Emoji with phase-based color
        float emojiT = (phase / 100.0f) % 1.0f;
        Component emoji = Component.text(String.valueOf(tag.emoji))
                .font(FONT_TAGS)
                .color(lerpColor(tag.colorStart, tag.colorEnd, emojiT));

        // Text characters with sliding gradient
        String txt = tag.displayName;
        int n = txt.length();
        Component textPart = Component.empty();
        for (int i = 0; i < n; i++) {
            float t = n > 1 ? ((float) i / (n - 1) + phase / 100.0f) % 1.0f : emojiT;
            textPart = textPart.append(Component.text(String.valueOf(txt.charAt(i)))
                    .font(FONT_CAPS)
                    .color(lerpColor(tag.colorStart, tag.colorEnd, t)));
        }

        return emoji.append(Component.text(" ")).append(textPart);
    }

    private static TextColor lerpColor(int c1, int c2, float t) {
        int r = lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = lerp((c1 >>  8) & 0xFF, (c2 >>  8) & 0xFF, t);
        int b = lerp( c1        & 0xFF,  c2        & 0xFF, t);
        return TextColor.color(r, g, b);
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private boolean isLobby() {
        return "LOBBY".equalsIgnoreCase(plugin.getConfig().getString("server-type", "LOBBY"));
    }
}
