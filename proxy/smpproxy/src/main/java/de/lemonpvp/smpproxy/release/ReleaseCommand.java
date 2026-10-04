package de.lemonpvp.smpproxy.release;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.lemonpvp.smpproxy.SMPProxy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ReleaseCommand implements SimpleCommand {

    public static final String ADMIN = "smpproxy.release.admin";

    private static final List<String> UNTERBEFEHLE = List.of("status", "zeit", "jetzt", "aus");

    private final SMPProxy plugin;

    public ReleaseCommand(SMPProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource quelle = invocation.source();
        String[] args = invocation.arguments();
        if (args.length == 0 || args[0].equalsIgnoreCase("status") || !quelle.hasPermission(ADMIN)) {
            status(quelle);
            return;
        }
        ReleaseManager release = plugin.release();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "zeit" -> {
                Optional<Instant> zeit = args.length < 2 ? Optional.empty()
                        : release.parsen(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                if (zeit.isEmpty()) {
                    quelle.sendMessage(plugin.message("release-zeit-ungueltig"));
                    return;
                }
                if (zeit.get().toEpochMilli() <= release.jetzt()) {
                    quelle.sendMessage(plugin.message("release-zeit-vergangen"));
                    return;
                }
                release.zeitSetzen(zeit.get());
                quelle.sendMessage(plugin.message("release-zeit-gesetzt", "%datum%", release.datumText(), "%dauer%", release.restText()));
            }
            case "jetzt" -> {
                int sekunden = 10;
                if (args.length >= 2) {
                    try {
                        sekunden = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignoriert) {
                    }
                }
                sekunden = Math.max(3, Math.min(3_600, sekunden));
                release.zeitSetzen(Instant.ofEpochMilli(release.jetzt() + sekunden * 1000L));
                quelle.sendMessage(plugin.message("release-zeit-gesetzt", "%datum%", release.datumText(), "%dauer%", release.restText()));
            }
            case "aus" -> {
                release.zeitSetzen(null);
                quelle.sendMessage(plugin.message("release-aus"));
            }
            default -> quelle.sendMessage(plugin.message("release-hilfe"));
        }
    }

    private void status(CommandSource quelle) {
        ReleaseManager release = plugin.release();
        switch (release.phase()) {
            case KEIN -> quelle.sendMessage(plugin.message("release-status-kein"));
            case OFFEN -> quelle.sendMessage(plugin.message("release-status-offen", "%datum%", release.datumText()));
            case LAEUFT -> quelle.sendMessage(plugin.message("release-status-laeuft",
                    "%wartende%", String.valueOf(release.inWarteschlange())));
            case GEPLANT -> quelle.sendMessage(plugin.message("release-status", "%datum%", release.datumText(),
                    "%dauer%", release.restText(), "%wartende%", String.valueOf(release.wartende())));
        }
        if (quelle.hasPermission(ADMIN)) {
            quelle.sendMessage(plugin.message("release-hilfe"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!invocation.source().hasPermission(ADMIN)) {
            return List.of();
        }
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return passend(UNTERBEFEHLE, args.length == 0 ? "" : args[0]);
        }
        if (args[0].equalsIgnoreCase("zeit")) {
            ZonedDateTime morgen = ZonedDateTime.now(zone()).plusDays(1);
            if (args.length == 2) {
                return passend(List.of(DateTimeFormatter.ofPattern("dd.MM.yyyy").format(morgen)), args[1]);
            }
            if (args.length == 3) {
                return passend(List.of("18:00", "16:00", "20:00"), args[2]);
            }
        }
        if (args[0].equalsIgnoreCase("jetzt") && args.length == 2) {
            return passend(List.of("10", "30", "60"), args[1]);
        }
        return List.of();
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(plugin.config().releaseZeitzone());
        } catch (RuntimeException fehler) {
            return ZoneId.of("Europe/Berlin");
        }
    }

    private static List<String> passend(List<String> werte, String anfang) {
        List<String> ergebnis = new ArrayList<>();
        for (String wert : werte) {
            if (wert.toLowerCase(Locale.ROOT).startsWith(anfang.toLowerCase(Locale.ROOT))) {
                ergebnis.add(wert);
            }
        }
        return ergebnis;
    }
}
