package de.lemonpvp.smpproxy.release;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

import java.util.List;

public final class TestReleaseCommand implements SimpleCommand {

    public static final String RECHT = "smpproxy.release.test";

    private final SMPProxy plugin;

    public TestReleaseCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player spieler)) {
            invocation.source().sendMessage(plugin.message("player-only"));
            return;
        }
        if (!spieler.hasPermission(RECHT)) {
            spieler.sendMessage(plugin.message("no-permission"));
            return;
        }
        String[] args = invocation.arguments();
        if (args.length > 0 && (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("abbrechen"))) {
            spieler.sendMessage(plugin.message(plugin.release().probeStoppen(spieler) ? "testrelease-gestoppt" : "testrelease-keiner"));
            return;
        }
        int sekunden = 15;
        if (args.length > 0) {
            try {
                sekunden = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignoriert) {
            }
        }
        sekunden = Math.max(3, Math.min(600, sekunden));
        if (!plugin.release().probeStarten(spieler, sekunden)) {
            spieler.sendMessage(plugin.message("testrelease-laeuft"));
            return;
        }
        spieler.sendMessage(plugin.message("testrelease-start", "%sekunden%", String.valueOf(sekunden)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(RECHT);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return invocation.arguments().length <= 1 ? List.of("15", "30", "60", "stop") : List.of();
    }
}
