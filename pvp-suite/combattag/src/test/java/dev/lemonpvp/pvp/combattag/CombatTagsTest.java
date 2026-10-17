package dev.lemonpvp.pvp.combattag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CombatTagsTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final CombatTags tags = new CombatTags(this.now::get);
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void tagCountsDownAndExpires() {
        assertTrue(this.tags.tag(this.alice, 15_000), "first hit starts the combat");
        assertEquals(15_000, this.tags.remaining(this.alice));
        this.now.addAndGet(10_000);
        assertEquals(5_000, this.tags.remaining(this.alice));
        assertEquals(List.of(), this.tags.expire());
        this.now.addAndGet(5_000);
        assertEquals(0, this.tags.remaining(this.alice));
        assertEquals(List.of(this.alice), this.tags.expire());
        assertEquals(List.of(), this.tags.expire(), "reported only once");
    }

    @Test
    void anotherHitExtendsButNeverShortens() {
        this.tags.tag(this.alice, 15_000);
        this.now.addAndGet(10_000);
        assertFalse(this.tags.tag(this.alice, 15_000), "still in combat: no new 'you are in combat' message");
        assertEquals(15_000, this.tags.remaining(this.alice));
        this.tags.tag(this.alice, 1_000);
        assertEquals(15_000, this.tags.remaining(this.alice), "a shorter tag does not cut the running one");
    }

    @Test
    void untagAndTaggedList() {
        this.tags.tag(this.alice, 15_000);
        this.tags.tag(this.bob, 5_000);
        assertEquals(2, this.tags.tagged().size());
        this.tags.untag(this.alice);
        assertEquals(List.of(this.bob), this.tags.tagged());
        this.now.addAndGet(5_000);
        assertEquals(List.of(), this.tags.tagged());
    }
}
