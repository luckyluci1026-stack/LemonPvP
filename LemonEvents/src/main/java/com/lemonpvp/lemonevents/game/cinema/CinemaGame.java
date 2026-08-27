package com.lemonpvp.lemonevents.game.cinema;

import com.lemonpvp.lemonevents.LemonEvents;
import com.lemonpvp.lemonevents.game.AbstractGame;
import com.lemonpvp.lemonevents.model.GameEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class CinemaGame extends AbstractGame {

    private final List<CinemaFrame> script = new ArrayList<>();

    public CinemaGame(LemonEvents plugin, GameEvent event) {
        super(plugin, event);
    }

    @Override
    public void startGame() {
        running = true;
        loadScript();

        broadcastParticipants(MM.deserialize(
            "<bold><gradient:#fffb00:#00ff00>🎬 Cinema Event</gradient></bold> " +
            "<green>is starting! Sit back and enjoy.</green>"));

        // Put all players in adventure mode, facing the screen
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setGameMode(org.bukkit.GameMode.ADVENTURE);
        }

        // Schedule each frame
        for (CinemaFrame frame : script) {
            scheduleTask(Bukkit.getScheduler().runTaskLater(plugin,
                () -> playFrame(frame), frame.getDelayTicks()));
        }

        // End cinema after last frame + 60 ticks buffer
        long endDelay = script.stream()
                .mapToLong(CinemaFrame::getDelayTicks)
                .max().orElse(0) + 60;
        scheduleTask(Bukkit.getScheduler().runTaskLater(plugin, this::endGame, endDelay));
    }

    private void loadScript() {
        // Look up script by event name in events.yml
        String key = "cinema." + event.getName() + ".frames";
        List<?> frames = plugin.getEventsConfig().getList(key);
        if (frames == null) return;

        for (Object raw : frames) {
            if (!(raw instanceof Map<?, ?> map)) continue;
            String typeStr = String.valueOf(map.get("type"));
            long delay = getLong(map, "delay", 0);

            CinemaFrame.Builder builder = new CinemaFrame.Builder().delay(delay);
            try {
                switch (typeStr.toUpperCase()) {
                    case "TITLE" -> {
                        builder.type(CinemaFrame.FrameType.TITLE)
                               .title(getString(map, "title", ""))
                               .subtitle(getString(map, "subtitle", ""))
                               .fadeIn(getInt(map, "fade_in", 10))
                               .stay(getInt(map, "stay", 70))
                               .fadeOut(getInt(map, "fade_out", 20));
                        script.add(builder.build());
                    }
                    case "CHAT" -> {
                        builder.type(CinemaFrame.FrameType.CHAT)
                               .message(getString(map, "message", ""));
                        script.add(builder.build());
                    }
                    case "SOUND" -> {
                        builder.type(CinemaFrame.FrameType.SOUND)
                               .sound(getString(map, "sound", ""))
                               .volume((float) getDouble(map, "volume", 1.0))
                               .pitch((float) getDouble(map, "pitch", 1.0));
                        script.add(builder.build());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Bad cinema frame: " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + script.size() + " cinema frames for " + event.getName());
    }

    private void playFrame(CinemaFrame frame) {
        for (UUID uuid : getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            switch (frame.getType()) {
                case TITLE -> {
                    Title title = Title.title(
                        MM.deserialize(frame.getTitle()),
                        MM.deserialize(frame.getSubtitle()),
                        Title.Times.times(
                            Duration.ofMillis(frame.getFadeIn() * 50L),
                            Duration.ofMillis(frame.getStay() * 50L),
                            Duration.ofMillis(frame.getFadeOut() * 50L)));
                    p.showTitle(title);
                }
                case CHAT -> p.sendMessage(MM.deserialize(frame.getMessage()));
                case SOUND -> {
                    try {
                        Sound sound = Sound.valueOf(
                            frame.getSound().replace(":", "_").replace(".", "_").toUpperCase());
                        p.playSound(p.getLocation(), sound, frame.getVolume(), frame.getPitch());
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private Set<UUID> getAllParticipants() {
        Set<UUID> all = new LinkedHashSet<>(participants);
        all.addAll(finishOrder);
        return all;
    }

    // Cinema has no winner — just end gracefully
    @Override
    protected void checkWinCondition() {} // override: cinema doesn't track eliminations

    @Override
    protected void doCleanup() {
        broadcastParticipants(MM.deserialize("<green>🎬 Cinema event has ended. Thank you for watching!"));
    }

    private static String getString(Map<?, ?> m, String k, String def) {
        Object v = m.get(k); return v != null ? v.toString() : def;
    }
    private static int getInt(Map<?, ?> m, String k, int def) {
        Object v = m.get(k); return v instanceof Number n ? n.intValue() : def;
    }
    private static long getLong(Map<?, ?> m, String k, long def) {
        Object v = m.get(k); return v instanceof Number n ? n.longValue() : def;
    }
    private static double getDouble(Map<?, ?> m, String k, double def) {
        Object v = m.get(k); return v instanceof Number n ? n.doubleValue() : def;
    }
}
