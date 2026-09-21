package de.lemonpvp.duelplus.session;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.arena.Arena;
import de.lemonpvp.duelplus.db.DuelDatabase;
import de.lemonpvp.duelplus.db.DuelRecord;
import de.lemonpvp.duelplus.db.SpielerSnapshot;
import de.lemonpvp.duelplus.loot.LootManager;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nur auf dem Duels-Server aktiv. Erkennt die Ankunft von Duellanten
 * (per Join-Event, kein Polling noetig - die Ankunft selbst ist das
 * Ereignis), wendet ihr mitgebrachtes Inventar an, startet den Kampf
 * sobald beide da sind, und wickelt das Ende ab (Todeskamera, Loot,
 * Rueckreise). Das eigentliche Zurueckschicken der Ergebnisse auf den
 * jeweiligen Herkunftsserver macht weiterhin AnfragePollTask dort -
 * hier wird nur der "ZURUECK"-Schnappschuss hinterlegt.
 */
public final class DuellSessionManager implements Listener {

    private final DuelPlus plugin;
    private final LootManager loot;

    /** Wer fuer ein noch nicht gestartetes Duell schon angekommen ist (Ankunft + Inventar angewendet, wartet auf den Gegner). */
    private final Map<String, Set<UUID>> wartendAufAnkunft = new ConcurrentHashMap<>();
    private final Map<UUID, DuellSession> sessionNachSpieler = new ConcurrentHashMap<>();

    public DuellSessionManager(DuelPlus plugin) {
        this.plugin = plugin;
        this.loot = new LootManager(plugin);
    }

    public void starten() {
        plugin.getServer().getPluginManager().registerEvents(loot, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Arena-Warteschlange: alle paar Sekunden pruefen, ob inzwischen
        // wieder eine Arena frei ist und jemand wartet.
        Bukkit.getScheduler().runTaskTimer(plugin, this::warteschlangeVerarbeiten, 40L, 40L);
    }

    public LootManager loot() {
        return loot;
    }

    public Optional<DuellSession> sessionVon(UUID spieler) {
        return Optional.ofNullable(sessionNachSpieler.get(spieler));
    }

    // ================================================================
    //  Ankunft
    // ================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimJoin(PlayerJoinEvent event) {
        Player spieler = event.getPlayer();
        // Schon in einer laufenden Session (z.B. kurz die Verbindung
        // verloren und wieder da)? Einfach zurueck in die Arena.
        DuellSession laufend = sessionNachSpieler.get(spieler.getUniqueId());
        if (laufend != null) {
            Arena arena = plugin.arenaManager().arena(laufend.arenaName());
            if (arena != null) {
                Location ziel = laufend.spielerA().equals(spieler.getUniqueId()) ? arena.spawnA() : arena.spawnB();
                spieler.teleport(ziel);
            }
            return;
        }
        plugin.db().aktivesDuellFuer(spieler.getUniqueId()).thenAccept(duellOpt ->
                duellOpt.filter(d -> DuelRecord.ANGENOMMEN.equals(d.status())).ifPresent(duell ->
                        Bukkit.getScheduler().runTask(plugin, () -> ankunftVerarbeiten(spieler, duell))));
    }

    private void ankunftVerarbeiten(Player spieler, DuelRecord duell) {
        plugin.db().snapshotHolenUndLoeschen(duell.id(), spieler.getUniqueId(), DuelDatabase.RICHTUNG_HIN)
                .thenAccept(snapshotOpt -> Bukkit.getScheduler().runTask(plugin, () -> {
                    snapshotOpt.ifPresent(snap -> snap.anwenden(spieler.getInventory()));
                    Set<UUID> wartend = wartendAufAnkunft.computeIfAbsent(duell.id(), k -> ConcurrentHashMap.newKeySet());
                    wartend.add(spieler.getUniqueId());
                    if (wartend.contains(duell.spielerA()) && wartend.contains(duell.spielerB())) {
                        wartendAufAnkunft.remove(duell.id());
                        duellStarten(duell);
                    }
                }));
    }

