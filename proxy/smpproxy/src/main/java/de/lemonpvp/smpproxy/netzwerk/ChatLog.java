package de.lemonpvp.smpproxy.netzwerk;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import de.lemonpvp.smpproxy.SMPProxy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public final class ChatLog {

    public enum Art { CHAT, MSG, GESPERRT }

    public record Eintrag(long zeit, Art art, String ort, String text) {
    }

    static final int MAX_ZEILEN = 20_000;

    private static final int MAX_TEXT = 1_000;
    private static final long TAG_MILLIS = 86_400_000L;

    private final SMPProxy plugin;
    private final Path ordner;
    private final ExecutorService schreiber = Executors.newSingleThreadExecutor(lauf -> {
        Thread faden = new Thread(lauf, "SMPProxy-ChatLog");
        faden.setDaemon(true);
        return faden;
    });

    public ChatLog(SMPProxy plugin, Path ordner) {
        this.plugin = plugin;
        this.ordner = ordner;
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public void beimChat(PlayerChatEvent event) {
        Player spieler = event.getPlayer();
        String server = ChatRelay.serverVon(spieler);
        if (server.isEmpty()) {
            server = "-";
        }
        if (!event.getResult().isAllowed()) {
            schreiben(spieler.getUniqueId(), Art.GESPERRT, "abgelehnt · " + server, event.getMessage());
        } else if (plugin.stummListe().restMillis(spieler.getUniqueId()) != 0) {
            schreiben(spieler.getUniqueId(), Art.GESPERRT, "stumm · " + server, event.getMessage());
        } else {
            schreiben(spieler.getUniqueId(), Art.CHAT, server, event.getMessage());
        }
    }

    public void privat(Player sender, Player ziel, String text, boolean blockiert) {
        if (blockiert) {
            schreiben(sender.getUniqueId(), Art.GESPERRT, "Filter · /msg → " + ziel.getUsername(), text);
        } else {
            schreiben(sender.getUniqueId(), Art.MSG, ziel.getUsername(), text);
        }
    }

    public void privatStumm(Player sender, String zielName, String text) {
        schreiben(sender.getUniqueId(), Art.GESPERRT, "stumm · /msg → " + zielName, text);
    }

    public CompletableFuture<List<Eintrag>> lesen(UUID spieler) {
        try {
            return CompletableFuture.supplyAsync(() -> leseDatei(datei(spieler)), schreiber);
        } catch (RejectedExecutionException beendet) {
            return CompletableFuture.completedFuture(leseDatei(datei(spieler)));
        }
    }

    public void aufraeumen() {
        ausfuehren(this::aufraeumenJetzt);
    }

    @Subscribe
    public void beimHerunterfahren(ProxyShutdownEvent event) {
        schreiber.shutdown();
        try {
            schreiber.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException unterbrochen) {
            Thread.currentThread().interrupt();
        }
    }

    private void schreiben(UUID spieler, Art art, String ort, String text) {
        if (!plugin.config().chatlogEnabled() || text == null || text.isBlank()) {
            return;
        }
        String zeile = System.currentTimeMillis() + "\t" + art.name() + "\t" + sauber(ort) + "\t" + sauber(text) + "\n";
        Path ziel = datei(spieler);
        ausfuehren(() -> {
            try {
                Files.createDirectories(ordner);
                Files.writeString(ziel, zeile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException fehler) {
                plugin.log().warn("Chat-Log konnte nicht geschrieben werden: {}", fehler.getMessage());
            }
        });
    }

    private void ausfuehren(Runnable aufgabe) {
        try {
            schreiber.execute(aufgabe);
        } catch (RejectedExecutionException beendet) {
            aufgabe.run();
        }
    }

    private List<Eintrag> leseDatei(Path datei) {
        List<Eintrag> eintraege = new ArrayList<>();
        if (Files.notExists(datei)) {
            return eintraege;
        }
        long grenze = grenze();
        try {
            for (String zeile : Files.readAllLines(datei, StandardCharsets.UTF_8)) {
                Eintrag eintrag = parsen(zeile);
                if (eintrag != null && eintrag.zeit() >= grenze) {
                    eintraege.add(eintrag);
                }
            }
        } catch (IOException fehler) {
            plugin.log().warn("Chat-Log {} konnte nicht gelesen werden: {}", datei.getFileName(), fehler.getMessage());
        }
        return eintraege;
    }

    private void aufraeumenJetzt() {
        if (Files.notExists(ordner)) {
            return;
        }
        long grenze = grenze();
        int geloescht = 0;
        try (DirectoryStream<Path> dateien = Files.newDirectoryStream(ordner, "*.log")) {
            for (Path datei : dateien) {
                try {
                    List<String> zeilen = Files.readAllLines(datei, StandardCharsets.UTF_8);
                    List<String> behalten = new ArrayList<>();
                    for (String zeile : zeilen) {
                        Eintrag eintrag = parsen(zeile);
                        if (eintrag != null && eintrag.zeit() >= grenze) {
                            behalten.add(zeile);
                        }
                    }
                    if (behalten.size() > MAX_ZEILEN) {
                        behalten = new ArrayList<>(behalten.subList(behalten.size() - MAX_ZEILEN, behalten.size()));
                    }
                    geloescht += zeilen.size() - behalten.size();
                    if (behalten.isEmpty()) {
                        Files.deleteIfExists(datei);
                    } else if (behalten.size() != zeilen.size()) {
                        Files.write(datei, behalten, StandardCharsets.UTF_8);
                    }
                } catch (IOException fehler) {
                    plugin.log().warn("Chat-Log {} konnte nicht aufgeräumt werden: {}", datei.getFileName(), fehler.getMessage());
                }
            }
        } catch (IOException fehler) {
            plugin.log().warn("Chat-Logs konnten nicht aufgeräumt werden: {}", fehler.getMessage());
        }
        if (geloescht > 0) {
            plugin.log().info("Chat-Log: {} Nachrichten älter als {} Tage gelöscht.", geloescht, plugin.config().chatlogTage());
        }
    }

    private long grenze() {
        return System.currentTimeMillis() - plugin.config().chatlogTage() * TAG_MILLIS;
    }

    private Path datei(UUID spieler) {
        return ordner.resolve(spieler + ".log");
    }

    static Eintrag parsen(String zeile) {
        String[] teile = zeile.split("\t", 4);
        if (teile.length < 4) {
            return null;
        }
        try {
            return new Eintrag(Long.parseLong(teile[0]), Art.valueOf(teile[1]), teile[2], teile[3]);
        } catch (IllegalArgumentException kaputt) {
            return null;
        }
    }

    static String sauber(String text) {
        String einzeilig = text.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
        return einzeilig.length() > MAX_TEXT ? einzeilig.substring(0, MAX_TEXT) : einzeilig;
    }
}
