package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;

public class FFAEventCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public FFAEventCommand(LemonEvents plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonevents.admin.ffaevent")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }

        sender.sendMessage(MM.deserialize("<yellow>Creating FFA backup... this may take a moment."));

        // Run FAWE operations async to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getMapManager().backupFfaWorld();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(MM.deserialize(
                        "<green>FFA backup created. Event server ready.</green>"));
                } else {
                    sender.sendMessage(MM.deserialize(
                        "<red>FFA backup failed. Check console for details."));
                }
            });
        });
        return true;
    }
}
