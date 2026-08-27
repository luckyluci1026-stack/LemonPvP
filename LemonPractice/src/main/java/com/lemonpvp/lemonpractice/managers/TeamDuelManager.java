package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelState;
import com.lemonpvp.lemonpractice.duel.TeamDuelGame;
import com.lemonpvp.lemonpractice.model.Arena;
import com.lemonpvp.lemonpractice.model.PlayerKit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Party 2v2 duels: duos queue per gamemode and are matched against the next
 * waiting duo. Runs alongside the 1v1 {@link DuelManager} without touching it —
 * team matches are casual (no ELO/streaks), use the same arenas/kits, block
 * friendly fire and end when a whole team is down.
 */
public class TeamDuelManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX =
            "<gradient:#fffb00:#00ff00><bold>2v2</bold></gradient> <dark_gray>»</dark_gray> ";

    private final LemonPractice plugin;
    /** All four participant UUIDs map to the same game. */
    private final Map<UUID, TeamDuelGame> activeGames = new ConcurrentHashMap<>();
    /** gamemode -> waiting duos (each an ordered pair of UUIDs). */
    private final Map<String, Deque<List<UUID>>> waitingDuos = new ConcurrentHashMap<>();

    public TeamDuelManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    // -- Queue ---------------------------------------------------------------

    /**
     * Queues a duo for a 2v2. If another duo is already waiting for the same
     * gamemode, the match starts immediately; otherwise the duo waits.
     */
    public void queueDuo(UUID a, UUID b, String gamemode) {
        String gm = gamemode.toLowerCase();
        if (isInTeamDuel(a) || isInTeamDuel(b)) return;
        // Drop any earlier waiting entry containing either player.
        for (Deque<List<UUID>> deque : waitingDuos.values()) {
            deque.removeIf(duo -> duo.contains(a) || duo.contains(b));
        }
        Deque<List<UUID>> queue = waitingDuos.computeIfAbsent(gm, k -> new ArrayDeque<>());
        List<UUID> opponentDuo;
        synchronized (queue) {
            opponentDuo = queue.poll();
            if (opponentDuo == null) {
                queue.add(List.of(a, b));
            }
        }
        if (opponentDuo != null) {
            List<UUID> duo = List.of(a, b);
            Bukkit.getScheduler().runTask(plugin, () -> startTeamDuel(opponentDuo, duo, gm));
        } else {
            message(a, "<gray>Your duo is queued for <gold>2v2 " + gm + "</gold> — waiting for opponents...");
            message(b, "<gray>Your duo is queued for <gold>2v2 " + gm + "</gold> — waiting for opponents...");
        }
    }

    /** Removes any waiting duo containing this player (e.g. on quit), telling the partner. */
    public void leaveQueue(UUID uuid) {
        for (Deque<List<UUID>> deque : waitingDuos.values()) {
            deque.removeIf(duo -> {
                if (!duo.contains(uuid)) return false;
                // Let the still-online partner know the 2v2 queue was cancelled.
                for (UUID member : duo) {
                    if (member.equals(uuid)) continue;
                    message(member, "<red>2v2 queue cancelled — your partner left.");
                }
                return true;
            });
        }
    }

    // -- Match lifecycle -----------------------------------------------------

    private void startTeamDuel(List<UUID> duo1, List<UUID> duo2, String gamemode) {
        List<Player> t1 = resolveOnline(duo1);
        List<Player> t2 = resolveOnline(duo2);
        if (t1.size() < 2 || t2.size() < 2) {
            // Someone vanished while waiting — requeue the complete duo.
            if (t1.size() == 2) queueDuo(duo1.get(0), duo1.get(1), gamemode);
            if (t2.size() == 2) queueDuo(duo2.get(0), duo2.get(1), gamemode);
            return;
        }

        Optional<Arena> arenaOpt = plugin.getArenaManager().getFreeArenaForGamemode(gamemode);
        if (arenaOpt.isEmpty()) {
            broadcastTo(t1, "<red>No free arena right now — try again shortly.");
            broadcastTo(t2, "<red>No free arena right now — try again shortly.");
            return;
        }
        Arena arena = arenaOpt.get();
        plugin.getArenaManager().markInUse(arena, true);

        TeamDuelGame game = new TeamDuelGame(
                new LinkedHashSet<>(duo1), new LinkedHashSet<>(duo2), gamemode, arena);
        for (UUID u : duo1) activeGames.put(u, game);
        for (UUID u : duo2) activeGames.put(u, game);

        // Teammates spawn side by side on their team's spawn point.
        placeTeam(t1, arena.getSpawn1(), gamemode);
        placeTeam(t2, arena.getSpawn2(), gamemode);

        for (Player p : t1) teamIntro(p, t1, t2);
        for (Player p : t2) teamIntro(p, t2, t1);

        startCountdown(game);
    }

    private void placeTeam(List<Player> team, Location spawn, String gamemode) {
        for (int i = 0; i < team.size(); i++) {
            Player p = team.get(i);
            preparePlayer(p);
            Location loc = spawn.clone().add(0, 2, 0);
            // Nudge the second teammate a block sideways so they don't stack.
            if (i == 1) loc.add(loc.getDirection().getZ(), 0, -loc.getDirection().getX());
            p.teleport(loc);
            applyKit(p, gamemode);
        }
    }

    private void teamIntro(Player p, List<Player> mates, List<Player> foes) {
        String mate = mates.stream().filter(m -> !m.equals(p)).findFirst().map(Player::getName).orElse("?");
        String enemies = foes.get(0).getName() + " & " + foes.get(1).getName();
        p.sendMessage(MM.deserialize("<!italic>" + PREFIX + "<gray>You + <aqua>" + mate
                + " <gray>vs <red>" + enemies));
    }

    private void startCountdown(TeamDuelGame game) {
        game.setState(DuelState.COUNTDOWN);
        final int[] count = {3};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (game.getState() == DuelState.ENDING) { task.cancel(); return; }
            List<Player> players = onlineParticipants(game);
            if (count[0] <= 0) {
                game.setState(DuelState.FIGHTING);
                game.setStartTime(System.currentTimeMillis());
                for (Player p : players) {
                    p.showTitle(Title.title(Component.text("FIGHT!", NamedTextColor.GREEN),
                            Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(300))));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                }
                task.cancel();
                return;
            }
            for (Player p : players) {
                p.showTitle(Title.title(Component.text(count[0], NamedTextColor.YELLOW),
                        Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1f);
            }
            count[0]--;
        }, 20L, 20L);
    }

    /** Called when a participant dies or disconnects mid-match. */
    public void handleDeath(UUID uuid) {
        TeamDuelGame game = activeGames.get(uuid);
        if (game == null) return;
        if (game.getState() == DuelState.ENDING) return;
        if (game.getState() != DuelState.FIGHTING) { abort(game); return; }
        if (!game.getAlive().remove(uuid)) return;

        Player dead = Bukkit.getPlayer(uuid);
        if (dead != null && dead.isOnline()) {
            Location spec = game.getArena().getSpawnSpec() != null
                    ? game.getArena().getSpawnSpec() : game.getArena().getSpawn1();
            plugin.getSpectatorManager().makeSpectator(dead, spec);
        }

        int team = game.teamOf(uuid);
        if (game.teamEliminated(team)) {
            finish(game, team == 1 ? 2 : 1);
        } else {
            // Teammate fights on — tell both sides.
            for (Player p : onlineParticipants(game)) {
                p.sendMessage(MM.deserialize("<!italic>" + PREFIX + "<gray>"
                        + (dead != null ? dead.getName() : "A player") + " <gray>is down!"));
            }
        }
    }

    private void finish(TeamDuelGame game, int winningTeam) {
        game.setState(DuelState.ENDING);
        Set<UUID> winners = winningTeam == 1 ? game.getTeam1() : game.getTeam2();

        for (Player p : onlineParticipants(game)) {
            boolean won = winners.contains(p.getUniqueId());
            p.showTitle(Title.title(
                    Component.text(won ? "Victory!" : "Defeat!", won ? NamedTextColor.GOLD : NamedTextColor.RED),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
            p.playSound(p.getLocation(), won
                    ? org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE
                    : org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 0.6f, won ? 1f : 1.5f);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanup(game), 60L);
    }

    /** A pre-fight dropout (countdown quit): scrap the match without a winner. */
    private void abort(TeamDuelGame game) {
        game.setState(DuelState.ENDING);
        for (Player p : onlineParticipants(game)) {
            p.sendMessage(MM.deserialize("<!italic>" + PREFIX + "<red>Match aborted — a player left."));
        }
        cleanup(game);
    }

    private void cleanup(TeamDuelGame game) {
        for (UUID u : new ArrayList<>(game.getTeam1())) activeGames.remove(u);
        for (UUID u : new ArrayList<>(game.getTeam2())) activeGames.remove(u);
        for (Player p : onlineParticipants(game)) {
            plugin.getSpectatorManager().removeSpectator(p);
            sendToLobby(p);
        }
        plugin.getArenaManager().markInUse(game.getArena(), false);
        plugin.getArenaManager().resetArena(game.getArena());
    }

    // -- Queries -------------------------------------------------------------

    public TeamDuelGame getGame(UUID uuid) { return activeGames.get(uuid); }
    public boolean isInTeamDuel(UUID uuid) { return activeGames.containsKey(uuid); }

    /** True if both players are participants of the same running team duel. */
    public boolean inSameMatch(UUID a, UUID b) {
        TeamDuelGame g = activeGames.get(a);
        return g != null && g == activeGames.get(b);
    }

    // -- Helpers (mirroring DuelManager's private ones) ----------------------

    private List<Player> resolveOnline(List<UUID> uuids) {
        List<Player> out = new ArrayList<>();
        for (UUID u : uuids) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) out.add(p);
        }
        return out;
    }

    private List<Player> onlineParticipants(TeamDuelGame game) {
        List<Player> out = new ArrayList<>();
        for (UUID u : game.getTeam1()) { Player p = Bukkit.getPlayer(u); if (p != null && p.isOnline()) out.add(p); }
        for (UUID u : game.getTeam2()) { Player p = Bukkit.getPlayer(u); if (p != null && p.isOnline()) out.add(p); }
        return out;
    }

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

    private void broadcastTo(List<Player> players, String mini) {
        for (Player p : players) p.sendMessage(MM.deserialize("<!italic>" + PREFIX + mini));
    }

    private void message(UUID uuid, String mini) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendMessage(MM.deserialize("<!italic>" + PREFIX + mini));
    }
}
