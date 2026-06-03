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

import java.util.HashSet;
import java.util.Set;
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

        // --- Synchronous filter checks: cancel immediately so message never gets through ---
        if (plugin.getConfig().getBoolean("anti-swear.enabled", true)) {
            if (plugin.getFilterManager().containsNword(plain)) {
                event.viewers().clear();
                event.setCancelled(true);
                handleNwordOffense(player, uuid);
                return;
            }
            if (plugin.getFilterManager().containsSlur(plain)) {
                event.viewers().clear();
                event.setCancelled(true);
                player.sendMessage(plugin.getMessagesManager().get("chat.filtered"));
                long muteSeconds = TextUtil.parseDuration(
                        plugin.getConfig().getString("anti-swear.other-insult-duration", "1d"));
                plugin.getMuteManager().mutePlayer(uuid, player.getName(), "Inappropriate language",
                        null, "Auto-Mute", muteSeconds);
                return;
            }
        }

        // --- Async mute check: cancel now, re-broadcast if player is not muted ---
        // Snapshot viewers before clearing so we can re-broadcast manually for non-muted players.
        // Clearing the event's viewer set prevents Paper from rendering an empty prefix line.
        final Set<net.kyori.adventure.audience.Audience> viewers = new HashSet<>(event.viewers());
        event.viewers().clear();
        event.setCancelled(true);
        final Component originalMessage = event.message();

        plugin.getMuteManager().getActiveMute(uuid).thenAccept(mute -> {
            if (mute != null) {
                String duration = mute.isPermanent() ? "Permanent"
                        : TextUtil.formatDuration(mute.getRemainingSeconds());
                player.sendMessage(plugin.getMessagesManager().get("mute.muted-duration",
                        "duration", duration));
                return;
            }

            // Not muted — build the formatted message and broadcast to all viewers
            Component msg = originalMessage;
            if (!player.hasPermission("lemoncore.use.minimessage") && !player.isOp()) {
                msg = Component.text(TextUtil.escapeTags(plain));
            }
            final Component finalMsg = msg;

            PlayerData data = plugin.getPlayerDataManager().getCached(uuid);
            String displayName = data != null ? data.getDisplayName() : player.getName();
            Component nameComp = TextUtil.parse(displayName);
            Component formatted = Component.text("<").append(nameComp).append(Component.text("> ")).append(finalMsg);

            for (net.kyori.adventure.audience.Audience viewer : viewers) {
                viewer.sendMessage(formatted);
            }
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
                    null, "Auto-Mute", banDuration).thenAccept(ban -> {
                if (ban != null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.getListenerManager().performBanKick(player, ban));
                }
            });

            plugin.getMuteManager().mutePlayer(uuid, player.getName(), reason, null, "Auto-Mute", banDuration);
        });
    }
}
