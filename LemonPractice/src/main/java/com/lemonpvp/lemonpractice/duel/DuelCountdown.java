package com.lemonpvp.lemonpractice.duel;

import com.lemonpvp.lemonpractice.LemonPractice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DuelCountdown {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // Ticks at which each countdown number is shown
    private static final int TICK_3     = 0;
    private static final int TICK_2     = 20;
    private static final int TICK_1     = 40;
    private static final int TICK_FIGHT = 60;

    // Title timing (in ticks): fade-in / stay / fade-out
    private static final Title.Times NUMBER_TIMES =
            Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(750), Duration.ofMillis(250));
    private static final Title.Times FIGHT_TIMES  =
            Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1000), Duration.ofMillis(500));

    // How far above spawn the player starts floating down from (blocks)
    private static final double FLOAT_START_OFFSET = 2.0;
    // Downward velocity applied each tick while floating (blocks/tick, negative = down)
    private static final double FALL_VELOCITY = -0.05;

    private final LemonPractice plugin;
    private final DuelGame game;
    private final Runnable onFightStart;

    private final Set<UUID> frozen = new HashSet<>();
    private BukkitTask task;
    private int ticks = 0;

    // Stored target Y (the actual spawn Y) so we stop once the player arrives
    private double targetY1;
    private double targetY2;

    public DuelCountdown(LemonPractice plugin, DuelGame game, Runnable onFightStart) {
        this.plugin = plugin;
        this.game = game;
        this.onFightStart = onFightStart;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void start() {
        // Capture target Y values from the arena spawn points
        Location spawn1 = game.getArena().getSpawn1();
        Location spawn2 = game.getArena().getSpawn2();
        targetY1 = spawn1 != null ? spawn1.getY() : Double.MIN_VALUE;
        targetY2 = spawn2 != null ? spawn2.getY() : Double.MIN_VALUE;

        // Freeze both players
        UUID p1uuid = game.getPlayer1Uuid();
        UUID p2uuid = game.getPlayer2Uuid();
        frozen.add(p1uuid);
        frozen.add(p2uuid);

        game.setState(DuelState.COUNTDOWN);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickCountdown();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        frozen.clear();
    }

    // -------------------------------------------------------------------------
    // Internal tick logic
    // -------------------------------------------------------------------------

    private void tickCountdown() {
        Player p1 = Bukkit.getPlayer(game.getPlayer1Uuid());
        Player p2 = Bukkit.getPlayer(game.getPlayer2Uuid());

        // If either player disconnected, cancel and let DuelManager handle it
        if (p1 == null || p2 == null) {
            cancel();
            return;
        }

        // Handle show-title / sound milestones first (at tick 0 these run before movement)
        switch (ticks) {
            case TICK_3 -> {
                showCountdownTitle(p1, "§f§l3", NUMBER_TIMES);
                showCountdownTitle(p2, "§f§l3", NUMBER_TIMES);
                playCountdownSound(p1);
                playCountdownSound(p2);
            }
            case TICK_2 -> {
                showCountdownTitle(p1, "§f§l2", NUMBER_TIMES);
                showCountdownTitle(p2, "§f§l2", NUMBER_TIMES);
                playCountdownSound(p1);
                playCountdownSound(p2);
            }
            case TICK_1 -> {
                showCountdownTitle(p1, "§f§l1", NUMBER_TIMES);
                showCountdownTitle(p2, "§f§l1", NUMBER_TIMES);
                playCountdownSound(p1);
                playCountdownSound(p2);
            }
            case TICK_FIGHT -> {
                // Show "Fight!" via MiniMessage gradient
                Component fightTitle = MM.deserialize("<gradient:#fffb00:#00ff00>Fight!</gradient>");
                Title fightTitleObj = Title.title(fightTitle, Component.empty(), FIGHT_TIMES);
                p1.showTitle(fightTitleObj);
                p2.showTitle(fightTitleObj);

                // Level-up sound
                p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                // Unfreeze
                frozen.remove(game.getPlayer1Uuid());
                frozen.remove(game.getPlayer2Uuid());

                // Transition state and fire callback
                game.setState(DuelState.FIGHTING);
                game.setStartTime(System.currentTimeMillis());

                cancel();
                if (onFightStart != null) onFightStart.run();
                return;
            }
        }

        // Float both players downward while frozen
        applyFloat(p1, targetY1);
        applyFloat(p2, targetY2);

        ticks++;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Applies a slow downward drift toward targetY while the player is frozen.
     * Once the player has reached (or passed) the target Y, zero out vertical velocity.
     */
    private void applyFloat(Player player, double targetY) {
        if (!frozen.contains(player.getUniqueId())) return;

        double currentY = player.getLocation().getY();

        // Zero out horizontal movement every tick to freeze the player in place
        double vy;
        if (currentY > targetY + 0.05) {
            vy = FALL_VELOCITY;
        } else {
            // Snap to target Y if close enough
            vy = 0;
            Location loc = player.getLocation().clone();
            loc.setY(targetY);
            player.teleport(loc);
        }

        player.setVelocity(new Vector(0, vy, 0));
    }

    /**
     * Shows a legacy-color countdown number as the title with an empty subtitle.
     */
    private void showCountdownTitle(Player player, String legacyNumber, Title.Times times) {
        // Use Component.text with legacy color parsing via Adventure's legacy serializer
        Component titleComponent = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(legacyNumber);
        Title title = Title.title(titleComponent, Component.empty(), times);
        player.showTitle(title);
    }

    /**
     * Plays the hat note-block click sound (used for countdown ticks).
     */
    private void playCountdownSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    public int getTicks() {
        return ticks;
    }
}
