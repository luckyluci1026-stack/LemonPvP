package de.lemonpvp.bettersmp.xray;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class AntiXrayEinrichtung implements Listener {

    public record Welt(String name, World.Environment umgebung, File ordner) {
    }

    public record Ergebnis(List<String> welten, boolean seedSchutz, List<String> uebersprungen) {

        public boolean neustartNoetig() {
            return !welten.isEmpty() || seedSchutz;
        }
    }

    static final char TRENNER = '\u001F';
    static final String ANTI_XRAY = "anticheat" + TRENNER + "anti-xray";
    static final String SEEDS = "feature-seeds" + TRENNER + "generate-random-seeds-for-all";

    static final List<String> OBERWELT_VERSTECKT = List.of("air", "copper_ore", "deepslate_copper_ore", "raw_copper_block",
            "diamond_ore", "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "gold_ore", "deepslate_gold_ore",
            "iron_ore", "deepslate_iron_ore", "raw_iron_block", "lapis_ore", "deepslate_lapis_ore", "redstone_ore",
            "deepslate_redstone_ore");
    static final List<String> OBERWELT_ERSETZT = List.of("chest", "amethyst_block", "andesite", "budding_amethyst", "calcite",
            "coal_ore", "deepslate_coal_ore", "deepslate", "diorite", "dirt", "emerald_ore", "deepslate_emerald_ore", "granite",
            "gravel", "oak_planks", "smooth_basalt", "stone", "tuff");
    static final List<String> NETHER_VERSTECKT = List.of("air", "ancient_debris", "bone_block", "glowstone", "magma_block",
            "nether_bricks", "nether_gold_ore", "nether_quartz_ore", "polished_blackstone_bricks");
    static final List<String> NETHER_ERSETZT = List.of("basalt", "blackstone", "gravel", "netherrack", "soul_sand", "soul_soil");
    static final List<String> OBERWELT_NUR_VERSTECKEN = List.of("copper_ore", "deepslate_copper_ore", "raw_copper_block",
            "gold_ore", "deepslate_gold_ore", "iron_ore", "deepslate_iron_ore", "raw_iron_block", "coal_ore",
            "deepslate_coal_ore", "lapis_ore", "deepslate_lapis_ore", "mossy_cobblestone", "obsidian", "chest", "diamond_ore",
            "deepslate_diamond_ore", "redstone_ore", "deepslate_redstone_ore", "clay", "emerald_ore", "deepslate_emerald_ore",
            "ender_chest");
    static final List<String> NETHER_NUR_VERSTECKEN = List.of("ancient_debris", "nether_gold_ore", "nether_quartz_ore");

    private final BetterSMP plugin;
    private final File serverOrdner;
    private final File zustand;
    private volatile boolean neustartNoetig;

    public AntiXrayEinrichtung(BetterSMP plugin) {
        this(plugin, serverOrdner(plugin), new File(plugin.getDataFolder(), "anti-xray.yml"));
    }

    public AntiXrayEinrichtung(BetterSMP plugin, File serverOrdner, File zustand) {
        this.plugin = plugin;
        this.serverOrdner = serverOrdner;
        this.zustand = zustand;
    }

    private static File serverOrdner(BetterSMP plugin) {
        File plugins = plugin.getDataFolder().getAbsoluteFile().getParentFile();
        File oben = plugins == null ? null : plugins.getParentFile();
        return oben != null ? oben : new File(".").getAbsoluteFile();
    }

    public void beimStart() {
        List<Welt> welten = new ArrayList<>();
        for (World welt : Bukkit.getWorlds()) {
            welten.add(new Welt(welt.getName(), welt.getEnvironment(), welt.getWorldFolder()));
        }
        einrichten(welten);
    }

    public Ergebnis einrichten(List<Welt> welten) {
        FileConfiguration config = plugin.getConfig();
        List<String> eingerichtet = new ArrayList<>();
        List<String> uebersprungen = new ArrayList<>();
        if (!config.getBoolean("anti-xray.paper-einrichten", true)) {
            return new Ergebnis(eingerichtet, false, uebersprungen);
        }
        int modus = Math.max(1, Math.min(3, config.getInt("anti-xray.modus", 3)));
        YamlConfiguration stand = YamlConfiguration.loadConfiguration(zustand);
        List<String> erledigt = new ArrayList<>(stand.getStringList("welten"));
        File standardDatei = new File(new File(serverOrdner, "config"), "paper-world-defaults.yml");
        YamlConfiguration standard = standardDatei.isFile() ? laden(standardDatei) : null;
        boolean globalAn = standard != null && standard.getBoolean(ANTI_XRAY + TRENNER + "enabled", false);
        for (Welt welt : welten) {
            boolean nether = welt.umgebung() == World.Environment.NETHER;
            if ((!nether && welt.umgebung() != World.Environment.NORMAL) || erledigt.contains(welt.name())) {
                continue;
            }
            if (globalAn) {
                erledigt.add(welt.name());
                uebersprungen.add(welt.name() + " (in paper-world-defaults.yml schon an)");
                continue;
            }
            File datei = new File(welt.ordner(), "paper-world.yml");
            YamlConfiguration yaml = datei.isFile() ? laden(datei) : leer();
            if (yaml == null) {
                uebersprungen.add(welt.name() + " (paper-world.yml nicht lesbar)");
                continue;
            }
            erledigt.add(welt.name());
            if (yaml.contains(ANTI_XRAY + TRENNER + "enabled")) {
                uebersprungen.add(welt.name() + " (eigene Einstellung bleibt)");
                continue;
            }
            setzen(yaml, nether, modus);
            if (speichern(yaml, datei)) {
                eingerichtet.add(welt.name());
            }
        }
        boolean seedSchutz = false;
        if (config.getBoolean("anti-xray.seed-schutz", true) && !stand.getBoolean("seed-schutz", false)) {
            if (standard == null) {
                plugin.getLogger().info("Anti-Xray: config/paper-world-defaults.yml nicht gefunden - Seed-Schutz übersprungen.");
            } else {
                stand.set("seed-schutz", true);
                if (!standard.getBoolean(SEEDS, false)) {
                    standard.set(SEEDS, true);
                    seedSchutz = speichern(standard, standardDatei);
                }
            }
        }
        stand.set("stand", 1);
        stand.set("welten", erledigt);
        try {
            stand.save(zustand);
        } catch (IOException fehler) {
            plugin.getLogger().warning("Anti-Xray: anti-xray.yml konnte nicht gespeichert werden: " + fehler.getMessage());
        }
        Ergebnis ergebnis = new Ergebnis(eingerichtet, seedSchutz, uebersprungen);
        melden(ergebnis, modus);
        return ergebnis;
    }

    private void melden(Ergebnis ergebnis, int modus) {
        if (!ergebnis.welten().isEmpty()) {
            plugin.getLogger().warning("Anti-Xray von Paper eingeschaltet für " + String.join(", ", ergebnis.welten())
                    + " (Modus " + modus + "). Wirkt nach dem nächsten Neustart des Servers.");
        }
        if (ergebnis.seedSchutz()) {
            plugin.getLogger().warning("Seed-Schutz eingeschaltet: Erze in neuen Chunks lassen sich nicht mehr aus dem Seed "
                    + "berechnen. Wirkt nach dem nächsten Neustart des Servers.");
        }
        for (String welt : ergebnis.uebersprungen()) {
            plugin.getLogger().info("Anti-Xray: " + welt);
        }
        if (ergebnis.neustartNoetig()) {
            neustartNoetig = true;
        }
    }

    static void setzen(YamlConfiguration yaml, boolean nether, int modus) {
        String basis = ANTI_XRAY + TRENNER;
        yaml.set(basis + "enabled", true);
        yaml.set(basis + "engine-mode", modus);
        if (modus == 1) {
            yaml.set(basis + "hidden-blocks", nether ? NETHER_NUR_VERSTECKEN : OBERWELT_NUR_VERSTECKEN);
        } else {
            yaml.set(basis + "hidden-blocks", nether ? NETHER_VERSTECKT : OBERWELT_VERSTECKT);
            yaml.set(basis + "replacement-blocks", nether ? NETHER_ERSETZT : OBERWELT_ERSETZT);
        }
        yaml.set(basis + "max-block-height", nether ? 128 : 64);
        yaml.set(basis + "update-radius", 2);
        yaml.set(basis + "lava-obscures", false);
        yaml.set(basis + "use-permission", false);
    }

    static YamlConfiguration leer() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator(TRENNER);
        return yaml;
    }

    private YamlConfiguration laden(File datei) {
        YamlConfiguration yaml = leer();
        try {
            yaml.load(datei);
            return yaml;
        } catch (IOException | InvalidConfigurationException fehler) {
            plugin.getLogger().warning("Anti-Xray: " + datei.getPath() + " konnte nicht gelesen werden, bleibt unverändert ("
                    + fehler.getMessage() + ")");
            return null;
        }
    }

    private boolean speichern(YamlConfiguration yaml, File datei) {
        try {
            File sicherung = new File(datei.getParentFile(), datei.getName() + ".vor-anti-xray");
            if (datei.isFile() && !sicherung.exists()) {
                Files.copy(datei.toPath(), sicherung.toPath());
            }
            yaml.save(datei);
            return true;
        } catch (IOException fehler) {
            plugin.getLogger().warning("Anti-Xray: " + datei.getPath() + " konnte nicht geschrieben werden: " + fehler.getMessage());
            return false;
        }
    }

    public boolean neustartNoetig() {
        return neustartNoetig;
    }

    @EventHandler
    public void beimJoin(PlayerJoinEvent event) {
        if (neustartNoetig && event.getPlayer().hasPermission("bettersmp.admin")) {
            plugin.msgs().send(event.getPlayer(), "xray.neustart");
        }
    }
}
