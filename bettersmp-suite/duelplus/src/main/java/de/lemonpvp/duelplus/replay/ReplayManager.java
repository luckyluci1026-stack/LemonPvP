package de.lemonpvp.duelplus.replay;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.arena.Arena;
import de.lemonpvp.duelplus.db.ReplayEintrag;
import de.lemonpvp.duelplus.session.DuellSession;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayManager implements Listener {

    public static final String RECHT = "duelplus.replay";
    public static final String PRAEFIX = "replay:";
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.GERMANY);
    private static final DateTimeFormatter DATUM_LANG = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
    private static final int LISTE = 10;

    private final DuelPlus plugin;
    private final ReplaySpeicher speicher;
    private final Map<UUID, ReplayWiedergabe> wiedergaben = new ConcurrentHashMap<>();
    private final Set<UUID> laedt = ConcurrentHashMap.newKeySet();
    private ReplayRecorder recorder;
    private BukkitTask aufraeumen;

    public ReplayManager(DuelPlus plugin) {
        this(plugin, new ReplaySpeicher(plugin, plugin.getDataFolder().toPath()
                .resolve(plugin.getConfig().getString("replay.ordner", "replays"))));
    }

    public ReplayManager(DuelPlus plugin, ReplaySpeicher speicher) {
        this.plugin = plugin;
        this.speicher = speicher;
    }

    public ReplaySpeicher speicher() {
        return speicher;
    }

    public ReplayRecorder recorder() {
        return recorder;
    }

    public void meldungenAbhoeren() {
        PluginManager plugins = plugin.getServer().getPluginManager();
        if (plugins.getPlugin("BetterSMP") != null) {
            try {
                plugins.registerEvents(new MeldungBetterSmp(this), plugin);
            } catch (LinkageError fehler) {
                plugin.getLogger().warning("BetterSMP-Meldungen lassen sich nicht abhoeren: " + fehler);
            }
        }
        if (plugins.getPlugin("ReportPlus") != null) {
            try {
                plugins.registerEvents(new MeldungReportPlus(this), plugin);
            } catch (LinkageError fehler) {
                plugin.getLogger().warning("ReportPlus-Meldungen lassen sich nicht abhoeren: " + fehler);
            }
        }
    }

    public void aufArenaServerStarten() {
        recorder = new ReplayRecorder(plugin, speicher);
        plugin.getServer().getPluginManager().registerEvents(recorder, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        recorder.starten();
        aufraeumen = Bukkit.getScheduler().runTaskTimer(plugin, () -> speicher.aufraeumenAsync(), 20L * 60, 20L * 60 * 10);
    }

    public void stoppen() {
        for (UUID zuschauer : List.copyOf(wiedergaben.keySet())) {
            beenden(zuschauer, true);
        }
        if (aufraeumen != null) {
            aufraeumen.cancel();
        }
        if (recorder != null) {
            recorder.stoppen();
        }
        speicher.schliessen();
    }

    public void duellGestartet(DuellSession session, Arena arena, Player a, Player b) {
        if (recorder != null) {
            recorder.aufnahmeStarten(session, arena, a, b);
        }
    }

    public void duellBeendet(String duellId, int ergebnis, UUID gewinner) {
        if (recorder != null) {
            recorder.aufnahmeBeenden(duellId, ergebnis, gewinner);
        }
    }

    public void treffer(Player opfer, double schaden) {
        if (recorder != null) {
            recorder.treffer(opfer, schaden);
        }
    }

    public void chat(Player spieler, String text) {
        if (recorder != null) {
            recorder.chat(spieler, text);
        }
    }

    public void spielerGemeldet(UUID gemeldet, String name) {
        if (!plugin.db().bereit() || !plugin.getConfig().getBoolean("replay.aktiv", true)) {
            return;
        }
        plugin.db().meldungMerken(gemeldet);
        long seit = System.currentTimeMillis() - speicher.aufbewahrenMillis();
        plugin.db().replaysMelden(gemeldet, seit, speicher.gemeldetMillis()).thenAccept(anzahl -> {
            if (anzahl > 0) {
                plugin.getLogger().info(name + " wurde gemeldet - " + anzahl + " Duell-Replay(s) werden "
                        + plugin.getConfig().getInt("replay.gemeldet-tage", 30) + " Tage aufbewahrt.");
            }
        });
    }

    public boolean istReplayAnfrage(String arenaWelt) {
        return arenaWelt != null && arenaWelt.startsWith(PRAEFIX);
    }

    public boolean schautZu(UUID spieler) {
        return wiedergaben.containsKey(spieler);
    }

    public Optional<ReplayWiedergabe> wiedergabe(UUID spieler) {
        return Optional.ofNullable(wiedergaben.get(spieler));
    }

    public void ansehen(Player spieler, String id) {
        if (wiedergaben.containsKey(spieler.getUniqueId()) || laedt.contains(spieler.getUniqueId())) {
            plugin.msgs().send(spieler, "replay-laeuft-schon");
            return;
        }
        plugin.db().replayHolen(id).thenCombine(plugin.db().istBeschaeftigt(spieler.getUniqueId()), (eintragOpt, beschaeftigt) -> {
            Bukkit.getScheduler().runTask(plugin, () -> ansehenWeiter(spieler, id, eintragOpt, beschaeftigt));
            return null;
        });
    }

    private void ansehenWeiter(Player spieler, String id, Optional<ReplayEintrag> eintragOpt, boolean beschaeftigt) {
        if (eintragOpt.isEmpty()) {
            plugin.msgs().send(spieler, "replay-nicht-gefunden", "id", id);
            return;
        }
        if (beschaeftigt || (plugin.istArenaServer() && plugin.sessionManager() != null
                && plugin.sessionManager().beschaeftigt(spieler.getUniqueId()))) {
            plugin.msgs().send(spieler, "replay-busy");
            return;
        }
        ReplayEintrag eintrag = eintragOpt.get();
        if (plugin.istArenaServer()) {
            starten(spieler, eintrag, plugin.serverName());
            return;
        }
        String ziel = plugin.arenaServerName();
        plugin.db().zuschauerAnfrageSchreiben(spieler.getUniqueId(), PRAEFIX + eintrag.id(), plugin.serverName())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.msgs().send(spieler, "replay-teleport", "server", ziel);
                    plugin.bridge().sende(spieler, ziel);
                }));
    }

    public void nachAnkunft(Player spieler, String anfrage, String herkunft) {
        String id = anfrage.substring(PRAEFIX.length());
        plugin.db().replayHolen(id).thenAccept(eintragOpt -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (eintragOpt.isEmpty()) {
                plugin.msgs().send(spieler, "replay-nicht-gefunden", "id", id);
                zurueckschicken(spieler, herkunft);
                return;
            }
            starten(spieler, eintragOpt.get(), herkunft);
        }));
    }

    private void zurueckschicken(Player spieler, String herkunft) {
        if (herkunft != null && !herkunft.equalsIgnoreCase(plugin.serverName()) && spieler.isOnline()) {
            plugin.bridge().sende(spieler, herkunft);
        }
    }

    void starten(Player spieler, ReplayEintrag eintrag, String herkunft) {
        UUID id = spieler.getUniqueId();
        if (!laedt.add(id)) {
            return;
        }
        if (!speicher.vorhanden(eintrag.id())) {
            laedt.remove(id);
            plugin.msgs().send(spieler, "replay-datei-fehlt");
            zurueckschicken(spieler, herkunft);
            return;
        }
        Optional<Arena> arena = plugin.arenaManager().reservieren(PRAEFIX + eintrag.id(), eintrag.arena());
        if (arena.isEmpty()) {
            laedt.remove(id);
            plugin.msgs().send(spieler, "replay-keine-arena");
            zurueckschicken(spieler, herkunft);
            return;
        }
        plugin.msgs().send(spieler, "replay-laden");
        speicher.laden(eintrag.id()).whenComplete((replay, fehler) -> Bukkit.getScheduler().runTask(plugin, () -> {
            laedt.remove(id);
            if (fehler != null || !spieler.isOnline()) {
                plugin.arenaManager().freigeben(arena.get().name());
                if (spieler.isOnline()) {
                    plugin.msgs().send(spieler, "replay-kaputt");
                    zurueckschicken(spieler, herkunft);
                }
                return;
            }
            ReplayWiedergabe wiedergabe = new ReplayWiedergabe(plugin, eintrag, replay, arena.get(), spieler, herkunft);
            wiedergaben.put(id, wiedergabe);
            wiedergabe.starten(spieler);
            plugin.msgs().send(spieler, "replay-gestartet", "a", eintrag.spielerAName(), "b", eintrag.spielerBName(),
                    "datum", datum(eintrag.start()), "dauer", ReplayWiedergabe.zeit((int) (eintrag.dauerMillis() / 50)));
            steuerleiste(spieler);
        }));
    }

    public void steuerleiste(Player spieler) {
        plugin.msgs().send(spieler, "replay-steuerung");
    }

    public void steuern(Player spieler, String aktion, String wert) {
        ReplayWiedergabe wiedergabe = wiedergaben.get(spieler.getUniqueId());
        if (wiedergabe == null) {
            plugin.msgs().send(spieler, "replay-keine-wiedergabe");
            return;
        }
        switch (aktion) {
            case "pause" -> wiedergabe.pause(!wiedergabe.pausiert());
            case "weiter" -> wiedergabe.pause(false);
            case "tempo" -> {
                Double tempo = tempoLesen(wert);
                if (tempo == null) {
                    steuerleiste(spieler);
                    return;
                }
                wiedergabe.tempo(tempo);
                plugin.msgs().send(spieler, "replay-tempo", "tempo", ReplayWiedergabe.tempoText(wiedergabe.tempo()));
            }
            case "springen" -> {
                if (wert == null) {
                    steuerleiste(spieler);
                    return;
                }
                if (wert.contains(":")) {
                    Integer sekunden = zeitLesen(wert);
                    if (sekunden != null) {
                        wiedergabe.springenAuf(sekunden * 20);
                    }
                } else {
                    try {
                        wiedergabe.springen((int) Math.round(Double.parseDouble(wert.replace(',', '.')) * 20));
                    } catch (NumberFormatException fehler) {
                        steuerleiste(spieler);
                    }
                }
            }
            case "stop" -> beenden(spieler.getUniqueId(), true);
            default -> steuerleiste(spieler);
        }
    }

    static Double tempoLesen(String wert) {
        if (wert == null) {
            return null;
        }
        try {
            double tempo = Double.parseDouble(wert.toLowerCase(Locale.ROOT).replace("x", "").replace(',', '.').trim());
            return tempo > 0 ? Math.max(0.25, Math.min(4, tempo)) : null;
        } catch (NumberFormatException fehler) {
            return null;
        }
    }

    static Integer zeitLesen(String wert) {
        String[] teile = wert.split(":");
        if (teile.length != 2) {
            return null;
        }
        try {
            return Integer.parseInt(teile[0].trim()) * 60 + Integer.parseInt(teile[1].trim());
        } catch (NumberFormatException fehler) {
            return null;
        }
    }

    public void beenden(UUID zuschauer, boolean zurueck) {
        ReplayWiedergabe wiedergabe = wiedergaben.remove(zuschauer);
        if (wiedergabe == null) {
            return;
        }
        wiedergabe.beenden();
        Player spieler = Bukkit.getPlayer(zuschauer);
        if (spieler != null && zurueck) {
            plugin.msgs().send(spieler, "replay-beendet");
            wiedergabe.zuschauerZurueck(spieler);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void beimVerlassen(PlayerQuitEvent event) {
        beenden(event.getPlayer().getUniqueId(), false);
    }

    public void liste(CommandSender sender, String name) {
        CompletableFuture<Optional<UUID>> wer;
        if (name == null) {
            wer = CompletableFuture.completedFuture(Optional.empty());
        } else {
            Player online = Bukkit.getPlayerExact(name);
            wer = online != null ? CompletableFuture.completedFuture(Optional.of(online.getUniqueId()))
                    : plugin.db().replaySpielerFuerName(name);
        }
        wer.thenAccept(uuid -> {
            if (name != null && uuid.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.msgs().send(sender, "replay-spieler-unbekannt", "spieler", name));
                return;
            }
            plugin.db().replayListe(uuid.orElse(null), LISTE).thenAccept(liste -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (liste.isEmpty()) {
                    plugin.msgs().send(sender, "replay-liste-leer");
                    return;
                }
                plugin.msgs().send(sender, "replay-liste-kopf", "fuer", name == null ? "" : " " + name);
                for (ReplayEintrag eintrag : liste) {
                    sender.sendMessage(zeile(eintrag));
                }
            }));
        });
    }

    public Component zeile(ReplayEintrag eintrag) {
        return plugin.msgs().format("replay-zeile",
                "id", eintrag.kurzId(),
                "datum", datum(eintrag.start()),
                "a", eintrag.spielerAName(),
                "b", eintrag.spielerBName(),
                "dauer", ReplayWiedergabe.zeit((int) (eintrag.dauerMillis() / 50)),
                "ergebnis", ergebnisText(eintrag),
                "gemeldet", eintrag.gemeldet() ? plugin.msgs().raw("replay-gemeldet-marke") : "",
                "rest", restText(eintrag.behaltenBis() - System.currentTimeMillis()));
    }

    private String ergebnisText(ReplayEintrag eintrag) {
        return switch (eintrag.ergebnis()) {
            case ReplayDaten.ERGEBNIS_SIEG -> plugin.msgs().raw("replay-ergebnis-sieg").replace("%spieler%",
                    eintrag.spielerA().equals(eintrag.gewinner()) ? eintrag.spielerAName() : eintrag.spielerBName());
            case ReplayDaten.ERGEBNIS_UNENTSCHIEDEN -> plugin.msgs().raw("replay-ergebnis-unentschieden");
            case ReplayDaten.ERGEBNIS_AUFGABE -> plugin.msgs().raw("replay-ergebnis-aufgabe");
            default -> plugin.msgs().raw("replay-ergebnis-abgebrochen");
        };
    }

    static String restText(long millis) {
        if (millis <= 0) {
            return "0 Std.";
        }
        long stunden = millis / 3_600_000L;
        return stunden >= 48 ? (stunden / 24) + " Tage" : Math.max(1, stunden) + " Std.";
    }

    private static String datum(long millis) {
        return DATUM.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    public void behalten(CommandSender sender, String id) {
        plugin.db().replayHolen(id).thenAccept(eintragOpt -> {
            if (eintragOpt.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.msgs().send(sender, "replay-nicht-gefunden", "id", id));
                return;
            }
            ReplayEintrag eintrag = eintragOpt.get();
            long bis = eintrag.start() + speicher.gemeldetMillis();
            plugin.db().replayBehalten(eintrag.id(), bis).thenAccept(ok -> Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.msgs().send(sender, "replay-behalten", "id", eintrag.kurzId(),
                            "datum", DATUM_LANG.format(Instant.ofEpochMilli(Math.max(bis, eintrag.behaltenBis())).atZone(ZoneId.systemDefault())))));
        });
    }

    public void speicherZeigen(CommandSender sender) {
        plugin.db().replayListe(null, Integer.MAX_VALUE).thenAccept(liste -> {
            long belegt = liste.stream().mapToLong(ReplayEintrag::groesse).sum();
            long gemeldet = liste.stream().filter(ReplayEintrag::gemeldet).count();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.msgs().send(sender, "replay-speicher",
                    "anzahl", String.valueOf(liste.size()), "gemeldet", String.valueOf(gemeldet),
                    "belegt", String.valueOf(belegt / (1024 * 1024)), "grenze", String.valueOf(speicher.maxBytes() / (1024 * 1024))));
        });
    }
}
