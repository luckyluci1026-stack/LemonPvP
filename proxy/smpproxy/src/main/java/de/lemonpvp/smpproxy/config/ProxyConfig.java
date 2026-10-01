package de.lemonpvp.smpproxy.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Liest die config.yml. Beim ersten Start wird die mitgelieferte Vorlage
 * in den Plugin-Ordner kopiert, damit man alles bequem anpassen kann.
 */
public final class ProxyConfig {

    private static final Map<String, String> STANDARD_TEXTE = Map.ofEntries(
            Map.entry("msg-usage", "%prefix%<gray>/msg <Spieler> <Nachricht></gray>"),
            Map.entry("reply-usage", "%prefix%<gray>/r <Nachricht></gray>"),
            Map.entry("msg-offline", "%prefix%<red><white>%target%</white> ist gerade nicht online.</red>"),
            Map.entry("msg-self", "%prefix%<red>Du kannst dir nicht selbst schreiben.</red>"),
            Map.entry("msg-no-reply", "%prefix%<red>Du hast noch keine Nachricht, auf die du antworten kannst.</red>"),
            Map.entry("msg-sent", "<dark_gray>[<gray>Du</gray> <dark_gray>→</dark_gray> "
                    + "<#00D4FF>%target%</#00D4FF>]</dark_gray> <white>%message%</white>"),
            Map.entry("msg-received", "<hover:show_text:'<gray>Klick zum Antworten'><click:suggest_command:'/msg %sender% '>"
                    + "<dark_gray>[<#00D4FF>%sender%</#00D4FF> <dark_gray>→</dark_gray> <gray>Dir</gray>]</dark_gray>"
                    + "</click></hover> <white>%message%</white>"),
            Map.entry("freeze-blocked", "%prefix%<red>Du bist eingefroren und kannst den Server gerade nicht wechseln.</red>"),
            Map.entry("server-crashed-home", "%prefix%<red>Verbindung zu <white>%server%</white> verloren.</red>"
                    + "<newline><gray>Du bist wieder auf <white>%home%</white>.</gray>"),
            Map.entry("server-unreachable", "%prefix%<red><white>%server%</white> ist gerade nicht erreichbar.</red>"),
            Map.entry("switch-failed", "%prefix%<red>Verbindung zu <white>%server%</white> fehlgeschlagen.</red>"),
            Map.entry("player-only", "%prefix%<red>Das geht nur im Spiel.</red>"),
            Map.entry("no-permission", "%prefix%<red>Dazu hast du keine Rechte.</red>"),
            Map.entry("ah-sending", "%prefix%<gray>Das Auktionshaus ist auf <white>%server%</white> - einen Moment ...</gray>"),
            Map.entry("ah-not-configured", "%prefix%<red>/ah ist nicht eingerichtet.</red><newline>"
                    + "<gray>In config.yml unter <white>ah.redirect-server</white> einen Server eintragen.</gray>"),
            Map.entry("muted", "%prefix%<red>Du bist stummgeschaltet und kannst gerade nicht schreiben "
                    + "(noch <white>%dauer%</white>).</red>"),
            Map.entry("network-join", "<dark_gray>[<green>+</green>]</dark_gray> <#00D4FF>%player%</#00D4FF> "
                    + "<gray>ist dem Server beigetreten.</gray>"),
            Map.entry("network-first-join", "<dark_gray>[<#00D4FF>★</#00D4FF>]</dark_gray> <#00D4FF>%player%</#00D4FF> "
                    + "<gray>ist zum <white>ersten Mal</white> hier! Willkommen!</gray>"),
            Map.entry("release-gesperrt", "%prefix%<red>Noch nicht offen!</red> <gray>Release am <white>%datum%</white> (noch <white>%dauer%</white>).</gray>"),
            Map.entry("release-bildschirm", "<gradient:#6C5CE7:#00D4FF><bold>BuckSMP öffnet bald!</bold></gradient><newline><newline><gray>Release am</gray> <white>%datum%</white><newline><gray>Noch</gray> <white>%dauer%</white><newline><newline><gray>Bis gleich!</gray>"),
            Map.entry("release-warten", "%prefix%<gray>Willkommen! BuckSMP öffnet am <white>%datum%</white> (noch <white>%dauer%</white>).</gray><newline><gray>Bleib einfach hier - wer früher da ist, ist beim Release früher drin.</gray>"),
            Map.entry("release-laeuft-warten", "%prefix%<green>Der Release läuft!</green> <gray>Du bist in der Warteschlange und kommst gleich rein.</gray>"),
            Map.entry("release-bossbar", "<gradient:#6C5CE7:#00D4FF><bold>Release</bold></gradient> <white>in %dauer%</white>"),
            Map.entry("release-bossbar-probe", "<gradient:#6C5CE7:#00D4FF><bold>Probe-Release</bold></gradient> <white>in %dauer%</white>"),
            Map.entry("release-bossbar-wellen", "<green><bold>Release läuft</bold></green> <white>- Welle %welle%</white> <gray>(%wartende% warten noch)</gray>"),
            Map.entry("release-bossbar-server-startet", "<gold><bold>%server% startet gleich ...</bold></gold>"),
            Map.entry("release-server-startet", "<gold>%server% startet noch - gleich geht's los!</gold>"),
            Map.entry("release-actionbar", "<gray>Noch</gray> <white><bold>%sekunden%</bold></white> <gray>Sekunden bis zum Release</gray>"),
            Map.entry("release-actionbar-probe", "<gray>Probe-Release in</gray> <white><bold>%sekunden%</bold></white> <gray>Sekunden</gray>"),
            Map.entry("release-titel-zahl", "<gradient:#6C5CE7:#00D4FF><bold>%sekunden%</bold></gradient>"),
            Map.entry("release-titel-zahl-unter", "<gray>bis zum Release</gray>"),
            Map.entry("release-titel-los", "<gradient:#FFD166:#FF6B6B><bold>RELEASE!</bold></gradient>"),
            Map.entry("release-titel-los-unter", "<white>Willkommen auf BuckSMP</white>"),
            Map.entry("release-position", "<green>Du bist gleich dran!</green> <gray>Platz <white>%position%</white> - noch etwa <white>%sekunden%s</white></gray>"),
            Map.entry("release-willkommen", "%prefix%<green>Viel Spaß auf BuckSMP!</green>"),
            Map.entry("release-offen", "%prefix%<gold><bold>Release!</bold></gold> <gray>BuckSMP ist ab jetzt offen.</gray>"),
            Map.entry("release-verbindung-fehlgeschlagen", "%prefix%<red>Die Verbindung zu <white>%server%</white> hat nicht geklappt.</red> <gray>Versuch es gleich nochmal mit /hub.</gray>"),
            Map.entry("release-erinnerung", "%prefix%<gray>Release am <white>%datum%</white> - noch <white>%dauer%</white>. <white>%wartende%</white> warten schon hier.</gray>"),
            Map.entry("release-neue-zeit", "%prefix%<gold>Neuer Release-Termin:</gold> <white>%datum%</white> <gray>(noch %dauer%)</gray>"),
            Map.entry("release-abgesagt", "%prefix%<gray>Der Release-Countdown ist beendet - du kommst gleich auf den Server.</gray>"),
            Map.entry("release-status", "%prefix%<gray>Release am <white>%datum%</white> (noch <white>%dauer%</white>) <dark_gray>·</dark_gray> <white>%wartende%</white> im Warteraum</gray>"),
            Map.entry("release-status-laeuft", "%prefix%<green>Der Release läuft gerade</green> <gray>(<white>%wartende%</white> in der Warteschlange).</gray>"),
            Map.entry("release-status-kein", "%prefix%<gray>Es ist gerade kein Release geplant.</gray>"),
            Map.entry("release-status-offen", "%prefix%<gray>Der Release vom <white>%datum%</white> ist schon gelaufen.</gray>"),
            Map.entry("release-hilfe", "%prefix%<gray>/release <white>zeit <Datum> <Uhrzeit></white> <dark_gray>|</dark_gray> <white>jetzt [Sekunden]</white> <dark_gray>|</dark_gray> <white>aus</white> <dark_gray>|</dark_gray> <white>status</white></gray>"),
            Map.entry("release-zeit-ungueltig", "%prefix%<red>Das Datum verstehe ich nicht.</red> <gray>Beispiel: /release zeit 03.10.2026 18:00</gray>"),
            Map.entry("release-zeit-vergangen", "%prefix%<red>Der Zeitpunkt liegt in der Vergangenheit.</red> <gray>Für sofort: /release jetzt</gray>"),
            Map.entry("release-zeit-gesetzt", "%prefix%<green>Release am <white>%datum%</white> (noch %dauer%).</green>"),
            Map.entry("release-aus", "%prefix%<green>Kein Release mehr geplant - alle Server sind normal erreichbar.</green>"),
            Map.entry("testrelease-start", "%prefix%<gray>Probe-Release in <white>%sekunden%s</white> - nur für dich, der echte Release bleibt unberührt.</gray>"),
            Map.entry("testrelease-laeuft", "%prefix%<gold>Dein Probe-Release läuft schon.</gold> <gray>/testrelease stop bricht ab.</gray>"),
            Map.entry("testrelease-gestoppt", "%prefix%<gray>Probe-Release abgebrochen.</gray>"),
            Map.entry("testrelease-keiner", "%prefix%<gray>Bei dir läuft gerade kein Probe-Release.</gray>"),
            Map.entry("testrelease-fertig", "%prefix%<green>Probe-Release fertig</green> <gray>- genau so erleben es alle beim echten Release.</gray>"),
            Map.entry("testrelease-fuer", "%prefix%<gray>Probe-Release für <white>%spieler%</white> in <white>%sekunden%s</white> gestartet.</gray>"),
            Map.entry("testrelease-laeuft-fuer", "%prefix%<gold>Bei <white>%spieler%</white> läuft schon ein Probe-Release.</gold>"),
            Map.entry("testrelease-gestoppt-fuer", "%prefix%<gray>Probe-Release von <white>%spieler%</white> abgebrochen.</gray>"),
            Map.entry("testrelease-keiner-fuer", "%prefix%<gray>Bei <white>%spieler%</white> läuft gerade kein Probe-Release.</gray>"),
            Map.entry("testrelease-gesperrt", "%prefix%<red>Noch nicht offen!</red> <gray>Probe-Release in <white>%dauer%</white> - bis dahin bleibst du im Warteraum, genau wie alle beim echten Release.</gray> <dark_gray>(/testrelease stop bricht ab)</dark_gray>"),
            Map.entry("testrelease-konsole", "%prefix%<gray>Aus der Konsole: <white>/testrelease <Spieler> [Sekunden]</white> oder <white>/testrelease stop <Spieler></white></gray>"),
            Map.entry("network-quit", "<dark_gray>[<red>-</red>]</dark_gray> <#00D4FF>%player%</#00D4FF> "
                    + "<gray>hat den Server verlassen.</gray>"),
            Map.entry("einlass-warteschlange", "%prefix%<gray>Gerade kommen viele gleichzeitig auf <white>%server%</white> - "
                    + "du bist in der Warteschlange (Platz <white>%platz%</white>) und kommst automatisch rein.</gray>"),
            Map.entry("einlass-warteraum", "%prefix%<gray>Gerade kommen viele gleichzeitig auf <white>%server%</white>. "
                    + "Du wartest kurz hier (Platz <white>%platz%</white>) und kommst automatisch rein.</gray>"),
            Map.entry("einlass-position", "<gray>Warteschlange <white>%server%</white>: Platz <white>%platz%</white> "
                    + "<dark_gray>·</dark_gray> noch etwa <white>%sekunden%s</white></gray>"),
            Map.entry("einlass-pause", "<gold>%server% ist gerade ausgelastet</gold> <gray>- Platz <white>%platz%</white>, "
                    + "gleich geht's weiter</gray>"),
            Map.entry("einlass-startet", "<gold>%server% startet gerade</gold> <gray>- Platz <white>%platz%</white>, "
                    + "du kommst automatisch rein</gray>"),
            Map.entry("release-bossbar-ausgelastet", "<gold><bold>%server% holt kurz Luft ...</bold></gold>"),
            Map.entry("release-ausgelastet", "<gold>%server% ist gerade ausgelastet - die nächste Welle kommt gleich.</gold>"),
            Map.entry("status-last", "<dark_gray>    ↳ </dark_gray><gray><white>%mspt%</white> ms/Tick <dark_gray>·</dark_gray> "
                    + "<white>%tps%</white> TPS <dark_gray>·</dark_gray> <white>%warten%</white> in der Warteschlange</gray>"));

