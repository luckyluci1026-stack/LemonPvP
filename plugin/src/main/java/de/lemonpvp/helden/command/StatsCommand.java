package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /stats [spieler]} */
public final class StatsCommand extends BaseCommand {

    public StatsCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.stats", false);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        HeldenProfile profile;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().send(sender, "general.player-only");
                return;
            }
            profile = plugin.profiles().getOrCreate(player);
        } else {
            profile = plugin.profiles().findByName(args[0]);
            if (profile == null) {
                plugin.messages().send(sender, "general.unknown-player", "%player%", args[0]);
                return;
            }
        }

        HeldenProfile partner = plugin.links().partnerOf(profile);
        String link = partner == null
                ? plugin.messages().raw("link.none")
                : Text.replace(plugin.messages().raw("link.partner"), "%partner%", partner.name());

        plugin.messages().sendRaw(sender, "stats.header", "%player%", profile.name());
        plugin.messages().sendRaw(sender, "stats.hearts", "%hearts%", profile.hearts());
        plugin.messages().sendRaw(sender, "stats.link", "%link%", link);
        plugin.messages().sendRaw(sender, "stats.kills", "%kills%", profile.kills());
        plugin.messages().sendRaw(sender, "stats.deaths", "%deaths%", profile.pvpDeaths());
        plugin.messages().sendRaw(sender, "stats.natural", "%natural%", profile.naturalDeaths());
        plugin.messages().sendRaw(sender, "stats.status", "%status%", plugin.game().statusOf(profile));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : List.of();
    }
}
