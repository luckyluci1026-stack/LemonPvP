package com.lemonpvp.lemonlobby.commands;

import com.lemonpvp.lemoncore.managers.PlayerData;
import com.lemonpvp.lemonlobby.LemonLobby;
import com.lemonpvp.lemonlobby.util.EconomyBridge;
import com.lemonpvp.lemonlobby.util.FormatUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlanksCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final LemonLobby plugin;

    public PlanksCommand(LemonLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        PlayerData data      = EconomyBridge.cached(player.getUniqueId());
        String     formatted = data != null ? FormatUtil.formatAmount(data.getPlanks()) : "…";
        player.sendMessage(MM.deserialize(
                "<!italic><gradient:#fffb00:#00ff00><bold>LemonPvP</bold></gradient> <dark_gray>»</dark_gray>"
                + " <gray>Your planks: <#D2691E>▬ <white>" + formatted));
        return true;
    }
}
