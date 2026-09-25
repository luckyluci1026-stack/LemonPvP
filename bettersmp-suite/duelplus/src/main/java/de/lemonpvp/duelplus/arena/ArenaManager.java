package de.lemonpvp.duelplus.arena;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Erzeugt und verwaltet die Arenen - nur auf dem Duels-Server aktiv.
 *
 * Jede Arena ist eine eigene, flache Welt OHNE echte Vanilla-
 * Terraingenerierung. Unter der begehbaren Steinplattform folgt
 * luecken-los echtes, grabbares Terrain bis zum Bedrock hinunter -
 * Erde, dann Stein, dann Tiefenschiefer (siehe terrainFuellen) - statt
 * wie frueher nur 1 Block Luft ueber dem Bedrock. Reisst eine Explosion
 * (Endkristall/Anker) den Boden weg, faellt man dadurch jetzt in
 * echtes Gestein statt direkt auf/durch den Bedrock - erst weit
 * UNTERHALB des (unzerstoerbaren) Bedrocks zaehlt das ueberhaupt noch
 * als automatische Niederlage, siehe ArenaGuardListener und
 * arenen.todeslinie-y. Seitlich steckt die ganze Arena in einer
 * unsichtbaren, unzerstoerbaren Barriere-Box, die bis auf Bedrock-
 * Niveau hinunterreicht (siehe barriereBauen) - sonst koennte man sich
 * durch das jetzt echte Terrain seitlich aus der Arena heraus graben.
 * Kein sichtbarer, begehbarer Rand, auf dem man stehen oder von dem
 * man abrutschen koennte. Die schrumpfende Worldborder (siehe
 * DuellSessionManager) bleibt der Druckmechanismus, der beide
 * zueinander zwingt - inklusive ihres normalen Vanilla-Schadens
 * ausserhalb.
 *
 * Der Boden ist ein aufwendiges Muster: zwei konzentrische Ringe plus
 * eine feine Schachbrett-Textur dazwischen, ueberlagert von acht
 * Speichen durch die Mitte (zu Spawnpunkten, Deckungspfeilern, Eck-
 * Tuermen) - alles aus gewoehnlichen, robusten Bloecken (keine Gefahren-
 * Materialien wie Lava/Wasser/Eis). Zwei Materialpaletten (Stein,
 * Tiefenschiefer), reihum nach Arena-Nummer verteilt.
 *
 * Block-Aenderungen sind waehrend eines Duells erlaubt (siehe
 * RollbackTracker), deshalb muss hier nichts nach jedem Kampf neu
 * gebaut werden - nur einmal beim allerersten Start.
 */
public final class ArenaManager {

    /**
     * Flache Welt OHNE echte Vanilla-Terraingenerierung - nur eine
     * einzelne Bedrock-Schicht als Fundament. Alles andere (Plattform UND
     * das Terrain darunter) malt ladeOderErzeuge direkt DARAUF, innerhalb
     * des Arena-Radius - ausserhalb davon (durch die Barriere-Box ohnehin
     * nicht erreichbar) bleibt es bei nacktem Bedrock.
     *
     * WICHTIG: Bedrock liegt beim Flachwelt-Generator IMMER fest am
     * Minimum der Welt (Y=BEDROCK_Y) und laesst sich nicht verschieben -
     * die Plattform-Hoehe ergibt sich deshalb rechnerisch aus BEDROCK_Y
     * plus den drei Terrain-Tiefen (siehe terrainHoeheBerechnen), nicht
     * mehr aus einem eigenen config-Wert. Wirkt nur beim ALLERERSTEN
     * Erzeugen einer Arena-Welt: bereits vorhandene Weltordner (z.B.
     * aus einer aelteren DuelPlus-Version mit anderem Aufbau) muessen
     * einmalig manuell geloescht werden, damit sie mit diesem Preset
     * neu entstehen - siehe README.
     */
    private static final String VOID_GENERATOR_SETTINGS =
            "{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1}],\"biome\":\"minecraft:the_void\"}";

    /** Fixe Y-Koordinate der einzelnen Bedrock-Schicht beim Flachwelt-Generator (siehe VOID_GENERATOR_SETTINGS) - Welt-Minimum, nicht verschiebbar. */
    private static final int BEDROCK_Y = -64;

