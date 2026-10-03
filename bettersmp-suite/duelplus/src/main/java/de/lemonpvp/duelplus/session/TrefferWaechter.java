package de.lemonpvp.duelplus.session;

import de.lemonpvp.duelplus.DuelPlus;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;

public final class TrefferWaechter implements Listener {

    private static final long MELDE_PAUSE_MILLIS = 5_000L;
    private static final long REPARATUR_MELDE_PAUSE_MILLIS = 30_000L;
    private static final int LEERE_SCHWUENGE_BIS_MELDUNG = 3;
    private static final int LEERE_SCHWUENGE_FENSTER_TICKS = 100;

    private final DuelPlus plugin;
    private final Map<UUID, Schlag> offeneSchlaege = new HashMap<>();
    private final Map<UUID, Integer> letzterAngriffTick = new HashMap<>();
    private final Map<UUID, Deque<Integer>> leereSchwuenge = new HashMap<>();
    private final Map<String, Long> letzteMeldung = new HashMap<>();
    private final Set<Scoreboard> eigeneScoreboards = Collections.newSetFromMap(new WeakHashMap<>());

    public TrefferWaechter(DuelPlus plugin) {
        this.plugin = plugin;
    }

    private static final class Schlag {
        private final UUID angreifer;
        private final UUID opfer;
        private final int tick;
        private final int schutzTicks;
        private final int maxSchutzTicks;
        private boolean schadenEreignis;
        private boolean fruehAbgebrochen;
        private boolean spaetAbgebrochen;

        private Schlag(UUID angreifer, UUID opfer, int tick, int schutzTicks, int maxSchutzTicks) {
            this.angreifer = angreifer;
            this.opfer = opfer;
            this.tick = tick;
            this.schutzTicks = schutzTicks;
            this.maxSchutzTicks = maxSchutzTicks;
        }
    }

    public void kampfbereitMachen(Player spieler, Player gegner) {
        List<String> behoben = zustandReparieren(spieler, gegner);
        if (behoben.isEmpty()) {
            return;
        }
        String schluessel = "reparatur:" + spieler.getUniqueId();
        if (!meldenErlaubt(schluessel, REPARATUR_MELDE_PAUSE_MILLIS)) {
            return;
        }
        plugin.getLogger().warning("Treffer-Pruefung: " + spieler.getName() + " war nicht kampfbereit und wurde repariert: "
                + String.join(", ", behoben) + ".");
    }

