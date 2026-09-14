/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Runs the punishment side of the anticheat without punishing anybody.
 *
 * <h2>Why this exists</h2>
 *
 * <p>Thresholds are guesses until real players have moved through them. The
 * only way to find out whether {@code Simulation} bans at 100 flags on
 * <em>this</em> server — with its TPS, its ping, its Bedrock players, its
 * elytra routes — is to let it run against real traffic. Doing that with bans
 * armed means finding out by banning somebody who did nothing wrong.</p>
 *
 * <p>With {@code punishment-dry-run} on, every ban and every console command
 * the ladders would have run is written down instead of executed. Alerts,
 * logging and the checks themselves are untouched, so the numbers in the file
 * are the same numbers a live run would have produced.</p>
 *
 * <h2>The file</h2>
 *
 * <p>Lines land in {@code plugins/BuckSMPAC/testmode.log} as well as the
 * console, because a console scrolls and a week-long soak test does not fit in
 * it. The format is one line per would-be punishment, timestamped, naming the
 * player, what fired and what it would have run.</p>
 */
@UtilityClass
public class TestMode {

    private static final String CONFIG_KEY = "punishment-dry-run";
    private static final String LOG_FILE = "testmode.log";
    /** DateTimeFormatter rather than SimpleDateFormat: this is shared state
     *  and SimpleDateFormat is not safe to share between threads. */
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Logged the first time something is held back, so the reason is obvious in the console. */
    private static volatile boolean announced;

    /**
     * Whether punishments should be recorded instead of carried out.
     *
     * <p>Read from the config on every call rather than cached: it is only
     * consulted when a punishment is about to fire, which is rare, and an
     * operator flipping the switch and reloading expects it to take effect.</p>
     */
    public static boolean isDryRun() {
        try {
            return GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse(CONFIG_KEY, false);
        } catch (Throwable ignored) {
            // Config not loaded yet - punish normally rather than silently not at all.
            return false;
        }
    }

    /**
     * Records a punishment that was held back.
     *
     * @param player what the punishment was aimed at, for the log line
     * @param what   the kind of punishment, e.g. {@code "console command"}
     * @param detail the command or ban that would have happened
     */
    public static void record(@NotNull String player, @NotNull String what, @NotNull String detail) {
        announceOnce();

        String line = "[" + STAMP.format(LocalDateTime.now()) + "] " + player + " - " + what + ": " + detail;
        LogUtil.info("[TEST] " + line);
        append(line);
    }

    private static void announceOnce() {
        if (announced) return;
        announced = true;
        LogUtil.info("BuckSMPAC is in test mode (" + CONFIG_KEY + ": true). Nothing below is actually being "
                + "carried out — it is written to " + LOG_FILE + " so you can check the thresholds first.");
    }

    /**
     * Appends one line to the log file.
     *
     * <p>Opened and closed per line rather than held open: this writes a
     * handful of lines a day even on a busy server, and a handle kept across
     * a reload is a handle leaked.</p>
     */
    private static void append(String line) {
        Path file = logFile();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // The console line above already carries the information; losing
            // the file copy must not cost us the punishment path.
            LogUtil.warn("Could not write to " + LOG_FILE + ": " + e);
        }
    }

    private static @Nullable Path logFile() {
        try {
            return GrimAPI.INSTANCE.getGrimPlugin().getDataFolder().toPath().resolve(LOG_FILE);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
