package de.lemonpvp.duelplus.arena;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;

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
 * Terraingenerierung, deren komplette begehbare Flaeche DIREKT auf
 * Bedrock liegt (nur 1 Block Abstand, siehe VOID_GENERATOR_SETTINGS) -
 * bewusst KEIN tiefer Abgrund mehr darunter. Das macht Endkristall-/
 * Anker-PvP unbedenklich: reisst eine Explosion den Boden weg, faellt
 * man hoechstens 1 Block auf den unzerstoerbaren Bedrock statt durch
 * eine grosse Luecke. Seitlich steckt die ganze Arena in einer
 * unsichtbaren, unzerstoerbaren Barriere-Box bis hinunter auf
 * Bedrock-Niveau (siehe barriereBauen) - kein sichtbarer, begehbarer
 * Rand mehr, auf dem man stehen oder von dem man abrutschen koennte,
 * kein Spalt, durch den man seitlich entkommen koennte. Die
 * schrumpfende Worldborder (siehe DuellSessionManager) bleibt der
 * Druckmechanismus, der beide zueinander zwingt - inklusive ihres
 * normalen Vanilla-Schadens ausserhalb.
 *
 * Der Boden ist ein aufwendiges Muster: zwei konzentrische Ringe plus
 * eine feine Schachbrett-Textur dazwischen, ueberlagert von acht
 * Speichen durch die Mitte (zu Spawnpunkten, Deckungspfeilern, Eck-
 * Tuermen) - alles aus gewoehnlichen, robusten Bloecken (keine Gefahren-
 * Materialien wie Lava/Wasser/Eis). Je Arena eine andere
 * Materialpalette, sonst identischer Aufbau.
 *
 * Block-Aenderungen sind waehrend eines Duells erlaubt (siehe
 * RollbackTracker), deshalb muss hier nichts nach jedem Kampf neu
 * gebaut werden - nur einmal beim allerersten Start.
 */
public final class ArenaManager {

    /**
     * Flache Welt OHNE echte Vanilla-Terraingenerierung - nur eine
     * einzelne Bedrock-Schicht als Fundament. Alles andere (der
     * komplette begehbare Boden) malt plattformBauen direkt DARAUF,
     * innerhalb des Arena-Radius - ausserhalb davon (durch die
     * Barriere-Box ohnehin nicht erreichbar) bleibt es bei nacktem
     * Bedrock.
     *
     * WICHTIG: Bedrock liegt beim Flachwelt-Generator IMMER fest am
     * Minimum der Welt (Y=-64) und laesst sich nicht verschieben -
     * deshalb setzt arenen.plattform-hoehe die Plattform bewusst nur
     * 1 Block darueber (Standard Y=-63). Wirkt nur beim ALLERERSTEN
     * Erzeugen einer Arena-Welt: bereits vorhandene Weltordner (z.B.
     * aus einer aelteren DuelPlus-Version mit anderem Aufbau) muessen
     * einmalig manuell geloescht werden, damit sie mit diesem Preset
     * neu entstehen - siehe README.
     */
    private static final String VOID_GENERATOR_SETTINGS =
            "{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1}],\"biome\":\"minecraft:the_void\"}";

    /**
     * Je Arena eine andere Materialpalette (Boden, Akzent, Mauer/Struktur,
     * Licht) - sonst wirkt spaetestens die dritte/vierte Arena immer
     * gleich. Reihum verteilt nach Arena-Nummer. Bewusst nur gewoehnliche,
     * robuste Bloecke - keine Gefahren-Materialien.
     */
    private static final Material[][] PALETTEN = {
            {Material.SMOOTH_STONE, Material.POLISHED_ANDESITE, Material.STONE_BRICK_WALL, Material.LANTERN},
            {Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE_WALL, Material.SOUL_LANTERN},
            {Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.SANDSTONE_WALL, Material.LANTERN},
            {Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS, Material.POLISHED_BLACKSTONE_WALL, Material.SOUL_LANTERN},
    };

    private final DuelPlus plugin;
    private final Map<String, Arena> arenen = new LinkedHashMap<>();
    private final Map<String, String> belegtVon = new LinkedHashMap<>();
    private final Deque<String> warteschlange = new ArrayDeque<>();

