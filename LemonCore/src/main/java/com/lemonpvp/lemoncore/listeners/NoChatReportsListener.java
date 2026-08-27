package com.lemonpvp.lemoncore.listeners;

import com.lemonpvp.lemoncore.LemonCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Prevents players from being able to use Minecraft's built-in chat reporting.
 *
 * <p>Mojang's chat-report system works by attaching a cryptographic signature
 * to each outgoing player message. A signed message can then be submitted to
 * Mojang. By cancelling the signed {@link AsyncChatEvent} at the lowest priority
 * and re-broadcasting it as an <em>unsigned</em> system message, the signature
 * is stripped and the "Report" button in the client GUI becomes unavailable for
 * all player messages that pass through this server.
 *
 * <p>The event is handled at {@link EventPriority#MONITOR} so it fires after all
 * other listeners (including {@link ChatListener}) have had a chance to
 * modify the content. If the event was already cancelled by {@link ChatListener},
 * this listener does nothing — the signed message was already suppressed.
 *
 * <p>Requires Paper 1.19+ (uses {@link AsyncChatEvent}).
 */
public class NoChatReportsListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final LemonCore plugin;

    public NoChatReportsListener(LemonCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Fires at LOWEST priority — before ChatListener — to grab the raw
     * signed message and requeue it as an unsigned broadcast if chat-report
     * cancellation is enabled and ChatListener would not otherwise handle it.
     *
     * <p>ChatListener runs at HIGH priority and cancels the event itself, so
     * in the normal flow this handler is a no-op. It only matters for edge
     * cases where another plugin bypasses ChatListener.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("no-chat-reports", true)) return;
        // If already cancelled (by ChatListener), the signed message was already
        // suppressed; nothing to do.
        if (event.isCancelled()) return;

        // Cancel the signed event and resend as unsigned.
        event.setCancelled(true);
        Component message = event.message();
        Component senderName = event.getPlayer().displayName();

        // Re-broadcast using the Paper system message API (unsigned).
        Component formatted = Component.text("<")
                .append(senderName)
                .append(Component.text("> "))
                .append(message);

        plugin.getServer().broadcast(formatted);
    }
}
