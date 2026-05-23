package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlinkCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonCore plugin;

    public UnlinkCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("lemoncore.use.unlink")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission.</red>"));
            return true;
        }

        plugin.getDiscordLinkManager().unlinkAccount(player.getUniqueId()).thenAccept(removed -> {
            if (removed) {
                player.sendMessage(MM.deserialize("<green>Discord account unlinked.</green>"));
            } else {
                player.sendMessage(MM.deserialize("<gray>No Discord account linked.</gray>"));
            }
        });
        return true;
    }
}
