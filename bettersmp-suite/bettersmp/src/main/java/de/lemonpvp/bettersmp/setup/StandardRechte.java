package de.lemonpvp.bettersmp.setup;

import de.lemonpvp.bettersmp.BetterSMP;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class StandardRechte {

    static final List<String> ERLAUBEN = List.of(
            "voicechat.listen", "voicechat.speak", "voicechat.groups",
            "bettersmp.homes", "bettersmp.stats", "bettersmp.report", "bettersmp.bugreport", "bettersmp.tutorial",
            "bettersmp.daily", "bettersmp.spawn",
            "betterrtp.use", "fastshop.use", "fastshop.sell", "fastshop.worth", "fastshop.ah",
            "duelplus.use", "smplobby.spawn",
            "essentials.spawn", "essentials.help", "essentials.list",
            "essentials.tpa", "essentials.tpahere", "essentials.tpaccept", "essentials.tpdeny", "essentials.tpacancel",
            "essentials.balance", "essentials.balance.others", "essentials.baltop", "essentials.pay");

    static final List<String> VERBIETEN = List.of("essentials.home", "essentials.sethome", "essentials.delhome", "essentials.msg");

    static final List<String> ERBEN = List.of("sup", "mod", "admin", "owner1", "owner2", "owner3", "owner4", "owner5");

    static final List<String> TEAM = List.of("bettersmp.xray", "bettersmp.xray.alarm");

    private final BetterSMP plugin;

    public StandardRechte(BetterSMP plugin) {
        this.plugin = plugin;
    }

    public void anwenden(CommandSender rueckmeldung) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("standard-rechte.enabled", true)) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            if (rueckmeldung != null) {
                plugin.msgs().send(rueckmeldung, "ranks.no-luckperms");
            }
            return;
        }
        String gruppe = config.getString("standard-rechte.gruppe", "default");
        try {
            Bridge.anwenden(plugin, rueckmeldung, gruppe, liste(config, "standard-rechte.erlauben", ERLAUBEN),
                    liste(config, "standard-rechte.verbieten", VERBIETEN), liste(config, "standard-rechte.erben", ERBEN),
                    liste(config, "standard-rechte.team", TEAM));
        } catch (LinkageError | RuntimeException fehler) {
            plugin.getLogger().warning("Standard-Rechte konnten nicht gesetzt werden: " + fehler.getMessage());
        }
    }

    static List<String> liste(FileConfiguration config, String pfad, List<String> standard) {
        return config.isList(pfad) ? config.getStringList(pfad) : standard;
    }

    static final class Bridge {

        private Bridge() {
        }

        static void anwenden(BetterSMP plugin, CommandSender rueckmeldung, String gruppe,
                             List<String> erlauben, List<String> verbieten, List<String> erben, List<String> team) {
            LuckPerms luckPerms = LuckPermsProvider.get();
            GroupManager gruppen = luckPerms.getGroupManager();
            AtomicInteger geaendert = new AtomicInteger();
            List<String> respektiert = Collections.synchronizedList(new ArrayList<>());
            gruppen.modifyGroup(gruppe, g -> {
                for (String recht : erlauben) {
                    if (setzen(g, recht, true, respektiert)) {
                        geaendert.incrementAndGet();
                    }
                }
                for (String recht : verbieten) {
                    if (setzen(g, recht, false, respektiert)) {
                        geaendert.incrementAndGet();
                    }
                }
            }).whenComplete((nichts, fehler) -> {
                if (fehler != null) {
                    plugin.getLogger().warning("Standard-Rechte fuer Gruppe '" + gruppe + "' fehlgeschlagen: " + fehler.getMessage());
                    return;
                }
                int anzahl = geaendert.get();
                plugin.getLogger().info(anzahl == 0 ? "Standard-Rechte fuer Gruppe '" + gruppe + "': alles schon da."
                        : "Standard-Rechte fuer Gruppe '" + gruppe + "': " + anzahl + " ergaenzt.");
                for (String recht : respektiert) {
                    plugin.getLogger().warning("Gruppe '" + gruppe + "' hat " + recht
                            + " ausdruecklich auf false - bleibt so. Wenn das nicht gewollt ist: lp group " + gruppe
                            + " permission unset " + recht);
                }
                if (anzahl > 0) {
                    luckPerms.getMessagingService().ifPresent(dienst -> dienst.pushUpdate());
                }
                if (rueckmeldung != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.msgs().send(rueckmeldung, "ranks.rechte",
                            "anzahl", String.valueOf(anzahl), "gruppe", gruppe));
                }
            });
            for (String name : erben) {
                if (name.equalsIgnoreCase(gruppe)) {
                    continue;
                }
                gruppen.loadGroup(name).thenAccept(gefunden -> gefunden.ifPresent(g -> {
                    boolean erbt = !erbtSchon(g, gruppe);
                    if (erbt) {
                        g.data().add(InheritanceNode.builder(gruppe).build());
                    }
                    List<String> teamRespektiert = new ArrayList<>();
                    int teamRechte = 0;
                    for (String recht : team) {
                        if (setzen(g, recht, true, teamRespektiert)) {
                            teamRechte++;
                        }
                    }
                    if (!erbt && teamRechte == 0) {
                        return;
                    }
                    int neu = teamRechte;
                    gruppen.saveGroup(g).thenRun(() -> {
                        if (erbt) {
                            plugin.getLogger().info("Gruppe '" + name + "' erbt jetzt alle Rechte von '" + gruppe + "'.");
                        }
                        if (neu > 0) {
                            plugin.getLogger().info("Gruppe '" + name + "': " + neu + " Team-Rechte ergaenzt (" + String.join(", ", team) + ").");
                        }
                    });
                }));
            }
        }

        static boolean erbtSchon(Group g, String gruppe) {
            for (InheritanceNode node : g.getNodes(NodeType.INHERITANCE)) {
                if (node.getGroupName().equalsIgnoreCase(gruppe) && node.getContexts().isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        static boolean setzen(Group g, String recht, boolean wert, List<String> respektiert) {
            Node vorhanden = null;
            for (Node node : g.getNodes()) {
                if (node.getKey().equalsIgnoreCase(recht) && node.getContexts().isEmpty() && !node.hasExpiry()) {
                    vorhanden = node;
                    break;
                }
            }
            if (vorhanden != null) {
                if (vorhanden.getValue() == wert) {
                    return false;
                }
                if (wert) {
                    respektiert.add(recht);
                    return false;
                }
                g.data().remove(vorhanden);
            }
            g.data().add(Node.builder(recht).value(wert).build());
            return true;
        }
    }
}
