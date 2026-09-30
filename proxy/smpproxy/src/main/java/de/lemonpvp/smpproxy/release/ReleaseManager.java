package de.lemonpvp.smpproxy.release;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.netzwerk.ChatRelay;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class ReleaseManager {

    public enum Phase { KEIN, GEPLANT, LAEUFT, OFFEN }

    public static final String BYPASS = "smpproxy.release.bypass";

    private static final List<DateTimeFormatter> FORMATE = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm"),
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm:ss"));
    private static final DateTimeFormatter ANZEIGE = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm 'Uhr'", Locale.GERMANY);
    private static final DateTimeFormatter SPEICHERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long STUNDE = 3_600_000L;
    private static final long MINUTE = 60_000L;
    private static final int MAX_VERSUCHE = 3;
    private static final Title.Times ZAHL_ZEITEN = Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(150));
    private static final Title.Times LOS_ZEITEN = Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(3500), Duration.ofMillis(800));

    private final SMPProxy plugin;
    private final Path datei;
    private final LongSupplier uhr;
    private final BossBar leiste = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
    private final Set<UUID> mitLeiste = ConcurrentHashMap.newKeySet();
    private final Set<UUID> begruesst = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> ankunft = new ConcurrentHashMap<>();
    private final Deque<UUID> warteschlange = new ConcurrentLinkedDeque<>();
    private final Set<UUID> unterwegs = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> versuche = new ConcurrentHashMap<>();
    private final Set<UUID> freigegeben = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ProbeRelease> proben = new ConcurrentHashMap<>();

    private volatile String zeitGespeichert;
    private volatile String konfigStand;
    private volatile long erledigt;
    private volatile Phase phase = Phase.KEIN;
    private volatile Instant zeit;
    private volatile long letzteWelle;
    private volatile long letzteErinnerung;
    private volatile long letzteSekunde = Long.MIN_VALUE;
    private volatile int welle;
    private ScheduledTask takt;

    private static final class ProbeRelease {
        private final BossBar leiste = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        private final long ende;
        private final long gesamt;
        private long letzteSekunde = Long.MIN_VALUE;
        private long losSeit;
        private boolean unterwegs;

        private ProbeRelease(long ende, long gesamt) {
            this.ende = ende;
            this.gesamt = gesamt;
        }
    }

    public ReleaseManager(SMPProxy plugin, Path ordner) {
        this(plugin, ordner, System::currentTimeMillis);
    }

    public ReleaseManager(SMPProxy plugin, Path ordner, LongSupplier uhr) {
        this.plugin = plugin;
        this.datei = ordner.resolve("release.yml");
        this.uhr = uhr;
    }

    public void laden() {
        zeitGespeichert = null;
        konfigStand = null;
        erledigt = 0;
        letzteErinnerung = uhr.getAsLong();
        if (Files.exists(datei)) {
            try (Reader reader = Files.newBufferedReader(datei, StandardCharsets.UTF_8)) {
                Object gelesen = new Yaml().load(reader);
                if (gelesen instanceof Map<?, ?> werte) {
                    Object roh = werte.get("zeit");
                    zeitGespeichert = roh == null ? null : String.valueOf(roh).trim();
                    Object stand = werte.get("config-stand");
                    konfigStand = stand == null ? null : String.valueOf(stand).trim();
                    Object fertig = werte.get("erledigt");
                    erledigt = fertig instanceof Number n ? n.longValue() : 0;
                }
            } catch (IOException | RuntimeException fehler) {
                plugin.log().error("release.yml konnte nicht gelesen werden", fehler);
            }
        }
        neuBerechnen();
    }

    public void neuBerechnen() {
        String ausKonfig = plugin.config().releaseZeit();
        if (zeitGespeichert != null && konfigStand != null && !konfigStand.equals(ausKonfig)) {
            zeitGespeichert = null;
            konfigStand = null;
            speichern();
        }
        String roh = zeitGespeichert != null ? zeitGespeichert : ausKonfig;
        Instant neu = roh == null || roh.isBlank() || roh.equalsIgnoreCase("aus") ? null : parsen(roh).orElse(null);
        if (neu == null && roh != null && !roh.isBlank() && !roh.equalsIgnoreCase("aus")) {
            plugin.log().warn("release.zeit '{}' ist kein gueltiges Datum (Beispiel: 2026-10-03 18:00) - kein Release geplant.", roh);
        }
        zeit = neu;
        if (neu == null) {
            phase = Phase.KEIN;
        } else if (erledigt == neu.toEpochMilli()) {
            phase = Phase.OFFEN;
        } else if (uhr.getAsLong() < neu.toEpochMilli()) {
            phase = Phase.GEPLANT;
        } else if (phase != Phase.LAEUFT) {
            phase = Phase.GEPLANT;
        }
        if (phase != Phase.LAEUFT) {
            warteschlange.clear();
            unterwegs.clear();
            versuche.clear();
            freigegeben.clear();
            welle = 0;
        }
    }

    public Optional<Instant> parsen(String text) {
        String sauber = text.trim().replaceAll("\\s+", " ");
        ZoneId zone = zone();
        for (DateTimeFormatter format : FORMATE) {
            try {
                return Optional.of(LocalDateTime.parse(sauber, format).atZone(zone).toInstant());
            } catch (DateTimeParseException ignoriert) {
            }
        }
        return Optional.empty();
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(plugin.config().releaseZeitzone());
        } catch (RuntimeException fehler) {
            return ZoneId.of("Europe/Berlin");
        }
    }

    public void starten() {
        stoppen();
        takt = plugin.proxy().getScheduler().buildTask(plugin, this::tick)
                .delay(1, TimeUnit.SECONDS).repeat(1, TimeUnit.SECONDS).schedule();
    }

    public void stoppen() {
        if (takt != null) {
            takt.cancel();
            takt = null;
        }
    }

    public Phase phase() {
        return phase;
    }

    public long jetzt() {
        return uhr.getAsLong();
    }

    public Optional<Instant> zeit() {
        return Optional.ofNullable(zeit);
    }

    public int wartende() {
        return (int) plugin.proxy().getAllPlayers().stream().filter(this::imWarteraum).count();
    }

    public int inWarteschlange() {
        return warteschlange.size();
    }

    public boolean haeltFest(Player spieler) {
        Phase jetzt = phase;
        return (jetzt == Phase.GEPLANT || jetzt == Phase.LAEUFT) && !spieler.hasPermission(BYPASS);
    }

    public boolean gesperrterServer(String server) {
        String warteraum = plugin.config().limbo();
        if (server == null || server.equalsIgnoreCase(warteraum)) {
            return false;
        }
        for (String gesperrt : plugin.config().releaseGesperrteServer()) {
            if (gesperrt.equalsIgnoreCase(server)) {
                return true;
            }
        }
        return false;
    }

    public boolean blockiert(Player spieler, String ziel) {
        UUID id = spieler.getUniqueId();
        if (freigegeben.contains(id) || probeDarf(id, ziel)) {
            return false;
        }
        return haeltFest(spieler) && gesperrterServer(ziel);
    }

    public boolean abweisen(Player spieler, String ziel) {
        if (!blockiert(spieler, ziel)) {
            return false;
        }
        spieler.sendMessage(plugin.message("release-gesperrt", "%dauer%", restText(), "%datum%", datumText()));
        return true;
    }

    private boolean probeDarf(UUID id, String ziel) {
        ProbeRelease probe = proben.get(id);
        return probe != null && probe.unterwegs && ziel != null && ziel.equalsIgnoreCase(plugin.config().releaseZielServer());
    }

    private boolean imWarteraum(Player spieler) {
        String warteraum = plugin.config().limbo();
        return !warteraum.isEmpty() && warteraum.equalsIgnoreCase(ChatRelay.serverVon(spieler));
    }

    private Optional<RegisteredServer> warteraum() {
        String name = plugin.config().limbo();
        return name.isEmpty() ? Optional.empty() : plugin.proxy().getServer(name);
    }

    @Subscribe(priority = 100)
    public void beimLogin(LoginEvent event) {
        if (!event.getResult().isAllowed() || !haeltFest(event.getPlayer())) {
            return;
        }
        String warteraum = plugin.config().limbo();
        if (warteraum().isPresent() && plugin.watcher().isOnline(warteraum)) {
            return;
        }
        event.setResult(ResultedEvent.ComponentResult.denied(
                plugin.screen("release-bildschirm", "%dauer%", restText(), "%datum%", datumText())));
    }

    @Subscribe(priority = -100)
    public void beimErstenServer(PlayerChooseInitialServerEvent event) {
        Player spieler = event.getPlayer();
        if (!haeltFest(spieler)) {
            return;
        }
        String ziel = event.getInitialServer().map(s -> s.getServerInfo().getName()).orElse(null);
        if (ziel != null && !gesperrterServer(ziel)) {
            return;
        }
        warteraum().ifPresent(event::setInitialServer);
    }

    @Subscribe(priority = -100)
    public void vorDemVerbinden(ServerPreConnectEvent event) {
        Player spieler = event.getPlayer();
        Optional<RegisteredServer> ziel = event.getResult().getServer();
        if (!event.getResult().isAllowed() || ziel.isEmpty()
                || !blockiert(spieler, ziel.get().getServerInfo().getName())) {
            return;
        }
        if (spieler.getCurrentServer().isEmpty()) {
            Optional<RegisteredServer> warteraum = warteraum();
            if (warteraum.isPresent()) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(warteraum.get()));
                return;
            }
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        spieler.sendMessage(plugin.message("release-gesperrt", "%dauer%", restText(), "%datum%", datumText()));
    }

    @Subscribe
    public void beimServerwechsel(ServerConnectedEvent event) {
        Player spieler = event.getPlayer();
        UUID id = spieler.getUniqueId();
        if (proben.containsKey(id)) {
            if (mitLeiste.remove(id)) {
                spieler.hideBossBar(leiste);
            }
            return;
        }
        boolean warteraum = event.getServer().getServerInfo().getName().equalsIgnoreCase(plugin.config().limbo());
        Phase jetzt = phase;
        if (warteraum && (jetzt == Phase.GEPLANT || jetzt == Phase.LAEUFT)) {
            ankunft.putIfAbsent(id, uhr.getAsLong());
            spieler.showBossBar(leiste);
            mitLeiste.add(id);
            if (begruesst.add(id)) {
                plugin.proxy().getScheduler().buildTask(plugin, () -> spieler.sendMessage(
                                plugin.message(jetzt == Phase.GEPLANT ? "release-warten" : "release-laeuft-warten",
                                        "%dauer%", restText(), "%datum%", datumText())))
                        .delay(1, TimeUnit.SECONDS).schedule();
            }
            return;
        }
        if (mitLeiste.remove(id)) {
            spieler.hideBossBar(leiste);
        }
    }

    @Subscribe
    public void beimTrennen(DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        mitLeiste.remove(id);
        begruesst.remove(id);
        warteschlange.remove(id);
        unterwegs.remove(id);
        proben.remove(id);
    }

    public void tick() {
        long jetzt = uhr.getAsLong();
        probenTick(jetzt);
        switch (phase) {
            case KEIN, OFFEN -> leisteAufraeumen();
            case GEPLANT -> {
                Instant ziel = zeit;
                if (ziel == null) {
                    return;
                }
                long rest = ziel.toEpochMilli() - jetzt;
                if (rest <= 0) {
                    releaseBeginnen(jetzt);
                    return;
                }
                countdownZeigen(rest, jetzt);
            }
            case LAEUFT -> wellen(jetzt);
        }
    }

    private void leisteAufraeumen() {
        if (mitLeiste.isEmpty()) {
            return;
        }
        for (UUID id : List.copyOf(mitLeiste)) {
            plugin.proxy().getPlayer(id).ifPresent(p -> p.hideBossBar(leiste));
            mitLeiste.remove(id);
        }
    }

    private List<Player> imWarteraum() {
        List<Player> liste = new ArrayList<>();
        for (Player spieler : plugin.proxy().getAllPlayers()) {
            if (imWarteraum(spieler) && !proben.containsKey(spieler.getUniqueId())) {
                liste.add(spieler);
            }
        }
        return liste;
    }

    private void leisteZeigen(List<Player> spieler) {
        for (Player p : spieler) {
            if (mitLeiste.add(p.getUniqueId())) {
                p.showBossBar(leiste);
            }
            ankunft.putIfAbsent(p.getUniqueId(), uhr.getAsLong());
        }
    }

    private void countdownZeigen(long rest, long jetzt) {
        List<Player> wartend = imWarteraum();
        leisteZeigen(wartend);
        long sekunden = (rest + 999) / 1000;
        int finale = plugin.config().releaseFinaleSekunden();
        leiste.name(plugin.screen("release-bossbar", "%dauer%", uhrzeitText(rest), "%datum%", datumText()));
        if (rest > STUNDE) {
            leiste.progress(1f);
            leiste.color(BossBar.Color.PURPLE);
        } else {
            leiste.progress(Math.max(0f, Math.min(1f, rest / (float) STUNDE)));
            leiste.color(rest <= MINUTE ? (sekunden <= finale ? BossBar.Color.RED : BossBar.Color.YELLOW) : BossBar.Color.BLUE);
        }
        if (sekunden == letzteSekunde) {
            return;
        }
        letzteSekunde = sekunden;
        if (sekunden <= finale) {
            Title titel = Title.title(plugin.screen("release-titel-zahl", "%sekunden%", String.valueOf(sekunden)),
                    plugin.screen("release-titel-zahl-unter"), ZAHL_ZEITEN);
            Sound ton = Sound.sound(Key.key("minecraft", "block.note_block.pling"), Sound.Source.MASTER, 1f,
                    (float) Math.min(2.0, 0.6 + (finale - sekunden) * (1.4 / Math.max(1, finale))));
            for (Player p : wartend) {
                p.showTitle(titel);
                p.playSound(ton, Sound.Emitter.self());
            }
        } else if (rest <= MINUTE) {
            Component text = plugin.screen("release-actionbar", "%sekunden%", String.valueOf(sekunden));
            wartend.forEach(p -> p.sendActionBar(text));
        }
        int alle = plugin.config().releaseErinnerungMinuten();
        if (alle > 0 && rest > MINUTE && jetzt - letzteErinnerung >= alle * MINUTE) {
            letzteErinnerung = jetzt;
            Component erinnerung = plugin.message("release-erinnerung", "%dauer%", restText(), "%datum%", datumText(),
                    "%wartende%", String.valueOf(wartend.size()));
            wartend.forEach(p -> p.sendMessage(erinnerung));
        }
    }

    private void releaseBeginnen(long jetzt) {
        phase = Phase.LAEUFT;
        welle = 0;
        letzteWelle = 0;
        List<Player> wartend = imWarteraum();
        leisteZeigen(wartend);
        wartend.sort(Comparator.comparingLong(p -> ankunft.getOrDefault(p.getUniqueId(), jetzt)));
        for (Player p : wartend) {
            if (!warteschlange.contains(p.getUniqueId())) {
                warteschlange.addLast(p.getUniqueId());
            }
        }
        Title titel = Title.title(plugin.screen("release-titel-los"), plugin.screen("release-titel-los-unter"), LOS_ZEITEN);
        Sound ton = Sound.sound(Key.key("minecraft", "ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1f);
        Sound level = Sound.sound(Key.key("minecraft", "entity.player.levelup"), Sound.Source.MASTER, 1f, 1f);
        for (Player p : wartend) {
            p.showTitle(titel);
            p.playSound(ton, Sound.Emitter.self());
            p.playSound(level, Sound.Emitter.self());
        }
        Component meldung = plugin.message("release-offen", "%server%", plugin.config().releaseZielServer());
        for (Player p : plugin.proxy().getAllPlayers()) {
            if (!imWarteraum(p)) {
                p.sendMessage(meldung);
            }
        }
        plugin.log().info("Release! {} Spieler warten im Warteraum und werden in Wellen verbunden.", warteschlange.size());
    }

    private void wellen(long jetzt) {
        for (Player p : imWarteraum()) {
            UUID id = p.getUniqueId();
            if (!warteschlange.contains(id) && !unterwegs.contains(id) && !freigegeben.contains(id)) {
                ankunft.putIfAbsent(id, jetzt);
                warteschlange.addLast(id);
                if (mitLeiste.add(id)) {
                    p.showBossBar(leiste);
                }
            }
        }
        warteschlange.removeIf(id -> plugin.proxy().getPlayer(id).map(p -> !imWarteraum(p)).orElse(true));
        if (warteschlange.isEmpty() && unterwegs.isEmpty()) {
            abschliessen();
            return;
        }
        String zielName = plugin.config().releaseZielServer();
        Optional<RegisteredServer> ziel = plugin.proxy().getServer(zielName);
        if (ziel.isEmpty() || !plugin.watcher().isOnline(zielName)) {
            leiste.name(plugin.screen("release-bossbar-server-startet", "%server%", zielName));
            leiste.color(BossBar.Color.YELLOW);
            leiste.progress(1f);
            Component text = plugin.screen("release-server-startet", "%server%", zielName);
            imWarteraum().forEach(p -> p.sendActionBar(text));
            return;
        }
        double faktor = plugin.einlass().faktor(zielName);
        if (faktor <= 0) {
            leiste.name(plugin.screen("release-bossbar-ausgelastet", "%server%", zielName));
            leiste.color(BossBar.Color.YELLOW);
            Component text = plugin.screen("release-ausgelastet", "%server%", zielName);
            imWarteraum().forEach(p -> p.sendActionBar(text));
            return;
        }
        int proWelle = Math.max(1, (int) Math.round(plugin.config().releaseProWelle() * faktor));
        long abstand = plugin.config().releaseWellenAbstandSekunden() * 1000L;
        if (letzteWelle == 0 || jetzt - letzteWelle >= abstand) {
            letzteWelle = jetzt;
            welle++;
            List<UUID> diesmal = new ArrayList<>();
            while (diesmal.size() < proWelle && !warteschlange.isEmpty()) {
                diesmal.add(warteschlange.pollFirst());
            }
            for (UUID id : diesmal) {
                plugin.proxy().getPlayer(id).ifPresent(p -> verbinden(p, ziel.get()));
            }
        }
        int gesamt = warteschlange.size();
        leiste.name(plugin.screen("release-bossbar-wellen", "%welle%", String.valueOf(Math.max(1, welle)),
                "%wartende%", String.valueOf(gesamt)));
        leiste.color(BossBar.Color.GREEN);
        int start = Math.max(1, gesamt + unterwegs.size());
        leiste.progress(Math.max(0f, Math.min(1f, gesamt / (float) start)));
        int position = 0;
        long bisNaechste = Math.max(0, abstand - (jetzt - letzteWelle));
        for (UUID id : warteschlange) {
            int wellenVorher = position / proWelle;
            long sekunden = (bisNaechste + wellenVorher * abstand + 999) / 1000;
            position++;
            int anzeige = position;
            plugin.proxy().getPlayer(id).ifPresent(p -> p.sendActionBar(plugin.screen("release-position",
                    "%position%", String.valueOf(anzeige), "%sekunden%", String.valueOf(Math.max(1, sekunden)))));
        }
    }

    private void verbinden(Player spieler, RegisteredServer ziel) {
        UUID id = spieler.getUniqueId();
        freigegeben.add(id);
        unterwegs.add(id);
        plugin.einlass().durchlassen(id);
        spieler.createConnectionRequest(ziel).connect().whenComplete((ergebnis, fehler) -> {
            unterwegs.remove(id);
            boolean ok = fehler == null && ergebnis != null && ergebnis.isSuccessful();
            if (ok) {
                if (mitLeiste.remove(id)) {
                    spieler.hideBossBar(leiste);
                }
                spieler.sendMessage(plugin.message("release-willkommen", "%server%", ziel.getServerInfo().getName()));
                return;
            }
            if (!spieler.isActive()) {
                return;
            }
            int anzahl = versuche.merge(id, 1, Integer::sum);
            if (anzahl < MAX_VERSUCHE) {
                freigegeben.remove(id);
                warteschlange.addFirst(id);
            } else {
                spieler.sendMessage(plugin.message("release-verbindung-fehlgeschlagen", "%server%", ziel.getServerInfo().getName()));
            }
        });
    }

    private void abschliessen() {
        Instant fertig = zeit;
        phase = Phase.OFFEN;
        erledigt = fertig == null ? 0 : fertig.toEpochMilli();
        speichern();
        leisteAufraeumen();
        warteschlange.clear();
        versuche.clear();
        freigegeben.clear();
        plugin.log().info("Release abgeschlossen - alle Wellen sind durch, ab jetzt laeuft alles normal.");
    }

    public void zeitSetzen(Instant neu) {
        zeitGespeichert = neu == null ? "aus" : SPEICHERN.format(neu.atZone(zone()).toLocalDateTime());
        konfigStand = plugin.config().releaseZeit();
        if (neu != null && erledigt == neu.toEpochMilli()) {
            erledigt = 0;
        }
        phase = Phase.KEIN;
        neuBerechnen();
        letzteSekunde = Long.MIN_VALUE;
        letzteErinnerung = 0;
        begruesst.clear();
        speichern();
        if (phase == Phase.GEPLANT) {
            Component meldung = plugin.message("release-neue-zeit", "%dauer%", restText(), "%datum%", datumText());
            imWarteraum().forEach(p -> p.sendMessage(meldung));
        } else if (phase == Phase.KEIN) {
            leisteAufraeumen();
            Component meldung = plugin.message("release-abgesagt");
            imWarteraum().forEach(p -> p.sendMessage(meldung));
        }
    }

    public void konfigNeuGeladen() {
        neuBerechnen();
    }

    private synchronized void speichern() {
        Map<String, Object> werte = new LinkedHashMap<>();
        if (zeitGespeichert != null) {
            werte.put("zeit", zeitGespeichert);
            werte.put("config-stand", konfigStand == null ? "" : konfigStand);
        }
        werte.put("erledigt", erledigt);
        try {
            Files.createDirectories(datei.getParent());
            DumperOptions optionen = new DumperOptions();
            optionen.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Path neu = datei.resolveSibling("release.yml.neu");
            try (Writer writer = Files.newBufferedWriter(neu, StandardCharsets.UTF_8)) {
                writer.write("# Release-Stand - von /release geschrieben. zeit ueberschreibt release.zeit aus der\n"
                        + "# config.yml (\"aus\" = kein Release), bis dort ein neuer Termin eingetragen wird.\n"
                        + "# erledigt = dieser Release ist schon gelaufen.\n\n");
                new Yaml(optionen).dump(werte, writer);
            }
            Files.move(neu, datei, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException fehler) {
            plugin.log().error("release.yml konnte nicht geschrieben werden", fehler);
        }
    }

    public boolean probeStarten(Player spieler, int sekunden) {
        UUID id = spieler.getUniqueId();
        if (proben.containsKey(id)) {
            return false;
        }
        long jetzt = uhr.getAsLong();
        ProbeRelease probe = new ProbeRelease(jetzt + sekunden * 1000L, sekunden * 1000L);
        proben.put(id, probe);
        if (mitLeiste.remove(id)) {
            spieler.hideBossBar(leiste);
        }
        spieler.showBossBar(probe.leiste);
        Optional<RegisteredServer> warteraum = warteraum();
        if (warteraum.isPresent() && !imWarteraum(spieler) && plugin.watcher().isOnline(plugin.config().limbo())) {
            spieler.createConnectionRequest(warteraum.get()).fireAndForget();
        }
        probeZeigen(spieler, probe, jetzt);
        return true;
    }

    public boolean probeStoppen(Player spieler) {
        ProbeRelease probe = proben.remove(spieler.getUniqueId());
        if (probe == null) {
            return false;
        }
        spieler.hideBossBar(probe.leiste);
        return true;
    }

    public boolean probeLaeuft(UUID spieler) {
        return proben.containsKey(spieler);
    }

    private void probenTick(long jetzt) {
        for (Map.Entry<UUID, ProbeRelease> eintrag : proben.entrySet()) {
            Optional<Player> spieler = plugin.proxy().getPlayer(eintrag.getKey());
            if (spieler.isEmpty()) {
                proben.remove(eintrag.getKey());
                continue;
            }
            probeZeigen(spieler.get(), eintrag.getValue(), jetzt);
        }
    }

    private void probeZeigen(Player spieler, ProbeRelease probe, long jetzt) {
        long rest = probe.ende - jetzt;
        int finale = plugin.config().releaseFinaleSekunden();
        if (rest > 0) {
            long sekunden = (rest + 999) / 1000;
            probe.leiste.name(plugin.screen("release-bossbar-probe", "%dauer%", uhrzeitText(rest)));
            probe.leiste.progress(Math.max(0f, Math.min(1f, rest / (float) probe.gesamt)));
            probe.leiste.color(sekunden <= finale ? BossBar.Color.RED : BossBar.Color.PURPLE);
            if (sekunden != probe.letzteSekunde) {
                probe.letzteSekunde = sekunden;
                if (sekunden <= finale) {
                    spieler.showTitle(Title.title(plugin.screen("release-titel-zahl", "%sekunden%", String.valueOf(sekunden)),
                            plugin.screen("release-titel-zahl-unter"), ZAHL_ZEITEN));
                    spieler.playSound(Sound.sound(Key.key("minecraft", "block.note_block.pling"), Sound.Source.MASTER, 1f,
                            (float) Math.min(2.0, 0.6 + (finale - sekunden) * (1.4 / Math.max(1, finale)))), Sound.Emitter.self());
                } else {
                    spieler.sendActionBar(plugin.screen("release-actionbar-probe", "%sekunden%", String.valueOf(sekunden)));
                }
            }
            return;
        }
        if (probe.losSeit == 0) {
            probe.losSeit = jetzt;
            spieler.showTitle(Title.title(plugin.screen("release-titel-los"), plugin.screen("release-titel-los-unter"), LOS_ZEITEN));
            spieler.playSound(Sound.sound(Key.key("minecraft", "ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self());
            spieler.playSound(Sound.sound(Key.key("minecraft", "entity.player.levelup"), Sound.Source.MASTER, 1f, 1f),
                    Sound.Emitter.self());
            probe.leiste.name(plugin.screen("release-bossbar-wellen", "%welle%", "1", "%wartende%", "0"));
            probe.leiste.color(BossBar.Color.GREEN);
            probe.leiste.progress(1f);
            spieler.sendActionBar(plugin.screen("release-position", "%position%", "1",
                    "%sekunden%", String.valueOf(plugin.config().releaseWellenAbstandSekunden())));
            return;
        }
        if (probe.unterwegs || jetzt - probe.losSeit < plugin.config().releaseWellenAbstandSekunden() * 1000L) {
            return;
        }
        probe.unterwegs = true;
        String zielName = plugin.config().releaseZielServer();
        Optional<RegisteredServer> ziel = plugin.proxy().getServer(zielName);
        if (ziel.isEmpty() || ChatRelay.serverVon(spieler).equalsIgnoreCase(zielName)) {
            proben.remove(spieler.getUniqueId());
            spieler.hideBossBar(probe.leiste);
            spieler.sendMessage(plugin.message("testrelease-fertig", "%server%", zielName));
            return;
        }
        plugin.einlass().durchlassen(spieler.getUniqueId());
        spieler.createConnectionRequest(ziel.get()).connect().whenComplete((ergebnis, fehler) -> {
            proben.remove(spieler.getUniqueId());
            spieler.hideBossBar(probe.leiste);
            boolean ok = fehler == null && ergebnis != null && ergebnis.isSuccessful();
            spieler.sendMessage(plugin.message(ok ? "testrelease-fertig" : "release-verbindung-fehlgeschlagen",
                    "%server%", zielName));
        });
    }

    public String restText() {
        Instant ziel = zeit;
        if (ziel == null) {
            return "-";
        }
        return dauerText(Math.max(0, ziel.toEpochMilli() - uhr.getAsLong()));
    }

    public String datumText() {
        Instant ziel = zeit;
        return ziel == null ? "-" : ANZEIGE.format(ziel.atZone(zone()));
    }

    public static String dauerText(long millis) {
        long sekunden = (millis + 999) / 1000;
        long tage = sekunden / 86_400;
        long stunden = sekunden % 86_400 / 3_600;
        long minuten = sekunden % 3_600 / 60;
        long rest = sekunden % 60;
        StringBuilder text = new StringBuilder();
        if (tage > 0) {
            text.append(tage).append(tage == 1 ? " Tag " : " Tage ");
        }
        if (tage > 0 || stunden > 0) {
            text.append(stunden).append(" Std. ");
        }
        if (tage == 0 && (stunden > 0 || minuten > 0)) {
            text.append(minuten).append(" Min. ");
        }
        if (tage == 0 && stunden == 0) {
            text.append(rest).append(" Sek.");
        }
        return text.toString().trim();
    }

    public static String uhrzeitText(long millis) {
        long sekunden = (millis + 999) / 1000;
        long tage = sekunden / 86_400;
        long stunden = sekunden % 86_400 / 3_600;
        long minuten = sekunden % 3_600 / 60;
        long rest = sekunden % 60;
        String uhrzeit = String.format(Locale.ROOT, "%02d:%02d:%02d", stunden, minuten, rest);
        return tage > 0 ? tage + "d " + uhrzeit : uhrzeit;
    }
}
