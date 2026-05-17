package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class ListenerManager {

    private final LemonCore plugin;

    public ListenerManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    public void performBanKick(Player player, BanRecord ban) {
        String discord = plugin.getConfigManager().getDiscord();
        String duration = ban.isPermanent() ? "Permanent"
                : TextUtil.formatDuration(ban.getRemainingSeconds());

        // Show title
        Component titleComp = plugin.getMessagesManager().get("ban.screen-title");
        Title title = Title.title(titleComp, Component.empty(), Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500)));
        player.showTitle(title);

        // Play wither sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        // Send ban messages (only to banned player)
        if (ban.isPermanent()) {
            player.sendMessage(plugin.getMessagesManager().get("ban.permanent"));
        } else {
            player.sendMessage(plugin.getMessagesManager().get("ban.temp", "reason", ban.reason));
        }
        player.sendMessage(plugin.getMessagesManager().get("ban.ban-id", "id", ban.id));
        player.sendMessage(plugin.getMessagesManager().get("ban.duration", "duration", duration));
        player.sendMessage(plugin.getMessagesManager().get("ban.appeal", "discord", discord));

        // Kick after 2 seconds
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.kick(plugin.getMessagesManager().get("ban.banned"));
        }, 40L);

        // Lemonizer broadcast
        String lmsg = plugin.getMessagesManager().getRaw("ban.lemonizer")
                .replace("{player}", player.getName())
                .replace("{reason}", ban.reason);
        Component lemonizer = TextUtil.parse(lmsg);
        org.bukkit.Bukkit.broadcast(lemonizer);
    }
}
