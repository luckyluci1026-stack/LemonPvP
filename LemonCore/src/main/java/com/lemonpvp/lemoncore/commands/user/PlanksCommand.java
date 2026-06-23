package com.lemonpvp.lemoncore.commands.user;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemoncore.util.TextUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlanksCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonCore plugin;

    public PlanksCommand(LemonCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        PlayerData data = plugin.getPlayerDataManager().getCached(player.getUniqueId());
        String formatted = data != null
                ? TextUtil.formatCoins(data.getPlanks())
                : "…";
        player.sendMessage(MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray>"
                + " <gray>Deine Planks: <#D2691E>▬ <white>" + formatted));
        return true;
    }
}
