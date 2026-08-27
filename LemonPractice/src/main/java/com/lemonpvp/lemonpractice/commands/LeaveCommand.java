package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * {@code /leave} — the universal "get me out" on the duels server. Spectating?
 * Stop watching. In a 1v1/2v2? Forfeit and go to the lobby server — exactly
 * like leaving via /lobby, /hub or /spawn: the server switch triggers the same
 * quit-forfeit path, so the opponent wins cleanly. Queued? Dequeue. Otherwise
 * it simply sends you to the lobby server.
 */
public class LeaveCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;

    public LeaveCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        UUID uuid = player.getUniqueId();

        // Watching a match → just stop watching (stay on this server).
        if (plugin.getDuelSpectateManager().isWatching(uuid)) {
            plugin.getDuelSpectateManager().leave(player, true);
            return true;
        }

        // Only in the queue → dequeue, stay here.
        boolean inMatch = plugin.getDuelManager().isInDuel(uuid)
                || plugin.getTeamDuelManager().isInTeamDuel(uuid);
        if (!inMatch && plugin.getQueueManager().isQueued(uuid)) {
            plugin.getQueueManager().removeFromQueue(uuid);
            player.sendMessage(MM.deserialize("<!italic><gray>Left the queue."));
            return true;
        }

        if (inMatch) {
            player.sendMessage(MM.deserialize("<!italic><red>Forfeiting the match…"));
        }
        // The server switch fires the quit handlers, which forfeit any running
        // match for us — identical to leaving via /lobby, /hub or /spawn.
        String lobbyServer = plugin.getServersConfig().getString("servers.lobby.name", "lobby");
        plugin.getVelocityMessaging().sendToServer(player, lobbyServer);
        return true;
    }
}
