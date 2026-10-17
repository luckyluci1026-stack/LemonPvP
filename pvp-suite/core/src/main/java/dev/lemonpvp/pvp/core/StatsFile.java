package dev.lemonpvp.pvp.core;

import dev.lemonpvp.pvp.api.PlayerStats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Reads and writes the statistics as a small text file, one player per line:
 * {@code uuid<TAB>name<TAB>kills<TAB>deaths<TAB>streak<TAB>bestStreak<TAB>rating}.
 * Writing goes to a temporary file that then replaces the old one, so a crash during
 * saving never leaves a half-written file behind.
 */
final class StatsFile {

    static final String HEADER = "# LemonPvP stats v1: uuid\tname\tkills\tdeaths\tstreak\tbestStreak\trating";

    /**
     * @param players  loaded statistics
     * @param problems one message per line that could not be read (the line is skipped)
     */
    record Loaded(List<PlayerStats> players, List<String> problems) {
    }

    private StatsFile() {
    }

    /** @return the statistics; empty if the file does not exist yet */
    static Loaded read(Path file) throws IOException {
        List<PlayerStats> players = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (!Files.exists(file)) {
            return new Loaded(players, problems);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            try {
                if (f.length != 7) {
                    throw new IllegalArgumentException("expected 7 columns, found " + f.length);
                }
                players.add(new PlayerStats(UUID.fromString(f[0]), f[1], nonNegative(f[2]), nonNegative(f[3]),
                        nonNegative(f[4]), nonNegative(f[5]), nonNegative(f[6])));
            } catch (IllegalArgumentException e) {
                problems.add(file.getFileName() + " line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
        return new Loaded(players, problems);
    }

    static void write(Path file, Collection<PlayerStats> players) throws IOException {
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (PlayerStats s : players) {
            out.append(s.id()).append('\t').append(clean(s.name())).append('\t').append(s.kills()).append('\t')
                    .append(s.deaths()).append('\t').append(s.streak()).append('\t').append(s.bestStreak()).append('\t')
                    .append(s.rating()).append('\n');
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, out, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int nonNegative(String s) {
        int v = Integer.parseInt(s.strip());
        if (v < 0) {
            throw new IllegalArgumentException("negative number " + v);
        }
        return v;
    }

    /** Player names never contain tabs or line breaks; this keeps the format intact anyway. */
    private static String clean(String name) {
        return name.replaceAll("[\\t\\r\\n]", "_");
    }
}
