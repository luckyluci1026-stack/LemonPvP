package com.lemonpvp.lemonevents.game.pvp;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.*;

public class SumoGame extends AbstractGame {

    private final List<UUID> bracket = new ArrayList<>();
    private int matchIndex = 0;
    private UUID fighter1, fighter2;
    private int platformRadius;
    private int voidY;

    public SumoGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        platformRadius = plugin.getEventsConfig().getInt("pvp.PVP_Sumo.platform-radius", 8);
        voidY = plugin.getEventsConfig().getInt("pvp.PVP_Sumo.void-y", 55);

        bracket.addAll(participants);
        Collections.shuffle(bracket);

        buildPlatform();

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>Sumo</gradient></bold> " +
            "<green>starting! Single elimination bracket. " + bracket.size() + " players.</green>"));

        // Spectate non-fighters
        for (UUID uuid : bracket) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setGameMode(GameMode.SPECTATOR);
        }

        scheduleTask(Bukkit.getScheduler().runTaskLater(plugin, this::startNextMatch, 60L));
    }

    private void buildPlatform() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (world == null) return;
        int centerX = 0, centerY = 60, centerZ = 0;
        for (int x = -platformRadius; x <= platformRadius; x++) {
            for (int z = -platformRadius; z <= platformRadius; z++) {
                if (x * x + z * z <= platformRadius * platformRadius) {
                    world.getBlockAt(centerX + x, centerY, centerZ + z).setType(Material.QUARTZ_BLOCK);
                }
            }
        }
    }

    private void startNextMatch() {
        if (bracket.size() == 1) {
            UUID winner = bracket.get(0);
            bracket.remove(winner);
            participants.remove(winner);
            finishOrder.add(0, winner);
            Collections.reverse(finishOrder);
            endGame();
            return;
        }

        fighter1 = bracket.get(matchIndex);
        fighter2 = bracket.get(matchIndex + 1);

        String n1 = getPlayerName(fighter1), n2 = getPlayerName(fighter2);
        broadcastParticipants(MM.deserialize(
            "<yellow>⚔ Sumo Match: <white>" + n1 + "</white> vs <white>" + n2 + "</white>!"));

        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        Player p1 = Bukkit.getPlayer(fighter1);
        Player p2 = Bukkit.getPlayer(fighter2);
        if (world != null) {
            if (p1 != null) { p1.setGameMode(GameMode.SURVIVAL); p1.getInventory().clear(); p1.teleport(new Location(world, -3.5, 61, 0.5)); p1.setHealth(20); }
            if (p2 != null) { p2.setGameMode(GameMode.SURVIVAL); p2.getInventory().clear(); p2.teleport(new Location(world, 3.5, 61, 0.5)); p2.setHealth(20); }
        }

        // Check for void falls every tick
        scheduleTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running) return;
            for (UUID uuid : List.of(fighter1, fighter2)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getLocation().getY() < voidY) {
                    onFellOff(p);
                }
            }
        }, 0L, 2L));
    }

    private void onFellOff(Player loser) {
        UUID loserId = loser.getUniqueId();
        UUID winnerId = loserId.equals(fighter1) ? fighter2 : fighter1;

        broadcastParticipants(MM.deserialize(
            "<yellow>" + loser.getName() + " fell off! <green>" + getPlayerName(winnerId) + " wins the match!"));
        finishOrder.add(0, loserId);
        bracket.remove(loserId);
        participants.remove(loserId);
        loser.setGameMode(GameMode.SPECTATOR);

        // Teleport winner back to platform
        Player winner = Bukkit.getPlayer(winnerId);
        World world = Bukkit.getWorld(plugin.getConfig().getString("events-world", "world"));
        if (winner != null && world != null) winner.teleport(new Location(world, 0.5, 61, 0.5));

        matchIndex = 0;
        scheduleTask(Bukkit.getScheduler().runTaskLater(plugin, () -> startNextMatch(), 80L));
    }

    @Override
    protected void checkWinCondition() {} // handled by onFellOff

    private String getPlayerName(UUID uuid) {
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n != null ? n : uuid.toString().substring(0, 8);
    }

    @Override
    protected void doCleanup() {
        // Platform stays, admin can restore map
    }
}
