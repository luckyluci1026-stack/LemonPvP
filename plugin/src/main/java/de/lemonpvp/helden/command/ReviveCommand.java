package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /wiederbeleben <spieler>} - kostet ein Heldenherz. */
public final class ReviveCommand extends BaseCommand {

    public ReviveCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.revive", true);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player healer = (Player) sender;
        if (args.length < 1) {
            plugin.messages().sendText(healer, "&7/wiederbeleben <spieler>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.messages().send(healer, "general.unknown-player", "%player%", args[0]);
            return;
        }
        plugin.lives().revive(healer, target);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        // Nur gefallene Spieler vorschlagen - alles andere waere nur Rauschen.
        List<String> fallen = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.lives().isFallen(player)) {
                fallen.add(player.getName());
            }
        }
        return filter(fallen, args[0]);
    }
}
