package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * "Golden Hour" — a server-wide event during which the apple tree yields
 * multiplied apples. Announced with a title, sound and a live bossbar countdown
 * shown to everyone. Can fire automatically on an interval or be triggered by an
 * admin. Reads {@code golden-hour.*} from config.
 */
public class GoldenHourManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX =
            "<gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonLobby plugin;

    private volatile double multiplier = 1.0;
    private volatile long startedAt = 0L;
    private volatile long endsAt = 0L;
    private BossBar bar;

    public GoldenHourManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return System.currentTimeMillis() < endsAt;
    }

    /** The current apple multiplier (1.0 when no Golden Hour is running). */
    public double getMultiplier() {
        return isActive() ? multiplier : 1.0;
    }

    // ── Control ─────────────────────────────────────────────────────────────────

    public void start(double mult, int seconds) {
        if (seconds <= 0 || mult <= 1.0) return;
        this.multiplier = mult;
        this.startedAt = System.currentTimeMillis();
        this.endsAt = startedAt + seconds * 1000L;

        if (bar == null) {
            bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
        }
        bar.name(buildName(seconds * 1000L));
        bar.progress(1f);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bar);
            p.showTitle(Title.title(
                    MM.deserialize("<gradient:#fffb00:#ff8f00><bold>🍎 GOLDEN HOUR 🍎"),
                    MM.deserialize("<yellow>" + fmtMult(mult) + "x apples for " + (Math.max(1, seconds / 60)) + " min!"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        }
        Bukkit.broadcast(MM.deserialize(PREFIX + "<gold>Golden Hour started! <yellow>" + fmtMult(mult)
                + "x <gold>apples for " + Math.max(1, seconds / 60) + " minutes — harvest the tree!"));
    }

    /** Called every second from LemonLobby's timer. */
    public void tick() {
        if (bar == null) return;
        if (!isActive()) {
            if (endsAt != 0L) {            // just ended this tick
                endsAt = 0L;
                multiplier = 1.0;
                for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
                Bukkit.broadcast(MM.deserialize(PREFIX + "<gray>Golden Hour has ended."));
            }
            return;
        }
        long remaining = endsAt - System.currentTimeMillis();
        long total = endsAt - startedAt;
        bar.name(buildName(remaining));
        bar.progress(Math.max(0f, Math.min(1f, (float) remaining / total)));
    }

    /** Show the bossbar to a player who joins mid-event. */
    public void show(Player p) {
        if (isActive() && bar != null) p.showBossBar(bar);
    }

    public void stopAndCleanup() {
        endsAt = 0L;
        multiplier = 1.0;
        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private Component buildName(long remainingMs) {
        long m = remainingMs / 60_000, s = (remainingMs / 1000) % 60;
        String t = m > 0 ? m + "m " + s + "s" : s + "s";
        return MM.deserialize("<gradient:#fffb00:#ff8f00><bold>🍎 GOLDEN HOUR</bold></gradient> "
                + "<yellow>" + fmtMult(multiplier) + "x apples <gold>▸ <white>" + t);
    }

    private String fmtMult(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
    }
}
