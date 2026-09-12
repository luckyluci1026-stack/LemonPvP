package de.lemonpvp.punishplus.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.punishplus.PunishManager;
import de.lemonpvp.punishplus.PunishPlus;
import de.lemonpvp.punishplus.util.Durations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * /offend und /punish - gleiche Syntax, eine Klasse fuer beide (der
 * Konstruktor sagt, welcher der zwei Befehle das gerade ist). Die
 * Grund-ID entscheidet ueber die Dauer bei /offend, /punish ist immer
 * dauerhaft (siehe PunishManager).
 *
 * Nur online Spieler als Ziel: ohne das liesse sich punishplus.exempt
 * bei Offline-Spielern gar nicht zuverlaessig pruefen.
 */
public final class PunishCommand implements SimpleCommand {

    private final PunishPlus plugin;
    private final boolean punish;

    public PunishCommand(PunishPlus plugin, boolean punish) {
        this.plugin = plugin;
        this.punish = punish;
    }

    private String permission() {
        return punish ? "punishplus.punish" : "punishplus.offend";
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission(permission())) {
            source.sendMessage(plugin.message("no-permission"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(plugin.message(punish ? "punish-usage" : "offend-usage"));
            return;
        }
        Optional<Player> ziel = plugin.proxy().getPlayer(args[0]);
        if (ziel.isEmpty()) {
            source.sendMessage(plugin.message("not-online", "%spieler%", args[0]));
            return;
        }
        String grundId = args[1];
        String ausfuehrer = source instanceof Player p ? p.getUsername() : "Konsole";

        PunishManager.Ergebnis ergebnis = punish
                ? plugin.manager().punish(ziel.get(), grundId, ausfuehrer)
                : plugin.manager().offend(ziel.get(), grundId, ausfuehrer);

        switch (ergebnis) {
            case OK -> {
                if (punish) {
                    source.sendMessage(plugin.message("punish-success",
                            "%spieler%", ziel.get().getUsername(), "%grund%", grundId));
                } else {
                    String dauer = plugin.gruende().get(grundId)
                            .map(g -> Durations.humanize(g.offendDauerMillis())).orElse("?");
                    source.sendMessage(plugin.message("offend-success",
                            "%spieler%", ziel.get().getUsername(), "%grund%", grundId, "%dauer%", dauer));
                }
            }
            case AUSGENOMMEN -> source.sendMessage(plugin.message("exempt",
                    "%spieler%", ziel.get().getUsername()));
            case UNBEKANNTER_GRUND -> source.sendMessage(plugin.message("unknown-reason",
                    "%grund%", grundId, "%liste%", plugin.gruende().liste()));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(permission())) {
            return List.of();
        }
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            List<String> namen = new ArrayList<>();
            for (Player player : plugin.proxy().getAllPlayers()) {
                namen.add(player.getUsername());
            }
            return namen;
        }
        if (args.length == 2) {
            return List.of(plugin.gruende().liste().split(", "));
        }
        return List.of();
    }
}