    private static final List<String> STANDARD_REGELN = List.of(
            "<gradient:#6C5CE7:#00D4FF><bold>BuckSMP · Regeln</bold></gradient>",
            "<white>1. Respektvoller Umgang</white> <dark_gray>-</dark_gray> <gray>Beleidigungen, Hassrede, Rassismus und "
                    + "Diskriminierung jeder Art sind nicht erlaubt - im Chat, Voice und im WhatsApp Channel.</gray>",
            "<white>2. Kein Cheaten</white> <dark_gray>-</dark_gray> <gray>Hacks, X-Ray, Autoclicker, Exploits und Bugusing "
                    + "sind verboten. Unser Anti-Cheat läuft mit - Verstöße fliegen auf.</gray>",
            "<white>3. Kein Griefing & Diebstahl</white> <dark_gray>-</dark_gray> <gray>Fremde Bauten, Farmen und Truhen "
                    + "bleiben unangetastet, außer im ausdrücklich erlaubten PvP-/Duell-Kontext.</gray>",
            "<white>4. Kein Spam & keine Werbung</white> <dark_gray>-</dark_gray> <gray>Kein Werben für andere Server, keine "
                    + "Dauerwerbung eigener Projekte, kein Chat-Spam oder Caps-Flooding.</gray>",
            "<white>5. Ein Account pro Spieler</white> <dark_gray>-</dark_gray> <gray>Multi-Accounts zum Umgehen von Strafen "
                    + "oder zum Ausnutzen von Vorteilen sind nicht gestattet.</gray>",
            "<white>6. Anweisungen vom Team befolgen</white> <dark_gray>-</dark_gray> <gray>Team-Mitglieder moderieren im Sinne "
                    + "der Community - ihren Anweisungen ist Folge zu leisten.</gray>",
            "",
            "<gray>Regelverstoß gesehen?</gray> <click:suggest_command:'/report '><hover:show_text:'<gray>Spieler melden'>"
                    + "<#00D4FF>/report</#00D4FF></hover></click> <dark_gray>·</dark_gray> <gray>Voice-Chat:</gray> "
                    + "<click:run_command:'/vcrules'><hover:show_text:'<gray>Regeln für den Voice-Chat'><#00D4FF>/vcrules</#00D4FF>"
                    + "</hover></click>");

