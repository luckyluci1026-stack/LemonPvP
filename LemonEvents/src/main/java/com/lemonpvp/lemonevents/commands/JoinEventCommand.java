package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.GameEvent;
import com.lemonpvp.lemonevents.model.EventStatus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class JoinEventCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public JoinEventCommand(LemonEvents plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can join events.");
            return true;
        }
        if (!player.hasPermission("lemonevents.use.join")) {
            player.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(MM.deserialize("<red>Usage: /joinevent <name>"));
            return true;
        }

        String name = args[0];
        GameEvent event = plugin.getEventManager().getEvent(name);
        if (event == null) {
            player.sendMessage(MM.deserialize("<red>Event '" + name + "' not found."));
            return true;
        }
        if (event.getStatus() != EventStatus.WAITING) {
            player.sendMessage(MM.deserialize("<red>This event is not currently open for joining."));
            return true;
        }

        boolean added = plugin.getEventManager().addToWaiting(player.getUniqueId(), name);
        if (added) {
            player.sendMessage(MM.deserialize(
                "<green>You joined the waiting list for <yellow>" + name + "</yellow>! " +
                "Stand by for the event to start."));
        } else {
            player.sendMessage(MM.deserialize(
                "<yellow>You are already in the waiting list for <white>" + name + "</white>."));
        }
        return true;
    }
}
