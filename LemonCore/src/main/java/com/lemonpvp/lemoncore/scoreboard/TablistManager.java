package com.lemonpvp.lemoncore.scoreboard;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Renders a professional network tablist:
 * <ul>
 *   <li>Gradient header + footer (branding, online count, server address).</li>
 *   <li>Per-player list names prefixed with their LuckPerms rank prefix.</li>
 * </ul>
 *
 * <p>Refreshed every second. Header/footer lines are configurable under
 * {@code tablist.*}; the online-count and player placeholders are resolved
 * server-side. LuckPerms is read from the cache via the synchronous
 * {@code PlayerAdapter}, so this is safe to call on the main thread.</p>
 */
public class TablistManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    /** Handles both &-codes and §-codes used by LuckPerms prefixes/suffixes. */
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private final LemonCore plugin;

    public TablistManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    private void updateAll() {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) return;

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        Component header = buildSection("tablist.header", online, max);
        Component footer = buildSection("tablist.footer", online, max);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header, footer);
            player.playerListName(buildListName(player));
        }
    }

    // -------------------------------------------------------------------------
    // Header / footer
    // -------------------------------------------------------------------------

    private Component buildSection(String path, int online, int max) {
        List<String> lines = plugin.getConfig().getStringList(path);
        if (lines.isEmpty()) return Component.empty();

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i)
                    .replace("{online}", String.valueOf(online))
                    .replace("{max}", String.valueOf(max));
            result = result.append(MM.deserialize(line));
            if (i < lines.size() - 1) result = result.append(Component.newline());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Per-player list name (rank prefix + name)
    // -------------------------------------------------------------------------

    private Component buildListName(Player player) {
        String prefix = "";
        String suffix = "";

        LuckPerms lp = plugin.getLuckPerms();
        if (lp != null) {
            try {
                CachedMetaData meta = lp.getPlayerAdapter(Player.class).getMetaData(player);
                if (meta.getPrefix() != null) prefix = meta.getPrefix();
                if (meta.getSuffix() != null) suffix = meta.getSuffix();
            } catch (Exception ignored) {
                // LuckPerms cache miss — fall back to no prefix.
            }
        }

        // Use the player's display name (handles nicks) when available.
        String name = player.getName();
        PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data != null) name = data.getDisplayName();

        // LuckPerms prefixes/suffixes are legacy colour-coded; normalise § to &
        // so the legacy serializer renders them correctly, then concatenate.
        Component prefixComp = LEGACY.deserialize(prefix.replace('§', '&'));
        Component nameComp   = LEGACY.deserialize(name.replace('§', '&'));
        Component suffixComp = LEGACY.deserialize(suffix.replace('§', '&'));

        // ELO division badge (e.g. "❺" in Diamond gradient) pushed by LemonPractice.
        String rank = data != null ? data.getRankDisplay() : null;
        Component rankComp = (rank != null && !rank.isEmpty())
                ? MM.deserialize(rank).append(Component.space())
                : Component.empty();

        Component result = prefixComp.append(rankComp).append(nameComp).append(suffixComp);

        // Cosmetic tag suffix (pushed by LemonCosmetics).
        String tag = data != null ? data.getTagDisplay() : null;
        if (tag != null && !tag.isEmpty()) {
            result = result.append(Component.space()).append(MM.deserialize(tag));
        }

        return result.decoration(TextDecoration.ITALIC, false);
    }
}
