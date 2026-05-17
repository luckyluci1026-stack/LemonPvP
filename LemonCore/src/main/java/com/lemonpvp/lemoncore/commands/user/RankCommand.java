package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import net.luckperms.api.LuckPerms;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final LemonCore plugin;
    private final LuckPerms lp;

    public RankCommand(LemonCore plugin, LuckPerms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        lp.getUserManager().loadUser(player.getUniqueId()).thenAccept(user -> {
            String rank = user.getPrimaryGroup();
            player.sendMessage(plugin.getMessagesManager().get("rank.show", "rank", rank));
        });
        return true;
    }
}
