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

/**
 * All WorldEdit / FastAsyncWorldEdit calls live here, isolated from {@link ArenaManager}.
 *
 * <p>The JVM eagerly resolves the WorldEdit types referenced in these method bodies when it
 * loads the enclosing class. By keeping them out of {@code ArenaManager} — which is constructed
 * during {@code onEnable} — the plugin still enables when FAWE failed to load (e.g. a FAWE jar
 * built for a newer Java than the server). This class is only loaded the first time a schematic
 * operation actually runs, and callers guard that behind {@code plugin.isWorldEditAvailable()}.
 */
final class ArenaSchematicIO {

    private ArenaSchematicIO() {}

    /** Copies the arena's configured region into a schematic file and records its path. */
    static void save(LemonPractice plugin, Arena arena) throws Exception {
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
        try (FileOutputStream fos = new FileOutputStream(schemFile);
             ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(fos)) {
            writer.write(clipboard);
        }

        String relativePath = "schematics/" + arena.getName() + ".schem";
        arena.setSchematicPath(relativePath);
        plugin.getDatabase().setArenaSchematic(arena.getId(), relativePath);

        plugin.getLogger().info("[ArenaManager] Saved schematic for arena '"
                + arena.getName() + "' -> " + schemFile.getPath());
    }

    /** Pastes the arena's saved schematic back over its region, restoring the build. */
    static void paste(LemonPractice plugin, Arena arena) throws Exception {
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
    }
}
