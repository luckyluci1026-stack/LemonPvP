package com.lemonpvp.lemonevents.game.horror;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

public class EscapeGame extends AbstractGame {

    private final int labyrinthSize;
    private final int monsterCount;
    private final List<Entity> monsters = new ArrayList<>();
    private Location exitLocation;
    /** Escapers in ESCAPE ORDER — the first one out of the labyrinth places 1st. */
    private final List<UUID> escaped = new ArrayList<>();

    public EscapeGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
        this.labyrinthSize = plugin.getEventsConfig().getInt("horror.HORROR_Escape.labyrinth-size", 50);
        this.monsterCount  = plugin.getEventsConfig().getInt("horror.HORROR_Escape.monster-count", 5);
    }

    @Override
    public void startGame() {
        running = true;
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;

        broadcastParticipants(MM.deserialize(
            "<bold><dark_red>HORROR: Escape</dark_red></bold> " +
            "<red>Find the exit before the monsters catch you!</red>"));

        buildLabyrinth(world);
        spawnPlayers(world);
        applyHorrorEffects();
        spawnMonsters(world);
        scheduleJumpScares();
    }

    private void buildLabyrinth(World world) {
        // Simple hollow box labyrinth — walls of stone, floor of stone, dark interior
        int ox = 0, oy = 64, oz = 0;
        for (int x = -labyrinthSize/2; x <= labyrinthSize/2; x++) {
            for (int z = -labyrinthSize/2; z <= labyrinthSize/2; z++) {
                // Floor
                world.getBlockAt(ox + x, oy, oz + z).setType(Material.STONE);
                // Ceiling
                world.getBlockAt(ox + x, oy + 5, oz + z).setType(Material.STONE);
                // Walls
                if (Math.abs(x) == labyrinthSize/2 || Math.abs(z) == labyrinthSize/2) {
                    for (int y = 1; y <= 4; y++) {
                        world.getBlockAt(ox + x, oy + y, oz + z).setType(Material.STONE);
                    }
                } else {
                    for (int y = 1; y <= 4; y++) {
                        world.getBlockAt(ox + x, oy + y, oz + z).setType(Material.AIR);
                    }
                }
            }
        }

        // Random internal walls for maze effect
        Random rng = new Random();
        for (int i = 0; i < labyrinthSize * 2; i++) {
            int wx = ox + rng.nextInt(labyrinthSize - 4) - (labyrinthSize/2 - 2);
            int wz = oz + rng.nextInt(labyrinthSize - 4) - (labyrinthSize/2 - 2);
            int wlen = 3 + rng.nextInt(8);
            boolean horizontal = rng.nextBoolean();
            for (int d = 0; d < wlen; d++) {
                int bx = horizontal ? wx + d : wx;
                int bz = horizontal ? wz : wz + d;
                for (int y = 1; y <= 4; y++) {
                    world.getBlockAt(bx, oy + y, bz).setType(Material.STONE);
                }
            }
        }

        // Exit marker at far corner
        exitLocation = new Location(world, ox + labyrinthSize/2 - 2, oy + 1, oz + labyrinthSize/2 - 2);
        world.getBlockAt(exitLocation).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(exitLocation.clone().add(0, 1, 0)).setType(Material.AIR);
        world.getBlockAt(exitLocation.clone().add(0, 2, 0)).setType(Material.AIR);
    }

    private void spawnPlayers(World world) {
        int i = 0;
        List<UUID> players = new ArrayList<>(participants);
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.teleport(new Location(world, -labyrinthSize/2 + 2 + (i % 4), 65, -labyrinthSize/2 + 2));
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            // Give "Escape Key" item (CMD 8002) as flavor
            ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
            var meta = key.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(8002);
                meta.displayName(MM.deserialize("<red>Escape Key</red>"));
                meta.lore(List.of(MM.deserialize("<gray>Find the exit. Survive.")));
                key.setItemMeta(meta);
            }
            p.getInventory().setItem(0, key);
            i++;
        }
    }

    private void applyHorrorEffects() {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            // Blindness level 0 = subtle fog effect
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        }
    }

    private void spawnMonsters(World world) {
        Random rng = new Random();
        for (int i = 0; i < monsterCount; i++) {
            double mx = (rng.nextDouble() - 0.5) * (labyrinthSize - 6);
            double mz = (rng.nextDouble() - 0.5) * (labyrinthSize - 6);
            Zombie zombie = (Zombie) world.spawnEntity(
                    new Location(world, mx, 65, mz), EntityType.ZOMBIE);
            zombie.setCustomName("§c§lThe Corrupted");
            zombie.setCustomNameVisible(true);
            zombie.getEquipment().clear();
            zombie.setRemoveWhenFarAway(false);
            // Custom texture via CMD (CMD 8001 for name tag visual)
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            monsters.add(zombie);
        }
    }

    private void scheduleJumpScares() {
        long interval = plugin.getEventsConfig().getLong(
                "horror.HORROR_Escape.jump-scare-interval", 200);
        scheduleTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running) return;
            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                // Flash title + wither sound
                p.showTitle(Title.title(
                    MM.deserialize("<bold><dark_red>⚠</dark_red></bold>"),
                    MM.deserialize(""),
                    Title.Times.times(
                        Duration.ZERO,
                        Duration.ofMillis(200),
                        Duration.ofMillis(200))));
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            }
        }, interval, interval));
    }

    /** Called by EventPlayerListener when a player reaches the exit block. */
    public void onPlayerExit(Player player) {
        UUID uuid = player.getUniqueId();
        if (!participants.contains(uuid)) return;
        // Escaping is a WIN, not an elimination: track escape order ourselves.
        // Routing through eliminate() prepended escapers (later escaper ranked
        // higher) and its checkWinCondition crowned the last TRAPPED player.
        participants.remove(uuid);
        escaped.add(uuid);
        broadcastParticipants(MM.deserialize(
            "<green>✔ <yellow>" + player.getName() + "</yellow> escaped the labyrinth! "
            + "<gray>(#" + escaped.size() + ")"));
        // Give players back normal effects
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setGameMode(GameMode.SPECTATOR);

        if (participants.isEmpty()) {
            finalizeOrder();
            endGame();
        }
    }

    /**
     * Escape has no last-man-standing: the round ends when nobody is left
     * inside. Whoever escaped ranks by escape order; the trapped rank behind.
     */
    @Override
    protected void checkWinCondition() {
        if (participants.isEmpty()) {
            finalizeOrder();
            endGame();
        }
    }

    /** Puts escapers (escape order) ahead of everyone who died/quit inside. */
    private void finalizeOrder() {
        finishOrder.removeAll(escaped);
        for (int i = 0; i < escaped.size() && i <= finishOrder.size(); i++) {
            finishOrder.add(i, escaped.get(i));
        }
    }

    @Override
    protected void onEliminated(Player player, Player killer) {
        // Only deaths/catches route here now — escapers go through onPlayerExit.
        player.sendMessage(MM.deserialize("<red>The labyrinth got you…"));
        player.setGameMode(GameMode.SPECTATOR);
    }

    @Override
    protected void doCleanup() {
        monsters.forEach(e -> { if (e.isValid()) e.remove(); });
        monsters.clear();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }
}
