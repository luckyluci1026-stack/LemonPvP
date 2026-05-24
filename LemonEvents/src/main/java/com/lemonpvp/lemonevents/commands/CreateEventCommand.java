package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.EventType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.*;

public class CreateEventCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public CreateEventCommand(LemonEvents plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonevents.admin.create")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage(MM.deserialize(
                "<red>Usage: /aowcreateevent <name> <type> <prize3> <prize2> <prize1>"));
            sender.sendMessage(MM.deserialize(
                "<gray>Types: LemonRoyale | HungerGames | Cinema | PvP | Horror"));
            return true;
        }

        String name = args[0];
        EventType type = EventType.fromString(args[1]);
        if (type == null) {
            sender.sendMessage(MM.deserialize(
                "<red>Unknown event type. Valid: LemonRoyale | HungerGames | Cinema | PvP | Horror"));
            return true;
        }

        int prize3, prize2, prize1;
        try {
            prize3 = Integer.parseInt(args[2]);
            prize2 = Integer.parseInt(args[3]);
            prize1 = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MM.deserialize("<red>Prizes must be integers."));
            return true;
        }

        if (plugin.getEventManager().getEvent(name) != null) {
            sender.sendMessage(MM.deserialize("<red>Event '" + name + "' already exists."));
            return true;
        }

        plugin.getEventManager().createEvent(name, type, prize3, prize2, prize1)
                .thenAccept(event -> {
                    if (event == null) {
                        sender.sendMessage(MM.deserialize("<red>Failed to create event. Check console."));
                    } else {
                        sender.sendMessage(MM.deserialize(
                            "<green>Event <yellow>" + name + "</yellow> (<gold>" + type.name() +
                            "</gold>) created! Prizes: <gold>" + prize1 + "</gold> / <gold>" +
                            prize2 + "</gold> / <gold>" + prize3 + "</gold> coins."));
                    }
                });
        return true;
    }
}
