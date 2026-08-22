package de.lemonpvp.helden.command;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.player.HeldenProfile;
import de.lemonpvp.helden.ui.PlayerListMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/** {@code /teilnehmer} - GUI fuer Spieler, Textliste fuer die Konsole. */
public final class PlayerListCommand extends BaseCommand {

    public PlayerListCommand(HeldenPlugin plugin) {
        super(plugin, "helden3.command.list", false);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (plugin.profiles().size() == 0) {
            plugin.messages().send(sender, "list.empty");
            return;
        }

        if (sender instanceof Player player) {
            new PlayerListMenu(plugin).open(player);
            return;
        }
        sendTextList(sender);
    }

    private void sendTextList(CommandSender sender) {
        plugin.messages().sendRaw(sender, "list.header",
                "%alive%", plugin.game().alive().size(),
                "%participants%", plugin.game().participants());

        for (HeldenProfile profile : plugin.profiles().ranked()) {
            plugin.messages().sendRaw(sender, "list.entry",
                    "%status%", plugin.game().statusOf(profile),
                    "%player%", profile.name(),
                    "%hearts%", profile.hearts(),
                    "%kills%", profile.kills());
        }
    }
}
