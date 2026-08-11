package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /leben [spieler]} */
public final class LivesCommand extends BaseCommand {

    public LivesCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.lives", false);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!plugin.settings().livesEnabled()) {
            plugin.messages().send(sender, "lives.disabled");
            return;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "general.player-only");
                return;
            }
            plugin.messages().send(player, "lives.current",
                    "%lives%", plugin.profiles().getOrCreate(player).lives());
            return;
        }

        HeldenProfile profile = plugin.profiles().findByName(args[0]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[0]);
            return;
        }
        plugin.messages().send(sender, "lives.other",
                "%player%", profile.name(),
                "%lives%", profile.lives());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : List.of();
    }
}
