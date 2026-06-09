package com.lemonpvp.lemonpractice.commands;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.builder.ArenaBuilder;
import com.lemonpvp.lemonpractice.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AowArenaCommand implements CommandExecutor {

    private final LemonPractice plugin;

    // Per-player WorldEdit-style position selections: int[] {x, y, z}
    private final Map<UUID, int[]> pos1Selections = new HashMap<>();
    private final Map<UUID, int[]> pos2Selections = new HashMap<>();

    public AowArenaCommand(LemonPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }

        if (!player.hasPermission("lemonpractice.admin.arena")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena create <name>"); return true; }
                String name = args[1];
                plugin.getDatabase().createArena(name).thenAccept(id -> {
                    if (id == -1) {
                        player.sendMessage("§cFailed to create arena '" + name + "' (may already exist or DB error).");
                        return;
                    }
                    Arena arena = new Arena(id, name);
                    plugin.getArenaManager().getAllArenas(); // already loaded; add manually
                    // Reload all arenas so the new one appears in cache
                    plugin.getArenaManager().loadAll().thenRun(() ->
                            player.sendMessage("§aArena §e" + name + " §acreated (id=" + id + ")."));
                });
            }

            case "setspawn1" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena setspawn1 <name>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                plugin.getDatabase().updateArenaSpawn(arena.getId(), 1, player.getLocation()).thenRun(() -> {
                    arena.setSpawn1(player.getLocation());
                    arena.setWorldName(player.getWorld().getName());
                    player.sendMessage("§aSpawn 1 of §e" + arena.getName() + " §aset.");
                });
            }

            case "setspawn2" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena setspawn2 <name>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                plugin.getDatabase().updateArenaSpawn(arena.getId(), 2, player.getLocation()).thenRun(() -> {
                    arena.setSpawn2(player.getLocation());
                    arena.setWorldName(player.getWorld().getName());
                    player.sendMessage("§aSpawn 2 of §e" + arena.getName() + " §aset.");
                });
            }

            case "setspawnspec" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena setspawnspec <name>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                plugin.getDatabase().updateArenaSpawn(arena.getId(), 3, player.getLocation()).thenRun(() -> {
                    arena.setSpawnSpec(player.getLocation());
                    player.sendMessage("§aSpec spawn of §e" + arena.getName() + " §aset.");
                });
            }

            case "setpos1" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena setpos1 <name>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                int[] pos = blockPos(player);
                pos1Selections.put(player.getUniqueId(), pos);
                player.sendMessage("§aPos1 set to " + pos[0] + ", " + pos[1] + ", " + pos[2]
                        + " for arena §e" + arena.getName() + "§a.");
                maybeSaveRegion(player, arena);
            }

            case "setpos2" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena setpos2 <name>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                int[] pos = blockPos(player);
                pos2Selections.put(player.getUniqueId(), pos);
                player.sendMessage("§aPos2 set to " + pos[0] + ", " + pos[1] + ", " + pos[2]
                        + " for arena §e" + arena.getName() + "§a.");
                maybeSaveRegion(player, arena);
            }

            case "bind" -> {
                if (args.length < 3) { player.sendMessage("§cUsage: /aowarena bind <arena> <gamemode>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                String gamemode = args[2].toLowerCase();
                if (plugin.getGamemodeManager().getGamemode(gamemode) == null) {
                    player.sendMessage("§cUnknown gamemode: §e" + gamemode + "§c. Check gamemodes.yml.");
                    return true;
                }
                plugin.getDatabase().bindArena(arena.getId(), gamemode).thenRun(() -> {
                    arena.addGamemodeBind(gamemode);
                    player.sendMessage("§aArena §e" + arena.getName()
                            + " §abound to gamemode §e" + gamemode + "§a.");
                });
            }

            case "dupe" -> {
                if (args.length < 3) { player.sendMessage("§cUsage: /aowarena dupe <name> <count>"); return true; }
                Arena source = requireArena(player, args[1]);
                if (source == null) return true;
                int count;
                try {
                    count = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§c<count> must be an integer.");
                    return true;
                }
                if (count < 1 || count > 500) {
                    player.sendMessage("§c<count> must be between 1 and 500.");
                    return true;
                }
                doDupe(player, source, count);
            }

            case "buildarenas" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena buildarenas <world>"); return true; }
                String worldName = args[1];
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    player.sendMessage("§cWorld §e" + worldName + " §cnot found.");
                    return true;
                }
                player.sendMessage("§eBuilding all tropical arenas in §6" + worldName + "§e... this may take a while.");
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        new ArenaBuilder(plugin, world).build();
                        player.sendMessage("§aAll arenas built successfully in §e" + worldName + "§a.");
                    } catch (Exception e) {
                        player.sendMessage("§cArena build failed: " + e.getMessage());
                        plugin.getLogger().severe("ArenaBuilder error: " + e.getMessage());
                    }
                });
            }

            case "save" -> {
                if (args.length < 2) { player.sendMessage("§cUsage: /aowarena save <name>"); return true; }
                Arena arena = requireArena(player, args[1]);
                if (arena == null) return true;
                player.sendMessage("§eSaving schematic for §6" + arena.getName() + "§e...");
                plugin.getArenaManager().saveArenaSchematic(arena).thenRun(() ->
                        player.sendMessage("§aSchematic for §e" + arena.getName() + " §asaved."));
            }

            default -> sendUsage(player);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Arena requireArena(Player player, String name) {
        Arena arena = plugin.getArenaManager().getArenaByName(name);
        if (arena == null) {
            player.sendMessage("§cArena §e" + name + " §cnot found. Use /aowarena create first.");
        }
        return arena;
    }

    private int[] blockPos(Player player) {
        return new int[]{
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
        };
    }

    /**
     * If both pos1 and pos2 are recorded for this player, persists the region to the DB
     * and updates the Arena object in memory.
     */
    private void maybeSaveRegion(Player player, Arena arena) {
        UUID uuid = player.getUniqueId();
        int[] p1 = pos1Selections.get(uuid);
        int[] p2 = pos2Selections.get(uuid);
        if (p1 == null || p2 == null) return;

        // Normalise min/max
        int x1 = Math.min(p1[0], p2[0]);
        int y1 = Math.min(p1[1], p2[1]);
        int z1 = Math.min(p1[2], p2[2]);
        int x2 = Math.max(p1[0], p2[0]);
        int y2 = Math.max(p1[1], p2[1]);
        int z2 = Math.max(p1[2], p2[2]);

        plugin.getDatabase().updateArenaRegion(arena.getId(), x1, y1, z1, x2, y2, z2).thenRun(() -> {
            arena.setRegionX1(x1); arena.setRegionY1(y1); arena.setRegionZ1(z1);
            arena.setRegionX2(x2); arena.setRegionY2(y2); arena.setRegionZ2(z2);
            player.sendMessage("§aRegion for arena §e" + arena.getName() + " §asaved.");
        });
    }

    /**
     * Duplicates {@code source} arena {@code count} times.
     * Each duplicate is offset +125 blocks on the X axis from the previous arena's region X1.
     * Names follow the pattern: <originalName>_2, _3, etc.
     * The dupe_group is set to the original arena name.
     */
    private void doDupe(Player player, Arena source, int count) {
        String baseName = source.getName();
        int offsetX = 125;

        // Run the whole operation asynchronously; DB calls are already async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int successCount = 0;
            for (int i = 0; i < count; i++) {
                int dupeIndex = i + 2; // start naming from _2
                String dupeName = baseName + "_" + dupeIndex;

                // Compute offset: each dupe is offsetX further on X than the previous
                int xShift = offsetX * (i + 1);

                try {
                    Integer id = plugin.getDatabase().createArena(dupeName).get();
                    if (id == null || id == -1) {
                        player.sendMessage("§cFailed to create dupe arena §e" + dupeName + "§c (may already exist).");
                        continue;
                    }

                    // Build a new Arena reflecting the duplicated DB entry
                    Arena dupe = new Arena(id, dupeName);
                    dupe.setDupeGroup(baseName);
                    dupe.setWorldName(source.getWorldName());

                    // Shift spawn points
                    if (source.getSpawn1() != null) {
                        org.bukkit.Location s1 = source.getSpawn1().clone();
                        s1.setX(s1.getX() + xShift);
                        plugin.getDatabase().updateArenaSpawn(id, 1, s1).get();
                        dupe.setSpawn1(s1);
                    }
                    if (source.getSpawn2() != null) {
                        org.bukkit.Location s2 = source.getSpawn2().clone();
                        s2.setX(s2.getX() + xShift);
                        plugin.getDatabase().updateArenaSpawn(id, 2, s2).get();
                        dupe.setSpawn2(s2);
                    }
                    if (source.getSpawnSpec() != null) {
                        org.bukkit.Location ss = source.getSpawnSpec().clone();
                        ss.setX(ss.getX() + xShift);
                        plugin.getDatabase().updateArenaSpawn(id, 3, ss).get();
                        dupe.setSpawnSpec(ss);
                    }

                    // Shift region corners
                    int rx1 = source.getRegionX1() + xShift;
                    int rx2 = source.getRegionX2() + xShift;
                    plugin.getDatabase().updateArenaRegion(id,
                            rx1, source.getRegionY1(), source.getRegionZ1(),
                            rx2, source.getRegionY2(), source.getRegionZ2()).get();
                    dupe.setRegionX1(rx1); dupe.setRegionY1(source.getRegionY1()); dupe.setRegionZ1(source.getRegionZ1());
                    dupe.setRegionX2(rx2); dupe.setRegionY2(source.getRegionY2()); dupe.setRegionZ2(source.getRegionZ2());

                    // Copy gamemode binds
                    for (String gm : source.getBoundGamemodes()) {
                        plugin.getDatabase().bindArena(id, gm).get();
                        dupe.addGamemodeBind(gm);
                    }

                    // Set dupe_group in DB via a direct update
                    plugin.getDatabase().executeAsync(conn -> {
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                "UPDATE lp_arenas SET dupe_group=? WHERE id=?")) {
                            ps.setString(1, baseName);
                            ps.setInt(2, id);
                            ps.executeUpdate();
                        } catch (java.sql.SQLException e) {
                            plugin.getLogger().severe("dupe setDupeGroup: " + e.getMessage());
                        }
                    }).get();

                    successCount++;
                } catch (Exception e) {
                    player.sendMessage("§cError creating dupe §e" + dupeName + "§c: " + e.getMessage());
                }
            }

            int finalSuccess = successCount;
            // Reload arena cache on main thread after all dupes are created
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    plugin.getArenaManager().loadAll().thenRun(() ->
                            player.sendMessage("§aDuplicated §e" + finalSuccess
                                    + "§a arenas from §e" + baseName + "§a (dupe_group='" + baseName + "').")));
        });
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6/aowarena §ecreate §7<name>");
        player.sendMessage("§6/aowarena §esetspawn1|setspawn2|setspawnspec §7<name>");
        player.sendMessage("§6/aowarena §esetpos1|setpos2 §7<name>");
        player.sendMessage("§6/aowarena §ebind §7<arena> <gamemode>");
        player.sendMessage("§6/aowarena §edupe §7<name> <count>");
        player.sendMessage("§6/aowarena §esave §7<name>");
        player.sendMessage("§6/aowarena §ebuildarenas §7<world>");
    }
}
