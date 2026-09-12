package de.lemonpvp.smpcontent;

import de.lemonpvp.smpcontent.ability.Abilities;
import de.lemonpvp.smpcontent.command.ContentCommand;
import de.lemonpvp.smpcontent.content.BlockStore;
import de.lemonpvp.smpcontent.furniture.FurnitureManager;
import de.lemonpvp.smpcontent.vehicle.ParachuteManager;
import de.lemonpvp.smpcontent.vehicle.VehicleManager;
import de.lemonpvp.smpcontent.boss.BossManager;
import de.lemonpvp.smpcontent.content.ContentRegistry;
import de.lemonpvp.smpcontent.content.CustomEntry;
import de.lemonpvp.smpcontent.gui.ContentGui;
import de.lemonpvp.smpcontent.listener.AbilityListener;
import de.lemonpvp.smpcontent.listener.BlockListener;
import de.lemonpvp.smpcontent.listener.FurnitureListener;
import de.lemonpvp.smpcontent.listener.VehicleListener;
import de.lemonpvp.smpcontent.listener.BossListener;
import de.lemonpvp.smpcontent.listener.GuiListener;
import de.lemonpvp.smpcontent.pack.PackGenerator;
import de.lemonpvp.smpcontent.util.ConfigProblem;
import de.lemonpvp.smpcontent.util.Msgs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * SMPContent - eigene Blöcke und Items passend zum SMP-Texturepack.
 *
 * Blöcke werden über feste Note-Block-Zustände umgesetzt; das Plugin hält
 * diese Zustände stabil und sorgt für die richtigen Drops. Items bekommen
 * ein item_model (Java) und CustomModelData (Geyser/Bedrock).
 */
public final class SMPContent extends JavaPlugin {

    /** Zusatzdateien mit eigenen Inhalten, die beim ersten Start angelegt werden. */
    private static final String[] CONTENT_FILES = {
            "moebel.yml", "platzhalter-bloecke.yml", "platzhalter-items.yml",
    };

    private Msgs msgs;
    private ContentRegistry registry;
    private PackGenerator pack;
    private Abilities abilities;
    private ContentGui gui;
    private BlockStore blocks;
    private FurnitureManager furniture;
    private VehicleManager vehicles;
    private ParachuteManager parachutes;
    private BossManager bosses;

