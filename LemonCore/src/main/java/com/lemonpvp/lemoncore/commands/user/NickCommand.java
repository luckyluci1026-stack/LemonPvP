package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NickCommand implements CommandExecutor {

    private final LemonCore plugin;

    public NickCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.nick")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data == null) return true;

        String nick = plugin.getRandomNameUtil().generateName();
        data.setNick(nick);
        player.sendMessage(plugin.getMessagesManager().get("nick.set", "nick", nick));
        plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        return true;
    }
}
