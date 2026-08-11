package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

/**
 * Kurzbefehl pro Server: /smp1, /smp2 ...
 * Wird für jeden Server aus der Domain-Liste angelegt.
 */
public final class ServerCommand implements SimpleCommand {

    private final SMPProxy plugin;
    private final String server;

    public ServerCommand(SMPProxy plugin, String server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(plugin.message("player-only"));
            return;
        }
        boolean alreadyThere = player.getCurrentServer()
                .map(connection -> connection.getServer().getServerInfo().getName())
                .filter(server::equals)
                .isPresent();
        if (alreadyThere) {
            player.sendMessage(plugin.message("switch-already", "%server%", server));
            return;
        }
        if (!plugin.watcher().isOnline(server)) {
            player.sendMessage(plugin.message("switch-offline", "%server%", server));
            return;
        }
        player.sendMessage(plugin.message("switch-sending", "%server%", server));
        plugin.connect(player, server, false);
    }
}
