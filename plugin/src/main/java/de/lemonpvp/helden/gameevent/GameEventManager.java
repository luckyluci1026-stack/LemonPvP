package de.lemonpvp.helden.gameevent;

import de.lemonpvp.helden.HeldenPlugin;
import de.lemonpvp.helden.gameevent.impl.BlutmondEvent;
import de.lemonpvp.helden.gameevent.impl.KopfgeldEvent;
import de.lemonpvp.helden.gameevent.impl.ZitronenregenEvent;
import de.lemonpvp.helden.util.Text;
import de.lemonpvp.helden.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Startet, taktet und beendet die Projekt-Events. */
public final class GameEventManager {

    private final HeldenPlugin plugin;
    private final Map<String, GameEvent> events = new LinkedHashMap<>();

    private BukkitTask task;
    private GameEvent active;
    private int elapsedSeconds;
    private int secondsUntilNext;
    private int rotationIndex;
    private boolean warmupAnnounced;

    public GameEventManager(HeldenPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
        events.clear();
        register(new BlutmondEvent(plugin));
        register(new ZitronenregenEvent(plugin));
        register(new KopfgeldEvent(plugin));
        plugin.getLogger().info("Events registriert: " + events.size());
    }

    public void register(GameEvent event) {
        events.put(event.id().toLowerCase(Locale.ROOT), event);
    }

    public GameEvent get(String id) {
        return id == null ? null : events.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return events.keySet();
    }

    public int size() {
        return events.size();
    }

    public GameEvent active() {
        return active;
    }

    public boolean isRunning() {
        return active != null;
    }

    public void start() {
        stopTask();
        resetTimer();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Timer auf das konfigurierte Intervall zuruecksetzen. */
    public void resetTimer() {
        secondsUntilNext = plugin.settings().eventIntervalMinutes() * 60;
        warmupAnnounced = false;
    }

    private void tick() {
        if (active != null) {
            elapsedSeconds++;
            active.onTick(elapsedSeconds);
            if (elapsedSeconds >= active.durationSeconds()) {
                stopEvent();
            }
            return;
        }

        if (!plugin.settings().eventsEnabled() || events.isEmpty()) {
            return;
        }

        secondsUntilNext--;
        int warmup = plugin.settings().eventWarmupSeconds();
        if (!warmupAnnounced && warmup > 0 && secondsUntilNext == warmup) {
            GameEvent next = peekNext();
            if (next != null) {
                warmupAnnounced = true;
                plugin.messages().broadcastRaw("events.warmup",
                        "%event%", next.displayName(),
                        "%seconds%", warmup);
            }
        }
        if (secondsUntilNext <= 0) {
            startNextFromRotation();
        }
    }

    private GameEvent peekNext() {
        List<String> rotation = plugin.settings().eventRotation();
        if (rotation.isEmpty()) {
            return null;
        }
        return get(rotation.get(rotationIndex % rotation.size()));
    }

    private void startNextFromRotation() {
        List<String> rotation = plugin.settings().eventRotation();
        if (rotation.isEmpty()) {
            resetTimer();
            return;
        }

        // Ein Event darf ablehnen (z. B. Kopfgeld ohne genug Spieler) - dann
        // probieren wir der Reihe nach das naechste.
        for (int attempt = 0; attempt < rotation.size(); attempt++) {
            String id = rotation.get(rotationIndex % rotation.size());
            rotationIndex++;
            if (startEvent(id)) {
                return;
            }
        }
        resetTimer();
    }

    /** Startet ein Event sofort. */
    public boolean startEvent(String id) {
        GameEvent event = get(id);
        if (event == null || active != null) {
            return false;
        }
        if (!event.onStart()) {
            resetTimer();
            return false;
        }

        active = event;
        elapsedSeconds = 0;
        plugin.messages().broadcastRaw("events.started",
                "%event%", event.displayName(),
                "%description%", event.description());
        plugin.hud().updateAll();
        return true;
    }

    public void stopEvent() {
        if (active == null) {
            return;
        }
        GameEvent finished = active;
        active = null;
        elapsedSeconds = 0;
        finished.onStop();
        resetTimer();
        plugin.messages().broadcastRaw("events.ended", "%event%", finished.displayName());
        plugin.hud().updateAll();
    }

    /** Zeile fuer das Scoreboard. */
    public String statusLine() {
        if (active == null) {
            return Text.color(plugin.messages().raw("events.none"));
        }
        int remaining = Math.max(0, active.durationSeconds() - elapsedSeconds);
        return Text.color(Text.replace(plugin.messages().raw("events.running"),
                "%event%", active.displayName(),
                "%time%", TimeUtil.clock(remaining)));
    }

    /** Schadensfaktor, den der Blutmond auf PvP legt. */
    public double pvpDamageMultiplier() {
        if (active instanceof BlutmondEvent blutmond) {
            return blutmond.damageMultiplier();
        }
        return 1.0;
    }

    /** Prueft und zahlt ein laufendes Kopfgeld aus. */
    public boolean claimBounty(Player killer, Player victim) {
        return active instanceof KopfgeldEvent kopfgeld && kopfgeld.claim(killer, victim);
    }
}
