package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /eventjoin} — join the open hosted event. On the events server this is
 * a direct join; the same command name is registered on the lobby to do the
 * cross-server handoff, so the JOIN broadcast works from anywhere on the network.
 */
public class EventJoinCommand implements CommandExecutor {

    private final LemonEvents plugin;

    public EventJoinCommand(LemonEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        plugin.getHostedEventManager().join(player);
        return true;
    }
}
