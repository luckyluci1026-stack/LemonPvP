package dev.lemonpvp.pvp.killstreaks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Which streaks are announced and what they are worth. Built from config.yml; invalid values
 * throw {@link IllegalArgumentException} with a message for the administrator. No server
 * code (StreakRulesTest).
 */
final class StreakRules {

    private static final Pattern EFFECT_NAME = Pattern.compile("[a-z0-9_]{1,64}");

    /**
     * A potion effect reward.
     *
     * @param effect    effect key, e.g. {@code speed}
     * @param amplifier Minecraft amplifier (level - 1)
     * @param seconds   duration
     */
    record Reward(String effect, int amplifier, int seconds) {
    }

    private final NavigableSet<Integer> milestones;
    private final int repeatEvery;
    private final int shutdownMin;
    private final NavigableMap<Integer, List<Reward>> rewards;

    private StreakRules(NavigableSet<Integer> milestones, int repeatEvery, int shutdownMin,
                        NavigableMap<Integer, List<Reward>> rewards) {
        this.milestones = milestones;
        this.repeatEvery = repeatEvery;
        this.shutdownMin = shutdownMin;
        this.rewards = rewards;
    }

    /**
     * @param milestones  streaks that are announced, e.g. ["3", "5", "10"]
     * @param repeatEvery after the last milestone, announce every N kills (0 = never)
     * @param shutdownMin announce who ended a streak of at least this length (0 = never)
     * @param rewards     streak → reward specs like "speed 1 10" (effect, level, seconds)
     */
    static StreakRules of(List<String> milestones, int repeatEvery, int shutdownMin, Map<String, List<String>> rewards) {
        TreeSet<Integer> ms = new TreeSet<>();
        for (String m : milestones) {
            ms.add(positive("milestone", m));
        }
        if (repeatEvery < 0 || shutdownMin < 0) {
            throw new IllegalArgumentException("repeat-every and shutdown-min-streak must not be negative");
        }
        TreeMap<Integer, List<Reward>> rw = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : rewards.entrySet()) {
            List<Reward> list = new ArrayList<>();
            for (String spec : e.getValue()) {
                list.add(reward(spec));
            }
            rw.put(positive("rewards key", e.getKey()), List.copyOf(list));
        }
        return new StreakRules(ms, repeatEvery, shutdownMin, rw);
    }

    /** @return whether a streak of this length is announced */
    boolean isMilestone(int streak) {
        if (this.milestones.contains(streak)) {
            return true;
        }
        if (this.repeatEvery == 0 || this.milestones.isEmpty()) {
            return false;
        }
        int last = this.milestones.last();
        return streak > last && (streak - last) % this.repeatEvery == 0;
    }

    /** @return whether ending a streak of this length is announced */
    boolean isShutdown(int endedStreak) {
        return this.shutdownMin > 0 && endedStreak >= this.shutdownMin;
    }

    /** @return the rewards of the highest reward level up to this streak (empty below the first) */
    List<Reward> rewardsFor(int streak) {
        Map.Entry<Integer, List<Reward>> e = this.rewards.floorEntry(streak);
        return e == null ? List.of() : e.getValue();
    }

    /** @return every effect name used in the rewards (checked against the server's registry) */
    List<String> effectNames() {
        return this.rewards.values().stream().flatMap(List::stream).map(Reward::effect).distinct().toList();
    }

    /** Parses "speed 1 10": effect, level 1-10, seconds 1-600. */
    static Reward reward(String spec) {
        String[] p = spec.strip().toLowerCase(Locale.ROOT).split("\\s+");
        if (p.length != 3 || !EFFECT_NAME.matcher(p[0]).matches()) {
            throw new IllegalArgumentException("reward '" + spec + "' must look like 'speed 1 10' (effect level seconds)");
        }
        int level = number("level", p[1], 1, 10, spec);
        int seconds = number("seconds", p[2], 1, 600, spec);
        return new Reward(p[0], level - 1, seconds);
    }

    private static int positive(String what, String value) {
        return number(what, value, 1, 100_000, value);
    }

    private static int number(String what, String value, int min, int max, String context) {
        try {
            int v = Integer.parseInt(value.strip());
            if (v >= min && v <= max) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        throw new IllegalArgumentException(what + " in '" + context + "' must be a whole number from " + min + " to " + max);
    }
}
