package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /freeze <Spieler> - einfrieren oder wieder freigeben, je nach Zustand.
 *
 * Der Gedanke dahinter: bevor ein /netban ausgesprochen wird, will man
 * oft erst reden - und genau in dem Moment neigt die betroffene Person
 * dazu, wegzulaufen oder die Verbindung zu trennen. Eingefroren bleibt
 * sie stehen und kann trotzdem antworten.
 */
public final class FreezeCommand implements TabExecutor {

    private final BetterSMP plugin;

    public FreezeCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bettersmp.freeze")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.msgs().send(sender, "freeze.usage");
            return true;
        }
        Player ziel = Bukkit.getPlayerExact(args[0]);
        if (ziel == null) {
            plugin.msgs().send(sender, "freeze.not-online", "spieler", args[0]);
            return true;
        }
        boolean jetztEingefroren = plugin.freeze().umschalten(ziel.getUniqueId());
        String von = sender instanceof Player p ? p.getName() : "Konsole";

        if (jetztEingefroren) {
            plugin.msgs().send(sender, "freeze.done", "spieler", ziel.getName());
            plugin.msgs().send(ziel, "freeze.frozen-notice", "von", von);
        } else {
            plugin.msgs().send(sender, "freeze.undone", "spieler", ziel.getName());
            plugin.msgs().send(ziel, "freeze.unfrozen-notice");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || !sender.hasPermission("bettersmp.freeze")) {
            return List.of();
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
