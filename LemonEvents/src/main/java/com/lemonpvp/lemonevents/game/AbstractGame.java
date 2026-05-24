package com.lemonpvp.lemonevents.game;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class AbstractGame {

    protected static final MiniMessage MM = MiniMessage.miniMessage();

    protected final LemonEvents plugin;
    protected final GameEvent event;

    /** Players still alive/active in the game. */
    protected final Set<UUID> participants = new LinkedHashSet<>();
    /** Ordered finish list: index 0 = winner (1st place). */
    protected final List<UUID> finishOrder = new ArrayList<>();
    /** Tasks spawned by this game, cancelled on cleanup. */
    protected final List<BukkitTask> tasks = new ArrayList<>();

    protected volatile boolean running = false;

    protected AbstractGame(LemonEvents plugin, GameEvent event) {
        this.plugin = plugin;
        this.event = event;
    }

    public void addParticipant(UUID uuid) {
        participants.add(uuid);
    }

    public boolean hasParticipant(UUID uuid) {
        return participants.contains(uuid) || finishOrder.contains(uuid);
    }

    /** Called by EventManager when the admin force-ends the event. */
    public final void forceEnd() {
        if (!running) return;
        running = false;
        cancelTasks();
        doCleanup();
    }

    /** Called by game logic when a player is eliminated (but not the winner). */
    public final void eliminate(UUID uuid, @org.jetbrains.annotations.Nullable UUID killerUuid) {
        participants.remove(uuid);
        finishOrder.add(0, uuid); // insert at front (lowest position)
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) onEliminated(p, killerUuid != null ? Bukkit.getPlayer(killerUuid) : null);
        checkWinCondition();
    }

    /** Called when a participant disconnects mid-game. */
    public void handleQuit(UUID uuid) {
        if (!participants.remove(uuid)) return;
        finishOrder.add(0, uuid);
        checkWinCondition();
    }

    protected void checkWinCondition() {
        if (participants.size() == 1) {
            UUID winner = participants.iterator().next();
            participants.remove(winner);
            finishOrder.add(0, winner); // winner at front
            Collections.reverse(finishOrder); // now index 0 = 1st place
            endGame();
        } else if (participants.isEmpty()) {
            endGame();
        }
    }

    protected final void endGame() {
        if (!running) return;
        running = false;
        cancelTasks();
        distributePrizes();
        doCleanup();
        plugin.getEventManager().onGameEnded(event.getName());
    }

    private void distributePrizes() {
        UUID first  = finishOrder.size() > 0 ? finishOrder.get(0) : null;
        UUID second = finishOrder.size() > 1 ? finishOrder.get(1) : null;
        UUID third  = finishOrder.size() > 2 ? finishOrder.get(2) : null;

        givePrize(first,  event.getPrize1(), 1);
        givePrize(second, event.getPrize2(), 2);
        givePrize(third,  event.getPrize3(), 3);

        if (first != null) {
            String winner = Bukkit.getOfflinePlayer(first).getName();
            if (winner == null) winner = first.toString().substring(0, 8);
            broadcastAll(MM.deserialize(
                "<bold><gradient:#fffb00:#00ff00>🏆 " + winner +
                " won " + event.getName() + "!</gradient></bold>"));
        }
    }

    private void givePrize(UUID uuid, int amount, int placement) {
        if (uuid == null || amount <= 0) return;
        plugin.getDatabase().setPlacement(event.getId(), uuid, placement);
        plugin.getDatabase().markPrizePaid(event.getId(), uuid);

        com.lemonpvp.lemoncore.LemonCore lc = getLemonCore();
        if (lc != null) {
            lc.getPlayerDataManager()
              .addCoins(uuid, amount, "events:prize:" + event.getName() + ":place" + placement, null);
        }
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.sendMessage(MM.deserialize(
                "<green>You finished <gold>#" + placement + "</gold> and earned <yellow>" +
                amount + " coins</yellow>!"));
        }
    }

    protected void broadcastAll(Component message) {
        Bukkit.broadcast(message);
    }

    protected void broadcastParticipants(Component message) {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
        for (UUID uuid : finishOrder) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    protected com.lemonpvp.lemoncore.LemonCore getLemonCore() {
        org.bukkit.plugin.Plugin lc = Bukkit.getPluginManager().getPlugin("LemonCore");
        if (lc instanceof com.lemonpvp.lemoncore.LemonCore core) return core;
        return null;
    }

    protected void scheduleTask(BukkitTask task) {
        tasks.add(task);
    }

    protected void cancelTasks() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    /** Override to handle a player being eliminated (teleport to spectator, etc.). */
    protected void onEliminated(Player player, @org.jetbrains.annotations.Nullable Player killer) {
        player.sendMessage(MM.deserialize("<red>You have been eliminated!"));
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
    }

    /** Override for event-specific cleanup (remove entities, restore map, etc.). */
    protected void doCleanup() {}

    /** Start the game. Subclasses implement the actual game logic here. */
    public abstract void startGame();
}
