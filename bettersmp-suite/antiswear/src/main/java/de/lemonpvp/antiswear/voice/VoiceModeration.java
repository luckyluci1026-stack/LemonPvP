package de.lemonpvp.antiswear.voice;

import de.lemonpvp.antiswear.AntiSwear;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import de.lemonpvp.antiswear.util.Durations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class VoiceModeration implements Listener {

    private static final long HINWEIS_ABSTAND_MILLIS = 3_000L;
    private static final long AUFBEWAHREN_MILLIS = 90L * 24 * 60 * 60 * 1000;

    private final AntiSwear plugin;
    private final VoiceDaten daten = new VoiceDaten();
    private final File datei;
    private final Map<UUID, Long> letzterStummHinweis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> letzterRegelHinweis = new ConcurrentHashMap<>();
    private final Set<UUID> regelnGezeigt = ConcurrentHashMap.newKeySet();
    private final ExecutorService schreiber = Executors.newSingleThreadExecutor(aufgabe -> {
        Thread thread = new Thread(aufgabe, "AntiSwear-Voice");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean pflicht = true;
    private volatile int version = 1;
    private volatile boolean gruppenPruefen = true;
    private volatile boolean verbunden;

    public VoiceModeration(AntiSwear plugin) {
        this.plugin = plugin;
        this.datei = new File(plugin.getDataFolder(), "voice.yml");
    }

    public void konfigurieren(FileConfiguration config) {
        pflicht = config.getBoolean("voice.regeln-pflicht", true);
        version = Math.max(1, config.getInt("voice.regeln-version", 1));
        gruppenPruefen = config.getBoolean("voice.gruppennamen-pruefen", true);
    }

    public void laden() {
        daten.laden(YamlConfiguration.loadConfiguration(datei), System.currentTimeMillis(), AUFBEWAHREN_MILLIS);
    }

    public void schliessen() {
        schreiber.shutdown();
        try {
            schreiber.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException fehler) {
            Thread.currentThread().interrupt();
        }
    }

    private void speichern() {
        String inhalt = daten.alsYaml().saveToString();
        schreiber.execute(() -> {
            try {
                Files.createDirectories(datei.getParentFile().toPath());
                File neu = new File(datei.getParentFile(), datei.getName() + ".neu");
                Files.writeString(neu.toPath(), inhalt, StandardCharsets.UTF_8);
                Files.move(neu.toPath(), datei.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fehler) {
                plugin.getLogger().warning("voice.yml liess sich nicht speichern: " + fehler.getMessage());
            }
        });
    }

    public void setVerbunden(boolean verbunden) {
        this.verbunden = verbunden;
    }

    public boolean verbunden() {
        return verbunden;
    }

    public VoiceDaten daten() {
        return daten;
    }

    public boolean hatZugestimmt(UUID spieler) {
        return daten.zugestimmteVersion(spieler) >= version;
    }

    public long stummBis(UUID spieler) {
        return daten.stummBis(spieler, System.currentTimeMillis());
    }

    public boolean darfSprechen(UUID spieler) {
        long bis = stummBis(spieler);
        if (bis > 0) {
            hinweisen(letzterStummHinweis, spieler, p -> p.sendActionBar(plugin.msgs().format("voice.hinweis-stumm", "rest", rest(bis))));
            return false;
        }
        if (pflicht && !hatZugestimmt(spieler)) {
            hinweisen(letzterRegelHinweis, spieler, p -> {
                p.sendActionBar(plugin.msgs().format("voice.hinweis-regeln"));
                if (regelnGezeigt.add(spieler)) {
                    regelnZeigen(p);
                }
            });
            return false;
        }
        return true;
    }

    private void hinweisen(Map<UUID, Long> letzte, UUID spieler, java.util.function.Consumer<Player> aktion) {
        long jetzt = System.currentTimeMillis();
        Long zuletzt = letzte.get(spieler);
        if (zuletzt != null && jetzt - zuletzt < HINWEIS_ABSTAND_MILLIS) {
            return;
        }
        letzte.put(spieler, jetzt);
        naechsterTick(() -> {
            Player p = Bukkit.getPlayer(spieler);
            if (p != null) {
                aktion.accept(p);
            }
        });
    }

    public boolean gruppennameErlaubt(UUID spieler, String name) {
        if (!gruppenPruefen || name == null || name.isBlank()) {
            return true;
        }
        Player p = Bukkit.getPlayer(spieler);
        if (p != null && p.hasPermission("antiswear.bypass")) {
            return true;
        }
        ChatPruefung.Ergebnis ergebnis = plugin.pruefung().pruefenOhneSpam(name);
        if (!ergebnis.verstoss()) {
            return true;
        }
        naechsterTick(() -> {
            Player online = Bukkit.getPlayer(spieler);
            if (online != null) {
                plugin.msgs().send(online, "voice.gruppe-verboten");
                plugin.moderator().verarbeiten(online, ergebnis.ohneHinweis(), plugin.msgs().raw("ort.voice-gruppe"), name);
            }
        });
        return false;
    }

    public void voiceVerbunden(UUID spieler) {
        naechsterTick(() -> {
            Player p = Bukkit.getPlayer(spieler);
            if (p != null && pflicht && !hatZugestimmt(spieler) && regelnGezeigt.add(spieler)) {
                regelnZeigen(p);
            }
        });
    }

    @EventHandler
    public void beimJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                beimBetreten(p);
            }
        }, 40L);
    }

    @EventHandler
    public void beimVerlassen(PlayerQuitEvent event) {
        letzterStummHinweis.remove(event.getPlayer().getUniqueId());
        letzterRegelHinweis.remove(event.getPlayer().getUniqueId());
        regelnGezeigt.remove(event.getPlayer().getUniqueId());
    }

    public void regelnZeigen(CommandSender empfaenger) {
        List<Component> zeilen = new ArrayList<>();
        for (String zeile : plugin.msgs().liste("voice.regeln")) {
            zeilen.add(zeile.isEmpty() ? Component.empty() : plugin.msgs().text(zeile));
        }
        if (empfaenger instanceof Player p) {
            zeilen.add(Component.empty());
            zeilen.add(plugin.msgs().format(hatZugestimmt(p.getUniqueId()) ? "voice.schon-akzeptiert" : "voice.akzeptieren-knopf"));
        }
        empfaenger.sendMessage(Component.join(JoinConfiguration.newlines(), zeilen));
    }

    public void zustimmen(Player p) {
        UUID id = p.getUniqueId();
        if (hatZugestimmt(id)) {
            plugin.msgs().send(p, "voice.schon-akzeptiert");
            return;
        }
        VoiceDaten.Eintrag eintrag = new VoiceDaten.Eintrag(version,
                daten.naechsteZeit(VoiceDaten.REGELN, id, System.currentTimeMillis()), p.getName(), "", "");
        daten.uebernehmen(VoiceDaten.REGELN, id, eintrag);
        speichern();
        plugin.netzwerk().voiceMelden(VoiceDaten.REGELN, id, eintrag);
        plugin.msgs().send(p, "voice.akzeptiert");
        plugin.protokoll().schreiben(plugin.servername(), p.getName(), "Voice", "Regeln akzeptiert (Version " + version + ")");
    }

    public void stummschalten(CommandSender von, UUID ziel, String name, long bis, String grund) {
        VoiceDaten.Eintrag eintrag = new VoiceDaten.Eintrag(bis,
                daten.naechsteZeit(VoiceDaten.STUMM, ziel, System.currentTimeMillis()), name, von.getName(), grund);
        daten.uebernehmen(VoiceDaten.STUMM, ziel, eintrag);
        speichern();
        plugin.netzwerk().voiceMelden(VoiceDaten.STUMM, ziel, eintrag);
        String dauer = rest(bis);
        Player betroffen = Bukkit.getPlayer(ziel);
        if (betroffen != null) {
            plugin.msgs().send(betroffen, "voice.stumm-du", "dauer", dauer, "grund", grund);
        }
        teamHinweis(von, "voice.team-stumm", "von", von.getName(), "spieler", name, "dauer", dauer, "grund", grund);
        plugin.protokoll().schreiben(plugin.servername(), name, "Voice", "stumm " + dauer + " von " + von.getName(), grund);
    }

    public boolean aufheben(CommandSender von, UUID ziel, String name) {
        if (stummBis(ziel) == 0) {
            return false;
        }
        VoiceDaten.Eintrag eintrag = new VoiceDaten.Eintrag(0,
                daten.naechsteZeit(VoiceDaten.STUMM, ziel, System.currentTimeMillis()), name, von.getName(), "");
        daten.uebernehmen(VoiceDaten.STUMM, ziel, eintrag);
        speichern();
        plugin.netzwerk().voiceMelden(VoiceDaten.STUMM, ziel, eintrag);
        Player betroffen = Bukkit.getPlayer(ziel);
        if (betroffen != null) {
            plugin.msgs().send(betroffen, "voice.entstummt-du");
        }
        teamHinweis(von, "voice.team-entstummt", "von", von.getName(), "spieler", name);
        plugin.protokoll().schreiben(plugin.servername(), name, "Voice", "Stummschaltung aufgehoben von " + von.getName());
        return true;
    }

    private void teamHinweis(CommandSender ausloeser, String pfad, String... ersetzungen) {
        for (Player team : Bukkit.getOnlinePlayers()) {
            if (team != ausloeser && team.hasPermission("antiswear.notify")) {
                plugin.msgs().send(team, pfad, ersetzungen);
            }
        }
    }

    public void vomNetzwerk(byte art, UUID spieler, VoiceDaten.Eintrag neu) {
        VoiceDaten.Eintrag alt = daten.eintrag(art, spieler);
        if (alt != null && alt.zeit() > neu.zeit()) {
            plugin.netzwerk().voiceMelden(art, spieler, alt);
            return;
        }
        boolean warStumm = art == VoiceDaten.STUMM && stummBis(spieler) > 0;
        if (!daten.uebernehmen(art, spieler, neu)) {
            return;
        }
        speichern();
        if (art != VoiceDaten.STUMM) {
            return;
        }
        long bis = stummBis(spieler);
        Player p = Bukkit.getPlayer(spieler);
        if (p == null || warStumm == (bis > 0)) {
            return;
        }
        if (bis > 0) {
            plugin.msgs().send(p, "voice.stumm-du", "dauer", rest(bis),
                    "grund", neu.grund().isEmpty() ? plugin.msgs().raw("voice.kein-grund") : neu.grund());
        } else {
            plugin.msgs().send(p, "voice.entstummt-du");
        }
    }

    public void beimBetreten(Player p) {
        UUID id = p.getUniqueId();
        for (byte art : new byte[]{VoiceDaten.STUMM, VoiceDaten.REGELN}) {
            VoiceDaten.Eintrag eintrag = daten.eintrag(art, id);
            if (eintrag != null) {
                plugin.netzwerk().voiceMelden(art, id, eintrag);
            }
        }
    }

    public String rest(long bis) {
        if (bis == VoiceDaten.FUER_IMMER) {
            return plugin.msgs().raw("unbegrenzt");
        }
        long rest = Math.max(0, bis - System.currentTimeMillis());
        return Durations.humanize((rest + 59_999) / 60_000 * 60_000);
    }

    private void naechsterTick(Runnable aufgabe) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            aufgabe.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, aufgabe);
        }
    }
}
