package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

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

        UUID uuid = player.getUniqueId();
        plugin.getDiscordLinkManager().unlinkAccount(uuid).thenAccept(removed -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return;
                if (removed) {
                    p.sendMessage(MM.deserialize("<green>Discord account unlinked.</green>"));
                } else {
                    p.sendMessage(MM.deserialize("<gray>No Discord account linked.</gray>"));
                }
            });
        });
        return true;
    }
}
