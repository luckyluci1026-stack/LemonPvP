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
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;
        List<UUID> players = new ArrayList<>(participants);
        Random rng = new Random();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            double angle = rng.nextDouble() * 2 * Math.PI;
            double radius = 15 + rng.nextInt(10);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            p.teleport(new Location(world, x, 65, z));
            p.setGameMode(GameMode.SURVIVAL);
            setupKit(p);
        }
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
        // Respawn victim
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (running && participants.contains(victim.getUniqueId())) {
                victim.setHealth(20);
                victim.setFoodLevel(20);
                victim.setGameMode(GameMode.SURVIVAL);
            }
        }, 60L);
    }

    private void declareWinner() {
        UUID winner = kills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (winner != null) {
            participants.remove(winner);
            finishOrder.add(0, winner);
            // Sort remaining by kill count desc
            kills.entrySet().stream()
                    .filter(e -> !e.getKey().equals(winner))
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .forEach(e -> finishOrder.add(0, e.getKey()));
            Collections.reverse(finishOrder);
        }
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
