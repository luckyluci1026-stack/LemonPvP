package dev.lemonpvp.pvp.killstreaks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StreakRulesTest {

    private final StreakRules rules = StreakRules.of(List.of("3", "5", "10"), 5, 5,
            Map.of("3", List.of("speed 1 10"), "10", List.of("strength 2 15", "Regeneration 1 5")));

    @Test
    void milestonesAndRepeats() {
        assertFalse(this.rules.isMilestone(1));
        assertTrue(this.rules.isMilestone(3));
        assertFalse(this.rules.isMilestone(4));
        assertTrue(this.rules.isMilestone(5));
        assertTrue(this.rules.isMilestone(10));
        assertFalse(this.rules.isMilestone(12));
        assertTrue(this.rules.isMilestone(15), "every 5 after the last milestone");
        assertTrue(this.rules.isMilestone(20));
    }

    @Test
    void shutdownFromTheMinimumStreak() {
        assertFalse(this.rules.isShutdown(4));
        assertTrue(this.rules.isShutdown(5));
        assertFalse(StreakRules.of(List.of(), 0, 0, Map.of()).isShutdown(99), "0 switches it off");
    }

    @Test
    void highestRewardLevelUpToTheStreak() {
        assertEquals(List.of(), this.rules.rewardsFor(2));
        assertEquals(List.of(new StreakRules.Reward("speed", 0, 10)), this.rules.rewardsFor(5));
        assertEquals(List.of(new StreakRules.Reward("strength", 1, 15), new StreakRules.Reward("regeneration", 0, 5)),
                this.rules.rewardsFor(25));
        assertEquals(List.of("speed", "strength", "regeneration").size(), this.rules.effectNames().size());
    }

    @Test
    void invalidValuesAreExplained() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> StreakRules.reward("speed fast 10"));
        assertTrue(e.getMessage().contains("level"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> StreakRules.reward("speed 1"));
        assertThrows(IllegalArgumentException.class, () -> StreakRules.reward("speed 11 10"));
        assertThrows(IllegalArgumentException.class, () -> StreakRules.reward("speed 1 601"));
        assertThrows(IllegalArgumentException.class, () -> StreakRules.of(List.of("zero"), 0, 0, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> StreakRules.of(List.of("3"), -1, 0, Map.of()));
    }
}
