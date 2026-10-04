package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.netzwerk.ChatRelay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class AhCommand implements SimpleCommand {

    private static final long NACHLAUF_MILLIS = 500L;
    private static final int MAX_LAENGE = 256;
    private static final List<String> UNTERBEFEHLE = List.of("sell", "search", "meine");
    private static final List<String> PREISE = List.of("1000", "10k", "1m");

    private final SMPProxy plugin;

    public AhCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player spieler)) {
            invocation.source().sendMessage(plugin.message("player-only"));
            return;
        }
        String befehl = befehl(invocation.arguments());
        String hier = ChatRelay.serverVon(spieler);
        String ziel = plugin.config().ahRedirectServer();
        if (plugin.config().ahPassthroughServers().contains(hier) || ziel.equals(hier)) {
            spieler.spoofChatInput(befehl);
            return;
        }
        Optional<RegisteredServer> zielServer = ziel.isEmpty() ? Optional.empty() : plugin.proxy().getServer(ziel);
        if (zielServer.isEmpty()) {
            spieler.sendMessage(plugin.message("ah-not-configured"));
            return;
        }
        if (plugin.release().abweisen(spieler, ziel)) {
            return;
        }
        spieler.sendMessage(plugin.message("ah-sending", "%server%", ziel));
        spieler.createConnectionRequest(zielServer.get()).connect().whenComplete((ergebnis, fehler) -> {
            if (fehler != null || ergebnis == null || !ergebnis.isSuccessful()) {
                spieler.sendMessage(plugin.message("switch-failed", "%server%", ziel));
                return;
            }
            plugin.proxy().getScheduler().buildTask(plugin, () -> {
                if (spieler.isActive() && ziel.equals(ChatRelay.serverVon(spieler))) {
                    spieler.spoofChatInput(befehl);
                }
            }).delay(NACHLAUF_MILLIS, TimeUnit.MILLISECONDS).schedule();
        });
    }

    public static String befehl(String[] args) {
        String text = ("/ah " + String.join(" ", args)).trim();
        return text.length() > MAX_LAENGE ? text.substring(0, MAX_LAENGE) : text;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return passend(UNTERBEFEHLE, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return passend(PREISE, args[1]);
        }
        return List.of();
    }

    private static List<String> passend(List<String> werte, String anfang) {
        String klein = anfang.toLowerCase(Locale.ROOT);
        List<String> liste = new ArrayList<>();
        for (String wert : werte) {
            if (wert.startsWith(klein)) {
                liste.add(wert);
            }
        }
        return liste;
    }
}