    /**
     * Zwei Materialpaletten (Boden, Akzent, Mauer/Struktur, Licht,
     * Ring-Akzent) - bewusst nur "echte Stein"-Familien (Stein,
     * Tiefenschiefer), damit die Plattform optisch zu dem Gestein passt,
     * das direkt darunter beginnt (siehe terrainFuellen). Reihum verteilt
     * nach Arena-Nummer. Bewusst nur gewoehnliche, robuste Bloecke - keine
     * Gefahren-Materialien.
     *
     * Ring-Akzent (5. Spalte) ist bewusst ein ganz normaler VOLLBLOCK, NIE
     * eine _WALL-Sorte: _WALL-Bloecke haben eine eigene, erhoehte Hitbox
     * (ein Pfosten in der Mitte, der sich zu Nachbar-Waenden verbindet) -
     * als einzelner Bodenblock verlegt wuerde ueberall dort, wo Ring/
     * Speichen-Linien aneinanderstossen, eine echte kleine Mauer aus dem
     * Boden ragen ("Stone Fences" mitten in der Arena). Fuer die Ringe/
     * Speichen im Boden zaehlt nur die Optik, keine Mauer-Kollision - dafuer
     * hier der gemeißelte Vollblock-Vetter der jeweiligen _WALL-Sorte.
     * mauer (3. Spalte) bleibt _WALL und wird NUR fuer echte aufragende
     * Struktur (Tuerme/Zinnen/Deckungspfeiler in strukturenBauen) benutzt,
     * dort ist die erhoehte Mauer-Form ausdruecklich gewollt.
     */
    private static final Material[][] PALETTEN = {
            {Material.SMOOTH_STONE, Material.POLISHED_ANDESITE, Material.STONE_BRICK_WALL, Material.LANTERN, Material.CHISELED_STONE_BRICKS},
            {Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE_WALL, Material.SOUL_LANTERN, Material.CHISELED_DEEPSLATE},
    };

    private final DuelPlus plugin;
    private final Map<String, Arena> arenen = new LinkedHashMap<>();
    private final Map<String, String> belegtVon = new LinkedHashMap<>();
    private final Deque<String> warteschlange = new ArrayDeque<>();

    /**
     * Haelt fest, mit welchem Radius/welcher Hoehe eine Arena TATSAECHLICH
     * gebaut wurde (siehe ladeOderErzeuge) - unabhaengig davon, ob
     * arenen.worldborder-groesse/boden-tiefe/stein-tiefe/deepslate-tiefe
     * in der config.yml sich DANACH nochmal geaendert haben, ohne dass
     * der Weltordner geloescht wurde. Ohne das wuerden bei einer bereits
     * vorhandenen Welt Radius/Hoehe live (und potenziell falsch) aus der
     * aktuellen config.yml berechnet - mit genau den Folgen, die Arena's
     * Klassen-Kommentar fuer plattformHoehe beschreibt (falsche Spawn-
     * Hoehe, falsche Worldborder-Mitte), zusaetzlich beim Radius auch
     * eine falsch grosse Worldborder UND eine Barriere-Box, die nicht bis
     * zum (neuen) Rand reicht.
     */
    private final File metaDatei;
    private final YamlConfiguration meta;

    public ArenaManager(DuelPlus plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.metaDatei = new File(plugin.getDataFolder(), "arena-meta.yml");
        this.meta = YamlConfiguration.loadConfiguration(metaDatei);
    }

    public void arenenVorbereiten() {
        int anzahl = Math.max(1, plugin.getConfig().getInt("arenen.anzahl", 4));
        String praefix = plugin.getConfig().getString("arenen.welt-praefix", "duell_arena_");
        for (int i = 1; i <= anzahl; i++) {
            String name = praefix + i;
            Arena arena = ladeOderErzeuge(name, i);
            arenen.put(name, arena);
        }
        plugin.getLogger().info("DuelPlus: " + arenen.size() + " Arena(s) bereit.");
    }

