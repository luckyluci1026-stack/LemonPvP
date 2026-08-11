package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.hero.Hero;
import de.lemonpvp.helden.ui.HeroMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /held [name]} - Auswahlmenue oder direkter Wechsel. */
public final class HeldCommand extends BaseCommand {

    public HeldCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.held", true);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            new HeroMenu(plugin).open(player);
            return;
        }

        Hero hero = plugin.heroes().get(args[0]);
        if (hero == null) {
            plugin.messages().send(player, "hero.unknown", "%hero%", args[0]);
            return;
        }
        plugin.heroes().trySelect(player, hero);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? filter(plugin.heroes().ids(), args[0]) : List.of();
    }
}
