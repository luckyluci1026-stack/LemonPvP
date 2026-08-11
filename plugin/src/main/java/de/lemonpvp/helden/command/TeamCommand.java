package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.team.HeldenTeam;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** {@code /team [info|liste|chat <text>]} */
public final class TeamCommand extends BaseCommand {

    public TeamCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.team", true);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "liste", "list" -> sendList(player);
            case "chat" -> sendTeamChat(player, args);
            default -> sendInfo(player);
        }
    }

    private void sendInfo(Player player) {
        HeldenTeam team = plugin.teams().of(player);
        if (team == null) {
            plugin.messages().send(player, "team.none");
            return;
        }
        plugin.messages().send(player, "team.info", "%team%", team.display());
    }

    private void sendList(Player player) {
        plugin.messages().sendRaw(player, "team.list-header");
        for (HeldenTeam team : plugin.teams().all()) {
            List<String> names = new ArrayList<>();
            for (HeldenProfile profile : plugin.teams().members(team.id())) {
                names.add(profile.name());
            }
            plugin.messages().sendRaw(player, "team.list-entry",
                    "%team%", team.display(),
                    "%size%", names.size(),
                    "%members%", names.isEmpty() ? "-" : String.join(", ", names));
        }
    }

    private void sendTeamChat(Player player, String[] args) {
        HeldenTeam team = plugin.teams().of(player);
        if (team == null) {
            plugin.messages().send(player, "team.none");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(player, "team.chat-empty");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        for (Player member : plugin.teams().onlineMembers(team.id())) {
            plugin.messages().sendRaw(member, "team.chat-format",
                    "%tag%", team.tag(),
                    "%player%", player.getName(),
                    "%message%", message);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? filter(List.of("info", "liste", "chat"), args[0]) : List.of();
    }
}
