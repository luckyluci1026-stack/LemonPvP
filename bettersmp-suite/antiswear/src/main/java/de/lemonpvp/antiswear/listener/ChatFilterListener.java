package de.lemonpvp.antiswear.listener;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatFilterListener implements Listener {

    private final AntiSwear plugin;

    public ChatFilterListener(AntiSwear plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player spieler = event.getPlayer();
        if (plugin.strikes().istStummgeschaltet(spieler.getUniqueId())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.stummHinweis(spieler));
            return;
        }
        if (spieler.hasPermission("antiswear.bypass")) {
            return;
        }
        String klartext = PlainTextComponentSerializer.plainText().serialize(event.message());
        ChatPruefung.Ergebnis ergebnis = plugin.pruefung().pruefen(spieler.getUniqueId(), klartext, true,
                System.currentTimeMillis());
        switch (ergebnis.aktion()) {
            case DURCHLASSEN -> {
                return;
            }
            case GEAENDERT -> event.message(Component.text(ergebnis.text()));
            case BLOCKIERT -> event.setCancelled(true);
        }
        if (ergebnis.verstoss() || ergebnis.hinweis() != null) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.moderator().verarbeiten(spieler, ergebnis, plugin.msgs().raw("ort.chat"), klartext));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.pruefung().vergessen(event.getPlayer().getUniqueId());
    }
}
