package de.lemonpvp.bettersmp.join;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.List;

/**
 * Join-/Quit-Nachrichten, Erst-Join-Begruessung mit Titel und MOTD.
 */
public final class JoinModule implements Listener {

    private final BetterSMP plugin;

    public JoinModule(BetterSMP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("join-quit.enabled", true);
    }

    private String applied(Player player, String path) {
        String text = plugin.getConfig().getString(path, "");
        text = text.replace("%player%", player.getName()).replace("%brand%", plugin.brand());
        return plugin.papi().apply(player, text);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getPlayer();
        boolean first = !player.hasPlayedBefore();

        String key = first ? "join-quit.first-join" : "join-quit.join";
        event.joinMessage(Text.mm(applied(player, key)));

        if (first && plugin.getConfig().getBoolean("join-quit.first-join-title.enabled", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.showTitle(Title.title(
                        Text.mm(applied(player, "join-quit.first-join-title.title")),
                        Text.mm(applied(player, "join-quit.first-join-title.subtitle")),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3),
                                Duration.ofMillis(1000))));
            }, 20L);
        }

        if (plugin.getConfig().getBoolean("join-quit.motd.enabled", true)) {
            long delay = plugin.getConfig().getLong("join-quit.motd.delay-ticks", 15);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                List<String> lines = plugin.getConfig().getStringList("join-quit.motd.lines");
                for (String line : lines) {
                    player.sendMessage(Text.mm(plugin.papi()
                            .apply(player, line.replace("%player%", player.getName())
                                    .replace("%brand%", plugin.brand()))));
                }
            }, delay);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled()) {
            return;
        }
        event.quitMessage(Text.mm(applied(event.getPlayer(), "join-quit.quit")));
    }
}
