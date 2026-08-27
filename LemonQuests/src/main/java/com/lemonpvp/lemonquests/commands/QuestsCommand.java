package com.lemonpvp.lemonquests.commands;

import com.lemonpvp.lemonquests.LemonQuests;
import com.lemonpvp.lemonquests.gui.QuestGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestsCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonQuests plugin;

    public QuestsCommand(LemonQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MM.deserialize("<red>Only players can use this command.</red>"));
            return true;
        }

        if (!player.hasPermission("lemonquests.use")) {
            player.sendMessage(MM.deserialize("<red>You don't have permission to use this command.</red>"));
            return true;
        }

        new QuestGUI(plugin, player).open();
        return true;
    }
}
