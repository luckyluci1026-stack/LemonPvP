package de.lemonpvp.antiswear.rechte;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class StandardRechte {

    static final List<String> ERLAUBEN = List.of("voicechat.listen", "voicechat.speak", "voicechat.groups");

    private StandardRechte() {
    }

    public static void anwenden(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("standard-rechte.enabled", true) || Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }
        String gruppe = config.getString("standard-rechte.gruppe", "default");
        List<String> erlauben = config.isList("standard-rechte.erlauben") ? config.getStringList("standard-rechte.erlauben") : ERLAUBEN;
        try {
            Bridge.anwenden(plugin, gruppe, erlauben);
        } catch (LinkageError | RuntimeException fehler) {
            plugin.getLogger().warning("Standard-Rechte konnten nicht gesetzt werden: " + fehler.getMessage());
        }
    }

    static final class Bridge {

        private Bridge() {
        }

        static void anwenden(JavaPlugin plugin, String gruppe, List<String> erlauben) {
            LuckPerms luckPerms = LuckPermsProvider.get();
            AtomicInteger neu = new AtomicInteger();
            List<String> respektiert = Collections.synchronizedList(new ArrayList<>());
            luckPerms.getGroupManager().modifyGroup(gruppe, g -> {
                for (String recht : erlauben) {
                    if (erlauben(g, recht, respektiert)) {
                        neu.incrementAndGet();
                    }
                }
            }).whenComplete((nichts, fehler) -> {
                if (fehler != null) {
                    plugin.getLogger().warning("Standard-Rechte fuer Gruppe '" + gruppe + "' fehlgeschlagen: " + fehler.getMessage());
                    return;
                }
                if (neu.get() > 0) {
                    plugin.getLogger().info("Voice-Chat-Rechte fuer Gruppe '" + gruppe + "': " + neu.get() + " ergaenzt.");
                    luckPerms.getMessagingService().ifPresent(dienst -> dienst.pushUpdate());
                }
                for (String recht : respektiert) {
                    plugin.getLogger().warning("Gruppe '" + gruppe + "' hat " + recht
                            + " ausdruecklich auf false - bleibt so. Wenn das nicht gewollt ist: lp group " + gruppe
                            + " permission unset " + recht);
                }
            });
        }

        static boolean erlauben(Group g, String recht, List<String> respektiert) {
            for (Node node : g.getNodes()) {
                if (node.getKey().equalsIgnoreCase(recht) && node.getContexts().isEmpty() && !node.hasExpiry()) {
                    if (!node.getValue()) {
                        respektiert.add(recht);
                    }
                    return false;
                }
            }
            g.data().add(Node.builder(recht).value(true).build());
            return true;
        }
    }
}
