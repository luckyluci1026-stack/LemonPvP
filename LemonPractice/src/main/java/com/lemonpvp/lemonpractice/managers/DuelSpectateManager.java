package com.lemonpvp.lemonpractice.managers;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.duel.DuelGame;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Voluntary duel spectating (distinct from the loser-death spectator flow):
 * any free player can watch a running duel from the arena's spectator spawn.
 * Tracks who watches which game so a finished or aborted duel automatically
 * returns its audience to the practice lobby spawn.
 */
public class DuelSpectateManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final Map<UUID, DuelGame> watching = new ConcurrentHashMap<>();
    private final Map<DuelGame, Set<UUID>> audience = new ConcurrentHashMap<>();

    public DuelSpectateManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    /** Starts spectating a duel. Fails politely if the player is busy. */
    public boolean spectate(Player viewer, DuelGame game) {
        UUID uuid = viewer.getUniqueId();
        if (plugin.getDuelManager().isInDuel(uuid) || plugin.getTeamDuelManager().isInTeamDuel(uuid)) {
            msg(viewer, "<red>You can't spectate while you're in a match.");
            return false;
        }
        if (plugin.getQueueManager().isQueued(uuid)) {
            plugin.getQueueManager().removeFromQueue(uuid);
            msg(viewer, "<gray>Left the queue to spectate.");
        }
        leave(viewer, false); // switching matches: detach from the old one silently

        Location spec = game.getArena().getSpawnSpec() != null
                ? game.getArena().getSpawnSpec() : game.getArena().getSpawn1();
        if (spec == null) { msg(viewer, "<red>That arena has no spectator spot."); return false; }

        watching.put(uuid, game);
        audience.computeIfAbsent(game, g -> ConcurrentHashMap.newKeySet()).add(uuid);
        plugin.getSpectatorManager().makeSpectator(viewer, spec.clone().add(0, 2, 0));
        msg(viewer, "<green>Now spectating <white>" + game.getPlayer1Name() + " <gray>vs <white>"
                + game.getPlayer2Name() + "<green>. <gray>Leave with <white>/spectate leave<gray>.");
        notifyFighter(game.getPlayer1Uuid(), viewer.getName());
        notifyFighter(game.getPlayer2Uuid(), viewer.getName());
        return true;
    }

    /** Stops spectating and returns the player to the practice lobby spawn. */
    public void leave(Player viewer, boolean announce) {
        DuelGame game = watching.remove(viewer.getUniqueId());
        if (game == null) return;
        Set<UUID> set = audience.get(game);
        if (set != null) set.remove(viewer.getUniqueId());
        plugin.getSpectatorManager().removeSpectator(viewer);
        toLobbySpawn(viewer);
        if (announce) msg(viewer, "<gray>Stopped spectating.");
    }

    /** Returns every voluntary spectator of a finished/aborted game to the lobby. */
    public void endFor(DuelGame game) {
        Set<UUID> set = audience.remove(game);
        if (set == null) return;
        for (UUID uuid : set) {
            watching.remove(uuid);
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer == null || !viewer.isOnline()) continue;
            plugin.getSpectatorManager().removeSpectator(viewer);
            toLobbySpawn(viewer);
            msg(viewer, "<gray>The match you were spectating ended.");
        }
    }

    public boolean isWatching(UUID uuid) {
        return watching.containsKey(uuid);
    }

    public void handleQuit(UUID uuid) {
        DuelGame game = watching.remove(uuid);
        if (game != null) {
            Set<UUID> set = audience.get(game);
            if (set != null) set.remove(uuid);
        }
    }

    /** The practice lobby spawn from config (same section the join flow uses). */
    private void toLobbySpawn(Player p) {
        var cfg = plugin.getConfig();
        if (cfg.contains("lobby.spawn")) {
            var world = Bukkit.getWorld(cfg.getString("lobby.spawn.world", "world"));
            if (world != null) {
                p.teleport(new Location(world,
                        cfg.getDouble("lobby.spawn.x"), cfg.getDouble("lobby.spawn.y"), cfg.getDouble("lobby.spawn.z"),
                        (float) cfg.getDouble("lobby.spawn.yaw", 0), (float) cfg.getDouble("lobby.spawn.pitch", 0)));
                return;
            }
        }
        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    private void notifyFighter(UUID uuid, String viewerName) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendActionBar(MM.deserialize("<!italic><gray>👁 <white>" + viewerName
                + " <gray>is now spectating"));
    }

    private void msg(Player p, String mini) {
        p.sendMessage(MM.deserialize("<!italic>" + mini));
    }
}
