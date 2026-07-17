package com.lemonpvp.lemonpractice.tournament;

import com.lemonpvp.lemonpractice.LemonPractice;
import com.lemonpvp.lemonpractice.database.PracticeDatabase.Standing;
import com.lemonpvp.lemonpractice.util.WinAnimation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Official multi-day tournaments — hosted on the DUELS server, because a
 * tournament is at its core ranked 1v1s: standings are this server's own
 * ranked-duel wins in the tournament gamemode during the qualification window,
 * among signed-up players. A periodic task closes expired qualifications and
 * freezes the top-N finalists; the owner runs the finals and records the champion.
 */
public class TournamentManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final LemonPractice plugin;
    private final Map<Integer, Tournament> tournaments = new ConcurrentHashMap<>();

    public TournamentManager(LemonPractice plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getDatabase().loadAllTournaments().thenAccept(list -> {
            if (list == null) return;
            for (Tournament t : list) tournaments.put(t.getId(), t);
        });
    }

    /** Auto-closes qualifications whose window has elapsed. */
    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L * 30, 20L * 60);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Tournament t : tournaments.values()) {
            if (t.getState() == Tournament.State.QUALIFICATION && t.getEndAt() > 0 && now >= t.getEndAt()) {
                closeToFinals(t.getId(), finalsSize());
            }
        }
    }

    // -- Lifecycle -----------------------------------------------------------

    public CompletableFuture<Tournament> create(String name, String gamemode) {
        return plugin.getDatabase().createTournament(name, gamemode).thenApply(t -> {
            if (t != null) tournaments.put(t.getId(), t);
            return t;
        });
    }

    /** Opens the qualification window for {@code days} days. */
    public boolean openQualification(int id, int days) {
        Tournament t = tournaments.get(id);
        if (t == null || t.getState() != Tournament.State.SIGNUP) return false;
        long now = System.currentTimeMillis();
        t.setStartAt(now);
        t.setEndAt(now + days * 86_400_000L);
        t.setState(Tournament.State.QUALIFICATION);
        plugin.getDatabase().updateTournament(t);
        Bukkit.broadcast(MM.deserialize(
                "<gradient:#fffb00:#ffa751><bold>TOURNAMENT</bold></gradient> <dark_gray>» <white>" + t.getName()
                + " <gray>qualification is live for <white>" + days + " days<gray>! Play <white>" + t.getGamemode()
                + " <gray>ranked to climb — <white>/tournament join"));
        return true;
    }

    public CompletableFuture<Boolean> signup(UUID uuid, int id) {
        Tournament t = tournaments.get(id);
        if (t == null || (t.getState() != Tournament.State.SIGNUP && t.getState() != Tournament.State.QUALIFICATION)) {
            return CompletableFuture.completedFuture(false);
        }
        return plugin.getDatabase().tournamentSignup(id, uuid);
    }

    /** Async standings (top-first) for a tournament, restricted to its sign-ups. */
    public CompletableFuture<List<Standing>> standings(int id) {
        Tournament t = tournaments.get(id);
        if (t == null) return CompletableFuture.completedFuture(List.of());
        return plugin.getDatabase().getTournamentSignups(id).thenCompose(signups ->
                plugin.getDatabase().getTournamentStandings(t.getGamemode(), t.getStartAt(), t.getEndAt(), signups));
    }

    /** Freezes the top-N players as finalists and moves the tournament to FINALS. */
    public void closeToFinals(int id, int topN) {
        Tournament t = tournaments.get(id);
        if (t == null || t.getState() != Tournament.State.QUALIFICATION) return;
        t.setState(Tournament.State.FINALS);
        plugin.getDatabase().updateTournament(t);
        standings(id).thenAccept(result -> {
            List<Standing> all = result != null ? result : List.of();
            List<Standing> top = all.size() > topN ? new ArrayList<>(all.subList(0, topN)) : new ArrayList<>(all);
            plugin.getDatabase().saveTournamentFinalists(id, top);
            Bukkit.getScheduler().runTask(plugin, () -> announceFinalists(t, top));
        });
    }

    public CompletableFuture<List<Standing>> finalists(int id) {
        return plugin.getDatabase().getTournamentFinalists(id);
    }

    /** Records the tournament champion and celebrates if they are online. */
    public void setChampion(int id, UUID uuid) {
        Tournament t = tournaments.get(id);
        if (t == null) return;
        t.setChampion(uuid);
        t.setState(Tournament.State.ENDED);
        plugin.getDatabase().updateTournament(t);
        String name = nameOf(uuid);
        Bukkit.broadcast(MM.deserialize(
                "<gradient:#fffb00:#ffa000><bold>🏆 " + name + " is the " + t.getName() + " champion!</bold></gradient>"));
        var winner = Bukkit.getPlayer(uuid);
        if (winner != null) WinAnimation.celebrate(plugin, winner, t.getName());
    }

    private void announceFinalists(Tournament t, List<Standing> top) {
        Bukkit.broadcast(MM.deserialize("<gradient:#fffb00:#ffa751><bold>TOURNAMENT</bold></gradient> <dark_gray>» "
                + "<white>" + t.getName() + " <gray>finals — top " + top.size() + ":"));
        int seed = 1;
        for (Standing s : top) {
            Bukkit.broadcast(MM.deserialize("<gray>#" + (seed++) + " <white>" + nameOf(s.uuid())
                    + " <dark_gray>(" + s.wins() + " wins)"));
        }
    }

    // -- Queries -------------------------------------------------------------

    public Tournament get(int id) { return tournaments.get(id); }
    public Collection<Tournament> all() { return tournaments.values(); }

    /** The most relevant live tournament (accepting sign-ups or in qualification). */
    public Tournament activeJoinable() {
        Tournament best = null;
        for (Tournament t : tournaments.values()) {
            if (t.getState() == Tournament.State.SIGNUP || t.getState() == Tournament.State.QUALIFICATION) {
                if (best == null || t.getId() > best.getId()) best = t;
            }
        }
        return best;
    }

    public int finalsSize() {
        return plugin.getConfig().getInt("tournaments.finals-size", 10);
    }

    public int defaultDays() {
        return plugin.getConfig().getInt("tournaments.default-days", 5);
    }

    private String nameOf(UUID id) {
        var p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n != null ? n : id.toString().substring(0, 8);
    }
}
