package com.lemonpvp.lemonevents.game.lemonroyale;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import com.lemonpvp.lemonevents.model.LootRarity;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class LemonRoyaleGame extends AbstractGame {

    // LemonRoyale maps cycle: pick random one each time
    private static final String[] MAP_IDS = {
        "LR_Tropic_1", "LR_Desert_2", "LR_Frost_3", "LR_Nether_4", "LR_Ancient_5"
    };

    private String selectedMap;
    private CorruptionZone corruptionZone;
    private final Map<Location, Boolean> lootChests = new HashMap<>(); // loc → opened

    public LemonRoyaleGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        selectedMap = MAP_IDS[new Random().nextInt(MAP_IDS.length)];

        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) {
            plugin.getLogger().warning("Events world not found!");
            return;
        }

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>LemonRoyale</gradient></bold> " +
            "<green>starting on map <yellow>" + selectedMap + "</yellow>!</green>"));

        // 10-second countdown
        scheduleTask(new BukkitRunnable() {
            int countdown = 10;
            @Override public void run() {
                if (countdown <= 0) { spawnPlayersAndBegin(world); cancel(); return; }
                broadcastParticipants(MM.deserialize("<yellow>Starting in <gold>" + countdown + "</gold>..."));
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void spawnPlayersAndBegin(World world) {
        // Teleport players to random spawnpoints
        List<Map<?, ?>> spawnList = getSpawnpoints(selectedMap);
        List<UUID> playerList = new ArrayList<>(participants);
        Collections.shuffle(playerList);

        for (int i = 0; i < playerList.size(); i++) {
            Player p = Bukkit.getPlayer(playerList.get(i));
            if (p == null) continue;
            Map<?, ?> sp = spawnList.get(i % spawnList.size());
            try {
                int x = ((Number) sp.get("x")).intValue();
                int y = ((Number) sp.get("y")).intValue();
                int z = ((Number) sp.get("z")).intValue();
                p.teleport(new Location(world, x + 0.5, y, z + 0.5));
            } catch (ClassCastException | NullPointerException e) {
                plugin.getLogger().warning("[LemonRoyale] Skipping malformed spawnpoint in events.yml: " + sp);
                p.teleport(new Location(world, 0.5, 65, 0.5));
            }
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
        }

        // Spawn loot chests
        spawnLootChests(world);

        // Set up corruption zone — starts shrinking after 60 seconds
        ConfigurationSection mapCfg = plugin.getEventsConfig()
                .getConfigurationSection("lemon-royale.maps." + selectedMap);
        int mapSize = mapCfg != null ? mapCfg.getInt("size", 300) : 300;
        Location center = new Location(world, 0, 64, 0);
        corruptionZone = new CorruptionZone(plugin, world, center,
                mapSize / 2.0, 15.0, 0.3, participants);
        corruptionZone.start(60 * 20L); // start after 60 seconds

        broadcastParticipants(MM.deserialize(
            "<green>The battle begins! <red>Last player standing wins!</red></green>"));
        broadcastParticipants(MM.deserialize(
            "<gray>The Corruption will start closing in after <yellow>60 seconds</yellow>."));
    }

    private void spawnLootChests(World world) {
        List<?> positions = plugin.getLootConfig().getList("lr-chest-positions." + selectedMap);
        if (positions == null) return;

        for (Object raw : positions) {
            if (!(raw instanceof Map<?, ?> pos)) continue;
            try {
                int x = ((Number) pos.get("x")).intValue();
                int y = ((Number) pos.get("y")).intValue();
                int z = ((Number) pos.get("z")).intValue();
                Location loc = new Location(world, x, y, z);
                world.getBlockAt(loc).setType(Material.CHEST);
                lootChests.put(loc, false);
            } catch (ClassCastException | NullPointerException e) {
                plugin.getLogger().warning("[LemonRoyale] Skipping malformed chest position in lootTables.yml: " + pos);
            }
        }
    }

    /** Called by EventPlayerListener when a chest is opened. */
    public void onChestOpen(Player player, Location loc) {
        if (!lootChests.containsKey(loc) || lootChests.get(loc)) return;
        lootChests.put(loc, true);

        // Fill chest with random loot
        if (!(loc.getBlock().getState() instanceof org.bukkit.block.Chest chest)) return;
        chest.getInventory().clear();
        int itemCount = 3 + new Random().nextInt(4);
        for (int i = 0; i < itemCount; i++) {
            ItemStack item = plugin.getLootManager().rollLoot();
            chest.getInventory().addItem(item);
        }
        player.sendActionBar(MM.deserialize("<green>Chest opened!"));
    }

    // ── Custom Item Effects ────────────────────────────────────────────────────

    /** Venom Bomb (CMD 7001): Wither II, radius 3, 3 seconds. */
    public static void applyVenomBomb(Location loc, Player thrower) {
        World world = loc.getWorld();
        if (world == null) return;
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof Player target && !target.equals(thrower)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
            }
        }
        world.spawnParticle(Particle.SMOKE, loc, 40, 1, 1, 1, 0.05);
    }

    /** Citrus Shield (CMD 7002): Absorption III, 20 seconds. */
    public static void applyCitrusShield(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 2));
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
            "<yellow>🛡 Citrus Shield activated! Absorption III for 20 seconds."));
    }

    /** Grove Salve (CMD 7003): Regeneration III, 5 seconds (2s use time). */
    public static void applyGroveSalve(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2));
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
            "<green>🌿 Grove Salve applied! Regeneration III for 5 seconds."));
    }

    /** Lemon Drop (CMD 7004): LEGENDARY loot crate drops after 10 seconds. */
    public static void activateLemonDrop(Player player, Location targetLoc, LemonEvents plugin) {
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
            "<gold>🍋 Lemon Drop incoming in 10 seconds!"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World dropWorld = targetLoc.getWorld();
            if (dropWorld == null) return;
            dropWorld.spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc, 50, 1, 1, 1, 0.1);
            dropWorld.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
            targetLoc.getBlock().setType(Material.CHEST);
            if (!(targetLoc.getBlock().getState() instanceof org.bukkit.block.Chest chest)) return;
            for (int i = 0; i < 5; i++) {
                ItemStack item = plugin.getLootManager().rollLootOfRarity(LootRarity.LEGENDARY);
                chest.getInventory().addItem(item);
            }
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                "<gold>★ LEGENDARY Lemon Drop landed at <yellow>" +
                (int) targetLoc.getX() + ", " + (int) targetLoc.getZ() + "</yellow>! ★"));
        }, 200L); // 10 seconds
    }

    private List<Map<?, ?>> getSpawnpoints(String mapId) {
        List<?> raw = plugin.getEventsConfig()
                .getList("lemon-royale.maps." + mapId + ".spawnpoints");
        List<Map<?, ?>> result = new ArrayList<>();
        if (raw != null) for (Object o : raw) if (o instanceof Map<?, ?> m) result.add(m);
        if (result.isEmpty()) result.add(Map.of("x", 0, "y", 65, "z", 0));
        return result;
    }

    @Override
    protected void onEliminated(Player player, Player killer) {
        if (killer != null) {
            broadcastAll(MM.deserialize(
                "<gray>" + player.getName() + " was eliminated by <yellow>" + killer.getName() + "</yellow>."));
        } else {
            broadcastAll(MM.deserialize("<gray>" + player.getName() + " was eliminated."));
        }
        player.sendMessage(MM.deserialize("<red>You have been eliminated from LemonRoyale!"));
        player.setGameMode(GameMode.SPECTATOR);
        int remaining = participants.size();
        if (remaining > 0) {
            broadcastParticipants(MM.deserialize(
                "<yellow>" + remaining + " player" + (remaining == 1 ? "" : "s") + " remaining."));
        }
    }

    @Override
    protected void doCleanup() {
        if (corruptionZone != null) corruptionZone.stop();
        // Remove chest markers
        lootChests.clear();
    }
}
