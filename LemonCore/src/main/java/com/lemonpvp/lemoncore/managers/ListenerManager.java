package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ListenerManager {

    private final LemonCore plugin;

    public ListenerManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void performBanKick(Player player, BanRecord ban) {
        String discord = plugin.getConfigManager().getDiscord();
        String duration = ban.isPermanent()
                ? plugin.getMessagesManager().getRaw("ban.permanent-label")
                : TextUtil.formatDuration(ban.getRemainingSeconds());

        // Tell the Velocity proxy NOT to intercept this kick and re-route to limbo.
        plugin.getVelocityMessaging().sendLemonMessage(player, "PlayerBanning",
                player.getUniqueId().toString());

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

        // Kick almost immediately so a banned player who joins doesn't stand in the
        // world for a "cooldown" — they should just see the ban screen. The tiny
        // 2-tick delay only guarantees the "PlayerBanning" plugin message above is
        // flushed to the proxy first (same TCP connection, so ordering holds), which
        // is what tells LemonQueue to show this kick instead of rerouting to limbo.
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.kick(kickScreen), 2L);

        String lmsg = plugin.getMessagesManager().getRaw("ban.lemonizer")
                .replace("{player}", player.getName())
                .replace("{reason}", ban.reason);
        org.bukkit.Bukkit.broadcast(TextUtil.parse(lmsg));
    }
}
