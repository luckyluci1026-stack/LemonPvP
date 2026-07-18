package com.lemonpvp.lemonchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

/**
 * Standalone pipeline (no LemonCore on this server): cancels the signed chat
 * event and rebroadcasts the formatted line as unsigned system messages —
 * removing the cryptographic signatures is what makes messages unreportable
 * (the NoChatReports behavior), and the format engine gives the LPC look.
 */
public class StandaloneChatListener implements Listener {

    private final LemonChat plugin;

    public StandaloneChatListener(LemonChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("no-chat-reports", true)) return;
        Player player = event.getPlayer();

        Set<Audience> viewers = new HashSet<>(event.viewers());
        event.viewers().clear();
        event.setCancelled(true);

        Component formatted = plugin.format(player, player.displayName(), event.message());
        for (Audience viewer : viewers) viewer.sendMessage(formatted);
    }
}
