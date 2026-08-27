package com.lemonpvp.lemonevents.managers;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.game.cinema.CinemaGame;
import com.lemonpvp.lemonevents.game.horror.EscapeGame;
import com.lemonpvp.lemonevents.game.horror.HuntGame;
import com.lemonpvp.lemonevents.game.horror.MafiaGame;
import com.lemonpvp.lemonevents.game.hungergames.HungerGamesGame;
import com.lemonpvp.lemonevents.game.lemonroyale.LemonRoyaleGame;
import com.lemonpvp.lemonevents.game.pvp.FreeForAllGame;
import com.lemonpvp.lemonevents.game.pvp.SumoGame;
import com.lemonpvp.lemonevents.game.pvp.TeamFightGame;
import com.lemonpvp.lemonevents.game.pvp.TournamentGame;
import com.lemonpvp.lemonevents.model.EventStatus;
import com.lemonpvp.lemonevents.model.EventType;
import com.lemonpvp.lemonevents.model.GameEvent;

import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EventManager {

    private final LemonEvents plugin;

    // name → event data (from DB)
    private final Map<String, GameEvent> events = new ConcurrentHashMap<>();
    // name → running game instance
    private final Map<String, AbstractGame> activeGames = new ConcurrentHashMap<>();
    // name → set of UUIDs waiting to join
    private final Map<String, Set<UUID>> waitingPlayers = new ConcurrentHashMap<>();

    public EventManager(LemonEvents plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getDatabase().loadAllEvents().thenAccept(list -> {
            for (GameEvent e : list) {
                events.put(e.getName(), e);
                if (e.getStatus() == EventStatus.WAITING) {
                    waitingPlayers.putIfAbsent(e.getName(), ConcurrentHashMap.newKeySet());
                }
            }
            plugin.getLogger().info("Loaded " + events.size() + " events.");
        }).exceptionally(ex -> { plugin.getLogger().severe("[EventManager] loadAll failed: " + ex.getMessage()); return null; });
    }

    public CompletableFuture<GameEvent> createEvent(String name, EventType type,
                                                      int prize3, int prize2, int prize1) {
        if (events.containsKey(name)) return CompletableFuture.completedFuture(null);
        return plugin.getDatabase().createEvent(name, type, prize3, prize2, prize1)
                .thenApply(event -> {
                    if (event != null) {
                        events.put(name, event);
                        waitingPlayers.put(name, ConcurrentHashMap.newKeySet());
                    }
                    return event;
                });
    }

    public boolean addToWaiting(UUID uuid, String eventName) {
        GameEvent event = events.get(eventName);
        if (event == null || event.getStatus() != EventStatus.WAITING) return false;
        Set<UUID> waiting = waitingPlayers.computeIfAbsent(eventName, k -> ConcurrentHashMap.newKeySet());
        if (!waiting.add(uuid)) return false;
        plugin.getDatabase().addParticipant(event.getId(), uuid);
        return true;
    }

    public CompletableFuture<Boolean> startEvent(String name) {
        GameEvent event = events.get(name);
        if (event == null || event.getStatus() != EventStatus.WAITING) {
            return CompletableFuture.completedFuture(false);
        }

        Set<UUID> participants = new LinkedHashSet<>(
                waitingPlayers.getOrDefault(name, Collections.emptySet()));

        AbstractGame game = createGameInstance(event);
        if (game == null) return CompletableFuture.completedFuture(false);

        participants.forEach(game::addParticipant);

        event.setStatus(EventStatus.ACTIVE);
        activeGames.put(name, game);

        return plugin.getDatabase().updateEventStatus(event.getId(), EventStatus.ACTIVE)
                .thenApply(v -> {
                    Bukkit.getScheduler().runTask(plugin, game::startGame);
                    return true;
                });
    }

    public CompletableFuture<Boolean> endEvent(String name) {
        GameEvent event = events.get(name);
        if (event == null || event.getStatus() != EventStatus.ACTIVE) {
            return CompletableFuture.completedFuture(false);
        }

        AbstractGame game = activeGames.remove(name);
        if (game != null) game.forceEnd();

        event.setStatus(EventStatus.ENDED);
        return plugin.getDatabase().updateEventStatus(event.getId(), EventStatus.ENDED)
                .thenApply(v -> true);
    }

    /** Called by game implementations when they naturally end. */
    public void onGameEnded(String eventName) {
        activeGames.remove(eventName);
        GameEvent event = events.get(eventName);
        if (event != null) {
            event.setStatus(EventStatus.ENDED);
            plugin.getDatabase().updateEventStatus(event.getId(), EventStatus.ENDED);
        }
    }

    /** Force-ends all active games and marks them ENDED in the DB. Called on plugin shutdown. */
    public void endAllActiveGames() {
        new ArrayList<>(activeGames.entrySet()).forEach(entry -> {
            AbstractGame game = entry.getValue();
            game.forceEnd();
            GameEvent ev = events.get(entry.getKey());
            if (ev != null) {
                ev.setStatus(EventStatus.ENDED);
                plugin.getDatabase().updateEventStatus(ev.getId(), EventStatus.ENDED);
            }
        });
        activeGames.clear();
    }

    public GameEvent getEvent(String name) {
        return events.get(name);
    }

    public AbstractGame getActiveGame(String name) {
        return activeGames.get(name);
    }

    public List<GameEvent> getEventsByStatus(EventStatus status) {
        return events.values().stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }

    public Collection<GameEvent> getAllEvents() {
        return Collections.unmodifiableCollection(events.values());
    }

    public AbstractGame getGameForPlayer(UUID uuid) {
        return activeGames.values().stream()
                .filter(g -> g.hasParticipant(uuid))
                .findFirst().orElse(null);
    }

    private AbstractGame createGameInstance(GameEvent event) {
        String name = event.getName().toUpperCase();
        return switch (event.getType()) {
            case LemonRoyale -> new LemonRoyaleGame(plugin, event);
            case HungerGames -> new HungerGamesGame(plugin, event);
            case Cinema      -> new CinemaGame(plugin, event);
            case PvP         -> {
                if (name.contains("TOURNAMENT")) yield new TournamentGame(plugin, event);
                if (name.contains("TEAMFIGHT"))  yield new TeamFightGame(plugin, event);
                if (name.contains("FFA") || name.contains("FREEFORALL")) yield new FreeForAllGame(plugin, event);
                if (name.contains("SUMO"))       yield new SumoGame(plugin, event);
                yield new FreeForAllGame(plugin, event); // default PvP
            }
            case Horror      -> {
                if (name.contains("ESCAPE")) yield new EscapeGame(plugin, event);
                if (name.contains("MAFIA"))  yield new MafiaGame(plugin, event);
                if (name.contains("HUNT"))   yield new HuntGame(plugin, event);
                yield new EscapeGame(plugin, event); // default Horror
            }
        };
    }
}
