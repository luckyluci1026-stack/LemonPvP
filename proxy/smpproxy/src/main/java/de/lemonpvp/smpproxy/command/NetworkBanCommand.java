package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.ban.BanEntry;
import de.lemonpvp.smpproxy.ban.Durations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * /netban, /netunban, /netbans, /netbaninfo.
 *
 * Eine Klasse fuer alle vier, weil sie dieselbe Kernfrage teilen: "wessen
 * UUID ist gemeint?" - online per Name, offline per gemerktem Namen, oder
 * gleich eine UUID. Vier eigene Klassen haetten diese Aufloesung viermal.
 *
 * Bewusst NICHT "/ban": BetterSMP hat auf dem SMP selbst schon ein
 * /gban, das (wenn gewuenscht) ueber MariaDB zwischen mehreren SMPs
 * geteilt wird. Ein am Proxy registrierter Befehl wuerde JEDEN Aufruf
 * abfangen, egal auf welchem Server der Spieler steht, und dieses
 * bestehende /gban unbenutzbar machen. Ein eigener Name raeumt den
 * Unterschied auch fuer die Lehrkraft ein: /netban wirft aus dem ganzen
 * Netzwerk inklusive Lobby, /gban ist die Werkzeugkiste des SMPs selbst.
 */
public final class NetworkBanCommand implements SimpleCommand {

    private static final String PERMISSION = "smpproxy.ban";

    /** Welcher der vier Befehle das hier gerade ist. */
    public enum Modus { BAN, UNBAN, LIST, INFO }

    private final SMPProxy plugin;
    private final Modus modus;

    public NetworkBanCommand(SMPProxy plugin, Modus modus) {
        this.plugin = plugin;
        this.modus = modus;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(plugin.message("no-permission"));
            return;
        }

        switch (modus) {
            case BAN -> ban(source, args);
            case UNBAN -> unban(source, args);
            case LIST -> list(source);
            case INFO -> info(source, args);
        }
    }

    // ------------------------------------------------------------------ /netban

    private void ban(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(plugin.message("netban-usage"));
            return;
        }
        Optional<UUID> ziel = aufloesen(args[0]);
        if (ziel.isEmpty()) {
            source.sendMessage(plugin.message("netban-unknown", "%spieler%", args[0]));
            return;
        }
        UUID id = ziel.get();

        // args[1] ist entweder eine Dauer (30m, 7d, perm, ...) oder schon
        // der Anfang des Grundes - ein Grund, der zufaellig wie eine
        // Dauer aussieht ("2 Leute gemeldet"), waere sonst verschluckt.
        // Deshalb: nur als Dauer nehmen, wenn Durations.parse() etwas
        // Erkennbares findet ODER es woertlich perm/permanent/0 ist.
        long dauer = 0;
        int grundAb = 1;
        if (args.length >= 2 && istDauer(args[1])) {
            dauer = Durations.parse(args[1]);
            grundAb = 2;
        }
        String grund = args.length > grundAb
                ? String.join(" ", java.util.Arrays.asList(args).subList(grundAb, args.length))
                : plugin.config().message("netban-default-reason");
        String von = source instanceof Player p ? p.getUsername() : "Konsole";
        String name = plugin.bans().nameFuer(id);

        plugin.bans().ban(id, name, grund, von, dauer);
        source.sendMessage(plugin.message("netban-ok", "%spieler%", name,
                "%dauer%", dauer <= 0 ? plugin.config().banPermanentWord()
                                      : Durations.humanize(dauer)));

        BanEntry frisch = plugin.bans().aktiv(id).orElse(null);
        if (frisch != null) {
            plugin.banListener().kickIfOnline(frisch);
        }
    }

    private static boolean istDauer(String token) {
        String s = token.toLowerCase(java.util.Locale.ROOT);
        return s.equals("perm") || s.equals("permanent") || s.equals("0") || Durations.parse(s) > 0;
    }

    // ------------------------------------------------------------------ /netunban

    private void unban(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(plugin.message("netunban-usage"));
            return;
        }
        Optional<UUID> ziel = aufloesen(args[0]);
        if (ziel.isEmpty()) {
            source.sendMessage(plugin.message("netban-unknown", "%spieler%", args[0]));
            return;
        }
        if (plugin.bans().unban(ziel.get())) {
            source.sendMessage(plugin.message("netunban-ok", "%spieler%", args[0]));
        } else {
            source.sendMessage(plugin.message("netunban-not-banned", "%spieler%", args[0]));
        }
    }

    // ------------------------------------------------------------------ /netbans

    private void list(CommandSource source) {
        Map<UUID, BanEntry> alle = plugin.bans().alle();
        if (alle.isEmpty()) {
            source.sendMessage(plugin.message("netbans-empty"));
            return;
        }
        source.sendMessage(plugin.message("netbans-header", "%anzahl%", String.valueOf(alle.size())));
        for (BanEntry ban : alle.values()) {
            source.sendMessage(plugin.message("netbans-entry",
                    "%spieler%", ban.name(),
                    "%dauer%", ban.permanent() ? plugin.config().banPermanentWord()
                                                : Durations.expiry(ban.until(), "?"),
                    "%grund%", ban.reason()));
        }
    }

    // ------------------------------------------------------------------ /netbaninfo

    private void info(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(plugin.message("netbaninfo-usage"));
            return;
        }
        Optional<UUID> ziel = aufloesen(args[0]);
        BanEntry ban = ziel.map(id -> plugin.bans().aktiv(id).orElse(null)).orElse(null);
        if (ban == null) {
            source.sendMessage(plugin.message("netbaninfo-not-banned", "%spieler%", args[0]));
            return;
        }
        source.sendMessage(plugin.message("netbaninfo-header",
                "%spieler%", ban.name(),
                "%grund%", ban.reason(),
                "%von%", ban.by(),
                "%dauer%", ban.permanent() ? plugin.config().banPermanentWord()
                                            : Durations.expiry(ban.until(), "?")));
    }

    // ------------------------------------------------------------------ Aufloesen

    /**
     * Ein Name oder eine UUID -> die UUID, um die es geht.
     *
     * Reihenfolge: erst pruefen, ob es schon eine UUID ist (fuer den
     * seltenen Fall, dass jemand sie direkt zur Hand hat), dann ein
     * online Spieler mit dem Namen, zuletzt der Name aus spieler.yml -
     * jeder, der sich hier schon einmal verbunden hat.
     */
    private Optional<UUID> aufloesen(String eingabe) {
        try {
            return Optional.of(UUID.fromString(eingabe));
        } catch (IllegalArgumentException ignored) {
            // keine UUID - weiter mit dem Namen
        }
        Optional<Player> online = plugin.proxy().getPlayer(eingabe);
        if (online.isPresent()) {
            return Optional.of(online.get().getUniqueId());
        }
        return plugin.bans().uuidFuer(eingabe);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(PERMISSION)) {
            return List.of();
        }
        if (invocation.arguments().length <= 1 && modus != Modus.LIST) {
            List<String> namen = new ArrayList<>();
            for (Player player : plugin.proxy().getAllPlayers()) {
                namen.add(player.getUsername());
            }
            return namen;
        }
        return List.of();
    }
}
