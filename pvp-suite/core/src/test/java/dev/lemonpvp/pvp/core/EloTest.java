package dev.lemonpvp.pvp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EloTest {

    @Test
    void equalPlayersMoveHalfTheKFactor() {
        assertEquals(16, Elo.change(1000, 1000, 32));
    }

    @Test
    void beatingAStrongerPlayerIsWorthMore() {
        int upset = Elo.change(1000, 1400, 32);
        int expected = Elo.change(1400, 1000, 32);
        assertEquals(29, upset);
        assertEquals(3, expected);
        assertTrue(upset > expected);
    }

    @Test
    void everyKillIsWorthAtLeastOnePoint() {
        assertEquals(1, Elo.change(3000, 100, 32));
    }
}
