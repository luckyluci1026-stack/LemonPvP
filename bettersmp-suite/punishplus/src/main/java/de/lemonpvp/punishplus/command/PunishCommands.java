package de.lemonpvp.punishplus.command;

import de.lemonpvp.punishplus.PunishManager;
import de.lemonpvp.punishplus.PunishPlus;
import de.lemonpvp.punishplus.util.Durations;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /offend und /punish - gleiche Syntax, ein Befehl je Grund-ID, die
 * Grund-ID entscheidet ueber die Dauer bei /offend (bei /punish ist es
 * immer dauerhaft, siehe PunishManager).
 */
public final class PunishCommands implements TabExecutor {

    private final PunishPlus plugin;

    public PunishCommands(PunishPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        boolean istPunish = command.getName().equalsIgnoreCase("punish");
        String benoetigtesRecht = istPunish ? "punishplus.punish" : "punishplus.offend";
        if (!sender.hasPermission(benoetigtesRecht)) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msgs().format(istPunish ? "punish.usage" : "offend.usage"));
            return true;
        }
        Player ziel = Bukkit.getPlayerExact(args[0]);
        if (ziel == null) {
            plugin.msgs().send(sender, "not-online", "spieler", args[0]);
            return true;
        }
        String grundId = args[1];
        String ausfuehrer = sender instanceof Player p ? p.getName() : "Konsole";

        PunishManager.Ergebnis ergebnis = istPunish
                ? plugin.manager().punish(ziel, grundId, ausfuehrer)
                : plugin.manager().offend(ziel, grundId, ausfuehrer);

        switch (ergebnis) {
            case OK -> {
                if (istPunish) {
                    plugin.msgs().send(sender, "punish.success", "spieler", ziel.getName(), "grund", grundId);
                } else {
                    String dauer = plugin.gruende().get(grundId)
                            .map(g -> Durations.humanize(g.offendDauerMillis())).orElse("?");
                    plugin.msgs().send(sender, "offend.success",
                            "spieler", ziel.getName(), "grund", grundId, "dauer", dauer);
                }
            }
            case AUSGENOMMEN -> plugin.msgs().send(sender, "exempt", "spieler", ziel.getName());
            case UNBEKANNTER_GRUND -> plugin.msgs().send(sender, "unknown-reason",
                    "grund", grundId, "liste", plugin.gruende().liste());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2) {
            return List.of(plugin.gruende().liste().split(", "));
        }
        return List.of();
    }
}
