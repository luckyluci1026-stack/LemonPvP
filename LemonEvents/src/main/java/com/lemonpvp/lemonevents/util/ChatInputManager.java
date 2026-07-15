package com.lemonpvp.lemonevents.util;

import com.lemonpvp.lemonevents.LemonEvents;
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
 * Lets a GUI ask a player to type a value in chat. The next chat line from an
 * awaiting player is captured (never broadcast) and delivered on the main
 * thread; typing {@code cancel} aborts (callback gets null). Used by the owner
 * event panel for tournament / event names.
 */
public class ChatInputManager implements Listener {

    private final LemonEvents plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatInputManager(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public void await(Player player, Consumer<String> onInput) {
        pending.put(player.getUniqueId(), onInput);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Consumer<String> cb = pending.remove(event.getPlayer().getUniqueId());
        if (cb == null) return;
        event.setCancelled(true);
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> cb.accept(msg.equalsIgnoreCase("cancel") ? null : msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
