package de.lemonpvp.antiswear;

import de.lemonpvp.antiswear.command.AntiSwearCommand;
import de.lemonpvp.antiswear.filter.ChatPruefung;
import de.lemonpvp.antiswear.filter.DatenFilter;
import de.lemonpvp.antiswear.filter.SpamSchutz;
import de.lemonpvp.antiswear.filter.WerbungFilter;
import de.lemonpvp.antiswear.filter.WortFilter;
import de.lemonpvp.antiswear.listener.ChatFilterListener;
import de.lemonpvp.antiswear.listener.InhaltListener;
import de.lemonpvp.antiswear.listener.Moderator;
import de.lemonpvp.antiswear.netz.NetzwerkBruecke;
import de.lemonpvp.antiswear.rechte.StandardRechte;
import de.lemonpvp.antiswear.strikes.Stufe;
import de.lemonpvp.antiswear.strikes.StrikeManager;
import de.lemonpvp.antiswear.util.Durations;
import de.lemonpvp.antiswear.util.Msgs;
import de.lemonpvp.antiswear.util.Protokoll;
import de.lemonpvp.antiswear.voice.VoiceAnbindung;
import de.lemonpvp.antiswear.voice.VoiceBefehle;
import de.lemonpvp.antiswear.voice.VoiceModeration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AntiSwear extends JavaPlugin {

    public enum Modus { ERSETZEN, BLOCKIEREN }

    private Msgs msgs;
    private final WortFilter filter = new WortFilter();
    private final SpamSchutz spam = new SpamSchutz();
    private final WerbungFilter werbung = new WerbungFilter();
    private final DatenFilter daten = new DatenFilter();
    private final ChatPruefung pruefung = new ChatPruefung(filter, spam, werbung, daten);
    private StrikeManager strikes;
    private Moderator moderator;
    private NetzwerkBruecke netzwerk;
    private Protokoll protokoll;
    private VoiceModeration voice;

    private Modus modus = Modus.ERSETZEN;
    private int verfallMinuten = 60;
    private List<Stufe> stufen = List.of();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.strikes = new StrikeManager(this);
        this.moderator = new Moderator(this);
        this.protokoll = new Protokoll(this, "protokoll.log");
        this.netzwerk = new NetzwerkBruecke(this);
        this.voice = new VoiceModeration(this);
        ladeAlles();
        voice.laden();

        netzwerk.start();
        getServer().getPluginManager().registerEvents(new ChatFilterListener(this), this);
        getServer().getPluginManager().registerEvents(new InhaltListener(this), this);
        getServer().getPluginManager().registerEvents(netzwerk, this);
        getServer().getPluginManager().registerEvents(voice, this);
        AntiSwearCommand befehl = new AntiSwearCommand(this);
        var command = getCommand("antiswear");
        if (command != null) {
            command.setExecutor(befehl);
            command.setTabCompleter(befehl);
        }
        VoiceBefehle voiceBefehle = new VoiceBefehle(this);
        for (String name : List.of("vcrules", "vcmute", "vcunmute", "vcmutes")) {
            var voiceCommand = getCommand(name);
            if (voiceCommand != null) {
                voiceCommand.setExecutor(voiceBefehle);
                voiceCommand.setTabCompleter(voiceBefehle);
            }
        }
        voiceAnbinden();
        getServer().getScheduler().runTaskLater(this, () -> StandardRechte.anwenden(this), 100L);
        getLogger().info("AntiSwear aktiviert (" + filter.woerter().size() + " Woerter, "
                + filter.anzahlAusnahmen() + " Ausnahmen, Modus " + modus + ").");
    }

    private void voiceAnbinden() {
        if (getServer().getPluginManager().getPlugin("voicechat") == null) {
            getLogger().info("Simple Voice Chat ist nicht installiert - /vcrules und /vcmute wirken erst, wenn es da ist.");
            return;
        }
        try {
            if (VoiceAnbindung.starten(this)) {
                getLogger().info("Simple Voice Chat erkannt - Voice-Moderation ist aktiv.");
            } else {
                getLogger().warning("Simple Voice Chat ist da, bietet aber keine Schnittstelle an - Voice-Moderation aus.");
            }
        } catch (LinkageError fehler) {
            getLogger().warning("Simple Voice Chat passt nicht zu dieser AntiSwear-Version - Voice-Moderation aus: " + fehler);
        }
    }

    @Override
    public void onDisable() {
        if (netzwerk != null) {
            netzwerk.stop();
        }
        if (voice != null) {
            voice.schliessen();
        }
        if (protokoll != null) {
            protokoll.schliessen();
        }
    }

    public void ladeAlles() {
        reloadConfig();
        msgs.reload();
        try {
            modus = Modus.valueOf(getConfig().getString("erkennung.modus", "ERSETZEN").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException fehler) {
            getLogger().warning("erkennung.modus ist weder ERSETZEN noch BLOCKIEREN - ERSETZEN wird genutzt.");
            modus = Modus.ERSETZEN;
        }
        filter.konfigurieren(
                getConfig().getBoolean("erkennung.leetspeak", true),
                getConfig().getBoolean("erkennung.trennzeichen", true),
                getConfig().getBoolean("erkennung.wiederholungen", true));
        ladeWoerter();
        spam.konfigurieren(
                getConfig().getInt("spam.max-nachrichten", 5),
                getConfig().getLong("spam.zeitraum-sekunden", 8) * 1000L,
                getConfig().getLong("spam.gleiche-nachricht-sekunden", 30) * 1000L);
        werbung.setErlaubt(getConfig().getStringList("werbung.erlaubte-domains"));
        pruefung.setEinstellungen(new ChatPruefung.Einstellungen(
                modus == Modus.ERSETZEN,
                getConfig().getBoolean("spam.aktiv", true),
                getConfig().getInt("spam.punkte", 1),
                getConfig().getBoolean("werbung.aktiv", true),
                getConfig().getInt("werbung.punkte", 2),
                getConfig().getBoolean("daten.aktiv", true),
                getConfig().getInt("daten.punkte", 0),
                getConfig().getBoolean("caps.aktiv", true),
                getConfig().getInt("caps.mindest-buchstaben", 8),
                getConfig().getInt("caps.anteil-prozent", 70) / 100.0,
                getConfig().getInt("zeichen-wiederholung.maximal", 4)));
        verfallMinuten = Math.max(1, getConfig().getInt("strikes.verfall-minuten", 60));
        stufen = ladeStufen();
        if (voice != null) {
            voice.konfigurieren(getConfig());
        }
    }

    private void ladeWoerter() {
        File datei = new File(getDataFolder(), "woerter.yml");
        if (!datei.exists()) {
            saveResource("woerter.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(datei);
        List<WortFilter.Definition> definitionen = new ArrayList<>();
        ConfigurationSection bereich = yaml.getConfigurationSection("woerter");
        if (bereich != null) {
            for (String wort : bereich.getKeys(false)) {
                ConfigurationSection einzeln = bereich.getConfigurationSection(wort);
                if (einzeln == null) {
                    definitionen.add(new WortFilter.Definition(wort, bereich.getInt(wort, 1), null));
                    continue;
                }
                String modusText = einzeln.getString("modus", "").toLowerCase(Locale.ROOT);
                WortFilter.Modus wortModus = switch (modusText) {
                    case "teil" -> WortFilter.Modus.TEIL;
                    case "wort" -> WortFilter.Modus.WORT;
                    default -> null;
                };
                definitionen.add(new WortFilter.Definition(wort, einzeln.getInt("punkte", 1), wortModus));
            }
        }
        filter.setDefinitionen(definitionen, yaml.getStringList("ausnahmen"));
    }

    private List<Stufe> ladeStufen() {
        List<Stufe> ergebnis = new ArrayList<>();
        ConfigurationSection sec = getConfig().getConfigurationSection("strikes.stufen");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection stufe = sec.getConfigurationSection(key);
                if (stufe == null) {
                    continue;
                }
                int schwelle;
                try {
                    schwelle = Integer.parseInt(key.trim());
                } catch (NumberFormatException ex) {
                    getLogger().warning("Stufen-Schwelle '" + key + "' ist keine Zahl - ignoriert.");
                    continue;
                }
                String aktionRoh = stufe.getString("aktion", "WARNEN");
                Stufe.Aktion aktion;
                try {
                    aktion = Stufe.Aktion.valueOf(aktionRoh.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    getLogger().warning("Unbekannte Aktion '" + aktionRoh + "' bei Stufe " + schwelle + " - ignoriert.");
                    continue;
                }
                String dauerText = stufe.getString("dauer", "0").trim().toLowerCase(Locale.ROOT);
                long dauer = dauerText.startsWith("perm") ? -1 : Durations.parse(dauerText);
                ergebnis.add(new Stufe(schwelle, aktion, dauer, stufe.getString("befehl", "")));
            }
        }
        ergebnis.sort(Comparator.comparingInt(Stufe::schwelle));
        return ergebnis;
    }

    public void stummHinweis(Player spieler) {
        long rest = strikes.stummRestMillis(spieler.getUniqueId());
        msgs.send(spieler, "stummgeschaltet", "rest", rest < 0 ? msgs.raw("unbegrenzt") : Durations.humanize(rest));
    }

    public String servername() {
        return getConfig().getString("protokoll.server", "");
    }

    public Msgs msgs() {
        return msgs;
    }

    public WortFilter filter() {
        return filter;
    }

    public ChatPruefung pruefung() {
        return pruefung;
    }

    public StrikeManager strikes() {
        return strikes;
    }

    public Moderator moderator() {
        return moderator;
    }

    public NetzwerkBruecke netzwerk() {
        return netzwerk;
    }

    public Protokoll protokoll() {
        return protokoll;
    }

    public VoiceModeration voice() {
        return voice;
    }

    public Modus modus() {
        return modus;
    }

    public int verfallMinuten() {
        return verfallMinuten;
    }

    public List<Stufe> stufen() {
        return stufen;
    }
}
