package de.lemonpvp.bettersmp.leistung;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

public final class Sichtweite implements Listener {

    private static final long PRUEF_TICKS = 100L;
    private static final long ANHEBEN_NACH_MILLIS = 30_000L;
    private static final int MAX_REDUKTION = 12;

    private final BetterSMP plugin;
    private final LongSupplier uhr;
    private final DoubleSupplier mspt;
    private final Map<UUID, Integer> gesetzt = new HashMap<>();
    private BukkitTask takt;
    private int reduktion;
    private long entspanntSeit = -1;

    public Sichtweite(BetterSMP plugin) {
        this(plugin, System::currentTimeMillis, Bukkit::getAverageTickTime);
    }

    public Sichtweite(BetterSMP plugin, LongSupplier uhr, DoubleSupplier mspt) {
        this.plugin = plugin;
        this.uhr = uhr;
        this.mspt = mspt;
    }

    public void start() {
        stop();
        takt = Bukkit.getScheduler().runTaskTimer(plugin, this::pruefen, PRUEF_TICKS, PRUEF_TICKS);
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            anwenden(spieler);
        }
    }

    public void stop() {
        if (takt != null) {
            takt.cancel();
            takt = null;
        }
        reduktion = 0;
        entspanntSeit = -1;
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            if (gesetzt.containsKey(spieler.getUniqueId())) {
                zuruecksetzen(spieler);
            }
        }
        gesetzt.clear();
    }

    public int reduktion() {
        return reduktion;
    }

    public static boolean bedrock(Player spieler) {
        return spieler.getUniqueId().getMostSignificantBits() == 0L;
    }

    private boolean dynamisch() {
        return plugin.getConfig().getBoolean("leistung.dynamische-sichtweite", true);
    }

    private int minimum() {
        return Math.max(2, plugin.getConfig().getInt("leistung.min-sichtweite", 5));
    }

    private int bedrockWeite() {
        return plugin.getConfig().getInt("leistung.bedrock-sichtweite", 6);
    }

    private double senkenAb() {
        return plugin.getConfig().getDouble("leistung.senken-ab-mspt", 45);
    }

    private double anhebenUnter() {
        return Math.min(senkenAb(), plugin.getConfig().getDouble("leistung.anheben-unter-mspt", 30));
    }

    public int zielWeite(Player spieler) {
        int basis = spieler.getWorld().getViewDistance();
        int weite = basis;
        int bedrock = bedrockWeite();
        if (bedrock > 0 && bedrock(spieler)) {
            weite = Math.min(weite, Math.max(2, bedrock));
        }
        if (reduktion > 0) {
            weite = Math.max(Math.min(minimum(), weite), weite - reduktion);
        }
        return Math.max(2, Math.min(basis, weite));
    }

    public void anwenden(Player spieler) {
        int basis = spieler.getWorld().getViewDistance();
        int ziel = zielWeite(spieler);
        Integer vorher = gesetzt.get(spieler.getUniqueId());
        if (ziel >= basis) {
            if (vorher != null) {
                zuruecksetzen(spieler);
                gesetzt.remove(spieler.getUniqueId());
            }
            return;
        }
        if (vorher != null && vorher == ziel) {
            return;
        }
        spieler.setViewDistance(ziel);
        spieler.setSendViewDistance(ziel);
        gesetzt.put(spieler.getUniqueId(), ziel);
    }

    private void zuruecksetzen(Player spieler) {
        spieler.setViewDistance(-1);
        spieler.setSendViewDistance(-1);
    }

    public void pruefen() {
        if (!dynamisch()) {
            if (reduktion != 0) {
                reduktion = 0;
                entspanntSeit = -1;
                alleAnwenden();
            }
            return;
        }
        double jetztMspt = mspt.getAsDouble();
        long jetzt = uhr.getAsLong();
        if (jetztMspt >= senkenAb()) {
            entspanntSeit = -1;
            if (reduktion < MAX_REDUKTION) {
                reduktion++;
                plugin.getLogger().info(String.format(java.util.Locale.ROOT,
                        "Server ausgelastet (%.1f ms/Tick) - Sichtweite voruebergehend um %d gesenkt.", jetztMspt, reduktion));
                alleAnwenden();
            }
            return;
        }
        if (jetztMspt >= anhebenUnter() || reduktion == 0) {
            entspanntSeit = -1;
            return;
        }
        if (entspanntSeit < 0) {
            entspanntSeit = jetzt;
            return;
        }
        if (jetzt - entspanntSeit >= ANHEBEN_NACH_MILLIS) {
            reduktion--;
            entspanntSeit = jetzt;
            plugin.getLogger().info(reduktion == 0
                    ? "Server wieder entspannt - volle Sichtweite."
                    : "Server entspannter - Sichtweite wieder um 1 hoeher (noch " + reduktion + " gesenkt).");
            alleAnwenden();
        }
    }

    private void alleAnwenden() {
        for (Player spieler : Bukkit.getOnlinePlayers()) {
            anwenden(spieler);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimBeitreten(PlayerJoinEvent event) {
        anwenden(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimWeltwechsel(PlayerChangedWorldEvent event) {
        gesetzt.remove(event.getPlayer().getUniqueId());
        zuruecksetzen(event.getPlayer());
        anwenden(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        gesetzt.remove(event.getPlayer().getUniqueId());
    }
}
