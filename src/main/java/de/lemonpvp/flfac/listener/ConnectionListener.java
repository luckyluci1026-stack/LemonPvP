package de.lemonpvp.flfac.listener;

import de.lemonpvp.flfac.FLFAC;
import de.lemonpvp.flfac.data.PlayerData;
import de.lemonpvp.flfac.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Tracks player join/quit to set up and tear down {@link PlayerData}. */
public final class ConnectionListener implements Listener {

    private final FLFAC plugin;

    public ConnectionListener(FLFAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getOrCreate(player);

        if (player.hasPermission("flfac.alerts")) {
            data.setAlertsEnabled(plugin.getConfigManager().isAlertsEnabledByDefault());
            player.sendMessage(plugin.getAlertManager().prefix()
                    .append(ColorUtil.gradientRaw(
                            "Alerts " + (data.isAlertsEnabled() ? "enabled" : "disabled")
                                    + " - this server is protected.",
                            plugin.getConfigManager().getAccentGradient())));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().isResetOnQuit()) {
            plugin.getPunishmentManager().clear(player.getUniqueId());
        }
        plugin.getPlayerDataManager().remove(player);
    }
}
