package dev.lemonpvp.pvp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lemonpvp.pvp.api.PlayerStats;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatsFileTest {

    @TempDir
    Path dir;

    @Test
    void writesAndReadsTheSameStatistics() throws Exception {
        Path file = this.dir.resolve("stats.tsv");
        List<PlayerStats> players = List.of(new PlayerStats(UUID.randomUUID(), "Alice", 12, 4, 3, 7, 1043),
                new PlayerStats(UUID.randomUUID(), "Bob_2", 0, 9, 0, 1, 912));
        StatsFile.write(file, players);
        StatsFile.Loaded loaded = StatsFile.read(file);
        assertEquals(players, loaded.players());
        assertTrue(loaded.problems().isEmpty());
        assertFalse(Files.exists(this.dir.resolve("stats.tsv.tmp")), "the temporary file was moved into place");
        assertTrue(Files.readString(file).startsWith(StatsFile.HEADER));
    }

    @Test
    void missingFileMeansNoStatistics() throws Exception {
        StatsFile.Loaded loaded = StatsFile.read(this.dir.resolve("nothing.tsv"));
        assertTrue(loaded.players().isEmpty());
        assertTrue(loaded.problems().isEmpty());
    }

    @Test
    void brokenLinesAreSkippedAndReported() throws Exception {
        Path file = this.dir.resolve("stats.tsv");
        UUID ok = UUID.randomUUID();
        Files.writeString(file, String.join("\n",
                StatsFile.HEADER,
                ok + "\tAlice\t1\t2\t0\t1\t1010",
                "not-a-uuid\tBob\t1\t2\t0\t1\t1010",
                ok + "\tCarol\t1\t2",
                ok + "\tDave\t-1\t2\t0\t1\t1010",
                ""), StandardCharsets.UTF_8);
        StatsFile.Loaded loaded = StatsFile.read(file);
        assertEquals(List.of("Alice"), loaded.players().stream().map(PlayerStats::name).toList());
        assertEquals(3, loaded.problems().size(), loaded.problems().toString());
        assertTrue(loaded.problems().get(0).contains("line 3"), loaded.problems().get(0));
    }
}