    private void duellStarten(DuelRecord duell) {
        Player a = Bukkit.getPlayer(duell.spielerA());
        Player b = Bukkit.getPlayer(duell.spielerB());
        if (a == null || b == null) {
            // Einer ist zwischenzeitlich wieder weg - beim naechsten Join
            // (siehe oben) wird es erneut versucht.
            return;
        }
        Optional<Arena> arenaOpt = plugin.arenaManager().zuweisen(duell.id());
        if (arenaOpt.isEmpty()) {
            plugin.msgs().send(a, "arena-queue");
            plugin.msgs().send(b, "arena-queue");
            return;
        }
        Arena arena = arenaOpt.get();
        plugin.db().setzeArenaUndAktiv(duell.id(), arena.name());
        plugin.rollback().verfolgungStarten(arena.world());

        a.teleport(arena.spawnA());
        b.teleport(arena.spawnB());
        for (Player p : new Player[]{a, b}) {
            p.setGameMode(GameMode.ADVENTURE);
            var maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                p.setHealth(maxHealth.getValue());
            }
            p.setFoodLevel(20);
            p.setSaturation(20f);
            // Sonst waere man z.B. noch vom letzten Duell in Folge am
            // Brennen oder haette einen Trank-Effekt vom Herkunftsserver
            // dabei - beides waere fuer ein faires Duell nicht in Ordnung.
            zustandZuruecksetzen(p);
        }

        DuellSession session = new DuellSession(duell.id(), arena.name(),
                duell.spielerA(), duell.spielerAName(), duell.spielerAServer(),
                duell.spielerB(), duell.spielerBName(), duell.spielerBServer());
        sessionNachSpieler.put(duell.spielerA(), session);
        sessionNachSpieler.put(duell.spielerB(), session);

