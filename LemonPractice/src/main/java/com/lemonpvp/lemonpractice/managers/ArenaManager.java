package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Arena;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ArenaManager {

    private final LemonPractice plugin;
    private final Map<Integer, Arena> arenas = new ConcurrentHashMap<>();

    public ArenaManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    public CompletableFuture<Void> loadAll() {
        return plugin.getDatabase().loadAllArenas().thenAccept(list -> {
            arenas.clear();
            for (Arena a : list) {
                arenas.put(a.getId(), a);
            }
            plugin.getLogger().info("[ArenaManager] Loaded " + arenas.size() + " arenas.");
        });
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public Optional<Arena> getFreeArenaForGamemode(String gamemode) {
        return arenas.values().stream()
                .filter(a -> a.isBoundTo(gamemode) && !a.isInUse() && a.isFullyConfigured())
                .findFirst();
    }

    /**
     * Whether any arena on this server could ever host this gamemode, regardless of whether one
     * is free right now. Lets callers tell "everything is busy" (wait) apart from "nothing is
     * configured" (an admin has to fix it), instead of retrying forever.
     */
    public boolean hasArenaForGamemode(String gamemode) {
        return arenas.values().stream()
                .anyMatch(a -> a.isBoundTo(gamemode) && a.isFullyConfigured());
    }

    public Arena getArenaByName(String name) {
        return arenas.values().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Arena getArenaById(int id) {
        return arenas.get(id);
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    // -----------------------------------------------------------------------
    // In-use state
    // -----------------------------------------------------------------------

    public void markInUse(Arena arena, boolean inUse) {
        arena.setInUse(inUse);
        arenas.put(arena.getId(), arena);
        // Vanilla (config-defined) arenas aren't in the DB — don't try to persist.
        if (!arena.isVanilla()) {
            plugin.getDatabase().setArenaInUse(arena.getId(), inUse);
        }
    }

    /** Registers an in-memory vanilla-world arena (from DuelWorldManager, not persisted). */
    public void registerVanillaArena(Arena arena) {
        arenas.put(arena.getId(), arena);
    }

    // -----------------------------------------------------------------------
    // FAWE – save schematic
    // -----------------------------------------------------------------------

    public CompletableFuture<Void> saveArenaSchematic(Arena arena) {
        if (!plugin.isWorldEditAvailable()) {
            plugin.getLogger().warning("[ArenaManager] Cannot save schematic for '" + arena.getName()
                    + "' — WorldEdit/FAWE is not loaded. Live vanilla-world duels are unaffected; "
                    + "install a FAWE build matching this server's Java version to use schematic arenas.");
            return CompletableFuture.completedFuture(null);
        }
        // WorldEdit calls are isolated in ArenaSchematicIO so this class links without FAWE.
        return CompletableFuture.runAsync(() -> {
            try {
                ArenaSchematicIO.save(plugin, arena);
            } catch (Throwable t) {
                plugin.getLogger().severe("[ArenaManager] saveArenaSchematic failed for '"
                        + arena.getName() + "': " + t.getMessage());
            }
        });
    }

    // -----------------------------------------------------------------------
    // FAWE – reset arena (paste schematic)
    // -----------------------------------------------------------------------

    public CompletableFuture<Void> resetArena(Arena arena) {
        // Vanilla biome worlds have no schematic — heal them by rolling back the
        // blocks changed during the match. Block edits must run on the main
        // thread; run synchronously when we're already on it (the duel cleanup
        // task is) so the heal completes before the arena can be re-booked.
        // This path uses no WorldEdit, so it works even when FAWE isn't loaded.
        if (arena.isVanilla()) {
            if (Bukkit.isPrimaryThread()) {
                plugin.getArenaRollbackManager().restore(arena.getWorldName());
            } else {
                Bukkit.getScheduler().runTask(plugin,
                        () -> plugin.getArenaRollbackManager().restore(arena.getWorldName()));
            }
            return CompletableFuture.completedFuture(null);
        }
        if (!plugin.isWorldEditAvailable()) {
            plugin.getLogger().warning("[ArenaManager] Cannot reset schematic arena '" + arena.getName()
                    + "' — WorldEdit/FAWE is not loaded.");
            return CompletableFuture.completedFuture(null);
        }
        // WorldEdit calls are isolated in ArenaSchematicIO so this class links without FAWE.
        return CompletableFuture.runAsync(() -> {
            try {
                ArenaSchematicIO.paste(plugin, arena);
            } catch (Throwable t) {
                plugin.getLogger().severe("[ArenaManager] resetArena failed for '"
                        + arena.getName() + "': " + t.getMessage());
            }
        });
    }
}
