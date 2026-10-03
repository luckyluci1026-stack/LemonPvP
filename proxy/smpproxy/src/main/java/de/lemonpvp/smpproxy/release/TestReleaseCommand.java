package de.lemonpvp.smpproxy.release;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TestReleaseCommand implements SimpleCommand {

    public static final String RECHT = "smpproxy.release.test";

    private static final int STANDARD_SEKUNDEN = 15;

    private final SMPProxy plugin;

    public TestReleaseCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource quelle = invocation.source();
        if (!darf(quelle)) {
            quelle.sendMessage(plugin.message("no-permission"));
            return;
        }
        String[] args = invocation.arguments();
        boolean stop = args.length > 0 && (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("abbrechen"));
        String name = null;
        Integer sekunden = null;
        boolean andere = fuerAndere(quelle);
        if (stop) {
            name = args.length > 1 && andere ? args[1] : null;
        } else if (args.length > 0) {
            sekunden = zahl(args[0]);
            if (sekunden == null && andere) {
                name = args[0];
                sekunden = args.length > 1 ? zahl(args[1]) : null;
            }
        }
        Player ziel;
        if (name == null) {
            if (!(quelle instanceof Player selbst)) {
                quelle.sendMessage(plugin.message("testrelease-konsole"));
                return;
            }
            ziel = selbst;
        } else {
            Optional<Player> gefunden = plugin.proxy().getPlayer(name);
            if (gefunden.isEmpty()) {
                quelle.sendMessage(plugin.message("msg-offline", "%target%", name));
                return;
            }
            ziel = gefunden.get();
        }
        boolean selbst = quelle instanceof Player spieler && spieler.getUniqueId().equals(ziel.getUniqueId());
        if (stop) {
            boolean gestoppt = plugin.release().probeStoppen(ziel);
            if (selbst) {
                quelle.sendMessage(plugin.message(gestoppt ? "testrelease-gestoppt" : "testrelease-keiner"));
                return;
            }
            if (gestoppt) {
                ziel.sendMessage(plugin.message("testrelease-gestoppt"));
            }
            quelle.sendMessage(plugin.message(gestoppt ? "testrelease-gestoppt-fuer" : "testrelease-keiner-fuer",
                    "%spieler%", ziel.getUsername()));
            return;
        }
        int dauer = Math.max(3, Math.min(600, sekunden == null ? STANDARD_SEKUNDEN : sekunden));
        if (!plugin.release().probeStarten(ziel, dauer)) {
            quelle.sendMessage(selbst ? plugin.message("testrelease-laeuft")
                    : plugin.message("testrelease-laeuft-fuer", "%spieler%", ziel.getUsername()));
            return;
        }
        ziel.sendMessage(plugin.message("testrelease-start", "%sekunden%", String.valueOf(dauer)));
        if (!selbst) {
            quelle.sendMessage(plugin.message("testrelease-fuer", "%spieler%", ziel.getUsername(),
                    "%sekunden%", String.valueOf(dauer)));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return darf(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource quelle = invocation.source();
        List<String> vorschlaege = new ArrayList<>();
        if (args.length <= 1) {
            if (quelle instanceof Player) {
                vorschlaege.addAll(List.of("15", "30", "60"));
            }
            vorschlaege.add("stop");
            if (fuerAndere(quelle)) {
                vorschlaege.addAll(spielerNamen());
            }
        } else if (args.length == 2 && fuerAndere(quelle)) {
            if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("abbrechen")) {
                vorschlaege.addAll(spielerNamen());
            } else if (zahl(args[0]) == null) {
                vorschlaege.addAll(List.of("15", "30", "60"));
            }
        }
        String anfang = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        vorschlaege.removeIf(vorschlag -> !vorschlag.toLowerCase(Locale.ROOT).startsWith(anfang));
        return vorschlaege;
    }

    private List<String> spielerNamen() {
        List<String> namen = new ArrayList<>();
        for (Player spieler : plugin.proxy().getAllPlayers()) {
            namen.add(spieler.getUsername());
        }
        return namen;
    }

    private static boolean darf(CommandSource quelle) {
        return !(quelle instanceof Player) || quelle.hasPermission(RECHT) || quelle.hasPermission(ReleaseCommand.ADMIN);
    }

    private static boolean fuerAndere(CommandSource quelle) {
        return !(quelle instanceof Player) || quelle.hasPermission(ReleaseCommand.ADMIN);
    }

    private static Integer zahl(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException keineZahl) {
            return null;
        }
    }
}
