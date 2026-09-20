package de.lemonpvp.duelplus.arena;

import de.lemonpvp.duelplus.DuelPlus;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;

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
 * Jede Arena ist eine eigene, per WorldCreator (normaler Vanilla-
 * Generator) erzeugte Welt mit einer flachen Steinplattform als
 * Kampfboden: rohes Vanilla-Gelaende waere je nach Landeplatz fuer die
 * zwei Duellanten unterschiedlich fair (Deckung, Hoehenvorteil, Wasser/
 * Lava in der Naehe, ...) - die Plattform macht jede Arena gleich.
 *
 * Block-Aenderungen sind waehrend eines Duells erlaubt (siehe
 * RollbackTracker), deshalb muss hier nichts nach jedem Kampf neu
 * gebaut werden - nur einmal beim allerersten Start.
 */
public final class ArenaManager {

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
            Arena arena = ladeOderErzeuge(name);
            arenen.put(name, arena);
        }
        plugin.getLogger().info("DuelPlus: " + arenen.size() + " Arena(s) bereit.");
    }

    private Arena ladeOderErzeuge(String name) {
        World world = Bukkit.getWorld(name);
        boolean neu = world == null;
        if (world == null) {
            world = new WorldCreator(name).environment(World.Environment.NORMAL).createWorld();
        }
        if (world == null) {
            throw new IllegalStateException("Arena-Welt " + name + " konnte nicht erzeugt werden");
        }
        konfiguriereWelt(world);

        int radius = Math.max(10, plugin.getConfig().getInt("arenen.worldborder-groesse", 50));
        int hoehe = plugin.getConfig().getInt("arenen.plattform-hoehe", 100);
        int abstand = Math.max(4, plugin.getConfig().getInt("arenen.spawn-abstand", 20));

        if (neu) {
            plattformBauen(world, radius, hoehe);
        }

        Location mitte = new Location(world, 0.5, hoehe + 1, 0.5);
        world.setSpawnLocation(mitte.getBlockX(), mitte.getBlockY(), mitte.getBlockZ());
        world.getWorldBorder().setCenter(mitte);
        world.getWorldBorder().setSize(radius * 2.0);

        Location spawnA = mitte.clone().add(-abstand / 2.0, 0, 0);
        Location spawnB = mitte.clone().add(abstand / 2.0, 0, 0);
        spawnA.setYaw(90f);
        spawnB.setYaw(-90f);
        return new Arena(name, world, spawnA, spawnB);
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
        world.setTime(6000);
        world.setStorm(false);
        world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
    }

    private void plattformBauen(World world, int radius, int y) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(x, y, z).setType(Material.SMOOTH_STONE, false);
                for (int dy = 1; dy <= 5; dy++) {
                    world.getBlockAt(x, y + dy, z).setType(Material.AIR, false);
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

    public synchronized void freigeben(String arenaName) {
        belegtVon.remove(arenaName);
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
