package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /herzen [spieler]} */
public final class HerzenCommand extends BaseCommand {

    public HerzenCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.hearts", false);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "general.player-only");
                return;
            }
            HeldenProfile profile = plugin.profiles().getOrCreate(player);
            plugin.messages().send(player, "hearts.own", "%hearts%", profile.hearts());

            long dummySeconds = plugin.dummies().remainingSecondsFor(player.getUniqueId());
            if (dummySeconds > 0) {
                plugin.messages().send(player, "dummy.survived-self", "%seconds%", dummySeconds);
            }
            return;
        }

        HeldenProfile profile = plugin.profiles().findByName(args[0]);
        if (profile == null) {
            plugin.messages().send(sender, "general.unknown-player", "%player%", args[0]);
            return;
        }
        plugin.messages().send(sender, "hearts.other",
                "%player%", profile.name(),
                "%hearts%", profile.hearts());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : List.of();
    }
}
