package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;

public class EndEventCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public EndEventCommand(LemonEvents plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonevents.admin.end")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Usage: /aowendevent <name>"));
            return true;
        }

        String name = args[0];
        plugin.getEventManager().endEvent(name).thenAccept(success -> {
            if (success) {
                sender.sendMessage(MM.deserialize(
                    "<green>Event <yellow>" + name + "</yellow> ended and prizes distributed."));
            } else {
                sender.sendMessage(MM.deserialize(
                    "<red>Could not end event '" + name + "'. It may not be active."));
            }
        });
        return true;
    }
}
