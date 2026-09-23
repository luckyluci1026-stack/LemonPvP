/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import github.scarsz.configuralize.DynamicConfig;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Works out whether this backend server sits behind a proxy.
 *
 * <p>This answer decides whether proxy-origin plugin messages are trusted, so
 * getting it wrong disconnects legitimate players. Upstream only parsed config
 * files off disk by relative path, which silently answers "no proxy" whenever
 * the working directory, the file layout or the YAML parser does not cooperate
 * — and "no proxy" is the answer that kicks people.</p>
 *
 * <p>So the file scan is now the last resort rather than the only strategy:</p>
 *
 * <ol>
 *   <li>an explicit {@code proxy.behind-proxy} in the config — the operator
 *       knows, and saying so should end the guessing;</li>
 *   <li>the running server's own loaded configuration, read through
 *       reflection. This is authoritative: it is the setting the server
 *       actually booted with, not a file that may or may not be the one in
 *       use;</li>
 *   <li>the old file scan, for platforms the reflection does not cover.</li>
 * </ol>
 *
 * <p>{@link #describe()} reports which strategy answered, so a wrong answer is
 * visible in the log instead of being a mystery.</p>
 */
@UtilityClass
public class ProxyDetection {

    private static boolean behindProxy;
    private static String source = "not yet detected";

    /**
     * Resolves proxy status. Safe to call again after a config reload; every
     * strategy swallows its own failures and falls through to the next.
     */
    public static void detect() {
        Boolean declared = readDeclaredSetting();
        if (declared != null) {
            behindProxy = declared;
            source = "proxy.behind-proxy in config.yml (set to " + declared + ")";
            return;
        }

        if (spigotBungeeEnabled()) {
            behindProxy = true;
            source = "the running server's SpigotConfig.bungee";
            return;
        }

        if (paperVelocityEnabled()) {
            behindProxy = true;
            source = "the running server's proxies.velocity.enabled";
            return;
        }

        if (scanConfigFiles()) {
            behindProxy = true;
            source = "a server config file on disk";
            return;
        }

        behindProxy = false;
        source = "nothing — no proxy forwarding could be found in this server's configuration";
    }

    /** True when this server is configured to sit behind a proxy. */
    public static boolean isBehindProxy() {
        return behindProxy;
    }

    /** Human-readable statement of what the answer was based on. */
    public static String describe() {
        return (behindProxy ? "behind a proxy, according to " : "NOT behind a proxy — detection found ") + source;
    }

    /**
     * @return the operator's explicit choice, or null when they left it on
     * "auto" (or the config is not readable yet).
     */
    private static Boolean readDeclaredSetting() {
        try {
            String raw = GrimAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("proxy.behind-proxy", "auto");
            if (raw == null) return null;
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "true", "yes" -> Boolean.TRUE;
                case "false", "no" -> Boolean.FALSE;
                default -> null; // "auto" and anything unrecognised
            };
        } catch (Throwable ignored) {
            // Config not loaded yet, or a platform without this key.
            return null;
        }
    }

    /** Spigot/Paper BungeeCord forwarding, straight off the loaded config. */
    private static boolean spigotBungeeEnabled() {
        try {
            Class<?> spigotConfig = Class.forName("org.spigotmc.SpigotConfig");
            Field bungee = spigotConfig.getDeclaredField("bungee");
            bungee.setAccessible(true);
            return bungee.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Paper's modern Velocity forwarding, straight off the loaded config. */
    private static boolean paperVelocityEnabled() {
        try {
            Class<?> global = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            Method get = global.getMethod("get");
            Object instance = get.invoke(null);
            if (instance == null) return false; // called before Paper finished booting

            Object proxies = readField(instance, "proxies");
            if (proxies == null) return false;
            Object velocity = readField(proxies, "velocity");
            if (velocity == null) return false;

            Field enabled = velocity.getClass().getDeclaredField("enabled");
            enabled.setAccessible(true);
            return enabled.getBoolean(velocity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object readField(Object holder, String name) throws Exception {
        Field field = holder.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(holder);
    }

    /** The original strategy, kept as a fallback for platforms the above miss. */
    private static boolean scanConfigFiles() {
        return getBooleanFromFile("spigot.yml", "settings.bungeecord")
                || getBooleanFromFile("paper.yml", "settings.velocity-support.enabled")
                || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19)
                && getBooleanFromFile("config/paper-global.yml", "proxies.velocity.enabled"));
    }

    private static boolean getBooleanFromFile(String pathToFile, String pathToValue) {
        File file = new File(pathToFile);
        if (!file.exists()) return false;

        DynamicConfig config = new DynamicConfig();
        config.addSource(ProxyDetection.class, "temp", file);
        try {
            config.loadAll();
            return config.getBoolean(pathToValue);
        } catch (Exception e) {
            return false;
        }
    }
}
