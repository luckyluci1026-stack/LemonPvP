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
 * <h2>The carrier problem</h2>
 *
 * <p>A plugin message needs a player connection to travel through. The
 * obvious candidate — the player being banned — is also the one about to be
 * disconnected, and a message written to a channel that closes a moment later
 * may never leave. On a quiet server that player is often the <em>only</em>
 * candidate, which is exactly when a cheater gets banned. So a carrier who is
 * not the subject of the message is preferred, and when there is no such
 * player {@link #lastSendUsedSubject()} tells the caller to let the
 * connection live a couple of ticks longer.</p>
 *
 * <p>An unban issued while the server is empty cannot be delivered at all.
 * That is reported rather than swallowed — the operator is told to run
 * {@code /acunban} on the proxy console instead.</p>
 */
@UtilityClass
public class AcBanProxyBridge {

    private static final String CHANNEL = "bucksmpac:bans";
    private static boolean registered;

    /**
     * Whether the most recent send had to travel through the very player it
     * was about to disconnect. Read straight after a {@code sendBan} on the
     * same thread; it is not a general-purpose status flag.
     */
    private static volatile boolean lastSendUsedSubject;

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

    // --- wire format -------------------------------------------------------
    //
    // Kept pure and separate from sending so both ends can be pinned by tests
    // against protocol/ban-channel-samples.tsv. The proxy's decoder is
    // velocity/.../BanMessage.

    /** {@code BAN|uuid|name|when|expires|actor|reason} */
    public static @NotNull String encodeBan(@NotNull UUID uuid, @NotNull String name, long whenEpochMs,
                                            long expiresEpochMs, @NotNull String actor, @NotNull String reason) {
        // The reason is last so a separator inside it cannot shift a field,
        // but every field is scrubbed anyway - the proxy splits with a limit
        // and would otherwise happily read half a reason as the next field.
        return "BAN|" + uuid + '|' + clean(name) + '|' + whenEpochMs + '|' + expiresEpochMs
                + '|' + clean(actor) + '|' + clean(reason);
    }

    /** {@code UNBAN|uuid|name}, with an empty name when it is not known. */
    public static @NotNull String encodeUnban(@NotNull UUID uuid, @Nullable String name) {
        return "UNBAN|" + uuid + '|' + (name == null ? "" : clean(name));
    }

    /** Keeps a field from containing the separator or a line break. */
    public static @NotNull String clean(@NotNull String input) {
        return input.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    // --- sending -----------------------------------------------------------

    public static void sendBan(@NotNull UUID uuid, @NotNull String name, long whenEpochMs,
                               long expiresEpochMs, @NotNull String actor, @NotNull String reason) {
        send(encodeBan(uuid, name, whenEpochMs, expiresEpochMs, actor, reason), "ban of " + name, uuid);
    }

    public static void sendUnban(@NotNull UUID uuid, @Nullable String name) {
        // Nobody is being disconnected here, so any carrier will do.
        send(encodeUnban(uuid, name), "unban of " + (name == null ? uuid.toString() : name), null);
    }

    /**
     * @param subject the player this message is about, who must not be used as
     *                the carrier if anybody else is connected; null when the
     *                message disconnects nobody.
     */
    private static void send(String payload, String what, @Nullable UUID subject) {
        register();
        lastSendUsedSubject = false;

        PlatformPlayer carrier = pickCarrier(subject);
        if (carrier == null) {
            LogUtil.warn("Could not tell the proxy about the " + what + " — a plugin message needs an online "
                    + "player to travel through and nobody is connected.");
            LogUtil.warn("Run the matching command on the proxy console instead, or it will take effect the next "
                    + "time this player is seen.");
            return;
        }

        lastSendUsedSubject = subject != null && subject.equals(carrier.getUniqueId());

        try {
            carrier.sendPluginMessage(CHANNEL, payload.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            LogUtil.warn("Failed to tell the proxy about the " + what + ": " + t);
        }
    }

    /** @see #lastSendUsedSubject */
    public static boolean lastSendUsedSubject() {
        return lastSendUsedSubject;
    }

    /** Any connected player, preferring one the message is not about. */
    private static @Nullable PlatformPlayer pickCarrier(@Nullable UUID avoid) {
        PlatformPlayer fallback = null;
        try {
            for (PlatformPlayer player : GrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
                if (player == null) continue;
                if (avoid != null && avoid.equals(player.getUniqueId())) {
                    fallback = player;
                    continue;
                }
                return player;
            }
        } catch (Throwable ignored) {
            // Server not fully up yet.
        }
        return fallback;
    }
}
