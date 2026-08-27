package com.lemonpvp.lemonlobby.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lemonpvp.lemonlobby.LemonLobby;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;

public class RestartManager {

    private final LemonLobby plugin;
    private BukkitTask task;

    public RestartManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleNext();
    }

    private void scheduleNext() {
        String timezone = plugin.getConfig().getString("restart.timezone", "Europe/Berlin");
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        ZonedDateTime next = now.withHour(2).withMinute(30).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        long delayMillis = Duration.between(now, next).toMillis();
        long delayTicks = delayMillis / 50L;

        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            doRestart();
            scheduleNext();
        }, delayTicks);

        plugin.getLogger().info("Next restart scheduled in " + (delayMillis / 1000 / 60) + " minutes.");
    }

    private void doRestart() {
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                "<bold><red>Server is restarting in 10 seconds!</red></bold>"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Notify all players on all servers via BungeeCord broadcast
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (!players.isEmpty()) {
                Player any = players.iterator().next();
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Message");
                out.writeUTF("ALL");
                out.writeUTF("§c§lServer is restarting now!");
                any.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        }, 200L); // 10 seconds (200 ticks)
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
