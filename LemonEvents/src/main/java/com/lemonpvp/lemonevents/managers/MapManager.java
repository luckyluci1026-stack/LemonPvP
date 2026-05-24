package com.lemonpvp.lemonevents.managers;

import com.lemonpvp.lemonevents.LemonEvents;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class MapManager {

    private final LemonEvents plugin;

    public MapManager(LemonEvents plugin) {
        this.plugin = plugin;
    }

    // ── Schematic Save ────────────────────────────────────────────────────────

    public boolean saveSchematic(World world, BlockVector3 min, BlockVector3 max, File outputFile) {
        try {
            outputFile.getParentFile().mkdirs();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld).maxBlocks(-1).build()) {
                ForwardExtentCopy copy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
                Operations.complete(copy);
            }

            try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(
                    new BufferedOutputStream(new FileOutputStream(outputFile)))) {
                writer.write(clipboard);
            }
            plugin.getLogger().info("Saved schematic: " + outputFile.getPath());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save schematic", e);
            return false;
        }
    }

    // ── Schematic Load & Paste ────────────────────────────────────────────────

    public boolean loadAndPasteSchematic(World world, BlockVector3 pasteAt, File schematicFile) {
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic not found: " + schematicFile.getPath());
            return false;
        }
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format: " + schematicFile.getName());
                return false;
            }
            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(
                    new BufferedInputStream(new FileInputStream(schematicFile)))) {
                clipboard = reader.read();
            }
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld).maxBlocks(-1).build()) {
                Operation op = new ClipboardHolder(clipboard)
                        .createPaste(session)
                        .to(pasteAt)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(op);
            }
            plugin.getLogger().info("Pasted schematic at " + pasteAt);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load schematic", e);
            return false;
        }
    }

    // ── FFA Backup ────────────────────────────────────────────────────────────

    public boolean backupFfaWorld() {
        String worldPath = plugin.getConfig().getString("ffa-world-path", "");
        String backupDir = plugin.getConfig().getString("ffa-backup-dir", "");
        if (worldPath.isEmpty() || backupDir.isEmpty()) {
            plugin.getLogger().warning("FFA world path or backup dir not configured.");
            return false;
        }

        // For cross-server FAWE operations we save the FFA world via file copy
        // (assumes shared filesystem or NFS mount)
        File worldFolder = new File(worldPath);
        if (!worldFolder.exists()) {
            plugin.getLogger().warning("FFA world folder not found: " + worldPath);
            return false;
        }

        File backupFolder = new File(backupDir, "ffa_backup_" + System.currentTimeMillis());
        try {
            copyDirectory(worldFolder.toPath(), backupFolder.toPath());
            plugin.getDatabase().saveFfaBackupRecord(backupFolder.getAbsolutePath());
            plugin.getLogger().info("FFA backup created at: " + backupFolder.getAbsolutePath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "FFA backup failed", e);
            return false;
        }
    }

    public boolean restoreFfaWorld(String backupPath) {
        String worldPath = plugin.getConfig().getString("ffa-world-path", "");
        if (worldPath.isEmpty()) {
            plugin.getLogger().warning("FFA world path not configured.");
            return false;
        }

        File backupFolder = new File(backupPath);
        if (!backupFolder.exists()) {
            plugin.getLogger().warning("FFA backup folder not found: " + backupPath);
            return false;
        }

        File worldFolder = new File(worldPath);
        try {
            // Delete current world folder contents
            deleteDirectory(worldFolder.toPath());
            // Copy backup over
            copyDirectory(backupFolder.toPath(), worldFolder.toPath());
            // Delete the backup
            deleteDirectory(backupFolder.toPath());
            plugin.getDatabase().clearFfaBackupRecord();
            plugin.getLogger().info("FFA restored from backup: " + backupPath);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "FFA restore failed", e);
            return false;
        }
    }

    // ── FAWE Procedural Building ──────────────────────────────────────────────

    /** Fills a flat platform (floor + air above). */
    public void fillPlatform(World world, BlockVector3 min, BlockVector3 max,
                              Material floor, Material fill) {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld).maxBlocks(-1).build()) {
            // Fill air layer
            BlockVector3 airMax = BlockVector3.at(max.x(), max.y(), max.z());
            session.setBlocks(new CuboidRegion(weWorld,
                    BlockVector3.at(min.x(), min.y() + 1, min.z()), airMax),
                    BlockTypes.AIR.getDefaultState());
            // Fill floor
            session.setBlocks(new CuboidRegion(weWorld,
                    BlockVector3.at(min.x(), min.y(), min.z()),
                    BlockVector3.at(max.x(), min.y(), max.z())),
                    BukkitAdapter.adapt(floor.createBlockData()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "fillPlatform error", e);
        }
    }

    /** Places a single block. */
    public void placeBlock(World world, Location loc, Material material) {
        world.getBlockAt(loc).setType(material);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
             .sorted(java.util.Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }
}
