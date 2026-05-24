package com.lemonpvp.lemonevents.commands;

import com.lemonpvp.lemonevents.LemonEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;

public class EventFFACommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonEvents plugin;

    public EventFFACommand(LemonEvents plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lemonevents.admin.eventffa")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }

        sender.sendMessage(MM.deserialize("<yellow>Looking up FFA backup..."));

        plugin.getDatabase().loadFfaBackupPath().thenAccept(backupPath -> {
            if (backupPath == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MM.deserialize("<red>No FFA backup found in database.")));
                return;
            }

            sender.sendMessage(MM.deserialize("<yellow>Restoring FFA from backup: " + backupPath));

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean success = plugin.getMapManager().restoreFfaWorld(backupPath);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(MM.deserialize(
                            "<green>FFA server restored. Backup deleted.</green>"));
                    } else {
                        sender.sendMessage(MM.deserialize(
                            "<red>FFA restore failed. Check console for details."));
                    }
                });
            });
        });
        return true;
    }
}
