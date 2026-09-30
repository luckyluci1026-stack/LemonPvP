package de.lemonpvp.smpproxy.einlass;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import de.lemonpvp.smpproxy.SMPProxy;
import de.lemonpvp.smpproxy.netzwerk.ChatRelay;
import net.kyori.adventure.text.Component;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class Einlass {

    public static final MinecraftChannelIdentifier KANAL = MinecraftChannelIdentifier.create("smpproxy", "last");
    public static final String BYPASS = "smpproxy.einlass.bypass";

    private static final long LAST_GUELTIG_MILLIS = 10_000L;
    private static final long TICKET_GUELTIG_MILLIS = 30_000L;
    private static final int MAX_VERSUCHE = 3;

    public record Last(double mspt, double tps, long zeit) {
    }

    private record Eintrag(UUID spieler, String server, Consumer<Player> danach) {
    }

    private final SMPProxy plugin;
    private final LongSupplier uhr;
    private final Map<String, Deque<Eintrag>> reihen = new ConcurrentHashMap<>();
    private final Map<UUID, Eintrag> wartend = new ConcurrentHashMap<>();
    private final Map<String, double[]> guthaben = new ConcurrentHashMap<>();
    private final Map<String, Last> lasten = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tickets = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> versuche = new ConcurrentHashMap<>();
    private volatile long letzteAnzeige;
    private ScheduledTask takt;

    public Einlass(SMPProxy plugin) {
        this(plugin, System::currentTimeMillis);
    }

    public Einlass(SMPProxy plugin, LongSupplier uhr) {
        this.plugin = plugin;
        this.uhr = uhr;
    }

    public void starten() {
        stoppen();
        takt = plugin.proxy().getScheduler().buildTask(plugin, this::tick)
                .delay(250, TimeUnit.MILLISECONDS).repeat(250, TimeUnit.MILLISECONDS).schedule();
    }

    public void stoppen() {
        if (takt != null) {
            takt.cancel();
            takt = null;
        }
    }

    private static String schluessel(String server) {
        return server.toLowerCase(Locale.ROOT);
    }

    public boolean geschuetzt(String server) {
        if (server == null || !plugin.config().einlassAktiv()) {
            return false;
        }
        for (String name : plugin.config().einlassServer()) {
            if (name.equalsIgnoreCase(server)) {
                return true;
            }
        }
        return false;
    }

    public Optional<Last> last(String server) {
        Last last = server == null ? null : lasten.get(schluessel(server));
        if (last == null || uhr.getAsLong() - last.zeit() > LAST_GUELTIG_MILLIS) {
            return Optional.empty();
        }
        return Optional.of(last);
    }

    public double faktor(String server) {
        Optional<Last> last = last(server);
        if (last.isEmpty()) {
            return 1.0;
        }
        double mspt = last.get().mspt();
        if (mspt >= plugin.config().einlassPauseAbMspt()) {
            return 0.0;
        }
        if (mspt >= plugin.config().einlassLangsamerAbMspt()) {
            return 0.5;
        }
        return 1.0;
    }

    private synchronized boolean platzNehmen(String server) {
        long jetzt = uhr.getAsLong();
        double proSekunde = plugin.config().einlassProSekunde();
        double faktor = faktor(server);
        double[] stand = guthaben.computeIfAbsent(schluessel(server), k -> new double[]{Math.max(1, proSekunde), jetzt});
        stand[0] = Math.min(Math.max(1, proSekunde), stand[0] + Math.max(0, jetzt - stand[1]) / 1000.0 * proSekunde * faktor);
        stand[1] = jetzt;
        if (faktor <= 0 || stand[0] < 1) {
            return false;
        }
        stand[0] -= 1;
        return true;
    }

    public void durchlassen(UUID spieler) {
        tickets.put(spieler, uhr.getAsLong() + TICKET_GUELTIG_MILLIS);
    }

    private boolean ticketNehmen(UUID spieler) {
        Long bis = tickets.remove(spieler);
        return bis != null && bis >= uhr.getAsLong();
    }

    private boolean ausnahme(Player spieler, RegisteredServer vorher) {
        if (spieler.hasPermission(BYPASS)) {
            return true;
        }
        if (vorher == null) {
            return false;
        }
        String name = vorher.getServerInfo().getName();
        for (String ausnahme : plugin.config().einlassAusnahmenVon()) {
            if (ausnahme.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean darfRein(Player spieler, String server, RegisteredServer vorher) {
        if (!geschuetzt(server) || ausnahme(spieler, vorher) || ticketNehmen(spieler.getUniqueId())) {
            return true;
        }
        Deque<Eintrag> reihe = reihen.get(schluessel(server));
        if (reihe != null && !reihe.isEmpty()) {
            return false;
        }
        return platzNehmen(server);
    }

    public synchronized boolean mussWarten(Player spieler, String server) {
        RegisteredServer vorher = spieler.getCurrentServer().map(ServerConnection::getServer).orElse(null);
        if (!darfRein(spieler, server, vorher)) {
            return true;
        }
        if (geschuetzt(server)) {
            durchlassen(spieler.getUniqueId());
        }
        return false;
    }

    public synchronized int anstellen(Player spieler, String server, Consumer<Player> danach) {
        UUID id = spieler.getUniqueId();
        Eintrag alt = wartend.get(id);
        if (alt != null && alt.server().equalsIgnoreCase(server)) {
            if (danach != null && alt.danach() == null) {
                Eintrag ersatz = new Eintrag(id, alt.server(), danach);
                Deque<Eintrag> reihe = reihen.get(schluessel(server));
                List<Eintrag> neu = new ArrayList<>();
                for (Eintrag eintrag : reihe) {
                    neu.add(eintrag.spieler().equals(id) ? ersatz : eintrag);
                }
                reihe.clear();
                reihe.addAll(neu);
                wartend.put(id, ersatz);
            }
            return position(id);
        }
        if (alt != null) {
            entfernen(id);
        }
        Eintrag eintrag = new Eintrag(id, server, danach);
        reihen.computeIfAbsent(schluessel(server), k -> new ConcurrentLinkedDeque<>()).addLast(eintrag);
        wartend.put(id, eintrag);
        return position(id);
    }

    public int anstellenUndMelden(Player spieler, String server, Consumer<Player> danach) {
        int platz = anstellen(spieler, server, danach);
        spieler.sendMessage(plugin.message("einlass-warteschlange", "%server%", server, "%platz%", String.valueOf(platz)));
        return platz;
    }

    public int position(UUID spieler) {
        Eintrag eintrag = wartend.get(spieler);
        if (eintrag == null) {
            return 0;
        }
        Deque<Eintrag> reihe = reihen.get(schluessel(eintrag.server()));
        if (reihe == null) {
            return 0;
        }
        int platz = 0;
        for (Eintrag anderer : reihe) {
            platz++;
            if (anderer.spieler().equals(spieler)) {
                return platz;
            }
        }
        return 0;
    }

    public Optional<String> wartetAuf(UUID spieler) {
        Eintrag eintrag = wartend.get(spieler);
        return eintrag == null ? Optional.empty() : Optional.of(eintrag.server());
    }

    public int wartende(String server) {
        Deque<Eintrag> reihe = reihen.get(schluessel(server));
        return reihe == null ? 0 : reihe.size();
    }

    public synchronized void entfernen(UUID spieler) {
        Eintrag eintrag = wartend.remove(spieler);
        if (eintrag == null) {
            return;
        }
        Deque<Eintrag> reihe = reihen.get(schluessel(eintrag.server()));
        if (reihe != null) {
            reihe.removeIf(anderer -> anderer.spieler().equals(spieler));
        }
    }

    private synchronized Eintrag naechster(Deque<Eintrag> reihe) {
        while (!reihe.isEmpty()) {
            Eintrag eintrag = reihe.peekFirst();
            Optional<Player> spieler = plugin.proxy().getPlayer(eintrag.spieler());
            if (spieler.isEmpty() || !spieler.get().isActive()
                    || eintrag.server().equalsIgnoreCase(ChatRelay.serverVon(spieler.get()))) {
                reihe.pollFirst();
                wartend.remove(eintrag.spieler());
                continue;
            }
            if (!platzNehmen(eintrag.server())) {
                return null;
            }
            reihe.pollFirst();
            wartend.remove(eintrag.spieler());
            return eintrag;
        }
        return null;
    }

    public void tick() {
        for (Deque<Eintrag> reihe : reihen.values()) {
            Eintrag erster = reihe.peekFirst();
            if (erster == null || !plugin.watcher().isOnline(erster.server())) {
                continue;
            }
            Eintrag eintrag;
            while ((eintrag = naechster(reihe)) != null) {
                Eintrag dran = eintrag;
                plugin.proxy().getPlayer(dran.spieler()).ifPresent(spieler -> verbinden(spieler, dran));
            }
        }
        long jetzt = uhr.getAsLong();
        if (jetzt - letzteAnzeige >= 1000) {
            letzteAnzeige = jetzt;
            anzeigen();
        }
    }

    private void verbinden(Player spieler, Eintrag eintrag) {
        Optional<RegisteredServer> ziel = plugin.proxy().getServer(eintrag.server());
        if (ziel.isEmpty()) {
            return;
        }
        UUID id = spieler.getUniqueId();
        durchlassen(id);
        spieler.createConnectionRequest(ziel.get()).connect().whenComplete((ergebnis, fehler) -> {
            tickets.remove(id);
            ConnectionRequestBuilder.Status status = ergebnis == null ? null : ergebnis.getStatus();
            if (fehler == null && ergebnis != null && (ergebnis.isSuccessful()
                    || status == ConnectionRequestBuilder.Status.ALREADY_CONNECTED)) {
                versuche.remove(id);
                if (eintrag.danach() != null) {
                    eintrag.danach().accept(spieler);
                }
                return;
            }
            if (!spieler.isActive() || status == ConnectionRequestBuilder.Status.CONNECTION_CANCELLED) {
                versuche.remove(id);
                return;
            }
            boolean serverWeg = !plugin.watcher().isOnline(eintrag.server());
            boolean nochmal = fehler != null || status == ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS || serverWeg;
            int anzahl = versuche.merge(id, 1, Integer::sum);
            if (nochmal && anzahl < MAX_VERSUCHE) {
                synchronized (this) {
                    if (!wartend.containsKey(id)) {
                        reihen.computeIfAbsent(schluessel(eintrag.server()), k -> new ConcurrentLinkedDeque<>()).addFirst(eintrag);
                        wartend.put(id, eintrag);
                    }
                }
                return;
            }
            versuche.remove(id);
            Component grund = ergebnis == null ? null : ergebnis.getReasonComponent().orElse(null);
            spieler.sendMessage(grund != null ? grund : plugin.message("switch-failed", "%server%", eintrag.server()));
        });
    }

    private void anzeigen() {
        double proSekunde = plugin.config().einlassProSekunde();
        for (Deque<Eintrag> reihe : reihen.values()) {
            int platz = 0;
            for (Eintrag eintrag : reihe) {
                platz++;
                Optional<Player> spieler = plugin.proxy().getPlayer(eintrag.spieler());
                if (spieler.isEmpty()) {
                    continue;
                }
                boolean online = plugin.watcher().isOnline(eintrag.server());
                double faktor = online ? faktor(eintrag.server()) : 0;
                Component text;
                if (!online) {
                    text = plugin.screen("einlass-startet", "%server%", eintrag.server(), "%platz%", String.valueOf(platz));
                } else if (faktor <= 0) {
                    text = plugin.screen("einlass-pause", "%server%", eintrag.server(), "%platz%", String.valueOf(platz));
                } else {
                    long sekunden = Math.max(1, (long) Math.ceil(platz / (proSekunde * faktor)));
                    text = plugin.screen("einlass-position", "%server%", eintrag.server(), "%platz%", String.valueOf(platz),
                            "%sekunden%", String.valueOf(sekunden));
                }
                spieler.get().sendActionBar(text);
            }
        }
    }

    private Optional<RegisteredServer> warteraum() {
        String name = plugin.config().limbo();
        if (name.isEmpty() || !plugin.watcher().isOnline(name)) {
            return Optional.empty();
        }
        return plugin.proxy().getServer(name);
    }

    @Subscribe(priority = -200)
    public void vorDemVerbinden(ServerPreConnectEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }
        Optional<RegisteredServer> ziel = event.getResult().getServer();
        if (ziel.isEmpty()) {
            return;
        }
        String name = ziel.get().getServerInfo().getName();
        Player spieler = event.getPlayer();
        if (darfRein(spieler, name, event.getPreviousServer())) {
            return;
        }
        int platz = anstellen(spieler, name, null);
        if (spieler.getCurrentServer().isEmpty()) {
            Optional<RegisteredServer> warteraum = warteraum();
            if (warteraum.isEmpty()) {
                entfernen(spieler.getUniqueId());
                return;
            }
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(warteraum.get()));
            Component meldung = plugin.message("einlass-warteraum", "%server%", name, "%platz%", String.valueOf(platz));
            plugin.proxy().getScheduler().buildTask(plugin, () -> {
                if (spieler.isActive()) {
                    spieler.sendMessage(meldung);
                }
            }).delay(1, TimeUnit.SECONDS).schedule();
            return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        spieler.sendMessage(plugin.message("einlass-warteschlange", "%server%", name, "%platz%", String.valueOf(platz)));
    }

    @Subscribe
    public void beimVerbunden(ServerConnectedEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Eintrag eintrag = wartend.get(id);
        if (eintrag == null) {
            return;
        }
        String name = event.getServer().getServerInfo().getName();
        if (name.equalsIgnoreCase(eintrag.server()) || !name.equalsIgnoreCase(plugin.config().limbo())) {
            entfernen(id);
        }
    }

    @Subscribe
    public void beimTrennen(DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        entfernen(id);
        tickets.remove(id);
        versuche.remove(id);
    }

    @Subscribe
    public void beimPluginKanal(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(KANAL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection verbindung)) {
            return;
        }
        try (DataInputStream ein = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            double mspt = ein.readDouble();
            double tps = ein.readDouble();
            if (Double.isFinite(mspt) && mspt >= 0 && Double.isFinite(tps) && tps >= 0) {
                lasten.put(schluessel(verbindung.getServerInfo().getName()), new Last(mspt, tps, uhr.getAsLong()));
            }
        } catch (IOException ignoriert) {
        }
    }
}
