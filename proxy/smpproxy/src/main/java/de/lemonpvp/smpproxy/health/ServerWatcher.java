package de.lemonpvp.smpproxy.health;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.lemonpvp.smpproxy.config.ProxyConfig;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pingt alle Backend-Server regelmäßig an und merkt sich, welche laufen.
 *
 * Daraus ergibt sich alles Weitere: Wohin ein Spieler beim Verbinden kommt,
 * ob ein Rauswurf ein echter Kick oder ein Absturz war und wann jemand aus
 * dem Limbo zurückgeholt werden kann.
 */
public final class ServerWatcher {

    private final ProxyServer proxy;
    private final ProxyConfig config;
    private final Logger log;

    /** Servername -> läuft gerade? */
    private final Map<String, Boolean> online = new ConcurrentHashMap<>();
    /** Servername -> wie oft hintereinander erfolgreich gepingt */
    private final Map<String, AtomicInteger> streak = new ConcurrentHashMap<>();

    public ServerWatcher(ProxyServer proxy, ProxyConfig config, Logger log) {
        this.proxy = proxy;
        this.config = config;
        this.log = log;
    }

    /** Ein Durchlauf: jeden bekannten Server anpingen. */
    public void tick() {
        for (RegisteredServer server : proxy.getAllServers()) {
            pingAsync(server);
        }
    }

    private void pingAsync(RegisteredServer server) {
        String name = server.getServerInfo().getName();
        server.ping()
                .orTimeout(config.pingTimeout(), TimeUnit.SECONDS)
                .whenComplete((ping, error) -> record(name, error == null));
    }

    private void record(String name, boolean up) {
        Boolean before = online.put(name, up);
        AtomicInteger counter = streak.computeIfAbsent(name, key -> new AtomicInteger());
        if (up) {
            counter.incrementAndGet();
        } else {
            counter.set(0);
        }
        if (before != null && before != up) {
            if (up) {
                log.info("Server '{}' ist wieder erreichbar.", name);
            } else {
                log.warn("Server '{}' antwortet nicht mehr.", name);
            }
        }
    }

    /**
     * Fragt einen Server sofort an und wartet kurz auf die Antwort.
     * Wird beim Rauswurf gebraucht: Antwortet der Server noch, war es ein
     * echter Kick (z.B. ein Bann) - antwortet er nicht, ist er abgestürzt.
     */
    public boolean pingNow(String name, long millis) {
        Optional<RegisteredServer> server = proxy.getServer(name);
        if (server.isEmpty()) {
            return false;
        }
        boolean up;
        try {
            server.get().ping().get(millis, TimeUnit.MILLISECONDS);
            up = true;
        } catch (Exception ex) {
            up = false;
        }
        record(name, up);
        return up;
    }

    /** Läuft der Server? Unbekannte Server gelten als erreichbar. */
    public boolean isOnline(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return online.getOrDefault(name, Boolean.TRUE);
    }

    /** Wurde der Server oft genug hintereinander erreicht? */
    public boolean isStable(String name) {
        AtomicInteger counter = streak.get(name);
        return counter != null && counter.get() >= config.returnConfirmations();
    }

    public int playersOn(String name) {
        return proxy.getServer(name)
                .map(server -> server.getPlayersConnected().size())
                .orElse(0);
    }

    public void reset() {
        online.clear();
        streak.clear();
    }
}
