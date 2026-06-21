package com.lemonpvp.lemonevents.game.hungergames;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import org.bukkit.scheduler.BukkitRunnable;

public class HungerGamesGame extends AbstractGame {

    private static final String[] MAP_IDS = {"HG_Classic", "HG_Tropic", "HG_Winter"};

    private String selectedMap;

    public HungerGamesGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        selectedMap = MAP_IDS[new Random().nextInt(MAP_IDS.length)];

        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>HungerGames</gradient></bold> " +
            "<green>starting on <yellow>" + selectedMap + "</yellow>!</green>"));

        spawnPlayers(world);

        // 10-second frozen countdown
        final boolean[] frozen = {true};
        // Freeze players
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setWalkSpeed(0f);
        }

        scheduleTask(new BukkitRunnable() {
            int countdown = 10;
            @Override public void run() {
                if (countdown <= 0) {
                    frozen[0] = false;
                    // Unfreeze
                    for (UUID uuid : participants) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.setWalkSpeed(0.2f);
                    }
                    broadcastParticipants(MM.deserialize("<bold><red>GO!</red></bold>"));
                    startBorderShrink(world);
                    cancel();
                    return;
                }
                broadcastParticipants(MM.deserialize("<yellow>Game starts in <gold>" + countdown + "</gold>..."));
                // Title countdown
                for (UUID uuid : participants) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.showTitle(net.kyori.adventure.title.Title.title(
                        MM.deserialize("<gold>" + countdown),
                        MM.deserialize("<gray>Get ready..."),
                        net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(0),
                            java.time.Duration.ofMillis(1100),
                            java.time.Duration.ofMillis(0))));
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void spawnPlayers(World world) {
        List<?> rawSpawns = plugin.getEventsConfig()
                .getList("hunger-games.maps." + selectedMap + ".spawnpoints");
        List<Map<?, ?>> spawnpoints = new ArrayList<>();
        if (rawSpawns != null) for (Object o : rawSpawns) if (o instanceof Map<?, ?> m) spawnpoints.add(m);
        if (spawnpoints.isEmpty()) spawnpoints.add(Map.of("x", 20, "y", 65, "z", 0));

        List<UUID> players = new ArrayList<>(participants);
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            Player p = Bukkit.getPlayer(players.get(i));
            if (p == null) continue;
            Map<?, ?> sp = spawnpoints.get(i % spawnpoints.size());
            try {
                int x = ((Number) sp.get("x")).intValue();
                int y = ((Number) sp.get("y")).intValue();
                int z = ((Number) sp.get("z")).intValue();
                p.teleport(new Location(world, x + 0.5, y, z + 0.5));
            } catch (ClassCastException | NullPointerException e) {
                plugin.getLogger().warning("[HungerGames] Skipping malformed spawnpoint in events.yml: " + sp);
                p.teleport(new Location(world, 20.5, 65, 0.5));
            }
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
        }
    }

    private void startBorderShrink(World world) {
        WorldBorder border = world.getWorldBorder();
        int mapSize = plugin.getEventsConfig().getInt(
                "hunger-games.maps." + selectedMap + ".size", 200);
        border.setSize(mapSize);
        border.setCenter(world.getSpawnLocation());
        // Shrink to 10×10 over 5 minutes
        border.setSize(10, 300);

        broadcastParticipants(MM.deserialize(
            "<gray>The border is now shrinking. Stay inside!"));
    }

    @Override
    protected void onEliminated(Player player, Player killer) {
        if (killer != null) {
            broadcastAll(MM.deserialize(
                "<gray>" + player.getName() + " was eliminated by <yellow>" + killer.getName() + "</yellow>."));
        } else {
            broadcastAll(MM.deserialize("<gray>" + player.getName() + " died."));
        }
        player.setGameMode(GameMode.SPECTATOR);
        int remaining = participants.size();
        if (remaining > 0) {
            broadcastParticipants(MM.deserialize(
                "<yellow>" + remaining + " player" + (remaining == 1 ? "" : "s") + " remaining."));
        }
    }

    @Override
    protected void doCleanup() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world != null) {
            world.getWorldBorder().reset();
        }
    }
}
