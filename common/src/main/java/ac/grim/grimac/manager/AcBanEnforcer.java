/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Turns a row in {@link AcBanStore} into an actual disconnect.
 *
 * <p>Checked once at login, and again immediately when {@code /acban} names
 * somebody who is currently online.</p>
 *
 * <p>The lookup is asynchronous, so a banned player can technically be
 * connected for the fraction of a second it takes the database to answer. That
 * is deliberate: the alternative is blocking the Netty thread on a database
 * round-trip for every single join, which costs every legitimate player to
 * inconvenience a banned one slightly less.</p>
 */
@UtilityClass
public class AcBanEnforcer {

    private static final String DEFAULT_SCREEN =
            "<gradient:#6C5CE7:#00D4FF><bold>BuckSMPAC</bold></gradient>"
                    + "<newline><newline><white>You are banned from this network."
                    + "<newline><newline><gray>Reason: <white>%reason%"
                    + "<newline><gray>Date: <white>%date%"
                    + "<newline><gray>Expires in: <white>%remaining%";

    /** Checks the ban list for a joining player and disconnects them if banned. */
    public static void checkOnLogin(@NotNull User user) {
        UUID uuid = user.getUUID();
        if (uuid == null) return;

        AcBanStore.lookup(uuid).thenAccept(record -> {
            if (record == null) return;
            disconnect(user, screen(record.reason(), record.whenEpochMs(), record.expiresEpochMs()));
            LogUtil.info("Refused " + user.getName() + " — banned by BuckSMPAC (" + record.reason() + ")");
        });
    }

    /** Disconnects a player who was just banned while online. */
    public static void kickBanned(@NotNull UUID uuid, @NotNull String reason, long expiresEpochMs) {
        User user = findUser(uuid);
        if (user == null) return;
        disconnect(user, screen(reason, System.currentTimeMillis(), expiresEpochMs));
    }

    private static @Nullable User findUser(UUID uuid) {
        try {
            Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(uuid);
            if (channel == null) return null;
            return PacketEvents.getAPI().getProtocolManager().getUser(channel);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void disconnect(User user, Component screen) {
        try {
            user.sendPacket(new WrapperPlayServerDisconnect(screen));
        } catch (Exception ignored) {
            // Not in a state that accepts a disconnect packet; closing still works.
        }
        user.closeConnection();
    }

    private static Component screen(String reason, long whenEpochMs, long expiresEpochMs) {
        String raw = GrimAPI.INSTANCE.getConfigManager().getConfig()
                .getStringElse("acban-screen", DEFAULT_SCREEN);
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(whenEpochMs));
        return MessageUtil.miniMessage(raw
                .replace("%reason%", MessageUtil.miniMessageSafe(reason))
                .replace("%date%", date)
                .replace("%remaining%", AcBanDuration.remaining(expiresEpochMs)));
    }
}
