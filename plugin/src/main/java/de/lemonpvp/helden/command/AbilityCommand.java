package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /faehigkeit} - Alternative zum Rechtsklick, praktisch auf Bedrock. */
public final class AbilityCommand extends BaseCommand {

    public AbilityCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.ability", true);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        plugin.abilities().trigger((Player) sender);
    }
}
