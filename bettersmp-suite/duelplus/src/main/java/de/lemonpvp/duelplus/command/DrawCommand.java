package de.lemonpvp.duelplus.command;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.session.DuellSession;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * /draw - nur auf dem Duels-Server sinnvoll (nur dort existieren
 * DuellSession-Objekte). Wirkt erst, wenn BEIDE Duellanten es benutzen
 * (siehe DuellSessionManager.unentschiedenVorschlagen).
 */
public final class DrawCommand implements CommandExecutor {

    private final DuelPlus plugin;

    public DrawCommand(DuelPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player spieler)) {
            plugin.msgs().send(sender, "player-only");
            return true;
        }
        if (!spieler.hasPermission("duelplus.use")) {
            plugin.msgs().send(spieler, "no-permission");
            return true;
        }
        if (plugin.sessionManager() == null) {
            plugin.msgs().send(spieler, "draw-not-in-duel");
            return true;
        }
        Optional<DuellSession> sessionOpt = plugin.sessionManager().sessionVon(spieler.getUniqueId());
        if (sessionOpt.isEmpty()) {
            plugin.msgs().send(spieler, "draw-not-in-duel");
            return true;
        }
        plugin.sessionManager().unentschiedenVorschlagen(sessionOpt.get(), spieler);
        return true;
    }
}
