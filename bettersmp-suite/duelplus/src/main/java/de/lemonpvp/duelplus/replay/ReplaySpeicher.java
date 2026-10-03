package de.lemonpvp.duelplus.replay;

import de.lemonpvp.duelplus.DuelPlus;
import de.lemonpvp.duelplus.db.ReplayEintrag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class ReplaySpeicher {

    public static final String ENDUNG = ".bpr";
    private static final long TAG = 24L * 60 * 60 * 1000;
    private static final long MB = 1024L * 1024;
    private static final long WAISE_NACH_MILLIS = 6L * 60 * 60 * 1000;

    public record Fertig(String id, UUID spielerA, String spielerAName, UUID spielerB, String spielerBName, String arena,
                         long start, long dauer, UUID gewinner, int ergebnis, byte[] daten) {
    }

    public record Bilanz(int abgelaufen, int platz, int waisen, long belegt) {
    }

    private final DuelPlus plugin;
    private final Path ordner;
    private final LongSupplier uhr;
    private final ExecutorService arbeiter = Executors.newSingleThreadExecutor(aufgabe -> {
        Thread thread = new Thread(aufgabe, "DuelPlus-Replays");
        thread.setDaemon(true);
        return thread;
    });

    public ReplaySpeicher(DuelPlus plugin, Path ordner) {
        this(plugin, ordner, System::currentTimeMillis);
    }

    public ReplaySpeicher(DuelPlus plugin, Path ordner, LongSupplier uhr) {
        this.plugin = plugin;
        this.ordner = ordner;
        this.uhr = uhr;
    }

    public Path datei(String id) {
        return ordner.resolve(id + ENDUNG);
    }

    public long aufbewahrenMillis() {
        return Math.max(1, plugin.getConfig().getInt("replay.aufbewahren-tage", 3)) * TAG;
    }

    public long gemeldetMillis() {
        return Math.max(1, plugin.getConfig().getInt("replay.gemeldet-tage", 30)) * TAG;
    }

    public long maxBytes() {
        double gb = plugin.getConfig().getDouble("replay.max-gb", 3.0);
        return (long) (Math.max(0.0001, gb) * 1024 * MB);
    }

    public long maxBytesProReplay() {
        return Math.max(1, plugin.getConfig().getInt("replay.max-mb-pro-replay", 50)) * MB;
    }

    public void ablegen(Fertig fertig, boolean sofort) {
        Runnable arbeit = () -> schreiben(fertig);
        if (sofort || arbeiter.isShutdown()) {
            arbeit.run();
        } else {
            arbeiter.execute(arbeit);
        }
    }

    private void schreiben(Fertig fertig) {
        try {
            Files.createDirectories(ordner);
            Path ziel = datei(fertig.id());
            Path neu = ordner.resolve(fertig.id() + ENDUNG + ".neu");
            Files.write(neu, fertig.daten());
            Files.move(neu, ziel, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException fehler) {
            plugin.getLogger().warning("Replay " + fertig.id() + " konnte nicht gespeichert werden: " + fehler.getMessage());
            return;
        }
        if (!plugin.db().bereit()) {
            return;
        }
        ReplayEintrag eintrag = new ReplayEintrag(fertig.id(), fertig.spielerA(), fertig.spielerAName(),
                fertig.spielerB(), fertig.spielerBName(), fertig.arena(), fertig.start(), fertig.dauer(),
                fertig.daten().length, fertig.gewinner(), fertig.ergebnis(), false,
                fertig.start() + aufbewahrenMillis(), plugin.serverName());
        plugin.db().replayEintragen(eintrag).join();
        plugin.db().replayNachMeldungPruefen(eintrag, gemeldetMillis()).join();
    }

    public CompletableFuture<ReplayDaten.Replay> laden(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream ein = Files.newInputStream(datei(id))) {
                return ReplayDaten.lesen(ein);
            } catch (IOException fehler) {
                throw new IllegalStateException(fehler.getMessage(), fehler);
            }
        }, arbeiter);
    }

    public boolean vorhanden(String id) {
        return Files.isRegularFile(datei(id));
    }

    public CompletableFuture<Bilanz> aufraeumenAsync() {
        return CompletableFuture.supplyAsync(this::aufraeumen, arbeiter);
    }

    public Bilanz aufraeumen() {
        if (!plugin.db().bereit()) {
            return new Bilanz(0, 0, 0, belegt());
        }
        List<ReplayEintrag> eintraege = plugin.db().replaysAufServer(plugin.serverName()).join();
        if (eintraege == null) {
            return new Bilanz(0, 0, 0, belegt());
        }
        long jetzt = uhr.getAsLong();
        Map<String, Long> dateien = dateien();
        Set<String> eigene = new HashSet<>();
        for (ReplayEintrag eintrag : eintraege) {
            eigene.add(eintrag.id());
        }
        Set<String> fremd = new HashSet<>(dateien.keySet());
        fremd.removeAll(eigene);
        if (!fremd.isEmpty() && plugin.db().replaysUebernehmen(fremd, plugin.serverName()).join() > 0) {
            eintraege = plugin.db().replaysAufServer(plugin.serverName()).join();
            if (eintraege == null) {
                return new Bilanz(0, 0, 0, belegt());
            }
        }
        int abgelaufen = 0;
        int platz = 0;
        int waisen = 0;
        List<ReplayEintrag> uebrig = new ArrayList<>();
        Set<String> bekannt = new HashSet<>();
        for (ReplayEintrag eintrag : eintraege) {
            bekannt.add(eintrag.id());
            if (eintrag.behaltenBis() < jetzt) {
                loeschen(eintrag.id());
                dateien.remove(eintrag.id());
                abgelaufen++;
            } else if (!dateien.containsKey(eintrag.id())) {
                plugin.db().replayLoeschen(eintrag.id()).join();
                waisen++;
            } else {
                uebrig.add(eintrag);
            }
        }
        for (Map.Entry<String, Long> datei : new HashMap<>(dateien).entrySet()) {
            if (bekannt.contains(datei.getKey())) {
                continue;
            }
            try {
                if (jetzt - Files.getLastModifiedTime(datei(datei.getKey())).toMillis() > WAISE_NACH_MILLIS) {
                    Files.deleteIfExists(datei(datei.getKey()));
                    dateien.remove(datei.getKey());
                    waisen++;
                }
            } catch (IOException ignoriert) {
            }
        }
        long belegt = dateien.values().stream().mapToLong(Long::longValue).sum();
        long grenze = maxBytes();
        if (belegt > grenze) {
            long ziel = (long) (grenze * 0.95);
            uebrig.sort(Comparator.comparing(ReplayEintrag::gemeldet).thenComparingLong(ReplayEintrag::start));
            for (ReplayEintrag eintrag : uebrig) {
                if (belegt <= ziel) {
                    break;
                }
                long groesse = dateien.getOrDefault(eintrag.id(), 0L);
                loeschen(eintrag.id());
                belegt -= groesse;
                platz++;
                if (eintrag.gemeldet()) {
                    plugin.getLogger().warning("Replay-Speicher voll: auch das gemeldete Replay " + eintrag.kurzId()
                            + " (" + eintrag.spielerAName() + " vs " + eintrag.spielerBName() + ") musste weichen.");
                }
            }
        }
        if (abgelaufen + platz + waisen > 0) {
            plugin.getLogger().info("Replays aufgeraeumt: " + abgelaufen + " abgelaufen, " + platz + " fuer Platz, "
                    + waisen + " verwaist - belegt jetzt " + (belegt / MB) + " MB von " + (grenze / MB) + " MB.");
        }
        return new Bilanz(abgelaufen, platz, waisen, belegt);
    }

    private void loeschen(String id) {
        try {
            Files.deleteIfExists(datei(id));
        } catch (IOException fehler) {
            plugin.getLogger().warning("Replay " + id + " liess sich nicht loeschen: " + fehler.getMessage());
        }
        plugin.db().replayLoeschen(id).join();
    }

    private Map<String, Long> dateien() {
        Map<String, Long> liste = new HashMap<>();
        if (!Files.isDirectory(ordner)) {
            return liste;
        }
        try (DirectoryStream<Path> strom = Files.newDirectoryStream(ordner, "*" + ENDUNG)) {
            for (Path pfad : strom) {
                String name = pfad.getFileName().toString();
                liste.put(name.substring(0, name.length() - ENDUNG.length()), Files.size(pfad));
            }
        } catch (IOException fehler) {
            plugin.getLogger().warning("Replay-Ordner nicht lesbar: " + fehler.getMessage());
        }
        return liste;
    }

    public long belegt() {
        return dateien().values().stream().mapToLong(Long::longValue).sum();
    }

    public void schliessen() {
        arbeiter.shutdown();
        try {
            arbeiter.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException fehler) {
            Thread.currentThread().interrupt();
        }
    }
}
