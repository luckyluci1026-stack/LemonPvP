package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Arena;
import com.lemonpvp.lemonpractice.model.Gamemode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.List;
import java.util.Locale;

/**
 * Loads the VANILLA biome duel worlds this server owns (config
 * {@code duel-worlds.worlds}) and registers one arena per world. Each world is
 * a real Minecraft world with natural terrain — fights happen on it directly,
 * no schematics. Blocks broken during a match heal via {@link ArenaRollbackManager}.
 *
 * <p>One duel runs per world at a time (world == arena), so 15 worlds = 15
 * concurrent duels on this server. Only runs on {@code server-type: DUELS}.</p>
 */
public class DuelWorldManager {

    private final LemonPractice plugin;
    private int nextId = -1; // synthetic negative ids so config arenas never clash with DB ones

    public DuelWorldManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    /** Loads every configured world and registers it as a vanilla arena. Call on enable (DUELS only). */
    public void loadAndRegister() {
        var cfg = plugin.getDuelWorldsConfig();
        if (cfg == null || !cfg.getBoolean("enabled", true)) return;

        List<String> worlds = new java.util.ArrayList<>(cfg.getStringList("worlds"));
        // Generator shorthand: `biomes: [badlands, taiga]` + `per-biome: 5`
        // expands to badlands-01..05, taiga-01..05 — so the world count is one
        // number in the config instead of a hand-written list.
        List<String> biomes = cfg.getStringList("biomes");
        int perBiome = cfg.getInt("per-biome", 0);
        for (String biome : biomes) {
            for (int i = 1; i <= perBiome; i++) {
                String name = String.format("%s-%02d", biome, i);
                if (!worlds.contains(name)) worlds.add(name);
            }
        }
        if (worlds.isEmpty()) {
            plugin.getLogger().warning("[DuelWorlds] server-type is DUELS but duel-worlds.yml 'worlds' is empty — "
                    + "no vanilla arenas registered.");
            return;
        }

        double offset = cfg.getDouble("spawn-offset", 20);
        int fixedY = cfg.getInt("spawn-y", -1);
        List<String> gmConfig = cfg.getStringList("gamemodes");

        int ok = 0;
        for (String name : worlds) {
            World world = loadWorld(name);
            if (world == null) {
                plugin.getLogger().warning("[DuelWorlds] Could not load world '" + name + "' — skipped.");
                continue;
            }
            registerArena(world, offset, fixedY, gmConfig);
            plugin.getArenaRollbackManager().track(world.getName());
            ok++;
        }
        plugin.getLogger().info("[DuelWorlds] Registered " + ok + " vanilla duel arenas.");
    }

    /** Loads an existing vanilla world, or creates a normal one if the folder is missing. */
    private World loadWorld(String name) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        try {
            // No custom generator → normal vanilla biome terrain.
            World w = new WorldCreator(name).createWorld();
            if (w != null) {
                // Duel worlds are combat arenas: no natural mobs, calm weather, keep them loaded.
                w.setSpawnFlags(false, false);
                try { w.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false); } catch (Throwable ignored) {}
                try { w.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false); } catch (Throwable ignored) {}
                try { w.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false); } catch (Throwable ignored) {}
                try { w.setGameRule(org.bukkit.GameRule.FALL_DAMAGE, true); } catch (Throwable ignored) {}
                w.setStorm(false);
                w.setThundering(false);
                // Don't pin spawn chunks: an idle arena world shouldn't sit in RAM.
                // Chunks load on demand when two fighters are teleported in.
                try { w.setKeepSpawnInMemory(false); } catch (Throwable ignored) {}
            }
            return w;
        } catch (Throwable t) {
            plugin.getLogger().warning("[DuelWorlds] Error loading '" + name + "': " + t.getMessage());
            return null;
        }
    }

    /** Registers one vanilla arena for a world: two spawns offset from the world spawn, facing each other. */
    private void registerArena(World world, double offset, int fixedY, List<String> gmConfig) {
        Location center = world.getSpawnLocation();
        double half = Math.max(2, offset) / 2.0;

        Location s1 = spawn(world, center.getX() + half, center.getZ(), fixedY, 90f);   // east side, looks west
        Location s2 = spawn(world, center.getX() - half, center.getZ(), fixedY, 270f);  // west side, looks east

        Arena arena = new Arena(nextId--, world.getName());
        arena.setWorldName(world.getName());
        arena.setSpawn1(s1);
        arena.setSpawn2(s2);
        arena.setSpawnSpec(spawn(world, center.getX(), center.getZ() + half, fixedY, 0f));
        arena.setVanilla(true);
        arena.setActive(true);

        // Bind to the configured gamemodes, or to every enabled gamemode when unset.
        if (gmConfig == null || gmConfig.isEmpty()) {
            for (Gamemode gm : plugin.getGamemodeManager().getAllGamemodes()) {
                if (gm.isEnabled()) arena.addGamemodeBind(gm.getId());
            }
        } else {
            for (String gm : gmConfig) arena.addGamemodeBind(gm.toLowerCase(Locale.ROOT));
        }

        plugin.getArenaManager().registerVanillaArena(arena);
    }

    private Location spawn(World world, double x, double z, int fixedY, float yaw) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        double y = (fixedY >= 0) ? fixedY : world.getHighestBlockYAt(bx, bz) + 1;
        return new Location(world, bx + 0.5, y, bz + 0.5, yaw, 0f);
    }
}
