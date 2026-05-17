package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HideCommand implements CommandExecutor {

    private final LemonCore plugin;

    public HideCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("lemoncore.use.hide")) {
            player.sendMessage(plugin.getMessagesManager().get("no-permission"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.getMessagesManager().get("invalid-usage", "usage", "/hide <name>"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        if (data == null) return true;

        // Strip MiniMessage tags unless they have permission
        String name = player.hasPermission("lemoncore.use.minimessage")
                ? String.join(" ", args)
                : TextUtil.escapeTags(String.join(" ", args));

        if (name.length() > 32) name = name.substring(0, 32);

        data.setHiddenName(name);
        player.sendMessage(plugin.getMessagesManager().get("hide.set", "name", name));
        plugin.getPlayerDataManager().savePlayer(player.getUniqueId());
        return true;
    }
}
