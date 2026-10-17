package dev.lemonpvp.pvp.combattag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Who is in combat until when. The clock is passed in, so tests can move time forward
 * without waiting (CombatTagsTest). Main thread only.
 */
final class CombatTags {

    private final Map<UUID, Long> until = new HashMap<>();
    private final LongSupplier clock;

    /** @param clock current time in milliseconds, e.g. {@code System::currentTimeMillis} */
    CombatTags(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Tags a player for at least {@code millis} from now; a longer running tag is kept.
     *
     * @return whether the player was not in combat before
     */
    boolean tag(UUID player, long millis) {
        boolean fresh = remaining(player) == 0;
        this.until.merge(player, this.clock.getAsLong() + millis, Math::max);
        return fresh;
    }

    /** @return milliseconds left, 0 if not in combat */
    long remaining(UUID player) {
        Long end = this.until.get(player);
        return end == null ? 0 : Math.max(0, end - this.clock.getAsLong());
    }

    void untag(UUID player) {
        this.until.remove(player);
    }

    /** @return players whose tag ran out since the last call; they are no longer tagged */
    List<UUID> expire() {
        long now = this.clock.getAsLong();
        List<UUID> expired = new ArrayList<>();
        for (Iterator<Map.Entry<UUID, Long>> it = this.until.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() <= now) {
                expired.add(e.getKey());
                it.remove();
            }
        }
        return expired;
    }

    /** @return players currently in combat */
    List<UUID> tagged() {
        long now = this.clock.getAsLong();
        return this.until.entrySet().stream().filter(e -> e.getValue() > now).map(Map.Entry::getKey).toList();
    }
}