    public ArenaManager(DuelPlus plugin) {
        this.plugin = plugin;
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

        int radius = Math.max(10, plugin.getConfig().getInt("arenen.worldborder-groesse", 115));
        int hoehe = plugin.getConfig().getInt("arenen.plattform-hoehe", -63);
        int abstand = Math.max(4, plugin.getConfig().getInt("arenen.spawn-abstand", 20));

        // Die Plattform bleibt dauerhaft geladen (nicht erst, wenn zufaellig
        // ein Spieler in der Naehe ist) - sonst kann direkt nach der Ankunft
        // ein kurzes Nachladen wie ein Ruckler/"Zurueckgebuggtwerden" wirken.
        chunksLaden(world, radius);

        int paletteIndex = (index - 1) % PALETTEN.length;
        if (neu) {
            Material[] palette = PALETTEN[paletteIndex];
            plattformBauen(world, radius, hoehe, palette);
            barriereBauen(world, radius, hoehe);
            plugin.getLogger().info("DuelPlus: Arena '" + name + "' NEU gebaut (Palette "
                    + (paletteIndex + 1) + "/" + PALETTEN.length + ", Radius " + radius
                    + ", Plattform-Hoehe " + hoehe + ").");
        } else {
            // Nur aus der Welt geladen, NICHT neu gebaut - eine bereits
            // vorhandene Weltdatei behaelt ihren alten Aufbau, ganz gleich,
            // was jetzt in der config.yml oder im Plugin-Code steht. Wer
            // eine strukturelle Aenderung erwartet, aber diese Zeile hier
            // im Log sieht statt "NEU gebaut" oben, hat die Weltordner
            // nicht wirklich geloescht (oder der Server wurde seitdem nicht
            // neu gestartet - ein reines /duelplus reload baut nichts neu).
            plugin.getLogger().info("DuelPlus: Arena '" + name + "' aus vorhandener Welt geladen - "
                    + "KEINE Struktur-Aenderung uebernommen (Weltordner dafuer loeschen + Server neu starten).");
        }

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

    /** Haelt die Chunks der kompletten Plattform dauerhaft geladen (siehe ladeOderErzeuge). */
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
        // BEWUSST NICHT Difficulty.PEACEFUL: das erledigt DO_MOB_SPAWNING
        // oben schon vollstaendig (keine Mobs in einer frischen Void-Welt
        // sowieso). Peaceful hat bei manchen Bedrock-/Geyser-Clients dazu
        // gefuehrt, dass eigene Treffer serverseitig komplett ins Leere
        // gingen (kein Ton, kein Knockback, kein Schaden) - Java-PvP ist
        // von der Difficulty unabhaengig, Bedrock-seitig offenbar nicht
        // immer.
    }

    // ------------------------------------------------------------ Boden

    /**
     * Zwei konzentrische Ringe (bei 35% und 75% des Radius) plus eine
     * feine Schachbrett-Textur dazwischen, ueberlagert von acht Speichen
     * durch die Mitte - waagerecht/senkrecht zu den Spawnpunkten bzw.
     * den Deckungspfeilern, diagonal zu den vier Eck-Tuermen. Direkt auf
     * dem Bedrock der Welt gebaut (siehe VOID_GENERATOR_SETTINGS),
     * bewusst OHNE sichtbare Randmauer: der aeussere Abschluss ist
     * ausschliesslich die unsichtbare Barriere-Box (siehe
     * barriereBauen) - keine begehbare Kante mehr, auf der man stehen
     * oder von der man unerwartet abrutschen/durchfallen koennte.
     */
    private void plattformBauen(World world, int radius, int y, Material[] palette) {
        Material boden = palette[0];
        Material akzent = palette[1];
        Material mauer = palette[2];
        Material licht = palette[3];
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
                    bodenBlock = mauer;
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

    /** Wie hoch die unsichtbare Barriere-Box ueber der Plattform reicht - komfortable Reserve gegen jeden Enderperlen-Bogen. */
    private static final int BARRIERE_HOEHE = 30;

    /**
     * Unsichtbare, unzerstoerbare Box (vier Waende + Decke) in der VOLLEN
     * Start-Groesse der Arena, OHNE sichtbare Mauer mehr davor (siehe
     * Klassen-Kommentar) - macht ein Entkommen (z.B. per Enderperle ueber
     * die schrumpfende Worldborder, siehe ladeOderErzeuge) unabhaengig
     * vom aktuellen Border-Stand unmoeglich. Reicht bis EXAKT auf
     * Bedrock-Niveau hinunter (kein Spalt): das gesamte Fundament der
     * Arena liegt nur 1 Block ueber dem Bedrock, ein tieferer Puffer ist
     * nicht mehr noetig UND wuerde unterhalb des Bedrocks ohnehin nur ins
     * Leere zeigen. KEIN Boden in der Box selbst - das Bedrock der Welt
     * ist bereits der Boden.
     */
    private void barriereBauen(World world, int radius, int y) {
        int aussen = radius + 1;
        int unten = y - 1;
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
