package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelGame;
import com.lemonpvp.lemonpractice.duel.DuelState;
import com.lemonpvp.lemonpractice.model.Arena;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private final LemonPractice plugin;
    /** Both participant UUIDs map to the same DuelGame instance */
    private final Map<UUID, DuelGame> activeDuels = new ConcurrentHashMap<>();

    public DuelManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Start duel
    // -----------------------------------------------------------------------

    public void startDuel(UUID p1UUID, UUID p2UUID, String gamemode) {
        Player p1 = Bukkit.getPlayer(p1UUID);
        Player p2 = Bukkit.getPlayer(p2UUID);

        // Find a free arena
        Optional<Arena> arenaOpt = plugin.getArenaManager().getFreeArenaForGamemode(gamemode);
        if (arenaOpt.isEmpty()) {
            plugin.getLogger().warning("[DuelManager] No free arena for gamemode '" + gamemode
                    + "'. Sending players back to lobby.");
            sendToLobby(p1);
            sendToLobby(p2);
            return;
        }

        Arena arena = arenaOpt.get();

        // Verify both players are online; if not, free up and abort
        if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
            plugin.getLogger().warning("[DuelManager] One or both players offline when starting duel.");
            sendToLobby(p1);
            sendToLobby(p2);
            return;
        }

        // Create game
        DuelGame game = new DuelGame(p1UUID, p1.getName(), p2UUID, p2.getName(), gamemode, arena);
        game.setState(DuelState.WAITING);
        game.setStartTime(System.currentTimeMillis());

        // Mark arena in-use
        plugin.getArenaManager().markInUse(arena, true);

        // Register in active duels
        activeDuels.put(p1UUID, game);
        activeDuels.put(p2UUID, game);

        // Prepare players
        preparePlayer(p1);
        preparePlayer(p2);

        // Apply kits
        applyKit(p1, gamemode);
        applyKit(p2, gamemode);

        // Teleport — 2 blocks above actual spawn Y
        Location spawn1 = raiseY(arena.getSpawn1(), 2);
        Location spawn2 = raiseY(arena.getSpawn2(), 2);
        p1.teleport(spawn1);
        p2.teleport(spawn2);

        // Start countdown (transitions state to COUNTDOWN then FIGHTING)
        startCountdown(game, p1, p2);

        plugin.getLogger().info("[DuelManager] Started duel: " + p1.getName() + " vs " + p2.getName()
                + " | gamemode=" + gamemode + " | arena=" + arena.getName());
    }

    // -----------------------------------------------------------------------
    // Countdown
    // -----------------------------------------------------------------------

    private void startCountdown(DuelGame game, Player p1, Player p2) {
        game.setState(DuelState.COUNTDOWN);
        final int[] countdown = {3};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (countdown[0] <= 0) {
                task.cancel();
                game.setState(DuelState.FIGHTING);
                game.setStartTime(System.currentTimeMillis());

                Title fightTitle = Title.title(
                        Component.text("Fight!", NamedTextColor.GREEN),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(800), Duration.ofMillis(200)));

                if (p1.isOnline()) p1.showTitle(fightTitle);
                if (p2.isOnline()) p2.showTitle(fightTitle);
                return;
            }

            Title countTitle = Title.title(
                    Component.text(String.valueOf(countdown[0]), NamedTextColor.YELLOW),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(100)));

            if (p1.isOnline()) p1.showTitle(countTitle);
            if (p2.isOnline()) p2.showTitle(countTitle);
            countdown[0]--;
        }, 0L, 20L);
    }

    // -----------------------------------------------------------------------
    // Handle death
    // -----------------------------------------------------------------------

    public void handleDeath(UUID loserUuid) {
        DuelGame game = activeDuels.get(loserUuid);
        if (game == null || game.getState() != DuelState.FIGHTING) return;

        game.setState(DuelState.ENDING);

        UUID winnerUuid = game.getOpponent(loserUuid);
        if (winnerUuid == null) return;

        game.setWinnerUuid(winnerUuid);

        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);

        // Apply ELO — result is internal only, never displayed to players
        plugin.getEloManager().applyDuelResult(winnerUuid, loserUuid, game.getGamemode())
                .thenAccept(changes -> {
                    game.setEloChangeP1(game.getPlayer1Uuid().equals(winnerUuid) ? changes[0] : changes[1]);
                    game.setEloChangeP2(game.getPlayer2Uuid().equals(loserUuid)  ? changes[1] : changes[0]);

                    // Save duel record async
                    plugin.getDatabase().saveDuelRecord(
                            game.getPlayer1Uuid(), game.getPlayer2Uuid(),
                            game.getGamemode(), winnerUuid,
                            game.getEloChangeP1(), game.getEloChangeP2(),
                            game.getDurationSeconds());

                    // Switch back to main thread for Bukkit API calls
                    Bukkit.getScheduler().runTask(plugin, () ->
                            finishDuel(game, winner, loser));
                });
    }

    private void finishDuel(DuelGame game, Player winner, Player loser) {
        // Victory title for winner — ELO is internal only, not shown
        if (winner != null && winner.isOnline()) {
            winner.showTitle(Title.title(
                    Component.text("Victory!", NamedTextColor.GOLD),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));

            // 2 seconds of Absorption (invincibility flavour)
            winner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 4, false, false));
        }

        // Defeat title for loser + make spectator immediately
        if (loser != null && loser.isOnline()) {
            loser.showTitle(Title.title(
                    Component.text("Defeat!", NamedTextColor.RED),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));

            Arena arena = game.getArena();
            Location specSpawn = arena.getSpawnSpec() != null ? arena.getSpawnSpec() : arena.getSpawn1();
            plugin.getSpectatorManager().makeSpectator(loser, specSpawn);
        }

        // After 3 seconds: send both to lobby, free arena, reset
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (winner != null && winner.isOnline()) {
                plugin.getSpectatorManager().removeSpectator(winner);
                sendToLobby(winner);
            }
            if (loser != null && loser.isOnline()) {
                plugin.getSpectatorManager().removeSpectator(loser);
                sendToLobby(loser);
            }

            // Clean up
            activeDuels.remove(game.getPlayer1Uuid());
            activeDuels.remove(game.getPlayer2Uuid());
            plugin.getArenaManager().markInUse(game.getArena(), false);
            plugin.getArenaManager().resetArena(game.getArena());
        }, 60L); // 3 seconds = 60 ticks
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public DuelGame getDuel(UUID playerUuid) {
        return activeDuels.get(playerUuid);
    }

    public boolean isInDuel(UUID playerUuid) {
        return activeDuels.containsKey(playerUuid);
    }

    // -----------------------------------------------------------------------
    // Shutdown
    // -----------------------------------------------------------------------

    public void endAllDuels() {
        Set<DuelGame> unique = new HashSet<>(activeDuels.values());
        for (DuelGame game : unique) {
            Player p1 = Bukkit.getPlayer(game.getPlayer1Uuid());
            Player p2 = Bukkit.getPlayer(game.getPlayer2Uuid());
            sendToLobby(p1);
            sendToLobby(p2);
            plugin.getArenaManager().markInUse(game.getArena(), false);
        }
        activeDuels.clear();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void preparePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setExp(0);
        player.setLevel(0);
    }

    private void applyKit(Player player, String gamemode) {
        if (player == null || !player.isOnline()) return;
        PlayerKit kit = plugin.getKitManager().getEffectiveKit(player.getUniqueId(), gamemode);
        if (kit == null) return;
        kit.getSlots().forEach((slot, item) -> player.getInventory().setItem(slot, item));
    }

    private void sendToLobby(Player player) {
        if (player == null || !player.isOnline()) return;
        String lobbyServer = plugin.getServersConfig().getString("servers.lobby.name", "lobby");
        plugin.getVelocityMessaging().sendToServer(player, lobbyServer);
    }

    private Location raiseY(Location loc, double amount) {
        if (loc == null) return null;
        return loc.clone().add(0, amount, 0);
    }
}
