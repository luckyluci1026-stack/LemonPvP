package com.lemonpvp.lemoncore.util;

import com.lemonpvp.lemoncore.LemonCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lets a GUI ask a player to type a value in chat. The next chat message from an
 * awaiting player is captured (never broadcast) and delivered to the callback on
 * the main thread; typing {@code cancel} aborts. Used e.g. by the /aowcode
 * wizard for rank/kill-effect values that can't be listed as buttons.
 */
public class ChatInputManager implements Listener {

    private final LemonCore plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatInputManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    /** Captures the awaiting player's next chat line. Callback runs on the main thread; null = cancelled. */
    public void await(Player player, Consumer<String> onInput) {
        pending.put(player.getUniqueId(), onInput);
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Consumer<String> cb = pending.remove(event.getPlayer().getUniqueId());
        if (cb == null) return;
        event.setCancelled(true); // never broadcast the typed value
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> cb.accept(msg.equalsIgnoreCase("cancel") ? null : msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
