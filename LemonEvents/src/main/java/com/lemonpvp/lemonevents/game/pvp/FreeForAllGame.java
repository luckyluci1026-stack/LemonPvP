package com.lemonpvp.lemonevents.game.pvp;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FreeForAllGame extends AbstractGame {

    private final Map<UUID, Integer> kills = new LinkedHashMap<>();
    private int timeLeft;
    private Scoreboard scoreboard;
    private Objective objective;

    public FreeForAllGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        timeLeft = plugin.getEventsConfig().getInt("pvp.PVP_FreeForAll.game-duration", 300);
        participants.forEach(uuid -> kills.put(uuid, 0));

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>PvP FFA</gradient></bold> " +
            "<green>starting! Most kills in <yellow>" + (timeLeft / 60) + " minutes</yellow> wins!</green>"));

        spawnPlayers();
        setupScoreboard();

        // Timer
        scheduleTask(new BukkitRunnable() {
            @Override public void run() {
                if (!running) { cancel(); return; }
                timeLeft--;
                updateScoreboard();
                if (timeLeft <= 0) {
                    declareWinner();
                    cancel();
                }
                if (timeLeft == 60) {
                    broadcastParticipants(MM.deserialize("<yellow>1 minute remaining!"));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    private void spawnPlayers() {
        new ArrayList<>(participants).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) spawnPlayer(p);
        });
    }

    private void spawnPlayer(Player p) {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;
        Random rng = new Random();
        double angle = rng.nextDouble() * 2 * Math.PI;
        double radius = 15 + rng.nextInt(10);
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        p.teleport(new Location(world, x, 65, z));
        p.setGameMode(GameMode.SURVIVAL);
        setupKit(p);
    }

    private void setupKit(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));
        player.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    private void setupScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("ffa_kills", Criteria.DUMMY,
                MM.deserialize("<gold><bold>PvP FFA - Kills</bold></gold>"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        updateScoreboard();
        participants.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setScoreboard(scoreboard);
        });
    }

    private void updateScoreboard() {
        if (objective == null) return;
        kills.forEach((uuid, k) -> {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) objective.getScore(name).setScore(k);
        });
    }

    public void onPlayerKill(Player killer, Player victim) {
        kills.merge(killer.getUniqueId(), 1, Integer::sum);
        broadcastParticipants(MM.deserialize(
            "<gray>" + victim.getName() + " was killed by <yellow>" + killer.getName() +
            "</yellow> <gold>(" + kills.getOrDefault(killer.getUniqueId(), 0) + " kills)</gold>"));
        UUID victimUuid = victim.getUniqueId();
        // Force-respawn after 3 seconds (60 ticks) and teleport to a random spawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!running || !participants.contains(victimUuid)) return;
            Player p = Bukkit.getPlayer(victimUuid);
            if (p == null) return;
            if (p.isDead()) p.spigot().respawn();
            // Apply kit and teleport on the next tick after the respawn cycle completes
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player fresh = Bukkit.getPlayer(victimUuid);
                if (fresh != null && running && participants.contains(victimUuid)) {
                    spawnPlayer(fresh);
                }
            });
        }, 60L);
    }

    private void declareWinner() {
        // Determine winner only from still-active participants
        UUID winner = participants.stream()
                .max(Comparator.comparingInt(u -> kills.getOrDefault(u, 0)))
                .orElse(null);

        // Players who disconnected mid-game are already in finishOrder (via handleQuit).
        // Save them and rebuild the list so reverse() doesn't corrupt their positions.
        List<UUID> disconnected = new ArrayList<>(finishOrder);
        finishOrder.clear();

        if (winner != null) {
            participants.remove(winner);
            finishOrder.add(0, winner); // winner at front
            participants.stream()
                    .sorted(Comparator.comparingInt((UUID u) -> kills.getOrDefault(u, 0)).reversed())
                    .forEach(u -> finishOrder.add(0, u));
            Collections.reverse(finishOrder); // [winner, 2nd, 3rd, ...]
        }
        participants.clear();

        // Disconnected players rank below active participants; earlier-quit = further back
        finishOrder.addAll(disconnected);
        endGame();
    }

    @Override
    protected void checkWinCondition() {} // timer-based

    @Override
    protected void doCleanup() {
        participants.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        });
    }
}
