package com.lemonpvp.lemonlobby.managers;

import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.database.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * A "Recent Plays" hologram in the lobby, built from native Paper {@link TextDisplay}
 * entities (no external hologram plugin needed). It lists the latest duel replays
 * — written to the shared {@code lp_replays} table by LemonPractice — and refreshes
 * on an interval. Players read the names and watch with {@code /replay <name>}.
 *
 * <p>Config under {@code replay-hologram.*}; location is set in-game with
 * {@code /llobby hologram}.</p>
 */
public class ReplayHologramManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String TAG = "ll_replay_holo";

    private final LemonLobby plugin;
    private TextDisplay display;
    private BukkitTask task;

    public ReplayHologramManager(LemonLobby plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("replay-hologram.enabled", true)) return;
        Location loc = configLocation();
        if (loc == null) {
            plugin.getLogger().info("[ReplayHologram] No location set — use /llobby hologram to place it.");
            return;
        }
        spawn(loc);
        long refresh = Math.max(5, plugin.getConfig().getLong("replay-hologram.refresh-seconds", 30)) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, refresh);
    }

    public void shutdown() {
        if (task != null) { task.cancel(); task = null; }
        removeExisting();
        display = null;
    }

    /** Sets the hologram to the player's location, persists it, and respawns. */
    public void setLocation(Player player) {
        Location l = player.getLocation();
        plugin.getConfig().set("replay-hologram.enabled", true);
        plugin.getConfig().set("replay-hologram.world", l.getWorld().getName());
        plugin.getConfig().set("replay-hologram.x", l.getX());
        plugin.getConfig().set("replay-hologram.y", l.getY() + 1.6);
        plugin.getConfig().set("replay-hologram.z", l.getZ());
        plugin.saveConfig();
        shutdown();
        start();
    }

    // ── internals ────────────────────────────────────────────────────────────────

    private Location configLocation() {
        String worldName = plugin.getConfig().getString("replay-hologram.world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        if (!plugin.getConfig().contains("replay-hologram.x")) return null;
        return new Location(world,
                plugin.getConfig().getDouble("replay-hologram.x"),
                plugin.getConfig().getDouble("replay-hologram.y"),
                plugin.getConfig().getDouble("replay-hologram.z"));
    }

    private void removeExisting() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntitiesByClass(TextDisplay.class)) {
                if (e.getScoreboardTags().contains(TAG)) e.remove();
            }
        }
    }

    private void spawn(Location loc) {
        removeExisting();
        display = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.addScoreboardTag(TAG);
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setSeeThrough(true);
            td.text(loadingText());
        });
        refresh();
    }

    private Component loadingText() {
        return MM.deserialize("<gradient:#fffb00:#00ff00><bold>Recent Plays</bold></gradient>\n<gray>Loading…");
    }

    private void refresh() {
        if (display == null || !display.isValid()) return;
        int count = Math.max(1, Math.min(10, plugin.getConfig().getInt("replay-hologram.count", 5)));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Database.ReplayLite> recent = plugin.getDatabase().recentReplays(count);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (display == null || !display.isValid()) return;
                display.text(buildText(recent));
            });
        });
    }

    private Component buildText(List<Database.ReplayLite> recent) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm");
        StringBuilder sb = new StringBuilder("<gradient:#fffb00:#00ff00><bold>Recent Plays</bold></gradient>");
        if (recent.isEmpty()) {
            sb.append("\n<gray>No replays yet — go duel!");
        } else {
            int rank = 1;
            for (Database.ReplayLite r : recent) {
                sb.append("\n<gray>").append(rank++).append(". ")
                        .append("<yellow>").append(esc(r.player1()))
                        .append(" <dark_gray>vs <yellow>").append(esc(r.player2()))
                        .append(" <dark_gray>(").append(sdf.format(new Date(r.createdAt()))).append(")");
            }
            sb.append("\n<aqua>/replay <gray>to watch");
        }
        return MM.deserialize(sb.toString());
    }

    private String esc(String s) {
        return s == null ? "?" : s.replace("<", "").replace(">", "");
    }
}