    private static final String ALTE_WEBSEITEN_ZEILE = "<gray>Alles nachlesen:</gray> <click:open_url:'https://bucksmp.de/regeln/'>"
            + "<hover:show_text:'<gray>Im Browser öffnen'><#00D4FF><underlined>bucksmp.de/regeln</underlined></#00D4FF></hover></click>";

    private final Path folder;
    private final Logger log;

    private Map<String, Object> root = new LinkedHashMap<>();

    /** Domain (klein geschrieben) -> Servername */
    private final Map<String, String> domains = new LinkedHashMap<>();
    /** Stern-Einträge wie "*.lemon-servers.de" -> Servername */
    private final Map<String, String> wildcards = new LinkedHashMap<>();

    public ProxyConfig(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
    }

    public void load() {
        Path file = folder.resolve("config.yml");
        try {
            if (Files.notExists(folder)) {
                Files.createDirectories(folder);
            }
            if (Files.notExists(file)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, file);
                        log.info("config.yml angelegt: {}", file.toAbsolutePath());
                    }
                }
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Object parsed = new Yaml().load(reader);
                root = parsed instanceof Map ? castMap(parsed) : new LinkedHashMap<>();
            }
        } catch (IOException ex) {
            log.error("config.yml konnte nicht gelesen werden - benutze Standardwerte", ex);
            root = new LinkedHashMap<>();
        }
        buildRoutes();
    }

    private void buildRoutes() {
        domains.clear();
        wildcards.clear();
        Object raw = root.get("domains");
        if (!(raw instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String host = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT).trim();
            String server = String.valueOf(entry.getValue()).trim();
            if (host.isEmpty() || server.isEmpty()) {
                continue;
            }
            if (host.startsWith("*.")) {
                wildcards.put(host.substring(1), server);
            } else if (host.equals("*")) {
                wildcards.put("", server);
            } else {
                domains.put(host, server);
            }
        }
    }

    // ------------------------------------------------------------------
    //  Routing
    // ------------------------------------------------------------------

    /** Liefert den Server zu einer Domain, sonst den Standardserver. */
    public String serverFor(String host) {
        String clean = normalise(host);
        if (clean != null && !clean.isEmpty()) {
            String direct = domains.get(clean);
            if (direct != null) {
                return direct;
            }
            for (Map.Entry<String, String> entry : wildcards.entrySet()) {
                String suffix = entry.getKey();
                if (suffix.isEmpty() || clean.endsWith(suffix)) {
                    return entry.getValue();
                }
            }
        }
        return defaultServer();
    }

    /**
     * Macht aus dem, was der Client geschickt hat, einen sauberen Hostnamen:
     * Kleinbuchstaben, ohne Port, ohne Punkt am Ende und ohne den
     * FML-Anhang von Forge-Clients.
     */
    public static String normalise(String host) {
        if (host == null) {
            return null;
        }
        String clean = host.trim().toLowerCase(Locale.ROOT);
        int nul = clean.indexOf('\0');
        if (nul >= 0) {
            clean = clean.substring(0, nul);
        }
        int colon = clean.indexOf(':');
        if (colon >= 0) {
            clean = clean.substring(0, colon);
        }
        while (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    public Map<String, String> domains() {
        return domains;
    }

    public String defaultServer() {
        return string("default-server", "");
    }

    public String limbo() {
        return string("limbo", "");
    }

    /**
     * Wohin /hub und /lobby wirklich bringen.
     *
     * Frueher gab es nur den Warteraum, also zeigten /hub und /lobby
     * dorthin - besser als nichts, aber ein Warteraum ist kein Ort zum
     * Verweilen. Jetzt gibt es SMPLobby: einen richtigen Hub-Server mit
     * Spawn, Serverauswahl und allem drumherum. hub-server zeigt dorthin.
     *
     * Bleibt hub-server leer (z.B. in einer aelteren config.yml, die die
     * Zeile noch nicht kennt), faellt es auf den Warteraum zurueck -
     * genau das alte Verhalten, ohne dass jemand seine Konfiguration
     * nachziehen muesste.
     */
    public String hubServer() {
        String eigener = string("hub-server", "");
        return eigener.isEmpty() ? limbo() : eigener;
    }

    // ------------------------------------------------------------------
    //  Überwachung
    // ------------------------------------------------------------------

    public int pingInterval() {
        return Math.max(1, integer("monitor.interval", 5));
    }

    public int pingTimeout() {
        return Math.max(1, integer("monitor.timeout", 3));
    }

    public boolean pingOnStart() {
        return bool("monitor.check-on-start", true);
    }

    public boolean autoReturn() {
        return bool("auto-return.enabled", true);
    }

    public int returnConfirmations() {
        return Math.max(1, integer("auto-return.confirmations", 3));
    }

    public int returnCooldown() {
        return Math.max(1, integer("auto-return.cooldown", 5));
    }

    public List<String> returnSkipServers() {
        return strings("auto-return.skip-servers", List.of("Duels"));
    }

    public boolean returnNotice() {
        return bool("auto-return.show-notice", true);
    }

    // ------------------------------------------------------------------
    //  Befehle
    // ------------------------------------------------------------------

    public boolean hubEnabled() {
        return bool("commands.hub-enabled", true);
    }

    public List<String> hubAliases() {
        return strings("commands.hub-aliases", List.of("hub", "lobby"));
    }

    public boolean serverShortcuts() {
        return bool("commands.server-shortcuts", true);
    }

    public boolean combatEnabled() {
        return bool("combat.enabled", true);
    }

    public Set<String> combatBlockedCommands() {
        Set<String> befehle = new HashSet<>();
        befehle.add("server");
        if (hubEnabled()) {
            befehle.addAll(hubAliases());
        }
        if (serverShortcuts()) {
            befehle.addAll(domains.values());
        }
        befehle.addAll(strings("combat.blocked-commands", List.of()));
        Set<String> klein = new HashSet<>();
        for (String befehl : befehle) {
            klein.add(befehl.toLowerCase(Locale.ROOT).trim());
        }
        return klein;
    }

    public String combatBlockedMessage() {
        return string("messages.combat-blocked",
                "%prefix%<red>Dieser Befehl ist im Kampf gesperrt! (<white>%seconds%s</white>)</red>");
    }

    public String duelBlockedMessage() {
        return string("messages.duel-blocked",
                "%prefix%<red>Das geht erst, wenn das Duell vorbei ist.</red>");
    }

    public String duelEndBlockedMessage() {
        return string("messages.duel-end-blocked",
                "%prefix%<gray>Das geht erst, wenn du aus der Arena zurück bist -</gray> <white>/spawn</white> "
                        + "<gray>bringt dich sofort hin.</gray>");
    }

    // ------------------------------------------------------------------
    //  /rtp ueberall
    // ------------------------------------------------------------------

    public boolean rtpEnabled() {
        return bool("rtp.enabled", true);
    }

    /** Server, auf denen /rtp schon von sich aus existiert (BetterRTP ist dort drauf). */
    public List<String> rtpPassthroughServers() {
        return strings("rtp.passthrough-servers", List.of());
    }

    /** Wohin /rtp von ueberall sonst zuerst schickt, bevor RTP ausgeloest wird. */
    public String rtpRedirectServer() {
        return string("rtp.redirect-server", "");
    }

    public boolean ahEnabled() {
        return bool("ah.enabled", true);
    }

    public List<String> ahAliases() {
        List<String> namen = new ArrayList<>();
        for (String name : strings("ah.aliases", List.of("ah", "auktionshaus", "auktion", "auction", "auctionhouse"))) {
            String klein = name.toLowerCase(Locale.ROOT).trim();
            if (!klein.isEmpty() && !namen.contains(klein)) {
                namen.add(klein);
            }
        }
        return namen;
    }

    public List<String> ahPassthroughServers() {
        return strings("ah.passthrough-servers", List.of("SMP"));
    }

    public String ahRedirectServer() {
        return string("ah.redirect-server", "SMP");
    }

    public String releaseZeit() {
        return string("release.zeit", "");
    }

    public String releaseZeitzone() {
        return string("release.zeitzone", "Europe/Berlin");
    }

    public String releaseZielServer() {
        return string("release.ziel-server", defaultServer().isEmpty() ? "SMP" : defaultServer());
    }

    public List<String> releaseGesperrteServer() {
        return strings("release.gesperrte-server", List.of("SMP", "Lobby", "Duels"));
    }

    public int releaseProWelle() {
        return Math.max(1, integer("release.pro-welle", 5));
    }

    public int releaseWellenAbstandSekunden() {
        return Math.max(1, integer("release.wellen-abstand-sekunden", 2));
    }

    public int releaseFinaleSekunden() {
        return Math.max(3, Math.min(60, integer("release.finale-sekunden", 10)));
    }

    public int releaseErinnerungMinuten() {
        return Math.max(0, integer("release.erinnerung-minuten", 5));
    }

    public boolean einlassAktiv() {
        return bool("einlass.aktiv", true);
    }

    public List<String> einlassServer() {
        return strings("einlass.server", List.of(defaultServer().isEmpty() ? "SMP" : defaultServer()));
    }

    public double einlassProSekunde() {
        return Math.max(0.2, zahl("einlass.pro-sekunde", 4));
    }

    public double einlassLangsamerAbMspt() {
        return Math.max(1, zahl("einlass.langsamer-ab-mspt", 35));
    }

    public double einlassPauseAbMspt() {
        return Math.max(einlassLangsamerAbMspt(), zahl("einlass.pause-ab-mspt", 45));
    }

    public List<String> einlassAusnahmenVon() {
        return strings("einlass.ausnahmen-von", List.of("Duels"));
    }

    public boolean geyserOptimieren() {
        return bool("bedrock.geyser-optimieren", true);
    }

    public boolean regelnEnabled() {
        return bool("regeln.enabled", true);
    }

    public List<String> regelnAliases() {
        List<String> namen = new ArrayList<>();
        for (String name : strings("regeln.aliases", List.of("regeln", "rules"))) {
            String klein = name.toLowerCase(Locale.ROOT).trim();
            if (!klein.isEmpty() && !namen.contains(klein)) {
                namen.add(klein);
            }
        }
        return namen;
    }

    public List<String> regeln() {
        Object value = get("regeln.zeilen");
        if (value instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(zeile -> zeile == null ? "" : String.valueOf(zeile))
                    .filter(zeile -> !zeile.equals(ALTE_WEBSEITEN_ZEILE)).toList();
        }
        return STANDARD_REGELN;
    }

    // ------------------------------------------------------------------
    //  Nachrichten
    // ------------------------------------------------------------------

    public String prefix() {
        Object value = get("messages.prefix");
        return value == null ? "" : String.valueOf(value).stripLeading();
    }

    public String message(String key) {
        return string("messages." + key, STANDARD_TEXTE.getOrDefault(key, ""));
    }

    public boolean chatSyncEnabled() {
        return bool("chat-sync.enabled", true);
    }

    public List<String> chatServers() {
        return strings("chat-sync.servers", List.of("SMP", "Lobby"));
    }

    public String chatServerTag() {
        String tag = string("chat-sync.server-tag", "<dark_gray>[<gray>%server%</gray>]</dark_gray>");
        return tag.isEmpty() ? "" : tag + " ";
    }

    public boolean joinQuitEnabled() {
        return bool("join-quit.enabled", true);
    }

    public boolean msgEnabled() {
        return bool("msg.enabled", true);
    }

    public List<String> msgAliases() {
        return strings("msg.aliases", List.of("msg", "tell", "w", "whisper", "pm", "m"));
    }

    public List<String> replyAliases() {
        return strings("msg.reply-aliases", List.of("r", "reply"));
    }

    public String joinMessage() {
        return message("network-join");
    }

    public String firstJoinMessage() {
        return message("network-first-join");
    }

    public String quitMessage() {
        return message("network-quit");
    }

    public String msgSentMessage() {
        return message("msg-sent");
    }

    public String msgReceivedMessage() {
        return message("msg-received");
    }

    public String mutedMessage() {
        return message("muted");
    }

    /** Reines Wort, kein Nachrichten-Baustein - wird in andere Texte eingesetzt. */
    public String banPermanentWord() {
        return string("messages.ban-permanent-word", "für immer");
    }

    // ------------------------------------------------------------------
    //  Zugriff auf verschachtelte Werte ("a.b.c")
    // ------------------------------------------------------------------

    private Object get(String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }

    private String string(String path, String fallback) {
        Object value = get(path);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private int integer(String path, int fallback) {
        Object value = get(path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                // Standardwert benutzen
            }
        }
        return fallback;
    }

    private double zahl(String path, double fallback) {
        Object value = get(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value).trim().replace(',', '.'));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean bool(String path, boolean fallback) {
        Object value = get(path);
        if (value instanceof Boolean b) {
            return b;
        }
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private List<String> strings(String path, List<String> fallback) {
        Object value = get(path);
        if (value instanceof List<?> list && !list.isEmpty()) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
