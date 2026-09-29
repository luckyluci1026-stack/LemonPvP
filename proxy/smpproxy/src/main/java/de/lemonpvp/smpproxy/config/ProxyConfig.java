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
            Map.entry("ah-sending", "%prefix%<gray>Das Auktionshaus ist auf <white>%server%</white> - einen Moment ...</gray>"),
            Map.entry("ah-not-configured", "%prefix%<red>/ah ist nicht eingerichtet.</red><newline>"
                    + "<gray>In config.yml unter <white>ah.redirect-server</white> einen Server eintragen.</gray>"),
            Map.entry("muted", "%prefix%<red>Du bist stummgeschaltet und kannst gerade nicht schreiben "
                    + "(noch <white>%dauer%</white>).</red>"),
            Map.entry("network-join", "<dark_gray>[<green>+</green>]</dark_gray> <#00D4FF>%player%</#00D4FF> "
                    + "<gray>ist dem Server beigetreten.</gray>"),
            Map.entry("network-first-join", "<dark_gray>[<#00D4FF>★</#00D4FF>]</dark_gray> <#00D4FF>%player%</#00D4FF> "
                    + "<gray>ist zum <white>ersten Mal</white> hier! Willkommen!</gray>"),
            Map.entry("network-quit", "<dark_gray>[<red>-</red>]</dark_gray> <#00D4FF>%player%</#00D4FF> "
                    + "<gray>hat den Server verlassen.</gray>"));

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
                    + "</hover></click>",
            "<gray>Alles nachlesen:</gray> <click:open_url:'https://bucksmp.de/regeln/'><hover:show_text:'<gray>Im Browser öffnen'>"
                    + "<#00D4FF><underlined>bucksmp.de/regeln</underlined></#00D4FF></hover></click>");

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
            return list.stream().map(zeile -> zeile == null ? "" : String.valueOf(zeile)).toList();
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