    private YamlConfiguration config;
    private ConfigProblem.Report configProblem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        saveExtras();
        this.msgs = new Msgs(this);
        this.abilities = new Abilities(this);
        this.blocks = new BlockStore(this);
        this.registry = new ContentRegistry(this);
        this.pack = new PackGenerator(this);
        this.gui = new ContentGui(this);
        this.furniture = new FurnitureManager(this);
        this.vehicles = new VehicleManager(this);
        this.parachutes = new ParachuteManager(this);
        this.bosses = new BossManager(this);
        pack.ensureFolders();
        vehicles.load();
        bosses.load();

        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AbilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FurnitureListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VehicleListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BossListener(this), this);
        getCommand("smpcontent").setExecutor(new ContentCommand(this));
        startHeldTask();
        vehicles.start();
        bosses.start();
        Bukkit.getScheduler().runTaskTimer(this, parachutes::tick, 1L, 1L);
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                blocks.load(chunk);
                vehicles.adoptChunk(chunk);
            }
        }

        getLogger().info("SMPContent aktiviert.");
    }

    /**
     * Aufräumen beim Herunterfahren.
     *
     * Fallschirme und Sitze sind unsichtbare Objekte, an denen ein Spieler
     * gerade hängt. Ohne diesen Schritt bliebe er nach einem Neuladen des
     * Plugins darauf kleben - es gäbe dann niemanden mehr, der ihn löst.
     * Fahrzeuge bleiben absichtlich stehen, die sollen den Neustart
     * überdauern.
     */
    @Override
    public void onDisable() {
        if (parachutes != null) {
            parachutes.clear();
        }
        if (bosses != null) {
            bosses.clear();
        }
        int sitze = furniture != null ? furniture.clearSeats() : 0;
        if (sitze > 0) {
            getLogger().info(sitze + " Sitz(e) aufgelöst.");
        }
        getLogger().info("SMPContent deaktiviert.");
    }

    /**
     * Der Auslöser "held" wirkt, solange man das Item in der Hand hat.
     * Der Zeitgeber läuft nur, wenn es überhaupt so eine Fähigkeit gibt.
     */
    private void startHeldTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!abilities.hasTrigger("held")) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                String id = registry.idOf(player.getInventory().getItemInMainHand());
                if (id == null) {
                    continue;
                }
                CustomEntry entry = registry.get(id);
                if (entry != null && !entry.block()) {
                    abilities.run(player, entry, "held", null);
                }
            }
        }, 20L, 20L);
    }

    /** Legt content/ und animationen.yml beim ersten Start an. */
    private void saveExtras() {
        try {
            Files.createDirectories(contentDir());
        } catch (IOException ex) {
            getLogger().warning("content/ nicht anlegbar: " + ex.getMessage());
            return;
        }
        for (String name : CONTENT_FILES) {
            copyIfAbsent("content/" + name, contentDir().resolve(name));
        }
        copyIfAbsent("animationen.yml", getDataFolder().toPath().resolve("animationen.yml"));
        copyIfAbsent("FAEHIGKEITEN.txt", getDataFolder().toPath().resolve("FAEHIGKEITEN.txt"));
        saveAssets();
    }

    /**
     * Legt die mitgelieferten Texturen und 3D-Modelle in den Datenordner.
     *
     * Der Knackpunkt ist das Update. Früher wurde nur angelegt, was noch
     * nicht da war - eine verbesserte Textur aus einer neuen Jar kam damit
     * nie beim Server an, weil die alte Datei ja schon dort lag. Wer nichts
     * davon wusste, hat sein Pack neu gebaut und trotzdem die alten Bilder
     * bekommen.
     *
     * Jetzt merkt sich das Plugin in {@code .mitgeliefert.sha}, welchen
     * Inhalt es zuletzt selbst hingelegt hat:
     *
     *   Datei fehlt                  -> anlegen
     *   Datei = zuletzt Mitgeliefertes -> unangetastet, wird ersetzt
     *   Datei ist anders             -> von Hand geändert, bleibt liegen
     *
     * Damit werden Verbesserungen sauber nachgezogen und eigene Dateien
     * trotzdem nie überschrieben.
     */
    private void saveAssets() {
        Path merker = getDataFolder().toPath().resolve(".mitgeliefert.sha");
        java.util.Properties bekannt = new java.util.Properties();
        if (Files.exists(merker)) {
            try (var in = Files.newInputStream(merker)) {
                bekannt.load(in);
            } catch (IOException ex) {
                getLogger().warning(".mitgeliefert.sha nicht lesbar: " + ex.getMessage());
            }
        }

        int neu = 0, erneuert = 0, eigene = 0;
        java.util.Properties jetzt = new java.util.Properties();
        for (String art : new String[]{"textures/block", "textures/item",
                                       "models/block", "models/item"}) {
            for (String datei : listResources("assets/" + art)) {
                String quelle = "assets/" + art + "/" + datei;
                byte[] inhalt = readResource(quelle);
                if (inhalt == null) {
                    continue;
                }
                String schluessel = art + "/" + datei;
                String pruefsumme = sha256(inhalt);
                jetzt.setProperty(schluessel, pruefsumme);

                Path ziel = getDataFolder().toPath().resolve(art).resolve(datei);
                try {
                    if (!Files.exists(ziel)) {
                        Files.createDirectories(ziel.getParent());
                        Files.write(ziel, inhalt);
                        neu++;
                    } else if (pruefsumme.equals(sha256(Files.readAllBytes(ziel)))) {
                        continue;   // schon genau das, nichts zu tun
                    } else if (bekannt.getProperty(schluessel, "")
                            .equals(sha256(Files.readAllBytes(ziel)))) {
                        Files.write(ziel, inhalt);   // unsere alte Fassung
                        erneuert++;
                    } else {
                        eigene++;   // von Hand geändert - Finger weg
                    }
                } catch (IOException ex) {
                    getLogger().warning(schluessel + " nicht schreibbar: " + ex.getMessage());
                }
            }
        }
        try {
            Files.createDirectories(merker.getParent());
            try (var out = Files.newOutputStream(merker)) {
                jetzt.store(out, "Pruefsummen der mitgelieferten Dateien - nicht anfassen");
            }
        } catch (IOException ex) {
            getLogger().warning(".mitgeliefert.sha nicht schreibbar: " + ex.getMessage());
        }

        if (neu > 0) {
            getLogger().info(neu + " mitgelieferte Texturen und Modelle angelegt.");
        }
        if (erneuert > 0) {
            getLogger().info(erneuert + " mitgelieferte Texturen und Modelle auf den "
                    + "neuen Stand gebracht. Baue dein Pack neu: /smpcontent pack");
        }
        if (eigene > 0) {
            getLogger().info(eigene + " Dateien hast du selbst geändert - die bleiben, "
                    + "wie sie sind.");
        }
    }

    /** Eine Datei aus der Jar als Bytes. */
    private byte[] readResource(String resource) {
        try (InputStream in = getResource(resource)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException ex) {
            getLogger().warning(resource + " nicht lesbar: " + ex.getMessage());
            return null;
        }
    }

    private static String sha256(byte[] daten) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(daten);
            StringBuilder text = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                text.append(Character.forDigit((b >> 4) & 0xF, 16))
                    .append(Character.forDigit(b & 0xF, 16));
            }
            return text.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 fehlt", ex);
        }
    }

    /** Was liegt im Jar unter diesem Ordner? */
    private List<String> listResources(String ordner) {
        List<String> namen = new ArrayList<>();
        try (var jar = new java.util.jar.JarFile(getFile())) {
            var eintraege = jar.entries();
            while (eintraege.hasMoreElements()) {
                String name = eintraege.nextElement().getName();
                if (name.startsWith(ordner + "/") && !name.endsWith("/")) {
                    namen.add(name.substring(ordner.length() + 1));
                }
            }
        } catch (IOException ex) {
            getLogger().warning("Mitgelieferte Dateien nicht lesbar: " + ex.getMessage());
        }
        return namen;
    }

    private void copyIfAbsent(String resource, Path target) {
        if (Files.exists(target)) {
            return;
        }
        try (InputStream in = getResource(resource)) {
            if (in == null) {
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            getLogger().info("Angelegt: " + getDataFolder().toPath().relativize(target));
        } catch (IOException ex) {
            getLogger().warning(resource + " nicht anlegbar: " + ex.getMessage());
        }
    }

    /** plugins/SMPContent/content/ - hier liegen die Zusatzdateien. */
    public Path contentDir() {
        return getDataFolder().toPath().resolve("content");
    }

    public Abilities abilities() {
        return abilities;
    }

    public ContentGui gui() {
        return gui;
    }

    /** Wo welcher eigene Block steht - unabhängig vom Notenblock-Zustand. */
    public BlockStore blocks() {
        return blocks;
    }

    /** Möbel als Anzeige-Objekte: unbegrenzt viele, drehbar, zum Draufsetzen. */
    public FurnitureManager furniture() {
        return furniture;
    }

    /** Autos, Jets und Züge. */
    public VehicleManager vehicles() {
        return vehicles;
    }

    public BossManager bosses() {
        return bosses;
    }

    /** Fallschirme - aus dem Schleudersitz oder von Hand. */
    public ParachuteManager parachutes() {
        return parachutes;
    }

    // ------------------------------------------------------------------
    //  Konfiguration
    //
    //  Bukkit wirft bei einem Tippfehler in der config.yml einfach alles
    //  weg und arbeitet mit einer leeren Datei weiter - dann sind plötzlich
    //  alle eigenen Blöcke und Items verschwunden. Hier bleibt stattdessen
    //  der letzte funktionierende Stand aktiv, und in der Konsole steht,
    //  in welcher Zeile der Fehler steckt.
    // ------------------------------------------------------------------

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    public void reloadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }

        ConfigProblem.Result result = ConfigProblem.load(file);
        if (result.ok()) {
            YamlConfiguration fresh = result.config();
            fresh.setDefaults(bundledConfig());
            config = fresh;
            configProblem = null;
            return;
        }

        configProblem = result.problem();
        ConfigProblem.log(getLogger(), configProblem);
        if (config == null) {
            // Erster Start und die Datei ist schon kaputt: wenigstens mit den
            // mitgelieferten Standardinhalten weiterarbeiten.
            config = bundledConfig();
            getLogger().warning("Es werden vorerst die mitgelieferten Standardinhalte benutzt.");
        }
    }

    private YamlConfiguration bundledConfig() {
        try (InputStream in = getResource("config.yml")) {
            if (in == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return new YamlConfiguration();
        }
    }

    /** Null, solange die config.yml in Ordnung ist. */
    public ConfigProblem.Report configProblem() {
        return configProblem;
    }

    public Msgs msgs() {
        return msgs;
    }

    public ContentRegistry registry() {
        return registry;
    }

    public PackGenerator pack() {
        return pack;
    }
}
