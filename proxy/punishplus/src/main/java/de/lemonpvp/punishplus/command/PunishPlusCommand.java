package de.lemonpvp.punishplus.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.lemonpvp.punishplus.PunishPlus;

/** /punishplus reload - liest config.yml und bans.yml neu ein. */
public final class PunishPlusCommand implements SimpleCommand {

    private static final String PERMISSION = "punishplus.admin";

    private final PunishPlus plugin;

    public PunishPlusCommand(PunishPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(plugin.message("no-permission"));
            return;
        }
        plugin.reload();
        source.sendMessage(plugin.message("reloaded"));
    }
}
