package com.lemonpvp.lemoncore.listeners;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;

/**
 * Makes EVERY player kick fully disconnect the player from the proxy (showing the
 * kick screen) instead of being re-routed to the limbo by LemonQueue.
 *
 * <p>This covers admin kicks from any source — our {@code /gkick}, Essentials
 * {@code /kick}, vanilla {@code /kick}, plugin kicks, AFK/idle and duplicate-login
 * kicks, and maintenance kicks. Server-full handling is unaffected because that
 * goes through the login path (PlayerLoginEvent), not PlayerKickEvent.</p>
 *
 * <p>It works by cancelling the original kick and re-issuing it through
 * {@link com.lemonpvp.lemoncore.managers.ListenerManager#performKickDisconnect},
 * which signals the proxy first. Kicks we re-issue ourselves are marked as
 * pass-through so they are not intercepted again.</p>
 */
public class KickListener implements Listener {

    private final LemonCore plugin;

    public KickListener(LemonCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // A kick we issued ourselves (ban / re-issued disconnect) — let it through.
        if (plugin.getListenerManager().consumePassthrough(uuid)) {
            return;
        }

        // Re-issue as a proxy-disconnect kick so the player leaves the network.
        // Technical reasons (netty/Java exceptions, timeouts) are translated into
        // something a normal player can understand first.
        Component reason = com.lemonpvp.lemoncore.util.FriendlyErrors.translate(event.reason());
        event.setCancelled(true);
        plugin.getListenerManager().performKickDisconnect(event.getPlayer(), reason);
    }
}
