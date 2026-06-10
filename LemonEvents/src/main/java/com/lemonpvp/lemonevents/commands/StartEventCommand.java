package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;

public class StartEventCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public StartEventCommand(LemonEvents plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonevents.admin.start")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(MM.deserialize("<red>Usage: /aowstartevent <name>"));
            return true;
        }

        String name = args[0];
        plugin.getEventManager().startEvent(name).thenAccept(success ->
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(MM.deserialize(
                        "<green>Event <yellow>" + name + "</yellow> started!"));
                } else {
                    sender.sendMessage(MM.deserialize(
                        "<red>Could not start event '" + name + "'. It may not exist or is not in WAITING status."));
                }
            }));
        return true;
    }
}
