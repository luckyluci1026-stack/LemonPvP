/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager.config;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tells the operator when {@code punishments.yml} or {@code bans.yml} on disk
 * is older than the one this build ships.
 *
 * <p>These two files hold operator-authored data, so they are deliberately
 * excluded from the versioned config updater and {@code saveAllDefaults(false)}
 * never overwrites them. The consequence is easy to miss and expensive: a
 * server that has run the plugin once keeps its original copy <em>forever</em>.
 * Ship a changed punishment ladder and nothing happens — the operator reads the
 * new file in the jar, the server reads the old one on disk, and the two
 * disagree silently.</p>
 *
 * <p>Overwriting would throw away their edits, so this does not. It compares a
 * version marker, writes the current default next to the old file as
 * {@code <name>.new}, and says so loudly enough to be noticed.</p>
 */
@UtilityClass
public class OperatorConfigVersionCheck {

    /**
     * Bump the version when the bundled file changes in a way operators need to
     * pick up. The key differs per file on purpose: Configuralize merges every
     * source into one keyspace, so a shared {@code file-version} key would have
     * the two files overwrite each other.
     */
    private record Tracked(String fileName, String resourceDir, String versionKey, int bundledVersion) {
    }

    private static final List<Tracked> FILES = List.of(
            new Tracked("punishments.yml", "/punishments/", "punishments-version", 2),
            new Tracked("bans.yml", "/bans/", "bans-version", 2)
    );

    public static void run(String langCode) {
        for (Tracked tracked : FILES) {
            try {
                check(tracked, langCode);
            } catch (Exception e) {
                LogUtil.warn("Could not version-check " + tracked.fileName() + ": " + e);
            }
        }
    }

    private static void check(Tracked tracked, String langCode) throws IOException {
        File file = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), tracked.fileName());
        if (!file.exists()) return; // freshly written by saveAllDefaults, already current

        int onDisk = readVersion(file, tracked.versionKey());
        if (onDisk >= tracked.bundledVersion()) return;

        Path sideBySide = file.toPath().resolveSibling(tracked.fileName() + ".new");
        try (InputStream bundled = openBundled(tracked, langCode)) {
            if (bundled != null) {
                Files.copy(bundled, sideBySide, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        LogUtil.warn("Your " + tracked.fileName() + " is out of date (yours: v" + onDisk
                + ", this build ships v" + tracked.bundledVersion() + ").");
        LogUtil.warn("This file is never overwritten automatically, because it holds YOUR settings — "
                + "so the changes in this build are NOT active on your server.");
        LogUtil.warn("The current version has been written to " + tracked.fileName() + ".new — either copy the "
                + "parts you want out of it, or delete " + tracked.fileName() + " and restart to start fresh.");
    }

    /** Line scan rather than a YAML parse, so a hand-edited file still answers. */
    private static int readVersion(File file, String key) {
        Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*:\\s*(\\d+)\\s*$");
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception ignored) {
            // Unreadable or mis-encoded: treat as ancient and warn.
        }
        return 0; // no marker at all — predates versioning
    }

    private static InputStream openBundled(Tracked tracked, String langCode) {
        InputStream stream = GrimAPI.class.getResourceAsStream(tracked.resourceDir() + langCode + ".yml");
        if (stream != null) return stream;
        return GrimAPI.class.getResourceAsStream(tracked.resourceDir() + "en.yml");
    }
}
