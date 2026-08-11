package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.team.HeldenTeam;
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

        Hero hero = plugin.heroes().of(profile);
        HeldenTeam team = plugin.teams().of(profile);

        plugin.messages().sendRaw(sender, "stats.header", "%player%", profile.name());
        plugin.messages().sendRaw(sender, "stats.hero", "%hero%", hero == null ? "&7-" : hero.display());
        plugin.messages().sendRaw(sender, "stats.team", "%team%", team == null ? "&7-" : team.display());
        plugin.messages().sendRaw(sender, "stats.lives", "%lives%", profile.lives());
        plugin.messages().sendRaw(sender, "stats.kills", "%kills%", profile.kills());
        plugin.messages().sendRaw(sender, "stats.deaths", "%deaths%", profile.deaths());
        plugin.messages().sendRaw(sender, "stats.assists", "%assists%", profile.assists());
        plugin.messages().sendRaw(sender, "stats.kd", "%kd%", profile.kd());
        plugin.messages().sendRaw(sender, "stats.streak", "%best%", profile.bestKillStreak());
        plugin.messages().sendRaw(sender, "stats.coins",
                "%currency%", plugin.economy().currencyName(),
                "%coins%", profile.coins());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlinePlayerNames(args[0]) : List.of();
    }
}
