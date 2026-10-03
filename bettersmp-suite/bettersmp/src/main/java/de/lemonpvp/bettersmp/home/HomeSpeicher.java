package de.lemonpvp.bettersmp.home;

import de.lemonpvp.bettersmp.BetterSMP;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class HomeSpeicher {

    public static final int MAX_PLAETZE = 7;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_äöüÄÖÜß-]{1,16}");

    private final BetterSMP plugin;
    private final File ordner;
    private final File essentialsDaten;
    private final Map<UUID, TreeMap<Integer, Home>> cache = new HashMap<>();
    private final Map<UUID, Integer> zuMelden = new HashMap<>();
    private final ExecutorService schreiber = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BetterSMP-Homes");
        t.setDaemon(true);
        return t;
    });

    public HomeSpeicher(BetterSMP plugin) {
        this(plugin, new File(plugin.getDataFolder(), "homes"),
                new File(plugin.getDataFolder().getAbsoluteFile().getParentFile(), "Essentials/userdata"));
    }

    HomeSpeicher(BetterSMP plugin, File ordner, File essentialsDaten) {
        this.plugin = plugin;
        this.ordner = ordner;
        this.essentialsDaten = essentialsDaten;
    }

    public int plaetze() {
        return Math.max(1, Math.min(MAX_PLAETZE, plugin.getConfig().getInt("homes.anzahl", 3)));
    }

    public static boolean nameGueltig(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    public static String standardName(int platz) {
        return "Home " + platz;
    }

    public Map<Integer, Home> homes(UUID spieler) {
        return Collections.unmodifiableMap(laden(spieler));
    }

    public int belegt(UUID spieler) {
        int anzahl = 0;
        for (Integer platz : laden(spieler).keySet()) {
            if (platz <= plaetze()) {
                anzahl++;
            }
        }
        return anzahl;
    }

    public Home home(UUID spieler, int platz) {
        return platz < 1 || platz > plaetze() ? null : laden(spieler).get(platz);
    }

    public Home finden(UUID spieler, String eingabe) {
        Integer platz = platzAus(eingabe);
        if (platz != null) {
            return home(spieler, platz);
        }
        for (Home home : laden(spieler).values()) {
            if (home.platz() <= plaetze() && home.heisst(eingabe)) {
                return home;
            }
        }
        return null;
    }

    public Integer platzAus(String eingabe) {
        if (eingabe == null || eingabe.isEmpty() || eingabe.length() > 2) {
            return null;
        }
        for (char c : eingabe.toCharArray()) {
            if (!Character.isDigit(c)) {
                return null;
            }
        }
        return Integer.parseInt(eingabe);
    }

    public int freierPlatz(UUID spieler) {
        TreeMap<Integer, Home> homes = laden(spieler);
        for (int platz = 1; platz <= plaetze(); platz++) {
            if (!homes.containsKey(platz)) {
                return platz;
            }
        }
        return -1;
    }

    public void setzen(UUID spieler, Home home) {
        TreeMap<Integer, Home> homes = laden(spieler);
        homes.put(home.platz(), home);
        speichern(spieler, homes);
    }

    public Home loeschen(UUID spieler, int platz) {
        TreeMap<Integer, Home> homes = laden(spieler);
        Home weg = homes.remove(platz);
        if (weg != null) {
            speichern(spieler, homes);
        }
        return weg;
    }

    public int beimJoin(UUID spieler) {
        laden(spieler);
        Integer importiert = zuMelden.remove(spieler);
        return importiert == null ? 0 : importiert;
    }

    private TreeMap<Integer, Home> laden(UUID spieler) {
        TreeMap<Integer, Home> vorhanden = cache.get(spieler);
        if (vorhanden != null) {
            return vorhanden;
        }
        File datei = datei(spieler);
        TreeMap<Integer, Home> homes;
        if (datei.exists()) {
            homes = ausDatei(datei);
        } else {
            homes = new TreeMap<>();
            for (Home home : ausEssentials(new File(essentialsDaten, spieler + ".yml"), plaetze())) {
                homes.put(home.platz(), home);
            }
            if (!homes.isEmpty()) {
                zuMelden.put(spieler, homes.size());
            }
            speichern(spieler, homes);
        }
        cache.put(spieler, homes);
        return homes;
    }

    private File datei(UUID spieler) {
        return new File(ordner, spieler + ".yml");
    }

    static TreeMap<Integer, Home> ausDatei(File datei) {
        TreeMap<Integer, Home> homes = new TreeMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(datei);
        ConfigurationSection bereich = yaml.getConfigurationSection("homes");
        if (bereich == null) {
            return homes;
        }
        for (String schluessel : bereich.getKeys(false)) {
            ConfigurationSection h = bereich.getConfigurationSection(schluessel);
            int platz;
            try {
                platz = Integer.parseInt(schluessel);
            } catch (NumberFormatException kaputt) {
                continue;
            }
            if (h == null || platz < 1 || platz > MAX_PLAETZE) {
                continue;
            }
            homes.put(platz, new Home(platz, h.getString("name", standardName(platz)), h.getString("welt"),
                    uuid(h.getString("welt-id")), h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                    (float) h.getDouble("yaw"), (float) h.getDouble("pitch")));
        }
        return homes;
    }

    static List<Home> ausEssentials(File userdata, int plaetze) {
        List<Home> liste = new ArrayList<>();
        if (!userdata.isFile()) {
            return liste;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(userdata);
        ConfigurationSection bereich = yaml.getConfigurationSection("homes");
        if (bereich == null) {
            return liste;
        }
        for (String schluessel : bereich.getKeys(false)) {
            if (liste.size() >= plaetze) {
                break;
            }
            ConfigurationSection h = bereich.getConfigurationSection(schluessel);
            if (h == null) {
                continue;
            }
            String weltRoh = h.getString("world");
            String weltName = h.getString("world-name");
            UUID weltId = uuid(weltRoh);
            if (weltId == null && weltName == null) {
                weltName = weltRoh;
            }
            if (weltName == null && weltId != null) {
                World geladen = Bukkit.getWorld(weltId);
                weltName = geladen == null ? null : geladen.getName();
            }
            if (weltId == null && (weltName == null || weltName.isBlank())) {
                continue;
            }
            int platz = liste.size() + 1;
            String name = schluessel.equalsIgnoreCase("home") || !nameGueltig(schluessel) ? standardName(platz) : schluessel;
            liste.add(new Home(platz, name, weltName, weltId, h.getDouble("x"), h.getDouble("y"), h.getDouble("z"),
                    (float) h.getDouble("yaw"), (float) h.getDouble("pitch")));
        }
        return liste;
    }

    private static UUID uuid(String text) {
        if (text == null || text.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException keineUuid) {
            return null;
        }
    }

    static String alsText(Map<Integer, Home> homes) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("essentials-geprueft", true);
        yaml.createSection("homes");
        for (Home home : homes.values()) {
            String pfad = "homes." + home.platz();
            yaml.set(pfad + ".name", home.name());
            yaml.set(pfad + ".welt", home.welt());
            yaml.set(pfad + ".welt-id", home.weltId() == null ? null : home.weltId().toString());
            yaml.set(pfad + ".x", home.x());
            yaml.set(pfad + ".y", home.y());
            yaml.set(pfad + ".z", home.z());
            yaml.set(pfad + ".yaw", (double) home.yaw());
            yaml.set(pfad + ".pitch", (double) home.pitch());
        }
        return yaml.saveToString();
    }

    private void speichern(UUID spieler, Map<Integer, Home> homes) {
        String text = alsText(homes);
        File datei = datei(spieler);
        schreiber.execute(() -> schreiben(datei, text));
    }

    private void schreiben(File datei, String text) {
        try {
            Files.createDirectories(datei.getParentFile().toPath());
            File neu = new File(datei.getParentFile(), datei.getName() + ".neu");
            Files.writeString(neu.toPath(), text, StandardCharsets.UTF_8);
            try {
                Files.move(neu.toPath(), datei.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException nichtAtomar) {
                Files.move(neu.toPath(), datei.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException fehler) {
            plugin.getLogger().warning("Homes konnten nicht gespeichert werden (" + datei.getName() + "): " + fehler.getMessage());
        }
    }

    public void stoppen() {
        schreiber.shutdown();
        try {
            if (!schreiber.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Homes: Speichern nach 10 Sekunden nicht fertig.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