        int countdown = Math.max(1, plugin.getConfig().getInt("kampf.countdown-sekunden", 5));
        new BukkitRunnable() {
            int rest = countdown;

            @Override
            public void run() {
                if (rest <= 0) {
                    for (Player p : new Player[]{a, b}) {
                        if (p.isOnline()) {
                            p.setGameMode(GameMode.SURVIVAL);
                            plugin.msgs().send(p, "fight");
                            plugin.msgs().title(p, "title-fight", "title-fight-sub");
                        }
                    }
                    // Blitzeinschlag NUR als optischer/akustischer Effekt
                    // (strikeLightningEffect statt strikeLightning) - macht
                    // KEINEN Schaden und zuendet nichts an, ist aber ein
                    // dramatischer Start-Moment fuer beide sichtbar.
                    arena.world().strikeLightningEffect(arena.world().getSpawnLocation());
                    BossBar bossBar = BossBar.bossBar(
                            plugin.msgs().format("bossbar-normal", "groesse", String.valueOf((int) arena.vollGroesse())),
                            1f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
                    session.bossBarSetzen(bossBar);
                    for (Player p : new Player[]{a, b}) {
                        if (p.isOnline()) {
                            p.showBossBar(bossBar);
                        }
                    }
                    session.kampfStarten();
                    kampfUeberwachungStarten(arena, session);
                    cancel();
                    return;
                }
                for (Player p : new Player[]{a, b}) {
                    if (p.isOnline()) {
                        plugin.msgs().send(p, "countdown", "sekunden", String.valueOf(rest));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                }
                rest--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Eine gemeinsame Sekundentakt-Ueberwachung fuer alles, was vom
     * Treffer-Zeitpunkt abhaengt (session.treffer(), siehe ArenaGuardListener.
     * beiSchaden):
     *
     *  - Worldborder: nicht einfach linear ueber eine feste Zeit, sondern
     *    jede Sekunde neu berechnet - solange getroffen wird, im normalen
     *    (langsamen) Tempo, faellt laenger als camping-nach-sekunden kein
     *    Treffer, auf das schnellere Camping-Tempo. Jeder Schritt selbst
     *    laeuft ueber 1 Sekunde sanft (WorldBorder#setSize mit
     *    Uebergangszeit), damit es trotz haeufiger Neuberechnung nicht
     *    ruckelt.
     *  - Aufgabe bei Inaktivitaet: faellt laenger als warnung-nach-minuten
     *    KEIN Treffer, eine Warnung an beide - faellt danach nochmal
     *    frist-danach-minuten lang keiner, automatische Aufgabe mit
     *    Inventar-Teilverlust fuer beide (kein Sieger). Jeder Treffer
     *    setzt das komplett zurueck (siehe DuellSession.treffer()).
     *  - Boss-Bar + Atmosphaere: dieselbe Sekundentakt-Schleife haelt auch
     *    die Boss-Bar auf dem aktuellen Grenz-Stand (siehe
     *    bossBarAktualisieren) und streut Seelen-Partikel am Rand (siehe
     *    atmosphaerePartikel). Erreicht die Grenze zum ERSTEN Mal ihre
     *    Ziel-Groesse, loest das einmalig den "Ploetzlicher Tod"-Moment
     *    aus (siehe ploetzlicherTodAusloesen).
     */
    private void kampfUeberwachungStarten(Arena arena, DuellSession session) {
        double zielGroesse = Math.max(2, plugin.getConfig().getInt("kampf.worldborder-schrumpfen.ziel-groesse", 10));
        int normalDauer = Math.max(1, plugin.getConfig().getInt("kampf.worldborder-schrumpfen.dauer-sekunden", 270));
        long campingNachMillis = Math.max(1, plugin.getConfig().getInt("kampf.worldborder-schrumpfen.camping-nach-sekunden", 15)) * 1000L;
        int campingDauer = Math.max(1, plugin.getConfig().getInt("kampf.worldborder-schrumpfen.camping-dauer-sekunden", 60));

        double startGroesse = arena.vollGroesse();
        double gesamtStrecke = Math.max(0, startGroesse - zielGroesse);
        double normalProSekunde = gesamtStrecke / normalDauer;
        double campingProSekunde = gesamtStrecke / campingDauer;

        boolean inaktivitaetsAufgabeAktiv = plugin.getConfig().getBoolean("kampf.aufgabe-bei-inaktivitaet.aktiv", true);
        long warnungNachMillis = Math.max(1, plugin.getConfig().getInt("kampf.aufgabe-bei-inaktivitaet.warnung-nach-minuten", 10)) * 60_000L;
        long fristMillis = Math.max(1, plugin.getConfig().getInt("kampf.aufgabe-bei-inaktivitaet.frist-danach-minuten", 5)) * 60_000L;

        new BukkitRunnable() {
            double aktuelleGroesse = startGroesse;

            @Override
            public void run() {
                // Session vorbei (Sieg/Niederlage/Unentschieden/Aufgabe) -
                // nichts mehr zu tun.
                if (sessionNachSpieler.get(session.spielerA()) != session) {
                    cancel();
                    return;
                }
                long seitTreffer = session.millisSeitLetztemTreffer();
                boolean campt = seitTreffer >= campingNachMillis;

                if (aktuelleGroesse > zielGroesse) {
                    double proSekunde = campt ? campingProSekunde : normalProSekunde;
                    aktuelleGroesse = Math.max(zielGroesse, aktuelleGroesse - proSekunde);
                    arena.world().getWorldBorder().setSize(aktuelleGroesse, 1);
                }
                boolean ploetzlicherTod = aktuelleGroesse <= zielGroesse;

                bossBarAktualisieren(session, aktuelleGroesse, startGroesse, zielGroesse, campt, ploetzlicherTod);
                atmosphaerePartikel(arena);
                if (ploetzlicherTod && !session.ploetzlicherTodGezeigt()) {
                    session.ploetzlicherTodSetzen();
                    ploetzlicherTodAusloesen(session);
                }

                if (!inaktivitaetsAufgabeAktiv) {
                    return;
                }
                if (!session.inaktivitaetsWarnungGezeigt() && seitTreffer >= warnungNachMillis) {
                    session.inaktivitaetsWarnungSetzen();
                    String minuten = String.valueOf(fristMillis / 60_000L);
                    Player a = Bukkit.getPlayer(session.spielerA());
                    Player b = Bukkit.getPlayer(session.spielerB());
                    if (a != null) {
                        plugin.msgs().send(a, "inactivity-warning", "minuten", minuten);
                    }
                    if (b != null) {
                        plugin.msgs().send(b, "inactivity-warning", "minuten", minuten);
                    }
                } else if (session.inaktivitaetsWarnungGezeigt() && seitTreffer >= warnungNachMillis + fristMillis) {
                    cancel();
                    inaktivitaetsAufgabeAusloesen(session);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Text/Fortschritt/Farbe der Boss-Bar passend zum aktuellen
     * Grenz-Zustand: normal (blau, schrumpft im normalen Tempo), Camping
     * (gelb, schrumpft schneller, siehe kampfUeberwachungStarten) oder
     * Ploetzlicher Tod (rot, volle Bar, Boss-Musik + abgedunkelter
     * Bildschirm als zusaetzlicher Nachdruck - siehe ploetzlicherTodAusloesen).
     */
    private void bossBarAktualisieren(DuellSession session, double aktuelleGroesse, double startGroesse,
                                       double zielGroesse, boolean campt, boolean ploetzlicherTod) {
        BossBar bar = session.bossBar();
        if (bar == null) {
            return;
        }
        if (ploetzlicherTod) {
            bar.name(plugin.msgs().format("bossbar-suddendeath"));
            bar.progress(1f);
            bar.color(BossBar.Color.RED);
            bar.addFlags(BossBar.Flag.PLAY_BOSS_MUSIC, BossBar.Flag.DARKEN_SCREEN);
            return;
        }
        String groesseText = String.valueOf((int) Math.round(aktuelleGroesse));
        float fortschritt = (float) Math.max(0.0, Math.min(1.0,
                (aktuelleGroesse - zielGroesse) / Math.max(0.0001, startGroesse - zielGroesse)));
        bar.progress(fortschritt);
        if (campt) {
            bar.name(plugin.msgs().format("bossbar-camping", "groesse", groesseText));
            bar.color(BossBar.Color.YELLOW);
        } else {
            bar.name(plugin.msgs().format("bossbar-normal", "groesse", groesseText));
            bar.color(BossBar.Color.BLUE);
        }
    }

    /** Seelen-Partikel nahe am aeusseren Rand - Atmosphaere fuer die "im Nichts schwebende Plattform" (siehe ArenaManager). */
    private void atmosphaerePartikel(Arena arena) {
        World world = arena.world();
        Location mitte = world.getSpawnLocation();
        double radius = arena.vollGroesse() / 2.0;
        for (int i = 0; i < 5; i++) {
            double winkel = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            double r = radius - ThreadLocalRandom.current().nextDouble() * 5;
            double x = mitte.getX() + r * Math.cos(winkel);
            double z = mitte.getZ() + r * Math.sin(winkel);
            world.spawnParticle(Particle.SOUL, x, mitte.getY() + 0.2, z, 1, 0, 0.4, 0, 0.01);
        }
    }

    /** Einmaliger dramatischer Moment, sobald die Grenze zum ersten Mal ihre Ziel-Groesse erreicht - siehe kampfUeberwachungStarten. */
    private void ploetzlicherTodAusloesen(DuellSession session) {
        for (UUID uuid : new UUID[]{session.spielerA(), session.spielerB()}) {
            Player spieler = Bukkit.getPlayer(uuid);
            if (spieler == null) {
                continue;
            }
            plugin.msgs().title(spieler, "title-suddendeath", "title-suddendeath-sub");
            spieler.playSound(spieler.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
        }
    }

    /**
     * Groesserer Partikel-/Sound-Ausbruch genau im Moment des
     * entscheidenden Treffers - zusaetzlich zum kleineren Effekt, den
     * JEDER Treffer schon ueber ArenaGuardListener.trefferEffekt bekommt,
     * und zusaetzlich zu den Sieg/Niederlage-Titeln oben. Beide Parameter
     * koennen null sein (z.B. Verbindung waehrend des eigenen Duells
     * getrennt) - dann faellt der jeweilige Teil einfach aus.
     */
    private void killEffekt(Player verlierer, Player gewinnerSpieler) {
        if (verlierer != null) {
            verlierer.getWorld().spawnParticle(Particle.CRIT, verlierer.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.15);
        }
        if (gewinnerSpieler != null) {
            gewinnerSpieler.playSound(gewinnerSpieler.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        }
    }

    /** Boss-Bar fuer beide ausblenden - bei JEDEM Ende einer Session aufgerufen (Sieg/Niederlage, Unentschieden, Aufgabe). */
    private void bossBarVerstecken(DuellSession session) {
        BossBar bar = session.bossBar();
        if (bar == null) {
            return;
        }
        for (UUID uuid : new UUID[]{session.spielerA(), session.spielerB()}) {
            Player spieler = Bukkit.getPlayer(uuid);
            if (spieler != null) {
                spieler.hideBossBar(bar);
            }
        }
    }

    // ================================================================
    //  Ende eines Duells
    // ================================================================

    /**
     * Ein Teilnehmer hat verloren (toedlicher Treffer abgefangen ODER
     * die Verbindung getrennt) - beendet die Session, verteilt das
     * Loot, setzt die Arena zurueck und schickt beide zurueck.
     *
     * Verlierer und Gewinner verlassen die Arena bewusst zu
     * VERSCHIEDENEN Zeitpunkten: der Verlierer schon nach der kurzen
     * Todeskamera, der Gewinner erst nach seinem vollen Loot-
     * Schutzfenster (loot.schutz-sekunden) - sonst waere das exklusive
     * Zeitfenster aus LootManager fuer ihn nutzlos, weil er laengst weg
     * waere, bevor er das abgeworfene Loot ueberhaupt selbst aufheben
     * konnte. Deshalb wird auch sein Rueckreise-Inventar-Schnappschuss
     * ERST in dem Moment erfasst, in dem er wirklich geht - nicht schon
     * beim Sieg selbst, sonst wuerde aufgehobenes Loot gar nicht mit
     * zurueckreisen.
     *
     * Der Status faellt trotzdem schon sofort auf BEENDET: das ist
     * ungefaehrlich, weil AnfragePollTask.ergebnisAnwenden auf jedem
     * Server ohnehin erst dann etwas tut, wenn er den jeweiligen Spieler
     * DORT wirklich online findet - und der Gewinner wird ja erst NACH
     * dem finalen Schreiben seines Schnappschusses ueberhaupt losgeschickt.
     */
    public void niederlageAusloesen(UUID verliererUuid, boolean nochOnlineFuerTodeskamera) {
        DuellSession session = sessionNachSpieler.remove(verliererUuid);
        if (session == null) {
            return;
        }
        UUID gegnerUuid = session.gegnerVon(verliererUuid);
        sessionNachSpieler.remove(gegnerUuid);
        UUID gewinner = gegnerUuid;
        bossBarVerstecken(session);
        plugin.db().siegHinzufuegen(gewinner, session.eigenerName(gewinner));
        plugin.db().niederlageHinzufuegen(verliererUuid, session.eigenerName(verliererUuid));

        Player verlierer = Bukkit.getPlayer(verliererUuid);
        Player gewinnerSpieler = Bukkit.getPlayer(gegnerUuid);

        // Sofort, im Moment des Ausgangs selbst - nicht erst nach der
        // (teils viel spaeteren) Rueckreise auf den Herkunftsserver.
        if (verlierer != null) {
            plugin.msgs().title(verlierer, "title-lost", "title-lost-sub", "gegner", session.gegnerNameVon(verliererUuid));
        }
        if (gewinnerSpieler != null) {
            plugin.msgs().title(gewinnerSpieler, "title-won", "title-won-sub", "gegner", session.gegnerNameVon(gegnerUuid));
        }
        killEffekt(verlierer, gewinnerSpieler);

        if (verlierer != null) {
            Location ort = verlierer.getLocation();
            loot.verliererLootAbwerfen(verlierer, gewinner, ort);
        }

        plugin.db().snapshotSchreiben(session.duellId(), verliererUuid, DuelDatabase.RICHTUNG_ZURUECK, SpielerSnapshot.leer())
                .thenCompose(unused -> plugin.db().beenden(session.duellId(), gewinner));

        Arena arena = plugin.arenaManager().arena(session.arenaName());
        int anzeigeSekunden = Math.max(1, plugin.getConfig().getInt("kampf.todeskamera-sekunden", 5));
        int lootSekunden = Math.max(anzeigeSekunden, plugin.getConfig().getInt("loot.schutz-sekunden", 60));

        if (verlierer != null && nochOnlineFuerTodeskamera) {
            todeskameraStarten(verlierer, anzeigeSekunden);
        }
        if (gewinnerSpieler != null) {
            gewinnerSpieler.setGameMode(GameMode.ADVENTURE);
            // Sonst waere der Gewinner waehrend der Wartezeit auf sein
            // eigenes Loot ganz normal verwundbar (Feuer, Sturz, ...) -
            // ein Sieg soll keinen nachtraeglichen Schaden mehr bedeuten.
            gewinnerSpieler.setInvulnerable(true);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (verlierer != null && verlierer.isOnline()) {
                zustandZuruecksetzen(verlierer);
                verlierer.setGameMode(GameMode.SURVIVAL);
                plugin.bridge().sende(verlierer, session.herkunftsServerVon(verliererUuid));
            }
        }, anzeigeSekunden * 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (arena != null) {
                plugin.rollback().zuruecksetzenUndStoppen(arena.world());
                plugin.arenaManager().freigeben(arena.name());
            }
            if (gewinnerSpieler == null || !gewinnerSpieler.isOnline()) {
                return;
            }
            // Erst JETZT, nach dem vollen Schutzfenster, das tatsaechliche
            // Inventar erfassen - damit auch selbst aufgehobenes Loot
            // wirklich mit zurueckreist.
            SpielerSnapshot gewinnerSnapshot = SpielerSnapshot.von(gewinnerSpieler.getInventory());
            zustandZuruecksetzen(gewinnerSpieler);
            // GameMode/Unverwundbarkeit bewusst ERST im Callback (NACH dem
            // DB-Schreiben) umstellen, nicht schon hier - sonst waere der
            // Gewinner fuer die Dauer des (asynchronen) Schreibens kurz in
            // Survival, mitten in der Arena, und koennte dort noch Bloecke
            // abbauen, die zu dem Zeitpunkt nichtmal mehr vom RollbackTracker
            // erfasst wuerden (der ist ja schon oben gestoppt worden).
            plugin.db().snapshotSchreiben(session.duellId(), gegnerUuid, DuelDatabase.RICHTUNG_ZURUECK, gewinnerSnapshot)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (gewinnerSpieler.isOnline()) {
                            gewinnerSpieler.setGameMode(GameMode.SURVIVAL);
                            gewinnerSpieler.setInvulnerable(false);
                            plugin.bridge().sende(gewinnerSpieler, session.herkunftsServerVon(gegnerUuid));
                        }
                    }));
        }, lootSekunden * 20L);
    }

    // ================================================================
    //  /draw - Unentschieden per gegenseitiger Zustimmung
    // ================================================================

    /** Ein Spieler hat /draw benutzt - traegt seine Zustimmung ein und loest bei beidseitiger Zustimmung aus. */
    public void unentschiedenVorschlagen(DuellSession session, Player spieler) {
        UUID gegnerUuid = session.gegnerVon(spieler.getUniqueId());
        if (session.hatUnentschiedenVorgeschlagen(spieler.getUniqueId())) {
            plugin.msgs().send(spieler, "draw-already-requested", "gegner", session.gegnerNameVon(spieler.getUniqueId()));
            return;
        }
        boolean beideEinverstanden = session.unentschiedenZustimmen(spieler.getUniqueId());
        if (beideEinverstanden) {
            unentschiedenAusloesen(session);
            return;
        }
        plugin.msgs().send(spieler, "draw-requested", "gegner", session.gegnerNameVon(spieler.getUniqueId()));
        Player gegnerSpieler = Bukkit.getPlayer(gegnerUuid);
        if (gegnerSpieler != null) {
            plugin.msgs().send(gegnerSpieler, "draw-requested-by", "gegner", spieler.getName());
        }
    }

    /**
     * Beide haben /draw zugestimmt - beendet das Duell OHNE Sieger: jeder
     * bekommt sein eigenes, unveraendertes Inventar zurueck, kein Loot
     * wechselt den Besitzer, kein Shulker wird abgeworfen.
     */
    private void unentschiedenAusloesen(DuellSession session) {
        for (UUID uuid : new UUID[]{session.spielerA(), session.spielerB()}) {
            Player spieler = Bukkit.getPlayer(uuid);
            if (spieler != null) {
                plugin.msgs().title(spieler, "title-draw", "title-draw-sub");
            }
        }
        ohneSiegerBeenden(session);
    }

    // ================================================================
    //  Aufgabe bei Inaktivitaet (siehe kampfUeberwachungStarten)
    // ================================================================

    private void inaktivitaetsAufgabeAusloesen(DuellSession session) {
        double anteil = Math.max(0, Math.min(1,
                plugin.getConfig().getDouble("kampf.aufgabe-bei-inaktivitaet.inventar-verlust-anteil", 0.2)));
        for (UUID uuid : new UUID[]{session.spielerA(), session.spielerB()}) {
            Player spieler = Bukkit.getPlayer(uuid);
            if (spieler == null) {
                continue;
            }
            inventarAnteilVerlieren(spieler, anteil);
            plugin.msgs().send(spieler, "inactivity-forfeit");
            plugin.msgs().title(spieler, "title-forfeit", "title-forfeit-sub");
        }
        ohneSiegerBeenden(session);
    }

    /** Entfernt ersatzlos einen zufaelligen Anteil der BELEGTEN Faecher (Hauptinventar+Ruestung+Offhand zusammen). */
    private void inventarAnteilVerlieren(Player spieler, double anteil) {
        var inv = spieler.getInventory();
        ItemStack[] haupt = inv.getStorageContents();
        ItemStack[] ruestung = inv.getArmorContents();
        boolean offhandBelegt = inv.getItemInOffHand().getType() != Material.AIR;

        List<int[]> belegt = new ArrayList<>();
        for (int i = 0; i < haupt.length; i++) {
            if (haupt[i] != null && haupt[i].getType() != Material.AIR) {
                belegt.add(new int[]{0, i});
            }
        }
        for (int i = 0; i < ruestung.length; i++) {
            if (ruestung[i] != null && ruestung[i].getType() != Material.AIR) {
                belegt.add(new int[]{1, i});
            }
        }
        if (offhandBelegt) {
            belegt.add(new int[]{2, 0});
        }
        if (belegt.isEmpty()) {
            return;
        }
        Collections.shuffle(belegt);
        int anzahl = (int) Math.ceil(belegt.size() * anteil);
        boolean offhandEntfernen = false;
        for (int i = 0; i < anzahl && i < belegt.size(); i++) {
            int[] ziel = belegt.get(i);
            if (ziel[0] == 0) {
                haupt[ziel[1]] = null;
            } else if (ziel[0] == 1) {
                ruestung[ziel[1]] = null;
            } else {
                offhandEntfernen = true;
            }
        }
        inv.setStorageContents(haupt);
        inv.setArmorContents(ruestung);
        if (offhandEntfernen) {
            inv.setItemInOffHand(null);
        }
    }

    /**
     * Gemeinsamer Abschluss fuer jedes Duell-Ende OHNE Sieger (Unentschieden
     * per /draw ODER automatische Aufgabe bei Inaktivitaet) - jeder bekommt
     * sein aktuelles Inventar zurueck (bei Inaktivitaets-Aufgabe also schon
     * OHNE den verlorenen Anteil, der wurde vorher entfernt), kein Loot
     * wechselt den Besitzer, kein Shulker wird abgeworfen.
     */
    private void ohneSiegerBeenden(DuellSession session) {
        UUID aUuid = session.spielerA();
        UUID bUuid = session.spielerB();
        // Beide IMMER entfernen (nicht kurzschliessen) - sonst bliebe bei
        // bereits anderweitig beendeter Session (z.B. Verbindungsabbruch im
        // selben Moment) einer der beiden faelschlich als "in Session" stehen.
        boolean aEntfernt = sessionNachSpieler.remove(aUuid) != null;
        boolean bEntfernt = sessionNachSpieler.remove(bUuid) != null;
        if (!aEntfernt && !bEntfernt) {
            return;
        }
        bossBarVerstecken(session);
        plugin.db().unentschiedenHinzufuegen(aUuid, session.eigenerName(aUuid));
        plugin.db().unentschiedenHinzufuegen(bUuid, session.eigenerName(bUuid));

        Player a = Bukkit.getPlayer(aUuid);
        Player b = Bukkit.getPlayer(bUuid);

        var aGeschrieben = plugin.db().snapshotSchreiben(session.duellId(), aUuid, DuelDatabase.RICHTUNG_ZURUECK,
                a != null ? SpielerSnapshot.von(a.getInventory()) : SpielerSnapshot.leer());
        var bGeschrieben = plugin.db().snapshotSchreiben(session.duellId(), bUuid, DuelDatabase.RICHTUNG_ZURUECK,
                b != null ? SpielerSnapshot.von(b.getInventory()) : SpielerSnapshot.leer());
        java.util.concurrent.CompletableFuture.allOf(aGeschrieben, bGeschrieben)
                .thenCompose(unused -> plugin.db().beenden(session.duellId(), null));

        Arena arena = plugin.arenaManager().arena(session.arenaName());
        if (arena != null) {
            plugin.rollback().zuruecksetzenUndStoppen(arena.world());
            plugin.arenaManager().freigeben(arena.name());
        }
        for (Player p : new Player[]{a, b}) {
            if (p == null || !p.isOnline()) {
                continue;
            }
            zustandZuruecksetzen(p);
            p.setGameMode(GameMode.SURVIVAL);
            plugin.bridge().sende(p, session.herkunftsServerVon(p.getUniqueId()));
        }
    }

    private void zustandZuruecksetzen(Player spieler) {
        spieler.setFireTicks(0);
        spieler.setFallDistance(0f);
        for (var effekt : new ArrayList<>(spieler.getActivePotionEffects())) {
            spieler.removePotionEffect(effekt.getType());
        }
    }

    private void todeskameraStarten(Player verlierer, int sekunden) {
        Location zentrum = verlierer.getLocation();
        verlierer.setGameMode(GameMode.SPECTATOR);
        new BukkitRunnable() {
            double winkel = 0;
            int ticks = sekunden * 20;

            @Override
            public void run() {
                if (ticks-- <= 0 || !verlierer.isOnline()) {
                    cancel();
                    return;
                }
                winkel += 4;
                double radius = 5.0;
                double x = zentrum.getX() + radius * Math.cos(Math.toRadians(winkel));
                double z = zentrum.getZ() + radius * Math.sin(Math.toRadians(winkel));
                Location kamera = new Location(zentrum.getWorld(), x, zentrum.getY() + 2.5, z);
                kamera.setDirection(zentrum.toVector().subtract(kamera.toVector()));
                verlierer.teleport(kamera);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ================================================================
    //  Arena-Warteschlange
    // ================================================================

    private void warteschlangeVerarbeiten() {
        String duellId = plugin.arenaManager().naechsterAusWarteschlange();
        if (duellId == null) {
            return;
        }
        plugin.db().holeById(duellId).thenAccept(duellOpt -> Bukkit.getScheduler().runTask(plugin, () ->
                duellOpt.filter(d -> DuelRecord.ANGENOMMEN.equals(d.status())).ifPresent(this::duellStarten)));
    }
}
