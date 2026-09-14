/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.proxy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code protocol/ban-channel-samples.tsv}, the file that pins this
 * module's decoder and the anticheat's encoder to the same wire format.
 *
 * <p>Deliberately duplicated in the {@code common} test sources: the two
 * modules share no code, and a shared helper would have to live somewhere one
 * of them could reach — which is exactly the coupling the file avoids.</p>
 */
final class BanChannelSamples {

    record Sample(String payload, String kind, String uuid, String name,
                  String when, String expires, String actor, String reason) {
    }

    private BanChannelSamples() {
    }

    static List<Sample> load() throws IOException {
        // Gradle runs tests with the module directory as the working directory.
        Path file = Path.of("..", "protocol", "ban-channel-samples.tsv").toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("missing " + file + "; both sides of the ban channel read it");
        }

        List<Sample> samples = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] c = line.split("\t", -1);
            if (c.length != 8) {
                throw new IllegalStateException("expected 8 tab-separated columns, got " + c.length + ": " + line);
            }
            samples.add(new Sample(c[0], c[1], c[2], unescape(c[3]), c[4], c[5], unescape(c[6]), unescape(c[7])));
        }
        if (samples.isEmpty()) throw new IllegalStateException("no samples; the pinning would pass vacuously");
        return samples;
    }

    /** {@code <empty>} stands for an empty field, which TSV cannot show. */
    static String unescape(String column) {
        return "<empty>".equals(column) ? "" : column;
    }
}
