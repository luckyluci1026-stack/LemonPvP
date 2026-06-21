package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RtpCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public RtpCommand(LemonCore plugin) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>Only players can use this command."));
            return true;
        }
        player.sendMessage(MM.deserialize(
            "<yellow><bold>⚠ Under Development</bold></yellow>\n" +
            "<gray>Random Teleport (<white>/rtp</white>) wird aktuell entwickelt und ist bald verfügbar!</gray>"));
        return true;
    }
}
