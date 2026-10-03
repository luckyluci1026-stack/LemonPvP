package de.lemonpvp.smpproxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.netzwerk.ChatLog;
import de.lemonpvp.smpproxy.netzwerk.Texte;
import de.lemonpvp.smpproxy.util.Msg;
import net.kyori.adventure.text.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ChatLogCommand implements SimpleCommand {

    public static final String RECHT = "smpproxy.chatlog";
    private static final String ADMIN = "smpproxy.admin";

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter TAG_UND_ZEIT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final DateTimeFormatter NUR_ZEIT = DateTimeFormatter.ofPattern("HH:mm");

    private final SMPProxy plugin;

    public ChatLogCommand(SMPProxy plugin) {
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
        if (args.length == 0) {
            quelle.sendMessage(text("chatlog-usage", "%befehl%", befehl()));
            return;
        }
        String name = args[0];
        Optional<UUID> gefunden = plugin.proxy().getPlayer(name).map(Player::getUniqueId);
        if (gefunden.isEmpty()) {
            gefunden = plugin.bans().uuidFuer(name);
        }
        if (gefunden.isEmpty()) {
            quelle.sendMessage(text("chatlog-unbekannt", "%target%", name));
            return;
        }
        UUID spieler = gefunden.get();
        String anzeige = plugin.proxy().getPlayer(spieler).map(Player::getUsername)
                .orElseGet(() -> plugin.bans().nameFuer(spieler));
        int seite = args.length > 1 ? seite(args[1]) : 1;
        plugin.chatLog().lesen(spieler).thenAccept(eintraege -> zeigen(quelle, anzeige, eintraege, seite));
    }

    public void zeigen(CommandSource quelle, String name, List<ChatLog.Eintrag> eintraege, int gewuenscht) {
        String tage = String.valueOf(plugin.config().chatlogTage());
        if (eintraege.isEmpty()) {
            quelle.sendMessage(text("chatlog-leer", "%spieler%", name, "%tage%", tage));
            return;
        }
        int proSeite = plugin.config().chatlogZeilenProSeite();
        int seiten = (eintraege.size() + proSeite - 1) / proSeite;
        int seite = Math.max(1, Math.min(seiten, gewuenscht));
        int bis = eintraege.size() - (seite - 1) * proSeite;
        int von = Math.max(0, bis - proSeite);
        quelle.sendMessage(text("chatlog-kopf", "%spieler%", name, "%anzahl%", String.valueOf(eintraege.size()),
                "%tage%", tage));
        ZoneId zone = zone();
        LocalDate heute = LocalDate.now(zone);
        for (int i = von; i < bis; i++) {
            ChatLog.Eintrag eintrag = eintraege.get(i);
            ZonedDateTime zeit = Instant.ofEpochMilli(eintrag.zeit()).atZone(zone);
            String kurz = (zeit.toLocalDate().equals(heute) ? NUR_ZEIT : TAG_UND_ZEIT).format(zeit);
            String schluessel = switch (eintrag.art()) {
                case CHAT -> "chatlog-zeile";
                case MSG -> "chatlog-zeile-msg";
                case GESPERRT -> "chatlog-zeile-gesperrt";
            };
            quelle.sendMessage(text(schluessel, "%zeit%", kurz, "%datum%", DATUM.format(zeit),
                    "%ort%", eintrag.ort(), "%text%", eintrag.text()));
        }
        if (seiten > 1) {
            quelle.sendMessage(fuss(name, seite, seiten));
        }
    }

    private Component fuss(String name, int seite, int seiten) {
        String sicher = name.replaceAll("[^A-Za-z0-9_.]", "");
        String neuer = seite > 1 ? link("chatlog-neuer", sicher, seite - 1) : "";
        String aelter = seite < seiten ? link("chatlog-aelter", sicher, seite + 1) : "";
        String vorlage = plugin.config().message("chatlog-fuss")
                .replace("%seite%", String.valueOf(seite))
                .replace("%seiten%", String.valueOf(seiten))
                .replace("%neuer%", neuer)
                .replace("%aelter%", aelter);
        return Msg.of(vorlage, plugin.config().prefix());
    }

    private String link(String schluessel, String name, int seite) {
        return plugin.config().message(schluessel)
                .replace("%befehl%", befehl())
                .replace("%spieler%", name)
                .replace("%seite%", String.valueOf(seite));
    }

    private Component text(String schluessel, String... ersetzungen) {
        return Texte.mitSpielertext(plugin.config().message(schluessel), plugin.config().prefix(), ersetzungen);
    }

    private String befehl() {
        List<String> namen = plugin.config().chatlogAliases();
        return namen.isEmpty() ? "clog" : namen.get(0);
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(plugin.config().releaseZeitzone());
        } catch (DateTimeException falsch) {
            return ZoneId.of("Europe/Berlin");
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return darf(invocation.source());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> vorschlaege = new ArrayList<>();
        if (args.length > 1 || !darf(invocation.source())) {
            return vorschlaege;
        }
        String anfang = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        for (Player spieler : plugin.proxy().getAllPlayers()) {
            if (spieler.getUsername().toLowerCase(Locale.ROOT).startsWith(anfang)) {
                vorschlaege.add(spieler.getUsername());
            }
        }
        return vorschlaege;
    }

    private static boolean darf(CommandSource quelle) {
        return !(quelle instanceof Player) || quelle.hasPermission(RECHT) || quelle.hasPermission(ADMIN);
    }

    private static int seite(String text) {
        try {
            return Math.max(1, Integer.parseInt(text.trim()));
        } catch (NumberFormatException keineZahl) {
            return 1;
        }
    }
}
