package de.lemonpvp.bettersmp.command;

import de.lemonpvp.bettersmp.BetterSMP;
import de.lemonpvp.bettersmp.report.Ziel;
import de.lemonpvp.bettersmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * /report <Spieler> <Grund> - fuer alle, nicht nur fuers Team.
 *
 * Auf einem Schulserver ist das der Weg, wie eine gemobbte Klasse 8b
 * ohne Umweg ueber Discord jemanden erreicht, der gerade online ist.
 * Das Team bekommt die Meldung live, alle anderen sehen nichts davon -
 * eine oeffentliche Anschuldigung waere selbst schon ein Problem.
 */
public final class ReportCommand implements TabExecutor {

    private final BetterSMP plugin;

    public ReportCommand(BetterSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player melder)) {
            plugin.msgs().send(sender, "players-only");
            return true;
        }
        if (!melder.hasPermission("bettersmp.report")) {
            plugin.msgs().send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.msgs().send(melder, "report.usage");
            return true;
        }
        Ziel gemeldet = plugin.reports().finde(melder, args[0]).orElse(null);
        if (gemeldet == null) {
            plugin.msgs().send(melder, "report.unknown", "spieler", Text.sicher(args[0]));
            return true;
        }
        if (gemeldet.id().equals(melder.getUniqueId())) {
            plugin.msgs().send(melder, "report.not-yourself");
            return true;
        }
        long wartezeit = plugin.reports().verbleibendeSekunden(melder.getUniqueId());
        if (wartezeit > 0) {
            plugin.msgs().send(melder, "report.cooldown", "sekunden", String.valueOf(wartezeit));
            return true;
        }

        String grund = String.join(" ", java.util.Arrays.asList(args).subList(1, args.length));
        plugin.reports().vermerken(melder, gemeldet, grund);
        plugin.msgs().send(melder, "report.sent", "spieler", gemeldet.name());
        Bukkit.getPluginManager().callEvent(new de.lemonpvp.bettersmp.api.SpielerGemeldetEvent(
                melder.getUniqueId(), melder.getName(), gemeldet.id(), gemeldet.name(), grund));

        for (Player teammitglied : Bukkit.getOnlinePlayers()) {
            if (teammitglied.hasPermission("bettersmp.report.receive")) {
                plugin.msgs().send(teammitglied, "report.received",
                        "melder", melder.getName(), "spieler", gemeldet.name(),
                        "grund", Text.sicher(grund));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String anfang = args[0].toLowerCase(Locale.ROOT);
        Set<String> namen = new LinkedHashSet<>();
        if (sender instanceof Player melder) {
            for (Ziel ziel : plugin.reports().letzteGegner().letzte(melder.getUniqueId())) {
                namen.add(ziel.name());
            }
        }
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            if (!spieler.equals(sender)) {
                namen.add(spieler.getName());
            }
        }
        List<String> passend = new ArrayList<>();
        for (String name : namen) {
            if (name.toLowerCase(Locale.ROOT).startsWith(anfang)) {
                passend.add(name);
            }
        }
        return passend;
    }
}
