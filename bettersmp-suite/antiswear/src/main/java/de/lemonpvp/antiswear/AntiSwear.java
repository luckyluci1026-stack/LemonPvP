package de.lemonpvp.antiswear;

import de.lemonpvp.antiswear.command.AntiSwearCommand;
import de.lemonpvp.antiswear.filter.WortFilter;
import de.lemonpvp.antiswear.listener.ChatFilterListener;
import de.lemonpvp.antiswear.strikes.Stufe;
import de.lemonpvp.antiswear.strikes.StrikeManager;
import de.lemonpvp.antiswear.util.Durations;
import de.lemonpvp.antiswear.util.Msgs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chat-Filter mit Umgehungsschutz (Leetspeak, Trennzeichen, wiederholte
 * Buchstaben), Punkte-Stufen mit eigener Kurzzeit-Stummschaltung und
 * optionalen Konsolenbefehlen fuer weitergehende Strafen (z.B. an
 * AdvancedBan oder jedes andere gerade installierte Plugin).
 *
 * Eigenstaendig, ohne andere Plugins zu beruehren - "aktion: BEFEHL"
 * in der config.yml reicht als Bruecke zu jedem Bann-/Mute-Plugin, ohne
 * dass AntiSwear dessen API kennen muesste.
 */
public final class AntiSwear extends JavaPlugin {

    public enum Modus { ERSETZEN, BLOCKIEREN }

    private Msgs msgs;
    private final WortFilter filter = new WortFilter();
    private StrikeManager strikes;

    private Modus modus = Modus.ERSETZEN;
    private int verfallMinuten = 60;
    private List<Stufe> stufen = List.of();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.msgs = new Msgs(this);
        this.strikes = new StrikeManager(this);
        ladeAlles();

        getServer().getPluginManager().registerEvents(new ChatFilterListener(this), this);
        AntiSwearCommand befehl = new AntiSwearCommand(this);
        var command = getCommand("antiswear");
        if (command != null) {
            command.setExecutor(befehl);
            command.setTabCompleter(befehl);
        }

        getLogger().info("AntiSwear aktiviert (" + filter.woerter().size() + " Woerter, Modus " + modus + ").");
    }

    /** Von /antiswear reload aufgerufen - liest config.yml UND woerter.yml neu ein. */
    public void ladeAlles() {
        reloadConfig();
        modus = Modus.valueOf(getConfig().getString("erkennung.modus", "ERSETZEN").toUpperCase(Locale.ROOT));
        filter.konfigurieren(
                getConfig().getBoolean("erkennung.leetspeak", true),
                getConfig().getBoolean("erkennung.trennzeichen", true),
                getConfig().getBoolean("erkennung.wiederholungen", true));
        filter.setWoerter(ladeWoerter());
        verfallMinuten = Math.max(1, getConfig().getInt("strikes.verfall-minuten", 60));
        stufen = ladeStufen();
    }

    private Map<String, Integer> ladeWoerter() {
        Map<String, Integer> ergebnis = new LinkedHashMap<>();
        File datei = new File(getDataFolder(), "woerter.yml");
        if (!datei.exists()) {
            saveResource("woerter.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(datei);
        ConfigurationSection sec = yaml.getConfigurationSection("woerter");
        if (sec != null) {
            for (String wort : sec.getKeys(false)) {
                ergebnis.put(wort, sec.getInt(wort, 1));
            }
        }
        return ergebnis;
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
                long dauer = Durations.parse(stufe.getString("dauer", "0"));
                String befehl = stufe.getString("befehl", "");
                ergebnis.add(new Stufe(schwelle, aktion, dauer, befehl));
            }
        }
        ergebnis.sort(Comparator.comparingInt(Stufe::schwelle));
        return ergebnis;
    }

    public Msgs msgs() {
        return msgs;
    }

    public WortFilter filter() {
        return filter;
    }

    public StrikeManager strikes() {
        return strikes;
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
