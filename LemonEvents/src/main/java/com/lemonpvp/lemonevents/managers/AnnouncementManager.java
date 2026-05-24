package com.lemonpvp.lemonevents.managers;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.EventStatus;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class AnnouncementManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonEvents plugin;
    private BukkitTask task;

    public AnnouncementManager(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long intervalTicks = plugin.getConfig().getLong("announcement-interval", 180) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::announce, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void announce() {
        List<GameEvent> waiting = plugin.getEventManager().getEventsByStatus(EventStatus.WAITING);
        if (waiting.isEmpty()) return;

        for (GameEvent event : waiting) {
            Component announcement = buildAnnouncement(event);
            // Broadcast to all players on this server
            Bukkit.broadcast(announcement);
            // Also broadcast via BungeeCord channel to all servers
            plugin.getMessaging().broadcastToAll(buildLegacyAnnouncement(event));
        }
    }

    private Component buildAnnouncement(GameEvent event) {
        // "<bold><gradient:#fffb00:#00ff00><EventName></gradient></bold>
        //  <green>[join]</green>"  where [join] runs /joinevent <EventName>
        Component name = MM.deserialize(
                "<bold><gradient:#fffb00:#00ff00>" + event.getName() + "</gradient></bold>");
        Component join = MM.deserialize("<green>[join]</green>")
                .clickEvent(ClickEvent.runCommand("/joinevent " + event.getName()));
        return name.append(Component.text(" ")).append(join);
    }

    private String buildLegacyAnnouncement(GameEvent event) {
        // Plain legacy fallback for cross-server BungeeCord broadcast
        return "§6§l" + event.getName() + " §a[§f/joinevent " + event.getName() + "§a]";
    }
}
