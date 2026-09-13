/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Tells the BuckSMPAC Velocity plugin about bans, so the proxy can refuse the
 * player before they reach any server.
 *
 * <p>Without the proxy plugin a banned player still connects to Velocity and
 * lands in limbo; the backends just will not let them in. With it, they are
 * turned away at {@code PreLoginEvent}. This is the link between the two.</p>
 *
 * <p>A plugin message needs a player connection to travel through, so an unban
 * issued while the server is empty cannot be delivered. That is reported
 * rather than swallowed — the operator is told to run {@code /acunban} on the
 * proxy console instead.</p>
 */
@UtilityClass
public class AcBanProxyBridge {

    private static final String CHANNEL = "bucksmpac:bans";
    private static boolean registered;

    /** Registers the outgoing channel. Safe to call more than once. */
    public static void register() {
        if (registered) return;
        try {
            GrimAPI.INSTANCE.getPlatformServer().registerOutgoingPluginChannel(CHANNEL);
            registered = true;
        } catch (Throwable t) {
            LogUtil.warn("Could not register the " + CHANNEL + " channel; the Velocity plugin will not be told "
                    + "about bans: " + t);
        }
    }

    public static void sendBan(@NotNull UUID uuid, @NotNull String name, long whenEpochMs,
                               long expiresEpochMs, @NotNull String actor, @NotNull String reason) {
        // Pipes separate the fields, and the reason is last so one inside it
        // cannot shift anything - but the proxy splits with a limit anyway.
        send("BAN|" + uuid + '|' + name + '|' + whenEpochMs + '|' + expiresEpochMs
                        + '|' + clean(actor) + '|' + clean(reason),
                "ban of " + name);
    }

    public static void sendUnban(@NotNull UUID uuid, @Nullable String name) {
        send("UNBAN|" + uuid + '|' + (name == null ? "" : clean(name)),
                "unban of " + (name == null ? uuid.toString() : name));
    }

    private static void send(String payload, String what) {
        register();

        PlatformPlayer carrier = anyOnlinePlayer();
        if (carrier == null) {
            LogUtil.warn("Could not tell the proxy about the " + what + " — a plugin message needs an online "
                    + "player to travel through and nobody is connected.");
            LogUtil.warn("Run the matching command on the proxy console instead, or it will take effect the next "
                    + "time this player is seen.");
            return;
        }

        try {
            carrier.sendPluginMessage(CHANNEL, payload.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            LogUtil.warn("Failed to tell the proxy about the " + what + ": " + t);
        }
    }

    private static @Nullable PlatformPlayer anyOnlinePlayer() {
        try {
            for (PlatformPlayer player : GrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
                if (player != null) return player;
            }
        } catch (Throwable ignored) {
            // Server not fully up yet.
        }
        return null;
    }

    /** Keeps a field from containing the separator or a line break. */
    private static String clean(String input) {
        return input.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