    private Arena ladeOderErzeuge(String name, int index) {
        World world = Bukkit.getWorld(name);
        boolean neu = world == null;
        if (world == null) {
            world = new WorldCreator(name)
                    .type(WorldType.FLAT)
                    .generatorSettings(VOID_GENERATOR_SETTINGS)
                    .environment(World.Environment.NORMAL)
                    .createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("Arena-Welt " + name + " konnte nicht erzeugt werden");
        }
        konfiguriereWelt(world);

        int radius;
        int hoehe;
        int paletteIndex = (index - 1) % PALETTEN.length;
        if (neu) {
            // Nur beim ALLERERSTEN Bauen aus der aktuellen config.yml lesen -
            // das sind die Werte, die JETZT tatsaechlich verbaut werden.
            radius = Math.max(10, plugin.getConfig().getInt("arenen.worldborder-groesse", 115));
            int bodenTiefe = Math.max(0, plugin.getConfig().getInt("arenen.boden-tiefe", 7));
            int steinTiefe = Math.max(0, plugin.getConfig().getInt("arenen.stein-tiefe", 35));
            int deepslateTiefe = Math.max(0, plugin.getConfig().getInt("arenen.deepslate-tiefe", 15));
            hoehe = terrainHoeheBerechnen(bodenTiefe, steinTiefe, deepslateTiefe);
            Material[] palette = PALETTEN[paletteIndex];
            chunksLaden(world, radius);
            plattformBauen(world, radius, hoehe, palette);
            barriereBauen(world, radius, hoehe);
            terrainFuellen(world, radius, hoehe, bodenTiefe, steinTiefe, deepslateTiefe);
            metaSchreiben(name, radius, hoehe);
            plugin.getLogger().info("DuelPlus: Arena '" + name + "' NEU gebaut (Palette "
                    + (paletteIndex + 1) + "/" + PALETTEN.length + ", Radius " + radius
                    + ", Plattform-Hoehe " + hoehe + ", Terrain darunter " + bodenTiefe + "/" + steinTiefe
                    + "/" + deepslateTiefe + " Erde/Stein/Tiefenschiefer - fuellt sich ueber die naechsten "
                    + "Sekunden asynchron auf).");
        } else {
            // Bereits vorhandene Weltdatei - Radius/Hoehe kommen bewusst aus
            // arena-meta.yml (dort, wo sie beim tatsaechlichen Bauen
            // hinterlegt wurden), NICHT live aus der aktuellen config.yml:
            // die behaelt ihren alten Aufbau, ganz gleich, was inzwischen in
            // der config.yml steht. Nur falls diese Arena noch NIE ueber
            // diesen Mechanismus gebaut wurde (Update von einer sehr alten
            // DuelPlus-Version ohne arena-meta.yml), bleibt als bestmoeglicher
            // Ersatz die aktuelle config.yml.
            int radiusFallback = Math.max(10, plugin.getConfig().getInt("arenen.worldborder-groesse", 115));
            int hoeheFallback = terrainHoeheBerechnen(
                    Math.max(0, plugin.getConfig().getInt("arenen.boden-tiefe", 7)),
                    Math.max(0, plugin.getConfig().getInt("arenen.stein-tiefe", 35)),
                    Math.max(0, plugin.getConfig().getInt("arenen.deepslate-tiefe", 15)));
            radius = meta.getInt(name + ".radius", radiusFallback);
            hoehe = meta.getInt(name + ".plattform-hoehe", hoeheFallback);
            chunksLaden(world, radius);
            // Wer eine strukturelle Aenderung erwartet, aber diese Zeile hier
            // im Log sieht statt "NEU gebaut" oben, hat die Weltordner nicht
            // wirklich geloescht (oder der Server wurde seitdem nicht neu
            // gestartet - ein reines /duelplus reload baut nichts neu).
            plugin.getLogger().info("DuelPlus: Arena '" + name + "' aus vorhandener Welt geladen - "
                    + "KEINE Struktur-Aenderung uebernommen (Weltordner dafuer loeschen + Server neu starten). "
                    + "Tatsaechlicher Radius " + radius + ", Plattform-Hoehe " + hoehe + ".");
        }
        int abstand = Math.max(4, plugin.getConfig().getInt("arenen.spawn-abstand", 20));

        Location mitte = new Location(world, 0.5, hoehe + 1, 0.5);
        world.setSpawnLocation(mitte.getBlockX(), mitte.getBlockY(), mitte.getBlockZ());
        world.getWorldBorder().setCenter(mitte);
        double vollGroesse = radius * 2.0;
        world.getWorldBorder().setSize(vollGroesse);

        Location spawnA = mitte.clone().add(-abstand / 2.0, 0, 0);
        Location spawnB = mitte.clone().add(abstand / 2.0, 0, 0);
        // A steht westlich (-X), B oestlich (+X) der Mitte - beide sollen
        // sich ANSEHEN. Minecraft-Yaw: 0=Sued, 90=West, -90=Ost.
        spawnA.setYaw(-90f);
        spawnB.setYaw(90f);
        return new Arena(name, world, spawnA, spawnB, vollGroesse, hoehe);
    }

