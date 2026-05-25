package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RestartManager {

    private final LemonCore plugin;
    private boolean restarting = false;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public RestartManager(LemonCore plugin) {
        this.plugin = plugin;
    }

    public boolean isRestarting() { return restarting; }

    public void beginRestart() {
        if (restarting) return;
        restarting = true;

        // T+0: 5 minutes
        schedule(0, () -> broadcastMsg("restart.five-min"));

        // T+120s: 3 minutes
        schedule(120 * 20L, () -> broadcastMsg("restart.three-min"));

        // T+240s: 1 minute
        schedule(240 * 20L, () -> broadcastMsg("restart.one-min"));

        // T+270s: 30 seconds
        schedule(270 * 20L, () -> broadcastMsg("restart.thirty-sec"));

        // T+285s: 15 seconds - title warning
        schedule(285 * 20L, () -> broadcastTitle("restart.warning-15"));

        // T+290s: 10 seconds - title warning
        schedule(290 * 20L, () -> broadcastTitle("restart.warning-10"));

        // T+291-299: countdown 9..1
        for (int i = 9; i >= 1; i--) {
            final int secs = i;
            schedule((300 - secs) * 20L, () -> broadcastCountdown(secs));
        }

        // T+300s: restart
        schedule(300 * 20L, this::executeRestart);
    }

    private void schedule(long delayTicks, Runnable task) {
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    private void broadcastMsg(String msgKey) {
        Component msg = plugin.getMessagesManager().get(msgKey);
        Bukkit.broadcast(msg);
    }

    private void broadcastTitle(String msgKey) {
        String raw = plugin.getMessagesManager().getRaw(msgKey);
        String[] parts = raw.split("\n", 2);
        Component title = TextUtil.parse(parts[0]);
        Component subtitle = parts.length > 1 ? TextUtil.parse(parts[1]) : Component.empty();
        Title t = Title.title(title, subtitle, Title.Times.times(
                Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(t);
            p.sendMessage(title);
        }
    }

    private void broadcastCountdown(int seconds) {
        String raw = plugin.getMessagesManager().getRaw("restart.countdown")
                .replace("{seconds}", String.valueOf(seconds));
        String[] parts = raw.split("\n", 2);
        Component title = TextUtil.parse(parts[0]);
        Component subtitle = parts.length > 1 ? TextUtil.parse(parts[1]) : Component.empty();
        Title t = Title.title(title, subtitle, Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(0)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(t);
        }
    }

    private void executeRestart() {
        // Broadcast restart message
        Component msg = plugin.getMessagesManager().get("restart.now-chat");
        Bukkit.broadcast(msg);

        Component titleComp = plugin.getMessagesManager().get("restart.now-title");
        Title title = Title.title(titleComp, Component.empty(), Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500)));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        // Move players to limbo via plugin messaging, then restart server
        String limbo = plugin.getServersConfig().getString("servers.limbo.name", "limbo");
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getVelocityMessaging().sendToServer(p, limbo);
        }

        // Restart server after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.spigot().restart();
        }, 60L);
    }

    public void cancel() {
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        restarting = false;
    }
}
