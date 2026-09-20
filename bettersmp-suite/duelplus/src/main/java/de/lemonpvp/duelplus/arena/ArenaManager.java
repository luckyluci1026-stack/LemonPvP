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
 * Jede Arena ist eine eigene, komplett LEERE ("Void") Welt mit einer
 * flachen, dekorierten Plattform als Kampfboden: echtes Vanilla-Gelaende
 * waere je nach Landeplatz fuer die zwei Duellanten unterschiedlich fair
 * (Deckung, Hoehenvorteil, Wasser/Lava in der Naehe, ...) UND man wuerde
 * am Rand der Plattform in die "echte" Welt darunter/darum schauen -
 * beides verhindert die Void-Welt zuverlaessig.
 *
 * Der Boden ist ein Kompassmuster: konzentrische Kreise als Textur,
 * ueberlagert von acht Speichen durch die Mitte - waagerecht/senkrecht
 * zu den Spawnpunkten bzw. den Deckungspfeilern, diagonal zu den vier
 * Eck-Tuermen. Wirkt dadurch gestaltet statt willkuerlich, weil es
 * tatsaechlich alle anderen Bauten der Arena optisch verbindet.
 *
 * Block-Aenderungen sind waehrend eines Duells erlaubt (siehe
 * RollbackTracker), deshalb muss hier nichts nach jedem Kampf neu
 * gebaut werden - nur einmal beim allerersten Start.
 */
public final class ArenaManager {

    /**
     * Dasselbe Void-Preset, das Vanilla selbst fuer den Welttyp "Leere"
     * verwendet - nur EINE Luft-Schicht, keine Bloecke, Biom "the_void".
     * Wirkt nur beim ALLERERSTEN Erzeugen einer Arena-Welt: bereits
     * vorhandene Weltordner (z.B. aus einer aelteren DuelPlus-Version mit
     * echtem Gelaende) muessen einmalig manuell geloescht werden, damit
     * sie mit diesem Preset neu entstehen - siehe README.
     */
    private static final String VOID_GENERATOR_SETTINGS =
            "{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\"}";

    /**
     * Je Arena ein anderes Aussehen (Boden, Akzent, Mauer, Licht) - sonst
     * wirkt spaetestens die dritte/vierte Arena immer gleich und "tot".
     * Reihum verteilt nach Arena-Nummer.
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

        int radius = Math.max(10, plugin.getConfig().getInt("arenen.worldborder-groesse", 50));
        int hoehe = plugin.getConfig().getInt("arenen.plattform-hoehe", 100);
        int abstand = Math.max(4, plugin.getConfig().getInt("arenen.spawn-abstand", 20));

        // Die Plattform bleibt dauerhaft geladen (nicht erst, wenn zufaellig
        // ein Spieler in der Naehe ist) - sonst kann direkt nach der Ankunft
        // ein kurzes Nachladen wie ein Ruckler/"Zurueckgebuggtwerden" wirken.
        chunksLaden(world, radius);

        if (neu) {
            Material[] palette = PALETTEN[(index - 1) % PALETTEN.length];
            plattformBauen(world, radius, hoehe, palette);
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
        return new Arena(name, world, spawnA, spawnB, vollGroesse);
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
        world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
    }

    private void plattformBauen(World world, int radius, int y, Material[] palette) {
        Material boden = palette[0];
        Material akzent = palette[1];
        Material mauer = palette[2];
        Material licht = palette[3];
        int ringBreite = 6;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Quadratischer Abstand (Chebyshev) NUR fuer die Aussenmauer -
                // die muss dem eckigen Rand der Iteration/Worldborder folgen
                // (Minecrafts Worldborder ist selbst ein Quadrat, kein Kreis).
                int eckigerRand = Math.max(Math.abs(x), Math.abs(z));
                // Echter (runder) Abstand fuer das Bodenmuster selbst - wirkt
                // als konzentrische Kreise deutlich "gestalteter" als eckige
                // Ringe.
                double rundeEntfernung = Math.sqrt((double) x * x + (double) z * z);
                // Speichen: verbinden ueber die Mitte die Spawnpunkte (Z=0),
                // die Deckungspfeiler (X=0) und diagonal alle vier Eck-
                // tuerme (|X|=|Z|) - das Muster bekommt dadurch einen echten
                // Bezug zum Rest der Arena statt willkuerlich zu wirken.
                boolean speiche = (x == 0 || z == 0 || Math.abs(x) == Math.abs(z)) && rundeEntfernung > 3;
                Material bodenBlock;
                if (rundeEntfernung <= 3) {
                    bodenBlock = akzent;
                } else if (speiche) {
                    bodenBlock = mauer;
                } else {
                    int ring = (int) (rundeEntfernung / ringBreite);
                    bodenBlock = (ring % 2 == 0) ? boden : akzent;
                }
                world.getBlockAt(x, y, z).setType(bodenBlock, false);
                for (int dy = 1; dy <= 5; dy++) {
                    world.getBlockAt(x, y + dy, z).setType(Material.AIR, false);
                }
                if (eckigerRand == radius) {
                    world.getBlockAt(x, y + 1, z).setType(mauer, false);
                }
            }
        }
        // Vier Eckpfeiler mit Licht - Blickfang gegen die "leere graue
        // Flaeche" UND ein staerkerer Hinweis auf den Rand als nur die
        // 1 Block hohe Mauer.
        int[][] ecken = {{-radius, -radius}, {-radius, radius}, {radius, -radius}, {radius, radius}};
        for (int[] ecke : ecken) {
            int ex = ecke[0];
            int ez = ecke[1];
            world.getBlockAt(ex, y + 1, ez).setType(mauer, false);
            world.getBlockAt(ex, y + 2, ez).setType(mauer, false);
            world.getBlockAt(ex, y + 3, ez).setType(licht, false);
        }
        // Zwei Deckungspfeiler auf der Z-Achse (X=0) - taktische Tiefe
        // gegen die sonst komplett leere Mitte. Bewusst NUR auf der Achse
        // SENKRECHT zur Spawn-Linie (die liegt auf X, siehe spawnA/spawnB
        // unten): jeder Punkt mit X=0 ist per Pythagoras IMMER exakt gleich
        // weit von beiden Startpunkten entfernt, egal welches Z - keiner
        // der beiden wird dadurch bevorteilt.
        int deckungsAbstand = Math.min(radius - 8, 16);
        if (deckungsAbstand > 0) {
            for (int vorzeichen : new int[]{-1, 1}) {
                int dz = vorzeichen * deckungsAbstand;
                world.getBlockAt(0, y + 1, dz).setType(mauer, false);
                world.getBlockAt(0, y + 2, dz).setType(mauer, false);
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
