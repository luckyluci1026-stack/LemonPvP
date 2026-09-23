/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The index that answers {@code PreLoginEvent}.
 *
 * <p>Everything here is about the two cases that actually bite on a live
 * network: a ban looked up by name before the UUID is known, and a ban whose
 * sentence has run out.</p>
 */
class ProxyBanListTest {

    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);

    /** Records what the list asked the store to do, without any I/O. */
    private static class FakeStorage implements BanStorage {
        final Map<UUID, BanRecord> rows = new LinkedHashMap<>();
        final List<UUID> deleted = new ArrayList<>();

        @Override
        public synchronized Collection<BanRecord> loadAll() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public synchronized void save(BanRecord record) {
            rows.put(record.uuid(), record);
        }

        @Override
        public synchronized void delete(UUID uuid) {
            rows.remove(uuid);
            deleted.add(uuid);
        }

        @Override
        public String describe() {
            return "a fake";
        }

        synchronized int rowCount() {
            return rows.size();
        }

        synchronized List<UUID> deletedCopy() {
            return new ArrayList<>(deleted);
        }
    }

    private static BanRecord ban(UUID uuid, String name, long expiresEpochMs) {
        return new BanRecord(uuid, name, System.currentTimeMillis(), expiresEpochMs, "CONSOLE", "Reach");
    }

    private static ProxyBanList listOf(FakeStorage storage) {
        ProxyBanList bans = new ProxyBanList(storage, NOPLogger.NOP_LOGGER);
        bans.load();
        return bans;
    }

    /** Writes are pushed off the caller's thread, so assertions on them have to wait. */
    private static void awaitUntil(BooleanSupplier condition, String what) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.onSpinWait();
        }
        throw new AssertionError("timed out waiting for " + what);
    }

    @Test
    void findsABanByNameBeforeTheUuidIsKnown() {
        FakeStorage storage = new FakeStorage();
        ProxyBanList bans = listOf(storage);
        UUID uuid = UUID.randomUUID();

        bans.add(ban(uuid, "Lemonightt", System.currentTimeMillis() + 7 * 24 * 60 * MINUTE));

        assertNotNull(bans.lookup("Lemonightt"));
        assertNotNull(bans.lookup("lemonightt"), "pre-login names arrive in whatever case the player typed");
        assertNotNull(bans.lookup("LEMONIGHTT"));
        assertNotNull(bans.lookup(uuid));
        assertNull(bans.lookup("SomebodyElse"));
    }

    @Test
    void aBanThatHasServedItsTimeStopsAnswering() {
        FakeStorage storage = new FakeStorage();
        ProxyBanList bans = listOf(storage);
        UUID uuid = UUID.randomUUID();

        bans.add(ban(uuid, "Steve", System.currentTimeMillis() - MINUTE));

        assertNull(bans.lookup(uuid), "the sentence is over");
        assertNull(bans.lookup("Steve"));
        awaitUntil(() -> storage.deletedCopy().contains(uuid), "the lapsed row to be swept");
    }

    @Test
    void sweepingALapsedBanDoesNotBlockTheLookup() {
        // The sweep runs on the write thread precisely so a login never waits
        // on a database. If that ever regresses, this test hangs rather than
        // quietly costing every login a round-trip.
        // Wide margins on purpose: this must fail because the lookup waited on
        // the store, never because a build machine was busy for a moment.
        FakeStorage slow = new FakeStorage() {
            @Override
            public synchronized void delete(UUID uuid) {
                try {
                    Thread.sleep(3_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.delete(uuid);
            }
        };
        ProxyBanList bans = listOf(slow);
        bans.add(ban(UUID.randomUUID(), "Steve", System.currentTimeMillis() - MINUTE));

        long start = System.nanoTime();
        assertNull(bans.lookup("Steve"));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs < 1_500, "lookup waited " + elapsedMs + "ms on the store");
    }

    @Test
    void unbanningByNameClearsBothIndexes() {
        FakeStorage storage = new FakeStorage();
        ProxyBanList bans = listOf(storage);
        UUID uuid = UUID.randomUUID();
        bans.add(ban(uuid, "Steve", System.currentTimeMillis() + MINUTE));

        assertTrue(bans.remove(null, "steve"));

        assertNull(bans.lookup(uuid), "the uuid index must not outlive the name");
        assertNull(bans.lookup("Steve"));
        assertEquals(0, bans.size());
        awaitUntil(() -> storage.rowCount() == 0, "the row to be deleted");
    }

    @Test
    void unbanningByUuidClearsTheNameToo() {
        FakeStorage storage = new FakeStorage();
        ProxyBanList bans = listOf(storage);
        UUID uuid = UUID.randomUUID();
        bans.add(ban(uuid, "Steve", System.currentTimeMillis() + MINUTE));

        assertTrue(bans.remove(uuid, null));

        assertNull(bans.lookup("Steve"), "a stale name row would resurrect the ban at pre-login");
        assertEquals(0, bans.size());
    }

    @Test
    void unbanningSomebodyWhoIsNotBannedReportsNothingHappened() {
        ProxyBanList bans = listOf(new FakeStorage());

        assertFalse(bans.remove(UUID.randomUUID(), "Nobody"));
        assertFalse(bans.remove(null, "Nobody"));
    }

    @Test
    void loadRebuildsBothIndexesFromStorage() {
        FakeStorage storage = new FakeStorage();
        UUID uuid = UUID.randomUUID();
        storage.save(ban(uuid, "Steve", System.currentTimeMillis() + MINUTE));

        ProxyBanList bans = listOf(storage);

        assertEquals(1, bans.size());
        assertNotNull(bans.lookup("steve"), "a restart must not lose the name index");
        assertNotNull(bans.lookup(uuid));
    }
}