    /**
     * Plattform-Hoehe RECHNERISCH aus dem Terrain-Aufbau darunter, nicht
     * mehr als eigener config-Wert: BEDROCK_Y ist fest (siehe Feld-
     * Kommentar), direkt darueber liegt luecken-los Tiefenschiefer, dann
     * Stein, dann Erde, dann - genau 1 Block darueber - die begehbare
     * Plattform. So kann die Plattform nie versehentlich einen Spalt zum
     * Terrain darunter haben, ganz gleich, welche Tiefen in der config.yml
     * stehen.
     */
    private int terrainHoeheBerechnen(int bodenTiefe, int steinTiefe, int deepslateTiefe) {
        return BEDROCK_Y + 1 + deepslateTiefe + steinTiefe + bodenTiefe;
    }

    /** Haelt fest, mit welchem Radius/welcher Hoehe arenaName TATSAECHLICH gebaut wurde - siehe Kommentar beim meta-Feld. */
    private void metaSchreiben(String arenaName, int radius, int hoehe) {
        meta.set(arenaName + ".radius", radius);
        meta.set(arenaName + ".plattform-hoehe", hoehe);
        try {
            meta.save(metaDatei);
        } catch (IOException e) {
            plugin.getLogger().warning("DuelPlus: Arena-Metadaten fuer '" + arenaName + "' konnten nicht "
                    + "gespeichert werden (" + e.getMessage() + ") - bei einem spaeteren Server-Neustart wuerde "
                    + "dann als Ersatz auf die dann aktuelle config.yml zurueckgefallen.");
        }
    }

    /**
     * Haelt die Chunks der kompletten Plattform dauerhaft geladen (nicht
     * erst, wenn zufaellig ein Spieler in der Naehe ist) - sonst kann
     * direkt nach der Ankunft ein kurzes Nachladen wie ein Ruckler/
     * "Zurueckgebuggtwerden" wirken.
     */
    private void chunksLaden(World world, int radius) {
        int min = (-radius) >> 4;
        int max = radius >> 4;
        for (int cx = min; cx <= max; cx++) {
            for (int cz = min; cz <= max; cz++) {
                world.addPluginChunkTicket(cx, cz, plugin);
            }
        }
    }

    private void konfiguriereWelt(World world) {
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        // Sicherheitsnetz: sollte die Sturz-Schaden-Abfangung (siehe
        // ArenaGuardListener) durch einen unvorhergesehenen Todesfall
        // doch einmal durchrutschen, geht wenigstens kein Inventar durch
        // einen ganz normalen Vanilla-Tod verloren.
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        // Kein "XY hat den Fortschritt ... erreicht" mitten im Duell.
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setTime(6000);
        world.setStorm(false);
        // BEWUSST NICHT Difficulty.PEACEFUL, sondern EXPLIZIT NORMAL: das
        // erledigt DO_MOB_SPAWNING oben schon vollstaendig (keine Mobs in
        // einer frischen Void-Welt sowieso). Peaceful hat bei manchen
        // Bedrock-/Geyser-Clients dazu gefuehrt, dass eigene Treffer
        // serverseitig komplett ins Leere gingen (kein Ton, kein
        // Knockback, kein Schaden) - Java-PvP ist von der Difficulty
        // unabhaengig, Bedrock-seitig offenbar nicht immer. Ohne dieses
        // explizite setDifficulty erbt eine frisch erzeugte Welt die
        // Server-weite Standard-Difficulty aus server.properties - stand
        // die (aus welchem Grund auch immer) auf Peaceful, heilte dessen
        // extrem schnelle natuerliche Regeneration jeden Treffer quasi
        // sofort wieder weg: wirkte nach aussen wie "gar kein Schaden".
        world.setDifficulty(Difficulty.NORMAL);
        // Ebenfalls explizit statt dem Server-Standard ueberlassen - ohne
        // das koennte PvP zwischen den beiden Duellanten je nach globaler
        // server.properties (pvp=false) komplett wirkungslos bleiben.
        world.setPVP(true);
    }

