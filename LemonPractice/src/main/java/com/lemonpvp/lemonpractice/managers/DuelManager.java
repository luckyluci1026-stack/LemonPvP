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
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
            if (game.getState() != DuelState.COUNTDOWN) {
                task.cancel();
                return;
            }
            if (countdown[0] <= 0) {
                task.cancel();
                game.setState(DuelState.FIGHTING);
                game.setStartTime(System.currentTimeMillis());

                Title fightTitle = Title.title(
                        Component.text("Fight!", NamedTextColor.GREEN),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(800), Duration.ofMillis(200)));

                if (p1.isOnline()) {
                    p1.showTitle(fightTitle);
                    p1.playSound(p1.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                }
                if (p2.isOnline()) {
                    p2.showTitle(fightTitle);
                    p2.playSound(p2.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                }

                setupDuelScoreboard(game, p1, p2);
                startScoreboardUpdater(game);
                return;
            }

            Title countTitle = Title.title(
                    Component.text(String.valueOf(countdown[0]), NamedTextColor.YELLOW),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(800), Duration.ofMillis(100)));

            if (p1.isOnline()) {
                p1.showTitle(countTitle);
                p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
            }
            if (p2.isOnline()) {
                p2.showTitle(countTitle);
                p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
            }
            countdown[0]--;
        }, 0L, 20L);
    }

    // -----------------------------------------------------------------------
    // Duel Scoreboard
    // -----------------------------------------------------------------------

    private static final String[] SB_ENTRIES = {
        "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8"
    };

    private void setupDuelScoreboard(DuelGame game, Player p1, Player p2) {
        if (!p1.isOnline() || !p2.isOnline()) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("duel", Criteria.DUMMY,
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<gradient:#fffb00:#00ff00><bold>Practice</bold></gradient>"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < SB_ENTRIES.length; i++) {
            Team t = board.registerNewTeam("line" + i);
            t.addEntry(SB_ENTRIES[i]);
            obj.getScore(SB_ENTRIES[i]).setScore(SB_ENTRIES.length - 1 - i);
        }

        setLine(board, 0, "§8§m──────────────");
        setLine(board, 1, "§fvs §e" + (p1.getUniqueId().equals(game.getPlayer1Uuid()) ? game.getPlayer2Name() : game.getPlayer1Name()));
        setLine(board, 2, " ");
        setLine(board, 3, "§7Gegner HP: §c" + formatHp(getOpponent(game, p1)));
        setLine(board, 4, " ");
        setLine(board, 5, "§7Zeit: §f" + formatTime(0));
        setLine(board, 6, " ");
        setLine(board, 7, "§7Modus: §f" + capitalize(game.getGamemode()));
        setLine(board, 8, "§8§m──────────────");

        game.setScoreboard(board);
        p1.setScoreboard(board);

        // p2 gets a mirrored scoreboard showing p1 as opponent
        Scoreboard board2 = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj2 = board2.registerNewObjective("duel", Criteria.DUMMY,
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<gradient:#fffb00:#00ff00><bold>Practice</bold></gradient>"));
        obj2.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < SB_ENTRIES.length; i++) {
            Team t = board2.registerNewTeam("line" + i);
            t.addEntry(SB_ENTRIES[i]);
            obj2.getScore(SB_ENTRIES[i]).setScore(SB_ENTRIES.length - 1 - i);
        }

        setLine(board2, 0, "§8§m──────────────");
        setLine(board2, 1, "§fvs §e" + (p2.getUniqueId().equals(game.getPlayer1Uuid()) ? game.getPlayer2Name() : game.getPlayer1Name()));
        setLine(board2, 2, " ");
        setLine(board2, 3, "§7Gegner HP: §c" + formatHp(getOpponent(game, p2)));
        setLine(board2, 4, " ");
        setLine(board2, 5, "§7Zeit: §f" + formatTime(0));
        setLine(board2, 6, " ");
        setLine(board2, 7, "§7Modus: §f" + capitalize(game.getGamemode()));
        setLine(board2, 8, "§8§m──────────────");

        game.setScoreboard2(board2);
        p2.setScoreboard(board2);
    }

    private void startScoreboardUpdater(DuelGame game) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (game.getState() != DuelState.FIGHTING) {
                task.cancel();
                return;
            }
            int elapsed = game.getDurationSeconds();
            Player p1 = Bukkit.getPlayer(game.getPlayer1Uuid());
            Player p2 = Bukkit.getPlayer(game.getPlayer2Uuid());

            if (game.getScoreboard() != null && p1 != null) {
                setLine(game.getScoreboard(), 3, "§7Gegner HP: §c" + formatHp(p2));
                setLine(game.getScoreboard(), 5, "§7Zeit: §f" + formatTime(elapsed));
            }
            if (game.getScoreboard2() != null && p2 != null) {
                setLine(game.getScoreboard2(), 3, "§7Gegner HP: §c" + formatHp(p1));
                setLine(game.getScoreboard2(), 5, "§7Zeit: §f" + formatTime(elapsed));
            }
        }, 20L, 20L);
    }

    private void clearDuelScoreboard(Player player) {
        if (player != null && player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private void setLine(Scoreboard board, int lineIndex, String text) {
        Team team = board.getTeam("line" + lineIndex);
        if (team != null) team.setPrefix(text);
    }

    private String formatHp(Player p) {
        if (p == null || !p.isOnline()) return "§8-";
        double hp = p.getHealth();
        // Build a simple heart bar: up to 10 hearts
        int hearts = (int) Math.ceil(hp / 2.0);
        return "§c" + ("♥".repeat(Math.max(0, hearts))) + " §7(" + String.format("%.1f", hp) + ")";
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private Player getOpponent(DuelGame game, Player player) {
        UUID opponentUuid = game.getOpponent(player.getUniqueId());
        return opponentUuid != null ? Bukkit.getPlayer(opponentUuid) : null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // -----------------------------------------------------------------------
    // Handle death
    // -----------------------------------------------------------------------

    public void handleDeath(UUID loserUuid) {
        DuelGame game = activeDuels.get(loserUuid);
        if (game == null) return;

        // Player left/died before the fight started (WAITING or COUNTDOWN):
        // abort cleanly so the arena is freed and the opponent isn't left stuck.
        if (game.getState() != DuelState.FIGHTING) {
            if (game.getState() == DuelState.ENDING) return; // already being cleaned up
            abortDuel(game, loserUuid);
            return;
        }

        game.setState(DuelState.ENDING);

        UUID winnerUuid = game.getOpponent(loserUuid);
        if (winnerUuid == null) return;

        game.setWinnerUuid(winnerUuid);

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

                    // Re-fetch Player refs on main thread to avoid stale references
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player winner = Bukkit.getPlayer(winnerUuid);
                        Player loser  = Bukkit.getPlayer(loserUuid);
                        finishDuel(game, winner, loser);
                    });
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("[DuelManager] ELO update failed for "
                            + winnerUuid + " vs " + loserUuid + ": " + ex.getMessage());
                    // Free arena so it doesn't get permanently locked
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        activeDuels.remove(game.getPlayer1Uuid());
                        activeDuels.remove(game.getPlayer2Uuid());
                        plugin.getArenaManager().markInUse(game.getArena(), false);
                        plugin.getArenaManager().resetArena(game.getArena());
                        sendToLobby(Bukkit.getPlayer(winnerUuid));
                        sendToLobby(Bukkit.getPlayer(loserUuid));
                    });
                    return null;
                });
    }

    /**
     * Cancels a duel that never reached the FIGHTING state (e.g. a player
     * disconnected during the countdown). Frees the arena and returns the
     * remaining player to the lobby so nobody is left stranded.
     */
    private void abortDuel(DuelGame game, UUID quitterUuid) {
        game.setState(DuelState.ENDING); // makes the countdown task cancel itself

        activeDuels.remove(game.getPlayer1Uuid());
        activeDuels.remove(game.getPlayer2Uuid());

        UUID survivorUuid = game.getOpponent(quitterUuid);
        if (survivorUuid != null) {
            Player survivor = Bukkit.getPlayer(survivorUuid);
            if (survivor != null && survivor.isOnline()) {
                clearDuelScoreboard(survivor);
                plugin.getSpectatorManager().removeSpectator(survivor);
                sendToLobby(survivor);
            }
        }

        plugin.getArenaManager().markInUse(game.getArena(), false);
        plugin.getArenaManager().resetArena(game.getArena());
    }

    private void finishDuel(DuelGame game, Player winner, Player loser) {
        if (winner != null && winner.isOnline()) {
            clearDuelScoreboard(winner);
            winner.showTitle(Title.title(
                    Component.text("Victory!", NamedTextColor.GOLD),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
            winner.playSound(winner.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
            winner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 40, 4, false, false));
        }

        if (loser != null && loser.isOnline()) {
            clearDuelScoreboard(loser);
            loser.showTitle(Title.title(
                    Component.text("Defeat!", NamedTextColor.RED),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
            loser.playSound(loser.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.4f, 1.5f);

            Arena arena = game.getArena();
            Location specSpawn = arena.getSpawnSpec() != null ? arena.getSpawnSpec() : arena.getSpawn1();
            plugin.getSpectatorManager().makeSpectator(loser, specSpawn);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (winner != null && winner.isOnline()) {
                plugin.getSpectatorManager().removeSpectator(winner);
                sendToLobby(winner);
            }
            if (loser != null && loser.isOnline()) {
                plugin.getSpectatorManager().removeSpectator(loser);
                sendToLobby(loser);
            }

            activeDuels.remove(game.getPlayer1Uuid());
            activeDuels.remove(game.getPlayer2Uuid());
            plugin.getArenaManager().markInUse(game.getArena(), false);
            plugin.getArenaManager().resetArena(game.getArena());
        }, 60L);
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
