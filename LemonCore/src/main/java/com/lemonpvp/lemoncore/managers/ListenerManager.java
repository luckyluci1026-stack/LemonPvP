package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ListenerManager {

    private final LemonCore plugin;

    /** UUIDs whose imminent kick we triggered ourselves — the PlayerKickEvent
     *  interceptor (KickListener) must let these through unchanged instead of
     *  re-processing them. Consume-once. */
    private final java.util.Set<java.util.UUID> passthroughKicks =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public ListenerManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    /** True (and clears the mark) if this kick was issued by us and should pass through. */
    public boolean consumePassthrough(java.util.UUID uuid) {
        return passthroughKicks.remove(uuid);
    }

    /**
     * Kicks a player so the proxy fully DISCONNECTS them (shows the kick screen)
     * instead of re-routing them to the limbo. Signals LemonQueue via the
     * "PlayerKicking" message first, then kicks after a tiny delay so the message
     * flushes ahead of the disconnect (same TCP connection keeps the order).
     * Used for /kick, /gkick and maintenance kicks.
     */
    public void performKickDisconnect(Player player, Component kickScreen) {
        if (player == null) return;
        plugin.getVelocityMessaging().sendLemonMessage(player, "PlayerKicking",
                player.getUniqueId().toString());
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            passthroughKicks.add(player.getUniqueId());
            player.kick(kickScreen);
        }, 2L);
    }

    /** Issue-time ban kick: announces the Lemonizer broadcast. */
    public void performBanKick(Player player, BanRecord ban) {
        performBanKick(player, ban, true);
    }

    /**
     * Kicks a banned player with the ban screen. {@code announce} controls the
     * Lemonizer broadcast — pass {@code false} on re-kicks (a banned player
     * reconnecting) so the broadcast only happens once, when the ban is issued.
     */
    public void performBanKick(Player player, BanRecord ban, boolean announce) {
        String discord = plugin.getConfigManager().getDiscord();
        String duration = ban.isPermanent()
                ? plugin.getMessagesManager().getRaw("ban.permanent-label")
                : TextUtil.formatDuration(ban.getRemainingSeconds());

        // Build kick screen from messages.yml — no "Banned" header.
        String reasonLine = ban.isPermanent()
                ? plugin.getMessagesManager().getRaw("ban.permanent")
                : plugin.getMessagesManager().getRaw("ban.temp")
                        .replace("{reason}", ban.reason);
        String kickMsg = reasonLine + "\n"
                + plugin.getMessagesManager().getRaw("ban.duration")
                        .replace("{duration}", duration) + "\n"
                + plugin.getMessagesManager().getRaw("ban.ban-id")
                        .replace("{id}", ban.id) + "\n\n"
                + plugin.getMessagesManager().getRaw("ban.appeal")
                        .replace("{discord}", discord);
        Component kickScreen = TextUtil.parse(kickMsg);

        // Tell the Velocity proxy NOT to intercept this kick and re-route to limbo.
        // The extra args (expiry epoch ms + kick screen) let the proxy CACHE the
        // ban and deny future connection attempts at login — the player never
        // reaches a backend again until the ban expires. Old proxies that only
        // read the uuid simply ignore the extras.
        long expiryEpochMs = ban.isPermanent() ? 0L : ban.expires.getTime();
        plugin.getVelocityMessaging().sendLemonMessage(player, "PlayerBanning",
                player.getUniqueId().toString(), String.valueOf(expiryEpochMs), kickMsg);

        // Kick almost immediately so a banned player who joins doesn't stand in the
        // world for a "cooldown" — they should just see the ban screen. The tiny
        // 2-tick delay only guarantees the "PlayerBanning" plugin message above is
        // flushed to the proxy first (same TCP connection, so ordering holds), which
        // is what tells LemonQueue to show this kick instead of rerouting to limbo.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            passthroughKicks.add(player.getUniqueId());
            player.kick(kickScreen);
        }, 2L);

        if (announce) {
            String lmsg = plugin.getMessagesManager().getRaw("ban.lemonizer")
                    .replace("{player}", player.getName())
                    .replace("{reason}", ban.reason);
            org.bukkit.Bukkit.broadcast(TextUtil.parse(lmsg));
        }
    }
}
