package com.lemonpvp.lemoncore.listeners;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.*;
import com.lemonpvp.lemoncore.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatListener implements Listener {

    private final LemonCore plugin;

    public ChatListener(LemonCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());

        // Check mute (async-safe)
        plugin.getMuteManager().getActiveMute(uuid).thenAccept(mute -> {
            if (mute != null) {
                event.setCancelled(true);
                String duration = mute.isPermanent() ? "Permanent"
                        : TextUtil.formatDuration(mute.getRemainingSeconds());
                player.sendMessage(plugin.getMessagesManager().get("mute.muted-duration",
                        "duration", duration));
                return;
            }

            // Anti-swear checks
            if (plugin.getConfig().getBoolean("anti-swear.enabled", true)) {
                // N-word check
                if (plugin.getFilterManager().containsNword(plain)) {
                    event.setCancelled(true);
                    handleNwordOffense(player, uuid);
                    return;
                }

                // Other slur check
                if (plugin.getFilterManager().containsSlur(plain)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessagesManager().get("chat.filtered"));
                    long muteSeconds = TextUtil.parseDuration(
                            plugin.getConfig().getString("anti-swear.other-insult-duration", "1d"));
                    plugin.getMuteManager().mutePlayer(uuid, player.getName(), "Inappropriate language",
                            null, "System", muteSeconds);
                    return;
                }
            }

            // Strip MiniMessage tags for players without permission
            if (!player.hasPermission("lemoncore.use.minimessage") && !player.isOp()) {
                String escaped = TextUtil.escapeTags(plain);
                event.message(Component.text(escaped));
            }

            // Send as SYSTEM message to prevent vanilla reporting
            event.renderer((source, sourceDisplayName, message, viewer) -> {
                PlayerData data = plugin.getPlayerDataManager().getCached(source.getUniqueId());
                String displayName = data != null ? data.getDisplayName() : source.getName();
                return Component.text("<" + displayName + "> ").append(message);
            });
        });
    }

    private void handleNwordOffense(Player player, UUID uuid) {
        player.sendMessage(plugin.getMessagesManager().get("chat.filtered"));
        plugin.getOffenseManager().incrementNword(uuid).thenAccept(count -> {
            long banDuration;
            String reason = "Racism";
            if (count == 1) {
                banDuration = TextUtil.parseDuration(
                        plugin.getConfig().getString("anti-swear.nword.first-offense-duration", "3d"));
            } else if (count == 2) {
                banDuration = TextUtil.parseDuration(
                        plugin.getConfig().getString("anti-swear.nword.second-offense-duration", "14d"));
            } else {
                banDuration = 0; // Permanent
            }

            plugin.getBanManager().banPlayer(uuid, player.getName(), reason,
                    null, "System", banDuration).thenAccept(ban -> {
                if (ban != null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.getListenerManager().performBanKick(player, ban));
                }
            });

            // Also mute
            plugin.getMuteManager().mutePlayer(uuid, player.getName(), reason, null, "System", banDuration);
        });
    }
}
