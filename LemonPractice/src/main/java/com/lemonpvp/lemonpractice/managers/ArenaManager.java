package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.model.Arena;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        plugin.getDatabase().setArenaInUse(arena.getId(), inUse);
    }

    // -----------------------------------------------------------------------
    // FAWE – save schematic
    // -----------------------------------------------------------------------

    public CompletableFuture<Void> saveArenaSchematic(Arena arena) {
        return CompletableFuture.runAsync(() -> {
            try {
                World bukkitWorld = Bukkit.getWorld(arena.getWorldName());
                if (bukkitWorld == null) {
                    plugin.getLogger().warning("[ArenaManager] saveArenaSchematic: world '"
                            + arena.getWorldName() + "' not loaded.");
                    return;
                }

                BlockVector3 min = BlockVector3.at(arena.getRegionX1(), arena.getRegionY1(), arena.getRegionZ1());
                BlockVector3 max = BlockVector3.at(arena.getRegionX2(), arena.getRegionY2(), arena.getRegionZ2());
                CuboidRegion region = new CuboidRegion(new BukkitWorld(bukkitWorld), min, max);

                BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
                clipboard.setOrigin(min);

                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(new BukkitWorld(bukkitWorld))
                        .maxBlocks(-1)
                        .build()) {

                    ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, min);
                    copy.setCopyingEntities(false);
                    copy.setCopyingBiomes(false);
                    Operations.complete(copy);
                }

                File schemDir = new File(plugin.getDataFolder(), "schematics");
                if (!schemDir.exists()) schemDir.mkdirs();

                File schemFile = new File(schemDir, arena.getName() + ".schem");
                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC
                        .getWriter(new FileOutputStream(schemFile))) {
                    writer.write(clipboard);
                }

                String relativePath = "schematics/" + arena.getName() + ".schem";
                arena.setSchematicPath(relativePath);
                plugin.getDatabase().setArenaSchematic(arena.getId(), relativePath);

                plugin.getLogger().info("[ArenaManager] Saved schematic for arena '"
                        + arena.getName() + "' -> " + schemFile.getPath());

            } catch (Exception e) {
                plugin.getLogger().severe("[ArenaManager] saveArenaSchematic failed for '"
                        + arena.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // -----------------------------------------------------------------------
    // FAWE – reset arena (paste schematic)
    // -----------------------------------------------------------------------

    public CompletableFuture<Void> resetArena(Arena arena) {
        return CompletableFuture.runAsync(() -> {
            try {
                String path = arena.getSchematicPath();
                if (path == null || path.isEmpty()) {
                    plugin.getLogger().warning("[ArenaManager] resetArena: no schematic path for '"
                            + arena.getName() + "'.");
                    return;
                }

                File schemFile = new File(plugin.getDataFolder(), path);
                if (!schemFile.exists()) {
                    plugin.getLogger().warning("[ArenaManager] resetArena: schematic file not found: "
                            + schemFile.getPath());
                    return;
                }

                ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
                if (format == null) {
                    plugin.getLogger().warning("[ArenaManager] resetArena: unknown format for '"
                            + schemFile.getName() + "'.");
                    return;
                }

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                    clipboard = reader.read();
                }

                World bukkitWorld = Bukkit.getWorld(arena.getWorldName());
                if (bukkitWorld == null) {
                    plugin.getLogger().warning("[ArenaManager] resetArena: world '"
                            + arena.getWorldName() + "' not loaded.");
                    return;
                }

                BlockVector3 origin = BlockVector3.at(
                        arena.getRegionX1(), arena.getRegionY1(), arena.getRegionZ1());

                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(new BukkitWorld(bukkitWorld))
                        .maxBlocks(-1)
                        .build()) {

                    Operation paste = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(origin)
                            .ignoreAirBlocks(false)
                            .build();
                    Operations.complete(paste);
                }

                plugin.getLogger().info("[ArenaManager] Reset arena '" + arena.getName() + "'.");

            } catch (Exception e) {
                plugin.getLogger().severe("[ArenaManager] resetArena failed for '"
                        + arena.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
