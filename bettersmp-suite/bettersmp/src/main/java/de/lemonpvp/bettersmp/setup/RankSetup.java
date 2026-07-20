package de.lemonpvp.bettersmp.setup;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Legt via LuckPerms-Befehlen sinnvolle Standard-Raenge mit Prefixen und
 * Gewichtungen an (default, vip, mod, admin, owner). Idempotent - erneutes
 * Ausfuehren aktualisiert nur Prefix/Weight.
 */
public final class RankSetup {

    private record Rank(String name, int weight, String prefix) {
    }

    private static final List<Rank> RANKS = List.of(
            new Rank("default", 1, "&7[Spieler] "),
            new Rank("vip", 50, "&e[VIP] "),
            new Rank("mod", 100, "&2[Mod] "),
            new Rank("admin", 500, "&c[Admin] "),
            new Rank("owner", 1000, "&4[Owner] ")
    );

    private final BetterSMP plugin;

    public RankSetup(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public void run(CommandSender feedback) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.msgs().send(feedback, "ranks.no-luckperms");
            return;
        }
        plugin.msgs().send(feedback, "ranks.started");
        var console = Bukkit.getConsoleSender();
        for (Rank rank : RANKS) {
            if (!rank.name().equals("default")) {
                Bukkit.dispatchCommand(console, "lp creategroup " + rank.name());
            }
            Bukkit.dispatchCommand(console,
                    "lp group " + rank.name() + " setweight " + rank.weight());
            Bukkit.dispatchCommand(console,
                    "lp group " + rank.name() + " meta setprefix " + rank.weight()
                            + " \"" + rank.prefix() + "\"");
        }
        // Grundlegende Permissions fuer default (damit EssentialsX-Basics gehen)
        for (String perm : List.of("essentials.spawn", "essentials.help", "essentials.msg",
                "essentials.tpa", "essentials.tpaccept", "essentials.tpdeny",
                "essentials.sethome", "essentials.home", "essentials.balance",
                "essentials.pay", "betterrtp.use", "fastshop.use", "bettersmp.chat.format")) {
            Bukkit.dispatchCommand(console, "lp group default permission set " + perm + " true");
        }
        plugin.msgs().send(feedback, "ranks.done");
    }
}
