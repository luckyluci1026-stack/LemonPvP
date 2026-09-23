/*
 * This file is part of BuckSMPAC, a fork of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 DefineOutside and contributors, licensed under GPLv3.
 *
 * BuckSMPAC custom addition.
 */
package ac.grim.grimac.command.commands;

import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.AcBanStore;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * {@code /acunban <player>} — lifts a BuckSMPAC ban.
 *
 * <p>Takes a plain name rather than a player selector, because the whole point
 * is that the player is offline: they were disconnected by the ban. The name is
 * resolved through the index {@link AcBanStore} writes alongside each ban. A
 * raw UUID is accepted too, for the case where somebody changed their name
 * after being banned.</p>
 */
public class AcUnban implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("acunban")
                        .permission("bucksmpac.acunban")
                        .required("target", StringParser.stringParser())
                        .handler(this::handle)
        );
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        if (!AcBanStore.isAvailable()) {
            AcBan.send(sender, "acban-storage-unavailable",
                    "%prefix% <red>The ban list is unavailable — BuckSMPAC's database is off or failed to load. See the console.");
            return;
        }

        String target = context.getOrDefault("target", "");
        if (target == null || target.isBlank()) return;

        UUID direct = tryParseUuid(target);
        if (direct != null) {
            finish(sender, direct, null, target);
            return;
        }

        AcBanStore.resolveBannedName(target).thenAccept(uuid -> {
            if (uuid == null) {
                AcBan.send(sender, "acunban-not-banned",
                        "%prefix% <gray>No BuckSMPAC ban found for</gray> <gradient:#6C5CE7:#00D4FF>%target%</gradient><gray>.</gray>",
                        target, null);
                return;
            }
            finish(sender, uuid, target, target);
        });
    }

    private void finish(Sender sender, UUID uuid, String name, String shown) {
        AcBanStore.unban(uuid, name);
        LogUtil.info("acunban: " + shown + " unbanned by " + sender.getName());
        AcBan.send(sender, "acunban-success",
                "%prefix% <gradient:#6C5CE7:#00D4FF>%target%</gradient> <gray>has been unbanned.</gray>",
                shown, null);
    }

    private static UUID tryParseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
