package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Handles the server list MOTD and favicon.
 *
 * <p>The MOTD uses MiniMessage gradients and Unicode "item" symbols
 * (⚔ ♦ ✦ ★ ☆) — no texture pack required. Both lines are configurable
 * under {@code motd.*} in config.yml; the defaults give a professional
 * FlowPvP/FoxPvP style appearance.
 *
 * <p>Player count can be offset so the list always shows "slots" available
 * rather than the hard maximum ({@code motd.max-players-offset}).
 */
public class MotdManager implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonCore plugin;
    private CachedServerIcon favicon;

    public MotdManager(LemonCore plugin) {
        this.plugin = plugin;
        loadFavicon();
    }

    private void loadFavicon() {
        File file = new File(plugin.getDataFolder(), "server-icon.png");
        if (!file.exists()) file = new File("server-icon.png");
        if (!file.exists()) return;
        try {
            BufferedImage img = ImageIO.read(file);
            favicon = plugin.getServer().loadServerIcon(img);
        } catch (Exception e) {
            plugin.getLogger().warning("[MotdManager] Could not load favicon: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (!plugin.getConfig().getBoolean("motd.enabled", true)) return;

        String line1 = plugin.getConfig().getString("motd.line1",
                "<gradient:#fffb00:#00ff00><bold>⚔ LᴇᴍᴏɴPᴠP ⚔</bold></gradient>");
        String line2 = plugin.getConfig().getString("motd.line2",
                "<gray>♦ Practice Network  <yellow>✦  <white>play.lemonpvp.de");

        Component motd = MM.deserialize(line1)
                .append(Component.newline())
                .append(MM.deserialize(line2));

        event.motd(motd);

        int offset = plugin.getConfig().getInt("motd.max-players-offset", 0);
        if (offset != 0) {
            event.setMaxPlayers(event.getNumPlayers() + offset);
        }

        if (favicon != null) {
            event.setServerIcon(favicon);
        }
    }
}
