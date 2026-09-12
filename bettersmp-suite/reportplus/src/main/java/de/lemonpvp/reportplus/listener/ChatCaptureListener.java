package de.lemonpvp.reportplus.listener;

import de.lemonpvp.reportplus.ReportPlus;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Faengt genau eine Chatzeile ab, wenn jemand nach der Kategorie-Wahl bei
 * /bugreport auf seinen Beschreibungstext wartet - sonst waere der Text
 * fuer alle im Chat sichtbar.
 *
 * setCancelled() muss synchron im Event selbst passieren (AsyncChatEvent
 * ist bereits verarbeitet, sobald ein spaeter geplanter Task drankommt) -
 * nur die eigentliche Verarbeitung (Speichern, Broadcast) geht auf den
 * Hauptthread.
 */
public final class ChatCaptureListener implements Listener {

    private final ReportPlus plugin;

    public ChatCaptureListener(ReportPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.bugFlow().wartetAuf(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> plugin.bugFlow().empfangeText(event.getPlayer(), text));
    }
}
