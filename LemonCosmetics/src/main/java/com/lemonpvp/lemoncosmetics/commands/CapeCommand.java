package com.lemonpvp.lemoncosmetics.commands;

import com.lemonpvp.lemoncosmetics.LemonCosmetics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /cape}:
 * <ul>
 *   <li>{@code /cape} or {@code /cape list} — list uploaded capes</li>
 *   <li>{@code /cape <name>} — wear a cape</li>
 *   <li>{@code /cape off} — remove your cape</li>
 *   <li>{@code /cape reload} — re-apply your cape with current config placement (tuning)</li>
 * </ul>
 * Capes are files in {@code plugins/LemonCosmetics/capes/} (png/gif/jpg), rendered
 * on a map-holding display entity — no resource pack.
 */
public class CapeCommand implements CommandExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX =
            "<gradient:#ffe259:#ffa751><bold>Cape</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonCosmetics plugin;

    public CapeCommand(LemonCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("lemoncosmetics.cape")) {
            msg(player, "<red>You don't have permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listCapes(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "off", "remove", "none" -> {
                plugin.getCapeManager().unequip(player.getUniqueId());
                msg(player, "<gray>Cape removed.");
            }
            case "reload" -> {
                String current = plugin.getCapeManager().currentCape(player.getUniqueId());
                if (current == null) { msg(player, "<gray>You aren't wearing a cape."); return true; }
                plugin.reloadConfig();
                if (plugin.getCapeManager().equip(player, current)) {
                    msg(player, "<green>Re-applied <yellow>" + current + " <green>with current placement.");
                } else {
                    msg(player, "<red>Could not re-apply <yellow>" + current + "<red>.");
                }
            }
            default -> {
                String name = args[0];
                if (plugin.getCapeManager().equip(player, name)) {
                    msg(player, "<green>Now wearing <yellow>" + name + "<green>.");
                } else {
                    msg(player, "<red>Cape <yellow>" + name + " <red>not found or unreadable. Try <white>/cape list<red>.");
                }
            }
        }
        return true;
    }

    private void listCapes(Player player) {
        List<String> capes = plugin.getCapeManager().listCapes();
        if (capes.isEmpty()) {
            msg(player, "<gray>No capes uploaded yet. Drop a <white>128x64 png/gif <gray>into "
                    + "<white>plugins/LemonCosmetics/capes/<gray>.");
            return;
        }
        msg(player, PREFIX + "<white>Available capes <gray>(" + capes.size() + "):");
        StringBuilder sb = new StringBuilder();
        for (String c : capes) {
            sb.append("<click:run_command:'/cape ").append(c).append("'>")
              .append("<hover:show_text:'<green>Click to wear'><yellow>").append(c).append("</yellow></hover></click>  ");
        }
        player.sendMessage(MM.deserialize("<!italic>" + sb));
        msg(player, "<gray>/cape off <dark_gray>— remove");
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
