package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.lemonpvp.smpproxy.SMPProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /smpproxy status | reload */
public final class ProxyCommand implements SimpleCommand {

    private static final String PERMISSION = "smpproxy.admin";

    private final SMPProxy plugin;

    public ProxyCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(plugin.message("no-permission"));
            return;
        }
        if (args.length == 0) {
            source.sendMessage(plugin.message("help"));
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> showStatus(source);
            case "reload" -> {
                plugin.reload();
                source.sendMessage(plugin.message("reloaded"));
            }
            default -> source.sendMessage(plugin.message("help"));
        }
    }

    private void showStatus(CommandSource source) {
        source.sendMessage(plugin.message("status-header"));
        for (RegisteredServer server : plugin.proxy().getAllServers()) {
            String name = server.getServerInfo().getName();
            if (plugin.watcher().isOnline(name)) {
                source.sendMessage(plugin.message("status-online",
                        "%server%", name,
                        "%spieler%", String.valueOf(server.getPlayersConnected().size())));
            } else {
                source.sendMessage(plugin.message("status-offline", "%server%", name));
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.source().hasPermission(PERMISSION) && invocation.arguments().length <= 1) {
            List<String> options = new ArrayList<>();
            options.add("status");
            options.add("reload");
            return options;
        }
        return List.of();
    }
}
