package dev.lemonpvp.pvp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.pvp.api.PlayerStats;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatsBookTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    @Test
    void killCountsForBothPlayersAndMovesRating() {
        StatsBook book = new StatsBook(32);
        StatsBook.Kill k = book.recordKill(ALICE, "Alice", BOB, "Bob");
        assertEquals(new PlayerStats(ALICE, "Alice", 1, 0, 1, 1, 1016), k.killer());
        assertEquals(new PlayerStats(BOB, "Bob", 0, 1, 0, 0, 984), k.victim());
        assertEquals(16, k.ratingChange());
        assertEquals(0, k.endedStreak());
        assertTrue(book.dirty());
    }

    @Test
    void streaksGrowAndEndWithTheNextDeath() {
        StatsBook book = new StatsBook(32);
        for (int i = 0; i < 5; i++) {
            book.recordKill(ALICE, "Alice", BOB, "Bob");
        }
        assertEquals(5, book.stats(ALICE).orElseThrow().streak());
        StatsBook.Kill revenge = book.recordKill(BOB, "Bob", ALICE, "Alice");
        assertEquals(5, revenge.endedStreak(), "Bob ended Alice's streak of 5");
        assertEquals(0, revenge.victim().streak());
        assertEquals(5, revenge.victim().bestStreak(), "the best streak is kept");
        assertEquals(1, revenge.killer().streak());
    }

    @Test
    void deathWithoutKillerEndsTheStreakButKeepsTheRating() {
        StatsBook book = new StatsBook(32);
        book.recordKill(ALICE, "Alice", BOB, "Bob");
        int rating = book.stats(ALICE).orElseThrow().rating();
        assertEquals(1, book.recordDeath(ALICE, "Alice"));
        PlayerStats after = book.stats(ALICE).orElseThrow();
        assertEquals(0, after.streak());
        assertEquals(1, after.deaths());
        assertEquals(rating, after.rating());
    }

    @Test
    void ratingNeverGoesBelowZero() {
        // A weak killer (rating 0) beats Bob (rating 3): about 50 points move, more than Bob has.
        StatsBook book = new StatsBook(100);
        book.load(List.of(new PlayerStats(ALICE, "Alice", 0, 0, 0, 0, 0), new PlayerStats(BOB, "Bob", 0, 0, 0, 0, 3)));
        StatsBook.Kill k = book.recordKill(ALICE, "Alice", BOB, "Bob");
        assertEquals(50, k.ratingChange());
        assertEquals(0, k.victim().rating());
    }

    @Test
    void aPlayerCannotKillThemselves() {
        assertThrows(IllegalArgumentException.class, () -> new StatsBook(32).recordKill(ALICE, "Alice", ALICE, "Alice"));
    }

    @Test
    void rankingIsByRatingThenKills() {
        StatsBook book = new StatsBook(32);
        book.load(List.of(new PlayerStats(ALICE, "Alice", 3, 0, 0, 3, 1100),
                new PlayerStats(BOB, "Bob", 9, 0, 0, 9, 1100),
                new PlayerStats(CAROL, "Carol", 1, 5, 0, 1, 900)));
        assertEquals(List.of("Bob", "Alice", "Carol"), book.top(10).stream().map(PlayerStats::name).toList());
        assertEquals(List.of("Bob"), book.top(1).stream().map(PlayerStats::name).toList());
        assertEquals(1, book.rank(BOB));
        assertEquals(2, book.rank(ALICE));
        assertEquals(3, book.rank(CAROL));
        assertEquals(0, book.rank(UUID.randomUUID()));
        assertFalse(book.dirty(), "loading is not a change");
    }

    @Test
    void namesAreFoundCaseInsensitiveAndFollowRenames() {
        StatsBook book = new StatsBook(32);
        book.recordKill(ALICE, "Alice", BOB, "Bob");
        assertTrue(book.statsByName("aLiCe").isPresent());
        book.seen(ALICE, "Alicia");
        assertTrue(book.statsByName("alicia").isPresent());
        assertTrue(book.statsByName("alice").isEmpty());
    }

    @Test
    void resetRemovesPlayers() {
        StatsBook book = new StatsBook(32);
        book.recordKill(ALICE, "Alice", BOB, "Bob");
        book.markSaved();
        assertTrue(book.reset(ALICE));
        assertFalse(book.reset(ALICE));
        assertTrue(book.dirty());
        assertEquals(1, book.resetAll());
        assertEquals(0, book.size());
    }
}
