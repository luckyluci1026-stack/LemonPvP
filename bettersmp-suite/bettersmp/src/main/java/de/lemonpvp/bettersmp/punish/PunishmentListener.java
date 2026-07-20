package de.lemonpvp.bettersmp.punish;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.storage.Punishment;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Setzt Bans (beim Login) und Mutes (im Chat) durch.
 */
public final class PunishmentListener implements Listener {

    private final BetterSMP plugin;

    public PunishmentListener(BetterSMP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("punishments.enabled", true);
    }

    /** Ban-Pruefung noch vor dem Join (auch bedrock-freundlich - der Client
     *  zeigt den Disconnect-Screen an). Laeuft bereits auf einem Async-Thread. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled()) {
            return;
        }
        Punishment ban = plugin.database().getBanBlocking(event.getUniqueId());
        if (ban != null && !ban.isExpired()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    plugin.punishments().banScreen(ban));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!enabled()) {
            return;
        }
        Punishment mute = plugin.punishments().activeMute(event.getPlayer().getUniqueId());
        if (mute != null) {
            event.setCancelled(true);
            plugin.punishments().sendMuteNotice(event.getPlayer(), mute);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled()) {
            plugin.punishments().loadMute(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.punishments().unloadMute(event.getPlayer().getUniqueId());
    }
}
