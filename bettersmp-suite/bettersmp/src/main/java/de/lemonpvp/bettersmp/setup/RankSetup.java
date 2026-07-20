package de.lemonpvp.bettersmp.setup;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Legt via LuckPerms-Befehlen die Standard-Raenge mit MiniMessage-Gradient-
 * Prefixen an. Das Nametag-/Chat-System liest diese Prefixe automatisch -
 * neue Raenge in LuckPerms funktionieren also sofort, ohne Code-Aenderung.
 *
 * Owner gibt es in 5 Looks (owner1..owner5), Admin/Mod/Sup je einen,
 * default ohne Prefix (Name weiss).
 */
public final class RankSetup {

    private record Rank(String name, int weight, String prefix) {
    }

    private static final List<Rank> RANKS = List.of(
            // default: kein Prefix, Name bleibt weiss
            new Rank("default", 1, ""),
            new Rank("sup",   300, "<gradient:#00E5FF:#0091EA>Sup</gradient> "),
            new Rank("mod",   500, "<gradient:#00FF7F:#00B34A>Mod</gradient> "),
            new Rank("admin", 800, "<gradient:#FF4D4D:#B30000>Admin</gradient> "),
            // Owner-Looks:
            new Rank("owner1", 1000, "<gradient:#FFFB00:#00FF00>Owner</gradient> "), // gelb -> gruen
            new Rank("owner2", 1000, "<gradient:#00008B:#00BFFF>Owner</gradient> "), // dunkelblau -> hellblau
            new Rank("owner3", 1000, "<gradient:#FFB300:#FF0000>Owner</gradient> "), // orange/gelb -> rot
            new Rank("owner4", 1000, "<gradient:#1E90FF:#8A2BE2>Owner</gradient> "), // blau -> lila
            new Rank("owner5", 1000, "<gradient:#40E0D0:#7CFC00>Owner</gradient> ")  // tuerkis -> hellgruen
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
            Bukkit.dispatchCommand(console, "lp group " + rank.name() + " setweight " + rank.weight());
            if (rank.prefix().isEmpty()) {
                Bukkit.dispatchCommand(console, "lp group " + rank.name() + " meta removeprefix " + rank.weight());
            } else {
                Bukkit.dispatchCommand(console, "lp group " + rank.name()
                        + " meta setprefix " + rank.weight() + " \"" + rank.prefix() + "\"");
            }
        }
        // Grundrechte fuer default
        for (String perm : List.of("essentials.spawn", "essentials.help", "essentials.msg",
                "essentials.tpa", "essentials.tpaccept", "essentials.tpdeny",
                "essentials.sethome", "essentials.home", "essentials.balance",
                "essentials.pay", "betterrtp.use", "fastshop.use", "fastshop.sell",
                "bettersmp.chat.format", "bettersmp.stats")) {
            Bukkit.dispatchCommand(console, "lp group default permission set " + perm + " true");
        }
        plugin.msgs().send(feedback, "ranks.done");
    }
}
