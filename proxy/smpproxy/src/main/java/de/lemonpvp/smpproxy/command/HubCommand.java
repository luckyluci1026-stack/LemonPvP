package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

/** /hub, /lobby - bringt den Spieler zum Hub-Server (SMPLobby, siehe hub-server in config.yml). */
public final class HubCommand implements SimpleCommand {

    private final SMPProxy plugin;

    public HubCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(plugin.message("player-only"));
            return;
        }
        String ziel = plugin.config().hubServer();
        if (ziel.isEmpty()) {
            player.sendMessage(plugin.message("hub-missing"));
            return;
        }
        boolean alreadyThere = player.getCurrentServer()
                .map(connection -> connection.getServer().getServerInfo().getName())
                .filter(ziel::equals)
                .isPresent();
        if (alreadyThere) {
            player.sendMessage(plugin.message("hub-already"));
            return;
        }
        player.sendMessage(plugin.message("hub-sending"));
        plugin.connect(player, ziel, false);
    }
}
