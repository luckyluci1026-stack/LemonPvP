package com.lemonpvp.lemonevents.game.pvp;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.*;
import org.bukkit.scheduler.BukkitRunnable;

public class TournamentGame extends AbstractGame {

    private final List<UUID> bracket = new ArrayList<>();
    // current match: index into bracket (two fighters at a time)
    private int matchIndex = 0;
    private UUID currentFighter1;
    private UUID currentFighter2;

    public TournamentGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        bracket.addAll(participants);
        Collections.shuffle(bracket);

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>PvP Tournament</gradient></bold> " +
            "<green>starting! " + bracket.size() + " participants.</green>"));

        displayBracket();
        scheduleTask(Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 100L));
    }

    private void displayBracket() {
        Component header = MM.deserialize("<gold><bold>── Tournament Bracket ──</bold></gold>");
        broadcastParticipants(header);
        for (int i = 0; i < bracket.size(); i += 2) {
            String p1 = getPlayerName(bracket.get(i));
            String p2 = i + 1 < bracket.size() ? getPlayerName(bracket.get(i + 1)) : "<bye>";
            broadcastParticipants(MM.deserialize(
                "<gray>" + (i / 2 + 1) + ". <yellow>" + p1 + "</yellow> vs <yellow>" + p2 + "</yellow>"));
        }
    }

    private void startNextMatch() {
        if (matchIndex + 1 >= bracket.size()) {
            // Only one player left = winner
            finishOrder.add(0, bracket.get(matchIndex));
            Collections.reverse(finishOrder);
            endGame();
            return;
        }

        currentFighter1 = bracket.get(matchIndex);
        currentFighter2 = bracket.get(matchIndex + 1);

        Player p1 = Bukkit.getPlayer(currentFighter1);
        Player p2 = Bukkit.getPlayer(currentFighter2);

        String name1 = getPlayerName(currentFighter1);
        String name2 = getPlayerName(currentFighter2);

        broadcastParticipants(MM.deserialize(
            "<red>⚔ Match: <yellow>" + name1 + "</yellow> vs <yellow>" + name2 + "</yellow>"));

        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (p1 != null && world != null) {
            p1.teleport(new Location(world, -5.5, 65, 0.5));
            p1.setGameMode(GameMode.SURVIVAL);
            p1.getInventory().clear();
            setupKit(p1);
        }
        if (p2 != null && world != null) {
            p2.teleport(new Location(world, 5.5, 65, 0.5));
            p2.setGameMode(GameMode.SURVIVAL);
            p2.getInventory().clear();
            setupKit(p2);
        }

        // Countdown
        scheduleTask(new BukkitRunnable() {
            int c = 5;
            @Override public void run() {
                if (c <= 0) { if (p1 != null) p1.setWalkSpeed(0.2f); if (p2 != null) p2.setWalkSpeed(0.2f); cancel(); return; }
                if (p1 != null) p1.setWalkSpeed(0); if (p2 != null) p2.setWalkSpeed(0);
                broadcastParticipants(MM.deserialize("<yellow>Fight starts in <gold>" + c + "</gold>..."));
                c--;
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void setupKit(Player player) {
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.COOKED_BEEF, 8));
        player.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.IRON_HELMET));
        player.getEquipment().setChestplate(new org.bukkit.inventory.ItemStack(Material.IRON_CHESTPLATE));
        player.getEquipment().setLeggings(new org.bukkit.inventory.ItemStack(Material.IRON_LEGGINGS));
        player.getEquipment().setBoots(new org.bukkit.inventory.ItemStack(Material.IRON_BOOTS));
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    /** Called by EventPlayerListener when a fighter dies. */
    public void onFighterDeath(Player loser) {
        UUID loserId = loser.getUniqueId();
        if (!loserId.equals(currentFighter1) && !loserId.equals(currentFighter2)) return;

        UUID winnerId = loserId.equals(currentFighter1) ? currentFighter2 : currentFighter1;
        String winnerName = getPlayerName(winnerId);
        String loserName = getPlayerName(loserId);

        broadcastParticipants(MM.deserialize(
            "<green>✔ <yellow>" + winnerName + "</yellow> defeated <yellow>" + loserName + "</yellow>!"));

        finishOrder.add(0, loserId); // loser gets current placement
        bracket.set(matchIndex, winnerId); // winner stays in bracket at same slot
        // Remove the second slot used by this match
        if (matchIndex + 1 < bracket.size()) bracket.remove(matchIndex + 1);

        loser.setGameMode(GameMode.SPECTATOR);

        // Next match after delay
        matchIndex = 0; // restart from top with remaining players
        scheduleTask(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            displayBracket();
            startNextMatch();
        }, 100L));
    }

    @Override
    protected void checkWinCondition() {
        // Tournament handles win condition internally via onFighterDeath
    }

    private String getPlayerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    @Override
    protected void doCleanup() {}
}