    public void eigeneTeamsAufraeumen(String... namen) {
        for (Scoreboard board : new ArrayList<>(eigeneScoreboards)) {
            Team team = board.getTeam(DuellSessionManager.KAMPF_TEAM_NAME);
            if (team == null) {
                continue;
            }
            for (String name : namen) {
                if (name != null) {
                    team.removeEntry(name);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimAngriff(PrePlayerAttackEntityEvent event) {
        if (!aktiv() || !(event.getAttacked() instanceof Player opfer)) {
            return;
        }
        Player angreifer = event.getPlayer();
        if (laufendeSession(angreifer, opfer).isEmpty()) {
            return;
        }
        int jetzt = Bukkit.getCurrentTick();
        letzterAngriffTick.put(angreifer.getUniqueId(), jetzt);
        if (!event.willAttack()) {
            melden(angreifer, opfer, "Minecraft haelt " + opfer.getName() + " fuer nicht angreifbar ("
                    + zustand(opfer) + ")", List.of());
            return;
        }
        if (event.isCancelled()) {
            melden(angreifer, opfer, "der Schlag wurde von einem Plugin abgebrochen, bevor Schaden berechnet wurde - "
                    + "in Frage kommen: " + fremdePlugins(PrePlayerAttackEntityEvent.getHandlerList()), List.of());
            return;
        }
        Schlag schlag = new Schlag(angreifer.getUniqueId(), opfer.getUniqueId(), jetzt,
                opfer.getNoDamageTicks(), opfer.getMaximumNoDamageTicks());
        offeneSchlaege.put(angreifer.getUniqueId(), schlag);
        Bukkit.getScheduler().runTask(plugin, () -> auswerten(schlag));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beimSchadenFrueh(EntityDamageByEntityEvent event) {
        Schlag schlag = passenderSchlag(event);
        if (schlag != null) {
            schlag.schadenEreignis = true;
            schlag.fruehAbgebrochen = event.isCancelled();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimSchadenSpaet(EntityDamageByEntityEvent event) {
        Schlag schlag = passenderSchlag(event);
        if (schlag != null) {
            schlag.spaetAbgebrochen = event.isCancelled();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimSchwingen(PlayerAnimationEvent event) {
        if (!aktiv() || event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player angreifer = event.getPlayer();
        Optional<DuellSession> sessionOpt = plugin.sessionManager().sessionVon(angreifer.getUniqueId());
        if (sessionOpt.isEmpty() || !sessionOpt.get().kampfLaeuft()) {
            return;
        }
        Player gegner = Bukkit.getPlayer(sessionOpt.get().gegnerVon(angreifer.getUniqueId()));
        if (gegner == null || !zieltAuf(angreifer, gegner)) {
            return;
        }
        int schwungTick = Bukkit.getCurrentTick();
        UUID angreiferId = angreifer.getUniqueId();
        UUID gegnerId = gegner.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> schwungAuswerten(angreiferId, gegnerId, schwungTick));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        offeneSchlaege.remove(id);
        letzterAngriffTick.remove(id);
        leereSchwuenge.remove(id);
        letzteMeldung.keySet().removeIf(schluessel -> schluessel.contains(id.toString()));
    }

    private void auswerten(Schlag schlag) {
        offeneSchlaege.remove(schlag.angreifer, schlag);
        Player angreifer = Bukkit.getPlayer(schlag.angreifer);
        Player opfer = Bukkit.getPlayer(schlag.opfer);
        if (angreifer == null || opfer == null || laufendeSession(angreifer, opfer).isEmpty()) {
            return;
        }
        if (schlag.schadenEreignis) {
            if (schlag.fruehAbgebrochen) {
                melden(angreifer, opfer, "der Server hat den Schaden schon vor allen Plugins abgebrochen ("
                        + zustand(opfer) + ")", List.of());
            } else if (schlag.spaetAbgebrochen) {
                melden(angreifer, opfer, "ein anderes Plugin hat den Schaden abgebrochen - in Frage kommen: "
                        + fremdePlugins(EntityDamageEvent.getHandlerList()), List.of());
            }
            return;
        }
        if (schlag.schutzTicks > schlag.maxSchutzTicks / 2 || angriffsSchaden(angreifer) <= 0) {
            return;
        }
        List<String> befunde = befunde(angreifer, opfer);
        List<String> behoben = new ArrayList<>(zustandReparieren(opfer, angreifer));
        behoben.addAll(zustandReparieren(angreifer, opfer));
        String grund = befunde.isEmpty()
                ? "unbekannt - Minecraft hat den Treffer ohne Schadens-Ereignis verworfen (" + zustand(opfer) + ")"
                : String.join(", ", befunde);
        melden(angreifer, opfer, grund, behoben);
    }

    private void schwungAuswerten(UUID angreiferId, UUID gegnerId, int schwungTick) {
        Integer letzterAngriff = letzterAngriffTick.get(angreiferId);
        if (letzterAngriff != null && letzterAngriff >= schwungTick - 1) {
            leereSchwuenge.remove(angreiferId);
            return;
        }
        Player angreifer = Bukkit.getPlayer(angreiferId);
        Player gegner = Bukkit.getPlayer(gegnerId);
        if (angreifer == null || gegner == null || laufendeSession(angreifer, gegner).isEmpty()) {
            return;
        }
        Deque<Integer> liste = leereSchwuenge.computeIfAbsent(angreiferId, k -> new ArrayDeque<>());
        liste.addLast(schwungTick);
        while (!liste.isEmpty() && schwungTick - liste.peekFirst() > LEERE_SCHWUENGE_FENSTER_TICKS) {
            liste.pollFirst();
        }
        if (liste.size() < LEERE_SCHWUENGE_BIS_MELDUNG) {
            return;
        }
        liste.clear();
        melden(angreifer, gegner, "Schlaege auf " + gegner.getName() + " kommen gar nicht erst beim Server an - "
                + "das blockiert vor Minecraft ein Anticheat oder ein Paket-Plugin. Installiert: " + allePluginsAusser(), List.of());
    }

    private List<String> befunde(Player angreifer, Player opfer) {
        List<String> befunde = new ArrayList<>();
        if (opfer.isInvulnerable()) {
            befunde.add(opfer.getName() + " war als unverwundbar markiert");
        }
        if (opfer.getGameMode() == GameMode.CREATIVE || opfer.getGameMode() == GameMode.SPECTATOR) {
            befunde.add(opfer.getName() + " war im Spielmodus " + opfer.getGameMode().name());
        }
        if (!opfer.getWorld().getPVP()) {
            befunde.add("PvP war in der Welt " + opfer.getWorld().getName() + " aus");
        }
        if (opfer.isDead() || opfer.getHealth() <= 0) {
            befunde.add(opfer.getName() + " galt fuer den Server als tot");
        }
        Scoreboard board = angreifer.getScoreboard();
        Team team = board.getEntryTeam(angreifer.getName());
        if (team != null && !team.allowFriendlyFire() && team.hasEntry(opfer.getName())) {
            boolean hauptBoard = board.equals(Bukkit.getScoreboardManager().getMainScoreboard());
            befunde.add("beide steckten im Team '" + team.getName() + "' ohne Friendly Fire"
                    + (hauptBoard ? "" : " (auf einem eigenen Scoreboard des Angreifers)"));
        }
        Object handle = handle(opfer);
        if (Boolean.TRUE.equals(aufrufen(handle, "isChangingDimension"))) {
            befunde.add(opfer.getName() + " hing fuer den Server noch im Weltwechsel fest");
        }
        if (Boolean.FALSE.equals(aufrufen(feldLesen(handle, "connection"), "hasClientLoaded"))) {
            befunde.add("der Server wartete noch auf die Meldung 'Welt geladen' von " + opfer.getName());
        }
        return befunde;
    }

    private List<String> zustandReparieren(Player spieler, Player gegner) {
        List<String> behoben = new ArrayList<>();
        if (spieler.isDead()) {
            return behoben;
        }
        if (spieler.isInvulnerable()) {
            spieler.setInvulnerable(false);
            behoben.add("Unverwundbarkeit entfernt");
        }
        if (spieler.getGameMode() != GameMode.SURVIVAL) {
            spieler.setGameMode(GameMode.SURVIVAL);
            if (spieler.getGameMode() == GameMode.SURVIVAL) {
                behoben.add("Survival gesetzt");
            }
        }
        World welt = spieler.getWorld();
        if (!welt.getPVP()) {
            welt.setPVP(true);
            behoben.add("PvP in " + welt.getName() + " eingeschaltet");
        }
        if (teamSichern(spieler.getScoreboard(), spieler, gegner)) {
            behoben.add("Friendly-Fire-Team gesetzt");
        }
        Object handle = handle(spieler);
        if (Boolean.TRUE.equals(aufrufen(handle, "isChangingDimension")) && aufrufen(handle, "hasChangedDimension") != FEHLER) {
            behoben.add("Weltwechsel abgeschlossen");
        }
        Object verbindung = feldLesen(handle, "connection");
        if (Boolean.FALSE.equals(aufrufen(verbindung, "hasClientLoaded")) && alsGeladenMarkieren(verbindung)) {
            behoben.add("Welt als geladen markiert");
        }
        return behoben;
    }

    private boolean teamSichern(Scoreboard board, Player spieler, Player gegner) {
        boolean geaendert = false;
        Team team = board.getTeam(DuellSessionManager.KAMPF_TEAM_NAME);
        if (team == null) {
            team = board.registerNewTeam(DuellSessionManager.KAMPF_TEAM_NAME);
            geaendert = true;
        }
        if (!team.allowFriendlyFire()) {
            team.setAllowFriendlyFire(true);
            geaendert = true;
        }
        for (String name : new String[]{spieler.getName(), gegner.getName()}) {
            if (!team.hasEntry(name)) {
                team.addEntry(name);
                geaendert = true;
            }
        }
        if (!board.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            eigeneScoreboards.add(board);
        }
        return geaendert;
    }

    private boolean zieltAuf(Player angreifer, Player gegner) {
        if (!angreifer.getWorld().equals(gegner.getWorld())) {
            return false;
        }
        Location auge = angreifer.getEyeLocation();
        Vector richtung = auge.getDirection();
        double reichweite = attributWert(angreifer, Attribute.ENTITY_INTERACTION_RANGE, 3.0) - 0.2;
        RayTraceResult trefferGegner = gegner.getBoundingBox().clone().expand(-0.1).rayTrace(auge.toVector(), richtung, reichweite);
        if (trefferGegner == null) {
            return false;
        }
        double abstand = trefferGegner.getHitPosition().distance(auge.toVector());
        RayTraceResult trefferBlock = angreifer.getWorld().rayTraceBlocks(auge, richtung, abstand, FluidCollisionMode.NEVER, true);
        return trefferBlock == null;
    }

    private Optional<DuellSession> laufendeSession(Player angreifer, Player opfer) {
        Optional<DuellSession> sessionOpt = plugin.sessionManager().sessionVon(angreifer.getUniqueId());
        if (sessionOpt.isEmpty() || !sessionOpt.get().kampfLaeuft()
                || !sessionOpt.get().gegnerVon(angreifer.getUniqueId()).equals(opfer.getUniqueId())) {
            return Optional.empty();
        }
        return sessionOpt;
    }

    private Schlag passenderSchlag(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player opfer) || !(event.getDamager() instanceof Player angreifer)) {
            return null;
        }
        Schlag schlag = offeneSchlaege.get(angreifer.getUniqueId());
        if (schlag == null || !schlag.opfer.equals(opfer.getUniqueId()) || schlag.tick != Bukkit.getCurrentTick()) {
            return null;
        }
        return schlag;
    }

    private void melden(Player angreifer, Player opfer, String grund, List<String> behoben) {
        String schluessel = angreifer.getUniqueId() + ">" + opfer.getUniqueId();
        if (!meldenErlaubt(schluessel, MELDE_PAUSE_MILLIS)) {
            return;
        }
        String behobenText = behoben.isEmpty() ? "" : " Automatisch repariert: " + String.join(", ", behoben) + ".";
        plugin.getLogger().warning("Treffer-Pruefung: Schlag von " + angreifer.getName() + " auf " + opfer.getName()
                + " hat keinen Schaden gemacht. Grund: " + grund + "." + behobenText);
        String grundChat = ohneTags(grund);
        String behobenChat = behoben.isEmpty() ? "" : " <green>Repariert: " + ohneTags(String.join(", ", behoben)) + ".</green>";
        for (Player empfaenger : new Player[]{angreifer, opfer}) {
            if (empfaenger.hasPermission("duelplus.admin")) {
                plugin.msgs().send(empfaenger, "hit-check", "angreifer", angreifer.getName(), "opfer", opfer.getName(),
                        "grund", grundChat, "behoben", behobenChat);
            }
        }
    }

    private boolean meldenErlaubt(String schluessel, long pauseMillis) {
        long jetzt = System.currentTimeMillis();
        Long zuletzt = letzteMeldung.get(schluessel);
        if (zuletzt != null && jetzt - zuletzt < pauseMillis) {
            return false;
        }
        letzteMeldung.put(schluessel, jetzt);
        return true;
    }

    private boolean aktiv() {
        return plugin.getConfig().getBoolean("kampf.treffer-pruefung", true) && plugin.sessionManager() != null;
    }

    private String zustand(Player spieler) {
        return "Modus " + spieler.getGameMode().name()
                + ", unverwundbar " + (spieler.isInvulnerable() ? "ja" : "nein")
                + ", Leben " + Math.round(spieler.getHealth() * 10) / 10.0
                + ", Schutz-Ticks " + spieler.getNoDamageTicks() + "/" + spieler.getMaximumNoDamageTicks()
                + ", PvP " + (spieler.getWorld().getPVP() ? "an" : "aus");
    }

    private String fremdePlugins(HandlerList liste) {
        Set<String> namen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (RegisteredListener eintrag : liste.getRegisteredListeners()) {
            if (!eintrag.getPlugin().equals(plugin)) {
                namen.add(eintrag.getPlugin().getName());
            }
        }
        return namen.isEmpty() ? "keins gefunden" : String.join(", ", namen);
    }

    private String allePluginsAusser() {
        Set<String> namen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (var anderes : Bukkit.getPluginManager().getPlugins()) {
            if (!anderes.equals(plugin) && anderes.isEnabled()) {
                namen.add(anderes.getName());
            }
        }
        return namen.isEmpty() ? "keine weiteren Plugins" : String.join(", ", namen);
    }

    private static String ohneTags(String text) {
        return text.replace("<", "").replace(">", "");
    }

    private static double angriffsSchaden(Player spieler) {
        return attributWert(spieler, Attribute.ATTACK_DAMAGE, 1.0);
    }

    private static double attributWert(Player spieler, Attribute attribut, double ersatz) {
        AttributeInstance instanz = spieler.getAttribute(attribut);
        return instanz == null ? ersatz : instanz.getValue();
    }

    private static final Object FEHLER = new Object();

    private static Object handle(Player spieler) {
        return aufrufen(spieler, "getHandle");
    }

    private static Object aufrufen(Object ziel, String name) {
        if (ziel == null) {
            return FEHLER;
        }
        Method methode = methodeSuchen(ziel.getClass(), name);
        if (methode == null) {
            return FEHLER;
        }
        try {
            methode.setAccessible(true);
            return methode.invoke(ziel);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return FEHLER;
        }
    }

    private static Method methodeSuchen(Class<?> klasse, String name, Class<?>... parameter) {
        for (Class<?> aktuell = klasse; aktuell != null; aktuell = aktuell.getSuperclass()) {
            try {
                return aktuell.getDeclaredMethod(name, parameter);
            } catch (NoSuchMethodException e) {
                continue;
            }
        }
        return null;
    }

    private static Object feldLesen(Object ziel, String name) {
        if (ziel == null || ziel == FEHLER) {
            return null;
        }
        for (Class<?> aktuell = ziel.getClass(); aktuell != null; aktuell = aktuell.getSuperclass()) {
            try {
                Field feld = aktuell.getDeclaredField(name);
                feld.setAccessible(true);
                return feld.get(ziel);
            } catch (NoSuchFieldException e) {
                continue;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private static boolean alsGeladenMarkieren(Object verbindung) {
        if (verbindung == null) {
            return false;
        }
        try {
            Method mitZeitablauf = methodeSuchen(verbindung.getClass(), "markClientLoaded", boolean.class);
            if (mitZeitablauf != null) {
                mitZeitablauf.setAccessible(true);
                mitZeitablauf.invoke(verbindung, true);
                return true;
            }
            Method ohneParameter = methodeSuchen(verbindung.getClass(), "markClientLoaded");
            if (ohneParameter != null) {
                ohneParameter.setAccessible(true);
                ohneParameter.invoke(verbindung);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
        return false;
    }
}
