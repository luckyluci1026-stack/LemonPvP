package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemonlobby.LemonLobby;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Lobby half of {@code /eventjoin} (the JOIN button in an event broadcast): marks
 * the player as wanting to join the open hosted event and sends them to the
 * events server, where LemonEvents consumes the marker on join and adds them.
 */
public class EventJoinCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonLobby plugin;

    public EventJoinCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        UUID uuid = player.getUniqueId();
        player.sendMessage(MM.deserialize("<!italic><gradient:#fffb00:#ffa751><bold>Event</bold></gradient> "
                + "<gray>Sending you to the event..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().savePendingEventJoin(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    plugin.getLobbyMessaging().connectToServer(online,
                            plugin.getServersConfig().getString("servers.events.name", "events"));
                }
            });
        });
        return true;
    }
}