    // ------------------------------------------------------------ Boden

    /**
     * Zwei konzentrische Ringe (bei 35% und 75% des Radius) plus eine
     * feine Schachbrett-Textur dazwischen, ueberlagert von acht Speichen
     * durch die Mitte - waagerecht/senkrecht zu den Spawnpunkten bzw.
     * den Deckungspfeilern, diagonal zu den vier Eck-Tuermen. Bewusst OHNE
     * sichtbare Randmauer: der aeussere Abschluss ist ausschliesslich die
     * unsichtbare Barriere-Box (siehe barriereBauen) - keine begehbare
     * Kante mehr, auf der man stehen oder von der man unerwartet
     * abrutschen/durchfallen koennte. Was UNTER dieser einen Blockschicht
     * liegt, baut nicht diese Methode, sondern terrainFuellen.
     */
    private void plattformBauen(World world, int radius, int y, Material[] palette) {
        Material boden = palette[0];
        Material akzent = palette[1];
        Material mauer = palette[2];
        Material licht = palette[3];
        Material ringAkzent = palette[4];
        double ringInnen = radius * 0.35;
        double ringAussen = radius * 0.75;
        double ringBreite = 3.0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double rundeEntfernung = Math.sqrt((double) x * x + (double) z * z);
                // Speichen: verbinden ueber die Mitte die Spawnpunkte (Z=0),
                // die Deckungspfeiler (X=0) und diagonal alle vier Eck-
                // tuerme (|X|=|Z|) - das Muster bekommt dadurch einen echten
                // Bezug zum Rest der Arena statt willkuerlich zu wirken.
                boolean speiche = (x == 0 || z == 0 || Math.abs(x) == Math.abs(z)) && rundeEntfernung > 3;
                boolean ring1 = Math.abs(rundeEntfernung - ringInnen) <= ringBreite / 2.0;
                boolean ring2 = Math.abs(rundeEntfernung - ringAussen) <= ringBreite / 2.0;
                Material bodenBlock;
                if (rundeEntfernung <= 3) {
                    bodenBlock = akzent;
                } else if (ring1 || ring2 || speiche) {
                    bodenBlock = ringAkzent;
                } else {
                    // Feine 2x2-Schachbrett-Textur zwischen den Ringen/
                    // Speichen - floorDiv statt normaler Ganzzahl-Division,
                    // damit das Muster auch bei negativen Koordinaten sauber
                    // durchlaeuft (Javas "/" rundet bei negativen Zahlen
                    // Richtung 0, nicht immer nach unten).
                    boolean schachbrett = (Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) % 2 == 0;
                    bodenBlock = schachbrett ? boden : akzent;
                }
                platzieren(world, x, y, z, bodenBlock);
            }
        }
        strukturenBauen(world, radius, y, mauer, licht);
    }

    /**
     * Bodenblock setzen und die 5 Bloecke darueber freiraeumen - genug
     * Kopf-/Sprung-Freiraum ueber der gesamten begehbaren Flaeche.
     */
    private void platzieren(World world, int x, int y, int z, Material bodenBlock) {
        world.getBlockAt(x, y, z).setType(bodenBlock, false);
        for (int dy = 1; dy <= 5; dy++) {
            world.getBlockAt(x, y + dy, z).setType(Material.AIR, false);
        }
    }

    /**
     * Vier Eck-TUERME (8 Bloecke hoch, mit Zinnenkranz und Licht auf der
     * Spitze) und zwei Deckungspfeiler auf der Z-Achse (X=0) - taktische
     * Tiefe gegen die sonst komplett leere Mitte. Deckung bewusst NUR auf
     * der Achse SENKRECHT zur Spawn-Linie (die liegt auf X, siehe
     * spawnA/spawnB in ladeOderErzeuge): jeder Punkt mit X=0 ist per
     * Pythagoras IMMER exakt gleich weit von beiden Startpunkten entfernt,
     * egal welches Z - keiner der beiden wird dadurch bevorteilt.
     */
    private void strukturenBauen(World world, int radius, int y, Material mauer, Material licht) {
        int[][] ecken = {{-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}};
        int turmHoehe = 8;
        for (int[] ecke : ecken) {
            int ex = ecke[0];
            int ez = ecke[1];
            for (int ty = 1; ty <= turmHoehe; ty++) {
                world.getBlockAt(ex, y + ty, ez).setType(mauer, false);
            }
            // Zinnenkranz NUR nach INNEN versetzt (nie nach aussen) - sonst
            // wuerden zwei der vier Bloecke exakt auf dem Ring landen, auf
            // dem gleich danach barriereBauen die unsichtbare Barriere-Box
            // baut, und dort wieder spurlos verschwinden.
            int kranzY = y + turmHoehe - 1;
            int dx = ex < 0 ? 1 : -1;
            int dz = ez < 0 ? 1 : -1;
            world.getBlockAt(ex + dx, kranzY, ez).setType(mauer, false);
            world.getBlockAt(ex, kranzY, ez + dz).setType(mauer, false);
            world.getBlockAt(ex, y + turmHoehe + 1, ez).setType(licht, false);
        }
        int deckungsAbstand = Math.min(radius - 8, 16);
        if (deckungsAbstand > 0) {
            for (int vorzeichen : new int[]{-1, 1}) {
                int dz = vorzeichen * deckungsAbstand;
                world.getBlockAt(0, y + 1, dz).setType(mauer, false);
                world.getBlockAt(0, y + 2, dz).setType(mauer, false);
            }
        }
    }

    /**
     * Fuellt das echte Terrain UNTER der Plattform auf: Erde, dann Stein,
     * dann Tiefenschiefer, luecken-los bis direkt auf den Bedrock (siehe
     * terrainHoeheBerechnen - die Plattform-Hoehe ist so gewaehlt, dass
     * exakt das aufgeht). Bei voller Arena-Groesse waeren das schnell
     * mehrere Millionen Bloecke auf einen Schlag - statt das synchron in
     * EINEM Aufruf zu erledigen (Absturzrisiko: der Server-Watchdog killt
     * den Prozess, wenn der Haupt-Thread zu lange nicht reagiert), baut
     * dieser Task pro Tick genau EINE horizontale Schicht (radius- und
     * nicht tiefenabhaengig - ungefaehr so viele Bloecke wie eine einzelne
     * Plattform-Schicht, das laeuft nachweislich schon synchron
     * problemlos). Faellt ueber ein paar Sekunden nach dem Bauen komplett
     * auf - bis dahin ist die Arena oben schon voll begehbar, es fehlt nur
     * das kosmetische Gestein tief darunter, das ohnehin niemand in den
     * ersten Sekunden nach einem Server-Neustart erreicht.
     */
    private void terrainFuellen(World world, int radius, int plattformY, int bodenTiefe, int steinTiefe, int deepslateTiefe) {
        List<Material> schichten = new ArrayList<>();
        for (int i = 0; i < bodenTiefe; i++) {
            schichten.add(Material.DIRT);
        }
        for (int i = 0; i < steinTiefe; i++) {
            schichten.add(Material.STONE);
        }
        for (int i = 0; i < deepslateTiefe; i++) {
            schichten.add(Material.DEEPSLATE);
        }
        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= schichten.size()) {
                    plugin.getLogger().info("DuelPlus: Terrain unter Arena '" + world.getName() + "' fertig aufgefuellt.");
                    cancel();
                    return;
                }
                int y = plattformY - 1 - index;
                Material material = schichten.get(index);
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        world.getBlockAt(x, y, z).setType(material, false);
                    }
                }
                index++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Wie hoch die unsichtbare Barriere-Box ueber der Plattform reicht - komfortable Reserve gegen jeden Enderperlen-Bogen. */
    private static final int BARRIERE_HOEHE = 30;

    /**
     * Unsichtbare, unzerstoerbare Box (vier Waende + Decke) in der VOLLEN
     * Start-Groesse der Arena, OHNE sichtbare Mauer mehr davor (siehe
     * Klassen-Kommentar) - macht ein Entkommen (z.B. per Enderperle ueber
     * die schrumpfende Worldborder, siehe ladeOderErzeuge) unabhaengig
     * vom aktuellen Border-Stand unmoeglich. Reicht bis EXAKT auf
     * Bedrock-Niveau hinunter (kein Spalt): seit es unter der Plattform
     * echtes, grabbares Terrain gibt (siehe terrainFuellen), MUSS die
     * Barriere bis dorthin reichen - sonst koennte man sich seitlich durch
     * Erde/Stein/Tiefenschiefer aus der Arena heraus graben, weit unterhalb
     * der eigentlichen Plattform. KEIN Boden in der Box selbst - das
     * Bedrock der Welt ist bereits der Boden.
     */
    private void barriereBauen(World world, int radius, int y) {
        int aussen = radius + 1;
        int unten = BEDROCK_Y + 1;
        int oben = y + BARRIERE_HOEHE;
        for (int x = -aussen; x <= aussen; x++) {
            for (int z = -aussen; z <= aussen; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == aussen) {
                    for (int by = unten; by <= oben; by++) {
                        world.getBlockAt(x, by, z).setType(Material.BARRIER, false);
                    }
                } else {
                    world.getBlockAt(x, oben, z).setType(Material.BARRIER, false);
                }
            }
        }
    }

    // ------------------------------------------------------------ Belegung

    public synchronized Optional<Arena> zuweisen(String duellId) {
        for (Arena arena : arenen.values()) {
            if (!belegtVon.containsKey(arena.name())) {
                belegtVon.put(arena.name(), duellId);
                return Optional.of(arena);
            }
        }
        if (!warteschlange.contains(duellId)) {
            warteschlange.add(duellId);
        }
        return Optional.empty();
    }

    /**
     * Laeuft in dieser Arena GERADE ein Duell? Arenen selbst existieren
     * dauerhaft (einmal gebaut, nie wieder entfernt) - arena(name) liefert
     * also so gut wie immer ein Ergebnis, ganz unabhaengig davon, ob dort
     * gerade wirklich gekaempft wird. Fuer Faelle, die das wirklich
     * wissen muessen (z.B. ZuschauerManager: ist das beobachtete Duell
     * inzwischen zu Ende?), zaehlt NUR das hier.
     */
    public synchronized boolean istBelegt(String arenaName) {
        return belegtVon.containsKey(arenaName);
    }

    public synchronized void freigeben(String arenaName) {
        belegtVon.remove(arenaName);
        Arena arena = arenen.get(arenaName);
        if (arena != null) {
            // Sofort (0 Sekunden Uebergang) wieder die volle Groesse - sonst
            // wuerde das naechste Duell in dieser Arena mit einer bereits
            // geschrumpften Worldborder starten (siehe DuellSessionManager).
            arena.world().getWorldBorder().setSize(arena.vollGroesse());
            aufraeumen(arena.world());
        }
    }

    /**
     * Liegengebliebene Items/Loot-Shulker aus der Runde entfernen - der
     * RollbackTracker rollt nur BLOECKE zurueck, keine Item-Entities. Laeuft
     * erst hier (beim Freigeben), also erst NACHDEM das volle
     * Loot-Schutzfenster des Gewinners abgelaufen ist - alles, was er
     * liegen gelassen hat, ist zu dem Zeitpunkt ohnehin schon freies Loot.
     */
    private void aufraeumen(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item || entity instanceof ExperienceOrb) {
                entity.remove();
            }
        }
    }

    public synchronized String naechsterAusWarteschlange() {
        return warteschlange.poll();
    }

    public Arena arena(String name) {
        return arenen.get(name);
    }

    public List<Arena> alle() {
        return new ArrayList<>(arenen.values());
    }
}
