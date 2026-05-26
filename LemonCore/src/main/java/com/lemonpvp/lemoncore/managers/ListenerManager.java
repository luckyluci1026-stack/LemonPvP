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
        String duration = ban.isPermanent() ? "Permanent"
                : TextUtil.formatDuration(ban.getRemainingSeconds());

        // Play wither sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        // Build full kick screen — shown by Velocity as the disconnect screen
        String kickMsg = "<bold><red>You have been banned!</bold>\n\n"
                + "<gray>Reason: <white>" + ban.reason + "\n"
                + "<gray>Duration: <white>" + duration + "\n"
                + "<gray>Ban ID: <white>" + ban.id + "\n\n"
                + "<gray>Appeal at: <aqua>" + discord;
        Component kickScreen = TextUtil.parse(kickMsg);

        // Kick after 1 second so sound can play
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.kick(kickScreen), 20L);

        // Lemonizer broadcast
        String lmsg = plugin.getMessagesManager().getRaw("ban.lemonizer")
                .replace("{player}", player.getName())
                .replace("{reason}", ban.reason);
        org.bukkit.Bukkit.broadcast(TextUtil.parse(lmsg));
    }
}
